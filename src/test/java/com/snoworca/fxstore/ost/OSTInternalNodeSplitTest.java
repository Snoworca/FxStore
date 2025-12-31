package com.snoworca.fxstore.ost;

import com.snoworca.fxstore.storage.Allocator;
import com.snoworca.fxstore.storage.MemoryStorage;
import com.snoworca.fxstore.storage.Storage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * OST 내부 노드 분할 테스트.
 *
 * <p>목적: splitInternalNode() 0% → 100% 커버리지</p>
 * <p>조건:</p>
 * <ul>
 *   <li>OST Leaf 노드: 최대 100개 요소</li>
 *   <li>OST Internal 노드: 최대 128개 자식</li>
 *   <li>splitInternalNode 트리거: 129개 이상 leaf 필요</li>
 *   <li>필요 요소: 128 × 100 + 1 = 12,801개 이상</li>
 * </ul>
 * <p>메모리: ~55MB (128GB 시스템에서 충분)</p>
 */
public class OSTInternalNodeSplitTest {

    private static final int PAGE_SIZE = 4096;

    /**
     * 내부 노드 분할을 트리거하기 위한 최소 요소 수.
     * 128 leaves × 100 elements + 여유 = 13,000개
     */
    private static final int ELEMENTS_FOR_INTERNAL_SPLIT = 13000;

    private Storage storage;
    private Allocator allocator;

    @Before
    public void setUp() {
        storage = new MemoryStorage();
        // 충분한 공간 할당 (~100MB)
        storage.extend(PAGE_SIZE * 25000L);
        allocator = new Allocator(PAGE_SIZE, PAGE_SIZE * 3);
    }

    // ==================== 기본 대용량 삽입 테스트 ====================

    @Test
    public void insert_massiveData_shouldTriggerInternalNodeSplit() {
        // Given: 빈 OST
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);

        // When: 13,000개 요소 삽입 (내부 노드 분할 트리거)
        for (int i = 0; i < ELEMENTS_FOR_INTERNAL_SPLIT; i++) {
            ost.insert(i, 1000L + i);
        }

        // Then: 모든 요소 정상 저장
        assertEquals(ELEMENTS_FOR_INTERNAL_SPLIT, ost.size());

        // 데이터 무결성 검증 (샘플링)
        assertEquals(1000L, ost.get(0));
        assertEquals(1000L + 6500, ost.get(6500));
        assertEquals(1000L + 12999, ost.get(12999));
    }

    @Test
    public void insert_exactSplitBoundary_shouldWork() {
        // Given: 빈 OST
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);

        // When: 정확히 12,801개 삽입 (분할 경계)
        int exactBoundary = 12801;
        for (int i = 0; i < exactBoundary; i++) {
            ost.insert(i, 2000L + i);
        }

        // Then
        assertEquals(exactBoundary, ost.size());
    }

    // ==================== Stateless API 대용량 테스트 ====================

    @Test
    public void statelessInsert_massiveData_shouldTriggerSplit() {
        // Given: 빈 트리
        long rootPageId = 0L;
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);
        ost.setAllocTail(PAGE_SIZE * 3);

        // When: Stateless API로 13,000개 삽입
        for (int i = 0; i < ELEMENTS_FOR_INTERNAL_SPLIT; i++) {
            OST.StatelessInsertResult result = ost.insertWithRoot(rootPageId, i, 3000L + i);
            rootPageId = result.newRootPageId;
            ost.setAllocTail(ost.getAllocTail());
        }

        // Then
        assertEquals(ELEMENTS_FOR_INTERNAL_SPLIT, ost.sizeWithRoot(rootPageId));

        // 데이터 무결성 검증
        assertEquals(3000L, ost.getWithRoot(rootPageId, 0));
        assertEquals(3000L + 12999, ost.getWithRoot(rootPageId, 12999));
    }

    // ==================== 삭제 후 무결성 테스트 ====================

    @Test
    public void remove_afterMassiveInsert_shouldMaintainIntegrity() {
        // Given: 13,000개 요소가 있는 트리
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);
        for (int i = 0; i < ELEMENTS_FOR_INTERNAL_SPLIT; i++) {
            ost.insert(i, 1000L + i);
        }

        // When: 뒤에서부터 절반 삭제
        for (int i = ELEMENTS_FOR_INTERNAL_SPLIT - 1; i >= 6500; i--) {
            ost.remove(i);
        }

        // Then
        assertEquals(6500, ost.size());
        for (int i = 0; i < 100; i++) {  // 샘플 검증
            assertEquals(1000L + i, ost.get(i));
        }
    }

    @Test
    public void remove_fromBeginning_shouldMaintainIntegrity() {
        // Given: 13,000개 요소가 있는 트리
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);
        for (int i = 0; i < ELEMENTS_FOR_INTERNAL_SPLIT; i++) {
            ost.insert(i, 1000L + i);
        }

        // When: 앞에서부터 절반 삭제
        for (int i = 0; i < 6500; i++) {
            ost.remove(0);  // 항상 첫 번째 요소 삭제
        }

        // Then
        assertEquals(6500, ost.size());
        // 남은 요소는 원래 인덱스 6500~12999의 값
        assertEquals(1000L + 6500, ost.get(0));
    }

    // ==================== 중간 삽입 테스트 ====================

    @Test
    public void insert_atMiddle_massiveData_shouldWork() {
        // Given: 빈 OST
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);

        // When: 번갈아가며 삽입 (중간 삽입 유발)
        for (int i = 0; i < ELEMENTS_FOR_INTERNAL_SPLIT; i++) {
            int insertIndex = i / 2;  // 중간에 삽입
            ost.insert(insertIndex, 4000L + i);
        }

        // Then
        assertEquals(ELEMENTS_FOR_INTERNAL_SPLIT, ost.size());
    }

    // ==================== 랜덤 접근 테스트 ====================

    @Test
    public void get_randomAccess_afterMassiveInsert_shouldWork() {
        // Given: 13,000개 요소가 있는 트리
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);
        for (int i = 0; i < ELEMENTS_FOR_INTERNAL_SPLIT; i++) {
            ost.insert(i, 5000L + i);
        }

        // When/Then: 랜덤 인덱스 접근
        int[] testIndices = {0, 100, 1000, 5000, 10000, 12500, 12999};
        for (int idx : testIndices) {
            assertEquals(5000L + idx, ost.get(idx));
        }
    }

    // ==================== 연속 분할 테스트 ====================

    @Test
    public void insert_beyondFirstSplit_shouldHandleMultipleSplits() {
        // Given: 빈 OST
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);

        // 더 큰 스토리지 확장
        storage.extend(PAGE_SIZE * 50000L);

        // When: 20,000개 삽입 (여러 번 분할)
        int largeCount = 20000;
        for (int i = 0; i < largeCount; i++) {
            ost.insert(i, 6000L + i);
        }

        // Then
        assertEquals(largeCount, ost.size());
        assertEquals(6000L, ost.get(0));
        assertEquals(6000L + 19999, ost.get(19999));
    }
}
