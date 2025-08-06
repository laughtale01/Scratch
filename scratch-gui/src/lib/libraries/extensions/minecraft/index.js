 /**
   * Minecraft Remote Controller Extension for Scratch 3.0
   * Compatible with Minecraft 1.20.1 + Forge
   */

  // 定数定義
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

  class MinecraftExtension {
      constructor(runtime) {
          this.runtime = runtime;
          this.websocket = null;
          this.connectionStatus = 'disconnected';
          this.playerPos = {x: 0, y: 0, z: 0};
          this.lastBlockInfo = '';
          this.invitationCount = 0;
          this.currentWorld = 'my_world';
      }

      getInfo() {
          return {
              id: 'minecraft',
              name: '🎮 Minecraft Controller',
              color1: '#4CAF50',
              color2: '#388E3C',
              menus: {
                  blockTypes: {
                      acceptReporters: true,
                      items: [
                          // 基本ブロック
                          {text: '石', value: 'stone'},
                          {text: '土', value: 'dirt'},
                          {text: '草ブロック', value: 'grass_block'},
                          {text: '丸石', value: 'cobblestone'},
                          {text: '岩盤', value: 'bedrock'},
                          {text: '砂', value: 'sand'},
                          {text: '砂利', value: 'gravel'},
                          {text: '空気', value: 'air'},
                          
                          // 木材系
                          {text: 'オークの板材', value: 'oak_planks'},
                          {text: 'オークの原木', value: 'oak_log'},
                          {text: 'シラカバの板材', value: 'birch_planks'},
                          {text: 'シラカバの原木', value: 'birch_log'},
                          {text: 'トウヒの板材', value: 'spruce_planks'},
                          {text: 'トウヒの原木', value: 'spruce_log'},
                          {text: 'ジャングルの板材', value: 'jungle_planks'},
                          {text: 'ジャングルの原木', value: 'jungle_log'},
                          {text: 'アカシアの板材', value: 'acacia_planks'},
                          {text: 'アカシアの原木', value: 'acacia_log'},
                          {text: 'ダークオークの板材', value: 'dark_oak_planks'},
                          {text: 'ダークオークの原木', value: 'dark_oak_log'},
                          
                          // 石系
                          {text: '花崗岩', value: 'granite'},
                          {text: '閃緑岩', value: 'diorite'},
                          {text: '安山岩', value: 'andesite'},
                          {text: '深層岩', value: 'deepslate'},
                          {text: '方解石', value: 'calcite'},
                          {text: '凝灰岩', value: 'tuff'},
                          {text: '砂岩', value: 'sandstone'},
                          {text: '赤い砂岩', value: 'red_sandstone'},
                          
                          // レンガ・建築系
                          {text: 'レンガ', value: 'bricks'},
                          {text: '石レンガ', value: 'stone_bricks'},
                          {text: '苔むした石レンガ', value: 'mossy_stone_bricks'},
                          {text: 'ひび割れた石レンガ', value: 'cracked_stone_bricks'},
                          {text: '模様入り石レンガ', value: 'chiseled_stone_bricks'},
                          {text: 'ネザーレンガ', value: 'nether_bricks'},
                          {text: '赤いネザーレンガ', value: 'red_nether_bricks'},
                          {text: 'エンドストーンレンガ', value: 'end_stone_bricks'},
                          {text: 'プリズマリン', value: 'prismarine'},
                          {text: 'プリズマリンレンガ', value: 'prismarine_bricks'},
                          {text: 'ダークプリズマリン', value: 'dark_prismarine'},
                          {text: 'クォーツブロック', value: 'quartz_block'},
                          {text: '滑らかなクォーツ', value: 'smooth_quartz'},
                          
                          // ガラス系
                          {text: 'ガラス', value: 'glass'},
                          {text: '白色のガラス', value: 'white_stained_glass'},
                          {text: '橙色のガラス', value: 'orange_stained_glass'},
                          {text: '赤紫色のガラス', value: 'magenta_stained_glass'},
                          {text: '空色のガラス', value: 'light_blue_stained_glass'},
                          {text: '黄色のガラス', value: 'yellow_stained_glass'},
                          {text: '黄緑色のガラス', value: 'lime_stained_glass'},
                          {text: '桃色のガラス', value: 'pink_stained_glass'},
                          {text: '灰色のガラス', value: 'gray_stained_glass'},
                          {text: '青緑色のガラス', value: 'cyan_stained_glass'},
                          {text: '紫色のガラス', value: 'purple_stained_glass'},
                          {text: '青色のガラス', value: 'blue_stained_glass'},
                          {text: '茶色のガラス', value: 'brown_stained_glass'},
                          {text: '緑色のガラス', value: 'green_stained_glass'},
                          {text: '赤色のガラス', value: 'red_stained_glass'},
                          {text: '黒色のガラス', value: 'black_stained_glass'},
                          
                          // ウール系
                          {text: '白色の羊毛', value: 'white_wool'},
                          {text: '橙色の羊毛', value: 'orange_wool'},
                          {text: '赤紫色の羊毛', value: 'magenta_wool'},
                          {text: '空色の羊毛', value: 'light_blue_wool'},
                          {text: '黄色の羊毛', value: 'yellow_wool'},
                          {text: '黄緑色の羊毛', value: 'lime_wool'},
                          {text: '桃色の羊毛', value: 'pink_wool'},
                          {text: '灰色の羊毛', value: 'gray_wool'},
                          {text: '薄灰色の羊毛', value: 'light_gray_wool'},
                          {text: '青緑色の羊毛', value: 'cyan_wool'},
                          {text: '紫色の羊毛', value: 'purple_wool'},
                          {text: '青色の羊毛', value: 'blue_wool'},
                          {text: '茶色の羊毛', value: 'brown_wool'},
                          {text: '緑色の羊毛', value: 'green_wool'},
                          {text: '赤色の羊毛', value: 'red_wool'},
                          {text: '黒色の羊毛', value: 'black_wool'},
                          
                          // コンクリート系
                          {text: '白色のコンクリート', value: 'white_concrete'},
                          {text: '橙色のコンクリート', value: 'orange_concrete'},
                          {text: '赤紫色のコンクリート', value: 'magenta_concrete'},
                          {text: '空色のコンクリート', value: 'light_blue_concrete'},
                          {text: '黄色のコンクリート', value: 'yellow_concrete'},
                          {text: '黄緑色のコンクリート', value: 'lime_concrete'},
                          {text: '桃色のコンクリート', value: 'pink_concrete'},
                          {text: '灰色のコンクリート', value: 'gray_concrete'},
                          {text: '薄灰色のコンクリート', value: 'light_gray_concrete'},
                          {text: '青緑色のコンクリート', value: 'cyan_concrete'},
                          {text: '紫色のコンクリート', value: 'purple_concrete'},
                          {text: '青色のコンクリート', value: 'blue_concrete'},
                          {text: '茶色のコンクリート', value: 'brown_concrete'},
                          {text: '緑色のコンクリート', value: 'green_concrete'},
                          {text: '赤色のコンクリート', value: 'red_concrete'},
                          {text: '黒色のコンクリート', value: 'black_concrete'},
                          
                          // テラコッタ系
                          {text: 'テラコッタ', value: 'terracotta'},
                          {text: '白色のテラコッタ', value: 'white_terracotta'},
                          {text: '橙色のテラコッタ', value: 'orange_terracotta'},
                          {text: '赤紫色のテラコッタ', value: 'magenta_terracotta'},
                          {text: '空色のテラコッタ', value: 'light_blue_terracotta'},
                          {text: '黄色のテラコッタ', value: 'yellow_terracotta'},
                          {text: '黄緑色のテラコッタ', value: 'lime_terracotta'},
                          {text: '桃色のテラコッタ', value: 'pink_terracotta'},
                          {text: '灰色のテラコッタ', value: 'gray_terracotta'},
                          {text: '薄灰色のテラコッタ', value: 'light_gray_terracotta'},
                          {text: '青緑色のテラコッタ', value: 'cyan_terracotta'},
                          {text: '紫色のテラコッタ', value: 'purple_terracotta'},
                          {text: '青色のテラコッタ', value: 'blue_terracotta'},
                          {text: '茶色のテラコッタ', value: 'brown_terracotta'},
                          {text: '緑色のテラコッタ', value: 'green_terracotta'},
                          {text: '赤色のテラコッタ', value: 'red_terracotta'},
                          {text: '黒色のテラコッタ', value: 'black_terracotta'},
                          
                          // 鉱石・貴重ブロック
                          {text: '金ブロック', value: 'gold_block'},
                          {text: '鉄ブロック', value: 'iron_block'},
                          {text: 'ダイヤモンドブロック', value: 'diamond_block'},
                          {text: 'エメラルドブロック', value: 'emerald_block'},
                          {text: 'ラピスラズリブロック', value: 'lapis_block'},
                          {text: 'レッドストーンブロック', value: 'redstone_block'},
                          {text: '石炭ブロック', value: 'coal_block'},
                          {text: '銅ブロック', value: 'copper_block'},
                          {text: 'ネザライトブロック', value: 'netherite_block'},
                          
                          // 機能ブロック
                          {text: 'グロウストーン', value: 'glowstone'},
                          {text: 'シーランタン', value: 'sea_lantern'},
                          {text: 'シュルームライト', value: 'shroomlight'},
                          {text: '黒曜石', value: 'obsidian'},
                          {text: '泣く黒曜石', value: 'crying_obsidian'},
                          {text: 'TNT', value: 'tnt'},
                          {text: 'スポンジ', value: 'sponge'},
                          {text: '濡れたスポンジ', value: 'wet_sponge'},
                          {text: '氷', value: 'ice'},
                          {text: '氷塊', value: 'packed_ice'},
                          {text: '青氷', value: 'blue_ice'},
                          {text: '雪ブロック', value: 'snow_block'},
                          
                          // 植物・自然系
                          {text: 'オークの葉', value: 'oak_leaves'},
                          {text: 'シラカバの葉', value: 'birch_leaves'},
                          {text: 'トウヒの葉', value: 'spruce_leaves'},
                          {text: 'ジャングルの葉', value: 'jungle_leaves'},
                          {text: 'アカシアの葉', value: 'acacia_leaves'},
                          {text: 'ダークオークの葉', value: 'dark_oak_leaves'},
                          {text: '苔ブロック', value: 'moss_block'},
                          {text: '干草の俵', value: 'hay_block'},
                          {text: '乾燥した昆布ブロック', value: 'dried_kelp_block'},
                          
                          // ネザー系
                          {text: 'ネザーラック', value: 'netherrack'},
                          {text: 'ソウルサンド', value: 'soul_sand'},
                          {text: 'ソウルソイル', value: 'soul_soil'},
                          {text: '玄武岩', value: 'basalt'},
                          {text: '磨かれた玄武岩', value: 'polished_basalt'},
                          {text: 'ブラックストーン', value: 'blackstone'},
                          {text: '磨かれたブラックストーン', value: 'polished_blackstone'},
                          {text: 'マグマブロック', value: 'magma_block'},
                          
                          // エンド系
                          {text: 'エンドストーン', value: 'end_stone'},
                          {text: 'プルプァブロック', value: 'purpur_block'},
                          {text: 'プルプァの柱', value: 'purpur_pillar'},
                          {text: 'コーラスフラワー', value: 'chorus_flower'},
                          {text: 'コーラスプラント', value: 'chorus_plant'},
                          
                          // 装飾ブロック
                          {text: '本棚', value: 'bookshelf'},
                          {text: 'ジャック・オ・ランタン', value: 'jack_o_lantern'},
                          {text: 'メロン', value: 'melon'},
                          {text: 'カボチャ', value: 'pumpkin'},
                          {text: 'くり抜かれたカボチャ', value: 'carved_pumpkin'},
                          {text: '骨ブロック', value: 'bone_block'},
                          {text: 'スライムブロック', value: 'slime_block'},
                          {text: 'ハチミツブロック', value: 'honey_block'},
                          {text: 'ハニカムブロック', value: 'honeycomb_block'}
                      ]
                  }
              },
              blocks: [
                  // 接続管理
                  {
                      opcode: 'connect',
                      blockType: BlockType.COMMAND,
                      text: '🔌 Minecraftに接続する'
                  },
                  {
                      opcode: 'isConnected',
                      blockType: BlockType.BOOLEAN,
                      text: '📡 接続されている？'
                  },
                  // ブロック操作
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
                  // プレイヤー操作
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
                  // 建築ブロック - 基本操作
                  {
                      opcode: 'removeBlock',
                      blockType: BlockType.COMMAND,
                      text: '⛏️ X:[X] Y:[Y] Z:[Z] のブロックを壊す',
                      arguments: {
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                      }
                  },
                  {
                      opcode: 'getBlockType',
                      blockType: BlockType.REPORTER,
                      text: '🔍 X:[X] Y:[Y] Z:[Z] のブロック名',
                      arguments: {
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                      }
                  },
                  // 建築ブロック - 範囲操作
                  {
                      opcode: 'fillBlocks',
                      blockType: BlockType.COMMAND,
                      text: '🧱 [BLOCK]で X1:[X1] Y1:[Y1] Z1:[Z1] から X2:[X2] Y2:[Y2] Z2:[Z2] を埋める',
                      arguments: {
                          BLOCK: { type: ArgumentType.STRING, menu: 'blockTypes', defaultValue: 'stone' },
                          X1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          X2: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y2: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z2: { type: ArgumentType.NUMBER, defaultValue: 0 }
                      }
                  },
                  {
                      opcode: 'clearArea',
                      blockType: BlockType.COMMAND,
                      text: '🧽 X1:[X1] Y1:[Y1] Z1:[Z1] から X2:[X2] Y2:[Y2] Z2:[Z2] を空にする',
                      arguments: {
                          X1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          X2: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y2: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z2: { type: ArgumentType.NUMBER, defaultValue: 0 }
                      }
                  },
                  // 建築ブロック - 幾何学形状
                  {
                      opcode: 'buildCircle',
                      blockType: BlockType.COMMAND,
                      text: '⭕ [BLOCK]で 中心X:[X] Y:[Y] Z:[Z] 半径:[RADIUS] の円を作る',
                      arguments: {
                          BLOCK: { type: ArgumentType.STRING, menu: 'blockTypes', defaultValue: 'stone' },
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          RADIUS: { type: ArgumentType.NUMBER, defaultValue: 5 }
                      }
                  },
                  {
                      opcode: 'buildSphere',
                      blockType: BlockType.COMMAND,
                      text: '🌕 [BLOCK]で 中心X:[X] Y:[Y] Z:[Z] 半径:[RADIUS] の球を作る',
                      arguments: {
                          BLOCK: { type: ArgumentType.STRING, menu: 'blockTypes', defaultValue: 'stone' },
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          RADIUS: { type: ArgumentType.NUMBER, defaultValue: 5 }
                      }
                  },
                  {
                      opcode: 'buildWall',
                      blockType: BlockType.COMMAND,
                      text: '🧱 [BLOCK]で X1:[X1] Z1:[Z1] から X2:[X2] Z2:[Z2] 高さ:[HEIGHT] の壁を作る',
                      arguments: {
                          BLOCK: { type: ArgumentType.STRING, menu: 'blockTypes', defaultValue: 'stone' },
                          X1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z1: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          X2: { type: ArgumentType.NUMBER, defaultValue: 10 },
                          Z2: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          HEIGHT: { type: ArgumentType.NUMBER, defaultValue: 3 }
                      }
                  },
                  {
                      opcode: 'buildHouse',
                      blockType: BlockType.COMMAND,
                      text: '🏠 [BLOCK]で X:[X] Y:[Y] Z:[Z] に 幅:[WIDTH] 奥行:[DEPTH] 高さ:[HEIGHT] の家を作る',
                      arguments: {
                          BLOCK: { type: ArgumentType.STRING, menu: 'blockTypes', defaultValue: 'oak_planks' },
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          WIDTH: { type: ArgumentType.NUMBER, defaultValue: 7 },
                          DEPTH: { type: ArgumentType.NUMBER, defaultValue: 7 },
                          HEIGHT: { type: ArgumentType.NUMBER, defaultValue: 4 }
                      }
                  },
                  // プレイヤー操作
                  {
                      opcode: 'teleportPlayer',
                      blockType: BlockType.COMMAND,
                      text: '🚀 プレイヤーを X:[X] Y:[Y] Z:[Z] にテレポート',
                      arguments: {
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                      }
                  },
                  {
                      opcode: 'teleportToSpawn',
                      blockType: BlockType.COMMAND,
                      text: '🏠 スポーン地点 (0,0,0) に戻る'
                  },
                  {
                      opcode: 'setPlayerMode',
                      blockType: BlockType.COMMAND,
                      text: '🎮 プレイヤーのモードを [MODE] に変更',
                      arguments: {
                          MODE: {
                              type: ArgumentType.STRING,
                              menu: 'gameMode',
                              defaultValue: 'creative'
                          }
                      }
                  },
                  // 時間・天候
                  {
                      opcode: 'setTime',
                      blockType: BlockType.COMMAND,
                      text: '🕐 時間を [TIME] に設定',
                      arguments: {
                          TIME: {
                              type: ArgumentType.STRING,
                              menu: 'timeMenu',
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
                              menu: 'weatherMenu',
                              defaultValue: 'clear'
                          }
                      }
                  },
                  // 時間と天候の固定
                  {
                      opcode: 'lockTime',
                      blockType: BlockType.COMMAND,
                      text: '🔒 時間を [LOCK] する',
                      arguments: {
                          LOCK: {
                              type: ArgumentType.STRING,
                              menu: 'lockMenu',
                              defaultValue: 'lock'
                          }
                      }
                  },
                  {
                      opcode: 'lockWeather',
                      blockType: BlockType.COMMAND,
                      text: '🔒 天候を [LOCK] する',
                      arguments: {
                          LOCK: {
                              type: ArgumentType.STRING,
                              menu: 'lockMenu',
                              defaultValue: 'lock'
                          }
                      }
                  },
                  // コマンド実行
                  {
                      opcode: 'executeCommand',
                      blockType: BlockType.COMMAND,
                      text: '⚡ コマンド [COMMAND] を実行',
                      arguments: {
                          COMMAND: {
                              type: ArgumentType.STRING,
                              defaultValue: 'give @s diamond 1'
                          }
                      }
                  },
                  // チャット
                  {
                      opcode: 'sendChat',
                      blockType: BlockType.COMMAND,
                      text: '💬 チャット: [MESSAGE]',
                      arguments: {
                          MESSAGE: {
                              type: ArgumentType.STRING,
                              defaultValue: 'Hello Minecraft!'
                          }
                      }
                  },
                  // 協調機能 - 招待システム
                  '---',
                  {
                      opcode: 'inviteFriend',
                      blockType: BlockType.COMMAND,
                      text: '📧 [FRIEND]さんを招待',
                      arguments: {
                          FRIEND: {
                              type: ArgumentType.STRING,
                              defaultValue: '友達の名前'
                          }
                      }
                  },
                  {
                      opcode: 'getInvitations',
                      blockType: BlockType.REPORTER,
                      text: '📬 招待通知の数'
                  },
                  // 協調機能 - 訪問システム
                  {
                      opcode: 'requestVisit',
                      blockType: BlockType.COMMAND,
                      text: '🚪 [FRIEND]さんの世界に訪問申請',
                      arguments: {
                          FRIEND: {
                              type: ArgumentType.STRING,
                              defaultValue: '友達の名前'
                          }
                      }
                  },
                  {
                      opcode: 'approveVisit',
                      blockType: BlockType.COMMAND,
                      text: '✅ [VISITOR]さんの訪問を承認',
                      arguments: {
                          VISITOR: {
                              type: ArgumentType.STRING,
                              defaultValue: '訪問者の名前'
                          }
                      }
                  },
                  {
                      opcode: 'getCurrentWorld',
                      blockType: BlockType.REPORTER,
                      text: '🌍 現在いる世界'
                  },
                  // 協調機能 - 帰宅システム
                  {
                      opcode: 'returnHome',
                      blockType: BlockType.COMMAND,
                      text: '🏠 自分のワールドに帰る'
                  },
                  {
                      opcode: 'emergencyReturn',
                      blockType: BlockType.COMMAND,
                      text: '🚨 緊急帰宅'
                  },
                  // エージェントシステム
                  '---',
                  {
                      opcode: 'summonAgent',
                      blockType: BlockType.COMMAND,
                      text: '🤖 エージェント[NAME]を召喚',
                      arguments: {
                          NAME: {
                              type: ArgumentType.STRING,
                              defaultValue: 'Agent'
                          }
                      }
                  },
                  {
                      opcode: 'moveAgentDirection',
                      blockType: BlockType.COMMAND,
                      text: '🤖 エージェントを[DIRECTION]に[DISTANCE]ブロック移動',
                      arguments: {
                          DIRECTION: {
                              type: ArgumentType.STRING,
                              menu: 'directionMenu',
                              defaultValue: 'forward'
                          },
                          DISTANCE: {
                              type: ArgumentType.NUMBER,
                              defaultValue: 1
                          }
                      }
                  },
                  {
                      opcode: 'moveAgentTo',
                      blockType: BlockType.COMMAND,
                      text: '🤖 エージェントをX:[X] Y:[Y] Z:[Z]に移動',
                      arguments: {
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                      }
                  },
                  {
                      opcode: 'agentFollow',
                      blockType: BlockType.COMMAND,
                      text: '🤖 エージェントに[FOLLOW]',
                      arguments: {
                          FOLLOW: {
                              type: ArgumentType.STRING,
                              menu: 'followMenu',
                              defaultValue: 'follow'
                          }
                      }
                  },
                  {
                      opcode: 'agentAction',
                      blockType: BlockType.COMMAND,
                      text: '🤖 エージェントに[ACTION]させる',
                      arguments: {
                          ACTION: {
                              type: ArgumentType.STRING,
                              menu: 'actionMenu',
                              defaultValue: 'jump'
                          }
                      }
                  },
                  {
                      opcode: 'dismissAgent',
                      blockType: BlockType.COMMAND,
                      text: '🤖 エージェントを帰す'
                  },
                  // === 教師管理機能 ===
                  '---',
                  {
                      opcode: 'registerTeacher',
                      blockType: BlockType.COMMAND,
                      text: '👩‍🏫 教師として登録 パスワード:[PASSWORD]',
                      arguments: {
                          PASSWORD: {
                              type: ArgumentType.STRING,
                              defaultValue: ''  // No default password for security
                          }
                      }
                  },
                  {
                      opcode: 'toggleClassroomMode',
                      blockType: BlockType.COMMAND,
                      text: '🏫 授業モードを切り替え'
                  },
                  {
                      opcode: 'setGlobalPermissions',
                      blockType: BlockType.COMMAND,
                      text: '📋 全体権限設定 建築:[BUILDING] チャット:[CHAT] 訪問:[VISITS]',
                      arguments: {
                          BUILDING: {
                              type: ArgumentType.STRING,
                              menu: 'permission',
                              defaultValue: 'true'
                          },
                          CHAT: {
                              type: ArgumentType.STRING,
                              menu: 'permission',
                              defaultValue: 'true'
                          },
                          VISITS: {
                              type: ArgumentType.STRING,
                              menu: 'permission',
                              defaultValue: 'true'
                          }
                      }
                  },
                  {
                      opcode: 'setStudentTimeLimit',
                      blockType: BlockType.COMMAND,
                      text: '⏰ 生徒 [STUDENT] の制限時間を [MINUTES] 分に設定',
                      arguments: {
                          STUDENT: {
                              type: ArgumentType.STRING,
                              defaultValue: 'PlayerName'
                          },
                          MINUTES: {
                              type: ArgumentType.NUMBER,
                              defaultValue: 30
                          }
                      }
                  },
                  {
                      opcode: 'addStudentRestriction',
                      blockType: BlockType.COMMAND,
                      text: '🚫 生徒 [STUDENT] に制限 [ACTION] を追加',
                      arguments: {
                          STUDENT: {
                              type: ArgumentType.STRING,
                              defaultValue: 'PlayerName'
                          },
                          ACTION: {
                              type: ArgumentType.STRING,
                              menu: 'restrictionAction',
                              defaultValue: 'build'
                          }
                      }
                  },
                  {
                      opcode: 'freezeAllStudents',
                      blockType: BlockType.COMMAND,
                      text: '❄️ 全生徒を [FREEZE] する',
                      arguments: {
                          FREEZE: {
                              type: ArgumentType.STRING,
                              menu: 'freezeAction',
                              defaultValue: 'freeze'
                          }
                      }
                  },
                  {
                      opcode: 'summonAllStudents',
                      blockType: BlockType.COMMAND,
                      text: '📢 全生徒を集合させる'
                  },
                  {
                      opcode: 'broadcastToStudents',
                      blockType: BlockType.COMMAND,
                      text: '📣 生徒に一斉送信: [MESSAGE]',
                      arguments: {
                          MESSAGE: {
                              type: ArgumentType.STRING,
                              defaultValue: 'お知らせです'
                          }
                      }
                  },
                  {
                      opcode: 'getStudentActivities',
                      blockType: BlockType.COMMAND,
                      text: '📊 生徒活動レポートを取得 [STUDENT]',
                      arguments: {
                          STUDENT: {
                              type: ArgumentType.STRING,
                              defaultValue: ''
                          }
                      }
                  },
                  // === 学習進捗機能 ===
                  '---',
                  {
                      opcode: 'getMyProgress',
                      blockType: BlockType.COMMAND,
                      text: '📈 私の学習進捗を確認'
                  },
                  {
                      opcode: 'getStudentProgress',
                      blockType: BlockType.COMMAND,
                      text: '📊 生徒の進捗を確認 [STUDENT]',
                      arguments: {
                          STUDENT: {
                              type: ArgumentType.STRING,
                              defaultValue: 'PlayerName'
                          }
                      }
                  },
                  {
                      opcode: 'configureProgressTracking',
                      blockType: BlockType.COMMAND,
                      text: '⚙️ 進捗設定 有効:[ENABLED] ブロック:[BLOCK_POINTS]pt コマンド:[COMMAND_POINTS]pt 協調:[COLLAB_POINTS]pt',
                      arguments: {
                          ENABLED: {
                              type: ArgumentType.STRING,
                              menu: 'permission',
                              defaultValue: 'true'
                          },
                          BLOCK_POINTS: {
                              type: ArgumentType.NUMBER,
                              defaultValue: 1
                          },
                          COMMAND_POINTS: {
                              type: ArgumentType.NUMBER,
                              defaultValue: 2
                          },
                          COLLAB_POINTS: {
                              type: ArgumentType.NUMBER,
                              defaultValue: 5
                          }
                      }
                  },
                  {
                      opcode: 'resetStudentProgress',
                      blockType: BlockType.COMMAND,
                      text: '🔄 生徒の進捗をリセット [STUDENT]',
                      arguments: {
                          STUDENT: {
                              type: ArgumentType.STRING,
                              defaultValue: 'PlayerName'
                          }
                      }
                  },
                  // === 多言語サポート ===
                  '---',
                  {
                      opcode: 'setLanguage',
                      blockType: BlockType.COMMAND,
                      text: '🌍 言語を設定 [LANGUAGE]',
                      arguments: {
                          LANGUAGE: {
                              type: ArgumentType.STRING,
                              menu: 'languageMenu',
                              defaultValue: 'ja_JP'
                          }
                      }
                  },
                  {
                      opcode: 'getLanguage',
                      blockType: BlockType.REPORTER,
                      text: '🌐 現在の言語'
                  },
                  {
                      opcode: 'listLanguages',
                      blockType: BlockType.COMMAND,
                      text: '📋 対応言語一覧を表示'
                  },
                  {
                      opcode: 'setDefaultLanguage',
                      blockType: BlockType.COMMAND,
                      text: '⚙️ デフォルト言語を設定 [LANGUAGE]',
                      arguments: {
                          LANGUAGE: {
                              type: ArgumentType.STRING,
                              menu: 'languageMenu',
                              defaultValue: 'ja_JP'
                          }
                      }
                  },
                  // === ブロックパック機能 ===
                  '---',
                  {
                      opcode: 'applyBlockPack',
                      blockType: BlockType.COMMAND,
                      text: '📦 ブロックパックを適用 [PACK_ID]',
                      arguments: {
                          PACK_ID: {
                              type: ArgumentType.STRING,
                              menu: 'blockPackMenu',
                              defaultValue: 'basic'
                          }
                      }
                  },
                  {
                      opcode: 'getAvailableBlockPacks',
                      blockType: BlockType.REPORTER,
                      text: '📋 利用可能なブロックパック一覧'
                  },
                  {
                      opcode: 'getCurrentBlockPack',
                      blockType: BlockType.REPORTER,
                      text: '📦 現在のブロックパック'
                  },
                  {
                      opcode: 'getBlockPackInfo',
                      blockType: BlockType.REPORTER,
                      text: '📖 ブロックパック情報 [PACK_ID]',
                      arguments: {
                          PACK_ID: {
                              type: ArgumentType.STRING,
                              menu: 'blockPackMenu',
                              defaultValue: 'basic'
                          }
                      }
                  },
                  {
                      opcode: 'createCustomBlockPack',
                      blockType: BlockType.COMMAND,
                      text: '🛠️ カスタムパック作成 ID:[PACK_ID] 名前:[PACK_NAME] 説明:[DESCRIPTION] ブロック:[BLOCKS]',
                      arguments: {
                          PACK_ID: {
                              type: ArgumentType.STRING,
                              defaultValue: 'my_pack'
                          },
                          PACK_NAME: {
                              type: ArgumentType.STRING,
                              defaultValue: 'マイパック'
                          },
                          DESCRIPTION: {
                              type: ArgumentType.STRING,
                              defaultValue: '私の専用ブロックパック'
                          },
                          BLOCKS: {
                              type: ArgumentType.STRING,
                              defaultValue: 'stone,dirt,grass_block'
                          }
                      }
                  },
                  // === オフラインモード機能 ===
                  '---',
                  {
                      opcode: 'setOfflineMode',
                      blockType: BlockType.COMMAND,
                      text: '📴 オフラインモード [ENABLED]',
                      arguments: {
                          ENABLED: {
                              type: ArgumentType.STRING,
                              menu: 'enabledMenu',
                              defaultValue: 'true'
                          }
                      }
                  },
                  {
                      opcode: 'getOfflineStatus',
                      blockType: BlockType.REPORTER,
                      text: '📊 オフライン状態'
                  },
                  {
                      opcode: 'syncOfflineData',
                      blockType: BlockType.COMMAND,
                      text: '🔄 オフラインデータを同期'
                  },
                  {
                      opcode: 'exportOfflineData',
                      blockType: BlockType.COMMAND,
                      text: '📤 オフラインデータをエクスポート'
                  },
                  {
                      opcode: 'setAutoSync',
                      blockType: BlockType.COMMAND,
                      text: '⚙️ 自動同期 [ENABLED]',
                      arguments: {
                          ENABLED: {
                              type: ArgumentType.STRING,
                              menu: 'enabledMenu',
                              defaultValue: 'true'
                          }
                      }
                  }
              ],
              menus: {
                  gameMode: {
                      acceptReporters: true,
                      items: [
                          'survival',
                          'creative', 
                          'adventure',
                          'spectator'
                      ]
                  },
                  timeMenu: {
                      acceptReporters: true,
                      items: [
                          'day',
                          'night',
                          'noon',
                          'midnight',
                          'sunrise',
                          'sunset'
                      ]
                  },
                  weatherMenu: {
                      acceptReporters: true,
                      items: [
                          'clear',
                          'rain',
                          'thunder'
                      ]
                  },
                  directionMenu: {
                      acceptReporters: true,
                      items: [
                          'forward',
                          'backward',
                          'left',
                          'right',
                          'up',
                          'down'
                      ]
                  },
                  followMenu: {
                      acceptReporters: true,
                      items: [
                          {text: 'ついてくるようにする', value: 'follow'},
                          {text: 'ついてこないようにする', value: 'stop'}
                      ]
                  },
                  actionMenu: {
                      acceptReporters: true,
                      items: [
                          {text: 'ジャンプ', value: 'jump'},
                          {text: '回転', value: 'spin'},
                          {text: 'ダンス', value: 'dance'}
                      ]
                  },
                  lockMenu: {
                      acceptReporters: true,
                      items: [
                          {text: '固定', value: 'lock'},
                          {text: '解除', value: 'unlock'}
                      ]
                  },
                  permission: {
                      acceptReporters: true,
                      items: [
                          {text: '許可', value: 'true'},
                          {text: '禁止', value: 'false'}
                      ]
                  },
                  restrictionAction: {
                      acceptReporters: true,
                      items: [
                          {text: '建築', value: 'build'},
                          {text: 'チャット', value: 'chat'},
                          {text: '訪問', value: 'visit'}
                      ]
                  },
                  freezeAction: {
                      acceptReporters: true,
                      items: [
                          {text: '停止', value: 'freeze'},
                          {text: '再開', value: 'unfreeze'}
                      ]
                  },
                  languageMenu: {
                      acceptReporters: true,
                      items: [
                          {text: '日本語', value: 'ja_JP'},
                          {text: 'English', value: 'en_US'},
                          {text: '简体中文', value: 'zh_CN'},
                          {text: '繁體中文', value: 'zh_TW'},
                          {text: '한국어', value: 'ko_KR'},
                          {text: 'Español', value: 'es_ES'},
                          {text: 'Français', value: 'fr_FR'},
                          {text: 'Deutsch', value: 'de_DE'}
                      ]
                  },
                  blockPackMenu: {
                      acceptReporters: true,
                      items: [
                          {text: '基本', value: 'basic'},
                          {text: '教育', value: 'educational'},
                          {text: 'クリエイティブ', value: 'creative'},
                          {text: '建築', value: 'architectural'},
                          {text: 'レッドストーン', value: 'redstone'},
                          {text: '自然', value: 'nature'},
                          {text: '初心者', value: 'beginner'},
                          {text: '上級者', value: 'advanced'}
                      ]
                  },
                  enabledMenu: {
                      acceptReporters: true,
                      items: [
                          {text: '有効', value: 'true'},
                          {text: '無効', value: 'false'}
                      ]
                  }
              }
          };
      }

      // WebSocket接続
      connect() {
          if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
              console.log('Already connected to Minecraft');
              return;
          }

          try {
              this.websocket = new WebSocket('ws://localhost:14711');
              
              // Connection timeout
              const connectionTimeout = setTimeout(() => {
                  if (this.websocket.readyState !== WebSocket.OPEN) {
                      console.error('Connection timeout');
                      this.websocket.close();
                      this.connectionStatus = 'timeout';
                  }
              }, 5000);

              this.websocket.onopen = () => {
                  clearTimeout(connectionTimeout);
                  this.connectionStatus = 'connected';
                  console.log('Minecraft WebSocket connected');
              };

              this.websocket.onmessage = (event) => {
                  this.handleMessage(event.data);
              };

              this.websocket.onclose = () => {
                  clearTimeout(connectionTimeout);
                  this.connectionStatus = 'disconnected';
                  console.log('Minecraft WebSocket disconnected');
                  // Auto-reconnect logic could be added here
              };
              
              this.websocket.onerror = (error) => {
                  clearTimeout(connectionTimeout);
                  console.error('WebSocket error:', error);
                  this.connectionStatus = 'error';
              };

          } catch (error) {
              console.error('Failed to connect:', error);
              this.connectionStatus = 'error';
          }
      }

      isConnected() {
          return this.connectionStatus === 'connected';
      }

      // メッセージハンドリング
      handleMessage(data) {
          try {
              const response = JSON.parse(data);
              
              switch (response.type) {
                  case 'playerPos':
                      if (response.data && typeof response.data.x === 'number') {
                          this.playerPos = response.data;
                      }
                      break;
                      
                  case 'blockInfo':
                      this.lastBlockInfo = response.data || '';
                      break;
                      
                  case 'invitations':
                      if (response.data && typeof response.data.count === 'number') {
                          this.invitationCount = response.data.count;
                      }
                      break;
                      
                  case 'currentWorld':
                      if (response.data && response.data.world) {
                          this.currentWorld = response.data.world;
                      }
                      break;
                      
                  case 'error':
                      console.error('Server error:', response.message);
                      break;
                      
                  case 'welcome':
                      console.log('Connected to server:', response.message);
                      break;
                      
                  default:
                      console.log('Unknown message type:', response.type);
              }
          } catch (error) {
              console.error('Failed to parse message:', error);
              // Try to handle as plain text
              console.log('Raw message:', data);
          }
      }

      // コマンド送信
      sendCommand(command, args = {}) {
          if (!this.isConnected()) {
              console.warn('Not connected to Minecraft');
              return Promise.reject('Not connected');
          }
          
          // Validate args
          const sanitizedArgs = {};
          for (const [key, value] of Object.entries(args)) {
              if (value !== null && value !== undefined) {
                  sanitizedArgs[key] = String(value);
              }
          }

          const message = JSON.stringify({
              command: command,
              args: sanitizedArgs
          });

          try {
              this.websocket.send(message);
              return Promise.resolve();
          } catch (error) {
              console.error('Failed to send command:', error);
              return Promise.reject(error);
          }
      }

      // ブロック操作
      placeBlock(args) {
          // Validate coordinates
          const x = this.validateNumber(args.X, 0);
          const y = this.validateNumber(args.Y, 0);  // デフォルトを0に変更
          const z = this.validateNumber(args.Z, 0);
          const block = this.validateString(args.BLOCK, 'stone');
          
          // スーパーフラット用にY座標を調整
          const adjustedY = this.adjustYForSuperflat(y);
          
          this.sendCommand('placeBlock', {
              block: block,
              x: x,
              y: adjustedY,
              z: z
          });
      }

      // プレイヤー座標取得
      getPlayerX() {
          this.sendCommand('getPlayerPos');
          return this.playerPos.x;
      }

      getPlayerY() {
          this.sendCommand('getPlayerPos');
          // Minecraft 1.18+: 地表Y=-60を0として表示
          return this.playerPos.y + 60;
      }

      getPlayerZ() {
          this.sendCommand('getPlayerPos');
          return this.playerPos.z;
      }

      // ブロック破壊
      removeBlock(args) {
          const x = this.validateNumber(args.X, 0);
          const y = this.validateNumber(args.Y, 0);
          const z = this.validateNumber(args.Z, 0);
          
          // スーパーフラット用にY座標を調整
          const adjustedY = this.adjustYForSuperflat(y);
          
          this.sendCommand('removeBlock', {
              x: x,
              y: adjustedY,
              z: z
          });
      }

      // ブロック情報取得
      getBlockType(args) {
          const x = this.validateNumber(args.X, 0);
          const y = this.validateNumber(args.Y, 0);
          const z = this.validateNumber(args.Z, 0);
          
          // スーパーフラット用にY座標を調整
          const adjustedY = this.adjustYForSuperflat(y);
          
          this.sendCommand('getBlock', {
              x: x,
              y: adjustedY,
              z: z
          });
          return this.lastBlockInfo;
      }

      // 範囲埋め
      fillBlocks(args) {
          const x1 = this.validateNumber(args.X1, 0);
          const y1 = this.validateNumber(args.Y1, 0);
          const z1 = this.validateNumber(args.Z1, 0);
          const x2 = this.validateNumber(args.X2, 0);
          const y2 = this.validateNumber(args.Y2, 0);
          const z2 = this.validateNumber(args.Z2, 0);
          
          // スーパーフラット用にY座標を調整
          const adjustedY1 = this.adjustYForSuperflat(y1);
          const adjustedY2 = this.adjustYForSuperflat(y2);
          
          this.sendCommand('fill', {
              block: args.BLOCK,
              x1: x1,
              y1: adjustedY1,
              z1: z1,
              x2: x2,
              y2: adjustedY2,
              z2: z2
          });
      }

      // 範囲クリア
      clearArea(args) {
          const x1 = this.validateNumber(args.X1, 0);
          const y1 = this.validateNumber(args.Y1, 0);
          const z1 = this.validateNumber(args.Z1, 0);
          const x2 = this.validateNumber(args.X2, 0);
          const y2 = this.validateNumber(args.Y2, 0);
          const z2 = this.validateNumber(args.Z2, 0);
          
          // スーパーフラット用にY座標を調整
          const adjustedY1 = this.adjustYForSuperflat(y1);
          const adjustedY2 = this.adjustYForSuperflat(y2);
          
          this.sendCommand('fill', {
              block: 'air',
              x1: x1,
              y1: adjustedY1,
              z1: z1,
              x2: x2,
              y2: adjustedY2,
              z2: z2
          });
      }

      // 円形建築
      buildCircle(args) {
          const x = this.validateNumber(args.X, 0);
          const y = this.validateNumber(args.Y, 0);
          const z = this.validateNumber(args.Z, 0);
          
          // スーパーフラット用にY座標を調整
          const adjustedY = this.adjustYForSuperflat(y);
          
          this.sendCommand('buildCircle', {
              block: args.BLOCK,
              x: x,
              y: adjustedY,
              z: z,
              radius: args.RADIUS
          });
      }

      // 球形建築
      buildSphere(args) {
          const x = this.validateNumber(args.X, 0);
          const y = this.validateNumber(args.Y, 0);
          const z = this.validateNumber(args.Z, 0);
          
          // スーパーフラット用にY座標を調整
          const adjustedY = this.adjustYForSuperflat(y);
          
          this.sendCommand('buildSphere', {
              block: args.BLOCK,
              x: x,
              y: adjustedY,
              z: z,
              radius: args.RADIUS
          });
      }

      // 壁建築
      buildWall(args) {
          // 壁はY座標を直接指定しないが、地面レベルからの高さを指定
          // サーバー側で地面検出されるため、ここでは調整不要
          this.sendCommand('buildWall', {
              block: args.BLOCK,
              x1: args.X1,
              z1: args.Z1,
              x2: args.X2,
              z2: args.Z2,
              height: args.HEIGHT
          });
      }

      // 家建築
      buildHouse(args) {
          const x = this.validateNumber(args.X, 0);
          const y = this.validateNumber(args.Y, 0);
          const z = this.validateNumber(args.Z, 0);
          
          // スーパーフラット用にY座標を調整
          const adjustedY = this.adjustYForSuperflat(y);
          
          this.sendCommand('buildHouse', {
              block: args.BLOCK,
              x: x,
              y: adjustedY,
              z: z,
              width: args.WIDTH,
              depth: args.DEPTH,
              height: args.HEIGHT
          });
      }

      // プレイヤーテレポート
      teleportPlayer(args) {
          const x = this.validateNumber(args.X, 0);
          const y = this.validateNumber(args.Y, 0);
          const z = this.validateNumber(args.Z, 0);
          
          // スーパーフラット用にY座標を調整
          const adjustedY = this.adjustYForSuperflat(y);
          
          this.sendCommand('teleport', {
              x: x,
              y: adjustedY,
              z: z
          });
      }

      // スポーン地点にテレポート
      teleportToSpawn() {
          // 座標(0,0,0)にテレポート（実際はY=-60の地表になる）
          this.sendCommand('teleport', {
              x: 0,
              y: -60,  // Minecraft 1.18+のスーパーフラットの地表レベル
              z: 0
          });
      }

      // ゲームモード設定
      setPlayerMode(args) {
          this.sendCommand('gamemode', {
              mode: args.MODE
          });
      }

      // 時間設定
      setTime(args) {
          this.sendCommand('time', {
              time: args.TIME
          });
      }

      // 天気設定
      setWeather(args) {
          this.sendCommand('weather', {
              weather: args.WEATHER
          });
      }

      // 時間を固定/解除
      lockTime(args) {
          const isLocked = args.LOCK === 'lock';
          
          if (isLocked) {
              // 時間を固定（ゲームルールを変更）
              this.sendCommand('gamerule', { 
                  rule: 'doDaylightCycle',
                  value: 'false'
              });
              // チャットで通知
              this.sendCommand('chat', {
                  message: '⛰️ 時間を固定しました'
              });
          } else {
              // 時間の固定を解除
              this.sendCommand('gamerule', { 
                  rule: 'doDaylightCycle',
                  value: 'true'
              });
              // チャットで通知
              this.sendCommand('chat', {
                  message: '⏰ 時間の固定を解除しました'
              });
          }
      }

      // 天候を固定/解除
      lockWeather(args) {
          const isLocked = args.LOCK === 'lock';
          
          if (isLocked) {
              // 天候を固定（ゲームルールを変更）
              this.sendCommand('gamerule', { 
                  rule: 'doWeatherCycle',
                  value: 'false'
              });
              // チャットで通知
              this.sendCommand('chat', {
                  message: '☁️ 天候を固定しました'
              });
          } else {
              // 天候の固定を解除
              this.sendCommand('gamerule', { 
                  rule: 'doWeatherCycle',
                  value: 'true'
              });
              // チャットで通知
              this.sendCommand('chat', {
                  message: '🌦️ 天候の固定を解除しました'
              });
          }
      }

      // コマンド実行
      executeCommand(args) {
          const command = this.validateString(args.COMMAND, '');
          
          // セキュリティチェック - 危険なコマンドをブロック
          const dangerousCommands = [
              'stop', 'op', 'deop', 'ban', 'kick', 'whitelist',
              'save-all', 'save-on', 'save-off', 'reload'
          ];
          
          const commandLower = command.toLowerCase().trim();
          const firstWord = commandLower.split(' ')[0];
          
          if (dangerousCommands.includes(firstWord)) {
              console.warn(`Blocked dangerous command: ${command}`);
              this.sendCommand('chat', {
                  message: `⚠️ 危険なコマンドはブロックされました: ${firstWord}`
              });
              return;
          }
          
          // コマンドが/で始まっていない場合は追加
          const fullCommand = command.startsWith('/') ? command.substring(1) : command;
          
          // コマンドを実行
          this.sendCommand('executeCommand', { 
              command: fullCommand 
          });
      }

      // チャット
      sendChat(args) {
          const message = this.validateString(args.MESSAGE, '');
          if (message.length > 256) {
              console.warn('Chat message too long, truncating');
              message = message.substring(0, 256);
          }
          
          this.sendCommand('chat', {
              message: message
          });
      }
      
      // === 協調機能 ===
      
      // 友達を招待
      inviteFriend(args) {
          const friendName = this.validateString(args.FRIEND, '');
          if (!friendName) return;
          
          if (this.websocket && this.websocket.readyState === 1) {
              this.websocket.send(`collaboration.invite(${friendName})`);
          }
      }
      
      // 招待数を取得
      getInvitations() {
          if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
              // Request invitation count from server
              this.sendCommand('getInvitations');
          }
          // Return cached value (will be updated by server response)
          return this.invitationCount || 0;
      }
      
      // 訪問申請
      requestVisit(args) {
          const friendName = this.validateString(args.FRIEND, '');
          if (!friendName) return;
          
          if (this.websocket && this.websocket.readyState === 1) {
              this.websocket.send(`collaboration.requestVisit(${friendName})`);
          }
      }
      
      // 訪問承認
      approveVisit(args) {
          const visitorName = this.validateString(args.VISITOR, '');
          if (!visitorName) return;
          
          if (this.websocket && this.websocket.readyState === 1) {
              this.websocket.send(`collaboration.approveVisit(${visitorName})`);
          }
      }
      
      // 現在のワールドを取得
      getCurrentWorld() {
          if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
              // Request current world from server
              this.sendCommand('getCurrentWorld');
          }
          // Return cached value (will be updated by server response)
          return this.currentWorld || 'my_world';
      }
      
      // ホームに帰る
      returnHome() {
          if (this.websocket && this.websocket.readyState === 1) {
              this.websocket.send('collaboration.returnHome()');
          }
      }
      
      // 緊急帰宅
      emergencyReturn() {
          if (this.websocket && this.websocket.readyState === 1) {
              this.websocket.send('collaboration.emergencyReturn()');
          }
      }
      
      // === エージェントシステム ===
      
      // エージェントを召喚
      summonAgent(args) {
          const name = this.validateString(args.NAME, 'Agent');
          this.sendCommand('summonAgent', { name: name });
      }
      
      // エージェントを方向に移動
      moveAgentDirection(args) {
          const direction = this.validateString(args.DIRECTION, 'forward');
          const distance = this.validateNumber(args.DISTANCE, 1);
          this.sendCommand('moveAgent', { 
              direction: direction,
              distance: Math.max(1, Math.min(10, distance)) // 1-10の範囲に制限
          });
      }
      
      // エージェントを座標に移動
      moveAgentTo(args) {
          const x = this.validateNumber(args.X, 0);
          const y = this.validateNumber(args.Y, 0);
          const z = this.validateNumber(args.Z, 0);
          
          // スーパーフラット用にY座標を調整
          const adjustedY = this.adjustYForSuperflat(y);
          
          this.sendCommand('moveAgent', { x: x, y: adjustedY, z: z });
      }
      
      // エージェントのフォロー設定
      agentFollow(args) {
          const follow = args.FOLLOW === 'follow';
          this.sendCommand('agentFollow', { follow: follow });
      }
      
      // エージェントにアクション実行
      agentAction(args) {
          const action = this.validateString(args.ACTION, 'jump');
          this.sendCommand('agentAction', { action: action });
      }
      
      // エージェントを帰す
      dismissAgent() {
          this.sendCommand('dismissAgent');
      }
      
      // === 教師管理機能 ===
      
      // 教師として登録
      registerTeacher(args) {
          const password = this.validateString(args.PASSWORD, '');
          if (!password) {
              console.error('Password is required for teacher registration');
              return;
          }
          this.sendCommand('registerTeacher', { password: password });
      }
      
      // 授業モードの切り替え
      toggleClassroomMode() {
          this.sendCommand('toggleClassroomMode');
      }
      
      // 全体権限設定
      setGlobalPermissions(args) {
          const building = args.BUILDING === 'true';
          const chat = args.CHAT === 'true';
          const visits = args.VISITS === 'true';
          this.sendCommand('setGlobalPermissions', { 
              building: building, 
              chat: chat, 
              visits: visits 
          });
      }
      
      // 生徒の制限時間設定
      setStudentTimeLimit(args) {
          const student = this.validateString(args.STUDENT, '');
          const minutes = this.validateNumber(args.MINUTES, 30);
          if (!student) return;
          
          this.sendCommand('setStudentTimeLimit', { 
              student: student, 
              minutes: Math.max(0, Math.min(120, minutes)) // 0-120分の範囲に制限
          });
      }
      
      // 生徒に制限を追加
      addStudentRestriction(args) {
          const student = this.validateString(args.STUDENT, '');
          const action = this.validateString(args.ACTION, 'build');
          if (!student) return;
          
          this.sendCommand('addStudentRestriction', { 
              student: student, 
              action: action 
          });
      }
      
      // 全生徒を停止/再開
      freezeAllStudents(args) {
          const freeze = args.FREEZE === 'freeze';
          this.sendCommand('freezeAllStudents', { freeze: freeze });
      }
      
      // 全生徒を集合
      summonAllStudents() {
          this.sendCommand('summonAllStudents');
      }
      
      // 生徒に一斉送信
      broadcastToStudents(args) {
          const message = this.validateString(args.MESSAGE, '');
          if (!message) return;
          
          this.sendCommand('broadcastToStudents', { message: message });
      }
      
      // 生徒活動レポート取得
      getStudentActivities(args) {
          const student = this.validateString(args.STUDENT, '');
          if (student) {
              this.sendCommand('getStudentActivities', { student: student });
          } else {
              this.sendCommand('getStudentActivities'); // 全体レポート
          }
      }
      
      // === 学習進捗機能 ===
      
      // 自分の学習進捗を確認
      getMyProgress() {
          this.sendCommand('getMyProgress');
      }
      
      // 生徒の進捗を確認
      getStudentProgress(args) {
          const student = this.validateString(args.STUDENT, '');
          if (!student) return;
          
          this.sendCommand('getStudentProgress', { student: student });
      }
      
      // 進捗トラッキングの設定
      configureProgressTracking(args) {
          const enabled = args.ENABLED === 'true';
          const blockPoints = this.validateNumber(args.BLOCK_POINTS, 1);
          const commandPoints = this.validateNumber(args.COMMAND_POINTS, 2);
          const collabPoints = this.validateNumber(args.COLLAB_POINTS, 5);
          
          this.sendCommand('configureProgressTracking', {
              enabled: enabled,
              blockPoints: Math.max(0, Math.min(10, blockPoints)), // 0-10の範囲に制限
              commandPoints: Math.max(0, Math.min(20, commandPoints)), // 0-20の範囲に制限
              collabPoints: Math.max(0, Math.min(50, collabPoints)) // 0-50の範囲に制限
          });
      }
      
      // 生徒の進捗をリセット
      resetStudentProgress(args) {
          const student = this.validateString(args.STUDENT, '');
          if (!student) return;
          
          this.sendCommand('resetStudentProgress', { student: student });
      }
      
      // === 多言語サポート機能 ===
      
      // 言語を設定
      setLanguage(args) {
          const language = this.validateString(args.LANGUAGE, 'ja_JP');
          this.sendCommand('setLanguage', { language: language });
      }
      
      // 現在の言語を取得
      getLanguage() {
          this.sendCommand('getLanguage');
          return 'ja_JP'; // デフォルト値（実際の値はサーバーから返される）
      }
      
      // 対応言語一覧を表示
      listLanguages() {
          this.sendCommand('listLanguages');
      }
      
      // デフォルト言語を設定（教師のみ）
      setDefaultLanguage(args) {
          const language = this.validateString(args.LANGUAGE, 'ja_JP');
          this.sendCommand('setDefaultLanguage', { language: language });
      }
      
      // === Block Pack Management ===
      
      // ブロックパックを適用
      applyBlockPack(args) {
          const packId = this.validateString(args.PACK_ID, 'basic');
          this.sendCommand('applyBlockPack', { packId: packId });
      }
      
      // 利用可能なブロックパック一覧を取得
      getAvailableBlockPacks() {
          this.sendCommand('getAvailableBlockPacks');
          return 'basic,基本ブロック,基本,初心者,10'; // デフォルト値（実際の値はサーバーから返される）
      }
      
      // 現在のブロックパックを取得
      getCurrentBlockPack() {
          this.sendCommand('getCurrentBlockPack');
          return 'basic,基本ブロック,基本,初心者,10'; // デフォルト値（実際の値はサーバーから返される）
      }
      
      // ブロックパック情報を取得
      getBlockPackInfo(args) {
          const packId = this.validateString(args.PACK_ID, 'basic');
          this.sendCommand('getBlockPackInfo', { packId: packId });
          return 'Pack: 基本ブロック\nDescription: 最も基本的な建築ブロックのセットです。'; // デフォルト値
      }
      
      // カスタムブロックパックを作成（教師のみ）
      createCustomBlockPack(args) {
          const packId = this.validateString(args.PACK_ID, 'custom1');
          const packName = this.validateString(args.PACK_NAME, 'Custom Pack');
          const description = this.validateString(args.DESCRIPTION, 'A custom block pack');
          const blocks = this.validateString(args.BLOCKS, 'stone,dirt,grass_block');
          
          this.sendCommand('createCustomBlockPack', { 
              packId: packId, 
              packName: packName, 
              description: description, 
              blocks: blocks 
          });
      }
      
      // === Offline Mode Management ===
      
      // オフラインモードを設定（教師のみ）
      setOfflineMode(args) {
          const enabled = this.validateString(args.ENABLED, 'true');
          this.sendCommand('setOfflineMode', { enabled: enabled });
      }
      
      // オフライン状態を取得
      getOfflineStatus() {
          this.sendCommand('getOfflineStatus');
          return 'Offline Mode: false|Pending Actions: 0|Cached Students: 0'; // デフォルト値
      }
      
      // オフラインデータを同期（教師のみ）
      syncOfflineData() {
          this.sendCommand('syncOfflineData');
      }
      
      // オフラインデータをエクスポート（教師のみ）
      exportOfflineData() {
          this.sendCommand('exportOfflineData');
      }
      
      // 自動同期を設定（教師のみ）
      setAutoSync(args) {
          const enabled = this.validateString(args.ENABLED, 'true');
          this.sendCommand('setAutoSync', { enabled: enabled });
      }
      
      // Validation helpers
      validateNumber(value, defaultValue) {
          const num = Number(value);
          return isNaN(num) ? defaultValue : num;
      }
      
      validateString(value, defaultValue) {
          return value != null ? String(value) : defaultValue;
      }

      // スーパーフラット用Y座標調整（Minecraft 1.18+対応）
      adjustYForSuperflat(y) {
          // Minecraft 1.18+では、スーパーフラットの地表はY=-60
          // ユーザーが0を入力したら地表（Y=-60）になる
          return y - 60;
      }
      
      validateCoordinate(value, defaultValue, isY = false) {
          const num = this.validateNumber(value, defaultValue);
          // Basic bounds checking
          if (isY) {
              return Math.max(-64, Math.min(320, num));
          } else {
              return Math.max(-30000000, Math.min(30000000, num));
          }
      }
  }

  module.exports = MinecraftExtension;