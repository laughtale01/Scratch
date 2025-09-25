package edu.minecraft.collaboration.monitoring.alerts;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of evaluating multiple alert rules
 */
public class AlertEvaluationResult {

    private final Map<String, Boolean> ruleResults;
    private final List<AlertRule> triggeredRules;
    private final Map<String, String> ruleErrors;
    private final Instant evaluationTime;

    private AlertEvaluationResult(Builder builder) {
        this.ruleResults = new HashMap<>(builder.ruleResults);
        this.triggeredRules = new ArrayList<>(builder.triggeredRules);
        this.ruleErrors = new HashMap<>(builder.ruleErrors);
        this.evaluationTime = Instant.now();
    }

    public Map<String, Boolean> getRuleResults() { return ruleResults; }
    public List<AlertRule> getTriggeredRules() { return triggeredRules; }
    public Map<String, String> getRuleErrors() { return ruleErrors; }
    public Instant getEvaluationTime() { return evaluationTime; }

    /**
     * Check if any rules were triggered
     */
    public boolean hasTriggeredRules() {
        return !triggeredRules.isEmpty();
    }

    /**
     * Check if any errors occurred during evaluation
     */
    public boolean hasErrors() {
        return !ruleErrors.isEmpty();
    }

    /**
     * Get the count of triggered rules
     */
    public int getTriggeredRuleCount() {
        return triggeredRules.size();
    }

    /**
     * Get the highest severity of triggered rules
     */
    public AlertSeverity getHighestSeverity() {
        return triggeredRules.stream()
            .map(AlertRule::getSeverity)
            .max((s1, s2) -> Integer.compare(s1.getLevel(), s2.getLevel()))
            .orElse(AlertSeverity.LOW);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format(
            "AlertEvaluationResult{triggered=%d, errors=%d, highestSeverity=%s}",
            getTriggeredRuleCount(), ruleErrors.size(),
            hasTriggeredRules() ? getHighestSeverity() : "NONE"
        );
    }

    public static class Builder {
        private Map<String, Boolean> ruleResults = new HashMap<>();
        private List<AlertRule> triggeredRules = new ArrayList<>();
        private Map<String, String> ruleErrors = new HashMap<>();

        public Builder addRuleResult(String ruleName, boolean triggered) {
            this.ruleResults.put(ruleName, triggered);
            return this;
        }

        public Builder addTriggeredRule(AlertRule rule) {
            this.triggeredRules.add(rule);
            return this;
        }

        public Builder addRuleError(String ruleName, String error) {
            this.ruleErrors.put(ruleName, error);
            return this;
        }

        public AlertEvaluationResult build() {
            return new AlertEvaluationResult(this);
        }
    }
}
