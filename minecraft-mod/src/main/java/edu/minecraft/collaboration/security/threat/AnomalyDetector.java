package edu.minecraft.collaboration.security.threat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Anomaly Detection component for identifying unusual patterns
 */
public class AnomalyDetector {
    
    private static final double ANOMALY_THRESHOLD = 2.0; // Standard deviations
    
    /**
     * Analyze user activity for anomalies
     */
    public ThreatIndicator analyze(UserActivityEvent event, UserThreatProfile profile) {
        double anomalyScore = 0.0;
        StringBuilder description = new StringBuilder("Anomaly detected: ");
        
        // Time-based anomaly detection
        double timeAnomalyScore = analyzeTimeAnomaly(event, profile);
        if (timeAnomalyScore > ANOMALY_THRESHOLD) {
            anomalyScore += timeAnomalyScore;
            description.append("unusual access time, ");
        }
        
        // Frequency anomaly detection
        double frequencyAnomalyScore = analyzeFrequencyAnomaly(event, profile);
        if (frequencyAnomalyScore > ANOMALY_THRESHOLD) {
            anomalyScore += frequencyAnomalyScore;
            description.append("unusual activity frequency, ");
        }
        
        // Resource access anomaly
        double resourceAnomalyScore = analyzeResourceAnomaly(event, profile);
        if (resourceAnomalyScore > ANOMALY_THRESHOLD) {
            anomalyScore += resourceAnomalyScore;
            description.append("unusual resource access, ");
        }
        
        // Volume anomaly detection
        double volumeAnomalyScore = analyzeVolumeAnomaly(event, profile);
        if (volumeAnomalyScore > ANOMALY_THRESHOLD) {
            anomalyScore += volumeAnomalyScore;
            description.append("unusual activity volume, ");
        }
        
        // Geographic anomaly (if network info available)
        if (event.getNetworkInfo() != null) {
            double geoAnomalyScore = analyzeGeographicAnomaly(event, profile);
            if (geoAnomalyScore > ANOMALY_THRESHOLD) {
                anomalyScore += geoAnomalyScore;
                description.append("unusual geographic location, ");
            }
        }
        
        if (anomalyScore == 0.0) {
            return ThreatIndicator.none();
        }
        
        // Remove trailing comma and space
        String finalDescription = description.toString().replaceAll(", $", "");
        
        ThreatSeverity severity = calculateSeverity(anomalyScore);
        
        Map<String, Object> context = Map.of(
            "anomalyScore", anomalyScore,
            "threshold", ANOMALY_THRESHOLD,
            "timeAnomaly", timeAnomalyScore,
            "frequencyAnomaly", frequencyAnomalyScore,
            "resourceAnomaly", resourceAnomalyScore,
            "volumeAnomaly", volumeAnomalyScore
        );
        
        return new ThreatIndicator("ANOMALY_DETECTION", severity, finalDescription, context);
    }
    
    private double analyzeTimeAnomaly(UserActivityEvent event, UserThreatProfile profile) {
        LocalTime eventTime = event.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toLocalTime();
        
        // Get user's typical activity hours
        List<LocalTime> typicalTimes = profile.getTypicalActivityTimes();
        if (typicalTimes.isEmpty()) {
            return 0.0; // No baseline yet
        }
        
        // Calculate how far this time is from typical times
        double minDistance = typicalTimes.stream()
            .mapToDouble(time -> Math.abs(Duration.between(time, eventTime).toMinutes()))
            .min()
            .orElse(0.0);
        
        // Convert to hours and apply threshold
        double hoursFromNormal = minDistance / 60.0;
        
        // High anomaly if more than 4 hours from typical time
        return Math.max(0.0, (hoursFromNormal - 4.0) / 2.0);
    }
    
    private double analyzeFrequencyAnomaly(UserActivityEvent event, UserThreatProfile profile) {
        // Get recent activity count
        int recentActivityCount = profile.getRecentActivityCount(Duration.ofHours(1));
        double baselineFrequency = profile.getBaselineHourlyFrequency();
        
        if (baselineFrequency == 0.0) {
            return 0.0; // No baseline yet
        }
        
        // Calculate z-score (standard deviations from baseline)
        double frequencyRatio = recentActivityCount / baselineFrequency;
        
        // Return anomaly score if frequency is significantly higher than baseline
        return Math.max(0.0, frequencyRatio - 2.0);
    }
    
    private double analyzeResourceAnomaly(UserActivityEvent event, UserThreatProfile profile) {
        String resource = event.getResourceAccessed();
        if (resource == null) {
            return 0.0;
        }
        
        // Check if this resource type is typical for this user
        Map<String, Integer> resourceAccess = profile.getResourceAccessPattern();
        
        if (resourceAccess.isEmpty()) {
            return 0.0; // No baseline yet
        }
        
        // Check if this is a new or rarely accessed resource type
        int accessCount = resourceAccess.getOrDefault(resource, 0);
        int totalAccess = resourceAccess.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalAccess == 0) {
            return 0.0;
        }
        
        double accessRatio = (double) accessCount / totalAccess;
        
        // High anomaly if accessing a resource type they've never or rarely used
        if (accessCount == 0) {
            return 3.0; // New resource type
        } else if (accessRatio < 0.05) {
            return 2.0; // Rarely used resource type
        }
        
        return 0.0;
    }
    
    private double analyzeVolumeAnomaly(UserActivityEvent event, UserThreatProfile profile) {
        // Analyze the volume of operations in recent time window
        int recentOperations = profile.getRecentActivityCount(Duration.ofMinutes(10));
        double baselineVolume = profile.getBaselineVolumePerWindow(Duration.ofMinutes(10));
        
        if (baselineVolume == 0.0) {
            return 0.0; // No baseline yet
        }
        
        // Calculate z-score for volume
        double volumeRatio = recentOperations / baselineVolume;
        
        // Return anomaly score if volume is significantly higher
        return Math.max(0.0, volumeRatio - 3.0);
    }
    
    private double analyzeGeographicAnomaly(UserActivityEvent event, UserThreatProfile profile) {
        NetworkInfo networkInfo = event.getNetworkInfo();
        String currentCountry = networkInfo.getCountry();
        
        if ("unknown".equals(currentCountry)) {
            return 1.0; // Slight anomaly for unknown location
        }
        
        String typicalCountry = profile.getTypicalCountry();
        if (typicalCountry == null) {
            return 0.0; // No baseline yet
        }
        
        // High anomaly if from different country than typical
        if (!currentCountry.equals(typicalCountry)) {
            return 3.0;
        }
        
        return 0.0;
    }
    
    private ThreatSeverity calculateSeverity(double anomalyScore) {
        if (anomalyScore >= 8.0) {
            return ThreatSeverity.CRITICAL;
        } else if (anomalyScore >= 6.0) {
            return ThreatSeverity.HIGH;
        } else if (anomalyScore >= 4.0) {
            return ThreatSeverity.MEDIUM;
        } else if (anomalyScore >= 2.0) {
            return ThreatSeverity.LOW;
        } else {
            return ThreatSeverity.NONE;
        }
    }
}