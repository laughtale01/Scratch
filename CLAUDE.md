# CLAUDE.md - プロジェクト開発ガイド

このファイルはClaude Codeがプロジェクトを理解するためのガイドです。

## 絶対禁止事項（最重要）

1. **ファイル削除禁止** - ローカルファイルは絶対に削除しない
2. **force push禁止** - `git push --force` は絶対に実行しない
3. **無断変更禁止** - 大きな変更は必ずユーザーに確認してから実行
4. **ローカルファイルがマスター** - GitHubではなくローカルが正

## プロジェクト概要

**Scratch 3.0 + Minecraft Extension**
- Scratch 3.0ベースのビジュアルプログラミング環境
- Minecraft Java Edition (1.20.x) とWebSocket連携
- 教育用途向けの機能を多数搭載

### 対象ユーザー
- **小学生**（プログラミング初心者の子供向け）
- 直感的で簡単な操作性を重視
- 段階的に学べる設計

### 教育方針
- **簡単な操作性**: 小学生でも直感的に使える
- **段階的学習**: ステップバイステップで学べる構成
- **失敗しにくい設計**: エラーを起こしにくい仕組み

## コーディング規約

### 日本語コメント必須
```javascript
// 良い例
/**
 * ブロックを配置する
 * @param {number} x - X座標
 * @param {number} y - Y座標
 * @param {number} z - Z座標
 */
setBlock(x, y, z) { ... }

// 悪い例（英語コメント）
// Place a block at coordinates
setBlock(x, y, z) { ... }
```

### gui.jsの編集
- **直接編集OK** - gui.jsを直接編集してよい
- 約47万行あるため、Grepで該当箇所を特定してから編集
- Minecraft拡張機能は行64003付近から

## ブランチ構造（重要）

```
main ブランチ:
├── minecraft-mod/    ← MODソースコード（Git管理対象）
├── scratch-client/   ← Scratch VMソース（Git管理対象）
└── 全てのソースコード

gh-pages ブランチ（GitHub Pages用）:
├── gui.js, index.html など（Web用ビルド済みファイル）
├── .gitignore で以下を除外:
│   - minecraft-mod/   ← Git管理対象外！
│   - scratch-client/  ← Git管理対象外！
└── Webアプリのみ
```

### 注意事項
- **MOD開発は`main`ブランチで行うこと**
- `gh-pages`ブランチでは`minecraft-mod/`がGit管理対象外
- ブランチ切り替え時にGit管理外ファイルが消える可能性あり

## ディレクトリ構造

```
D:\laughtale01-scratch\
├── index.html              # メインHTML
├── gui.js                  # Scratch GUI + Minecraft拡張（約47万行）
├── blocksonly.js           # ブロックのみバージョン
├── player.js               # プレイヤーバージョン
├── minecraft-mod/          # Forge MOD ソースコード
│   ├── gradlew             # Gradle Wrapper（必須）
│   ├── gradlew.bat         # Windows用（必須）
│   ├── gradle/wrapper/     # Gradle Wrapper JAR（必須）
│   ├── build.gradle        # ビルド設定
│   └── src/main/java/      # Javaソース
├── static/                 # 静的リソース
└── docs/                   # ドキュメント
```

## デプロイ手順

### gh-pagesに直接push
```bash
# 1. gui.jsなどを編集
# 2. 変更をコミット
git add gui.js
git commit -m "feat: 機能説明"

# 3. gh-pagesにpush
git push origin gh-pages
```

- GitHub Pagesで自動的に公開される
- URL: https://laughtale01.github.io/Scratch

## テスト方法

### 手動テスト重視
1. **ローカルMinecraft**でMODを起動
2. **ブラウザ**でScratch GUIを開く
3. **接続テスト**: Minecraftブロックで接続確認
4. **機能テスト**: 各ブロックの動作を確認

### MODのテスト手順
```bash
# 1. MODをビルド
cd minecraft-mod
./gradlew build

# 2. JARをMinecraftのmodsフォルダにコピー
# 3. Minecraftを起動
# 4. Scratchから接続してテスト
```

## Minecraft MOD

### ビルド方法
```bash
cd minecraft-mod
./gradlew build
# 出力: build/libs/minecraftedu-mod-0.1.0.jar
```

### 必須ファイル（ビルドに必要）
- `gradlew` / `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle.properties`
- `build.gradle`
- `settings.gradle`

### MODが提供する機能
- WebSocketサーバー（ポート14711）
- 縦スラブブロック39種
- コマンド: setBlock, fillBlocks, teleport, setWeather, setTime, etc.

## Scratch拡張機能（gui.js内）

### クラス
- `Scratch3MinecraftBlocks`（行64003〜）

### 主要メソッド
- `connect(args)` - Minecraft接続
- `setBlock(args)` - ブロック配置
- `teleport(args)` - テレポート
- `prepareForLearning()` - 学習準備（教育向け）

### 翻訳（必須）
- 74言語対応
- formatMessage()でi18n
- **日本語（漢字・ひらがな）対応は必須**
- 新規ブロック追加時は必ず翻訳も追加すること

#### 翻訳の追加場所（gui.js内）
1. **ブロックテキスト**: `minecraft.ブロック名` のID
2. **メニュー項目**: `minecraft.menu.項目名` のID
3. **翻訳データ**: gui.js内の各言語セクション（`ja:`, `ja-Hira:` 等）

#### 必須対応言語
- `ja` - 日本語（漢字）
- `ja-Hira` - 日本語（ひらがな）
- 他言語は可能な限り対応

## 今後の開発方針

### 優先事項
1. **教育機能の強化** - 学習モード、段階的チュートリアルなど
2. **ブロック追加** - 対応Minecraftブロックの拡充

### 教育機能のアイデア
- 段階的な難易度設定
- ヒント機能
- 作品共有機能
- 先生向け管理機能

## よくある問題と解決方法

### MODがビルドできない
**原因**: Gradle Wrapperファイルが欠けている
**解決**: `main`ブランチから復元
```bash
git show main:minecraft-mod/gradlew > minecraft-mod/gradlew
git show main:minecraft-mod/gradlew.bat > minecraft-mod/gradlew.bat
mkdir -p minecraft-mod/gradle/wrapper
git show main:minecraft-mod/gradle/wrapper/gradle-wrapper.jar > minecraft-mod/gradle/wrapper/gradle-wrapper.jar
git show main:minecraft-mod/gradle/wrapper/gradle-wrapper.properties > minecraft-mod/gradle/wrapper/gradle-wrapper.properties
git show main:minecraft-mod/gradle.properties > minecraft-mod/gradle.properties
chmod +x minecraft-mod/gradlew
```

### ローカルファイルがマスター
- ユーザーはローカルファイルを正とする開発スタイル
- GitHubは共有/デプロイ用
- ローカルの変更を優先すること

## 開発ルール

1. **ローカルファイルを削除しない** - ユーザーの明示的な指示がない限り
2. **ローカルファイルがマスター** - 修正・追加は必ずローカルファイルを最優先で編集
3. **バックアップの場所**: `D:\laughtale01-scratch(セーブデータ変換機能実装前)\laughtale01-scratch`
4. **MOD開発**: `main`ブランチで行う
5. **Webデプロイ**: `gh-pages`ブランチを使用
6. **コメントは日本語で書く**

## ブロック修正・追加時の作業フロー（重要）

Scratchブロックを修正・追加した場合は、以下の手順を必ず実行すること：

### 1. ローカルファイルの編集（マスター）
```
gui.js                    ← Scratch側ブロック定義・メソッド
minecraft-mod/src/...     ← MOD側コマンド実装
```

### 2. MODの再ビルド（必要な場合）
```bash
cd minecraft-mod
./gradlew build
# 出力: build/libs/minecraftedu-mod-0.1.0.jar
```
※ MOD側（CommandExecutor.java等）を変更した場合は必須

### 3. コミット＆プッシュ
```bash
# gui.jsの変更をコミット
git add gui.js
git commit -m "feat: 機能説明"
git push origin gh-pages
```

### 4. 動作確認
1. MODをMinecraftのmodsフォルダにコピー
2. Minecraftを再起動
3. https://laughtale01.github.io/Scratch/ をリロード（Ctrl+F5）
4. 接続して動作確認

### ファイルの対応関係
| Scratch側 (gui.js) | MOD側 (CommandExecutor.java) |
|-------------------|------------------------------|
| ブロック定義 (`getInfo()`) | - |
| メニュー定義 (`menus`) | - |
| メソッド実装 | `execute()` のswitch文 |
| `sendCommand('xxx', params)` | `executeXxx(params)` |

## 技術スタック

- **Frontend**: Scratch 3.0 (React), JavaScript
- **MOD**: Minecraft Forge 1.20.1, Java 17
- **通信**: WebSocket (ポート14711)
- **デプロイ**: GitHub Pages
