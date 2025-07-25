# Minecraft-Scratch統合テストガイド

## 概要
このガイドでは、Minecraft modとScratch拡張機能の統合テストの手順を説明します。

## 前提条件
- Java 17がインストールされていること
- Node.js v24がインストールされていること
- Minecraftクライアントを実行できるGUI環境があること

## テスト手順

### 1. Minecraft Modの起動
```bash
cd minecraft-mod
./gradlew runClient
```
- Minecraftが起動したら、新しいワールドを作成またはロード
- F3キーを押してデバッグ情報を表示し、WebSocketサーバーが起動していることを確認

### 2. WebSocket接続テスト

#### ブラウザでのテスト
1. `test-websocket-connection.html`をブラウザで開く
2. 「接続」ボタンをクリック
3. 接続が成功したら、以下のコマンドをテスト：
   - `player.getPos()` - プレイヤーの位置を取得
   - `info.getPlayerName()` - プレイヤー名を取得
   - `player.chat("Hello!")` - チャットメッセージを送信
   - `block.place(stone,0,65,0)` - ブロックを設置

#### Node.jsでのテスト
```bash
node test-websocket-server.js
```

### 3. Scratch拡張機能のテスト

1. Scratch 3.0 GUIを起動
```bash
cd scratch-gui
npm start
```

2. ブラウザで`http://localhost:8601`を開く

3. 拡張機能を追加：
   - 左下の拡張機能ボタンをクリック
   - 「Minecraft Collaboration」を選択

4. 以下のブロックをテスト：
   - 「Minecraftに接続」
   - 「プレイヤーの位置を取得」
   - 「チャットメッセージを送信」
   - 「ブロックを配置」

## トラブルシューティング

### WebSocket接続できない場合
1. Minecraftが起動していることを確認
2. ポート14711が使用されていないことを確認
3. ファイアウォールでポートがブロックされていないか確認

### Minecraftが起動しない場合
1. Java 17が正しくインストールされているか確認
2. メモリ不足の場合は、`gradle.properties`でメモリ設定を調整

### Scratch拡張機能が表示されない場合
1. 拡張機能がビルドされているか確認（`npm run build`）
2. ブラウザのコンソールでエラーを確認

## 成功基準
- WebSocketでMinecraftに接続できること
- Scratchからコマンドを送信できること
- Minecraftからの応答を受信できること
- ブロックの配置やチャットメッセージが正常に動作すること