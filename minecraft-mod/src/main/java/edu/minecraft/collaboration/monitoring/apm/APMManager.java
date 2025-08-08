package edu.minecraft.collaboration.monitoring.apm;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application Performance Monitoring Manager
 * Provides comprehensive monitoring and performance tracking capabilities
 */
public class APMManager {
    
    private final Map<String, PerformanceMetric> metrics;
    private final ScheduledExecutorService scheduler;
    private volatile boolean isRunning;
    
    public APMManager() {
        this.metrics = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.isRunning = false;
    }
    
    /**
     * Start the APM manager
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            startPeriodicCollection();
        }
    }
    
    /**
     * Stop the APM manager
     */
    public void stop() {
        isRunning = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Close the APM manager (alias for stop method)
     */
    public void close() {
        stop();
    }
    
    /**
     * Check if APM manager is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Record a performance metric
     */
    public void recordMetric(String metricName, double value) {
        if (!isRunning) return;
        
        PerformanceMetric metric = metrics.computeIfAbsent(metricName, 
            k -> new PerformanceMetric(metricName));
        metric.recordValue(value);
    }
    
    /**
     * Record method execution time
     */
    public void recordExecutionTime(String methodName, long executionTimeMs) {
        recordMetric("method." + methodName + ".execution_time", executionTimeMs);
    }
    
    /**
     * Record memory usage
     */
    public void recordMemoryUsage(long memoryBytes) {
        recordMetric("system.memory.usage", memoryBytes);
    }
    
    /**
     * Record CPU usage
     */
    public void recordCpuUsage(double cpuPercentage) {
        recordMetric("system.cpu.usage", cpuPercentage);
    }
    
    /**
     * Get current metric value
     */
    public double getMetricValue(String metricName) {
        PerformanceMetric metric = metrics.get(metricName);
        return metric != null ? metric.getCurrentValue() : 0.0;
    }
    
    /**
     * Get metric statistics
     */
    public MetricStatistics getMetricStatistics(String metricName) {
        PerformanceMetric metric = metrics.get(metricName);
        return metric != null ? metric.getStatistics() : null;
    }
    
    /**
     * Get all metric names
     */
    public String[] getMetricNames() {
        return metrics.keySet().toArray(new String[0]);
    }
    
    /**
     * Clear all metrics
     */
    public void clearMetrics() {
        metrics.clear();
    }
    
    /**
     * Start periodic metric collection
     */
    private void startPeriodicCollection() {
        // Collect system metrics every 30 seconds
        scheduler.scheduleAtFixedRate(this::collectSystemMetrics, 0, 30, TimeUnit.SECONDS);
        
        // Clean old metrics every 5 minutes
        scheduler.scheduleAtFixedRate(this::cleanOldMetrics, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Collect system performance metrics
     */
    private void collectSystemMetrics() {
        if (!isRunning) return;
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            recordMemoryUsage(usedMemory);
            recordMetric("system.memory.total", totalMemory);
            recordMetric("system.memory.free", freeMemory);
            
            // Record timestamp
            recordMetric("system.timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            // Silently handle errors in metric collection
        }
    }
    
    /**
     * Clean old metric data
     */
    private void cleanOldMetrics() {
        if (!isRunning) return;
        
        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        metrics.values().forEach(metric -> metric.cleanOldData(cutoffTime));
    }
    
    /**
     * Performance metric tracking
     */
    public static class PerformanceMetric {
        private final String name;
        private double currentValue;
        private double minValue;
        private double maxValue;
        private double totalValue;
        private long count;
        private Instant lastUpdate;
        
        public PerformanceMetric(String name) {
            this.name = name;
            this.currentValue = 0.0;
            this.minValue = Double.MAX_VALUE;
            this.maxValue = Double.MIN_VALUE;
            this.totalValue = 0.0;
            this.count = 0;
            this.lastUpdate = Instant.now();
        }
        
        public synchronized void recordValue(double value) {
            currentValue = value;
            totalValue += value;
            count++;
            
            if (value < minValue) minValue = value;
            if (value > maxValue) maxValue = value;
            
            lastUpdate = Instant.now();
        }
        
        public double getCurrentValue() {
            return currentValue;
        }
        
        public MetricStatistics getStatistics() {
            return new MetricStatistics(name, currentValue, minValue, maxValue, 
                count > 0 ? totalValue / count : 0.0, count, lastUpdate);
        }
        
        public void cleanOldData(long cutoffTime) {
            // Simple cleanup - in real implementation might use time-series data
            if (lastUpdate.toEpochMilli() < cutoffTime) {
                // Reset old data
                currentValue = 0.0;
                totalValue = 0.0;
                count = 0;
                minValue = Double.MAX_VALUE;
                maxValue = Double.MIN_VALUE;
            }
        }
    }
    
    /**
     * Metric statistics data
     */
    public static class MetricStatistics {
        private final String name;
        private final double currentValue;
        private final double minValue;
        private final double maxValue;
        private final double averageValue;
        private final long count;
        private final Instant lastUpdate;
        
        public MetricStatistics(String name, double currentValue, double minValue, 
                              double maxValue, double averageValue, long count, Instant lastUpdate) {
            this.name = name;
            this.currentValue = currentValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.averageValue = averageValue;
            this.count = count;
            this.lastUpdate = lastUpdate;
        }
        
        // Getters
        public String getName() { return name; }
        public double getCurrentValue() { return currentValue; }
        public double getMinValue() { return minValue; }
        public double getMaxValue() { return maxValue; }
        public double getAverageValue() { return averageValue; }
        public long getCount() { return count; }
        public Instant getLastUpdate() { return lastUpdate; }
    }
    
    /**
     * Increment a counter metric
     */
    public void incrementCounter(String counterName) {
        PerformanceMetric metric = metrics.computeIfAbsent(counterName, 
            k -> new PerformanceMetric(k));
        metric.recordValue(metric.getCurrentValue() + 1);
    }
    
    /**
     * Record timing with Duration
     */
    public void recordTiming(String timerName, java.time.Duration duration) {
        recordMetric(timerName, duration.toMillis());
    }
    
    /**
     * Time execution of a task
     */
    public <T> T timeExecution(String taskName, java.util.concurrent.Callable<T> task) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            T result = task.call();
            recordExecutionTime(taskName, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            recordMetric(taskName + ".errors", 1);
            throw e;
        }
    }
    
    /**
     * Get Prometheus format metrics
     */
    public String getPrometheusMetrics() {
        StringBuilder sb = new StringBuilder();
        sb.append("# HELP apm_metrics Application Performance Metrics\n");
        sb.append("# TYPE apm_metrics gauge\n");
        
        for (Map.Entry<String, PerformanceMetric> entry : metrics.entrySet()) {
            String metricName = entry.getKey().replace(".", "_");
            double value = entry.getValue().getCurrentValue();
            sb.append(String.format("apm_metrics{name=\"%s\"} %f\n", metricName, value));
        }
        
        return sb.toString();
    }
}