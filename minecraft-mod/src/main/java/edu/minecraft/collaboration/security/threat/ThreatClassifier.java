package edu.minecraft.collaboration.security.threat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Threat Classifier that combines multiple threat indicators into a final assessment
 */
public class ThreatClassifier {
    
    /**
     * Classify threats based on multiple indicators
     */
    public ThreatAssessment classify(List<ThreatIndicator> indicators, UserActivityEvent event) {
        if (indicators.isEmpty()) {
            return ThreatAssessment.noThreat("No threat indicators detected");
        }
        
        // Calculate overall threat score
        ThreatClassificationResult result = calculateThreatScore(indicators);
        
        // Determine threat type based on indicators
        String threatType = determineThreatType(indicators, event);
        
        // Calculate confidence level
        double confidence = calculateConfidence(indicators, result);
        
        // Determine mitigation actions
        ThreatAssessment.Builder assessment = ThreatAssessment.builder()
            .threatLevel(result.threatLevel)
            .threatType(threatType)
            .confidence(confidence)
            .description(result.description)
            .timestamp(event.getTimestamp());
        
        // Add mitigation actions based on threat level
        addMitigationActions(assessment, result.threatLevel, threatType);
        
        // Add metadata
        assessment.metadata("indicatorCount", indicators.size())
                 .metadata("highestSeverity", result.highestSeverity.name())
                 .metadata("threatScore", result.threatScore)
                 .metadata("username", event.getUsername())
                 .metadata("activity", event.getActivity());
        
        return assessment.build();
    }
    
    private ThreatClassificationResult calculateThreatScore(List<ThreatIndicator> indicators) {
        double totalScore = 0.0;
        ThreatSeverity highestSeverity = ThreatSeverity.NONE;
        Map<String, Integer> severityCounts = new HashMap<>();
        
        // Analyze indicators
        for (ThreatIndicator indicator : indicators) {
            ThreatSeverity severity = indicator.getSeverity();
            
            // Weight the score based on severity
            double weight = switch (severity) {
                case CRITICAL -> 4.0;
                case HIGH -> 3.0;
                case MEDIUM -> 2.0;
                case LOW -> 1.0;
                case NONE -> 0.0;
            };
            
            totalScore += weight;
            
            if (severity.isHigherThan(highestSeverity)) {
                highestSeverity = severity;
            }
            
            severityCounts.merge(severity.name(), 1, Integer::sum);
        }
        
        // Determine final threat level
        ThreatLevel threatLevel = determineThreatLevel(totalScore, highestSeverity, severityCounts);
        
        // Generate description
        String description = generateThreatDescription(indicators, threatLevel, severityCounts);
        
        return new ThreatClassificationResult(threatLevel, totalScore, highestSeverity, description);
    }
    
    private ThreatLevel determineThreatLevel(double totalScore, ThreatSeverity highestSeverity, 
                                          Map<String, Integer> severityCounts) {
        
        // Critical threat conditions
        if (highestSeverity == ThreatSeverity.CRITICAL || totalScore >= 12.0) {
            return ThreatLevel.CRITICAL;
        }
        
        // High threat conditions
        if (highestSeverity == ThreatSeverity.HIGH || 
            totalScore >= 8.0 || 
            severityCounts.getOrDefault("HIGH", 0) >= 2) {
            return ThreatLevel.HIGH;
        }
        
        // Medium threat conditions
        if (highestSeverity == ThreatSeverity.MEDIUM || 
            totalScore >= 5.0 || 
            severityCounts.getOrDefault("MEDIUM", 0) >= 2) {
            return ThreatLevel.MEDIUM;
        }
        
        // Low threat conditions
        if (highestSeverity == ThreatSeverity.LOW || 
            totalScore >= 2.0 || 
            severityCounts.getOrDefault("LOW", 0) >= 3) {
            return ThreatLevel.LOW;
        }
        
        return ThreatLevel.NONE;
    }
    
    private String determineThreatType(List<ThreatIndicator> indicators, UserActivityEvent event) {
        Map<String, Integer> typeCounts = new HashMap<>();
        
        // Count indicator types
        for (ThreatIndicator indicator : indicators) {
            String type = indicator.getType();
            typeCounts.merge(type, 1, Integer::sum);
        }
        
        // Find the most common threat type
        String primaryType = typeCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("UNKNOWN");
        
        // Map to specific threat categories
        return switch (primaryType) {
            case "ANOMALY_DETECTION" -> "ANOMALOUS_BEHAVIOR";
            case "BEHAVIOR_ANALYSIS" -> "BEHAVIORAL_THREAT";
            case "PATTERN_DETECTION" -> "PATTERN_BASED_THREAT";
            case "NETWORK_THREAT" -> "NETWORK_SECURITY_THREAT";
            default -> "SECURITY_VIOLATION";
        };
    }
    
    private double calculateConfidence(List<ThreatIndicator> indicators, ThreatClassificationResult result) {
        if (indicators.isEmpty()) {
            return 0.0;
        }
        
        // Base confidence on consistency of indicators
        double baseConfidence = 0.5;
        
        // Increase confidence for multiple consistent indicators
        if (indicators.size() >= 3) {
            baseConfidence += 0.2;
        }
        
        // Increase confidence for high severity indicators
        if (result.highestSeverity == ThreatSeverity.CRITICAL) {
            baseConfidence += 0.3;
        } else if (result.highestSeverity == ThreatSeverity.HIGH) {
            baseConfidence += 0.2;
        }
        
        // Increase confidence for high threat scores
        if (result.threatScore >= 10.0) {
            baseConfidence += 0.2;
        } else if (result.threatScore >= 6.0) {
            baseConfidence += 0.1;
        }
        
        return Math.min(1.0, baseConfidence);
    }
    
    private String generateThreatDescription(List<ThreatIndicator> indicators, ThreatLevel threatLevel, 
                                           Map<String, Integer> severityCounts) {
        StringBuilder description = new StringBuilder();
        
        description.append(String.format("%s threat detected with %d indicators: ", 
            threatLevel.name(), indicators.size()));
        
        // Add severity summary
        if (severityCounts.getOrDefault("CRITICAL", 0) > 0) {
            description.append(String.format("%d critical, ", severityCounts.get("CRITICAL")));
        }
        if (severityCounts.getOrDefault("HIGH", 0) > 0) {
            description.append(String.format("%d high, ", severityCounts.get("HIGH")));
        }
        if (severityCounts.getOrDefault("MEDIUM", 0) > 0) {
            description.append(String.format("%d medium, ", severityCounts.get("MEDIUM")));
        }
        if (severityCounts.getOrDefault("LOW", 0) > 0) {
            description.append(String.format("%d low, ", severityCounts.get("LOW")));
        }
        
        // Remove trailing comma and space
        String result = description.toString().replaceAll(", $", " severity indicators");
        
        return result;
    }
    
    private void addMitigationActions(ThreatAssessment.Builder assessment, ThreatLevel threatLevel, String threatType) {
        switch (threatLevel) {
            case CRITICAL -> {
                assessment.addMitigationAction("IMMEDIATE_ACCOUNT_SUSPENSION")
                         .addMitigationAction("SECURITY_TEAM_ALERT")
                         .addMitigationAction("INCIDENT_RESPONSE_ACTIVATION")
                         .addMitigationAction("FORENSIC_DATA_COLLECTION");
            }
            case HIGH -> {
                assessment.addMitigationAction("ENHANCED_MONITORING")
                         .addMitigationAction("REQUIRE_ADDITIONAL_AUTHENTICATION")
                         .addMitigationAction("SUPERVISOR_NOTIFICATION")
                         .addMitigationAction("SESSION_REVIEW");
            }
            case MEDIUM -> {
                assessment.addMitigationAction("INCREASED_LOGGING")
                         .addMitigationAction("USER_NOTIFICATION")
                         .addMitigationAction("BEHAVIORAL_ANALYSIS");
            }
            case LOW -> {
                assessment.addMitigationAction("ACTIVITY_MONITORING")
                         .addMitigationAction("PATTERN_TRACKING");
            }
        }
        
        // Add threat-type specific actions
        if ("NETWORK_SECURITY_THREAT".equals(threatType)) {
            assessment.addMitigationAction("IP_REPUTATION_CHECK")
                     .addMitigationAction("NETWORK_ANALYSIS");
        } else if ("BEHAVIORAL_THREAT".equals(threatType)) {
            assessment.addMitigationAction("USER_BEHAVIOR_REVIEW")
                     .addMitigationAction("TRAINING_RECOMMENDATION");
        }
    }
    
    /**
     * Result of threat classification
     */
    private static class ThreatClassificationResult {
        final ThreatLevel threatLevel;
        final double threatScore;
        final ThreatSeverity highestSeverity;
        final String description;
        
        ThreatClassificationResult(ThreatLevel threatLevel, double threatScore, 
                                 ThreatSeverity highestSeverity, String description) {
            this.threatLevel = threatLevel;
            this.threatScore = threatScore;
            this.highestSeverity = highestSeverity;
            this.description = description;
        }
    }
}