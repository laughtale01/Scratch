# 📚 Scratch × Minecraft 協調学習システム - ドキュメント索引

Scratch公式フォークベースのMinecraft拡張機能プロジェクトのドキュメントガイド

---

## 🚀 クイックスタート

**開発者向けクイックスタート:**
- [CLAUDE.md](../CLAUDE.md) - プロジェクト概要と開発規約
- [README.md](../README.md) - プロジェクトの詳細説明
- [scratch-gui/README.md](../scratch-gui/README.md) - Scratch GUI公式フォーク

## 🏠 メインドキュメント

### プロジェクト概要
- [README.md](../README.md) - プロジェクトの概要と基本情報
- [LICENSE](../LICENSE) - MITライセンス

### プロジェクト状態
- **[最新] VSCode統合最適化完了 (2025-08-08)**
  - Java 17自動設定、ワンクリックビルド実現
  - タスクランナーによる自動デプロイ対応
  - Git Bash統合、開発効率大幅向上
- **WebSocket通信動作確認完了 (2025-08-05)**
  - ping/pong、chat、getPlayerPos全て正常動作
  - NoClassDefFoundError解決（リフレクション使用）

---

## 📋 セットアップ・利用ガイド

### セットアップ
- [setup/QUICK_START.md](setup/QUICK_START.md) - クイックスタートガイド ⭐新規・推奨
- [setup/UNIFIED_SETUP_GUIDE.md](setup/UNIFIED_SETUP_GUIDE.md) - 統合セットアップガイド
- [user/USER_MANUAL.md](user/USER_MANUAL.md) - ユーザーマニュアル

### 開発ガイド
- [architecture.md](architecture.md) - システムアーキテクチャ
- [dependencies.md](dependencies.md) - 依存関係の詳細
- [patterns.md](patterns.md) - 実装パターン
- [tdd-guidelines.md](tdd-guidelines.md) - TDD実装ガイドライン
- **[vscode-integration.md](vscode-integration.md) - VSCode統合ガイド** ⭐新規 (2025-08-08)

---

## 🔧 技術ドキュメント

### Scratch GUI統合
- [scratch-gui/](../scratch-gui/) - Scratch公式フォーク (v5.1.88)
- **開発サーバー**: http://localhost:8601 (既に起動中)
- `scratch-gui/src/lib/libraries/extensions/minecraft/` - Minecraft拡張定義
- `scratch-extension/src/index.js` - Minecraft拡張ソースコード

### テスト・品質管理
- [testing-guide.md](testing-guide.md) - テストガイド

### トラブルシューティング
- [troubleshooting.md](troubleshooting.md) - トラブルシューティングガイド
- [regression-prevention.md](regression-prevention.md) - リグレッション防止策

---

## 🚀 デプロイ・リリース

### デプロイメント
- [deployment/FIREBASE.md](deployment/FIREBASE.md) - Firebase Hostingガイド ⭐新規
- [deployment/GITHUB_PAGES.md](deployment/GITHUB_PAGES.md) - GitHub Pagesガイド ⭐新規
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - デプロイメント総合ガイド

### リリース管理
- [DEPLOY_INSTRUCTIONS.md](../DEPLOY_INSTRUCTIONS.md) - デプロイメント手順
- [QUICK_DEPLOYMENT.md](../QUICK_DEPLOYMENT.md) - クイックデプロイガイド

---

## 📁 プロジェクト構造

### 構造ガイド
- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - プロジェクト構造の詳細説明

### Minecraft Mod関連
- [minecraft-mod/README_INSTALLATION.md](../minecraft-mod/README_INSTALLATION.md) - Modインストールガイド

---

## 🗄️ アーカイブ

過去のドキュメントや作業ログは[archives/](archives/)ディレクトリに保存されています：

### セッションログ
- SESSION_LOG_2025-07-14.md
- SESSION_LOG_2025-07-15.md

### 過去の状態レポート
- PROJECT_STATUS_2025-07-20.md
- PROJECT_STATUS_2025-07-25.md

### 旧セットアップガイド
- SETUP_GUIDE.md
- SIMPLE_SETUP.md
- COMPLETE_SETUP_GUIDE.md
- QUICKSTART.md

### GitHub関連（統合済み）
- GITHUB_CREATE_REPO.md
- GITHUB_PRIVATE_REPO_PUSH.md
- その他GitHub関連ドキュメント

---

## 🔍 ドキュメント検索のヒント

### 目的別
- **初めての方**: [README.md](../README.md) → [QUICK_START.md](setup/QUICK_START.md)
- **開発者**: [CLAUDE.md](../CLAUDE.md) → [scratch-gui/README.md](../scratch-gui/README.md)
- **トラブル対応**: [troubleshooting.md](troubleshooting.md)
- **デプロイ作業**: [QUICK_DEPLOYMENT.md](../QUICK_DEPLOYMENT.md)

### キーワード別
- **Scratch GUI**: scratch-gui/README.md, CLAUDE.md
- **Minecraft拡張**: scratch-gui/static/extensions/minecraft-unified.js
- **WebSocket**: minecraft-mod/, ポート14711
- **ビルド**: README.md のセットアップ手順
- **デプロイ**: QUICK_DEPLOYMENT.md, firebase.json

---

## 📝 ドキュメント管理方針

1. **最新性**: 常に最新情報はREADME.mdとこのINDEX.mdに反映
2. **整理**: 古いドキュメントはarchives/へ移動
3. **統合**: 重複する内容は統合して一元管理
4. **アクセス性**: 明確な構造と索引で素早くアクセス

---

最終更新: 2025年8月8日

### 📝 最新の変更点 (2025-08-08)
- VSCode統合最適化完了
- Java 17自動環境設定スクリプト追加
- タスクランナーによるワンクリックビルド・デプロイ
- 開発ドキュメント更新（vscode-integration.md追加）

### 📝 前回の変更点 (2025-08-05)
- WebSocket通信の完全動作確認
- NoClassDefFoundError修正（リフレクションベースの実装）
- test-websocket.htmlデバッグツール追加