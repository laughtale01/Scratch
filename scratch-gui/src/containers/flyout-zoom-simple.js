// シンプルなフライアウトズームコントロール実装
// Blocklyの標準的な方法に従う

export const addSimpleZoomToFlyout = (workspace, ScratchBlocks) => {
    if (!workspace || !workspace.getFlyout) return null;
    
    const flyout = workspace.getFlyout();
    if (!flyout) return null;
    
    const flyoutWorkspace = flyout.getWorkspace();
    if (!flyoutWorkspace) return null;
    
    // フライアウトワークスペースのズームオプションを有効化
    if (!flyoutWorkspace.options.zoomOptions) {
        flyoutWorkspace.options.zoomOptions = {
            controls: true,
            wheel: true,
            startScale: 1
        };
    }
    
    // 既存のズームコントロールを確認
    if (flyoutWorkspace.zoomControls_) {
        return flyoutWorkspace.zoomControls_;
    }
    
    // Blocklyの標準的なZoomControlsクラスが使用可能か確認
    if (!ScratchBlocks.ZoomControls) {
        console.warn('Blockly.ZoomControls not available');
        return null;
    }
    
    // ズームコントロールを作成
    try {
        const zoomControls = new ScratchBlocks.ZoomControls(flyoutWorkspace);
        const zoomDom = zoomControls.createDom();
        
        // フライアウトのSVGグループに追加
        if (flyout.svgGroup_) {
            flyout.svgGroup_.appendChild(zoomDom);
        } else {
            // フライアウトのSVGが見つからない場合は、ワークスペースのSVGに追加
            const workspaceSvg = flyoutWorkspace.getParentSvg();
            if (workspaceSvg) {
                workspaceSvg.appendChild(zoomDom);
            }
        }
        
        // カスタム位置決め
        const originalPosition = zoomControls.position.bind(zoomControls);
        zoomControls.position = function() {
            const metrics = flyoutWorkspace.getMetrics();
            if (!metrics) {
                originalPosition();
                return;
            }
            
            // フライアウトの幅を考慮した位置決め
            const MARGIN = 12;
            const WIDTH = 36;
            const HEIGHT = 124;
            
            // RTLをサポート
            let left;
            if (flyoutWorkspace.RTL) {
                left = MARGIN;
            } else {
                // フライアウトの実際の幅を使用
                const flyoutWidth = flyout.width_ || metrics.viewWidth;
                left = flyoutWidth - WIDTH - MARGIN;
            }
            
            const top = metrics.viewHeight - HEIGHT - MARGIN;
            
            this.svgGroup_.setAttribute('transform', 'translate(' + left + ',' + top + ')');
        };
        
        // 初期位置を設定
        zoomControls.position();
        
        // フライアウトワークスペースに参照を保存
        flyoutWorkspace.zoomControls_ = zoomControls;
        
        // イベントリスナーを追加
        flyoutWorkspace.addChangeListener(function(e) {
            if (e.type === ScratchBlocks.Events.VIEWPORT_CHANGE ||
                e.type === ScratchBlocks.Events.FLYOUT_POSITION) {
                zoomControls.position();
            }
        });
        
        return zoomControls;
    } catch (error) {
        console.error('Failed to create zoom controls:', error);
        return null;
    }
};