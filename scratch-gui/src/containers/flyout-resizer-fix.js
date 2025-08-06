// Blocklyの座標系を修正するためのユーティリティ関数
export const fixBlocklyCoordinates = (workspace) => {
    if (!workspace) return;
    
    // ワークスペースの座標系をリセット
    const injectionDiv = workspace.getInjectionDiv();
    if (injectionDiv) {
        const rect = injectionDiv.getBoundingClientRect();
        
        // Blocklyの内部座標キャッシュをクリア
        if (workspace.injectionDiv_) {
            delete workspace.injectionDiv_.cachedLeft_;
            delete workspace.injectionDiv_.cachedTop_;
        }
        
        // ドラッグサーフェイスをリセット
        if (workspace.dragSurface_) {
            workspace.dragSurface_.clearAndHide();
            const dragSvg = workspace.dragSurface_.getSurfaceElement();
            if (dragSvg) {
                dragSvg.style.transform = '';
                dragSvg.setAttribute('transform', '');
            }
        }
        
        // ワークスペースのSVGをリセット
        const svg = workspace.getParentSvg();
        if (svg) {
            svg.style.transform = '';
            
            // キャンバスの変換をリセット
            const canvas = workspace.getCanvas();
            if (canvas) {
                canvas.setAttribute('transform', 'translate(0,0)');
            }
        }
        
        // Blocklyの座標計算を強制的に更新
        if (workspace.absoluteMetrics_) {
            workspace.absoluteMetrics_ = null;
        }
        
        // メトリクスを再計算
        workspace.resize();
        
        // スクロール位置をリセット
        workspace.scrollX = 0;
        workspace.scrollY = 0;
        
        // イベントを発火して全体を更新
        if (window.ScratchBlocks && window.ScratchBlocks.Events) {
            const event = new window.ScratchBlocks.Events.Ui(null, 'coordinateReset', null, null);
            event.workspaceId = workspace.id;
            window.ScratchBlocks.Events.fire(event);
        }
    }
};

// フライアウトの再描画を強制する関数
export const forceRedrawFlyout = (workspace) => {
    if (!workspace || !workspace.getFlyout) return;
    
    const flyout = workspace.getFlyout();
    if (!flyout) return;
    
    // 現在のカテゴリを保存
    const toolbox = workspace.toolbox_;
    const selectedItem = toolbox ? toolbox.selectedItem_ : null;
    
    // フライアウトを一時的に隠す
    if (flyout.setVisible) {
        flyout.setVisible(false);
        
        // フライアウトワークスペースもリセット
        const flyoutWorkspace = flyout.getWorkspace();
        if (flyoutWorkspace) {
            flyoutWorkspace.scale = 1;
            flyoutWorkspace.scrollX = 0;
            flyoutWorkspace.scrollY = 0;
            
            if (flyoutWorkspace.getCanvas) {
                const canvas = flyoutWorkspace.getCanvas();
                if (canvas) {
                    canvas.setAttribute('transform', 'translate(0,0)');
                }
            }
        }
        
        // 再表示
        setTimeout(() => {
            flyout.setVisible(true);
            
            // フライアウトの位置を再計算
            if (flyout.position_) {
                flyout.position_();
            }
            
            // 内容を再描画
            if (flyout.reflowInternal_) {
                flyout.reflowInternal_();
            }
            
            // カテゴリを再選択
            if (selectedItem && toolbox) {
                toolbox.setSelectedItem(selectedItem);
                
                // ブロックが表示されない場合は強制的に再表示
                if (flyout.show_) {
                    const blocks = selectedItem.getContents();
                    if (blocks) {
                        flyout.show_(blocks);
                    }
                }
            }
        }, 100);
    }
};