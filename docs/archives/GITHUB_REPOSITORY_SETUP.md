# 🎯 GitHubリポジトリセットアップガイド

## 📝 リポジトリ作成手順

1. **GitHubにログイン**
   - https://github.com/laughtale01

2. **新しいリポジトリを作成**
   - 右上の「+」ボタン → 「New repository」をクリック

3. **リポジトリ情報を入力**
   ```
   Repository name: minecraft-collaboration
   Description: ScratchとMinecraft 1.20.1を連携した小学生向け協調学習システム
   Public/Private: お好みで選択（教育目的ならPublic推奨）
   ```

4. **重要な設定**
   - ⚠️ 以下のチェックは**すべて外す**：
     - [ ] Add a README file
     - [ ] Add .gitignore
     - [ ] Choose a license

5. **「Create repository」をクリック**

## 🚀 作成後の手順

リポジトリ作成後、GitHubが表示するコマンドは無視して、以下を実行：

```bash
# 現在のディレクトリで実行
cd D:\minecraft_collaboration_project

# リモートリポジトリを追加
git remote add origin https://github.com/laughtale01/minecraft-collaboration.git

# GitHubにプッシュ
git push -u origin main
```

## 🔐 認証について

プッシュ時にユーザー名とパスワードを求められます：
- Username: laughtale01
- Password: Personal Access Token（パスワードではありません）

### Personal Access Tokenの作成方法
1. GitHub → Settings → Developer settings
2. Personal access tokens → Tokens (classic)
3. Generate new token
4. 必要な権限: `repo`（すべてチェック）
5. トークンをコピー（一度しか表示されません！）

## ✅ 成功確認

プッシュが成功したら：
- https://github.com/laughtale01/minecraft-collaboration にアクセス
- ファイルが表示されることを確認
- README.mdが自動的に表示されます

## 📦 リリースの作成（オプション）

```bash
# バージョンタグを作成
git tag -a v1.0.0 -m "初回リリース: Minecraft協調学習システム"

# タグをプッシュ
git push origin v1.0.0
```

GitHubでReleaseを作成し、ビルド済みファイルを添付できます。