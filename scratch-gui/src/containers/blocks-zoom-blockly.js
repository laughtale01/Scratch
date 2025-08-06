// Blocklyの正確な実装に基づいたズームコントロール
import {ZOOM_IN_DATA_URL, ZOOM_OUT_DATA_URL, ZOOM_RESET_DATA_URL} from './zoom-svg-data';

export const createBlocklyZoomControls = (ScratchBlocks, workspace) => {
    const WIDTH = 36;
    const HEIGHT = 36;
    const MARGIN_VERTICAL = 8;
    
    // ズームコントロールのグループを作成
    const svgGroup = ScratchBlocks.utils.createSvgElement('g', {
        'class': 'blocklyZoom'
    }, null);
    
    // ズームインボタン
    const zoominSvg = ScratchBlocks.utils.createSvgElement('image', {
        'width': WIDTH,
        'height': HEIGHT,
        'y': 0
    }, svgGroup);
    
    zoominSvg.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', ZOOM_IN_DATA_URL);
    
    // ズームインイベント
    const zoomInHandler = function(e) {
        if (workspace && workspace.markFocused) {
            workspace.markFocused();
        }
        workspace.zoomCenter(1);
        if (ScratchBlocks.Touch && ScratchBlocks.Touch.clearTouchIdentifier) {
            ScratchBlocks.Touch.clearTouchIdentifier();
        }
        e.stopPropagation();
        e.preventDefault();
    };
    
    // 複数の方法でイベントをバインド
    ScratchBlocks.bindEventWithChecks_(zoominSvg, 'mousedown', null, zoomInHandler);
    zoominSvg.addEventListener('click', zoomInHandler);
    zoominSvg.style.cursor = 'pointer';
    zoominSvg.style.pointerEvents = 'all';
    
    // ズームアウトボタン
    const zoomoutSvg = ScratchBlocks.utils.createSvgElement('image', {
        'width': WIDTH,
        'height': HEIGHT,
        'y': HEIGHT + MARGIN_VERTICAL
    }, svgGroup);
    
    zoomoutSvg.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', ZOOM_OUT_DATA_URL);
    
    // ズームアウトイベント
    const zoomOutHandler = function(e) {
        if (workspace && workspace.markFocused) {
            workspace.markFocused();
        }
        workspace.zoomCenter(-1);
        if (ScratchBlocks.Touch && ScratchBlocks.Touch.clearTouchIdentifier) {
            ScratchBlocks.Touch.clearTouchIdentifier();
        }
        e.stopPropagation();
        e.preventDefault();
    };
    
    ScratchBlocks.bindEventWithChecks_(zoomoutSvg, 'mousedown', null, zoomOutHandler);
    zoomoutSvg.addEventListener('click', zoomOutHandler);
    zoomoutSvg.style.cursor = 'pointer';
    zoomoutSvg.style.pointerEvents = 'all';
    
    // ズームリセットボタン
    const zoomresetSvg = ScratchBlocks.utils.createSvgElement('image', {
        'width': WIDTH,
        'height': HEIGHT,
        'y': (HEIGHT + MARGIN_VERTICAL) * 2
    }, svgGroup);
    
    zoomresetSvg.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', ZOOM_RESET_DATA_URL);
    
    // ズームリセットイベント
    const zoomResetHandler = function(e) {
        if (workspace && workspace.markFocused) {
            workspace.markFocused();
        }
        workspace.setScale(workspace.options && workspace.options.zoomOptions ? 
            workspace.options.zoomOptions.startScale : 1);
        workspace.scrollCenter();
        if (ScratchBlocks.Touch && ScratchBlocks.Touch.clearTouchIdentifier) {
            ScratchBlocks.Touch.clearTouchIdentifier();
        }
        e.stopPropagation();
        e.preventDefault();
    };
    
    ScratchBlocks.bindEventWithChecks_(zoomresetSvg, 'mousedown', null, zoomResetHandler);
    zoomresetSvg.addEventListener('click', zoomResetHandler);
    zoomresetSvg.style.cursor = 'pointer';
    zoomresetSvg.style.pointerEvents = 'all';
    
    return svgGroup;
};

// ズームコントロールの位置を計算
export const positionZoomControls = (workspace, svgGroup) => {
    const MARGIN_SIDE = 12;
    const MARGIN_BOTTOM = 12;
    const WIDTH = 36;
    const HEIGHT = 124; // 3ボタン + 2マージン
    
    const metrics = workspace.getMetrics();
    if (!metrics) return;
    
    // RTLとツールボックスの位置を考慮
    let left;
    if (workspace.RTL) {
        left = MARGIN_SIDE;
    } else {
        left = metrics.viewWidth + metrics.absoluteLeft - WIDTH - MARGIN_SIDE;
    }
    
    const top = metrics.viewHeight + metrics.absoluteTop - HEIGHT - MARGIN_BOTTOM;
    
    svgGroup.setAttribute('transform', 'translate(' + left + ',' + top + ')');
};