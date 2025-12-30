package com.snoworca.fxstore.ost;

/**
 * Base interface for Order-Statistic Tree nodes.
 */
public interface OSTNode {

    /**
     * Get the page ID of this node.
     */
    long getPageId();

    /**
     * Set the page ID (for COW operations).
     */
    void setPageId(long pageId);

    /**
     * Check if this is a leaf node.
     */
    boolean isLeaf();

    /**
     * Get the total element count in this subtree.
     */
    int getSubtreeCount();

    /**
     * Alias for getSubtreeCount.
     */
    int subtreeCount();

    /**
     * Serialize node to byte array.
     */
    byte[] serialize();

    /**
     * Serialize to page of specified size.
     */
    byte[] toPage(int pageSize);
}
