package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;

import static org.junit.Assert.*;

/**
 * HeadSetView/TailSetView 커버리지 개선 테스트
 *
 * <p>미테스트 메서드 28개를 커버하여 48-49% → 80%+ 달성 목표</p>
 *
 * <p>테스트 분류:</p>
 * <ul>
 *   <li>기능 테스트: iterator, navigation 메서드</li>
 *   <li>UOE 테스트: 읽기 전용 제약 검증</li>
 *   <li>경계값 테스트: inclusive/exclusive 처리</li>
 *   <li>빈 뷰 테스트: 빈 범위 동작</li>
 * </ul>
 *
 * @since 0.8
 * @see SETVIEW-COVERAGE-PLAN.md
 */
public class SetViewCoverageTest {

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

    // ==================== HeadSetView Iterator 테스트 ====================

    @Test
    public void headSet_iterator_shouldIterateInRange() {
        // Given: set = {10, 20, 30, 40, 50}, headSet(30, false) = {10, 20}
        NavigableSet<Long> headSet = set.headSet(30L, false);

        // When
        List<Long> result = new ArrayList<>();
        Iterator<Long> iter = headSet.iterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }

        // Then
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(10L), result.get(0));
        assertEquals(Long.valueOf(20L), result.get(1));
    }

    @Test
    public void headSet_iterator_inclusive_shouldIncludeBoundary() {
        // Given: set = {10, 20, 30, 40, 50}, headSet(30, true) = {10, 20, 30}
        NavigableSet<Long> headSet = set.headSet(30L, true);

        // When
        List<Long> result = new ArrayList<>();
        for (Long e : headSet) {
            result.add(e);
        }

        // Then
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(30L), result.get(2));
    }

    @Test
    public void headSet_descendingIterator_shouldIterateReverse() {
        // Given: set = {10, 20, 30, 40, 50}, headSet(35) = {10, 20, 30}
        NavigableSet<Long> headSet = set.headSet(35L, false);

        // When
        List<Long> result = new ArrayList<>();
        Iterator<Long> iter = headSet.descendingIterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }

        // Then
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(30L), result.get(0));
        assertEquals(Long.valueOf(20L), result.get(1));
        assertEquals(Long.valueOf(10L), result.get(2));
    }

    @Test
    public void headSet_iterator_empty_shouldReturnEmptyIterator() {
        // Given: headSet(5) = {} (empty)
        NavigableSet<Long> headSet = set.headSet(5L, false);

        // When
        Iterator<Long> iter = headSet.iterator();

        // Then
        assertFalse(iter.hasNext());
    }

    // ==================== HeadSetView Navigation 테스트 ====================

    @Test
    public void headSet_lower_shouldReturnLowerInRange() {
        // Given: headSet(35) = {10, 20, 30}
        NavigableSet<Long> headSet = set.headSet(35L, false);

        // When
        Long result = headSet.lower(25L);

        // Then
        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void headSet_lower_atBoundary_shouldReturnLower() {
        // Given: headSet(35) = {10, 20, 30}
        NavigableSet<Long> headSet = set.headSet(35L, false);

        // When
        Long result = headSet.lower(10L);

        // Then: 10보다 작은 요소 없음
        assertNull(result);
    }

    @Test
    public void headSet_floor_shouldReturnFloorInRange() {
        // Given: headSet(35) = {10, 20, 30}
        NavigableSet<Long> headSet = set.headSet(35L, false);

        // When
        Long result = headSet.floor(25L);

        // Then
        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void headSet_floor_exact_shouldReturnSame() {
        // Given: headSet(35) = {10, 20, 30}
        NavigableSet<Long> headSet = set.headSet(35L, false);

        // When
        Long result = headSet.floor(20L);

        // Then
        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void headSet_ceiling_shouldReturnCeilingInRange() {
        // Given: headSet(35) = {10, 20, 30}
        NavigableSet<Long> headSet = set.headSet(35L, false);

        // When
        Long result = headSet.ceiling(15L);

        // Then
        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void headSet_ceiling_atBoundary_shouldReturnNull() {
        // Given: headSet(25) = {10, 20}
        NavigableSet<Long> headSet = set.headSet(25L, false);

        // When: ceiling(25) - no element >= 25 in range
        Long result = headSet.ceiling(25L);

        // Then: null (no ceiling in range)
        assertNull(result);
    }

    @Test
    public void headSet_higher_shouldReturnHigherInRange() {
        // Given: headSet(35) = {10, 20, 30}
        NavigableSet<Long> headSet = set.headSet(35L, false);

        // When
        Long result = headSet.higher(15L);

        // Then
        assertEquals(Long.valueOf(20L), result);
    }

    @Test
    public void headSet_higher_atBoundary_shouldReturnNull() {
        // Given: headSet(35) = {10, 20, 30}
        NavigableSet<Long> headSet = set.headSet(35L, false);

        // When: higher(30) - no element > 30 in range
        Long result = headSet.higher(30L);

        // Then: null (no higher in range)
        assertNull(result);
    }

    // ==================== HeadSetView Comparator 테스트 ====================

    @Test
    public void headSet_comparator_shouldReturnParentComparator() {
        // Given: FxNavigableSet uses a custom Comparator (FxCodec-based)
        NavigableSet<Long> headSet = set.headSet(35L, false);

        // When
        // Then: returns parent's comparator (not null because it's FxCodec-based)
        assertNotNull(headSet.comparator());
    }

    // ==================== HeadSetView 뷰 생성 테스트 (UOE 개선) ====================

    @Test
    public void headSet_descendingSet_shouldWork() {
        NavigableSet<Long> headSet = set.headSet(30L, false);
        NavigableSet<Long> desc = headSet.descendingSet();

        assertEquals(Long.valueOf(20L), desc.first());
        assertEquals(Long.valueOf(10L), desc.last());
    }

    @Test
    public void headSet_subSet_4param_shouldWork() {
        NavigableSet<Long> headSet = set.headSet(30L, true);
        NavigableSet<Long> sub = headSet.subSet(10L, true, 20L, true);

        assertEquals(2, sub.size());
        assertTrue(sub.contains(10L));
        assertTrue(sub.contains(20L));
    }

    @Test
    public void headSet_headSet_2param_shouldWork() {
        NavigableSet<Long> headSet = set.headSet(30L, true);
        NavigableSet<Long> nested = headSet.headSet(20L, true);

        assertEquals(2, nested.size());
        assertTrue(nested.contains(10L));
        assertTrue(nested.contains(20L));
    }

    @Test
    public void headSet_tailSet_2param_shouldWork() {
        NavigableSet<Long> headSet = set.headSet(30L, true);
        NavigableSet<Long> tail = headSet.tailSet(15L, true);

        assertEquals(2, tail.size());
        assertTrue(tail.contains(20L));
        assertTrue(tail.contains(30L));
    }

    @Test
    public void headSet_subSet_2param_shouldWork() {
        NavigableSet<Long> headSet = set.headSet(30L, true);
        SortedSet<Long> sub = headSet.subSet(10L, 20L);

        assertEquals(1, sub.size());
        assertTrue(sub.contains(10L));
    }

    @Test
    public void headSet_headSet_1param_shouldWork() {
        NavigableSet<Long> headSet = set.headSet(30L, true);
        SortedSet<Long> nested = headSet.headSet(20L);

        assertEquals(1, nested.size());
        assertTrue(nested.contains(10L));
    }

    @Test
    public void headSet_tailSet_1param_shouldWork() {
        NavigableSet<Long> headSet = set.headSet(30L, true);
        SortedSet<Long> tail = headSet.tailSet(20L);

        assertEquals(2, tail.size());
        assertTrue(tail.contains(20L));
        assertTrue(tail.contains(30L));
    }

    // ==================== TailSetView Iterator 테스트 ====================

    @Test
    public void tailSet_iterator_shouldIterateInRange() {
        // Given: set = {10, 20, 30, 40, 50}, tailSet(30, true) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(30L, true);

        // When
        List<Long> result = new ArrayList<>();
        Iterator<Long> iter = tailSet.iterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }

        // Then
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(30L), result.get(0));
        assertEquals(Long.valueOf(40L), result.get(1));
        assertEquals(Long.valueOf(50L), result.get(2));
    }

    @Test
    public void tailSet_iterator_exclusive_shouldExcludeBoundary() {
        // Given: set = {10, 20, 30, 40, 50}, tailSet(30, false) = {40, 50}
        NavigableSet<Long> tailSet = set.tailSet(30L, false);

        // When
        List<Long> result = new ArrayList<>();
        for (Long e : tailSet) {
            result.add(e);
        }

        // Then
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(40L), result.get(0));
    }

    @Test
    public void tailSet_descendingIterator_shouldIterateReverse() {
        // Given: set = {10, 20, 30, 40, 50}, tailSet(25) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When
        List<Long> result = new ArrayList<>();
        Iterator<Long> iter = tailSet.descendingIterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }

        // Then
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(50L), result.get(0));
        assertEquals(Long.valueOf(40L), result.get(1));
        assertEquals(Long.valueOf(30L), result.get(2));
    }

    @Test
    public void tailSet_iterator_empty_shouldReturnEmptyIterator() {
        // Given: tailSet(60) = {} (empty)
        NavigableSet<Long> tailSet = set.tailSet(60L, true);

        // When
        Iterator<Long> iter = tailSet.iterator();

        // Then
        assertFalse(iter.hasNext());
    }

    // ==================== TailSetView Navigation 테스트 ====================

    @Test
    public void tailSet_lower_shouldReturnLowerInRange() {
        // Given: tailSet(25) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When
        Long result = tailSet.lower(45L);

        // Then
        assertEquals(Long.valueOf(40L), result);
    }

    @Test
    public void tailSet_lower_atBoundary_shouldReturnNull() {
        // Given: tailSet(25) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When: lower(30) - no element < 30 in range
        Long result = tailSet.lower(30L);

        // Then: null (no lower in range)
        assertNull(result);
    }

    @Test
    public void tailSet_floor_shouldReturnFloorInRange() {
        // Given: tailSet(25) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When
        Long result = tailSet.floor(45L);

        // Then
        assertEquals(Long.valueOf(40L), result);
    }

    @Test
    public void tailSet_floor_exact_shouldReturnSame() {
        // Given: tailSet(25) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When
        Long result = tailSet.floor(40L);

        // Then
        assertEquals(Long.valueOf(40L), result);
    }

    @Test
    public void tailSet_ceiling_shouldReturnCeilingInRange() {
        // Given: tailSet(25) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When
        Long result = tailSet.ceiling(35L);

        // Then
        assertEquals(Long.valueOf(40L), result);
    }

    @Test
    public void tailSet_ceiling_atFirst_shouldReturnFirst() {
        // Given: tailSet(25) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When
        Long result = tailSet.ceiling(25L);

        // Then
        assertEquals(Long.valueOf(30L), result);
    }

    @Test
    public void tailSet_higher_shouldReturnHigherInRange() {
        // Given: tailSet(25) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When
        Long result = tailSet.higher(35L);

        // Then
        assertEquals(Long.valueOf(40L), result);
    }

    @Test
    public void tailSet_higher_atLast_shouldReturnNull() {
        // Given: tailSet(25) = {30, 40, 50}
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When: 50보다 큰 요소 없음
        Long result = tailSet.higher(50L);

        // Then
        assertNull(result);
    }

    // ==================== TailSetView Comparator 테스트 ====================

    @Test
    public void tailSet_comparator_shouldReturnParentComparator() {
        // Given: FxNavigableSet uses a custom Comparator (FxCodec-based)
        NavigableSet<Long> tailSet = set.tailSet(25L, true);

        // When
        // Then: returns parent's comparator (not null because it's FxCodec-based)
        assertNotNull(tailSet.comparator());
    }

    // ==================== TailSetView 뷰 생성 테스트 (UOE 개선) ====================

    @Test
    public void tailSet_descendingSet_shouldWork() {
        NavigableSet<Long> tailSet = set.tailSet(30L, true);
        NavigableSet<Long> desc = tailSet.descendingSet();

        assertEquals(Long.valueOf(50L), desc.first());
        assertEquals(Long.valueOf(30L), desc.last());
    }

    @Test
    public void tailSet_subSet_4param_shouldWork() {
        NavigableSet<Long> tailSet = set.tailSet(30L, true);
        NavigableSet<Long> sub = tailSet.subSet(30L, true, 50L, true);

        assertEquals(3, sub.size());
        assertTrue(sub.contains(30L));
        assertTrue(sub.contains(40L));
        assertTrue(sub.contains(50L));
    }

    @Test
    public void tailSet_headSet_2param_shouldWork() {
        NavigableSet<Long> tailSet = set.tailSet(30L, true);
        NavigableSet<Long> head = tailSet.headSet(50L, true);

        assertEquals(3, head.size());
        assertTrue(head.contains(30L));
        assertTrue(head.contains(40L));
        assertTrue(head.contains(50L));
    }

    @Test
    public void tailSet_tailSet_2param_shouldWork() {
        NavigableSet<Long> tailSet = set.tailSet(30L, true);
        NavigableSet<Long> nested = tailSet.tailSet(40L, true);

        assertEquals(2, nested.size());
        assertTrue(nested.contains(40L));
        assertTrue(nested.contains(50L));
    }

    @Test
    public void tailSet_subSet_2param_shouldWork() {
        NavigableSet<Long> tailSet = set.tailSet(30L, true);
        SortedSet<Long> sub = tailSet.subSet(30L, 50L);

        assertEquals(2, sub.size());
        assertTrue(sub.contains(30L));
        assertTrue(sub.contains(40L));
    }

    @Test
    public void tailSet_headSet_1param_shouldWork() {
        NavigableSet<Long> tailSet = set.tailSet(30L, true);
        SortedSet<Long> head = tailSet.headSet(50L);

        assertEquals(2, head.size());
        assertTrue(head.contains(30L));
        assertTrue(head.contains(40L));
    }

    @Test
    public void tailSet_tailSet_1param_shouldWork() {
        NavigableSet<Long> tailSet = set.tailSet(30L, true);
        SortedSet<Long> nested = tailSet.tailSet(40L);

        assertEquals(2, nested.size());
        assertTrue(nested.contains(40L));
        assertTrue(nested.contains(50L));
    }

    // ==================== 경계값 테스트 ====================

    @Test
    public void headSet_withSingleElement_shouldWork() {
        // Given: headSet(15) = {10} (single element)
        NavigableSet<Long> headSet = set.headSet(15L, false);

        // When/Then
        assertEquals(1, headSet.size());
        assertEquals(Long.valueOf(10L), headSet.first());
        assertEquals(Long.valueOf(10L), headSet.last());
    }

    @Test
    public void tailSet_withSingleElement_shouldWork() {
        // Given: tailSet(45) = {50} (single element)
        NavigableSet<Long> tailSet = set.tailSet(45L, true);

        // When/Then
        assertEquals(1, tailSet.size());
        assertEquals(Long.valueOf(50L), tailSet.first());
        assertEquals(Long.valueOf(50L), tailSet.last());
    }

    @Test
    public void headSet_descendingIterator_empty_shouldWork() {
        // Given: empty headSet
        NavigableSet<Long> headSet = set.headSet(5L, false);

        // When
        Iterator<Long> iter = headSet.descendingIterator();

        // Then
        assertFalse(iter.hasNext());
    }

    @Test
    public void tailSet_descendingIterator_empty_shouldWork() {
        // Given: empty tailSet
        NavigableSet<Long> tailSet = set.tailSet(60L, true);

        // When
        Iterator<Long> iter = tailSet.descendingIterator();

        // Then
        assertFalse(iter.hasNext());
    }
}
