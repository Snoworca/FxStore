package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.CollectionKind;
import com.snoworca.fxstore.catalog.CatalogEntry;
import com.snoworca.fxstore.catalog.CollectionState;
import com.snoworca.fxstore.api.CodecRef;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * StoreSnapshot 단위 테스트
 *
 * <p>Phase 8 - 동시성 지원</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>불변성 검증 (INV-C2)</li>
 *   <li>방어적 복사 검증</li>
 *   <li>with* 메서드 동작 검증</li>
 *   <li>seqNo 증가 검증</li>
 * </ul>
 */
public class StoreSnapshotTest {

    private StoreSnapshot snapshot;
    private Map<String, CatalogEntry> catalog;
    private Map<Long, CollectionState> states;
    private Map<Long, Long> rootPageIds;

    @Before
    public void setUp() {
        catalog = new HashMap<>();
        catalog.put("testMap", new CatalogEntry("testMap", 1L));
        catalog.put("testSet", new CatalogEntry("testSet", 2L));

        states = new HashMap<>();
        states.put(1L, new CollectionState(1L, CollectionKind.MAP,
                new CodecRef("STRING", 1, null),
                new CodecRef("I64", 1, null),
                100L, 10L));
        states.put(2L, new CollectionState(2L, CollectionKind.SET,
                null,
                new CodecRef("STRING", 1, null),
                200L, 5L));

        rootPageIds = new HashMap<>();
        rootPageIds.put(1L, 100L);
        rootPageIds.put(2L, 200L);

        snapshot = new StoreSnapshot(1L, 4096L, catalog, states, rootPageIds, 3L);
    }

    // ==================== 불변성 테스트 ====================

    @Test
    public void testImmutability_OriginalMapModification() {
        // 원본 Map 수정해도 스냅샷 불변
        catalog.put("newCollection", new CatalogEntry("newCollection", 99L));

        assertFalse("스냅샷은 원본 Map 변경에 영향받지 않아야 함",
                snapshot.getCatalog().containsKey("newCollection"));
        assertEquals(2, snapshot.getCatalog().size());
    }

    @Test
    public void testImmutability_StatesMapModification() {
        // states Map 수정해도 스냅샷 불변
        states.put(99L, new CollectionState(99L, CollectionKind.LIST,
                null, new CodecRef("STRING", 1, null), 0L, 0L));

        assertFalse("스냅샷은 원본 states Map 변경에 영향받지 않아야 함",
                snapshot.getStates().containsKey(99L));
        assertEquals(2, snapshot.getStates().size());
    }

    @Test
    public void testImmutability_RootPageIdsMapModification() {
        // rootPageIds Map 수정해도 스냅샷 불변
        rootPageIds.put(99L, 9999L);

        assertNull("스냅샷은 원본 rootPageIds 변경에 영향받지 않아야 함",
                snapshot.getRootPageId(99L));
        assertEquals(2, snapshot.getRootPageIds().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutability_CatalogModificationThrowsException() {
        // 스냅샷의 catalog 직접 수정 시도 시 예외
        snapshot.getCatalog().put("illegal", new CatalogEntry("illegal", 999L));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutability_StatesModificationThrowsException() {
        // 스냅샷의 states 직접 수정 시도 시 예외
        snapshot.getStates().put(999L, new CollectionState(999L, CollectionKind.MAP,
                null, null, 0L, 0L));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutability_RootPageIdsModificationThrowsException() {
        // 스냅샷의 rootPageIds 직접 수정 시도 시 예외
        snapshot.getRootPageIds().put(999L, 9999L);
    }

    // ==================== with* 메서드 테스트 ====================

    @Test
    public void testWithAllocTail() {
        long newTail = 8192L;
        StoreSnapshot newSnap = snapshot.withAllocTail(newTail);

        // 원본 불변
        assertEquals(4096L, snapshot.getAllocTail());
        assertEquals(1L, snapshot.getSeqNo());

        // 새 스냅샷 변경됨
        assertEquals(newTail, newSnap.getAllocTail());
        assertEquals(2L, newSnap.getSeqNo());  // seqNo 증가

        // 다른 필드는 동일
        assertEquals(snapshot.getCatalog(), newSnap.getCatalog());
        assertEquals(snapshot.getStates(), newSnap.getStates());
        assertEquals(snapshot.getRootPageIds(), newSnap.getRootPageIds());
    }

    @Test
    public void testWithRootPageId() {
        long newRoot = 500L;
        StoreSnapshot newSnap = snapshot.withRootPageId(1L, newRoot);

        // 원본 불변
        assertEquals(Long.valueOf(100L), snapshot.getRootPageId(1L));
        assertEquals(1L, snapshot.getSeqNo());

        // 새 스냅샷 변경됨
        assertEquals(Long.valueOf(newRoot), newSnap.getRootPageId(1L));
        assertEquals(2L, newSnap.getSeqNo());

        // 다른 컬렉션의 rootPageId는 변경 안됨
        assertEquals(Long.valueOf(200L), newSnap.getRootPageId(2L));
    }

    @Test
    public void testWithRootAndAllocTail() {
        long newRoot = 600L;
        long newTail = 12288L;
        StoreSnapshot newSnap = snapshot.withRootAndAllocTail(1L, newRoot, newTail);

        // 원본 불변
        assertEquals(Long.valueOf(100L), snapshot.getRootPageId(1L));
        assertEquals(4096L, snapshot.getAllocTail());

        // 새 스냅샷 모두 변경됨
        assertEquals(Long.valueOf(newRoot), newSnap.getRootPageId(1L));
        assertEquals(newTail, newSnap.getAllocTail());
        assertEquals(2L, newSnap.getSeqNo());
    }

    @Test
    public void testWithState() {
        CollectionState newState = new CollectionState(1L, CollectionKind.MAP,
                new CodecRef("STRING", 2, null),  // version 업그레이드
                new CodecRef("I64", 1, null),
                150L, 20L);

        StoreSnapshot newSnap = snapshot.withState(1L, newState);

        // 원본 불변
        assertEquals(10L, snapshot.getState(1L).getCount());

        // 새 스냅샷 변경됨
        assertEquals(20L, newSnap.getState(1L).getCount());
        assertEquals(2L, newSnap.getSeqNo());
    }

    @Test
    public void testWithCatalogEntry() {
        CatalogEntry newEntry = new CatalogEntry("newList", 3L);
        StoreSnapshot newSnap = snapshot.withCatalogEntry("newList", newEntry);

        // 원본 불변
        assertFalse(snapshot.getCatalog().containsKey("newList"));
        assertEquals(2, snapshot.getCatalog().size());

        // 새 스냅샷에 추가됨
        assertTrue(newSnap.getCatalog().containsKey("newList"));
        assertEquals(3, newSnap.getCatalog().size());
        assertEquals(2L, newSnap.getSeqNo());
    }

    @Test
    public void testWithoutCatalogEntry() {
        StoreSnapshot newSnap = snapshot.withoutCatalogEntry("testMap");

        // 원본 불변
        assertTrue(snapshot.getCatalog().containsKey("testMap"));
        assertEquals(2, snapshot.getCatalog().size());

        // 새 스냅샷에서 삭제됨
        assertFalse(newSnap.getCatalog().containsKey("testMap"));
        assertEquals(1, newSnap.getCatalog().size());
        assertEquals(2L, newSnap.getSeqNo());
    }

    @Test
    public void testWithNextCollectionId() {
        StoreSnapshot newSnap = snapshot.withNextCollectionId(10L);

        // 원본 불변
        assertEquals(3L, snapshot.getNextCollectionId());

        // 새 스냅샷 변경됨
        assertEquals(10L, newSnap.getNextCollectionId());
        assertEquals(2L, newSnap.getSeqNo());
    }

    @Test
    public void testWithNewCollection() {
        String name = "newDeque";
        CatalogEntry entry = new CatalogEntry(name, 3L);
        CollectionState state = new CollectionState(3L, CollectionKind.DEQUE,
                null, new CodecRef("STRING", 1, null), 0L, 0L);

        StoreSnapshot newSnap = snapshot.withNewCollection(name, entry, state, 300L, 4L);

        // 원본 불변
        assertFalse(snapshot.getCatalog().containsKey(name));
        assertNull(snapshot.getState(3L));
        assertNull(snapshot.getRootPageId(3L));
        assertEquals(3L, snapshot.getNextCollectionId());

        // 새 스냅샷에 모두 추가됨
        assertTrue(newSnap.getCatalog().containsKey(name));
        assertNotNull(newSnap.getState(3L));
        assertEquals(Long.valueOf(300L), newSnap.getRootPageId(3L));
        assertEquals(4L, newSnap.getNextCollectionId());
        assertEquals(2L, newSnap.getSeqNo());
    }

    @Test
    public void testWithoutCollection() {
        StoreSnapshot newSnap = snapshot.withoutCollection("testMap", 1L);

        // 원본 불변
        assertTrue(snapshot.getCatalog().containsKey("testMap"));
        assertNotNull(snapshot.getState(1L));
        assertNotNull(snapshot.getRootPageId(1L));

        // 새 스냅샷에서 모두 삭제됨
        assertFalse(newSnap.getCatalog().containsKey("testMap"));
        assertNull(newSnap.getState(1L));
        assertNull(newSnap.getRootPageId(1L));
        assertEquals(2L, newSnap.getSeqNo());

        // 다른 컬렉션은 유지
        assertTrue(newSnap.getCatalog().containsKey("testSet"));
        assertNotNull(newSnap.getState(2L));
    }

    // ==================== Getter 테스트 ====================

    @Test
    public void testGetters() {
        assertEquals(1L, snapshot.getSeqNo());
        assertEquals(4096L, snapshot.getAllocTail());
        assertEquals(3L, snapshot.getNextCollectionId());
        assertEquals(2, snapshot.getCatalog().size());
        assertEquals(2, snapshot.getStates().size());
        assertEquals(2, snapshot.getRootPageIds().size());
    }

    @Test
    public void testGetRootPageId() {
        assertEquals(Long.valueOf(100L), snapshot.getRootPageId(1L));
        assertEquals(Long.valueOf(200L), snapshot.getRootPageId(2L));
        assertNull(snapshot.getRootPageId(999L));  // 없는 컬렉션
    }

    @Test
    public void testGetState() {
        CollectionState state1 = snapshot.getState(1L);
        assertNotNull(state1);
        assertEquals(CollectionKind.MAP, state1.getKind());
        assertEquals(10L, state1.getCount());

        CollectionState state2 = snapshot.getState(2L);
        assertNotNull(state2);
        assertEquals(CollectionKind.SET, state2.getKind());

        assertNull(snapshot.getState(999L));  // 없는 컬렉션
    }

    // ==================== seqNo 증가 테스트 ====================

    @Test
    public void testSeqNoIncrement_ChainedOperations() {
        // 연속 with 연산에서 seqNo가 계속 증가
        StoreSnapshot snap1 = snapshot.withAllocTail(5000L);
        StoreSnapshot snap2 = snap1.withAllocTail(6000L);
        StoreSnapshot snap3 = snap2.withRootPageId(1L, 999L);

        assertEquals(1L, snapshot.getSeqNo());
        assertEquals(2L, snap1.getSeqNo());
        assertEquals(3L, snap2.getSeqNo());
        assertEquals(4L, snap3.getSeqNo());
    }

    // ==================== equals/hashCode 테스트 ====================

    @Test
    public void testEquals() {
        StoreSnapshot same = new StoreSnapshot(1L, 4096L, catalog, states, rootPageIds, 3L);

        // 동일한 내용의 스냅샷은 equals
        assertEquals(snapshot, same);
        assertEquals(snapshot.hashCode(), same.hashCode());
    }

    @Test
    public void testNotEquals_DifferentSeqNo() {
        StoreSnapshot different = new StoreSnapshot(99L, 4096L, catalog, states, rootPageIds, 3L);
        assertNotEquals(snapshot, different);
    }

    @Test
    public void testNotEquals_DifferentAllocTail() {
        StoreSnapshot different = new StoreSnapshot(1L, 9999L, catalog, states, rootPageIds, 3L);
        assertNotEquals(snapshot, different);
    }

    // ==================== toString 테스트 ====================

    @Test
    public void testToString() {
        String str = snapshot.toString();
        assertTrue(str.contains("seqNo=1"));
        assertTrue(str.contains("allocTail=4096"));
        assertTrue(str.contains("catalogSize=2"));
    }

    // ==================== NullPointerException 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void testNullCatalog() {
        new StoreSnapshot(1L, 4096L, null, states, rootPageIds, 3L);
    }

    @Test(expected = NullPointerException.class)
    public void testNullStates() {
        new StoreSnapshot(1L, 4096L, catalog, null, rootPageIds, 3L);
    }

    @Test(expected = NullPointerException.class)
    public void testNullRootPageIds() {
        new StoreSnapshot(1L, 4096L, catalog, states, null, 3L);
    }

    // ==================== 빈 Map 테스트 ====================

    @Test
    public void testEmptyMaps() {
        StoreSnapshot empty = new StoreSnapshot(0L, 12288L,
                new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);

        assertEquals(0, empty.getCatalog().size());
        assertEquals(0, empty.getStates().size());
        assertEquals(0, empty.getRootPageIds().size());
        assertNull(empty.getRootPageId(1L));
        assertNull(empty.getState(1L));
    }
}
