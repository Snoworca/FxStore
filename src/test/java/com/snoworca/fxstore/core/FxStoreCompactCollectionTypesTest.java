package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * compactTo() 컬렉션 타입별 테스트
 *
 * <p>P0-3, P0-4, P0-5 해결: copySet(), copyList(), copyDeque()</p>
 *
 * <p>기존 compactTo 테스트는 Map만 포함했으므로, Set/List/Deque 복사 경로를 테스트합니다.</p>
 *
 * @since v1.0 Phase 3
 * @see FxStore#compactTo(java.nio.file.Path)
 */
public class FxStoreCompactCollectionTypesTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File sourceFile;
    private File targetFile;

    @Before
    public void setUp() throws Exception {
        sourceFile = tempFolder.newFile("source.fx");
        sourceFile.delete();

        targetFile = tempFolder.newFile("target.fx");
        targetFile.delete();
    }

    @After
    public void tearDown() {
        // TemporaryFolder가 처리
    }

    // ==================== Set 복사 테스트 ====================

    @Test
    public void compactTo_withSetOnly_shouldCopySet() throws Exception {
        // Given: Set만 있는 스토어
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            NavigableSet<String> set = source.createSet("users", String.class);
            set.add("alice");
            set.add("bob");
            set.add("charlie");

            // When: compactTo(newFile)
            source.compactTo(targetFile.toPath());
        }

        // Then: 새 파일에서 Set 데이터 검증
        assertTrue("Target file should exist", targetFile.exists());

        try (FxStore target = FxStore.open(targetFile.toPath())) {
            assertTrue("Collection 'users' should exist", target.exists("users"));

            NavigableSet<String> copiedSet = target.openSet("users", String.class);
            assertNotNull(copiedSet);
            assertEquals("Should have 3 elements", 3, copiedSet.size());
            assertTrue("Should contain alice", copiedSet.contains("alice"));
            assertTrue("Should contain bob", copiedSet.contains("bob"));
            assertTrue("Should contain charlie", copiedSet.contains("charlie"));

            // 정렬 순서 유지
            assertEquals("First should be alice", "alice", copiedSet.first());
            assertEquals("Last should be charlie", "charlie", copiedSet.last());
        }
    }

    // ==================== List 복사 테스트 ====================

    @Test
    public void compactTo_withListOnly_shouldCopyList() throws Exception {
        // Given: List만 있는 스토어
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            List<Long> list = source.createList("logs", Long.class);
            for (long i = 1; i <= 5; i++) {
                list.add(i);
            }

            // When: compactTo(newFile)
            source.compactTo(targetFile.toPath());
        }

        // Then: 새 파일에서 List 순서 및 데이터 검증
        assertTrue("Target file should exist", targetFile.exists());

        try (FxStore target = FxStore.open(targetFile.toPath())) {
            assertTrue("Collection 'logs' should exist", target.exists("logs"));

            List<Long> copiedList = target.openList("logs", Long.class);
            assertNotNull(copiedList);
            assertEquals("Should have 5 elements", 5, copiedList.size());

            // 인덱스 순서 유지
            assertEquals("Index 0 should be 1", Long.valueOf(1), copiedList.get(0));
            assertEquals("Index 1 should be 2", Long.valueOf(2), copiedList.get(1));
            assertEquals("Index 2 should be 3", Long.valueOf(3), copiedList.get(2));
            assertEquals("Index 3 should be 4", Long.valueOf(4), copiedList.get(3));
            assertEquals("Index 4 should be 5", Long.valueOf(5), copiedList.get(4));
        }
    }

    // ==================== Deque 복사 테스트 ====================

    @Test
    public void compactTo_withDequeOnly_shouldCopyDeque() throws Exception {
        // Given: Deque만 있는 스토어
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            Deque<String> deque = source.createDeque("queue", String.class);
            deque.addFirst("A");
            deque.addLast("Z");
            deque.addFirst("B"); // 순서: B, A, Z

            // When: compactTo(newFile)
            source.compactTo(targetFile.toPath());
        }

        // Then: 새 파일에서 Deque 순서 검증
        assertTrue("Target file should exist", targetFile.exists());

        try (FxStore target = FxStore.open(targetFile.toPath())) {
            assertTrue("Collection 'queue' should exist", target.exists("queue"));

            Deque<String> copiedDeque = target.openDeque("queue", String.class);
            assertNotNull(copiedDeque);
            assertEquals("Should have 3 elements", 3, copiedDeque.size());

            // 순서 검증: B, A, Z
            assertEquals("First should be B", "B", copiedDeque.peekFirst());
            assertEquals("Last should be Z", "Z", copiedDeque.peekLast());
        }
    }

    // ==================== 모든 컬렉션 타입 혼합 테스트 ====================

    @Test
    public void compactTo_withAllCollectionTypes_shouldCopyAll() throws Exception {
        // Given: Map, Set, List, Deque 모두 포함
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            // Map
            NavigableMap<Long, String> map = source.createMap("map", Long.class, String.class);
            map.put(1L, "one");
            map.put(2L, "two");

            // Set
            NavigableSet<String> set = source.createSet("set", String.class);
            set.add("x");
            set.add("y");
            set.add("z");

            // List
            List<String> list = source.createList("list", String.class);
            list.add("first");
            list.add("second");

            // Deque
            Deque<Long> deque = source.createDeque("deque", Long.class);
            deque.addLast(100L);
            deque.addLast(200L);

            // When: compactTo(newFile)
            source.compactTo(targetFile.toPath());
        }

        // Then: 모든 컬렉션 데이터 검증
        assertTrue("Target file should exist", targetFile.exists());

        try (FxStore target = FxStore.open(targetFile.toPath())) {
            // Map 검증
            assertTrue("Map should exist", target.exists("map"));
            NavigableMap<Long, String> copiedMap = target.openMap("map", Long.class, String.class);
            assertEquals(2, copiedMap.size());
            assertEquals("one", copiedMap.get(1L));
            assertEquals("two", copiedMap.get(2L));

            // Set 검증
            assertTrue("Set should exist", target.exists("set"));
            NavigableSet<String> copiedSet = target.openSet("set", String.class);
            assertEquals(3, copiedSet.size());
            assertTrue(copiedSet.contains("x"));

            // List 검증
            assertTrue("List should exist", target.exists("list"));
            List<String> copiedList = target.openList("list", String.class);
            assertEquals(2, copiedList.size());
            assertEquals("first", copiedList.get(0));

            // Deque 검증
            assertTrue("Deque should exist", target.exists("deque"));
            Deque<Long> copiedDeque = target.openDeque("deque", Long.class);
            assertEquals(2, copiedDeque.size());
            assertEquals(Long.valueOf(100), copiedDeque.peekFirst());
        }
    }

    // ==================== 빈 컬렉션 테스트 ====================

    @Test
    public void compactTo_withEmptyCollections_shouldCopyEmpty() throws Exception {
        // Given: 빈 Set, List, Deque
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            source.createSet("emptySet", String.class);
            source.createList("emptyList", String.class);
            source.createDeque("emptyDeque", String.class);

            // When: compactTo(newFile)
            source.compactTo(targetFile.toPath());
        }

        // Then: 빈 컬렉션으로 복사됨
        assertTrue("Target file should exist", targetFile.exists());

        try (FxStore target = FxStore.open(targetFile.toPath())) {
            assertTrue("emptySet should exist", target.exists("emptySet"));
            assertTrue("emptyList should exist", target.exists("emptyList"));
            assertTrue("emptyDeque should exist", target.exists("emptyDeque"));

            NavigableSet<String> copiedSet = target.openSet("emptySet", String.class);
            assertTrue("Set should be empty", copiedSet.isEmpty());

            List<String> copiedList = target.openList("emptyList", String.class);
            assertTrue("List should be empty", copiedList.isEmpty());

            Deque<String> copiedDeque = target.openDeque("emptyDeque", String.class);
            assertTrue("Deque should be empty", copiedDeque.isEmpty());
        }
    }

    // ==================== 대용량 Set 테스트 ====================

    @Test
    public void compactTo_withLargeSet_shouldPreserveOrder() throws Exception {
        // Given: 100개 요소의 Set (큰 세트 테스트는 별도의 통합 테스트에서)
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            NavigableSet<Long> set = source.createSet("largeSet", Long.class);
            for (long i = 0; i < 100; i++) {
                set.add(i);
            }

            // When: compactTo(newFile)
            source.compactTo(targetFile.toPath());
        }

        // Then: 정렬 순서 보존
        assertTrue("Target file should exist", targetFile.exists());

        try (FxStore target = FxStore.open(targetFile.toPath())) {
            NavigableSet<Long> copiedSet = target.openSet("largeSet", Long.class);
            assertEquals("Should have 100 elements", 100, copiedSet.size());
            assertEquals("First should be 0", Long.valueOf(0), copiedSet.first());
            assertEquals("Last should be 99", Long.valueOf(99), copiedSet.last());

            // 순서 확인
            long expected = 0;
            for (Long value : copiedSet) {
                assertEquals("Order should be preserved", Long.valueOf(expected), value);
                expected++;
            }
        }
    }

    // ==================== 대용량 List 테스트 ====================

    @Test
    public void compactTo_withLargeList_shouldPreserveIndexOrder() throws Exception {
        // Given: 500개 요소의 List
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            List<String> list = source.createList("largeList", String.class);
            for (int i = 0; i < 500; i++) {
                list.add("item-" + i);
            }

            // When: compactTo(newFile)
            source.compactTo(targetFile.toPath());
        }

        // Then: 인덱스 순서 보존
        assertTrue("Target file should exist", targetFile.exists());

        try (FxStore target = FxStore.open(targetFile.toPath())) {
            List<String> copiedList = target.openList("largeList", String.class);
            assertEquals("Should have 500 elements", 500, copiedList.size());

            // 인덱스별 검증
            for (int i = 0; i < 500; i++) {
                assertEquals("Index " + i + " should match", "item-" + i, copiedList.get(i));
            }
        }
    }

    // ==================== 대용량 Deque 테스트 ====================

    @Test
    public void compactTo_withLargeDeque_shouldPreserveOrder() throws Exception {
        // Given: 500개 요소의 Deque
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            Deque<Long> deque = source.createDeque("largeDeque", Long.class);
            for (long i = 0; i < 500; i++) {
                deque.addLast(i);
            }

            // When: compactTo(newFile)
            source.compactTo(targetFile.toPath());
        }

        // Then: 순서 보존
        assertTrue("Target file should exist", targetFile.exists());

        try (FxStore target = FxStore.open(targetFile.toPath())) {
            Deque<Long> copiedDeque = target.openDeque("largeDeque", Long.class);
            assertEquals("Should have 500 elements", 500, copiedDeque.size());
            assertEquals("First should be 0", Long.valueOf(0), copiedDeque.peekFirst());
            assertEquals("Last should be 499", Long.valueOf(499), copiedDeque.peekLast());
        }
    }

    // ==================== 메모리 스토어 → 파일 테스트 ====================

    @Test
    public void compactTo_memoryStoreWithSet_shouldCopyToFile() throws Exception {
        // Given: 메모리 스토어에 Set
        try (FxStore source = FxStore.openMemory()) {
            NavigableSet<String> set = source.createSet("memorySet", String.class);
            set.add("a");
            set.add("b");
            set.add("c");

            // When: compactTo(newFile)
            source.compactTo(targetFile.toPath());
        }

        // Then: 파일에 복사됨
        assertTrue("Target file should exist", targetFile.exists());

        try (FxStore target = FxStore.open(targetFile.toPath())) {
            NavigableSet<String> copiedSet = target.openSet("memorySet", String.class);
            assertEquals(3, copiedSet.size());
            assertTrue(copiedSet.contains("a"));
            assertTrue(copiedSet.contains("b"));
            assertTrue(copiedSet.contains("c"));
        }
    }
}
