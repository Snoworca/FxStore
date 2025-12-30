package com.snoworca.fxstore.api;

import java.util.Objects;

/**
 * Individual verification error.
 */
public final class VerifyError {
    private final VerifyErrorKind kind;
    private final long fileOffset;
    private final long objectId;
    private final String message;

    public VerifyError(VerifyErrorKind kind, long fileOffset, long objectId, String message) {
        this.kind = Objects.requireNonNull(kind);
        this.fileOffset = fileOffset;
        this.objectId = objectId;
        this.message = Objects.requireNonNull(message);
    }

    /**
     * Error category
     */
    public VerifyErrorKind kind() {
        return kind;
    }

    /**
     * File offset (-1 if unknown)
     */
    public long fileOffset() {
        return fileOffset;
    }

    /**
     * Page/object ID (0 if not applicable)
     */
    public long objectId() {
        return objectId;
    }

    /**
     * Error description
     */
    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "VerifyError{" +
                "kind=" + kind +
                ", fileOffset=" + fileOffset +
                ", objectId=" + objectId +
                ", message='" + message + '\'' +
                '}';
    }
}
