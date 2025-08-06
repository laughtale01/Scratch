// ズームコントロールの不透明度とクリック機能を修正するスクリプト
// ブラウザのコンソールで実行してください

(function fixZoomControls() {
    // すべてのズームコントロールを取得
    const zoomControls = document.querySelectorAll('.blocklyZoom');
    
    zoomControls.forEach((zoomGroup, index) => {
        console.log(`修正中: ズームコントロール ${index + 1}`);
        
        // グループ全体の不透明度を設定
        zoomGroup.style.opacity = '1';
        zoomGroup.style.pointerEvents = 'all';
        
        // 各画像ボタンを修正
        const images = zoomGroup.querySelectorAll('image');
        images.forEach((img, imgIndex) => {
            // 不透明度を1に設定
            img.style.opacity = '1';
            img.setAttribute('opacity', '1');
            img.style.cursor = 'pointer';
            img.style.pointerEvents = 'all';
            
            // 画像パスを修正
            const href = img.getAttributeNS('http://www.w3.org/1999/xlink', 'href');
            if (href && !href.startsWith('/')) {
                const fileName = href.split('/').pop();
                img.setAttributeNS('http://www.w3.org/1999/xlink', 'href', `/static/blocks-media/default/${fileName}`);
            }
            
            // クリックイベントを追加（フライアウト用）
            if (index === 1) { // 2番目のズームコントロール（フライアウト）
                img.onclick = function(e) {
                    e.stopPropagation();
                    e.preventDefault();
                    
                    // Blocklyワークスペースを取得
                    if (window.Blockly && window.Blockly.mainWorkspace) {
                        const flyout = window.Blockly.mainWorkspace.getFlyout();
                        if (flyout) {
                            const flyoutWs = flyout.getWorkspace();
                            const currentScale = flyoutWs.scale || 1;
                            
                            if (imgIndex === 0) { // ズームイン
                                flyoutWs.setScale(Math.min(currentScale * 1.1, 3));
                            } else if (imgIndex === 1) { // ズームアウト
                                flyoutWs.setScale(Math.max(currentScale * 0.9, 0.3));
                            } else if (imgIndex === 2) { // リセット
                                flyoutWs.setScale(1);
                            }
                            
                            // フライアウトを再描画
                            if (flyout.reflowInternal_) {
                                flyout.reflowInternal_();
                                flyout.position();
                            }
                        }
                    }
                };
            }
        });
    });
    
    console.log('✅ ズームコントロールの修正が完了しました');
})();