# プロジェクト開始ガイド

## はじめに

おめでとうございます！MinecraftEdu Scratch Controllerプロジェクトの計画フェーズが完了しました。

このドキュメントでは、プロジェクトの全体像と、これから開発を始めるための手順を説明します。

---

## プロジェクト概要

### 目標

**Scratchでプログラミングを学びながら、Minecraftの世界を操作できる教育プラットフォーム**

### 独自機能

1. **マルチプレイヤー対応**: 最大10人が同時接続して協働作業
2. **教育コンテンツ**: チュートリアル、課題、進捗管理、バッジシステム

### 技術スタック

- **フロントエンド**: React + Scratch VM/GUI（JavaScript/TypeScript）
- **バックエンド**: Minecraft Forge MOD（Java 17）
- **通信**: WebSocket + HTTP/REST
- **対象**: Minecraft 1.20.x

---

## 作成済みドキュメント

プロジェクト計画として、以下のドキュメントを作成しました：

### 📋 メインドキュメント

| ファイル | 説明 | 重要度 |
|---------|------|-------|
| `README.md` | プロジェクト概要・クイックスタート | ⭐⭐⭐ |
| `PROJECT_DESIGN.md` | 包括的な設計書（全体アーキテクチャ） | ⭐⭐⭐ |
| `GETTING_STARTED.md` | このファイル | ⭐⭐⭐ |

### 📚 技術ドキュメント

| ファイル | 説明 | 重要度 |
|---------|------|-------|
| `docs/SETUP_GUIDE.md` | 開発環境セットアップ手順 | ⭐⭐⭐ |
| `docs/MULTIPLAYER_DESIGN.md` | マルチプレイヤー機能の詳細設計 | ⭐⭐ |
| `docs/EDUCATION_DESIGN.md` | 教育コンテンツ機能の詳細設計 | ⭐⭐ |
| `shared/protocol/PROTOCOL_SPEC.md` | 通信プロトコル仕様 | ⭐⭐ |

### 📁 ディレクトリ構造

プロジェクトのディレクトリ構造は以下の通り：

```
minecraft-laughtare-project/
├── docs/                          # ドキュメント
│   ├── SETUP_GUIDE.md             # ✅ 作成済み
│   ├── MULTIPLAYER_DESIGN.md      # ✅ 作成済み
│   └── EDUCATION_DESIGN.md        # ✅ 作成済み
├── scratch-client/                # Scratchクライアント
│   ├── scratch-gui/               # ⏳ forkが必要
│   └── scratch-vm/                # ⏳ forkが必要
├── minecraft-mod/                 # Minecraft MOD
│   ├── src/main/java/             # ⏳ forkが必要
│   └── build.gradle               # ⏳ 設定が必要
├── shared/
│   └── protocol/
│       └── PROTOCOL_SPEC.md       # ✅ 作成済み
├── PROJECT_DESIGN.md              # ✅ 作成済み
├── README.md                      # ✅ 作成済み
└── GETTING_STARTED.md             # ✅ このファイル
```

---

## 次のステップ

プロジェクトを進めるには、以下の手順に従ってください：

### フェーズ1: 開発環境セットアップ（1-2日）

#### 1.1 必要なソフトウェアのインストール

詳細は`docs/SETUP_GUIDE.md`を参照してください。

```bash
# Node.js（18.x LTS推奨）
nvm install 18
nvm use 18

# Java JDK 17
# https://adoptium.net/ からダウンロード

# Git
# https://git-scm.com/ からダウンロード
```

#### 1.2 GitHubアカウントの準備

1. GitHubアカウントを作成（未登録の場合）
2. Git設定

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

---

### フェーズ2: リポジトリのfork（30分）

#### 2.1 takecxさんのリポジトリをfork

ブラウザで以下にアクセスし、「Fork」ボタンをクリック：

1. https://github.com/takecx/scratch-vm
2. https://github.com/takecx/scratch-gui
3. https://github.com/takecx/RemoteControllerMod

#### 2.2 ローカルにclone

```bash
# プロジェクトディレクトリに移動
cd "D:\minecraft laughtare project"

# Scratch VM
cd scratch-client
git clone https://github.com/YOUR_USERNAME/scratch-vm.git
cd scratch-vm
git checkout develop
git remote add upstream https://github.com/takecx/scratch-vm.git

# Scratch GUI
cd ..
git clone https://github.com/YOUR_USERNAME/scratch-gui.git
cd scratch-gui
git remote add upstream https://github.com/takecx/scratch-gui.git

# RemoteControllerMod
cd ../../minecraft-mod
git clone https://github.com/YOUR_USERNAME/RemoteControllerMod.git
cd RemoteControllerMod
git remote add upstream https://github.com/takecx/RemoteControllerMod.git
```

**`YOUR_USERNAME`を自分のGitHubユーザー名に置き換えてください**

---

### フェーズ3: ビルド確認（1-2時間）

#### 3.1 Scratch VM/GUI

```bash
# Scratch VM
cd scratch-client/scratch-vm
npm install
npm run build
npm start  # → http://localhost:8073/playground/

# 別ターミナルで Scratch GUI
cd scratch-client/scratch-gui
npm install
npm link ../scratch-vm
npm start  # → http://localhost:8601/
```

**期待される結果**:
- ブラウザでScratchが表示される
- 「拡張機能を追加」→「Minecraft」が選択できる

#### 3.2 Minecraft MOD

```bash
cd minecraft-mod/RemoteControllerMod

# Windows
gradlew.bat build

# macOS/Linux
./gradlew build
```

**期待される結果**:
```
BUILD SUCCESSFUL in 2m 15s
```

---

### フェーズ4: 動作確認（30分）

#### 4.1 Minecraft Forgeのインストール

1. https://files.minecraftforge.net/ にアクセス
2. Minecraft 1.20.1用のForgeをダウンロード
3. インストーラーを実行（「Install client」を選択）

#### 4.2 MODのインストール

```bash
# ビルドしたMODをコピー
copy build\libs\remotecontrollermod-*.jar %APPDATA%\.minecraft\mods\
```

#### 4.3 Minecraftを起動

1. Minecraftランチャーで「forge-1.20.x」プロファイルを選択
2. 「プレイ」をクリック
3. 「Mods」ボタンで「Remote Controller」が表示されることを確認

#### 4.4 エンドツーエンドテスト

1. Minecraftでシングルプレイヤーワールドを作成
2. Scratchで接続ブロックを実行
3. チャットブロックで「Hello!」を送信
4. Minecraftのチャットに表示されればOK！

---

### フェーズ5: 開発開始（本格的な実装）

#### 5.1 ブランチ戦略

```bash
# 開発用ブランチを作成
git checkout -b develop

# 機能ごとにブランチを切る
git checkout -b feature/multiplayer-connection-manager
```

#### 5.2 実装ロードマップ

`PROJECT_DESIGN.md`の「実装ロードマップ」セクションを参照してください。

推奨順序：

1. **Week 1-2**: 基本機能の確認とMinecraft 1.20.x対応
   - [ ] 既存コードの理解
   - [ ] 1.20.x互換性確認
   - [ ] 基本コマンド動作確認

2. **Week 3-4**: マルチプレイヤー対応（フェーズ3）
   - [ ] ConnectionManager実装
   - [ ] 認証システム実装
   - [ ] 権限システム実装
   - [ ] テスト

3. **Week 5-8**: 教育機能実装（フェーズ4）
   - [ ] TutorialManager実装
   - [ ] ChallengeSystem実装
   - [ ] ProgressTracker実装
   - [ ] UI実装

4. **Week 9-10**: テスト・最適化（フェーズ5）
   - [ ] 単体テスト
   - [ ] 統合テスト
   - [ ] 負荷テスト
   - [ ] ドキュメント最終化

5. **Week 11-12**: リリース準備（フェーズ6）
   - [ ] パッケージング
   - [ ] デプロイ
   - [ ] リリース

---

## 学習リソース

### 必読ドキュメント

#### Scratch拡張開発

- [Scratch Extensions Documentation](https://github.com/scratchfoundation/scratch-vm/blob/develop/docs/)
- [Scratch VM Architecture](https://github.com/scratchfoundation/scratch-vm/wiki)

#### Minecraft Forge MOD開発

- [Forge Documentation](https://docs.minecraftforge.net/)
- [Forge 1.20.x Tutorials](https://forge.gemwire.uk/wiki/Main_Page)
- [Minecraft Modding with Java](https://www.youtube.com/results?search_query=minecraft+forge+1.20+tutorial)

#### WebSocket/ネットワーク

- [Netty Documentation](https://netty.io/wiki/)
- [WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)

### 参考プロジェクト

- [takecx/RemoteControllerMod](https://github.com/takecx/RemoteControllerMod)
- [takecx/scratch-vm](https://github.com/takecx/scratch-vm)
- [Qiita記事: ScratchからMinecraftを操作](https://qiita.com/panda531/items/a6dfd87bd68ba2601793)

---

## トラブルシューティング

### よくある問題

#### 1. npm installが失敗する

```bash
# キャッシュクリア
npm cache clean --force

# node_modules削除
rm -rf node_modules package-lock.json
npm install
```

#### 2. Gradle buildが失敗する

```bash
# Gradleキャッシュクリア
./gradlew clean build --refresh-dependencies

# またはキャッシュ完全削除
rm -rf ~/.gradle/caches
./gradlew build
```

#### 3. Minecraft MODが読み込まれない

- Forgeバージョンが1.20.xか確認
- `logs/latest.log`でエラーを確認
- MOD JARファイルが`mods`フォルダに正しく配置されているか確認

---

## コミュニケーション

### 質問・相談

- プロジェクトに関する質問は[GitHub Issues](https://github.com/YOUR_USERNAME/minecraftedu-scratch/issues)で
- 設計に関する議論は[GitHub Discussions](https://github.com/YOUR_USERNAME/minecraftedu-scratch/discussions)で

### 進捗報告

開発の進捗は定期的にGitHubのIssuesやProject Boardで共有しましょう。

---

## チェックリスト

プロジェクトを始める前に、以下を確認してください：

### 環境準備

- [ ] Node.js 16.x以上がインストールされている
- [ ] Java JDK 17がインストールされている
- [ ] Gitがインストールされている
- [ ] GitHubアカウントを持っている
- [ ] Minecraft Java Edition 1.20.xを所有している

### ドキュメント確認

- [ ] README.mdを読んだ
- [ ] PROJECT_DESIGN.mdを読んだ
- [ ] SETUP_GUIDE.mdを読んだ
- [ ] 自分の役割と担当範囲を理解している

### リポジトリ準備

- [ ] takecxさんのリポジトリをforkした
- [ ] ローカルにcloneした
- [ ] ビルドが成功することを確認した
- [ ] 動作確認（Minecraft接続）ができた

### 開発準備

- [ ] IDEをインストールした（VSCode/IntelliJ）
- [ ] Git設定を完了した
- [ ] 開発ブランチを作成した

---

## 次に読むべきドキュメント

1. **開発環境構築**: `docs/SETUP_GUIDE.md`
2. **全体設計理解**: `PROJECT_DESIGN.md`
3. **実装開始**: `docs/MULTIPLAYER_DESIGN.md`または`docs/EDUCATION_DESIGN.md`

---

## まとめ

このプロジェクトは、以下の3つの柱で構成されています：

1. **Scratch-Minecraft連携**（takecxさんのプロジェクトベース）
2. **マルチプレイヤー対応**（新規実装）
3. **教育コンテンツ**（新規実装）

すべての設計ドキュメントが完成したので、これから実装フェーズに入ります。

**頑張って開発を進めていきましょう！🚀**

---

**作成日**: 2025-11-12
**対象**: プロジェクト開始メンバー
**次回更新**: フェーズ1完了後
