package com.snoworca.fxstore.btree;

import com.snoworca.fxstore.storage.MemoryStorage;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * BTree Stateless API 테스트 (Phase 8 - Week 2)
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>findWithRoot() - 지정된 root에서 검색</li>
 *   <li>insertWithRoot() - 지정된 root에서 삽입</li>
 *   <li>deleteWithRoot() - 지정된 root에서 삭제</li>
 *   <li>COW 불변성 검증</li>
 * </ul>
 */
public class BTreeStatelessTest {

    private static final int PAGE_SIZE = 4096;

    private MemoryStorage storage;
    private BTree btree;
    private Comparator<byte[]> comparator;

    @Before
    public void setUp() {
        storage = new MemoryStorage(1024 * 1024); // 1MB
        comparator = (a, b) -> {
            int len = Math.min(a.length, b.length);
            for (int i = 0; i < len; i++) {
                int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
                if (cmp != 0) return cmp;
            }
            return a.length - b.length;
        };
        btree = new BTree(storage, PAGE_SIZE, comparator);
    }

    // ==================== findWithRoot 테스트 ====================

    @Test
    public void testFindWithRoot_EmptyTree() {
        Long result = btree.findWithRoot(0, "key".getBytes(StandardCharsets.UTF_8));
        assertNull("Empty tree should return null", result);
    }

    @Test
    public void testFindWithRoot_BasicFind() {
        // 기존 API로 데이터 삽입
        btree.insert("key1".getBytes(StandardCharsets.UTF_8), 100L);
        btree.insert("key2".getBytes(StandardCharsets.UTF_8), 200L);
        btree.insert("key3".getBytes(StandardCharsets.UTF_8), 300L);

        long rootPageId = btree.getRootPageId();

        // Stateless API로 검색
        assertEquals(Long.valueOf(100L), btree.findWithRoot(rootPageId, "key1".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Long.valueOf(200L), btree.findWithRoot(rootPageId, "key2".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Long.valueOf(300L), btree.findWithRoot(rootPageId, "key3".getBytes(StandardCharsets.UTF_8)));
        assertNull(btree.findWithRoot(rootPageId, "key4".getBytes(StandardCharsets.UTF_8)));
    }

    @Test(expected = NullPointerException.class)
    public void testFindWithRoot_NullKey() {
        btree.findWithRoot(0, null);
    }

    // ==================== insertWithRoot 테스트 ====================

    @Test
    public void testInsertWithRoot_FirstInsert() {
        BTree.StatelessInsertResult result = btree.insertWithRoot(0,
                "key1".getBytes(StandardCharsets.UTF_8), 100L);

        assertTrue("New root should be non-zero", result.newRootPageId > 0);
        assertFalse("First insert is not a replace", result.replaced);

        // 검증
        assertEquals(Long.valueOf(100L), btree.findWithRoot(result.newRootPageId,
                "key1".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testInsertWithRoot_MultipleInserts() {
        // 첫 번째 삽입
        BTree.StatelessInsertResult r1 = btree.insertWithRoot(0,
                "key1".getBytes(StandardCharsets.UTF_8), 100L);

        // 두 번째 삽입 (이전 root 사용)
        BTree.StatelessInsertResult r2 = btree.insertWithRoot(r1.newRootPageId,
                "key2".getBytes(StandardCharsets.UTF_8), 200L);

        // 세 번째 삽입
        BTree.StatelessInsertResult r3 = btree.insertWithRoot(r2.newRootPageId,
                "key3".getBytes(StandardCharsets.UTF_8), 300L);

        // 최종 root에서 모든 키 검색 가능
        assertEquals(Long.valueOf(100L), btree.findWithRoot(r3.newRootPageId,
                "key1".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Long.valueOf(200L), btree.findWithRoot(r3.newRootPageId,
                "key2".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Long.valueOf(300L), btree.findWithRoot(r3.newRootPageId,
                "key3".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testInsertWithRoot_COW_OldRootUnchanged() {
        // 첫 번째 삽입
        BTree.StatelessInsertResult r1 = btree.insertWithRoot(0,
                "key1".getBytes(StandardCharsets.UTF_8), 100L);
        long oldRoot = r1.newRootPageId;

        // 두 번째 삽입
        BTree.StatelessInsertResult r2 = btree.insertWithRoot(oldRoot,
                "key2".getBytes(StandardCharsets.UTF_8), 200L);
        long newRoot = r2.newRootPageId;

        // COW: 이전 root는 여전히 key1만 가지고 있어야 함
        assertEquals(Long.valueOf(100L), btree.findWithRoot(oldRoot,
                "key1".getBytes(StandardCharsets.UTF_8)));
        assertNull("Old root should not have key2",
                btree.findWithRoot(oldRoot, "key2".getBytes(StandardCharsets.UTF_8)));

        // 새 root는 key1, key2 모두 있음
        assertEquals(Long.valueOf(100L), btree.findWithRoot(newRoot,
                "key1".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Long.valueOf(200L), btree.findWithRoot(newRoot,
                "key2".getBytes(StandardCharsets.UTF_8)));
    }

    @Test(expected = NullPointerException.class)
    public void testInsertWithRoot_NullKey() {
        btree.insertWithRoot(0, null, 100L);
    }

    // ==================== deleteWithRoot 테스트 ====================

    @Test
    public void testDeleteWithRoot_EmptyTree() {
        BTree.StatelessDeleteResult result = btree.deleteWithRoot(0,
                "key1".getBytes(StandardCharsets.UTF_8));

        assertEquals("Empty tree delete should return 0", 0, result.newRootPageId);
        assertFalse("Nothing deleted", result.deleted);
    }

    @Test
    public void testDeleteWithRoot_BasicDelete() {
        // 데이터 삽입
        BTree.StatelessInsertResult r1 = btree.insertWithRoot(0,
                "key1".getBytes(StandardCharsets.UTF_8), 100L);
        BTree.StatelessInsertResult r2 = btree.insertWithRoot(r1.newRootPageId,
                "key2".getBytes(StandardCharsets.UTF_8), 200L);

        // key1 삭제
        BTree.StatelessDeleteResult deleteResult = btree.deleteWithRoot(r2.newRootPageId,
                "key1".getBytes(StandardCharsets.UTF_8));

        assertTrue("Key should be deleted", deleteResult.deleted);

        // 새 root에서 key1 없음, key2 있음
        assertNull(btree.findWithRoot(deleteResult.newRootPageId,
                "key1".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Long.valueOf(200L), btree.findWithRoot(deleteResult.newRootPageId,
                "key2".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testDeleteWithRoot_NotFound() {
        BTree.StatelessInsertResult r1 = btree.insertWithRoot(0,
                "key1".getBytes(StandardCharsets.UTF_8), 100L);

        BTree.StatelessDeleteResult result = btree.deleteWithRoot(r1.newRootPageId,
                "key2".getBytes(StandardCharsets.UTF_8));

        assertFalse("Key not found, nothing deleted", result.deleted);
        // Root should remain same or equivalent
        assertEquals(Long.valueOf(100L), btree.findWithRoot(result.newRootPageId,
                "key1".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testDeleteWithRoot_COW_OldRootUnchanged() {
        // 데이터 삽입
        BTree.StatelessInsertResult r1 = btree.insertWithRoot(0,
                "key1".getBytes(StandardCharsets.UTF_8), 100L);
        BTree.StatelessInsertResult r2 = btree.insertWithRoot(r1.newRootPageId,
                "key2".getBytes(StandardCharsets.UTF_8), 200L);
        long oldRoot = r2.newRootPageId;

        // key1 삭제
        BTree.StatelessDeleteResult deleteResult = btree.deleteWithRoot(oldRoot,
                "key1".getBytes(StandardCharsets.UTF_8));
        long newRoot = deleteResult.newRootPageId;

        // COW: 이전 root는 여전히 key1, key2 모두 있어야 함
        assertEquals(Long.valueOf(100L), btree.findWithRoot(oldRoot,
                "key1".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Long.valueOf(200L), btree.findWithRoot(oldRoot,
                "key2".getBytes(StandardCharsets.UTF_8)));

        // 새 root는 key2만 있음
        assertNull(btree.findWithRoot(newRoot, "key1".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Long.valueOf(200L), btree.findWithRoot(newRoot,
                "key2".getBytes(StandardCharsets.UTF_8)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteWithRoot_NullKey() {
        btree.deleteWithRoot(0, null);
    }

    // ==================== 복합 시나리오 테스트 ====================

    @Test
    public void testStatelessAPI_MixedOperations() {
        // 여러 키 삽입
        long root = 0;
        for (int i = 0; i < 50; i++) {
            String key = String.format("key%03d", i);
            BTree.StatelessInsertResult result = btree.insertWithRoot(root,
                    key.getBytes(StandardCharsets.UTF_8), (long) i);
            root = result.newRootPageId;
        }

        // 검색 확인
        for (int i = 0; i < 50; i++) {
            String key = String.format("key%03d", i);
            Long value = btree.findWithRoot(root, key.getBytes(StandardCharsets.UTF_8));
            assertEquals(Long.valueOf(i), value);
        }

        // 일부 삭제
        for (int i = 0; i < 50; i += 2) {
            String key = String.format("key%03d", i);
            BTree.StatelessDeleteResult result = btree.deleteWithRoot(root,
                    key.getBytes(StandardCharsets.UTF_8));
            assertTrue(result.deleted);
            root = result.newRootPageId;
        }

        // 삭제 후 검증
        for (int i = 0; i < 50; i++) {
            String key = String.format("key%03d", i);
            Long value = btree.findWithRoot(root, key.getBytes(StandardCharsets.UTF_8));
            if (i % 2 == 0) {
                assertNull("Deleted key should not be found: " + key, value);
            } else {
                assertEquals(Long.valueOf(i), value);
            }
        }
    }

    @Test
    public void testStatelessAPI_MatchesLegacyAPI() {
        // 같은 데이터를 양 API로 처리하고 결과 비교
        byte[] key1 = "testKey1".getBytes(StandardCharsets.UTF_8);
        byte[] key2 = "testKey2".getBytes(StandardCharsets.UTF_8);

        // Legacy API
        btree.insert(key1, 1L);
        btree.insert(key2, 2L);
        long legacyRoot = btree.getRootPageId();

        // Stateless API로 검색
        assertEquals(Long.valueOf(1L), btree.findWithRoot(legacyRoot, key1));
        assertEquals(Long.valueOf(2L), btree.findWithRoot(legacyRoot, key2));

        // Legacy find도 동일
        assertEquals(Long.valueOf(1L), btree.find(key1));
        assertEquals(Long.valueOf(2L), btree.find(key2));
    }
}
