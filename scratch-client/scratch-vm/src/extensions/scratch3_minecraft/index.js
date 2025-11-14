/**
 * MinecraftEdu Scratch Extension
 * Original implementation for controlling Minecraft from Scratch
 */

class Scratch3MinecraftBlocks {
    constructor(runtime) {
        this.runtime = runtime;
        this.socket = null;
        this.connected = false;
        this.sessionId = null;
        this.pendingRequests = new Map();
        this.requestTimeout = 5000;
    }

    getInfo() {
        return {
            id: 'minecraft',
            name: 'Minecraft',
            color1: '#4C97FF',
            color2: '#3373CC',
            color3: '#2E5FA5',
            blocks: [
                {
                    opcode: 'connect',
                    blockType: 'command',
                    text: 'Minecraftに接続 ホスト [HOST] ポート [PORT]',
                    arguments: {
                        HOST: {
                            type: 'string',
                            defaultValue: 'localhost'
                        },
                        PORT: {
                            type: 'number',
                            defaultValue: 14711
                        }
                    }
                },
                {
                    opcode: 'disconnect',
                    blockType: 'command',
                    text: '切断'
                },
                '---',
                {
                    opcode: 'chat',
                    blockType: 'command',
                    text: 'チャットで言う [MESSAGE]',
                    arguments: {
                        MESSAGE: {
                            type: 'string',
                            defaultValue: 'Hello, Minecraft!'
                        }
                    }
                },
                '---',
                {
                    opcode: 'setBlock',
                    blockType: 'command',
                    text: 'ブロックを置く x:[X] y:[Y] z:[Z] ブロック:[BLOCK]',
                    arguments: {
                        X: {
                            type: 'number',
                            defaultValue: 0
                        },
                        Y: {
                            type: 'number',
                            defaultValue: 64
                        },
                        Z: {
                            type: 'number',
                            defaultValue: 0
                        },
                        BLOCK: {
                            type: 'string',
                            menu: 'blockTypes',
                            defaultValue: 'stone'
                        }
                    }
                },
                {
                    opcode: 'setBlockRelative',
                    blockType: 'command',
                    text: 'ブロックを置く ~[X] ~[Y] ~[Z] ブロック:[BLOCK]',
                    arguments: {
                        X: {
                            type: 'number',
                            defaultValue: 0
                        },
                        Y: {
                            type: 'number',
                            defaultValue: 1
                        },
                        Z: {
                            type: 'number',
                            defaultValue: 2
                        },
                        BLOCK: {
                            type: 'string',
                            menu: 'blockTypes',
                            defaultValue: 'stone'
                        }
                    }
                },
                {
                    opcode: 'setBlockRange',
                    blockType: 'command',
                    text: 'X:[X1] Y:[Y1] Z:[Z1] からX:[X2] Y:[Y2] Z:[Z2] ブロック:[BLOCK]',
                    arguments: {
                        X1: {
                            type: 'number',
                            defaultValue: 0
                        },
                        Y1: {
                            type: 'number',
                            defaultValue: 0
                        },
                        Z1: {
                            type: 'number',
                            defaultValue: 0
                        },
                        X2: {
                            type: 'number',
                            defaultValue: 5
                        },
                        Y2: {
                            type: 'number',
                            defaultValue: 5
                        },
                        Z2: {
                            type: 'number',
                            defaultValue: 5
                        },
                        BLOCK: {
                            type: 'string',
                            menu: 'blockTypes',
                            defaultValue: 'stone'
                        }
                    }
                },
                '---',
                {
                    opcode: 'summonEntity',
                    blockType: 'command',
                    text: 'エンティティを召喚 [ENTITY] x:[X] y:[Y] z:[Z]',
                    arguments: {
                        ENTITY: {
                            type: 'string',
                            menu: 'entityTypes',
                            defaultValue: 'pig'
                        },
                        X: {
                            type: 'number',
                            defaultValue: 0
                        },
                        Y: {
                            type: 'number',
                            defaultValue: 64
                        },
                        Z: {
                            type: 'number',
                            defaultValue: 0
                        }
                    }
                },
                '---',
                {
                    opcode: 'teleport',
                    blockType: 'command',
                    text: 'テレポート x:[X] y:[Y] z:[Z]',
                    arguments: {
                        X: {
                            type: 'number',
                            defaultValue: 0
                        },
                        Y: {
                            type: 'number',
                            defaultValue: 64
                        },
                        Z: {
                            type: 'number',
                            defaultValue: 0
                        }
                    }
                },
                {
                    opcode: 'getPosition',
                    blockType: 'reporter',
                    text: 'プレイヤーの位置 [COORD]',
                    arguments: {
                        COORD: {
                            type: 'string',
                            menu: 'coordinates',
                            defaultValue: 'x'
                        }
                    }
                },
                '---',
                {
                    opcode: 'setWeather',
                    blockType: 'command',
                    text: '天気を [WEATHER] にする',
                    arguments: {
                        WEATHER: {
                            type: 'string',
                            menu: 'weatherTypes',
                            defaultValue: 'clear'
                        }
                    }
                },
                {
                    opcode: 'setTime',
                    blockType: 'command',
                    text: '時刻を [TIME] にする',
                    arguments: {
                        TIME: {
                            type: 'string',
                            menu: 'timeValues',
                            defaultValue: 'day'
                        }
                    }
                },
                '---',
                {
                    opcode: 'isConnected',
                    blockType: 'Boolean',
                    text: '接続中？'
                }
            ],
            menus: {
                blockTypes: {
                    acceptReporters: true,
                    items: [
                        'stone', 'dirt', 'grass_block', 'cobblestone',
                        'oak_planks', 'glass', 'sand', 'gravel',
                        'gold_block', 'diamond_block', 'emerald_block',
                        'iron_block', 'coal_block', 'redstone_block',
                        'brick', 'oak_log', 'water', 'lava'
                    ]
                },
                entityTypes: {
                    acceptReporters: true,
                    items: [
                        'pig', 'cow', 'sheep', 'chicken',
                        'zombie', 'skeleton', 'creeper', 'spider',
                        'horse', 'wolf', 'cat', 'villager'
                    ]
                },
                coordinates: {
                    acceptReporters: false,
                    items: ['x', 'y', 'z']
                },
                weatherTypes: {
                    acceptReporters: false,
                    items: ['clear', 'rain', 'thunder']
                },
                timeValues: {
                    acceptReporters: true,
                    items: [
                        { text: '朝', value: 'day' },
                        { text: '昼', value: 'noon' },
                        { text: '夕方', value: 'sunset' },
                        { text: '夜', value: 'night' },
                        { text: '真夜中', value: 'midnight' }
                    ]
                }
            }
        };
    }

    /**
     * 接続
     */
    connect(args) {
        const host = args.HOST || 'localhost';
        const port = args.PORT || 14711;
        const url = `ws://${host}:${port}/minecraft`;

        return new Promise((resolve, reject) => {
            try {
                this.socket = new WebSocket(url);

                this.socket.onopen = () => {
                    console.log('Connected to Minecraft server');

                    // 接続メッセージ送信
                    this.sendMessage({
                        type: 'connect',
                        payload: {
                            clientId: 'scratch_client_' + Date.now(),
                            authToken: 'student-token-scratch',
                            clientInfo: {
                                userAgent: 'Scratch 3.0',
                                version: '0.1.0'
                            }
                        }
                    });

                    this.connected = true;
                    resolve();
                };

                this.socket.onmessage = (event) => {
                    this.handleMessage(JSON.parse(event.data));
                };

                this.socket.onerror = (error) => {
                    console.error('WebSocket error:', error);
                    this.connected = false;
                    reject(error);
                };

                this.socket.onclose = () => {
                    console.log('Disconnected from Minecraft server');
                    this.connected = false;
                };

            } catch (error) {
                console.error('Connection error:', error);
                this.connected = false;
                reject(error);
            }
        });
    }

    /**
     * 切断
     */
    disconnect() {
        if (this.socket) {
            this.socket.close();
            this.socket = null;
            this.connected = false;
            this.sessionId = null;
        }
    }

    /**
     * チャット送信
     */
    chat(args) {
        return this.sendCommand('chat', {
            message: args.MESSAGE
        });
    }

    /**
     * ブロック配置（絶対座標）
     * Y座標変換: ScratchのY=0 → MinecraftのY=-60（スーパーフラット地表）
     */
    setBlock(args) {
        return this.sendCommand('setBlock', {
            x: Number(args.X),
            y: Number(args.Y) - 60,  // Y座標変換: -60でスーパーフラット地表(Y=-60)に対応
            z: Number(args.Z),
            blockType: 'minecraft:' + args.BLOCK
        });
    }

    /**
     * ブロック配置（相対座標）
     */
    setBlockRelative(args) {
        return this.sendCommand('setBlock', {
            relativeX: Number(args.X),
            relativeY: Number(args.Y),
            relativeZ: Number(args.Z),
            blockType: 'minecraft:' + args.BLOCK
        });
    }

    /**
     * ブロック範囲設置
     * Y座標変換: ScratchのY=0 → MinecraftのY=-60（スーパーフラット地表）
     */
    setBlockRange(args) {
        const x1 = Math.floor(Number(args.X1));
        const x2 = Math.floor(Number(args.X2));
        const y1 = Math.floor(Number(args.Y1)) - 60;  // Y座標変換: -60でスーパーフラット地表(Y=-60)に対応
        const y2 = Math.floor(Number(args.Y2)) - 60;  // Y座標変換: -60でスーパーフラット地表(Y=-60)に対応
        const z1 = Math.floor(Number(args.Z1));
        const z2 = Math.floor(Number(args.Z2));
        const blockType = 'minecraft:' + args.BLOCK;

        // 座標を正規化（小さい方から大きい方へ）
        const minX = Math.min(x1, x2);
        const maxX = Math.max(x1, x2);
        const minY = Math.min(y1, y2);
        const maxY = Math.max(y1, y2);
        const minZ = Math.min(z1, z2);
        const maxZ = Math.max(z1, z2);

        // 範囲が大きすぎる場合は制限（最大200,000ブロック）
        const rangeX = maxX - minX + 1;
        const rangeY = maxY - minY + 1;
        const rangeZ = maxZ - minZ + 1;
        const volume = rangeX * rangeY * rangeZ;

        if (volume > 200000) {
            console.warn('範囲が大きすぎます。最大200,000ブロックまでです。');
            return Promise.reject(new Error('範囲が大きすぎます（最大200,000ブロック）'));
        }

        // fillBlocksコマンドでサーバー側に一括処理を依頼
        console.log(`範囲設置開始: ${volume}ブロック (${rangeX}×${rangeY}×${rangeZ})`);

        return this.sendCommand('fillBlocks', {
            from: {
                x: minX,
                y: minY,
                z: minZ
            },
            to: {
                x: maxX,
                y: maxY,
                z: maxZ
            },
            blockType: blockType
        }).then(() => {
            console.log(`範囲設置完了: ${volume}ブロック`);
        }).catch(error => {
            console.error('範囲設置エラー:', error);
            throw error;
        });
    }

    /**
     * エンティティ召喚
     * Y座標変換: ScratchのY=0 → MinecraftのY=-60（スーパーフラット地表）
     */
    summonEntity(args) {
        return this.sendCommand('summonEntity', {
            entityType: 'minecraft:' + args.ENTITY,
            x: Number(args.X),
            y: Number(args.Y) - 60,  // Y座標変換: -60でスーパーフラット地表(Y=-60)に対応
            z: Number(args.Z)
        });
    }

    /**
     * テレポート
     * Y座標変換: ScratchのY=0 → MinecraftのY=-60（スーパーフラット地表）
     */
    teleport(args) {
        return this.sendCommand('teleport', {
            x: Number(args.X),
            y: Number(args.Y) - 60,  // Y座標変換: -60でスーパーフラット地表(Y=-60)に対応
            z: Number(args.Z)
        });
    }

    /**
     * プレイヤー位置取得
     * Y座標変換: MinecraftのY=-60（スーパーフラット地表） → ScratchのY=0
     */
    getPosition(args) {
        const coord = args.COORD || 'x';

        return this.sendCommandWithResponse('getPosition', {})
            .then(response => {
                if (response && response.payload && response.payload.result) {
                    const result = response.payload.result;
                    switch (coord) {
                        case 'x':
                            return result.x || 0;
                        case 'y':
                            // Y座標逆変換: +60でScratch座標系に変換（スーパーフラット地表Y=-60対応）
                            return (result.y || 0) + 60;
                        case 'z':
                            return result.z || 0;
                        default:
                            return 0;
                    }
                }
                return 0;
            })
            .catch(error => {
                console.error('getPosition error:', error);
                return 0;
            });
    }

    /**
     * 天気変更
     */
    setWeather(args) {
        return this.sendCommand('setWeather', {
            weather: args.WEATHER
        });
    }

    /**
     * 時刻変更
     */
    setTime(args) {
        const timeMap = {
            'day': 1000,
            'noon': 6000,
            'sunset': 12000,
            'night': 13000,
            'midnight': 18000
        };

        return this.sendCommand('setTime', {
            time: timeMap[args.TIME] || 1000
        });
    }

    /**
     * 接続状態確認
     */
    isConnected() {
        return this.connected;
    }

    /**
     * コマンド送信
     */
    sendCommand(action, params) {
        if (!this.connected || !this.socket) {
            console.warn('Not connected to Minecraft server');
            return Promise.reject(new Error('Not connected'));
        }

        return this.sendMessage({
            type: 'command',
            payload: {
                action: action,
                params: params
            }
        });
    }

    /**
     * コマンド送信（応答待ち）
     */
    sendCommandWithResponse(action, params) {
        if (!this.connected || !this.socket) {
            console.warn('Not connected to Minecraft server');
            return Promise.reject(new Error('Not connected'));
        }

        const messageId = this.generateUUID();

        return new Promise((resolve, reject) => {
            const timeoutId = setTimeout(() => {
                this.pendingRequests.delete(messageId);
                reject(new Error('Request timeout'));
            }, this.requestTimeout);

            this.pendingRequests.set(messageId, {
                resolve: (response) => {
                    clearTimeout(timeoutId);
                    this.pendingRequests.delete(messageId);
                    resolve(response);
                },
                reject: (error) => {
                    clearTimeout(timeoutId);
                    this.pendingRequests.delete(messageId);
                    reject(error);
                }
            });

            this.sendMessageWithId(messageId, {
                type: 'command',
                payload: {
                    action: action,
                    params: params
                }
            }).catch(error => {
                clearTimeout(timeoutId);
                this.pendingRequests.delete(messageId);
                reject(error);
            });
        });
    }

    /**
     * メッセージ送信
     */
    sendMessage(message) {
        return new Promise((resolve, reject) => {
            try {
                const fullMessage = {
                    version: '1.0',
                    messageId: this.generateUUID(),
                    timestamp: Date.now(),
                    sessionId: this.sessionId || '',
                    ...message
                };

                this.socket.send(JSON.stringify(fullMessage));
                resolve();
            } catch (error) {
                console.error('Send error:', error);
                reject(error);
            }
        });
    }

    /**
     * メッセージ送信（messageId指定）
     */
    sendMessageWithId(messageId, message) {
        return new Promise((resolve, reject) => {
            try {
                const fullMessage = {
                    version: '1.0',
                    messageId: messageId,
                    timestamp: Date.now(),
                    sessionId: this.sessionId || '',
                    ...message
                };

                this.socket.send(JSON.stringify(fullMessage));
                resolve();
            } catch (error) {
                console.error('Send error:', error);
                reject(error);
            }
        });
    }

    /**
     * メッセージ受信処理
     */
    handleMessage(message) {
        console.log('Received message:', message);

        switch (message.type) {
            case 'connect_response':
                if (message.payload.success) {
                    this.sessionId = message.payload.sessionId;
                    console.log('Connected with session:', this.sessionId);
                } else {
                    console.error('Connection failed:', message.payload.errorMessage);
                }

                // 保留中のリクエストを解決
                if (message.requestId && this.pendingRequests.has(message.requestId)) {
                    const pending = this.pendingRequests.get(message.requestId);
                    if (message.payload.success) {
                        pending.resolve(message);
                    } else {
                        pending.reject(new Error(message.payload.errorMessage || 'Connection failed'));
                    }
                }
                break;

            case 'command_response':
                console.log('Command result:', message.payload);

                // 保留中のリクエストを解決
                if (message.requestId && this.pendingRequests.has(message.requestId)) {
                    const pending = this.pendingRequests.get(message.requestId);
                    if (message.payload.success) {
                        pending.resolve(message);
                    } else {
                        pending.reject(new Error(message.payload.errorMessage || 'Command failed'));
                    }
                }
                break;

            case 'query_response':
                console.log('Query result:', message.payload);

                // 保留中のリクエストを解決
                if (message.requestId && this.pendingRequests.has(message.requestId)) {
                    const pending = this.pendingRequests.get(message.requestId);
                    if (message.payload.success) {
                        pending.resolve(message);
                    } else {
                        pending.reject(new Error(message.payload.errorMessage || 'Query failed'));
                    }
                }
                break;

            case 'event':
                console.log('Event received:', message.payload);
                break;

            case 'error':
                console.error('Server error:', message.payload);

                // 保留中のリクエストを解決（エラーとして）
                if (message.requestId && this.pendingRequests.has(message.requestId)) {
                    const pending = this.pendingRequests.get(message.requestId);
                    pending.reject(new Error(message.payload.errorMessage || 'Server error'));
                }
                break;
        }
    }

    /**
     * UUID生成
     */
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }
}

module.exports = Scratch3MinecraftBlocks;
