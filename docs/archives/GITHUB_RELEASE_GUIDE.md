# 📦 GitHub Releaseの作成ガイド

## 🎯 リリースページへのアクセス

1. **リポジトリページを開く**
   https://github.com/laughtale01/Scratch

2. **Releasesセクションへ移動**
   - 右側の「Releases」をクリック
   - または直接: https://github.com/laughtale01/Scratch/releases

3. **「Create a new release」をクリック**

## 📝 リリース情報の入力

### Choose a tag
- プルダウンから `v1.0.0` を選択

### Release title
```
Minecraft協調学習システム v1.0.0 - 初回リリース
```

### Describe this release
以下の内容をコピー＆ペースト：

```markdown
## 🎉 初回リリース

ScratchとMinecraft 1.20.1を連携した小学生向け協調学習システムです。

### ✨ 主な機能
- 🤝 友達招待・訪問システム
- 🏠 安全な帰宅機能（通常・緊急）
- 💬 リアルタイムチャット
- 🧱 ブロック操作（設置・破壊）
- 📍 プレイヤー位置情報取得

### 🔧 技術仕様
- Minecraft Forge 1.20.1対応
- Scratch 3.0拡張機能（7つのブロック）
- WebSocket通信（ポート14711）
- ローカルネットワーク限定接続

### 📦 インストール
1. 下記のModファイルをダウンロード
2. Minecraft Forgeのmodsフォルダに配置
3. Scratch拡張機能を読み込み
4. 詳細は[QUICKSTART.md](https://github.com/laughtale01/Scratch/blob/main/QUICKSTART.md)参照

### 🙏 謝辞
takecxさんのRemoteControllerModを参考にさせていただきました。
```

### Attach binaries
以下のファイルをアップロード：

1. **Minecraft Mod** (必須)
   - `minecraft-mod/build/libs/minecraft-collaboration-mod-1.0.0-all.jar`
   - ファイル名を変更: `minecraft-collaboration-mod-1.0.0.jar`

2. **Scratch拡張機能** (必須)
   - `scratch-extension/dist/` フォルダをZIP化
   - ファイル名: `scratch-extension-1.0.0.zip`

3. **テストスクリプト** (オプション)
   - `test-websocket.js`
   - `test-collaboration.js`

### Set as a pre-release
- [ ] チェックしない（正式リリース）

## 🚀 公開

「Publish release」をクリックして公開！

## ✅ 公開後の確認

1. リリースページで正しく表示されているか確認
2. ダウンロードリンクが機能するか確認
3. READMEからリリースページへのリンクを追加（オプション）

## 📊 リリース後の統計

GitHubは以下の統計を提供：
- ダウンロード数
- リリースの閲覧数
- アセットごとのダウンロード数

---

これでプロジェクトの正式リリースが完了します！🎉