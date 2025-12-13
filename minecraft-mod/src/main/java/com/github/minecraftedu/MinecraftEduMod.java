package com.github.minecraftedu;

import com.github.minecraftedu.init.ModBlocks;
import com.github.minecraftedu.init.ModItems;
import com.github.minecraftedu.network.SimpleWebSocketServer;
import net.minecraftforge.common.MinecraftForge;
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

        // Register this class to the Forge event bus for server events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("MinecraftEdu Mod initializing...");
        LOGGER.info("Registered custom blocks: vertical_oak_slab");
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
