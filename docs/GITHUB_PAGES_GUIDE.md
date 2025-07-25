# 📚 GitHub Pages 公開ガイド

## 💰 料金

### 無料プラン（GitHub Free）
- **料金**: **完全無料**
- **公開リポジトリ**: 無制限
- **プライベートリポジトリ**: 無制限（GitHub Pages利用可能）
- **容量制限**: 1GB
- **帯域制限**: 月100GB
- **ビルド制限**: 月10回まで

### 有料プラン（必要な場合のみ）
- GitHub Pro: $4/月（より多くのビルド回数）
- GitHub Team: $4/月/ユーザー（チーム機能）

**📝 本プロジェクトは無料プランで十分です！**

## 🚀 公開手順

### 1. GitHubアカウント作成（未登録の場合）
1. [github.com](https://github.com) にアクセス
2. 「Sign up」をクリック
3. メールアドレス、パスワード、ユーザー名を入力
4. メール認証を完了

### 2. リポジトリ作成
```bash
# ローカルで初期化
git init
git add .
git commit -m "Initial commit"

# GitHubで新規リポジトリ作成後
git remote add origin https://github.com/[USERNAME]/minecraft-collaboration-project.git
git branch -M main
git push -u origin main
```

### 3. Scratch GUI のビルドと準備

#### 自動スクリプトを使用
```batch
# 1. 設定ファイルを編集
notepad update-extension-url.js

# 以下を変更:
const GITHUB_USERNAME = 'あなたのユーザー名';
const REPO_NAME = 'minecraft-collaboration-project';

# 2. ビルドスクリプト実行
cd scratch-gui-deploy
prepare-github-pages.bat

# 3. 設定を反映
cd ..
node update-extension-url.js
```

#### 手動でビルド（必要な場合）
```bash
# Scratch GUIをビルド
cd scratch-gui
npm run build

# docsフォルダにコピー
cd ..
mkdir docs
xcopy /E /I /Y scratch-gui\build\* docs\

# 拡張機能ファイルもコピー
copy scratch-extension\dist\minecraft-collaboration-extension.js docs\minecraft-extension.js
```

### 4. GitHubにプッシュ
```bash
git add docs/
git commit -m "Add Scratch GUI for GitHub Pages"
git push origin main
```

### 5. GitHub Pages を有効化

1. **リポジトリ設定を開く**
   - リポジトリページ → Settings タブ

2. **Pages セクションに移動**
   - 左サイドバーの「Pages」をクリック

3. **ソースを設定**
   - Source: `Deploy from a branch`
   - Branch: `main`
   - Folder: `/docs`
   - 「Save」をクリック

4. **公開を確認**
   - 数分後、上部に公開URLが表示される
   - `https://[USERNAME].github.io/minecraft-collaboration-project/`

## 🔧 カスタムドメイン（オプション）

### 独自ドメインを使用する場合
1. **CNAMEファイル作成**
   ```
   echo yourdomain.com > docs/CNAME
   ```

2. **DNS設定**
   - Aレコード: `185.199.108.153`
   - Aレコード: `185.199.109.153`
   - Aレコード: `185.199.110.153`
   - Aレコード: `185.199.111.153`

3. **GitHubで設定**
   - Settings → Pages → Custom domain
   - ドメインを入力して Save

## 📋 チェックリスト

- [ ] GitHubアカウント作成
- [ ] リポジトリ作成
- [ ] `update-extension-url.js` でユーザー名設定
- [ ] `prepare-github-pages.bat` 実行
- [ ] `docs/` フォルダをコミット
- [ ] GitHub Pages 有効化
- [ ] 公開URL確認

## 🚨 注意事項

### セキュリティ
- APIキーやパスワードを含めない
- 個人情報を公開しない
- プライベートにしたい場合はPrivateリポジトリを使用

### 制限事項
- ファイルサイズ: 100MB以下
- リポジトリサイズ: 1GB以下（推奨）
- 帯域: 月100GB（通常は十分）

### ビルドの自動化
GitHub Actionsを使用して自動ビルド：
```yaml
# .github/workflows/deploy.yml
name: Deploy to GitHub Pages

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - uses: actions/setup-node@v2
    - run: |
        cd scratch-gui
        npm install
        npm run build
    - run: |
        mkdir -p docs
        cp -r scratch-gui/build/* docs/
    - uses: JamesIves/github-pages-deploy-action@4.1.5
      with:
        branch: gh-pages
        folder: docs
```

## 🎉 完了！

これで、世界中の誰でもあなたのScratch × Minecraftプロジェクトを使えるようになります！

**公開URL例**: `https://your-username.github.io/minecraft-collaboration-project/`

## 📞 サポート

- GitHub Pages ドキュメント: https://docs.github.com/pages
- GitHub Community: https://github.community/
- Stack Overflow: タグ `github-pages`