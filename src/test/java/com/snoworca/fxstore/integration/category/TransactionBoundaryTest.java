package com.snoworca.fxstore.integration.category;

import com.snoworca.fxstore.api.FxOptions;
import com.snoworca.fxstore.api.FxReadTransaction;
import com.snoworca.fxstore.api.FxStore;
import com.snoworca.fxstore.api.OnClosePolicy;
import com.snoworca.fxstore.integration.IntegrationTestBase;
import org.junit.Test;

import java.util.NavigableMap;

import static org.junit.Assert.*;

/**
 * Category B: 트랜잭션 경계 테스트
 *
 * <p>목적: 트랜잭션 원자성, 격리성, 지속성(AID)을 검증합니다.
 *
 * <p>위험 영역:
 * <ul>
 *   <li>롤백 시 dirty page 정리</li>
 *   <li>읽기 트랜잭션 스냅샷 격리</li>
 *   <li>MVCC 일관성</li>
 * </ul>
 *
 * <p>테스트 케이스:
 * <ul>
 *   <li>B-1: 롤백 후 재시작</li>
 *   <li>B-2: 대량 변경 후 롤백</li>
 *   <li>B-3: 읽기 트랜잭션 격리</li>
 *   <li>B-4: 장기 실행 읽기 트랜잭션</li>
 *   <li>B-5: 커밋 없이 닫기</li>
 * </ul>
 */
public class TransactionBoundaryTest extends IntegrationTestBase {

    /**
     * B-1: 트랜잭션 원자성 검증
     *
     * <p>목적: 롤백이 모든 변경을 취소하는지 검증
     * <p>위험 영역: dirty page 정리
     * <p>검증: 롤백 후 원래 상태 유지
     */
    @Test
    public void test_B1_rollback_shouldDiscardChanges() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("rollbackMap", Long.class, String.class);
        map.put(1L, "committed");
        store.commit();

        // When: 변경 후 롤백
        map.put(2L, "uncommitted");
        map.put(1L, "modified");
        store.rollback();

        // Then: 롤백 전 상태 유지
        assertEquals(1, map.size());
        assertEquals("committed", map.get(1L));
        assertNull(map.get(2L));

        // And: 재시작 후에도 유지
        reopenStore();
        map = store.openMap("rollbackMap", Long.class, String.class);
        assertEquals(1, map.size());
        assertEquals("committed", map.get(1L));
    }

    /**
     * B-2: 대량 롤백 시 메모리 정리 검증
     *
     * <p>목적: 대량 변경 후 롤백 시 메모리 누수 없음
     * <p>위험 영역: 메모리 관리
     * <p>검증: OOM 없이 롤백 완료
     */
    @Test
    public void test_B2_largeRollback_shouldCleanMemory() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("largeRollback", Long.class, String.class);

        // 초기 데이터
        for (int i = 0; i < 1000; i++) {
            map.put((long) i, "initial" + i);
        }
        store.commit();

        // When: 대량 변경
        for (int i = 0; i < 50_000; i++) {
            map.put((long) (1000 + i), "uncommitted" + i);
        }

        // Then: 롤백 성공 (OOM 없음)
        store.rollback();

        // And: 초기 상태 유지
        assertEquals(1000, map.size());
        assertEquals("initial0", map.get(0L));
        assertNull(map.get(1000L));  // uncommitted 데이터 없음

        // And: verify 통과
        assertTrue(store.verify().ok());
    }

    /**
     * B-3: 읽기 트랜잭션 스냅샷 격리 검증
     *
     * <p>목적: 읽기 트랜잭션이 시작 시점의 스냅샷을 유지하는지 검증
     * <p>위험 영역: MVCC 구현
     * <p>검증: 쓰기 후에도 읽기 트랜잭션은 원래 값 유지
     */
    @Test
    public void test_B3_readTransaction_shouldBeIsolated() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("isolationMap", Long.class, String.class);
        map.put(1L, "original");
        store.commit();

        // When: 읽기 트랜잭션 시작
        try (FxReadTransaction tx = store.beginRead()) {
            // 메인 스레드에서 수정
            map.put(1L, "modified");
            map.put(2L, "new");
            store.commit();

            // Then: 읽기 트랜잭션은 원래 값 유지
            assertEquals("original", tx.get(map, 1L));
            assertNull(tx.get(map, 2L));
            assertEquals(1, tx.size(map));
        }

        // And: 트랜잭션 종료 후 최신 값 접근
        assertEquals("modified", map.get(1L));
        assertEquals("new", map.get(2L));
    }

    /**
     * B-4: 장기 실행 트랜잭션 리소스 관리
     *
     * <p>목적: 장기 실행 읽기 트랜잭션이 스냅샷을 유지하는지 검증
     * <p>위험 영역: 스냅샷 핀닝
     * <p>검증: 여러 커밋 후에도 원래 스냅샷 유지
     */
    @Test
    public void test_B4_longRunningRead_shouldMaintainSnapshot() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("longRunning", Long.class, String.class);
        for (int i = 0; i < 1000; i++) {
            map.put((long) i, "v" + i);
        }
        store.commit();

        // When: 읽기 트랜잭션 유지하면서 여러 커밋 발생
        try (FxReadTransaction tx = store.beginRead()) {
            // 여러 번 커밋
            for (int batch = 0; batch < 10; batch++) {
                for (int i = 0; i < 100; i++) {
                    map.put((long) (1000 + batch * 100 + i), "new" + i);
                }
                store.commit();
            }

            // Then: 읽기 트랜잭션은 원래 스냅샷 유지
            assertEquals(1000, tx.size(map));
            for (int i = 0; i < 1000; i++) {
                assertEquals("v" + i, tx.get(map, (long) i));
            }

            // And: 새 데이터는 보이지 않음
            assertNull(tx.get(map, 1000L));
        }

        // And: 트랜잭션 종료 후 최신 데이터 확인
        assertEquals(2000, map.size());
    }

    /**
     * B-5: OnClosePolicy 동작 검증
     *
     * <p>목적: 커밋 없이 닫을 때 정책에 따른 동작 확인
     * <p>위험 영역: Store 종료 처리
     * <p>검증: ROLLBACK 정책 시 uncommitted 변경 취소
     */
    @Test
    public void test_B5_closeWithoutCommit_shouldFollowPolicy() throws Exception {
        // Given: ROLLBACK 정책으로 열기
        FxOptions options = rollbackPolicyBuilder().build();
        store = FxStore.open(storeFile.toPath(), options);

        NavigableMap<Long, String> map = store.createMap("policyMap", Long.class, String.class);
        map.put(1L, "committed");
        store.commit();

        map.put(2L, "uncommitted");

        // When: 커밋 없이 닫기
        store.close();
        store = null;

        // Then: uncommitted 데이터 롤백됨
        openStore();
        map = store.openMap("policyMap", Long.class, String.class);
        assertEquals(1, map.size());
        assertEquals("committed", map.get(1L));
        assertNull(map.get(2L));
    }

    /**
     * 추가 테스트: 빈 트랜잭션 커밋
     */
    @Test
    public void test_B_emptyCommit_shouldBeNoOp() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("emptyCommit", Long.class, String.class);
        map.put(1L, "value");
        store.commit();

        // When: 변경 없이 커밋
        store.commit();
        store.commit();
        store.commit();

        // Then: 데이터 유지
        assertEquals(1, map.size());
        assertEquals("value", map.get(1L));

        // And: verify 통과
        assertTrue(store.verify().ok());
    }

    /**
     * 추가 테스트: 롤백 후 새 데이터 삽입
     */
    @Test
    public void test_B_afterRollback_shouldAcceptNewData() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("afterRollback", Long.class, String.class);
        map.put(1L, "initial");
        store.commit();

        // When: 롤백 후 새 데이터
        map.put(2L, "will_be_rolled_back");
        store.rollback();

        map.put(3L, "new_data");
        store.commit();

        // Then: 새 데이터만 추가됨
        assertEquals(2, map.size());
        assertEquals("initial", map.get(1L));
        assertNull(map.get(2L));
        assertEquals("new_data", map.get(3L));

        // And: 재시작 후 확인
        reopenStore();
        map = store.openMap("afterRollback", Long.class, String.class);
        assertEquals(2, map.size());
    }

    /**
     * 추가 테스트: 여러 컬렉션 동시 롤백
     */
    @Test
    public void test_B_multipleCollections_shouldRollbackTogether() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map1 = store.createMap("multi1", Long.class, String.class);
        NavigableMap<Long, String> map2 = store.createMap("multi2", Long.class, String.class);

        map1.put(1L, "m1-committed");
        map2.put(1L, "m2-committed");
        store.commit();

        // When: 두 컬렉션 변경 후 롤백
        map1.put(2L, "m1-uncommitted");
        map2.put(2L, "m2-uncommitted");
        store.rollback();

        // Then: 두 컬렉션 모두 롤백됨
        assertEquals(1, map1.size());
        assertEquals(1, map2.size());
        assertNull(map1.get(2L));
        assertNull(map2.get(2L));
    }
}
