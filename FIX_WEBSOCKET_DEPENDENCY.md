# WebSocket依存関係問題 - 修正完了 🔧

## 🚨 問題の原因

**ClassNotFoundException: org.java_websocket.server.WebSocketServer**

- WebSocketライブラリがJARファイルに正しくバンドルされていませんでした
- jarJarの設定が不完全でした

## ✅ 修正内容

### 1. Forgeバージョン更新
```gradle
// 47.2.0 → 47.4.0 に更新
minecraft 'net.minecraftforge:forge:1.20.1-47.4.0'
```

### 2. jarJar設定強化
```gradle
jarJar(group: 'org.java-websocket', name: 'Java-WebSocket', version: '[1.5.4,)') {
    jarJar.ranged(it, '[1.5.4,1.5.5)')
}
```

### 3. 新しいJARファイル生成
- `minecraft-collaboration-mod-1.0.0-all.jar` - WebSocket依存関係含む
- ファイルサイズが大きくなり、依存関係が正しく含まれています

## 📋 インストール手順

### 手動操作が必要:

1. **古いJARファイルを削除**
   ```
   %APPDATA%\.minecraft\mods\minecraft-collaboration-mod-1.0.0.jar (削除)
   ```

2. **新しいJARファイルをコピー**
   ```
   コピー元: D:\minecraft_collaboration_project\minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar
   コピー先: %APPDATA%\.minecraft\mods\
   ```

3. **Minecraft再起動**
   - 完全終了してから起動
   - Forge 1.20.1 プロファイルを使用
   - ワールドにログイン

## 🔍 確認項目

### Mod読み込み確認
```
/forge コマンドでModリストを確認
→ "minecraftcollaboration" が表示されること
```

### ログ確認
```
[INFO] Minecraft Collaboration Mod initialized
[INFO] WebSocket server started successfully
[INFO] WebSocket server listening on port: 14711
```

### 接続テスト
```bash
node quick-connection-check.js
```

## 🎯 期待される結果

- ✅ Modが正常に読み込まれる
- ✅ WebSocketサーバーが起動する
- ✅ ポート14711で接続可能
- ✅ 全コマンドが正常動作

---
**重要: 必ず "-all.jar" ファイルを使用してください（依存関係含む）**