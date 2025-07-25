# 🚀 Minecraft協調学習システム セットアップガイド

## 📋 前提条件
- Java 17がインストールされていること
- Node.js v24.4.0以上がインストールされていること
- Minecraftクライアントがインストールされていること

## 🔧 セットアップ手順

### 1. プロジェクトのクローン
```bash
git clone [repository-url]
cd minecraft_collaboration_project
```

### 2. Minecraft Modのビルド
```bash
cd minecraft-mod
.\gradlew.bat clean build
```

成功すると、`build/libs/minecraft-collaboration-mod-1.0.0-all.jar`が生成されます。

### 3. Scratch拡張のビルド
```bash
cd scratch-extension
npm install
npm run build
```

成功すると、`dist/minecraft-collaboration-extension.js`が生成されます。

## 🎮 使用方法

### 1. Minecraft Modの起動
```bash
# プロジェクトルートから
.\run-minecraft.bat

# または直接実行
cd minecraft-mod
.\gradlew.bat runClient
```

### 2. 動作確認
Minecraftが起動したら、ログで以下を確認：
- "Minecraft Collaboration Mod initialized"
- "WebSocket library is available" 
- "WebSocket server started on port: 14711"

### 3. Scratch拡張の使用

#### Scratch GUIでの使用
1. Scratch GUI (https://scratch.mit.edu/projects/editor/) を開く
2. 左下の拡張機能ボタンをクリック
3. 「拡張機能を選ぶ」画面でカスタム拡張機能を追加
4. `dist/minecraft-collaboration-extension.js`をアップロード

#### ローカルScratch環境での使用
```bash
# HTTPサーバーを起動
cd scratch-extension
python -m http.server 8000

# Scratch GUIで http://localhost:8000/dist/minecraft-collaboration-extension.js を読み込み
```

### 4. 基本的な使い方

#### 接続
```scratch
🔌 Minecraftに接続する
```

#### ブロック配置
```scratch
🧱 [stone]を X:[0] Y:[64] Z:[0] に置く
```

#### 建築
```scratch
🏠 [oak_planks]で X:[10] Y:[64] Z:[10] に 幅:[7] 奥行:[7] 高さ:[4] の家を作る
```

## 🐛 トラブルシューティング

### WebSocketエラー
- ポート14711が他のアプリケーションで使用されていないか確認
- Windows Defenderファイアウォールでブロックされていないか確認

### ビルドエラー
- Java 17が正しくインストールされているか確認: `java -version`
- Node.jsが正しくインストールされているか確認: `node --version`

### 実行時エラー
- `minecraft-mod/run/logs/latest.log`でエラー詳細を確認
- メモリ不足の場合は、`gradle.properties`でメモリ設定を調整

## 📚 開発者向け情報

### プロジェクト構造
```
minecraft_collaboration_project/
├── minecraft-mod/        # Minecraft Forge 1.20.1 Mod
├── scratch-extension/    # Scratch 3.0拡張機能
├── docs/                # ドキュメント
└── config/              # 設定ファイル
```

### 主要ポート
- WebSocketサーバー: 14711
- 協調サーバー: 14712（将来実装予定）

### デバッグモード
```bash
# 詳細ログを有効化
cd minecraft-mod
.\gradlew.bat runClient --debug
```