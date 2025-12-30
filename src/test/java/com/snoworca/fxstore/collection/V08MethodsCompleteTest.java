package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxOptions;
import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * v0.8 미구현 메서드 완전 테스트
 *
 * <p>테스트 대상 (15개 메서드):
 * <ul>
 *   <li>FxNavigableMapImpl: clear(), descendingMap(), navigableKeySet(),
 *       descendingKeySet(), subMap(), headMap(), tailMap()</li>
 *   <li>FxNavigableSetImpl: descendingSet(), descendingIterator(),
 *       subSet(), headSet(), tailSet()</li>
 *   <li>FxDequeImpl: removeFirstOccurrence(), removeLastOccurrence(),
 *       descendingIterator()</li>
 * </ul>
 *
 * @since 0.8
 */
public class V08MethodsCompleteTest {

    private FxStore store;

    @Before
    public void setUp() {
        store = FxStore.openMemory(FxOptions.defaults());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try {
                store.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    // ==========================================================================
    // FxNavigableMapImpl 테스트
    // ==========================================================================

    // ==================== clear() ====================

    @Test
    public void map_clear_shouldRemoveAllEntries() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");
        map.put(3L, "C");

        assertEquals(3, map.size());
        map.clear();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void map_clear_emptyMap_shouldSucceed() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.clear(); // Should not throw
        assertTrue(map.isEmpty());
    }

    @Test
    public void map_clear_shouldAllowReuse() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.clear();
        map.put(2L, "B");
        assertEquals(1, map.size());
        assertEquals("B", map.get(2L));
    }

    // ==================== descendingMap() ====================

    @Test
    public void map_descendingMap_shouldReturnReverseOrder() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");
        map.put(3L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();
        assertNotNull(descMap);

        // descending order: 3, 2, 1
        Iterator<Long> it = descMap.keySet().iterator();
        assertEquals(Long.valueOf(3L), it.next());
        assertEquals(Long.valueOf(2L), it.next());
        assertEquals(Long.valueOf(1L), it.next());
    }

    @Test
    public void map_descendingMap_size_shouldMatch() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");

        NavigableMap<Long, String> descMap = map.descendingMap();
        assertEquals(2, descMap.size());
    }

    @Test
    public void map_descendingMap_containsKey_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");

        NavigableMap<Long, String> descMap = map.descendingMap();
        assertTrue(descMap.containsKey(1L));
        assertTrue(descMap.containsKey(2L));
        assertFalse(descMap.containsKey(999L));
    }

    @Test
    public void map_descendingMap_get_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");

        NavigableMap<Long, String> descMap = map.descendingMap();
        assertEquals("A", descMap.get(1L));
        assertEquals("B", descMap.get(2L));
    }

    @Test
    public void map_descendingMap_firstLastKey_shouldBeReversed() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");
        map.put(3L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();
        assertEquals(Long.valueOf(3L), descMap.firstKey());
        assertEquals(Long.valueOf(1L), descMap.lastKey());
    }

    @Test
    public void map_descendingMap_put_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        NavigableMap<Long, String> descMap = map.descendingMap();
        descMap.put(2L, "B");
        assertTrue(map.containsKey(2L));
        assertEquals("B", map.get(2L));
    }

    // ==================== navigableKeySet() ====================

    @Test
    public void map_navigableKeySet_shouldReturnKeys() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");
        map.put(3L, "C");

        NavigableSet<Long> keySet = map.navigableKeySet();
        assertNotNull(keySet);
        assertEquals(3, keySet.size());
        assertTrue(keySet.contains(1L));
        assertTrue(keySet.contains(2L));
        assertTrue(keySet.contains(3L));
    }

    @Test
    public void map_navigableKeySet_iterator_shouldBeInOrder() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(3L, "C");
        map.put(1L, "A");
        map.put(2L, "B");

        NavigableSet<Long> keySet = map.navigableKeySet();
        Iterator<Long> it = keySet.iterator();
        assertEquals(Long.valueOf(1L), it.next());
        assertEquals(Long.valueOf(2L), it.next());
        assertEquals(Long.valueOf(3L), it.next());
    }

    // ==================== descendingKeySet() ====================

    @Test
    public void map_descendingKeySet_shouldReturnReverseOrder() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");
        map.put(3L, "C");

        NavigableSet<Long> descKeySet = map.descendingKeySet();
        assertNotNull(descKeySet);
        assertEquals(3, descKeySet.size());

        Iterator<Long> it = descKeySet.iterator();
        assertEquals(Long.valueOf(3L), it.next());
        assertEquals(Long.valueOf(2L), it.next());
        assertEquals(Long.valueOf(1L), it.next());
    }

    // ==================== subMap() ====================

    @Test
    public void map_subMap_shouldReturnRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");
        map.put(50L, "E");

        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);
        assertNotNull(subMap);
        assertEquals(3, subMap.size());
        assertTrue(subMap.containsKey(20L));
        assertTrue(subMap.containsKey(30L));
        assertTrue(subMap.containsKey(40L));
        assertFalse(subMap.containsKey(10L));
        assertFalse(subMap.containsKey(50L));
    }

    @Test
    public void map_subMap_exclusive_shouldExcludeBoundaries() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(10L, false, 40L, false);
        assertEquals(2, subMap.size());
        assertFalse(subMap.containsKey(10L));
        assertTrue(subMap.containsKey(20L));
        assertTrue(subMap.containsKey(30L));
        assertFalse(subMap.containsKey(40L));
    }

    @Test(expected = NullPointerException.class)
    public void map_subMap_nullFromKey_shouldThrowNPE() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.subMap(null, true, 20L, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void map_subMap_invalidRange_shouldThrowIAE() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.subMap(40L, true, 20L, true); // from > to
    }

    @Test
    public void map_subMap_get_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> subMap = map.subMap(10L, true, 30L, true);
        assertEquals("A", subMap.get(10L));
        assertEquals("B", subMap.get(20L));
        assertEquals("C", subMap.get(30L));
    }

    @Test
    public void map_subMap_firstLast_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);
        assertEquals(Long.valueOf(20L), subMap.firstKey());
        assertEquals(Long.valueOf(30L), subMap.lastKey());
    }

    @Test
    public void map_subMap_put_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        NavigableMap<Long, String> subMap = map.subMap(5L, true, 15L, true);
        subMap.put(12L, "B");
        assertTrue(map.containsKey(12L));
        assertEquals("B", map.get(12L));
    }

    // ==================== headMap() ====================

    @Test
    public void map_headMap_shouldReturnLessThan() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> headMap = map.headMap(30L, false);
        assertEquals(2, headMap.size());
        assertTrue(headMap.containsKey(10L));
        assertTrue(headMap.containsKey(20L));
        assertFalse(headMap.containsKey(30L));
    }

    @Test
    public void map_headMap_inclusive_shouldIncludeBoundary() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> headMap = map.headMap(20L, true);
        assertEquals(2, headMap.size());
        assertTrue(headMap.containsKey(20L));
    }

    @Test
    public void map_headMap_get_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> headMap = map.headMap(25L, true);
        assertEquals("A", headMap.get(10L));
        assertEquals("B", headMap.get(20L));
        assertNull(headMap.get(30L)); // Out of range
    }

    // ==================== tailMap() ====================

    @Test
    public void map_tailMap_shouldReturnGreaterThan() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> tailMap = map.tailMap(20L, true);
        assertEquals(3, tailMap.size());
        assertFalse(tailMap.containsKey(10L));
        assertTrue(tailMap.containsKey(20L));
        assertTrue(tailMap.containsKey(30L));
        assertTrue(tailMap.containsKey(40L));
    }

    @Test
    public void map_tailMap_exclusive_shouldExcludeBoundary() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> tailMap = map.tailMap(20L, false);
        assertEquals(1, tailMap.size());
        assertFalse(tailMap.containsKey(20L));
        assertTrue(tailMap.containsKey(30L));
    }

    // ==========================================================================
    // FxNavigableSetImpl 테스트
    // ==========================================================================

    // ==================== descendingSet() ====================

    @Test
    public void set_descendingSet_shouldReturnReverseOrder() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        NavigableSet<Long> descSet = set.descendingSet();
        assertNotNull(descSet);
        assertEquals(3, descSet.size());

        Iterator<Long> it = descSet.iterator();
        assertEquals(Long.valueOf(3L), it.next());
        assertEquals(Long.valueOf(2L), it.next());
        assertEquals(Long.valueOf(1L), it.next());
    }

    @Test
    public void set_descendingSet_firstLast_shouldBeReversed() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        NavigableSet<Long> descSet = set.descendingSet();
        assertEquals(Long.valueOf(3L), descSet.first());
        assertEquals(Long.valueOf(1L), descSet.last());
    }

    @Test
    public void set_descendingSet_contains_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(1L);
        set.add(2L);

        NavigableSet<Long> descSet = set.descendingSet();
        assertTrue(descSet.contains(1L));
        assertTrue(descSet.contains(2L));
        assertFalse(descSet.contains(999L));
    }

    @Test
    public void set_descendingSet_add_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(1L);
        NavigableSet<Long> descSet = set.descendingSet();
        descSet.add(2L);
        assertTrue(set.contains(2L));
        assertEquals(2, set.size());
    }

    // ==================== descendingIterator() ====================

    @Test
    public void set_descendingIterator_shouldReturnReverseOrder() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(1L);
        set.add(2L);
        set.add(3L);

        Iterator<Long> it = set.descendingIterator();
        assertEquals(Long.valueOf(3L), it.next());
        assertEquals(Long.valueOf(2L), it.next());
        assertEquals(Long.valueOf(1L), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void set_descendingIterator_emptySet_shouldReturnEmptyIterator() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        Iterator<Long> it = set.descendingIterator();
        assertFalse(it.hasNext());
    }

    // ==================== subSet() ====================

    @Test
    public void set_subSet_shouldReturnRange() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);
        set.add(50L);

        NavigableSet<Long> subSet = set.subSet(20L, true, 40L, true);
        assertEquals(3, subSet.size());
        assertTrue(subSet.contains(20L));
        assertTrue(subSet.contains(30L));
        assertTrue(subSet.contains(40L));
        assertFalse(subSet.contains(10L));
        assertFalse(subSet.contains(50L));
    }

    @Test
    public void set_subSet_exclusive_shouldExcludeBoundaries() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> subSet = set.subSet(10L, false, 40L, false);
        assertEquals(2, subSet.size());
        assertFalse(subSet.contains(10L));
        assertTrue(subSet.contains(20L));
        assertTrue(subSet.contains(30L));
        assertFalse(subSet.contains(40L));
    }

    @Test(expected = NullPointerException.class)
    public void set_subSet_nullFromElement_shouldThrowNPE() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.subSet(null, true, 20L, true);
    }

    @Test
    public void set_subSet_firstLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> subSet = set.subSet(15L, true, 35L, true);
        assertEquals(Long.valueOf(20L), subSet.first());
        assertEquals(Long.valueOf(30L), subSet.last());
    }

    @Test
    public void set_subSet_add_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        NavigableSet<Long> subSet = set.subSet(5L, true, 15L, true);
        subSet.add(12L);
        assertTrue(set.contains(12L));
        assertEquals(2, set.size());
    }

    // ==================== headSet() ====================

    @Test
    public void set_headSet_shouldReturnLessThan() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> headSet = set.headSet(30L, false);
        assertEquals(2, headSet.size());
        assertTrue(headSet.contains(10L));
        assertTrue(headSet.contains(20L));
        assertFalse(headSet.contains(30L));
    }

    @Test
    public void set_headSet_inclusive_shouldIncludeBoundary() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> headSet = set.headSet(20L, true);
        assertEquals(2, headSet.size());
        assertTrue(headSet.contains(20L));
    }

    @Test
    public void set_headSet_firstLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> headSet = set.headSet(25L, true);
        assertEquals(Long.valueOf(10L), headSet.first());
        assertEquals(Long.valueOf(20L), headSet.last());
    }

    // ==================== tailSet() ====================

    @Test
    public void set_tailSet_shouldReturnGreaterThan() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> tailSet = set.tailSet(20L, true);
        assertEquals(3, tailSet.size());
        assertFalse(tailSet.contains(10L));
        assertTrue(tailSet.contains(20L));
        assertTrue(tailSet.contains(30L));
        assertTrue(tailSet.contains(40L));
    }

    @Test
    public void set_tailSet_exclusive_shouldExcludeBoundary() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> tailSet = set.tailSet(20L, false);
        assertEquals(1, tailSet.size());
        assertFalse(tailSet.contains(20L));
        assertTrue(tailSet.contains(30L));
    }

    @Test
    public void set_tailSet_firstLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> tailSet = set.tailSet(15L, true);
        assertEquals(Long.valueOf(20L), tailSet.first());
        assertEquals(Long.valueOf(30L), tailSet.last());
    }

    // ==========================================================================
    // FxDequeImpl 테스트
    // ==========================================================================

    // ==================== removeFirstOccurrence() ====================

    @Test
    public void deque_removeFirstOccurrence_shouldRemoveFirst() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("A");
        deque.addLast("C");

        assertTrue(deque.removeFirstOccurrence("A"));
        assertEquals(3, deque.size());

        // 순서 확인: B, A, C
        assertEquals("B", deque.pollFirst());
        assertEquals("A", deque.pollFirst());
        assertEquals("C", deque.pollFirst());
    }

    @Test
    public void deque_removeFirstOccurrence_notFound_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addLast("A");
        deque.addLast("B");

        assertFalse(deque.removeFirstOccurrence("Z"));
        assertEquals(2, deque.size());
    }

    @Test
    public void deque_removeFirstOccurrence_emptyDeque_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("deque", String.class);
        assertFalse(deque.removeFirstOccurrence("A"));
    }

    @Test
    public void deque_removeFirstOccurrence_singleElement_shouldRemove() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addLast("A");

        assertTrue(deque.removeFirstOccurrence("A"));
        assertTrue(deque.isEmpty());
    }

    // ==================== removeLastOccurrence() ====================

    @Test
    public void deque_removeLastOccurrence_shouldRemoveLast() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("A");
        deque.addLast("C");

        assertTrue(deque.removeLastOccurrence("A"));
        assertEquals(3, deque.size());

        // 순서 확인: A, B, C
        assertEquals("A", deque.pollFirst());
        assertEquals("B", deque.pollFirst());
        assertEquals("C", deque.pollFirst());
    }

    @Test
    public void deque_removeLastOccurrence_notFound_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addLast("A");
        deque.addLast("B");

        assertFalse(deque.removeLastOccurrence("Z"));
        assertEquals(2, deque.size());
    }

    @Test
    public void deque_removeLastOccurrence_emptyDeque_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("deque", String.class);
        assertFalse(deque.removeLastOccurrence("A"));
    }

    @Test
    public void deque_removeLastOccurrence_singleElement_shouldRemove() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addLast("A");

        assertTrue(deque.removeLastOccurrence("A"));
        assertTrue(deque.isEmpty());
    }

    // ==================== descendingIterator() ====================

    @Test
    public void deque_descendingIterator_shouldReturnReverseOrder() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");

        Iterator<String> it = deque.descendingIterator();
        assertEquals("C", it.next());
        assertEquals("B", it.next());
        assertEquals("A", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void deque_descendingIterator_emptyDeque_shouldReturnEmptyIterator() {
        Deque<String> deque = store.createDeque("deque", String.class);
        Iterator<String> it = deque.descendingIterator();
        assertFalse(it.hasNext());
    }

    @Test
    public void deque_descendingIterator_singleElement_shouldWork() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addLast("A");

        Iterator<String> it = deque.descendingIterator();
        assertTrue(it.hasNext());
        assertEquals("A", it.next());
        assertFalse(it.hasNext());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void deque_descendingIterator_remove_shouldThrowUOE() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addLast("A");
        deque.addLast("B");

        Iterator<String> it = deque.descendingIterator();
        it.next();
        it.remove();
    }

    // ==========================================================================
    // 추가 엣지 케이스 테스트
    // ==========================================================================

    @Test
    public void map_subMap_emptyRange_shouldReturnEmpty() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        // 범위에 해당하는 요소 없음
        NavigableMap<Long, String> subMap = map.subMap(15L, true, 18L, true);
        assertEquals(0, subMap.size());
        assertTrue(subMap.isEmpty());
    }

    @Test
    public void set_subSet_emptyRange_shouldReturnEmpty() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> subSet = set.subSet(15L, true, 18L, true);
        assertEquals(0, subSet.size());
        assertTrue(subSet.isEmpty());
    }

    @Test
    public void deque_mixedAddRemove_descendingIterator_shouldWork() {
        Deque<String> deque = store.createDeque("deque", String.class);
        deque.addFirst("B");
        deque.addLast("C");
        deque.addFirst("A");
        // 순서: A, B, C

        Iterator<String> it = deque.descendingIterator();
        assertEquals("C", it.next());
        assertEquals("B", it.next());
        assertEquals("A", it.next());
    }

    // ==================== 뷰 navigation 메서드 테스트 ====================

    @Test
    public void map_descendingMap_navigableMethods_shouldBeReversed() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();

        // descending view에서 lower는 원본의 higher, higher는 원본의 lower
        // descending에서 정렬 순서: 30, 20, 10
        // lower(15)는 15보다 "더 큰" 값을 찾음 (descending 관점) → 원래의 higher(15) = 20
        assertEquals(Long.valueOf(20L), descMap.lowerKey(15L));
        // higher(25)는 25보다 "더 작은" 값을 찾음 (descending 관점) → 원래의 lower(25) = 20
        assertEquals(Long.valueOf(20L), descMap.higherKey(25L));
    }

    @Test
    public void set_descendingSet_navigableMethods_shouldBeReversed() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> descSet = set.descendingSet();

        // descending view에서 lower는 원본의 higher, higher는 원본의 lower
        assertEquals(Long.valueOf(20L), descSet.lower(15L));
        assertEquals(Long.valueOf(20L), descSet.higher(25L));
    }

    // ==================== 뷰 iterator 테스트 ====================

    @Test
    public void map_subMap_iterator_shouldBeInRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);

        List<Long> keys = new ArrayList<>();
        for (Long key : subMap.keySet()) {
            keys.add(key);
        }

        assertEquals(2, keys.size());
        assertEquals(Long.valueOf(20L), keys.get(0));
        assertEquals(Long.valueOf(30L), keys.get(1));
    }

    @Test
    public void set_subSet_iterator_shouldBeInRange() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> subSet = set.subSet(15L, true, 35L, true);

        List<Long> elements = new ArrayList<>();
        for (Long e : subSet) {
            elements.add(e);
        }

        assertEquals(2, elements.size());
        assertEquals(Long.valueOf(20L), elements.get(0));
        assertEquals(Long.valueOf(30L), elements.get(1));
    }

    // ==========================================================================
    // 범위 뷰 navigation 메서드 테스트 (코드 품질 개선 검증)
    // ==========================================================================

    @Test
    public void headMap_lowerEntry_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        // headMap은 30 미만
        NavigableMap<Long, String> headMap = map.headMap(30L, false);

        // lowerEntry(25)는 20 반환 (범위 내)
        Map.Entry<Long, String> entry = headMap.lowerEntry(25L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());

        // lowerEntry(15)는 10 반환 (범위 내)
        entry = headMap.lowerEntry(15L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(10L), entry.getKey());

        // lowerEntry(5)는 null (범위 내 더 작은 값 없음)
        assertNull(headMap.lowerEntry(5L));
    }

    @Test
    public void headMap_higherEntry_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        // headMap은 30 미만
        NavigableMap<Long, String> headMap = map.headMap(30L, false);

        // higherEntry(15)는 20 반환 (범위 내)
        Map.Entry<Long, String> entry = headMap.higherEntry(15L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());

        // higherEntry(20)는 null (30은 범위 외)
        assertNull(headMap.higherEntry(20L));
    }

    @Test
    public void headMap_floorEntry_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        // headMap은 25 이하 (inclusive=true)
        NavigableMap<Long, String> headMap = map.headMap(25L, true);

        // floorEntry(25)는 20 반환 (25에 정확한 매치 없음)
        Map.Entry<Long, String> entry = headMap.floorEntry(25L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());

        // floorEntry(50)는 범위 외이므로 lastEntry(20) 반환
        entry = headMap.floorEntry(50L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());
    }

    @Test
    public void headMap_ceilingEntry_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> headMap = map.headMap(30L, false);

        // ceilingEntry(15)는 20 반환
        Map.Entry<Long, String> entry = headMap.ceilingEntry(15L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());

        // ceilingEntry(25)는 null (30은 범위 외)
        assertNull(headMap.ceilingEntry(25L));
    }

    @Test
    public void tailMap_lowerEntry_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        // tailMap은 20 이상
        NavigableMap<Long, String> tailMap = map.tailMap(20L, true);

        // lowerEntry(35)는 30 반환 (범위 내)
        Map.Entry<Long, String> entry = tailMap.lowerEntry(35L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());

        // lowerEntry(25)는 20 반환 (범위 내)
        entry = tailMap.lowerEntry(25L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());

        // lowerEntry(15)는 null (10은 범위 외)
        assertNull(tailMap.lowerEntry(15L));
    }

    @Test
    public void tailMap_higherEntry_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> tailMap = map.tailMap(20L, true);

        // higherEntry(25)는 30 반환
        Map.Entry<Long, String> entry = tailMap.higherEntry(25L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());

        // higherEntry(35)는 40 반환
        entry = tailMap.higherEntry(35L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(40L), entry.getKey());
    }

    @Test
    public void tailMap_floorEntry_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> tailMap = map.tailMap(20L, true);

        // floorEntry(25)는 20 반환
        Map.Entry<Long, String> entry = tailMap.floorEntry(25L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());

        // floorEntry(15)는 null (10은 범위 외, 15 이하 범위 내 없음)
        assertNull(tailMap.floorEntry(15L));
    }

    @Test
    public void tailMap_ceilingEntry_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> tailMap = map.tailMap(20L, true);

        // ceilingEntry(25)는 30 반환
        Map.Entry<Long, String> entry = tailMap.ceilingEntry(25L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());

        // ceilingEntry(5)는 범위 외이므로 firstEntry(20) 반환
        entry = tailMap.ceilingEntry(5L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());
    }

    @Test
    public void subMap_allNavigationMethods_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");
        map.put(50L, "E");

        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);

        // 범위: 20, 30, 40

        // lowerEntry(35)는 30 반환
        assertEquals(Long.valueOf(30L), subMap.lowerEntry(35L).getKey());

        // higherEntry(25)는 30 반환
        assertEquals(Long.valueOf(30L), subMap.higherEntry(25L).getKey());

        // floorEntry(35)는 30 반환
        assertEquals(Long.valueOf(30L), subMap.floorEntry(35L).getKey());

        // ceilingEntry(25)는 30 반환
        assertEquals(Long.valueOf(30L), subMap.ceilingEntry(25L).getKey());

        // 범위 외 검색
        assertNull(subMap.lowerEntry(15L)); // 20 미만의 더 작은 값 없음
        assertNull(subMap.higherEntry(45L)); // 40 초과 없음
    }

    // ==========================================================================
    // 뷰 UnsupportedOperationException 테스트 (커버리지 향상)
    // ==========================================================================

    @Test
    public void headMap_descendingMap_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        NavigableMap<Long, String> headMap = map.headMap(20L, true);
        NavigableMap<Long, String> descHeadMap = headMap.descendingMap();
        assertNotNull(descHeadMap);
        assertEquals(Long.valueOf(20L), descHeadMap.firstKey());
        assertEquals(Long.valueOf(10L), descHeadMap.lastKey());
    }

    @Test
    public void headMap_navigableKeySet_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        NavigableMap<Long, String> headMap = map.headMap(20L, true);
        NavigableSet<Long> keySet = headMap.navigableKeySet();
        assertNotNull(keySet);
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains(10L));
        assertTrue(keySet.contains(20L));
    }

    @Test
    public void tailMap_subMap_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        NavigableMap<Long, String> tailMap = map.tailMap(10L, true);
        NavigableMap<Long, String> subMap = tailMap.subMap(10L, true, 25L, true);
        assertNotNull(subMap);
        assertEquals(2, subMap.size());
        assertTrue(subMap.containsKey(10L));
        assertTrue(subMap.containsKey(20L));
    }

    @Test
    public void subMap_descendingMap_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        NavigableMap<Long, String> subMap = map.subMap(5L, true, 25L, true);
        NavigableMap<Long, String> descSubMap = subMap.descendingMap();
        assertNotNull(descSubMap);
        assertEquals(Long.valueOf(20L), descSubMap.firstKey());
        assertEquals(Long.valueOf(10L), descSubMap.lastKey());
    }

    @Test
    public void descendingMap_subMap_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        NavigableMap<Long, String> descMap = map.descendingMap();
        // Note: in descending view, fromKey > toKey (reversed)
        NavigableMap<Long, String> subMap = descMap.subMap(25L, true, 5L, true);
        assertNotNull(subMap);
        assertEquals(2, subMap.size());
    }

    @Test
    public void keySet_subSet_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> subSet = keySet.subSet(5L, true, 25L, true);
        assertNotNull(subSet);
        assertEquals(2, subSet.size());
        assertTrue(subSet.contains(10L));
        assertTrue(subSet.contains(20L));
    }

    // ==========================================================================
    // 추가 엣지 케이스 테스트
    // ==========================================================================

    @Test
    public void map_clear_largeDataset_shouldSucceed() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);

        // 100개 요소 추가
        for (int i = 0; i < 100; i++) {
            map.put((long) i, "value-" + i);
        }
        assertEquals(100, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void descendingMap_entrySet_shouldBeReversed() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");
        map.put(3L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();
        Set<Map.Entry<Long, String>> entries = descMap.entrySet();

        List<Long> keys = new ArrayList<>();
        for (Map.Entry<Long, String> entry : entries) {
            keys.add(entry.getKey());
        }

        assertEquals(3, keys.size());
        assertEquals(Long.valueOf(3L), keys.get(0));
        assertEquals(Long.valueOf(2L), keys.get(1));
        assertEquals(Long.valueOf(1L), keys.get(2));
    }

    @Test
    public void navigableKeySet_descendingSet_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");
        map.put(3L, "C");

        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> descSet = keySet.descendingSet();

        assertEquals(Long.valueOf(3L), descSet.first());
        assertEquals(Long.valueOf(1L), descSet.last());
    }

    @Test
    public void navigableKeySet_navigationMethods_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableSet<Long> keySet = map.navigableKeySet();

        assertEquals(Long.valueOf(10L), keySet.lower(15L));
        assertEquals(Long.valueOf(20L), keySet.floor(20L));
        assertEquals(Long.valueOf(20L), keySet.ceiling(15L));
        assertEquals(Long.valueOf(20L), keySet.higher(15L));
    }

    @Test
    public void descendingKeySet_navigationMethods_shouldBeReversed() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableSet<Long> descKeySet = map.descendingKeySet();

        // descending에서 lower는 원본의 higher
        assertEquals(Long.valueOf(20L), descKeySet.lower(15L));
        // descending에서 higher는 원본의 lower
        assertEquals(Long.valueOf(20L), descKeySet.higher(25L));
    }

    // ==========================================================================
    // DescendingMapView 커버리지 개선 테스트
    // ==========================================================================

    @Test
    public void descendingMap_containsKey_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();

        assertTrue(descMap.containsKey(10L));
        assertTrue(descMap.containsKey(20L));
        assertTrue(descMap.containsKey(30L));
        assertFalse(descMap.containsKey(40L));
    }

    @Test
    public void descendingMap_containsValue_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");

        NavigableMap<Long, String> descMap = map.descendingMap();

        assertTrue(descMap.containsValue("A"));
        assertTrue(descMap.containsValue("B"));
        assertFalse(descMap.containsValue("C"));
    }

    @Test
    public void descendingMap_get_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");

        NavigableMap<Long, String> descMap = map.descendingMap();

        assertEquals("A", descMap.get(10L));
        assertEquals("B", descMap.get(20L));
        assertNull(descMap.get(30L));
    }

    @Test
    public void descendingMap_put_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");

        NavigableMap<Long, String> descMap = map.descendingMap();
        descMap.put(20L, "B");
        assertTrue(map.containsKey(20L));
        assertEquals("B", map.get(20L));
    }

    @Test
    public void descendingMap_remove_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");

        NavigableMap<Long, String> descMap = map.descendingMap();
        String removed = descMap.remove(10L);
        assertEquals("A", removed);
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void descendingMap_isEmpty_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        NavigableMap<Long, String> descMap = map.descendingMap();

        assertTrue(descMap.isEmpty());

        map.put(10L, "A");
        assertFalse(descMap.isEmpty());
    }

    @Test
    public void descendingMap_size_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        NavigableMap<Long, String> descMap = map.descendingMap();

        assertEquals(0, descMap.size());

        map.put(10L, "A");
        map.put(20L, "B");
        assertEquals(2, descMap.size());
    }

    @Test
    public void descendingMap_values_shouldBeReversed() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(1L, "A");
        map.put(2L, "B");
        map.put(3L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();
        Collection<String> values = descMap.values();

        List<String> valueList = new ArrayList<>(values);
        assertEquals(3, valueList.size());
        assertEquals("C", valueList.get(0));
        assertEquals("B", valueList.get(1));
        assertEquals("A", valueList.get(2));
    }

    @Test
    public void descendingMap_pollFirstEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();
        Map.Entry<Long, String> polled = descMap.pollFirstEntry();
        assertEquals(Long.valueOf(30L), polled.getKey());
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void descendingMap_pollLastEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();
        Map.Entry<Long, String> polled = descMap.pollLastEntry();
        assertEquals(Long.valueOf(10L), polled.getKey());
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void descendingMap_firstLastEntry_shouldBeReversed() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();

        assertEquals(Long.valueOf(30L), descMap.firstEntry().getKey());
        assertEquals(Long.valueOf(10L), descMap.lastEntry().getKey());
        assertEquals(Long.valueOf(30L), descMap.firstKey());
        assertEquals(Long.valueOf(10L), descMap.lastKey());
    }

    @Test
    public void descendingMap_comparator_shouldBeReversed() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        NavigableMap<Long, String> descMap = map.descendingMap();

        assertNotNull(descMap.comparator());
        // Comparator가 reversed이므로 큰 값이 "작은" 것으로 취급
        assertTrue(descMap.comparator().compare(10L, 20L) > 0);
    }

    @Test
    public void descendingMap_descendingMap_shouldReturnOriginal() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");

        NavigableMap<Long, String> descMap = map.descendingMap();
        NavigableMap<Long, String> original = descMap.descendingMap();

        // descendingMap of descendingMap returns the original
        assertEquals(map.firstKey(), original.firstKey());
        assertEquals(map.lastKey(), original.lastKey());
    }

    @Test
    public void descendingMap_navigableKeySet_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();
        NavigableSet<Long> keySet = descMap.navigableKeySet();

        // Returns descending key set
        assertEquals(Long.valueOf(30L), keySet.first());
        assertEquals(Long.valueOf(10L), keySet.last());
    }

    @Test
    public void descendingMap_descendingKeySet_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> descMap = map.descendingMap();
        NavigableSet<Long> descKeySet = descMap.descendingKeySet();

        // Returns ascending (original order) key set
        assertEquals(Long.valueOf(10L), descKeySet.first());
        assertEquals(Long.valueOf(30L), descKeySet.last());
    }

    // ==========================================================================
    // DescendingSetView 커버리지 개선 테스트
    // ==========================================================================

    @Test
    public void descendingSet_contains_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> descSet = set.descendingSet();

        assertTrue(descSet.contains(10L));
        assertTrue(descSet.contains(20L));
        assertTrue(descSet.contains(30L));
        assertFalse(descSet.contains(40L));
    }

    @Test
    public void descendingSet_add_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);

        NavigableSet<Long> descSet = set.descendingSet();
        descSet.add(20L);
        assertTrue(set.contains(20L));
        assertEquals(2, set.size());
    }

    @Test
    public void descendingSet_remove_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);

        NavigableSet<Long> descSet = set.descendingSet();
        boolean removed = descSet.remove(10L);
        assertTrue(removed);
        assertFalse(set.contains(10L));
    }

    @Test
    public void descendingSet_isEmpty_size_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        NavigableSet<Long> descSet = set.descendingSet();

        assertTrue(descSet.isEmpty());
        assertEquals(0, descSet.size());

        set.add(10L);
        assertFalse(descSet.isEmpty());
        assertEquals(1, descSet.size());
    }

    @Test
    public void descendingSet_pollFirst_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> descSet = set.descendingSet();
        Long polled = descSet.pollFirst();
        assertEquals(Long.valueOf(30L), polled);
        assertFalse(set.contains(30L));
    }

    @Test
    public void descendingSet_pollLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> descSet = set.descendingSet();
        Long polled = descSet.pollLast();
        assertEquals(Long.valueOf(10L), polled);
        assertFalse(set.contains(10L));
    }

    @Test
    public void descendingSet_firstLast_shouldBeReversed() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> descSet = set.descendingSet();

        assertEquals(Long.valueOf(30L), descSet.first());
        assertEquals(Long.valueOf(10L), descSet.last());
    }

    @Test
    public void descendingSet_comparator_shouldBeReversed() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        NavigableSet<Long> descSet = set.descendingSet();

        assertNotNull(descSet.comparator());
        assertTrue(descSet.comparator().compare(10L, 20L) > 0);
    }

    @Test
    public void descendingSet_descendingSet_shouldReturnOriginal() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);

        NavigableSet<Long> descSet = set.descendingSet();
        NavigableSet<Long> original = descSet.descendingSet();

        // descendingSet of descendingSet returns the original
        assertEquals(set.first(), original.first());
        assertEquals(set.last(), original.last());
    }

    // ==========================================================================
    // KeySetView 커버리지 개선 테스트
    // ==========================================================================

    @Test
    public void keySet_contains_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");

        NavigableSet<Long> keySet = map.navigableKeySet();

        assertTrue(keySet.contains(10L));
        assertTrue(keySet.contains(20L));
        assertFalse(keySet.contains(30L));
    }

    @Test
    public void keySet_remove_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");

        NavigableSet<Long> keySet = map.navigableKeySet();
        boolean removed = keySet.remove(10L);
        assertTrue(removed);
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void keySet_isEmpty_size_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        NavigableSet<Long> keySet = map.navigableKeySet();

        assertTrue(keySet.isEmpty());
        assertEquals(0, keySet.size());

        map.put(10L, "A");
        assertFalse(keySet.isEmpty());
        assertEquals(1, keySet.size());
    }

    @Test
    public void keySet_pollFirst_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableSet<Long> keySet = map.navigableKeySet();
        Long polled = keySet.pollFirst();
        assertEquals(Long.valueOf(10L), polled);
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void keySet_pollLast_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableSet<Long> keySet = map.navigableKeySet();
        Long polled = keySet.pollLast();
        assertEquals(Long.valueOf(30L), polled);
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void keySet_firstLast_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableSet<Long> keySet = map.navigableKeySet();
        assertEquals(Long.valueOf(10L), keySet.first());
        assertEquals(Long.valueOf(30L), keySet.last());
    }

    // ==========================================================================
    // HeadSetView / TailSetView 커버리지 개선 테스트
    // ==========================================================================

    @Test
    public void headSet_contains_shouldRespectRange() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> headSet = set.headSet(25L, true);

        assertTrue(headSet.contains(10L));
        assertTrue(headSet.contains(20L));
        assertFalse(headSet.contains(30L));
    }

    @Test
    public void headSet_add_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);

        NavigableSet<Long> headSet = set.headSet(30L, true);
        headSet.add(20L);
        assertTrue(set.contains(20L));
        assertEquals(2, set.size());
    }

    @Test
    public void headSet_remove_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);

        NavigableSet<Long> headSet = set.headSet(30L, true);
        boolean removed = headSet.remove(10L);
        assertTrue(removed);
        assertFalse(set.contains(10L));
    }

    @Test
    public void headSet_isEmpty_size_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        NavigableSet<Long> headSet = set.headSet(30L, true);

        assertTrue(headSet.isEmpty());
        assertEquals(0, headSet.size());

        set.add(10L);
        set.add(20L);
        set.add(40L); // 범위 외

        assertFalse(headSet.isEmpty());
        assertEquals(2, headSet.size());
    }

    @Test
    public void headSet_pollFirst_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> headSet = set.headSet(35L, true);
        Long polled = headSet.pollFirst();
        assertEquals(Long.valueOf(10L), polled);
        assertFalse(set.contains(10L));
    }

    @Test
    public void headSet_pollLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> headSet = set.headSet(35L, true);
        Long polled = headSet.pollLast();
        assertEquals(Long.valueOf(30L), polled);
        assertFalse(set.contains(30L));
    }

    @Test
    public void headSet_firstLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> headSet = set.headSet(35L, true);

        assertEquals(Long.valueOf(10L), headSet.first());
        assertEquals(Long.valueOf(30L), headSet.last());
    }

    @Test
    public void tailSet_contains_shouldRespectRange() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> tailSet = set.tailSet(15L, true);

        assertFalse(tailSet.contains(10L));
        assertTrue(tailSet.contains(20L));
        assertTrue(tailSet.contains(30L));
    }

    @Test
    public void tailSet_add_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(20L);

        NavigableSet<Long> tailSet = set.tailSet(15L, true);
        tailSet.add(30L);
        assertTrue(set.contains(30L));
        assertEquals(2, set.size());
    }

    @Test
    public void tailSet_remove_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(20L);
        set.add(30L);

        NavigableSet<Long> tailSet = set.tailSet(15L, true);
        boolean removed = tailSet.remove(20L);
        assertTrue(removed);
        assertFalse(set.contains(20L));
    }

    @Test
    public void tailSet_isEmpty_size_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        NavigableSet<Long> tailSet = set.tailSet(15L, true);

        assertTrue(tailSet.isEmpty());
        assertEquals(0, tailSet.size());

        set.add(10L); // 범위 외
        set.add(20L);
        set.add(30L);

        assertFalse(tailSet.isEmpty());
        assertEquals(2, tailSet.size());
    }

    @Test
    public void tailSet_pollFirst_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> tailSet = set.tailSet(15L, true);
        Long polled = tailSet.pollFirst();
        assertEquals(Long.valueOf(20L), polled);
        assertFalse(set.contains(20L));
    }

    @Test
    public void tailSet_pollLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> tailSet = set.tailSet(15L, true);
        Long polled = tailSet.pollLast();
        assertEquals(Long.valueOf(40L), polled);
        assertFalse(set.contains(40L));
    }

    @Test
    public void tailSet_firstLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> tailSet = set.tailSet(15L, true);

        assertEquals(Long.valueOf(20L), tailSet.first());
        assertEquals(Long.valueOf(40L), tailSet.last());
    }

    // ==========================================================================
    // SubSetView 커버리지 개선 테스트
    // ==========================================================================

    @Test
    public void subSet_contains_shouldRespectRange() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> subSet = set.subSet(15L, true, 35L, true);

        assertFalse(subSet.contains(10L));
        assertTrue(subSet.contains(20L));
        assertTrue(subSet.contains(30L));
        assertFalse(subSet.contains(40L));
    }

    @Test
    public void subSet_add_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(20L);

        NavigableSet<Long> subSet = set.subSet(15L, true, 35L, true);
        subSet.add(25L);
        assertTrue(set.contains(25L));
        assertEquals(2, set.size());
    }

    @Test
    public void subSet_remove_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(20L);

        NavigableSet<Long> subSet = set.subSet(15L, true, 35L, true);
        boolean removed = subSet.remove(20L);
        assertTrue(removed);
        assertFalse(set.contains(20L));
    }

    @Test
    public void subSet_pollFirst_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> subSet = set.subSet(15L, true, 35L, true);
        Long polled = subSet.pollFirst();
        assertEquals(Long.valueOf(20L), polled);
        assertFalse(set.contains(20L));
    }

    @Test
    public void subSet_pollLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> subSet = set.subSet(15L, true, 35L, true);
        Long polled = subSet.pollLast();
        assertEquals(Long.valueOf(30L), polled);
        assertFalse(set.contains(30L));
    }

    @Test
    public void subSet_firstLast_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        set.add(40L);

        NavigableSet<Long> subSet = set.subSet(15L, true, 35L, true);

        assertEquals(Long.valueOf(20L), subSet.first());
        assertEquals(Long.valueOf(30L), subSet.last());
    }

    // ==========================================================================
    // 뷰 UnsupportedOperationException 추가 테스트
    // ==========================================================================

    @Test
    public void descendingMap_headMap_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        NavigableMap<Long, String> descMap = map.descendingMap();
        NavigableMap<Long, String> headMap = descMap.headMap(15L, true);
        assertNotNull(headMap);
        // In descending view, headMap(15) gets elements >= 15 in ascending order
        assertEquals(2, headMap.size());
    }

    @Test
    public void descendingMap_tailMap_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        NavigableMap<Long, String> descMap = map.descendingMap();
        NavigableMap<Long, String> tailMap = descMap.tailMap(25L, true);
        assertNotNull(tailMap);
        // In descending view, tailMap(25) gets elements <= 25 in ascending order
        assertEquals(2, tailMap.size());
    }

    @Test
    public void descendingSet_subSet_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        NavigableSet<Long> descSet = set.descendingSet();
        // In descending view, subSet(from=25, to=5) means elements from 25 to 5 in descending order
        NavigableSet<Long> subSet = descSet.subSet(25L, true, 5L, true);
        assertNotNull(subSet);
        assertEquals(2, subSet.size());
    }

    @Test
    public void descendingSet_headSet_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        NavigableSet<Long> descSet = set.descendingSet();
        NavigableSet<Long> headSet = descSet.headSet(15L, true);
        assertNotNull(headSet);
        assertEquals(2, headSet.size());
    }

    @Test
    public void descendingSet_tailSet_shouldWork() {
        NavigableSet<Long> set = store.createSet("set", Long.class);
        set.add(10L);
        set.add(20L);
        set.add(30L);
        NavigableSet<Long> descSet = set.descendingSet();
        NavigableSet<Long> tailSet = descSet.tailSet(25L, true);
        assertNotNull(tailSet);
        assertEquals(2, tailSet.size());
    }

    // ==========================================================================
    // 추가 커버리지 테스트 - SubMapView
    // ==========================================================================

    @Test
    public void subMap_containsKey_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);

        assertFalse(subMap.containsKey(10L));
        assertTrue(subMap.containsKey(20L));
        assertTrue(subMap.containsKey(30L));
        assertFalse(subMap.containsKey(40L));
    }

    @Test
    public void subMap_containsValue_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);

        assertFalse(subMap.containsValue("A"));
        assertTrue(subMap.containsValue("B"));
        assertTrue(subMap.containsValue("C"));
        assertFalse(subMap.containsValue("D"));
    }

    @Test
    public void subMap_get_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);

        assertNull(subMap.get(10L));
        assertEquals("B", subMap.get(20L));
        assertEquals("C", subMap.get(30L));
        assertNull(subMap.get(40L));
    }

    @Test
    public void subMap_put_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(20L, "B");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);
        subMap.put(25L, "X");
        assertTrue(map.containsKey(25L));
        assertEquals("X", map.get(25L));
    }

    @Test
    public void subMap_remove_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(20L, "B");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);
        String removed = subMap.remove(20L);
        assertEquals("B", removed);
        assertFalse(map.containsKey(20L));
    }

    @Test
    public void subMap_values_shouldBeInRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);
        Collection<String> values = subMap.values();

        assertEquals(2, values.size());
        assertTrue(values.contains("B"));
        assertTrue(values.contains("C"));
        assertFalse(values.contains("A"));
        assertFalse(values.contains("D"));
    }

    @Test
    public void subMap_pollFirstEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);
        Map.Entry<Long, String> polled = subMap.pollFirstEntry();
        assertEquals(Long.valueOf(20L), polled.getKey());
        assertFalse(map.containsKey(20L));
    }

    @Test
    public void subMap_pollLastEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);
        Map.Entry<Long, String> polled = subMap.pollLastEntry();
        assertEquals(Long.valueOf(30L), polled.getKey());
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void subMap_firstLastEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");

        NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);

        assertEquals(Long.valueOf(20L), subMap.firstEntry().getKey());
        assertEquals(Long.valueOf(30L), subMap.lastEntry().getKey());
        assertEquals(Long.valueOf(20L), subMap.firstKey());
        assertEquals(Long.valueOf(30L), subMap.lastKey());
    }

    // ==========================================================================
    // 추가 커버리지 테스트 - HeadMapView/TailMapView
    // ==========================================================================

    @Test
    public void headMap_containsKey_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> headMap = map.headMap(25L, true);

        assertTrue(headMap.containsKey(10L));
        assertTrue(headMap.containsKey(20L));
        assertFalse(headMap.containsKey(30L));
    }

    @Test
    public void headMap_containsValue_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> headMap = map.headMap(25L, true);

        assertTrue(headMap.containsValue("A"));
        assertTrue(headMap.containsValue("B"));
        assertFalse(headMap.containsValue("C"));
    }

    @Test
    public void headMap_get_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> headMap = map.headMap(25L, true);

        assertEquals("A", headMap.get(10L));
        assertEquals("B", headMap.get(20L));
        assertNull(headMap.get(30L));
    }

    @Test
    public void headMap_values_shouldBeInRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> headMap = map.headMap(25L, true);
        Collection<String> values = headMap.values();

        assertEquals(2, values.size());
        assertTrue(values.contains("A"));
        assertTrue(values.contains("B"));
        assertFalse(values.contains("C"));
    }

    @Test
    public void headMap_pollFirstEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> headMap = map.headMap(25L, true);
        Map.Entry<Long, String> polled = headMap.pollFirstEntry();
        assertEquals(Long.valueOf(10L), polled.getKey());
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void headMap_pollLastEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> headMap = map.headMap(25L, true);
        Map.Entry<Long, String> polled = headMap.pollLastEntry();
        assertEquals(Long.valueOf(20L), polled.getKey());
        assertFalse(map.containsKey(20L));
    }

    @Test
    public void headMap_firstLastEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> headMap = map.headMap(25L, true);

        assertEquals(Long.valueOf(10L), headMap.firstEntry().getKey());
        assertEquals(Long.valueOf(20L), headMap.lastEntry().getKey());
    }

    @Test
    public void tailMap_containsKey_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> tailMap = map.tailMap(15L, true);

        assertFalse(tailMap.containsKey(10L));
        assertTrue(tailMap.containsKey(20L));
        assertTrue(tailMap.containsKey(30L));
    }

    @Test
    public void tailMap_containsValue_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> tailMap = map.tailMap(15L, true);

        assertFalse(tailMap.containsValue("A"));
        assertTrue(tailMap.containsValue("B"));
        assertTrue(tailMap.containsValue("C"));
    }

    @Test
    public void tailMap_get_shouldRespectRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> tailMap = map.tailMap(15L, true);

        assertNull(tailMap.get(10L));
        assertEquals("B", tailMap.get(20L));
        assertEquals("C", tailMap.get(30L));
    }

    @Test
    public void tailMap_values_shouldBeInRange() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> tailMap = map.tailMap(15L, true);
        Collection<String> values = tailMap.values();

        assertEquals(2, values.size());
        assertFalse(values.contains("A"));
        assertTrue(values.contains("B"));
        assertTrue(values.contains("C"));
    }

    @Test
    public void tailMap_pollFirstEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> tailMap = map.tailMap(15L, true);
        Map.Entry<Long, String> polled = tailMap.pollFirstEntry();
        assertEquals(Long.valueOf(20L), polled.getKey());
        assertFalse(map.containsKey(20L));
    }

    @Test
    public void tailMap_pollLastEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> tailMap = map.tailMap(15L, true);
        Map.Entry<Long, String> polled = tailMap.pollLastEntry();
        assertEquals(Long.valueOf(30L), polled.getKey());
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void tailMap_firstLastEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");

        NavigableMap<Long, String> tailMap = map.tailMap(15L, true);

        assertEquals(Long.valueOf(20L), tailMap.firstEntry().getKey());
        assertEquals(Long.valueOf(30L), tailMap.lastEntry().getKey());
    }
}
