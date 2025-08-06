/**
 * ブロックパレットとスクリプトエリア境界線のリサイザー実装
 * 正しい位置（境界線）でリサイズ機能を提供
 */

export const addSimpleBlocklyResizer = (workspace) => {
    if (!workspace || !workspace.getInjectionDiv) {
        console.warn('Workspace not available for resizer');
        return null;
    }

    const injectionDiv = workspace.getInjectionDiv();
    if (!injectionDiv) {
        console.warn('Injection div not found');
        return null;
    }

    // ツールボックスとメインワークスペースを取得
    const toolboxDiv = injectionDiv.querySelector('.blocklyToolboxDiv');
    const mainWorkspaceDiv = injectionDiv.querySelector('.blocklyMainWorkspaceDiv');
    
    if (!toolboxDiv || !mainWorkspaceDiv) {
        console.warn('Toolbox or main workspace div not found');
        return null;
    }

    // 設定
    const MIN_WIDTH = 180;
    const MAX_WIDTH = 600;
    const DEFAULT_WIDTH = 320;
    
    // 保存された幅を取得
    let currentWidth = DEFAULT_WIDTH;
    try {
        const saved = localStorage.getItem('scratch-blockly-width');
        if (saved) {
            const parsedWidth = parseInt(saved, 10);
            if (parsedWidth >= MIN_WIDTH && parsedWidth <= MAX_WIDTH) {
                currentWidth = parsedWidth;
            }
        }
    } catch (e) {
        console.warn('Failed to load saved width:', e);
    }

    // リサイザーを境界線の位置に作成
    const resizer = document.createElement('div');
    resizer.id = 'blockly-boundary-resizer';
    resizer.style.cssText = `
        position: absolute;
        top: 0;
        bottom: 0;
        width: 4px;
        background-color: #e8eaed;
        cursor: col-resize;
        z-index: 1000;
        user-select: none;
        left: ${currentWidth}px;
        transition: background-color 0.2s ease;
        border-radius: 2px;
        border-left: 1px solid #ddd;
        border-right: 1px solid #ddd;
    `;
    
    // ホバー効果
    resizer.addEventListener('mouseenter', function() {
        this.style.backgroundColor = '#4c97ff';
        this.style.width = '6px';
        this.style.left = (currentWidth - 1) + 'px';
    });
    
    resizer.addEventListener('mouseleave', function() {
        if (!isDragging) {
            this.style.backgroundColor = '#e8eaed';
            this.style.width = '4px';
            this.style.left = currentWidth + 'px';
        }
    });

    // 境界線位置を更新してレイアウトを調整する関数
    const updateBoundaryPosition = function(newWidth) {
        // 範囲チェック
        newWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, newWidth));
        
        // ツールボックスの幅を更新
        if (toolboxDiv) {
            toolboxDiv.style.width = newWidth + 'px';
        }
        
        // メインワークスペースの左位置を調整（境界線の右側に配置）
        if (mainWorkspaceDiv) {
            mainWorkspaceDiv.style.left = newWidth + 'px';
            mainWorkspaceDiv.style.width = `calc(100% - ${newWidth}px)`;
        }
        
        // リサイザーの位置を境界線に配置
        resizer.style.left = newWidth + 'px';
        
        // currentWidthも更新
        currentWidth = newWidth;
        
        // 白いオーバーレイを防止するための追加処理
        const injectionDiv = workspace.getInjectionDiv();
        if (injectionDiv) {
            // 不要な白い背景要素を非表示にする
            const whiteElements = injectionDiv.querySelectorAll('rect[fill="#ffffff"], rect[fill="white"]');
            for (let i = 0; i < whiteElements.length; i++) {
                const element = whiteElements[i];
                if (element && element.getAttribute('width') === newWidth.toString()) {
                    element.style.opacity = '0';
                    element.style.display = 'none';
                }
            }
            
            // ブロックパレットエリアの可視性を確保
            const blocklyDiv = injectionDiv.querySelector('.blocklyDiv');
            if (blocklyDiv) {
                blocklyDiv.style.visibility = 'visible';
                blocklyDiv.style.opacity = '1';
            }
        }
        
        // Blocklyワークスペースをリサイズ
        if (workspace && workspace.resize) {
            setTimeout(function() {
                workspace.resize();
                
                // 追加のクリーンアップ
                const injectionDiv = workspace.getInjectionDiv();
                if (injectionDiv) {
                    // 隠れたオーバーレイを強制削除
                    const overlays = injectionDiv.querySelectorAll('div[style*="position: absolute"][style*="background"]');
                    for (let i = 0; i < overlays.length; i++) {
                        const overlay = overlays[i];
                        if (overlay && overlay.style.backgroundColor === 'rgb(255, 255, 255)') {
                            overlay.remove();
                        }
                    }
                }
            }, 50);
        }
        
        // 幅を保存
        try {
            localStorage.setItem('scratch-blockly-width', newWidth.toString());
        } catch (e) {
            console.warn('Failed to save width:', e);
        }
        
        return newWidth;
    };

    // ドラッグ機能
    let isDragging = false;
    let startX = 0;
    let startWidth = currentWidth;

    const handleMouseDown = function(e) {
        e.preventDefault();
        e.stopPropagation();
        
        isDragging = true;
        startX = e.clientX;
        startWidth = currentWidth;
        
        resizer.style.backgroundColor = '#4c97ff';
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
        
        // イベントリスナーを追加
        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);
    };

    const handleMouseMove = function(e) {
        if (!isDragging) return;
        
        e.preventDefault();
        
        const deltaX = e.clientX - startX;
        const newWidth = startWidth + deltaX;
        
        currentWidth = updateBoundaryPosition(newWidth);
    };

    const handleMouseUp = function(e) {
        if (!isDragging) return;
        
        isDragging = false;
        resizer.style.backgroundColor = '#e8eaed';
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        // イベントリスナーを削除
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
    };

    // マウスイベントを設定
    resizer.addEventListener('mousedown', handleMouseDown);

    // injectionDivに追加
    injectionDiv.appendChild(resizer);

    // 初期境界線位置を設定
    currentWidth = updateBoundaryPosition(currentWidth);

    console.log('Simple Blockly resizer added successfully');

    // クリーンアップ関数を返す
    return {
        dispose: function() {
            if (resizer && resizer.parentNode) {
                resizer.parentNode.removeChild(resizer);
            }
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
        },
        setWidth: function(width) {
            currentWidth = updateBoundaryPosition(width);
        },
        getWidth: function() {
            return currentWidth;
        }
    };
};