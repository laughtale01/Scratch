import PropTypes from 'prop-types';
import React from 'react';
import ReactModal from 'react-modal';
import {defineMessages, FormattedMessage, injectIntl, intlShape} from 'react-intl';
import Box from '../box/box.jsx';
import Button from '../button/button.jsx';
import styles from './minecraft-modal.css';

const messages = defineMessages({
    title: {
        id: 'gui.minecraftModal.title',
        defaultMessage: 'Minecraft Connection',
        description: 'Title for Minecraft connection modal'
    }
});

const MinecraftModal = ({intl, isConnected, isConnecting, onConnect, onDisconnect, onCancel, ...props}) => (
    <ReactModal
        isOpen
        className={styles.modalContent}
        contentLabel={intl.formatMessage(messages.title)}
        overlayClassName={styles.modalOverlay}
        onRequestClose={onCancel}
    >
        <Box className={styles.body}>
            <Box className={styles.header}>
                <Box className={styles.headerItem}>
                    <Box className={styles.headerText}>
                        <FormattedMessage {...messages.title} />
                    </Box>
                </Box>
            </Box>
            <Box className={styles.content}>
                <Box className={styles.statusSection}>
                    <Box className={styles.statusIcon}>
                        {isConnected ? '✅' : isConnecting ? '⏳' : '❌'}
                    </Box>
                    <Box className={styles.statusText}>
                        {isConnected ? (
                            <FormattedMessage
                                defaultMessage="Connected to Minecraft"
                                description="Status when connected to Minecraft"
                                id="gui.minecraftModal.connected"
                            />
                        ) : isConnecting ? (
                            <FormattedMessage
                                defaultMessage="Connecting..."
                                description="Status when connecting to Minecraft"
                                id="gui.minecraftModal.connecting"
                            />
                        ) : (
                            <FormattedMessage
                                defaultMessage="Not connected"
                                description="Status when not connected to Minecraft"
                                id="gui.minecraftModal.notConnected"
                            />
                        )}
                    </Box>
                </Box>
                <Box className={styles.instructions}>
                    <FormattedMessage
                        defaultMessage="Make sure Minecraft is running with the collaboration mod installed."
                        description="Instructions for connecting to Minecraft"
                        id="gui.minecraftModal.instructions"
                    />
                </Box>
                <Box className={styles.buttonRow}>
                    {isConnected ? (
                        <Button
                            className={styles.disconnectButton}
                            onClick={onDisconnect}
                        >
                            <FormattedMessage
                                defaultMessage="Disconnect"
                                description="Button to disconnect from Minecraft"
                                id="gui.minecraftModal.disconnect"
                            />
                        </Button>
                    ) : (
                        <Button
                            className={styles.connectButton}
                            disabled={isConnecting}
                            onClick={onConnect}
                        >
                            <FormattedMessage
                                defaultMessage="Connect"
                                description="Button to connect to Minecraft"
                                id="gui.minecraftModal.connect"
                            />
                        </Button>
                    )}
                    <Button
                        className={styles.cancelButton}
                        onClick={onCancel}
                    >
                        <FormattedMessage
                            defaultMessage="Close"
                            description="Button to close modal"
                            id="gui.minecraftModal.close"
                        />
                    </Button>
                </Box>
            </Box>
        </Box>
    </ReactModal>
);

MinecraftModal.propTypes = {
    intl: intlShape.isRequired,
    isConnected: PropTypes.bool,
    isConnecting: PropTypes.bool,
    onConnect: PropTypes.func.isRequired,
    onDisconnect: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired
};

export default injectIntl(MinecraftModal);