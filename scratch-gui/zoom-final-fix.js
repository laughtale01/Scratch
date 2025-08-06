// 最終修正スクリプト - ブラウザコンソールで実行

(function finalZoomFix() {
    console.log('=== ズームコントロール最終修正開始 ===');
    
    // 1. すべてのズームコントロールを取得
    const zoomControls = document.querySelectorAll('.blocklyZoom');
    console.log('見つかったズームコントロール数:', zoomControls.length);
    
    if (zoomControls.length < 2) {
        console.error('ズームコントロールが見つかりません。ページを再読み込みしてください。');
        return;
    }
    
    // 2. フライアウトのズームコントロールを修正（通常は2番目）
    const flyoutZoom = zoomControls[1];
    if (!flyoutZoom) {
        console.error('フライアウトのズームコントロールが見つかりません。');
        return;
    }
    
    // 3. 不透明度を修正
    flyoutZoom.style.opacity = '1';
    flyoutZoom.style.pointerEvents = 'all';
    
    // 4. 各画像を修正
    const images = flyoutZoom.querySelectorAll('image');
    images.forEach((img, index) => {
        // 画像の不透明度を強制的に1に
        img.style.opacity = '1';
        img.setAttribute('opacity', '1');
        img.style.pointerEvents = 'all';
        img.style.cursor = 'pointer';
        
        // 既存のイベントリスナーを保持しながら、見た目を修正
        const wrapper = document.createElement('div');
        wrapper.style.cssText = `
            position: absolute;
            width: 36px;
            height: 36px;
            background: rgba(255, 255, 255, 0.9);
            border-radius: 5px;
            pointer-events: none;
        `;
        
        // SVG内でdivは使えないので、代わりにフィルターを適用
        img.style.filter = 'opacity(1) brightness(1.2) contrast(1.2)';
    });
    
    // 5. Blocklyワークスペースの確認
    if (window.Blockly && window.Blockly.mainWorkspace) {
        const flyout = window.Blockly.mainWorkspace.getFlyout();
        if (flyout && flyout.getWorkspace()) {
            console.log('✅ フライアウトワークスペース確認済み');
            
            // クリックイベントが動作しているか確認
            const testClick = () => {
                const scale = flyout.getWorkspace().scale;
                console.log('現在のズームスケール:', scale);
            };
            
            // 最初のボタンをクリックしてテスト
            images[0].addEventListener('click', () => {
                console.log('ズームインボタンがクリックされました');
                testClick();
            });
        }
    }
    
    console.log('=== 修正完了 ===');
    console.log('ズームコントロールが正常に表示され、クリック可能になりました。');
    
    return '修正が完了しました';
})();