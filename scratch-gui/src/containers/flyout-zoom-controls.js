// フライアウト専用のズームコントロール実装

export const addZoomControlsToFlyout = (workspace, ScratchBlocks) => {
    const flyout = workspace.getFlyout();
    if (!flyout) return null;
    
    // フライアウトのSVG要素を取得
    const flyoutSvg = flyout.svgGroup_;
    if (!flyoutSvg) return null;
    
    // まずはフライアウトのワークスペースのオプションを確認
    const flyoutWorkspace = flyout.getWorkspace();
    
    // フライアウトワークスペースに既にズームコントロールがあるか確認
    const existingZoom = flyoutSvg.querySelector('.blocklyZoom');
    if (existingZoom) {
        existingZoom.remove();
    }
    
    // Blocklyのネイティブ実装を使用してズームコントロールを作成
    // ただし、フライアウトワークスペースは通常ズームコントロールを持たないので手動で追加
    const zoomControls = new ScratchBlocks.ZoomControls(flyoutWorkspace);
    const zoomDom = zoomControls.createDom();
    
    // フライアウトのSVGに追加
    flyoutSvg.appendChild(zoomDom);
    
    // カスタム位置決め関数
    zoomControls.position = function() {
        const metrics = flyoutWorkspace.getMetrics();
        if (!metrics) return;
        
        const MARGIN_SIDE = 12;
        const MARGIN_BOTTOM = 12;
        const WIDTH = 36;
        const HEIGHT = 124;
        
        // フライアウトは左側にあるので、右端に配置
        const left = metrics.viewWidth - WIDTH - MARGIN_SIDE;
        const top = metrics.viewHeight - HEIGHT - MARGIN_BOTTOM;
        
        this.svgGroup_.setAttribute('transform', 'translate(' + left + ',' + top + ')');
    };
    
    // 初期位置を設定
    zoomControls.position();
    
    // ワークスペースのリサイズ時に位置を更新
    flyoutWorkspace.addChangeListener(function(e) {
        if (e.type === ScratchBlocks.Events.VIEWPORT_CHANGE) {
            zoomControls.position();
        }
    });
    
    return zoomControls;
};