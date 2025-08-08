package edu.minecraft.collaboration.server;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.network.WebSocketHandler;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main collaboration server that manages WebSocket communication
 * and coordination between different Minecraft instances for collaboration.
 * Now implements AutoCloseable for proper resource management.
 */
public class CollaborationServer implements AutoCloseable {
    
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
        coordinator = CollaborationCoordinator.getInstance();
        
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
        close();
    }
    
    /**
     * Close the collaboration server and release resources
     */
    @Override
    public void close() {
        LOGGER.info("Closing Collaboration Server...");
        
        // Close WebSocket server with timeout
        if (webSocketServer != null) {
            try {
                CompletableFuture<Void> closeFuture = CompletableFuture.runAsync(() -> {
                    webSocketServer.close();
                });
                
                closeFuture.get(10, TimeUnit.SECONDS);
                LOGGER.info("WebSocket server closed successfully");
            } catch (TimeoutException e) {
                LOGGER.error("Timeout while closing WebSocket server, forcing stop");
                try {
                    webSocketServer.stop();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted while force stopping WebSocket server", ie);
                }
            } catch (Exception e) {
                LOGGER.error("Error closing WebSocket server", e);
            }
        }
        
        // Stop coordination system
        if (coordinator != null) {
            coordinator.stop();
            LOGGER.info("Collaboration coordinator stopped");
        }
        
        LOGGER.info("Collaboration Server closed successfully");
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
            
            System.out.println("=== WEBSOCKET SERVER STARTED ===");
            System.out.println("Port: " + webSocketPort);
            System.out.println("Address: " + address);
            System.out.println("=================================");
            
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