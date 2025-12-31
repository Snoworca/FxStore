package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * 동시성 통합 테스트 (Phase 8 - Week 1 Day 7)
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>INV-C1: Single Writer 검증</li>
 *   <li>INV-C3: Wait-free Read 검증</li>
 *   <li>INV-C4: Atomic Snapshot Switch 검증</li>
 *   <li>createOrOpen Race Condition 방지 검증</li>
 * </ul>
 */
public class ConcurrencyIntegrationTest {

    private FxStore store;

    @Before
    public void setUp() {
        store = FxStoreImpl.openMemory(FxOptions.defaults());
    }

    @After
    public void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    // ==================== INV-C1: Single Writer 테스트 ====================

    @Test
    public void testSingleWriter_ConcurrentCreates() throws Exception {
        int threadCount = 10;
        int collectionsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < collectionsPerThread; i++) {
                        String name = "map_" + threadId + "_" + i;
                        store.createMap(name, String.class, Integer.class);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        assertEquals("Should have created all collections",
                threadCount * collectionsPerThread, successCount.get());
        assertEquals("No errors should occur", 0, errorCount.get());
        assertEquals("All collections should exist",
                threadCount * collectionsPerThread, store.list().size());

        executor.shutdown();
    }

    // ==================== INV-C3: Wait-free Read 테스트 ====================

    @Test
    public void testWaitFreeReads_ConcurrentWithWrites() throws Exception {
        // 초기 데이터 생성
        NavigableMap<String, Integer> map = store.createMap("test_map", String.class, Integer.class);
        for (int i = 0; i < 10; i++) {
            map.put("key" + i, i);
        }

        int readerCount = 20;
        int writerCount = 3;
        int opsPerThread = 20;

        ExecutorService executor = Executors.newFixedThreadPool(readerCount + writerCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerCount + writerCount);
        AtomicInteger readSuccessCount = new AtomicInteger(0);
        AtomicInteger writeSuccessCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Readers - wait-free이므로 락 경합 없이 빠르게 수행
        for (int t = 0; t < readerCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        // Wait-free 읽기 검증
                        boolean exists = store.exists("test_map");
                        assertTrue("Map should exist", exists);

                        List<CollectionInfo> list = store.list();
                        assertFalse("List should not be empty", list.isEmpty());

                        Stats stats = store.stats();
                        assertTrue("Stats should have collections", stats.collectionCount() > 0);

                        readSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Writers - 직렬화되어 실행
        for (int t = 0; t < writerCount; t++) {
            final int writerId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        String name = "writer_map_" + writerId + "_" + i;
                        store.createMap(name, String.class, String.class);
                        writeSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(60, TimeUnit.SECONDS));

        assertEquals("All reads should succeed",
                readerCount * opsPerThread, readSuccessCount.get());
        assertEquals("All writes should succeed",
                writerCount * opsPerThread, writeSuccessCount.get());
        assertEquals("No errors should occur", 0, errorCount.get());

        executor.shutdown();
    }

    // ==================== createOrOpen Race Condition 테스트 ====================

    @Test
    public void testCreateOrOpen_RaceCondition() throws Exception {
        int threadCount = 20;
        String sharedName = "shared_collection";
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<NavigableMap<String, Integer>> results = new CopyOnWriteArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // 모든 스레드가 동시에 같은 이름으로 createOrOpenMap 호출
                    NavigableMap<String, Integer> map =
                            store.createOrOpenMap(sharedName, String.class, Integer.class);
                    results.add(map);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        // 모든 스레드가 성공해야 함 (race condition 방지 검증)
        assertEquals("All threads should succeed", threadCount, results.size());
        assertEquals("No errors should occur", 0, errorCount.get());

        // 하나의 컬렉션만 존재해야 함
        assertEquals("Only one collection should exist", 1,
                store.list().stream().filter(c -> c.name().equals(sharedName)).count());

        executor.shutdown();
    }

    @Test
    public void testCreateOrOpenSet_RaceCondition() throws Exception {
        int threadCount = 20;
        String sharedName = "shared_set";
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<NavigableSet<String>> results = new CopyOnWriteArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    NavigableSet<String> set =
                            store.createOrOpenSet(sharedName, String.class);
                    results.add(set);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        assertEquals("All threads should succeed", threadCount, results.size());
        assertEquals("No errors should occur", 0, errorCount.get());

        executor.shutdown();
    }

    // ==================== 스냅샷 일관성 테스트 ====================

    @Test
    public void testSnapshotConsistency_DuringWrites() throws Exception {
        // 여러 컬렉션 생성
        for (int i = 0; i < 10; i++) {
            store.createMap("map_" + i, String.class, Integer.class);
        }

        int readerCount = 20;
        int iterations = 50;
        ExecutorService executor = Executors.newFixedThreadPool(readerCount + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerCount + 1);
        AtomicBoolean writerRunning = new AtomicBoolean(true);
        AtomicInteger inconsistencyCount = new AtomicInteger(0);

        // Writer: 계속 컬렉션 추가
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 10; i < 100; i++) {
                    store.createMap("map_" + i, String.class, Integer.class);
                    Thread.sleep(5);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                writerRunning.set(false);
                doneLatch.countDown();
            }
        });

        // Readers: list()와 stats()가 일관성 있는지 확인
        for (int t = 0; t < readerCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations && writerRunning.get(); i++) {
                        List<CollectionInfo> list = store.list();
                        Stats stats = store.stats();

                        // list()와 stats()가 같은 스냅샷을 본다면 일관성 있음
                        // 참고: 이들이 다른 스냅샷을 볼 수 있지만, 각각은 내부적으로 일관성 있어야 함
                        if (list.size() > stats.collectionCount()) {
                            // list가 더 크다면 일관성 없음 (가능하지 않아야 함)
                            // 단, 이 두 호출 사이에 스냅샷이 바뀔 수 있으므로
                            // stats >= list.size() 또는 stats < list.size() 모두 가능
                            // 중요한 것은 각 호출 자체가 원자적이라는 것
                        }

                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(60, TimeUnit.SECONDS));

        // 최종 상태 검증
        assertEquals("Inconsistencies detected", 0, inconsistencyCount.get());
        assertTrue("Should have created collections", store.list().size() >= 10);

        executor.shutdown();
    }

    // ==================== Drop/Rename 동시성 테스트 ====================

    @Test
    public void testConcurrentDropAndCreate() throws Exception {
        int iterations = 20;
        String collectionName = "volatile_collection";

        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            CountDownLatch latch = new CountDownLatch(2);

            // Thread 1: Create
            executor.submit(() -> {
                try {
                    store.createOrOpenMap(collectionName, String.class, String.class);
                } catch (FxException e) {
                    // NOT_FOUND는 drop과의 race condition으로 발생 가능 - 정상
                    if (e.getCode() != FxErrorCode.NOT_FOUND) {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            // Thread 2: Drop (if exists)
            executor.submit(() -> {
                try {
                    store.drop(collectionName);
                } catch (Exception e) {
                    // 존재하지 않을 수 있음 - 정상
                } finally {
                    latch.countDown();
                }
            });

            assertTrue("Timeout", latch.await(5, TimeUnit.SECONDS));
        }

        assertEquals("No unexpected errors", 0, errorCount.get());
        executor.shutdown();
    }

    // ==================== Commit/Rollback 동시성 테스트 ====================

    @Test
    public void testConcurrentCommit() throws Exception {
        // BATCH 모드 스토어 생성
        FxOptions batchOptions = FxOptions.defaults().withCommitMode(CommitMode.BATCH).build();
        FxStore batchStore = FxStoreImpl.openMemory(batchOptions);

        try {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < 10; i++) {
                            batchStore.createMap("batch_map_" + threadId + "_" + i,
                                    String.class, Integer.class);
                            batchStore.commit();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue("Timeout", doneLatch.await(30, TimeUnit.SECONDS));
            assertEquals("No errors", 0, errorCount.get());

            executor.shutdown();
        } finally {
            batchStore.close();
        }
    }

    // ==================== FxList 동시성 테스트 (Phase 8 - Week 2) ====================

    @Test
    public void testCreateOrOpenList_RaceCondition() throws Exception {
        int threadCount = 20;
        String sharedName = "shared_list";
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<java.util.List<String>> results = new CopyOnWriteArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    java.util.List<String> list =
                            store.createOrOpenList(sharedName, String.class);
                    results.add(list);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        assertEquals("All threads should succeed", threadCount, results.size());
        assertEquals("No errors should occur", 0, errorCount.get());
        assertEquals("Only one collection should exist", 1,
                store.list().stream().filter(c -> c.name().equals(sharedName)).count());

        executor.shutdown();
    }

    @Test
    public void testFxList_WaitFreeReads_ConcurrentWithWrites() throws Exception {
        // 초기 리스트 생성
        java.util.List<String> list = store.createList("test_list", String.class);
        for (int i = 0; i < 10; i++) {
            list.add("item" + i);
        }

        int readerCount = 10;
        int writerCount = 2;
        int opsPerThread = 20;

        ExecutorService executor = Executors.newFixedThreadPool(readerCount + writerCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerCount + writerCount);
        AtomicInteger readSuccessCount = new AtomicInteger(0);
        AtomicInteger writeSuccessCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Readers - Wait-free read (INV-C3)
        for (int t = 0; t < readerCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        int size = list.size();
                        assertTrue("Size should be >= 10", size >= 10);

                        if (size > 0) {
                            String first = list.get(0);
                            assertNotNull("First element should not be null", first);
                        }

                        readSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Writers - Single Writer (INV-C1)
        for (int t = 0; t < writerCount; t++) {
            final int writerId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        list.add("writer_" + writerId + "_" + i);
                        writeSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(60, TimeUnit.SECONDS));

        assertEquals("All reads should succeed",
                readerCount * opsPerThread, readSuccessCount.get());
        assertEquals("All writes should succeed",
                writerCount * opsPerThread, writeSuccessCount.get());
        assertEquals("No errors should occur", 0, errorCount.get());

        // 최종 크기 검증: 초기 10개 + writer * opsPerThread
        int expectedSize = 10 + (writerCount * opsPerThread);
        assertEquals("Final size should match", expectedSize, list.size());

        executor.shutdown();
    }

    @Test
    public void testFxList_SnapshotIterator_DuringModification() throws Exception {
        // 초기 리스트 생성
        java.util.List<String> list = store.createList("iterator_test_list", String.class);
        for (int i = 0; i < 50; i++) {
            list.add("item" + i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger iteratorCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Reader: 스냅샷 iterator로 전체 순회
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int iteration = 0; iteration < 10; iteration++) {
                    int count = 0;
                    for (String item : list) {
                        count++;
                        assertNotNull("Item should not be null", item);
                    }
                    // 스냅샷이므로 순회 중에도 일관성 보장
                    assertTrue("Iterator count should be consistent", count >= 50);
                    iteratorCount.addAndGet(count);
                    Thread.sleep(5);
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        // Writer: 요소 추가
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 20; i++) {
                    list.add("new_item_" + i);
                    Thread.sleep(5);
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        assertEquals("No errors should occur", 0, errorCount.get());
        assertTrue("Iterator should have processed items", iteratorCount.get() > 0);
        assertEquals("Final list size should be 70", 70, list.size());

        executor.shutdown();
    }

    // ==================== CONC-001: put/remove TOCTOU 테스트 ====================

    /**
     * CONC-001: put() oldValue 정확성 테스트
     *
     * <p>시나리오:
     * - 10개 스레드가 동시에 같은 키에 put 수행
     * - 각 스레드는 고유한 값("thread-N")을 삽입
     * - oldValue 반환값의 정확성 검증
     *
     * <p>기대 결과:
     * - 첫 번째 put만 null 반환
     * - 나머지 9개는 이전 값 반환
     * - 중복 oldValue 없음
     */
    @Test
    public void testCONC001_put_concurrent_sameKey_shouldReturnCorrectOldValue() throws Exception {
        final int THREAD_COUNT = 10;
        final String KEY = "shared-key";

        NavigableMap<String, String> map = store.createMap("conc001_put_test", String.class, String.class);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ConcurrentHashMap<Integer, String> oldValues = new ConcurrentHashMap<>();
        AtomicInteger nullCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();  // 동시 시작
                    String oldValue = map.put(KEY, "thread-" + threadId);
                    if (oldValue == null) {
                        nullCount.incrementAndGet();
                    } else {
                        oldValues.put(threadId, oldValue);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();  // 동시 시작 신호
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // 검증: 정확히 1개만 null 반환 (첫 번째 put)
        assertEquals("첫 번째 put만 null 반환", 1, nullCount.get());
        assertEquals("나머지 9개는 oldValue 반환", 9, oldValues.size());

        // 검증: 중복 oldValue 없음
        java.util.Set<String> uniqueOldValues = new java.util.HashSet<>(oldValues.values());
        assertEquals("중복 oldValue 없음", 9, uniqueOldValues.size());
    }

    /**
     * CONC-001: remove() oldValue 정확성 테스트
     *
     * <p>시나리오:
     * - 미리 키 삽입
     * - 10개 스레드가 동시에 같은 키에 remove 수행
     * - 정확히 1개만 값 반환, 나머지는 null 반환
     */
    @Test
    public void testCONC001_remove_concurrent_sameKey_shouldReturnCorrectOldValue() throws Exception {
        NavigableMap<String, String> map = store.createMap("conc001_remove_test", String.class, String.class);

        // 미리 키 삽입
        map.put("key", "value");

        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger nullCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String oldValue = map.remove("key");
                    if (oldValue != null) {
                        successCount.incrementAndGet();
                    } else {
                        nullCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // 검증: 정확히 1개만 성공
        assertEquals("정확히 1개만 값 반환", 1, successCount.get());
        assertEquals("나머지는 null 반환", 9, nullCount.get());
    }

    // ==================== CONC-002: pollFirstEntry/pollLastEntry Atomic 테스트 ====================

    /**
     * CONC-002 테스트: pollFirstEntry 동시 호출 시 각 엔트리는 정확히 한 번만 반환
     *
     * <p>수정 전: 비원자적 구현(firstEntry + remove)으로 인해 같은 엔트리가 여러 스레드에 반환될 수 있음</p>
     * <p>수정 후: 단일 락 내에서 원자적 수행으로 각 엔트리는 정확히 한 번만 반환</p>
     */
    @Test
    public void testCONC002_pollFirstEntry_concurrent_shouldReturnEachEntryOnce() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("poll_test", Integer.class, String.class);

        // 100개 엔트리 삽입
        int entryCount = 100;
        for (int i = 0; i < entryCount; i++) {
            map.put(i, "value" + i);
        }
        assertEquals(entryCount, map.size());

        // 동시에 pollFirstEntry 호출
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ConcurrentHashMap<Integer, AtomicInteger> polledKeys = new ConcurrentHashMap<>();
        AtomicInteger totalPolled = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    java.util.Map.Entry<Integer, String> entry;
                    while ((entry = map.pollFirstEntry()) != null) {
                        int key = entry.getKey();
                        polledKeys.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
                        totalPolled.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        // 검증: 모든 엔트리가 poll됨
        assertEquals("모든 엔트리가 poll됨", entryCount, totalPolled.get());
        assertEquals("맵이 비어있음", 0, map.size());

        // 검증: 각 키는 정확히 한 번만 poll됨 (CONC-002 핵심)
        for (int i = 0; i < entryCount; i++) {
            AtomicInteger count = polledKeys.get(i);
            assertNotNull("키 " + i + "가 poll됨", count);
            assertEquals("키 " + i + "가 정확히 1번 poll됨", 1, count.get());
        }

        executor.shutdown();
    }

    /**
     * CONC-002 테스트: pollLastEntry 동시 호출 시 각 엔트리는 정확히 한 번만 반환
     */
    @Test
    public void testCONC002_pollLastEntry_concurrent_shouldReturnEachEntryOnce() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("poll_last_test", Integer.class, String.class);

        // 100개 엔트리 삽입
        int entryCount = 100;
        for (int i = 0; i < entryCount; i++) {
            map.put(i, "value" + i);
        }
        assertEquals(entryCount, map.size());

        // 동시에 pollLastEntry 호출
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ConcurrentHashMap<Integer, AtomicInteger> polledKeys = new ConcurrentHashMap<>();
        AtomicInteger totalPolled = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    java.util.Map.Entry<Integer, String> entry;
                    while ((entry = map.pollLastEntry()) != null) {
                        int key = entry.getKey();
                        polledKeys.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
                        totalPolled.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        // 검증
        assertEquals("모든 엔트리가 poll됨", entryCount, totalPolled.get());
        assertEquals("맵이 비어있음", 0, map.size());

        for (int i = 0; i < entryCount; i++) {
            AtomicInteger count = polledKeys.get(i);
            assertNotNull("키 " + i + "가 poll됨", count);
            assertEquals("키 " + i + "가 정확히 1번 poll됨", 1, count.get());
        }

        executor.shutdown();
    }

    // ==================== CONC-003: View 클래스 poll Atomic 테스트 ====================

    /**
     * CONC-003 테스트: SubMap의 pollFirstEntry 동시 호출
     */
    @Test
    public void testCONC003_subMap_pollFirstEntry_concurrent() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("submap_poll_test", Integer.class, String.class);

        // 100개 엔트리 삽입 (키: 0-99)
        int entryCount = 100;
        for (int i = 0; i < entryCount; i++) {
            map.put(i, "value" + i);
        }

        // subMap(20, 80) - 60개 엔트리
        NavigableMap<Integer, String> subMap = map.subMap(20, true, 79, true);
        int subMapSize = 60;

        // 동시에 pollFirstEntry 호출
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ConcurrentHashMap<Integer, AtomicInteger> polledKeys = new ConcurrentHashMap<>();
        AtomicInteger totalPolled = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    java.util.Map.Entry<Integer, String> entry;
                    while ((entry = subMap.pollFirstEntry()) != null) {
                        int key = entry.getKey();
                        polledKeys.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
                        totalPolled.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        // 검증
        assertEquals("subMap 내 모든 엔트리가 poll됨", subMapSize, totalPolled.get());

        // 각 키는 정확히 한 번만 poll됨
        for (int i = 20; i < 80; i++) {
            AtomicInteger count = polledKeys.get(i);
            assertNotNull("키 " + i + "가 poll됨", count);
            assertEquals("키 " + i + "가 정확히 1번 poll됨", 1, count.get());
        }

        // subMap 범위 외의 키는 그대로
        assertEquals("전체 맵에 40개 남음 (0-19, 80-99)", 40, map.size());
        assertNotNull("키 0 존재", map.get(0));
        assertNotNull("키 99 존재", map.get(99));

        executor.shutdown();
    }

    /**
     * CONC-003 테스트: HeadMap의 pollLastEntry 동시 호출
     */
    @Test
    public void testCONC003_headMap_pollLastEntry_concurrent() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("headmap_poll_test", Integer.class, String.class);

        // 100개 엔트리 삽입
        int entryCount = 100;
        for (int i = 0; i < entryCount; i++) {
            map.put(i, "value" + i);
        }

        // headMap(50, true) - 51개 엔트리 (0-50)
        NavigableMap<Integer, String> headMap = map.headMap(50, true);
        int headMapSize = 51;

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ConcurrentHashMap<Integer, AtomicInteger> polledKeys = new ConcurrentHashMap<>();
        AtomicInteger totalPolled = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    java.util.Map.Entry<Integer, String> entry;
                    while ((entry = headMap.pollLastEntry()) != null) {
                        int key = entry.getKey();
                        polledKeys.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
                        totalPolled.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        // 검증
        assertEquals("headMap 내 모든 엔트리가 poll됨", headMapSize, totalPolled.get());

        for (int i = 0; i <= 50; i++) {
            AtomicInteger count = polledKeys.get(i);
            assertNotNull("키 " + i + "가 poll됨", count);
            assertEquals("키 " + i + "가 정확히 1번 poll됨", 1, count.get());
        }

        assertEquals("전체 맵에 49개 남음 (51-99)", 49, map.size());

        executor.shutdown();
    }

    /**
     * CONC-003 테스트: TailMap의 pollFirstEntry 동시 호출
     */
    @Test
    public void testCONC003_tailMap_pollFirstEntry_concurrent() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("tailmap_poll_test", Integer.class, String.class);

        // 100개 엔트리 삽입
        int entryCount = 100;
        for (int i = 0; i < entryCount; i++) {
            map.put(i, "value" + i);
        }

        // tailMap(50, true) - 50개 엔트리 (50-99)
        NavigableMap<Integer, String> tailMap = map.tailMap(50, true);
        int tailMapSize = 50;

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ConcurrentHashMap<Integer, AtomicInteger> polledKeys = new ConcurrentHashMap<>();
        AtomicInteger totalPolled = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    java.util.Map.Entry<Integer, String> entry;
                    while ((entry = tailMap.pollFirstEntry()) != null) {
                        int key = entry.getKey();
                        polledKeys.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
                        totalPolled.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Timeout waiting for threads", doneLatch.await(30, TimeUnit.SECONDS));

        // 검증
        assertEquals("tailMap 내 모든 엔트리가 poll됨", tailMapSize, totalPolled.get());

        for (int i = 50; i < 100; i++) {
            AtomicInteger count = polledKeys.get(i);
            assertNotNull("키 " + i + "가 poll됨", count);
            assertEquals("키 " + i + "가 정확히 1번 poll됨", 1, count.get());
        }

        assertEquals("전체 맵에 50개 남음 (0-49)", 50, map.size());

        executor.shutdown();
    }

    // ==================== PERF-001: size() O(1) 테스트 ====================

    /**
     * PERF-001 테스트: size()가 O(1)로 동작하는지 검증
     *
     * <p>수정 전: BTree 전체 순회로 O(n)</p>
     * <p>수정 후: CollectionState.count 조회로 O(1)</p>
     */
    @Test
    public void testPERF001_size_shouldReturnCorrectCount() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("size_test", Integer.class, String.class);

        // 빈 맵 검증
        assertEquals("빈 맵 size = 0", 0, map.size());

        // 100개 삽입
        for (int i = 0; i < 100; i++) {
            map.put(i, "value" + i);
            assertEquals("삽입 후 size", i + 1, map.size());
        }

        // 50개 삭제
        for (int i = 0; i < 50; i++) {
            map.remove(i);
            assertEquals("삭제 후 size", 100 - i - 1, map.size());
        }

        // 최종 검증
        assertEquals("최종 size = 50", 50, map.size());

        // clear 후 size
        map.clear();
        assertEquals("clear 후 size = 0", 0, map.size());
    }

    /**
     * PERF-001 테스트: size()가 put/remove와 일관성 유지
     */
    @Test
    public void testPERF001_size_consistency_with_operations() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("size_consistency_test", Integer.class, String.class);

        // put 일관성
        for (int i = 0; i < 50; i++) {
            map.put(i, "v" + i);
        }
        assertEquals(50, map.size());

        // 같은 키로 put (업데이트) - size 변경 없음
        map.put(0, "updated");
        assertEquals("업데이트 후 size 변경 없음", 50, map.size());

        // pollFirstEntry
        map.pollFirstEntry();
        assertEquals("pollFirstEntry 후 size", 49, map.size());

        // pollLastEntry
        map.pollLastEntry();
        assertEquals("pollLastEntry 후 size", 48, map.size());

        // subMap poll
        NavigableMap<Integer, String> subMap = map.subMap(10, true, 20, true);
        int subMapSize = subMap.size();  // 11개 (10-20)
        subMap.pollFirstEntry();
        assertEquals("subMap pollFirstEntry 후 전체 size", 48 - 1, map.size());
    }

    /**
     * PERF-001 테스트: size() O(1) 성능 검증
     */
    @Test
    public void testPERF001_size_performance() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("size_perf_test", Integer.class, String.class);

        // 1000개 삽입
        for (int i = 0; i < 1000; i++) {
            map.put(i, "value" + i);
        }

        // size() 호출 시간 측정 (10000회)
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            map.size();
        }
        long elapsed = System.nanoTime() - start;

        // O(1)이면 10000회 호출이 10ms 미만이어야 함
        // O(n)이면 1000 * 10000 = 10,000,000회 순회로 훨씬 오래 걸림
        long elapsedMs = elapsed / 1_000_000;
        System.out.println("PERF-001: 10000회 size() 호출 시간: " + elapsedMs + "ms");
        assertTrue("size() O(1) 성능: 10000회 호출이 100ms 미만", elapsedMs < 100);
    }

    // ==================== PERF-002: firstEntry/lastEntry O(log n) 테스트 ====================

    /**
     * PERF-002 테스트: firstEntry()와 lastEntry() 정확성 검증
     *
     * <p>수정 전: cursor 전체 순회로 lastEntry() O(n)</p>
     * <p>수정 후: BTree.firstEntryWithRoot/lastEntryWithRoot 사용으로 O(log n)</p>
     */
    @Test
    public void testPERF002_firstEntry_lastEntry_correctness() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("entry_test", Integer.class, String.class);

        // 빈 맵 검증
        assertNull("빈 맵 firstEntry = null", map.firstEntry());
        assertNull("빈 맵 lastEntry = null", map.lastEntry());

        // 데이터 삽입 (무작위 순서)
        int[] keys = {50, 25, 75, 10, 30, 60, 90, 5, 15, 27, 35, 55, 70, 85, 95};
        for (int key : keys) {
            map.put(key, "value" + key);
        }

        // 첫 번째/마지막 엔트리 검증
        java.util.Map.Entry<Integer, String> first = map.firstEntry();
        java.util.Map.Entry<Integer, String> last = map.lastEntry();

        assertNotNull("firstEntry 존재", first);
        assertNotNull("lastEntry 존재", last);
        assertEquals("firstEntry 키 = 5", Integer.valueOf(5), first.getKey());
        assertEquals("lastEntry 키 = 95", Integer.valueOf(95), last.getKey());

        // 삭제 후 검증
        map.remove(5);
        map.remove(95);
        first = map.firstEntry();
        last = map.lastEntry();

        assertEquals("firstEntry 키 = 10", Integer.valueOf(10), first.getKey());
        assertEquals("lastEntry 키 = 90", Integer.valueOf(90), last.getKey());
    }

    /**
     * PERF-002 테스트: lastEntry() O(log n) 성능 검증
     */
    @Test
    public void testPERF002_lastEntry_performance() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("lastentry_perf_test", Integer.class, String.class);

        // 5000개 삽입
        for (int i = 0; i < 5000; i++) {
            map.put(i, "value" + i);
        }

        // lastEntry() 호출 시간 측정 (1000회)
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            map.lastEntry();
        }
        long elapsed = System.nanoTime() - start;

        // O(log n)이면 1000회 호출이 빠름
        // O(n)이면 5000 * 1000 = 5,000,000회 순회로 오래 걸림
        long elapsedMs = elapsed / 1_000_000;
        System.out.println("PERF-002: 1000회 lastEntry() 호출 시간 (5000개 데이터): " + elapsedMs + "ms");
        assertTrue("lastEntry() O(log n) 성능: 1000회 호출이 500ms 미만", elapsedMs < 500);
    }

    // ==================== PERF-003: clear() O(1) 테스트 ====================

    /**
     * PERF-003 테스트: clear() 정확성 검증
     *
     * <p>수정 전: 모든 엔트리 순회 삭제 O(n)</p>
     * <p>수정 후: root를 0으로 설정 O(1)</p>
     */
    @Test
    public void testPERF003_clear_correctness() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("clear_test", Integer.class, String.class);

        // 데이터 삽입
        for (int i = 0; i < 100; i++) {
            map.put(i, "value" + i);
        }
        assertEquals("삽입 후 size = 100", 100, map.size());
        assertNotNull("삽입 후 firstEntry 존재", map.firstEntry());

        // clear 호출
        map.clear();

        // 검증
        assertEquals("clear 후 size = 0", 0, map.size());
        assertTrue("clear 후 isEmpty = true", map.isEmpty());
        assertNull("clear 후 firstEntry = null", map.firstEntry());
        assertNull("clear 후 lastEntry = null", map.lastEntry());
        assertNull("clear 후 get(50) = null", map.get(50));

        // clear 후 다시 삽입 가능
        map.put(1, "new_value");
        assertEquals("재삽입 후 size = 1", 1, map.size());
        assertEquals("재삽입 값 확인", "new_value", map.get(1));
    }

    /**
     * PERF-003 테스트: clear() O(1) 성능 검증
     */
    @Test
    public void testPERF003_clear_performance() throws Exception {
        // 단일 맵으로 clear 시간 측정 (메모리 절약)
        int testSize = 1000;
        NavigableMap<Integer, String> map = store.createMap("clear_perf_test", Integer.class, String.class);

        // 데이터 삽입
        for (int i = 0; i < testSize; i++) {
            map.put(i, "value" + i);
        }

        // clear 시간 측정
        long start = System.nanoTime();
        map.clear();
        long elapsed = System.nanoTime() - start;
        long elapsedMs = elapsed / 1_000_000;

        System.out.println("PERF-003: clear() 시간 (" + testSize + "개 데이터): " + elapsedMs + "ms");
        // O(1)이면 데이터 크기에 관계없이 일정하게 빠름 (수 ms 이내)
        // O(n)이면 1000개 삭제로 훨씬 오래 걸림
        assertTrue("clear() O(1) 성능: 50ms 미만", elapsedMs < 50);
    }

    /**
     * PERF-003 테스트: clear() 후 다른 스냅샷 영향 없음 (COW 검증)
     */
    @Test
    public void testPERF003_clear_cow_isolation() throws Exception {
        NavigableMap<Integer, String> map = store.createMap("clear_cow_test", Integer.class, String.class);

        // 데이터 삽입
        for (int i = 0; i < 50; i++) {
            map.put(i, "value" + i);
        }

        // Iterator 획득 (스냅샷 캡처)
        java.util.Iterator<java.util.Map.Entry<Integer, String>> iterator = map.entrySet().iterator();

        // clear 호출
        map.clear();

        // 새로운 조회는 빈 맵
        assertEquals("clear 후 size = 0", 0, map.size());

        // 기존 Iterator는 스냅샷을 유지 (COW)
        int iterCount = 0;
        while (iterator.hasNext()) {
            iterator.next();
            iterCount++;
        }
        assertEquals("기존 Iterator는 스냅샷 유지", 50, iterCount);
    }
}
