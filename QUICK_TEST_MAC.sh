#!/bin/bash

echo "========================================"
echo "  Minecraft x Scratch テスト起動ツール"
echo "========================================"
echo ""

echo "[1/4] Scratchをビルドしています..."
cd scratch-gui
npm install
if [ $? -ne 0 ]; then
    echo "エラー: npm install が失敗しました"
    echo "Node.jsがインストールされているか確認してください"
    exit 1
fi

npm run build
if [ $? -ne 0 ]; then
    echo "エラー: ビルドが失敗しました"
    exit 1
fi
cd ..

echo ""
echo "[2/4] Minecraft MODをビルドしています..."
cd minecraft-mod
./gradlew build
if [ $? -ne 0 ]; then
    echo "エラー: MODのビルドが失敗しました"
    echo "Java 17がインストールされているか確認してください"
    exit 1
fi
cd ..

echo ""
echo "[3/4] Scratchを開いています..."
open scratch-gui/build/index.html

echo ""
echo "[4/4] 完了！"
echo ""
echo "========================================"
echo "  セットアップが完了しました！"
echo "========================================"
echo ""
echo "次の手順："
echo "1. Minecraft (Forge 1.20.1) を起動"
echo "2. MODフォルダに以下のファイルをコピー："
echo "   minecraft-mod/build/libs/minecraft-collaboration-mod-1.0.0.jar"
echo "3. Minecraftを再起動"
echo "4. ワールドに入って /collab start を実行"
echo "5. Scratchで「Minecraftコラボレーション」拡張機能を追加"
echo ""
echo "詳しい手順は EASY_TEST_GUIDE.md を参照してください"
echo ""
read -p "Enterキーを押して終了..."