package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * FxList 에지 케이스 테스트 - 브랜치 커버리지 개선용
 *
 * <p>목적:</p>
 * <ul>
 *   <li>remove(int) 범위 검사 브랜치</li>
 *   <li>clear() 빈 리스트 케이스</li>
 *   <li>get() 경계값 테스트</li>
 * </ul>
 */
public class FxListEdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("list-edge-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== remove() 범위 검사 테스트 ====================

    @Test(expected = IndexOutOfBoundsException.class)
    public void remove_negativeIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.remove(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void remove_indexEqualToSize_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.remove(1); // size is 1, valid indices are 0
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void remove_indexGreaterThanSize_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.remove(10);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void remove_emptyList_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.remove(0);
    }

    // ==================== get() 범위 검사 테스트 ====================

    @Test(expected = IndexOutOfBoundsException.class)
    public void get_negativeIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void get_indexEqualToSize_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.get(1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void get_emptyList_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.get(0);
    }

    // ==================== set() 범위 검사 테스트 ====================

    @Test(expected = IndexOutOfBoundsException.class)
    public void set_negativeIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.set(-1, "new");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void set_indexEqualToSize_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.set(1, "new");
    }

    // ==================== add(index, element) 범위 검사 테스트 ====================

    @Test(expected = IndexOutOfBoundsException.class)
    public void add_negativeIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add(-1, "item");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void add_indexGreaterThanSize_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("first");
        list.add(5, "item"); // size is 1, valid indices are 0 and 1
    }

    @Test
    public void add_indexEqualToSize_shouldAppend() {
        List<String> list = store.createList("test", String.class);
        list.add("first");
        list.add(1, "second"); // size is 1, adding at end
        assertEquals(2, list.size());
        assertEquals("second", list.get(1));
    }

    // ==================== clear() 테스트 ====================

    @Test
    public void clear_emptyList_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void clear_withElements_shouldRemoveAll() {
        List<String> list = store.createList("test", String.class);
        for (int i = 0; i < 10; i++) {
            list.add("item" + i);
        }
        assertEquals(10, list.size());

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    // ==================== isEmpty() 테스트 ====================

    @Test
    public void isEmpty_newList_shouldBeTrue() {
        List<String> list = store.createList("test", String.class);
        assertTrue(list.isEmpty());
    }

    @Test
    public void isEmpty_afterAdd_shouldBeFalse() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        assertFalse(list.isEmpty());
    }

    @Test
    public void isEmpty_afterClear_shouldBeTrue() {
        List<String> list = store.createList("test", String.class);
        list.add("item");
        list.clear();
        assertTrue(list.isEmpty());
    }

    // ==================== size() 테스트 ====================

    @Test
    public void size_emptyList_shouldBeZero() {
        List<String> list = store.createList("test", String.class);
        assertEquals(0, list.size());
    }

    @Test
    public void size_afterMultipleOps_shouldBeAccurate() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals(3, list.size());

        list.remove(1);
        assertEquals(2, list.size());

        list.add(1, "new");
        assertEquals(3, list.size());
    }

    // ==================== subList 테스트 ====================

    @Test
    public void subList_validRange_shouldWork() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");

        List<String> sub = list.subList(1, 3);
        assertEquals(2, sub.size());
        assertEquals("b", sub.get(0));
        assertEquals("c", sub.get(1));
    }

    @Test
    public void subList_emptyRange_shouldBeEmpty() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");

        List<String> sub = list.subList(1, 1);
        assertTrue(sub.isEmpty());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void subList_invalidFromIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.subList(-1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void subList_invalidToIndex_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.subList(0, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void subList_fromGreaterThanTo_shouldThrow() {
        List<String> list = store.createList("test", String.class);
        list.add("a");
        list.add("b");
        list.subList(2, 1);
    }
}
