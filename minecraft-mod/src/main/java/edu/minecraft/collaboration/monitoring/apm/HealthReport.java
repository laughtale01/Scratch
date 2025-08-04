package edu.minecraft.collaboration.monitoring.apm;

import java.time.Instant;

/**
 * System health report containing key performance indicators
 */
public class HealthReport {
    
    private final double cpuUsage;
    private final long memoryUsage;
    private final int activeConnections;
    private final double averageResponseTime;
    private final double requestCount;
    private final double errorCount;
    private final Instant timestamp;
    
    private HealthReport(Builder builder) {
        this.cpuUsage = builder.cpuUsage;
        this.memoryUsage = builder.memoryUsage;
        this.activeConnections = builder.activeConnections;
        this.averageResponseTime = builder.averageResponseTime;
        this.requestCount = builder.requestCount;
        this.errorCount = builder.errorCount;
        this.timestamp = Instant.now();
    }
    
    public double getCpuUsage() { return cpuUsage; }
    public long getMemoryUsage() { return memoryUsage; }
    public int getActiveConnections() { return activeConnections; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public double getRequestCount() { return requestCount; }
    public double getErrorCount() { return errorCount; }
    public Instant getTimestamp() { return timestamp; }
    
    /**
     * Calculate error rate as percentage
     */
    public double getErrorRate() {
        return requestCount > 0 ? (errorCount / requestCount) * 100 : 0.0;
    }
    
    /**
     * Get memory usage as percentage of max memory
     */
    public double getMemoryUsagePercent() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        return ((double) memoryUsage / maxMemory) * 100;
    }
    
    /**
     * Determine overall system health status
     */
    public HealthStatus getOverallHealth() {
        if (cpuUsage > 90 || getMemoryUsagePercent() > 90 || getErrorRate() > 10) {
            return HealthStatus.CRITICAL;
        } else if (cpuUsage > 70 || getMemoryUsagePercent() > 70 || getErrorRate() > 5 || averageResponseTime > 1000) {
            return HealthStatus.WARNING;
        } else if (cpuUsage > 50 || getMemoryUsagePercent() > 50 || getErrorRate() > 2 || averageResponseTime > 500) {
            return HealthStatus.CAUTION;
        } else {
            return HealthStatus.HEALTHY;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return String.format(
            "HealthReport{status=%s, cpu=%.1f%%, memory=%.1f%% (%dMB), connections=%d, " +
            "responseTime=%.1fms, requests=%.0f, errors=%.0f, errorRate=%.2f%%}",
            getOverallHealth(),
            cpuUsage,
            getMemoryUsagePercent(),
            memoryUsage / (1024 * 1024),
            activeConnections,
            averageResponseTime,
            requestCount,
            errorCount,
            getErrorRate()
        );
    }
    
    public static class Builder {
        private double cpuUsage = 0.0;
        private long memoryUsage = 0L;
        private int activeConnections = 0;
        private double averageResponseTime = 0.0;
        private double requestCount = 0.0;
        private double errorCount = 0.0;
        
        public Builder cpuUsage(double cpuUsage) {
            this.cpuUsage = cpuUsage;
            return this;
        }
        
        public Builder memoryUsage(long memoryUsage) {
            this.memoryUsage = memoryUsage;
            return this;
        }
        
        public Builder activeConnections(int activeConnections) {
            this.activeConnections = activeConnections;
            return this;
        }
        
        public Builder averageResponseTime(double averageResponseTime) {
            this.averageResponseTime = averageResponseTime;
            return this;
        }
        
        public Builder requestCount(double requestCount) {
            this.requestCount = requestCount;
            return this;
        }
        
        public Builder errorCount(double errorCount) {
            this.errorCount = errorCount;
            return this;
        }
        
        public HealthReport build() {
            return new HealthReport(this);
        }
    }
    
    public enum HealthStatus {
        HEALTHY("Healthy", "green"),
        CAUTION("Caution", "yellow"),
        WARNING("Warning", "orange"),
        CRITICAL("Critical", "red");
        
        private final String displayName;
        private final String color;
        
        HealthStatus(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }
}