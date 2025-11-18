package com.github.minecraftedu.init;

import com.github.minecraftedu.MinecraftEduMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Item registration for custom items and block items
 *
 * This class manages the registration of all items in the MinecraftEdu MOD,
 * including BlockItems that represent blocks in the player's inventory.
 */
public class ModItems {

    /**
     * Deferred register for items
     * Uses the MOD_ID from MinecraftEduMod to properly namespace all custom items
     */
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, MinecraftEduMod.MOD_ID);

    /**
     * Vertical Oak Slab Item
     *
     * This is the item form of the vertical_oak_slab block.
     * When placed, it will place the VerticalSlabBlock with appropriate facing direction.
     */
    public static final RegistryObject<Item> VERTICAL_OAK_SLAB = ITEMS.register(
        "vertical_oak_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_OAK_SLAB.get(),
            new Item.Properties()
        )
    );

    // ========================================
    // 木材系垂直スラブアイテム (Wood Vertical Slab Items)
    // ========================================

    public static final RegistryObject<Item> VERTICAL_BIRCH_SLAB = ITEMS.register(
        "vertical_birch_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_BIRCH_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_SPRUCE_SLAB = ITEMS.register(
        "vertical_spruce_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_SPRUCE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_JUNGLE_SLAB = ITEMS.register(
        "vertical_jungle_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_JUNGLE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_ACACIA_SLAB = ITEMS.register(
        "vertical_acacia_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_ACACIA_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_DARK_OAK_SLAB = ITEMS.register(
        "vertical_dark_oak_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_DARK_OAK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_CHERRY_SLAB = ITEMS.register(
        "vertical_cherry_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_CHERRY_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_MANGROVE_SLAB = ITEMS.register(
        "vertical_mangrove_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_MANGROVE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_CRIMSON_SLAB = ITEMS.register(
        "vertical_crimson_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_CRIMSON_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WARPED_SLAB = ITEMS.register(
        "vertical_warped_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WARPED_SLAB.get(),
            new Item.Properties()
        )
    );

    // ========================================
    // 石材系垂直スラブアイテム (Stone Vertical Slab Items)
    // ========================================

    public static final RegistryObject<Item> VERTICAL_STONE_SLAB = ITEMS.register(
        "vertical_stone_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_STONE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_COBBLESTONE_SLAB = ITEMS.register(
        "vertical_cobblestone_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_COBBLESTONE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_STONE_BRICK_SLAB = ITEMS.register(
        "vertical_stone_brick_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_STONE_BRICK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_SMOOTH_STONE_SLAB = ITEMS.register(
        "vertical_smooth_stone_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_SMOOTH_STONE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_ANDESITE_SLAB = ITEMS.register(
        "vertical_andesite_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_ANDESITE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_GRANITE_SLAB = ITEMS.register(
        "vertical_granite_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_GRANITE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_DIORITE_SLAB = ITEMS.register(
        "vertical_diorite_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_DIORITE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_SANDSTONE_SLAB = ITEMS.register(
        "vertical_sandstone_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_SANDSTONE_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_BRICK_SLAB = ITEMS.register(
        "vertical_brick_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_BRICK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_QUARTZ_SLAB = ITEMS.register(
        "vertical_quartz_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_QUARTZ_SLAB.get(),
            new Item.Properties()
        )
    );

    // ========================================
    // 鉱石・鉱物系垂直スラブアイテム (Ore/Mineral Vertical Slab Items)
    // ========================================

    public static final RegistryObject<Item> VERTICAL_IRON_BLOCK_SLAB = ITEMS.register(
        "vertical_iron_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_IRON_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_GOLD_BLOCK_SLAB = ITEMS.register(
        "vertical_gold_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_GOLD_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_DIAMOND_BLOCK_SLAB = ITEMS.register(
        "vertical_diamond_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_DIAMOND_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_EMERALD_BLOCK_SLAB = ITEMS.register(
        "vertical_emerald_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_EMERALD_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_COPPER_BLOCK_SLAB = ITEMS.register(
        "vertical_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_LAPIS_BLOCK_SLAB = ITEMS.register(
        "vertical_lapis_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_LAPIS_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_REDSTONE_BLOCK_SLAB = ITEMS.register(
        "vertical_redstone_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_REDSTONE_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_COAL_BLOCK_SLAB = ITEMS.register(
        "vertical_coal_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_COAL_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_NETHERITE_BLOCK_SLAB = ITEMS.register(
        "vertical_netherite_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_NETHERITE_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_AMETHYST_BLOCK_SLAB = ITEMS.register(
        "vertical_amethyst_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_AMETHYST_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    // ========================================
    // 銅系垂直スラブアイテム - 日本建築の屋根用 (Copper Vertical Slab Items - For Japanese Architecture Roofs)
    // ========================================

    public static final RegistryObject<Item> VERTICAL_CUT_COPPER_SLAB = ITEMS.register(
        "vertical_cut_copper_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_CUT_COPPER_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_EXPOSED_CUT_COPPER_SLAB = ITEMS.register(
        "vertical_exposed_cut_copper_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_EXPOSED_CUT_COPPER_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WEATHERED_CUT_COPPER_SLAB = ITEMS.register(
        "vertical_weathered_cut_copper_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WEATHERED_CUT_COPPER_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_OXIDIZED_CUT_COPPER_SLAB = ITEMS.register(
        "vertical_oxidized_cut_copper_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_OXIDIZED_CUT_COPPER_SLAB.get(),
            new Item.Properties()
        )
    );

    // ========================================
    // Copper Slab Items
    // ========================================

    public static final RegistryObject<Item> VERTICAL_EXPOSED_COPPER_BLOCK_SLAB = ITEMS.register(
        "vertical_exposed_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_EXPOSED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WEATHERED_COPPER_BLOCK_SLAB = ITEMS.register(
        "vertical_weathered_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WEATHERED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_OXIDIZED_COPPER_BLOCK_SLAB = ITEMS.register(
        "vertical_oxidized_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_OXIDIZED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WAXED_COPPER_BLOCK_SLAB = ITEMS.register(
        "vertical_waxed_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WAXED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WAXED_EXPOSED_COPPER_BLOCK_SLAB = ITEMS.register(
        "vertical_waxed_exposed_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WAXED_EXPOSED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WAXED_WEATHERED_COPPER_BLOCK_SLAB = ITEMS.register(
        "vertical_waxed_weathered_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WAXED_WEATHERED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WAXED_OXIDIZED_COPPER_BLOCK_SLAB = ITEMS.register(
        "vertical_waxed_oxidized_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WAXED_OXIDIZED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WAXED_CUT_COPPER_SLAB = ITEMS.register(
        "vertical_waxed_cut_copper_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WAXED_CUT_COPPER_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WAXED_EXPOSED_CUT_COPPER_SLAB = ITEMS.register(
        "vertical_waxed_exposed_cut_copper_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WAXED_EXPOSED_CUT_COPPER_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WAXED_WEATHERED_CUT_COPPER_SLAB = ITEMS.register(
        "vertical_waxed_weathered_cut_copper_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WAXED_WEATHERED_CUT_COPPER_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> VERTICAL_WAXED_OXIDIZED_CUT_COPPER_SLAB = ITEMS.register(
        "vertical_waxed_oxidized_cut_copper_slab",
        () -> new BlockItem(
            ModBlocks.VERTICAL_WAXED_OXIDIZED_CUT_COPPER_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> COPPER_BLOCK_SLAB = ITEMS.register(
        "copper_block_slab",
        () -> new BlockItem(
            ModBlocks.COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> EXPOSED_COPPER_BLOCK_SLAB = ITEMS.register(
        "exposed_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.EXPOSED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> WEATHERED_COPPER_BLOCK_SLAB = ITEMS.register(
        "weathered_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.WEATHERED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> OXIDIZED_COPPER_BLOCK_SLAB = ITEMS.register(
        "oxidized_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.OXIDIZED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> WAXED_COPPER_BLOCK_SLAB = ITEMS.register(
        "waxed_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.WAXED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> WAXED_EXPOSED_COPPER_BLOCK_SLAB = ITEMS.register(
        "waxed_exposed_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.WAXED_EXPOSED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> WAXED_WEATHERED_COPPER_BLOCK_SLAB = ITEMS.register(
        "waxed_weathered_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.WAXED_WEATHERED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

    public static final RegistryObject<Item> WAXED_OXIDIZED_COPPER_BLOCK_SLAB = ITEMS.register(
        "waxed_oxidized_copper_block_slab",
        () -> new BlockItem(
            ModBlocks.WAXED_OXIDIZED_COPPER_BLOCK_SLAB.get(),
            new Item.Properties()
        )
    );

}
