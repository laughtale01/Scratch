package edu.minecraft.collaboration.security.threat;

import java.time.Instant;
import java.util.*;

/**
 * Threat Assessment result containing detailed threat analysis
 */
public class ThreatAssessment {
    
    private final ThreatLevel threatLevel;
    private final String threatType;
    private final double confidence;
    private final String description;
    private final List<String> mitigationActions;
    private final Map<String, Object> metadata;
    private final Instant timestamp;
    private final boolean isError;
    
    private ThreatAssessment(Builder builder) {
        this.threatLevel = builder.threatLevel;
        this.threatType = builder.threatType;
        this.confidence = builder.confidence;
        this.description = builder.description;
        this.mitigationActions = new ArrayList<>(builder.mitigationActions);
        this.metadata = new HashMap<>(builder.metadata);
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.isError = builder.isError;
    }
    
    public ThreatLevel getThreatLevel() { return threatLevel; }
    public String getThreatType() { return threatType; }
    public double getConfidence() { return confidence; }
    public String getDescription() { return description; }
    public List<String> getMitigationActions() { return mitigationActions; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getTimestamp() { return timestamp; }
    public boolean isError() { return isError; }
    
    public static ThreatAssessment noThreat(String reason) {
        return builder()
            .threatLevel(ThreatLevel.NONE)
            .threatType("NO_THREAT")
            .confidence(1.0)
            .description(reason)
            .build();
    }
    
    public static ThreatAssessment error(String errorMessage) {
        return builder()
            .threatLevel(ThreatLevel.NONE)
            .threatType("ERROR")
            .confidence(0.0)
            .description(errorMessage)
            .isError(true)
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return String.format("ThreatAssessment{level=%s, type=%s, confidence=%.2f, description='%s'}", 
            threatLevel, threatType, confidence, description);
    }
    
    public static class Builder {
        private ThreatLevel threatLevel = ThreatLevel.NONE;
        private String threatType = "UNKNOWN";
        private double confidence = 0.0;
        private String description = "";
        private List<String> mitigationActions = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();
        private Instant timestamp;
        private boolean isError = false;
        
        public Builder threatLevel(ThreatLevel threatLevel) {
            this.threatLevel = threatLevel;
            return this;
        }
        
        public Builder threatType(String threatType) {
            this.threatType = threatType;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder addMitigationAction(String action) {
            this.mitigationActions.add(action);
            return this;
        }
        
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder isError(boolean isError) {
            this.isError = isError;
            return this;
        }
        
        public ThreatAssessment build() {
            return new ThreatAssessment(this);
        }
    }
}

/**
 * Threat severity levels
 */
enum ThreatLevel {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);
    
    private final int severity;
    
    ThreatLevel(int severity) {
        this.severity = severity;
    }
    
    public int getSeverity() {
        return severity;
    }
    
    public boolean isHigherThan(ThreatLevel other) {
        return this.severity > other.severity;
    }
}