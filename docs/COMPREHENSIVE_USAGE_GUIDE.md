# 🎮 Minecraft×Scratch協調学習システム - 完全使い方ガイド

**最終更新**: 2025年10月2日
**バージョン**: v1.5.1
**対象**: 初心者〜上級者

---

## 📚 目次

1. [クイックスタート（5分で始める）](#クイックスタート5分で始める)
2. [基本的な使い方（学生・教師向け）](#基本的な使い方学生教師向け)
3. [Scratchブロック完全ガイド](#scratchブロック完全ガイド)
4. [開発者向けガイド](#開発者向けガイド)
5. [トラブルシューティング](#トラブルシューティング)
6. [よくある質問（FAQ）](#よくある質問faq)

---

## 🚀 クイックスタート（5分で始める）

### 📋 必要なもの

- **Minecraft Java Edition 1.20.1** (有料)
- **Java 17** (無料 - 自動インストール可能)
- **ブラウザ** (Chrome/Firefox/Edge最新版)

### ⚡ 最速セットアップ（Windows）

#### ステップ1: Java 17のインストール（1分）

```bash
# 環境変数を自動設定
set-java17-vscode.bat
```

**または手動インストール**:
1. [Adoptium OpenJDK 17](https://adoptium.net/temurin/releases/?version=17) からダウンロード
2. インストーラー実行
3. 「Set JAVA_HOME variable」にチェック

**確認**:
```bash
java -version
# 出力: openjdk version "17.0.X" ...
```

#### ステップ2: Minecraftのセットアップ（2分）

1. **Forge 1.20.1のインストール**
   - [Minecraft Forge](https://files.minecraftforge.net/) にアクセス
   - バージョン「1.20.1」→「Installer」をダウンロード
   - 実行して「Install Client」を選択

2. **Modのインストール**
   - [リリースページ](https://github.com/laughtale01/Scratch/releases) から最新版ダウンロード
   - `minecraft-collaboration-mod-1.0.0-all.jar` を取得
   - `Win + R` → `%appdata%` と入力
   - `.minecraft/mods/` フォルダにコピー（フォルダがなければ作成）

3. **Minecraft起動**
   - Minecraft Launcher起動
   - 「Forge 1.20.1」プロファイルを選択
   - 「プレイ」をクリック

#### ステップ3: Scratchへ接続（2分）

**オンライン版（推奨）**:
1. https://laughtale-scratch-ca803.web.app にアクセス
2. 左下の「拡張機能」ボタンをクリック
3. 「🎮 Minecraft」を選択

**ローカル版**:
```bash
cd scratch-gui
npm start
# http://localhost:8601 にアクセス
```

#### ステップ4: 動作確認（1分）

1. Minecraftでシングルプレイワールドを開く
2. Scratchで以下のブロックを配置:

```
🚩 緑の旗がクリックされたとき
🔌 Minecraftに接続
⏱️ 1秒待つ
💬 チャット: [Hello Minecraft!]
```

3. 緑の旗をクリック
4. Minecraftにメッセージが表示されれば成功！✅

---

## 🎯 基本的な使い方（学生・教師向け）

### 🧑‍🎓 学生向け - 基本操作

#### 1. 接続管理

```scratch
【基本パターン】
🚩 緑の旗がクリックされたとき
🔌 Minecraftに接続する
⏱️ 1秒待つ
もし <接続されている？> なら
  💬 チャット: [接続成功！]
終わり
```

#### 2. プレイヤー情報の取得

```scratch
【座標を取得】
🚩 緑の旗がクリックされたとき
📍 プレイヤーのX座標 を [x] にする
📍 プレイヤーのY座標 を [y] にする
📍 プレイヤーのZ座標 を [z] にする
💬 チャット: ((x) と ([,] と (y)) を [,] と (z))
```

#### 3. ブロックの設置

```scratch
【ダイヤブロックを置く】
🚩 緑の旗がクリックされたとき
🧱 [diamond_block]を X:[10] Y:[64] Z:[10] に置く
```

**使えるブロックの例**:
- `stone` (石)
- `dirt` (土)
- `grass_block` (草ブロック)
- `oak_planks` (オークの板)
- `diamond_block` (ダイヤモンドブロック)
- `gold_block` (金ブロック)
- `iron_block` (鉄ブロック)
- `glass` (ガラス)

#### 4. 建築プログラム

**例1: 円を作る**
```scratch
🚩 緑の旗がクリックされたとき
📍 プレイヤーのX座標 を [x] にする
📍 プレイヤーのZ座標 を [z] にする
🏗️ [stone]で円を X:[x] Y:[64] Z:[z] に半径[5]で建築
💬 チャット: [円を作ったよ！]
```

**例2: 家を建てる**
```scratch
🚩 緑の旗がクリックされたとき
📍 プレイヤーのX座標 を [x] にする
📍 プレイヤーのZ座標 を [z] にする
🏠 [simple]スタイルの家を X:[x] Y:[64] Z:[z] にサイズ[medium]で建築
💬 チャット: [家が完成しました！]
```

#### 5. テレポート機能

```scratch
【ホームシステム】
スペースキーが押されたとき
もし <接続されている？> なら
  📍 現在位置をホーム設定
  💬 チャット: [ホームを設定しました]
終わり

Hキーが押されたとき
もし <接続されている？> なら
  🏠 ホームに戻る
  💬 チャット: [ホームに戻りました]
終わり
```

### 👨‍🏫 教師向け - 管理機能

#### 1. 生徒の監視

```scratch
【生徒の位置を確認】
🚩 緑の旗がクリックされたとき
ずっと
  📊 生徒の活動状況を取得
  もし <異常な活動？> なら
    🚨 アラートを送信
  終わり
  ⏱️ 5秒待つ
終わり
```

#### 2. 一斉操作

```scratch
【全員を集合】
🚩 緑の旗がクリックされたとき
👥 全生徒にテレポート: X:[0] Y:[100] Z:[0]
💬 全体チャット: [全員集合してください]
```

#### 3. ブロック制限

```scratch
【教育モード設定】
🚩 緑の旗がクリックされたとき
🎒 ブロックパック: [beginner] に設定
📊 難易度レベル: [easy] に設定
💬 チャット: [初心者モードを有効にしました]
```

---

## 📦 Scratchブロック完全ガイド

### 🔌 接続・設定カテゴリ（3ブロック）

| ブロック | 説明 | 使用例 |
|---------|------|--------|
| 🔌 **Minecraftに接続** | WebSocket接続を確立 | プログラム開始時 |
| 📡 **接続中？** | 接続状態を確認（真/偽） | 条件分岐で使用 |
| 🔌 **Minecraftから切断** | 接続を終了 | プログラム終了時 |

### 🧱 ブロック操作カテゴリ（4ブロック）

| ブロック | 説明 | パラメータ |
|---------|------|----------|
| 🧱 **[BLOCK]を X:[X] Y:[Y] Z:[Z] に置く** | ブロック設置 | BLOCK: ブロック種類<br>X,Y,Z: 座標 |
| ⛏️ **X:[X] Y:[Y] Z:[Z] のブロックを壊す** | ブロック破壊 | X,Y,Z: 座標 |
| 🔍 **X:[X] Y:[Y] Z:[Z] のブロック** | ブロック種類取得 | X,Y,Z: 座標<br>戻り値: ブロックID |
| 🎨 **[BLOCK]で[X1][Y1][Z1]から[X2][Y2][Z2]を埋める** | 範囲塗りつぶし | BLOCK: ブロック種類<br>X1,Y1,Z1〜X2,Y2,Z2: 範囲 |

### 🏗️ 建築カテゴリ（3ブロック）

| ブロック | 説明 | パラメータ |
|---------|------|----------|
| 🏗️ **[BLOCK]で壁を[X][Y][Z]から高さ[HEIGHT]幅[WIDTH]で建築** | 壁作成 | BLOCK: ブロック種類<br>X,Y,Z: 開始位置<br>HEIGHT: 高さ<br>WIDTH: 幅 |
| 🏠 **[STYLE]スタイルの家を[X][Y][Z]にサイズ[SIZE]で建築** | 家作成 | STYLE: simple/modern/medieval/japanese/castle<br>SIZE: small/medium/large/huge |
| ⚪ **[BLOCK]で円を[X][Y][Z]に半径[RADIUS]で建築** | 円形構造 | BLOCK: ブロック種類<br>X,Y,Z: 中心座標<br>RADIUS: 半径 |

### ⚙️ コマンドカテゴリ（4ブロック）

| ブロック | 説明 | パラメータ |
|---------|------|----------|
| 🚀 **プレイヤーを[X][Y][Z]にテレポート** | 瞬間移動 | X,Y,Z: 目的地座標 |
| 🎮 **ゲームモードを[MODE]に変更** | モード変更 | MODE: survival/creative/adventure/spectator |
| 🎁 **[ITEM]を[AMOUNT]個付与** | アイテム付与 | ITEM: アイテムID<br>AMOUNT: 個数 |
| 💬 **チャットに[MESSAGE]を送信** | メッセージ送信 | MESSAGE: テキスト |

### 📍 情報表示カテゴリ（6ブロック）

| ブロック | 説明 | 戻り値 |
|---------|------|--------|
| 📍 **プレイヤーのX座標** | X座標取得 | 数値 |
| 📍 **プレイヤーのY座標** | Y座標取得 | 数値 |
| 📍 **プレイヤーのZ座標** | Z座標取得 | 数値 |
| ❤️ **プレイヤーの体力** | 体力取得 | 数値（0-20） |
| 🧭 **プレイヤーの向き** | 向き取得 | north/south/east/west |
| 🌳 **現在のバイオーム** | バイオーム取得 | バイオームID |

### 👥 コラボレーションカテゴリ（4ブロック）

| ブロック | 説明 | パラメータ |
|---------|------|----------|
| 📧 **[FRIEND_NAME]を招待** | 友達招待 | FRIEND_NAME: プレイヤー名 |
| 🚪 **[WORLD_NAME]のワールドを訪問** | ワールド訪問 | WORLD_NAME: ワールド名 |
| 🏠 **自分のワールドに帰る** | 通常帰宅 | なし |
| 🚨 **緊急帰宅** | 緊急帰宅（体力回復付き） | なし |

---

## 💻 開発者向けガイド

### 🔧 ビルド環境の構築

#### 必須ツール

```bash
# Java 17
java -version
# openjdk version "17.0.X" が表示されることを確認

# Node.js v16+
node --version
# v16.0.0 以上が表示されることを確認

# Gradle (自動インストール)
cd minecraft-mod
./gradlew --version
```

#### プロジェクトのビルド

```bash
# 1. リポジトリをクローン
git clone [REPO_URL]
cd minecraft-collaboration-system

# 2. Minecraft Modをビルド
cd minecraft-mod
./gradlew jarJar -x test -x checkstyleMain
# 成功: build/libs/minecraft-collaboration-mod-1.0.0-all.jar (635KB)

# 3. Scratch GUIをビルド
cd ../scratch-gui
npm install
npm run build
# 成功: build/ディレクトリに静的ファイル生成

# 4. 開発サーバー起動
npm start
# http://localhost:8601 で起動
```

### 🧪 テストの実行

```bash
# 単体テスト（推奨）
cd minecraft-mod
./gradlew test -x integrationTest

# 統合テスト（Docker必要）
./gradlew integrationTest

# コードカバレッジ
./gradlew jacocoTestReport
# build/reports/jacoco/test/html/index.html を開く

# 静的解析
./gradlew checkstyleMain pmdMain spotbugsMain
```

### 📝 新しいコマンドの追加

#### 1. Scratchブロックの定義

`scratch-gui/src/examples/extensions/minecraft-unified.js`
```javascript
{
    opcode: 'myNewCommand',
    blockType: BlockType.COMMAND,
    text: '新しいコマンド [ARG]',
    arguments: {
        ARG: {
            type: ArgumentType.STRING,
            defaultValue: 'value'
        }
    }
}
```

#### 2. コマンド実装

`minecraft-mod/src/main/java/edu/minecraft/collaboration/commands/BasicCommandHandler.java`
```java
@CommandHandler("myNewCommand")
public JsonObject handleMyNewCommand(JsonObject args, ServerPlayer player) {
    String argValue = args.get("ARG").getAsString();

    // コマンド処理
    // ...

    return ResponseHelper.success("myNewCommand", "Success");
}
```

#### 3. ルーティング追加

`minecraft-mod/src/main/java/edu/minecraft/collaboration/network/CollaborationMessageProcessor.java`
```java
case "myNewCommand":
    return basicCommandHandler.handleMyNewCommand(args, player);
```

### 🚀 CI/CDパイプライン

#### GitHub Actions設定

`.github/workflows/build.yml`
```yaml
name: Build and Test
on:
  push:
    branches: [ main, develop ]

jobs:
  build-minecraft-mod:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Gradle
        run: |
          cd minecraft-mod
          ./gradlew jarJar --no-daemon
```

### 📊 監視・メトリクス

```bash
# APM監視画面を開く
open simple-monitoring.html

# WebSocket通信をデバッグ
open websocket-test.html

# パフォーマンステスト
open test-websocket-performance.html
```

---

## 🔧 トラブルシューティング

### ❌ よくある問題と解決方法

#### 問題1: 接続できない

**症状**:
- 「Connection refused」エラー
- Scratchから接続失敗

**解決方法**:
1. Minecraftが起動していることを確認
2. シングルプレイワールドを開いているか確認
3. WebSocketサーバーが起動しているか確認:
   ```bash
   netstat -an | findstr 14711
   ```
4. ファイアウォール設定でポート14711を許可
5. アンチウイルスソフトを一時停止して再試行

#### 問題2: Java バージョンエラー

**症状**:
```
Unsupported class file major version 65
java.lang.UnsupportedClassVersionError
```

**解決方法**:
```bash
# 1. Javaバージョン確認
java -version
javac -version

# 2. Java 17に切り替え
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

# macOS/Linux
export JAVA_HOME=/path/to/java17
export PATH=$JAVA_HOME/bin:$PATH

# 3. 環境変数の永続化（Windows）
setx JAVA_HOME "C:\Program Files\Java\jdk-17"

# 4. Gradleキャッシュをクリア
cd minecraft-mod
./gradlew clean
rm -rf ~/.gradle/caches/

# 5. 再ビルド
./gradlew build
```

#### 問題3: ビルドエラー

**症状**:
- Gradle build が失敗
- 依存関係エラー

**解決方法**:
```bash
# 1. クリーンビルド
./gradlew clean

# 2. 依存関係を再取得
./gradlew --refresh-dependencies

# 3. Gradle Wrapper更新
./gradlew wrapper --gradle-version=7.6.4

# 4. キャッシュ削除
rm -rf ~/.gradle/caches/

# 5. 再ビルド
./gradlew build
```

#### 問題4: Modが読み込まれない

**症状**:
- Minecraftの「Mods」リストに表示されない
- 起動時にクラッシュ

**解決方法**:
1. Forgeバージョンが1.20.1か確認
2. JARファイルが`.minecraft/mods/`に配置されているか確認
3. JARファイル名を変更していないか確認
4. 他のModとの競合を確認（他のModを一時削除）
5. ログを確認:
   ```bash
   # Windows
   %appdata%\.minecraft\logs\latest.log

   # macOS
   ~/Library/Application Support/minecraft/logs/latest.log
   ```

#### 問題5: パフォーマンスが悪い

**症状**:
- 動作が重い
- FPSが低下
- 遅延が発生

**解決方法**:
1. Minecraftビデオ設定を下げる:
   - 描画距離: 8〜12チャンク
   - グラフィック: 高速
   - スムースライティング: オフ

2. メモリ割り当てを増やす:
   ```bash
   # Minecraft Launcher → 設定 → JVM引数
   -Xmx4G -Xms2G
   ```

3. 他のアプリケーションを閉じる

4. コマンド実行頻度を下げる:
   ```scratch
   ずっと
     【処理】
     ⏱️ 0.5秒待つ  # ← 待機時間を追加
   終わり
   ```

### 🐛 デバッグツール

#### 1. WebSocket接続テスト

```bash
# test-websocket.htmlを開く
open test-connection.html

# または Node.js でテスト
node test-websocket-server.js
```

#### 2. ログ確認

```bash
# Minecraft ログ
tail -f ~/.minecraft/logs/latest.log

# WebSocket通信ログ
# ブラウザのコンソール（F12）で確認
```

#### 3. パフォーマンス監視

```bash
# 監視画面を開く
open simple-monitoring.html

# メトリクス確認
curl http://localhost:14711/metrics
```

---

## ❓ よくある質問（FAQ）

### 一般的な質問

**Q1: 無料で使えますか？**
A: Mod自体は無料ですが、Minecraft Java Edition（有料）が必要です。

**Q2: 対応プラットフォームは？**
A: Windows 10/11, macOS 10.14+, Linux（Ubuntu/Debian）

**Q3: モバイル版で使えますか？**
A: いいえ、Java Edition専用です。Bedrock Edition（モバイル/Switch）は非対応です。

**Q4: オフラインで使えますか？**
A: シングルプレイであれば、インターネット接続不要で使用できます。

**Q5: 他のModと一緒に使えますか？**
A: 基本的に可能ですが、WebSocketポートを使用する他のModとは競合する可能性があります。

### 技術的な質問

**Q6: WebSocketポートを変更できますか？**
A: はい、`gradle.properties`で変更可能:
```properties
websocket.port=14711  # 任意のポート番号に変更
```

**Q7: 複数人で同時に使えますか？**
A: はい、マルチプレイサーバーで使用可能です。各プレイヤーが個別に接続します。

**Q8: 最大接続数は？**
A: デフォルトで10接続まで。設定で変更可能です。

**Q9: コマンドの実行速度制限は？**
A: 1秒間に10コマンドまで（レート制限）。

**Q10: カスタムコマンドを作れますか？**
A: はい、JavaとJavaScriptで独自のコマンドを実装できます。

### セキュリティに関する質問

**Q11: 安全ですか？**
A: はい、以下のセキュリティ機能があります:
- ローカルネットワーク限定
- レート制限
- 危険なコマンド/ブロックの制限
- 入力検証（XSS/SQLインジェクション防止）

**Q12: 個人情報は収集されますか？**
A: いいえ、一切収集しません。すべてローカルで動作します。

**Q13: チャット内容は記録されますか？**
A: いいえ、記録されません。

---

## 🎓 学習リソース

### 初心者向けチュートリアル

1. **基礎編** (30分)
   - 接続とメッセージ送信
   - 座標の取得と移動
   - 簡単なブロック設置

2. **建築編** (1時間)
   - 繰り返しで大きな構造物
   - 変数を使った座標管理
   - 関数化とモジュール設計

3. **協調編** (45分)
   - 友達招待システム
   - ワールド訪問
   - 共同建築プロジェクト

### サンプルプロジェクト

```scratch
【プロジェクト1: 自動階段建設】
🚩 緑の旗がクリックされたとき
🔌 Minecraftに接続
📍 プレイヤーのX座標 を [x] にする
📍 プレイヤーのY座標 を [y] にする
📍 プレイヤーのZ座標 を [z] にする
[10] 回繰り返す
  🧱 [stone]を X:(x) Y:(y) Z:(z) に置く
  (y) を (1) ずつ変える
  (x) を (1) ずつ変える
終わり
💬 チャット: [階段完成！]
```

```scratch
【プロジェクト2: タイマー付きゲーム】
🚩 緑の旗がクリックされたとき
[時間] を [60] にする
ずっと
  もし <(時間) > [0]> なら
    💬 チャット: ([残り] と (時間) と [秒])
    ⏱️ 1秒待つ
    (時間) を (-1) ずつ変える
  でなければ
    💬 チャット: [時間切れ！]
    🚨 緊急帰宅
    止める
  終わり
終わり
```

---

## 📞 サポート・コミュニティ

### お問い合わせ

- **GitHub Issues**: https://github.com/laughtale01/Scratch/issues
- **Discord**: [準備中]
- **Email**: [準備中]

### 貢献方法

プロジェクトに貢献したい方:
1. GitHubでFork
2. 機能追加/バグ修正
3. Pull Request作成

### ライセンス

MIT License - 教育目的での使用は自由です。

---

## 📚 関連ドキュメント

- [API リファレンス](API_REFERENCE.md) - WebSocket API仕様
- [アーキテクチャ設計](architecture.md) - システム設計詳細
- [開発者ガイド](development/DEVELOPER_GUIDE.md) - 開発者向け詳細ガイド
- [トラブルシューティング](troubleshooting.md) - 問題解決ガイド
- [セットアップガイド](setup/UNIFIED_SETUP_GUIDE.md) - 詳細セットアップ手順

---

**🎉 Happy Crafting with Scratch!**

このガイドで、Minecraft×Scratchシステムを最大限に活用できます。わからないことがあれば、コミュニティやサポートチャンネルで質問してください！
