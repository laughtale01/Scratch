# Minecraft Collaboration Mod インストールガイド

## ビルド済みJARファイル

以下のJARファイルが正常にビルドされました：
- `minecraft-collaboration-mod-1.0.0-all.jar` - **WebSocketライブラリを含む完全版（推奨）**
- `minecraft-collaboration-mod-1.0.0.jar` - 基本版（WebSocketライブラリ別途必要）

## インストール手順

### 1. Minecraft Forge 1.20.1のインストール
1. [Minecraft Forge 1.20.1-47.2.0](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)をダウンロード
2. インストーラーを実行してForgeをインストール

### 2. Modのインストール
1. Minecraftランチャーを開く
2. Forgeプロファイルを選択
3. 「インストールフォルダ」を開く
4. `mods`フォルダに`minecraft-collaboration-mod-1.0.0-all.jar`をコピー

### 3. Minecraftの起動
1. Minecraft Forge 1.20.1プロファイルを選択して起動
2. タイトル画面で「Mods」をクリック
3. "Minecraft Collaboration Mod"がリストに表示されることを確認

## WebSocket接続の確認

Modが正常に動作すると、以下のポートでWebSocketサーバーが起動します：
- **ポート 14711**: Scratch拡張機能との通信用
- **ポート 14712**: コラボレーションサーバー

### 接続テスト方法
1. Minecraft（Forgeプロファイル）を起動
2. ワールドに入る
3. F3キーを押してデバッグ画面を表示
4. WebSocketサーバーの起動状態を確認

## トラブルシューティング

### WebSocketサーバーが起動しない場合
- ログファイル（`.minecraft/logs/latest.log`）を確認
- ポート14711/14712が他のアプリケーションで使用されていないか確認
- ファイアウォール設定を確認

### Scratch拡張機能と接続できない場合
1. Minecraftが起動していることを確認
2. Scratch GUIが`http://localhost:8601`で動作していることを確認
3. ブラウザのコンソールでエラーメッセージを確認

## 次のステップ

1. Scratch GUIを開く（`http://localhost:8601`）
2. 拡張機能メニューから「Minecraft 接続」を追加
3. 「Minecraftに接続」ブロックを使用して接続テスト
4. その他のMinecraft拡張機能を追加して建築開始！

---
最終更新: 2025-07-15