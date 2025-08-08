package edu.minecraft.collaboration.monitoring.alerts;

/**
 * Alert severity levels
 */
public enum AlertSeverity {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    WARNING(4, "Warning"),
    CRITICAL(5, "Critical");
    
    private final int level;
    private final String displayName;
    
    AlertSeverity(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
    
    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }
    
    public boolean isHigherThan(AlertSeverity other) {
        return this.level > other.level;
    }
}