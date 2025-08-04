package edu.minecraft.collaboration.monitoring.apm;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance Profiler for detailed method-level performance analysis
 */
public class PerformanceProfiler {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final Map<String, MethodProfile> methodProfiles = new ConcurrentHashMap<>();
    private final Queue<ProfileEvent> recentEvents = new ConcurrentLinkedQueue<>();
    private final ThreadLocal<Stack<ProfileContext>> contextStack = ThreadLocal.withInitial(Stack::new);
    
    private volatile boolean enabled = true;
    private volatile int maxEventHistory = 10000;
    
    /**
     * Start profiling a method
     */
    public ProfileContext startProfiling(String methodName) {
        return startProfiling(methodName, null);
    }
    
    public ProfileContext startProfiling(String methodName, Map<String, Object> attributes) {
        if (!enabled) {
            return new NoOpProfileContext();
        }
        
        ProfileContext context = new ActiveProfileContext(methodName, attributes);
        contextStack.get().push(context);
        return context;
    }
    
    /**
     * Profile a code block
     */
    public <T> T profile(String methodName, java.util.function.Supplier<T> operation) {
        if (!enabled) {
            return operation.get();
        }
        
        try (ProfileContext context = startProfiling(methodName)) {
            return operation.get();
        }
    }
    
    /**
     * Profile a code block with attributes
     */
    public <T> T profile(String methodName, Map<String, Object> attributes, java.util.function.Supplier<T> operation) {
        if (!enabled) {
            return operation.get();
        }
        
        try (ProfileContext context = startProfiling(methodName, attributes)) {
            return operation.get();
        }
    }
    
    /**
     * Get performance statistics for a method
     */
    public MethodStats getMethodStats(String methodName) {
        MethodProfile profile = methodProfiles.get(methodName);
        return profile != null ? profile.getStats() : null;
    }
    
    /**
     * Get all method statistics
     */
    public Map<String, MethodStats> getAllMethodStats() {
        Map<String, MethodStats> stats = new HashMap<>();
        methodProfiles.forEach((name, profile) -> stats.put(name, profile.getStats()));
        return stats;
    }
    
    /**
     * Get top slowest methods
     */
    public List<MethodStats> getTopSlowestMethods(int limit) {
        return methodProfiles.values().stream()
            .map(MethodProfile::getStats)
            .sorted((a, b) -> Double.compare(b.getAverageTime(), a.getAverageTime()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Get methods with highest call frequency
     */
    public List<MethodStats> getTopFrequentMethods(int limit) {
        return methodProfiles.values().stream()
            .map(MethodProfile::getStats)
            .sorted((a, b) -> Long.compare(b.getCallCount(), a.getCallCount()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Get recent performance events
     */
    public List<ProfileEvent> getRecentEvents(int limit) {
        return recentEvents.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Clear all profiling data
     */
    public void clear() {
        methodProfiles.clear();
        recentEvents.clear();
        LOGGER.info("Performance profiling data cleared");
    }
    
    /**
     * Generate performance report
     */
    public PerformanceReport generateReport() {
        return PerformanceReport.builder()
            .totalMethods(methodProfiles.size())
            .totalCalls(methodProfiles.values().stream().mapToLong(p -> p.getCallCount().get()).sum())
            .topSlowestMethods(getTopSlowestMethods(10))
            .topFrequentMethods(getTopFrequentMethods(10))
            .recentEvents(getRecentEvents(100))
            .build();
    }
    
    private void recordProfileEvent(String methodName, Duration duration, Map<String, Object> attributes, Exception error) {
        // Update method profile
        MethodProfile profile = methodProfiles.computeIfAbsent(methodName, MethodProfile::new);
        profile.recordCall(duration, error);
        
        // Create profile event
        ProfileEvent event = new ProfileEvent(methodName, duration, attributes, error);
        recentEvents.offer(event);
        
        // Cleanup old events
        while (recentEvents.size() > maxEventHistory) {
            recentEvents.poll();
        }
        
        // Log slow methods
        if (duration.toMillis() > 1000) { // Methods taking more than 1 second
            LOGGER.warn("SLOW METHOD DETECTED: {} took {}ms", methodName, duration.toMillis());
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("Performance profiler {}", enabled ? "enabled" : "disabled");
    }
    
    public void setMaxEventHistory(int maxEventHistory) {
        this.maxEventHistory = maxEventHistory;
    }
    
    /**
     * Active profile context implementation
     */
    private class ActiveProfileContext implements ProfileContext {
        private final String methodName;
        private final Map<String, Object> attributes;
        private final Instant startTime;
        private volatile Exception error;
        
        public ActiveProfileContext(String methodName, Map<String, Object> attributes) {
            this.methodName = methodName;
            this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
            this.startTime = Instant.now();
        }
        
        @Override
        public void addAttribute(String key, Object value) {
            attributes.put(key, value);
        }
        
        @Override
        public void setError(Exception error) {
            this.error = error;
        }
        
        @Override
        public void close() {
            try {
                Duration duration = Duration.between(startTime, Instant.now());
                recordProfileEvent(methodName, duration, attributes, error);
            } finally {
                Stack<ProfileContext> stack = contextStack.get();
                if (!stack.isEmpty() && stack.peek() == this) {
                    stack.pop();
                }
            }
        }
    }
    
    /**
     * No-op profile context for when profiling is disabled
     */
    private static class NoOpProfileContext implements ProfileContext {
        @Override
        public void addAttribute(String key, Object value) {
            // No-op
        }
        
        @Override
        public void setError(Exception error) {
            // No-op
        }
        
        @Override
        public void close() {
            // No-op
        }
    }
    
    /**
     * Method profile for tracking performance of individual methods
     */
    private static class MethodProfile {
        private final String methodName;
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private volatile long minTime = Long.MAX_VALUE;
        private volatile long maxTime = 0;
        
        public MethodProfile(String methodName) {
            this.methodName = methodName;
        }
        
        public void recordCall(Duration duration, Exception error) {
            long durationNanos = duration.toNanos();
            
            callCount.incrementAndGet();
            totalTime.addAndGet(durationNanos);
            
            if (error != null) {
                errorCount.incrementAndGet();
            }
            
            // Update min/max times
            synchronized (this) {
                minTime = Math.min(minTime, durationNanos);
                maxTime = Math.max(maxTime, durationNanos);
            }
        }
        
        public AtomicLong getCallCount() {
            return callCount;
        }
        
        public MethodStats getStats() {
            long calls = callCount.get();
            long total = totalTime.get();
            long errors = errorCount.get();
            
            double averageTime = calls > 0 ? (double) total / calls / 1_000_000 : 0; // Convert to milliseconds
            double minTimeMs = calls > 0 ? (double) minTime / 1_000_000 : 0;
            double maxTimeMs = (double) maxTime / 1_000_000;
            double errorRate = calls > 0 ? (double) errors / calls * 100 : 0;
            
            return new MethodStats(methodName, calls, averageTime, minTimeMs, maxTimeMs, errorRate);
        }
    }
}