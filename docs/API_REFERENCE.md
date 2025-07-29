# 📖 Minecraft協調学習システム APIリファレンス

## 📋 概要

このドキュメントは、Minecraft協調学習システムのWebSocket APIの完全なリファレンスです。ScratchとMinecraft間の通信プロトコルについて説明します。

---

## 🔌 接続情報

### WebSocketエンドポイント
```
ws://localhost:14711
```

### メッセージ形式
システムは2つのメッセージ形式をサポートしています：

#### 1. JSON形式（推奨）
```json
{
  "command": "commandName",
  "args": {
    "param1": "value1",
    "param2": "value2"
  }
}
```

#### 2. レガシー形式
```
commandName(arg1,arg2,arg3)
```

---

## 📡 API コマンド一覧

### 🔌 接続管理

#### minecraft.connect
WebSocketサーバーへの接続を確立します。
```json
{
  "command": "connect",
  "args": {}
}
```

**レスポンス:**
```json
{
  "type": "welcome",
  "message": "Connected to Minecraft Collaboration Server"
}
```

---

### 🧱 ブロック操作

#### placeBlock
指定座標にブロックを設置します。

**リクエスト:**
```json
{
  "command": "placeBlock",
  "args": {
    "x": "10",
    "y": "64",
    "z": "10",
    "block": "stone"
  }
}
```

**レスポンス:**
```json
{
  "type": "placeBlock",
  "status": "success",
  "message": "Placed stone at 10,64,10"
}
```

#### removeBlock
指定座標のブロックを破壊します。

**リクエスト:**
```json
{
  "command": "removeBlock",
  "args": {
    "x": "10",
    "y": "64",
    "z": "10"
  }
}
```

#### getBlock
指定座標のブロック情報を取得します。

**リクエスト:**
```json
{
  "command": "getBlock",
  "args": {
    "x": "10",
    "y": "64",
    "z": "10"
  }
}
```

**レスポンス:**
```json
{
  "type": "blockInfo",
  "data": "minecraft:stone"
}
```

---

### 📍 プレイヤー情報

#### getPlayerPos
プレイヤーの現在位置を取得します。

**リクエスト:**
```json
{
  "command": "getPlayerPos",
  "args": {}
}
```

**レスポンス:**
```json
{
  "type": "playerPos",
  "data": {
    "x": 100.5,
    "y": 64.0,
    "z": -200.5
  }
}
```

#### teleport
プレイヤーを指定座標にテレポートします。

**リクエスト:**
```json
{
  "command": "teleport",
  "args": {
    "x": "100",
    "y": "70",
    "z": "100"
  }
}
```

---

### 🏗️ 建築支援

#### fill
指定範囲をブロックで埋めます。

**リクエスト:**
```json
{
  "command": "fill",
  "args": {
    "x1": "0", "y1": "64", "z1": "0",
    "x2": "10", "y2": "64", "z2": "10",
    "block": "grass_block"
  }
}
```

#### buildCircle
円を建築します。

**リクエスト:**
```json
{
  "command": "buildCircle",
  "args": {
    "x": "0",
    "y": "64",
    "z": "0",
    "radius": "5",
    "block": "stone"
  }
}
```

#### buildSphere
球体を建築します。

**リクエスト:**
```json
{
  "command": "buildSphere",
  "args": {
    "x": "0",
    "y": "70",
    "z": "0",
    "radius": "5",
    "block": "glass"
  }
}
```

#### buildWall
壁を建築します。

**リクエスト:**
```json
{
  "command": "buildWall",
  "args": {
    "x1": "0",
    "z1": "0",
    "x2": "10",
    "z2": "0",
    "height": "5",
    "block": "stone_bricks"
  }
}
```

#### buildHouse
簡単な家を建築します。

**リクエスト:**
```json
{
  "command": "buildHouse",
  "args": {
    "x": "0",
    "y": "64",
    "z": "0",
    "width": "7",
    "depth": "7",
    "height": "4",
    "block": "oak_planks"
  }
}
```

---

### 🎮 ゲーム設定

#### gamemode
ゲームモードを変更します。

**リクエスト:**
```json
{
  "command": "gamemode",
  "args": {
    "mode": "creative"
  }
}
```

**値:** `survival`, `creative`, `adventure`, `spectator`

#### time
ゲーム内時間を設定します。

**リクエスト:**
```json
{
  "command": "time",
  "args": {
    "time": "day"
  }
}
```

**値:** `day`, `night`, `noon`, `midnight`, `sunrise`, `sunset`

#### weather
天候を設定します。

**リクエスト:**
```json
{
  "command": "weather",
  "args": {
    "weather": "clear"
  }
}
```

**値:** `clear`, `rain`, `thunder`

---

### 💬 コミュニケーション

#### chat
チャットメッセージを送信します。

**リクエスト:**
```json
{
  "command": "chat",
  "args": {
    "message": "Hello Minecraft!"
  }
}
```

---

### 🤝 協調機能

#### collaboration.invite (レガシー形式)
友達を招待します。

**リクエスト:**
```
collaboration.invite(FriendName)
```

#### getInvitations
招待数を取得します。

**リクエスト:**
```json
{
  "command": "getInvitations",
  "args": {}
}
```

**レスポンス:**
```json
{
  "type": "invitations",
  "data": {
    "count": 2
  }
}
```

#### collaboration.requestVisit (レガシー形式)
友達のワールドへの訪問を申請します。

**リクエスト:**
```
collaboration.requestVisit(FriendName)
```

#### collaboration.approveVisit (レガシー形式)
訪問申請を承認します。

**リクエスト:**
```
collaboration.approveVisit(VisitorName)
```

#### getCurrentWorld
現在のワールド名を取得します。

**リクエスト:**
```json
{
  "command": "getCurrentWorld",
  "args": {}
}
```

**レスポンス:**
```json
{
  "type": "currentWorld",
  "data": {
    "world": "minecraft:overworld"
  }
}
```

#### collaboration.returnHome (レガシー形式)
自分のワールドに戻ります。

**リクエスト:**
```
collaboration.returnHome()
```

#### collaboration.emergencyReturn (レガシー形式)
緊急帰宅（体力・空腹度回復付き）します。

**リクエスト:**
```
collaboration.emergencyReturn()
```

---

### 🤖 エージェントシステム

#### summonAgent
エージェントを召喚します。

**リクエスト:**
```json
{
  "command": "summonAgent",
  "args": {
    "name": "MyAgent"
  }
}
```

#### moveAgent
エージェントを移動させます。

**方向指定:**
```json
{
  "command": "moveAgent",
  "args": {
    "direction": "forward",
    "distance": "5"
  }
}
```

**座標指定:**
```json
{
  "command": "moveAgent",
  "args": {
    "x": "100",
    "y": "64",
    "z": "100"
  }
}
```

#### agentFollow
エージェントのフォロー設定を変更します。

**リクエスト:**
```json
{
  "command": "agentFollow",
  "args": {
    "follow": "true"
  }
}
```

#### agentAction
エージェントにアクションを実行させます。

**リクエスト:**
```json
{
  "command": "agentAction",
  "args": {
    "action": "jump"
  }
}
```

**アクション:** `jump`, `spin`, `dance`

#### dismissAgent
エージェントを解散させます。

**リクエスト:**
```json
{
  "command": "dismissAgent",
  "args": {}
}
```

---

## 📨 レスポンス形式

### 成功レスポンス
```json
{
  "type": "commandType",
  "status": "success",
  "message": "Operation completed successfully"
}
```

### エラーレスポンス
```json
{
  "type": "error",
  "error": "errorCode",
  "message": "Error description"
}
```

### エラーコード
- `emptyMessage`: 空のメッセージ
- `invalidJson`: 無効なJSON形式
- `unknownCommand`: 不明なコマンド
- `invalidParams`: 無効なパラメータ
- `notFound`: リソースが見つからない
- `forbidden`: アクセス拒否
- `internal`: 内部エラー
- `rateLimitExceeded`: レート制限超過

---

## 🔒 セキュリティと制限

### レート制限
- **制限:** 10コマンド/秒
- **対象:** 接続ごと
- **超過時:** `rateLimitExceeded`エラー

### ネットワーク制限
- ローカルネットワークのみ接続可能
- 許可されたIPレンジ:
  - `localhost` / `127.0.0.1`
  - `192.168.*.*`
  - `10.*.*.*`
  - `172.16.*.*` - `172.31.*.*`

### ブロック制限
以下のブロックは教育環境の安全のため使用できません：
- `tnt`
- `end_crystal`
- `respawn_anchor`
- `bed` (ネザー/エンドでの爆発防止)

### コマンド制限
以下のコマンドはブロックされています：
- `op`, `deop`
- `stop`
- `kick`, `ban`
- `save-all`, `save-on`, `save-off`

---

## 📝 使用例

### Node.js (ws library)
```javascript
const WebSocket = require('ws');

const ws = new WebSocket('ws://localhost:14711');

ws.on('open', () => {
    console.log('Connected to Minecraft');
    
    // プレイヤー位置を取得
    ws.send(JSON.stringify({
        command: 'getPlayerPos',
        args: {}
    }));
});

ws.on('message', (data) => {
    const response = JSON.parse(data);
    console.log('Received:', response);
});
```

### ブラウザ JavaScript
```javascript
const ws = new WebSocket('ws://localhost:14711');

ws.onopen = () => {
    // ブロックを設置
    ws.send(JSON.stringify({
        command: 'placeBlock',
        args: {
            x: '10',
            y: '64',
            z: '10',
            block: 'diamond_block'
        }
    }));
};

ws.onmessage = (event) => {
    const response = JSON.parse(event.data);
    console.log(response);
};
```

---

## 🔄 更新履歴

- **2025-01-26**: 初版作成
- **2025-01-26**: エージェントシステムAPI追加
- **2025-01-26**: セキュリティセクション追加

---

最終更新: 2025年7月26日