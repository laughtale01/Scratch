# MinecraftEdu Scratch GUI

Scratch GUI with custom Minecraft extension integrated

## 概要

このディレクトリには、Minecraft拡張機能が統合されたScratch GUIが含まれています。

Scratch GUIは、Scratchブロックをビジュアルに編集するためのWebベースのユーザーインターフェースです。

## セットアップ

### 必要なソフトウェア

- Node.js 16.x以上
- npm 8.x以上

### インストール

```bash
npm install
```

### 開発サーバー起動

```bash
npm start
# → http://localhost:8601/ にアクセス
```

### ビルド

```bash
# プロダクションビルド
npm run build

# 開発ビルド（watch mode）
npm run dev
```

## カスタムVMとの統合

### scratch-vmをリンクする方法

このプロジェクトは、カスタマイズされた `scratch-vm` を使用します。

#### 方法1: npmリンク（推奨）

```bash
# scratch-vmディレクトリでリンクを作成
cd ../scratch-vm
npm link

# scratch-guiディレクトリでリンクを使用
cd ../scratch-gui
npm link scratch-vm

# 起動
npm start
```

#### 方法2: package.jsonで直接指定

`package.json` の dependencies に以下を追加：

```json
{
  "dependencies": {
    "scratch-vm": "file:../scratch-vm"
  }
}
```

その後：

```bash
npm install
npm start
```

## Minecraft拡張機能の有効化

### 拡張機能メニューへの追加

`src/lib/libraries/extensions/index.jsx` を編集し、Minecraft拡張機能を追加します：

```javascript
{
    name: 'Minecraft',
    extensionId: 'minecraft',
    iconURL: minecraftIconURL,
    insetIconURL: minecraftInsetIconURL,
    description: 'Minecraftをコントロール',
    featured: true,
    disabled: false,
    internetConnectionRequired: true,
    bluetoothRequired: false,
    helpLink: 'https://github.com/laughtale01/Scratch'
}
```

### アイコン画像の配置

拡張機能のアイコンを準備：

```
src/lib/libraries/extensions/
├── minecraft/
│   ├── minecraft.png           # メインアイコン（600x372px推奨）
│   └── minecraft-small.svg     # 小アイコン（20x20px推奨）
```

アイコンをimport：

```javascript
import minecraftIconURL from './minecraft/minecraft.png';
import minecraftInsetIconURL from './minecraft/minecraft-small.svg';
```

## デプロイ

### GitHub Pagesへのデプロイ

1. `package.json` の `homepage` を設定：

```json
{
  "homepage": "https://laughtale01.github.io/Scratch"
}
```

2. デプロイスクリプトを実行：

```bash
npm run deploy
```

### 静的ホスティング（Netlify, Vercel等）

1. プロダクションビルド：

```bash
npm run build
```

2. `build/` ディレクトリをデプロイ

## 開発

### ディレクトリ構造

```
scratch-gui/
├── package.json          # プロジェクト設定
├── src/
│   ├── components/       # Reactコンポーネント
│   ├── lib/
│   │   └── libraries/
│   │       └── extensions/  # 拡張機能定義
│   └── playground/       # プレイグラウンド（開発用）
├── build/               # ビルド成果物（npm run build後）
└── node_modules/        # 依存パッケージ
```

### カスタマイズ

#### ロゴの変更

`src/components/menu-bar/menu-bar.jsx` でロゴを変更できます。

#### スプラッシュスクリーンの変更

`src/components/loader/loader.jsx` でローディング画面を変更できます。

#### デフォルトプロジェクトの設定

`src/lib/default-project/` にデフォルトで開くプロジェクトを配置できます。

## 統合テスト

### Minecraft拡張機能のテスト手順

1. Minecraft MODを起動（WebSocketサーバーがport 14711で起動）
2. Scratch GUI起動：`npm start`
3. ブラウザで http://localhost:8601/ を開く
4. 「拡張機能を追加」→「Minecraft」を選択
5. 「Minecraftに接続」ブロックを実行
6. 接続成功を確認（ブラウザコンソールで確認）
7. 各ブロックが正常に動作するかテスト

### デバッグ方法

#### ブラウザ開発者ツール

- F12キーで開発者ツールを開く
- コンソールタブでログを確認
- ネットワークタブでWebSocket通信を確認

#### React DevTools

React DevToolsブラウザ拡張機能を使用：

- Chrome: https://chrome.google.com/webstore
- Firefox: https://addons.mozilla.org/firefox

#### Redux DevTools

Redux DevToolsでStateを監視：

- Chrome: https://chrome.google.com/webstore

## トラブルシューティング

### npm install エラー

```bash
# キャッシュクリア
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### scratch-vmがリンクされない

```bash
# リンクを削除して再作成
npm unlink scratch-vm
cd ../scratch-vm
npm link
cd ../scratch-gui
npm link scratch-vm

# 確認
npm ls scratch-vm
```

### ビルドエラー: "Module not found"

```bash
# 依存関係を再インストール
rm -rf node_modules
npm install
```

### ポート8601が既に使用中

```bash
# 環境変数でポートを変更
PORT=8602 npm start
```

### Minecraft拡張機能が表示されない

1. `src/lib/libraries/extensions/index.jsx` に正しく追加されているか確認
2. アイコン画像が正しいパスに配置されているか確認
3. `npm start` を再起動
4. ブラウザのキャッシュをクリア（Ctrl+Shift+R）

### WebSocket接続エラー

1. Minecraft MODが起動しているか確認
2. ブラウザコンソールでエラーメッセージを確認
3. CORS設定を確認（開発時は通常問題なし）
4. ファイアウォール設定を確認

## 本番環境への適用

### 環境変数の設定

`.env.production` ファイルを作成：

```env
# WebSocketサーバーのデフォルトホスト
REACT_APP_MINECRAFT_HOST=localhost
REACT_APP_MINECRAFT_PORT=14711

# Google Analytics（オプション）
REACT_APP_GA_ID=UA-XXXXXXXXX-X
```

### セキュリティ設定

本番環境では以下を検討：

1. HTTPS化（WebSocket → WSS）
2. 認証機能の追加
3. CORS設定の厳格化
4. レート制限の実装

## パフォーマンス最適化

### ビルドサイズの削減

```bash
# webpack-bundle-analyzerで分析
npm run analyze

# 不要な依存関係を削除
npm prune
```

### キャッシュ戦略

Service Workerを使用してオフラインでも動作するように：

`src/index.js` で：

```javascript
import * as serviceWorker from './serviceWorker';

// Service Workerを有効化
serviceWorker.register();
```

## リンク

- Scratch GUI公式: https://github.com/scratchfoundation/scratch-gui
- プロジェクトリポジトリ: https://github.com/laughtale01/Scratch
- ドキュメント: ../../docs/
- Scratch VM: ../scratch-vm/

## ライセンス

このプロジェクトは、Scratch GUIの派生物であり、BSD 3-Clause Licenseに従います。

---

**バージョン**: 0.1.0
**最終更新**: 2025-11-12
