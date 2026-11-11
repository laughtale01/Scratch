# MinecraftEdu MOD

Minecraft Forge MOD for MinecraftEdu Scratch Controller

## 概要

このMODは、Scratch 3.0からMinecraftを操作できるようにするForge MODです。
WebSocketサーバーを起動し、Scratchクライアントからのコマンドを受け取ってMinecraft内で実行します。

## 必要環境

- **Minecraft**: Java Edition 1.20.1
- **Minecraft Forge**: 47.2.0以上
- **Java**: JDK 17以上

## ビルド方法

### 前提条件

- Java JDK 17がインストールされていること
- `JAVA_HOME`環境変数が設定されていること

### ビルド手順

#### Windows

```bash
gradlew.bat build
```

#### macOS/Linux

```bash
./gradlew build
```

### ビルド成果物

ビルドが成功すると、以下にMODファイルが生成されます：

```
build/libs/minecraftedu-mod-0.3.0-1.20.1.jar
```

## インストール方法

### 1. Minecraft Forgeのインストール

1. https://files.minecraftforge.net/ にアクセス
2. Minecraft 1.20.1用のForgeをダウンロード
3. インストーラーを実行し、「Install client」を選択

### 2. MODのインストール

ビルドしたJARファイルをMinecraftのmodsフォルダにコピー：

先に、mods フォルダにある古い `minecraftedu-mod-*.jar` を外してください（同じ MOD が 2 つあると正しく読み込めません）。
`build/libs/` には前にビルドした古い jar が残ることがあるので、コピーする jar は名前を書いて選びます。

```bash
# Windows
copy build\libs\minecraftedu-mod-0.3.0-1.20.1.jar %APPDATA%\.minecraft\mods\

# macOS
cp build/libs/minecraftedu-mod-0.3.0-1.20.1.jar ~/Library/Application\ Support/minecraft/mods/

# Linux
cp build/libs/minecraftedu-mod-0.3.0-1.20.1.jar ~/.minecraft/mods/
```

### 3. Minecraftの起動

1. Minecraftランチャーを開く
2. 「forge-1.20.1」プロファイルを選択
3. 「プレイ」をクリック

## 機能

### WebSocketサーバー

- **ポート**: 14711
- **エンドポイント**: `/minecraft`
- **プロトコル**: JSON over WebSocket

### 対応コマンド

- ブロック: `setBlock`（絶対座標・相対座標）、`fillBlocks`、`getBlockType`、`getTargetBlock`、`setContainerItem`
- プレイヤー: `getPosition`、`getPlayerFacing`、`teleport`、`setGameMode`、`setMoveSpeed`、`setFlySpeed`、`setNightVision`
- ワールド: `chat`、`setWeather`、`setTime`、`setGameRule`、`summonEntity`、`setEntitySpawning`、`clearArea`、`clearAllEntities`
- 録画（Windows だけ）: `startRecording`、`stopRecording`、`getRecordingStatus`

命令の中身（受け付ける値・断るとき）は `src/main/java/com/github/minecraftedu/commands/CommandExecutor.java` を、
上限や断ったときのチャットの確かめ方は `tests/websocket/README.md` を参照してください。

## 開発

### 開発環境でのMinecraft起動

```bash
# クライアント起動
./gradlew runClient

# サーバー起動
./gradlew runServer
```

### コードの配置

```
src/main/java/com/github/minecraftedu/
├── MinecraftEduMod.java          # メインMODクラス（起動・停止、/entityspawn）
├── network/                       # Scratch との通信（WebSocket）
│   ├── SimpleWebSocketServer.java
│   └── MinecraftWebSocketHandler.java
├── commands/                      # 命令の実行
│   ├── CommandExecutor.java
│   ├── BuildQueue.java            # 世界を変える仕事を届いた順に並べ、少しずつ進める列
│   └── FillJob.java               # 箱の形の範囲に、続きから置いていく仕事（埋める・周りをきれいにする）
├── block/                         # 追加ブロック（縦スラブ。金の階段・ハーフは Minecraft の仕組みを使うので、ここには無い）
│   └── VerticalSlabBlock.java
└── init/                          # ブロック・アイテムの登録
    ├── ModBlocks.java
    └── ModItems.java
```

## トラブルシューティング

### ビルドエラー

#### エラー: "JAVA_HOME is not set"

```bash
# Java 17をインストールし、環境変数を設定
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17

# macOS/Linux
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

#### エラー: "Could not resolve dependencies"

```bash
# Gradleキャッシュをクリア
./gradlew clean --refresh-dependencies
./gradlew build
```

### 実行時エラー

#### MODが読み込まれない

1. Forgeバージョンを確認（1.20.1対応か）
2. `logs/latest.log`でエラーを確認
3. JARファイルが正しく`mods`フォルダにあるか確認

#### WebSocketサーバーが起動しない

1. ポート14711が他のプログラムで使用されていないか確認
2. ファイアウォール設定を確認
3. `logs/latest.log`でエラーメッセージを確認

## ライセンス

MIT License

## クレジット

このプロジェクトは[takecx](https://github.com/takecx)さんのScratch-Minecraft連携プロジェクトのコンセプトを参考にしています。

## リンク

- プロジェクトリポジトリ: https://github.com/laughtale01/Scratch
- Minecraft Forge: https://files.minecraftforge.net/
- ドキュメント: ../docs/

---

**バージョン**: 0.3.0（build.gradle の version。0.3.0 で金ブロックの階段とハーフブロックを足した）
**対応Minecraft**: 1.20.1
**最終更新**: 2026-11-10
