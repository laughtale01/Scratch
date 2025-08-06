package edu.minecraft.collaboration.monitoring.alerts;

import java.time.Instant;

/**
 * Represents a single health data point for analysis
 */
public class HealthDataPoint {
    
    private final double cpuUsage;
    private final double memoryUsage;
    private final int activeConnections;
    private final double responseTime;
    private final double errorRate;
    private final Instant timestamp;
    
    public HealthDataPoint(double cpuUsage, double memoryUsage, int activeConnections, 
                          double responseTime, double errorRate, Instant timestamp) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.activeConnections = activeConnections;
        this.responseTime = responseTime;
        this.errorRate = errorRate;
        this.timestamp = timestamp;
    }
    
    public double getCpuUsage() { return cpuUsage; }
    public double getMemoryUsage() { return memoryUsage; }
    public int getActiveConnections() { return activeConnections; }
    public double getResponseTime() { return responseTime; }
    public double getErrorRate() { return errorRate; }
    public Instant getTimestamp() { return timestamp; }
    
    /**
     * Calculate overall health score (0-100, higher is better)
     */
    public double getHealthScore() {
        double cpuScore = Math.max(0, 100 - cpuUsage);
        double memoryScore = Math.max(0, 100 - memoryUsage);
        double responseScore = Math.max(0, 100 - Math.min(100, responseTime / 10)); // 1000ms = 0 score
        double errorScore = Math.max(0, 100 - errorRate * 10); // 10% error = 0 score
        
        return (cpuScore + memoryScore + responseScore + errorScore) / 4.0;
    }
    
    /**
     * Check if this data point indicates a healthy system
     */
    public boolean isHealthy() {
        return cpuUsage < 70 && memoryUsage < 70 && responseTime < 500 && errorRate < 2;
    }
    
    /**
     * Check if this data point indicates a critical system state
     */
    public boolean isCritical() {
        return cpuUsage > 90 || memoryUsage > 90 || responseTime > 2000 || errorRate > 10;
    }
    
    @Override
    public String toString() {
        return String.format(
            "HealthDataPoint{cpu=%.1f%%, memory=%.1f%%, connections=%d, responseTime=%.1fms, errorRate=%.2f%%, score=%.1f}",
            cpuUsage, memoryUsage, activeConnections, responseTime, errorRate, getHealthScore()
        );
    }
}

/**
 * Represents a single metric data point
 */
class MetricDataPoint {
    
    final double value;
    final Instant timestamp;
    
    public MetricDataPoint(double value, Instant timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }
    
    public double getValue() { return value; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("MetricDataPoint{value=%.2f, timestamp=%s}", value, timestamp);
    }
}