# MinecraftEdu Scratch Controller

**Scratchでプログラミングを学びながら、Minecraftの世界を操作しよう！**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x-green.svg)](https://minecraft.net)
[![Scratch](https://img.shields.io/badge/Scratch-3.0-orange.svg)](https://scratch.mit.edu)

---

## プロジェクト概要

このプロジェクトは、[takecx](https://github.com/takecx)さんの優れた[Scratch-Minecraft連携プロジェクト](https://qiita.com/panda531/items/a6dfd87bd68ba2601793)をベースに、以下の独自機能を追加した教育向けシステムです：

### 主な特徴

🎮 **ビジュアルプログラミング**
- Scratch 3.0でMinecraft Java Editionを操作
- コーディング不要！ブロックをつなげるだけ
- プログラミング初心者に最適

👥 **マルチプレイヤー対応**（新機能）
- 最大10人が同時接続
- 教師・生徒の役割分担
- リアルタイム協働作業

📚 **教育コンテンツ**（新機能）
- ステップバイステップのチュートリアル
- 自動評価システム
- 進捗管理とバッジシステム
- 課題の作成と割り当て

🏆 **ゲーミフィケーション**
- ポイント・バッジ獲得
- 達成感のある学習体験
- モチベーション維持

---

## デモ

### チュートリアルの様子

```
┌─────────────────────────────────────────────────────┐
│  📚 チュートリアル: はじめてのプログラミング           │
├─────────────────────────────────────────────────────┤
│  ステップ 2/4: Minecraftに接続しよう                │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━    │
│                                                      │
│  「Minecraftに接続」ブロックを使って接続してね！     │
│                                                      │
│  ✅ 完了: ステップ1（5pt獲得）                       │
│  🔄 進行中: ステップ2                                │
│  ⏭️ 次: ステップ3                                    │
└─────────────────────────────────────────────────────┘
```

### 実際の操作例

```scratch
[緑の旗がクリックされたとき]
[Minecraftに接続 ▼] ホスト [localhost ▼] ポート [14711 ▼]
[チャットで言う ▼] [Hello, Minecraft!]
[ブロックを置く ▼] x [~0 ▼] y [~1 ▼] z [~2 ▼] ブロック [stone ▼]
```

→ Minecraftの画面に「Hello, Minecraft!」が表示され、プレイヤーの目の前に石ブロックが出現！

---

## クイックスタート

### 必要なもの

- Minecraft Java Edition 1.20.x
- Webブラウザ（Chrome、Firefox、Edge推奨）
- インターネット接続

### インストール（5分で完了！）

#### 1. Minecraft MODをダウンロード

[Releases](https://github.com/YOUR_USERNAME/minecraftedu-scratch/releases)から最新版の`minecraftedu-mod-x.x.x.jar`をダウンロード。

#### 2. MODをインストール

```
1. Minecraft Forgeをインストール（まだの場合）
   https://files.minecraftforge.net/
   → Minecraft 1.20.1 用のForgeをダウンロード

2. ダウンロードしたMODファイルを以下に配置：
   Windows: %APPDATA%\.minecraft\mods\
   macOS: ~/Library/Application Support/minecraft/mods/
   Linux: ~/.minecraft/mods/
```

#### 3. Minecraftを起動

```
Minecraftランチャーで「forge-1.20.x」プロファイルを選択して起動
```

#### 4. Scratchを開く

ブラウザで以下のURLにアクセス：

```
https://YOUR_USERNAME.github.io/scratch-gui/
```

または、ローカル開発版を使用：

```bash
cd scratch-client/scratch-gui
npm install
npm start
# → http://localhost:8601/ を開く
```

#### 5. 接続！

Scratchの「拡張機能を追加」→「Minecraft」を選択し、接続ブロックを実行！

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

#### 3. ブロックを置く

```scratch
[ブロックを置く] x [100] y [64] z [50] ブロック [diamond_block]
```

#### 4. エンティティを召喚

```scratch
[エンティティを召喚] 種類 [pig] x [100] y [64] z [50]
```

### チュートリアルを始める

Scratchの画面右上の「📚 チュートリアル」ボタンをクリックして、初心者向けガイドをスタート！

### 教師向け機能

#### 課題を作成

1. 「Teacher」役割でログイン
2. 「課題管理」パネルを開く
3. 「新規課題」ボタンをクリック
4. 課題の詳細を入力して保存

#### 生徒の進捗を確認

「進捗管理」パネルで、各生徒の学習状況をリアルタイムに確認できます。

---

## 対応ブロック

### ブロック操作

- ✅ ブロック配置（絶対座標・相対座標）
- ✅ ブロック取得
- ✅ 範囲ブロック配置（fill）
- ✅ ブロック検索

### エンティティ

- ✅ エンティティ召喚
- ✅ 近くのエンティティ取得

### プレイヤー操作

- ✅ テレポート
- ✅ 位置取得
- ✅ ゲームモード変更
- ✅ アイテム付与

### ワールド操作

- ✅ 天気変更
- ✅ 時刻変更
- ✅ パーティクル生成

### その他

- ✅ チャット送信
- ✅ コマンド実行

詳細は[コマンドリファレンス](docs/COMMAND_REFERENCE.md)を参照。

---

## プロジェクト構成

```
minecraft-laughtare-project/
│
├── docs/                          # ドキュメント
│   ├── PROJECT_DESIGN.md          # 全体設計書
│   ├── SETUP_GUIDE.md             # セットアップガイド
│   ├── MULTIPLAYER_DESIGN.md      # マルチプレイヤー設計
│   ├── EDUCATION_DESIGN.md        # 教育機能設計
│   └── COMMAND_REFERENCE.md       # コマンドリファレンス
│
├── scratch-client/                # Scratchクライアント
│   ├── scratch-gui/               # GUI (React)
│   └── scratch-vm/                # 仮想マシン
│
├── minecraft-mod/                 # Minecraft MOD
│   └── src/main/java/             # Javaソースコード
│
├── shared/                        # 共有リソース
│   └── protocol/                  # 通信プロトコル仕様
│
└── README.md                      # このファイル
```

---

## 開発者向け情報

### 開発環境のセットアップ

詳細は[SETUP_GUIDE.md](docs/SETUP_GUIDE.md)を参照してください。

#### 必要なツール

- Node.js 16.x以上
- Java JDK 17
- Git
- VSCode または IntelliJ IDEA

#### ビルド手順

```bash
# Scratch VM/GUI
cd scratch-client/scratch-vm
npm install && npm run build

cd ../scratch-gui
npm install && npm start

# Minecraft MOD
cd ../../minecraft-mod/RemoteControllerMod
./gradlew build
```

### 貢献方法

プルリクエスト大歓迎です！以下の手順でご協力ください：

1. このリポジトリをフォーク
2. 機能ブランチを作成（`git checkout -b feature/amazing-feature`）
3. 変更をコミット（`git commit -m 'Add amazing feature'`）
4. ブランチにプッシュ（`git push origin feature/amazing-feature`）
5. プルリクエストを作成

詳細は[CONTRIBUTING.md](CONTRIBUTING.md)を参照。

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

## サポート・コミュニティ

### 質問・バグ報告

- [Issue Tracker](https://github.com/YOUR_USERNAME/minecraftedu-scratch/issues)
- [Discussions](https://github.com/YOUR_USERNAME/minecraftedu-scratch/discussions)

### ドキュメント

- [プロジェクト設計書](docs/PROJECT_DESIGN.md)
- [開発環境セットアップ](docs/SETUP_GUIDE.md)
- [マルチプレイヤー機能](docs/MULTIPLAYER_DESIGN.md)
- [教育コンテンツ機能](docs/EDUCATION_DESIGN.md)
- [通信プロトコル仕様](shared/protocol/PROTOCOL_SPEC.md)

### リンク

- [Scratch公式サイト](https://scratch.mit.edu)
- [Minecraft Forge](https://docs.minecraftforge.net/)
- [オリジナルプロジェクト](https://github.com/takecx)

---

## よくある質問（FAQ）

### Q: どのMinecraftバージョンに対応していますか？

A: Minecraft Java Edition 1.20.x（1.20.1推奨）に対応しています。

### Q: Scratchのアカウントは必要ですか？

A: いいえ、アカウントなしでブラウザ上ですぐに使えます。

### Q: 統合版（Bedrock Edition）には対応していますか？

A: 現在はJava Editionのみ対応しています。統合版対応は将来的に検討中です。

### Q: オンラインマルチプレイで使えますか？

A: はい！サーバーのポート（14711）を開放すれば、離れた場所からでも接続できます。

### Q: 商用利用は可能ですか？

A: MIT Licenseの範囲内で可能です。詳細はLICENSEファイルをご確認ください。

---

## ロードマップ

### Version 1.0（2025年12月予定）

- [x] 基本的なScratch-Minecraft連携
- [x] マルチプレイヤー対応（最大10人）
- [x] チュートリアルシステム
- [x] 課題システム
- [x] 進捗管理・バッジシステム

### Version 1.1（2026年1月予定）

- [ ] より多くのチュートリアル追加（20本以上）
- [ ] 3Dモデルインポート機能
- [ ] AIアシスタント連携（ChatGPT等）
- [ ] モバイル対応

### Version 2.0（2026年3月予定）

- [ ] 統合版（Bedrock）対応
- [ ] VR/AR対応
- [ ] クラウド保存機能
- [ ] 教師用ダッシュボード強化

---

## 謝辞

このプロジェクトは多くの方々の協力により実現しました：

- **takecx**さん - オリジナルプロジェクトの作成者
- **Scratch Team** - 素晴らしいビジュアルプログラミング環境の提供
- **Minecraft Forge Team** - MOD開発環境の提供
- すべての貢献者とテスターの皆様

---

## スクリーンショット

### Scratchインターフェース

![Scratch Interface](docs/assets/screenshots/scratch_interface.png)

### チュートリアル画面

![Tutorial](docs/assets/screenshots/tutorial.png)

### 進捗ダッシュボード

![Progress Dashboard](docs/assets/screenshots/progress_dashboard.png)

---

**作成日**: 2025-11-12
**最終更新**: 2025-11-12
**バージョン**: 0.1.0 (開発中)

---

⭐ このプロジェクトが役に立ったら、ぜひスターをお願いします！

💬 質問・提案があれば、お気軽にIssueを開いてください。

🤝 貢献を歓迎します！一緒に素晴らしい教育ツールを作りましょう。
