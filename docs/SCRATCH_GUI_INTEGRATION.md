# Scratch GUI統合ガイド

## 概要
このドキュメントでは、カスタマイズされたScratch GUIとMinecraft拡張機能の統合方法を説明します。

## 主な機能

### 1. ドラッグ可能な境界線
- ブロックパレットとスクリプトエリアの境界線をドラッグして、サイズを調整可能
- 設定はlocalStorageに保存され、次回起動時も維持される
- 最小幅: 200px、最大幅: 600px

### 2. Minecraft拡張機能
完全なMinecraft制御ブロックセット：

#### 接続管理
- 🔌 Minecraftに接続
- 📡 接続状態確認

#### プレイヤー操作
- 📍 プレイヤー位置取得（X/Y/Z）
- 🚀 プレイヤーテレポート
- 🎮 ゲームモード変更

#### ブロック操作
- 🧱 ブロック配置
- ⛏️ ブロック破壊
- 🔍 ブロック種類取得
- 🧽 範囲クリア

#### 建築機能
- ⭕ 円形建築
- 🌕 球体建築
- 🧱 壁建築
- 🏠 家建築

#### ワールド制御
- 🕐 時間設定
- 🌤️ 天候制御

#### コミュニケーション
- 💬 チャット送信

## セットアップ手順

### 必要なもの
1. Node.js v16以上
2. Git
3. Minecraft Java Edition 1.20.1
4. Minecraft Forge 47.2.0
5. minecraft-collaboration-mod

### インストール

#### Windows
```batch
REM 1. セットアップスクリプトを実行
setup-scratch-gui.bat

REM 2. 統合起動
start-all.bat
```

#### macOS/Linux
```bash
# 1. セットアップスクリプトを実行
chmod +x setup-scratch-gui.sh
./setup-scratch-gui.sh

# 2. 各コンポーネントを起動
# ターミナル1
cd scratch-extension
python -m http.server 8000

# ターミナル2
./run-minecraft.bat

# ターミナル3
cd scratch-gui
npm start
```

## アーキテクチャ

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Scratch GUI    │────▶│  Extension      │────▶│  Minecraft      │
│  (Port 8601)    │     │  (Port 8000)    │     │  (Port 14711)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                        │                        │
        │   HTTP Request         │   WebSocket           │
        └────────────────────────┴────────────────────────┘
```

## カスタマイズ

### ブロックの追加
`scratch-extension/src/index.js`の`getInfo()`メソッドに新しいブロックを追加：

```javascript
{
    opcode: 'newBlock',
    blockType: BlockType.COMMAND,
    text: '新しいブロック [ARG]',
    arguments: {
        ARG: {
            type: ArgumentType.STRING,
            defaultValue: 'default'
        }
    }
}
```

### 境界線のデフォルトサイズ変更
`scratch-gui-custom/src/components/gui/gui.jsx`の`GUI_SIZES`を編集：

```javascript
const GUI_SIZES = {
    blocksPane: {
        default: 450,  // 変更
        min: 300,      // 変更
        max: 700       // 変更
    }
};
```

## トラブルシューティング

### 拡張機能が表示されない
1. 拡張機能サーバーが起動しているか確認（ポート8000）
2. ブラウザの開発者ツールでエラーを確認
3. CORSエラーの場合は、`--cors`オプション付きでサーバー起動

### WebSocket接続エラー
1. Minecraftが起動しているか確認
2. minecraft-collaboration-modが読み込まれているか確認
3. ポート14711が開いているか確認

### 境界線がドラッグできない
1. react-split-paneがインストールされているか確認
2. CSSが正しく読み込まれているか確認

## 今後の拡張予定
- [ ] ブロックカテゴリの追加
- [ ] カスタムテーマ対応
- [ ] マルチプレイヤー対応
- [ ] ブロック実行履歴機能
- [ ] オフライン動作モード