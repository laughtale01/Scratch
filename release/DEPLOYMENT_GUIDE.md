# MinecraftEdu Scratch Controller - デプロイメントガイド

本番環境へのデプロイ手順を説明します。

---

## デプロイメント方法の選択

### 🏠 方法1: ローカル配布（教室・個人利用）

**対象**:
- 学校のPC教室
- 個人のPC
- 小規模グループ

**手順**:
1. リリースパッケージを各PCに配布
2. 各自でMODとScratch GUIをインストール
3. ローカルHTTPサーバーで実行

**メリット**:
- サーバー不要
- インターネット接続不要（オフライン可）
- シンプル

**デメリット**:
- 各PCに個別インストールが必要

---

### 🌐 方法2: Webサーバー配布（大規模利用）

**対象**:
- 複数の教室
- オンライン授業
- コミュニティ

**手順**:
1. Webサーバーを用意
2. Scratch GUIをホスティング
3. MODのみを各PCに配布
4. ユーザーにURLを共有

**メリット**:
- Scratch GUIの個別インストール不要
- アップデートが簡単
- アクセス管理可能

**デメリット**:
- Webサーバーが必要
- インターネット接続必要

---

## 方法2の詳細手順: Webサーバーデプロイ

### オプションA: GitHub Pagesでホスティング（無料）

#### 1. GitHubリポジトリを作成

```bash
cd /path/to/project
git init
git add .
git commit -m "Initial release"
git remote add origin https://github.com/your-username/minecraft-scratch-controller.git
git push -u origin main
```

#### 2. GitHub Pagesを有効化

1. GitHubリポジトリのSettingsを開く
2. 左メニューから「Pages」を選択
3. Sourceで「main」ブランチ、「/ (root)」を選択
4. 「Save」をクリック

#### 3. Scratch GUIをデプロイ

```bash
# buildフォルダをgh-pagesブランチにデプロイ
cd scratch-client/scratch-gui-official
npm run deploy
```

#### 4. アクセスURL

`https://your-username.github.io/minecraft-scratch-controller/`

---

### オプションB: Netlifyでホスティング（無料・推奨）

#### 1. Netlifyアカウント作成

[https://netlify.com](https://netlify.com)でアカウント作成

#### 2. サイトをデプロイ

**方法A: ドラッグ&ドロップ**
1. Netlifyにログイン
2. 「Add new site」→「Deploy manually」
3. `scratch-client/scratch-gui-official/build`フォルダをドロップ

**方法B: GitHub連携**
1. 「Add new site」→「Import from Git」
2. GitHubリポジトリを選択
3. ビルド設定:
   ```
   Base directory: scratch-client/scratch-gui-official
   Build command: npm run build
   Publish directory: build
   ```

#### 3. カスタムドメイン設定（オプション）

Netlifyの設定でカスタムドメインを追加可能

---

### オプションC: 自前サーバー（Apache/Nginx）

#### Apacheの場合

```bash
# Scratch GUIを展開
tar -xzf scratch-gui.tar.gz

# Apacheの公開ディレクトリにコピー
sudo cp -r build/* /var/www/html/minecraft-scratch/

# 権限設定
sudo chown -R www-data:www-data /var/www/html/minecraft-scratch
sudo chmod -R 755 /var/www/html/minecraft-scratch
```

#### Nginxの場合

```nginx
# /etc/nginx/sites-available/minecraft-scratch
server {
    listen 80;
    server_name minecraft-scratch.example.com;

    root /var/www/minecraft-scratch;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # WebSocket support
    location /ws {
        proxy_pass http://localhost:14711;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

```bash
# 設定を有効化
sudo ln -s /etc/nginx/sites-available/minecraft-scratch /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## Minecraft MODの配布

### 配布方法

#### オプション1: 直接配布

リリースパッケージの `minecraft-mod/minecraftedu-mod-0.1.0-1.20.1.jar` を配布

#### オプション2: ダウンロードリンク

Webサーバーに配置し、ダウンロードリンクを提供：

```html
<a href="/downloads/minecraftedu-mod-0.1.0-1.20.1.jar">
  MODをダウンロード
</a>
```

#### オプション3: CurseForge/Modrinth（将来的）

MOD配布プラットフォームにアップロード

---

## セキュリティ設定

### ファイアウォール設定（重要）

#### Windowsファイアウォール

```powershell
# ポート14711を許可（ローカルネットワークのみ）
New-NetFirewallRule -DisplayName "Minecraft Scratch Controller" `
    -Direction Inbound -LocalPort 14711 -Protocol TCP -Action Allow `
    -Profile Private
```

#### Linuxファイアウォール（ufw）

```bash
# ローカルネットワークからのみ許可
sudo ufw allow from 192.168.0.0/24 to any port 14711
```

### アクセス制限

**推奨設定**:
- ポート14711はローカルネットワークのみ
- 外部からのアクセスはブロック
- VPN経由での接続を検討

---

## アップデート手順

### Scratch GUIのアップデート

```bash
# 新しいビルドを作成
cd scratch-client/scratch-gui-official
npm run build

# Webサーバーに再デプロイ
# (GitHub Pages/Netlifyの場合は自動)

# 自前サーバーの場合
sudo cp -r build/* /var/www/html/minecraft-scratch/
```

### Minecraft MODのアップデート

1. 新しいJARファイルを作成
2. 古いバージョンを削除
3. 新しいバージョンを配布

---

## モニタリングとログ

### Minecraftサーバーログ

```bash
# ログファイルの場所
tail -f ~/.minecraft/logs/latest.log

# WebSocket関連のログを抽出
grep "WebSocket" ~/.minecraft/logs/latest.log
```

### Webサーバーアクセスログ

```bash
# Apache
tail -f /var/log/apache2/access.log

# Nginx
tail -f /var/log/nginx/access.log
```

---

## トラブルシューティング

### Scratch GUIが表示されない

1. ブラウザのキャッシュをクリア
2. 開発者ツール（F12）でエラー確認
3. HTTPSで配信している場合、Mixed Contentエラーを確認

### WebSocket接続エラー

1. Minecraftが起動しているか確認
2. ポート14711が開いているか確認（`netstat -an | grep 14711`）
3. ファイアウォール設定を確認

---

## バックアップ

### 定期バックアップ

```bash
#!/bin/bash
# backup.sh

DATE=$(date +%Y%m%d)
BACKUP_DIR="/backups/minecraft-scratch"

# Webコンテンツのバックアップ
tar -czf "$BACKUP_DIR/scratch-gui-$DATE.tar.gz" \
    /var/www/html/minecraft-scratch/

# MODのバックアップ
cp ~/.minecraft/mods/minecraftedu-mod-*.jar \
    "$BACKUP_DIR/mod-$DATE.jar"

# 7日以上古いバックアップを削除
find "$BACKUP_DIR" -name "*.tar.gz" -mtime +7 -delete
find "$BACKUP_DIR" -name "*.jar" -mtime +7 -delete
```

---

## サポート

デプロイメントに関する質問や問題は：
- GitHub Issues
- Discord/Slackコミュニティ
- ドキュメント: README.md, INSTALL.md参照

---

**デプロイメント成功を祈ります！** 🚀
