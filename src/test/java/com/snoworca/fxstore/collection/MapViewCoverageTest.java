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
 * NavigableMap View 커버리지 개선 테스트 (P1)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>FxNavigableMapImpl.TailMapView (63% → 85%+)</li>
 *   <li>FxNavigableMapImpl.HeadMapView (64% → 85%+)</li>
 *   <li>FxNavigableMapImpl.KeySetView (65% → 85%+)</li>
 *   <li>FxNavigableMapImpl.DescendingMapView (69% → 85%+)</li>
 * </ul>
 *
 * @since 0.8
 * @see FxNavigableMapImpl
 */
public class MapViewCoverageTest {

    private File tempFile;
    private FxStore store;
    private NavigableMap<Long, String> map;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-mapview-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());
        map = store.createMap("testMap", Long.class, String.class);

        // 기본 데이터: {10→A, 20→B, 30→C, 40→D, 50→E}
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");
        map.put(50L, "E");
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

    // ==================== TailMapView 테스트 (14개) ====================

    // --- Iterator 테스트 ---

    @Test
    public void tailMap_iterator_shouldIterateFromKey() {
        // Given: tailMap(25)
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: iterate
        List<Long> keys = new ArrayList<>();
        for (Long key : tail.keySet()) {
            keys.add(key);
        }

        // Then: [30, 40, 50]
        assertEquals(Arrays.asList(30L, 40L, 50L), keys);
    }

    @Test
    public void tailMap_iterator_inclusive_shouldIncludeBoundary() {
        // Given: tailMap(30, true)
        NavigableMap<Long, String> tail = map.tailMap(30L, true);

        // When: check containsKey
        assertTrue(tail.containsKey(30L));
        assertEquals(3, tail.size());
    }

    @Test
    public void tailMap_iterator_exclusive_shouldExcludeBoundary() {
        // Given: tailMap(30, false)
        NavigableMap<Long, String> tail = map.tailMap(30L, false);

        // When: check containsKey
        assertFalse(tail.containsKey(30L));
        assertEquals(2, tail.size());
    }

    @Test
    public void tailMap_entrySet_shouldReturnEntriesInRange() {
        // Given: tailMap(25)
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: get entrySet
        Set<Map.Entry<Long, String>> entries = tail.entrySet();

        // Then: 3 entries
        assertEquals(3, entries.size());
    }

    // --- Navigation 테스트 ---

    @Test
    public void tailMap_lowerEntry_shouldReturnLowerInRange() {
        // Given: tailMap(25) = {30→C, 40→D, 50→E}
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: lowerEntry(40)
        Map.Entry<Long, String> entry = tail.lowerEntry(40L);

        // Then: 30→C
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
        assertEquals("C", entry.getValue());
    }

    @Test
    public void tailMap_floorEntry_shouldReturnFloorInRange() {
        // Given: tailMap(25) = {30→C, 40→D, 50→E}
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: floorEntry(35)
        Map.Entry<Long, String> entry = tail.floorEntry(35L);

        // Then: 30→C
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
    }

    @Test
    public void tailMap_ceilingEntry_shouldReturnCeilingInRange() {
        // Given: tailMap(25) = {30→C, 40→D, 50→E}
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: ceilingEntry(35)
        Map.Entry<Long, String> entry = tail.ceilingEntry(35L);

        // Then: 40→D
        assertNotNull(entry);
        assertEquals(Long.valueOf(40L), entry.getKey());
    }

    @Test
    public void tailMap_higherEntry_shouldReturnHigherInRange() {
        // Given: tailMap(25) = {30→C, 40→D, 50→E}
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: higherEntry(30)
        Map.Entry<Long, String> entry = tail.higherEntry(30L);

        // Then: 40→D
        assertNotNull(entry);
        assertEquals(Long.valueOf(40L), entry.getKey());
    }

    @Test
    public void tailMap_firstEntry_shouldReturnFirstInRange() {
        // Given: tailMap(25) = {30→C, 40→D, 50→E}
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: firstEntry()
        Map.Entry<Long, String> entry = tail.firstEntry();

        // Then: 30→C
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
    }

    @Test
    public void tailMap_lastEntry_shouldReturnLastInRange() {
        // Given: tailMap(25) = {30→C, 40→D, 50→E}
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: lastEntry()
        Map.Entry<Long, String> entry = tail.lastEntry();

        // Then: 50→E
        assertNotNull(entry);
        assertEquals(Long.valueOf(50L), entry.getKey());
    }

    // --- 경계 및 특수 케이스 테스트 ---

    @Test
    public void tailMap_empty_shouldReturnEmptyView() {
        // Given: tailMap(100)
        NavigableMap<Long, String> tail = map.tailMap(100L, true);

        // Then: empty
        assertTrue(tail.isEmpty());
        assertEquals(0, tail.size());
    }

    @Test
    public void tailMap_get_outOfRange_shouldReturnNull() {
        // Given: tailMap(25), original map has 10→A
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: get(10)
        String value = tail.get(10L);

        // Then: null (범위 밖)
        assertNull(value);
    }

    @Test
    public void tailMap_comparator_shouldReturnParentComparator() {
        // Given: tailMap
        NavigableMap<Long, String> tail = map.tailMap(25L, true);

        // When: comparator()
        Comparator<? super Long> cmp = tail.comparator();

        // Then: not null (natural ordering wrapper)
        assertNotNull(cmp);
    }

    @Test
    public void tailMap_put_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(25L, true);
        tail.put(60L, "F");
        assertTrue(map.containsKey(60L));
        assertEquals("F", map.get(60L));
    }

    // ==================== HeadMapView 테스트 (14개) ====================

    // --- Iterator 테스트 ---

    @Test
    public void headMap_iterator_shouldIterateToKey() {
        // Given: headMap(35)
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: iterate
        List<Long> keys = new ArrayList<>();
        for (Long key : head.keySet()) {
            keys.add(key);
        }

        // Then: [10, 20, 30]
        assertEquals(Arrays.asList(10L, 20L, 30L), keys);
    }

    @Test
    public void headMap_iterator_inclusive_shouldIncludeBoundary() {
        // Given: headMap(30, true)
        NavigableMap<Long, String> head = map.headMap(30L, true);

        // When: check containsKey
        assertTrue(head.containsKey(30L));
        assertEquals(3, head.size());
    }

    @Test
    public void headMap_iterator_exclusive_shouldExcludeBoundary() {
        // Given: headMap(30, false)
        NavigableMap<Long, String> head = map.headMap(30L, false);

        // When: check containsKey
        assertFalse(head.containsKey(30L));
        assertEquals(2, head.size());
    }

    @Test
    public void headMap_entrySet_shouldReturnEntriesInRange() {
        // Given: headMap(35)
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: get entrySet
        Set<Map.Entry<Long, String>> entries = head.entrySet();

        // Then: 3 entries
        assertEquals(3, entries.size());
    }

    // --- Navigation 테스트 ---

    @Test
    public void headMap_lowerEntry_shouldReturnLowerInRange() {
        // Given: headMap(35) = {10→A, 20→B, 30→C}
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: lowerEntry(30)
        Map.Entry<Long, String> entry = head.lowerEntry(30L);

        // Then: 20→B
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());
    }

    @Test
    public void headMap_floorEntry_shouldReturnFloorInRange() {
        // Given: headMap(35) = {10→A, 20→B, 30→C}
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: floorEntry(25)
        Map.Entry<Long, String> entry = head.floorEntry(25L);

        // Then: 20→B
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());
    }

    @Test
    public void headMap_ceilingEntry_shouldReturnCeilingInRange() {
        // Given: headMap(35) = {10→A, 20→B, 30→C}
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: ceilingEntry(15)
        Map.Entry<Long, String> entry = head.ceilingEntry(15L);

        // Then: 20→B
        assertNotNull(entry);
        assertEquals(Long.valueOf(20L), entry.getKey());
    }

    @Test
    public void headMap_higherEntry_shouldReturnHigherInRange() {
        // Given: headMap(35) = {10→A, 20→B, 30→C}
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: higherEntry(20)
        Map.Entry<Long, String> entry = head.higherEntry(20L);

        // Then: 30→C
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
    }

    @Test
    public void headMap_firstEntry_shouldReturnFirstInRange() {
        // Given: headMap(35)
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: firstEntry()
        Map.Entry<Long, String> entry = head.firstEntry();

        // Then: 10→A
        assertNotNull(entry);
        assertEquals(Long.valueOf(10L), entry.getKey());
    }

    @Test
    public void headMap_lastEntry_shouldReturnLastInRange() {
        // Given: headMap(35) = {10→A, 20→B, 30→C}
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: lastEntry()
        Map.Entry<Long, String> entry = head.lastEntry();

        // Then: 30→C
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
    }

    // --- 경계 및 특수 케이스 테스트 ---

    @Test
    public void headMap_empty_shouldReturnEmptyView() {
        // Given: headMap(5)
        NavigableMap<Long, String> head = map.headMap(5L, false);

        // Then: empty
        assertTrue(head.isEmpty());
    }

    @Test
    public void headMap_get_outOfRange_shouldReturnNull() {
        // Given: headMap(35), original map has 50→E
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: get(50)
        String value = head.get(50L);

        // Then: null (범위 밖)
        assertNull(value);
    }

    @Test
    public void headMap_floorEntry_keyAboveRange_shouldReturnLast() {
        // Given: headMap(35) = {10→A, 20→B, 30→C}
        NavigableMap<Long, String> head = map.headMap(35L, false);

        // When: floorEntry(100) - key above range
        Map.Entry<Long, String> entry = head.floorEntry(100L);

        // Then: 30→C (범위 내 가장 큰 값)
        assertNotNull(entry);
        assertEquals(Long.valueOf(30L), entry.getKey());
    }

    @Test
    public void headMap_remove_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(35L, false);
        head.remove(10L);
        assertFalse(map.containsKey(10L));
    }

    // ==================== KeySetView 테스트 (10개) ====================

    @Test
    public void keySet_iterator_shouldIterateAllKeys() {
        // Given: navigableKeySet
        NavigableSet<Long> keySet = map.navigableKeySet();

        // When: iterate
        List<Long> keys = new ArrayList<>();
        for (Long key : keySet) {
            keys.add(key);
        }

        // Then: [10, 20, 30, 40, 50]
        assertEquals(Arrays.asList(10L, 20L, 30L, 40L, 50L), keys);
    }

    @Test
    public void keySet_descendingIterator_shouldIterateReverse() {
        // Given: navigableKeySet
        NavigableSet<Long> keySet = map.navigableKeySet();

        // When: descendingIterator
        List<Long> keys = new ArrayList<>();
        Iterator<Long> it = keySet.descendingIterator();
        while (it.hasNext()) {
            keys.add(it.next());
        }

        // Then: [50, 40, 30, 20, 10]
        assertEquals(Arrays.asList(50L, 40L, 30L, 20L, 10L), keys);
    }

    @Test
    public void keySet_lower_shouldReturnLower() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        assertEquals(Long.valueOf(20L), keySet.lower(30L));
    }

    @Test
    public void keySet_floor_shouldReturnFloor() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        assertEquals(Long.valueOf(30L), keySet.floor(30L));
    }

    @Test
    public void keySet_ceiling_shouldReturnCeiling() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        assertEquals(Long.valueOf(30L), keySet.ceiling(25L));
    }

    @Test
    public void keySet_higher_shouldReturnHigher() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        assertEquals(Long.valueOf(40L), keySet.higher(30L));
    }

    @Test
    public void keySet_first_last_shouldWork() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        assertEquals(Long.valueOf(10L), keySet.first());
        assertEquals(Long.valueOf(50L), keySet.last());
    }

    @Test
    public void keySet_descendingSet_shouldReturnReverse() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> descending = keySet.descendingSet();

        assertEquals(Long.valueOf(50L), descending.first());
        assertEquals(Long.valueOf(10L), descending.last());
    }

    @Test
    public void keySet_pollFirst_shouldWork() {
        Long first = map.navigableKeySet().pollFirst();
        assertEquals(Long.valueOf(10L), first);
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void keySet_subSet_shouldWork() {
        NavigableSet<Long> subSet = map.navigableKeySet().subSet(10L, true, 30L, true);
        assertEquals(3, subSet.size());
        assertTrue(subSet.contains(10L));
        assertTrue(subSet.contains(20L));
        assertTrue(subSet.contains(30L));
    }

    // ==================== DescendingMapView 테스트 (10개) ====================

    @Test
    public void descendingMap_iterator_shouldIterateReverse() {
        // Given: descendingMap
        NavigableMap<Long, String> desc = map.descendingMap();

        // When: iterate
        List<Long> keys = new ArrayList<>();
        for (Long key : desc.keySet()) {
            keys.add(key);
        }

        // Then: [50, 40, 30, 20, 10]
        assertEquals(Arrays.asList(50L, 40L, 30L, 20L, 10L), keys);
    }

    @Test
    public void descendingMap_firstEntry_shouldReturnOriginalLast() {
        NavigableMap<Long, String> desc = map.descendingMap();

        Map.Entry<Long, String> entry = desc.firstEntry();

        assertEquals(Long.valueOf(50L), entry.getKey());
        assertEquals("E", entry.getValue());
    }

    @Test
    public void descendingMap_lastEntry_shouldReturnOriginalFirst() {
        NavigableMap<Long, String> desc = map.descendingMap();

        Map.Entry<Long, String> entry = desc.lastEntry();

        assertEquals(Long.valueOf(10L), entry.getKey());
    }

    @Test
    public void descendingMap_lowerEntry_shouldReturnHigherFromOriginal() {
        NavigableMap<Long, String> desc = map.descendingMap();

        // lower in descending = higher in original
        Map.Entry<Long, String> entry = desc.lowerEntry(30L);

        assertEquals(Long.valueOf(40L), entry.getKey());
    }

    @Test
    public void descendingMap_higherEntry_shouldReturnLowerFromOriginal() {
        NavigableMap<Long, String> desc = map.descendingMap();

        // higher in descending = lower in original
        Map.Entry<Long, String> entry = desc.higherEntry(30L);

        assertEquals(Long.valueOf(20L), entry.getKey());
    }

    @Test
    public void descendingMap_floorEntry_shouldReturnCeilingFromOriginal() {
        NavigableMap<Long, String> desc = map.descendingMap();

        Map.Entry<Long, String> entry = desc.floorEntry(25L);

        assertEquals(Long.valueOf(30L), entry.getKey());
    }

    @Test
    public void descendingMap_ceilingEntry_shouldReturnFloorFromOriginal() {
        NavigableMap<Long, String> desc = map.descendingMap();

        Map.Entry<Long, String> entry = desc.ceilingEntry(25L);

        assertEquals(Long.valueOf(20L), entry.getKey());
    }

    @Test
    public void descendingMap_get_shouldWork() {
        NavigableMap<Long, String> desc = map.descendingMap();

        assertEquals("C", desc.get(30L));
        assertNull(desc.get(100L));
    }

    @Test
    public void descendingMap_containsKey_shouldWork() {
        NavigableMap<Long, String> desc = map.descendingMap();

        assertTrue(desc.containsKey(30L));
        assertFalse(desc.containsKey(100L));
    }

    @Test
    public void descendingMap_descendingMap_shouldReturnOriginal() {
        NavigableMap<Long, String> desc = map.descendingMap();
        NavigableMap<Long, String> original = desc.descendingMap();

        // descendingMap of descendingMap returns original
        List<Long> keys = new ArrayList<>();
        for (Long key : original.keySet()) {
            keys.add(key);
        }

        assertEquals(Arrays.asList(10L, 20L, 30L, 40L, 50L), keys);
    }

    // ==================== 추가 Edge Case 테스트 ====================

    @Test
    public void tailMap_lowerKey_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(25L, true);
        assertEquals(Long.valueOf(30L), tail.lowerKey(40L));
    }

    @Test
    public void tailMap_floorKey_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(25L, true);
        assertEquals(Long.valueOf(30L), tail.floorKey(35L));
    }

    @Test
    public void tailMap_ceilingKey_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(25L, true);
        assertEquals(Long.valueOf(40L), tail.ceilingKey(35L));
    }

    @Test
    public void tailMap_higherKey_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(25L, true);
        assertEquals(Long.valueOf(40L), tail.higherKey(30L));
    }

    @Test
    public void headMap_lowerKey_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(35L, false);
        assertEquals(Long.valueOf(20L), head.lowerKey(30L));
    }

    @Test
    public void headMap_ceilingKey_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(35L, false);
        assertEquals(Long.valueOf(20L), head.ceilingKey(15L));
    }

    @Test
    public void headMap_higherKey_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(35L, false);
        assertEquals(Long.valueOf(30L), head.higherKey(20L));
    }

    @Test
    public void descendingMap_lowerKey_shouldWork() {
        NavigableMap<Long, String> desc = map.descendingMap();
        assertEquals(Long.valueOf(40L), desc.lowerKey(30L));
    }

    @Test
    public void descendingMap_floorKey_shouldWork() {
        NavigableMap<Long, String> desc = map.descendingMap();
        assertEquals(Long.valueOf(30L), desc.floorKey(25L));
    }

    @Test
    public void descendingMap_ceilingKey_shouldWork() {
        NavigableMap<Long, String> desc = map.descendingMap();
        assertEquals(Long.valueOf(20L), desc.ceilingKey(25L));
    }

    @Test
    public void descendingMap_higherKey_shouldWork() {
        NavigableMap<Long, String> desc = map.descendingMap();
        assertEquals(Long.valueOf(20L), desc.higherKey(30L));
    }

    @Test
    public void descendingMap_navigableKeySet_shouldWork() {
        NavigableMap<Long, String> desc = map.descendingMap();
        NavigableSet<Long> keySet = desc.navigableKeySet();

        assertEquals(Long.valueOf(50L), keySet.first());
    }

    @Test
    public void descendingMap_descendingKeySet_shouldWork() {
        NavigableMap<Long, String> desc = map.descendingMap();
        NavigableSet<Long> keySet = desc.descendingKeySet();

        assertEquals(Long.valueOf(10L), keySet.first());
    }

    @Test
    public void keySet_comparator_shouldWork() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        assertNotNull(keySet.comparator());
    }

    @Test
    public void keySet_contains_shouldWork() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        assertTrue(keySet.contains(30L));
        assertFalse(keySet.contains(100L));
    }

    @Test
    public void keySet_size_shouldWork() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        assertEquals(5, keySet.size());
    }

    @Test
    public void descendingMap_size_isEmpty_shouldWork() {
        NavigableMap<Long, String> desc = map.descendingMap();
        assertEquals(5, desc.size());
        assertFalse(desc.isEmpty());
    }

    @Test
    public void descendingMap_comparator_shouldBeReverse() {
        NavigableMap<Long, String> desc = map.descendingMap();
        Comparator<? super Long> cmp = desc.comparator();

        assertNotNull(cmp);
        // Descending comparator: 30 should be "less than" 20
        assertTrue(cmp.compare(30L, 20L) < 0);
    }

    @Test
    public void descendingMap_put_shouldWork() {
        map.descendingMap().put(60L, "F");
        assertTrue(map.containsKey(60L));
        assertEquals("F", map.get(60L));
    }

    @Test
    public void descendingMap_remove_shouldWork() {
        map.descendingMap().remove(30L);
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void descendingMap_clear_shouldWork() {
        map.descendingMap().clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void descendingMap_pollFirstEntry_shouldWork() {
        Map.Entry<Long, String> entry = map.descendingMap().pollFirstEntry();
        assertEquals(Long.valueOf(50L), entry.getKey());
        assertFalse(map.containsKey(50L));
    }

    @Test
    public void descendingMap_pollLastEntry_shouldWork() {
        Map.Entry<Long, String> entry = map.descendingMap().pollLastEntry();
        assertEquals(Long.valueOf(10L), entry.getKey());
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void keySet_pollLast_shouldWork() {
        Long last = map.navigableKeySet().pollLast();
        assertEquals(Long.valueOf(50L), last);
        assertFalse(map.containsKey(50L));
    }

    @Test
    public void keySet_headSet_shouldWork() {
        NavigableSet<Long> headSet = map.navigableKeySet().headSet(30L, true);
        assertEquals(3, headSet.size());
        assertTrue(headSet.contains(10L));
        assertTrue(headSet.contains(20L));
        assertTrue(headSet.contains(30L));
    }

    @Test
    public void keySet_tailSet_shouldWork() {
        NavigableSet<Long> tailSet = map.navigableKeySet().tailSet(30L, true);
        assertEquals(3, tailSet.size());
        assertTrue(tailSet.contains(30L));
        assertTrue(tailSet.contains(40L));
        assertTrue(tailSet.contains(50L));
    }

    @Test
    public void tailMap_pollFirstEntry_shouldWork() {
        Map.Entry<Long, String> entry = map.tailMap(25L, true).pollFirstEntry();
        assertEquals(Long.valueOf(30L), entry.getKey());
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void tailMap_pollLastEntry_shouldWork() {
        Map.Entry<Long, String> entry = map.tailMap(25L, true).pollLastEntry();
        assertEquals(Long.valueOf(50L), entry.getKey());
        assertFalse(map.containsKey(50L));
    }

    @Test
    public void headMap_pollFirstEntry_shouldWork() {
        Map.Entry<Long, String> entry = map.headMap(35L, false).pollFirstEntry();
        assertEquals(Long.valueOf(10L), entry.getKey());
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void headMap_pollLastEntry_shouldWork() {
        Map.Entry<Long, String> entry = map.headMap(35L, false).pollLastEntry();
        assertEquals(Long.valueOf(30L), entry.getKey());
        assertFalse(map.containsKey(30L));
    }
}
