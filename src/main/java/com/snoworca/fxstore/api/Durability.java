package com.snoworca.fxstore.api;

/**
 * Durability mode for commit operations.
 */
public enum Durability {
    /** fsync on commit (higher durability, higher latency) */
    SYNC,
    /** OS buffer only (higher performance, possible data loss on crash) */
    ASYNC
}
