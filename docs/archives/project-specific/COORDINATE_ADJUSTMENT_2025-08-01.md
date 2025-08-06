# Minecraft 1.18+ スーパーフラット座標調整

## 問題
- Minecraft 1.18以降、ワールドの高さが拡張された
- スーパーフラットの地表がY=4からY=-60に変更された
- 古い座標調整では正しく動作しない

## 修正内容

### 1. Y座標調整関数の更新
```javascript
// 旧: Y + 4
// 新: Y - 60
adjustYForSuperflat(y) {
    return y - 60;
}
```

### 2. プレイヤーY座標表示の調整
```javascript
getPlayerY() {
    return this.playerPos.y + 60; // 地表を0として表示
}
```

### 3. スポーン地点の更新
```javascript
// 旧: Y=4
// 新: Y=-60
teleportToSpawn() {
    this.sendCommand('teleport', {
        x: 0,
        y: -60,
        z: 0
    });
}
```

## 動作確認
- ユーザーがY=0と入力 → 実際のY=-60（地表）に配置
- プレイヤーが地表にいる時 → Y=0と表示される
- すべてのビルドコマンドで正しく地表基準で動作

## 影響範囲
- placeBlock
- removeBlock
- buildCircle
- buildSphere
- buildWall
- buildHouse
- fillBlocks
- teleportPlayer
- teleportToSpawn
- getPlayerY

## テスト済み
- ✅ プレイヤー位置取得（地表でY=0表示）
- ✅ ブロック配置（Y=0で地表に配置）
- ✅ テレポート（Y=0で地表にテレポート）