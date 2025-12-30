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
 * HeadMapView/TailMapView 경계 조건 및 커버리지 테스트 (P1)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>FxNavigableMapImpl.HeadMapView (74% → 85%+)</li>
 *   <li>FxNavigableMapImpl.TailMapView (76% → 85%+)</li>
 * </ul>
 *
 * @since 0.9
 * @see FxNavigableMapImpl
 */
public class MapViewBoundaryTest {

    private File tempFile;
    private FxStore store;
    private NavigableMap<Long, String> map;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-mapview-boundary-", ".db").toFile();
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

    // ==================== HeadMapView 테스트 ====================

    @Test
    public void headMap_inclusive_shouldIncludeBoundary() {
        // Given: headMap(30, true)
        NavigableMap<Long, String> head = map.headMap(30L, true);

        // Then: should include 30
        assertTrue(head.containsKey(30L));
        assertEquals(3, head.size());
        assertEquals("C", head.get(30L));
    }

    @Test
    public void headMap_exclusive_shouldExcludeBoundary() {
        // Given: headMap(30, false)
        NavigableMap<Long, String> head = map.headMap(30L, false);

        // Then: should not include 30
        assertFalse(head.containsKey(30L));
        assertEquals(2, head.size());
    }

    @Test
    public void headMap_navigableKeySet_shouldWork() {
        // Given: headMap(40, true)
        NavigableMap<Long, String> head = map.headMap(40L, true);

        // When: navigableKeySet()
        NavigableSet<Long> keySet = head.navigableKeySet();

        // Then
        assertEquals(4, keySet.size());
        assertEquals(Long.valueOf(10L), keySet.first());
    }

    @Test
    public void headMap_descendingKeySet_shouldWork() {
        // Given: headMap(40, true)
        NavigableMap<Long, String> head = map.headMap(40L, true);

        // When: descendingKeySet()
        NavigableSet<Long> keySet = head.descendingKeySet();

        // Then
        assertEquals(Long.valueOf(40L), keySet.first());
    }

    @Test
    public void headMap_firstEntry_shouldReturnSmallest() {
        // Given: headMap(40, true)
        NavigableMap<Long, String> head = map.headMap(40L, true);

        // When
        Map.Entry<Long, String> first = head.firstEntry();

        // Then
        assertNotNull(first);
        assertEquals(Long.valueOf(10L), first.getKey());
        assertEquals("A", first.getValue());
    }

    @Test
    public void headMap_lastEntry_shouldReturnLargestInRange() {
        // Given: headMap(40, true)
        NavigableMap<Long, String> head = map.headMap(40L, true);

        // When
        Map.Entry<Long, String> last = head.lastEntry();

        // Then
        assertNotNull(last);
        assertEquals(Long.valueOf(40L), last.getKey());
        assertEquals("D", last.getValue());
    }

    @Test
    public void headMap_pollFirstEntry_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        Map.Entry<Long, String> entry = head.pollFirstEntry();
        assertEquals(Long.valueOf(10L), entry.getKey());
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void headMap_pollLastEntry_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        Map.Entry<Long, String> entry = head.pollLastEntry();
        assertEquals(Long.valueOf(40L), entry.getKey());
        assertFalse(map.containsKey(40L));
    }

    @Test
    public void headMap_lowerEntry_shouldWork() {
        // Given: headMap(40, true) = {10, 20, 30, 40}
        NavigableMap<Long, String> head = map.headMap(40L, true);

        // When: lowerEntry(30)
        Map.Entry<Long, String> lower = head.lowerEntry(30L);

        // Then: 20=B
        assertNotNull(lower);
        assertEquals(Long.valueOf(20L), lower.getKey());
    }

    @Test
    public void headMap_floorEntry_shouldWork() {
        // Given: headMap(40, true)
        NavigableMap<Long, String> head = map.headMap(40L, true);

        // When: floorEntry(25)
        Map.Entry<Long, String> floor = head.floorEntry(25L);

        // Then: 20=B
        assertNotNull(floor);
        assertEquals(Long.valueOf(20L), floor.getKey());
    }

    @Test
    public void headMap_ceilingEntry_shouldWork() {
        // Given: headMap(40, true)
        NavigableMap<Long, String> head = map.headMap(40L, true);

        // When: ceilingEntry(25)
        Map.Entry<Long, String> ceiling = head.ceilingEntry(25L);

        // Then: 30=C
        assertNotNull(ceiling);
        assertEquals(Long.valueOf(30L), ceiling.getKey());
    }

    @Test
    public void headMap_higherEntry_shouldWork() {
        // Given: headMap(40, true)
        NavigableMap<Long, String> head = map.headMap(40L, true);

        // When: higherEntry(30)
        Map.Entry<Long, String> higher = head.higherEntry(30L);

        // Then: 40=D
        assertNotNull(higher);
        assertEquals(Long.valueOf(40L), higher.getKey());
    }

    @Test
    public void headMap_descendingMap_shouldWork() {
        // Given: headMap(40, true)
        NavigableMap<Long, String> head = map.headMap(40L, true);

        // When: descendingMap()
        NavigableMap<Long, String> desc = head.descendingMap();

        // Then
        assertEquals(Long.valueOf(40L), desc.firstKey());
        assertEquals(Long.valueOf(10L), desc.lastKey());
    }

    @Test
    public void headMap_comparator_shouldReturnParentComparator() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        Comparator<? super Long> cmp = head.comparator();
        assertNotNull(cmp);
    }

    @Test
    public void headMap_containsValue_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertTrue(head.containsValue("A"));
        assertTrue(head.containsValue("D"));
        assertFalse(head.containsValue("E")); // 50은 범위 밖
    }

    @Test
    public void headMap_isEmpty_shouldWork() {
        // Given: headMap(5, true) - 아무 요소도 없음
        NavigableMap<Long, String> head = map.headMap(5L, true);
        assertTrue(head.isEmpty());
    }

    @Test
    public void headMap_entrySet_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(30L, true);
        Set<Map.Entry<Long, String>> entries = head.entrySet();
        assertEquals(3, entries.size());
    }

    @Test
    public void headMap_values_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(30L, true);
        Collection<String> values = head.values();
        assertEquals(3, values.size());
        assertTrue(values.contains("A"));
        assertTrue(values.contains("C"));
    }

    // ==================== TailMapView 테스트 ====================

    @Test
    public void tailMap_inclusive_shouldIncludeBoundary() {
        // Given: tailMap(30, true)
        NavigableMap<Long, String> tail = map.tailMap(30L, true);

        // Then: should include 30
        assertTrue(tail.containsKey(30L));
        assertEquals(3, tail.size());
        assertEquals("C", tail.get(30L));
    }

    @Test
    public void tailMap_exclusive_shouldExcludeBoundary() {
        // Given: tailMap(30, false)
        NavigableMap<Long, String> tail = map.tailMap(30L, false);

        // Then: should not include 30
        assertFalse(tail.containsKey(30L));
        assertEquals(2, tail.size());
    }

    @Test
    public void tailMap_navigableKeySet_shouldWork() {
        // Given: tailMap(20, true)
        NavigableMap<Long, String> tail = map.tailMap(20L, true);

        // When: navigableKeySet()
        NavigableSet<Long> keySet = tail.navigableKeySet();

        // Then
        assertEquals(4, keySet.size());
        assertEquals(Long.valueOf(20L), keySet.first());
    }

    @Test
    public void tailMap_descendingKeySet_shouldWork() {
        // Given: tailMap(20, true)
        NavigableMap<Long, String> tail = map.tailMap(20L, true);

        // When: descendingKeySet()
        NavigableSet<Long> keySet = tail.descendingKeySet();

        // Then
        assertEquals(Long.valueOf(50L), keySet.first());
    }

    @Test
    public void tailMap_firstEntry_shouldReturnSmallestInRange() {
        // Given: tailMap(20, true)
        NavigableMap<Long, String> tail = map.tailMap(20L, true);

        // When
        Map.Entry<Long, String> first = tail.firstEntry();

        // Then
        assertNotNull(first);
        assertEquals(Long.valueOf(20L), first.getKey());
        assertEquals("B", first.getValue());
    }

    @Test
    public void tailMap_lastEntry_shouldReturnLargest() {
        // Given: tailMap(20, true)
        NavigableMap<Long, String> tail = map.tailMap(20L, true);

        // When
        Map.Entry<Long, String> last = tail.lastEntry();

        // Then
        assertNotNull(last);
        assertEquals(Long.valueOf(50L), last.getKey());
        assertEquals("E", last.getValue());
    }

    @Test
    public void tailMap_pollFirstEntry_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        Map.Entry<Long, String> entry = tail.pollFirstEntry();
        assertEquals(Long.valueOf(20L), entry.getKey());
        assertFalse(map.containsKey(20L));
    }

    @Test
    public void tailMap_pollLastEntry_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        Map.Entry<Long, String> entry = tail.pollLastEntry();
        assertEquals(Long.valueOf(50L), entry.getKey());
        assertFalse(map.containsKey(50L));
    }

    @Test
    public void tailMap_lowerEntry_shouldWork() {
        // Given: tailMap(20, true) = {20, 30, 40, 50}
        NavigableMap<Long, String> tail = map.tailMap(20L, true);

        // When: lowerEntry(40)
        Map.Entry<Long, String> lower = tail.lowerEntry(40L);

        // Then: 30=C
        assertNotNull(lower);
        assertEquals(Long.valueOf(30L), lower.getKey());
    }

    @Test
    public void tailMap_floorEntry_shouldWork() {
        // Given: tailMap(20, true)
        NavigableMap<Long, String> tail = map.tailMap(20L, true);

        // When: floorEntry(35)
        Map.Entry<Long, String> floor = tail.floorEntry(35L);

        // Then: 30=C
        assertNotNull(floor);
        assertEquals(Long.valueOf(30L), floor.getKey());
    }

    @Test
    public void tailMap_ceilingEntry_shouldWork() {
        // Given: tailMap(20, true)
        NavigableMap<Long, String> tail = map.tailMap(20L, true);

        // When: ceilingEntry(35)
        Map.Entry<Long, String> ceiling = tail.ceilingEntry(35L);

        // Then: 40=D
        assertNotNull(ceiling);
        assertEquals(Long.valueOf(40L), ceiling.getKey());
    }

    @Test
    public void tailMap_higherEntry_shouldWork() {
        // Given: tailMap(20, true)
        NavigableMap<Long, String> tail = map.tailMap(20L, true);

        // When: higherEntry(40)
        Map.Entry<Long, String> higher = tail.higherEntry(40L);

        // Then: 50=E
        assertNotNull(higher);
        assertEquals(Long.valueOf(50L), higher.getKey());
    }

    @Test
    public void tailMap_descendingMap_shouldWork() {
        // Given: tailMap(20, true)
        NavigableMap<Long, String> tail = map.tailMap(20L, true);

        // When: descendingMap()
        NavigableMap<Long, String> desc = tail.descendingMap();

        // Then
        assertEquals(Long.valueOf(50L), desc.firstKey());
        assertEquals(Long.valueOf(20L), desc.lastKey());
    }

    @Test
    public void tailMap_comparator_shouldReturnParentComparator() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        Comparator<? super Long> cmp = tail.comparator();
        assertNotNull(cmp);
    }

    @Test
    public void tailMap_containsValue_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertTrue(tail.containsValue("B"));
        assertTrue(tail.containsValue("E"));
        assertFalse(tail.containsValue("A")); // 10은 범위 밖
    }

    @Test
    public void tailMap_isEmpty_shouldWork() {
        // Given: tailMap(100, true) - 아무 요소도 없음
        NavigableMap<Long, String> tail = map.tailMap(100L, true);
        assertTrue(tail.isEmpty());
    }

    @Test
    public void tailMap_entrySet_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(30L, true);
        Set<Map.Entry<Long, String>> entries = tail.entrySet();
        assertEquals(3, entries.size());
    }

    @Test
    public void tailMap_values_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(30L, true);
        Collection<String> values = tail.values();
        assertEquals(3, values.size());
        assertTrue(values.contains("C"));
        assertTrue(values.contains("E"));
    }

    // ==================== 경계 조합 테스트 ====================

    @Test
    public void headMap_tailMap_combination() {
        // Given: headMap(40, false) then tailMap(20, true)
        // 결과: {20, 30} (20 <= x < 40)
        NavigableMap<Long, String> head = map.headMap(40L, false);
        // 참고: head에서 tailMap을 호출하면 UOE 발생할 수 있음
        // 이 테스트는 각각 독립적으로 확인
        assertEquals(3, head.size()); // 10, 20, 30
    }

    @Test
    public void singleElementView() {
        // Given: headMap(20, true) - {10, 20}
        NavigableMap<Long, String> view = map.headMap(20L, true);
        assertEquals(2, view.size());

        // tailMap(20, true) on original - {20, 30, 40, 50}
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertEquals(4, tail.size());
    }

    @Test
    public void emptyView_operations() {
        // Given: empty headMap
        NavigableMap<Long, String> empty = map.headMap(5L, true);

        // Then
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        // UOE 개선 후: 빈 뷰에서 firstEntry/lastEntry는 null 반환
        assertNull(empty.firstEntry());
        assertNull(empty.lastEntry());
        assertNull(empty.lowerEntry(10L));
        assertNull(empty.higherEntry(10L));
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyHeadMap_firstKey_shouldThrow() {
        // UOE 개선 후: 빈 뷰에서 firstKey()는 NoSuchElementException 발생
        NavigableMap<Long, String> empty = map.headMap(5L, true);
        empty.firstKey();
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyHeadMap_lastKey_shouldThrow() {
        // HeadMapView.lastKey() correctly checks range and throws for empty view
        NavigableMap<Long, String> empty = map.headMap(5L, true);
        empty.lastKey();
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyTailMap_firstKey_shouldThrow() {
        // TailMapView.firstKey() correctly checks range and throws for empty view
        NavigableMap<Long, String> empty = map.tailMap(100L, true);
        empty.firstKey();
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyTailMap_lastKey_shouldThrow() {
        // TailMapView.lastKey() correctly checks range and throws for empty view
        NavigableMap<Long, String> empty = map.tailMap(100L, true);
        empty.lastKey();
    }

    @Test
    public void emptyTailMap_operations() {
        // Given: empty tailMap
        NavigableMap<Long, String> empty = map.tailMap(100L, true);

        // Then
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        // UOE 개선 후: 빈 뷰에서 firstEntry/lastEntry는 null 반환
        assertNull(empty.firstEntry());
        assertNull(empty.lastEntry());
    }

    // ==================== 중첩 SubMap 테스트 (UOE 개선) ====================

    @Test
    public void headMap_subMap_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        NavigableMap<Long, String> sub = head.subMap(10L, true, 30L, true);

        assertEquals(3, sub.size());
        assertTrue(sub.containsKey(10L));
        assertTrue(sub.containsKey(20L));
        assertTrue(sub.containsKey(30L));
    }

    @Test
    public void headMap_headMap_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        NavigableMap<Long, String> nested = head.headMap(30L, true);

        assertEquals(3, nested.size());
        assertTrue(nested.containsKey(10L));
        assertTrue(nested.containsKey(20L));
        assertTrue(nested.containsKey(30L));
    }

    @Test
    public void headMap_tailMap_shouldWork() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        NavigableMap<Long, String> tail = head.tailMap(20L, true);

        assertEquals(3, tail.size());
        assertTrue(tail.containsKey(20L));
        assertTrue(tail.containsKey(30L));
        assertTrue(tail.containsKey(40L));
    }

    @Test
    public void tailMap_subMap_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        NavigableMap<Long, String> sub = tail.subMap(30L, true, 50L, true);

        assertEquals(3, sub.size());
        assertTrue(sub.containsKey(30L));
        assertTrue(sub.containsKey(40L));
        assertTrue(sub.containsKey(50L));
    }

    @Test
    public void tailMap_headMap_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        NavigableMap<Long, String> head = tail.headMap(40L, true);

        assertEquals(3, head.size());
        assertTrue(head.containsKey(20L));
        assertTrue(head.containsKey(30L));
        assertTrue(head.containsKey(40L));
    }

    @Test
    public void tailMap_tailMap_shouldWork() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        NavigableMap<Long, String> nested = tail.tailMap(30L, true);

        assertEquals(3, nested.size());
        assertTrue(nested.containsKey(30L));
        assertTrue(nested.containsKey(40L));
        assertTrue(nested.containsKey(50L));
    }

    // ==================== 키 탐색 테스트 ====================

    @Test
    public void headMap_lowerKey_higherKey() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertEquals(Long.valueOf(20L), head.lowerKey(30L));
        assertEquals(Long.valueOf(40L), head.higherKey(30L));
    }

    @Test
    public void headMap_floorKey_ceilingKey() {
        NavigableMap<Long, String> head = map.headMap(40L, true);
        assertEquals(Long.valueOf(30L), head.floorKey(30L));
        assertEquals(Long.valueOf(30L), head.ceilingKey(30L));
        assertEquals(Long.valueOf(20L), head.floorKey(25L));
        assertEquals(Long.valueOf(30L), head.ceilingKey(25L));
    }

    @Test
    public void tailMap_lowerKey_higherKey() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertEquals(Long.valueOf(30L), tail.lowerKey(40L));
        assertEquals(Long.valueOf(50L), tail.higherKey(40L));
    }

    @Test
    public void tailMap_floorKey_ceilingKey() {
        NavigableMap<Long, String> tail = map.tailMap(20L, true);
        assertEquals(Long.valueOf(40L), tail.floorKey(40L));
        assertEquals(Long.valueOf(40L), tail.ceilingKey(40L));
        assertEquals(Long.valueOf(30L), tail.floorKey(35L));
        assertEquals(Long.valueOf(40L), tail.ceilingKey(35L));
    }
}
