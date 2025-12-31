package com.snoworca.fxstore.integration.category;

import com.snoworca.fxstore.api.FxException;
import com.snoworca.fxstore.integration.IntegrationTestBase;
import com.snoworca.fxstore.integration.util.TestDataGenerator;
import org.junit.Test;

import java.util.Deque;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;

import static org.junit.Assert.*;

/**
 * Category E: 엣지 케이스 테스트
 *
 * <p>목적: 경계 조건과 특수 상황에서의 동작을 검증합니다.
 *
 * <p>위험 영역:
 * <ul>
 *   <li>빈 컬렉션 연산</li>
 *   <li>null 처리</li>
 *   <li>경계값 처리</li>
 *   <li>특수 문자 인코딩</li>
 * </ul>
 *
 * <p>테스트 케이스:
 * <ul>
 *   <li>E-1: 빈 컬렉션 연산</li>
 *   <li>E-2: 단일 요소 컬렉션</li>
 *   <li>E-3: 최대 키/값 크기</li>
 *   <li>E-4: 특수 문자 처리</li>
 *   <li>E-5: null 키 처리</li>
 * </ul>
 */
public class EdgeCaseIntegrationTest extends IntegrationTestBase {

    /**
     * E-1: 빈 컬렉션 연산
     *
     * <p>목적: 빈 컬렉션에서의 모든 연산 안전성
     * <p>위험 영역: 빈 상태 처리
     * <p>검증: 예외 없이 graceful 처리
     */
    @Test
    public void test_E1_emptyCollection_shouldHandleGracefully() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("emptyMap", Long.class, String.class);
        store.commit();

        // Then: 빈 컬렉션 연산
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get(1L));
        assertNull(map.firstEntry());
        assertNull(map.lastEntry());
        assertNull(map.pollFirstEntry());
        assertNull(map.pollLastEntry());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());

        // And: 빈 subMap
        NavigableMap<Long, String> sub = map.subMap(0L, true, 100L, true);
        assertTrue(sub.isEmpty());

        // And: 빈 iterator
        assertFalse(map.entrySet().iterator().hasNext());
    }

    /**
     * E-2: 단일 요소 컬렉션
     *
     * <p>목적: 단일 요소 컬렉션의 모든 연산
     * <p>위험 영역: 단일 노드 트리 처리
     * <p>검증: 모든 연산 정확
     */
    @Test
    public void test_E2_singleElement_shouldWork() throws Exception {
        openStore();

        // List
        List<String> list = store.createList("singleList", String.class);
        list.add("only");
        store.commit();

        assertEquals(1, list.size());
        assertEquals("only", list.get(0));
        assertEquals("only", list.remove(0));
        assertTrue(list.isEmpty());
        store.commit();

        // Set
        NavigableSet<Long> set = store.createSet("singleSet", Long.class);
        set.add(42L);
        store.commit();

        assertEquals(1, set.size());
        assertEquals(Long.valueOf(42L), set.first());
        assertEquals(Long.valueOf(42L), set.last());
        assertTrue(set.contains(42L));
        assertTrue(set.remove(42L));
        assertTrue(set.isEmpty());
        store.commit();

        // Deque
        Deque<String> deque = store.createDeque("singleDeque", String.class);
        deque.addFirst("one");
        store.commit();

        assertEquals(1, deque.size());
        assertEquals("one", deque.peekFirst());
        assertEquals("one", deque.peekLast());
        assertEquals("one", deque.pollFirst());
        assertTrue(deque.isEmpty());
    }

    /**
     * E-3: 최대 키/값 크기
     *
     * <p>목적: 큰 키/값 처리 능력
     * <p>위험 영역: 페이지 경계
     * <p>검증: 큰 데이터 저장/복원
     */
    @Test
    public void test_E3_largeKeyValue_shouldHandleWithinLimit() throws Exception {
        openStore();
        NavigableMap<String, String> map = store.createMap("largeKV", String.class, String.class);

        // 큰 키/값 (500B, 1KB)
        String largeKey = TestDataGenerator.largeString(500);
        String largeValue = TestDataGenerator.largeString(1000);

        map.put(largeKey, largeValue);
        store.commit();

        assertEquals(largeValue, map.get(largeKey));

        // 재시작 후 확인
        reopenStore();
        map = store.openMap("largeKV", String.class, String.class);
        assertEquals(largeValue, map.get(largeKey));
    }

    /**
     * E-4: 특수 문자 처리
     *
     * <p>목적: 유니코드, 제어 문자 등 올바른 처리
     * <p>위험 영역: 인코딩/디코딩
     * <p>검증: 특수 문자 보존
     */
    @Test
    public void test_E4_specialCharacters_shouldPreserve() throws Exception {
        openStore();
        NavigableMap<String, String> map = store.createMap("specialChar", String.class, String.class);

        // 유니코드, 이모지, 제어 문자
        String[] specialKeys = TestDataGenerator.specialStrings();

        for (int i = 0; i < specialKeys.length; i++) {
            String key = specialKeys[i];
            if (key != null && !key.isEmpty()) {  // null과 빈 문자열 제외
                map.put(key, "value:" + i);
            }
        }
        store.commit();

        reopenStore();
        map = store.openMap("specialChar", String.class, String.class);

        for (int i = 0; i < specialKeys.length; i++) {
            String key = specialKeys[i];
            if (key != null && !key.isEmpty()) {
                assertEquals("value:" + i, map.get(key));
            }
        }
    }

    /**
     * E-5: null 키 처리 (예외 발생해야 함)
     *
     * <p>목적: null 키 삽입 시 예외 처리
     * <p>위험 영역: null 검증
     * <p>검증: FxException 발생
     */
    @Test
    public void test_E5_nullKey_shouldThrow() throws Exception {
        openStore();
        NavigableMap<String, String> map = store.createMap("nullTest", String.class, String.class);

        try {
            map.put(null, "value");
            fail("Should throw exception for null key");
        } catch (FxException e) {
            // 예상대로 예외 발생
        } catch (NullPointerException e) {
            // NullPointerException도 허용
        }
    }

    /**
     * 추가 테스트: 경계값 Long 테스트
     *
     * <p>참고: Long.MIN_VALUE/MAX_VALUE의 정렬 순서는 구현에 따라 다를 수 있음
     */
    @Test
    public void test_E_boundaryLongs_shouldWork() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("boundaryLong", Long.class, String.class);

        Long[] boundaries = TestDataGenerator.boundaryLongs();
        for (Long value : boundaries) {
            map.put(value, "v" + value);
        }
        store.commit();

        // 모든 경계값이 저장되고 조회 가능한지 확인
        for (Long value : boundaries) {
            assertEquals("v" + value, map.get(value));
        }

        // 크기 확인
        assertEquals(boundaries.length, map.size());

        // 재시작 후 확인
        reopenStore();
        map = store.openMap("boundaryLong", Long.class, String.class);
        assertEquals(boundaries.length, map.size());

        // 재시작 후에도 모든 값 조회 가능
        for (Long value : boundaries) {
            assertEquals("v" + value, map.get(value));
        }
    }

    /**
     * 추가 테스트: List 인덱스 경계
     */
    @Test
    public void test_E_listIndexBoundary_shouldThrow() throws Exception {
        openStore();
        List<String> list = store.createList("indexBoundary", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        store.commit();

        // 정상 접근
        assertEquals("a", list.get(0));
        assertEquals("c", list.get(2));

        // 경계 초과
        try {
            list.get(-1);
            fail("Should throw for negative index");
        } catch (IndexOutOfBoundsException e) {
            // 예상대로
        }

        try {
            list.get(3);
            fail("Should throw for index >= size");
        } catch (IndexOutOfBoundsException e) {
            // 예상대로
        }
    }

    /**
     * 추가 테스트: Deque 양쪽 끝 연산
     */
    @Test
    public void test_E_dequeBothEnds_shouldWork() throws Exception {
        openStore();
        Deque<Long> deque = store.createDeque("bothEnds", Long.class);

        // 양쪽 끝 삽입
        deque.addFirst(1L);
        deque.addLast(2L);
        deque.addFirst(0L);
        deque.addLast(3L);
        store.commit();

        // 순서: 0, 1, 2, 3
        assertEquals(4, deque.size());
        assertEquals(Long.valueOf(0L), deque.peekFirst());
        assertEquals(Long.valueOf(3L), deque.peekLast());

        // 양쪽 끝 제거
        assertEquals(Long.valueOf(0L), deque.pollFirst());
        assertEquals(Long.valueOf(3L), deque.pollLast());
        assertEquals(2, deque.size());
        store.commit();

        // 재시작 후 확인
        reopenStore();
        deque = store.openDeque("bothEnds", Long.class);
        assertEquals(2, deque.size());
        assertEquals(Long.valueOf(1L), deque.peekFirst());
        assertEquals(Long.valueOf(2L), deque.peekLast());
    }

    /**
     * 추가 테스트: Set 중복 삽입
     */
    @Test
    public void test_E_setDuplicate_shouldIgnore() throws Exception {
        openStore();
        NavigableSet<Long> set = store.createSet("duplicate", Long.class);

        // 중복 삽입
        assertTrue(set.add(1L));
        assertFalse(set.add(1L));  // 중복은 false
        assertTrue(set.add(2L));
        assertFalse(set.add(2L));
        store.commit();

        assertEquals(2, set.size());

        // 재시작 후 확인
        reopenStore();
        set = store.openSet("duplicate", Long.class);
        assertEquals(2, set.size());
    }

    /**
     * 추가 테스트: Map 덮어쓰기
     */
    @Test
    public void test_E_mapOverwrite_shouldReplace() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("overwrite", Long.class, String.class);

        map.put(1L, "first");
        store.commit();

        map.put(1L, "second");
        store.commit();

        assertEquals(1, map.size());
        assertEquals("second", map.get(1L));

        // 재시작 후 확인
        reopenStore();
        map = store.openMap("overwrite", Long.class, String.class);
        assertEquals(1, map.size());
        assertEquals("second", map.get(1L));
    }
}
