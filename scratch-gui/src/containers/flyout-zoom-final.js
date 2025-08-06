// フライアウトズームコントロールの最終実装
// スクリプトエリアのズームコントロールと同じデザインで、フライアウト内に表示

export const addZoomControlsToFlyout = (workspace, ScratchBlocks) => {
    if (!workspace || !workspace.getFlyout) return null;
    
    const flyout = workspace.getFlyout();
    if (!flyout) return null;
    
    const flyoutWorkspace = flyout.getWorkspace();
    if (!flyoutWorkspace) return null;
    
    // メインワークスペースの觪 SVG要素を取得
    const parentSvg = workspace.getParentSvg();
    if (!parentSvg) return null;
    
    // 既存のフライアウトズームコントロールを削除
    const existingZoom = parentSvg.querySelector('.flyout-zoom-controls');
    if (existingZoom) {
        existingZoom.remove();
    }
    
    // Blocklyの標準ZoomControlsクラスを使用
    const zoomControls = new ScratchBlocks.ZoomControls(flyoutWorkspace);
    const zoomDom = zoomControls.createDom();
    
    // フライアウト専用のクラスを追加
    zoomDom.classList.add('flyout-zoom-controls');
    
    // メインワークスペースのSVGに追加
    parentSvg.appendChild(zoomDom);
    
    // カスタム位置決め関数
    const originalPosition = zoomControls.position.bind(zoomControls);
    zoomControls.position = function() {
        // フライアウトのメトリクスを取得
        const flyoutMetrics = flyout.getMetrics_();
        if (!flyoutMetrics) {
            originalPosition();
            return;
        }
        
        // ズームコントロールのサイズ
        const WIDTH = 36;
        const HEIGHT = 124; // 3つのボタン + 間隔
        const MARGIN_SIDE = 12;
        const MARGIN_BOTTOM = 12;
        
        // フライアウトの実際の幅を取得
        const flyoutWidth = flyout.width_ || flyoutMetrics.width || 248;
        
        // フライアウトの左端からの相対位置を計算
        // フライアウトは常にワークスペースの左側にある
        let left = flyoutWidth - WIDTH - MARGIN_SIDE;
        
        // メインワークスペースの高さから位置を計算
        const mainMetrics = workspace.getMetrics();
        let top = mainMetrics.viewHeight - HEIGHT - MARGIN_BOTTOM;
        
        // RTLサポート
        if (workspace.RTL) {
            // RTLの場合、フライアウトは右側にある
            const mainWidth = mainMetrics.viewWidth;
            left = mainWidth - flyoutWidth + MARGIN_SIDE;
        }
        
        // トランスフォームを適用
        this.svgGroup_.setAttribute('transform', 'translate(' + left + ',' + top + ')');
    };
    
    // 初期位置を設定
    zoomControls.position();
    
    // フライアウトワークスペースに参照を保存
    flyoutWorkspace.zoomControls_ = zoomControls;
    
    // イベントリスナーを追加
    workspace.addChangeListener(function(e) {
        if (e.type === ScratchBlocks.Events.VIEWPORT_CHANGE ||
            e.type === ScratchBlocks.Events.RESIZE) {
            zoomControls.position();
        }
    });
    
    // フライアウトの位置更新時にもズームコントロールを更新
    const originalPositionFlyout = flyout.position.bind(flyout);
    flyout.position = function() {
        originalPositionFlyout();
        if (zoomControls && zoomControls.position) {
            zoomControls.position();
        }
    };
    
    // フライアウトの表示/非表示に対応
    const originalSetVisible = flyout.setVisible_.bind(flyout);
    flyout.setVisible_ = function(visible) {
        originalSetVisible(visible);
        if (zoomControls && zoomControls.svgGroup_) {
            zoomControls.svgGroup_.style.display = visible ? 'block' : 'none';
        }
    };
    
    return zoomControls;
};