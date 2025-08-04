package edu.minecraft.collaboration.security.zerotrust;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Risk Assessment Engine for Zero-Trust Security
 * Analyzes context and calculates risk scores for access decisions
 */
public class RiskAssessmentEngine {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final Map<String, UserBehaviorProfile> userProfiles = new ConcurrentHashMap<>();
    private final Map<String, NetworkThreatProfile> networkProfiles = new ConcurrentHashMap<>();
    
    /**
     * Assess risk for the given access context
     */
    public RiskAssessment assessRisk(AccessContext context) {
        RiskAssessment.Builder assessment = RiskAssessment.builder();
        
        // Analyze different risk factors
        int userRisk = analyzeUserRisk(context);
        int networkRisk = analyzeNetworkRisk(context);
        int operationRisk = analyzeOperationRisk(context);
        int timeRisk = analyzeTimeRisk(context);
        int sessionRisk = analyzeSessionRisk(context);
        int deviceRisk = analyzeDeviceRisk(context);
        
        // Calculate weighted risk score
        double weightedScore = calculateWeightedRiskScore(
            userRisk, networkRisk, operationRisk, 
            timeRisk, sessionRisk, deviceRisk
        );
        
        // Determine risk level
        ZeroTrustAccessControl.RiskLevel riskLevel = determineRiskLevel(weightedScore);
        
        // Determine required verifications
        Set<String> requiredVerifications = determineRequiredVerifications(riskLevel, context);
        
        // Build final assessment
        assessment.riskScore(weightedScore)
                 .riskLevel(riskLevel)
                 .userRisk(userRisk)
                 .networkRisk(networkRisk)
                 .operationRisk(operationRisk)
                 .timeRisk(timeRisk)
                 .sessionRisk(sessionRisk)
                 .deviceRisk(deviceRisk)
                 .requiredVerifications(requiredVerifications);
        
        // Add risk indicators
        addRiskIndicators(assessment, context, riskLevel);
        
        LOGGER.debug("Risk assessment for user {}: Score={}, Level={}", 
            context.getUser().getUsername(), weightedScore, riskLevel);
        
        // Update user profile
        updateUserProfile(context, weightedScore);
        
        return assessment.build();
    }
    
    private int analyzeUserRisk(AccessContext context) {
        UserBehaviorProfile profile = getUserProfile(context.getUser().getUsername());
        int risk = 0;
        
        // Check for unusual behavior patterns
        if (profile.hasUnusualActivity()) {
            risk += 30;
        }
        
        // Check user role risk
        switch (context.getUser().getRole()) {
            case STUDENT -> risk += 10; // Lower inherent risk
            case TEACHER -> risk += 5;
            case ADMIN -> risk += 20;  // Higher privilege = higher risk
        }
        
        // Check recent failed attempts
        if (profile.getRecentFailedAttempts() > 3) {
            risk += 40;
        }
        
        // Check account age and activity
        if (profile.isNewAccount()) {
            risk += 15;
        }
        
        return Math.min(risk, 100);
    }
    
    private int analyzeNetworkRisk(AccessContext context) {
        String ipAddress = context.getNetworkContext().getIpAddress();
        int risk = 0;
        
        // Check IP reputation
        NetworkThreatProfile threatProfile = getNetworkProfile(ipAddress);
        risk += threatProfile.getThreatScore();
        
        // Check if from trusted network
        if (!isTrustedNetwork(ipAddress)) {
            risk += 25;
        }
        
        // Check for VPN/Proxy usage
        if (threatProfile.isVpnOrProxy()) {
            risk += 20;
        }
        
        // Check geographic location
        if (threatProfile.isFromHighRiskCountry()) {
            risk += 30;
        }
        
        // Check connection encryption
        if (!context.getNetworkContext().isEncrypted()) {
            risk += 40;
        }
        
        return Math.min(risk, 100);
    }
    
    private int analyzeOperationRisk(AccessContext context) {
        int risk = 0;
        
        // Risk based on operation category
        switch (context.getOperation().getCategory()) {
            case BASIC -> risk += 5;
            case COLLABORATION -> risk += 10;
            case BUILDING -> risk += 15;
            case ADMINISTRATIVE -> risk += 40;
            case SECURITY -> risk += 50;
        }
        
        // Check for sensitive resource access
        if (isSensitiveResource(context.getResource())) {
            risk += 25;
        }
        
        // Check for bulk operations
        if (isBulkOperation(context.getOperation())) {
            risk += 20;
        }
        
        return Math.min(risk, 100);
    }
    
    private int analyzeTimeRisk(AccessContext context) {
        LocalTime currentTime = context.getTimeContext().getCurrentTime();
        int risk = 0;
        
        // Higher risk during off-hours
        if (currentTime.isBefore(LocalTime.of(8, 0)) || currentTime.isAfter(LocalTime.of(18, 0))) {
            risk += 20;
        }
        
        // Very high risk during night hours
        if (currentTime.isBefore(LocalTime.of(6, 0)) || currentTime.isAfter(LocalTime.of(22, 0))) {
            risk += 40;
        }
        
        // Check for weekend access (would need additional date context)
        // This is a simplified version
        
        return Math.min(risk, 100);
    }
    
    private int analyzeSessionRisk(AccessContext context) {
        int risk = 0;
        
        // Check session duration
        Duration sessionDuration = Duration.between(
            context.getSessionContext().getSessionStart(),
            Instant.now()
        );
        
        if (sessionDuration.toHours() > 8) {
            risk += 30; // Long sessions are riskier
        }
        
        if (sessionDuration.toMinutes() < 5) {
            risk += 15; // Very new sessions are somewhat risky
        }
        
        return Math.min(risk, 100);
    }
    
    private int analyzeDeviceRisk(AccessContext context) {
        int risk = 0;
        
        // Check device type
        String deviceType = context.getDeviceContext().getDeviceType();
        if ("unknown".equals(deviceType)) {
            risk += 25;
        }
        
        // Check if device is registered/trusted
        if (!isTrustedDevice(context.getDeviceContext().getDeviceId())) {
            risk += 20;
        }
        
        return Math.min(risk, 100);
    }
    
    private double calculateWeightedRiskScore(int userRisk, int networkRisk, int operationRisk, 
                                            int timeRisk, int sessionRisk, int deviceRisk) {
        // Weighted calculation - user and network risks are most important
        return (userRisk * 0.25) +
               (networkRisk * 0.25) +
               (operationRisk * 0.20) +
               (timeRisk * 0.10) +
               (sessionRisk * 0.10) +
               (deviceRisk * 0.10);
    }
    
    private ZeroTrustAccessControl.RiskLevel determineRiskLevel(double riskScore) {
        if (riskScore >= 80) {
            return ZeroTrustAccessControl.RiskLevel.CRITICAL;
        } else if (riskScore >= 60) {
            return ZeroTrustAccessControl.RiskLevel.HIGH;
        } else if (riskScore >= 30) {
            return ZeroTrustAccessControl.RiskLevel.MEDIUM;
        } else {
            return ZeroTrustAccessControl.RiskLevel.LOW;
        }
    }
    
    private Set<String> determineRequiredVerifications(ZeroTrustAccessControl.RiskLevel riskLevel, AccessContext context) {
        Set<String> verifications = new HashSet<>();
        
        switch (riskLevel) {
            case CRITICAL -> {
                verifications.add("ADMIN_APPROVAL");
                verifications.add("SECONDARY_AUTHENTICATION");
                verifications.add("IP_WHITELIST_CHECK");
            }
            case HIGH -> {
                verifications.add("SECONDARY_AUTHENTICATION");
                verifications.add("SUPERVISOR_NOTIFICATION");
            }
            case MEDIUM -> {
                if (context.getOperation().getCategory() == ZeroTrustAccessControl.OperationCategory.ADMINISTRATIVE) {
                    verifications.add("CONFIRMATION_PROMPT");
                }
            }
            case LOW -> {
                // No additional verification required
            }
        }
        
        return verifications;
    }
    
    private void addRiskIndicators(RiskAssessment.Builder assessment, AccessContext context, ZeroTrustAccessControl.RiskLevel riskLevel) {
        List<String> indicators = new ArrayList<>();
        
        if (riskLevel == ZeroTrustAccessControl.RiskLevel.CRITICAL || riskLevel == ZeroTrustAccessControl.RiskLevel.HIGH) {
            indicators.add("High risk operation detected");
        }
        
        if (!isTrustedNetwork(context.getNetworkContext().getIpAddress())) {
            indicators.add("Access from untrusted network");
        }
        
        UserBehaviorProfile profile = getUserProfile(context.getUser().getUsername());
        if (profile.hasUnusualActivity()) {
            indicators.add("Unusual user behavior pattern");
        }
        
        if (profile.getRecentFailedAttempts() > 3) {
            indicators.add("Recent authentication failures");
        }
        
        assessment.riskIndicators(indicators);
    }
    
    private void updateUserProfile(AccessContext context, double riskScore) {
        String username = context.getUser().getUsername();
        UserBehaviorProfile profile = getUserProfile(username);
        profile.recordActivity(context.getOperation().getName(), riskScore);
    }
    
    private UserBehaviorProfile getUserProfile(String username) {
        return userProfiles.computeIfAbsent(username, k -> new UserBehaviorProfile(username));
    }
    
    private NetworkThreatProfile getNetworkProfile(String ipAddress) {
        return networkProfiles.computeIfAbsent(ipAddress, k -> new NetworkThreatProfile(ipAddress));
    }
    
    private boolean isTrustedNetwork(String ipAddress) {
        return ipAddress.startsWith("192.168.") || 
               ipAddress.startsWith("10.") || 
               ipAddress.equals("127.0.0.1");
    }
    
    private boolean isSensitiveResource(ZeroTrustAccessControl.Resource resource) {
        return "admin".equals(resource.getType()) || 
               "config".equals(resource.getType()) ||
               "security".equals(resource.getType());
    }
    
    private boolean isBulkOperation(ZeroTrustAccessControl.Operation operation) {
        return operation.getName().contains("batch") || 
               operation.getName().contains("bulk") ||
               operation.getName().contains("mass");
    }
    
    private boolean isTrustedDevice(String deviceId) {
        // In a real implementation, this would check against a database of trusted devices
        return !"unknown".equals(deviceId);
    }
    
    /**
     * User behavior profile for risk analysis
     */
    private static class UserBehaviorProfile {
        private final String username;
        private final List<ActivityRecord> recentActivities = new ArrayList<>();
        private int recentFailedAttempts = 0;
        private final Instant accountCreated;
        
        public UserBehaviorProfile(String username) {
            this.username = username;
            this.accountCreated = Instant.now();
        }
        
        public void recordActivity(String operation, double riskScore) {
            recentActivities.add(new ActivityRecord(operation, riskScore, Instant.now()));
            
            // Keep only recent activities (last 24 hours)
            Instant cutoff = Instant.now().minus(Duration.ofHours(24));
            recentActivities.removeIf(activity -> activity.timestamp.isBefore(cutoff));
        }
        
        public boolean hasUnusualActivity() {
            if (recentActivities.size() < 5) {
                return false;
            }
            
            // Check for unusual patterns (simplified)
            double avgRisk = recentActivities.stream()
                .mapToDouble(activity -> activity.riskScore)
                .average()
                .orElse(0.0);
            
            return avgRisk > 50.0;
        }
        
        public int getRecentFailedAttempts() {
            return recentFailedAttempts;
        }
        
        public boolean isNewAccount() {
            return Duration.between(accountCreated, Instant.now()).toDays() < 7;
        }
        
        private static class ActivityRecord {
            final String operation;
            final double riskScore;
            final Instant timestamp;
            
            ActivityRecord(String operation, double riskScore, Instant timestamp) {
                this.operation = operation;
                this.riskScore = riskScore;
                this.timestamp = timestamp;
            }
        }
    }
    
    /**
     * Network threat profile for IP-based risk analysis
     */
    private static class NetworkThreatProfile {
        private final String ipAddress;
        private int threatScore = 0;
        private boolean isVpnOrProxy = false;
        private boolean isHighRiskCountry = false;
        
        public NetworkThreatProfile(String ipAddress) {
            this.ipAddress = ipAddress;
            // In a real implementation, this would query threat intelligence APIs
            analyzeThreatProfile();
        }
        
        private void analyzeThreatProfile() {
            // Simplified threat analysis
            if (ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") || ipAddress.equals("127.0.0.1")) {
                threatScore = 5; // Local network - low threat
            } else {
                threatScore = 15; // External network - medium threat
            }
        }
        
        public int getThreatScore() {
            return threatScore;
        }
        
        public boolean isVpnOrProxy() {
            return isVpnOrProxy;
        }
        
        public boolean isFromHighRiskCountry() {
            return isHighRiskCountry;
        }
    }
}