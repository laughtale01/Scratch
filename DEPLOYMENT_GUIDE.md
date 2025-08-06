# 📚 本番環境デプロイメントガイド

## 🎯 概要

このガイドでは、Minecraft×Scratchシステムを本番環境（クラウド）にデプロイする手順を説明します。

### システム構成
- **生徒側**: Minecraft（MOD導入済み）+ ブラウザ
- **サーバー側**: クラウド上のWebSocketサーバー + Scratch Webアプリ
- **通信**: インターネット経由でのWebSocket通信（WSS）

## 🚀 クイックスタート

### 1. サーバー環境の準備

#### 推奨スペック
- OS: Ubuntu 22.04 LTS
- CPU: 2コア以上
- メモリ: 4GB以上
- ストレージ: 20GB以上
- 固定IPアドレスまたはドメイン名

### 2. 必要なソフトウェアのインストール

```bash
# システム更新
sudo apt update && sudo apt upgrade -y

# Node.js インストール（v18 LTS）
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Nginx インストール
sudo apt install -y nginx

# PM2 インストール（プロセス管理）
sudo npm install -g pm2

# Git インストール
sudo apt install -y git
```

### 3. SSL証明書の取得（Let's Encrypt）

```bash
# Certbot インストール
sudo apt install -y certbot python3-certbot-nginx

# SSL証明書取得（your-domain.comを実際のドメインに置き換え）
sudo certbot --nginx -d your-domain.com -d www.your-domain.com
```

## 📦 アプリケーションのデプロイ

### 1. プロジェクトのクローン

```bash
cd /var/www
sudo git clone https://github.com/your-repo/minecraft-scratch.git
cd minecraft-scratch
```

### 2. WebSocketサーバーのセットアップ

```bash
# 依存関係インストール
cd cloud-server
npm install

# 環境変数設定
cat > .env << EOF
WS_PORT=14711
SSL_ENABLED=true
SSL_CERT_PATH=/etc/letsencrypt/live/your-domain.com/fullchain.pem
SSL_KEY_PATH=/etc/letsencrypt/live/your-domain.com/privkey.pem
CORS_ORIGIN=https://your-domain.com
MAX_CONNECTIONS=200
RATE_LIMIT=10
EOF

# PM2で起動
pm2 start websocket-server.js --name minecraft-ws
pm2 save
pm2 startup
```

### 3. Scratch Webアプリのビルドとデプロイ

```bash
# Scratchアプリのビルド
cd /var/www/minecraft-scratch/scratch-gui
npm install
npm run build

# ビルドファイルをNginxディレクトリにコピー
sudo cp -r build/* /var/www/html/
```

### 4. Nginx設定

```nginx
# /etc/nginx/sites-available/minecraft-scratch
server {
    listen 80;
    listen [::]:80;
    server_name your-domain.com www.your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name your-domain.com www.your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    root /var/www/html;
    index index.html;

    # Scratch Web App
    location / {
        try_files $uri $uri/ /index.html;
    }

    # WebSocket プロキシ
    location /ws {
        proxy_pass https://localhost:14711;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # タイムアウト設定
        proxy_connect_timeout 7d;
        proxy_send_timeout 7d;
        proxy_read_timeout 7d;
    }

    # 静的ファイルのキャッシュ
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

```bash
# 設定を有効化
sudo ln -s /etc/nginx/sites-available/minecraft-scratch /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 🎮 MODの本番環境対応

### 1. 設定ファイルの更新

`minecraft-collaboration-production.toml`:
```toml
[connection]
mode = "cloud"
server_url = "wss://your-domain.com/ws"
auto_connect = true

[authentication]
enabled = true
classroom_code = "CLASS001"  # 教室ごとに設定
```

### 2. MODの再ビルド

```bash
cd minecraft-mod

# 本番用設定でビルド
./gradlew clean build -Pproduction=true

# 生成されたJARファイル
# build/libs/minecraft-collaboration-mod-1.0.0-production.jar
```

### 3. 配布準備

```
配布パッケージ/
├── minecraft-collaboration-mod-1.0.0-production.jar
├── インストール方法.pdf
├── 使い方ガイド.pdf
└── config/
    └── minecraft-collaboration.toml (本番設定済み)
```

## 🔒 セキュリティ設定

### ファイアウォール設定

```bash
# UFW設定
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 14711/tcp # WebSocket
sudo ufw enable
```

### 教室コード管理

```javascript
// classroom-codes.json
{
  "CLASS001": {
    "name": "3年A組",
    "teacher": "山田先生",
    "maxStudents": 30,
    "validUntil": "2025-12-31"
  },
  "CLASS002": {
    "name": "3年B組",
    "teacher": "鈴木先生",
    "maxStudents": 30,
    "validUntil": "2025-12-31"
  }
}
```

## 📊 モニタリング

### PM2 モニタリング

```bash
# プロセス状態確認
pm2 status

# ログ確認
pm2 logs minecraft-ws

# モニタリングダッシュボード
pm2 monit
```

### Nginxアクセスログ

```bash
# アクセスログ
tail -f /var/log/nginx/access.log

# エラーログ
tail -f /var/log/nginx/error.log
```

### システムリソース

```bash
# リソース使用状況
htop

# ディスク使用状況
df -h

# メモリ使用状況
free -h
```

## 🔄 メンテナンス

### アップデート手順

```bash
# コードの更新
cd /var/www/minecraft-scratch
git pull origin main

# 依存関係の更新
npm install

# Scratchの再ビルド
cd scratch-gui
npm run build
sudo cp -r build/* /var/www/html/

# WebSocketサーバーの再起動
pm2 restart minecraft-ws
```

### バックアップ

```bash
# 自動バックアップスクリプト
#!/bin/bash
BACKUP_DIR="/backup/minecraft-scratch"
DATE=$(date +%Y%m%d_%H%M%S)

# アプリケーションバックアップ
tar -czf "$BACKUP_DIR/app_$DATE.tar.gz" /var/www/minecraft-scratch

# 設定ファイルバックアップ
tar -czf "$BACKUP_DIR/config_$DATE.tar.gz" /etc/nginx/sites-available

# 古いバックアップを削除（30日以上）
find $BACKUP_DIR -type f -mtime +30 -delete
```

## 📝 トラブルシューティング

### よくある問題と解決方法

| 問題 | 原因 | 解決方法 |
|------|------|----------|
| WebSocket接続失敗 | SSL証明書の問題 | 証明書の更新: `sudo certbot renew` |
| 高負荷 | 同時接続数が多い | サーバースペックの増強 |
| Scratch が表示されない | ビルドエラー | `npm run build` を再実行 |
| MODが接続できない | ファイアウォール | ポート14711が開いているか確認 |

### ログの確認方法

```bash
# WebSocketサーバーログ
pm2 logs minecraft-ws --lines 100

# Nginxエラーログ
sudo tail -f /var/log/nginx/error.log

# システムログ
sudo journalctl -xe
```

## 📋 チェックリスト

### デプロイ前
- [ ] ドメイン名の取得
- [ ] サーバーの準備
- [ ] SSL証明書の取得
- [ ] ファイアウォール設定

### デプロイ時
- [ ] アプリケーションのインストール
- [ ] 環境変数の設定
- [ ] Nginx設定
- [ ] PM2での起動

### デプロイ後
- [ ] WebSocket接続テスト
- [ ] Scratch動作確認
- [ ] MOD接続テスト
- [ ] モニタリング設定

## 🚨 緊急時対応

### サービス再起動

```bash
# すべてのサービスを再起動
sudo systemctl restart nginx
pm2 restart all

# 個別再起動
pm2 restart minecraft-ws
sudo systemctl reload nginx
```

### ロールバック

```bash
# 前のバージョンに戻す
cd /var/www/minecraft-scratch
git checkout [前のコミットハッシュ]
npm install
pm2 restart minecraft-ws
```

---

**作成日**: 2025-08-04  
**対象環境**: Ubuntu 22.04 LTS  
**必要な権限**: sudo権限のあるユーザー