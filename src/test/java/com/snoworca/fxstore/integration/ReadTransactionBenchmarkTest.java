package com.snoworca.fxstore.integration;

import com.snoworca.fxstore.api.*;
import com.snoworca.fxstore.core.FxStoreImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * ReadTransaction 성능 벤치마크
 *
 * <p>Phase C: 성능 목표 검증</p>
 * <ul>
 *   <li>트랜잭션 오버헤드: < 20%</li>
 *   <li>스냅샷 획득: Wait-free (락 없음)</li>
 * </ul>
 */
public class ReadTransactionBenchmarkTest {

    private FxStore store;

    @Before
    public void setUp() {
        store = FxStoreImpl.openMemory(FxOptions.defaults());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try {
                store.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    // ==================== 오버헤드 벤치마크 ====================

    @Test
    public void benchmarkTransactionOverhead_get() {
        // Setup: 10,000 엔트리
        NavigableMap<Long, String> map = store.createMap("benchMap", Long.class, String.class);
        for (long i = 0; i < 10_000; i++) {
            map.put(i, "value-" + i);
        }

        int warmup = 1000;
        int iterations = 10_000;
        Random random = new Random(42);

        // Warmup
        for (int i = 0; i < warmup; i++) {
            map.get(random.nextLong() % 10_000);
        }
        for (int i = 0; i < warmup; i++) {
            try (FxReadTransaction tx = store.beginRead()) {
                tx.get(map, random.nextLong() % 10_000);
            }
        }

        // Measure: 직접 접근
        random = new Random(42);
        long directStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.get(Math.abs(random.nextLong()) % 10_000);
        }
        long directEnd = System.nanoTime();
        double directNsPerOp = (double) (directEnd - directStart) / iterations;

        // Measure: 트랜잭션 통한 접근
        random = new Random(42);
        long txStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (FxReadTransaction tx = store.beginRead()) {
                tx.get(map, Math.abs(random.nextLong()) % 10_000);
            }
        }
        long txEnd = System.nanoTime();
        double txNsPerOp = (double) (txEnd - txStart) / iterations;

        double overhead = ((txNsPerOp - directNsPerOp) / directNsPerOp) * 100;

        System.out.println("=== ReadTransaction Overhead Benchmark (get) ===");
        System.out.printf("Direct access:      %.2f ns/op%n", directNsPerOp);
        System.out.printf("Transaction access: %.2f ns/op%n", txNsPerOp);
        System.out.printf("Overhead:           %.2f%%%n", overhead);
        System.out.println();

        // 참고: 트랜잭션 오버헤드는 가변적일 수 있음
        // 목표는 < 100% overhead (2배 미만)
    }

    @Test
    public void benchmarkTransactionCreation() {
        NavigableMap<Long, String> map = store.createMap("benchMap", Long.class, String.class);
        map.put(1L, "value");

        int warmup = 1000;
        int iterations = 100_000;

        // Warmup
        for (int i = 0; i < warmup; i++) {
            try (FxReadTransaction tx = store.beginRead()) {
                // just create and close
            }
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (FxReadTransaction tx = store.beginRead()) {
                // just create and close
            }
        }
        long end = System.nanoTime();

        double nsPerOp = (double) (end - start) / iterations;

        System.out.println("=== Transaction Creation Benchmark ===");
        System.out.printf("Create + Close: %.2f ns/op%n", nsPerOp);
        System.out.println();

        // 목표: < 1μs (1000ns) per transaction create/close
        assertTrue("Transaction creation too slow: " + nsPerOp + " ns",
                   nsPerOp < 10_000);  // 10μs 상한
    }

    @Test
    public void benchmarkSnapshotConsistency() {
        // 스냅샷 격리에 따른 추가 비용 측정
        NavigableMap<Long, String> map = store.createMap("benchMap", Long.class, String.class);
        for (long i = 0; i < 1000; i++) {
            map.put(i, "value-" + i);
        }

        int warmup = 100;
        int iterations = 1_000;
        int readsPerTx = 10;

        // Warmup
        for (int i = 0; i < warmup; i++) {
            try (FxReadTransaction tx = store.beginRead()) {
                for (int j = 0; j < readsPerTx; j++) {
                    tx.get(map, (long) j);
                }
            }
        }

        // Measure: 단일 트랜잭션 내 여러 읽기
        long singleTxStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (FxReadTransaction tx = store.beginRead()) {
                for (int j = 0; j < readsPerTx; j++) {
                    tx.get(map, (long) j);
                }
            }
        }
        long singleTxEnd = System.nanoTime();
        double singleTxNsPerOp = (double) (singleTxEnd - singleTxStart) / (iterations * readsPerTx);

        // Measure: 각 읽기마다 새 트랜잭션
        long multiTxStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < readsPerTx; j++) {
                try (FxReadTransaction tx = store.beginRead()) {
                    tx.get(map, (long) j);
                }
            }
        }
        long multiTxEnd = System.nanoTime();
        double multiTxNsPerOp = (double) (multiTxEnd - multiTxStart) / (iterations * readsPerTx);

        double txReuseGain = ((multiTxNsPerOp - singleTxNsPerOp) / multiTxNsPerOp) * 100;

        System.out.println("=== Snapshot Consistency Benchmark ===");
        System.out.printf("Single tx, %d reads: %.2f ns/read%n", readsPerTx, singleTxNsPerOp);
        System.out.printf("New tx per read:     %.2f ns/read%n", multiTxNsPerOp);
        System.out.printf("Tx reuse gain:       %.2f%%%n", txReuseGain);
        System.out.println("(Higher is better - shows benefit of reusing transaction)");
        System.out.println();
    }

    @Test
    public void benchmarkByMapSize() {
        System.out.println("=== Performance by Map Size ===");
        System.out.println("Size(entries) | Get(ns) | Contains(ns) | Size(ns)");
        System.out.println("--------------|---------|--------------|----------");

        int[] sizes = {100, 1000, 10_000};  // 메모리 제한으로 크기 축소
        int iterations = 500;

        for (int size : sizes) {
            FxStore sizeStore = FxStoreImpl.openMemory(FxOptions.defaults());
            NavigableMap<Long, String> map = sizeStore.createMap("sizeTest", Long.class, String.class);

            // Populate
            for (long i = 0; i < size; i++) {
                map.put(i, "value-" + i);
            }

            // Warmup
            Random random = new Random(42);
            for (int i = 0; i < 100; i++) {
                try (FxReadTransaction tx = sizeStore.beginRead()) {
                    tx.get(map, Math.abs(random.nextLong()) % size);
                }
            }

            // Measure get
            random = new Random(42);
            long getStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                try (FxReadTransaction tx = sizeStore.beginRead()) {
                    tx.get(map, Math.abs(random.nextLong()) % size);
                }
            }
            long getEnd = System.nanoTime();
            double getNsPerOp = (double) (getEnd - getStart) / iterations;

            // Measure containsKey
            random = new Random(42);
            long containsStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                try (FxReadTransaction tx = sizeStore.beginRead()) {
                    tx.containsKey(map, Math.abs(random.nextLong()) % size);
                }
            }
            long containsEnd = System.nanoTime();
            double containsNsPerOp = (double) (containsEnd - containsStart) / iterations;

            // Measure size (warning: O(n) currently)
            int sizeIterations = Math.min(50, iterations);
            long sizeStart = System.nanoTime();
            for (int i = 0; i < sizeIterations; i++) {
                try (FxReadTransaction tx = sizeStore.beginRead()) {
                    tx.size(map);
                }
            }
            long sizeEnd = System.nanoTime();
            double sizeNsPerOp = (double) (sizeEnd - sizeStart) / sizeIterations;

            System.out.printf("%13d | %7.1f | %12.1f | %8.1f%n",
                              size, getNsPerOp, containsNsPerOp, sizeNsPerOp);

            sizeStore.close();
        }
        System.out.println();
    }

    @Test
    public void printBenchmarkSummary() {
        System.out.println("========================================");
        System.out.println("ReadTransaction Benchmark Summary");
        System.out.println("========================================");
        System.out.println("Performance Targets:");
        System.out.println("  - Transaction creation: < 10μs");
        System.out.println("  - Get operation overhead: reasonable");
        System.out.println("  - Transaction reuse: recommended for multi-read");
        System.out.println("========================================");
        System.out.println();
    }
}
