# 倉庫番ゲーム 完全実装ガイド

このガイドでは、Scratchエディターで倉庫番ゲームを完全に実装する方法を説明します。

## 生成されたファイル

1. **sokoban_game.sb3** - 基本版（29ブロック）
2. **sokoban_full.sb3** - 改良版（20ブロック）
3. **sokoban_design.md** - 詳細な設計書

## 現在実装されている機能

- [x] 変数とリストの定義
- [x] 初期化処理（緑の旗）
- [x] キー入力処理（矢印キー4方向）
- [x] 基本的なマップ描画
- [x] プレイヤー移動（座標更新）

## まだ実装されていない機能

- [ ] マップデータの正しい読み取り
- [ ] 壁の当たり判定
- [ ] 箱の検出処理
- [ ] 箱押し処理
- [ ] プレイヤーと箱の描画
- [ ] クリア判定
- [ ] ゴール表示

---

## 実装手順

### Phase 1: マップ読み取り処理の実装

#### カスタムブロック「getMapValue(x, y)」を作成

このブロックは、座標(x, y)のマップデータ値を取得します。

**Scratchでの作成手順:**

1. 「ブロック定義」→「ブロックを作る」
2. ブロック名: `getMapValue`
3. 引数を追加:
   - `x` (数値)
   - `y` (数値)
4. 「引数を使わずに画面を再描画」のチェックを外す

**ブロックの内容:**

```
定義 getMapValue (x) (y)

tempIndex を ((y) * (10)) + (x) + (1) にする

mapValue を stageData の (tempIndex) 番目 にする
```

**擬似コード:**
```
定義 getMapValue(x, y):
  tempIndex = y * mapWidth + x + 1  // リストは1始まり
  mapValue = stageData[tempIndex]
```

---

### Phase 2: 壁判定カスタムブロックの実装

#### カスタムブロック「isWall(x, y)」を作成

**Scratchでの作成手順:**

1. 「ブロック定義」→「ブロックを作る」
2. ブロック名: `isWall`
3. 引数: `x` (数値), `y` (数値)

**ブロックの内容:**

```
定義 isWall (x) (y)

getMapValue (x) (y) ▶ を実行

もし mapValue = (0) なら
  isWallResult を (1) にする // 壁
でなければ
  isWallResult を (0) にする // 壁でない
end
```

**必要な変数:**
- `isWallResult` (0または1)

---

### Phase 3: 箱検出処理の実装

#### カスタムブロック「findBox(x, y)」を作成

このブロックは、座標(x, y)に箱があるかチェックし、箱のインデックスを返します。

**Scratchでの作成手順:**

1. 「ブロック定義」→「ブロックを作る」
2. ブロック名: `findBox`
3. 引数: `x` (数値), `y` (数値)

**ブロックの内容:**

```
定義 findBox (x) (y)

boxIndex を (0) にする // 初期値：箱が見つからない

loopIndex を (1) にする

(boxesX の長さ) 回繰り返す

  もし ((boxesX の (loopIndex) 番目) = (x)) かつ ((boxesY の (loopIndex) 番目) = (y)) なら
    boxIndex を loopIndex にする
  end

  loopIndex を (1) ずつ変える

end
```

**擬似コード:**
```
定義 findBox(x, y):
  boxIndex = 0
  for i in 1 to length(boxesX):
    if boxesX[i] == x AND boxesY[i] == y:
      boxIndex = i
      break
```

---

### Phase 4: 移動処理の改良

#### 現在のキー入力処理を改良

**↑キーが押されたとき（改良版）:**

```
↑キーが押されたとき

tempX を playerX にする
tempY を (playerY) - (1) にする // 上方向

isWall (tempX) (tempY) ▶ を実行

もし isWallResult = (0) なら // 壁でない

  findBox (tempX) (tempY) ▶ を実行

  もし boxIndex = (0) なら // 箱がない
    playerY を (tempY) にする
    moveCount を (1) ずつ変える
  でなければ // 箱がある
    pushBox (0) (-1) ▶ を実行 // dx=0, dy=-1
  end

  broadcast [描画 v]
end
```

---

### Phase 5: 箱押し処理の実装

#### カスタムブロック「pushBox(dx, dy)」を作成

**Scratchでの作成手順:**

1. 「ブロック定義」→「ブロックを作る」
2. ブロック名: `pushBox`
3. 引数: `dx` (数値), `dy` (数値)

**ブロックの内容:**

```
定義 pushBox (dx) (dy)

// 箱の現在位置
tempX を (playerX) + (dx) にする
tempY を (playerY) + (dy) にする

// 箱の移動先
checkX を (tempX) + (dx) にする
checkY を (tempY) + (dy) にする

// 移動先が壁かチェック
isWall (checkX) (checkY) ▶ を実行

もし isWallResult = (0) なら // 壁でない

  // 移動先に別の箱があるかチェック
  findBox (checkX) (checkY) ▶ を実行

  もし boxIndex = (0) なら // 箱がない

    // 現在位置の箱を見つける
    findBox (tempX) (tempY) ▶ を実行

    もし (boxIndex) > (0) なら

      // 箱を移動
      boxesX の (boxIndex) 番目を (checkX) で置き換える
      boxesY の (boxIndex) 番目を (checkY) で置き換える

      // プレイヤーを移動
      playerX を (tempX) にする
      playerY を (tempY) にする

      // カウント更新
      moveCount を (1) ずつ変える
      pushCount を (1) ずつ変える

    end
  end
end
```

---

### Phase 6: 描画処理の完全実装

#### 「描画」メッセージを受け取ったときの処理を改良

```
[描画 v] を受け取ったとき

// エリアをクリア
minecraft: エリアをクリア x: (baseX) z: (baseZ)

// 0.1秒待つ（描画遅延）
(0.1) 秒待つ

// ループ変数初期化
loopY を (0) にする

(10) 回繰り返す // Y方向

  loopX を (0) にする

  (10) 回繰り返す // X方向

    // マップデータ取得
    getMapValue (loopX) (loopY) ▶ を実行

    // 座標計算
    tempX を (baseX) + (loopX) にする
    tempY を (baseY) にする
    tempZ を (baseZ) + (loopY) にする

    // マップ値に応じてブロック設置
    もし mapValue = (0) なら // 壁
      minecraft: ブロックを置く x: (tempX) y: (tempY) z: (tempZ) ブロック: [stone_bricks v]
    end

    もし (mapValue = (1)) または (mapValue = (2)) または (mapValue = (3)) または (mapValue = (4)) なら // 床系
      minecraft: ブロックを置く x: (tempX) y: (tempY) z: (tempZ) ブロック: [smooth_stone v]
    end

    もし mapValue = (2) なら // ゴール
      minecraft: ブロックを置く x: (tempX) y: (0) z: (tempZ) ブロック: [gold_block v]
    end

    loopX を (1) ずつ変える
  end

  loopY を (1) ずつ変える
end

// 箱を描画
loopIndex を (1) にする

(boxesX の長さ) 回繰り返す

  tempX を (baseX) + (boxesX の (loopIndex) 番目) にする
  tempZ を (baseZ) + (boxesY の (loopIndex) 番目) にする

  // 箱の位置がゴールかチェック
  isGoal (boxesX の (loopIndex) 番目) (boxesY の (loopIndex) 番目) ▶ を実行

  もし isOnGoal = (1) なら
    minecraft: ブロックを置く x: (tempX) y: (1) z: (tempZ) ブロック: [emerald_block v]
  でなければ
    minecraft: ブロックを置く x: (tempX) y: (1) z: (tempZ) ブロック: [redstone_block v]
  end

  loopIndex を (1) ずつ変える
end

// プレイヤーを描画
tempX を (baseX) + (playerX) にする
tempZ を (baseZ) + (playerY) にする

minecraft: ブロックを置く x: (tempX) y: (1) z: (tempZ) ブロック: [diamond_block v]

// クリア判定
broadcast [クリア判定 v]
```

---

### Phase 7: ゴール判定処理の実装

#### カスタムブロック「isGoal(x, y)」を作成

```
定義 isGoal (x) (y)

isOnGoal を (0) にする

loopIndex を (1) にする

(goalsX の長さ) 回繰り返す

  もし ((goalsX の (loopIndex) 番目) = (x)) かつ ((goalsY の (loopIndex) 番目) = (y)) なら
    isOnGoal を (1) にする
  end

  loopIndex を (1) ずつ変える
end
```

---

### Phase 8: クリア判定の実装

#### 「クリア判定」メッセージを受け取ったとき

```
[クリア判定 v] を受け取ったとき

clearedCount を (0) にする

loopIndex を (1) にする

(boxesX の長さ) 回繰り返す

  isGoal (boxesX の (loopIndex) 番目) (boxesY の (loopIndex) 番目) ▶ を実行

  もし isOnGoal = (1) なら
    clearedCount を (1) ずつ変える
  end

  loopIndex を (1) ずつ変える
end

// すべての箱がゴール上にある
もし clearedCount = (boxesX の長さ) なら
  gameState を (2) にする
  minecraft: チャット メッセージ: (ゲームクリア！移動: () 回、押し: () 回) と ((moveCount) と ((pushCount) を組み合わせる))
end
```

---

## 完成版の機能一覧

実装完了後、以下の機能がすべて動作します：

1. **初期化**
   - ステージデータの読み込み
   - 箱とゴールの位置抽出
   - プレイヤー初期位置設定

2. **移動処理**
   - 矢印キーでプレイヤー移動
   - 壁の当たり判定
   - 箱の検出と押し処理

3. **描画処理**
   - マップ全体の描画（壁、床、ゴール）
   - 箱の描画（通常/ゴール上で色変更）
   - プレイヤーの描画

4. **ゲームロジック**
   - 箱を押す処理
   - クリア判定
   - 移動回数・押し回数のカウント

5. **ブロック配置**
   - 壁: stone_bricks（石レンガ）
   - 床: smooth_stone（滑らかな石）
   - ゴール: gold_block（金ブロック）
   - 箱（通常）: redstone_block（レッドストーンブロック）Y=1
   - 箱（ゴール上）: emerald_block（エメラルドブロック）Y=1
   - プレイヤー: diamond_block（ダイヤモンドブロック）Y=1

---

## デバッグのヒント

### 問題: 壁をすり抜けてしまう
- `isWall` カスタムブロックが正しく動作しているか確認
- `getMapValue` でマップデータが正しく取得できているか確認
- 座標計算が正しいか確認（0始まり、リストは1始まり）

### 問題: 箱が動かない
- `findBox` カスタムブロックが正しく箱を検出しているか確認
- `pushBox` の移動先判定が正しいか確認
- boxesX, boxesY リストが正しく初期化されているか確認

### 問題: クリアにならない
- `isGoal` カスタムブロックが正しく動作しているか確認
- goalsX, goalsY リストが正しく初期化されているか確認
- クリア判定の条件式が正しいか確認

---

## テスト用のステージ

初期のステージデータ（10x10）:

```
# # # # # # # # # #
# . . . # . . . . #
# . B . # . G . . #
# . . . . . . . . #
# # # # . # # # # #
# . . . . . . . . #
# . G . # . B . . #
# . . . # . . . P #
# . . . . . . . . #
# # # # # # # # # #
```

- `#`: 壁（0）
- `.`: 床（1）
- `G`: ゴール（2）
- `B`: 箱（3）
- `P`: プレイヤー初期位置（4）

**箱の初期位置:**
- 箱1: (2, 2)
- 箱2: (6, 6)

**ゴールの位置:**
- ゴール1: (6, 2)
- ゴール2: (2, 6)

**プレイヤー初期位置:**
- (8, 7)

---

## 拡張アイデア

完成後、以下の機能を追加できます：

1. **複数ステージ**
   - ステージセレクト機能
   - 難易度別のステージ

2. **アンドゥ機能**
   - 移動履歴の記録
   - 1手戻る機能

3. **タイマー機能**
   - クリアタイムの計測
   - ベストタイムの記録

4. **サウンド**
   - 移動音
   - 箱を押す音
   - クリア音

5. **ビジュアル改善**
   - プレイヤーのアニメーション
   - 箱の色バリエーション
   - パーティクルエフェクト

---

## まとめ

このガイドに従って実装すれば、完全に動作する倉庫番ゲームが完成します。各フェーズを順番に実装し、動作確認しながら進めてください。

**推奨実装順序:**
1. Phase 1: マップ読み取り → テスト
2. Phase 2: 壁判定 → テスト
3. Phase 3: 箱検出 → テスト
4. Phase 4: 移動処理改良 → テスト
5. Phase 5: 箱押し処理 → テスト
6. Phase 6: 描画処理完全版 → テスト
7. Phase 7: ゴール判定 → テスト
8. Phase 8: クリア判定 → テスト

各フェーズでテストを行い、正しく動作することを確認してから次に進むことが重要です。

頑張ってください！
