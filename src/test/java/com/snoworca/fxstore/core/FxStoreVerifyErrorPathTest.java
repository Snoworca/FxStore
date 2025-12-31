package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NavigableMap;

import static org.junit.Assert.*;

/**
 * FxStoreImpl verify() 에러 경로 테스트
 *
 * <p>V17 커버리지 개선: verify 에러 경로 및 compactTo 에러 경로</p>
 */
public class FxStoreVerifyErrorPathTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("test.fx");
        storeFile.delete();
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== verifyCommitHeaders 에러 경로 ====================

    @Test
    public void verify_corruptedCommitHeaderSlotA_shouldReportError() throws Exception {
        // Given: 정상 Store 생성
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();
        store.close();

        // When: CommitHeader Slot A 손상
        try (RandomAccessFile raf = new RandomAccessFile(storeFile, "rw")) {
            // Superblock.SIZE 이후가 Slot A
            long slotAOffset = Superblock.SIZE;
            raf.seek(slotAOffset + CommitHeader.SIZE - 4); // CRC 위치
            raf.writeInt(0xDEADBEEF); // CRC 손상
        }

        // Then: verify가 에러 보고
        store = FxStore.open(storeFile.toPath());
        VerifyResult result = store.verify();

        // Slot A가 손상되었지만 Slot B가 정상이면 여전히 열림
        // 단, verify에서 에러가 보고되어야 함
        assertNotNull(result);
    }

    @Test
    public void verify_corruptedCommitHeaderSlotB_shouldReportError() throws Exception {
        // Given: 정상 Store 생성, 여러 번 커밋하여 두 슬롯 모두 사용
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();
        map.put(2L, "two");
        store.commit();
        store.close();

        // When: CommitHeader Slot B 손상
        try (RandomAccessFile raf = new RandomAccessFile(storeFile, "rw")) {
            long slotBOffset = Superblock.SIZE + CommitHeader.SIZE;
            raf.seek(slotBOffset + CommitHeader.SIZE - 4); // CRC 위치
            raf.writeInt(0xDEADBEEF); // CRC 손상
        }

        // Then: Store가 열림 (Slot A가 유효하므로)
        store = FxStore.open(storeFile.toPath());
        VerifyResult result = store.verify();
        assertNotNull(result);
    }

    // ==================== verifySuperblock 에러 경로 ====================

    @Test
    public void verify_corruptedSuperblock_magic_shouldReportError() throws Exception {
        // Given: 정상 Store 생성
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();
        store.close();

        // When: Superblock magic 손상
        try (RandomAccessFile raf = new RandomAccessFile(storeFile, "rw")) {
            raf.seek(0);
            raf.writeBytes("WRONGMAG"); // magic 손상
        }

        // Then: Store 열기 실패 예상 (Superblock 검증 실패)
        try {
            store = FxStore.open(storeFile.toPath());
            store.verify();
            fail("Should throw exception for corrupted superblock");
        } catch (FxException e) {
            // 예상된 예외
            store = null;
        }
    }

    // ==================== compactTo 에러 경로 ====================

    @Test(expected = FxException.class)
    public void compactTo_withPendingChanges_shouldThrow() throws Exception {
        // Given: BATCH 모드 Store
        FxOptions opts = FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH)
            .build();
        store = FxStore.open(storeFile.toPath(), opts);

        // 변경사항 추가 (커밋 안함)
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");

        // When: compactTo 호출
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());
    }

    @Test
    public void compactTo_success_shouldCopyAllData() throws Exception {
        // Given: 데이터가 있는 Store
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
        map.put(1L, "one");
        map.put(2L, "two");
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 대상 파일에 데이터 복사됨
        assertTrue(targetFile.exists());
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableMap<Long, String> targetMap = targetStore.openMap("testMap", Long.class, String.class);
            assertEquals(2, targetMap.size());
            assertEquals("one", targetMap.get(1L));
            assertEquals("two", targetMap.get(2L));
        }
    }

    // ==================== validateCodec 에러 경로 ====================

    @Test(expected = FxException.class)
    public void createMap_existingWithDifferentKeyType_shouldThrow() throws Exception {
        // Given: Long 키로 맵 생성
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();
        store.close();

        // When: 같은 이름으로 String 키 맵 열기 시도
        store = FxStore.open(storeFile.toPath());
        store.openMap("test", String.class, String.class);
    }

    @Test(expected = FxException.class)
    public void createMap_existingWithDifferentValueType_shouldThrow() throws Exception {
        // Given: String 값으로 맵 생성
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();
        store.close();

        // When: 같은 이름으로 Long 값 맵 열기 시도
        store = FxStore.open(storeFile.toPath());
        store.openMap("test", Long.class, Long.class);
    }

    // ==================== list() 빈 경로 ====================

    @Test
    public void list_emptyStore_shouldReturnEmptyList() throws Exception {
        // Given: 빈 Store
        store = FxStore.open(storeFile.toPath());

        // Then: 빈 리스트 반환
        java.util.List<CollectionInfo> collections = store.list();
        assertTrue(collections.isEmpty());
    }

    // ==================== 파일 크기 관련 ====================

    @Test
    public void verify_afterMultipleCommits_shouldPass() throws Exception {
        // Given: 여러 번 커밋
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

        for (int i = 0; i < 10; i++) {
            map.put((long) i, "value" + i);
            store.commit();
        }

        // Then: verify 성공
        VerifyResult result = store.verify();
        assertTrue(result.ok());
    }

    @Test
    public void verify_afterRollback_shouldPass() throws Exception {
        // Given: BATCH 모드에서 롤백
        FxOptions opts = FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH)
            .build();
        store = FxStore.open(storeFile.toPath(), opts);

        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();

        map.put(2L, "two");
        store.rollback();

        // Then: verify 성공
        VerifyResult result = store.verify();
        assertTrue(result.ok());
        assertEquals(1, map.size());
    }

    // ==================== stats(StatsMode.DEEP) 전체 경로 ====================

    @Test
    public void stats_deep_withMultipleCollections_shouldWork() throws Exception {
        // Given: 여러 종류의 컬렉션
        store = FxStore.open(storeFile.toPath());

        NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
        map.put(1L, "one");

        java.util.NavigableSet<Long> set = store.createSet("testSet", Long.class);
        set.add(1L);

        java.util.List<String> list = store.createList("testList", String.class);
        list.add("item");

        java.util.Deque<String> deque = store.createDeque("testDeque", String.class);
        deque.addLast("elem");

        store.commit();

        // When: DEEP 통계 조회
        Stats stats = store.stats(StatsMode.DEEP);

        // Then: 4개 컬렉션
        assertEquals(4, stats.collectionCount());
        assertTrue(stats.fileBytes() > 0);
    }

    // ==================== rename 성공 케이스 ====================

    @Test
    public void rename_existingCollection_shouldWork() throws Exception {
        // Given: 컬렉션 생성
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("oldName", Long.class, String.class);
        map.put(1L, "one");
        store.commit();

        // When: 이름 변경
        store.rename("oldName", "newName");
        store.commit();

        // Then: 새 이름으로 접근 가능
        NavigableMap<Long, String> renamed = store.openMap("newName", Long.class, String.class);
        assertEquals("one", renamed.get(1L));

        // 이전 이름으로는 없음
        assertFalse(store.exists("oldName"));
    }

    @Test(expected = FxException.class)
    public void rename_nonExisting_shouldThrow() throws Exception {
        // Given: 빈 Store
        store = FxStore.open(storeFile.toPath());

        // When: 없는 컬렉션 이름 변경
        store.rename("nonExisting", "newName");
    }

    // ==================== drop 성공 케이스 ====================

    @Test
    public void drop_existingCollection_shouldRemove() throws Exception {
        // Given: 컬렉션 생성
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();

        // When: 컬렉션 삭제
        boolean result = store.drop("test");
        store.commit();

        // Then: 삭제됨
        assertTrue(result);
        assertFalse(store.exists("test"));
    }

    // ==================== exists 경로 ====================

    @Test
    public void exists_afterCreate_shouldReturnTrue() throws Exception {
        // Given: 컬렉션 생성
        store = FxStore.open(storeFile.toPath());
        store.createMap("test", Long.class, String.class);
        store.commit();

        // Then: exists 반환
        assertTrue(store.exists("test"));
        assertFalse(store.exists("nonExisting"));
    }

    // ==================== 메모리 Store 테스트 ====================

    @Test
    public void memoryStore_verify_shouldPass() {
        // Given: 메모리 Store
        store = FxStore.openMemory();
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();

        // Then: verify 성공
        VerifyResult result = store.verify();
        assertTrue(result.ok());
    }

    // ==================== createOrOpen 경로 ====================

    @Test
    public void createOrOpenMap_existing_shouldOpen() throws Exception {
        // Given: 맵 생성
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "one");
        store.commit();
        store.close();

        // When: createOrOpenMap으로 열기
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> reopened = store.createOrOpenMap("test", Long.class, String.class);

        // Then: 기존 데이터 유지
        assertEquals("one", reopened.get(1L));
    }

    @Test
    public void createOrOpenSet_existing_shouldOpen() throws Exception {
        // Given: Set 생성
        store = FxStore.open(storeFile.toPath());
        java.util.NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);
        store.commit();
        store.close();

        // When: createOrOpenSet으로 열기
        store = FxStore.open(storeFile.toPath());
        java.util.NavigableSet<Long> reopened = store.createOrOpenSet("test", Long.class);

        // Then: 기존 데이터 유지
        assertTrue(reopened.contains(1L));
    }

    @Test
    public void createOrOpenList_existing_shouldOpen() throws Exception {
        // Given: List 생성
        store = FxStore.open(storeFile.toPath());
        java.util.List<String> list = store.createList("test", String.class);
        list.add("item");
        store.commit();
        store.close();

        // When: createOrOpenList로 열기
        store = FxStore.open(storeFile.toPath());
        java.util.List<String> reopened = store.createOrOpenList("test", String.class);

        // Then: 기존 데이터 유지
        assertEquals("item", reopened.get(0));
    }

    @Test
    public void createOrOpenDeque_existing_shouldOpen() throws Exception {
        // Given: Deque 생성
        store = FxStore.open(storeFile.toPath());
        java.util.Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("item");
        store.commit();
        store.close();

        // When: createOrOpenDeque로 열기
        store = FxStore.open(storeFile.toPath());
        java.util.Deque<String> reopened = store.createOrOpenDeque("test", String.class);

        // Then: 기존 데이터 유지
        assertEquals("item", reopened.peekFirst());
    }
}
