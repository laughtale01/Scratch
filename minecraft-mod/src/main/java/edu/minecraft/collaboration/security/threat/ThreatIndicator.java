package edu.minecraft.collaboration.security.threat;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a threat indicator with severity and context
 */
public class ThreatIndicator {
    
    private final String type;
    private final ThreatSeverity severity;
    private final String description;
    private final Map<String, Object> context;
    private final Instant timestamp;
    
    public ThreatIndicator(String type, ThreatSeverity severity, String description, Map<String, Object> context) {
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.context = context != null ? new HashMap<>(context) : Collections.emptyMap();
        this.timestamp = Instant.now();
    }
    
    public static ThreatIndicator none() {
        return new ThreatIndicator("NONE", ThreatSeverity.NONE, "No threat detected", null);
    }
    
    public static ThreatIndicator low(String type, String description) {
        return new ThreatIndicator(type, ThreatSeverity.LOW, description, null);
    }
    
    public static ThreatIndicator medium(String type, String description) {
        return new ThreatIndicator(type, ThreatSeverity.MEDIUM, description, null);
    }
    
    public static ThreatIndicator high(String type, String description) {
        return new ThreatIndicator(type, ThreatSeverity.HIGH, description, null);
    }
    
    public static ThreatIndicator critical(String type, String description) {
        return new ThreatIndicator(type, ThreatSeverity.CRITICAL, description, null);
    }
    
    public String getType() { return type; }
    public ThreatSeverity getSeverity() { return severity; }
    public String getDescription() { return description; }
    public Map<String, Object> getContext() { return context; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("ThreatIndicator{type=%s, severity=%s, description='%s'}", 
            type, severity, description);
    }
}

/**
 * Threat severity levels for indicators
 */
enum ThreatSeverity {
    NONE(0),
    LOW(1),
    MEDIUM(2), 
    HIGH(3),
    CRITICAL(4);
    
    private final int level;
    
    ThreatSeverity(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isHigherThan(ThreatSeverity other) {
        return this.level > other.level;
    }
}