# MinecraftEdu 実行ガイド

初めてMinecraftEduを使用する方向けの詳細な手順書です。

## 目次

1. [前提条件の確認とインストール](#1-前提条件の確認とインストール)
2. [Minecraft MODのビルドとインストール](#2-minecraft-modのビルドとインストール)
3. [Scratch環境のセットアップ](#3-scratch環境のセットアップ)
4. [Minecraftの起動とMOD確認](#4-minecraftの起動とmod確認)
5. [Scratchの起動と接続](#5-scratchの起動と接続)
6. [動作確認](#6-動作確認)
7. [トラブルシューティング](#7-トラブルシューティング)

---

## 1. 前提条件の確認とインストール

### 1.1 必要なソフトウェア

MinecraftEduを動作させるには、以下のソフトウェアが必要です：

- ✅ Java JDK 17以上（Minecraft MODのビルド用）
- ✅ Node.js 16.x以上（Scratch環境用）
- ✅ Minecraft Java Edition 1.20.1
- ✅ Minecraft Forge 1.20.1（バージョン47.2.0推奨）

### 1.2 Java JDK 17のインストール

#### Windows

1. Oracle JDK公式サイトへアクセス:
   https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html

2. "Windows x64 Installer" をダウンロード

3. インストーラーを実行し、指示に従ってインストール

4. インストール確認：
   ```cmd
   java -version
   ```

   以下のような出力が表示されればOK：
   ```
   java version "17.0.x"
   Java(TM) SE Runtime Environment (build 17.0.x+x)
   ```

#### macOS

1. Homebrewを使用してインストール：
   ```bash
   brew install openjdk@17
   ```

2. パスを設定：
   ```bash
   echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
   source ~/.zshrc
   ```

3. 確認：
   ```bash
   java -version
   ```

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

### 1.3 Node.js 16.x以上のインストール

#### Windows

1. Node.js公式サイトへアクセス:
   https://nodejs.org/

2. "LTS" バージョンをダウンロード（推奨）

3. インストーラーを実行し、全てデフォルト設定でインストール

4. インストール確認：
   ```cmd
   node --version
   npm --version
   ```

#### macOS

Homebrewを使用：
```bash
brew install node
node --version
npm --version
```

#### Linux (Ubuntu/Debian)

```bash
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt install -y nodejs
node --version
npm --version
```

### 1.4 Minecraft Java Edition 1.20.1

1. Minecraft Launcherをインストール（未インストールの場合）
   https://www.minecraft.net/ja-jp/download

2. Minecraftを起動し、"Installations" タブへ

3. "New installation" をクリック

4. バージョンを "release 1.20.1" に設定

5. 名前を "1.20.1" などに設定して保存

### 1.5 Minecraft Forge 1.20.1のインストール

1. Forge公式サイトへアクセス:
   https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html

2. **Recommended (推奨)** の "Installer" をクリックしてダウンロード
   - バージョン 47.2.0 を推奨

3. ダウンロードした `.jar` ファイルをダブルクリック
   - Javaが関連付けされていない場合：右クリック → "Open with" → "Java"

4. インストーラーで "Install client" を選択

5. Minecraftのインストール先を確認（通常は自動検出）

6. "OK" をクリックしてインストール

7. インストール完了後、Minecraft Launcherを確認
   - "forge-1.20.1" プロファイルが作成されているはずです

---

## 2. Minecraft MODのビルドとインストール

### 2.1 プロジェクトのダウンロード

既にプロジェクトをダウンロード済みの場合はスキップしてください。

```bash
# GitHubからクローン
git clone https://github.com/laughtale01/Scratch.git
cd Scratch
```

### 2.2 MODのビルドとインストール

#### Windows

1. コマンドプロンプトまたはPowerShellを開く

2. プロジェクトのtoolsディレクトリへ移動：
   ```cmd
   cd tools
   ```

3. ビルド＆インストールスクリプトを実行：
   ```cmd
   build-and-install.bat
   ```

4. 処理が進行します（初回は5〜15分かかる場合があります）：
   ```
   [1/5] Checking Java...
   [2/5] Checking Gradle Wrapper...
   [3/5] Building MOD...
   [4/5] Locating built JAR file...
   [5/5] Installing to Minecraft...
   ```

5. 完了メッセージが表示されれば成功：
   ```
   ========================================
   Installation Complete!
   ========================================
   ```

#### macOS / Linux

1. ターミナルを開く

2. プロジェクトのtoolsディレクトリへ移動：
   ```bash
   cd tools
   ```

3. スクリプトに実行権限を付与：
   ```bash
   chmod +x build-and-install.sh
   ```

4. ビルド＆インストールスクリプトを実行：
   ```bash
   ./build-and-install.sh
   ```

5. 完了メッセージが表示されれば成功

### 2.3 インストール確認

MODファイルが以下の場所にコピーされているか確認：

- **Windows**: `%APPDATA%\.minecraft\mods\`
- **macOS**: `~/Library/Application Support/minecraft/mods`
- **Linux**: `~/.minecraft/mods`

`minecraftedu-0.1.0.jar` （または類似の名前）のファイルが存在すればOKです。

---

## 3. Scratch環境のセットアップ

### 3.1 Scratchクライアントのセットアップ

#### Windows

1. コマンドプロンプトまたはPowerShellを開く

2. プロジェクトのscratch-clientディレクトリへ移動：
   ```cmd
   cd scratch-client
   ```

3. セットアップスクリプトを実行：
   ```cmd
   setup.bat
   ```

4. 処理が進行します（初回は数分かかります）：
   ```
   [1/4] Checking Node.js...
   [2/4] Installing Scratch VM dependencies...
   [3/4] Installing Scratch GUI dependencies...
   [4/4] Linking scratch-vm to scratch-gui...
   ```

5. 完了メッセージが表示されれば成功：
   ```
   ========================================
   Setup Complete!
   ========================================
   ```

#### macOS / Linux

1. ターミナルを開く

2. プロジェクトのscratch-clientディレクトリへ移動：
   ```bash
   cd scratch-client
   ```

3. スクリプトに実行権限を付与：
   ```bash
   chmod +x setup.sh
   ```

4. セットアップスクリプトを実行：
   ```bash
   ./setup.sh
   ```

5. 完了メッセージが表示されれば成功

### 3.2 セットアップエラーが発生した場合

#### エラー: "npm install failed"

キャッシュをクリアして再試行：

```bash
npm cache clean --force
rm -rf scratch-vm/node_modules scratch-gui/node_modules

# 再度セットアップ
./setup.bat  # または ./setup.sh
```

#### エラー: "npm link failed"

管理者権限で実行：

**Windows**: コマンドプロンプトを「管理者として実行」してから `setup.bat`

**macOS/Linux**: `sudo ./setup.sh`

または、手動でリンク：

```bash
cd scratch-vm
npm link

cd ../scratch-gui
npm link scratch-vm
```

---

## 4. Minecraftの起動とMOD確認

### 4.1 Minecraftの起動

1. Minecraft Launcherを起動

2. プロファイル選択で "forge-1.20.1" を選択

3. "Play" をクリック

4. Minecraftが起動したら、メインメニューで "Mods" ボタンをクリック

5. MODリストに "MinecraftEdu Scratch Controller" が表示されていることを確認

   **表示されていない場合**:
   - MODファイルが正しい場所にあるか確認
   - Forgeのバージョンが1.20.1であることを確認
   - Minecraftを再起動

### 4.2 ワールドの作成または読み込み

1. "Singleplayer" を選択

2. 新しいワールドを作成するか、既存のワールドを選択

3. ワールドに入る

### 4.3 WebSocketサーバーの起動確認

MODが正常にロードされると、WebSocketサーバーが自動的に起動します。

#### 確認方法1: ゲーム内チャット

ワールド内で以下のメッセージが表示されます：

```
[MinecraftEdu] WebSocket server started on port 14711
```

#### 確認方法2: ログファイル

Minecraftのログファイルを確認：

- **Windows**: `%APPDATA%\.minecraft\logs\latest.log`
- **macOS**: `~/Library/Application Support/minecraft/logs/latest.log`
- **Linux**: `~/.minecraft/logs/latest.log`

ログファイル内で以下を検索：

```
WebSocket server started on port 14711
```

**表示されていない場合**:
- MODが正しくロードされているか "Mods" メニューで確認
- Forgeのバージョンを確認
- ログファイルでエラーメッセージを確認

---

## 5. Scratchの起動と接続

### 5.1 Scratch GUIの起動

1. 新しいターミナル/コマンドプロンプトを開く

2. プロジェクトの `scratch-client/scratch-gui` ディレクトリへ移動：
   ```bash
   cd scratch-client/scratch-gui
   ```

3. 開発サーバーを起動：
   ```bash
   npm start
   ```

4. 起動ログが表示されます：
   ```
   webpack compiled successfully
   ```

5. ブラウザが自動的に開き、http://localhost:8601/ にアクセスします

   **自動的に開かない場合**:
   - 手動でブラウザを開き、 http://localhost:8601/ にアクセス

### 5.2 Minecraft拡張機能の追加

1. Scratch画面で、左下の**「拡張機能を追加」**ボタンをクリック
   - 青い「+」アイコンのボタン

2. 拡張機能一覧から **「Minecraft」** を選択

3. 画面左のブロックパレットに「Minecraft」カテゴリが追加される

### 5.3 Minecraftサーバーへの接続

1. 「Minecraft」カテゴリから以下のブロックをワークスペースにドラッグ：
   ```
   Minecraftに接続 ホスト [localhost] ポート [14711]
   ```

2. デフォルト値のまま（`localhost` と `14711`）でOK

3. ブロックをクリックして実行

4. 接続成功の確認方法：

   **方法1: ブラウザの開発者ツール**
   - F12キーを押して開発者ツールを開く
   - "Console" タブを確認
   - 以下のようなメッセージが表示されればOK：
     ```
     Connected to Minecraft server
     Session ID: xxxxx-xxxxx-xxxxx
     ```

   **方法2: Minecraftゲーム内**
   - Minecraftのチャットに以下が表示される：
     ```
     [MinecraftEdu] Client connected
     ```

**接続できない場合**:
- Minecraftが起動しているか確認
- Minecraftでワールドに入っているか確認
- WebSocketサーバーが起動しているか確認（4.3参照）
- ファイアウォールでポート14711がブロックされていないか確認

---

## 6. 動作確認

接続が成功したら、実際にブロックを使ってMinecraftを操作してみましょう。

### 6.1 チャットメッセージの送信

1. 以下のブロックを配置：
   ```
   チャットで言う [Hello from Scratch!]
   ```

2. ブロックをクリック

3. Minecraftのチャット欄に "Hello from Scratch!" が表示されることを確認

### 6.2 ブロックの配置

1. Minecraftで自分の座標を確認：
   - F3キーを押す（デバッグ画面を開く）
   - "Block: X Y Z" の値を確認

2. Scratchで以下のブロックを配置：
   ```
   ブロックを置く x:[自分のX座標+5] y:[自分のY座標] z:[自分のZ座標] ブロック:[minecraft:diamond_block]
   ```

3. ブロックをクリック

4. Minecraftで自分の近くにダイヤモンドブロックが出現することを確認

### 6.3 エンティティの召喚

1. 以下のブロックを配置：
   ```
   エンティティを召喚 x:[X座標] y:[Y座標+2] z:[Z座標] エンティティ:[minecraft:pig]
   ```

2. ブロックをクリック

3. Minecraftで豚が召喚されることを確認

### 6.4 簡単なプログラムの作成

複数のブロックを組み合わせて、簡単なプログラムを作成してみましょう：

```scratch
イベント「緑の旗がクリックされたとき」
↓
Minecraft「Minecraftに接続 ホスト [localhost] ポート [14711]」
↓
制御「1秒待つ」
↓
Minecraft「チャットで言う [Building started!]」
↓
制御「10回繰り返す」
  ├ 変数「counter を 1ずつ変える」
  ├ Minecraft「ブロックを置く x:[counter] y:[10] z:[0] ブロック:[minecraft:stone]」
  └ 制御「0.5秒待つ」
↓
Minecraft「チャットで言う [Building completed!]」
```

このプログラムを実行すると、Minecraftに石ブロックが1列に10個並びます。

---

## 7. トラブルシューティング

### 7.1 Minecraft MODが動作しない

#### 症状: MODがMODリストに表示されない

**原因と解決策**:

1. **Forgeのバージョンが正しくない**
   - Minecraft 1.20.1用のForge（47.2.0推奨）を使用しているか確認
   - Minecraft Launcherで "forge-1.20.1" プロファイルを選択

2. **MODファイルが正しい場所にない**
   - MODファイルの場所を確認：
     - Windows: `%APPDATA%\.minecraft\mods\`
     - macOS: `~/Library/Application Support/minecraft/mods`
     - Linux: `~/.minecraft/mods`
   - `minecraftedu-0.1.0.jar` が存在するか確認

3. **ビルドが失敗している**
   - `tools/build-only.bat`（または`.sh`）を実行してビルドログを確認
   - エラーメッセージに基づいて対処

#### 症状: WebSocketサーバーが起動しない

**原因と解決策**:

1. **ポート14711が既に使用されている**
   - 他のプログラムがポート14711を使用していないか確認
   - Windowsの場合：`netstat -ano | findstr 14711`
   - macOS/Linuxの場合：`lsof -i :14711`

2. **ファイアウォールでブロックされている**
   - Windowsファイアウォールの設定を確認
   - セキュリティソフトの設定を確認

3. **MODのログにエラーが出ている**
   - `logs/latest.log` を確認
   - "MinecraftEdu" でログを検索してエラーメッセージを確認

### 7.2 Scratch環境が動作しない

#### 症状: npm start が失敗する

**原因と解決策**:

1. **Node.jsのバージョンが古い**
   - `node --version` で確認（16.x以上が必要）
   - 最新のLTSバージョンをインストール

2. **依存関係のインストールに失敗している**
   - キャッシュをクリア：
     ```bash
     npm cache clean --force
     rm -rf node_modules
     npm install
     ```

3. **ポート8601が既に使用されている**
   - 別のポートで起動：
     ```bash
     PORT=8602 npm start
     ```

#### 症状: Minecraft拡張機能が表示されない

**原因と解決策**:

1. **拡張機能が正しく登録されていない**
   - `scratch-gui/src/lib/libraries/extensions/index.jsx` を確認
   - Minecraftエントリが存在するか確認

2. **scratch-vmがリンクされていない**
   - リンクを確認：
     ```bash
     cd scratch-gui
     npm ls scratch-vm
     ```
   - 再リンク：
     ```bash
     cd ../scratch-vm
     npm link
     cd ../scratch-gui
     npm link scratch-vm
     ```

3. **ブラウザキャッシュの問題**
   - ブラウザで Ctrl+Shift+R（強制リロード）
   - または、ブラウザキャッシュをクリア

### 7.3 接続エラー

#### 症状: "Connection failed" エラーが出る

**原因と解決策**:

1. **Minecraftが起動していない**
   - Minecraftを起動してワールドに入る

2. **WebSocketサーバーが起動していない**
   - Minecraftのログで "WebSocket server started" を確認
   - MODが正しくロードされているか確認

3. **ホストまたはポートが間違っている**
   - 接続ブロックで `localhost` と `14711` を使用
   - Minecraftが別のPCで動作している場合は、そのIPアドレスを使用

4. **ファイアウォールでブロックされている**
   - Windowsファイアウォールで例外を追加
   - セキュリティソフトで14711番ポートを許可

#### 症状: 接続後すぐに切断される

**原因と解決策**:

1. **Minecraftサーバーのエラー**
   - Minecraftのログファイルでエラーを確認

2. **ネットワークの問題**
   - localhostでの接続を確認
   - 別のPCからの接続の場合、ネットワーク設定を確認

### 7.4 ブロック実行エラー

#### 症状: ブロックを実行してもMinecraftで何も起こらない

**原因と解決策**:

1. **接続が確立されていない**
   - ブラウザの開発者ツール（F12）でコンソールを確認
   - "Connected to Minecraft server" が表示されているか確認

2. **座標が正しくない**
   - ブロック配置の座標を確認
   - Minecraftで自分の現在座標を確認（F3キー）

3. **ブロックIDが正しくない**
   - 正しいブロックID形式を使用：`minecraft:block_name`
   - 例：`minecraft:stone`, `minecraft:diamond_block`

4. **権限の問題（マルチプレイの場合）**
   - オペレーター権限があるか確認
   - `/op <プレイヤー名>` でOP権限を付与

#### 症状: エラーメッセージが表示される

**デバッグ方法**:

1. **ブラウザの開発者ツールを開く**（F12キー）

2. **Consoleタブでエラーメッセージを確認**
   - WebSocketエラー
   - JavaScript実行エラー
   - ネットワークエラー

3. **Networkタブで通信を確認**
   - WS（WebSocket）接続を確認
   - 送受信メッセージを確認

4. **Minecraftのログを確認**
   - `logs/latest.log` でサーバー側のエラーを確認

### 7.5 よくある質問

#### Q: 複数のScratchクライアントから同時に接続できますか？

A: はい、可能です。MinecraftEduは最大10クライアントの同時接続をサポートしています。

#### Q: Minecraftをサーバーモードで実行できますか？

A: 現在のバージョンはシングルプレイヤーモードのみをサポートしています。マルチプレイヤーサーバーのサポートは今後のバージョンで追加予定です。

#### Q: Scratchプロジェクトを保存できますか？

A: はい、Scratch GUIの「ファイル」→「コンピューターに保存」で.sb3ファイルとして保存できます。

#### Q: カスタムブロックを追加できますか？

A: はい、`scratch-vm/src/extensions/scratch3_minecraft/index.js` を編集することで新しいブロックを追加できます。詳細は開発ドキュメントを参照してください。

---

## 次のステップ

基本的な動作確認ができたら、以下のリソースを参考にしてください：

- **チュートリアル**: `docs/TUTORIAL.md` - Scratchでのプログラミング例
- **ブロックリファレンス**: `scratch-client/scratch-vm/src/extensions/scratch3_minecraft/README.md`
- **開発ガイド**: `docs/DEVELOPMENT.md` - カスタマイズ方法
- **APIリファレンス**: `shared/protocol/PROTOCOL_SPEC.md` - プロトコル仕様

---

## サポート

問題が解決しない場合は、以下の方法でサポートを受けられます：

- **GitHub Issues**: https://github.com/laughtale01/Scratch/issues
- **ドキュメント**: `docs/` ディレクトリ内の詳細ドキュメント

---

**最終更新**: 2025-11-12
**バージョン**: 0.1.0
