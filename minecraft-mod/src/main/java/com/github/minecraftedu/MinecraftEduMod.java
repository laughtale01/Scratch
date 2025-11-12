package com.github.minecraftedu;

import com.github.minecraftedu.network.WebSocketServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("minecraftedu")
public class MinecraftEduMod {

    public static final String MOD_ID = "minecraftedu";
    public static final Logger LOGGER = LogManager.getLogger();

    private WebSocketServer webSocketServer;

    public MinecraftEduMod() {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("MinecraftEdu Mod initializing...");
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
            webSocketServer = new WebSocketServer(14711, event.getServer());
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
