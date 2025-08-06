// 正確な位置に配置されるフライアウトズームコントロール
// フライアウト内に正しく表示され、完全に機能する実装

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
        
        // フライアウトのSVGグループを取得（フライアウト内に配置）
        const flyoutSvg = flyout.svgGroup_;
        if (!flyoutSvg) {
            console.warn('Flyout zoom: No flyout SVG group found');
            return null;
        }
        
        // 既存のズームコントロールを削除
        const existingZoom = flyoutSvg.querySelector('.flyout-zoom-controls-correct');
        if (existingZoom) {
            existingZoom.remove();
        }
        
        // メインのグループを作成
        const zoomGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        zoomGroup.setAttribute('class', 'blocklyZoom flyout-zoom-controls-correct');
        
        // 定数
        const BUTTON_SIZE = 32;  // ボタンのサイズを少し小さく
        const SPACING = 6;       // ボタン間のスペースを狭く
        const MARGIN = 8;        // マージンも小さく
        
        // SVGパスを直接埋め込む（不透明度の問題を回避）
        const createButton = (type, y) => {
            const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            g.setAttribute('transform', `translate(0, ${y})`);
            g.style.cursor = 'pointer';
            
            // 背景の円（薄いグレー）
            const bgCircle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            bgCircle.setAttribute('cx', BUTTON_SIZE / 2);
            bgCircle.setAttribute('cy', BUTTON_SIZE / 2);
            bgCircle.setAttribute('r', BUTTON_SIZE / 2);
            bgCircle.setAttribute('fill', '#231f20');
            bgCircle.setAttribute('opacity', '0.15');
            g.appendChild(bgCircle);
            
            // 白い円
            const whiteCircle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            whiteCircle.setAttribute('cx', BUTTON_SIZE / 2);
            whiteCircle.setAttribute('cy', BUTTON_SIZE / 2);
            whiteCircle.setAttribute('r', (BUTTON_SIZE / 2) - 1);
            whiteCircle.setAttribute('fill', '#fff');
            g.appendChild(whiteCircle);
            
            // アイコン
            const icon = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            icon.setAttribute('stroke', '#575e75');
            icon.setAttribute('stroke-linecap', 'round');
            icon.setAttribute('stroke-linejoin', 'round');
            icon.setAttribute('stroke-width', '1.5');
            icon.setAttribute('fill', 'none');
            icon.setAttribute('opacity', '1'); // 完全に不透明
            
            if (type === 'in') {
                // 虫眼鏡
                const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                circle.setAttribute('cx', '16');
                circle.setAttribute('cy', '16');
                circle.setAttribute('r', '6');
                icon.appendChild(circle);
                
                const line1 = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line1.setAttribute('x1', '21');
                line1.setAttribute('y1', '21');
                line1.setAttribute('x2', '24');
                line1.setAttribute('y2', '24');
                icon.appendChild(line1);
                
                // プラス記号
                const line2 = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line2.setAttribute('x1', '14');
                line2.setAttribute('y1', '16');
                line2.setAttribute('x2', '18');
                line2.setAttribute('y2', '16');
                icon.appendChild(line2);
                
                const line3 = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line3.setAttribute('x1', '16');
                line3.setAttribute('y1', '14');
                line3.setAttribute('x2', '16');
                line3.setAttribute('y2', '18');
                icon.appendChild(line3);
            } else if (type === 'out') {
                // 虫眼鏡
                const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                circle.setAttribute('cx', '16');
                circle.setAttribute('cy', '16');
                circle.setAttribute('r', '6');
                icon.appendChild(circle);
                
                const line1 = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line1.setAttribute('x1', '21');
                line1.setAttribute('y1', '21');
                line1.setAttribute('x2', '24');
                line1.setAttribute('y2', '24');
                icon.appendChild(line1);
                
                // マイナス記号
                const line2 = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line2.setAttribute('x1', '14');
                line2.setAttribute('y1', '16');
                line2.setAttribute('x2', '18');
                line2.setAttribute('y2', '16');
                icon.appendChild(line2);
            } else if (type === 'reset') {
                // リセット（＝）
                const line1 = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line1.setAttribute('x1', '10');
                line1.setAttribute('y1', '14');
                line1.setAttribute('x2', '22');
                line1.setAttribute('y2', '14');
                icon.appendChild(line1);
                
                const line2 = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line2.setAttribute('x1', '10');
                line2.setAttribute('y1', '18');
                line2.setAttribute('x2', '22');
                line2.setAttribute('y2', '18');
                icon.appendChild(line2);
            }
            
            g.appendChild(icon);
            return g;
        };
        
        // ボタンを作成
        const zoomIn = createButton('in', 0);
        const zoomOut = createButton('out', BUTTON_SIZE + SPACING);
        const zoomReset = createButton('reset', (BUTTON_SIZE + SPACING) * 2);
        
        // ボタンをグループに追加
        zoomGroup.appendChild(zoomIn);
        zoomGroup.appendChild(zoomOut);
        zoomGroup.appendChild(zoomReset);
        
        // イベントハンドラ
        zoomIn.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
            flyoutWorkspace.zoomCenter(1);
        });
        
        zoomOut.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
            flyoutWorkspace.zoomCenter(-1);
        });
        
        zoomReset.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
            flyoutWorkspace.setScale(1);
            flyoutWorkspace.scrollCenter();
        });
        
        // フライアウトSVGに追加
        flyoutSvg.appendChild(zoomGroup);
        
        // 位置更新関数
        const updatePosition = () => {
            try {
                // フライアウトの幅を取得
                const flyoutWidth = flyout.width_ || 248;
                
                // メインワークスペースのメトリクスを取得
                const mainMetrics = workspace.getMetrics();
                if (!mainMetrics) return;
                
                // ボタングループの総高さ
                const totalHeight = (BUTTON_SIZE * 3) + (SPACING * 2);
                
                // フライアウト内での位置（右下）
                const left = flyoutWidth - BUTTON_SIZE - MARGIN;
                const top = mainMetrics.viewHeight - totalHeight - MARGIN - 60; // 下部に余裕を持たせる
                
                zoomGroup.setAttribute('transform', `translate(${left}, ${top})`);
                
                // z-indexを最前面に
                zoomGroup.style.zIndex = '1000';
                
                console.log('Flyout zoom position updated:', { left, top, flyoutWidth });
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
        
        // フライアウトの表示/非表示に対応
        if (flyout.setVisible_) {
            const originalSetVisible = flyout.setVisible_.bind(flyout);
            flyout.setVisible_ = function(visible) {
                originalSetVisible(visible);
                if (zoomGroup) {
                    zoomGroup.style.display = visible ? 'block' : 'none';
                }
            };
        }
        
        console.log('Flyout zoom controls initialized successfully');
        
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