package edu.minecraft.collaboration.security.threat;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents the result of threat detection analysis
 */
public class ThreatAssessment {
    
    private final String username;
    private final ThreatLevel threatLevel;
    private final double riskScore;
    private final List<String> detectedThreats;
    private final List<String> recommendations;
    private final Instant assessmentTime;
    private final String reason;
    
    private ThreatAssessment(Builder builder) {
        this.username = builder.username;
        this.threatLevel = builder.threatLevel;
        this.riskScore = builder.riskScore;
        this.detectedThreats = new ArrayList<>(builder.detectedThreats);
        this.recommendations = new ArrayList<>(builder.recommendations);
        this.assessmentTime = Instant.now();
        this.reason = builder.reason;
    }
    
    public String getUsername() { return username; }
    public ThreatLevel getThreatLevel() { return threatLevel; }
    public double getRiskScore() { return riskScore; }
    public List<String> getDetectedThreats() { return detectedThreats; }
    public List<String> getRecommendations() { return recommendations; }
    public Instant getAssessmentTime() { return assessmentTime; }
    public String getReason() { return reason; }
    
    public boolean isThreatDetected() {
        return threatLevel.getSeverity() > ThreatLevel.NONE.getSeverity();
    }
    
    public boolean isHighRisk() {
        return threatLevel == ThreatLevel.HIGH || threatLevel == ThreatLevel.CRITICAL;
    }
    
    public boolean isError() {
        return false; // Assessments don't have errors in this implementation
    }
    
    public String getThreatType() {
        if (detectedThreats.isEmpty()) {
            return "none";
        }
        return detectedThreats.get(0); // Return the primary threat type
    }
    
    public String getDescription() {
        if (reason != null && !reason.isEmpty()) {
            return reason;
        }
        if (!detectedThreats.isEmpty()) {
            return "Detected threats: " + String.join(", ", detectedThreats);
        }
        return "No threats detected";
    }
    
    public static Builder builder(String username) {
        return new Builder(username);
    }
    
    public static class Builder {
        private final String username;
        private ThreatLevel threatLevel = ThreatLevel.NONE;
        private double riskScore = 0.0;
        private List<String> detectedThreats = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        private String reason = "";
        
        public Builder(String username) {
            this.username = username;
        }
        
        public Builder threatLevel(ThreatLevel threatLevel) {
            this.threatLevel = threatLevel;
            return this;
        }
        
        public Builder riskScore(double riskScore) {
            this.riskScore = riskScore;
            return this;
        }
        
        public Builder detectedThreats(List<String> threats) {
            this.detectedThreats = new ArrayList<>(threats);
            return this;
        }
        
        public Builder addThreat(String threat) {
            this.detectedThreats.add(threat);
            return this;
        }
        
        public Builder recommendations(List<String> recommendations) {
            this.recommendations = new ArrayList<>(recommendations);
            return this;
        }
        
        public Builder addRecommendation(String recommendation) {
            this.recommendations.add(recommendation);
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public ThreatAssessment build() {
            return new ThreatAssessment(this);
        }
    }
}