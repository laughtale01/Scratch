# サービスアカウントへの権限付与リクエスト

## 🔐 必要なアクション

サービスアカウントが作成されましたが、プロジェクトへのアクセス権限が必要です。

### サービスアカウント情報
- **メール**: `firebase-adminsdk-fbsvc@laughtale-scratch-ca803.iam.gserviceaccount.com`
- **プロジェクトID**: `laughtale-scratch-ca803`

## 📝 権限付与手順

### 方法1: Firebase Console（推奨）

1. **Firebase Console を開く**:
   ```
   https://console.firebase.google.com/u/5/project/laughtale-scratch-ca803/settings/iam
   ```

2. **「メンバーを追加」をクリック**

3. **以下の情報を入力**:
   - **新しいメンバー**: `firebase-adminsdk-fbsvc@laughtale-scratch-ca803.iam.gserviceaccount.com`
   - **ロール**: `Firebase Hosting 管理者`

4. **「追加」をクリック**

### 方法2: Google Cloud Console

1. **Google Cloud Console を開く**:
   ```
   https://console.cloud.google.com/iam-admin/iam?project=laughtale-scratch-ca803
   ```

2. **「メンバーを追加」をクリック**

3. **以下の情報を入力**:
   - **新しいメンバー**: `firebase-adminsdk-fbsvc@laughtale-scratch-ca803.iam.gserviceaccount.com`
   - **ロール**: 
     - `Firebase Hosting 管理者`
     - `Firebase サービス エージェント`

4. **「保存」をクリック**

## ✅ 確認方法

権限付与完了後、以下のコマンドで確認できます：

```bash
GOOGLE_APPLICATION_CREDENTIALS=service-account-key.json firebase projects:list
```

`laughtale-scratch-ca803`プロジェクトが表示されれば成功です。

## 🎯 最終目標

権限付与後、以下のデプロイコマンドが実行可能になります：

```bash
GOOGLE_APPLICATION_CREDENTIALS=service-account-key.json firebase deploy --only hosting --project laughtale-scratch-ca803
```

---

**次のステップ**: 上記の手順でサービスアカウントに権限を付与し、完了後お知らせください。