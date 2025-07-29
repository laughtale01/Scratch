package com.yourname.minecraftcollaboration.progress;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a learning milestone that measures educational progress
 */
public class LearningMilestone {
    private final String id;
    private final String name;
    private final String description;
    private final List<String> requiredActivities;
    private final int requiredCount;
    private final int pointReward;
    private final LocalDateTime createdAt;
    
    public LearningMilestone(String id, String name, String description, 
                           List<String> requiredActivities, int requiredCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredActivities = requiredActivities;
        this.requiredCount = requiredCount;
        this.pointReward = requiredCount * 10; // Base point calculation
        this.createdAt = LocalDateTime.now();
    }
    
    public LearningMilestone(String id, String name, String description, 
                           List<String> requiredActivities, int requiredCount, int pointReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredActivities = requiredActivities;
        this.requiredCount = requiredCount;
        this.pointReward = pointReward;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getRequiredActivities() { return requiredActivities; }
    public int getRequiredCount() { return requiredCount; }
    public int getPointReward() { return pointReward; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    /**
     * Check if the milestone requirements are met
     */
    public boolean isCompletedBy(java.util.Map<String, Integer> activityCounts) {
        for (String activity : requiredActivities) {
            int count = activityCounts.getOrDefault(activity, 0);
            if (count < requiredCount) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get completion percentage
     */
    public double getCompletionPercentage(java.util.Map<String, Integer> activityCounts) {
        int totalRequired = requiredActivities.size() * requiredCount;
        int totalCompleted = 0;
        
        for (String activity : requiredActivities) {
            int count = activityCounts.getOrDefault(activity, 0);
            totalCompleted += Math.min(count, requiredCount);
        }
        
        return totalRequired > 0 ? (double) totalCompleted / totalRequired : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("LearningMilestone[id=%s, name=%s, activities=%s, count=%d, points=%d]",
            id, name, requiredActivities, requiredCount, pointReward);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LearningMilestone that = (LearningMilestone) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}