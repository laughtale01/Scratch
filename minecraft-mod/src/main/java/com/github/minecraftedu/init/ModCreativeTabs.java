package com.github.minecraftedu.init;

import com.github.minecraftedu.MinecraftEduMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MinecraftEduMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MINECRAFTEDU_TAB = CREATIVE_MODE_TABS.register("minecraftedu_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.minecraftedu"))
            .icon(() -> new ItemStack(ModItems.VERTICAL_OAK_SLAB.get()))
            .displayItems((parameters, output) -> {
                // Wood vertical slabs
                output.accept(ModItems.VERTICAL_OAK_SLAB.get());
                output.accept(ModItems.VERTICAL_BIRCH_SLAB.get());
                output.accept(ModItems.VERTICAL_SPRUCE_SLAB.get());
                output.accept(ModItems.VERTICAL_JUNGLE_SLAB.get());
                output.accept(ModItems.VERTICAL_ACACIA_SLAB.get());
                output.accept(ModItems.VERTICAL_DARK_OAK_SLAB.get());
                output.accept(ModItems.VERTICAL_CHERRY_SLAB.get());
                output.accept(ModItems.VERTICAL_MANGROVE_SLAB.get());
                output.accept(ModItems.VERTICAL_CRIMSON_SLAB.get());
                output.accept(ModItems.VERTICAL_WARPED_SLAB.get());

                // Stone vertical slabs
                output.accept(ModItems.VERTICAL_STONE_SLAB.get());
                output.accept(ModItems.VERTICAL_COBBLESTONE_SLAB.get());
                output.accept(ModItems.VERTICAL_STONE_BRICK_SLAB.get());
                output.accept(ModItems.VERTICAL_SMOOTH_STONE_SLAB.get());
                output.accept(ModItems.VERTICAL_ANDESITE_SLAB.get());
                output.accept(ModItems.VERTICAL_GRANITE_SLAB.get());
                output.accept(ModItems.VERTICAL_DIORITE_SLAB.get());
                output.accept(ModItems.VERTICAL_SANDSTONE_SLAB.get());
                output.accept(ModItems.VERTICAL_BRICK_SLAB.get());
                output.accept(ModItems.VERTICAL_QUARTZ_SLAB.get());

                // Mineral vertical slabs
                output.accept(ModItems.VERTICAL_IRON_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_GOLD_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_DIAMOND_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_EMERALD_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_LAPIS_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_REDSTONE_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_COAL_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_NETHERITE_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_AMETHYST_BLOCK_SLAB.get());

                // Copper vertical slabs - cut copper variants
                output.accept(ModItems.VERTICAL_CUT_COPPER_SLAB.get());
                output.accept(ModItems.VERTICAL_EXPOSED_CUT_COPPER_SLAB.get());
                output.accept(ModItems.VERTICAL_WEATHERED_CUT_COPPER_SLAB.get());
                output.accept(ModItems.VERTICAL_OXIDIZED_CUT_COPPER_SLAB.get());

                // Copper vertical slabs - block variants with oxidation stages
                output.accept(ModItems.VERTICAL_EXPOSED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_WEATHERED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_OXIDIZED_COPPER_BLOCK_SLAB.get());

                // Waxed copper vertical slabs - block variants
                output.accept(ModItems.VERTICAL_WAXED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_WAXED_EXPOSED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_WAXED_WEATHERED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.VERTICAL_WAXED_OXIDIZED_COPPER_BLOCK_SLAB.get());

                // Waxed copper vertical slabs - cut copper variants
                output.accept(ModItems.VERTICAL_WAXED_CUT_COPPER_SLAB.get());
                output.accept(ModItems.VERTICAL_WAXED_EXPOSED_CUT_COPPER_SLAB.get());
                output.accept(ModItems.VERTICAL_WAXED_WEATHERED_CUT_COPPER_SLAB.get());
                output.accept(ModItems.VERTICAL_WAXED_OXIDIZED_CUT_COPPER_SLAB.get());

                // Horizontal copper slabs - normal oxidation stages
                output.accept(ModItems.COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.EXPOSED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.WEATHERED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.OXIDIZED_COPPER_BLOCK_SLAB.get());

                // Horizontal waxed copper slabs
                output.accept(ModItems.WAXED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.WAXED_EXPOSED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.WAXED_WEATHERED_COPPER_BLOCK_SLAB.get());
                output.accept(ModItems.WAXED_OXIDIZED_COPPER_BLOCK_SLAB.get());
            })
            .build()
    );
}
