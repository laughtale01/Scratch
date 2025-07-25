# 🚀 GitHub Pages クイックスタート（5分で公開！）

## 📋 必要なもの
- GitHubアカウント（無料）
- このプロジェクトのファイル

## 💰 料金
**完全無料！** クレジットカード不要

## 🎯 5分で公開する手順

### 1️⃣ GitHubアカウント作成（1分）
1. [github.com](https://github.com) → 「Sign up」
2. メールアドレスとパスワード入力
3. メール認証

### 2️⃣ リポジトリ作成（1分）
1. GitHub にログイン
2. 右上の「+」→「New repository」
3. Repository name: `minecraft-collaboration-project`
4. Public を選択
5. 「Create repository」クリック

### 3️⃣ ファイルアップロード（2分）

#### 方法A: GitHub Webインターフェース（簡単）
1. 「uploading an existing file」をクリック
2. `docs` フォルダをドラッグ&ドロップ
3. 「Commit changes」クリック

#### 方法B: コマンドライン
```bash
git init
git add docs/
git commit -m "Add Scratch GUI"
git remote add origin https://github.com/[あなたのユーザー名]/minecraft-collaboration-project.git
git push -u origin main
```

### 4️⃣ GitHub Pages 有効化（1分）
1. リポジトリページ → Settings
2. 左メニュー → Pages
3. Source: `Deploy from a branch`
4. Branch: `main` 、Folder: `/docs`
5. Save

### 5️⃣ 完了！
- 数分待つと公開される
- URL: `https://[あなたのユーザー名].github.io/minecraft-collaboration-project/`

## 📝 事前準備（ローカルでビルド）

```batch
# 1. 設定を変更
notepad update-extension-url.js
# GITHUB_USERNAME を自分のものに変更

# 2. ビルド実行
cd scratch-gui-deploy
prepare-github-pages.bat

# 3. docsフォルダが生成される
```

## ✅ 確認方法
1. `https://[ユーザー名].github.io/minecraft-collaboration-project/` を開く
2. Scratch GUIが表示される
3. 拡張機能から「Minecraft」が選択できる

## 🆘 トラブルシューティング

### ページが表示されない
- Settings → Pages で緑のチェックマークを確認
- 最大10分待つ
- ブラウザのキャッシュをクリア

### 404エラー
- `/docs` フォルダが正しくアップロードされているか確認
- index.html が docs 直下にあるか確認

### 拡張機能が読み込まれない
- `docs/minecraft-extension.js` が存在するか確認
- ブラウザの開発者ツールでエラーを確認

## 🎉 これで完了！
無料で世界中に公開できました！