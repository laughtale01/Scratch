# コンテナアイテム設定機能 実装仕様書

## 概要

Scratch から「指定座標のコンテナブロック（ディスペンサー、チェスト、ホッパー等）に指定アイテムを入れる」機能を実装する。

### 機能概要
- 座標を指定してコンテナブロックを特定
- スロット番号を指定してアイテムを配置
- アイテムの種類と個数を指定可能

### 対象コンテナブロック
| ブロック | スロット数 | 有効スロット番号 |
|---------|-----------|-----------------|
| ディスペンサー (dispenser) | 9 | 0-8 |
| ドロッパー (dropper) | 9 | 0-8 |
| チェスト (chest) | 27 | 0-26 |
| ホッパー (hopper) | 5 | 0-4 |
| 樽 (barrel) | 27 | 0-26 |
| かまど (furnace) | 3 | 0-2 |

---

## 1. MOD側の実装

### 1.1 ファイル
`minecraft-mod/src/main/java/com/github/minecraftedu/commands/CommandExecutor.java`

### 1.2 追加するimport文
```java
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
```

### 1.3 switch文への追加（execute メソッド内）
```java
case "setContainerItem":
    return executeSetContainerItem(params);
```

### 1.4 新規メソッド
```java
/**
 * コンテナブロックにアイテムを設定する
 * @param params x, y, z: 座標、slot: スロット番号、itemType: アイテムID、count: 個数
 * @return 成功時true
 */
private boolean executeSetContainerItem(JsonObject params) {
    int x = params.get("x").getAsInt();
    int y = params.get("y").getAsInt();
    int z = params.get("z").getAsInt();
    int slot = params.get("slot").getAsInt();
    String itemType = params.get("itemType").getAsString();
    int count = params.get("count").getAsInt();

    BlockPos pos = new BlockPos(x, y, z);

    server.execute(() -> {
        ServerLevel world = server.overworld();
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof Container container) {
            // スロット番号の検証
            if (slot < 0 || slot >= container.getContainerSize()) {
                MinecraftEduMod.LOGGER.warn("Invalid slot number: " + slot + " (max: " + (container.getContainerSize() - 1) + ")");
                return;
            }

            // アイテム取得
            ResourceLocation itemId = new ResourceLocation(itemType);
            Item item = BuiltInRegistries.ITEM.get(itemId);

            // 個数を1〜64に制限
            int validCount = Math.min(64, Math.max(1, count));

            // アイテムスタック作成
            ItemStack itemStack = new ItemStack(item, validCount);

            // スロットにセット
            container.setItem(slot, itemStack);
            blockEntity.setChanged();

            MinecraftEduMod.LOGGER.info("Container item set at " + pos + " slot " + slot + ": " + itemType + " x" + validCount);
        } else {
            MinecraftEduMod.LOGGER.warn("No container block at position: " + pos);
        }
    });

    // 結果データを設定
    lastResult.addProperty("success", true);
    lastResult.addProperty("x", x);
    lastResult.addProperty("y", y);
    lastResult.addProperty("z", z);
    lastResult.addProperty("slot", slot);
    lastResult.addProperty("itemType", itemType);
    lastResult.addProperty("count", count);

    return true;
}
```

### 1.5 パラメータ仕様
| パラメータ | 型 | 必須 | 説明 |
|-----------|------|------|------|
| x | int | Yes | X座標 |
| y | int | Yes | Y座標 |
| z | int | Yes | Z座標 |
| slot | int | Yes | スロット番号（0から開始） |
| itemType | String | Yes | アイテムID（例: `minecraft:arrow`） |
| count | int | Yes | 個数（1〜64） |

---

## 2. gui.js側の実装

### 2.1 ブロック定義の追加
**場所**: `getInfo()` 内の blocks 配列（約64200行目付近）

```javascript
{
  opcode: 'setContainerItem',
  blockType: 'command',
  text: formatMessage({
    id: 'minecraft.setContainerItem',
    default: 'set container at X:[X] Y:[Y] Z:[Z] slot [SLOT] to [ITEM] x [COUNT]',
    description: 'set item in container block like dispenser, chest, hopper'
  }),
  arguments: {
    X: { type: 'number', defaultValue: 0 },
    Y: { type: 'number', defaultValue: 0 },
    Z: { type: 'number', defaultValue: 0 },
    SLOT: { type: 'number', defaultValue: 0 },
    ITEM: { type: 'string', menu: 'containerItems', defaultValue: 'arrow' },
    COUNT: { type: 'number', defaultValue: 1 }
  }
}
```

### 2.2 メニュー定義の追加
**場所**: `getInfo()` 内の menus オブジェクト（約64531行目付近）

```javascript
containerItems: {
  acceptReporters: true,
  items: [
    // 発射物
    { text: { id: 'minecraft.item.arrow', default: 'Arrow' }, value: 'arrow' },
    { text: { id: 'minecraft.item.spectral_arrow', default: 'Spectral Arrow' }, value: 'spectral_arrow' },
    { text: { id: 'minecraft.item.tipped_arrow', default: 'Tipped Arrow' }, value: 'tipped_arrow' },
    { text: { id: 'minecraft.item.fire_charge', default: 'Fire Charge' }, value: 'fire_charge' },
    { text: { id: 'minecraft.item.snowball', default: 'Snowball' }, value: 'snowball' },
    { text: { id: 'minecraft.item.egg', default: 'Egg' }, value: 'egg' },
    { text: { id: 'minecraft.item.splash_potion', default: 'Splash Potion' }, value: 'splash_potion' },
    { text: { id: 'minecraft.item.lingering_potion', default: 'Lingering Potion' }, value: 'lingering_potion' },
    // バケツ類
    { text: { id: 'minecraft.item.bucket', default: 'Bucket' }, value: 'bucket' },
    { text: { id: 'minecraft.item.water_bucket', default: 'Water Bucket' }, value: 'water_bucket' },
    { text: { id: 'minecraft.item.lava_bucket', default: 'Lava Bucket' }, value: 'lava_bucket' },
    // 便利アイテム
    { text: { id: 'minecraft.item.bone_meal', default: 'Bone Meal' }, value: 'bone_meal' },
    { text: { id: 'minecraft.item.shears', default: 'Shears' }, value: 'shears' },
    { text: { id: 'minecraft.item.flint_and_steel', default: 'Flint and Steel' }, value: 'flint_and_steel' },
    // 建材
    { text: { id: 'minecraft.item.cobblestone', default: 'Cobblestone' }, value: 'cobblestone' },
    { text: { id: 'minecraft.item.stone', default: 'Stone' }, value: 'stone' },
    { text: { id: 'minecraft.item.oak_planks', default: 'Oak Planks' }, value: 'oak_planks' },
    // レッドストーン関連
    { text: { id: 'minecraft.item.redstone', default: 'Redstone' }, value: 'redstone' },
    { text: { id: 'minecraft.item.redstone_torch', default: 'Redstone Torch' }, value: 'redstone_torch' },
    // TNT
    { text: { id: 'minecraft.item.tnt', default: 'TNT' }, value: 'tnt' }
  ]
}
```

### 2.3 メソッド実装
**場所**: クラス内のメソッド定義部分

```javascript
/**
 * コンテナブロックにアイテムを設定する
 * @param {object} args - ブロック引数
 * @returns {Promise} コマンド実行結果
 */
setContainerItem(args) {
  return this.sendCommand('setContainerItem', {
    x: Number(args.X),
    y: Number(args.Y) - 60,  // Y座標オフセット（スーパーフラット対応）
    z: Number(args.Z),
    slot: Math.max(0, Math.floor(Number(args.SLOT))),
    itemType: 'minecraft:' + args.ITEM,
    count: Math.min(64, Math.max(1, Math.floor(Number(args.COUNT))))
  });
}
```

---

## 3. 翻訳

### 3.1 日本語（漢字）
**場所**: 約341950行目付近

```javascript
"minecraft.setContainerItem": "X:[X] Y:[Y] Z:[Z] のコンテナのスロット [SLOT] に [ITEM] を [COUNT] 個入れる",
"minecraft.item.arrow": "矢",
"minecraft.item.spectral_arrow": "光の矢",
"minecraft.item.tipped_arrow": "効果付きの矢",
"minecraft.item.fire_charge": "ファイヤーチャージ",
"minecraft.item.snowball": "雪玉",
"minecraft.item.egg": "卵",
"minecraft.item.splash_potion": "スプラッシュポーション",
"minecraft.item.lingering_potion": "残留ポーション",
"minecraft.item.bucket": "バケツ",
"minecraft.item.water_bucket": "水入りバケツ",
"minecraft.item.lava_bucket": "溶岩入りバケツ",
"minecraft.item.bone_meal": "骨粉",
"minecraft.item.shears": "ハサミ",
"minecraft.item.flint_and_steel": "火打ち石と打ち金",
"minecraft.item.cobblestone": "丸石",
"minecraft.item.stone": "石",
"minecraft.item.oak_planks": "オークの板材",
"minecraft.item.redstone": "レッドストーン",
"minecraft.item.redstone_torch": "レッドストーントーチ",
"minecraft.item.tnt": "TNT",
```

### 3.2 日本語（ひらがな）
**場所**: 約342850行目付近

```javascript
"minecraft.setContainerItem": "X:[X] Y:[Y] Z:[Z] の コンテナ の スロット [SLOT] に [ITEM] を [COUNT] こ いれる",
"minecraft.item.arrow": "や",
"minecraft.item.spectral_arrow": "ひかりのや",
"minecraft.item.tipped_arrow": "こうかつきのや",
"minecraft.item.fire_charge": "ふぁいやーちゃーじ",
"minecraft.item.snowball": "ゆきだま",
"minecraft.item.egg": "たまご",
"minecraft.item.splash_potion": "すぷらっしゅぽーしょん",
"minecraft.item.lingering_potion": "ざんりゅうぽーしょん",
"minecraft.item.bucket": "ばけつ",
"minecraft.item.water_bucket": "みずいりばけつ",
"minecraft.item.lava_bucket": "ようがんいりばけつ",
"minecraft.item.bone_meal": "ほねこ",
"minecraft.item.shears": "はさみ",
"minecraft.item.flint_and_steel": "ひうちいしとうちがね",
"minecraft.item.cobblestone": "まるいし",
"minecraft.item.stone": "いし",
"minecraft.item.oak_planks": "おーくのいたざい",
"minecraft.item.redstone": "れっどすとーん",
"minecraft.item.redstone_torch": "れっどすとーんとーち",
"minecraft.item.tnt": "TNT",
```

---

## 4. エラーハンドリング

### 4.1 検証項目
| チェック項目 | 処理 |
|-------------|------|
| 座標にコンテナブロックがない | ログ出力、処理をスキップ |
| スロット番号が範囲外 | ログ出力、処理をスキップ |
| 無効なアイテムID | `Items.AIR` になる（Minecraft仕様） |
| 個数が範囲外 | 1〜64に自動補正 |

### 4.2 ログ出力例
```
[INFO] Container item set at BlockPos{x=10, y=64, z=20} slot 0: minecraft:arrow x16
[WARN] No container block at position: BlockPos{x=0, y=0, z=0}
[WARN] Invalid slot number: 10 (max: 8)
```

---

## 5. 作業チェックリスト

### MOD側
- [ ] import文を追加（Container, Item, ItemStack, BlockEntity）
- [ ] switch文に `case "setContainerItem"` を追加
- [ ] `executeSetContainerItem` メソッドを追加
- [ ] MODをビルド (`./gradlew build`)
- [ ] JARファイルをminecraftのmodsフォルダにコピー

### gui.js側
- [ ] ブロック定義を追加
- [ ] メニュー定義（containerItems）を追加
- [ ] `setContainerItem` メソッドを追加
- [ ] 日本語（漢字）翻訳を追加
- [ ] 日本語（ひらがな）翻訳を追加

### テスト
- [ ] Minecraftでディスペンサーを設置
- [ ] Scratchからブロックを使用してアイテムを入れる
- [ ] ディスペンサーを開いてアイテムが入っているか確認
- [ ] 無効な座標でエラーにならないことを確認
- [ ] 無効なスロット番号でエラーにならないことを確認

### デプロイ
- [ ] git add / commit / push
- [ ] GitHub Pages で動作確認

---

## 6. 技術リファレンス

### Minecraft Forge 1.20.1 API
- [DispenserBlockEntity](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.17.1/net/minecraft/world/level/block/entity/DispenserBlockEntity.html)
- [ItemStack](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.20.6-neoforge/net/minecraft/world/item/ItemStack.html)
- [Container Interface](https://docs.neoforged.net/docs/1.21.1/blockentities/container/)

### クラス継承関係
```
DispenserBlockEntity
  └─ RandomizableContainerBlockEntity
      └─ BaseContainerBlockEntity
          └─ BlockEntity
              └─ CapabilityProvider<BlockEntity>

implements: Container, Clearable, MenuConstructor, MenuProvider, Nameable
```

---

## 7. 今後の拡張案

1. **アイテムの取り出し機能**: コンテナからアイテムを取得するブロック
2. **コンテナの中身確認**: 指定スロットのアイテムを返すレポーターブロック
3. **一括設定**: 複数スロットを一度に設定
4. **NBTタグ対応**: エンチャントやポーション効果の指定

---

*作成日: 2026-01-10*
*対象バージョン: Minecraft Forge 1.20.1-47.2.0*
