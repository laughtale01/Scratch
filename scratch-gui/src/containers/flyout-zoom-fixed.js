// フライアウト専用のズームコントロール実装
// Blocklyの標準ZoomControlsクラスを使用し、フライアウトに適応

export const addZoomControlsToFlyout = (workspace, ScratchBlocks) => {
    if (!workspace || !workspace.getFlyout) return null;
    
    const flyout = workspace.getFlyout();
    if (!flyout) return null;
    
    // フライアウトのワークスペースを取得
    const flyoutWorkspace = flyout.getWorkspace();
    if (!flyoutWorkspace) return null;
    
    // 既存のズームコントロールを削除
    const existingZoom = flyout.svgGroup_ && flyout.svgGroup_.querySelector('.blocklyZoom');
    if (existingZoom) {
        existingZoom.remove();
    }
    
    // Blocklyの標準ZoomControlsを作成
    const zoomControls = new ScratchBlocks.ZoomControls(flyoutWorkspace);
    const zoomDom = zoomControls.createDom();
    
    // SVGファイルのパスを修正（Scratchのmediaパスを使用）
    const pathToMedia = workspace.options.pathToMedia || '../media/';
    const images = zoomDom.querySelectorAll('image');
    images.forEach(img => {
        const href = img.getAttributeNS('http://www.w3.org/1999/xlink', 'href');
        if (href) {
            const filename = href.split('/').pop();
            img.setAttributeNS('http://www.w3.org/1999/xlink', 'href', pathToMedia + filename);
        }
    });
    
    // フライアウトのSVGグループに追加
    flyout.svgGroup_.appendChild(zoomDom);
    
    // カスタム位置決め関数をオーバーライド
    const originalPosition = zoomControls.position.bind(zoomControls);
    zoomControls.position = function() {
        const metrics = flyoutWorkspace.getMetrics();
        if (!metrics) {
            originalPosition();
            return;
        }
        
        // フライアウトの実際の幅を取得
        const flyoutWidth = flyout.width_ || metrics.viewWidth || 248;
        const MARGIN_SIDE = 12;
        const MARGIN_BOTTOM = 12;
        const WIDTH = 36;
        const HEIGHT = 124;
        
        // フライアウトは左側にあるので、右端に配置
        const left = flyoutWidth - WIDTH - MARGIN_SIDE;
        const top = metrics.viewHeight - HEIGHT - MARGIN_BOTTOM;
        
        this.svgGroup_.setAttribute('transform', 'translate(' + left + ',' + top + ')');
    };
    
    // 初期位置を設定
    zoomControls.position();
    
    // フライアウトのイベントリスナーを追加
    flyout.workspace_.addChangeListener(function(e) {
        if (e.type === ScratchBlocks.Events.VIEWPORT_CHANGE ||
            e.type === ScratchBlocks.Events.RESIZE) {
            zoomControls.position();
        }
    });
    
    // フライアウトの表示/非表示に連動
    const originalSetVisible = flyout.setVisible_.bind(flyout);
    flyout.setVisible_ = function(visible) {
        originalSetVisible(visible);
        if (zoomControls.svgGroup_) {
            zoomControls.svgGroup_.style.display = visible ? 'block' : 'none';
        }
    };
    
    // フライアウトワークスペースに参照を保存
    flyoutWorkspace.zoomControls_ = zoomControls;
    
    return zoomControls;
};