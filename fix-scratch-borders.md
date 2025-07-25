# Scratch GUI 境界線重複問題の修正方法

## 問題の原因

ブロックパレットとスクリプトエリアの間に複数の境界線が表示される原因：

1. **`.injectionDiv`** (Blockly全体のコンテナ)
   - `border: 1px solid $ui-black-transparent;`

2. **`.blocklyToolboxDiv`** (カテゴリメニュー)
   - `border-right: 1px solid $ui-black-transparent;`

3. **`.blocklyFlyout`** (ブロックパレット)
   - `border-right: 1px solid $ui-black-transparent;`

これらが重なって表示されるため、複数の線が見えてしまいます。

## 修正方法

### 方法1: CSSファイルを直接編集（推奨）

`scratch-gui/src/components/blocks/blocks.css` を編集：

```css
/* 27行目あたりの .blocks :global(.injectionDiv) の border を削除 */
.blocks :global(.injectionDiv) {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    /* border: 1px solid $ui-black-transparent; ← この行をコメントアウトまたは削除 */
    border-top-right-radius: $space;
    border-bottom-right-radius: $space;
}
```

### 方法2: カスタムCSSを追加

Scratch GUIのHTMLに以下のスタイルを追加：

```html
<style>
/* 重複した境界線を修正 */
.injectionDiv {
    border: none !important;
}

/* または個別の境界線を削除 */
.blocklyToolboxDiv {
    border-right: none !important;
}
</style>
```

### 方法3: webpack設定でCSSを上書き

`webpack.config.js` でカスタムCSSをインポート：

```javascript
// エントリーポイントに追加
entry: {
    gui: [
        './src/index.js',
        './custom-fixes.css' // カスタム修正CSS
    ]
}
```

## 即座の確認方法

ブラウザの開発者ツールで一時的に確認：

1. F12キーで開発者ツールを開く
2. 要素を選択ツールで境界線が重複している部分を選択
3. スタイルパネルで以下を追加：

```css
.injectionDiv {
    border: none !important;
}
```

## 根本的な解決

最も清潔な解決方法は、`blocks.css`を修正してビルドし直すことです：

```bash
cd scratch-gui
# blocks.cssを編集後
npm run build
```

これにより、境界線の重複問題が解決され、きれいな単一の境界線のみが表示されるようになります。