# 🎮 公式Scratch GUIベースのカスタムビルド作成ガイド

## 📋 必要な手順

### 1. Scratch GUI のクローン
```bash
git clone https://github.com/LLK/scratch-gui.git
cd scratch-gui
```

### 2. 依存関係のインストール
```bash
npm install
```

### 3. Minecraft拡張機能の追加
```javascript
// src/lib/libraries/extensions/index.jsx に追加
{
    name: 'Minecraft Controller',
    extensionId: 'minecraft',
    iconURL: minecraftIconURL,
    insetIconURL: minecraftInsetIconURL,
    description: 'Control Minecraft from Scratch',
    featured: true,
    internetConnectionRequired: true,
    bluetoothRequired: false,
    launchPeripheralConnectionFlow: false,
    useAutoScan: false,
    extensionURL: 'minecraft-extension.js'
}
```

### 4. ビルド
```bash
npm run build
```

### 5. 静的ファイルのホスティング
- `build/` フォルダの内容をFirebase Hostingにアップロード
- 拡張機能が自動的にロードされる

## 🚨 現在の問題点

現在のTurboWarpベースの実装では：
- 外部サービスに依存
- カスタマイズに制限
- 拡張機能の自動ロードが困難

## ✅ 理想的な実装

参考サイトのように：
1. 公式Scratch GUIをフォーク
2. Minecraft拡張機能を組み込み
3. ビルドして静的ホスティング
4. ユーザーは「拡張機能をクリック」するだけで使える

このアプローチにより、完全にカスタマイズされた専用Scratchエディターが実現できます。