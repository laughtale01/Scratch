# 🎮 Minecraft協調学習システム - 利用ガイド

## 📦 あなたが求めていたファイル

### 1️⃣ **Scratchページ** ✅
- **ファイル**: `scratch-page.html`
- **機能**: ブラウザでScratch拡張機能を管理・利用
- **アクセス**: ファイルをダブルクリックしてブラウザで開く

### 2️⃣ **必要なModファイル** ✅
- **ファイル**: `minecraft-mod/build/libs/minecraft-collaboration-mod-1.0.0-all.jar`
- **機能**: WebSocket統合済みMinecraft Mod
- **サイズ**: 約178KB（依存ライブラリ含む）

## 🚀 **簡単な使い方**

### ステップ1: Minecraftサーバー起動
```bash
cd minecraft-mod
gradlew runServer
```

### ステップ2: Scratchページを開く
- `scratch-page.html`をダブルクリック
- ブラウザで開いて接続確認

### ステップ3: 本格利用（Scratch GUI）
```bash
cd scratch-gui  
npm start
```
- http://localhost:8601 でScratchプログラミング

## 📁 **重要ファイル一覧**

```
minecraft_collaboration_project/
├── scratch-page.html              # 🎯 Scratchメインページ
├── minecraft-mod/
│   └── build/libs/
│       └── minecraft-collaboration-mod-1.0.0-all.jar  # 🎯 Minecraftモッドファイル
├── scratch-extension/dist/        # 5つの拡張機能JS
├── scratch-gui/                   # フル機能Scratch環境
└── test-connection.html          # 接続テスト用
```

## 🎯 **利用可能なScratchブロック**

| カテゴリ | ブロック例 | デスクリプション |
|---------|----------|---------------|
| 🔌 **接続** | `🔌 Minecraftに接続` | WebSocket接続開始 |
| 📧 **招待** | `📧 [友達]さんを招待` | 協調学習の招待 |
| 🚪 **訪問** | `🚪 [友達]さんの世界に訪問申請` | ワールド訪問 |
| 🏠 **帰宅** | `🏠 自分のワールドに帰る` | ホーム帰還 |
| 🧱 **建築** | `🧱 [座標]にブロック設置` | ブロック操作 |
| 💬 **チャット** | `💬 チャット: [メッセージ]` | メッセージ送信 |

## ✅ **動作確認済みの環境**
- **OS**: Windows 10
- **Java**: 17.0.15 (Eclipse Adoptium)
- **Node.js**: v24.4.0
- **Minecraft**: 1.20.1 + Forge 47.2.0

---

**🎉 すべてのファイルが準備完了しています！**
**`scratch-page.html`を開いて協調学習を始めましょう！**