package edu.minecraft.collaboration.security.threat;

/**
 * Represents the severity level of detected threats
 */
public enum ThreatLevel {
    NONE(0, "No threat detected"),
    LOW(1, "Low risk threat"),
    MEDIUM(2, "Medium risk threat"),
    HIGH(3, "High risk threat"),
    CRITICAL(4, "Critical threat - immediate action required");
    
    private final int severity;
    private final String description;
    
    ThreatLevel(int severity, String description) {
        this.severity = severity;
        this.description = description;
    }
    
    public int getSeverity() { return severity; }
    public String getDescription() { return description; }
    
    /**
     * Check if this threat level is higher than another
     */
    public boolean isHigherThan(ThreatLevel other) {
        return this.severity > other.severity;
    }
    
    /**
     * Check if this threat level is lower than another
     */
    public boolean isLowerThan(ThreatLevel other) {
        return this.severity < other.severity;
    }
    
    /**
     * Get the maximum threat level from two levels
     */
    public static ThreatLevel max(ThreatLevel level1, ThreatLevel level2) {
        return level1.severity >= level2.severity ? level1 : level2;
    }
    
    /**
     * Get threat level from severity score (0-100)
     */
    public static ThreatLevel fromScore(double score) {
        if (score >= 80) return CRITICAL;
        if (score >= 60) return HIGH;
        if (score >= 30) return MEDIUM;
        if (score > 0) return LOW;
        return NONE;
    }
}