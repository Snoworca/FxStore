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
 * FxNavigableMapImpl null 및 예외 처리 테스트
 *
 * 커버리지 개선 대상:
 * - null key/value 예외 처리
 * - ClassCastException 처리
 * - 빈 Map 연산
 * - 경계 조건 테스트
 */
public class FxNavigableMapNullEdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("map-null-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== null key 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void put_nullKey_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(null, "value");
    }

    @Test(expected = NullPointerException.class)
    public void put_nullValue_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, null);
    }

    @Test(expected = NullPointerException.class)
    public void put_nullKeyAndValue_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void get_nullKey_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.get(null);
    }

    @Test(expected = NullPointerException.class)
    public void containsKey_nullKey_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.containsKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void remove_nullKey_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.remove(null);
    }

    // ==================== lowerKey/higherKey null 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void lowerKey_nullKey_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.lowerKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void higherKey_nullKey_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.higherKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void floorKey_nullKey_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.floorKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void ceilingKey_nullKey_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.ceilingKey(null);
    }

    // ==================== 빈 맵에서 null 반환 테스트 ====================

    @Test
    public void lowerKey_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.lowerKey(5L));
    }

    @Test
    public void higherKey_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.higherKey(5L));
    }

    @Test
    public void floorKey_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.floorKey(5L));
    }

    @Test
    public void ceilingKey_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.ceilingKey(5L));
    }

    @Test
    public void lowerEntry_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.lowerEntry(5L));
    }

    @Test
    public void higherEntry_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.higherEntry(5L));
    }

    @Test
    public void floorEntry_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.floorEntry(5L));
    }

    @Test
    public void ceilingEntry_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.ceilingEntry(5L));
    }

    @Test
    public void firstEntry_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.firstEntry());
    }

    @Test
    public void lastEntry_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.lastEntry());
    }

    // ==================== 경계 조건 null 반환 테스트 ====================

    @Test
    public void lowerKey_noLowerExists_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(5L, "five");
        map.put(10L, "ten");
        assertNull(map.lowerKey(5L)); // 5 이하의 키 없음
    }

    @Test
    public void higherKey_noHigherExists_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(5L, "five");
        map.put(10L, "ten");
        assertNull(map.higherKey(10L)); // 10 이상의 키 없음
    }

    @Test
    public void floorKey_allKeysHigher_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");
        map.put(20L, "twenty");
        assertNull(map.floorKey(5L)); // 5 이하의 키 없음
    }

    @Test
    public void ceilingKey_allKeysLower_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");
        assertNull(map.ceilingKey(10L)); // 10 이상의 키 없음
    }

    // ==================== containsValue 테스트 ====================

    @Test
    public void containsValue_existingValue_shouldReturnTrue() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");
        assertTrue(map.containsValue("one"));
        assertTrue(map.containsValue("two"));
    }

    @Test
    public void containsValue_nonExistingValue_shouldReturnFalse() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        assertFalse(map.containsValue("notexist"));
    }

    @Test
    public void containsValue_emptyMap_shouldReturnFalse() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertFalse(map.containsValue("any"));
    }

    @Test
    public void containsValue_nullValue_shouldReturnFalse() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        assertFalse(map.containsValue(null));
    }

    // ==================== remove 결과 테스트 ====================

    @Test
    public void remove_nonExistingKey_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        assertNull(map.remove(999L));
    }

    @Test
    public void remove_existingKey_shouldReturnOldValue() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        assertEquals("one", map.remove(1L));
        assertFalse(map.containsKey(1L));
    }

    @Test
    public void remove_emptyMap_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertNull(map.remove(1L));
    }

    // ==================== SubMapView null 반환 테스트 ====================

    @Test
    public void subMap_lowerKey_noLowerInRange_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(5L, true, 8L, true);
        assertNull(sub.lowerKey(5L)); // 5 이하의 키가 범위 내에 없음
    }

    @Test
    public void subMap_higherKey_noHigherInRange_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(5L, true, 8L, true);
        assertNull(sub.higherKey(8L)); // 8 이상의 키가 범위 내에 없음
    }

    @Test
    public void subMap_pollFirstEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(10L, "ten");

        NavigableMap<Long, String> sub = map.subMap(5L, true, 8L, true);
        assertNull(sub.pollFirstEntry()); // 빈 서브맵
    }

    @Test
    public void subMap_pollLastEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(10L, "ten");

        NavigableMap<Long, String> sub = map.subMap(5L, true, 8L, true);
        assertNull(sub.pollLastEntry()); // 빈 서브맵
    }

    // ==================== HeadMapView null 반환 테스트 ====================

    @Test
    public void headMap_pollFirstEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");

        NavigableMap<Long, String> head = map.headMap(5L, false);
        assertNull(head.pollFirstEntry()); // 빈 헤드맵
    }

    @Test
    public void headMap_pollLastEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");

        NavigableMap<Long, String> head = map.headMap(5L, false);
        assertNull(head.pollLastEntry()); // 빈 헤드맵
    }

    @Test(expected = NoSuchElementException.class)
    public void headMap_firstKey_empty_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");

        NavigableMap<Long, String> head = map.headMap(5L, false);
        head.firstKey();
    }

    @Test(expected = NoSuchElementException.class)
    public void headMap_lastKey_empty_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(10L, "ten");

        NavigableMap<Long, String> head = map.headMap(5L, false);
        head.lastKey();
    }

    // ==================== TailMapView null 반환 테스트 ====================

    @Test
    public void tailMap_pollFirstEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        NavigableMap<Long, String> tail = map.tailMap(10L, true);
        assertNull(tail.pollFirstEntry()); // 빈 테일맵
    }

    @Test
    public void tailMap_pollLastEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        NavigableMap<Long, String> tail = map.tailMap(10L, true);
        assertNull(tail.pollLastEntry()); // 빈 테일맵
    }

    @Test(expected = NoSuchElementException.class)
    public void tailMap_firstKey_empty_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        NavigableMap<Long, String> tail = map.tailMap(10L, true);
        tail.firstKey();
    }

    @Test(expected = NoSuchElementException.class)
    public void tailMap_lastKey_empty_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        NavigableMap<Long, String> tail = map.tailMap(10L, true);
        tail.lastKey();
    }

    // ==================== DescendingMap null 반환 테스트 ====================

    @Test
    public void descendingMap_pollFirstEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        NavigableMap<Long, String> desc = map.descendingMap();
        assertNull(desc.pollFirstEntry());
    }

    @Test
    public void descendingMap_pollLastEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        NavigableMap<Long, String> desc = map.descendingMap();
        assertNull(desc.pollLastEntry());
    }

    // ==================== NavigableKeySet null 반환 테스트 ====================

    @Test
    public void navigableKeySet_pollFirst_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        NavigableSet<Long> keys = map.navigableKeySet();
        assertNull(keys.pollFirst());
    }

    @Test
    public void navigableKeySet_pollLast_empty_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        NavigableSet<Long> keys = map.navigableKeySet();
        assertNull(keys.pollLast());
    }

    @Test
    public void navigableKeySet_lower_emptySet_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        NavigableSet<Long> keys = map.navigableKeySet();
        assertNull(keys.lower(5L));
    }

    @Test
    public void navigableKeySet_higher_emptySet_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        NavigableSet<Long> keys = map.navigableKeySet();
        assertNull(keys.higher(5L));
    }

    @Test
    public void navigableKeySet_floor_emptySet_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        NavigableSet<Long> keys = map.navigableKeySet();
        assertNull(keys.floor(5L));
    }

    @Test
    public void navigableKeySet_ceiling_emptySet_shouldReturnNull() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        NavigableSet<Long> keys = map.navigableKeySet();
        assertNull(keys.ceiling(5L));
    }

    // ==================== SubMapView 중첩 경계 테스트 ====================

    @Test(expected = IllegalArgumentException.class)
    public void subMap_nestedSubMap_outOfRange_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 20; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> sub = map.subMap(5L, true, 15L, true);
        // 내부 범위를 벗어나는 서브맵 생성 시도
        sub.subMap(1L, true, 20L, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void headMap_nestedHeadMap_outOfRange_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> head = map.headMap(5L, true);
        // 범위를 벗어나는 헤드맵 생성 시도
        head.headMap(10L, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tailMap_nestedTailMap_outOfRange_shouldThrow() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableMap<Long, String> tail = map.tailMap(5L, true);
        // 범위를 벗어나는 테일맵 생성 시도
        tail.tailMap(1L, true);
    }

    // ==================== KeySetView SubSet 테스트 ====================

    @Test
    public void navigableKeySet_subSet_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableSet<Long> keys = map.navigableKeySet();
        NavigableSet<Long> subKeys = keys.subSet(3L, true, 7L, true);

        assertEquals(5, subKeys.size());
        assertEquals(Long.valueOf(3L), subKeys.first());
        assertEquals(Long.valueOf(7L), subKeys.last());
    }

    @Test
    public void navigableKeySet_headSet_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableSet<Long> keys = map.navigableKeySet();
        NavigableSet<Long> headKeys = keys.headSet(5L, false);

        assertEquals(4, headKeys.size());
        assertFalse(headKeys.contains(5L));
    }

    @Test
    public void navigableKeySet_tailSet_shouldWork() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        NavigableSet<Long> keys = map.navigableKeySet();
        NavigableSet<Long> tailKeys = keys.tailSet(5L, true);

        assertEquals(6, tailKeys.size());
        assertTrue(tailKeys.contains(5L));
    }

    // ==================== isEmpty 테스트 ====================

    @Test
    public void isEmpty_emptyMap_shouldReturnTrue() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        assertTrue(map.isEmpty());
    }

    @Test
    public void isEmpty_afterAdd_shouldReturnFalse() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        assertFalse(map.isEmpty());
    }

    @Test
    public void isEmpty_afterClear_shouldReturnTrue() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.clear();
        assertTrue(map.isEmpty());
    }

    // ==================== subMap isEmpty 테스트 ====================

    @Test
    public void subMap_isEmpty_whenNoElementsInRange_shouldReturnTrue() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(10L, "ten");

        NavigableMap<Long, String> sub = map.subMap(4L, true, 6L, true);
        assertTrue(sub.isEmpty());
    }

    @Test
    public void subMap_isEmpty_whenElementsInRange_shouldReturnFalse() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        map.put(5L, "five");
        map.put(10L, "ten");

        NavigableMap<Long, String> sub = map.subMap(4L, true, 6L, true);
        assertFalse(sub.isEmpty());
    }
}
