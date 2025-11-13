# MinecraftEdu Scratch Controller

**Scratchでプログラミングを学びながら、Minecraftの世界を操作しよう！**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://minecraft.net)
[![Scratch](https://img.shields.io/badge/Scratch-3.0-orange.svg)](https://scratch.mit.edu)

---

## プロジェクト概要

このプロジェクトは、[takecx](https://github.com/takecx)さんの優れた[Scratch-Minecraft連携プロジェクト](https://qiita.com/panda531/items/a6dfd87bd68ba2601793)をベースに、独自の改良を加えた教育向けシステムです。

### 主な特徴

🎮 **ビジュアルプログラミング**
- Scratch 3.0でMinecraft Java Editionを操作
- コーディング不要！ブロックをつなげるだけ
- プログラミング初心者に最適

🎨 **日本語対応 & Minecraftテーマ**
- 全てのブロック名・エンティティ名が日本語
- Minecraftの緑色をテーマにした統一デザイン
- 直感的で分かりやすいインターフェース

⚡ **強力な範囲設置機能**
- 一度に最大1000ブロックを設置可能
- 座標範囲を指定して効率的にビルド
- 大規模な建築もすぐに完成

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
[ブロックを範囲設置 ▼] x [0~5 ▼] y [64~64 ▼] z [0~5 ▼] ブロック [草ブロック ▼]
```

→ Minecraftの画面に「Hello, Minecraft!」が表示され、プレイヤーの目の前に石ブロックが出現！さらに、6×1×6の草ブロックの床が一瞬で完成！

---

## クイックスタート

### 必要なもの

- Minecraft Java Edition 1.20.1
- Minecraft Forge 1.20.1
- Webブラウザ（Chrome、Firefox、Edge推奨）
- Node.js 16.x以上（開発環境の場合）

### インストール（5分で完了！）

#### 1. Minecraft Forgeをインストール

1. [Minecraft Forge公式サイト](https://files.minecraftforge.net/)から1.20.1用のForgeをダウンロード
2. ダウンロードしたインストーラーを実行
3. "Install client"を選択してインストール

#### 2. Minecraft MODをインストール

```
1. ビルド済みMODファイルを以下に配置：
   Windows: %APPDATA%\.minecraft\mods\
   macOS: ~/Library/Application Support/minecraft/mods/
   Linux: ~/.minecraft/mods/

   または、自分でビルド：
   cd minecraft-mod/RemoteControllerMod
   ./gradlew build
   # → build/libs/にJARファイルが生成されます
```

#### 3. Minecraftを起動

```
Minecraftランチャーで「forge-1.20.1」プロファイルを選択して起動
→ ワールドを開くと、コンソールに"WebSocket server started on port 14711"と表示されます
```

#### 4. Scratchクライアントをセットアップ

```bash
# Scratch VMをビルド
cd scratch-client/scratch-vm-official
npm install
npm run build
npm link

# Scratch GUIをセットアップ
cd ../scratch-gui-official
npm install
npm link scratch-vm
npm start

# → ブラウザで http://localhost:8601/ が自動的に開きます
```

#### 5. 接続！

1. Scratchの「拡張機能を追加」ボタンをクリック
2. 「Minecraft」拡張機能を選択（緑色のアイコン）
3. 「Minecraftに接続」ブロックを実行
4. 接続成功！🎉

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

#### 5. 範囲でブロックを設置（新機能！）

```scratch
[ブロックを範囲設置] x [0~10] y [64~66] z [0~10] ブロック [ガラス]
```
指定した範囲内に一括でブロックを設置できます（最大1000ブロック）。

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
| ブロックを範囲設置 | 指定範囲内にブロックを一括配置（最大1000個） |

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
│   │   └── dist/                           # ビルド成果物
│   │
│   └── scratch-gui-official/               # Scratch GUI (カスタム版)
│       ├── src/lib/libraries/extensions/   # 拡張機能ライブラリ
│       └── dist/                           # ビルド成果物
│
├── minecraft-mod/                          # Minecraft MOD
│   └── RemoteControllerMod/                # MODプロジェクト
│       ├── src/main/java/                  # Javaソースコード
│       │   └── net/sevenparallel/minecraft/
│       │       ├── RemoteControllerMod.java
│       │       ├── WebSocketServer.java
│       │       └── CommandHandler.java
│       └── build/libs/                     # ビルド成果物
│
└── README.md                               # このファイル
```

---

## 開発者向け情報

### 技術スタック

**Scratch側:**
- Scratch VM 5.0.x (Node.js)
- Scratch GUI 5.2.x (React)
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
npm start
# → http://localhost:8601/ で開発サーバーが起動
```

#### Minecraft MODのビルド

```bash
cd minecraft-mod/RemoteControllerMod

# Windowsの場合
.\gradlew.bat build

# Mac/Linuxの場合
./gradlew build

# ビルド成果物: build/libs/RemoteControllerMod-1.0.jar
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
    "action": "setBlock|teleport|chat|...",
    "params": { /* アクション固有のパラメータ */ }
  }
}
```

詳細な仕様は `minecraft-mod/RemoteControllerMod/src/main/java/net/sevenparallel/minecraft/` のソースコードを参照してください。

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

### オリジナルプロジェクトのクレジット

このプロジェクトは以下のプロジェクトをベースにしています：

- [takecx/RemoteControllerMod](https://github.com/takecx/RemoteControllerMod)
- [takecx/scratch-vm](https://github.com/takecx/scratch-vm)
- [takecx/scratch-gui](https://github.com/takecx/scratch-gui)

オリジナル作者: [takecx](https://github.com/takecx)

参考記事: [ScratchからJava版Minecraftを操作する拡張機能 by panda531](https://qiita.com/panda531/items/a6dfd87bd68ba2601793)

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

### Q: 範囲設置ブロックの制限（1000ブロック）を変更できますか？

A: はい、`scratch-vm-official/src/extensions/scratch3_minecraft/index.js` の `setBlockRange` メソッド内の制限値を変更できます。

---

## 変更履歴

### Version 0.2.0 (2025-11-13)

- ✨ ブロックの色を緑系に変更（Minecraftテーマに合わせて）
- ✨ ブロック名を日本語化（18種類）
- ✨ エンティティ名を日本語化（12種類）
- ✨ 範囲設置ブロックを新規追加（最大1000ブロック対応）
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
- [ ] マルチプレイヤー対応
- [ ] 進捗管理システム
- [ ] Scratch作品の共有機能

---

## トラブルシューティング

### 接続できない場合

1. Minecraftでワールドを開いていることを確認
2. コンソールに "WebSocket server started on port 14711" が表示されていることを確認
3. ファイアウォールでポート14711が許可されていることを確認
4. localhostではなくIPアドレスで接続してみる

### ブロックが表示されない場合

1. ブラウザのキャッシュをクリア（Ctrl+Shift+Delete）
2. ページをリロード（Ctrl+R）
3. 開発者ツール（F12）でエラーメッセージを確認

### MODが読み込まれない場合

1. Minecraft Forgeが正しくインストールされているか確認
2. MODファイルが `mods` フォルダに配置されているか確認
3. Minecraftのバージョンが1.20.1であることを確認
4. ログファイル（`.minecraft/logs/latest.log`）でエラーを確認

---

## 謝辞

このプロジェクトは多くの方々の協力により実現しました：

- **takecx**さん - オリジナルプロジェクトの作成者
- **Scratch Team** - 素晴らしいビジュアルプログラミング環境の提供
- **Minecraft Forge Team** - MOD開発環境の提供
- すべてのオープンソースコミュニティの皆様

---

**作成日**: 2025-11-12
**最終更新**: 2025-11-13
**バージョン**: 0.2.0

---

⭐ このプロジェクトが役に立ったら、ぜひスターをお願いします！

💬 質問・提案があれば、お気軽にIssueを開いてください。

🤝 貢献を歓迎します！一緒に素晴らしい教育ツールを作りましょう。
