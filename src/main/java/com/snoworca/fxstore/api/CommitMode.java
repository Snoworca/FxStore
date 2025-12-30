package com.snoworca.fxstore.api;

/**
 * Commit mode for FxStore operations.
 */
public enum CommitMode {
    /** Commit immediately after each modification */
    AUTO,
    /** Commit only when commit() is called */
    BATCH
}
