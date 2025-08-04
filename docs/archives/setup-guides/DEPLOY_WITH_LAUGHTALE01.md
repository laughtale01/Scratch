# Firebase Hosting デプロイ手順 (laughtale.education@gmail.com)

## 1. Firebaseログイン

### コマンドプロンプト/ターミナルで実行:
```bash
firebase login
```

### ログイン手順:
1. ブラウザが自動的に開きます
2. **laughtale.education@gmail.com**アカウントを選択してログイン
3. Firebaseへのアクセス許可を承認
4. ターミナルに「Success! Logged in as laughtale.education@gmail.com」と表示されることを確認

## 2. 正しいアカウントの確認
```bash
firebase login:list
```
laughtale.education@gmail.comアカウントが表示されることを確認

## 3. プロジェクトの確認
```bash
firebase projects:list
```
`laughtale-scratch-ca803`プロジェクトが表示されることを確認

## 4. デプロイ実行
```bash
# プロジェクトディレクトリに移動
cd D:\minecraft_collaboration_project

# デプロイ実行
firebase deploy --only hosting
```

## 5. デプロイ成功後の確認

### アクセスURL:
- https://laughtale-scratch-ca803.web.app/
- https://laughtale-scratch-ca803.firebaseapp.com/

### Firebase Console確認:
1. https://console.firebase.google.com/ にアクセス
2. laughtale.education@gmail.comアカウントでログイン
3. `laughtale-scratch-ca803`プロジェクトを選択
4. 左メニューから「Hosting」を選択
5. デプロイ履歴を確認

## トラブルシューティング

### 別のアカウントでログインしている場合:
```bash
# 現在のアカウントをログアウト
firebase logout

# laughtale.education@gmail.comでログイン
firebase login
```

### プロジェクトが見つからない場合:
```bash
# プロジェクトを明示的に指定
firebase use laughtale-scratch-ca803

# または
firebase deploy --only hosting --project laughtale-scratch-ca803
```

### 権限エラーが出る場合:
laughtale.education@gmail.comアカウントがプロジェクトの適切な権限を持っているか確認:
- Firebase Console → プロジェクト設定 → ユーザーと権限

## デプロイコマンドまとめ

```bash
# 1. ログイン確認
firebase login:list

# 2. プロジェクト確認
firebase use laughtale-scratch-ca803

# 3. デプロイ実行
firebase deploy --only hosting

# 4. デプロイ状況確認
firebase hosting:channel:list
```

## 成功メッセージ例:
```
=== Deploying to 'laughtale-scratch-ca803'...

i  deploying hosting
i  hosting[laughtale-scratch-ca803]: beginning deploy...
i  hosting[laughtale-scratch-ca803]: found XX files in docs
✔  hosting[laughtale-scratch-ca803]: file upload complete
i  hosting[laughtale-scratch-ca803]: finalizing version...
✔  hosting[laughtale-scratch-ca803]: version finalized
i  hosting[laughtale-scratch-ca803]: releasing new version...
✔  hosting[laughtale-scratch-ca803]: release complete

✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/laughtale-scratch-ca803/overview
Hosting URL: https://laughtale-scratch-ca803.web.app
```