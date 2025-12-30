package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.SortedSet;

import static org.junit.Assert.*;

/**
 * DescendingSetView 커버리지 테스트 (P3)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>FxNavigableSetImpl.DescendingSetView (84% → 90%+)</li>
 * </ul>
 *
 * @since 0.9
 * @see FxNavigableSetImpl
 */
public class DescendingSetViewTest {

    private File tempFile;
    private FxStore store;
    private NavigableSet<Long> set;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-descset-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());
        set = store.createSet("testSet", Long.class);

        // 기본 데이터: {10, 20, 30, 40, 50}
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);
        set.add(50L);
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

    // ==================== 기본 동작 테스트 ====================

    @Test
    public void descendingSet_size_shouldBeSameAsOriginal() {
        NavigableSet<Long> desc = set.descendingSet();
        assertEquals(5, desc.size());
    }

    @Test
    public void descendingSet_contains_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        assertTrue(desc.contains(30L));
        assertFalse(desc.contains(100L));
    }

    @Test
    public void descendingSet_isEmpty_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        assertFalse(desc.isEmpty());

        set.clear();
        assertTrue(desc.isEmpty());
    }

    // ==================== first/last 테스트 (역순) ====================

    @Test
    public void descendingSet_first_shouldReturnLargestFromOriginal() {
        NavigableSet<Long> desc = set.descendingSet();
        assertEquals(Long.valueOf(50L), desc.first());
    }

    @Test
    public void descendingSet_last_shouldReturnSmallestFromOriginal() {
        NavigableSet<Long> desc = set.descendingSet();
        assertEquals(Long.valueOf(10L), desc.last());
    }

    // ==================== lower/floor/ceiling/higher (역순) ====================

    @Test
    public void descendingSet_lower_shouldBeReversed() {
        NavigableSet<Long> desc = set.descendingSet();
        // descending에서 lower(30)은 원래 higher(30) = 40
        assertEquals(Long.valueOf(40L), desc.lower(30L));
    }

    @Test
    public void descendingSet_higher_shouldBeReversed() {
        NavigableSet<Long> desc = set.descendingSet();
        // descending에서 higher(30)은 원래 lower(30) = 20
        assertEquals(Long.valueOf(20L), desc.higher(30L));
    }

    @Test
    public void descendingSet_floor_shouldBeReversed() {
        NavigableSet<Long> desc = set.descendingSet();
        // descending에서 floor(35)은 원래 ceiling(35) = 40
        assertEquals(Long.valueOf(40L), desc.floor(35L));
    }

    @Test
    public void descendingSet_ceiling_shouldBeReversed() {
        NavigableSet<Long> desc = set.descendingSet();
        // descending에서 ceiling(35)은 원래 floor(35) = 30
        assertEquals(Long.valueOf(30L), desc.ceiling(35L));
    }

    @Test
    public void descendingSet_lower_atBoundary_shouldReturnNull() {
        NavigableSet<Long> desc = set.descendingSet();
        // descending에서 lower(50)은 원래 higher(50) = null
        assertNull(desc.lower(50L));
    }

    @Test
    public void descendingSet_higher_atBoundary_shouldReturnNull() {
        NavigableSet<Long> desc = set.descendingSet();
        // descending에서 higher(10)은 원래 lower(10) = null
        assertNull(desc.higher(10L));
    }

    // ==================== iterator 테스트 ====================

    @Test
    public void descendingSet_iterator_shouldReturnReverseOrder() {
        NavigableSet<Long> desc = set.descendingSet();
        Iterator<Long> it = desc.iterator();

        List<Long> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }

        assertEquals(Arrays.asList(50L, 40L, 30L, 20L, 10L), result);
    }

    @Test
    public void descendingSet_descendingIterator_shouldReturnOriginalOrder() {
        NavigableSet<Long> desc = set.descendingSet();
        Iterator<Long> it = desc.descendingIterator();

        List<Long> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }

        assertEquals(Arrays.asList(10L, 20L, 30L, 40L, 50L), result);
    }

    // ==================== descendingSet().descendingSet() 테스트 ====================

    @Test
    public void descendingSet_twice_shouldRestoreOriginalOrder() {
        NavigableSet<Long> desc1 = set.descendingSet();
        NavigableSet<Long> desc2 = desc1.descendingSet();

        assertEquals(Long.valueOf(10L), desc2.first());
        assertEquals(Long.valueOf(50L), desc2.last());
    }

    @Test
    public void descendingSet_twice_iterator_shouldMatchOriginal() {
        NavigableSet<Long> desc2 = set.descendingSet().descendingSet();
        Iterator<Long> it = desc2.iterator();

        List<Long> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }

        assertEquals(Arrays.asList(10L, 20L, 30L, 40L, 50L), result);
    }

    // ==================== comparator 테스트 ====================

    @Test
    public void descendingSet_comparator_shouldBeReversed() {
        NavigableSet<Long> desc = set.descendingSet();
        Comparator<? super Long> cmp = desc.comparator();

        // reversed comparator: 10 > 20 in descending order
        assertTrue(cmp.compare(10L, 20L) > 0);
        assertTrue(cmp.compare(50L, 30L) < 0);
    }

    // ==================== pollFirst/pollLast 테스트 (UOE 개선) ====================

    @Test
    public void descendingSet_pollFirst_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        Long first = desc.pollFirst();
        assertEquals(Long.valueOf(50L), first);
        assertFalse(set.contains(50L));
    }

    @Test
    public void descendingSet_pollLast_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        Long last = desc.pollLast();
        assertEquals(Long.valueOf(10L), last);
        assertFalse(set.contains(10L));
    }

    // ==================== subSet/headSet/tailSet 테스트 (UOE 개선) ====================

    @Test
    public void descendingSet_subSet_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        // desc에서 40 > 20이므로 40~20 범위
        NavigableSet<Long> sub = desc.subSet(40L, true, 20L, true);

        assertEquals(3, sub.size());
        assertTrue(sub.contains(40L));
        assertTrue(sub.contains(30L));
        assertTrue(sub.contains(20L));
    }

    @Test
    public void descendingSet_headSet_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        // desc.headSet(30) = desc에서 30보다 "작은" 것들 = 원래 순서로 30보다 큰 것들 = {50, 40}
        NavigableSet<Long> head = desc.headSet(30L, false);

        assertEquals(2, head.size());
        assertTrue(head.contains(50L));
        assertTrue(head.contains(40L));
    }

    @Test
    public void descendingSet_tailSet_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        // desc.tailSet(30) = desc에서 30보다 "큰" 것들 = 원래 순서로 30보다 작은 것들 = {30, 20, 10}
        NavigableSet<Long> tail = desc.tailSet(30L, true);

        assertEquals(3, tail.size());
        assertTrue(tail.contains(30L));
        assertTrue(tail.contains(20L));
        assertTrue(tail.contains(10L));
    }

    @Test
    public void descendingSet_subSet_sorted_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        // desc.subSet(40, 20) = subSet(40, true, 20, false)
        SortedSet<Long> sub = desc.subSet(40L, 20L);

        assertEquals(2, sub.size());
        assertTrue(sub.contains(40L));
        assertTrue(sub.contains(30L));
    }

    @Test
    public void descendingSet_headSet_sorted_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        SortedSet<Long> head = desc.headSet(30L);

        assertEquals(2, head.size());
        assertTrue(head.contains(50L));
        assertTrue(head.contains(40L));
    }

    @Test
    public void descendingSet_tailSet_sorted_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        SortedSet<Long> tail = desc.tailSet(30L);

        assertEquals(3, tail.size());
        assertTrue(tail.contains(30L));
        assertTrue(tail.contains(20L));
        assertTrue(tail.contains(10L));
    }

    // ==================== toArray 테스트 ====================

    @Test
    public void descendingSet_toArray_shouldReturnReverseOrder() {
        NavigableSet<Long> desc = set.descendingSet();
        Object[] arr = desc.toArray();

        assertEquals(5, arr.length);
        assertEquals(50L, arr[0]);
        assertEquals(10L, arr[4]);
    }

    @Test
    public void descendingSet_toArray_typed_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        Long[] arr = desc.toArray(new Long[0]);

        assertEquals(5, arr.length);
        assertEquals(Long.valueOf(50L), arr[0]);
        assertEquals(Long.valueOf(10L), arr[4]);
    }

    // ==================== 빈 집합 테스트 ====================

    @Test
    public void descendingSet_empty_shouldWork() {
        NavigableSet<Long> emptySet = store.createSet("emptySet", Long.class);
        NavigableSet<Long> desc = emptySet.descendingSet();

        assertTrue(desc.isEmpty());
        assertEquals(0, desc.size());
    }

    @Test(expected = NoSuchElementException.class)
    public void descendingSet_empty_first_shouldThrow() {
        NavigableSet<Long> emptySet = store.createSet("emptySet2", Long.class);
        NavigableSet<Long> desc = emptySet.descendingSet();
        desc.first();
    }

    @Test(expected = NoSuchElementException.class)
    public void descendingSet_empty_last_shouldThrow() {
        NavigableSet<Long> emptySet = store.createSet("emptySet3", Long.class);
        NavigableSet<Long> desc = emptySet.descendingSet();
        desc.last();
    }
}
