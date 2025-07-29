package com.yourname.minecraftcollaboration.monitoring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates reports from collected metrics
 */
public class MetricsReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsReporter.class);
    private static MetricsReporter instance;
    
    private final MetricsCollector collector;
    private final Gson gson;
    private final File reportsDir;
    
    private MetricsReporter() {
        this.collector = MetricsCollector.getInstance();
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
        
        this.reportsDir = new File("metrics/reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
    }
    
    public static MetricsReporter getInstance() {
        if (instance == null) {
            instance = new MetricsReporter();
        }
        return instance;
    }
    
    /**
     * Generate a comprehensive metrics report
     */
    public MetricsReport generateReport() {
        MetricsReport report = new MetricsReport();
        report.generatedAt = LocalDateTime.now();
        
        // Get metrics snapshot
        MetricsCollector.MetricsSnapshot snapshot = collector.getSnapshot();
        
        // Server status
        report.serverStatus = generateServerStatus();
        
        // Performance metrics
        report.performanceMetrics = generatePerformanceMetrics(snapshot);
        
        // WebSocket metrics
        report.webSocketMetrics = generateWebSocketMetrics(snapshot);
        
        // Collaboration metrics
        report.collaborationMetrics = generateCollaborationMetrics(snapshot);
        
        // System metrics
        report.systemMetrics = generateSystemMetrics(snapshot);
        
        // Player activity
        report.playerActivity = generatePlayerActivity();
        
        return report;
    }
    
    /**
     * Export report to file
     */
    public void exportReport(MetricsReport report) {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        );
        
        File reportFile = new File(reportsDir, "report_" + timestamp + ".json");
        try (FileWriter writer = new FileWriter(reportFile)) {
            gson.toJson(report, writer);
            LOGGER.info("Exported metrics report to: {}", reportFile.getName());
        } catch (IOException e) {
            LOGGER.error("Failed to export metrics report", e);
        }
    }
    
    /**
     * Generate HTML report
     */
    public void generateHtmlReport() {
        MetricsReport report = generateReport();
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        );
        
        File htmlFile = new File(reportsDir, "report_" + timestamp + ".html");
        try (FileWriter writer = new FileWriter(htmlFile)) {
            writer.write(generateHtml(report));
            LOGGER.info("Generated HTML report: {}", htmlFile.getName());
        } catch (IOException e) {
            LOGGER.error("Failed to generate HTML report", e);
        }
    }
    
    private ServerStatus generateServerStatus() {
        ServerStatus status = new ServerStatus();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server != null) {
            status.isRunning = true;
            status.playerCount = server.getPlayerCount();
            status.maxPlayers = server.getMaxPlayers();
            status.tickTime = server.getAverageTickTime();
            status.motd = server.getMotd();
        }
        
        return status;
    }
    
    private PerformanceMetrics generatePerformanceMetrics(MetricsCollector.MetricsSnapshot snapshot) {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        // Block operations
        metrics.blocksPlaced = snapshot.counters.getOrDefault(MetricsCollector.Metrics.BLOCKS_PLACED, 0L);
        metrics.blocksBroken = snapshot.counters.getOrDefault(MetricsCollector.Metrics.BLOCKS_BROKEN, 0L);
        metrics.batchOperations = snapshot.counters.getOrDefault(MetricsCollector.Metrics.BATCH_OPERATIONS, 0L);
        
        // Cache performance
        long cacheHits = snapshot.counters.getOrDefault(MetricsCollector.Metrics.CACHE_HITS, 0L);
        long cacheMisses = snapshot.counters.getOrDefault(MetricsCollector.Metrics.CACHE_MISSES, 0L);
        long totalCacheAccess = cacheHits + cacheMisses;
        metrics.cacheHitRate = (totalCacheAccess > 0) ? (cacheHits * 100.0 / totalCacheAccess) : 0;
        
        // Timing metrics
        snapshot.timings.forEach((name, stats) -> {
            if (name.startsWith(MetricsCollector.Metrics.COMMAND_TIMING_PREFIX)) {
                String commandName = name.substring(MetricsCollector.Metrics.COMMAND_TIMING_PREFIX.length());
                metrics.commandTimings.put(commandName, stats.average);
            }
        });
        
        return metrics;
    }
    
    private WebSocketMetrics generateWebSocketMetrics(MetricsCollector.MetricsSnapshot snapshot) {
        WebSocketMetrics metrics = new WebSocketMetrics();
        
        metrics.totalConnections = snapshot.counters.getOrDefault(MetricsCollector.Metrics.WS_CONNECTIONS_TOTAL, 0L);
        metrics.activeConnections = snapshot.gauges.getOrDefault(MetricsCollector.Metrics.WS_CONNECTIONS_ACTIVE, 0L);
        metrics.messagesReceived = snapshot.counters.getOrDefault(MetricsCollector.Metrics.WS_MESSAGES_RECEIVED, 0L);
        metrics.messagesSent = snapshot.counters.getOrDefault(MetricsCollector.Metrics.WS_MESSAGES_SENT, 0L);
        metrics.errors = snapshot.counters.getOrDefault(MetricsCollector.Metrics.WS_ERRORS, 0L);
        
        return metrics;
    }
    
    private CollaborationMetrics generateCollaborationMetrics(MetricsCollector.MetricsSnapshot snapshot) {
        CollaborationMetrics metrics = new CollaborationMetrics();
        
        metrics.invitationsSent = snapshot.counters.getOrDefault(MetricsCollector.Metrics.INVITATIONS_SENT, 0L);
        metrics.invitationsAccepted = snapshot.counters.getOrDefault(MetricsCollector.Metrics.INVITATIONS_ACCEPTED, 0L);
        metrics.invitationsDeclined = snapshot.counters.getOrDefault(MetricsCollector.Metrics.INVITATIONS_DECLINED, 0L);
        metrics.visitsRequested = snapshot.counters.getOrDefault(MetricsCollector.Metrics.VISITS_REQUESTED, 0L);
        metrics.visitsApproved = snapshot.counters.getOrDefault(MetricsCollector.Metrics.VISITS_APPROVED, 0L);
        
        // Calculate acceptance rate
        long totalInvitations = metrics.invitationsAccepted + metrics.invitationsDeclined;
        metrics.acceptanceRate = (totalInvitations > 0) ? 
            (metrics.invitationsAccepted * 100.0 / totalInvitations) : 0;
        
        return metrics;
    }
    
    private SystemMetricsData generateSystemMetrics(MetricsCollector.MetricsSnapshot snapshot) {
        SystemMetricsData metrics = new SystemMetricsData();
        
        if (snapshot.systemMetrics != null) {
            SystemMetrics.SystemSnapshot sys = snapshot.systemMetrics;
            metrics.cpuUsage = sys.cpuUsage;
            metrics.memoryUsage = sys.getMemoryUsagePercent();
            metrics.usedMemoryMB = sys.getUsedMemoryMB();
            metrics.availableMemoryMB = sys.getAvailableMemoryMB();
            metrics.threadCount = sys.threadCount;
        }
        
        return metrics;
    }
    
    private List<PlayerActivityData> generatePlayerActivity() {
        List<PlayerActivityData> activities = new ArrayList<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerActivityData activity = new PlayerActivityData();
                activity.playerName = player.getName().getString();
                activity.gameMode = player.gameMode.getGameModeForPlayer().getName();
                activity.dimension = player.level().dimension().location().toString();
                activity.health = player.getHealth();
                activity.foodLevel = player.getFoodData().getFoodLevel();
                activities.add(activity);
            }
        }
        
        return activities;
    }
    
    private String generateHtml(MetricsReport report) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>Minecraft Collaboration Metrics Report</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("h1 { color: #333; }\n");
        html.append("h2 { color: #666; margin-top: 30px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 10px; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append(".metric-value { font-weight: bold; color: #2196F3; }\n");
        html.append(".warning { color: #ff9800; }\n");
        html.append(".error { color: #f44336; }\n");
        html.append("</style>\n</head>\n<body>\n");
        
        // Header
        html.append("<h1>Minecraft Collaboration Metrics Report</h1>\n");
        html.append("<p>Generated at: ").append(report.generatedAt).append("</p>\n");
        
        // Server Status
        html.append("<h2>Server Status</h2>\n");
        html.append("<table>\n");
        html.append("<tr><td>Status</td><td class='metric-value'>")
            .append(report.serverStatus.isRunning ? "Running" : "Stopped").append("</td></tr>\n");
        html.append("<tr><td>Players</td><td class='metric-value'>")
            .append(report.serverStatus.playerCount).append(" / ").append(report.serverStatus.maxPlayers).append("</td></tr>\n");
        html.append("<tr><td>Average Tick Time</td><td class='metric-value'>")
            .append(String.format("%.2f ms", report.serverStatus.tickTime)).append("</td></tr>\n");
        html.append("</table>\n");
        
        // System Metrics
        html.append("<h2>System Metrics</h2>\n");
        html.append("<table>\n");
        html.append("<tr><td>CPU Usage</td><td class='metric-value'>")
            .append(String.format("%.1f%%", report.systemMetrics.cpuUsage)).append("</td></tr>\n");
        html.append("<tr><td>Memory Usage</td><td class='metric-value'>")
            .append(String.format("%.1f%%", report.systemMetrics.memoryUsage)).append("</td></tr>\n");
        html.append("<tr><td>Used Memory</td><td class='metric-value'>")
            .append(report.systemMetrics.usedMemoryMB).append(" MB</td></tr>\n");
        html.append("<tr><td>Thread Count</td><td class='metric-value'>")
            .append(report.systemMetrics.threadCount).append("</td></tr>\n");
        html.append("</table>\n");
        
        // Performance Metrics
        html.append("<h2>Performance Metrics</h2>\n");
        html.append("<table>\n");
        html.append("<tr><td>Blocks Placed</td><td class='metric-value'>")
            .append(report.performanceMetrics.blocksPlaced).append("</td></tr>\n");
        html.append("<tr><td>Blocks Broken</td><td class='metric-value'>")
            .append(report.performanceMetrics.blocksBroken).append("</td></tr>\n");
        html.append("<tr><td>Cache Hit Rate</td><td class='metric-value'>")
            .append(String.format("%.1f%%", report.performanceMetrics.cacheHitRate)).append("</td></tr>\n");
        html.append("</table>\n");
        
        // WebSocket Metrics
        html.append("<h2>WebSocket Metrics</h2>\n");
        html.append("<table>\n");
        html.append("<tr><td>Active Connections</td><td class='metric-value'>")
            .append(report.webSocketMetrics.activeConnections).append("</td></tr>\n");
        html.append("<tr><td>Messages Received</td><td class='metric-value'>")
            .append(report.webSocketMetrics.messagesReceived).append("</td></tr>\n");
        html.append("<tr><td>Messages Sent</td><td class='metric-value'>")
            .append(report.webSocketMetrics.messagesSent).append("</td></tr>\n");
        html.append("<tr><td>Errors</td><td class='")
            .append(report.webSocketMetrics.errors > 0 ? "error" : "metric-value").append("'>")
            .append(report.webSocketMetrics.errors).append("</td></tr>\n");
        html.append("</table>\n");
        
        html.append("</body>\n</html>");
        return html.toString();
    }
    
    // Report data classes
    public static class MetricsReport {
        public LocalDateTime generatedAt;
        public ServerStatus serverStatus;
        public PerformanceMetrics performanceMetrics;
        public WebSocketMetrics webSocketMetrics;
        public CollaborationMetrics collaborationMetrics;
        public SystemMetricsData systemMetrics;
        public List<PlayerActivityData> playerActivity;
    }
    
    public static class ServerStatus {
        public boolean isRunning;
        public int playerCount;
        public int maxPlayers;
        public double tickTime;
        public String motd;
    }
    
    public static class PerformanceMetrics {
        public long blocksPlaced;
        public long blocksBroken;
        public long batchOperations;
        public double cacheHitRate;
        public Map<String, Double> commandTimings = new ConcurrentHashMap<>();
    }
    
    public static class WebSocketMetrics {
        public long totalConnections;
        public long activeConnections;
        public long messagesReceived;
        public long messagesSent;
        public long errors;
    }
    
    public static class CollaborationMetrics {
        public long invitationsSent;
        public long invitationsAccepted;
        public long invitationsDeclined;
        public long visitsRequested;
        public long visitsApproved;
        public double acceptanceRate;
    }
    
    public static class SystemMetricsData {
        public double cpuUsage;
        public double memoryUsage;
        public long usedMemoryMB;
        public long availableMemoryMB;
        public int threadCount;
    }
    
    public static class PlayerActivityData {
        public String playerName;
        public String gameMode;
        public String dimension;
        public float health;
        public int foodLevel;
    }
}