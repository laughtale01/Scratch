package com.github.minecraftedu;

import com.github.minecraftedu.init.ModBlocks;
import com.github.minecraftedu.init.ModItems;
import com.github.minecraftedu.init.ModCreativeTabs;
import com.github.minecraftedu.network.SimpleWebSocketServer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("minecraftedu")
public class MinecraftEduMod {

    public static final String MOD_ID = "minecraftedu";
    public static final Logger LOGGER = LogManager.getLogger();

    private SimpleWebSocketServer webSocketServer;

    public MinecraftEduMod() {
        // Get the MOD event bus for registration
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register deferred registries to the MOD event bus
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Register creative mode tab handler
        modEventBus.addListener(this::addCreative);

        // Register this class to the Forge event bus for server events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("MinecraftEdu Mod initializing...");
        LOGGER.info("Registered custom blocks and items");
    }

    public void addCreative(BuildCreativeModeTabContentsEvent event) {
        LOGGER.info("BuildCreativeModeTabContentsEvent called for tab: {}", event.getTabKey().location());
        // Add all custom blocks to the Building Blocks creative tab
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            LOGGER.info("Adding custom blocks to Building Blocks tab");
            // Wood vertical slabs
            event.accept(ModItems.VERTICAL_OAK_SLAB.get());
            event.accept(ModItems.VERTICAL_BIRCH_SLAB.get());
            event.accept(ModItems.VERTICAL_SPRUCE_SLAB.get());
            event.accept(ModItems.VERTICAL_JUNGLE_SLAB.get());
            event.accept(ModItems.VERTICAL_ACACIA_SLAB.get());
            event.accept(ModItems.VERTICAL_DARK_OAK_SLAB.get());
            event.accept(ModItems.VERTICAL_CHERRY_SLAB.get());
            event.accept(ModItems.VERTICAL_MANGROVE_SLAB.get());
            event.accept(ModItems.VERTICAL_CRIMSON_SLAB.get());
            event.accept(ModItems.VERTICAL_WARPED_SLAB.get());

            // Stone vertical slabs
            event.accept(ModItems.VERTICAL_STONE_SLAB.get());
            event.accept(ModItems.VERTICAL_COBBLESTONE_SLAB.get());
            event.accept(ModItems.VERTICAL_STONE_BRICK_SLAB.get());
            event.accept(ModItems.VERTICAL_SMOOTH_STONE_SLAB.get());
            event.accept(ModItems.VERTICAL_ANDESITE_SLAB.get());
            event.accept(ModItems.VERTICAL_GRANITE_SLAB.get());
            event.accept(ModItems.VERTICAL_DIORITE_SLAB.get());
            event.accept(ModItems.VERTICAL_SANDSTONE_SLAB.get());
            event.accept(ModItems.VERTICAL_BRICK_SLAB.get());
            event.accept(ModItems.VERTICAL_QUARTZ_SLAB.get());

            // Mineral vertical slabs
            event.accept(ModItems.VERTICAL_IRON_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_GOLD_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_DIAMOND_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_EMERALD_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_LAPIS_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_REDSTONE_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_COAL_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_NETHERITE_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_AMETHYST_BLOCK_SLAB.get());

            // Copper vertical slabs - cut copper variants
            event.accept(ModItems.VERTICAL_CUT_COPPER_SLAB.get());
            event.accept(ModItems.VERTICAL_EXPOSED_CUT_COPPER_SLAB.get());
            event.accept(ModItems.VERTICAL_WEATHERED_CUT_COPPER_SLAB.get());
            event.accept(ModItems.VERTICAL_OXIDIZED_CUT_COPPER_SLAB.get());

            // NEW: Copper vertical slabs - block variants with oxidation stages
            event.accept(ModItems.VERTICAL_EXPOSED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_WEATHERED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_OXIDIZED_COPPER_BLOCK_SLAB.get());

            // NEW: Waxed copper vertical slabs - block variants
            event.accept(ModItems.VERTICAL_WAXED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_WAXED_EXPOSED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_WAXED_WEATHERED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.VERTICAL_WAXED_OXIDIZED_COPPER_BLOCK_SLAB.get());

            // NEW: Waxed copper vertical slabs - cut copper variants
            event.accept(ModItems.VERTICAL_WAXED_CUT_COPPER_SLAB.get());
            event.accept(ModItems.VERTICAL_WAXED_EXPOSED_CUT_COPPER_SLAB.get());
            event.accept(ModItems.VERTICAL_WAXED_WEATHERED_CUT_COPPER_SLAB.get());
            event.accept(ModItems.VERTICAL_WAXED_OXIDIZED_CUT_COPPER_SLAB.get());

            // NEW: Horizontal copper slabs - normal oxidation stages
            event.accept(ModItems.COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.EXPOSED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.WEATHERED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.OXIDIZED_COPPER_BLOCK_SLAB.get());

            // NEW: Horizontal waxed copper slabs
            event.accept(ModItems.WAXED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.WAXED_EXPOSED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.WAXED_WEATHERED_COPPER_BLOCK_SLAB.get());
            event.accept(ModItems.WAXED_OXIDIZED_COPPER_BLOCK_SLAB.get());

            LOGGER.info("Added all custom blocks to Building Blocks creative tab");
        }
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("MinecraftEdu common setup");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("MinecraftEdu server starting...");

        try {
            // WebSocketサーバー起動
            webSocketServer = new SimpleWebSocketServer(14711, event.getServer());
            webSocketServer.start();

            LOGGER.info("WebSocket server started on port 14711");
            LOGGER.info("Scratch clients can now connect!");
        } catch (Exception e) {
            LOGGER.error("Failed to start WebSocket server", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("MinecraftEdu server stopping...");

        if (webSocketServer != null) {
            try {
                webSocketServer.stop();
                LOGGER.info("WebSocket server stopped");
            } catch (Exception e) {
                LOGGER.error("Error stopping WebSocket server", e);
            }
        }
    }
}
