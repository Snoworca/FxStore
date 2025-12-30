package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

/**
 * View 클래스 커버리지 향상 테스트
 *
 * <p>목표: 85% → 95% 커버리지 달성</p>
 *
 * <p>테스트 대상:</p>
 * <ul>
 *   <li>DescendingSubSetView (16% → 90%+)</li>
 *   <li>DescendingHeadSetView (16% → 90%+)</li>
 *   <li>DescendingTailSetView (16% → 90%+)</li>
 *   <li>SortedSet/SortedMap 1-arg 메서드</li>
 *   <li>범위 예외 처리</li>
 *   <li>HeadMapView/TailMapView put 연산</li>
 * </ul>
 */
public class ViewCoverageBoostTest {

    private File tempFile;
    private FxStore store;
    private NavigableSet<Long> set;
    private NavigableMap<Long, String> map;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-coverage-boost-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());

        set = store.createSet("testSet", Long.class);
        map = store.createMap("testMap", Long.class, String.class);

        // 기본 데이터: {10, 20, 30, 40, 50}
        for (long i = 10; i <= 50; i += 10) {
            set.add(i);
            map.put(i, "V" + i);
        }
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

    // ==================== DescendingSubSetView 테스트 ====================

    @Test
    public void subSet_descendingSet_size() {
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        NavigableSet<Long> desc = sub.descendingSet();
        assertEquals(3, desc.size());
    }

    @Test
    public void subSet_descendingSet_contains() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        assertTrue(desc.contains(30L));
        assertFalse(desc.contains(10L));
    }

    @Test
    public void subSet_descendingSet_iterator() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        List<Long> result = new ArrayList<>();
        for (Long e : desc) {
            result.add(e);
        }
        assertEquals(Arrays.asList(40L, 30L, 20L), result);
    }

    @Test
    public void subSet_descendingSet_descendingIterator() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        List<Long> result = new ArrayList<>();
        Iterator<Long> it = desc.descendingIterator();
        while (it.hasNext()) {
            result.add(it.next());
        }
        assertEquals(Arrays.asList(20L, 30L, 40L), result);
    }

    @Test
    public void subSet_descendingSet_add() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        assertTrue(desc.add(25L));
        assertTrue(set.contains(25L));
    }

    @Test
    public void subSet_descendingSet_remove() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        assertTrue(desc.remove(30L));
        assertFalse(set.contains(30L));
    }

    @Test
    public void subSet_descendingSet_clear() {
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        NavigableSet<Long> desc = sub.descendingSet();
        desc.clear();
        assertTrue(sub.isEmpty());
        assertEquals(2, set.size()); // 10, 50 remain
    }

    @Test
    public void subSet_descendingSet_comparator() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        Comparator<? super Long> cmp = desc.comparator();
        assertNotNull(cmp);
        assertTrue(cmp.compare(20L, 30L) > 0); // reversed
    }

    @Test
    public void subSet_descendingSet_first_last() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        assertEquals(Long.valueOf(40L), desc.first());
        assertEquals(Long.valueOf(20L), desc.last());
    }

    @Test
    public void subSet_descendingSet_lower_higher() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        assertEquals(Long.valueOf(40L), desc.lower(30L)); // desc lower = parent higher
        assertEquals(Long.valueOf(20L), desc.higher(30L)); // desc higher = parent lower
    }

    @Test
    public void subSet_descendingSet_floor_ceiling() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        assertEquals(Long.valueOf(30L), desc.floor(30L));
        assertEquals(Long.valueOf(30L), desc.ceiling(30L));
    }

    @Test
    public void subSet_descendingSet_pollFirst_pollLast() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        assertEquals(Long.valueOf(40L), desc.pollFirst()); // desc pollFirst = parent pollLast
        assertEquals(Long.valueOf(20L), desc.pollLast());  // desc pollLast = parent pollFirst
    }

    @Test
    public void subSet_descendingSet_descendingSet() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        NavigableSet<Long> original = desc.descendingSet();
        assertEquals(Long.valueOf(20L), original.first());
    }

    @Test
    public void subSet_descendingSet_subSet() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        NavigableSet<Long> sub = desc.subSet(40L, true, 20L, true);
        assertEquals(3, sub.size());
    }

    @Test
    public void subSet_descendingSet_headSet() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        NavigableSet<Long> head = desc.headSet(30L, true);
        assertEquals(2, head.size()); // 40, 30
    }

    @Test
    public void subSet_descendingSet_tailSet() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        NavigableSet<Long> tail = desc.tailSet(30L, true);
        assertEquals(2, tail.size()); // 30, 20
    }

    @Test
    public void subSet_descendingSet_sortedSubSet() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        SortedSet<Long> sub = desc.subSet(40L, 20L);
        assertEquals(2, sub.size()); // 40, 30 (exclusive end)
    }

    @Test
    public void subSet_descendingSet_sortedHeadSet() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        SortedSet<Long> head = desc.headSet(30L);
        assertEquals(1, head.size()); // 40
    }

    @Test
    public void subSet_descendingSet_sortedTailSet() {
        NavigableSet<Long> desc = set.subSet(20L, true, 40L, true).descendingSet();
        SortedSet<Long> tail = desc.tailSet(30L);
        assertEquals(2, tail.size()); // 30, 20
    }

    // ==================== DescendingHeadSetView 테스트 ====================

    @Test
    public void headSet_descendingSet_size() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        assertEquals(4, desc.size());
    }

    @Test
    public void headSet_descendingSet_iterator() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        List<Long> result = new ArrayList<>();
        for (Long e : desc) {
            result.add(e);
        }
        assertEquals(Arrays.asList(40L, 30L, 20L, 10L), result);
    }

    @Test
    public void headSet_descendingSet_descendingIterator() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        List<Long> result = new ArrayList<>();
        Iterator<Long> it = desc.descendingIterator();
        while (it.hasNext()) {
            result.add(it.next());
        }
        assertEquals(Arrays.asList(10L, 20L, 30L, 40L), result);
    }

    @Test
    public void headSet_descendingSet_add() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        assertTrue(desc.add(15L));
        assertTrue(set.contains(15L));
    }

    @Test
    public void headSet_descendingSet_remove() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        assertTrue(desc.remove(20L));
        assertFalse(set.contains(20L));
    }

    @Test
    public void headSet_descendingSet_clear() {
        NavigableSet<Long> head = set.headSet(40L, true);
        NavigableSet<Long> desc = head.descendingSet();
        desc.clear();
        assertEquals(1, set.size()); // only 50 remains
    }

    @Test
    public void headSet_descendingSet_comparator() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        Comparator<? super Long> cmp = desc.comparator();
        assertTrue(cmp.compare(10L, 20L) > 0);
    }

    @Test
    public void headSet_descendingSet_first_last() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        assertEquals(Long.valueOf(40L), desc.first());
        assertEquals(Long.valueOf(10L), desc.last());
    }

    @Test
    public void headSet_descendingSet_lower_higher_floor_ceiling() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        assertEquals(Long.valueOf(40L), desc.lower(30L));
        assertEquals(Long.valueOf(20L), desc.higher(30L));
        assertEquals(Long.valueOf(30L), desc.floor(30L));
        assertEquals(Long.valueOf(30L), desc.ceiling(30L));
    }

    @Test
    public void headSet_descendingSet_pollFirst_pollLast() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        assertEquals(Long.valueOf(40L), desc.pollFirst());
        assertEquals(Long.valueOf(10L), desc.pollLast());
    }

    @Test
    public void headSet_descendingSet_subSet_headSet_tailSet() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        // desc = {40, 30, 20, 10} in descending order
        // subSet(40, true, 20, true) = {40, 30, 20} = 3 elements
        assertEquals(3, desc.subSet(40L, true, 20L, true).size());
        // headSet(30, true) in desc = elements "before" 30 in desc order = {40, 30} = 2 elements
        assertEquals(2, desc.headSet(30L, true).size());
        // tailSet(30, true) in desc = elements "after or equal to" 30 in desc order = {30, 20, 10} = 3 elements
        assertEquals(3, desc.tailSet(30L, true).size());
    }

    @Test
    public void headSet_descendingSet_sorted_methods() {
        NavigableSet<Long> desc = set.headSet(40L, true).descendingSet();
        assertEquals(2, desc.subSet(40L, 20L).size());
        assertEquals(1, desc.headSet(30L).size());
        assertEquals(3, desc.tailSet(30L).size());
    }

    // ==================== DescendingTailSetView 테스트 ====================

    @Test
    public void tailSet_descendingSet_size() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        assertEquals(4, desc.size());
    }

    @Test
    public void tailSet_descendingSet_iterator() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        List<Long> result = new ArrayList<>();
        for (Long e : desc) {
            result.add(e);
        }
        assertEquals(Arrays.asList(50L, 40L, 30L, 20L), result);
    }

    @Test
    public void tailSet_descendingSet_descendingIterator() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        List<Long> result = new ArrayList<>();
        Iterator<Long> it = desc.descendingIterator();
        while (it.hasNext()) {
            result.add(it.next());
        }
        assertEquals(Arrays.asList(20L, 30L, 40L, 50L), result);
    }

    @Test
    public void tailSet_descendingSet_add() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        assertTrue(desc.add(25L));
        assertTrue(set.contains(25L));
    }

    @Test
    public void tailSet_descendingSet_remove() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        assertTrue(desc.remove(30L));
        assertFalse(set.contains(30L));
    }

    @Test
    public void tailSet_descendingSet_clear() {
        NavigableSet<Long> tail = set.tailSet(20L, true);
        NavigableSet<Long> desc = tail.descendingSet();
        desc.clear();
        assertEquals(1, set.size()); // only 10 remains
    }

    @Test
    public void tailSet_descendingSet_comparator() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        Comparator<? super Long> cmp = desc.comparator();
        assertTrue(cmp.compare(20L, 30L) > 0);
    }

    @Test
    public void tailSet_descendingSet_first_last() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        assertEquals(Long.valueOf(50L), desc.first());
        assertEquals(Long.valueOf(20L), desc.last());
    }

    @Test
    public void tailSet_descendingSet_lower_higher_floor_ceiling() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        assertEquals(Long.valueOf(40L), desc.lower(30L));
        assertEquals(Long.valueOf(20L), desc.higher(30L));
        assertEquals(Long.valueOf(30L), desc.floor(30L));
        assertEquals(Long.valueOf(30L), desc.ceiling(30L));
    }

    @Test
    public void tailSet_descendingSet_pollFirst_pollLast() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        assertEquals(Long.valueOf(50L), desc.pollFirst());
        assertEquals(Long.valueOf(20L), desc.pollLast());
    }

    @Test
    public void tailSet_descendingSet_subSet_headSet_tailSet() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        // desc = {50, 40, 30, 20} in descending order
        // subSet(50, true, 30, true) = {50, 40, 30} = 3 elements
        assertEquals(3, desc.subSet(50L, true, 30L, true).size());
        // headSet(40, true) = {50, 40} = 2 elements
        assertEquals(2, desc.headSet(40L, true).size());
        // tailSet(40, true) = {40, 30, 20} = 3 elements
        assertEquals(3, desc.tailSet(40L, true).size());
    }

    @Test
    public void tailSet_descendingSet_sorted_methods() {
        NavigableSet<Long> desc = set.tailSet(20L, true).descendingSet();
        assertEquals(2, desc.subSet(50L, 30L).size());
        assertEquals(1, desc.headSet(40L).size());
        assertEquals(3, desc.tailSet(40L).size());
    }

    // ==================== SortedSet 1-arg 메서드 테스트 ====================

    @Test
    public void subSet_sorted_1arg() {
        NavigableSet<Long> sub = set.subSet(20L, true, 50L, true);
        SortedSet<Long> sorted = sub.subSet(25L, 45L);
        assertEquals(2, sorted.size()); // 30, 40
    }

    @Test
    public void headSet_sorted_1arg() {
        NavigableSet<Long> head = set.headSet(40L, true);
        SortedSet<Long> sorted = head.headSet(30L);
        assertEquals(2, sorted.size()); // 10, 20
    }

    @Test
    public void tailSet_sorted_1arg() {
        NavigableSet<Long> tail = set.tailSet(20L, true);
        SortedSet<Long> sorted = tail.tailSet(30L);
        assertEquals(3, sorted.size()); // 30, 40, 50
    }

    // ==================== SortedMap 1-arg 메서드 테스트 ====================

    @Test
    public void subMap_sorted_1arg() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 50L, true);
        SortedMap<Long, String> sorted = sub.subMap(25L, 45L);
        assertEquals(2, sorted.size());
    }

    @Test
    public void headMap_sorted_1arg() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        SortedMap<Long, String> sorted = head.headMap(30L);
        assertEquals(2, sorted.size());
    }

    @Test
    public void tailMap_sorted_1arg() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        SortedMap<Long, String> sorted = tail.tailMap(30L);
        assertEquals(3, sorted.size());
    }

    // ==================== HeadMapView/TailMapView put 테스트 ====================

    @Test
    public void headMap_put_inRange() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        head.put(15L, "V15");
        assertTrue(map.containsKey(15L));
        assertEquals("V15", map.get(15L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void headMap_put_outOfRange() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        head.put(60L, "V60"); // out of range
    }

    @Test
    public void tailMap_put_inRange() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        tail.put(25L, "V25");
        assertTrue(map.containsKey(25L));
        assertEquals("V25", map.get(25L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tailMap_put_outOfRange() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        tail.put(5L, "V5"); // out of range
    }

    // ==================== 범위 예외 테스트 (Set) ====================

    @Test(expected = IllegalArgumentException.class)
    public void subSet_add_outOfRange_low() {
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        sub.add(10L); // out of range
    }

    @Test(expected = IllegalArgumentException.class)
    public void subSet_add_outOfRange_high() {
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        sub.add(50L); // out of range
    }

    @Test(expected = IllegalArgumentException.class)
    public void headSet_add_outOfRange() {
        NavigableSet<Long> head = set.headSet(40L, true);
        head.add(50L); // out of range
    }

    @Test(expected = IllegalArgumentException.class)
    public void tailSet_add_outOfRange() {
        NavigableSet<Long> tail = set.tailSet(20L, true);
        tail.add(10L); // out of range
    }

    // ==================== SubSetView lowerKey/floorKey/ceilingKey/higherKey 테스트 ====================

    @Test
    public void subMap_lowerKey() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertEquals(Long.valueOf(20L), sub.lowerKey(30L));
        assertNull(sub.lowerKey(20L));
    }

    @Test
    public void subMap_floorKey() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertEquals(Long.valueOf(30L), sub.floorKey(30L));
        assertEquals(Long.valueOf(30L), sub.floorKey(35L));
    }

    @Test
    public void subMap_ceilingKey() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertEquals(Long.valueOf(30L), sub.ceilingKey(30L));
        assertEquals(Long.valueOf(30L), sub.ceilingKey(25L));
    }

    @Test
    public void subMap_higherKey() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertEquals(Long.valueOf(40L), sub.higherKey(30L));
        assertNull(sub.higherKey(40L));
    }

    // ==================== 빈 뷰에서 NoSuchElementException 테스트 ====================

    @Test(expected = NoSuchElementException.class)
    public void emptySubSet_first_shouldThrow() {
        NavigableSet<Long> sub = set.subSet(100L, true, 200L, true);
        sub.first();
    }

    @Test(expected = NoSuchElementException.class)
    public void emptySubSet_last_shouldThrow() {
        NavigableSet<Long> sub = set.subSet(100L, true, 200L, true);
        sub.last();
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyHeadSet_first_shouldThrow() {
        NavigableSet<Long> head = set.headSet(5L, true);
        head.first();
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyHeadSet_last_shouldThrow() {
        NavigableSet<Long> head = set.headSet(5L, true);
        head.last();
    }

    // ==================== DescendingSetView clear 테스트 ====================

    @Test
    public void descendingSet_clear() {
        NavigableSet<Long> desc = set.descendingSet();
        desc.clear();
        assertTrue(set.isEmpty());
    }

    // ==================== SubMapView 중첩 범위 검증 테스트 ====================

    @Test(expected = IllegalArgumentException.class)
    public void subMap_nested_subMap_outOfRange_low() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        sub.subMap(10L, true, 30L, true); // 10 is out of parent range
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_nested_subMap_outOfRange_high() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        sub.subMap(25L, true, 50L, true); // 50 is out of parent range
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_nested_headMap_outOfRange() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        sub.headMap(50L, true); // 50 is out of parent range
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_nested_tailMap_outOfRange() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        sub.tailMap(10L, true); // 10 is out of parent range
    }

    @Test(expected = IllegalArgumentException.class)
    public void headMap_nested_tailMap_outOfRange() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        head.tailMap(50L, true); // 50 is out of parent range
    }

    @Test(expected = IllegalArgumentException.class)
    public void tailMap_nested_headMap_outOfRange() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        tail.headMap(10L, true); // 10 is out of parent range
    }

    // ==================== DescendingKeySetView subSet/headSet/tailSet 테스트 ====================

    @Test
    public void descendingKeySet_subSet() {
        NavigableSet<Long> descKeys = map.descendingKeySet();
        NavigableSet<Long> sub = descKeys.subSet(40L, true, 20L, true);
        assertEquals(3, sub.size());
    }

    @Test
    public void descendingKeySet_headSet() {
        NavigableSet<Long> descKeys = map.descendingKeySet();
        NavigableSet<Long> head = descKeys.headSet(30L, true);
        assertEquals(3, head.size()); // 50, 40, 30
    }

    @Test
    public void descendingKeySet_tailSet() {
        NavigableSet<Long> descKeys = map.descendingKeySet();
        NavigableSet<Long> tail = descKeys.tailSet(30L, true);
        assertEquals(3, tail.size()); // 30, 20, 10
    }

    // ==================== remove 범위 밖 요소 테스트 ====================

    @Test
    public void subSet_remove_outOfRange_returnsFalse() {
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        assertFalse(sub.remove(10L)); // out of range, returns false
        assertFalse(sub.remove(50L)); // out of range, returns false
    }

    @Test
    public void headSet_remove_outOfRange_returnsFalse() {
        NavigableSet<Long> head = set.headSet(40L, true);
        assertFalse(head.remove(50L)); // out of range
    }

    @Test
    public void tailSet_remove_outOfRange_returnsFalse() {
        NavigableSet<Long> tail = set.tailSet(20L, true);
        assertFalse(tail.remove(10L)); // out of range
    }

    // ==================== 빈 뷰 poll 테스트 ====================

    @Test
    public void emptySubSet_poll_returnsNull() {
        NavigableSet<Long> sub = set.subSet(100L, true, 200L, true);
        assertNull(sub.pollFirst());
        assertNull(sub.pollLast());
    }

    @Test
    public void emptyHeadSet_poll_returnsNull() {
        NavigableSet<Long> head = set.headSet(5L, true);
        assertNull(head.pollFirst());
        assertNull(head.pollLast());
    }

    @Test
    public void emptyTailSet_poll_returnsNull() {
        NavigableSet<Long> tail = set.tailSet(100L, true);
        assertNull(tail.pollFirst());
        assertNull(tail.pollLast());
    }

    // ==================== lower/floor/ceiling/higher null 반환 테스트 ====================

    @Test
    public void subSet_lower_returnsNull_atBoundary() {
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        assertNull(sub.lower(20L)); // no element < 20 in range
    }

    @Test
    public void subSet_higher_returnsNull_atBoundary() {
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        assertNull(sub.higher(40L)); // no element > 40 in range
    }

    @Test
    public void subSet_floor_returnsNull_belowRange() {
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        assertNull(sub.floor(15L)); // 15 < 20, nothing to floor
    }

    @Test
    public void subSet_ceiling_returnsNull_aboveRange() {
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        assertNull(sub.ceiling(45L)); // 45 > 40, nothing to ceil
    }

    // ==================== TailMapView remove 테스트 ====================

    @Test
    public void tailMap_remove_inRange() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertEquals("V30", tail.remove(30L));
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void tailMap_remove_outOfRange() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertNull(tail.remove(10L)); // out of range
        assertTrue(map.containsKey(10L)); // 원본에는 여전히 존재
    }

    @Test
    public void tailMap_remove_notExist() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertNull(tail.remove(25L)); // doesn't exist
    }

    // ==================== TailMapView SortedMap 1-arg 메서드 테스트 ====================

    @Test
    public void tailMap_subMap_sorted_1arg() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        SortedMap<Long, String> sub = tail.subMap(25L, 45L);
        assertEquals(2, sub.size()); // 30, 40
    }

    @Test
    public void tailMap_headMap_sorted_1arg() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        SortedMap<Long, String> head = tail.headMap(40L);
        assertEquals(2, head.size()); // 20, 30
    }

    // ==================== HeadMapView SortedMap 1-arg 메서드 테스트 ====================

    @Test
    public void headMap_subMap_sorted_1arg() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        SortedMap<Long, String> sub = head.subMap(15L, 35L);
        assertEquals(2, sub.size()); // 20, 30
    }

    @Test
    public void headMap_tailMap_sorted_1arg() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        SortedMap<Long, String> tail = head.tailMap(20L);
        assertEquals(3, tail.size()); // 20, 30, 40
    }

    // ==================== DescendingMapView SortedMap 1-arg 메서드 테스트 ====================

    @Test
    public void descendingMap_subMap_sorted_1arg() {
        NavigableMap<Long, String> desc = map.descendingMap();
        SortedMap<Long, String> sub = desc.subMap(40L, 20L);
        assertEquals(2, sub.size()); // 40, 30 (exclusive end)
    }

    @Test
    public void descendingMap_headMap_sorted_1arg() {
        NavigableMap<Long, String> desc = map.descendingMap();
        SortedMap<Long, String> head = desc.headMap(30L);
        assertEquals(2, head.size()); // 50, 40
    }

    @Test
    public void descendingMap_tailMap_sorted_1arg() {
        NavigableMap<Long, String> desc = map.descendingMap();
        SortedMap<Long, String> tail = desc.tailMap(30L);
        assertEquals(3, tail.size()); // 30, 20, 10
    }

    // ==================== TailMapView 브랜치 커버리지 ====================

    @Test
    public void tailMap_ceilingEntry_outOfRange() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        // 범위 내에 있지만 아래쪽 경계
        Map.Entry<Long, String> entry = tail.ceilingEntry(15L);
        assertEquals(Long.valueOf(20L), entry.getKey()); // 20이 ceiling
    }

    @Test
    public void tailMap_ceilingEntry_aboveAll() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertNull(tail.ceilingEntry(60L)); // 모든 요소보다 큼
    }

    @Test
    public void tailMap_higherEntry_boundary() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertNull(tail.higherEntry(50L)); // 50보다 큰 요소 없음
    }

    @Test
    public void tailMap_lowerKey_returnNull() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertNull(tail.lowerKey(20L)); // 20보다 작은 요소 없음
    }

    @Test
    public void tailMap_floorKey_returnNull() {
        NavigableMap<Long, String> tail = map.tailMap(30L, true);
        assertNull(tail.floorKey(25L)); // 25 이하 요소 없음
    }

    @Test
    public void tailMap_ceilingKey_returnNull() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertNull(tail.ceilingKey(60L)); // 60 이상 요소 없음
    }

    @Test
    public void tailMap_higherKey_returnNull() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertNull(tail.higherKey(50L)); // 50보다 큰 요소 없음
    }

    @Test
    public void tailMap_compare_withComparator() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        // compare 메서드는 내부적으로 사용됨 - inRange 체크를 통해 테스트
        assertTrue(tail.containsKey(30L));
        assertFalse(tail.containsKey(10L)); // out of range
    }

    // ==================== HeadMapView 브랜치 커버리지 ====================

    @Test
    public void headMap_floorEntry_outOfRange() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        Map.Entry<Long, String> entry = head.floorEntry(50L);
        assertEquals(Long.valueOf(40L), entry.getKey()); // 40이 floor
    }

    @Test
    public void headMap_floorEntry_belowAll() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertNull(head.floorEntry(5L)); // 모든 요소보다 작음
    }

    @Test
    public void headMap_lowerKey_returnNull() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertNull(head.lowerKey(10L)); // 10보다 작은 요소 없음
    }

    @Test
    public void headMap_floorKey_returnNull() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertNull(head.floorKey(5L)); // 5 이하 요소 없음
    }

    @Test
    public void headMap_ceilingKey_returnNull() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertNull(head.ceilingKey(50L)); // 50 이상 요소 없음
    }

    @Test
    public void headMap_higherKey_returnNull() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertNull(head.higherKey(40L)); // 40보다 큰 요소 없음
    }

    @Test
    public void headMap_remove_inRange() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertEquals("V30", head.remove(30L));
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void headMap_remove_outOfRange() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertNull(head.remove(50L)); // out of range
        assertTrue(map.containsKey(50L));
    }

    // ==================== pollFirstEntry/pollLastEntry null 브랜치 ====================

    @Test
    public void tailMap_pollFirstEntry_empty() {
        NavigableMap<Long, String> tail = map.tailMap(100L, true);
        assertNull(tail.pollFirstEntry()); // empty view
    }

    @Test
    public void tailMap_pollLastEntry_empty() {
        NavigableMap<Long, String> tail = map.tailMap(100L, true);
        assertNull(tail.pollLastEntry()); // empty view
    }

    @Test
    public void headMap_pollFirstEntry_empty() {
        NavigableMap<Long, String> head = map.headMap(5L, true);
        assertNull(head.pollFirstEntry()); // empty view
    }

    @Test
    public void headMap_pollLastEntry_empty() {
        NavigableMap<Long, String> head = map.headMap(5L, true);
        assertNull(head.pollLastEntry()); // empty view
    }

    // ==================== TailMapView tailMap/subMap 브랜치 커버리지 ====================

    @Test
    public void tailMap_nested_tailMap_inclusive_exclusive() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        // inclusive=true 테스트
        NavigableMap<Long, String> inner1 = tail.tailMap(30L, true);
        assertEquals(3, inner1.size()); // 30, 40, 50

        // inclusive=false 테스트
        NavigableMap<Long, String> inner2 = tail.tailMap(30L, false);
        assertEquals(2, inner2.size()); // 40, 50
    }

    @Test
    public void tailMap_nested_subMap_various_bounds() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        // 다양한 경계 조합
        NavigableMap<Long, String> sub1 = tail.subMap(25L, true, 45L, true);
        assertEquals(2, sub1.size()); // 30, 40

        NavigableMap<Long, String> sub2 = tail.subMap(30L, false, 50L, false);
        assertEquals(1, sub2.size()); // 40
    }

    // ==================== HeadMapView headMap/subMap 브랜치 커버리지 ====================

    @Test
    public void headMap_nested_headMap_inclusive_exclusive() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        // inclusive=true 테스트
        NavigableMap<Long, String> inner1 = head.headMap(30L, true);
        assertEquals(3, inner1.size()); // 10, 20, 30

        // inclusive=false 테스트
        NavigableMap<Long, String> inner2 = head.headMap(30L, false);
        assertEquals(2, inner2.size()); // 10, 20
    }

    @Test
    public void headMap_nested_subMap_various_bounds() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        // 다양한 경계 조합
        NavigableMap<Long, String> sub1 = head.subMap(15L, true, 35L, true);
        assertEquals(2, sub1.size()); // 20, 30

        NavigableMap<Long, String> sub2 = head.subMap(20L, false, 40L, false);
        assertEquals(1, sub2.size()); // 30
    }

    // ==================== headMap/tailMap tailMap 조합 테스트 ====================

    @Test
    public void headMap_tailMap_outOfBounds_exclusive() {
        NavigableMap<Long, String> head = map.headMap(40L, false);
        // 경계가 exclusive일 때
        NavigableMap<Long, String> tail = head.tailMap(10L, true);
        assertEquals(3, tail.size()); // 10, 20, 30
    }

    @Test
    public void tailMap_headMap_outOfBounds_exclusive() {
        NavigableMap<Long, String> tail = map.tailMap(20L, false);
        // 경계가 exclusive일 때
        NavigableMap<Long, String> head = tail.headMap(50L, true);
        assertEquals(3, head.size()); // 30, 40, 50
    }

    // ==================== lowerEntry/floorEntry 결과 null 브랜치 ====================

    @Test
    public void tailMap_lowerEntry_belowFromKey() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        // fromKey보다 작은 키 요청
        assertNull(tail.lowerEntry(20L)); // 20보다 작은 요소는 범위 밖
    }

    @Test
    public void tailMap_floorEntry_belowFromKey() {
        NavigableMap<Long, String> tail = map.tailMap(30L, true);
        // fromKey보다 작은 키로 floor 요청
        assertNull(tail.floorEntry(25L)); // 25 이하 중 범위 내 없음
    }

    @Test
    public void headMap_ceilingEntry_aboveToKey() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        // toKey보다 큰 키 요청
        assertNull(head.ceilingEntry(50L)); // 50 이상은 범위 밖
    }

    @Test
    public void headMap_higherEntry_aboveToKey() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        // toKey 이상 요청
        assertNull(head.higherEntry(40L)); // 40보다 큰 요소 없음
    }
}
