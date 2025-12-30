package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/**
 * FxStore 통합 테스트.
 *
 * 메모리 및 파일 기반 Store의 핵심 기능을 테스트합니다.
 */
public class FxStoreIntegrationTest {

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

    // ==================== 메모리 Store 테스트 ====================

    @Test
    public void testMemoryStore_CreateMap() {
        NavigableMap<Long, String> map = memoryStore.createMap("testMap", Long.class, String.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMemoryStore_CreateSet() {
        NavigableSet<Long> set = memoryStore.createSet("testSet", Long.class);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testMemoryStore_CreateDeque() {
        Deque<String> deque = memoryStore.createDeque("testDeque", String.class);
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testMemoryStore_CreateList() {
        List<String> list = memoryStore.createList("testList", String.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== DDL 테스트 ====================

    @Test
    public void testExists() {
        assertFalse(memoryStore.exists("myMap"));
        memoryStore.createMap("myMap", Long.class, String.class);
        assertTrue(memoryStore.exists("myMap"));
    }

    @Test
    public void testDrop() {
        memoryStore.createMap("toDrop", Long.class, String.class);
        assertTrue(memoryStore.exists("toDrop"));

        boolean result = memoryStore.drop("toDrop");
        assertTrue(result);
        assertFalse(memoryStore.exists("toDrop"));
    }

    @Test
    public void testDrop_NonExistent() {
        boolean result = memoryStore.drop("nonExistent");
        assertFalse(result);
    }

    @Test
    public void testRename() {
        NavigableMap<Long, String> map = memoryStore.createMap("oldName", Long.class, String.class);
        map.put(1L, "value1");

        boolean result = memoryStore.rename("oldName", "newName");
        assertTrue(result);

        assertFalse(memoryStore.exists("oldName"));
        assertTrue(memoryStore.exists("newName"));
    }

    @Test
    public void testList() {
        memoryStore.createMap("map1", Long.class, String.class);
        memoryStore.createSet("set1", Long.class);
        memoryStore.createDeque("deque1", String.class);

        List<CollectionInfo> collections = memoryStore.list();
        assertTrue(collections.size() >= 3);

        Set<String> names = new HashSet<>();
        for (CollectionInfo info : collections) {
            names.add(info.name());
        }
        assertTrue(names.contains("map1"));
        assertTrue(names.contains("set1"));
        assertTrue(names.contains("deque1"));
    }

    // ==================== Map 연산 테스트 ====================

    @Test
    public void testMap_PutGet() {
        NavigableMap<Long, String> map = memoryStore.createMap("mapTest", Long.class, String.class);

        map.put(1L, "one");
        map.put(2L, "two");
        map.put(3L, "three");

        assertEquals("one", map.get(1L));
        assertEquals("two", map.get(2L));
        assertEquals("three", map.get(3L));
    }

    @Test
    public void testMap_Remove() {
        NavigableMap<Long, String> map = memoryStore.createMap("mapRemove", Long.class, String.class);

        map.put(1L, "one");
        map.put(2L, "two");

        assertEquals("one", map.remove(1L));
        assertNull(map.get(1L));
        assertEquals(1, map.size());
    }

    @Test
    public void testMap_Navigable() {
        NavigableMap<Long, String> map = memoryStore.createMap("mapNav", Long.class, String.class);

        for (long i = 0; i < 10; i++) {
            map.put(i * 10, "v" + i);
        }

        assertEquals(Long.valueOf(0L), map.firstKey());
        assertEquals(Long.valueOf(90L), map.lastKey());
        assertEquals(Long.valueOf(30L), map.floorKey(35L));
        assertEquals(Long.valueOf(40L), map.ceilingKey(35L));
    }

    // ==================== Set 연산 테스트 ====================

    @Test
    public void testSet_AddContains() {
        NavigableSet<Long> set = memoryStore.createSet("setTest", Long.class);

        set.add(1L);
        set.add(2L);
        set.add(3L);

        assertTrue(set.contains(1L));
        assertTrue(set.contains(2L));
        assertTrue(set.contains(3L));
        assertFalse(set.contains(4L));
    }

    @Test
    public void testSet_Remove() {
        NavigableSet<Long> set = memoryStore.createSet("setRemove", Long.class);

        set.add(1L);
        set.add(2L);

        assertTrue(set.remove(1L));
        assertFalse(set.contains(1L));
        assertEquals(1, set.size());
    }

    @Test
    public void testSet_Navigable() {
        NavigableSet<Long> set = memoryStore.createSet("setNav", Long.class);

        for (long i = 0; i < 10; i++) {
            set.add(i * 10);
        }

        assertEquals(Long.valueOf(0L), set.first());
        assertEquals(Long.valueOf(90L), set.last());
        assertEquals(Long.valueOf(30L), set.floor(35L));
        assertEquals(Long.valueOf(40L), set.ceiling(35L));
    }

    // ==================== Deque 연산 테스트 ====================

    @Test
    public void testDeque_AddFirstLast() {
        Deque<String> deque = memoryStore.createDeque("dequeTest", String.class);

        deque.addFirst("first");
        deque.addLast("last");
        deque.addFirst("newest");

        assertEquals("newest", deque.getFirst());
        assertEquals("last", deque.getLast());
        assertEquals(3, deque.size());
    }

    @Test
    public void testDeque_RemoveFirstLast() {
        Deque<String> deque = memoryStore.createDeque("dequeRemove", String.class);

        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        assertEquals("a", deque.removeFirst());
        assertEquals("c", deque.removeLast());
        assertEquals(1, deque.size());
    }

    // ==================== List 연산 테스트 ====================

    @Test
    public void testList_AddGet() {
        List<String> list = memoryStore.createList("listTest", String.class);

        list.add("a");
        list.add("b");
        list.add("c");

        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    public void testList_Set() {
        List<String> list = memoryStore.createList("listSet", String.class);

        list.add("a");
        list.add("b");

        assertEquals("a", list.set(0, "x"));
        assertEquals("x", list.get(0));
    }

    // ==================== 파일 Store 테스트 ====================

    @Test
    public void testFileStore_CreateAndReopen() throws Exception {
        File storeFile = tempFolder.newFile("test.fx");
        Path path = storeFile.toPath();

        // 생성 및 데이터 삽입
        try (FxStore store = FxStore.open(path)) {
            NavigableMap<Long, String> map = store.createMap("myMap", Long.class, String.class);
            map.put(1L, "one");
            map.put(2L, "two");
        }

        // 재열기 및 데이터 확인
        try (FxStore store = FxStore.open(path)) {
            NavigableMap<Long, String> map = store.openMap("myMap", Long.class, String.class);
            assertEquals("one", map.get(1L));
            assertEquals("two", map.get(2L));
        }
    }

    @Test
    public void testFileStore_MultipleCollections() throws Exception {
        File storeFile = tempFolder.newFile("multi.fx");
        Path path = storeFile.toPath();

        // 생성
        try (FxStore store = FxStore.open(path)) {
            NavigableMap<Long, String> map = store.createMap("map1", Long.class, String.class);
            NavigableSet<Long> set = store.createSet("set1", Long.class);
            Deque<String> deque = store.createDeque("deque1", String.class);

            map.put(1L, "value");
            set.add(100L);
            deque.addLast("item");
        }

        // 재열기
        try (FxStore store = FxStore.open(path)) {
            assertEquals("value", store.openMap("map1", Long.class, String.class).get(1L));
            assertTrue(store.openSet("set1", Long.class).contains(100L));
            assertEquals("item", store.openDeque("deque1", String.class).getFirst());
        }
    }

    // ==================== 옵션 테스트 ====================

    @Test
    public void testOptions_PageSize() {
        FxOptions options = FxOptions.defaults()
                .withPageSize(PageSize.PAGE_8K)
                .build();

        FxStore store = FxStore.openMemory(options);
        assertNotNull(store);

        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "value");
        assertEquals("value", map.get(1L));

        store.close();
    }

    @Test
    public void testOptions_CacheSize() {
        FxOptions options = FxOptions.defaults()
                .withCacheBytes(1024 * 1024)  // 1MB
                .build();

        FxStore store = FxStore.openMemory(options);
        assertNotNull(store);

        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 0; i < 1000; i++) {
            set.add(i);
        }
        assertEquals(1000, set.size());

        store.close();
    }

    // ==================== 대량 데이터 테스트 ====================

    @Test
    public void testLargeDataSet_Map() {
        NavigableMap<Long, String> map = memoryStore.createMap("largeMap", Long.class, String.class);

        // 10,000개 삽입
        for (long i = 0; i < 10000; i++) {
            map.put(i, "value" + i);
        }

        assertEquals(10000, map.size());

        // 랜덤 조회
        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            long key = random.nextInt(10000);
            assertEquals("value" + key, map.get(key));
        }
    }

    @Test
    public void testLargeDataSet_Set() {
        NavigableSet<Long> set = memoryStore.createSet("largeSet", Long.class);

        for (long i = 0; i < 10000; i++) {
            set.add(i);
        }

        assertEquals(10000, set.size());
        assertTrue(set.contains(0L));
        assertTrue(set.contains(9999L));
    }

    // ==================== CreateOrOpen 테스트 ====================

    @Test
    public void testCreateOrOpenMap() {
        NavigableMap<Long, String> map1 = memoryStore.createOrOpenMap("sharedMap", Long.class, String.class);
        map1.put(1L, "first");

        NavigableMap<Long, String> map2 = memoryStore.createOrOpenMap("sharedMap", Long.class, String.class);
        assertEquals("first", map2.get(1L));
    }

    @Test
    public void testCreateOrOpenSet() {
        NavigableSet<Long> set1 = memoryStore.createOrOpenSet("sharedSet", Long.class);
        set1.add(1L);

        NavigableSet<Long> set2 = memoryStore.createOrOpenSet("sharedSet", Long.class);
        assertTrue(set2.contains(1L));
    }

    @Test
    public void testCreateOrOpenDeque() {
        Deque<String> deque1 = memoryStore.createOrOpenDeque("sharedDeque", String.class);
        deque1.addLast("item");

        Deque<String> deque2 = memoryStore.createOrOpenDeque("sharedDeque", String.class);
        assertEquals("item", deque2.getFirst());
    }

    @Test
    public void testCreateOrOpenList() {
        List<String> list1 = memoryStore.createOrOpenList("sharedList", String.class);
        list1.add("item");

        List<String> list2 = memoryStore.createOrOpenList("sharedList", String.class);
        assertEquals("item", list2.get(0));
    }

    // ==================== 코덱 테스트 ====================

    @Test
    public void testCodecRegistry() {
        FxCodecRegistry registry = memoryStore.codecs();
        assertNotNull(registry);
    }

    // ==================== 예외 테스트 ====================

    @Test(expected = FxException.class)
    public void testCreateMap_AlreadyExists() {
        memoryStore.createMap("duplicate", Long.class, String.class);
        memoryStore.createMap("duplicate", Long.class, String.class);
    }

    @Test(expected = FxException.class)
    public void testOpenMap_NotFound() {
        memoryStore.openMap("nonExistent", Long.class, String.class);
    }

    @Test(expected = FxException.class)
    public void testRename_TargetExists() {
        memoryStore.createMap("name1", Long.class, String.class);
        memoryStore.createMap("name2", Long.class, String.class);
        memoryStore.rename("name1", "name2");
    }
}
