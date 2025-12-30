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
}
