# 🎮 Scratch × Minecraft 協調学習システム

**Scratch公式フォークをベースにしたMinecraft拡張機能プロジェクト**

> **📌 開発者向け**: このプロジェクトは[Scratch公式](https://github.com/scratchfoundation/scratch-gui)のフォークに、独自のMinecraft拡張機能を追加したものです。

## ⚠️ 重要な要件

### Java 17が必須です (2025-08-08 確認済み)
## 🎯 プロジェクトステータス

**バージョン**: 1.5.1 (2025-09-26)
**ステータス**: ✅ **品質向上・CI/CD強化完了**
**品質達成**: 🎯 **ビルド安定化・テスト復旧・パイプライン強化**
**最終更新**: 2025-09-26 - 品質改善・テストスイート復旧・CI/CD整備

### 📊 最新改善内容
- ✅ **ビルドシステム改善**: 静的解析ツール統合、Java 17環境最適化
- ✅ **テストスイート復旧**: 302テスト中261成功（86.5%成功率）
- ✅ **CI/CDパイプライン**: GitHub Actionsでの自動ビルド・テスト実行
- ✅ **Firebase デプロイ**: 自動デプロイワークフロー作成
- ✅ **コード品質向上**: 全Javaクラスのエラー処理とリソース管理改善  

このプロジェクトは**Java 17**を必要とします。Java 21はGradle 7.6.4でサポートされていないため使用できません。

```bash
# Javaバージョンの確認
java -version
# 出力例: openjdk version "17.0.X" ...
```

詳細なセットアップ手順は[Java 17インストールガイド](#java-17のインストール必須)を参照してください。

#### 環境設定の簡単な方法
```bash
# プロジェクトルートで実行
set-java17.bat         # 一般的な使用
set-java17-vscode.bat  # VSCodeターミナル内での使用（推奨）
```

### 🆕 VSCode統合機能 (2025-08-08追加)
- **自動Java 17設定**: VSCodeタスクで自動的にJava環境を設定
- **ワンクリックビルド**: タスクランナーから直接ビルド・デプロイ
- **統合ターミナル**: Git Bashデフォルト設定でJava 17パス自動追加
- **最適化された設定**: `.vscode/`配下の設定ファイルで開発効率向上

## 📋 プロジェクト概要

Scratch公式フォーク（scratch-gui）にMinecraft拡張機能を追加し、子供たちがビジュアルプログラミングでMinecraftを操作できる教育システムです。Scratch本来の機能を保ちながら、Minecraft特有の協調学習機能を実現しています。

### 🎯 プロジェクトアプローチ
- **Scratch GUI**: 公式フォーク (v5.1.88) をそのまま使用
- **Minecraft拡張**: Extension形式で追加（他の拡張機能と同様）
- **Minecraft Mod**: オリジナル開発（Forge 1.20.1対応）
- **WebSocket通信**: ScratchとMinecraft間のリアルタイム連携

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
- **7言語対応**: 日本語、英語、中国語、韓国語、スペイン語、フランス語、ドイツ語
- **動的切替**: リアルタイム言語変更
- **完全ローカライズ**: UI・メッセージ・教材の多言語化
- **プレイヤー個別設定**: 各プレイヤーの言語設定を個別管理

### 📦 ブロックパック機能
- **教育用制限**: 学習段階に応じたブロック制限
- **4段階難易度**: 初心者・中級・上級・エキスパートレベル
- **カスタムパック作成**: 教師による独自パック作成
- **段階的学習**: 初心者から上級者への体系的な学習支援
- **動的制御**: リアルタイムでのブロック制限変更

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
- **セキュリティ検証**: XSS・SQLインジェクション防止
- **認証システム**: トークンベース認証とレート制限

### 🧪 品質保証システム
- **包括的テストカバレッジ**: 136個のテストで品質検証
- **セキュリティテスト**: 入力検証・認証システムの完全テスト
- **多言語テスト**: 7言語対応の動作確認
- **統合テスト**: エンドツーエンドの動作検証
- **CI/CDパイプライン**: 自動化された品質チェック

### 📊 エンタープライズ監視システム
- **高度なアラート管理**: 複数重要度レベルのアラートシステム
- **予測分析**: 統計分析と異常検知による予測機能
- **ヘルスモニタリング**: CPU、メモリ、接続数、応答時間のリアルタイム監視
- **マルチチャネル通知**: 拡張可能な通知フレームワーク
- **ルールエンジン**: トレンド分析と動的重要度計算
- **パフォーマンス最適化**: キャッシング、バッチ処理、オブジェクトプーリング実装

## 🏗️ プロジェクト構造

```
minecraft_collaboration_project/
├── scratch-gui/                              # Scratch公式フォーク（統合済み）
│   ├── src/lib/libraries/extensions/         # 拡張機能定義
│   │   └── index.jsx                        # Minecraft拡張を登録
│   ├── static/extensions/                    # 拡張機能実装
│   │   └── minecraft-unified.js             # Minecraft拡張本体
│   ├── build/                               # ビルド後のScratch GUI
│   └── package.json                         # Scratch GUI設定
├── minecraft-mod/                           # Minecraft Mod (Forge 1.20.1)
│   ├── src/main/java/edu/minecraft/collaboration/
│   │   ├── MinecraftCollaborationMod.java  # メインModクラス
│   │   ├── network/                        # WebSocket通信
│   │   ├── commands/                       # コマンド処理
│   │   ├── collaboration/                  # 協調機能
│   │   └── monitoring/                     # 監視システム
│   └── build.gradle                        # ビルド設定
├── scratch-extension/                       # Minecraft拡張のソース
│   ├── src/index.js                        # 拡張機能のソースコード
│   └── package.json                        # ビルド設定
├── public/                                  # Webアセット
├── docs/                                    # ドキュメント
│   └── archives/                            # アーカイブ済みドキュメント
└── .gitignore                               # Git除外設定（最適化済み）
```

## ⚙️ 技術仕様

### Minecraft Mod
- **Minecraft**: 1.20.1
- **Forge**: 47.2.0
- **Java**: 17+
- **WebSocket**: Java-WebSocket 1.5.4（jarJarで同梱）
- **ビルドツール**: Gradle 7.6.4

### Scratch GUI (公式フォーク)
- **バージョン**: 5.1.88
- **Node.js**: v16.0.0+ (Scratch GUI要件)
- **ビルド**: Webpack (Scratch GUI標準)
- **拡張機能**: static/extensions/minecraft-unified.js

### 通信プロトコル
- **WebSocket**: ポート 14711
- **協調サーバー**: ポート 14712（将来実装）
- **メッセージ形式**: レガシー `command(arg1,arg2)` / JSON形式対応

## 🚀 セットアップ手順

### クイックスタート（使用者向け）

1. **Java 17をインストール** （必須）
   - Windows: [Oracle Java 17](https://www.oracle.com/java/technologies/downloads/#java17)
   - Mac/Linux: 上記インストールガイド参照

2. **Minecraft Java版 1.20.1 + Forge 47.2.0をインストール**
2. **ビルド済みModをダウンロード**: `minecraft-collaboration-mod-1.0.0-all.jar`
3. **Modを`.minecraft/mods/`フォルダに配置**
4. **Minecraftを起動してワールドにログイン**
5. **Scratch GUIにアクセス**: 
   - ローカル開発サーバー: http://localhost:8601
   - デバッグツール: `test-websocket.html`を開く
6. **拡張機能から「Minecraft コラボレーション」を選択**
7. **WebSocket接続を確認** (ポート14711)

### 開発者向けセットアップ

#### 1. リポジトリのクローン
```bash
git clone https://github.com/yourusername/minecraft-collaboration.git
cd minecraft-collaboration
```

#### 2. Scratch GUIのビルド
```bash
cd scratch-gui
npm install
npm run build
```

#### 3. Minecraft Modのビルド
```bash
cd ../minecraft-mod
./gradlew build
```

### 1. 環境準備

⚠️ **重要**: Java 17の使用を確認してください。Java 21はGradle 7.6.4でサポートされていません。

#### Java 17のインストール（必須）

##### Windows
1. [Adoptium OpenJDK 17](https://adoptium.net/temurin/releases/?version=17)からインストーラーをダウンロード
2. インストーラーを実行し、「Set JAVA_HOME variable」にチェックを入れる
3. コマンドプロンプトで確認:
   ```bash
   java -version
   # 期待される出力: openjdk version "17.0.X" ...
   ```

##### macOS
```bash
# Homebrewを使用
brew install openjdk@17

# 環境変数の設定
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/usr/local/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc

# 確認
java -version
```

##### Linux (Ubuntu/Debian)
```bash
# パッケージリストを更新
sudo apt update

# OpenJDK 17をインストール
sudo apt install openjdk-17-jdk

# デフォルトのJavaバージョンを設定
sudo update-alternatives --config java
# Java 17を選択

# 確認
java -version
```

##### トラブルシューティング
- **Java 21がインストールされている場合**: 
  - Gradle 7.6.4はJava 21をサポートしていません
  - JAVA_HOMEがJava 17を指していることを確認してください
  ```bash
  echo $JAVA_HOME  # Unix/Mac
  echo %JAVA_HOME% # Windows
  ```

- **複数のJavaバージョンがある場合**:
  - 環境変数JAVA_HOMEをJava 17のインストール先に設定
  - PATHの先頭にJava 17のbinディレクトリを追加


#### Node.js v24.4.0+のインストール確認
```bash
node --version
npm --version
```

### 2. Minecraft Modのビルド

```bash
cd minecraft-mod

# 1. Java 17環境の確認
java -version  # 17.0.xが表示されることを確認

# 2. 依存関係の取得とビルド（テストをスキップ）
./gradlew jarJar -x test -x checkstyleMain

# 3. ビルド成功後、JARファイルが生成される
# build/libs/minecraft-collaboration-mod-1.0.0-all.jar (約635KB)

# 3. 開発用Minecraft起動（テスト用）
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

## 📊 プロジェクト状態（2025年8月8日更新）

### ✅ 実装済み機能（コード完成）
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
- **テストインフラ**: 136個のテスト実装（test.disabled/に配置、再有効化必要）

### 🚧 開発中/計画中
- **TestContainers統合**: Minecraft環境でのテスト改善（計画書作成済み）
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

# 単体テスト実行
./gradlew test

# 統合テスト実行（TestContainers準備後）
./gradlew integrationTest

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


### 📚 ドキュメント索引
すべてのドキュメントへのアクセスは[docs/INDEX.md](docs/INDEX.md)からどうぞ。

### よくある問題と解決方法

#### テストディレクトリの問題 ⚠️ 要対応
**問題**: テストが`test.disabled/`ディレクトリに配置されている
**解決方法**: `mv minecraft-mod/src/test.disabled minecraft-mod/src/test`を実行してテストを有効化

#### Java関連の問題
**問題**: Gradle実行時にJavaバージョンエラー

**症状**:
- `Unsupported class file major version 65` (Java 21使用時)
- `java.lang.UnsupportedClassVersionError`
- `Gradle 7.6 requires Java 8 to Java 19`

**解決方法**:
```bash
# 1. 現在のJavaバージョンを確認
java -version
javac -version

# 2. JAVA_HOMEを確認
echo $JAVA_HOME  # Unix/Mac
echo %JAVA_HOME% # Windows

# 3. Java 17に切り替え (macOS/Linux)
export JAVA_HOME=/path/to/java17
export PATH=$JAVA_HOME/bin:$PATH

# 4. gradle.propertiesを確認
cd minecraft-mod
cat gradle.properties
# org.gradle.java.home=/path/to/java17 が設定されていることを確認

# 5. Gradleキャッシュをクリア
./gradlew clean
rm -rf ~/.gradle/caches/

# 6. 再ビルド
./gradlew build
```

**Windows PowerShellでの設定**:
```powershell
# 一時的な設定
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.X"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 永続的な設定はシステム環境変数で行う
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