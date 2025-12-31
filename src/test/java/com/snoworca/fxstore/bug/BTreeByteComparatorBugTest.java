package com.snoworca.fxstore.bug;

import com.snoworca.fxstore.integration.IntegrationTestBase;
import org.junit.Test;

import java.util.Iterator;
import java.util.NavigableMap;

import static org.junit.Assert.*;

/**
 * BUG-V12-001: BTree 바이트 비교자 버그 재현 테스트
 *
 * <p>문제: BTree의 byteComparator가 Unsigned byte 비교를 사용하여
 * Signed Long 값을 잘못 정렬함.
 *
 * <p>근본 원인:
 * <ul>
 *   <li>FxStoreImpl.getBTreeForCollection()의 byteComparator</li>
 *   <li>FxReadTransactionImpl.createBTree()의 byteComparator</li>
 *   <li>Little-Endian 인코딩 + Unsigned byte 비교 = 음수 정렬 오류</li>
 * </ul>
 *
 * <p>영향:
 * <ul>
 *   <li>lastKey()가 잘못된 값 반환</li>
 *   <li>firstKey()가 음수 대신 양수 반환</li>
 *   <li>subMap() 범위가 잘못됨</li>
 *   <li>ceiling/floor/higher/lower 등 탐색 오류</li>
 * </ul>
 *
 * @see <a href="docs/plan/BUG-FIX-BTREE-COMPARATOR-V12.md">버그 수정 계획</a>
 */
public class BTreeByteComparatorBugTest extends IntegrationTestBase {

    /**
     * BUG-V12-001-1: lastKey()가 65535를 반환하는 버그
     *
     * <p>증상: 100K 데이터에서 lastKey()가 99999 대신 65535 반환
     * <p>원인: Unsigned byte 비교로 인해 큰 값들이 잘못된 위치에 저장
     */
    @Test
    public void test_bug_lastKey_returns65535_shouldReturn99999() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug1", Long.class, String.class);

        // 100K 요소 삽입
        for (int i = 0; i < 100_000; i++) {
            map.put((long) i, "v" + i);
        }
        store.commit();

        // 크기 검증
        assertEquals(100_000, map.size());

        // lastKey 검증
        // 버그 수정 전: 65535 반환 (2^16 - 1)
        // 버그 수정 후: 99999 반환
        assertEquals("lastKey should be 99999", Long.valueOf(99999L), map.lastKey());
    }

    /**
     * BUG-V12-001-2: firstKey()가 음수 대신 0을 반환하는 버그
     *
     * <p>증상: Long.MIN_VALUE 포함 데이터에서 firstKey()가 0 반환
     * <p>원인: 음수의 MSB(0x80~0xFF)가 unsigned로 해석되어 큰 값으로 취급
     */
    @Test
    public void test_bug_firstKey_shouldReturnMinValue() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug2", Long.class, String.class);

        // 경계값 포함 데이터 삽입
        map.put(Long.MIN_VALUE, "min");
        map.put(-1L, "neg");
        map.put(0L, "zero");
        map.put(1L, "pos");
        map.put(Long.MAX_VALUE, "max");
        store.commit();

        assertEquals(5, map.size());

        // firstKey 검증
        // 버그 수정 전: 0 반환 (양수가 먼저 정렬됨)
        // 버그 수정 후: Long.MIN_VALUE 반환
        assertEquals("firstKey should be Long.MIN_VALUE",
                Long.valueOf(Long.MIN_VALUE), map.firstKey());

        // lastKey 검증
        assertEquals("lastKey should be Long.MAX_VALUE",
                Long.valueOf(Long.MAX_VALUE), map.lastKey());
    }

    /**
     * BUG-V12-001-3: subMap() 범위가 잘못되는 버그
     *
     * <p>증상: subMap(10000, 10010)이 11개 대신 3911개 반환
     * <p>원인: BTreeCursor의 범위 검사가 잘못된 비교자 사용
     */
    @Test
    public void test_bug_subMap_shouldReturn11Elements() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug3", Long.class, String.class);

        // 100K 요소 삽입
        for (int i = 0; i < 100_000; i++) {
            map.put((long) i, "v" + i);
        }
        store.commit();

        // subMap 검증
        NavigableMap<Long, String> sub = map.subMap(10000L, true, 10010L, true);

        // 버그 수정 전: 3911 반환
        // 버그 수정 후: 11 반환
        assertEquals("subMap(10000, 10010) should have 11 elements", 11, sub.size());

        // 포함된 키 검증
        for (long i = 10000L; i <= 10010L; i++) {
            assertTrue("subMap should contain key " + i, sub.containsKey(i));
        }
    }

    /**
     * BUG-V12-001-4: Iterator 정렬 순서 버그
     *
     * <p>증상: keySet() 순회 시 signed 순서가 아닌 unsigned 순서
     * <p>원인: BTree 내부 구조가 unsigned 순서로 구성됨
     */
    @Test
    public void test_bug_sortOrder_shouldBeSigned() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug4", Long.class, String.class);

        // 양수/음수 혼합 데이터
        Long[] values = {Long.MIN_VALUE, -1000L, -1L, 0L, 1L, 1000L, Long.MAX_VALUE};
        for (Long v : values) {
            map.put(v, "v" + v);
        }
        store.commit();

        assertEquals(values.length, map.size());

        // Iterator 순서 검증 (signed 순서여야 함)
        Iterator<Long> it = map.keySet().iterator();
        Long prev = it.next();
        int count = 1;
        while (it.hasNext()) {
            Long curr = it.next();
            assertTrue("Keys should be in signed order: " + prev + " < " + curr,
                    prev < curr);
            prev = curr;
            count++;
        }
        assertEquals("All keys should be iterated", values.length, count);
    }

    /**
     * BUG-V12-001-5: ceiling/floor 탐색 버그
     *
     * <p>증상: 음수 범위에서 ceiling/floor가 잘못된 결과 반환
     * <p>원인: BTree 내부 탐색이 잘못된 비교자 사용
     */
    @Test
    public void test_bug_ceilingFloor_shouldWork() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug5", Long.class, String.class);

        map.put(-100L, "a");
        map.put(0L, "b");
        map.put(100L, "c");
        store.commit();

        // ceiling 검증
        assertEquals("ceiling(-150) should be -100",
                Long.valueOf(-100L), map.ceilingKey(-150L));
        assertEquals("ceiling(-50) should be 0",
                Long.valueOf(0L), map.ceilingKey(-50L));
        assertEquals("ceiling(50) should be 100",
                Long.valueOf(100L), map.ceilingKey(50L));

        // floor 검증
        assertEquals("floor(-50) should be -100",
                Long.valueOf(-100L), map.floorKey(-50L));
        assertEquals("floor(50) should be 0",
                Long.valueOf(0L), map.floorKey(50L));
        assertEquals("floor(150) should be 100",
                Long.valueOf(100L), map.floorKey(150L));
    }

    /**
     * BUG-V12-001-6: higher/lower 탐색 버그
     *
     * <p>증상: 음수 범위에서 higher/lower가 잘못된 결과 반환
     */
    @Test
    public void test_bug_higherLower_shouldWork() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug6", Long.class, String.class);

        map.put(-100L, "a");
        map.put(0L, "b");
        map.put(100L, "c");
        store.commit();

        // higher 검증
        assertEquals("higher(-100) should be 0",
                Long.valueOf(0L), map.higherKey(-100L));
        assertEquals("higher(0) should be 100",
                Long.valueOf(100L), map.higherKey(0L));
        assertNull("higher(100) should be null",
                map.higherKey(100L));

        // lower 검증
        assertNull("lower(-100) should be null",
                map.lowerKey(-100L));
        assertEquals("lower(0) should be -100",
                Long.valueOf(-100L), map.lowerKey(0L));
        assertEquals("lower(100) should be 0",
                Long.valueOf(0L), map.lowerKey(100L));
    }

    /**
     * BUG-V12-001-7: headMap/tailMap 범위 버그
     *
     * <p>증상: 음수 포함 데이터에서 headMap/tailMap 범위 오류
     */
    @Test
    public void test_bug_headTailMap_shouldWork() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug7", Long.class, String.class);

        for (long i = -50; i <= 50; i++) {
            map.put(i, "v" + i);
        }
        store.commit();

        assertEquals(101, map.size());

        // headMap 검증 (toKey 미만)
        NavigableMap<Long, String> head = map.headMap(0L, false);
        assertEquals("headMap(0, false) should have 50 elements (−50 to −1)",
                50, head.size());
        assertEquals(Long.valueOf(-50L), head.firstKey());
        assertEquals(Long.valueOf(-1L), head.lastKey());

        // tailMap 검증 (fromKey 이상)
        NavigableMap<Long, String> tail = map.tailMap(0L, true);
        assertEquals("tailMap(0, true) should have 51 elements (0 to 50)",
                51, tail.size());
        assertEquals(Long.valueOf(0L), tail.firstKey());
        assertEquals(Long.valueOf(50L), tail.lastKey());
    }

    /**
     * BUG-V12-001-8: 재시작 후 정렬 순서 유지 검증
     *
     * <p>버그 수정 후에도 재시작 시 올바른 순서 유지되어야 함
     */
    @Test
    public void test_bug_afterRestart_shouldMaintainOrder() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug8", Long.class, String.class);

        Long[] values = {Long.MIN_VALUE, -1000L, 0L, 1000L, Long.MAX_VALUE};
        for (Long v : values) {
            map.put(v, "v" + v);
        }
        store.commit();

        // 재시작
        reopenStore();
        map = store.openMap("bug8", Long.class, String.class);

        // 순서 검증
        assertEquals(Long.valueOf(Long.MIN_VALUE), map.firstKey());
        assertEquals(Long.valueOf(Long.MAX_VALUE), map.lastKey());

        // Iterator 순서 검증
        Iterator<Long> it = map.keySet().iterator();
        Long prev = it.next();
        while (it.hasNext()) {
            Long curr = it.next();
            assertTrue("After restart, keys should be in signed order: " + prev + " < " + curr,
                    prev < curr);
            prev = curr;
        }
    }
}
