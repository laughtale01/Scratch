/**
 * カスタマイズされたGUIコンポーネント
 * ドラッグ可能な境界線を追加
 */
import React from 'react';
import SplitPane from 'react-split-pane';
import './gui.css';

const GUI_SIZES = {
    blocksPane: {
        default: 400,
        min: 200,
        max: 600
    }
};

class GUIComponent extends React.Component {
    constructor(props) {
        super(props);
        
        // localStorageから保存されたサイズを読み込み
        const savedSize = localStorage.getItem('scratchBlocksPaneSize');
        
        this.state = {
            blocksPaneSize: savedSize ? parseInt(savedSize, 10) : GUI_SIZES.blocksPane.default
        };
    }
    
    handlePaneSizeChange = (size) => {
        // サイズ制限
        const constrainedSize = Math.max(
            GUI_SIZES.blocksPane.min,
            Math.min(GUI_SIZES.blocksPane.max, size)
        );
        
        this.setState({ blocksPaneSize: constrainedSize });
        
        // localStorageに保存（デバウンス）
        clearTimeout(this.saveTimeout);
        this.saveTimeout = setTimeout(() => {
            localStorage.setItem('scratchBlocksPaneSize', constrainedSize);
        }, 500);
    }
    
    componentWillUnmount() {
        clearTimeout(this.saveTimeout);
    }
    
    render() {
        const {
            blocksTabVisible,
            costumesTabVisible,
            soundsTabVisible,
            onTabSelect,
            children
        } = this.props;
        
        return (
            <div className="gui">
                <div className="menu-bar-position">
                    {/* メニューバー */}
                </div>
                
                <div className="main-area">
                    <SplitPane
                        split="vertical"
                        minSize={GUI_SIZES.blocksPane.min}
                        maxSize={GUI_SIZES.blocksPane.max}
                        defaultSize={this.state.blocksPaneSize}
                        onChange={this.handlePaneSizeChange}
                        resizerStyle={{
                            background: '#e0e0e0',
                            cursor: 'col-resize',
                            width: '8px',
                            boxSizing: 'border-box',
                            backgroundClip: 'padding-box',
                            border: '2px solid transparent'
                        }}
                    >
                        {/* 左側: ブロックパレット */}
                        <div className="blocks-pane">
                            <div className="tab-list">
                                <button
                                    className={`tab ${blocksTabVisible ? 'active' : ''}`}
                                    onClick={() => onTabSelect('blocks')}
                                >
                                    ブロック
                                </button>
                                <button
                                    className={`tab ${costumesTabVisible ? 'active' : ''}`}
                                    onClick={() => onTabSelect('costumes')}
                                >
                                    コスチューム
                                </button>
                                <button
                                    className={`tab ${soundsTabVisible ? 'active' : ''}`}
                                    onClick={() => onTabSelect('sounds')}
                                >
                                    音
                                </button>
                            </div>
                            <div className="blocks-wrapper">
                                {children.blocks}
                            </div>
                        </div>
                        
                        {/* 右側: スクリプトエリア */}
                        <div className="editor-pane">
                            <div className="stage-and-target-wrapper">
                                {children.stage}
                                {children.targetPane}
                            </div>
                            <div className="editor-wrapper">
                                {children.editorTab}
                            </div>
                        </div>
                    </SplitPane>
                </div>
            </div>
        );
    }
}

export default GUIComponent;