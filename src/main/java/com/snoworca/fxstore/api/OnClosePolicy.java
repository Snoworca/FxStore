package com.snoworca.fxstore.api;

/**
 * Policy for handling uncommitted changes on close (BATCH mode).
 */
public enum OnClosePolicy {
    /** Throw exception if uncommitted changes exist */
    ERROR,
    /** Auto-commit on close */
    COMMIT,
    /** Auto-rollback on close */
    ROLLBACK
}
