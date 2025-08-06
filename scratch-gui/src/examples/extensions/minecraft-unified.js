/**
 * 統合されたMinecraft拡張機能
 * 全てのMinecraft機能を一つの拡張機能に統合
 */

(function(Scratch) {
    'use strict';

    // Scratch 3.0 拡張機能として動作するための設定
    if (!Scratch || !Scratch.extensions) {
        throw new Error('This extension requires Scratch extensions API');
    }

    const ArgumentType = Scratch.ArgumentType || Scratch.Argument;
    const BlockType = Scratch.BlockType || Scratch.Block;
    const Cast = Scratch.Cast || {
        toNumber: (value) => Number(value),
        toString: (value) => String(value),
        toBoolean: (value) => Boolean(value)
    };

    class MinecraftUnified {
        constructor() {
            this.socket = null;
            this.isConnected = false;
            this.lastMessage = '';
            this.playerInfo = {};
            this.zoomLevel = 1.0;
            
            // WebSocket接続の初期化
            this.initWebSocket();
        }

        getInfo() {
            return {
                id: 'minecraftUnified',
                name: '🎮 Minecraft',
                color1: '#8FBC8F',
                color2: '#7A8B7A',
                color3: '#6B7B6B',
                menuIconURI: this.getMenuIconURI(),
                blockIconURI: this.getBlockIconURI(),
                blocks: [
                    // === 接続・設定カテゴリ ===
                    {
                        blockType: BlockType.LABEL,
                        text: '🔌 接続・設定'
                    },
                    {
                        opcode: 'connectToMinecraft',
                        blockType: BlockType.COMMAND,
                        text: 'Minecraftに接続',
                        arguments: {}
                    },
                    {
                        opcode: 'disconnectFromMinecraft',
                        blockType: BlockType.COMMAND,
                        text: 'Minecraftから切断',
                        arguments: {}
                    },
                    {
                        opcode: 'isConnected',
                        blockType: BlockType.BOOLEAN,
                        text: '接続中？',
                        arguments: {}
                    },

                    // === ブロック操作カテゴリ ===
                    {
                        blockType: BlockType.LABEL,
                        text: '🧱 ブロック操作'
                    },
                    {
                        opcode: 'placeBlock',
                        blockType: BlockType.COMMAND,
                        text: '[BLOCK_TYPE]を座標[X][Y][Z]に設置',
                        arguments: {
                            BLOCK_TYPE: {
                                type: ArgumentType.STRING,
                                menu: 'blockTypes',
                                defaultValue: 'stone'
                            },
                            X: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Y: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Z: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            }
                        }
                    },
                    {
                        opcode: 'breakBlock',
                        blockType: BlockType.COMMAND,
                        text: '座標[X][Y][Z]のブロックを破壊',
                        arguments: {
                            X: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Y: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Z: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            }
                        }
                    },
                    {
                        opcode: 'getBlockType',
                        blockType: BlockType.REPORTER,
                        text: '座標[X][Y][Z]のブロック',
                        arguments: {
                            X: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Y: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Z: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            }
                        }
                    },
                    {
                        opcode: 'fillBlocks',
                        blockType: BlockType.COMMAND,
                        text: '[BLOCK_TYPE]で[X1][Y1][Z1]から[X2][Y2][Z2]を埋める',
                        arguments: {
                            BLOCK_TYPE: {
                                type: ArgumentType.STRING,
                                menu: 'blockTypes',
                                defaultValue: 'stone'
                            },
                            X1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            Y1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            Z1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            X2: { type: ArgumentType.NUMBER, defaultValue: 10 },
                            Y2: { type: ArgumentType.NUMBER, defaultValue: 10 },
                            Z2: { type: ArgumentType.NUMBER, defaultValue: 10 }
                        }
                    },

                    // === 建築カテゴリ ===
                    {
                        blockType: BlockType.LABEL,
                        text: '🏗️ 建築'
                    },
                    {
                        opcode: 'buildWall',
                        blockType: BlockType.COMMAND,
                        text: '[BLOCK_TYPE]で壁を[X][Y][Z]から高さ[HEIGHT]幅[WIDTH]で建築',
                        arguments: {
                            BLOCK_TYPE: {
                                type: ArgumentType.STRING,
                                menu: 'blockTypes',
                                defaultValue: 'stone_bricks'
                            },
                            X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            HEIGHT: { type: ArgumentType.NUMBER, defaultValue: 5 },
                            WIDTH: { type: ArgumentType.NUMBER, defaultValue: 10 }
                        }
                    },
                    {
                        opcode: 'buildHouse',
                        blockType: BlockType.COMMAND,
                        text: '[STYLE]スタイルの家を[X][Y][Z]にサイズ[SIZE]で建築',
                        arguments: {
                            STYLE: {
                                type: ArgumentType.STRING,
                                menu: 'houseStyles',
                                defaultValue: 'simple'
                            },
                            X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            SIZE: {
                                type: ArgumentType.STRING,
                                menu: 'sizes',
                                defaultValue: 'medium'
                            }
                        }
                    },
                    {
                        opcode: 'buildCircle',
                        blockType: BlockType.COMMAND,
                        text: '[BLOCK_TYPE]で円を[X][Y][Z]に半径[RADIUS]で建築',
                        arguments: {
                            BLOCK_TYPE: {
                                type: ArgumentType.STRING,
                                menu: 'blockTypes',
                                defaultValue: 'stone'
                            },
                            X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            RADIUS: { type: ArgumentType.NUMBER, defaultValue: 5 }
                        }
                    },

                    // === コマンドカテゴリ ===
                    {
                        blockType: BlockType.LABEL,
                        text: '⚡ コマンド'
                    },
                    {
                        opcode: 'teleportPlayer',
                        blockType: BlockType.COMMAND,
                        text: 'プレイヤーを[X][Y][Z]にテレポート',
                        arguments: {
                            X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                            Y: { type: ArgumentType.NUMBER, defaultValue: 100 },
                            Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                        }
                    },
                    {
                        opcode: 'changeGameMode',
                        blockType: BlockType.COMMAND,
                        text: 'ゲームモードを[MODE]に変更',
                        arguments: {
                            MODE: {
                                type: ArgumentType.STRING,
                                menu: 'gameModes',
                                defaultValue: 'creative'
                            }
                        }
                    },
                    {
                        opcode: 'giveItem',
                        blockType: BlockType.COMMAND,
                        text: '[ITEM]を[AMOUNT]個付与',
                        arguments: {
                            ITEM: {
                                type: ArgumentType.STRING,
                                menu: 'items',
                                defaultValue: 'diamond_sword'
                            },
                            AMOUNT: { type: ArgumentType.NUMBER, defaultValue: 1 }
                        }
                    },
                    {
                        opcode: 'sendChatMessage',
                        blockType: BlockType.COMMAND,
                        text: 'チャットに[MESSAGE]を送信',
                        arguments: {
                            MESSAGE: {
                                type: ArgumentType.STRING,
                                defaultValue: 'Hello from Scratch!'
                            }
                        }
                    },

                    // === 情報表示カテゴリ ===
                    {
                        blockType: BlockType.LABEL,
                        text: '📍 情報表示'
                    },
                    {
                        opcode: 'getPlayerX',
                        blockType: BlockType.REPORTER,
                        text: 'プレイヤーのX座標'
                    },
                    {
                        opcode: 'getPlayerY',
                        blockType: BlockType.REPORTER,
                        text: 'プレイヤーのY座標'
                    },
                    {
                        opcode: 'getPlayerZ',
                        blockType: BlockType.REPORTER,
                        text: 'プレイヤーのZ座標'
                    },
                    {
                        opcode: 'getPlayerHealth',
                        blockType: BlockType.REPORTER,
                        text: 'プレイヤーの体力'
                    },
                    {
                        opcode: 'getPlayerDirection',
                        blockType: BlockType.REPORTER,
                        text: 'プレイヤーの向き'
                    },
                    {
                        opcode: 'getBiome',
                        blockType: BlockType.REPORTER,
                        text: '現在のバイオーム'
                    },

                    // === コラボレーションカテゴリ ===
                    {
                        blockType: BlockType.LABEL,
                        text: '👥 コラボレーション'
                    },
                    {
                        opcode: 'inviteFriend',
                        blockType: BlockType.COMMAND,
                        text: '[FRIEND_NAME]を招待',
                        arguments: {
                            FRIEND_NAME: {
                                type: ArgumentType.STRING,
                                defaultValue: 'friend'
                            }
                        }
                    },
                    {
                        opcode: 'visitWorld',
                        blockType: BlockType.COMMAND,
                        text: '[WORLD_NAME]のワールドを訪問',
                        arguments: {
                            WORLD_NAME: {
                                type: ArgumentType.STRING,
                                defaultValue: 'world'
                            }
                        }
                    },
                    {
                        opcode: 'returnHome',
                        blockType: BlockType.COMMAND,
                        text: '自分のワールドに帰る'
                    },
                    {
                        opcode: 'emergencyReturn',
                        blockType: BlockType.COMMAND,
                        text: '緊急帰宅'
                    }
                ],
                
                menus: {
                    blockTypes: {
                        acceptReporters: true,
                        items: [
                            'stone', 'dirt', 'grass_block', 'cobblestone', 'wood_planks',
                            'stone_bricks', 'brick', 'sandstone', 'glass', 'wool',
                            'iron_block', 'gold_block', 'diamond_block', 'emerald_block',
                            'obsidian', 'bedrock', 'water', 'lava'
                        ]
                    },
                    houseStyles: {
                        acceptReporters: true,
                        items: ['simple', 'modern', 'medieval', 'japanese', 'castle']
                    },
                    sizes: {
                        acceptReporters: true,
                        items: ['small', 'medium', 'large', 'huge']
                    },
                    gameModes: {
                        acceptReporters: true,
                        items: ['survival', 'creative', 'adventure', 'spectator']
                    },
                    items: {
                        acceptReporters: true,
                        items: [
                            'diamond_sword', 'iron_pickaxe', 'golden_apple', 'bread',
                            'arrow', 'bow', 'shield', 'elytra', 'diamond_armor',
                            'redstone', 'tnt', 'torch', 'book', 'map'
                        ]
                    }
                }
            };
        }

        // WebSocket接続管理
        initWebSocket() {
            try {
                this.socket = new WebSocket('ws://localhost:14711');
                
                this.socket.onopen = () => {
                    this.isConnected = true;
                    console.log('Minecraft WebSocket接続が確立されました');
                };
                
                this.socket.onmessage = (event) => {
                    this.lastMessage = event.data;
                    this.handleMessage(event.data);
                };
                
                this.socket.onclose = () => {
                    this.isConnected = false;
                    console.log('Minecraft WebSocket接続が切断されました');
                };
                
                this.socket.onerror = (error) => {
                    console.error('WebSocketエラー:', error);
                    this.isConnected = false;
                };
            } catch (error) {
                console.error('WebSocket接続に失敗:', error);
                this.isConnected = false;
            }
        }

        handleMessage(data) {
            try {
                const response = JSON.parse(data);
                if (response.type === 'playerInfo') {
                    this.playerInfo = response.data;
                }
            } catch (error) {
                // JSONでない場合はそのまま保存
                this.lastMessage = data;
            }
        }

        sendCommand(command) {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.socket.send(command);
                return new Promise((resolve) => {
                    setTimeout(() => resolve(this.lastMessage), 100);
                });
            } else {
                throw new Error('Minecraftに接続されていません');
            }
        }

        // === 接続・設定ブロック ===
        connectToMinecraft() {
            this.initWebSocket();
            return new Promise((resolve) => {
                setTimeout(() => resolve(), 1000);
            });
        }

        disconnectFromMinecraft() {
            if (this.socket) {
                this.socket.close();
                this.isConnected = false;
            }
        }

        isConnected() {
            return this.isConnected;
        }

        // === ブロック操作ブロック ===
        placeBlock(args) {
            return this.sendCommand(`placeBlock(${args.BLOCK_TYPE},${args.X},${args.Y},${args.Z})`);
        }

        breakBlock(args) {
            return this.sendCommand(`breakBlock(${args.X},${args.Y},${args.Z})`);
        }

        getBlockType(args) {
            return this.sendCommand(`getBlock(${args.X},${args.Y},${args.Z})`);
        }

        fillBlocks(args) {
            return this.sendCommand(`fill(${args.X1},${args.Y1},${args.Z1},${args.X2},${args.Y2},${args.Z2},${args.BLOCK_TYPE})`);
        }

        // === 建築ブロック ===
        buildWall(args) {
            return this.sendCommand(`buildWall(${args.BLOCK_TYPE},${args.X},${args.Y},${args.Z},${args.HEIGHT},${args.WIDTH})`);
        }

        buildHouse(args) {
            return this.sendCommand(`buildHouse(${args.STYLE},${args.X},${args.Y},${args.Z},${args.SIZE})`);
        }

        buildCircle(args) {
            return this.sendCommand(`buildCircle(${args.BLOCK_TYPE},${args.X},${args.Y},${args.Z},${args.RADIUS})`);
        }

        // === コマンドブロック ===
        teleportPlayer(args) {
            return this.sendCommand(`teleport(${args.X},${args.Y},${args.Z})`);
        }

        changeGameMode(args) {
            return this.sendCommand(`gamemode(${args.MODE})`);
        }

        giveItem(args) {
            return this.sendCommand(`give(${args.ITEM},${args.AMOUNT})`);
        }

        sendChatMessage(args) {
            return this.sendCommand(`chat(${args.MESSAGE})`);
        }

        // === 情報表示ブロック ===
        getPlayerX() {
            return this.playerInfo.x || 0;
        }

        getPlayerY() {
            return this.playerInfo.y || 0;
        }

        getPlayerZ() {
            return this.playerInfo.z || 0;
        }

        getPlayerHealth() {
            return this.playerInfo.health || 20;
        }

        getPlayerDirection() {
            return this.playerInfo.direction || 'north';
        }

        getBiome() {
            return this.playerInfo.biome || 'plains';
        }

        // === コラボレーションブロック ===
        inviteFriend(args) {
            return this.sendCommand(`invite(${args.FRIEND_NAME})`);
        }

        visitWorld(args) {
            return this.sendCommand(`visit(${args.WORLD_NAME})`);
        }

        returnHome() {
            return this.sendCommand('returnHome()');
        }

        emergencyReturn() {
            return this.sendCommand('emergencyReturn()');
        }

        // アイコン
        getMenuIconURI() {
            return 'data:image/svg+xml;base64,' + btoa(`
                <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40">
                    <rect width="40" height="40" fill="#8FBC8F" rx="8"/>
                    <rect x="8" y="8" width="8" height="8" fill="#228B22"/>
                    <rect x="24" y="8" width="8" height="8" fill="#32CD32"/>
                    <rect x="8" y="24" width="8" height="8" fill="#32CD32"/>
                    <rect x="24" y="24" width="8" height="8" fill="#228B22"/>
                    <rect x="16" y="16" width="8" height="8" fill="#90EE90"/>
                </svg>
            `);
        }

        getBlockIconURI() {
            return 'data:image/svg+xml;base64,' + btoa(`
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20">
                    <rect width="20" height="20" fill="#8FBC8F" rx="4"/>
                    <rect x="4" y="4" width="4" height="4" fill="#228B22"/>
                    <rect x="12" y="4" width="4" height="4" fill="#32CD32"/>
                    <rect x="4" y="12" width="4" height="4" fill="#32CD32"/>
                    <rect x="12" y="12" width="4" height="4" fill="#228B22"/>
                    <rect x="8" y="8" width="4" height="4" fill="#90EE90"/>
                </svg>
            `);
        }
    }

    if (Scratch.extensions) {
        Scratch.extensions.register(new MinecraftUnified());
    } else {
        console.error('Scratch extensions not available');
    }
})(Scratch);