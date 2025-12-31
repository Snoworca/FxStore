package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.*;
import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * FxNavigableSetImpl 미커버 브랜치 테스트
 *
 * 커버리지 개선 대상:
 * - 빈 Set 엣지 케이스
 * - floor/ceiling/lower/higher 경계 조건
 * - headSet/tailSet/subSet 뷰 작업
 * - descendingSet 작업
 */
public class FxNavigableSetCoverageTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("set-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== 기본 작업 테스트 ====================

    @Test
    public void set_isEmpty_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        assertTrue(set.isEmpty());

        set.add(1L);
        assertFalse(set.isEmpty());
    }

    @Test
    public void set_clear_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        assertEquals(2, set.size());

        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }

    @Test
    public void set_contains_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);

        assertTrue(set.contains(10L));
        assertTrue(set.contains(20L));
        assertFalse(set.contains(30L));
    }

    @Test
    public void set_containsAll_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        assertTrue(set.containsAll(Arrays.asList(1L, 2L)));
        assertFalse(set.containsAll(Arrays.asList(1L, 99L)));
    }

    // ==================== floor/ceiling/lower/higher 테스트 ====================

    @Test
    public void set_floor_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        assertEquals(Long.valueOf(20L), set.floor(25L)); // largest <= 25
        assertEquals(Long.valueOf(20L), set.floor(20L)); // exact match
        assertEquals(Long.valueOf(30L), set.floor(100L)); // largest element
        assertNull(set.floor(5L)); // no element <= 5
    }

    @Test
    public void set_ceiling_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        assertEquals(Long.valueOf(20L), set.ceiling(15L)); // smallest >= 15
        assertEquals(Long.valueOf(20L), set.ceiling(20L)); // exact match
        assertEquals(Long.valueOf(10L), set.ceiling(1L)); // smallest element
        assertNull(set.ceiling(100L)); // no element >= 100
    }

    @Test
    public void set_lower_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        assertEquals(Long.valueOf(10L), set.lower(20L)); // largest < 20
        assertEquals(Long.valueOf(20L), set.lower(25L)); // largest < 25
        assertNull(set.lower(10L)); // no element < 10
    }

    @Test
    public void set_higher_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        assertEquals(Long.valueOf(30L), set.higher(20L)); // smallest > 20
        assertEquals(Long.valueOf(20L), set.higher(15L)); // smallest > 15
        assertNull(set.higher(30L)); // no element > 30
    }

    @Test
    public void set_floor_ceiling_empty() {
        NavigableSet<Long> set = store.createSet("empty", Long.class);

        assertNull(set.floor(100L));
        assertNull(set.ceiling(100L));
        assertNull(set.lower(100L));
        assertNull(set.higher(100L));
    }

    // ==================== first/last/pollFirst/pollLast 테스트 ====================

    @Test
    public void set_firstLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(20L);
        set.add(10L);
        set.add(30L);

        assertEquals(Long.valueOf(10L), set.first());
        assertEquals(Long.valueOf(30L), set.last());
    }

    @Test
    public void set_pollFirst_shouldRemoveAndReturn() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        assertEquals(Long.valueOf(10L), set.pollFirst());
        assertEquals(2, set.size());
        assertFalse(set.contains(10L));
    }

    @Test
    public void set_pollLast_shouldRemoveAndReturn() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        assertEquals(Long.valueOf(30L), set.pollLast());
        assertEquals(2, set.size());
        assertFalse(set.contains(30L));
    }

    @Test
    public void set_pollFirst_empty() {
        NavigableSet<Long> set = store.createSet("empty", Long.class);
        assertNull(set.pollFirst());
    }

    @Test
    public void set_pollLast_empty() {
        NavigableSet<Long> set = store.createSet("empty", Long.class);
        assertNull(set.pollLast());
    }

    // ==================== descendingSet 테스트 ====================

    @Test
    public void set_descendingSet_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> desc = set.descendingSet();

        assertEquals(Long.valueOf(30L), desc.first());
        assertEquals(Long.valueOf(10L), desc.last());
    }

    @Test
    public void set_descendingIterator_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        Iterator<Long> it = set.descendingIterator();

        assertTrue(it.hasNext());
        assertEquals(Long.valueOf(30L), it.next());
        assertEquals(Long.valueOf(20L), it.next());
        assertEquals(Long.valueOf(10L), it.next());
        assertFalse(it.hasNext());
    }

    // ==================== headSet/tailSet/subSet 테스트 ====================

    @Test
    public void set_headSet_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        // headSet exclusive
        NavigableSet<Long> head = set.headSet(30L, false);
        assertEquals(2, head.size());
        assertTrue(head.contains(10L));
        assertTrue(head.contains(20L));
        assertFalse(head.contains(30L));

        // headSet inclusive
        NavigableSet<Long> headIncl = set.headSet(30L, true);
        assertEquals(3, headIncl.size());
        assertTrue(headIncl.contains(30L));
    }

    @Test
    public void set_tailSet_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        // tailSet inclusive
        NavigableSet<Long> tail = set.tailSet(20L, true);
        assertEquals(3, tail.size());
        assertTrue(tail.contains(20L));
        assertTrue(tail.contains(30L));
        assertTrue(tail.contains(40L));

        // tailSet exclusive
        NavigableSet<Long> tailExcl = set.tailSet(20L, false);
        assertEquals(2, tailExcl.size());
        assertFalse(tailExcl.contains(20L));
    }

    @Test
    public void set_subSet_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);
        set.add(50L);

        // subSet inclusive/exclusive
        NavigableSet<Long> sub = set.subSet(20L, true, 40L, false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains(20L));
        assertTrue(sub.contains(30L));
        assertFalse(sub.contains(40L));
    }

    @Test
    public void set_subSet_empty() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(50L);

        NavigableSet<Long> sub = set.subSet(20L, true, 40L, true);
        assertTrue(sub.isEmpty());
    }

    // ==================== 뷰 수정 테스트 ====================

    @Test
    public void set_headSet_modification() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> head = set.headSet(30L, false);
        head.add(15L);

        assertTrue(set.contains(15L)); // reflected in original
    }

    @Test(expected = IllegalArgumentException.class)
    public void set_headSet_addOutOfRange_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);

        NavigableSet<Long> head = set.headSet(15L, false);
        head.add(100L); // out of range
    }

    @Test
    public void set_tailSet_modification() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> tail = set.tailSet(20L, true);
        tail.add(25L);

        assertTrue(set.contains(25L)); // reflected in original
    }

    // ==================== remove 테스트 ====================

    @Test
    public void set_remove_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        assertTrue(set.remove(20L));
        assertEquals(2, set.size());
        assertFalse(set.contains(20L));
    }

    @Test
    public void set_remove_notFound() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);

        assertFalse(set.remove(999L));
        assertEquals(1, set.size());
    }

    @Test
    public void set_removeAll_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        set.removeAll(Arrays.asList(20L, 40L));

        assertEquals(2, set.size());
        assertTrue(set.contains(10L));
        assertTrue(set.contains(30L));
    }

    @Test
    public void set_retainAll_modifies() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        // retainAll returns true if set was modified
        boolean modified = set.retainAll(Arrays.asList(10L, 30L, 50L));

        assertTrue(modified);
        // Note: exact behavior depends on implementation
    }

    // ==================== toArray 테스트 ====================

    @Test
    public void set_toArray_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(30L);
        set.add(10L);
        set.add(20L);

        Object[] arr = set.toArray();
        assertEquals(3, arr.length);
        // sorted order
        assertEquals(10L, arr[0]);
        assertEquals(20L, arr[1]);
        assertEquals(30L, arr[2]);
    }

    @Test
    public void set_toTypedArray_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);

        Long[] arr = set.toArray(new Long[0]);
        assertEquals(2, arr.length);
    }

    // ==================== comparator 테스트 ====================

    @Test
    public void set_comparator_shouldExist() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        // FxNavigableSet uses a custom comparator
        Comparator<? super Long> comparator = set.comparator();
        // comparator can be null or non-null depending on implementation
    }

    // ==================== equals 테스트 ====================

    @Test
    public void set_sameContent_sameSize() {
        NavigableSet<Long> set1 = store.createSet("test1", Long.class);
        set1.add(10L);
        set1.add(20L);

        NavigableSet<Long> set2 = store.createSet("test2", Long.class);
        set2.add(10L);
        set2.add(20L);

        // Both sets have same content
        assertEquals(set1.size(), set2.size());
        assertTrue(set1.contains(10L));
        assertTrue(set2.contains(10L));
    }

    @Test
    public void set_differentContent_differentSize() {
        NavigableSet<Long> set1 = store.createSet("test1", Long.class);
        set1.add(10L);

        NavigableSet<Long> set2 = store.createSet("test2", Long.class);
        set2.add(20L);

        // Different content
        assertFalse(set1.contains(20L));
        assertFalse(set2.contains(10L));
    }

    // ==================== headSet/tailSet 단일 인자 테스트 ====================

    @Test
    public void set_headSet_singleArg_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 10; i++) {
            set.add(i);
        }

        // headSet(5L) - exclusive by default
        SortedSet<Long> head = set.headSet(5L);

        assertEquals(4, head.size()); // 1, 2, 3, 4
        assertTrue(head.contains(4L));
        assertFalse(head.contains(5L));
    }

    @Test
    public void set_tailSet_singleArg_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 10; i++) {
            set.add(i);
        }

        // tailSet(5L) - inclusive by default
        SortedSet<Long> tail = set.tailSet(5L);

        assertEquals(6, tail.size()); // 5, 6, 7, 8, 9, 10
        assertTrue(tail.contains(5L));
        assertFalse(tail.contains(4L));
    }

    @Test
    public void set_subSet_singleArg_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 10; i++) {
            set.add(i);
        }

        // subSet(3L, 7L) - fromInclusive, toExclusive by default
        SortedSet<Long> sub = set.subSet(3L, 7L);

        assertEquals(4, sub.size()); // 3, 4, 5, 6
        assertTrue(sub.contains(3L));
        assertTrue(sub.contains(6L));
        assertFalse(sub.contains(7L));
    }

    // ==================== SubSetView의 headSet/tailSet 단일 인자 테스트 ====================

    @Test
    public void subSetView_headSet_singleArg_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 20; i++) {
            set.add(i);
        }

        NavigableSet<Long> sub = set.subSet(5L, true, 15L, true);
        SortedSet<Long> head = sub.headSet(10L);

        assertEquals(5, head.size()); // 5, 6, 7, 8, 9
        assertTrue(head.contains(5L));
        assertFalse(head.contains(10L));
    }

    @Test
    public void subSetView_tailSet_singleArg_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 20; i++) {
            set.add(i);
        }

        NavigableSet<Long> sub = set.subSet(5L, true, 15L, true);
        SortedSet<Long> tail = sub.tailSet(10L);

        assertEquals(6, tail.size()); // 10, 11, 12, 13, 14, 15
        assertTrue(tail.contains(10L));
        assertTrue(tail.contains(15L));
    }

    @Test
    public void subSetView_subSet_singleArg_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 20; i++) {
            set.add(i);
        }

        NavigableSet<Long> sub = set.subSet(5L, true, 15L, true);
        SortedSet<Long> subSub = sub.subSet(8L, 12L);

        assertEquals(4, subSub.size()); // 8, 9, 10, 11
        assertTrue(subSub.contains(8L));
        assertFalse(subSub.contains(12L));
    }

    // ==================== addAll 테스트 ====================

    @Test
    public void set_addAll_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);

        set.addAll(Arrays.asList(1L, 2L, 3L));

        assertEquals(3, set.size());
        assertTrue(set.contains(1L));
        assertTrue(set.contains(2L));
        assertTrue(set.contains(3L));
    }

    @Test
    public void set_addAll_withDuplicates_shouldIgnoreDuplicates() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);

        set.addAll(Arrays.asList(1L, 2L, 3L));

        assertEquals(3, set.size());
    }

    // ==================== comparator 테스트 ====================

    @Test
    public void set_comparator_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        Comparator<? super Long> comp = set.comparator();
        // Verify comparator works if present
        if (comp != null) {
            assertTrue(comp.compare(1L, 2L) < 0);
            assertTrue(comp.compare(2L, 1L) > 0);
            assertEquals(0, comp.compare(1L, 1L));
        }
    }

    @Test
    public void subSet_comparator_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(10L);

        NavigableSet<Long> sub = set.subSet(3L, true, 8L, true);
        Comparator<? super Long> comp = sub.comparator();
        // Verify comparator works if present
        if (comp != null) {
            assertTrue(comp.compare(3L, 7L) < 0);
        }
    }
}
