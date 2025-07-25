# Testing Guide - Minecraft Collaboration System

## 動作確認手順

### 1. Minecraft Modのビルドと起動

```bash
cd minecraft-mod
.\gradlew.bat clean build
.\gradlew.bat runClient
```

### 2. ビルド成功確認
- `build/libs/`にjarファイルが生成されていること
- jarJarによりWebSocketライブラリが含まれていること

### 3. Minecraft起動時の確認
ログで以下を確認:
- "Minecraft Collaboration Mod initialized"
- "WebSocket library is available"
- "WebSocket server started on port: 14711"

### 4. Scratch拡張のビルドとテスト

```bash
cd scratch-extension
npm install
npm run build
```

### 5. WebSocket接続テスト

Scratch拡張で以下のブロックを実行:
1. 「🔌 Minecraftに接続する」
2. 「📡 接続されている？」→ trueが返ること

### 6. 基本機能テスト

#### ブロック配置テスト
```scratch
🧱 [stone]を X:[0] Y:[64] Z:[0] に置く
```

#### プレイヤー位置取得テスト
```scratch
📍 プレイヤーのX座標
📍 プレイヤーのY座標
📍 プレイヤーのZ座標
```

#### チャットテスト
```scratch
💬 チャット: [Hello from Scratch!]
```

### 7. 高度な機能テスト

#### 円形建築テスト
```scratch
⭕ [stone]で 中心X:[0] Y:[64] Z:[0] 半径:[5] の円を作る
```

#### 家建築テスト
```scratch
🏠 [oak_planks]で X:[10] Y:[64] Z:[10] に 幅:[7] 奥行:[7] 高さ:[4] の家を作る
```

#### 時間・天候制御テスト
```scratch
🕐 時間を [day] に設定
🌤️ 天気を [clear] に設定
```

## トラブルシューティング

### WebSocket接続エラー
1. ポート14711が使用されていないか確認
2. Windows Firewallの設定を確認
3. Minecraftが正常に起動しているか確認

### ビルドエラー
1. Java 17が正しくインストールされているか確認
2. gradle.propertiesのJavaパス設定を確認
3. 依存関係のダウンロードが完了しているか確認

### 実行時エラー
1. logs/latest.logでエラー詳細を確認
2. WebSocketライブラリが正しく含まれているか確認
3. メッセージフォーマットが正しいか確認

## 開発者向けデバッグ

### ログレベル設定
`run/config/logging.properties`で詳細ログを有効化:
```properties
com.yourname.minecraftcollaboration.level=DEBUG
```

### WebSocketメッセージ監視
ブラウザの開発者ツールでWebSocket通信を監視可能

### Minecraft内コマンド
F3キーでデバッグ情報表示