package edu.minecraft.collaboration.monitoring.alerts;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a system alert
 */
public class Alert {
    
    private final String id;
    private final String ruleName;
    private final AlertSeverity severity;
    private final String description;
    private final Map<String, Object> details;
    private final Instant createdAt;
    
    private volatile AlertStatus status;
    private volatile Instant lastTriggered;
    private volatile Instant acknowledgedAt;
    private volatile String acknowledgedBy;
    private volatile Instant resolvedAt;
    private volatile String resolvedBy;
    
    private Alert(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.ruleName = builder.ruleName;
        this.severity = builder.severity;
        this.description = builder.description;
        this.details = new HashMap<>(builder.details);
        this.createdAt = Instant.now();
        this.lastTriggered = this.createdAt;
        this.status = AlertStatus.ACTIVE;
    }
    
    public String getId() { return id; }
    public String getRuleName() { return ruleName; }
    public AlertSeverity getSeverity() { return severity; }
    public String getDescription() { return description; }
    public Map<String, Object> getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
    public AlertStatus getStatus() { return status; }
    public Instant getLastTriggered() { return lastTriggered; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    
    /**
     * Update the last triggered time
     */
    public void updateLastTriggered() {
        this.lastTriggered = Instant.now();
    }
    
    /**
     * Acknowledge the alert
     */
    public void acknowledge(String acknowledgedBy) {
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = Instant.now();
        this.acknowledgedBy = acknowledgedBy;
    }
    
    /**
     * Resolve the alert
     */
    public void resolve(String resolvedBy) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedBy;
    }
    
    /**
     * Get the age of the alert
     */
    public long getAgeInMinutes() {
        return java.time.Duration.between(createdAt, Instant.now()).toMinutes();
    }
    
    /**
     * Check if the alert is critical
     */
    public boolean isCritical() {
        return severity == AlertSeverity.CRITICAL;
    }
    
    /**
     * Check if the alert is active
     */
    public boolean isActive() {
        return status == AlertStatus.ACTIVE;
    }
    
    /**
     * Check if the alert is acknowledged
     */
    public boolean isAcknowledged() {
        return status == AlertStatus.ACKNOWLEDGED;
    }
    
    /**
     * Check if the alert is resolved
     */
    public boolean isResolved() {
        return status == AlertStatus.RESOLVED;
    }
    
    /**
     * Get a summary of the alert
     */
    public String getSummary() {
        return String.format("[%s] %s - %s (Age: %d min, Status: %s)",
            severity.name(), ruleName, description, getAgeInMinutes(), status.name());
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    public static class Builder {
        private String ruleName;
        private AlertSeverity severity = AlertSeverity.LOW;
        private String description = "";
        private Map<String, Object> details = new HashMap<>();
        
        public Builder ruleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }
        
        public Builder severity(AlertSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder details(Map<String, Object> details) {
            this.details = new HashMap<>(details);
            return this;
        }
        
        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }
        
        public Alert build() {
            if (ruleName == null || ruleName.trim().isEmpty()) {
                throw new IllegalArgumentException("Rule name is required");
            }
            return new Alert(this);
        }
    }
}

/**
 * Alert severity levels
 */
enum AlertSeverity {
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

/**
 * Alert status
 */
enum AlertStatus {
    ACTIVE("Active"),
    ACKNOWLEDGED("Acknowledged"),
    RESOLVED("Resolved");
    
    private final String displayName;
    
    AlertStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() { return displayName; }
}