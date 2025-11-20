package com.github.minecraftedu.init;

import com.github.minecraftedu.MinecraftEduMod;
import com.github.minecraftedu.block.VerticalSlabBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Block registration for custom blocks
 *
 * This class manages the registration of all custom blocks in the MinecraftEdu MOD.
 * Currently includes vertical slab variants for directional placement.
 */
public class ModBlocks {

    /**
     * Deferred register for blocks
     * Uses the MOD_ID from MinecraftEduMod to properly namespace all custom blocks
     */
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, MinecraftEduMod.MOD_ID);

    /**
     * Vertical Oak Slab - Prototype vertical slab block
     *
     * Properties:
     * - facing: north, south, east, west (horizontal directional placement)
     * - waterlogged: true/false (compatibility with water)
     * - Collision box: 8x16x16 (half width, full height and depth)
     *
     * Unlike vanilla slabs which only have vertical positioning (bottom/top/double),
     * vertical slabs can be placed facing any of the 4 horizontal directions.
     */
    public static final RegistryObject<Block> VERTICAL_OAK_SLAB = BLOCKS.register(
        "vertical_oak_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
                .strength(2.0F, 3.0F)  // Same as oak planks
                .requiresCorrectToolForDrops()
        )
    );

    // ========================================
    // 木材系垂直スラブ (Wood Vertical Slabs)
    // ========================================

    public static final RegistryObject<Block> VERTICAL_BIRCH_SLAB = BLOCKS.register(
        "vertical_birch_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.BIRCH_PLANKS)
                .strength(2.0F, 3.0F)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_SPRUCE_SLAB = BLOCKS.register(
        "vertical_spruce_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.SPRUCE_PLANKS)
                .strength(2.0F, 3.0F)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_JUNGLE_SLAB = BLOCKS.register(
        "vertical_jungle_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.JUNGLE_PLANKS)
                .strength(2.0F, 3.0F)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_ACACIA_SLAB = BLOCKS.register(
        "vertical_acacia_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.ACACIA_PLANKS)
                .strength(2.0F, 3.0F)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_DARK_OAK_SLAB = BLOCKS.register(
        "vertical_dark_oak_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS)
                .strength(2.0F, 3.0F)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_CHERRY_SLAB = BLOCKS.register(
        "vertical_cherry_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.CHERRY_PLANKS)
                .strength(2.0F, 3.0F)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_MANGROVE_SLAB = BLOCKS.register(
        "vertical_mangrove_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.MANGROVE_PLANKS)
                .strength(2.0F, 3.0F)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_CRIMSON_SLAB = BLOCKS.register(
        "vertical_crimson_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.CRIMSON_PLANKS)
                .strength(2.0F, 3.0F)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_WARPED_SLAB = BLOCKS.register(
        "vertical_warped_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.WARPED_PLANKS)
                .strength(2.0F, 3.0F)
                .requiresCorrectToolForDrops()
        )
    );

    // ========================================
    // 石材系垂直スラブ (Stone Vertical Slabs)
    // ========================================

    public static final RegistryObject<Block> VERTICAL_STONE_SLAB = BLOCKS.register(
        "vertical_stone_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.STONE)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_COBBLESTONE_SLAB = BLOCKS.register(
        "vertical_cobblestone_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_STONE_BRICK_SLAB = BLOCKS.register(
        "vertical_stone_brick_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_SMOOTH_STONE_SLAB = BLOCKS.register(
        "vertical_smooth_stone_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.SMOOTH_STONE)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_ANDESITE_SLAB = BLOCKS.register(
        "vertical_andesite_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.ANDESITE)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_GRANITE_SLAB = BLOCKS.register(
        "vertical_granite_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.GRANITE)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_DIORITE_SLAB = BLOCKS.register(
        "vertical_diorite_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.DIORITE)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_SANDSTONE_SLAB = BLOCKS.register(
        "vertical_sandstone_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.SANDSTONE)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_BRICK_SLAB = BLOCKS.register(
        "vertical_brick_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.BRICKS)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_QUARTZ_SLAB = BLOCKS.register(
        "vertical_quartz_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.QUARTZ_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    // ========================================
    // 鉱石・鉱物系垂直スラブ (Ore/Mineral Vertical Slabs)
    // ========================================

    public static final RegistryObject<Block> VERTICAL_IRON_BLOCK_SLAB = BLOCKS.register(
        "vertical_iron_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_GOLD_BLOCK_SLAB = BLOCKS.register(
        "vertical_gold_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_DIAMOND_BLOCK_SLAB = BLOCKS.register(
        "vertical_diamond_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.DIAMOND_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_EMERALD_BLOCK_SLAB = BLOCKS.register(
        "vertical_emerald_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.EMERALD_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_COPPER_BLOCK_SLAB = BLOCKS.register(
        "vertical_copper_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_LAPIS_BLOCK_SLAB = BLOCKS.register(
        "vertical_lapis_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.LAPIS_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_REDSTONE_BLOCK_SLAB = BLOCKS.register(
        "vertical_redstone_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.REDSTONE_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_COAL_BLOCK_SLAB = BLOCKS.register(
        "vertical_coal_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.COAL_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_NETHERITE_BLOCK_SLAB = BLOCKS.register(
        "vertical_netherite_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_AMETHYST_BLOCK_SLAB = BLOCKS.register(
        "vertical_amethyst_block_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                .requiresCorrectToolForDrops()
        )
    );

    // ========================================
    // 銅系垂直スラブ - 日本建築の屋根用 (Copper Vertical Slabs - For Japanese Architecture Roofs)
    // ========================================

    public static final RegistryObject<Block> VERTICAL_CUT_COPPER_SLAB = BLOCKS.register(
        "vertical_cut_copper_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.CUT_COPPER)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_EXPOSED_CUT_COPPER_SLAB = BLOCKS.register(
        "vertical_exposed_cut_copper_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.EXPOSED_CUT_COPPER)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_WEATHERED_CUT_COPPER_SLAB = BLOCKS.register(
        "vertical_weathered_cut_copper_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.WEATHERED_CUT_COPPER)
                .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<Block> VERTICAL_OXIDIZED_CUT_COPPER_SLAB = BLOCKS.register(
        "vertical_oxidized_cut_copper_slab",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.OXIDIZED_CUT_COPPER)
                .requiresCorrectToolForDrops()
        )
    );
}
