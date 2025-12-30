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
 * FxList 미커버 브랜치 테스트
 *
 * 커버리지 개선 대상:
 * - subList 뷰 작업
 * - 범위 검사 브랜치
 * - ListIterator 역방향 순회
 * - 빈 리스트 엣지 케이스
 */
public class FxListCoverageTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("list-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== 기본 작업 테스트 ====================

    @Test
    public void list_isEmpty_shouldWork() {
        List<String> list = store.createList("test", String.class);
        assertTrue(list.isEmpty());

        list.add("item");
        assertFalse(list.isEmpty());
    }

    @Test
    public void list_clear_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        assertEquals(2, list.size());

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void list_contains_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("apple");
        list.add("banana");

        assertTrue(list.contains("apple"));
        assertTrue(list.contains("banana"));
        assertFalse(list.contains("cherry"));
    }

    @Test
    public void list_containsAll_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        assertTrue(list.containsAll(Arrays.asList("a", "b")));
        assertFalse(list.containsAll(Arrays.asList("a", "x")));
    }

    @Test
    public void list_toArray_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");

        Object[] arr = list.toArray();
        assertEquals(2, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);

        String[] typedArr = list.toArray(new String[0]);
        assertEquals(2, typedArr.length);
        assertEquals("a", typedArr[0]);
    }

    // ==================== indexOf / lastIndexOf 테스트 ====================

    @Test
    public void list_indexOf_notFound_shouldReturnMinusOne() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");

        assertEquals(-1, list.indexOf("z"));
    }

    @Test
    public void list_lastIndexOf_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("a"); // duplicate

        assertEquals(2, list.lastIndexOf("a")); // last occurrence
        assertEquals(1, list.lastIndexOf("b"));
        assertEquals(-1, list.lastIndexOf("z"));
    }

    @Test
    public void list_lastIndexOf_empty_shouldReturnMinusOne() {
        List<String> list = store.createList("empty", String.class);
        assertEquals(-1, list.lastIndexOf("any"));
    }

    // ==================== add/set/remove 작업 테스트 ====================

    @Test
    public void list_addAtIndex_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("c");

        list.add(1, "b"); // insert at index 1

        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    public void list_addAtIndex_atBeginning() {
        List<String> list = store.createList("test", String.class);
        list.add("b");
        list.add(0, "a"); // insert at beginning

        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
    }

    @Test
    public void list_addAtIndex_atEnd() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add(1, "b"); // insert at end

        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
    }

    @Test
    public void list_set_shouldReturnOldValue() {
        List<String> list = store.createList("test", String.class);
        list.add("old");

        String oldValue = list.set(0, "new");

        assertEquals("old", oldValue);
        assertEquals("new", list.get(0));
    }

    @Test
    public void list_removeAtIndex_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        String removed = list.remove(1); // remove "b"

        assertEquals("b", removed);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals("c", list.get(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void list_removeObject_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        // FxList doesn't support remove(Object) - throws UnsupportedOperationException
        list.remove("b");
    }

    // Note: remove(Object) throws UnsupportedOperationException

    @Test
    public void list_addAll_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");

        list.addAll(Arrays.asList("b", "c", "d"));

        assertEquals(4, list.size());
        assertEquals("d", list.get(3));
    }

    @Test
    public void list_addAll_atIndex_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("d");

        list.addAll(1, Arrays.asList("b", "c")); // insert at index 1

        assertEquals(4, list.size());
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
        assertEquals("d", list.get(3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void list_removeAll_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");

        // FxList doesn't support removeAll - throws UnsupportedOperationException
        list.removeAll(Arrays.asList("b", "d"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void list_retainAll_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");

        // FxList doesn't support retainAll - throws UnsupportedOperationException
        list.retainAll(Arrays.asList("a", "c"));
    }

    // ==================== ListIterator 테스트 ====================

    @Test
    public void list_listIterator_forward_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        ListIterator<String> it = list.listIterator();

        assertTrue(it.hasNext());
        assertEquals(0, it.nextIndex());
        assertEquals("a", it.next());

        assertEquals(1, it.nextIndex());
        assertEquals("b", it.next());
    }

    @Test
    public void list_listIterator_backward_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        ListIterator<String> it = list.listIterator(3); // start at end

        assertTrue(it.hasPrevious());
        assertEquals(2, it.previousIndex());
        assertEquals("c", it.previous());

        assertEquals(1, it.previousIndex());
        assertEquals("b", it.previous());
    }

    @Test
    public void list_listIterator_fromIndex_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        ListIterator<String> it = list.listIterator(1); // start at index 1

        assertEquals("b", it.next());
        assertTrue(it.hasPrevious());
        assertEquals("b", it.previous());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void list_iterator_remove_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        Iterator<String> it = list.iterator();
        it.next();
        // FxList iterator doesn't support remove - throws UnsupportedOperationException
        it.remove();
    }

    // ==================== subList 테스트 ====================

    @Test
    public void list_subList_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");

        List<String> sub = list.subList(1, 4); // b, c, d

        assertEquals(3, sub.size());
        assertEquals("b", sub.get(0));
        assertEquals("c", sub.get(1));
        assertEquals("d", sub.get(2));
    }

    @Test
    public void list_subList_modification_shouldReflect() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");

        List<String> sub = list.subList(0, 2);
        sub.set(0, "A");

        assertEquals("A", list.get(0)); // change reflected in original
    }

    // ==================== 범위 검사 테스트 ====================

    @Test(expected = IndexOutOfBoundsException.class)
    public void list_get_negativeIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void list_get_indexOutOfBounds_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.get(1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void list_set_indexOutOfBounds_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.set(5, "x");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void list_add_invalidIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add(5, "x");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void list_remove_invalidIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.remove(5);
    }

    // ==================== equals / hashCode 테스트 ====================

    @Test
    public void list_equals_shouldWork() {
        List<String> list1 = store.createList("test1", String.class);
        list1.add("a");
        list1.add("b");

        List<String> list2 = store.createList("test2", String.class);
        list2.add("a");
        list2.add("b");

        assertEquals(list1, list2);
    }

    @Test
    public void list_equals_differentContent() {
        List<String> list1 = store.createList("test1", String.class);
        list1.add("a");

        List<String> list2 = store.createList("test2", String.class);
        list2.add("b");

        assertNotEquals(list1, list2);
    }

    // ==================== 빈 리스트 테스트 ====================

    @Test
    public void list_empty_operations() {
        List<String> list = store.createList("empty", String.class);

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.contains("anything"));
        assertEquals(-1, list.indexOf("anything"));
        assertEquals(-1, list.lastIndexOf("anything"));

        // iterator on empty list
        Iterator<String> it = list.iterator();
        assertFalse(it.hasNext());

        ListIterator<String> lit = list.listIterator();
        assertFalse(lit.hasNext());
        assertFalse(lit.hasPrevious());
    }

    // ==================== null 요소 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void list_add_null_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add(null);
    }

    @Test(expected = NullPointerException.class)
    public void list_addAtIndex_null_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add(0, null);
    }

    @Test(expected = NullPointerException.class)
    public void list_set_null_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.set(0, null);
    }

    // ==================== Iterator NoSuchElement 테스트 ====================

    @Test(expected = NoSuchElementException.class)
    public void list_iterator_next_empty_shouldThrow() {
        List<String> list = store.createList("empty", String.class);
        Iterator<String> it = list.iterator();
        it.next(); // empty list - throws
    }

    @Test(expected = NoSuchElementException.class)
    public void list_listIterator_previous_atStart_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        ListIterator<String> it = list.listIterator(0);
        it.previous(); // at start - throws
    }

    @Test(expected = NoSuchElementException.class)
    public void list_listIterator_next_atEnd_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        ListIterator<String> it = list.listIterator(1); // at end
        it.next(); // throws
    }

    // ==================== ListIterator UnsupportedOperation 테스트 ====================

    @Test(expected = UnsupportedOperationException.class)
    public void list_listIterator_set_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        ListIterator<String> it = list.listIterator();
        it.next();
        it.set("new"); // snapshot iterator - throws
    }

    @Test(expected = UnsupportedOperationException.class)
    public void list_listIterator_add_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        ListIterator<String> it = list.listIterator();
        it.add("new"); // snapshot iterator - throws
    }

    // ==================== clear 빈 리스트 테스트 ====================

    @Test
    public void list_clear_empty_shouldWork() {
        List<String> list = store.createList("empty", String.class);
        assertTrue(list.isEmpty());
        list.clear(); // no-op
        assertTrue(list.isEmpty());
    }

    // ==================== listIterator 잘못된 인덱스 테스트 ====================

    @Test(expected = IndexOutOfBoundsException.class)
    public void list_listIterator_negativeIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.listIterator(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void list_listIterator_tooLargeIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.listIterator(5); // size is 1
    }

    // ==================== hashCode 테스트 ====================

    @Test
    public void list_hashCode_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");

        int hashCode = list.hashCode();
        // AbstractList hashCode implementation
        assertNotEquals(0, hashCode);
    }

    // ==================== subList clear 테스트 ====================

    @Test(expected = UnsupportedOperationException.class)
    public void list_subList_clear_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");

        List<String> sub = list.subList(1, 4); // b, c, d
        sub.clear(); // FxList subList doesn't support clear
    }

    // ==================== 대용량 리스트 테스트 ====================

    @Test
    public void list_largeData_shouldWork() {
        List<String> list = store.createList("large", String.class);

        // 많은 데이터 추가
        for (int i = 0; i < 500; i++) {
            list.add("item-" + i);
        }

        assertEquals(500, list.size());
        assertEquals("item-0", list.get(0));
        assertEquals("item-499", list.get(499));

        // 중간 삭제
        list.remove(250);
        assertEquals(499, list.size());
    }

    // ==================== set 반환값 테스트 ====================

    @Test
    public void list_set_shouldReturnOldValueCorrectly() {
        List<String> list = store.createList("test", String.class);
        list.add("first");
        list.add("second");
        list.add("third");

        // 중간 요소 변경
        String old = list.set(1, "MODIFIED");
        assertEquals("second", old);
        assertEquals("MODIFIED", list.get(1));
    }

    // ==================== addAll 빈 컬렉션 테스트 ====================

    @Test
    public void list_addAll_empty_shouldReturnFalse() {
        List<String> list = store.createList("test", String.class);
        list.add("item");

        boolean changed = list.addAll(Collections.emptyList());
        assertFalse(changed);
        assertEquals(1, list.size());
    }

    @Test
    public void list_addAllAtIndex_empty_shouldReturnFalse() {
        List<String> list = store.createList("test", String.class);
        list.add("item");

        boolean changed = list.addAll(0, Collections.emptyList());
        assertFalse(changed);
        assertEquals(1, list.size());
    }
}
