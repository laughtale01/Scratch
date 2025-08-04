# 🔨 Minecraft協調学習システム - 包括的開発ガイド
## 最終更新: 2025年7月29日

### 🆕 新規追加 (2025-07-29)
- **品質保証システム**: 136個の包括的テストを追加
- **セキュリティテスト**: XSS・SQLインジェクション防止の検証
- **認証システムテスト**: トークンベース認証の動作検証
- **多言語テスト**: 7言語サポートの確認
- **テストカバレッジ**: 3.7% → 25%に大幅向上

---

## 📋 目次

1. [システム全体像](#システム全体像)
2. [プロジェクト構造詳細](#プロジェクト構造詳細)
3. [コアコンポーネント解説](#コアコンポーネント解説)
4. [通信プロトコル仕様](#通信プロトコル仕様)
5. [機能追加ガイド](#機能追加ガイド)
6. [修正・デバッグガイド](#修正デバッグガイド)
7. [ビルド・デプロイ手順](#ビルドデプロイ手順)

---

## 🌐 システム全体像

### アーキテクチャ概要

```
┌─────────────────────┐         WebSocket         ┌──────────────────────┐
│   Scratch 3.0       │ ◄────────14711──────────► │  Minecraft Mod       │
│   Extension         │                            │  (Forge 1.20.1)      │
└─────────────────────┘                            └──────────────────────┘
         │                                                    │
         │                                                    │
    [JavaScript]                                         [Java 17]
         │                                                    │
         └─────────── JSON/Legacy Protocol ──────────────────┘
```

### 主要技術スタック

| コンポーネント | 技術 | バージョン | 役割 |
|------------|------|----------|------|
| Minecraft Mod | Java + Forge | 1.20.1-47.2.0 | ゲーム内操作実行 |
| Scratch拡張 | JavaScript | ES6+ | ユーザーインターフェース |
| 通信層 | WebSocket | RFC 6455 | リアルタイム双方向通信 |
| ビルドツール | Gradle/Webpack | 7.6.4/5.x | 自動ビルド |

---

## 📁 プロジェクト構造詳細

### Minecraft Mod構造

```
minecraft-mod/
├── src/main/java/com/yourname/minecraftcollaboration/
│   ├── MinecraftCollaborationMod.java    # エントリーポイント
│   ├── network/
│   │   ├── WebSocketHandler.java         # WebSocketサーバー実装
│   │   └── CollaborationMessageProcessor.java # メッセージ処理
│   ├── commands/
│   │   └── CollaborationCommandHandler.java # コマンド実行
│   ├── collaboration/
│   │   └── CollaborationManager.java     # 協調機能管理
│   ├── models/
│   │   ├── Invitation.java               # 招待モデル
│   │   └── VisitRequest.java            # 訪問要求モデル
│   ├── server/
│   │   ├── CollaborationServer.java      # サーバー統合管理
│   │   └── CollaborationCoordinator.java # 協調処理調整
│   ├── security/
│   │   └── SecurityConfig.java          # セキュリティ設定
│   └── util/
│       ├── BlockUtils.java              # ブロック操作ユーティリティ
│       └── ValidationUtils.java         # バリデーション
```

### Scratch拡張構造

```
scratch-extension/
├── src/
│   ├── index.js           # メイン拡張ファイル（全機能統合）
│   ├── minecraft-*.js     # 機能別分割ファイル（未使用）
│   └── index.js.backup    # バックアップ
├── dist/                  # ビルド成果物
├── webpack.config.js      # Webpack設定
└── package.json          # 依存関係定義
```

---

## 🔧 コアコンポーネント解説

### 1. MinecraftCollaborationMod.java

**役割**: Modのエントリーポイント、初期化とライフサイクル管理

```java
主要機能:
- Mod初期化 (@Mod アノテーション)
- WebSocketサーバー起動
- イベントハンドリング
- シングルプレイヤー/マルチプレイヤー対応
```

**重要メソッド**:
- `setup()`: Mod初期化、WebSocketライブラリ確認
- `doClientStuff()`: クライアント側初期化（シングルプレイヤー）
- `onServerStarting()`: サーバー起動時処理
- `onServerStopping()`: サーバー停止時処理

### 2. WebSocketHandler.java

**役割**: WebSocket通信の管理とセキュリティ

```java
主要機能:
- WebSocketサーバー実装（ポート14711）
- 接続管理（最大接続数制限）
- セキュリティチェック（IPアドレス制限）
- メッセージルーティング
```

**重要メソッド**:
- `onOpen()`: 新規接続処理、セキュリティチェック
- `onMessage()`: メッセージ受信・処理
- `broadcastToClients()`: 全クライアントへ送信

### 3. CollaborationMessageProcessor.java

**役割**: メッセージ解析とコマンドルーティング

```java
サポート形式:
1. JSON形式: {"command": "cmdName", "args": {...}}
2. レガシー形式: "command(arg1,arg2,arg3)"
```

**処理フロー**:
1. メッセージ形式判定（JSON/レガシー）
2. コマンドと引数の抽出
3. 適切なハンドラーへルーティング
4. レスポンス生成・送信

**主要コマンドマッピング**:
- 基本操作: placeBlock, removeBlock, getBlock
- プレイヤー: getPlayerPos, teleport, gamemode
- 建築: fill, buildCircle, buildSphere, buildWall, buildHouse
- 環境: time, weather
- 協調: invite, requestVisit, approveVisit, returnHome

### 4. CollaborationCommandHandler.java

**役割**: 実際のMinecraft操作実行

```java
実装カテゴリ:
1. 接続管理: connect, status
2. 協調機能: 招待、訪問、帰宅
3. 基本操作: ブロック配置、破壊、取得
4. プレイヤー操作: 位置取得、テレポート
5. 環境制御: 時間、天候
```

**セキュリティ機能**:
- 座標検証
- ブロックタイプ検証
- 権限チェック
- エラーハンドリング

### 5. Scratch拡張 (index.js)

**役割**: Scratchブロック定義とWebSocket通信

**ブロック構成**:
```javascript
19種類のブロック:
- 接続管理 (2)
- 基本操作 (5)
- 建築支援 (6)
- 環境制御 (3)
- 協調機能 (7)
```

**通信実装**:
- WebSocket接続管理（自動再接続なし）
- JSONメッセージ送信
- レスポンス処理
- エラーハンドリング

---

## 📡 通信プロトコル仕様

### メッセージフォーマット

#### 1. JSON形式（推奨）
```json
// リクエスト
{
  "command": "placeBlock",
  "args": {
    "x": 100,
    "y": 64,
    "z": 100,
    "block": "stone"
  }
}

// レスポンス（成功）
{
  "type": "placeBlock",
  "status": "success",
  "message": "Block placed successfully"
}

// レスポンス（エラー）
{
  "type": "error",
  "error": "invalidBlock",
  "message": "Unknown block type: invalid_block"
}
```

#### 2. レガシー形式（後方互換）
```
// リクエスト
world.setBlock(100,64,100,stone)

// レスポンス
block.set(100,64,100,stone)
```

### 主要コマンド一覧

| カテゴリ | コマンド | 引数 | 説明 |
|---------|---------|------|------|
| 接続 | connect | - | WebSocket接続確立 |
| ブロック | placeBlock | x,y,z,block | ブロック配置 |
| ブロック | removeBlock | x,y,z | ブロック破壊 |
| プレイヤー | getPlayerPos | - | 座標取得 |
| プレイヤー | teleport | x,y,z | テレポート |
| 建築 | fill | x1,y1,z1,x2,y2,z2,block | 範囲塗りつぶし |
| 建築 | buildCircle | x,y,z,radius,block | 円作成 |
| 協調 | inviteFriend | friendName | 招待送信 |
| 協調 | emergencyReturn | - | 緊急帰宅 |

---

## ➕ 機能追加ガイド

### 新しいScratchブロックの追加

#### 1. Scratch拡張側 (index.js)

```javascript
// Step 1: getInfo()にブロック定義追加
{
    opcode: 'newFeature',
    blockType: BlockType.COMMAND,
    text: '🆕 新機能: [PARAM]',
    arguments: {
        PARAM: {
            type: ArgumentType.STRING,
            defaultValue: 'default'
        }
    }
}

// Step 2: 実行メソッド追加
newFeature(args) {
    this.sendCommand('newFeature', {
        param: args.PARAM
    });
}
```

#### 2. Minecraft Mod側

```java
// Step 1: CollaborationMessageProcessor.javaにルーティング追加
case "newFeature":
    return commandHandler.handleNewFeature(new String[] {
        args.get("param")
    });

// Step 2: CollaborationCommandHandler.javaにハンドラー追加
public String handleNewFeature(String[] args) {
    try {
        // 実装
        return createSuccessResponse("newFeature", "完了");
    } catch (Exception e) {
        return createErrorResponse("newFeatureError", e.getMessage());
    }
}
```

### 新しい協調機能の追加

#### 1. モデル作成 (models/)
```java
public class NewCollaborationFeature {
    private String id;
    private String initiator;
    private String target;
    private LocalDateTime timestamp;
    // getters, setters
}
```

#### 2. CollaborationManagerに機能追加
```java
public NewCollaborationFeature createNewFeature(String initiator, String target) {
    // 実装
}
```

#### 3. 通信プロトコル定義
```java
// コマンド追加
case "collaboration.newFeature":
    return handleNewCollaborationFeature(args);
```

---

## 🔧 修正・デバッグガイド

### デバッグ手順

#### 1. ログ確認
```java
// Minecraft側
LOGGER.debug("Debug message: {}", variable);
LOGGER.error("Error occurred", exception);

// Scratch側
console.log('Debug:', data);
console.error('Error:', error);
```

#### 2. WebSocket通信確認
```bash
# テストクライアント実行
node tests/test-websocket.js

# ブラウザコンソールで確認
ws = new WebSocket('ws://localhost:14711');
ws.onmessage = (e) => console.log(e.data);
```

### よくある問題と解決方法

#### 1. ClassNotFoundException
```
原因: jarJar設定ミス
解決: build.gradleのjarJar設定確認
```

#### 2. WebSocket接続失敗
```
原因: ポート競合、ファイアウォール
解決: netstat -an | grep 14711
```

#### 3. コマンド実行エラー
```
原因: 引数不足、型エラー
解決: CollaborationMessageProcessorのログ確認
```

---

## 🏗️ ビルド・デプロイ手順

### 開発ビルド

#### Minecraft Mod
```bash
cd minecraft-mod
./gradlew clean build
# 成果物: build/libs/minecraft-collaboration-mod-1.0.0-all.jar
```

#### Scratch拡張
```bash
cd scratch-extension
npm install
npm run build
# 成果物: dist/minecraft-collaboration-extension.js
```

### プロダクションビルド

#### 1. バージョン更新
```gradle
// build.gradle
version = '1.1.0'

// package.json
"version": "1.1.0"
```

#### 2. リリースビルド
```bash
# Mod
./gradlew clean build

# 拡張
npm run build -- --mode production
```

#### 3. 配布準備
```
releases/
├── minecraft-collaboration-mod-1.1.0-all.jar
├── minecraft-collaboration-extension-1.1.0.js
└── README.txt
```

---

## 📝 開発時の注意事項

### コーディング規約

1. **Java側**
   - パッケージ名: com.yourname.minecraftcollaboration
   - クラス名: PascalCase
   - メソッド名: camelCase
   - 定数: UPPER_SNAKE_CASE

2. **JavaScript側**
   - 関数名: camelCase
   - 定数: UPPER_SNAKE_CASE
   - Promiseベースの非同期処理

### セキュリティ考慮事項

1. **入力検証**
   - 座標の範囲チェック
   - ブロックタイプの検証
   - 文字列長の制限

2. **接続管理**
   - 最大接続数制限
   - IPアドレス制限
   - タイムアウト設定

3. **権限管理**
   - プレイヤー権限確認
   - 危険な操作の制限

---

## 🔗 参考リンク

- [Minecraft Forge Documentation](https://docs.minecraftforge.net/)
- [Scratch Extension Development](https://github.com/LLK/scratch-vm/wiki/Extensions)
- [WebSocket Protocol](https://datatracker.ietf.org/doc/html/rfc6455)
- [プロジェクトGitHub](https://github.com/YOUR_USERNAME/minecraft-collaboration-system)