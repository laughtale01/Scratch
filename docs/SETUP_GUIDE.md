# 開発環境セットアップガイド

## 目次

1. [前提条件](#前提条件)
2. [ソフトウェアインストール](#ソフトウェアインストール)
3. [リポジトリのforkとclone](#リポジトリのforkとclone)
4. [Scratch VM/GUI のセットアップ](#scratch-vmgui-のセットアップ)
5. [Minecraft MOD のセットアップ](#minecraft-mod-のセットアップ)
6. [動作確認](#動作確認)
7. [トラブルシューティング](#トラブルシューティング)

---

## 前提条件

開発を始める前に、以下を確認してください：

- [ ] Minecraft Java Edition 1.20.x を所有している
- [ ] 基本的なJavaScriptの知識がある
- [ ] 基本的なJavaの知識がある
- [ ] Gitの基本操作ができる
- [ ] コマンドライン/ターミナルの基本操作ができる
- [ ] 10GB以上の空きディスク容量がある

---

## ソフトウェアインストール

### Windows環境

#### 1. Node.js のインストール

**推奨バージョン**: 16.x LTS または 18.x LTS

```powershell
# 公式サイトからインストーラーをダウンロード
# https://nodejs.org/

# インストール確認
node --version  # v16.x.x または v18.x.x
npm --version   # 8.x.x 以上
```

**または、nvm-windows を使用（推奨）**：

```powershell
# nvm-windows インストール: https://github.com/coreybutler/nvm-windows

# Node.js 18 LTS をインストール
nvm install 18
nvm use 18
```

#### 2. Java Development Kit (JDK) のインストール

**必須バージョン**: Java 17

```powershell
# Microsoft OpenJDK 17 をダウンロード
# https://learn.microsoft.com/en-us/java/openjdk/download

# または、Adoptium Temurin 17
# https://adoptium.net/

# インストール確認
java -version   # Java 17.x.x
javac -version  # javac 17.x.x
```

**環境変数設定**：
1. システム環境変数に `JAVA_HOME` を追加
   - 値: `C:\Program Files\Java\jdk-17` (インストール先に応じて変更)
2. `Path` に `%JAVA_HOME%\bin` を追加

#### 3. Git のインストール

```powershell
# Git for Windows をダウンロード
# https://git-scm.com/download/win

# インストール確認
git --version  # git version 2.x.x
```

**Git Bash推奨設定**：
- デフォルトエディタ: VSCode または Nano
- 改行コード: `Checkout as-is, commit as-is`

#### 4. IDE のインストール

**Scratch開発用**: Visual Studio Code
```powershell
# https://code.visualstudio.com/
# 推奨拡張機能:
# - ESLint
# - Prettier
# - JavaScript Debugger
```

**Java/MOD開発用**: IntelliJ IDEA Community Edition
```powershell
# https://www.jetbrains.com/idea/download/
# または、Eclipse IDE for Java Developers
```

---

### macOS環境

#### 1. Homebrew のインストール（未インストールの場合）

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

#### 2. Node.js のインストール

```bash
# nvm を使用（推奨）
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.zshrc  # または ~/.bash_profile

nvm install 18
nvm use 18
nvm alias default 18

# 確認
node --version
npm --version
```

#### 3. Java 17 のインストール

```bash
# Homebrew経由でインストール
brew install openjdk@17

# シンボリックリンク作成
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# 環境変数設定 (~/.zshrc または ~/.bash_profile)
echo 'export JAVA_HOME="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home"' >> ~/.zshrc
source ~/.zshrc

# 確認
java -version
```

#### 4. Git のインストール

```bash
brew install git
git --version
```

---

### Linux環境 (Ubuntu/Debian)

```bash
# Node.js (nvm経由)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc
nvm install 18
nvm use 18

# Java 17
sudo apt update
sudo apt install openjdk-17-jdk -y
java -version

# Git
sudo apt install git -y
git --version
```

---

## リポジトリのforkとclone

### 1. GitHubアカウントでfork

以下のリポジトリをブラウザで開き、「Fork」ボタンをクリック：

1. https://github.com/takecx/scratch-vm
2. https://github.com/takecx/scratch-gui
3. https://github.com/takecx/RemoteControllerMod

### 2. ローカルにclone

```bash
# プロジェクトディレクトリに移動
cd "D:\minecraft laughtare project"  # Windows
# または
cd ~/minecraft-laughtare-project    # macOS/Linux

# Scratch VM
cd scratch-client
git clone https://github.com/YOUR_USERNAME/scratch-vm.git
cd scratch-vm
git checkout develop  # develop ブランチを使用
git remote add upstream https://github.com/takecx/scratch-vm.git
cd ..

# Scratch GUI
git clone https://github.com/YOUR_USERNAME/scratch-gui.git
cd scratch-gui
git remote add upstream https://github.com/takecx/scratch-gui.git
cd ..

# RemoteControllerMod
cd ../minecraft-mod
git clone https://github.com/YOUR_USERNAME/RemoteControllerMod.git
cd RemoteControllerMod
git remote add upstream https://github.com/takecx/RemoteControllerMod.git
```

**`YOUR_USERNAME` を自分のGitHubユーザー名に置き換えてください**

---

## Scratch VM/GUI のセットアップ

### 1. Scratch VM

```bash
cd scratch-client/scratch-vm

# 依存関係インストール
npm install

# ビルド
npm run build

# 開発サーバー起動（テスト用）
npm start
# → http://localhost:8073/playground/ にアクセス
```

**期待される結果**:
- ブラウザでScratchのプレイグラウンドが表示される
- 左側にブロックカテゴリが表示される
- エラーがコンソールに出ていない

### 2. Scratch GUI

別のターミナルを開いて：

```bash
cd scratch-client/scratch-gui

# 依存関係インストール
npm install

# scratch-vm とリンク（重要）
npm link ../scratch-vm

# 開発サーバー起動
npm start
# → http://localhost:8601/ にアクセス
```

**期待される結果**:
- Scratchの完全なGUIが表示される
- 「拡張機能を追加」ボタンが機能する
- サンプルプロジェクトが読み込める

### 3. Minecraft拡張機能の確認

```bash
cd scratch-client/scratch-vm/src/extensions

# scratch3_minecraft ディレクトリの確認
ls scratch3_minecraft/
# 期待される出力:
# block_info.js  enchant_info.js  entity_info.js  index.js  particle_info.js
```

Scratchの拡張機能メニューに「Minecraft」が表示されるか確認してください。

---

## Minecraft MOD のセットアップ

### 1. RemoteControllerMod のビルド

```bash
cd minecraft-mod/RemoteControllerMod

# Windows
gradlew.bat build

# macOS/Linux
./gradlew build
```

**初回ビルド注意**:
- 初回は依存関係のダウンロードに5-10分かかる場合があります
- インターネット接続が必要です

**期待される結果**:
```
BUILD SUCCESSFUL in 2m 15s
```

ビルドされたMODファイルは以下に生成されます：
```
build/libs/remotecontrollermod-x.x.x.jar
```

### 2. Minecraft Forge のインストール

#### Forge 1.20.x のダウンロード

1. https://files.minecraftforge.net/ にアクセス
2. Minecraft 1.20.1（または最新1.20.x）を選択
3. 「Installer」をダウンロード
4. 実行して「Install client」を選択

#### Minecraftランチャーでプロファイル確認

1. Minecraftランチャーを開く
2. 「起動構成」タブ
3. 「forge-1.20.x」プロファイルが追加されていることを確認

### 3. MOD のインストール

```bash
# Windows
# Minecraft mods フォルダ
%APPDATA%\.minecraft\mods\

# macOS
~/Library/Application Support/minecraft/mods/

# Linux
~/.minecraft/mods/
```

ビルドしたJARファイルを上記の `mods` フォルダにコピー：

```bash
# Windows例
copy build\libs\remotecontrollermod-*.jar %APPDATA%\.minecraft\mods\

# macOS/Linux例
cp build/libs/remotecontrollermod-*.jar ~/Library/Application\ Support/minecraft/mods/
```

### 4. Minecraft起動とMOD確認

1. Minecraftランチャーで「forge-1.20.x」プロファイルを選択
2. 「プレイ」をクリック
3. メインメニューで「Mods」ボタンをクリック
4. 「Remote Controller」MODがリストに表示されることを確認

**トラブルシューティング**:
- MODが表示されない場合、Forgeバージョンとビルドバージョンを確認
- クラッシュする場合、`logs/latest.log` を確認

---

## 動作確認

### エンドツーエンドテスト

#### 1. Minecraftサーバー起動

1. Minecraftでシングルプレイヤーワールドを作成
2. クリエイティブモードを選択
3. ワールドに入る

#### 2. MODの動作確認

チャットウィンドウ（`T`キー）で以下を実行：

```
/remotecontroller status
```

期待される出力：
```
Remote Controller is running on port 14711
```

#### 3. Scratchから接続

1. ブラウザで http://localhost:8601/ を開く
2. 「拡張機能を追加」をクリック
3. 「Minecraft」拡張を選択
4. 「接続」ブロックを実行
   - ホスト: `localhost`
   - ポート: `14711`

期待される結果：
```
接続成功メッセージが表示される
```

#### 4. 簡単なテスト

Scratchで以下のブロックを組んで実行：

```
[Minecraft に接続 ▼] ホスト [localhost ▼] ポート [14711 ▼]
[チャットで言う ▼] [Hello from Scratch!]
```

Minecraftのチャットに「Hello from Scratch!」が表示されれば成功！

#### 5. ブロック配置テスト

```
[ブロックを置く ▼] x [~0 ▼] y [~1 ▼] z [~2 ▼] ブロック [stone ▼]
```

プレイヤーの目の前に石ブロックが出現すれば成功！

---

## 開発ワークフロー

### 日常的な開発手順

#### Scratch VM/GUI の開発

```bash
# Terminal 1: Scratch VM
cd scratch-client/scratch-vm
npm start  # → http://localhost:8073/playground/

# Terminal 2: Scratch GUI
cd scratch-client/scratch-gui
npm start  # → http://localhost:8601/

# コード変更時
# → ホットリロードで自動的に反映される
```

#### Minecraft MOD の開発

```bash
cd minecraft-mod/RemoteControllerMod

# IntelliJ IDEAで開く（推奨）
# または

# コマンドラインでビルド
./gradlew build

# Minecraft開発環境で起動（デバッグ用）
./gradlew runClient
```

**デバッグモード**:
```bash
# リモートデバッグポート5005で起動
./gradlew runClient --debug-jvm
```

IntelliJ IDEAで「Remote JVM Debug」構成を作成し、ポート5005に接続。

### Git ワークフロー

```bash
# 機能ブランチ作成
git checkout -b feature/multiplayer-support

# 変更をコミット
git add .
git commit -m "feat: add connection manager for multiplayer"

# プッシュ
git push origin feature/multiplayer-support

# Pull Request作成（GitHub上で）
```

**コミットメッセージ規約**（推奨）:
- `feat:` 新機能
- `fix:` バグ修正
- `docs:` ドキュメント
- `refactor:` リファクタリング
- `test:` テスト追加
- `chore:` ビルド・ツール関連

---

## トラブルシューティング

### よくある問題と解決策

#### 1. `npm install` が失敗する

**症状**:
```
npm ERR! code ELIFECYCLE
```

**解決策**:
```bash
# キャッシュクリア
npm cache clean --force

# node_modules 削除して再インストール
rm -rf node_modules package-lock.json
npm install
```

#### 2. Gradle ビルドが失敗する

**症状**:
```
Could not resolve all files for configuration ':compileClasspath'
```

**解決策**:
```bash
# Gradleキャッシュクリア
./gradlew clean build --refresh-dependencies

# または完全にキャッシュ削除
rm -rf ~/.gradle/caches
./gradlew build
```

#### 3. Minecraft MOD が読み込まれない

**チェックリスト**:
- [ ] Forgeバージョンが1.20.xか確認
- [ ] MOD JARファイルが `mods` フォルダに正しく配置されているか
- [ ] `mods.toml` ファイルが正しく構成されているか
- [ ] `logs/latest.log` でエラーを確認

#### 4. WebSocket接続が失敗する

**症状**:
Scratchから接続できない

**解決策**:
```bash
# ファイアウォール確認（Windows）
netsh advfirewall firewall add rule name="Minecraft Remote Controller" dir=in action=allow protocol=TCP localport=14711

# ポート使用状況確認
netstat -ano | findstr :14711  # Windows
lsof -i :14711                  # macOS/Linux
```

#### 5. scratch-vm と scratch-gui のリンクエラー

**症状**:
```
Module not found: Can't resolve 'scratch-vm'
```

**解決策**:
```bash
# scratch-vm で npm link 作成
cd scratch-client/scratch-vm
npm link

# scratch-gui でリンク
cd ../scratch-gui
npm link scratch-vm

# 確認
npm ls scratch-vm
```

#### 6. Java バージョンエラー

**症状**:
```
Unsupported class file major version 61
```

**解決策**:
```bash
# 現在のJavaバージョン確認
java -version

# Java 17がインストールされているか確認
# JAVA_HOME環境変数が正しく設定されているか確認
echo $JAVA_HOME  # macOS/Linux
echo %JAVA_HOME% # Windows
```

---

## パフォーマンス最適化

### 開発時のTips

1. **Node.js メモリ増加**（大規模プロジェクト時）
   ```bash
   export NODE_OPTIONS="--max-old-space-size=4096"
   npm start
   ```

2. **Gradleビルド高速化**
   `gradle.properties` に追加：
   ```properties
   org.gradle.daemon=true
   org.gradle.parallel=true
   org.gradle.caching=true
   ```

3. **ホットリロード高速化**
   VSCodeで不要なファイル監視を除外：
   ```json
   {
     "files.watcherExclude": {
       "**/node_modules/**": true,
       "**/build/**": true
     }
   }
   ```

---

## 次のステップ

セットアップが完了したら：

1. [ ] `PROJECT_DESIGN.md` を読んで全体設計を理解
2. [ ] フェーズ1のタスクに着手
3. [ ] 簡単な機能追加でワークフローを習得
4. [ ] チーム開発の場合、ブランチ戦略を決定

---

## 参考リンク

### 公式ドキュメント
- [Scratch Extensions Documentation](https://github.com/scratchfoundation/scratch-vm/blob/develop/docs/)
- [Minecraft Forge Documentation](https://docs.minecraftforge.net/)
- [Forge 1.20.x Primer](https://forge.gemwire.uk/wiki/Main_Page)

### コミュニティ
- [Scratch Developer Forum](https://scratch.mit.edu/discuss/31/)
- [Forge Forums](https://forums.minecraftforge.net/)
- [takecx/RemoteControllerMod Issues](https://github.com/takecx/RemoteControllerMod/issues)

---

**最終更新**: 2025-11-12
**対象OS**: Windows 10/11, macOS 12+, Ubuntu 20.04+
