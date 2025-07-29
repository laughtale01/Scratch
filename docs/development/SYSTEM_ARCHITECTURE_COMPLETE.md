# 🏗️ Minecraft協調学習システム - 完全システムアーキテクチャ
## 最終更新: 2025年7月26日

---

## 📚 ドキュメント概要

このドキュメントは、プロジェクトの全ファイル構造、各ファイルの役割、相互関係を完全に記述した技術資料です。
機能追加や修正を行う開発者が、システム全体を理解し、正確な作業を行えるようにすることを目的としています。

---

## 🗂️ 完全ファイル構造マップ

### 📁 ルートディレクトリ構成

```
D:\minecraft_collaboration_project\
├── 📄 設定・管理ファイル
│   ├── README.md                 # プロジェクト概要（日本語）
│   ├── LICENSE                   # MITライセンス
│   ├── CLAUDE.md                 # Claude Code開発規約
│   ├── .gitignore               # Git除外設定
│   ├── _config.yml              # GitHub Pages設定
│   ├── package.json             # ルートNode.js設定（テスト用）
│   └── package-lock.json        # 依存関係ロック
│
├── 📁 minecraft-mod/            # Minecraft Forge Mod
├── 📁 scratch-extension/        # Scratch 3.0拡張
├── 📁 docs/                     # ドキュメント群
├── 📁 assets/                   # 画像リソース
├── 📁 scripts/                  # ビルド・実行スクリプト
├── 📁 tests/                    # テストファイル
├── 📁 config/                   # 設定ファイル
├── 📁 public/                   # 公開用HTML
└── 📁 scratch-gui/              # Scratch GUI（別管理推奨）
```

---

## 🎮 Minecraft Mod詳細構造

### 📂 minecraft-mod/

#### ビルド設定
```
build.gradle              # Gradleビルド設定
├── Minecraft: 1.20.1
├── Forge: 47.2.0
├── Java: 17
└── jarJar: WebSocket依存関係同梱

gradle.properties         # Gradle環境設定
├── org.gradle.java.home=JAVA_HOME
└── org.gradle.jvmargs=-Xmx3G

settings.gradle          # プロジェクト名設定
```

#### ソースコード構造
```
src/main/java/com/yourname/minecraftcollaboration/
├── 📄 MinecraftCollaborationMod.java    [151行]
│   責任: Modエントリーポイント、初期化
│   ├── setup(): WebSocketライブラリ確認
│   ├── doClientStuff(): クライアント側初期化
│   ├── onServerStarting(): サーバー起動処理
│   └── onServerStopping(): クリーンアップ
│
├── 📁 network/
│   ├── 📄 WebSocketHandler.java        [141行]
│   │   責任: WebSocketサーバー実装
│   │   ├── ポート: 14711
│   │   ├── セキュリティ: IP制限、接続数制限
│   │   └── メッセージ配信
│   │
│   └── 📄 CollaborationMessageProcessor.java [584行]
│       責任: メッセージ解析・ルーティング
│       ├── JSON形式サポート
│       ├── レガシー形式サポート
│       └── 19種類のコマンドルーティング
│
├── 📁 commands/
│   └── 📄 CollaborationCommandHandler.java [446行]
│       責任: Minecraftコマンド実行
│       ├── 協調機能（7種）
│       ├── 基本操作（4種）
│       └── エラーハンドリング
│
├── 📁 collaboration/
│   └── 📄 CollaborationManager.java    [約200行]
│       責任: 協調機能の中央管理
│       ├── 招待システム
│       ├── 訪問管理
│       └── ワールド切り替え
│
├── 📁 models/
│   ├── 📄 Invitation.java              [約50行]
│   │   フィールド: id, senderName, receiverName, timestamp, status
│   │
│   └── 📄 VisitRequest.java            [約50行]
│       フィールド: id, requesterName, hostName, timestamp, approved
│
├── 📁 server/
│   ├── 📄 CollaborationServer.java     [約150行]
│   │   責任: サーバー統合管理
│   │   └── WebSocketとMinecraftの橋渡し
│   │
│   └── 📄 CollaborationCoordinator.java [約100行]
│       責任: 非同期処理調整
│
├── 📁 security/
│   └── 📄 SecurityConfig.java          [約80行]
│       責任: セキュリティポリシー
│       ├── ALLOWED_IPS: ローカルのみ
│       └── MAX_CONNECTIONS: 10
│
└── 📁 util/
    ├── 📄 BlockUtils.java              [約100行]
    │   責任: ブロック操作ユーティリティ
    │   └── getBlockFromString(): 文字列→Block変換
    │
    └── 📄 ValidationUtils.java         [約80行]
        責任: 入力検証
        └── 座標、ブロック名の検証
```

#### リソース
```
src/main/resources/
├── META-INF/
│   └── mods.toml         # Mod設定（名前、バージョン、依存関係）
└── pack.mcmeta          # リソースパック情報
```

---

## 🧩 Scratch拡張詳細構造

### 📂 scratch-extension/

#### 設定ファイル
```
package.json             # Node.js設定
├── 依存関係: format-message, scratch-vm, ws
└── ビルド: webpack

webpack.config.js        # Webpack設定
├── エントリー: src/index.js
├── 出力: dist/minecraft-collaboration-extension.js
└── Babel設定
```

#### ソースコード
```
src/
├── 📄 index.js                 [702行] ⭐メイン
│   責任: Scratch拡張の完全実装
│   ├── getInfo(): 19種類のブロック定義
│   ├── WebSocket通信実装
│   ├── コマンド送信メソッド
│   └── バリデーション
│
├── 📄 minecraft-blocks.js      [未使用]
├── 📄 minecraft-build.js       [未使用]
├── 📄 minecraft-commands.js    [未使用]
├── 📄 minecraft-info.js        [未使用]
├── 📄 minecraft-main.js        [未使用]
└── 📄 index.js.backup         [バックアップ]
```

#### Scratchブロック定義（19種）
```javascript
1. 接続管理（2種）
   - connect: Minecraft接続
   - isConnected: 接続状態確認

2. 基本操作（5種）
   - placeBlock: ブロック配置
   - removeBlock: ブロック破壊
   - getBlockType: ブロック取得
   - getPlayerX/Y/Z: 座標取得

3. 建築支援（6種）
   - fillBlocks: 範囲塗りつぶし
   - clearArea: 範囲クリア
   - buildCircle: 円作成
   - buildSphere: 球作成
   - buildWall: 壁作成
   - buildHouse: 家作成

4. プレイヤー制御（3種）
   - teleportPlayer: テレポート
   - setPlayerMode: ゲームモード
   - sendChat: チャット送信

5. 環境制御（2種）
   - setTime: 時間設定
   - setWeather: 天候設定

6. 協調機能（7種）
   - inviteFriend: 友達招待
   - getInvitations: 招待数取得
   - requestVisit: 訪問申請
   - approveVisit: 訪問承認
   - getCurrentWorld: 現在ワールド
   - returnHome: 帰宅
   - emergencyReturn: 緊急帰宅
```

---

## 📡 通信フロー詳細

### WebSocket通信シーケンス

```
[Scratch] → [WebSocket Client] → [Port 14711] → [WebSocketHandler]
                                                        ↓
                                            [CollaborationMessageProcessor]
                                                        ↓
                                            [CollaborationCommandHandler]
                                                        ↓
                                                  [Minecraft World]
```

### メッセージ処理フロー

1. **Scratch側送信**
```javascript
// index.js - sendCommand()
{
  "command": "placeBlock",
  "args": {
    "x": 100,
    "y": 64,
    "z": 100,
    "block": "stone"
  }
}
```

2. **WebSocketHandler受信**
```java
// WebSocketHandler.java - onMessage()
- セキュリティチェック
- CollaborationMessageProcessorへ転送
```

3. **メッセージ解析**
```java
// CollaborationMessageProcessor.java
- JSON/レガシー形式判定
- コマンド抽出
- 引数パース
```

4. **コマンド実行**
```java
// CollaborationCommandHandler.java
- Minecraft APIコール
- ワールド操作
- レスポンス生成
```

5. **レスポンス返信**
```java
// 成功時
{"type":"placeBlock","status":"success","message":"Block placed"}

// エラー時
{"type":"error","error":"invalidBlock","message":"Unknown block"}
```

---

## 🔧 依存関係完全マップ

### Minecraft Mod依存関係

```gradle
実行時依存:
├── net.minecraftforge:forge:1.20.1-47.2.0
├── org.java-websocket:Java-WebSocket:1.5.4 (jarJar同梱)
├── com.google.code.gson:gson:2.10.1
└── org.slf4j:slf4j-api:2.0.9

ビルド時依存:
└── net.minecraftforge.gradle:ForgeGradle:5.1.+
```

### Scratch拡張依存関係

```json
本番依存:
├── format-message@6.2.4     # 国際化
├── scratch-vm@5.0.300       # Scratch VM統合
└── ws@8.18.3               # WebSocketクライアント

開発依存:
├── @babel/core@7.28.0      # トランスパイル
├── @babel/preset-env@7.28.0
├── babel-loader@10.0.0
├── webpack@5.100.1         # バンドル
└── webpack-cli@6.0.1
```

---

## 🛠️ ビルドシステム詳細

### Minecraft Modビルドプロセス

```bash
# ビルドコマンド
./gradlew clean build

# 処理フロー
1. Java 17環境確認
2. 依存関係ダウンロード
3. ソースコンパイル
4. jarJarによるWebSocketライブラリ同梱
5. リソース処理
6. JARファイル生成
   └── build/libs/minecraft-collaboration-mod-1.0.0-all.jar
```

### Scratch拡張ビルドプロセス

```bash
# ビルドコマンド
npm run build

# 処理フロー
1. Babel: ES6+ → ES5変換
2. Webpack: モジュールバンドル
3. 最適化: minify, tree-shaking
4. 出力生成
   └── dist/minecraft-collaboration-extension.js
```

---

## 📋 設定ファイル詳細

### .gitignore重要項目
```
# ビルド成果物
build/
dist/
*.jar

# 実行時生成
run/
logs/
crash-reports/

# 開発環境
.gradle/
node_modules/
.idea/

# 大規模ディレクトリ
scratch-gui/
```

### GitHub Pages設定 (_config.yml)
```yaml
theme: jekyll-theme-cayman
title: Minecraft協調学習システム
description: ScratchとMinecraftを連携した教育プラットフォーム
```

---

## 🧪 テストファイル構造

### tests/ディレクトリ
```
test-websocket.js        # WebSocket接続テスト
test-collaboration.js    # 協調機能テスト
test-correct-commands.js # コマンド動作テスト
test-server.js          # サーバー起動テスト
test-websocket-server.js # サーバー側テスト
```

### テスト実行フロー
```javascript
// 基本的なテストパターン
const ws = new WebSocket('ws://localhost:14711');
ws.on('open', () => {
    ws.send(JSON.stringify({
        command: 'getPlayerPos',
        args: {}
    }));
});
```

---

## 🚀 スクリプトファイル

### scripts/ディレクトリ
```
Windows用:
├── build-mod.bat          # Modビルド
├── run-minecraft.bat      # Minecraft起動
├── start-all.bat         # 全サービス起動
├── setup-scratch-gui.bat  # Scratch GUI設定
└── test-gradle.bat       # Gradle動作確認

クロスプラットフォーム:
├── setup-scratch-gui.sh   # Unix系設定
└── test-minecraft-mod.sh  # Unix系テスト
```

---

## 📝 開発フロー完全ガイド

### 新機能追加の完全手順

1. **要件定義**
   - Scratchブロックの仕様決定
   - 必要なパラメータ定義
   - エラーケース想定

2. **Scratch拡張実装**
   ```javascript
   // index.js
   - getInfo()にブロック追加
   - 実行メソッド実装
   - バリデーション追加
   ```

3. **通信プロトコル定義**
   ```java
   // CollaborationMessageProcessor.java
   - コマンドルーティング追加
   - 引数マッピング実装
   ```

4. **Minecraft実装**
   ```java
   // CollaborationCommandHandler.java
   - ハンドラーメソッド追加
   - Minecraft API呼び出し
   - エラーハンドリング
   ```

5. **テスト**
   - ユニットテスト作成
   - 統合テスト実行
   - エッジケース確認

6. **ドキュメント更新**
   - README.md更新
   - API仕様追記
   - 変更履歴記録

---

## 🔍 トラブルシューティングマップ

### 問題診断フローチャート

```
接続エラー？
├── Yes → ポート14711確認
│         └── netstat -an | grep 14711
└── No → コマンドエラー？
         ├── Yes → ログ確認
         │         ├── Minecraft: logs/latest.log
         │         └── Browser: F12 Console
         └── No → ビルドエラー？
                  ├── Mod: ./gradlew clean build
                  └── Extension: npm install && npm run build
```

---

## 🎯 まとめ

このドキュメントは、Minecraft協調学習システムの完全な技術仕様書です。
各ファイルの役割、相互関係、処理フローを理解することで、
効率的な機能追加と確実なデバッグが可能になります。

最終更新: 2025年7月26日
総ファイル数: 約50ファイル（コア部分）
総コード行数: 約3,000行（Java + JavaScript）