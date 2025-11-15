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

        // 座標変換定数
        // スーパーフラットワールドの地表がY=-60のため、
        // Scratch Y座標から60を引いてMinecraft座標に変換
        this.Y_OFFSET = -60;

        // 座標範囲の制限（Minecraft 1.20.1の仕様）
        this.MAX_XZ_COORD = 30000000;   // X/Z座標の最大値
        this.MIN_XZ_COORD = -30000000;  // X/Z座標の最小値
        this.MAX_Y_COORD = 319;         // Y座標の最大値（Minecraft座標系）
        this.MIN_Y_COORD = -64;         // Y座標の最小値（Minecraft座標系）
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

                        // 原木（通常版）
                        'oak_log', 'spruce_log', 'birch_log', 'jungle_log',
                        'acacia_log', 'dark_oak_log', 'mangrove_log', 'cherry_log',
                        // 原木（剥皮版）
                        'stripped_oak_log', 'stripped_spruce_log', 'stripped_birch_log', 'stripped_jungle_log',
                        'stripped_acacia_log', 'stripped_dark_oak_log', 'stripped_mangrove_log', 'stripped_cherry_log',
                        // 木ブロック（全面樹皮版）
                        'oak_wood', 'spruce_wood', 'birch_wood', 'jungle_wood',
                        'acacia_wood', 'dark_oak_wood', 'mangrove_wood', 'cherry_wood',
                        // 木ブロック（剥皮版）
                        'stripped_oak_wood', 'stripped_spruce_wood', 'stripped_birch_wood', 'stripped_jungle_wood',
                        'stripped_acacia_wood', 'stripped_dark_oak_wood', 'stripped_mangrove_wood', 'stripped_cherry_wood',
                        // ネザー原木
                        'crimson_stem', 'warped_stem', 'stripped_crimson_stem', 'stripped_warped_stem',
                        'crimson_hyphae', 'warped_hyphae', 'stripped_crimson_hyphae', 'stripped_warped_hyphae',
                        // 竹ブロック
                        'bamboo_block', 'stripped_bamboo_block',

                        // ハーフブロック（全種類）
                        'oak_slab', 'spruce_slab', 'birch_slab', 'jungle_slab',
                        'acacia_slab', 'dark_oak_slab', 'mangrove_slab', 'cherry_slab',
                        'bamboo_slab', 'bamboo_mosaic_slab', 'crimson_slab', 'warped_slab',
                        'stone_slab', 'cobblestone_slab', 'stone_brick_slab', 'brick_slab',
                        'nether_brick_slab', 'quartz_slab', 'sandstone_slab', 'red_sandstone_slab',
                        'cut_sandstone_slab', 'cut_red_sandstone_slab',
                        'prismarine_slab', 'prismarine_brick_slab', 'dark_prismarine_slab',
                        'smooth_stone_slab', 'smooth_sandstone_slab', 'smooth_red_sandstone_slab', 'smooth_quartz_slab',
                        'granite_slab', 'polished_granite_slab', 'diorite_slab', 'polished_diorite_slab',
                        'andesite_slab', 'polished_andesite_slab',
                        'cobbled_deepslate_slab', 'polished_deepslate_slab', 'deepslate_brick_slab', 'deepslate_tile_slab',
                        'blackstone_slab', 'polished_blackstone_slab', 'polished_blackstone_brick_slab',
                        'end_stone_brick_slab', 'purpur_slab',

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
                        'purpur_stairs', 'blackstone_stairs', 'polished_blackstone_stairs', 'polished_blackstone_brick_stairs',
                        'cobbled_deepslate_stairs', 'polished_deepslate_stairs', 'deepslate_brick_stairs', 'deepslate_tile_stairs',

                        // 石材系
                        'smooth_stone', 'stone_bricks', 'cracked_stone_bricks', 'mossy_stone_bricks', 'chiseled_stone_bricks',
                        'mossy_cobblestone', 'granite', 'polished_granite', 'diorite', 'polished_diorite', 'andesite', 'polished_andesite',
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

                        // コンクリートパウダー（全16色）
                        'white_concrete_powder', 'orange_concrete_powder', 'magenta_concrete_powder', 'light_blue_concrete_powder',
                        'yellow_concrete_powder', 'lime_concrete_powder', 'pink_concrete_powder', 'gray_concrete_powder',
                        'light_gray_concrete_powder', 'cyan_concrete_powder', 'purple_concrete_powder', 'blue_concrete_powder',
                        'brown_concrete_powder', 'green_concrete_powder', 'red_concrete_powder', 'black_concrete_powder',

                        // テラコッタ（全16色）
                        'white_terracotta', 'orange_terracotta', 'magenta_terracotta', 'light_blue_terracotta',
                        'yellow_terracotta', 'lime_terracotta', 'pink_terracotta', 'gray_terracotta',
                        'light_gray_terracotta', 'cyan_terracotta', 'purple_terracotta', 'blue_terracotta',
                        'brown_terracotta', 'green_terracotta', 'red_terracotta', 'black_terracotta',

                        // 彩釉テラコッタ（全16色）
                        'white_glazed_terracotta', 'orange_glazed_terracotta', 'magenta_glazed_terracotta',
                        'light_blue_glazed_terracotta', 'yellow_glazed_terracotta', 'lime_glazed_terracotta',
                        'pink_glazed_terracotta', 'gray_glazed_terracotta', 'light_gray_glazed_terracotta',
                        'cyan_glazed_terracotta', 'purple_glazed_terracotta', 'blue_glazed_terracotta',
                        'brown_glazed_terracotta', 'green_glazed_terracotta', 'red_glazed_terracotta', 'black_glazed_terracotta',

                        // ガラス系
                        'glass', 'tinted_glass',
                        'white_stained_glass', 'orange_stained_glass', 'magenta_stained_glass', 'light_blue_stained_glass',
                        'yellow_stained_glass', 'lime_stained_glass', 'pink_stained_glass', 'gray_stained_glass',
                        'light_gray_stained_glass', 'cyan_stained_glass', 'purple_stained_glass', 'blue_stained_glass',
                        'brown_stained_glass', 'green_stained_glass', 'red_stained_glass', 'black_stained_glass',
                        // ガラス板（全種類）
                        'glass_pane',
                        'white_stained_glass_pane', 'orange_stained_glass_pane', 'magenta_stained_glass_pane', 'light_blue_stained_glass_pane',
                        'yellow_stained_glass_pane', 'lime_stained_glass_pane', 'pink_stained_glass_pane', 'gray_stained_glass_pane',
                        'light_gray_stained_glass_pane', 'cyan_stained_glass_pane', 'purple_stained_glass_pane', 'blue_stained_glass_pane',
                        'brown_stained_glass_pane', 'green_stained_glass_pane', 'red_stained_glass_pane', 'black_stained_glass_pane',

                        // 鉱石ブロック
                        'gold_block', 'iron_block', 'copper_block', 'diamond_block', 'emerald_block',
                        'coal_block', 'redstone_block', 'lapis_block',
                        'netherite_block', 'amethyst_block', 'raw_iron_block', 'raw_copper_block', 'raw_gold_block',

                        // 自然ブロック
                        'podzol', 'mycelium', 'dirt_path', 'farmland',
                        'coarse_dirt', 'rooted_dirt', 'mud', 'ice', 'packed_ice', 'blue_ice',
                        'snow_block', 'snow', 'powder_snow',
                        'mangrove_roots', 'muddy_mangrove_roots',

                        // 植物系
                        'oak_leaves', 'spruce_leaves', 'birch_leaves', 'jungle_leaves',
                        'acacia_leaves', 'dark_oak_leaves', 'mangrove_leaves', 'cherry_leaves',
                        'azalea_leaves', 'flowering_azalea_leaves',

                        // フェンス（全種類）
                        'oak_fence', 'spruce_fence', 'birch_fence', 'jungle_fence',
                        'acacia_fence', 'dark_oak_fence', 'mangrove_fence', 'cherry_fence',
                        'bamboo_fence', 'crimson_fence', 'warped_fence', 'nether_brick_fence',

                        // フェンスゲート（全種類）
                        'oak_fence_gate', 'spruce_fence_gate', 'birch_fence_gate', 'jungle_fence_gate',
                        'acacia_fence_gate', 'dark_oak_fence_gate', 'mangrove_fence_gate', 'cherry_fence_gate',
                        'bamboo_fence_gate', 'crimson_fence_gate', 'warped_fence_gate',

                        // 塀（全種類）
                        'cobblestone_wall', 'mossy_cobblestone_wall', 'stone_brick_wall', 'brick_wall',
                        'granite_wall', 'diorite_wall', 'andesite_wall', 'sandstone_wall', 'red_sandstone_wall',
                        'nether_brick_wall', 'red_nether_brick_wall', 'prismarine_wall', 'end_stone_brick_wall', 'mud_brick_wall',
                        'cobbled_deepslate_wall', 'polished_deepslate_wall', 'deepslate_brick_wall', 'deepslate_tile_wall',
                        'blackstone_wall', 'polished_blackstone_wall', 'polished_blackstone_brick_wall',

                        // ドア（全種類）
                        'oak_door', 'spruce_door', 'birch_door', 'jungle_door',
                        'acacia_door', 'dark_oak_door', 'mangrove_door', 'cherry_door',
                        'bamboo_door', 'crimson_door', 'warped_door', 'iron_door',

                        // トラップドア（全種類）
                        'oak_trapdoor', 'spruce_trapdoor', 'birch_trapdoor', 'jungle_trapdoor',
                        'acacia_trapdoor', 'dark_oak_trapdoor', 'mangrove_trapdoor', 'cherry_trapdoor',
                        'bamboo_trapdoor', 'crimson_trapdoor', 'warped_trapdoor', 'iron_trapdoor',

                        // ボタン（全種類）
                        'oak_button', 'spruce_button', 'birch_button', 'jungle_button',
                        'acacia_button', 'dark_oak_button', 'mangrove_button', 'cherry_button',
                        'bamboo_button', 'crimson_button', 'warped_button', 'stone_button',

                        // 感圧板（全種類）
                        'oak_pressure_plate', 'spruce_pressure_plate', 'birch_pressure_plate', 'jungle_pressure_plate',
                        'acacia_pressure_plate', 'dark_oak_pressure_plate', 'mangrove_pressure_plate', 'cherry_pressure_plate',
                        'bamboo_pressure_plate', 'crimson_pressure_plate', 'warped_pressure_plate', 'stone_pressure_plate',
                        'heavy_weighted_pressure_plate', 'light_weighted_pressure_plate',

                        // 建築用装飾ブロック
                        'ladder', 'chain', 'lantern', 'soul_lantern', 'iron_bars',
                        'bell', 'anvil', 'barrel', 'brewing_stand', 'cauldron',
                        'composter', 'grindstone', 'lectern', 'stonecutter',

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
                        'water', 'lava', 'magma_block',

                        // シュルカーボックス（全17種類）
                        'shulker_box',
                        'white_shulker_box', 'orange_shulker_box', 'magenta_shulker_box', 'light_blue_shulker_box',
                        'yellow_shulker_box', 'lime_shulker_box', 'pink_shulker_box', 'gray_shulker_box',
                        'light_gray_shulker_box', 'cyan_shulker_box', 'purple_shulker_box', 'blue_shulker_box',
                        'brown_shulker_box', 'green_shulker_box', 'red_shulker_box', 'black_shulker_box',

                        // ベッド（全16色）
                        'white_bed', 'orange_bed', 'magenta_bed', 'light_blue_bed',
                        'yellow_bed', 'lime_bed', 'pink_bed', 'gray_bed',
                        'light_gray_bed', 'cyan_bed', 'purple_bed', 'blue_bed',
                        'brown_bed', 'green_bed', 'red_bed', 'black_bed',

                        // キャンドル（全17種類）
                        'candle',
                        'white_candle', 'orange_candle', 'magenta_candle', 'light_blue_candle',
                        'yellow_candle', 'lime_candle', 'pink_candle', 'gray_candle',
                        'light_gray_candle', 'cyan_candle', 'purple_candle', 'blue_candle',
                        'brown_candle', 'green_candle', 'red_candle', 'black_candle'
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
                            authToken: this._generateClientToken(),
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

                    // ユーザーに分かりやすいエラーメッセージを作成
                    const userMessage = 'Minecraftに接続できませんでした。\n\n' +
                        '以下を確認してください：\n' +
                        '1. Minecraftが起動しているか\n' +
                        '2. MODが正しくインストールされているか\n' +
                        '3. ホストとポートが正しいか（デフォルト: localhost:14711）';

                    reject(new Error(userMessage));
                };

                this.socket.onclose = () => {
                    console.log('Disconnected from Minecraft server');
                    this.connected = false;
                };

            } catch (error) {
                console.error('Connection error:', error);
                this.connected = false;

                // ユーザーに分かりやすいエラーメッセージを作成
                const userMessage = '接続中にエラーが発生しました。\n\n' +
                    '以下を確認してください：\n' +
                    '1. インターネット接続が正常か\n' +
                    '2. ファイアウォールでブロックされていないか\n' +
                    '3. ホスト名（' + (args.HOST || 'localhost') + '）が正しいか';

                reject(new Error(userMessage));
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
        // メッセージを検証
        let message = String(args.MESSAGE || '').trim();

        if (!message) {
            console.warn('チャットメッセージが空です。送信をスキップします。');
            return Promise.resolve();  // 空メッセージは無視
        }

        if (message.length > 256) {
            console.warn(`チャットメッセージが長すぎます（${message.length}文字）。256文字に短縮します。`);
            message = message.substring(0, 256);
        }

        return this.sendCommand('chat', {
            message: message
        });
    }

    /**
     * Scratch Y座標をMinecraft Y座標に変換
     * @param {number} scratchY - Scratch Y座標
     * @returns {number} Minecraft Y座標
     */
    _toMinecraftY(scratchY) {
        return Number(scratchY) + this.Y_OFFSET;
    }

    /**
     * Minecraft Y座標をScratch Y座標に変換
     * @param {number} minecraftY - Minecraft Y座標
     * @returns {number} Scratch Y座標
     */
    _toScratchY(minecraftY) {
        return Number(minecraftY) - this.Y_OFFSET;
    }

    /**
     * ブロックタイプとプロパティを構築
     * @param {string} blockId - ブロックID（例: 'stone_slab'）
     * @param {string} placement - 配置タイプ（'bottom', 'top', 'double'）
     * @param {string} facing - 向き（'north', 'south', 'east', 'west', 'none'）
     * @returns {string} 完全なブロックタイプ文字列（例: 'minecraft:stone_slab[type=top]'）
     */
    _buildBlockTypeWithProperties(blockId, placement, facing) {
        let blockType = 'minecraft:' + blockId;
        const properties = [];

        // スラブかどうかを判定
        const isSlab = blockId.includes('_slab');

        // 配置パラメータを適用
        if (placement && placement !== 'bottom') {
            if (isSlab) {
                // スラブの場合: type プロパティを使用
                properties.push('type=' + placement);
            } else {
                // 階段などの場合: half プロパティを使用
                properties.push('half=' + placement);
            }
        }

        // 向きパラメータを適用（階段ブロックなどの方向）
        if (facing && facing !== 'none') {
            properties.push('facing=' + facing);
        }

        // プロパティを結合
        if (properties.length > 0) {
            blockType += '[' + properties.join(',') + ']';
        }

        return blockType;
    }

    /**
     * クライアント用の一意な認証トークンを生成
     * @returns {string} 生成されたトークン
     */
    _generateClientToken() {
        // タイムスタンプとランダム値を組み合わせて一意なトークンを生成
        const timestamp = Date.now();
        const random = Math.random().toString(36).substring(2, 15);
        return `scratch_token_${timestamp}_${random}`;
    }

    /**
     * 座標値を検証（汎用）
     * @param {*} coord - 検証する座標値
     * @param {string} name - 座標名（エラーメッセージ用）
     * @param {number} min - 最小値
     * @param {number} max - 最大値
     * @returns {number} 検証済みの座標値
     * @throws {Error} 無効な座標値の場合
     */
    _validateCoordinate(coord, name, min, max) {
        const value = Number(coord);

        if (isNaN(value)) {
            console.error(`${name}が数値ではありません: ${coord}`);
            return 0;  // デフォルト値として0を返す（子供向けのため、エラーで止めない）
        }

        if (value < min || value > max) {
            console.warn(`${name}が範囲外です（${min}〜${max}）: ${value}。範囲内に収めます。`);
            // 範囲内に収める（clamp）
            return Math.max(min, Math.min(max, value));
        }

        return value;
    }

    /**
     * X/Z座標を検証
     * @param {*} coord - 検証する座標値
     * @param {string} name - 座標名（'X'または'Z'）
     * @returns {number} 検証済みの座標値
     */
    _validateXZCoordinate(coord, name) {
        return this._validateCoordinate(coord, name, this.MIN_XZ_COORD, this.MAX_XZ_COORD);
    }

    /**
     * Y座標を検証
     * @param {*} coord - 検証する座標値
     * @param {string} name - 座標名（'Y'）
     * @param {boolean} isScratchY - Scratch座標系かどうか（true: Scratch、false: Minecraft）
     * @returns {number} 検証済みの座標値
     */
    _validateYCoordinate(coord, name, isScratchY = true) {
        if (isScratchY) {
            // Scratch座標系の場合: Minecraft座標系に変換してから範囲チェック
            const minScratchY = this.MIN_Y_COORD - this.Y_OFFSET;  // -64 - (-60) = -4
            const maxScratchY = this.MAX_Y_COORD - this.Y_OFFSET;  // 319 - (-60) = 379
            return this._validateCoordinate(coord, name, minScratchY, maxScratchY);
        } else {
            // Minecraft座標系の場合
            return this._validateCoordinate(coord, name, this.MIN_Y_COORD, this.MAX_Y_COORD);
        }
    }

    /**
     * ブロック配置（絶対座標）
     * Y座標変換: ScratchのY=0 → MinecraftのY=-60（スーパーフラット地表）
     */
    setBlock(args) {
        const blockType = this._buildBlockTypeWithProperties(args.BLOCK, args.PLACEMENT, args.FACING);

        // 座標を検証
        const x = this._validateXZCoordinate(args.X, 'X座標');
        const y = this._validateYCoordinate(args.Y, 'Y座標', true);  // Scratch座標系
        const z = this._validateXZCoordinate(args.Z, 'Z座標');

        return this.sendCommand('setBlock', {
            x: x,
            y: this._toMinecraftY(y),
            z: z,
            blockType: blockType
        });
    }

    /**
     * ブロック配置（相対座標）
     */
    setBlockRelative(args) {
        const blockType = this._buildBlockTypeWithProperties(args.BLOCK, args.PLACEMENT, args.FACING);

        // 相対座標の検証（NaNチェックのみ、範囲は現在位置次第）
        const relX = Number(args.X) || 0;
        const relY = Number(args.Y) || 0;
        const relZ = Number(args.Z) || 0;

        return this.sendCommand('setBlock', {
            relativeX: relX,
            relativeY: relY,
            relativeZ: relZ,
            blockType: blockType
        });
    }

    /**
     * ブロック範囲設置
     * Y座標変換: ScratchのY=0 → MinecraftのY=-60（スーパーフラット地表）
     */
    setBlockRange(args) {
        // 座標を検証
        const x1 = Math.floor(this._validateXZCoordinate(args.X1, 'X1座標'));
        const x2 = Math.floor(this._validateXZCoordinate(args.X2, 'X2座標'));
        const y1Scratch = this._validateYCoordinate(args.Y1, 'Y1座標', true);
        const y2Scratch = this._validateYCoordinate(args.Y2, 'Y2座標', true);
        const y1 = Math.floor(this._toMinecraftY(y1Scratch));
        const y2 = Math.floor(this._toMinecraftY(y2Scratch));
        const z1 = Math.floor(this._validateXZCoordinate(args.Z1, 'Z1座標'));
        const z2 = Math.floor(this._validateXZCoordinate(args.Z2, 'Z2座標'));

        const blockType = this._buildBlockTypeWithProperties(args.BLOCK, args.PLACEMENT, args.FACING);

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
            const userMessage = `置こうとしているブロックが多すぎます！\n現在: ${volume.toLocaleString()}個\nもう少し小さい範囲にしてみてください（最大200万個まで）。`;
            console.warn(userMessage);
            return Promise.reject(new Error(userMessage));
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
        // 座標を検証
        const x = this._validateXZCoordinate(args.X, 'X座標');
        const y = this._validateYCoordinate(args.Y, 'Y座標', true);
        const z = this._validateXZCoordinate(args.Z, 'Z座標');

        return this.sendCommand('summonEntity', {
            entityType: 'minecraft:' + args.ENTITY,
            x: x,
            y: this._toMinecraftY(y),
            z: z
        });
    }

    /**
     * テレポート
     * Y座標変換: ScratchのY=0 → MinecraftのY=-60（スーパーフラット地表）
     */
    teleport(args) {
        // 座標を検証
        const x = this._validateXZCoordinate(args.X, 'X座標');
        const y = this._validateYCoordinate(args.Y, 'Y座標', true);
        const z = this._validateXZCoordinate(args.Z, 'Z座標');

        return this.sendCommand('teleport', {
            x: x,
            y: this._toMinecraftY(y),
            z: z
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
                            // Y座標逆変換: Minecraft座標をScratch座標に変換
                            return this._toScratchY(result.y || 0);
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
        // 座標を検証
        const centerX = this._validateXZCoordinate(args.X || 0, 'X座標');
        const centerZ = this._validateXZCoordinate(args.Z || 0, 'Z座標');
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
        // 座標を検証
        const centerX = this._validateXZCoordinate(args.X || 0, 'X座標');
        const centerZ = this._validateXZCoordinate(args.Z || 0, 'Z座標');
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
