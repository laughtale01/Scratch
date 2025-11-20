# 銅系ハーフブロックの実装状況調査レポート

## 調査日時
2025-11-20

## 調査結果サマリー

### ❌ 実装されていないもの（バニラMinecraftに存在）

**通常のハーフブロック（Horizontal Slabs） - 8種類すべて未実装:**
1. `cut_copper_slab` - 切り込み入りの銅のハーフブロック
2. `exposed_cut_copper_slab` - 風化した切り込み入りの銅のハーフブロック
3. `weathered_cut_copper_slab` - 錆びた切り込み入りの銅のハーフブロック
4. `oxidized_cut_copper_slab` - 酸化した切り込み入りの銅のハーフブロック
5. `waxed_cut_copper_slab` - ワックスを塗った切り込み入りの銅のハーフブロック
6. `waxed_exposed_cut_copper_slab` - ワックスを塗った風化した切り込み入りの銅のハーフブロック
7. `waxed_weathered_cut_copper_slab` - ワックスを塗った錆びた切り込み入りの銅のハーフブロック
8. `waxed_oxidized_cut_copper_slab` - ワックスを塗った酸化した切り込み入りの銅のハーフブロック

**階段（Stairs） - 8種類すべて未実装:**
1. `cut_copper_stairs` - 切り込み入りの銅の階段
2. `exposed_cut_copper_stairs` - 風化した切り込み入りの銅の階段
3. `weathered_cut_copper_stairs` - 錆びた切り込み入りの銅の階段
4. `oxidized_cut_copper_stairs` - 酸化した切り込み入りの銅の階段
5. `waxed_cut_copper_stairs` - ワックス版（4種類）

### ✅ 実装されているもの

**垂直スラブ（Vertical Slabs） - 5種類実装済み（MOD独自機能）:**
1. `vertical_copper_block_slab` - 銅ブロックのスラブ（垂直）
2. `vertical_cut_copper_slab` - 切り込み入りの銅のスラブ（垂直）
3. `vertical_exposed_cut_copper_slab` - 風化した切り込み入りの銅のスラブ（垂直）
4. `vertical_oxidized_cut_copper_slab` - 酸化した切り込み入りの銅のスラブ（垂直）
5. `vertical_weathered_cut_copper_slab` - 錆びた切り込み入りの銅のスラブ（垂直）

**フルブロック - 9種類実装済み（ORE_BLOCKSカテゴリ）:**
1. `copper_ore` - 銅鉱石
2. `deepslate_copper_ore` - ディープスレート銅鉱石
3. `copper_block` - 銅ブロック
4. `raw_copper_block` - 銅の原石
5. `cut_copper` - 切り込み入りの銅
6. `oxidized_copper` - 錆びた銅
7. `waxed_copper_block` - ワックスを塗った銅ブロック
8. `waxed_cut_copper` - ワックスを塗った切り込み入りの銅
9. `weathered_copper` - 風化した銅

## 詳細分析

### バニラMinecraftの銅系ブロック仕様
- 銅ブロックは4段階の酸化状態がある（通常 → exposed → weathered → oxidized）
- 各状態でワックスをかけることで酸化を止められる
- 合計8バリエーション（4酸化状態 × 2（ワックス有無））

### 現在のMOD実装の特徴
1. **通常のハーフブロックと階段が完全に欠落**
   - バニラMinecraft 1.17で追加された重要なブロックが未実装

2. **垂直スラブのみ実装**
   - 垂直スラブはMOD独自機能（バニラには存在しない）
   - 5種類のみ（ワックス版なし、copper_blockベースのみ含む）

3. **酸化状態とワックス版の実装が不完全**
   - 垂直スラブ: ワックス版なし、4酸化状態も不完全
   - フルブロック: 一部の状態のみ実装

## 推奨事項

### 優先度：高
1. **Cut Copper Slab（通常版）8種類を追加**
   - バニラMinecraftに存在する基本的なブロック
   - プレイヤーの期待に応える

2. **Cut Copper Stairs 8種類を追加**
   - ハーフブロックとセットで提供すべき

### 優先度：中
3. **銅系フルブロックの全バリエーションを追加**
   - 現在一部のみ実装
   - 4酸化状態 × 2（ワックス）= 8種類すべて

4. **垂直スラブのワックス版を追加**
   - 現在の垂直スラブにもワックス版を追加して一貫性を保つ

### 優先度：低
5. **その他の銅系装飾ブロック**
   - Copper Door, Copper Trapdoor, Copper Grate, Copper Bulb など
   - Minecraft 1.21+ で追加された新しいブロック

## 影響範囲

### ユーザー体験
- 現在、バニラMinecraftに存在する基本的な銅系ハーフブロックと階段が使えない
- 建築の幅が制限されている
- MOD独自の垂直スラブは便利だが、標準的なハーフブロックの代替にはならない

### 実装作業
- カテゴリ: BUILDING_BLOCKS
- 追加ブロック数: 最低16個（ハーフブロック8 + 階段8）
- 日本語名の追加が必要

## 参照
- Minecraft Wiki: https://minecraft.wiki/w/Copper
- Minecraft Wiki: https://minecraft.wiki/w/Cut_Copper_Slab
- 実装確認ファイル: sorted_categories.json
