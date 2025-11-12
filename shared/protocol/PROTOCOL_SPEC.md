# 通信プロトコル仕様書

## バージョン情報

- **プロトコルバージョン**: 1.0
- **最終更新日**: 2025-11-12
- **互換性**: Minecraft 1.20.x

---

## 目次

1. [概要](#概要)
2. [接続方式](#接続方式)
3. [メッセージフォーマット](#メッセージフォーマット)
4. [WebSocketプロトコル](#websocketプロトコル)
5. [HTTPプロトコル](#httpプロトコル)
6. [コマンド仕様](#コマンド仕様)
7. [イベント仕様](#イベント仕様)
8. [エラーハンドリング](#エラーハンドリング)
9. [セキュリティ](#セキュリティ)

---

## 概要

### 通信モデル

```
[Scratch Client] <--WebSocket/HTTP--> [Minecraft Server + MOD]
```

- **WebSocket**: リアルタイム双方向通信（コマンド送信、イベント受信）
- **HTTP/REST**: 状態取得、認証、設定

### ポート

| プロトコル | デフォルトポート | 用途 |
|-----------|---------------|------|
| WebSocket | 14711 | メインコマンド・イベント |
| HTTP | 14712 | REST API |

---

## 接続方式

### WebSocket接続フロー

```
1. [Client] HTTP Upgrade Request
   → ws://localhost:14711/minecraft

2. [Server] 101 Switching Protocols

3. [Client] Connect Message
   {
     "type": "connect",
     "payload": { "clientId": "...", "authToken": "..." }
   }

4. [Server] Welcome Message
   {
     "type": "connect_response",
     "success": true,
     "payload": { "sessionId": "...", ... }
   }

5. [Client/Server] 双方向メッセージ交換

6. [Client/Server] Heartbeat (30秒ごと)

7. [Client] Disconnect / [Server] Close
```

### HTTP接続

- 標準的なRESTful APIパターン
- JSON形式のリクエスト/レスポンス
- 認証: Bearer Tokenまたはカスタムヘッダー

---

## メッセージフォーマット

### 基本構造（JSON）

```json
{
  "version": "1.0",
  "messageId": "uuid-v4",
  "timestamp": 1699876543210,
  "sessionId": "session-uuid",
  "type": "message_type",
  "payload": {}
}
```

### フィールド説明

| フィールド | 型 | 必須 | 説明 |
|----------|---|------|------|
| version | string | ✓ | プロトコルバージョン（例: "1.0"） |
| messageId | string | ✓ | 一意のメッセージID（UUID v4） |
| timestamp | long | ✓ | UNIXタイムスタンプ（ミリ秒） |
| sessionId | string | ✓ | セッションID（接続後に付与） |
| type | string | ✓ | メッセージタイプ |
| payload | object | ✓ | メッセージ本体 |

### メッセージタイプ

| タイプ | 方向 | 説明 |
|-------|------|------|
| `connect` | C→S | 接続リクエスト |
| `connect_response` | S→C | 接続レスポンス |
| `command` | C→S | コマンド送信 |
| `command_response` | S→C | コマンド実行結果 |
| `query` | C→S | 情報取得リクエスト |
| `query_response` | S→C | 情報取得レスポンス |
| `event` | S→C | サーバーイベント通知 |
| `heartbeat` | C↔S | 生存確認 |
| `error` | S→C | エラー通知 |
| `disconnect` | C→S | 切断通知 |

**C→S**: Client to Server
**S→C**: Server to Client
**C↔S**: 双方向

---

## WebSocketプロトコル

### 1. 接続（Connect）

#### リクエスト（Client → Server）

```json
{
  "version": "1.0",
  "messageId": "a1b2c3d4-...",
  "timestamp": 1699876543210,
  "sessionId": "",
  "type": "connect",
  "payload": {
    "clientId": "student_001",
    "authToken": "student-token-xyz789",
    "clientInfo": {
      "userAgent": "Scratch 3.0",
      "version": "1.0.0",
      "platform": "web"
    }
  }
}
```

#### レスポンス（Server → Client）

**成功時**:

```json
{
  "version": "1.0",
  "messageId": "b2c3d4e5-...",
  "timestamp": 1699876543500,
  "sessionId": "session-abc123",
  "type": "connect_response",
  "payload": {
    "success": true,
    "sessionId": "session-abc123",
    "clientName": "田中太郎",
    "role": "STUDENT_FULL",
    "permissions": ["CHAT", "PLACE_BLOCK", "SUMMON_ENTITY"],
    "serverInfo": {
      "version": "1.0.0",
      "minecraftVersion": "1.20.1",
      "maxClients": 10,
      "currentClients": 3
    }
  }
}
```

**失敗時**:

```json
{
  "version": "1.0",
  "messageId": "...",
  "timestamp": ...,
  "sessionId": "",
  "type": "connect_response",
  "payload": {
    "success": false,
    "errorCode": "AUTH_FAILED",
    "errorMessage": "認証に失敗しました"
  }
}
```

---

### 2. コマンド送信（Command）

#### リクエスト（Client → Server）

```json
{
  "version": "1.0",
  "messageId": "c3d4e5f6-...",
  "timestamp": 1699876550000,
  "sessionId": "session-abc123",
  "type": "command",
  "payload": {
    "action": "setBlock",
    "params": {
      "x": 100,
      "y": 64,
      "z": -50,
      "blockType": "minecraft:stone",
      "blockState": {}
    }
  }
}
```

#### レスポンス（Server → Client）

**成功時**:

```json
{
  "version": "1.0",
  "messageId": "d4e5f6g7-...",
  "timestamp": 1699876550100,
  "sessionId": "session-abc123",
  "type": "command_response",
  "payload": {
    "success": true,
    "requestMessageId": "c3d4e5f6-...",
    "action": "setBlock",
    "result": {
      "blockPlaced": true,
      "position": { "x": 100, "y": 64, "z": -50 }
    }
  }
}
```

**失敗時**:

```json
{
  "version": "1.0",
  "messageId": "...",
  "timestamp": ...,
  "sessionId": "session-abc123",
  "type": "command_response",
  "payload": {
    "success": false,
    "requestMessageId": "c3d4e5f6-...",
    "action": "setBlock",
    "errorCode": "PERMISSION_DENIED",
    "errorMessage": "ブロック配置の権限がありません"
  }
}
```

---

### 3. イベント通知（Event）

#### サーバーからクライアントへのプッシュ通知

```json
{
  "version": "1.0",
  "messageId": "e5f6g7h8-...",
  "timestamp": 1699876555000,
  "sessionId": "session-abc123",
  "type": "event",
  "payload": {
    "eventType": "tutorialStepCompleted",
    "data": {
      "tutorialId": "beginner_001",
      "stepId": 2,
      "pointsEarned": 20,
      "badgeEarned": "first_builder",
      "message": "やったね！初めてブロックを置きました！"
    }
  }
}
```

#### イベントタイプ一覧

| eventType | 説明 | データ例 |
|-----------|------|---------|
| `clientConnected` | 別クライアントが接続 | `{ "clientName": "..." }` |
| `clientDisconnected` | 別クライアントが切断 | `{ "clientName": "..." }` |
| `blockPlaced` | ブロックが配置された | `{ "position": {...}, "blockType": "..." }` |
| `entitySummoned` | エンティティが召喚された | `{ "entityType": "...", "position": {...} }` |
| `tutorialStepCompleted` | チュートリアルステップ完了 | `{ "tutorialId": "...", "stepId": ... }` |
| `challengeAssigned` | 課題が割り当てられた | `{ "challengeId": "...", "deadline": ... }` |
| `badgeAwarded` | バッジ獲得 | `{ "badgeId": "...", "badgeName": "..." }` |
| `conflictDetected` | 操作競合検出 | `{ "conflictType": "...", "resolution": "..." }` |

---

### 4. ハートビート（Heartbeat）

#### クライアントからサーバーへ（30秒ごと）

```json
{
  "version": "1.0",
  "messageId": "...",
  "timestamp": ...,
  "sessionId": "session-abc123",
  "type": "heartbeat",
  "payload": {}
}
```

#### サーバーからクライアントへ（応答）

```json
{
  "version": "1.0",
  "messageId": "...",
  "timestamp": ...,
  "sessionId": "session-abc123",
  "type": "heartbeat",
  "payload": {
    "serverTime": 1699876560000
  }
}
```

---

## HTTPプロトコル

### ベースURL

```
http://localhost:14712/api/v1
```

### 認証

**方法**: Bearer Token

```http
Authorization: Bearer <auth-token>
```

---

### エンドポイント一覧

#### 1. サーバーステータス取得

```http
GET /api/v1/status
```

**レスポンス**:

```json
{
  "status": "running",
  "version": "1.0.0",
  "minecraftVersion": "1.20.1",
  "uptime": 3600000,
  "activeClients": 3,
  "maxClients": 10
}
```

---

#### 2. クライアント接続認証

```http
POST /api/v1/auth/connect
Content-Type: application/json

{
  "clientId": "student_001",
  "authToken": "student-token-xyz789"
}
```

**レスポンス**:

```json
{
  "success": true,
  "sessionToken": "...",
  "expiresAt": 1699880143210
}
```

---

#### 3. セッション情報取得

```http
GET /api/v1/session/{sessionId}
Authorization: Bearer <token>
```

**レスポンス**:

```json
{
  "sessionId": "session-abc123",
  "clientId": "student_001",
  "clientName": "田中太郎",
  "role": "STUDENT_FULL",
  "permissions": ["CHAT", "PLACE_BLOCK"],
  "connectedAt": 1699876543210,
  "lastActivity": 1699876560000
}
```

---

#### 4. チュートリアル一覧取得

```http
GET /api/v1/tutorials
Authorization: Bearer <token>
```

**レスポンス**:

```json
{
  "tutorials": [
    {
      "tutorialId": "beginner_001",
      "title": "はじめてのプログラミング",
      "difficulty": "BEGINNER",
      "estimatedMinutes": 10,
      "completed": false,
      "canStart": true
    },
    ...
  ]
}
```

---

#### 5. 課題一覧取得

```http
GET /api/v1/challenges
Authorization: Bearer <token>
```

**レスポンス**:

```json
{
  "challenges": [
    {
      "challengeId": "build_001",
      "title": "5x5の家を作ろう",
      "type": "BUILD",
      "difficulty": "BEGINNER",
      "deadline": "2025-11-20T23:59:59Z",
      "status": "ASSIGNED"
    },
    ...
  ]
}
```

---

#### 6. 進捗情報取得

```http
GET /api/v1/progress/{userId}
Authorization: Bearer <token>
```

**レスポンス**:

```json
{
  "userId": "student_001",
  "totalPoints": 1250,
  "tutorialsCompleted": 5,
  "challengesCompleted": 3,
  "blocksPlaced": 250,
  "earnedBadges": [
    "connected",
    "first_chat",
    "first_builder"
  ],
  "lastActivity": "2025-11-12T10:30:00Z"
}
```

---

## コマンド仕様

### コマンド一覧

#### 1. チャット送信（chat）

```json
{
  "action": "chat",
  "params": {
    "message": "Hello, Minecraft!"
  }
}
```

---

#### 2. ブロック配置（setBlock）

```json
{
  "action": "setBlock",
  "params": {
    "x": 100,
    "y": 64,
    "z": -50,
    "blockType": "minecraft:stone",
    "blockState": {}
  }
}
```

**相対座標の場合**:

```json
{
  "action": "setBlock",
  "params": {
    "relativeX": 0,
    "relativeY": 1,
    "relativeZ": 2,
    "blockType": "minecraft:stone"
  }
}
```

---

#### 3. ブロック取得（getBlock）

```json
{
  "action": "getBlock",
  "params": {
    "x": 100,
    "y": 64,
    "z": -50
  }
}
```

**レスポンス**:

```json
{
  "success": true,
  "result": {
    "blockType": "minecraft:stone",
    "blockState": {},
    "position": { "x": 100, "y": 64, "z": -50 }
  }
}
```

---

#### 4. 範囲ブロック配置（fillBlocks）

```json
{
  "action": "fillBlocks",
  "params": {
    "from": { "x": 100, "y": 64, "z": -50 },
    "to": { "x": 105, "y": 68, "z": -45 },
    "blockType": "minecraft:stone"
  }
}
```

---

#### 5. エンティティ召喚（summonEntity）

```json
{
  "action": "summonEntity",
  "params": {
    "entityType": "minecraft:pig",
    "x": 100,
    "y": 64,
    "z": -50,
    "nbt": {}
  }
}
```

---

#### 6. テレポート（teleport）

```json
{
  "action": "teleport",
  "params": {
    "x": 100,
    "y": 64,
    "z": -50,
    "yaw": 0,
    "pitch": 0
  }
}
```

---

#### 7. プレイヤー位置取得（getPosition）

```json
{
  "action": "getPosition",
  "params": {}
}
```

**レスポンス**:

```json
{
  "success": true,
  "result": {
    "x": 100.5,
    "y": 64.0,
    "z": -50.3,
    "yaw": 45.2,
    "pitch": 10.5
  }
}
```

---

#### 8. 天気変更（setWeather）

```json
{
  "action": "setWeather",
  "params": {
    "weather": "clear" // clear | rain | thunder
  }
}
```

---

#### 9. 時刻変更（setTime）

```json
{
  "action": "setTime",
  "params": {
    "time": 1000 // 0-24000 (0=朝, 6000=昼, 18000=夜)
  }
}
```

---

#### 10. ゲームモード変更（setGameMode）

```json
{
  "action": "setGameMode",
  "params": {
    "mode": "creative" // survival | creative | adventure | spectator
  }
}
```

---

## エラーハンドリング

### エラーコード

| コード | 説明 | HTTPステータス |
|-------|------|---------------|
| `AUTH_FAILED` | 認証失敗 | 401 |
| `PERMISSION_DENIED` | 権限不足 | 403 |
| `SESSION_NOT_FOUND` | セッション不明 | 404 |
| `SESSION_EXPIRED` | セッション期限切れ | 401 |
| `INVALID_PARAMS` | パラメータ不正 | 400 |
| `COMMAND_FAILED` | コマンド実行失敗 | 500 |
| `SERVER_FULL` | サーバー満員 | 503 |
| `RATE_LIMIT_EXCEEDED` | レート制限超過 | 429 |
| `CONFLICT` | 操作競合 | 409 |
| `INTERNAL_ERROR` | 内部エラー | 500 |

### エラーレスポンス形式

```json
{
  "version": "1.0",
  "messageId": "...",
  "timestamp": ...,
  "sessionId": "...",
  "type": "error",
  "payload": {
    "errorCode": "PERMISSION_DENIED",
    "errorMessage": "ブロック配置の権限がありません",
    "details": {
      "requiredPermission": "PLACE_BLOCK",
      "currentRole": "OBSERVER"
    }
  }
}
```

---

## セキュリティ

### 1. 認証トークン

- **生成**: サーバー側でランダム生成
- **形式**: Base64エンコードされた128ビット値
- **有効期限**: デフォルト24時間
- **保存**: config.jsonまたはデータベース

### 2. レート制限

| 操作 | 制限 |
|------|------|
| コマンド送信 | 100/分（クライアントごと） |
| API呼び出し | 60/分（クライアントごと） |
| ハートビート | 2/分 |

### 3. 入力検証

- すべてのパラメータを検証
- SQLインジェクション対策
- コマンドインジェクション対策
- 座標範囲チェック（-30000000 ~ 30000000）

---

## バージョン互換性

### プロトコルバージョニング

```
major.minor
```

- **major**: 非互換な変更
- **minor**: 後方互換な変更

### バージョンネゴシエーション

クライアントは接続時にサポートするバージョンを送信：

```json
{
  "type": "connect",
  "payload": {
    "supportedVersions": ["1.0", "1.1"]
  }
}
```

サーバーは使用するバージョンを返す：

```json
{
  "type": "connect_response",
  "payload": {
    "protocolVersion": "1.0"
  }
}
```

---

**作成日**: 2025-11-12
**対象バージョン**: 1.0.0
**メンテナー**: MinecraftEdu Team
