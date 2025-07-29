# 📁 Minecraft協調学習システム - プロジェクト構造ガイド

## 概要
このドキュメントは、プロジェクトのディレクトリ構造と各ファイルの役割を説明します。

---

## 🗂️ メインディレクトリ

### `/minecraft-mod/` - Minecraft Mod本体
Minecraft Forge 1.20.1対応のModソースコードとビルド設定。

#### 主要ファイル
- `build.gradle` - Gradleビルド設定（jarJar含む）
- `src/main/java/` - Javaソースコード
  - `MinecraftCollaborationMod.java` - メインModクラス
  - `network/` - WebSocket通信関連
  - `commands/` - コマンド処理
  - `collaboration/` - 協調機能
- `src/main/resources/` - リソースファイル
  - `META-INF/mods.toml` - Mod設定

### `/scratch-extension/` - Scratch拡張機能
Scratch 3.0用のMinecraft操作拡張機能。

#### 主要ファイル
- `src/index.js` - メイン拡張機能（19種類のブロック定義）
- `package.json` - Node.js依存関係
- `webpack.config.js` - ビルド設定
- `dist/` - ビルド成果物

### `/docs/` - ドキュメント
プロジェクトの設計・開発・運用に関する文書。

#### 主要ドキュメント
- `architecture.md` - アーキテクチャ設計
- `troubleshooting.md` - トラブルシューティング
- `testing-guide.md` - テストガイド
- `claude-code-handoff.md` - Claude Code引き継ぎ資料

---

## 📄 ルートディレクトリの重要ファイル

### プロジェクト管理
- `README.md` - プロジェクト概要とセットアップ
- `LICENSE` - MITライセンス
- `CLAUDE.md` - Claude Code設定と規約
- `PROJECT_ANALYSIS_2025-01-26.md` - 最新のプロジェクト分析

### ビルド・実行スクリプト
- `run-minecraft.bat` - Minecraft起動スクリプト
- `start-all.bat` - 全サービス起動
- `build-mod.bat` - Modビルドスクリプト

### 設定ファイル
- `package.json` - ルートのNode.js設定
- `.gitignore` - Git除外設定
- `_config.yml` - GitHub Pages設定

---

## 🚮 整理が必要なファイル

### 画像ファイル
- `001.jpg`～`016.jpg` - 用途不明の番号付き画像
  → `assets/`フォルダへの移動を推奨

### 古いログ・レポート
- `SESSION_LOG_*.md` - 過去の作業ログ
- `PROJECT_STATUS_2025-07-*.md` - 古い状態レポート
  → `docs/archives/`への移動を推奨

### 重複ドキュメント
- 複数のセットアップガイド
- 類似のGitHub関連ドキュメント
  → 統合・整理が必要

### scratch-guiディレクトリ
- Scratch GUI全体のクローン（巨大）
- プロジェクトには拡張機能のみ必要
  → 別リポジトリ管理を推奨

---

## 🏗️ 推奨ディレクトリ構造

```
minecraft_collaboration_project/
├── minecraft-mod/          # Minecraft Mod
├── scratch-extension/      # Scratch拡張
├── docs/                   # ドキュメント
│   ├── setup/             # セットアップガイド
│   ├── development/       # 開発ガイド
│   ├── api/              # API仕様
│   └── archives/         # 過去のログ
├── assets/               # 画像・リソース
├── scripts/              # ビルド・テストスクリプト
├── tests/                # 統合テスト
└── config/              # 設定ファイル
```

---

## 📝 メンテナンス指針

1. **新規ファイル追加時**
   - 適切なディレクトリに配置
   - このドキュメントを更新

2. **ファイル移動・削除時**
   - 関連ドキュメントの参照を更新
   - Git履歴に記録

3. **定期的な整理**
   - 四半期ごとに不要ファイルを確認
   - ドキュメントの最新性を維持