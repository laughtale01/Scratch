# GitHub リポジトリセットアップガイド

## 現在の状態

✅ ローカルGitリポジトリ初期化完了
✅ 初期コミット作成完了（9ファイル、5402行）
✅ ブランチ名を `main` に設定完了

---

## 次のステップ

### ステップ1: GitHubで新しいリポジトリを作成

1. **GitHubにログイン**
   - https://github.com にアクセス

2. **新しいリポジトリを作成**
   - 右上の「+」アイコン → "New repository" をクリック

3. **リポジトリ情報を入力**
   ```
   Repository name: minecraftedu-scratch-controller
   Description: Educational Minecraft controller with Scratch 3.0 - Multiplayer support & Tutorial system

   ☑ Public（または Private - お好みで）
   ☐ Add a README file（チェックしない - 既に作成済み）
   ☐ Add .gitignore（チェックしない - 既に作成済み）
   ☐ Choose a license（後で追加するのでスキップ）
   ```

4. **"Create repository" をクリック**

---

### ステップ2: リモートリポジトリの追加とプッシュ

GitHubでリポジトリを作成したら、以下のコマンドを実行してください：

#### Windows (Git Bash または PowerShell)

```bash
cd "D:\minecraft laughtare project"

# リモートリポジトリを追加（YOUR_USERNAMEを自分のGitHubユーザー名に置き換え）
git remote add origin https://github.com/YOUR_USERNAME/minecraftedu-scratch-controller.git

# プッシュ
git push -u origin main
```

#### 初回プッシュ時に認証を求められた場合

**Personal Access Token (PAT) を使用（推奨）**:

1. GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. "Generate new token (classic)" をクリック
3. 以下の権限を選択：
   - ☑ repo（すべて）
   - ☑ workflow
4. トークンを生成してコピー（**重要**: 一度しか表示されません）
5. コマンドプロンプトでユーザー名とトークンを入力：
   - Username: `あなたのGitHubユーザー名`
   - Password: `生成したトークン`

---

### ステップ3: プッシュの確認

```bash
# プッシュが成功すると、以下のような出力が表示されます
Enumerating objects: 13, done.
Counting objects: 100% (13/13), done.
Delta compression using up to 8 threads
Compressing objects: 100% (12/12), done.
Writing objects: 100% (13/13), X.XX KiB | X.XX MiB/s, done.
Total 13 (delta 0), reused 0 (delta 0), pack-reused 0
To https://github.com/YOUR_USERNAME/minecraftedu-scratch-controller.git
 * [new branch]      main -> main
Branch 'main' set up to track remote branch 'main' from 'origin'.
```

GitHubのリポジトリページをブラウザで開いて、ファイルがアップロードされていることを確認してください！

---

### ステップ4: takecxさんのリポジトリをFork

次に、ベースとなるリポジトリを自分のアカウントにforkします：

#### 4-1. Scratch VM をFork

1. https://github.com/takecx/scratch-vm にアクセス
2. 右上の「Fork」ボタンをクリック
3. 自分のアカウントを選択
4. "Create fork" をクリック

#### 4-2. Scratch GUI をFork

1. https://github.com/takecx/scratch-gui にアクセス
2. 同様に「Fork」

#### 4-3. RemoteControllerMod をFork

1. https://github.com/takecx/RemoteControllerMod にアクセス
2. 同様に「Fork」

---

### ステップ5: サブモジュールとして追加

forkが完了したら、メインプロジェクトにサブモジュールとして追加します：

```bash
cd "D:\minecraft laughtare project"

# Scratch VM を追加
git submodule add https://github.com/YOUR_USERNAME/scratch-vm.git scratch-client/scratch-vm

# Scratch GUI を追加
git submodule add https://github.com/YOUR_USERNAME/scratch-gui.git scratch-client/scratch-gui

# RemoteControllerMod を追加
git submodule add https://github.com/YOUR_USERNAME/RemoteControllerMod.git minecraft-mod/RemoteControllerMod

# サブモジュールをコミット
git add .gitmodules scratch-client/ minecraft-mod/
git commit -m "Add forked repositories as submodules

- scratch-vm: Scratch virtual machine (forked from takecx)
- scratch-gui: Scratch GUI (forked from takecx)
- RemoteControllerMod: Minecraft Forge MOD (forked from takecx)"

# プッシュ
git push
```

---

### ステップ6: サブモジュールの初期化とビルド

```bash
# サブモジュールを初期化して内容を取得
git submodule update --init --recursive

# Scratch VM のセットアップ
cd scratch-client/scratch-vm
git checkout develop  # develop ブランチを使用
npm install

# Scratch GUI のセットアップ
cd ../scratch-gui
npm install
npm link ../scratch-vm

# RemoteControllerMod のビルド
cd ../../minecraft-mod/RemoteControllerMod
./gradlew build  # または gradlew.bat build (Windows)
```

---

## リポジトリ構成の確認

正しくセットアップされると、以下のような構成になります：

```
GitHub上:
├── YOUR_USERNAME/minecraftedu-scratch-controller (メインリポジトリ)
├── YOUR_USERNAME/scratch-vm (fork)
├── YOUR_USERNAME/scratch-gui (fork)
└── YOUR_USERNAME/RemoteControllerMod (fork)

ローカル:
D:\minecraft laughtare project\
├── .git/                          (メインリポジトリ)
├── docs/                          (ドキュメント)
├── scratch-client/
│   ├── scratch-vm/                (サブモジュール → YOUR_USERNAME/scratch-vm)
│   └── scratch-gui/               (サブモジュール → YOUR_USERNAME/scratch-gui)
├── minecraft-mod/
│   └── RemoteControllerMod/       (サブモジュール → YOUR_USERNAME/RemoteControllerMod)
└── ...
```

---

## トラブルシューティング

### Q: `git push` で認証エラーが出る

**A**: Personal Access Token (PAT) を使用してください（上記参照）

### Q: サブモジュールが空になっている

**A**: 以下を実行してください：
```bash
git submodule update --init --recursive
```

### Q: fork したリポジトリの upstream を設定したい

**A**: 元のリポジトリを upstream として追加：
```bash
cd scratch-client/scratch-vm
git remote add upstream https://github.com/takecx/scratch-vm.git
git fetch upstream
```

---

## 次のドキュメント

セットアップが完了したら、以下のドキュメントを読んで開発を開始してください：

1. **GETTING_STARTED.md** - プロジェクト開始ガイド
2. **docs/SETUP_GUIDE.md** - 開発環境の詳細セットアップ
3. **PROJECT_DESIGN.md** - プロジェクト全体の設計

---

## チェックリスト

- [ ] GitHubで新しいリポジトリを作成
- [ ] `git remote add origin` でリモートを追加
- [ ] `git push -u origin main` でプッシュ
- [ ] GitHubでリポジトリを確認
- [ ] takecxさんの3つのリポジトリをfork
- [ ] サブモジュールとして追加
- [ ] サブモジュールを初期化
- [ ] ビルド確認

---

**作成日**: 2025-11-12
**次のステップ**: GitHubでリポジトリを作成してください！
