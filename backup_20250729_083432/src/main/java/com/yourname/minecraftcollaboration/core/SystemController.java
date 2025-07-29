package com.yourname.minecraftcollaboration.core;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import com.yourname.minecraftcollaboration.collaboration.CollaborationManager;
import com.yourname.minecraftcollaboration.teacher.TeacherManager;
import com.yourname.minecraftcollaboration.progress.ProgressTracker;
import com.yourname.minecraftcollaboration.localization.LanguageManager;
import com.yourname.minecraftcollaboration.blockpacks.BlockPackManager;
import com.yourname.minecraftcollaboration.offline.OfflineModeManager;
import com.yourname.minecraftcollaboration.entities.AgentManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * システム全体の統合管理とライフサイクル制御
 */
public class SystemController {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static SystemController instance;
    
    // システムコンポーネント
    private final CollaborationManager collaborationManager;
    private final TeacherManager teacherManager;
    private final ProgressTracker progressTracker;
    private final LanguageManager languageManager;
    private final BlockPackManager blockPackManager;
    private final OfflineModeManager offlineModeManager;
    private final AgentManager agentManager;
    
    // システム状態管理
    private boolean systemInitialized = false;
    private boolean systemRunning = false;
    private LocalDateTime systemStartTime;
    private final Map<String, Object> systemMetrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // パフォーマンス監視
    private long totalCommandsProcessed = 0;
    private long totalErrorsOccurred = 0;
    private final Map<String, Long> commandExecutionTimes = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSession> playerSessions = new ConcurrentHashMap<>();
    
    private SystemController() {
        this.collaborationManager = CollaborationManager.getInstance();
        this.teacherManager = TeacherManager.getInstance();
        this.progressTracker = ProgressTracker.getInstance();
        this.languageManager = LanguageManager.getInstance();
        this.blockPackManager = BlockPackManager.getInstance();
        this.offlineModeManager = OfflineModeManager.getInstance();
        this.agentManager = AgentManager.getInstance();
    }
    
    public static SystemController getInstance() {
        if (instance == null) {
            instance = new SystemController();
        }
        return instance;
    }
    
    /**
     * システム初期化
     */
    public void initializeSystem() {
        if (systemInitialized) {
            LOGGER.warn("System already initialized");
            return;
        }
        
        LOGGER.info("Initializing Minecraft Collaboration Learning System...");
        
        try {
            // 各コンポーネントの初期化確認
            verifyComponentInitialization();
            
            // システムメトリクスの初期化
            initializeSystemMetrics();
            
            // 定期タスクの開始
            startPeriodicTasks();
            
            // システム状態の設定
            systemInitialized = true;
            systemRunning = true;
            systemStartTime = LocalDateTime.now();
            
            LOGGER.info("Minecraft Collaboration Learning System initialized successfully");
            
            // 初期化完了通知
            notifySystemStart();
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize system", e);
            systemInitialized = false;
            systemRunning = false;
            throw new RuntimeException("System initialization failed", e);
        }
    }
    
    /**
     * システム終了処理
     */
    public void shutdownSystem() {
        if (!systemRunning) {
            return;
        }
        
        LOGGER.info("Shutting down Minecraft Collaboration Learning System...");
        
        try {
            // 定期タスクの停止
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            // オフラインデータの保存
            if (offlineModeManager.getPendingActionsCount() > 0) {
                LOGGER.info("Saving {} pending offline actions", offlineModeManager.getPendingActionsCount());
            }
            
            // システム統計の出力
            logSystemStatistics();
            
            // 終了通知
            notifySystemShutdown();
            
            systemRunning = false;
            
            LOGGER.info("Minecraft Collaboration Learning System shutdown complete");
            
        } catch (Exception e) {
            LOGGER.error("Error during system shutdown", e);
        }
    }
    
    /**
     * コンポーネント初期化確認
     */
    private void verifyComponentInitialization() {
        Map<String, Boolean> componentStatus = new HashMap<>();
        
        // 各コンポーネントの状態確認
        componentStatus.put("CollaborationManager", collaborationManager != null);
        componentStatus.put("TeacherManager", teacherManager != null);
        componentStatus.put("ProgressTracker", progressTracker != null);
        componentStatus.put("LanguageManager", languageManager != null);
        componentStatus.put("BlockPackManager", blockPackManager != null);
        componentStatus.put("OfflineModeManager", offlineModeManager != null);
        componentStatus.put("AgentManager", agentManager != null);
        
        // 初期化失敗したコンポーネントのチェック
        List<String> failedComponents = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : componentStatus.entrySet()) {
            if (!entry.getValue()) {
                failedComponents.add(entry.getKey());
            }
        }
        
        if (!failedComponents.isEmpty()) {
            throw new RuntimeException("Failed to initialize components: " + String.join(", ", failedComponents));
        }
        
        LOGGER.info("All system components initialized successfully");
        systemMetrics.put("componentStatus", componentStatus);
    }
    
    /**
     * システムメトリクスの初期化
     */
    private void initializeSystemMetrics() {
        systemMetrics.put("systemStartTime", systemStartTime);
        systemMetrics.put("totalCommandsProcessed", 0L);
        systemMetrics.put("totalErrorsOccurred", 0L);
        systemMetrics.put("activePlayerSessions", 0);
        systemMetrics.put("offlineModeEnabled", offlineModeManager.isOfflineModeEnabled());
        systemMetrics.put("classroomModeEnabled", teacherManager.isClassroomMode());
        systemMetrics.put("availableLanguages", languageManager.getSupportedLanguages().size());
        systemMetrics.put("availableBlockPacks", blockPackManager.getAllBlockPacks().size());
    }
    
    /**
     * 定期タスクの開始
     */
    private void startPeriodicTasks() {
        // システム統計更新（5分間隔）
        scheduler.scheduleAtFixedRate(this::updateSystemMetrics, 5, 5, TimeUnit.MINUTES);
        
        // パフォーマンス最適化（10分間隔）
        scheduler.scheduleAtFixedRate(this::performMaintenanceTasks, 10, 10, TimeUnit.MINUTES);
        
        // オフライン同期チェック（1分間隔）
        scheduler.scheduleAtFixedRate(this::checkOfflineSync, 1, 1, TimeUnit.MINUTES);
        
        LOGGER.info("Started periodic system tasks");
    }
    
    /**
     * プレイヤーセッション開始
     */
    public void startPlayerSession(UUID playerUUID, String playerName) {
        PlayerSession session = new PlayerSession(playerUUID, playerName);
        playerSessions.put(playerUUID, session);
        
        // 言語設定の確認
        String language = languageManager.getPlayerLanguage(playerUUID);
        session.setLanguage(language);
        
        // ブロックパックの確認
        String blockPack = blockPackManager.getPlayerActivePackId(playerUUID);
        session.setActiveBlockPack(blockPack);
        
        updateSystemMetrics();
        
        LOGGER.debug("Started player session: {} ({})", playerName, playerUUID);
    }
    
    /**
     * プレイヤーセッション終了
     */
    public void endPlayerSession(UUID playerUUID) {
        PlayerSession session = playerSessions.remove(playerUUID);
        if (session != null) {
            session.endSession();
            
            // セッション統計をオフラインマネージャーに記録
            if (offlineModeManager.isOfflineModeEnabled()) {
                Map<String, Object> sessionData = new HashMap<>();
                sessionData.put("sessionDuration", session.getSessionDurationMinutes());
                sessionData.put("commandsExecuted", session.getCommandCount());
                sessionData.put("language", session.getLanguage());
                sessionData.put("blockPack", session.getActiveBlockPack());
                
                offlineModeManager.cacheStudentData(playerUUID, sessionData);
            }
            
            updateSystemMetrics();
            
            LOGGER.debug("Ended player session: {} (Duration: {} minutes)", 
                session.getPlayerName(), session.getSessionDurationMinutes());
        }
    }
    
    /**
     * コマンド実行の記録
     */
    public void recordCommandExecution(String command, long executionTimeMs, boolean success) {
        totalCommandsProcessed++;
        
        if (!success) {
            totalErrorsOccurred++;
        }
        
        // 実行時間の記録
        commandExecutionTimes.merge(command, executionTimeMs, Long::sum);
        
        // プレイヤーセッションの更新
        UUID playerUUID = getCurrentPlayerUUID();
        if (playerUUID != null) {
            PlayerSession session = playerSessions.get(playerUUID);
            if (session != null) {
                session.incrementCommandCount();
                if (!success) {
                    session.incrementErrorCount();
                }
            }
        }
        
        // パフォーマンス警告
        if (executionTimeMs > 1000) { // 1秒以上
            LOGGER.warn("Slow command execution: {} took {}ms", command, executionTimeMs);
        }
    }
    
    /**
     * システムメトリクスの更新
     */
    private void updateSystemMetrics() {
        systemMetrics.put("totalCommandsProcessed", totalCommandsProcessed);
        systemMetrics.put("totalErrorsOccurred", totalErrorsOccurred);
        systemMetrics.put("activePlayerSessions", playerSessions.size());
        systemMetrics.put("offlineModeEnabled", offlineModeManager.isOfflineModeEnabled());
        systemMetrics.put("classroomModeEnabled", teacherManager.isClassroomMode());
        systemMetrics.put("pendingOfflineActions", offlineModeManager.getPendingActionsCount());
        systemMetrics.put("lastUpdated", LocalDateTime.now());
        
        // エラー率の計算
        if (totalCommandsProcessed > 0) {
            double errorRate = (double) totalErrorsOccurred / totalCommandsProcessed * 100;
            systemMetrics.put("errorRate", errorRate);
            
            // 高エラー率の警告
            if (errorRate > 10.0) {
                LOGGER.warn("High error rate detected: {:.2f}%", errorRate);
            }
        }
    }
    
    /**
     * メンテナンスタスクの実行
     */
    private void performMaintenanceTasks() {
        try {
            // 古いコマンド実行時間データのクリーンアップ
            if (commandExecutionTimes.size() > 1000) {
                commandExecutionTimes.clear();
                LOGGER.debug("Cleared command execution time cache");
            }
            
            // メモリ使用量のログ出力
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            systemMetrics.put("memoryUsed", usedMemory);
            systemMetrics.put("memoryTotal", totalMemory);
            
            if (usedMemory > totalMemory * 0.8) {
                LOGGER.warn("High memory usage: {}MB / {}MB", 
                    usedMemory / 1024 / 1024, totalMemory / 1024 / 1024);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during maintenance tasks", e);
        }
    }
    
    /**
     * オフライン同期チェック
     */
    private void checkOfflineSync() {
        try {
            if (!offlineModeManager.isOfflineModeEnabled() && 
                offlineModeManager.getPendingActionsCount() > 0 &&
                offlineModeManager.isAutoSyncEnabled() &&
                !offlineModeManager.isSyncInProgress()) {
                
                LOGGER.info("Auto-syncing {} pending offline actions", 
                    offlineModeManager.getPendingActionsCount());
                
                offlineModeManager.syncPendingActions();
            }
        } catch (Exception e) {
            LOGGER.error("Error during offline sync check", e);
        }
    }
    
    /**
     * システム開始通知
     */
    private void notifySystemStart() {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Component message = Component.literal("§a[System] Minecraft協調学習システムが開始されました");
                server.getPlayerList().getPlayers().forEach(player -> 
                    player.sendSystemMessage(message));
            }
        } catch (Exception e) {
            LOGGER.debug("Could not send system start notification", e);
        }
    }
    
    /**
     * システム終了通知
     */
    private void notifySystemShutdown() {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Component message = Component.literal("§e[System] Minecraft協調学習システムを終了しています...");
                server.getPlayerList().getPlayers().forEach(player -> 
                    player.sendSystemMessage(message));
            }
        } catch (Exception e) {
            LOGGER.debug("Could not send system shutdown notification", e);
        }
    }
    
    /**
     * システム統計のログ出力
     */
    private void logSystemStatistics() {
        LOGGER.info("=== System Statistics ===");
        LOGGER.info("Runtime: {} minutes", getSystemUptimeMinutes());
        LOGGER.info("Commands processed: {}", totalCommandsProcessed);
        LOGGER.info("Errors occurred: {}", totalErrorsOccurred);
        LOGGER.info("Error rate: {:.2f}%", systemMetrics.getOrDefault("errorRate", 0.0));
        LOGGER.info("Active sessions: {}", playerSessions.size());
        LOGGER.info("Pending offline actions: {}", offlineModeManager.getPendingActionsCount());
        LOGGER.info("========================");
    }
    
    /**
     * 現在のプレイヤーUUIDを取得（簡易実装）
     */
    private UUID getCurrentPlayerUUID() {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                return player.getUUID();
            }
        } catch (Exception e) {
            // Silent fail
        }
        return null;
    }
    
    /**
     * システム稼働時間を分で取得
     */
    public long getSystemUptimeMinutes() {
        if (systemStartTime == null) {
            return 0;
        }
        return java.time.Duration.between(systemStartTime, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * システム健全性チェック
     */
    public boolean isSystemHealthy() {
        if (!systemRunning) {
            return false;
        }
        
        // エラー率チェック
        Double errorRate = (Double) systemMetrics.get("errorRate");
        if (errorRate != null && errorRate > 20.0) {
            return false;
        }
        
        // メモリ使用量チェック
        Long memoryUsed = (Long) systemMetrics.get("memoryUsed");
        Long memoryTotal = (Long) systemMetrics.get("memoryTotal");
        if (memoryUsed != null && memoryTotal != null && memoryUsed > memoryTotal * 0.9) {
            return false;
        }
        
        return true;
    }
    
    // === ゲッター ===
    
    public boolean isSystemInitialized() {
        return systemInitialized;
    }
    
    public boolean isSystemRunning() {
        return systemRunning;
    }
    
    public LocalDateTime getSystemStartTime() {
        return systemStartTime;
    }
    
    public Map<String, Object> getSystemMetrics() {
        return new HashMap<>(systemMetrics);
    }
    
    public long getTotalCommandsProcessed() {
        return totalCommandsProcessed;
    }
    
    public long getTotalErrorsOccurred() {
        return totalErrorsOccurred;
    }
    
    public int getActivePlayerCount() {
        return playerSessions.size();
    }
    
    public Map<UUID, PlayerSession> getPlayerSessions() {
        return new HashMap<>(playerSessions);
    }
    
    /**
     * プレイヤーセッション情報
     */
    public static class PlayerSession {
        private final UUID playerUUID;
        private final String playerName;
        private final LocalDateTime sessionStart;
        private LocalDateTime sessionEnd;
        private String language;
        private String activeBlockPack;
        private int commandCount = 0;
        private int errorCount = 0;
        
        public PlayerSession(UUID playerUUID, String playerName) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.sessionStart = LocalDateTime.now();
        }
        
        public void endSession() {
            this.sessionEnd = LocalDateTime.now();
        }
        
        public void incrementCommandCount() {
            this.commandCount++;
        }
        
        public void incrementErrorCount() {
            this.errorCount++;
        }
        
        public long getSessionDurationMinutes() {
            LocalDateTime end = sessionEnd != null ? sessionEnd : LocalDateTime.now();
            return java.time.Duration.between(sessionStart, end).toMinutes();
        }
        
        // Getters and Setters
        public UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public LocalDateTime getSessionStart() { return sessionStart; }
        public LocalDateTime getSessionEnd() { return sessionEnd; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getActiveBlockPack() { return activeBlockPack; }
        public void setActiveBlockPack(String activeBlockPack) { this.activeBlockPack = activeBlockPack; }
        public int getCommandCount() { return commandCount; }
        public int getErrorCount() { return errorCount; }
    }
}