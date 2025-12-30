package com.snoworca.fxstore.api;

/**
 * Error codes for FxStore exceptions.
 * 
 * @see FxException
 */
public enum FxErrorCode {
    // I/O and Resource Errors
    /** I/O operation failed (read/write/force) */
    IO,
    
    /** Data corruption detected (CRC/format violation) */
    CORRUPTION,
    
    /** Memory limit exceeded */
    OUT_OF_MEMORY,
    
    /** Failed to acquire file lock */
    LOCK_FAILED,
    
    // State Errors
    /** Operation on closed store */
    CLOSED,

    /** Illegal state (operation not valid in current state) */
    ILLEGAL_STATE,
    
    // Existence Errors
    /** Collection not found */
    NOT_FOUND,
    
    /** Collection already exists */
    ALREADY_EXISTS,
    
    // Type/Version Errors
    /** Collection kind or codec ID mismatch */
    TYPE_MISMATCH,
    
    /** Codec version, NumberMode, format version, or page size conflict */
    VERSION_MISMATCH,
    
    /** Required codec not registered */
    CODEC_NOT_FOUND,
    
    /** Codec upgrade hook failed */
    UPGRADE_FAILED,
    
    // General Errors
    /** Invalid argument (null, out of range, size exceeded, etc.) */
    ILLEGAL_ARGUMENT,

    /** Unsupported operation (e.g., write on view) */
    UNSUPPORTED,

    /** Internal error (programming error, reflection failure, etc.) */
    INTERNAL
}
