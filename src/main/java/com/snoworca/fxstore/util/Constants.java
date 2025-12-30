package com.snoworca.fxstore.util;

/**
 * Global constants for FxStore.
 */
public final class Constants {

    private Constants() {
    }

    // ==================== Magic Numbers ====================

    /** Superblock magic: "FXSTORE\0" */
    public static final byte[] SUPERBLOCK_MAGIC = {
            'F', 'X', 'S', 'T', 'O', 'R', 'E', '\0'
    };

    /** CommitHeader magic: "FXHDR\0\0\0" */
    public static final byte[] HEADER_MAGIC = {
            'F', 'X', 'H', 'D', 'R', '\0', '\0', '\0'
    };

    /** Page magic: "FXPG" */
    public static final byte[] PAGE_MAGIC = {
            'F', 'X', 'P', 'G'
    };

    /** Record magic: "FXRC" */
    public static final byte[] RECORD_MAGIC = {
            'F', 'X', 'R', 'C'
    };

    // ==================== File Layout ====================

    /** Superblock offset */
    public static final long SUPERBLOCK_OFFSET = 0;

    /** Superblock size */
    public static final int SUPERBLOCK_SIZE = 4096;

    /** CommitHeader slot A offset */
    public static final long HEADER_A_OFFSET = 4096;

    /** CommitHeader slot B offset */
    public static final long HEADER_B_OFFSET = 8192;

    /** CommitHeader size */
    public static final int HEADER_SIZE = 4096;

    /** Allocation area start offset */
    public static final long ALLOC_START = 12288;

    // ==================== Format Versions ====================

    /** Current format version */
    public static final int FORMAT_VERSION = 1;

    /** Current header version */
    public static final int HEADER_VERSION = 1;

    // ==================== Page Types ====================

    public static final int PAGE_TYPE_BTREE_INTERNAL = 1;
    public static final int PAGE_TYPE_BTREE_LEAF = 2;
    public static final int PAGE_TYPE_OST_INTERNAL = 3;
    public static final int PAGE_TYPE_OST_LEAF = 4;

    // ==================== Record Types ====================

    public static final int RECORD_TYPE_VALUE = 1;
    public static final int RECORD_TYPE_OVERFLOW = 2;
    public static final int RECORD_TYPE_CODEC_META = 3;

    // ==================== Page Header ====================

    public static final int PAGE_HEADER_SIZE = 32;

    // ==================== Limits ====================

    /** Maximum collection name length in bytes */
    public static final int MAX_NAME_LENGTH = 255;

    /** Maximum key/value size in bytes */
    public static final int MAX_KV_SIZE = 1024 * 1024; // 1 MiB

    // ==================== Default Options ====================

    /** Default page size */
    public static final int DEFAULT_PAGE_SIZE = 4096;

    /** Default cache size in bytes (64 MiB) */
    public static final long DEFAULT_CACHE_BYTES = 64 * 1024 * 1024L;

    /** Default memory limit (unlimited) */
    public static final long DEFAULT_MEMORY_LIMIT = Long.MAX_VALUE;
}
