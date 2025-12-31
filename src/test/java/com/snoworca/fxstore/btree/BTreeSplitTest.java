package com.snoworca.fxstore.btree;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * BTree 분할 동작 테스트 (BTreeInternal.SplitResult 간접 커버)
 * P0 클래스 커버리지 개선
 */
public class BTreeSplitTest {

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

    // ==================== 순차 삽입으로 분할 유도 ====================

    @Test
    public void insert_sequential_shouldCauseSplits() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("splitTest", Long.class, String.class);

        // When: 충분한 데이터 삽입하여 노드 분할 유도
        int count = 5000;
        for (long i = 0; i < count; i++) {
            map.put(i, "value" + i);
        }

        // Then: 모든 데이터가 올바르게 저장됨
        assertEquals(count, map.size());
        for (long i = 0; i < count; i++) {
            assertEquals("value" + i, map.get(i));
        }
    }

    @Test
    public void insert_reverseSequential_shouldCauseSplits() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("reverseSplit", Long.class, String.class);

        // When: 역순 삽입
        int count = 2000;
        for (long i = count - 1; i >= 0; i--) {
            map.put(i, "v" + i);
        }

        // Then: 모든 데이터 삽입 확인
        assertEquals(count, map.size());
        // firstKey/lastKey 대신 get으로 검증
        assertEquals("v0", map.get(0L));
        assertEquals("v" + (count - 1), map.get((long)(count - 1)));
    }

    // ==================== 랜덤 삽입으로 분할 검증 ====================

    @Test
    public void insert_randomOrder_shouldCauseSplits() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("randomSplit", Long.class, String.class);
        List<Long> keys = new ArrayList<>();
        for (long i = 0; i < 2000; i++) {
            keys.add(i);
        }
        Collections.shuffle(keys, new Random(42));

        // When
        for (Long key : keys) {
            map.put(key, "v" + key);
        }

        // Then: 크기 확인 및 일부 키 검증
        assertEquals(2000, map.size());
        assertEquals("v0", map.get(0L));
        assertEquals("v1999", map.get(1999L));
    }

    @Test
    public void insert_alternatingPattern_shouldCauseSplits() {
        // Given: 교대 패턴으로 삽입
        NavigableMap<Long, String> map = memoryStore.createMap("alternateSplit", Long.class, String.class);

        // When
        int count = 2000;
        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                map.put((long) i / 2, "low" + i);
            } else {
                map.put((long) (count - 1 - i / 2), "high" + i);
            }
        }

        // Then
        assertEquals(count, map.size());
    }

    // ==================== 삭제 후 재삽입 ====================

    @Test
    public void deleteAndReinsert_shouldMaintainStructure() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("deleteReinsert", Long.class, String.class);
        for (long i = 0; i < 500; i++) {
            map.put(i, "v" + i);
        }

        // When: 절반 삭제 후 재삽입
        for (long i = 0; i < 250; i++) {
            map.remove(i);
        }
        assertEquals(250, map.size());

        for (long i = 0; i < 250; i++) {
            map.put(i, "new" + i);
        }

        // Then
        assertEquals(500, map.size());
        for (long i = 0; i < 250; i++) {
            assertEquals("new" + i, map.get(i));
        }
        for (long i = 250; i < 500; i++) {
            assertEquals("v" + i, map.get(i));
        }
    }

    @Test
    public void deleteAll_shouldEmptyTree() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("deleteAll", Long.class, String.class);
        for (long i = 0; i < 500; i++) {
            map.put(i, "v" + i);
        }

        // When: 모든 항목 삭제
        for (long i = 0; i < 500; i++) {
            map.remove(i);
        }

        // Then
        assertEquals(0, map.size());
    }

    // ==================== 대량 데이터 테스트 ====================

    @Test
    public void insert_largeDataset_shouldPreserveOrder() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("largeDataset", Long.class, String.class);
        int count = 2000;

        // When
        for (long i = 0; i < count; i++) {
            map.put(i, "value" + i);
        }

        // Then: 데이터 검증
        assertEquals(count, map.size());
        assertEquals("value0", map.get(0L));
        assertEquals("value" + (count - 1), map.get((long)(count - 1)));

        // 네비게이션 메서드 검증
        assertEquals(Long.valueOf(1000L), map.floorKey(1000L));
        assertEquals(Long.valueOf(1001L), map.ceilingKey(1001L));
    }

    @Test
    public void insert_duplicateKeys_shouldUpdate() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("duplicateKeys", Long.class, String.class);

        // When: 같은 키로 여러 번 삽입
        for (long i = 0; i < 500; i++) {
            map.put(i, "first" + i);
        }
        for (long i = 0; i < 500; i++) {
            map.put(i, "second" + i);
        }

        // Then: 마지막 값만 유지
        assertEquals(500, map.size());
        for (long i = 0; i < 500; i++) {
            assertEquals("second" + i, map.get(i));
        }
    }

    // ==================== SubMap 테스트 ====================

    @Test
    public void subMap_afterSplits_shouldWork() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("subMapTest", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "v" + i);
        }

        // When: 서브맵 생성 (20-50 범위)
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 50L, false);

        // Then: 기본 검증 - 범위 내 키만 접근 가능
        assertTrue(subMap.containsKey(20L));
        assertTrue(subMap.containsKey(49L));
        assertFalse(subMap.containsKey(50L));
        assertFalse(subMap.containsKey(19L));
        assertEquals("v30", subMap.get(30L));
    }

    @Test
    public void headMap_afterSplits_shouldWork() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("headMapTest", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "v" + i);
        }

        // When: 50 미만
        NavigableMap<Long, String> headMap = map.headMap(50L, false);

        // Then: 범위 검증
        assertTrue(headMap.containsKey(0L));
        assertTrue(headMap.containsKey(49L));
        assertFalse(headMap.containsKey(50L));
        assertEquals("v25", headMap.get(25L));
    }

    @Test
    public void tailMap_afterSplits_shouldWork() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("tailMapTest", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "v" + i);
        }

        // When: 50 이상
        NavigableMap<Long, String> tailMap = map.tailMap(50L, true);

        // Then: 범위 검증
        assertTrue(tailMap.containsKey(50L));
        assertTrue(tailMap.containsKey(99L));
        assertFalse(tailMap.containsKey(49L));
        assertEquals("v75", tailMap.get(75L));
    }

    // ==================== Descending 테스트 ====================

    @Test
    public void descendingMap_afterSplits_shouldWork() {
        // Given
        NavigableMap<Long, String> map = memoryStore.createMap("descendingTest", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "v" + i);
        }

        // When
        NavigableMap<Long, String> descMap = map.descendingMap();

        // Then: 역순 접근 가능
        assertEquals("v0", descMap.get(0L));
        assertEquals("v99", descMap.get(99L));
        assertTrue(descMap.containsKey(50L));
    }

    // ==================== BUG-V11-002: 리프 분할 후 nextLeaf 연결 검증 ====================

    /**
     * TC-V11-002-01: 리프 분할 후 nextLeaf 연결 검증
     *
     * <p>BUG-V11-002 수정 후 리프 간 연결이 올바르게 유지되는지 확인
     */
    @Test
    public void testLeafSplitNextLeafLink() {
        NavigableMap<Integer, String> map = memoryStore.createMap("nextLeafTest", Integer.class, String.class);

        // 리프 용량 초과하도록 충분한 엔트리 삽입
        for (int i = 0; i < 200; i++) {
            map.put(i, "value-" + i);
        }

        // 모든 엔트리가 순서대로 순회되어야 함 (nextLeaf 링크 정상)
        int expected = 0;
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            assertEquals(Integer.valueOf(expected), entry.getKey());
            expected++;
        }
        assertEquals(200, expected);
    }

    /**
     * TC-V11-002-02: 대량 삽입 후 무결성 검증
     */
    @Test
    public void testMassInsertIntegrity() {
        NavigableMap<Integer, String> map = memoryStore.createMap("massInsert", Integer.class, String.class);

        // 1000개 삽입 (다수의 분할 유발)
        for (int i = 0; i < 1000; i++) {
            map.put(i, "v" + i);
        }

        // 크기 확인
        assertEquals(1000, map.size());

        // 모든 키 존재 확인
        for (int i = 0; i < 1000; i++) {
            assertTrue("Key " + i + " should exist", map.containsKey(i));
            assertEquals("v" + i, map.get(i));
        }
    }

    /**
     * TC-V11-002-03: 스토어 재오픈 후 무결성
     */
    @Test
    public void testLeafSplitPersistence() throws Exception {
        File tempFile = tempFolder.newFile("split-persist.fx");
        tempFile.delete();

        // 1. 데이터 삽입
        try (FxStore store1 = FxStore.open(tempFile.toPath())) {
            NavigableMap<Integer, String> map = store1.createMap("test", Integer.class, String.class);
            for (int i = 0; i < 500; i++) {
                map.put(i, "value-" + i);
            }
        }

        // 2. 재오픈 후 검증
        try (FxStore store2 = FxStore.open(tempFile.toPath())) {
            NavigableMap<Integer, String> map = store2.openMap("test", Integer.class, String.class);
            assertEquals(500, map.size());

            // 모든 키 존재 및 값 확인
            for (int i = 0; i < 500; i++) {
                assertTrue("Key " + i + " should exist", map.containsKey(i));
                assertEquals("value-" + i, map.get(i));
            }
        }
    }

    // ==================== 파일 스토리지 테스트 ====================

    @Test
    public void fileStore_splitAndPersist_shouldWork() throws Exception {
        // Given
        File tempFile = tempFolder.newFile("split.fx");
        tempFile.delete();
        FxStore fileStore = FxStore.open(tempFile.toPath());
        try {
            NavigableMap<Long, String> map = fileStore.createMap("fileSplit", Long.class, String.class);

            // When: 충분한 데이터 삽입하여 분할 유도
            for (long i = 0; i < 2000; i++) {
                map.put(i, "value" + i);
            }

            // Then
            assertEquals(2000, map.size());
            assertEquals("value1000", map.get(1000L));
        } finally {
            fileStore.close();
        }
    }

    @Test
    public void fileStore_randomInsertDelete_shouldWork() throws Exception {
        // Given
        File tempFile = tempFolder.newFile("random.fx");
        tempFile.delete();
        FxStore fileStore = FxStore.open(tempFile.toPath());
        try {
            NavigableMap<Long, String> map = fileStore.createMap("fileRandomOps", Long.class, String.class);
            Random random = new Random(42);

            // When: 랜덤 삽입 및 삭제
            Set<Long> inserted = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                long key = random.nextLong() & 0x7FFFFFFF; // positive only
                map.put(key, "v" + key);
                inserted.add(key);
            }

            // Then: 일부 삭제 후 검증
            for (Long key : new ArrayList<>(inserted)) {
                if (random.nextBoolean()) {
                    map.remove(key);
                    inserted.remove(key);
                }
            }

            assertEquals(inserted.size(), map.size());
        } finally {
            fileStore.close();
        }
    }
}
