package com.snoworca.fxstore.storage;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Allocator Stateless API 테스트 (Phase 8)
 *
 * <p>Phase 8 - 동시성 지원</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>Stateless 생성자</li>
 *   <li>allocatePage(currentAllocTail) 메서드</li>
 *   <li>allocateRecord(currentAllocTail, size) 메서드</li>
 *   <li>AllocationResult 불변 클래스</li>
 *   <li>정렬 규칙 검증</li>
 *   <li>오버플로우 처리</li>
 * </ul>
 */
public class AllocatorStatelessTest {

    private static final int PAGE_SIZE_4K = 4096;
    private static final int PAGE_SIZE_8K = 8192;
    private static final int PAGE_SIZE_16K = 16384;
    private static final long INITIAL_TAIL = 12288; // Superblock + 2 headers

    private Allocator allocator;

    @Before
    public void setUp() {
        allocator = new Allocator(PAGE_SIZE_4K);
    }

    // ==================== Stateless 생성자 테스트 ====================

    @Test
    public void testStatelessConstructor() {
        Allocator stateless = new Allocator(PAGE_SIZE_4K);
        assertEquals(PAGE_SIZE_4K, stateless.getPageSize());
    }

    @Test
    public void testStatelessConstructor_AllValidSizes() {
        Allocator alloc4k = new Allocator(PAGE_SIZE_4K);
        Allocator alloc8k = new Allocator(PAGE_SIZE_8K);
        Allocator alloc16k = new Allocator(PAGE_SIZE_16K);

        assertEquals(PAGE_SIZE_4K, alloc4k.getPageSize());
        assertEquals(PAGE_SIZE_8K, alloc8k.getPageSize());
        assertEquals(PAGE_SIZE_16K, alloc16k.getPageSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStatelessConstructor_InvalidPageSize() {
        new Allocator(2048); // 유효하지 않은 크기
    }

    // ==================== allocatePage(currentAllocTail) 테스트 ====================

    @Test
    public void testAllocatePageStateless_Basic() {
        Allocator.AllocationResult result = allocator.allocatePage(INITIAL_TAIL);

        assertEquals("Offset should be at initial tail", INITIAL_TAIL, result.offset);
        assertEquals("Page ID should be correct", INITIAL_TAIL / PAGE_SIZE_4K, result.pageId);
        assertEquals("New tail should advance by page size",
                INITIAL_TAIL + PAGE_SIZE_4K, result.newAllocTail);
    }

    @Test
    public void testAllocatePageStateless_Alignment() {
        // 정렬되지 않은 위치에서 시작
        long unalignedTail = 12290; // 4K로 정렬되지 않음
        Allocator.AllocationResult result = allocator.allocatePage(unalignedTail);

        // 다음 4K 경계로 정렬
        long expectedOffset = 16384; // 다음 4K 경계
        assertEquals("Offset should be aligned to page size", expectedOffset, result.offset);
        assertEquals("Offset should be page-aligned", 0, result.offset % PAGE_SIZE_4K);
        assertEquals("Page ID should be correct", expectedOffset / PAGE_SIZE_4K, result.pageId);
        assertEquals("New tail should be offset + pageSize",
                expectedOffset + PAGE_SIZE_4K, result.newAllocTail);
    }

    @Test
    public void testAllocatePageStateless_Sequential() {
        long tail = INITIAL_TAIL;

        // 첫 번째 할당
        Allocator.AllocationResult r1 = allocator.allocatePage(tail);
        assertEquals(INITIAL_TAIL, r1.offset);
        assertEquals(3, r1.pageId); // 12288 / 4096 = 3

        // 두 번째 할당 (이전 결과의 newAllocTail 사용)
        Allocator.AllocationResult r2 = allocator.allocatePage(r1.newAllocTail);
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K, r2.offset);
        assertEquals(4, r2.pageId);

        // 세 번째 할당
        Allocator.AllocationResult r3 = allocator.allocatePage(r2.newAllocTail);
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K * 2, r3.offset);
        assertEquals(5, r3.pageId);
    }

    @Test
    public void testAllocatePageStateless_DifferentPageSizes() {
        // 8K 페이지
        Allocator alloc8k = new Allocator(PAGE_SIZE_8K);
        Allocator.AllocationResult result8k = alloc8k.allocatePage(24576);
        assertEquals(24576, result8k.offset);
        assertEquals(24576 / PAGE_SIZE_8K, result8k.pageId);
        assertEquals(24576 + PAGE_SIZE_8K, result8k.newAllocTail);

        // 16K 페이지
        Allocator alloc16k = new Allocator(PAGE_SIZE_16K);
        Allocator.AllocationResult result16k = alloc16k.allocatePage(49152);
        assertEquals(49152, result16k.offset);
        assertEquals(49152 / PAGE_SIZE_16K, result16k.pageId);
        assertEquals(49152 + PAGE_SIZE_16K, result16k.newAllocTail);
    }

    @Test(expected = IllegalStateException.class)
    public void testAllocatePageStateless_Overflow() {
        long nearMax = Long.MAX_VALUE - (16384 * 2) + 1;
        allocator.allocatePage(nearMax);
    }

    // ==================== allocateRecord(currentAllocTail, size) 테스트 ====================

    @Test
    public void testAllocateRecordStateless_Basic() {
        Allocator.AllocationResult result = allocator.allocateRecord(INITIAL_TAIL, 100);

        assertEquals("Offset should be at initial tail", INITIAL_TAIL, result.offset);
        assertEquals("Page ID should be -1 for records", -1, result.pageId);
        assertEquals("New tail should advance by record size",
                INITIAL_TAIL + 100, result.newAllocTail);
    }

    @Test
    public void testAllocateRecordStateless_Alignment() {
        // 정렬되지 않은 위치에서 시작
        long unalignedTail = 12290; // 8로 정렬되지 않음
        Allocator.AllocationResult result = allocator.allocateRecord(unalignedTail, 100);

        // 다음 8바이트 경계로 정렬
        long expectedOffset = 12296; // 다음 8바이트 경계
        assertEquals("Offset should be 8-byte aligned", expectedOffset, result.offset);
        assertEquals("Offset should be 8-byte aligned", 0, result.offset % 8);
        assertEquals("New tail should be offset + size",
                expectedOffset + 100, result.newAllocTail);
    }

    @Test
    public void testAllocateRecordStateless_Sequential() {
        long tail = INITIAL_TAIL;

        // 첫 번째 레코드 (100 바이트)
        Allocator.AllocationResult r1 = allocator.allocateRecord(tail, 100);
        assertEquals(INITIAL_TAIL, r1.offset);
        assertEquals(INITIAL_TAIL + 100, r1.newAllocTail);

        // 두 번째 레코드 (50 바이트) - 정렬 필요
        // 12388 → align to 12392
        Allocator.AllocationResult r2 = allocator.allocateRecord(r1.newAllocTail, 50);
        assertEquals(12392, r2.offset); // 12388 aligned to 8 = 12392
        assertEquals(12392 + 50, r2.newAllocTail);

        // 세 번째 레코드 (200 바이트) - 정렬 필요
        // 12442 → align to 12448
        Allocator.AllocationResult r3 = allocator.allocateRecord(r2.newAllocTail, 200);
        assertEquals(12448, r3.offset);
        assertEquals(12448 + 200, r3.newAllocTail);
    }

    @Test
    public void testAllocateRecordStateless_Large() {
        int recordSize = 1_048_576; // 1 MiB
        Allocator.AllocationResult result = allocator.allocateRecord(INITIAL_TAIL, recordSize);

        assertEquals(INITIAL_TAIL, result.offset);
        assertEquals(INITIAL_TAIL + recordSize, result.newAllocTail);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllocateRecordStateless_ZeroSize() {
        allocator.allocateRecord(INITIAL_TAIL, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllocateRecordStateless_NegativeSize() {
        allocator.allocateRecord(INITIAL_TAIL, -100);
    }

    @Test(expected = IllegalStateException.class)
    public void testAllocateRecordStateless_Overflow() {
        long nearMax = Long.MAX_VALUE - 100;
        allocator.allocateRecord(nearMax, 200);
    }

    // ==================== 혼합 할당 테스트 ====================

    @Test
    public void testMixedAllocation() {
        long tail = INITIAL_TAIL;

        // 레코드 할당 (100 바이트)
        Allocator.AllocationResult r1 = allocator.allocateRecord(tail, 100);
        assertEquals(INITIAL_TAIL, r1.offset);
        tail = r1.newAllocTail;

        // 페이지 할당 - 다음 4K 경계로 정렬
        Allocator.AllocationResult r2 = allocator.allocatePage(tail);
        assertEquals(16384, r2.offset); // 12388 → 16384 (4K 정렬)
        assertEquals(4, r2.pageId);
        tail = r2.newAllocTail;

        // 레코드 할당 (200 바이트)
        Allocator.AllocationResult r3 = allocator.allocateRecord(tail, 200);
        assertEquals(20480, r3.offset); // 20480 (이미 8바이트 정렬)
        tail = r3.newAllocTail;

        // 페이지 할당 - 다음 4K 경계로 정렬
        Allocator.AllocationResult r4 = allocator.allocatePage(tail);
        assertEquals(24576, r4.offset); // 20680 → 24576 (4K 정렬)
        assertEquals(6, r4.pageId);
    }

    // ==================== AllocationResult 불변성 테스트 ====================

    @Test
    public void testAllocationResult_Immutability() {
        Allocator.AllocationResult result = allocator.allocatePage(INITIAL_TAIL);

        // 필드가 final이므로 변경 불가
        assertEquals(3, result.pageId);
        assertEquals(INITIAL_TAIL, result.offset);
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K, result.newAllocTail);

        // getter 메서드 테스트
        assertEquals(result.pageId, result.getPageId());
        assertEquals(result.offset, result.getOffset());
        assertEquals(result.newAllocTail, result.getNewAllocTail());
    }

    @Test
    public void testAllocationResult_Equals() {
        Allocator.AllocationResult r1 = new Allocator.AllocationResult(3, 12288, 16384);
        Allocator.AllocationResult r2 = new Allocator.AllocationResult(3, 12288, 16384);
        Allocator.AllocationResult r3 = new Allocator.AllocationResult(4, 16384, 20480);

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    public void testAllocationResult_ToString() {
        Allocator.AllocationResult result = new Allocator.AllocationResult(3, 12288, 16384);
        String str = result.toString();

        assertTrue(str.contains("pageId=3"));
        assertTrue(str.contains("offset=12288"));
        assertTrue(str.contains("newAllocTail=16384"));
    }

    @Test
    public void testAllocationResult_EqualsEdgeCases() {
        Allocator.AllocationResult r1 = new Allocator.AllocationResult(3, 12288, 16384);

        // 자기 자신과 같음
        assertEquals(r1, r1);

        // null과 같지 않음
        assertNotEquals(r1, null);

        // 다른 타입과 같지 않음
        assertNotEquals(r1, "not an AllocationResult");
    }

    // ==================== Stateless vs Legacy 비교 테스트 ====================

    @Test
    public void testStatelessMatchesLegacy() {
        // Legacy API
        Allocator legacy = new Allocator(PAGE_SIZE_4K, INITIAL_TAIL);
        long legacyOffset = legacy.allocatePage();
        long legacyTail = legacy.getAllocTail();

        // Stateless API
        Allocator stateless = new Allocator(PAGE_SIZE_4K);
        Allocator.AllocationResult result = stateless.allocatePage(INITIAL_TAIL);

        assertEquals("Offsets should match", legacyOffset, result.offset);
        assertEquals("Tails should match", legacyTail, result.newAllocTail);
    }

    // ==================== 동시성 시나리오 시뮬레이션 ====================

    @Test
    public void testConcurrencyScenario_SnapshotBased() {
        // 시뮬레이션: StoreSnapshot 기반 할당
        // 실제 동시성 테스트는 통합 테스트에서 수행

        long snapshotAllocTail = INITIAL_TAIL;

        // Writer 1이 페이지 할당
        Allocator.AllocationResult w1Result = allocator.allocatePage(snapshotAllocTail);
        long newSnapshotTail1 = w1Result.newAllocTail;

        // 새 스냅샷 생성 후 다음 할당
        Allocator.AllocationResult w2Result = allocator.allocatePage(newSnapshotTail1);
        long newSnapshotTail2 = w2Result.newAllocTail;

        // 검증
        assertEquals(INITIAL_TAIL, w1Result.offset);
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K, w2Result.offset);
        assertEquals(INITIAL_TAIL + PAGE_SIZE_4K * 2, newSnapshotTail2);
    }

    // ==================== 성능 테스트 ====================

    @Test
    public void testPerformance_StatelessAllocation() {
        int count = 100_000;
        long tail = INITIAL_TAIL;

        long startTime = System.nanoTime();

        for (int i = 0; i < count; i++) {
            Allocator.AllocationResult result = allocator.allocatePage(tail);
            tail = result.newAllocTail;
        }

        long endTime = System.nanoTime();
        double avgTimeUs = (endTime - startTime) / (double) count / 1000.0;

        System.out.printf("Stateless 할당 성능: %d회, 평균 %.2fμs%n", count, avgTimeUs);
        assertTrue("Average allocation time should be < 1μs", avgTimeUs < 1.0);
    }
}
