// 完全に機能するフライアウトズームコントロール
// ブロックパレット内のブロックを正しく拡大縮小

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
        
        // フライアウトのSVGグループを取得
        const flyoutSvg = flyout.svgGroup_;
        if (!flyoutSvg) {
            console.warn('Flyout zoom: No flyout SVG group found');
            return null;
        }
        
        // 既存のズームコントロールを削除
        const existingZoom = flyoutSvg.querySelector('.flyout-zoom-functional');
        if (existingZoom) {
            existingZoom.remove();
        }
        
        // メインのグループを作成
        const zoomGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        zoomGroup.setAttribute('class', 'blocklyZoom flyout-zoom-functional');
        
        // 定数
        const BUTTON_SIZE = 32;
        const SPACING = 6;
        const MARGIN = 10;
        
        // ボタンを作成する関数
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
            
            if (type === 'in') {
                // プラス
                const line1 = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line1.setAttribute('x1', '10');
                line1.setAttribute('y1', '16');
                line1.setAttribute('x2', '22');
                line1.setAttribute('y2', '16');
                icon.appendChild(line1);
                
                const line2 = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line2.setAttribute('x1', '16');
                line2.setAttribute('y1', '10');
                line2.setAttribute('x2', '16');
                line2.setAttribute('y2', '22');
                icon.appendChild(line2);
            } else if (type === 'out') {
                // マイナス
                const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                line.setAttribute('x1', '10');
                line.setAttribute('y1', '16');
                line.setAttribute('x2', '22');
                line.setAttribute('y2', '16');
                icon.appendChild(line);
            } else if (type === 'reset') {
                // ○（リセット）
                const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                circle.setAttribute('cx', '16');
                circle.setAttribute('cy', '16');
                circle.setAttribute('r', '6');
                circle.setAttribute('fill', 'none');
                icon.appendChild(circle);
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
        
        // フライアウトワークスペースのズーム機能を有効化
        if (!flyoutWorkspace.options.zoomOptions) {
            flyoutWorkspace.options.zoomOptions = {
                controls: false,
                wheel: false,  // マウスホイールはスクロール専用にする
                startScale: 1,
                maxScale: 3,
                minScale: 0.3,
                scaleSpeed: 1.2
            };
        }
        
        // イベントハンドラ
        zoomIn.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const currentScale = flyoutWorkspace.scale;
            const newScale = currentScale * 1.2;
            if (newScale <= 3) {
                flyoutWorkspace.setScale(newScale);
                // ズーム後にワークスペースを更新
                flyoutWorkspace.resize();
                flyoutWorkspace.recordDeleteAreas();
                
                // スクロールバーとスクロール可能領域を更新
                if (flyout.scrollbar_) {
                    flyout.scrollbar_.resize();
                    // スクロール位置をリセット
                    flyout.scrollbar_.set(0);
                }
                
                // フライアウトの再配置
                if (flyout.position_) {
                    flyout.position_();
                }
                
                console.log('Flyout zoom in:', newScale);
            }
        });
        
        zoomOut.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const currentScale = flyoutWorkspace.scale;
            const newScale = currentScale / 1.2;
            if (newScale >= 0.3) {
                flyoutWorkspace.setScale(newScale);
                // ズーム後にワークスペースを更新
                flyoutWorkspace.resize();
                flyoutWorkspace.recordDeleteAreas();
                
                // スクロールバーとスクロール可能領域を更新
                if (flyout.scrollbar_) {
                    flyout.scrollbar_.resize();
                    // スクロール位置をリセット
                    flyout.scrollbar_.set(0);
                }
                
                // フライアウトの再配置
                if (flyout.position_) {
                    flyout.position_();
                }
                
                console.log('Flyout zoom out:', newScale);
            }
        });
        
        zoomReset.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            flyoutWorkspace.setScale(1);
            flyoutWorkspace.scrollX = 0;
            flyoutWorkspace.scrollY = 0;
            flyoutWorkspace.translate(0, 0);
            // ズーム後にワークスペースを更新
            flyoutWorkspace.resize();
            flyoutWorkspace.recordDeleteAreas();
            // スクロールバーを更新
            if (flyout.scrollbar_) {
                flyout.scrollbar_.resize();
            }
            console.log('Flyout zoom reset');
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
                const top = mainMetrics.viewHeight - totalHeight - MARGIN - 20; // スクロールバーを避ける
                
                // ボタンがフライアウト内に収まるように調整
                const adjustedLeft = Math.min(left, flyoutWidth - BUTTON_SIZE - 5);
                const adjustedTop = Math.max(10, top);
                
                zoomGroup.setAttribute('transform', `translate(${adjustedLeft}, ${adjustedTop})`);
                
                // 確実に最前面に表示
                if (zoomGroup.parentNode) {
                    zoomGroup.parentNode.appendChild(zoomGroup); // 最前面に移動
                }
            } catch (error) {
                console.error('Flyout zoom: Error updating position', error);
            }
        };
        
        // 初期位置を設定
        updatePosition();
        
        // リサイズイベントをリッスン
        const resizeHandler = () => {
            setTimeout(updatePosition, 10);
        };
        window.addEventListener('resize', resizeHandler);
        
        // ワークスペースイベントをリッスン
        workspace.addChangeListener((e) => {
            if (e.type === ScratchBlocks.Events.VIEWPORT_CHANGE ||
                e.type === ScratchBlocks.Events.RESIZE) {
                updatePosition();
            }
        });
        
        // フライアウトリサイズイベントをリッスン
        const flyoutResizeHandler = () => {
            setTimeout(updatePosition, 10);
        };
        window.addEventListener('flyoutResized', flyoutResizeHandler);
        
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
                window.removeEventListener('flyoutResized', flyoutResizeHandler);
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