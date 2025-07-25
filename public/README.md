# 🎮 Minecraft協調学習システム - Web Interface

このページから、あなたのMinecraftサーバーに接続してScratchプログラミングを始められます。

## 🚀 クイックスタート

### 1. Minecraft Modのセットアップ

1. [Releases](https://github.com/[your-username]/minecraft-collaboration/releases)から最新の`minecraft-collaboration-mod-x.x.x.jar`をダウンロード
2. Minecraft Forge 1.20.1がインストールされていることを確認
3. ダウンロードしたJARファイルを`mods`フォルダに配置
4. Minecraftを起動

### 2. 接続

1. WebSocketサーバーURLを入力
   - ローカル: `ws://localhost:14711`
   - LAN内: `ws://[あなたのIPアドレス]:14711`
2. 「接続」ボタンをクリック
3. 接続成功のメッセージを確認

### 3. プログラミング開始

「Scratch GUI」ボタンをクリックして、ビジュアルプログラミングを開始！

## 🔧 トラブルシューティング

### 接続できない場合

- **Minecraftが起動しているか確認**
  - サーバーログに`WebSocket server started on port: 14711`が表示されているか確認

- **ファイアウォール設定**
  - Windows Defenderやアンチウイルスソフトがポート14711をブロックしていないか確認

- **正しいURLを使用しているか**
  - ローカル: `ws://localhost:14711`
  - 同一ネットワーク: `ws://192.168.x.x:14711`

## 📚 利用可能なコマンド

| コマンド | 説明 |
|---------|------|
| `minecraft.connect()` | 接続確認 |
| `minecraft.status()` | サーバーステータス取得 |
| `player.getPos()` | プレイヤー位置取得 |
| `chat.post(message)` | チャットメッセージ送信 |
| `world.setBlock(x,y,z,blockType)` | ブロック設置 |
| `world.getBlock(x,y,z)` | ブロック情報取得 |

## 🛡️ セキュリティについて

- このシステムはローカルネットワーク内での使用を推奨します
- インターネット経由で接続する場合は、適切なセキュリティ対策を行ってください
- 不明なサーバーには接続しないでください

## 🤝 貢献

バグ報告や機能リクエストは[Issues](https://github.com/[your-username]/minecraft-collaboration/issues)へ

## 📄 ライセンス

MIT License - 詳細は[LICENSE](https://github.com/[your-username]/minecraft-collaboration/blob/main/LICENSE)を参照