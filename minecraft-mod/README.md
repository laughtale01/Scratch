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
build/libs/minecraftedu-mod-0.1.0-1.20.1.jar
```

## インストール方法

### 1. Minecraft Forgeのインストール

1. https://files.minecraftforge.net/ にアクセス
2. Minecraft 1.20.1用のForgeをダウンロード
3. インストーラーを実行し、「Install client」を選択

### 2. MODのインストール

ビルドしたJARファイルをMinecraftのmodsフォルダにコピー：

```bash
# Windows
copy build\libs\*.jar %APPDATA%\.minecraft\mods\

# macOS
cp build/libs/*.jar ~/Library/Application\ Support/minecraft/mods/

# Linux
cp build/libs/*.jar ~/.minecraft/mods/
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

- `chat` - チャットメッセージ送信
- `setBlock` - ブロック配置（絶対座標・相対座標）
- `summonEntity` - エンティティ召喚
- `teleport` - プレイヤーテレポート
- `setWeather` - 天気変更
- `setTime` - 時刻変更

詳細は `shared/protocol/PROTOCOL_SPEC.md` を参照してください。

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
├── MinecraftEduMod.java          # メインMODクラス
├── network/                       # ネットワーク層
│   ├── WebSocketServer.java
│   └── MinecraftWebSocketHandler.java
├── commands/                      # コマンド処理
│   └── CommandExecutor.java
├── education/                     # 教育機能（今後実装）
└── utils/                         # ユーティリティ
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

**バージョン**: 0.1.0
**対応Minecraft**: 1.20.1
**最終更新**: 2025-11-12
