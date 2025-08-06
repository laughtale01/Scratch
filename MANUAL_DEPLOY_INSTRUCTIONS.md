# 手動デプロイ手順 - laughtale-scratch-ca803

## 🚨 現在の状況

サービスアカウントの権限がまだ完全に反映されていません。
以下の手順で手動デプロイを実行してください。

## 📁 デプロイファイルの準備

**準備完了**: `public/` フォルダに1,709ファイルが配置済み

主要ファイル:
- `index.html` - メインScratch GUI
- `gui.js` - Scratch GUI JavaScript
- `static/extensions/minecraft-unified.js` - Minecraft拡張
- `static/assets/` - 全画像・音声ファイル

## 🔧 手動デプロイ方法

### 方法1: Firebase Console（最も確実）

1. **Firebase Console を開く**:
   ```
   https://console.firebase.google.com/u/5/project/laughtale-scratch-ca803/hosting
   ```

2. **「開始する」または「デプロイ」ボタンをクリック**

3. **「ファイルをドラッグ」または「参照」をクリック**

4. **以下のフォルダ全体を選択**:
   ```
   D:\minecraft_collaboration_project\public\
   ```
   
5. **アップロード完了まで待機**（数分かかります）

6. **「公開」をクリック**

### 方法2: Firebase CLI（ユーザーアカウント）

1. **新しいコマンドプロンプトを開く**

2. **Firebaseにログイン**:
   ```bash
   firebase login
   ```

3. **プロジェクトディレクトリに移動**:
   ```bash
   cd D:\minecraft_collaboration_project
   ```

4. **プロジェクトを設定**:
   ```bash
   firebase use laughtale-scratch-ca803
   ```

5. **デプロイ実行**:
   ```bash
   firebase deploy --only hosting
   ```

### 方法3: GitHub Actions（自動化）

1. **GitHub にプッシュ**:
   ```bash
   git add -A
   git commit -m "Production deployment ready"
   git push origin main
   ```

2. **GitHub Actions が自動実行**
   - `.github/workflows/build.yml` が設定済み
   - mainブランチプッシュで自動デプロイ

## ✅ 期待される結果

デプロイ成功後の URL:
- **メインサイト**: https://laughtale-scratch-ca803.web.app
- **Firebase App**: https://laughtale-scratch-ca803.firebaseapp.com

## 🔍 動作確認手順

1. **サイトアクセス**: 上記URLにアクセス
2. **拡張機能確認**: 左下の拡張機能ボタンをクリック
3. **Minecraft拡張選択**: 「Minecraft コラボレーション」を選択
4. **ブロック確認**: Minecraft関連ブロックが表示される

## 📊 デプロイ内容

- **総ファイル数**: 1,709ファイル
- **メインアプリ**: Scratch GUI v5.1.88
- **Minecraft拡張**: 統合済み（minecraft-unified.js）
- **多言語対応**: 12言語サポート
- **アセット**: 画像60MB、音声等

## 🛠️ トラブルシューティング

### 問題: アップロードが失敗する
**解決方法**:
1. ファイルサイズ制限を確認（Firebase: 2GB）
2. 分割アップロード（static/フォルダを先にアップロード）
3. 安定したネットワーク環境で実行

### 問題: サイトが表示されない
**解決方法**:
1. 数分待つ（CDN反映時間）
2. ブラウザキャッシュクリア（Ctrl+F5）
3. Firebase Console でデプロイ状況確認

### 問題: 拡張機能が動作しない
**解決方法**:
1. `static/extensions/minecraft-unified.js` ファイル確認
2. ブラウザ開発者ツールでエラー確認
3. WebSocket接続設定確認

## 📞 サポート

デプロイ完了後、以下をお知らせください:
- ✅ デプロイURL
- ✅ 動作確認結果
- ❓ 問題があれば具体的な症状

---

**推奨**: 方法1（Firebase Console）が最も確実です。
アップロード完了後、URLをお教えください！