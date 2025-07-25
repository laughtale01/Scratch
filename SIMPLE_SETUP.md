# 🎮 Minecraft × Scratch シンプルセットアップ

## たった3つのコンポーネント！

takecxの実装と同じように、以下の3つだけで動作します：

### 1️⃣ Webページ（Scratch GUI）
- **URL**: `https://[username].github.io/minecraft-collaboration-project/`
- ブラウザで開くだけ！インストール不要

### 2️⃣ 専用Mod
- **ファイル**: `minecraft-collaboration-mod-1.0.0-all.jar`
- Minecraftのmodsフォルダに入れるだけ

### 3️⃣ Minecraft
- **バージョン**: Java版 1.20.1 + Forge 47.2.0
- 通常通り起動するだけ

## 🚀 かんたん3ステップ

### ステップ1: Modをダウンロード
```
minecraft-collaboration-mod-1.0.0-all.jar
```
これを `.minecraft/mods/` フォルダにコピー

### ステップ2: Minecraftを起動
Forge 1.20.1で起動（WebSocketサーバーが自動起動）

### ステップ3: Webページを開く
ブラウザで Scratch GUI を開いて、拡張機能から「Minecraft」を選択

## ✨ 完了！

これだけで、ScratchからMinecraftを制御できます！

---

### 📝 開発者向け情報

#### ローカルでテスト
```batch
# すべて起動
start-all.bat
```

#### GitHub Pagesへデプロイ
```batch
# 準備
cd scratch-gui-deploy
prepare-github-pages.bat

# プッシュ
git add docs/
git commit -m "Deploy Scratch GUI"
git push
```

#### Modのビルド
```batch
cd minecraft-mod
.\gradlew.bat build
```