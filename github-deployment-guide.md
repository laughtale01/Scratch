# GitHub公開に向けた設計ガイド

## 🌐 現在のアーキテクチャと将来の展開

### 現在の構成（ローカル環境）
```
[ブラウザ] → WebSocket → [localhost:14711] → [Minecraft Server]
```

### GitHub Pages公開時の構成
```
[任意のブラウザ] → WebSocket → [ユーザーのMinecraftサーバー:14711]
        ↑
    GitHub Pages
   (静的ホスティング)
```

## 📋 必要な対応事項

### 1. WebSocket接続先の設定可能化

現在のコード:
```javascript
ws = new WebSocket('ws://localhost:14711');
```

改善案:
```javascript
// 設定可能なWebSocket URL
const WS_URL = localStorage.getItem('minecraft_ws_url') || 'ws://localhost:14711';
ws = new WebSocket(WS_URL);
```

### 2. CORS対応（重要）

Minecraftサーバー側で異なるオリジンからの接続を許可する必要があります：

```java
// WebSocketHandler.java に追加
@Override
public void onOpen(WebSocket conn, ClientHandshake handshake) {
    // GitHub Pagesからの接続を許可
    String origin = handshake.getFieldValue("Origin");
    if (isAllowedOrigin(origin)) {
        // 接続を受け入れる
    }
}
```

### 3. セキュリティ考慮事項

- **認証機能**: 任意のユーザーが接続できないよう、トークン認証を実装
- **接続数制限**: DoS攻撃を防ぐため、同時接続数を制限
- **コマンド検証**: 悪意のあるコマンドを防ぐための入力検証

## 🚀 推奨実装ステップ

### Phase 1: 設定可能なクライアント
```html
<!-- 接続設定UI -->
<div class="connection-settings">
    <input type="text" id="server-url" placeholder="ws://your-server:14711">
    <button onclick="saveServerUrl()">保存</button>
</div>
```

### Phase 2: 認証システム
```javascript
// 接続時に認証トークンを送信
const ws = new WebSocket(WS_URL);
ws.onopen = () => {
    ws.send(JSON.stringify({
        type: 'auth',
        token: authToken
    }));
};
```

### Phase 3: GitHub Actions でのビルド自動化
```yaml
name: Build and Deploy
on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build Scratch Extensions
        run: |
          cd scratch-extension
          npm install
          npm run build
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./public
```

## 📁 推奨ディレクトリ構造

```
minecraft-collaboration-project/
├── public/                    # GitHub Pages用
│   ├── index.html            # メインページ
│   ├── scratch-gui.html      # Scratch GUI
│   ├── js/                   # ビルド済みJS
│   └── assets/               # 画像等
├── minecraft-mod/            # Modソース（ユーザーがビルド）
├── scratch-extension/        # 拡張機能ソース
└── docs/                     # ドキュメント
```

## 🔧 ユーザー側の設定

### 1. Minecraft Modのインストール
```
1. Releases から minecraft-collaboration-mod-x.x.x.jar をダウンロード
2. Minecraft の mods フォルダに配置
3. Minecraft を起動
```

### 2. ポート開放（必要な場合）
- ルーターで 14711 ポートを開放
- またはローカルネットワーク内での利用

### 3. 接続
```
1. https://[your-github-username].github.io/minecraft-collaboration にアクセス
2. サーバーURLを入力（例: ws://192.168.1.100:14711）
3. 接続ボタンをクリック
```

## 🛡️ セキュリティベストプラクティス

1. **ローカルネットワーク限定**: 初期段階では LAN 内のみでの利用を推奨
2. **HTTPS/WSS**: 可能であれば WSS (WebSocket Secure) を使用
3. **アクセス制御**: IP ホワイトリストや認証トークンの実装
4. **ログ記録**: 接続とコマンドのログを保存

## 📱 レスポンシブデザイン

GitHub Pages で公開する際は、様々なデバイスでの利用を想定：

```css
/* モバイル対応 */
@media (max-width: 768px) {
    .scratch-container {
        width: 100%;
        height: auto;
    }
}
```

## 🔄 アップデート配信

GitHub Releases を使用した自動アップデート通知：

```javascript
// バージョンチェック
fetch('https://api.github.com/repos/[user]/[repo]/releases/latest')
    .then(res => res.json())
    .then(data => {
        if (data.tag_name > currentVersion) {
            showUpdateNotification();
        }
    });
```

## 📈 将来の拡張可能性

1. **WebRTC**: P2P接続でサーバー負荷軽減
2. **PWA**: オフライン対応
3. **多言語対応**: i18n の実装
4. **分析機能**: 学習進捗の可視化

このガイドに従って実装することで、安全で拡張性の高いシステムを GitHub Pages で公開できます。