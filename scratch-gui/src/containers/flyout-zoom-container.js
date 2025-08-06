// フライアウトコンテナにズームコントロールを追加する実装
// フライアウトのSVG内ではなく、メインワークスペースのSVGに追加

export const addZoomControlsToFlyout = (workspace, ScratchBlocks) => {
    if (!workspace || !workspace.getFlyout) return null;
    
    const flyout = workspace.getFlyout();
    if (!flyout) return null;
    
    const flyoutWorkspace = flyout.getWorkspace();
    if (!flyoutWorkspace) return null;
    
    // メインワークスペースの親 SVG要素を取得
    const parentSvg = workspace.getParentSvg();
    if (!parentSvg) return null;
    
    // 既存のフライアウトズームコントロールを削除
    const existingZoom = parentSvg.querySelector('.flyout-zoom-controls');
    if (existingZoom) {
        existingZoom.remove();
    }
    
    // ズームコントロールコンテナを作成
    const zoomGroup = ScratchBlocks.utils.createSvgElement('g', {
        'class': 'blocklyZoom flyout-zoom-controls'
    }, parentSvg);
    
    // 各ボタンの設定
    const WIDTH = 36;
    const HEIGHT = 36;
    const MARGIN_BETWEEN = 8;
    const pathToMedia = workspace.options.pathToMedia || '../media/';
    
    // ズームインボタン
    const zoomInButton = ScratchBlocks.utils.createSvgElement('image', {
        'width': WIDTH,
        'height': HEIGHT,
        'y': 0,
        'xlink:href': pathToMedia + 'zoom-in.svg'
    }, zoomGroup);
    
    // ズームアウトボタン
    const zoomOutButton = ScratchBlocks.utils.createSvgElement('image', {
        'width': WIDTH,
        'height': HEIGHT,
        'y': HEIGHT + MARGIN_BETWEEN,
        'xlink:href': pathToMedia + 'zoom-out.svg'
    }, zoomGroup);
    
    // ズームリセットボタン
    const zoomResetButton = ScratchBlocks.utils.createSvgElement('image', {
        'width': WIDTH,
        'height': HEIGHT,
        'y': (HEIGHT * 2) + (MARGIN_BETWEEN * 2),
        'xlink:href': pathToMedia + 'zoom-reset.svg'
    }, zoomGroup);
    
    // イベントハンドラを追加
    ScratchBlocks.bindEventWithChecks_(zoomInButton, 'mousedown', null, function(e) {
        flyoutWorkspace.markFocused();
        flyoutWorkspace.zoomCenter(1);
        ScratchBlocks.Touch.clearTouchIdentifier();
        e.stopPropagation();
        e.preventDefault();
    });
    
    ScratchBlocks.bindEventWithChecks_(zoomOutButton, 'mousedown', null, function(e) {
        flyoutWorkspace.markFocused();
        flyoutWorkspace.zoomCenter(-1);
        ScratchBlocks.Touch.clearTouchIdentifier();
        e.stopPropagation();
        e.preventDefault();
    });
    
    ScratchBlocks.bindEventWithChecks_(zoomResetButton, 'mousedown', null, function(e) {
        flyoutWorkspace.markFocused();
        flyoutWorkspace.setScale(1);
        flyoutWorkspace.scrollCenter();
        ScratchBlocks.Touch.clearTouchIdentifier();
        e.stopPropagation();
        e.preventDefault();
    });
    
    // 位置更新関数
    const updatePosition = () => {
        const flyoutMetrics = flyout.getMetrics_();
        if (!flyoutMetrics) return;
        
        const MARGIN_SIDE = 12;
        const MARGIN_BOTTOM = 12;
        const TOTAL_HEIGHT = (HEIGHT * 3) + (MARGIN_BETWEEN * 2);
        
        // フライアウトの位置とサイズに基づいて位置を計算
        let left, top;
        
        if (workspace.RTL) {
            // RTLの場合、フライアウトは右側にある
            left = flyoutMetrics.absoluteLeft + MARGIN_SIDE;
        } else {
            // LTRの場合、フライアウトは左側にある
            left = flyoutMetrics.absoluteLeft + flyoutMetrics.width - WIDTH - MARGIN_SIDE;
        }
        
        top = flyoutMetrics.absoluteTop + flyoutMetrics.height - TOTAL_HEIGHT - MARGIN_BOTTOM;
        
        zoomGroup.setAttribute('transform', 'translate(' + left + ',' + top + ')');
    };
    
    // 初期位置を設定
    updatePosition();
    
    // リサイズやビューポート変更時に位置を更新
    workspace.addChangeListener((e) => {
        if (e.type === ScratchBlocks.Events.VIEWPORT_CHANGE ||
            e.type === ScratchBlocks.Events.RESIZE) {
            updatePosition();
        }
    });
    
    // フライアウトの位置更新にも対応
    const originalPositionFlyout = flyout.position.bind(flyout);
    flyout.position = function() {
        originalPositionFlyout();
        updatePosition();
    };
    
    return {
        svgGroup_: zoomGroup,
        position: updatePosition
    };
};