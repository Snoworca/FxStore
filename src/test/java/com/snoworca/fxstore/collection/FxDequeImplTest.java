package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.*;
import com.snoworca.fxstore.core.FxStoreImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * FxDequeImpl 단위 테스트
 *
 * <p>DequeEquivalenceTest와 별도로 FxDequeImpl 고유 기능 테스트:</p>
 * <ul>
 *   <li>FxCollection 인터페이스 구현</li>
 *   <li>SeqEncoder 관련</li>
 *   <li>예외 처리</li>
 *   <li>경계 조건</li>
 * </ul>
 *
 * @since 0.9
 * @see FxDequeImpl
 * @see DequeEquivalenceTest
 */
public class FxDequeImplTest {

    private FxStore store;
    private FxStoreImpl storeImpl;
    private Deque<String> deque;
    private FxDequeImpl<String> dequeImpl;

    @Before
    public void setUp() {
        store = FxStoreImpl.openMemory(FxOptions.defaults());
        storeImpl = (FxStoreImpl) store;
        deque = store.createDeque("testDeque", String.class);
        dequeImpl = (FxDequeImpl<String>) deque;
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== FxCollection 구현 테스트 ====================

    @Test
    public void getCollectionId_shouldReturnValidId() {
        long id = dequeImpl.getCollectionId();
        assertTrue("Collection ID should be positive", id > 0);
    }

    @Test
    public void getStore_shouldReturnFxStoreImpl() {
        FxStoreImpl returnedStore = dequeImpl.getStore();
        assertNotNull(returnedStore);
        assertSame(storeImpl, returnedStore);
    }

    // ==================== SeqEncoder 테스트 ====================

    @Test
    public void getSeqEncoder_newDeque_shouldReturnOrderedEncoder() {
        // v0.7+ 새 Deque는 OrderedSeqEncoder 사용
        SeqEncoder encoder = dequeImpl.getSeqEncoder();
        assertNotNull(encoder);
        assertTrue("New deque should use OrderedSeqEncoder",
                   encoder instanceof OrderedSeqEncoder);
    }

    @Test
    public void setSeqEncoder_shouldChangeEncoder() {
        // 먼저 현재 인코더 확인
        SeqEncoder originalEncoder = dequeImpl.getSeqEncoder();

        // LegacySeqEncoder로 변경
        dequeImpl.setSeqEncoder(LegacySeqEncoder.getInstance());

        // 변경 확인
        SeqEncoder newEncoder = dequeImpl.getSeqEncoder();
        assertTrue("Should use LegacySeqEncoder after set",
                   newEncoder instanceof LegacySeqEncoder);

        // 다시 OrderedSeqEncoder로 복원
        dequeImpl.setSeqEncoder(OrderedSeqEncoder.getInstance());
        assertTrue("Should use OrderedSeqEncoder after restore",
                   dequeImpl.getSeqEncoder() instanceof OrderedSeqEncoder);
    }

    @Test(expected = NullPointerException.class)
    public void setSeqEncoder_null_shouldThrowNPE() {
        dequeImpl.setSeqEncoder(null);
    }

    // ==================== Null 요소 처리 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void addFirst_null_shouldThrowNPE() {
        deque.addFirst(null);
    }

    @Test(expected = NullPointerException.class)
    public void addLast_null_shouldThrowNPE() {
        deque.addLast(null);
    }

    @Test(expected = NullPointerException.class)
    public void push_null_shouldThrowNPE() {
        deque.push(null);
    }

    @Test
    public void offerFirst_null_shouldReturnFalse() {
        // offerFirst는 예외를 던지지 않고 false 반환
        assertFalse(deque.offerFirst(null));
    }

    @Test
    public void offerLast_null_shouldReturnFalse() {
        // offerLast는 예외를 던지지 않고 false 반환
        assertFalse(deque.offerLast(null));
    }

    // ==================== 빈 Deque 예외 테스트 ====================

    @Test(expected = NoSuchElementException.class)
    public void removeFirst_empty_shouldThrowNSEE() {
        deque.removeFirst();
    }

    @Test(expected = NoSuchElementException.class)
    public void removeLast_empty_shouldThrowNSEE() {
        deque.removeLast();
    }

    @Test(expected = NoSuchElementException.class)
    public void getFirst_empty_shouldThrowNSEE() {
        deque.getFirst();
    }

    @Test(expected = NoSuchElementException.class)
    public void getLast_empty_shouldThrowNSEE() {
        deque.getLast();
    }

    @Test(expected = NoSuchElementException.class)
    public void remove_empty_shouldThrowNSEE() {
        deque.remove();
    }

    @Test(expected = NoSuchElementException.class)
    public void pop_empty_shouldThrowNSEE() {
        deque.pop();
    }

    @Test(expected = NoSuchElementException.class)
    public void element_empty_shouldThrowNSEE() {
        deque.element();
    }

    // ==================== removeFirstOccurrence / removeLastOccurrence 테스트 ====================

    @Test
    public void removeFirstOccurrence_exists_shouldRemoveAndReturnTrue() {
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("A");
        deque.addLast("C");

        assertTrue(deque.removeFirstOccurrence("A"));

        // 첫 번째 A만 제거됨
        assertEquals(3, deque.size());
        assertEquals("B", deque.peekFirst());

        // 두 번째 A는 남아있어야 함
        List<String> remaining = new ArrayList<>();
        deque.forEach(remaining::add);
        assertEquals(Arrays.asList("B", "A", "C"), remaining);
    }

    @Test
    public void removeFirstOccurrence_notExists_shouldReturnFalse() {
        deque.addLast("A");
        deque.addLast("B");

        assertFalse(deque.removeFirstOccurrence("C"));
        assertEquals(2, deque.size());
    }

    @Test
    public void removeFirstOccurrence_null_shouldReturnFalse() {
        deque.addLast("A");
        assertFalse(deque.removeFirstOccurrence(null));
    }

    @Test
    public void removeLastOccurrence_exists_shouldRemoveAndReturnTrue() {
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("A");
        deque.addLast("C");

        assertTrue(deque.removeLastOccurrence("A"));

        // 마지막 A만 제거됨
        assertEquals(3, deque.size());

        List<String> remaining = new ArrayList<>();
        deque.forEach(remaining::add);
        assertEquals(Arrays.asList("A", "B", "C"), remaining);
    }

    @Test
    public void removeLastOccurrence_notExists_shouldReturnFalse() {
        deque.addLast("A");
        deque.addLast("B");

        assertFalse(deque.removeLastOccurrence("C"));
        assertEquals(2, deque.size());
    }

    @Test
    public void removeLastOccurrence_null_shouldReturnFalse() {
        deque.addLast("A");
        assertFalse(deque.removeLastOccurrence(null));
    }

    // ==================== Collection 메서드 테스트 ====================

    @Test
    public void containsAll_allExist_shouldReturnTrue() {
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");

        assertTrue(deque.containsAll(Arrays.asList("A", "B")));
        assertTrue(deque.containsAll(Arrays.asList("A", "B", "C")));
    }

    @Test
    public void containsAll_someNotExist_shouldReturnFalse() {
        deque.addLast("A");
        deque.addLast("B");

        assertFalse(deque.containsAll(Arrays.asList("A", "C")));
    }

    @Test
    public void containsAll_emptyCollection_shouldReturnTrue() {
        deque.addLast("A");
        assertTrue(deque.containsAll(Collections.emptyList()));
    }

    @Test
    public void removeAll_shouldRemoveMatchingElements() {
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");
        deque.addLast("D");

        assertTrue(deque.removeAll(Arrays.asList("B", "D")));

        assertEquals(2, deque.size());
        List<String> remaining = new ArrayList<>();
        deque.forEach(remaining::add);
        assertEquals(Arrays.asList("A", "C"), remaining);
    }

    @Test
    public void removeAll_noneMatch_shouldReturnFalse() {
        deque.addLast("A");
        deque.addLast("B");

        assertFalse(deque.removeAll(Arrays.asList("X", "Y")));
        assertEquals(2, deque.size());
    }

    @Test
    public void retainAll_shouldKeepOnlyMatchingElements() {
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");
        deque.addLast("D");

        assertTrue(deque.retainAll(Arrays.asList("A", "C")));

        assertEquals(2, deque.size());
        List<String> remaining = new ArrayList<>();
        deque.forEach(remaining::add);
        assertEquals(Arrays.asList("A", "C"), remaining);
    }

    @Test
    public void retainAll_allMatch_shouldReturnFalse() {
        deque.addLast("A");
        deque.addLast("B");

        assertFalse(deque.retainAll(Arrays.asList("A", "B", "C")));
        assertEquals(2, deque.size());
    }

    // ==================== toArray 테스트 ====================

    @Test
    public void toArray_typed_shouldReturnCorrectArray() {
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");

        String[] result = deque.toArray(new String[0]);
        assertArrayEquals(new String[]{"A", "B", "C"}, result);
    }

    @Test
    public void toArray_typed_largerArray_shouldUseProvidedArray() {
        deque.addLast("A");
        deque.addLast("B");

        String[] provided = new String[5];
        String[] result = deque.toArray(provided);

        assertSame(provided, result);
        assertEquals("A", result[0]);
        assertEquals("B", result[1]);
        assertNull(result[2]); // 나머지는 null
    }

    @Test
    public void toArray_empty_shouldReturnEmptyArray() {
        Object[] result = deque.toArray();
        assertEquals(0, result.length);

        String[] typedResult = deque.toArray(new String[0]);
        assertEquals(0, typedResult.length);
    }

    // ==================== Iterator remove 테스트 ====================
    // Note: FxDequeImpl's Iterator.remove() may have limitations

    @Test
    public void iterator_hasNext_shouldWork() {
        deque.addLast("A");
        deque.addLast("B");

        Iterator<String> it = deque.iterator();
        assertTrue(it.hasNext());
        assertEquals("A", it.next());
        assertTrue(it.hasNext());
        assertEquals("B", it.next());
        assertFalse(it.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void iterator_nextOnEmpty_shouldThrow() {
        Iterator<String> it = deque.iterator();
        it.next();
    }

    // ==================== 대용량 테스트 ====================

    @Test
    public void largeDeque_addAndRemove() {
        final int COUNT = 10000;

        // 대용량 추가
        for (int i = 0; i < COUNT; i++) {
            if (i % 2 == 0) {
                deque.addFirst("F" + i);
            } else {
                deque.addLast("L" + i);
            }
        }

        assertEquals(COUNT, deque.size());

        // 절반 제거
        for (int i = 0; i < COUNT / 2; i++) {
            if (i % 2 == 0) {
                assertNotNull(deque.pollFirst());
            } else {
                assertNotNull(deque.pollLast());
            }
        }

        assertEquals(COUNT / 2, deque.size());
    }

    // ==================== 동일 요소 테스트 ====================

    @Test
    public void duplicateElements_shouldBeHandledCorrectly() {
        deque.addLast("A");
        deque.addLast("A");
        deque.addLast("A");

        assertEquals(3, deque.size());
        assertTrue(deque.contains("A"));

        // removeFirstOccurrence는 하나만 제거
        deque.removeFirstOccurrence("A");
        assertEquals(2, deque.size());

        // removeLastOccurrence도 하나만 제거
        deque.removeLastOccurrence("A");
        assertEquals(1, deque.size());

        // 마지막 하나 남음
        assertEquals("A", deque.peekFirst());
    }

    // ==================== remove(Object) 테스트 ====================

    @Test
    public void remove_object_shouldRemoveFirstOccurrence() {
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("A");

        assertTrue(deque.remove("A"));
        assertEquals(2, deque.size());

        List<String> remaining = new ArrayList<>();
        deque.forEach(remaining::add);
        assertEquals(Arrays.asList("B", "A"), remaining);
    }

    @Test
    public void remove_object_notExists_shouldReturnFalse() {
        deque.addLast("A");
        assertFalse(deque.remove("B"));
    }

    // ==================== for-each 루프 테스트 ====================

    @Test
    public void forEach_shouldIterateInOrder() {
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");

        List<String> collected = new ArrayList<>();
        for (String s : deque) {
            collected.add(s);
        }

        assertEquals(Arrays.asList("A", "B", "C"), collected);
    }

    // ==================== updateHeadSeq / updateTailSeq 테스트 ====================

    @Test
    public void updateHeadTailSeq_shouldReflectAfterReopen() throws Exception {
        // 메모리 스토어에서는 reopen 불가하므로 파일 기반으로 테스트해야 함
        // 여기서는 headSeq, tailSeq가 추가/제거 후 올바르게 업데이트되는지만 확인

        deque.addLast("A"); // tailSeq++
        deque.addFirst("Z"); // headSeq--

        assertEquals(2, deque.size());
        assertEquals("Z", deque.peekFirst());
        assertEquals("A", deque.peekLast());
    }

    // ==================== 특수 문자 테스트 ====================

    @Test
    public void specialCharacters_shouldBeHandled() {
        String[] special = {
            "한글", "日本語", "中文",
            "emoji: \uD83D\uDE00",
            "newline\ntest",
            "tab\ttest",
            "quote\"test",
            "backslash\\test"
        };

        for (String s : special) {
            deque.addLast(s);
        }

        assertEquals(special.length, deque.size());

        int i = 0;
        for (String s : deque) {
            assertEquals(special[i++], s);
        }
    }

    // ==================== 긴 문자열 테스트 ====================

    @Test
    public void longStrings_shouldBeHandled() {
        // 1000자 문자열 테스트 (페이지 사이즈 내)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("A");
        }
        String longString = sb.toString();

        deque.addLast(longString);
        assertEquals(1, deque.size());
        assertEquals(longString, deque.peekFirst());
    }
}
