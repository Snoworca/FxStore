package com.snoworca.fxstore.integration;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Verify 통합 테스트.
 *
 * <p>목적: verifyCommitHeaders, verifySuperblock, verifyCatalogState 커버리지 향상</p>
 *
 * @see com.snoworca.fxstore.core.FxStoreImpl#verify()
 */
public class VerifyIntegrationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore memoryStore;

    @Before
    public void setUp() {
        memoryStore = FxStore.openMemory();
    }

    @After
    public void tearDown() {
        if (memoryStore != null) {
            memoryStore.close();
        }
    }

    /**
     * 메모리 스토어 verify.
     */
    @Test
    public void testVerify_memoryStore() {
        NavigableMap<Long, String> map = memoryStore.createMap("map", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "value" + i);
        }

        VerifyResult result = memoryStore.verify();
        assertTrue("Memory store should verify ok", result.ok());
        assertTrue("Errors should be empty", result.errors().isEmpty());
    }

    /**
     * 빈 메모리 스토어 verify.
     */
    @Test
    public void testVerify_emptyMemoryStore() {
        VerifyResult result = memoryStore.verify();
        assertTrue("Empty memory store should verify ok", result.ok());
        assertTrue("Errors should be empty", result.errors().isEmpty());
    }

    /**
     * 정상 파일 Store verify.
     */
    @Test
    public void testVerify_normalFileStore() throws Exception {
        Path tempFile = tempFolder.newFile("normal.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            for (long i = 0; i < 500; i++) {
                map.put(i, "value" + i);
            }

            VerifyResult result = store.verify();
            assertTrue("Normal store should verify ok", result.ok());
        }
    }

    /**
     * 재오픈 후 verify.
     */
    @Test
    public void testVerify_afterReopen() throws Exception {
        Path tempFile = tempFolder.newFile("reopen.fx").toPath();

        // 생성 및 데이터 저장
        try (FxStore store = FxStore.open(tempFile)) {
            store.createMap("map", Long.class, String.class).put(1L, "value");
        }

        // 재오픈 후 verify
        try (FxStore store = FxStore.open(tempFile)) {
            VerifyResult result = store.verify();
            assertTrue("Reopened store should verify ok", result.ok());
            assertTrue("Errors should be empty", result.errors().isEmpty());
        }
    }

    /**
     * BATCH 모드에서 verify.
     */
    @Test
    public void testVerify_batchMode() throws Exception {
        Path tempFile = tempFolder.newFile("batch.fx").toPath();
        FxOptions options = FxOptions.defaults().withCommitMode(CommitMode.BATCH).build();

        try (FxStore store = FxStore.open(tempFile, options)) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            map.put(1L, "value");
            store.commit();

            VerifyResult result = store.verify();
            assertTrue("Batch mode store should verify ok", result.ok());
        }
    }

    /**
     * 다중 컬렉션 verify.
     */
    @Test
    public void testVerify_multipleCollections() throws Exception {
        Path tempFile = tempFolder.newFile("multi.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            store.createMap("map", Long.class, String.class);
            store.createSet("set", Long.class);
            store.createList("list", String.class);
            store.createDeque("deque", Integer.class);

            VerifyResult result = store.verify();
            assertTrue("Multiple collections should verify ok", result.ok());
        }
    }

    /**
     * 대용량 데이터 verify.
     */
    @Test
    public void testVerify_largeData() throws Exception {
        Path tempFile = tempFolder.newFile("large.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            NavigableMap<Long, String> map = store.createMap("largeMap", Long.class, String.class);
            for (long i = 0; i < 5000; i++) {
                map.put(i, "value" + i);
            }

            VerifyResult result = store.verify();
            assertTrue("Large data store should verify ok", result.ok());
        }
    }

    /**
     * List 컬렉션 verify.
     */
    @Test
    public void testVerify_listCollection() throws Exception {
        Path tempFile = tempFolder.newFile("list.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            List<Long> list = store.createList("list", Long.class);
            for (long i = 0; i < 1000; i++) {
                list.add(i);
            }

            VerifyResult result = store.verify();
            assertTrue("List store should verify ok", result.ok());
        }
    }

    /**
     * Deque 컬렉션 verify.
     */
    @Test
    public void testVerify_dequeCollection() throws Exception {
        Path tempFile = tempFolder.newFile("deque.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            Deque<Long> deque = store.createDeque("deque", Long.class);
            for (long i = 0; i < 500; i++) {
                deque.addLast(i);
            }

            VerifyResult result = store.verify();
            assertTrue("Deque store should verify ok", result.ok());
        }
    }

    /**
     * 다중 커밋 후 verify.
     */
    @Test
    public void testVerify_afterMultipleCommits() throws Exception {
        Path tempFile = tempFolder.newFile("multi-commit.fx").toPath();
        FxOptions options = FxOptions.defaults().withCommitMode(CommitMode.BATCH).build();

        try (FxStore store = FxStore.open(tempFile, options)) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);

            // 다중 커밋
            for (int batch = 0; batch < 10; batch++) {
                for (long i = 0; i < 100; i++) {
                    map.put(batch * 100 + i, "value" + (batch * 100 + i));
                }
                store.commit();

                VerifyResult result = store.verify();
                assertTrue("Store should verify ok after commit " + batch, result.ok());
            }
        }
    }

    /**
     * 삽입/삭제 후 verify.
     */
    @Test
    public void testVerify_afterInsertAndDelete() throws Exception {
        Path tempFile = tempFolder.newFile("insert-delete.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);

            // 삽입
            for (long i = 0; i < 500; i++) {
                map.put(i, "value" + i);
            }

            VerifyResult result1 = store.verify();
            assertTrue("Store should verify ok after insert", result1.ok());

            // 삭제
            for (long i = 0; i < 250; i++) {
                map.remove(i);
            }

            VerifyResult result2 = store.verify();
            assertTrue("Store should verify ok after delete", result2.ok());
        }
    }

    /**
     * 빈 파일 스토어 verify.
     */
    @Test
    public void testVerify_emptyFileStore() throws Exception {
        Path tempFile = tempFolder.newFile("empty.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            VerifyResult result = store.verify();
            assertTrue("Empty file store should verify ok", result.ok());
        }
    }

    /**
     * verify 후 데이터 접근.
     */
    @Test
    public void testVerify_thenDataAccess() throws Exception {
        Path tempFile = tempFolder.newFile("verify-access.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "value" + i);
            }

            // verify
            VerifyResult result = store.verify();
            assertTrue(result.ok());

            // verify 후 데이터 접근 가능
            assertEquals("value50", map.get(50L));
            assertEquals(100, map.size());
        }
    }

    /**
     * Set 컬렉션 verify.
     */
    @Test
    public void testVerify_setCollection() throws Exception {
        Path tempFile = tempFolder.newFile("set.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            NavigableSet<Long> set = store.createSet("set", Long.class);
            for (long i = 0; i < 1000; i++) {
                set.add(i);
            }

            VerifyResult result = store.verify();
            assertTrue("Set store should verify ok", result.ok());
        }
    }

    /**
     * 모든 컬렉션 타입 조합 verify.
     */
    @Test
    public void testVerify_allCollectionTypesCombined() throws Exception {
        Path tempFile = tempFolder.newFile("all-types.fx").toPath();

        try (FxStore store = FxStore.open(tempFile)) {
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            NavigableSet<Long> set = store.createSet("set", Long.class);
            List<Long> list = store.createList("list", Long.class);
            Deque<Long> deque = store.createDeque("deque", Long.class);

            // 각 컬렉션에 데이터 추가
            for (long i = 0; i < 200; i++) {
                map.put(i, "v" + i);
                set.add(i);
                list.add(i);
                deque.add(i);
            }

            VerifyResult result = store.verify();
            assertTrue("All types store should verify ok", result.ok());
        }

        // 재오픈 후 verify
        try (FxStore store = FxStore.open(tempFile)) {
            VerifyResult result = store.verify();
            assertTrue("Reopened all types store should verify ok", result.ok());
        }
    }
}
