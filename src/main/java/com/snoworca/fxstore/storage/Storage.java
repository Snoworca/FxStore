package com.snoworca.fxstore.storage;

/**
 * Storage abstraction - unifies file and memory modes.
 */
public interface Storage extends AutoCloseable {

    /**
     * Read data from specified offset.
     *
     * @param offset    byte offset to read from
     * @param buffer    destination buffer
     * @param bufOffset offset within buffer
     * @param length    number of bytes to read
     * @throws com.snoworca.fxstore.api.FxException with IO on failure
     */
    void read(long offset, byte[] buffer, int bufOffset, int length);

    /**
     * Write data to specified offset.
     * Append-only policy: offset must be >= current size or within header area.
     *
     * @param offset    byte offset to write to
     * @param buffer    source buffer
     * @param bufOffset offset within buffer
     * @param length    number of bytes to write
     */
    void write(long offset, byte[] buffer, int bufOffset, int length);

    /**
     * Sync buffer to disk.
     *
     * @param metadata true to also sync file metadata
     */
    void force(boolean metadata);

    /**
     * Current storage size in bytes.
     */
    long size();

    /**
     * Extend storage to new size.
     *
     * @param newSize new size in bytes
     */
    void extend(long newSize);

    /**
     * Truncate storage to new size (for compaction only).
     *
     * @param newSize new size in bytes
     */
    void truncate(long newSize);

    /**
     * Close the storage and release resources.
     */
    @Override
    void close();
}
