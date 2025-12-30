package com.snoworca.fxstore.btree;

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
 * BTreeCursor 테스트
 *
 * <p>P1-5: BTreeCursor를 통한 순회 기능 테스트</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>기본 순회 (hasNext/next)</li>
 *   <li>peek 메서드</li>
 *   <li>범위 순회 (startKey/endKey)</li>
 *   <li>빈 트리 순회</li>
 *   <li>단일 요소 순회</li>
 *   <li>대용량 데이터 순회</li>
 * </ul>
 *
 * @since v1.0 Phase 2
 * @see BTreeCursor
 */
public class BTreeCursorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;
    private NavigableMap<Long, String> map;

    @Before
    public void setUp() throws Exception {
        File storeFile = tempFolder.newFile("test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
        map = store.createMap("test", Long.class, String.class);
    }

    @After
    public void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    // ==================== 기본 순회 테스트 ====================

    @Test
    public void iterator_emptyMap_hasNoElements() {
        // Given: 빈 맵
        Iterator<Map.Entry<Long, String>> iterator = map.entrySet().iterator();

        // Then
        assertFalse(iterator.hasNext());
    }

    @Test
    public void iterator_singleElement_shouldIterateOnce() {
        // Given
        map.put(1L, "one");
        Iterator<Map.Entry<Long, String>> iterator = map.entrySet().iterator();

        // Then
        assertTrue(iterator.hasNext());
        Map.Entry<Long, String> entry = iterator.next();
        assertEquals(Long.valueOf(1L), entry.getKey());
        assertEquals("one", entry.getValue());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void iterator_multipleElements_shouldIterateInOrder() {
        // Given: 순서 없이 삽입
        map.put(30L, "thirty");
        map.put(10L, "ten");
        map.put(50L, "fifty");
        map.put(20L, "twenty");
        map.put(40L, "forty");

        // When
        List<Long> keys = new ArrayList<>();
        for (Map.Entry<Long, String> entry : map.entrySet()) {
            keys.add(entry.getKey());
        }

        // Then: 정렬 순서로 반환
        assertEquals(5, keys.size());
        assertEquals(Long.valueOf(10L), keys.get(0));
        assertEquals(Long.valueOf(20L), keys.get(1));
        assertEquals(Long.valueOf(30L), keys.get(2));
        assertEquals(Long.valueOf(40L), keys.get(3));
        assertEquals(Long.valueOf(50L), keys.get(4));
    }

    @Test(expected = NoSuchElementException.class)
    public void iterator_nextWithoutHasNext_shouldThrow() {
        // Given: 빈 맵
        Iterator<Map.Entry<Long, String>> iterator = map.entrySet().iterator();

        // When: hasNext 없이 next 호출
        iterator.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void iterator_nextAfterExhausted_shouldThrow() {
        // Given
        map.put(1L, "one");
        Iterator<Map.Entry<Long, String>> iterator = map.entrySet().iterator();

        // When: 모든 요소 소비 후 next 호출
        iterator.next();
        iterator.next(); // 예외
    }

    // ==================== keySet 순회 테스트 ====================

    @Test
    public void keySet_shouldIterateInOrder() {
        // Given
        map.put(30L, "thirty");
        map.put(10L, "ten");
        map.put(20L, "twenty");

        // When
        List<Long> keys = new ArrayList<>(map.keySet());

        // Then
        assertEquals(3, keys.size());
        assertEquals(Long.valueOf(10L), keys.get(0));
        assertEquals(Long.valueOf(20L), keys.get(1));
        assertEquals(Long.valueOf(30L), keys.get(2));
    }

    @Test
    public void keySet_empty_shouldBeEmpty() {
        assertTrue(map.keySet().isEmpty());
    }

    // ==================== values 순회 테스트 ====================

    @Test
    public void values_shouldIterateInKeyOrder() {
        // Given
        map.put(30L, "thirty");
        map.put(10L, "ten");
        map.put(20L, "twenty");

        // When
        List<String> values = new ArrayList<>(map.values());

        // Then: 키 순서대로 값 반환
        assertEquals(3, values.size());
        assertEquals("ten", values.get(0));
        assertEquals("twenty", values.get(1));
        assertEquals("thirty", values.get(2));
    }

    // ==================== 범위 순회 테스트 (subMap/headMap/tailMap) ====================

    @Test
    public void subMap_iterator() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");
        map.put(40L, "forty");
        map.put(50L, "fifty");

        // When
        NavigableMap<Long, String> sub = map.subMap(20L, true, 40L, true);
        List<Long> keys = new ArrayList<>();
        for (Long key : sub.keySet()) {
            keys.add(key);
        }

        // Then
        assertEquals(3, keys.size());
        assertEquals(Long.valueOf(20L), keys.get(0));
        assertEquals(Long.valueOf(30L), keys.get(1));
        assertEquals(Long.valueOf(40L), keys.get(2));
    }

    @Test
    public void headMap_iterator() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");
        map.put(40L, "forty");

        // When
        NavigableMap<Long, String> head = map.headMap(30L, false);
        List<Long> keys = new ArrayList<>();
        for (Long key : head.keySet()) {
            keys.add(key);
        }

        // Then
        assertEquals(2, keys.size());
        assertEquals(Long.valueOf(10L), keys.get(0));
        assertEquals(Long.valueOf(20L), keys.get(1));
    }

    @Test
    public void tailMap_iterator() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");
        map.put(40L, "forty");

        // When
        NavigableMap<Long, String> tail = map.tailMap(30L, true);
        List<Long> keys = new ArrayList<>();
        for (Long key : tail.keySet()) {
            keys.add(key);
        }

        // Then
        assertEquals(2, keys.size());
        assertEquals(Long.valueOf(30L), keys.get(0));
        assertEquals(Long.valueOf(40L), keys.get(1));
    }

    // ==================== descendingMap 순회 테스트 ====================

    @Test
    public void descendingMap_iterator() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");

        // When
        NavigableMap<Long, String> desc = map.descendingMap();
        List<Long> keys = new ArrayList<>();
        for (Long key : desc.keySet()) {
            keys.add(key);
        }

        // Then: 역순
        assertEquals(3, keys.size());
        assertEquals(Long.valueOf(30L), keys.get(0));
        assertEquals(Long.valueOf(20L), keys.get(1));
        assertEquals(Long.valueOf(10L), keys.get(2));
    }

    @Test
    public void descendingKeySet_iterator() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");

        // When
        NavigableSet<Long> descKeys = map.descendingKeySet();
        List<Long> keys = new ArrayList<>();
        for (Long key : descKeys) {
            keys.add(key);
        }

        // Then: 역순
        assertEquals(3, keys.size());
        assertEquals(Long.valueOf(30L), keys.get(0));
        assertEquals(Long.valueOf(20L), keys.get(1));
        assertEquals(Long.valueOf(10L), keys.get(2));
    }

    // ==================== 대용량 데이터 순회 테스트 ====================

    @Test
    public void iterator_largeData_shouldIterateAll() {
        // Given: 많은 데이터 (여러 페이지에 걸쳐 저장됨)
        for (long i = 0; i < 100; i++) {
            map.put(i, "value" + i);
        }

        // When
        int count = 0;
        Set<Long> seenKeys = new HashSet<>();
        for (Map.Entry<Long, String> entry : map.entrySet()) {
            seenKeys.add(entry.getKey());
            count++;
        }

        // Then
        assertEquals(100, count);
        assertEquals(100, seenKeys.size());
    }

    @Test
    public void iterator_largeData_subMap() {
        // Given: 많은 데이터
        for (long i = 0; i < 100; i++) {
            map.put(i, "value" + i);
        }

        // When: 중간 범위 조회
        NavigableMap<Long, String> sub = map.subMap(25L, true, 75L, false);
        Set<Long> seenKeys = new HashSet<>();
        for (Map.Entry<Long, String> entry : sub.entrySet()) {
            seenKeys.add(entry.getKey());
        }

        // Then: 범위 내 모든 키 포함
        assertEquals(50, seenKeys.size());
        for (long i = 25; i < 75; i++) {
            assertTrue("Should contain key " + i, seenKeys.contains(i));
        }
    }

    // ==================== 동시 수정 시 순회 테스트 ====================

    @Test
    public void iterator_whileModifying_shouldWork() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");

        // When: 순회 중 새 요소 추가
        List<Long> keys = new ArrayList<>();
        for (Long key : map.keySet()) {
            keys.add(key);
            if (key == 20L) {
                map.put(25L, "twenty-five");
            }
        }

        // Then: 원래 요소는 순회됨 (새로 추가된 것 포함 여부는 구현에 따라 다름)
        assertTrue(keys.contains(10L));
        assertTrue(keys.contains(20L));
        assertTrue(keys.contains(30L));
    }

    // ==================== navigableKeySet 메서드 테스트 ====================

    @Test
    public void navigableKeySet_pollFirst() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");

        // When
        NavigableSet<Long> keySet = map.navigableKeySet();
        Long first = keySet.pollFirst();

        // Then
        assertEquals(Long.valueOf(10L), first);
        assertEquals(2, map.size());
        assertFalse(map.containsKey(10L));
    }

    @Test
    public void navigableKeySet_pollLast() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");

        // When
        NavigableSet<Long> keySet = map.navigableKeySet();
        Long last = keySet.pollLast();

        // Then
        assertEquals(Long.valueOf(30L), last);
        assertEquals(2, map.size());
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void navigableKeySet_subSet() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");
        map.put(40L, "forty");

        // When
        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> subSet = keySet.subSet(15L, true, 35L, true);

        // Then
        assertEquals(2, subSet.size());
        assertTrue(subSet.contains(20L));
        assertTrue(subSet.contains(30L));
    }

    @Test
    public void navigableKeySet_headSet() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");

        // When
        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> headSet = keySet.headSet(25L, true);

        // Then
        assertEquals(2, headSet.size());
        assertTrue(headSet.contains(10L));
        assertTrue(headSet.contains(20L));
    }

    @Test
    public void navigableKeySet_tailSet() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");

        // When
        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> tailSet = keySet.tailSet(15L, true);

        // Then
        assertEquals(2, tailSet.size());
        assertTrue(tailSet.contains(20L));
        assertTrue(tailSet.contains(30L));
    }

    // ==================== 정렬 순서 검증 테스트 ====================

    @Test
    public void iterator_shouldMaintainSortOrder() {
        // Given: 무작위 순서로 삽입
        long[] insertOrder = {50L, 10L, 30L, 20L, 40L};
        for (long key : insertOrder) {
            map.put(key, "value" + key);
        }

        // When
        List<Long> iteratedKeys = new ArrayList<>();
        for (Long key : map.keySet()) {
            iteratedKeys.add(key);
        }

        // Then: 오름차순 정렬
        for (int i = 0; i < iteratedKeys.size() - 1; i++) {
            assertTrue(iteratedKeys.get(i) < iteratedKeys.get(i + 1));
        }
    }

    @Test
    public void descendingIterator_shouldMaintainReverseOrder() {
        // Given
        for (long i = 1; i <= 10; i++) {
            map.put(i, "value" + i);
        }

        // When
        NavigableSet<Long> descKeySet = map.descendingKeySet();
        List<Long> iteratedKeys = new ArrayList<>();
        for (Long key : descKeySet) {
            iteratedKeys.add(key);
        }

        // Then: 내림차순 정렬
        for (int i = 0; i < iteratedKeys.size() - 1; i++) {
            assertTrue(iteratedKeys.get(i) > iteratedKeys.get(i + 1));
        }
    }

    // ==================== entrySet 메서드 테스트 ====================

    @Test
    public void entrySet_shouldContainAllEntries() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");

        // When
        Set<Map.Entry<Long, String>> entries = map.entrySet();

        // Then
        assertEquals(3, entries.size());

        boolean found10 = false, found20 = false, found30 = false;
        for (Map.Entry<Long, String> entry : entries) {
            if (entry.getKey() == 10L && "ten".equals(entry.getValue())) found10 = true;
            if (entry.getKey() == 20L && "twenty".equals(entry.getValue())) found20 = true;
            if (entry.getKey() == 30L && "thirty".equals(entry.getValue())) found30 = true;
        }

        assertTrue(found10);
        assertTrue(found20);
        assertTrue(found30);
    }

    // ==================== 다양한 키 타입 테스트 ====================

    @Test
    public void iterator_stringKeys() {
        // Given: 문자열 키
        NavigableMap<String, Integer> stringMap = store.createMap("stringMap", String.class, Integer.class);
        stringMap.put("banana", 2);
        stringMap.put("apple", 1);
        stringMap.put("cherry", 3);

        // When
        List<String> keys = new ArrayList<>();
        for (String key : stringMap.keySet()) {
            keys.add(key);
        }

        // Then: 사전 순 정렬
        assertEquals(3, keys.size());
        assertEquals("apple", keys.get(0));
        assertEquals("banana", keys.get(1));
        assertEquals("cherry", keys.get(2));
    }

    @Test
    public void iterator_integerKeys() {
        // Given: Integer 키
        NavigableMap<Integer, String> intMap = store.createMap("intMap", Integer.class, String.class);
        intMap.put(100, "hundred");
        intMap.put(10, "ten");
        intMap.put(50, "fifty");

        // When
        List<Integer> keys = new ArrayList<>();
        for (Integer key : intMap.keySet()) {
            keys.add(key);
        }

        // Then: 숫자 순 정렬
        assertEquals(3, keys.size());
        assertEquals(Integer.valueOf(10), keys.get(0));
        assertEquals(Integer.valueOf(50), keys.get(1));
        assertEquals(Integer.valueOf(100), keys.get(2));
    }

    // ==================== 재오픈 후 순회 테스트 ====================

    @Test
    public void iterator_afterReopen_shouldWork() throws Exception {
        // Given: 데이터 저장
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");
        store.close();

        // When: 재오픈
        File storeFile = new File(tempFolder.getRoot(), "test.fx");
        store = FxStore.open(storeFile.toPath());
        map = store.openMap("test", Long.class, String.class);

        // Then: 순회 가능
        List<Long> keys = new ArrayList<>();
        for (Long key : map.keySet()) {
            keys.add(key);
        }

        assertEquals(3, keys.size());
        assertEquals(Long.valueOf(10L), keys.get(0));
        assertEquals(Long.valueOf(20L), keys.get(1));
        assertEquals(Long.valueOf(30L), keys.get(2));
    }

    // ==================== 메모리 스토어 순회 테스트 ====================

    @Test
    public void iterator_memoryStore() {
        // Given: 메모리 스토어
        store.close();
        store = FxStore.openMemory();
        map = store.createMap("memTest", Long.class, String.class);
        map.put(30L, "thirty");
        map.put(10L, "ten");
        map.put(20L, "twenty");

        // When
        List<Long> keys = new ArrayList<>();
        for (Long key : map.keySet()) {
            keys.add(key);
        }

        // Then
        assertEquals(3, keys.size());
        assertEquals(Long.valueOf(10L), keys.get(0));
        assertEquals(Long.valueOf(20L), keys.get(1));
        assertEquals(Long.valueOf(30L), keys.get(2));
    }

    // ==================== 삭제 후 순회 테스트 ====================

    @Test
    public void iterator_afterRemove_shouldSkipRemoved() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.put(30L, "thirty");
        map.remove(20L);

        // When
        List<Long> keys = new ArrayList<>();
        for (Long key : map.keySet()) {
            keys.add(key);
        }

        // Then
        assertEquals(2, keys.size());
        assertTrue(keys.contains(10L));
        assertTrue(keys.contains(30L));
        assertFalse(keys.contains(20L));
    }

    @Test
    public void iterator_afterClear_shouldBeEmpty() {
        // Given
        map.put(10L, "ten");
        map.put(20L, "twenty");
        map.clear();

        // When
        Iterator<Map.Entry<Long, String>> iterator = map.entrySet().iterator();

        // Then
        assertFalse(iterator.hasNext());
    }

    // ==================== size() 메서드 일관성 테스트 ====================

    @Test
    public void size_shouldMatchIterationCount() {
        // Given
        for (long i = 0; i < 100; i++) {
            map.put(i, "value" + i);
        }

        // When
        int iteratedCount = 0;
        for (Map.Entry<Long, String> entry : map.entrySet()) {
            iteratedCount++;
        }

        // Then
        assertEquals(map.size(), iteratedCount);
    }
}
