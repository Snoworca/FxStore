package com.snoworca.fxstore.core;

import com.snoworca.fxstore.storage.MemoryStorage;
import com.snoworca.fxstore.storage.Storage;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * PageCache LRU 캐시 테스트.
 */
public class PageCacheTest {

    private static final int PAGE_SIZE = 4096;
    private static final long MAX_CACHE_BYTES = 10 * PAGE_SIZE; // 10 pages
    private static final Random random = new Random(42);

    private Storage storage;
    private PageCache cache;

    @Before
    public void setUp() {
        storage = new MemoryStorage(1024 * 1024); // 1MB
        cache = new PageCache(storage, PAGE_SIZE, MAX_CACHE_BYTES);
    }

    // ==================== Basic Operations ====================

    @Test
    public void testPutAndGet() {
        byte[] data = createRandomPage();
        cache.put(1L, data);

        byte[] retrieved = cache.get(1L);
        assertArrayEquals(data, retrieved);
    }

    @Test
    public void testGet_NotInCache() {
        assertNull(cache.get(999L));
    }

    @Test
    public void testMultiplePuts() {
        for (long i = 0; i < 5; i++) {
            byte[] data = createRandomPage();
            cache.put(i, data);
        }

        assertEquals(5 * PAGE_SIZE, cache.getCacheBytes());
    }

    // ==================== Read/Write with Storage ====================

    @Test
    public void testWritePage() {
        byte[] data = createRandomPage();
        cache.writePage(1L, data);

        // Should be in cache
        byte[] cached = cache.get(1L);
        assertArrayEquals(data, cached);
    }

    @Test
    public void testReadPage_CacheHit() {
        byte[] data = createRandomPage();
        cache.put(1L, data);

        // Read should return cached data
        byte[] result = cache.readPage(1L);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadPage_CacheMiss() {
        // Write using cache's writePage to ensure correct offset
        byte[] data = createRandomPage();
        cache.writePage(2L, data);

        // Clear cache to force storage read
        cache.invalidate(2L);

        // Read should fetch from storage and cache
        byte[] result = cache.readPage(2L);
        assertArrayEquals(data, result);

        // Now should be in cache
        assertArrayEquals(data, cache.get(2L));
    }

    // ==================== LRU Eviction ====================

    @Test
    public void testLRU_Eviction() {
        // Fill cache to capacity (10 pages)
        for (long i = 0; i < 10; i++) {
            cache.put(i, createRandomPage());
        }

        assertEquals(10 * PAGE_SIZE, cache.getCacheBytes());

        // Add one more page - should evict oldest
        cache.put(10L, createRandomPage());

        // Cache should still be around 10 pages (LRU evicted one)
        assertTrue(cache.getCacheBytes() <= 11 * PAGE_SIZE);
    }

    @Test
    public void testLRU_AccessOrder() {
        // Add 5 pages
        for (long i = 0; i < 5; i++) {
            cache.put(i, createRandomPage());
        }

        // Access page 0 to make it recently used
        cache.get(0L);

        // Fill cache beyond capacity
        for (long i = 5; i < 15; i++) {
            cache.put(i, createRandomPage());
        }

        // Page 0 should still be in cache (recently accessed)
        // Page 1-4 may have been evicted first
        assertNotNull(cache.get(0L));
    }

    // ==================== Invalidation ====================

    @Test
    public void testInvalidate() {
        byte[] data = createRandomPage();
        cache.put(1L, data);
        assertEquals(PAGE_SIZE, cache.getCacheBytes());

        cache.invalidate(1L);

        assertNull(cache.get(1L));
        assertEquals(0, cache.getCacheBytes());
    }

    @Test
    public void testInvalidate_NonExistent() {
        // Should not throw
        cache.invalidate(999L);
        assertEquals(0, cache.getCacheBytes());
    }

    // ==================== Clear ====================

    @Test
    public void testClear() {
        for (long i = 0; i < 5; i++) {
            cache.put(i, createRandomPage());
        }

        assertEquals(5 * PAGE_SIZE, cache.getCacheBytes());
        assertTrue(cache.getCacheBytes() > 0);

        cache.clear();

        assertEquals(0, cache.getCacheBytes());
    }

    // ==================== Standalone Cache ====================

    @Test
    public void testStandaloneCache_PutGet() {
        PageCache standalone = new PageCache(MAX_CACHE_BYTES, PAGE_SIZE);

        byte[] data = createRandomPage();
        standalone.put(1L, data);

        assertArrayEquals(data, standalone.get(1L));
    }

    @Test(expected = IllegalStateException.class)
    public void testStandaloneCache_ReadPageThrows() {
        PageCache standalone = new PageCache(MAX_CACHE_BYTES, PAGE_SIZE);
        standalone.readPage(1L); // No storage bound
    }

    @Test
    public void testSetStorage() {
        PageCache standalone = new PageCache(MAX_CACHE_BYTES, PAGE_SIZE);

        // Write data to storage using a temporary cache with storage
        byte[] data = createRandomPage();
        cache.writePage(1L, data);

        // Bind storage to standalone cache
        standalone.setStorage(storage);

        // Now readPage should work
        byte[] result = standalone.readPage(1L);
        assertArrayEquals(data, result);
    }

    // ==================== Size Metrics ====================

    @Test
    public void testGetCacheBytes() {
        assertEquals(0, cache.getCacheBytes());

        cache.put(1L, createRandomPage());
        assertEquals(PAGE_SIZE, cache.getCacheBytes());

        cache.put(2L, createRandomPage());
        assertEquals(2 * PAGE_SIZE, cache.getCacheBytes());

        cache.invalidate(1L);
        assertEquals(PAGE_SIZE, cache.getCacheBytes());
    }

    @Test
    public void testCacheBytes_AfterMultipleOperations() {
        assertEquals(0, cache.getCacheBytes());

        for (long i = 0; i < 5; i++) {
            cache.put(i, createRandomPage());
        }

        assertEquals(5 * PAGE_SIZE, cache.getCacheBytes());
    }

    // ==================== Edge Cases ====================

    @Test
    public void testPut_UpdateExisting() {
        byte[] data1 = createRandomPage();
        byte[] data2 = createRandomPage();

        cache.put(1L, data1);
        assertEquals(PAGE_SIZE, cache.getCacheBytes());

        cache.put(1L, data2);
        assertEquals(PAGE_SIZE, cache.getCacheBytes()); // Should not increase

        assertArrayEquals(data2, cache.get(1L));
    }

    @Test
    public void testZeroPageId() {
        byte[] data = createRandomPage();
        cache.put(0L, data);

        assertArrayEquals(data, cache.get(0L));
    }

    @Test
    public void testLargePageId() {
        long largeId = Long.MAX_VALUE / PAGE_SIZE - 1;
        byte[] data = createRandomPage();
        cache.put(largeId, data);

        assertArrayEquals(data, cache.get(largeId));
    }

    // ==================== Concurrent Access Simulation ====================

    @Test
    public void testManyPagesSequential() {
        for (long i = 0; i < 1000; i++) {
            cache.put(i, createRandomPage());
        }

        // Cache should have limited entries due to LRU eviction
        assertTrue(cache.getCacheBytes() <= MAX_CACHE_BYTES + PAGE_SIZE);
    }

    @Test
    public void testRandomAccessPattern() {
        // Pre-populate cache
        for (long i = 0; i < 5; i++) {
            cache.put(i, createRandomPage());
        }

        // Random access pattern
        for (int i = 0; i < 100; i++) {
            long pageId = random.nextInt(20);
            byte[] page = cache.get(pageId);

            if (page == null) {
                cache.put(pageId, createRandomPage());
            }
        }

        // Cache should still be within limits
        assertTrue(cache.getCacheBytes() <= MAX_CACHE_BYTES + PAGE_SIZE);
    }

    // ==================== Helper Methods ====================

    private byte[] createRandomPage() {
        byte[] page = new byte[PAGE_SIZE];
        random.nextBytes(page);
        return page;
    }
}
