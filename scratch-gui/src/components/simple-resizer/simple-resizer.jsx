import React, {useState, useCallback, useRef} from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import styles from './simple-resizer.css';

const SimpleResizer = ({onResize, className}) => {
    const [isDragging, setIsDragging] = useState(false);
    const startX = useRef(0);

    const handleMouseDown = useCallback(e => {
        e.preventDefault();
        setIsDragging(true);
        startX.current = e.clientX;
        
        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);
    }, []);

    const handleMouseMove = useCallback(e => {
        if (!isDragging) return;
        const deltaX = e.clientX - startX.current;
        if (onResize) {
            onResize(deltaX);
        }
    }, [isDragging, onResize]);

    const handleMouseUp = useCallback(() => {
        setIsDragging(false);
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
    }, [handleMouseMove]);

    return (
        <div
            className={classNames(styles.resizer, className, {
                [styles.dragging]: isDragging
            })}
            onMouseDown={handleMouseDown}
        />
    );
};

SimpleResizer.propTypes = {
    onResize: PropTypes.func.isRequired,
    className: PropTypes.string
};

export default SimpleResizer;