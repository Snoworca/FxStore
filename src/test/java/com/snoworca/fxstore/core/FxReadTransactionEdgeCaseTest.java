package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Deque;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;

import static org.junit.Assert.*;

/**
 * FxReadTransaction 에지 케이스 테스트
 *
 * <p>V16 커버리지 개선: 읽기 트랜잭션의 빈 컬렉션, 타입 검증 경로 테스트</p>
 *
 * <h3>테스트 대상</h3>
 * <ul>
 *   <li>peekFirst/peekLast on empty Deque</li>
 *   <li>size() on empty collections via transaction</li>
 *   <li>first/last on empty Set/Map</li>
 *   <li>firstEntry/lastEntry on empty Map</li>
 * </ul>
 */
public class FxReadTransactionEdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("read-tx-edge-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== Deque peekFirst/peekLast 테스트 ====================

    @Test
    public void peekFirst_emptyDeque_viaTransaction_shouldReturnNull() {
        Deque<String> deque = store.createDeque("test", String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            String result = tx.peekFirst(deque);
            assertNull(result);
        }
    }

    @Test
    public void peekLast_emptyDeque_viaTransaction_shouldReturnNull() {
        Deque<String> deque = store.createDeque("test", String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            String result = tx.peekLast(deque);
            assertNull(result);
        }
    }

    @Test
    public void peekFirst_nonEmptyDeque_viaTransaction_shouldReturnFirst() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            String result = tx.peekFirst(deque);
            assertEquals("a", result);
        }
    }

    @Test
    public void peekLast_nonEmptyDeque_viaTransaction_shouldReturnLast() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            String result = tx.peekLast(deque);
            assertEquals("c", result);
        }
    }

    // ==================== Deque size() 테스트 ====================

    @Test
    public void size_emptyDeque_viaTransaction_shouldReturnZero() {
        Deque<String> deque = store.createDeque("test", String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            int size = tx.size(deque);
            assertEquals(0, size);
        }
    }

    @Test
    public void size_nonEmptyDeque_viaTransaction_shouldReturnCount() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            int size = tx.size(deque);
            assertEquals(3, size);
        }
    }

    // ==================== Map via Transaction 테스트 ====================

    @Test
    public void firstEntry_emptyMap_viaTransaction_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            java.util.Map.Entry<Long, String> result = tx.firstEntry(map);
            assertNull(result);
        }
    }

    @Test
    public void lastEntry_emptyMap_viaTransaction_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            java.util.Map.Entry<Long, String> result = tx.lastEntry(map);
            assertNull(result);
        }
    }

    @Test
    public void firstEntry_nonEmptyMap_viaTransaction_shouldReturnFirst() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");
        map.put(5L, "five");
        map.put(20L, "twenty");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            java.util.Map.Entry<Long, String> result = tx.firstEntry(map);
            assertNotNull(result);
            assertEquals(Long.valueOf(5L), result.getKey());
            assertEquals("five", result.getValue());
        }
    }

    @Test
    public void lastEntry_nonEmptyMap_viaTransaction_shouldReturnLast() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");
        map.put(5L, "five");
        map.put(20L, "twenty");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            java.util.Map.Entry<Long, String> result = tx.lastEntry(map);
            assertNotNull(result);
            assertEquals(Long.valueOf(20L), result.getKey());
            assertEquals("twenty", result.getValue());
        }
    }

    @Test
    public void get_emptyMap_viaTransaction_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            String result = tx.get(map, 10L);
            assertNull(result);
        }
    }

    @Test
    public void get_nonExistingKey_viaTransaction_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            String result = tx.get(map, 999L);
            assertNull(result);
        }
    }

    @Test
    public void containsKey_emptyMap_viaTransaction_shouldReturnFalse() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            boolean result = tx.containsKey(map, 10L);
            assertFalse(result);
        }
    }

    @Test
    public void size_emptyMap_viaTransaction_shouldReturnZero() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            int size = tx.size(map);
            assertEquals(0, size);
        }
    }

    // ==================== Set via Transaction 테스트 ====================

    @Test
    public void first_emptySet_viaTransaction_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            Long result = tx.first(set);
            assertNull(result);
        }
    }

    @Test
    public void last_emptySet_viaTransaction_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            Long result = tx.last(set);
            assertNull(result);
        }
    }

    @Test
    public void first_nonEmptySet_viaTransaction_shouldReturnSmallest() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(30L);
        set.add(10L);
        set.add(20L);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            Long result = tx.first(set);
            assertEquals(Long.valueOf(10L), result);
        }
    }

    @Test
    public void last_nonEmptySet_viaTransaction_shouldReturnLargest() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(30L);
        set.add(10L);
        set.add(20L);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            Long result = tx.last(set);
            assertEquals(Long.valueOf(30L), result);
        }
    }

    @Test
    public void contains_emptySet_viaTransaction_shouldReturnFalse() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            boolean result = tx.contains(set, 10L);
            assertFalse(result);
        }
    }

    @Test
    public void size_emptySet_viaTransaction_shouldReturnZero() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            int size = tx.size(set);
            assertEquals(0, size);
        }
    }

    // ==================== List via Transaction 테스트 ====================

    @Test
    public void get_emptyList_viaTransaction_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            try {
                tx.get(list, 0);
                fail("Expected IndexOutOfBoundsException");
            } catch (IndexOutOfBoundsException e) {
                // expected
            }
        }
    }

    @Test
    public void get_nonEmptyList_viaTransaction_shouldReturnElement() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("a", tx.get(list, 0));
            assertEquals("b", tx.get(list, 1));
            assertEquals("c", tx.get(list, 2));
        }
    }

    @Test
    public void indexOf_emptyList_viaTransaction_shouldReturnMinusOne() {
        List<String> list = store.createList("test", String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            int index = tx.indexOf(list, "a");
            assertEquals(-1, index);
        }
    }

    @Test
    public void indexOf_nonExistingElement_viaTransaction_shouldReturnMinusOne() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            int index = tx.indexOf(list, "x");
            assertEquals(-1, index);
        }
    }

    @Test
    public void indexOf_existingElement_viaTransaction_shouldReturnIndex() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(0, tx.indexOf(list, "a"));
            assertEquals(1, tx.indexOf(list, "b"));
            assertEquals(2, tx.indexOf(list, "c"));
        }
    }

    @Test
    public void size_emptyList_viaTransaction_shouldReturnZero() {
        List<String> list = store.createList("test", String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            int size = tx.size(list);
            assertEquals(0, size);
        }
    }

    // ==================== Transaction 상태 테스트 ====================

    @Test
    public void isActive_freshTransaction_shouldReturnTrue() {
        store.createDeque("test", String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            assertTrue(tx.isActive());
        }
    }

    @Test
    public void isActive_afterClose_shouldReturnFalse() {
        store.createDeque("test", String.class);
        store.commit();

        FxReadTransaction tx = store.beginRead();
        assertTrue(tx.isActive());

        tx.close();
        assertFalse(tx.isActive());
    }

    @Test(expected = IllegalStateException.class)
    public void operationAfterClose_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        store.commit();

        FxReadTransaction tx = store.beginRead();
        tx.close();

        // 닫힌 트랜잭션에서 작업 시도
        tx.peekFirst(deque);
    }

    // ==================== 스냅샷 일관성 테스트 ====================

    @Test
    public void transaction_shouldSeeConsistentSnapshot() throws Exception {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("original");
        store.commit();

        // 읽기 트랜잭션 시작
        try (FxReadTransaction tx = store.beginRead()) {
            // 트랜잭션 중에 데이터 변경
            deque.addLast("new");
            store.commit();

            // 트랜잭션은 여전히 원래 스냅샷을 봄
            assertEquals(1, tx.size(deque));
            assertEquals("original", tx.peekFirst(deque));
        }

        // 새로운 트랜잭션은 변경된 데이터를 봄
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertEquals(2, tx2.size(deque));
        }
    }

    // ==================== 다중 컬렉션 테스트 ====================

    @Test
    public void transaction_shouldWorkAcrossCollections() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("set", Long.class);
        Deque<String> deque = store.createDeque("deque", String.class);
        List<String> list = store.createList("list", String.class);

        map.put(1L, "one");
        set.add(10L);
        deque.addLast("first");
        list.add("item");
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("one", tx.get(map, 1L));
            assertTrue(tx.contains(set, 10L));
            assertEquals("first", tx.peekFirst(deque));
            assertEquals("item", tx.get(list, 0));

            assertEquals(1, tx.size(map));
            assertEquals(1, tx.size(set));
            assertEquals(1, tx.size(deque));
            assertEquals(1, tx.size(list));
        }
    }
}
