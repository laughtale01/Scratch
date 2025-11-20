# 垂直ハーフブロック（Vertical Slabs）使用ガイド

**バージョン**: 0.1.0
**対応Minecraft**: 1.20.1
**MODタイプ**: Forge MOD
**最終更新**: 2025-11-15

---

## 概要

垂直ハーフブロック（Vertical Slabs）は、バニラMinecraftには存在しない、**水平方向に設置できるハーフブロック**です。通常のハーフブロックが上下の配置しかできないのに対し、垂直ハーフブロックは東西南北の4方向に設置できます。

### バニラハーフブロックとの違い

| 特徴 | バニラハーフブロック | 垂直ハーフブロック |
|------|---------------------|------------------|
| **配置方向** | 上/下/二重（垂直のみ） | 北/南/東/西（水平） |
| **プロパティ** | `type`: bottom/top/double | `facing`: north/south/east/west |
| **用途** | 床、天井、段差 | 壁、仕切り、柱 |
| **MOD** | バニラ（不要） | MinecraftEdu MOD（必須） |
| **名前空間** | `minecraft:` | `minecraftedu:` |

### 実装されているブロック

現在のバージョンでは、プロトタイプとして以下の1種類が実装されています：

- **vertical_oak_slab（オークのスラブ（垂直））**
  - 素材: オークの木材
  - テクスチャ: バニラのオークの板材を再利用
  - 名前空間: `minecraftedu:vertical_oak_slab`

---

## 使い方

### Scratchから使用する

1. **Scratchでブロック選択メニューを開く**
   - 「ブロックを設置」ブロックのドロップダウンを開きます

2. **垂直ハーフブロックを選択**
   - リストから「オークのスラブ（垂直）」を選択
   - 通常のハーフブロックの下に表示されます

3. **向きを指定**
   - 「向き」パラメータで以下から選択：
     - **北（North）**: Z軸の負の方向（北半分を占有）
     - **南（South）**: Z軸の正の方向（南半分を占有）
     - **東（East）**: X軸の正の方向（東半分を占有）
     - **西（West）**: X軸の負の方向（西半分を占有）

4. **配置タイプは無視される**
   - 垂直ハーフブロックでは、「配置」パラメータ（上/下/二重）は**自動的に無視**されます
   - 向きパラメータのみが使用されます

### 使用例（Scratchブロック）

```
ブロックを設置 [オークのスラブ（垂直）▼] 位置 X:(0) Y:(0) Z:(0) 配置 [bottom▼] 向き [north▼]
→ 結果: minecraftedu:vertical_oak_slab[facing=north] が (0, 0, 0) に設置されます
```

**重要**: 配置パラメータ（bottom/top/double）を変更しても、垂直ハーフブロックには影響しません。

---

## 技術仕様

### ブロックプロパティ

```
minecraftedu:vertical_oak_slab[facing=<direction>,waterlogged=<boolean>]
```

| プロパティ | 値 | 説明 |
|-----------|------|------|
| `facing` | north, south, east, west | ブロックが向いている方向（必須） |
| `waterlogged` | true, false | 水没状態（オプション、デフォルト: false） |

### 衝突ボックス（Collision Box）

垂直ハーフブロックの衝突ボックスは、向きに応じて変化します：

| 向き | 占有範囲（X, Y, Z） | 説明 |
|------|------------------|------|
| **North** | (0-16, 0-16, 0-8) | 北側の半分（Z軸の前半） |
| **South** | (0-16, 0-16, 8-16) | 南側の半分（Z軸の後半） |
| **East** | (8-16, 0-16, 0-16) | 東側の半分（X軸の後半） |
| **West** | (0-8, 0-16, 0-16) | 西側の半分（X軸の前半） |

- 高さは常にフルブロック（16ピクセル）
- 幅は半分（8ピクセル）

### 配置ロジック

ゲーム内で手動配置する場合（クリエイティブモード）：

1. **横面をクリック**: その面の方向に向いて配置
   - 例: 東面をクリック → `facing=east`

2. **上面/下面をクリック**: プレイヤーの向いている方向に配置
   - 例: プレイヤーが北を向いている → `facing=north`

---

## ファイル構成

### Forge MOD（Java）

```
minecraft-mod/src/main/java/com/github/minecraftedu/
├── block/
│   └── VerticalSlabBlock.java          # 垂直スラブブロッククラス
└── init/
    ├── ModBlocks.java                   # ブロック登録
    └── ModItems.java                    # アイテム登録

minecraft-mod/src/main/resources/assets/minecraftedu/
├── blockstates/
│   └── vertical_oak_slab.json          # ブロック状態定義（4方向）
├── models/
│   ├── block/
│   │   └── vertical_oak_slab.json      # ブロックモデル（半幅キューブ）
│   └── item/
│       └── vertical_oak_slab.json      # アイテムモデル
└── lang/
    ├── en_us.json                       # 英語名: "Vertical Oak Slab"
    └── ja_jp.json                       # 日本語名: "オークのスラブ（垂直）"
```

### Scratch拡張（JavaScript）

```
scratch-client/scratch-vm/src/extensions/scratch3_minecraft/
└── index.js                             # 垂直スラブをブロックリストに追加
                                         # _buildBlockTypeWithProperties更新
```

---

## WebSocketプロトコル

垂直ハーフブロックは、既存のWebSocketプロトコルをそのまま使用します。

### 設置コマンド例

```json
{
  "type": "request",
  "payload": {
    "action": "setBlock",
    "params": {
      "blockType": "minecraftedu:vertical_oak_slab[facing=north]",
      "x": 0,
      "y": 64,
      "z": 0
    }
  }
}
```

**変更点なし**: 既存の `setBlock` アクションがそのまま使用可能です。

---

## テスト

### 単体テスト（Jest）

垂直ハーフブロックのテストは `__tests__/scratch3_minecraft.test.js` に含まれています：

```bash
cd scratch-client/scratch-vm
npm test
```

**テストケース**（7件）:
1. ✅ minecraftedu 名前空間の使用確認
2. ✅ facing=north の生成確認
3. ✅ facing=south の生成確認
4. ✅ facing=east の生成確認
5. ✅ facing=west の生成確認
6. ✅ placement パラメータ無視確認（top）
7. ✅ placement パラメータ無視確認（double）

### MODコンパイルテスト

```bash
cd minecraft-mod
./gradlew build
```

**成功条件**: ビルドエラーなし、JAR生成成功

### ゲーム内テスト

1. Minecraftサーバー起動（MOD読み込み）
2. Scratchから接続
3. 垂直ハーフブロックを各方向に設置
4. 目視確認:
   - ✅ ブロックが正しい方向を向いている
   - ✅ テクスチャが正しく表示される
   - ✅ 衝突判定が正しい（半分の幅）
   - ✅ 水没が機能する

---

## トラブルシューティング

### ブロックが表示されない

**症状**: Scratchのドロップダウンに「オークのスラブ（垂直）」が表示されない

**原因**: Scratch拡張が更新されていない

**解決策**:
```bash
cd scratch-client/scratch-vm
npm install
npm run build
```

### ブロックが設置できない

**症状**: Scratchから設置しようとするとエラーになる

**原因**: MODがサーバーに読み込まれていない

**解決策**:
1. `minecraft-mod/build/libs/minecraftedu-mod-0.1.0-1.20.1.jar` が存在するか確認
2. Minecraftサーバーの `mods/` フォルダにJARファイルを配置
3. サーバーを再起動

### テクスチャが表示されない（紫黒のチェック模様）

**症状**: ブロックが紫とピンクのチェック模様で表示される

**原因**: モデルまたはテクスチャパスが間違っている

**確認事項**:
1. `vertical_oak_slab.json` のテクスチャパス: `"minecraft:block/oak_planks"` が正しいか
2. モデルファイルが `assets/minecraftedu/models/block/` にあるか
3. blockstateファイルが `assets/minecraftedu/blockstates/` にあるか

### 向きが反映されない

**症状**: どの向きを指定しても同じ向きになる

**原因**: blockstateファイルのrotation設定が間違っている

**確認**: `vertical_oak_slab.json` の各 facing バリアントで `"y"` が正しく設定されているか

---

## 制限事項

### 現在の実装での制限

1. **ブロック種類**: オークのみ（他の木材や石材は未実装）
2. **二重配置**: 2つの垂直スラブを組み合わせてフルブロックにする機能は未実装
3. **レシピ**: クラフトレシピや石切台レシピは未実装
4. **クリエイティブタブ**: 専用のクリエイティブタブなし（検索で見つける必要あり）

### 今後の拡張予定

- **追加ブロック**: vertical_stone_slab, vertical_birch_slab など
- **二重配置機能**: 向かい合う2つの垂直スラブ → フルブロック変換
- **クラフトレシピ**: オークの板材 → 垂直オークスラブ（通常のスラブと同様）
- **石切台サポート**: オーク原木 → 垂直オークスラブ

---

## よくある質問（FAQ）

### Q1: バニラのハーフブロックと一緒に使えますか？

**A**: はい、問題なく併用できます。バニラハーフブロックは上下配置、垂直ハーフブロックは水平配置と、用途が異なります。

### Q2: マルチプレイヤーで使えますか？

**A**: はい、サーバーにMODがインストールされていれば、全てのプレイヤーが使用できます。

### Q3: 既存のワールドに追加できますか？

**A**: はい、既存ワールドに問題なく追加できます。新しいブロックIDが追加されるだけで、既存のブロックには影響しません。

### Q4: MODを削除するとどうなりますか？

**A**: MODを削除すると、設置済みの垂直ハーフブロックは消えます（または「Missing Block」表示になります）。バックアップを取ってから削除してください。

### Q5: 他のMODと競合しますか？

**A**: 基本的に競合しません。ただし、同じブロックIDを使用する他のMODとは競合する可能性があります。

---

## 開発者向け情報

### 新しい垂直スラブを追加する方法

垂直スラブは拡張可能な設計になっています。新しい種類を追加するには：

#### 1. MOD側（Java）

**ModBlocks.java**:
```java
public static final RegistryObject<Block> VERTICAL_STONE_SLAB = BLOCKS.register(
    "vertical_stone_slab",
    () -> new VerticalSlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE))
);
```

**ModItems.java**:
```java
public static final RegistryObject<Item> VERTICAL_STONE_SLAB = ITEMS.register(
    "vertical_stone_slab",
    () -> new BlockItem(ModBlocks.VERTICAL_STONE_SLAB.get(), new Item.Properties())
);
```

#### 2. リソースファイル

- blockstate JSON: `assets/minecraftedu/blockstates/vertical_stone_slab.json`
- block model: `assets/minecraftedu/models/block/vertical_stone_slab.json`
- item model: `assets/minecraftedu/models/item/vertical_stone_slab.json`
- 言語ファイル: `assets/minecraftedu/lang/ja_jp.json`, `en_us.json`

#### 3. Scratch拡張（JavaScript）

**index.js** のブロックリストに追加:
```javascript
// 垂直ハーフブロック（MOD追加ブロック）
'vertical_oak_slab',
'vertical_stone_slab',  // 新規追加
```

#### 4. テスト追加

**__tests__/scratch3_minecraft.test.js**:
```javascript
test('should build vertical stone slab with minecraftedu namespace', () => {
    const result = instance._buildBlockTypeWithProperties('vertical_stone_slab', 'bottom', 'north');
    expect(result).toBe('minecraftedu:vertical_stone_slab[facing=north]');
});
```

---

## 関連ドキュメント

- [実装計画書](./VERTICAL_SLAB_IMPLEMENTATION_PLAN.md) - 開発計画と技術詳細
- [ハーフブロック調査報告](./SLAB_FACING_INVESTIGATION.md) - バニラスラブの制限調査
- [ブロックリファレンス](../BLOCK_REFERENCE.md) - 全ブロックの使用方法

---

## 変更履歴

### v0.1.0 (2025-11-15)
- ✨ 垂直ハーフブロック機能の初回実装
- ✅ vertical_oak_slab プロトタイプ追加
- ✅ Scratch拡張対応
- ✅ テスト完備（Jest 7件、全てパス）
- 📝 ドキュメント作成

---

## ライセンス

MinecraftEdu MOD の一部として、プロジェクトのライセンスに従います。

---

## サポート

問題が発生した場合は、以下の情報と共に報告してください：

1. Minecraftバージョン
2. Forge version
3. MODバージョン
4. エラーメッセージ（ログファイル）
5. 再現手順

**ログファイル場所**:
- Minecraftサーバー: `logs/latest.log`
- Scratchコンソール: ブラウザの開発者ツール
