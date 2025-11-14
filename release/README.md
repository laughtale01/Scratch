# MinecraftEdu Scratch Controller - リリースパッケージ v1.0.0

🎉 **本番リリース第1版が完成しました！**

---

## パッケージ概要

このフォルダには、MinecraftEdu Scratch Controllerの配布用パッケージが含まれています。

### パッケージサイズ
- **合計**: 約120MB（圧縮済み）
- **Minecraft MOD**: 23KB
- **Scratch GUI**: 120MB（tar.gz圧縮）

---

## ファイル構成

```
release/
├── minecraft-scratch-controller/      メインリリースパッケージ
│   ├── minecraft-mod/                 Minecraft MOD
│   │   └── minecraftedu-mod-0.1.0-1.20.1.jar
│   ├── scratch-gui.tar.gz            Scratch GUI（圧縮済み）
│   ├── INSTALL.md                     インストール手順
│   ├── RELEASE_NOTES.md              リリースノート
│   ├── README.md                      プロジェクト概要
│   └── README_RELEASE.md             リリースパッケージ説明
│
└── DEPLOYMENT_GUIDE.md                デプロイメントガイド（このフォルダ）
```

---

## 配布方法

### 🎯 エンドユーザー向け配布

`minecraft-scratch-controller/` フォルダ全体を配布してください。

#### 配布形式の選択肢:

1. **ZIPファイル**: 圧縮して配布
   ```bash
   cd release
   zip -r minecraft-scratch-controller-v1.0.0.zip minecraft-scratch-controller/
   ```

2. **tar.gz**: Linux/macOS向け
   ```bash
   cd release
   tar -czf minecraft-scratch-controller-v1.0.0.tar.gz minecraft-scratch-controller/
   ```

3. **GitHub Release**: GitHubでリリースを作成し、アセットとしてアップロード

4. **ダウンロードリンク**: Webサーバーに配置

---

## クイックスタートガイド（ユーザー向け）

### ステップ1: パッケージを取得

- ダウンロードまたは受け取ったZIPファイルを解凍

### ステップ2: ドキュメントを読む

1. **INSTALL.md**: インストール手順
2. **RELEASE_NOTES.md**: 機能と制限事項
3. **README_RELEASE.md**: パッケージの詳細

### ステップ3: インストール

INSTALL.mdに従ってインストールを実行

---

## デプロイメント（管理者向け）

### 小規模利用（〜20名）

1. リリースパッケージを各PCに配布
2. 各自でインストール
3. ローカルHTTPサーバーで実行

### 大規模利用（20名〜）

1. Scratch GUIをWebサーバーにデプロイ
2. Minecraft MODのみを各PCに配布
3. ユーザーにWebサイトURLを共有

詳細は `DEPLOYMENT_GUIDE.md` を参照してください。

---

## アップデート情報

### 現在のバージョン
- **バージョン**: 1.0.0
- **リリース日**: 2025-11-14
- **ビルド番号**: 2025-11-14-001

### 次期バージョン予定
- Version 1.1.0: 2026年Q1予定
  - より多くのブロックタイプ
  - エンティティ操作の拡張
  - パフォーマンス改善

---

## サポートとフィードバック

### バグ報告
GitHub Issuesで報告してください：
[https://github.com/your-username/minecraft-scratch-controller/issues](https://github.com/your-username/minecraft-scratch-controller/issues)

### 機能要望
GitHub Discussionsで提案してください：
[https://github.com/your-username/minecraft-scratch-controller/discussions](https://github.com/your-username/minecraft-scratch-controller/discussions)

### コミュニティ
- Discord: [招待リンク]
- Twitter: [@your_handle]

---

## ライセンスとクレジット

### ライセンス
MIT License - 詳細は minecraft-scratch-controller/README.md 参照

### 参考プロジェクト
- [takecx/RemoteControllerMod](https://github.com/takecx/RemoteControllerMod)
- [takecx/scratch-vm](https://github.com/takecx/scratch-vm)
- [takecx/scratch-gui](https://github.com/takecx/scratch-gui)

---

## チェックリスト（配布前）

### 配布準備

- [ ] すべてのドキュメントを確認
- [ ] リリースノートのバージョン番号を確認
- [ ] MODファイルが正しくビルドされているか確認
- [ ] Scratch GUIが正常に動作するか確認
- [ ] パッケージサイズを確認（適切に圧縮されているか）

### テスト

- [ ] クリーンなMinecraft環境でMODをテスト
- [ ] 異なるブラウザでScratch GUIをテスト
- [ ] WebSocket接続をテスト
- [ ] 主要機能（ブロック設置、エンティティ召喚等）をテスト

### 配布

- [ ] GitHubリリースページを作成
- [ ] リリースノートを公開
- [ ] ダウンロードリンクを共有
- [ ] コミュニティに告知

---

## トラブルシューティング

配布後に問題が発生した場合は、以下を確認してください：

1. **ユーザー環境**:
   - Minecraftバージョン（1.20.1）
   - Forgeバージョン（1.20.1）
   - Javaバージョン（17以上）

2. **ファイル整合性**:
   - MODファイルが破損していないか
   - Scratch GUIが正しく展開されているか

3. **ネットワーク**:
   - ファイアウォール設定
   - WebSocket接続
   - ポート14711の状態

---

**リリースおめでとうございます！** 🎊

プログラミング教育の未来を一緒に創りましょう！
