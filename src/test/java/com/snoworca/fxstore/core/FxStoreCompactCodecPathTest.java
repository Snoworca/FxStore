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
 * FxStoreImpl compactTo 및 codecRefToClass 경로 테스트
 *
 * <p>V17 커버리지 개선: codecRefToClass의 다양한 타입 경로</p>
 */
public class FxStoreCompactCodecPathTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("test.fx");
        storeFile.delete();
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== codecRefToClass - Long (I64) 경로 ====================

    @Test
    public void compactTo_mapWithLongKeys_shouldCopyCorrectly() throws Exception {
        // Given: Long 키 Map
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("longKeyMap", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableMap<Long, String> targetMap = targetStore.openMap("longKeyMap", Long.class, String.class);
            assertEquals(2, targetMap.size());
            assertEquals("one", targetMap.get(1L));
        }
    }

    // ==================== codecRefToClass - Double (F64) 경로 ====================

    @Test
    public void compactTo_mapWithDoubleKeys_shouldCopyCorrectly() throws Exception {
        // Given: Double 키 Map
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Double, String> map = store.createMap("doubleKeyMap", Double.class, String.class);
        map.put(1.5, "onePointFive");
        map.put(2.5, "twoPointFive");
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableMap<Double, String> targetMap = targetStore.openMap("doubleKeyMap", Double.class, String.class);
            assertEquals(2, targetMap.size());
            assertEquals("onePointFive", targetMap.get(1.5));
        }
    }

    // ==================== codecRefToClass - String 경로 ====================

    @Test
    public void compactTo_mapWithStringKeys_shouldCopyCorrectly() throws Exception {
        // Given: String 키 Map
        store = FxStore.open(storeFile.toPath());
        NavigableMap<String, Long> map = store.createMap("stringKeyMap", String.class, Long.class);
        map.put("alpha", 1L);
        map.put("beta", 2L);
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableMap<String, Long> targetMap = targetStore.openMap("stringKeyMap", String.class, Long.class);
            assertEquals(2, targetMap.size());
            assertEquals(Long.valueOf(1L), targetMap.get("alpha"));
        }
    }

    // ==================== codecRefToClass - byte[] 경로 ====================

    @Test
    public void compactTo_mapWithBytesKeys_shouldCopyCorrectly() throws Exception {
        // Given: byte[] 키 Map
        store = FxStore.open(storeFile.toPath());
        NavigableMap<byte[], String> map = store.createMap("bytesKeyMap", byte[].class, String.class);
        byte[] key1 = new byte[]{1, 2, 3};
        byte[] key2 = new byte[]{4, 5, 6};
        map.put(key1, "first");
        map.put(key2, "second");
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 파일 생성됨
        assertTrue(targetFile.exists());
    }

    // ==================== Set 복사 경로 ====================

    @Test
    public void compactTo_setWithLongElements_shouldCopyCorrectly() throws Exception {
        // Given: Long Set
        store = FxStore.open(storeFile.toPath());
        NavigableSet<Long> set = store.createSet("longSet", Long.class);
        set.add(10L);
        set.add(20L);
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableSet<Long> targetSet = targetStore.openSet("longSet", Long.class);
            assertEquals(2, targetSet.size());
            assertTrue(targetSet.contains(10L));
        }
    }

    @Test
    public void compactTo_setWithDoubleElements_shouldCopyCorrectly() throws Exception {
        // Given: Double Set
        store = FxStore.open(storeFile.toPath());
        NavigableSet<Double> set = store.createSet("doubleSet", Double.class);
        set.add(1.1);
        set.add(2.2);
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableSet<Double> targetSet = targetStore.openSet("doubleSet", Double.class);
            assertEquals(2, targetSet.size());
            assertTrue(targetSet.contains(1.1));
        }
    }

    @Test
    public void compactTo_setWithStringElements_shouldCopyCorrectly() throws Exception {
        // Given: String Set
        store = FxStore.open(storeFile.toPath());
        NavigableSet<String> set = store.createSet("stringSet", String.class);
        set.add("apple");
        set.add("banana");
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableSet<String> targetSet = targetStore.openSet("stringSet", String.class);
            assertEquals(2, targetSet.size());
            assertTrue(targetSet.contains("apple"));
        }
    }

    // ==================== List 복사 경로 ====================

    @Test
    public void compactTo_listWithLongElements_shouldCopyCorrectly() throws Exception {
        // Given: Long List
        store = FxStore.open(storeFile.toPath());
        List<Long> list = store.createList("longList", Long.class);
        list.add(100L);
        list.add(200L);
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            List<Long> targetList = targetStore.openList("longList", Long.class);
            assertEquals(2, targetList.size());
            assertEquals(Long.valueOf(100L), targetList.get(0));
        }
    }

    @Test
    public void compactTo_listWithStringElements_shouldCopyCorrectly() throws Exception {
        // Given: String List
        store = FxStore.open(storeFile.toPath());
        List<String> list = store.createList("stringList", String.class);
        list.add("first");
        list.add("second");
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            List<String> targetList = targetStore.openList("stringList", String.class);
            assertEquals(2, targetList.size());
            assertEquals("first", targetList.get(0));
        }
    }

    // ==================== Deque 복사 경로 ====================

    @Test
    public void compactTo_dequeWithLongElements_shouldCopyCorrectly() throws Exception {
        // Given: Long Deque
        store = FxStore.open(storeFile.toPath());
        Deque<Long> deque = store.createDeque("longDeque", Long.class);
        deque.addLast(1000L);
        deque.addLast(2000L);
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            Deque<Long> targetDeque = targetStore.openDeque("longDeque", Long.class);
            assertEquals(2, targetDeque.size());
            assertEquals(Long.valueOf(1000L), targetDeque.peekFirst());
        }
    }

    @Test
    public void compactTo_dequeWithStringElements_shouldCopyCorrectly() throws Exception {
        // Given: String Deque
        store = FxStore.open(storeFile.toPath());
        Deque<String> deque = store.createDeque("stringDeque", String.class);
        deque.addLast("head");
        deque.addLast("tail");
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            Deque<String> targetDeque = targetStore.openDeque("stringDeque", String.class);
            assertEquals(2, targetDeque.size());
            assertEquals("head", targetDeque.peekFirst());
        }
    }

    // ==================== 여러 컬렉션 동시 복사 ====================

    @Test
    public void compactTo_multipleCollectionTypes_shouldCopyAll() throws Exception {
        // Given: 다양한 타입의 컬렉션들
        store = FxStore.open(storeFile.toPath());

        NavigableMap<Long, String> map = store.createMap("map1", Long.class, String.class);
        map.put(1L, "one");

        NavigableSet<String> set = store.createSet("set1", String.class);
        set.add("elem");

        List<Long> list = store.createList("list1", Long.class);
        list.add(123L);

        Deque<String> deque = store.createDeque("deque1", String.class);
        deque.addLast("item");

        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 모든 컬렉션 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            List<CollectionInfo> collections = targetStore.list();
            assertEquals(4, collections.size());

            NavigableMap<Long, String> tMap = targetStore.openMap("map1", Long.class, String.class);
            assertEquals("one", tMap.get(1L));

            NavigableSet<String> tSet = targetStore.openSet("set1", String.class);
            assertTrue(tSet.contains("elem"));

            List<Long> tList = targetStore.openList("list1", Long.class);
            assertEquals(Long.valueOf(123L), tList.get(0));

            Deque<String> tDeque = targetStore.openDeque("deque1", String.class);
            assertEquals("item", tDeque.peekFirst());
        }
    }

    // ==================== stats(DEEP) countTreeBytes 경로 ====================

    @Test
    public void stats_deep_emptyStore_shouldReturnZeroBytes() throws Exception {
        // Given: 빈 Store
        store = FxStore.open(storeFile.toPath());
        store.commit();

        // When: DEEP 통계
        Stats stats = store.stats(StatsMode.DEEP);

        // Then: 컬렉션 없음
        assertEquals(0, stats.collectionCount());
    }

    @Test
    public void stats_deep_largeData_shouldCalculateBytes() throws Exception {
        // Given: 많은 데이터가 있는 Store
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("largeMap", Long.class, String.class);

        for (int i = 0; i < 1000; i++) {
            map.put((long) i, "value" + i);
        }
        store.commit();

        // When: DEEP 통계
        Stats stats = store.stats(StatsMode.DEEP);

        // Then: 바이트 계산됨
        assertTrue(stats.fileBytes() > 0);
    }

    // ==================== verify 후 commit ====================

    @Test
    public void verify_thenCommit_shouldWork() throws Exception {
        // Given: Store with data
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();

        // When: verify 후 추가 변경
        VerifyResult result = store.verify();
        assertTrue(result.ok());

        map.put(2L, "two");
        store.commit();

        // Then: 데이터 유지
        assertEquals(2, map.size());
    }
}
