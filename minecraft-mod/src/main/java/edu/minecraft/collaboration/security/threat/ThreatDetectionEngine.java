package edu.minecraft.collaboration.security.threat;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.security.AuthenticationManager.UserRole;
import edu.minecraft.collaboration.security.SecurityAuditLogger;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Threat Detection Engine implementation
 * Monitors and detects potential security threats in real-time
 */
public class ThreatDetectionEngine {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private final SecurityAuditLogger auditLogger;
    private final Map<String, ThreatScore> userThreatScores;
    private final List<ThreatDetectionRule> detectionRules;
    private volatile boolean enabled = true;
    private volatile boolean isRunning;
    
    public ThreatDetectionEngine(SecurityAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
        this.userThreatScores = new ConcurrentHashMap<>();
        this.detectionRules = new ArrayList<>();
        this.isRunning = false;
        initializeDefaultRules();
    }
    
    /**
     * Start the threat detection engine
     */
    public void start() {
        isRunning = true;
        if (auditLogger != null) {
            auditLogger.logSecurityEvent("ThreatDetectionEngine", "Engine started", "INFO");
        }
    }
    
    /**
     * Stop the threat detection engine
     */
    public void stop() {
        isRunning = false;
        if (auditLogger != null) {
            auditLogger.logSecurityEvent("ThreatDetectionEngine", "Engine stopped", "INFO");
        }
    }
    
    /**
     * Check if the engine is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Analyze user behavior for potential threats
     */
    public ThreatAnalysisResult analyzeThreat(String userId, String action, Map<String, Object> context) {
        if (!isRunning) {
            return new ThreatAnalysisResult(false, 0.0, "Engine not running");
        }
        
        try {
            ThreatScore currentScore = userThreatScores.computeIfAbsent(userId, 
                k -> new ThreatScore(userId));
            
            double riskScore = calculateRiskScore(action, context);
            currentScore.updateScore(riskScore);
            
            boolean isThreat = currentScore.getTotalScore() > 70.0; // Threshold
            String analysis = String.format("Risk score: %.2f, Total: %.2f", 
                riskScore, currentScore.getTotalScore());
            
            if (isThreat && auditLogger != null) {
                auditLogger.logThreatDetected(userId, action, currentScore.getTotalScore());
            }
            
            return new ThreatAnalysisResult(isThreat, currentScore.getTotalScore(), analysis);
        } catch (Exception e) {
            if (auditLogger != null) {
                auditLogger.logSecurityEvent("ThreatDetectionEngine", 
                    "Analysis error: " + e.getMessage(), "ERROR");
            }
            return new ThreatAnalysisResult(false, 0.0, "Analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Get current threat score for a user
     */
    public double getThreatScore(String userId) {
        ThreatScore score = userThreatScores.get(userId);
        return score != null ? score.getTotalScore() : 0.0;
    }
    
    /**
     * Reset threat score for a user
     */
    public void resetThreatScore(String userId) {
        userThreatScores.remove(userId);
        if (auditLogger != null) {
            auditLogger.logSecurityEvent("ThreatDetectionEngine", 
                "Reset threat score for user: " + userId, "INFO");
        }
    }
    
    /**
     * Set enabled state of the threat detection engine
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (auditLogger != null) {
            auditLogger.logSecurityEvent("ThreatDetectionEngine", 
                "Engine " + (enabled ? "enabled" : "disabled"), "INFO");
        }
    }
    
    /**
     * Check if the engine is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Analyze user activity and return threat assessment
     */
    public ThreatAssessment analyzeUserActivity(UserActivityEvent event) {
        // Return safe result if disabled
        if (!enabled) {
            return ThreatAssessment.builder(event.getUsername())
                .threatLevel(ThreatLevel.NONE)
                .riskScore(0.0)
                .reason("System disabled")
                .build();
        }
        String username = event.getUsername();
        String action = event.getAction();
        
        // Build context from event
        Map<String, Object> context = new HashMap<>(event.getMetadata());
        context.put("timestamp", event.getTimestamp());
        context.put("ip_address", event.getIpAddress());
        
        // Analyze threat using existing method
        ThreatAnalysisResult result = analyzeThreat(username, action, context);
        
        // Convert to ThreatAssessment
        ThreatLevel threatLevel = ThreatLevel.fromScore(result.getRiskScore());
        
        ThreatAssessment.Builder builder = ThreatAssessment.builder(username)
            .threatLevel(threatLevel)
            .riskScore(result.getRiskScore())
            .reason(result.getDetails());
        
        // Add detected threats and recommendations based on threat level
        if (result.isThreat()) {
            builder.addThreat("Suspicious activity pattern detected");
            
            if (threatLevel == ThreatLevel.HIGH || threatLevel == ThreatLevel.CRITICAL) {
                builder.addRecommendation("Consider temporary access restrictions");
                builder.addRecommendation("Review user activity logs");
            }
            
            if (threatLevel == ThreatLevel.CRITICAL) {
                builder.addRecommendation("Immediate security review required");
                builder.addRecommendation("Consider account suspension");
            }
        }
        
        ThreatAssessment assessment = builder.build();
        
        // Log to audit system if available
        if (auditLogger != null && result.isThreat()) {
            Map<String, Object> auditContext = new HashMap<>();
            auditContext.put("threat_level", threatLevel.name());
            auditContext.put("risk_score", result.getRiskScore());
            auditContext.put("action", action);
            
            auditLogger.logSuspiciousActivity(username, result.getDetails(), auditContext);
        }
        
        return assessment;
    }
    
    /**
     * Add a custom detection rule
     */
    public void addDetectionRule(ThreatDetectionRule rule) {
        detectionRules.add(rule);
    }
    
    /**
     * Get threat metrics for monitoring
     */
    public Map<String, Long> getThreatMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        
        // Count active threats
        long activeThreats = userThreatScores.values().stream()
            .filter(score -> score.getTotalScore() > 50)
            .count();
        
        // Count total monitored users
        long totalUsers = userThreatScores.size();
        
        // Count high risk users
        long highRiskUsers = userThreatScores.values().stream()
            .filter(score -> score.getTotalScore() > 75)
            .count();
        
        metrics.put("active_threats", activeThreats);
        metrics.put("total_users", totalUsers);
        metrics.put("high_risk_users", highRiskUsers);
        metrics.put("detection_rules", (long) detectionRules.size());
        
        return metrics;
    }
    
    /**
     * Close the threat detection engine
     */
    public void close() {
        // Clean up resources
        userThreatScores.clear();
        detectionRules.clear();
        LOGGER.info("ThreatDetectionEngine closed");
    }
    
    /**
     * Get recent threat events
     */
    public List<ThreatEvent> getRecentThreatEvents(int count) {
        // Placeholder implementation - would integrate with actual event storage
        List<ThreatEvent> events = new ArrayList<>();
        
        // Create sample events for testing
        for (int i = 0; i < Math.min(count, 3); i++) {
            events.add(new ThreatEvent(
                "user" + i,
                "suspicious_activity",
                Instant.now().minusSeconds(i * 60),
                50.0 + (i * 10)
            ));
        }
        
        return events;
    }
    
    /**
     * Calculate risk score based on action and context
     */
    private double calculateRiskScore(String action, Map<String, Object> context) {
        double baseScore = 0.0;
        
        // Apply detection rules
        for (ThreatDetectionRule rule : detectionRules) {
            baseScore += rule.evaluate(action, context);
        }
        
        return Math.min(100.0, Math.max(0.0, baseScore));
    }
    
    /**
     * Initialize default detection rules
     */
    private void initializeDefaultRules() {
        // High frequency actions rule
        addDetectionRule((action, context) -> {
            if ("high_frequency".equals(context.get("pattern"))) {
                return 25.0;
            }
            return 0.0;
        });
        
        // Suspicious commands rule
        addDetectionRule((action, context) -> {
            if (action.contains("admin") || action.contains("delete") || action.contains("modify")) {
                return 15.0;
            }
            return 0.0;
        });
        
        // Off-hours access rule
        addDetectionRule((action, context) -> {
            Boolean isOffHours = (Boolean) context.get("off_hours");
            if (Boolean.TRUE.equals(isOffHours)) {
                return 10.0;
            }
            return 0.0;
        });
    }
    
    /**
     * Threat detection rule interface
     */
    @FunctionalInterface
    public interface ThreatDetectionRule {
        double evaluate(String action, Map<String, Object> context);
    }
    
    /**
     * Threat score tracking for users
     */
    public static class ThreatScore {
        private final String userId;
        private double totalScore;
        private Instant lastUpdate;
        private final List<Double> recentScores;
        
        public ThreatScore(String userId) {
            this.userId = userId;
            this.totalScore = 0.0;
            this.lastUpdate = Instant.now();
            this.recentScores = new ArrayList<>();
        }
        
        public void updateScore(double newScore) {
            recentScores.add(newScore);
            if (recentScores.size() > 10) { // Keep last 10 scores
                recentScores.remove(0);
            }
            
            // Calculate weighted average (recent scores have more weight)
            double weightedSum = 0.0;
            double weightTotal = 0.0;
            for (int i = 0; i < recentScores.size(); i++) {
                double weight = 0.5 + (i * 0.5 / recentScores.size());
                weightedSum += recentScores.get(i) * weight;
                weightTotal += weight;
            }
            
            totalScore = weightedSum / weightTotal;
            lastUpdate = Instant.now();
        }
        
        public double getTotalScore() {
            return totalScore;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public Instant getLastUpdate() {
            return lastUpdate;
        }
    }
    
    /**
     * Result of threat analysis
     */
    public static class ThreatAnalysisResult {
        private final boolean isThreat;
        private final double riskScore;
        private final String details;
        
        public ThreatAnalysisResult(boolean isThreat, double riskScore, String details) {
            this.isThreat = isThreat;
            this.riskScore = riskScore;
            this.details = details;
        }
        
        public boolean isThreat() {
            return isThreat;
        }
        
        public double getRiskScore() {
            return riskScore;
        }
        
        public String getDetails() {
            return details;
        }
    }
}