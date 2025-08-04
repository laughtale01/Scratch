# 🎮 Minecraft × Scratch 完全セットアップガイド

## 📋 必要なコンポーネント

### 1. Minecraft環境
- **Minecraft Java Edition** 1.20.1
- **Minecraft Forge** 47.2.0
- **minecraft-collaboration-mod** (このプロジェクトのmod)

### 2. 開発環境
- **Java 17**
- **Node.js v16以上**
- **Git**

### 3. プロジェクトファイル
- **minecraft-mod/** - Minecraft Mod本体
- **scratch-extension/** - Scratch拡張機能
- **scratch-gui-custom/** - カスタマイズ設定

## 🚀 クイックスタート（3ステップ）

### ステップ1: ビルド
```batch
cd minecraft-mod
.\gradlew.bat build

cd ..\scratch-extension
npm install
npm run build
```

### ステップ2: Scratch GUIセットアップ
```batch
setup-scratch-gui.bat
```

### ステップ3: 統合起動
```batch
start-all.bat
```

これで完了！ブラウザで http://localhost:8601 を開いてください。

## 📝 詳細セットアップ

### A. Minecraft Modのインストール

1. **Modのビルド**
   ```batch
   cd minecraft-mod
   .\gradlew.bat clean build
   ```

2. **生成されたファイル**
   - `build/libs/minecraft-collaboration-mod-1.0.0-all.jar`

3. **Modの配置**（オプション）
   - 開発時は`.\gradlew.bat runClient`で直接起動
   - 本番環境では`.minecraft/mods/`フォルダにコピー

### B. Scratch拡張機能の準備

1. **ビルド**
   ```batch
   cd scratch-extension
   npm install
   npm run build
   ```

2. **拡張機能サーバー起動**
   ```batch
   start-extension-server.bat
   ```

### C. Scratch GUIの設定

1. **初回セットアップ**
   ```batch
   setup-scratch-gui.bat
   ```

2. **手動設定が必要な場合**
   - `scratch-gui/src/lib/libraries/extensions/index.jsx`を編集
   - Minecraft拡張を追加

## 🎯 使い方

### 1. Minecraftを起動
```batch
run-minecraft.bat
```
- WebSocketサーバーがポート14711で起動

### 2. 拡張機能サーバーを起動
```batch
start-extension-server.bat
```
- HTTPサーバーがポート8000で起動

### 3. Scratch GUIを起動
```batch
cd scratch-gui
npm start
```
- http://localhost:8601 で起動

### 4. Minecraftブロックを使用
1. 左下の拡張機能ボタンをクリック
2. 「Minecraft」を選択
3. ブロックが追加される

## 🔧 カスタマイズ

### 境界線のサイズ調整
- ブロックパレットとスクリプトエリアの境界線をドラッグ
- 設定は自動保存

### ブロックの追加
`scratch-extension/src/index.js`を編集して新しいブロックを追加可能

## 🐛 トラブルシューティング

### 「Minecraftに接続できません」
1. Minecraftが起動しているか確認
2. WebSocketログを確認：
   ```
   "WebSocket server started on port: 14711"
   ```

### 拡張機能が読み込まれない
1. 拡張機能サーバーを確認：http://localhost:8000
2. ブラウザのコンソールでエラーを確認

### ビルドエラー
1. Java 17が正しくインストールされているか確認
2. `gradle.properties`のJavaパスを確認

## 📁 プロジェクト構造

```
minecraft_collaboration_project/
├── minecraft-mod/          # Forge Mod
├── scratch-extension/      # Scratch拡張
├── scratch-gui-custom/     # GUI設定
├── docs/                   # ドキュメント
├── run-minecraft.bat       # Minecraft起動
├── start-extension-server.bat  # 拡張サーバー
├── setup-scratch-gui.bat   # GUI設定
└── start-all.bat          # 統合起動
```

## 🌐 GitHub統合（将来）

1. このプロジェクトをGitHubにプッシュ
2. GitHub Pagesで拡張機能をホスト
3. URLを更新して公開版として使用

## 🎉 完成！

これで、Scratch × Minecraft統合環境の準備が完了しました。
創造的なプログラミングをお楽しみください！

---
問題が発生した場合は、`docs/`フォルダ内の詳細ドキュメントを参照してください。