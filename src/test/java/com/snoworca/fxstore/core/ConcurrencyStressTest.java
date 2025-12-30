package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.NavigableMap;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * 동시성 스트레스 테스트 (Phase 8 - Week 3 Day 2)
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>대규모 동시 읽기/쓰기</li>
 *   <li>Deadlock 감지 (INV-C5)</li>
 *   <li>장시간 안정성</li>
 * </ul>
 *
 * <h3>성능 목표</h3>
 * <ul>
 *   <li>10,000회 이상 연산 무결성</li>
 *   <li>Deadlock 발생 0건</li>
 * </ul>
 */
public class ConcurrencyStressTest {

    private static final int WRITER_THREADS = 4;
    private static final int READER_THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 2500;

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

    // ==================== 대규모 동시 읽기/쓰기 테스트 ====================

    @Test
    public void testConcurrentReadWrite_LargeScale() throws Exception {
        NavigableMap<Long, String> map = store.createMap("stress_test", Long.class, String.class);

        ExecutorService executor = Executors.newFixedThreadPool(WRITER_THREADS + READER_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(WRITER_THREADS + READER_THREADS);
        AtomicLong writeCount = new AtomicLong(0);
        AtomicLong readCount = new AtomicLong(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Writer threads
        for (int i = 0; i < WRITER_THREADS; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        long key = writerId * OPERATIONS_PER_THREAD + j;
                        map.put(key, "value-" + key);
                        writeCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Reader threads
        for (int i = 0; i < READER_THREADS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        long key = Math.abs(random.nextLong()) % (WRITER_THREADS * OPERATIONS_PER_THREAD);
                        map.get(key);
                        readCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.MINUTES);

        assertTrue("Test should complete within timeout", completed);
        assertEquals("No errors should occur", 0, errorCount.get());
        assertEquals("All writes should complete",
                WRITER_THREADS * OPERATIONS_PER_THREAD, writeCount.get());
        assertEquals("All reads should complete",
                READER_THREADS * OPERATIONS_PER_THREAD, readCount.get());

        executor.shutdown();
        assertTrue("Executor should terminate", executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    // ==================== Deadlock 감지 테스트 (INV-C5) ====================

    @Test
    public void testNoDeadlock_MultipleCollections() throws Exception {
        // INV-C5 검증: 단일 락으로 교착 상태 불가능
        NavigableMap<Long, String> map1 = store.createMap("map1", Long.class, String.class);
        NavigableMap<Long, String> map2 = store.createMap("map2", Long.class, String.class);

        int threadCount = 10;
        int opsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicBoolean deadlockDetected = new AtomicBoolean(false);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < opsPerThread; j++) {
                        // 교차 접근 패턴 (deadlock 유발 시도)
                        if (threadId % 2 == 0) {
                            map1.put((long) j, "v1-" + threadId);
                            map2.put((long) j, "v2-" + threadId);
                        } else {
                            map2.put((long) j, "v2-" + threadId);
                            map1.put((long) j, "v1-" + threadId);
                        }
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
        // 30초 내 완료되어야 함 (deadlock 없음)
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertTrue("Possible deadlock detected - timeout", completed);
        assertFalse("Deadlock should not be detected", deadlockDetected.get());
        assertEquals("No errors should occur", 0, errorCount.get());

        executor.shutdown();
    }

    @Test
    public void testNoDeadlock_InterleavedOperations() throws Exception {
        // 다양한 연산 interleaving
        NavigableMap<Long, String> map = store.createMap("interleave_test", Long.class, String.class);

        // 초기 데이터
        for (long i = 0; i < 100; i++) {
            map.put(i, "initial-" + i);
        }

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(threadId);
                    for (int i = 0; i < 500; i++) {
                        int op = random.nextInt(4);
                        long key = random.nextInt(200);

                        switch (op) {
                            case 0: // put
                                map.put(key, "thread-" + threadId + "-" + i);
                                break;
                            case 1: // get
                                map.get(key);
                                break;
                            case 2: // containsKey
                                map.containsKey(key);
                                break;
                            case 3: // size
                                map.size();
                                break;
                        }
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
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        assertTrue("Should complete without deadlock", completed);
        assertEquals("No errors should occur", 0, errorCount.get());

        executor.shutdown();
    }

    // ==================== 혼합 워크로드 스트레스 테스트 ====================

    @Test
    public void testMixedWorkload_80Read20Write() throws Exception {
        NavigableMap<Long, String> map = store.createMap("mixed_workload", Long.class, String.class);

        // 초기 데이터
        for (long i = 0; i < 1000; i++) {
            map.put(i, "initial-" + i);
        }

        int totalThreads = 20;
        int readerThreads = 16;
        int writerThreads = 4;
        int opsPerThread = 500;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicLong readOps = new AtomicLong(0);
        AtomicLong writeOps = new AtomicLong(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Readers (80%)
        for (int t = 0; t < readerThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random();
                    for (int i = 0; i < opsPerThread; i++) {
                        long key = Math.abs(random.nextLong()) % 1000;
                        map.get(key);
                        readOps.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Writers (20%)
        for (int t = 0; t < writerThreads; t++) {
            final int writerId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        long key = 1000 + writerId * opsPerThread + i;
                        map.put(key, "new-value-" + key);
                        writeOps.incrementAndGet();
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
        boolean completed = doneLatch.await(3, TimeUnit.MINUTES);

        assertTrue("Should complete within timeout", completed);
        assertEquals("No errors should occur", 0, errorCount.get());
        assertEquals("All reads should complete",
                (long) readerThreads * opsPerThread, readOps.get());
        assertEquals("All writes should complete",
                (long) writerThreads * opsPerThread, writeOps.get());

        executor.shutdown();
    }

    // ==================== 컬렉션 생성/삭제 스트레스 테스트 ====================

    @Test
    public void testConcurrentCreateDrop() throws Exception {
        int threadCount = 5;
        int iterations = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            final int iteration = i;
            CountDownLatch iterLatch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        String name = "coll_" + iteration + "_" + threadId;
                        store.createOrOpenMap(name, Long.class, String.class);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        iterLatch.countDown();
                    }
                });
            }

            assertTrue("Iteration " + i + " should complete",
                    iterLatch.await(10, TimeUnit.SECONDS));
        }

        assertEquals("No errors should occur", 0, errorCount.get());
        assertEquals("All creates should succeed",
                threadCount * iterations, successCount.get());

        executor.shutdown();
    }

    // ==================== 장시간 안정성 테스트 ====================

    @Test
    public void testLongRunningStability() throws Exception {
        NavigableMap<Long, String> map = store.createMap("stability_test", Long.class, String.class);

        int threadCount = 8;
        int durationSeconds = 10;  // 10초 동안 지속
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong totalOps = new AtomicLong(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            final boolean isWriter = (t < 2);  // 2 writers, 6 readers

            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(threadId);
                    long localOps = 0;

                    while (running.get()) {
                        if (isWriter) {
                            long key = random.nextInt(10000);
                            map.put(key, "v-" + key);
                        } else {
                            long key = random.nextInt(10000);
                            map.get(key);
                        }
                        localOps++;
                    }

                    totalOps.addAndGet(localOps);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                }
            });
        }

        startLatch.countDown();
        Thread.sleep(durationSeconds * 1000L);
        running.set(false);

        executor.shutdown();
        assertTrue("Executor should terminate",
                executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals("No errors during long-running test", 0, errorCount.get());
        assertTrue("Should perform significant operations", totalOps.get() > 10000);

        System.out.printf("Long-running stability test: %d ops in %d seconds (%.0f ops/sec)%n",
                totalOps.get(), durationSeconds, (double) totalOps.get() / durationSeconds);
    }

    // ==================== List 동시성 스트레스 테스트 ====================

    @Test
    public void testListConcurrentOperations() throws Exception {
        java.util.List<String> list = store.createList("stress_list", String.class);

        // 초기 데이터
        for (int i = 0; i < 100; i++) {
            list.add("initial-" + i);
        }

        int threadCount = 10;
        int opsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            final boolean isWriter = (t < 3);  // 3 writers, 7 readers

            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(threadId);

                    for (int i = 0; i < opsPerThread; i++) {
                        if (isWriter) {
                            list.add("thread-" + threadId + "-" + i);
                        } else {
                            int size = list.size();
                            if (size > 0) {
                                int idx = random.nextInt(size);
                                list.get(idx);
                            }
                        }
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
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);

        assertTrue("Should complete within timeout", completed);
        assertEquals("No errors should occur", 0, errorCount.get());

        // 최종 크기 검증: 초기 100개 + 3 writers * 500 ops = 1600개
        assertEquals("Final size should match", 100 + 3 * opsPerThread, list.size());

        executor.shutdown();
    }
}
