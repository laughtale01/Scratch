# ゲーム内チャット表示 & 日本語ブロック名 実装計画書

## 概要

MinecraftEdu MODにおいて、以下の2つの機能を実装する。

1. **ゲーム内チャット表示**: 各種コマンド実行時にゲーム内チャット画面に結果を表示
2. **日本語ブロック名表示**: ブロック調査時に日本語でブロック名を表示

---

## 背景

### 現状の問題

| 問題 | 詳細 |
|------|------|
| ログが見えない | `LOGGER.info()` はログファイル（latest.log）にのみ出力され、ゲーム内チャットには表示されない |
| 英語表示 | ブロック調査時に `stone` などの英語内部名が表示される |

### ユーザーの期待

- ゲームルール設定時（時刻固定、天気固定など）にゲーム内チャットで結果を確認したい
- ブロック名を日本語で表示してほしい

---

## 修正対象ファイル

| ファイル | 変更内容 |
|----------|----------|
| `CommandExecutor.java` | チャット表示機能追加、日本語マッピング追加 |
| `BlockNameTranslator.java` | 新規作成（日本語ブロック名マッピング） |

### ファイルパス

```
minecraft-mod/src/main/java/com/github/minecraftedu/
├── commands/
│   └── CommandExecutor.java  (修正)
└── util/
    └── BlockNameTranslator.java  (新規)
```

---

## 実装詳細

### 1. BlockNameTranslator.java（新規作成）

#### 目的
英語のブロック内部名を日本語に変換するユーティリティクラス

#### 設計

```java
package com.github.minecraftedu.util;

import java.util.HashMap;
import java.util.Map;

/**
 * ブロック名の日本語翻訳
 * gui.jsの翻訳データから抽出
 */
public class BlockNameTranslator {

    private static final Map<String, String> BLOCK_NAMES = new HashMap<>();

    static {
        // 基本ブロック
        BLOCK_NAMES.put("stone", "石");
        BLOCK_NAMES.put("cobblestone", "丸石");
        BLOCK_NAMES.put("terracotta", "テラコッタ");

        // 板材
        BLOCK_NAMES.put("oak_planks", "オークの板材");
        BLOCK_NAMES.put("spruce_planks", "トウヒの板材");
        BLOCK_NAMES.put("birch_planks", "シラカバの板材");
        BLOCK_NAMES.put("jungle_planks", "ジャングルの板材");
        BLOCK_NAMES.put("acacia_planks", "アカシアの板材");
        BLOCK_NAMES.put("dark_oak_planks", "ダークオークの板材");
        BLOCK_NAMES.put("mangrove_planks", "マングローブの板材");
        BLOCK_NAMES.put("cherry_planks", "サクラの板材");
        BLOCK_NAMES.put("bamboo_planks", "竹の板材");
        BLOCK_NAMES.put("crimson_planks", "真紅の板材");
        BLOCK_NAMES.put("warped_planks", "歪んだ板材");

        // 原木（樹皮を剥いだ）
        BLOCK_NAMES.put("stripped_oak_log", "樹皮を剥いだオークの原木");
        BLOCK_NAMES.put("stripped_spruce_log", "樹皮を剥いだトウヒの原木");
        BLOCK_NAMES.put("stripped_birch_log", "樹皮を剥いだシラカバの原木");
        BLOCK_NAMES.put("stripped_jungle_log", "樹皮を剥いだジャングルの原木");
        BLOCK_NAMES.put("stripped_acacia_log", "樹皮を剥いだアカシアの原木");
        BLOCK_NAMES.put("stripped_dark_oak_log", "樹皮を剥いだダークオークの原木");
        BLOCK_NAMES.put("stripped_mangrove_log", "樹皮を剥いだマングローブの原木");
        BLOCK_NAMES.put("stripped_cherry_log", "樹皮を剥いだサクラの原木");
        BLOCK_NAMES.put("stripped_crimson_stem", "樹皮を剥いだ真紅の幹");
        BLOCK_NAMES.put("stripped_warped_stem", "樹皮を剥いだ歪んだ幹");

        // 木
        BLOCK_NAMES.put("oak_wood", "オークの木");
        BLOCK_NAMES.put("spruce_wood", "トウヒの木");
        BLOCK_NAMES.put("birch_wood", "シラカバの木");
        BLOCK_NAMES.put("jungle_wood", "ジャングルの木");
        BLOCK_NAMES.put("acacia_wood", "アカシアの木");
        BLOCK_NAMES.put("dark_oak_wood", "ダークオークの木");
        BLOCK_NAMES.put("mangrove_wood", "マングローブの木");
        BLOCK_NAMES.put("cherry_wood", "サクラの木");
        BLOCK_NAMES.put("crimson_hyphae", "真紅の菌糸");
        BLOCK_NAMES.put("warped_hyphae", "歪んだ菌糸");

        // 石系
        BLOCK_NAMES.put("andesite", "安山岩");
        BLOCK_NAMES.put("polished_andesite", "磨かれた安山岩");
        BLOCK_NAMES.put("diorite", "閃緑岩");
        BLOCK_NAMES.put("polished_diorite", "磨かれた閃緑岩");
        BLOCK_NAMES.put("granite", "花崗岩");
        BLOCK_NAMES.put("polished_granite", "磨かれた花崗岩");
        BLOCK_NAMES.put("calcite", "方解石");
        BLOCK_NAMES.put("tuff", "凝灰岩");

        // ディープスレート
        BLOCK_NAMES.put("deepslate", "ディープスレート");
        BLOCK_NAMES.put("cobbled_deepslate", "丸石ディープスレート");
        BLOCK_NAMES.put("polished_deepslate", "磨かれたディープスレート");
        BLOCK_NAMES.put("deepslate_bricks", "ディープスレートレンガ");
        BLOCK_NAMES.put("deepslate_tiles", "ディープスレートタイル");
        BLOCK_NAMES.put("chiseled_deepslate", "模様入りのディープスレート");
        BLOCK_NAMES.put("cracked_deepslate_bricks", "ひび割れたディープスレートレンガ");
        BLOCK_NAMES.put("cracked_deepslate_tiles", "ひび割れたディープスレートタイル");

        // 黒石
        BLOCK_NAMES.put("basalt", "玄武岩");
        BLOCK_NAMES.put("polished_basalt", "磨かれた玄武岩");
        BLOCK_NAMES.put("blackstone", "黒石");
        BLOCK_NAMES.put("gilded_blackstone", "金入りの黒石");
        BLOCK_NAMES.put("polished_blackstone", "磨かれた黒石");
        BLOCK_NAMES.put("polished_blackstone_bricks", "磨かれた黒石レンガ");
        BLOCK_NAMES.put("cracked_polished_blackstone_bricks", "ひび割れた磨かれた黒石レンガ");

        // レンガ系
        BLOCK_NAMES.put("bricks", "レンガ");
        BLOCK_NAMES.put("stone_bricks", "石レンガ");
        BLOCK_NAMES.put("mossy_stone_bricks", "苔むした石レンガ");
        BLOCK_NAMES.put("cracked_stone_bricks", "ひび割れた石レンガ");
        BLOCK_NAMES.put("chiseled_stone_bricks", "模様入りの石レンガ");
        BLOCK_NAMES.put("nether_bricks", "ネザーレンガ");
        BLOCK_NAMES.put("red_nether_bricks", "赤いネザーレンガ");
        BLOCK_NAMES.put("chiseled_nether_bricks", "模様入りのネザーレンガ");
        BLOCK_NAMES.put("cracked_nether_bricks", "ひび割れたネザーレンガ");
        BLOCK_NAMES.put("end_stone_bricks", "エンドストーンレンガ");
        BLOCK_NAMES.put("prismarine_bricks", "プリズマリンレンガ");
        BLOCK_NAMES.put("quartz_bricks", "クォーツレンガ");
        BLOCK_NAMES.put("mud_bricks", "泥レンガ");

        // コンクリート
        BLOCK_NAMES.put("white_concrete", "白色のコンクリート");
        BLOCK_NAMES.put("orange_concrete", "橙色のコンクリート");
        BLOCK_NAMES.put("magenta_concrete", "赤紫色のコンクリート");
        BLOCK_NAMES.put("light_blue_concrete", "空色のコンクリート");
        BLOCK_NAMES.put("yellow_concrete", "黄色のコンクリート");
        BLOCK_NAMES.put("lime_concrete", "黄緑色のコンクリート");
        BLOCK_NAMES.put("pink_concrete", "桃色のコンクリート");
        BLOCK_NAMES.put("gray_concrete", "灰色のコンクリート");
        BLOCK_NAMES.put("light_gray_concrete", "薄灰色のコンクリート");
        BLOCK_NAMES.put("cyan_concrete", "青緑色のコンクリート");
        BLOCK_NAMES.put("purple_concrete", "紫色のコンクリート");
        BLOCK_NAMES.put("blue_concrete", "青色のコンクリート");
        BLOCK_NAMES.put("brown_concrete", "茶色のコンクリート");
        BLOCK_NAMES.put("green_concrete", "緑色のコンクリート");
        BLOCK_NAMES.put("red_concrete", "赤色のコンクリート");
        BLOCK_NAMES.put("black_concrete", "黒色のコンクリート");

        // その他
        BLOCK_NAMES.put("dark_prismarine", "ダークプリズマリン");
        BLOCK_NAMES.put("prismarine", "プリズマリン");
        BLOCK_NAMES.put("chiseled_quartz_block", "模様入りのクォーツブロック");
        BLOCK_NAMES.put("quartz_pillar", "クォーツの柱");
        BLOCK_NAMES.put("smooth_quartz", "滑らかなクォーツ");
        BLOCK_NAMES.put("purpur_block", "プルパーブロック");
        BLOCK_NAMES.put("purpur_pillar", "プルパーの柱");
        BLOCK_NAMES.put("sandstone", "砂岩");
        BLOCK_NAMES.put("chiseled_sandstone", "模様入りの砂岩");
        BLOCK_NAMES.put("cut_sandstone", "カットされた砂岩");
        BLOCK_NAMES.put("smooth_sandstone", "滑らかな砂岩");
        BLOCK_NAMES.put("red_sandstone", "赤い砂岩");
        BLOCK_NAMES.put("chiseled_red_sandstone", "模様入りの赤い砂岩");
        BLOCK_NAMES.put("cut_red_sandstone", "カットされた赤い砂岩");
        BLOCK_NAMES.put("smooth_red_sandstone", "滑らかな赤い砂岩");
        BLOCK_NAMES.put("smooth_stone", "滑らかな石");
        BLOCK_NAMES.put("bamboo_mosaic", "竹モザイク");

        // 階段
        BLOCK_NAMES.put("oak_stairs", "オークの階段");
        BLOCK_NAMES.put("spruce_stairs", "トウヒの階段");
        BLOCK_NAMES.put("birch_stairs", "シラカバの階段");
        BLOCK_NAMES.put("jungle_stairs", "ジャングルの階段");
        BLOCK_NAMES.put("acacia_stairs", "アカシアの階段");
        BLOCK_NAMES.put("dark_oak_stairs", "ダークオークの階段");
        BLOCK_NAMES.put("mangrove_stairs", "マングローブの階段");
        BLOCK_NAMES.put("cherry_stairs", "サクラの階段");
        BLOCK_NAMES.put("bamboo_stairs", "竹の階段");
        BLOCK_NAMES.put("bamboo_mosaic_stairs", "竹モザイクの階段");
        BLOCK_NAMES.put("crimson_stairs", "真紅の階段");
        BLOCK_NAMES.put("warped_stairs", "歪んだ階段");
        BLOCK_NAMES.put("stone_stairs", "石の階段");
        BLOCK_NAMES.put("cobblestone_stairs", "丸石の階段");
        BLOCK_NAMES.put("stone_brick_stairs", "石レンガの階段");
        BLOCK_NAMES.put("brick_stairs", "レンガの階段");
        BLOCK_NAMES.put("nether_brick_stairs", "ネザーレンガの階段");
        BLOCK_NAMES.put("sandstone_stairs", "砂岩の階段");
        BLOCK_NAMES.put("red_sandstone_stairs", "赤い砂岩の階段");
        BLOCK_NAMES.put("quartz_stairs", "クォーツの階段");
        BLOCK_NAMES.put("purpur_stairs", "プルパーの階段");
        BLOCK_NAMES.put("prismarine_stairs", "プリズマリンの階段");
        BLOCK_NAMES.put("prismarine_brick_stairs", "プリズマリンレンガの階段");
        BLOCK_NAMES.put("dark_prismarine_stairs", "ダークプリズマリンの階段");

        // ハーフブロック
        BLOCK_NAMES.put("oak_slab", "オークのハーフブロック");
        BLOCK_NAMES.put("spruce_slab", "トウヒのハーフブロック");
        BLOCK_NAMES.put("birch_slab", "シラカバのハーフブロック");
        BLOCK_NAMES.put("jungle_slab", "ジャングルのハーフブロック");
        BLOCK_NAMES.put("acacia_slab", "アカシアのハーフブロック");
        BLOCK_NAMES.put("dark_oak_slab", "ダークオークのハーフブロック");
        BLOCK_NAMES.put("mangrove_slab", "マングローブのハーフブロック");
        BLOCK_NAMES.put("cherry_slab", "サクラのハーフブロック");
        BLOCK_NAMES.put("bamboo_slab", "竹のハーフブロック");
        BLOCK_NAMES.put("bamboo_mosaic_slab", "竹モザイクのハーフブロック");
        BLOCK_NAMES.put("crimson_slab", "真紅のハーフブロック");
        BLOCK_NAMES.put("warped_slab", "歪んだハーフブロック");
        BLOCK_NAMES.put("stone_slab", "石のハーフブロック");
        BLOCK_NAMES.put("cobblestone_slab", "丸石のハーフブロック");
        BLOCK_NAMES.put("smooth_stone_slab", "滑らかな石のハーフブロック");
        BLOCK_NAMES.put("stone_brick_slab", "石レンガのハーフブロック");
        BLOCK_NAMES.put("brick_slab", "レンガのハーフブロック");
        BLOCK_NAMES.put("nether_brick_slab", "ネザーレンガのハーフブロック");
        BLOCK_NAMES.put("sandstone_slab", "砂岩のハーフブロック");
        BLOCK_NAMES.put("red_sandstone_slab", "赤い砂岩のハーフブロック");
        BLOCK_NAMES.put("quartz_slab", "クォーツのハーフブロック");
        BLOCK_NAMES.put("smooth_quartz_slab", "滑らかなクォーツのハーフブロック");
        BLOCK_NAMES.put("purpur_slab", "プルプァのハーフブロック");
        BLOCK_NAMES.put("prismarine_slab", "プリズマリンのハーフブロック");
        BLOCK_NAMES.put("prismarine_brick_slab", "プリズマリンレンガのハーフブロック");
        BLOCK_NAMES.put("dark_prismarine_slab", "ダークプリズマリンのハーフブロック");

        // 塀
        BLOCK_NAMES.put("cobblestone_wall", "丸石の塀");
        BLOCK_NAMES.put("mossy_cobblestone_wall", "苔むした丸石の塀");
        BLOCK_NAMES.put("stone_brick_wall", "石レンガの塀");
        BLOCK_NAMES.put("brick_wall", "レンガの塀");
        BLOCK_NAMES.put("nether_brick_wall", "ネザーレンガの塀");
        BLOCK_NAMES.put("red_nether_brick_wall", "赤いネザーレンガの塀");
        BLOCK_NAMES.put("sandstone_wall", "砂岩の塀");
        BLOCK_NAMES.put("red_sandstone_wall", "赤い砂岩の塀");
        BLOCK_NAMES.put("andesite_wall", "安山岩の塀");
        BLOCK_NAMES.put("diorite_wall", "閃緑岩の塀");
        BLOCK_NAMES.put("granite_wall", "花崗岩の塀");
        BLOCK_NAMES.put("prismarine_wall", "プリズマリンの塀");
        BLOCK_NAMES.put("blackstone_wall", "黒石の塀");
        BLOCK_NAMES.put("polished_blackstone_wall", "磨かれた黒石の塀");
        BLOCK_NAMES.put("polished_blackstone_brick_wall", "磨かれた黒石レンガの塀");
        BLOCK_NAMES.put("cobbled_deepslate_wall", "丸石状ディープスレートの塀");
        BLOCK_NAMES.put("polished_deepslate_wall", "磨かれたディープスレートの塀");
        BLOCK_NAMES.put("deepslate_brick_wall", "ディープスレートレンガの塀");
        BLOCK_NAMES.put("deepslate_tile_wall", "ディープスレートタイルの塀");
        BLOCK_NAMES.put("end_stone_brick_wall", "エンドストーンレンガの塀");
        BLOCK_NAMES.put("mud_brick_wall", "泥レンガの塀");

        // フェンス
        BLOCK_NAMES.put("oak_fence", "オークのフェンス");
        BLOCK_NAMES.put("spruce_fence", "トウヒのフェンス");
        BLOCK_NAMES.put("birch_fence", "シラカバのフェンス");
        BLOCK_NAMES.put("jungle_fence", "ジャングルのフェンス");
        BLOCK_NAMES.put("acacia_fence", "アカシアのフェンス");
        BLOCK_NAMES.put("dark_oak_fence", "ダークオークのフェンス");
        BLOCK_NAMES.put("mangrove_fence", "マングローブのフェンス");
        BLOCK_NAMES.put("cherry_fence", "サクラのフェンス");
        BLOCK_NAMES.put("bamboo_fence", "竹のフェンス");
        BLOCK_NAMES.put("crimson_fence", "真紅のフェンス");
        BLOCK_NAMES.put("warped_fence", "歪んだフェンス");
        BLOCK_NAMES.put("nether_brick_fence", "ネザーレンガのフェンス");

        // フェンスゲート
        BLOCK_NAMES.put("oak_fence_gate", "オークのフェンスゲート");
        BLOCK_NAMES.put("spruce_fence_gate", "トウヒのフェンスゲート");
        BLOCK_NAMES.put("birch_fence_gate", "シラカバのフェンスゲート");
        BLOCK_NAMES.put("jungle_fence_gate", "ジャングルのフェンスゲート");
        BLOCK_NAMES.put("acacia_fence_gate", "アカシアのフェンスゲート");
        BLOCK_NAMES.put("dark_oak_fence_gate", "ダークオークのフェンスゲート");
        BLOCK_NAMES.put("mangrove_fence_gate", "マングローブのフェンスゲート");
        BLOCK_NAMES.put("cherry_fence_gate", "サクラのフェンスゲート");
        BLOCK_NAMES.put("bamboo_fence_gate", "竹のフェンスゲート");
        BLOCK_NAMES.put("crimson_fence_gate", "真紅のフェンスゲート");
        BLOCK_NAMES.put("warped_fence_gate", "歪んだフェンスゲート");

        // 自然ブロック
        BLOCK_NAMES.put("grass_block", "草ブロック");
        BLOCK_NAMES.put("dirt", "土");
        BLOCK_NAMES.put("coarse_dirt", "粗い土");
        BLOCK_NAMES.put("podzol", "ポドゾル");
        BLOCK_NAMES.put("rooted_dirt", "根付いた土");
        BLOCK_NAMES.put("mud", "泥");
        BLOCK_NAMES.put("packed_mud", "固めた泥");
        BLOCK_NAMES.put("clay", "粘土");
        BLOCK_NAMES.put("gravel", "砂利");
        BLOCK_NAMES.put("sand", "砂");
        BLOCK_NAMES.put("red_sand", "赤い砂");
        BLOCK_NAMES.put("soul_sand", "ソウルサンド");
        BLOCK_NAMES.put("soul_soil", "ソウルソイル");
        BLOCK_NAMES.put("mycelium", "菌糸");
        BLOCK_NAMES.put("moss_block", "苔ブロック");
        BLOCK_NAMES.put("snow_block", "雪ブロック");
        BLOCK_NAMES.put("ice", "氷");
        BLOCK_NAMES.put("packed_ice", "氷塊");
        BLOCK_NAMES.put("blue_ice", "青氷");
        BLOCK_NAMES.put("obsidian", "黒曜石");
        BLOCK_NAMES.put("crying_obsidian", "泣く黒曜石");
        BLOCK_NAMES.put("bedrock", "岩盤");
        BLOCK_NAMES.put("netherrack", "ネザーラック");
        BLOCK_NAMES.put("end_stone", "エンドストーン");
        BLOCK_NAMES.put("glowstone", "グロウストーン");
        BLOCK_NAMES.put("sea_lantern", "シーランタン");
        BLOCK_NAMES.put("shroomlight", "シュルームライト");

        // 鉱石ブロック
        BLOCK_NAMES.put("coal_block", "石炭ブロック");
        BLOCK_NAMES.put("iron_block", "鉄ブロック");
        BLOCK_NAMES.put("gold_block", "金ブロック");
        BLOCK_NAMES.put("diamond_block", "ダイヤモンドブロック");
        BLOCK_NAMES.put("emerald_block", "エメラルドブロック");
        BLOCK_NAMES.put("lapis_block", "ラピスラズリブロック");
        BLOCK_NAMES.put("redstone_block", "レッドストーンブロック");
        BLOCK_NAMES.put("netherite_block", "ネザライトブロック");
        BLOCK_NAMES.put("copper_block", "銅ブロック");
        BLOCK_NAMES.put("raw_iron_block", "鉄の原石ブロック");
        BLOCK_NAMES.put("raw_gold_block", "金の原石ブロック");
        BLOCK_NAMES.put("raw_copper_block", "銅の原石ブロック");
        BLOCK_NAMES.put("amethyst_block", "アメジストブロック");

        // 羊毛
        BLOCK_NAMES.put("white_wool", "白色の羊毛");
        BLOCK_NAMES.put("orange_wool", "橙色の羊毛");
        BLOCK_NAMES.put("magenta_wool", "赤紫色の羊毛");
        BLOCK_NAMES.put("light_blue_wool", "空色の羊毛");
        BLOCK_NAMES.put("yellow_wool", "黄色の羊毛");
        BLOCK_NAMES.put("lime_wool", "黄緑色の羊毛");
        BLOCK_NAMES.put("pink_wool", "桃色の羊毛");
        BLOCK_NAMES.put("gray_wool", "灰色の羊毛");
        BLOCK_NAMES.put("light_gray_wool", "薄灰色の羊毛");
        BLOCK_NAMES.put("cyan_wool", "青緑色の羊毛");
        BLOCK_NAMES.put("purple_wool", "紫色の羊毛");
        BLOCK_NAMES.put("blue_wool", "青色の羊毛");
        BLOCK_NAMES.put("brown_wool", "茶色の羊毛");
        BLOCK_NAMES.put("green_wool", "緑色の羊毛");
        BLOCK_NAMES.put("red_wool", "赤色の羊毛");
        BLOCK_NAMES.put("black_wool", "黒色の羊毛");

        // ガラス
        BLOCK_NAMES.put("glass", "ガラス");
        BLOCK_NAMES.put("tinted_glass", "遮光ガラス");
        BLOCK_NAMES.put("white_stained_glass", "白色の色付きガラス");
        BLOCK_NAMES.put("orange_stained_glass", "橙色の色付きガラス");
        BLOCK_NAMES.put("magenta_stained_glass", "赤紫色の色付きガラス");
        BLOCK_NAMES.put("light_blue_stained_glass", "空色の色付きガラス");
        BLOCK_NAMES.put("yellow_stained_glass", "黄色の色付きガラス");
        BLOCK_NAMES.put("lime_stained_glass", "黄緑色の色付きガラス");
        BLOCK_NAMES.put("pink_stained_glass", "桃色の色付きガラス");
        BLOCK_NAMES.put("gray_stained_glass", "灰色の色付きガラス");
        BLOCK_NAMES.put("light_gray_stained_glass", "薄灰色の色付きガラス");
        BLOCK_NAMES.put("cyan_stained_glass", "青緑色の色付きガラス");
        BLOCK_NAMES.put("purple_stained_glass", "紫色の色付きガラス");
        BLOCK_NAMES.put("blue_stained_glass", "青色の色付きガラス");
        BLOCK_NAMES.put("brown_stained_glass", "茶色の色付きガラス");
        BLOCK_NAMES.put("green_stained_glass", "緑色の色付きガラス");
        BLOCK_NAMES.put("red_stained_glass", "赤色の色付きガラス");
        BLOCK_NAMES.put("black_stained_glass", "黒色の色付きガラス");

        // 機能ブロック
        BLOCK_NAMES.put("crafting_table", "作業台");
        BLOCK_NAMES.put("furnace", "かまど");
        BLOCK_NAMES.put("blast_furnace", "溶鉱炉");
        BLOCK_NAMES.put("smoker", "燻製器");
        BLOCK_NAMES.put("chest", "チェスト");
        BLOCK_NAMES.put("barrel", "樽");
        BLOCK_NAMES.put("ender_chest", "エンダーチェスト");
        BLOCK_NAMES.put("shulker_box", "シュルカーボックス");
        BLOCK_NAMES.put("anvil", "金床");
        BLOCK_NAMES.put("enchanting_table", "エンチャントテーブル");
        BLOCK_NAMES.put("brewing_stand", "醸造台");
        BLOCK_NAMES.put("cauldron", "大釜");
        BLOCK_NAMES.put("beacon", "ビーコン");
        BLOCK_NAMES.put("hopper", "ホッパー");
        BLOCK_NAMES.put("dispenser", "ディスペンサー");
        BLOCK_NAMES.put("dropper", "ドロッパー");
        BLOCK_NAMES.put("observer", "オブザーバー");
        BLOCK_NAMES.put("piston", "ピストン");
        BLOCK_NAMES.put("sticky_piston", "粘着ピストン");
        BLOCK_NAMES.put("tnt", "TNT");
        BLOCK_NAMES.put("lever", "レバー");
        BLOCK_NAMES.put("tripwire_hook", "トリップワイヤーフック");
        BLOCK_NAMES.put("daylight_detector", "日照センサー");
        BLOCK_NAMES.put("note_block", "音符ブロック");
        BLOCK_NAMES.put("jukebox", "ジュークボックス");
        BLOCK_NAMES.put("campfire", "焚き火");
        BLOCK_NAMES.put("soul_campfire", "魂の焚き火");
        BLOCK_NAMES.put("lantern", "ランタン");
        BLOCK_NAMES.put("soul_lantern", "魂のランタン");
        BLOCK_NAMES.put("torch", "松明");
        BLOCK_NAMES.put("soul_torch", "魂の松明");
        BLOCK_NAMES.put("redstone_torch", "レッドストーントーチ");
        BLOCK_NAMES.put("redstone_lamp", "レッドストーンランプ");
        BLOCK_NAMES.put("target", "的");
        BLOCK_NAMES.put("bell", "鐘");
        BLOCK_NAMES.put("lectern", "書見台");
        BLOCK_NAMES.put("composter", "コンポスター");
        BLOCK_NAMES.put("grindstone", "砥石");
        BLOCK_NAMES.put("stonecutter", "石切台");
        BLOCK_NAMES.put("loom", "機織り機");
        BLOCK_NAMES.put("cartography_table", "製図台");
        BLOCK_NAMES.put("smithing_table", "鍛冶台");
        BLOCK_NAMES.put("fletching_table", "矢細工台");

        // その他のブロック
        BLOCK_NAMES.put("air", "空気");
        BLOCK_NAMES.put("water", "水");
        BLOCK_NAMES.put("lava", "溶岩");
        BLOCK_NAMES.put("sponge", "スポンジ");
        BLOCK_NAMES.put("wet_sponge", "濡れたスポンジ");
        BLOCK_NAMES.put("hay_block", "干草の俵");
        BLOCK_NAMES.put("honey_block", "ハチミツブロック");
        BLOCK_NAMES.put("honeycomb_block", "ハニカムブロック");
        BLOCK_NAMES.put("slime_block", "スライムブロック");
        BLOCK_NAMES.put("melon", "スイカ");
        BLOCK_NAMES.put("pumpkin", "カボチャ");
        BLOCK_NAMES.put("carved_pumpkin", "くり抜かれたカボチャ");
        BLOCK_NAMES.put("jack_o_lantern", "ジャック・オ・ランタン");
        BLOCK_NAMES.put("bookshelf", "本棚");
        BLOCK_NAMES.put("chiseled_bookshelf", "模様入りの本棚");
        BLOCK_NAMES.put("ladder", "はしご");
        BLOCK_NAMES.put("scaffolding", "足場");
        BLOCK_NAMES.put("chain", "鎖");
        BLOCK_NAMES.put("iron_bars", "鉄格子");
        BLOCK_NAMES.put("glass_pane", "板ガラス");

        // 原木
        BLOCK_NAMES.put("oak_log", "オークの原木");
        BLOCK_NAMES.put("spruce_log", "トウヒの原木");
        BLOCK_NAMES.put("birch_log", "シラカバの原木");
        BLOCK_NAMES.put("jungle_log", "ジャングルの原木");
        BLOCK_NAMES.put("acacia_log", "アカシアの原木");
        BLOCK_NAMES.put("dark_oak_log", "ダークオークの原木");
        BLOCK_NAMES.put("mangrove_log", "マングローブの原木");
        BLOCK_NAMES.put("cherry_log", "サクラの原木");
        BLOCK_NAMES.put("crimson_stem", "真紅の幹");
        BLOCK_NAMES.put("warped_stem", "歪んだ幹");

        // 葉
        BLOCK_NAMES.put("oak_leaves", "オークの葉");
        BLOCK_NAMES.put("spruce_leaves", "トウヒの葉");
        BLOCK_NAMES.put("birch_leaves", "シラカバの葉");
        BLOCK_NAMES.put("jungle_leaves", "ジャングルの葉");
        BLOCK_NAMES.put("acacia_leaves", "アカシアの葉");
        BLOCK_NAMES.put("dark_oak_leaves", "ダークオークの葉");
        BLOCK_NAMES.put("mangrove_leaves", "マングローブの葉");
        BLOCK_NAMES.put("cherry_leaves", "サクラの葉");
        BLOCK_NAMES.put("azalea_leaves", "ツツジの葉");
        BLOCK_NAMES.put("flowering_azalea_leaves", "開花したツツジの葉");

        // ドア・トラップドア
        BLOCK_NAMES.put("oak_door", "オークのドア");
        BLOCK_NAMES.put("spruce_door", "トウヒのドア");
        BLOCK_NAMES.put("birch_door", "シラカバのドア");
        BLOCK_NAMES.put("jungle_door", "ジャングルのドア");
        BLOCK_NAMES.put("acacia_door", "アカシアのドア");
        BLOCK_NAMES.put("dark_oak_door", "ダークオークのドア");
        BLOCK_NAMES.put("mangrove_door", "マングローブのドア");
        BLOCK_NAMES.put("cherry_door", "サクラのドア");
        BLOCK_NAMES.put("bamboo_door", "竹のドア");
        BLOCK_NAMES.put("crimson_door", "真紅のドア");
        BLOCK_NAMES.put("warped_door", "歪んだドア");
        BLOCK_NAMES.put("iron_door", "鉄のドア");

        BLOCK_NAMES.put("oak_trapdoor", "オークのトラップドア");
        BLOCK_NAMES.put("spruce_trapdoor", "トウヒのトラップドア");
        BLOCK_NAMES.put("birch_trapdoor", "シラカバのトラップドア");
        BLOCK_NAMES.put("jungle_trapdoor", "ジャングルのトラップドア");
        BLOCK_NAMES.put("acacia_trapdoor", "アカシアのトラップドア");
        BLOCK_NAMES.put("dark_oak_trapdoor", "ダークオークのトラップドア");
        BLOCK_NAMES.put("mangrove_trapdoor", "マングローブのトラップドア");
        BLOCK_NAMES.put("cherry_trapdoor", "サクラのトラップドア");
        BLOCK_NAMES.put("bamboo_trapdoor", "竹のトラップドア");
        BLOCK_NAMES.put("crimson_trapdoor", "真紅のトラップドア");
        BLOCK_NAMES.put("warped_trapdoor", "歪んだトラップドア");
        BLOCK_NAMES.put("iron_trapdoor", "鉄のトラップドア");

        // ボタン
        BLOCK_NAMES.put("oak_button", "オークのボタン");
        BLOCK_NAMES.put("spruce_button", "トウヒのボタン");
        BLOCK_NAMES.put("birch_button", "シラカバのボタン");
        BLOCK_NAMES.put("jungle_button", "ジャングルのボタン");
        BLOCK_NAMES.put("acacia_button", "アカシアのボタン");
        BLOCK_NAMES.put("dark_oak_button", "ダークオークのボタン");
        BLOCK_NAMES.put("mangrove_button", "マングローブのボタン");
        BLOCK_NAMES.put("cherry_button", "サクラのボタン");
        BLOCK_NAMES.put("bamboo_button", "竹のボタン");
        BLOCK_NAMES.put("crimson_button", "真紅のボタン");
        BLOCK_NAMES.put("warped_button", "歪んだボタン");
        BLOCK_NAMES.put("stone_button", "石のボタン");
        BLOCK_NAMES.put("polished_blackstone_button", "磨かれた黒石のボタン");

        // 感圧板
        BLOCK_NAMES.put("oak_pressure_plate", "オークの感圧板");
        BLOCK_NAMES.put("spruce_pressure_plate", "トウヒの感圧板");
        BLOCK_NAMES.put("birch_pressure_plate", "シラカバの感圧板");
        BLOCK_NAMES.put("jungle_pressure_plate", "ジャングルの感圧板");
        BLOCK_NAMES.put("acacia_pressure_plate", "アカシアの感圧板");
        BLOCK_NAMES.put("dark_oak_pressure_plate", "ダークオークの感圧板");
        BLOCK_NAMES.put("mangrove_pressure_plate", "マングローブの感圧板");
        BLOCK_NAMES.put("cherry_pressure_plate", "サクラの感圧板");
        BLOCK_NAMES.put("bamboo_pressure_plate", "竹の感圧板");
        BLOCK_NAMES.put("crimson_pressure_plate", "真紅の感圧板");
        BLOCK_NAMES.put("warped_pressure_plate", "歪んだの感圧板");
        BLOCK_NAMES.put("stone_pressure_plate", "石の感圧板");
        BLOCK_NAMES.put("polished_blackstone_pressure_plate", "磨かれた黒石の感圧板");
        BLOCK_NAMES.put("light_weighted_pressure_plate", "軽量カバー感圧板");
        BLOCK_NAMES.put("heavy_weighted_pressure_plate", "重量カバー感圧板");

        // 鉱石
        BLOCK_NAMES.put("coal_ore", "石炭鉱石");
        BLOCK_NAMES.put("deepslate_coal_ore", "深層石炭鉱石");
        BLOCK_NAMES.put("iron_ore", "鉄鉱石");
        BLOCK_NAMES.put("deepslate_iron_ore", "深層鉄鉱石");
        BLOCK_NAMES.put("copper_ore", "銅鉱石");
        BLOCK_NAMES.put("deepslate_copper_ore", "深層銅鉱石");
        BLOCK_NAMES.put("gold_ore", "金鉱石");
        BLOCK_NAMES.put("deepslate_gold_ore", "深層金鉱石");
        BLOCK_NAMES.put("nether_gold_ore", "ネザー金鉱石");
        BLOCK_NAMES.put("redstone_ore", "レッドストーン鉱石");
        BLOCK_NAMES.put("deepslate_redstone_ore", "深層レッドストーン鉱石");
        BLOCK_NAMES.put("emerald_ore", "エメラルド鉱石");
        BLOCK_NAMES.put("deepslate_emerald_ore", "深層エメラルド鉱石");
        BLOCK_NAMES.put("lapis_ore", "ラピスラズリ鉱石");
        BLOCK_NAMES.put("deepslate_lapis_ore", "深層ラピスラズリ鉱石");
        BLOCK_NAMES.put("diamond_ore", "ダイヤモンド鉱石");
        BLOCK_NAMES.put("deepslate_diamond_ore", "深層ダイヤモンド鉱石");
        BLOCK_NAMES.put("nether_quartz_ore", "ネザークォーツ鉱石");
        BLOCK_NAMES.put("ancient_debris", "古代の残骸");

        // 注: 必要に応じて追加
    }

    /**
     * 英語のブロック名を日本語に変換
     * @param englishName 英語のブロック内部名（例: "stone"）
     * @return 日本語名。翻訳がない場合は英語名をそのまま返す
     */
    public static String getJapaneseName(String englishName) {
        return BLOCK_NAMES.getOrDefault(englishName, englishName);
    }

    /**
     * 翻訳が存在するかチェック
     * @param englishName 英語のブロック内部名
     * @return 翻訳が存在する場合true
     */
    public static boolean hasTranslation(String englishName) {
        return BLOCK_NAMES.containsKey(englishName);
    }
}
```

---

### 2. CommandExecutor.java の修正

#### 2.1 インポート追加

```java
import com.github.minecraftedu.util.BlockNameTranslator;
import net.minecraft.network.chat.Component;
```

#### 2.2 チャット表示用ヘルパーメソッド追加

```java
// ========================================
// チャット表示用ヘルパーメソッド
// ========================================

/**
 * ゲーム内チャットにメッセージを送信
 * @param message 表示するメッセージ
 */
private void sendChatMessage(String message) {
    server.execute(() -> {
        server.getPlayerList().getPlayers().forEach(player -> {
            player.sendSystemMessage(Component.literal("[MinecraftEdu] " + message));
        });
    });
}

/**
 * 時刻を日本語名に変換
 * @param time 時刻（tick）
 * @return 日本語の時刻名
 */
private String getTimeNameJapanese(long time) {
    if (time == 0 || time == 24000) return "夜明け";
    if (time == 1000) return "朝";
    if (time == 6000) return "正午";
    if (time == 12000) return "夕方";
    if (time == 13000) return "夜";
    if (time == 18000) return "真夜中";
    return time + " tick";
}

/**
 * 天気を日本語名に変換
 * @param weather 天気（英語）
 * @return 日本語の天気名
 */
private String getWeatherNameJapanese(String weather) {
    switch (weather) {
        case "clear": return "晴れ";
        case "rain": return "雨";
        case "thunder": return "雷雨";
        default: return weather;
    }
}

/**
 * ゲームルールを日本語名に変換
 * @param rule ゲームルール名（英語）
 * @return 日本語のルール名
 */
private String getGameRuleNameJapanese(String rule) {
    switch (rule) {
        case "doDaylightCycle": return "時刻固定";
        case "doWeatherCycle": return "天気固定";
        case "doMobSpawning": return "モブスポーン";
        default: return rule;
    }
}

/**
 * ON/OFFを日本語に変換（ゲームルール用、反転考慮）
 * @param rule ルール名
 * @param value 値
 * @return 日本語のON/OFF
 */
private String getOnOffJapanese(String rule, boolean value) {
    // doDaylightCycle と doWeatherCycle は反転されているので、
    // true が送られてきた場合は実際には「固定ON」の意味
    if (rule.equals("doDaylightCycle") || rule.equals("doWeatherCycle")) {
        return value ? "ON" : "OFF";
    }
    // doMobSpawning は反転なし
    return value ? "ON" : "OFF";
}
```

#### 2.3 各メソッドへのチャット表示追加

##### executeSetTime (行493付近)

```java
private boolean executeSetTime(JsonObject params) {
    long time = params.get("time").getAsLong();

    server.execute(() -> {
        ServerLevel world = server.overworld();
        world.setDayTime(time);
    });

    // チャット表示を追加
    sendChatMessage("時刻: " + getTimeNameJapanese(time));

    MinecraftEduMod.LOGGER.info("Time set to: " + time);
    return true;
}
```

##### executeSetWeather (行470付近)

```java
private boolean executeSetWeather(JsonObject params) {
    String weather = params.get("weather").getAsString();

    server.execute(() -> {
        ServerLevel world = server.overworld();
        switch (weather) {
            case "clear":
                world.setWeatherParameters(6000, 0, false, false);
                break;
            case "rain":
                world.setWeatherParameters(0, 6000, true, false);
                break;
            case "thunder":
                world.setWeatherParameters(0, 6000, true, true);
                break;
        }
    });

    // チャット表示を追加
    sendChatMessage("天気: " + getWeatherNameJapanese(weather));

    MinecraftEduMod.LOGGER.info("Weather set to: " + weather);
    return true;
}
```

##### executeSetGameRule (行564付近)

```java
private boolean executeSetGameRule(JsonObject params) {
    String rule = params.get("rule").getAsString();
    String value = params.get("value").getAsString();
    boolean boolValue = value.equalsIgnoreCase("true");

    server.execute(() -> {
        ServerLevel world = server.overworld();
        net.minecraft.world.level.GameRules gameRules = world.getGameRules();

        switch (rule) {
            case "doDaylightCycle":
                boolean invertedDaylightValue = !boolValue;
                gameRules.getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                    .set(invertedDaylightValue, server);
                MinecraftEduMod.LOGGER.info("GameRule set: doDaylightCycle = " + invertedDaylightValue);
                break;
            case "doWeatherCycle":
                boolean invertedWeatherValue = !boolValue;
                gameRules.getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE)
                    .set(invertedWeatherValue, server);
                MinecraftEduMod.LOGGER.info("GameRule set: doWeatherCycle = " + invertedWeatherValue);
                break;
            case "doMobSpawning":
                gameRules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)
                    .set(boolValue, server);
                MinecraftEduMod.LOGGER.info("GameRule set: doMobSpawning = " + boolValue);
                break;
            default:
                MinecraftEduMod.LOGGER.warn("Unknown game rule: " + rule);
                return;
        }
    });

    // チャット表示を追加
    sendChatMessage(getGameRuleNameJapanese(rule) + ": " + getOnOffJapanese(rule, boolValue));

    lastResult.addProperty("gameRule", rule);
    lastResult.addProperty("value", value);
    lastResult.addProperty("success", true);

    return true;
}
```

##### executeGetBlockType (行379付近)

```java
private boolean executeGetBlockType(JsonObject params) {
    int x = params.get("x").getAsInt();
    int y = params.get("y").getAsInt();
    int z = params.get("z").getAsInt();

    BlockPos pos = new BlockPos(x, y, z);
    ServerLevel world = server.overworld();

    BlockState blockState = world.getBlockState(pos);
    Block block = blockState.getBlock();
    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

    if (blockId == null) {
        MinecraftEduMod.LOGGER.warn("Failed to get block ID at " + x + "," + y + "," + z);
        return false;
    }

    String blockType = blockId.toString();
    if (blockType.startsWith("minecraft:")) {
        blockType = blockType.substring(10);
    }

    // 日本語名を取得
    String japaneseName = BlockNameTranslator.getJapaneseName(blockType);

    // チャット表示を追加（日本語名）
    sendChatMessage("ブロック: " + japaneseName + " (" + x + ", " + y + ", " + z + ")");

    MinecraftEduMod.LOGGER.info("Block type retrieved: " + blockType + " at " + x + "," + y + "," + z);

    lastResult.addProperty("blockType", blockType);
    JsonObject position = new JsonObject();
    position.addProperty("x", x);
    position.addProperty("y", y);
    position.addProperty("z", z);
    lastResult.add("position", position);

    return true;
}
```

##### executeSetNightVision (行834付近)

```java
private boolean executeSetNightVision(JsonObject params) {
    if (!params.has("enabled")) {
        MinecraftEduMod.LOGGER.warn("setNightVision: enabled parameter required");
        return false;
    }

    boolean enabled = params.get("enabled").getAsBoolean();

    server.execute(() -> {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (enabled) {
                MobEffectInstance effect = new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    999999,
                    0,
                    true,
                    false
                );
                player.addEffect(effect);
                MinecraftEduMod.LOGGER.info("Night vision enabled for player: " + player.getName().getString());
            } else {
                player.removeEffect(MobEffects.NIGHT_VISION);
                MinecraftEduMod.LOGGER.info("Night vision disabled for player: " + player.getName().getString());
            }
        }
    });

    // チャット表示を追加
    sendChatMessage("暗視: " + (enabled ? "ON" : "OFF"));

    lastResult.addProperty("nightVision", enabled);

    return true;
}
```

##### executeSetMoveSpeed (行791付近)

```java
// 最後に追加
sendChatMessage("移動速度: " + finalMultiplier + "倍");
```

##### executeSetFlySpeed (行874付近)

```java
// 最後に追加
sendChatMessage("飛行速度: " + finalMultiplier + "倍");
```

##### executeSetEntitySpawning (行1092付近)

```java
private boolean executeSetEntitySpawning(JsonObject params) {
    boolean enabled = params.get("enabled").getAsBoolean();
    this.entitySpawningAllowed = enabled;

    // チャット表示を追加
    sendChatMessage("エンティティ召喚: " + (enabled ? "許可" : "禁止"));

    MinecraftEduMod.LOGGER.info("Entity spawning " + (enabled ? "enabled" : "disabled") + " via WebSocket");
    lastResult.addProperty("entitySpawningAllowed", enabled);

    return true;
}
```

---

## チャット表示の一覧

| コマンド | 表示例 |
|----------|--------|
| setTime | `[MinecraftEdu] 時刻: 正午` |
| setWeather | `[MinecraftEdu] 天気: 晴れ` |
| setGameRule (時刻固定) | `[MinecraftEdu] 時刻固定: ON` |
| setGameRule (天気固定) | `[MinecraftEdu] 天気固定: ON` |
| setGameRule (モブスポーン) | `[MinecraftEdu] モブスポーン: OFF` |
| getBlockType | `[MinecraftEdu] ブロック: 石 (0, 64, 0)` |
| setNightVision | `[MinecraftEdu] 暗視: ON` |
| setMoveSpeed | `[MinecraftEdu] 移動速度: 1.0倍` |
| setFlySpeed | `[MinecraftEdu] 飛行速度: 2.0倍` |
| setEntitySpawning | `[MinecraftEdu] エンティティ召喚: 禁止` |

---

## 実装手順

1. **BlockNameTranslator.java 作成**
   - `minecraft-mod/src/main/java/com/github/minecraftedu/util/` ディレクトリ作成
   - BlockNameTranslator.java を作成
   - gui.jsから抽出した日本語ブロック名マッピングを追加

2. **CommandExecutor.java 修正**
   - インポート追加
   - ヘルパーメソッド追加（sendChatMessage, getTimeNameJapanese 等）
   - 各executeメソッドにチャット表示を追加

3. **MODビルド**
   ```bash
   cd minecraft-mod
   ./gradlew build
   ```

4. **JARをmodsフォルダにコピー**
   ```bash
   cp build/libs/minecraftedu-mod-0.1.0-1.20.1.jar "/c/Users/riyum/AppData/Roaming/.minecraft_1.20.1/mods/"
   ```

5. **動作確認**
   - Minecraftを起動
   - Scratchから接続
   - 各ブロックを実行してチャット表示を確認

---

## 注意事項

- ブロック名マッピングは約350件を手動で追加
- 翻訳がないブロックは英語名がそのまま表示される
- 将来的にブロックが追加された場合は、マッピングの更新が必要

---

## 作成日

2026年1月15日
