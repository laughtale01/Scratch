# 次のステップ：開発開始ガイド

## ✅ 完了した作業

おめでとうございます！以下の基本実装が完了しました：

### 📦 作成されたファイル

#### Scratch拡張機能
- ✅ `scratch-client/scratch-vm/package.json` - プロジェクト設定
- ✅ `scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js` - Minecraft拡張機能（完全オリジナル実装）

#### Minecraft MOD
- ✅ `minecraft-mod/build.gradle` - Gradleビルド設定
- ✅ `minecraft-mod/src/main/java/.../MinecraftEduMod.java` - メインMODクラス
- ✅ `minecraft-mod/src/main/java/.../network/WebSocketServer.java` - WebSocketサーバー
- ✅ `minecraft-mod/src/main/java/.../network/MinecraftWebSocketHandler.java` - メッセージハンドラー
- ✅ `minecraft-mod/src/main/java/.../commands/CommandExecutor.java` - コマンド実行

### 🎯 実装済み機能

#### Scratchブロック
- ✅ 接続/切断
- ✅ チャット送信
- ✅ ブロック配置（絶対座標・相対座標）
- ✅ エンティティ召喚
- ✅ テレポート
- ✅ 天気変更
- ✅ 時刻変更
- ✅ プレイヤー位置取得（準備中）

#### Minecraft MOD
- ✅ WebSocketサーバー（ポート14711）
- ✅ JSON通信プロトコル
- ✅ 全コマンドの実行機能

### 📤 GitHubプッシュ完了
https://github.com/laughtale01/Scratch

---

## 🚀 次にやること

### オプション1: すぐに動作確認（推奨）

基本実装が完了したので、すぐにビルドして動作確認できます。

#### ステップ1: Minecraft MODのビルド

```bash
cd "D:\minecraft laughtare project\minecraft-mod"

# Windows
gradlew.bat build

# macOS/Linux
./gradlew build
```

**初回は5-10分かかります**（依存関係のダウンロード）

**期待される結果**:
```
BUILD SUCCESSFUL in Xm Xs
```

ビルドされたMOD: `minecraft-mod/build/libs/minecraftedu-mod-0.1.0-1.20.1.jar`

#### ステップ2: Forgeのインストール

1. https://files.minecraftforge.net/ にアクセス
2. **Minecraft 1.20.1** 用のForgeをダウンロード
3. インストーラーを実行（「Install client」を選択）

#### ステップ3: MODのインストール

```bash
# ビルドしたMODをMinecraftのmodsフォルダにコピー

# Windows
copy minecraft-mod\build\libs\*.jar %APPDATA%\.minecraft\mods\

# macOS
cp minecraft-mod/build/libs/*.jar ~/Library/Application\ Support/minecraft/mods/

# Linux
cp minecraft-mod/build/libs/*.jar ~/.minecraft/mods/
```

#### ステップ4: Minecraftを起動

1. Minecraftランチャーを開く
2. 「forge-1.20.1」プロファイルを選択
3. 「プレイ」をクリック
4. シングルプレイヤーでワールドを作成

**ログに以下が表示されればOK**:
```
[MinecraftEdu]: MinecraftEdu Mod initializing...
[MinecraftEdu]: WebSocket server started on port 14711
[MinecraftEdu]: Scratch clients can now connect!
```

#### ステップ5: Scratch側のセットアップ（暫定）

Scratch VMは現在スタンドアロンなので、Scratch GUIと統合する必要があります。

**一時的な方法**:
既存のScratch 3.0にカスタム拡張として読み込む方法を後述します。

または、**オプション2**で完全なScratch環境を構築します。

---

### オプション2: 完全なScratch環境を構築

#### ステップ1: Scratch公式リポジトリをクローン

```bash
cd "D:\minecraft laughtare project\scratch-client"

# Scratch VM（公式版）
git clone https://github.com/scratchfoundation/scratch-vm.git official-scratch-vm
cd official-scratch-vm
npm install
npm link

# Scratch GUI（公式版）
cd ..
git clone https://github.com/scratchfoundation/scratch-gui.git official-scratch-gui
cd official-scratch-gui
npm install
npm link scratch-vm
```

#### ステップ2: カスタム拡張を追加

```bash
# scratch3_minecraft拡張をコピー
cp -r ../scratch-vm/src/extensions/scratch3_minecraft official-scratch-vm/src/extensions/

# 拡張を登録
# official-scratch-vm/src/extension-support/extension-manager.js を編集
```

**extension-manager.jsに追加**:
```javascript
const minecraftExtension = require('../extensions/scratch3_minecraft');

// builtinExtensions に追加
const builtinExtensions = {
    // ...existing extensions...
    minecraft: minecraftExtension
};
```

#### ステップ3: Scratch GUIに拡張を登録

**official-scratch-gui/src/lib/libraries/extensions/index.jsx**に追加:
```javascript
{
    name: 'Minecraft',
    extensionId: 'minecraft',
    iconURL: minecraftIconURL,
    insetIconURL: minecraftInsetIconURL,
    description: 'Control Minecraft from Scratch',
    featured: true
}
```

#### ステップ4: 起動

```bash
cd official-scratch-gui
npm start
```

ブラウザで http://localhost:8601/ を開く

---

### オプション3: 段階的に追加機能を実装

基本実装は完了したので、以下の機能を順次追加できます：

#### Phase 1: 追加の基本機能
- [ ] `getBlock` - ブロック情報取得
- [ ] `fillBlocks` - 範囲ブロック配置
- [ ] `setGameMode` - ゲームモード変更
- [ ] プレイヤー位置取得の完全実装

#### Phase 2: マルチプレイヤー対応
- [ ] ConnectionManager実装（`docs/MULTIPLAYER_DESIGN.md`参照）
- [ ] 認証システム
- [ ] 権限管理
- [ ] 複数クライアント同時接続テスト

#### Phase 3: 教育機能
- [ ] TutorialManager実装（`docs/EDUCATION_DESIGN.md`参照）
- [ ] チュートリアルJSON読み込み
- [ ] ValidationEngine実装
- [ ] 進捗管理

---

## 🔍 トラブルシューティング

### Gradleビルドエラー

```bash
# Gradleキャッシュクリア
cd minecraft-mod
gradlew clean --refresh-dependencies
gradlew build
```

### MODが読み込まれない

1. Forgeバージョンを確認（1.20.1対応か）
2. `logs/latest.log` でエラー確認
3. Java 17がインストールされているか確認

### WebSocket接続エラー

1. Minecraftが起動しているか確認
2. ポート14711が使用可能か確認
3. ファイアウォール設定を確認

---

## 📚 参考ドキュメント

実装の詳細は以下を参照：

- **PROJECT_DESIGN.md** - 全体設計
- **docs/SETUP_GUIDE.md** - 開発環境セットアップ
- **docs/MULTIPLAYER_DESIGN.md** - マルチプレイヤー設計
- **docs/EDUCATION_DESIGN.md** - 教育機能設計
- **shared/protocol/PROTOCOL_SPEC.md** - 通信プロトコル

---

## 💡 推奨する次のアクション

1. **今すぐ**: Minecraft MODをビルドして動作確認
2. **明日**: Scratch環境を完全にセットアップ
3. **今週**: 基本機能の動作テストとデバッグ
4. **来週**: マルチプレイヤー機能の実装開始

---

**質問や問題があれば、いつでも聞いてください！**

作成日: 2025-11-12
