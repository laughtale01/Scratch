// フライアウトズームコントロールの安全な実装
// エラーハンドリングを強化し、クラッシュを防止

export const addZoomControlsToFlyout = (workspace, ScratchBlocks) => {
    try {
        // 初期チェック
        if (!workspace || !workspace.getFlyout || !ScratchBlocks) {
            console.warn('Flyout zoom controls: Missing required parameters');
            return null;
        }
        
        const flyout = workspace.getFlyout();
        if (!flyout) {
            console.warn('Flyout zoom controls: No flyout found');
            return null;
        }
        
        const flyoutWorkspace = flyout.getWorkspace();
        if (!flyoutWorkspace) {
            console.warn('Flyout zoom controls: No flyout workspace found');
            return null;
        }
        
        // ZoomControlsクラスが存在するか確認
        if (!ScratchBlocks.ZoomControls) {
            console.warn('Flyout zoom controls: Blockly.ZoomControls not available');
            return null;
        }
        
        // メインワークスペースの親 SVG要素を取得
        const parentSvg = workspace.getParentSvg();
        if (!parentSvg) {
            console.warn('Flyout zoom controls: No parent SVG found');
            return null;
        }
        
        // 既存のフライアウトズームコントロールを削除
        const existingZoom = parentSvg.querySelector('.flyout-zoom-controls');
        if (existingZoom) {
            existingZoom.remove();
        }
        
        // Blocklyの標準ZoomControlsクラスを使用
        let zoomControls;
        try {
            zoomControls = new ScratchBlocks.ZoomControls(flyoutWorkspace);
        } catch (error) {
            console.error('Flyout zoom controls: Failed to create ZoomControls', error);
            return null;
        }
        
        let zoomDom;
        try {
            zoomDom = zoomControls.createDom();
        } catch (error) {
            console.error('Flyout zoom controls: Failed to create DOM', error);
            return null;
        }
        
        // フライアウト専用のクラスを追加
        if (zoomDom) {
            zoomDom.classList.add('flyout-zoom-controls');
            
            // メインワークスペースのSVGに追加
            parentSvg.appendChild(zoomDom);
        }
        
        // カスタム位置決め関数
        if (zoomControls.position) {
            const originalPosition = zoomControls.position.bind(zoomControls);
            zoomControls.position = function() {
                try {
                    // フライアウトのメトリクスを取得
                    const flyoutMetrics = flyout.getMetrics_ ? flyout.getMetrics_() : null;
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
                    let left = flyoutWidth - WIDTH - MARGIN_SIDE;
                    
                    // メインワークスペースの高さから位置を計算
                    const mainMetrics = workspace.getMetrics();
                    let top = (mainMetrics ? mainMetrics.viewHeight : 600) - HEIGHT - MARGIN_BOTTOM;
                    
                    // RTLサポート
                    if (workspace.RTL) {
                        const mainWidth = mainMetrics ? mainMetrics.viewWidth : 800;
                        left = mainWidth - flyoutWidth + MARGIN_SIDE;
                    }
                    
                    // トランスフォームを適用
                    if (this.svgGroup_) {
                        this.svgGroup_.setAttribute('transform', 'translate(' + left + ',' + top + ')');
                    }
                } catch (error) {
                    console.error('Flyout zoom controls: Error in position function', error);
                    originalPosition();
                }
            };
        }
        
        // 初期位置を設定
        try {
            if (zoomControls.position) {
                zoomControls.position();
            }
        } catch (error) {
            console.error('Flyout zoom controls: Error setting initial position', error);
        }
        
        // フライアウトワークスペースに参照を保存
        flyoutWorkspace.zoomControls_ = zoomControls;
        
        // イベントリスナーを追加
        try {
            workspace.addChangeListener(function(e) {
                if (e.type === ScratchBlocks.Events.VIEWPORT_CHANGE ||
                    e.type === ScratchBlocks.Events.RESIZE) {
                    if (zoomControls && zoomControls.position) {
                        zoomControls.position();
                    }
                }
            });
        } catch (error) {
            console.error('Flyout zoom controls: Error adding event listener', error);
        }
        
        // フライアウトの位置更新時にもズームコントロールを更新
        if (flyout.position) {
            const originalPositionFlyout = flyout.position.bind(flyout);
            flyout.position = function() {
                try {
                    originalPositionFlyout();
                    if (zoomControls && zoomControls.position) {
                        zoomControls.position();
                    }
                } catch (error) {
                    console.error('Flyout zoom controls: Error in flyout position', error);
                    originalPositionFlyout();
                }
            };
        }
        
        // フライアウトの表示/非表示に対応
        if (flyout.setVisible_) {
            const originalSetVisible = flyout.setVisible_.bind(flyout);
            flyout.setVisible_ = function(visible) {
                try {
                    originalSetVisible(visible);
                    if (zoomControls && zoomControls.svgGroup_) {
                        zoomControls.svgGroup_.style.display = visible ? 'block' : 'none';
                    }
                } catch (error) {
                    console.error('Flyout zoom controls: Error in setVisible', error);
                    originalSetVisible(visible);
                }
            };
        }
        
        console.log('Flyout zoom controls: Successfully initialized');
        return zoomControls;
        
    } catch (error) {
        console.error('Flyout zoom controls: Fatal error during initialization', error);
        return null;
    }
};