/**
 * Minecraft Commands Extension - Minecraftコマンド専用
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

class MinecraftCommandsExtension {
    constructor(runtime) {
        this.runtime = runtime;
    }

    getInfo() {
        return {
            id: 'minecraftCommands',
            name: '⚡ Minecraft コマンド',
            color1: '#9932CC',
            color2: '#8A2BE2',
            blocks: [
                // プレイヤー操作
                {
                    opcode: 'teleportPlayer',
                    blockType: BlockType.COMMAND,
                    text: '🚀 [PLAYER]を X:[X] Y:[Y] Z:[Z] にテレポート',
                    arguments: {
                        PLAYER: {
                            type: ArgumentType.STRING,
                            defaultValue: '@s'
                        },
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                {
                    opcode: 'setGameMode',
                    blockType: BlockType.COMMAND,
                    text: '🎮 [PLAYER]のゲームモードを [MODE] に変更',
                    arguments: {
                        PLAYER: {
                            type: ArgumentType.STRING,
                            defaultValue: '@s'
                        },
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
                    text: '🎁 [PLAYER]に [ITEM] を [COUNT] 個与える',
                    arguments: {
                        PLAYER: {
                            type: ArgumentType.STRING,
                            defaultValue: '@s'
                        },
                        ITEM: {
                            type: ArgumentType.STRING,
                            menu: 'items',
                            defaultValue: 'diamond'
                        },
                        COUNT: { type: ArgumentType.NUMBER, defaultValue: 1 }
                    }
                },
                {
                    opcode: 'healPlayer',
                    blockType: BlockType.COMMAND,
                    text: '❤️ [PLAYER]を完全回復させる',
                    arguments: {
                        PLAYER: {
                            type: ArgumentType.STRING,
                            defaultValue: '@s'
                        }
                    }
                },
                {
                    opcode: 'feedPlayer',
                    blockType: BlockType.COMMAND,
                    text: '🍖 [PLAYER]の満腹度を回復させる',
                    arguments: {
                        PLAYER: {
                            type: ArgumentType.STRING,
                            defaultValue: '@s'
                        }
                    }
                },
                // 時間・天候制御
                {
                    opcode: 'setTime',
                    blockType: BlockType.COMMAND,
                    text: '🕐 時間を [TIME] に設定',
                    arguments: {
                        TIME: {
                            type: ArgumentType.STRING,
                            menu: 'timeOptions',
                            defaultValue: 'day'
                        }
                    }
                },
                {
                    opcode: 'setWeather',
                    blockType: BlockType.COMMAND,
                    text: '🌤️ 天気を [WEATHER] に設定',
                    arguments: {
                        WEATHER: {
                            type: ArgumentType.STRING,
                            menu: 'weatherOptions',
                            defaultValue: 'clear'
                        }
                    }
                },
                {
                    opcode: 'setDifficulty',
                    blockType: BlockType.COMMAND,
                    text: '💀 難易度を [DIFFICULTY] に設定',
                    arguments: {
                        DIFFICULTY: {
                            type: ArgumentType.STRING,
                            menu: 'difficulties',
                            defaultValue: 'easy'
                        }
                    }
                },
                // エフェクト
                {
                    opcode: 'giveEffect',
                    blockType: BlockType.COMMAND,
                    text: '✨ [PLAYER]に [EFFECT] を [DURATION] 秒間与える',
                    arguments: {
                        PLAYER: {
                            type: ArgumentType.STRING,
                            defaultValue: '@s'
                        },
                        EFFECT: {
                            type: ArgumentType.STRING,
                            menu: 'effects',
                            defaultValue: 'speed'
                        },
                        DURATION: { type: ArgumentType.NUMBER, defaultValue: 30 }
                    }
                },
                {
                    opcode: 'clearEffects',
                    blockType: BlockType.COMMAND,
                    text: '🧹 [PLAYER]のエフェクトをすべて除去',
                    arguments: {
                        PLAYER: {
                            type: ArgumentType.STRING,
                            defaultValue: '@s'
                        }
                    }
                },
                // モブ・エンティティ操作
                {
                    opcode: 'summonMob',
                    blockType: BlockType.COMMAND,
                    text: '🐄 [MOB]を X:[X] Y:[Y] Z:[Z] に召喚',
                    arguments: {
                        MOB: {
                            type: ArgumentType.STRING,
                            menu: 'mobs',
                            defaultValue: 'cow'
                        },
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                {
                    opcode: 'killEntities',
                    blockType: BlockType.COMMAND,
                    text: '💀 周囲の [TYPE] をすべて除去',
                    arguments: {
                        TYPE: {
                            type: ArgumentType.STRING,
                            menu: 'entityTypes',
                            defaultValue: 'hostile'
                        }
                    }
                },
                // ワールド操作
                {
                    opcode: 'setSpawn',
                    blockType: BlockType.COMMAND,
                    text: '🏁 スポーン地点を X:[X] Y:[Y] Z:[Z] に設定',
                    arguments: {
                        X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                        Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                        Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                    }
                },
                {
                    opcode: 'broadcastMessage',
                    blockType: BlockType.COMMAND,
                    text: '📢 全員に [MESSAGE] を表示',
                    arguments: {
                        MESSAGE: {
                            type: ArgumentType.STRING,
                            defaultValue: 'Hello everyone!'
                        }
                    }
                },
                // カスタムコマンド
                {
                    opcode: 'executeRawCommand',
                    blockType: BlockType.COMMAND,
                    text: '⚡ コマンド実行: [COMMAND]',
                    arguments: {
                        COMMAND: {
                            type: ArgumentType.STRING,
                            defaultValue: 'say Hello World!'
                        }
                    }
                }
            ],
            menus: {
                gameModes: {
                    acceptReporters: true,
                    items: [
                        { text: 'サバイバル (Survival)', value: 'survival' },
                        { text: 'クリエイティブ (Creative)', value: 'creative' },
                        { text: 'アドベンチャー (Adventure)', value: 'adventure' },
                        { text: 'スペクテイター (Spectator)', value: 'spectator' }
                    ]
                },
                items: {
                    acceptReporters: true,
                    items: [
                        { text: 'ダイヤモンド (Diamond)', value: 'diamond' },
                        { text: 'エメラルド (Emerald)', value: 'emerald' },
                        { text: '金 (Gold Ingot)', value: 'gold_ingot' },
                        { text: '鉄 (Iron Ingot)', value: 'iron_ingot' },
                        { text: 'ダイヤの剣 (Diamond Sword)', value: 'diamond_sword' },
                        { text: 'ダイヤのつるはし (Diamond Pickaxe)', value: 'diamond_pickaxe' },
                        { text: 'ダイヤの防具セット', value: 'diamond_armor_set' },
                        { text: 'エリトラ (Elytra)', value: 'elytra' },
                        { text: 'エンダーパール (Ender Pearl)', value: 'ender_pearl' },
                        { text: '経験値ボトル (Experience Bottle)', value: 'experience_bottle' }
                    ]
                },
                timeOptions: {
                    acceptReporters: true,
                    items: [
                        { text: '昼 (Day)', value: 'day' },
                        { text: '夜 (Night)', value: 'night' },
                        { text: '正午 (Noon)', value: 'noon' },
                        { text: '真夜中 (Midnight)', value: 'midnight' },
                        { text: '日の出 (Sunrise)', value: 'sunrise' },
                        { text: '日の入り (Sunset)', value: 'sunset' }
                    ]
                },
                weatherOptions: {
                    acceptReporters: true,
                    items: [
                        { text: '晴れ (Clear)', value: 'clear' },
                        { text: '雨 (Rain)', value: 'rain' },
                        { text: '雷雨 (Thunder)', value: 'thunder' }
                    ]
                },
                difficulties: {
                    acceptReporters: true,
                    items: [
                        { text: 'ピースフル (Peaceful)', value: 'peaceful' },
                        { text: 'イージー (Easy)', value: 'easy' },
                        { text: 'ノーマル (Normal)', value: 'normal' },
                        { text: 'ハード (Hard)', value: 'hard' }
                    ]
                },
                effects: {
                    acceptReporters: true,
                    items: [
                        { text: '俊敏 (Speed)', value: 'speed' },
                        { text: '跳躍 (Jump Boost)', value: 'jump_boost' },
                        { text: '力 (Strength)', value: 'strength' },
                        { text: '再生 (Regeneration)', value: 'regeneration' },
                        { text: '耐性 (Resistance)', value: 'resistance' },
                        { text: '暗視 (Night Vision)', value: 'night_vision' },
                        { text: '水中呼吸 (Water Breathing)', value: 'water_breathing' },
                        { text: '透明化 (Invisibility)', value: 'invisibility' },
                        { text: '飛行 (Levitation)', value: 'levitation' }
                    ]
                },
                mobs: {
                    acceptReporters: true,
                    items: [
                        { text: '牛 (Cow)', value: 'cow' },
                        { text: '豚 (Pig)', value: 'pig' },
                        { text: '羊 (Sheep)', value: 'sheep' },
                        { text: '鶏 (Chicken)', value: 'chicken' },
                        { text: '馬 (Horse)', value: 'horse' },
                        { text: '犬 (Wolf)', value: 'wolf' },
                        { text: '猫 (Cat)', value: 'cat' },
                        { text: '村人 (Villager)', value: 'villager' },
                        { text: 'アイアンゴーレム (Iron Golem)', value: 'iron_golem' },
                        { text: 'ゾンビ (Zombie)', value: 'zombie' },
                        { text: 'スケルトン (Skeleton)', value: 'skeleton' },
                        { text: 'クリーパー (Creeper)', value: 'creeper' }
                    ]
                },
                entityTypes: {
                    acceptReporters: true,
                    items: [
                        { text: '敵対的なモブ (Hostile)', value: 'hostile' },
                        { text: '友好的なモブ (Peaceful)', value: 'peaceful' },
                        { text: 'すべてのモブ (All Mobs)', value: 'all_mobs' },
                        { text: 'アイテム (Items)', value: 'items' },
                        { text: '経験値オーブ (Experience Orbs)', value: 'experience_orbs' }
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

    // コマンドメソッド
    teleportPlayer(args) {
        this.sendCommand('tp', {
            player: args.PLAYER,
            x: args.X, y: args.Y, z: args.Z
        });
    }

    setGameMode(args) {
        this.sendCommand('gamemode', {
            player: args.PLAYER,
            mode: args.MODE
        });
    }

    giveItem(args) {
        this.sendCommand('give', {
            player: args.PLAYER,
            item: args.ITEM,
            count: args.COUNT
        });
    }

    healPlayer(args) {
        this.sendCommand('heal', {
            player: args.PLAYER
        });
    }

    feedPlayer(args) {
        this.sendCommand('feed', {
            player: args.PLAYER
        });
    }

    setTime(args) {
        this.sendCommand('time', {
            time: args.TIME
        });
    }

    setWeather(args) {
        this.sendCommand('weather', {
            weather: args.WEATHER
        });
    }

    setDifficulty(args) {
        this.sendCommand('difficulty', {
            difficulty: args.DIFFICULTY
        });
    }

    giveEffect(args) {
        this.sendCommand('effect', {
            player: args.PLAYER,
            effect: args.EFFECT,
            duration: args.DURATION
        });
    }

    clearEffects(args) {
        this.sendCommand('effect_clear', {
            player: args.PLAYER
        });
    }

    summonMob(args) {
        this.sendCommand('summon', {
            mob: args.MOB,
            x: args.X, y: args.Y, z: args.Z
        });
    }

    killEntities(args) {
        this.sendCommand('kill_entities', {
            type: args.TYPE
        });
    }

    setSpawn(args) {
        this.sendCommand('setworldspawn', {
            x: args.X, y: args.Y, z: args.Z
        });
    }

    broadcastMessage(args) {
        this.sendCommand('say', {
            message: args.MESSAGE
        });
    }

    executeRawCommand(args) {
        this.sendCommand('raw_command', {
            command: args.COMMAND
        });
    }
}

module.exports = MinecraftCommandsExtension;