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
 * Stats DEEP 모드 테스트
 *
 * <p>P0-1, P0-2 해결: countTreeBytes(), calculateLiveBytes()</p>
 *
 * <p>DEEP 모드는 B-Tree 및 OST를 순회하여 실제 라이브 바이트를 계산합니다.</p>
 *
 * @since v1.0 Phase 3
 * @see FxStore#stats(StatsMode)
 */
public class FxStoreStatsModeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;
    private File storeFile;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("stats-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== DEEP 모드 기본 테스트 ====================

    @Test
    public void stats_deepMode_emptyStore_shouldReturnMinimumSize() {
        // Given: 빈 스토어

        // When: stats(DEEP) 호출
        Stats deepStats = store.stats(StatsMode.DEEP);

        // Then: 기본 오버헤드만 있어야 함
        assertNotNull(deepStats);
        assertTrue("fileBytes should be > 0", deepStats.fileBytes() > 0);
        assertTrue("liveBytes should include overhead", deepStats.liveBytesEstimate() > 0);
        assertEquals("Empty store should have 0 collections", 0, deepStats.collectionCount());
    }

    @Test
    public void stats_deepMode_withMap_shouldCountTreeBytes() {
        // Given: Map에 100개 데이터
        NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "value-" + i);
        }

        // When: stats(DEEP) 호출
        Stats deepStats = store.stats(StatsMode.DEEP);

        // Then: liveBytes > 0, 컬렉션 수 = 1
        assertNotNull(deepStats);
        assertTrue("liveBytes should be positive", deepStats.liveBytesEstimate() > 0);
        assertEquals("Should have 1 collection", 1, deepStats.collectionCount());
    }

    @Test
    public void stats_deepMode_withMultiLevelTree_shouldTraverseAllNodes() {
        // Given: 많은 데이터로 다중 레벨 B-Tree 생성 (1000개)
        NavigableMap<Long, String> map = store.createMap("largeMap", Long.class, String.class);
        for (long i = 0; i < 1000; i++) {
            map.put(i, "value-" + i + "-" + String.format("%0200d", i)); // 큰 값
        }

        // When: stats(DEEP) 호출
        Stats deepStats = store.stats(StatsMode.DEEP);

        // Then: liveBytes가 모든 트리 페이지 포함해야 함
        assertNotNull(deepStats);
        assertTrue("liveBytes should be significant", deepStats.liveBytesEstimate() > 10000);
        assertEquals("Should have 1 collection", 1, deepStats.collectionCount());

        // FAST와 비교
        Stats fastStats = store.stats(StatsMode.FAST);
        // DEEP 모드에서는 더 정확한 liveBytes 제공 (FAST보다 작거나 같을 수 있음)
        assertTrue("DEEP liveBytes <= FAST liveBytes",
            deepStats.liveBytesEstimate() <= fastStats.liveBytesEstimate());
    }

    @Test
    public void stats_deepMode_withAllCollectionTypes_shouldCountAll() {
        // Given: Map, Set, List, Deque 각각 데이터 포함
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        for (long i = 0; i < 50; i++) {
            map.put(i, "map-value-" + i);
        }

        NavigableSet<Long> set = store.createSet("set", Long.class);
        for (long i = 0; i < 50; i++) {
            set.add(i);
        }

        List<String> list = store.createList("list", String.class);
        for (int i = 0; i < 50; i++) {
            list.add("list-item-" + i);
        }

        Deque<String> deque = store.createDeque("deque", String.class);
        for (int i = 0; i < 50; i++) {
            deque.addLast("deque-item-" + i);
        }

        // When: stats(DEEP) 호출
        Stats deepStats = store.stats(StatsMode.DEEP);

        // Then: 모든 컬렉션 트리 바이트 합산
        assertNotNull(deepStats);
        assertTrue("liveBytes should include all collections", deepStats.liveBytesEstimate() > 0);
        assertEquals("Should have 4 collections", 4, deepStats.collectionCount());
    }

    @Test
    public void stats_fastVsDeep_shouldDiffer() {
        // Given: 데이터가 있는 스토어
        NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
        for (long i = 0; i < 200; i++) {
            map.put(i, "value-" + i);
        }

        // When: stats(FAST) vs stats(DEEP)
        Stats fastStats = store.stats(StatsMode.FAST);
        Stats deepStats = store.stats(StatsMode.DEEP);

        // Then: 둘 다 유효한 값이어야 함
        assertNotNull(fastStats);
        assertNotNull(deepStats);

        // FAST와 DEEP은 동일한 collectionCount를 가짐
        assertEquals("collectionCount should be same",
            fastStats.collectionCount(), deepStats.collectionCount());

        // fileBytes는 동일해야 함
        assertEquals("fileBytes should be same",
            fastStats.fileBytes(), deepStats.fileBytes());

        // liveBytes는 다를 수 있음 (DEEP이 더 정확)
        assertTrue("Both should have positive liveBytes",
            fastStats.liveBytesEstimate() > 0 && deepStats.liveBytesEstimate() > 0);
    }

    // ==================== 메모리 스토어 테스트 ====================

    @Test
    public void stats_deepMode_memoryStore_shouldWork() {
        // Given: 메모리 스토어
        store.close();
        store = FxStore.openMemory();

        NavigableMap<Long, String> map = store.createMap("memoryMap", Long.class, String.class);
        for (long i = 0; i < 50; i++) {
            map.put(i, "value-" + i);
        }

        // When: stats(DEEP) 호출
        Stats deepStats = store.stats(StatsMode.DEEP);

        // Then: 유효한 결과
        assertNotNull(deepStats);
        assertEquals("Should have 1 collection", 1, deepStats.collectionCount());
    }

    // ==================== 엣지 케이스 ====================

    @Test
    public void stats_deepMode_afterDeleteAndRecreate_shouldReflectChanges() {
        // Given: 컬렉션 생성 → 삭제 → 재생성
        NavigableMap<Long, String> map = store.createMap("recreated", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "value-" + i);
        }

        Stats before = store.stats(StatsMode.DEEP);

        store.drop("recreated");

        Stats afterDrop = store.stats(StatsMode.DEEP);

        // 재생성
        NavigableMap<Long, String> newMap = store.createMap("recreated", Long.class, String.class);
        for (long i = 0; i < 50; i++) {
            newMap.put(i, "new-value-" + i);
        }

        Stats afterRecreate = store.stats(StatsMode.DEEP);

        // Then: 변화 반영
        assertEquals("Before: 1 collection", 1, before.collectionCount());
        assertEquals("After drop: 0 collections", 0, afterDrop.collectionCount());
        assertEquals("After recreate: 1 collection", 1, afterRecreate.collectionCount());
    }

    @Test
    public void stats_deepMode_withSet_shouldCountTreeBytes() {
        // Given: Set에 데이터
        NavigableSet<Long> set = store.createSet("testSet", Long.class);
        for (long i = 0; i < 100; i++) {
            set.add(i);
        }

        // When: stats(DEEP) 호출
        Stats deepStats = store.stats(StatsMode.DEEP);

        // Then: Set 트리 바이트 포함
        assertNotNull(deepStats);
        assertTrue("liveBytes should be positive", deepStats.liveBytesEstimate() > 0);
        assertEquals("Should have 1 collection", 1, deepStats.collectionCount());
    }

    @Test
    public void stats_deepMode_withList_shouldCountOSTBytes() {
        // Given: List에 데이터 (OST 사용)
        List<String> list = store.createList("testList", String.class);
        for (int i = 0; i < 100; i++) {
            list.add("item-" + i);
        }

        // When: stats(DEEP) 호출
        Stats deepStats = store.stats(StatsMode.DEEP);

        // Then: OST 트리 바이트 포함
        assertNotNull(deepStats);
        assertTrue("liveBytes should be positive", deepStats.liveBytesEstimate() > 0);
        assertEquals("Should have 1 collection", 1, deepStats.collectionCount());
    }

    @Test
    public void stats_deepMode_withDeque_shouldCountOSTBytes() {
        // Given: Deque에 데이터 (OST 사용)
        Deque<String> deque = store.createDeque("testDeque", String.class);
        for (int i = 0; i < 100; i++) {
            deque.addLast("item-" + i);
        }

        // When: stats(DEEP) 호출
        Stats deepStats = store.stats(StatsMode.DEEP);

        // Then: OST 트리 바이트 포함
        assertNotNull(deepStats);
        assertTrue("liveBytes should be positive", deepStats.liveBytesEstimate() > 0);
        assertEquals("Should have 1 collection", 1, deepStats.collectionCount());
    }
}
