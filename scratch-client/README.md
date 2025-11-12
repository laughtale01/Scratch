# MinecraftEdu Scratch Client

Scratch環境（VM + GUI）でMinecraftを制御

## 概要

このディレクトリには、MinecraftEduプロジェクトのScratchクライアント側のコードが含まれています。

- **scratch-vm**: Minecraft拡張機能を含むScratch Virtual Machine
- **scratch-gui**: Scratchのビジュアルエディタ（GUI）

## クイックスタート

### 前提条件

- Node.js 16.x以上
- npm 8.x以上
- Minecraftサーバー（MinecraftEdu MOD起動済み）

### セットアップ（初回のみ）

#### Windows

```cmd
setup.bat
```

#### Linux / macOS

```bash
chmod +x setup.sh
./setup.sh
```

### 起動方法

```bash
cd scratch-gui
npm start
```

ブラウザで http://localhost:8601/ を開く

## ディレクトリ構成

```
scratch-client/
├── package.json          # ルートパッケージ設定（セットアップスクリプト用）
├── setup.bat            # Windows用セットアップスクリプト
├── setup.sh             # Linux/macOS用セットアップスクリプト
├── scratch-vm/          # Scratch Virtual Machine
│   ├── package.json
│   ├── src/
│   │   └── extensions/
│   │       └── scratch3_minecraft/
│   │           └── index.js  # Minecraft拡張機能
│   └── README.md
└── scratch-gui/         # Scratch Graphical User Interface
    ├── package.json
    ├── src/
    │   ├── components/
    │   └── lib/
    └── README.md
```

## 使い方

### 1. Minecraftサーバーを起動

まず、Minecraft側でMinecraftEdu MODを起動します：

```bash
# MODをビルド＆インストール（初回のみ）
cd ../tools
./build-and-install.bat  # Windows
# または
./build-and-install.sh   # Linux/macOS

# Minecraftを起動してワールドを開く
# → コンソールに "WebSocket server started on port 14711" と表示されるのを確認
```

### 2. Scratch GUIを起動

```bash
cd scratch-gui
npm start
```

ブラウザが自動的に開かない場合は、手動で http://localhost:8601/ を開きます。

### 3. Minecraft拡張機能を追加

1. 画面左下の「拡張機能を追加」ボタンをクリック
2. 「Minecraft」を選択
3. Minecraftカテゴリのブロックが追加される

### 4. Minecraftに接続

「Minecraftに接続」ブロックを使用：

```
Minecraftに接続 ホスト [localhost] ポート [14711]
```

接続成功すると、コンソールに "Connected to Minecraft server" と表示されます。

### 5. ブロックを使って操作

以下のブロックが使用可能：

- チャット送信
- ブロック配置（絶対座標・相対座標）
- エンティティ召喚
- プレイヤーテレポート
- 天気変更
- 時刻変更
- コマンド実行

詳細は `scratch-vm/src/extensions/scratch3_minecraft/README.md` を参照してください。

## 開発

### scratch-vmの開発

```bash
cd scratch-vm
npm start
# → http://localhost:8073/playground/ でテスト
```

### scratch-guiの開発

```bash
cd scratch-gui
npm start
# → http://localhost:8601/ で開発
```

### 両方を同時に開発

2つのターミナルを開いて、それぞれで起動：

```bash
# ターミナル1
cd scratch-vm
npm start

# ターミナル2
cd scratch-gui
npm start
```

### ビルド

```bash
# scratch-vmをビルド
cd scratch-vm
npm run build

# scratch-guiをビルド
cd scratch-gui
npm run build
# → build/ ディレクトリに成果物が生成される
```

## トラブルシューティング

### セットアップエラー

#### npm install が失敗する

```bash
# キャッシュをクリアして再試行
npm cache clean --force
rm -rf scratch-vm/node_modules scratch-gui/node_modules
./setup.bat  # または ./setup.sh
```

#### npm link が失敗する

管理者権限で実行してみてください：

```bash
# Windows
# コマンドプロンプトを管理者として実行してから setup.bat

# Linux/macOS
sudo ./setup.sh
```

または、手動でリンク：

```bash
cd scratch-vm
npm link

cd ../scratch-gui
npm link scratch-vm
```

### 実行時エラー

#### ポート8601が既に使用中

```bash
# 別のポートで起動
PORT=8602 npm start
```

#### Minecraft拡張機能が表示されない

1. `scratch-gui/src/lib/libraries/extensions/index.jsx` を確認
2. ブラウザのキャッシュをクリア（Ctrl+Shift+R）
3. `npm start` を再起動

#### WebSocket接続エラー

1. Minecraft MODが起動しているか確認
2. ポート14711が開いているか確認
3. ファイアウォール設定を確認
4. ブラウザコンソール（F12）でエラー詳細を確認

### デバッグ

#### ブラウザ開発者ツール

F12キーで開発者ツールを開き：

- **Console**: JavaScriptのログとエラーを確認
- **Network**: WebSocket通信を確認（WS/WSS）
- **Application**: ローカルストレージやService Workerを確認

#### ログの確認

scratch-vmのログ：

```javascript
// scratch-vm/src/extensions/scratch3_minecraft/index.js
console.log('Connected:', this.connected);
console.log('Sending:', message);
console.log('Received:', message);
```

## デプロイ

### GitHub Pagesへのデプロイ

1. `scratch-gui/package.json` の `homepage` を設定：

```json
{
  "homepage": "https://laughtale01.github.io/Scratch"
}
```

2. デプロイ：

```bash
cd scratch-gui
npm run deploy
```

### 静的ホスティング（Netlify, Vercel等）

```bash
cd scratch-gui
npm run build
# build/ ディレクトリをデプロイ
```

## 参考リンク

- Scratch VM公式: https://github.com/scratchfoundation/scratch-vm
- Scratch GUI公式: https://github.com/scratchfoundation/scratch-gui
- プロジェクトリポジトリ: https://github.com/laughtale01/Scratch
- ドキュメント: ../docs/

## ライセンス

このプロジェクトは、Scratch VM/GUIの派生物であり、BSD 3-Clause Licenseに従います。

---

**バージョン**: 0.1.0
**最終更新**: 2025-11-12
