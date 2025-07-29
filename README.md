# 🎮 Minecraft協調学習システム (Minecraft Forge 1.20.1版)

> **📌 開発者向け**: Claude Codeを使用している場合は、まず[CLAUDE_CODE_STARTUP_GUIDE.md](CLAUDE_CODE_STARTUP_GUIDE.md)を参照してください。

## 📋 プロジェクト概要

ScratchとMinecraft 1.20.1を連携した小学生向け協調学習システムです。友達の作品世界への招待・訪問機能を通じて、安全で楽しい協調学習環境を提供します。

## 🎯 主な機能

### 🤝 協調学習機能
- **友達招待システム**: 自分のワールドに友達を招待
- **世界訪問システム**: 友達のワールドへの訪問申請・承認
- **安全な帰宅機能**: 通常帰宅・緊急帰宅システム
- **リアルタイム通信**: ScratchとMinecraftの双方向通信
- **AIエージェント**: 学習支援ガイドとインタラクティブサポート

### 👨‍🏫 教師管理機能
- **教室モード**: 学生活動のリアルタイム監視
- **権限制御**: 学生の行動制限と安全管理
- **活動追跡**: 詳細な学習活動ログと統計
- **一括管理**: 全学生への通知・召集・制御機能

### 📊 学習進捗管理
- **自動進捗追跡**: 建築・協調・創造性の多面的評価
- **達成システム**: マイルストーン達成と表彰機能
- **個別化学習**: 学習者に応じた目標設定
- **ピア評価**: 学生間の相互評価機能

### 🌐 多言語サポート
- **8言語対応**: 日本語、英語、中国語、韓国語、スペイン語、フランス語、ドイツ語
- **動的切替**: リアルタイム言語変更
- **完全ローカライズ**: UI・メッセージ・教材の多言語化

### 📦 ブロックパック機能
- **教育用制限**: 学習段階に応じたブロック制限
- **8つの定義パック**: 基本・教育・クリエイティブ・建築・レッドストーン・自然・初心者・上級者
- **カスタムパック作成**: 教師による独自パック作成
- **段階的学習**: 初心者から上級者への体系的な学習支援

### 📴 オフラインモード
- **ネットワーク非依存**: インターネット接続なしでも学習継続
- **自動同期**: オンライン復帰時の自動データ同期
- **データ保護**: 学習成果の確実な保存と復旧
- **継続性保証**: 環境変化に依存しない学習体験

### 🛡️ 安全・信頼性機能
- **包括的エラー処理**: 自動エラー検知と回復
- **パフォーマンス最適化**: インテリジェントキャッシュシステム
- **システム監視**: リアルタイム健全性チェック
- **データバックアップ**: 自動バックアップと復旧機能

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

### 🏗️ 建築支援
- `🏗️ 円を描く`: 指定座標に円を作成
- `⚪ 球を作る`: 指定座標に球体を作成
- `🧱 壁を作る`: 2点間に壁を建設
- `🏠 家を建てる`: 簡単な家を自動建築
- `🎨 範囲を塗りつぶす`: 指定範囲をブロックで塗りつぶし

### ⚙️ ゲーム設定
- `🎮 ゲームモード変更`: サバイバル/クリエイティブ切り替え
- `☀️ 時間を設定`: ゲーム内時間の変更
- `🌦️ 天候を変更`: 晴れ/雨/雷雨の切り替え
- `🚀 プレイヤーをテレポート`: 指定座標への瞬間移動

### 🧱 ブロック操作
- `🧱 [BLOCK]を X:[X] Y:[Y] Z:[Z] に置く`: 指定座標にブロック設置
- `⛏️ X:[X] Y:[Y] Z:[Z] のブロックを壊す`: 指定座標のブロック破壊

### 📍 プレイヤー情報
- `📍 プレイヤーのX座標`: X座標を取得
- `📍 プレイヤーのY座標`: Y座標を取得
- `📍 プレイヤーのZ座標`: Z座標を取得

### 💬 コミュニケーション
- `💬 チャット: [MESSAGE]`: チャットメッセージを送信

## 📊 プロジェクト状態（2025年7月28日更新）

### ✅ 実装完了機能
- **WebSocket通信**: jarJar設定により解決済み
- **基本コマンド**: 位置取得、チャット、ブロック操作
- **協調機能（完全実装済み）**: 
  - 招待システム（送信、受信、通知、有効期限管理）
  - 訪問システム（申請、承認、自動テレポート）
  - ワールド管理（現在位置追跡、ホーム位置記録）
  - 帰宅機能（通常帰宅、緊急帰宅＋体力/空腹度/状態異常回復）
- **建築支援機能（全機能実装済み）**: 
  - 円作成（半径指定、中空構造）
  - 球体作成（半径指定、外殻のみ）
  - 壁作成（2点間、高さ指定、地面検出）
  - 家作成（ドア・窓・屋根付き）
  - 範囲塗りつぶし（3次元範囲指定）
- **ゲーム制御**: モード変更、時間設定、天候操作
- **セキュリティ機能（完全実装）**:
  - レート制限（10コマンド/秒、自動リセット機能付き）
  - ローカルネットワーク限定接続（IP制限）
  - 危険なブロック/コマンドの制限
  - 接続数制限（最大10接続）
- **エラーハンドリング**: 統一されたJSON形式レスポンス
- **リアルタイムデータ同期**: Scratch側でのキャッシュ機能
- **非同期処理**: CollaborationCoordinatorによる並行処理

### 🚧 開発中/計画中
- 協調サーバー（ポート14712）による複数ワールド間通信
- 時間制限機能
- 先生用管理機能
- パフォーマンス最適化
- エージェントシステム（Scratch上のキャラクター）

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

### プロジェクト分析レポート
最新のプロジェクト分析は[PROJECT_ANALYSIS_2025-07-26.md](PROJECT_ANALYSIS_2025-07-26.md)を参照してください。

### 📚 ドキュメント索引
すべてのドキュメントへのアクセスは[docs/INDEX.md](docs/INDEX.md)からどうぞ。

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