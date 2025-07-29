package com.yourname.minecraftcollaboration.offline;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import com.yourname.minecraftcollaboration.localization.LanguageManager;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * オフライン環境での学習データ管理システム
 * インターネット接続がない場合でも協調学習機能を提供
 */
public class OfflineModeManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static OfflineModeManager instance;
    private final LanguageManager languageManager;
    
    // オフラインモード設定
    private boolean offlineModeEnabled = false;
    private boolean autoSyncEnabled = true;
    private String offlineDataDirectory = "offline_data";
    
    // オフラインデータ管理
    private final Map<String, OfflineSession> offlineSessions = new ConcurrentHashMap<>();
    private final Queue<OfflineAction> pendingActions = new LinkedList<>();
    private final Map<UUID, OfflineStudentData> studentDataCache = new ConcurrentHashMap<>();
    
    // 同期状態管理
    private LocalDateTime lastSyncTime;
    private boolean syncInProgress = false;
    private int pendingActionsCount = 0;
    
    private OfflineModeManager() {
        this.languageManager = LanguageManager.getInstance();
        initializeOfflineDirectory();
        loadOfflineData();
    }
    
    public static OfflineModeManager getInstance() {
        if (instance == null) {
            instance = new OfflineModeManager();
        }
        return instance;
    }
    
    /**
     * オフラインディレクトリの初期化
     */
    private void initializeOfflineDirectory() {
        try {
            Path offlinePath = Paths.get(offlineDataDirectory);
            if (!Files.exists(offlinePath)) {
                Files.createDirectories(offlinePath);
                LOGGER.info("Created offline data directory: {}", offlineDataDirectory);
            }
            
            // サブディレクトリの作成
            Files.createDirectories(offlinePath.resolve("sessions"));
            Files.createDirectories(offlinePath.resolve("progress"));
            Files.createDirectories(offlinePath.resolve("activities"));
            Files.createDirectories(offlinePath.resolve("sync"));
            
        } catch (IOException e) {
            LOGGER.error("Failed to create offline data directory", e);
        }
    }
    
    /**
     * オフラインモードの有効/無効を設定
     */
    public void setOfflineModeEnabled(boolean enabled) {
        this.offlineModeEnabled = enabled;
        
        if (enabled) {
            LOGGER.info("Offline mode ENABLED");
            startOfflineSession();
        } else {
            LOGGER.info("Offline mode DISABLED");
            endCurrentOfflineSession();
            
            // オンラインに戻った時に自動同期
            if (autoSyncEnabled) {
                scheduleAutoSync();
            }
        }
    }
    
    /**
     * オフラインモードの状態を取得
     */
    public boolean isOfflineModeEnabled() {
        return offlineModeEnabled;
    }
    
    /**
     * オフラインセッションを開始
     */
    private void startOfflineSession() {
        String sessionId = "session_" + System.currentTimeMillis();
        OfflineSession session = new OfflineSession(sessionId, LocalDateTime.now());
        
        offlineSessions.put(sessionId, session);
        LOGGER.info("Started offline session: {}", sessionId);
        
        // 現在の状態をスナップショットとして保存
        saveCurrentStateSnapshot(sessionId);
    }
    
    /**
     * 現在のオフラインセッションを終了
     */
    private void endCurrentOfflineSession() {
        if (!offlineSessions.isEmpty()) {
            String latestSessionId = getLatestSessionId();
            OfflineSession session = offlineSessions.get(latestSessionId);
            
            if (session != null) {
                session.endSession();
                saveOfflineSession(session);
                LOGGER.info("Ended offline session: {}", latestSessionId);
            }
        }
    }
    
    /**
     * オフラインアクションを記録
     */
    public void recordOfflineAction(String actionType, Map<String, Object> actionData) {
        if (!offlineModeEnabled) {
            return;
        }
        
        OfflineAction action = new OfflineAction(
            UUID.randomUUID().toString(),
            actionType,
            actionData,
            LocalDateTime.now()
        );
        
        pendingActions.offer(action);
        pendingActionsCount++;
        
        // ローカルファイルに即座に保存
        saveOfflineAction(action);
        
        LOGGER.debug("Recorded offline action: {} (Total pending: {})", actionType, pendingActionsCount);
    }
    
    /**
     * 学生データをオフラインキャッシュに保存
     */
    public void cacheStudentData(UUID studentUUID, Map<String, Object> studentData) {
        OfflineStudentData data = studentDataCache.computeIfAbsent(
            studentUUID, 
            k -> new OfflineStudentData(studentUUID)
        );
        
        data.updateData(studentData);
        saveStudentDataCache(data);
        
        LOGGER.debug("Cached student data for: {}", studentUUID);
    }
    
    /**
     * オフラインキャッシュから学生データを取得
     */
    public Map<String, Object> getCachedStudentData(UUID studentUUID) {
        OfflineStudentData data = studentDataCache.get(studentUUID);
        return data != null ? data.getData() : new HashMap<>();
    }
    
    /**
     * 保留中のアクションを同期
     */
    public boolean syncPendingActions() {
        if (syncInProgress) {
            LOGGER.warn("Sync already in progress");
            return false;
        }
        
        syncInProgress = true;
        
        try {
            LOGGER.info("Starting sync of {} pending actions", pendingActionsCount);
            
            List<OfflineAction> actionsToSync = new ArrayList<>(pendingActions);
            int successCount = 0;
            int failureCount = 0;
            
            for (OfflineAction action : actionsToSync) {
                try {
                    if (syncSingleAction(action)) {
                        pendingActions.remove(action);
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to sync action: {}", action.getActionType(), e);
                    failureCount++;
                }
            }
            
            pendingActionsCount = pendingActions.size();
            lastSyncTime = LocalDateTime.now();
            
            LOGGER.info("Sync completed - Success: {}, Failed: {}, Remaining: {}", 
                successCount, failureCount, pendingActionsCount);
            
            return failureCount == 0;
            
        } finally {
            syncInProgress = false;
        }
    }
    
    /**
     * 単一アクションの同期
     */
    private boolean syncSingleAction(OfflineAction action) {
        // 実際の同期ロジックをここに実装
        // この例では、アクションタイプに応じて適切な同期処理を実行
        
        switch (action.getActionType()) {
            case "build_block":
                return syncBuildAction(action);
            case "chat_message":
                return syncChatAction(action);
            case "progress_update":
                return syncProgressAction(action);
            case "achievement_earned":
                return syncAchievementAction(action);
            default:
                LOGGER.warn("Unknown action type for sync: {}", action.getActionType());
                return false;
        }
    }
    
    /**
     * 建築アクションの同期
     */
    private boolean syncBuildAction(OfflineAction action) {
        try {
            // 建築データをサーバーまたはメインシステムに送信
            LOGGER.debug("Syncing build action: {}", action.getActionId());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to sync build action", e);
            return false;
        }
    }
    
    /**
     * チャットアクションの同期
     */
    private boolean syncChatAction(OfflineAction action) {
        try {
            // チャットログをメインシステムに送信
            LOGGER.debug("Syncing chat action: {}", action.getActionId());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to sync chat action", e);
            return false;
        }
    }
    
    /**
     * 進捗アクションの同期
     */
    private boolean syncProgressAction(OfflineAction action) {
        try {
            // 学習進捗をメインシステムに送信
            LOGGER.debug("Syncing progress action: {}", action.getActionId());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to sync progress action", e);
            return false;
        }
    }
    
    /**
     * 達成度アクションの同期
     */
    private boolean syncAchievementAction(OfflineAction action) {
        try {
            // 達成度をメインシステムに送信
            LOGGER.debug("Syncing achievement action: {}", action.getActionId());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to sync achievement action", e);
            return false;
        }
    }
    
    /**
     * 自動同期のスケジュール
     */
    private void scheduleAutoSync() {
        // バックグラウンドで定期的に同期を実行
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 5秒待機してから同期開始
                if (!offlineModeEnabled && pendingActionsCount > 0) {
                    syncPendingActions();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.debug("Auto sync interrupted");
            }
        }).start();
    }
    
    /**
     * オフラインデータのエクスポート
     */
    public String exportOfflineData() {
        try {
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("sessions", new ArrayList<>(offlineSessions.values()));
            exportData.put("pendingActions", new ArrayList<>(pendingActions));
            exportData.put("studentDataCache", new HashMap<>(studentDataCache));
            exportData.put("exportTime", LocalDateTime.now().toString());
            exportData.put("pendingActionsCount", pendingActionsCount);
            exportData.put("lastSyncTime", lastSyncTime != null ? lastSyncTime.toString() : "Never");
            
            return exportData.toString();
            
        } catch (Exception e) {
            LOGGER.error("Failed to export offline data", e);
            return "Export failed: " + e.getMessage();
        }
    }
    
    /**
     * オフライン統計の取得
     */
    public Map<String, Object> getOfflineStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("offlineModeEnabled", offlineModeEnabled);
        stats.put("totalSessions", offlineSessions.size());
        stats.put("pendingActionsCount", pendingActionsCount);
        stats.put("cachedStudentsCount", studentDataCache.size());
        stats.put("lastSyncTime", lastSyncTime != null ? lastSyncTime.toString() : "Never");
        stats.put("syncInProgress", syncInProgress);
        stats.put("autoSyncEnabled", autoSyncEnabled);
        
        return stats;
    }
    
    // === ファイル操作メソッド ===
    
    /**
     * 現在の状態スナップショットを保存
     */
    private void saveCurrentStateSnapshot(String sessionId) {
        try {
            Path snapshotPath = Paths.get(offlineDataDirectory, "sessions", sessionId + "_snapshot.json");
            
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("sessionId", sessionId);
            snapshot.put("timestamp", LocalDateTime.now().toString());
            snapshot.put("playerCount", 0); // 実際の値に置き換え
            snapshot.put("worldState", "saved"); // 実際のワールド状態
            
            Files.write(snapshotPath, snapshot.toString().getBytes());
            
        } catch (IOException e) {
            LOGGER.error("Failed to save state snapshot", e);
        }
    }
    
    /**
     * オフラインセッションをファイルに保存
     */
    private void saveOfflineSession(OfflineSession session) {
        try {
            Path sessionPath = Paths.get(offlineDataDirectory, "sessions", session.getSessionId() + ".json");
            Files.write(sessionPath, session.toJson().getBytes());
            
        } catch (IOException e) {
            LOGGER.error("Failed to save offline session", e);
        }
    }
    
    /**
     * オフラインアクションをファイルに保存
     */
    private void saveOfflineAction(OfflineAction action) {
        try {
            Path actionPath = Paths.get(offlineDataDirectory, "activities", action.getActionId() + ".json");
            Files.write(actionPath, action.toJson().getBytes());
            
        } catch (IOException e) {
            LOGGER.error("Failed to save offline action", e);
        }
    }
    
    /**
     * 学生データキャッシュをファイルに保存
     */
    private void saveStudentDataCache(OfflineStudentData data) {
        try {
            Path cachePath = Paths.get(offlineDataDirectory, "progress", data.getStudentUUID() + ".json");
            Files.write(cachePath, data.toJson().getBytes());
            
        } catch (IOException e) {
            LOGGER.error("Failed to save student data cache", e);
        }
    }
    
    /**
     * オフラインデータの読み込み
     */
    private void loadOfflineData() {
        try {
            loadOfflineSessions();
            loadPendingActions();
            loadStudentDataCache();
            
            LOGGER.info("Loaded offline data - Sessions: {}, Pending actions: {}, Cached students: {}", 
                offlineSessions.size(), pendingActionsCount, studentDataCache.size());
                
        } catch (Exception e) {
            LOGGER.error("Failed to load offline data", e);
        }
    }
    
    /**
     * オフラインセッションの読み込み
     */
    private void loadOfflineSessions() throws IOException {
        Path sessionsDir = Paths.get(offlineDataDirectory, "sessions");
        if (Files.exists(sessionsDir)) {
            Files.list(sessionsDir)
                .filter(path -> path.toString().endsWith(".json") && !path.toString().contains("_snapshot"))
                .forEach(this::loadSingleSession);
        }
    }
    
    /**
     * 単一セッションの読み込み
     */
    private void loadSingleSession(Path sessionPath) {
        try {
            String content = new String(Files.readAllBytes(sessionPath));
            OfflineSession session = OfflineSession.fromJson(content);
            offlineSessions.put(session.getSessionId(), session);
            
        } catch (IOException e) {
            LOGGER.error("Failed to load session from: {}", sessionPath, e);
        }
    }
    
    /**
     * 保留中アクションの読み込み
     */
    private void loadPendingActions() throws IOException {
        Path activitiesDir = Paths.get(offlineDataDirectory, "activities");
        if (Files.exists(activitiesDir)) {
            Files.list(activitiesDir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(this::loadSingleAction);
                
            pendingActionsCount = pendingActions.size();
        }
    }
    
    /**
     * 単一アクションの読み込み
     */
    private void loadSingleAction(Path actionPath) {
        try {
            String content = new String(Files.readAllBytes(actionPath));
            OfflineAction action = OfflineAction.fromJson(content);
            pendingActions.offer(action);
            
        } catch (IOException e) {
            LOGGER.error("Failed to load action from: {}", actionPath, e);
        }
    }
    
    /**
     * 学生データキャッシュの読み込み
     */
    private void loadStudentDataCache() throws IOException {
        Path progressDir = Paths.get(offlineDataDirectory, "progress");
        if (Files.exists(progressDir)) {
            Files.list(progressDir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(this::loadSingleStudentData);
        }
    }
    
    /**
     * 単一学生データの読み込み
     */
    private void loadSingleStudentData(Path dataPath) {
        try {
            String content = new String(Files.readAllBytes(dataPath));
            OfflineStudentData data = OfflineStudentData.fromJson(content);
            studentDataCache.put(data.getStudentUUID(), data);
            
        } catch (IOException e) {
            LOGGER.error("Failed to load student data from: {}", dataPath, e);
        }
    }
    
    /**
     * 最新のセッションIDを取得
     */
    private String getLatestSessionId() {
        return offlineSessions.keySet().stream()
            .max(Comparator.naturalOrder())
            .orElse("");
    }
    
    // === 設定メソッド ===
    
    /**
     * 自動同期の有効/無効を設定
     */
    public void setAutoSyncEnabled(boolean enabled) {
        this.autoSyncEnabled = enabled;
        LOGGER.info("Auto sync: {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    /**
     * オフラインデータディレクトリを設定
     */
    public void setOfflineDataDirectory(String directory) {
        this.offlineDataDirectory = directory;
        initializeOfflineDirectory();
        LOGGER.info("Offline data directory set to: {}", directory);
    }
    
    // === ゲッター ===
    
    public boolean isAutoSyncEnabled() {
        return autoSyncEnabled;
    }
    
    public String getOfflineDataDirectory() {
        return offlineDataDirectory;
    }
    
    public int getPendingActionsCount() {
        return pendingActionsCount;
    }
    
    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }
    
    public boolean isSyncInProgress() {
        return syncInProgress;
    }
}