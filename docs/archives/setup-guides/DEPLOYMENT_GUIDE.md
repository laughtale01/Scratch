# Minecraft Collaboration Project - デプロイメントガイド

## 📋 デプロイ前チェックリスト

### 1. ビルド確認
```bash
cd minecraft-mod
./gradlew clean build
./gradlew test
```
✅ 全165テストが合格（100%成功率）

### 2. 必要な環境
- Java 17以上
- Minecraft Forge 1.20.1
- Node.js 16以上（Scratch拡張機能用）

## 🚀 デプロイ手順

### A. Minecraft Mod のデプロイ

#### 1. リリースビルドの作成
```bash
cd minecraft-mod
./gradlew clean build
```

#### 2. JARファイルの確認
```
minecraft-mod/build/libs/
├── minecraft-collaboration-mod-1.0.0.jar     # メインJAR
└── minecraft-collaboration-mod-1.0.0-sources.jar  # ソースJAR
```

#### 3. Minecraftへのインストール

##### シングルプレイヤー向け
1. Minecraft Forgeをインストール（1.20.1）
2. `.minecraft/mods/`フォルダに`minecraft-collaboration-mod-1.0.0.jar`をコピー
3. Minecraftを起動

##### マルチプレイヤーサーバー向け
1. Minecraft Forgeサーバーをセットアップ
2. `server/mods/`フォルダにJARファイルをコピー
3. サーバー設定ファイル（`server.properties`）を編集：
```properties
# WebSocketサーバー設定
enable-websocket=true
websocket-port=14711
```

### B. Scratch拡張機能のデプロイ

#### 1. ビルド
```bash
cd scratch-extension
npm install
npm run build
```

#### 2. デプロイ方法

##### オプション1: ローカルScratch
1. Scratch GUIをクローン
2. 拡張機能を追加：
```bash
cp -r scratch-extension/* /path/to/scratch-gui/src/extensions/minecraft/
```

##### オプション2: Webホスティング
1. 静的ファイルをホスティングサービスにアップロード
2. CORSヘッダーを設定：
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST
```

### C. Firebase（Webインターフェース）のデプロイ

#### 1. Firebase設定
```bash
npm install -g firebase-tools
firebase login
firebase init
```

#### 2. プロジェクト設定
`.firebaserc`:
```json
{
  "projects": {
    "default": "minecraft-collaboration"
  }
}
```

`firebase.json`:
```json
{
  "hosting": {
    "public": "docs",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**"
    ],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ],
    "headers": [
      {
        "source": "**/*.js",
        "headers": [
          {
            "key": "Access-Control-Allow-Origin",
            "value": "*"
          }
        ]
      }
    ]
  }
}
```

#### 3. デプロイ実行
```bash
firebase deploy
```

## 🔧 設定ファイル

### minecraft-collaboration.toml（Mod設定）
```toml
[general]
  # WebSocketサーバー設定
  websocket_enabled = true
  websocket_port = 14711
  
  # セキュリティ設定
  allow_external_connections = false
  rate_limit_per_second = 10
  max_connections = 10

[educational]
  # 教育モード設定
  classroom_mode_default = false
  restricted_blocks_enabled = true
  
[localization]
  # デフォルト言語
  default_language = "en_US"
```

## 📦 配布パッケージの作成

### 1. リリースパッケージ構成
```
minecraft-collaboration-v1.0.0/
├── mods/
│   └── minecraft-collaboration-mod-1.0.0.jar
├── scratch-extension/
│   ├── index.js
│   └── minecraft-collaboration.sb3
├── docs/
│   ├── README.md
│   ├── INSTALLATION_GUIDE.md
│   └── USER_MANUAL.md
└── config/
    └── minecraft-collaboration.toml
```

### 2. ZIPファイルの作成
```bash
# パッケージディレクトリの作成
mkdir -p release/minecraft-collaboration-v1.0.0/{mods,scratch-extension,docs,config}

# ファイルのコピー
cp minecraft-mod/build/libs/*.jar release/minecraft-collaboration-v1.0.0/mods/
cp -r scratch-extension/dist/* release/minecraft-collaboration-v1.0.0/scratch-extension/
cp -r docs/* release/minecraft-collaboration-v1.0.0/docs/
cp minecraft-mod/src/main/resources/minecraft-collaboration.toml release/minecraft-collaboration-v1.0.0/config/

# ZIP作成
cd release
zip -r minecraft-collaboration-v1.0.0.zip minecraft-collaboration-v1.0.0/
```

## 🌐 GitHub Releasesへの公開

### 1. タグの作成
```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### 2. リリースノートの作成
```markdown
# Minecraft Collaboration Mod v1.0.0

## 🎉 Features
- WebSocket通信による外部連携
- Scratch拡張機能対応
- 多言語サポート（7言語）
- 教育モード機能
- コラボレーション機能

## 📦 Installation
1. Download `minecraft-collaboration-v1.0.0.zip`
2. Extract to your Minecraft directory
3. Launch Minecraft with Forge 1.20.1

## 🔧 Requirements
- Minecraft 1.20.1
- Minecraft Forge 1.20.1
- Java 17+

## 📝 Changelog
- Initial release
- Full test coverage (165 tests, 100% pass rate)
- Security features implemented
- Multi-language support added
```

### 3. アセットのアップロード
- `minecraft-collaboration-v1.0.0.zip`
- `minecraft-collaboration-mod-1.0.0.jar`（単体）
- `scratch-extension.zip`（Scratch拡張のみ）

## 🔍 デプロイ後の確認

### 1. 動作確認チェックリスト
- [ ] Minecraftが正常に起動する
- [ ] WebSocketサーバーがポート14711で起動する
- [ ] Scratch拡張機能が接続できる
- [ ] コマンドが正常に動作する
- [ ] 多言語切り替えが機能する

### 2. パフォーマンス確認
```bash
# WebSocketサーバーの確認
netstat -an | grep 14711

# ログの確認
tail -f logs/minecraft-collaboration.log
```

### 3. セキュリティ確認
- [ ] 外部接続が制限されている
- [ ] レート制限が機能している
- [ ] 危険なブロックがフィルタリングされている

## 📚 ドキュメント

### エンドユーザー向け
- `/docs/setup/QUICK_START.md` - クイックスタートガイド
- `/docs/USER_MANUAL.md` - ユーザーマニュアル
- `/docs/FAQ.md` - よくある質問

### 開発者向け
- `/docs/API_REFERENCE.md` - API リファレンス
- `/docs/development/CONTRIBUTING.md` - 貢献ガイド
- `/docs/architecture.md` - アーキテクチャ設計

## ⚠️ トラブルシューティング

### よくある問題

#### 1. WebSocketサーバーが起動しない
```bash
# ポートの確認
lsof -i :14711

# 別のポートで起動
java -Dwebsocket.port=14712 -jar minecraft-server.jar
```

#### 2. Scratch拡張機能が接続できない
- ブラウザのコンソールでエラーを確認
- CORSポリシーを確認
- ファイアウォール設定を確認

#### 3. Modがクラッシュする
- Minecraft Forgeのバージョンを確認
- 他のModとの競合を確認
- ログファイルを確認：`logs/debug.log`

## 📞 サポート

- GitHub Issues: https://github.com/yourname/minecraft-collaboration/issues
- Discord: https://discord.gg/minecraft-collab
- Email: support@minecraft-collaboration.com

---
最終更新: 2025-07-30