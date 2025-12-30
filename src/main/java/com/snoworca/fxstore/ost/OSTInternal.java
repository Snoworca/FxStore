package com.snoworca.fxstore.ost;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Order-Statistic Tree internal node.
 * Contains child references and subtree counts.
 */
public class OSTInternal implements OSTNode {

    private long pageId;
    private int level;
    private final List<Long> children;
    private final List<Integer> subtreeCounts;

    public OSTInternal(long pageId, int level) {
        this.pageId = pageId;
        this.level = level;
        this.children = new ArrayList<>();
        this.subtreeCounts = new ArrayList<>();
    }

    /**
     * Constructor for creating node with existing children and counts.
     */
    public OSTInternal(int level, java.util.List<Long> children, java.util.List<Integer> counts) {
        this.pageId = 0;
        this.level = level;
        this.children = new ArrayList<>(children);
        this.subtreeCounts = new ArrayList<>(counts);
    }

    @Override
    public long getPageId() {
        return pageId;
    }

    @Override
    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public int getSubtreeCount() {
        int total = 0;
        for (Integer count : subtreeCounts) {
            total += count;
        }
        return total;
    }

    public int getChildCount() {
        return children.size();
    }

    public long getChild(int index) {
        return children.get(index);
    }

    /**
     * Get child page ID (alias for getChild).
     */
    public long getChildPageId(int index) {
        return children.get(index);
    }

    public void setChild(int index, long childPageId) {
        children.set(index, childPageId);
    }

    public int getChildSubtreeCount(int index) {
        return subtreeCounts.get(index);
    }

    /**
     * Get subtree count for specific child (alias for getChildSubtreeCount).
     */
    public int getSubtreeCount(int index) {
        return subtreeCounts.get(index);
    }

    /**
     * Get total subtree count (alias without args).
     */
    public int subtreeCount() {
        int total = 0;
        for (Integer count : subtreeCounts) {
            total += count;
        }
        return total;
    }

    public void setChildSubtreeCount(int index, int count) {
        subtreeCounts.set(index, count);
    }

    public void addChild(long childPageId, int subtreeCount) {
        children.add(childPageId);
        subtreeCounts.add(subtreeCount);
    }

    public void insertChild(int index, long childPageId, int subtreeCount) {
        children.add(index, childPageId);
        subtreeCounts.add(index, subtreeCount);
    }

    public void removeChild(int index) {
        children.remove(index);
        subtreeCounts.remove(index);
    }

    /**
     * Find child index for given global position.
     *
     * @param position global position (0-based)
     * @return array of [childIndex, localPosition]
     */
    public int[] findChildForPosition(int position) {
        int accumulated = 0;
        for (int i = 0; i < subtreeCounts.size(); i++) {
            int count = subtreeCounts.get(i);
            if (position < accumulated + count) {
                return new int[]{i, position - accumulated};
            }
            accumulated += count;
        }
        throw new IndexOutOfBoundsException("Position out of range: " + position);
    }

    public boolean needsSplit(int maxChildren) {
        return children.size() > maxChildren;
    }

    public boolean canMerge(int minChildren) {
        return children.size() < minChildren;
    }

    /**
     * Split this node and return new right node.
     */
    public OSTInternal split(long newPageId) {
        int mid = children.size() / 2;
        OSTInternal right = new OSTInternal(newPageId, level);

        for (int i = mid; i < children.size(); i++) {
            right.children.add(children.get(i));
            right.subtreeCounts.add(subtreeCounts.get(i));
        }

        while (children.size() > mid) {
            children.remove(children.size() - 1);
            subtreeCounts.remove(subtreeCounts.size() - 1);
        }

        return right;
    }

    @Override
    public byte[] serialize() {
        int size = 2 + 2 + children.size() * (8 + 4); // level + count + (child + subtreeCount)
        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

        buf.putShort((short) level);
        buf.putShort((short) children.size());

        for (int i = 0; i < children.size(); i++) {
            buf.putLong(children.get(i));
            buf.putInt(subtreeCounts.get(i));
        }

        return buf.array();
    }

    public static OSTInternal deserialize(long pageId, byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int level = buf.getShort() & 0xFFFF;
        int count = buf.getShort() & 0xFFFF;

        OSTInternal internal = new OSTInternal(pageId, level);

        for (int i = 0; i < count; i++) {
            long child = buf.getLong();
            int subtreeCount = buf.getInt();
            internal.children.add(child);
            internal.subtreeCounts.add(subtreeCount);
        }

        return internal;
    }

    /**
     * Deserialize from page bytes (pageId defaults to 0).
     * Expects page type byte at position 0 (skipped).
     */
    public static OSTInternal fromPage(byte[] data) {
        // Skip page type byte at position 0
        byte[] dataWithoutType = new byte[data.length - 1];
        System.arraycopy(data, 1, dataWithoutType, 0, dataWithoutType.length);
        return deserialize(0, dataWithoutType);
    }

    /**
     * Serialize to page of specified size.
     * Includes page type byte (2 = INTERNAL) at position 0.
     */
    public byte[] toPage(int pageSize) {
        byte[] page = new byte[pageSize];
        page[0] = 2; // Page type: INTERNAL
        byte[] serialized = serialize();
        System.arraycopy(serialized, 0, page, 1, serialized.length);
        return page;
    }
}
