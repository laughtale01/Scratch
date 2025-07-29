package com.yourname.minecraftcollaboration.core;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * システム全体のキャッシュ管理とパフォーマンス最適化
 */
public class CacheManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static CacheManager instance;
    
    // キャッシュストレージ
    private final Map<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 統計情報
    private long totalCacheHits = 0;
    private long totalCacheMisses = 0;
    private final Map<String, CacheStatistics> cacheStats = new ConcurrentHashMap<>();
    
    // 設定
    private static final long DEFAULT_TTL_MINUTES = 15;
    private static final int DEFAULT_MAX_SIZE = 1000;
    
    private CacheManager() {
        initializeDefaultCaches();
        startCleanupTask();
    }
    
    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * デフォルトキャッシュの初期化
     */
    private void initializeDefaultCaches() {
        // プレイヤー情報キャッシュ
        createCache("player_data", 30, 500);
        
        // 言語設定キャッシュ
        createCache("language_settings", 60, 200);
        
        // ブロックパック情報キャッシュ
        createCache("block_packs", 120, 100);
        
        // 学習進捗キャッシュ
        createCache("progress_data", 10, 1000);
        
        // コラボレーション情報キャッシュ
        createCache("collaboration_data", 5, 300);
        
        // コマンド結果キャッシュ
        createCache("command_results", 2, 500);
        
        // エージェント状態キャッシュ
        createCache("agent_states", 15, 200);
        
        LOGGER.info("Initialized {} default caches", caches.size());
    }
    
    /**
     * キャッシュの作成
     */
    public void createCache(String cacheName, long ttlMinutes, int maxSize) {
        Cache<String, Object> cache = new Cache<>(ttlMinutes, maxSize);
        caches.put(cacheName, cache);
        cacheStats.put(cacheName, new CacheStatistics(cacheName));
        
        LOGGER.debug("Created cache: {} (TTL: {}min, MaxSize: {})", cacheName, ttlMinutes, maxSize);
    }
    
    /**
     * キャッシュへの書き込み
     */
    public void put(String cacheName, String key, Object value) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.put(key, value);
            
            CacheStatistics stats = cacheStats.get(cacheName);
            if (stats != null) {
                stats.incrementWrites();
            }
        } else {
            LOGGER.warn("Cache not found: {}", cacheName);
        }
    }
    
    /**
     * キャッシュからの読み込み
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String cacheName, String key) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            Optional<Object> result = cache.get(key);
            
            CacheStatistics stats = cacheStats.get(cacheName);
            if (stats != null) {
                if (result.isPresent()) {
                    stats.incrementHits();
                    totalCacheHits++;
                } else {
                    stats.incrementMisses();
                    totalCacheMisses++;
                }
            }
            
            return result.map(value -> (T) value);
        } else {
            LOGGER.warn("Cache not found: {}", cacheName);
            return Optional.empty();
        }
    }
    
    /**
     * キャッシュからの削除
     */
    public void remove(String cacheName, String key) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.remove(key);
            
            CacheStatistics stats = cacheStats.get(cacheName);
            if (stats != null) {
                stats.incrementRemovals();
            }
        }
    }
    
    /**
     * キャッシュのクリア
     */
    public void clear(String cacheName) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            int clearedItems = cache.size();
            cache.clear();
            
            CacheStatistics stats = cacheStats.get(cacheName);
            if (stats != null) {
                stats.addCleared(clearedItems);
            }
            
            LOGGER.debug("Cleared cache: {} ({} items)", cacheName, clearedItems);
        }
    }
    
    /**
     * 全キャッシュのクリア
     */
    public void clearAll() {
        for (String cacheName : caches.keySet()) {
            clear(cacheName);
        }
        LOGGER.info("Cleared all caches");
    }
    
    /**
     * 期限切れエントリのクリーンアップ
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
        LOGGER.debug("Started cache cleanup task");
    }
    
    /**
     * 期限切れエントリの削除
     */
    private void cleanupExpiredEntries() {
        int totalCleaned = 0;
        
        for (Map.Entry<String, Cache<String, Object>> entry : caches.entrySet()) {
            String cacheName = entry.getKey();
            Cache<String, Object> cache = entry.getValue();
            
            int cleaned = cache.cleanupExpired();
            totalCleaned += cleaned;
            
            if (cleaned > 0) {
                CacheStatistics stats = cacheStats.get(cacheName);
                if (stats != null) {
                    stats.addExpired(cleaned);
                }
                
                LOGGER.debug("Cleaned {} expired entries from cache: {}", cleaned, cacheName);
            }
        }
        
        if (totalCleaned > 0) {
            LOGGER.debug("Total cleaned expired entries: {}", totalCleaned);
        }
    }
    
    /**
     * キャッシュ統計の取得
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 全体統計
        stats.put("totalCacheHits", totalCacheHits);
        stats.put("totalCacheMisses", totalCacheMisses);
        
        double hitRate = totalCacheHits + totalCacheMisses > 0 ? 
            (double) totalCacheHits / (totalCacheHits + totalCacheMisses) * 100 : 0;
        stats.put("overallHitRate", hitRate);
        
        // キャッシュ別統計
        Map<String, Object> cacheDetails = new HashMap<>();
        for (Map.Entry<String, Cache<String, Object>> entry : caches.entrySet()) {
            String cacheName = entry.getKey();
            Cache<String, Object> cache = entry.getValue();
            CacheStatistics cacheStats = this.cacheStats.get(cacheName);
            
            Map<String, Object> cacheInfo = new HashMap<>();
            cacheInfo.put("size", cache.size());
            cacheInfo.put("maxSize", cache.getMaxSize());
            cacheInfo.put("ttlMinutes", cache.getTtlMinutes());
            
            if (cacheStats != null) {
                cacheInfo.put("hits", cacheStats.getHits());
                cacheInfo.put("misses", cacheStats.getMisses());
                cacheInfo.put("writes", cacheStats.getWrites());
                cacheInfo.put("removals", cacheStats.getRemovals());
                cacheInfo.put("expired", cacheStats.getExpired());
                cacheInfo.put("hitRate", cacheStats.getHitRate());
            }
            
            cacheDetails.put(cacheName, cacheInfo);
        }
        stats.put("cacheDetails", cacheDetails);
        
        return stats;
    }
    
    /**
     * パフォーマンス最適化の実行
     */
    public void optimizePerformance() {
        LOGGER.info("Starting performance optimization...");
        
        // キャッシュサイズの最適化
        optimizeCacheSizes();
        
        // 低ヒット率キャッシュの調整
        adjustLowHitRateCaches();
        
        // メモリ使用量の最適化
        optimizeMemoryUsage();
        
        LOGGER.info("Performance optimization completed");
    }
    
    /**
     * キャッシュサイズの最適化
     */
    private void optimizeCacheSizes() {
        for (Map.Entry<String, Cache<String, Object>> entry : caches.entrySet()) {
            String cacheName = entry.getKey();
            Cache<String, Object> cache = entry.getValue();
            CacheStatistics stats = cacheStats.get(cacheName);
            
            if (stats != null) {
                // ヒット率が高く、キャッシュが満杯に近い場合はサイズを増加
                if (stats.getHitRate() > 80 && cache.size() > cache.getMaxSize() * 0.9) {
                    int newSize = (int) (cache.getMaxSize() * 1.2);
                    LOGGER.debug("Increasing cache size for {}: {} -> {}", cacheName, cache.getMaxSize(), newSize);
                    // cache.setMaxSize(newSize); // 実装があれば
                }
                
                // ヒット率が低く、キャッシュがあまり使われていない場合はサイズを減少
                if (stats.getHitRate() < 20 && cache.size() < cache.getMaxSize() * 0.3) {
                    int newSize = Math.max(50, (int) (cache.getMaxSize() * 0.8));
                    LOGGER.debug("Decreasing cache size for {}: {} -> {}", cacheName, cache.getMaxSize(), newSize);
                    // cache.setMaxSize(newSize); // 実装があれば
                }
            }
        }
    }
    
    /**
     * 低ヒット率キャッシュの調整
     */
    private void adjustLowHitRateCaches() {
        for (Map.Entry<String, CacheStatistics> entry : cacheStats.entrySet()) {
            String cacheName = entry.getKey();
            CacheStatistics stats = entry.getValue();
            
            if (stats.getHitRate() < 10 && stats.getTotal() > 100) {
                LOGGER.warn("Cache {} has very low hit rate: {:.2f}% - consider reviewing cache strategy", 
                    cacheName, stats.getHitRate());
                
                // 低ヒット率キャッシュの一部クリア
                Cache<String, Object> cache = caches.get(cacheName);
                if (cache != null && cache.size() > 50) {
                    // 古いエントリの一部を削除
                    cache.cleanupExpired();
                }
            }
        }
    }
    
    /**
     * メモリ使用量の最適化
     */
    private void optimizeMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // メモリ使用率が高い場合
        if (usedMemory > totalMemory * 0.8) {
            LOGGER.warn("High memory usage detected: {}MB / {}MB", 
                usedMemory / 1024 / 1024, totalMemory / 1024 / 1024);
            
            // 低ヒット率キャッシュを積極的にクリア
            for (Map.Entry<String, CacheStatistics> entry : cacheStats.entrySet()) {
                String cacheName = entry.getKey();
                CacheStatistics stats = entry.getValue();
                
                if (stats.getHitRate() < 30) {
                    clear(cacheName);
                    LOGGER.info("Cleared low-hit-rate cache {} to free memory", cacheName);
                }
            }
            
            // ガベージコレクションの実行
            System.gc();
        }
    }
    
    /**
     * システム終了時の処理
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        clearAll();
        LOGGER.info("Cache manager shutdown completed");
    }
    
    // === 内部クラス ===
    
    /**
     * キャッシュエントリ
     */
    private static class CacheEntry<T> {
        private final T value;
        private final LocalDateTime expiry;
        
        public CacheEntry(T value, LocalDateTime expiry) {
            this.value = value;
            this.expiry = expiry;
        }
        
        public T getValue() { return value; }
        public LocalDateTime getExpiry() { return expiry; }
        public boolean isExpired() { return LocalDateTime.now().isAfter(expiry); }
    }
    
    /**
     * キャッシュ実装
     */
    private static class Cache<K, V> {
        private final Map<K, CacheEntry<V>> storage = new ConcurrentHashMap<>();
        private final long ttlMinutes;
        private final int maxSize;
        
        public Cache(long ttlMinutes, int maxSize) {
            this.ttlMinutes = ttlMinutes;
            this.maxSize = maxSize;
        }
        
        public void put(K key, V value) {
            LocalDateTime expiry = LocalDateTime.now().plus(ttlMinutes, ChronoUnit.MINUTES);
            
            // サイズ制限チェック
            if (storage.size() >= maxSize) {
                // LRU的な削除（簡易実装）
                K oldestKey = storage.keySet().iterator().next();
                storage.remove(oldestKey);
            }
            
            storage.put(key, new CacheEntry<>(value, expiry));
        }
        
        public Optional<V> get(K key) {
            CacheEntry<V> entry = storage.get(key);
            if (entry != null && !entry.isExpired()) {
                return Optional.of(entry.getValue());
            } else if (entry != null) {
                storage.remove(key); // 期限切れエントリを削除
            }
            return Optional.empty();
        }
        
        public void remove(K key) {
            storage.remove(key);
        }
        
        public void clear() {
            storage.clear();
        }
        
        public int size() {
            return storage.size();
        }
        
        public int cleanupExpired() {
            List<K> expiredKeys = new ArrayList<>();
            for (Map.Entry<K, CacheEntry<V>> entry : storage.entrySet()) {
                if (entry.getValue().isExpired()) {
                    expiredKeys.add(entry.getKey());
                }
            }
            
            for (K key : expiredKeys) {
                storage.remove(key);
            }
            
            return expiredKeys.size();
        }
        
        public long getTtlMinutes() { return ttlMinutes; }
        public int getMaxSize() { return maxSize; }
    }
    
    /**
     * キャッシュ統計
     */
    private static class CacheStatistics {
        private final String cacheName;
        private long hits = 0;
        private long misses = 0;
        private long writes = 0;
        private long removals = 0;
        private long expired = 0;
        private long cleared = 0;
        
        public CacheStatistics(String cacheName) {
            this.cacheName = cacheName;
        }
        
        public synchronized void incrementHits() { hits++; }
        public synchronized void incrementMisses() { misses++; }
        public synchronized void incrementWrites() { writes++; }
        public synchronized void incrementRemovals() { removals++; }
        public synchronized void addExpired(int count) { expired += count; }
        public synchronized void addCleared(int count) { cleared += count; }
        
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getWrites() { return writes; }
        public long getRemovals() { return removals; }
        public long getExpired() { return expired; }
        public long getCleared() { return cleared; }
        public long getTotal() { return hits + misses; }
        
        public double getHitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total * 100 : 0;
        }
        
        public String getCacheName() { return cacheName; }
    }
}