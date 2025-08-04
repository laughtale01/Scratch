# Firebase Hosting デプロイメントガイド

## 🚀 デプロイ準備完了！

Firebase Hostingへのデプロイ準備が整いました。以下の手順でデプロイしてください。

## 📋 デプロイ手順

### 1. コマンドプロンプト/ターミナルを開く
```bash
cd D:\minecraft_collaboration_project
```

### 2. Firebaseにログイン（初回のみ）
```bash
firebase login
```
- ブラウザが開きGoogleアカウントでログインを求められます
- プロジェクトへのアクセス権限があるアカウントでログインしてください

### 3. デプロイ実行
```bash
firebase deploy --only hosting
```

または、作成したバッチファイルを実行：
```bash
FIREBASE_DEPLOY_COMMANDS.bat
```

## 🌐 デプロイ後のURL

デプロイが成功すると、以下のURLでアクセス可能になります：

### メインサイト
- https://laughtale-scratch-ca803.web.app/
- https://laughtale-scratch-ca803.firebaseapp.com/

### 各ページ
- **メインエディタ**: https://laughtale-scratch-ca803.web.app/index.html
- **シンプル版**: https://laughtale-scratch-ca803.web.app/index-simple.html
- **拡張機能**: https://laughtale-scratch-ca803.web.app/minecraft-extension.js

## 📁 デプロイされるファイル

```
docs/
├── index.html                # メインのScratchエディタ
├── index-simple.html         # シンプル版エディタ
├── minecraft-extension.js    # Minecraft拡張機能
├── INDEX.md                 # ドキュメントインデックス
└── その他のドキュメント
```

## ✅ デプロイ確認チェックリスト

1. **サイトアクセス確認**
   - [ ] メインページが表示される
   - [ ] ローディング画面が正しく動作する
   - [ ] Scratchエディタが読み込まれる

2. **拡張機能確認**
   - [ ] minecraft-extension.jsが読み込める
   - [ ] CORSエラーが発生しない
   - [ ] 拡張機能URLが正しく設定されている

3. **機能確認**
   - [ ] WebSocket接続情報が表示される
   - [ ] 拡張機能の追加手順が表示される
   - [ ] コンソールにエラーが出ていない

## 🔧 トラブルシューティング

### "Permission denied"エラー
```bash
firebase logout
firebase login
firebase use laughtale-scratch-ca803
```

### デプロイが反映されない
```bash
# キャッシュをクリア
firebase hosting:channel:delete preview --force
firebase deploy --only hosting
```

### 404エラー
- firebase.jsonのpublicディレクトリが"docs"になっているか確認
- ファイルが正しく配置されているか確認

## 📝 重要な設定

### firebase.json
```json
{
  "hosting": {
    "public": "docs",
    "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
    "rewrites": [{"source": "**", "destination": "/index.html"}],
    "headers": [
      {
        "source": "**/*.js",
        "headers": [
          {"key": "Cache-Control", "value": "public, max-age=3600"},
          {"key": "Access-Control-Allow-Origin", "value": "*"}
        ]
      }
    ]
  }
}
```

### .firebaserc
```json
{
  "projects": {
    "default": "laughtale-scratch-ca803"
  }
}
```

## 🎉 デプロイ成功後

1. URLをScratchコミュニティで共有
2. Minecraft Modのドキュメントを更新
3. 使用方法のチュートリアルを作成

お疲れ様でした！🎮✨