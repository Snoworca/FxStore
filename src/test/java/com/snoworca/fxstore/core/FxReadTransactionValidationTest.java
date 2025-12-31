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
 * FxReadTransaction 검증 경로 테스트
 *
 * <p>V17 커버리지 개선: 다른 Store 컬렉션 접근 경로 테스트</p>
 */
public class FxReadTransactionValidationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile1;
    private File storeFile2;
    private FxStore store1;
    private FxStore store2;

    @Before
    public void setUp() throws Exception {
        storeFile1 = tempFolder.newFile("store1.fx");
        storeFile1.delete();
        storeFile2 = tempFolder.newFile("store2.fx");
        storeFile2.delete();

        store1 = FxStore.open(storeFile1.toPath());
        store2 = FxStore.open(storeFile2.toPath());
    }

    @After
    public void tearDown() {
        if (store1 != null) {
            try { store1.close(); } catch (Exception e) { /* ignore */ }
        }
        if (store2 != null) {
            try { store2.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== Map 다른 Store 테스트 ====================

    @Test(expected = IllegalArgumentException.class)
    public void transaction_getFromDifferentStoreMap_shouldThrow() {
        // Given: store1에서 map 생성
        NavigableMap<Long, String> map = store1.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store1.commit();

        // store2에서 트랜잭션 시작
        try (FxReadTransaction tx = store2.beginRead()) {
            // When: store1의 map을 store2의 트랜잭션에서 접근
            tx.get(map, 1L);  // 다른 Store의 컬렉션 사용
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_containsKeyFromDifferentStoreMap_shouldThrow() {
        NavigableMap<Long, String> map = store1.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.containsKey(map, 1L);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_sizeFromDifferentStoreMap_shouldThrow() {
        NavigableMap<Long, String> map = store1.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.size(map);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_firstEntryFromDifferentStoreMap_shouldThrow() {
        NavigableMap<Long, String> map = store1.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.firstEntry(map);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_lastEntryFromDifferentStoreMap_shouldThrow() {
        NavigableMap<Long, String> map = store1.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.lastEntry(map);
        }
    }

    // ==================== Set 다른 Store 테스트 ====================

    @Test(expected = IllegalArgumentException.class)
    public void transaction_containsFromDifferentStoreSet_shouldThrow() {
        NavigableSet<Long> set = store1.createSet("test", Long.class);
        set.add(1L);
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.contains(set, 1L);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_sizeFromDifferentStoreSet_shouldThrow() {
        NavigableSet<Long> set = store1.createSet("test", Long.class);
        set.add(1L);
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.size(set);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_firstFromDifferentStoreSet_shouldThrow() {
        NavigableSet<Long> set = store1.createSet("test", Long.class);
        set.add(1L);
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.first(set);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_lastFromDifferentStoreSet_shouldThrow() {
        NavigableSet<Long> set = store1.createSet("test", Long.class);
        set.add(1L);
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.last(set);
        }
    }

    // ==================== List 다른 Store 테스트 ====================

    @Test(expected = IllegalArgumentException.class)
    public void transaction_getFromDifferentStoreList_shouldThrow() {
        List<String> list = store1.createList("test", String.class);
        list.add("one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.get(list, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_sizeFromDifferentStoreList_shouldThrow() {
        List<String> list = store1.createList("test", String.class);
        list.add("one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.size(list);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_indexOfFromDifferentStoreList_shouldThrow() {
        List<String> list = store1.createList("test", String.class);
        list.add("one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.indexOf(list, "one");
        }
    }

    // ==================== Deque 다른 Store 테스트 ====================

    @Test(expected = IllegalArgumentException.class)
    public void transaction_peekFirstFromDifferentStoreDeque_shouldThrow() {
        Deque<String> deque = store1.createDeque("test", String.class);
        deque.addLast("one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.peekFirst(deque);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_peekLastFromDifferentStoreDeque_shouldThrow() {
        Deque<String> deque = store1.createDeque("test", String.class);
        deque.addLast("one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.peekLast(deque);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transaction_sizeFromDifferentStoreDeque_shouldThrow() {
        Deque<String> deque = store1.createDeque("test", String.class);
        deque.addLast("one");
        store1.commit();

        try (FxReadTransaction tx = store2.beginRead()) {
            tx.size(deque);
        }
    }

    // ==================== 같은 Store는 정상 동작 ====================

    @Test
    public void transaction_sameStoreMap_shouldWork() {
        NavigableMap<Long, String> map = store1.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store1.commit();

        try (FxReadTransaction tx = store1.beginRead()) {
            assertEquals("one", tx.get(map, 1L));
            assertEquals(1, tx.size(map));
        }
    }

    @Test
    public void transaction_sameStoreSet_shouldWork() {
        NavigableSet<Long> set = store1.createSet("test", Long.class);
        set.add(1L);
        store1.commit();

        try (FxReadTransaction tx = store1.beginRead()) {
            assertTrue(tx.contains(set, 1L));
            assertEquals(1, tx.size(set));
        }
    }

    @Test
    public void transaction_sameStoreList_shouldWork() {
        List<String> list = store1.createList("test", String.class);
        list.add("one");
        store1.commit();

        try (FxReadTransaction tx = store1.beginRead()) {
            assertEquals("one", tx.get(list, 0));
            assertEquals(1, tx.size(list));
        }
    }

    @Test
    public void transaction_sameStoreDeque_shouldWork() {
        Deque<String> deque = store1.createDeque("test", String.class);
        deque.addLast("one");
        store1.commit();

        try (FxReadTransaction tx = store1.beginRead()) {
            assertEquals("one", tx.peekFirst(deque));
            assertEquals(1, tx.size(deque));
        }
    }
}
