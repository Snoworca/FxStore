package com.snoworca.fxstore.btree;

import com.snoworca.fxstore.storage.MemoryStorage;
import com.snoworca.fxstore.storage.Storage;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * BTree 에지 케이스 테스트
 *
 * <p>V16 커버리지 개선: first(), last(), find(), insert() 등의 빈 트리/루트 null 경로 테스트</p>
 *
 * <h3>테스트 대상</h3>
 * <ul>
 *   <li>first() - rootPageId == 0 경로</li>
 *   <li>last() - rootPageId == 0 경로</li>
 *   <li>find() - rootPageId == 0 경로</li>
 *   <li>sizeRecursive() - 재귀 크기 계산</li>
 *   <li>cursorWithRoot() - 범위 커서</li>
 * </ul>
 */
public class BTreeEdgeCaseTest {

    private static final int PAGE_SIZE = 4096;

    private Storage storage;
    private BTree tree;
    private Comparator<byte[]> keyComparator;

    @Before
    public void setUp() {
        storage = new MemoryStorage();
        keyComparator = (a, b) -> {
            int len = Math.min(a.length, b.length);
            for (int i = 0; i < len; i++) {
                int cmp = Byte.compare(a[i], b[i]);
                if (cmp != 0) return cmp;
            }
            return Integer.compare(a.length, b.length);
        };
        tree = new BTree(storage, PAGE_SIZE, keyComparator);
    }

    private byte[] key(int value) {
        return new byte[]{(byte) value};
    }

    // ==================== first() 에지 케이스 ====================

    @Test
    public void first_emptyTree_shouldReturnNull() {
        // Given: 빈 트리 (rootPageId == 0)
        assertTrue(tree.isEmpty());
        assertEquals(0, tree.getRootPageId());

        // When/Then: first()는 null 반환
        assertNull(tree.first());
    }

    @Test
    public void first_afterClear_shouldReturnNull() {
        // Given: 요소 추가 후 클리어
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        assertNotNull(tree.first());

        tree.clear();

        // When/Then: 빈 트리에서 first()는 null
        assertNull(tree.first());
    }

    @Test
    public void first_singleElement_shouldReturnThatElement() {
        tree.insert(key(50), 500L);

        BTree.Entry entry = tree.first();

        assertNotNull(entry);
        assertArrayEquals(key(50), entry.getKey());
        assertEquals(Long.valueOf(500L), entry.getValueRecordId());
    }

    @Test
    public void first_multipleElements_shouldReturnSmallest() {
        tree.insert(key(30), 300L);
        tree.insert(key(10), 100L);
        tree.insert(key(50), 500L);
        tree.insert(key(20), 200L);
        tree.insert(key(40), 400L);

        BTree.Entry entry = tree.first();

        assertNotNull(entry);
        assertArrayEquals(key(10), entry.getKey());
        assertEquals(Long.valueOf(100L), entry.getValueRecordId());
    }

    // ==================== last() 에지 케이스 ====================

    @Test
    public void last_emptyTree_shouldReturnNull() {
        // Given: 빈 트리 (rootPageId == 0)
        assertTrue(tree.isEmpty());

        // When/Then: last()는 null 반환
        assertNull(tree.last());
    }

    @Test
    public void last_afterClear_shouldReturnNull() {
        // Given: 요소 추가 후 클리어
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        assertNotNull(tree.last());

        tree.clear();

        // When/Then: 빈 트리에서 last()는 null
        assertNull(tree.last());
    }

    @Test
    public void last_singleElement_shouldReturnThatElement() {
        tree.insert(key(50), 500L);

        BTree.Entry entry = tree.last();

        assertNotNull(entry);
        assertArrayEquals(key(50), entry.getKey());
        assertEquals(Long.valueOf(500L), entry.getValueRecordId());
    }

    @Test
    public void last_multipleElements_shouldReturnLargest() {
        tree.insert(key(30), 300L);
        tree.insert(key(10), 100L);
        tree.insert(key(50), 500L);
        tree.insert(key(20), 200L);
        tree.insert(key(40), 400L);

        BTree.Entry entry = tree.last();

        assertNotNull(entry);
        assertArrayEquals(key(50), entry.getKey());
        assertEquals(Long.valueOf(500L), entry.getValueRecordId());
    }

    // ==================== find() 에지 케이스 ====================

    @Test
    public void find_emptyTree_shouldReturnNull() {
        assertNull(tree.find(key(10)));
    }

    @Test
    public void find_afterClear_shouldReturnNull() {
        tree.insert(key(10), 100L);
        assertNotNull(tree.find(key(10)));

        tree.clear();

        assertNull(tree.find(key(10)));
    }

    @Test
    public void find_existingKey_shouldReturnValueRecordId() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);

        Long value = tree.find(key(20));

        assertNotNull(value);
        assertEquals(Long.valueOf(200L), value);
    }

    @Test
    public void find_nonExistingKey_shouldReturnNull() {
        tree.insert(key(10), 100L);
        tree.insert(key(30), 300L);

        assertNull(tree.find(key(20))); // 존재하지 않는 키
    }

    // ==================== insert() 에지 케이스 ====================

    @Test
    public void insert_emptyTree_shouldCreateRoot() {
        // Given: 빈 트리
        assertEquals(0, tree.getRootPageId());

        // When: 첫 삽입
        tree.insert(key(10), 100L);

        // Then: 루트가 생성됨
        assertNotEquals(0, tree.getRootPageId());
        assertFalse(tree.isEmpty());
    }

    @Test
    public void insert_duplicateKey_shouldUpdateValue() {
        tree.insert(key(10), 100L);
        assertEquals(Long.valueOf(100L), tree.find(key(10)));

        tree.insert(key(10), 999L);

        assertEquals(Long.valueOf(999L), tree.find(key(10)));
    }

    // ==================== size() 에지 케이스 ====================

    @Test
    public void size_emptyTree_shouldReturnZero() {
        assertEquals(0, tree.size());
    }

    @Test
    public void size_afterInsert_shouldIncrement() {
        assertEquals(0, tree.size());

        tree.insert(key(10), 100L);
        assertEquals(1, tree.size());

        tree.insert(key(20), 200L);
        assertEquals(2, tree.size());

        tree.insert(key(30), 300L);
        assertEquals(3, tree.size());
    }

    @Test
    public void size_afterDelete_shouldDecrement() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);
        assertEquals(3, tree.size());

        tree.delete(key(20));
        assertEquals(2, tree.size());
    }

    @Test
    public void size_afterClear_shouldBeZero() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);
        assertEquals(3, tree.size());

        tree.clear();
        assertEquals(0, tree.size());
    }

    // ==================== get()/put()/remove() Map-like API ====================

    @Test
    public void get_emptyTree_shouldReturnNull() {
        assertNull(tree.get(key(10)));
    }

    @Test
    public void get_existingKey_shouldReturnValue() {
        tree.put(key(10), 100L);
        assertEquals(Long.valueOf(100L), tree.get(key(10)));
    }

    @Test
    public void put_shouldInsertOrUpdate() {
        tree.put(key(10), 100L);
        assertEquals(Long.valueOf(100L), tree.get(key(10)));

        tree.put(key(10), 200L);
        assertEquals(Long.valueOf(200L), tree.get(key(10)));
    }

    @Test
    public void remove_existingKey_shouldReturnValue() {
        tree.put(key(10), 100L);

        Long removed = tree.remove(key(10));

        assertEquals(Long.valueOf(100L), removed);
        assertNull(tree.get(key(10)));
    }

    @Test
    public void remove_nonExistingKey_shouldReturnNull() {
        tree.put(key(10), 100L);

        Long removed = tree.remove(key(20));

        assertNull(removed);
    }

    // ==================== cursor 에지 케이스 ====================

    @Test
    public void cursor_emptyTree_shouldHaveNoElements() {
        BTreeCursor cursor = tree.cursor();

        assertFalse(cursor.hasNext());
    }

    @Test
    public void cursor_singleElement_shouldIterateOnce() {
        tree.insert(key(10), 100L);

        BTreeCursor cursor = tree.cursor();

        assertTrue(cursor.hasNext());
        BTree.Entry entry = cursor.next();
        assertArrayEquals(key(10), entry.getKey());
        assertFalse(cursor.hasNext());
    }

    // ==================== 대량 데이터 테스트 ====================

    @Test
    public void largeTree_firstLastShouldWork() {
        // 많은 요소 삽입하여 트리 깊이 증가
        for (int i = 1; i <= 100; i++) {
            tree.insert(key(i), (long) (i * 10));
        }

        assertEquals(100, tree.size());

        BTree.Entry first = tree.first();
        assertNotNull(first);
        assertArrayEquals(key(1), first.getKey());

        BTree.Entry last = tree.last();
        assertNotNull(last);
        assertArrayEquals(key(100), last.getKey());
    }

    @Test
    public void largeTree_randomDeletion_firstLastShouldUpdate() {
        for (int i = 1; i <= 50; i++) {
            tree.insert(key(i), (long) (i * 10));
        }

        // 첫 번째 요소 삭제
        tree.delete(key(1));
        assertArrayEquals(key(2), tree.first().getKey());

        // 마지막 요소 삭제
        tree.delete(key(50));
        assertArrayEquals(key(49), tree.last().getKey());
    }

    // ==================== allocTail 테스트 ====================

    @Test
    public void getAllocTail_initialValue_shouldBeZero() {
        BTree freshTree = new BTree(storage, PAGE_SIZE, keyComparator);
        assertEquals(0, freshTree.getAllocTail());
    }

    @Test
    public void setAllocTail_shouldPersistValue() {
        tree.setAllocTail(12345L);
        assertEquals(12345L, tree.getAllocTail());
    }

    @Test
    public void setRootPageId_shouldUpdateRoot() {
        assertEquals(0, tree.getRootPageId());

        tree.setRootPageId(999L);

        assertEquals(999L, tree.getRootPageId());
    }

    // ==================== searchRelative (lower/floor/ceiling/higher) ====================

    @Test
    public void lower_emptyTree_shouldReturnNull() {
        assertNull(tree.lower(key(10)));
    }

    @Test
    public void lower_shouldFindStrictlyLess() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);

        BTree.Entry entry = tree.lower(key(25));
        assertNotNull(entry);
        assertArrayEquals(key(20), entry.getKey());

        // 정확히 일치하는 키의 경우 그보다 작은 키 반환
        entry = tree.lower(key(20));
        assertNotNull(entry);
        assertArrayEquals(key(10), entry.getKey());

        // 가장 작은 키보다 작은 경우
        assertNull(tree.lower(key(5)));
    }

    @Test
    public void floor_shouldReturnEntry() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);

        // floor()가 Entry를 반환하는지 테스트
        BTree.Entry entry = tree.floor(key(25));
        // 반환된 entry는 null이 아니어야 함
        assertNotNull(entry);
        // entry.getKey()가 key(25)보다 작거나 같아야 함 (byte 값 비교)
        byte[] entryKey = entry.getKey();
        assertTrue(entryKey.length > 0);
        assertTrue(entryKey[0] <= 25);
    }

    @Test
    public void ceiling_shouldFindGreaterOrEqual() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);

        // 일치하는 키가 있으면 그 키 반환
        BTree.Entry entry = tree.ceiling(key(20));
        assertNotNull(entry);
        assertArrayEquals(key(20), entry.getKey());

        // 일치하는 키가 없으면 더 큰 키 중 최소값
        entry = tree.ceiling(key(15));
        assertNotNull(entry);
        assertArrayEquals(key(20), entry.getKey());
    }

    @Test
    public void higher_shouldFindStrictlyGreater() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);

        BTree.Entry entry = tree.higher(key(15));
        assertNotNull(entry);
        assertArrayEquals(key(20), entry.getKey());

        // 정확히 일치하는 키의 경우 그보다 큰 키 반환
        entry = tree.higher(key(20));
        assertNotNull(entry);
        assertArrayEquals(key(30), entry.getKey());

        // 가장 큰 키보다 큰 경우
        assertNull(tree.higher(key(35)));
    }

    // ==================== withRoot 메서드들 ====================

    @Test
    public void firstEntryWithRoot_validRoot_shouldWork() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        long rootPageId = tree.getRootPageId();

        BTree.Entry entry = tree.firstEntryWithRoot(rootPageId);

        assertNotNull(entry);
        assertArrayEquals(key(10), entry.getKey());
    }

    @Test
    public void lastEntryWithRoot_validRoot_shouldWork() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        long rootPageId = tree.getRootPageId();

        BTree.Entry entry = tree.lastEntryWithRoot(rootPageId);

        assertNotNull(entry);
        assertArrayEquals(key(20), entry.getKey());
    }

    @Test
    public void findWithRoot_validRoot_shouldWork() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        long rootPageId = tree.getRootPageId();

        Long valueRecordId = tree.findWithRoot(rootPageId, key(20));

        assertNotNull(valueRecordId);
        assertEquals(Long.valueOf(200L), valueRecordId);
    }

    @Test
    public void cursorWithRoot_validRoot_shouldIterate() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);
        long rootPageId = tree.getRootPageId();

        BTreeCursor cursor = tree.cursorWithRoot(rootPageId);

        int count = 0;
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void descendingCursorWithRoot_shouldIterateReverse() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);
        long rootPageId = tree.getRootPageId();

        java.util.Iterator<BTree.Entry> cursor = tree.descendingCursorWithRoot(rootPageId);

        assertTrue(cursor.hasNext());
        // Descending order
        BTree.Entry first = cursor.next();
        assertArrayEquals(key(30), first.getKey());
    }

    // ==================== 범위 커서 테스트 ====================

    @Test
    public void cursorWithRange_shouldFilterElements() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);
        tree.insert(key(40), 400L);
        tree.insert(key(50), 500L);

        // 20 <= key <= 40 범위
        BTreeCursor cursor = tree.cursor(key(20), key(40), true, true);

        int count = 0;
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            int keyVal = entry.getKey()[0];
            assertTrue(keyVal >= 20 && keyVal <= 40);
            count++;
        }
        assertEquals(3, count); // 20, 30, 40
    }

    @Test
    public void cursorWithRange_exclusiveBounds_shouldWork() {
        tree.insert(key(10), 100L);
        tree.insert(key(20), 200L);
        tree.insert(key(30), 300L);
        tree.insert(key(40), 400L);
        tree.insert(key(50), 500L);

        // 20 < key < 40 범위
        BTreeCursor cursor = tree.cursor(key(20), key(40), false, false);

        int count = 0;
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            int keyVal = entry.getKey()[0];
            assertTrue(keyVal > 20 && keyVal < 40);
            count++;
        }
        assertEquals(1, count); // 30만
    }
}
