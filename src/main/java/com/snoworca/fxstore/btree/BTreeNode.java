package com.snoworca.fxstore.btree;

/**
 * Base interface for B+Tree nodes.
 */
public interface BTreeNode {

    /**
     * Get the page ID of this node.
     */
    long getPageId();

    /**
     * Check if this is a leaf node.
     */
    boolean isLeaf();

    /**
     * Get the number of keys in this node.
     */
    int getKeyCount();

    /**
     * Get key at specified index.
     *
     * @param index key index
     * @return key bytes
     */
    byte[] getKey(int index);

    /**
     * Check if node needs split (has too many keys).
     *
     * @param maxKeys maximum keys before split
     * @return true if split needed
     */
    boolean needsSplit(int maxKeys);

    /**
     * Check if node can merge (has too few keys).
     *
     * @param minKeys minimum keys before merge
     * @return true if merge possible
     */
    boolean canMerge(int minKeys);

    /**
     * Serialize node to byte array.
     */
    byte[] serialize();

    /**
     * Convert node to page bytes (alias for serialize).
     */
    byte[] toPage();
}
