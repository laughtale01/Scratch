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
                    opcode: 'setBlock',
                    blockType: 'command',
                    text: 'ブロックを置く x:[X] y:[Y] z:[Z] ブロック:[BLOCK] 配置:[PLACEMENT] 向き:[FACING]',
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
                        },
                        PLACEMENT: {
                            type: 'string',
                            menu: 'blockPlacement',
                            defaultValue: 'bottom'
                        },
                        FACING: {
                            type: 'string',
                            menu: 'blockFacing',
                            defaultValue: 'none'
                        }
                    }
                },
                {
                    opcode: 'setBlockRelative',
                    blockType: 'command',
                    text: 'ブロックを置く ~[X] ~[Y] ~[Z] ブロック:[BLOCK] 配置:[PLACEMENT] 向き:[FACING]',
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
                        },
                        PLACEMENT: {
                            type: 'string',
                            menu: 'blockPlacement',
                            defaultValue: 'bottom'
                        },
                        FACING: {
                            type: 'string',
                            menu: 'blockFacing',
                            defaultValue: 'none'
                        }
                    }
                },
                {
                    opcode: 'setBlockRange',
                    blockType: 'command',
                    text: 'X:[X1] Y:[Y1] Z:[Z1] からX:[X2] Y:[Y2] Z:[Z2] ブロック:[BLOCK] 配置:[PLACEMENT] 向き:[FACING]',
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
                        },
                        PLACEMENT: {
                            type: 'string',
                            menu: 'blockPlacement',
                            defaultValue: 'bottom'
                        },
                        FACING: {
                            type: 'string',
                            menu: 'blockFacing',
                            defaultValue: 'none'
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
                    opcode: 'clearArea',
                    blockType: 'command',
                    text: '周囲をクリア X:[X] Z:[Z]',
                    arguments: {
                        X: {
                            type: 'number',
                            defaultValue: 0
                        },
                        Z: {
                            type: 'number',
                            defaultValue: 0
                        }
                    }
                },
                {
                    opcode: 'clearAllEntities',
                    blockType: 'command',
                    text: '全エンティティをクリア X:[X] Z:[Z]',
                    arguments: {
                        X: {
                            type: 'number',
                            defaultValue: 0
                        },
                        Z: {
                            type: 'number',
                            defaultValue: 0
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
                        // 基本ブロック
                        'stone', 'dirt', 'grass_block', 'cobblestone', 'bedrock',
                        'sand', 'red_sand', 'gravel', 'clay', 'terracotta',

                        // 木材系（全種類）
                        'oak_planks', 'spruce_planks', 'birch_planks', 'jungle_planks',
                        'acacia_planks', 'dark_oak_planks', 'mangrove_planks', 'cherry_planks',
                        'bamboo_planks', 'bamboo_mosaic', 'crimson_planks', 'warped_planks',

                        // 原木
                        'oak_log', 'spruce_log', 'birch_log', 'jungle_log',
                        'acacia_log', 'dark_oak_log', 'mangrove_log', 'cherry_log',

                        // ハーフブロック（全種類）
                        'oak_slab', 'spruce_slab', 'birch_slab', 'jungle_slab',
                        'acacia_slab', 'dark_oak_slab', 'mangrove_slab', 'cherry_slab',
                        'bamboo_slab', 'bamboo_mosaic_slab', 'crimson_slab', 'warped_slab',
                        'stone_slab', 'cobblestone_slab', 'stone_brick_slab', 'brick_slab',
                        'nether_brick_slab', 'quartz_slab', 'sandstone_slab', 'red_sandstone_slab',
                        'prismarine_slab', 'prismarine_brick_slab', 'dark_prismarine_slab',
                        'smooth_stone_slab', 'smooth_sandstone_slab', 'smooth_red_sandstone_slab', 'smooth_quartz_slab',
                        'granite_slab', 'polished_granite_slab', 'diorite_slab', 'polished_diorite_slab',
                        'andesite_slab', 'polished_andesite_slab',

                        // 階段（全種類）
                        'oak_stairs', 'spruce_stairs', 'birch_stairs', 'jungle_stairs',
                        'acacia_stairs', 'dark_oak_stairs', 'mangrove_stairs', 'cherry_stairs',
                        'bamboo_stairs', 'bamboo_mosaic_stairs', 'crimson_stairs', 'warped_stairs',
                        'stone_stairs', 'cobblestone_stairs', 'stone_brick_stairs', 'brick_stairs',
                        'nether_brick_stairs', 'red_nether_brick_stairs', 'quartz_stairs',
                        'sandstone_stairs', 'red_sandstone_stairs', 'smooth_sandstone_stairs', 'smooth_red_sandstone_stairs',
                        'prismarine_stairs', 'prismarine_brick_stairs', 'dark_prismarine_stairs',
                        'granite_stairs', 'polished_granite_stairs', 'diorite_stairs', 'polished_diorite_stairs',
                        'andesite_stairs', 'polished_andesite_stairs', 'end_stone_brick_stairs',
                        'purpur_stairs', 'blackstone_stairs', 'polished_blackstone_stairs',

                        // 石材系
                        'smooth_stone', 'stone_bricks', 'cracked_stone_bricks', 'mossy_stone_bricks', 'chiseled_stone_bricks',
                        'granite', 'polished_granite', 'diorite', 'polished_diorite', 'andesite', 'polished_andesite',
                        'calcite', 'tuff', 'dripstone_block', 'moss_block',

                        // ディープスレート系
                        'deepslate', 'cobbled_deepslate', 'polished_deepslate', 'deepslate_bricks',
                        'deepslate_tiles', 'chiseled_deepslate', 'cracked_deepslate_bricks', 'cracked_deepslate_tiles',

                        // レンガ系
                        'bricks', 'nether_bricks', 'red_nether_bricks', 'cracked_nether_bricks', 'chiseled_nether_bricks',
                        'end_stone_bricks', 'prismarine', 'prismarine_bricks', 'dark_prismarine',
                        'mud_bricks', 'packed_mud',

                        // クォーツ系
                        'quartz_block', 'smooth_quartz', 'quartz_bricks', 'quartz_pillar', 'chiseled_quartz_block',

                        // 砂岩系
                        'sandstone', 'smooth_sandstone', 'chiseled_sandstone', 'cut_sandstone',
                        'red_sandstone', 'smooth_red_sandstone', 'chiseled_red_sandstone', 'cut_red_sandstone',

                        // 羊毛（全16色）
                        'white_wool', 'orange_wool', 'magenta_wool', 'light_blue_wool',
                        'yellow_wool', 'lime_wool', 'pink_wool', 'gray_wool',
                        'light_gray_wool', 'cyan_wool', 'purple_wool', 'blue_wool',
                        'brown_wool', 'green_wool', 'red_wool', 'black_wool',

                        // カーペット（全16色）
                        'white_carpet', 'orange_carpet', 'magenta_carpet', 'light_blue_carpet',
                        'yellow_carpet', 'lime_carpet', 'pink_carpet', 'gray_carpet',
                        'light_gray_carpet', 'cyan_carpet', 'purple_carpet', 'blue_carpet',
                        'brown_carpet', 'green_carpet', 'red_carpet', 'black_carpet',

                        // コンクリート（全16色）
                        'white_concrete', 'orange_concrete', 'magenta_concrete', 'light_blue_concrete',
                        'yellow_concrete', 'lime_concrete', 'pink_concrete', 'gray_concrete',
                        'light_gray_concrete', 'cyan_concrete', 'purple_concrete', 'blue_concrete',
                        'brown_concrete', 'green_concrete', 'red_concrete', 'black_concrete',

                        // コンクリートパウダー
                        'white_concrete_powder', 'orange_concrete_powder', 'magenta_concrete_powder', 'light_blue_concrete_powder',
                        'yellow_concrete_powder', 'lime_concrete_powder', 'pink_concrete_powder', 'gray_concrete_powder',

                        // テラコッタ（全16色）
                        'white_terracotta', 'orange_terracotta', 'magenta_terracotta', 'light_blue_terracotta',
                        'yellow_terracotta', 'lime_terracotta', 'pink_terracotta', 'gray_terracotta',
                        'light_gray_terracotta', 'cyan_terracotta', 'purple_terracotta', 'blue_terracotta',
                        'brown_terracotta', 'green_terracotta', 'red_terracotta', 'black_terracotta',

                        // 彩釉テラコッタ（主要色）
                        'white_glazed_terracotta', 'orange_glazed_terracotta', 'magenta_glazed_terracotta',
                        'light_blue_glazed_terracotta', 'yellow_glazed_terracotta', 'lime_glazed_terracotta',
                        'pink_glazed_terracotta', 'cyan_glazed_terracotta', 'purple_glazed_terracotta',
                        'blue_glazed_terracotta', 'red_glazed_terracotta', 'black_glazed_terracotta',

                        // ガラス系
                        'glass', 'tinted_glass',
                        'white_stained_glass', 'orange_stained_glass', 'magenta_stained_glass', 'light_blue_stained_glass',
                        'yellow_stained_glass', 'lime_stained_glass', 'pink_stained_glass', 'gray_stained_glass',
                        'cyan_stained_glass', 'purple_stained_glass', 'blue_stained_glass',
                        'red_stained_glass', 'green_stained_glass', 'black_stained_glass',

                        // 鉱石ブロック
                        'gold_block', 'iron_block', 'copper_block', 'diamond_block', 'emerald_block',
                        'coal_block', 'redstone_block', 'lapis_block', 'quartz_block',
                        'netherite_block', 'amethyst_block', 'raw_iron_block', 'raw_copper_block', 'raw_gold_block',

                        // 自然ブロック
                        'grass_block', 'podzol', 'mycelium', 'dirt_path', 'farmland',
                        'coarse_dirt', 'rooted_dirt', 'mud', 'ice', 'packed_ice', 'blue_ice',
                        'snow_block', 'snow', 'powder_snow',

                        // 植物系
                        'oak_leaves', 'spruce_leaves', 'birch_leaves', 'jungle_leaves',
                        'acacia_leaves', 'dark_oak_leaves', 'mangrove_leaves', 'cherry_leaves',
                        'azalea_leaves', 'flowering_azalea_leaves',

                        // その他重要ブロック
                        'obsidian', 'crying_obsidian', 'netherrack', 'soul_sand', 'soul_soil',
                        'basalt', 'smooth_basalt', 'polished_basalt', 'blackstone', 'polished_blackstone',
                        'gilded_blackstone', 'polished_blackstone_bricks', 'cracked_polished_blackstone_bricks',
                        'end_stone', 'purpur_block', 'purpur_pillar',
                        'bone_block', 'coal_ore', 'iron_ore', 'gold_ore', 'diamond_ore',
                        'emerald_ore', 'lapis_ore', 'redstone_ore', 'copper_ore',
                        'glowstone', 'sea_lantern', 'shroomlight',
                        'sponge', 'wet_sponge', 'slime_block', 'honey_block',
                        'tnt', 'bookshelf', 'crafting_table', 'furnace', 'chest',
                        'water', 'lava', 'magma_block'
                    ]
                },
                blockPlacement: {
                    acceptReporters: false,
                    items: [
                        { text: '通常（下）', value: 'bottom' },
                        { text: '上下反転（上）', value: 'top' },
                        { text: 'ダブル', value: 'double' }
                    ]
                },
                blockFacing: {
                    acceptReporters: false,
                    items: [
                        { text: 'デフォルト', value: 'none' },
                        { text: '北', value: 'north' },
                        { text: '南', value: 'south' },
                        { text: '東', value: 'east' },
                        { text: '西', value: 'west' }
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
        let blockType = 'minecraft:' + args.BLOCK;

        // プロパティの配列を作成
        const properties = [];

        // スラブかどうかを判定
        const isSlab = args.BLOCK.includes('_slab');

        // 配置パラメータを適用
        if (args.PLACEMENT && args.PLACEMENT !== 'bottom') {
            if (isSlab) {
                // スラブの場合: type プロパティを使用
                properties.push('type=' + args.PLACEMENT);
            } else {
                // 階段などの場合: half プロパティを使用
                properties.push('half=' + args.PLACEMENT);
            }
        }

        // 向きパラメータを適用（階段ブロックなどの方向）
        if (args.FACING && args.FACING !== 'none') {
            properties.push('facing=' + args.FACING);
        }

        // プロパティを結合
        if (properties.length > 0) {
            blockType += '[' + properties.join(',') + ']';
        }

        return this.sendCommand('setBlock', {
            x: Number(args.X),
            y: Number(args.Y) - 60,  // Y座標変換: -60でスーパーフラット地表(Y=-60)に対応
            z: Number(args.Z),
            blockType: blockType
        });
    }

    /**
     * ブロック配置（相対座標）
     */
    setBlockRelative(args) {
        let blockType = 'minecraft:' + args.BLOCK;

        // プロパティの配列を作成
        const properties = [];

        // スラブかどうかを判定
        const isSlab = args.BLOCK.includes('_slab');

        // 配置パラメータを適用
        if (args.PLACEMENT && args.PLACEMENT !== 'bottom') {
            if (isSlab) {
                // スラブの場合: type プロパティを使用
                properties.push('type=' + args.PLACEMENT);
            } else {
                // 階段などの場合: half プロパティを使用
                properties.push('half=' + args.PLACEMENT);
            }
        }

        // 向きパラメータを適用（階段ブロックなどの方向）
        if (args.FACING && args.FACING !== 'none') {
            properties.push('facing=' + args.FACING);
        }

        // プロパティを結合
        if (properties.length > 0) {
            blockType += '[' + properties.join(',') + ']';
        }

        return this.sendCommand('setBlock', {
            relativeX: Number(args.X),
            relativeY: Number(args.Y),
            relativeZ: Number(args.Z),
            blockType: blockType
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

        let blockType = 'minecraft:' + args.BLOCK;

        // プロパティの配列を作成
        const properties = [];

        // スラブかどうかを判定
        const isSlab = args.BLOCK.includes('_slab');

        // 配置パラメータを適用
        if (args.PLACEMENT && args.PLACEMENT !== 'bottom') {
            if (isSlab) {
                // スラブの場合: type プロパティを使用
                properties.push('type=' + args.PLACEMENT);
            } else {
                // 階段などの場合: half プロパティを使用
                properties.push('half=' + args.PLACEMENT);
            }
        }

        // 向きパラメータを適用（階段ブロックなどの方向）
        if (args.FACING && args.FACING !== 'none') {
            properties.push('facing=' + args.FACING);
        }

        // プロパティを結合
        if (properties.length > 0) {
            blockType += '[' + properties.join(',') + ']';
        }

        // 座標を正規化（小さい方から大きい方へ）
        const minX = Math.min(x1, x2);
        const maxX = Math.max(x1, x2);
        const minY = Math.min(y1, y2);
        const maxY = Math.max(y1, y2);
        const minZ = Math.min(z1, z2);
        const maxZ = Math.max(z1, z2);

        // 範囲が大きすぎる場合は制限（最大2,000,000ブロック）
        const rangeX = maxX - minX + 1;
        const rangeY = maxY - minY + 1;
        const rangeZ = maxZ - minZ + 1;
        const volume = rangeX * rangeY * rangeZ;

        if (volume > 2000000) {
            console.warn('範囲が大きすぎます。最大2,000,000ブロックまでです。');
            return Promise.reject(new Error('範囲が大きすぎます（最大2,000,000ブロック）'));
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
     * 周囲クリア
     * 指定された中心座標(X, Z)から±50ブロックの範囲をスーパーフラットの初期状態に戻す
     * Y=-64: 岩盤、Y=-63～-62: 土（2層）、Y=-61: 草ブロック、Y=-60～100: 空気
     */
    clearArea(args) {
        const centerX = Number(args.X) || 0;
        const centerZ = Number(args.Z) || 0;
        console.log(`周囲クリア開始: 中心(${centerX}, ${centerZ}) から±50ブロック範囲`);
        return this.sendCommand('clearArea', {
            centerX: centerX,
            centerZ: centerZ
        });
    }

    /**
     * 全エンティティをクリア
     * 指定された中心座標(X, Z)から±50ブロック、Y:-64～100の範囲のエンティティを削除（プレイヤーを除く）
     */
    clearAllEntities(args) {
        const centerX = Number(args.X) || 0;
        const centerZ = Number(args.Z) || 0;
        console.log(`全エンティティクリア開始: 中心(${centerX}, ${centerZ}) から±50ブロック範囲`);
        return this.sendCommand('clearAllEntities', {
            centerX: centerX,
            centerZ: centerZ
        });
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
