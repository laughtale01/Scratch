package edu.minecraft.collaboration.core;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.localization.LanguageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Queue;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * System-wide error management and logging
 */
public final class ErrorManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static ErrorManager instance;
    private final LanguageManager languageManager;
    
    // Error categories
    public enum ErrorCategory {
        COMMAND_EXECUTION("Command Execution Error", "Command Execution Error"),
        NETWORK_CONNECTION("Network Connection Error", "Network Connection Error"),
        DATA_VALIDATION("Data Validation Error", "Data Validation Error"),
        PERMISSION_DENIED("Permission Denied", "Permission Denied"),
        RESOURCE_NOT_FOUND("Resource Not Found", "Resource Not Found"),
        SYSTEM_INTERNAL("System Internal Error", "System Internal Error"),
        USER_INPUT("User Input Error", "User Input Error"),
        CONFIGURATION("Configuration Error", "Configuration Error");
        
        private final String japaneseDescription;
        private final String englishDescription;
        
        ErrorCategory(String japaneseDescription, String englishDescription) {
            this.japaneseDescription = japaneseDescription;
            this.englishDescription = englishDescription;
        }
        
        public String getDescription(String languageCode) {
            return "ja_JP".equals(languageCode) ? japaneseDescription : englishDescription;
        }
    }
    
    // Error severity levels
    public enum ErrorSeverity {
        LOW("Low", "Low", "§a"),
        MEDIUM("Medium", "Medium", "§e"),
        HIGH("High", "High", "§6"),
        CRITICAL("Critical", "Critical", "§c");
        
        private final String japaneseLevel;
        private final String englishLevel;
        private final String colorCode;
        
        ErrorSeverity(String japaneseLevel, String englishLevel, String colorCode) {
            this.japaneseLevel = japaneseLevel;
            this.englishLevel = englishLevel;
            this.colorCode = colorCode;
        }
        
        public String getLevel(String languageCode) {
            return "ja_JP".equals(languageCode) ? japaneseLevel : englishLevel;
        }
        
        public String getColorCode() {
            return colorCode;
        }
    }
    
    // Error storage and tracking
    private static final int MAX_ERROR_HISTORY = 1000;
    private final Queue<ErrorRecord> errorHistory = new ConcurrentLinkedQueue<>();
    private final Map<ErrorCategory, Integer> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> userErrorCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> rateLimitMap = new ConcurrentHashMap<>();
    
    // Rate limiting configuration
    private static final long RATE_LIMIT_WINDOW_MS = 5000; // 5 seconds
    private static final int MAX_ERRORS_PER_WINDOW = 10;
    
    private ErrorManager() {
        this.languageManager = DependencyInjector.getInstance().getService(LanguageManager.class);
        initializeErrorCounts();
    }
    
    public static ErrorManager getInstance() {
        if (instance == null) {
            instance = new ErrorManager();
        }
        return instance;
    }
    
    private void initializeErrorCounts() {
        for (ErrorCategory category : ErrorCategory.values()) {
            errorCounts.put(category, 0);
        }
    }
    
    /**
     * Record an error without player context
     */
    public void recordError(ErrorCategory category, ErrorSeverity severity, String message, Exception exception) {
        recordError(category, severity, message, exception, null);
    }
    
    /**
     * Record an error with player context
     */
    public void recordError(ErrorCategory category, ErrorSeverity severity, String message, Exception exception, UUID playerUUID) {
        String rateLimitKey = category.name() + "_" + (playerUUID != null ? playerUUID.toString() : "system");
        
        if (isRateLimited(rateLimitKey)) {
            return; // Skip if rate limited
        }
        
        // Create error record
        ErrorRecord errorRecord = new ErrorRecord(category, severity, message, exception, playerUUID);
        
        // Store in history
        errorHistory.offer(errorRecord);
        if (errorHistory.size() > MAX_ERROR_HISTORY) {
            errorHistory.poll(); // Remove oldest
        }
        
        // Update counters
        errorCounts.merge(category, 1, Integer::sum);
        if (playerUUID != null) {
            userErrorCounts.merge(playerUUID.toString(), 1, Integer::sum);
        }
        
        // Log the error
        logError(errorRecord);
        
        // Notify relevant players if appropriate
        if (severity == ErrorSeverity.HIGH || severity == ErrorSeverity.CRITICAL) {
            notifyRelevantPlayers(errorRecord);
        }
    }
    
    private boolean isRateLimited(String key) {
        long currentTime = System.currentTimeMillis();
        Long lastErrorTime = rateLimitMap.get(key);
        
        if (lastErrorTime == null || currentTime - lastErrorTime > RATE_LIMIT_WINDOW_MS) {
            rateLimitMap.put(key, currentTime);
            return false;
        }
        return true;
    }
    
    private void logError(ErrorRecord error) {
        String logMessage = String.format("[%s] %s: %s", 
            error.getSeverity().name(),
            error.getCategory().name(),
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
            default:
                LOGGER.warn("Unknown error severity: {}", error.getSeverity());
                break;
        }
    }
    
    private void notifyRelevantPlayers(ErrorRecord error) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }
            
            String message = formatErrorMessage(error, "en_US"); // Default to English
            Component component = Component.literal("§c[Error] " + message);
            
            if (error.getPlayerUUID() != null) {
                // Notify specific player
                ServerPlayer player = server.getPlayerList().getPlayer(error.getPlayerUUID());
                if (player != null) {
                    player.sendSystemMessage(component);
                }
            } else if (error.getSeverity() == ErrorSeverity.CRITICAL) {
                // Notify all administrators for critical system errors
                server.getPlayerList().getPlayers().stream()
                    .filter(player -> server.getPlayerList().isOp(player.getGameProfile()))
                    .forEach(admin -> admin.sendSystemMessage(component));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to notify players about error", e);
        }
    }
    
    private String formatErrorMessage(ErrorRecord error, String languageCode) {
        return String.format("%s: %s", 
            error.getCategory().getDescription(languageCode),
            error.getMessage()
        );
    }
    
    /**
     * Get error statistics for monitoring
     */
    public Map<String, Object> getErrorStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalErrors", errorHistory.size());
        stats.put("errorsByCategory", new HashMap<>(errorCounts));
        stats.put("errorsByUser", new HashMap<>(userErrorCounts));
        
        // Calculate error rates
        Map<ErrorSeverity, Long> severityCounts = new HashMap<>();
        for (ErrorSeverity severity : ErrorSeverity.values()) {
            long count = errorHistory.stream()
                .mapToLong(record -> record.getSeverity() == severity ? 1 : 0)
                .sum();
            severityCounts.put(severity, count);
        }
        stats.put("errorsBySeverity", severityCounts);
        
        return stats;
    }
    
    /**
     * Get recent errors for debugging
     */
    public List<ErrorRecord> getRecentErrors(int limit) {
        return errorHistory.stream()
            .skip(Math.max(0, errorHistory.size() - limit))
            .toList();
    }
    
    /**
     * Clear error history (for maintenance)
     */
    public void clearHistory() {
        errorHistory.clear();
        errorCounts.replaceAll((k, v) -> 0);
        userErrorCounts.clear();
        rateLimitMap.clear();
        LOGGER.info("Error history cleared");
    }
    
    /**
     * Error record data class
     */
    public static class ErrorRecord {
        private final ErrorCategory category;
        private final ErrorSeverity severity;
        private final String message;
        private final Exception exception;
        private final UUID playerUUID;
        private final LocalDateTime timestamp;
        
        public ErrorRecord(ErrorCategory category, ErrorSeverity severity, String message, 
                          Exception exception, UUID playerUUID) {
            this.category = category;
            this.severity = severity;
            this.message = message;
            this.exception = exception;
            this.playerUUID = playerUUID;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public ErrorCategory getCategory() {
            return category;
        }
        public ErrorSeverity getSeverity() {
            return severity;
        }
        public String getMessage() {
            return message;
        }
        public Exception getException() {
            return exception;
        }
        public UUID getPlayerUUID() {
            return playerUUID;
        }
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public String getFormattedTimestamp() {
            return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s - %s: %s", 
                getFormattedTimestamp(),
                severity.name(),
                category.name(),
                message
            );
        }
    }
}