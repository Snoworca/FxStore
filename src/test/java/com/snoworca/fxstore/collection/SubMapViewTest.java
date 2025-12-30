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
 * SubMapView 커버리지 테스트 (P3)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>FxNavigableMapImpl.SubMapView (78% → 85%+)</li>
 * </ul>
 *
 * @since 0.9
 * @see FxNavigableMapImpl
 */
public class SubMapViewTest {

    private File tempFile;
    private FxStore store;
    private NavigableMap<Long, String> map;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-submap-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());
        map = store.createMap("testMap", Long.class, String.class);

        // 기본 데이터: {10=A, 20=B, 30=C, 40=D, 50=E}
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

    // ==================== 4가지 경계 조합 테스트 ====================

    @Test
    public void subMap_bothInclusive_shouldIncludeBothBoundaries() {
        // subMap(20, true, 40, true) = {20, 30, 40}
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);

        assertEquals(3, sub.size());
        assertTrue(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
        assertTrue(sub.containsKey(40L));
        assertFalse(sub.containsKey(10L));
        assertFalse(sub.containsKey(50L));
    }

    @Test
    public void subMap_bothExclusive_shouldExcludeBothBoundaries() {
        // subMap(20, false, 40, false) = {30}
        NavigableMap<Long, String> sub = map.subMap(20L, false, 40L, false);

        assertEquals(1, sub.size());
        assertFalse(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
        assertFalse(sub.containsKey(40L));
    }

    @Test
    public void subMap_fromInclusiveToExclusive_shouldWork() {
        // subMap(20, true, 40, false) = {20, 30}
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, false);

        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
        assertFalse(sub.containsKey(40L));
    }

    @Test
    public void subMap_fromExclusiveToInclusive_shouldWork() {
        // subMap(20, false, 40, true) = {30, 40}
        NavigableMap<Long, String> sub = map.subMap(20L, false, 40L, true);

        assertEquals(2, sub.size());
        assertFalse(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
        assertTrue(sub.containsKey(40L));
    }

    // ==================== SortedMap 형식 subMap 테스트 ====================

    @Test
    public void subMap_sortedMapStyle_shouldDefaultToFromInclusiveToExclusive() {
        // subMap(20, 40) = subMap(20, true, 40, false) = {20, 30}
        SortedMap<Long, String> sub = map.subMap(20L, 40L);

        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
        assertFalse(sub.containsKey(40L));
    }

    // ==================== 빈 subMap 테스트 ====================

    @Test
    public void subMap_emptyRange_shouldBeEmpty() {
        // subMap(25, true, 25, true) where 25 doesn't exist
        NavigableMap<Long, String> sub = map.subMap(25L, true, 28L, true);
        assertTrue(sub.isEmpty());
        assertEquals(0, sub.size());
    }

    @Test
    public void subMap_sameKeyBothExclusive_shouldBeEmpty() {
        // subMap(30, false, 30, false) = empty
        NavigableMap<Long, String> sub = map.subMap(30L, false, 30L, false);
        assertTrue(sub.isEmpty());
    }

    // ==================== firstEntry/lastEntry 테스트 ====================

    @Test
    public void subMap_firstEntry_shouldReturnFirst() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Map.Entry<Long, String> first = sub.firstEntry();

        assertNotNull(first);
        assertEquals(Long.valueOf(20L), first.getKey());
        assertEquals("B", first.getValue());
    }

    @Test
    public void subMap_lastEntry_shouldReturnLast() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Map.Entry<Long, String> last = sub.lastEntry();

        assertNotNull(last);
        assertEquals(Long.valueOf(40L), last.getKey());
        assertEquals("D", last.getValue());
    }

    // ==================== firstKey/lastKey 테스트 ====================

    @Test
    public void subMap_firstKey_shouldReturnFirstKey() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertEquals(Long.valueOf(20L), sub.firstKey());
    }

    @Test
    public void subMap_lastKey_shouldReturnLastKey() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertEquals(Long.valueOf(40L), sub.lastKey());
    }

    // ==================== lower/floor/ceiling/higher 테스트 ====================

    @Test
    public void subMap_lowerEntry_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Map.Entry<Long, String> lower = sub.lowerEntry(30L);

        assertNotNull(lower);
        assertEquals(Long.valueOf(20L), lower.getKey());
    }

    @Test
    public void subMap_floorEntry_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Map.Entry<Long, String> floor = sub.floorEntry(35L);

        assertNotNull(floor);
        assertEquals(Long.valueOf(30L), floor.getKey());
    }

    @Test
    public void subMap_ceilingEntry_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Map.Entry<Long, String> ceiling = sub.ceilingEntry(25L);

        assertNotNull(ceiling);
        assertEquals(Long.valueOf(30L), ceiling.getKey());
    }

    @Test
    public void subMap_higherEntry_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Map.Entry<Long, String> higher = sub.higherEntry(30L);

        assertNotNull(higher);
        assertEquals(Long.valueOf(40L), higher.getKey());
    }

    // ==================== iterator 테스트 ====================

    @Test
    public void subMap_entrySet_iterator_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Set<Map.Entry<Long, String>> entries = sub.entrySet();

        List<Long> keys = new ArrayList<>();
        for (Map.Entry<Long, String> entry : entries) {
            keys.add(entry.getKey());
        }

        assertEquals(Arrays.asList(20L, 30L, 40L), keys);
    }

    @Test
    public void subMap_keySet_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Set<Long> keySet = sub.keySet();

        assertEquals(3, keySet.size());
        assertTrue(keySet.contains(20L));
        assertTrue(keySet.contains(30L));
        assertTrue(keySet.contains(40L));
    }

    @Test
    public void subMap_values_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Collection<String> values = sub.values();

        assertEquals(3, values.size());
        assertTrue(values.contains("B"));
        assertTrue(values.contains("C"));
        assertTrue(values.contains("D"));
    }

    // ==================== containsKey/containsValue 테스트 ====================

    @Test
    public void subMap_containsKey_inRange_shouldReturnTrue() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertTrue(sub.containsKey(30L));
    }

    @Test
    public void subMap_containsKey_outOfRange_shouldReturnFalse() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertFalse(sub.containsKey(10L)); // below range
        assertFalse(sub.containsKey(50L)); // above range
    }

    @Test
    public void subMap_containsValue_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertTrue(sub.containsValue("C"));
        assertFalse(sub.containsValue("A")); // 10 is out of range
        assertFalse(sub.containsValue("E")); // 50 is out of range
    }

    // ==================== get 테스트 ====================

    @Test
    public void subMap_get_inRange_shouldReturnValue() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertEquals("C", sub.get(30L));
    }

    @Test
    public void subMap_get_outOfRange_shouldReturnNull() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        assertNull(sub.get(10L));
        assertNull(sub.get(50L));
    }

    // ==================== comparator 테스트 ====================

    @Test
    public void subMap_comparator_shouldReturnParentComparator() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Comparator<? super Long> cmp = sub.comparator();
        assertNotNull(cmp);
    }

    // ==================== UOE 개선된 연산 테스트 ====================

    @Test
    public void subMap_pollFirstEntry_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Map.Entry<Long, String> entry = sub.pollFirstEntry();
        assertEquals(Long.valueOf(20L), entry.getKey());
        assertFalse(map.containsKey(20L));
    }

    @Test
    public void subMap_pollLastEntry_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        Map.Entry<Long, String> entry = sub.pollLastEntry();
        assertEquals(Long.valueOf(40L), entry.getKey());
        assertFalse(map.containsKey(40L));
    }

    @Test
    public void subMap_descendingMap_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        NavigableMap<Long, String> desc = sub.descendingMap();

        assertEquals(Long.valueOf(40L), desc.firstKey());
        assertEquals(Long.valueOf(20L), desc.lastKey());
    }

    @Test
    public void subMap_navigableKeySet_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        NavigableSet<Long> keySet = sub.navigableKeySet();

        assertEquals(3, keySet.size());
        assertEquals(Long.valueOf(20L), keySet.first());
        assertEquals(Long.valueOf(40L), keySet.last());
    }

    @Test
    public void subMap_descendingKeySet_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        NavigableSet<Long> keySet = sub.descendingKeySet();

        assertEquals(Long.valueOf(40L), keySet.first());
        assertEquals(Long.valueOf(20L), keySet.last());
    }

    @Test
    public void subMap_nestedSubMap_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        NavigableMap<Long, String> nested = sub.subMap(25L, true, 35L, true);

        assertEquals(1, nested.size());
        assertTrue(nested.containsKey(30L));
    }

    @Test
    public void subMap_headMap_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        NavigableMap<Long, String> head = sub.headMap(30L, true);

        assertEquals(2, head.size());
        assertTrue(head.containsKey(20L));
        assertTrue(head.containsKey(30L));
    }

    @Test
    public void subMap_tailMap_shouldWork() {
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        NavigableMap<Long, String> tail = sub.tailMap(30L, true);

        assertEquals(2, tail.size());
        assertTrue(tail.containsKey(30L));
        assertTrue(tail.containsKey(40L));
    }
}
