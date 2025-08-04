# Firebase Hosting デプロイ手順

## 前提条件
- Firebase CLIがインストール済み（完了）
- Firebaseプロジェクト: `laughtale-scratch-ca803`

## デプロイ手順

### 1. Firebaseにログイン
```bash
firebase login
```

### 2. デプロイ実行
```bash
firebase deploy --only hosting
```

### 3. デプロイ確認
デプロイが成功すると、以下のURLでアクセス可能になります：
- https://laughtale-scratch-ca803.web.app/
- https://laughtale-scratch-ca803.firebaseapp.com/

## ファイル構成

```
docs/
├── index.html          # メインのScratchエディタ（高度な統合版）
├── index-simple.html   # シンプル版Scratchエディタ
├── minecraft-extension.js  # Minecraft拡張機能
└── その他のドキュメント
```

## 重要なURL

- **本番環境**: https://laughtale-scratch-ca803.web.app/
- **拡張機能URL**: https://laughtale-scratch-ca803.web.app/minecraft-extension.js

## デプロイ後の確認事項

1. **メインページの確認**
   - https://laughtale-scratch-ca803.web.app/ にアクセス
   - Scratchエディタが正しく表示されることを確認

2. **拡張機能の確認**
   - https://laughtale-scratch-ca803.web.app/minecraft-extension.js にアクセス
   - JavaScriptファイルが正しく配信されることを確認

3. **CORS設定の確認**
   - Scratch.mit.eduから拡張機能が読み込めることを確認
   - コンソールでCORSエラーが出ていないことを確認

## トラブルシューティング

### デプロイが失敗する場合
```bash
# キャッシュをクリア
firebase hosting:disable
firebase hosting:enable

# 再デプロイ
firebase deploy --only hosting --force
```

### 404エラーが出る場合
- firebase.jsonの設定を確認
- publicディレクトリが"docs"になっていることを確認

### CORSエラーが出る場合
- firebase.jsonのheaders設定を確認
- Access-Control-Allow-Originが適切に設定されていることを確認