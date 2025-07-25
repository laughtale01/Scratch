/**
 * Minecraft Build Extension - 建築・形状作成専用
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

class MinecraftBuildExtension {
    constructor(runtime) {
        this.runtime = runtime;
    }

    getInfo() {
        return {
            id: 'minecraftBuild',
            name: '🏗️ Minecraft 建築',
            color1: '#FF8C00',
            color2: '#FF7F00',
            blocks: [
                // 基本形状
                {
                    opcode: 'buildWall',
                    blockType: BlockType.COMMAND,
                    text: '🧱 [BLOCK]で壁を作る X1:[X1] Z1:[Z1] ～ X2:[X2] Z2:[Z2] 高さ:[HEIGHT]',
                    arguments: {
                        BLOCK: {
                            type: ArgumentType.STRING,
                            menu: 'buildMaterials',
                            defaultValue: 'stone_bricks'
                        },
                        X1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Z1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        X2: { type: ArgumentType.NUMBER, defaultValue: 10 },
                        Z2: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        HEIGHT: { type: ArgumentType.NUMBER, defaultValue: 3 }
                    }
                },
                {
                    opcode: 'buildFloor',
                    blockType: BlockType.COMMAND,
                    text: '🏠 [BLOCK]で床を作る X1:[X1] Z1:[Z1] ～ X2:[X2] Z2:[Z2] 高さ:[Y]',
                    arguments: {
                        BLOCK: {
                            type: ArgumentType.STRING,
                            menu: 'buildMaterials',
                            defaultValue: 'oak_planks'
                        },
                        X1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Z1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        X2: { type: ArgumentType.NUMBER, defaultValue: 10 },
                        Z2: { type: ArgumentType.NUMBER, defaultValue: 10 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 }
                    }
                },
                {
                    opcode: 'buildRoof',
                    blockType: BlockType.COMMAND,
                    text: '🏠 [BLOCK]で屋根を作る 中心X:[X] Z:[Z] 幅:[WIDTH] 奥行:[DEPTH] 高さ:[Y]',
                    arguments: {
                        BLOCK: {
                            type: ArgumentType.STRING,
                            menu: 'roofMaterials',
                            defaultValue: 'oak_stairs'
                        },
                        X: { type: ArgumentType.NUMBER, defaultValue: 5 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 5 },
                        WIDTH: { type: ArgumentType.NUMBER, defaultValue: 11 },
                        DEPTH: { type: ArgumentType.NUMBER, defaultValue: 11 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 68 }
                    }
                },
                // 幾何学形状
                {
                    opcode: 'buildCircle',
                    blockType: BlockType.COMMAND,
                    text: '⭕ [BLOCK]で円を作る 中心X:[X] Y:[Y] Z:[Z] 半径:[RADIUS]',
                    arguments: {
                        BLOCK: {
                            type: ArgumentType.STRING,
                            menu: 'buildMaterials',
                            defaultValue: 'stone'
                        },
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        RADIUS: { type: ArgumentType.NUMBER, defaultValue: 5 }
                    }
                },
                {
                    opcode: 'buildSphere',
                    blockType: BlockType.COMMAND,
                    text: '🌕 [BLOCK]で球を作る 中心X:[X] Y:[Y] Z:[Z] 半径:[RADIUS]',
                    arguments: {
                        BLOCK: {
                            type: ArgumentType.STRING,
                            menu: 'buildMaterials',
                            defaultValue: 'glass'
                        },
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 70 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        RADIUS: { type: ArgumentType.NUMBER, defaultValue: 5 }
                    }
                },
                {
                    opcode: 'buildTower',
                    blockType: BlockType.COMMAND,
                    text: '🗼 [BLOCK]で塔を作る X:[X] Z:[Z] 幅:[WIDTH] 高さ:[HEIGHT]',
                    arguments: {
                        BLOCK: {
                            type: ArgumentType.STRING,
                            menu: 'buildMaterials',
                            defaultValue: 'stone_bricks'
                        },
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        WIDTH: { type: ArgumentType.NUMBER, defaultValue: 5 },
                        HEIGHT: { type: ArgumentType.NUMBER, defaultValue: 20 }
                    }
                },
                // 完全な建造物
                {
                    opcode: 'buildHouse',
                    blockType: BlockType.COMMAND,
                    text: '🏠 [STYLE]の家を建てる X:[X] Y:[Y] Z:[Z] サイズ:[SIZE]',
                    arguments: {
                        STYLE: {
                            type: ArgumentType.STRING,
                            menu: 'houseStyles',
                            defaultValue: 'modern'
                        },
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        SIZE: {
                            type: ArgumentType.STRING,
                            menu: 'houseSizes',
                            defaultValue: 'medium'
                        }
                    }
                },
                {
                    opcode: 'buildCastle',
                    blockType: BlockType.COMMAND,
                    text: '🏰 城を建てる X:[X] Y:[Y] Z:[Z] サイズ:[SIZE]',
                    arguments: {
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        SIZE: {
                            type: ArgumentType.STRING,
                            menu: 'castleSizes',
                            defaultValue: 'small'
                        }
                    }
                }
            ],
            menus: {
                buildMaterials: {
                    acceptReporters: true,
                    items: [
                        { text: '石レンガ', value: 'stone_bricks' },
                        { text: 'オークの木材', value: 'oak_planks' },
                        { text: 'スプルースの木材', value: 'spruce_planks' },
                        { text: 'レンガ', value: 'bricks' },
                        { text: '磨かれた花崗岩', value: 'polished_granite' },
                        { text: '磨かれた安山岩', value: 'polished_andesite' },
                        { text: '磨かれた閃緑岩', value: 'polished_diorite' },
                        { text: '白いコンクリート', value: 'white_concrete' },
                        { text: 'ガラス', value: 'glass' },
                        { text: '石', value: 'stone' }
                    ]
                },
                roofMaterials: {
                    acceptReporters: true,
                    items: [
                        { text: 'オークの階段', value: 'oak_stairs' },
                        { text: 'スプルースの階段', value: 'spruce_stairs' },
                        { text: '石レンガの階段', value: 'stone_brick_stairs' },
                        { text: 'レンガの階段', value: 'brick_stairs' },
                        { text: '赤いテラコッタ', value: 'red_terracotta' },
                        { text: '茶色のテラコッタ', value: 'brown_terracotta' }
                    ]
                },
                houseStyles: {
                    acceptReporters: true,
                    items: [
                        { text: 'モダン', value: 'modern' },
                        { text: '伝統的', value: 'traditional' },
                        { text: '中世風', value: 'medieval' },
                        { text: '和風', value: 'japanese' },
                        { text: 'ログハウス', value: 'log_house' }
                    ]
                },
                houseSizes: {
                    acceptReporters: true,
                    items: [
                        { text: '小', value: 'small' },
                        { text: '中', value: 'medium' },
                        { text: '大', value: 'large' }
                    ]
                },
                castleSizes: {
                    acceptReporters: true,
                    items: [
                        { text: '小さな城', value: 'small' },
                        { text: '中規模な城', value: 'medium' },
                        { text: '大きな城', value: 'large' }
                    ]
                }
            }
        };
    }

    // 共通のWebSocket接続を取得
    getWebSocket() {
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

    // 建築メソッド
    buildWall(args) {
        this.sendCommand('buildWall', {
            block: args.BLOCK,
            x1: args.X1, z1: args.Z1,
            x2: args.X2, z2: args.Z2,
            height: args.HEIGHT
        });
    }

    buildFloor(args) {
        this.sendCommand('buildFloor', {
            block: args.BLOCK,
            x1: args.X1, z1: args.Z1,
            x2: args.X2, z2: args.Z2,
            y: args.Y
        });
    }

    buildRoof(args) {
        this.sendCommand('buildRoof', {
            block: args.BLOCK,
            x: args.X, z: args.Z,
            width: args.WIDTH, depth: args.DEPTH,
            y: args.Y
        });
    }

    buildCircle(args) {
        this.sendCommand('buildCircle', {
            block: args.BLOCK,
            x: args.X, y: args.Y, z: args.Z,
            radius: args.RADIUS
        });
    }

    buildSphere(args) {
        this.sendCommand('buildSphere', {
            block: args.BLOCK,
            x: args.X, y: args.Y, z: args.Z,
            radius: args.RADIUS
        });
    }

    buildTower(args) {
        this.sendCommand('buildTower', {
            block: args.BLOCK,
            x: args.X, z: args.Z,
            width: args.WIDTH, height: args.HEIGHT
        });
    }

    buildHouse(args) {
        this.sendCommand('buildHouse', {
            style: args.STYLE,
            x: args.X, y: args.Y, z: args.Z,
            size: args.SIZE
        });
    }

    buildCastle(args) {
        this.sendCommand('buildCastle', {
            x: args.X, y: args.Y, z: args.Z,
            size: args.SIZE
        });
    }
}

module.exports = MinecraftBuildExtension;