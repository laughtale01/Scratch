/**
 * Minecraft Main Extension - 接続管理・コラボレーション専用
 * Compatible with Minecraft 1.20.1 + Forge
 */

const ArgumentType = {
    STRING: 'string',
    NUMBER: 'number',
    BOOLEAN: 'Boolean'
};

const BlockType = {
    COMMAND: 'command',
    REPORTER: 'reporter',
    HAT: 'hat',
    BOOLEAN: 'Boolean'
};

const formatMessage = require('format-message');

class MinecraftMainExtension {
    constructor(runtime) {
        this.runtime = runtime;
        this.websocket = null;
        this.connectionStatus = 'disconnected';
        this.playerPos = {x: 0, y: 0, z: 0};
        this.lastBlockInfo = '';
        
        // グローバル接続を設定
        if (typeof window !== 'undefined') {
            window.minecraftConnection = this;
        }
    }

    getInfo() {
        return {
            id: 'minecraftMain',
            name: '🎮 Minecraft 接続',
            color1: '#2E7D32',
            color2: '#1B5E20',
            blocks: [
                // 接続管理
                {
                    opcode: 'connect',
                    blockType: BlockType.COMMAND,
                    text: '🔌 Minecraftに接続する'
                },
                {
                    opcode: 'disconnect',
                    blockType: BlockType.COMMAND,
                    text: '🔌 Minecraftから切断する'
                },
                {
                    opcode: 'isConnected',
                    blockType: BlockType.BOOLEAN,
                    text: '📡 接続されている？'
                },
                {
                    opcode: 'getConnectionStatus',
                    blockType: BlockType.REPORTER,
                    text: '📡 接続状態'
                },
                // チャット・コミュニケーション
                {
                    opcode: 'sendChat',
                    blockType: BlockType.COMMAND,
                    text: '💬 チャット: [MESSAGE]',
                    arguments: {
                        MESSAGE: {
                            type: ArgumentType.STRING,
                            defaultValue: 'Hello Minecraft!'
                        }
                    }
                },
                {
                    opcode: 'sendPrivateMessage',
                    blockType: BlockType.COMMAND,
                    text: '📨 [PLAYER]に個人メッセージ: [MESSAGE]',
                    arguments: {
                        PLAYER: {
                            type: ArgumentType.STRING,
                            defaultValue: 'PlayerName'
                        },
                        MESSAGE: {
                            type: ArgumentType.STRING,
                            defaultValue: 'Hello!'
                        }
                    }
                },
                // コラボレーション機能
                {
                    opcode: 'inviteFriend',
                    blockType: BlockType.COMMAND,
                    text: '📧 [FRIEND]さんを招待',
                    arguments: {
                        FRIEND: {
                            type: ArgumentType.STRING,
                            defaultValue: 'FriendName'
                        }
                    }
                },
                {
                    opcode: 'getInvitationCount',
                    blockType: BlockType.REPORTER,
                    text: '📬 招待通知の数'
                },
                {
                    opcode: 'visitFriend',
                    blockType: BlockType.COMMAND,
                    text: '🚪 [FRIEND]さんの世界に訪問申請',
                    arguments: {
                        FRIEND: {
                            type: ArgumentType.STRING,
                            defaultValue: 'FriendName'
                        }
                    }
                },
                {
                    opcode: 'approveVisit',
                    blockType: BlockType.COMMAND,
                    text: '✅ [VISITOR]さんの訪問を承認',
                    arguments: {
                        VISITOR: {
                            type: ArgumentType.STRING,
                            defaultValue: 'VisitorName'
                        }
                    }
                },
                {
                    opcode: 'getCurrentWorld',
                    blockType: BlockType.REPORTER,
                    text: '🌍 現在いる世界'
                },
                {
                    opcode: 'returnHome',
                    blockType: BlockType.COMMAND,
                    text: '🏠 自分のワールドに帰る'
                },
                {
                    opcode: 'emergencyReturn',
                    blockType: BlockType.COMMAND,
                    text: '🚨 緊急帰宅'
                },
                // サーバー情報
                {
                    opcode: 'getOnlinePlayers',
                    blockType: BlockType.REPORTER,
                    text: '👥 オンラインプレイヤー数'
                },
                {
                    opcode: 'getPlayerList',
                    blockType: BlockType.REPORTER,
                    text: '📋 プレイヤーリスト'
                },
                {
                    opcode: 'getServerTPS',
                    blockType: BlockType.REPORTER,
                    text: '⚡ サーバーTPS'
                }
            ]
        };
    }

    // WebSocket接続
    connect() {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
            console.log('Already connected to Minecraft');
            return;
        }

        try {
            this.websocket = new WebSocket('ws://localhost:14711');
            
            this.websocket.onopen = () => {
                this.connectionStatus = 'connected';
                console.log('Minecraft WebSocket connected');
                
                // 接続成功を他の拡張機能に通知
                this.broadcastConnectionStatus(true);
            };

            this.websocket.onmessage = (event) => {
                this.handleMessage(event.data);
            };

            this.websocket.onclose = () => {
                this.connectionStatus = 'disconnected';
                console.log('Minecraft WebSocket disconnected');
                
                // 切断を他の拡張機能に通知
                this.broadcastConnectionStatus(false);
                
                // 自動再接続（5秒後）
                setTimeout(() => {
                    if (this.connectionStatus === 'disconnected') {
                        console.log('Attempting to reconnect...');
                        this.connect();
                    }
                }, 5000);
            };

            this.websocket.onerror = (error) => {
                console.error('WebSocket error:', error);
                this.connectionStatus = 'error';
            };

        } catch (error) {
            console.error('Failed to connect:', error);
            this.connectionStatus = 'error';
        }
    }

    disconnect() {
        if (this.websocket) {
            this.websocket.close();
            this.websocket = null;
        }
        this.connectionStatus = 'disconnected';
        this.broadcastConnectionStatus(false);
    }

    isConnected() {
        return this.connectionStatus === 'connected';
    }

    getConnectionStatus() {
        const statusMap = {
            'connected': '接続済み',
            'disconnected': '切断',
            'connecting': '接続中',
            'error': 'エラー'
        };
        return statusMap[this.connectionStatus] || this.connectionStatus;
    }

    // メッセージハンドリング
    handleMessage(data) {
        try {
            const message = JSON.parse(data);
            
            switch (message.type) {
                case 'playerPos':
                    this.playerPos = message.data;
                    this.updateInfoExtension({ 
                        x: message.data.x, 
                        y: message.data.y, 
                        z: message.data.z 
                    });
                    break;
                    
                case 'playerInfo':
                    this.updateInfoExtension(message.data);
                    break;
                    
                case 'blockInfo':
                    this.lastBlockInfo = message.data;
                    break;
                    
                case 'chatMessage':
                    console.log('Chat:', message.data);
                    break;
                    
                case 'invitation':
                    console.log('Invitation received:', message.data);
                    break;
                    
                default:
                    console.log('Unknown message type:', message.type);
            }
        } catch (error) {
            console.error('Failed to parse message:', error);
        }
    }

    // 他の拡張機能への通知
    broadcastConnectionStatus(connected) {
        // カスタムイベントを発火
        if (typeof window !== 'undefined') {
            window.dispatchEvent(new CustomEvent('minecraftConnectionChange', {
                detail: { connected, extension: this }
            }));
        }
    }

    updateInfoExtension(data) {
        // 情報拡張機能にデータを送信
        if (typeof window !== 'undefined' && window.minecraftInfoExtension) {
            window.minecraftInfoExtension.updatePlayerData(data);
        }
    }

    // コマンド送信（他の拡張機能から使用）
    sendCommand(command, args = {}) {
        if (this.isConnected()) {
            const message = JSON.stringify({
                command: command,
                args: args,
                timestamp: Date.now()
            });
            this.websocket.send(message);
        } else {
            console.warn('Not connected to Minecraft');
        }
    }

    // チャット・コミュニケーション
    sendChat(args) {
        this.sendCommand('chat', {
            message: args.MESSAGE
        });
    }

    sendPrivateMessage(args) {
        this.sendCommand('whisper', {
            player: args.PLAYER,
            message: args.MESSAGE
        });
    }

    // コラボレーション機能
    inviteFriend(args) {
        this.sendCommand('invite', {
            friend: args.FRIEND
        });
    }

    getInvitationCount() {
        this.sendCommand('getInvitations');
        return 0; // プレースホルダー
    }

    visitFriend(args) {
        this.sendCommand('visitRequest', {
            friend: args.FRIEND
        });
    }

    approveVisit(args) {
        this.sendCommand('approveVisit', {
            visitor: args.VISITOR
        });
    }

    getCurrentWorld() {
        this.sendCommand('getCurrentWorld');
        return 'MyWorld'; // プレースホルダー
    }

    returnHome() {
        this.sendCommand('returnHome');
    }

    emergencyReturn() {
        this.sendCommand('emergencyReturn');
    }

    // サーバー情報
    getOnlinePlayers() {
        this.sendCommand('getOnlinePlayers');
        return 1; // プレースホルダー
    }

    getPlayerList() {
        this.sendCommand('getPlayerList');
        return 'Player1, Player2'; // プレースホルダー
    }

    getServerTPS() {
        this.sendCommand('getServerTPS');
        return 20.0; // プレースホルダー
    }
}

module.exports = MinecraftMainExtension;