/**
 * Minecraft Info Extension - プレイヤー情報・ナビゲーション専用
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

class MinecraftInfoExtension {
    constructor(runtime) {
        this.runtime = runtime;
        this.overlayVisible = false;
        this.playerData = {
            x: 0, y: 0, z: 0,
            direction: 'North',
            biome: 'Plains',
            dimension: 'Overworld',
            health: 20,
            hunger: 20,
            level: 0
        };
    }

    getInfo() {
        return {
            id: 'minecraftInfo',
            name: '📍 Minecraft 情報',
            color1: '#00CED1',
            color2: '#00BFFF',
            blocks: [
                // プレイヤー位置情報
                {
                    opcode: 'getPlayerX',
                    blockType: BlockType.REPORTER,
                    text: '📍 プレイヤーのX座標'
                },
                {
                    opcode: 'getPlayerY',
                    blockType: BlockType.REPORTER,
                    text: '📍 プレイヤーのY座標'
                },
                {
                    opcode: 'getPlayerZ',
                    blockType: BlockType.REPORTER,
                    text: '📍 プレイヤーのZ座標'
                },
                {
                    opcode: 'getPlayerDirection',
                    blockType: BlockType.REPORTER,
                    text: '🧭 プレイヤーの向いている方角'
                },
                {
                    opcode: 'getPlayerBiome',
                    blockType: BlockType.REPORTER,
                    text: '🌿 現在のバイオーム'
                },
                {
                    opcode: 'getPlayerDimension',
                    blockType: BlockType.REPORTER,
                    text: '🌍 現在のディメンション'
                },
                // プレイヤーステータス
                {
                    opcode: 'getPlayerHealth',
                    blockType: BlockType.REPORTER,
                    text: '❤️ プレイヤーの体力'
                },
                {
                    opcode: 'getPlayerHunger',
                    blockType: BlockType.REPORTER,
                    text: '🍖 プレイヤーの満腹度'
                },
                {
                    opcode: 'getPlayerLevel',
                    blockType: BlockType.REPORTER,
                    text: '⭐ プレイヤーのレベル'
                },
                {
                    opcode: 'getPlayerGameMode',
                    blockType: BlockType.REPORTER,
                    text: '🎮 プレイヤーのゲームモード'
                },
                // ワールド情報
                {
                    opcode: 'getWorldTime',
                    blockType: BlockType.REPORTER,
                    text: '🕐 ワールドの時間'
                },
                {
                    opcode: 'getWorldWeather',
                    blockType: BlockType.REPORTER,
                    text: '🌤️ ワールドの天気'
                },
                {
                    opcode: 'getWorldDifficulty',
                    blockType: BlockType.REPORTER,
                    text: '💀 ワールドの難易度'
                },
                // 距離・計算
                {
                    opcode: 'getDistanceTo',
                    blockType: BlockType.REPORTER,
                    text: '📏 X:[X] Y:[Y] Z:[Z] までの距離',
                    arguments: {
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                {
                    opcode: 'getDirectionTo',
                    blockType: BlockType.REPORTER,
                    text: '🧭 X:[X] Z:[Z] への方角',
                    arguments: {
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                // ブロック・環境情報
                {
                    opcode: 'getBlockAt',
                    blockType: BlockType.REPORTER,
                    text: '🔍 X:[X] Y:[Y] Z:[Z] のブロック名',
                    arguments: {
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                {
                    opcode: 'getLightLevel',
                    blockType: BlockType.REPORTER,
                    text: '💡 X:[X] Y:[Y] Z:[Z] の明るさレベル',
                    arguments: {
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                // 条件判定
                {
                    opcode: 'isPlayerInBiome',
                    blockType: BlockType.BOOLEAN,
                    text: '🌿 プレイヤーが [BIOME] にいる？',
                    arguments: {
                        BIOME: {
                            type: ArgumentType.STRING,
                            menu: 'biomes',
                            defaultValue: 'plains'
                        }
                    }
                },
                {
                    opcode: 'isPlayerInDimension',
                    blockType: BlockType.BOOLEAN,
                    text: '🌍 プレイヤーが [DIMENSION] にいる？',
                    arguments: {
                        DIMENSION: {
                            type: ArgumentType.STRING,
                            menu: 'dimensions',
                            defaultValue: 'overworld'
                        }
                    }
                },
                {
                    opcode: 'isNearPosition',
                    blockType: BlockType.BOOLEAN,
                    text: '📍 X:[X] Y:[Y] Z:[Z] から [DISTANCE] ブロック以内にいる？',
                    arguments: {
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        DISTANCE: { type: ArgumentType.NUMBER, defaultValue: 10 }
                    }
                },
                // オーバーレイ制御
                {
                    opcode: 'showOverlay',
                    blockType: BlockType.COMMAND,
                    text: '👁️ 位置情報オーバーレイを表示'
                },
                {
                    opcode: 'hideOverlay',
                    blockType: BlockType.COMMAND,
                    text: '🙈 位置情報オーバーレイを非表示'
                },
                {
                    opcode: 'toggleOverlay',
                    blockType: BlockType.COMMAND,
                    text: '🔄 位置情報オーバーレイを切り替え'
                },
                {
                    opcode: 'isOverlayVisible',
                    blockType: BlockType.BOOLEAN,
                    text: '👁️ オーバーレイが表示されている？'
                }
            ],
            menus: {
                biomes: {
                    acceptReporters: true,
                    items: [
                        { text: '平原 (Plains)', value: 'plains' },
                        { text: '森林 (Forest)', value: 'forest' },
                        { text: '砂漠 (Desert)', value: 'desert' },
                        { text: 'タイガ (Taiga)', value: 'taiga' },
                        { text: '山岳 (Mountains)', value: 'mountains' },
                        { text: '海洋 (Ocean)', value: 'ocean' },
                        { text: '沼地 (Swamp)', value: 'swamp' },
                        { text: 'ジャングル (Jungle)', value: 'jungle' },
                        { text: 'ツンドラ (Tundra)', value: 'tundra' },
                        { text: 'サバンナ (Savanna)', value: 'savanna' }
                    ]
                },
                dimensions: {
                    acceptReporters: true,
                    items: [
                        { text: 'オーバーワールド (Overworld)', value: 'overworld' },
                        { text: 'ネザー (Nether)', value: 'nether' },
                        { text: 'エンド (End)', value: 'end' }
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

    // 位置情報メソッド
    getPlayerX() {
        this.sendCommand('getPlayerPos');
        return this.playerData.x;
    }

    getPlayerY() {
        this.sendCommand('getPlayerPos');
        return this.playerData.y;
    }

    getPlayerZ() {
        this.sendCommand('getPlayerPos');
        return this.playerData.z;
    }

    getPlayerDirection() {
        this.sendCommand('getPlayerDirection');
        return this.playerData.direction;
    }

    getPlayerBiome() {
        this.sendCommand('getPlayerBiome');
        return this.playerData.biome;
    }

    getPlayerDimension() {
        this.sendCommand('getPlayerDimension');
        return this.playerData.dimension;
    }

    // ステータス情報メソッド
    getPlayerHealth() {
        this.sendCommand('getPlayerHealth');
        return this.playerData.health;
    }

    getPlayerHunger() {
        this.sendCommand('getPlayerHunger');
        return this.playerData.hunger;
    }

    getPlayerLevel() {
        this.sendCommand('getPlayerLevel');
        return this.playerData.level;
    }

    getPlayerGameMode() {
        this.sendCommand('getPlayerGameMode');
        return this.playerData.gameMode || 'creative';
    }

    // ワールド情報メソッド
    getWorldTime() {
        this.sendCommand('getWorldTime');
        return '昼'; // プレースホルダー
    }

    getWorldWeather() {
        this.sendCommand('getWorldWeather');
        return '晴れ'; // プレースホルダー
    }

    getWorldDifficulty() {
        this.sendCommand('getWorldDifficulty');
        return 'Easy'; // プレースホルダー
    }

    // 距離・計算メソッド
    getDistanceTo(args) {
        const dx = this.playerData.x - args.X;
        const dy = this.playerData.y - args.Y;
        const dz = this.playerData.z - args.Z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz).toFixed(1);
    }

    getDirectionTo(args) {
        const dx = args.X - this.playerData.x;
        const dz = args.Z - this.playerData.z;
        const angle = Math.atan2(dz, dx) * 180 / Math.PI;
        
        if (angle >= -22.5 && angle < 22.5) return '東';
        if (angle >= 22.5 && angle < 67.5) return '南東';
        if (angle >= 67.5 && angle < 112.5) return '南';
        if (angle >= 112.5 && angle < 157.5) return '南西';
        if (angle >= 157.5 || angle < -157.5) return '西';
        if (angle >= -157.5 && angle < -112.5) return '北西';
        if (angle >= -112.5 && angle < -67.5) return '北';
        if (angle >= -67.5 && angle < -22.5) return '北東';
        return '不明';
    }

    // ブロック・環境情報メソッド
    getBlockAt(args) {
        this.sendCommand('getBlock', {
            x: args.X, y: args.Y, z: args.Z
        });
        return 'Stone'; // プレースホルダー
    }

    getLightLevel(args) {
        this.sendCommand('getLightLevel', {
            x: args.X, y: args.Y, z: args.Z
        });
        return 15; // プレースホルダー
    }

    // 条件判定メソッド
    isPlayerInBiome(args) {
        return this.playerData.biome.toLowerCase() === args.BIOME.toLowerCase();
    }

    isPlayerInDimension(args) {
        return this.playerData.dimension.toLowerCase() === args.DIMENSION.toLowerCase();
    }

    isNearPosition(args) {
        const distance = this.getDistanceTo(args);
        return parseFloat(distance) <= args.DISTANCE;
    }

    // オーバーレイ制御メソッド
    showOverlay() {
        this.overlayVisible = true;
        this.createOverlay();
        this.sendCommand('showOverlay');
    }

    hideOverlay() {
        this.overlayVisible = false;
        this.removeOverlay();
        this.sendCommand('hideOverlay');
    }

    toggleOverlay() {
        if (this.overlayVisible) {
            this.hideOverlay();
        } else {
            this.showOverlay();
        }
    }

    isOverlayVisible() {
        return this.overlayVisible;
    }

    // オーバーレイUI作成
    createOverlay() {
        // 既存のオーバーレイを削除
        this.removeOverlay();

        const overlay = document.createElement('div');
        overlay.id = 'minecraft-info-overlay';
        overlay.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            width: 250px;
            background: rgba(0, 0, 0, 0.8);
            color: white;
            padding: 15px;
            border-radius: 10px;
            font-family: monospace;
            font-size: 12px;
            z-index: 10000;
            border: 2px solid #00CED1;
            box-shadow: 0 0 20px rgba(0, 206, 209, 0.5);
        `;

        const updateOverlay = () => {
            if (!this.overlayVisible) return;
            
            overlay.innerHTML = `
                <div style="text-align: center; font-weight: bold; margin-bottom: 10px; color: #00CED1;">
                    📍 プレイヤー情報
                </div>
                <div>📍 座標: ${this.playerData.x}, ${this.playerData.y}, ${this.playerData.z}</div>
                <div>🧭 方角: ${this.playerData.direction}</div>
                <div>🌿 バイオーム: ${this.playerData.biome}</div>
                <div>🌍 ディメンション: ${this.playerData.dimension}</div>
                <div>❤️ 体力: ${this.playerData.health}/20</div>
                <div>🍖 満腹度: ${this.playerData.hunger}/20</div>
                <div>⭐ レベル: ${this.playerData.level}</div>
                <div style="margin-top: 10px; text-align: center; font-size: 10px; color: #AAA;">
                    リアルタイム更新中...
                </div>
            `;
        };

        document.body.appendChild(overlay);
        updateOverlay();

        // 定期更新
        this.overlayUpdateInterval = setInterval(updateOverlay, 1000);
    }

    removeOverlay() {
        const overlay = document.getElementById('minecraft-info-overlay');
        if (overlay) {
            overlay.remove();
        }
        if (this.overlayUpdateInterval) {
            clearInterval(this.overlayUpdateInterval);
            this.overlayUpdateInterval = null;
        }
    }

    // データ更新用メソッド（他の拡張機能から呼び出される）
    updatePlayerData(data) {
        Object.assign(this.playerData, data);
    }
}

module.exports = MinecraftInfoExtension;