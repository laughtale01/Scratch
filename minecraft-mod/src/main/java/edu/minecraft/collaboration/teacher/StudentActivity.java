package edu.minecraft.collaboration.teacher;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks individual student activities in the Minecraft collaboration system
 */
public class StudentActivity {
    private final UUID playerUUID;
    private final LocalDateTime sessionStart;
    private final Deque<ActivityLog> activityLogs = new ConcurrentLinkedDeque<>();
    private final Map<String, AtomicInteger> activityCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastActivityTime = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicInteger totalBlocks = new AtomicInteger(0);
    private final AtomicInteger totalCommands = new AtomicInteger(0);
    private final AtomicInteger totalMessages = new AtomicInteger(0);
    private final AtomicInteger totalVisits = new AtomicInteger(0);
    private final AtomicInteger emergencyReturns = new AtomicInteger(0);
    
    // Configuration
    private static final int MAX_ACTIVITY_LOGS = 1000;
    private static final int MAX_ACTIVITY_AGE_MINUTES = 60;
    
    public StudentActivity(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.sessionStart = LocalDateTime.now();
    }
    
    /**
     * Get player name from UUID (simplified implementation)
     */
    public String getPlayerName() {
        // In a real implementation, this would look up the player name from the server
        // For now, return a simplified name based on UUID
        return "Student_" + playerUUID.toString().substring(0, 8);
    }
    
    /**
     * Add a new activity
     */
    public void addActivity(String activity, String details) {
        LocalDateTime now = LocalDateTime.now();
        
        // Create activity log
        ActivityLog log = new ActivityLog(activity, details, now);
        activityLogs.addFirst(log);
        
        // Update counts
        activityCounts.computeIfAbsent(activity, k -> new AtomicInteger(0)).incrementAndGet();
        lastActivityTime.put(activity, now);
        
        // Update statistics based on activity type
        updateStatistics(activity, details);
        
        // Clean old logs
        cleanOldLogs();
    }
    
    /**
     * Update statistics based on activity type
     */
    private void updateStatistics(String activity, String details) {
        switch (activity.toLowerCase()) {
            case "place_block":
            case "break_block":
                totalBlocks.incrementAndGet();
                break;
            case "command":
                totalCommands.incrementAndGet();
                break;
            case "chat":
                totalMessages.incrementAndGet();
                break;
            case "visit_request":
            case "visit_approved":
                totalVisits.incrementAndGet();
                break;
            case "emergency_return":
                emergencyReturns.incrementAndGet();
                break;
        }
    }
    
    /**
     * Clean old activity logs
     */
    private void cleanOldLogs() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(MAX_ACTIVITY_AGE_MINUTES);
        
        // Remove old logs
        while (activityLogs.size() > MAX_ACTIVITY_LOGS) {
            activityLogs.removeLast();
        }
        
        // Remove logs older than cutoff time
        activityLogs.removeIf(log -> log.timestamp.isBefore(cutoffTime));
    }
    
    /**
     * Get recent activities
     */
    public List<String> getRecentActivities(int count) {
        List<String> recent = new ArrayList<>();
        int i = 0;
        
        for (ActivityLog log : activityLogs) {
            if (i++ >= count) break;
            recent.add(String.format("[%s] %s: %s", 
                log.timestamp.toLocalTime(), 
                log.activity, 
                log.details));
        }
        
        return recent;
    }
    
    /**
     * Get activity count for specific type
     */
    public int getActivityCount(String activity) {
        AtomicInteger count = activityCounts.get(activity);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Get total number of actions
     */
    public int getTotalActions() {
        return activityCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
    }
    
    /**
     * Get session duration in minutes
     */
    public long getSessionDuration() {
        return Duration.between(sessionStart, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * Get last activity time for specific type
     */
    public LocalDateTime getLastActivityTime(String activity) {
        return lastActivityTime.get(activity);
    }
    
    /**
     * Get activity summary
     */
    public ActivitySummary getSummary() {
        return new ActivitySummary(
            playerUUID,
            sessionStart,
            getTotalActions(),
            totalBlocks.get(),
            totalCommands.get(),
            totalMessages.get(),
            totalVisits.get(),
            emergencyReturns.get(),
            getSessionDuration(),
            getMostFrequentActivity()
        );
    }
    
    /**
     * Get most frequent activity
     */
    private String getMostFrequentActivity() {
        return activityCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue(Comparator.comparing(AtomicInteger::get)))
            .map(Map.Entry::getKey)
            .orElse("none");
    }
    
    /**
     * Check if student is currently active
     */
    public boolean isActive(int inactiveThresholdMinutes) {
        if (activityLogs.isEmpty()) {
            return false;
        }
        
        ActivityLog lastLog = activityLogs.peekFirst();
        Duration timeSinceLastActivity = Duration.between(lastLog.timestamp, LocalDateTime.now());
        return timeSinceLastActivity.toMinutes() < inactiveThresholdMinutes;
    }
    
    /**
     * Export activity data for reporting
     */
    public Map<String, Object> exportData() {
        Map<String, Object> data = new HashMap<>();
        data.put("playerUUID", playerUUID.toString());
        data.put("sessionStart", sessionStart.toString());
        data.put("sessionDurationMinutes", getSessionDuration());
        data.put("totalActions", getTotalActions());
        data.put("statistics", Map.of(
            "blocks", totalBlocks.get(),
            "commands", totalCommands.get(),
            "messages", totalMessages.get(),
            "visits", totalVisits.get(),
            "emergencyReturns", emergencyReturns.get()
        ));
        data.put("activityCounts", new HashMap<>(activityCounts));
        data.put("recentActivities", getRecentActivities(10));
        data.put("isActive", isActive(5));
        
        return data;
    }
    
    /**
     * Activity log entry
     */
    private static class ActivityLog {
        final String activity;
        final String details;
        final LocalDateTime timestamp;
        
        ActivityLog(String activity, String details, LocalDateTime timestamp) {
            this.activity = activity;
            this.details = details;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Activity summary for reporting
     */
    public static class ActivitySummary {
        public final UUID playerUUID;
        public final LocalDateTime sessionStart;
        public final int totalActions;
        public final int totalBlocks;
        public final int totalCommands;
        public final int totalMessages;
        public final int totalVisits;
        public final int emergencyReturns;
        public final long sessionMinutes;
        public final String mostFrequentActivity;
        
        public ActivitySummary(UUID playerUUID, LocalDateTime sessionStart, 
                             int totalActions, int totalBlocks, int totalCommands,
                             int totalMessages, int totalVisits, int emergencyReturns,
                             long sessionMinutes, String mostFrequentActivity) {
            this.playerUUID = playerUUID;
            this.sessionStart = sessionStart;
            this.totalActions = totalActions;
            this.totalBlocks = totalBlocks;
            this.totalCommands = totalCommands;
            this.totalMessages = totalMessages;
            this.totalVisits = totalVisits;
            this.emergencyReturns = emergencyReturns;
            this.sessionMinutes = sessionMinutes;
            this.mostFrequentActivity = mostFrequentActivity;
        }
        
        @Override
        public String toString() {
            return String.format(
                "StudentActivity[player=%s, duration=%dm, actions=%d, blocks=%d, commands=%d, messages=%d, visits=%d, emergencies=%d, mostFrequent=%s]",
                playerUUID, sessionMinutes, totalActions, totalBlocks, totalCommands, 
                totalMessages, totalVisits, emergencyReturns, mostFrequentActivity
            );
        }
    }
}