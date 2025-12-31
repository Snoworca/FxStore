package com.snoworca.fxstore.integration;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/**
 * 빈 컬렉션 및 엣지 케이스 통합 테스트.
 *
 * <p>목적:</p>
 * <ul>
 *   <li>빈 컬렉션에서의 연산 테스트</li>
 *   <li>경계값 테스트</li>
 *   <li>null 처리 테스트</li>
 * </ul>
 */
public class EmptyCollectionEdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;

    @Before
    public void setUp() {
        store = FxStore.openMemory();
    }

    @After
    public void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    // ==================== Empty Deque Tests ====================

    /**
     * 빈 Deque의 모든 peek 연산 테스트.
     */
    @Test
    public void testEmptyDeque_allPeekOperations() {
        Deque<String> deque = store.createDeque("emptyDeque", String.class);

        assertNull(deque.peekFirst());
        assertNull(deque.peekLast());
        assertNull(deque.pollFirst());
        assertNull(deque.pollLast());
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }

    /**
     * 빈 Deque에서 예외 발생 연산 테스트.
     */
    @Test
    public void testEmptyDeque_exceptionOperations() {
        Deque<String> deque = store.createDeque("exDeque", String.class);

        try {
            deque.getFirst();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }

        try {
            deque.getLast();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }

        try {
            deque.removeFirst();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }

        try {
            deque.removeLast();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    /**
     * Deque 단일 요소 추가/제거.
     */
    @Test
    public void testDeque_singleElement() {
        Deque<String> deque = store.createDeque("singleDeque", String.class);

        deque.addFirst("only");
        assertEquals(1, deque.size());
        assertEquals("only", deque.peekFirst());
        assertEquals("only", deque.peekLast());

        String removed = deque.removeFirst();
        assertEquals("only", removed);
        assertTrue(deque.isEmpty());
    }

    // ==================== Empty List Tests ====================

    /**
     * 빈 List의 모든 연산 테스트.
     */
    @Test
    public void testEmptyList_allOperations() {
        List<Integer> list = store.createList("emptyList", Integer.class);

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());

        try {
            list.get(0);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            list.remove(0);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    /**
     * List 단일 요소 추가/제거.
     */
    @Test
    public void testList_singleElement() {
        List<Integer> list = store.createList("singleList", Integer.class);

        list.add(42);
        assertEquals(1, list.size());
        assertEquals(Integer.valueOf(42), list.get(0));

        list.remove(0);
        assertTrue(list.isEmpty());
    }

    /**
     * List 경계값 테스트.
     */
    @Test
    public void testList_boundaryIndices() {
        List<Long> list = store.createList("boundaryList", Long.class);

        // 10개 요소 추가
        for (int i = 0; i < 10; i++) {
            list.add((long) i);
        }

        // 경계 접근
        assertEquals(Long.valueOf(0L), list.get(0));
        assertEquals(Long.valueOf(9L), list.get(9));

        // 경계 밖 접근
        try {
            list.get(-1);
            fail("Expected IndexOutOfBoundsException for negative index");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            list.get(10);
            fail("Expected IndexOutOfBoundsException for index >= size");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    // ==================== Empty Map Tests ====================

    /**
     * 빈 NavigableMap의 모든 연산 테스트.
     */
    @Test
    public void testEmptyMap_allOperations() {
        NavigableMap<Long, String> map = store.createMap("emptyMap", Long.class, String.class);

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(1L));
        assertNull(map.firstEntry());
        assertNull(map.lastEntry());
        assertNull(map.pollFirstEntry());
        assertNull(map.pollLastEntry());
        assertNull(map.floorEntry(100L));
        assertNull(map.ceilingEntry(100L));
        assertNull(map.lowerEntry(100L));
        assertNull(map.higherEntry(100L));
    }

    /**
     * Map 단일 엔트리 추가/제거.
     */
    @Test
    public void testMap_singleEntry() {
        NavigableMap<Long, String> map = store.createMap("singleMap", Long.class, String.class);

        map.put(1L, "one");
        assertEquals(1, map.size());
        assertEquals("one", map.get(1L));
        assertEquals(Long.valueOf(1L), map.firstKey());
        assertEquals(Long.valueOf(1L), map.lastKey());

        map.remove(1L);
        assertTrue(map.isEmpty());
    }

    // ==================== Empty Set Tests ====================

    /**
     * 빈 NavigableSet의 모든 연산 테스트.
     */
    @Test
    public void testEmptySet_allOperations() {
        NavigableSet<Long> set = store.createSet("emptySet", Long.class);

        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains(1L));
        assertNull(set.pollFirst());
        assertNull(set.pollLast());
        assertNull(set.floor(100L));
        assertNull(set.ceiling(100L));
        assertNull(set.lower(100L));
        assertNull(set.higher(100L));
    }

    /**
     * Set 단일 요소 추가/제거.
     */
    @Test
    public void testSet_singleElement() {
        NavigableSet<Long> set = store.createSet("singleSet", Long.class);

        set.add(42L);
        assertEquals(1, set.size());
        assertTrue(set.contains(42L));
        assertEquals(Long.valueOf(42L), set.first());
        assertEquals(Long.valueOf(42L), set.last());

        set.remove(42L);
        assertTrue(set.isEmpty());
    }

    // ==================== Clear Tests ====================

    /**
     * 모든 컬렉션 타입의 clear() 테스트.
     */
    @Test
    public void testClear_allCollectionTypes() {
        NavigableMap<Long, String> map = store.createMap("clearMap", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("clearSet", Long.class);
        List<Long> list = store.createList("clearList", Long.class);
        Deque<Long> deque = store.createDeque("clearDeque", Long.class);

        // 데이터 추가
        for (long i = 0; i < 100; i++) {
            map.put(i, "v" + i);
            set.add(i);
            list.add(i);
            deque.add(i);
        }

        assertEquals(100, map.size());
        assertEquals(100, set.size());
        assertEquals(100, list.size());
        assertEquals(100, deque.size());

        // clear
        map.clear();
        set.clear();
        list.clear();
        deque.clear();

        assertTrue(map.isEmpty());
        assertTrue(set.isEmpty());
        assertTrue(list.isEmpty());
        assertTrue(deque.isEmpty());
    }

    /**
     * 빈 컬렉션에서 clear() 호출.
     */
    @Test
    public void testClear_emptyCollections() {
        NavigableMap<Long, String> map = store.createMap("clearEmptyMap", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("clearEmptySet", Long.class);
        List<Long> list = store.createList("clearEmptyList", Long.class);
        Deque<Long> deque = store.createDeque("clearEmptyDeque", Long.class);

        // 빈 상태에서 clear() - 예외 없이 동작해야 함
        map.clear();
        set.clear();
        list.clear();
        deque.clear();

        assertTrue(map.isEmpty());
        assertTrue(set.isEmpty());
        assertTrue(list.isEmpty());
        assertTrue(deque.isEmpty());
    }

    // ==================== Persistence Tests ====================

    /**
     * 빈 컬렉션의 영속성 테스트.
     */
    @Test
    public void testEmptyCollection_persistence() throws Exception {
        Path tempFile = tempFolder.newFile("empty-persist.fx").toPath();

        // 빈 컬렉션 생성
        try (FxStore fileStore = FxStore.open(tempFile)) {
            fileStore.createMap("emptyMap", Long.class, String.class);
            fileStore.createSet("emptySet", Long.class);
            fileStore.createList("emptyList", Long.class);
            fileStore.createDeque("emptyDeque", Long.class);
        }

        // 재오픈 후 검증
        try (FxStore fileStore = FxStore.open(tempFile)) {
            NavigableMap<Long, String> map = fileStore.openMap("emptyMap", Long.class, String.class);
            NavigableSet<Long> set = fileStore.openSet("emptySet", Long.class);
            List<Long> list = fileStore.openList("emptyList", Long.class);
            Deque<Long> deque = fileStore.openDeque("emptyDeque", Long.class);

            assertNotNull(map);
            assertNotNull(set);
            assertNotNull(list);
            assertNotNull(deque);

            assertTrue(map.isEmpty());
            assertTrue(set.isEmpty());
            assertTrue(list.isEmpty());
            assertTrue(deque.isEmpty());
        }
    }

    // ==================== Iterator Tests ====================

    /**
     * 빈 컬렉션의 Iterator 테스트.
     */
    @Test
    public void testEmptyCollection_iterators() {
        NavigableMap<Long, String> map = store.createMap("iterMap", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("iterSet", Long.class);
        List<Long> list = store.createList("iterList", Long.class);
        Deque<Long> deque = store.createDeque("iterDeque", Long.class);

        // 빈 컬렉션에서 iterator
        assertFalse(map.entrySet().iterator().hasNext());
        assertFalse(set.iterator().hasNext());
        assertFalse(list.iterator().hasNext());
        assertFalse(deque.iterator().hasNext());

        // Descending iterators
        assertFalse(map.descendingMap().entrySet().iterator().hasNext());
        assertFalse(set.descendingIterator().hasNext());
        assertFalse(deque.descendingIterator().hasNext());
    }

    // ==================== SubView Tests ====================

    /**
     * 빈 범위의 SubMap/SubSet 테스트.
     */
    @Test
    public void testEmptySubViews() {
        NavigableMap<Long, String> map = store.createMap("subMap", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("subSet", Long.class);

        // 데이터 추가
        for (long i = 10; i < 20; i++) {
            map.put(i, "v" + i);
            set.add(i);
        }

        // 빈 범위의 SubMap/SubSet
        NavigableMap<Long, String> emptySubMap = map.subMap(0L, true, 5L, true);
        NavigableSet<Long> emptySubSet = set.subSet(0L, true, 5L, true);

        assertTrue(emptySubMap.isEmpty());
        assertTrue(emptySubSet.isEmpty());
    }
}
