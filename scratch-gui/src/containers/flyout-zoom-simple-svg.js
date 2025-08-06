// シンプルなSVGベースのフライアウトズームコントロール
// Blocklyの複雑な機能を使わず、直接SVG要素を作成

export const addZoomControlsToFlyout = (workspace, ScratchBlocks) => {
    try {
        if (!workspace || !workspace.getFlyout || !ScratchBlocks) {
            console.warn('Flyout zoom: Missing required parameters');
            return null;
        }
        
        const flyout = workspace.getFlyout();
        if (!flyout) {
            console.warn('Flyout zoom: No flyout found');
            return null;
        }
        
        const flyoutWorkspace = flyout.getWorkspace();
        if (!flyoutWorkspace) {
            console.warn('Flyout zoom: No flyout workspace found');
            return null;
        }
        
        const parentSvg = workspace.getParentSvg();
        if (!parentSvg) {
            console.warn('Flyout zoom: No parent SVG found');
            return null;
        }
        
        // 既存のズームコントロールを削除
        const existingZoom = parentSvg.querySelector('.flyout-zoom-simple');
        if (existingZoom) {
            existingZoom.remove();
        }
        
        // メインのグループを作成
        const zoomGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        zoomGroup.setAttribute('class', 'blocklyZoom flyout-zoom-simple');
        
        const WIDTH = 36;
        const HEIGHT = 36;
        const SPACING = 8;
        // Get the correct path to media files
        const pathToMedia = workspace.options.pathToMedia || '../static/blocks-media/default/';
        
        // ズームインボタン
        const zoomIn = document.createElementNS('http://www.w3.org/2000/svg', 'image');
        zoomIn.setAttribute('width', WIDTH);
        zoomIn.setAttribute('height', HEIGHT);
        zoomIn.setAttribute('x', 0);
        zoomIn.setAttribute('y', 0);
        zoomIn.setAttributeNS('http://www.w3.org/1999/xlink', 'href', pathToMedia + 'zoom-in.svg');
        zoomIn.style.cursor = 'pointer';
        zoomIn.style.opacity = '1';
        
        // ズームアウトボタン
        const zoomOut = document.createElementNS('http://www.w3.org/2000/svg', 'image');
        zoomOut.setAttribute('width', WIDTH);
        zoomOut.setAttribute('height', HEIGHT);
        zoomOut.setAttribute('x', 0);
        zoomOut.setAttribute('y', HEIGHT + SPACING);
        zoomOut.setAttributeNS('http://www.w3.org/1999/xlink', 'href', pathToMedia + 'zoom-out.svg');
        zoomOut.style.cursor = 'pointer';
        zoomOut.style.opacity = '1';
        
        // ズームリセットボタン
        const zoomReset = document.createElementNS('http://www.w3.org/2000/svg', 'image');
        zoomReset.setAttribute('width', WIDTH);
        zoomReset.setAttribute('height', HEIGHT);
        zoomReset.setAttribute('x', 0);
        zoomReset.setAttribute('y', (HEIGHT + SPACING) * 2);
        zoomReset.setAttributeNS('http://www.w3.org/1999/xlink', 'href', pathToMedia + 'zoom-reset.svg');
        zoomReset.style.cursor = 'pointer';
        zoomReset.style.opacity = '1';
        
        // ボタンをグループに追加
        zoomGroup.appendChild(zoomIn);
        zoomGroup.appendChild(zoomOut);
        zoomGroup.appendChild(zoomReset);
        
        // イベントハンドラ
        zoomIn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (flyoutWorkspace.zoomCenter) {
                flyoutWorkspace.zoomCenter(1);
            }
        });
        
        zoomOut.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (flyoutWorkspace.zoomCenter) {
                flyoutWorkspace.zoomCenter(-1);
            }
        });
        
        zoomReset.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (flyoutWorkspace.setScale) {
                flyoutWorkspace.setScale(1);
                if (flyoutWorkspace.scrollCenter) {
                    flyoutWorkspace.scrollCenter();
                }
            }
        });
        
        // 親SVGに追加
        parentSvg.appendChild(zoomGroup);
        
        // 位置更新関数
        const updatePosition = () => {
            try {
                const flyoutWidth = flyout.width_ || 248;
                const mainMetrics = workspace.getMetrics();
                if (!mainMetrics) return;
                
                const MARGIN = 12;
                const TOTAL_HEIGHT = (HEIGHT * 3) + (SPACING * 2);
                
                const left = flyoutWidth - WIDTH - MARGIN;
                const top = mainMetrics.viewHeight - TOTAL_HEIGHT - MARGIN;
                
                zoomGroup.setAttribute('transform', `translate(${left}, ${top})`);
            } catch (error) {
                console.error('Flyout zoom: Error updating position', error);
            }
        };
        
        // 初期位置を設定
        updatePosition();
        
        // リサイズイベントをリッスン
        const resizeHandler = () => updatePosition();
        window.addEventListener('resize', resizeHandler);
        
        // ワークスペースイベントをリッスン
        workspace.addChangeListener((e) => {
            if (e.type === ScratchBlocks.Events.VIEWPORT_CHANGE ||
                e.type === ScratchBlocks.Events.RESIZE) {
                updatePosition();
            }
        });
        
        console.log('Flyout zoom: Simple SVG controls initialized successfully');
        
        return {
            svgGroup_: zoomGroup,
            position: updatePosition,
            dispose: () => {
                window.removeEventListener('resize', resizeHandler);
                if (zoomGroup.parentNode) {
                    zoomGroup.remove();
                }
            }
        };
        
    } catch (error) {
        console.error('Flyout zoom: Fatal error', error);
        return null;
    }
};