// シンプルなフライアウトリサイザー実装
export const addSimpleFlyoutResizer = (workspace, ScratchBlocks) => {
    if (!workspace || !workspace.getFlyout) {
        console.warn('Workspace or getFlyout not available');
        return null;
    }
    
    const flyout = workspace.getFlyout();
    if (!flyout) {
        console.warn('Flyout not available');
        return null;
    }
    
    // 定数
    const MIN_WIDTH = 200;
    const MAX_WIDTH = 400;
    const DEFAULT_WIDTH = 248;
    
    // 既存のリサイザーを削除
    const existingResizer = document.querySelector('.simple-flyout-resizer');
    if (existingResizer) {
        existingResizer.remove();
    }
    
    // リサイザーハンドルを作成
    const resizer = document.createElement('div');
    resizer.className = 'simple-flyout-resizer';
    resizer.style.cssText = `
        position: fixed;
        width: 5px;
        height: 100vh;
        background: transparent;
        cursor: col-resize;
        z-index: 1000;
        transition: background 0.2s;
    `;
    
    // ホバー時の視覚効果
    resizer.addEventListener('mouseenter', () => {
        resizer.style.background = 'rgba(76, 151, 255, 0.5)';
    });
    
    resizer.addEventListener('mouseleave', () => {
        if (!isDragging) {
            resizer.style.background = 'transparent';
        }
    });
    
    // ドラッグ状態
    let isDragging = false;
    let startX = 0;
    let startWidth = DEFAULT_WIDTH;
    
    // リサイザーの位置を更新
    const updateResizerPosition = () => {
        const injectionDiv = workspace.getInjectionDiv();
        if (!injectionDiv) return;
        
        const toolbox = injectionDiv.querySelector('.blocklyToolboxDiv');
        const flyoutSvg = injectionDiv.querySelector('.blocklyFlyout');
        
        if (!toolbox || !flyoutSvg) return;
        
        const rect = injectionDiv.getBoundingClientRect();
        const toolboxWidth = toolbox.offsetWidth || 60;
        const flyoutWidth = flyout.width_ || DEFAULT_WIDTH;
        
        resizer.style.left = (rect.left + toolboxWidth + flyoutWidth - 2) + 'px';
        resizer.style.top = rect.top + 'px';
        resizer.style.height = rect.height + 'px';
    };
    
    // フライアウト幅を更新
    const updateFlyoutWidth = (newWidth) => {
        newWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, newWidth));
        
        // Blocklyの内部幅を更新
        flyout.width_ = newWidth;
        
        // フライアウトのSVGグループ全体を取得
        const flyoutGroup = flyout.svgGroup_;
        if (flyoutGroup) {
            // フライアウトのメインSVG要素
            const flyoutSvg = flyoutGroup.querySelector('.blocklyFlyout');
            if (flyoutSvg) {
                flyoutSvg.setAttribute('width', newWidth);
                flyoutSvg.style.width = newWidth + 'px';
            }
            
            // 背景要素
            const background = flyout.svgBackground_;
            if (background) {
                background.setAttribute('width', newWidth);
            }
            
            // フライアウトの親要素も更新
            if (flyoutGroup.parentElement) {
                flyoutGroup.parentElement.style.width = newWidth + 'px';
            }
        }
        
        // フライアウトのワークスペースも更新
        const flyoutWorkspace = flyout.getWorkspace();
        if (flyoutWorkspace) {
            const flyoutMetrics = flyoutWorkspace.getMetrics();
            if (flyoutMetrics) {
                flyoutMetrics.viewWidth = newWidth;
                flyoutWorkspace.setMetrics_(flyoutMetrics);
            }
        }
        
        // ワークスペースを移動
        const injectionDiv = workspace.getInjectionDiv();
        const toolbox = injectionDiv.querySelector('.blocklyToolboxDiv');
        const mainWorkspace = injectionDiv.querySelector('.blocklyMainBackground').parentElement.parentElement;
        
        if (toolbox && mainWorkspace) {
            const toolboxWidth = toolbox.offsetWidth || 60;
            mainWorkspace.style.left = (toolboxWidth + newWidth) + 'px';
        }
        
        // Blocklyの内部座標系をリセット
        if (workspace.injectionDiv_) {
            // キャッシュされた座標をクリア
            delete workspace.injectionDiv_.cachedLeft_;
            delete workspace.injectionDiv_.cachedTop_;
        }
        
        // ドラッグサーフェイスをクリア
        if (workspace.dragSurface_) {
            workspace.dragSurface_.clearAndHide();
            const dragSvg = workspace.dragSurface_.getSurfaceElement();
            if (dragSvg) {
                dragSvg.style.left = '0px';
                dragSvg.style.top = '0px';
            }
        }
        
        // ワークスペースをリサイズ
        workspace.resize();
        
        // 座標変換をリセット
        if (workspace.getCanvas) {
            const canvas = workspace.getCanvas();
            if (canvas) {
                const currentTransform = canvas.getAttribute('transform');
                if (currentTransform) {
                    // 現在のスケールを維持しながら位置をリセット
                    const match = currentTransform.match(/scale\(([\d.]+)\)/);
                    if (match) {
                        canvas.setAttribute('transform', `translate(0,0) scale(${match[1]})`);
                    }
                }
            }
        }
        
        // Blocklyにメトリクスの再計算を強制
        if (workspace.getMetrics && workspace.setMetrics_) {
            const metrics = workspace.getMetrics();
            workspace.setMetrics_(metrics);
        }
        
        // フライアウトの位置を更新
        if (flyout.position_) {
            flyout.position_();
        }
        
        // フライアウトを再描画（強制的に更新）
        const selectedCategory = workspace.toolbox_ ? workspace.toolbox_.selectedItem_ : null;
        if (selectedCategory) {
            // 一時的に別のカテゴリに切り替えてから戻す
            const categories = workspace.toolbox_.contents_;
            if (categories && categories.length > 1) {
                const currentIndex = categories.indexOf(selectedCategory);
                const tempCategory = categories[(currentIndex + 1) % categories.length];
                workspace.toolbox_.setSelectedItem(tempCategory);
                setTimeout(() => {
                    workspace.toolbox_.setSelectedItem(selectedCategory);
                }, 1);
            }
        }
        
        // リサイザーの位置を更新
        updateResizerPosition();
        
        // ローカルストレージに保存
        try {
            localStorage.setItem('scratchSimpleFlyoutWidth', newWidth.toString());
        } catch (e) {
            console.error('Failed to save width:', e);
        }
    };
    
    // マウスイベントハンドラ
    const handleMouseDown = (e) => {
        e.preventDefault();
        isDragging = true;
        startX = e.clientX;
        startWidth = flyout.width_ || DEFAULT_WIDTH;
        resizer.style.background = 'rgba(76, 151, 255, 0.8)';
        document.body.style.cursor = 'col-resize';
        
        // ドラッグ中のハイライト
        const highlightDiv = document.createElement('div');
        highlightDiv.className = 'resizer-highlight';
        highlightDiv.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            z-index: 999;
            cursor: col-resize;
        `;
        document.body.appendChild(highlightDiv);
    };
    
    const handleMouseMove = (e) => {
        if (!isDragging) return;
        
        const deltaX = e.clientX - startX;
        const newWidth = startWidth + deltaX;
        updateFlyoutWidth(newWidth);
    };
    
    const handleMouseUp = () => {
        if (!isDragging) return;
        
        isDragging = false;
        resizer.style.background = 'transparent';
        document.body.style.cursor = '';
        
        // ハイライトを削除
        const highlight = document.querySelector('.resizer-highlight');
        if (highlight) {
            highlight.remove();
        }
        
        // 最終的な座標修正
        setTimeout(() => {
            // 完全な座標系リセット
            if (workspace.injectionDiv_) {
                const rect = workspace.getInjectionDiv().getBoundingClientRect();
                workspace.injectionDiv_.cachedLeft_ = rect.left;
                workspace.injectionDiv_.cachedTop_ = rect.top;
            }
            
            // ドラッグサーフェイスの完全リセット
            if (workspace.dragSurface_) {
                workspace.dragSurface_.clearAndHide();
                // SVGとHTMLの両方のコンテナをリセット
                const svgContainer = workspace.dragSurface_.SVG_;
                const htmlContainer = workspace.dragSurface_.container_;
                if (svgContainer) {
                    svgContainer.style.transform = '';
                    svgContainer.setAttribute('transform', '');
                }
                if (htmlContainer) {
                    htmlContainer.style.transform = '';
                }
            }
            
            workspace.resize();
            updateResizerPosition();
            
            // イベントを発火して全体を更新
            if (ScratchBlocks && ScratchBlocks.Events) {
                const event = new ScratchBlocks.Events.Ui(null, 'workspaceResize', null, null);
                event.workspaceId = workspace.id;
                ScratchBlocks.Events.fire(event);
            }
        }, 100);
    };
    
    // イベントリスナー
    resizer.addEventListener('mousedown', handleMouseDown);
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
    
    // ドキュメントに追加
    document.body.appendChild(resizer);
    
    // 初期設定
    const savedWidth = localStorage.getItem('scratchSimpleFlyoutWidth');
    if (savedWidth) {
        // 少し遅延を入れて確実に初期化
        setTimeout(() => {
            updateFlyoutWidth(parseInt(savedWidth, 10));
        }, 200);
    }
    
    // 初期位置設定
    setTimeout(() => {
        updateResizerPosition();
        // フライアウトが正しく表示されているか確認
        if (flyout && flyout.isVisible() && flyout.position_) {
            flyout.position_();
        }
    }, 300);
    
    // リサイズイベント
    window.addEventListener('resize', updateResizerPosition);
    
    // ワークスペースイベント
    workspace.addChangeListener((e) => {
        if (e.type === ScratchBlocks.Events.TOOLBOX_ITEM_SELECT ||
            e.type === ScratchBlocks.Events.VIEWPORT_CHANGE) {
            updateResizerPosition();
        }
    });
    
    return {
        updatePosition: updateResizerPosition,
        dispose: () => {
            resizer.remove();
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
            window.removeEventListener('resize', updateResizerPosition);
        }
    };
};