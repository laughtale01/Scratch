package edu.minecraft.collaboration.security.threat;

import java.time.Instant;

/**
 * Represents a threat event in the system
 */
public class ThreatEvent {

    private final String username;
    private final String eventType;
    private final Instant timestamp;
    private final double riskScore;
    private final String description;

    public ThreatEvent(String username, String eventType, Instant timestamp, double riskScore) {
        this.username = username;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.riskScore = riskScore;
        this.description = String.format("%s event for user %s with risk score %.1f",
            eventType, username, riskScore);
    }

    public ThreatEvent(String username, String eventType, Instant timestamp, double riskScore, String description) {
        this.username = username;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.riskScore = riskScore;
        this.description = description;
    }

    public String getUsername() { return username; }
    public String getEventType() { return eventType; }
    public Instant getTimestamp() { return timestamp; }
    public Instant getDetectionTime() { return timestamp; } // Alias for getTimestamp
    public double getRiskScore() { return riskScore; }
    public String getDescription() { return description; }

    /**
     * Check if this event represents a high risk threat
     */
    public boolean isHighRisk() {
        return riskScore > 75.0;
    }

    /**
     * Check if this event is recent (within last hour)
     */
    public boolean isRecent() {
        return timestamp.isAfter(Instant.now().minusSeconds(3600));
    }

    @Override
    public String toString() {
        return String.format("ThreatEvent{user=%s, type=%s, risk=%.1f, time=%s}",
            username, eventType, riskScore, timestamp);
    }
}
