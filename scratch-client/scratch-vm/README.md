# MinecraftEdu Scratch VM Extension

Scratch Virtual Machine with custom Minecraft extension

## 概要

このディレクトリには、Minecraftを制御するためのScratch拡張機能が含まれています。

## セットアップ

### 必要なソフトウェア

- Node.js 16.x以上
- npm 8.x以上

### インストール

```bash
npm install
```

### 開発

```bash
# 開発サーバー起動
npm start
# → http://localhost:8073/playground/ にアクセス
```

### ビルド

```bash
# プロダクションビルド
npm run build

# 開発ビルド（watch mode）
npm run dev
```

## Minecraft拡張機能

### 場所

```
src/extensions/scratch3_minecraft/
├── index.js           # メイン拡張機能ファイル
└── README.md          # 拡張機能の説明
```

### 機能

- Minecraftサーバーへの接続（WebSocket）
- チャット送信
- ブロック配置（絶対座標・相対座標）
- エンティティ召喚
- プレイヤーテレポート
- 天気・時刻変更
- その他のMinecraft操作

### 使い方

1. Minecraftで MinecraftEdu MODを起動
2. Scratchのプレイグラウンドで「拡張機能を追加」
3. 「Minecraft」を選択
4. 接続ブロックでMinecraftサーバーに接続

## Scratch GUIとの統合

完全なScratch環境で使用するには、Scratch GUIとの統合が必要です。

### オプション1: 公式Scratch GUI を使用

```bash
# 別ディレクトリで公式Scratch GUIをクローン
git clone https://github.com/scratchfoundation/scratch-gui.git
cd scratch-gui
npm install

# この scratch-vm をリンク
cd ../scratch-vm
npm link

cd ../scratch-gui
npm link scratch-vm

# 起動
npm start
# → http://localhost:8601/
```

### オプション2: カスタムScratch GUI（推奨）

詳細は `../scratch-gui/README.md` を参照してください。

## 開発Tips

### 拡張機能の追加

新しいブロックを追加するには、`src/extensions/scratch3_minecraft/index.js` を編集：

```javascript
// getInfo() メソッドの blocks 配列に追加
{
    opcode: 'newCommand',
    blockType: 'command',
    text: '新しいコマンド [PARAM]',
    arguments: {
        PARAM: {
            type: 'string',
            defaultValue: 'value'
        }
    }
}

// 対応するメソッドを実装
newCommand(args) {
    return this.sendCommand('newCommand', {
        param: args.PARAM
    });
}
```

### デバッグ

ブラウザの開発者ツール（F12）でコンソールを確認：

```javascript
// 接続状態
console.log('Connected:', this.connected);

// 送信メッセージ
console.log('Sending:', message);

// 受信メッセージ
console.log('Received:', message);
```

## トラブルシューティング

### npm install エラー

```bash
# キャッシュクリア
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### WebSocket接続エラー

1. Minecraftが起動しているか確認
2. MinecraftEdu MODが読み込まれているか確認
3. ポート14711が開いているか確認
4. ファイアウォール設定を確認

## ディレクトリ構造

```
scratch-vm/
├── package.json          # プロジェクト設定
├── src/
│   └── extensions/
│       └── scratch3_minecraft/
│           └── index.js  # Minecraft拡張機能
├── node_modules/         # 依存パッケージ
└── README.md            # このファイル
```

## リンク

- Scratch VM公式: https://github.com/scratchfoundation/scratch-vm
- プロジェクトリポジトリ: https://github.com/laughtale01/Scratch
- ドキュメント: ../../docs/

---

**バージョン**: 0.1.0
**最終更新**: 2025-11-12
