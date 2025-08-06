# Firebase プロジェクトへのアクセス権限付与手順

## 🔐 CLIツール（Claude Code）に権限を付与する方法

### 方法1: サービスアカウントキーを使用（推奨）

#### ステップ1: サービスアカウントの作成

1. **Firebase Console を開く**
   ```
   https://console.firebase.google.com/u/5/project/laughtale-scratch-ca803/settings/serviceaccounts/adminsdk
   ```

2. **「新しい秘密鍵を生成」をクリック**
   - JSONファイルがダウンロードされます
   - 例: `laughtale-scratch-ca803-firebase-adminsdk-xxxxx-xxxxxxxxxx.json`

3. **JSONファイルをプロジェクトに配置**
   ```bash
   # ダウンロードしたJSONファイルを以下の場所に配置してください：
   D:\minecraft_collaboration_project\service-account-key.json
   ```

4. **環境変数を設定**
   ```bash
   # Windows (PowerShell)
   $env:GOOGLE_APPLICATION_CREDENTIALS="D:\minecraft_collaboration_project\service-account-key.json"
   
   # または、プロジェクトディレクトリで実行
   set GOOGLE_APPLICATION_CREDENTIALS=service-account-key.json
   ```

#### ステップ2: サービスアカウントでデプロイ

JSONファイルを配置したら、私（Claude Code）が以下のコマンドでデプロイできます：

```bash
# サービスアカウントを使用してデプロイ
firebase deploy --only hosting --project laughtale-scratch-ca803
```

### 方法2: Firebase CLIトークンを使用

#### ステップ1: トークンの生成

1. **あなたのローカル環境で実行**（別のターミナル/コマンドプロンプト）：
   ```bash
   firebase login:ci
   ```

2. **ブラウザが開き、Googleアカウントでログイン**

3. **トークンが表示される**
   ```
   ✔  Success! Use this token to login on a CI server:

   1//0gxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```

4. **このトークンをコピー**

#### ステップ2: トークンを環境変数に設定

```bash
# Windows (PowerShell)
$env:FIREBASE_TOKEN="1//0gxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

# または
set FIREBASE_TOKEN=1//0gxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

#### ステップ3: トークンを使用してデプロイ

トークンを設定したら、私が以下のコマンドでデプロイできます：

```bash
firebase deploy --only hosting --project laughtale-scratch-ca803 --token $FIREBASE_TOKEN
```

### 方法3: プロジェクトメンバーとして追加

#### ステップ1: IAMでユーザーを追加

1. **Google Cloud Console を開く**
   ```
   https://console.cloud.google.com/iam-admin/iam?project=laughtale-scratch-ca803
   ```

2. **「メンバーを追加」をクリック**

3. **以下の情報を入力**：
   - **新しいメンバー**: `firebase-adminsdk@laughtale-scratch-ca803.iam.gserviceaccount.com`
   - **ロール**: 
     - `Firebase Hosting 管理者`
     - `Firebase サービス エージェント`
     - `編集者`

4. **「保存」をクリック**

## 🎯 最も簡単な方法（推奨）

### オプションA: サービスアカウントキー（最速）

1. こちらにアクセス：
   ```
   https://console.firebase.google.com/u/5/project/laughtale-scratch-ca803/settings/serviceaccounts/adminsdk
   ```

2. 「新しい秘密鍵を生成」をクリック

3. ダウンロードしたJSONファイルを以下の名前で保存：
   ```
   D:\minecraft_collaboration_project\service-account-key.json
   ```

4. 私に「JSONファイルを配置しました」と伝えてください

### オプションB: CIトークン（安全）

1. 新しいコマンドプロンプトを開く

2. 実行：
   ```bash
   firebase login:ci
   ```

3. 表示されたトークンをコピー

4. 私に「トークン: [ここにトークンを貼り付け]」と伝えてください

## ⚠️ セキュリティ注意事項

- **サービスアカウントキー**は機密情報です
- `.gitignore`に追加済み（自動的にGitから除外）
- トークンやキーを公開リポジトリにコミットしないでください

## 📝 確認事項

権限付与後、以下が可能になります：
- ✅ Firebase Hostingへのデプロイ
- ✅ プロジェクト設定の確認
- ✅ デプロイ履歴の確認
- ✅ ホスティング設定の更新

## 🆘 サポート

問題が発生した場合：
1. Firebase Consoleでプロジェクトへのアクセスを確認
2. 正しいプロジェクトID: `laughtale-scratch-ca803`
3. リージョン: `asia-northeast1`（東京）または適切なリージョン

---

**次のステップ**: 
上記のいずれかの方法で権限を設定し、設定完了後お知らせください。