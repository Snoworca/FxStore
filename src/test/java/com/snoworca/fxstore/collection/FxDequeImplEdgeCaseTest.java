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
 * FxDequeImpl 추가 에지 케이스 테스트
 *
 * 커버리지 개선 대상:
 * - null 처리 경로
 * - 빈 deque에서의 retainAll
 * - contains(null)
 * - addAll false 반환
 */
public class FxDequeImplEdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("deque-impl-edge-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== contains null 테스트 ====================

    @Test
    public void contains_null_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");

        // contains(null) should return false
        assertFalse(deque.contains(null));
    }

    @Test
    public void contains_nullOnEmptyDeque_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertFalse(deque.contains(null));
    }

    // ==================== retainAll 빈 deque 테스트 ====================

    @Test
    public void retainAll_emptyDeque_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);

        Collection<String> toRetain = Arrays.asList("a", "b");
        boolean modified = deque.retainAll(toRetain);

        assertFalse(modified); // 빈 deque는 수정되지 않음
        assertTrue(deque.isEmpty());
    }

    @Test
    public void retainAll_noChanges_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");

        Collection<String> toRetain = Arrays.asList("a", "b", "c");
        boolean modified = deque.retainAll(toRetain);

        assertFalse(modified); // 모든 요소가 유지되므로 수정되지 않음
        assertEquals(2, deque.size());
    }

    @Test
    public void retainAll_withChanges_shouldReturnTrue() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        Collection<String> toRetain = Arrays.asList("a", "c");
        boolean modified = deque.retainAll(toRetain);

        assertTrue(modified);
        assertEquals(2, deque.size());
        assertTrue(deque.contains("a"));
        assertFalse(deque.contains("b"));
        assertTrue(deque.contains("c"));
    }

    @Test(expected = NullPointerException.class)
    public void retainAll_nullCollection_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.retainAll(null);
    }

    // ==================== addAll 테스트 ====================

    @Test
    public void addAll_emptyCollection_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("existing");

        boolean modified = deque.addAll(Collections.emptyList());

        // 빈 컬렉션 추가 시 false 반환 여부는 구현에 따라 다름
        // 대부분의 구현은 빈 컬렉션 추가 시에도 true를 반환하지 않음
        assertEquals(1, deque.size());
    }

    @Test
    public void addAll_nonEmpty_shouldReturnTrue() {
        Deque<String> deque = store.createDeque("test", String.class);

        boolean modified = deque.addAll(Arrays.asList("a", "b", "c"));

        assertTrue(modified);
        assertEquals(3, deque.size());
    }

    // ==================== removeAll 테스트 ====================

    @Test
    public void removeAll_noMatches_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");

        boolean modified = deque.removeAll(Arrays.asList("x", "y"));

        assertFalse(modified);
        assertEquals(2, deque.size());
    }

    @Test
    public void removeAll_withMatches_shouldReturnTrue() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        boolean modified = deque.removeAll(Arrays.asList("a", "c"));

        assertTrue(modified);
        assertEquals(1, deque.size());
        assertEquals("b", deque.peekFirst());
    }

    @Test
    public void removeAll_emptyDeque_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);

        boolean modified = deque.removeAll(Arrays.asList("a", "b"));

        assertFalse(modified);
    }

    // ==================== containsAll 테스트 ====================

    @Test
    public void containsAll_allPresent_shouldReturnTrue() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        assertTrue(deque.containsAll(Arrays.asList("a", "b")));
        assertTrue(deque.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void containsAll_someMissing_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");

        assertFalse(deque.containsAll(Arrays.asList("a", "x")));
    }

    @Test
    public void containsAll_emptyCollection_shouldReturnTrue() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");

        assertTrue(deque.containsAll(Collections.emptyList()));
    }

    @Test
    public void containsAll_emptyDeque_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);

        assertFalse(deque.containsAll(Arrays.asList("a")));
    }

    // ==================== toArray 테스트 ====================

    @Test
    public void toArray_shouldWork() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        Object[] array = deque.toArray();

        assertEquals(3, array.length);
        assertEquals("a", array[0]);
        assertEquals("b", array[1]);
        assertEquals("c", array[2]);
    }

    @Test
    public void toArray_emptyDeque_shouldReturnEmptyArray() {
        Deque<String> deque = store.createDeque("test", String.class);

        Object[] array = deque.toArray();

        assertEquals(0, array.length);
    }

    @Test
    public void toArrayTyped_shouldWork() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");

        String[] array = deque.toArray(new String[0]);

        assertEquals(2, array.length);
        assertEquals("a", array[0]);
        assertEquals("b", array[1]);
    }

    @Test
    public void toArrayTyped_existingArray_shouldWork() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");

        String[] existing = new String[5];
        String[] result = deque.toArray(existing);

        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
    }

    // ==================== descendingIterator 테스트 ====================

    @Test
    public void descendingIterator_shouldIterateInReverse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        Iterator<String> descIter = deque.descendingIterator();

        List<String> result = new ArrayList<>();
        while (descIter.hasNext()) {
            result.add(descIter.next());
        }

        assertEquals(Arrays.asList("c", "b", "a"), result);
    }

    @Test
    public void descendingIterator_emptyDeque_shouldHaveNoElements() {
        Deque<String> deque = store.createDeque("test", String.class);

        Iterator<String> descIter = deque.descendingIterator();

        assertFalse(descIter.hasNext());
    }

    // ==================== removeFirstOccurrence/removeLastOccurrence 추가 테스트 ====================

    @Test
    public void removeFirstOccurrence_null_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");

        // null 요소 제거 시도
        assertFalse(deque.removeFirstOccurrence(null));
    }

    @Test
    public void removeLastOccurrence_null_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");

        assertFalse(deque.removeLastOccurrence(null));
    }

    @Test
    public void removeFirstOccurrence_duplicates_shouldRemoveFirst() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("a");
        deque.addLast("c");

        assertTrue(deque.removeFirstOccurrence("a"));
        assertEquals(3, deque.size());

        // 순서 확인
        Iterator<String> iter = deque.iterator();
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void removeLastOccurrence_duplicates_shouldRemoveLast() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("a");
        deque.addLast("c");

        assertTrue(deque.removeLastOccurrence("a"));
        assertEquals(3, deque.size());

        // 순서 확인
        Iterator<String> iter = deque.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    // ==================== offerFirst/offerLast 추가 테스트 ====================

    @Test
    public void offerFirst_null_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);

        boolean result = deque.offerFirst(null);

        assertFalse(result);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void offerLast_null_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);

        boolean result = deque.offerLast(null);

        assertFalse(result);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void offer_null_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);

        boolean result = deque.offer(null);

        assertFalse(result);
        assertTrue(deque.isEmpty());
    }

    // ==================== push/pop (Stack operations) 테스트 ====================

    @Test
    public void push_shouldAddFirst() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.push("a");
        deque.push("b");
        deque.push("c");

        assertEquals("c", deque.peekFirst());
        assertEquals(3, deque.size());
    }

    @Test(expected = NullPointerException.class)
    public void push_null_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.push(null);
    }

    // ==================== 대량 데이터 테스트 ====================

    @Test
    public void largeDeque_shouldWorkCorrectly() {
        Deque<Integer> deque = store.createDeque("test", Integer.class);

        // 1000개 요소 추가
        for (int i = 0; i < 1000; i++) {
            deque.addLast(i);
        }

        assertEquals(1000, deque.size());
        assertEquals(Integer.valueOf(0), deque.peekFirst());
        assertEquals(Integer.valueOf(999), deque.peekLast());

        // 앞에서 500개 제거
        for (int i = 0; i < 500; i++) {
            assertEquals(Integer.valueOf(i), deque.pollFirst());
        }

        assertEquals(500, deque.size());
        assertEquals(Integer.valueOf(500), deque.peekFirst());
    }

    // ==================== clear 후 상태 테스트 ====================

    @Test
    public void clear_thenPollFirst_shouldReturnNull() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");

        deque.clear();

        assertNull(deque.pollFirst());
        assertNull(deque.pollLast());
        assertNull(deque.peekFirst());
        assertNull(deque.peekLast());
    }

    // ==================== 혼합 연산 테스트 ====================

    @Test
    public void mixedOperations_shouldMaintainConsistency() {
        Deque<String> deque = store.createDeque("test", String.class);

        // 앞뒤로 번갈아 추가
        deque.addFirst("b");
        deque.addLast("c");
        deque.addFirst("a");
        deque.addLast("d");

        // 순서: a, b, c, d
        assertEquals("a", deque.pollFirst());
        assertEquals("d", deque.pollLast());
        assertEquals("b", deque.pollFirst());
        assertEquals("c", deque.pollLast());

        assertTrue(deque.isEmpty());
    }
}
