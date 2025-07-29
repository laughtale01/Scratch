# 🚀 Minecraft協調学習システム - 統合セットアップガイド

このガイドは、システムのセットアップに必要なすべての手順を統合したものです。

---

## 📋 事前準備

### システム要件
- **OS**: Windows 10/11, macOS, Linux
- **Java**: 17以上
- **Node.js**: v24.4.0以上
- **メモリ**: 4GB以上推奨
- **Minecraft Java Edition**: 1.20.1

### 必要なツール
1. **Git** - バージョン管理
2. **Java 17** - Minecraft Mod実行用
3. **Node.js** - Scratch拡張ビルド用
4. **任意**: VSCode等のエディタ

---

## 🔧 環境構築

### 1. Java 17のインストール

#### Windows
```powershell
# Chocolateyを使用する場合
choco install openjdk17

# 手動インストールの場合
# https://adoptium.net/ からダウンロード
```

#### macOS
```bash
brew install openjdk@17
```

#### Linux
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### 2. Node.jsのインストール

#### すべてのOS
[Node.js公式サイト](https://nodejs.org/)から最新のLTS版をダウンロード

確認:
```bash
node --version  # v24.4.0以上
npm --version   # 10.0.0以上
```

### 3. プロジェクトのクローン

```bash
git clone https://github.com/YOUR_USERNAME/minecraft-collaboration-system.git
cd minecraft-collaboration-system
```

---

## 🏗️ ビルド手順

### 1. Minecraft Modのビルド

```bash
cd minecraft-mod

# Windows
.\gradlew.bat clean build

# macOS/Linux
./gradlew clean build
```

成功すると以下にJARファイルが生成されます:
- `build/libs/minecraft-collaboration-mod-1.0.0-all.jar`

### 2. Scratch拡張のビルド

```bash
cd ../scratch-extension
npm install
npm run build
```

成功すると以下にJSファイルが生成されます:
- `dist/minecraft-collaboration-extension.js`

---

## 🎮 実行方法

### 1. Minecraftの起動

#### 開発環境での実行
```bash
cd minecraft-mod

# Windows
.\gradlew.bat runClient

# macOS/Linux
./gradlew runClient
```

#### 通常のMinecraftでの実行
1. Minecraft Launcherを起動
2. Forge 1.20.1-47.2.0プロファイルを選択
3. `mods`フォルダに`minecraft-collaboration-mod-1.0.0-all.jar`をコピー
4. Minecraftを起動

### 2. WebSocketサーバーの確認

Minecraftコンソールで以下のメッセージを確認:
```
[MinecraftCollaboration] WebSocket server started on port: 14711
```

### 3. Scratch拡張の読み込み

#### オンライン版Scratch
1. https://scratch.mit.edu/ にアクセス
2. 「作る」をクリック
3. 左下の拡張機能ボタンをクリック
4. URL指定で以下を入力:
   ```
   http://localhost:8080/minecraft-collaboration-extension.js
   ```

#### ローカルHTTPサーバーの起動
```bash
cd scratch-extension
npx http-server dist -p 8080
```

---

## 🧪 動作確認

### 1. 接続テスト

```bash
cd tests
node test-websocket.js
```

期待される出力:
```
Connected to WebSocket server
Sent: {"command":"getPlayerPos"}
Received: {"x":100,"y":64,"z":100}
```

### 2. Scratchでのテスト

1. 「🔌 Minecraftに接続する」ブロックを配置
2. 緑の旗をクリック
3. 「📡 接続されている？」が真を返すことを確認

---

## 🔧 トラブルシューティング

### よくある問題と解決方法

#### Java関連のエラー
```bash
# Javaバージョンの確認
java -version

# JAVA_HOMEの設定（Windows）
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot
```

#### WebSocket接続エラー
1. ポート14711が使用されていないか確認
2. ファイアウォール設定を確認
3. Minecraftが起動しているか確認

#### ビルドエラー
```bash
# キャッシュクリア
cd minecraft-mod
.\gradlew.bat clean
.\gradlew.bat --refresh-dependencies
```

---

## 🌟 クイックスタート（最短手順）

経験者向けの最短セットアップ:

```bash
# 1. クローン
git clone [REPO_URL] && cd minecraft-collaboration-system

# 2. Modビルド
cd minecraft-mod && .\gradlew.bat build

# 3. 拡張ビルド
cd ../scratch-extension && npm install && npm run build

# 4. Minecraft起動
cd ../minecraft-mod && .\gradlew.bat runClient

# 5. 拡張サーバー起動（別ターミナル）
cd ../scratch-extension && npx http-server dist -p 8080
```

---

## 📚 次のステップ

1. [使い方ガイド](../USER_GUIDE.md) - 基本的な使い方
2. [API仕様](../api/API_REFERENCE.md) - 詳細な技術仕様
3. [開発ガイド](../development/DEVELOPMENT_GUIDE.md) - 機能追加方法

---

## 🆘 サポート

問題が解決しない場合:
1. [トラブルシューティングガイド](../troubleshooting.md)を確認
2. [GitHubのIssue](https://github.com/YOUR_USERNAME/minecraft-collaboration-system/issues)で報告
3. [Discord/Slack]でコミュニティに質問

---

最終更新: 2025年7月26日