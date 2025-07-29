# セキュリティ設定修正 - IPv6 localhost対応 🔐

## 🔧 修正内容

**問題**: IPv6 localhost（`0:0:0:0:0:0:0:1`）からの接続が拒否されていました

**解決**: SecurityConfig.javaでIPv6 localhostアドレスを許可リストに追加

```java
public static boolean isAddressAllowed(String address) {
    return address != null && (
        address.equals("localhost") ||
        address.equals("127.0.0.1") ||
        address.startsWith("127.") ||
        address.equals("::1") ||
        address.equals("0:0:0:0:0:0:0:1") ||  // ← 追加
        // ... 他のローカルネットワーク
    );
}
```

## 📋 インストール手順

### 手動操作が必要:

1. **更新されたJARファイルをコピー**
   ```
   コピー元: D:\minecraft_collaboration_project\minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar
   コピー先: %APPDATA%\.minecraft\mods\
   ```
   ※ 既存ファイルを上書き

2. **Minecraft再起動**
   - 完全終了してから起動
   - ワールドにログイン

## 🎯 期待される修正結果

### 修正前
```
[WARN] Connection rejected from unauthorized address: /[0:0:0:0:0:0:0:1]:54242
[INFO] Scratch client disconnected: /[0:0:0:0:0:0:0:1]:54242 (Code: 1003, Reason: Unauthorized address)
```

### 修正後
```
[INFO] New Scratch client connected: /[0:0:0:0:0:0:0:1]:xxxxx
[INFO] WebSocket接続成功
```

## 🧪 テスト項目

修正後に以下のテストが成功することを確認：

1. **基本接続テスト**
   ```bash
   node quick-connection-check.js
   ```

2. **詳細機能テスト**
   ```bash
   node detailed-test.js
   ```

3. **期待される結果**
   - ✅ WebSocket接続成功
   - ✅ 認証処理成功
   - ✅ 全コマンドが正常応答
   - ✅ 総合成功率80%+達成

---
**重要: 更新されたJARファイルをインストール後、Minecraftを再起動してください**