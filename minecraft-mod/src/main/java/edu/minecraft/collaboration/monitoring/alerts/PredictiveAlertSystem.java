package edu.minecraft.collaboration.monitoring.alerts;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.core.ResourceManager;
import edu.minecraft.collaboration.monitoring.apm.HealthReport;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Predictive Alert System using machine learning-inspired algorithms
 * Analyzes historical data to predict and prevent system issues
 */
public class PredictiveAlertSystem implements AutoCloseable {

    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    private final ResourceManager resourceManager;
    private final ScheduledExecutorService scheduler;
    private final AlertRuleEngine ruleEngine;
    private final PredictionEngine predictionEngine;
    private final AlertNotificationManager notificationManager;

    // Historical data storage
    private final Queue<HealthDataPoint> healthHistory = new ConcurrentLinkedQueue<>();
    private final Map<String, Queue<MetricDataPoint>> metricHistory = new ConcurrentHashMap<>();
    private final List<Alert> activeAlerts = new CopyOnWriteArrayList<>();
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();

    private volatile boolean enabled = true;
    private static final int MAX_HISTORY_SIZE = 10000;
    private static final Duration PREDICTION_WINDOW = Duration.ofMinutes(30);

    public PredictiveAlertSystem() {
        this.resourceManager = ResourceManager.getInstance();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "PredictiveAlert-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });

        this.ruleEngine = new AlertRuleEngine();
        this.predictionEngine = new PredictionEngine();
        this.notificationManager = new AlertNotificationManager();

        resourceManager.registerExecutor("PredictiveAlertSystem", scheduler);

        // Initialize default alert rules
        initializeDefaultAlertRules();

        // Start predictive analysis
        startPredictiveAnalysis();

        LOGGER.info("Predictive Alert System initialized");
    }

    /**
     * Process health data and check for alerts
     */
    public void processHealthData(HealthReport healthReport) {
        if (!enabled) return;

        try {
            // Store health data
            HealthDataPoint dataPoint = new HealthDataPoint(
                healthReport.getCpuUsage(),
                healthReport.getMemoryUsagePercent(),
                healthReport.getActiveConnections(),
                healthReport.getAverageResponseTime(),
                healthReport.getErrorRate(),
                Instant.now()
            );

            healthHistory.offer(dataPoint);

            // Clean up old data
            cleanupOldData();

            // Check immediate alert rules
            checkImmediateAlerts(healthReport);

            // Update predictions
            updatePredictions();

        } catch (Exception e) {
            LOGGER.error("Error processing health data for alerts", e);
        }
    }

    /**
     * Process custom metric data
     */
    public void processMetricData(String metricName, double value) {
        if (!enabled) return;

        MetricDataPoint dataPoint = new MetricDataPoint(value, Instant.now());
        Queue<MetricDataPoint> history = metricHistory.computeIfAbsent(metricName, k -> new ConcurrentLinkedQueue<>());
        history.offer(dataPoint);

        // Clean up old metric data
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        history.removeIf(point -> point.timestamp.isBefore(cutoff));
    }

    /**
     * Add a custom alert rule
     */
    public void addAlertRule(AlertRule rule) {
        alertRules.put(rule.getName(), rule);
        LOGGER.info("Added alert rule: {}", rule.getName());
    }

    /**
     * Remove an alert rule
     */
    public void removeAlertRule(String ruleName) {
        alertRules.remove(ruleName);
        LOGGER.info("Removed alert rule: {}", ruleName);
    }

    /**
     * Get all active alerts
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }

    /**
     * Get alerts by severity
     */
    public List<Alert> getAlertsBySeverity(AlertSeverity severity) {
        return activeAlerts.stream()
            .filter(alert -> alert.getSeverity() == severity)
            .collect(Collectors.toList());
    }

    /**
     * Get prediction results
     */
    public PredictionResult getPredictions() {
        return predictionEngine.getLatestPredictions();
    }

    /**
     * Acknowledge an alert
     */
    public void acknowledgeAlert(String alertId, String acknowledgedBy) {
        activeAlerts.stream()
            .filter(alert -> alert.getId().equals(alertId))
            .findFirst()
            .ifPresent(alert -> {
                alert.acknowledge(acknowledgedBy);
                LOGGER.info("Alert {} acknowledged by {}", alertId, acknowledgedBy);
            });
    }

    /**
     * Resolve an alert
     */
    public void resolveAlert(String alertId, String resolvedBy) {
        activeAlerts.removeIf(alert -> {
            if (alert.getId().equals(alertId)) {
                alert.resolve(resolvedBy);
                LOGGER.info("Alert {} resolved by {}", alertId, resolvedBy);
                return true;
            }
            return false;
        });
    }

    private void initializeDefaultAlertRules() {
        // High CPU usage rule
        addAlertRule(AlertRule.builder()
            .name("HIGH_CPU_USAGE")
            .description("CPU usage is critically high")
            .severity(AlertSeverity.CRITICAL)
            .condition(data -> data.getCpuUsage() > 90.0)
            .threshold(90.0)
            .evaluationWindow(Duration.ofMinutes(2))
            .build());

        // High memory usage rule
        addAlertRule(AlertRule.builder()
            .name("HIGH_MEMORY_USAGE")
            .description("Memory usage is critically high")
            .severity(AlertSeverity.CRITICAL)
            .condition(data -> data.getMemoryUsage() > 85.0)
            .threshold(85.0)
            .evaluationWindow(Duration.ofMinutes(2))
            .build());

        // High error rate rule
        addAlertRule(AlertRule.builder()
            .name("HIGH_ERROR_RATE")
            .description("Error rate is too high")
            .severity(AlertSeverity.HIGH)
            .condition(data -> data.getErrorRate() > 10.0)
            .threshold(10.0)
            .evaluationWindow(Duration.ofMinutes(5))
            .build());

        // Slow response time rule
        addAlertRule(AlertRule.builder()
            .name("SLOW_RESPONSE_TIME")
            .description("Response time is too slow")
            .severity(AlertSeverity.MEDIUM)
            .condition(data -> data.getResponseTime() > 2000.0)
            .threshold(2000.0)
            .evaluationWindow(Duration.ofMinutes(3))
            .build());

        // Predictive CPU spike rule
        addAlertRule(AlertRule.builder()
            .name("PREDICTED_CPU_SPIKE")
            .description("CPU spike predicted in next 30 minutes")
            .severity(AlertSeverity.WARNING)
            .condition(data -> false) // Will be set by prediction engine
            .threshold(80.0)
            .evaluationWindow(Duration.ofMinutes(1))
            .isPredictive(true)
            .build());

        // Predictive memory exhaustion rule
        addAlertRule(AlertRule.builder()
            .name("PREDICTED_MEMORY_EXHAUSTION")
            .description("Memory exhaustion predicted")
            .severity(AlertSeverity.HIGH)
            .condition(data -> false) // Will be set by prediction engine
            .threshold(95.0)
            .evaluationWindow(Duration.ofMinutes(1))
            .isPredictive(true)
            .build());

        LOGGER.info("Initialized {} default alert rules", alertRules.size());
    }

    private void checkImmediateAlerts(HealthReport healthReport) {
        HealthDataPoint currentData = new HealthDataPoint(
            healthReport.getCpuUsage(),
            healthReport.getMemoryUsagePercent(),
            healthReport.getActiveConnections(),
            healthReport.getAverageResponseTime(),
            healthReport.getErrorRate(),
            Instant.now()
        );

        for (AlertRule rule : alertRules.values()) {
            if (rule.isPredictive()) {
                continue; // Skip predictive rules in immediate check
            }

            try {
                if (ruleEngine.evaluateRule(rule, currentData, getRecentHealthData(rule.getEvaluationWindow()))) {
                    createOrUpdateAlert(rule, currentData);
                }
            } catch (Exception e) {
                LOGGER.error("Error evaluating alert rule: {}", rule.getName(), e);
            }
        }
    }

    private void updatePredictions() {
        if (healthHistory.size() < 10) {
            return; // Need minimum data for predictions
        }

        try {
            PredictionResult predictions = predictionEngine.generatePredictions(healthHistory, PREDICTION_WINDOW);

            // Check predictive alert rules
            for (AlertRule rule : alertRules.values()) {
                if (!rule.isPredictive()) {
                    continue;
                }

                if (checkPredictiveRule(rule, predictions)) {
                    HealthDataPoint currentData = healthHistory.stream()
                        .reduce((first, second) -> second)
                        .orElse(null);
                    if (currentData != null) {
                        createOrUpdateAlert(rule, currentData);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error updating predictions", e);
        }
    }

    private boolean checkPredictiveRule(AlertRule rule, PredictionResult predictions) {
        switch (rule.getName()) {
            case "PREDICTED_CPU_SPIKE":
                return predictions.getPredictedCpuMax() > rule.getThreshold();
            case "PREDICTED_MEMORY_EXHAUSTION":
                return predictions.getPredictedMemoryMax() > rule.getThreshold();
            default:
                return false;
        }
    }

    private void createOrUpdateAlert(AlertRule rule, HealthDataPoint data) {
        // Check if alert already exists
        Optional<Alert> existingAlert = activeAlerts.stream()
            .filter(alert -> alert.getRuleName().equals(rule.getName()))
            .filter(alert -> alert.getStatus() == AlertStatus.ACTIVE)
            .findFirst();

        if (existingAlert.isPresent()) {
            // Update existing alert
            existingAlert.get().updateLastTriggered();
        } else {
            // Create new alert
            Alert alert = Alert.builder()
                .ruleName(rule.getName())
                .severity(rule.getSeverity())
                .description(rule.getDescription())
                .details(generateAlertDetails(rule, data))
                .build();

            activeAlerts.add(alert);

            // Send notification
            notificationManager.sendAlert(alert);

            LOGGER.warn("ALERT TRIGGERED: {} - {}", rule.getName(), rule.getDescription());
        }
    }

    private Map<String, Object> generateAlertDetails(AlertRule rule, HealthDataPoint data) {
        Map<String, Object> details = new HashMap<>();
        details.put("cpuUsage", data.getCpuUsage());
        details.put("memoryUsage", data.getMemoryUsage());
        details.put("activeConnections", data.getActiveConnections());
        details.put("responseTime", data.getResponseTime());
        details.put("errorRate", data.getErrorRate());
        details.put("threshold", rule.getThreshold());
        details.put("timestamp", data.getTimestamp().toString());

        if (rule.isPredictive()) {
            details.put("predictionWindow", PREDICTION_WINDOW.toString());
            details.put("predictive", true);
        }

        return details;
    }

    private List<HealthDataPoint> getRecentHealthData(Duration window) {
        Instant cutoff = Instant.now().minus(window);
        return healthHistory.stream()
            .filter(point -> point.getTimestamp().isAfter(cutoff))
            .collect(Collectors.toList());
    }

    private void startPredictiveAnalysis() {
        // Run predictive analysis every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                updatePredictions();
            }
        }, 5, 5, TimeUnit.MINUTES);

        // Clean up resolved alerts every hour
        scheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                cleanupResolvedAlerts();
            }
        }, 1, 1, TimeUnit.HOURS);

        // Auto-resolve alerts that are no longer triggered
        scheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                autoResolveAlerts();
            }
        }, 2, 2, TimeUnit.MINUTES);
    }

    private void cleanupOldData() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));

        // Clean health history
        while (healthHistory.size() > MAX_HISTORY_SIZE) {
            healthHistory.poll();
        }
        healthHistory.removeIf(point -> point.getTimestamp().isBefore(cutoff));
    }

    private void cleanupResolvedAlerts() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));
        activeAlerts.removeIf(alert ->
            alert.getStatus() == AlertStatus.RESOLVED
            && alert.getResolvedAt() != null
            && alert.getResolvedAt().isBefore(cutoff)
        );
    }

    private void autoResolveAlerts() {
        if (healthHistory.isEmpty()) return;

        HealthDataPoint latestData = healthHistory.stream()
            .reduce((first, second) -> second)
            .orElse(null);

        if (latestData == null) return;

        List<Alert> alertsToResolve = new ArrayList<>();

        for (Alert alert : activeAlerts) {
            if (alert.getStatus() != AlertStatus.ACTIVE) continue;

            AlertRule rule = alertRules.get(alert.getRuleName());
            if (rule == null) continue;

            // Check if condition is no longer met
            if (!ruleEngine.evaluateRule(rule, latestData, getRecentHealthData(rule.getEvaluationWindow()))) {
                // Allow some time for the condition to stabilize before auto-resolving
                if (Duration.between(alert.getLastTriggered(), Instant.now()).toMinutes() > 10) {
                    alertsToResolve.add(alert);
                }
            }
        }

        for (Alert alert : alertsToResolve) {
            resolveAlert(alert.getId(), "system-auto-resolve");
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("Predictive Alert System {}", enabled ? "enabled" : "disabled");
    }

    @Override
    public void close() {
        LOGGER.info("Shutting down Predictive Alert System");
        enabled = false;

        // Clean up resources
        healthHistory.clear();
        metricHistory.clear();
        activeAlerts.clear();
        alertRules.clear();

        // Shutdown scheduler
        resourceManager.unregisterAndShutdownExecutor("PredictiveAlertSystem");
    }
}
