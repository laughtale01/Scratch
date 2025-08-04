package edu.minecraft.collaboration.monitoring.apm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive performance report
 */
public class PerformanceReport {
    
    private final int totalMethods;
    private final long totalCalls;
    private final List<MethodStats> topSlowestMethods;
    private final List<MethodStats> topFrequentMethods;
    private final List<ProfileEvent> recentEvents;
    private final Instant generatedAt;
    
    private PerformanceReport(Builder builder) {
        this.totalMethods = builder.totalMethods;
        this.totalCalls = builder.totalCalls;
        this.topSlowestMethods = new ArrayList<>(builder.topSlowestMethods);
        this.topFrequentMethods = new ArrayList<>(builder.topFrequentMethods);
        this.recentEvents = new ArrayList<>(builder.recentEvents);
        this.generatedAt = Instant.now();
    }
    
    public int getTotalMethods() { return totalMethods; }
    public long getTotalCalls() { return totalCalls; }
    public List<MethodStats> getTopSlowestMethods() { return topSlowestMethods; }
    public List<MethodStats> getTopFrequentMethods() { return topFrequentMethods; }
    public List<ProfileEvent> getRecentEvents() { return recentEvents; }
    public Instant getGeneratedAt() { return generatedAt; }
    
    /**
     * Get overall performance summary
     */
    public String getSummary() {
        int slowMethods = (int) topSlowestMethods.stream().filter(MethodStats::isSlow).count();
        int errorMethods = (int) topSlowestMethods.stream().filter(MethodStats::hasHighErrorRate).count();
        long slowEvents = recentEvents.stream().filter(ProfileEvent::isSlow).count();
        long errorEvents = recentEvents.stream().filter(event -> !event.isSuccessful()).count();
        
        return String.format(
            "Performance Summary: %d methods profiled, %d total calls, %d slow methods, %d error-prone methods, " +
            "%d slow recent events, %d failed recent events",
            totalMethods, totalCalls, slowMethods, errorMethods, slowEvents, errorEvents
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    public static class Builder {
        private int totalMethods = 0;
        private long totalCalls = 0L;
        private List<MethodStats> topSlowestMethods = new ArrayList<>();
        private List<MethodStats> topFrequentMethods = new ArrayList<>();
        private List<ProfileEvent> recentEvents = new ArrayList<>();
        
        public Builder totalMethods(int totalMethods) {
            this.totalMethods = totalMethods;
            return this;
        }
        
        public Builder totalCalls(long totalCalls) {
            this.totalCalls = totalCalls;
            return this;
        }
        
        public Builder topSlowestMethods(List<MethodStats> topSlowestMethods) {
            this.topSlowestMethods = new ArrayList<>(topSlowestMethods);
            return this;
        }
        
        public Builder topFrequentMethods(List<MethodStats> topFrequentMethods) {
            this.topFrequentMethods = new ArrayList<>(topFrequentMethods);
            return this;
        }
        
        public Builder recentEvents(List<ProfileEvent> recentEvents) {
            this.recentEvents = new ArrayList<>(recentEvents);
            return this;
        }
        
        public PerformanceReport build() {
            return new PerformanceReport(this);
        }
    }
}