import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';

import styles from './record-button.css';

const RecordButton = props => {
    const {
        active,
        className,
        onClick,
        title,
        recording,
        ...componentProps
    } = props;
    return (
        <div
            aria-label={title}
            className={classNames(
                styles.recordButton,
                className,
                {
                    [styles.isActive]: active,
                    [styles.isRecording]: recording
                }
            )}
            role="button"
            tabIndex={0}
            onClick={onClick}
            {...componentProps}
        >
            <div className={styles.recordIcon}>
                {recording ? (
                    // Stop recording icon
                    <svg width="20" height="20" viewBox="0 0 20 20">
                        <rect x="3" y="3" width="14" height="14" fill="currentColor" rx="2" />
                    </svg>
                ) : (
                    // Start recording icon (circle)
                    <svg width="20" height="20" viewBox="0 0 20 20">
                        <circle cx="10" cy="10" r="8" fill="currentColor" />
                    </svg>
                )}
            </div>
            {recording && (
                <div className={styles.recordingIndicator}>
                    <span className={styles.recordingDot}></span>
                    <span className={styles.recordingText}>REC</span>
                </div>
            )}
        </div>
    );
};

RecordButton.propTypes = {
    active: PropTypes.bool,
    className: PropTypes.string,
    onClick: PropTypes.func.isRequired,
    title: PropTypes.string,
    recording: PropTypes.bool
};

RecordButton.defaultProps = {
    active: false,
    title: 'Record',
    recording: false
};

export default RecordButton;