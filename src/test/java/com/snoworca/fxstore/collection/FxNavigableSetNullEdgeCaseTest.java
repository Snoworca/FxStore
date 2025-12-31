package com.snoworca.fxstore.collection;

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
 * FxNavigableSetImpl null 및 경계 조건 테스트
 *
 * 커버리지 개선 대상:
 * - null 처리 경로
 * - SubSetView/HeadSetView/TailSetView 경계 조건
 * - DescendingSetView 연산
 * - 빈 set 연산
 */
public class FxNavigableSetNullEdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("set-null-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== null 처리 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void add_null_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(null);
    }

    @Test(expected = NullPointerException.class)
    public void contains_null_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.contains(null);
    }

    @Test(expected = NullPointerException.class)
    public void remove_null_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.remove(null);
    }

    @Test(expected = NullPointerException.class)
    public void lower_null_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.lower(null);
    }

    @Test(expected = NullPointerException.class)
    public void higher_null_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.higher(null);
    }

    @Test(expected = NullPointerException.class)
    public void floor_null_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.floor(null);
    }

    @Test(expected = NullPointerException.class)
    public void ceiling_null_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.ceiling(null);
    }

    // ==================== 빈 set null 반환 테스트 ====================

    @Test
    public void pollFirst_emptySet_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        assertNull(set.pollFirst());
    }

    @Test
    public void pollLast_emptySet_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        assertNull(set.pollLast());
    }

    @Test
    public void lower_emptySet_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        assertNull(set.lower(5L));
    }

    @Test
    public void higher_emptySet_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        assertNull(set.higher(5L));
    }

    @Test
    public void floor_emptySet_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        assertNull(set.floor(5L));
    }

    @Test
    public void ceiling_emptySet_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        assertNull(set.ceiling(5L));
    }

    @Test(expected = NoSuchElementException.class)
    public void first_emptySet_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.first();
    }

    @Test(expected = NoSuchElementException.class)
    public void last_emptySet_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.last();
    }

    // ==================== 경계값 null 반환 테스트 ====================

    @Test
    public void lower_noLowerExists_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(5L);
        set.add(10L);
        assertNull(set.lower(5L)); // 5 미만의 요소 없음
    }

    @Test
    public void higher_noHigherExists_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(5L);
        set.add(10L);
        assertNull(set.higher(10L)); // 10 초과의 요소 없음
    }

    @Test
    public void floor_allElementsHigher_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);
        set.add(20L);
        assertNull(set.floor(5L)); // 5 이하의 요소 없음
    }

    @Test
    public void ceiling_allElementsLower_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        assertNull(set.ceiling(10L)); // 10 이상의 요소 없음
    }

    // ==================== SubSetView 테스트 ====================

    @Test
    public void subSet_pollFirst_empty_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(10L);

        NavigableSet<Long> sub = set.subSet(4L, true, 6L, true);
        assertNull(sub.pollFirst()); // 빈 서브셋
    }

    @Test
    public void subSet_pollLast_empty_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(10L);

        NavigableSet<Long> sub = set.subSet(4L, true, 6L, true);
        assertNull(sub.pollLast()); // 빈 서브셋
    }

    @Test(expected = NoSuchElementException.class)
    public void subSet_first_empty_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(10L);

        NavigableSet<Long> sub = set.subSet(4L, true, 6L, true);
        sub.first();
    }

    @Test(expected = NoSuchElementException.class)
    public void subSet_last_empty_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(10L);

        NavigableSet<Long> sub = set.subSet(4L, true, 6L, true);
        sub.last();
    }

    @Test(expected = IllegalArgumentException.class)
    public void subSet_nestedSubSet_outOfRange_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 20; i++) {
            set.add(i);
        }

        NavigableSet<Long> sub = set.subSet(5L, true, 15L, true);
        sub.subSet(1L, true, 20L, true); // 범위 벗어남
    }

    // ==================== HeadSetView 테스트 ====================

    @Test
    public void headSet_pollFirst_empty_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);

        NavigableSet<Long> head = set.headSet(5L, false);
        assertNull(head.pollFirst()); // 빈 헤드셋
    }

    @Test
    public void headSet_pollLast_empty_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);

        NavigableSet<Long> head = set.headSet(5L, false);
        assertNull(head.pollLast()); // 빈 헤드셋
    }

    @Test(expected = NoSuchElementException.class)
    public void headSet_first_empty_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);

        NavigableSet<Long> head = set.headSet(5L, false);
        head.first();
    }

    @Test(expected = NoSuchElementException.class)
    public void headSet_last_empty_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(10L);

        NavigableSet<Long> head = set.headSet(5L, false);
        head.last();
    }

    @Test(expected = IllegalArgumentException.class)
    public void headSet_nestedHeadSet_outOfRange_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 10; i++) {
            set.add(i);
        }

        NavigableSet<Long> head = set.headSet(5L, true);
        head.headSet(10L, true); // 범위 벗어남
    }

    // ==================== TailSetView 테스트 ====================

    @Test
    public void tailSet_pollFirst_empty_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);

        NavigableSet<Long> tail = set.tailSet(10L, true);
        assertNull(tail.pollFirst()); // 빈 테일셋
    }

    @Test
    public void tailSet_pollLast_empty_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);

        NavigableSet<Long> tail = set.tailSet(10L, true);
        assertNull(tail.pollLast()); // 빈 테일셋
    }

    @Test(expected = NoSuchElementException.class)
    public void tailSet_first_empty_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);

        NavigableSet<Long> tail = set.tailSet(10L, true);
        tail.first();
    }

    @Test(expected = NoSuchElementException.class)
    public void tailSet_last_empty_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);

        NavigableSet<Long> tail = set.tailSet(10L, true);
        tail.last();
    }

    @Test(expected = IllegalArgumentException.class)
    public void tailSet_nestedTailSet_outOfRange_shouldThrow() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 10; i++) {
            set.add(i);
        }

        NavigableSet<Long> tail = set.tailSet(5L, true);
        tail.tailSet(1L, true); // 범위 벗어남
    }

    // ==================== DescendingSet 테스트 ====================

    @Test
    public void descendingSet_basic_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 5; i++) {
            set.add(i);
        }

        NavigableSet<Long> desc = set.descendingSet();

        assertEquals(Long.valueOf(5L), desc.first());
        assertEquals(Long.valueOf(1L), desc.last());
    }

    @Test
    public void descendingSet_iteration_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 5; i++) {
            set.add(i);
        }

        NavigableSet<Long> desc = set.descendingSet();

        List<Long> elements = new ArrayList<>();
        for (Long e : desc) {
            elements.add(e);
        }

        assertEquals(Arrays.asList(5L, 4L, 3L, 2L, 1L), elements);
    }

    @Test
    public void descendingSet_pollFirst_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        NavigableSet<Long> desc = set.descendingSet();
        assertEquals(Long.valueOf(3L), desc.pollFirst());

        assertFalse(set.contains(3L));
    }

    @Test
    public void descendingSet_pollLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        NavigableSet<Long> desc = set.descendingSet();
        assertEquals(Long.valueOf(1L), desc.pollLast());

        assertFalse(set.contains(1L));
    }

    @Test
    public void descendingSet_empty_pollFirst_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        NavigableSet<Long> desc = set.descendingSet();
        assertNull(desc.pollFirst());
    }

    @Test
    public void descendingSet_empty_pollLast_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        NavigableSet<Long> desc = set.descendingSet();
        assertNull(desc.pollLast());
    }

    @Test
    public void descendingSet_lowerHigher_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 5; i++) {
            set.add(i);
        }

        NavigableSet<Long> desc = set.descendingSet();

        // 역순에서 lower는 숫자가 더 큰 것
        assertEquals(Long.valueOf(4L), desc.lower(3L)); // 4 > 3
        assertEquals(Long.valueOf(2L), desc.higher(3L)); // 2 < 3
    }

    @Test
    public void descendingSet_descendingSet_shouldReturnOriginal() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);

        NavigableSet<Long> desc = set.descendingSet();
        NavigableSet<Long> descDesc = desc.descendingSet();

        // 역순의 역순은 원래 순서
        assertEquals(Long.valueOf(1L), descDesc.first());
        assertEquals(Long.valueOf(2L), descDesc.last());
    }

    // ==================== SubSetView lower/higher null 테스트 ====================

    @Test
    public void subSet_lower_noLowerInRange_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 10; i++) {
            set.add(i);
        }

        NavigableSet<Long> sub = set.subSet(5L, true, 8L, true);
        assertNull(sub.lower(5L)); // 5 미만의 요소가 범위 내에 없음
    }

    @Test
    public void subSet_higher_noHigherInRange_shouldReturnNull() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 10; i++) {
            set.add(i);
        }

        NavigableSet<Long> sub = set.subSet(5L, true, 8L, true);
        assertNull(sub.higher(8L)); // 8 초과의 요소가 범위 내에 없음
    }

    // ==================== isEmpty 테스트 ====================

    @Test
    public void isEmpty_emptySet_shouldReturnTrue() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        assertTrue(set.isEmpty());
    }

    @Test
    public void isEmpty_afterAdd_shouldReturnFalse() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        assertFalse(set.isEmpty());
    }

    @Test
    public void isEmpty_afterClear_shouldReturnTrue() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void subSet_isEmpty_whenNoElementsInRange_shouldReturnTrue() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(10L);

        NavigableSet<Long> sub = set.subSet(4L, true, 6L, true);
        assertTrue(sub.isEmpty());
    }

    // ==================== size 테스트 ====================

    @Test
    public void size_emptySet_shouldBeZero() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        assertEquals(0, set.size());
    }

    @Test
    public void size_afterOperations_shouldBeAccurate() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);
        assertEquals(3, set.size());

        set.remove(2L);
        assertEquals(2, set.size());

        set.add(4L);
        assertEquals(3, set.size());
    }

    // ==================== comparator 테스트 ====================

    @Test
    public void comparator_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        Comparator<? super Long> comp = set.comparator();

        // comparator may be null or non-null depending on implementation
        if (comp != null) {
            assertTrue(comp.compare(1L, 2L) < 0);
            assertTrue(comp.compare(2L, 1L) > 0);
            assertEquals(0, comp.compare(1L, 1L));
        }
    }

    @Test
    public void subSet_comparator_shouldWork() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        for (long i = 1; i <= 10; i++) {
            set.add(i);
        }

        NavigableSet<Long> sub = set.subSet(3L, true, 7L, true);
        Comparator<? super Long> comp = sub.comparator();

        if (comp != null) {
            assertTrue(comp.compare(3L, 7L) < 0);
        }
    }

    // ==================== containsAll / addAll / removeAll 테스트 ====================

    @Test
    public void containsAll_allPresent_shouldReturnTrue() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        assertTrue(set.containsAll(Arrays.asList(1L, 2L)));
    }

    @Test
    public void containsAll_someMissing_shouldReturnFalse() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);

        assertFalse(set.containsAll(Arrays.asList(1L, 5L)));
    }

    @Test
    public void addAll_shouldAddAllElements() {
        NavigableSet<Long> set = store.createSet("test", Long.class);

        boolean modified = set.addAll(Arrays.asList(1L, 2L, 3L));

        assertTrue(modified);
        assertEquals(3, set.size());
    }

    @Test
    public void addAll_duplicates_shouldNotAddDuplicates() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);

        boolean modified = set.addAll(Arrays.asList(2L, 3L));

        assertTrue(modified); // 3이 추가됨
        assertEquals(3, set.size());
    }

    @Test
    public void removeAll_shouldRemoveMatchingElements() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        boolean modified = set.removeAll(Arrays.asList(1L, 3L));

        assertTrue(modified);
        assertEquals(1, set.size());
        assertTrue(set.contains(2L));
    }
}
