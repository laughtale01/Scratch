# MinecraftEdu Scratch Controller

**Scratchでプログラミングを学びながら、Minecraftの世界を操作しよう！**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://minecraft.net)
[![Scratch](https://img.shields.io/badge/Scratch-3.0-orange.svg)](https://scratch.mit.edu)
[![Live Demo](https://img.shields.io/badge/Live%20Demo-GitHub%20Pages-brightgreen)](https://laughtale01.github.io/Scratch/)

---

## 🎮 今すぐ試す！

### **ステップ1: MODをダウンロード**

[📥 minecraftedu-mod-0.1.0-1.20.1.jar をダウンロード](https://github.com/laughtale01/Scratch/blob/main/release/minecraft-scratch-controller/minecraftedu-mod-0.1.0-1.20.1.jar)

「Download」ボタンをクリックしてMODファイルを入手

### **ステップ2: MODをインストール**

1. [Minecraft Forge 1.20.1](https://files.minecraftforge.net/)をインストール
2. ダウンロードしたMODファイルを以下のフォルダにコピー：
   - **Windows**: `%APPDATA%\.minecraft\mods\`
   - **macOS**: `~/Library/Application Support/minecraft/mods/`
   - **Linux**: `~/.minecraft/mods/`

### **ステップ3: Scratch GUIにアクセス**

[🌐 https://laughtale01.github.io/Scratch/](https://laughtale01.github.io/Scratch/)

ブラウザで開くだけ！インストール不要！

### **ステップ4: 接続して遊ぶ**

1. Minecraftを起動してワールドを開く
2. Scratchで「拡張機能を追加」→「Minecraft」を選択
3. 「Minecraftに接続」ブロックを実行
4. 接続成功！🎉

詳しいインストール手順は [INSTALL.md](release/minecraft-scratch-controller/INSTALL.md) を参照してください。

---

## プロジェクト概要

このプロジェクトは、[takecx](https://github.com/takecx)さんの優れた[Scratch-Minecraft連携プロジェクト](https://qiita.com/panda531/items/a6dfd87bd68ba2601793)を参考にしながら、完全に独自で構築した教育向けシステムです。

### 主な特徴

🎮 **ビジュアルプログラミング**
- Scratch 3.0でMinecraft Java Editionを操作
- コーディング不要！ブロックをつなげるだけ
- プログラミング初心者に最適

🌐 **どこからでもアクセス可能**
- GitHub Pagesで公開されたScratch GUI
- インストール不要、ブラウザだけでOK
- 常に最新版が使用可能

🎨 **日本語対応 & Minecraftテーマ**
- 全てのブロック名・エンティティ名が日本語
- Minecraftの緑色をテーマにした統一デザイン
- 直感的で分かりやすいインターフェース

⚡ **超強力な範囲設置機能**
- 一度に最大**200,000ブロック**を設置可能
- 座標範囲を指定して効率的にビルド
- 大規模な建築もすぐに完成

🔧 **Y座標自動変換機能**
- Y座標が負の値の場合、自動的に正の値に変換
- 座標計算ミスを気にせず安心してプログラミング

🔌 **リアルタイム通信**
- WebSocketによる高速通信
- Scratchとの即座の連携
- 安定した接続

---

## デモ

### 実際の操作例

```scratch
[緑の旗がクリックされたとき]
[Minecraftに接続 ▼] ホスト [localhost ▼] ポート [14711 ▼]
[チャットで言う ▼] [Hello, Minecraft!]
[ブロックを置く ▼] x [~0 ▼] y [~1 ▼] z [~2 ▼] ブロック [石 ▼]
[ブロックを範囲設置 ▼] x1 [0 ▼] y1 [64 ▼] z1 [0 ▼] x2 [10 ▼] y2 [64 ▼] z2 [10 ▼] ブロック [草ブロック ▼]
```

→ Minecraftの画面に「Hello, Minecraft!」が表示され、プレイヤーの目の前に石ブロックが出現！さらに、11×1×11の草ブロックの床が一瞬で完成！

---

## 使い方

### 基本操作

#### 1. Minecraftに接続

```scratch
[Minecraftに接続] ホスト [localhost] ポート [14711]
```

#### 2. チャットを送信

```scratch
[チャットで言う] [こんにちは、Minecraft！]
```

#### 3. ブロックを置く（絶対座標）

```scratch
[ブロックを置く] x [100] y [64] z [50] ブロック [ダイヤモンドブロック]
```

#### 4. ブロックを置く（相対座標）

```scratch
[ブロックを置く] ~[0] ~[1] ~[2] ブロック [石]
```
プレイヤーの現在位置を基準とした相対的な位置に配置できます。

#### 5. 範囲でブロックを設置（超強力！）

```scratch
[ブロックを範囲設置] x1 [0] y1 [64] z1 [0] x2 [100] y2 [100] z2 [100] ブロック [ガラス]
```
指定した範囲内に一括でブロックを設置できます（**最大200,000ブロック**）。

#### 6. エンティティを召喚

```scratch
[エンティティを召喚] [ブタ] x [100] y [64] z [50]
```
利用可能なエンティティ：ブタ、ウシ、ヒツジ、ニワトリ、ゾンビ、スケルトン、クリーパー、クモ、ウマ、オオカミ、ネコ、村人

#### 7. プレイヤーをテレポート

```scratch
[テレポート] x [0] y [100] z [0]
```

#### 8. プレイヤーの位置を取得

```scratch
[プレイヤーの位置] [x]
```
x、y、zの座標を個別に取得できます。

#### 9. 天気を変更

```scratch
[天気を] [clear] [にする]
```
天気の種類：clear（晴れ）、rain（雨）、thunder（雷雨）

#### 10. 時刻を変更

```scratch
[時刻を] [朝] [にする]
```
時刻の種類：朝、昼、夕方、夜、真夜中

---

## 対応ブロック一覧

### 接続・通信

| ブロック名 | 説明 |
|-----------|------|
| Minecraftに接続 | WebSocketでMinecraftサーバーに接続 |
| 切断 | Minecraftサーバーから切断 |
| 接続中？ | 接続状態を確認（真偽値） |

### チャット

| ブロック名 | 説明 |
|-----------|------|
| チャットで言う | Minecraftのチャットにメッセージを送信 |

### ブロック操作

| ブロック名 | 説明 |
|-----------|------|
| ブロックを置く (絶対座標) | 指定した座標にブロックを配置 |
| ブロックを置く (相対座標) | プレイヤーの位置からの相対座標にブロックを配置 |
| ブロックを範囲設置 | 指定範囲内にブロックを一括配置（最大200,000個） |

**対応ブロックタイプ（18種類）:**
石、土、草ブロック、丸石、オークの板材、ガラス、砂、砂利、金ブロック、ダイヤモンドブロック、エメラルドブロック、鉄ブロック、石炭ブロック、レッドストーンブロック、レンガ、オークの原木、水、溶岩

### エンティティ操作

| ブロック名 | 説明 |
|-----------|------|
| エンティティを召喚 | 指定した座標にエンティティを召喚 |

**対応エンティティ（12種類）:**
ブタ、ウシ、ヒツジ、ニワトリ、ゾンビ、スケルトン、クリーパー、クモ、ウマ、オオカミ、ネコ、村人

### プレイヤー操作

| ブロック名 | 説明 |
|-----------|------|
| テレポート | プレイヤーを指定座標にテレポート |
| プレイヤーの位置 | プレイヤーの現在位置（x/y/z）を取得 |

### ワールド操作

| ブロック名 | 説明 |
|-----------|------|
| 天気を〜にする | 天気を変更（晴れ・雨・雷雨） |
| 時刻を〜にする | 時刻を変更（朝・昼・夕方・夜・真夜中） |

---

## 配布・インストール

### ユーザー向け

#### MODのダウンロード

[📥 minecraftedu-mod-0.1.0-1.20.1.jar](https://github.com/laughtale01/Scratch/blob/main/release/minecraft-scratch-controller/minecraftedu-mod-0.1.0-1.20.1.jar)

GitHubから直接ダウンロードできます（ページ内の「Download」ボタンをクリック）

#### Scratch GUI

[🌐 https://laughtale01.github.io/Scratch/](https://laughtale01.github.io/Scratch/)

ブラウザでアクセスするだけ！インストール不要！

#### インストール手順

詳細な手順は以下を参照：
- [INSTALL.md](release/minecraft-scratch-controller/INSTALL.md) - インストールガイド
- [RELEASE_NOTES.md](release/minecraft-scratch-controller/RELEASE_NOTES.md) - リリースノート
- [GITHUB_PAGES_SETUP.md](GITHUB_PAGES_SETUP.md) - GitHub Pages設定（管理者向け）

---

## プロジェクト構成

```
minecraft-laughtare-project/
│
├── docs/                                   # ドキュメント
│   └── github-setup.md                     # GitHub設定ガイド
│
├── scratch-client/                         # Scratchクライアント
│   ├── scratch-vm-official/                # Scratch VM (カスタム版)
│   │   ├── src/extensions/scratch3_minecraft/  # Minecraft拡張機能
│   │   └── build/                          # ビルド成果物
│   │
│   └── scratch-gui-official/               # Scratch GUI (カスタム版)
│       ├── src/lib/libraries/extensions/   # 拡張機能ライブラリ
│       └── build/                          # 本番用ビルド → GitHub Pages
│
├── minecraft-mod/                          # Minecraft MOD
│   ├── src/main/java/                      # Javaソースコード
│   │   └── com/github/minecraftedu/
│   │       ├── MinecraftEduMod.java
│   │       ├── WebSocketServer.java
│   │       └── commands/
│   └── build/libs/                         # ビルド成果物
│       └── minecraftedu-mod-0.1.0-1.20.1.jar
│
├── release/                                # リリースパッケージ
│   ├── minecraft-scratch-controller/       # 配布用ファイル
│   │   ├── minecraftedu-mod-0.1.0-1.20.1.jar
│   │   ├── INSTALL.md
│   │   ├── README.md
│   │   └── RELEASE_NOTES.md
│   └── DEPLOYMENT_GUIDE.md                 # デプロイガイド
│
├── GITHUB_PAGES_SETUP.md                   # GitHub Pages設定手順
└── README.md                               # このファイル
```

---

## 開発者向け情報

### 技術スタック

**Scratch側:**
- Scratch VM 5.0.x (Node.js)
- Scratch GUI 5.2.14 (React)
- WebSocket Client (Browser API)
- Webpack 5

**Minecraft側:**
- Minecraft 1.20.1
- Minecraft Forge 1.20.1
- Java 17
- Gson (JSON処理)
- Java-WebSocket (WebSocketサーバー)

### 開発環境のセットアップ

#### 必要なツール

- Node.js 16.x以上
- Java JDK 17
- Git
- VSCode または IntelliJ IDEA

#### Scratch VM/GUIのビルド

```bash
# Scratch VM
cd scratch-client/scratch-vm-official
npm install
npm run build
npm link

# Scratch GUI
cd ../scratch-gui-official
npm install
npm link scratch-vm

# 開発サーバー起動
npm start
# → http://localhost:8601/ で開発サーバーが起動

# 本番用ビルド
npm run build
# → build/ ディレクトリに成果物が生成
```

#### Minecraft MODのビルド

```bash
cd minecraft-mod

# Windowsの場合
.\gradlew.bat build

# Mac/Linuxの場合
./gradlew build

# ビルド成果物: build/libs/minecraftedu-mod-0.1.0-1.20.1.jar
```

### 通信プロトコル

Scratch拡張機能とMinecraft MODはWebSocket (JSON形式) で通信します。

**接続URL:** `ws://localhost:14711/minecraft`

**メッセージ形式:**

```json
{
  "version": "1.0",
  "messageId": "uuid",
  "timestamp": 1234567890,
  "sessionId": "session-id",
  "type": "command|query|event",
  "payload": {
    "action": "setBlock|fillBlocks|teleport|chat|...",
    "params": { /* アクション固有のパラメータ */ }
  }
}
```

詳細な仕様は `minecraft-mod/src/main/java/com/github/minecraftedu/` のソースコードを参照してください。

### GitHub Pagesへのデプロイ

Scratch GUIの更新をGitHub Pagesに反映する方法：

```bash
# 1. Scratch GUIをビルド
cd scratch-client/scratch-gui-official
npm run build

# 2. gh-pagesブランチに切り替え
git checkout gh-pages

# 3. ビルド成果物をコピー
cp -r build/* .

# 4. コミット＆プッシュ
git add -A
git commit -m "Update Scratch GUI"
git push origin gh-pages

# 5. mainブランチに戻る
git checkout main
```

数分後、自動的にGitHub Pagesが更新されます。

詳細は [GITHUB_PAGES_SETUP.md](GITHUB_PAGES_SETUP.md) を参照してください。

### 貢献方法

プルリクエスト大歓迎です！以下の手順でご協力ください：

1. このリポジトリをフォーク
2. 機能ブランチを作成（`git checkout -b feature/amazing-feature`）
3. 変更をコミット（`git commit -m 'feat: add amazing feature'`）
4. ブランチにプッシュ（`git push origin feature/amazing-feature`）
5. プルリクエストを作成

---

## ライセンス

このプロジェクトは[MIT License](LICENSE)の下で公開されています。

### 参考プロジェクトのクレジット

このプロジェクトは、以下のプロジェクトを参考にしながら、完全に独自で実装されています：

- [takecx/RemoteControllerMod](https://github.com/takecx/RemoteControllerMod) - WebSocket通信のアイデア
- [takecx/scratch-vm](https://github.com/takecx/scratch-vm) - Scratch拡張機能の実装パターン
- [takecx/scratch-gui](https://github.com/takecx/scratch-gui) - GUI統合の参考

参考プロジェクト作者: [takecx](https://github.com/takecx)

参考記事: [ScratchからJava版Minecraftを操作する拡張機能 by panda531](https://qiita.com/panda531/items/a6dfd87bd68ba2601793)

**注:** 本プロジェクトは上記プロジェクトのフォークではなく、完全に独自に実装されています。

---

## よくある質問（FAQ）

### Q: どのMinecraftバージョンに対応していますか？

A: Minecraft Java Edition 1.20.1 に対応しています。

### Q: Scratchのアカウントは必要ですか？

A: いいえ、アカウントなしでブラウザ上ですぐに使えます。

### Q: 統合版（Bedrock Edition）には対応していますか？

A: 現在はJava Editionのみ対応しています。

### Q: オンラインマルチプレイで使えますか？

A: サーバーのポート（14711）を適切に設定すれば可能ですが、セキュリティに注意してください。

### Q: 範囲設置ブロックの制限（200,000ブロック）を変更できますか？

A: はい、`CommandExecutor.java` の制限値を変更して再ビルドできます。ただし、大量のブロック設置はゲームのパフォーマンスに影響する可能性があります。

### Q: Scratch GUIをローカルで動かすことはできますか？

A: はい、開発者向け手順に従って `npm start` を実行すれば、ローカルサーバー（http://localhost:8601/）で動作します。

---

## 変更履歴

### Version 1.0.0 (2025-11-14) 🎉

- 🎉 **本番リリース！**
- 🌐 GitHub Pagesで公開（https://laughtale01.github.io/Scratch/）
- ⚡ ブロック設置上限を**200,000ブロック**に拡大
- 🔧 Y座標自動変換機能を実装（負の値を自動的に正の値に変換）
- 📦 配布パッケージとドキュメントを整備
- ✅ 本番環境での動作確認完了

### Version 0.2.0 (2025-11-13)

- ✨ ブロックの色を緑系に変更（Minecraftテーマに合わせて）
- ✨ ブロック名を日本語化（18種類）
- ✨ エンティティ名を日本語化（12種類）
- ✨ 範囲設置ブロックを新規追加
- 🐛 WebSocket通信の安定性を改善

### Version 0.1.0 (2025-11-12)

- 🎉 初回リリース
- ✅ 基本的なScratch-Minecraft連携
- ✅ WebSocketによるリアルタイム通信
- ✅ ブロック配置、エンティティ召喚、天気・時刻変更

---

## 今後の開発予定

### 近日中に追加予定

- [ ] より多くのブロックタイプとエンティティの追加
- [ ] ブロックの取得・検索機能
- [ ] インベントリ操作
- [ ] ゲームモード変更
- [ ] パーティクル生成

### 将来的に検討中

- [ ] 教育向けチュートリアルシステム
- [ ] マルチプレイヤー対応の強化
- [ ] 進捗管理システム
- [ ] Scratch作品の共有機能

---

## トラブルシューティング

### 接続できない場合

1. Minecraftでワールドを開いていることを確認
2. コンソールに "WebSocket server started on port 14711" が表示されていることを確認
3. ファイアウォールでポート14711が許可されていることを確認
4. localhostではなくIPアドレス（127.0.0.1）で接続してみる

### ブロックが表示されない場合

1. ブラウザのキャッシュをクリア（Ctrl+Shift+Delete）
2. ページをリロード（Ctrl+R）
3. 開発者ツール（F12）でエラーメッセージを確認
4. 拡張機能が正しくロードされているか確認

### MODが読み込まれない場合

1. Minecraft Forgeが正しくインストールされているか確認
2. MODファイルが `mods` フォルダに配置されているか確認
3. Minecraftのバージョンが1.20.1であることを確認
4. ログファイル（`.minecraft/logs/latest.log`）でエラーを確認

### GitHub Pagesにアクセスできない場合

1. GitHubリポジトリの設定でPagesが有効になっているか確認
2. `gh-pages` ブランチが存在し、最新のファイルがプッシュされているか確認
3. デプロイに数分かかることがあるので、少し待ってから再度アクセス
4. ブラウザのキャッシュをクリアして再試行

---

## 謝辞

このプロジェクトは多くの方々の協力により実現しました：

- **takecx**さん - 参考にさせていただいたプロジェクトの作成者
- **Scratch Team** - 素晴らしいビジュアルプログラミング環境の提供
- **Minecraft Forge Team** - MOD開発環境の提供
- すべてのオープンソースコミュニティの皆様

---

**作成日**: 2025-11-12
**最終更新**: 2025-11-14
**バージョン**: 1.0.0

---

⭐ このプロジェクトが役に立ったら、ぜひスターをお願いします！

💬 質問・提案があれば、お気軽にIssueを開いてください。

🤝 貢献を歓迎します！一緒に素晴らしい教育ツールを作りましょう。
