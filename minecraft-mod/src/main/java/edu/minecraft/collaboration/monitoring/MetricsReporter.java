package edu.minecraft.collaboration.monitoring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.util.FileSecurityUtils;
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
public final class MetricsReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsReporter.class);
    private static MetricsReporter instance;

    private final MetricsCollector collector;
    private final Gson gson;
    private final File reportsDir;

    private MetricsReporter() {
        this.collector = DependencyInjector.getInstance().getService(MetricsCollector.class);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

        // Use secure directory creation
        if (FileSecurityUtils.ensureSafeDirectory("metrics/reports")) {
            this.reportsDir = new File("metrics/reports");
        } else {
            LOGGER.error("Failed to create secure reports directory");
            this.reportsDir = null;
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
        report.setGeneratedAt(LocalDateTime.now());

        // Get metrics snapshot
        MetricsCollector.MetricsSnapshot snapshot = collector.getSnapshot();

        // Server status
        report.setServerStatus(generateServerStatus());

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
        if (reportsDir == null) {
            LOGGER.error("Reports directory not initialized");
            return;
        }

        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        );

        // Use secure file creation
        String fileName = "report_" + timestamp + ".json";
        File reportFile = FileSecurityUtils.getSafeFile("metrics/reports", fileName);

        if (reportFile == null) {
            LOGGER.error("Failed to create safe report file");
            return;
        }

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

        if (reportsDir == null) {
            LOGGER.error("Reports directory not initialized");
            return;
        }

        // Use secure file creation
        String fileName = "report_" + timestamp + ".html";
        File htmlFile = FileSecurityUtils.getSafeFile("metrics/reports", fileName);

        if (htmlFile == null) {
            LOGGER.error("Failed to create safe HTML report file");
            return;
        }

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
            status.setRunning(true);
            status.setPlayerCount(server.getPlayerCount());
            status.setMaxPlayers(server.getMaxPlayers());
            status.setTickTime(server.getAverageTickTime());
            status.motd = server.getMotd();
        }

        return status;
    }

    private PerformanceMetrics generatePerformanceMetrics(MetricsCollector.MetricsSnapshot snapshot) {
        PerformanceMetrics metrics = new PerformanceMetrics();

        // Block operations
        metrics.blocksPlaced = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.BLOCKS_PLACED, 0L);
        metrics.blocksBroken = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.BLOCKS_BROKEN, 0L);
        metrics.batchOperations = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.BATCH_OPERATIONS, 0L);

        // Cache performance
        long cacheHits = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.CACHE_HITS, 0L);
        long cacheMisses = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.CACHE_MISSES, 0L);
        long totalCacheAccess = cacheHits + cacheMisses;
        metrics.cacheHitRate = (totalCacheAccess > 0) ? (cacheHits * 100.0 / totalCacheAccess) : 0;

        // Timing metrics
        snapshot.getTimings().forEach((name, stats) -> {
            if (name.startsWith(MetricsCollector.Metrics.COMMAND_TIMING_PREFIX)) {
                String commandName = name.substring(MetricsCollector.Metrics.COMMAND_TIMING_PREFIX.length());
                metrics.commandTimings.put(commandName, stats.getAverage());
            }
        });

        return metrics;
    }

    private WebSocketMetrics generateWebSocketMetrics(MetricsCollector.MetricsSnapshot snapshot) {
        WebSocketMetrics metrics = new WebSocketMetrics();

        metrics.totalConnections = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.WS_CONNECTIONS_TOTAL, 0L);
        metrics.activeConnections = snapshot.getGauges().getOrDefault(MetricsCollector.Metrics.WS_CONNECTIONS_ACTIVE, 0L);
        metrics.messagesReceived = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.WS_MESSAGES_RECEIVED, 0L);
        metrics.messagesSent = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.WS_MESSAGES_SENT, 0L);
        metrics.errors = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.WS_ERRORS, 0L);

        return metrics;
    }

    private CollaborationMetrics generateCollaborationMetrics(MetricsCollector.MetricsSnapshot snapshot) {
        CollaborationMetrics metrics = new CollaborationMetrics();

        metrics.invitationsSent = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.INVITATIONS_SENT, 0L);
        metrics.invitationsAccepted = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.INVITATIONS_ACCEPTED, 0L);
        metrics.invitationsDeclined = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.INVITATIONS_DECLINED, 0L);
        metrics.visitsRequested = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.VISITS_REQUESTED, 0L);
        metrics.visitsApproved = snapshot.getCounters().getOrDefault(MetricsCollector.Metrics.VISITS_APPROVED, 0L);

        // Calculate acceptance rate
        long totalInvitations = metrics.invitationsAccepted + metrics.invitationsDeclined;
        metrics.acceptanceRate = (totalInvitations > 0)
                ? (metrics.invitationsAccepted * 100.0 / totalInvitations) : 0;

        return metrics;
    }

    private SystemMetricsData generateSystemMetrics(MetricsCollector.MetricsSnapshot snapshot) {
        SystemMetricsData metrics = new SystemMetricsData();

        if (snapshot.getSystemMetrics() != null) {
            SystemMetrics.SystemSnapshot sys = snapshot.getSystemMetrics();
            metrics.setCpuUsage(sys.getCpuUsage());
            metrics.setMemoryUsage(sys.getMemoryUsagePercent());
            metrics.setUsedMemoryMB(sys.getUsedMemoryMB());
            metrics.setAvailableMemoryMB(sys.getAvailableMemoryMB());
            metrics.setThreadCount(sys.getThreadCount());
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
        html.append("<p>Generated at: ").append(report.getGeneratedAt()).append("</p>\n");

        // Server Status
        html.append("<h2>Server Status</h2>\n");
        html.append("<table>\n");
        html.append("<tr><td>Status</td><td class='metric-value'>")
            .append(report.getServerStatus().isRunning() ? "Running" : "Stopped").append("</td></tr>\n");
        html.append("<tr><td>Players</td><td class='metric-value'>")
            .append(report.getServerStatus().getPlayerCount()).append(" / ").append(report.getServerStatus().getMaxPlayers()).append("</td></tr>\n");
        html.append("<tr><td>Average Tick Time</td><td class='metric-value'>")
            .append(String.format("%.2f ms", report.getServerStatus().getTickTime())).append("</td></tr>\n");
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
        private LocalDateTime generatedAt;
        private ServerStatus serverStatus;
        private PerformanceMetrics performanceMetrics;
        private WebSocketMetrics webSocketMetrics;
        private CollaborationMetrics collaborationMetrics;
        private SystemMetricsData systemMetrics;
        private List<PlayerActivityData> playerActivity;

        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }
        public void setGeneratedAt(LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
        }
        public ServerStatus getServerStatus() {
            return serverStatus;
        }
        public void setServerStatus(ServerStatus serverStatus) {
            this.serverStatus = serverStatus;
        }
        public PerformanceMetrics getPerformanceMetrics() {
            return performanceMetrics;
        }
        public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) {
            this.performanceMetrics = performanceMetrics;
        }
        public WebSocketMetrics getWebSocketMetrics() {
            return webSocketMetrics;
        }
        public void setWebSocketMetrics(WebSocketMetrics webSocketMetrics) {
            this.webSocketMetrics = webSocketMetrics;
        }
        public CollaborationMetrics getCollaborationMetrics() {
            return collaborationMetrics;
        }
        public void setCollaborationMetrics(CollaborationMetrics collaborationMetrics) {
            this.collaborationMetrics = collaborationMetrics;
        }
        public SystemMetricsData getSystemMetrics() {
            return systemMetrics;
        }
        public void setSystemMetrics(SystemMetricsData systemMetrics) {
            this.systemMetrics = systemMetrics;
        }
        public List<PlayerActivityData> getPlayerActivity() {
            return playerActivity;
        }
        public void setPlayerActivity(List<PlayerActivityData> playerActivity) {
            this.playerActivity = playerActivity;
        }
    }

    public static class ServerStatus {
        private boolean isRunning;
        private int playerCount;
        private int maxPlayers;
        private double tickTime;
        private String motd;

        public boolean isRunning() {
            return isRunning;
        }
        public void setRunning(boolean running) {
            isRunning = running;
        }
        public int getPlayerCount() {
            return playerCount;
        }
        public void setPlayerCount(int playerCount) {
            this.playerCount = playerCount;
        }
        public int getMaxPlayers() {
            return maxPlayers;
        }
        public void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }
        public double getTickTime() {
            return tickTime;
        }
        public void setTickTime(double tickTime) {
            this.tickTime = tickTime;
        }
        public String getMotd() {
            return motd;
        }
        public void setMotd(String motd) {
            this.motd = motd;
        }
    }

    public static class PerformanceMetrics {
        private long blocksPlaced;
        private long blocksBroken;
        private long batchOperations;
        private double cacheHitRate;
        private Map<String, Double> commandTimings = new ConcurrentHashMap<>();

        public long getBlocksPlaced() {
            return blocksPlaced;
        }
        public void setBlocksPlaced(long blocksPlaced) {
            this.blocksPlaced = blocksPlaced;
        }
        public long getBlocksBroken() {
            return blocksBroken;
        }
        public void setBlocksBroken(long blocksBroken) {
            this.blocksBroken = blocksBroken;
        }
        public long getBatchOperations() {
            return batchOperations;
        }
        public void setBatchOperations(long batchOperations) {
            this.batchOperations = batchOperations;
        }
        public double getCacheHitRate() {
            return cacheHitRate;
        }
        public void setCacheHitRate(double cacheHitRate) {
            this.cacheHitRate = cacheHitRate;
        }
        public Map<String, Double> getCommandTimings() {
            return commandTimings;
        }
        public void setCommandTimings(Map<String, Double> commandTimings) {
            this.commandTimings = commandTimings;
        }
    }

    public static class WebSocketMetrics {
        private long totalConnections;
        private long activeConnections;
        private long messagesReceived;
        private long messagesSent;
        private long errors;

        public long getTotalConnections() {
            return totalConnections;
        }
        public void setTotalConnections(long totalConnections) {
            this.totalConnections = totalConnections;
        }
        public long getActiveConnections() {
            return activeConnections;
        }
        public void setActiveConnections(long activeConnections) {
            this.activeConnections = activeConnections;
        }
        public long getMessagesReceived() {
            return messagesReceived;
        }
        public void setMessagesReceived(long messagesReceived) {
            this.messagesReceived = messagesReceived;
        }
        public long getMessagesSent() {
            return messagesSent;
        }
        public void setMessagesSent(long messagesSent) {
            this.messagesSent = messagesSent;
        }
        public long getErrors() {
            return errors;
        }
        public void setErrors(long errors) {
            this.errors = errors;
        }
    }

    public static class CollaborationMetrics {
        private long invitationsSent;
        private long invitationsAccepted;
        private long invitationsDeclined;
        private long visitsRequested;
        private long visitsApproved;
        private double acceptanceRate;

        public long getInvitationsSent() {
            return invitationsSent;
        }
        public void setInvitationsSent(long invitationsSent) {
            this.invitationsSent = invitationsSent;
        }
        public long getInvitationsAccepted() {
            return invitationsAccepted;
        }
        public void setInvitationsAccepted(long invitationsAccepted) {
            this.invitationsAccepted = invitationsAccepted;
        }
        public long getInvitationsDeclined() {
            return invitationsDeclined;
        }
        public void setInvitationsDeclined(long invitationsDeclined) {
            this.invitationsDeclined = invitationsDeclined;
        }
        public long getVisitsRequested() {
            return visitsRequested;
        }
        public void setVisitsRequested(long visitsRequested) {
            this.visitsRequested = visitsRequested;
        }
        public long getVisitsApproved() {
            return visitsApproved;
        }
        public void setVisitsApproved(long visitsApproved) {
            this.visitsApproved = visitsApproved;
        }
        public double getAcceptanceRate() {
            return acceptanceRate;
        }
        public void setAcceptanceRate(double acceptanceRate) {
            this.acceptanceRate = acceptanceRate;
        }
    }

    public static class SystemMetricsData {
        private double cpuUsage;
        private double memoryUsage;
        private long usedMemoryMB;
        private long availableMemoryMB;
        private int threadCount;

        public double getCpuUsage() {
            return cpuUsage;
        }
        public void setCpuUsage(double cpuUsage) {
            this.cpuUsage = cpuUsage;
        }
        public double getMemoryUsage() {
            return memoryUsage;
        }
        public void setMemoryUsage(double memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
        public long getUsedMemoryMB() {
            return usedMemoryMB;
        }
        public void setUsedMemoryMB(long usedMemoryMB) {
            this.usedMemoryMB = usedMemoryMB;
        }
        public long getAvailableMemoryMB() {
            return availableMemoryMB;
        }
        public void setAvailableMemoryMB(long availableMemoryMB) {
            this.availableMemoryMB = availableMemoryMB;
        }
        public int getThreadCount() {
            return threadCount;
        }
        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }
    }

    public static class PlayerActivityData {
        private String playerName;
        private String gameMode;
        private String dimension;
        private float health;
        private int foodLevel;

        public String getPlayerName() {
            return playerName;
        }
        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }
        public String getGameMode() {
            return gameMode;
        }
        public void setGameMode(String gameMode) {
            this.gameMode = gameMode;
        }
        public String getDimension() {
            return dimension;
        }
        public void setDimension(String dimension) {
            this.dimension = dimension;
        }
        public float getHealth() {
            return health;
        }
        public void setHealth(float health) {
            this.health = health;
        }
        public int getFoodLevel() {
            return foodLevel;
        }
        public void setFoodLevel(int foodLevel) {
            this.foodLevel = foodLevel;
        }
    }
}
