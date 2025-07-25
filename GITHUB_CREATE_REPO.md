# 🌐 GitHubリポジトリ作成手順

## 📋 手動で作成が必要です

GitHubリポジトリは、セキュリティ上の理由からブラウザで手動作成する必要があります。

### 🔗 作成手順

1. **以下のURLにアクセス**
   ```
   https://github.com/new
   ```

2. **以下の情報を入力**
   - **Repository name**: `minecraft-collaboration`
   - **Description**: `ScratchとMinecraft 1.20.1を連携した小学生向け協調学習システム`
   - **Public/Private**: Public推奨（教育目的）
   
3. **重要**: 以下は**チェックしない**
   - [ ] Add a README file
   - [ ] Add .gitignore  
   - [ ] Choose a license

4. **「Create repository」をクリック**

## ✅ リポジトリ作成後

リポジトリが作成されたら、ターミナルで以下を実行：

```bash
# すでに設定済みなので、直接プッシュ
git push -u origin main
```

## 🔐 認証が必要な場合

Username: laughtale01
Password: [Personal Access Token]

### Token作成リンク
```
https://github.com/settings/tokens/new
```
- Scopeは「repo」を選択
- Generateをクリック
- トークンをコピー（一度だけ表示）

## 📝 現在の状態

- ✅ ローカルリポジトリ: 初期化済み
- ✅ 初回コミット: 完了（138ファイル）
- ✅ リモート設定: `https://github.com/laughtale01/minecraft-collaboration.git`
- ⏳ 待機中: GitHubでのリポジトリ作成

リポジトリを作成したら、`git push -u origin main`を実行してください！