# エージェント・アスレチックシステム設計書

Scratchでプログラムしたエージェントが、マインクラフト内のアスレチックコースを自動で移動するシステムの設計書です。

## プロジェクト概要

### 目標
- Scratchでエージェントの移動アルゴリズムをプログラミング
- マインクラフトに作成したアスレチックコースをエージェントが自動走破
- ジャンプ、左右移動、待機などの動作を組み合わせて複雑な動きを実現

### ユースケース
1. **スピードラン**: エージェントがコースを最速でクリアするようプログラム
2. **障害物回避**: ブロックや溶岩を避けながら進む
3. **パルクール**: タイミングを計算してジャンプ
4. **複数エージェント競争**: 2体以上のエージェントでレース

---

## システム設計

### アーキテクチャ

```
[Scratch スクリプト]
    ↓ WebSocket (port 14711)
[Minecraft MOD]
    ↓ Forge API
[エージェントエンティティ]
    ↓ 物理演算
[マインクラフトワールド]
```

### 実装方式の比較

| 方式 | 実装時間 | 物理演算 | 見た目 | 推奨度 |
|------|---------|---------|--------|--------|
| A. ブロック表現 | 即座 | なし | ★★☆☆☆ | プロトタイプ用 |
| B. 既存Mob利用 | 数日 | あり | ★★★☆☆ | 推奨（短期） |
| C. カスタムエンティティ | 数週間 | あり | ★★★★★ | 推奨（長期） |

---

## 方式A: ブロック表現エージェント【即座に実装可能】

### 概要
エージェントを「移動するブロック」として表現。MOD改造不要で、今すぐ試せます。

### 特徴
- ✅ MOD改造不要
- ✅ 今すぐ実装可能
- ✅ Scratchだけで完結
- ❌ 物理演算なし（落下しない）
- ❌ 見た目が地味

### Scratchスクリプト例

```scratch
// 変数定義
変数 [agentX v] を [0] にする
変数 [agentY v] を [10] にする
変数 [agentZ v] を [0] にする
変数 [agentDirection v] を [0] にする  // 0=北, 1=東, 2=南, 3=西

// エージェント表示用ブロック
変数 [agentBlockType v] を [emerald_block] にする

// ===== カスタムブロック：エージェント初期化 =====
定義 エージェント初期化 (x) (y) (z)
  agentX を (x) にする
  agentY を (y) にする
  agentZ を (z) にする
  agentDirection を [0] にする
  エージェント描画 ▶

// ===== カスタムブロック：エージェント描画 =====
定義 エージェント描画
  minecraft: ブロックを置く
    x: (agentX)
    y: (agentY)
    z: (agentZ)
    ブロック: (agentBlockType)

// ===== カスタムブロック：エージェント消去 =====
定義 エージェント消去
  minecraft: ブロックを置く
    x: (agentX)
    y: (agentY)
    z: (agentZ)
    ブロック: [air]

// ===== カスタムブロック：前に移動 =====
定義 前に移動 (距離)
  エージェント消去 ▶

  もし (agentDirection) = [0] なら  // 北（-Z方向）
    agentZ を (距離 * -1) ずつ変える
  end

  もし (agentDirection) = [1] なら  // 東（+X方向）
    agentX を (距離) ずつ変える
  end

  もし (agentDirection) = [2] なら  // 南（+Z方向）
    agentZ を (距離) ずつ変える
  end

  もし (agentDirection) = [3] なら  // 西（-X方向）
    agentX を (距離 * -1) ずつ変える
  end

  エージェント描画 ▶

// ===== カスタムブロック：ジャンプ =====
定義 ジャンプ
  エージェント消去 ▶
  agentY を [1] ずつ変える
  エージェント描画 ▶
  0.2 秒待つ

  エージェント消去 ▶
  agentY を [1] ずつ変える
  エージェント描画 ▶
  0.2 秒待つ

  // 着地
  エージェント消去 ▶
  agentY を [-1] ずつ変える
  エージェント描画 ▶
  0.2 秒待つ

  エージェント消去 ▶
  agentY を [-1] ずつ変える
  エージェント描画 ▶

// ===== カスタムブロック：右を向く =====
定義 右を向く
  agentDirection を [1] ずつ変える
  もし (agentDirection) > [3] なら
    agentDirection を [0] にする
  end

// ===== カスタムブロック：左を向く =====
定義 左を向く
  agentDirection を [-1] ずつ変える
  もし (agentDirection) < [0] なら
    agentDirection を [3] にする
  end

// ===== アスレチックコース実行例 =====
緑の旗がクリックされたとき

minecraft: チャット メッセージ: [エージェントスタート！]

エージェント初期化 (0) (10) (0) ▶

// コース1: 直進してジャンプ
前に移動 (3) ▶
0.5 秒待つ
ジャンプ ▶
前に移動 (2) ▶

// コース2: 右に曲がって進む
右を向く ▶
0.3 秒待つ
前に移動 (4) ▶

// コース3: ジャンプして前進
ジャンプ ▶
前に移動 (1) ▶

// コース4: 左に曲がって進む
左を向く ▶
0.3 秒待つ
前に移動 (5) ▶

minecraft: チャット メッセージ: [ゴール！]
```

### アスレチックコースの例

```
スタート地点 (0, 10, 0)
  ↓
直進 3マス (stone)
  ↓
ジャンプ台 (1マス高い)
  ↓
着地点 (stone)
  ↓
右折
  ↓
直進 4マス
  ↓
大ジャンプ (2マス高い)
  ↓
左折
  ↓
ゴール地点 (gold_block)
```

---

## 方式B: 既存Mob利用エージェント【推奨・短期実装】

### 概要
村人やアイアンゴーレムをエージェントとして利用し、MODから制御します。

### 特徴
- ✅ 物理演算あり（重力、衝突判定）
- ✅ 見た目が良い
- ✅ 比較的簡単に実装
- ✅ 1週間程度で実装可能
- ❌ 既存Mobの制約あり

### MOD実装（Java）

```java
// AgentController.java
package com.github.minecraftedu.agent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.Vec3;

public class AgentController {
    private static IronGolem agent;

    // エージェント召喚
    public static void spawnAgent(ServerPlayer player, double x, double y, double z) {
        if (agent != null) {
            agent.discard();
        }

        agent = new IronGolem(EntityType.IRON_GOLEM, player.level());
        agent.setPos(x, y, z);
        agent.setCustomName(Component.literal("エージェント"));
        agent.setCustomNameVisible(true);

        // AIをクリア（自動行動を停止）
        agent.goalSelector.removeAllGoals();
        agent.targetSelector.removeAllGoals();

        player.level().addFreshEntity(agent);
    }

    // 前進
    public static void moveForward(double distance) {
        if (agent == null) return;

        Vec3 lookVec = agent.getLookAngle();
        Vec3 targetPos = agent.position().add(
            lookVec.x * distance,
            0,
            lookVec.z * distance
        );

        agent.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
    }

    // ジャンプ
    public static void jump() {
        if (agent == null) return;

        Vec3 motion = agent.getDeltaMovement();
        agent.setDeltaMovement(motion.x, 0.5, motion.z);  // Y方向に速度を与える
    }

    // 回転
    public static void turn(float angle) {
        if (agent == null) return;

        agent.setYRot(agent.getYRot() + angle);
        agent.setYHeadRot(agent.getYHeadRot() + angle);
    }

    // テレポート（即座に移動）
    public static void teleport(double x, double y, double z) {
        if (agent == null) return;
        agent.teleportTo(x, y, z);
    }

    // エージェント削除
    public static void removeAgent() {
        if (agent != null) {
            agent.discard();
            agent = null;
        }
    }

    // 位置取得
    public static Vec3 getPosition() {
        if (agent == null) return Vec3.ZERO;
        return agent.position();
    }
}
```

### WebSocketコマンド追加

```java
// WebSocketCommandHandler.java に追加

case "agentSpawn":
    double x = json.getDouble("x");
    double y = json.getDouble("y");
    double z = json.getDouble("z");
    AgentController.spawnAgent(player, x, y, z);
    sendResponse(ws, "success", "エージェント召喚成功");
    break;

case "agentMove":
    double distance = json.getDouble("distance");
    AgentController.moveForward(distance);
    sendResponse(ws, "success", "移動開始");
    break;

case "agentJump":
    AgentController.jump();
    sendResponse(ws, "success", "ジャンプ");
    break;

case "agentTurn":
    float angle = (float)json.getDouble("angle");
    AgentController.turn(angle);
    sendResponse(ws, "success", "回転");
    break;

case "agentRemove":
    AgentController.removeAgent();
    sendResponse(ws, "success", "エージェント削除");
    break;

case "agentGetPosition":
    Vec3 pos = AgentController.getPosition();
    JSONObject response = new JSONObject();
    response.put("x", pos.x);
    response.put("y", pos.y);
    response.put("z", pos.z);
    sendResponse(ws, "position", response.toString());
    break;
```

### Scratch拡張ブロック（JavaScript）

```javascript
// gui.js の MINECRAFT_BLOCKS に追加

{
    opcode: 'minecraft_agentSpawn',
    blockType: Scratch.BlockType.COMMAND,
    text: 'エージェント召喚 x:[X] y:[Y] z:[Z]',
    arguments: {
        X: { type: Scratch.ArgumentType.NUMBER, defaultValue: 0 },
        Y: { type: Scratch.ArgumentType.NUMBER, defaultValue: 10 },
        Z: { type: Scratch.ArgumentType.NUMBER, defaultValue: 0 }
    }
},

{
    opcode: 'minecraft_agentMove',
    blockType: Scratch.BlockType.COMMAND,
    text: 'エージェント [DISTANCE] マス前進',
    arguments: {
        DISTANCE: { type: Scratch.ArgumentType.NUMBER, defaultValue: 1 }
    }
},

{
    opcode: 'minecraft_agentJump',
    blockType: Scratch.BlockType.COMMAND,
    text: 'エージェント ジャンプ'
},

{
    opcode: 'minecraft_agentTurn',
    blockType: Scratch.BlockType.COMMAND,
    text: 'エージェント [DIRECTION] を向く',
    arguments: {
        DIRECTION: {
            type: Scratch.ArgumentType.STRING,
            menu: 'agentDirections',
            defaultValue: '右'
        }
    }
},

{
    opcode: 'minecraft_agentRemove',
    blockType: Scratch.BlockType.COMMAND,
    text: 'エージェント削除'
},

{
    opcode: 'minecraft_agentGetX',
    blockType: Scratch.BlockType.REPORTER,
    text: 'エージェントのX座標'
},

{
    opcode: 'minecraft_agentGetY',
    blockType: Scratch.BlockType.REPORTER,
    text: 'エージェントのY座標'
},

{
    opcode: 'minecraft_agentGetZ',
    blockType: Scratch.BlockType.REPORTER,
    text: 'エージェントのZ座標'
}
```

### 実装ハンドラー

```javascript
minecraft_agentSpawn(args) {
    const message = {
        command: 'agentSpawn',
        x: Number(args.X),
        y: Number(args.Y),
        z: Number(args.Z)
    };
    this.sendCommand(message);
},

minecraft_agentMove(args) {
    const message = {
        command: 'agentMove',
        distance: Number(args.DISTANCE)
    };
    this.sendCommand(message);
},

minecraft_agentJump(args) {
    const message = { command: 'agentJump' };
    this.sendCommand(message);
},

minecraft_agentTurn(args) {
    const angles = {
        '右': 90,
        '左': -90,
        '後ろ': 180
    };
    const message = {
        command: 'agentTurn',
        angle: angles[args.DIRECTION] || 0
    };
    this.sendCommand(message);
}
```

### Scratchでのアスレチックスクリプト

```scratch
緑の旗がクリックされたとき

minecraft: チャット メッセージ: [アスレチック開始！]

// エージェント召喚
minecraft: エージェント召喚 x:[0] y:[10] z:[0]

1 秒待つ

// ステージ1: 直進
minecraft: エージェント [3] マス前進
2 秒待つ

// ステージ2: ジャンプ
minecraft: エージェント ジャンプ
0.5 秒待つ
minecraft: エージェント [1] マス前進
2 秒待つ

// ステージ3: 右折
minecraft: エージェント [右 v] を向く
1 秒待つ

// ステージ4: 長距離走
minecraft: エージェント [5] マス前進
3 秒待つ

// ステージ5: 連続ジャンプ
3 回繰り返す
  minecraft: エージェント ジャンプ
  0.3 秒待つ
  minecraft: エージェント [1] マス前進
  1.5 秒待つ
end

minecraft: チャット メッセージ: [ゴール到達！]
```

---

## アスレチックコースの設計例

### コース1: 初級パルクール

```
座標 (0, 10, 0) スタート地点（stone）
   ↓ 3マス直進
座標 (0, 10, 3) 踏み台（stone）
   ↓ ジャンプ
座標 (0, 11, 4) 高台（stone）
   ↓ 2マス直進
座標 (0, 11, 6) 踏み台（stone）
   ↓ 右折 + 4マス直進
座標 (4, 11, 6) 中間地点（gold_block）
   ↓ ジャンプ + 1マス前進
座標 (5, 12, 6) 高台（emerald_block）
   ↓ 左折 + 3マス直進
座標 (5, 12, 3) ゴール（diamond_block）
```

### コース2: 上級タイミングジャンプ

```
スタート
   ↓
溶岩の上を飛び越える（2マスジャンプ）
   ↓
細い道（1マス幅）を進む
   ↓
連続ジャンプ（3回）
   ↓
回転ジャンプ（右に90度回転しながらジャンプ）
   ↓
ゴール
```

### Scratchスクリプト（上級コース）

```scratch
// 上級コース実行
定義 上級コース実行

minecraft: エージェント召喚 x:[0] y:[10] z:[0]
1 秒待つ

// ステージ1: 溶岩ジャンプ
minecraft: エージェント [2] マス前進
2 秒待つ
minecraft: エージェント ジャンプ
0.2 秒待つ
minecraft: エージェント [2] マス前進
3 秒待つ

// ステージ2: 細い道
5 回繰り返す
  minecraft: エージェント [1] マス前進
  0.8 秒待つ
end

// ステージ3: 連続ジャンプ
3 回繰り返す
  minecraft: エージェント ジャンプ
  0.3 秒待つ
  minecraft: エージェント [1] マス前進
  1.5 秒待つ
end

// ステージ4: 回転ジャンプ
minecraft: エージェント ジャンプ
0.2 秒待つ
minecraft: エージェント [右 v] を向く
0.3 秒待つ
minecraft: エージェント [1] マス前進
2 秒待つ

minecraft: チャット メッセージ: [上級コースクリア！]
```

---

## 高度な機能

### 1. 障害物検知

```java
// AgentController.java に追加
public static boolean canMoveForward(double distance) {
    if (agent == null) return false;

    Vec3 lookVec = agent.getLookAngle();
    Vec3 targetPos = agent.position().add(
        lookVec.x * distance,
        0,
        lookVec.z * distance
    );

    BlockPos blockPos = new BlockPos(
        (int)targetPos.x,
        (int)targetPos.y,
        (int)targetPos.z
    );

    return agent.level().isEmptyBlock(blockPos);
}
```

Scratch側:
```scratch
もし <minecraft: エージェント前方に障害物？> なら
  minecraft: エージェント ジャンプ
  1 秒待つ
end
minecraft: エージェント [1] マス前進
```

### 2. 自動経路探索

```scratch
定義 ゴールまで自動移動

ずっと
  もし (エージェントのX座標) < (ゴールX) なら
    minecraft: エージェント [右 v] を向く
    minecraft: エージェント [1] マス前進
  end

  もし (エージェントのZ座標) < (ゴールZ) なら
    minecraft: エージェント [1] マス前進
  end

  もし <minecraft: エージェント前方に障害物？> なら
    minecraft: エージェント ジャンプ
  end

  // ゴール判定
  もし ((エージェントのX座標) = (ゴールX)) かつ ((エージェントのZ座標) = (ゴールZ)) なら
    minecraft: チャット メッセージ: [ゴール到達！]
    このスクリプトを止める
  end

  0.5 秒待つ
end
```

### 3. タイムアタック

```scratch
変数 [スタート時刻] を作る
変数 [ゴール時刻] を作る
変数 [タイム] を作る

緑の旗がクリックされたとき

スタート時刻 を (タイマー) にする
タイマーをリセット

// コース実行...

ゴール時刻 を (タイマー) にする
タイム を (ゴール時刻) - (スタート時刻) にする

minecraft: チャット メッセージ: (クリアタイム: () 秒) と (タイム) を組み合わせる
```

---

## 実装ロードマップ

### フェーズ1: プロトタイプ（今すぐ）
- 方式A（ブロック表現）で実装
- 基本的な移動・ジャンプを試す
- シンプルなアスレチックコースでテスト

### フェーズ2: MOD拡張（1週間）
- AgentController.java を実装
- WebSocketコマンド追加
- 方式B（既存Mob利用）で実装

### フェーズ3: Scratch拡張（3日）
- gui.js にエージェントブロック追加
- ビルドしてテスト

### フェーズ4: 高度な機能（1週間）
- 障害物検知
- 自動経路探索
- タイムアタック機能

---

## まとめ

**今すぐ試せる方法:**
1. `sokoban_working.sb3` を開く
2. 上記の「方式A」のスクリプトを追加
3. マインクラフトでアスレチックコースを建築
4. 緑の旗をクリック！

**本格実装なら:**
- MODを拡張して方式Bを実装
- 1-2週間で完成
- 物理演算とAIで本格的なエージェント

どちらの方式で進めますか？プロトタイプから始めるのがおすすめです！
