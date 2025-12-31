package com.snoworca.fxstore.ost;

import com.snoworca.fxstore.storage.Allocator;
import com.snoworca.fxstore.storage.MemoryStorage;
import com.snoworca.fxstore.storage.Storage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * OST Stateless API 테스트.
 *
 * <p>목적: getWithRoot, removeWithRoot, insertWithRoot 커버리지 향상</p>
 * <p>대상 메서드:</p>
 * <ul>
 *   <li>getWithRoot(long, int) - 50% → 80%+</li>
 *   <li>removeWithRoot(long, int) - 74% → 90%+</li>
 *   <li>insertWithRoot(long, int, long) - 84% → 95%+</li>
 * </ul>
 */
public class OSTStatelessApiTest {

    private static final int PAGE_SIZE = 4096;

    private Storage storage;
    private Allocator allocator;
    private OST ost;

    @Before
    public void setUp() {
        storage = new MemoryStorage();
        storage.extend(PAGE_SIZE * 100);
        allocator = new Allocator(PAGE_SIZE, PAGE_SIZE * 3);
        ost = OST.createEmpty(storage, allocator, PAGE_SIZE);
    }

    // ==================== sizeWithRoot 테스트 ====================

    @Test
    public void sizeWithRoot_emptyTree_shouldReturnZero() {
        // Given: 빈 트리 (rootPageId = 0)
        long rootPageId = 0L;

        // When/Then
        assertEquals(0, ost.sizeWithRoot(rootPageId));
    }

    @Test
    public void sizeWithRoot_nonEmptyTree_shouldReturnSize() {
        // Given: 요소가 있는 트리
        for (int i = 0; i < 50; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();

        // When/Then
        assertEquals(50, ost.sizeWithRoot(rootPageId));
    }

    // ==================== getWithRoot 테스트 ====================

    @Test(expected = IndexOutOfBoundsException.class)
    public void getWithRoot_emptyTree_shouldThrow() {
        // Given: 빈 트리
        long rootPageId = 0L;

        // When/Then: 빈 트리에서 조회 -> 예외
        ost.getWithRoot(rootPageId, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getWithRoot_negativeIndex_shouldThrow() {
        // Given: 요소가 있는 트리
        ost.insert(0, 1000L);
        long rootPageId = ost.getRootPageId();

        // When/Then: 음수 인덱스 -> 예외
        ost.getWithRoot(rootPageId, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getWithRoot_indexOutOfBounds_shouldThrow() {
        // Given: 10개 요소가 있는 트리
        for (int i = 0; i < 10; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();

        // When/Then: 범위 밖 인덱스 -> 예외
        ost.getWithRoot(rootPageId, 100);
    }

    @Test
    public void getWithRoot_validIndex_shouldReturnRecordId() {
        // Given: 요소가 있는 트리
        for (int i = 0; i < 20; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();

        // When/Then: 모든 요소 조회 가능
        for (int i = 0; i < 20; i++) {
            assertEquals(1000L + i, ost.getWithRoot(rootPageId, i));
        }
    }

    @Test
    public void getWithRoot_deepTree_shouldTraverseCorrectly() {
        // Given: 많은 요소 (내부 노드 생성)
        for (int i = 0; i < 300; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();

        // When/Then: 다양한 위치 조회
        assertEquals(1000L, ost.getWithRoot(rootPageId, 0));
        assertEquals(1150L, ost.getWithRoot(rootPageId, 150));
        assertEquals(1299L, ost.getWithRoot(rootPageId, 299));
    }

    // ==================== insertWithRoot 테스트 ====================

    @Test
    public void insertWithRoot_emptyTree_shouldCreateLeaf() {
        // Given: 빈 트리
        long rootPageId = 0L;
        ost.setAllocTail(PAGE_SIZE * 3);

        // When: 첫 요소 삽입
        OST.StatelessInsertResult result = ost.insertWithRoot(rootPageId, 0, 1000L);

        // Then: 새 루트 생성
        assertNotNull(result);
        assertTrue(result.newRootPageId > 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void insertWithRoot_negativeIndex_shouldThrow() {
        // Given: 빈 트리
        long rootPageId = 0L;

        // When: 음수 인덱스
        ost.insertWithRoot(rootPageId, -1, 1000L);

        // Then: IndexOutOfBoundsException
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void insertWithRoot_indexTooLarge_shouldThrow() {
        // Given: 요소가 있는 트리
        ost.insert(0, 1000L);
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 범위 밖 인덱스
        ost.insertWithRoot(rootPageId, 10, 2000L);

        // Then: IndexOutOfBoundsException
    }

    @Test
    public void insertWithRoot_atBeginning_shouldInsertCorrectly() {
        // Given: 요소가 있는 트리
        for (int i = 0; i < 10; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 맨 앞에 삽입
        OST.StatelessInsertResult result = ost.insertWithRoot(rootPageId, 0, 999L);

        // Then: 새 루트 반환
        assertNotNull(result);
        assertEquals(999L, ost.getWithRoot(result.newRootPageId, 0));
    }

    @Test
    public void insertWithRoot_atMiddle_shouldInsertCorrectly() {
        // Given: 요소가 있는 트리
        for (int i = 0; i < 10; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 중간에 삽입
        OST.StatelessInsertResult result = ost.insertWithRoot(rootPageId, 5, 9999L);

        // Then: 새 루트 반환, 중간에 삽입됨
        assertNotNull(result);
        assertEquals(9999L, ost.getWithRoot(result.newRootPageId, 5));
    }

    @Test
    public void insertWithRoot_atEnd_shouldInsertCorrectly() {
        // Given: 요소가 있는 트리
        for (int i = 0; i < 10; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 맨 뒤에 삽입
        OST.StatelessInsertResult result = ost.insertWithRoot(rootPageId, 10, 9999L);

        // Then: 새 루트 반환, 끝에 삽입됨
        assertNotNull(result);
        assertEquals(9999L, ost.getWithRoot(result.newRootPageId, 10));
    }

    // ==================== removeWithRoot 테스트 ====================

    @Test(expected = IndexOutOfBoundsException.class)
    public void removeWithRoot_emptyTree_shouldThrow() {
        // Given: 빈 트리
        long rootPageId = 0L;

        // When/Then: 빈 트리에서 삭제 -> 예외
        ost.removeWithRoot(rootPageId, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void removeWithRoot_negativeIndex_shouldThrow() {
        // Given: 요소가 있는 트리
        ost.insert(0, 1000L);
        long rootPageId = ost.getRootPageId();

        // When/Then: 음수 인덱스 -> 예외
        ost.removeWithRoot(rootPageId, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void removeWithRoot_indexOutOfBounds_shouldThrow() {
        // Given: 10개 요소가 있는 트리
        for (int i = 0; i < 10; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();

        // When/Then: 범위 밖 인덱스 -> 예외
        ost.removeWithRoot(rootPageId, 100);
    }

    @Test
    public void removeWithRoot_singleElement_shouldReturnEmptyTree() {
        // Given: 단일 요소 트리
        ost.insert(0, 1000L);
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 유일한 요소 삭제
        OST.StatelessRemoveResult result = ost.removeWithRoot(rootPageId, 0);

        // Then: 빈 트리 (rootPageId = 0)
        assertNotNull(result);
        assertEquals(1000L, result.removedRecordId);
        assertEquals(0L, result.newRootPageId);
    }

    @Test
    public void removeWithRoot_fromBeginning_shouldRemoveCorrectly() {
        // Given: 요소가 있는 트리
        for (int i = 0; i < 10; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 첫 번째 요소 삭제
        OST.StatelessRemoveResult result = ost.removeWithRoot(rootPageId, 0);

        // Then
        assertNotNull(result);
        assertEquals(1000L, result.removedRecordId);
        assertEquals(9, ost.sizeWithRoot(result.newRootPageId));
    }

    @Test
    public void removeWithRoot_fromMiddle_shouldRemoveCorrectly() {
        // Given: 요소가 있는 트리
        for (int i = 0; i < 10; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 중간 요소 삭제
        OST.StatelessRemoveResult result = ost.removeWithRoot(rootPageId, 5);

        // Then
        assertNotNull(result);
        assertEquals(1005L, result.removedRecordId);
        assertEquals(9, ost.sizeWithRoot(result.newRootPageId));
    }

    @Test
    public void removeWithRoot_fromEnd_shouldRemoveCorrectly() {
        // Given: 요소가 있는 트리
        for (int i = 0; i < 10; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 마지막 요소 삭제
        OST.StatelessRemoveResult result = ost.removeWithRoot(rootPageId, 9);

        // Then
        assertNotNull(result);
        assertEquals(1009L, result.removedRecordId);
        assertEquals(9, ost.sizeWithRoot(result.newRootPageId));
    }

    @Test
    public void removeWithRoot_deepTree_shouldTraverseCorrectly() {
        // Given: 많은 요소 (내부 노드 생성)
        for (int i = 0; i < 300; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 중간 요소 삭제
        OST.StatelessRemoveResult result = ost.removeWithRoot(rootPageId, 150);

        // Then
        assertNotNull(result);
        assertEquals(1150L, result.removedRecordId);
        assertEquals(299, ost.sizeWithRoot(result.newRootPageId));
    }

    @Test
    public void removeWithRoot_allElements_shouldResultInEmptyTree() {
        // Given: 요소가 있는 트리
        for (int i = 0; i < 20; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 모든 요소 삭제
        for (int i = 19; i >= 0; i--) {
            OST.StatelessRemoveResult result = ost.removeWithRoot(rootPageId, i);
            assertNotNull(result);
            rootPageId = result.newRootPageId;
            ost.setAllocTail(ost.getAllocTail());
        }

        // Then: 빈 트리
        assertEquals(0L, rootPageId);
        assertEquals(0, ost.sizeWithRoot(rootPageId));
    }

    // ==================== 복합 시나리오 ====================

    @Test
    public void statelessApi_mixedOperations_shouldMaintainCorrectness() {
        // Given: 초기 트리
        for (int i = 0; i < 50; i++) {
            ost.insert(i, 1000L + i);
        }
        long rootPageId = ost.getRootPageId();
        ost.setAllocTail(ost.getAllocTail());

        // When: 삽입과 삭제 혼합
        OST.StatelessInsertResult insertResult = ost.insertWithRoot(rootPageId, 25, 9999L);
        rootPageId = insertResult.newRootPageId;
        ost.setAllocTail(ost.getAllocTail());

        OST.StatelessRemoveResult removeResult = ost.removeWithRoot(rootPageId, 0);
        rootPageId = removeResult.newRootPageId;
        ost.setAllocTail(ost.getAllocTail());

        // Then: 크기 유지 (50 + 1 - 1 = 50)
        assertEquals(50, ost.sizeWithRoot(rootPageId));
    }

    @Test
    public void statelessApi_leafSplit_shouldWork() {
        // Given: 빈 트리
        long rootPageId = 0L;
        ost.setAllocTail(PAGE_SIZE * 3);

        // When: 100개 이상 삽입 (리프 분할 유발)
        for (int i = 0; i < 150; i++) {
            OST.StatelessInsertResult result = ost.insertWithRoot(rootPageId, i, 1000L + i);
            rootPageId = result.newRootPageId;
            ost.setAllocTail(ost.getAllocTail());
        }

        // Then
        assertEquals(150, ost.sizeWithRoot(rootPageId));
        for (int i = 0; i < 150; i++) {
            assertEquals(1000L + i, ost.getWithRoot(rootPageId, i));
        }
    }
}
