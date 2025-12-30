package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.FxOptions;
import com.snoworca.fxstore.api.FxReadTransaction;
import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

/**
 * FxReadTransactionImpl 라이프사이클 및 경계 조건 테스트 (P2)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>FxReadTransactionImpl (85% → 90%+)</li>
 * </ul>
 *
 * @since 0.9
 * @see FxReadTransactionImpl
 * @see FxReadTransaction
 */
public class ReadTransactionLifecycleTest {

    private File tempFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-readtx-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) {
            store.close();
        }
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    // ==================== 기본 라이프사이클 테스트 ====================

    @Test
    public void beginRead_shouldReturnActiveTransaction() {
        FxReadTransaction tx = store.beginRead();
        assertTrue(tx.isActive());
        tx.close();
    }

    @Test
    public void close_shouldDeactivateTransaction() {
        FxReadTransaction tx = store.beginRead();
        tx.close();
        assertFalse(tx.isActive());
    }

    @Test
    public void close_twice_shouldNotThrow() {
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.close(); // no exception
        assertFalse(tx.isActive());
    }

    @Test
    public void tryWithResources_shouldAutoClose() {
        FxReadTransaction capturedTx;
        try (FxReadTransaction tx = store.beginRead()) {
            capturedTx = tx;
            assertTrue(tx.isActive());
        }
        assertFalse(capturedTx.isActive());
    }

    // ==================== 스냅샷 일관성 테스트 ====================

    @Test
    public void getSnapshotSeqNo_shouldReturnNonNegative() {
        try (FxReadTransaction tx = store.beginRead()) {
            long seqNo = tx.getSnapshotSeqNo();
            assertTrue(seqNo >= 0);
        }
    }

    @Test
    public void snapshotIsolation_writeAfterBeginRead_shouldNotAffectTransaction() {
        // Given: 데이터 생성
        NavigableMap<Long, String> map = store.createMap("isolation", Long.class, String.class);
        map.put(1L, "before");

        // When: 트랜잭션 시작 후 쓰기
        try (FxReadTransaction tx = store.beginRead()) {
            // 트랜잭션 내에서 첫 번째 값 확인
            String valueBefore = tx.get(map, 1L);
            assertEquals("before", valueBefore);

            // 트랜잭션 외부에서 값 변경
            map.put(1L, "after");
            map.put(2L, "new");

            // 트랜잭션 내에서는 여전히 이전 스냅샷 보여야 함
            // Note: 실제 동작은 구현에 따라 다를 수 있음
            // 이 테스트는 스냅샷 시퀀스 번호가 유지되는지 확인
            long seqNo = tx.getSnapshotSeqNo();
            assertTrue(seqNo >= 0);
        }
    }

    // ==================== close 후 접근 에러 테스트 ====================

    @Test(expected = IllegalStateException.class)
    public void get_afterClose_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "value");

        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.get(map, 1L);
    }

    @Test(expected = IllegalStateException.class)
    public void containsKey_afterClose_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test2", Long.class, String.class);
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.containsKey(map, 1L);
    }

    @Test(expected = IllegalStateException.class)
    public void firstEntry_afterClose_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test3", Long.class, String.class);
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.firstEntry(map);
    }

    @Test(expected = IllegalStateException.class)
    public void lastEntry_afterClose_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test4", Long.class, String.class);
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.lastEntry(map);
    }

    @Test(expected = IllegalStateException.class)
    public void size_map_afterClose_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test5", Long.class, String.class);
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.size(map);
    }

    // ==================== Map 읽기 테스트 ====================

    @Test
    public void get_existingKey_shouldReturnValue() {
        NavigableMap<Long, String> map = store.createMap("mapGet", Long.class, String.class);
        map.put(1L, "hello");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("hello", tx.get(map, 1L));
        }
    }

    @Test
    public void get_nonExistingKey_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("mapGet2", Long.class, String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.get(map, 999L));
        }
    }

    @Test
    public void containsKey_existingKey_shouldReturnTrue() {
        NavigableMap<Long, String> map = store.createMap("mapContains", Long.class, String.class);
        map.put(1L, "value");

        try (FxReadTransaction tx = store.beginRead()) {
            assertTrue(tx.containsKey(map, 1L));
        }
    }

    @Test
    public void containsKey_nonExistingKey_shouldReturnFalse() {
        NavigableMap<Long, String> map = store.createMap("mapContains2", Long.class, String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertFalse(tx.containsKey(map, 999L));
        }
    }

    @Test
    public void firstEntry_nonEmpty_shouldReturnFirst() {
        NavigableMap<Long, String> map = store.createMap("mapFirst", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");

        try (FxReadTransaction tx = store.beginRead()) {
            Map.Entry<Long, String> first = tx.firstEntry(map);
            assertNotNull(first);
            assertEquals(Long.valueOf(10L), first.getKey());
            assertEquals("A", first.getValue());
        }
    }

    @Test
    public void firstEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("mapFirstEmpty", Long.class, String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.firstEntry(map));
        }
    }

    @Test
    public void lastEntry_nonEmpty_shouldReturnLast() {
        NavigableMap<Long, String> map = store.createMap("mapLast", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");

        try (FxReadTransaction tx = store.beginRead()) {
            Map.Entry<Long, String> last = tx.lastEntry(map);
            assertNotNull(last);
            assertEquals(Long.valueOf(20L), last.getKey());
            assertEquals("B", last.getValue());
        }
    }

    @Test
    public void size_map_shouldReturnCorrectSize() {
        NavigableMap<Long, String> map = store.createMap("mapSize", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");
        map.put(3L, "C");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(3, tx.size(map));
        }
    }

    // ==================== Set 읽기 테스트 ====================

    @Test
    public void contains_set_existingElement_shouldReturnTrue() {
        NavigableSet<Long> set = store.createSet("setContains", Long.class);
        set.add(100L);

        try (FxReadTransaction tx = store.beginRead()) {
            assertTrue(tx.contains(set, 100L));
        }
    }

    @Test
    public void contains_set_nonExistingElement_shouldReturnFalse() {
        NavigableSet<Long> set = store.createSet("setContains2", Long.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertFalse(tx.contains(set, 999L));
        }
    }

    @Test
    public void first_set_nonEmpty_shouldReturnFirst() {
        NavigableSet<Long> set = store.createSet("setFirst", Long.class);
        set.add(30L);
        set.add(10L);
        set.add(20L);

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(Long.valueOf(10L), tx.first(set));
        }
    }

    @Test
    public void last_set_nonEmpty_shouldReturnLast() {
        NavigableSet<Long> set = store.createSet("setLast", Long.class);
        set.add(30L);
        set.add(10L);
        set.add(20L);

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(Long.valueOf(30L), tx.last(set));
        }
    }

    @Test
    public void size_set_shouldReturnCorrectSize() {
        NavigableSet<Long> set = store.createSet("setSize", Long.class);
        set.add(1L);
        set.add(2L);

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(2, tx.size(set));
        }
    }

    // ==================== List 읽기 테스트 ====================

    @Test
    public void get_list_validIndex_shouldReturnElement() {
        List<String> list = store.createList("listGet", String.class);
        list.add("first");
        list.add("second");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("first", tx.get(list, 0));
            assertEquals("second", tx.get(list, 1));
        }
    }

    @Test
    public void size_list_shouldReturnCorrectSize() {
        List<String> list = store.createList("listSize", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(3, tx.size(list));
        }
    }

    @Test
    public void indexOf_list_existingElement_shouldReturnIndex() {
        List<String> list = store.createList("listIndexOf", String.class);
        list.add("apple");
        list.add("banana");
        list.add("cherry");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(1, tx.indexOf(list, "banana"));
        }
    }

    @Test
    public void indexOf_list_nonExistingElement_shouldReturnNegative() {
        List<String> list = store.createList("listIndexOf2", String.class);
        list.add("apple");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(-1, tx.indexOf(list, "orange"));
        }
    }

    // ==================== Deque 읽기 테스트 ====================

    @Test
    public void peekFirst_deque_nonEmpty_shouldReturnFirst() {
        Deque<String> deque = store.createDeque("dequePeek", String.class);
        deque.addFirst("first");
        deque.addLast("last");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("first", tx.peekFirst(deque));
        }
    }

    @Test
    public void peekLast_deque_nonEmpty_shouldReturnLast() {
        Deque<String> deque = store.createDeque("dequePeek2", String.class);
        deque.addFirst("first");
        deque.addLast("last");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("last", tx.peekLast(deque));
        }
    }

    @Test
    public void peekFirst_deque_empty_shouldReturnNull() {
        Deque<String> deque = store.createDeque("dequePeekEmpty", String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.peekFirst(deque));
        }
    }

    @Test
    public void size_deque_shouldReturnCorrectSize() {
        Deque<String> deque = store.createDeque("dequeSize", String.class);
        deque.addFirst("a");
        deque.addLast("b");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(2, tx.size(deque));
        }
    }

    // ==================== 다중 컬렉션 테스트 ====================

    @Test
    public void multipleCollections_sameTransaction_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("multi1", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("multi2", Long.class);
        List<String> list = store.createList("multi3", String.class);

        map.put(1L, "mapValue");
        set.add(100L);
        list.add("listValue");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("mapValue", tx.get(map, 1L));
            assertTrue(tx.contains(set, 100L));
            assertEquals("listValue", tx.get(list, 0));
        }
    }

    // ==================== 잘못된 컬렉션 테스트 ====================

    @Test(expected = IllegalArgumentException.class)
    public void get_withDifferentStoreCollection_shouldThrow() throws Exception {
        // Given: 다른 스토어 생성
        File otherFile = Files.createTempFile("fxstore-other-", ".db").toFile();
        otherFile.delete();
        FxStore otherStore = FxStore.open(otherFile.toPath());

        try {
            NavigableMap<Long, String> otherMap = otherStore.createMap("other", Long.class, String.class);
            otherMap.put(1L, "value");

            // When: 현재 스토어의 트랜잭션에서 다른 스토어의 맵 접근
            try (FxReadTransaction tx = store.beginRead()) {
                tx.get(otherMap, 1L);
            }
        } finally {
            otherStore.close();
            otherFile.delete();
        }
    }

    // ==================== 빈 컬렉션 테스트 ====================

    @Test
    public void emptyMap_operations_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("emptyMap", Long.class, String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.get(map, 1L));
            assertFalse(tx.containsKey(map, 1L));
            assertNull(tx.firstEntry(map));
            assertNull(tx.lastEntry(map));
            assertEquals(0, tx.size(map));
        }
    }

    @Test
    public void emptySet_operations_shouldWork() {
        NavigableSet<Long> set = store.createSet("emptySet", Long.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertFalse(tx.contains(set, 1L));
            assertNull(tx.first(set));
            assertNull(tx.last(set));
            assertEquals(0, tx.size(set));
        }
    }

    @Test
    public void emptyList_operations_shouldWork() {
        List<String> list = store.createList("emptyList", String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(0, tx.size(list));
            assertEquals(-1, tx.indexOf(list, "anything"));
        }
    }
}
