# Scratch-Minecraft 連携プロジェクト 包括的設計書

## プロジェクト概要

### プロジェクト名
**MinecraftEdu Scratch Controller** (仮称)

### 目的
takecxさんの「Scratch-Minecraft連携システム」をベースに、以下の独自機能を追加したオリジナルプロジェクトを構築：
1. **マルチプレイヤー対応**: 複数のScratchクライアントから同時にMinecraftを操作
2. **教育コンテンツ**: チュートリアル、課題システム、進捗管理機能

### 対象バージョン
- **Minecraft**: 1.20.x（最新版）
- **Scratch**: 3.0
- **Minecraft Forge**: 1.20.x対応版

---

## アーキテクチャ設計

### システム構成図

```
┌─────────────────────────────────────────────────────────┐
│                   複数のScratchクライアント                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Scratch GUI  │  │ Scratch GUI  │  │ Scratch GUI  │  │
│  │   (React)    │  │   (React)    │  │   (React)    │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                  │                  │          │
│         └──────────────────┼──────────────────┘          │
│                            │                             │
│                   ┌────────▼────────┐                    │
│                   │   Scratch VM    │                    │
│                   │ (scratch3_      │                    │
│                   │  minecraft)     │                    │
│                   └────────┬────────┘                    │
└────────────────────────────┼──────────────────────────────┘
                             │
                    WebSocket/HTTP
                   (Port: 14711/14712)
                             │
┌────────────────────────────▼──────────────────────────────┐
│                    Connection Manager                      │
│              (マルチクライアント管理層)                      │
│   ┌──────────────────────────────────────────────────┐   │
│   │ ・クライアント認証                                 │   │
│   │ ・セッション管理                                   │   │
│   │ ・権限制御                                         │   │
│   │ ・コマンドキュー                                   │   │
│   └──────────────────────────────────────────────────┘   │
└────────────────────────────┬──────────────────────────────┘
                             │
┌────────────────────────────▼──────────────────────────────┐
│              Minecraft Server (1.20.x)                     │
│                                                            │
│   ┌────────────────────────────────────────────────┐     │
│   │      RemoteControllerMod (Forge MOD)            │     │
│   │  ┌──────────────────────────────────────────┐  │     │
│   │  │ ・コマンド処理                            │  │     │
│   │  │ ・ワールド操作                            │  │     │
│   │  │ ・イベントハンドリング                     │  │     │
│   │  │ ・教育コンテンツマネージャー               │  │     │
│   │  └──────────────────────────────────────────┘  │     │
│   └────────────────────────────────────────────────┘     │
└───────────────────────────────────────────────────────────┘
```

---

## 技術スタック

### フロントエンド (Scratchクライアント)

| 技術 | 用途 | バージョン |
|------|------|-----------|
| React | UI構築 | 最新安定版 |
| JavaScript/ES6+ | Scratch GUI実装 | - |
| scratch-gui | Scratchインターフェース | fork from takecx |
| scratch-vm | Scratch仮想マシン | fork from takecx |
| Webpack | ビルドツール | 設定済み |

### バックエンド (Minecraft MOD)

| 技術 | 用途 | バージョン |
|------|------|-----------|
| Java | MOD開発言語 | 17+ |
| Minecraft Forge | MODプラットフォーム | 1.20.x |
| Gradle | ビルドシステム | 7.x+ |
| Netty | ネットワーク通信 | Forge同梱 |

### 通信プロトコル

| プロトコル | 用途 | ポート |
|-----------|------|--------|
| WebSocket | リアルタイム双方向通信 | 14711 |
| HTTP/REST | 初期接続・状態取得 | 14712 |
| JSON | データフォーマット | - |

---

## プロジェクトディレクトリ構造

```
minecraft-laughtare-project/
│
├── docs/                          # ドキュメント
│   ├── architecture.md            # アーキテクチャ詳細
│   ├── api-reference.md           # API仕様書
│   ├── development-guide.md       # 開発ガイド
│   └── user-manual.md             # ユーザーマニュアル
│
├── scratch-client/                # Scratchクライアント
│   ├── scratch-gui/               # GUI (React)
│   │   ├── src/
│   │   │   ├── components/        # Reactコンポーネント
│   │   │   ├── lib/               # ライブラリ
│   │   │   └── playground/        # 開発用プレイグラウンド
│   │   ├── package.json
│   │   └── webpack.config.js
│   │
│   └── scratch-vm/                # 仮想マシン
│       ├── src/
│       │   ├── extensions/
│       │   │   └── scratch3_minecraft/
│       │   │       ├── index.js               # メイン拡張機能
│       │   │       ├── block_info.js          # ブロック情報
│       │   │       ├── entity_info.js         # エンティティ情報
│       │   │       ├── enchant_info.js        # エンチャント情報
│       │   │       ├── particle_info.js       # パーティクル情報
│       │   │       ├── connection_manager.js  # 接続管理（新規）
│       │   │       └── education_client.js    # 教育機能（新規）
│       │   └── io/
│       │       └── websocket.js               # WebSocket通信
│       └── package.json
│
├── minecraft-mod/                 # Minecraft MOD
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/github/yourname/minecraftedu/
│   │   │   │       ├── RemoteControllerMod.java        # メインMODクラス
│   │   │   │       ├── network/                         # ネットワーク層
│   │   │   │       │   ├── WebSocketServer.java
│   │   │   │       │   ├── ConnectionManager.java       # マルチクライアント対応
│   │   │   │       │   ├── ClientSession.java
│   │   │   │       │   └── CommandQueue.java
│   │   │   │       ├── commands/                        # コマンド処理
│   │   │   │       │   ├── CommandHandler.java
│   │   │   │       │   ├── BlockCommand.java
│   │   │   │       │   ├── EntityCommand.java
│   │   │   │       │   └── ChatCommand.java
│   │   │   │       ├── education/                       # 教育機能（新規）
│   │   │   │       │   ├── TutorialManager.java
│   │   │   │       │   ├── ChallengeSystem.java
│   │   │   │       │   ├── ProgressTracker.java
│   │   │   │       │   └── AchievementManager.java
│   │   │   │       └── utils/
│   │   │   │           └── CoordinateHelper.java
│   │   │   └── resources/
│   │   │       ├── META-INF/
│   │   │       │   └── mods.toml                        # MOD情報
│   │   │       ├── assets/
│   │   │       └── data/
│   │   │           └── tutorials/                       # チュートリアルデータ
│   │   └── test/
│   ├── build.gradle
│   ├── gradle.properties
│   └── settings.gradle
│
├── shared/                        # 共有リソース
│   ├── protocol/                  # 通信プロトコル定義
│   │   ├── protocol.json          # プロトコル仕様
│   │   └── message-schema.json    # メッセージスキーマ
│   └── assets/                    # 共有アセット
│       ├── icons/
│       └── sounds/
│
├── tools/                         # 開発ツール
│   ├── build-all.sh               # 一括ビルドスクリプト
│   ├── deploy.sh                  # デプロイスクリプト
│   └── test-server.js             # テストサーバー
│
├── .gitignore
├── README.md
├── LICENSE
└── PROJECT_DESIGN.md              # この設計書
```

---

## コア機能設計

### 1. 基本Minecraft操作機能（takecxプロジェクトから継承）

#### Scratchブロック一覧

| カテゴリ | ブロック | 機能 | 実装優先度 |
|---------|---------|------|-----------|
| **チャット** | say [TEXT] | チャットメッセージ送信 | P0（必須） |
| **ブロック操作** | set block at x:[X] y:[Y] z:[Z] to [BLOCK] | ブロック配置（絶対座標） | P0 |
| | set block at ~[X] ~[Y] ~[Z] to [BLOCK] | ブロック配置（相対座標） | P0 |
| | get block at x:[X] y:[Y] z:[Z] | ブロック情報取得 | P1 |
| | fill blocks from [X1,Y1,Z1] to [X2,Y2,Z2] with [BLOCK] | 範囲ブロック配置 | P1 |
| **エンティティ** | summon [ENTITY] at x:[X] y:[Y] z:[Z] | エンティティ召喚 | P0 |
| | get entity [TYPE] near [RADIUS] | 近くのエンティティ取得 | P2 |
| **プレイヤー** | teleport to x:[X] y:[Y] z:[Z] | テレポート | P0 |
| | get player position | プレイヤー座標取得 | P0 |
| | set gamemode to [MODE] | ゲームモード変更 | P1 |
| **ワールド** | set weather to [WEATHER] | 天気変更 | P1 |
| | set time to [TIME] | 時刻変更 | P1 |
| **エフェクト** | spawn particle [PARTICLE] at [X,Y,Z] | パーティクル生成 | P2 |
| | give enchantment [ENCHANT] level [LEVEL] | エンチャント付与 | P2 |
| **コマンド** | execute command [CMD] | 任意コマンド実行 | P1 |

**優先度説明**: P0=MVP必須, P1=初期リリース, P2=追加機能

---

### 2. マルチプレイヤー対応機能（新規実装）

#### 設計目標
- 最大10クライアントの同時接続
- クライアント間の権限管理
- コマンド競合の解決
- セッション管理

#### 主要コンポーネント

##### 2.1 Connection Manager (Java側)

```java
public class ConnectionManager {
    // クライアントセッション管理
    private Map<String, ClientSession> activeSessions;

    // クライアント認証
    public ClientSession authenticateClient(String clientId, String token);

    // 権限チェック
    public boolean hasPermission(ClientSession session, Permission permission);

    // コマンド実行キュー
    public void queueCommand(ClientSession session, Command command);
}
```

##### 2.2 Client Session

```java
public class ClientSession {
    private String sessionId;          // セッションID
    private String clientName;         // クライアント名
    private PlayerRole role;           // 役割（Teacher/Student）
    private Set<Permission> permissions; // 権限セット
    private long connectionTime;       // 接続時刻
    private boolean isActive;          // アクティブ状態
}
```

##### 2.3 権限システム

| 権限レベル | 役割 | 許可される操作 |
|-----------|------|--------------|
| TEACHER | 教師 | すべての操作 + 生徒管理 |
| STUDENT_FULL | 生徒（フル） | ブロック配置、エンティティ、コマンド |
| STUDENT_LIMITED | 生徒（制限） | 基本ブロック配置のみ |
| OBSERVER | 観察者 | 読み取り専用 |

##### 2.4 コマンド競合解決

**戦略**: タイムスタンプベースの順序保証
```
1. 各コマンドにタイムスタンプを付与
2. コマンドキューで順序を管理
3. 同一ブロックへの同時操作は後勝ち（Last-Write-Wins）
4. 競合発生時は両クライアントに通知
```

---

### 3. 教育コンテンツ機能（新規実装）

#### 3.1 チュートリアルシステム

##### チュートリアル構造

```json
{
  "tutorial": {
    "id": "tutorial_001",
    "title": "はじめてのプログラミング",
    "description": "Scratchを使ってMinecraftの世界を操作しよう",
    "difficulty": "beginner",
    "estimatedTime": "15分",
    "steps": [
      {
        "stepId": 1,
        "title": "チャットメッセージを送ろう",
        "instruction": "「say」ブロックを使って、「Hello, Minecraft!」と送信してみよう",
        "hint": "緑色のブロックを探してね",
        "validation": {
          "type": "chat_message",
          "expectedText": "Hello, Minecraft!"
        },
        "reward": {
          "points": 10,
          "badge": "first_chat"
        }
      },
      {
        "stepId": 2,
        "title": "ブロックを置こう",
        "instruction": "プレイヤーの目の前に石ブロックを置いてみよう",
        "hint": "相対座標 ~0 ~1 ~2 を使うと目の前に置けるよ",
        "validation": {
          "type": "block_placed",
          "blockType": "stone",
          "relativePosition": [0, 1, 2]
        },
        "reward": {
          "points": 20,
          "badge": "first_builder"
        }
      }
    ]
  }
}
```

##### チュートリアル管理クラス (Java)

```java
public class TutorialManager {
    // チュートリアル読み込み
    public Tutorial loadTutorial(String tutorialId);

    // 進捗チェック
    public void validateStep(ClientSession session, int stepId, Object action);

    // ヒント提供
    public String getHint(String tutorialId, int stepId);

    // 完了処理
    public void completeTutorial(ClientSession session, String tutorialId);
}
```

#### 3.2 課題システム

##### 課題タイプ

| タイプ | 説明 | 例 |
|-------|------|---|
| **建築課題** | 指定された構造を建築 | 「5x5の家を作ろう」 |
| **探索課題** | 特定の条件を満たす | 「村を見つけよう」 |
| **プログラミング課題** | 効率的なコードを書く | 「10行以内で階段を作ろう」 |
| **創造課題** | 自由創作 | 「テーマに沿った作品を作ろう」 |

##### 課題評価システム

```java
public class ChallengeSystem {
    // 課題評価基準
    public enum CriteriaType {
        BLOCK_COUNT,        // ブロック数
        STRUCTURE_MATCH,    // 構造一致度
        CODE_EFFICIENCY,    // コード効率
        CREATIVITY,         // 創造性（教師評価）
        TIME_LIMIT          // 制限時間
    }

    // 自動評価
    public ChallengeResult evaluateChallenge(ClientSession session, Challenge challenge);

    // 手動評価（教師用）
    public void teacherGrade(ClientSession teacher, String studentId, int grade, String feedback);
}
```

#### 3.3 進捗管理システム

##### 進捗トラッキング

```java
public class ProgressTracker {
    // 統計データ
    public class ProgressStats {
        private int totalBlocks;              // 配置ブロック総数
        private int tutorialsCompleted;       // 完了チュートリアル数
        private int challengesCompleted;      // 完了課題数
        private int totalPoints;              // 獲得ポイント
        private Set<String> earnedBadges;     // 獲得バッジ
        private Map<String, Integer> blockUsage; // ブロック使用統計
    }

    // 進捗保存
    public void saveProgress(ClientSession session, ProgressStats stats);

    // 進捗読み込み
    public ProgressStats loadProgress(String userId);

    // レポート生成
    public ProgressReport generateReport(String userId, DateRange range);
}
```

##### バッジシステム

| バッジID | 名前 | 取得条件 |
|---------|------|---------|
| first_chat | おしゃべり初心者 | 初めてチャットを送信 |
| first_builder | 建築家見習い | 初めてブロックを配置 |
| speedster | スピードスター | 課題を5分以内にクリア |
| architect | 建築マスター | 建築課題を10個クリア |
| creative_genius | 創造の天才 | 創造課題で満点を獲得 |

---

## 通信プロトコル設計

### メッセージフォーマット

#### 基本構造（JSON）

```json
{
  "version": "1.0",
  "messageId": "uuid-v4",
  "timestamp": 1234567890,
  "sessionId": "session-uuid",
  "type": "command|query|event|response",
  "payload": {}
}
```

#### コマンドメッセージ例

```json
{
  "version": "1.0",
  "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "timestamp": 1699876543210,
  "sessionId": "session-12345",
  "type": "command",
  "payload": {
    "action": "setBlock",
    "params": {
      "x": 100,
      "y": 64,
      "z": -50,
      "blockType": "minecraft:stone",
      "blockState": {}
    }
  }
}
```

#### イベントメッセージ例（進捗通知）

```json
{
  "version": "1.0",
  "messageId": "e1f2g3h4-i5j6-7890-abcd-ef1234567890",
  "timestamp": 1699876555000,
  "sessionId": "session-12345",
  "type": "event",
  "payload": {
    "eventType": "tutorialStepCompleted",
    "data": {
      "tutorialId": "tutorial_001",
      "stepId": 2,
      "pointsEarned": 20,
      "badgeEarned": "first_builder"
    }
  }
}
```

### WebSocketエンドポイント

| エンドポイント | 用途 |
|--------------|------|
| `ws://localhost:14711/minecraft` | メインコマンド実行 |
| `ws://localhost:14711/events` | イベントストリーム |
| `ws://localhost:14711/education` | 教育機能専用 |

### HTTPエンドポイント

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/status` | サーバーステータス |
| POST | `/api/auth/connect` | クライアント接続認証 |
| GET | `/api/session/{sessionId}` | セッション情報取得 |
| GET | `/api/tutorials` | チュートリアル一覧 |
| GET | `/api/challenges` | 課題一覧 |
| GET | `/api/progress/{userId}` | 進捗情報取得 |

---

## 実装ロードマップ

### フェーズ1: 基盤構築（1-2週間）

#### 目標
基本的なScratch-Minecraft連携を動作させる

#### タスク
1. 開発環境セットアップ
   - [ ] Node.js環境構築
   - [ ] Java開発環境構築
   - [ ] Minecraft Forge開発環境構築
2. リポジトリfork
   - [ ] scratch-vm fork
   - [ ] scratch-gui fork
   - [ ] RemoteControllerMod fork
3. ビルド確認
   - [ ] Scratch VM/GUIのビルド
   - [ ] MODのビルド
   - [ ] ローカルテスト実行

### フェーズ2: 基本機能実装（2-3週間）

#### 目標
takecxプロジェクトの主要機能を1.20.x対応で実装

#### タスク
1. Minecraft MOD側
   - [ ] WebSocketサーバー実装
   - [ ] 基本コマンドハンドラ（P0ブロック）
     - [ ] チャット送信
     - [ ] ブロック配置（絶対/相対）
     - [ ] エンティティ召喚
     - [ ] プレイヤー位置取得
     - [ ] テレポート
   - [ ] エラーハンドリング
2. Scratch VM側
   - [ ] scratch3_minecraft拡張のアップデート
   - [ ] WebSocket通信実装
   - [ ] 基本ブロック動作確認
3. 統合テスト
   - [ ] エンドツーエンドテスト
   - [ ] 基本機能デモ

### フェーズ3: マルチプレイヤー対応（3-4週間）

#### 目標
複数クライアント同時接続を実現

#### タスク
1. MOD側実装
   - [ ] ConnectionManager実装
   - [ ] ClientSession管理
   - [ ] 認証システム
   - [ ] 権限システム
   - [ ] コマンドキュー実装
   - [ ] 競合解決ロジック
2. Scratch側実装
   - [ ] 接続管理UI
   - [ ] クライアント識別
   - [ ] 権限表示
3. テスト
   - [ ] 2クライアント接続テスト
   - [ ] 5クライアント負荷テスト
   - [ ] 権限制御テスト

### フェーズ4: 教育機能実装（4-5週間）

#### 目標
チュートリアル・課題・進捗管理システムの実装

#### タスク
1. チュートリアルシステム
   - [ ] TutorialManager実装
   - [ ] チュートリアルデータ構造設計
   - [ ] ステップ検証ロジック
   - [ ] ヒント機能
   - [ ] サンプルチュートリアル3本作成
2. 課題システム
   - [ ] ChallengeSystem実装
   - [ ] 評価基準実装
   - [ ] 自動評価ロジック
   - [ ] 教師評価UI
3. 進捗管理
   - [ ] ProgressTracker実装
   - [ ] データ永続化（JSON/SQLite）
   - [ ] バッジシステム
   - [ ] レポート生成
4. Scratch UI拡張
   - [ ] チュートリアルパネル
   - [ ] 課題表示
   - [ ] 進捗ダッシュボード
   - [ ] バッジ表示

### フェーズ5: テスト・最適化（2-3週間）

#### 目標
品質保証とパフォーマンス最適化

#### タスク
1. テスト
   - [ ] 単体テスト（カバレッジ70%以上）
   - [ ] 統合テスト
   - [ ] ユーザー受け入れテスト
   - [ ] 負荷テスト（10クライアント）
2. 最適化
   - [ ] WebSocket通信最適化
   - [ ] メモリ使用量削減
   - [ ] レスポンス時間改善
3. ドキュメント
   - [ ] APIリファレンス
   - [ ] 開発者ガイド
   - [ ] ユーザーマニュアル
   - [ ] チュートリアル作成ガイド

### フェーズ6: リリース準備（1-2週間）

#### タスク
1. パッケージング
   - [ ] MOD配布パッケージ作成
   - [ ] Scratch GUI デプロイ（GitHub Pages）
   - [ ] インストーラー作成
2. ドキュメント最終化
   - [ ] README.md
   - [ ] CHANGELOG.md
   - [ ] LICENSE
3. リリース
   - [ ] CurseForgeへのMOD公開
   - [ ] GitHub Pages公開
   - [ ] デモビデオ作成

---

## 開発環境セットアップ

### 必要なソフトウェア

| ソフトウェア | バージョン | 用途 |
|------------|-----------|------|
| Node.js | 16.x以上 | Scratch開発 |
| npm/yarn | 最新 | パッケージ管理 |
| Java JDK | 17 | Minecraft MOD開発 |
| Gradle | 7.x以上 | MODビルド |
| Git | 最新 | バージョン管理 |
| Minecraft Java Edition | 1.20.x | テスト環境 |
| VSCode/IntelliJ IDEA | 最新 | IDE |

### セットアップ手順

#### 1. リポジトリのfork

```bash
# Scratch VM
git clone https://github.com/takecx/scratch-vm.git
cd scratch-vm
git checkout develop
npm install

# Scratch GUI
git clone https://github.com/takecx/scratch-gui.git
cd scratch-gui
npm install

# RemoteControllerMod
git clone https://github.com/takecx/RemoteControllerMod.git
cd RemoteControllerMod
./gradlew build
```

#### 2. ローカル開発環境

```bash
# Scratch VM 開発サーバー起動
cd scratch-vm
npm start
# → http://localhost:8073/playground/

# Scratch GUI 開発サーバー起動（別ターミナル）
cd scratch-gui
npm start
# → http://localhost:8601/

# MODビルドとテスト
cd minecraft-mod
./gradlew runClient  # Minecraft起動
```

#### 3. npm linkによる連携

```bash
# scratch-vmをリンク
cd scratch-vm
npm link

# scratch-guiからリンク
cd scratch-gui
npm link scratch-vm
```

---

## テスト戦略

### テストレベル

| レベル | 範囲 | ツール | 目標カバレッジ |
|-------|------|--------|--------------|
| 単体テスト | 個別メソッド/関数 | Jest (JS), JUnit (Java) | 70% |
| 統合テスト | コンポーネント間連携 | Jest, Gradle Test | 60% |
| E2Eテスト | システム全体 | Selenium, Puppeteer | 主要シナリオ |
| 負荷テスト | 性能・スケーラビリティ | JMeter, カスタムスクリプト | 10同時接続 |

### テストケース例

#### 単体テスト (Java - ConnectionManager)

```java
@Test
public void testClientAuthentication() {
    ConnectionManager manager = new ConnectionManager();
    ClientSession session = manager.authenticateClient("client1", "valid-token");
    assertNotNull(session);
    assertEquals("client1", session.getClientName());
}

@Test
public void testPermissionCheck() {
    ClientSession teacherSession = createTeacherSession();
    assertTrue(manager.hasPermission(teacherSession, Permission.PLACE_BLOCK));

    ClientSession studentSession = createLimitedStudentSession();
    assertFalse(manager.hasPermission(studentSession, Permission.EXECUTE_COMMAND));
}
```

#### E2Eテスト（シナリオ）

```
Scenario: 生徒がチュートリアルを完了する
  Given 生徒がScratchクライアントに接続している
  And チュートリアル「はじめてのプログラミング」を開始している
  When ステップ1「チャットメッセージ」を完了する
  And ステップ2「ブロック配置」を完了する
  Then チュートリアル完了通知が表示される
  And バッジ「first_builder」が獲得される
  And 進捗に30ポイントが追加される
```

---

## セキュリティ考慮事項

### 脅威モデル

| 脅威 | 影響 | 対策 |
|------|------|------|
| 不正なクライアント接続 | 高 | トークンベース認証、IP制限 |
| コマンドインジェクション | 高 | 入力検証、コマンドホワイトリスト |
| DoS攻撃 | 中 | レート制限、接続数制限 |
| 権限昇格 | 高 | 厳格な権限チェック、セッション検証 |
| データ改ざん | 中 | 進捗データの署名、サーバー側検証 |

### セキュリティ実装

```java
public class SecurityManager {
    // コマンド検証
    public boolean isCommandSafe(String command) {
        // ホワイトリストチェック
        // 危険なコマンドの除外（/stop, /op など）
    }

    // レート制限
    public boolean checkRateLimit(ClientSession session) {
        // 1秒あたりのコマンド数制限
    }

    // セッション検証
    public boolean validateSession(String sessionId, String token) {
        // トークンの有効性チェック
        // セッションタイムアウト確認
    }
}
```

---

## パフォーマンス目標

| メトリクス | 目標値 |
|-----------|--------|
| コマンド応答時間 | < 100ms（平均） |
| WebSocket接続確立 | < 500ms |
| 同時接続数 | 10クライアント |
| メモリ使用量（MOD） | < 256MB |
| CPU使用率 | < 30%（10クライアント時） |
| ブロック配置スループット | > 100 blocks/sec |

---

## ライセンスとクレジット

### ライセンス
- **このプロジェクト**: MIT License（予定）
- **takecx/scratch-vm**: BSD-3-Clause
- **takecx/scratch-gui**: BSD-3-Clause
- **takecx/RemoteControllerMod**: ライセンス確認要

### クレジット
```
このプロジェクトは以下のプロジェクトをベースにしています：
- takecx/RemoteControllerMod
- takecx/scratch-vm
- takecx/scratch-gui

オリジナル作者: takecx (https://github.com/takecx)
参考記事: https://qiita.com/panda531/items/a6dfd87bd68ba2601793
```

---

## 次のステップ

1. **このドキュメントのレビュー**: 設計内容を確認し、必要に応じて修正
2. **プロジェクトディレクトリの作成**: 上記構造に従ってディレクトリを作成
3. **開発環境のセットアップ**: 必要なツールをインストール
4. **リポジトリのfork**: takecxさんのリポジトリをfork
5. **フェーズ1の開始**: 基盤構築タスクに着手

---

## 更新履歴

| 日付 | バージョン | 変更内容 |
|------|-----------|---------|
| 2025-11-12 | 1.0 | 初版作成 |

---

**作成日**: 2025-11-12
**プロジェクト開始予定**: 2025-11-12
**目標リリース日**: 2025-12-31（約6週間後）
