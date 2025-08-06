// フライアウトズームコントロールのデバッグバージョン
// 位置とメトリクス情報を詳細にログ出力

export const addZoomControlsToFlyout = (workspace, ScratchBlocks) => {
    console.log('\n=== Flyout Zoom Controls Debug ===');
    
    if (!workspace || !workspace.getFlyout) {
        console.error('No workspace or getFlyout method');
        return null;
    }
    
    const flyout = workspace.getFlyout();
    if (!flyout) {
        console.error('No flyout found');
        return null;
    }
    
    console.log('Flyout found:', flyout);
    console.log('Flyout SVG group:', flyout.svgGroup_);
    console.log('Flyout width:', flyout.width_);
    
    const flyoutWorkspace = flyout.getWorkspace();
    if (!flyoutWorkspace) {
        console.error('No flyout workspace');
        return null;
    }
    
    console.log('Flyout workspace:', flyoutWorkspace);
    
    // メトリクス情報を取得
    const metrics = flyoutWorkspace.getMetrics();
    console.log('Flyout workspace metrics:', metrics);
    
    const flyoutMetrics = flyout.getMetrics_();
    console.log('Flyout metrics:', flyoutMetrics);
    
    // メインワークスペースのメトリクス
    const mainMetrics = workspace.getMetrics();
    console.log('Main workspace metrics:', mainMetrics);
    
    // DOM要素の確認
    const parentSvg = workspace.getParentSvg();
    console.log('Parent SVG:', parentSvg);
    
    const flyoutSvg = flyout.svgGroup_;
    console.log('Flyout SVG bounding box:', flyoutSvg ? flyoutSvg.getBoundingClientRect() : 'N/A');
    
    // 既存のズームコントロールを削除
    const existingZoom = parentSvg.querySelector('.flyout-zoom-debug');
    if (existingZoom) {
        existingZoom.remove();
    }
    
    // ズームコントロールを作成
    const zoomGroup = ScratchBlocks.utils.createSvgElement('g', {
        'class': 'blocklyZoom flyout-zoom-debug'
    }, parentSvg);
    
    const WIDTH = 36;
    const HEIGHT = 124;
    const pathToMedia = workspace.options.pathToMedia || '../media/';
    
    // ボタンを作成（簡略化）
    const zoomInButton = ScratchBlocks.utils.createSvgElement('image', {
        'width': WIDTH,
        'height': 36,
        'y': 0,
        'xlink:href': pathToMedia + 'zoom-in.svg'
    }, zoomGroup);
    
    const zoomOutButton = ScratchBlocks.utils.createSvgElement('image', {
        'width': WIDTH,
        'height': 36,
        'y': 44,
        'xlink:href': pathToMedia + 'zoom-out.svg'
    }, zoomGroup);
    
    const zoomResetButton = ScratchBlocks.utils.createSvgElement('image', {
        'width': WIDTH,
        'height': 36,
        'y': 88,
        'xlink:href': pathToMedia + 'zoom-reset.svg'
    }, zoomGroup);
    
    // イベントハンドラ
    [zoomInButton, zoomOutButton, zoomResetButton].forEach((button, idx) => {
        ScratchBlocks.bindEventWithChecks_(button, 'mousedown', null, function(e) {
            console.log('Zoom button clicked:', idx);
            flyoutWorkspace.markFocused();
            if (idx === 0) flyoutWorkspace.zoomCenter(1);
            else if (idx === 1) flyoutWorkspace.zoomCenter(-1);
            else flyoutWorkspace.setScale(1);
            e.stopPropagation();
            e.preventDefault();
        });
    });
    
    // 位置計算と設定
    const updatePosition = () => {
        console.log('\n--- Updating position ---');
        
        // 様々な方法でメトリクスを取得
        const flyoutMetrics = flyout.getMetrics_();
        const workspaceMetrics = flyoutWorkspace.getMetrics();
        const mainMetrics = workspace.getMetrics();
        
        console.log('Flyout metrics:', flyoutMetrics);
        console.log('Flyout workspace metrics:', workspaceMetrics);
        console.log('Main workspace metrics:', mainMetrics);
        
        // フライアウトの実際の幅を計算
        const flyoutWidth = flyout.width_ || flyoutMetrics.width || 248;
        console.log('Flyout width:', flyoutWidth);
        
        // 位置を計算
        const MARGIN = 12;
        let left, top;
        
        // フライアウト内の右下に配置
        left = flyoutWidth - WIDTH - MARGIN;
        top = mainMetrics.viewHeight - HEIGHT - MARGIN;
        
        console.log('Calculated position - left:', left, 'top:', top);
        
        zoomGroup.setAttribute('transform', 'translate(' + left + ',' + top + ')');
    };
    
    // 初期位置を設定
    updatePosition();
    
    // イベントリスナー
    workspace.addChangeListener((e) => {
        if (e.type === ScratchBlocks.Events.VIEWPORT_CHANGE || 
            e.type === ScratchBlocks.Events.RESIZE) {
            console.log('Event triggered:', e.type);
            updatePosition();
        }
    });
    
    console.log('\n=== Flyout Zoom Controls Created ===\n');
    
    return {
        svgGroup_: zoomGroup,
        position: updatePosition
    };
};