package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.monitoring.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles WebSocket-related metrics collection and reporting
 */
public class WebSocketMetricsHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketMetricsHandler.class);
    
    private final MetricsCollector metrics;
    
    // Static metrics for compatibility with existing code
    private static long startTime = System.currentTimeMillis();
    private static int totalMessages = 0;
    private static int errorCount = 0;
    private static int totalCommands = 0;
    private static int successfulCommands = 0;
    private static int failedCommands = 0;
    private static int connectionCount = 0;
    
    public WebSocketMetricsHandler(final MetricsCollector metrics) {
        this.metrics = metrics;
    }
    
    /**
     * Record message received
     */
    public void recordMessageReceived() {
        metrics.incrementCounter(MetricsCollector.Metrics.WS_MESSAGES_RECEIVED);
        totalMessages++;
    }
    
    /**
     * Record error occurred
     */
    public void recordError() {
        metrics.incrementCounter(MetricsCollector.Metrics.WS_ERRORS);
        errorCount++;
    }
    
    /**
     * Record successful command
     */
    public void recordSuccessfulCommand() {
        totalCommands++;
        successfulCommands++;
    }
    
    /**
     * Record failed command
     */
    public void recordFailedCommand() {
        totalCommands++;
        failedCommands++;
    }
    
    /**
     * Record connection opened
     */
    public void recordConnectionOpened() {
        connectionCount++;
        metrics.incrementCounter(MetricsCollector.Metrics.WS_CONNECTIONS);
    }
    
    /**
     * Record connection closed
     */
    public void recordConnectionClosed() {
        connectionCount--;
        if (connectionCount < 0) {
            connectionCount = 0;
        }
    }
    
    /**
     * Get comprehensive metrics
     */
    public Map<String, Object> getMetrics() {
        final Map<String, Object> metricsMap = new HashMap<>();
        
        // Basic metrics
        metricsMap.put("totalMessages", totalMessages);
        metricsMap.put("errorCount", errorCount);
        metricsMap.put("totalCommands", totalCommands);
        metricsMap.put("successfulCommands", successfulCommands);
        metricsMap.put("failedCommands", failedCommands);
        metricsMap.put("connectionCount", connectionCount);
        
        // Calculated metrics
        final long uptime = System.currentTimeMillis() - startTime;
        metricsMap.put("uptimeMs", uptime);
        metricsMap.put("uptimeSeconds", uptime / 1000);
        
        if (totalCommands > 0) {
            metricsMap.put("successRate", (double) successfulCommands / totalCommands * 100);
            metricsMap.put("errorRate", (double) failedCommands / totalCommands * 100);
        } else {
            metricsMap.put("successRate", 0.0);
            metricsMap.put("errorRate", 0.0);
        }
        
        if (uptime > 0) {
            metricsMap.put("messagesPerSecond", (double) totalMessages / (uptime / 1000.0));
            metricsMap.put("commandsPerSecond", (double) totalCommands / (uptime / 1000.0));
        } else {
            metricsMap.put("messagesPerSecond", 0.0);
            metricsMap.put("commandsPerSecond", 0.0);
        }
        
        return metricsMap;
    }
    
    /**
     * Reset all metrics (for testing)
     */
    public void resetMetrics() {
        startTime = System.currentTimeMillis();
        totalMessages = 0;
        errorCount = 0;
        totalCommands = 0;
        successfulCommands = 0;
        failedCommands = 0;
        connectionCount = 0;
    }
    
    /**
     * Log current metrics state
     */
    public void logMetrics() {
        final Map<String, Object> currentMetrics = getMetrics();
        LOGGER.info("WebSocket Metrics: {}", currentMetrics);
    }
}