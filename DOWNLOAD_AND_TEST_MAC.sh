#!/bin/bash

echo "================================================"
echo "  Minecraft x Scratch 自動ダウンロード＆テスト"
echo "================================================"
echo ""

echo "GitHubからプロジェクトをダウンロードしています..."
echo "リポジトリ: https://github.com/laughtale01/Scratch"
echo ""

# デスクトップに移動
cd ~/Desktop

# 既存のフォルダがあれば削除
if [ -d "minecraft_collaboration_project" ]; then
    echo "既存のプロジェクトフォルダを削除しています..."
    rm -rf minecraft_collaboration_project
fi

# GitHubからクローン
echo "[1/5] プロジェクトをダウンロード中..."
git clone https://github.com/laughtale01/Scratch.git minecraft_collaboration_project
if [ $? -ne 0 ]; then
    echo ""
    echo "========================================"
    echo "エラー: ダウンロードに失敗しました"
    echo "========================================"
    echo "Gitがインストールされているか確認してください"
    echo "インストール方法: brew install git"
    exit 1
fi

cd minecraft_collaboration_project

echo ""
echo "[2/5] Scratchの依存関係をインストール中..."
cd scratch-gui
npm install
if [ $? -ne 0 ]; then
    echo ""
    echo "========================================"
    echo "エラー: npm install が失敗しました"
    echo "========================================"
    echo "Node.jsがインストールされているか確認してください"
    echo "インストール方法: https://nodejs.org/"
    exit 1
fi

echo ""
echo "[3/5] Scratchをビルド中..."
npm run build
if [ $? -ne 0 ]; then
    echo "エラー: Scratchのビルドが失敗しました"
    exit 1
fi
cd ..

echo ""
echo "[4/5] Minecraft MODをビルド中..."
cd minecraft-mod
./gradlew build
if [ $? -ne 0 ]; then
    echo ""
    echo "========================================"
    echo "エラー: MODのビルドが失敗しました"
    echo "========================================"
    echo "Java 17がインストールされているか確認してください"
    echo "確認コマンド: java -version"
    exit 1
fi
cd ..

echo ""
echo "[5/5] ブラウザでScratchを開いています..."
open scratch-gui/build/index.html

echo ""
echo "================================================"
echo "  ✅ セットアップ完了！"
echo "================================================"
echo ""
echo "📋 次にやること："
echo ""
echo "1. Minecraft Launcher を開く"
echo "2. Forge 1.20.1 のプロファイルを選択"
echo "3. 一度起動して、MODフォルダを作成"
echo "4. 以下のMODファイルをMODフォルダにコピー："
echo "   ~/Desktop/minecraft_collaboration_project/minecraft-mod/build/libs/minecraft-collaboration-mod-1.0.0.jar"
echo ""
echo "5. Minecraftを再起動"
echo "6. シングルプレイでワールドを作成"
echo "7. チャット画面（Tキー）で以下のコマンドを実行："
echo "   /collab start"
echo ""
echo "8. ブラウザのScratchで："
echo "   - 左下の「拡張機能」ボタンをクリック"
echo "   - 「Minecraft コラボレーション」を選択"
echo ""
echo "================================================"
echo "準備ができたらEnterキーを押してください"
read -p ""