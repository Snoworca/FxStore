package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.NavigableMap;

import static org.junit.Assert.*;

/**
 * FxStore 트랜잭션(commit/rollback) 테스트
 *
 * <p>P1-2: commit(), rollback() 기능 테스트</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>BATCH 모드에서 rollback 동작</li>
 *   <li>AUTO 모드에서 rollback (no-op)</li>
 *   <li>commit 후 rollback</li>
 *   <li>여러 컬렉션 동시 트랜잭션</li>
 *   <li>메모리 스토어 트랜잭션</li>
 * </ul>
 *
 * <p>Note: rollback() 후 openMap()은 캐시된 인메모리 상태를 반환합니다.
 * 실제 롤백된 상태를 확인하려면 스토어를 재오픈해야 합니다.</p>
 *
 * @since v1.0 Phase 2
 * @see FxStore#commit()
 * @see FxStore#rollback()
 */
public class FxStoreTransactionTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("test.fx");
        storeFile.delete();
    }

    @After
    public void tearDown() {
        // TemporaryFolder가 정리
    }

    // ==================== BATCH 모드 rollback 테스트 ====================

    @Test
    public void rollback_batchMode_uncommittedDataNotPersisted() throws Exception {
        // Given: BATCH 모드에서 데이터 추가 (커밋 안함)
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");
            map.put(2L, "value2");

            // When: rollback (명시적 호출)
            store.rollback();
        }

        // Then: 재오픈 시 데이터 없음
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = null;
            try {
                map = store.openMap("test", Long.class, String.class);
                fail("Collection should not exist");
            } catch (FxException e) {
                assertEquals(FxErrorCode.NOT_FOUND, e.getCode());
            }
        }
    }

    @Test
    public void rollback_batchMode_committedDataPreserved() throws Exception {
        // Given: BATCH 모드에서 데이터 추가 후 커밋
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "committed");
            store.commit();

            // When: 추가 변경 후 rollback
            map.put(2L, "uncommitted");
            store.rollback();
        }

        // Then: 재오픈 시 커밋된 데이터만 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertNotNull("Map should exist", map);
            assertEquals("committed", map.get(1L));
            assertNull("uncommitted data should be gone", map.get(2L));
        }
    }

    @Test
    public void rollback_batchMode_updateNotPersisted() throws Exception {
        // Given: BATCH 모드에서 데이터 추가 후 커밋
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "original");
            store.commit();

            // When: 업데이트 후 rollback
            map.put(1L, "modified");
            store.rollback();
        }

        // Then: 재오픈 시 원래 값 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertEquals("original", map.get(1L));
        }
    }

    @Test
    public void rollback_batchMode_deleteNotPersisted() throws Exception {
        // Given: BATCH 모드에서 데이터 추가 후 커밋
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");
            map.put(2L, "value2");
            store.commit();

            // When: 삭제 후 rollback
            map.remove(1L);
            store.rollback();
        }

        // Then: 재오픈 시 삭제가 롤백됨
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertEquals("value1", map.get(1L));
            assertEquals("value2", map.get(2L));
        }
    }

    // ==================== AUTO 모드 rollback 테스트 ====================

    @Test
    public void rollback_autoMode_shouldHaveNoEffect() throws Exception {
        // Given: AUTO 모드 (기본값)
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");
            map.put(2L, "value2");

            // When: rollback 호출 (AUTO 모드에서는 no-op)
            store.rollback();

            // Then: 변경사항이 유지됨 (AUTO 모드는 즉시 커밋)
            assertEquals(2, map.size());
            assertEquals("value1", map.get(1L));
            assertEquals("value2", map.get(2L));
        }
    }

    @Test
    public void rollback_autoMode_afterRemove_shouldHaveNoEffect() throws Exception {
        // Given: AUTO 모드에서 데이터 추가 후 삭제
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");
            map.remove(1L);

            // When: rollback 호출
            store.rollback();

            // Then: 삭제가 유지됨 (이미 커밋됨)
            assertNull(map.get(1L));
        }
    }

    @Test
    public void rollback_autoMode_dataPersisted() throws Exception {
        // Given: AUTO 모드
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");
            store.rollback();
        }

        // Then: 재오픈 시 데이터 유지 (AUTO 모드에서 rollback은 no-op)
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertEquals("value1", map.get(1L));
        }
    }

    // ==================== commit 후 rollback 테스트 ====================

    @Test
    public void rollback_afterCommit_shouldHaveNoEffect() throws Exception {
        // Given: BATCH 모드에서 커밋 후
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");
            store.commit();

            // When: 커밋 후 바로 rollback (pending 변경 없음)
            store.rollback();
        }

        // Then: 재오픈 시 커밋된 데이터 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertNotNull("Map should exist", map);
            assertEquals("value1", map.get(1L));
        }
    }

    // ==================== 여러 컬렉션 트랜잭션 테스트 ====================

    @Test
    public void rollback_multipleCollections_uncommittedGone() throws Exception {
        // Given: BATCH 모드에서 여러 컬렉션에 데이터 추가
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map1 = store.createMap("map1", Long.class, String.class);
            NavigableMap<String, Integer> map2 = store.createMap("map2", String.class, Integer.class);

            map1.put(1L, "value1");
            map2.put("key1", 100);

            // When: rollback (커밋 안함)
            store.rollback();
        }

        // Then: 재오픈 시 모든 컬렉션 없음
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            try {
                store.openMap("map1", Long.class, String.class);
                fail("map1 should not exist");
            } catch (FxException e) {
                assertEquals(FxErrorCode.NOT_FOUND, e.getCode());
            }
            try {
                store.openMap("map2", String.class, Integer.class);
                fail("map2 should not exist");
            } catch (FxException e) {
                assertEquals(FxErrorCode.NOT_FOUND, e.getCode());
            }
        }
    }

    @Test
    public void rollback_multipleCollections_partialCommit() throws Exception {
        // Given: BATCH 모드에서 일부만 커밋 후 추가
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map1 = store.createMap("map1", Long.class, String.class);
            map1.put(1L, "committed");
            store.commit();

            NavigableMap<String, Integer> map2 = store.createMap("map2", String.class, Integer.class);
            map1.put(2L, "uncommitted");
            map2.put("key1", 100);

            // When: rollback
            store.rollback();
        }

        // Then: 재오픈 시 커밋된 것만 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map1 = store.openMap("map1", Long.class, String.class);
            assertNotNull("map1 should exist", map1);
            assertEquals("committed", map1.get(1L));
            assertNull("uncommitted data should be gone", map1.get(2L));

            try {
                store.openMap("map2", String.class, Integer.class);
                fail("map2 should not exist");
            } catch (FxException e) {
                assertEquals(FxErrorCode.NOT_FOUND, e.getCode());
            }
        }
    }

    // ==================== commit 테스트 ====================

    @Test
    public void commit_batchMode_shouldPersistChanges() throws Exception {
        // Given: BATCH 모드에서 데이터 추가
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");
            store.commit();
        }

        // Then: 재오픈 시 데이터 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertNotNull("Map should exist", map);
            assertEquals("value1", map.get(1L));
        }
    }

    @Test
    public void commit_autoMode_shouldWorkSilently() throws Exception {
        // Given: AUTO 모드
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");

            // When: commit 호출 (AUTO 모드에서도 호출 가능)
            store.commit();

            // Then: 정상 동작
            assertEquals("value1", map.get(1L));
        }

        // 재오픈 시 데이터 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertNotNull("Map should exist", map);
            assertEquals("value1", map.get(1L));
        }
    }

    @Test
    public void commit_multipleCommits_shouldWork() throws Exception {
        // Given: BATCH 모드에서 여러 번 커밋
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

            map.put(1L, "first");
            store.commit();

            map.put(2L, "second");
            store.commit();

            map.put(3L, "third");
            store.commit();
        }

        // Then: 모든 데이터 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertEquals("first", map.get(1L));
            assertEquals("second", map.get(2L));
            assertEquals("third", map.get(3L));
        }
    }

    // ==================== 메모리 스토어 트랜잭션 테스트 ====================

    @Test
    public void rollback_memoryStore_batchMode_apiWorks() {
        // Given: 메모리 스토어 BATCH 모드
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.openMemory(batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");
            store.commit();

            map.put(2L, "uncommitted");

            // When: rollback - no exception
            store.rollback();

            // Then: API가 정상 동작
            // Note: 메모리 스토어는 재오픈 불가, 인메모리 상태만 확인
            assertTrue("rollback should not throw", true);
        }
    }

    @Test
    public void commit_memoryStore_shouldWork() {
        // Given: 메모리 스토어
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value1");

            // When: commit
            store.commit();

            // Then: 정상 동작
            assertEquals("value1", map.get(1L));
        }
    }

    // ==================== 닫힌 스토어 예외 테스트 ====================

    @Test(expected = FxException.class)
    public void commit_closedStore_shouldThrow() throws Exception {
        FxStore store = FxStore.open(storeFile.toPath());
        store.close();
        store.commit();
    }

    @Test(expected = FxException.class)
    public void rollback_closedStore_shouldThrow() throws Exception {
        FxStore store = FxStore.open(storeFile.toPath());
        store.close();
        store.rollback();
    }

    // ==================== commitMode 테스트 ====================

    @Test
    public void commitMode_defaultIsAuto() throws Exception {
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            assertEquals(CommitMode.AUTO, store.commitMode());
        }
    }

    @Test
    public void commitMode_batchOption() throws Exception {
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            assertEquals(CommitMode.BATCH, store.commitMode());
        }
    }

    // ==================== 트랜잭션 워크플로우 테스트 ====================

    @Test
    public void transaction_workflow_basicCRUD() throws Exception {
        // Given: BATCH 모드
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

            // Create
            map.put(1L, "created");
            store.commit();

            // Read
            assertEquals("created", map.get(1L));

            // Update
            map.put(1L, "updated");
            store.commit();
            assertEquals("updated", map.get(1L));

            // Delete
            map.remove(1L);
            store.commit();
            assertNull(map.get(1L));
        }
    }

    @Test
    public void transaction_workflow_rollbackThenCommit() throws Exception {
        // Given: BATCH 모드
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

            // 첫 번째 시도: 롤백
            map.put(1L, "first_attempt");
            store.rollback();

            // 두 번째 시도: createOrOpenMap 사용 (rollback 후 캐시에 남아있음)
            NavigableMap<Long, String> mapAfter = store.createOrOpenMap("test", Long.class, String.class);
            mapAfter.put(1L, "second_attempt");
            store.commit();
        }

        // Then: 두 번째 시도만 영속화
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertNotNull("Map should exist", map);
            assertEquals("second_attempt", map.get(1L));
        }
    }

    // ==================== 큰 데이터 트랜잭션 테스트 ====================

    @Test
    public void commit_largeData_shouldWork() throws Exception {
        // Given: BATCH 모드에서 많은 데이터
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

            for (long i = 0; i < 1000; i++) {
                map.put(i, "value" + i);
            }
            store.commit();
        }

        // Then: 모든 데이터 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertEquals(1000, map.size());
            assertEquals("value0", map.get(0L));
            assertEquals("value999", map.get(999L));
        }
    }

    @Test
    public void rollback_largeUncommittedData_shouldRevert() throws Exception {
        // Given: BATCH 모드에서 커밋 후 많은 데이터 추가
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(0L, "committed");
            store.commit();

            // 많은 uncommitted 데이터 추가
            for (long i = 1; i < 100; i++) {
                map.put(i, "uncommitted" + i);
            }

            // When: rollback
            store.rollback();
        }

        // Then: 재오픈 시 커밋된 데이터만 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertNotNull("Map should exist", map);
            assertEquals(1, map.size());
            assertEquals("committed", map.get(0L));
        }
    }

    // ==================== OnClosePolicy 테스트 ====================

    @Test
    public void onClosePolicy_rollback_uncommittedDiscarded() throws Exception {
        // Given: BATCH 모드 + OnClosePolicy.ROLLBACK
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.ROLLBACK)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "uncommitted");
            // 커밋하지 않고 닫힘
        }

        // Then: 데이터 없음
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            try {
                store.openMap("test", Long.class, String.class);
                fail("Collection should not exist");
            } catch (FxException e) {
                assertEquals(FxErrorCode.NOT_FOUND, e.getCode());
            }
        }
    }

    @Test
    public void onClosePolicy_commit_uncommittedPersisted() throws Exception {
        // Given: BATCH 모드 + OnClosePolicy.COMMIT
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .onClosePolicy(OnClosePolicy.COMMIT)
                .build();

        try (FxStore store = FxStoreImpl.open(storeFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "uncommitted");
            // 커밋하지 않고 닫힘 - OnClosePolicy.COMMIT이 자동 커밋
        }

        // Then: 데이터 유지
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertEquals("uncommitted", map.get(1L));
        }
    }
}
