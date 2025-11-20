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
                    opcode: 'blockSlab',
                    blockType: 'reporter',
                    text: 'スラブ [SLAB]',
                    arguments: {
                        SLAB: {
                            type: 'string',
                            menu: 'slabBlocks',
                            defaultValue: 'vertical_oak_slab'
                        }
                    }
                },
                '---',
                {
                    opcode: 'isConnected',
                    blockType: 'Boolean',
                    text: '接続中？'
                },
                '---',
                {
                    opcode: 'blockBuilding',
                    blockType: 'reporter',
                    text: '建築ブロック [BLOCK]',
                    arguments: {
                        BLOCK: {
                            type: 'string',
                            menu: 'buildingBlocks',
                            defaultValue: 'stone'
                        }
                    }
                },
                {
                    opcode: 'blockLighting',
                    blockType: 'reporter',
                    text: '照明ブロック [BLOCK]',
                    arguments: {
                        BLOCK: {
                            type: 'string',
                            menu: 'lightingBlocks',
                            defaultValue: 'torch'
                        }
                    }
                },
                {
                    opcode: 'blockDecoration',
                    blockType: 'reporter',
                    text: '装飾ブロック [BLOCK]',
                    arguments: {
                        BLOCK: {
                            type: 'string',
                            menu: 'decorationBlocks',
                            defaultValue: 'white_wool'
                        }
                    }
                },
                {
                    opcode: 'blockNature',
                    blockType: 'reporter',
                    text: '自然ブロック [BLOCK]',
                    arguments: {
                        BLOCK: {
                            type: 'string',
                            menu: 'natureBlocks',
                            defaultValue: 'grass_block'
                        }
                    }
                },
                {
                    opcode: 'blockFunctional',
                    blockType: 'reporter',
                    text: '機能ブロック [BLOCK]',
                    arguments: {
                        BLOCK: {
                            type: 'string',
                            menu: 'functionalBlocks',
                            defaultValue: 'tnt'
                        }
                    }
                },
                {
                    opcode: 'blockOre',
                    blockType: 'reporter',
                    text: '鉱石ブロック [BLOCK]',
                    arguments: {
                        BLOCK: {
                            type: 'string',
                            menu: 'oreBlocks',
                            defaultValue: 'iron_block'
                        }
                    }
                },
                {
                    opcode: 'blockSpecial',
                    blockType: 'reporter',
                    text: '特殊ブロック [BLOCK]',
                    arguments: {
                        BLOCK: {
                            type: 'string',
                            menu: 'specialBlocks',
                            defaultValue: 'bedrock'
                        }
                    }
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

                        // 垂直ハーフブロック（MOD追加ブロック）
                        // 木材系
                        'vertical_oak_slab', 'vertical_birch_slab', 'vertical_spruce_slab', 'vertical_jungle_slab',
                        'vertical_acacia_slab', 'vertical_dark_oak_slab', 'vertical_cherry_slab', 'vertical_mangrove_slab',
                        'vertical_crimson_slab', 'vertical_warped_slab',
                        // 石材系
                        'vertical_stone_slab', 'vertical_cobblestone_slab', 'vertical_stone_brick_slab', 'vertical_smooth_stone_slab',
                        'vertical_andesite_slab', 'vertical_granite_slab', 'vertical_diorite_slab', 'vertical_sandstone_slab',
                        'vertical_brick_slab', 'vertical_quartz_slab',
                        // 鉱石・鉱物系
                        'vertical_iron_block_slab', 'vertical_gold_block_slab', 'vertical_diamond_block_slab', 'vertical_emerald_block_slab',
                        'vertical_copper_block_slab', 'vertical_lapis_block_slab', 'vertical_redstone_block_slab', 'vertical_coal_block_slab',
                        'vertical_netherite_block_slab', 'vertical_amethyst_block_slab',
                        // 銅系（日本建築の屋根用）
                        'vertical_cut_copper_slab', 'vertical_exposed_cut_copper_slab', 'vertical_weathered_cut_copper_slab', 'vertical_oxidized_cut_copper_slab',

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
                        {text: '通常（下）', value: 'bottom'},
                        {text: '上下反転（上）', value: 'top'},
                        {text: 'ダブル', value: 'double'}
                    ]
                },
                blockFacing: {
                    acceptReporters: false,
                    items: [
                        {text: 'デフォルト', value: 'none'},
                        {text: '北', value: 'north'},
                        {text: '南', value: 'south'},
                        {text: '東', value: 'east'},
                        {text: '西', value: 'west'}
                    ]
                },
                entityTypes: {
                    acceptReporters: true,
                    items: [
                        {text: 'ブタ', value: 'pig'},
                        {text: 'ウシ', value: 'cow'},
                        {text: 'ヒツジ', value: 'sheep'},
                        {text: 'ニワトリ', value: 'chicken'},
                        {text: 'ゾンビ', value: 'zombie'},
                        {text: 'スケルトン', value: 'skeleton'},
                        {text: 'クリーパー', value: 'creeper'},
                        {text: 'クモ', value: 'spider'},
                        {text: 'ウマ', value: 'horse'},
                        {text: 'オオカミ', value: 'wolf'},
                        {text: 'ネコ', value: 'cat'},
                        {text: '村人', value: 'villager'}
                    ]
                },
                coordinates: {
                    acceptReporters: false,
                    items: ['x', 'y', 'z']
                },
                weatherTypes: {
                    acceptReporters: false,
                    items: [
                        {text: '晴れ', value: 'clear'},
                        {text: '雨', value: 'rain'},
                        {text: '雷雨', value: 'thunder'}
                    ]
                },
                timeValues: {
                    acceptReporters: true,
                    items: [
                        {text: '朝', value: 'day'},
                        {text: '昼', value: 'noon'},
                        {text: '夕方', value: 'sunset'},
                        {text: '夜', value: 'night'},
                        {text: '真夜中', value: 'midnight'}
                    ]
                },
                slabBlocks: {
                    acceptReporters: true,
                    items: [
                        // 木材系垂直スラブ
                        {text: 'オークの垂直スラブ', value: 'vertical_oak_slab'},
                        {text: 'シラカバの垂直スラブ', value: 'vertical_birch_slab'},
                        {text: 'マツの垂直スラブ', value: 'vertical_spruce_slab'},
                        {text: 'ジャングルの垂直スラブ', value: 'vertical_jungle_slab'},
                        {text: 'アカシアの垂直スラブ', value: 'vertical_acacia_slab'},
                        {text: 'ダークオークの垂直スラブ', value: 'vertical_dark_oak_slab'},
                        {text: 'サクラの垂直スラブ', value: 'vertical_cherry_slab'},
                        {text: 'マングローブの垂直スラブ', value: 'vertical_mangrove_slab'},
                        {text: '真紅の垂直スラブ', value: 'vertical_crimson_slab'},
                        {text: '歪んだ垂直スラブ', value: 'vertical_warped_slab'},

                        // 石材系垂直スラブ
                        {text: '石の垂直スラブ', value: 'vertical_stone_slab'},
                        {text: '丸石の垂直スラブ', value: 'vertical_cobblestone_slab'},
                        {text: '石レンガの垂直スラブ', value: 'vertical_stone_brick_slab'},
                        {text: '滑らかな石の垂直スラブ', value: 'vertical_smooth_stone_slab'},
                        {text: '安山岩の垂直スラブ', value: 'vertical_andesite_slab'},
                        {text: '花崗岩の垂直スラブ', value: 'vertical_granite_slab'},
                        {text: '閃緑岩の垂直スラブ', value: 'vertical_diorite_slab'},
                        {text: '砂岩の垂直スラブ', value: 'vertical_sandstone_slab'},
                        {text: 'レンガの垂直スラブ', value: 'vertical_brick_slab'},
                        {text: 'クォーツの垂直スラブ', value: 'vertical_quartz_slab'},

                        // 鉱石・鉱物系垂直スラブ
                        {text: '鉄ブロックの垂直スラブ', value: 'vertical_iron_block_slab'},
                        {text: '金ブロックの垂直スラブ', value: 'vertical_gold_block_slab'},
                        {text: 'ダイヤブロックの垂直スラブ', value: 'vertical_diamond_block_slab'},
                        {text: 'エメラルドブロックの垂直スラブ', value: 'vertical_emerald_block_slab'},
                        {text: '銅ブロックの垂直スラブ', value: 'vertical_copper_block_slab'},
                        {text: 'ラピスラズリブロックの垂直スラブ', value: 'vertical_lapis_block_slab'},
                        {text: 'レッドストーンブロックの垂直スラブ', value: 'vertical_redstone_block_slab'},
                        {text: '石炭ブロックの垂直スラブ', value: 'vertical_coal_block_slab'},
                        {text: 'ネザライトブロックの垂直スラブ', value: 'vertical_netherite_block_slab'},
                        {text: 'アメジストブロックの垂直スラブ', value: 'vertical_amethyst_block_slab'},

                        // 銅系垂直スラブ（日本建築の屋根用）
                        {text: '切り込み入り銅の垂直スラブ', value: 'vertical_cut_copper_slab'},
                        {text: '風化した切り込み入り銅の垂直スラブ', value: 'vertical_exposed_cut_copper_slab'},
                        {text: '錆びた切り込み入り銅の垂直スラブ', value: 'vertical_weathered_cut_copper_slab'},
                        {text: '酸化した切り込み入り銅の垂直スラブ', value: 'vertical_oxidized_cut_copper_slab'}
                    ]
                },
                buildingBlocks: {
                    acceptReporters: true,
                    items: BUILDING_BLOCKS
                },
                lightingBlocks: {
                    acceptReporters: true,
                    items: LIGHTING_BLOCKS
                },
                decorationBlocks: {
                    acceptReporters: true,
                    items: DECORATION_BLOCKS
                },
                natureBlocks: {
                    acceptReporters: true,
                    items: NATURE_BLOCKS
                },
                functionalBlocks: {
                    acceptReporters: true,
                    items: FUNCTIONAL_BLOCKS
                },
                oreBlocks: {
                    acceptReporters: true,
                    items: ORE_BLOCKS
                },
                specialBlocks: {
                    acceptReporters: true,
                    items: SPECIAL_BLOCKS
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
                            clientId: `scratch_client_${Date.now()}`,
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

                this.socket.onmessage = event => {
                    this.handleMessage(JSON.parse(event.data));
                };

                this.socket.onerror = error => {
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
                const userMessage = `接続中にエラーが発生しました。\n\n` +
                    `以下を確認してください：\n` +
                    `1. インターネット接続が正常か\n` +
                    `2. ファイアウォールでブロックされていないか\n` +
                    `3. ホスト名（${args.HOST || 'localhost'}）が正しいか`;

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
            message
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
     * @param {string} blockId - ブロックID（例: 'stone_slab', 'vertical_oak_slab'）
     * @param {string} placement - 配置タイプ（'bottom', 'top', 'double'）
     * @param {string} facing - 向き（'north', 'south', 'east', 'west', 'none'）
     * @returns {string} 完全なブロックタイプ文字列（例: 'minecraft:stone_slab[type=top]', 'minecraftedu:vertical_oak_slab[facing=north]'）
     */
    _buildBlockTypeWithProperties(blockId, placement, facing) {
        // 垂直スラブかどうかを判定（MOD追加ブロック）
        const isVerticalSlab = blockId.startsWith('vertical_') && blockId.includes('_slab');

        // 垂直スラブの場合は minecraftedu 名前空間を使用、それ以外は minecraft を使用
        const namespace = isVerticalSlab ? 'minecraftedu' : 'minecraft';
        let blockType = `${namespace}:${blockId}`;
        const properties = [];

        // 垂直スラブの場合は特別な処理
        if (isVerticalSlab) {
            // 垂直スラブは向きプロパティのみを持つ（placementは無視）
            // facingは必須プロパティなので、'none'の場合はデフォルトで'north'を使用
            const verticalSlabFacing = (facing && facing !== 'none') ? facing : 'north';
            properties.push(`facing=${verticalSlabFacing}`);
        } else {
            // 通常のブロック（バニラスラブ、階段など）の処理
            const isSlab = blockId.includes('_slab');

            // 配置パラメータを適用
            if (placement && placement !== 'bottom') {
                if (isSlab) {
                    // バニラスラブの場合: type プロパティを使用
                    properties.push(`type=${placement}`);
                } else {
                    // 階段などの場合: half プロパティを使用
                    properties.push(`half=${placement}`);
                }
            }

            // 向きパラメータを適用（階段、ドア、フェンスゲートなど - バニラスラブ以外）
            // バニラスラブには方向性がないため、isSlab=trueの場合はfacingを追加しない
            if (facing && facing !== 'none' && !isSlab) {
                properties.push(`facing=${facing}`);
            }
        }

        // プロパティを結合
        if (properties.length > 0) {
            blockType += `[${properties.join(',')}]`;
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
            x,
            y: this._toMinecraftY(y),
            z,
            blockType
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
            blockType
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
            blockType
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
            entityType: `minecraft:${args.ENTITY}`,
            x,
            y: this._toMinecraftY(y),
            z
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
            x,
            y: this._toMinecraftY(y),
            z
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
                    const {result} = response.payload;
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
     * スラブレポーター
     * @param {object} args - ブロック引数
     * @param {string} args.SLAB - スラブの種類
     * @returns {string} スラブID
     */
    blockSlab(args) {
        return args.SLAB;
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
                action,
                params
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
                resolve: response => {
                    clearTimeout(timeoutId);
                    this.pendingRequests.delete(messageId);
                    resolve(response);
                },
                reject: error => {
                    clearTimeout(timeoutId);
                    this.pendingRequests.delete(messageId);
                    reject(error);
                }
            });

            this.sendMessageWithId(messageId, {
                type: 'command',
                payload: {
                    action,
                    params
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
                    messageId,
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
            centerX,
            centerZ
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
            centerX,
            centerZ
        });
    }

    /**
     * UUID生成
     */
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    /**
     * 建築ブロック選択
     */
    blockBuilding(args) {
        return args.BLOCK;
    }

    /**
     * 照明ブロック選択
     */
    blockLighting(args) {
        return args.BLOCK;
    }

    /**
     * 装飾ブロック選択
     */
    blockDecoration(args) {
        return args.BLOCK;
    }

    /**
     * 自然ブロック選択
     */
    blockNature(args) {
        return args.BLOCK;
    }

    /**
     * 機能ブロック選択
     */
    blockFunctional(args) {
        return args.BLOCK;
    }

    /**
     * 鉱石ブロック選択
     */
    blockOre(args) {
        return args.BLOCK;
    }

    /**
     * 特殊ブロック選択
     */
    blockSpecial(args) {
        return args.BLOCK;
    }
}

// カテゴリ別ブロック定義
const BUILDING_BLOCKS = [{
  text: '石',
  value: 'stone'
}, {
  text: '丸石',
  value: 'cobblestone'
}, {
  text: 'テラコッタ',
  value: 'terracotta'
}, {
  text: 'オークの板材',
  value: 'oak_planks'
}, {
  text: 'トウヒの板材',
  value: 'spruce_planks'
}, {
  text: 'シラカバの板材',
  value: 'birch_planks'
}, {
  text: 'ジャングルの板材',
  value: 'jungle_planks'
}, {
  text: 'アカシアの板材',
  value: 'acacia_planks'
}, {
  text: 'ダークオークの板材',
  value: 'dark_oak_planks'
}, {
  text: 'マングローブの板材',
  value: 'mangrove_planks'
}, {
  text: 'サクラの板材',
  value: 'cherry_planks'
}, {
  text: '竹の板材',
  value: 'bamboo_planks'
}, {
  text: '真紅の板材',
  value: 'crimson_planks'
}, {
  text: '歪んだ板材',
  value: 'warped_planks'
}, {
  text: '樹皮を剥いだオークの原木',
  value: 'stripped_oak_log'
}, {
  text: '樹皮を剥いだトウヒの原木',
  value: 'stripped_spruce_log'
}, {
  text: '樹皮を剥いだシラカバの原木',
  value: 'stripped_birch_log'
}, {
  text: '樹皮を剥いだジャングルの原木',
  value: 'stripped_jungle_log'
}, {
  text: '樹皮を剥いだアカシアの原木',
  value: 'stripped_acacia_log'
}, {
  text: '樹皮を剥いだダークオークの原木',
  value: 'stripped_dark_oak_log'
}, {
  text: '樹皮を剥いだマングローブの原木',
  value: 'stripped_mangrove_log'
}, {
  text: '樹皮を剥いだサクラの原木',
  value: 'stripped_cherry_log'
}, {
  text: '樹皮を剥いだ真紅の幹',
  value: 'stripped_crimson_stem'
}, {
  text: '樹皮を剥いだ歪んだ幹',
  value: 'stripped_warped_stem'
}, {
  text: 'オークの木',
  value: 'oak_wood'
}, {
  text: 'トウヒの木',
  value: 'spruce_wood'
}, {
  text: 'シラカバの木',
  value: 'birch_wood'
}, {
  text: 'ジャングルの木',
  value: 'jungle_wood'
}, {
  text: 'アカシアの木',
  value: 'acacia_wood'
}, {
  text: 'ダークオークの木',
  value: 'dark_oak_wood'
}, {
  text: 'マングローブの木',
  value: 'mangrove_wood'
}, {
  text: 'サクラの木',
  value: 'cherry_wood'
}, {
  text: '真紅の菌糸',
  value: 'crimson_hyphae'
}, {
  text: '歪んだ菌糸',
  value: 'warped_hyphae'
}, {
  text: '樹皮を剥いだオークの木',
  value: 'stripped_oak_wood'
}, {
  text: '樹皮を剥いだトウヒの木',
  value: 'stripped_spruce_wood'
}, {
  text: '樹皮を剥いだシラカバの木',
  value: 'stripped_birch_wood'
}, {
  text: '樹皮を剥いだジャングルの木',
  value: 'stripped_jungle_wood'
}, {
  text: '樹皮を剥いだアカシアの木',
  value: 'stripped_acacia_wood'
}, {
  text: '樹皮を剥いだダークオークの木',
  value: 'stripped_dark_oak_wood'
}, {
  text: '樹皮を剥いだマングローブの木',
  value: 'stripped_mangrove_wood'
}, {
  text: '樹皮を剥いだサクラの木',
  value: 'stripped_cherry_wood'
}, {
  text: '樹皮を剥いだ真紅の菌糸',
  value: 'stripped_crimson_hyphae'
}, {
  text: '樹皮を剥いだ歪んだ菌糸',
  value: 'stripped_warped_hyphae'
}, {
  text: '安山岩',
  value: 'andesite'
}, {
  text: '磨かれた安山岩',
  value: 'polished_andesite'
}, {
  text: '閃緑岩',
  value: 'diorite'
}, {
  text: '磨かれた閃緑岩',
  value: 'polished_diorite'
}, {
  text: '花崗岩',
  value: 'granite'
}, {
  text: '磨かれた花崗岩',
  value: 'polished_granite'
}, {
  text: '方解石',
  value: 'calcite'
}, {
  text: '凝灰岩',
  value: 'tuff'
}, {
  text: '模様入りのディープスレート',
  value: 'chiseled_deepslate'
}, {
  text: '丸石ディープスレート',
  value: 'cobbled_deepslate'
}, {
  text: 'ひび割れたディープスレートレンガ',
  value: 'cracked_deepslate_bricks'
}, {
  text: 'ひび割れたディープスレートタイル',
  value: 'cracked_deepslate_tiles'
}, {
  text: 'ディープスレート',
  value: 'deepslate'
}, {
  text: 'ディープスレートレンガ',
  value: 'deepslate_bricks'
}, {
  text: 'ディープスレートタイル',
  value: 'deepslate_tiles'
}, {
  text: '磨かれたディープスレート',
  value: 'polished_deepslate'
}, {
  text: '玄武岩',
  value: 'basalt'
}, {
  text: '磨かれた玄武岩',
  value: 'polished_basalt'
}, {
  text: '黒石',
  value: 'blackstone'
}, {
  text: '金入りの黒石',
  value: 'gilded_blackstone'
}, {
  text: '磨かれた黒石',
  value: 'polished_blackstone'
}, {
  text: '磨かれた黒石レンガ',
  value: 'polished_blackstone_bricks'
}, {
  text: 'ひび割れた磨かれた黒石レンガ',
  value: 'cracked_polished_blackstone_bricks'
}, {
  text: 'レンガ',
  value: 'bricks'
}, {
  text: 'エンドストーンレンガ',
  value: 'end_stone_bricks'
}, {
  text: 'ネザーレンガ',
  value: 'nether_bricks'
}, {
  text: '赤いネザーレンガ',
  value: 'red_nether_bricks'
}, {
  text: 'プリズマリンレンガ',
  value: 'prismarine_bricks'
}, {
  text: 'クォーツレンガ',
  value: 'quartz_bricks'
}, {
  text: '石レンガ',
  value: 'stone_bricks'
}, {
  text: '苔むした石レンガ',
  value: 'mossy_stone_bricks'
}, {
  text: 'ひび割れた石レンガ',
  value: 'cracked_stone_bricks'
}, {
  text: '模様入りの石レンガ',
  value: 'chiseled_stone_bricks'
}, {
  text: '模様入りのネザーレンガ',
  value: 'chiseled_nether_bricks'
}, {
  text: 'ひび割れたネザーレンガ',
  value: 'cracked_nether_bricks'
}, {
  text: '泥レンガ',
  value: 'mud_bricks'
}, {
  text: '白色のコンクリート',
  value: 'white_concrete'
}, {
  text: '橙色のコンクリート',
  value: 'orange_concrete'
}, {
  text: '赤紫色のコンクリート',
  value: 'magenta_concrete'
}, {
  text: '空色のコンクリート',
  value: 'light_blue_concrete'
}, {
  text: '黄色のコンクリート',
  value: 'yellow_concrete'
}, {
  text: '黄緑色のコンクリート',
  value: 'lime_concrete'
}, {
  text: '桃色のコンクリート',
  value: 'pink_concrete'
}, {
  text: '灰色のコンクリート',
  value: 'gray_concrete'
}, {
  text: '薄灰色のコンクリート',
  value: 'light_gray_concrete'
}, {
  text: '青緑色のコンクリート',
  value: 'cyan_concrete'
}, {
  text: '紫色のコンクリート',
  value: 'purple_concrete'
}, {
  text: '青色のコンクリート',
  value: 'blue_concrete'
}, {
  text: '茶色のコンクリート',
  value: 'brown_concrete'
}, {
  text: '緑色のコンクリート',
  value: 'green_concrete'
}, {
  text: '赤色のコンクリート',
  value: 'red_concrete'
}, {
  text: '黒色のコンクリート',
  value: 'black_concrete'
}, {
  text: 'ダークプリズマリン',
  value: 'dark_prismarine'
}, {
  text: '模様入りのクォーツブロック',
  value: 'chiseled_quartz_block'
}, {
  text: 'クォーツの柱',
  value: 'quartz_pillar'
}, {
  text: '滑らかなクォーツ',
  value: 'smooth_quartz'
}, {
  text: 'プルパーブロック',
  value: 'purpur_block'
}, {
  text: 'プルパーの柱',
  value: 'purpur_pillar'
}, {
  text: '竹モザイク',
  value: 'bamboo_mosaic'
}, {
  text: '砂岩',
  value: 'sandstone'
}, {
  text: '模様入りの砂岩',
  value: 'chiseled_sandstone'
}, {
  text: 'カットされた砂岩',
  value: 'cut_sandstone'
}, {
  text: '滑らかな砂岩',
  value: 'smooth_sandstone'
}, {
  text: '赤い砂岩',
  value: 'red_sandstone'
}, {
  text: '模様入りの赤い砂岩',
  value: 'chiseled_red_sandstone'
}, {
  text: 'カットされた赤い砂岩',
  value: 'cut_red_sandstone'
}, {
  text: '滑らかな赤い砂岩',
  value: 'smooth_red_sandstone'
}, {
  text: '滑らかな石',
  value: 'smooth_stone'
}, {
  text: 'オークの階段',
  value: 'oak_stairs'
}, {
  text: 'トウヒの階段',
  value: 'spruce_stairs'
}, {
  text: 'シラカバの階段',
  value: 'birch_stairs'
}, {
  text: 'ジャングルの階段',
  value: 'jungle_stairs'
}, {
  text: 'アカシアの階段',
  value: 'acacia_stairs'
}, {
  text: 'ダークオークの階段',
  value: 'dark_oak_stairs'
}, {
  text: 'マングローブの階段',
  value: 'mangrove_stairs'
}, {
  text: 'サクラの階段',
  value: 'cherry_stairs'
}, {
  text: '竹モザイクの階段',
  value: 'bamboo_mosaic_stairs'
}, {
  text: '竹の階段',
  value: 'bamboo_stairs'
}, {
  text: '真紅の階段',
  value: 'crimson_stairs'
}, {
  text: '歪んだ階段',
  value: 'warped_stairs'
}, {
  text: '黒石の階段',
  value: 'blackstone_stairs'
}, {
  text: '丸石の階段',
  value: 'cobblestone_stairs'
}, {
  text: 'エンドストーンレンガの階段',
  value: 'end_stone_brick_stairs'
}, {
  text: '磨かれた黒石レンガの階段',
  value: 'polished_blackstone_brick_stairs'
}, {
  text: '磨かれた黒石の階段',
  value: 'polished_blackstone_stairs'
}, {
  text: '赤い砂岩の階段',
  value: 'red_sandstone_stairs'
}, {
  text: '砂岩の階段',
  value: 'sandstone_stairs'
}, {
  text: '滑らかな赤い砂岩の階段',
  value: 'smooth_red_sandstone_stairs'
}, {
  text: '滑らかな砂岩の階段',
  value: 'smooth_sandstone_stairs'
}, {
  text: '石レンガの階段',
  value: 'stone_brick_stairs'
}, {
  text: '石の階段',
  value: 'stone_stairs'
}, {
  text: '安山岩の階段',
  value: 'andesite_stairs'
}, {
  text: '磨かれた安山岩の階段',
  value: 'polished_andesite_stairs'
}, {
  text: '閃緑岩の階段',
  value: 'diorite_stairs'
}, {
  text: '磨かれた閃緑岩の階段',
  value: 'polished_diorite_stairs'
}, {
  text: '花崗岩の階段',
  value: 'granite_stairs'
}, {
  text: '磨かれた花崗岩の階段',
  value: 'polished_granite_stairs'
}, {
  text: '丸石状ディープスレートの階段',
  value: 'cobbled_deepslate_stairs'
}, {
  text: '磨かれたディープスレートの階段',
  value: 'polished_deepslate_stairs'
}, {
  text: 'ディープスレートレンガの階段',
  value: 'deepslate_brick_stairs'
}, {
  text: 'ディープスレートタイルの階段',
  value: 'deepslate_tile_stairs'
}, {
  text: 'レンガの階段',
  value: 'brick_stairs'
}, {
  text: 'ネザーレンガの階段',
  value: 'nether_brick_stairs'
}, {
  text: 'プリズマリンレンガの階段',
  value: 'prismarine_brick_stairs'
}, {
  text: '赤いネザーレンガの階段',
  value: 'red_nether_brick_stairs'
}, {
  text: 'ダークプリズマリンの階段',
  value: 'dark_prismarine_stairs'
}, {
  text: 'プリズマリンの階段',
  value: 'prismarine_stairs'
}, {
  text: 'クォーツの階段',
  value: 'quartz_stairs'
}, {
  text: 'プルパーの階段',
  value: 'purpur_stairs'
}, {
  text: 'オークのハーフブロック',
  value: 'oak_slab'
}, {
  text: 'トウヒのハーフブロック',
  value: 'spruce_slab'
}, {
  text: 'シラカバのハーフブロック',
  value: 'birch_slab'
}, {
  text: 'ジャングルのハーフブロック',
  value: 'jungle_slab'
}, {
  text: 'アカシアのハーフブロック',
  value: 'acacia_slab'
}, {
  text: 'ダークオークのハーフブロック',
  value: 'dark_oak_slab'
}, {
  text: 'マングローブのハーフブロック',
  value: 'mangrove_slab'
}, {
  text: 'サクラのハーフブロック',
  value: 'cherry_slab'
}, {
  text: '竹モザイクのハーフブロック',
  value: 'bamboo_mosaic_slab'
}, {
  text: '竹のハーフブロック',
  value: 'bamboo_slab'
}, {
  text: '真紅のハーフブロック',
  value: 'crimson_slab'
}, {
  text: '歪んだハーフブロック',
  value: 'warped_slab'
}, {
  text: '黒石のハーフブロック',
  value: 'blackstone_slab'
}, {
  text: '丸石のハーフブロック',
  value: 'cobblestone_slab'
}, {
  text: '模様入り赤い砂岩のハーフブロック',
  value: 'cut_red_sandstone_slab'
}, {
  text: '模様入り砂岩のハーフブロック',
  value: 'cut_sandstone_slab'
}, {
  text: 'エンドストーンレンガのハーフブロック',
  value: 'end_stone_brick_slab'
}, {
  text: '磨かれた黒石レンガのハーフブロック',
  value: 'polished_blackstone_brick_slab'
}, {
  text: '磨かれた黒石のハーフブロック',
  value: 'polished_blackstone_slab'
}, {
  text: '赤い砂岩のハーフブロック',
  value: 'red_sandstone_slab'
}, {
  text: '砂岩のハーフブロック',
  value: 'sandstone_slab'
}, {
  text: '滑らかな赤い砂岩のハーフブロック',
  value: 'smooth_red_sandstone_slab'
}, {
  text: '滑らかな砂岩のハーフブロック',
  value: 'smooth_sandstone_slab'
}, {
  text: '滑らかな石のハーフブロック',
  value: 'smooth_stone_slab'
}, {
  text: '石レンガのハーフブロック',
  value: 'stone_brick_slab'
}, {
  text: '石のハーフブロック',
  value: 'stone_slab'
}, {
  text: '安山岩のハーフブロック',
  value: 'andesite_slab'
}, {
  text: '磨かれた安山岩のハーフブロック',
  value: 'polished_andesite_slab'
}, {
  text: '閃緑岩のハーフブロック',
  value: 'diorite_slab'
}, {
  text: '磨かれた閃緑岩のハーフブロック',
  value: 'polished_diorite_slab'
}, {
  text: '花崗岩のハーフブロック',
  value: 'granite_slab'
}, {
  text: '磨かれた花崗岩のハーフブロック',
  value: 'polished_granite_slab'
}, {
  text: '丸石状ディープスレートのハーフブロック',
  value: 'cobbled_deepslate_slab'
}, {
  text: '磨かれたディープスレートのハーフブロック',
  value: 'polished_deepslate_slab'
}, {
  text: 'ディープスレートレンガのハーフブロック',
  value: 'deepslate_brick_slab'
}, {
  text: 'ディープスレートタイルのハーフブロック',
  value: 'deepslate_tile_slab'
}, {
  text: 'レンガのハーフブロック',
  value: 'brick_slab'
}, {
  text: 'ネザーレンガのハーフブロック',
  value: 'nether_brick_slab'
}, {
  text: 'プリズマリンレンガのハーフブロック',
  value: 'prismarine_brick_slab'
}, {
  text: 'ダークプリズマリンのハーフブロック',
  value: 'dark_prismarine_slab'
}, {
  text: 'プリズマリンのハーフブロック',
  value: 'prismarine_slab'
}, {
  text: 'クォーツのハーフブロック',
  value: 'quartz_slab'
}, {
  text: '滑らかなクォーツのハーフブロック',
  value: 'smooth_quartz_slab'
}, {
  text: 'プルプァのハーフブロック',
  value: 'purpur_slab'
}, {
  text: 'アカシアのスラブ（垂直）',
  value: 'vertical_acacia_slab'
}, {
  text: 'アメジストブロックのスラブ（垂直）',
  value: 'vertical_amethyst_block_slab'
}, {
  text: '安山岩のスラブ（垂直）',
  value: 'vertical_andesite_slab'
}, {
  text: 'シラカバのスラブ（垂直）',
  value: 'vertical_birch_slab'
}, {
  text: 'レンガのスラブ（垂直）',
  value: 'vertical_brick_slab'
}, {
  text: 'サクラのスラブ（垂直）',
  value: 'vertical_cherry_slab'
}, {
  text: '石炭ブロックのスラブ（垂直）',
  value: 'vertical_coal_block_slab'
}, {
  text: '丸石のスラブ（垂直）',
  value: 'vertical_cobblestone_slab'
}, {
  text: '銅ブロックのスラブ（垂直）',
  value: 'vertical_copper_block_slab'
}, {
  text: '真紅のスラブ（垂直）',
  value: 'vertical_crimson_slab'
}, {
  text: '切り込み入り銅のスラブ（垂直）',
  value: 'vertical_cut_copper_slab'
}, {
  text: 'ダークオークのスラブ（垂直）',
  value: 'vertical_dark_oak_slab'
}, {
  text: 'ダイヤブロックのスラブ（垂直）',
  value: 'vertical_diamond_block_slab'
}, {
  text: '閃緑岩のスラブ（垂直）',
  value: 'vertical_diorite_slab'
}, {
  text: 'エメラルドブロックのスラブ（垂直）',
  value: 'vertical_emerald_block_slab'
}, {
  text: '風化した切り込み入り銅のスラブ（垂直）',
  value: 'vertical_exposed_cut_copper_slab'
}, {
  text: '金ブロックのスラブ（垂直）',
  value: 'vertical_gold_block_slab'
}, {
  text: '花崗岩のスラブ（垂直）',
  value: 'vertical_granite_slab'
}, {
  text: '鉄ブロックのスラブ（垂直）',
  value: 'vertical_iron_block_slab'
}, {
  text: 'ジャングルのスラブ（垂直）',
  value: 'vertical_jungle_slab'
}, {
  text: 'ラピスラズリブロックのスラブ（垂直）',
  value: 'vertical_lapis_block_slab'
}, {
  text: 'マングローブのスラブ（垂直）',
  value: 'vertical_mangrove_slab'
}, {
  text: 'ネザライトブロックのスラブ（垂直）',
  value: 'vertical_netherite_block_slab'
}, {
  text: 'オークのスラブ（垂直）',
  value: 'vertical_oak_slab'
}, {
  text: '酸化した切り込み入り銅のスラブ（垂直）',
  value: 'vertical_oxidized_cut_copper_slab'
}, {
  text: 'クォーツのスラブ（垂直）',
  value: 'vertical_quartz_slab'
}, {
  text: 'レッドストーンブロックのスラブ（垂直）',
  value: 'vertical_redstone_block_slab'
}, {
  text: '砂岩のスラブ（垂直）',
  value: 'vertical_sandstone_slab'
}, {
  text: '滑らかな石のスラブ（垂直）',
  value: 'vertical_smooth_stone_slab'
}, {
  text: 'マツのスラブ（垂直）',
  value: 'vertical_spruce_slab'
}, {
  text: '石レンガのスラブ（垂直）',
  value: 'vertical_stone_brick_slab'
}, {
  text: '石のスラブ（垂直）',
  value: 'vertical_stone_slab'
}, {
  text: '歪んだスラブ（垂直）',
  value: 'vertical_warped_slab'
}, {
  text: '錆びた切り込み入り銅のスラブ（垂直）',
  value: 'vertical_weathered_cut_copper_slab'
}, {
  text: '丸石の塀',
  value: 'cobblestone_wall'
}, {
  text: '苔むした丸石の塀',
  value: 'mossy_cobblestone_wall'
}, {
  text: '安山岩の塀',
  value: 'andesite_wall'
}, {
  text: '閃緑岩の塀',
  value: 'diorite_wall'
}, {
  text: '花崗岩の塀',
  value: 'granite_wall'
}, {
  text: '丸石状ディープスレートの塀',
  value: 'cobbled_deepslate_wall'
}, {
  text: '磨かれたディープスレートの塀',
  value: 'polished_deepslate_wall'
}, {
  text: 'ディープスレートレンガの塀',
  value: 'deepslate_brick_wall'
}, {
  text: 'ディープスレートタイルの塀',
  value: 'deepslate_tile_wall'
}, {
  text: '黒石の塀',
  value: 'blackstone_wall'
}, {
  text: '磨かれた黒石レンガの塀',
  value: 'polished_blackstone_brick_wall'
}, {
  text: '磨かれた黒石の塀',
  value: 'polished_blackstone_wall'
}, {
  text: 'レンガの塀',
  value: 'brick_wall'
}, {
  text: 'エンドストーンレンガの塀',
  value: 'end_stone_brick_wall'
}, {
  text: '泥レンガの塀',
  value: 'mud_brick_wall'
}, {
  text: 'ネザーレンガの塀',
  value: 'nether_brick_wall'
}, {
  text: '赤いネザーレンガの塀',
  value: 'red_nether_brick_wall'
}, {
  text: '石レンガの塀',
  value: 'stone_brick_wall'
}, {
  text: '赤い砂岩の塀',
  value: 'red_sandstone_wall'
}, {
  text: '砂岩の塀',
  value: 'sandstone_wall'
}, {
  text: 'プリズマリンの塀',
  value: 'prismarine_wall'
}, {
  text: 'オークのフェンス',
  value: 'oak_fence'
}, {
  text: 'トウヒのフェンス',
  value: 'spruce_fence'
}, {
  text: 'シラカバのフェンス',
  value: 'birch_fence'
}, {
  text: 'ジャングルのフェンス',
  value: 'jungle_fence'
}, {
  text: 'アカシアのフェンス',
  value: 'acacia_fence'
}, {
  text: 'ダークオークのフェンス',
  value: 'dark_oak_fence'
}, {
  text: 'マングローブのフェンス',
  value: 'mangrove_fence'
}, {
  text: 'サクラのフェンス',
  value: 'cherry_fence'
}, {
  text: '竹のフェンス',
  value: 'bamboo_fence'
}, {
  text: '真紅のフェンス',
  value: 'crimson_fence'
}, {
  text: '歪んだフェンス',
  value: 'warped_fence'
}, {
  text: 'ネザーレンガのフェンス',
  value: 'nether_brick_fence'
}, {
  text: 'オークのフェンスゲート',
  value: 'oak_fence_gate'
}, {
  text: 'トウヒのフェンスゲート',
  value: 'spruce_fence_gate'
}, {
  text: 'シラカバのフェンスゲート',
  value: 'birch_fence_gate'
}, {
  text: 'ジャングルのフェンスゲート',
  value: 'jungle_fence_gate'
}, {
  text: 'アカシアのフェンスゲート',
  value: 'acacia_fence_gate'
}, {
  text: 'ダークオークのフェンスゲート',
  value: 'dark_oak_fence_gate'
}, {
  text: 'マングローブのフェンスゲート',
  value: 'mangrove_fence_gate'
}, {
  text: 'サクラのフェンスゲート',
  value: 'cherry_fence_gate'
}, {
  text: '竹のフェンスゲート',
  value: 'bamboo_fence_gate'
}, {
  text: '真紅のフェンスゲート',
  value: 'crimson_fence_gate'
}, {
  text: '歪んだフェンスゲート',
  value: 'warped_fence_gate'
}, {
  text: 'トウヒのボタン',
  value: 'spruce_button'
}, {
  text: 'シラカバのボタン',
  value: 'birch_button'
}, {
  text: 'ジャングルのボタン',
  value: 'jungle_button'
}, {
  text: 'アカシアのボタン',
  value: 'acacia_button'
}, {
  text: 'ダークオークのボタン',
  value: 'dark_oak_button'
}, {
  text: 'マングローブのボタン',
  value: 'mangrove_button'
}, {
  text: 'サクラのボタン',
  value: 'cherry_button'
}, {
  text: '竹のボタン',
  value: 'bamboo_button'
}, {
  text: '真紅のボタン',
  value: 'crimson_button'
}, {
  text: '歪んだボタン',
  value: 'warped_button'
}, {
  text: 'トウヒの感圧板',
  value: 'spruce_pressure_plate'
}, {
  text: 'シラカバの感圧板',
  value: 'birch_pressure_plate'
}, {
  text: 'ジャングルの感圧板',
  value: 'jungle_pressure_plate'
}, {
  text: 'アカシアの感圧板',
  value: 'acacia_pressure_plate'
}, {
  text: 'ダークオークの感圧板',
  value: 'dark_oak_pressure_plate'
}, {
  text: 'マングローブの感圧板',
  value: 'mangrove_pressure_plate'
}, {
  text: 'サクラの感圧板',
  value: 'cherry_pressure_plate'
}, {
  text: '竹の感圧板',
  value: 'bamboo_pressure_plate'
}, {
  text: '真紅の感圧板',
  value: 'crimson_pressure_plate'
}, {
  text: '歪んだ感圧板',
  value: 'warped_pressure_plate'
}, {
  text: 'トウヒのドア',
  value: 'spruce_door'
}, {
  text: 'シラカバのドア',
  value: 'birch_door'
}, {
  text: 'ジャングルのドア',
  value: 'jungle_door'
}, {
  text: 'アカシアのドア',
  value: 'acacia_door'
}, {
  text: 'ダークオークのドア',
  value: 'dark_oak_door'
}, {
  text: 'マングローブのドア',
  value: 'mangrove_door'
}, {
  text: 'サクラのドア',
  value: 'cherry_door'
}, {
  text: '竹のドア',
  value: 'bamboo_door'
}, {
  text: '真紅のドア',
  value: 'crimson_door'
}, {
  text: '歪んだドア',
  value: 'warped_door'
}, {
  text: 'トウヒのトラップドア',
  value: 'spruce_trapdoor'
}, {
  text: 'シラカバのトラップドア',
  value: 'birch_trapdoor'
}, {
  text: 'ジャングルのトラップドア',
  value: 'jungle_trapdoor'
}, {
  text: 'アカシアのトラップドア',
  value: 'acacia_trapdoor'
}, {
  text: 'ダークオークのトラップドア',
  value: 'dark_oak_trapdoor'
}, {
  text: 'マングローブのトラップドア',
  value: 'mangrove_trapdoor'
}, {
  text: 'サクラのトラップドア',
  value: 'cherry_trapdoor'
}, {
  text: '竹のトラップドア',
  value: 'bamboo_trapdoor'
}, {
  text: '真紅のトラップドア',
  value: 'crimson_trapdoor'
}, {
  text: '歪んだトラップドア',
  value: 'warped_trapdoor'
}, {
  text: '黒色のカーペット',
  value: 'black_carpet'
}, {
  text: '黒色のコンクリートパウダー',
  value: 'black_concrete_powder'
}, {
  text: '黒色の彩釉テラコッタ',
  value: 'black_glazed_terracotta'
}, {
  text: '黒色のシュルカーボックス',
  value: 'black_shulker_box'
}, {
  text: '黒色の色付きガラス板',
  value: 'black_stained_glass_pane'
}, {
  text: '青色のカーペット',
  value: 'blue_carpet'
}, {
  text: '青色のコンクリートパウダー',
  value: 'blue_concrete_powder'
}, {
  text: '青色の色付きガラス板',
  value: 'blue_stained_glass_pane'
}, {
  text: '骨ブロック',
  value: 'bone_block'
}, {
  text: '茶色のベッド',
  value: 'brown_bed'
}, {
  text: '茶色のカーペット',
  value: 'brown_carpet'
}, {
  text: '茶色のコンクリートパウダー',
  value: 'brown_concrete_powder'
}, {
  text: '茶色の彩釉テラコッタ',
  value: 'brown_glazed_terracotta'
}, {
  text: '茶色のシュルカーボックス',
  value: 'brown_shulker_box'
}, {
  text: '茶色の色付きガラス板',
  value: 'brown_stained_glass_pane'
}, {
  text: '鎖',
  value: 'chain'
}, {
  text: '青緑色のベッド',
  value: 'cyan_bed'
}, {
  text: '青緑色のカーペット',
  value: 'cyan_carpet'
}, {
  text: '青緑色のコンクリートパウダー',
  value: 'cyan_concrete_powder'
}, {
  text: '青緑色の彩釉テラコッタ',
  value: 'cyan_glazed_terracotta'
}, {
  text: '青緑色のシュルカーボックス',
  value: 'cyan_shulker_box'
}, {
  text: '青緑色の色付きガラス板',
  value: 'cyan_stained_glass_pane'
}, {
  text: '草の道',
  value: 'dirt_path'
}, {
  text: 'ガラス板',
  value: 'glass_pane'
}, {
  text: '灰色のベッド',
  value: 'gray_bed'
}, {
  text: '灰色のコンクリートパウダー',
  value: 'gray_concrete_powder'
}, {
  text: '灰色の彩釉テラコッタ',
  value: 'gray_glazed_terracotta'
}, {
  text: '灰色のシュルカーボックス',
  value: 'gray_shulker_box'
}, {
  text: '灰色の色付きガラス板',
  value: 'gray_stained_glass_pane'
}, {
  text: '緑色のカーペット',
  value: 'green_carpet'
}, {
  text: '緑色のコンクリートパウダー',
  value: 'green_concrete_powder'
}, {
  text: '緑色の色付きガラス板',
  value: 'green_stained_glass_pane'
}, {
  text: '鉄格子',
  value: 'iron_bars'
}, {
  text: 'はしご',
  value: 'ladder'
}, {
  text: '空色のベッド',
  value: 'light_blue_bed'
}, {
  text: '空色のコンクリートパウダー',
  value: 'light_blue_concrete_powder'
}, {
  text: '空色の彩釉テラコッタ',
  value: 'light_blue_glazed_terracotta'
}, {
  text: '空色のシュルカーボックス',
  value: 'light_blue_shulker_box'
}, {
  text: '空色の色付きガラス板',
  value: 'light_blue_stained_glass_pane'
}, {
  text: '薄灰色のベッド',
  value: 'light_gray_bed'
}, {
  text: '薄灰色のカーペット',
  value: 'light_gray_carpet'
}, {
  text: '薄灰色のコンクリートパウダー',
  value: 'light_gray_concrete_powder'
}, {
  text: '薄灰色の彩釉テラコッタ',
  value: 'light_gray_glazed_terracotta'
}, {
  text: '薄灰色のシュルカーボックス',
  value: 'light_gray_shulker_box'
}, {
  text: '薄灰色の色付きガラス板',
  value: 'light_gray_stained_glass_pane'
}, {
  text: '黄緑色のベッド',
  value: 'lime_bed'
}, {
  text: '黄緑色のコンクリートパウダー',
  value: 'lime_concrete_powder'
}, {
  text: '黄緑色の彩釉テラコッタ',
  value: 'lime_glazed_terracotta'
}, {
  text: '黄緑色のシュルカーボックス',
  value: 'lime_shulker_box'
}, {
  text: '黄緑色の色付きガラス板',
  value: 'lime_stained_glass_pane'
}, {
  text: '赤紫色のベッド',
  value: 'magenta_bed'
}, {
  text: '赤紫色のコンクリートパウダー',
  value: 'magenta_concrete_powder'
}, {
  text: '赤紫色の彩釉テラコッタ',
  value: 'magenta_glazed_terracotta'
}, {
  text: '赤紫色のシュルカーボックス',
  value: 'magenta_shulker_box'
}, {
  text: '赤紫色の色付きガラス板',
  value: 'magenta_stained_glass_pane'
}, {
  text: 'マングローブの根',
  value: 'mangrove_roots'
}, {
  text: '苔むした丸石',
  value: 'mossy_cobblestone'
}, {
  text: '泥だらけのマングローブの根',
  value: 'muddy_mangrove_roots'
}, {
  text: '橙色のベッド',
  value: 'orange_bed'
}, {
  text: '橙色のコンクリートパウダー',
  value: 'orange_concrete_powder'
}, {
  text: '橙色のシュルカーボックス',
  value: 'orange_shulker_box'
}, {
  text: '橙色の色付きガラス板',
  value: 'orange_stained_glass_pane'
}, {
  text: '固めた泥',
  value: 'packed_mud'
}, {
  text: '桃色のベッド',
  value: 'pink_bed'
}, {
  text: '桃色のコンクリートパウダー',
  value: 'pink_concrete_powder'
}, {
  text: '桃色の彩釉テラコッタ',
  value: 'pink_glazed_terracotta'
}, {
  text: '桃色のシュルカーボックス',
  value: 'pink_shulker_box'
}, {
  text: '桃色の色付きガラス板',
  value: 'pink_stained_glass_pane'
}, {
  text: '粉雪',
  value: 'powder_snow'
}, {
  text: '紫色のベッド',
  value: 'purple_bed'
}, {
  text: '紫色のカーペット',
  value: 'purple_carpet'
}, {
  text: '紫色のコンクリートパウダー',
  value: 'purple_concrete_powder'
}, {
  text: '紫色の彩釉テラコッタ',
  value: 'purple_glazed_terracotta'
}, {
  text: '紫色のシュルカーボックス',
  value: 'purple_shulker_box'
}, {
  text: '紫色の色付きガラス板',
  value: 'purple_stained_glass_pane'
}, {
  text: '赤色のカーペット',
  value: 'red_carpet'
}, {
  text: '赤色のコンクリートパウダー',
  value: 'red_concrete_powder'
}, {
  text: '赤色の彩釉テラコッタ',
  value: 'red_glazed_terracotta'
}, {
  text: '赤色の色付きガラス板',
  value: 'red_stained_glass_pane'
}, {
  text: '根付いた土',
  value: 'rooted_dirt'
}, {
  text: '樹皮を剥いだ竹ブロック',
  value: 'stripped_bamboo_block'
}, {
  text: '遮光ガラス',
  value: 'tinted_glass'
}, {
  text: '白色のコンクリートパウダー',
  value: 'white_concrete_powder'
}, {
  text: '白色の色付きガラス板',
  value: 'white_stained_glass_pane'
}, {
  text: '黄色のコンクリートパウダー',
  value: 'yellow_concrete_powder'
}, {
  text: '黄色の彩釉テラコッタ',
  value: 'yellow_glazed_terracotta'
}, {
  text: '黄色のシュルカーボックス',
  value: 'yellow_shulker_box'
}, {
  text: '黄色の色付きガラス板',
  value: 'yellow_stained_glass_pane'
}];
const LIGHTING_BLOCKS = [{
  text: '松明',
  value: 'torch'
}, {
  text: 'ソウルトーチ',
  value: 'soul_torch'
}, {
  text: 'ランタン',
  value: 'lantern'
}, {
  text: 'ソウルランタン',
  value: 'soul_lantern'
}, {
  text: 'グロウストーン',
  value: 'glowstone'
}, {
  text: 'シーランタン',
  value: 'sea_lantern'
}, {
  text: 'シュルームライト',
  value: 'shroomlight'
}, {
  text: 'ジャック・オ・ランタン',
  value: 'jack_o_lantern'
}, {
  text: 'ろうそく',
  value: 'candle'
}, {
  text: '白色のろうそく',
  value: 'white_candle'
}, {
  text: '橙色のろうそく',
  value: 'orange_candle'
}, {
  text: '赤紫色のろうそく',
  value: 'magenta_candle'
}, {
  text: '空色のろうそく',
  value: 'light_blue_candle'
}, {
  text: '黄色のろうそく',
  value: 'yellow_candle'
}, {
  text: '黄緑色のろうそく',
  value: 'lime_candle'
}, {
  text: '桃色のろうそく',
  value: 'pink_candle'
}, {
  text: '灰色のろうそく',
  value: 'gray_candle'
}, {
  text: '薄灰色のろうそく',
  value: 'light_gray_candle'
}, {
  text: '青緑色のろうそく',
  value: 'cyan_candle'
}, {
  text: '紫色のろうそく',
  value: 'purple_candle'
}, {
  text: '青色のろうそく',
  value: 'blue_candle'
}, {
  text: '茶色のろうそく',
  value: 'brown_candle'
}, {
  text: '緑色のろうそく',
  value: 'green_candle'
}, {
  text: '赤色のろうそく',
  value: 'red_candle'
}, {
  text: '黒色のろうそく',
  value: 'black_candle'
}, {
  text: '焚き火',
  value: 'campfire'
}, {
  text: 'ソウル焚き火',
  value: 'soul_campfire'
}, {
  text: 'レッドストーンランプ',
  value: 'redstone_lamp'
}, {
  text: 'レッドストーントーチ',
  value: 'redstone_torch'
}, {
  text: 'ソウルファイア',
  value: 'soul_fire'
}];
const DECORATION_BLOCKS = [{
  text: '白色の羊毛',
  value: 'white_wool'
}, {
  text: '橙色の羊毛',
  value: 'orange_wool'
}, {
  text: '赤紫色の羊毛',
  value: 'magenta_wool'
}, {
  text: '空色の羊毛',
  value: 'light_blue_wool'
}, {
  text: '黄色の羊毛',
  value: 'yellow_wool'
}, {
  text: '黄緑色の羊毛',
  value: 'lime_wool'
}, {
  text: '桃色の羊毛',
  value: 'pink_wool'
}, {
  text: '灰色の羊毛',
  value: 'gray_wool'
}, {
  text: '薄灰色の羊毛',
  value: 'light_gray_wool'
}, {
  text: '青緑色の羊毛',
  value: 'cyan_wool'
}, {
  text: '紫色の羊毛',
  value: 'purple_wool'
}, {
  text: '青色の羊毛',
  value: 'blue_wool'
}, {
  text: '茶色の羊毛',
  value: 'brown_wool'
}, {
  text: '緑色の羊毛',
  value: 'green_wool'
}, {
  text: '赤色の羊毛',
  value: 'red_wool'
}, {
  text: '黒色の羊毛',
  value: 'black_wool'
}, {
  text: '苔のカーペット',
  value: 'moss_carpet'
}, {
  text: '白色のカーペット',
  value: 'white_carpet'
}, {
  text: '橙色のカーペット',
  value: 'orange_carpet'
}, {
  text: '赤紫色のカーペット',
  value: 'magenta_carpet'
}, {
  text: '空色のカーペット',
  value: 'light_blue_carpet'
}, {
  text: '黄色のカーペット',
  value: 'yellow_carpet'
}, {
  text: '黄緑色のカーペット',
  value: 'lime_carpet'
}, {
  text: '桃色のカーペット',
  value: 'pink_carpet'
}, {
  text: '灰色のカーペット',
  value: 'gray_carpet'
}, {
  text: '白色のテラコッタ',
  value: 'white_terracotta'
}, {
  text: '橙色のテラコッタ',
  value: 'orange_terracotta'
}, {
  text: '赤紫色のテラコッタ',
  value: 'magenta_terracotta'
}, {
  text: '空色のテラコッタ',
  value: 'light_blue_terracotta'
}, {
  text: '黄色のテラコッタ',
  value: 'yellow_terracotta'
}, {
  text: '黄緑色のテラコッタ',
  value: 'lime_terracotta'
}, {
  text: '桃色のテラコッタ',
  value: 'pink_terracotta'
}, {
  text: '灰色のテラコッタ',
  value: 'gray_terracotta'
}, {
  text: '薄灰色のテラコッタ',
  value: 'light_gray_terracotta'
}, {
  text: '青緑色のテラコッタ',
  value: 'cyan_terracotta'
}, {
  text: '紫色のテラコッタ',
  value: 'purple_terracotta'
}, {
  text: '青色のテラコッタ',
  value: 'blue_terracotta'
}, {
  text: '茶色のテラコッタ',
  value: 'brown_terracotta'
}, {
  text: '緑色のテラコッタ',
  value: 'green_terracotta'
}, {
  text: '赤色のテラコッタ',
  value: 'red_terracotta'
}, {
  text: '黒色のテラコッタ',
  value: 'black_terracotta'
}, {
  text: '白色の彩釉テラコッタ',
  value: 'white_glazed_terracotta'
}, {
  text: '橙色の彩釉テラコッタ',
  value: 'orange_glazed_terracotta'
}, {
  text: '青色の彩釉テラコッタ',
  value: 'blue_glazed_terracotta'
}, {
  text: '緑色の彩釉テラコッタ',
  value: 'green_glazed_terracotta'
}, {
  text: 'ガラス',
  value: 'glass'
}, {
  text: '白色の色付きガラス',
  value: 'white_stained_glass'
}, {
  text: '橙色の色付きガラス',
  value: 'orange_stained_glass'
}, {
  text: '赤紫色の色付きガラス',
  value: 'magenta_stained_glass'
}, {
  text: '空色の色付きガラス',
  value: 'light_blue_stained_glass'
}, {
  text: '黄色の色付きガラス',
  value: 'yellow_stained_glass'
}, {
  text: '黄緑色の色付きガラス',
  value: 'lime_stained_glass'
}, {
  text: '桃色の色付きガラス',
  value: 'pink_stained_glass'
}, {
  text: '灰色の色付きガラス',
  value: 'gray_stained_glass'
}, {
  text: '薄灰色の色付きガラス',
  value: 'light_gray_stained_glass'
}, {
  text: '青緑色の色付きガラス',
  value: 'cyan_stained_glass'
}, {
  text: '紫色の色付きガラス',
  value: 'purple_stained_glass'
}, {
  text: '青色の色付きガラス',
  value: 'blue_stained_glass'
}, {
  text: '茶色の色付きガラス',
  value: 'brown_stained_glass'
}, {
  text: '緑色の色付きガラス',
  value: 'green_stained_glass'
}, {
  text: '赤色の色付きガラス',
  value: 'red_stained_glass'
}, {
  text: '黒色の色付きガラス',
  value: 'black_stained_glass'
}, {
  text: '白色のベッド',
  value: 'white_bed'
}, {
  text: '黄色のベッド',
  value: 'yellow_bed'
}, {
  text: '青色のベッド',
  value: 'blue_bed'
}, {
  text: '緑色のベッド',
  value: 'green_bed'
}, {
  text: '赤色のベッド',
  value: 'red_bed'
}, {
  text: '黒色のベッド',
  value: 'black_bed'
}, {
  text: 'シュルカーボックス',
  value: 'shulker_box'
}, {
  text: '白色のシュルカーボックス',
  value: 'white_shulker_box'
}, {
  text: '青色のシュルカーボックス',
  value: 'blue_shulker_box'
}, {
  text: '緑色のシュルカーボックス',
  value: 'green_shulker_box'
}, {
  text: '赤色のシュルカーボックス',
  value: 'red_shulker_box'
}, {
  text: '本棚',
  value: 'bookshelf'
}, {
  text: '芽生えたアメジスト',
  value: 'budding_amethyst'
}, {
  text: 'ドリップストーンブロック',
  value: 'dripstone_block'
}, {
  text: '植木鉢',
  value: 'flower_pot'
}, {
  text: '額縁',
  value: 'item_frame'
}, {
  text: '苔ブロック',
  value: 'moss_block'
}, {
  text: '絵画',
  value: 'painting'
}, {
  text: '滑らかな玄武岩',
  value: 'smooth_basalt'
}];
const NATURE_BLOCKS = [{
  text: '土',
  value: 'dirt'
}, {
  text: '草ブロック',
  value: 'grass_block'
}, {
  text: '粗い土',
  value: 'coarse_dirt'
}, {
  text: 'ポドゾル',
  value: 'podzol'
}, {
  text: '菌糸',
  value: 'mycelium'
}, {
  text: '耕地',
  value: 'farmland'
}, {
  text: '泥',
  value: 'mud'
}, {
  text: '砂',
  value: 'sand'
}, {
  text: '赤い砂',
  value: 'red_sand'
}, {
  text: '砂利',
  value: 'gravel'
}, {
  text: 'ソウルサンド',
  value: 'soul_sand'
}, {
  text: 'ソウルソイル',
  value: 'soul_soil'
}, {
  text: 'オークの原木',
  value: 'oak_log'
}, {
  text: 'トウヒの原木',
  value: 'spruce_log'
}, {
  text: 'シラカバの原木',
  value: 'birch_log'
}, {
  text: 'ジャングルの原木',
  value: 'jungle_log'
}, {
  text: 'アカシアの原木',
  value: 'acacia_log'
}, {
  text: 'ダークオークの原木',
  value: 'dark_oak_log'
}, {
  text: 'マングローブの原木',
  value: 'mangrove_log'
}, {
  text: 'サクラの原木',
  value: 'cherry_log'
}, {
  text: '真紅の幹',
  value: 'crimson_stem'
}, {
  text: '歪んだ幹',
  value: 'warped_stem'
}, {
  text: 'キノコの柄',
  value: 'mushroom_stem'
}, {
  text: 'オークの葉',
  value: 'oak_leaves'
}, {
  text: 'トウヒの葉',
  value: 'spruce_leaves'
}, {
  text: 'シラカバの葉',
  value: 'birch_leaves'
}, {
  text: 'ジャングルの葉',
  value: 'jungle_leaves'
}, {
  text: 'アカシアの葉',
  value: 'acacia_leaves'
}, {
  text: 'ダークオークの葉',
  value: 'dark_oak_leaves'
}, {
  text: 'マングローブの葉',
  value: 'mangrove_leaves'
}, {
  text: 'サクラの葉',
  value: 'cherry_leaves'
}, {
  text: 'ツツジの葉',
  value: 'azalea_leaves'
}, {
  text: '開花したツツジの葉',
  value: 'flowering_azalea_leaves'
}, {
  text: 'ポピー',
  value: 'poppy'
}, {
  text: 'タンポポ',
  value: 'dandelion'
}, {
  text: 'ヒスイラン',
  value: 'blue_orchid'
}, {
  text: 'アリウム',
  value: 'allium'
}, {
  text: 'フランスギク',
  value: 'oxeye_daisy'
}, {
  text: 'ヤグルマギク',
  value: 'cornflower'
}, {
  text: 'スズラン',
  value: 'lily_of_the_valley'
}, {
  text: 'ライラック',
  value: 'lilac'
}, {
  text: 'バラの低木',
  value: 'rose_bush'
}, {
  text: 'ボタン',
  value: 'peony'
}, {
  text: 'ヒマワリ',
  value: 'sunflower'
}, {
  text: '氷',
  value: 'ice'
}, {
  text: '氷塊',
  value: 'packed_ice'
}, {
  text: '雪',
  value: 'snow'
}, {
  text: '雪ブロック',
  value: 'snow_block'
}, {
  text: '水',
  value: 'water'
}, {
  text: '竹',
  value: 'bamboo'
}, {
  text: '竹ブロック',
  value: 'bamboo_block'
}, {
  text: 'サボテン',
  value: 'cactus'
}, {
  text: 'サトウキビ',
  value: 'sugar_cane'
}, {
  text: '昆布',
  value: 'kelp'
}, {
  text: '海草',
  value: 'seagrass'
}, {
  text: '草',
  value: 'grass'
}, {
  text: 'シダ',
  value: 'fern'
}, {
  text: '枯れ木',
  value: 'dead_bush'
}, {
  text: 'つる',
  value: 'vine'
}, {
  text: 'ねじれつる',
  value: 'twisting_vines'
}, {
  text: 'しだれツタ',
  value: 'weeping_vines'
}, {
  text: 'グロウベリー',
  value: 'glow_berries'
}, {
  text: 'ツツジの葉',
  value: 'azalea'
}, {
  text: '脳サンゴブロック',
  value: 'brain_coral_block'
}, {
  text: 'キノコブロック(茶)',
  value: 'brown_mushroom_block'
}, {
  text: '泡サンゴブロック',
  value: 'bubble_coral_block'
}, {
  text: '粘土',
  value: 'clay'
}, {
  text: '火サンゴブロック',
  value: 'fire_coral_block'
}, {
  text: '開花したツツジ',
  value: 'flowering_azalea'
}, {
  text: '干草の俵',
  value: 'hay_block'
}, {
  text: '角サンゴブロック',
  value: 'horn_coral_block'
}, {
  text: 'スイカ',
  value: 'melon'
}, {
  text: 'ネザーウォート',
  value: 'nether_wart_block'
}, {
  text: 'プリズマリン',
  value: 'prismarine'
}, {
  text: 'カボチャ',
  value: 'pumpkin'
}, {
  text: 'キノコブロック(赤)',
  value: 'red_mushroom_block'
}, {
  text: 'サンゴブロック',
  value: 'tube_coral_block'
}, {
  text: '歪んだウォート',
  value: 'warped_wart_block'
}];
const FUNCTIONAL_BLOCKS = [{
  text: '作業台',
  value: 'crafting_table'
}, {
  text: 'かまど',
  value: 'furnace'
}, {
  text: '溶鉱炉',
  value: 'blast_furnace'
}, {
  text: '燻製器',
  value: 'smoker'
}, {
  text: '金床',
  value: 'anvil'
}, {
  text: '砥石',
  value: 'grindstone'
}, {
  text: '石切台',
  value: 'stonecutter'
}, {
  text: '鍛冶台',
  value: 'smithing_table'
}, {
  text: '製図台',
  value: 'cartography_table'
}, {
  text: '矢細工台',
  value: 'fletching_table'
}, {
  text: '機織り機',
  value: 'loom'
}, {
  text: '醸造台',
  value: 'brewing_stand'
}, {
  text: 'エンチャントテーブル',
  value: 'enchanting_table'
}, {
  text: 'チェスト',
  value: 'chest'
}, {
  text: '樽',
  value: 'barrel'
}, {
  text: 'ホッパー',
  value: 'hopper'
}, {
  text: '鉄のドア',
  value: 'iron_door'
}, {
  text: 'オークのドア',
  value: 'oak_door'
}, {
  text: '鉄のトラップドア',
  value: 'iron_trapdoor'
}, {
  text: 'オークのトラップドア',
  value: 'oak_trapdoor'
}, {
  text: 'レッドストーンコンパレーター',
  value: 'comparator'
}, {
  text: 'レッドストーンリピーター',
  value: 'repeater'
}, {
  text: 'オブザーバー',
  value: 'observer'
}, {
  text: 'レバー',
  value: 'lever'
}, {
  text: '的',
  value: 'target'
}, {
  text: 'ディスペンサー',
  value: 'dispenser'
}, {
  text: 'ドロッパー',
  value: 'dropper'
}, {
  text: 'ピストン',
  value: 'piston'
}, {
  text: '粘着ピストン',
  value: 'sticky_piston'
}, {
  text: 'レール',
  value: 'rail'
}, {
  text: 'パワードレール',
  value: 'powered_rail'
}, {
  text: 'ディテクターレール',
  value: 'detector_rail'
}, {
  text: 'アクティベーターレール',
  value: 'activator_rail'
}, {
  text: 'オークのボタン',
  value: 'oak_button'
}, {
  text: '石のボタン',
  value: 'stone_button'
}, {
  text: 'オークの感圧板',
  value: 'oak_pressure_plate'
}, {
  text: '石の感圧板',
  value: 'stone_pressure_plate'
}, {
  text: 'ビーコン',
  value: 'beacon'
}, {
  text: 'ベル',
  value: 'bell'
}, {
  text: '大釜',
  value: 'cauldron'
}, {
  text: 'コンポスター',
  value: 'composter'
}, {
  text: 'コンジット',
  value: 'conduit'
}, {
  text: 'かまど付きトロッコ',
  value: 'furnace_minecart'
}, {
  text: '重量用感圧板（重）',
  value: 'heavy_weighted_pressure_plate'
}, {
  text: 'ハニーブロック',
  value: 'honey_block'
}, {
  text: 'ジュークボックス',
  value: 'jukebox'
}, {
  text: '書見台',
  value: 'lectern'
}, {
  text: '重量用感圧板（軽）',
  value: 'light_weighted_pressure_plate'
}, {
  text: '音符ブロック',
  value: 'note_block'
}, {
  text: 'リスポーンアンカー',
  value: 'respawn_anchor'
}, {
  text: 'スライムブロック',
  value: 'slime_block'
}, {
  text: 'TNT',
  value: 'tnt'
}];
const ORE_BLOCKS = [{
  text: '石炭鉱石',
  value: 'coal_ore'
}, {
  text: '鉄鉱石',
  value: 'iron_ore'
}, {
  text: '銅鉱石',
  value: 'copper_ore'
}, {
  text: '金鉱石',
  value: 'gold_ore'
}, {
  text: 'ラピスラズリ鉱石',
  value: 'lapis_ore'
}, {
  text: 'レッドストーン鉱石',
  value: 'redstone_ore'
}, {
  text: 'ダイヤモンド鉱石',
  value: 'diamond_ore'
}, {
  text: 'エメラルド鉱石',
  value: 'emerald_ore'
}, {
  text: 'ディープスレート石炭鉱石',
  value: 'deepslate_coal_ore'
}, {
  text: 'ディープスレート鉄鉱石',
  value: 'deepslate_iron_ore'
}, {
  text: 'ディープスレート銅鉱石',
  value: 'deepslate_copper_ore'
}, {
  text: 'ディープスレート金鉱石',
  value: 'deepslate_gold_ore'
}, {
  text: 'ディープスレートラピスラズリ鉱石',
  value: 'deepslate_lapis_ore'
}, {
  text: 'ディープスレートレッドストーン鉱石',
  value: 'deepslate_redstone_ore'
}, {
  text: 'ディープスレートダイヤモンド鉱石',
  value: 'deepslate_diamond_ore'
}, {
  text: 'ディープスレートエメラルド鉱石',
  value: 'deepslate_emerald_ore'
}, {
  text: 'ネザー金鉱石',
  value: 'nether_gold_ore'
}, {
  text: 'ネザークォーツ鉱石',
  value: 'nether_quartz_ore'
}, {
  text: '石炭ブロック',
  value: 'coal_block'
}, {
  text: '鉄ブロック',
  value: 'iron_block'
}, {
  text: '銅ブロック',
  value: 'copper_block'
}, {
  text: '金ブロック',
  value: 'gold_block'
}, {
  text: 'ラピスラズリブロック',
  value: 'lapis_block'
}, {
  text: 'レッドストーンブロック',
  value: 'redstone_block'
}, {
  text: 'ダイヤモンドブロック',
  value: 'diamond_block'
}, {
  text: 'エメラルドブロック',
  value: 'emerald_block'
}, {
  text: 'クォーツブロック',
  value: 'quartz_block'
}, {
  text: 'アメジストブロック',
  value: 'amethyst_block'
}, {
  text: 'ネザライトブロック',
  value: 'netherite_block'
}, {
  text: '鉄の原石',
  value: 'raw_iron_block'
}, {
  text: '銅の原石',
  value: 'raw_copper_block'
}, {
  text: '金の原石',
  value: 'raw_gold_block'
}, {
  text: '古代の残骸',
  value: 'ancient_debris'
}, {
  text: '切り込み入りの銅',
  value: 'cut_copper'
}, {
  text: '錆びた銅',
  value: 'oxidized_copper'
}, {
  text: 'ワックスを塗った銅',
  value: 'waxed_copper_block'
}, {
  text: 'ワックスを塗った切り込み入りの銅',
  value: 'waxed_cut_copper'
}, {
  text: '風化した銅',
  value: 'weathered_copper'
}];
const SPECIAL_BLOCKS = [{
  text: '空気（空）',
  value: 'air'
}, {
  text: 'バリアブロック',
  value: 'barrier'
}, {
  text: 'ストラクチャーヴォイド',
  value: 'structure_void'
}, {
  text: '透明ブロック',
  value: 'light'
}, {
  text: '岩盤',
  value: 'bedrock'
}, {
  text: '黒曜石',
  value: 'obsidian'
}, {
  text: '泣く黒曜石',
  value: 'crying_obsidian'
}, {
  text: 'スポンジ',
  value: 'sponge'
}, {
  text: '濡れたスポンジ',
  value: 'wet_sponge'
}, {
  text: '溶岩',
  value: 'lava'
}, {
  text: 'エンドポータル',
  value: 'end_portal'
}, {
  text: 'エンドポータルフレーム',
  value: 'end_portal_frame'
}, {
  text: 'エンドゲートウェイ',
  value: 'end_gateway'
}, {
  text: 'エンドロッド',
  value: 'end_rod'
}, {
  text: 'エンドストーン',
  value: 'end_stone'
}, {
  text: 'ネザーラック',
  value: 'netherrack'
}, {
  text: 'マグマブロック',
  value: 'magma_block'
}, {
  text: 'スカルクブロック',
  value: 'sculk'
}, {
  text: 'スカルクヴェイン',
  value: 'sculk_vein'
}, {
  text: 'スカルクセンサー',
  value: 'sculk_sensor'
}, {
  text: 'スカルクカタリスト',
  value: 'sculk_catalyst'
}, {
  text: 'スカルクシュリーカー',
  value: 'sculk_shrieker'
}, {
  text: 'ブルーアイス',
  value: 'blue_ice'
}, {
  text: 'コマンドブロック',
  value: 'command_block'
}, {
  text: 'デバッグ棒',
  value: 'debug_stick'
}, {
  text: 'ドラゴンの卵',
  value: 'dragon_egg'
}, {
  text: 'ジグソーブロック',
  value: 'jigsaw'
}, {
  text: '移動するピストン',
  value: 'moving_piston'
}, {
  text: '強化ディープスレート',
  value: 'reinforced_deepslate'
}, {
  text: 'スポナー',
  value: 'spawner'
}, {
  text: 'ストラクチャーブロック',
  value: 'structure_block'
}];

// エンティティカテゴリのデータ定義（モジュールレベル定数）

module.exports = Scratch3MinecraftBlocks;
