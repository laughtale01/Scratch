package edu.minecraft.collaboration.core;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * System-wide cache management and performance optimization
 */
public class CacheManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static CacheManager instance;
    
    // Cache storage
    private final Map<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();
    private final Map<String, CacheStatistics> cacheStats = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(2);
    
    // Cache configuration
    private static final int DEFAULT_MAX_SIZE = 1000;
    private static final long DEFAULT_TTL_MINUTES = 30;
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    
    private CacheManager() {
        startCleanupScheduler();
        LOGGER.info("Cache manager initialized");
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
    }
    
    public void recordCacheMiss(String cacheName) {
        CacheStatistics stats = cacheStats.get(cacheName);
        if (stats != null) {
            stats.recordMiss();
        }
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
     * Optimize memory usage
     */
    private void optimizeMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // If memory usage is high, be more aggressive with cache cleanup
        if (usedMemory > totalMemory * 0.8) {
            LOGGER.warn("High memory usage detected: {}MB / {}MB", 
                usedMemory / (1024 * 1024), totalMemory / (1024 * 1024));
            
            // Clear caches with low hit rates
            for (Map.Entry<String, CacheStatistics> entry : cacheStats.entrySet()) {
                String cacheName = entry.getKey();
                CacheStatistics stats = entry.getValue();
                
                if (stats.getHitRate() < 30) {
                    caches.get(cacheName).clear();
                    LOGGER.info("Cleared low-hit-rate cache {} to free memory", cacheName);
                }
            }
        }
    }
    
    /**
     * Shutdown the cache manager
     */
    public void shutdown() {
        LOGGER.info("Shutting down cache manager...");
        try {
            cleanupExecutor.shutdown();
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        clearAllCaches();
        LOGGER.info("Cache manager shutdown completed");
    }
    
    /**
     * Cache implementation
     */
    public static class Cache<K, V> {
        private final Map<K, CacheEntry<V>> data = new ConcurrentHashMap<>();
        private final int maxSize;
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
        
        public CacheEntry(V value, long ttlMinutes) {
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
}