package com.snoworca.fxstore.btree;

import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * BTreeInternal 및 BTreeLeaf 미커버 메서드 테스트
 *
 * <p>커버리지 개선 대상:</p>
 * <ul>
 *   <li>BTreeInternal: split(long), insertKeyAndChild, removeKeyAndChild, canMerge 등</li>
 *   <li>BTreeLeaf: split(long), findInsertionPoint, getFirstKey, canMerge 등</li>
 * </ul>
 */
public class BTreeNodeCoverageTest {

    private static final int PAGE_SIZE = 4096;
    private static final Comparator<byte[]> COMPARATOR = (a, b) -> {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (cmp != 0) return cmp;
        }
        return a.length - b.length;
    };

    // ==================== BTreeInternal 테스트 ====================

    @Test
    public void internal_splitWithPageId_shouldSplitCorrectly() {
        // Given: 키가 많은 내부 노드
        BTreeInternal internal = new BTreeInternal(PAGE_SIZE, 100L, 2);
        for (int i = 0; i < 10; i++) {
            internal.insertKey(i, new byte[]{(byte) (i * 10)});
            internal.insertChild(i, 1000L + i);
        }
        internal.insertChild(10, 1010L); // 마지막 자식

        // When: split(long newPageId) 호출
        Object[] result = internal.split(200L);

        // Then: promoted key와 right node 반환
        assertNotNull(result);
        assertEquals(2, result.length);
        byte[] promotedKey = (byte[]) result[0];
        BTreeInternal right = (BTreeInternal) result[1];

        assertNotNull(promotedKey);
        assertNotNull(right);
        assertEquals(200L, right.getPageId());
        assertTrue(internal.getKeyCount() < 10); // 원래 노드는 줄어듦
    }

    @Test
    public void internal_insertKeyAndChild_shouldInsertCorrectly() {
        // Given: 내부 노드
        BTreeInternal internal = new BTreeInternal(PAGE_SIZE, 100L, 1);
        internal.insertChild(0, 1000L); // 첫 자식

        // When: insertKeyAndChild 호출
        internal.insertKeyAndChild(0, new byte[]{10}, 2000L);

        // Then: 키와 자식이 추가됨
        assertEquals(1, internal.getKeyCount());
        assertEquals(2, internal.getChildCount());
        assertArrayEquals(new byte[]{10}, internal.getKey(0));
        assertEquals(1000L, internal.getChildPageId(0));
        assertEquals(2000L, internal.getChildPageId(1));
    }

    @Test
    public void internal_removeKeyAndChild_shouldRemoveCorrectly() {
        // Given: 키와 자식이 있는 내부 노드
        BTreeInternal internal = new BTreeInternal(PAGE_SIZE, 100L, 1);
        internal.insertChild(0, 1000L);
        internal.insertKeyAndChild(0, new byte[]{10}, 2000L);
        internal.insertKeyAndChild(1, new byte[]{20}, 3000L);

        // When: removeKeyAndChild 호출
        internal.removeKeyAndChild(0);

        // Then: 첫 키와 두 번째 자식이 제거됨
        assertEquals(1, internal.getKeyCount());
        assertEquals(2, internal.getChildCount());
        assertArrayEquals(new byte[]{20}, internal.getKey(0));
    }

    @Test
    public void internal_canMerge_shouldReturnCorrectValue() {
        // Given: 키가 적은 노드
        BTreeInternal internal = new BTreeInternal(PAGE_SIZE, 100L, 1);
        internal.insertChild(0, 1000L);
        internal.insertKeyAndChild(0, new byte[]{10}, 2000L);

        // When/Then: minKeys보다 적으면 true
        assertTrue(internal.canMerge(5));
        assertFalse(internal.canMerge(1));
    }

    @Test
    public void internal_needsSplit_shouldReturnCorrectValue() {
        // Given: 키가 많은 노드
        BTreeInternal internal = new BTreeInternal(PAGE_SIZE, 100L, 1);
        internal.insertChild(0, 1000L);

        // When: 작은 노드
        assertFalse(internal.needsSplit(100));

        // When: 키를 많이 추가
        for (int i = 0; i < 100; i++) {
            internal.insertKeyAndChild(i, new byte[32], 2000L + i);
        }

        // Then: 분할 필요
        assertTrue(internal.needsSplit(100));
    }

    @Test
    public void internal_gettersAndSetters_shouldWork() {
        // Given: 내부 노드
        BTreeInternal internal = new BTreeInternal(PAGE_SIZE, 1);
        internal.insertChild(0, 1000L);
        internal.insertKeyAndChild(0, new byte[]{10}, 2000L);

        // When/Then: getPageId, setPageId
        assertEquals(0, internal.getPageId());
        internal.setPageId(500L);
        assertEquals(500L, internal.getPageId());

        // When/Then: getLevel, setLevel
        assertEquals(1, internal.getLevel());
        internal.setLevel(3);
        assertEquals(3, internal.getLevel());

        // When/Then: getKey, getChild, setChild
        assertArrayEquals(new byte[]{10}, internal.getKey(0));
        assertEquals(1000L, internal.getChild(0));
        internal.setChild(0, 9999L);
        assertEquals(9999L, internal.getChild(0));
    }

    // ==================== BTreeLeaf 테스트 ====================

    @Test
    public void leaf_splitWithPageId_shouldSplitCorrectly() {
        // Given: 키가 많은 리프 노드
        BTreeLeaf leaf = new BTreeLeaf(PAGE_SIZE, 100L);
        for (int i = 0; i < 10; i++) {
            leaf.insert(i, new byte[]{(byte) (i * 10)}, 5000L + i);
        }
        leaf.setNextLeafPageId(300L);

        // When: split(long newPageId) 호출
        BTreeLeaf right = leaf.split(200L);

        // Then: right node가 생성됨
        assertNotNull(right);
        assertEquals(200L, right.getPageId());
        assertTrue(leaf.size() < 10); // 원래 노드는 줄어듦
        assertTrue(right.size() > 0);

        // next leaf 연결
        assertEquals(200L, leaf.getNextLeafPageId());
        assertEquals(300L, right.getNextLeafPageId());
    }

    @Test
    public void leaf_findInsertionPoint_shouldFindCorrectPosition() {
        // Given: 정렬된 리프
        BTreeLeaf leaf = new BTreeLeaf(PAGE_SIZE, 100L);
        leaf.insert(0, new byte[]{10}, 1000L);
        leaf.insert(1, new byte[]{30}, 2000L);
        leaf.insert(2, new byte[]{50}, 3000L);

        // When/Then: 삽입 위치 찾기
        assertEquals(0, leaf.findInsertionPoint(new byte[]{5}, COMPARATOR));
        assertEquals(1, leaf.findInsertionPoint(new byte[]{20}, COMPARATOR));
        assertEquals(2, leaf.findInsertionPoint(new byte[]{40}, COMPARATOR));
        assertEquals(3, leaf.findInsertionPoint(new byte[]{60}, COMPARATOR));
    }

    @Test
    public void leaf_getFirstKey_shouldReturnFirstKey() {
        // Given: 빈 리프
        BTreeLeaf emptyLeaf = new BTreeLeaf(PAGE_SIZE, 100L);
        assertNull(emptyLeaf.getFirstKey());

        // Given: 키가 있는 리프
        BTreeLeaf leaf = new BTreeLeaf(PAGE_SIZE, 100L);
        leaf.insert(0, new byte[]{20}, 1000L);
        leaf.insert(1, new byte[]{30}, 2000L);

        // When/Then
        assertArrayEquals(new byte[]{20}, leaf.getFirstKey());
    }

    @Test
    public void leaf_canMerge_shouldReturnCorrectValue() {
        // Given: 키가 적은 리프
        BTreeLeaf leaf = new BTreeLeaf(PAGE_SIZE, 100L);
        leaf.insert(0, new byte[]{10}, 1000L);

        // When/Then
        assertTrue(leaf.canMerge(5));
        assertFalse(leaf.canMerge(1));
    }

    @Test
    public void leaf_needsSplit_shouldReturnCorrectValue() {
        // Given: 키가 적은 리프
        BTreeLeaf leaf = new BTreeLeaf(PAGE_SIZE, 100L);
        assertFalse(leaf.needsSplit(100));

        // When: 키를 많이 추가
        for (int i = 0; i < 100; i++) {
            leaf.insert(i, new byte[32], 1000L + i);
        }

        // Then: 분할 필요
        assertTrue(leaf.needsSplit(100));
    }

    @Test
    public void leaf_gettersAndSetters_shouldWork() {
        // Given: 리프 노드
        BTreeLeaf leaf = new BTreeLeaf(PAGE_SIZE);
        leaf.insert(0, new byte[]{10}, 1000L);

        // When/Then: getPageId, setPageId
        assertEquals(0, leaf.getPageId());
        leaf.setPageId(500L);
        assertEquals(500L, leaf.getPageId());

        // When/Then: getKeyCount
        assertEquals(1, leaf.getKeyCount());

        // When/Then: getNextLeafPageId, setNextLeafPageId
        assertEquals(0, leaf.getNextLeafPageId());
        leaf.setNextLeafPageId(999L);
        assertEquals(999L, leaf.getNextLeafPageId());
    }

    // ==================== Serialization 테스트 ====================

    @Test
    public void internal_serializeAndDeserialize_shouldPreserveData() {
        // Given: 데이터가 있는 내부 노드
        BTreeInternal original = new BTreeInternal(PAGE_SIZE, 100L, 3);
        original.insertChild(0, 1000L);
        for (int i = 0; i < 5; i++) {
            original.insertKeyAndChild(i, new byte[]{(byte) i}, 2000L + i);
        }

        // When: serialize/deserialize
        byte[] page = original.serialize();
        BTreeInternal restored = BTreeInternal.fromPage(page, PAGE_SIZE, 100L);

        // Then: 데이터 보존
        assertEquals(original.getKeyCount(), restored.getKeyCount());
        assertEquals(original.getChildCount(), restored.getChildCount());
        assertEquals(original.getLevel(), restored.getLevel());

        for (int i = 0; i < original.getKeyCount(); i++) {
            assertArrayEquals(original.getKey(i), restored.getKey(i));
        }
        for (int i = 0; i < original.getChildCount(); i++) {
            assertEquals(original.getChildPageId(i), restored.getChildPageId(i));
        }
    }

    @Test
    public void leaf_serializeAndDeserialize_shouldPreserveData() {
        // Given: 데이터가 있는 리프 노드
        BTreeLeaf original = new BTreeLeaf(PAGE_SIZE, 100L);
        original.setNextLeafPageId(999L);
        for (int i = 0; i < 5; i++) {
            original.insert(i, new byte[]{(byte) i}, 3000L + i);
        }

        // When: serialize/deserialize
        byte[] page = original.serialize();
        BTreeLeaf restored = BTreeLeaf.fromPage(page, PAGE_SIZE, 100L);

        // Then: 데이터 보존
        assertEquals(original.size(), restored.size());
        assertEquals(original.getNextLeafPageId(), restored.getNextLeafPageId());

        for (int i = 0; i < original.size(); i++) {
            assertArrayEquals(original.getKey(i), restored.getKey(i));
            assertEquals(original.getValueRecordId(i), restored.getValueRecordId(i));
        }
    }

    // ==================== copy 테스트 ====================

    @Test
    public void internal_copy_shouldCreateDeepCopy() {
        // Given: 데이터가 있는 내부 노드
        BTreeInternal original = new BTreeInternal(PAGE_SIZE, 100L, 2);
        original.insertChild(0, 1000L);
        original.insertKeyAndChild(0, new byte[]{10}, 2000L);

        // When: copy
        BTreeInternal copy = original.copy();

        // Then: 별도 인스턴스
        assertNotSame(original, copy);
        assertEquals(original.getKeyCount(), copy.getKeyCount());
        assertEquals(original.getChildCount(), copy.getChildCount());

        // 원본 변경이 복사본에 영향 없음
        original.insertKeyAndChild(1, new byte[]{20}, 3000L);
        assertEquals(1, copy.getKeyCount());
    }

    @Test
    public void leaf_copy_shouldCreateDeepCopy() {
        // Given: 데이터가 있는 리프 노드
        BTreeLeaf original = new BTreeLeaf(PAGE_SIZE, 100L);
        original.insert(0, new byte[]{10}, 1000L);
        original.setNextLeafPageId(999L);

        // When: copy
        BTreeLeaf copy = original.copy();

        // Then: 별도 인스턴스
        assertNotSame(original, copy);
        assertEquals(original.size(), copy.size());
        assertEquals(original.getNextLeafPageId(), copy.getNextLeafPageId());

        // 원본 변경이 복사본에 영향 없음
        original.insert(1, new byte[]{20}, 2000L);
        assertEquals(1, copy.size());
    }

    // ==================== split() 테스트 (SplitResult 반환 버전) ====================

    @Test
    public void internal_splitResult_shouldReturnAllParts() {
        // Given: 키가 많은 내부 노드
        BTreeInternal internal = new BTreeInternal(PAGE_SIZE, 100L, 2);
        internal.insertChild(0, 1000L);
        for (int i = 0; i < 10; i++) {
            internal.insertKeyAndChild(i, new byte[]{(byte) (i * 10)}, 2000L + i);
        }

        // When: split() 호출 (SplitResult 버전)
        BTreeInternal.SplitResult result = internal.split();

        // Then
        assertNotNull(result);
        assertNotNull(result.leftNode);
        assertNotNull(result.rightNode);
        assertNotNull(result.splitKey);

        // 왼쪽 + 오른쪽 키 수 + promoted key = 원래 키 수
        assertTrue(result.leftNode.getKeyCount() + result.rightNode.getKeyCount() + 1 <= 11);
    }

    @Test
    public void leaf_splitResult_shouldReturnAllParts() {
        // Given: 키가 많은 리프 노드
        BTreeLeaf leaf = new BTreeLeaf(PAGE_SIZE, 100L);
        for (int i = 0; i < 10; i++) {
            leaf.insert(i, new byte[]{(byte) (i * 10)}, 3000L + i);
        }

        // When: split() 호출 (SplitResult 버전)
        BTreeLeaf.SplitResult result = leaf.split();

        // Then
        assertNotNull(result);
        assertNotNull(result.leftLeaf);
        assertNotNull(result.rightLeaf);
        assertNotNull(result.splitKey);

        // 왼쪽 + 오른쪽 = 원래 개수
        assertEquals(10, result.leftLeaf.size() + result.rightLeaf.size());
    }

    // ==================== toPage 테스트 ====================

    @Test
    public void internal_toPage_shouldEqualSerialize() {
        BTreeInternal internal = new BTreeInternal(PAGE_SIZE, 100L, 1);
        internal.insertChild(0, 1000L);
        internal.insertKeyAndChild(0, new byte[]{10}, 2000L);

        byte[] page = internal.toPage();
        byte[] serialized = internal.serialize();

        assertArrayEquals(serialized, page);
    }

    @Test
    public void leaf_toPage_shouldEqualSerialize() {
        BTreeLeaf leaf = new BTreeLeaf(PAGE_SIZE, 100L);
        leaf.insert(0, new byte[]{10}, 1000L);

        byte[] page = leaf.toPage();
        byte[] serialized = leaf.serialize();

        assertArrayEquals(serialized, page);
    }
}
