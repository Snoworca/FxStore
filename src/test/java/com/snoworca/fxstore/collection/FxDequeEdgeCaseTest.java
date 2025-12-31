package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Deque;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

/**
 * FxDeque 에지 케이스 테스트 - 브랜치 커버리지 개선용
 *
 * <p>목적:</p>
 * <ul>
 *   <li>poll/peek null 반환 브랜치</li>
 *   <li>빈 deque 에지 케이스</li>
 *   <li>getFirst/getLast 예외 케이스</li>
 * </ul>
 */
public class FxDequeEdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("deque-edge-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== poll null 반환 테스트 ====================

    @Test
    public void pollFirst_emptyDeque_shouldReturnNull() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertNull(deque.pollFirst());
    }

    @Test
    public void pollLast_emptyDeque_shouldReturnNull() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertNull(deque.pollLast());
    }

    @Test
    public void poll_emptyDeque_shouldReturnNull() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertNull(deque.poll());
    }

    // ==================== peek null 반환 테스트 ====================

    @Test
    public void peekFirst_emptyDeque_shouldReturnNull() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertNull(deque.peekFirst());
    }

    @Test
    public void peekLast_emptyDeque_shouldReturnNull() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertNull(deque.peekLast());
    }

    @Test
    public void peek_emptyDeque_shouldReturnNull() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertNull(deque.peek());
    }

    // ==================== getFirst/getLast 예외 테스트 ====================

    @Test(expected = NoSuchElementException.class)
    public void getFirst_emptyDeque_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.getFirst();
    }

    @Test(expected = NoSuchElementException.class)
    public void getLast_emptyDeque_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.getLast();
    }

    @Test(expected = NoSuchElementException.class)
    public void element_emptyDeque_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.element();
    }

    @Test(expected = NoSuchElementException.class)
    public void removeFirst_emptyDeque_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.removeFirst();
    }

    @Test(expected = NoSuchElementException.class)
    public void removeLast_emptyDeque_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.removeLast();
    }

    @Test(expected = NoSuchElementException.class)
    public void remove_emptyDeque_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.remove();
    }

    @Test(expected = NoSuchElementException.class)
    public void pop_emptyDeque_shouldThrow() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.pop();
    }

    // ==================== 빈 deque 상태 테스트 ====================

    @Test
    public void isEmpty_newDeque_shouldBeTrue() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void size_emptyDeque_shouldBeZero() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertEquals(0, deque.size());
    }

    // ==================== poll 후 상태 테스트 ====================

    @Test
    public void pollFirst_singleElement_shouldMakeEmpty() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addFirst("only");

        String result = deque.pollFirst();

        assertEquals("only", result);
        assertTrue(deque.isEmpty());
        assertNull(deque.pollFirst());
    }

    @Test
    public void pollLast_singleElement_shouldMakeEmpty() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("only");

        String result = deque.pollLast();

        assertEquals("only", result);
        assertTrue(deque.isEmpty());
        assertNull(deque.pollLast());
    }

    // ==================== 연속 poll 테스트 ====================

    @Test
    public void pollFirst_multiple_shouldDrainInOrder() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        assertEquals("a", deque.pollFirst());
        assertEquals("b", deque.pollFirst());
        assertEquals("c", deque.pollFirst());
        assertNull(deque.pollFirst());
    }

    @Test
    public void pollLast_multiple_shouldDrainInReverseOrder() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("c");

        assertEquals("c", deque.pollLast());
        assertEquals("b", deque.pollLast());
        assertEquals("a", deque.pollLast());
        assertNull(deque.pollLast());
    }

    // ==================== clear 테스트 ====================

    @Test
    public void clear_emptyDeque_shouldWork() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.clear();
        assertTrue(deque.isEmpty());
    }

    @Test
    public void clear_withElements_shouldRemoveAll() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addFirst("a");
        deque.addLast("b");
        deque.addFirst("c");

        deque.clear();

        assertTrue(deque.isEmpty());
        assertNull(deque.peekFirst());
        assertNull(deque.peekLast());
    }

    // ==================== contains 테스트 ====================

    @Test
    public void contains_emptyDeque_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertFalse(deque.contains("anything"));
    }

    @Test
    public void contains_existingElement_shouldReturnTrue() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("exists");
        assertTrue(deque.contains("exists"));
    }

    @Test
    public void contains_nonExistingElement_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("exists");
        assertFalse(deque.contains("nothere"));
    }

    // ==================== iterator 빈 컬렉션 테스트 ====================

    @Test
    public void iterator_emptyDeque_shouldHaveNoElements() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertFalse(deque.iterator().hasNext());
    }

    @Test
    public void descendingIterator_emptyDeque_shouldHaveNoElements() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertFalse(deque.descendingIterator().hasNext());
    }

    // ==================== removeFirstOccurrence/removeLastOccurrence 테스트 ====================

    @Test
    public void removeFirstOccurrence_emptyDeque_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertFalse(deque.removeFirstOccurrence("anything"));
    }

    @Test
    public void removeLastOccurrence_emptyDeque_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        assertFalse(deque.removeLastOccurrence("anything"));
    }

    @Test
    public void removeFirstOccurrence_notFound_shouldReturnFalse() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("exists");
        assertFalse(deque.removeFirstOccurrence("nothere"));
    }

    @Test
    public void removeFirstOccurrence_found_shouldReturnTrue() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("a");

        assertTrue(deque.removeFirstOccurrence("a"));
        assertEquals(2, deque.size());
        assertEquals("b", deque.peekFirst());
    }
}
