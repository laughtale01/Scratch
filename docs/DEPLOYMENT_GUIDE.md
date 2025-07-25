# 🚀 3コンポーネント デプロイメントガイド

## 概要

このプロジェクトは、takecxの実装と同様に以下の3つのコンポーネントで構成されます：

1. **Webページ** - GitHub Pagesでホストされる Scratch GUI
2. **専用Mod** - minecraft-collaboration-mod.jar
3. **Minecraft** - Java版 1.20.1 + Forge

## 🌐 コンポーネント1: Webページ (Scratch GUI)

### GitHub Pages へのデプロイ

1. **準備スクリプトの実行**
   ```batch
   cd scratch-gui-deploy
   prepare-github-pages.bat
   ```

2. **GitHub設定の更新**
   `update-extension-url.js` を編集:
   ```javascript
   const GITHUB_USERNAME = 'あなたのユーザー名';
   const REPO_NAME = 'minecraft-collaboration-project';
   ```

3. **再度スクリプトを実行**
   ```batch
   node update-extension-url.js
   ```

4. **GitHubにプッシュ**
   ```bash
   git add docs/
   git commit -m "Add Scratch GUI for GitHub Pages"
   git push origin main
   ```

5. **GitHub Pages を有効化**
   - リポジトリの Settings → Pages
   - Source: Deploy from a branch
   - Branch: main, /docs

### アクセスURL
```
https://[YOUR_USERNAME].github.io/[REPO_NAME]/
```

## 🎮 コンポーネント2: 専用Mod

### ビルド
```batch
cd minecraft-mod
.\gradlew.bat clean build
```

### 生成ファイル
```
minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar
```

### 配布方法
- GitHub Releases でリリース
- 直接ダウンロードリンクを提供

## ⛏️ コンポーネント3: Minecraft環境

### 必要環境
- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.2.0

### インストール手順
1. Minecraft Forge をインストール
2. `.minecraft/mods/` フォルダに専用Modをコピー
3. Minecraft を起動

## 📋 使用手順（エンドユーザー向け）

### 簡単3ステップ

1. **Modをインストール**
   - `minecraft-collaboration-mod-1.0.0-all.jar` を mods フォルダへ

2. **Minecraftを起動**
   - Forge 1.20.1 で起動
   - WebSocketサーバーが自動的に開始

3. **Webページを開く**
   - `https://[YOUR_USERNAME].github.io/[REPO_NAME]/`
   - 拡張機能から「Minecraft」を選択

## 🔧 カスタマイズ

### 独自ドメインの使用
1. `docs/CNAME` ファイルを作成
2. ドメイン名を記入
3. DNSを設定

### 拡張機能の更新
1. `scratch-extension/src/index.js` を編集
2. ビルド: `npm run build`
3. `docs/minecraft-extension.js` を更新
4. GitHubにプッシュ

## 📦 リリースチェックリスト

- [ ] Mod のバージョン番号を更新
- [ ] 拡張機能をビルド
- [ ] GitHub Pages用ファイルを生成
- [ ] READMEを更新
- [ ] GitHub Releasesで公開
- [ ] ダウンロードリンクをテスト

## 🌟 メリット

1. **シンプルな構成** - 3つのコンポーネントのみ
2. **簡単な配布** - Modファイル1つとWebページURL
3. **インストール不要** - Scratch GUIはWeb上で動作
4. **クロスプラットフォーム** - Windows/Mac/Linux対応
5. **更新が簡単** - GitHub経由で自動更新