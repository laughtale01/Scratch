# 📤 GitHubへのプッシュ手順

このドキュメントでは、プロジェクトをGitHubにアップロードする手順を説明します。

## 📋 前提条件

- GitHubアカウント: https://github.com/laughtale01/Scratch ✅
- Gitがインストールされていること
- プロジェクトディレクトリ: `D:\minecraft_collaboration_project`

## 🚀 手順

### 1. GitHubでリポジトリを作成

1. GitHubにログイン: https://github.com/laughtale01
2. 右上の「+」ボタンから「New repository」を選択
3. 以下の設定でリポジトリを作成:
   - Repository name: `minecraft-collaboration`
   - Description: `ScratchとMinecraft 1.20.1を連携した小学生向け協調学習システム`
   - Public/Private: お好みで選択（教育目的ならPublic推奨）
   - **重要**: 「Initialize this repository with:」のチェックは**すべて外す**
4. 「Create repository」をクリック

### 2. ローカルでの初期コミット

```bash
# プロジェクトディレクトリに移動
cd D:\minecraft_collaboration_project

# すでにgit initは実行済み

# ユーザー情報を設定（初回のみ）
git config user.name "laughtale01"
git config user.email "あなたのメールアドレス"

# すべてのファイルをステージング
git add .

# 初回コミット
git commit -m "Initial commit: Minecraft協調学習システム v1.0.0

- Minecraft Forge 1.20.1対応
- Scratch 3.0拡張機能
- WebSocket通信による協調機能
- 友達招待・訪問システム
- 安全な帰宅機能"
```

### 3. GitHubにプッシュ

```bash
# リモートリポジトリを追加
git remote add origin https://github.com/laughtale01/minecraft-collaboration.git

# メインブランチに名前を変更（Git 2.28以降）
git branch -M main

# GitHubにプッシュ
git push -u origin main
```

### 4. 認証について

初回プッシュ時には、GitHubの認証が必要です：

#### 方法1: Personal Access Token（推奨）
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. 「Generate new token」をクリック
3. 必要な権限（repo）を選択
4. トークンをコピー（一度しか表示されません！）
5. パスワードの代わりにトークンを使用

#### 方法2: GitHub CLI
```bash
# GitHub CLIをインストール後
gh auth login
```

### 5. プッシュ後の確認

1. ブラウザで https://github.com/laughtale01/minecraft-collaboration にアクセス
2. ファイルがアップロードされていることを確認
3. README.mdが正しく表示されることを確認

## 📝 今後の更新方法

```bash
# 変更をステージング
git add .

# コミット
git commit -m "コミットメッセージ"

# プッシュ
git push
```

## 🏷️ リリースタグの作成

```bash
# バージョンタグを作成
git tag -a v1.0.0 -m "初回リリース: Minecraft協調学習システム"

# タグをプッシュ
git push origin v1.0.0
```

## 📦 GitHubリリースの作成

1. リポジトリページで「Releases」をクリック
2. 「Create a new release」をクリック
3. タグを選択（v1.0.0）
4. リリースタイトルとノートを記入
5. ビルド済みファイルを添付:
   - `minecraft-mod/build/libs/minecraft-collaboration-mod-1.0.0-all.jar`
   - `scratch-extension/dist/` の内容をZIP化
6. 「Publish release」をクリック

## ⚠️ 注意事項

- `.gitignore`ファイルにより、ビルド成果物やログファイルは除外されます
- 機密情報（APIキー等）が含まれていないことを確認してください
- 大きなファイル（100MB以上）は Git LFS の使用を検討してください

## 🆘 トラブルシューティング

### プッシュが拒否される場合
```bash
# 強制プッシュ（初回のみ、注意して使用）
git push -u origin main --force
```

### リモートURLを変更する場合
```bash
# 現在のリモートを確認
git remote -v

# リモートURLを変更
git remote set-url origin https://github.com/laughtale01/新しいリポジトリ名.git
```

頑張ってください！🎉