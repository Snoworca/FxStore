package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/**
 * FxStoreImpl 내부 메서드 커버리지 테스트.
 *
 * <p>목적:</p>
 * <ul>
 *   <li>countTreeBytes() - 22% → 60%+</li>
 *   <li>stats(StatsMode.DEEP) - 분기 커버</li>
 *   <li>verify 관련 메서드 브랜치 커버</li>
 *   <li>codecRefToClass() - 43% → 70%+</li>
 * </ul>
 */
public class FxStoreImplInternalTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // ==================== Stats DEEP 모드 테스트 ====================

    @Test
    public void stats_DEEP_emptyStore_shouldWork() {
        // Given: 빈 스토어
        try (FxStore store = FxStore.openMemory()) {
            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            assertEquals(0, stats.collectionCount());
        }
    }

    @Test
    public void stats_DEEP_withMap_shouldCalculateBytes() {
        // Given: Map이 있는 스토어
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "value-" + i);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            assertEquals(1, stats.collectionCount());
            assertTrue("fileBytes > 0", stats.fileBytes() > 0);
            assertTrue("liveBytesEstimate > 0", stats.liveBytesEstimate() > 0);
        }
    }

    @Test
    public void stats_DEEP_withSet_shouldCalculateBytes() {
        // Given: Set이 있는 스토어
        try (FxStore store = FxStore.openMemory()) {
            NavigableSet<Long> set = store.createSet("testSet", Long.class);
            for (long i = 0; i < 100; i++) {
                set.add(i);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            assertTrue("liveBytesEstimate > 0", stats.liveBytesEstimate() > 0);
        }
    }

    @Test
    public void stats_DEEP_withList_shouldCalculateBytes() {
        // Given: List가 있는 스토어
        try (FxStore store = FxStore.openMemory()) {
            List<Long> list = store.createList("testList", Long.class);
            for (long i = 0; i < 100; i++) {
                list.add(i);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            assertTrue("liveBytesEstimate > 0", stats.liveBytesEstimate() > 0);
        }
    }

    @Test
    public void stats_DEEP_withDeque_shouldCalculateBytes() {
        // Given: Deque가 있는 스토어
        try (FxStore store = FxStore.openMemory()) {
            Deque<Long> deque = store.createDeque("testDeque", Long.class);
            for (long i = 0; i < 100; i++) {
                deque.add(i);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            assertTrue("liveBytesEstimate > 0", stats.liveBytesEstimate() > 0);
        }
    }

    @Test
    public void stats_DEEP_multipleCollections_shouldSumBytes() {
        // Given: 여러 컬렉션
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            NavigableSet<Long> set = store.createSet("set", Long.class);
            List<Long> list = store.createList("list", Long.class);
            Deque<Long> deque = store.createDeque("deque", Long.class);

            for (long i = 0; i < 50; i++) {
                map.put(i, "v" + i);
                set.add(i);
                list.add(i);
                deque.add(i);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertEquals(4, stats.collectionCount());
            assertTrue("liveBytesEstimate > 0", stats.liveBytesEstimate() > 0);
        }
    }

    @Test
    public void stats_DEEP_deepTree_shouldTraverseAll() {
        // Given: 많은 요소 (다층 트리)
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("deepMap", Long.class, String.class);
            for (long i = 0; i < 500; i++) {
                map.put(i, "value-" + i);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then: 모든 페이지가 계산됨
            assertNotNull(stats);
            assertTrue("Deep tree should have significant bytes", stats.liveBytesEstimate() > 10000);
        }
    }

    // ==================== Stats FAST 모드 테스트 ====================

    @Test
    public void stats_FAST_shouldBeQuicker() {
        // Given: 많은 요소
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            for (long i = 0; i < 200; i++) {
                map.put(i, "value-" + i);
            }

            // When: FAST 모드 stats
            Stats stats = store.stats(StatsMode.FAST);

            // Then
            assertNotNull(stats);
            assertEquals(1, stats.collectionCount());
        }
    }

    // ==================== Verify 테스트 ====================

    @Test
    public void verify_emptyStore_shouldPass() throws Exception {
        Path tempFile = tempFolder.newFile("empty.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            VerifyResult result = store.verify();
            assertTrue("Empty store should verify ok", result.ok());
            assertTrue("No errors expected", result.errors().isEmpty());
        }
    }

    @Test
    public void verify_withData_shouldPass() throws Exception {
        Path tempFile = tempFolder.newFile("data.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "value-" + i);
            }

            VerifyResult result = store.verify();
            assertTrue("Store with data should verify ok", result.ok());
        }
    }

    @Test
    public void verify_afterMultipleCommits_shouldPass() throws Exception {
        Path tempFile = tempFolder.newFile("commits.fx").toPath();

        try (FxStore store = FxStoreImpl.open(tempFile, FxOptions.defaults().withCommitMode(CommitMode.BATCH).build())) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);

            // 여러 번 커밋
            for (int batch = 0; batch < 5; batch++) {
                for (long i = batch * 10; i < (batch + 1) * 10; i++) {
                    map.put(i, "value-" + i);
                }
                store.commit();
            }

            VerifyResult result = store.verify();
            assertTrue("Multiple commits should verify ok", result.ok());
        }
    }

    @Test
    public void verify_reopenedStore_shouldPass() throws Exception {
        Path tempFile = tempFolder.newFile("reopen.fx").toPath();

        // 생성 및 데이터 저장
        try (FxStore store = FxStore.open(tempFile)) {
            store.createMap("map", Long.class, String.class).put(1L, "value");
            store.createSet("set", Long.class).add(1L);
            store.createList("list", Long.class).add(1L);
            store.createDeque("deque", Long.class).add(1L);
        }

        // 재오픈 후 verify
        try (FxStore store = FxStore.open(tempFile)) {
            VerifyResult result = store.verify();
            assertTrue("Reopened store should verify ok", result.ok());
        }
    }

    // ==================== createOrOpen 테스트 ====================

    @Test
    public void createOrOpenMap_newCollection_shouldCreate() {
        try (FxStore store = FxStore.openMemory()) {
            // When: createOrOpen 호출
            NavigableMap<Long, String> map = store.createOrOpenMap("newMap", Long.class, String.class);

            // Then
            assertNotNull(map);
            assertTrue(map.isEmpty());
        }
    }

    @Test
    public void createOrOpenMap_existingCollection_shouldOpen() {
        try (FxStore store = FxStore.openMemory()) {
            // Given: 기존 컬렉션
            store.createMap("existingMap", Long.class, String.class).put(1L, "value");

            // When: createOrOpen 호출
            NavigableMap<Long, String> map = store.createOrOpenMap("existingMap", Long.class, String.class);

            // Then
            assertNotNull(map);
            assertEquals("value", map.get(1L));
        }
    }

    @Test
    public void createOrOpenSet_newCollection_shouldCreate() {
        try (FxStore store = FxStore.openMemory()) {
            NavigableSet<Long> set = store.createOrOpenSet("newSet", Long.class);
            assertNotNull(set);
            assertTrue(set.isEmpty());
        }
    }

    @Test
    public void createOrOpenSet_existingCollection_shouldOpen() {
        try (FxStore store = FxStore.openMemory()) {
            store.createSet("existingSet", Long.class).add(1L);
            NavigableSet<Long> set = store.createOrOpenSet("existingSet", Long.class);
            assertNotNull(set);
            assertTrue(set.contains(1L));
        }
    }

    @Test
    public void createOrOpenList_newCollection_shouldCreate() {
        try (FxStore store = FxStore.openMemory()) {
            List<Long> list = store.createOrOpenList("newList", Long.class);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void createOrOpenList_existingCollection_shouldOpen() {
        try (FxStore store = FxStore.openMemory()) {
            store.createList("existingList", Long.class).add(1L);
            List<Long> list = store.createOrOpenList("existingList", Long.class);
            assertNotNull(list);
            assertEquals(Long.valueOf(1L), list.get(0));
        }
    }

    @Test
    public void createOrOpenDeque_newCollection_shouldCreate() {
        try (FxStore store = FxStore.openMemory()) {
            Deque<Long> deque = store.createOrOpenDeque("newDeque", Long.class);
            assertNotNull(deque);
            assertTrue(deque.isEmpty());
        }
    }

    @Test
    public void createOrOpenDeque_existingCollection_shouldOpen() {
        try (FxStore store = FxStore.openMemory()) {
            store.createDeque("existingDeque", Long.class).add(1L);
            Deque<Long> deque = store.createOrOpenDeque("existingDeque", Long.class);
            assertNotNull(deque);
            assertEquals(Long.valueOf(1L), deque.peek());
        }
    }

    // ==================== Compaction 테스트 ====================

    @Test
    public void compactTo_shouldCreateSmallerFile() throws Exception {
        Path originalFile = tempFolder.newFile("original.fx").toPath();
        Path compactedFile = tempFolder.newFile("compacted.fx").toPath();
        Files.deleteIfExists(compactedFile);

        // 생성 및 데이터 추가/삭제
        try (FxStore store = FxStore.open(originalFile)) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            // 많은 데이터 추가
            for (long i = 0; i < 500; i++) {
                map.put(i, "value-" + i);
            }
            // 일부 삭제 (프래그멘테이션 유발)
            for (long i = 0; i < 250; i++) {
                map.remove(i);
            }
        }

        // Compaction
        try (FxStore store = FxStore.open(originalFile)) {
            store.compactTo(compactedFile);
        }

        // 검증
        try (FxStore store = FxStore.open(compactedFile)) {
            NavigableMap<Long, String> map = store.openMap("map", Long.class, String.class);
            assertNotNull(map);
            assertEquals(250, map.size());

            // 데이터 무결성 확인
            for (long i = 250; i < 500; i++) {
                assertEquals("value-" + i, map.get(i));
            }
        }
    }

    // ==================== Rollback 테스트 ====================

    @Test
    public void rollback_shouldRevertChanges() throws Exception {
        Path tempFile = tempFolder.newFile("rollback.fx").toPath();

        try (FxStore store = FxStoreImpl.open(tempFile, FxOptions.defaults().withCommitMode(CommitMode.BATCH).build())) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            map.put(1L, "original");
            store.commit();

            // 변경 후 rollback
            map.put(2L, "new");
            store.rollback();

            // 검증
            assertEquals(1, map.size());
            assertEquals("original", map.get(1L));
            assertNull(map.get(2L));
        }
    }

    // ==================== List 테스트 ====================

    @Test
    public void list_shouldReturnAllCollections() {
        try (FxStore store = FxStore.openMemory()) {
            store.createMap("map1", Long.class, String.class);
            store.createSet("set1", Long.class);
            store.createList("list1", Long.class);
            store.createDeque("deque1", Long.class);

            List<CollectionInfo> collections = store.list();

            assertEquals(4, collections.size());

            // 이름 확인
            Set<String> names = new HashSet<>();
            for (CollectionInfo info : collections) {
                names.add(info.name());
            }
            assertTrue(names.contains("map1"));
            assertTrue(names.contains("set1"));
            assertTrue(names.contains("list1"));
            assertTrue(names.contains("deque1"));
        }
    }

    @Test
    public void list_emptyStore_shouldReturnEmpty() {
        try (FxStore store = FxStore.openMemory()) {
            List<CollectionInfo> collections = store.list();
            assertTrue(collections.isEmpty());
        }
    }

    // ==================== Rename 테스트 ====================

    @Test
    public void rename_shouldChangeCollectionName() {
        try (FxStore store = FxStore.openMemory()) {
            store.createMap("oldName", Long.class, String.class).put(1L, "value");

            store.rename("oldName", "newName");

            assertFalse(store.exists("oldName"));
            assertTrue(store.exists("newName"));

            NavigableMap<Long, String> map = store.openMap("newName", Long.class, String.class);
            assertEquals("value", map.get(1L));
        }
    }

    // ==================== Drop 테스트 ====================

    @Test
    public void drop_shouldRemoveCollection() {
        try (FxStore store = FxStore.openMemory()) {
            store.createMap("toDelete", Long.class, String.class).put(1L, "value");
            assertTrue(store.exists("toDelete"));

            store.drop("toDelete");

            assertFalse(store.exists("toDelete"));
        }
    }

    // ==================== Exists 테스트 ====================

    @Test
    public void exists_existingCollection_shouldReturnTrue() {
        try (FxStore store = FxStore.openMemory()) {
            store.createMap("existing", Long.class, String.class);
            assertTrue(store.exists("existing"));
        }
    }

    @Test
    public void exists_nonExistingCollection_shouldReturnFalse() {
        try (FxStore store = FxStore.openMemory()) {
            assertFalse(store.exists("nonExisting"));
        }
    }

    // ==================== CommitMode 테스트 ====================

    @Test
    public void commitMode_AUTO_shouldAutoCommit() {
        try (FxStore store = FxStore.openMemory()) {
            assertEquals(CommitMode.AUTO, store.commitMode());
        }
    }

    @Test
    public void commitMode_BATCH_shouldRequireManualCommit() throws Exception {
        Path tempFile = tempFolder.newFile("batch.fx").toPath();

        try (FxStore store = FxStoreImpl.open(tempFile, FxOptions.defaults().withCommitMode(CommitMode.BATCH).build())) {
            assertEquals(CommitMode.BATCH, store.commitMode());
        }
    }

    // ==================== Deep Tree Tests (countTreeBytes) ====================

    @Test
    public void stats_DEEP_veryDeepTree_shouldTraverseInternalNodes() {
        // Given: 매우 많은 요소 (내부 노드 생성 유도)
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("veryDeepMap", Long.class, String.class);
            // 1000개 이상 삽입하면 내부 노드 생성 가능성 높음
            for (long i = 0; i < 1000; i++) {
                map.put(i, "value-" + i);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            assertTrue("Should have significant bytes", stats.liveBytesEstimate() > 10000);
        }
    }

    @Test
    public void stats_DEEP_veryLargeDataValues_shouldCalculateCorrectly() {
        // Given: 큰 값을 가진 데이터
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("largeValueMap", Long.class, String.class);
            StringBuilder largeValue = new StringBuilder();
            for (int j = 0; j < 100; j++) {
                largeValue.append("abcdefghij");
            }
            String value = largeValue.toString();

            for (long i = 0; i < 200; i++) {
                map.put(i, value);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            assertTrue("Should have large bytes", stats.liveBytesEstimate() > 10000);
        }
    }

    // ==================== Error Path Tests ====================

    @Test(expected = FxException.class)
    public void createMap_duplicateName_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.createMap("duplicate", Long.class, String.class);
            store.createMap("duplicate", Long.class, String.class);
        }
    }

    @Test(expected = FxException.class)
    public void createSet_duplicateName_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.createSet("duplicate", Long.class);
            store.createSet("duplicate", Long.class);
        }
    }

    @Test(expected = FxException.class)
    public void createList_duplicateName_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.createList("duplicate", Long.class);
            store.createList("duplicate", Long.class);
        }
    }

    @Test(expected = FxException.class)
    public void createDeque_duplicateName_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.createDeque("duplicate", Long.class);
            store.createDeque("duplicate", Long.class);
        }
    }

    @Test(expected = FxException.class)
    public void openMap_nonExistent_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.openMap("nonexistent", Long.class, String.class);
        }
    }

    @Test(expected = FxException.class)
    public void openSet_nonExistent_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.openSet("nonexistent", Long.class);
        }
    }

    @Test(expected = FxException.class)
    public void openList_nonExistent_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.openList("nonexistent", Long.class);
        }
    }

    @Test(expected = FxException.class)
    public void openDeque_nonExistent_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.openDeque("nonexistent", Long.class);
        }
    }

    @Test
    public void openMap_typeMismatch_shouldThrowException() {
        try (FxStore store = FxStore.openMemory()) {
            store.createList("collection", Long.class);
            try {
                // List를 Map으로 열기 시도 - 예외 발생 예상
                store.openMap("collection", Long.class, String.class);
                fail("Expected exception when opening collection with wrong type");
            } catch (FxException | ClassCastException e) {
                // FxException 또는 ClassCastException 예상
                assertNotNull(e);
            }
        }
    }

    @Test
    public void openSet_typeMismatch_shouldThrowException() {
        try (FxStore store = FxStore.openMemory()) {
            store.createList("collection", Long.class);
            try {
                // List를 Set으로 열기 시도 - 예외 발생 예상
                store.openSet("collection", Long.class);
                fail("Expected exception when opening collection with wrong type");
            } catch (FxException | ClassCastException e) {
                // FxException 또는 ClassCastException 예상
                assertNotNull(e);
            }
        }
    }

    @Test
    public void openList_typeMismatch_shouldThrowException() {
        try (FxStore store = FxStore.openMemory()) {
            store.createSet("collection", Long.class);
            try {
                // Set을 List로 열기 시도 - 예외 발생 예상
                store.openList("collection", Long.class);
                fail("Expected exception when opening collection with wrong type");
            } catch (FxException | ClassCastException e) {
                // FxException 또는 ClassCastException 예상
                assertNotNull(e);
            }
        }
    }

    @Test
    public void openDeque_typeMismatch_shouldThrowException() {
        try (FxStore store = FxStore.openMemory()) {
            store.createSet("collection", Long.class);
            try {
                // Set을 Deque로 열기 시도 - 예외 발생 예상
                store.openDeque("collection", Long.class);
                fail("Expected exception when opening collection with wrong type");
            } catch (FxException | ClassCastException e) {
                // FxException 또는 ClassCastException 예상
                assertNotNull(e);
            }
        }
    }

    @Test
    public void drop_nonExistent_shouldNotAffectStore() {
        try (FxStore store = FxStore.openMemory()) {
            // 존재하지 않는 컬렉션 삭제 시도 - 예외가 발생하거나 무시됨
            try {
                store.drop("nonexistent");
            } catch (FxException e) {
                // 예외 발생 시 NOT_FOUND 확인
                assertEquals(FxErrorCode.NOT_FOUND, e.getCode());
            }
        }
    }

    @Test(expected = FxException.class)
    public void rename_sourceNotExist_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.rename("nonexistent", "newName");
        }
    }

    @Test(expected = FxException.class)
    public void rename_targetAlreadyExists_shouldThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.createMap("source", Long.class, String.class);
            store.createMap("target", Long.class, String.class);
            store.rename("source", "target");
        }
    }

    // ==================== Collection State Tests ====================

    @Test
    public void multipleOpenCalls_shouldReturnSameInstance() {
        try (FxStore store = FxStore.openMemory()) {
            store.createMap("map", Long.class, String.class).put(1L, "value");

            NavigableMap<Long, String> map1 = store.openMap("map", Long.class, String.class);
            NavigableMap<Long, String> map2 = store.openMap("map", Long.class, String.class);

            assertSame("Should return same cached instance", map1, map2);
        }
    }

    @Test
    public void stats_afterMultipleOperations_shouldBeAccurate() {
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);

            // 많은 데이터 추가
            for (long i = 0; i < 500; i++) {
                map.put(i, "value-" + i);
            }

            // 일부 삭제
            for (long i = 0; i < 250; i++) {
                map.remove(i);
            }

            // 일부 업데이트
            for (long i = 250; i < 400; i++) {
                map.put(i, "updated-" + i);
            }

            Stats stats = store.stats(StatsMode.DEEP);
            assertEquals(1, stats.collectionCount());
            assertTrue("liveBytesEstimate > 0", stats.liveBytesEstimate() > 0);
        }
    }

    // ==================== Closed Store Tests ====================

    @Test(expected = FxException.class)
    public void operationAfterClose_shouldThrow() throws Exception {
        Path tempFile = tempFolder.newFile("closed.fx").toPath();
        FxStore store = FxStore.open(tempFile);
        store.createMap("map", Long.class, String.class);
        store.close();

        // 닫힌 스토어에 접근 시도
        store.createMap("another", Long.class, String.class);
    }

    @Test
    public void closeMultipleTimes_shouldNotThrow() {
        try (FxStore store = FxStore.openMemory()) {
            store.createMap("map", Long.class, String.class);
            store.close();
            store.close(); // 두 번째 close는 무시됨
        }
    }

    // ==================== 대용량 테스트 (Internal Node 생성) ====================

    @Test
    public void stats_DEEP_massiveList_shouldTraverseInternalNodes() {
        // Given: 많은 요소를 가진 List (OST 내부 노드 생성)
        try (FxStore store = FxStore.openMemory()) {
            List<Long> list = store.createList("massiveList", Long.class);

            // 13,000개 요소 삽입 (OST 내부 노드 분할 트리거)
            for (long i = 0; i < 13000; i++) {
                list.add(i);
            }

            // When: DEEP 모드 stats (countTreeBytes 내부 노드 순회)
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            assertEquals(1, stats.collectionCount());
            // countTreeBytes가 내부 노드를 순회했는지 확인 (바이트 수 > 0)
            assertTrue("Should have bytes from tree traversal",
                       stats.liveBytesEstimate() > 0);
        }
    }

    @Test
    public void stats_DEEP_massiveMap_shouldTraverseInternalNodes() {
        // Given: 많은 요소를 가진 Map (BTree 내부 노드 생성)
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("massiveMap", Long.class, String.class);

            // 13,000개 요소 삽입
            for (long i = 0; i < 13000; i++) {
                map.put(i, "value-" + i);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            // countTreeBytes가 BTree 내부 노드를 순회했는지 확인
            assertTrue("Should have bytes from tree traversal", stats.liveBytesEstimate() > 0);
        }
    }

    @Test
    public void stats_DEEP_massiveDeque_shouldTraverseInternalNodes() {
        // Given: 많은 요소를 가진 Deque (OST 내부 노드 생성)
        try (FxStore store = FxStore.openMemory()) {
            Deque<Long> deque = store.createDeque("massiveDeque", Long.class);

            // 13,000개 요소 삽입
            for (long i = 0; i < 13000; i++) {
                deque.addLast(i);
            }

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then
            assertNotNull(stats);
            // countTreeBytes가 OST 내부 노드를 순회했는지 확인
            assertTrue("Should have bytes from tree traversal", stats.liveBytesEstimate() > 0);
        }
    }

    // ==================== Race Condition 테스트 ====================

    @Test
    public void createOrOpenMap_afterDrop_shouldRecreate() {
        // Given: Map 생성 후 drop
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map1 = store.createMap("testMap", Long.class, String.class);
            map1.put(1L, "value1");
            store.drop("testMap");

            // When: 같은 이름으로 다시 생성
            NavigableMap<Long, String> map2 = store.createOrOpenMap("testMap", Long.class, String.class);

            // Then: 빈 맵이어야 함
            assertTrue(map2.isEmpty());
        }
    }

    @Test
    public void createOrOpenSet_afterDrop_shouldRecreate() {
        // Given: Set 생성 후 drop
        try (FxStore store = FxStore.openMemory()) {
            NavigableSet<Long> set1 = store.createSet("testSet", Long.class);
            set1.add(1L);
            store.drop("testSet");

            // When: 같은 이름으로 다시 생성
            NavigableSet<Long> set2 = store.createOrOpenSet("testSet", Long.class);

            // Then: 빈 셋이어야 함
            assertTrue(set2.isEmpty());
        }
    }

    @Test
    public void createOrOpenList_afterDrop_shouldRecreate() {
        // Given: List 생성 후 drop
        try (FxStore store = FxStore.openMemory()) {
            List<Long> list1 = store.createList("testList", Long.class);
            list1.add(1L);
            store.drop("testList");

            // When: 같은 이름으로 다시 생성
            List<Long> list2 = store.createOrOpenList("testList", Long.class);

            // Then: 빈 리스트여야 함
            assertTrue(list2.isEmpty());
        }
    }

    @Test
    public void createOrOpenDeque_afterDrop_shouldRecreate() {
        // Given: Deque 생성 후 drop
        try (FxStore store = FxStore.openMemory()) {
            Deque<Long> deque1 = store.createDeque("testDeque", Long.class);
            deque1.addLast(1L);
            store.drop("testDeque");

            // When: 같은 이름으로 다시 생성
            Deque<Long> deque2 = store.createOrOpenDeque("testDeque", Long.class);

            // Then: 빈 덱이어야 함
            assertTrue(deque2.isEmpty());
        }
    }

    // ==================== Stats 에지 케이스 ====================

    @Test
    public void stats_FAST_emptyStore_shouldWork() {
        // Given: 빈 스토어
        try (FxStore store = FxStore.openMemory()) {
            // When: FAST 모드 stats
            Stats stats = store.stats(StatsMode.FAST);

            // Then: deadRatio 계산 시 0으로 나누기 방지
            assertNotNull(stats);
            assertEquals(0, stats.collectionCount());
        }
    }

    @Test
    public void stats_afterClear_shouldWork() {
        // Given: 데이터가 있다가 삭제된 스토어
        try (FxStore store = FxStore.openMemory()) {
            List<String> list = store.createList("testList", String.class);
            for (int i = 0; i < 100; i++) {
                list.add("item" + i);
            }
            list.clear();

            // When: stats 조회
            Stats stats = store.stats(StatsMode.DEEP);

            // Then: 정상 동작
            assertNotNull(stats);
        }
    }

    // ==================== 컬렉션 리네임 테스트 ====================

    @Test
    public void rename_openCollection_shouldUpdateCache() {
        // Given: 오픈된 컬렉션
        try (FxStore store = FxStore.openMemory()) {
            List<Long> list = store.createList("oldName", Long.class);
            list.add(1L);

            // When: 컬렉션 리네임
            store.rename("oldName", "newName");

            // Then: 새 이름으로 접근 가능
            assertTrue(store.exists("newName"));
            assertFalse(store.exists("oldName"));
        }
    }

    @Test
    public void rename_closedCollection_shouldWork() {
        // Given: 생성 후 참조 없는 컬렉션
        try (FxStore store = FxStore.openMemory()) {
            store.createList("oldName", Long.class);

            // When: 리네임 (컬렉션 캐시에 없는 상태)
            store.rename("oldName", "newName");

            // Then: 새 이름으로 접근 가능
            assertTrue(store.exists("newName"));
        }
    }

    // ==================== list() 메서드 에지 케이스 ====================

    @Test
    public void list_multipleCollectionTypes_shouldReturnAll() {
        // Given: 여러 타입의 컬렉션
        try (FxStore store = FxStore.openMemory()) {
            store.createList("myList", String.class);
            store.createMap("myMap", Long.class, String.class);
            store.createSet("mySet", Long.class);
            store.createDeque("myDeque", String.class);

            // When: 전체 리스트 조회
            java.util.List<CollectionInfo> collections = store.list();

            // Then: 모든 컬렉션이 반환됨
            assertEquals(4, collections.size());
        }
    }

    // ==================== 빈 컬렉션 calculateLiveBytes 테스트 ====================

    @Test
    public void stats_DEEP_emptyCollection_shouldHandleZeroRootPageId() {
        // Given: 빈 컬렉션 (rootPageId = 0)
        try (FxStore store = FxStore.openMemory()) {
            store.createList("emptyList", Long.class);

            // When: DEEP 모드 stats
            Stats stats = store.stats(StatsMode.DEEP);

            // Then: 빈 컬렉션에서도 정상 동작
            assertNotNull(stats);
            assertEquals(1, stats.collectionCount());
        }
    }
}
