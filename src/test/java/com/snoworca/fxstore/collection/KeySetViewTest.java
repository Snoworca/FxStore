package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxOptions;
import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

/**
 * KeySetView 커버리지 테스트 (P2)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>FxNavigableMapImpl.KeySetView (83% → 90%+)</li>
 * </ul>
 *
 * @since 0.9
 * @see FxNavigableMapImpl
 */
public class KeySetViewTest {

    private File tempFile;
    private FxStore store;
    private NavigableMap<Long, String> map;
    private NavigableSet<Long> keySet;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-keyset-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());
        map = store.createMap("testMap", Long.class, String.class);

        // 기본 데이터: {10=A, 20=B, 30=C, 40=D, 50=E}
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");
        map.put(50L, "E");

        keySet = map.navigableKeySet();
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
    public void size_shouldReturnMapSize() {
        assertEquals(5, keySet.size());
    }

    @Test
    public void contains_existingKey_shouldReturnTrue() {
        assertTrue(keySet.contains(30L));
    }

    @Test
    public void contains_nonExistingKey_shouldReturnFalse() {
        assertFalse(keySet.contains(100L));
    }

    @Test
    public void isEmpty_nonEmpty_shouldReturnFalse() {
        assertFalse(keySet.isEmpty());
    }

    @Test
    public void isEmpty_empty_shouldReturnTrue() {
        map.clear();
        assertTrue(keySet.isEmpty());
    }

    // ==================== iterator 테스트 ====================

    @Test
    public void iterator_shouldReturnKeysInOrder() {
        Iterator<Long> it = keySet.iterator();
        List<Long> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        assertEquals(Arrays.asList(10L, 20L, 30L, 40L, 50L), keys);
    }

    @Test
    public void descendingIterator_shouldReturnKeysInReverseOrder() {
        Iterator<Long> it = keySet.descendingIterator();
        List<Long> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        assertEquals(Arrays.asList(50L, 40L, 30L, 20L, 10L), keys);
    }

    // ==================== comparator 테스트 ====================

    @Test
    public void comparator_shouldReturnNonNull() {
        Comparator<? super Long> cmp = keySet.comparator();
        assertNotNull(cmp);
    }

    @Test
    public void comparator_shouldPreserveOrder() {
        Comparator<? super Long> cmp = keySet.comparator();
        assertTrue(cmp.compare(10L, 20L) < 0);
        assertTrue(cmp.compare(50L, 30L) > 0);
        assertEquals(0, cmp.compare(30L, 30L));
    }

    // ==================== first/last 테스트 ====================

    @Test
    public void first_shouldReturnSmallestKey() {
        assertEquals(Long.valueOf(10L), keySet.first());
    }

    @Test
    public void last_shouldReturnLargestKey() {
        assertEquals(Long.valueOf(50L), keySet.last());
    }

    @Test(expected = NoSuchElementException.class)
    public void first_empty_shouldThrow() {
        map.clear();
        keySet.first();
    }

    @Test(expected = NoSuchElementException.class)
    public void last_empty_shouldThrow() {
        map.clear();
        keySet.last();
    }

    // ==================== lower/floor/ceiling/higher 테스트 ====================

    @Test
    public void lower_shouldReturnPreviousKey() {
        assertEquals(Long.valueOf(20L), keySet.lower(30L));
        assertEquals(Long.valueOf(30L), keySet.lower(40L));
    }

    @Test
    public void lower_smallerThanFirst_shouldReturnNull() {
        assertNull(keySet.lower(10L));
        assertNull(keySet.lower(5L));
    }

    @Test
    public void floor_existingKey_shouldReturnSameKey() {
        assertEquals(Long.valueOf(30L), keySet.floor(30L));
    }

    @Test
    public void floor_nonExistingKey_shouldReturnPrevious() {
        assertEquals(Long.valueOf(30L), keySet.floor(35L));
    }

    @Test
    public void ceiling_existingKey_shouldReturnSameKey() {
        assertEquals(Long.valueOf(30L), keySet.ceiling(30L));
    }

    @Test
    public void ceiling_nonExistingKey_shouldReturnNext() {
        assertEquals(Long.valueOf(30L), keySet.ceiling(25L));
    }

    @Test
    public void higher_shouldReturnNextKey() {
        assertEquals(Long.valueOf(40L), keySet.higher(30L));
        assertEquals(Long.valueOf(30L), keySet.higher(20L));
    }

    @Test
    public void higher_largerThanLast_shouldReturnNull() {
        assertNull(keySet.higher(50L));
        assertNull(keySet.higher(100L));
    }

    // ==================== pollFirst/pollLast 테스트 (UOE 개선) ====================

    @Test
    public void pollFirst_shouldWork() {
        Long first = keySet.pollFirst();
        assertEquals(Long.valueOf(10L), first);
        assertFalse(map.containsKey(10L));
        assertEquals(4, map.size());
    }

    @Test
    public void pollLast_shouldWork() {
        Long last = keySet.pollLast();
        assertEquals(Long.valueOf(50L), last);
        assertFalse(map.containsKey(50L));
        assertEquals(4, map.size());
    }

    // ==================== descendingSet 테스트 ====================

    @Test
    public void descendingSet_shouldReturnReverseOrder() {
        NavigableSet<Long> descSet = keySet.descendingSet();
        assertEquals(Long.valueOf(50L), descSet.first());
        assertEquals(Long.valueOf(10L), descSet.last());
    }

    @Test
    public void descendingSet_iterator_shouldBeReversed() {
        NavigableSet<Long> descSet = keySet.descendingSet();
        Iterator<Long> it = descSet.iterator();
        List<Long> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        assertEquals(Arrays.asList(50L, 40L, 30L, 20L, 10L), keys);
    }

    @Test
    public void descendingSet_descendingIterator_shouldBeOriginalOrder() {
        NavigableSet<Long> descSet = keySet.descendingSet();
        Iterator<Long> it = descSet.descendingIterator();
        List<Long> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        assertEquals(Arrays.asList(10L, 20L, 30L, 40L, 50L), keys);
    }

    @Test
    public void descendingSet_lower_shouldBeReversed() {
        NavigableSet<Long> descSet = keySet.descendingSet();
        // descending에서 lower(30)은 40 (원래 순서에서 higher)
        assertEquals(Long.valueOf(40L), descSet.lower(30L));
    }

    @Test
    public void descendingSet_higher_shouldBeReversed() {
        NavigableSet<Long> descSet = keySet.descendingSet();
        // descending에서 higher(30)은 20 (원래 순서에서 lower)
        assertEquals(Long.valueOf(20L), descSet.higher(30L));
    }

    @Test
    public void descendingSet_floor_shouldBeReversed() {
        NavigableSet<Long> descSet = keySet.descendingSet();
        // descending에서 floor(35)은 40 (원래 순서에서 ceiling)
        assertEquals(Long.valueOf(40L), descSet.floor(35L));
    }

    @Test
    public void descendingSet_ceiling_shouldBeReversed() {
        NavigableSet<Long> descSet = keySet.descendingSet();
        // descending에서 ceiling(35)은 30 (원래 순서에서 floor)
        assertEquals(Long.valueOf(30L), descSet.ceiling(35L));
    }

    @Test
    public void descendingSet_comparator_shouldBeReversed() {
        NavigableSet<Long> descSet = keySet.descendingSet();
        Comparator<? super Long> cmp = descSet.comparator();
        assertTrue(cmp.compare(10L, 20L) > 0); // reversed
        assertTrue(cmp.compare(50L, 30L) < 0); // reversed
    }

    // ==================== subSet/headSet/tailSet 테스트 (UOE 개선) ====================

    @Test
    public void subSet_shouldWork() {
        NavigableSet<Long> sub = keySet.subSet(20L, true, 40L, true);
        assertEquals(3, sub.size());
        assertTrue(sub.contains(20L));
        assertTrue(sub.contains(30L));
        assertTrue(sub.contains(40L));
    }

    @Test
    public void headSet_shouldWork() {
        NavigableSet<Long> head = keySet.headSet(40L, true);
        assertEquals(4, head.size());
        assertTrue(head.contains(10L));
        assertTrue(head.contains(40L));
        assertFalse(head.contains(50L));
    }

    @Test
    public void tailSet_shouldWork() {
        NavigableSet<Long> tail = keySet.tailSet(20L, true);
        assertEquals(4, tail.size());
        assertTrue(tail.contains(20L));
        assertTrue(tail.contains(50L));
        assertFalse(tail.contains(10L));
    }

    @Test
    public void subSet_sorted_shouldWork() {
        SortedSet<Long> sub = keySet.subSet(20L, 40L);
        assertEquals(2, sub.size());
        assertTrue(sub.contains(20L));
        assertTrue(sub.contains(30L));
        assertFalse(sub.contains(40L));
    }

    @Test
    public void headSet_sorted_shouldWork() {
        SortedSet<Long> head = keySet.headSet(40L);
        assertEquals(3, head.size());
        assertTrue(head.contains(10L));
        assertTrue(head.contains(30L));
        assertFalse(head.contains(40L));
    }

    @Test
    public void tailSet_sorted_shouldWork() {
        SortedSet<Long> tail = keySet.tailSet(20L);
        assertEquals(4, tail.size());
        assertTrue(tail.contains(20L));
        assertTrue(tail.contains(50L));
        assertFalse(tail.contains(10L));
    }

    // ==================== toArray 테스트 ====================

    @Test
    public void toArray_shouldReturnAllKeys() {
        Object[] arr = keySet.toArray();
        assertEquals(5, arr.length);
        assertEquals(10L, arr[0]);
        assertEquals(50L, arr[4]);
    }

    @Test
    public void toArray_typed_shouldReturnTypedArray() {
        Long[] arr = keySet.toArray(new Long[0]);
        assertEquals(5, arr.length);
        assertEquals(Long.valueOf(10L), arr[0]);
        assertEquals(Long.valueOf(50L), arr[4]);
    }

    // ==================== 추가 coverage 테스트 ====================

    @Test
    public void descendingSet_twice_shouldRestoreOriginalOrder() {
        NavigableSet<Long> desc1 = keySet.descendingSet();
        NavigableSet<Long> desc2 = desc1.descendingSet();

        // 두 번 descending하면 원래 순서
        assertEquals(Long.valueOf(10L), desc2.first());
        assertEquals(Long.valueOf(50L), desc2.last());
    }

    @Test
    public void keySet_fromEmptyMap_shouldBeEmpty() {
        NavigableMap<Long, String> emptyMap = store.createMap("emptyMap", Long.class, String.class);
        NavigableSet<Long> emptyKeys = emptyMap.navigableKeySet();
        assertTrue(emptyKeys.isEmpty());
        assertEquals(0, emptyKeys.size());
    }

    @Test
    public void keySet_afterMapUpdate_shouldReflectChanges() {
        map.put(60L, "F");
        assertTrue(keySet.contains(60L));
        assertEquals(6, keySet.size());
    }

    @Test
    public void keySet_afterMapRemove_shouldReflectChanges() {
        map.remove(30L);
        assertFalse(keySet.contains(30L));
        assertEquals(4, keySet.size());
    }
}
