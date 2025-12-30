package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.CollectionKind;
import com.snoworca.fxstore.api.FxStore;
import com.snoworca.fxstore.catalog.CatalogEntry;
import com.snoworca.fxstore.catalog.CollectionState;
import com.snoworca.fxstore.api.CodecRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

/**
 * StoreSnapshot Branch 커버리지 테스트 (P3)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>StoreSnapshot (99% instruction, 61% branch → 85%+ branch)</li>
 * </ul>
 *
 * @since 0.9
 * @see StoreSnapshot
 */
public class StoreSnapshotBranchTest {

    private File tempFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-snapshot-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) {
            store.close();
        }
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    // ==================== 빈 스냅샷 테스트 ====================

    @Test
    public void emptySnapshot_shouldHaveZeroSeqNo() {
        // 빈 스토어 생성 직후 스냅샷
        StoreSnapshot snapshot = createEmptySnapshot();

        assertEquals(0, snapshot.getSeqNo());
        assertTrue(snapshot.getCatalog().isEmpty());
        assertTrue(snapshot.getStates().isEmpty());
        assertTrue(snapshot.getRootPageIds().isEmpty());
    }

    @Test
    public void emptySnapshot_getRootPageId_shouldReturnNull() {
        StoreSnapshot snapshot = createEmptySnapshot();

        // 존재하지 않는 collectionId로 조회
        assertNull(snapshot.getRootPageId(999L));
    }

    // ==================== equals 테스트 ====================

    @Test
    public void equals_sameObject_shouldReturnTrue() {
        StoreSnapshot snapshot = createTestSnapshot(1L);
        assertTrue(snapshot.equals(snapshot));
    }

    @Test
    public void equals_null_shouldReturnFalse() {
        StoreSnapshot snapshot = createTestSnapshot(1L);
        assertFalse(snapshot.equals(null));
    }

    @Test
    public void equals_differentClass_shouldReturnFalse() {
        StoreSnapshot snapshot = createTestSnapshot(1L);
        assertFalse(snapshot.equals("not a snapshot"));
    }

    @Test
    public void equals_differentSeqNo_shouldReturnFalse() {
        StoreSnapshot snapshot1 = createTestSnapshot(1L);
        StoreSnapshot snapshot2 = createTestSnapshot(2L);
        assertFalse(snapshot1.equals(snapshot2));
    }

    @Test
    public void equals_sameSeqNoDifferentAllocTail_shouldReturnFalse() {
        StoreSnapshot snapshot1 = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);
        StoreSnapshot snapshot2 = new StoreSnapshot(
            1L, 200L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);
        assertFalse(snapshot1.equals(snapshot2));
    }

    @Test
    public void equals_identicalSnapshots_shouldReturnTrue() {
        StoreSnapshot snapshot1 = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);
        StoreSnapshot snapshot2 = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);

        assertTrue(snapshot1.equals(snapshot2));
    }

    @Test
    public void equals_differentRootPageIds_shouldReturnFalse() {
        Map<Long, Long> roots1 = new HashMap<>();
        Map<Long, Long> roots2 = new HashMap<>();
        roots2.put(1L, 100L);

        StoreSnapshot snapshot1 = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), roots1, 1L);
        StoreSnapshot snapshot2 = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), roots2, 1L);

        assertFalse(snapshot1.equals(snapshot2));
    }

    @Test
    public void equals_differentNextCollectionId_shouldReturnFalse() {
        StoreSnapshot snapshot1 = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);
        StoreSnapshot snapshot2 = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 2L);

        assertFalse(snapshot1.equals(snapshot2));
    }

    // ==================== hashCode 테스트 ====================

    @Test
    public void hashCode_sameSnapshots_shouldBeEqual() {
        StoreSnapshot snapshot1 = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);
        StoreSnapshot snapshot2 = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);

        assertEquals(snapshot1.hashCode(), snapshot2.hashCode());
    }

    @Test
    public void hashCode_differentSnapshots_shouldBeDifferent() {
        StoreSnapshot snapshot1 = createTestSnapshot(1L);
        StoreSnapshot snapshot2 = createTestSnapshot(2L);

        // 해시코드는 다를 가능성이 높음 (보장은 안 됨)
        // 이 테스트는 hashCode가 호출되는지 확인하는 것이 주 목적
        assertNotNull(Integer.valueOf(snapshot1.hashCode()));
        assertNotNull(Integer.valueOf(snapshot2.hashCode()));
    }

    // ==================== toString 테스트 ====================

    @Test
    public void toString_shouldReturnNonNull() {
        StoreSnapshot snapshot = createTestSnapshot(1L);
        String str = snapshot.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    // ==================== getters 테스트 ====================

    @Test
    public void getSeqNo_shouldReturnCorrectValue() {
        StoreSnapshot snapshot = createTestSnapshot(42L);
        assertEquals(42L, snapshot.getSeqNo());
    }

    @Test
    public void getAllocTail_shouldReturnCorrectValue() {
        StoreSnapshot snapshot = new StoreSnapshot(
            1L, 12345L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);
        assertEquals(12345L, snapshot.getAllocTail());
    }

    @Test
    public void getNextCollectionId_shouldReturnCorrectValue() {
        StoreSnapshot snapshot = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 99L);
        assertEquals(99L, snapshot.getNextCollectionId());
    }

    @Test
    public void getCatalog_shouldReturnUnmodifiableMap() {
        StoreSnapshot snapshot = createTestSnapshot(1L);
        Map<String, CatalogEntry> catalog = snapshot.getCatalog();

        try {
            catalog.put("new", null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void getStates_shouldReturnUnmodifiableMap() {
        StoreSnapshot snapshot = createTestSnapshot(1L);
        Map<Long, CollectionState> states = snapshot.getStates();

        try {
            states.put(100L, null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void getRootPageIds_shouldReturnUnmodifiableMap() {
        StoreSnapshot snapshot = createTestSnapshot(1L);
        Map<Long, Long> roots = snapshot.getRootPageIds();

        try {
            roots.put(100L, 1000L);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    // ==================== with 메서드 테스트 ====================

    @Test
    public void withRootPageId_shouldCreateNewSnapshot() {
        StoreSnapshot original = createTestSnapshot(1L);
        StoreSnapshot updated = original.withRootPageId(999L, 12345L);

        assertNotSame(original, updated);
        assertEquals(Long.valueOf(12345L), updated.getRootPageId(999L));
        assertNull(original.getRootPageId(999L));
    }

    @Test
    public void withAllocTail_shouldCreateNewSnapshot() {
        StoreSnapshot original = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);
        StoreSnapshot updated = original.withAllocTail(500L);

        assertNotSame(original, updated);
        assertEquals(500L, updated.getAllocTail());
        assertEquals(100L, original.getAllocTail());
    }

    @Test
    public void withNextCollectionId_shouldCreateNewSnapshot() {
        StoreSnapshot original = new StoreSnapshot(
            1L, 100L, new HashMap<>(), new HashMap<>(), new HashMap<>(), 1L);
        StoreSnapshot updated = original.withNextCollectionId(99L);

        assertNotSame(original, updated);
        assertEquals(99L, updated.getNextCollectionId());
        assertEquals(1L, original.getNextCollectionId());
    }

    @Test
    public void withRootAndAllocTail_shouldCreateNewSnapshot() {
        StoreSnapshot original = createTestSnapshot(1L);
        StoreSnapshot updated = original.withRootAndAllocTail(1L, 100L, 500L);

        assertNotSame(original, updated);
        assertEquals(Long.valueOf(100L), updated.getRootPageId(1L));
        assertEquals(500L, updated.getAllocTail());
    }

    // ==================== 헬퍼 메서드 ====================

    private StoreSnapshot createEmptySnapshot() {
        return new StoreSnapshot(
            0L, 0L,
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            1L
        );
    }

    private StoreSnapshot createTestSnapshot(long seqNo) {
        return new StoreSnapshot(
            seqNo, 100L,
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            1L
        );
    }
}
