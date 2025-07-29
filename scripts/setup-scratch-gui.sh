#!/bin/bash
# Scratch GUI セットアップスクリプト

echo "🎮 Minecraft対応Scratch GUIセットアップ開始..."

# 1. Scratch GUIのクローン
if [ ! -d "scratch-gui" ]; then
    echo "📥 Scratch GUIをクローン中..."
    git clone https://github.com/LLK/scratch-gui.git
    cd scratch-gui
    git checkout v3.0.0
else
    echo "✅ Scratch GUIは既に存在します"
    cd scratch-gui
fi

# 2. カスタムファイルのコピー
echo "📋 カスタムファイルをコピー中..."
cp -r ../scratch-gui-custom/src/* ./src/

# 3. package.jsonのマージ
echo "📦 依存関係を更新中..."
if command -v jq &> /dev/null; then
    # jqがある場合は自動マージ
    jq -s '.[0] * .[1]' package.json ../scratch-gui-custom/package-patch.json > package.json.tmp
    mv package.json.tmp package.json
else
    echo "⚠️ jqがインストールされていません。手動でpackage.jsonを更新してください"
    echo "以下を追加してください:"
    cat ../scratch-gui-custom/package-patch.json
fi

# 4. 依存関係のインストール
echo "📦 依存関係をインストール中..."
npm install
npm install react-split-pane

# 5. Minecraft拡張機能の登録
echo "🔧 Minecraft拡張機能を登録中..."
EXTENSION_INDEX="src/lib/libraries/extensions/index.jsx"

# バックアップ作成
cp $EXTENSION_INDEX ${EXTENSION_INDEX}.backup

# Minecraft拡張をインポート
if ! grep -q "minecraft" $EXTENSION_INDEX; then
    # importセクションに追加
    sed -i "/^import.*from.*extensions/a import minecraft from './minecraft/index.js';" $EXTENSION_INDEX
    
    # 拡張リストに追加
    sed -i "/export default \[/a \ \ \ \ minecraft," $EXTENSION_INDEX
fi

# 6. アイコンファイルのコピー
echo "🎨 アイコンファイルを準備中..."
mkdir -p src/lib/libraries/extensions/minecraft
# アイコンファイルは後で追加する必要があります

echo "✅ セットアップ完了！"
echo ""
echo "📝 次のステップ:"
echo "1. cd scratch-gui"
echo "2. npm start"
echo "3. ブラウザで http://localhost:8601 を開く"
echo "4. 拡張機能からMinecraftを選択"
echo ""
echo "⚠️ 注意事項:"
echo "- Minecraft拡張サーバー（ポート8000）を起動してください"
echo "- Minecraft + Modが起動していることを確認してください"