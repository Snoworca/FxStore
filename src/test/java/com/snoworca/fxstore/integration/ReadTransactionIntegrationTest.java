package com.snoworca.fxstore.integration;

import com.snoworca.fxstore.api.*;
import com.snoworca.fxstore.core.FxStoreImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.Assert.*;

/**
 * ReadTransaction 통합 테스트
 *
 * <p>Phase C: 동시성 시나리오 및 스냅샷 격리 검증</p>
 */
public class ReadTransactionIntegrationTest {

    private FxStore store;
    private ExecutorService executor;

    @Before
    public void setUp() {
        store = FxStoreImpl.openMemory(FxOptions.defaults());
        executor = Executors.newFixedThreadPool(8);
    }

    @After
    public void tearDown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (store != null) {
            try {
                store.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    // ==================== 동시성 테스트 ====================

    @Test
    public void testConcurrentReads_noContention() throws Exception {
        // Given: 초기 데이터
        NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "value-" + i);
        }

        // When: 여러 스레드에서 동시에 읽기 트랜잭션 수행
        int numReaders = 10;
        CountDownLatch latch = new CountDownLatch(numReaders);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicReference<Throwable> error = new AtomicReference<>(null);

        for (int i = 0; i < numReaders; i++) {
            final int readerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        try (FxReadTransaction tx = store.beginRead()) {
                            long key = (readerId * 10 + j) % 100;
                            String value = tx.get(map, key);
                            assertEquals("value-" + key, value);
                        }
                    }
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertNull("Unexpected error: " + error.get(), error.get());
        assertEquals(numReaders, successCount.get());
    }

    @Test
    public void testConcurrentReadsWithWriter_snapshotIsolation() throws Exception {
        // Given: 초기 데이터
        NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "initial-" + i);
        }

        int numReaders = 4;
        int numWriteOps = 50;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch readersDone = new CountDownLatch(numReaders);
        AtomicBoolean writerDone = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>(null);
        AtomicInteger isolationViolations = new AtomicInteger(0);

        // 독자 스레드들: 트랜잭션 내에서 일관된 읽기 확인
        for (int i = 0; i < numReaders; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await();

                    for (int j = 0; j < 20; j++) {
                        try (FxReadTransaction tx = store.beginRead()) {
                            // 트랜잭션 시작 시점의 스냅샷 캡처
                            long seqNo = tx.getSnapshotSeqNo();
                            Map<Long, String> snapshot = new HashMap<>();
                            for (long key = 0; key < 10; key++) {
                                String value = tx.get(map, key);
                                if (value != null) {
                                    snapshot.put(key, value);
                                }
                            }

                            // 잠시 대기하여 writer가 쓸 시간 제공
                            Thread.sleep(1);

                            // 같은 트랜잭션 내에서 다시 읽기
                            for (long key = 0; key < 10; key++) {
                                String current = tx.get(map, key);
                                String expected = snapshot.get(key);

                                // 스냅샷 격리 검증: 같은 트랜잭션 내에서 동일 값
                                if (!Objects.equals(current, expected)) {
                                    isolationViolations.incrementAndGet();
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                } finally {
                    readersDone.countDown();
                }
            });
        }

        // 쓰기 스레드: 지속적으로 데이터 업데이트
        executor.submit(() -> {
            try {
                startSignal.await();

                for (int i = 0; i < numWriteOps; i++) {
                    long key = i % 100;
                    map.put(key, "updated-" + key + "-" + i);
                    Thread.sleep(2);
                }
                writerDone.set(true);
            } catch (Throwable t) {
                error.compareAndSet(null, t);
            }
        });

        // Start
        startSignal.countDown();

        assertTrue(readersDone.await(30, TimeUnit.SECONDS));
        assertNull("Unexpected error: " + error.get(), error.get());
        assertEquals("Snapshot isolation violations", 0, isolationViolations.get());
    }

    @Test
    public void testLongRunningTransaction_isolatedFromWrites() throws Exception {
        // Given: 초기 데이터
        NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
        map.put(1L, "original");

        // When: 장기 실행 읽기 트랜잭션 시작
        FxReadTransaction tx = store.beginRead();
        assertEquals("original", tx.get(map, 1L));
        long initialSeqNo = tx.getSnapshotSeqNo();

        // 트랜잭션이 열린 상태에서 여러 번 쓰기
        for (int i = 0; i < 10; i++) {
            map.put(1L, "updated-" + i);
        }

        // Then: 장기 실행 트랜잭션은 여전히 원래 값 보유
        assertEquals("original", tx.get(map, 1L));
        assertEquals(initialSeqNo, tx.getSnapshotSeqNo());

        tx.close();

        // 새 트랜잭션에서는 최신 값
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertEquals("updated-9", tx2.get(map, 1L));
            assertTrue(tx2.getSnapshotSeqNo() > initialSeqNo);
        }
    }

    @Test
    public void testMultipleCollections_sameSnapshot() throws Exception {
        // Given: 여러 컬렉션에 데이터
        NavigableMap<Long, String> map1 = store.createMap("map1", Long.class, String.class);
        NavigableMap<Long, String> map2 = store.createMap("map2", Long.class, String.class);

        map1.put(1L, "m1-original");
        map2.put(1L, "m2-original");

        // When: 하나의 읽기 트랜잭션에서 여러 컬렉션 접근
        FxReadTransaction tx = store.beginRead();
        assertEquals("m1-original", tx.get(map1, 1L));
        assertEquals("m2-original", tx.get(map2, 1L));

        // 트랜잭션 중에 양쪽 컬렉션 업데이트
        map1.put(1L, "m1-updated");
        map2.put(1L, "m2-updated");

        // Then: 두 컬렉션 모두 원래 값 유지 (동일 스냅샷)
        assertEquals("m1-original", tx.get(map1, 1L));
        assertEquals("m2-original", tx.get(map2, 1L));

        tx.close();
    }

    // ==================== 스트레스 테스트 ====================

    @Test
    public void testHighConcurrency_stressTest() throws Exception {
        NavigableMap<Long, String> map = store.createMap("stressMap", Long.class, String.class);

        // 초기 데이터
        for (long i = 0; i < 1000; i++) {
            map.put(i, "value-" + i);
        }

        int numReaders = 16;
        int numOperationsPerReader = 100;
        CountDownLatch latch = new CountDownLatch(numReaders);
        AtomicInteger totalReads = new AtomicInteger(0);
        AtomicReference<Throwable> error = new AtomicReference<>(null);

        for (int i = 0; i < numReaders; i++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int j = 0; j < numOperationsPerReader; j++) {
                        try (FxReadTransaction tx = store.beginRead()) {
                            long key = random.nextInt(1000);
                            String value = tx.get(map, key);
                            assertNotNull(value);
                            totalReads.incrementAndGet();
                        }
                    }
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS));
        assertNull("Unexpected error: " + error.get(), error.get());
        assertEquals(numReaders * numOperationsPerReader, totalReads.get());
    }

    // ==================== 엣지 케이스 ====================

    @Test
    public void testEmptyCollection_inTransaction() {
        NavigableMap<Long, String> emptyMap = store.createMap("emptyMap", Long.class, String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.get(emptyMap, 1L));
            assertFalse(tx.containsKey(emptyMap, 1L));
            assertNull(tx.firstEntry(emptyMap));
            assertNull(tx.lastEntry(emptyMap));
            assertEquals(0, tx.size(emptyMap));
        }
    }

    @Test
    public void testRapidTransactionOpenClose() throws Exception {
        NavigableMap<Long, String> map = store.createMap("rapidMap", Long.class, String.class);
        map.put(1L, "value");

        int numIterations = 1000;
        for (int i = 0; i < numIterations; i++) {
            FxReadTransaction tx = store.beginRead();
            assertEquals("value", tx.get(map, 1L));
            tx.close();
        }
    }

    @Test
    public void testTransactionWithNestedLoops() {
        NavigableMap<Long, String> map = store.createMap("nestedMap", Long.class, String.class);
        for (long i = 0; i < 10; i++) {
            map.put(i, "value-" + i);
        }

        try (FxReadTransaction tx = store.beginRead()) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    String value = tx.get(map, (long) i);
                    assertEquals("value-" + i, value);
                }
            }
        }
    }

    // ==================== 성능 힌트 ====================

    @Test
    public void testTransactionOverhead_baseline() {
        // 트랜잭션 생성/종료 오버헤드 측정 (간단한 성능 확인)
        NavigableMap<Long, String> map = store.createMap("perfMap", Long.class, String.class);
        map.put(1L, "value");

        int warmup = 1000;
        int iterations = 10000;

        // Warmup
        for (int i = 0; i < warmup; i++) {
            try (FxReadTransaction tx = store.beginRead()) {
                tx.get(map, 1L);
            }
        }

        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (FxReadTransaction tx = store.beginRead()) {
                tx.get(map, 1L);
            }
        }
        long endTime = System.nanoTime();

        double nsPerOp = (double) (endTime - startTime) / iterations;
        System.out.printf("ReadTransaction overhead: %.2f ns/op%n", nsPerOp);

        // 기본적인 성능 검증 (1ms/op 미만)
        assertTrue("Transaction overhead too high: " + nsPerOp + " ns",
                   nsPerOp < 1_000_000);
    }
}
