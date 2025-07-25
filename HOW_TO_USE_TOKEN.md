# 🔐 GitHub Tokenの使用方法

## 方法1: コマンドプロンプトで直接入力（推奨）

```bash
git push -u origin main
```

実行すると以下のように表示されます：
```
Username for 'https://github.com': laughtale01
Password for 'https://laughtale01@github.com': 
```

ここで：
1. Username: `laughtale01` と入力してEnter
2. Password: トークン（ghp_xxxxx）を貼り付けてEnter
   ※入力時は画面に表示されません

## 方法2: PowerShellで環境変数を使用

```powershell
# PowerShellで実行
$env:GIT_TOKEN = "ghp_あなたのトークン"
git remote set-url origin https://laughtale01:$env:GIT_TOKEN@github.com/laughtale01/Scratch.git
git push -u origin main

# 成功後、セキュリティのためURLを戻す
git remote set-url origin https://github.com/laughtale01/Scratch.git
```

## 方法3: Git Bashを使用

Git Bashを開いて：
```bash
# Git Bashで実行
git push -u origin main
```
プロンプトが表示されたら、トークンを貼り付け

## 方法4: Windows資格情報マネージャーをリセット

1. **既存の認証情報をクリア**
   ```bash
   git config --global --unset credential.helper
   git config --global credential.helper manager
   ```

2. **もう一度プッシュ**
   ```bash
   git push -u origin main
   ```

3. **Windowsの認証ダイアログが表示されたら**
   - ユーザー名: laughtale01
   - パスワード: トークン（ghp_xxxxx）

## 方法5: 一時的にURLにトークンを含める

```bash
# トークンを含むURLを設定
git remote set-url origin https://laughtale01:ghp_あなたのトークン@github.com/laughtale01/Scratch.git

# プッシュ（認証不要）
git push -u origin main

# 成功後、必ずトークンを削除
git remote set-url origin https://github.com/laughtale01/Scratch.git
```

## ⚠️ 注意事項

- トークンは画面に表示されません（セキュリティのため）
- Ctrl+Vで貼り付けできます
- 右クリックで貼り付けもできます（コマンドプロンプトの場合）

## 🎯 最も簡単な方法

1. コマンドプロンプトまたはPowerShellで：
   ```
   git push -u origin main
   ```

2. 表示されるプロンプトで：
   - `Username for 'https://github.com':` → `laughtale01` と入力
   - `Password for 'https://laughtale01@github.com':` → トークンを貼り付け（表示されません）

トークンの貼り付け方法：
- **Ctrl + V**
- **右クリック**（コマンドプロンプト）
- **Shift + Insert**