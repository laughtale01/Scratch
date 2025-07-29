# Minecraft Collaboration Mod - インストールガイド 📦

## 現在の状況

✅ **JARファイル生成完了**
- `minecraft-collaboration-mod-1.0.0.jar` - メインMod
- `minecraft-collaboration-mod-1.0.0-all.jar` - 依存関係含む

## インストール手順

### 1. JARファイルの配置
```bash
# 生成されたJARファイルの場所
D:\minecraft_collaboration_project\minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0.jar
```

### 2. Minecraftのmodsフォルダに配置
```
%APPDATA%\.minecraft\mods\
```

**手動操作が必要:**
1. **minecraft-collaboration-mod-1.0.0.jar** をコピー
2. **%APPDATA%\.minecraft\mods\** フォルダに貼り付け
3. 既存の古いバージョンがあれば削除

### 3. Minecraft再起動
1. 現在のMinecraftを完全終了
2. Minecraft Launcher で Forge 1.20.1 プロファイルを選択
3. Minecraftを起動
4. ワールドにログイン

### 4. Mod読み込み確認
Minecraftのチャットで以下を確認:
```
/forge
```
リストに "minecraftcollaboration" が表示されることを確認

### 5. WebSocketサーバー起動確認
ログに以下のメッセージが表示されることを確認:
```
[INFO] Minecraft Collaboration Mod initialized
[INFO] WebSocket server started successfully
[INFO] WebSocket server listening on port: 14711
```

## トラブルシューティング

### 問題1: Modが読み込まれない
- JARファイルがmodsフォルダにあることを確認
- Forge バージョンが 1.20.1 であることを確認
- 他のModとの競合がないか確認

### 問題2: WebSocketサーバーが起動しない
- ポート14711が他のプロセスで使用されていないか確認
- ファイアウォールの設定を確認
- ログファイルでエラーメッセージを確認

### 問題3: 依存関係エラー
- `minecraft-collaboration-mod-1.0.0-all.jar` を使用
- Java WebSocket ライブラリが含まれています

## 次のステップ

Mod が正常にインストールされたら:

1. **接続テスト実行**
   ```bash
   node quick-connection-check.js
   ```

2. **詳細機能テスト実行**
   ```bash
   node detailed-test.js
   ```

3. **期待される結果**
   - WebSocket接続成功
   - 全コマンドが正常動作
   - 総合成功率 80%+ 達成

---
**重要: 手動でJARファイルをmodsフォルダに配置してから、Minecraftを再起動してください。**