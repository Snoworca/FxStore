package com.snoworca.fxstore.btree;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * B+Tree internal node.
 * Contains separator keys and child page references.
 */
public class BTreeInternal implements BTreeNode {

    private static final int LEVEL_OFFSET = 32;
    private static final int COUNT_OFFSET = 34;
    private static final int CHILDREN_OFFSET = 36;

    private long pageId;
    private final int pageSize;
    private int level;
    private final List<byte[]> keys;
    private final List<Long> children;

    public BTreeInternal(int pageSize, long pageId, int level) {
        this.pageSize = pageSize;
        this.pageId = pageId;
        this.level = level;
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public BTreeInternal(int pageSize, int level) {
        this(pageSize, 0, level);
    }

    @Override
    public long getPageId() {
        return pageId;
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public int getKeyCount() {
        return keys.size();
    }

    @Override
    public byte[] getKey(int index) {
        return keys.get(index);
    }

    public int getChildCount() {
        return children.size();
    }

    public long getChild(int index) {
        return children.get(index);
    }

    public long getChildPageId(int index) {
        return children.get(index);
    }

    public void setChild(int index, long childPageId) {
        children.set(index, childPageId);
    }

    /**
     * Find child index for given key.
     */
    public int findChildIndex(byte[] key, Comparator<byte[]> comparator) {
        int lo = 0, hi = keys.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (comparator.compare(key, keys.get(mid)) >= 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    public void insertKey(int index, byte[] key) {
        keys.add(index, Arrays.copyOf(key, key.length));
    }

    public void insertChild(int index, long childPageId) {
        children.add(index, childPageId);
    }

    public void insertKeyAndChild(int keyIndex, byte[] key, long rightChildPageId) {
        keys.add(keyIndex, Arrays.copyOf(key, key.length));
        children.add(keyIndex + 1, rightChildPageId);
    }

    public void removeKeyAndChild(int keyIndex) {
        keys.remove(keyIndex);
        children.remove(keyIndex + 1);
    }

    public void setChildPageId(int index, long childPageId) {
        children.set(index, childPageId);
    }

    /**
     * Insert child with separator key (3-arg version).
     */
    public void insertChild(int index, byte[] key, long rightChildPageId) {
        keys.add(index, Arrays.copyOf(key, key.length));
        children.add(index + 1, rightChildPageId);
    }

    public byte[] toPage() {
        return serialize();
    }

    /**
     * Split result for internal nodes.
     */
    public static class SplitResult {
        public final BTreeInternal leftNode;
        public final BTreeInternal rightNode;
        public final byte[] splitKey;

        public SplitResult(BTreeInternal leftNode, BTreeInternal rightNode, byte[] splitKey) {
            this.leftNode = leftNode;
            this.rightNode = rightNode;
            this.splitKey = splitKey;
        }
    }

    /**
     * Split this internal node and return SplitResult.
     */
    public SplitResult split() {
        int mid = keys.size() / 2;
        byte[] promotedKey = keys.get(mid);

        BTreeInternal left = new BTreeInternal(pageSize, 0, level);
        BTreeInternal right = new BTreeInternal(pageSize, 0, level);

        for (int i = 0; i < mid; i++) {
            left.keys.add(keys.get(i));
        }
        for (int i = 0; i <= mid; i++) {
            left.children.add(children.get(i));
        }

        for (int i = mid + 1; i < keys.size(); i++) {
            right.keys.add(keys.get(i));
        }
        for (int i = mid + 1; i < children.size(); i++) {
            right.children.add(children.get(i));
        }

        return new SplitResult(left, right, promotedKey);
    }

    public boolean isFull() {
        return calculateSerializedSize() > pageSize - 100;
    }

    @Override
    public boolean needsSplit(int maxKeys) {
        return isFull();
    }

    @Override
    public boolean canMerge(int minKeys) {
        return keys.size() < minKeys;
    }

    public BTreeInternal copy() {
        BTreeInternal copy = new BTreeInternal(pageSize, pageId, level);
        for (byte[] key : keys) {
            copy.keys.add(Arrays.copyOf(key, key.length));
        }
        copy.children.addAll(children);
        return copy;
    }

    /**
     * Split this internal node and return new right node and promoted key.
     */
    public Object[] split(long newPageId) {
        int mid = keys.size() / 2;
        byte[] promotedKey = keys.get(mid);

        BTreeInternal right = new BTreeInternal(pageSize, newPageId, level);

        for (int i = mid + 1; i < keys.size(); i++) {
            right.keys.add(keys.get(i));
        }

        for (int i = mid + 1; i < children.size(); i++) {
            right.children.add(children.get(i));
        }

        while (keys.size() > mid) {
            keys.remove(keys.size() - 1);
        }
        while (children.size() > mid + 1) {
            children.remove(children.size() - 1);
        }

        return new Object[]{promotedKey, right};
    }

    private int calculateSerializedSize() {
        int size = CHILDREN_OFFSET + children.size() * 8;
        for (byte[] key : keys) {
            size += 4 + key.length;
        }
        return size;
    }

    @Override
    public byte[] serialize() {
        byte[] page = new byte[pageSize];
        ByteBuffer buf = ByteBuffer.wrap(page).order(ByteOrder.LITTLE_ENDIAN);

        buf.position(LEVEL_OFFSET);
        buf.putShort((short) level);
        buf.putShort((short) keys.size());

        // Write children
        for (Long child : children) {
            buf.putLong(child);
        }

        // Write keys
        for (byte[] key : keys) {
            buf.putInt(key.length);
            buf.put(key);
        }

        return page;
    }

    public static BTreeInternal fromPage(byte[] page, int pageSize, long pageId) {
        ByteBuffer buf = ByteBuffer.wrap(page).order(ByteOrder.LITTLE_ENDIAN);

        buf.position(LEVEL_OFFSET);
        int level = buf.getShort() & 0xFFFF;
        int keyCount = buf.getShort() & 0xFFFF;
        int childCount = keyCount + 1;

        BTreeInternal internal = new BTreeInternal(pageSize, pageId, level);

        // Read children
        for (int i = 0; i < childCount; i++) {
            internal.children.add(buf.getLong());
        }

        // Read keys
        for (int i = 0; i < keyCount; i++) {
            int keyLen = buf.getInt();
            byte[] key = new byte[keyLen];
            buf.get(key);
            internal.keys.add(key);
        }

        return internal;
    }
}
