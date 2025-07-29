package com.yourname.minecraftcollaboration.progress;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks individual student progress and statistics
 */
public class StudentProgress {
    private final UUID studentUUID;
    private final LocalDateTime sessionStart;
    private final AtomicInteger totalPoints = new AtomicInteger(0);
    
    // Activity tracking
    private final Map<String, AtomicInteger> activityCounts = new ConcurrentHashMap<>();
    private final List<ActivityRecord> activityHistory = new ArrayList<>();
    private final Set<String> uniqueActivities = new HashSet<>();
    
    // Achievements and milestones
    private final Set<String> completedMilestones = new HashSet<>();
    private final List<Achievement> earnedAchievements = new ArrayList<>();
    
    // Statistics
    private final AtomicInteger totalBlocks = new AtomicInteger(0);
    private final AtomicInteger totalCommands = new AtomicInteger(0);
    private final AtomicInteger collaborationCount = new AtomicInteger(0);
    
    // Learning metrics
    private LocalDateTime lastActivityTime;
    private int currentLevel = 1;
    private final Map<String, Integer> skillLevels = new ConcurrentHashMap<>();
    
    public StudentProgress(UUID studentUUID) {
        this.studentUUID = studentUUID;
        this.sessionStart = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
        
        // Initialize skill levels
        skillLevels.put("building", 0);
        skillLevels.put("programming", 0);
        skillLevels.put("collaboration", 0);
        skillLevels.put("creativity", 0);
    }
    
    /**
     * Record a new activity
     */
    public synchronized void recordActivity(String activity, String details) {
        LocalDateTime now = LocalDateTime.now();
        
        // Update counts
        activityCounts.computeIfAbsent(activity, k -> new AtomicInteger(0)).incrementAndGet();
        uniqueActivities.add(activity);
        
        // Add to history
        activityHistory.add(new ActivityRecord(activity, details, now));
        
        // Update statistics
        updateStatistics(activity);
        
        // Update skill levels
        updateSkillLevels(activity);
        
        // Update last activity time
        lastActivityTime = now;
        
        // Clean old history (keep last 1000 entries)
        if (activityHistory.size() > 1000) {
            activityHistory.remove(0);
        }
    }
    
    /**
     * Update statistics based on activity
     */
    private void updateStatistics(String activity) {
        switch (activity.toLowerCase()) {
            case "place_block":
            case "break_block":
            case "build_circle":
            case "build_sphere":
            case "build_wall":
            case "build_house":
                totalBlocks.incrementAndGet();
                break;
            case "command":
            case "agent_action":
            case "teleport":
                totalCommands.incrementAndGet();
                break;
            case "invite_friend":
            case "visit_request":
            case "visit_approved":
                collaborationCount.incrementAndGet();
                break;
        }
    }
    
    /**
     * Update skill levels based on activity
     */
    private void updateSkillLevels(String activity) {
        String skill = null;
        
        switch (activity.toLowerCase()) {
            case "place_block":
            case "break_block":
            case "build_circle":
            case "build_sphere":
            case "build_wall":
            case "build_house":
                skill = "building";
                break;
            case "command":
            case "agent_action":
                skill = "programming";
                break;
            case "invite_friend":
            case "visit_request":
            case "visit_approved":
                skill = "collaboration";
                break;
            case "summon_agent":
            case "agent_follow":
                skill = "creativity";
                break;
        }
        
        if (skill != null) {
            skillLevels.put(skill, skillLevels.get(skill) + 1);
        }
    }
    
    /**
     * Add points and update level
     */
    public void addPoints(int points) {
        int newTotal = totalPoints.addAndGet(points);
        
        // Calculate new level (every 100 points = 1 level)
        int newLevel = (newTotal / 100) + 1;
        if (newLevel > currentLevel) {
            currentLevel = newLevel;
        }
    }
    
    /**
     * Award achievement
     */
    public void awardAchievement(Achievement achievement) {
        if (!earnedAchievements.contains(achievement)) {
            earnedAchievements.add(achievement);
        }
    }
    
    /**
     * Complete milestone
     */
    public void completeMilestone(String milestoneId) {
        completedMilestones.add(milestoneId);
    }
    
    /**
     * Check if milestone is completed
     */
    public boolean isMilestoneCompleted(String milestoneId) {
        return completedMilestones.contains(milestoneId);
    }
    
    /**
     * Get activity count for specific activity
     */
    public int getActivityCount(String activity) {
        AtomicInteger count = activityCounts.get(activity);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Get recent activities
     */
    public List<ActivityRecord> getRecentActivities(int count) {
        int size = activityHistory.size();
        int start = Math.max(0, size - count);
        return new ArrayList<>(activityHistory.subList(start, size));
    }
    
    /**
     * Get session duration in minutes
     */
    public long getSessionMinutes() {
        return Duration.between(sessionStart, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * Check if student is currently active
     */
    public boolean isActive(int inactiveThresholdMinutes) {
        Duration timeSinceLastActivity = Duration.between(lastActivityTime, LocalDateTime.now());
        return timeSinceLastActivity.toMinutes() < inactiveThresholdMinutes;
    }
    
    /**
     * Get learning progress summary
     */
    public ProgressSummary getSummary() {
        return new ProgressSummary(
            studentUUID,
            totalPoints.get(),
            currentLevel,
            getSessionMinutes(),
            totalBlocks.get(),
            totalCommands.get(),
            collaborationCount.get(),
            earnedAchievements.size(),
            completedMilestones.size(),
            isActive(5),
            new HashMap<>(skillLevels)
        );
    }
    
    /**
     * Export progress data
     */
    public Map<String, Object> exportData() {
        Map<String, Object> data = new HashMap<>();
        data.put("studentUUID", studentUUID.toString());
        data.put("sessionStart", sessionStart.toString());
        data.put("totalPoints", totalPoints.get());
        data.put("currentLevel", currentLevel);
        data.put("sessionMinutes", getSessionMinutes());
        data.put("activityCounts", new HashMap<>(activityCounts));
        data.put("skillLevels", new HashMap<>(skillLevels));
        data.put("completedMilestones", new HashSet<>(completedMilestones));
        data.put("earnedAchievements", earnedAchievements.size());
        data.put("isActive", isActive(5));
        
        return data;
    }
    
    // Getters
    public UUID getStudentUUID() { return studentUUID; }
    public LocalDateTime getSessionStart() { return sessionStart; }
    public int getTotalPoints() { return totalPoints.get(); }
    public int getLevel() { return currentLevel; }
    public int getTotalBlocks() { return totalBlocks.get(); }
    public int getTotalCommands() { return totalCommands.get(); }
    public int getCollaborationCount() { return collaborationCount.get(); }
    public Set<String> getUniqueActivities() { return new HashSet<>(uniqueActivities); }
    public Set<String> getCompletedMilestones() { return new HashSet<>(completedMilestones); }
    public List<Achievement> getEarnedAchievements() { return new ArrayList<>(earnedAchievements); }
    public Map<String, AtomicInteger> getActivityCounts() { return new HashMap<>(activityCounts); }
    public Map<String, Integer> getSkillLevels() { return new HashMap<>(skillLevels); }
    public LocalDateTime getLastActivityTime() { return lastActivityTime; }
    
    /**
     * Activity record for history tracking
     */
    public static class ActivityRecord {
        public final String activity;
        public final String details;
        public final LocalDateTime timestamp;
        
        public ActivityRecord(String activity, String details, LocalDateTime timestamp) {
            this.activity = activity;
            this.details = details;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s", 
                timestamp.toLocalTime(), activity, details);
        }
    }
    
    /**
     * Progress summary for reporting
     */
    public static class ProgressSummary {
        public final UUID studentUUID;
        public final int totalPoints;
        public final int level;
        public final long sessionMinutes;
        public final int totalBlocks;
        public final int totalCommands;
        public final int collaborationCount;
        public final int achievementCount;
        public final int milestoneCount;
        public final boolean isActive;
        public final Map<String, Integer> skillLevels;
        
        public ProgressSummary(UUID studentUUID, int totalPoints, int level, 
                             long sessionMinutes, int totalBlocks, int totalCommands,
                             int collaborationCount, int achievementCount, 
                             int milestoneCount, boolean isActive,
                             Map<String, Integer> skillLevels) {
            this.studentUUID = studentUUID;
            this.totalPoints = totalPoints;
            this.level = level;
            this.sessionMinutes = sessionMinutes;
            this.totalBlocks = totalBlocks;
            this.totalCommands = totalCommands;
            this.collaborationCount = collaborationCount;
            this.achievementCount = achievementCount;
            this.milestoneCount = milestoneCount;
            this.isActive = isActive;
            this.skillLevels = new HashMap<>(skillLevels);
        }
        
        @Override
        public String toString() {
            return String.format(
                "ProgressSummary[student=%s, points=%d, level=%d, time=%dm, blocks=%d, commands=%d, collab=%d, achievements=%d, milestones=%d, active=%s]",
                studentUUID, totalPoints, level, sessionMinutes, totalBlocks, 
                totalCommands, collaborationCount, achievementCount, milestoneCount, isActive
            );
        }
    }
}