package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;

import static org.junit.Assert.*;

/**
 * FxStoreImpl 내부 메서드 및 엣지 케이스 테스트
 *
 * <p>P0-6, P0-7, P0-8 해결:</p>
 * <ul>
 *   <li>getCollectionState(long) - ID로 상태 조회</li>
 *   <li>markCollectionChanged() - 컬렉션 루트 페이지 변경</li>
 *   <li>openList 예외 경로 - NOT_FOUND, TYPE_MISMATCH</li>
 * </ul>
 *
 * @since v1.0 Phase 3
 */
public class FxStoreInternalMethodsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;
    private File storeFile;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("internal-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== openList NOT_FOUND 테스트 ====================

    @Test(expected = FxException.class)
    public void openList_notFound_shouldThrow() {
        // Given: 빈 스토어
        // When: 존재하지 않는 List 열기 시도
        store.openList("nonexistent", String.class);
        // Then: FxException.NOT_FOUND 발생
    }

    @Test
    public void openList_notFound_checkErrorMessage() {
        // Given: 빈 스토어
        try {
            // When: 존재하지 않는 List 열기 시도
            store.openList("nonexistent", String.class);
            fail("Expected FxException");
        } catch (FxException e) {
            // Then: NOT_FOUND 관련 에러 메시지 확인
            assertTrue("Error should mention NOT_FOUND or not found",
                e.getMessage().contains("not found") || e.getMessage().contains("NOT_FOUND"));
        }
    }

    // ==================== openList TYPE_MISMATCH 테스트 ====================
    // 참고: 캐시된 컬렉션이 있으면 ClassCastException이 발생하므로,
    // 스토어를 닫고 다시 열어 캐시를 비워야 TYPE_MISMATCH 검증이 실행됨

    @Test(expected = FxException.class)
    public void openList_wrongType_Map_shouldThrow() throws Exception {
        // Given: Map으로 생성된 컬렉션
        store.createMap("mapCollection", Long.class, String.class);
        store.close();
        store = FxStore.open(storeFile.toPath());

        // When: 같은 이름으로 List 열기 시도
        store.openList("mapCollection", String.class);
        // Then: FxException.TYPE_MISMATCH 발생
    }

    @Test(expected = FxException.class)
    public void openList_wrongType_Set_shouldThrow() throws Exception {
        // Given: Set으로 생성된 컬렉션
        store.createSet("setCollection", Long.class);
        store.close();
        store = FxStore.open(storeFile.toPath());

        // When: 같은 이름으로 List 열기 시도
        store.openList("setCollection", Long.class);
        // Then: FxException.TYPE_MISMATCH 발생
    }

    @Test(expected = FxException.class)
    public void openList_wrongType_Deque_shouldThrow() throws Exception {
        // Given: Deque으로 생성된 컬렉션
        store.createDeque("dequeCollection", String.class);
        store.close();
        store = FxStore.open(storeFile.toPath());

        // When: 같은 이름으로 List 열기 시도
        store.openList("dequeCollection", String.class);
        // Then: FxException.TYPE_MISMATCH 발생
    }

    @Test
    public void openList_wrongType_checkErrorMessage() throws Exception {
        // Given: Map으로 생성된 컬렉션
        store.createMap("mapCollection2", Long.class, String.class);
        store.close();
        store = FxStore.open(storeFile.toPath());

        try {
            // When: 같은 이름으로 List 열기 시도
            store.openList("mapCollection2", String.class);
            fail("Expected FxException");
        } catch (FxException e) {
            // Then: TYPE_MISMATCH 관련 에러 메시지 확인
            assertTrue("Error should mention type mismatch or LIST",
                e.getMessage().contains("mismatch") || e.getMessage().contains("LIST"));
        }
    }

    // ==================== openMap TYPE_MISMATCH 테스트 ====================

    @Test(expected = FxException.class)
    public void openMap_wrongType_List_shouldThrow() throws Exception {
        // Given: List로 생성된 컬렉션
        store.createList("listCollection", String.class);
        store.close();
        store = FxStore.open(storeFile.toPath());

        // When: 같은 이름으로 Map 열기 시도
        store.openMap("listCollection", Long.class, String.class);
        // Then: FxException.TYPE_MISMATCH 발생
    }

    @Test(expected = FxException.class)
    public void openMap_notFound_shouldThrow() {
        // Given: 빈 스토어
        // When: 존재하지 않는 Map 열기 시도
        store.openMap("nonexistent", Long.class, String.class);
        // Then: FxException.NOT_FOUND 발생
    }

    // ==================== openSet TYPE_MISMATCH/NOT_FOUND 테스트 ====================

    @Test(expected = FxException.class)
    public void openSet_notFound_shouldThrow() {
        // Given: 빈 스토어
        // When: 존재하지 않는 Set 열기 시도
        store.openSet("nonexistent", Long.class);
        // Then: FxException.NOT_FOUND 발생
    }

    @Test(expected = FxException.class)
    public void openSet_wrongType_Map_shouldThrow() throws Exception {
        // Given: Map으로 생성된 컬렉션
        store.createMap("mapCollection3", Long.class, String.class);
        store.close();
        store = FxStore.open(storeFile.toPath());

        // When: 같은 이름으로 Set 열기 시도
        store.openSet("mapCollection3", Long.class);
        // Then: FxException.TYPE_MISMATCH 발생
    }

    // ==================== openDeque TYPE_MISMATCH/NOT_FOUND 테스트 ====================

    @Test(expected = FxException.class)
    public void openDeque_notFound_shouldThrow() {
        // Given: 빈 스토어
        // When: 존재하지 않는 Deque 열기 시도
        store.openDeque("nonexistent", String.class);
        // Then: FxException.NOT_FOUND 발생
    }

    @Test(expected = FxException.class)
    public void openDeque_wrongType_List_shouldThrow() throws Exception {
        // Given: List로 생성된 컬렉션
        store.createList("listCollection2", String.class);
        store.close();
        store = FxStore.open(storeFile.toPath());

        // When: 같은 이름으로 Deque 열기 시도
        store.openDeque("listCollection2", String.class);
        // Then: FxException.TYPE_MISMATCH 발생
    }

    // ==================== 컬렉션 상태 변경 테스트 ====================

    @Test
    public void collectionState_afterModification_shouldChange() {
        // Given: List 생성
        List<String> list = store.createList("testList", String.class);

        // When: 데이터 추가 (내부적으로 markCollectionChanged 호출)
        list.add("item1");
        list.add("item2");
        list.add("item3");

        // Then: 리스트 크기 확인
        assertEquals(3, list.size());

        // reopen 후에도 데이터 유지
        store.close();
        store = FxStore.open(storeFile.toPath());
        List<String> reopened = store.openList("testList", String.class);
        assertEquals(3, reopened.size());
    }

    @Test
    public void collectionState_afterBulkModification_shouldReflectChanges() {
        // Given: Map 생성
        NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);

        // When: 많은 데이터 추가 (다중 markCollectionChanged 호출)
        for (long i = 0; i < 100; i++) {
            map.put(i, "value-" + i);
        }

        // Then: 맵 크기 확인
        assertEquals(100, map.size());

        // reopen 후에도 데이터 유지
        store.close();
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> reopened = store.openMap("testMap", Long.class, String.class);
        assertEquals(100, reopened.size());
        assertEquals("value-50", reopened.get(50L));
    }

    // ==================== 다중 컬렉션 동시 조작 ====================

    @Test
    public void multipleCollections_independentState() {
        // Given: 여러 컬렉션 생성
        NavigableMap<Long, String> map = store.createMap("map1", Long.class, String.class);
        NavigableSet<String> set = store.createSet("set1", String.class);
        List<Integer> list = store.createList("list1", Integer.class);

        // When: 각각 데이터 추가
        map.put(1L, "one");
        map.put(2L, "two");

        set.add("a");
        set.add("b");
        set.add("c");

        list.add(100);
        list.add(200);

        // Then: 각 컬렉션 독립적으로 상태 유지
        assertEquals(2, map.size());
        assertEquals(3, set.size());
        assertEquals(2, list.size());

        // reopen 후 검증
        store.close();
        store = FxStore.open(storeFile.toPath());

        NavigableMap<Long, String> reopenedMap = store.openMap("map1", Long.class, String.class);
        NavigableSet<String> reopenedSet = store.openSet("set1", String.class);
        List<Integer> reopenedList = store.openList("list1", Integer.class);

        assertEquals(2, reopenedMap.size());
        assertEquals(3, reopenedSet.size());
        assertEquals(2, reopenedList.size());
    }

    // ==================== 컬렉션 삭제 후 재생성 ====================

    @Test
    public void dropAndRecreate_shouldWorkCorrectly() {
        // Given: 컬렉션 생성 및 데이터 추가
        NavigableMap<Long, String> map = store.createMap("recreated", Long.class, String.class);
        map.put(1L, "original");

        // When: 삭제 후 재생성
        boolean dropped = store.drop("recreated");
        assertTrue("Drop should succeed", dropped);
        assertFalse("Collection should not exist after drop", store.exists("recreated"));

        // 재생성
        NavigableMap<Long, String> newMap = store.createMap("recreated", Long.class, String.class);
        newMap.put(2L, "new");

        // Then: 새 데이터만 있어야 함
        assertEquals(1, newMap.size());
        assertNull(newMap.get(1L));
        assertEquals("new", newMap.get(2L));
    }

    // ==================== exists() 동작 확인 ====================

    @Test
    public void exists_variousScenarios() {
        // Given: 빈 스토어
        assertFalse("nonexistent should not exist", store.exists("nonexistent"));

        // When: Map 생성
        store.createMap("myMap", Long.class, String.class);
        assertTrue("myMap should exist", store.exists("myMap"));

        // When: Set 생성
        store.createSet("mySet", Long.class);
        assertTrue("mySet should exist", store.exists("mySet"));

        // When: List 생성
        store.createList("myList", String.class);
        assertTrue("myList should exist", store.exists("myList"));

        // When: Deque 생성
        store.createDeque("myDeque", String.class);
        assertTrue("myDeque should exist", store.exists("myDeque"));

        // When: Map 삭제
        store.drop("myMap");
        assertFalse("myMap should not exist after drop", store.exists("myMap"));
    }

    // ==================== 빈 컬렉션 상태 ====================

    @Test
    public void emptyCollection_stateIsValid() {
        // Given: 빈 컬렉션들 생성
        NavigableMap<Long, String> map = store.createMap("emptyMap", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("emptySet", Long.class);
        List<String> list = store.createList("emptyList", String.class);

        // Then: 빈 상태 확인
        assertTrue(map.isEmpty());
        assertTrue(set.isEmpty());
        assertTrue(list.isEmpty());

        // reopen 후에도 빈 상태 유지
        store.close();
        store = FxStore.open(storeFile.toPath());

        NavigableMap<Long, String> reopenedMap = store.openMap("emptyMap", Long.class, String.class);
        NavigableSet<Long> reopenedSet = store.openSet("emptySet", Long.class);
        List<String> reopenedList = store.openList("emptyList", String.class);

        assertTrue(reopenedMap.isEmpty());
        assertTrue(reopenedSet.isEmpty());
        assertTrue(reopenedList.isEmpty());
    }
}
