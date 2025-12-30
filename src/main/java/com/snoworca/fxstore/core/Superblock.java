package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.FxErrorCode;
import com.snoworca.fxstore.api.FxException;
import com.snoworca.fxstore.api.PageSize;
import com.snoworca.fxstore.util.ByteUtils;
import com.snoworca.fxstore.util.CRC32C;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Superblock: First 4096 bytes of the file.
 * Contains file format metadata.
 */
public class Superblock {

    public static final int SIZE = 4096;
    public static final byte[] MAGIC = "FXSTORE\0".getBytes(StandardCharsets.US_ASCII);
    public static final int FORMAT_VERSION = 1;

    private static final int MAGIC_OFFSET = 0;
    private static final int FORMAT_VERSION_OFFSET = 8;
    private static final int PAGE_SIZE_OFFSET = 12;
    private static final int FEATURE_FLAGS_OFFSET = 16;
    private static final int CREATED_AT_OFFSET = 24;
    private static final int CRC_OFFSET = 4092;

    private final int formatVersion;
    private final int pageSize;
    private final long featureFlags;
    private final long createdAtEpochMs;

    public Superblock(int pageSize) {
        this(FORMAT_VERSION, pageSize, 0, System.currentTimeMillis());
    }

    public Superblock(int formatVersion, int pageSize, long featureFlags, long createdAtEpochMs) {
        this.formatVersion = formatVersion;
        this.pageSize = pageSize;
        this.featureFlags = featureFlags;
        this.createdAtEpochMs = createdAtEpochMs;
    }

    /**
     * Create a new Superblock with default settings.
     */
    public static Superblock create(int pageSize) {
        return new Superblock(pageSize);
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getFeatureFlags() {
        return featureFlags;
    }

    public long getCreatedAtEpochMs() {
        return createdAtEpochMs;
    }

    public byte[] serialize() {
        byte[] data = new byte[SIZE];

        // Magic
        System.arraycopy(MAGIC, 0, data, MAGIC_OFFSET, MAGIC.length);

        // Format version
        ByteUtils.writeI32LE(data, FORMAT_VERSION_OFFSET, formatVersion);

        // Page size
        ByteUtils.writeI32LE(data, PAGE_SIZE_OFFSET, pageSize);

        // Feature flags
        ByteUtils.writeI64LE(data, FEATURE_FLAGS_OFFSET, featureFlags);

        // Created at
        ByteUtils.writeI64LE(data, CREATED_AT_OFFSET, createdAtEpochMs);

        // CRC
        int crc = CRC32C.compute(data, 0, CRC_OFFSET);
        ByteUtils.writeI32LE(data, CRC_OFFSET, crc);

        return data;
    }

    /**
     * Encode to byte array (alias for serialize).
     */
    public byte[] encode() {
        return serialize();
    }

    /**
     * Decode from byte array (alias for deserialize).
     */
    public static Superblock decode(byte[] data) {
        return deserialize(data);
    }

    public static Superblock deserialize(byte[] data) {
        // Verify magic
        byte[] magic = Arrays.copyOfRange(data, MAGIC_OFFSET, MAGIC_OFFSET + MAGIC.length);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new FxException("Invalid superblock magic", FxErrorCode.CORRUPTION);
        }

        // Verify CRC
        int storedCrc = ByteUtils.readI32LE(data, CRC_OFFSET);
        int computedCrc = CRC32C.compute(data, 0, CRC_OFFSET);
        if (storedCrc != computedCrc) {
            throw new FxException("Superblock CRC mismatch", FxErrorCode.CORRUPTION);
        }

        int formatVersion = ByteUtils.readI32LE(data, FORMAT_VERSION_OFFSET);
        int pageSize = ByteUtils.readI32LE(data, PAGE_SIZE_OFFSET);
        long featureFlags = ByteUtils.readI64LE(data, FEATURE_FLAGS_OFFSET);
        long createdAtEpochMs = ByteUtils.readI64LE(data, CREATED_AT_OFFSET);

        return new Superblock(formatVersion, pageSize, featureFlags, createdAtEpochMs);
    }

    public static boolean verify(byte[] data) {
        try {
            deserialize(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
