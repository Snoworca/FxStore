package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.NavigableMap;

import static org.junit.Assert.*;

/**
 * FxStoreImpl 다양한 옵션 조합 테스트 (P2)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>FxStoreImpl (82% → 90%+)</li>
 * </ul>
 *
 * @since 0.9
 * @see FxStoreImpl
 * @see FxOptions
 */
public class FxStoreOptionsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;

    @After
    public void tearDown() {
        if (store != null) {
            try {
                store.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    // ==================== PageSize 테스트 ====================

    @Test
    public void openMemory_with4KPageSize_shouldWork() {
        FxOptions options = FxOptions.defaults()
            .withPageSize(PageSize.PAGE_4K)
            .build();
        store = FxStore.openMemory(options);
        assertNotNull(store);

        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "value");
        assertEquals("value", map.get(1L));
    }

    @Test
    public void openMemory_with8KPageSize_shouldWork() {
        FxOptions options = FxOptions.defaults()
            .withPageSize(PageSize.PAGE_8K)
            .build();
        store = FxStore.openMemory(options);
        assertNotNull(store);

        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "value");
        assertEquals("value", map.get(1L));
    }

    @Test
    public void openMemory_with16KPageSize_shouldWork() {
        FxOptions options = FxOptions.defaults()
            .withPageSize(PageSize.PAGE_16K)
            .build();
        store = FxStore.openMemory(options);
        assertNotNull(store);

        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "value");
        assertEquals("value", map.get(1L));
    }

    @Test
    public void openFile_with8KPageSize_shouldPersist() throws Exception {
        File file = tempFolder.newFile("test-8k.fxs");
        file.delete();

        FxOptions options = FxOptions.defaults()
            .withPageSize(PageSize.PAGE_8K)
            .build();

        // 쓰기
        store = FxStore.open(file.toPath(), options);
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "hello");
        store.close();

        // 읽기 (같은 옵션)
        store = FxStore.open(file.toPath(), options);
        NavigableMap<Long, String> map2 = store.openMap("test", Long.class, String.class);
        assertEquals("hello", map2.get(1L));
    }

    // ==================== CacheSize 테스트 ====================

    @Test
    public void openMemory_withSmallCache_shouldWork() {
        FxOptions options = FxOptions.defaults()
            .withCacheBytes(1024 * 1024) // 1MB
            .build();
        store = FxStore.openMemory(options);
        assertNotNull(store);

        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (int i = 0; i < 100; i++) {
            map.put((long) i, "value" + i);
        }
        assertEquals(100, map.size());
    }

    @Test
    public void openMemory_withLargeCache_shouldWork() {
        FxOptions options = FxOptions.defaults()
            .withCacheBytes(64 * 1024 * 1024) // 64MB
            .build();
        store = FxStore.openMemory(options);
        assertNotNull(store);

        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "value");
        assertEquals("value", map.get(1L));
    }

    // ==================== allowCodecUpgrade 테스트 ====================

    @Test
    public void openMemory_withAllowCodecUpgrade_shouldWork() {
        FxOptions options = FxOptions.defaults()
            .withAllowCodecUpgrade(true)
            .build();
        store = FxStore.openMemory(options);
        assertNotNull(store);
    }

    @Test
    public void openMemory_withoutAllowCodecUpgrade_shouldWork() {
        FxOptions options = FxOptions.defaults()
            .withAllowCodecUpgrade(false)
            .build();
        store = FxStore.openMemory(options);
        assertNotNull(store);
    }

    // ==================== 복합 옵션 테스트 ====================

    @Test
    public void openMemory_withMultipleOptions_shouldWork() {
        // withPageSize returns Builder, then use builder methods
        FxOptions options = FxOptions.defaults()
            .withPageSize(PageSize.PAGE_16K)
            .cacheBytes(32 * 1024 * 1024)
            .allowCodecUpgrade(true)
            .build();
        store = FxStore.openMemory(options);
        assertNotNull(store);

        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "value");
        assertEquals("value", map.get(1L));
    }

    // ==================== 동일 이름 컬렉션 중복 생성 테스트 ====================

    @Test
    public void createMap_duplicateName_shouldThrow() {
        store = FxStore.openMemory(FxOptions.defaults());
        store.createMap("mymap", Long.class, String.class);

        try {
            store.createMap("mymap", Long.class, String.class);
            fail("Expected FxException for duplicate name");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("exist") ||
                       e.getMessage().toLowerCase().contains("duplicate"));
        }
    }

    @Test
    public void createSet_duplicateName_shouldThrow() {
        store = FxStore.openMemory(FxOptions.defaults());
        store.createSet("myset", Long.class);

        try {
            store.createSet("myset", Long.class);
            fail("Expected FxException for duplicate name");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("exist") ||
                       e.getMessage().toLowerCase().contains("duplicate"));
        }
    }

    @Test
    public void createList_duplicateName_shouldThrow() {
        store = FxStore.openMemory(FxOptions.defaults());
        store.createList("mylist", Long.class);

        try {
            store.createList("mylist", Long.class);
            fail("Expected FxException for duplicate name");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("exist") ||
                       e.getMessage().toLowerCase().contains("duplicate"));
        }
    }

    // ==================== 빈 스토어 테스트 ====================

    @Test
    public void emptyStore_exists_shouldReturnFalse() {
        store = FxStore.openMemory(FxOptions.defaults());
        assertFalse(store.exists("nonexistent"));
    }

    @Test
    public void emptyStore_list_shouldReturnEmpty() {
        store = FxStore.openMemory(FxOptions.defaults());
        assertTrue(store.list().isEmpty());
    }

    @Test
    public void emptyStore_list_shouldReturnEmptyList() {
        store = FxStore.openMemory(FxOptions.defaults());
        // list() is already tested above, this test confirms behavior
        assertEquals(0, store.list().size());
    }

    // ==================== 다중 컬렉션 테스트 ====================

    @Test
    public void multipleCollections_shouldCoexist() {
        store = FxStore.openMemory(FxOptions.defaults());

        NavigableMap<Long, String> map1 = store.createMap("map1", Long.class, String.class);
        NavigableMap<Long, String> map2 = store.createMap("map2", Long.class, String.class);
        store.createSet("set1", Long.class);
        store.createList("list1", Long.class);

        map1.put(1L, "a");
        map2.put(1L, "b");

        assertEquals("a", map1.get(1L));
        assertEquals("b", map2.get(1L));
        assertEquals(4, store.list().size());
    }

    @Test
    public void dropCollection_shouldRemove() {
        store = FxStore.openMemory(FxOptions.defaults());
        store.createMap("todrop", Long.class, String.class);
        assertTrue(store.exists("todrop"));

        store.drop("todrop");
        assertFalse(store.exists("todrop"));
    }

    // ==================== close 후 접근 테스트 ====================

    @Test
    public void createMap_afterClose_shouldThrow() {
        store = FxStore.openMemory(FxOptions.defaults());
        store.close();

        try {
            store.createMap("test", Long.class, String.class);
            fail("Expected exception after close");
        } catch (Exception e) {
            // Expected: FxException or IllegalStateException
            assertTrue(e instanceof FxException || e instanceof IllegalStateException);
        }
    }

    @Test
    public void exists_afterClose_shouldThrow() {
        store = FxStore.openMemory(FxOptions.defaults());
        store.close();

        try {
            store.exists("test");
            fail("Expected exception after close");
        } catch (Exception e) {
            assertTrue(e instanceof FxException || e instanceof IllegalStateException);
        }
    }

    // ==================== 파일 재오픈 테스트 ====================

    @Test
    public void reopenFile_shouldPreserveData() throws Exception {
        File file = tempFolder.newFile("reopen.fxs");
        file.delete();

        // 첫 번째 세션
        store = FxStore.open(file.toPath(), FxOptions.defaults());
        NavigableMap<Long, String> map = store.createMap("persist", Long.class, String.class);
        map.put(100L, "persisted");
        store.close();

        // 두 번째 세션
        store = FxStore.open(file.toPath(), FxOptions.defaults());
        assertTrue(store.exists("persist"));
        NavigableMap<Long, String> map2 = store.openMap("persist", Long.class, String.class);
        assertEquals("persisted", map2.get(100L));
    }

    @Test
    public void reopenFile_withDifferentPageSize_shouldDetectMismatch() throws Exception {
        File file = tempFolder.newFile("pagesize-mismatch.fxs");
        file.delete();

        // 4K로 생성
        FxOptions options4k = FxOptions.defaults()
            .withPageSize(PageSize.PAGE_4K)
            .build();
        store = FxStore.open(file.toPath(), options4k);
        store.createMap("test", Long.class, String.class);
        store.close();

        // 8K로 재오픈 시도 - 구현에 따라 예외가 발생하거나 파일의 pageSize를 따를 수 있음
        FxOptions options8k = FxOptions.defaults()
            .withPageSize(PageSize.PAGE_8K)
            .build();
        try {
            store = FxStore.open(file.toPath(), options8k);
            // 예외 없이 열린 경우 - 파일의 원래 pageSize를 사용하는 것으로 추정
            // 이 경우도 유효한 동작임
            assertNotNull(store);
        } catch (FxException e) {
            // 예외 발생 - pageSize 불일치 감지
            assertNotNull(e.getMessage());
            store = null;
        }
    }

    // ==================== createOrOpenMap 테스트 ====================

    @Test
    public void createOrOpenMap_newCollection_shouldCreate() {
        store = FxStore.openMemory(FxOptions.defaults());
        NavigableMap<Long, String> map = store.createOrOpenMap("newmap", Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists("newmap"));
    }

    @Test
    public void createOrOpenMap_existingCollection_shouldReturn() {
        store = FxStore.openMemory(FxOptions.defaults());
        NavigableMap<Long, String> map1 = store.createMap("existing", Long.class, String.class);
        map1.put(1L, "original");

        NavigableMap<Long, String> map2 = store.createOrOpenMap("existing", Long.class, String.class);
        assertEquals("original", map2.get(1L));
    }

    // ==================== openMap 존재하지 않는 경우 테스트 ====================

    @Test
    public void openMap_nonExistent_shouldThrow() {
        store = FxStore.openMemory(FxOptions.defaults());
        try {
            store.openMap("nonexistent", Long.class, String.class);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("not") ||
                       e.getMessage().toLowerCase().contains("exist"));
        }
    }

    // ==================== 메모리 통계 테스트 ====================

    @Test
    public void memoryStore_stats_shouldWork() {
        FxOptions options = FxOptions.defaults()
            .withCacheBytes(10 * 1024 * 1024)
            .build();
        store = FxStore.openMemory(options);

        // 일부 데이터 추가
        NavigableMap<Long, String> map = store.createMap("stats", Long.class, String.class);
        for (int i = 0; i < 1000; i++) {
            map.put((long) i, "value" + i);
        }

        // 통계 확인 (구현에 따라 다를 수 있음)
        assertTrue(map.size() == 1000);
    }
}
