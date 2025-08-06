// 統合されたフライアウトリサイザー実装
// ブロックパレットとスクリプトエリアの境界線をドラッグして幅を調整

// 座標修正関数をインポート
import { fixBlocklyCoordinates, forceRedrawFlyout } from './flyout-resizer-fix';

export const addFlyoutResizer = (workspace, ScratchBlocks) => {
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
    const DRAG_AREA_WIDTH = 10;
    
    // ツールボックスの位置を固定
    if (toolboxDiv) {
        toolboxDiv.style.position = 'absolute';
        toolboxDiv.style.left = '0px';
        toolboxDiv.style.top = '0px';
    }
    
    // DOM要素を取得
    const workspaceDiv = workspace.getParentSvg().parentElement;
    const injectionDiv = workspace.getInjectionDiv();
    const flyoutSvg = injectionDiv.querySelector('.blocklyFlyout');
    const toolboxDiv = injectionDiv.querySelector('.blocklyToolboxDiv');
    
    if (!workspaceDiv || !flyoutSvg || !toolboxDiv) {
        console.warn('Required DOM elements not found');
        console.log('workspaceDiv:', workspaceDiv);
        console.log('flyoutSvg:', flyoutSvg);
        console.log('toolboxDiv:', toolboxDiv);
        return null;
    }
    
    // ドラッグ状態
    let isDragging = false;
    let startX = 0;
    let startWidth = DEFAULT_WIDTH;
    let dragArea = null;
    let isUpdating = false;
    
    // 現在の幅を取得
    const getCurrentWidth = () => {
        return flyout.width_ || DEFAULT_WIDTH;
    };
    
    // フライアウトの幅を更新
    const updateFlyoutWidth = (newWidth) => {
        if (isUpdating) return;
        isUpdating = true;
        
        // 範囲内に制限
        newWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, newWidth));
        
        console.log('Updating flyout width to:', newWidth);
        
        // Blocklyの内部プロパティを更新
        flyout.width_ = newWidth;
        
        // フライアウトのSVG要素の幅を直接更新
        flyoutSvg.setAttribute('width', newWidth);
        flyoutSvg.style.width = newWidth + 'px';
        
        // フライアウトの背景も更新
        const flyoutBackground = flyoutSvg.querySelector('.blocklyFlyoutBackground');
        if (flyoutBackground) {
            flyoutBackground.setAttribute('width', newWidth);
        }
        
        // ツールボックスも含めた全体の幅を計算
        const toolboxWidth = toolboxDiv.offsetWidth || 60;
        const totalWidth = toolboxWidth + newWidth;
        
        // フライアウトの位置を更新（ツールボックスの右側に配置）
        if (flyoutSvg && flyoutSvg.parentElement) {
            flyoutSvg.parentElement.style.left = toolboxWidth + 'px';
            flyoutSvg.style.left = '0px';
        }
        
        // ワークスペースのSVGを移動
        const workspaceSvg = workspace.getParentSvg();
        if (workspaceSvg) {
            // ワークスペースの親要素を更新
            const workspaceParent = workspaceSvg.parentElement;
            if (workspaceParent) {
                workspaceParent.style.left = totalWidth + 'px';
            }
            
            // Blocklyの内部変数を更新
            if (workspace.injectionDiv_) {
                const injectionDiv = workspace.injectionDiv_;
                // インジェクションDIV内のワークスペース部分のみを移動
                const blocklyMainBackground = injectionDiv.querySelector('.blocklyMainBackground');
                if (blocklyMainBackground && blocklyMainBackground.parentElement) {
                    blocklyMainBackground.parentElement.style.left = totalWidth + 'px';
                }
            }
        }
        
        // フライアウトを非表示にしてから再表示（これが重要）
        if (flyout.setVisible) {
            // 現在のカテゴリを保存
            const currentCategory = workspace.toolbox_ ? workspace.toolbox_.selectedItem_ : null;
            
            flyout.setVisible(false);
            // 少し遅延を入れて確実に再描画されるようにする
            setTimeout(() => {
                flyout.setVisible(true);
                // フライアウトの位置を再計算
                if (flyout.position_) {
                    flyout.position_();
                }
                // フライアウトの内容を再描画
                if (flyout.reflowInternal_) {
                    flyout.reflowInternal_();
                }
                
                // カテゴリを再選択してブロックを再表示
                if (currentCategory && workspace.toolbox_) {
                    workspace.toolbox_.setSelectedItem(currentCategory);
                }
            }, 50);  // 遅延を少し長くして確実に再描画
        }
        
        // ワークスペースをリサイズ
        workspace.resize();
        
        // フライアウトのスクロールバーを更新
        if (flyout.scrollbar_) {
            flyout.scrollbar_.resize();
        }
        
        // ワークスペースのドラッグ表面を再計算
        if (workspace.dragSurface_) {
            workspace.dragSurface_.clearAndHide();
            // ドラッグサーフェイスのサイズも更新
            if (workspace.dragSurface_.setContentsAndShow) {
                const svg = workspace.dragSurface_.getSurfaceElement();
                if (svg) {
                    svg.style.left = '0px';
                    svg.style.top = '0px';
                }
            }
        }
        
        // ワークスペースのビューポートメトリクスを更新
        if (workspace.updateViewportMetrics_) {
            workspace.updateViewportMetrics_();
        }
        
        // Blocklyの内部メトリクスを完全に再計算
        if (workspace.setMetrics_) {
            const metrics = workspace.getMetrics();
            if (metrics) {
                workspace.setMetrics_(metrics);
            }
        }
        
        // レンダリングを強制
        if (workspace.render) {
            workspace.render();
        }
        
        // ローカルストレージに保存
        try {
            localStorage.setItem('scratchFlyoutWidth', newWidth.toString());
        } catch (e) {
            console.error('Failed to save width to localStorage:', e);
        }
        
        setTimeout(() => {
            isUpdating = false;
        }, 100);
    };
    
    // ドラッグエリアを作成
    const createDragArea = () => {
        dragArea = document.createElement('div');
        dragArea.className = 'flyout-drag-area';
        dragArea.style.cssText = `
            position: absolute;
            width: ${DRAG_AREA_WIDTH}px;
            height: 100%;
            cursor: col-resize;
            background-color: transparent;
            z-index: 100;
            user-select: none;
        `;
        
        // ホバー効果
        dragArea.addEventListener('mouseenter', () => {
            if (!isDragging) {
                dragArea.style.backgroundColor = 'rgba(76, 151, 255, 0.3)';
            }
        });
        
        dragArea.addEventListener('mouseleave', () => {
            if (!isDragging) {
                dragArea.style.backgroundColor = 'transparent';
            }
        });
        
        // インジェクションDivに追加
        injectionDiv.appendChild(dragArea);
    };
    
    // ドラッグエリアの位置を更新
    const updateDragAreaPosition = () => {
        if (!dragArea) return;
        
        // ツールボックスとフライアウトの幅を取得
        const toolboxWidth = toolboxDiv.offsetWidth || 60;
        const flyoutWidth = getCurrentWidth();
        
        // ドラッグエリアをフライアウトの右端に配置
        const borderPosition = toolboxWidth + flyoutWidth;
        
        dragArea.style.left = (borderPosition - DRAG_AREA_WIDTH / 2) + 'px';
        dragArea.style.top = '0';
        dragArea.style.height = injectionDiv.offsetHeight + 'px';
    };
    
    // マウスイベントハンドラ
    let updateTimer = null;
    
    const handleMouseDown = (e) => {
        e.preventDefault();
        e.stopPropagation();
        isDragging = true;
        startX = e.clientX;
        startWidth = getCurrentWidth();
        
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
        
        if (dragArea) {
            dragArea.style.backgroundColor = 'rgba(76, 151, 255, 0.5)';
        }
        
        e.target.setPointerCapture(e.pointerId);
    };
    
    const handleMouseMove = (e) => {
        if (!isDragging) return;
        
        e.preventDefault();
        e.stopPropagation();
        
        const deltaX = e.clientX - startX;
        const newWidth = startWidth + deltaX;
        
        // ドラッグ中は頻繁に更新しすぎないようにデバウンス
        if (updateTimer) {
            clearTimeout(updateTimer);
        }
        
        updateTimer = setTimeout(() => {
            updateFlyoutWidth(newWidth);
            updateDragAreaPosition();
        }, 50);
    };
    
    const handleMouseUp = (e) => {
        if (!isDragging) return;
        
        isDragging = false;
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        if (dragArea) {
            dragArea.style.backgroundColor = 'transparent';
        }
        
        if (e.target.releasePointerCapture) {
            e.target.releasePointerCapture(e.pointerId);
        }
        
        // 最終的な位置で確実に更新
        if (updateTimer) {
            clearTimeout(updateTimer);
        }
        
        const deltaX = e.clientX - startX;
        const finalWidth = startWidth + deltaX;
        updateFlyoutWidth(finalWidth);
        updateDragAreaPosition();
        
        // リサイズ完了後にワークスペースの状態をリセット
        setTimeout(() => {
            // Blocklyの座標系を完全に修正
            fixBlocklyCoordinates(workspace);
            
            // フライアウトを強制的に再描画
            forceRedrawFlyout(workspace);
            
            // 追加の修正: グローバルウィンドウオブジェクトにScratchBlocksを設定
            if (!window.ScratchBlocks && ScratchBlocks) {
                window.ScratchBlocks = ScratchBlocks;
            }
        }, 150);
    };
    
    // ドラッグエリアを作成
    createDragArea();
    
    // イベントリスナーを追加
    if (dragArea) {
        dragArea.addEventListener('pointerdown', handleMouseDown);
        dragArea.addEventListener('pointermove', handleMouseMove);
        dragArea.addEventListener('pointerup', handleMouseUp);
        dragArea.addEventListener('pointercancel', handleMouseUp);
    }
    
    // グローバルイベントリスナー
    document.addEventListener('pointermove', handleMouseMove);
    document.addEventListener('pointerup', handleMouseUp);
    
    // 保存された幅を復元
    try {
        const savedWidth = localStorage.getItem('scratchFlyoutWidth');
        if (savedWidth) {
            const width = parseInt(savedWidth, 10);
            if (!isNaN(width) && width >= MIN_WIDTH && width <= MAX_WIDTH) {
                setTimeout(() => {
                    updateFlyoutWidth(width);
                    updateDragAreaPosition();
                }, 500);
            }
        }
    } catch (e) {
        console.error('Failed to restore saved width:', e);
    }
    
    // 初期位置を設定
    setTimeout(() => {
        updateDragAreaPosition();
    }, 300);
    
    // リサイズイベントリスナー
    const resizeHandler = () => {
        requestAnimationFrame(() => {
            updateDragAreaPosition();
        });
    };
    window.addEventListener('resize', resizeHandler);
    
    // クリーンアップ関数
    const dispose = () => {
        if (dragArea) {
            dragArea.remove();
        }
        document.removeEventListener('pointermove', handleMouseMove);
        document.removeEventListener('pointerup', handleMouseUp);
        window.removeEventListener('resize', resizeHandler);
    };
    
    console.log('Unified flyout resizer initialized');
    
    return {
        updatePosition: updateDragAreaPosition,
        updateWidth: updateFlyoutWidth,
        dispose
    };
};