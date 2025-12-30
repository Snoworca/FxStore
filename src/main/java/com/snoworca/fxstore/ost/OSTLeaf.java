package com.snoworca.fxstore.ost;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Order-Statistic Tree leaf node.
 * Contains value references.
 */
public class OSTLeaf implements OSTNode {

    private long pageId;
    private final List<Long> valueRefs;
    private long nextLeafPageId;

    public OSTLeaf(long pageId) {
        this.pageId = pageId;
        this.valueRefs = new ArrayList<>();
        this.nextLeafPageId = 0;
    }

    /**
     * Default constructor for temporary leaf.
     */
    public OSTLeaf() {
        this(0);
    }

    @Override
    public long getPageId() {
        return pageId;
    }

    @Override
    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public int getSubtreeCount() {
        return valueRefs.size();
    }

    /**
     * Alias for getSubtreeCount.
     */
    public int subtreeCount() {
        return valueRefs.size();
    }

    public int getValueCount() {
        return valueRefs.size();
    }

    public long getValueRef(int index) {
        return valueRefs.get(index);
    }

    /**
     * Alias for getValueRef.
     */
    public long getElementRecordId(int index) {
        return valueRefs.get(index);
    }

    public void setValueRef(int index, long valueRef) {
        valueRefs.set(index, valueRef);
    }

    public long getNextLeafPageId() {
        return nextLeafPageId;
    }

    public void setNextLeafPageId(long nextLeafPageId) {
        this.nextLeafPageId = nextLeafPageId;
    }

    public void insert(int index, long valueRef) {
        valueRefs.add(index, valueRef);
    }

    public void add(long valueRef) {
        valueRefs.add(valueRef);
    }

    /**
     * Alias for add.
     */
    public void addElement(long elementRecordId) {
        valueRefs.add(elementRecordId);
    }

    public long remove(int index) {
        return valueRefs.remove(index);
    }

    public boolean needsSplit(int maxEntries) {
        return valueRefs.size() > maxEntries;
    }

    public boolean canMerge(int minEntries) {
        return valueRefs.size() < minEntries;
    }

    /**
     * Split this leaf and return new right leaf.
     */
    public OSTLeaf split(long newPageId) {
        int mid = valueRefs.size() / 2;
        OSTLeaf right = new OSTLeaf(newPageId);

        for (int i = mid; i < valueRefs.size(); i++) {
            right.valueRefs.add(valueRefs.get(i));
        }

        right.nextLeafPageId = this.nextLeafPageId;
        this.nextLeafPageId = newPageId;

        while (valueRefs.size() > mid) {
            valueRefs.remove(valueRefs.size() - 1);
        }

        return right;
    }

    @Override
    public byte[] serialize() {
        int size = 2 + 8 + valueRefs.size() * 8; // count + nextLeaf + valueRefs
        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

        buf.putShort((short) valueRefs.size());
        buf.putLong(nextLeafPageId);

        for (Long valueRef : valueRefs) {
            buf.putLong(valueRef);
        }

        return buf.array();
    }

    public static OSTLeaf deserialize(long pageId, byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int count = buf.getShort() & 0xFFFF;
        long nextLeafPageId = buf.getLong();

        OSTLeaf leaf = new OSTLeaf(pageId);
        leaf.nextLeafPageId = nextLeafPageId;

        for (int i = 0; i < count; i++) {
            leaf.valueRefs.add(buf.getLong());
        }

        return leaf;
    }

    /**
     * Deserialize from page bytes (pageId defaults to 0).
     * Expects page type byte at position 0 (skipped).
     */
    public static OSTLeaf fromPage(byte[] data) {
        // Skip page type byte at position 0
        byte[] dataWithoutType = new byte[data.length - 1];
        System.arraycopy(data, 1, dataWithoutType, 0, dataWithoutType.length);
        return deserialize(0, dataWithoutType);
    }

    /**
     * Serialize to page of specified size.
     * Includes page type byte (1 = LEAF) at position 0.
     */
    public byte[] toPage(int pageSize) {
        byte[] page = new byte[pageSize];
        page[0] = 1; // Page type: LEAF
        byte[] serialized = serialize();
        System.arraycopy(serialized, 0, page, 1, serialized.length);
        return page;
    }
}
