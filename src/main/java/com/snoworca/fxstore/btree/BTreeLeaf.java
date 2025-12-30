package com.snoworca.fxstore.btree;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * B+Tree leaf node.
 * Contains key-value pairs where value is a reference to a record.
 */
public class BTreeLeaf implements BTreeNode {

    private static final int HEADER_SIZE = 32;
    private static final int LEVEL_OFFSET = 32;
    private static final int COUNT_OFFSET = 34;
    private static final int NEXT_LEAF_OFFSET = 36;
    private static final int DATA_START = 44;

    private long pageId;
    private final int pageSize;
    private final List<byte[]> keys;
    private final List<Long> valueRecordIds;
    private long nextLeafPageId;

    public BTreeLeaf(int pageSize, long pageId) {
        this.pageSize = pageSize;
        this.pageId = pageId;
        this.keys = new ArrayList<>();
        this.valueRecordIds = new ArrayList<>();
        this.nextLeafPageId = 0;
    }

    public BTreeLeaf(int pageSize) {
        this(pageSize, 0);
    }

    @Override
    public long getPageId() {
        return pageId;
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public int getKeyCount() {
        return keys.size();
    }

    @Override
    public byte[] getKey(int index) {
        return keys.get(index);
    }

    public long getValueRecordId(int index) {
        return valueRecordIds.get(index);
    }

    public void setValueRecordId(int index, long valueRecordId) {
        valueRecordIds.set(index, valueRecordId);
    }

    public long getNextLeafPageId() {
        return nextLeafPageId;
    }

    public void setNextLeafPageId(long nextLeafPageId) {
        this.nextLeafPageId = nextLeafPageId;
    }

    /**
     * Find key index using binary search.
     *
     * @return index if found, otherwise -(insertionPoint + 1)
     */
    public int find(byte[] key, Comparator<byte[]> comparator) {
        int lo = 0, hi = keys.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = comparator.compare(keys.get(mid), key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    /**
     * Find insertion point for key.
     */
    public int findInsertionPoint(byte[] key, Comparator<byte[]> comparator) {
        int lo = 0, hi = keys.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (comparator.compare(keys.get(mid), key) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    public void insert(int index, byte[] key, long valueRecordId) {
        keys.add(index, Arrays.copyOf(key, key.length));
        valueRecordIds.add(index, valueRecordId);
    }

    public void remove(int index) {
        keys.remove(index);
        valueRecordIds.remove(index);
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

    public BTreeLeaf copy() {
        BTreeLeaf copy = new BTreeLeaf(pageSize, pageId);
        for (int i = 0; i < keys.size(); i++) {
            copy.keys.add(Arrays.copyOf(keys.get(i), keys.get(i).length));
            copy.valueRecordIds.add(valueRecordIds.get(i));
        }
        copy.nextLeafPageId = nextLeafPageId;
        return copy;
    }

    /**
     * Split this leaf and return the new right leaf.
     */
    public BTreeLeaf split(long newPageId) {
        int mid = keys.size() / 2;
        BTreeLeaf right = new BTreeLeaf(pageSize, newPageId);

        for (int i = mid; i < keys.size(); i++) {
            right.keys.add(keys.get(i));
            right.valueRecordIds.add(valueRecordIds.get(i));
        }

        right.nextLeafPageId = this.nextLeafPageId;
        this.nextLeafPageId = newPageId;

        while (keys.size() > mid) {
            keys.remove(keys.size() - 1);
            valueRecordIds.remove(valueRecordIds.size() - 1);
        }

        return right;
    }

    public byte[] getFirstKey() {
        return keys.isEmpty() ? null : keys.get(0);
    }

    public int size() {
        return keys.size();
    }

    public void deleteEntry(int index) {
        remove(index);
    }

    public byte[] toPage() {
        return serialize();
    }

    /**
     * Split result for leaf nodes.
     */
    public static class SplitResult {
        public final BTreeLeaf leftLeaf;
        public final BTreeLeaf rightLeaf;
        public final byte[] splitKey;

        public SplitResult(BTreeLeaf leftLeaf, BTreeLeaf rightLeaf, byte[] splitKey) {
            this.leftLeaf = leftLeaf;
            this.rightLeaf = rightLeaf;
            this.splitKey = splitKey;
        }
    }

    /**
     * Split this leaf and return SplitResult.
     */
    public SplitResult split() {
        int mid = keys.size() / 2;
        BTreeLeaf right = new BTreeLeaf(pageSize, 0);

        for (int i = mid; i < keys.size(); i++) {
            right.keys.add(keys.get(i));
            right.valueRecordIds.add(valueRecordIds.get(i));
        }

        byte[] splitKey = right.keys.get(0);

        BTreeLeaf left = new BTreeLeaf(pageSize, 0);
        for (int i = 0; i < mid; i++) {
            left.keys.add(keys.get(i));
            left.valueRecordIds.add(valueRecordIds.get(i));
        }

        return new SplitResult(left, right, splitKey);
    }

    private int calculateSerializedSize() {
        int size = DATA_START;
        for (byte[] key : keys) {
            size += 4 + key.length + 8; // keyLen + key + valueRecordId
        }
        return size;
    }

    @Override
    public byte[] serialize() {
        byte[] page = new byte[pageSize];
        ByteBuffer buf = ByteBuffer.wrap(page).order(ByteOrder.LITTLE_ENDIAN);

        // Skip header area
        buf.position(LEVEL_OFFSET);
        buf.putShort((short) 0); // level = 0 for leaf
        buf.putShort((short) keys.size());
        buf.putLong(nextLeafPageId);

        for (int i = 0; i < keys.size(); i++) {
            byte[] key = keys.get(i);
            buf.putInt(key.length);
            buf.put(key);
            buf.putLong(valueRecordIds.get(i));
        }

        return page;
    }

    public static BTreeLeaf fromPage(byte[] page, int pageSize, long pageId) {
        ByteBuffer buf = ByteBuffer.wrap(page).order(ByteOrder.LITTLE_ENDIAN);

        buf.position(LEVEL_OFFSET);
        buf.getShort(); // level (skip)
        int count = buf.getShort() & 0xFFFF;
        long nextLeafPageId = buf.getLong();

        BTreeLeaf leaf = new BTreeLeaf(pageSize, pageId);
        leaf.nextLeafPageId = nextLeafPageId;

        for (int i = 0; i < count; i++) {
            int keyLen = buf.getInt();
            byte[] key = new byte[keyLen];
            buf.get(key);
            long valueRecordId = buf.getLong();
            leaf.keys.add(key);
            leaf.valueRecordIds.add(valueRecordId);
        }

        return leaf;
    }
}
