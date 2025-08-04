package edu.minecraft.collaboration.core;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.monitoring.MetricsCollector;
import edu.minecraft.collaboration.util.ResourceCleanupManager;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * System-wide cache management and performance optimization
 */
public final class CacheManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static CacheManager instance;
    
    // Cache storage
    private final Map<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();
    private final Map<String, CacheStatistics> cacheStats = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;
    
    // Cache configuration
    private static final int DEFAULT_MAX_SIZE = 1000;
    private static final long DEFAULT_TTL_MINUTES = 30;
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    
    // Memory pressure thresholds
    private static final double MEMORY_PRESSURE_HIGH_THRESHOLD = 0.85;
    private static final double MEMORY_PRESSURE_CRITICAL_THRESHOLD = 0.95;
    private static final double MEMORY_PRESSURE_LOW_THRESHOLD = 0.7;
    
    // Memory monitoring
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private volatile double currentMemoryPressure = 0.0;
    private volatile MemoryPressureLevel memoryPressureLevel = MemoryPressureLevel.LOW;
    private final MetricsCollector metrics;
    
    private CacheManager() {
        this.metrics = DependencyInjector.getInstance().getService(MetricsCollector.class);
        // Use ResourceCleanupManager for executor
        ResourceCleanupManager cleanupManager = ResourceCleanupManager.getInstance();
        cleanupExecutor = cleanupManager.createManagedScheduledExecutor("CacheManager-Cleanup", 2);
        
        startCleanupScheduler();
        startMemoryMonitoring();
        LOGGER.info("Cache manager initialized with memory pressure handling");
    }
    
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * Create or get a cache with default settings
     */
    public Cache<String, Object> getCache(String cacheName) {
        return getCache(cacheName, DEFAULT_MAX_SIZE, DEFAULT_TTL_MINUTES);
    }
    
    /**
     * Create or get a cache with custom settings
     */
    public Cache<String, Object> getCache(String cacheName, int maxSize, long ttlMinutes) {
        return caches.computeIfAbsent(cacheName, name -> {
            Cache<String, Object> cache = new Cache<>(maxSize, ttlMinutes);
            cacheStats.put(name, new CacheStatistics());
            LOGGER.debug("Created cache: {} (maxSize={}, ttl={}min)", name, maxSize, ttlMinutes);
            return cache;
        });
    }
    
    /**
     * Remove a cache
     */
    public boolean removeCache(String cacheName) {
        Cache<String, Object> removed = caches.remove(cacheName);
        cacheStats.remove(cacheName);
        if (removed != null) {
            removed.clear();
            LOGGER.info("Removed cache: {}", cacheName);
            return true;
        }
        return false;
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, CacheStatistics> getAllCacheStatistics() {
        return new HashMap<>(cacheStats);
    }
    
    /**
     * Get statistics for a specific cache
     */
    public CacheStatistics getCacheStatistics(String cacheName) {
        return cacheStats.get(cacheName);
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        caches.values().forEach(Cache::clear);
        cacheStats.values().forEach(CacheStatistics::reset);
        LOGGER.info("Cleared all caches");
    }
    
    /**
     * Update cache statistics
     */
    public void recordCacheHit(String cacheName) {
        CacheStatistics stats = cacheStats.get(cacheName);
        if (stats != null) {
            stats.recordHit();
        }
        metrics.incrementCounter(MetricsCollector.Metrics.CACHE_HITS);
    }
    
    public void recordCacheMiss(String cacheName) {
        CacheStatistics stats = cacheStats.get(cacheName);
        if (stats != null) {
            stats.recordMiss();
        }
        metrics.incrementCounter(MetricsCollector.Metrics.CACHE_MISSES);
    }
    
    /**
     * Start cleanup scheduler
     */
    private void startCleanupScheduler() {
        cleanupExecutor.scheduleAtFixedRate(this::performCleanup, 
            CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
        
        cleanupExecutor.scheduleAtFixedRate(this::optimizeMemoryUsage, 
            10, 10, TimeUnit.MINUTES);
    }
    
    /**
     * Perform cleanup operations
     */
    private void performCleanup() {
        try {
            int totalCleaned = 0;
            for (Map.Entry<String, Cache<String, Object>> entry : caches.entrySet()) {
                String cacheName = entry.getKey();
                Cache<String, Object> cache = entry.getValue();
                
                int sizeBefore = cache.size();
                cache.cleanupExpired();
                int sizeAfter = cache.size();
                
                int cleaned = sizeBefore - sizeAfter;
                totalCleaned += cleaned;
                
                if (cleaned > 0) {
                    LOGGER.debug("Cleaned {} expired entries from cache: {}", cleaned, cacheName);
                }
            }
            
            if (totalCleaned > 0) {
                LOGGER.debug("Cache cleanup completed: {} total entries removed", totalCleaned);
            }
            
            adjustLowHitRateCaches();
        } catch (Exception e) {
            LOGGER.error("Error during cache cleanup", e);
        }
    }
    
    /**
     * Adjust low hit rate caches
     */
    private void adjustLowHitRateCaches() {
        for (Map.Entry<String, CacheStatistics> entry : cacheStats.entrySet()) {
            String cacheName = entry.getKey();
            CacheStatistics stats = entry.getValue();
            
            if (stats.getHitRate() < 10 && stats.getTotal() > 100) {
                LOGGER.warn("Cache {} has very low hit rate: {:.2f}% - consider reviewing cache strategy", 
                    cacheName, stats.getHitRate());
                
                // Partially clear low hit rate caches
                Cache<String, Object> cache = caches.get(cacheName);
                if (cache != null && cache.size() > 50) {
                    // Remove some old entries
                    cache.cleanupExpired();
                }
            }
        }
    }
    
    /**
     * Start memory monitoring
     */
    private void startMemoryMonitoring() {
        cleanupExecutor.scheduleAtFixedRate(this::updateMemoryPressure, 0, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Update current memory pressure
     */
    private void updateMemoryPressure() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        
        if (max > 0) {
            currentMemoryPressure = (double) used / max;
            
            // Update memory pressure level
            MemoryPressureLevel newLevel;
            if (currentMemoryPressure >= MEMORY_PRESSURE_CRITICAL_THRESHOLD) {
                newLevel = MemoryPressureLevel.CRITICAL;
            } else if (currentMemoryPressure >= MEMORY_PRESSURE_HIGH_THRESHOLD) {
                newLevel = MemoryPressureLevel.HIGH;
            } else if (currentMemoryPressure >= MEMORY_PRESSURE_LOW_THRESHOLD) {
                newLevel = MemoryPressureLevel.MEDIUM;
            } else {
                newLevel = MemoryPressureLevel.LOW;
            }
            
            if (newLevel != memoryPressureLevel) {
                LOGGER.info("Memory pressure level changed from {} to {} ({}% usage)", 
                    memoryPressureLevel, newLevel, String.format("%.1f", currentMemoryPressure * 100));
                memoryPressureLevel = newLevel;
                
                // Update metrics
                metrics.setGauge("cache.memory.pressure", (long) (currentMemoryPressure * 100));
                metrics.incrementCounter("cache.memory.pressure." + newLevel.name().toLowerCase());
                
                handleMemoryPressureChange();
            }
        }
    }
    
    /**
     * Handle memory pressure level changes
     */
    private void handleMemoryPressureChange() {
        switch (memoryPressureLevel) {
            case CRITICAL:
                handleCriticalMemoryPressure();
                break;
            case HIGH:
                handleHighMemoryPressure();
                break;
            case MEDIUM:
                handleMediumMemoryPressure();
                break;
            case LOW:
                // Normal operation
                break;
            default:
                LOGGER.warn("Unknown memory pressure level: {}", memoryPressureLevel);
                break;
        }
    }
    
    /**
     * Handle critical memory pressure
     */
    private void handleCriticalMemoryPressure() {
        LOGGER.warn("CRITICAL memory pressure detected! Aggressively clearing caches");
        
        // Clear all caches except essential ones
        int totalCleared = 0;
        for (Map.Entry<String, Cache<String, Object>> entry : caches.entrySet()) {
            String cacheName = entry.getKey();
            Cache<String, Object> cache = entry.getValue();
            
            // Keep only essential caches
            if (!isEssentialCache(cacheName)) {
                int sizeBefore = cache.size();
                cache.clear();
                totalCleared += sizeBefore;
                LOGGER.info("Cleared cache {} ({} entries) due to critical memory pressure", 
                    cacheName, sizeBefore);
            }
        }
        
        // Force garbage collection
        System.gc();
        
        metrics.incrementCounter("cache.evictions.critical", totalCleared);
        LOGGER.info("Cleared {} total cache entries due to critical memory pressure", totalCleared);
    }
    
    /**
     * Handle high memory pressure
     */
    private void handleHighMemoryPressure() {
        LOGGER.warn("HIGH memory pressure detected! Clearing low-priority caches");
        
        // Sort caches by hit rate and clear the lowest performers
        List<Map.Entry<String, CacheStatistics>> sortedStats = new ArrayList<>(cacheStats.entrySet());
        sortedStats.sort(Comparator.comparingDouble(e -> e.getValue().getHitRate()));
        
        int targetCaches = Math.max(1, sortedStats.size() / 3);
        int totalCleared = 0;
        
        for (int i = 0; i < targetCaches && i < sortedStats.size(); i++) {
            String cacheName = sortedStats.get(i).getKey();
            Cache<String, Object> cache = caches.get(cacheName);
            
            if (cache != null && !isEssentialCache(cacheName)) {
                int sizeBefore = cache.size();
                cache.clear();
                totalCleared += sizeBefore;
                LOGGER.info("Cleared low-hit-rate cache {} ({} entries, {:.1f}% hit rate)", 
                    cacheName, sizeBefore, sortedStats.get(i).getValue().getHitRate());
            }
        }
        
        metrics.incrementCounter("cache.evictions.high", totalCleared);
        LOGGER.info("Cleared {} total cache entries due to high memory pressure", totalCleared);
    }
    
    /**
     * Handle medium memory pressure
     */
    private void handleMediumMemoryPressure() {
        LOGGER.info("MEDIUM memory pressure detected. Evicting expired entries");
        
        // Clean up expired entries more aggressively
        int totalCleaned = 0;
        for (Map.Entry<String, Cache<String, Object>> entry : caches.entrySet()) {
            Cache<String, Object> cache = entry.getValue();
            int sizeBefore = cache.size();
            cache.cleanupExpired();
            
            // Also evict LRU entries if cache is large
            if (cache.size() > 100) {
                cache.evictLRU(cache.size() / 4); // Evict 25% of entries
            }
            
            int cleaned = sizeBefore - cache.size();
            totalCleaned += cleaned;
        }
        
        metrics.incrementCounter("cache.evictions.medium", totalCleaned);
        LOGGER.info("Cleaned {} cache entries due to medium memory pressure", totalCleaned);
    }
    
    /**
     * Check if a cache is essential and should not be cleared
     */
    private boolean isEssentialCache(String cacheName) {
        // Define essential caches that should be preserved
        return cacheName.equals("player_data") 
                || cacheName.equals("authentication")
                || cacheName.equals("websocket_connections");
    }
    
    /**
     * Optimize memory usage
     */
    private void optimizeMemoryUsage() {
        // This method is now called periodically to perform general optimization
        updateMemoryPressure();
        
        // Adjust cache sizes based on memory pressure
        if (memoryPressureLevel == MemoryPressureLevel.HIGH 
                || memoryPressureLevel == MemoryPressureLevel.CRITICAL) {
            adjustCacheSizes();
        }
    }
    
    /**
     * Adjust cache sizes based on memory pressure
     */
    private void adjustCacheSizes() {
        for (Map.Entry<String, Cache<String, Object>> entry : caches.entrySet()) {
            String cacheName = entry.getKey();
            Cache<String, Object> cache = entry.getValue();
            
            // Reduce cache max size under memory pressure
            int newMaxSize = (int) (cache.maxSize * (1 - currentMemoryPressure / 2));
            newMaxSize = Math.max(10, newMaxSize); // Keep minimum size
            
            if (newMaxSize < cache.maxSize) {
                cache.setMaxSize(newMaxSize);
                LOGGER.debug("Reduced max size for cache {} from {} to {} due to memory pressure", 
                    cacheName, cache.maxSize, newMaxSize);
            }
        }
    }
    
    /**
     * Shutdown the cache manager
     */
    public void shutdown() {
        LOGGER.info("Shutting down cache manager...");
        clearAllCaches();
        // Executor will be shut down by ResourceCleanupManager
        LOGGER.info("Cache manager shutdown completed");
    }
    
    /**
     * Cache implementation
     */
    public static class Cache<K, V> {
        private final Map<K, CacheEntry<V>> data = new ConcurrentHashMap<>();
        private int maxSize;
        private final long ttlMinutes;
        
        public Cache(int maxSize, long ttlMinutes) {
            this.maxSize = maxSize;
            this.ttlMinutes = ttlMinutes;
        }
        
        public V get(K key) {
            CacheEntry<V> entry = data.get(key);
            if (entry == null || entry.isExpired()) {
                data.remove(key);
                return null;
            }
            entry.updateAccessTime();
            return entry.getValue();
        }
        
        public void put(K key, V value) {
            if (data.size() >= maxSize) {
                evictOldest();
            }
            data.put(key, new CacheEntry<>(value, ttlMinutes));
        }
        
        public void remove(K key) {
            data.remove(key);
        }
        
        public void clear() {
            data.clear();
        }
        
        public int size() {
            return data.size();
        }
        
        public void cleanupExpired() {
            data.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
        
        public void setMaxSize(int newMaxSize) {
            this.maxSize = newMaxSize;
            // Evict entries if necessary
            while (data.size() > maxSize) {
                evictOldest();
            }
        }
        
        public void evictLRU(int count) {
            if (count <= 0 || data.isEmpty()) {
                return;
            }
            
            // Sort entries by last access time
            List<Map.Entry<K, CacheEntry<V>>> entries = new ArrayList<>(data.entrySet());
            entries.sort(Comparator.comparing(e -> e.getValue().lastAccessTime));
            
            // Remove least recently used entries
            int toRemove = Math.min(count, entries.size());
            for (int i = 0; i < toRemove; i++) {
                data.remove(entries.get(i).getKey());
            }
        }
        
        private void evictOldest() {
            Optional<Map.Entry<K, CacheEntry<V>>> oldest = data.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().getCreateTime()));
            oldest.ifPresent(entry -> data.remove(entry.getKey()));
        }
    }
    
    /**
     * Cache entry with expiration
     */
    private static class CacheEntry<V> {
        private final V value;
        private final LocalDateTime createTime;
        private final long ttlMinutes;
        private LocalDateTime lastAccessTime;
        
        CacheEntry(V value, long ttlMinutes) {
            this.value = value;
            this.createTime = LocalDateTime.now();
            this.lastAccessTime = this.createTime;
            this.ttlMinutes = ttlMinutes;
        }
        
        public V getValue() {
            return value;
        }
        
        public LocalDateTime getCreateTime() {
            return createTime;
        }
        
        public void updateAccessTime() {
            this.lastAccessTime = LocalDateTime.now();
        }
        
        public boolean isExpired() {
            return ChronoUnit.MINUTES.between(createTime, LocalDateTime.now()) > ttlMinutes;
        }
    }
    
    /**
     * Cache statistics
     */
    public static class CacheStatistics {
        private long hits = 0;
        private long misses = 0;
        private final LocalDateTime createdTime = LocalDateTime.now();
        
        public synchronized void recordHit() {
            hits++;
        }
        
        public synchronized void recordMiss() {
            misses++;
        }
        
        public synchronized void reset() {
            hits = 0;
            misses = 0;
        }
        
        public synchronized long getHits() {
            return hits;
        }
        
        public synchronized long getMisses() {
            return misses;
        }
        
        public synchronized long getTotal() {
            return hits + misses;
        }
        
        public synchronized double getHitRate() {
            long total = getTotal();
            return total == 0 ? 0.0 : (double) hits / total * 100.0;
        }
        
        public LocalDateTime getCreatedTime() {
            return createdTime;
        }
        
        @Override
        public synchronized String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, total=%d}", 
                hits, misses, getHitRate(), getTotal());
        }
    }
    
    /**
     * Memory pressure levels
     */
    public enum MemoryPressureLevel {
        LOW,      // < 70% memory usage
        MEDIUM,   // 70-85% memory usage
        HIGH,     // 85-95% memory usage
        CRITICAL  // > 95% memory usage
    }
    
    /**
     * Get current memory pressure information
     */
    public MemoryPressureInfo getMemoryPressureInfo() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return new MemoryPressureInfo(
            currentMemoryPressure,
            memoryPressureLevel,
            heapUsage.getUsed() / (1024 * 1024),
            heapUsage.getMax() / (1024 * 1024),
            getTotalCacheEntries()
        );
    }
    
    /**
     * Get total number of cached entries
     */
    private int getTotalCacheEntries() {
        return caches.values().stream()
            .mapToInt(Cache::size)
            .sum();
    }
    
    /**
     * Memory pressure information
     */
    public static class MemoryPressureInfo {
        private final double pressure;
        private final MemoryPressureLevel level;
        private final long usedMemoryMB;
        private final long maxMemoryMB;
        private final int totalCacheEntries;
        
        public MemoryPressureInfo(double pressure, MemoryPressureLevel level, 
                                 long usedMemoryMB, long maxMemoryMB, int totalCacheEntries) {
            this.pressure = pressure;
            this.level = level;
            this.usedMemoryMB = usedMemoryMB;
            this.maxMemoryMB = maxMemoryMB;
            this.totalCacheEntries = totalCacheEntries;
        }
        
        public double getPressure() {
            return pressure;
        }
        
        public MemoryPressureLevel getLevel() {
            return level;
        }
        
        public long getUsedMemoryMB() {
            return usedMemoryMB;
        }
        
        public long getMaxMemoryMB() {
            return maxMemoryMB;
        }
        
        public int getTotalCacheEntries() {
            return totalCacheEntries;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryPressure{level=%s, usage=%.1f%%, memory=%dMB/%dMB, cacheEntries=%d}",
                level, pressure * 100, usedMemoryMB, maxMemoryMB, totalCacheEntries);
        }
    }
}