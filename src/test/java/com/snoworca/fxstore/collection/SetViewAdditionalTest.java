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
 * NavigableSet View 추가 커버리지 테스트 (P2)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>FxNavigableSetImpl.SubSetView (66% → 85%+)</li>
 *   <li>FxNavigableSetImpl.DescendingSetView (72% → 85%+)</li>
 * </ul>
 *
 * @since 0.8
 * @see FxNavigableSetImpl
 */
public class SetViewAdditionalTest {

    private File tempFile;
    private FxStore store;
    private NavigableSet<Long> set;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-setview-", ".db").toFile();
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

    // ==================== SubSetView 테스트 (12개) ====================

    @Test
    public void subSet_iterator_shouldIterateInRange() {
        // Given: subSet(15, 45) - should include {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // When: iterate
        List<Long> elements = new ArrayList<>();
        for (Long e : sub) {
            elements.add(e);
        }

        // Then: [20, 30, 40]
        assertEquals(Arrays.asList(20L, 30L, 40L), elements);
    }

    @Test
    public void subSet_inclusiveExclusive_shouldRespectBounds() {
        // Given: subSet(20, true, 40, false)
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, false);

        // Then: contains 20, 30 but not 40
        assertTrue(sub.contains(20L));
        assertTrue(sub.contains(30L));
        assertFalse(sub.contains(40L));
        assertEquals(2, sub.size());
    }

    @Test
    public void subSet_lower_shouldReturnLowerInRange() {
        // Given: subSet(15, 45) = {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // When: lower(30)
        Long result = sub.lower(30L);

        // Then: 20
        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void subSet_floor_shouldReturnFloorInRange() {
        // Given: subSet(15, 45) = {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // When: floor(25)
        Long result = sub.floor(25L);

        // Then: 20
        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void subSet_ceiling_shouldReturnCeilingInRange() {
        // Given: subSet(15, 45) = {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // When: ceiling(25)
        Long result = sub.ceiling(25L);

        // Then: 30
        assertEquals(Long.valueOf(30L), result);
    }

    @Test
    public void subSet_higher_shouldReturnHigherInRange() {
        // Given: subSet(15, 45) = {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // When: higher(25)
        Long result = sub.higher(25L);

        // Then: 30
        assertEquals(Long.valueOf(30L), result);
    }

    @Test
    public void subSet_empty_shouldReturnEmptyView() {
        // Given: subSet(15, 18) - no elements in this range
        NavigableSet<Long> sub = set.subSet(15L, true, 18L, false);

        // Then: empty
        assertTrue(sub.isEmpty());
        assertEquals(0, sub.size());
    }

    @Test
    public void subSet_lower_atBoundary_shouldReturnNull() {
        // Given: subSet(15, 45) = {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // When: lower(20) - no element < 20 in range
        Long result = sub.lower(20L);

        // Then: null (no lower in range)
        assertNull(result);
    }

    @Test
    public void subSet_first_last_shouldReturnBoundaryElements() {
        // Given: subSet(15, 45) = {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // Then
        assertEquals(Long.valueOf(20L), sub.first());
        assertEquals(Long.valueOf(40L), sub.last());
    }

    @Test
    public void subSet_contains_shouldCheckRange() {
        // Given: subSet(15, 45) = {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // Then
        assertFalse(sub.contains(10L)); // 범위 밖
        assertTrue(sub.contains(30L));  // 범위 내
        assertFalse(sub.contains(50L)); // 범위 밖
    }

    @Test
    public void subSet_descendingIterator_shouldIterateReverse() {
        // Given: subSet(15, 45) = {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // When: descendingIterator
        List<Long> elements = new ArrayList<>();
        Iterator<Long> it = sub.descendingIterator();
        while (it.hasNext()) {
            elements.add(it.next());
        }

        // Then: [40, 30, 20]
        assertEquals(Arrays.asList(40L, 30L, 20L), elements);
    }

    @Test
    public void subSet_size_shouldReturnRangeCount() {
        // Given: subSet(15, 45) = {20, 30, 40}
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);

        // Then: 3
        assertEquals(3, sub.size());
    }

    // ==================== DescendingSetView 테스트 (6개) ====================

    @Test
    public void descendingSet_iterator_shouldIterateReverse() {
        // Given: descendingSet
        NavigableSet<Long> desc = set.descendingSet();

        // When: iterate
        List<Long> elements = new ArrayList<>();
        for (Long e : desc) {
            elements.add(e);
        }

        // Then: [50, 40, 30, 20, 10]
        assertEquals(Arrays.asList(50L, 40L, 30L, 20L, 10L), elements);
    }

    @Test
    public void descendingSet_first_shouldReturnOriginalLast() {
        // Given: descendingSet
        NavigableSet<Long> desc = set.descendingSet();

        // Then: first() returns 50 (original last)
        assertEquals(Long.valueOf(50L), desc.first());
    }

    @Test
    public void descendingSet_last_shouldReturnOriginalFirst() {
        // Given: descendingSet
        NavigableSet<Long> desc = set.descendingSet();

        // Then: last() returns 10 (original first)
        assertEquals(Long.valueOf(10L), desc.last());
    }

    @Test
    public void descendingSet_lower_shouldReturnOriginalHigher() {
        // Given: descendingSet
        NavigableSet<Long> desc = set.descendingSet();

        // When: lower(30) - in descending order, "lower" is the next larger in original
        Long result = desc.lower(30L);

        // Then: 40 (higher in original = lower in descending)
        assertEquals(Long.valueOf(40L), result);
    }

    @Test
    public void descendingSet_descendingIterator_shouldReturnOriginalOrder() {
        // Given: descendingSet
        NavigableSet<Long> desc = set.descendingSet();

        // When: descendingIterator (descending of descending = original)
        List<Long> elements = new ArrayList<>();
        Iterator<Long> it = desc.descendingIterator();
        while (it.hasNext()) {
            elements.add(it.next());
        }

        // Then: [10, 20, 30, 40, 50] (original order)
        assertEquals(Arrays.asList(10L, 20L, 30L, 40L, 50L), elements);
    }

    @Test
    public void descendingSet_contains_shouldWork() {
        // Given: descendingSet
        NavigableSet<Long> desc = set.descendingSet();

        // Then
        assertTrue(desc.contains(30L));
        assertFalse(desc.contains(100L));
    }

    // ==================== 추가 Edge Case 테스트 ====================

    @Test
    public void subSet_comparator_shouldReturnParentComparator() {
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);
        assertNotNull(sub.comparator());
    }

    @Test
    public void descendingSet_comparator_shouldBeReverse() {
        NavigableSet<Long> desc = set.descendingSet();
        Comparator<? super Long> cmp = desc.comparator();

        assertNotNull(cmp);
        // Descending: 30 should be "less than" 20
        assertTrue(cmp.compare(30L, 20L) < 0);
    }

    @Test
    public void descendingSet_higher_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();

        // higher in descending = lower in original
        Long result = desc.higher(30L);

        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void descendingSet_floor_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();

        // floor in descending = ceiling in original
        Long result = desc.floor(25L);

        assertEquals(Long.valueOf(30L), result);
    }

    @Test
    public void descendingSet_ceiling_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();

        // ceiling in descending = floor in original
        Long result = desc.ceiling(25L);

        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void descendingSet_size_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        assertEquals(5, desc.size());
    }

    @Test
    public void descendingSet_isEmpty_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        assertFalse(desc.isEmpty());
    }

    @Test
    public void subSet_pollFirst_shouldWork() {
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);
        Long first = sub.pollFirst();
        assertEquals(Long.valueOf(20L), first);
        assertFalse(set.contains(20L));
    }

    @Test
    public void subSet_pollLast_shouldWork() {
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);
        Long last = sub.pollLast();
        assertEquals(Long.valueOf(40L), last);
        assertFalse(set.contains(40L));
    }

    @Test
    public void descendingSet_pollFirst_shouldWork() {
        Long first = set.descendingSet().pollFirst();
        assertEquals(Long.valueOf(50L), first);
        assertFalse(set.contains(50L));
    }

    @Test
    public void descendingSet_pollLast_shouldWork() {
        Long last = set.descendingSet().pollLast();
        assertEquals(Long.valueOf(10L), last);
        assertFalse(set.contains(10L));
    }

    @Test
    public void subSet_descendingSet_shouldWork() {
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);
        NavigableSet<Long> desc = sub.descendingSet();

        assertEquals(Long.valueOf(40L), desc.first());
        assertEquals(Long.valueOf(20L), desc.last());
    }

    @Test
    public void subSet_subSet_shouldWork() {
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);
        NavigableSet<Long> nested = sub.subSet(20L, true, 35L, false);

        assertEquals(2, nested.size());
        assertTrue(nested.contains(20L));
        assertTrue(nested.contains(30L));
    }

    @Test
    public void subSet_headSet_shouldWork() {
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);
        NavigableSet<Long> head = sub.headSet(30L, true);

        assertEquals(2, head.size());
        assertTrue(head.contains(20L));
        assertTrue(head.contains(30L));
    }

    @Test
    public void subSet_tailSet_shouldWork() {
        NavigableSet<Long> sub = set.subSet(15L, true, 45L, false);
        NavigableSet<Long> tail = sub.tailSet(25L, true);

        assertEquals(2, tail.size());
        assertTrue(tail.contains(30L));
        assertTrue(tail.contains(40L));
    }

    @Test
    public void descendingSet_subSet_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        NavigableSet<Long> sub = desc.subSet(40L, true, 20L, true);

        assertEquals(3, sub.size());
        assertTrue(sub.contains(40L));
        assertTrue(sub.contains(30L));
        assertTrue(sub.contains(20L));
    }

    @Test
    public void descendingSet_headSet_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        NavigableSet<Long> head = desc.headSet(30L, true);

        assertEquals(3, head.size());
        assertTrue(head.contains(50L));
        assertTrue(head.contains(40L));
        assertTrue(head.contains(30L));
    }

    @Test
    public void descendingSet_tailSet_shouldWork() {
        NavigableSet<Long> desc = set.descendingSet();
        NavigableSet<Long> tail = desc.tailSet(30L, true);

        assertEquals(3, tail.size());
        assertTrue(tail.contains(30L));
        assertTrue(tail.contains(20L));
        assertTrue(tail.contains(10L));
    }
}
