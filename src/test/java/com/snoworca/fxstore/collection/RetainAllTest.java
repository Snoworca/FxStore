package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxOptions;
import com.snoworca.fxstore.api.FxStore;
import com.snoworca.fxstore.core.FxStoreImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * BUG-V11-001 수정 테스트: retainAll() UnsupportedOperationException 수정 검증
 *
 * <p>FxNavigableSetImpl 및 7개 View 클래스의 retainAll() 정상 동작 검증
 */
public class RetainAllTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;

    @Before
    public void setUp() {
        store = FxStoreImpl.openMemory(FxOptions.defaults());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== TC-V11-001-01: retainAll() 기본 동작 ====================

    @Test
    public void testRetainAllBasic() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        boolean modified = set.retainAll(Arrays.asList(2, 4));

        assertTrue(modified);
        assertEquals(2, set.size());
        assertTrue(set.contains(2));
        assertTrue(set.contains(4));
        assertFalse(set.contains(1));
        assertFalse(set.contains(3));
        assertFalse(set.contains(5));
    }

    // ==================== TC-V11-001-02: retainAll() 빈 컬렉션 ====================

    @Test
    public void testRetainAllEmptyCollection() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3));

        boolean modified = set.retainAll(Collections.emptyList());

        assertTrue(modified);
        assertTrue(set.isEmpty());
    }

    // ==================== TC-V11-001-03: retainAll() 모두 유지 ====================

    @Test
    public void testRetainAllKeepAll() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3));

        boolean modified = set.retainAll(Arrays.asList(1, 2, 3, 4, 5));

        assertFalse(modified);
        assertEquals(3, set.size());
    }

    // ==================== TC-V11-001-04: retainAll() null 인자 예외 ====================

    @Test(expected = NullPointerException.class)
    public void testRetainAllNullCollection() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.retainAll(null);
    }

    // ==================== TC-V11-001-05: TreeSet 동등성 검증 ====================

    @Test
    public void testRetainAllEquivalenceWithTreeSet() {
        // FxStore Set
        NavigableSet<String> fxSet = store.createSet("test", String.class);
        fxSet.addAll(Arrays.asList("a", "b", "c", "d", "e"));

        // TreeSet (참조 구현)
        TreeSet<String> treeSet = new TreeSet<>(Arrays.asList("a", "b", "c", "d", "e"));

        Collection<String> retain = Arrays.asList("b", "d", "f");

        boolean fxModified = fxSet.retainAll(retain);
        boolean treeModified = treeSet.retainAll(retain);

        assertEquals(treeModified, fxModified);
        assertEquals(treeSet.size(), fxSet.size());
        assertTrue(fxSet.containsAll(treeSet));
        assertTrue(treeSet.containsAll(fxSet));
    }

    // ==================== TC-V11-001-06: SubSetView.retainAll() ====================

    @Test
    public void testSubSetViewRetainAll() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        // SubSet: [3, 8)
        NavigableSet<Integer> subSet = set.subSet(3, true, 8, false);
        assertEquals(5, subSet.size()); // 3, 4, 5, 6, 7

        boolean modified = subSet.retainAll(Arrays.asList(4, 6, 10));

        assertTrue(modified);
        assertEquals(2, subSet.size()); // 4, 6
        assertTrue(subSet.contains(4));
        assertTrue(subSet.contains(6));

        // 원본 Set에도 반영 확인
        assertEquals(7, set.size()); // 1, 2, 4, 6, 8, 9, 10
        assertFalse(set.contains(3));
        assertFalse(set.contains(5));
        assertFalse(set.contains(7));
    }

    // ==================== TC-V11-001-07: HeadSetView.retainAll() ====================

    @Test
    public void testHeadSetViewRetainAll() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        NavigableSet<Integer> headSet = set.headSet(4, false); // 1, 2, 3

        boolean modified = headSet.retainAll(Arrays.asList(2, 5));

        assertTrue(modified);
        assertEquals(1, headSet.size());
        assertTrue(headSet.contains(2));

        // 원본 Set: 2, 4, 5
        assertEquals(3, set.size());
    }

    // ==================== TC-V11-001-08: DescendingSet.retainAll() ====================

    @Test
    public void testDescendingSetRetainAll() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        NavigableSet<Integer> descSet = set.descendingSet();

        boolean modified = descSet.retainAll(Arrays.asList(2, 4));

        assertTrue(modified);
        assertEquals(2, descSet.size());

        // 원본에도 반영
        assertEquals(2, set.size());
        assertTrue(set.contains(2));
        assertTrue(set.contains(4));
    }

    // ==================== 추가 테스트: TailSetView.retainAll() ====================

    @Test
    public void testTailSetViewRetainAll() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        NavigableSet<Integer> tailSet = set.tailSet(3, true); // 3, 4, 5

        boolean modified = tailSet.retainAll(Arrays.asList(1, 4));

        assertTrue(modified);
        assertEquals(1, tailSet.size());
        assertTrue(tailSet.contains(4));

        // 원본 Set: 1, 2, 4
        assertEquals(3, set.size());
    }

    // ==================== 추가 테스트: DescendingSubSetView.retainAll() ====================

    @Test
    public void testDescendingSubSetViewRetainAll() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        // SubSet [3, 8) -> DescendingSet
        NavigableSet<Integer> subSet = set.subSet(3, true, 8, false);
        NavigableSet<Integer> descSubSet = subSet.descendingSet();

        boolean modified = descSubSet.retainAll(Arrays.asList(5, 6));

        assertTrue(modified);
        assertEquals(2, descSubSet.size());

        // 원본에도 반영
        assertFalse(set.contains(3));
        assertFalse(set.contains(4));
        assertFalse(set.contains(7));
        assertTrue(set.contains(5));
        assertTrue(set.contains(6));
    }

    // ==================== 추가 테스트: DescendingHeadSetView.retainAll() ====================

    @Test
    public void testDescendingHeadSetViewRetainAll() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        NavigableSet<Integer> headSet = set.headSet(4, false); // 1, 2, 3
        NavigableSet<Integer> descHeadSet = headSet.descendingSet();

        boolean modified = descHeadSet.retainAll(Arrays.asList(2));

        assertTrue(modified);
        assertEquals(1, descHeadSet.size());
        assertTrue(descHeadSet.contains(2));
    }

    // ==================== 추가 테스트: DescendingTailSetView.retainAll() ====================

    @Test
    public void testDescendingTailSetViewRetainAll() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        NavigableSet<Integer> tailSet = set.tailSet(3, true); // 3, 4, 5
        NavigableSet<Integer> descTailSet = tailSet.descendingSet();

        boolean modified = descTailSet.retainAll(Arrays.asList(3, 5));

        assertTrue(modified);
        assertEquals(2, descTailSet.size());
        assertTrue(descTailSet.contains(3));
        assertTrue(descTailSet.contains(5));
    }

    // ==================== 파일 기반 테스트 ====================

    @Test
    public void testRetainAllWithFileStore() throws Exception {
        File tempFile = tempFolder.newFile("retainall.fx");
        tempFile.delete();

        FxStore fileStore = FxStore.open(tempFile.toPath());
        try {
            NavigableSet<Integer> set = fileStore.createSet("test", Integer.class);
            set.addAll(Arrays.asList(1, 2, 3, 4, 5));

            boolean modified = set.retainAll(Arrays.asList(2, 4));

            assertTrue(modified);
            assertEquals(2, set.size());
            assertTrue(set.contains(2));
            assertTrue(set.contains(4));
        } finally {
            fileStore.close();
        }
    }

    // ==================== 대량 데이터 테스트 ====================

    @Test
    public void testRetainAllLargeDataset() {
        NavigableSet<Integer> set = store.createSet("test", Integer.class);
        for (int i = 0; i < 1000; i++) {
            set.add(i);
        }

        // 짝수만 유지
        List<Integer> evens = new ArrayList<>();
        for (int i = 0; i < 1000; i += 2) {
            evens.add(i);
        }

        boolean modified = set.retainAll(evens);

        assertTrue(modified);
        assertEquals(500, set.size());
        for (int i = 0; i < 1000; i++) {
            if (i % 2 == 0) {
                assertTrue("Even " + i + " should be present", set.contains(i));
            } else {
                assertFalse("Odd " + i + " should be absent", set.contains(i));
            }
        }
    }
}
