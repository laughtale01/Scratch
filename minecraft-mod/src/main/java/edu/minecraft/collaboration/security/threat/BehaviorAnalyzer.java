package edu.minecraft.collaboration.security.threat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Behavioral Analysis component for detecting suspicious behavior patterns
 */
public class BehaviorAnalyzer {
    
    /**
     * Analyze user behavior patterns for threats
     */
    public ThreatIndicator analyze(UserActivityEvent event, UserThreatProfile profile) {
        StringBuilder description = new StringBuilder();
        ThreatSeverity maxSeverity = ThreatSeverity.NONE;
        
        // Check for rapid successive actions
        ThreatSeverity rapidActionSeverity = checkRapidActions(event, profile);
        if (rapidActionSeverity != ThreatSeverity.NONE) {
            description.append("rapid successive actions, ");
            maxSeverity = getMaxSeverity(maxSeverity, rapidActionSeverity);
        }
        
        // Check for failed attempt patterns
        ThreatSeverity failedAttemptSeverity = checkFailedAttemptPattern(event, profile);
        if (failedAttemptSeverity != ThreatSeverity.NONE) {
            description.append("suspicious failed attempt pattern, ");
            maxSeverity = getMaxSeverity(maxSeverity, failedAttemptSeverity);
        }
        
        // Check for privilege escalation behavior
        ThreatSeverity privilegeEscalationSeverity = checkPrivilegeEscalation(event, profile);
        if (privilegeEscalationSeverity != ThreatSeverity.NONE) {
            description.append("privilege escalation attempts, ");
            maxSeverity = getMaxSeverity(maxSeverity, privilegeEscalationSeverity);
        }
        
        // Check for data harvesting behavior
        ThreatSeverity dataHarvestingSeverity = checkDataHarvesting(event, profile);
        if (dataHarvestingSeverity != ThreatSeverity.NONE) {
            description.append("potential data harvesting, ");
            maxSeverity = getMaxSeverity(maxSeverity, dataHarvestingSeverity);
        }
        
        // Check for session anomalies
        ThreatSeverity sessionAnomalySeverity = checkSessionAnomalies(event, profile);
        if (sessionAnomalySeverity != ThreatSeverity.NONE) {
            description.append("session anomalies, ");
            maxSeverity = getMaxSeverity(maxSeverity, sessionAnomalySeverity);
        }
        
        // Check for automation patterns
        ThreatSeverity automationSeverity = checkAutomationPatterns(event, profile);
        if (automationSeverity != ThreatSeverity.NONE) {
            description.append("automated behavior patterns, ");
            maxSeverity = getMaxSeverity(maxSeverity, automationSeverity);
        }
        
        if (maxSeverity == ThreatSeverity.NONE) {
            return ThreatIndicator.none();
        }
        
        // Remove trailing comma and space
        String finalDescription = "Behavioral analysis: " + description.toString().replaceAll(", $", "");
        
        Map<String, Object> context = Map.of(
            "rapidActions", rapidActionSeverity.name(),
            "failedAttempts", failedAttemptSeverity.name(),
            "privilegeEscalation", privilegeEscalationSeverity.name(),
            "dataHarvesting", dataHarvestingSeverity.name(),
            "sessionAnomalies", sessionAnomalySeverity.name(),
            "automation", automationSeverity.name()
        );
        
        return new ThreatIndicator("BEHAVIOR_ANALYSIS", maxSeverity, finalDescription, context);
    }
    
    private ThreatSeverity checkRapidActions(UserActivityEvent event, UserThreatProfile profile) {
        List<Instant> recentTimestamps = profile.getRecentActivityTimestamps(Duration.ofMinutes(1));
        
        if (recentTimestamps.size() > 20) {
            return ThreatSeverity.HIGH; // More than 20 actions per minute
        } else if (recentTimestamps.size() > 10) {
            return ThreatSeverity.MEDIUM; // More than 10 actions per minute
        } else if (recentTimestamps.size() > 5) {
            return ThreatSeverity.LOW; // More than 5 actions per minute
        }
        
        return ThreatSeverity.NONE;
    }
    
    private ThreatSeverity checkFailedAttemptPattern(UserActivityEvent event, UserThreatProfile profile) {
        int recentFailures = profile.getRecentFailedAttempts();
        Duration timeSinceFirstFailure = Duration.between(profile.getFirstFailedAttempt(), Instant.now());
        
        if (recentFailures > 10 && timeSinceFirstFailure.toMinutes() < 5) {
            return ThreatSeverity.CRITICAL; // Rapid brute force
        } else if (recentFailures > 5 && timeSinceFirstFailure.toMinutes() < 10) {
            return ThreatSeverity.HIGH; // Potential brute force
        } else if (recentFailures > 3 && timeSinceFirstFailure.toMinutes() < 15) {
            return ThreatSeverity.MEDIUM; // Suspicious pattern
        }
        
        return ThreatSeverity.NONE;
    }
    
    private ThreatSeverity checkPrivilegeEscalation(UserActivityEvent event, UserThreatProfile profile) {
        if (!profile.hasRecentPrivilegeAttempts()) {
            return ThreatSeverity.NONE;
        }
        
        // Check if user is consistently trying to access higher privilege resources
        int privilegeAttempts = profile.getPrivilegeEscalationAttempts();
        
        switch (event.getUserRole()) {
            case STUDENT -> {
                if (privilegeAttempts > 3) {
                    return ThreatSeverity.HIGH; // Student trying to access admin functions
                } else if (privilegeAttempts > 1) {
                    return ThreatSeverity.MEDIUM;
                }
            }
            case TEACHER -> {
                if (privilegeAttempts > 5) {
                    return ThreatSeverity.MEDIUM; // Teacher trying to access admin functions
                }
            }
            case ADMIN -> {
                // Admin attempting privilege escalation is very suspicious
                if (privilegeAttempts > 1) {
                    return ThreatSeverity.CRITICAL;
                }
            }
        }
        
        return ThreatSeverity.NONE;
    }
    
    private ThreatSeverity checkDataHarvesting(UserActivityEvent event, UserThreatProfile profile) {
        // Check for patterns that suggest data exfiltration
        int dataAccessCount = profile.getRecentDataAccessCount();
        boolean accessingSensitiveData = profile.hasAccessedSensitiveResources();
        boolean unusualDataPattern = profile.hasUnusualDataAccessPattern();
        
        if (dataAccessCount > 100 && accessingSensitiveData && unusualDataPattern) {
            return ThreatSeverity.HIGH; // Strong indication of data harvesting
        } else if (dataAccessCount > 50 && (accessingSensitiveData || unusualDataPattern)) {
            return ThreatSeverity.MEDIUM; // Moderate indication
        } else if (dataAccessCount > 20 && unusualDataPattern) {
            return ThreatSeverity.LOW; // Slight indication
        }
        
        return ThreatSeverity.NONE;
    }
    
    private ThreatSeverity checkSessionAnomalies(UserActivityEvent event, UserThreatProfile profile) {
        Duration sessionDuration = profile.getCurrentSessionDuration();
        boolean hasMultipleSessions = profile.hasMultipleActiveSessions();
        boolean hasSessionJumping = profile.hasSessionJumpingPattern();
        
        if (sessionDuration.toHours() > 12 && hasMultipleSessions) {
            return ThreatSeverity.MEDIUM; // Very long session with multiple connections
        } else if (hasSessionJumping) {
            return ThreatSeverity.LOW; // Rapid session changes
        }
        
        return ThreatSeverity.NONE;
    }
    
    private ThreatSeverity checkAutomationPatterns(UserActivityEvent event, UserThreatProfile profile) {
        boolean hasRoboticTiming = profile.hasRobotTimingPattern();
        boolean hasIdenticalRequests = profile.hasIdenticalRequestPattern();
        boolean hasHighFrequency = profile.getRecentActivityCount(Duration.ofMinutes(1)) > 15;
        
        if (hasRoboticTiming && hasIdenticalRequests && hasHighFrequency) {
            return ThreatSeverity.HIGH; // Strong bot behavior
        } else if ((hasRoboticTiming && hasIdenticalRequests) || (hasRoboticTiming && hasHighFrequency)) {
            return ThreatSeverity.MEDIUM; // Moderate bot behavior
        } else if (hasRoboticTiming || hasIdenticalRequests) {
            return ThreatSeverity.LOW; // Slight bot behavior
        }
        
        return ThreatSeverity.NONE;
    }
    
    private ThreatSeverity getMaxSeverity(ThreatSeverity current, ThreatSeverity candidate) {
        return candidate.isHigherThan(current) ? candidate : current;
    }
}