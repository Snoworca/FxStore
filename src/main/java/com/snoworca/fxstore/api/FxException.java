package com.snoworca.fxstore.api;

/**
 * Runtime exception for all FxStore errors.
 */
public class FxException extends RuntimeException {
    private final FxErrorCode code;

    public FxException(String message, FxErrorCode code) {
        super(message);
        this.code = code;
    }

    public FxException(String message, Throwable cause, FxErrorCode code) {
        super(message, cause);
        this.code = code;
    }

    // Alternative constructor order for compatibility
    public FxException(FxErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public FxException(FxErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public FxErrorCode code() {
        return code;
    }

    public FxErrorCode getCode() {
        return code;
    }

    // ==================== Factory Methods ====================

    public static FxException illegalArgument(String message) {
        return new FxException(message, FxErrorCode.ILLEGAL_ARGUMENT);
    }

    public static FxException unsupported(String message) {
        return new FxException(message, FxErrorCode.UNSUPPORTED);
    }

    public static FxException io(String message) {
        return new FxException(message, FxErrorCode.IO);
    }

    public static FxException io(String message, Throwable cause) {
        return new FxException(message, cause, FxErrorCode.IO);
    }

    public static FxException corruption(String message) {
        return new FxException(message, FxErrorCode.CORRUPTION);
    }

    public static FxException closed(String message) {
        return new FxException(message, FxErrorCode.CLOSED);
    }

    public static FxException notFound(String message) {
        return new FxException(message, FxErrorCode.NOT_FOUND);
    }

    public static FxException alreadyExists(String message) {
        return new FxException(message, FxErrorCode.ALREADY_EXISTS);
    }

    public static FxException typeMismatch(String message) {
        return new FxException(message, FxErrorCode.TYPE_MISMATCH);
    }

    public static FxException versionMismatch(String message) {
        return new FxException(message, FxErrorCode.VERSION_MISMATCH);
    }

    public static FxException codecNotFound(String message) {
        return new FxException(message, FxErrorCode.CODEC_NOT_FOUND);
    }

    public static FxException upgradeFailed(String message, Throwable cause) {
        return new FxException(message, cause, FxErrorCode.UPGRADE_FAILED);
    }

    public static FxException outOfMemory(String message) {
        return new FxException(message, FxErrorCode.OUT_OF_MEMORY);
    }

    public static FxException lockFailed(String message) {
        return new FxException(message, FxErrorCode.LOCK_FAILED);
    }

    public static FxException illegalState(String message) {
        return new FxException(message, FxErrorCode.ILLEGAL_STATE);
    }
}
