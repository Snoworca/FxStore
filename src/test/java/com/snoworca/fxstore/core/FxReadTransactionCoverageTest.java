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
 * FxReadTransactionImpl 미커버 브랜치 테스트
 *
 * <p>커버리지 개선 대상:</p>
 * <ul>
 *   <li>peekFirst/peekLast (Deque) - 빈 Deque 브랜치</li>
 *   <li>size (Deque) - 브랜치</li>
 *   <li>first/last (Set) - 빈 Set 브랜치</li>
 *   <li>firstEntry/lastEntry (Map) - 빈 Map 브랜치</li>
 * </ul>
 */
public class FxReadTransactionCoverageTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("read-tx-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== Map beginRead 테스트 ====================

    @Test
    public void readTransaction_map_get_shouldWork() {
        // Given
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");
        map.put(3L, "three");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            // Then
            assertEquals("one", tx.get(map, 1L));
            assertEquals("two", tx.get(map, 2L));
            assertEquals("three", tx.get(map, 3L));
            assertNull(tx.get(map, 999L));
        }
    }

    @Test
    public void readTransaction_map_containsKey_shouldWork() {
        // Given
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertTrue(tx.containsKey(map, 1L));
            assertFalse(tx.containsKey(map, 999L));
        }
    }

    @Test
    public void readTransaction_map_firstEntry_empty_shouldReturnNull() {
        // Given: 빈 맵
        NavigableMap<Long, String> emptyMap = store.createMap("empty", Long.class, String.class);

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.firstEntry(emptyMap));
            assertNull(tx.lastEntry(emptyMap));
        }
    }

    @Test
    public void readTransaction_map_firstLastEntry_shouldWork() {
        // Given
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(Long.valueOf(10L), tx.firstEntry(map).getKey());
            assertEquals("ten", tx.firstEntry(map).getValue());
            assertEquals(Long.valueOf(30L), tx.lastEntry(map).getKey());
            assertEquals("thirty", tx.lastEntry(map).getValue());
        }
    }

    @Test
    public void readTransaction_map_size_shouldWork() {
        // Given
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(2, tx.size(map));
        }
    }

    @Test
    public void readTransaction_map_size_empty_shouldReturnZero() {
        // Given: 빈 맵
        NavigableMap<Long, String> emptyMap = store.createMap("empty", Long.class, String.class);

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(0, tx.size(emptyMap));
        }
    }

    // ==================== Set beginRead 테스트 ====================

    @Test
    public void readTransaction_set_contains_shouldWork() {
        // Given
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertTrue(tx.contains(set, 1L));
            assertTrue(tx.contains(set, 2L));
            assertFalse(tx.contains(set, 999L));
        }
    }

    @Test
    public void readTransaction_set_firstLast_empty_shouldReturnNull() {
        // Given: 빈 Set
        NavigableSet<Long> emptySet = store.createSet("empty", Long.class);

        // When/Then: 빈 Set에서 first/last는 null 반환
        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.first(emptySet));
            assertNull(tx.last(emptySet));
        }
    }

    @Test
    public void readTransaction_set_firstLast_shouldWork() {
        // Given
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(Long.valueOf(10L), tx.first(set));
            assertEquals(Long.valueOf(30L), tx.last(set));
        }
    }

    @Test
    public void readTransaction_set_size_shouldWork() {
        // Given
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(3, tx.size(set));
        }
    }

    // ==================== List beginRead 테스트 ====================

    @Test
    public void readTransaction_list_get_shouldWork() {
        // Given
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("a", tx.get(list, 0));
            assertEquals("b", tx.get(list, 1));
            assertEquals("c", tx.get(list, 2));
        }
    }

    @Test
    public void readTransaction_list_size_shouldWork() {
        // Given
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(2, tx.size(list));
        }
    }

    @Test
    public void readTransaction_list_indexOf_shouldWork() {
        // Given
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("b"); // 중복

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(0, tx.indexOf(list, "a"));
            assertEquals(1, tx.indexOf(list, "b")); // 첫 번째 b
            assertEquals(2, tx.indexOf(list, "c"));
            assertEquals(-1, tx.indexOf(list, "z")); // 없음
        }
    }

    // ==================== Deque beginRead 테스트 ====================

    @Test
    public void readTransaction_deque_peekFirstLast_shouldWork() {
        // Given
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("first");
        deque.addLast("middle");
        deque.addLast("last");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("first", tx.peekFirst(deque));
            assertEquals("last", tx.peekLast(deque));
        }
    }

    @Test
    public void readTransaction_deque_peekFirstLast_empty_shouldReturnNull() {
        // Given: 빈 Deque
        Deque<String> emptyDeque = store.createDeque("empty", String.class);

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.peekFirst(emptyDeque));
            assertNull(tx.peekLast(emptyDeque));
        }
    }

    @Test
    public void readTransaction_deque_size_shouldWork() {
        // Given
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(3, tx.size(deque));
        }
    }

    @Test
    public void readTransaction_deque_size_empty_shouldReturnZero() {
        // Given: 빈 Deque
        Deque<String> emptyDeque = store.createDeque("empty", String.class);

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(0, tx.size(emptyDeque));
        }
    }

    // ==================== Transaction 상태 테스트 ====================

    @Test
    public void readTransaction_isActive_shouldWork() {
        FxReadTransaction tx = store.beginRead();
        assertTrue(tx.isActive());

        tx.close();
        assertFalse(tx.isActive());
    }

    @Test(expected = IllegalStateException.class)
    public void readTransaction_afterClose_shouldThrow() {
        // Given
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        FxReadTransaction tx = store.beginRead();
        tx.close();

        // When: 닫힌 트랜잭션 사용
        tx.get(map, 1L);

        // Then: IllegalStateException
    }

    // ==================== 다중 컬렉션 읽기 테스트 ====================

    @Test
    public void readTransaction_multipleCollections_shouldWork() {
        // Given
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("set", Long.class);
        List<String> list = store.createList("list", String.class);
        Deque<String> deque = store.createDeque("deque", String.class);

        map.put(1L, "one");
        set.add(100L);
        list.add("item");
        deque.addLast("element");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("one", tx.get(map, 1L));
            assertTrue(tx.contains(set, 100L));
            assertEquals("item", tx.get(list, 0));
            assertEquals("element", tx.peekFirst(deque));
        }
    }

    // ==================== 스냅샷 일관성 테스트 ====================

    @Test
    public void readTransaction_snapshot_shouldBeConsistent() {
        // Given
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "original");

        // When: 트랜잭션 시작
        FxReadTransaction tx = store.beginRead();

        // 데이터 변경
        map.put(1L, "modified");
        map.put(2L, "new");

        // Then: 트랜잭션은 이전 스냅샷 사용
        assertEquals("original", tx.get(map, 1L));
        // 새로 추가된 데이터는 보이지 않음
        assertNull(tx.get(map, 2L));

        tx.close();
    }

    // ==================== getSnapshotSeqNo 테스트 ====================

    @Test
    public void readTransaction_getSnapshotSeqNo_shouldWork() {
        // Given
        store.createMap("test", Long.class, String.class).put(1L, "one");

        // When
        try (FxReadTransaction tx = store.beginRead()) {
            long seqNo = tx.getSnapshotSeqNo();
            assertTrue("SeqNo should be positive", seqNo >= 0);
        }
    }
}
