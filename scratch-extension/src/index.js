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
      }

      getInfo() {
          return {
              id: 'minecraft',
              name: '🎮 Minecraft Controller',
              color1: '#4CAF50',
              color2: '#388E3C',
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
                          BLOCK: { type: ArgumentType.STRING, defaultValue: 'stone' },
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
                          BLOCK: { type: ArgumentType.STRING, defaultValue: 'stone' },
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          RADIUS: { type: ArgumentType.NUMBER, defaultValue: 5 }
                      }
                  },
                  {
                      opcode: 'buildSphere',
                      blockType: BlockType.COMMAND,
                      text: '🌕 [BLOCK]で 中心X:[X] Y:[Y] Z:[Z] 半径:[RADIUS] の球を作る',
                      arguments: {
                          BLOCK: { type: ArgumentType.STRING, defaultValue: 'stone' },
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          RADIUS: { type: ArgumentType.NUMBER, defaultValue: 5 }
                      }
                  },
                  {
                      opcode: 'buildWall',
                      blockType: BlockType.COMMAND,
                      text: '🧱 [BLOCK]で X1:[X1] Z1:[Z1] から X2:[X2] Z2:[Z2] 高さ:[HEIGHT] の壁を作る',
                      arguments: {
                          BLOCK: { type: ArgumentType.STRING, defaultValue: 'stone' },
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
                          BLOCK: { type: ArgumentType.STRING, defaultValue: 'oak_planks' },
                          X: { type: ArgumentType.NUMBER, defaultValue: 0 },
                          Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
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
                          Y: { type: ArgumentType.NUMBER, defaultValue: 64 },
                          Z: { type: ArgumentType.NUMBER, defaultValue: 0 }
                      }
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
          const y = this.validateNumber(args.Y, 64);
          const z = this.validateNumber(args.Z, 0);
          const block = this.validateString(args.BLOCK, 'stone');
          
          this.sendCommand('placeBlock', {
              block: block,
              x: x,
              y: y,
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
          return this.playerPos.y;
      }

      getPlayerZ() {
          this.sendCommand('getPlayerPos');
          return this.playerPos.z;
      }

      // ブロック破壊
      removeBlock(args) {
          this.sendCommand('removeBlock', {
              x: args.X,
              y: args.Y,
              z: args.Z
          });
      }

      // ブロック情報取得
      getBlockType(args) {
          this.sendCommand('getBlock', {
              x: args.X,
              y: args.Y,
              z: args.Z
          });
          return this.lastBlockInfo;
      }

      // 範囲埋め
      fillBlocks(args) {
          this.sendCommand('fill', {
              block: args.BLOCK,
              x1: args.X1,
              y1: args.Y1,
              z1: args.Z1,
              x2: args.X2,
              y2: args.Y2,
              z2: args.Z2
          });
      }

      // 範囲クリア
      clearArea(args) {
          this.sendCommand('fill', {
              block: 'air',
              x1: args.X1,
              y1: args.Y1,
              z1: args.Z1,
              x2: args.X2,
              y2: args.Y2,
              z2: args.Z2
          });
      }

      // 円形建築
      buildCircle(args) {
          this.sendCommand('buildCircle', {
              block: args.BLOCK,
              x: args.X,
              y: args.Y,
              z: args.Z,
              radius: args.RADIUS
          });
      }

      // 球形建築
      buildSphere(args) {
          this.sendCommand('buildSphere', {
              block: args.BLOCK,
              x: args.X,
              y: args.Y,
              z: args.Z,
              radius: args.RADIUS
          });
      }

      // 壁建築
      buildWall(args) {
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
          this.sendCommand('buildHouse', {
              block: args.BLOCK,
              x: args.X,
              y: args.Y,
              z: args.Z,
              width: args.WIDTH,
              depth: args.DEPTH,
              height: args.HEIGHT
          });
      }

      // プレイヤーテレポート
      teleportPlayer(args) {
          this.sendCommand('teleport', {
              x: args.X,
              y: args.Y,
              z: args.Z
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
          // TODO: 実際の招待数を返すようにする
          return 0;
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
          // TODO: 実際のワールド名を返すようにする
          return 'my_world';
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
      
      // Validation helpers
      validateNumber(value, defaultValue) {
          const num = Number(value);
          return isNaN(num) ? defaultValue : num;
      }
      
      validateString(value, defaultValue) {
          return value != null ? String(value) : defaultValue;
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