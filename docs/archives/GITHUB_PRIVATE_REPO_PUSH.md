# 🔐 プライベートリポジトリへのプッシュ方法

プライベートリポジトリ `https://github.com/laughtale01/Scratch` へプッシュするには認証が必要です。

## 📋 認証方法

### 方法1: Personal Access Token（推奨）

1. **Personal Access Tokenを作成**
   - https://github.com/settings/tokens/new にアクセス
   - 以下を設定：
     - Note: `minecraft-collaboration-access`
     - Expiration: 90 days（お好みで）
     - Scopes: 
       - ✅ repo（すべてチェック）
   - 「Generate token」をクリック
   - **トークンをコピー**（一度しか表示されません！）

2. **プッシュコマンドを実行**
   ```bash
   git push -u origin main
   ```

3. **認証情報を入力**
   ```
   Username: laughtale01
   Password: [コピーしたトークンを貼り付け]
   ```
   ※パスワード入力時は画面に表示されません

### 方法2: GitHub CLI（gh）を使用

1. **GitHub CLIのインストール**
   ```bash
   # Windowsの場合（wingetを使用）
   winget install --id GitHub.cli
   
   # または https://cli.github.com/ からダウンロード
   ```

2. **認証**
   ```bash
   gh auth login
   ```
   - GitHub.comを選択
   - HTTPSを選択
   - Authenticate with your web browserを選択

3. **プッシュ**
   ```bash
   git push -u origin main
   ```

### 方法3: Git Credential Managerを使用

Windows環境では、Git for Windowsに含まれるCredential Managerが自動的に認証を管理します。

```bash
# 認証情報をクリア（必要な場合）
git config --global --unset credential.helper

# Credential Managerを設定
git config --global credential.helper manager

# プッシュ（ポップアップで認証）
git push -u origin main
```

## 🚀 プッシュ実行

認証準備ができたら、以下を実行：

```bash
cd D:\minecraft_collaboration_project
git push -u origin main
```

## ⚠️ トラブルシューティング

### "Repository not found"エラーの場合
- リポジトリ名の確認: `Scratch`（大文字小文字に注意）
- アカウント名の確認: `laughtale01`
- プライベートリポジトリへのアクセス権限を確認

### 認証が通らない場合
- Personal Access Tokenの権限（repo）を確認
- トークンの有効期限を確認
- 正しいユーザー名を使用しているか確認

## 📝 認証情報の保存

一度認証に成功すると、Windowsの資格情報マネージャーに保存されます：
- コントロールパネル → 資格情報マネージャー → Windows資格情報
- git:https://github.com のエントリ

## 🎯 次のステップ

プッシュが成功したら：
1. https://github.com/laughtale01/Scratch にアクセス
2. ファイルがアップロードされていることを確認
3. README.mdが表示されることを確認