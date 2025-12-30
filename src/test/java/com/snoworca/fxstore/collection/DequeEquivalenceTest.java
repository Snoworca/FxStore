package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.*;
import com.snoworca.fxstore.core.FxStoreImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * FxDeque vs ArrayDeque Equivalence 테스트.
 *
 * FxDeque와 Java 표준 ArrayDeque가 동일한 연산에 대해 동일한 결과를 내는지 검증합니다.
 */
public class DequeEquivalenceTest {

    private FxStore store;
    private Deque<String> fxDeque;
    private ArrayDeque<String> refDeque;
    private Random random;

    @Before
    public void setUp() {
        store = FxStoreImpl.openMemory(FxOptions.defaults());
        fxDeque = store.createDeque("testDeque", String.class);
        refDeque = new ArrayDeque<>();
        random = new Random(42);
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== 기본 연산 ====================

    @Test
    public void testEquivalence_AddFirstLast() {
        for (int i = 0; i < 50; i++) {
            String value = "v" + i;
            if (i % 2 == 0) {
                fxDeque.addFirst(value);
                refDeque.addFirst(value);
            } else {
                fxDeque.addLast(value);
                refDeque.addLast(value);
            }
        }

        assertDequesEqual();
    }

    @Test
    public void testEquivalence_OfferFirstLast() {
        for (int i = 0; i < 50; i++) {
            String value = "v" + i;
            assertEquals(refDeque.offerFirst(value), fxDeque.offerFirst(value));
            assertEquals(refDeque.offerLast(value), fxDeque.offerLast(value));
        }

        assertDequesEqual();
    }

    @Test
    public void testEquivalence_RemoveFirstLast() {
        for (int i = 0; i < 30; i++) {
            fxDeque.addLast("v" + i);
            refDeque.addLast("v" + i);
        }

        for (int i = 0; i < 15; i++) {
            if (i % 2 == 0) {
                assertEquals(refDeque.removeFirst(), fxDeque.removeFirst());
            } else {
                assertEquals(refDeque.removeLast(), fxDeque.removeLast());
            }
        }

        assertDequesEqual();
    }

    @Test
    public void testEquivalence_PollFirstLast() {
        for (int i = 0; i < 30; i++) {
            fxDeque.addLast("v" + i);
            refDeque.addLast("v" + i);
        }

        for (int i = 0; i < 15; i++) {
            if (i % 2 == 0) {
                assertEquals(refDeque.pollFirst(), fxDeque.pollFirst());
            } else {
                assertEquals(refDeque.pollLast(), fxDeque.pollLast());
            }
        }

        assertDequesEqual();

        // 빈 deque에서 poll
        fxDeque.clear();
        refDeque.clear();
        assertEquals(refDeque.pollFirst(), fxDeque.pollFirst());
        assertEquals(refDeque.pollLast(), fxDeque.pollLast());
    }

    @Test
    public void testEquivalence_PeekFirstLast() {
        for (int i = 0; i < 30; i++) {
            fxDeque.addLast("v" + i);
            refDeque.addLast("v" + i);
        }

        assertEquals(refDeque.peekFirst(), fxDeque.peekFirst());
        assertEquals(refDeque.peekLast(), fxDeque.peekLast());
        assertEquals(refDeque.getFirst(), fxDeque.getFirst());
        assertEquals(refDeque.getLast(), fxDeque.getLast());

        // 빈 deque에서 peek
        fxDeque.clear();
        refDeque.clear();
        assertEquals(refDeque.peekFirst(), fxDeque.peekFirst());
        assertEquals(refDeque.peekLast(), fxDeque.peekLast());
    }

    // ==================== Stack/Queue 연산 ====================

    @Test
    public void testEquivalence_Push() {
        for (int i = 0; i < 30; i++) {
            String value = "v" + i;
            fxDeque.push(value);
            refDeque.push(value);
        }

        assertDequesEqual();
    }

    @Test
    public void testEquivalence_Pop() {
        for (int i = 0; i < 30; i++) {
            fxDeque.push("v" + i);
            refDeque.push("v" + i);
        }

        for (int i = 0; i < 15; i++) {
            assertEquals(refDeque.pop(), fxDeque.pop());
        }

        assertDequesEqual();
    }

    @Test
    public void testEquivalence_Add() {
        for (int i = 0; i < 30; i++) {
            String value = "v" + i;
            assertEquals(refDeque.add(value), fxDeque.add(value));
        }

        assertDequesEqual();
    }

    @Test
    public void testEquivalence_Offer() {
        for (int i = 0; i < 30; i++) {
            String value = "v" + i;
            assertEquals(refDeque.offer(value), fxDeque.offer(value));
        }

        assertDequesEqual();
    }

    @Test
    public void testEquivalence_Remove() {
        for (int i = 0; i < 30; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
        }

        for (int i = 0; i < 15; i++) {
            assertEquals(refDeque.remove(), fxDeque.remove());
        }

        assertDequesEqual();
    }

    @Test
    public void testEquivalence_Poll() {
        for (int i = 0; i < 30; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
        }

        for (int i = 0; i < 15; i++) {
            assertEquals(refDeque.poll(), fxDeque.poll());
        }

        assertDequesEqual();
    }

    @Test
    public void testEquivalence_Element() {
        for (int i = 0; i < 10; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
        }

        assertEquals(refDeque.element(), fxDeque.element());
    }

    @Test
    public void testEquivalence_Peek() {
        for (int i = 0; i < 10; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
        }

        assertEquals(refDeque.peek(), fxDeque.peek());

        // 빈 deque
        fxDeque.clear();
        refDeque.clear();
        assertEquals(refDeque.peek(), fxDeque.peek());
    }

    // ==================== 검색 연산 ====================

    @Test
    public void testEquivalence_Contains() {
        for (int i = 0; i < 30; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
        }

        for (int i = 0; i < 50; i++) {
            assertEquals(refDeque.contains("v" + i), fxDeque.contains("v" + i));
        }
    }

    @Test
    public void testEquivalence_Size() {
        assertEquals(refDeque.size(), fxDeque.size());

        for (int i = 0; i < 30; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
            assertEquals(refDeque.size(), fxDeque.size());
        }

        for (int i = 0; i < 15; i++) {
            fxDeque.removeFirst();
            refDeque.removeFirst();
            assertEquals(refDeque.size(), fxDeque.size());
        }
    }

    @Test
    public void testEquivalence_IsEmpty() {
        assertTrue(fxDeque.isEmpty());
        assertTrue(refDeque.isEmpty());

        fxDeque.add("test");
        refDeque.add("test");

        assertEquals(refDeque.isEmpty(), fxDeque.isEmpty());

        fxDeque.removeFirst();
        refDeque.removeFirst();

        assertEquals(refDeque.isEmpty(), fxDeque.isEmpty());
    }

    // ==================== 반복자 ====================

    @Test
    public void testEquivalence_Iterator() {
        for (int i = 0; i < 30; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
        }

        Iterator<String> fxIt = fxDeque.iterator();
        Iterator<String> refIt = refDeque.iterator();

        while (refIt.hasNext()) {
            assertTrue(fxIt.hasNext());
            assertEquals(refIt.next(), fxIt.next());
        }

        assertFalse(fxIt.hasNext());
    }

    @Test
    public void testEquivalence_DescendingIterator() {
        for (int i = 0; i < 30; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
        }

        Iterator<String> fxDescIt = fxDeque.descendingIterator();
        Iterator<String> refDescIt = refDeque.descendingIterator();

        while (refDescIt.hasNext()) {
            assertTrue(fxDescIt.hasNext());
            assertEquals(refDescIt.next(), fxDescIt.next());
        }

        assertFalse(fxDescIt.hasNext());
    }

    // ==================== 랜덤 연산 ====================

    @Test
    public void testEquivalence_RandomOperations() {
        for (int i = 0; i < 1000; i++) {
            int op = random.nextInt(100);
            String value = "v" + random.nextInt(500);

            if (op < 25) {
                // 25% addFirst
                fxDeque.addFirst(value);
                refDeque.addFirst(value);
            } else if (op < 50) {
                // 25% addLast
                fxDeque.addLast(value);
                refDeque.addLast(value);
            } else if (op < 65 && !fxDeque.isEmpty()) {
                // 15% removeFirst
                assertEquals(refDeque.removeFirst(), fxDeque.removeFirst());
            } else if (op < 80 && !fxDeque.isEmpty()) {
                // 15% removeLast
                assertEquals(refDeque.removeLast(), fxDeque.removeLast());
            } else if (op < 90 && !fxDeque.isEmpty()) {
                // 10% peekFirst
                assertEquals(refDeque.peekFirst(), fxDeque.peekFirst());
            } else {
                // 10% peekLast
                assertEquals(refDeque.peekLast(), fxDeque.peekLast());
            }

            if (i % 100 == 0) {
                assertEquals(refDeque.size(), fxDeque.size());
            }
        }

        assertDequesEqual();
    }

    // ==================== 엣지 케이스 ====================

    @Test
    public void testEquivalence_EmptyDeque() {
        assertTrue(fxDeque.isEmpty());
        assertEquals(0, fxDeque.size());
        assertNull(fxDeque.peekFirst());
        assertNull(fxDeque.peekLast());
        assertNull(fxDeque.pollFirst());
        assertNull(fxDeque.pollLast());
    }

    @Test
    public void testEquivalence_SingleElement() {
        fxDeque.add("only");
        refDeque.add("only");

        assertEquals(refDeque.size(), fxDeque.size());
        assertEquals(refDeque.peekFirst(), fxDeque.peekFirst());
        assertEquals(refDeque.peekLast(), fxDeque.peekLast());
    }

    @Test
    public void testEquivalence_ToArray() {
        for (int i = 0; i < 30; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
        }

        Object[] fxArray = fxDeque.toArray();
        Object[] refArray = refDeque.toArray();

        assertArrayEquals(refArray, fxArray);
    }

    @Test
    public void testEquivalence_Clear() {
        for (int i = 0; i < 30; i++) {
            fxDeque.add("v" + i);
            refDeque.add("v" + i);
        }

        fxDeque.clear();
        refDeque.clear();

        assertEquals(refDeque.size(), fxDeque.size());
        assertTrue(fxDeque.isEmpty());
    }

    @Test
    public void testEquivalence_AddAll() {
        List<String> toAdd = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(refDeque.addAll(toAdd), fxDeque.addAll(toAdd));
        assertDequesEqual();
    }

    // ==================== 헬퍼 메서드 ====================

    private void assertDequesEqual() {
        assertEquals("Size mismatch", refDeque.size(), fxDeque.size());

        Iterator<String> refIt = refDeque.iterator();
        Iterator<String> fxIt = fxDeque.iterator();

        int index = 0;
        while (refIt.hasNext()) {
            assertTrue("FxDeque should have element at index " + index, fxIt.hasNext());
            assertEquals("Element mismatch at index " + index, refIt.next(), fxIt.next());
            index++;
        }
        assertFalse("FxDeque should not have more elements", fxIt.hasNext());
    }
}
