# 🎮 Minecraft×Scratch 動作テストガイド（中学生向け）

## 📚 はじめに
このガイドでは、MinecraftとScratchを連携させて使うための手順を説明します。
難しそうに見えるかもしれませんが、1つずつ進めれば必ずできます！

## 🎯 必要なもの

### パソコンに入れておくソフト
1. **Java 17**（Minecraftを動かすため）
2. **Node.js**（Scratchを動かすため）  
3. **Minecraft Java版 1.20.1**
4. **Google Chrome**または**Microsoft Edge**（ウェブブラウザ）

## 🚀 起動手順

### 📦 ステップ1: プロジェクトをダウンロード

#### Windows の場合：
1. キーボードで `Windows + R` を押す
2. `cmd` と入力して Enter
3. 黒い画面（コマンドプロンプト）が開く
4. 以下のコマンドをコピペして Enter：

```cmd
cd Desktop
git clone https://github.com/laughtale01/Scratch.git minecraft_collaboration_project
cd minecraft_collaboration_project
```

#### Mac の場合：
1. `Command + Space` を押して「ターミナル」と入力
2. ターミナルを開く
3. 以下のコマンドをコピペして Enter：

```bash
cd ~/Desktop
git clone https://github.com/laughtale01/Scratch.git minecraft_collaboration_project
cd minecraft_collaboration_project
```

### 🌐 ステップ2: Scratch（スクラッチ）を起動

#### Windows の場合：
コマンドプロンプトで以下を実行：

```cmd
cd scratch-gui
npm install
npm run build
cd ..
start scratch-gui\build\index.html
```

#### Mac の場合：
ターミナルで以下を実行：

```bash
cd scratch-gui
npm install
npm run build
cd ..
open scratch-gui/build/index.html
```

**待ち時間**: `npm install`は5〜10分かかります。コーヒーブレイクタイム！☕

### 🎮 ステップ3: Minecraft MODを準備

#### Windows の場合：
新しいコマンドプロンプトを開いて：

```cmd
cd Desktop\minecraft_collaboration_project\minecraft-mod
gradlew.bat build
```

#### Mac の場合：
新しいターミナルを開いて：

```bash
cd ~/Desktop/minecraft_collaboration_project/minecraft-mod
./gradlew build
```

**成功のサイン**: 「BUILD SUCCESSFUL」と表示されればOK！

### 🔧 ステップ4: Minecraftを起動

1. **Minecraft Launcher**を開く
2. **Forge 1.20.1**のプロファイルを選択
3. **MODフォルダ**を開く（ゲーム内の「Mods」ボタン→「Open Mods Folder」）
4. 以下のファイルをMODフォルダにコピー：
   - `minecraft-mod/build/libs/minecraft-collaboration-mod-1.0.0.jar`

5. Minecraftを再起動

### 🎯 ステップ5: 接続テスト

#### A. Minecraftでの設定
1. シングルプレイでワールドを作成
2. ゲーム内でチャット画面を開く（`T`キー）
3. 以下のコマンドを入力：

```
/collab start
```

**成功メッセージ**: 「WebSocketサーバーが起動しました」と表示

#### B. Scratchでの設定
1. ブラウザでScratchを開く（ステップ2で開いたページ）
2. 左下の「拡張機能」ボタンをクリック
3. 「Minecraft コラボレーション」を選択
4. ブロックが追加されるのを確認

## 🧪 動作テスト項目

### テスト1: 接続確認
Scratchで以下のブロックを配置して実行：

```scratch
[Minecraftに接続する]
[接続状態を表示]
```

✅ **成功**: 「接続済み」と表示される

### テスト2: プレイヤー位置取得
```scratch
[プレイヤーの位置を取得]
[X座標を表示]
```

✅ **成功**: 数字（座標）が表示される

### テスト3: ブロック設置
```scratch
[X:0 Y:100 Z:0 にブロックを置く（種類：石）]
```

✅ **成功**: Minecraftの空中に石ブロックが現れる

### テスト4: チャットメッセージ送信
```scratch
[チャットに「Hello Minecraft!」を送信]
```

✅ **成功**: Minecraft画面にメッセージが表示される

### テスト5: コラボレーション機能（上級）
```scratch
[プレイヤー「Steve」に招待を送る]
```

✅ **成功**: Minecraftに招待メッセージが表示される

## 🔍 トラブルシューティング

### ❌ エラー: 「接続できません」
**解決方法**:
1. Minecraftで `/collab start` を実行したか確認
2. ファイアウォールの設定を確認
3. ポート14711が使用可能か確認

### ❌ エラー: 「npm: command not found」
**解決方法**:
1. Node.jsをインストール: https://nodejs.org/
2. パソコンを再起動
3. もう一度試す

### ❌ エラー: 「BUILD FAILED」
**解決方法**:
1. Java 17がインストールされているか確認
2. 以下のコマンドで確認：
   ```
   java -version
   ```
3. 「17」が含まれていればOK

### ❌ エラー: Scratchにブロックが表示されない
**解決方法**:
1. ブラウザを更新（F5キー）
2. キャッシュをクリア（Ctrl+Shift+Delete）
3. 別のブラウザで試す

## 📊 テスト結果の記録

以下の表を使って、テスト結果を記録しましょう：

| テスト項目 | 結果（○/×） | メモ |
|-----------|------------|------|
| Scratch起動 | | |
| MODビルド | | |
| Minecraft起動 | | |
| WebSocket接続 | | |
| 位置取得 | | |
| ブロック設置 | | |
| チャット送信 | | |
| コラボレーション | | |

## 🎉 成功のコツ

1. **あせらない** - エラーが出ても大丈夫！
2. **1つずつ確認** - 手順を飛ばさない
3. **メモを取る** - どこでエラーが出たか記録
4. **助けを求める** - 分からないことは聞こう

## 📝 上級者向けテスト

### パフォーマンステスト
```scratch
繰り返す (100回)
  [ランダムな位置にブロックを置く]
end
```

時間を測って、100個のブロックが設置されるまでの時間を記録

### 同時接続テスト
1. 複数のブラウザタブでScratchを開く
2. それぞれで接続
3. 同時に命令を実行

## 🆘 困ったときは

### ログファイルの確認
Minecraftのログ：
```
.minecraft/logs/latest.log
```

### よくある質問

**Q: どのくらい時間がかかりますか？**
A: 初回セットアップは30分〜1時間。2回目以降は5分で起動できます。

**Q: スマホでもできますか？**
A: 残念ながらパソコンが必要です。

**Q: 友達と一緒にできますか？**
A: はい！同じネットワーク（Wi-Fi）につながっていれば可能です。

## 🎊 完了！

すべてのテストが成功したら、あなたはMinecraft×Scratchマスターです！
自由に創造的なプログラムを作ってみましょう。

### 次のステップ
- Scratchで自動建築プログラムを作る
- 友達とコラボレーションモードで遊ぶ
- オリジナルのゲームを作る

頑張ってください！ 🚀