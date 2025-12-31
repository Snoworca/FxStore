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
 * FxStoreImpl 심층 경로 테스트
 *
 * <p>V18 커버리지 개선: countTreeBytes, Deque 트랜잭션 경로</p>
 */
public class FxStoreDeepPathTest {

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

    // ==================== countTreeBytes 트리 순회 경로 ====================

    @Test
    public void stats_deep_largeMap_shouldTraverseTree() throws Exception {
        // Given: 트리 분할을 유발하는 대량 데이터
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("largeMap", Long.class, String.class);

        // 충분히 많은 데이터로 트리 분할 유발
        for (int i = 0; i < 5000; i++) {
            map.put((long) i, "value-" + i + "-" + String.format("%0100d", i));
        }
        store.commit();

        // When: DEEP 통계
        Stats stats = store.stats(StatsMode.DEEP);

        // Then: 바이트 계산됨
        assertTrue(stats.fileBytes() > 0);
        assertEquals(1, stats.collectionCount());
    }

    @Test
    public void stats_deep_multipleCollections_shouldTraverseAllTrees() throws Exception {
        // Given: 여러 컬렉션에 대량 데이터
        store = FxStore.open(storeFile.toPath());

        NavigableMap<Long, String> map1 = store.createMap("map1", Long.class, String.class);
        for (int i = 0; i < 2000; i++) {
            map1.put((long) i, "value" + i);
        }

        NavigableMap<Long, String> map2 = store.createMap("map2", Long.class, String.class);
        for (int i = 0; i < 2000; i++) {
            map2.put((long) i, "data" + i);
        }

        store.commit();

        // When: DEEP 통계
        Stats stats = store.stats(StatsMode.DEEP);

        // Then: 2개 컬렉션
        assertEquals(2, stats.collectionCount());
        assertTrue(stats.fileBytes() > 0);
    }

    @Test
    public void stats_deep_setWithManyElements_shouldWork() throws Exception {
        // Given: 대량 Set
        store = FxStore.open(storeFile.toPath());
        NavigableSet<Long> set = store.createSet("largeSet", Long.class);

        for (int i = 0; i < 3000; i++) {
            set.add((long) i);
        }
        store.commit();

        // When: DEEP 통계
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertEquals(1, stats.collectionCount());
        assertTrue(stats.fileBytes() > 0);
    }

    @Test
    public void stats_deep_dequeWithManyElements_shouldWork() throws Exception {
        // Given: 대량 Deque
        store = FxStore.open(storeFile.toPath());
        Deque<String> deque = store.createDeque("largeDeque", String.class);

        for (int i = 0; i < 2000; i++) {
            deque.addLast("element-" + i);
        }
        store.commit();

        // When: DEEP 통계
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertEquals(1, stats.collectionCount());
        assertTrue(stats.fileBytes() > 0);
    }

    @Test
    public void stats_deep_listWithManyElements_shouldWork() throws Exception {
        // Given: 대량 List
        store = FxStore.open(storeFile.toPath());
        List<String> list = store.createList("largeList", String.class);

        for (int i = 0; i < 2000; i++) {
            list.add("item-" + i);
        }
        store.commit();

        // When: DEEP 통계
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertEquals(1, stats.collectionCount());
    }

    // ==================== Deque 트랜잭션 경로 ====================

    @Test
    public void readTransaction_deque_peekFirst_shouldWork() throws Exception {
        // Given: Deque with data
        store = FxStore.open(storeFile.toPath());
        Deque<String> deque = store.createDeque("testDeque", String.class);
        deque.addLast("first");
        deque.addLast("second");
        deque.addLast("third");
        store.commit();

        // When: 트랜잭션에서 peekFirst
        try (FxReadTransaction tx = store.beginRead()) {
            String first = tx.peekFirst(deque);
            assertEquals("first", first);
        }
    }

    @Test
    public void readTransaction_deque_peekLast_shouldWork() throws Exception {
        // Given: Deque with data
        store = FxStore.open(storeFile.toPath());
        Deque<String> deque = store.createDeque("testDeque", String.class);
        deque.addLast("first");
        deque.addLast("second");
        deque.addLast("third");
        store.commit();

        // When: 트랜잭션에서 peekLast
        try (FxReadTransaction tx = store.beginRead()) {
            String last = tx.peekLast(deque);
            assertEquals("third", last);
        }
    }

    @Test
    public void readTransaction_deque_size_shouldWork() throws Exception {
        // Given: Deque with data
        store = FxStore.open(storeFile.toPath());
        Deque<String> deque = store.createDeque("testDeque", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");
        store.commit();

        // When: 트랜잭션에서 size
        try (FxReadTransaction tx = store.beginRead()) {
            int size = tx.size(deque);
            assertEquals(3, size);
        }
    }

    @Test
    public void readTransaction_deque_empty_peekFirst_shouldReturnNull() throws Exception {
        // Given: 빈 Deque
        store = FxStore.open(storeFile.toPath());
        Deque<String> deque = store.createDeque("emptyDeque", String.class);
        store.commit();

        // When: 빈 Deque에서 peekFirst
        try (FxReadTransaction tx = store.beginRead()) {
            String result = tx.peekFirst(deque);
            assertNull(result);
        }
    }

    @Test
    public void readTransaction_deque_empty_peekLast_shouldReturnNull() throws Exception {
        // Given: 빈 Deque
        store = FxStore.open(storeFile.toPath());
        Deque<String> deque = store.createDeque("emptyDeque", String.class);
        store.commit();

        // When: 빈 Deque에서 peekLast
        try (FxReadTransaction tx = store.beginRead()) {
            String result = tx.peekLast(deque);
            assertNull(result);
        }
    }

    @Test
    public void readTransaction_deque_empty_size_shouldReturnZero() throws Exception {
        // Given: 빈 Deque
        store = FxStore.open(storeFile.toPath());
        Deque<String> deque = store.createDeque("emptyDeque", String.class);
        store.commit();

        // When: 빈 Deque에서 size
        try (FxReadTransaction tx = store.beginRead()) {
            int size = tx.size(deque);
            assertEquals(0, size);
        }
    }

    @Test
    public void readTransaction_deque_snapshotIsolation() throws Exception {
        // Given: Deque with initial data
        store = FxStore.open(storeFile.toPath());
        Deque<String> deque = store.createDeque("testDeque", String.class);
        deque.addLast("initial");
        store.commit();

        // When: 트랜잭션 시작 후 데이터 변경
        try (FxReadTransaction tx = store.beginRead()) {
            // 트랜잭션 외부에서 변경
            deque.addLast("added");
            store.commit();

            // Then: 트랜잭션은 스냅샷 시점의 데이터 조회
            // 스냅샷 격리이므로 새 데이터는 보이지 않아야 함
            int size = tx.size(deque);
            // 스냅샷이 이미 taken이므로 1이 반환되어야 함
            assertEquals(1, size);
        }
    }

    // ==================== verify 에러 경로 추가 ====================

    @Test
    public void verify_largeData_shouldPass() throws Exception {
        // Given: 대량 데이터
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("large", Long.class, String.class);

        for (int i = 0; i < 1000; i++) {
            map.put((long) i, "value" + i);
        }
        store.commit();

        // Then: verify 성공
        VerifyResult result = store.verify();
        assertTrue(result.ok());
    }

    // ==================== 다양한 타입 테스트 ====================

    @Test
    public void stats_deep_byteArrayKey_shouldWork() throws Exception {
        // Given: byte[] 키 맵
        store = FxStore.open(storeFile.toPath());
        NavigableMap<byte[], String> map = store.createMap("bytesMap", byte[].class, String.class);

        for (int i = 0; i < 500; i++) {
            byte[] key = new byte[]{(byte)(i >> 8), (byte)i, (byte)(i * 2)};
            map.put(key, "value" + i);
        }
        store.commit();

        // When
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertEquals(1, stats.collectionCount());
    }

    @Test
    public void stats_deep_doubleKey_shouldWork() throws Exception {
        // Given: Double 키 맵
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Double, String> map = store.createMap("doubleMap", Double.class, String.class);

        for (int i = 0; i < 500; i++) {
            map.put((double) i + 0.5, "value" + i);
        }
        store.commit();

        // When
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertEquals(1, stats.collectionCount());
    }

    // ==================== compactTo 추가 경로 ====================

    @Test
    public void compactTo_largeData_shouldCopyCorrectly() throws Exception {
        // Given: 대량 데이터
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("large", Long.class, String.class);

        for (int i = 0; i < 1000; i++) {
            map.put((long) i, "value" + i);
        }
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 복사됨
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableMap<Long, String> targetMap = targetStore.openMap("large", Long.class, String.class);
            assertEquals(1000, targetMap.size());
            assertEquals("value0", targetMap.get(0L));
            assertEquals("value999", targetMap.get(999L));
        }
    }

    // ==================== 동시성 관련 ====================

    @Test
    public void multipleReadTransactions_shouldWork() throws Exception {
        // Given: 데이터가 있는 Store
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");
        store.commit();

        // When: 여러 트랜잭션 동시 시작
        try (FxReadTransaction tx1 = store.beginRead();
             FxReadTransaction tx2 = store.beginRead()) {

            // Then: 둘 다 데이터 조회 가능
            assertEquals("one", tx1.get(map, 1L));
            assertEquals("two", tx2.get(map, 2L));
        }
    }

    @Test
    public void readTransaction_afterModification_shouldSeeOldData() throws Exception {
        // Given: 데이터가 있는 Store
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "original");
        store.commit();

        // When: 트랜잭션 시작 후 수정
        try (FxReadTransaction tx = store.beginRead()) {
            map.put(1L, "modified");
            store.commit();

            // Then: 트랜잭션은 원래 값 조회
            assertEquals("original", tx.get(map, 1L));
        }
    }
}
