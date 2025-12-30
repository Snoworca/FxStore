package com.snoworca.fxstore.ost;

/**
 * Stack frame for OST traversal path.
 * Used during insert/delete operations to track the path from root to leaf.
 */
public class OSTPathFrame {

    public final OSTInternal node;  // public for direct access
    public final int childIndex;  // public for direct access
    private final int localPosition;

    public OSTPathFrame(OSTInternal node, int childIndex, int localPosition) {
        this.node = node;
        this.childIndex = childIndex;
        this.localPosition = localPosition;
    }

    /**
     * Get the internal node at this level.
     */
    public OSTInternal getNode() {
        return node;
    }

    /**
     * Get the index of the child taken at this level.
     */
    public int getChildIndex() {
        return childIndex;
    }

    /**
     * Get the local position within the subtree at this level.
     */
    public int getLocalPosition() {
        return localPosition;
    }

    @Override
    public String toString() {
        return "OSTPathFrame{" +
                "pageId=" + node.getPageId() +
                ", childIndex=" + childIndex +
                ", localPosition=" + localPosition +
                '}';
    }
}
