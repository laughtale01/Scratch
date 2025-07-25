# 📋 Claude Code引き継ぎ資料
## Minecraft協調学習システム開発プロジェクト

---

## 🎯 プロジェクト概要

### 目的
ScratchとMinecraftを連携した小学生向け協調学習システムの開発

### 主な機能
- 友達の作品世界への招待・訪問システム
- 安全な接続切り替え機能
- 子供向けの直感的なScratchブロック
- 安全機能（時間制限、緊急帰宅）

---

## 📁 プロジェクト場所

### メインプロジェクト
D:\minecraft_collaboration_project\

### 関連プロジェクト
D:\multi_track_app_enterprise\     # Flutter マルチトラックアプリ
D:\multi_track_app_landing\        # Flutter ランディングページ

---

## ✅ 環境設定（完了済み）

### Java環境
- **Java 17**: デフォルト（Flutter/Android開発用）
- **Java 11**: Minecraft Forge用
    - 場所: ``C:\Program Files\Eclipse Adoptium\jdk-11.0.26.4-hotspot``
    - gradle.properties設定済み

### Node.js環境
- **Node.js**: v24.4.0
- **npm**: v11.4.2

### 開発ツール
- **Git**: v2.46.0
- **Chocolatey**: v2.5.0
- **Minecraft Java Edition**: インストール済み（modsフォルダ確認済み）

---

## 🏗️ プロジェクト構造
D:\minecraft_collaboration_project
├── remote-controller-mod\           # Minecraft Mod開発
│   ├── src\                        # Javaソースコード
│   ├── build.gradle               # ビルド設定
│   ├── gradle.properties          # Java 11設定
│   ├── gradlew.bat               # Gradleラッパー
│   └── settings.gradle           # プロジェクト設定
├── scratch-extension\              # Scratch拡張開発
│   ├── src\index.js              # メイン拡張コード
│   ├── package.json              # Node.js設定
│   ├── webpack.config.js         # ビルド設定
│   ├── .babelrc                  # Babel設定
│   ├── dist\                     # ビルド成果物
│   └── node_modules\             # 依存関係
├── docs\                          # ドキュメント
├── assets\                        # リソース
├── scripts\                       # スクリプト
├── config\                        # 設定
├── README.md                      # プロジェクト文書
└── .gitignore                    # Git除外設定

---

## 📋 現在の進捗状況

### ✅ 完了項目
- [x] 開発環境構築
- [x] プロジェクト構造作成
- [x] RemoteControllerMod基盤準備（takecxプロジェクトクローン済み）
- [x] Scratch拡張基盤準備
- [x] 基本Scratchブロック実装（12個）
- [x] WebSocket通信フレームワーク
- [x] package.json設定
- [x] webpack設定
- [x] Java 11環境設定

### ❌ 未完了項目
- [ ] RemoteControllerModビルドテスト
- [ ] Scratch拡張ビルドテスト
- [ ] WebSocket通信実装（Minecraft Mod側）
- [ ] 招待・訪問機能実装
- [ ] 安全機能実装

---

## 🎯 実装済みScratchブロック

### 接続管理
- ``🔌 Minecraftに接続``: WebSocket接続開始
- ``📡 接続状態を確認``: 接続状況表示

### 招待システム
- ``📧 [FRIEND]さんを招待``: 友達を自分のワールドに招待
- ``📬 招待通知の数``: 受信した招待件数表示

### 訪問システム
- ``🚪 [FRIEND]さんの世界に訪問申請``: 友達のワールドへの訪問申請
- ``✅ [VISITOR]さんの訪問を承認``: 訪問申請の承認
- ``🌍 現在いる世界``: 現在のワールド名表示

### 帰宅システム
- ``🏠 自分のワールドに帰る``: 通常の帰宅処理
- ``🚨 緊急帰宅``: 即座に帰宅

---

## 🚀 次に実行すべきステップ

### 1. RemoteControllerModビルドテスト
``````bash
cd D:\minecraft_collaboration_project\remote-controller-mod

# 1. 権限確認（管理者権限推奨）
# 2. Java 11環境確認
"C:\Program Files\Eclipse Adoptium\jdk-11.0.26.4-hotspot\bin\java" -version

# 3. ビルド実行
.\gradlew.bat clean build

# 4. 開発用Minecraft起動テスト
.\gradlew.bat runClient
2. Scratch拡張ビルドテスト
bashcd D:\minecraft_collaboration_project\scratch-extension

# 1. 依存関係確認
npm list

# 2. ビルド実行
npm run build

# 3. 成果物確認
ls dist\
3. WebSocket通信実装（優先）
Minecraft Mod側にWebSocketサーバー機能を実装

⚙️ 重要な設定値
WebSocket設定

ポート: 14711
プロトコル: ws://localhost:14711
メッセージ形式: JSON

Java設定
properties# gradle.properties
org.gradle.java.home=C:\\Program Files\\Eclipse Adoptium\\jdk-11.0.26.4-hotspot
Node.js設定
json// package.json (主要部分)
{
  "name": "minecraft-collaboration-extension",
  "scripts": {
    "build": "webpack --mode=production",
    "dev": "webpack --mode=development --watch"
  }
}

🔧 開発コマンド集
Minecraft Mod
bash# 基本操作
cd D:\minecraft_collaboration_project\remote-controller-mod
.\gradlew.bat build                    # ビルド
.\gradlew.bat runClient               # 開発用Minecraft起動
.\gradlew.bat clean                   # クリーン
.\gradlew.bat tasks                   # 利用可能タスク一覧

# Java切り替え（必要に応じて）
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-11.0.26.4-hotspot
Scratch拡張
bash# 基本操作
cd D:\minecraft_collaboration_project\scratch-extension
npm run build                        # プロダクションビルド
npm run dev                          # 開発モード（自動ビルド）
npm install                          # 依存関係再インストール
npm list                            # インストール済みパッケージ確認

🐛 トラブルシューティング
よくある問題
Java関連
問題: Gradle実行時にJavaバージョンエラー
解決:
bash# Java 11が正しく設定されているか確認
type "C:\Program Files\Eclipse Adoptium\jdk-11.0.26.4-hotspot\bin\java" -version
# gradle.propertiesの設定確認
Node.js関連
問題: npm installで権限エラー
解決:
bash# 管理者権限でPowerShell実行
# または npm cache clean --force
WebSocket関連
問題: 接続できない
解決:

Minecraftが起動しているか確認
ポート14711が使用可能か確認
ファイアウォール設定確認


📚 参考情報
元プロジェクト

RemoteControllerMod: https://github.com/takecx/RemoteControllerMod
takecxさんの実装: 基盤として活用済み

技術スタック

Minecraft Mod: Java + Minecraft Forge 1.16.5
Scratch拡張: JavaScript + WebSocket
ビルドツール: Gradle + Webpack

開発方針

小学生向けの分かりやすいUI
安全性を最優先
段階的な機能実装
エラーハンドリングの充実


🎓 実装方針・注意事項
子供向け設計

直感的なブロック名: 絵文字と分かりやすい日本語
エラーメッセージ: 子供が理解できる表現
安全機能: 時間制限、緊急帰宅機能必須

セキュリティ

ローカルネットワーク限定: 外部接続は不可
招待制: 知らない人からの接続拒否
先生用管理機能: 全体統制可能

技術的考慮

エラーハンドリング: すべての通信にエラー処理
接続安定性: 再接続機能実装
ログ出力: デバッグ用の詳細ログ


📞 Claude Code作業開始時の確認事項

環境確認: Java, Node.js, プロジェクト場所
権限確認: 管理者権限での実行
目標設定: まずビルドテストから開始
段階的開発: 小さな機能から実装・テスト
ドキュメント更新: 実装完了後にこの資料を更新