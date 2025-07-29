# 🚀 Claude Code スタートアップガイド
## Minecraft協調学習システム - 現状把握手順

---

## 📋 Claude Code起動時の確認手順

### 1️⃣ 最初に必ず確認するファイル

```bash
# プロジェクトルートで実行
1. CLAUDE.md を読む
2. docs/INDEX.md を開く（全ドキュメントへの索引）
```

### 2️⃣ プロジェクト現状把握

#### 必須確認ドキュメント（優先順）
1. **docs/INDEX.md** - ドキュメント索引（ここから始める）
2. **PROJECT_STATUS_REPORT_2025-07-26.md** - 最新の総合状況
3. **docs/development/SYSTEM_ARCHITECTURE_COMPLETE.md** - 完全なシステム構造

#### 作業内容による追加確認
- **機能追加時**: docs/development/COMPREHENSIVE_DEVELOPMENT_GUIDE.md
- **バグ修正時**: docs/troubleshooting.md
- **テスト実装時**: docs/testing-guide.md, docs/tdd-guidelines.md

---

## 🔍 プロジェクト構造の把握

### コアディレクトリ
```
minecraft_collaboration_project/
├── minecraft-mod/        # Java Minecraft Mod
├── scratch-extension/    # JavaScript Scratch拡張
├── docs/                # ドキュメント
│   ├── INDEX.md        # ⭐ ドキュメント索引
│   └── development/    # 開発ガイド
└── tests/              # テストファイル
```

### 重要ファイル
- **MinecraftCollaborationMod.java** - Modエントリーポイント
- **index.js** - Scratch拡張メイン（19種類のブロック）
- **WebSocketHandler.java** - 通信管理（ポート14711）

---

## 💡 作業開始前のチェックリスト

### ✅ 環境確認
```bash
# Java確認
java -version  # 17以上

# Node.js確認
node --version  # v24.4.0以上

# プロジェクト状態
git status
```

### ✅ ドキュメント確認
- [ ] CLAUDE.md読了
- [ ] docs/INDEX.md確認
- [ ] 作業に関連するドキュメント特定
- [ ] 最新の状態レポート確認

### ✅ コード理解
- [ ] 影響範囲の特定
- [ ] 既存機能の確認
- [ ] テストの有無確認

---

## 🚨 重要な注意事項

### 回帰防止ルール
1. **既存機能を削除しない**
2. **スコープ外の変更をしない**
3. **動作する機能を壊さない**
4. **テストを破壊しない**

### 作業前の確認
1. 影響を受ける既存機能をすべて特定
2. 現在の動作を確認/テスト
3. 変更しない部分を明確化
4. 実装計画に記載

---

## 📊 プロジェクト状態サマリー（2025年7月26日現在）

### 完成度: 85%
- ✅ WebSocket通信: 完全動作
- ✅ 基本機能: 19種類のScratchブロック実装済み
- ✅ 協調機能: 招待・訪問システム実装済み
- ⚠️ 協調サーバー: 未実装（ポート14712）
- ⚠️ 統合テスト: 整備中

### 技術スタック
- **Minecraft**: 1.20.1 + Forge 47.2.0
- **Java**: 17
- **Node.js**: v24.4.0+
- **WebSocket**: ポート14711

---

## 🔗 クイックリンク

### 開発ガイド
- [包括的開発ガイド](docs/development/COMPREHENSIVE_DEVELOPMENT_GUIDE.md)
- [システムアーキテクチャ](docs/development/SYSTEM_ARCHITECTURE_COMPLETE.md)

### セットアップ
- [統合セットアップガイド](docs/setup/UNIFIED_SETUP_GUIDE.md)

### トラブルシューティング
- [トラブルシューティング](docs/troubleshooting.md)
- [プロジェクト分析](PROJECT_ANALYSIS_2025-07-26.md)

---

## 🎯 推奨ワークフロー

1. **このガイドを読む**
2. **docs/INDEX.mdで必要なドキュメントを特定**
3. **関連ドキュメントを読む**
4. **コードを確認**
5. **作業計画を立てる**
6. **実装・テスト**
7. **ドキュメント更新**

---

最終更新: 2025年7月26日