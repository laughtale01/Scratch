# 既存リポジトリを新プロジェクトに置き換える手順

## 目的

既存の https://github.com/laughtale01/Scratch を新しいプロジェクトで完全に置き換えます。

---

## 方法A: 履歴を完全にリセット（推奨）

既存の履歴をすべて削除して、新しいプロジェクトとして開始します。

### ステップ1: リモートリポジトリを追加

```bash
cd "D:\minecraft laughtare project"

# 既存のリポジトリをリモートとして追加
git remote add origin https://github.com/laughtale01/Scratch.git
```

### ステップ2: 強制プッシュで置き換え

```bash
# 既存の内容を完全に上書き（注意: 元のデータは失われます）
git push -f origin main
```

**⚠️ 警告**: これにより既存のリポジトリの内容は完全に削除されます！

### ステップ3: GitHubで確認

https://github.com/laughtale01/Scratch にアクセスして、新しいプロジェクトの内容が表示されることを確認してください。

---

## 方法B: リポジトリ名を変更して保存

既存のプロジェクトを保存したい場合は、リポジトリ名を変更してからアーカイブします。

### ステップ1: 既存リポジトリのリネーム

1. https://github.com/laughtale01/Scratch にアクセス
2. Settings → General
3. Repository name を `Scratch-old` または `Scratch-archived` に変更
4. "Rename" をクリック

### ステップ2: アーカイブ（オプション）

1. Settings → 下にスクロール
2. "Archive this repository" をクリック

### ステップ3: 新しいリポジトリを作成

1. GitHub → New repository
2. Repository name: `Scratch`
3. Public/Private を選択
4. "Create repository" をクリック

### ステップ4: ローカルからプッシュ

```bash
cd "D:\minecraft laughtare project"
git remote add origin https://github.com/laughtale01/Scratch.git
git push -u origin main
```

---

## 方法C: 新しいブランチで置き換え

既存のリポジトリに新しいブランチを作成して、後でデフォルトブランチを切り替えます。

### ステップ1: リモートを追加してプッシュ

```bash
cd "D:\minecraft laughtare project"
git remote add origin https://github.com/laughtale01/Scratch.git

# 新しいブランチ名でプッシュ
git push -u origin main:project-redesign
```

### ステップ2: GitHub上でデフォルトブランチを変更

1. https://github.com/laughtale01/Scratch にアクセス
2. Settings → Branches
3. Default branch を `project-redesign` に変更
4. "Update" をクリック

### ステップ3: 古いブランチを削除（オプション）

```bash
# GitHub上で古い main や master ブランチを削除
# Settings → Branches → ブランチ名の横のゴミ箱アイコン
```

---

## トラブルシューティング

### エラー: `! [rejected] main -> main (fetch first)`

既存のリポジトリに内容がある場合、このエラーが出ます。

**解決策**: 強制プッシュを使用
```bash
git push -f origin main
```

**⚠️ 注意**: `-f`（force）オプションは既存のデータを上書きします！

### エラー: `remote: Permission denied`

認証が必要です。

**解決策**: Personal Access Token (PAT) を使用
1. GitHub → Settings → Developer settings → Personal access tokens
2. "Generate new token (classic)"
3. `repo` 権限を選択
4. トークンをコピー
5. プッシュ時にパスワードとして使用

---

## 推奨する方法

**状況に応じて選択してください**:

| 状況 | 推奨方法 |
|------|---------|
| 旧プロジェクトは不要、完全に置き換えたい | **方法A** |
| 旧プロジェクトを念のため保存しておきたい | **方法B** |
| 段階的に移行したい | 方法C |

---

## 次のステップ

リポジトリの置き換えが完了したら：

1. **takecxさんのリポジトリをFork**
   - scratch-vm
   - scratch-gui
   - RemoteControllerMod

2. **サブモジュールとして追加**
   ```bash
   git submodule add https://github.com/laughtale01/scratch-vm.git scratch-client/scratch-vm
   git submodule add https://github.com/laughtale01/scratch-gui.git scratch-client/scratch-gui
   git submodule add https://github.com/laughtale01/RemoteControllerMod.git minecraft-mod/RemoteControllerMod

   git add .
   git commit -m "Add forked repositories as submodules"
   git push
   ```

3. **開発環境のセットアップ**
   - `docs/SETUP_GUIDE.md` を参照

---

**作成日**: 2025-11-12
