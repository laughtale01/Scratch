package com.yourname.minecraftcollaboration.core;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import com.yourname.minecraftcollaboration.localization.LanguageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * システム全体のエラー管理とログ機能
 */
public class ErrorManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static ErrorManager instance;
    private final LanguageManager languageManager;
    
    // エラー分類
    public enum ErrorCategory {
        COMMAND_EXECUTION("コマンド実行エラー", "Command Execution Error"),
        NETWORK_CONNECTION("ネットワーク接続エラー", "Network Connection Error"),
        DATA_VALIDATION("データ検証エラー", "Data Validation Error"),
        PERMISSION_DENIED("権限エラー", "Permission Denied"),
        RESOURCE_NOT_FOUND("リソース未発見", "Resource Not Found"),
        SYSTEM_INTERNAL("システム内部エラー", "System Internal Error"),
        OFFLINE_SYNC("オフライン同期エラー", "Offline Sync Error"),
        BLOCK_OPERATION("ブロック操作エラー", "Block Operation Error"),
        PLAYER_MANAGEMENT("プレイヤー管理エラー", "Player Management Error"),
        CONFIGURATION("設定エラー", "Configuration Error");
        
        private final String japaneseDescription;
        private final String englishDescription;
        
        ErrorCategory(String japaneseDescription, String englishDescription) {
            this.japaneseDescription = japaneseDescription;
            this.englishDescription = englishDescription;
        }
        
        public String getDescription(String language) {
            return "ja_JP".equals(language) ? japaneseDescription : englishDescription;
        }
    }
    
    // エラー重要度
    public enum ErrorSeverity {
        LOW("低", "Low", "§a"),
        MEDIUM("中", "Medium", "§e"),
        HIGH("高", "High", "§6"),
        CRITICAL("重大", "Critical", "§c");
        
        private final String japaneseLevel;
        private final String englishLevel;
        private final String colorCode;
        
        ErrorSeverity(String japaneseLevel, String englishLevel, String colorCode) {
            this.japaneseLevel = japaneseLevel;
            this.englishLevel = englishLevel;
            this.colorCode = colorCode;
        }
        
        public String getLevel(String language) {
            return "ja_JP".equals(language) ? japaneseLevel : englishLevel;
        }
        
        public String getColorCode() {
            return colorCode;
        }
    }
    
    // エラー記録
    private final Queue<ErrorRecord> errorHistory = new ConcurrentLinkedQueue<>();
    private final Map<ErrorCategory, Integer> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> userErrorCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> rateLimitMap = new ConcurrentHashMap<>();
    
    // 設定
    private static final int MAX_ERROR_HISTORY = 1000;
    private static final long RATE_LIMIT_WINDOW_MS = 5000; // 5秒
    private static final int MAX_ERRORS_PER_WINDOW = 10;
    
    private ErrorManager() {
        this.languageManager = LanguageManager.getInstance();
        initializeErrorCounts();
    }
    
    public static ErrorManager getInstance() {
        if (instance == null) {
            instance = new ErrorManager();
        }
        return instance;
    }
    
    /**
     * エラーカウンターの初期化
     */
    private void initializeErrorCounts() {
        for (ErrorCategory category : ErrorCategory.values()) {
            errorCounts.put(category, 0);
        }
    }
    
    /**
     * エラーを記録
     */
    public void recordError(ErrorCategory category, ErrorSeverity severity, String message, Exception exception) {
        recordError(category, severity, message, exception, null);
    }
    
    /**
     * エラーを記録（プレイヤー指定）
     */
    public void recordError(ErrorCategory category, ErrorSeverity severity, String message, Exception exception, UUID playerUUID) {
        // レート制限チェック
        String rateLimitKey = category.name() + (playerUUID != null ? ":" + playerUUID : "");
        if (isRateLimited(rateLimitKey)) {
            return;
        }
        
        // エラー記録の作成
        ErrorRecord errorRecord = new ErrorRecord(category, severity, message, exception, playerUUID);
        
        // 履歴に追加
        errorHistory.offer(errorRecord);
        if (errorHistory.size() > MAX_ERROR_HISTORY) {
            errorHistory.poll();
        }
        
        // カウンターの更新
        errorCounts.merge(category, 1, Integer::sum);
        if (playerUUID != null) {
            userErrorCounts.merge(playerUUID.toString(), 1, Integer::sum);
        }
        
        // ログ出力
        logError(errorRecord);
        
        // 重要度に応じた処理
        handleErrorBySeverity(errorRecord);
        
        // プレイヤーへの通知
        if (playerUUID != null && severity != ErrorSeverity.LOW) {
            notifyPlayer(playerUUID, errorRecord);
        }
        
        // システム管理者への通知
        if (severity == ErrorSeverity.CRITICAL) {
            notifyAdministrators(errorRecord);
        }
    }
    
    /**
     * レート制限チェック
     */
    private boolean isRateLimited(String key) {
        long currentTime = System.currentTimeMillis();
        Long lastErrorTime = rateLimitMap.get(key);
        
        if (lastErrorTime == null || currentTime - lastErrorTime > RATE_LIMIT_WINDOW_MS) {
            rateLimitMap.put(key, currentTime);
            return false;
        }
        
        return true;
    }
    
    /**
     * エラーのログ出力
     */
    private void logError(ErrorRecord error) {
        String logMessage = String.format("[%s] %s - %s: %s", 
            error.getSeverity().name(),
            error.getCategory().name(),
            error.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            error.getMessage()
        );
        
        switch (error.getSeverity()) {
            case LOW:
                LOGGER.debug(logMessage, error.getException());
                break;
            case MEDIUM:
                LOGGER.info(logMessage, error.getException());
                break;
            case HIGH:
                LOGGER.warn(logMessage, error.getException());
                break;
            case CRITICAL:
                LOGGER.error(logMessage, error.getException());
                break;
        }
    }
    
    /**
     * 重要度に応じたエラー処理
     */
    private void handleErrorBySeverity(ErrorRecord error) {
        switch (error.getSeverity()) {
            case CRITICAL:
                // クリティカルエラーの特別処理
                handleCriticalError(error);
                break;
            case HIGH:
                // 高重要度エラーの処理
                handleHighSeverityError(error);
                break;
            case MEDIUM:
                // 中重要度エラーの処理
                handleMediumSeverityError(error);
                break;
            case LOW:
                // 低重要度エラーの処理（ログのみ）
                break;
        }
    }
    
    /**
     * クリティカルエラーの処理
     */
    private void handleCriticalError(ErrorRecord error) {
        // システムコントローラーに通知
        SystemController systemController = SystemController.getInstance();
        if (systemController.isSystemRunning()) {
            // 必要に応じてシステムの一部機能を無効化
            LOGGER.error("Critical error detected, system stability may be compromised");
        }
        
        // オフラインモードを強制的に有効化（データ保護のため）
        if (error.getCategory() == ErrorCategory.NETWORK_CONNECTION) {
            // NetworkConnectionManager.forceOfflineMode(); // 実装があれば
        }
    }
    
    /**
     * 高重要度エラーの処理
     */
    private void handleHighSeverityError(ErrorRecord error) {
        // 特定のカテゴリのエラーが連続した場合の処理
        long recentHighErrors = errorHistory.stream()
            .filter(e -> e.getSeverity() == ErrorSeverity.HIGH)
            .filter(e -> e.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(5)))
            .count();
        
        if (recentHighErrors > 5) {
            LOGGER.warn("Multiple high-severity errors detected in the last 5 minutes: {}", recentHighErrors);
        }
    }
    
    /**
     * 中重要度エラーの処理
     */
    private void handleMediumSeverityError(ErrorRecord error) {
        // 特定ユーザーのエラーが多い場合の処理
        if (error.getPlayerUUID() != null) {
            Integer userErrors = userErrorCounts.get(error.getPlayerUUID().toString());
            if (userErrors != null && userErrors > 20) {
                LOGGER.info("User {} has generated {} errors, may need assistance", 
                    error.getPlayerUUID(), userErrors);
            }
        }
    }
    
    /**
     * プレイヤーへのエラー通知
     */
    private void notifyPlayer(UUID playerUUID, ErrorRecord error) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                if (player != null) {
                    String language = languageManager.getPlayerLanguage(playerUUID);
                    
                    String categoryDesc = error.getCategory().getDescription(language);
                    String severityDesc = error.getSeverity().getLevel(language);
                    String colorCode = error.getSeverity().getColorCode();
                    
                    String userMessage;
                    if ("ja_JP".equals(language)) {
                        userMessage = String.format("%s[%s] %s: %s", 
                            colorCode, severityDesc, categoryDesc, getUserFriendlyMessage(error, language));
                    } else {
                        userMessage = String.format("%s[%s] %s: %s", 
                            colorCode, severityDesc, categoryDesc, getUserFriendlyMessage(error, language));
                    }
                    
                    player.sendSystemMessage(Component.literal(userMessage));
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to notify player of error", e);
        }
    }
    
    /**
     * 管理者への通知
     */
    private void notifyAdministrators(ErrorRecord error) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                String adminMessage = String.format("§c[CRITICAL ERROR] %s: %s", 
                    error.getCategory().getDescription("en_US"), error.getMessage());
                
                server.getPlayerList().getPlayers().forEach(player -> {
                    // 教師権限を持つプレイヤーに通知（TeacherManagerとの連携）
                    try {
                        SystemController systemController = SystemController.getInstance();
                        // if (teacherManager.isTeacher(player.getUUID())) {
                        //     player.sendSystemMessage(Component.literal(adminMessage));
                        // }
                        player.sendSystemMessage(Component.literal(adminMessage));
                    } catch (Exception e) {
                        // Silent fail
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to notify administrators", e);
        }
    }
    
    /**
     * ユーザーフレンドリーなエラーメッセージの生成
     */
    private String getUserFriendlyMessage(ErrorRecord error, String language) {
        switch (error.getCategory()) {
            case COMMAND_EXECUTION:
                return "ja_JP".equals(language) ? "コマンドの実行に失敗しました" : "Command execution failed";
            case NETWORK_CONNECTION:
                return "ja_JP".equals(language) ? "ネットワークに接続できません" : "Cannot connect to network";
            case DATA_VALIDATION:
                return "ja_JP".equals(language) ? "入力データが無効です" : "Invalid input data";
            case PERMISSION_DENIED:
                return "ja_JP".equals(language) ? "この操作の権限がありません" : "Permission denied for this operation";
            case RESOURCE_NOT_FOUND:
                return "ja_JP".equals(language) ? "指定されたリソースが見つかりません" : "Specified resource not found";
            case OFFLINE_SYNC:
                return "ja_JP".equals(language) ? "オフラインデータの同期に失敗しました" : "Failed to sync offline data";
            case BLOCK_OPERATION:
                return "ja_JP".equals(language) ? "ブロック操作を実行できませんでした" : "Block operation could not be executed";
            case PLAYER_MANAGEMENT:
                return "ja_JP".equals(language) ? "プレイヤー管理操作に失敗しました" : "Player management operation failed";
            case CONFIGURATION:
                return "ja_JP".equals(language) ? "設定に問題があります" : "Configuration issue detected";
            default:
                return error.getMessage();
        }
    }
    
    /**
     * エラー統計の取得
     */
    public Map<String, Object> getErrorStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // カテゴリ別エラー数
        stats.put("errorsByCategory", new HashMap<>(errorCounts));
        
        // 重要度別エラー数
        Map<ErrorSeverity, Long> severityCounts = new HashMap<>();
        for (ErrorSeverity severity : ErrorSeverity.values()) {
            long count = errorHistory.stream()
                .filter(e -> e.getSeverity() == severity)
                .count();
            severityCounts.put(severity, count);
        }
        stats.put("errorsBySeverity", severityCounts);
        
        // 最近のエラー（直近1時間）
        long recentErrors = errorHistory.stream()
            .filter(e -> e.getTimestamp().isAfter(LocalDateTime.now().minusHours(1)))
            .count();
        stats.put("recentErrors", recentErrors);
        
        // 総エラー数
        stats.put("totalErrors", errorHistory.size());
        
        // ユーザー別エラー数（上位5位）
        Map<String, Integer> topUserErrors = userErrorCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
        stats.put("topUserErrors", topUserErrors);
        
        return stats;
    }
    
    /**
     * エラー履歴のクリア
     */
    public void clearErrorHistory() {
        errorHistory.clear();
        errorCounts.replaceAll((k, v) -> 0);
        userErrorCounts.clear();
        rateLimitMap.clear();
        LOGGER.info("Error history cleared");
    }
    
    /**
     * 最近のエラーを取得
     */
    public List<ErrorRecord> getRecentErrors(int count) {
        return errorHistory.stream()
            .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
            .limit(count)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * エラーレコードクラス
     */
    public static class ErrorRecord {
        private final String errorId;
        private final ErrorCategory category;
        private final ErrorSeverity severity;
        private final String message;
        private final Exception exception;
        private final UUID playerUUID;
        private final LocalDateTime timestamp;
        
        public ErrorRecord(ErrorCategory category, ErrorSeverity severity, String message, Exception exception, UUID playerUUID) {
            this.errorId = UUID.randomUUID().toString().substring(0, 8);
            this.category = category;
            this.severity = severity;
            this.message = message;
            this.exception = exception;
            this.playerUUID = playerUUID;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public String getErrorId() { return errorId; }
        public ErrorCategory getCategory() { return category; }
        public ErrorSeverity getSeverity() { return severity; }
        public String getMessage() { return message; }
        public Exception getException() { return exception; }
        public UUID getPlayerUUID() { return playerUUID; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("ErrorRecord[id=%s, category=%s, severity=%s, time=%s, message=%s]",
                errorId, category, severity, 
                timestamp.format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")), message);
        }
    }
}