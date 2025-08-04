package edu.minecraft.collaboration.core;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.teacher.TeacherManager;
import edu.minecraft.collaboration.progress.ProgressTracker;
import edu.minecraft.collaboration.localization.LanguageManager;
import edu.minecraft.collaboration.blockpacks.BlockPackManager;
import edu.minecraft.collaboration.offline.OfflineModeManager;
import edu.minecraft.collaboration.entities.AgentManager;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central system controller that manages all core components and their lifecycle
 */
public final class SystemController {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static SystemController instance;
    
    // Core components
    private final TeacherManager teacherManager;
    private final ProgressTracker progressTracker;
    private final LanguageManager languageManager;
    private final BlockPackManager blockPackManager;
    private final OfflineModeManager offlineModeManager;
    private final AgentManager agentManager;
    
    // System monitoring
    private final Map<String, Object> systemMetrics = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerSessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> componentStatus = new ConcurrentHashMap<>();
    
    // Scheduling
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    
    // System state
    private boolean systemRunning = false;
    private final long startTime = System.currentTimeMillis();
    
    private SystemController() {
        LOGGER.info("Initializing Minecraft Collaboration Learning System...");
        
        // Initialize all core components
        this.teacherManager = TeacherManager.getInstance();
        this.progressTracker = ProgressTracker.getInstance();
        this.languageManager = DependencyInjector.getInstance().getService(LanguageManager.class);
        this.blockPackManager = BlockPackManager.getInstance();
        this.offlineModeManager = OfflineModeManager.getInstance();
        this.agentManager = AgentManager.getInstance();
        
        initializeSystemMetrics();
        verifyComponentInitialization();
        
        LOGGER.info("System controller initialized successfully");
    }
    
    public static synchronized SystemController getInstance() {
        if (instance == null) {
            instance = new SystemController();
        }
        return instance;
    }
    
    /**
     * Start the system and all background processes
     */
    public void startSystem() {
        if (systemRunning) {
            LOGGER.warn("System is already running");
            return;
        }
        
        LOGGER.info("Starting Minecraft Collaboration Learning System...");
        
        try {
            // Start monitoring tasks
            startMonitoringTasks();
            
            // Start component-specific background tasks
            startComponentTasks();
            
            systemRunning = true;
            
            LOGGER.info("Minecraft Collaboration Learning System started successfully");
            
            // Notify all online players
            notifySystemStart();
            
        } catch (Exception e) {
            LOGGER.error("Failed to start system", e);
            throw new RuntimeException("System startup failed", e);
        }
    }
    
    /**
     * Stop the system and clean up resources
     */
    public void stopSystem() {
        if (!systemRunning) {
            LOGGER.warn("System is not running");
            return;
        }
        
        LOGGER.info("Shutting down Minecraft Collaboration Learning System...");
        
        try {
            // Stop scheduled tasks
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            // Save offline data
            if (offlineModeManager.getPendingActionsCount() > 0) {
                LOGGER.info("Saving {} pending offline actions", offlineModeManager.getPendingActionsCount());
            }
            
            // Log system statistics
            logSystemStatistics();
            
            // Notify shutdown
            notifySystemShutdown();
            
            systemRunning = false;
            
            LOGGER.info("Minecraft Collaboration Learning System shutdown complete");
            
        } catch (Exception e) {
            LOGGER.error("Error during system shutdown", e);
        }
    }
    
    /**
     * Initialize system metrics
     */
    private void initializeSystemMetrics() {
        systemMetrics.put("totalCommandsProcessed", 0L);
        systemMetrics.put("totalErrorsOccurred", 0L);
        systemMetrics.put("systemStartTime", startTime);
        systemMetrics.put("lastHealthCheck", System.currentTimeMillis());
        
        LOGGER.debug("System metrics initialized");
    }
    
    /**
     * Verify all components are properly initialized
     */
    private void verifyComponentInitialization() {
        componentStatus.put("TeacherManager", teacherManager != null);
        componentStatus.put("ProgressTracker", progressTracker != null);
        componentStatus.put("LanguageManager", languageManager != null);
        componentStatus.put("BlockPackManager", blockPackManager != null);
        componentStatus.put("OfflineModeManager", offlineModeManager != null);
        componentStatus.put("AgentManager", agentManager != null);
        
        // Check for failed components
        List<String> failedComponents = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : componentStatus.entrySet()) {
            if (!entry.getValue()) {
                failedComponents.add(entry.getKey());
            }
        }
        
        if (!failedComponents.isEmpty()) {
            LOGGER.error("Failed to initialize components: {}", failedComponents);
            throw new RuntimeException("Component initialization failed");
        }
        
        systemMetrics.put("componentStatus", componentStatus);
    }
    
    /**
     * Start monitoring tasks
     */
    private void startMonitoringTasks() {
        // Health check every 5 minutes
        scheduler.scheduleAtFixedRate(this::performHealthCheck, 5, 5, TimeUnit.MINUTES);
        
        // Memory monitoring every 10 minutes
        scheduler.scheduleAtFixedRate(this::monitorMemoryUsage, 10, 10, TimeUnit.MINUTES);
        
        // Metrics collection every minute
        scheduler.scheduleAtFixedRate(this::collectSystemMetrics, 1, 1, TimeUnit.MINUTES);
        
        LOGGER.info("Monitoring tasks started");
    }
    
    /**
     * Start component-specific background tasks
     */
    private void startComponentTasks() {
        // Offline mode synchronization (if enabled)
        if (offlineModeManager.isOfflineModeEnabled()) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    offlineModeManager.syncPendingActions();
                } catch (Exception e) {
                    LOGGER.error("Error during offline sync", e);
                }
            }, 30, 30, TimeUnit.SECONDS);
        }
        
        LOGGER.info("Component tasks started");
    }
    
    /**
     * Perform system health check
     */
    private void performHealthCheck() {
        try {
            LOGGER.debug("Performing system health check...");
            
            // Update last health check time
            systemMetrics.put("lastHealthCheck", System.currentTimeMillis());
            
            // Check memory usage
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            systemMetrics.put("memoryUsed", usedMemory);
            systemMetrics.put("memoryTotal", totalMemory);
            
            if (usedMemory > totalMemory * 0.8) {
                LOGGER.warn("High memory usage detected: {}MB / {}MB", 
                    usedMemory / (1024 * 1024), totalMemory / (1024 * 1024));
            }
            
            LOGGER.debug("Health check completed");
            
        } catch (Exception e) {
            LOGGER.error("Error during health check", e);
        }
    }
    
    /**
     * Monitor memory usage
     */
    private void monitorMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usagePercent = (double) usedMemory / totalMemory * 100;
        
        if (usagePercent > 85) {
            LOGGER.warn("Critical memory usage: {:.1f}%", usagePercent);
            // Force garbage collection
            System.gc();
        } else if (usagePercent > 70) {
            LOGGER.info("High memory usage: {:.1f}%", usagePercent);
        }
        
        systemMetrics.put("memoryUsagePercent", usagePercent);
    }
    
    /**
     * Collect system metrics
     */
    private void collectSystemMetrics() {
        try {
            // Update uptime
            long uptime = System.currentTimeMillis() - startTime;
            systemMetrics.put("uptimeMillis", uptime);
            
            // Count active players
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                int playerCount = server.getPlayerList().getPlayerCount();
                systemMetrics.put("activePlayerCount", playerCount);
            }
            
            // Component-specific metrics
            if (teacherManager.isClassroomMode()) {
                systemMetrics.put("classroomModeActive", true);
                systemMetrics.put("activeTeachers", teacherManager.getActiveTeacherCount());
            }
            
            if (offlineModeManager.isOfflineModeEnabled()) {
                systemMetrics.put("pendingOfflineActions", offlineModeManager.getPendingActionsCount());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error collecting system metrics", e);
        }
    }
    
    /**
     * Notify system start
     */
    private void notifySystemStart() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Component message = Component.literal("§a[System] Minecraft Collaboration Learning System started");
                server.getPlayerList().getPlayers().forEach(player -> 
                    player.sendSystemMessage(message)
                );
            }
        } catch (Exception e) {
            LOGGER.error("Error notifying system start", e);
        }
    }
    
    /**
     * Notify system shutdown
     */
    private void notifySystemShutdown() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Component message = Component.literal("§c[System] Minecraft Collaboration Learning System shutting down");
                server.getPlayerList().getPlayers().forEach(player -> 
                    player.sendSystemMessage(message)
                );
            }
        } catch (Exception e) {
            LOGGER.error("Error notifying system shutdown", e);
        }
    }
    
    /**
     * Log system statistics
     */
    private void logSystemStatistics() {
        LOGGER.info("========================");
        LOGGER.info("System Statistics Summary");
        LOGGER.info("========================");
        LOGGER.info("Uptime: {} minutes", getSystemUptimeMinutes());
        LOGGER.info("Commands processed: {}", systemMetrics.getOrDefault("totalCommandsProcessed", 0L));
        LOGGER.info("Errors occurred: {}", systemMetrics.getOrDefault("totalErrorsOccurred", 0L));
        LOGGER.info("Error rate: {:.2f}%", systemMetrics.getOrDefault("errorRate", 0.0));
        LOGGER.info("Active sessions: {}", playerSessions.size());
        LOGGER.info("Pending offline actions: {}", offlineModeManager.getPendingActionsCount());
        LOGGER.info("========================");
    }
    
    /**
     * Get primary player UUID (for single-player commands)
     */
    private UUID getPrimaryPlayerUUID() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                return player.getUUID();
            }
        } catch (Exception e) {
            LOGGER.debug("No primary player available", e);
        }
        return null;
    }
    
    /**
     * Get system uptime in minutes
     */
    public long getSystemUptimeMinutes() {
        return (System.currentTimeMillis() - startTime) / (1000 * 60);
    }
    
    /**
     * Check if system is healthy
     */
    public boolean isSystemHealthy() {
        try {
            // Check if all core components are working
            boolean componentsHealthy = componentStatus.values().stream().allMatch(status -> status);
            
            // Check memory usage
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            double usagePercent = (double) (totalMemory - freeMemory) / totalMemory * 100;
            boolean memoryHealthy = usagePercent < 90;
            
            // Check if system is responsive
            long lastHealthCheck = (Long) systemMetrics.getOrDefault("lastHealthCheck", 0L);
            boolean responsive = (System.currentTimeMillis() - lastHealthCheck) < 600000; // 10 minutes
            
            return componentsHealthy && memoryHealthy && responsive && systemRunning;
            
        } catch (Exception e) {
            LOGGER.error("Error checking system health", e);
            return false;
        }
    }
    
    /**
     * Get system metrics
     */
    public Map<String, Object> getSystemMetrics() {
        return new HashMap<>(systemMetrics);
    }
    
    /**
     * Get component status
     */
    public Map<String, Boolean> getComponentStatus() {
        return new HashMap<>(componentStatus);
    }
    
    /**
     * Check if system is running
     */
    public boolean isSystemRunning() {
        return systemRunning;
    }
    
    /**
     * Record player session start
     */
    public void recordPlayerSessionStart(UUID playerUUID) {
        playerSessions.put(playerUUID, System.currentTimeMillis());
    }
    
    /**
     * Record player session end
     */
    public void recordPlayerSessionEnd(UUID playerUUID) {
        playerSessions.remove(playerUUID);
    }
    
    /**
     * Increment command counter
     */
    public void incrementCommandCounter() {
        long current = (Long) systemMetrics.getOrDefault("totalCommandsProcessed", 0L);
        systemMetrics.put("totalCommandsProcessed", current + 1);
    }
    
    /**
     * Increment error counter
     */
    public void incrementErrorCounter() {
        long current = (Long) systemMetrics.getOrDefault("totalErrorsOccurred", 0L);
        systemMetrics.put("totalErrorsOccurred", current + 1);
        
        // Update error rate
        long totalCommands = (Long) systemMetrics.getOrDefault("totalCommandsProcessed", 1L);
        double errorRate = ((double) (current + 1) / totalCommands) * 100;
        systemMetrics.put("errorRate", errorRate);
    }
}