package com.snoworca.fxstore.api;

/**
 * Store statistics.
 */
public final class Stats {
    private final long fileBytes;
    private final long liveBytesEstimate;
    private final long deadBytesEstimate;
    private final double deadRatio;
    private final int collectionCount;

    public Stats(long fileBytes, long liveBytesEstimate, long deadBytesEstimate,
                 double deadRatio, int collectionCount) {
        this.fileBytes = fileBytes;
        this.liveBytesEstimate = liveBytesEstimate;
        this.deadBytesEstimate = deadBytesEstimate;
        this.deadRatio = deadRatio;
        this.collectionCount = collectionCount;
    }

    /**
     * File size in bytes or estimated memory usage
     */
    public long fileBytes() {
        return fileBytes;
    }

    /**
     * Estimated reachable data size
     */
    public long liveBytesEstimate() {
        return liveBytesEstimate;
    }

    /**
     * Estimated unreachable data size
     */
    public long deadBytesEstimate() {
        return deadBytesEstimate;
    }

    /**
     * Dead ratio (0.0 ~ 1.0)
     */
    public double deadRatio() {
        return deadRatio;
    }

    /**
     * Number of collections
     */
    public int collectionCount() {
        return collectionCount;
    }

    @Override
    public String toString() {
        return "Stats{" +
                "fileBytes=" + fileBytes +
                ", liveBytesEstimate=" + liveBytesEstimate +
                ", deadBytesEstimate=" + deadBytesEstimate +
                ", deadRatio=" + deadRatio +
                ", collectionCount=" + collectionCount +
                '}';
    }
}
