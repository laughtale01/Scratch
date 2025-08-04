// Minecraft Extension for Scratch 3.0
(function(Scratch) {
    'use strict';

    if (!Scratch || !Scratch.extensions) {
        console.error('Scratch.extensions not found');
        return;
    }

    const ArgumentType = Scratch.ArgumentType;
    const BlockType = Scratch.BlockType;
    const Cast = Scratch.Cast;

    class MinecraftExtension {
        constructor(runtime) {
            this.runtime = runtime;
            this.websocket = null;
            this.connectionStatus = 'disconnected';
            this.playerPos = { x: 0, y: 0, z: 0 };
            this.lastBlockInfo = '';
            this.connect();
        }

        getInfo() {
            return {
                id: 'minecraft',
                name: 'Minecraft',
                color1: '#4CAF50',
                color2: '#388E3C',
                blocks: [
                    {
                        opcode: 'connect',
                        blockType: BlockType.COMMAND,
                        text: '🔌 Minecraftに接続'
                    },
                    {
                        opcode: 'isConnected',
                        blockType: BlockType.BOOLEAN,
                        text: '📡 接続している'
                    },
                    {
                        opcode: 'placeBlock',
                        blockType: BlockType.COMMAND,
                        text: '🧱 [BLOCK] を x:[X] y:[Y] z:[Z] に置く',
                        arguments: {
                            BLOCK: {
                                type: ArgumentType.STRING,
                                defaultValue: 'stone'
                            },
                            X: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            },
                            Y: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 64
                            },
                            Z: {
                                type: ArgumentType.NUMBER,
                                defaultValue: 0
                            }
                        }
                    },
                    {
                        opcode: 'getPlayerX',
                        blockType: BlockType.REPORTER,
                        text: '📍 プレイヤーのX座標'
                    },
                    {
                        opcode: 'sendChat',
                        blockType: BlockType.COMMAND,
                        text: '💬 チャット: [MESSAGE]',
                        arguments: {
                            MESSAGE: {
                                type: ArgumentType.STRING,
                                defaultValue: 'Hello!'
                            }
                        }
                    }
                ]
            };
        }

        connect() {
            if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
                return;
            }

            try {
                this.websocket = new WebSocket('ws://localhost:14711');
                
                this.websocket.onopen = () => {
                    this.connectionStatus = 'connected';
                    console.log('[Minecraft] Connected');
                };

                this.websocket.onmessage = (event) => {
                    this.handleMessage(event.data);
                };

                this.websocket.onclose = () => {
                    this.connectionStatus = 'disconnected';
                    setTimeout(() => this.connect(), 3000);
                };

                this.websocket.onerror = (error) => {
                    console.error('[Minecraft] Error:', error);
                    this.connectionStatus = 'error';
                };
            } catch (error) {
                console.error('[Minecraft] Failed to connect:', error);
                this.connectionStatus = 'error';
            }
        }

        isConnected() {
            return this.connectionStatus === 'connected';
        }

        handleMessage(data) {
            try {
                const message = JSON.parse(data);
                
                switch (message.type) {
                    case 'playerPos':
                        if (message.data) {
                            this.playerPos = message.data;
                        }
                        break;
                }
            } catch (error) {
                console.error('[Minecraft] Parse error:', error);
            }
        }

        sendCommand(command, args = {}) {
            if (!this.isConnected()) {
                return Promise.resolve();
            }

            const message = JSON.stringify({
                command: command,
                args: args
            });

            try {
                this.websocket.send(message);
                return new Promise(resolve => setTimeout(resolve, 100));
            } catch (error) {
                console.error('[Minecraft] Send error:', error);
                return Promise.resolve();
            }
        }

        placeBlock(args) {
            return this.sendCommand('placeBlock', {
                block: args.BLOCK,
                x: Cast.toNumber(args.X),
                y: Cast.toNumber(args.Y),
                z: Cast.toNumber(args.Z)
            });
        }

        getPlayerX() {
            this.sendCommand('getPlayerPos');
            return this.playerPos.x;
        }

        sendChat(args) {
            return this.sendCommand('chat', {
                message: Cast.toString(args.MESSAGE)
            });
        }
    }

    Scratch.extensions.register(new MinecraftExtension());
    
})(window.Scratch);