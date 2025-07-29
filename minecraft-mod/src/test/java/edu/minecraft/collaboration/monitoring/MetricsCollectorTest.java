package edu.minecraft.collaboration.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorTest {
    
    private MetricsCollector metricsCollector;
    
    @BeforeEach
    void setUp() {
        metricsCollector = MetricsCollector.getInstance();
    }
    
    @Test
    @DisplayName("Should increment counter correctly")
    void testIncrementCounter() {
        // Given
        String counterName = "test.counter";
        
        // When
        metricsCollector.incrementCounter(counterName);
        metricsCollector.incrementCounter(counterName, 5);
        
        // Then
        MetricsCollector.MetricsSnapshot snapshot = metricsCollector.getSnapshot();
        assertEquals(6L, snapshot.counters.get(counterName));
    }
    
    @Test
    @DisplayName("Should set gauge value")
    void testSetGauge() {
        // Given
        String gaugeName = "test.gauge";
        
        // When
        metricsCollector.setGauge(gaugeName, 42);
        
        // Then
        MetricsCollector.MetricsSnapshot snapshot = metricsCollector.getSnapshot();
        assertEquals(42L, snapshot.gauges.get(gaugeName));
    }
    
    @Test
    @DisplayName("Should record timing metrics")
    void testTimingMetrics() throws InterruptedException {
        // Given
        String timingName = "test.timing";
        
        // When
        try (MetricsCollector.TimingContext timing = metricsCollector.startTiming(timingName)) {
            Thread.sleep(50); // Simulate work
        }
        
        metricsCollector.recordTiming(timingName, 100);
        metricsCollector.recordTiming(timingName, 200);
        
        // Then
        MetricsCollector.MetricsSnapshot snapshot = metricsCollector.getSnapshot();
        MetricsCollector.TimingStatistics stats = snapshot.timings.get(timingName);
        
        assertNotNull(stats);
        assertEquals(3, stats.count);
        assertTrue(stats.average > 0);
        assertTrue(stats.min >= 50);
        assertTrue(stats.max >= 200);
    }
    
    @Test
    @DisplayName("Should collect system metrics")
    void testSystemMetrics() {
        // When
        MetricsCollector.MetricsSnapshot snapshot = metricsCollector.getSnapshot();
        
        // Then
        assertNotNull(snapshot.systemMetrics);
        assertTrue(snapshot.systemMetrics.availableProcessors > 0);
        assertTrue(snapshot.systemMetrics.totalMemory > 0);
        assertTrue(snapshot.systemMetrics.threadCount > 0);
    }
    
    @Test
    @DisplayName("Should handle WebSocket metrics")
    void testWebSocketMetrics() {
        // When
        metricsCollector.incrementCounter(MetricsCollector.Metrics.WS_CONNECTIONS_TOTAL);
        metricsCollector.setGauge(MetricsCollector.Metrics.WS_CONNECTIONS_ACTIVE, 5);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.WS_MESSAGES_RECEIVED, 100);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.WS_MESSAGES_SENT, 80);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.WS_ERRORS, 2);
        
        // Then
        MetricsCollector.MetricsSnapshot snapshot = metricsCollector.getSnapshot();
        assertEquals(1L, snapshot.counters.get(MetricsCollector.Metrics.WS_CONNECTIONS_TOTAL));
        assertEquals(5L, snapshot.gauges.get(MetricsCollector.Metrics.WS_CONNECTIONS_ACTIVE));
        assertEquals(100L, snapshot.counters.get(MetricsCollector.Metrics.WS_MESSAGES_RECEIVED));
        assertEquals(80L, snapshot.counters.get(MetricsCollector.Metrics.WS_MESSAGES_SENT));
        assertEquals(2L, snapshot.counters.get(MetricsCollector.Metrics.WS_ERRORS));
    }
    
    @Test
    @DisplayName("Should handle block operation metrics")
    void testBlockOperationMetrics() {
        // When
        metricsCollector.incrementCounter(MetricsCollector.Metrics.BLOCKS_PLACED, 50);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.BLOCKS_BROKEN, 20);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.BLOCKS_BATCH_OPERATIONS, 3);
        
        // Then
        MetricsCollector.MetricsSnapshot snapshot = metricsCollector.getSnapshot();
        assertEquals(50L, snapshot.counters.get(MetricsCollector.Metrics.BLOCKS_PLACED));
        assertEquals(20L, snapshot.counters.get(MetricsCollector.Metrics.BLOCKS_BROKEN));
        assertEquals(3L, snapshot.counters.get(MetricsCollector.Metrics.BLOCKS_BATCH_OPERATIONS));
    }
    
    @Test
    @DisplayName("Should handle collaboration metrics")
    void testCollaborationMetrics() {
        // When
        metricsCollector.incrementCounter(MetricsCollector.Metrics.INVITATIONS_SENT, 10);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.INVITATIONS_ACCEPTED, 7);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.INVITATIONS_DECLINED, 3);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.VISITS_REQUESTED, 5);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.VISITS_APPROVED, 4);
        
        // Then
        MetricsCollector.MetricsSnapshot snapshot = metricsCollector.getSnapshot();
        assertEquals(10L, snapshot.counters.get(MetricsCollector.Metrics.INVITATIONS_SENT));
        assertEquals(7L, snapshot.counters.get(MetricsCollector.Metrics.INVITATIONS_ACCEPTED));
        assertEquals(3L, snapshot.counters.get(MetricsCollector.Metrics.INVITATIONS_DECLINED));
        assertEquals(5L, snapshot.counters.get(MetricsCollector.Metrics.VISITS_REQUESTED));
        assertEquals(4L, snapshot.counters.get(MetricsCollector.Metrics.VISITS_APPROVED));
    }
    
    @Test
    @DisplayName("Should handle cache metrics")
    void testCacheMetrics() {
        // When
        metricsCollector.incrementCounter(MetricsCollector.Metrics.CACHE_HITS, 850);
        metricsCollector.incrementCounter(MetricsCollector.Metrics.CACHE_MISSES, 150);
        
        // Then
        MetricsCollector.MetricsSnapshot snapshot = metricsCollector.getSnapshot();
        assertEquals(850L, snapshot.counters.get(MetricsCollector.Metrics.CACHE_HITS));
        assertEquals(150L, snapshot.counters.get(MetricsCollector.Metrics.CACHE_MISSES));
        
        // Calculate hit rate
        long totalAccess = 850 + 150;
        double hitRate = (850.0 / totalAccess) * 100;
        assertEquals(85.0, hitRate, 0.1);
    }
    
    @Test
    @DisplayName("Should handle disabled metrics collection")
    void testDisabledMetrics() {
        // When
        metricsCollector.setEnabled(false);
        metricsCollector.incrementCounter("test.disabled");
        
        // Then
        MetricsCollector.MetricsSnapshot snapshot = metricsCollector.getSnapshot();
        // Metrics are still collected but not exported when disabled
        assertEquals(1L, snapshot.counters.get("test.disabled"));
        
        // Re-enable
        metricsCollector.setEnabled(true);
    }
}