import PropTypes from 'prop-types';
import React, {useState, useCallback, useRef, useEffect} from 'react';
import classNames from 'classnames';

import styles from './resizable-panes.css';

const ResizablePanes = ({
    children,
    defaultLeftWidth = 480,
    minLeftWidth = 200,
    maxLeftWidth = 800,
    className,
    onResize
}) => {
    const [leftWidth, setLeftWidth] = useState(defaultLeftWidth);
    const [isDragging, setIsDragging] = useState(false);
    const containerRef = useRef(null);

    const handleMouseDown = useCallback((e) => {
        e.preventDefault();
        setIsDragging(true);
    }, []);

    const handleMouseMove = useCallback((e) => {
        if (!isDragging || !containerRef.current) return;

        const containerRect = containerRef.current.getBoundingClientRect();
        const newLeftWidth = e.clientX - containerRect.left;
        
        if (newLeftWidth >= minLeftWidth && newLeftWidth <= maxLeftWidth) {
            setLeftWidth(newLeftWidth);
            
            // Blocklyワークスペースのリサイズを強制実行
            setTimeout(() => {
                if (window.Blockly && window.Blockly.getMainWorkspace()) {
                    window.Blockly.getMainWorkspace().resize();
                }
                // ウィンドウリサイズイベントを発火
                window.dispatchEvent(new Event('resize'));
            }, 0);
            
            // localStorageに保存
            localStorage.setItem('scratch-block-palette-width', newLeftWidth.toString());
            
            if (onResize) {
                onResize(newLeftWidth);
            }
        }
    }, [isDragging, minLeftWidth, maxLeftWidth, onResize]);

    const handleMouseUp = useCallback(() => {
        setIsDragging(false);
    }, []);

    // タッチイベント用のstate
    const [startTouchX, setStartTouchX] = useState(0);
    
    // タッチイベントハンドラー
    const handleTouchStart = useCallback((e) => {
        if (e.touches.length === 1) {
            e.preventDefault();
            setIsDragging(true);
            setStartTouchX(e.touches[0].clientX);
        }
    }, []);
    
    const handleTouchMove = useCallback((e) => {
        if (!isDragging || e.touches.length !== 1 || !containerRef.current) return;
        e.preventDefault();
        
        const containerRect = containerRef.current.getBoundingClientRect();
        const newLeftWidth = e.touches[0].clientX - containerRect.left;
        
        if (newLeftWidth >= minLeftWidth && newLeftWidth <= maxLeftWidth) {
            setLeftWidth(newLeftWidth);
            
            // Blocklyワークスペースのリサイズを強制実行
            setTimeout(() => {
                if (window.Blockly && window.Blockly.getMainWorkspace()) {
                    window.Blockly.getMainWorkspace().resize();
                }
                window.dispatchEvent(new Event('resize'));
            }, 0);
            
            // localStorageに保存
            localStorage.setItem('scratch-block-palette-width', newLeftWidth.toString());
            
            if (onResize) {
                onResize(newLeftWidth);
            }
        }
    }, [isDragging, minLeftWidth, maxLeftWidth, onResize]);
    
    const handleTouchEnd = useCallback(() => {
        setIsDragging(false);
    }, []);

    useEffect(() => {
        if (isDragging) {
            document.addEventListener('mousemove', handleMouseMove);
            document.addEventListener('mouseup', handleMouseUp);
            document.addEventListener('touchmove', handleTouchMove, { passive: false });
            document.addEventListener('touchend', handleTouchEnd);
            
            document.body.style.cursor = 'col-resize';
            document.body.style.userSelect = 'none';
            document.body.style.pointerEvents = 'none';
            
            return () => {
                document.removeEventListener('mousemove', handleMouseMove);
                document.removeEventListener('mouseup', handleMouseUp);
                document.removeEventListener('touchmove', handleTouchMove);
                document.removeEventListener('touchend', handleTouchEnd);
                
                document.body.style.cursor = '';
                document.body.style.userSelect = '';
                document.body.style.pointerEvents = '';
            };
        }
    }, [isDragging, handleMouseMove, handleMouseUp, handleTouchMove, handleTouchEnd]);

    return (
        <div 
            ref={containerRef}
            className={classNames(styles.container, className)}
        >
            <div 
                className={styles.leftPane}
                style={{width: leftWidth}}
            >
                {children[0]}
            </div>
            
            <div 
                className={classNames(styles.resizer, {
                    [styles.resizerActive]: isDragging
                })}
                onMouseDown={handleMouseDown}
                onTouchStart={handleTouchStart}
            >
                <div className={styles.resizerLine} />
            </div>
            
            <div 
                className={styles.rightPane}
            >
                {children[1]}
            </div>
        </div>
    );
};

ResizablePanes.propTypes = {
    children: PropTypes.arrayOf(PropTypes.node).isRequired,
    defaultLeftWidth: PropTypes.number,
    minLeftWidth: PropTypes.number,
    maxLeftWidth: PropTypes.number,
    className: PropTypes.string,
    onResize: PropTypes.func
};

export default ResizablePanes;