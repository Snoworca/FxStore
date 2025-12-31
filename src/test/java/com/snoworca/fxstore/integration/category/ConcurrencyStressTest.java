package com.snoworca.fxstore.integration.category;

import com.snoworca.fxstore.api.FxReadTransaction;
import com.snoworca.fxstore.integration.IntegrationTestBase;
import com.snoworca.fxstore.integration.util.ConcurrencyTestHelper;
import org.junit.Test;

import java.util.NavigableMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Category D: 동시성 스트레스 테스트
 *
 * <p>목적: 다중 스레드 환경에서의 안전성을 검증합니다.
 *
 * <p>위험 영역:
 * <ul>
 *   <li>읽기 트랜잭션 MVCC 격리</li>
 *   <li>스냅샷 핀닝/해제</li>
 *   <li>캐시 동시 접근</li>
 * </ul>
 *
 * <p>테스트 케이스:
 * <ul>
 *   <li>D-1: 다중 읽기 트랜잭션 동시</li>
 *   <li>D-2: 읽기 중 쓰기</li>
 *   <li>D-3: 빈번한 커밋 경합</li>
 *   <li>D-4: 스냅샷 회전 스트레스</li>
 *   <li>D-5: 장기 실행 + 빈번한 GC</li>
 * </ul>
 */
public class ConcurrencyStressTest extends IntegrationTestBase {

    /**
     * D-1: 동시 읽기 트랜잭션 안전성
     *
     * <p>목적: 여러 스레드에서 동시에 읽기 트랜잭션 수행
     * <p>위험 영역: 읽기 트랜잭션 격리
     * <p>검증: 모든 스레드 성공
     */
    @Test
    public void test_D1_concurrentReads_shouldBeThreadSafe() throws Exception {
        // Given
        openStore();
        final NavigableMap<Long, String> map = store.createMap("concurrentRead", Long.class, String.class);
        for (int i = 0; i < 10_000; i++) {
            map.put((long) i, "value" + i);
        }
        store.commit();

        // When: 10개 스레드에서 동시 읽기
        final int threadCount = 10;
        AtomicInteger successCount = new AtomicInteger(0);

        boolean success = ConcurrencyTestHelper.runConcurrently(threadCount, () -> {
            try (FxReadTransaction tx = store.beginRead()) {
                // 스냅샷에서 모든 데이터 확인
                assertEquals(10_000, tx.size(map));

                // 랜덤 접근 검증
                for (int i = 0; i < 100; i++) {
                    long key = (long) (i * 100);
                    assertEquals("value" + key, tx.get(map, key));
                }
                successCount.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Then
        assertTrue(success);
        assertEquals(threadCount, successCount.get());
    }

    /**
     * D-2: 읽기 중 쓰기 MVCC 격리
     *
     * <p>목적: 읽기 트랜잭션 중 쓰기 발생 시 격리 유지
     * <p>위험 영역: MVCC 스냅샷 일관성
     * <p>검증: 읽기 트랜잭션은 원래 값 유지
     */
    @Test
    public void test_D2_readWhileWrite_shouldIsolate() throws Exception {
        // Given
        openStore();
        final NavigableMap<Long, String> map = store.createMap("mvccMap", Long.class, String.class);
        for (int i = 0; i < 1000; i++) {
            map.put((long) i, "v" + i);
        }
        store.commit();

        final AtomicBoolean readerSuccess = new AtomicBoolean(false);
        final CountDownLatch readerStarted = new CountDownLatch(1);
        final CountDownLatch writerDone = new CountDownLatch(1);

        // When: 읽기 스레드
        Thread reader = new Thread(() -> {
            try (FxReadTransaction tx = store.beginRead()) {
                readerStarted.countDown();

                // 쓰기 완료 대기
                writerDone.await();

                // 원래 값 확인 (트랜잭션 시작 시점 스냅샷)
                assertEquals(1000, tx.size(map));
                assertEquals("v0", tx.get(map, 0L));
                readerSuccess.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        reader.start();

        // 쓰기
        readerStarted.await();
        for (int i = 0; i < 500; i++) {
            map.put((long) (1000 + i), "new" + i);
        }
        store.commit();
        writerDone.countDown();

        reader.join(10000);

        // Then
        assertTrue(readerSuccess.get());
        assertEquals(1500, map.size());  // 메인 스레드는 최신 값
    }

    /**
     * D-3: 빈번한 커밋 시 데이터 손실 없음
     *
     * <p>목적: 빠른 연속 커밋에서 데이터 일관성
     * <p>위험 영역: 커밋 슬롯 교체
     * <p>검증: 모든 커밋 데이터 유지
     */
    @Test
    public void test_D3_frequentCommits_shouldNotLoseData() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("frequentCommit", Long.class, String.class);

        // When: 빠른 연속 커밋
        for (int i = 0; i < 1000; i++) {
            map.put((long) i, "value" + i);
            store.commit();
        }

        // Then: 모든 데이터 존재
        assertEquals(1000, map.size());

        // And: 재시작 후에도 유지
        reopenStore();
        map = store.openMap("frequentCommit", Long.class, String.class);
        assertEquals(1000, map.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals("value" + i, map.get((long) i));
        }
    }

    /**
     * D-4: 스냅샷 회전 시 리소스 누수 없음
     *
     * <p>목적: 많은 읽기 트랜잭션 생성/종료 시 리소스 관리
     * <p>위험 영역: 스냅샷 핀닝/해제
     * <p>검증: verify 통과
     */
    @Test
    public void test_D4_snapshotRotation_shouldNotLeak() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("snapshotRotation", Long.class, String.class);

        // When: 많은 읽기 트랜잭션 생성/종료
        for (int i = 0; i < 500; i++) {
            map.put((long) i, "v" + i);
            store.commit();

            // 읽기 트랜잭션 생성 후 즉시 종료
            try (FxReadTransaction tx = store.beginRead()) {
                assertEquals(i + 1, tx.size(map));
            }
        }

        // Then: verify 통과 (리소스 정상 해제)
        assertTrue(store.verify().ok());
    }

    /**
     * D-5: 장기 실행 시 메모리 안정성
     *
     * <p>목적: 장기 실행 중 메모리 안정성
     * <p>위험 영역: 메모리 누수
     * <p>검증: OOM 없이 완료
     */
    @Test
    public void test_D5_longRunningWithGC_shouldBeStable() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("longRunningGC", Long.class, String.class);

        // When: 반복적인 삽입/삭제 + GC
        for (int round = 0; round < 50; round++) {
            // 삽입
            for (int i = 0; i < 500; i++) {
                map.put((long) (round * 500 + i), "value" + i);
            }
            store.commit();

            // 일부 삭제
            for (int i = 0; i < 250; i++) {
                map.remove((long) (round * 500 + i));
            }
            store.commit();

            // 강제 GC (메모리 부담 테스트)
            if (round % 10 == 0) {
                System.gc();
            }
        }

        // Then: 정상 동작 (OOM 없음)
        assertTrue(map.size() > 0);
        assertTrue(store.verify().ok());
    }

    /**
     * 추가 테스트: 여러 읽기 트랜잭션의 서로 다른 스냅샷
     */
    @Test
    public void test_D_multipleSnapshots_shouldBeDifferent() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("multiSnap", Long.class, String.class);
        map.put(1L, "v1");
        store.commit();

        // When: 첫 번째 읽기 트랜잭션
        FxReadTransaction tx1 = store.beginRead();

        // 데이터 추가 후 커밋
        map.put(2L, "v2");
        store.commit();

        // 두 번째 읽기 트랜잭션
        FxReadTransaction tx2 = store.beginRead();

        // Then: 각 트랜잭션은 자신의 스냅샷 시점 데이터 보유
        assertEquals(1, tx1.size(map));
        assertEquals(2, tx2.size(map));

        tx1.close();
        tx2.close();
    }

    /**
     * 추가 테스트: 동시 읽기 트랜잭션 중 롤백
     */
    @Test
    public void test_D_readDuringRollback_shouldBeIsolated() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("rollbackIsolate", Long.class, String.class);
        map.put(1L, "committed");
        store.commit();

        // When: 읽기 트랜잭션 중 롤백
        try (FxReadTransaction tx = store.beginRead()) {
            // uncommitted 변경
            map.put(2L, "uncommitted");

            // 롤백
            store.rollback();

            // Then: 읽기 트랜잭션은 영향 없음
            assertEquals(1, tx.size(map));
            assertEquals("committed", tx.get(map, 1L));
        }

        // And: 메인에서도 롤백됨
        assertEquals(1, map.size());
        assertNull(map.get(2L));
    }
}
