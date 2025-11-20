# MinecraftEdu Scratch Controller - インストールガイド

本番リリース版のインストール手順です。

## システム要件

### 必須環境
- **Minecraft Java Edition 1.20.1**
- **Minecraft Forge 1.20.1**
- **Webブラウザ**（Chrome、Firefox、Edge推奨）
- **Javaランタイム 17以上**

### 推奨環境
- Windows 10/11、macOS 10.15+、または Ubuntu 20.04+
- RAM 4GB以上
- 空きディスク容量 1GB以上

---

## インストール手順

### ステップ1: Minecraft Forgeのインストール

1. [Minecraft Forge公式サイト](https://files.minecraftforge.net/)にアクセス
2. **1.20.1用のForge**をダウンロード（Recommended版を推奨）
3. ダウンロードしたインストーラー（.jar）を実行
4. "Install client"を選択して「OK」をクリック
5. インストール完了を確認

### ステップ2: Minecraft MODのインストール

1. **Minecraftの modsフォルダ**を開く：
   - **Windows**: `%APPDATA%\.minecraft\mods\`
   - **macOS**: `~/Library/Application Support/minecraft/mods/`
   - **Linux**: `~/.minecraft/mods/`

2. このリリースパッケージの `minecraft-mod` フォルダ内にある
   **`minecraftedu-mod-0.1.0-1.20.1.jar`** を modsフォルダにコピー

### ステップ3: Minecraftの起動と確認

1. Minecraftランチャーを起動
2. プロファイルで **「forge-1.20.1」** を選択
3. 「プレイ」をクリックしてMinecraftを起動
4. ワールドを開く（新規作成または既存ワールド）
5. **ゲームログ**を確認（F3+Lまたはログファイルを参照）
   ```
   [WebSocketServer] WebSocket server started on port 14711
   ```
   このメッセージが表示されればMODが正常に動作しています

### ステップ4: Scratch GUIのセットアップ

#### オプションA: Webサーバーでホスティング（推奨）

1. このリリースパッケージの `scratch-gui` フォルダ全体をWebサーバーに配置
2. ブラウザで `http://your-server/scratch-gui/` にアクセス

#### オプションB: ローカルサーバーで実行

1. Python 3がインストールされている場合：
   ```bash
   cd scratch-gui
   python -m http.server 8601
   ```

2. Node.jsがインストールされている場合：
   ```bash
   cd scratch-gui
   npx http-server -p 8601
   ```

3. ブラウザで `http://localhost:8601/` を開く

### ステップ5: Minecraft拡張機能の有効化

1. Scratchエディタが開いたら、左下の **「拡張機能を追加」** ボタンをクリック
2. 拡張機能リストから **「Minecraft」** （緑色のアイコン）を選択
3. Minecraftカテゴリのブロックが表示されます

### ステップ6: 接続テスト

1. 以下のブロックを配置して実行：
   ```
   [Minecraftに接続] ホスト [localhost] ポート [14711]
   ```

2. 接続が成功すると、Minecraftのチャットに「WebSocket接続が確立されました」と表示されます

3. 次のブロックで動作確認：
   ```
   [チャットで言う] [Hello from Scratch!]
   ```

---

## トラブルシューティング

### 接続できない場合

#### 1. ポート14711が使用可能か確認
```bash
# Windows
netstat -an | findstr :14711

# macOS/Linux
netstat -an | grep 14711
```

#### 2. ファイアウォール設定を確認
- Windowsファイアウォールでポート14711を許可
- セキュリティソフトがブロックしていないか確認

#### 3. Minecraftのログを確認
- `.minecraft/logs/latest.log` を開く
- "WebSocket" でエラーメッセージを検索

### MODが読み込まれない場合

1. Minecraftのバージョンが1.20.1であることを確認
2. Forgeが正しくインストールされているか確認
3. modsフォルダのパスが正しいか確認
4. 他のMODとの競合がないか確認

### Scratch GUIが表示されない場合

1. ブラウザのキャッシュをクリア（Ctrl+Shift+Delete）
2. ページをリロード（Ctrl+R）
3. 開発者ツール（F12）でコンソールエラーを確認
4. JavaScriptが有効になっているか確認

---

## アンインストール

### MODの削除
modsフォルダから `minecraftedu-mod-0.1.0-1.20.1.jar` を削除

### Scratch GUIの削除
インストールした scratch-gui フォルダを削除

---

## サポート

問題が解決しない場合は、以下をお試しください：

1. [GitHub Issues](https://github.com/your-username/minecraft-scratch-controller/issues)で既知の問題を検索
2. 新しいIssueを作成して質問
3. READMEの「よくある質問」セクションを参照

---

**インストール完了！プログラミングを楽しんでください！** 🎮✨
