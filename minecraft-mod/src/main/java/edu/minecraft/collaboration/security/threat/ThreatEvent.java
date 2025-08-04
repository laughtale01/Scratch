package edu.minecraft.collaboration.security.threat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a threat event containing the activity, assessment, and indicators
 */
public class ThreatEvent {
    
    private final UserActivityEvent activity;
    private final ThreatAssessment assessment;
    private final List<ThreatIndicator> indicators;
    private final Instant detectionTime;
    
    public ThreatEvent(UserActivityEvent activity, ThreatAssessment assessment, List<ThreatIndicator> indicators) {
        this.activity = activity;
        this.assessment = assessment;
        this.indicators = new ArrayList<>(indicators);
        this.detectionTime = Instant.now();
    }
    
    public UserActivityEvent getActivity() { return activity; }
    public ThreatAssessment getAssessment() { return assessment; }
    public List<ThreatIndicator> getIndicators() { return indicators; }
    public Instant getDetectionTime() { return detectionTime; }
    
    /**
     * Get a summary of the threat event
     */
    public String getSummary() {
        return String.format("Threat Event: %s threat (%s) detected for user %s at %s - %d indicators",
            assessment.getThreatLevel().name(),
            assessment.getThreatType(),
            activity.getUsername(),
            detectionTime,
            indicators.size()
        );
    }
    
    /**
     * Check if this is a high-priority threat
     */
    public boolean isHighPriority() {
        return assessment.getThreatLevel() == ThreatLevel.HIGH || 
               assessment.getThreatLevel() == ThreatLevel.CRITICAL;
    }
    
    /**
     * Get the threat severity level
     */
    public ThreatLevel getThreatLevel() {
        return assessment.getThreatLevel();
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}