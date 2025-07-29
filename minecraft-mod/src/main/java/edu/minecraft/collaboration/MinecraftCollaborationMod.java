package edu.minecraft.collaboration;

import edu.minecraft.collaboration.network.WebSocketHandler;
import edu.minecraft.collaboration.server.CollaborationServer;
import edu.minecraft.collaboration.entities.ModEntities;
import edu.minecraft.collaboration.entities.CollaborationAgent;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.level.LevelEvent;
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
    
    // Configuration
    public static final int WEBSOCKET_PORT = 14711;
    public static final int COLLABORATION_PORT = 14712;
    
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
        
        // Check if WebSocket library is available
        try {
            Class.forName("org.java_websocket.server.WebSocketServer");
            LOGGER.info("WebSocket library is available");
        } catch (ClassNotFoundException e) {
            LOGGER.error("WebSocket library not found! Make sure Java-WebSocket is in the classpath", e);
        }
        
        LOGGER.info("Minecraft Collaboration Mod setup completed");
    }
    
    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
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
                    LOGGER.info("Starting WebSocket server for single-player mode");
                    collaborationServer = new CollaborationServer(WEBSOCKET_PORT, COLLABORATION_PORT, null);
                    collaborationServer.start();
                    
                    LOGGER.info("WebSocket server started successfully in single-player mode");
                    LOGGER.info("WebSocket server listening on port: {}", WEBSOCKET_PORT);
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
                // Start WebSocket server for Scratch communication
                collaborationServer = new CollaborationServer(WEBSOCKET_PORT, COLLABORATION_PORT, server);
                collaborationServer.start();
                
                LOGGER.info("Collaboration servers started successfully");
                LOGGER.info("WebSocket server listening on port: {}", WEBSOCKET_PORT);
                LOGGER.info("Collaboration server listening on port: {}", COLLABORATION_PORT);
                
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
                collaborationServer.stop();
                LOGGER.info("Collaboration servers stopped successfully");
            } catch (InterruptedException e) {
                LOGGER.error("Error while stopping collaboration servers", e);
                Thread.currentThread().interrupt();
            }
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
        }));
    }
    
    private static final java.util.concurrent.ScheduledExecutorService executor = 
        java.util.concurrent.Executors.newScheduledThreadPool(2);
    
    /**
     * Get the shared executor service
     */
    public static java.util.concurrent.ScheduledExecutorService getExecutor() {
        return executor;
    }
}