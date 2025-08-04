package edu.minecraft.collaboration.security.threat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User threat profile for behavioral analysis and anomaly detection
 */
public class UserThreatProfile {
    
    private final String username;
    private final Queue<ActivityRecord> activityHistory = new ConcurrentLinkedQueue<>();
    private final Map<String, AtomicInteger> resourceAccessCounts = new ConcurrentHashMap<>();
    private final Queue<Instant> failedAttempts = new ConcurrentLinkedQueue<>();
    private final AtomicInteger privilegeEscalationAttempts = new AtomicInteger(0);
    private final AtomicLong dataAccessCount = new AtomicLong(0);
    
    private volatile Instant profileCreated;
    private volatile Instant lastActivity;
    private volatile String typicalCountry = null;
    private volatile int monitoringLevel = 1; // 1=normal, 2=enhanced, 3=intensive
    private volatile boolean flaggedForReview = false;
    
    // Behavioral baselines
    private volatile double baselineHourlyFrequency = 0.0;
    private volatile double baselineActivity = 0.0;
    private final Set<LocalTime> typicalActivityTimes = ConcurrentHashMap.newKeySet();
    
    public UserThreatProfile(String username) {
        this.username = username;
        this.profileCreated = Instant.now();
        this.lastActivity = Instant.now();
    }
    
    /**
     * Record user activity
     */
    public void recordActivity(UserActivityEvent event) {
        ActivityRecord record = new ActivityRecord(
            event.getActivity(),
            event.getResourceAccessed(),
            event.isSuccessful(),
            event.getTimestamp(),
            event.getNetworkInfo()
        );
        
        activityHistory.offer(record);
        lastActivity = event.getTimestamp();
        
        // Update resource access patterns
        if (event.getResourceAccessed() != null) {
            resourceAccessCounts.computeIfAbsent(event.getResourceAccessed(), k -> new AtomicInteger(0))
                              .incrementAndGet();
        }
        
        // Update typical activity times
        LocalTime activityTime = event.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toLocalTime();
        typicalActivityTimes.add(activityTime);
        
        // Record failed attempts
        if (!event.isSuccessful()) {
            failedAttempts.offer(event.getTimestamp());
        }
        
        // Update network info
        if (event.getNetworkInfo() != null && typicalCountry == null) {
            typicalCountry = event.getNetworkInfo().getCountry();
        }
        
        // Clean up old data periodically
        if (activityHistory.size() % 100 == 0) {
            cleanupOldData();
        }
        
        // Update baselines
        updateBaselines();
    }
    
    /**
     * Analyze behavioral patterns
     */
    public void analyzePatterns() {
        // Analyze timing patterns
        analyzeTimingPatterns();
        
        // Analyze resource access patterns
        analyzeResourcePatterns();
        
        // Update risk indicators
        updateRiskIndicators();
    }
    
    /**
     * Clean up old data to prevent memory leaks
     */
    public void cleanupOldData() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));
        
        // Clean activity history
        activityHistory.removeIf(record -> record.timestamp.isBefore(cutoff));
        
        // Clean failed attempts
        failedAttempts.removeIf(attempt -> attempt.isBefore(cutoff));
        
        // Clean typical activity times if too many
        if (typicalActivityTimes.size() > 100) {
            // Keep a representative sample
            List<LocalTime> timesList = new ArrayList<>(typicalActivityTimes);
            Collections.shuffle(timesList);
            typicalActivityTimes.clear();
            typicalActivityTimes.addAll(timesList.subList(0, 50));
        }
    }
    
    private void updateBaselines() {
        List<ActivityRecord> recentActivities = getRecentActivities(Duration.ofDays(1));
        
        if (recentActivities.size() >= 10) {
            // Update hourly frequency baseline
            baselineHourlyFrequency = recentActivities.size() / 24.0;
            
            // Update general activity baseline
            baselineActivity = recentActivities.size();
        }
    }
    
    private void analyzeTimingPatterns() {
        List<ActivityRecord> recentActivities = getRecentActivities(Duration.ofHours(1));
        
        if (recentActivities.size() > 20) {
            // Very high activity - potential bot behavior
            flaggedForReview = true;
        }
    }
    
    private void analyzeResourcePatterns() {
        // Check for unusual resource access
        int totalAccess = resourceAccessCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        
        if (totalAccess > 1000) {
            // High volume access - potential data harvesting
            flaggedForReview = true;
        }
    }
    
    private void updateRiskIndicators() {
        // Update data access count
        long currentDataAccess = activityHistory.stream()
            .filter(record -> isDataAccessActivity(record.activity))
            .count();
        dataAccessCount.set(currentDataAccess);
    }
    
    private boolean isDataAccessActivity(String activity) {
        return activity.contains("read") || activity.contains("export") || 
               activity.contains("download") || activity.contains("access");
    }
    
    private List<ActivityRecord> getRecentActivities(Duration duration) {
        Instant cutoff = Instant.now().minus(duration);
        return activityHistory.stream()
            .filter(record -> record.timestamp.isAfter(cutoff))
            .toList();
    }
    
    // Getter methods for threat analysis
    public String getUsername() { return username; }
    
    public List<LocalTime> getTypicalActivityTimes() {
        return new ArrayList<>(typicalActivityTimes);
    }
    
    public int getRecentActivityCount(Duration duration) {
        return getRecentActivities(duration).size();
    }
    
    public double getBaselineHourlyFrequency() {
        return baselineHourlyFrequency;
    }
    
    public Map<String, Integer> getResourceAccessPattern() {
        Map<String, Integer> pattern = new HashMap<>();
        resourceAccessCounts.forEach((resource, count) -> pattern.put(resource, count.get()));
        return pattern;
    }
    
    public int getRecentFailedAttempts() {
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        return (int) failedAttempts.stream()
            .filter(attempt -> attempt.isAfter(oneHourAgo))
            .count();
    }
    
    public Instant getFirstFailedAttempt() {
        return failedAttempts.isEmpty() ? Instant.now() : failedAttempts.peek();
    }
    
    public boolean hasRecentPrivilegeAttempts() {
        return privilegeEscalationAttempts.get() > 0;
    }
    
    public int getPrivilegeEscalationAttempts() {
        return privilegeEscalationAttempts.get();
    }
    
    public boolean hasUnusualActivity() {
        return flaggedForReview || getRecentActivityCount(Duration.ofHours(1)) > baselineHourlyFrequency * 3;
    }
    
    public double getBaselineVolumePerWindow(Duration window) {
        // Calculate average activity for the given window
        return baselineActivity * (window.toHours() / 24.0);
    }
    
    public String getTypicalCountry() {
        return typicalCountry;
    }
    
    public boolean hasAccessedSensitiveResources() {
        return resourceAccessCounts.keySet().stream()
            .anyMatch(resource -> resource.contains("admin") || resource.contains("config"));
    }
    
    public boolean hasUnusualDataAccessPattern() {
        return dataAccessCount.get() > 50; // Simplified threshold
    }
    
    public boolean hasOffHoursActivity() {
        return typicalActivityTimes.stream()
            .anyMatch(time -> time.isBefore(LocalTime.of(6, 0)) || time.isAfter(LocalTime.of(22, 0)));
    }
    
    public double getActivityVolume() {
        return activityHistory.size();
    }
    
    public double getBaselineActivity() {
        return baselineActivity;
    }
    
    public boolean hasRobotTimingPattern() {
        List<ActivityRecord> recent = getRecentActivities(Duration.ofMinutes(10));
        if (recent.size() < 5) return false;
        
        // Check for very regular timing intervals
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < recent.size(); i++) {
            long interval = Duration.between(recent.get(i-1).timestamp, recent.get(i).timestamp).toMillis();
            intervals.add(interval);
        }
        
        // Check if all intervals are very similar (within 100ms)
        if (intervals.size() < 2) return false;
        long avgInterval = intervals.stream().mapToLong(Long::longValue).sum() / intervals.size();
        return intervals.stream().allMatch(interval -> Math.abs(interval - avgInterval) < 100);
    }
    
    public boolean hasIdenticalRequestPattern() {
        List<ActivityRecord> recent = getRecentActivities(Duration.ofMinutes(5));
        if (recent.size() < 5) return false;
        
        // Check for identical consecutive requests
        Map<String, Integer> activityCounts = new HashMap<>();
        for (ActivityRecord record : recent) {
            activityCounts.merge(record.activity, 1, Integer::sum);
        }
        
        // If any activity appears more than 5 times in 5 minutes, it's likely automated
        return activityCounts.values().stream().anyMatch(count -> count > 5);
    }
    
    public Duration getCurrentSessionDuration() {
        return Duration.between(profileCreated, lastActivity);
    }
    
    public boolean hasMultipleActiveSessions() {
        // This would require integration with session management
        // For now, return false as a placeholder
        return false;
    }
    
    public boolean hasSessionJumpingPattern() {
        // Check for rapid session changes - placeholder implementation
        return false;
    }
    
    public List<Instant> getRecentActivityTimestamps(Duration duration) {
        return getRecentActivities(duration).stream()
            .map(record -> record.timestamp)
            .toList();
    }
    
    public int getRecentDataAccessCount() {
        return (int) dataAccessCount.get();
    }
    
    public void increaseMonitoringLevel() {
        monitoringLevel = Math.min(3, monitoringLevel + 1);
    }
    
    public int getMonitoringLevel() {
        return monitoringLevel;
    }
    
    /**
     * Activity record for storing user activities
     */
    private static class ActivityRecord {
        final String activity;
        final String resource;
        final boolean successful;
        final Instant timestamp;
        final NetworkInfo networkInfo;
        
        ActivityRecord(String activity, String resource, boolean successful, 
                      Instant timestamp, NetworkInfo networkInfo) {
            this.activity = activity;
            this.resource = resource;
            this.successful = successful;
            this.timestamp = timestamp;
            this.networkInfo = networkInfo;
        }
    }
}