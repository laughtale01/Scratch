# MinecraftEdu Scratch Controller - リリースパッケージ

**バージョン**: 1.0.0
**リリース日**: 2025-11-14
**プラットフォーム**: Windows / macOS / Linux

---

## パッケージ内容

このリリースパッケージには、MinecraftEdu Scratch Controllerを実行するために必要なすべてのファイルが含まれています。

### ファイル構成

```
minecraft-scratch-controller/
│
├── minecraft-mod/                           Minecraft MOD
│   └── minecraftedu-mod-0.1.0-1.20.1.jar   MODファイル (23KB)
│
├── scratch-gui.tar.gz                       Scratch GUI (圧縮済み, ~60MB)
│
├── INSTALL.md                               インストール手順
├── RELEASE_NOTES.md                         リリースノート
├── README.md                                プロジェクト概要
└── README_RELEASE.md                        このファイル
```

---

## クイックスタート

### 1. Minecraft MODをインストール

```bash
# Minecraft modsフォルダにコピー
cp minecraft-mod/minecraftedu-mod-0.1.0-1.20.1.jar ~/.minecraft/mods/
```

### 2. Scratch GUIを展開

```bash
# 圧縮ファイルを展開
tar -xzf scratch-gui.tar.gz

# または、解凍ソフトで展開（Windows: 7-Zip, WinRAR等）
```

### 3. Scratch GUIをWebサーバーにデプロイ

#### オプションA: ローカルでテスト

```bash
cd build
python -m http.server 8601
# ブラウザで http://localhost:8601/ を開く
```

#### オプションB: Webサーバーにアップロード

buildフォルダの内容を、Webサーバーの公開ディレクトリにアップロードします。

---

## 配布方法の選択肢

Scratch GUIは複数の方法で配布できます：

### 方法1: ローカルHTTPサーバー（開発・テスト向け）

**利点**:
- インストール不要
- すぐに試せる

**手順**:
1. `scratch-gui.tar.gz`を展開
2. `build`フォルダで簡易サーバーを起動
3. ブラウザでアクセス

### 方法2: Webサーバーでホスティング（本番環境向け）

**利点**:
- 複数ユーザーで共有可能
- 高速アクセス

**推奨サーバー**:
- Apache
- Nginx
- GitHub Pages
- Netlify / Vercel

**手順**:
1. `scratch-gui.tar.gz`を展開
2. `build`フォルダの内容をWebサーバーにアップロード
3. ユーザーにURLを共有

### 方法3: Electronアプリ化（将来の拡張）

デスクトップアプリとして配布する場合は、Electronでパッケージ化できます（現在は未実装）。

---

## システム要件

### Minecraftクライアント側
- Minecraft Java Edition 1.20.1
- Minecraft Forge 1.20.1
- Java 17以上

### Scratch GUI側
- モダンWebブラウザ（Chrome 90+, Firefox 88+, Edge 90+）
- JavaScript有効
- WebSocket対応

### サーバー側（Webホスティングの場合）
- 静的ファイルをホスティングできる環境
- HTTPS推奨（WebSocket接続の安定性向上）

---

## セキュリティに関する注意

### 重要な警告

1. **ポート14711を外部に公開しない**
   - このMODはローカルネットワーク内での使用を想定
   - インターネット経由の接続はセキュリティリスクあり

2. **認証機能は未実装**
   - 誰でも接続できる状態
   - 信頼できるネットワーク内でのみ使用

3. **ファイアウォール設定**
   - 必要に応じてポート14711をブロック
   - 使用時のみ開放を推奨

---

## ライセンス

MIT License - 詳細はREADME.mdを参照

---

## サポート

- **バグ報告**: GitHub Issues
- **機能要望**: GitHub Discussions
- **ドキュメント**: README.md, INSTALL.md参照

---

## 次のステップ

1. `INSTALL.md`で詳細なインストール手順を確認
2. `RELEASE_NOTES.md`で機能と制限事項を確認
3. テスト環境でインストールと動作確認
4. 本番環境へのデプロイ

---

**プログラミング学習を楽しんでください！** 🚀✨
