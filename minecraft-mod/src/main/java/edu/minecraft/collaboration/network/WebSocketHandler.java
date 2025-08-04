package edu.minecraft.collaboration.network;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.minecraft.collaboration.security.SecurityConfig;
import edu.minecraft.collaboration.security.RateLimiter;
import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.monitoring.MetricsCollector;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.config.ConfigurationManager;
import edu.minecraft.collaboration.constants.ErrorConstants;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * WebSocket server for handling Scratch extension communications
 * Minecraft Forge 1.20.1 compatible version.
 * Now implements AutoCloseable for proper resource management.
 */
public class WebSocketHandler extends WebSocketServer implements AutoCloseable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandler.class);
    private CollaborationMessageProcessor messageProcessor;
    private final RateLimiter rateLimiter;
    private final AuthenticationManager authManager;
    private final MetricsCollector metrics;
    private final ConfigurationManager configManager;
    private final WebSocketTimeoutHandler timeoutHandler = new WebSocketTimeoutHandler();
    private final ConnectionHealthMonitor healthMonitor;
    
    // Helper classes to reduce complexity
    private final WebSocketMessageValidator messageValidator;
    private final WebSocketMetricsHandler metricsHandler;
    
    // Configuration-based settings
    private final boolean developmentMode;
    private final int maxConnections;
    private final int maxCommandLength;
    
    public WebSocketHandler() {
        super();
        DependencyInjector injector = DependencyInjector.getInstance();
        this.rateLimiter = injector.getService(RateLimiter.class);
        this.authManager = injector.getService(AuthenticationManager.class);
        this.metrics = injector.getService(MetricsCollector.class);
        this.configManager = injector.getService(ConfigurationManager.class);
        this.messageProcessor = new CollaborationMessageProcessor();
        this.healthMonitor = new ConnectionHealthMonitor(this, timeoutHandler);
        
        // Load configuration
        this.developmentMode = configManager.getBooleanProperty("development.mode", true);
        this.maxConnections = configManager.getIntProperty("websocket.max.connections", 10);
        this.maxCommandLength = configManager.getIntProperty("security.max.command.length", 1024);
        
        // Initialize helper classes
        this.messageValidator = new WebSocketMessageValidator(authManager, maxCommandLength, developmentMode);
        this.metricsHandler = new WebSocketMetricsHandler(metrics);
    }
    
    public WebSocketHandler(InetSocketAddress address) {
        super(address);
        DependencyInjector injector = DependencyInjector.getInstance();
        this.rateLimiter = injector.getService(RateLimiter.class);
        this.authManager = injector.getService(AuthenticationManager.class);
        this.metrics = injector.getService(MetricsCollector.class);
        this.configManager = injector.getService(ConfigurationManager.class);
        this.messageProcessor = new CollaborationMessageProcessor();
        this.healthMonitor = new ConnectionHealthMonitor(this, timeoutHandler);
        
        // Load configuration
        this.developmentMode = configManager.getBooleanProperty("development.mode", true);
        this.maxConnections = configManager.getIntProperty("websocket.max.connections", 10);
        this.maxCommandLength = configManager.getIntProperty("security.max.command.length", 1024);
        
        // Initialize helper classes
        this.messageValidator = new WebSocketMessageValidator(authManager, maxCommandLength, developmentMode);
        this.metricsHandler = new WebSocketMetricsHandler(metrics);
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (conn == null) {
            LOGGER.error("Null WebSocket connection in onOpen");
            return;
        }
        
        String remoteAddress = conn.getRemoteSocketAddress() != null
            ? conn.getRemoteSocketAddress().toString() : ErrorConstants.UNKNOWN_CONNECTION;
            
        // Security check
        String clientIp = conn.getRemoteSocketAddress() != null
            ? conn.getRemoteSocketAddress().getAddress().getHostAddress() : "";
            
        if (!SecurityConfig.isAddressAllowed(clientIp)) {
            LOGGER.warn("Connection rejected from unauthorized address: {}", remoteAddress);
            conn.close(1003, "Unauthorized address");
            return;
        }
        
        // Check connection limit
        if (getConnections().size() > maxConnections) {
            LOGGER.warn("Connection rejected - max connections reached");
            conn.close(1008, "Server full");
            return;
        }
        
        LOGGER.info("New Scratch client connected: {}", remoteAddress);
        
        // Update metrics
        metricsHandler.recordConnectionOpened();
        metrics.incrementCounter(MetricsCollector.Metrics.WS_CONNECTIONS_TOTAL);
        metrics.setGauge(MetricsCollector.Metrics.WS_CONNECTIONS_ACTIVE, getConnections().size());
        
        // Send welcome message with available commands
        String welcomeMessage = createWelcomeMessage();
        timeoutHandler.sendWithTimeout(conn, welcomeMessage)
            .thenAccept(success -> {
                if (!success) {
                    LOGGER.warn("Failed to send welcome message to {}", remoteAddress);
                }
            });
        
        // Broadcast to all clients about new connection with timeout
        broadcastWithTimeout("{\"type\":\"system\",\"event\":\"newConnection\",\"address\":\"" + remoteAddress + "\"}");
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String identifier = conn.getRemoteSocketAddress() != null
            ? conn.getRemoteSocketAddress().toString() : ErrorConstants.UNKNOWN_CONNECTION;
            
        LOGGER.info("Scratch client disconnected: {} (Code: {}, Reason: {})", 
                    identifier, code, reason);
        
        // Remove authentication
        authManager.removeConnection(identifier);
        
        // Update metrics
        metricsHandler.recordConnectionClosed();
        metrics.setGauge(MetricsCollector.Metrics.WS_CONNECTIONS_ACTIVE, getConnections().size() - 1);
        
        // Clear health check history
        healthMonitor.clearHealthCheckHistory(identifier);
    }
    
    @Override
    public void onMessage(final WebSocket conn, final String message) {
        LOGGER.debug("Received message from {}: {}", conn.getRemoteSocketAddress(), message);
        
        final String identifier = conn.getRemoteSocketAddress().toString();
        
        // Update metrics
        metricsHandler.recordMessageReceived();
        
        // Validate message using helper class
        final WebSocketMessageValidator.ValidationResult lengthValidation = messageValidator.validateMessageLength(message);
        if (!lengthValidation.isValid()) {
            handleValidationError(conn, identifier, lengthValidation);
            return;
        }
        
        final WebSocketMessageValidator.ValidationResult authValidation = messageValidator.validateAuthentication(message, identifier);
        if (!authValidation.isValid()) {
            handleValidationError(conn, identifier, authValidation);
            return;
        }
        
        // Apply rate limiting
        if (!rateLimiter.allowCommand(identifier)) {
            handleRateLimitExceeded(conn, identifier);
            return;
        }
        
        // Process the message
        processValidMessage(conn, identifier, message);
    }
    
    /**
     * Handle validation errors by sending error response and updating metrics
     */
    private void handleValidationError(final WebSocket conn, final String identifier, 
                                     final WebSocketMessageValidator.ValidationResult validation) {
        LOGGER.warn("Validation error from {}: {}", identifier, validation.getErrorMessage());
        timeoutHandler.sendWithTimeout(conn, validation.toJsonResponse());
        metricsHandler.recordError();
    }
    
    /**
     * Handle rate limit exceeded scenario
     */
    private void handleRateLimitExceeded(final WebSocket conn, final String identifier) {
        LOGGER.warn("Rate limit exceeded for {}", identifier);
        final String errorResponse = String.format(ErrorConstants.JSON_ERROR_TEMPLATE, 
                                                  ErrorConstants.ERROR_RATE_LIMIT_EXCEEDED, 
                                                  ErrorConstants.MSG_RATE_LIMIT_EXCEEDED);
        timeoutHandler.sendWithTimeout(conn, errorResponse);
        metricsHandler.recordError();
    }
    
    /**
     * Process a valid message and send response
     */
    private void processValidMessage(final WebSocket conn, final String identifier, final String message) {
        try {
            // Set connection ID for authentication
            messageProcessor.setConnectionId(identifier);
            
            // Process the message and get response
            final String response = messageProcessor.processMessage(message);
            
            if (response != null && !response.isEmpty()) {
                // Send response back to the specific client with timeout
                timeoutHandler.sendWithTimeout(conn, response)
                    .thenAccept(success -> {
                        if (success) {
                            LOGGER.debug("Sent response to {}: {}", conn.getRemoteSocketAddress(), response);
                            metricsHandler.recordSuccessfulCommand();
                        } else {
                            LOGGER.warn("Failed to send response to {}", conn.getRemoteSocketAddress());
                            metricsHandler.recordError();
                        }
                    });
            } else {
                metricsHandler.recordSuccessfulCommand();
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing message from {}: {}", conn.getRemoteSocketAddress(), message, e);
            metricsHandler.recordFailedCommand();
            
            // Send error response to client with timeout
            final String errorResponse = "error.processing(" + e.getMessage() + ")";
            timeoutHandler.sendWithTimeout(conn, errorResponse);
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
        
        // Start health monitoring
        healthMonitor.start();
    }
    
    /**
     * Creates a welcome message with available commands for Scratch clients
     */
    private String createWelcomeMessage() {
        return ErrorConstants.JSON_WELCOME_MESSAGE;
    }
    
    
    /**
     * Broadcast a message to all connected Scratch clients with timeout
     */
    public void broadcastToClients(String message) {
        broadcastWithTimeout(message);
    }
    
    /**
     * Broadcast a message with timeout handling
     */
    private void broadcastWithTimeout(String message) {
        getConnections().parallelStream().forEach(conn -> {
            if (conn != null && conn.isOpen()) {
                timeoutHandler.sendWithTimeout(conn, message)
                    .thenAccept(success -> {
                        if (!success) {
                            LOGGER.warn("Failed to broadcast to client: {}", 
                                conn.getRemoteSocketAddress());
                        }
                    });
            }
        });
        LOGGER.debug("Broadcasted message to all clients: {}", message);
    }
    
    /**
     * Send a message to a specific client
     */
    public void sendToClient(WebSocket client, String message) {
        if (client != null && client.isOpen()) {
            timeoutHandler.sendWithTimeout(client, message)
                .thenAccept(success -> {
                    if (success) {
                        LOGGER.debug("Sent message to specific client: {}", message);
                    } else {
                        LOGGER.warn("Failed to send message to client");
                    }
                });
        }
    }
    
    /**
     * Get the number of connected clients
     */
    public int getConnectedClientCount() {
        return getConnections().size();
    }
    
    // Static getter methods for metrics
    public long getStartTime() {
        return (Long) metricsHandler.getMetrics().get("uptimeMs");
    }
    
    public int getConnectionCount() {
        return (Integer) metricsHandler.getMetrics().get("connectionCount");
    }
    
    public int getTotalMessages() {
        return (Integer) metricsHandler.getMetrics().get("totalMessages");
    }
    
    public int getErrorCount() {
        return (Integer) metricsHandler.getMetrics().get("errorCount");
    }
    
    public int getTotalCommands() {
        return (Integer) metricsHandler.getMetrics().get("totalCommands");
    }
    
    public int getSuccessfulCommands() {
        return (Integer) metricsHandler.getMetrics().get("successfulCommands");
    }
    
    public int getFailedCommands() {
        return (Integer) metricsHandler.getMetrics().get("failedCommands");
    }
    
    /**
     * Get comprehensive metrics from the metrics handler
     */
    public Map<String, Object> getMetrics() {
        return metricsHandler.getMetrics();
    }
    
    /**
     * Shutdown the WebSocket handler and cleanup resources
     */
    public void shutdown() {
        close();
    }
    
    /**
     * Close the WebSocket handler and release resources
     */
    @Override
    public void close() {
        LOGGER.info("Closing WebSocket handler...");
        
        // Stop the WebSocket server
        try {
            stop(5000); // 5 second timeout
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while stopping WebSocket server", e);
            Thread.currentThread().interrupt();
        }
        
        // Stop health monitoring
        if (healthMonitor != null) {
            healthMonitor.close();
        }
        
        // Close timeout handler
        if (timeoutHandler != null) {
            timeoutHandler.close();
        }
        
        // Close all connections
        getConnections().forEach(conn -> {
            if (conn != null && conn.isOpen()) {
                conn.close(1001, "Server shutting down");
            }
        });
        
        LOGGER.info("WebSocket handler closed successfully");
    }
}