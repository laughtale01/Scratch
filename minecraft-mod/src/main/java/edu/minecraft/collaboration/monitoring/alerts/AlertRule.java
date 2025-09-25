package edu.minecraft.collaboration.monitoring.alerts;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Defines an alert rule for monitoring system health
 */
public class AlertRule {

    private final String name;
    private final String description;
    private final AlertSeverity severity;
    private final Predicate<HealthDataPoint> condition;
    private final double threshold;
    private final Duration evaluationWindow;
    private final boolean isPredictive;
    private final int minimumOccurrences;

    private AlertRule(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.severity = builder.severity;
        this.condition = builder.condition;
        this.threshold = builder.threshold;
        this.evaluationWindow = builder.evaluationWindow;
        this.isPredictive = builder.isPredictive;
        this.minimumOccurrences = builder.minimumOccurrences;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public AlertSeverity getSeverity() { return severity; }
    public Predicate<HealthDataPoint> getCondition() { return condition; }
    public double getThreshold() { return threshold; }
    public Duration getEvaluationWindow() { return evaluationWindow; }
    public boolean isPredictive() { return isPredictive; }
    public int getMinimumOccurrences() { return minimumOccurrences; }

    /**
     * Evaluate metrics against this rule
     */
    public Alert evaluate(java.util.Map<String, Double> currentMetrics) {
        if (currentMetrics == null) {
            return null;
        }

        // Simple evaluation logic - check if any metric exceeds threshold
        for (java.util.Map.Entry<String, Double> entry : currentMetrics.entrySet()) {
            Double value = entry.getValue();
            if (value != null && value > threshold) {
                return Alert.builder()
                    .ruleName(name)
                    .severity(severity)
                    .description(String.format("Alert: %s - %s exceeded threshold %.2f with value %.2f",
                        name, entry.getKey(), threshold, value))
                    .build();
            }
        }

        return null; // No alert triggered
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("AlertRule{name='%s', severity=%s, threshold=%.2f, predictive=%s}",
            name, severity, threshold, isPredictive);
    }

    public static class Builder {
        private String name;
        private String description = "";
        private AlertSeverity severity = AlertSeverity.MEDIUM;
        private Predicate<HealthDataPoint> condition;
        private double threshold = 0.0;
        private Duration evaluationWindow = Duration.ofMinutes(5);
        private boolean isPredictive = false;
        private int minimumOccurrences = 1;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder severity(AlertSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder condition(Predicate<HealthDataPoint> condition) {
            this.condition = condition;
            return this;
        }

        public Builder threshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder evaluationWindow(Duration evaluationWindow) {
            this.evaluationWindow = evaluationWindow;
            return this;
        }

        public Builder isPredictive(boolean isPredictive) {
            this.isPredictive = isPredictive;
            return this;
        }

        public Builder minimumOccurrences(int minimumOccurrences) {
            this.minimumOccurrences = minimumOccurrences;
            return this;
        }

        public AlertRule build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Alert rule name is required");
            }
            if (condition == null) {
                throw new IllegalArgumentException("Alert rule condition is required");
            }
            return new AlertRule(this);
        }
    }
}
