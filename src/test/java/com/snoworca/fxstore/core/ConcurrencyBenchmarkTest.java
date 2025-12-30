package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.NavigableMap;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * 동시성 벤치마크 테스트 (Phase 8 - Week 3 Day 4)
 *
 * <h3>성능 목표</h3>
 * <ul>
 *   <li>단일 스레드 Write: >= 50,000 ops/sec</li>
 *   <li>단일 스레드 Read: >= 100,000 ops/sec</li>
 *   <li>동시 Read 확장성: 스레드 수에 근접한 선형 확장</li>
 *   <li>혼합 워크로드 (80R/20W): >= 80,000 total ops/sec</li>
 * </ul>
 */
public class ConcurrencyBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 500;
    private static final int MEASURE_ITERATIONS = 10000;

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

    // ==================== 단일 스레드 벤치마크 ====================

    @Test
    public void benchmarkSingleThreadBaseline() {
        NavigableMap<Long, String> map = store.createMap("bench_single", Long.class, String.class);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            map.put((long) i, "warmup-" + i);
        }

        // Measure writes
        long writeStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            map.put((long) i, "value-" + i);
        }
        long writeTime = System.nanoTime() - writeStart;

        // Measure reads
        long readStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            map.get((long) (i % WARMUP_ITERATIONS));
        }
        long readTime = System.nanoTime() - readStart;

        double writeOpsPerSec = MEASURE_ITERATIONS / (writeTime / 1_000_000_000.0);
        double readOpsPerSec = MEASURE_ITERATIONS / (readTime / 1_000_000_000.0);

        System.out.println("=== Single-thread Baseline Benchmark ===");
        System.out.printf("Write: %.2f ops/sec (target: >= 50,000)%n", writeOpsPerSec);
        System.out.printf("Read:  %.2f ops/sec (target: >= 100,000)%n", readOpsPerSec);

        // 성능 목표 검증 (환경에 따라 달라질 수 있으므로 경고만)
        if (writeOpsPerSec < 50000) {
            System.out.println("WARNING: Write performance below target");
        }
        if (readOpsPerSec < 100000) {
            System.out.println("WARNING: Read performance below target");
        }
    }

    // ==================== 동시 읽기 확장성 벤치마크 ====================

    @Test
    public void benchmarkConcurrentReads_Scalability() throws Exception {
        NavigableMap<Long, String> map = store.createMap("bench_read_scale", Long.class, String.class);

        // 데이터 준비
        for (int i = 0; i < 10000; i++) {
            map.put((long) i, "value-" + i);
        }

        int[] threadCounts = {1, 2, 4, 8, 16};
        double[] opsPerSecResults = new double[threadCounts.length];

        System.out.println("\n=== Concurrent Read Scalability Benchmark ===");

        for (int idx = 0; idx < threadCounts.length; idx++) {
            int threadCount = threadCounts[idx];
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicLong totalOps = new AtomicLong(0);

            int opsPerThread = MEASURE_ITERATIONS / threadCount;

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Random random = new Random();
                        for (int i = 0; i < opsPerThread; i++) {
                            map.get((long) random.nextInt(10000));
                            totalOps.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            long start = System.nanoTime();
            startLatch.countDown();
            doneLatch.await();
            long elapsed = System.nanoTime() - start;

            double opsPerSec = totalOps.get() / (elapsed / 1_000_000_000.0);
            opsPerSecResults[idx] = opsPerSec;
            System.out.printf("%2d threads: %,.0f ops/sec%n", threadCount, opsPerSec);

            executor.shutdown();
        }

        // 확장성 분석
        System.out.println("\nScalability Analysis:");
        for (int i = 1; i < threadCounts.length; i++) {
            double scaleFactor = opsPerSecResults[i] / opsPerSecResults[0];
            double idealFactor = (double) threadCounts[i] / threadCounts[0];
            double efficiency = scaleFactor / idealFactor * 100;
            System.out.printf("%d->%d threads: %.1fx speedup (%.0f%% efficiency)%n",
                    threadCounts[0], threadCounts[i], scaleFactor, efficiency);
        }
    }

    // ==================== 혼합 워크로드 벤치마크 ====================

    @Test
    public void benchmarkReadWriteMix() throws Exception {
        NavigableMap<Long, String> map = store.createMap("bench_mixed", Long.class, String.class);

        // 초기 데이터
        for (int i = 0; i < 10000; i++) {
            map.put((long) i, "initial-" + i);
        }

        int totalThreads = 10;
        int readerThreads = 8;  // 80%
        int writerThreads = 2;  // 20%
        int opsPerThread = 10000;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicLong readOps = new AtomicLong(0);
        AtomicLong writeOps = new AtomicLong(0);

        // Readers
        for (int t = 0; t < readerThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random();
                    for (int i = 0; i < opsPerThread; i++) {
                        map.get((long) random.nextInt(10000));
                        readOps.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Writers
        for (int t = 0; t < writerThreads; t++) {
            final int writerId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        long key = 10000 + writerId * opsPerThread + i;
                        map.put(key, "new-" + key);
                        writeOps.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startLatch.countDown();
        doneLatch.await();
        long elapsed = System.nanoTime() - start;

        long totalOps = readOps.get() + writeOps.get();
        double opsPerSec = totalOps / (elapsed / 1_000_000_000.0);

        System.out.println("\n=== Mixed Workload Benchmark (80% Read, 20% Write) ===");
        System.out.printf("Total:  %,.0f ops/sec (target: >= 80,000)%n", opsPerSec);
        System.out.printf("Reads:  %d (%,.0f ops/sec)%n", readOps.get(),
                readOps.get() / (elapsed / 1_000_000_000.0));
        System.out.printf("Writes: %d (%,.0f ops/sec)%n", writeOps.get(),
                writeOps.get() / (elapsed / 1_000_000_000.0));

        if (opsPerSec < 80000) {
            System.out.println("WARNING: Mixed workload performance below target");
        }

        executor.shutdown();
    }

    // ==================== 쓰기 경합 벤치마크 ====================

    @Test
    public void benchmarkWriteContention() throws Exception {
        int[] writerCounts = {1, 2, 4};
        int opsPerWriter = 1000;

        System.out.println("\n=== Write Contention Benchmark ===");

        for (int idx = 0; idx < writerCounts.length; idx++) {
            int writerCount = writerCounts[idx];
            // 각 반복마다 새 map 생성 (final 변수로 캡처 가능)
            final NavigableMap<Long, String> currentMap =
                store.createMap("bench_write_contention_" + idx, Long.class, String.class);

            ExecutorService executor = Executors.newFixedThreadPool(writerCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(writerCount);
            AtomicLong totalOps = new AtomicLong(0);

            for (int t = 0; t < writerCount; t++) {
                final int writerId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < opsPerWriter; i++) {
                            long key = writerId * opsPerWriter + i;
                            currentMap.put(key, "v-" + key);
                            totalOps.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            long start = System.nanoTime();
            startLatch.countDown();
            doneLatch.await();
            long elapsed = System.nanoTime() - start;

            double opsPerSec = totalOps.get() / (elapsed / 1_000_000_000.0);
            System.out.printf("%d writers: %,.0f ops/sec%n", writerCount, opsPerSec);

            executor.shutdown();
        }
    }

    // ==================== List 벤치마크 ====================

    @Test
    public void benchmarkListOperations() throws Exception {
        java.util.List<String> list = store.createList("bench_list", String.class);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            list.add("warmup-" + i);
        }

        // Clear and measure add
        list.clear();
        long addStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS / 10; i++) {  // 10000 adds
            list.add("value-" + i);
        }
        long addTime = System.nanoTime() - addStart;

        // Measure random access
        int listSize = list.size();
        Random random = new Random();
        long getStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS / 10; i++) {
            list.get(random.nextInt(listSize));
        }
        long getTime = System.nanoTime() - getStart;

        double addOpsPerSec = (MEASURE_ITERATIONS / 10) / (addTime / 1_000_000_000.0);
        double getOpsPerSec = (MEASURE_ITERATIONS / 10) / (getTime / 1_000_000_000.0);

        System.out.println("\n=== List Operations Benchmark ===");
        System.out.printf("Add: %,.0f ops/sec%n", addOpsPerSec);
        System.out.printf("Get (random): %,.0f ops/sec%n", getOpsPerSec);
    }

    // ==================== 전체 요약 ====================

    @Test
    public void printBenchmarkSummary() {
        System.out.println("\n========================================");
        System.out.println("FxStore Concurrency Benchmark Summary");
        System.out.println("========================================");
        System.out.println("Performance Targets:");
        System.out.println("  - Single-thread Write: >= 50,000 ops/sec");
        System.out.println("  - Single-thread Read:  >= 100,000 ops/sec");
        System.out.println("  - Concurrent Read Scaling: Near-linear");
        System.out.println("  - Mixed Workload (80R/20W): >= 80,000 ops/sec");
        System.out.println("========================================\n");
    }
}
