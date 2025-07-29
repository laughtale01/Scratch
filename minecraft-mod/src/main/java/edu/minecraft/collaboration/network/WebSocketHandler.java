package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.minecraft.collaboration.security.SecurityConfig;
import edu.minecraft.collaboration.security.RateLimiter;
import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.security.InputValidator;
import edu.minecraft.collaboration.monitoring.MetricsCollector;

import java.net.InetSocketAddress;

/**
 * WebSocket server for handling Scratch extension communications
 * Minecraft Forge 1.20.1 compatible version
 */
public class WebSocketHandler extends WebSocketServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandler.class);
    private CollaborationMessageProcessor messageProcessor;
    private final RateLimiter rateLimiter = RateLimiter.getInstance();
    private final AuthenticationManager authManager = AuthenticationManager.getInstance();
    private final MetricsCollector metrics = MetricsCollector.getInstance();
    
    public WebSocketHandler() {
        super();
        this.messageProcessor = new CollaborationMessageProcessor();
    }
    
    public WebSocketHandler(InetSocketAddress address) {
        super(address);
        this.messageProcessor = new CollaborationMessageProcessor();
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (conn == null) {
            LOGGER.error("Null WebSocket connection in onOpen");
            return;
        }
        
        String remoteAddress = conn.getRemoteSocketAddress() != null ? 
            conn.getRemoteSocketAddress().toString() : "unknown";
            
        // Security check
        String clientIp = conn.getRemoteSocketAddress() != null ?
            conn.getRemoteSocketAddress().getAddress().getHostAddress() : "";
            
        if (!SecurityConfig.isAddressAllowed(clientIp)) {
            LOGGER.warn("Connection rejected from unauthorized address: {}", remoteAddress);
            conn.close(1003, "Unauthorized address");
            return;
        }
        
        // Check connection limit
        if (getConnections().size() > SecurityConfig.MAX_CONNECTIONS) {
            LOGGER.warn("Connection rejected - max connections reached");
            conn.close(1008, "Server full");
            return;
        }
        
        LOGGER.info("New Scratch client connected: {}", remoteAddress);
        
        // Update metrics
        metrics.incrementCounter(MetricsCollector.Metrics.WS_CONNECTIONS_TOTAL);
        metrics.setGauge(MetricsCollector.Metrics.WS_CONNECTIONS_ACTIVE, getConnections().size());
        
        // Send welcome message with available commands
        String welcomeMessage = createWelcomeMessage();
        conn.send(welcomeMessage);
        
        // Broadcast to all clients about new connection
        broadcast("{\"type\":\"system\",\"event\":\"newConnection\",\"address\":\"" + remoteAddress + "\"}");
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String identifier = conn.getRemoteSocketAddress() != null ? 
            conn.getRemoteSocketAddress().toString() : "unknown";
            
        LOGGER.info("Scratch client disconnected: {} (Code: {}, Reason: {})", 
                    identifier, code, reason);
        
        // Remove authentication
        authManager.removeConnection(identifier);
        
        // Update metrics
        metrics.setGauge(MetricsCollector.Metrics.WS_CONNECTIONS_ACTIVE, getConnections().size() - 1);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        LOGGER.debug("Received message from {}: {}", conn.getRemoteSocketAddress(), message);
        
        // Update metrics
        metrics.incrementCounter(MetricsCollector.Metrics.WS_MESSAGES_RECEIVED);
        
        String identifier = conn.getRemoteSocketAddress().toString();
        
        // Validate input length
        if (message == null || message.length() > SecurityConfig.MAX_COMMAND_LENGTH) {
            LOGGER.warn("Invalid message length from {}", identifier);
            conn.send("{\"type\":\"error\",\"error\":\"invalidMessageLength\",\"message\":\"Message too long\"}");
            metrics.incrementCounter(MetricsCollector.Metrics.WS_ERRORS);
            return;
        }
        
        // Check authentication for non-auth commands
        if (!message.contains("\"command\":\"auth\"") && !authManager.isAuthenticated(identifier)) {
            LOGGER.warn("Unauthenticated request from {}", identifier);
            conn.send("{\"type\":\"error\",\"error\":\"unauthenticated\",\"message\":\"Please authenticate first\"}");
            metrics.incrementCounter(MetricsCollector.Metrics.WS_ERRORS);
            return;
        }
        
        // Apply rate limiting
        if (!rateLimiter.allowCommand(identifier)) {
            LOGGER.warn("Rate limit exceeded for {}", identifier);
            conn.send("{\"type\":\"error\",\"error\":\"rateLimitExceeded\",\"message\":\"Too many commands. Please slow down.\"}");
            metrics.incrementCounter(MetricsCollector.Metrics.WS_ERRORS);
            return;
        }
        
        try {
            // Process the message and get response
            String response = messageProcessor.processMessage(message);
            
            if (response != null && !response.isEmpty()) {
                // Send response back to the specific client
                conn.send(response);
                LOGGER.debug("Sent response to {}: {}", conn.getRemoteSocketAddress(), response);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing message from {}: {}", conn.getRemoteSocketAddress(), message, e);
            
            // Send error response to client
            String errorResponse = "error.processing(" + e.getMessage() + ")";
            conn.send(errorResponse);
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("WebSocket error on connection {}: {}", 
                     conn != null ? conn.getRemoteSocketAddress() : "unknown", ex.getMessage(), ex);
    }
    
    @Override
    public void onStart() {
        LOGGER.info("WebSocket server started successfully on: {}", getAddress());
        LOGGER.info("Ready to accept Scratch extension connections");
    }
    
    /**
     * Creates a welcome message with available commands for Scratch clients
     */
    private String createWelcomeMessage() {
        return "{\"type\":\"welcome\",\"status\":\"connected\",\"message\":\"Minecraft Collaboration System Ready\",\"version\":\"1.0.0\"}";
    }
    
    /**
     * Broadcast a message to all connected Scratch clients
     */
    public void broadcastToClients(String message) {
        broadcast(message);
        LOGGER.debug("Broadcasted message to all clients: {}", message);
    }
    
    /**
     * Send a message to a specific client
     */
    public void sendToClient(WebSocket client, String message) {
        if (client != null && client.isOpen()) {
            client.send(message);
            LOGGER.debug("Sent message to specific client: {}", message);
        }
    }
    
    /**
     * Get the number of connected clients
     */
    public int getConnectedClientCount() {
        return getConnections().size();
    }
}