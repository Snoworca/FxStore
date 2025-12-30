package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * FxStoreImpl 고급 테스트
 * P3 클래스 커버리지 개선
 */
public class FxStoreAdvancedTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore memoryStore;

    @Before
    public void setUp() {
        memoryStore = FxStore.openMemory();
    }

    @After
    public void tearDown() {
        if (memoryStore != null) {
            try { memoryStore.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== 파일 재오픈 테스트 ====================

    @Test
    public void fileStore_reopenAfterClose_shouldRestoreData() throws Exception {
        // Given
        File file = tempFolder.newFile("reopen.fx");
        file.delete();
        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "value" + i);
            }
        }

        // When: 재오픈
        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);

            // Then
            assertEquals(100, map.size());
            assertEquals("value50", map.get(50L));
        }
    }

    @Test
    public void fileStore_largeData_shouldPersist() throws Exception {
        // Given
        File file = tempFolder.newFile("large.fx");
        file.delete();
        int count = 5000; // 더 작은 수로 테스트

        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.createMap("large", Long.class, String.class);
            for (long i = 0; i < count; i++) {
                map.put(i, "value" + i);
            }
        }

        // When & Then
        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.openMap("large", Long.class, String.class);
            assertEquals(count, map.size());
        }
    }

    @Test
    public void fileStore_multipleCollections_shouldAllPersist() throws Exception {
        // Given
        File file = tempFolder.newFile("multi.fx");
        file.delete();

        try (FxStore store = FxStore.open(file.toPath())) {
            store.createMap("map1", Long.class, String.class).put(1L, "a");
            store.createSet("set1", Long.class).add(1L);
            store.createDeque("deque1", String.class).addLast("item");
        }

        // When & Then
        try (FxStore store = FxStore.open(file.toPath())) {
            assertEquals("a", store.openMap("map1", Long.class, String.class).get(1L));
            assertTrue(store.openSet("set1", Long.class).contains(1L));
            assertEquals("item", store.openDeque("deque1", String.class).getFirst());
        }
    }

    @Test
    public void fileStore_deleteAndReopen_shouldReflectDeletion() throws Exception {
        // Given
        File file = tempFolder.newFile("delete.fx");
        file.delete();

        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "v" + i);
            }
            // 절반 삭제
            for (long i = 0; i < 50; i++) {
                map.remove(i);
            }
        }

        // When & Then
        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertEquals(50, map.size());
            assertNull(map.get(25L));
            assertEquals("v75", map.get(75L));
        }
    }

    // ==================== list() 메서드 테스트 ====================

    @Test
    public void list_shouldReturnAllCollections() {
        // Given
        memoryStore.createMap("map1", Long.class, String.class);
        memoryStore.createMap("map2", Long.class, String.class);
        memoryStore.createSet("set1", Long.class);

        // When
        List<CollectionInfo> infos = memoryStore.list();

        // Then
        assertEquals(3, infos.size());
    }

    @Test
    public void list_emptyStore_shouldReturnEmptyList() {
        // Given: empty store

        // When
        List<CollectionInfo> infos = memoryStore.list();

        // Then
        assertTrue(infos.isEmpty());
    }

    // ==================== exists() 메서드 테스트 ====================

    @Test
    public void exists_shouldReturnCorrectResult() {
        // Given
        memoryStore.createMap("existing", Long.class, String.class);

        // When & Then
        assertTrue(memoryStore.exists("existing"));
        assertFalse(memoryStore.exists("nonexistent"));
    }

    // ==================== stats() 메서드 테스트 ====================

    @Test
    public void stats_shouldReturnValidStats() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("test", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "value" + i);
        }

        // When
        Stats stats = memoryStore.stats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.fileBytes() >= 0);
        assertTrue(stats.collectionCount() >= 1);
    }

    @Test
    public void stats_emptyStore_shouldWork() {
        // Given: Empty store

        // When
        Stats stats = memoryStore.stats();

        // Then
        assertNotNull(stats);
        assertEquals(0, stats.collectionCount());
    }

    // ==================== verify() 메서드 테스트 ====================

    @Test
    public void verify_validStore_shouldReturnValid() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("test", Long.class, String.class);
        for (long i = 0; i < 50; i++) {
            map.put(i, "value" + i);
        }

        // When
        VerifyResult result = memoryStore.verify();

        // Then
        assertNotNull(result);
        assertTrue(result.ok());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    public void verify_fileStore_shouldReturnValid() throws Exception {
        // Given
        File file = tempFolder.newFile("verify.fx");
        file.delete();

        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "value" + i);
            }

            // When
            VerifyResult result = store.verify();

            // Then
            assertNotNull(result);
            assertTrue(result.ok());
        }
    }

    // ==================== drop() 메서드 테스트 ====================

    @Test
    public void drop_existingCollection_shouldRemove() {
        // Given
        memoryStore.createMap("toDrop", Long.class, String.class);
        assertTrue(memoryStore.exists("toDrop"));

        // When
        memoryStore.drop("toDrop");

        // Then
        assertFalse(memoryStore.exists("toDrop"));
    }

    @Test
    public void drop_nonexistentCollection_shouldNotThrow() {
        // Given & When: drop may return silently if collection doesn't exist
        memoryStore.drop("nonexistent");

        // Then: No exception means API allows silent drop
        assertFalse(memoryStore.exists("nonexistent"));
    }

    // ==================== 컬렉션 생성/열기 테스트 ====================

    @Test
    public void createMap_shouldReturnNavigableMap() {
        // Given & When
        NavigableMap<Long, String> map = memoryStore.createMap("testMap", Long.class, String.class);

        // Then
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test(expected = FxException.class)
    public void createMap_duplicate_shouldThrow() {
        // Given
        memoryStore.createMap("duplicate", Long.class, String.class);

        // When & Then
        memoryStore.createMap("duplicate", Long.class, String.class);
    }

    @Test
    public void openMap_existing_shouldReturnSameMap() {
        // Given
        NavigableMap<Long, String> created = memoryStore.createMap("existing", Long.class, String.class);
        created.put(1L, "test");

        // When
        NavigableMap<Long, String> opened = memoryStore.openMap("existing", Long.class, String.class);

        // Then
        assertEquals("test", opened.get(1L));
    }

    @Test(expected = FxException.class)
    public void openMap_nonexistent_shouldThrow() {
        // Given & When & Then
        memoryStore.openMap("nonexistent", Long.class, String.class);
    }

    @Test
    public void createSet_shouldReturnNavigableSet() {
        // Given & When
        NavigableSet<Long> set = memoryStore.createSet("testSet", Long.class);

        // Then
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void createDeque_shouldReturnDeque() {
        // Given & When
        Deque<String> deque = memoryStore.createDeque("testDeque", String.class);

        // Then
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    // ==================== 다양한 타입 테스트 ====================

    @Test
    public void map_withIntegerKey_shouldWork() {
        // Given
        NavigableMap<Integer, String> map = memoryStore.createMap("intMap", Integer.class, String.class);

        // When
        map.put(100, "hundred");
        map.put(-50, "negative fifty");
        map.put(0, "zero");

        // Then
        assertEquals(3, map.size());
        assertEquals("hundred", map.get(100));
        assertEquals("negative fifty", map.get(-50));
    }

    @Test
    public void map_withDoubleKey_shouldWork() {
        // Given
        NavigableMap<Double, String> map = memoryStore.createMap("doubleMap", Double.class, String.class);

        // When
        map.put(3.14, "pi");
        map.put(2.718, "e");
        map.put(-1.0, "negative");

        // Then
        assertEquals(3, map.size());
        assertEquals("pi", map.get(3.14));
    }

    @Test
    public void map_withBytesValue_shouldWork() {
        // Given
        NavigableMap<Long, byte[]> map = memoryStore.createMap("bytesMap", Long.class, byte[].class);
        byte[] data = {1, 2, 3, 4, 5};

        // When
        map.put(1L, data);

        // Then
        assertArrayEquals(data, map.get(1L));
    }

    // ==================== 네비게이션 테스트 ====================

    @Test
    public void map_navigation_shouldWork() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("navMap", Long.class, String.class);
        for (long i = 0; i < 100; i += 10) {
            map.put(i, "v" + i);
        }

        // When & Then
        assertEquals(Long.valueOf(0L), map.firstKey());
        assertEquals(Long.valueOf(90L), map.lastKey());
        assertEquals(Long.valueOf(50L), map.floorKey(55L));
        assertEquals(Long.valueOf(60L), map.ceilingKey(55L));
        assertEquals(Long.valueOf(40L), map.lowerKey(50L));
        assertEquals(Long.valueOf(60L), map.higherKey(50L));
    }

    @Test
    public void map_subMap_shouldWork() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("subMap", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "v" + i);
        }

        // When
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, false);

        // Then
        assertEquals(20, sub.size());
        assertTrue(sub.containsKey(20L));
        assertFalse(sub.containsKey(40L));
    }

    // ==================== Set 테스트 ====================

    @Test
    public void set_operations_shouldWork() {
        // Given
        NavigableSet<Long> set = memoryStore.createSet("opSet", Long.class);

        // When
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(20L); // 중복

        // Then
        assertEquals(3, set.size());
        assertTrue(set.contains(20L));
        assertFalse(set.contains(15L));
    }

    @Test
    public void set_navigation_shouldWork() {
        // Given
        NavigableSet<Long> set = memoryStore.createSet("navSet", Long.class);
        for (long i = 0; i < 100; i += 10) {
            set.add(i);
        }

        // When & Then
        assertEquals(Long.valueOf(0L), set.first());
        assertEquals(Long.valueOf(90L), set.last());
        assertEquals(Long.valueOf(50L), set.floor(55L));
        assertEquals(Long.valueOf(60L), set.ceiling(55L));
    }

    // ==================== Deque 테스트 ====================

    @Test
    public void deque_operations_shouldWork() {
        // Given
        Deque<String> deque = memoryStore.createDeque("opDeque", String.class);

        // When
        deque.addFirst("first");
        deque.addLast("last");
        deque.addFirst("new first");

        // Then
        assertEquals(3, deque.size());
        assertEquals("new first", deque.getFirst());
        assertEquals("last", deque.getLast());
    }

    @Test
    public void deque_pollOperations_shouldWork() {
        // Given
        Deque<String> deque = memoryStore.createDeque("pollDeque", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        // When & Then
        assertEquals("a", deque.pollFirst());
        assertEquals("c", deque.pollLast());
        assertEquals(1, deque.size());
    }

    // ==================== 메모리 스토어 특수 테스트 ====================

    @Test
    public void openMemory_shouldCreateFunctionalStore() {
        // Given & When
        FxStore store = FxStore.openMemory();

        // Then
        assertNotNull(store);

        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "test");
        assertEquals("test", map.get(1L));

        store.close();
    }

    // ==================== 닫힌 스토어 테스트 ====================

    @Test(expected = FxException.class)
    public void closedStore_createMap_shouldThrow() {
        // Given
        FxStore store = FxStore.openMemory();
        store.close();

        // When & Then
        store.createMap("test", Long.class, String.class);
    }

    @Test(expected = FxException.class)
    public void closedStore_list_shouldThrow() {
        // Given
        FxStore store = FxStore.openMemory();
        store.close();

        // When & Then
        store.list();
    }
}
