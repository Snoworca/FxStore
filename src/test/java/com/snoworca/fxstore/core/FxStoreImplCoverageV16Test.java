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
 * FxStoreImpl V16 커버리지 개선 테스트
 *
 * <p>V16 커버리지 개선: createOrOpen, getCollectionState, stats, verify 등 미커버 영역 테스트</p>
 *
 * <h3>테스트 대상</h3>
 * <ul>
 *   <li>createOrOpenXXX() - 기존 컬렉션 열기 경로</li>
 *   <li>getCollectionState() - 컬렉션 상태 조회</li>
 *   <li>getCollectionCount() - 컬렉션 카운트 조회</li>
 *   <li>stats() - 통계 정보 조회</li>
 *   <li>list() - 빈 스토어에서 컬렉션 목록</li>
 * </ul>
 */
public class FxStoreImplCoverageV16Test {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("coverage-v16-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== createOrOpenMap 테스트 ====================

    @Test
    public void createOrOpenMap_newCollection_shouldCreate() {
        NavigableMap<Long, String> map = store.createOrOpenMap("test", Long.class, String.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void createOrOpenMap_existingCollection_shouldOpen() {
        // 먼저 생성
        NavigableMap<Long, String> map1 = store.createMap("test", Long.class, String.class);
        map1.put(1L, "one");
        store.commit();

        // createOrOpen으로 열기
        NavigableMap<Long, String> map2 = store.createOrOpenMap("test", Long.class, String.class);
        assertNotNull(map2);
        assertEquals(1, map2.size());
        assertEquals("one", map2.get(1L));
    }

    @Test
    public void createOrOpenMap_afterReopen_shouldWork() throws Exception {
        // 생성 및 데이터 추가
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");
        store.commit();
        store.close();

        // 재오픈 후 createOrOpen
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map2 = store.createOrOpenMap("test", Long.class, String.class);
        assertEquals(2, map2.size());
        assertEquals("one", map2.get(1L));
        assertEquals("two", map2.get(2L));
    }

    // ==================== createOrOpenSet 테스트 ====================

    @Test
    public void createOrOpenSet_newCollection_shouldCreate() {
        NavigableSet<Long> set = store.createOrOpenSet("test", Long.class);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void createOrOpenSet_existingCollection_shouldOpen() {
        NavigableSet<Long> set1 = store.createSet("test", Long.class);
        set1.add(10L);
        store.commit();

        NavigableSet<Long> set2 = store.createOrOpenSet("test", Long.class);
        assertEquals(1, set2.size());
        assertTrue(set2.contains(10L));
    }

    // ==================== createOrOpenList 테스트 ====================

    @Test
    public void createOrOpenList_newCollection_shouldCreate() {
        List<String> list = store.createOrOpenList("test", String.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void createOrOpenList_existingCollection_shouldOpen() {
        List<String> list1 = store.createList("test", String.class);
        list1.add("a");
        store.commit();

        List<String> list2 = store.createOrOpenList("test", String.class);
        assertEquals(1, list2.size());
        assertEquals("a", list2.get(0));
    }

    // ==================== createOrOpenDeque 테스트 ====================

    @Test
    public void createOrOpenDeque_newCollection_shouldCreate() {
        Deque<String> deque = store.createOrOpenDeque("test", String.class);
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void createOrOpenDeque_existingCollection_shouldOpen() {
        Deque<String> deque1 = store.createDeque("test", String.class);
        deque1.addLast("first");
        store.commit();

        Deque<String> deque2 = store.createOrOpenDeque("test", String.class);
        assertEquals(1, deque2.size());
        assertEquals("first", deque2.peekFirst());
    }

    // ==================== list() 테스트 ====================

    @Test
    public void list_emptyStore_shouldReturnEmptyList() {
        java.util.List<CollectionInfo> collections = store.list();
        assertTrue(collections.isEmpty());
    }

    @Test
    public void list_withCollections_shouldReturnAllCollections() {
        store.createMap("map1", Long.class, String.class);
        store.createSet("set1", Long.class);
        store.createList("list1", String.class);
        store.createDeque("deque1", String.class);
        store.commit();

        java.util.List<CollectionInfo> collections = store.list();
        assertEquals(4, collections.size());

        boolean hasMap = collections.stream().anyMatch(c -> "map1".equals(c.name()));
        boolean hasSet = collections.stream().anyMatch(c -> "set1".equals(c.name()));
        boolean hasList = collections.stream().anyMatch(c -> "list1".equals(c.name()));
        boolean hasDeque = collections.stream().anyMatch(c -> "deque1".equals(c.name()));

        assertTrue(hasMap);
        assertTrue(hasSet);
        assertTrue(hasList);
        assertTrue(hasDeque);
    }

    // ==================== exists() 테스트 ====================

    @Test
    public void exists_nonExisting_shouldReturnFalse() {
        assertFalse(store.exists("nonexistent"));
    }

    @Test
    public void exists_existing_shouldReturnTrue() {
        store.createMap("test", Long.class, String.class);
        store.commit();

        assertTrue(store.exists("test"));
    }

    // ==================== stats() 테스트 ====================

    @Test
    public void stats_emptyStore_shouldReturnStats() {
        Stats stats = store.stats();

        assertNotNull(stats);
        assertEquals(0, stats.collectionCount());
    }

    @Test
    public void stats_withCollections_shouldReturnStats() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");

        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        store.commit();

        Stats stats = store.stats();

        assertNotNull(stats);
        assertEquals(2, stats.collectionCount());
    }

    @Test
    public void stats_fullMode_shouldReturnDetailedStats() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        for (long i = 1; i <= 100; i++) {
            map.put(i, "value" + i);
        }
        store.commit();

        Stats stats = store.stats(StatsMode.DEEP);

        assertNotNull(stats);
        assertTrue(stats.fileBytes() > 0);
    }

    // ==================== rename 테스트 ====================

    @Test
    public void rename_existingCollection_shouldSucceed() {
        store.createMap("original", Long.class, String.class);
        store.commit();

        assertTrue(store.exists("original"));
        assertFalse(store.exists("renamed"));

        store.rename("original", "renamed");
        store.commit();

        assertFalse(store.exists("original"));
        assertTrue(store.exists("renamed"));
    }

    @Test(expected = FxException.class)
    public void rename_nonExisting_shouldThrow() {
        store.rename("nonexistent", "newname");
    }

    // ==================== drop 테스트 ====================

    @Test
    public void drop_existingCollection_shouldRemove() {
        store.createMap("test", Long.class, String.class);
        store.commit();

        assertTrue(store.exists("test"));

        store.drop("test");
        store.commit();

        assertFalse(store.exists("test"));
    }

    @Test
    public void drop_nonExisting_shouldReturnFalse() {
        boolean result = store.drop("nonexistent");
        assertFalse(result);
    }

    // ==================== verify 테스트 ====================

    @Test
    public void verify_emptyStore_shouldPass() {
        VerifyResult result = store.verify();
        assertTrue(result.ok());
    }

    @Test
    public void verify_withData_shouldPass() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        for (long i = 1; i <= 50; i++) {
            map.put(i, "value" + i);
        }
        store.commit();

        VerifyResult result = store.verify();
        assertTrue(result.ok());
    }

    // ==================== compact 테스트 ====================

    @Test
    public void compactTo_shouldCreateCompactedCopy() throws Exception {
        // 데이터 추가
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        for (long i = 1; i <= 100; i++) {
            map.put(i, "value" + i);
        }
        store.commit();

        // 일부 삭제
        for (long i = 1; i <= 50; i++) {
            map.remove(i);
        }
        store.commit();

        // compact
        File compactFile = tempFolder.newFile("compact.fx");
        compactFile.delete();

        store.compactTo(compactFile.toPath());

        // 압축된 파일 검증
        try (FxStore compactStore = FxStore.open(compactFile.toPath())) {
            NavigableMap<Long, String> compactMap = compactStore.openMap("map", Long.class, String.class);
            assertEquals(50, compactMap.size());

            for (long i = 51; i <= 100; i++) {
                assertEquals("value" + i, compactMap.get(i));
            }
        }
    }

    // ==================== 메모리 스토어 테스트 ====================

    @Test
    public void memoryStore_shouldWorkCorrectly() throws Exception {
        try (FxStore memStore = FxStore.openMemory()) {
            NavigableMap<Long, String> map = memStore.createMap("test", Long.class, String.class);
            map.put(1L, "one");
            map.put(2L, "two");

            assertEquals(2, map.size());
            assertEquals("one", map.get(1L));
        }
    }

    // ==================== AutoCommit 테스트 ====================

    @Test
    public void autoCommit_shouldBeEnabledByDefault() {
        assertEquals(CommitMode.AUTO, store.commitMode());
    }

    @Test
    public void batchMode_commitMode_shouldBeBatch() throws Exception {
        store.close();

        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .build();

        store = FxStoreImpl.open(storeFile.toPath(), batchOptions);
        assertEquals(CommitMode.BATCH, store.commitMode());
    }

    // ==================== Codec 테스트 ====================

    @Test
    public void codecs_shouldReturnCodecRegistry() {
        FxCodecRegistry codecs = store.codecs();
        assertNotNull(codecs);
    }

    @Test
    public void registerCodec_customType_shouldWork() {
        FxCodec<TestRecord> codec = new FxCodec<TestRecord>() {
            @Override
            public String id() {
                return "test:TestRecord";
            }

            @Override
            public int version() {
                return 1;
            }

            @Override
            public byte[] encode(TestRecord value) {
                return (value.name + "|" + value.value).getBytes();
            }

            @Override
            public TestRecord decode(byte[] bytes) {
                String[] parts = new String(bytes).split("\\|");
                return new TestRecord(parts[0], Integer.parseInt(parts[1]));
            }

            @Override
            public int compareBytes(byte[] a, byte[] b) {
                // Compare by name (first field)
                String aStr = new String(a).split("\\|")[0];
                String bStr = new String(b).split("\\|")[0];
                return aStr.compareTo(bStr);
            }

            @Override
            public boolean equalsBytes(byte[] a, byte[] b) {
                return java.util.Arrays.equals(a, b);
            }

            @Override
            public int hashBytes(byte[] bytes) {
                return java.util.Arrays.hashCode(bytes);
            }
        };

        store.registerCodec(TestRecord.class, codec);

        NavigableSet<TestRecord> set = store.createSet("records", TestRecord.class);
        set.add(new TestRecord("a", 1));
        set.add(new TestRecord("b", 2));
        store.commit();

        assertEquals(2, set.size());
    }

    private static class TestRecord {
        final String name;
        final int value;

        TestRecord(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    // ==================== commitMode() 테스트 ====================

    @Test
    public void commitMode_shouldReturnCurrentMode() {
        CommitMode mode = store.commitMode();
        assertEquals(CommitMode.AUTO, mode);
    }

    // ==================== 다중 타입 컬렉션 테스트 ====================

    @Test
    public void multipleTypes_shouldWorkTogether() {
        // Map with Long key
        NavigableMap<Long, String> mapLong = store.createMap("mapLong", Long.class, String.class);
        mapLong.put(1L, "one");

        // Map with String key
        NavigableMap<String, Integer> mapString = store.createMap("mapString", String.class, Integer.class);
        mapString.put("key", 100);

        // Set with Integer
        NavigableSet<Integer> setInt = store.createSet("setInt", Integer.class);
        setInt.add(10);

        // Set with String
        NavigableSet<String> setString = store.createSet("setString", String.class);
        setString.add("element");

        store.commit();

        assertEquals("one", mapLong.get(1L));
        assertEquals(Integer.valueOf(100), mapString.get("key"));
        assertTrue(setInt.contains(10));
        assertTrue(setString.contains("element"));
    }

    // ==================== 대량 데이터 테스트 ====================

    @Test
    public void largeCollection_shouldWorkCorrectly() {
        NavigableMap<Long, String> map = store.createMap("large", Long.class, String.class);

        for (long i = 1; i <= 1000; i++) {
            map.put(i, "value" + i);
        }
        store.commit();

        assertEquals(1000, map.size());
        assertEquals("value1", map.get(1L));
        assertEquals("value1000", map.get(1000L));

        // 통계 확인
        Stats stats = store.stats();
        assertTrue(stats.fileBytes() > 0);
    }

    // ==================== 재오픈 일관성 테스트 ====================

    @Test
    public void reopen_shouldPreserveData() throws Exception {
        // 데이터 생성
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("set", Long.class);
        Deque<String> deque = store.createDeque("deque", String.class);
        List<String> list = store.createList("list", String.class);

        map.put(1L, "one");
        set.add(10L);
        deque.addLast("first");
        list.add("item");
        store.commit();

        // 닫기
        store.close();

        // 재오픈
        store = FxStore.open(storeFile.toPath());

        // 검증
        NavigableMap<Long, String> map2 = store.openMap("map", Long.class, String.class);
        assertEquals("one", map2.get(1L));

        NavigableSet<Long> set2 = store.openSet("set", Long.class);
        assertTrue(set2.contains(10L));

        Deque<String> deque2 = store.openDeque("deque", String.class);
        assertEquals("first", deque2.peekFirst());

        List<String> list2 = store.openList("list", String.class);
        assertEquals("item", list2.get(0));
    }

    // ==================== 닫힌 스토어 테스트 ====================

    @Test(expected = FxException.class)
    public void closedStore_createMap_shouldThrow() throws Exception {
        store.close();
        store.createMap("test", Long.class, String.class);
    }

    @Test(expected = FxException.class)
    public void closedStore_commit_shouldThrow() throws Exception {
        store.close();
        store.commit();
    }

    // ==================== beginRead 테스트 ====================

    @Test
    public void beginRead_shouldCreateTransaction() {
        store.createMap("test", Long.class, String.class);
        store.commit();

        try (FxReadTransaction tx = store.beginRead()) {
            assertNotNull(tx);
            assertTrue(tx.isActive());
        }
    }
}
