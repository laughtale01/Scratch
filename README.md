# 🎮 Minecraft協調学習システム (Minecraft Forge 1.20.1版)

## 📋 プロジェクト概要

ScratchとMinecraft 1.20.1を連携した小学生向け協調学習システムです。友達の作品世界への招待・訪問機能を通じて、安全で楽しい協調学習環境を提供します。

## 🎯 主な機能

### 🤝 協調学習機能
- **友達招待システム**: 自分のワールドに友達を招待
- **世界訪問システム**: 友達のワールドへの訪問申請・承認
- **安全な帰宅機能**: 通常帰宅・緊急帰宅システム
- **リアルタイム通信**: ScratchとMinecraftの双方向通信

### 🛡️ 安全機能
- ローカルネットワーク限定接続
- 招待制による安全な接続管理
- 緊急帰宅システム
- 時間制限機能（実装予定）

## 🏗️ プロジェクト構造

```
minecraft_collaboration_project/
├── minecraft-mod/                              # Minecraft Mod (Forge 1.20.1)
│   ├── src/main/java/com/yourname/minecraftcollaboration/
│   │   ├── MinecraftCollaborationMod.java     # メインModクラス
│   │   ├── network/                           # ネットワーク通信
│   │   │   ├── WebSocketHandler.java          # WebSocket通信処理
│   │   │   └── CollaborationMessageProcessor.java # メッセージ処理
│   │   ├── server/                           # サーバー管理
│   │   │   ├── CollaborationServer.java       # 協調サーバー
│   │   │   └── CollaborationCoordinator.java  # 協調機能管理
│   │   ├── commands/                         # コマンド処理
│   │   │   └── CollaborationCommandHandler.java # Scratchコマンド処理
│   │   └── entities/                         # エンティティ（将来実装）
│   ├── src/main/resources/                   # リソース
│   │   ├── META-INF/mods.toml               # Mod設定
│   │   └── pack.mcmeta                      # パック情報
│   ├── build.gradle                         # ビルド設定
│   ├── gradle.properties                    # Gradle設定
│   └── settings.gradle                      # プロジェクト設定
├── scratch-extension/                        # Scratch拡張
│   ├── src/index.js                         # メイン拡張コード
│   ├── package.json                         # Node.js設定
│   ├── webpack.config.js                    # ビルド設定
│   └── .babelrc                            # Babel設定
├── docs/                                    # ドキュメント
└── config/                                  # 設定ファイル
```

## ⚙️ 技術仕様

### Minecraft Mod
- **Minecraft**: 1.20.1
- **Forge**: 47.2.0
- **Java**: 17+
- **WebSocket**: Java-WebSocket 1.5.4（jarJarで同梱）
- **ビルドツール**: Gradle 7.6.4

### Scratch拡張
- **Node.js**: v24.4.0+
- **WebSocket**: ws 8.18.3
- **ビルド**: Webpack 5+ + Babel
- **対応ブラウザ**: Chrome/Edge推奨

### 通信プロトコル
- **WebSocket**: ポート 14711
- **協調サーバー**: ポート 14712（将来実装）
- **メッセージ形式**: レガシー `command(arg1,arg2)` / JSON形式対応

## 🚀 セットアップ手順

### 1. 環境準備

#### Java 17のインストール確認
```bash
java -version
# Java 17以上が必要
```

#### Node.js v24.4.0+のインストール確認
```bash
node --version
npm --version
```

### 2. Minecraft Modのビルド

```bash
cd minecraft-mod

# 1. 依存関係の取得とビルド
./gradlew build

# 2. 開発用Minecraft起動（テスト用）
./gradlew runClient
```

### 3. Scratch拡張のビルド

```bash
cd scratch-extension

# 1. 依存関係のインストール
npm install

# 2. プロダクションビルド
npm run build

# 3. 開発モード（自動ビルド）
npm run dev
```

## 🎮 Scratchブロック一覧

### 🔌 接続管理
- `🔌 Minecraftに接続する`: WebSocket接続を開始
- `📡 接続されている？`: 接続状態をブール値で返す

### 📧 招待システム
- `📧 [FRIEND]さんを招待`: 友達を自分のワールドに招待
- `📬 招待通知の数`: 受信した招待件数を表示

### 🚪 訪問システム
- `🚪 [FRIEND]さんの世界に訪問申請`: 友達のワールドへ訪問申請
- `✅ [VISITOR]さんの訪問を承認`: 訪問申請を承認
- `🌍 現在いる世界`: 現在のワールド名を取得

### 🏠 帰宅システム
- `🏠 自分のワールドに帰る`: 通常の帰宅処理
- `🚨 緊急帰宅`: 即座に自分のワールドに帰る（体力・空腹度回復）

### 🧱 ブロック操作
- `🧱 [BLOCK]を X:[X] Y:[Y] Z:[Z] に置く`: 指定座標にブロック設置
- `⛏️ X:[X] Y:[Y] Z:[Z] のブロックを壊す`: 指定座標のブロック破壊

### 📍 プレイヤー情報
- `📍 プレイヤーのX座標`: X座標を取得
- `📍 プレイヤーのY座標`: Y座標を取得
- `📍 プレイヤーのZ座標`: Z座標を取得

### 💬 コミュニケーション
- `💬 チャット: [MESSAGE]`: チャットメッセージを送信

## 🔧 開発コマンド

### Minecraft Mod開発
```bash
# ビルド
./gradlew build

# 開発用Minecraft起動
./gradlew runClient

# サーバー用起動
./gradlew runServer

# クリーンビルド
./gradlew clean build

# 利用可能タスク一覧
./gradlew tasks
```

### Scratch拡張開発
```bash
# プロダクションビルド
npm run build

# 開発モード（自動ビルド）
npm run dev

# 依存関係の確認
npm list

# 依存関係の再インストール
npm install
```

## 🐛 トラブルシューティング

詳細なトラブルシューティングガイドは[docs/troubleshooting.md](docs/troubleshooting.md)を参照してください。

### よくある問題と解決方法

#### WebSocket接続の問題 ✅ 解決済み
**問題**: ClassNotFoundException: org.java_websocket.server.WebSocketServer
**解決**: jarJar設定を使用して依存関係を適切にパッケージング（2025-01-25修正済み）

#### Java関連の問題
**問題**: Gradle実行時にJavaバージョンエラー
```bash
# 解決: Java 17の確認
java -version

# gradle.propertiesの設定確認
cat gradle.properties
```

#### ビルドエラーの問題
**問題**: Gradleビルドが失敗する
```bash
# 1. クリーンビルド実行
./gradlew clean

# 2. Gradle Wrapper更新  
./gradlew wrapper --gradle-version=7.6

# 3. 再ビルド
./gradlew build
```

#### 接続テスト
WebSocket接続をテストするには：
```bash
# Minecraftを起動後、別のターミナルで実行
node test-websocket.js
```

## 📚 開発ガイド

### 新しいScratchブロックの追加

1. `scratch-extension/src/index.js`の`getInfo()`メソッドに新しいブロック定義を追加
2. 対応するメソッドを実装
3. `CollaborationMessageProcessor.java`にコマンドルーティングを追加
4. `CollaborationCommandHandler.java`に処理ロジックを実装

### 新しいMinecraftコマンドの追加

1. `CollaborationCommandHandler.java`に新しいハンドラーメソッドを追加
2. `CollaborationMessageProcessor.java`でルーティングを設定
3. 必要に応じて`CollaborationCoordinator.java`に協調機能を追加

## 🎓 教育利用について

### 学習目標
- プログラミング的思考の育成
- 協調作業スキルの向上
- 創造性と表現力の発達
- 問題解決能力の育成

### 安全性
- ローカルネットワーク内での利用を推奨
- 招待制による接続管理
- 緊急帰宅機能による安全確保
- 先生用管理機能（実装予定）

## 📄 ライセンス

MIT License - 教育目的での利用を推奨します。

## 🙏 謝辞

このプロジェクトは[takecx](https://github.com/takecx)さんのRemoteControllerModをベースに、Minecraft Forge 1.20.1対応版として新規開発されました。教育現場での協調学習支援を目的としています。