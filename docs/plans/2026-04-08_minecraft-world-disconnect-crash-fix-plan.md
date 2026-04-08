# Minecraftワールド開閉時のクラッシュ防止 / 自動切断機能 実装計画書

**作成日**: 2026-04-08
**最終更新**: 2026-04-08（Round 1 レビュー反映）
**作成者**: Claude (調査担当)
**ステータス**: 計画策定済み（未実装） / Round 1 レビュー完了
**改訂履歴**:
- v1.0 (2026-04-08): 初版策定
- v1.1 (2026-04-08): Round 1 レビュー反映 — `server.execute()` ラムダ内部の二重チェック必要性、`safeExecute` ヘルパー導入、`server.isRunning()` 併用、`MinecraftEduMod` フィールドの `volatile` 化、`SimpleWebSocketServer` の同期制御方針確定
- v1.2 (2026-04-08): Round 2 レビュー反映 — `MinecraftWebSocketHandler.processMessage()` への defense-in-depth チェック追加、Phase 5 を `_cleanupSocket()` 再利用に簡素化、`stop()` の冪等性ガード追加、`/entityspawn` スラッシュコマンドのスレッドセーフ化、テスト追加（再接続レース、二重停止）、Windows 環境での `SO_REUSEADDR` 注記
- v1.3 (2026-04-08): Round 3 レビュー反映 — `stop()` ステップの順序の意味論を明文化、JAR名を実物に修正 (`minecraftedu-mod-0.1.0-1.20.1.jar`)、`Future.get` と `shutdownNow()` の相互作用の保証説明、`SimpleWebSocketServer.commandExecutor` の `final` 性の安全性根拠、停止時ログノイズ抑制、Forge tick タイミング window 説明、テスト不正確箇所削除
**対象ブランチ**: gh-pages / main 両方
**優先度**: HIGH（クラッシュにより作業データ喪失リスクあり）

---

## 0. 目次

1. 背景とユーザー要望
2. 期待される正常動作
3. 現状コードの完全調査
4. クラッシュ・ハングのシナリオ分析
5. 根本原因の整理（5項目）
6. 修正方針サマリ
7. 詳細実装計画（フェーズ別）
8. 各ファイル変更点の詳細
9. テスト計画
10. リリース手順
11. ロールバック手順
12. リスクと未解決事項
13. 関連ファイル一覧
14. 承認チェックリスト

---

## 1. 背景とユーザー要望

### 1.1 現象
- Scratchをブラウザで開いて Minecraft と接続している状態（WebSocket接続中）。
- この状態で Minecraft の **ワールドを閉じる**、または **別ワールドを開く**、**タイトル画面に戻る** などの操作を行うと、Minecraft 本体がクラッシュすることがある。
- クラッシュ後はゲーム再起動が必要となり、未保存の建築物・スクリプト操作が失われるリスクがある。

### 1.2 ユーザーの要望（原文要約）
> マイクラのワールドを閉じるときに自動的に切断、ワールドを起動したら、手動で接続して、クラッシュしないように起動できるようにしたい。

要件をより明確に分解すると以下の通り。

| ID | 要件 | 受け入れ条件 |
|----|------|------------|
| R1 | ワールドを閉じる際、Scratch ↔ Minecraft間の接続が**自動的に切断**される | ワールドを閉じた瞬間、Scratch 側が即座に「未接続」状態になる |
| R2 | ワールドを閉じる際、**Minecraft がクラッシュしない** | 100回以上の開閉操作でクラッシュ0件 |
| R3 | 新しいワールド（または同じワールド）を開いた後、**手動で「接続」ブロック**を実行すると問題なく接続できる | ポート占有や残存スレッドによる失敗が発生しない |
| R4 | ワールドを閉じた後にScratch側からコマンドが送られた場合も、**Minecraft 側は安全に拒否**する | クラッシュせずエラーレスポンスを返す（または無視） |
| R5 | クラッシュ防止のため、**実装は最小限の変更**で安全側に倒す（小学生向け教育ツールであることを尊重） | 既存のコマンドブロックの動作は変えない |

### 1.3 対象ユーザー
- 小学生（プログラミング初心者）
- そのため、**接続/切断の難しい操作はユーザー側に求めない**。MOD 側で安全弁を効かせる方針が望ましい。

---

## 2. 期待される正常動作（仕様）

```
[シングルプレイヤーの典型的なフロー]

(1) Minecraft 起動 → タイトル画面
    - WebSocketサーバー: 未起動

(2) ワールドA を開く
    - ServerStartingEvent 発火
    - WebSocketサーバー: 起動 (port 14711 listen)
    - Scratch側: 「接続」ブロックを実行 → 接続成功

(3) ユーザーがブロック配置などを行う
    - 通常通り動作

(4) ユーザーが「ワールドを保存して終了」などでタイトルに戻る
    - ServerStoppingEvent 発火
    - 【新規】MOD: 全クライアントに WebSocket Close フレーム送信
    - 【新規】MOD: 一定時間でハンドラスレッド完了を待機
    - 【新規】MOD: 以降のコマンド受信は serverStopping フラグで全拒否
    - 【新規】MOD: clearArea などの再帰タスクは中断
    - WebSocketサーバー: 停止 (port 14711 解放)
    - Scratch側: onclose を受信し、内部状態を完全リセット
    - Scratch側: ステータスインジケータが「未接続」になる

(5) 同ユーザーが ワールドB を開く（または ワールドA を再オープン）
    - ServerStartingEvent 発火
    - WebSocketサーバー: 起動 (port 14711 listen) ← ポート占有エラーが起きないこと
    - Scratch側: ユーザーが手動で「接続」ブロックを実行 → 接続成功

(6) ステップ (3) と同様
```

---

## 3. 現状コードの完全調査

### 3.1 MOD 側

#### 3.1.1 `MinecraftEduMod.java` (133行)

**ライフサイクルイベント:**
- `onServerStarting` (52-69行) - ServerStartingEvent
  - `commandExecutor = new CommandExecutor(server);`
  - `webSocketServer = new SimpleWebSocketServer(14711, server, commandExecutor);`
  - `webSocketServer.start();`
- `onServerStopping` (71-83行) - ServerStoppingEvent
  - `webSocketServer.stop();` のみ
  - **CommandExecutor へは何も通知していない**
  - **Scratch クライアントへは何も通知していない**
  - **スレッド完了待機なし**

**問題点:** ServerStoppingEvent は **ワールドのアンロード前** に発火するが、`webSocketServer.stop()` だけでは並行実行中のコマンドが止まらない。

#### 3.1.2 `SimpleWebSocketServer.java` (259行)

**接続受付ループ** (40-52行):
```java
executor.submit(() -> {
    while (running) {
        try {
            Socket client = serverSocket.accept();
            executor.submit(() -> handleClient(client));
        } catch (IOException e) { ... }
    }
});
```

**ハンドラループ** (94-157行):
```java
while (running && !client.isClosed()) {
    int firstByte = in.read();   // ← ブロッキング read（割り込み不可）
    ...
    if (opcode == 0x1) {
        handleWebSocketMessage(handler, message, out);
    } else if (opcode == 0x8) {
        break; // Close frame
    }
}
```

**stop() メソッド** (245-258行):
```java
public void stop() {
    running = false;
    try {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        if (executor != null) {
            executor.shutdownNow();   // ← 割り込みのみ。awaitTermination なし
        }
        ...
    }
}
```

**問題点（致命的）:**
1. **クライアントへ Close フレームを送らない** → Scratch側は TCP の異常切断扱いになり、検知が遅れる。
2. **`shutdownNow()` のみで `awaitTermination` なし** → ハンドラスレッドが終了する前に `stop()` が return する。
3. **`InputStream.read()` は割り込みで抜けない** → socket.close() でしか脱出しない。実際にはServerSocketのみ閉じてクライアントSocketは閉じていないため、ハンドラスレッドはブロック状態のまま生き残る可能性がある。
4. **`serverSocket = new ServerSocket(port)` で `setReuseAddress(true)` を呼んでいない** → 連続でワールドを開閉した場合、TIME_WAIT 状態のソケットが残り `BindException: Address already in use` でサーバ再起動に失敗するリスク。

#### 3.1.3 `MinecraftWebSocketHandler.java` (225行)

**handleMessage** (24-34行):
- JSONパース → `processMessage` へ委譲
- サーバ停止フラグのチェックなし

**processMessage** (36-55行):
- `connect`/`command`/`query`/`heartbeat` を処理
- どの分岐も停止チェックなし

**handleConnect** (57-95行):
- セッションIDを生成し、ロール `STUDENT_FULL` を返す
- サーバ停止状態でも応答してしまう

**問題点:** ハンドラレベルでサーバ停止を検知しないため、停止中であってもコマンドが `CommandExecutor` まで到達する。

#### 3.1.4 `CommandExecutor.java` (1330行)

**主要観察点:**
- 全コマンドが `server.execute(() -> { ... })` でメインスレッドにタスクをポストする方式
- 例: `executeSetBlock` (162-218行), `executeFillBlocks` (253-324行), `executeClearArea` (790-856行)

**特に問題のあるコマンド:**

##### A. `executeClearArea` + `scheduleChunkedTasks` (790-882行)
```java
private void scheduleChunkedTasks(List<Runnable> tasks, int index, int centerX, int centerZ) {
    if (index >= tasks.size()) {
        clearAreaInProgress = false;
        return;
    }
    server.execute(() -> {
        ...
        tasks.get(index).run();
        scheduleChunkedTasks(tasks, index + 1, centerX, centerZ);
    });
}
```
- 自身を再帰的に `server.execute` する **チェーン型タスク**。
- ワールド停止検知の経路がないため、停止中も次のタスクを `server.execute` に積み続ける。
- `world.setBlock(...)` が **save in progress** 中の世界を触ると save corruption / crash の元。

##### B. `executeGetTargetBlock` (356-415行)
```java
CompletableFuture<Boolean> future = new CompletableFuture<>();
server.execute(() -> { ... future.complete(...); });
boolean success = future.get(5, TimeUnit.SECONDS);
```
- ハンドラスレッドが最大5秒間ブロック。
- ワールド停止中は future が完了しないため必ず5秒経過。その間 stop() を遅延させる。

##### C. `getFirstPlayer()` (1052-1057行)
```java
return server.getPlayerList().getPlayers().get(0);
```
- 停止中に `getPlayerList()` が null や empty を返す可能性。チェック不完全。

##### D. `executeSetBlock` 系 (162-218, 290-306, 等)
- **メインスレッド外** で `server.overworld()` を取得しない（取得は execute ラムダ内）が、ラムダ実行時点で `overworld()` が unloaded だと NPE 経路あり。

#### 3.1.5 ファイル間の参照関係

```
MinecraftEduMod
   │
   ├─ ServerStartingEvent → 生成: CommandExecutor + SimpleWebSocketServer
   │
   └─ ServerStoppingEvent → SimpleWebSocketServer.stop()
                                    │
                                    └─ ServerSocket.close() + executor.shutdownNow()

SimpleWebSocketServer  ─── (per client) ───> MinecraftWebSocketHandler ──> CommandExecutor ──> server.execute(...)
```

### 3.2 Scratch (ブラウザ) 側

#### 3.2.1 対象ファイル（4ファイル同期必須）

| ファイル | 用途 | `Scratch3MinecraftBlocks` 開始行 |
|----|----|----|
| `gui.js` | 通常エディタ | 64006 |
| `blocksonly.js` | ブロックのみ表示 | 66282 |
| `player.js` | プレイヤー（実行のみ） | 66282 |
| `compatibilitytesting.js` | 互換性テスト | 66282 |

ファイル内のメソッドの行番号は gui.js を基準に記載。他3ファイルは ~+2276行のオフセットで同名実装が存在する。

#### 3.2.2 コンストラクタ (64006-64017)

```javascript
class Scratch3MinecraftBlocks {
  constructor(runtime) {
    this.runtime = runtime;
    this.socket = null;
    this.connected = false;
    this.sessionId = null;
    this.pendingRequests = new Map();
    this.requestTimeout = 5000;
    // 録画状態
    this._isRecording = false;
    this._recordingPath = null;
  }
```

**観察:**
- ステータス通知の peripheralExtensions 連携なし（カスタム拡張のため Scratch 標準の接続UIは使えない）。
- ハートビートタイマーなし。

#### 3.2.3 接続ブロック定義 (~64041)

```javascript
{
  opcode: 'disconnect',
  blockType: 'command',
  text: formatMessage({ id: 'minecraft.disconnect', default: 'disconnect', ... })
}
```

#### 3.2.4 `connect(args)` (64871-64928)

```javascript
connect(args) {
    ...
    this._cleanupSocket();
    return new Promise((resolve, reject) => {
      try {
        this.socket = new WebSocket(url);
        this.socket.onopen = () => { this.sendMessage({type:'connect',...}); this.connected = true; resolve(); };
        this.socket.onmessage = event => { ... this.handleMessage(message); };
        this.socket.onerror = error => { this.connected = false; reject(error); };
        this.socket.onclose = () => {
          this.connected = false;
          this.pendingRequests.forEach((pending) => { pending.reject(new Error('Connection closed')); });
          this.pendingRequests.clear();
        };
      } catch (error) { ... }
    });
}
```

**問題点:**
- `onclose` で `_cleanupSocket()` を呼んでいない（部分的なクリーンアップのみ）。
- `sessionId` が残ったまま、`socket` インスタンスも残る。
- onclose 後に「接続」ブロックを再実行すれば `connect` 冒頭の `_cleanupSocket()` で結局リセットされるが、自動切断の体感としては不完全。

#### 3.2.5 `_cleanupSocket()` (64934-64953)

```javascript
_cleanupSocket() {
    if (this.socket) {
      try {
        this.socket.onopen = null;
        this.socket.onmessage = null;
        this.socket.onerror = null;
        this.socket.onclose = null;
        this.socket.close();
      } catch (e) {}
      this.socket = null;
    }
    this.connected = false;
    this.sessionId = null;
    this.pendingRequests.forEach((pending) => { pending.reject(new Error('Connection closed')); });
    this.pendingRequests.clear();
}
```

#### 3.2.6 `disconnect()` (64958-64960)

```javascript
disconnect() { this._cleanupSocket(); }
```

#### 3.2.7 `sendCommand` (65780-65792)

```javascript
sendCommand(action, params) {
    if (!this.connected || !this.socket) {
      console.warn('Not connected to Minecraft server');
      return Promise.reject(new Error('Not connected'));
    }
    return this.sendMessage({...});
}
```

#### 3.2.8 `handleMessage` (65879-65943)

- 既に 2026-04-06 計画書で payload ガード修正済み（要確認）。

---

## 4. クラッシュ・ハングのシナリオ分析

### 4.1 シナリオA: ワールド閉鎖中にコマンド到着

```
時刻 t0: ユーザーがワールドを閉じる
時刻 t1: ServerStoppingEvent 発火
        → MinecraftEduMod.onServerStopping 実行
        → SimpleWebSocketServer.stop()
        → ServerSocket.close() / executor.shutdownNow()
        → onServerStopping return（数ms）
時刻 t2: メインスレッドがワールド保存処理を開始
時刻 t2.1: ハンドラスレッドはまだ生存。Scratch 側が直前に送った setBlock を受信
時刻 t2.2: handleClient → handleWebSocketMessage → handler.handleMessage
時刻 t2.3: CommandExecutor.executeSetBlock → server.execute(() -> { world.setBlock(...) })
時刻 t3: メインスレッドがワールド保存中に setBlock タスクを実行 → 中途半端なワールド状態に書き込み
時刻 t4: → NullPointerException / IllegalStateException / save corruption / クラッシュ
```

### 4.2 シナリオB: clearArea 実行中にワールドを閉じる

```
時刻 t0: ユーザーが「周囲をクリア」ブロックを実行
時刻 t0.1: clearAreaInProgress = true
時刻 t0.2: scheduleChunkedTasks(tasks, 0, ...) → server.execute(...)
時刻 t1: ユーザーがワールドを閉じる（クリア中）
時刻 t1.1: ServerStoppingEvent 発火
時刻 t2: SimpleWebSocketServer.stop() 完了
時刻 t3: メインスレッドがワールド保存処理を開始
時刻 t4: server.execute キューに残っている scheduleChunkedTasks の次タスクが実行
        → world.setBlock(...) が unloaded world に対して呼ばれる
時刻 t5: → クラッシュ
```

### 4.3 シナリオC: 連続ワールド開閉でポート占有

```
時刻 t0: ワールドA を閉じる → SimpleWebSocketServer.stop()
時刻 t1: ワールドB を開く → SimpleWebSocketServer.start()
        → new ServerSocket(14711) （SO_REUSEADDR 未設定）
時刻 t2: → BindException: Address already in use
時刻 t3: WebSocketサーバー起動失敗。Scratchから接続不可能。
時刻 t4: ユーザーが「接続」ブロックを押しても無反応 → Minecraft再起動が必要
```

### 4.4 シナリオD: getTargetBlock 実行中の停止

```
時刻 t0: Scratch が getTargetBlock を送信
時刻 t1: ハンドラスレッドが server.execute → future.get(5s) でブロック
時刻 t2: ユーザーがワールドを閉じる
時刻 t3: メインスレッド停止
時刻 t4: future が完了しない → 5秒間ハンドラスレッドが滞留
時刻 t5: stop() の shutdownNow() でも InputStream.read() がブロックしているので解放されない
```

---

## 5. 根本原因の整理

| # | 原因 | 影響度 | 対象ファイル |
|---|------|------|-----|
| **C1** | サーバ停止中のコマンド実行をブロックする仕組みがない | **致命** | `CommandExecutor.java`, `MinecraftWebSocketHandler.java` |
| **C1'** | `server.execute()` でキューイング後に停止状態へ遷移するレースが防げない【★Round 1】 | **致命** | `CommandExecutor.java` (18箇所) |
| **C2** | クライアントへ Close フレームを送らない | 高 | `SimpleWebSocketServer.java` |
| **C3** | ハンドラスレッドの完了を待たずに stop() が return | 高 | `SimpleWebSocketServer.java` |
| **C4** | clearArea の再帰タスクに停止検知経路がない | 高 | `CommandExecutor.java` |
| **C5** | ServerSocket に SO_REUSEADDR が未設定 | 中 | `SimpleWebSocketServer.java` |
| **C6** | Scratch 側 onclose で完全クリーンアップを呼ばない | 低 | `gui.js` (4ファイル) |
| **C7** | `MinecraftEduMod` のフィールドに `volatile` がなく、複数スレッドからの参照で stale な値を見るリスク【★Round 1】 | 低 | `MinecraftEduMod.java` |
| **C8** | `sendTextFrame` と `sendCloseToAllClients` が同一 OutputStream に並行書き込みし得る【★Round 1】 | 中 | `SimpleWebSocketServer.java` |

---

## 6. 修正方針サマリ

### 6.1 設計原則
1. **MOD 側を防御の主役にする** — 小学生が誤操作してもクラッシュしないこと。
2. **Scratch 側は最小変更** — 既存のブロックの動作・見た目を変えない。
3. **段階的に投入できる構成** — Phase 1 が単独で投入可能、Phase 2 以降で補強。
4. **ロールバック容易性** — 各フェーズは独立してリバート可能。
5. **既存テスト計画と矛盾しない** — 2026-04-06 のクリティカル修正、payloadガード修正と整合。

### 6.2 修正フェーズ

| Phase | 対象 | 目的 | 必須/任意 |
|-------|------|------|---------|
| **Phase 1** | `CommandExecutor.java` | 停止フラグ + `safeExecute` ヘルパーで全コマンド拒否 | **必須** |
| **Phase 2** | `MinecraftEduMod.java` | `onServerStopping` から `CommandExecutor.shutdown()` を先に呼ぶ + フィールド `volatile` 化 | **必須** |
| **Phase 2.5**【★Round 2】 | `MinecraftWebSocketHandler.java` | `processMessage()` への defense-in-depth チェック | **必須** |
| **Phase 3** | `SimpleWebSocketServer.java` | Close フレーム送信 + awaitTermination + SO_REUSEADDR + 同期化 + idempotent stop | **必須** |
| **Phase 4** | `CommandExecutor.java` | `clearArea` の停止対応 (Phase 1 の `safeExecute` で大半カバー) | **必須** |
| **Phase 5** | `gui.js` ほか3ファイル | `onclose` で `_cleanupSocket()` を呼んで完全クリーンアップ | 推奨 |
| **Phase 6** | `gui.js` ほか3ファイル | （任意）切断時のチャット風通知ブロック追加 | 任意 |
| **Phase 7** | テスト・ドキュメント更新 | CLAUDE.md / docs 更新 | **必須** |

Phase 1〜4 だけで R1〜R4 をほぼ満たせる。Phase 5 は UX 改善。

---

## 7. 詳細実装計画（フェーズ別）

### Phase 1: CommandExecutor に停止フラグと `safeExecute` ヘルパーを導入

**目的:**
- サーバ停止中はコマンドを完全に実行しない（入口チェック）。
- `server.execute()` でキューに積まれたタスクが、後から実行される時点で停止していた場合も安全に no-op にする（**ラムダ内部の二重チェック**）。

**変更ファイル:** `minecraft-mod/src/main/java/com/github/minecraftedu/commands/CommandExecutor.java`

**【Round 1 で追加】設計上の重要ポイント:**

`server.execute()` は **メインスレッドへタスクをキューイング** するだけで、タスク実行時点では `serverStopping` が変化している可能性がある。したがって以下の **2段階チェック** が必要:

1. `execute()` 入口（ハンドラスレッド側）: 新規コマンド受理を即座に拒否
2. `server.execute()` ラムダ内部（メインスレッド側）: キュー内タスクを no-op 化

`CommandExecutor.java` には **`server.execute(...)` が18箇所** ある。すべてを個別修正するのは保守性が悪いため、**`safeExecute(Runnable)` ヘルパーを導入し、18箇所すべてを置換** する。

**【Round 3 で追記】Forge MinecraftServer の tick タイミング理解:**

`ServerStoppingEvent` は MC サーバのメインループ内の停止処理ステップで fire される。fire 後の挙動:

```
[Main Loop]
   ↓
ServerStoppingEvent fire (← 我々の onServerStopping() が実行される)
   ↓
全レベル保存処理 (1〜数 tick かかる)
   ↓
レベルクローズ
   ↓
ServerStoppedEvent fire
   ↓
[Main Loop 終了]
```

**重要な観察:**
- `ServerStoppingEvent` の処理中に `commandExecutor.shutdown()` で `serverStopping = true` をセット
- その後 MC は数 tick の保存処理を行う。この間 **`server.execute()` でキュー済みのタスクは順次処理される**
- したがって、ラムダ内チェックがないと、保存処理中に `world.setBlock()` が走る → save corruption
- `safeExecute` のラムダ内チェックが、この window 内の各タスク実行時に発動して no-op 化する
- 保存処理完了後はメインループが終了し、未処理タスクは捨てられる（実行されない）

**変更内容:**

#### (1-1) フィールド追加（49行付近）
```java
// clearArea重複実行防止フラグ
private volatile boolean clearAreaInProgress = false;

// 【新規】サーバ停止フラグ。trueの間は全コマンドを拒否する
// volatile 必須: ハンドラスレッドが書き込み or 読み出し / メインスレッドが読み出し
private volatile boolean serverStopping = false;
```

#### (1-2) `shutdown()` / `isServerStopping()` メソッド追加（クラス末尾）
```java
/**
 * サーバ停止時の後処理
 * このフラグが立った後、execute() は全てnullを返し、
 * 進行中のclearAreaも次タスク投入を停止する。
 * server.execute() でキューイングされた既存タスクも safeExecute 経由なら no-op になる。
 */
public void shutdown() {
    this.serverStopping = true;
    this.clearAreaInProgress = false;

    // 録画中であれば強制停止（任意・推奨）
    if (isRecording && ffmpegProcess != null) {
        try { ffmpegProcess.destroyForcibly(); } catch (Exception ignored) {}
        isRecording = false;
        ffmpegProcess = null;
        currentRecordingPath = null;
    }
    MinecraftEduMod.LOGGER.info("CommandExecutor: shutdown signaled");
}

public boolean isServerStopping() {
    return this.serverStopping;
}
```

#### (1-3) `safeExecute()` ヘルパーメソッド追加（クラス末尾）【★Round 1 新規】
```java
/**
 * server.execute() の安全ラッパー
 *
 * 1. 入口でserverStopping をチェック → trueなら何もしない
 * 2. server.isRunning() でForge公式APIも併せてチェック
 * 3. ラムダ内部でも再度 serverStopping / isRunning() をチェック
 *    （キューに積まれてから実行までの間に停止状態に遷移する可能性があるため）
 * 4. ラムダ内例外を捕捉してログ出力（メインスレッドに例外を漏らさない）
 */
private void safeExecute(Runnable task) {
    if (serverStopping || !server.isRunning()) {
        return;
    }
    server.execute(() -> {
        // メインスレッド時点での再チェック
        if (serverStopping || !server.isRunning()) {
            return;
        }
        try {
            task.run();
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("safeExecute task error", e);
        }
    });
}
```

#### (1-4) execute() の冒頭にガード追加（64行）
```java
public JsonObject execute(String action, JsonObject params) {
    // 【新規】サーバ停止中は何も実行しない
    if (serverStopping || !server.isRunning()) {
        MinecraftEduMod.LOGGER.warn("Command rejected (server stopping): " + action);
        return null;
    }
    try {
        switch (action) {
            ...
        }
    }
}
```

**理由:**
- `volatile` で書き込みの可視性を保証。
- `null` を返すことで、既存の `MinecraftWebSocketHandler.handleCommand` の `success = (result != null)` 経路に乗り、自然にエラーレスポンスが返る。
- `server.isRunning()` 併用により、Forgeイベント発火前に停止が始まった場合（例: Minecraftのクラッシュ復旧パス）にも防御。

#### (1-5) 全 `server.execute()` を `safeExecute()` に置換【★Round 1 新規】

**置換対象（合計18箇所）:**

| # | メソッド | 行番号 |
|---|---|---|
| 1 | `executeChat` | 152 |
| 2 | `executeSetBlock` | 201 |
| 3 | `executeFillBlocks` | 291 |
| 4 | `executeGetTargetBlock` | 371 |
| 5 | `executeSummonEntity` | 523 |
| 6 | `executeTeleport` | 548 |
| 7 | `executeSetWeather` | 559 |
| 8 | `executeSetTime` | 585 |
| 9 | `executeSetGameMode` | 625 |
| 10 | `executeSetGameRule` | 662 |
| 11 | `scheduleChunkedTasks` | 868 |
| 12 | `executeClearAllEntities` | 893 |
| 13 | `executeSetMoveSpeed` | 955 |
| 14 | `executeSetNightVision` | 985 |
| 15 | `executeSetFlySpeed` | 1033 |
| 16 | `sendRawChatMessage` | 1067 |
| 17 | `sendChatMessage` | 1078 |
| 18 | `executeSetContainerItem` | 1293 |

**置換方針:**
```java
// Before
server.execute(() -> {
    ServerLevel world = server.overworld();
    world.setBlock(pos, blockState, 3);
});

// After
safeExecute(() -> {
    ServerLevel world = server.overworld();
    if (world == null) return;  // ワールド未ロード時の保険
    world.setBlock(pos, blockState, 3);
});
```

**注意点:**
- `executeGetTargetBlock` は `CompletableFuture` の `complete()` を伴うため、`safeExecute` 内で `future.complete(false)` を確実に呼ぶ必要あり（さもないと `future.get(5s)` が必ずタイムアウトする）。
- `safeExecute` 内例外時は catch ブロックで logger 記録するため、future 非完了経路は `executeGetTargetBlock` 側で **タイムアウト＝失敗** として扱う既存ロジックでカバー可能。

**`executeGetTargetBlock` のラップ例外対応案:**
```java
final CompletableFuture<Boolean> future = new CompletableFuture<>();
safeExecute(() -> {
    try {
        // ... 既存処理
        future.complete(true);
    } catch (Exception e) {
        MinecraftEduMod.LOGGER.error("Error in getTargetBlock raycast", e);
        future.complete(false);
    }
});
// safeExecute がserverStopping等で no-op になった場合、futureが完了しないので
// 既存のtimeout経路で5秒後に「失敗」となる（クラッシュにはつながらない）
try {
    boolean success = future.get(5, TimeUnit.SECONDS);
    return success ? result : null;
} catch (Exception e) {
    return null;
}
```

> 補足: より安全にするなら `safeExecute` を返り値ありに拡張し、no-op 時に `future.complete(false)` を即座に呼ぶ実装も可能だが、現状の5秒タイムアウトで実害がないので採用しない。

#### (1-6) `getFirstPlayer()` の null 安全性強化（1052行）
```java
private ServerPlayer getFirstPlayer() {
    if (server == null || !server.isRunning()) return null;
    try {
        var playerList = server.getPlayerList();
        if (playerList == null) return null;
        var players = playerList.getPlayers();
        if (players == null || players.isEmpty()) return null;
        return players.get(0);
    } catch (Exception e) {
        MinecraftEduMod.LOGGER.warn("getFirstPlayer failed: " + e.getMessage());
        return null;
    }
}
```

---

### Phase 2: MinecraftEduMod から shutdown を呼ぶ + フィールド可視性確保

**変更ファイル:** `minecraft-mod/src/main/java/com/github/minecraftedu/MinecraftEduMod.java`

#### (2-1) フィールドに `volatile` を追加【★Round 1 新規】

```java
// Before
private SimpleWebSocketServer webSocketServer;
private CommandExecutor commandExecutor;

// After
private volatile SimpleWebSocketServer webSocketServer;
private volatile CommandExecutor commandExecutor;
```

**理由:**
- `onServerStarting` / `onServerStopping` はForgeイベントスレッドで実行される。
- `onRegisterCommands` で登録した `/entityspawn` スラッシュコマンドのハンドラは別スレッド（コマンドディスパッチャスレッド）から `commandExecutor` を参照する。
- スレッド間で書き込み/読み出しが発生するため、`volatile` で可視性を保証する必要がある。

#### (2-2) `onServerStopping` の改修

```java
@SubscribeEvent
public void onServerStopping(ServerStoppingEvent event) {
    LOGGER.info("MinecraftEdu server stopping...");

    // 【新規】最初に CommandExecutor を停止状態にし、新規コマンドをブロック
    // ローカル変数にコピーしてからnullチェック → スレッドセーフ
    final CommandExecutor ce = this.commandExecutor;
    if (ce != null) {
        try {
            ce.shutdown();
        } catch (Exception e) {
            LOGGER.error("Error shutting down CommandExecutor", e);
        }
    }

    final SimpleWebSocketServer ws = this.webSocketServer;
    if (ws != null) {
        try {
            ws.stop();
            LOGGER.info("WebSocket server stopped");
        } catch (Exception e) {
            LOGGER.error("Error stopping WebSocket server", e);
        }
    }

    // 【新規】次回 ServerStartingEvent で確実に新規生成されるよう参照をクリア
    this.webSocketServer = null;
    this.commandExecutor = null;
}
```

**順序の理由:**
1. 先に `commandExecutor.shutdown()` で「以後コマンド実行禁止」を宣言
2. その後 WebSocketサーバを停止 → ハンドラスレッドが残っていてもコマンドは実行されない
3. 最後に参照を null にクリア → 次回 ServerStartingEvent で確実に新規生成される

**ローカル変数コピーの理由:**
- nullチェック後にメンバ変数を再度参照するとレース条件が発生する可能性
- ローカル変数にスナップショットしてから操作することで、参照のアトミック性を保証

#### (2-3) `/entityspawn` スラッシュコマンド内も同様に修正【★Round 2 新規】

**Before（onRegisterCommands 内、95-130行付近）:**
```java
.executes(context -> {
    if (commandExecutor != null) {
        commandExecutor.setEntitySpawningAllowed(true);
        ...
    }
    return 1;
})
```

**After:**
```java
.executes(context -> {
    final CommandExecutor ce = this.commandExecutor;  // ローカルスナップショット
    if (ce != null) {
        ce.setEntitySpawningAllowed(true);
        context.getSource().sendSuccess(...);
    }
    return 1;
})
```

**理由:**
- スラッシュコマンドはコマンドディスパッチャスレッドで実行される（イベントスレッドとは別）
- フィールドを直接参照すると、null チェック後にメインスレッドが null 化する可能性
- ローカル変数にコピーすればその後の参照は安全

`status` / `deny` / `allow` の3箇所すべてに同じパターンを適用する。

---

### Phase 2.5: MinecraftWebSocketHandler への defense-in-depth【★Round 2 新規】

**変更ファイル:** `minecraft-mod/src/main/java/com/github/minecraftedu/network/MinecraftWebSocketHandler.java`

**目的:** `handleConnect` / `handleHeartbeat` は `CommandExecutor` を経由しないため、Phase 1 の serverStopping チェックでは捕捉されない。停止中に新しい WebSocket 接続が来た場合、現状では `handleConnect` がそのまま `STUDENT_FULL` ロールを返してしまう。

#### (2.5-1) `processMessage` の冒頭に停止チェックを追加

**Before（36行）:**
```java
private String processMessage(JsonObject message) {
    String type = message.get("type").getAsString();

    switch (type) {
        case "connect":
            return handleConnect(message);
        ...
    }
}
```

**After:**
```java
private String processMessage(JsonObject message) {
    // 【新規】サーバ停止中は heartbeat 以外を全て拒否
    // heartbeat だけは許可: クライアント側のキープアライブを破壊しないため
    if (commandExecutor != null && commandExecutor.isServerStopping()) {
        String type = message.has("type") ? message.get("type").getAsString() : "unknown";
        if (!"heartbeat".equals(type)) {
            return createError("SERVER_STOPPING",
                "Server is stopping, please reconnect after world reload");
        }
    }

    String type = message.get("type").getAsString();
    switch (type) {
        ...
    }
}
```

**設計判断:**
- `heartbeat` は許可: クライアントがハートビートを送るとサーバから応答を返すが、サーバ停止中は応答しない方がよい場合もある。本実装では「heartbeatはエラーを返さず無害な応答（serverTime: 0など）を返す」のが理想。最小実装では heartbeat も拒否でOK。
- それ以外（connect / command / query）は全て `SERVER_STOPPING` エラーを返す
- Scratch 側は `error` メッセージを既存の `handleMessage` で受信する（既存実装で対応済み）

---

### Phase 3: SimpleWebSocketServer の安全停止対応

**変更ファイル:** `minecraft-mod/src/main/java/com/github/minecraftedu/network/SimpleWebSocketServer.java`

**【Round 1 で確定した実装方針】**
- 追跡対象は **`Set<Socket>` のみ**（OutputStream は追跡しない）。書き込み時に `socket.getOutputStream()` で取得する（同一Socketに対して何度呼んでも同じ参照が返る）。
- `sendCloseToAllClients()` と `sendTextFrame()` の競合は **Socket単位の同期オブジェクト** で直列化する。
- `try-with-resources` から `OutputStream out` を **取り出し**、明示的な finally で close する形に書き換える（scopeとセット追跡を両立）。

#### (3-1) フィールド追加

```java
public class SimpleWebSocketServer {
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private final int port;
    private final MinecraftServer minecraftServer;
    private final CommandExecutor commandExecutor;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;

    // 【新規】アクティブ接続追跡（停止時に明示的にcloseするため）
    private final java.util.Set<Socket> activeClients =
        java.util.Collections.synchronizedSet(new java.util.HashSet<>());
```

#### (3-2) start() で SO_REUSEADDR を設定

**Before:**
```java
public void start() throws IOException {
    serverSocket = new ServerSocket(port);
    running = true;
    ...
}
```

**After:**
```java
public void start() throws IOException {
    serverSocket = new ServerSocket();
    serverSocket.setReuseAddress(true);  // 【新規】TIME_WAIT回避
    serverSocket.bind(new java.net.InetSocketAddress(port));
    running = true;
    ...
}
```

#### (3-3) handleClient の改修（接続追跡 + 同期書き込み）

**Before（55-169行）:**
```java
private void handleClient(Socket client) {
    MinecraftWebSocketHandler handler = new MinecraftWebSocketHandler(minecraftServer, commandExecutor);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
         OutputStream out = client.getOutputStream()) {
        ...
        // ループ内で sendTextFrame(out, response)
    } catch (Exception e) { ... }
    finally {
        try { client.close(); } catch (IOException e) { ... }
    }
}
```

**After:**
```java
private void handleClient(Socket client) {
    activeClients.add(client);
    MinecraftWebSocketHandler handler = new MinecraftWebSocketHandler(minecraftServer, commandExecutor);

    BufferedReader reader = null;
    OutputStream out = null;
    try {
        reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = client.getOutputStream();

        // === Handshakeフェーズ（既存のロジック） ===
        // ...

        // === メッセージループ（既存のロジック） ===
        // 既存の sendTextFrame(out, response) の呼び出しは変更不要
        //   → sendTextFrame 内部で synchronized(out) を使用するため
        // ...
    } catch (Exception e) {
        MinecraftEduMod.LOGGER.error("Error handling client", e);
    } finally {
        activeClients.remove(client);
        try { if (reader != null) reader.close(); } catch (IOException ignored) {}
        try { client.close(); } catch (IOException e) {
            MinecraftEduMod.LOGGER.error("Error closing client", e);
        }
        MinecraftEduMod.LOGGER.info("Client disconnected");
    }
}
```

**ポイント:**
- `try-with-resources` を使わず、明示的な変数宣言と finally で管理
- `reader` を閉じれば内部の InputStream も閉じる（chain close）
- `out` は閉じない（client.close() で一緒に閉じる。明示的にclose()するとSocketも閉じてしまう実装系がある）
- `activeClients.remove(client)` を finally の冒頭に置き、必ず除去されるようにする

#### (3-4) sendTextFrame の同期化【★Round 1 新規】

**Before（196-222行）:**
```java
private void sendTextFrame(OutputStream out, String text) throws IOException {
    byte[] payload = text.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream frame = new ByteArrayOutputStream();
    frame.write(0x81);
    ...
    out.write(frame.toByteArray());
    out.flush();
}
```

**After:**
```java
private void sendTextFrame(OutputStream out, String text) throws IOException {
    byte[] payload = text.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream frame = new ByteArrayOutputStream();
    frame.write(0x81);
    ...
    byte[] bytes = frame.toByteArray();
    // 【新規】sendCloseToAllClients との競合を防ぐため OutputStream で同期
    synchronized (out) {
        out.write(bytes);
        out.flush();
    }
}
```

`sendPong` 内の write も同様に `synchronized (out)` でラップする。

#### (3-5) Close フレーム送信メソッド追加

```java
/**
 * 全クライアントへ正常クローズ通知を送信
 * RFC 6455: opcode 0x8 + status code 1001 (Going Away)
 *
 * 注: sendTextFrame と同じ OutputStream への書き込みになるため、
 *     OutputStream で synchronized することで sendTextFrame との
 *     バイト混在を防ぐ。
 */
private void sendCloseToAllClients() {
    byte[] closeFrame = new byte[] {
        (byte) 0x88,              // FIN + Close opcode
        (byte) 0x02,              // payload length 2
        (byte) 0x03, (byte) 0xE9  // status code 1001 (Going Away)
    };
    // activeClients のスナップショットを取り、その上でループ
    // （iteration中の他スレッドからのremoveを避けるため）
    Socket[] snapshot;
    synchronized (activeClients) {
        snapshot = activeClients.toArray(new Socket[0]);
    }
    for (Socket s : snapshot) {
        try {
            if (!s.isClosed()) {
                OutputStream os = s.getOutputStream();
                synchronized (os) {  // sendTextFrame と同じロックで直列化
                    os.write(closeFrame);
                    os.flush();
                }
            }
        } catch (IOException ignored) {
            // 既に切れている可能性 → 無視
        }
    }
}
```

#### (3-6) stop() の改修（idempotent + 全段階）

```java
public void stop() {
    // 【新規・Round 2】二重呼び出しガード（idempotent）
    if (!running) {
        MinecraftEduMod.LOGGER.debug("WebSocket server stop() called while not running");
        return;
    }
    running = false;
    try {
        // 【新規】(1) 全クライアントへ Close フレーム送信
        try { sendCloseToAllClients(); } catch (Exception ignored) {}

        // 【新規】(2) 各クライアントソケットを明示的に閉じる
        //         （InputStream.read() のブロックを解除するため）
        Socket[] snapshot;
        synchronized (activeClients) {
            snapshot = activeClients.toArray(new Socket[0]);
        }
        for (Socket s : snapshot) {
            try { s.close(); } catch (IOException ignored) {}
        }

        // (3) ServerSocket を閉じる
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        // 【改修】(4) executor を shutdown → 待機 → 必要なら shutdownNow
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                        MinecraftEduMod.LOGGER.warn("Some handler threads did not terminate cleanly");
                    }
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        MinecraftEduMod.LOGGER.info("WebSocket server stopped");
    } catch (IOException e) {
        MinecraftEduMod.LOGGER.error("Error stopping server", e);
    }
}
```

**ポイント:**
- 順序が重要: `Close フレーム送信` → `クライアントソケット close（read解除）` → `ServerSocket close` → `executor待機`
- スナップショットを取ってから iterate することで `ConcurrentModificationException` を回避
- `awaitTermination(2 + 1秒)` で最大 3秒の待機
- Forge側の ServerStoppingEvent はサーバ停止前段階で発火するため、3秒程度の遅延は許容範囲

**【Round 3 で追記】各ステップの意味論的根拠:**

| ステップ | 目的 | これがないとどうなるか |
|---|----|---|
| 1. Close frame 送信 | Scratch 側の `onclose` を `code=1001 (Going Away)` で発火させる | Scratch 側は `code=1006 (abnormal)` を受信。動作上は同じだが、デバッグ時に「正常な切断」か「異常切断」かの区別ができない |
| 2. 各 client socket close | ハンドラスレッドの `InputStream.read()` ブロックを解除する | ハンドラスレッドが永久にブロック。`awaitTermination` がタイムアウトし、`shutdownNow()` も `read()` を解除できないので解放されない |
| 3. ServerSocket close | accept() ループの終了 | accept ループが残り、新規接続を受け付け続ける |
| 4. executor.shutdown() + awaitTermination | ハンドラスレッドの完了確認 | 並行実行の最終クリーンアップが保証されない |
| 5. shutdownNow() (フォールバック) | 停止しないスレッドを interrupt で叩く | `Future.get(5s)` で待っているスレッドを2秒で叩き起こす |

**Future.get() と shutdownNow() の相互作用:**

`executeGetTargetBlock` 内で `future.get(5, TimeUnit.SECONDS)` がブロックされている場合:
1. `safeExecute` が no-op になり future が完了しない
2. `awaitTermination(2s)` が満了
3. `shutdownNow()` が全ハンドラスレッドに `Thread.interrupt()` を投げる
4. `Future.get()` は InterruptedException を投げる（Java 仕様）
5. ハンドラの try/catch で捕捉 → `MinecraftEduMod.LOGGER.warn(...)` → return null
6. ハンドラスレッド終了
7. 次の `awaitTermination(1s)` でこの終了が確認される

**結論:** ハンドラスレッドが `Future.get()` でブロックしていても、最大 3秒で確実に終了する。

**`SimpleWebSocketServer.commandExecutor` の `final` 性による安全性保証:**

```java
public class SimpleWebSocketServer {
    private final CommandExecutor commandExecutor;  // 構築時にのみセット、再代入不可

    public SimpleWebSocketServer(int port, MinecraftServer server, CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;  // ここで参照を保持
    }
}
```

- `MinecraftEduMod.commandExecutor` を null 化しても、`SimpleWebSocketServer` 内部の参照は不変
- 既存ハンドラスレッドが `this.commandExecutor.execute(...)` を呼んでも NPE しない
- ハンドラは `serverStopping=true` のおかげで null を返し、安全に終了

#### (3-7) 停止時のログノイズ抑制【★Round 3 新規】

`handleClient` の catch ブロックで、停止中に発生する SocketException は想定内のため、エラーレベルではなく info / debug にする:

```java
} catch (Exception e) {
    if (running) {
        // 想定外のエラー
        MinecraftEduMod.LOGGER.error("Error handling client", e);
    } else {
        // 停止中の想定内エラー
        MinecraftEduMod.LOGGER.debug("Client connection closed during shutdown: " + e.getMessage());
    }
} finally {
    ...
}
```

---

### Phase 4: CommandExecutor の clearArea を停止対応

**変更ファイル:** `minecraft-mod/src/main/java/com/github/minecraftedu/commands/CommandExecutor.java`

#### (4-1) `scheduleChunkedTasks` の冒頭にガード追加（861行）

Phase 1 で `safeExecute` を導入したあと、`scheduleChunkedTasks` の `server.execute` も `safeExecute` に置換されているため、ラムダ内部のチェックは `safeExecute` 側で自動処理される。ただしリスト境界チェック（既存ロジック）の前後に `serverStopping` チェックを追加する。

```java
private void scheduleChunkedTasks(List<Runnable> tasks, int index, int centerX, int centerZ) {
    // 【新規】サーバ停止中は中断
    if (serverStopping || !server.isRunning()) {
        clearAreaInProgress = false;
        MinecraftEduMod.LOGGER.info("clearArea aborted: server stopping (index=" + index + "/" + tasks.size() + ")");
        return;
    }
    if (index >= tasks.size()) {
        clearAreaInProgress = false;
        sendChatMessage("§a周囲クリア完了: 中心(" + centerX + ", " + centerZ + ")");
        MinecraftEduMod.LOGGER.info("チャンク分割タスク完了: " + tasks.size() + "チャンク処理済み");
        return;
    }
    safeExecute(() -> {
        // safeExecute 内部でも serverStopping チェック済み
        // 25%ごとに進捗をチャットに表示
        int quarter = tasks.size() / 4;
        if (quarter > 0 && index > 0 && index % quarter == 0) {
            int percent = (index * 100) / tasks.size();
            sendChatMessage("§a周囲クリア進行中... " + percent + "%");
        }
        try {
            tasks.get(index).run();
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("チャンク処理エラー (index=" + index + "): " + e.getMessage());
        }
        scheduleChunkedTasks(tasks, index + 1, centerX, centerZ);
    });
}
```

**重要:** `scheduleChunkedTasks` 内で次タスクをスケジュールする再帰呼び出しはラムダ内部にあるため、`safeExecute` の no-op 化により次タスクの投入が止まる。**追加の再帰中断ロジックは不要**。

#### (4-2) clearArea 内のタスクラムダ最適化

`tasks.add(() -> { ... })` で生成される個別ラムダ（815-842行）は `safeExecute` を経由して実行される。**ラムダ内で `world = server.overworld()` を取得しているが、これが null になる可能性に対処する必要がある**:

```java
tasks.add(() -> {
    ServerLevel world = server.overworld();
    if (world == null) return;  // 【新規】null guard
    BlockState bedrock = ...;
    ...
});
```

`safeExecute` でラムダ全体を try/catch しているため、null参照によるNPEは捕捉されるが、例外ログが大量に出るのを避けるため明示的にチェックする方が望ましい。

#### (4-3) `executeGetTargetBlock` のタイムアウト【★Round 1 で方針変更】

**結論: 変更しない（5秒のまま据え置き）**

理由:
- 2秒に短縮すると、サーバが clearArea で重い処理中の場合に正常クエリが失敗するリスク
- 停止時の遅延短縮効果は限定的（最大5秒の遅延でも実用上問題なし）
- `safeExecute` の no-op 化により、停止後の `getTargetBlock` は future が完了せずタイムアウトするが、これはハンドラスレッドが5秒滞留する以外の害がない
- 待機中もハンドラスレッドが滞留するだけで、Minecraft本体に影響はない

> 将来課題U5: ハンドラスレッド数に上限を設け、滞留が増えても健全性を維持する仕組みを検討。

#### (4-4) 録画プロセスの停止連動

Phase 1 (1-2) の `shutdown()` メソッドに含めるため、Phase 4 では追加作業なし。

---

### Phase 5: Scratch 側 onclose の完全クリーンアップ【★Round 2 で簡素化】

**変更ファイル（4ファイル全て）:**
- `gui.js`
- `blocksonly.js`
- `player.js`
- `compatibilitytesting.js`

#### (5-1) `connect` 内の onclose を改修 — `_cleanupSocket()` を直接呼ぶ

**Before (gui.js: 64913-64921):**
```javascript
this.socket.onclose = () => {
  console.log('Disconnected from Minecraft server');
  this.connected = false;
  // 切断時にpendingRequestsをクリア
  this.pendingRequests.forEach((pending) => {
    pending.reject(new Error('Connection closed'));
  });
  this.pendingRequests.clear();
};
```

**After:**
```javascript
this.socket.onclose = (event) => {
  // event.code: 1000=正常, 1001=Going Away (サーバ停止), 1006=異常切断
  console.log('Disconnected from Minecraft server',
              'code=' + (event && event.code),
              'wasClean=' + (event && event.wasClean));
  // 既存の _cleanupSocket() を直接呼ぶ（DRY原則）
  // _cleanupSocket() は最初に this.socket.onclose = null するため、
  // 内部の this.socket.close() が onclose を再発火させる懸念はない
  this._cleanupSocket();
};
```

**再入の安全性検証（重要）:**

`_cleanupSocket()` の処理順序:
```javascript
_cleanupSocket() {
  if (this.socket) {
    try {
      this.socket.onopen = null;
      this.socket.onmessage = null;
      this.socket.onerror = null;
      this.socket.onclose = null;   // ← 最初にnull化（再入防止）
      this.socket.close();          // ← その後close
    } catch (e) {}
    this.socket = null;
  }
  ...
}
```

**実行フロー:**
1. ブラウザが `onclose` イベントを発火（既にTCPは閉じている）
2. 我々の `onclose` ハンドラが実行される（this.socket.onclose は現時点ではまだ参照が残っている）
3. `_cleanupSocket()` が呼ばれる
4. `this.socket.onclose = null` で参照を切る
5. `this.socket.close()` を呼ぶが、既に閉じているので no-op
6. `this.socket = null` でフィールドをクリア
7. `pendingRequests` を全 reject + clear
8. リターン

**結論: 再入は発生しない**。`_cleanupSocket()` の `close()` 呼び出しは、既に閉じた socket に対する操作で、ブラウザは新たな onclose イベントを発火しない（W3C仕様）。

#### (5-2) `event.code` のロギング理由

| event.code | 意味 | 期待される発生状況 |
|----|----|----|
| 1000 | 正常クローズ | Scratch から `disconnect` ブロック実行 |
| 1001 | Going Away | Phase 3 で送る Close フレーム（サーバ停止） |
| 1005 | 受信ステータスなし | クローズフレームが来ずに切断 |
| 1006 | 異常クローズ | TCP切断（Minecraftクラッシュ等） |

ログを残すことで、本番環境で問題が起きた際の切り分けが容易になる。

#### (5-3) ファイル間の同期確認

3つの兄弟ファイル（blocksonly.js / player.js / compatibilitytesting.js）にも同じ修正を適用すること。`docs/plans/2026-04-06_handleMessage-payload-guard-plan.md` と同じ手順で同期する。

**4ファイルの onclose 行番号（事前確認結果）:**

| ファイル | onclose 行 |
|----|----|
| `gui.js` | 64913 |
| `blocksonly.js` | 67017 |
| `player.js` | 67017 |
| `compatibilitytesting.js` | 67017 |

---

### Phase 6: （任意）切断時のユーザー通知

省略可。ニーズが顕在化したら別計画書で対応。

---

### Phase 7: テスト・ドキュメント更新

#### (7-1) `CLAUDE.md` の更新
「ブロック修正・追加時の作業フロー」に注意点を追記:
- MOD 側の `serverStopping` フラグはワールド開閉時に自動でリセットされるため、手動操作不要
- 連続でワールドを開閉して動作確認すること

#### (7-2) `docs/service-overview.md` （存在する場合）
WebSocket 接続のライフサイクルセクションに、ワールド閉鎖時の挙動を記載。

#### (7-3) コミットメッセージ雛形
```
fix: Minecraftワールド閉鎖時のクラッシュ防止と自動切断対応

- CommandExecutor: serverStopping フラグでサーバ停止中の全コマンドを拒否
- MinecraftEduMod: ServerStoppingEvent で CommandExecutor.shutdown() を先に呼ぶ
- SimpleWebSocketServer: クライアントへCloseフレーム送信、awaitTermination 追加、SO_REUSEADDR
- CommandExecutor: clearArea の再帰タスクが停止フラグを尊重するように修正
- gui.js / blocksonly.js / player.js / compatibilitytesting.js: onclose で完全クリーンアップ

Refs: docs/plans/2026-04-08_minecraft-world-disconnect-crash-fix-plan.md
```

---

## 8. 各ファイル変更点の詳細サマリ

| ファイル | 変更行数（概算） | フェーズ |
|----|-----|-----|
| `minecraft-mod/src/main/java/com/github/minecraftedu/commands/CommandExecutor.java` | +35行 / 修正5箇所 | 1, 4 |
| `minecraft-mod/src/main/java/com/github/minecraftedu/MinecraftEduMod.java` | +12行 | 2 |
| `minecraft-mod/src/main/java/com/github/minecraftedu/network/SimpleWebSocketServer.java` | +60行 | 3 |
| `gui.js` | +5行 | 5 |
| `blocksonly.js` | +5行 | 5 |
| `player.js` | +5行 | 5 |
| `compatibilitytesting.js` | +5行 | 5 |
| `CLAUDE.md` | +10行（任意） | 7 |

---

## 9. テスト計画

### 9.1 単体動作確認（手動）

| # | テスト項目 | 期待結果 |
|---|----|----|
| T1 | ワールドを開く → 接続 → ブロック配置 → ワールドを閉じる | クラッシュなし。Scratch側が自動で「未接続」になる。onclose code=1001 |
| T2 | T1 後、別ワールドを開く → 手動で「接続」 | 接続成功 |
| T3 | T1 後、同ワールドを再オープン → 手動で「接続」 | 接続成功 |
| T4 | clearArea 実行中にワールドを閉じる | クラッシュなし。チャンク処理が中断される。`clearArea aborted: server stopping` ログ |
| T5 | getTargetBlock 実行直後にワールドを閉じる | クラッシュなし。Scratch側にタイムアウトエラー（5秒後） |
| T6 | 連続10回ワールド開閉 → 接続 | 全回成功（ポート占有エラーなし） |
| T7 | ワールド閉鎖 → Scratch側で setBlock 実行 | 「Not connected」エラー（クラッシュなし） |
| T8 | ワールド閉鎖中（ServerStopping発火後すぐ）に Scratch から再接続試行 | 接続自体は確立するが、`SERVER_STOPPING` エラーレスポンスを受信。クラッシュなし |
| T9 | Minecraft 起動直後（ワールド未オープン）に Scratch から接続 | 接続失敗（WebSocketサーバが起動していない） |
| T10 | ワールドオープン中に Minecraft を強制終了（Alt+F4） | Scratch側の onclose が発火（code=1006）。次回 Minecraft 再起動 → ワールドオープン → 再接続可能 |
| T11【★Round 2】 | ワールド閉鎖 → 即座に `connect` ブロック → 即座に `setBlock` ブロック | 接続失敗 or `SERVER_STOPPING` レスポンス。MC側クラッシュなし |
| T12【★Round 2】 | `disconnect` ブロック実行 → onclose 発火 | `_cleanupSocket()` が二重実行されるが安全（再入なし）。次回 connect 可能 |
| T13【★Round 2】 | 再接続後の `/entityspawn allow` スラッシュコマンド | 新しい CommandExecutor インスタンスに対して動作 |
| ~~T14~~ | ~~ワールド閉鎖直後に `/entityspawn status`~~ | **【Round 3 で削除】** ワールド閉鎖中はスラッシュコマンド入力欄が消えるためテスト不可 |
| T15【★Round 2】 | onclose 内で `_cleanupSocket()` 呼び出し時、ブラウザが新たな onclose を発火しないことを確認 | コンソールに onclose ログが1回のみ |
| T16【★Round 3】 | `getTargetBlock` 実行 → 直後にワールド閉鎖 → 5秒以内にハンドラスレッド終了 | サーバログで `executor terminated` 確認、handler thread リーク0 |
| T17【★Round 3】 | ワールド閉鎖中に Scratch から `connect` メッセージ送信 | `SERVER_STOPPING` エラーレスポンスを受信 |
| T18【★Round 3】 | 停止時の SocketException ログレベル確認 | `LOGGER.error` ではなく `LOGGER.debug` で出力されること |

### 9.2 ストレステスト

| # | テスト項目 | 期待結果 |
|---|----|----|
| S1 | 100回連続でワールド開閉 | クラッシュ0件、ポート占有エラー0件 |
| S2 | clearArea 実行 → 即ワールド閉鎖 → 即ワールドオープン → 接続 | クラッシュ0件 |
| S3 | 10秒間 setBlock を高頻度送信 → ワールド閉鎖 | クラッシュ0件 |
| S4【★Round 2】 | `stop()` を意図的に2回連続呼び出し（テスト用フック） | クラッシュなし、エラーログなし |
| S5【★Round 2】 | ワールド開閉を1秒間隔で繰り返し（10回） | 全回ポートバインド成功（SO_REUSEADDRの効果検証） |

### 9.3 リグレッションテスト

既存の主要ブロックが動作することを確認:
- 接続 / 切断
- setBlock / fillBlocks / clearArea
- teleport / getPosition / getTargetBlock
- chat
- prepareForLearning（複合コマンド）

### 9.4 ログ確認ポイント

サーバログ (`logs/latest.log`) で以下のメッセージが期待どおり出ること:
- `MinecraftEdu server stopping...`
- `CommandExecutor: shutdown signaled`
- `WebSocket server stopped`
- `Command rejected (server stopping): xxx`（停止中にコマンド到着時）
- `clearArea aborted: server stopping`（clearArea中に閉じた場合）

---

## 10. リリース手順

### 10.1 ローカルビルドと検証
```bash
# (1) main ブランチに切り替え
git checkout main

# (2) Phase 1〜4 の変更を適用（MOD側のみ）
# (3) MOD ビルド
cd minecraft-mod
./gradlew build

# (4) JARをmodsフォルダにコピー
cp build/libs/minecraftedu-mod-0.1.0-1.20.1.jar \
   "/c/Users/riyum/AppData/Roaming/.minecraft_1.20.1/mods/"

# (5) Minecraft起動 → セクション9のテストを実行
```

### 10.2 Scratch 側 (Phase 5) の適用
```bash
# (6) gh-pages ブランチに切り替え（gui.js等はこちらが本流）
git checkout gh-pages

# (7) 4ファイル（gui.js, blocksonly.js, player.js, compatibilitytesting.js）を編集
# (8) ローカルでindex.htmlを開いて動作確認
# (9) 既存のMOD（修正版）と組み合わせて再度テスト
```

### 10.3 コミット & デプロイ
```bash
# (10) MOD側コミット（mainブランチ）
git checkout main
git add minecraft-mod/src/main/java/com/github/minecraftedu/
git commit -m "fix: Minecraftワールド閉鎖時のクラッシュ防止 (MOD側)"

# (11) Scratch側コミット（gh-pagesブランチ）
git checkout gh-pages
git add gui.js blocksonly.js player.js compatibilitytesting.js
git commit -m "fix: WebSocket切断時の完全クリーンアップ (Scratch側)"

# (12) 必要に応じて gh-pages へ push
git push origin gh-pages
```

> **注意**: 計画書 `docs/plans/2026-04-08_minecraft-world-disconnect-crash-fix-plan.md` は両ブランチで管理する場合は両方にコピーする。あるいは main ブランチのみで管理する。

---

## 11. ロールバック手順

各フェーズはコミットを分けるため、問題発生時は該当コミットだけリバート可能。

### 11.1 緊急ロールバック
```bash
# 直前の修正コミットを取り消し（履歴に残す形）
git revert <commit-hash>

# MODの場合は再ビルド
cd minecraft-mod && ./gradlew build
cp build/libs/minecraftedu-mod-0.1.0-1.20.1.jar "/c/Users/riyum/AppData/Roaming/.minecraft_1.20.1/mods/"
```

### 11.2 部分ロールバック
- Phase 3 (SimpleWebSocketServer) のみ問題があれば、その変更ファイルだけ revert
- Phase 1, 2 だけでもクラッシュ抑制効果は得られる（最低限の安全弁）

---

## 12. リスクと未解決事項

### 12.1 既知のリスク

| リスク | 影響度 | 緩和策 |
|----|----|----|
| `SimpleWebSocketServer.stop()` の awaitTermination が3秒で完了しない場合、Forgeのサーバ停止処理が遅延し、Minecraft 側で「サーバが応答しません」警告が出る可能性 | 中 | 待機を 2+1秒 にとどめる。logだけ出して continue |
| ~~Scratch 側 onclose で `_cleanupSocket()` を呼ばない~~ | - | **【Round 2 で解消】** Phase 5 で `_cleanupSocket()` を直接呼ぶように変更 |
| ~~`getTargetBlock` のタイムアウト短縮が既存の挙動を変える~~ | - | **【Round 1 で解消】** タイムアウトは5秒のまま据え置き |
| MOD 側のみ修正・Scratch 側未修正でデプロイした場合 | 低 | MOD 側だけでもクラッシュ防止は可能（要件R2,R3,R4を満たす）。Scratch側はR1のUX改善のみ |
| 4つのJSファイルの行番号がずれている可能性 | 低 | 計画書では gui.js を基準に記載。実装時に grep で再確認。Round 2 で各ファイルの行番号確認済み |
| TEXT frame after CLOSE frame の protocol violation【★Round 3】 | 低 | ブラウザは無視する。実害なし。詳細は付録C-1 |
| Forge `ServerStoppingEvent` 後の数 tick 内に setBlock 等が走る可能性【★Round 3】 | - | **`safeExecute` のラムダ内チェックで吸収** |
| `executeGetTargetBlock` 中にワールド閉鎖された場合のハンドラスレッド滞留【★Round 3】 | 低 | `executor.shutdownNow()` の interrupt で `Future.get()` が解除される（最大3秒で確実終了） |

### 12.2 未解決事項（将来の課題）
- **U1**: Scratch 側にハートビート機構なし → 別計画書で対応すべき
- **U2**: WebSocketサーバが port 14711 固定 → ユーザーが変更したい場合の設定UI なし
- **U3**: ~~`MinecraftEduMod` のフィールドの `volatile` 化~~ → **本計画書 Phase 2 で対応済み**
- **U4**: clearArea の進捗をユーザーがキャンセルする方法がない（停止フラグは内部用）
- **U5**【★Round 2】: `getTargetBlock` の同期待ち5秒中、ハンドラスレッドが滞留する。ハンドラスレッド数が無制限なので大量の滞留があってもMC本体は安全だが、メモリリークの懸念あり
- **U6**【★Round 2】: `entitySpawningAllowed` フィールドが `volatile` でない（Phase 1 のスコープ外。本計画書とは別途修正推奨）
- **U7**【★Round 2】: Windows 環境での `SO_REUSEADDR` の動作差分（同一ポートへの複数バインドを許す可能性）。実装後に Windows での実機検証が必要

---

## 13. 関連ファイル一覧

### 13.1 MOD 側（`minecraft-mod/src/main/java/com/github/minecraftedu/`）
- `MinecraftEduMod.java` - エントリポイント、ライフサイクル
- `network/SimpleWebSocketServer.java` - WebSocketサーバ本体
- `network/MinecraftWebSocketHandler.java` - メッセージディスパッチ
- `commands/CommandExecutor.java` - 全コマンド実装
- `init/ModBlocks.java` - 縦スラブ等のブロック登録（本計画書とは無関係）
- `init/ModItems.java` - アイテム登録（本計画書とは無関係）
- `block/VerticalSlabBlock.java` - 縦スラブクラス（本計画書とは無関係）

### 13.2 Scratch 側（プロジェクトルート）
- `gui.js` - 通常エディタ
- `blocksonly.js` - ブロックのみ表示
- `player.js` - プレイヤー
- `compatibilitytesting.js` - 互換テスト

### 13.3 関連計画書（参考）
- `docs/plans/2026-04-06_critical-issues-fix-plan.md`
- `docs/plans/2026-04-06_handleMessage-payload-guard-plan.md`
- `docs/plans/cross-tab-script-copy-paste-plan.md`

---

## 13.4 ログ確認による動作検証ガイド【★Round 3 新規】

実装後、以下のログメッセージが期待どおりに出ることを確認:

### 正常系（ワールド開閉）

**ワールドを開いた直後:**
```
[INFO] MinecraftEdu server starting...
[INFO] WebSocket server started on port 14711
[INFO] Scratch clients can now connect!
```

**Scratch から接続:**
```
[INFO] Client connected: /127.0.0.1:xxxx
[INFO] WebSocket handshake completed
[INFO] Client connected: scratch_client_xxx with session: yyyy
```

**ワールドを閉じた直後:**
```
[INFO] MinecraftEdu server stopping...
[INFO] CommandExecutor: shutdown signaled
[DEBUG] Client connection closed during shutdown: Socket closed   ← Round 3 で追加
[INFO] WebSocket server stopped
```

### 異常系（停止中にコマンド到着）

```
[WARN] Command rejected (server stopping): setBlock
```

### 異常系（clearArea 中にワールド閉鎖）

```
[INFO] clearArea aborted: server stopping (index=15/64)
```

### 確認できないと問題

- `Error handling client` が **error レベル** で出る → Round 3 (3-7) のログレベル変更が未適用
- `BindException: Address already in use` → SO_REUSEADDR が効いていない
- `safeExecute task error` が大量に出る → どこかで `safeExecute` を経由しない `server.execute` が残っている

---

## 14. 承認チェックリスト

実装着手前にユーザーに確認すべき項目:

- [ ] **Q1**: Phase 1〜7 のうち、どの範囲を一括で実装するか? それとも段階的か?
  - 推奨: **Phase 1〜4 を一括** + **Phase 5 を続けて適用**（クラッシュ防止が最優先）
- [ ] **Q2**: テストは Claude が手動で行うか、ユーザーが手動で行うか?
  - MOD のビルドと配置は Claude が可能。Minecraft の起動・操作はユーザー側が必要
- [ ] **Q3**: 計画書のフェーズ分けでコミットを分けるか、まとめて1コミットにするか?
- [ ] **Q4**: Phase 5 の Scratch 側修正で、4ファイル全てを修正するか、`gui.js` のみで様子を見るか?
- [ ] **Q5**: `getTargetBlock` のタイムアウト 5秒→2秒 短縮を含めるか?
- [ ] **Q6**: 録画中（`isRecording=true`）にワールドを閉じた場合、ffmpeg を強制停止する変更（Phase 4-3）を含めるか?
- [ ] **Q7**: コミット先ブランチ
  - MOD側: `main`
  - Scratch側: `gh-pages`
  - 計画書: 両方にコピー / `main` のみ / `gh-pages` のみ?

---

## 付録A: 修正前後の挙動比較表

| シナリオ | 修正前 | 修正後 |
|----|----|----|
| ワールド閉鎖 | クラッシュリスクあり / Scratch検知遅延 | 安全停止 / Scratch即時onclose |
| clearArea中の閉鎖 | クラッシュリスク高 | 停止フラグで中断 |
| 連続ワールド開閉 | ポート占有でMC再起動が必要なケースあり | SO_REUSEADDRで回避 |
| 閉鎖中のコマンド受信 | クラッシュリスクあり | コマンド即拒否（null返却） |
| getTargetBlock中の閉鎖 | 5秒ハンドラ滞留 | 2秒で復帰 |
| 再オープン後の手動接続 | 失敗するケースあり | 安定して成功 |

## 付録B: WebSocket Close フレームの形式

RFC 6455 に基づく Close フレーム（status code 1001 = Going Away）:

```
バイト0: 0x88  (FIN=1, RSV=000, opcode=0x8 Close)
バイト1: 0x02  (MASK=0, payload length=2) ※サーバ→クライアントなのでMASK不要
バイト2: 0x03  (status code 上位8ビット)
バイト3: 0xE9  (status code 下位8ビット) → 0x03E9 = 1001
```

## 付録C-1: 停止時の既知の小問題【★Round 3 追記】

### TEXT フレーム after CLOSE フレームの protocol violation

**シナリオ:**
1. `stop()` が `sendCloseToAllClients()` を呼び、socket A に Close フレームを送信
2. socket A の `synchronized(out)` ロックを解放
3. ハンドラスレッド B が処理中だったコマンドのレスポンスを `sendTextFrame(outA, response)` で送信しようとする
4. `synchronized(out)` を取得し、TEXT フレームを送信
5. クライアント側は **CLOSE の後に TEXT** を受信する

**RFC 6455 違反:** Close frame 後にデータフレームを送ってはいけない。

**実害:**
- Chrome / Firefox / Edge: TEXT フレームを無視。`onmessage` には届かない or 警告を出すのみ
- Safari: 同上
- 我々の Scratch クライアント: `onmessage` が呼ばれても、その直後 `onclose` が呼ばれて全 pendingRequests が reject されるため、応答は捨てられる
- **ユーザー視点での実害なし**

**より厳密な対策（採用しない）:**
- 各 Socket に「closing」フラグを持たせ、close frame 送信後は sendTextFrame をスキップ
- 複雑度に対する効果が薄いため不採用

---

## 付録C: serverStopping フラグの状態遷移

```
[起動時]
  ServerStartingEvent → new CommandExecutor() → serverStopping = false

[実行中]
  serverStopping = false  ※全コマンド実行可能

[停止時]
  ServerStoppingEvent
    ↓
  commandExecutor.shutdown() → serverStopping = true
    ↓
  webSocketServer.stop()
    ↓
  webSocketServer = null, commandExecutor = null

[再起動時]
  ServerStartingEvent → new CommandExecutor() → 新インスタンスで serverStopping = false
```

---

**以上**

この計画書に同意いただけましたら、Phase 1 から実装に着手します。
チェックリスト Q1〜Q7 についてご回答いただければ、それに沿って進めます。
