package edu.minecraft.collaboration.monitoring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.minecraft.collaboration.core.ResourceManager;
import edu.minecraft.collaboration.util.FileSecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and manages system metrics for monitoring and analysis.
 * Converted from singleton to dependency injection pattern.
 * Now integrates with ResourceManager for proper cleanup.
 */
public final class MetricsCollector implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCollector.class);

    // Metric counters
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();
    private final Map<String, TimingMetric> timings = new ConcurrentHashMap<>();

    // System metrics
    private final SystemMetrics systemMetrics = new SystemMetrics();

    // Configuration
    private final ScheduledExecutorService scheduler;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File metricsDir;
    private final ResourceManager resourceManager;
    private boolean enabled = true;

    public MetricsCollector() {
        this.resourceManager = ResourceManager.getInstance();

        // Use secure directory creation
        if (FileSecurityUtils.ensureSafeDirectory("metrics")) {
            this.metricsDir = new File("metrics");
        } else {
            LOGGER.error("Failed to create secure metrics directory");
            this.metricsDir = null;
            this.enabled = false;
        }

        // Create scheduler with proper thread naming
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "MetricsCollector-Scheduler");
            t.setDaemon(true);
            return t;
        });

        // Register executor with ResourceManager for proper cleanup
        resourceManager.registerExecutor("MetricsCollector", scheduler);

        // Start periodic metrics collection
        startPeriodicCollection();
        LOGGER.info("MetricsCollector initialized with enabled={}", enabled);
    }

    /**
     * Increment a counter metric
     */
    public void incrementCounter(String name) {
        incrementCounter(name, 1);
    }

    public void incrementCounter(String name, long value) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(value);
    }

    /**
     * Set a gauge metric
     */
    public void setGauge(String name, long value) {
        gauges.computeIfAbsent(name, k -> new AtomicLong(0)).set(value);
    }

    /**
     * Record timing for an operation
     */
    public TimingContext startTiming(String name) {
        return new TimingContext(name);
    }

    /**
     * Record a timing value directly
     */
    public void recordTiming(String name, long durationMs) {
        timings.computeIfAbsent(name, k -> new TimingMetric(name)).record(durationMs);
    }

    /**
     * Get current metrics snapshot
     */
    public MetricsSnapshot getSnapshot() {
        MetricsSnapshot snapshot = new MetricsSnapshot();
        snapshot.timestamp = Instant.now();

        // Copy counters
        counters.forEach((name, value) ->
            snapshot.counters.put(name, value.get()));

        // Copy gauges
        gauges.forEach((name, value) ->
            snapshot.gauges.put(name, value.get()));

        // Copy timing statistics
        timings.forEach((name, timing) ->
            snapshot.timings.put(name, timing.getStatistics()));

        // Add system metrics
        snapshot.systemMetrics = systemMetrics.collect();

        return snapshot;
    }

    /**
     * Start periodic collection and export
     */
    private void startPeriodicCollection() {
        // Collect system metrics every second
        scheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                systemMetrics.update();
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Export metrics every minute
        scheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                exportMetrics();
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Export metrics to file
     */
    private void exportMetrics() {
        if (!enabled || metricsDir == null) {
            return;
        }

        try {
            MetricsSnapshot snapshot = getSnapshot();
            String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            );

            // Use secure file creation
            String fileName = "metrics_" + timestamp + ".json";
            File metricsFile = FileSecurityUtils.getSafeFile("metrics", fileName);

            if (metricsFile == null) {
                LOGGER.error("Failed to create safe metrics file");
                return;
            }

            try (FileWriter writer = new FileWriter(metricsFile)) {
                gson.toJson(snapshot, writer);
            }

            LOGGER.info("Exported metrics to: {}", metricsFile.getName());

            // Clean up old metrics files (keep last 24 hours)
            cleanupOldMetrics();

        } catch (IOException e) {
            LOGGER.error("Failed to export metrics", e);
        }
    }

    /**
     * Clean up metrics files older than 24 hours
     */
    private void cleanupOldMetrics() {
        if (!enabled || metricsDir == null) {
            return;
        }

        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        File[] files = metricsDir.listFiles((dir, name) -> name.startsWith("metrics_"));

        if (files != null) {
            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    // Validate file is in metrics directory before deletion
                    try {
                        String canonicalPath = file.getCanonicalPath();
                        String metricsCanonical = metricsDir.getCanonicalPath();

                        if (canonicalPath.startsWith(metricsCanonical + File.separator)) {
                            if (!file.delete()) {
                                LOGGER.warn("Failed to delete old metrics file: {}", file.getName());
                            }
                        } else {
                            LOGGER.error("Attempted to delete file outside metrics directory: {}", canonicalPath);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error validating file path for deletion", e);
                    }
                }
            }
        }
    }

    /**
     * Enable or disable metrics collection
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Shutdown metrics collector - delegates to close()
     */
    public void shutdown() {
        close();
    }

    /**
     * Close the metrics collector and release resources
     */
    @Override
    public void close() {
        LOGGER.info("Closing MetricsCollector");
        enabled = false;

        // Clear all metrics data
        counters.clear();
        gauges.clear();
        timings.clear();

        // ResourceManager will handle executor shutdown
        resourceManager.unregisterAndShutdownExecutor("MetricsCollector");
    }

    /**
     * Context for timing measurements
     */
    public final class TimingContext implements AutoCloseable {
        private final String name;
        private final long startTime;

        private TimingContext(String name) {
            this.name = name;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            recordTiming(name, duration);
        }
    }

    /**
     * Timing metric with statistics
     */
    private static class TimingMetric {
        private final String name;
        private long count = 0;
        private long sum = 0;
        private long min = Long.MAX_VALUE;
        private long max = 0;

        TimingMetric(String name) {
            this.name = name;
        }

        synchronized void record(long value) {
            count++;
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        synchronized TimingStatistics getStatistics() {
            TimingStatistics stats = new TimingStatistics();
            stats.count = count;
            stats.sum = sum;
            stats.min = (count > 0) ? min : 0;
            stats.max = max;
            stats.average = (count > 0) ? (double) sum / count : 0;
            return stats;
        }
    }

    /**
     * Timing statistics
     */
    public static class TimingStatistics {
        private long count;
        private long sum;
        private long min;
        private long max;
        private double average;

        public long getCount() {
            return count;
        }

        public long getSum() {
            return sum;
        }

        public long getMin() {
            return min;
        }

        public long getMax() {
            return max;
        }

        public double getAverage() {
            return average;
        }
    }

    /**
     * Metrics snapshot
     */
    public static class MetricsSnapshot {
        private Instant timestamp;
        private final Map<String, Long> counters = new ConcurrentHashMap<>();
        private final Map<String, Long> gauges = new ConcurrentHashMap<>();
        private final Map<String, TimingStatistics> timings = new ConcurrentHashMap<>();
        private SystemMetrics.SystemSnapshot systemMetrics;

        public Instant getTimestamp() {
            return timestamp;
        }

        public Map<String, Long> getCounters() {
            return new ConcurrentHashMap<>(counters);
        }

        public Map<String, Long> getGauges() {
            return new ConcurrentHashMap<>(gauges);
        }

        public Map<String, TimingStatistics> getTimings() {
            return new ConcurrentHashMap<>(timings);
        }

        public SystemMetrics.SystemSnapshot getSystemMetrics() {
            return systemMetrics;
        }
    }

    /**
     * Common metric names
     */
    public static class Metrics {
        // WebSocket metrics
        public static final String WS_CONNECTIONS_TOTAL = "websocket.connections.total";
        public static final String WS_CONNECTIONS_ACTIVE = "websocket.connections.active";
        public static final String WS_MESSAGES_RECEIVED = "websocket.messages.received";
        public static final String WS_MESSAGES_SENT = "websocket.messages.sent";
        public static final String WS_ERRORS = "websocket.errors";

        // Command metrics
        public static final String COMMANDS_EXECUTED = "commands.executed";
        public static final String COMMANDS_FAILED = "commands.failed";
        public static final String COMMAND_TIMING_PREFIX = "commands.timing.";

        // Block operations
        public static final String BLOCKS_PLACED = "blocks.placed";
        public static final String BLOCKS_BROKEN = "blocks.broken";
        public static final String BLOCKS_BATCH_OPERATIONS = "blocks.batch.operations";

        // Collaboration metrics
        public static final String INVITATIONS_SENT = "collaboration.invitations.sent";
        public static final String INVITATIONS_ACCEPTED = "collaboration.invitations.accepted";
        public static final String INVITATIONS_DECLINED = "collaboration.invitations.declined";
        public static final String VISITS_REQUESTED = "collaboration.visits.requested";
        public static final String VISITS_APPROVED = "collaboration.visits.approved";

        // Performance metrics
        public static final String CHUNK_UPDATES = "performance.chunk.updates";
        public static final String BATCH_OPERATIONS = "performance.batch.operations";
        public static final String CACHE_HITS = "performance.cache.hits";
        public static final String CACHE_MISSES = "performance.cache.misses";
    }
}
