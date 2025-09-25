package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.core.ResourceManager;
import edu.minecraft.collaboration.monitoring.MetricsCollector;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitors WebSocket connection health and performs periodic health checks.
 * Now integrates with ResourceManager for proper cleanup.
 */
public class ConnectionHealthMonitor implements AutoCloseable {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    // Health check configuration
    private static final long HEALTH_CHECK_INTERVAL = 30000; // 30 seconds
    private static final int MAX_FAILED_CHECKS = 3;

    private final WebSocketHandler webSocketHandler;
    private final WebSocketTimeoutHandler timeoutHandler;
    private final MetricsCollector metrics;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, AtomicInteger> failedChecks;
    private final ResourceManager resourceManager;

    private volatile boolean running = false;

    public ConnectionHealthMonitor(WebSocketHandler handler, WebSocketTimeoutHandler timeoutHandler) {
        this.webSocketHandler = handler;
        this.timeoutHandler = timeoutHandler;
        this.metrics = DependencyInjector.getInstance().getService(MetricsCollector.class);
        this.resourceManager = ResourceManager.getInstance();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WebSocket-Health-Monitor");
            t.setDaemon(true);
            return t;
        });
        this.failedChecks = new ConcurrentHashMap<>();

        // Register executor with ResourceManager for proper cleanup
        resourceManager.registerExecutor("ConnectionHealthMonitor", scheduler);
    }

    /**
     * Start monitoring connection health
     */
    public void start() {
        if (running) {
            return;
        }

        running = true;
        LOGGER.info("Starting connection health monitoring");

        scheduler.scheduleWithFixedDelay(
            this::performHealthChecks,
            HEALTH_CHECK_INTERVAL,
            HEALTH_CHECK_INTERVAL,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stop monitoring - delegates to close()
     */
    public void stop() {
        close();
    }

    /**
     * Close the health monitor and release resources
     */
    @Override
    public void close() {
        running = false;
        LOGGER.info("Closing connection health monitoring");

        // Clear tracking data
        failedChecks.clear();

        // ResourceManager will handle executor shutdown
        resourceManager.unregisterAndShutdownExecutor("ConnectionHealthMonitor");
    }

    /**
     * Perform health checks on all connections
     */
    private void performHealthChecks() {
        if (!running || webSocketHandler == null) {
            return;
        }

        LOGGER.debug("Performing connection health checks");

        webSocketHandler.getConnections().forEach(conn -> {
            if (conn != null && conn.isOpen()) {
                checkConnectionHealth(conn);
            }
        });
    }

    /**
     * Check health of a specific connection
     */
    private void checkConnectionHealth(WebSocket conn) {
        String connectionId = conn.getRemoteSocketAddress().toString();

        timeoutHandler.healthCheckWithTimeout(conn)
            .thenAccept(healthy -> {
                if (healthy) {
                    // Connection is healthy, reset failed checks
                    failedChecks.remove(connectionId);
                    LOGGER.debug("Connection {} is healthy", connectionId);
                } else {
                    // Health check failed
                    handleFailedHealthCheck(conn, connectionId);
                }
            })
            .exceptionally(ex -> {
                LOGGER.error("Exception during health check for {}: {}", connectionId, ex.getMessage());
                handleFailedHealthCheck(conn, connectionId);
                return null;
            });
    }

    /**
     * Handle a failed health check
     */
    private void handleFailedHealthCheck(WebSocket conn, String connectionId) {
        AtomicInteger failures = failedChecks.computeIfAbsent(connectionId, k -> new AtomicInteger(0));
        int failCount = failures.incrementAndGet();

        LOGGER.warn("Health check failed for {} (failure count: {})", connectionId, failCount);
        metrics.incrementCounter("websocket.health.check.failed");

        if (failCount >= MAX_FAILED_CHECKS) {
            LOGGER.error("Connection {} failed {} health checks, closing connection", connectionId, failCount);
            failedChecks.remove(connectionId);

            // Close the unhealthy connection
            if (conn.isOpen()) {
                conn.close(1001, "Connection health check failed");
            }

            metrics.incrementCounter("websocket.health.connection.closed");
        }
    }

    /**
     * Manually check a specific connection
     */
    public void checkConnection(WebSocket conn) {
        if (conn != null && conn.isOpen()) {
            checkConnectionHealth(conn);
        }
    }

    /**
     * Get the number of connections with failed health checks
     */
    public int getUnhealthyConnectionCount() {
        return failedChecks.size();
    }

    /**
     * Clear health check history for a connection
     */
    public void clearHealthCheckHistory(String connectionId) {
        failedChecks.remove(connectionId);
    }
}
