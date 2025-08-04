package edu.minecraft.collaboration.teacher;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Deque;
import java.util.Comparator;
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
    private final AtomicInteger totalActions = new AtomicInteger(0);
    
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
            default:
                // Unknown activity type, count as general action
                totalActions.incrementAndGet();
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
            if (i++ >= count) {
                break;
            }
            recent.add(String.format("[%s] %s: %s", 
                log.getTimestamp().toLocalTime(), 
                log.getActivity(), 
                log.getDetails()));
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
        return new ActivitySummary.Builder(playerUUID, sessionStart)
            .totalActions(getTotalActions())
            .totalBlocks(totalBlocks.get())
            .totalCommands(totalCommands.get())
            .totalMessages(totalMessages.get())
            .totalVisits(totalVisits.get())
            .emergencyReturns(emergencyReturns.get())
            .sessionMinutes(getSessionDuration())
            .mostFrequentActivity(getMostFrequentActivity())
            .build();
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
        private final String activity;
        private final String details;
        private final LocalDateTime timestamp;
        
        public String getActivity() {
            return activity;
        }
        public String getDetails() {
            return details;
        }
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
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
        private final UUID playerUUID;
        private final LocalDateTime sessionStart;
        private final int totalActions;
        private final int totalBlocks;
        private final int totalCommands;
        private final int totalMessages;
        private final int totalVisits;
        private final int emergencyReturns;
        private final long sessionMinutes;
        private final String mostFrequentActivity;
        
        public UUID getPlayerUUID() {
            return playerUUID;
        }
        public LocalDateTime getSessionStart() {
            return sessionStart;
        }
        public int getTotalActions() {
            return totalActions;
        }
        public int getTotalBlocks() {
            return totalBlocks;
        }
        public int getTotalCommands() {
            return totalCommands;
        }
        public int getTotalMessages() {
            return totalMessages;
        }
        public int getTotalVisits() {
            return totalVisits;
        }
        public int getEmergencyReturns() {
            return emergencyReturns;
        }
        public long getSessionMinutes() {
            return sessionMinutes;
        }
        public String getMostFrequentActivity() {
            return mostFrequentActivity;
        }
        
        // Private constructor for builder pattern
        private ActivitySummary(Builder builder) {
            this.playerUUID = builder.playerUUID;
            this.sessionStart = builder.sessionStart;
            this.totalActions = builder.totalActions;
            this.totalBlocks = builder.totalBlocks;
            this.totalCommands = builder.totalCommands;
            this.totalMessages = builder.totalMessages;
            this.totalVisits = builder.totalVisits;
            this.emergencyReturns = builder.emergencyReturns;
            this.sessionMinutes = builder.sessionMinutes;
            this.mostFrequentActivity = builder.mostFrequentActivity;
        }
        
        // Builder class
        public static class Builder {
            private final UUID playerUUID;
            private final LocalDateTime sessionStart;
            private int totalActions;
            private int totalBlocks;
            private int totalCommands;
            private int totalMessages;
            private int totalVisits;
            private int emergencyReturns;
            private long sessionMinutes;
            private String mostFrequentActivity;
            
            public Builder(UUID playerUUID, LocalDateTime sessionStart) {
                this.playerUUID = playerUUID;
                this.sessionStart = sessionStart;
            }
            
            public Builder totalActions(int totalActions) {
                this.totalActions = totalActions;
                return this;
            }
            
            public Builder totalBlocks(int totalBlocks) {
                this.totalBlocks = totalBlocks;
                return this;
            }
            
            public Builder totalCommands(int totalCommands) {
                this.totalCommands = totalCommands;
                return this;
            }
            
            public Builder totalMessages(int totalMessages) {
                this.totalMessages = totalMessages;
                return this;
            }
            
            public Builder totalVisits(int totalVisits) {
                this.totalVisits = totalVisits;
                return this;
            }
            
            public Builder emergencyReturns(int emergencyReturns) {
                this.emergencyReturns = emergencyReturns;
                return this;
            }
            
            public Builder sessionMinutes(long sessionMinutes) {
                this.sessionMinutes = sessionMinutes;
                return this;
            }
            
            public Builder mostFrequentActivity(String mostFrequentActivity) {
                this.mostFrequentActivity = mostFrequentActivity;
                return this;
            }
            
            public ActivitySummary build() {
                return new ActivitySummary(this);
            }
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