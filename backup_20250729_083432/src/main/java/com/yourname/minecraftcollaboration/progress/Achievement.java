package com.yourname.minecraftcollaboration.progress;

import java.time.LocalDateTime;

/**
 * Represents an achievement that students can earn
 */
public class Achievement {
    private final String id;
    private final String name;
    private final String description;
    private final AchievementType type;
    private final int requirement;
    private final int pointReward;
    private final LocalDateTime createdAt;
    
    public Achievement(String id, String name, String description, 
                     AchievementType type, int requirement, int pointReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.requirement = requirement;
        this.pointReward = pointReward;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public AchievementType getType() { return type; }
    public int getRequirement() { return requirement; }
    public int getPointReward() { return pointReward; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    @Override
    public String toString() {
        return String.format("Achievement[id=%s, name=%s, type=%s, req=%d, points=%d]",
            id, name, type, requirement, pointReward);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Achievement that = (Achievement) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}