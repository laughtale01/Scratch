# 📦 GitHub統合ガイド

このドキュメントは、プロジェクトのGitHub関連のすべての操作をまとめた統合ガイドです。

---

## 🚀 クイックスタート

### 1. リポジトリの作成
```bash
# GitHubで新規リポジトリを作成
# リポジトリ名: minecraft-collaboration-system
# 可視性: Private（教育機関向け）
```

### 2. 初期プッシュ
```bash
git init
git add .
git commit -m "Initial commit: Minecraft協調学習システム v1.0.0"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/minecraft-collaboration-system.git
git push -u origin main
```

---

## 🔐 アクセストークンの設定

### Personal Access Token (PAT)の作成
1. GitHub → Settings → Developer settings → Personal access tokens
2. "Generate new token (classic)" をクリック
3. 必要な権限:
   - `repo` (Full control)
   - `workflow` (Update GitHub Action workflows)
4. トークンを安全に保存

### トークンの使用
```bash
# HTTPSでのクローン
git clone https://YOUR_TOKEN@github.com/USERNAME/REPO.git

# 既存リポジトリへの設定
git remote set-url origin https://YOUR_TOKEN@github.com/USERNAME/REPO.git
```

---

## 📋 リリース手順

### 1. バージョンタグの作成
```bash
git tag -a v1.0.0 -m "初回リリース: 基本機能実装完了"
git push origin v1.0.0
```

### 2. GitHub Releaseの作成
1. Releases → "Create a new release"
2. タグを選択: v1.0.0
3. リリースノートを記入
4. アセットをアップロード:
   - `minecraft-collaboration-mod-1.0.0-all.jar`
   - `minecraft-collaboration-extension.js`

### 3. リリースノートテンプレート
```markdown
## 🎉 Minecraft協調学習システム v1.0.0

### ✨ 新機能
- WebSocket通信によるScratch-Minecraft連携
- 19種類の操作ブロック
- 協調学習機能（招待・訪問）
- 安全機能（緊急帰宅）

### 📦 ダウンロード
- **Minecraft Mod**: minecraft-collaboration-mod-1.0.0-all.jar
- **Scratch拡張**: minecraft-collaboration-extension.js

### 📚 セットアップ
[セットアップガイド](docs/setup/SETUP_GUIDE.md)を参照

### 🐛 既知の問題
- 複数同時接続時の競合（対応予定）
```

---

## 🌐 GitHub Pages設定

### 1. 有効化
Settings → Pages → Source → Deploy from a branch → main → /docs

### 2. カスタムドメイン（オプション）
```
minecraft-collab.example.edu
```

### 3. ドキュメントサイト構造
```
docs/
├── index.md          # トップページ
├── setup/            # セットアップガイド
├── api/              # API仕様
└── _config.yml       # Jekyll設定
```

---

## 🔄 ワークフロー

### ブランチ戦略
```
main
├── develop          # 開発ブランチ
├── feature/*        # 機能開発
├── hotfix/*         # 緊急修正
└── release/*        # リリース準備
```

### プルリクエストテンプレート
`.github/pull_request_template.md`:
```markdown
## 概要
変更内容の簡潔な説明

## 変更点
- [ ] 機能A
- [ ] バグ修正B

## テスト
- [ ] ユニットテスト
- [ ] 統合テスト
- [ ] 動作確認

## スクリーンショット
（該当する場合）
```

---

## 🛡️ セキュリティ

### 1. 依存関係の管理
```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "npm"
    directory: "/scratch-extension"
    schedule:
      interval: "weekly"
  
  - package-ecosystem: "gradle"
    directory: "/minecraft-mod"
    schedule:
      interval: "weekly"
```

### 2. シークレットの管理
- 環境変数を使用
- `.env`ファイルは絶対にコミットしない
- GitHub Secretsを活用

---

## 📊 プロジェクト管理

### Issue管理
- バグ報告テンプレート
- 機能要望テンプレート
- ラベルの活用（bug, enhancement, documentation等）

### プロジェクトボード
1. To Do
2. In Progress
3. Review
4. Done

---

## 🚨 トラブルシューティング

### よくある問題

#### プッシュ失敗
```bash
# 認証エラーの場合
git config --global credential.helper manager
git push -u origin main
```

#### 大きなファイル
```bash
# Git LFSの使用
git lfs track "*.jar"
git add .gitattributes
git commit -m "Add Git LFS tracking"
```

---

## 📝 ベストプラクティス

1. **コミットメッセージ**
   - 明確で簡潔に
   - 日本語OK（プロジェクトの性質上）

2. **ブランチ名**
   - feature/add-collaboration
   - fix/websocket-error
   - docs/update-readme

3. **リリースサイクル**
   - 月次でマイナーリリース
   - 緊急修正は即座に

---

## 🔗 関連リンク

- [GitHub Docs](https://docs.github.com)
- [Git LFS](https://git-lfs.github.com/)
- [GitHub Actions](https://github.com/features/actions)