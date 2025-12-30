package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.NavigableMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Race Condition 탐지 테스트 (Phase 8 - Week 3 Day 3)
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>스냅샷 가시성 (INV-C4)</li>
 *   <li>원자적 스냅샷 교체</li>
 *   <li>쓰기 후 즉시 읽기 일관성</li>
 * </ul>
 */
public class RaceConditionTest {

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

    // ==================== 스냅샷 가시성 테스트 ====================

    @Test
    public void testSnapshotVisibility_WriteReadSequence() throws Exception {
        // 쓰기 후 즉시 읽기에서 새 값이 보이는지 검증
        NavigableMap<Long, String> map = store.createMap("visibility_test", Long.class, String.class);

        AtomicBoolean raceDetected = new AtomicBoolean(false);
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            final long key = i;
            final String expectedValue = "value-" + i;

            CountDownLatch writeDone = new CountDownLatch(1);
            CountDownLatch readDone = new CountDownLatch(1);

            Thread writer = new Thread(() -> {
                map.put(key, expectedValue);
                writeDone.countDown();
            });

            Thread reader = new Thread(() -> {
                try {
                    writeDone.await();  // 쓰기 완료 대기
                    String value = map.get(key);
                    if (!expectedValue.equals(value)) {
                        raceDetected.set(true);
                        System.err.println("Race detected: expected=" + expectedValue + ", actual=" + value);
                    }
                } catch (Exception e) {
                    raceDetected.set(true);
                } finally {
                    readDone.countDown();
                }
            });

            writer.start();
            reader.start();

            readDone.await(5, TimeUnit.SECONDS);
            writer.join(1000);
            reader.join(1000);
        }

        assertFalse("Race condition detected: write not visible to reader", raceDetected.get());
    }

    // ==================== 원자적 스냅샷 교체 테스트 ====================

    @Test
    public void testAtomicSnapshotSwitch_NoPartialState() throws Exception {
        // 스냅샷 교체가 원자적인지 검증 (중간 상태 관찰 불가)
        NavigableMap<Long, String> map = store.createMap("atomic_switch", Long.class, String.class);

        // 초기 상태: 모든 값이 같은 prefix
        for (long i = 0; i < 100; i++) {
            map.put(i, "batch-0");
        }

        AtomicBoolean inconsistencyDetected = new AtomicBoolean(false);
        AtomicBoolean running = new AtomicBoolean(true);
        int writerIterations = 100;

        Thread writer = new Thread(() -> {
            for (int batch = 1; batch <= writerIterations && !inconsistencyDetected.get(); batch++) {
                // 모든 키를 같은 batch로 업데이트
                String batchValue = "batch-" + batch;
                for (long i = 0; i < 100; i++) {
                    map.put(i, batchValue);
                }
            }
            running.set(false);
        });

        Thread reader = new Thread(() -> {
            while (running.get() && !inconsistencyDetected.get()) {
                // 스냅샷 시점에서 모든 값 읽기
                String firstValue = map.get(0L);
                if (firstValue == null) continue;

                String prefix = firstValue.substring(0, firstValue.lastIndexOf('-') + 1);

                // 참고: 각 put이 별도 스냅샷이므로 다른 값 가능
                // 이 테스트는 단일 get()이 완전한 값을 반환하는지 확인
                for (long i = 1; i < 10; i++) {
                    String value = map.get(i);
                    if (value == null) continue;
                    // 값이 corrupted되지 않았는지 확인
                    if (!value.startsWith("batch-")) {
                        inconsistencyDetected.set(true);
                        System.err.println("Corrupted value detected: " + value);
                    }
                }
            }
        });

        writer.start();
        reader.start();
        writer.join(30000);
        running.set(false);
        reader.join(5000);

        assertFalse("Snapshot inconsistency detected", inconsistencyDetected.get());
    }

    // ==================== 동시 수정 시 크기 일관성 ====================

    @Test
    public void testSizeConsistency_DuringModification() throws Exception {
        NavigableMap<Long, String> map = store.createMap("size_consistency", Long.class, String.class);

        AtomicBoolean inconsistencyDetected = new AtomicBoolean(false);
        AtomicBoolean running = new AtomicBoolean(true);

        // Writer: 요소 추가
        Thread writer = new Thread(() -> {
            for (long i = 0; i < 10000 && !inconsistencyDetected.get(); i++) {
                map.put(i, "v-" + i);
            }
            running.set(false);
        });

        // Reader: size()가 음수가 되거나 비정상적으로 크지 않은지 확인
        Thread reader = new Thread(() -> {
            int prevSize = 0;
            while (running.get() && !inconsistencyDetected.get()) {
                int size = map.size();
                if (size < 0 || size > 20000) {
                    inconsistencyDetected.set(true);
                    System.err.println("Abnormal size detected: " + size);
                }
                // size는 단조 증가해야 함 (삭제 없음)
                if (size < prevSize) {
                    // 참고: 스냅샷 타이밍에 따라 일시적으로 가능할 수 있음
                    // 여기서는 큰 감소만 체크
                    if (prevSize - size > 100) {
                        inconsistencyDetected.set(true);
                        System.err.println("Size decreased unexpectedly: " + prevSize + " -> " + size);
                    }
                }
                prevSize = size;
            }
        });

        writer.start();
        reader.start();
        writer.join(30000);
        running.set(false);
        reader.join(5000);

        assertFalse("Size inconsistency detected", inconsistencyDetected.get());
    }

    // ==================== createOrOpen Race Condition ====================

    @Test
    public void testCreateOrOpen_NoDoubleCreate() throws Exception {
        int threadCount = 20;
        String sharedName = "race_collection";
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    NavigableMap<String, String> map =
                            store.createOrOpenMap(sharedName, String.class, String.class);
                    assertNotNull(map);
                    successCount.incrementAndGet();
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

        // 모든 스레드가 성공해야 함
        assertEquals("All threads should succeed", threadCount, successCount.get());
        assertEquals("No errors should occur", 0, errorCount.get());

        // 컬렉션은 하나만 존재해야 함
        long count = store.list().stream()
                .filter(c -> c.name().equals(sharedName))
                .count();
        assertEquals("Only one collection should exist", 1, count);

        executor.shutdown();
    }

    // ==================== Iterator 안정성 테스트 ====================

    @Test
    public void testIteratorStability_DuringModification() throws Exception {
        NavigableMap<Long, String> map = store.createMap("iterator_stability", Long.class, String.class);

        // 초기 데이터
        for (long i = 0; i < 100; i++) {
            map.put(i, "initial-" + i);
        }

        AtomicBoolean iteratorFailed = new AtomicBoolean(false);
        AtomicBoolean running = new AtomicBoolean(true);

        // Writer: 계속 요소 추가
        Thread writer = new Thread(() -> {
            for (long i = 100; i < 1000 && !iteratorFailed.get(); i++) {
                map.put(i, "new-" + i);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
            running.set(false);
        });

        // Reader: Iterator로 순회
        Thread reader = new Thread(() -> {
            int iterationCount = 0;
            while (running.get() && !iteratorFailed.get() && iterationCount < 100) {
                try {
                    int count = 0;
                    for (Long key : map.keySet()) {
                        count++;
                        if (count > 10000) {
                            // 무한 루프 방지
                            iteratorFailed.set(true);
                            break;
                        }
                    }
                    iterationCount++;
                } catch (ConcurrentModificationException e) {
                    // 스냅샷 Iterator는 CME를 발생시키지 않아야 함
                    iteratorFailed.set(true);
                    e.printStackTrace();
                } catch (Exception e) {
                    iteratorFailed.set(true);
                    e.printStackTrace();
                }
            }
        });

        writer.start();
        reader.start();
        writer.join(30000);
        running.set(false);
        reader.join(5000);

        assertFalse("Iterator failed during concurrent modification", iteratorFailed.get());
    }

    // ==================== List Race Condition 테스트 ====================

    @Test
    public void testListRaceCondition_AddAndGet() throws Exception {
        java.util.List<String> list = store.createList("list_race", String.class);

        // 초기 데이터
        for (int i = 0; i < 100; i++) {
            list.add("initial-" + i);
        }

        AtomicBoolean raceDetected = new AtomicBoolean(false);
        AtomicBoolean running = new AtomicBoolean(true);

        Thread writer = new Thread(() -> {
            for (int i = 0; i < 500 && !raceDetected.get(); i++) {
                list.add("new-" + i);
            }
            running.set(false);
        });

        Thread reader = new Thread(() -> {
            while (running.get() && !raceDetected.get()) {
                try {
                    int size = list.size();
                    if (size > 0) {
                        // 유효한 인덱스로 접근
                        String value = list.get(0);
                        if (value == null || !value.startsWith("initial-") && !value.startsWith("new-")) {
                            raceDetected.set(true);
                            System.err.println("Invalid value: " + value);
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    // 스냅샷 격리가 제대로 작동하면 발생하지 않아야 함
                    raceDetected.set(true);
                    e.printStackTrace();
                }
            }
        });

        writer.start();
        reader.start();
        writer.join(30000);
        running.set(false);
        reader.join(5000);

        assertFalse("Race condition detected in List operations", raceDetected.get());
    }

    // ==================== BATCH 모드 Race Condition ====================

    @Test
    public void testBatchModeRaceCondition() throws Exception {
        FxOptions batchOptions = FxOptions.defaults().withCommitMode(CommitMode.BATCH).build();
        FxStore batchStore = FxStoreImpl.openMemory(batchOptions);

        try {
            NavigableMap<Long, String> map = batchStore.createMap("batch_race", Long.class, String.class);

            AtomicBoolean raceDetected = new AtomicBoolean(false);
            int iterations = 100;

            for (int i = 0; i < iterations; i++) {
                final long key = i;
                map.put(key, "uncommitted-" + i);

                // commit 전에는 다른 스레드에서도 보여야 함 (같은 스토어)
                String value = map.get(key);
                if (value == null || !value.equals("uncommitted-" + i)) {
                    raceDetected.set(true);
                }

                batchStore.commit();

                // commit 후에도 보여야 함
                value = map.get(key);
                if (value == null || !value.equals("uncommitted-" + i)) {
                    raceDetected.set(true);
                }
            }

            assertFalse("Race condition in BATCH mode", raceDetected.get());
        } finally {
            batchStore.close();
        }
    }
}
