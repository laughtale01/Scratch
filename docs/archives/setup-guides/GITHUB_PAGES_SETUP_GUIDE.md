# 🌐 GitHub Pages 設定ガイド

## 🎯 目標
リポジトリの `docs/index.html` をGitHub Pagesで公開して、以下のURLでアクセス可能にする：
```
https://laughtale01.github.io/Scratch/
```

---

## 📋 設定手順（5分で完了）

### ステップ1️⃣: GitHubリポジトリにアクセス

1. **ブラウザで以下のURLを開く**
   ```
   https://github.com/laughtale01/Scratch
   ```

2. **ログイン確認**
   - GitHubアカウントでログインしていることを確認
   - ログインしていない場合は、右上の「Sign in」をクリック

### ステップ2️⃣: Settings（設定）ページに移動

1. **リポジトリのメニューバーで「Settings」をクリック**
   ```
   Code | Issues | Pull requests | Actions | Projects | Wiki | Security | Insights | Settings
                                                                                        ↑ここ
   ```

2. **左サイドバーから「Pages」を選択**
   ```
   General
   Access
   Code and automation
   ├ Actions
   ├ Webhooks
   ├ Environments
   └ Pages  ← ここをクリック
   ```

### ステップ3️⃣: GitHub Pages を有効化

1. **「Source」セクションで設定**
   - **Deploy from a branch** を選択（デフォルト）

2. **「Branch」セクションで設定**
   - **Branch**: `main` を選択
   - **Folder**: `/ docs` を選択
   ```
   Branch: [main ▼] [/ docs ▼]
   ```

3. **「Save」ボタンをクリック**
   - 設定が保存され、ビルドが開始されます

### ステップ4️⃣: 公開確認

1. **ビルド完了を待つ（1-2分）**
   - ページ上部に緑色のチェックマークが表示されます
   - 「Your site is live at https://laughtale01.github.io/Scratch/」というメッセージが表示

2. **URLにアクセスして確認**
   ```
   https://laughtale01.github.io/Scratch/
   ```

---

## 🖼️ 画面イメージ

### Settings > Pages画面
```
┌─────────────────────────────────────────────────────────┐
│ GitHub Pages                                            │
├─────────────────────────────────────────────────────────┤
│ ✅ Your site is published at                            │
│    https://laughtale01.github.io/Scratch/              │
│                                                         │
│ Source                                                  │
│ ○ GitHub Actions                                        │
│ ● Deploy from a branch                                  │
│                                                         │
│ Branch                                                  │
│ [main ▼] [/ docs ▼] [Save]                            │
│                                                         │
│ Custom domain                                           │
│ [                    ] [Save]                          │
└─────────────────────────────────────────────────────────┘
```

---

## ⚠️ トラブルシューティング

### 問題1: 「Settings」タブが見つからない
**原因**: 権限不足またはリポジトリが見つからない
**解決法**: 
1. 正しいリポジトリ（laughtale01/Scratch）にアクセスしているか確認
2. リポジトリのオーナーとしてログインしているか確認
3. ブラウザを再読み込み

### 問題2: 「Pages」メニューが見つからない
**原因**: プライベートリポジトリでGitHub Pages無効
**解決法**: 
1. リポジトリを「Public」に変更
2. または GitHub Pro アカウントを使用

### 問題3: 「404 Not Found」エラー
**原因**: ビルドが未完了または設定ミス
**解決法**: 
1. 5-10分待ってから再度アクセス
2. `main` ブランチ、`/ docs` フォルダーが正しく設定されているか確認
3. `docs/index.html` ファイルが存在するか確認

### 問題4: ページは開くが拡張機能が動作しない
**原因**: WebSocketがHTTPS環境で制限される場合
**解決法**: 
1. ローカル版（`start-minecraft-scratch.bat`）を使用
2. またはブラウザの設定でHTTPS mixed contentを許可

---

## 🎉 成功後の確認項目

### ✅ チェックリスト
- [ ] https://laughtale01.github.io/Scratch/ にアクセス可能
- [ ] 「🎮 Minecraft × Scratch」ページが表示される
- [ ] 「📦 Minecraft拡張機能を読み込む」ボタンが表示される
- [ ] Minecraftを起動した状態で拡張機能が動作する

### 🌟 公開完了！
設定が成功すると、世界中どこからでも以下の手順でMinecraft×Scratchプログラミングが楽しめます：

1. **Minecraft起動** → Java版でワールドに入る
2. **ページアクセス** → https://laughtale01.github.io/Scratch/
3. **拡張機能読み込み** → ボタンクリックで即開始！

---

## 📞 サポート

問題が解決しない場合：
1. GitHub公式ドキュメント: https://docs.github.com/pages
2. ブラウザの開発者ツールでエラー確認
3. リポジトリのActionsタブでビルド状況確認

**素晴らしいMinecraft×Scratchプログラミング体験をお楽しみください！** 🎮✨