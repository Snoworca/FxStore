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
 * FxNavigableMapImpl 미커버 브랜치 테스트
 *
 * 커버리지 개선 대상:
 * - SubMapView 경계 조건
 * - HeadMapView 경계 조건
 * - TailMapView 경계 조건
 * - KeySetView 작업
 * - DescendingMapView 작업
 */
public class FxNavigableMapCoverageTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("map-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== SubMapView 테스트 ====================

    @Test
    public void subMap_basic_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // subMap [3, 7)
        NavigableMap<Long, String> sub = map.subMap(3L, true, 7L, false);

        assertEquals(4, sub.size()); // 3, 4, 5, 6
        assertTrue(sub.containsKey(3L));
        assertTrue(sub.containsKey(6L));
        assertFalse(sub.containsKey(7L));
        assertFalse(sub.containsKey(2L));
    }

    @Test
    public void subMap_inclusiveExclusive_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // subMap (3, 7] - exclusive low, inclusive high
        NavigableMap<Long, String> sub = map.subMap(3L, false, 7L, true);

        assertEquals(4, sub.size()); // 4, 5, 6, 7
        assertFalse(sub.containsKey(3L));
        assertTrue(sub.containsKey(4L));
        assertTrue(sub.containsKey(7L));
    }

    @Test
    public void subMap_firstKey_lastKey_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(3L, true, 7L, true);

        assertEquals(Long.valueOf(3L), sub.firstKey());
        assertEquals(Long.valueOf(7L), sub.lastKey());
    }

    @Test
    public void subMap_put_inRange_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(10L, "ten");

        NavigableMap<Long, String> sub = map.subMap(3L, true, 8L, true);
        sub.put(5L, "five");

        assertTrue(map.containsKey(5L)); // reflected in parent
        assertTrue(sub.containsKey(5L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_put_outOfRange_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(10L, "ten");

        NavigableMap<Long, String> sub = map.subMap(3L, true, 8L, true);
        sub.put(100L, "out"); // out of range
    }

    @Test
    public void subMap_remove_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(3L, true, 7L, true);
        sub.remove(5L);

        assertFalse(map.containsKey(5L)); // reflected in parent
        assertEquals(9, map.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void subMap_clear_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(3L, true, 7L, true);
        sub.clear(); // FxNavigableMapImpl subMap doesn't support clear
    }

    @Test
    public void subMap_lowerKey_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(3L, true, 7L, true);

        assertEquals(Long.valueOf(4L), sub.lowerKey(5L));
        assertEquals(Long.valueOf(3L), sub.lowerKey(4L));
        assertNull(sub.lowerKey(3L)); // no lower in range
    }

    @Test
    public void subMap_higherKey_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(3L, true, 7L, true);

        assertEquals(Long.valueOf(6L), sub.higherKey(5L));
        assertEquals(Long.valueOf(7L), sub.higherKey(6L));
        assertNull(sub.higherKey(7L)); // no higher in range
    }

    @Test
    public void subMap_floorKey_ceilingKey_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(2L, "two");
        map.put(4L, "four");
        map.put(6L, "six");
        map.put(8L, "eight");

        NavigableMap<Long, String> sub = map.subMap(3L, true, 7L, true);

        assertEquals(Long.valueOf(4L), sub.floorKey(5L)); // floor of 5 is 4
        assertEquals(Long.valueOf(6L), sub.ceilingKey(5L)); // ceiling of 5 is 6
    }

    @Test
    public void subMap_pollFirstEntry_pollLastEntry_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(3L, true, 7L, true);

        Map.Entry<Long, String> first = sub.pollFirstEntry();
        assertEquals(Long.valueOf(3L), first.getKey());
        assertFalse(map.containsKey(3L));

        Map.Entry<Long, String> last = sub.pollLastEntry();
        assertEquals(Long.valueOf(7L), last.getKey());
        assertFalse(map.containsKey(7L));
    }

    // ==================== HeadMapView 테스트 ====================

    @Test
    public void headMap_basic_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // headMap < 5 (exclusive)
        NavigableMap<Long, String> head = map.headMap(5L, false);

        assertEquals(4, head.size()); // 1, 2, 3, 4
        assertTrue(head.containsKey(4L));
        assertFalse(head.containsKey(5L));
    }

    @Test
    public void headMap_inclusive_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // headMap <= 5 (inclusive)
        NavigableMap<Long, String> head = map.headMap(5L, true);

        assertEquals(5, head.size()); // 1, 2, 3, 4, 5
        assertTrue(head.containsKey(5L));
    }

    @Test
    public void headMap_firstKey_lastKey_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> head = map.headMap(5L, true);

        assertEquals(Long.valueOf(1L), head.firstKey());
        assertEquals(Long.valueOf(5L), head.lastKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void headMap_put_outOfRange_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        NavigableMap<Long, String> head = map.headMap(5L, false);
        head.put(10L, "out"); // out of range
    }

    @Test
    public void headMap_lowerHigher_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> head = map.headMap(5L, true);

        assertEquals(Long.valueOf(3L), head.lowerKey(4L));
        assertEquals(Long.valueOf(5L), head.higherKey(4L));
        assertNull(head.higherKey(5L)); // no higher in range
    }

    // ==================== TailMapView 테스트 ====================

    @Test
    public void tailMap_basic_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // tailMap >= 5 (inclusive)
        NavigableMap<Long, String> tail = map.tailMap(5L, true);

        assertEquals(6, tail.size()); // 5, 6, 7, 8, 9, 10
        assertTrue(tail.containsKey(5L));
        assertTrue(tail.containsKey(10L));
        assertFalse(tail.containsKey(4L));
    }

    @Test
    public void tailMap_exclusive_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // tailMap > 5 (exclusive)
        NavigableMap<Long, String> tail = map.tailMap(5L, false);

        assertEquals(5, tail.size()); // 6, 7, 8, 9, 10
        assertFalse(tail.containsKey(5L));
        assertTrue(tail.containsKey(6L));
    }

    @Test
    public void tailMap_firstKey_lastKey_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> tail = map.tailMap(5L, true);

        assertEquals(Long.valueOf(5L), tail.firstKey());
        assertEquals(Long.valueOf(10L), tail.lastKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void tailMap_put_outOfRange_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");

        NavigableMap<Long, String> tail = map.tailMap(5L, true);
        tail.put(1L, "out"); // out of range
    }

    @Test
    public void tailMap_lowerHigher_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> tail = map.tailMap(5L, true);

        assertEquals(Long.valueOf(5L), tail.lowerKey(6L));
        assertNull(tail.lowerKey(5L)); // no lower in range
        assertEquals(Long.valueOf(10L), tail.higherKey(9L));
    }

    // ==================== DescendingMap 테스트 ====================

    @Test
    public void descendingMap_basic_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 5; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> desc = map.descendingMap();

        assertEquals(Long.valueOf(5L), desc.firstKey());
        assertEquals(Long.valueOf(1L), desc.lastKey());
    }

    @Test
    public void descendingMap_iteration_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 5; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> desc = map.descendingMap();

        List<Long> keys = new ArrayList<>(desc.keySet());
        assertEquals(Arrays.asList(5L, 4L, 3L, 2L, 1L), keys);
    }

    @Test
    public void descendingMap_lowerHigher_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 5; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> desc = map.descendingMap();

        // In descending order, "lower" is actually higher numerically
        assertEquals(Long.valueOf(4L), desc.lowerKey(3L));  // 4 > 3
        assertEquals(Long.valueOf(2L), desc.higherKey(3L)); // 2 < 3
    }

    @Test
    public void descendingMap_put_shouldReflectInParent() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        NavigableMap<Long, String> desc = map.descendingMap();
        desc.put(2L, "two");

        assertTrue(map.containsKey(2L));
        assertEquals(2, map.size());
    }

    // ==================== KeySetView 테스트 ====================

    @Test
    public void keySet_basic_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 5; i++) {
            map.put(i, "value" + i);
        }

        NavigableSet<Long> keys = map.navigableKeySet();

        assertEquals(5, keys.size());
        assertTrue(keys.contains(3L));
        assertEquals(Long.valueOf(1L), keys.first());
        assertEquals(Long.valueOf(5L), keys.last());
    }

    @Test
    public void keySet_lowerHigher_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 5; i++) {
            map.put(i, "value" + i);
        }

        NavigableSet<Long> keys = map.navigableKeySet();

        assertEquals(Long.valueOf(2L), keys.lower(3L));
        assertEquals(Long.valueOf(4L), keys.higher(3L));
        assertEquals(Long.valueOf(3L), keys.floor(3L));
        assertEquals(Long.valueOf(3L), keys.ceiling(3L));
    }

    @Test
    public void keySet_pollFirst_pollLast_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 5; i++) {
            map.put(i, "value" + i);
        }

        NavigableSet<Long> keys = map.navigableKeySet();

        assertEquals(Long.valueOf(1L), keys.pollFirst());
        assertFalse(map.containsKey(1L));

        assertEquals(Long.valueOf(5L), keys.pollLast());
        assertFalse(map.containsKey(5L));
    }

    @Test
    public void keySet_descendingSet_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 5; i++) {
            map.put(i, "value" + i);
        }

        NavigableSet<Long> descKeys = map.navigableKeySet().descendingSet();

        assertEquals(Long.valueOf(5L), descKeys.first());
        assertEquals(Long.valueOf(1L), descKeys.last());
    }

    // ==================== 빈 맵 엣지 케이스 테스트 ====================

    @Test(expected = NoSuchElementException.class)
    public void map_firstKey_empty_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("empty", Long.class, String.class);
        map.firstKey();
    }

    @Test(expected = NoSuchElementException.class)
    public void map_lastKey_empty_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("empty", Long.class, String.class);
        map.lastKey();
    }

    @Test
    public void map_pollFirstEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("empty", Long.class, String.class);
        assertNull(map.pollFirstEntry());
    }

    @Test
    public void map_pollLastEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("empty", Long.class, String.class);
        assertNull(map.pollLastEntry());
    }

    // ==================== 중첩 뷰 테스트 ====================

    @Test
    public void nestedSubMap_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 20; i++) {
            map.put(i, "value" + i);
        }

        // Nested views
        NavigableMap<Long, String> sub1 = map.subMap(5L, true, 15L, true);
        NavigableMap<Long, String> sub2 = sub1.subMap(8L, true, 12L, true);

        assertEquals(5, sub2.size()); // 8, 9, 10, 11, 12
        assertEquals(Long.valueOf(8L), sub2.firstKey());
        assertEquals(Long.valueOf(12L), sub2.lastKey());
    }

    @Test
    public void headMap_on_tailMap_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 20; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> tail = map.tailMap(5L, true);
        NavigableMap<Long, String> head = tail.headMap(15L, true);

        assertEquals(11, head.size()); // 5 to 15
        assertEquals(Long.valueOf(5L), head.firstKey());
        assertEquals(Long.valueOf(15L), head.lastKey());
    }

    // ==================== entrySet 테스트 ====================

    @Test
    public void entrySet_iteration_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");
        map.put(3L, "three");

        int count = 0;
        for (Map.Entry<Long, String> entry : map.entrySet()) {
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void values_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");

        Collection<String> values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.contains("one"));
        assertTrue(values.contains("two"));
    }

    // ==================== compute / merge 테스트 ====================

    @Test
    public void putIfAbsent_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "existing");

        String result1 = map.putIfAbsent(1L, "new");
        assertEquals("existing", result1);
        assertEquals("existing", map.get(1L));

        String result2 = map.putIfAbsent(2L, "new");
        assertNull(result2);
        assertEquals("new", map.get(2L));
    }

    @Test
    public void getOrDefault_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        assertEquals("one", map.getOrDefault(1L, "default"));
        assertEquals("default", map.getOrDefault(999L, "default"));
    }

    // ==================== putAll 테스트 ====================

    @Test
    public void putAll_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

        Map<Long, String> source = new HashMap<>();
        source.put(1L, "one");
        source.put(2L, "two");
        source.put(3L, "three");

        map.putAll(source);

        assertEquals(3, map.size());
        assertEquals("one", map.get(1L));
        assertEquals("two", map.get(2L));
        assertEquals("three", map.get(3L));
    }

    @Test
    public void putAll_emptyMap_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "existing");

        map.putAll(new HashMap<>());

        assertEquals(1, map.size());
    }

    @Test
    public void putAll_overwriteExisting_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "old");

        Map<Long, String> source = new HashMap<>();
        source.put(1L, "new");
        source.put(2L, "two");

        map.putAll(source);

        assertEquals(2, map.size());
        assertEquals("new", map.get(1L));
    }

    // ==================== headMap/tailMap 단일 인자 테스트 ====================

    @Test
    public void headMap_singleArg_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // headMap(5L) - exclusive by default
        SortedMap<Long, String> head = map.headMap(5L);

        assertEquals(4, head.size()); // 1, 2, 3, 4
        assertTrue(head.containsKey(4L));
        assertFalse(head.containsKey(5L));
    }

    @Test
    public void tailMap_singleArg_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // tailMap(5L) - inclusive by default
        SortedMap<Long, String> tail = map.tailMap(5L);

        assertEquals(6, tail.size()); // 5, 6, 7, 8, 9, 10
        assertTrue(tail.containsKey(5L));
        assertFalse(tail.containsKey(4L));
    }

    @Test
    public void subMap_singleArg_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // subMap(3L, 7L) - fromInclusive, toExclusive by default
        SortedMap<Long, String> sub = map.subMap(3L, 7L);

        assertEquals(4, sub.size()); // 3, 4, 5, 6
        assertTrue(sub.containsKey(3L));
        assertTrue(sub.containsKey(6L));
        assertFalse(sub.containsKey(7L));
    }

    // ==================== SubMapView의 headMap/tailMap 단일 인자 테스트 ====================

    @Test
    public void subMapView_headMap_singleArg_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 20; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(5L, true, 15L, true);
        SortedMap<Long, String> head = sub.headMap(10L);

        assertEquals(5, head.size()); // 5, 6, 7, 8, 9
        assertTrue(head.containsKey(5L));
        assertFalse(head.containsKey(10L));
    }

    @Test
    public void subMapView_tailMap_singleArg_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 20; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(5L, true, 15L, true);
        SortedMap<Long, String> tail = sub.tailMap(10L);

        assertEquals(6, tail.size()); // 10, 11, 12, 13, 14, 15
        assertTrue(tail.containsKey(10L));
        assertTrue(tail.containsKey(15L));
    }

    @Test
    public void subMapView_subMap_singleArg_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 20; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(5L, true, 15L, true);
        SortedMap<Long, String> subSub = sub.subMap(8L, 12L);

        assertEquals(4, subSub.size()); // 8, 9, 10, 11
        assertTrue(subSub.containsKey(8L));
        assertFalse(subSub.containsKey(12L));
    }

    // ==================== comparator 테스트 ====================

    @Test
    public void comparator_shouldReturnComparator() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        // FxNavigableMap uses a byte comparator
        Comparator<? super Long> comp = map.comparator();
        // comparator may be null or non-null depending on implementation
        if (comp != null) {
            // Verify it works correctly
            assertTrue(comp.compare(1L, 2L) < 0);
            assertTrue(comp.compare(2L, 1L) > 0);
            assertEquals(0, comp.compare(1L, 1L));
        }
    }

    @Test
    public void subMap_comparator_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(10L, "ten");

        NavigableMap<Long, String> sub = map.subMap(3L, true, 8L, true);
        Comparator<? super Long> comp = sub.comparator();
        // Verify comparator works if present
        if (comp != null) {
            assertTrue(comp.compare(3L, 7L) < 0);
        }
    }
}
