# 🚀 Minecraft協調学習システム - クイックスタートガイド

## 📋 必要なもの
- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.2.0
- Scratch 3.0（デスクトップ版推奨）
- Java 17以上

## 🔧 インストール手順

### 1. Minecraft Modのインストール
1. Minecraft Forgeをインストール（まだの場合）
2. 以下のファイルをMinecraftのmodsフォルダにコピー：
   ```
   minecraft-collaboration-mod-1.0.0-all.jar
   ```
   modsフォルダの場所：
   - Windows: `%APPDATA%\.minecraft\mods\`
   - Mac: `~/Library/Application Support/minecraft/mods/`

### 2. Scratch拡張機能の準備
1. `scratch-extension/dist/minecraft-collaboration-extension.js`を任意の場所にコピー
2. Scratch 3.0を開く
3. 左下の拡張機能ボタンをクリック
4. 「拡張機能を読み込む」からJSファイルを選択

## 🎮 使い方

### 基本的な流れ
1. Minecraftを起動（シングルプレイヤーでOK）
2. ワールドに入る
3. Scratchで「Minecraftに接続」ブロックを実行
4. 各種ブロックが使用可能に！

### 主な機能

#### 🤝 友達との協調プレイ
```scratch
[📧 太郎さんを招待]  // 友達を自分のワールドに招待
[🚪 花子さんの世界に訪問申請]  // 友達のワールドへ行く
[✅ 次郎さんの訪問を承認]  // 訪問を許可
```

#### 🏠 安全機能
```scratch
[🏠 自分のワールドに帰る]  // 通常の帰宅
[🚨 緊急帰宅]  // 危険な時にすぐ帰る（体力も回復）
```

#### 💬 コミュニケーション
```scratch
[💬 チャット: こんにちは！]  // メッセージを送る
[📬 招待通知の数]  // 届いた招待を確認
```

## 🎯 サンプルプログラム

### 例1: 友達を招待して一緒に建築
```scratch
[🔌 Minecraftに接続する]
[📧 友達の名前 を招待]
[💬 チャット: 一緒に家を作ろう！]
[🧱 stone を X:10 Y:64 Z:10 に置く]
```

### 例2: 友達の世界を訪問
```scratch
[🔌 Minecraftに接続する]
[🚪 友達の名前 の世界に訪問申請]
もし <📬 招待通知の数 > 0> なら
  [💬 チャット: 招待ありがとう！]
```

### 例3: 危険から逃げる
```scratch
もし <プレイヤーのY座標 < 10> なら
  [🚨 緊急帰宅]
  [💬 チャット: 危なかった！]
```

## ⚠️ 注意事項
- 友達の名前は正確に入力してください
- 緊急帰宅は1日3回までにしましょう
- 他の人のワールドでは許可を得てから建築しましょう

## 🆘 困ったときは
- 接続できない → Minecraftが起動しているか確認
- ブロックが動かない → 「Minecraftに接続」を最初に実行
- 友達が見つからない → 同じネットワーク内にいるか確認

## 📞 サポート
問題が解決しない場合は、以下を確認してください：
- `minecraft-mod/run/logs/latest.log` - Minecraftのログ
- ブラウザの開発者コンソール - Scratchのエラー

楽しい協調学習を！ 🎉