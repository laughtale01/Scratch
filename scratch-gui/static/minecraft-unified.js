/**
 * シンプルなMinecraft拡張機能 - DataCloneError修正版
 */
(function(Scratch) {
    'use strict';

    if (!Scratch.extensions.unsandboxed) {
        throw new Error('minecraft-unified extension must run unsandboxed');
    }

    const Cast = Scratch.Cast;
    const ArgumentType = Scratch.ArgumentType;
    const BlockType = Scratch.BlockType;

    class MinecraftUnified {
        constructor() {
            this.socket = null;
            this.isConnected = false;
            this.lastMessage = '';
            this.playerInfo = {
                x: 0,
                y: 0, 
                z: 0,
                health: 20
            };
            
            // 自動接続を試行
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
                    // === 接続管理 ===
                    {
                        blockType: BlockType.LABEL,
                        text: '🔌 接続'
                    },
                    {
                        opcode: 'connectToMinecraft',
                        blockType: BlockType.COMMAND,
                        text: 'Minecraftに接続'
                    },
                    {
                        opcode: 'authenticateAsTeacher',
                        blockType: BlockType.COMMAND,
                        text: '🔑 先生として認証'
                    },
                    {
                        opcode: 'authenticateAsStudent',
                        blockType: BlockType.COMMAND,
                        text: '👨‍🎓 生徒として認証'
                    },
                    {
                        opcode: 'isConnected',
                        blockType: BlockType.BOOLEAN,
                        text: '接続中？'
                    },

                    // === 開発モードテスト ===
                    {
                        blockType: BlockType.LABEL,
                        text: '🧪 開発モードテスト'
                    },
                    {
                        opcode: 'testDevMode',
                        blockType: BlockType.COMMAND,
                        text: '💬 開発モードでチャットテスト'
                    },

                    // === チャット ===
                    {
                        blockType: BlockType.LABEL,
                        text: '💬 チャット'
                    },
                    {
                        opcode: 'sendChat',
                        blockType: BlockType.COMMAND,
                        text: 'チャット: [MESSAGE]',
                        arguments: {
                            MESSAGE: {
                                type: ArgumentType.STRING,
                                defaultValue: 'Hello Minecraft!'
                            }
                        }
                    },

                    // === ブロック操作 ===
                    {
                        blockType: BlockType.LABEL,
                        text: '🧱 ブロック操作'
                    },
                    {
                        opcode: 'placeBlock',
                        blockType: BlockType.COMMAND,
                        text: '[BLOCK]を座標[X][Y][Z]に設置',
                        arguments: {
                            BLOCK: {
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

                    // === プレイヤー情報 ===
                    {
                        blockType: BlockType.LABEL,
                        text: '📍 プレイヤー情報'
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
                    }
                ],
                
                menus: {
                    blockTypes: {
                        acceptReporters: true,
                        items: [
                            'stone', 'dirt', 'grass_block', 'cobblestone',
                            'oak_planks', 'glass', 'iron_block', 'gold_block',
                            'diamond_block', 'wool', 'brick'
                        ]
                    }
                }
            };
        }

        // WebSocket接続管理
        initWebSocket() {
            try {
                if (this.socket) {
                    this.socket.close();
                }

                this.socket = new WebSocket('ws://localhost:14711');
                
                this.socket.onopen = () => {
                    this.isConnected = true;
                    console.log('[Minecraft] WebSocket接続が確立されました');
                };
                
                this.socket.onmessage = (event) => {
                    this.handleMessage(event.data);
                };
                
                this.socket.onclose = () => {
                    this.isConnected = false;
                    console.log('[Minecraft] WebSocket接続が切断されました');
                };
                
                this.socket.onerror = (error) => {
                    console.error('[Minecraft] WebSocketエラー:', error);
                    this.isConnected = false;
                };

            } catch (error) {
                console.error('[Minecraft] WebSocket接続に失敗:', error);
                this.isConnected = false;
            }
        }

        // メッセージ処理 - シンプルに文字列のみ扱う
        handleMessage(data) {
            // 必ず文字列として保存
            this.lastMessage = String(data);
            
            try {
                const response = JSON.parse(data);
                
                // プレイヤー座標の更新
                if (response.x !== undefined) {
                    this.playerInfo.x = Number(response.x) || 0;
                }
                if (response.y !== undefined) {
                    this.playerInfo.y = Number(response.y) || 0;
                }
                if (response.z !== undefined) {
                    this.playerInfo.z = Number(response.z) || 0;
                }
                if (response.health !== undefined) {
                    this.playerInfo.health = Number(response.health) || 20;
                }

                console.log('[Minecraft] 受信:', response);
                
            } catch (error) {
                // JSONでない場合はそのまま処理
                console.log('[Minecraft] Raw message:', data);
            }
        }

        // コマンド送信 - 必ずプリミティブ値を返す
        sendCommand(command) {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                console.log('[Minecraft] 送信:', command);
                this.socket.send(command);
                // DataCloneError回避：必ず文字列を返す
                return '';
            } else {
                console.warn('[Minecraft] 接続されていません');
                return '';
            }
        }

        // === ブロック実装 ===

        // 接続
        connectToMinecraft() {
            this.initWebSocket();
            return ''; // 必ず文字列を返す
        }

        // 先生として認証
        authenticateAsTeacher() {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                const authCommand = JSON.stringify({
                    "command": "auth",
                    "username": "testUser"
                });
                console.log('[Minecraft] 先生として認証中: testUser');
                this.socket.send(authCommand);
            } else {
                console.warn('[Minecraft] 接続されていません - 先に接続してください');
            }
            return '';
        }

        // 生徒として認証
        authenticateAsStudent() {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                const authCommand = JSON.stringify({
                    "command": "auth",
                    "username": "testUser2"
                });
                console.log('[Minecraft] 生徒として認証中: testUser2');
                this.socket.send(authCommand);
            } else {
                console.warn('[Minecraft] 接続されていません - 先に接続してください');
            }
            return '';
        }

        // 接続状態
        isConnected() {
            return this.isConnected; // ブール値は安全
        }

        // 開発モードテスト
        testDevMode() {
            console.log('[Minecraft] 開発モードテスト実行中');
            this.sendCommand('chat(DevMode Test from Scratch!)');
            return '';
        }

        // チャット送信
        sendChat(args) {
            const message = String(args.MESSAGE || 'Hello');
            this.sendCommand(`chat(${message})`);
            return ''; // 必ず文字列を返す
        }

        // ブロック設置
        placeBlock(args) {
            const block = String(args.BLOCK || 'stone');
            const x = Number(args.X) || 0;
            const y = Number(args.Y) || 0; 
            const z = Number(args.Z) || 0;
            
            this.sendCommand(`placeBlock(${block},${x},${y},${z})`);
            return ''; // 必ず文字列を返す
        }

        // プレイヤー座標取得
        getPlayerX() {
            // WebSocketで最新情報をリクエスト
            this.sendCommand('getPlayerPos()');
            return this.playerInfo.x; // 数値は安全
        }

        getPlayerY() {
            this.sendCommand('getPlayerPos()');
            return this.playerInfo.y;
        }

        getPlayerZ() {
            this.sendCommand('getPlayerPos()');
            return this.playerInfo.z;
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

    Scratch.extensions.register(new MinecraftUnified());
})(Scratch);