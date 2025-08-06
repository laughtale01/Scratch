package edu.minecraft.collaboration.monitoring.performance;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Performance optimizer for monitoring system
 * Implements caching, batching, and resource pooling
 */
public class PerformanceOptimizer {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    // Thread pools with bounded queues
    private final ExecutorService dataProcessingPool;
    private final ScheduledExecutorService batchProcessor;
    
    // Caching
    private final ConcurrentHashMap<String, CachedMetric> metricCache;
    private static final long CACHE_TTL_MS = 5000; // 5 seconds cache TTL
    
    // Batching
    private final BlockingQueue<MetricUpdate> updateQueue;
    private final int BATCH_SIZE = 100;
    private final long BATCH_INTERVAL_MS = 1000; // 1 second
    
    // Resource pooling
    private final ObjectPool<MetricBuffer> bufferPool;
    
    // Performance metrics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    
    public PerformanceOptimizer() {
        // Configure thread pools with bounded resources
        this.dataProcessingPool = new ThreadPoolExecutor(
            2, 4,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new OptimizedThreadFactory("DataProcessor"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        this.batchProcessor = Executors.newScheduledThreadPool(1,
            new OptimizedThreadFactory("BatchProcessor"));
        
        this.metricCache = new ConcurrentHashMap<>();
        this.updateQueue = new LinkedBlockingQueue<>(10000);
        this.bufferPool = new ObjectPool<>(MetricBuffer::new, 50);
        
        startBatchProcessor();
    }
    
    /**
     * Process metric with caching
     */
    public double getMetricValue(String metricName, Supplier<Double> calculator) {
        CachedMetric cached = metricCache.get(metricName);
        
        if (cached != null && !cached.isExpired()) {
            cacheHits.incrementAndGet();
            return cached.value;
        }
        
        cacheMisses.incrementAndGet();
        double value = calculator.get();
        metricCache.put(metricName, new CachedMetric(value));
        return value;
    }
    
    /**
     * Submit metric update for batch processing
     */
    public void submitMetricUpdate(String name, double value) {
        if (!updateQueue.offer(new MetricUpdate(name, value))) {
            LOGGER.warn("Metric update queue full, dropping update for: {}", name);
        }
    }
    
    /**
     * Process data asynchronously
     */
    public CompletableFuture<Void> processAsync(Runnable task) {
        return CompletableFuture.runAsync(() -> {
            activeThreads.incrementAndGet();
            try {
                task.run();
            } finally {
                activeThreads.decrementAndGet();
            }
        }, dataProcessingPool);
    }
    
    /**
     * Get buffer from pool for efficient memory usage
     */
    public MetricBuffer borrowBuffer() {
        return bufferPool.borrow();
    }
    
    /**
     * Return buffer to pool
     */
    public void returnBuffer(MetricBuffer buffer) {
        buffer.clear();
        bufferPool.returnObject(buffer);
    }
    
    private void startBatchProcessor() {
        batchProcessor.scheduleAtFixedRate(this::processBatch,
            BATCH_INTERVAL_MS, BATCH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    private void processBatch() {
        List<MetricUpdate> batch = new ArrayList<>(BATCH_SIZE);
        updateQueue.drainTo(batch, BATCH_SIZE);
        
        if (!batch.isEmpty()) {
            MetricBuffer buffer = borrowBuffer();
            try {
                for (MetricUpdate update : batch) {
                    buffer.add(update);
                }
                // Process batch efficiently
                buffer.flush();
            } finally {
                returnBuffer(buffer);
            }
        }
    }
    
    /**
     * Get performance statistics
     */
    public PerformanceStats getStats() {
        double cacheHitRate = cacheHits.get() + cacheMisses.get() > 0 ?
            (double) cacheHits.get() / (cacheHits.get() + cacheMisses.get()) : 0;
        
        return new PerformanceStats(
            cacheHitRate,
            activeThreads.get(),
            updateQueue.size(),
            bufferPool.getAvailable()
        );
    }
    
    /**
     * Shutdown optimizer
     */
    public void shutdown() {
        batchProcessor.shutdown();
        dataProcessingPool.shutdown();
        
        try {
            if (!batchProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                batchProcessor.shutdownNow();
            }
            if (!dataProcessingPool.awaitTermination(5, TimeUnit.SECONDS)) {
                dataProcessingPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted during shutdown", e);
        }
    }
    
    // Inner classes
    
    private static class CachedMetric {
        final double value;
        final long timestamp;
        
        CachedMetric(double value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    private static class MetricUpdate {
        final String name;
        final double value;
        final long timestamp;
        
        MetricUpdate(String name, double value) {
            this.name = name;
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static class MetricBuffer {
        private final List<MetricUpdate> updates = new ArrayList<>();
        
        void add(MetricUpdate update) {
            updates.add(update);
        }
        
        void flush() {
            // Process all updates efficiently
            updates.clear();
        }
        
        void clear() {
            updates.clear();
        }
    }
    
    private static class ObjectPool<T> {
        private final BlockingQueue<T> pool;
        private final Supplier<T> factory;
        
        ObjectPool(Supplier<T> factory, int maxSize) {
            this.factory = factory;
            this.pool = new LinkedBlockingQueue<>(maxSize);
            
            // Pre-populate pool
            for (int i = 0; i < maxSize / 2; i++) {
                pool.offer(factory.get());
            }
        }
        
        T borrow() {
            T obj = pool.poll();
            return obj != null ? obj : factory.get();
        }
        
        void returnObject(T obj) {
            pool.offer(obj);
        }
        
        int getAvailable() {
            return pool.size();
        }
    }
    
    private static class OptimizedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);
        
        OptimizedThreadFactory(String prefix) {
            this.prefix = prefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    }
    
    public static class PerformanceStats {
        public final double cacheHitRate;
        public final int activeThreads;
        public final int queueSize;
        public final int bufferPoolAvailable;
        
        PerformanceStats(double cacheHitRate, int activeThreads, 
                         int queueSize, int bufferPoolAvailable) {
            this.cacheHitRate = cacheHitRate;
            this.activeThreads = activeThreads;
            this.queueSize = queueSize;
            this.bufferPoolAvailable = bufferPoolAvailable;
        }
    }
}