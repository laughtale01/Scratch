# 開発環境セットアップガイド - 2025-08-06

## 📋 前提条件

### 必須ソフトウェア
- **Java 17** (Java 21は非対応)
- **Node.js 16+** (Scratch GUI用)
- **Git**
- **Minecraft Java Edition 1.20.1**
- **Minecraft Forge 47.2.0**

## 🔧 環境セットアップ

### 1. Java 17のインストールと設定

#### Windows
```powershell
# Java 17をダウンロード
# https://www.oracle.com/java/technologies/downloads/#java17

# 環境変数の設定
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 確認
java -version
# 出力: java version "17.0.X"
```

#### Mac/Linux
```bash
# Macの場合
brew install openjdk@17
export JAVA_HOME=/usr/local/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH

# Linuxの場合
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# 確認
java -version
```

### 2. プロジェクトのクローン

```bash
git clone https://github.com/yourusername/minecraft_collaboration_project.git
cd minecraft_collaboration_project
```

### 3. Minecraft Modのビルド

```bash
cd minecraft-mod

# gradle.propertiesの確認（Java 17パスが正しいことを確認）
cat gradle.properties

# ビルド実行（テストをスキップ）
./gradlew jarJar -x test

# 成功すると以下のファイルが生成される
# build/libs/minecraft-collaboration-mod-1.0.0-all.jar
```

### 4. Scratch GUIのセットアップ

```bash
cd ../scratch-gui

# 依存関係のインストール
npm install

# 開発サーバーの起動
npm start
# → http://localhost:8601 でアクセス可能

# プロダクションビルド
npm run build
```

### 5. Scratch拡張機能のビルド

```bash
cd ../scratch-extension

# 依存関係のインストール
npm install

# ビルド
npm run build

# 開発モード（自動リビルド）
npm run dev
```

## 🚀 動作確認

### 1. Minecraft Modのデプロイ

```bash
# Windows
copy minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar "%APPDATA%\.minecraft\mods\"

# Mac/Linux
cp minecraft-mod/build/libs/minecraft-collaboration-mod-1.0.0-all.jar ~/.minecraft/mods/
```

### 2. Minecraftの起動
1. Minecraft Launcher起動
2. Forge 1.20.1-47.2.0プロファイル選択
3. プレイボタンクリック
4. ワールドに参加

### 3. Scratch GUIでの接続
1. http://localhost:8601 にアクセス
2. 拡張機能ボタンをクリック
3. 「Minecraft コラボレーション」を選択
4. 「Minecraftに接続する」ブロックを実行

### 4. 接続確認
- Minecraft内でF3キーを押してデバッグ情報表示
- 「WebSocket: Connected」が表示されることを確認

## 🐛 トラブルシューティング

### Java関連の問題

#### エラー: Unsupported class file major version 65
```bash
# Java 21が使用されている
# 解決方法：Java 17に切り替える
export JAVA_HOME=/path/to/java17
./gradlew clean
./gradlew jarJar -x test
```

#### Gradle実行エラー
```bash
# Gradleキャッシュをクリア
rm -rf ~/.gradle/caches/
cd minecraft-mod
./gradlew clean
./gradlew jarJar -x test
```

### WebSocket接続の問題

#### 接続できない場合
1. ポート14711が使用されていないか確認
```bash
# Windows
netstat -an | findstr 14711

# Mac/Linux
lsof -i :14711
```

2. ファイアウォール設定を確認
3. Minecraft内でデバッグモードを有効化（F3）

### ビルドエラー

#### npm installエラー
```bash
# node_modulesとpackage-lock.jsonを削除
rm -rf node_modules package-lock.json
npm install
```

#### Gradleビルドエラー
```bash
# プロジェクトをクリーンアップ
cd minecraft-mod
./gradlew clean
rm -rf build/
./gradlew jarJar -x test --stacktrace
```

## 📝 開発のヒント

### 1. 効率的な開発フロー
- Minecraft Modの変更時は`./gradlew jarJar -x test`でビルド
- Scratch拡張の変更時は`npm run dev`で自動リビルド
- 両方を同時に開発する場合は2つのターミナルを使用

### 2. デバッグ方法
- Minecraft: F3キーでデバッグ情報表示
- WebSocket: `test-websocket.html`で接続テスト
- Scratch: ブラウザの開発者ツールでコンソールログ確認

### 3. コミット前のチェック
```bash
# Minecraft Modのビルド確認
cd minecraft-mod && ./gradlew jarJar -x test

# Scratch拡張のビルド確認
cd ../scratch-extension && npm run build

# Scratch GUIのビルド確認
cd ../scratch-gui && npm run build
```

## 🔄 CI/CD

GitHub Actionsが設定済み：
- プッシュ時に自動ビルド
- Java 17とNode.js 16/18を使用
- ビルド成果物を自動保存

## 📞 サポート

問題が解決しない場合：
1. `docs/troubleshooting.md`を確認
2. GitHubのIssuesに報告
3. プロジェクトのDiscordに参加（リンクはREADME参照）