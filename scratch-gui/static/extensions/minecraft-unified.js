(function(Scratch) {
    'use strict';

    if (!Scratch.extensions) {
        throw new Error('Scratch.extensions is not defined');
    }

    class MinecraftExtension {
        constructor(runtime) {
            this.runtime = runtime;
        }

        getInfo() {
            return {
                id: 'minecraftUnified',
                name: '🎮 Minecraft Controller',
                color1: '#4CAF50',
                color2: '#388E3C',
                blocks: [
                    // 接続管理
                    {
                        opcode: 'connect',
                        blockType: Scratch.BlockType.COMMAND,
                        text: '🔌 Minecraftに接続する'
                    },
                    {
                        opcode: 'isConnected',
                        blockType: Scratch.BlockType.BOOLEAN,
                        text: '📡 接続されている？'
                    },
                    // ブロック操作
                    {
                        opcode: 'placeBlock',
                        blockType: Scratch.BlockType.COMMAND,
                        text: '🧱 [BLOCK]を X:[X] Y:[Y] Z:[Z] に置く',
                        arguments: {
                            BLOCK: {
                                type: Scratch.ArgumentType.STRING,
                                menu: 'blockTypes',
                                defaultValue: 'stone'
                            },
                            X: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Y: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Z: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 0
                            }
                        }
                    },
                    // プレイヤー操作
                    {
                        opcode: 'getPlayerX',
                        blockType: Scratch.BlockType.REPORTER,
                        text: '📍 プレイヤーのX座標'
                    },
                    {
                        opcode: 'getPlayerY',
                        blockType: Scratch.BlockType.REPORTER,
                        text: '📍 プレイヤーのY座標'
                    },
                    {
                        opcode: 'getPlayerZ',
                        blockType: Scratch.BlockType.REPORTER,
                        text: '📍 プレイヤーのZ座標'
                    },
                    // チャット
                    {
                        opcode: 'sendChat',
                        blockType: Scratch.BlockType.COMMAND,
                        text: '💬 チャット: [MESSAGE]',
                        arguments: {
                            MESSAGE: {
                                type: Scratch.ArgumentType.STRING,
                                defaultValue: 'Hello Minecraft!'
                            }
                        }
                    },
                    // 建築支援
                    {
                        opcode: 'buildCircle',
                        blockType: Scratch.BlockType.COMMAND,
                        text: '⭕ [BLOCK]で 中心X:[X] Y:[Y] Z:[Z] 半径:[RADIUS] の円を作る',
                        arguments: {
                            BLOCK: {
                                type: Scratch.ArgumentType.STRING,
                                menu: 'blockTypes',
                                defaultValue: 'stone'
                            },
                            X: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Y: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Z: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            RADIUS: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 5
                            }
                        }
                    },
                    // テレポート
                    {
                        opcode: 'teleportPlayer',
                        blockType: Scratch.BlockType.COMMAND,
                        text: '🚀 プレイヤーを X:[X] Y:[Y] Z:[Z] にテレポート',
                        arguments: {
                            X: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Y: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Z: {
                                type: Scratch.ArgumentType.NUMBER,
                                defaultValue: 0
                            }
                        }
                    }
                ],
                menus: {
                    blockTypes: {
                        acceptReporters: true,
                        items: [
                            {text: '石', value: 'stone'},
                            {text: '土', value: 'dirt'},
                            {text: '草ブロック', value: 'grass_block'},
                            {text: '丸石', value: 'cobblestone'},
                            {text: '木材', value: 'oak_planks'},
                            {text: 'レンガ', value: 'bricks'},
                            {text: 'ガラス', value: 'glass'},
                            {text: '砂', value: 'sand'}
                        ]
                    }
                }
            };
        }

        // 接続
        connect() {
            // Worker環境チェック
            if (typeof self !== 'undefined' && typeof self.importScripts === 'function') {
                // Worker内では実行しない
                return;
            }
            
            // メインスレッドで実行
            if (typeof window !== 'undefined') {
                if (!window.minecraftWS) {
                    window.minecraftWS = {
                        socket: null,
                        connected: false,
                        playerPos: {x: 0, y: 0, z: 0}
                    };
                }
                
                if (window.minecraftWS.socket && window.minecraftWS.socket.readyState === 1) {
                    console.log('Already connected');
                    return;
                }
                
                try {
                    window.minecraftWS.socket = new WebSocket('ws://localhost:14711');
                    
                    window.minecraftWS.socket.onopen = function() {
                        console.log('Connected to Minecraft');
                        window.minecraftWS.connected = true;
                        window.minecraftWS.socket.send(JSON.stringify({type: 'ping'}));
                    };
                    
                    window.minecraftWS.socket.onmessage = function(event) {
                        try {
                            const data = JSON.parse(event.data);
                            console.log('Received:', data);
                            if (data.type === 'playerPos') {
                                window.minecraftWS.playerPos = {
                                    x: data.x || 0,
                                    y: data.y || 0,
                                    z: data.z || 0
                                };
                            }
                        } catch (e) {
                            console.log('Received non-JSON:', event.data);
                        }
                    };
                    
                    window.minecraftWS.socket.onclose = function() {
                        console.log('Disconnected from Minecraft');
                        window.minecraftWS.connected = false;
                    };
                    
                    window.minecraftWS.socket.onerror = function(error) {
                        console.error('WebSocket error:', error);
                        window.minecraftWS.connected = false;
                    };
                    
                } catch (error) {
                    console.error('Failed to connect:', error);
                }
            }
        }

        isConnected() {
            if (typeof self !== 'undefined' && typeof self.importScripts === 'function') {
                return false;
            }
            
            if (typeof window !== 'undefined' && window.minecraftWS) {
                return window.minecraftWS.connected;
            }
            return false;
        }

        // ブロック配置
        placeBlock(args) {
            if (typeof self !== 'undefined' && typeof self.importScripts === 'function') {
                return;
            }
            
            if (typeof window !== 'undefined' && window.minecraftWS && window.minecraftWS.socket && window.minecraftWS.socket.readyState === 1) {
                const message = {
                    type: 'placeBlock',
                    block: String(args.BLOCK),
                    x: Number(args.X),
                    y: Number(args.Y),
                    z: Number(args.Z)
                };
                window.minecraftWS.socket.send(JSON.stringify(message));
            }
        }

        // プレイヤー座標
        getPlayerX() {
            if (typeof self !== 'undefined' && typeof self.importScripts === 'function') {
                return 0;
            }
            
            if (typeof window !== 'undefined' && window.minecraftWS) {
                if (window.minecraftWS.socket && window.minecraftWS.socket.readyState === 1) {
                    window.minecraftWS.socket.send(JSON.stringify({type: 'getPlayerPos'}));
                }
                return window.minecraftWS.playerPos.x;
            }
            return 0;
        }

        getPlayerY() {
            if (typeof self !== 'undefined' && typeof self.importScripts === 'function') {
                return 0;
            }
            
            if (typeof window !== 'undefined' && window.minecraftWS) {
                if (window.minecraftWS.socket && window.minecraftWS.socket.readyState === 1) {
                    window.minecraftWS.socket.send(JSON.stringify({type: 'getPlayerPos'}));
                }
                return window.minecraftWS.playerPos.y;
            }
            return 0;
        }

        getPlayerZ() {
            if (typeof self !== 'undefined' && typeof self.importScripts === 'function') {
                return 0;
            }
            
            if (typeof window !== 'undefined' && window.minecraftWS) {
                if (window.minecraftWS.socket && window.minecraftWS.socket.readyState === 1) {
                    window.minecraftWS.socket.send(JSON.stringify({type: 'getPlayerPos'}));
                }
                return window.minecraftWS.playerPos.z;
            }
            return 0;
        }

        // チャット
        sendChat(args) {
            if (typeof self !== 'undefined' && typeof self.importScripts === 'function') {
                return;
            }
            
            if (typeof window !== 'undefined' && window.minecraftWS && window.minecraftWS.socket && window.minecraftWS.socket.readyState === 1) {
                const message = {
                    type: 'chat',
                    message: String(args.MESSAGE)
                };
                window.minecraftWS.socket.send(JSON.stringify(message));
            }
        }

        // 円を作る
        buildCircle(args) {
            if (typeof self !== 'undefined' && typeof self.importScripts === 'function') {
                return;
            }
            
            if (typeof window !== 'undefined' && window.minecraftWS && window.minecraftWS.socket && window.minecraftWS.socket.readyState === 1) {
                const message = {
                    type: 'buildCircle',
                    block: String(args.BLOCK),
                    x: Number(args.X),
                    y: Number(args.Y),
                    z: Number(args.Z),
                    radius: Number(args.RADIUS)
                };
                window.minecraftWS.socket.send(JSON.stringify(message));
            }
        }

        // テレポート
        teleportPlayer(args) {
            if (typeof self !== 'undefined' && typeof self.importScripts === 'function') {
                return;
            }
            
            if (typeof window !== 'undefined' && window.minecraftWS && window.minecraftWS.socket && window.minecraftWS.socket.readyState === 1) {
                const message = {
                    type: 'teleportPlayer',
                    x: Number(args.X),
                    y: Number(args.Y),
                    z: Number(args.Z)
                };
                window.minecraftWS.socket.send(JSON.stringify(message));
            }
        }
    }

    Scratch.extensions.register(new MinecraftExtension());
})(Scratch);