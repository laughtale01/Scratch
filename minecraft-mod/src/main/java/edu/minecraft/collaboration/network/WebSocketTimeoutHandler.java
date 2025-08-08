package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.core.ResourceManager;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles timeout operations for WebSocket connections to prevent hanging.
 * Now integrates with ResourceManager for proper cleanup.
 */
public class WebSocketTimeoutHandler implements AutoCloseable {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    // Timeout configurations (in milliseconds)
    public static final long CONNECTION_TIMEOUT = 10000; // 10 seconds
    public static final long MESSAGE_SEND_TIMEOUT = 5000; // 5 seconds
    public static final long SHUTDOWN_TIMEOUT = 15000; // 15 seconds
    public static final long HEALTH_CHECK_TIMEOUT = 3000; // 3 seconds
    
    private final ScheduledExecutorService timeoutExecutor;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTimeouts;
    private final ResourceManager resourceManager;
    
    public WebSocketTimeoutHandler() {
        this.resourceManager = ResourceManager.getInstance();
        this.timeoutExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "WebSocket-Timeout-Handler");
            t.setDaemon(true);
            return t;
        });
        this.activeTimeouts = new ConcurrentHashMap<>();
        
        // Register executor with ResourceManager for proper cleanup
        resourceManager.registerExecutor("WebSocketTimeoutHandler", timeoutExecutor);
    }
    
    /**
     * Send a message with timeout
     * @param conn The WebSocket connection
     * @param message The message to send
     * @param timeoutMs Timeout in milliseconds
     * @return CompletableFuture that completes when message is sent or times out
     */
    public CompletableFuture<Boolean> sendWithTimeout(WebSocket conn, String message, long timeoutMs) {
        if (conn == null || !conn.isOpen()) {
            return CompletableFuture.completedFuture(false);
        }
        
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        String connectionId = conn.getRemoteSocketAddress().toString();
        
        // Check if executor is still running
        if (timeoutExecutor.isShutdown() || timeoutExecutor.isTerminated()) {
            // Fallback: send without timeout
            try {
                conn.send(message);
                return CompletableFuture.completedFuture(true);
            } catch (Exception e) {
                LOGGER.error("Failed to send message to {}: {}", connectionId, e.getMessage());
                return CompletableFuture.completedFuture(false);
            }
        }
        
        // Create timeout task
        try {
            ScheduledFuture<?> timeoutTask = timeoutExecutor.schedule(() -> {
                if (!future.isDone()) {
                    LOGGER.warn("Message send timeout for connection: {} after {}ms", connectionId, timeoutMs);
                    future.complete(false);
                    activeTimeouts.remove(connectionId);
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);
            
            activeTimeouts.put(connectionId, timeoutTask);
        } catch (RejectedExecutionException e) {
            // Executor was shut down, send without timeout
            try {
                conn.send(message);
                return CompletableFuture.completedFuture(true);
            } catch (Exception ex) {
                LOGGER.error("Failed to send message to {}: {}", connectionId, ex.getMessage());
                return CompletableFuture.completedFuture(false);
            }
        }
        
        try {
            // Send message
            conn.send(message);
            
            // Cancel timeout and complete future
            ScheduledFuture<?> task = activeTimeouts.remove(connectionId);
            if (task != null) {
                task.cancel(false);
            }
            future.complete(true);
            
        } catch (Exception e) {
            LOGGER.error("Error sending message to {}: {}", connectionId, e.getMessage());
            ScheduledFuture<?> task = activeTimeouts.remove(connectionId);
            if (task != null) {
                task.cancel(false);
            }
            future.complete(false);
        }
        
        return future;
    }
    
    /**
     * Send a message with default timeout
     */
    public CompletableFuture<Boolean> sendWithTimeout(WebSocket conn, String message) {
        return sendWithTimeout(conn, message, MESSAGE_SEND_TIMEOUT);
    }
    
    /**
     * Execute a WebSocket operation with timeout
     * @param operation The operation to execute
     * @param timeoutMs Timeout in milliseconds
     * @param timeoutMessage Message to log on timeout
     * @return CompletableFuture that completes when operation finishes or times out
     */
    public <T> CompletableFuture<T> executeWithTimeout(
            Callable<T> operation, 
            long timeoutMs, 
            String timeoutMessage) {
        
        CompletableFuture<T> future = new CompletableFuture<>();
        
        // Submit operation
        Future<T> operationFuture = timeoutExecutor.submit(() -> {
            try {
                T result = operation.call();
                future.complete(result);
                return result;
            } catch (Exception e) {
                future.completeExceptionally(e);
                throw new RuntimeException(e);
            }
        });
        
        // Schedule timeout
        timeoutExecutor.schedule(() -> {
            if (!future.isDone()) {
                LOGGER.warn("Operation timeout: {}", timeoutMessage);
                operationFuture.cancel(true);
                future.completeExceptionally(new TimeoutException(timeoutMessage));
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
        
        return future;
    }
    
    /**
     * Perform health check with timeout
     * @param conn The WebSocket connection to check
     * @return CompletableFuture that completes with true if healthy, false otherwise
     */
    public CompletableFuture<Boolean> healthCheckWithTimeout(WebSocket conn) {
        if (conn == null || !conn.isOpen()) {
            return CompletableFuture.completedFuture(false);
        }
        
        String connectionId = conn.getRemoteSocketAddress().toString();
        AtomicBoolean responseReceived = new AtomicBoolean(false);
        
        return executeWithTimeout(() -> {
            // Send ping
            conn.sendPing();
            
            // Wait for pong with timeout
            long startTime = System.currentTimeMillis();
            while (!responseReceived.get()
                   && (System.currentTimeMillis() - startTime) < HEALTH_CHECK_TIMEOUT) {
                Thread.sleep(100);
            }
            
            return responseReceived.get();
        }, HEALTH_CHECK_TIMEOUT, "Health check timeout for " + connectionId);
    }
    
    /**
     * Shutdown with timeout - delegates to ResourceManager
     */
    public void shutdown() {
        close();
    }
    
    /**
     * Close the timeout handler and release resources
     */
    @Override
    public void close() {
        LOGGER.info("Closing WebSocket timeout handler...");
        
        // Cancel all active timeouts
        activeTimeouts.values().forEach(future -> future.cancel(false));
        activeTimeouts.clear();
        
        // ResourceManager will handle executor shutdown
        resourceManager.unregisterAndShutdownExecutor("WebSocketTimeoutHandler");
    }
    
    /**
     * Cancel timeout for a specific connection
     */
    public void cancelTimeout(String connectionId) {
        ScheduledFuture<?> timeout = activeTimeouts.remove(connectionId);
        if (timeout != null) {
            timeout.cancel(false);
        }
    }
}