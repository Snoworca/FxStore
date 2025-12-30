package com.snoworca.fxstore.core;

import com.snoworca.fxstore.storage.Storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU page cache for reducing I/O operations.
 */
public class PageCache {

    private Storage storage;
    private final int pageSize;
    private final long maxCacheBytes;
    private final Map<Long, byte[]> cache;
    private long cacheBytes;

    /**
     * Create a page cache with storage backend.
     */
    public PageCache(Storage storage, int pageSize, long maxCacheBytes) {
        this.storage = storage;
        this.pageSize = pageSize;
        this.maxCacheBytes = maxCacheBytes;
        this.cacheBytes = 0;
        this.cache = createLRUCache();
    }

    /**
     * Create a standalone page cache without storage backend.
     * Used for caching pages before storage is bound.
     */
    public PageCache(long maxCacheBytes, int pageSize) {
        this.storage = null;
        this.pageSize = pageSize;
        this.maxCacheBytes = maxCacheBytes;
        this.cacheBytes = 0;
        this.cache = createLRUCache();
    }

    private Map<Long, byte[]> createLRUCache() {
        return new LinkedHashMap<Long, byte[]>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
                if (cacheBytes > maxCacheBytes && !isEmpty()) {
                    cacheBytes -= eldest.getValue().length;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Bind storage to this cache.
     */
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    /**
     * Read a page from cache or storage.
     *
     * @param pageId the page ID
     * @return page data
     */
    public byte[] readPage(long pageId) {
        byte[] page = cache.get(pageId);
        if (page != null) {
            return page;
        }

        if (storage == null) {
            throw new IllegalStateException("Storage not bound to cache");
        }

        // Read from storage
        page = new byte[pageSize];
        long offset = pageIdToOffset(pageId);
        storage.read(offset, page, 0, pageSize);

        // Add to cache
        cache.put(pageId, page);
        cacheBytes += page.length;

        return page;
    }

    /**
     * Get a page from cache only (no storage read).
     *
     * @param pageId the page ID
     * @return page data or null if not in cache
     */
    public byte[] get(long pageId) {
        return cache.get(pageId);
    }

    /**
     * Put a page directly into cache.
     *
     * @param pageId the page ID
     * @param data   page data
     */
    public void put(long pageId, byte[] data) {
        byte[] existing = cache.put(pageId, data);
        if (existing == null) {
            cacheBytes += data.length;
        }
    }

    /**
     * Write a page to storage and update cache.
     *
     * @param pageId the page ID
     * @param data   page data
     */
    public void writePage(long pageId, byte[] data) {
        long offset = pageIdToOffset(pageId);
        storage.write(offset, data, 0, data.length);

        // Update cache
        byte[] existing = cache.put(pageId, data);
        if (existing == null) {
            cacheBytes += data.length;
        }
    }

    /**
     * Invalidate a page from cache.
     *
     * @param pageId the page ID to invalidate
     */
    public void invalidate(long pageId) {
        byte[] removed = cache.remove(pageId);
        if (removed != null) {
            cacheBytes -= removed.length;
        }
    }

    /**
     * Clear all cached pages.
     */
    public void clear() {
        cache.clear();
        cacheBytes = 0;
    }

    /**
     * Get current cache size in bytes.
     */
    public long getCacheBytes() {
        return cacheBytes;
    }

    /**
     * Get number of cached pages.
     */
    public int getCachedPageCount() {
        return cache.size();
    }

    /**
     * Convert page ID to file offset.
     */
    private long pageIdToOffset(long pageId) {
        // Pages start after header area (12KB)
        return 12288 + pageId * pageSize;
    }

    /**
     * Get the underlying storage.
     */
    public Storage getStorage() {
        return storage;
    }

    /**
     * Get page size.
     */
    public int getPageSize() {
        return pageSize;
    }
}
