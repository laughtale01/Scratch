# 🔥 Firebase Hosting デプロイガイド

## 📋 目次
- [クイックスタート（手動アップロード）](#クイックスタート手動アップロード)
- [CLI を使った自動デプロイ](#cli-を使った自動デプロイ)
- [カスタムドメイン設定](#カスタムドメイン設定)
- [トラブルシューティング](#トラブルシューティング)

---

## 🚀 クイックスタート（手動アップロード）

### 前提条件
- Googleアカウント
- アップロードするファイル（`docs/`フォルダ内）

### 手順

#### 1. Firebase プロジェクト作成
1. [Firebase Console](https://console.firebase.google.com/) にアクセス
2. **「プロジェクトを作成」** をクリック
3. プロジェクト名を入力（例: `laughtale-scratch`）
4. Google アナリティクスは任意（通常は無効でOK）
5. **「プロジェクトを作成」** をクリック

#### 2. Hosting を有効化
1. 左サイドバーの **「Hosting」** をクリック
2. **「始める」** をクリック
3. セットアップ画面は **「次へ」** で進む
4. **「コンソールに進む」** をクリック

#### 3. ファイルをアップロード
1. Hostingダッシュボードで **「ファイルをアップロード」** を選択
2. 以下のファイルをアップロード：
   - `docs/index.html`
   - `docs/minecraft-extension.js`
   - その他必要なファイル
3. **「デプロイ」** をクリック

#### 4. 公開URLを確認
```
https://[プロジェクトID].web.app/
```

---

## 🔧 CLI を使った自動デプロイ

### Firebase CLI のインストール
```bash
npm install -g firebase-tools
```

### 初期設定
```bash
# Firebaseにログイン
firebase login

# プロジェクトを初期化
firebase init hosting

# 質問への回答:
# - Use an existing project → 作成したプロジェクトを選択
# - Public directory → docs
# - Single-page app → No
# - Automatic builds → No
```

### デプロイ
```bash
# ファイルをデプロイ
firebase deploy --only hosting

# プレビュー（本番環境に影響なし）
firebase hosting:channel:deploy preview
```

### 自動デプロイ設定（GitHub Actions）
`.github/workflows/firebase-hosting.yml`:
```yaml
name: Deploy to Firebase Hosting
on:
  push:
    branches: [ main ]
    paths:
      - 'docs/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: FirebaseExtended/action-hosting-deploy@v0
        with:
          repoToken: '${{ secrets.GITHUB_TOKEN }}'
          firebaseServiceAccount: '${{ secrets.FIREBASE_SERVICE_ACCOUNT }}'
          channelId: live
          projectId: your-project-id
```

---

## 🌐 カスタムドメイン設定

### 独自ドメインの追加
1. Hostingダッシュボードで **「カスタムドメインを追加」**
2. ドメイン名を入力（例: `minecraft.example.com`）
3. DNS設定の指示に従う：
   ```
   Type: A
   Host: @
   Value: [表示されるIPアドレス]
   ```
4. SSL証明書の自動プロビジョニングを待つ（最大24時間）

### サブドメインの設定
```
Type: CNAME
Host: minecraft
Value: [プロジェクトID].web.app
```

---

## 📊 パフォーマンス最適化

### キャッシュ設定
`firebase.json`:
```json
{
  "hosting": {
    "public": "docs",
    "headers": [
      {
        "source": "**/*.js",
        "headers": [{
          "key": "Cache-Control",
          "value": "public, max-age=3600"
        }]
      },
      {
        "source": "**/*.html",
        "headers": [{
          "key": "Cache-Control",
          "value": "public, max-age=300"
        }]
      }
    ]
  }
}
```

### 圧縮設定
Firebase Hostingは自動的に以下を行います：
- Gzip圧縮
- Brotli圧縮（対応ブラウザ）
- 画像最適化

---

## 🔧 トラブルシューティング

### 問題: デプロイが失敗する
**解決方法:**
1. Firebase CLIが最新か確認: `firebase --version`
2. 再ログイン: `firebase logout && firebase login`
3. プロジェクト設定確認: `firebase use`

### 問題: ファイルが更新されない
**解決方法:**
1. ブラウザキャッシュをクリア
2. デプロイ履歴を確認: `firebase hosting:releases:list`
3. 強制デプロイ: `firebase deploy --only hosting --force`

### 問題: CORS エラー
**解決方法:**
`firebase.json` に追加:
```json
{
  "hosting": {
    "headers": [
      {
        "source": "**",
        "headers": [{
          "key": "Access-Control-Allow-Origin",
          "value": "*"
        }]
      }
    ]
  }
}
```

### 問題: 404 エラー
**解決方法:**
1. `public` ディレクトリが正しいか確認
2. `index.html` が存在するか確認
3. リライトルールを設定:
```json
{
  "hosting": {
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  }
}
```

---

## 📈 モニタリング

### 利用状況の確認
1. Firebase Console → Hosting → 使用状況
2. 確認できる項目：
   - 帯域幅使用量
   - ストレージ使用量
   - リクエスト数

### アナリティクス連携
Google アナリティクスを有効にした場合：
1. Firebase Console → Analytics
2. ユーザー行動、エンゲージメントを確認

---

## 🔒 セキュリティ

### 基本的なセキュリティヘッダー
`firebase.json`:
```json
{
  "hosting": {
    "headers": [
      {
        "source": "**",
        "headers": [
          {
            "key": "X-Frame-Options",
            "value": "SAMEORIGIN"
          },
          {
            "key": "X-Content-Type-Options",
            "value": "nosniff"
          }
        ]
      }
    ]
  }
}
```

### アクセス制限
開発中のプレビューチャンネル:
```bash
# 期限付きプレビューURL生成
firebase hosting:channel:deploy preview --expires 7d
```

---

## 🎯 ベストプラクティス

1. **バージョン管理**: タグ付けしてからデプロイ
2. **プレビュー確認**: 本番デプロイ前にプレビューチャンネルでテスト
3. **監視設定**: Firebase のアラート機能を活用
4. **バックアップ**: デプロイ前のファイルを保管

---

## 📚 参考リンク

- [Firebase Hosting 公式ドキュメント](https://firebase.google.com/docs/hosting)
- [Firebase CLI リファレンス](https://firebase.google.com/docs/cli)
- [料金計算ツール](https://firebase.google.com/pricing)

---

**注意**: 無料枠（Spark プラン）の制限
- ストレージ: 10GB
- 帯域幅: 360MB/日
- カスタムドメイン: 利用可能

プロジェクトが成長したら Blaze プラン（従量課金）への移行を検討してください。