package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.FxReadTransaction;
import com.snoworca.fxstore.api.FxStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Deque;

import static org.junit.Assert.*;

/**
 * Deque 성능 테스트
 *
 * <p>v0.7 최적화 검증: peekFirst/peekLast O(log n)</p>
 */
public class DequePerformanceTest {

    private File tempFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-deque-perf-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) {
            store.close();
        }
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /**
     * peekFirst/peekLast O(log n) 검증
     *
     * <p>10,000개 요소로 성능 측정</p>
     */
    @Test
    public void testPeekFirstLast_performance() {
        int elementCount = 10000;
        Deque<String> deque = store.createDeque("perfDeque", String.class);

        // 데이터 삽입
        for (int i = 0; i < elementCount; i++) {
            deque.addLast("item-" + i);
        }

        // peekFirst 성능 측정
        long startTime = System.nanoTime();
        int iterations = 10000;
        try (FxReadTransaction tx = store.beginRead()) {
            for (int i = 0; i < iterations; i++) {
                String first = tx.peekFirst(deque);
                assertEquals("item-0", first);
            }
        }
        long peekFirstElapsed = System.nanoTime() - startTime;
        double peekFirstNsPerOp = (double) peekFirstElapsed / iterations;

        // peekLast 성능 측정
        startTime = System.nanoTime();
        try (FxReadTransaction tx = store.beginRead()) {
            for (int i = 0; i < iterations; i++) {
                String last = tx.peekLast(deque);
                assertEquals("item-" + (elementCount - 1), last);
            }
        }
        long peekLastElapsed = System.nanoTime() - startTime;
        double peekLastNsPerOp = (double) peekLastElapsed / iterations;

        System.out.printf("peekFirst (n=%d): %.2f ns/op (%.2f μs/op)%n",
                          elementCount, peekFirstNsPerOp, peekFirstNsPerOp / 1000);
        System.out.printf("peekLast (n=%d): %.2f ns/op (%.2f μs/op)%n",
                          elementCount, peekLastNsPerOp, peekLastNsPerOp / 1000);

        // O(log n) 기대: < 50μs (여유 있게)
        // O(n) 이면 ~100,000ns 이상 예상
        assertTrue("peekFirst should be O(log n) - expected < 50μs, got " + (peekFirstNsPerOp / 1000) + "μs",
                   peekFirstNsPerOp < 50000); // 50μs 미만
        assertTrue("peekLast should be O(log n) - expected < 50μs, got " + (peekLastNsPerOp / 1000) + "μs",
                   peekLastNsPerOp < 50000);
    }

    /**
     * addFirst로 추가된 요소도 정확히 peekFirst로 조회되는지 검증
     */
    @Test
    public void testPeekFirst_withAddFirst() {
        Deque<String> deque = store.createDeque("addFirstDeque", String.class);

        // addLast로 A, B, C 추가
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");

        // addFirst로 Z, Y 추가 (논리적 순서: Y, Z, A, B, C)
        deque.addFirst("Z");
        deque.addFirst("Y");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("Y", tx.peekFirst(deque));
            assertEquals("C", tx.peekLast(deque));
            assertEquals(5, tx.size(deque));
        }
    }

    /**
     * 혼합 addFirst/addLast 후 순서 검증
     */
    @Test
    public void testDequeOrdering_mixedOperations() {
        Deque<Long> deque = store.createDeque("mixedDeque", Long.class);

        // 시퀀스: 0, 1, 2에 addLast
        deque.addLast(100L);  // seq=0
        deque.addLast(101L);  // seq=1
        deque.addLast(102L);  // seq=2

        // addFirst로 -1, -2에 추가
        deque.addFirst(99L);  // seq=-1
        deque.addFirst(98L);  // seq=-2

        // 논리적 순서: 98, 99, 100, 101, 102

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(Long.valueOf(98L), tx.peekFirst(deque));
            assertEquals(Long.valueOf(102L), tx.peekLast(deque));
        }

        // 직접 deque 메서드로도 확인
        assertEquals(Long.valueOf(98L), deque.peekFirst());
        assertEquals(Long.valueOf(102L), deque.peekLast());
    }

    /**
     * 스냅샷 격리 테스트
     */
    @Test
    public void testSnapshotIsolation_peekFirst() {
        Deque<String> deque = store.createDeque("snapshotDeque", String.class);

        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");

        // 트랜잭션 시작
        FxReadTransaction tx = store.beginRead();
        assertEquals("A", tx.peekFirst(deque));

        // 트랜잭션 중 수정
        deque.addFirst("Z");

        // 트랜잭션은 이전 상태 유지
        assertEquals("A", tx.peekFirst(deque));

        tx.close();

        // 새 트랜잭션은 변경 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertEquals("Z", tx2.peekFirst(deque));
        }
    }

    /**
     * 빈 Deque 테스트
     */
    @Test
    public void testPeekFirst_emptyDeque() {
        Deque<String> deque = store.createDeque("emptyDeque", String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.peekFirst(deque));
            assertNull(tx.peekLast(deque));
            assertEquals(0, tx.size(deque));
        }
    }

    /**
     * 단일 요소 Deque 테스트
     */
    @Test
    public void testPeekFirst_singleElement() {
        Deque<String> deque = store.createDeque("singleDeque", String.class);
        deque.addLast("only");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("only", tx.peekFirst(deque));
            assertEquals("only", tx.peekLast(deque));
            assertEquals(1, tx.size(deque));
        }
    }

    /**
     * size O(1) 검증
     *
     * <p>캐싱된 count를 사용하므로 O(1) 성능 기대</p>
     */
    @Test
    public void testSize_performance() {
        int elementCount = 10000;
        Deque<String> deque = store.createDeque("sizeDeque", String.class);

        // 데이터 삽입
        for (int i = 0; i < elementCount; i++) {
            deque.addLast("item-" + i);
        }

        // size 성능 측정
        long startTime = System.nanoTime();
        int iterations = 100000;
        try (FxReadTransaction tx = store.beginRead()) {
            for (int i = 0; i < iterations; i++) {
                int size = tx.size(deque);
                assertEquals(elementCount, size);
            }
        }
        long elapsed = System.nanoTime() - startTime;
        double nsPerOp = (double) elapsed / iterations;

        System.out.printf("size (n=%d): %.2f ns/op%n", elementCount, nsPerOp);

        // O(1) 기대: < 1000ns (여유 있게)
        // O(n)이면 ~100,000ns 이상 예상
        assertTrue("size should be O(1) - expected < 1μs, got " + nsPerOp + "ns",
                   nsPerOp < 1000);
    }

    /**
     * 추가/삭제 후 size 정확성 검증
     */
    @Test
    public void testSize_afterModifications() {
        Deque<String> deque = store.createDeque("modDeque", String.class);

        // 추가
        deque.addLast("A");
        deque.addLast("B");
        deque.addFirst("Z");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(3, tx.size(deque));
        }

        // 삭제
        deque.pollFirst();

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(2, tx.size(deque));
        }

        // 다시 추가
        deque.addLast("C");
        deque.addLast("D");

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(4, tx.size(deque));
        }
    }
}
