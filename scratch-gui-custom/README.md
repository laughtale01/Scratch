# Scratch GUI カスタム版 - Minecraft連携対応

## 概要
このプロジェクトは、Scratch GUIに以下の機能を追加したカスタム版です：
1. ドラッグ可能な境界線によるレイアウト調整
2. Minecraft拡張機能の統合
3. takecx版と互換性のあるブロック実装

## セットアップ手順

### 1. Scratch GUIのクローン
```bash
git clone https://github.com/LLK/scratch-gui.git
cd scratch-gui
git checkout v3.0.0
```

### 2. カスタマイズファイルの適用
```bash
# このディレクトリのファイルをscratch-guiにコピー
cp -r ../scratch-gui-custom/src/* ./src/
```

### 3. 依存関係のインストール
```bash
npm install
npm install react-split-pane  # ドラッグ可能な境界線用
```

### 4. Minecraft拡張機能の登録
`src/lib/libraries/extensions/index.jsx`に以下を追加：
```javascript
{
    name: 'Minecraft',
    extensionId: 'minecraft',
    extensionURL: 'http://localhost:8000/minecraft-collaboration-extension.js',
    iconURL: minecraftIcon,
    description: 'Control Minecraft with Scratch blocks',
    featured: true
}
```

### 5. ビルドと実行
```bash
npm start
```

## カスタマイズ内容

### ドラッグ可能な境界線
- `react-split-pane`を使用
- ブロックパレットとスクリプトエリアのサイズを動的に調整可能
- ユーザー設定はlocalStorageに保存

### Minecraft拡張機能
- WebSocket経由でMinecraftと通信
- takecx版と同じブロック構成
- 日本語対応

## 必要な環境
- Node.js v16以上
- npm v8以上
- Minecraft 1.20.1 + Forge
- minecraft-collaboration-mod