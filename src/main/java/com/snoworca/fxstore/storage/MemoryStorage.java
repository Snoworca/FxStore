package com.snoworca.fxstore.storage;

import com.snoworca.fxstore.api.FxErrorCode;
import com.snoworca.fxstore.api.FxException;

import java.util.Arrays;

/**
 * Memory-based storage implementation.
 */
public class MemoryStorage implements Storage {

    private byte[] data;
    private int size;
    private final long memoryLimit;
    private boolean closed;

    public MemoryStorage() {
        this(Long.MAX_VALUE);
    }

    public MemoryStorage(long memoryLimit) {
        this.memoryLimit = memoryLimit;
        this.data = new byte[4096];
        this.size = 0;
        this.closed = false;
    }

    private void checkClosed() {
        if (closed) {
            throw new FxException("Storage is closed", FxErrorCode.CLOSED);
        }
    }

    private void ensureCapacity(long requiredSize) {
        if (requiredSize > memoryLimit) {
            throw new FxException("Memory limit exceeded: " + requiredSize + " > " + memoryLimit,
                    FxErrorCode.OUT_OF_MEMORY);
        }
        if (requiredSize > data.length) {
            int newCapacity = data.length;
            while (newCapacity < requiredSize) {
                newCapacity = (int) Math.min((long) newCapacity * 2, Integer.MAX_VALUE);
            }
            data = Arrays.copyOf(data, newCapacity);
        }
    }

    @Override
    public void read(long offset, byte[] buffer, int bufOffset, int length) {
        checkClosed();
        if (offset < 0 || offset + length > size) {
            throw new FxException("Read out of bounds: offset=" + offset + ", length=" + length + ", size=" + size,
                    FxErrorCode.IO);
        }
        System.arraycopy(data, (int) offset, buffer, bufOffset, length);
    }

    @Override
    public void write(long offset, byte[] buffer, int bufOffset, int length) {
        checkClosed();
        long requiredSize = offset + length;
        ensureCapacity(requiredSize);
        System.arraycopy(buffer, bufOffset, data, (int) offset, length);
        if (requiredSize > size) {
            size = (int) requiredSize;
        }
    }

    @Override
    public void force(boolean metadata) {
        checkClosed();
        // No-op for memory storage
    }

    @Override
    public long size() {
        checkClosed();
        return size;
    }

    @Override
    public void extend(long newSize) {
        checkClosed();
        ensureCapacity(newSize);
        if (newSize > size) {
            size = (int) newSize;
        }
    }

    @Override
    public void truncate(long newSize) {
        checkClosed();
        if (newSize < size) {
            size = (int) newSize;
        }
    }

    @Override
    public void close() {
        closed = true;
        data = null;
    }

    /**
     * Get a copy of all data for export/compaction.
     */
    public byte[] toByteArray() {
        checkClosed();
        return Arrays.copyOf(data, size);
    }
}
