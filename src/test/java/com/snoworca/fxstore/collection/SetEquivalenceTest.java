package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.*;
import com.snoworca.fxstore.core.FxStoreImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * FxNavigableSet vs TreeSet Equivalence 테스트.
 *
 * FxNavigableSet과 Java 표준 TreeSet이 동일한 연산에 대해 동일한 결과를 내는지 검증합니다.
 */
public class SetEquivalenceTest {

    private FxStore store;
    private NavigableSet<Long> fxSet;
    private TreeSet<Long> refSet;
    private Random random;

    @Before
    public void setUp() {
        store = FxStoreImpl.openMemory(FxOptions.defaults());
        fxSet = store.createSet("testSet", Long.class);
        refSet = new TreeSet<>();
        random = new Random(42);
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== 기본 연산 ====================

    @Test
    public void testEquivalence_AddContainsRemove() {
        for (int i = 0; i < 100; i++) {
            Long value = (long) i;
            assertEquals(refSet.add(value), fxSet.add(value));
        }

        assertSetsEqual();

        // Contains
        for (int i = 0; i < 150; i++) {
            assertEquals(refSet.contains((long) i), fxSet.contains((long) i));
        }

        // Remove
        for (int i = 0; i < 50; i++) {
            Long value = (long) (i * 2);
            assertEquals(refSet.remove(value), fxSet.remove(value));
        }

        assertSetsEqual();
    }

    @Test
    public void testEquivalence_DuplicateAdd() {
        fxSet.add(1L);
        refSet.add(1L);

        // 중복 추가 시 false 반환
        assertEquals(refSet.add(1L), fxSet.add(1L));
        assertEquals(refSet.size(), fxSet.size());
    }

    // ==================== NavigableSet 연산 ====================

    @Test
    public void testEquivalence_FirstLast() {
        // Use deterministic values to avoid random differences
        for (int i = 0; i < 50; i++) {
            Long value = (long) i;
            fxSet.add(value);
            refSet.add(value);
        }

        assertEquals(refSet.first(), fxSet.first());
        assertEquals(refSet.last(), fxSet.last());
    }

    @Test
    public void testEquivalence_FloorCeiling() {
        long[] values = {10L, 20L, 30L, 40L, 50L};
        for (long v : values) {
            fxSet.add(v);
            refSet.add(v);
        }

        for (long k = 0; k <= 60; k += 5) {
            assertEquals("floor(" + k + ")", refSet.floor(k), fxSet.floor(k));
            assertEquals("ceiling(" + k + ")", refSet.ceiling(k), fxSet.ceiling(k));
            assertEquals("lower(" + k + ")", refSet.lower(k), fxSet.lower(k));
            assertEquals("higher(" + k + ")", refSet.higher(k), fxSet.higher(k));
        }
    }

    @Test
    public void testEquivalence_FloorCeilingEdgeCases() {
        fxSet.add(50L);
        refSet.add(50L);

        // 단일 요소에서 테스트
        assertEquals(refSet.floor(50L), fxSet.floor(50L));
        assertEquals(refSet.ceiling(50L), fxSet.ceiling(50L));
        assertEquals(refSet.lower(50L), fxSet.lower(50L));
        assertEquals(refSet.higher(50L), fxSet.higher(50L));

        // 범위 밖
        assertEquals(refSet.floor(0L), fxSet.floor(0L));
        assertEquals(refSet.ceiling(100L), fxSet.ceiling(100L));
    }

    // ==================== 서브셋 연산 ====================

    @Test
    public void testEquivalence_SubSet() {
        for (int i = 0; i < 100; i++) {
            fxSet.add((long) i);
            refSet.add((long) i);
        }

        NavigableSet<Long> fxSub = fxSet.subSet(20L, true, 80L, false);
        NavigableSet<Long> refSub = refSet.subSet(20L, true, 80L, false);

        assertEquals(refSub.size(), fxSub.size());
        assertEquals(refSub.first(), fxSub.first());
        assertEquals(refSub.last(), fxSub.last());
    }

    @Test
    public void testEquivalence_HeadSet() {
        for (int i = 0; i < 50; i++) {
            fxSet.add((long) i);
            refSet.add((long) i);
        }

        NavigableSet<Long> fxHead = fxSet.headSet(25L, false);
        NavigableSet<Long> refHead = refSet.headSet(25L, false);

        assertEquals(refHead.size(), fxHead.size());
        assertEquals(refHead.first(), fxHead.first());
        assertEquals(refHead.last(), fxHead.last());
    }

    @Test
    public void testEquivalence_TailSet() {
        for (int i = 0; i < 50; i++) {
            fxSet.add((long) i);
            refSet.add((long) i);
        }

        NavigableSet<Long> fxTail = fxSet.tailSet(25L, true);
        NavigableSet<Long> refTail = refSet.tailSet(25L, true);

        assertEquals(refTail.size(), fxTail.size());
        assertEquals(refTail.first(), fxTail.first());
        assertEquals(refTail.last(), fxTail.last());
    }

    // ==================== 뷰 연산 ====================

    @Test
    public void testEquivalence_DescendingSet() {
        for (int i = 0; i < 30; i++) {
            fxSet.add((long) i);
            refSet.add((long) i);
        }

        NavigableSet<Long> fxDesc = fxSet.descendingSet();
        NavigableSet<Long> refDesc = refSet.descendingSet();

        assertEquals(refDesc.size(), fxDesc.size());
        assertEquals(refDesc.first(), fxDesc.first());
        assertEquals(refDesc.last(), fxDesc.last());
    }

    @Test
    public void testEquivalence_DescendingIterator() {
        for (int i = 0; i < 30; i++) {
            fxSet.add((long) i);
            refSet.add((long) i);
        }

        Iterator<Long> fxDescIt = fxSet.descendingIterator();
        Iterator<Long> refDescIt = refSet.descendingIterator();

        while (refDescIt.hasNext()) {
            assertTrue(fxDescIt.hasNext());
            assertEquals(refDescIt.next(), fxDescIt.next());
        }
        assertFalse(fxDescIt.hasNext());
    }

    // ==================== 랜덤 연산 ====================

    @Test
    public void testEquivalence_RandomOperations() {
        for (int i = 0; i < 1000; i++) {
            int op = random.nextInt(100);
            Long value = (long) random.nextInt(200);

            if (op < 50) {
                // 50% add
                assertEquals(refSet.add(value), fxSet.add(value));
            } else if (op < 70) {
                // 20% contains
                assertEquals(refSet.contains(value), fxSet.contains(value));
            } else if (op < 90) {
                // 20% remove
                assertEquals(refSet.remove(value), fxSet.remove(value));
            } else {
                // 10% size check
                assertEquals(refSet.size(), fxSet.size());
            }

            if (i % 100 == 0) {
                assertEquals(refSet.size(), fxSet.size());
            }
        }

        assertSetsEqual();
    }

    @Test
    public void testEquivalence_Iterator() {
        for (int i = 0; i < 50; i++) {
            fxSet.add((long) i);
            refSet.add((long) i);
        }

        Iterator<Long> fxIt = fxSet.iterator();
        Iterator<Long> refIt = refSet.iterator();

        while (refIt.hasNext()) {
            assertTrue(fxIt.hasNext());
            assertEquals(refIt.next(), fxIt.next());
        }

        assertFalse(fxIt.hasNext());
    }

    // ==================== 엣지 케이스 ====================

    @Test
    public void testEquivalence_EmptySet() {
        assertTrue(fxSet.isEmpty());
        assertTrue(refSet.isEmpty());
        assertEquals(refSet.size(), fxSet.size());
        assertFalse(fxSet.contains(1L));
    }

    @Test
    public void testEquivalence_SingleElement() {
        fxSet.add(42L);
        refSet.add(42L);

        assertEquals(refSet.size(), fxSet.size());
        assertEquals(refSet.first(), fxSet.first());
        assertEquals(refSet.last(), fxSet.last());
        assertTrue(fxSet.contains(42L));
    }

    @Test
    public void testEquivalence_ToArray() {
        for (int i = 0; i < 30; i++) {
            fxSet.add((long) i);
            refSet.add((long) i);
        }

        Object[] fxArray = fxSet.toArray();
        Object[] refArray = refSet.toArray();

        assertArrayEquals(refArray, fxArray);
    }

    @Test
    public void testEquivalence_AddAll() {
        List<Long> toAdd = Arrays.asList(1L, 2L, 3L, 4L, 5L);

        assertEquals(refSet.addAll(toAdd), fxSet.addAll(toAdd));
        assertSetsEqual();

        // 중복 추가
        assertEquals(refSet.addAll(toAdd), fxSet.addAll(toAdd));
        assertSetsEqual();
    }

    @Test
    public void testEquivalence_ContainsAll() {
        for (int i = 0; i < 10; i++) {
            fxSet.add((long) i);
            refSet.add((long) i);
        }

        List<Long> subset = Arrays.asList(1L, 3L, 5L);
        List<Long> superset = Arrays.asList(1L, 3L, 50L);

        assertEquals(refSet.containsAll(subset), fxSet.containsAll(subset));
        assertEquals(refSet.containsAll(superset), fxSet.containsAll(superset));
    }

    // ==================== 헬퍼 메서드 ====================

    private void assertSetsEqual() {
        assertEquals("Size mismatch", refSet.size(), fxSet.size());

        Iterator<Long> refIt = refSet.iterator();
        Iterator<Long> fxIt = fxSet.iterator();

        while (refIt.hasNext()) {
            assertTrue("FxSet should have more elements", fxIt.hasNext());
            assertEquals(refIt.next(), fxIt.next());
        }
        assertFalse("FxSet should not have more elements", fxIt.hasNext());
    }
}
