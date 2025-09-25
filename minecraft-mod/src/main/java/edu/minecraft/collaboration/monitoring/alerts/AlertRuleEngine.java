package edu.minecraft.collaboration.monitoring.alerts;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Alert Rule Engine for evaluating alert conditions
 */
public class AlertRuleEngine {

    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    /**
     * Evaluate an alert rule against current and historical data
     */
    public boolean evaluateRule(AlertRule rule, HealthDataPoint currentData, List<HealthDataPoint> historicalData) {
        try {
            // First check the basic condition
            if (!rule.getCondition().test(currentData)) {
                return false;
            }

            // For immediate rules (no minimum occurrences), return true
            if (rule.getMinimumOccurrences() <= 1) {
                return true;
            }

            // Count occurrences in the evaluation window
            int occurrences = countOccurrencesInWindow(rule, historicalData);

            // Include current data point if it matches
            if (rule.getCondition().test(currentData)) {
                occurrences++;
            }

            boolean triggered = occurrences >= rule.getMinimumOccurrences();

            if (triggered) {
                LOGGER.debug("Alert rule '{}' triggered: {} occurrences (minimum: {})",
                    rule.getName(), occurrences, rule.getMinimumOccurrences());
            }

            return triggered;

        } catch (Exception e) {
            LOGGER.error("Error evaluating alert rule: {}", rule.getName(), e);
            return false;
        }
    }

    /**
     * Count the number of times the rule condition was met in the historical data
     */
    private int countOccurrencesInWindow(AlertRule rule, List<HealthDataPoint> historicalData) {
        if (historicalData == null || historicalData.isEmpty()) {
            return 0;
        }

        Instant windowStart = Instant.now().minus(rule.getEvaluationWindow());

        return (int) historicalData.stream()
            .filter(dataPoint -> dataPoint.getTimestamp().isAfter(windowStart))
            .filter(rule.getCondition()::test)
            .count();
    }

    /**
     * Evaluate multiple rules against the same data
     */
    public AlertEvaluationResult evaluateMultipleRules(List<AlertRule> rules,
                                                       HealthDataPoint currentData,
                                                       List<HealthDataPoint> historicalData) {
        AlertEvaluationResult.Builder resultBuilder = AlertEvaluationResult.builder();

        for (AlertRule rule : rules) {
            try {
                boolean triggered = evaluateRule(rule, currentData, historicalData);
                resultBuilder.addRuleResult(rule.getName(), triggered);

                if (triggered) {
                    resultBuilder.addTriggeredRule(rule);
                }

            } catch (Exception e) {
                LOGGER.error("Error evaluating rule: {}", rule.getName(), e);
                resultBuilder.addRuleError(rule.getName(), e.getMessage());
            }
        }

        return resultBuilder.build();
    }

    /**
     * Check if a data point trend is concerning
     */
    public boolean isTrendConcerning(List<HealthDataPoint> data, String metric, Duration trendWindow) {
        if (data.size() < 3) {
            return false; // Need at least 3 points for trend analysis
        }

        Instant windowStart = Instant.now().minus(trendWindow);
        List<HealthDataPoint> recentData = data.stream()
            .filter(point -> point.getTimestamp().isAfter(windowStart))
            .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
            .toList();

        if (recentData.size() < 3) {
            return false;
        }

        // Calculate trend based on metric type
        switch (metric.toLowerCase()) {
            case "cpu":
                return calculateTrend(recentData, HealthDataPoint::getCpuUsage) > 20; // 20% increase trend
            case "memory":
                return calculateTrend(recentData, HealthDataPoint::getMemoryUsage) > 20;
            case "responsetime":
                return calculateTrend(recentData, HealthDataPoint::getResponseTime) > 500; // 500ms increase trend
            case "errorrate":
                return calculateTrend(recentData, HealthDataPoint::getErrorRate) > 2; // 2% increase trend
            default:
                return false;
        }
    }

    /**
     * Calculate trend slope for a metric
     */
    private double calculateTrend(List<HealthDataPoint> data, java.util.function.ToDoubleFunction<HealthDataPoint> metricExtractor) {
        if (data.size() < 2) {
            return 0.0;
        }

        // Simple linear trend calculation
        double firstValue = metricExtractor.applyAsDouble(data.get(0));
        double lastValue = metricExtractor.applyAsDouble(data.get(data.size() - 1));

        return lastValue - firstValue;
    }

    /**
     * Calculate the severity of a condition based on how much it exceeds the threshold
     */
    public AlertSeverity calculateDynamicSeverity(AlertRule baseRule, HealthDataPoint data) {
        double threshold = baseRule.getThreshold();
        double actualValue = getMetricValue(data, baseRule.getName());

        if (actualValue <= threshold) {
            return AlertSeverity.LOW;
        }

        double exceedanceRatio = actualValue / threshold;

        if (exceedanceRatio >= 2.0) {
            return AlertSeverity.CRITICAL;
        } else if (exceedanceRatio >= 1.5) {
            return AlertSeverity.HIGH;
        } else if (exceedanceRatio >= 1.2) {
            return AlertSeverity.MEDIUM;
        } else {
            return AlertSeverity.LOW;
        }
    }

    private double getMetricValue(HealthDataPoint data, String ruleName) {
        // Map rule names to metric values
        switch (ruleName.toLowerCase()) {
            case "high_cpu_usage":
            case "predicted_cpu_spike":
                return data.getCpuUsage();
            case "high_memory_usage":
            case "predicted_memory_exhaustion":
                return data.getMemoryUsage();
            case "high_error_rate":
                return data.getErrorRate();
            case "slow_response_time":
                return data.getResponseTime();
            default:
                return 0.0;
        }
    }

    /**
     * Check if system is in a degraded state
     */
    public boolean isSystemDegraded(HealthDataPoint data) {
        return data.getCpuUsage() > 80
               || data.getMemoryUsage() > 80
               || data.getResponseTime() > 1000
               || data.getErrorRate() > 5;
    }

    /**
     * Check if system is in a critical state
     */
    public boolean isSystemCritical(HealthDataPoint data) {
        return data.isCritical();
    }
}
