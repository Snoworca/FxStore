package com.snoworca.fxstore.btree;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * BTree 기반 NavigableMap의 범위 쿼리 테스트
 *
 * <p>P1-4: subMap, headMap, tailMap, floor/ceiling/higher/lower 메서드 테스트</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>subMap - 범위 추출</li>
 *   <li>headMap - 상한 범위</li>
 *   <li>tailMap - 하한 범위</li>
 *   <li>floor/ceiling/higher/lower Entry/Key 메서드</li>
 *   <li>first/last Entry/Key 메서드</li>
 *   <li>descendingMap</li>
 * </ul>
 *
 * @since v1.0 Phase 2
 */
public class BTreeRangeQueryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;
    private NavigableMap<Long, String> map;

    @Before
    public void setUp() throws Exception {
        File storeFile = tempFolder.newFile("test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
        map = store.createMap("test", Long.class, String.class);

        // 테스트 데이터 삽입: 10, 20, 30, 40, 50
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");
        map.put(40L, "forty");
        map.put(50L, "fifty");
    }

    @After
    public void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    // ==================== subMap 테스트 ====================

    @Test
    public void subMap_inclusive_shouldContainBothBounds() {
        // Given: 20~40 범위 (양쪽 포함)
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);

        // Then
        assertEquals(3, sub.size());
        assertTrue(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
        assertTrue(sub.containsKey(40L));
    }

    @Test
    public void subMap_exclusive_shouldExcludeBounds() {
        // Given: 20~40 범위 (양쪽 제외)
        NavigableMap<Long, String> sub = map.subMap(20L, false, 40L, false);

        // Then
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey(30L));
        assertFalse(sub.containsKey(20L));
        assertFalse(sub.containsKey(40L));
    }

    @Test
    public void subMap_mixed_fromInclusive() {
        // Given: 20(포함)~40(제외)
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, false);

        // Then
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
        assertFalse(sub.containsKey(40L));
    }

    @Test
    public void subMap_mixed_toInclusive() {
        // Given: 20(제외)~40(포함)
        NavigableMap<Long, String> sub = map.subMap(20L, false, 40L, true);

        // Then
        assertEquals(2, sub.size());
        assertFalse(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
        assertTrue(sub.containsKey(40L));
    }

    @Test
    public void subMap_singleElement() {
        // Given: 30~30 범위 (포함)
        NavigableMap<Long, String> sub = map.subMap(30L, true, 30L, true);

        // Then
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey(30L));
    }

    @Test
    public void subMap_empty() {
        // Given: 25~35 범위 (둘 다 맵에 없음, 30만 있음)
        NavigableMap<Long, String> sub = map.subMap(25L, true, 35L, true);

        // Then
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey(30L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_invalidRange_shouldThrow() {
        // from > to
        map.subMap(40L, true, 20L, true);
    }

    @Test
    public void subMap_sortedMapOverload() {
        // SortedMap 오버로드 테스트 (fromInclusive=true, toInclusive=false)
        SortedMap<Long, String> sub = map.subMap(20L, 40L);

        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
        assertFalse(sub.containsKey(40L));
    }

    // ==================== headMap 테스트 ====================

    @Test
    public void headMap_inclusive_shouldContainBound() {
        // Given: ~30 (포함)
        NavigableMap<Long, String> head = map.headMap(30L, true);

        // Then
        assertEquals(3, head.size());
        assertTrue(head.containsKey(10L));
        assertTrue(head.containsKey(20L));
        assertTrue(head.containsKey(30L));
        assertFalse(head.containsKey(40L));
    }

    @Test
    public void headMap_exclusive_shouldExcludeBound() {
        // Given: ~30 (제외)
        NavigableMap<Long, String> head = map.headMap(30L, false);

        // Then
        assertEquals(2, head.size());
        assertTrue(head.containsKey(10L));
        assertTrue(head.containsKey(20L));
        assertFalse(head.containsKey(30L));
    }

    @Test
    public void headMap_allElements() {
        // Given: ~60 (모든 요소 포함)
        NavigableMap<Long, String> head = map.headMap(60L, true);

        // Then
        assertEquals(5, head.size());
    }

    @Test
    public void headMap_noElements() {
        // Given: ~5 (요소 없음)
        NavigableMap<Long, String> head = map.headMap(5L, true);

        // Then
        assertTrue(head.isEmpty());
    }

    @Test
    public void headMap_sortedMapOverload() {
        // SortedMap 오버로드 (inclusive=false)
        SortedMap<Long, String> head = map.headMap(30L);

        assertEquals(2, head.size());
        assertFalse(head.containsKey(30L));
    }

    // ==================== tailMap 테스트 ====================

    @Test
    public void tailMap_inclusive_shouldContainBound() {
        // Given: 30~ (포함)
        NavigableMap<Long, String> tail = map.tailMap(30L, true);

        // Then
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey(30L));
        assertTrue(tail.containsKey(40L));
        assertTrue(tail.containsKey(50L));
        assertFalse(tail.containsKey(20L));
    }

    @Test
    public void tailMap_exclusive_shouldExcludeBound() {
        // Given: 30~ (제외)
        NavigableMap<Long, String> tail = map.tailMap(30L, false);

        // Then
        assertEquals(2, tail.size());
        assertFalse(tail.containsKey(30L));
        assertTrue(tail.containsKey(40L));
        assertTrue(tail.containsKey(50L));
    }

    @Test
    public void tailMap_allElements() {
        // Given: 5~ (모든 요소 포함)
        NavigableMap<Long, String> tail = map.tailMap(5L, true);

        // Then
        assertEquals(5, tail.size());
    }

    @Test
    public void tailMap_noElements() {
        // Given: 60~ (요소 없음)
        NavigableMap<Long, String> tail = map.tailMap(60L, true);

        // Then
        assertTrue(tail.isEmpty());
    }

    @Test
    public void tailMap_sortedMapOverload() {
        // SortedMap 오버로드 (inclusive=true)
        SortedMap<Long, String> tail = map.tailMap(30L);

        assertEquals(3, tail.size());
        assertTrue(tail.containsKey(30L));
    }

    // ==================== floorEntry/floorKey 테스트 ====================

    @Test
    public void floorEntry_exactMatch() {
        Map.Entry<Long, String> entry = map.floorEntry(30L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
        assertEquals("thirty", entry.getValue());
    }

    @Test
    public void floorEntry_noExactMatch() {
        Map.Entry<Long, String> entry = map.floorEntry(25L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());
    }

    @Test
    public void floorEntry_belowMin() {
        Map.Entry<Long, String> entry = map.floorEntry(5L);
        assertNull(entry);
    }

    @Test
    public void floorKey_exactMatch() {
        Long key = map.floorKey(30L);
        assertEquals(Long.valueOf(30L), key);
    }

    @Test
    public void floorKey_noExactMatch() {
        Long key = map.floorKey(35L);
        assertEquals(Long.valueOf(30L), key);
    }

    @Test
    public void floorKey_belowMin() {
        Long key = map.floorKey(5L);
        assertNull(key);
    }

    // ==================== ceilingEntry/ceilingKey 테스트 ====================

    @Test
    public void ceilingEntry_exactMatch() {
        Map.Entry<Long, String> entry = map.ceilingEntry(30L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
        assertEquals("thirty", entry.getValue());
    }

    @Test
    public void ceilingEntry_noExactMatch() {
        Map.Entry<Long, String> entry = map.ceilingEntry(25L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
    }

    @Test
    public void ceilingEntry_aboveMax() {
        Map.Entry<Long, String> entry = map.ceilingEntry(55L);
        assertNull(entry);
    }

    @Test
    public void ceilingKey_exactMatch() {
        Long key = map.ceilingKey(30L);
        assertEquals(Long.valueOf(30L), key);
    }

    @Test
    public void ceilingKey_noExactMatch() {
        Long key = map.ceilingKey(35L);
        assertEquals(Long.valueOf(40L), key);
    }

    @Test
    public void ceilingKey_aboveMax() {
        Long key = map.ceilingKey(55L);
        assertNull(key);
    }

    // ==================== higherEntry/higherKey 테스트 ====================

    @Test
    public void higherEntry_exactMatch() {
        // higher는 정확히 매치하는 키보다 큰 것 반환
        Map.Entry<Long, String> entry = map.higherEntry(30L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(40L), entry.getKey());
    }

    @Test
    public void higherEntry_betweenKeys() {
        Map.Entry<Long, String> entry = map.higherEntry(25L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
    }

    @Test
    public void higherEntry_atMax() {
        Map.Entry<Long, String> entry = map.higherEntry(50L);
        assertNull(entry);
    }

    @Test
    public void higherKey_exactMatch() {
        Long key = map.higherKey(30L);
        assertEquals(Long.valueOf(40L), key);
    }

    @Test
    public void higherKey_betweenKeys() {
        Long key = map.higherKey(35L);
        assertEquals(Long.valueOf(40L), key);
    }

    @Test
    public void higherKey_atMax() {
        Long key = map.higherKey(50L);
        assertNull(key);
    }

    // ==================== lowerEntry/lowerKey 테스트 ====================

    @Test
    public void lowerEntry_exactMatch() {
        // lower는 정확히 매치하는 키보다 작은 것 반환
        Map.Entry<Long, String> entry = map.lowerEntry(30L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());
    }

    @Test
    public void lowerEntry_betweenKeys() {
        Map.Entry<Long, String> entry = map.lowerEntry(35L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
    }

    @Test
    public void lowerEntry_atMin() {
        Map.Entry<Long, String> entry = map.lowerEntry(10L);
        assertNull(entry);
    }

    @Test
    public void lowerKey_exactMatch() {
        Long key = map.lowerKey(30L);
        assertEquals(Long.valueOf(20L), key);
    }

    @Test
    public void lowerKey_betweenKeys() {
        Long key = map.lowerKey(25L);
        assertEquals(Long.valueOf(20L), key);
    }

    @Test
    public void lowerKey_atMin() {
        Long key = map.lowerKey(10L);
        assertNull(key);
    }

    // ==================== firstEntry/firstKey 테스트 ====================

    @Test
    public void firstEntry_shouldReturnSmallest() {
        Map.Entry<Long, String> entry = map.firstEntry();
        assertNotNull(entry);
        assertEquals(Long.valueOf(10L), entry.getKey());
        assertEquals("ten", entry.getValue());
    }

    @Test
    public void firstKey_shouldReturnSmallest() {
        Long key = map.firstKey();
        assertEquals(Long.valueOf(10L), key);
    }

    @Test(expected = NoSuchElementException.class)
    public void firstKey_emptyMap_shouldThrow() {
        NavigableMap<Long, String> emptyMap = store.createMap("empty", Long.class, String.class);
        emptyMap.firstKey();
    }

    // ==================== lastEntry/lastKey 테스트 ====================

    @Test
    public void lastEntry_shouldReturnLargest() {
        Map.Entry<Long, String> entry = map.lastEntry();
        assertNotNull(entry);
        assertEquals(Long.valueOf(50L), entry.getKey());
        assertEquals("fifty", entry.getValue());
    }

    @Test
    public void lastKey_shouldReturnLargest() {
        Long key = map.lastKey();
        assertEquals(Long.valueOf(50L), key);
    }

    @Test(expected = NoSuchElementException.class)
    public void lastKey_emptyMap_shouldThrow() {
        NavigableMap<Long, String> emptyMap = store.createMap("empty2", Long.class, String.class);
        emptyMap.lastKey();
    }

    // ==================== pollFirstEntry/pollLastEntry 테스트 ====================

    @Test
    public void pollFirstEntry_shouldRemoveAndReturn() {
        Map.Entry<Long, String> entry = map.pollFirstEntry();
        assertNotNull(entry);
        assertEquals(Long.valueOf(10L), entry.getKey());
        assertEquals("ten", entry.getValue());

        // 삭제 확인
        assertNull(map.get(10L));
        assertEquals(4, map.size());
    }

    @Test
    public void pollLastEntry_shouldRemoveAndReturn() {
        Map.Entry<Long, String> entry = map.pollLastEntry();
        assertNotNull(entry);
        assertEquals(Long.valueOf(50L), entry.getKey());
        assertEquals("fifty", entry.getValue());

        // 삭제 확인
        assertNull(map.get(50L));
        assertEquals(4, map.size());
    }

    @Test
    public void pollFirstEntry_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> emptyMap = store.createMap("empty3", Long.class, String.class);
        assertNull(emptyMap.pollFirstEntry());
    }

    @Test
    public void pollLastEntry_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> emptyMap = store.createMap("empty4", Long.class, String.class);
        assertNull(emptyMap.pollLastEntry());
    }

    // ==================== descendingMap 테스트 ====================

    @Test
    public void descendingMap_shouldReverseOrder() {
        NavigableMap<Long, String> desc = map.descendingMap();

        // 첫 번째 = 원래 맵의 마지막
        assertEquals(Long.valueOf(50L), desc.firstKey());
        // 마지막 = 원래 맵의 첫 번째
        assertEquals(Long.valueOf(10L), desc.lastKey());
    }

    @Test
    public void descendingMap_iteration() {
        NavigableMap<Long, String> desc = map.descendingMap();
        List<Long> keys = new ArrayList<>(desc.keySet());

        assertEquals(5, keys.size());
        assertEquals(Long.valueOf(50L), keys.get(0));
        assertEquals(Long.valueOf(40L), keys.get(1));
        assertEquals(Long.valueOf(30L), keys.get(2));
        assertEquals(Long.valueOf(20L), keys.get(3));
        assertEquals(Long.valueOf(10L), keys.get(4));
    }

    @Test
    public void descendingMap_higherLower_reversed() {
        NavigableMap<Long, String> desc = map.descendingMap();

        // desc에서 higher(30) = original에서 lower(30) = 20
        assertEquals(Long.valueOf(20L), desc.higherKey(30L));
        // desc에서 lower(30) = original에서 higher(30) = 40
        assertEquals(Long.valueOf(40L), desc.lowerKey(30L));
    }

    // ==================== navigableKeySet 테스트 ====================

    @Test
    public void navigableKeySet_shouldContainAllKeys() {
        NavigableSet<Long> keySet = map.navigableKeySet();

        assertEquals(5, keySet.size());
        assertTrue(keySet.contains(10L));
        assertTrue(keySet.contains(50L));
    }

    @Test
    public void navigableKeySet_first_last() {
        NavigableSet<Long> keySet = map.navigableKeySet();

        assertEquals(Long.valueOf(10L), keySet.first());
        assertEquals(Long.valueOf(50L), keySet.last());
    }

    @Test
    public void navigableKeySet_floor_ceiling() {
        NavigableSet<Long> keySet = map.navigableKeySet();

        assertEquals(Long.valueOf(20L), keySet.floor(25L));
        assertEquals(Long.valueOf(30L), keySet.ceiling(25L));
    }

    // ==================== descendingKeySet 테스트 ====================

    @Test
    public void descendingKeySet_shouldReverseOrder() {
        NavigableSet<Long> descKeySet = map.descendingKeySet();

        assertEquals(Long.valueOf(50L), descKeySet.first());
        assertEquals(Long.valueOf(10L), descKeySet.last());
    }

    @Test
    public void descendingKeySet_iteration() {
        NavigableSet<Long> descKeySet = map.descendingKeySet();
        List<Long> keys = new ArrayList<>();
        for (Long key : descKeySet) {
            keys.add(key);
        }

        assertEquals(5, keys.size());
        assertEquals(Long.valueOf(50L), keys.get(0));
        assertEquals(Long.valueOf(10L), keys.get(4));
    }

    // ==================== 뷰 수정 테스트 ====================

    @Test
    public void subMap_put_shouldAffectParent() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        sub.put(25L, "twenty-five");

        // parent map도 영향 받음
        assertEquals("twenty-five", map.get(25L));
        assertEquals(6, map.size());
    }

    @Test
    public void subMap_remove_shouldAffectParent() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        sub.remove(30L);

        // parent map도 영향 받음
        assertNull(map.get(30L));
        assertEquals(4, map.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_put_outOfRange_shouldThrow() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        sub.put(50L, "out"); // 범위 밖
    }

    @Test
    public void headMap_put_shouldAffectParent() {
        NavigableMap<Long, String> head = map.headMap(30L, true);
        head.put(15L, "fifteen");

        assertEquals("fifteen", map.get(15L));
    }

    @Test
    public void tailMap_put_shouldAffectParent() {
        NavigableMap<Long, String> tail = map.tailMap(30L, true);
        tail.put(35L, "thirty-five");

        assertEquals("thirty-five", map.get(35L));
    }

    // ==================== 빈 맵 범위 쿼리 테스트 ====================

    @Test
    public void rangeQueries_emptyMap() {
        NavigableMap<Long, String> emptyMap = store.createMap("emptyRange", Long.class, String.class);

        assertTrue(emptyMap.subMap(10L, true, 50L, true).isEmpty());
        assertTrue(emptyMap.headMap(30L, true).isEmpty());
        assertTrue(emptyMap.tailMap(30L, true).isEmpty());

        assertNull(emptyMap.floorEntry(30L));
        assertNull(emptyMap.ceilingEntry(30L));
        assertNull(emptyMap.higherEntry(30L));
        assertNull(emptyMap.lowerEntry(30L));
    }

    // ==================== null 파라미터 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void floorKey_null_shouldThrow() {
        map.floorKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void ceilingKey_null_shouldThrow() {
        map.ceilingKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void higherKey_null_shouldThrow() {
        map.higherKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void lowerKey_null_shouldThrow() {
        map.lowerKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void subMap_nullFrom_shouldThrow() {
        map.subMap(null, true, 30L, true);
    }

    @Test(expected = NullPointerException.class)
    public void subMap_nullTo_shouldThrow() {
        map.subMap(10L, true, null, true);
    }

    // ==================== 복잡한 범위 조합 테스트 ====================

    @Test
    public void complexRange_subMapOfSubMap() {
        NavigableMap<Long, String> sub1 = map.subMap(10L, true, 50L, true);
        NavigableMap<Long, String> sub2 = sub1.subMap(20L, true, 40L, true);

        assertEquals(3, sub2.size());
        assertTrue(sub2.containsKey(20L));
        assertTrue(sub2.containsKey(30L));
        assertTrue(sub2.containsKey(40L));
    }

    @Test
    public void complexRange_tailMapHeadMap() {
        // tailMap 후 headMap
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        NavigableMap<Long, String> both = tail.headMap(40L, true);

        assertEquals(3, both.size());
        assertTrue(both.containsKey(20L));
        assertTrue(both.containsKey(30L));
        assertTrue(both.containsKey(40L));
    }

    @Test
    public void complexRange_headMapTailMap() {
        // headMap 후 tailMap
        NavigableMap<Long, String> head = map.headMap(40L, true);
        NavigableMap<Long, String> both = head.tailMap(20L, true);

        assertEquals(3, both.size());
        assertTrue(both.containsKey(20L));
        assertTrue(both.containsKey(30L));
        assertTrue(both.containsKey(40L));
    }
}
