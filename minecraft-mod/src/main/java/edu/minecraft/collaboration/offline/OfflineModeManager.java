package edu.minecraft.collaboration.offline;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.util.FileSecurityUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages offline mode functionality for the collaboration system
 */
public final class OfflineModeManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static OfflineModeManager instance;
    
    // Offline mode state
    private boolean offlineModeEnabled = false;
    private boolean autoSyncEnabled = false;
    private boolean syncInProgress = false;
    
    // Data storage
    private final Queue<OfflineAction> pendingActions = new ConcurrentLinkedQueue<>();
    private final Map<String, OfflineSession> offlineSessions = new ConcurrentHashMap<>();
    private final Map<UUID, OfflineStudentData> cachedStudentData = new ConcurrentHashMap<>();
    private final AtomicInteger pendingActionsCount = new AtomicInteger(0);
    
    // Configuration
    private static final int MAX_PENDING_ACTIONS = 10000;
    private static final String OFFLINE_DATA_DIR = "offline_data";
    private LocalDateTime lastSyncTime;
    
    private OfflineModeManager() {
        initializeOfflineDirectory();
        LOGGER.info("Offline mode manager initialized");
    }
    
    public static synchronized OfflineModeManager getInstance() {
        if (instance == null) {
            instance = new OfflineModeManager();
        }
        return instance;
    }
    
    /**
     * Initialize offline data directory
     */
    private void initializeOfflineDirectory() {
        // Use secure directory creation
        if (!FileSecurityUtils.ensureSafeDirectory(OFFLINE_DATA_DIR)) {
            LOGGER.error("Failed to create secure offline data directory");
        } else {
            LOGGER.info("Offline data directory initialized");
        }
    }
    
    /**
     * Enable or disable offline mode
     */
    public void setOfflineModeEnabled(boolean enabled) {
        if (this.offlineModeEnabled != enabled) {
            this.offlineModeEnabled = enabled;
            
            if (enabled) {
                startOfflineSession();
                LOGGER.info("Offline mode enabled");
            } else {
                endOfflineSession();
                LOGGER.info("Offline mode disabled");
            }
        }
    }
    
    /**
     * Check if offline mode is enabled
     */
    public boolean isOfflineModeEnabled() {
        return offlineModeEnabled;
    }
    
    /**
     * Start offline session
     */
    private void startOfflineSession() {
        String sessionId = "session_" + System.currentTimeMillis();
        OfflineSession session = new OfflineSession(sessionId, LocalDateTime.now());
        
        offlineSessions.put(sessionId, session);
        LOGGER.info("Started offline session: {}", sessionId);
        
        // Save current state snapshot
        saveCurrentStateSnapshot(sessionId);
    }
    
    /**
     * End offline session
     */
    private void endOfflineSession() {
        if (!offlineSessions.isEmpty()) {
            String latestSessionId = offlineSessions.keySet().iterator().next();
            OfflineSession session = offlineSessions.get(latestSessionId);
            
            if (session != null) {
                session.setEndTime(LocalDateTime.now());
                saveOfflineSession(session);
                LOGGER.info("Ended offline session: {}", latestSessionId);
            }
        }
    }
    
    /**
     * Record offline action
     */
    public void recordOfflineAction(String actionType, Map<String, Object> actionData) {
        if (!offlineModeEnabled) {
            return;
        }
        
        if (pendingActionsCount.get() >= MAX_PENDING_ACTIONS) {
            LOGGER.warn("Maximum pending actions reached, dropping oldest actions");
            // Remove oldest actions
            for (int i = 0; i < 100 && !pendingActions.isEmpty(); i++) {
                pendingActions.poll();
                pendingActionsCount.decrementAndGet();
            }
        }
        
        OfflineAction action = new OfflineAction(actionType, actionData, LocalDateTime.now());
        pendingActions.offer(action);
        pendingActionsCount.incrementAndGet();
        
        // Auto-save periodically
        if (pendingActionsCount.get() % 50 == 0) {
            saveActionsToFile();
        }
        
        LOGGER.debug("Recorded offline action: {} (Total pending: {})", actionType, pendingActionsCount.get());
    }
    
    /**
     * Cache student data for offline access
     */
    public void cacheStudentData(UUID studentUUID, Map<String, Object> studentData) {
        OfflineStudentData data = cachedStudentData.computeIfAbsent(studentUUID, 
            uuid -> new OfflineStudentData(uuid));
        
        data.updateData(studentData);
        saveStudentDataCache(data);
        
        LOGGER.debug("Cached student data for: {}", studentUUID);
    }
    
    /**
     * Get cached student data
     */
    public Map<String, Object> getCachedStudentData(UUID studentUUID) {
        OfflineStudentData data = cachedStudentData.get(studentUUID);
        return data != null ? data.getData() : new HashMap<>();
    }
    
    /**
     * Synchronize pending actions
     */
    public boolean syncPendingActions() {
        if (syncInProgress || pendingActions.isEmpty()) {
            return true;
        }
        
        syncInProgress = true;
        int processedCount = 0;
        int failedCount = 0;
        
        try {
            LOGGER.info("Starting synchronization of {} pending actions", pendingActionsCount.get());
            
            List<OfflineAction> actionsToProcess = new ArrayList<>();
            OfflineAction action;
            
            // Process actions in batches
            while ((action = pendingActions.poll()) != null && actionsToProcess.size() < 100) {
                actionsToProcess.add(action);
            }
            
            for (OfflineAction offlineAction : actionsToProcess) {
                try {
                    if (processOfflineAction(offlineAction)) {
                        processedCount++;
                        pendingActionsCount.decrementAndGet();
                    } else {
                        failedCount++;
                        // Re-queue failed action
                        pendingActions.offer(offlineAction);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing offline action: {}", offlineAction.getActionType(), e);
                    failedCount++;
                    // Re-queue failed action
                    pendingActions.offer(offlineAction);
                }
            }
            
            lastSyncTime = LocalDateTime.now();
            
            LOGGER.info("Synchronization completed: {} processed, {} failed, {} remaining", 
                processedCount, failedCount, pendingActionsCount.get());
                
            return failedCount == 0;
            
        } finally {
            syncInProgress = false;
        }
    }
    
    /**
     * Process individual offline action
     */
    private boolean processOfflineAction(OfflineAction action) {
        try {
            // Here you would implement the actual processing logic
            // For now, we'll just simulate processing
            String actionType = action.getActionType();
            
            switch (actionType) {
                case "student_progress":
                    return processStudentProgressAction(action);
                case "teacher_action":
                    return processTeacherAction(action);
                case "collaboration_event":
                    return processCollaborationEvent(action);
                default:
                    LOGGER.debug("Processed generic action: {}", actionType);
                    return true;
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to process offline action: {}", action.getActionType(), e);
            return false;
        }
    }
    
    /**
     * Process student progress action
     */
    private boolean processStudentProgressAction(OfflineAction action) {
        // Implementation would integrate with ProgressTracker
        LOGGER.debug("Processing student progress action");
        return true;
    }
    
    /**
     * Process teacher action
     */
    private boolean processTeacherAction(OfflineAction action) {
        // Implementation would integrate with TeacherManager
        LOGGER.debug("Processing teacher action");
        return true;
    }
    
    /**
     * Process collaboration event
     */
    private boolean processCollaborationEvent(OfflineAction action) {
        // Implementation would integrate with CollaborationManager
        LOGGER.debug("Processing collaboration event");
        return true;
    }
    
    /**
     * Get pending actions count
     */
    public int getPendingActionsCount() {
        return pendingActionsCount.get();
    }
    
    /**
     * Set auto-sync enabled
     */
    public void setAutoSyncEnabled(boolean enabled) {
        this.autoSyncEnabled = enabled;
        LOGGER.info("Auto-sync {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Check if auto-sync is enabled
     */
    public boolean isAutoSyncEnabled() {
        return autoSyncEnabled;
    }
    
    /**
     * Export offline data
     */
    public String exportOfflineData() {
        try {
            Map<String, Object> exportData = new HashMap<>();
            
            // Export pending actions
            List<Map<String, Object>> actionsList = new ArrayList<>();
            for (OfflineAction action : pendingActions) {
                actionsList.add(action.toMap());
            }
            exportData.put("pendingActions", actionsList);
            
            // Export cached student data
            Map<String, Object> studentDataMap = new HashMap<>();
            for (Map.Entry<UUID, OfflineStudentData> entry : cachedStudentData.entrySet()) {
                studentDataMap.put(entry.getKey().toString(), entry.getValue().getData());
            }
            exportData.put("cachedStudentData", studentDataMap);
            
            // Export sessions
            Map<String, Object> sessionsMap = new HashMap<>();
            for (Map.Entry<String, OfflineSession> entry : offlineSessions.entrySet()) {
                sessionsMap.put(entry.getKey(), entry.getValue().toMap());
            }
            exportData.put("sessions", sessionsMap);
            
            // Export metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exportTime", LocalDateTime.now().toString());
            metadata.put("totalActions", pendingActionsCount.get());
            metadata.put("lastSyncTime", lastSyncTime != null ? lastSyncTime.toString() : null);
            exportData.put("metadata", metadata);
            
            String exportJson = exportData.toString(); // Simplified JSON representation
            
            // Save to file using secure method
            String fileName = "export_" + System.currentTimeMillis() + ".json";
            java.io.File exportFile = FileSecurityUtils.getSafeFile(OFFLINE_DATA_DIR, fileName);
            
            if (exportFile == null) {
                LOGGER.error("Failed to create safe export file");
                return null;
            }
            
            try (java.io.FileWriter writer = new java.io.FileWriter(exportFile)) {
                writer.write(exportJson);
            }
            
            LOGGER.info("Offline data exported to: {}", exportFile.getName());
            return exportFile.getAbsolutePath();
            
        } catch (IOException e) {
            LOGGER.error("Failed to export offline data", e);
            return null;
        }
    }
    
    /**
     * Get offline statistics
     */
    public Map<String, Object> getOfflineStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("offlineModeEnabled", offlineModeEnabled);
        stats.put("pendingActionsCount", pendingActionsCount.get());
        stats.put("cachedStudentsCount", cachedStudentData.size());
        stats.put("activeSessionsCount", offlineSessions.size());
        stats.put("lastSyncTime", lastSyncTime != null ? lastSyncTime.toString() : "Never");
        stats.put("syncInProgress", syncInProgress);
        stats.put("autoSyncEnabled", autoSyncEnabled);
        
        return stats;
    }
    
    /**
     * Save current state snapshot
     */
    private void saveCurrentStateSnapshot(String sessionId) {
        try {
            // Sanitize session ID to prevent path traversal
            String safeSessionId = FileSecurityUtils.sanitizeFileName(sessionId);
            if (safeSessionId == null) {
                LOGGER.error("Invalid session ID for snapshot");
                return;
            }
            
            String fileName = "snapshot_" + safeSessionId + ".json";
            java.io.File snapshotFile = FileSecurityUtils.getSafeFile(OFFLINE_DATA_DIR, fileName);
            
            if (snapshotFile == null) {
                LOGGER.error("Failed to create safe snapshot file");
                return;
            }
            
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("sessionId", sessionId);
            snapshot.put("timestamp", LocalDateTime.now().toString());
            snapshot.put("playerCount", 0); // Replace with actual value
            snapshot.put("worldState", "saved"); // Replace with actual world state
            
            try (java.io.FileWriter writer = new java.io.FileWriter(snapshotFile)) {
                writer.write(snapshot.toString());
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to save state snapshot", e);
        }
    }
    
    /**
     * Save offline session
     */
    private void saveOfflineSession(OfflineSession session) {
        try {
            // Sanitize session ID
            String safeSessionId = FileSecurityUtils.sanitizeFileName(session.getSessionId());
            if (safeSessionId == null) {
                LOGGER.error("Invalid session ID");
                return;
            }
            
            String fileName = "session_" + safeSessionId + ".json";
            java.io.File sessionFile = FileSecurityUtils.getSafeFile(OFFLINE_DATA_DIR, fileName);
            
            if (sessionFile == null) {
                LOGGER.error("Failed to create safe session file");
                return;
            }
            
            try (java.io.FileWriter writer = new java.io.FileWriter(sessionFile)) {
                writer.write(session.toJson());
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to save offline session", e);
        }
    }
    
    /**
     * Save student data cache
     */
    private void saveStudentDataCache(OfflineStudentData data) {
        try {
            // Sanitize UUID string
            String safeUUID = FileSecurityUtils.sanitizeFileName(data.getStudentUUID().toString());
            if (safeUUID == null) {
                LOGGER.error("Invalid student UUID");
                return;
            }
            
            String fileName = "student_" + safeUUID + ".json";
            java.io.File dataFile = FileSecurityUtils.getSafeFile(OFFLINE_DATA_DIR, fileName);
            
            if (dataFile == null) {
                LOGGER.error("Failed to create safe student data file");
                return;
            }
            
            try (java.io.FileWriter writer = new java.io.FileWriter(dataFile)) {
                writer.write(data.toJson());
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to save student data cache", e);
        }
    }
    
    /**
     * Save actions to file
     */
    private void saveActionsToFile() {
        try {
            java.io.File actionsFile = FileSecurityUtils.getSafeFile(OFFLINE_DATA_DIR, "pending_actions.json");
            
            if (actionsFile == null) {
                LOGGER.error("Failed to create safe actions file");
                return;
            }
            
            List<Map<String, Object>> actionsList = new ArrayList<>();
            for (OfflineAction action : pendingActions) {
                actionsList.add(action.toMap());
            }
            
            String actionsJson = actionsList.toString();
            
            try (java.io.FileWriter writer = new java.io.FileWriter(actionsFile)) {
                writer.write(actionsJson);
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to save actions to file", e);
        }
    }
    
    /**
     * Clear all offline data
     */
    public void clearOfflineData() {
        pendingActions.clear();
        pendingActionsCount.set(0);
        cachedStudentData.clear();
        offlineSessions.clear();
        lastSyncTime = null;
        
        LOGGER.info("Cleared all offline data");
    }
}