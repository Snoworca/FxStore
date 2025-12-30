package com.snoworca.fxstore.storage;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Allocator Legacy BATCH 모드 테스트
 *
 * <p>P0-2: Allocator 브랜치 커버리지 47% -> 70%+ 달성을 위한 테스트</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>Legacy 생성자 (pageSize, initialTail)</li>
 *   <li>beginPending() / commitPending() / rollbackPending()</li>
 *   <li>isPendingActive()</li>
 *   <li>getCommittedAllocTail()</li>
 *   <li>allocatePage() (Legacy)</li>
 *   <li>allocateRecord(int) (Legacy)</li>
 * </ul>
 *
 * @since v1.0 Phase 2
 * @see Allocator
 */
public class AllocatorBatchModeTest {

    private static final int PAGE_SIZE_4K = 4096;
    private static final int PAGE_SIZE_8K = 8192;
    private static final int PAGE_SIZE_16K = 16384;
    private static final long INITIAL_TAIL = 12288; // Superblock + 2 headers

    private Allocator allocator;

    @Before
    public void setUp() {
        allocator = new Allocator(PAGE_SIZE_4K, INITIAL_TAIL);
    }

    // ==================== Legacy 생성자 테스트 ====================

    @Test
    public void constructor_legacyWithInitialTail_shouldSetValues() {
        Allocator alloc = new Allocator(PAGE_SIZE_4K, INITIAL_TAIL);

        assertEquals(PAGE_SIZE_4K, alloc.getPageSize());
        assertEquals(INITIAL_TAIL, alloc.getAllocTail());
        assertEquals(INITIAL_TAIL, alloc.getCommittedAllocTail());
        assertFalse(alloc.isPendingActive());
    }

    @Test
    public void constructor_legacyAllPageSizes_shouldWork() {
        Allocator alloc4k = new Allocator(PAGE_SIZE_4K, INITIAL_TAIL);
        Allocator alloc8k = new Allocator(PAGE_SIZE_8K, INITIAL_TAIL);
        Allocator alloc16k = new Allocator(PAGE_SIZE_16K, INITIAL_TAIL);

        assertEquals(PAGE_SIZE_4K, alloc4k.getPageSize());
        assertEquals(PAGE_SIZE_8K, alloc8k.getPageSize());
        assertEquals(PAGE_SIZE_16K, alloc16k.getPageSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_invalidPageSize_shouldThrow() {
        new Allocator(2048, INITIAL_TAIL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_negativeInitialTail_shouldThrow() {
        new Allocator(PAGE_SIZE_4K, -1);
    }

    @Test
    public void constructor_zeroInitialTail_shouldWork() {
        Allocator alloc = new Allocator(PAGE_SIZE_4K, 0);
        assertEquals(0, alloc.getAllocTail());
    }

    // ==================== beginPending() 테스트 ====================

    @Test
    public void beginPending_shouldActivatePendingMode() {
        // Given: pending 모드가 비활성화된 상태
        assertFalse(allocator.isPendingActive());

        // When: beginPending 호출
        allocator.beginPending();

        // Then: pending 모드가 활성화됨
        assertTrue(allocator.isPendingActive());
    }

    @Test(expected = IllegalStateException.class)
    public void beginPending_alreadyActive_shouldThrow() {
        // Given: 이미 pending 모드
        allocator.beginPending();
        assertTrue(allocator.isPendingActive());

        // When & Then: 다시 호출하면 예외
        allocator.beginPending();
    }

    // ==================== commitPending() 테스트 ====================

    @Test
    public void commitPending_shouldPersistChanges() {
        // Given: pending 모드에서 할당 수행
        allocator.beginPending();
        allocator.allocatePage();
        allocator.allocateRecord(100);

        long tailBeforeCommit = allocator.getAllocTail();
        long committedBeforeCommit = allocator.getCommittedAllocTail();

        // tail은 변경됨, committed는 변경 안됨
        assertNotEquals(committedBeforeCommit, tailBeforeCommit);

        // When: 커밋
        allocator.commitPending();

        // Then: committed가 tail과 같아지고, pending 비활성화
        assertEquals(tailBeforeCommit, allocator.getCommittedAllocTail());
        assertFalse(allocator.isPendingActive());
    }

    @Test(expected = IllegalStateException.class)
    public void commitPending_notInPendingMode_shouldThrow() {
        // Given: pending 모드가 아닌 상태
        assertFalse(allocator.isPendingActive());

        // When & Then: 예외 발생
        allocator.commitPending();
    }

    // ==================== rollbackPending() 테스트 ====================

    @Test
    public void rollbackPending_shouldRevertChanges() {
        // Given: pending 모드에서 할당 수행
        long originalTail = allocator.getAllocTail();
        allocator.beginPending();
        allocator.allocatePage();
        allocator.allocateRecord(100);

        // tail이 변경됨
        assertNotEquals(originalTail, allocator.getAllocTail());

        // When: 롤백
        allocator.rollbackPending();

        // Then: tail이 원래 값으로 돌아가고, pending 비활성화
        assertEquals(originalTail, allocator.getAllocTail());
        assertEquals(originalTail, allocator.getCommittedAllocTail());
        assertFalse(allocator.isPendingActive());
    }

    @Test(expected = IllegalStateException.class)
    public void rollbackPending_notInPendingMode_shouldThrow() {
        // Given: pending 모드가 아닌 상태
        assertFalse(allocator.isPendingActive());

        // When & Then: 예외 발생
        allocator.rollbackPending();
    }

    // ==================== isPendingActive() 테스트 ====================

    @Test
    public void isPendingActive_initialState_shouldBeFalse() {
        assertFalse(allocator.isPendingActive());
    }

    @Test
    public void isPendingActive_afterBegin_shouldBeTrue() {
        allocator.beginPending();
        assertTrue(allocator.isPendingActive());
    }

    @Test
    public void isPendingActive_afterCommit_shouldBeFalse() {
        allocator.beginPending();
        allocator.commitPending();
        assertFalse(allocator.isPendingActive());
    }

    @Test
    public void isPendingActive_afterRollback_shouldBeFalse() {
        allocator.beginPending();
        allocator.rollbackPending();
        assertFalse(allocator.isPendingActive());
    }

    // ==================== getCommittedAllocTail() 테스트 ====================

    @Test
    public void getCommittedAllocTail_initialState_shouldEqualInitialTail() {
        assertEquals(INITIAL_TAIL, allocator.getCommittedAllocTail());
    }

    @Test
    public void getCommittedAllocTail_afterNonPendingAllocation_shouldUpdate() {
        // Given: pending 모드가 아닌 상태에서 할당
        long originalCommitted = allocator.getCommittedAllocTail();
        allocator.allocatePage();

        // Then: committed도 즉시 업데이트됨
        assertNotEquals(originalCommitted, allocator.getCommittedAllocTail());
        assertEquals(allocator.getAllocTail(), allocator.getCommittedAllocTail());
    }

    @Test
    public void getCommittedAllocTail_duringPending_shouldNotChange() {
        // Given: pending 모드 시작
        long originalCommitted = allocator.getCommittedAllocTail();
        allocator.beginPending();

        // When: pending 모드에서 할당
        allocator.allocatePage();
        allocator.allocateRecord(100);

        // Then: committed는 변경되지 않음
        assertEquals(originalCommitted, allocator.getCommittedAllocTail());
        assertNotEquals(allocator.getAllocTail(), allocator.getCommittedAllocTail());
    }

    // ==================== allocatePage() Legacy 테스트 ====================

    @Test
    public void allocatePage_legacy_shouldReturnAlignedOffset() {
        long offset = allocator.allocatePage();

        assertEquals(INITIAL_TAIL, offset);
        assertEquals(0, offset % PAGE_SIZE_4K);
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K, allocator.getAllocTail());
    }

    @Test
    public void allocatePage_legacy_sequential_shouldIncrement() {
        long offset1 = allocator.allocatePage();
        long offset2 = allocator.allocatePage();
        long offset3 = allocator.allocatePage();

        assertEquals(INITIAL_TAIL, offset1);
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K, offset2);
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K * 2, offset3);
    }

    @Test
    public void allocatePage_legacy_pendingMode_shouldNotCommit() {
        allocator.beginPending();
        long originalCommitted = allocator.getCommittedAllocTail();

        allocator.allocatePage();

        // tail은 변경되지만 committed는 변경되지 않음
        assertNotEquals(originalCommitted, allocator.getAllocTail());
        assertEquals(originalCommitted, allocator.getCommittedAllocTail());
    }

    @Test
    public void allocatePage_legacy_notPendingMode_shouldCommit() {
        long originalCommitted = allocator.getCommittedAllocTail();

        allocator.allocatePage();

        // tail과 committed 모두 변경됨
        assertNotEquals(originalCommitted, allocator.getAllocTail());
        assertEquals(allocator.getAllocTail(), allocator.getCommittedAllocTail());
    }

    // ==================== allocateRecord(int) Legacy 테스트 ====================

    @Test
    public void allocateRecord_legacy_shouldReturnAlignedOffset() {
        long offset = allocator.allocateRecord(100);

        assertEquals(INITIAL_TAIL, offset);
        assertEquals(0, offset % 8); // 8바이트 정렬
        assertEquals(INITIAL_TAIL + 100, allocator.getAllocTail());
    }

    @Test
    public void allocateRecord_legacy_sequential_shouldAlign() {
        long offset1 = allocator.allocateRecord(100); // 12288, tail = 12388
        long offset2 = allocator.allocateRecord(50);  // 12392 (8바이트 정렬), tail = 12442
        long offset3 = allocator.allocateRecord(200); // 12448 (8바이트 정렬), tail = 12648

        assertEquals(INITIAL_TAIL, offset1);
        assertEquals(12392, offset2); // 12388 -> 12392 (다음 8바이트 경계)
        assertEquals(12448, offset3); // 12442 -> 12448 (다음 8바이트 경계)
    }

    @Test(expected = IllegalArgumentException.class)
    public void allocateRecord_legacy_zeroSize_shouldThrow() {
        allocator.allocateRecord(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void allocateRecord_legacy_negativeSize_shouldThrow() {
        allocator.allocateRecord(-100);
    }

    @Test
    public void allocateRecord_legacy_pendingMode_shouldNotCommit() {
        allocator.beginPending();
        long originalCommitted = allocator.getCommittedAllocTail();

        allocator.allocateRecord(100);

        // tail은 변경되지만 committed는 변경되지 않음
        assertNotEquals(originalCommitted, allocator.getAllocTail());
        assertEquals(originalCommitted, allocator.getCommittedAllocTail());
    }

    @Test
    public void allocateRecord_legacy_notPendingMode_shouldCommit() {
        long originalCommitted = allocator.getCommittedAllocTail();

        allocator.allocateRecord(100);

        // tail과 committed 모두 변경됨
        assertNotEquals(originalCommitted, allocator.getAllocTail());
        assertEquals(allocator.getAllocTail(), allocator.getCommittedAllocTail());
    }

    // ==================== BATCH 모드 워크플로우 테스트 ====================

    @Test
    public void batchWorkflow_commitAfterMultipleAllocations() {
        // Given: 초기 상태
        long initial = allocator.getAllocTail();

        // When: BATCH 모드에서 여러 할당 후 커밋
        allocator.beginPending();
        allocator.allocatePage();
        allocator.allocatePage();
        allocator.allocateRecord(100);
        allocator.commitPending();

        // Then: 모든 변경사항이 커밋됨
        long finalTail = allocator.getAllocTail();
        assertTrue(finalTail > initial);
        assertEquals(finalTail, allocator.getCommittedAllocTail());
        assertFalse(allocator.isPendingActive());
    }

    @Test
    public void batchWorkflow_rollbackAfterMultipleAllocations() {
        // Given: 초기 상태
        long initial = allocator.getAllocTail();

        // When: BATCH 모드에서 여러 할당 후 롤백
        allocator.beginPending();
        allocator.allocatePage();
        allocator.allocatePage();
        allocator.allocateRecord(100);
        allocator.rollbackPending();

        // Then: 모든 변경사항이 롤백됨
        assertEquals(initial, allocator.getAllocTail());
        assertEquals(initial, allocator.getCommittedAllocTail());
        assertFalse(allocator.isPendingActive());
    }

    @Test
    public void batchWorkflow_multipleCycles() {
        // 첫 번째 BATCH 사이클: 커밋
        allocator.beginPending();
        allocator.allocatePage();
        long afterFirst = allocator.getAllocTail();
        allocator.commitPending();

        assertEquals(afterFirst, allocator.getCommittedAllocTail());

        // 두 번째 BATCH 사이클: 롤백
        allocator.beginPending();
        allocator.allocatePage();
        assertNotEquals(afterFirst, allocator.getAllocTail());
        allocator.rollbackPending();

        assertEquals(afterFirst, allocator.getAllocTail());

        // 세 번째 BATCH 사이클: 커밋
        allocator.beginPending();
        allocator.allocatePage();
        long afterThird = allocator.getAllocTail();
        allocator.commitPending();

        assertEquals(afterThird, allocator.getCommittedAllocTail());
    }

    // ==================== 혼합 할당 테스트 ====================

    @Test
    public void mixedAllocation_pageAndRecord_shouldAlignCorrectly() {
        // 레코드 할당 (100 바이트)
        long r1 = allocator.allocateRecord(100);
        assertEquals(INITIAL_TAIL, r1);

        // 페이지 할당 - 다음 4K 경계로 정렬
        long p1 = allocator.allocatePage();
        assertEquals(16384, p1); // 12388 -> 16384 (4K 정렬)

        // 레코드 할당 (200 바이트)
        long r2 = allocator.allocateRecord(200);
        assertEquals(20480, r2); // 20480 (이미 8바이트 정렬)

        // 페이지 할당 - 다음 4K 경계로 정렬
        long p2 = allocator.allocatePage();
        assertEquals(24576, p2); // 20680 -> 24576 (4K 정렬)
    }

    // ==================== 경계 조건 테스트 ====================

    @Test
    public void allocatePage_atExactPageBoundary_shouldNotAddPadding() {
        // initialTail이 정확히 4K 경계에 있음 (12288 = 3 * 4096)
        long offset = allocator.allocatePage();
        assertEquals(INITIAL_TAIL, offset); // 패딩 없음
    }

    @Test
    public void allocateRecord_atExact8ByteBoundary_shouldNotAddPadding() {
        // initialTail이 정확히 8바이트 경계에 있음 (12288 = 1536 * 8)
        long offset = allocator.allocateRecord(16);
        assertEquals(INITIAL_TAIL, offset); // 패딩 없음
    }

    @Test
    public void allocate_largeRecord_shouldWork() {
        int size = 1_048_576; // 1 MiB
        long offset = allocator.allocateRecord(size);

        assertEquals(INITIAL_TAIL, offset);
        assertEquals(INITIAL_TAIL + size, allocator.getAllocTail());
    }

    // ==================== 오버플로우 테스트 ====================

    @Test(expected = IllegalStateException.class)
    public void allocatePage_legacy_overflow_shouldThrow() {
        // 오버플로우 임계값 근처에서 할당
        Allocator nearMax = new Allocator(PAGE_SIZE_4K, Long.MAX_VALUE - 16384);
        nearMax.allocatePage();
    }

    @Test(expected = IllegalStateException.class)
    public void allocateRecord_legacy_overflow_shouldThrow() {
        // 오버플로우 임계값 근처에서 할당
        Allocator nearMax = new Allocator(PAGE_SIZE_4K, Long.MAX_VALUE - 100);
        nearMax.allocateRecord(200);
    }

    // ==================== getAllocTail() 테스트 ====================

    @Test
    public void getAllocTail_afterMultipleOperations_shouldReflectCurrentState() {
        assertEquals(INITIAL_TAIL, allocator.getAllocTail());

        allocator.allocatePage();
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K, allocator.getAllocTail());

        allocator.allocateRecord(100);
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K + 100, allocator.getAllocTail());
    }

    @Test
    public void getAllocTail_duringPending_shouldReflectPendingChanges() {
        allocator.beginPending();

        assertEquals(INITIAL_TAIL, allocator.getAllocTail());

        allocator.allocatePage();
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K, allocator.getAllocTail());

        // 롤백 후 원래 값으로 복원
        allocator.rollbackPending();
        assertEquals(INITIAL_TAIL, allocator.getAllocTail());
    }
}
