# 🔑 GitHub Personal Access Token 作成ガイド

GitHubはセキュリティ強化のため、2021年8月からパスワード認証を廃止しました。
代わりにPersonal Access Token（PAT）を使用します。

## 📋 Token作成手順

### 1. GitHub設定ページへアクセス
以下のリンクをクリック：
👉 https://github.com/settings/tokens/new

または手動で：
1. GitHubにログイン
2. 右上のプロフィール画像をクリック
3. Settings → Developer settings → Personal access tokens → Tokens (classic)

### 2. 新しいトークンを作成

**Note（名前）**: `minecraft-collaboration-push`

**Expiration（有効期限）**: 
- 30 days（推奨）
- 90 days
- Custom（カスタム）
- No expiration（無期限）※セキュリティ上非推奨

**Select scopes（権限）**:
以下にチェック：
- ✅ **repo** （リポジトリへのフルアクセス）
  - ✅ repo:status
  - ✅ repo_deployment
  - ✅ public_repo
  - ✅ repo:invite
  - ✅ security_events

### 3. Generate tokenをクリック

### 4. トークンをコピー
⚠️ **重要**: トークンは一度しか表示されません！必ずコピーしてください。

## 🚀 トークンを使ってプッシュ

```bash
git push -u origin main
```

認証プロンプトが表示されたら：
```
Username: laughtale01
Password: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx（コピーしたトークン）
```

※パスワード入力時は画面に表示されません

## 💾 認証情報の保存

一度認証に成功すると、Windowsの資格情報マネージャーに保存されます。

### 確認方法：
1. コントロールパネル → 資格情報マネージャー
2. Windows資格情報
3. `git:https://github.com` を探す

### 手動で保存する場合：
```bash
git config --global credential.helper manager-core
```

## 🔧 トラブルシューティング

### 古い認証情報をクリア
```bash
# Windows資格情報をクリア
git config --global --unset credential.helper
git config --global credential.helper manager-core
```

### コマンドラインでトークンを設定（一時的）
```bash
# 注意：履歴に残るため、使用後は削除推奨
git remote set-url origin https://laughtale01:ghp_YOUR_TOKEN@github.com/laughtale01/Scratch.git
git push -u origin main

# プッシュ後、トークンを削除
git remote set-url origin https://github.com/laughtale01/Scratch.git
```

## 📝 トークン管理のベストプラクティス

1. **最小限の権限**: 必要な権限のみ付与
2. **有効期限を設定**: 無期限は避ける
3. **定期的に更新**: 期限切れ前に新しいトークンを作成
4. **用途別に作成**: プロジェクトごとに別のトークン
5. **安全に保管**: パスワードマネージャーを使用

## 🔗 参考リンク

- [Creating a personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)
- [About authentication to GitHub](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/about-authentication-to-github)

---

トークン作成後、`git push -u origin main`を実行してください！