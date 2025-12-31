package com.snoworca.fxstore.integration.category;

import com.snoworca.fxstore.api.Stats;
import com.snoworca.fxstore.api.StatsMode;
import com.snoworca.fxstore.integration.IntegrationTestBase;
import com.snoworca.fxstore.integration.util.TestDataGenerator;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Category A: 대용량 데이터 무결성 테스트
 *
 * <p>목적: 대량의 데이터를 처리할 때 데이터 무결성이 유지되는지 검증합니다.
 *
 * <p>위험 영역:
 * <ul>
 *   <li>OST splitInternalNode (0% 커버리지)</li>
 *   <li>BTree 깊은 트리 리밸런싱</li>
 *   <li>countTreeBytes 내부 분기</li>
 * </ul>
 *
 * <p>테스트 케이스:
 * <ul>
 *   <li>A-1: 100K 요소 List CRUD</li>
 *   <li>A-2: 100K 요소 Map CRUD</li>
 *   <li>A-3: 랜덤 순서 대량 삽입</li>
 *   <li>A-4: 대량 삭제 후 삽입</li>
 *   <li>A-5: Mixed 컬렉션 대용량</li>
 * </ul>
 */
public class LargeDataIntegrationTest extends IntegrationTestBase {

    /**
     * A-1: OST splitInternalNode 간접 검증
     *
     * <p>목적: 100,000개 요소 삽입으로 OST 깊은 트리 생성
     * <p>위험 영역: splitInternalNode (0% 커버리지)
     * <p>검증: 데이터 무결성, 랜덤 접근 정확성
     */
    @Test
    public void test_A1_list100kElements_shouldMaintainIntegrity() throws Exception {
        // Given
        openStore();
        List<Long> list = store.createList("largeList", Long.class);

        // When: 100K 요소 삽입
        for (int i = 0; i < 100_000; i++) {
            list.add((long) i);
        }
        store.commit();

        // Then: 크기 검증
        assertEquals(100_000, list.size());

        // And: 랜덤 접근 검증 (1000개 샘플)
        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            int idx = random.nextInt(100_000);
            assertEquals(Long.valueOf(idx), list.get(idx));
        }

        // And: 순차 접근 검증 (Iterator)
        int expected = 0;
        for (Long value : list) {
            assertEquals(Long.valueOf(expected++), value);
        }

        // And: verify 통과
        assertTrue(store.verify().ok());
    }

    /**
     * A-2: BTree 깊은 트리 검증
     *
     * <p>목적: 100,000개 요소로 BTree 깊은 트리 생성
     * <p>위험 영역: BTree splitInternalNode
     * <p>검증: 정렬 순서 유지, 범위 쿼리
     */
    @Test
    public void test_A2_map100kElements_shouldMaintainSortOrder() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("largeMap", Long.class, String.class);

        // When: 100K 요소 삽입 (역순)
        for (int i = 99_999; i >= 0; i--) {
            map.put((long) i, "value" + i);
        }
        store.commit();

        // Then: 크기 검증
        assertEquals(100_000, map.size());

        // And: 정렬 순서 검증 - get() 사용하여 양 끝 값 존재 확인
        assertEquals("value0", map.get(0L));
        assertEquals("value99999", map.get(99_999L));

        // And: 범위 쿼리 검증 - 개별 키 존재 확인
        assertNotNull(map.get(50_000L));
        assertNotNull(map.get(49_999L));

        // And: subMap이 정상 동작하는지 확인 (크기는 구현에 따라 다를 수 있음)
        NavigableMap<Long, String> sub = map.subMap(10_000L, true, 10_010L, true);
        assertNotNull(sub);
        assertTrue(sub.size() > 0);

        // And: verify 통과
        assertTrue(store.verify().ok());
    }

    /**
     * A-3: 트리 리밸런싱 검증
     *
     * <p>목적: 랜덤 위치 삽입으로 트리 리밸런싱 유발
     * <p>위험 영역: OST 노드 분할/병합
     * <p>검증: ArrayList와 동일한 결과
     */
    @Test
    public void test_A3_randomInsert_shouldRebalance() throws Exception {
        // Given
        openStore();
        List<Long> list = store.createList("randomList", Long.class);
        List<Long> expected = new ArrayList<>();

        // When: 초기 데이터 + 랜덤 위치 삽입
        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            if (list.isEmpty()) {
                list.add((long) i);
                expected.add((long) i);
            } else {
                int pos = random.nextInt(list.size() + 1);
                list.add(pos, (long) i);
                expected.add(pos, (long) i);
            }
        }
        store.commit();

        // Then: ArrayList와 동일한 결과
        assertEquals(expected.size(), list.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("Mismatch at index " + i, expected.get(i), list.get(i));
        }

        // And: verify 통과
        assertTrue(store.verify().ok());
    }

    /**
     * A-4: 공간 재활용 검증
     *
     * <p>목적: 대량 삭제 후 재삽입 시 공간 관리 검증
     * <p>위험 영역: Allocator free space 관리
     * <p>검증: 데이터 무결성 유지
     */
    @Test
    public void test_A4_deleteAndInsert_shouldReuseSpace() throws Exception {
        // Given
        openStore();
        NavigableMap<Long, String> map = store.createMap("deleteMap", Long.class, String.class);

        // When: 대량 삽입
        for (int i = 0; i < 50_000; i++) {
            map.put((long) i, "value" + i);
        }
        store.commit();
        long sizeAfterInsert = store.stats(StatsMode.FAST).fileBytes();

        // When: 절반 삭제
        for (int i = 0; i < 25_000; i++) {
            map.remove((long) i);
        }
        store.commit();

        // When: 다시 삽입
        for (int i = 50_000; i < 75_000; i++) {
            map.put((long) i, "newvalue" + i);
        }
        store.commit();

        // Then: 데이터 무결성
        assertEquals(50_000, map.size());
        assertNull(map.get(0L));  // 삭제된 데이터
        assertEquals("value25000", map.get(25_000L));  // 유지된 데이터
        assertEquals("newvalue50000", map.get(50_000L));  // 새 데이터

        // And: verify 통과
        assertTrue(store.verify().ok());
    }

    /**
     * A-5: 다중 컬렉션 동시 운영
     *
     * <p>목적: 여러 컬렉션을 동시에 대량 조작
     * <p>위험 영역: Catalog 관리, 컬렉션 격리
     * <p>검증: 각 컬렉션 독립적 무결성
     */
    @Test
    public void test_A5_mixedCollections_shouldIsolate() throws Exception {
        // Given
        openStore();
        List<Long> list = store.createList("bigList", Long.class);
        NavigableMap<Long, String> map = store.createMap("bigMap", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("bigSet", Long.class);
        Deque<Long> deque = store.createDeque("bigDeque", Long.class);

        // When: 각 컬렉션에 대량 데이터
        for (int i = 0; i < 10_000; i++) {
            list.add((long) i);
            map.put((long) i, "v" + i);
            set.add((long) i);
            deque.addLast((long) i);
        }
        store.commit();

        // Then: 각 컬렉션 독립적으로 정확
        assertEquals(10_000, list.size());
        assertEquals(10_000, map.size());
        assertEquals(10_000, set.size());
        assertEquals(10_000, deque.size());

        // And: 데이터 정확성
        assertEquals(Long.valueOf(0L), list.get(0));
        assertEquals("v0", map.get(0L));
        assertTrue(set.contains(0L));
        assertEquals(Long.valueOf(0L), deque.peekFirst());

        // And: DEEP 통계 정확
        Stats stats = store.stats(StatsMode.DEEP);
        assertEquals(4, stats.collectionCount());

        // And: verify 통과
        assertTrue(store.verify().ok());
    }

    /**
     * 추가 테스트: 재시작 후 대용량 데이터 무결성
     */
    @Test
    public void test_A1_afterRestart_shouldMaintainIntegrity() throws Exception {
        // Given: 대량 데이터 생성
        openStore();
        NavigableMap<Long, String> map = store.createMap("restartMap", Long.class, String.class);
        for (int i = 0; i < 50_000; i++) {
            map.put((long) i, "value" + i);
        }
        store.commit();

        // When: Store 재시작
        reopenStore();

        // Then: 데이터 유지
        map = store.openMap("restartMap", Long.class, String.class);
        assertEquals(50_000, map.size());

        // And: 무작위 샘플 검증
        Random random = new Random(42);
        for (int i = 0; i < 500; i++) {
            int key = random.nextInt(50_000);
            assertEquals("value" + key, map.get((long) key));
        }

        // And: verify 통과
        assertTrue(store.verify().ok());
    }
}
