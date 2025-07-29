package com.yourname.minecraftcollaboration.server;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import com.yourname.minecraftcollaboration.network.WebSocketHandler;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Main collaboration server that manages WebSocket communication
 * and coordination between different Minecraft instances for collaboration
 */
public class CollaborationServer {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final int webSocketPort;
    private final int collaborationPort;
    private final MinecraftServer minecraftServer;
    
    private WebSocketHandler webSocketServer;
    private CollaborationCoordinator coordinator;
    
    public CollaborationServer(int webSocketPort, int collaborationPort, MinecraftServer server) {
        this.webSocketPort = webSocketPort;
        this.collaborationPort = collaborationPort;
        this.minecraftServer = server;
    }
    
    /**
     * Start all collaboration services
     */
    public void start() throws IOException {
        LOGGER.info("Starting Collaboration Server...");
        
        // Initialize coordination system
        coordinator = new CollaborationCoordinator(minecraftServer);
        
        // Start WebSocket server for Scratch communication
        startWebSocketServer();
        
        // Start collaboration coordinator
        coordinator.start();
        
        LOGGER.info("Collaboration Server started successfully");
    }
    
    /**
     * Stop all collaboration services
     */
    public void stop() throws InterruptedException {
        LOGGER.info("Stopping Collaboration Server...");
        
        // Stop WebSocket server
        if (webSocketServer != null) {
            webSocketServer.stop();
            LOGGER.info("WebSocket server stopped");
        }
        
        // Stop coordination system
        if (coordinator != null) {
            coordinator.stop();
            LOGGER.info("Collaboration coordinator stopped");
        }
        
        LOGGER.info("Collaboration Server stopped successfully");
    }
    
    /**
     * Start the WebSocket server for Scratch extension communication
     */
    private void startWebSocketServer() throws IOException {
        try {
            InetSocketAddress address = new InetSocketAddress("localhost", webSocketPort);
            webSocketServer = new WebSocketHandler(address);
            
            // Start the WebSocket server
            webSocketServer.start();
            
            LOGGER.info("WebSocket server started on port: {}", webSocketPort);
        } catch (NoClassDefFoundError e) {
            LOGGER.error("WebSocket library not available. WebSocket server will not start.", e);
            LOGGER.error("Please ensure Java-WebSocket library is properly included in the build");
        }
    }
    
    /**
     * Get the WebSocket server instance
     */
    public WebSocketHandler getWebSocketServer() {
        return webSocketServer;
    }
    
    /**
     * Get the collaboration coordinator
     */
    public CollaborationCoordinator getCoordinator() {
        return coordinator;
    }
    
    /**
     * Broadcast a message to all connected Scratch clients
     */
    public void broadcastToScratchClients(String message) {
        if (webSocketServer != null) {
            webSocketServer.broadcastToClients(message);
        }
    }
    
    /**
     * Get the number of connected Scratch clients
     */
    public int getConnectedClientCount() {
        return webSocketServer != null ? webSocketServer.getConnectedClientCount() : 0;
    }
    
    /**
     * Check if the collaboration server is running
     */
    public boolean isRunning() {
        return webSocketServer != null && coordinator != null && coordinator.isRunning();
    }
}