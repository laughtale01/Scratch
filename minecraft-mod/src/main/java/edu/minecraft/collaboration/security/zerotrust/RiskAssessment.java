package edu.minecraft.collaboration.security.zerotrust;

import java.util.*;

/**
 * Risk Assessment result containing detailed risk analysis
 */
public class RiskAssessment {
    
    private final double riskScore;
    private final ZeroTrustAccessControl.RiskLevel riskLevel;
    private final int userRisk;
    private final int networkRisk;
    private final int operationRisk;
    private final int timeRisk;
    private final int sessionRisk;
    private final int deviceRisk;
    private final Set<String> requiredVerifications;
    private final List<String> riskIndicators;
    private final Map<String, Object> additionalData;
    
    private RiskAssessment(Builder builder) {
        this.riskScore = builder.riskScore;
        this.riskLevel = builder.riskLevel;
        this.userRisk = builder.userRisk;
        this.networkRisk = builder.networkRisk;
        this.operationRisk = builder.operationRisk;
        this.timeRisk = builder.timeRisk;
        this.sessionRisk = builder.sessionRisk;
        this.deviceRisk = builder.deviceRisk;
        this.requiredVerifications = new HashSet<>(builder.requiredVerifications);
        this.riskIndicators = new ArrayList<>(builder.riskIndicators);
        this.additionalData = new HashMap<>(builder.additionalData);
    }
    
    public double getRiskScore() { return riskScore; }
    public ZeroTrustAccessControl.RiskLevel getRiskLevel() { return riskLevel; }
    public int getUserRisk() { return userRisk; }
    public int getNetworkRisk() { return networkRisk; }
    public int getOperationRisk() { return operationRisk; }
    public int getTimeRisk() { return timeRisk; }
    public int getSessionRisk() { return sessionRisk; }
    public int getDeviceRisk() { return deviceRisk; }
    public Set<String> getRequiredVerifications() { return requiredVerifications; }
    public List<String> getRiskIndicators() { return riskIndicators; }
    public Map<String, Object> getAdditionalData() { return additionalData; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private double riskScore = 0.0;
        private ZeroTrustAccessControl.RiskLevel riskLevel = ZeroTrustAccessControl.RiskLevel.LOW;
        private int userRisk = 0;
        private int networkRisk = 0;
        private int operationRisk = 0;
        private int timeRisk = 0;
        private int sessionRisk = 0;
        private int deviceRisk = 0;
        private Set<String> requiredVerifications = new HashSet<>();
        private List<String> riskIndicators = new ArrayList<>();
        private Map<String, Object> additionalData = new HashMap<>();
        
        public Builder riskScore(double riskScore) {
            this.riskScore = riskScore;
            return this;
        }
        
        public Builder riskLevel(ZeroTrustAccessControl.RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }
        
        public Builder userRisk(int userRisk) {
            this.userRisk = userRisk;
            return this;
        }
        
        public Builder networkRisk(int networkRisk) {
            this.networkRisk = networkRisk;
            return this;
        }
        
        public Builder operationRisk(int operationRisk) {
            this.operationRisk = operationRisk;
            return this;
        }
        
        public Builder timeRisk(int timeRisk) {
            this.timeRisk = timeRisk;
            return this;
        }
        
        public Builder sessionRisk(int sessionRisk) {
            this.sessionRisk = sessionRisk;
            return this;
        }
        
        public Builder deviceRisk(int deviceRisk) {
            this.deviceRisk = deviceRisk;
            return this;
        }
        
        public Builder requiredVerifications(Set<String> requiredVerifications) {
            this.requiredVerifications = new HashSet<>(requiredVerifications);
            return this;
        }
        
        public Builder addRequiredVerification(String verification) {
            this.requiredVerifications.add(verification);
            return this;
        }
        
        public Builder riskIndicators(List<String> riskIndicators) {
            this.riskIndicators = new ArrayList<>(riskIndicators);
            return this;
        }
        
        public Builder addRiskIndicator(String indicator) {
            this.riskIndicators.add(indicator);
            return this;
        }
        
        public Builder additionalData(String key, Object value) {
            this.additionalData.put(key, value);
            return this;
        }
        
        public RiskAssessment build() {
            return new RiskAssessment(this);
        }
    }
}