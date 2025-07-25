/**
 * Minecraft Blocks Extension - ブロック建築専用
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

class MinecraftBlocksExtension {
    constructor(runtime) {
        this.runtime = runtime;
        this.websocket = null;
        this.connectionStatus = 'disconnected';
    }

    getInfo() {
        return {
            id: 'minecraftBlocks',
            name: '🧱 Minecraft ブロック',
            color1: '#8B4513',
            color2: '#654321',
            blocks: [
                // 基本ブロック操作
                {
                    opcode: 'placeBlock',
                    blockType: BlockType.COMMAND,
                    text: '🧱 [BLOCK]を X:[X] Y:[Y] Z:[Z] に置く',
                    arguments: {
                        BLOCK: {
                            type: ArgumentType.STRING,
                            menu: 'blockTypes',
                            defaultValue: 'stone'
                        },
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                {
                    opcode: 'removeBlock',
                    blockType: BlockType.COMMAND,
                    text: '⛏️ X:[X] Y:[Y] Z:[Z] のブロックを壊す',
                    arguments: {
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                {
                    opcode: 'getBlockType',
                    blockType: BlockType.REPORTER,
                    text: '🔍 X:[X] Y:[Y] Z:[Z] のブロック名',
                    arguments: {
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                // 範囲操作
                {
                    opcode: 'fillBlocks',
                    blockType: BlockType.COMMAND,
                    text: '🧱 [BLOCK]で範囲埋め X1:[X1] Y1:[Y1] Z1:[Z1] ～ X2:[X2] Y2:[Y2] Z2:[Z2]',
                    arguments: {
                        BLOCK: {
                            type: ArgumentType.STRING,
                            menu: 'blockTypes',
                            defaultValue: 'stone'
                        },
                        X1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y1: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        X2: { type: ArgumentType.NUMBER, defaultValue: 5 },
                        Y2: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z2: { type: ArgumentType.NUMBER, defaultValue: 5 }
                    }
                },
                {
                    opcode: 'clearArea',
                    blockType: BlockType.COMMAND,
                    text: '🧽 範囲クリア X1:[X1] Y1:[Y1] Z1:[Z1] ～ X2:[X2] Y2:[Y2] Z2:[Z2]',
                    arguments: {
                        X1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y1: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        X2: { type: ArgumentType.NUMBER, defaultValue: 5 },
                        Y2: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z2: { type: ArgumentType.NUMBER, defaultValue: 5 }
                    }
                }
            ],
            menus: {
                blockTypes: {
                    acceptReporters: true,
                    items: [
                        // 建築ブロック
                        { text: '石', value: 'stone' },
                        { text: '花崗岩', value: 'granite' },
                        { text: '安山岩', value: 'andesite' },
                        { text: '閃緑岩', value: 'diorite' },
                        { text: 'オークの木材', value: 'oak_planks' },
                        { text: 'スプルースの木材', value: 'spruce_planks' },
                        { text: 'シラカバの木材', value: 'birch_planks' },
                        { text: 'ジャングルの木材', value: 'jungle_planks' },
                        { text: 'アカシアの木材', value: 'acacia_planks' },
                        { text: 'ダークオークの木材', value: 'dark_oak_planks' },
                        // ガラス
                        { text: 'ガラス', value: 'glass' },
                        { text: '白いガラス', value: 'white_stained_glass' },
                        { text: '赤いガラス', value: 'red_stained_glass' },
                        { text: '青いガラス', value: 'blue_stained_glass' },
                        { text: '緑のガラス', value: 'green_stained_glass' },
                        { text: '黄色いガラス', value: 'yellow_stained_glass' },
                        // ウール
                        { text: '白いウール', value: 'white_wool' },
                        { text: '赤いウール', value: 'red_wool' },
                        { text: '青いウール', value: 'blue_wool' },
                        { text: '緑のウール', value: 'green_wool' },
                        { text: '黄色いウール', value: 'yellow_wool' },
                        { text: '紫のウール', value: 'purple_wool' },
                        { text: 'ピンクのウール', value: 'pink_wool' },
                        { text: '黒いウール', value: 'black_wool' },
                        // レンガ・コンクリート
                        { text: 'レンガ', value: 'bricks' },
                        { text: '石レンガ', value: 'stone_bricks' },
                        { text: '白いコンクリート', value: 'white_concrete' },
                        { text: '赤いコンクリート', value: 'red_concrete' },
                        { text: '青いコンクリート', value: 'blue_concrete' },
                        // 特殊
                        { text: '空気', value: 'air' },
                        { text: '水', value: 'water' },
                        { text: '溶岩', value: 'lava' }
                    ]
                }
            }
        };
    }

    // 共通のWebSocket接続を取得
    getWebSocket() {
        // メイン拡張機能のWebSocket接続を利用
        if (typeof window !== 'undefined' && window.minecraftConnection) {
            return window.minecraftConnection;
        }
        return null;
    }

    sendCommand(command, args = {}) {
        const ws = this.getWebSocket();
        if (ws && ws.isConnected()) {
            ws.sendCommand(command, args);
        } else {
            console.warn('Minecraft connection not available');
        }
    }

    // ブロック操作メソッド
    placeBlock(args) {
        this.sendCommand('placeBlock', {
            block: args.BLOCK,
            x: args.X,
            y: args.Y,
            z: args.Z
        });
    }

    removeBlock(args) {
        this.sendCommand('removeBlock', {
            x: args.X,
            y: args.Y,
            z: args.Z
        });
    }

    getBlockType(args) {
        this.sendCommand('getBlock', {
            x: args.X,
            y: args.Y,
            z: args.Z
        });
        // 実際の値は共有状態から取得
        return window.minecraftConnection?.lastBlockInfo || '';
    }

    fillBlocks(args) {
        this.sendCommand('fill', {
            block: args.BLOCK,
            x1: args.X1, y1: args.Y1, z1: args.Z1,
            x2: args.X2, y2: args.Y2, z2: args.Z2
        });
    }

    clearArea(args) {
        this.sendCommand('fill', {
            block: 'air',
            x1: args.X1, y1: args.Y1, z1: args.Z1,
            x2: args.X2, y2: args.Y2, z2: args.Z2
        });
    }
}

module.exports = MinecraftBlocksExtension;