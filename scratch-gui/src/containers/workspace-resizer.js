/**
 * Scratch Workspace Resizer - 正しい実装
 * ブロックパレット（toolbox + flyout）とスクリプトワークスペースの境界線を調整
 */

export const addWorkspaceResizer = (workspace) => {
    if (!workspace || !workspace.getInjectionDiv) {
        console.warn('Workspace not available for resizer');
        return null;
    }

    const injectionDiv = workspace.getInjectionDiv();
    if (!injectionDiv) {
        console.warn('Injection div not found');
        return null;
    }

    // 設定
    const MIN_WIDTH = 200;
    const MAX_WIDTH = 500;
    const DEFAULT_WIDTH = 300;
    const RESIZER_WIDTH = 4;
    
    // 現在の幅を管理
    let currentPaletteWidth = DEFAULT_WIDTH;
    
    // ローカルストレージから復元
    try {
        const saved = localStorage.getItem('scratch-workspace-palette-width');
        if (saved) {
            const parsedWidth = parseInt(saved, 10);
            if (parsedWidth >= MIN_WIDTH && parsedWidth <= MAX_WIDTH) {
                currentPaletteWidth = parsedWidth;
            }
        }
    } catch (e) {
        console.warn('Failed to load saved palette width:', e);
    }

    // リサイザー要素を作成
    const resizer = document.createElement('div');
    resizer.id = 'workspace-resizer';
    resizer.style.cssText = `
        position: absolute;
        top: 0;
        bottom: 0;
        width: ${RESIZER_WIDTH}px;
        background-color: #e8eaed;
        cursor: col-resize;
        z-index: 1000;
        user-select: none;
        left: ${currentPaletteWidth}px;
        transition: background-color 0.2s ease;
        border-radius: 2px;
    `;
    
    // ホバー効果
    let isDragging = false;
    
    resizer.addEventListener('mouseenter', function() {
        if (!isDragging) {
            this.style.backgroundColor = '#4c97ff';
        }
    });
    
    resizer.addEventListener('mouseleave', function() {
        if (!isDragging) {
            this.style.backgroundColor = '#e8eaed';
        }
    });

    // 現在の境界位置を計算する関数
    const calculateBoundaryPosition = () => {
        const toolboxDiv = injectionDiv.querySelector('.blocklyToolboxDiv');
        const flyout = injectionDiv.querySelector('.blocklyFlyout');
        const mainWorkspaceDiv = injectionDiv.querySelector('.blocklyMainWorkspaceDiv');
        
        console.log('DOM Elements found:');
        console.log('- toolboxDiv:', toolboxDiv, toolboxDiv ? `width: ${toolboxDiv.offsetWidth}px` : 'not found');
        console.log('- flyout:', flyout, flyout ? `width: ${flyout.getBoundingClientRect().width}px` : 'not found');
        console.log('- mainWorkspaceDiv:', mainWorkspaceDiv, mainWorkspaceDiv ? `left: ${mainWorkspaceDiv.offsetLeft}px` : 'not found');
        
        let boundaryX = 0;
        
        if (mainWorkspaceDiv) {
            // メインワークスペースの左端位置を境界として使用
            boundaryX = mainWorkspaceDiv.offsetLeft;
        } else if (toolboxDiv && flyout) {
            // ツールボックス + フライアウトの幅を計算
            boundaryX = toolboxDiv.offsetWidth + flyout.getBoundingClientRect().width;
        } else if (toolboxDiv) {
            // ツールボックスのみの場合
            boundaryX = toolboxDiv.offsetWidth;
        } else {
            // デフォルト値
            boundaryX = currentPaletteWidth;
        }
        
        console.log('Calculated boundary position:', boundaryX);
        return boundaryX;
    };

    // レイアウトを更新する関数
    const updateLayout = (newPaletteWidth) => {
        // 範囲チェック
        newPaletteWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, newPaletteWidth));
        
        // 実際のDOM要素を取得して直接更新
        const toolboxDiv = injectionDiv.querySelector('.blocklyToolboxDiv');
        const flyout = injectionDiv.querySelector('.blocklyFlyout');
        const mainWorkspaceDiv = injectionDiv.querySelector('.blocklyMainWorkspaceDiv');
        
        if (toolboxDiv && flyout && mainWorkspaceDiv) {
            // ツールボックスは固定（通常60px）
            const toolboxWidth = 60;
            const flyoutWidth = newPaletteWidth - toolboxWidth;
            
            // フライアウト幅を調整
            flyout.style.width = flyoutWidth + 'px';
            flyout.setAttribute('width', flyoutWidth);
            
            // フライアウト内の背景も更新
            const flyoutBackground = flyout.querySelector('.blocklyFlyoutBackground');
            if (flyoutBackground) {
                flyoutBackground.setAttribute('width', flyoutWidth);
            }
            
            // メインワークスペースの位置を調整
            mainWorkspaceDiv.style.left = newPaletteWidth + 'px';
            mainWorkspaceDiv.style.width = `calc(100% - ${newPaletteWidth}px)`;
            
            // リサイザーを境界位置に配置
            resizer.style.left = newPaletteWidth + 'px';
        }
        
        currentPaletteWidth = newPaletteWidth;
        
        // ワークスペースをリサイズ
        if (workspace.resize) {
            requestAnimationFrame(() => {
                workspace.resize();
            });
        }
        
        // ローカルストレージに保存
        try {
            localStorage.setItem('scratch-workspace-palette-width', newPaletteWidth.toString());
        } catch (e) {
            console.warn('Failed to save palette width:', e);
        }
        
        return newPaletteWidth;
    };

    // ドラッグ機能
    let startX = 0;
    let startWidth = currentPaletteWidth;

    const handleMouseDown = (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        isDragging = true;
        startX = e.clientX;
        startWidth = currentPaletteWidth;
        
        resizer.style.backgroundColor = '#4c97ff';
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
        
        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);
    };

    const handleMouseMove = (e) => {
        if (!isDragging) return;
        
        e.preventDefault();
        
        const deltaX = e.clientX - startX;
        const newWidth = startWidth + deltaX;
        
        updateLayout(newWidth);
    };

    const handleMouseUp = (e) => {
        if (!isDragging) return;
        
        isDragging = false;
        resizer.style.backgroundColor = '#e8eaed';
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
    };

    // イベントリスナーを設定
    resizer.addEventListener('mousedown', handleMouseDown);

    // 初期位置を正確に設定する関数
    const initializePosition = () => {
        // 実際の境界位置を計算してリサイザーを配置
        const actualBoundary = calculateBoundaryPosition();
        if (actualBoundary !== currentPaletteWidth) {
            currentPaletteWidth = actualBoundary;
        }
        updateLayout(currentPaletteWidth);
    };

    // injectionDivに追加
    injectionDiv.appendChild(resizer);

    // 初期位置を設定（遅延実行でBlocklyの初期化完了を待つ）
    setTimeout(() => {
        initializePosition();
        console.log('Workspace resizer initialized at boundary position:', currentPaletteWidth);
    }, 100);

    // クリーンアップ関数を返す
    return {
        dispose: () => {
            if (resizer && resizer.parentNode) {
                resizer.parentNode.removeChild(resizer);
            }
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
        },
        setWidth: (width) => {
            updateLayout(width);
        },
        getWidth: () => {
            return currentPaletteWidth;
        }
    };
};