package edu.minecraft.collaboration.monitoring.apm;

/**
 * Statistics for a profiled method
 */
public class MethodStats {
    
    private final String methodName;
    private final long callCount;
    private final double averageTime;
    private final double minTime;
    private final double maxTime;
    private final double errorRate;
    
    public MethodStats(String methodName, long callCount, double averageTime, 
                      double minTime, double maxTime, double errorRate) {
        this.methodName = methodName;
        this.callCount = callCount;
        this.averageTime = averageTime;
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.errorRate = errorRate;
    }
    
    public String getMethodName() { return methodName; }
    public long getCallCount() { return callCount; }
    public double getAverageTime() { return averageTime; }
    public double getMinTime() { return minTime; }
    public double getMaxTime() { return maxTime; }
    public double getErrorRate() { return errorRate; }
    
    /**
     * Get performance grade based on average time
     */
    public String getPerformanceGrade() {
        if (averageTime < 10) return "A"; // < 10ms
        else if (averageTime < 50) return "B"; // < 50ms
        else if (averageTime < 100) return "C"; // < 100ms
        else if (averageTime < 500) return "D"; // < 500ms
        else return "F"; // >= 500ms
    }
    
    /**
     * Check if this method is considered slow
     */
    public boolean isSlow() {
        return averageTime > 100; // > 100ms average
    }
    
    /**
     * Check if this method has high error rate
     */
    public boolean hasHighErrorRate() {
        return errorRate > 5.0; // > 5% error rate
    }
    
    @Override
    public String toString() {
        return String.format(
            "MethodStats{method='%s', calls=%d, avgTime=%.2fms, min=%.2fms, max=%.2fms, errorRate=%.2f%%, grade=%s}",
            methodName, callCount, averageTime, minTime, maxTime, errorRate, getPerformanceGrade()
        );
    }
}