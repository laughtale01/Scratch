# 🌐 GitHub Pages デプロイガイド

## 📋 目次
- [クイックスタート](#クイックスタート)
- [カスタムドメイン設定](#カスタムドメイン設定)
- [自動デプロイ設定](#自動デプロイ設定)
- [トラブルシューティング](#トラブルシューティング)

---

## 🚀 クイックスタート

### 前提条件
- GitHubアカウント
- パブリックリポジトリ（プライベートは有料プランが必要）

### 手順

#### 1. リポジトリをパブリックに変更
```bash
# GitHubでリポジトリ設定を開く
Settings → General → Danger Zone → Change visibility → Change to public
```

#### 2. GitHub Pages を有効化
1. リポジトリの **Settings** タブを開く
2. 左サイドバーの **Pages** をクリック
3. **Source** セクションで設定：
   - Source: `Deploy from a branch`
   - Branch: `main`
   - Folder: `/docs`
4. **Save** をクリック

#### 3. 公開URLを確認
```
https://[username].github.io/minecraft_collaboration_project/
```

デプロイ完了まで最大10分かかります。

---

## 🎨 Jekyll テーマ設定

### シンプルなテーマ適用
`docs/_config.yml`:
```yaml
theme: jekyll-theme-cayman
title: Minecraft×Scratch 協調学習システム
description: プログラミングで協力して建築しよう！
```

### カスタムレイアウト
`docs/_layouts/default.html`:
```html
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ page.title | default: site.title }}</title>
    <link rel="stylesheet" href="{{ '/assets/css/style.css' | relative_url }}">
</head>
<body>
    <header>
        <h1>{{ site.title }}</h1>
        <p>{{ site.description }}</p>
    </header>
    
    <main>
        {{ content }}
    </main>
    
    <footer>
        <p>&copy; 2025 {{ site.title }}</p>
    </footer>
</body>
</html>
```

---

## 🌍 カスタムドメイン設定

### 1. CNAMEファイルの作成
`docs/CNAME`:
```
minecraft.example.com
```

### 2. DNS設定
ドメインプロバイダーで以下を設定：

#### Apex ドメイン（example.com）
```
Type: A
Host: @
Value: 185.199.108.153
       185.199.109.153
       185.199.110.153
       185.199.111.153
```

#### サブドメイン（minecraft.example.com）
```
Type: CNAME
Host: minecraft
Value: [username].github.io
```

### 3. HTTPS を強制
Settings → Pages → Enforce HTTPS にチェック

---

## 🔄 自動デプロイ設定

### GitHub Actions ワークフロー
`.github/workflows/deploy.yml`:
```yaml
name: Deploy to GitHub Pages

on:
  push:
    branches: [ main ]
    paths:
      - 'docs/**'
      - '.github/workflows/deploy.yml'

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        
      - name: Setup Pages
        uses: actions/configure-pages@v4
        
      - name: Build with Jekyll
        uses: actions/jekyll-build-pages@v1
        with:
          source: ./docs
          destination: ./_site
          
      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3

  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    needs: build
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

---

## 📁 推奨ディレクトリ構造

```
docs/
├── index.html              # メインページ
├── _config.yml            # Jekyll設定
├── CNAME                  # カスタムドメイン（オプション）
├── assets/
│   ├── css/
│   │   └── style.css     # カスタムスタイル
│   ├── js/
│   │   └── minecraft-extension.js
│   └── images/
│       └── logo.png
├── _layouts/              # カスタムレイアウト
│   └── default.html
└── _includes/             # 再利用可能なコンポーネント
    ├── header.html
    └── footer.html
```

---

## 🎯 最適化テクニック

### 1. 軽量化
```html
<!-- 最小限のCSS -->
<style>
body { font-family: sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
.scratch-embed { width: 100%; height: 600px; border: 1px solid #ccc; }
</style>
```

### 2. CDN活用
```html
<!-- Scratch拡張機能をjsDelivrから読み込み -->
<script src="https://cdn.jsdelivr.net/gh/[username]/minecraft_collaboration_project@main/docs/minecraft-extension.js"></script>
```

### 3. プログレッシブエンハンスメント
```javascript
// 機能検出
if ('WebSocket' in window) {
    // WebSocket対応ブラウザでのみ有効化
    initializeMinecraftExtension();
} else {
    showFallbackMessage();
}
```

---

## 🔧 トラブルシューティング

### 問題: ページが404エラー
**解決方法:**
1. リポジトリがパブリックか確認
2. GitHub Pages が有効か確認
3. ブランチとフォルダ設定を確認
4. `index.html` が存在するか確認

### 問題: 更新が反映されない
**解決方法:**
1. GitHub Actions の実行状況を確認
2. ブラウザキャッシュをクリア（Ctrl+F5）
3. CDNキャッシュをパージ:
   ```
   https://purge.jsdelivr.net/gh/[username]/[repo]@main/[file]
   ```

### 問題: カスタムドメインが機能しない
**解決方法:**
1. DNS設定が反映されるまで待つ（最大48時間）
2. CNAMEファイルの内容を確認
3. HTTPS設定を確認
4. `dig` コマンドでDNS確認:
   ```bash
   dig minecraft.example.com
   ```

### 問題: Jekyllビルドエラー
**解決方法:**
1. `_config.yml` の構文エラーを確認
2. ローカルでテスト:
   ```bash
   bundle exec jekyll serve --source docs
   ```
3. Gemfileを追加:
   ```ruby
   source "https://rubygems.org"
   gem "github-pages", group: :jekyll_plugins
   ```

---

## 📊 アクセス解析

### GitHub Insights
- リポジトリ → Insights → Traffic
- ページビュー、ユニークビジター、参照元を確認

### Google Analytics 連携
`docs/_includes/analytics.html`:
```html
<!-- Google tag (gtag.js) -->
<script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-XXXXXXXXXX');
</script>
```

---

## 🔒 セキュリティ考慮事項

### 1. センシティブ情報の除外
`.gitignore`:
```
# APIキーなど
.env
config.local.js
**/secrets/
```

### 2. CORS設定
必要に応じて `docs/.nojekyll` ファイルを作成してJekyll処理をスキップ

### 3. コンテンツセキュリティポリシー
`docs/index.html`:
```html
<meta http-equiv="Content-Security-Policy" 
      content="default-src 'self'; script-src 'self' https://cdn.jsdelivr.net">
```

---

## 💡 ベストプラクティス

1. **ブランチ保護**: main ブランチへの直接プッシュを制限
2. **プルリクエスト**: 変更は PR 経由でレビュー
3. **自動テスト**: デプロイ前にリンクチェッカーを実行
4. **バージョニング**: タグを使用してリリース管理

---

## 📚 参考リンク

- [GitHub Pages 公式ドキュメント](https://docs.github.com/pages)
- [Jekyll ドキュメント](https://jekyllrb.com/docs/)
- [GitHub Actions ドキュメント](https://docs.github.com/actions)

---

## 🆚 Firebase Hosting との比較

| 機能 | GitHub Pages | Firebase Hosting |
|------|-------------|------------------|
| 料金 | 無料（パブリックリポジトリ） | 無料枠あり |
| カスタムドメイン | ✅ | ✅ |
| HTTPS | ✅（自動） | ✅（自動） |
| ビルドプロセス | Jekyll標準 | 任意 |
| 動的機能 | ❌ | ✅（Functions） |
| アクセス制限 | ❌ | ✅ |
| デプロイ速度 | 中（1-10分） | 速（1-2分） |

**推奨**: 
- シンプルな静的サイト → GitHub Pages
- 高度な機能が必要 → Firebase Hosting