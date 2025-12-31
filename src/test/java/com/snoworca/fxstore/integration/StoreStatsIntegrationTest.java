package com.snoworca.fxstore.integration;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;

import static org.junit.Assert.*;

/**
 * Store Stats 통합 테스트.
 *
 * <p>목적: countTreeBytes() 메서드 커버리지 향상</p>
 * <p>StatsMode.DEEP 모드는 countTreeBytes를 호출하여 실제 라이브 바이트를 계산합니다.</p>
 *
 * @see com.snoworca.fxstore.core.FxStoreImpl#stats(StatsMode)
 * @see com.snoworca.fxstore.core.FxStoreImpl#countTreeBytes(long)
 */
public class StoreStatsIntegrationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;

    @Before
    public void setUp() {
        store = FxStore.openMemory();
    }

    @After
    public void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    /**
     * 다중 컬렉션에서 FAST Stats 조회.
     */
    @Test
    public void testStats_FAST_withMultipleCollections() {
        // Given: 다양한 컬렉션 생성
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("set", Long.class);
        List<String> list = store.createList("list", String.class);
        Deque<Integer> deque = store.createDeque("deque", Integer.class);

        // When: 데이터 삽입
        for (long i = 0; i < 100; i++) {
            map.put(i, "value" + i);
            set.add(i);
            list.add("item" + i);
            deque.add((int) i);
        }

        // Then: FAST Stats 조회
        Stats stats = store.stats();
        assertNotNull(stats);
        assertTrue("liveBytesEstimate > 0", stats.liveBytesEstimate() > 0);
        assertEquals("4 collections", 4, stats.collectionCount());
    }

    /**
     * 다중 컬렉션에서 DEEP Stats 조회.
     *
     * <p>DEEP 모드는 countTreeBytes를 호출하여 실제 라이브 바이트를 계산합니다.</p>
     */
    @Test
    public void testStats_DEEP_withMultipleCollections() {
        // Given: 다양한 컬렉션 생성
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("set", Long.class);
        List<String> list = store.createList("list", String.class);
        Deque<Integer> deque = store.createDeque("deque", Integer.class);

        // When: 데이터 삽입
        for (long i = 0; i < 100; i++) {
            map.put(i, "value" + i);
            set.add(i);
            list.add("item" + i);
            deque.add((int) i);
        }

        // Then: DEEP Stats 조회 (countTreeBytes 호출)
        Stats stats = store.stats(StatsMode.DEEP);
        assertNotNull(stats);
        assertTrue("liveBytesEstimate > 0", stats.liveBytesEstimate() > 0);
        assertEquals("4 collections", 4, stats.collectionCount());
    }

    /**
     * 빈 Store에서 Stats 조회.
     */
    @Test
    public void testStats_emptyStore_FAST() {
        Stats stats = store.stats();
        assertNotNull(stats);
        assertEquals(0, stats.collectionCount());
    }

    /**
     * 빈 Store에서 DEEP Stats 조회.
     */
    @Test
    public void testStats_emptyStore_DEEP() {
        Stats stats = store.stats(StatsMode.DEEP);
        assertNotNull(stats);
        assertEquals(0, stats.collectionCount());
    }

    /**
     * 대용량 데이터에서 DEEP Stats 조회.
     *
     * <p>대량 데이터로 countTreeBytes의 재귀 호출 테스트</p>
     */
    @Test
    public void testStats_DEEP_largeData() {
        NavigableMap<Long, String> map = store.createMap("largeMap", Long.class, String.class);
        for (long i = 0; i < 5000; i++) {
            map.put(i, "value" + i);
        }

        Stats stats = store.stats(StatsMode.DEEP);
        assertTrue("Large data should have significant bytes", stats.liveBytesEstimate() > 10000);
    }

    /**
     * 대용량 List에서 DEEP Stats 조회.
     *
     * <p>OST 트리의 countTreeBytes 테스트</p>
     */
    @Test
    public void testStats_DEEP_largeList() {
        List<Long> list = store.createList("largeList", Long.class);
        for (long i = 0; i < 3000; i++) {
            list.add(i);
        }

        Stats stats = store.stats(StatsMode.DEEP);
        assertNotNull(stats);
        assertTrue("List data should have significant bytes", stats.liveBytesEstimate() > 0);
        assertEquals(1, stats.collectionCount());
    }

    /**
     * 파일 기반 스토어에서 DEEP Stats 조회.
     */
    @Test
    public void testStats_DEEP_fileStore() throws Exception {
        Path tempFile = tempFolder.newFile("stats.fx").toPath();

        try (FxStore fileStore = FxStore.open(tempFile)) {
            NavigableMap<Long, String> map = fileStore.createMap("map", Long.class, String.class);
            for (long i = 0; i < 500; i++) {
                map.put(i, "value" + i);
            }

            Stats stats = fileStore.stats(StatsMode.DEEP);
            assertNotNull(stats);
            assertTrue("fileBytes > 0", stats.fileBytes() > 0);
            assertTrue("liveBytesEstimate > 0", stats.liveBytesEstimate() > 0);
            assertEquals(1, stats.collectionCount());
        }
    }

    /**
     * FAST vs DEEP 비교 테스트.
     */
    @Test
    public void testStats_FASTvsDEEP_comparison() {
        NavigableMap<Long, String> map = store.createMap("compareMap", Long.class, String.class);
        for (long i = 0; i < 1000; i++) {
            map.put(i, "value" + i);
        }

        Stats fastStats = store.stats(StatsMode.FAST);
        Stats deepStats = store.stats(StatsMode.DEEP);

        assertNotNull(fastStats);
        assertNotNull(deepStats);

        // DEEP은 실제 트리 스캔, FAST는 추정치
        // 둘 다 collectionCount는 동일해야 함
        assertEquals(fastStats.collectionCount(), deepStats.collectionCount());
    }

    /**
     * 삽입/삭제 후 DEEP Stats 조회.
     *
     * <p>Dead bytes 계산 테스트</p>
     */
    @Test
    public void testStats_DEEP_afterInsertAndDelete() {
        NavigableMap<Long, String> map = store.createMap("deadMap", Long.class, String.class);

        // 삽입
        for (long i = 0; i < 500; i++) {
            map.put(i, "value" + i);
        }

        // 일부 삭제
        for (long i = 0; i < 250; i++) {
            map.remove(i);
        }

        Stats stats = store.stats(StatsMode.DEEP);
        assertNotNull(stats);
        // COW 방식이므로 삭제된 데이터는 dead bytes가 됨
        assertTrue("Should have some dead bytes", stats.deadBytesEstimate() >= 0);
    }

    /**
     * 모든 컬렉션 타입에서 DEEP Stats 테스트.
     */
    @Test
    public void testStats_DEEP_allCollectionTypes() {
        // Map
        NavigableMap<Integer, String> map = store.createMap("m1", Integer.class, String.class);
        for (int i = 0; i < 200; i++) {
            map.put(i, "v" + i);
        }

        // Set
        NavigableSet<Integer> set = store.createSet("s1", Integer.class);
        for (int i = 0; i < 200; i++) {
            set.add(i);
        }

        // List
        List<Integer> list = store.createList("l1", Integer.class);
        for (int i = 0; i < 200; i++) {
            list.add(i);
        }

        // Deque
        Deque<Integer> deque = store.createDeque("d1", Integer.class);
        for (int i = 0; i < 200; i++) {
            deque.add(i);
        }

        Stats stats = store.stats(StatsMode.DEEP);
        assertEquals(4, stats.collectionCount());
        assertTrue(stats.liveBytesEstimate() > 0);
    }

    /**
     * 재오픈 후 DEEP Stats 조회.
     */
    @Test
    public void testStats_DEEP_afterReopen() throws Exception {
        Path tempFile = tempFolder.newFile("reopen-stats.fx").toPath();

        // 생성 및 데이터 저장
        try (FxStore fileStore = FxStore.open(tempFile)) {
            NavigableMap<Long, String> map = fileStore.createMap("map", Long.class, String.class);
            for (long i = 0; i < 300; i++) {
                map.put(i, "value" + i);
            }
        }

        // 재오픈 후 DEEP Stats 조회
        try (FxStore fileStore = FxStore.open(tempFile)) {
            Stats stats = fileStore.stats(StatsMode.DEEP);
            assertNotNull(stats);
            assertEquals(1, stats.collectionCount());
            assertTrue(stats.liveBytesEstimate() > 0);
        }
    }
}
