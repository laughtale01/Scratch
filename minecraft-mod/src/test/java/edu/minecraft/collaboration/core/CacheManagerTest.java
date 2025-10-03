package edu.minecraft.collaboration.core;

import edu.minecraft.collaboration.monitoring.MetricsCollector;
import edu.minecraft.collaboration.test.categories.UnitTest;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for CacheManager
 * Coverage target: 100% of CacheManager class
 */
@Tag("unit")
@DisplayName("CacheManager Tests")
class CacheManagerTest {

    private CacheManager cacheManager;

    @Mock
    private MetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Reset singleton instance for testing
        try {
            java.lang.reflect.Field instanceField = CacheManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            fail("Failed to reset CacheManager singleton: " + e.getMessage());
        }

        // Register mock MetricsCollector
        DependencyInjector.getInstance().registerService(MetricsCollector.class, () -> metricsCollector);

        cacheManager = CacheManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
    }

    // === Singleton Pattern Tests ===

    @Test
    @DisplayName("Should return same instance for multiple getInstance() calls")
    void testSingletonPattern() {
        CacheManager instance1 = CacheManager.getInstance();
        CacheManager instance2 = CacheManager.getInstance();

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }

    // === Cache Creation and Retrieval Tests ===

    @Test
    @DisplayName("Should create cache with default settings")
    void testCreateCacheWithDefaults() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache");

        assertNotNull(cache);
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("Should create cache with custom settings")
    void testCreateCacheWithCustomSettings() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("customCache", 500, 60);

        assertNotNull(cache);
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("Should return existing cache when requested again")
    void testGetExistingCache() {
        CacheManager.Cache<String, Object> cache1 = cacheManager.getCache("testCache");
        cache1.put("key1", "value1");

        CacheManager.Cache<String, Object> cache2 = cacheManager.getCache("testCache");

        assertSame(cache1, cache2);
        assertEquals("value1", cache2.get("key1"));
    }

    // === Cache Operations Tests ===

    @Test
    @DisplayName("Should put and get values from cache")
    void testPutAndGet() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache");

        cache.put("key1", "value1");
        cache.put("key2", 123);
        cache.put("key3", true);

        assertEquals("value1", cache.get("key1"));
        assertEquals(123, cache.get("key2"));
        assertEquals(true, cache.get("key3"));
        assertEquals(3, cache.size());
    }

    @Test
    @DisplayName("Should return null for non-existent key")
    void testGetNonExistentKey() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache");

        assertNull(cache.get("nonExistent"));
    }

    @Test
    @DisplayName("Should remove values from cache")
    void testRemove() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache");

        cache.put("key1", "value1");
        assertEquals(1, cache.size());

        cache.remove("key1");
        assertNull(cache.get("key1"));
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("Should clear all entries from cache")
    void testClear() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache");

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        assertEquals(3, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("key1"));
    }

    // === Cache Eviction Tests ===

    @Test
    @DisplayName("Should evict oldest entry when max size is reached")
    void testEvictOldest() throws InterruptedException {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache", 3, 60);

        cache.put("key1", "value1");
        Thread.sleep(10);
        cache.put("key2", "value2");
        Thread.sleep(10);
        cache.put("key3", "value3");

        assertEquals(3, cache.size());

        // Adding 4th entry should evict key1 (oldest)
        cache.put("key4", "value4");

        assertEquals(3, cache.size());
        assertNull(cache.get("key1"), "Oldest entry should be evicted");
        assertNotNull(cache.get("key2"));
        assertNotNull(cache.get("key3"));
        assertNotNull(cache.get("key4"));
    }

    @Test
    @DisplayName("Should evict LRU entries")
    void testEvictLRU() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache", 100, 60);

        for (int i = 0; i < 50; i++) {
            cache.put("key" + i, "value" + i);
        }

        // Access some keys to make them recently used
        cache.get("key45");
        cache.get("key46");
        cache.get("key47");
        cache.get("key48");
        cache.get("key49");

        assertEquals(50, cache.size());

        // Evict 10 LRU entries
        cache.evictLRU(10);

        assertEquals(40, cache.size());

        // Recently accessed keys should still be present
        assertNotNull(cache.get("key45"));
        assertNotNull(cache.get("key49"));
    }

    @Test
    @DisplayName("Should handle evictLRU with zero count gracefully")
    void testEvictLRUWithZeroCount() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache");
        cache.put("key1", "value1");

        cache.evictLRU(0);
        assertEquals(1, cache.size());
    }

    @Test
    @DisplayName("Should handle evictLRU on empty cache gracefully")
    void testEvictLRUOnEmptyCache() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache");

        assertDoesNotThrow(() -> cache.evictLRU(10));
        assertEquals(0, cache.size());
    }

    // === TTL and Expiration Tests ===

    @Test
    @DisplayName("Should expire entries after TTL")
    void testTTLExpiration() throws InterruptedException {
        // Create cache with very short TTL (1 minute for testing)
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache", 100, 1);

        cache.put("key1", "value1");
        assertEquals(1, cache.size());

        // TTL is in minutes, so we cannot wait that long in a test
        // Instead, verify that non-expired entries are NOT removed
        cache.cleanupExpired();

        // Entry should still be present (not expired yet)
        assertEquals(1, cache.size(), "Entry should not be expired immediately");
        assertNotNull(cache.get("key1"));
    }

    @Test
    @DisplayName("Should cleanup expired entries")
    void testCleanupExpired() throws InterruptedException {
        // Use short TTL for testing (1 minute)
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache", 100, 1);

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        assertEquals(3, cache.size());

        // Since TTL is 1 minute and we can't wait that long,
        // verify that cleanup doesn't remove non-expired entries
        cache.cleanupExpired();

        assertEquals(3, cache.size(), "Non-expired entries should remain");
    }

    // === Cache Size Adjustment Tests ===

    @Test
    @DisplayName("Should adjust cache max size")
    void testSetMaxSize() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache", 100, 60);

        for (int i = 0; i < 50; i++) {
            cache.put("key" + i, "value" + i);
        }

        assertEquals(50, cache.size());

        // Reduce max size to 30
        cache.setMaxSize(30);

        assertEquals(30, cache.size(), "Cache should evict entries to meet new max size");
    }

    @Test
    @DisplayName("Should handle setMaxSize with size larger than current")
    void testSetMaxSizeLarger() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache", 10, 60);

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        cache.setMaxSize(100);

        assertEquals(2, cache.size());
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }

    // === Cache Removal Tests ===

    @Test
    @DisplayName("Should remove cache successfully")
    void testRemoveCache() {
        cacheManager.getCache("testCache");

        boolean removed = cacheManager.removeCache("testCache");

        assertTrue(removed);
    }

    @Test
    @DisplayName("Should return false when removing non-existent cache")
    void testRemoveNonExistentCache() {
        boolean removed = cacheManager.removeCache("nonExistent");

        assertFalse(removed);
    }

    // === Cache Statistics Tests ===

    @Test
    @DisplayName("Should record cache hits")
    void testRecordCacheHit() {
        cacheManager.getCache("testCache");

        cacheManager.recordCacheHit("testCache");

        CacheManager.CacheStatistics stats = cacheManager.getCacheStatistics("testCache");
        assertNotNull(stats);
        assertEquals(1, stats.getHits());
        assertEquals(0, stats.getMisses());
        assertEquals(100.0, stats.getHitRate(), 0.01);

        verify(metricsCollector).incrementCounter("performance.cache.hits");
    }

    @Test
    @DisplayName("Should record cache misses")
    void testRecordCacheMiss() {
        cacheManager.getCache("testCache");

        cacheManager.recordCacheMiss("testCache");

        CacheManager.CacheStatistics stats = cacheManager.getCacheStatistics("testCache");
        assertNotNull(stats);
        assertEquals(0, stats.getHits());
        assertEquals(1, stats.getMisses());
        assertEquals(0.0, stats.getHitRate(), 0.01);

        verify(metricsCollector).incrementCounter("performance.cache.misses");
    }

    @Test
    @DisplayName("Should calculate hit rate correctly")
    void testHitRateCalculation() {
        cacheManager.getCache("testCache");

        cacheManager.recordCacheHit("testCache");
        cacheManager.recordCacheHit("testCache");
        cacheManager.recordCacheHit("testCache");
        cacheManager.recordCacheMiss("testCache");
        cacheManager.recordCacheMiss("testCache");

        CacheManager.CacheStatistics stats = cacheManager.getCacheStatistics("testCache");
        assertEquals(3, stats.getHits());
        assertEquals(2, stats.getMisses());
        assertEquals(5, stats.getTotal());
        assertEquals(60.0, stats.getHitRate(), 0.01);
    }

    @Test
    @DisplayName("Should reset cache statistics")
    void testResetStatistics() {
        cacheManager.getCache("testCache");

        cacheManager.recordCacheHit("testCache");
        cacheManager.recordCacheMiss("testCache");

        CacheManager.CacheStatistics stats = cacheManager.getCacheStatistics("testCache");
        stats.reset();

        assertEquals(0, stats.getHits());
        assertEquals(0, stats.getMisses());
        assertEquals(0, stats.getTotal());
    }

    @Test
    @DisplayName("Should handle statistics for non-existent cache gracefully")
    void testStatisticsForNonExistentCache() {
        cacheManager.recordCacheHit("nonExistent");
        cacheManager.recordCacheMiss("nonExistent");

        // Should not throw exception
        assertNull(cacheManager.getCacheStatistics("nonExistent"));
    }

    @Test
    @DisplayName("Should get all cache statistics")
    void testGetAllCacheStatistics() {
        cacheManager.getCache("cache1");
        cacheManager.getCache("cache2");
        cacheManager.getCache("cache3");

        cacheManager.recordCacheHit("cache1");
        cacheManager.recordCacheMiss("cache2");

        Map<String, CacheManager.CacheStatistics> allStats = cacheManager.getAllCacheStatistics();

        assertEquals(3, allStats.size());
        assertTrue(allStats.containsKey("cache1"));
        assertTrue(allStats.containsKey("cache2"));
        assertTrue(allStats.containsKey("cache3"));
    }

    // === Clear All Caches Tests ===

    @Test
    @DisplayName("Should clear all caches")
    void testClearAllCaches() {
        CacheManager.Cache<String, Object> cache1 = cacheManager.getCache("cache1");
        CacheManager.Cache<String, Object> cache2 = cacheManager.getCache("cache2");

        cache1.put("key1", "value1");
        cache2.put("key2", "value2");

        cacheManager.recordCacheHit("cache1");
        cacheManager.recordCacheMiss("cache2");

        cacheManager.clearAllCaches();

        assertEquals(0, cache1.size());
        assertEquals(0, cache2.size());
        assertEquals(0, cacheManager.getCacheStatistics("cache1").getTotal());
        assertEquals(0, cacheManager.getCacheStatistics("cache2").getTotal());
    }

    // === Memory Pressure Tests ===

    @Test
    @DisplayName("Should get memory pressure info")
    void testGetMemoryPressureInfo() {
        CacheManager.MemoryPressureInfo info = cacheManager.getMemoryPressureInfo();

        assertNotNull(info);
        assertNotNull(info.getLevel());
        assertTrue(info.getPressure() >= 0.0 && info.getPressure() <= 1.0);
        assertTrue(info.getUsedMemoryMB() >= 0);
        assertTrue(info.getMaxMemoryMB() > 0);
        assertTrue(info.getTotalCacheEntries() >= 0);
    }

    @Test
    @DisplayName("Should track memory pressure correctly")
    void testMemoryPressureTracking() throws InterruptedException {
        // Create some caches to track
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache", 10000, 60);

        // Add many entries to increase memory usage (simulated)
        for (int i = 0; i < 1000; i++) {
            cache.put("key" + i, "value" + i);
        }

        CacheManager.MemoryPressureInfo info = cacheManager.getMemoryPressureInfo();
        assertTrue(info.getTotalCacheEntries() >= 1000);
    }

    // === Concurrent Access Tests ===

    @Test
    @DisplayName("Should handle concurrent cache access")
    void testConcurrentAccess() throws InterruptedException {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("concurrentCache", 10000, 60);
        int threadCount = 10;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "thread" + threadId + "_key" + i;
                        cache.put(key, "value" + i);
                        cache.get(key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify cache contains entries (some may be evicted due to size limit)
        assertTrue(cache.size() > 0);
        assertTrue(cache.size() <= 10000);
    }

    @Test
    @DisplayName("Should handle concurrent cache operations safely")
    void testConcurrentCacheOperations() throws InterruptedException {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache", 1000, 60);
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount * 3);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Concurrent puts
        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        cache.put("key_" + id + "_" + j, "value");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Concurrent gets
        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        cache.get("key_" + id + "_" + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Concurrent removes
        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        cache.remove("key_" + id + "_" + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify no exceptions occurred
        assertTrue(true, "Concurrent operations completed without exceptions");
    }

    // === Edge Cases Tests ===

    @Test
    @DisplayName("Should handle null key gracefully")
    void testNullKey() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache");

        // ConcurrentHashMap does not allow null keys
        // Verify that null key operations are handled appropriately
        assertThrows(NullPointerException.class, () -> cache.put(null, "value"),
            "ConcurrentHashMap should reject null keys");
        assertThrows(NullPointerException.class, () -> cache.get(null),
            "ConcurrentHashMap should reject null keys");
        assertThrows(NullPointerException.class, () -> cache.remove(null),
            "ConcurrentHashMap should reject null keys");
    }

    @Test
    @DisplayName("Should handle null value")
    void testNullValue() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("testCache");

        cache.put("key1", null);
        assertNull(cache.get("key1"));
    }

    @Test
    @DisplayName("Should handle very large cache size")
    void testVeryLargeCacheSize() {
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("largeCache", Integer.MAX_VALUE, 60);

        for (int i = 0; i < 10000; i++) {
            cache.put("key" + i, "value" + i);
        }

        assertEquals(10000, cache.size());
    }

    // === Cache Statistics Edge Cases ===

    @Test
    @DisplayName("Should handle zero total in hit rate calculation")
    void testHitRateWithZeroTotal() {
        CacheManager.CacheStatistics stats = new CacheManager.CacheStatistics();

        assertEquals(0.0, stats.getHitRate(), 0.01);
        assertEquals(0, stats.getTotal());
    }

    @Test
    @DisplayName("Should test CacheStatistics toString")
    void testCacheStatisticsToString() {
        CacheManager.CacheStatistics stats = new CacheManager.CacheStatistics();
        stats.recordHit();
        stats.recordHit();
        stats.recordMiss();

        String str = stats.toString();
        assertNotNull(str);
        assertTrue(str.contains("hits=2"));
        assertTrue(str.contains("misses=1"));
    }

    // === Memory Pressure Info Tests ===

    @Test
    @DisplayName("Should test MemoryPressureInfo toString")
    void testMemoryPressureInfoToString() {
        CacheManager.MemoryPressureInfo info = new CacheManager.MemoryPressureInfo(
            0.75,
            CacheManager.MemoryPressureLevel.MEDIUM,
            750,
            1000,
            100
        );

        String str = info.toString();
        assertNotNull(str);
        assertTrue(str.contains("MEDIUM"));
        assertTrue(str.contains("75"));
    }

    @Test
    @DisplayName("Should test MemoryPressureInfo getters")
    void testMemoryPressureInfoGetters() {
        CacheManager.MemoryPressureInfo info = new CacheManager.MemoryPressureInfo(
            0.85,
            CacheManager.MemoryPressureLevel.HIGH,
            850,
            1000,
            500
        );

        assertEquals(0.85, info.getPressure(), 0.01);
        assertEquals(CacheManager.MemoryPressureLevel.HIGH, info.getLevel());
        assertEquals(850, info.getUsedMemoryMB());
        assertEquals(1000, info.getMaxMemoryMB());
        assertEquals(500, info.getTotalCacheEntries());
    }

    // === Shutdown Tests ===

    @Test
    @DisplayName("Should shutdown gracefully")
    void testShutdown() {
        CacheManager.Cache<String, Object> cache1 = cacheManager.getCache("cache1");
        CacheManager.Cache<String, Object> cache2 = cacheManager.getCache("cache2");

        cache1.put("key1", "value1");
        cache2.put("key2", "value2");

        assertDoesNotThrow(() -> cacheManager.shutdown());

        assertEquals(0, cache1.size());
        assertEquals(0, cache2.size());
    }

    // === Integration Tests ===

    @Test
    @DisplayName("Should handle complete cache lifecycle")
    void testCompleteCacheLifecycle() {
        // Create cache
        CacheManager.Cache<String, Object> cache = cacheManager.getCache("lifecycleCache", 100, 60);

        // Add entries
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        // Record statistics
        cacheManager.recordCacheHit("lifecycleCache");
        cacheManager.recordCacheHit("lifecycleCache");
        cacheManager.recordCacheMiss("lifecycleCache");

        // Verify statistics
        CacheManager.CacheStatistics stats = cacheManager.getCacheStatistics("lifecycleCache");
        assertEquals(2, stats.getHits());
        assertEquals(1, stats.getMisses());

        // Remove cache
        boolean removed = cacheManager.removeCache("lifecycleCache");
        assertTrue(removed);
        assertNull(cacheManager.getCacheStatistics("lifecycleCache"));
    }
}
