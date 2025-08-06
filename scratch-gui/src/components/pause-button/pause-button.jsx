import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';

import styles from './pause-button.css';

const PauseButton = props => {
    const {
        active,
        className,
        onClick,
        title,
        paused,
        ...componentProps
    } = props;
    return (
        <div
            aria-label={title}
            className={classNames(
                styles.pauseButton,
                className,
                {
                    [styles.isActive]: active,
                    [styles.isPaused]: paused
                }
            )}
            role="button"
            tabIndex={0}
            onClick={onClick}
            {...componentProps}
        >
            <div className={styles.pauseIcon}>
                {paused ? (
                    // Play icon when paused
                    <svg width="20" height="20" viewBox="0 0 20 20">
                        <polygon fill="currentColor" points="4,2 4,18 16,10" />
                    </svg>
                ) : (
                    // Pause icon when playing
                    <svg width="20" height="20" viewBox="0 0 20 20">
                        <rect x="4" y="2" width="4" height="16" fill="currentColor" />
                        <rect x="12" y="2" width="4" height="16" fill="currentColor" />
                    </svg>
                )}
            </div>
        </div>
    );
};

PauseButton.propTypes = {
    active: PropTypes.bool,
    className: PropTypes.string,
    onClick: PropTypes.func.isRequired,
    title: PropTypes.string,
    paused: PropTypes.bool
};

PauseButton.defaultProps = {
    active: false,
    title: 'Pause/Resume',
    paused: false
};

export default PauseButton;