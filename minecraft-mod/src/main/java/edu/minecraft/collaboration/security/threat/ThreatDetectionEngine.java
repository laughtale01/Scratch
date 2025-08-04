package edu.minecraft.collaboration.security.threat;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.security.SecurityAuditLogger;
import edu.minecraft.collaboration.security.AuthenticationManager.UserRole;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced Threat Detection Engine
 * Implements real-time threat analysis using behavioral patterns and anomaly detection
 */
public class ThreatDetectionEngine implements AutoCloseable {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final SecurityAuditLogger auditLogger;
    private final AnomalyDetector anomalyDetector;
    private final BehaviorAnalyzer behaviorAnalyzer;
    private final ThreatClassifier threatClassifier;
    private final ScheduledExecutorService scheduler;
    
    private final Map<String, UserThreatProfile> userProfiles = new ConcurrentHashMap<>();
    private final Map<String, NetworkThreatProfile> networkProfiles = new ConcurrentHashMap<>();
    private final Queue<ThreatEvent> recentEvents = new ConcurrentLinkedQueue<>();
    private final Map<String, AtomicLong> threatMetrics = new ConcurrentHashMap<>();
    
    private volatile boolean enabled = true;
    
    public ThreatDetectionEngine(SecurityAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
        this.anomalyDetector = new AnomalyDetector();
        this.behaviorAnalyzer = new BehaviorAnalyzer();
        this.threatClassifier = new ThreatClassifier();
        
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ThreatDetection-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        startBackgroundAnalysis();
        LOGGER.info("Advanced Threat Detection Engine initialized");
    }
    
    /**
     * Analyze user activity for potential threats
     */
    public ThreatAssessment analyzeUserActivity(UserActivityEvent event) {
        if (!enabled) {
            return ThreatAssessment.noThreat("System disabled");
        }
        
        try {
            // Update user profile
            UserThreatProfile userProfile = getUserProfile(event.getUsername());
            userProfile.recordActivity(event);
            
            // Perform multi-layered analysis
            List<ThreatIndicator> indicators = new ArrayList<>();
            
            // 1. Anomaly Detection
            ThreatIndicator anomalyIndicator = anomalyDetector.analyze(event, userProfile);
            if (anomalyIndicator.getSeverity() != ThreatSeverity.NONE) {
                indicators.add(anomalyIndicator);
            }
            
            // 2. Behavioral Analysis
            ThreatIndicator behaviorIndicator = behaviorAnalyzer.analyze(event, userProfile);
            if (behaviorIndicator.getSeverity() != ThreatSeverity.NONE) {
                indicators.add(behaviorIndicator);
            }
            
            // 3. Pattern Matching
            ThreatIndicator patternIndicator = detectThreatPatterns(event, userProfile);
            if (patternIndicator.getSeverity() != ThreatSeverity.NONE) {
                indicators.add(patternIndicator);
            }
            
            // 4. Network Analysis (if applicable)
            if (event.getNetworkInfo() != null) {
                ThreatIndicator networkIndicator = analyzeNetworkThreats(event);
                if (networkIndicator.getSeverity() != ThreatSeverity.NONE) {
                    indicators.add(networkIndicator);
                }
            }
            
            // Combine indicators and classify threat
            ThreatAssessment assessment = threatClassifier.classify(indicators, event);
            
            // Record the event
            ThreatEvent threatEvent = new ThreatEvent(event, assessment, indicators);
            recentEvents.offer(threatEvent);
            
            // Clean up old events
            cleanupOldEvents();
            
            // Update metrics
            updateThreatMetrics(assessment);
            
            // Take action if threat detected
            if (assessment.getThreatLevel() != ThreatLevel.NONE) {
                handleThreatDetection(threatEvent);
            }
            
            return assessment;
            
        } catch (Exception e) {
            LOGGER.error("Error analyzing user activity for threat detection", e);
            return ThreatAssessment.error("Analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Detect specific threat patterns
     */
    private ThreatIndicator detectThreatPatterns(UserActivityEvent event, UserThreatProfile profile) {
        List<String> patterns = new ArrayList<>();
        ThreatSeverity severity = ThreatSeverity.NONE;
        
        // Check for privilege escalation attempts
        if (isPrivilegeEscalationAttempt(event, profile)) {
            patterns.add("PRIVILEGE_ESCALATION");
            severity = ThreatSeverity.HIGH;
        }
        
        // Check for brute force patterns
        if (isBruteForcePattern(event, profile)) {
            patterns.add("BRUTE_FORCE");
            severity = ThreatSeverity.MEDIUM;
        }
        
        // Check for data exfiltration patterns
        if (isDataExfiltrationPattern(event, profile)) {
            patterns.add("DATA_EXFILTRATION");
            severity = ThreatSeverity.HIGH;
        }
        
        // Check for insider threat indicators
        if (isInsiderThreatPattern(event, profile)) {
            patterns.add("INSIDER_THREAT");
            severity = ThreatSeverity.MEDIUM;
        }
        
        // Check for automation/bot behavior
        if (isBotBehaviorPattern(event, profile)) {
            patterns.add("BOT_BEHAVIOR");
            severity = ThreatSeverity.LOW;
        }
        
        if (patterns.isEmpty()) {
            return ThreatIndicator.none();
        }
        
        return new ThreatIndicator(
            "PATTERN_DETECTION",
            severity,
            "Detected threat patterns: " + String.join(", ", patterns),
            Map.of("patterns", patterns)
        );
    }
    
    /**
     * Analyze network-based threats
     */
    private ThreatIndicator analyzeNetworkThreats(UserActivityEvent event) {
        NetworkInfo networkInfo = event.getNetworkInfo();
        NetworkThreatProfile networkProfile = getNetworkProfile(networkInfo.getIpAddress());
        
        List<String> threats = new ArrayList<>();
        ThreatSeverity severity = ThreatSeverity.NONE;
        
        // Check IP reputation
        if (networkProfile.isKnownMalicious()) {
            threats.add("MALICIOUS_IP");
            severity = ThreatSeverity.HIGH;
        }
        
        // Check for suspicious geographic location
        if (networkProfile.isFromHighRiskCountry()) {
            threats.add("HIGH_RISK_GEOGRAPHY");
            severity = ThreatSeverity.MEDIUM;
        }
        
        // Check for VPN/Proxy usage
        if (networkProfile.isVpnOrProxy()) {
            threats.add("ANONYMOUS_PROXY");
            severity = ThreatSeverity.LOW;
        }
        
        // Check for connection frequency anomalies
        if (networkProfile.hasAnomalousConnectionPattern()) {
            threats.add("ABNORMAL_CONNECTION_PATTERN");
            severity = ThreatSeverity.MEDIUM;
        }
        
        if (threats.isEmpty()) {
            return ThreatIndicator.none();
        }
        
        return new ThreatIndicator(
            "NETWORK_THREAT",
            severity,
            "Network-based threats detected: " + String.join(", ", threats),
            Map.of("threats", threats, "ipAddress", networkInfo.getIpAddress())
        );
    }
    
    /**
     * Handle detected threats
     */
    private void handleThreatDetection(ThreatEvent threatEvent) {
        ThreatAssessment assessment = threatEvent.getAssessment();
        UserActivityEvent activity = threatEvent.getActivity();
        
        // Log security event
        auditLogger.logSecurityThreat(
            activity.getUsername(),
            assessment.getThreatType(),
            assessment.getThreatLevel().name(),
            Map.of(
                "confidence", String.valueOf(assessment.getConfidence()),
                "indicators", threatEvent.getIndicators().size(),
                "timestamp", activity.getTimestamp().toString()
            )
        );
        
        // Take automated response actions
        switch (assessment.getThreatLevel()) {
            case CRITICAL -> {
                // Immediate account suspension
                LOGGER.error("CRITICAL THREAT DETECTED - User: {} - Type: {} - Auto-suspending account", 
                    activity.getUsername(), assessment.getThreatType());
                
                // Would integrate with account management system
                // accountManager.suspendAccount(activity.getUsername(), "Security threat detected");
                
                // Alert administrators immediately
                // alertManager.sendImmediateAlert("Critical security threat", threatEvent);
            }
            case HIGH -> {
                // Enhanced monitoring and alerts
                LOGGER.warn("HIGH THREAT DETECTED - User: {} - Type: {} - Enhanced monitoring activated", 
                    activity.getUsername(), assessment.getThreatType());
                
                // Increase monitoring for this user
                UserThreatProfile profile = getUserProfile(activity.getUsername());
                profile.increaseMonitoringLevel();
                
                // Require additional authentication for sensitive operations
                // authManager.requireStepUpAuth(activity.getUsername());
            }
            case MEDIUM -> {
                // Additional verification and logging
                LOGGER.info("MEDIUM THREAT DETECTED - User: {} - Type: {} - Additional verification required", 
                    activity.getUsername(), assessment.getThreatType());
                
                // Log for investigation
                auditLogger.logSuspiciousActivity(
                    activity.getUsername(),
                    assessment.getThreatType(),
                    Map.of("assessment", assessment.toString())
                );
            }
            case LOW -> {
                // Just log for analysis
                LOGGER.debug("LOW THREAT DETECTED - User: {} - Type: {}", 
                    activity.getUsername(), assessment.getThreatType());
            }
        }
    }
    
    /**
     * Start background analysis tasks
     */
    private void startBackgroundAnalysis() {
        // Profile analysis every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                analyzeUserProfiles();
            }
        }, 5, 5, TimeUnit.MINUTES);
        
        // Cleanup old data every hour
        scheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                cleanupOldData();
            }
        }, 1, 1, TimeUnit.HOURS);
        
        // Update threat intelligence every 6 hours
        scheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                updateThreatIntelligence();
            }
        }, 0, 6, TimeUnit.HOURS);
    }
    
    private void analyzeUserProfiles() {
        userProfiles.values().parallelStream().forEach(profile -> {
            try {
                profile.analyzePatterns();
            } catch (Exception e) {
                LOGGER.error("Error analyzing user profile: {}", profile.getUsername(), e);
            }
        });
    }
    
    private void cleanupOldData() {
        // Clean old events
        cleanupOldEvents();
        
        // Clean old profile data
        userProfiles.values().forEach(UserThreatProfile::cleanupOldData);
        networkProfiles.values().forEach(NetworkThreatProfile::cleanupOldData);
    }
    
    private void cleanupOldEvents() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        recentEvents.removeIf(event -> event.getActivity().getTimestamp().isBefore(cutoff));
    }
    
    private void updateThreatIntelligence() {
        // In a real implementation, this would update threat intelligence feeds
        LOGGER.debug("Updating threat intelligence data");
    }
    
    private void updateThreatMetrics(ThreatAssessment assessment) {
        String metricKey = "threats." + assessment.getThreatLevel().name().toLowerCase();
        threatMetrics.computeIfAbsent(metricKey, k -> new AtomicLong(0)).incrementAndGet();
        
        if (assessment.getThreatLevel() != ThreatLevel.NONE) {
            threatMetrics.computeIfAbsent("threats.total", k -> new AtomicLong(0)).incrementAndGet();
        }
    }
    
    // Helper methods for pattern detection
    private boolean isPrivilegeEscalationAttempt(UserActivityEvent event, UserThreatProfile profile) {
        // Check if user is trying to access resources above their privilege level
        return event.getActivity().contains("admin") && 
               event.getUserRole() != UserRole.ADMIN &&
               profile.hasRecentPrivilegeAttempts();
    }
    
    private boolean isBruteForcePattern(UserActivityEvent event, UserThreatProfile profile) {
        return profile.getRecentFailedAttempts() > 5 &&
               Duration.between(profile.getFirstFailedAttempt(), Instant.now()).toMinutes() < 10;
    }
    
    private boolean isDataExfiltrationPattern(UserActivityEvent event, UserThreatProfile profile) {
        return event.getActivity().contains("export") && 
               profile.hasUnusualDataAccessPattern();
    }
    
    private boolean isInsiderThreatPattern(UserActivityEvent event, UserThreatProfile profile) {
        return profile.hasOffHoursActivity() && 
               profile.hasAccessedSensitiveResources() &&
               profile.getActivityVolume() > profile.getBaselineActivity() * 3;
    }
    
    private boolean isBotBehaviorPattern(UserActivityEvent event, UserThreatProfile profile) {
        return profile.hasRobotTimingPattern() ||
               profile.hasIdenticalRequestPattern();
    }
    
    private UserThreatProfile getUserProfile(String username) {
        return userProfiles.computeIfAbsent(username, UserThreatProfile::new);
    }
    
    private NetworkThreatProfile getNetworkProfile(String ipAddress) {
        return networkProfiles.computeIfAbsent(ipAddress, NetworkThreatProfile::new);
    }
    
    /**
     * Get threat metrics
     */
    public Map<String, Long> getThreatMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        threatMetrics.forEach((key, value) -> metrics.put(key, value.get()));
        return metrics;
    }
    
    /**
     * Get recent threat events
     */
    public List<ThreatEvent> getRecentThreatEvents(int limit) {
        return recentEvents.stream()
            .filter(event -> event.getAssessment().getThreatLevel() != ThreatLevel.NONE)
            .sorted((e1, e2) -> e2.getActivity().getTimestamp().compareTo(e1.getActivity().getTimestamp()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Enable or disable threat detection
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("Threat detection engine {}", enabled ? "enabled" : "disabled");
    }
    
    @Override
    public void close() {
        LOGGER.info("Shutting down Threat Detection Engine");
        enabled = false;
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        userProfiles.clear();
        networkProfiles.clear();
        recentEvents.clear();
        threatMetrics.clear();
    }
}