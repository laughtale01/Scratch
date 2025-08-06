import PropTypes from 'prop-types';
import React from 'react';
import classNames from 'classnames';
import {defineMessages, injectIntl, intlShape} from 'react-intl';

import Button from '../button/button.jsx';

import styles from './block-palette-zoom.css';
import zoomInIcon from './icon--zoom-in.svg';
import zoomOutIcon from './icon--zoom-out.svg';
import zoomResetIcon from './icon--zoom-reset.svg';

const messages = defineMessages({
    zoomIn: {
        id: 'gui.blockPaletteZoom.zoomIn',
        defaultMessage: 'Zoom in',
        description: 'Button to zoom in block palette'
    },
    zoomOut: {
        id: 'gui.blockPaletteZoom.zoomOut',
        defaultMessage: 'Zoom out',
        description: 'Button to zoom out block palette'
    },
    zoomReset: {
        id: 'gui.blockPaletteZoom.zoomReset',
        defaultMessage: 'Reset zoom',
        description: 'Button to reset block palette zoom'
    }
});

const BlockPaletteZoom = ({
    className,
    intl,
    onZoomIn,
    onZoomOut,
    onZoomReset,
    zoomLevel,
    ...props
}) => (
    <div
        className={classNames(styles.container, className)}
        {...props}
    >
        <div className={styles.zoomInfo}>
            {Math.round(zoomLevel * 100)}%
        </div>
        
        <Button
            className={styles.zoomButton}
            iconClassName={styles.zoomButtonIcon}
            iconSrc={zoomOutIcon}
            onClick={onZoomOut}
            title={intl.formatMessage(messages.zoomOut)}
        />
        
        <Button
            className={styles.zoomButton}
            iconClassName={styles.zoomButtonIcon}
            iconSrc={zoomResetIcon}
            onClick={onZoomReset}
            title={intl.formatMessage(messages.zoomReset)}
        />
        
        <Button
            className={styles.zoomButton}
            iconClassName={styles.zoomButtonIcon}
            iconSrc={zoomInIcon}
            onClick={onZoomIn}
            title={intl.formatMessage(messages.zoomIn)}
        />
    </div>
);

BlockPaletteZoom.propTypes = {
    className: PropTypes.string,
    intl: intlShape.isRequired,
    onZoomIn: PropTypes.func.isRequired,
    onZoomOut: PropTypes.func.isRequired,
    onZoomReset: PropTypes.func.isRequired,
    zoomLevel: PropTypes.number
};

BlockPaletteZoom.defaultProps = {
    zoomLevel: 1.0
};

export default injectIntl(BlockPaletteZoom);