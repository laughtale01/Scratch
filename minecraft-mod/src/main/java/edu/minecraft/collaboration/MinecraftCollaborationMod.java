package edu.minecraft.collaboration;

import edu.minecraft.collaboration.network.WebSocketHandler;
import edu.minecraft.collaboration.server.CollaborationServer;
import edu.minecraft.collaboration.entities.ModEntities;
import edu.minecraft.collaboration.entities.CollaborationAgent;
import edu.minecraft.collaboration.util.ResourceCleanupManager;
import edu.minecraft.collaboration.core.ResourceManager;
import edu.minecraft.collaboration.config.ConfigurationManager;
import edu.minecraft.collaboration.core.DependencyInjector;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod(MinecraftCollaborationMod.MODID)
public class MinecraftCollaborationMod {

    public static final String MODID = "minecraftcollaboration";
    public static final String MOD_ID = MODID; // Alias for consistency
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftCollaborationMod.class);

    // Collaboration system components
    private static CollaborationServer collaborationServer;
    private static WebSocketHandler webSocketHandler;

    // Configuration manager
    private static ConfigurationManager configManager;

    // Resource management - both old and new systems for transition
    private static final ResourceCleanupManager RESOURCE_CLEANUP_MANAGER = ResourceCleanupManager.getInstance();
    private static final ResourceManager RESOURCE_MANAGER = ResourceManager.getInstance();

    public MinecraftCollaborationMod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerEntityAttributes);

        // Register entities
        ModEntities.register(FMLJavaModLoadingContext.get().getModEventBus());

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Minecraft Collaboration Mod initialized");
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Minecraft Collaboration Mod setup started");

        // Initialize configuration manager
        configManager = DependencyInjector.getInstance().getService(ConfigurationManager.class);
        LOGGER.info("Configuration loaded with profile: {}", configManager.getActiveProfile());

        // Check if WebSocket library is available
        try {
            Class.forName("org.java_websocket.server.WebSocketServer");
            LOGGER.info("WebSocket library is available");
        } catch (ClassNotFoundException e) {
            LOGGER.error("WebSocket library not found! Make sure Java-WebSocket is in the classpath", e);
        }

        LOGGER.info("Minecraft Collaboration Mod setup completed");
    }

    private void registerEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.COLLABORATION_AGENT.get(), CollaborationAgent.createAttributes().build());
        LOGGER.info("Registered entity attributes for CollaborationAgent");
    }

    @OnlyIn(Dist.CLIENT)
    private void doClientStuff(final FMLClientSetupEvent event) {
        LOGGER.info("Client setup for Minecraft Collaboration Mod");

        // Start WebSocket server in single-player mode
        event.enqueueWork(() -> {
            if (collaborationServer == null) {
                try {
                    // Get configuration
                    final ConfigurationManager config = getConfigurationManager();
                    final int websocketPort = config.getIntProperty("websocket.port", 14711);
                    int collaborationPort = config.getIntProperty("websocket.collaboration.port", 14712);

                    LOGGER.info("Starting WebSocket server for single-player mode");
                    collaborationServer = new CollaborationServer(websocketPort, collaborationPort, null);
                    collaborationServer.start();

                    LOGGER.info("WebSocket server started successfully in single-player mode");
                    LOGGER.info("WebSocket server listening on port: {}", websocketPort);
                } catch (IOException e) {
                    LOGGER.error("Failed to start WebSocket server in single-player mode", e);
                }
            }
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Starting Minecraft Collaboration Server");

        MinecraftServer server = event.getServer();

        if (server != null) {
            try {
                // Get configuration
                ConfigurationManager config = getConfigurationManager();
                int websocketPort = config.getIntProperty("websocket.port", 14711);
                int collaborationPort = config.getIntProperty("websocket.collaboration.port", 14712);

                // Start WebSocket server for Scratch communication
                collaborationServer = new CollaborationServer(websocketPort, collaborationPort, server);
                collaborationServer.start();

                // Register collaboration server with ResourceManager for proper cleanup
                RESOURCE_MANAGER.registerResource("CollaborationServer", collaborationServer);

                LOGGER.info("Collaboration servers started successfully");
                LOGGER.info("WebSocket server listening on port: {}", websocketPort);
                LOGGER.info("Collaboration server listening on port: {}", collaborationPort);

            } catch (IOException e) {
                LOGGER.error("Failed to start collaboration servers", e);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Stopping Minecraft Collaboration Server");

        if (collaborationServer != null) {
            try {
                collaborationServer.close(); // Use close() instead of stop()
                LOGGER.info("Collaboration server closed successfully");
            } catch (Exception e) {
                LOGGER.error("Error closing collaboration server", e);
            }
        }

        // Perform resource cleanup with new ResourceManager
        LOGGER.info("Performing resource cleanup...");

        try {
            ResourceManager.ResourceStatistics stats = RESOURCE_MANAGER.getStatistics();
            LOGGER.info("Resource statistics before cleanup: {}", stats);
            RESOURCE_MANAGER.shutdown();
            LOGGER.info("ResourceManager shutdown completed");
        } catch (Exception e) {
            LOGGER.error("Error during ResourceManager shutdown", e);
        }

        // Fallback cleanup with old system
        try {
            ResourceCleanupManager.ResourceStatistics oldStats = RESOURCE_CLEANUP_MANAGER.getStatistics();
            LOGGER.info("Legacy resource statistics before cleanup: {}", oldStats);
            RESOURCE_CLEANUP_MANAGER.cleanup();
            LOGGER.info("Legacy ResourceCleanupManager cleanup completed");
        } catch (Exception e) {
            LOGGER.error("Error during legacy cleanup", e);
        }
    }

    // Utility methods for other classes
    public static CollaborationServer getCollaborationServer() {
        return collaborationServer;
    }

    public static WebSocketHandler getWebSocketHandler() {
        return webSocketHandler;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static boolean isServerRunning() {
        return collaborationServer != null && collaborationServer.isRunning();
    }

    public static ConfigurationManager getConfigurationManager() {
        if (configManager == null) {
            configManager = DependencyInjector.getInstance().getService(ConfigurationManager.class);
        }
        return configManager;
    }

    // Add shutdown hook for client-side cleanup
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (collaborationServer != null) {
                try {
                    LOGGER.info("Shutting down collaboration server...");
                    collaborationServer.stop();
                } catch (Exception e) {
                    LOGGER.error("Error stopping collaboration server", e);
                }
            }

            // Perform resource cleanup
            LOGGER.info("Performing resource cleanup...");
            RESOURCE_CLEANUP_MANAGER.cleanup();
        }));
    }

    private static final java.util.concurrent.ScheduledExecutorService EXECUTOR;

    static {
        // Initialize executor using ResourceCleanupManager
        EXECUTOR = RESOURCE_CLEANUP_MANAGER.createManagedScheduledExecutor("MinecraftCollab-Main", 2);
    }

    /**
     * Get the shared executor service
     */
    public static java.util.concurrent.ScheduledExecutorService getExecutor() {
        return EXECUTOR;
    }

    /**
     * Callback for WebSocket connection establishment (for test compatibility)
     */
    public void onWebSocketConnected() {
        LOGGER.info("WebSocket connection established");
    }

}
