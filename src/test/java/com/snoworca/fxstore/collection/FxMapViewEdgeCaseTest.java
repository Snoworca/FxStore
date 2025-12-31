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
 * FxNavigableMapImpl View 클래스 에지 케이스 테스트
 *
 * <p>V16 커버리지 개선: SubMapView, HeadMapView, TailMapView의 미커버 경로 테스트</p>
 *
 * <h3>테스트 대상</h3>
 * <ul>
 *   <li>SubMapView - 범위 밖 키 처리, pollFirst/pollLast</li>
 *   <li>HeadMapView - 범위 밖 키 처리</li>
 *   <li>TailMapView - 범위 밖 키 처리</li>
 *   <li>중첩 뷰 생성</li>
 * </ul>
 */
public class FxMapViewEdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("map-view-edge-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    private NavigableMap<Long, String> createTestMap() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 10; i <= 100; i += 10) {
            map.put(i, "value" + i);
        }
        return map;
    }

    // ==================== SubMap 에지 케이스 ====================

    @Test
    public void subMap_emptyRange_shouldBeEmpty() {
        NavigableMap<Long, String> map = createTestMap();

        // 50 ~ 50 범위 (exclusive-exclusive = empty)
        NavigableMap<Long, String> sub = map.subMap(50L, false, 50L, false);
        assertTrue(sub.isEmpty());
    }

    @Test
    public void subMap_singleElement_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();

        // 50 ~ 50 범위 (inclusive-inclusive)
        NavigableMap<Long, String> sub = map.subMap(50L, true, 50L, true);
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey(50L));
    }

    @Test
    public void subMap_pollFirstEntry_shouldRemoveFromOriginal() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        assertEquals(5, sub.size()); // 30, 40, 50, 60, 70

        Map.Entry<Long, String> first = sub.pollFirstEntry();
        assertEquals(Long.valueOf(30L), first.getKey());
        assertEquals(4, sub.size());
        assertFalse(map.containsKey(30L)); // 원본에서도 제거됨
    }

    @Test
    public void subMap_pollLastEntry_shouldRemoveFromOriginal() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        Map.Entry<Long, String> last = sub.pollLastEntry();
        assertEquals(Long.valueOf(70L), last.getKey());
        assertFalse(map.containsKey(70L));
    }

    @Test
    public void subMap_pollFirstEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("empty", Long.class, String.class);
        NavigableMap<Long, String> sub = map.subMap(10L, true, 50L, true);

        assertNull(sub.pollFirstEntry());
    }

    @Test
    public void subMap_pollLastEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("empty", Long.class, String.class);
        NavigableMap<Long, String> sub = map.subMap(10L, true, 50L, true);

        assertNull(sub.pollLastEntry());
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_putOutOfRange_shouldThrow() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        sub.put(100L, "out-of-range"); // 범위 밖
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_putBelowRange_shouldThrow() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        sub.put(10L, "below-range");
    }

    @Test
    public void subMap_containsKey_outsideRange_shouldReturnFalse() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        assertFalse(sub.containsKey(10L)); // 범위 밖
        assertFalse(sub.containsKey(100L)); // 범위 밖
        assertTrue(sub.containsKey(50L)); // 범위 내
    }

    @Test
    public void subMap_ceilingEntry_inRange_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        Map.Entry<Long, String> entry = sub.ceilingEntry(35L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(40L), entry.getKey());
    }

    @Test
    public void subMap_floorEntry_inRange_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        Map.Entry<Long, String> entry = sub.floorEntry(45L);
        assertNotNull(entry);
        assertEquals(Long.valueOf(40L), entry.getKey());
    }

    @Test
    public void subMap_higherEntry_atBoundary_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        // 70은 범위 내 최대값이므로 higher(70)은 null
        Map.Entry<Long, String> entry = sub.higherEntry(70L);
        assertNull(entry);
    }

    @Test
    public void subMap_lowerEntry_atBoundary_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        // 30은 범위 내 최소값이므로 lower(30)은 null
        Map.Entry<Long, String> entry = sub.lowerEntry(30L);
        assertNull(entry);
    }

    // ==================== HeadMap 에지 케이스 ====================

    @Test
    public void headMap_emptyRange_shouldBeEmpty() {
        NavigableMap<Long, String> map = createTestMap();

        // < 10 (최소값 미만)
        NavigableMap<Long, String> head = map.headMap(10L, false);
        assertTrue(head.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void headMap_putAboveRange_shouldThrow() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> head = map.headMap(50L, true);

        head.put(60L, "above-range");
    }

    @Test
    public void headMap_pollLastEntry_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> head = map.headMap(50L, true);

        assertEquals(5, head.size()); // 10, 20, 30, 40, 50

        Map.Entry<Long, String> last = head.pollLastEntry();
        assertEquals(Long.valueOf(50L), last.getKey());
        assertEquals(4, head.size());
    }

    @Test
    public void headMap_firstEntry_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> head = map.headMap(50L, true);

        Map.Entry<Long, String> first = head.firstEntry();
        assertNotNull(first);
        assertEquals(Long.valueOf(10L), first.getKey());
    }

    @Test
    public void headMap_lastEntry_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> head = map.headMap(50L, true);

        Map.Entry<Long, String> last = head.lastEntry();
        assertNotNull(last);
        assertEquals(Long.valueOf(50L), last.getKey());
    }

    // ==================== TailMap 에지 케이스 ====================

    @Test
    public void tailMap_emptyRange_shouldBeEmpty() {
        NavigableMap<Long, String> map = createTestMap();

        // > 100 (최대값 초과)
        NavigableMap<Long, String> tail = map.tailMap(101L, true);
        assertTrue(tail.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void tailMap_putBelowRange_shouldThrow() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> tail = map.tailMap(50L, true);

        tail.put(40L, "below-range");
    }

    @Test
    public void tailMap_pollFirstEntry_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> tail = map.tailMap(50L, true);

        assertEquals(6, tail.size()); // 50, 60, 70, 80, 90, 100

        Map.Entry<Long, String> first = tail.pollFirstEntry();
        assertEquals(Long.valueOf(50L), first.getKey());
        assertEquals(5, tail.size());
    }

    @Test
    public void tailMap_firstEntry_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> tail = map.tailMap(50L, true);

        Map.Entry<Long, String> first = tail.firstEntry();
        assertNotNull(first);
        assertEquals(Long.valueOf(50L), first.getKey());
    }

    @Test
    public void tailMap_lastEntry_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> tail = map.tailMap(50L, true);

        Map.Entry<Long, String> last = tail.lastEntry();
        assertNotNull(last);
        assertEquals(Long.valueOf(100L), last.getKey());
    }

    // ==================== 중첩 뷰 테스트 ====================

    @Test
    public void nestedSubMap_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub1 = map.subMap(20L, true, 90L, true);
        NavigableMap<Long, String> sub2 = sub1.subMap(40L, true, 70L, true);

        assertEquals(4, sub2.size()); // 40, 50, 60, 70
        assertTrue(sub2.containsKey(50L));
        assertFalse(sub2.containsKey(30L));
    }

    @Test
    public void headMapOfSubMap_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 80L, true);
        NavigableMap<Long, String> head = sub.headMap(60L, true);

        assertEquals(4, head.size()); // 30, 40, 50, 60
    }

    @Test
    public void tailMapOfSubMap_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 80L, true);
        NavigableMap<Long, String> tail = sub.tailMap(50L, true);

        assertEquals(4, tail.size()); // 50, 60, 70, 80
    }

    // ==================== DescendingMap 테스트 ====================

    @Test
    public void subMap_descendingMap_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);
        NavigableMap<Long, String> desc = sub.descendingMap();

        Map.Entry<Long, String> first = desc.firstEntry();
        assertEquals(Long.valueOf(70L), first.getKey());

        Map.Entry<Long, String> last = desc.lastEntry();
        assertEquals(Long.valueOf(30L), last.getKey());
    }

    @Test
    public void headMap_descendingMap_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> head = map.headMap(50L, true);
        NavigableMap<Long, String> desc = head.descendingMap();

        assertEquals(Long.valueOf(50L), desc.firstEntry().getKey());
        assertEquals(Long.valueOf(10L), desc.lastEntry().getKey());
    }

    // ==================== NavigableKeySet 테스트 ====================

    @Test
    public void subMap_navigableKeySet_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);
        NavigableSet<Long> keySet = sub.navigableKeySet();

        assertEquals(5, keySet.size());
        assertEquals(Long.valueOf(30L), keySet.first());
        assertEquals(Long.valueOf(70L), keySet.last());
    }

    @Test
    public void subMap_descendingKeySet_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);
        NavigableSet<Long> descKeySet = sub.descendingKeySet();

        assertEquals(Long.valueOf(70L), descKeySet.first());
        assertEquals(Long.valueOf(30L), descKeySet.last());
    }

    // ==================== entrySet/keySet/values 테스트 ====================

    @Test
    public void subMap_entrySet_iterate_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        int count = 0;
        for (Map.Entry<Long, String> entry : sub.entrySet()) {
            long key = entry.getKey();
            assertTrue(key >= 30 && key <= 70);
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void subMap_keySet_iterate_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        int count = 0;
        for (Long key : sub.keySet()) {
            assertTrue(key >= 30 && key <= 70);
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void subMap_values_iterate_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        int count = 0;
        for (String value : sub.values()) {
            assertNotNull(value);
            count++;
        }
        assertEquals(5, count);
    }

    // ==================== clear 테스트 ====================

    @Test(expected = UnsupportedOperationException.class)
    public void subMap_clear_shouldThrowUnsupported() {
        NavigableMap<Long, String> map = createTestMap();

        NavigableMap<Long, String> sub = map.subMap(40L, true, 60L, true);
        assertEquals(3, sub.size()); // 40, 50, 60

        // SubMapView does not support clear()
        sub.clear();
    }

    // ==================== get/remove 테스트 ====================

    @Test
    public void subMap_get_outsideRange_shouldReturnNull() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        assertNull(sub.get(10L)); // 범위 밖
        assertNull(sub.get(100L)); // 범위 밖
        assertNotNull(sub.get(50L)); // 범위 내
    }

    @Test
    public void subMap_remove_insideRange_shouldWork() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        String removed = sub.remove(50L);
        assertEquals("value50", removed);
        assertFalse(sub.containsKey(50L));
        assertFalse(map.containsKey(50L));
    }

    @Test
    public void subMap_remove_outsideRange_shouldReturnNull() {
        NavigableMap<Long, String> map = createTestMap();
        NavigableMap<Long, String> sub = map.subMap(30L, true, 70L, true);

        String removed = sub.remove(10L); // 범위 밖
        assertNull(removed);
        assertTrue(map.containsKey(10L)); // 원본에는 여전히 존재
    }
}
