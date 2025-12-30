package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * FxReadTransaction 단위 테스트
 *
 * <p>Phase A: 기본 기능 테스트</p>
 */
public class FxReadTransactionTest {

    private FxStore store;
    private NavigableMap<Long, String> testMap;
    private NavigableSet<Long> testSet;
    private List<String> testList;
    private Deque<String> testDeque;

    @Before
    public void setUp() {
        store = FxStoreImpl.openMemory(FxOptions.defaults());

        // Map 테스트 데이터
        testMap = store.createMap("testMap", Long.class, String.class);
        testMap.put(1L, "one");
        testMap.put(2L, "two");
        testMap.put(3L, "three");
        testMap.put(5L, "five");
        testMap.put(10L, "ten");

        // Set 테스트 데이터
        testSet = store.createSet("testSet", Long.class);
        testSet.add(10L);
        testSet.add(20L);
        testSet.add(30L);
        testSet.add(50L);

        // List 테스트 데이터
        testList = store.createList("testList", String.class);
        testList.add("first");
        testList.add("second");
        testList.add("third");
        testList.add("fourth");

        // Deque 테스트 데이터
        testDeque = store.createDeque("testDeque", String.class);
        testDeque.addLast("alpha");
        testDeque.addLast("beta");
        testDeque.addLast("gamma");
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

    // ==================== 기본 트랜잭션 생명주기 ====================

    @Test
    public void testBeginRead_returnsActiveTransaction() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertNotNull(tx);
            assertTrue(tx.isActive());
        }
    }

    @Test
    public void testClose_deactivatesTransaction() {
        FxReadTransaction tx = store.beginRead();
        assertTrue(tx.isActive());
        tx.close();
        assertFalse(tx.isActive());
    }

    @Test
    public void testClose_isIdempotent() {
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.close();  // 두 번 호출해도 예외 없음
        assertFalse(tx.isActive());
    }

    @Test
    public void testGetSnapshotSeqNo_returnsValidSequence() {
        try (FxReadTransaction tx = store.beginRead()) {
            long seqNo = tx.getSnapshotSeqNo();
            assertTrue(seqNo >= 0);
        }
    }

    // ==================== Map 연산 ====================

    @Test
    public void testMapGet_existingKey() {
        try (FxReadTransaction tx = store.beginRead()) {
            String value = tx.get(testMap, 1L);
            assertEquals("one", value);
        }
    }

    @Test
    public void testMapGet_nonExistingKey() {
        try (FxReadTransaction tx = store.beginRead()) {
            String value = tx.get(testMap, 999L);
            assertNull(value);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testMapGet_nullKey() {
        try (FxReadTransaction tx = store.beginRead()) {
            tx.get(testMap, null);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testMapGet_afterClose() {
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.get(testMap, 1L);  // 닫힌 트랜잭션에서 예외
    }

    @Test
    public void testMapContainsKey_existing() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertTrue(tx.containsKey(testMap, 1L));
            assertTrue(tx.containsKey(testMap, 5L));
        }
    }

    @Test
    public void testMapContainsKey_nonExisting() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertFalse(tx.containsKey(testMap, 999L));
        }
    }

    @Test
    public void testMapFirstEntry() {
        try (FxReadTransaction tx = store.beginRead()) {
            Map.Entry<Long, String> first = tx.firstEntry(testMap);
            assertNotNull(first);
            assertEquals(Long.valueOf(1L), first.getKey());
            assertEquals("one", first.getValue());
        }
    }

    @Test
    public void testMapLastEntry() {
        try (FxReadTransaction tx = store.beginRead()) {
            Map.Entry<Long, String> last = tx.lastEntry(testMap);
            assertNotNull(last);
            assertEquals(Long.valueOf(10L), last.getKey());
            assertEquals("ten", last.getValue());
        }
    }

    @Test
    public void testMapSize() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(5, tx.size(testMap));
        }
    }

    @Test
    public void testMapFirstEntry_emptyMap() {
        NavigableMap<String, Integer> emptyMap = store.createMap("emptyMap", String.class, Integer.class);
        try (FxReadTransaction tx = store.beginRead()) {
            Map.Entry<String, Integer> first = tx.firstEntry(emptyMap);
            assertNull(first);
        }
    }

    // ==================== 스냅샷 격리 (Snapshot Isolation) ====================

    @Test
    public void testSnapshotIsolation_writesAfterTransactionStart() {
        // 트랜잭션 시작
        try (FxReadTransaction tx = store.beginRead()) {
            // 트랜잭션 시작 시점 데이터 확인
            assertEquals("one", tx.get(testMap, 1L));
            assertEquals(5, tx.size(testMap));

            // 트랜잭션 중에 새 데이터 삽입
            testMap.put(100L, "hundred");
            testMap.put(200L, "two hundred");

            // 트랜잭션 내에서는 이전 스냅샷만 보임 (INV-RT1, INV-RT3)
            assertNull(tx.get(testMap, 100L));
            assertNull(tx.get(testMap, 200L));
            assertEquals(5, tx.size(testMap));  // 여전히 5개
        }

        // 트랜잭션 종료 후 새 트랜잭션에서는 변경 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertEquals("hundred", tx2.get(testMap, 100L));
            assertEquals("two hundred", tx2.get(testMap, 200L));
            assertEquals(7, tx2.size(testMap));  // 7개
        }
    }

    @Test
    public void testSnapshotIsolation_updatesAfterTransactionStart() {
        try (FxReadTransaction tx = store.beginRead()) {
            // 트랜잭션 시작 시점 값 확인
            assertEquals("one", tx.get(testMap, 1L));

            // 트랜잭션 중에 값 업데이트
            testMap.put(1L, "ONE_UPDATED");

            // 트랜잭션 내에서는 이전 값만 보임
            assertEquals("one", tx.get(testMap, 1L));
        }

        // 트랜잭션 종료 후 새 트랜잭션에서는 변경 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertEquals("ONE_UPDATED", tx2.get(testMap, 1L));
        }
    }

    @Test
    public void testSnapshotIsolation_deletesAfterTransactionStart() {
        try (FxReadTransaction tx = store.beginRead()) {
            // 트랜잭션 시작 시점 데이터 확인
            assertEquals("two", tx.get(testMap, 2L));
            assertTrue(tx.containsKey(testMap, 2L));

            // 트랜잭션 중에 삭제
            testMap.remove(2L);

            // 트랜잭션 내에서는 여전히 보임
            assertEquals("two", tx.get(testMap, 2L));
            assertTrue(tx.containsKey(testMap, 2L));
        }

        // 트랜잭션 종료 후 새 트랜잭션에서는 삭제 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertNull(tx2.get(testMap, 2L));
            assertFalse(tx2.containsKey(testMap, 2L));
        }
    }

    // ==================== Set 연산 ====================

    @Test
    public void testSetContains_existing() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertTrue(tx.contains(testSet, 10L));
            assertTrue(tx.contains(testSet, 30L));
            assertTrue(tx.contains(testSet, 50L));
        }
    }

    @Test
    public void testSetContains_nonExisting() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertFalse(tx.contains(testSet, 999L));
            assertFalse(tx.contains(testSet, 0L));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testSetContains_nullElement() {
        try (FxReadTransaction tx = store.beginRead()) {
            tx.contains(testSet, null);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testSetContains_afterClose() {
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.contains(testSet, 10L);
    }

    @Test
    public void testSetFirst() {
        try (FxReadTransaction tx = store.beginRead()) {
            Long first = tx.first(testSet);
            assertNotNull(first);
            assertEquals(Long.valueOf(10L), first);
        }
    }

    @Test
    public void testSetLast() {
        try (FxReadTransaction tx = store.beginRead()) {
            Long last = tx.last(testSet);
            assertNotNull(last);
            assertEquals(Long.valueOf(50L), last);
        }
    }

    @Test
    public void testSetFirst_emptySet() {
        NavigableSet<String> emptySet = store.createSet("emptySet", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            String first = tx.first(emptySet);
            assertNull(first);
        }
    }

    @Test
    public void testSetLast_emptySet() {
        NavigableSet<String> emptySet = store.createSet("emptySet", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            String last = tx.last(emptySet);
            assertNull(last);
        }
    }

    @Test
    public void testSetSize() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(4, tx.size(testSet));
        }
    }

    @Test
    public void testSetSize_emptySet() {
        NavigableSet<String> emptySet = store.createSet("emptySet", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(0, tx.size(emptySet));
        }
    }

    @Test
    public void testSetSnapshotIsolation_addsAfterTransactionStart() {
        try (FxReadTransaction tx = store.beginRead()) {
            // 트랜잭션 시작 시점
            assertTrue(tx.contains(testSet, 10L));
            assertEquals(4, tx.size(testSet));

            // 트랜잭션 중 새 요소 추가
            testSet.add(100L);
            testSet.add(200L);

            // 트랜잭션 내에서는 이전 스냅샷만 보임
            assertFalse(tx.contains(testSet, 100L));
            assertFalse(tx.contains(testSet, 200L));
            assertEquals(4, tx.size(testSet));
        }

        // 새 트랜잭션에서는 변경 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertTrue(tx2.contains(testSet, 100L));
            assertTrue(tx2.contains(testSet, 200L));
            assertEquals(6, tx2.size(testSet));
        }
    }

    @Test
    public void testSetSnapshotIsolation_removesAfterTransactionStart() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertTrue(tx.contains(testSet, 20L));

            // 트랜잭션 중 삭제
            testSet.remove(20L);

            // 트랜잭션 내에서는 여전히 보임
            assertTrue(tx.contains(testSet, 20L));
        }

        // 새 트랜잭션에서는 삭제 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertFalse(tx2.contains(testSet, 20L));
        }
    }

    // ==================== List 연산 ====================

    @Test
    public void testListGet_validIndex() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("first", tx.get(testList, 0));
            assertEquals("second", tx.get(testList, 1));
            assertEquals("third", tx.get(testList, 2));
            assertEquals("fourth", tx.get(testList, 3));
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testListGet_negativeIndex() {
        try (FxReadTransaction tx = store.beginRead()) {
            tx.get(testList, -1);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testListGet_indexOutOfBounds() {
        try (FxReadTransaction tx = store.beginRead()) {
            tx.get(testList, 100);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testListGet_afterClose() {
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.get(testList, 0);
    }

    @Test
    public void testListSize() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(4, tx.size(testList));
        }
    }

    @Test
    public void testListSize_emptyList() {
        List<String> emptyList = store.createList("emptyList", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(0, tx.size(emptyList));
        }
    }

    @Test
    public void testListIndexOf_existing() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(0, tx.indexOf(testList, "first"));
            assertEquals(1, tx.indexOf(testList, "second"));
            assertEquals(3, tx.indexOf(testList, "fourth"));
        }
    }

    @Test
    public void testListIndexOf_nonExisting() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(-1, tx.indexOf(testList, "notfound"));
        }
    }

    @Test
    public void testListSnapshotIsolation_addsAfterTransactionStart() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(4, tx.size(testList));
            assertEquals("fourth", tx.get(testList, 3));

            // 트랜잭션 중 새 요소 추가
            testList.add("fifth");
            testList.add("sixth");

            // 트랜잭션 내에서는 이전 스냅샷만 보임
            assertEquals(4, tx.size(testList));
        }

        // 새 트랜잭션에서는 변경 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertEquals(6, tx2.size(testList));
            assertEquals("fifth", tx2.get(testList, 4));
            assertEquals("sixth", tx2.get(testList, 5));
        }
    }

    @Test
    public void testListSnapshotIsolation_updatesAfterTransactionStart() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("first", tx.get(testList, 0));

            // 트랜잭션 중 업데이트
            testList.set(0, "FIRST_UPDATED");

            // 트랜잭션 내에서는 이전 값만 보임
            assertEquals("first", tx.get(testList, 0));
        }

        // 새 트랜잭션에서는 변경 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertEquals("FIRST_UPDATED", tx2.get(testList, 0));
        }
    }

    // ==================== Deque 연산 ====================

    @Test
    public void testDequePeekFirst() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("alpha", tx.peekFirst(testDeque));
        }
    }

    @Test
    public void testDequePeekLast() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("gamma", tx.peekLast(testDeque));
        }
    }

    @Test
    public void testDequePeekFirst_emptyDeque() {
        Deque<String> emptyDeque = store.createDeque("emptyDeque", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.peekFirst(emptyDeque));
        }
    }

    @Test
    public void testDequePeekLast_emptyDeque() {
        Deque<String> emptyDeque = store.createDeque("emptyDeque", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.peekLast(emptyDeque));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testDequePeekFirst_afterClose() {
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.peekFirst(testDeque);
    }

    @Test(expected = IllegalStateException.class)
    public void testDequePeekLast_afterClose() {
        FxReadTransaction tx = store.beginRead();
        tx.close();
        tx.peekLast(testDeque);
    }

    @Test
    public void testDequeSize() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(3, tx.size(testDeque));
        }
    }

    @Test
    public void testDequeSize_emptyDeque() {
        Deque<String> emptyDeque = store.createDeque("emptyDeque", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(0, tx.size(emptyDeque));
        }
    }

    @Test
    public void testDequeSnapshotIsolation_addsAfterTransactionStart() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(3, tx.size(testDeque));
            assertEquals("alpha", tx.peekFirst(testDeque));
            assertEquals("gamma", tx.peekLast(testDeque));

            // 트랜잭션 중 새 요소 추가
            testDeque.addFirst("zero");
            testDeque.addLast("delta");

            // 트랜잭션 내에서는 이전 스냅샷만 보임
            assertEquals(3, tx.size(testDeque));
            assertEquals("alpha", tx.peekFirst(testDeque));
            assertEquals("gamma", tx.peekLast(testDeque));
        }

        // 새 트랜잭션에서는 변경 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertEquals(5, tx2.size(testDeque));
            assertEquals("zero", tx2.peekFirst(testDeque));
            assertEquals("delta", tx2.peekLast(testDeque));
        }
    }

    @Test
    public void testDequeSnapshotIsolation_removesAfterTransactionStart() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("alpha", tx.peekFirst(testDeque));

            // 트랜잭션 중 앞에서 제거
            testDeque.removeFirst();

            // 트랜잭션 내에서는 여전히 보임
            assertEquals("alpha", tx.peekFirst(testDeque));
        }

        // 새 트랜잭션에서는 삭제 반영
        try (FxReadTransaction tx2 = store.beginRead()) {
            assertEquals("beta", tx2.peekFirst(testDeque));
        }
    }

    // ==================== INV-RT5: Store 소속 검증 ====================

    @Test(expected = IllegalArgumentException.class)
    public void testDifferentStoreCollection_throwsException() {
        FxStore otherStore = FxStoreImpl.openMemory(FxOptions.defaults());
        try {
            NavigableMap<Long, String> otherMap = otherStore.createMap("otherMap", Long.class, String.class);
            otherMap.put(1L, "other");

            try (FxReadTransaction tx = store.beginRead()) {
                tx.get(otherMap, 1L);  // 다른 store의 컬렉션은 예외
            }
        } finally {
            otherStore.close();
        }
    }

    // ==================== 여러 트랜잭션 동시 사용 ====================

    @Test
    public void testMultipleTransactions_independentSnapshots() {
        // 첫 번째 트랜잭션 시작
        FxReadTransaction tx1 = store.beginRead();
        long seqNo1 = tx1.getSnapshotSeqNo();

        // 데이터 변경
        testMap.put(50L, "fifty");

        // 두 번째 트랜잭션 시작 (변경 후)
        FxReadTransaction tx2 = store.beginRead();
        long seqNo2 = tx2.getSnapshotSeqNo();

        // 시퀀스 번호가 다름
        assertTrue(seqNo2 > seqNo1);

        // tx1은 변경 전 스냅샷
        assertNull(tx1.get(testMap, 50L));

        // tx2는 변경 후 스냅샷
        assertEquals("fifty", tx2.get(testMap, 50L));

        tx1.close();
        tx2.close();
    }

    // ==================== 추가 엣지 케이스 ====================

    @Test(expected = NullPointerException.class)
    public void testMapContainsKey_nullKey() {
        try (FxReadTransaction tx = store.beginRead()) {
            tx.containsKey(testMap, null);
        }
    }

    @Test
    public void testListIndexOf_nullElement() {
        try (FxReadTransaction tx = store.beginRead()) {
            // null은 항상 -1 반환 (NPE 대신)
            assertEquals(-1, tx.indexOf(testList, null));
        }
    }

    @Test
    public void testMapContainsKey_emptyMap() {
        NavigableMap<String, Integer> emptyMap = store.createMap("emptyContainsMap", String.class, Integer.class);
        try (FxReadTransaction tx = store.beginRead()) {
            assertFalse(tx.containsKey(emptyMap, "key"));
        }
    }

    @Test
    public void testMapFirstEntry_cursor() {
        try (FxReadTransaction tx = store.beginRead()) {
            // 첫 번째 엔트리가 존재하는 경우
            Map.Entry<Long, String> first = tx.firstEntry(testMap);
            assertNotNull(first);
            assertEquals(Long.valueOf(1L), first.getKey());
        }
    }

    @Test
    public void testMapLastEntry_cursor() {
        try (FxReadTransaction tx = store.beginRead()) {
            // 마지막 엔트리가 존재하는 경우
            Map.Entry<Long, String> last = tx.lastEntry(testMap);
            assertNotNull(last);
            assertEquals(Long.valueOf(10L), last.getKey());
        }
    }

    @Test
    public void testSetContains_emptySet() {
        NavigableSet<String> emptySet = store.createSet("emptyContainsSet", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            assertFalse(tx.contains(emptySet, "element"));
        }
    }

    @Test
    public void testDequeSize_nonEmpty() {
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(3, tx.size(testDeque));
        }
    }

    @Test
    public void testListGet_emptyList() {
        List<String> emptyList = store.createList("emptyGetList", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            // 빈 리스트에서 get은 IndexOutOfBoundsException
            try {
                tx.get(emptyList, 0);
                fail("Expected IndexOutOfBoundsException");
            } catch (IndexOutOfBoundsException e) {
                // expected
            }
        }
    }

    @Test
    public void testListIndexOf_emptyList() {
        List<String> emptyList = store.createList("emptyIndexOfList", String.class);
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals(-1, tx.indexOf(emptyList, "element"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonFxCollectionMap_throwsException() {
        // 일반 TreeMap (FxCollection 아님)은 예외 발생
        NavigableMap<Long, String> regularMap = new TreeMap<>();
        regularMap.put(1L, "one");

        try (FxReadTransaction tx = store.beginRead()) {
            tx.get(regularMap, 1L);  // FxCollection이 아니므로 예외
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonFxCollectionSet_throwsException() {
        // 일반 TreeSet (FxCollection 아님)은 예외 발생
        NavigableSet<Long> regularSet = new TreeSet<>();
        regularSet.add(1L);

        try (FxReadTransaction tx = store.beginRead()) {
            tx.contains(regularSet, 1L);  // FxCollection이 아니므로 예외
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonFxCollectionList_throwsException() {
        // 일반 ArrayList (FxCollection 아님)은 예외 발생
        List<String> regularList = new ArrayList<>();
        regularList.add("item");

        try (FxReadTransaction tx = store.beginRead()) {
            tx.get(regularList, 0);  // FxCollection이 아니므로 예외
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonFxCollectionDeque_throwsException() {
        // 일반 ArrayDeque (FxCollection 아님)은 예외 발생
        Deque<String> regularDeque = new ArrayDeque<>();
        regularDeque.add("item");

        try (FxReadTransaction tx = store.beginRead()) {
            tx.peekFirst(regularDeque);  // FxCollection이 아니므로 예외
        }
    }

    // ==================== 스냅샷 null 케이스 ====================

    @Test
    public void testCollectionCreatedAfterTransaction_isEmpty() {
        // 트랜잭션 시작
        FxReadTransaction tx = store.beginRead();

        // 트랜잭션 시작 후 새 컬렉션 생성
        NavigableMap<String, Integer> newMap = store.createMap("newAfterTxMap", String.class, Integer.class);
        newMap.put("key", 100);

        // 트랜잭션에서는 이 컬렉션이 비어있음 (스냅샷에 rootPageId 없음)
        // 새로 생성된 컬렉션도 validateCollection은 통과하지만 데이터는 빈 상태
        assertNull(tx.get(newMap, "key"));
        assertFalse(tx.containsKey(newMap, "key"));
        assertNull(tx.firstEntry(newMap));
        assertNull(tx.lastEntry(newMap));
        assertEquals(0, tx.size(newMap));

        tx.close();
    }

    @Test
    public void testSetCreatedAfterTransaction_isEmpty() {
        FxReadTransaction tx = store.beginRead();

        NavigableSet<Long> newSet = store.createSet("newAfterTxSet", Long.class);
        newSet.add(999L);

        // 트랜잭션 시작 후 생성된 컬렉션은 비어있음
        assertFalse(tx.contains(newSet, 999L));
        assertNull(tx.first(newSet));
        assertNull(tx.last(newSet));
        assertEquals(0, tx.size(newSet));

        tx.close();
    }

    @Test
    public void testListCreatedAfterTransaction_isEmpty() {
        FxReadTransaction tx = store.beginRead();

        List<String> newList = store.createList("newAfterTxList", String.class);
        newList.add("item");

        // 트랜잭션 시작 후 생성된 컬렉션은 비어있음
        assertEquals(0, tx.size(newList));
        assertEquals(-1, tx.indexOf(newList, "item"));

        tx.close();
    }

    @Test
    public void testDequeCreatedAfterTransaction_isEmpty() {
        FxReadTransaction tx = store.beginRead();

        Deque<String> newDeque = store.createDeque("newAfterTxDeque", String.class);
        newDeque.add("item");

        // 트랜잭션 시작 후 생성된 컬렉션은 비어있음
        assertNull(tx.peekFirst(newDeque));
        assertNull(tx.peekLast(newDeque));
        assertEquals(0, tx.size(newDeque));

        tx.close();
    }
}
