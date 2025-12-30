package com.snoworca.fxstore.storage;

/**
 * Stateless Append-only 할당자 (Phase 8 동시성 지원)
 *
 * <p>페이지와 레코드의 할당 오프셋을 계산하며, 각각 적절한 정렬을 보장합니다.
 * 이 클래스는 상태를 관리하지 않으며, allocTail은 StoreSnapshot에서 관리됩니다.</p>
 *
 * <h3>정렬 규칙</h3>
 * <ul>
 *   <li>페이지: pageSize 정렬 (4096, 8192, 또는 16384 바이트)</li>
 *   <li>레코드: 8바이트 정렬</li>
 * </ul>
 *
 * <h3>동시성 모델 (Phase 8)</h3>
 * <p>allocTail 상태는 StoreSnapshot에서 불변으로 관리됩니다.
 * 모든 할당 메서드는 현재 allocTail을 파라미터로 받고,
 * AllocationResult로 새 allocTail을 반환합니다.</p>
 *
 * <pre>{@code
 * // 사용 예시 (쓰기 락 하에서)
 * StoreSnapshot snap = store.snapshot();
 * AllocationResult result = allocator.allocatePage(snap.getAllocTail());
 * StoreSnapshot newSnap = snap.withAllocTail(result.newAllocTail);
 * store.publishSnapshot(newSnap);
 * }</pre>
 *
 * <h3>불변식</h3>
 * <ul>
 *   <li>INV-9: allocTail은 항상 증가만 한다 (컴팩션 제외)</li>
 *   <li>INV-C2: StoreSnapshot 불변성으로 스레드 안전 보장</li>
 * </ul>
 *
 * <h3>스레드 안전성</h3>
 * <p>이 클래스는 stateless이므로 스레드 안전합니다.
 * 단, 동시 쓰기는 FxStoreImpl의 WriteLock으로 직렬화됩니다.</p>
 *
 * @since 1.0
 * @see com.snoworca.fxstore.core.StoreSnapshot
 */
public class Allocator {

    /** 유효한 페이지 크기 */
    private static final int[] VALID_PAGE_SIZES = {4096, 8192, 16384};

    /** 레코드 정렬 크기 */
    private static final int RECORD_ALIGNMENT = 8;

    /** 오버플로우 체크 임계값 */
    private static final long OVERFLOW_THRESHOLD = Long.MAX_VALUE - (16384 * 2);

    /** 페이지 크기 */
    private final int pageSize;

    // ============================================================
    // Legacy 상태 필드 (하위 호환성용, Phase 8 완료 후 제거 예정)
    // ============================================================

    /** 커밋된 allocTail (영속 상태) - Legacy */
    private long committedAllocTail;

    /** 현재 allocTail (BATCH 모드가 아니면 committedAllocTail과 동일) - Legacy */
    private long currentAllocTail;

    /** BATCH 모드(pending) 활성화 여부 - Legacy */
    private boolean pendingActive;

    /**
     * Allocator 생성자
     *
     * @param pageSize 페이지 크기 (4096, 8192, 또는 16384만 허용)
     * @param initialTail 초기 allocTail (Superblock + 2 headers = 12288)
     * @throws IllegalArgumentException pageSize가 유효하지 않거나 initialTail이 음수인 경우
     */
    public Allocator(int pageSize, long initialTail) {
        // pageSize 검증
        if (!isValidPageSize(pageSize)) {
            throw new IllegalArgumentException(
                "Invalid pageSize: must be 4096, 8192, or 16384, but was: " + pageSize
            );
        }

        // initialTail 검증
        if (initialTail < 0) {
            throw new IllegalArgumentException(
                "initialTail must be non-negative, but was: " + initialTail
            );
        }

        this.pageSize = pageSize;
        this.committedAllocTail = initialTail;
        this.currentAllocTail = initialTail;
        this.pendingActive = false;
    }

    /**
     * 페이지 크기만으로 Allocator 생성 (Stateless 모드)
     *
     * <p>Phase 8 동시성 모델에서 allocTail 상태 없이 사용할 때 사용합니다.</p>
     *
     * @param pageSize 페이지 크기 (4096, 8192, 또는 16384만 허용)
     * @throws IllegalArgumentException pageSize가 유효하지 않은 경우
     */
    public Allocator(int pageSize) {
        if (!isValidPageSize(pageSize)) {
            throw new IllegalArgumentException(
                "Invalid pageSize: must be 4096, 8192, or 16384, but was: " + pageSize
            );
        }

        this.pageSize = pageSize;
        this.committedAllocTail = 0;
        this.currentAllocTail = 0;
        this.pendingActive = false;
    }

    // ============================================================
    // Phase 8 Stateless API (권장)
    // ============================================================

    /**
     * 새 페이지를 할당합니다 (Stateless)
     *
     * <p>페이지는 pageSize에 정렬됩니다. 쓰기 락 하에서만 호출해야 합니다.</p>
     *
     * <pre>{@code
     * long stamp = store.acquireWriteLock();
     * try {
     *     StoreSnapshot snap = store.snapshot();
     *     AllocationResult result = allocator.allocatePage(snap.getAllocTail());
     *     // result.offset에 데이터 쓰기
     *     // snap.withAllocTail(result.newAllocTail)로 새 스냅샷 생성
     * } finally {
     *     store.releaseWriteLock(stamp);
     * }
     * }</pre>
     *
     * @param currentAllocTail 현재 allocTail (StoreSnapshot에서 획득)
     * @return 할당 결과 (offset, pageId, newAllocTail)
     * @throws IllegalStateException allocTail 오버플로우 발생 시
     */
    public AllocationResult allocatePage(long currentAllocTail) {
        long aligned = alignUp(currentAllocTail, pageSize);
        long newTail = aligned + pageSize;

        // 오버플로우 체크
        if (aligned > OVERFLOW_THRESHOLD || newTail < aligned) {
            throw new IllegalStateException(
                "Allocation overflow: cannot allocate page at offset " + aligned
            );
        }

        long pageId = aligned / pageSize;
        return new AllocationResult(pageId, aligned, newTail);
    }

    /**
     * 새 레코드를 할당합니다 (Stateless)
     *
     * <p>레코드는 8바이트에 정렬됩니다. 쓰기 락 하에서만 호출해야 합니다.</p>
     *
     * @param currentAllocTail 현재 allocTail (StoreSnapshot에서 획득)
     * @param size 레코드 크기 (바이트, 양수여야 함)
     * @return 할당 결과 (offset, newAllocTail)
     * @throws IllegalArgumentException size가 0 이하인 경우
     * @throws IllegalStateException allocTail 오버플로우 발생 시
     */
    public AllocationResult allocateRecord(long currentAllocTail, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException(
                "Record size must be positive, but was: " + size
            );
        }

        long aligned = alignUp(currentAllocTail, RECORD_ALIGNMENT);
        long newTail = aligned + size;

        // 오버플로우 체크
        if (aligned > OVERFLOW_THRESHOLD || newTail < aligned) {
            throw new IllegalStateException(
                "Allocation overflow: cannot allocate record at offset " + aligned
            );
        }

        // 레코드는 pageId가 없음 (-1)
        return new AllocationResult(-1, aligned, newTail);
    }

    /**
     * 페이지 크기 반환
     *
     * @return 페이지 크기 (바이트)
     */
    public int getPageSize() {
        return pageSize;
    }

    // ============================================================
    // Legacy API (하위 호환성용)
    // 주의: Phase 8 완료 후 deprecated 예정
    // ============================================================

    /**
     * 새 페이지를 할당합니다 (Legacy, 상태 기반)
     *
     * <p>페이지는 pageSize에 정렬됩니다.</p>
     *
     * @return 할당된 페이지의 오프셋
     * @throws IllegalStateException allocTail 오버플로우 발생 시
     * @deprecated Phase 8에서는 {@link #allocatePage(long)} 사용 권장
     */
    public long allocatePage() {
        long aligned = alignUp(currentAllocTail, pageSize);
        long newTail = aligned + pageSize;

        // 오버플로우 체크
        if (aligned > OVERFLOW_THRESHOLD || newTail < aligned) {
            throw new IllegalStateException(
                "Allocation overflow: cannot allocate page at offset " + aligned
            );
        }

        currentAllocTail = newTail;

        // Pending 모드가 아니면 즉시 커밋
        if (!pendingActive) {
            committedAllocTail = newTail;
        }

        return aligned;
    }

    /**
     * 새 레코드를 할당합니다 (Legacy, 상태 기반)
     *
     * <p>레코드는 8바이트에 정렬됩니다.</p>
     *
     * @param size 레코드 크기 (바이트, 양수여야 함)
     * @return 할당된 레코드의 오프셋
     * @throws IllegalArgumentException size가 0 이하인 경우
     * @throws IllegalStateException allocTail 오버플로우 발생 시
     * @deprecated Phase 8에서는 {@link #allocateRecord(long, int)} 사용 권장
     */
    public long allocateRecord(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException(
                "Record size must be positive, but was: " + size
            );
        }

        long aligned = alignUp(currentAllocTail, RECORD_ALIGNMENT);
        long newTail = aligned + size;

        // 오버플로우 체크
        if (aligned > OVERFLOW_THRESHOLD || newTail < aligned) {
            throw new IllegalStateException(
                "Allocation overflow: cannot allocate record at offset " + aligned
            );
        }

        currentAllocTail = newTail;

        // Pending 모드가 아니면 즉시 커밋
        if (!pendingActive) {
            committedAllocTail = newTail;
        }

        return aligned;
    }

    /**
     * BATCH 모드를 시작합니다 (Legacy)
     *
     * <p>이후 할당은 pending 상태로 관리되며, commit() 또는 rollback() 호출 전까지
     * committedAllocTail은 변하지 않습니다.</p>
     *
     * @throws IllegalStateException 이미 BATCH 모드인 경우
     * @deprecated Phase 8에서는 StoreSnapshot 기반 BATCH 모드 사용
     */
    public void beginPending() {
        if (pendingActive) {
            throw new IllegalStateException("Already in pending mode");
        }
        pendingActive = true;
    }

    /**
     * Pending 상태를 커밋합니다 (Legacy)
     *
     * @throws IllegalStateException BATCH 모드가 아닌 경우
     * @deprecated Phase 8에서는 StoreSnapshot 기반 BATCH 모드 사용
     */
    public void commitPending() {
        if (!pendingActive) {
            throw new IllegalStateException("Not in pending mode");
        }

        committedAllocTail = currentAllocTail;
        pendingActive = false;
    }

    /**
     * Pending 상태를 롤백합니다 (Legacy)
     *
     * @throws IllegalStateException BATCH 모드가 아닌 경우
     * @deprecated Phase 8에서는 StoreSnapshot 기반 BATCH 모드 사용
     */
    public void rollbackPending() {
        if (!pendingActive) {
            throw new IllegalStateException("Not in pending mode");
        }

        currentAllocTail = committedAllocTail;
        pendingActive = false;
    }

    /**
     * 현재 allocTail을 반환합니다 (Legacy)
     *
     * @return 현재 allocTail
     * @deprecated Phase 8에서는 StoreSnapshot.getAllocTail() 사용
     */
    public long getAllocTail() {
        return currentAllocTail;
    }

    /**
     * 커밋된 allocTail을 반환합니다 (Legacy)
     *
     * @return 커밋된 allocTail
     * @deprecated Phase 8에서는 StoreSnapshot.getAllocTail() 사용
     */
    public long getCommittedAllocTail() {
        return committedAllocTail;
    }

    /**
     * BATCH 모드 활성화 여부 (Legacy)
     *
     * @return pending 활성화 여부
     * @deprecated Phase 8에서는 StoreSnapshot 기반 BATCH 모드 사용
     */
    public boolean isPendingActive() {
        return pendingActive;
    }

    // ============================================================
    // 유틸리티 메서드
    // ============================================================

    /**
     * 값을 정렬 크기로 올림합니다.
     *
     * @param value 정렬할 값
     * @param alignment 정렬 크기 (2의 거듭제곱이어야 함)
     * @return 정렬된 값
     */
    private static long alignUp(long value, int alignment) {
        return (value + alignment - 1) & ~((long) alignment - 1);
    }

    /**
     * pageSize가 유효한지 검증합니다.
     *
     * @param size 검증할 페이지 크기
     * @return 유효하면 true
     */
    private static boolean isValidPageSize(int size) {
        for (int validSize : VALID_PAGE_SIZES) {
            if (size == validSize) {
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // 할당 결과 (불변 클래스)
    // ============================================================

    /**
     * 할당 결과를 나타내는 불변 클래스
     *
     * <p>Thread-safety: Immutable, 모든 스레드에서 안전하게 사용 가능</p>
     *
     * <h3>사용 예시</h3>
     * <pre>{@code
     * AllocationResult result = allocator.allocatePage(currentTail);
     * long offset = result.offset;           // 할당된 오프셋
     * long pageId = result.pageId;           // 페이지 ID (레코드는 -1)
     * long newTail = result.newAllocTail;    // 새 allocTail
     * }</pre>
     */
    public static final class AllocationResult {

        /**
         * 페이지 ID (페이지 할당 시) 또는 -1 (레코드 할당 시)
         *
         * <p>pageId = offset / pageSize</p>
         */
        public final long pageId;

        /**
         * 할당된 오프셋 (정렬된 위치)
         */
        public final long offset;

        /**
         * 새 allocTail 값
         *
         * <p>이 값을 StoreSnapshot에 반영하여 새 스냅샷을 생성합니다.</p>
         */
        public final long newAllocTail;

        /**
         * AllocationResult 생성
         *
         * @param pageId 페이지 ID (레코드는 -1)
         * @param offset 할당된 오프셋
         * @param newAllocTail 새 allocTail
         */
        public AllocationResult(long pageId, long offset, long newAllocTail) {
            this.pageId = pageId;
            this.offset = offset;
            this.newAllocTail = newAllocTail;
        }

        /**
         * 페이지 ID 반환
         *
         * @return 페이지 ID (레코드 할당의 경우 -1)
         */
        public long getPageId() {
            return pageId;
        }

        /**
         * 할당된 오프셋 반환
         *
         * @return 오프셋
         */
        public long getOffset() {
            return offset;
        }

        /**
         * 새 allocTail 반환
         *
         * @return 새 allocTail
         */
        public long getNewAllocTail() {
            return newAllocTail;
        }

        @Override
        public String toString() {
            return String.format("AllocationResult{pageId=%d, offset=%d, newAllocTail=%d}",
                    pageId, offset, newAllocTail);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AllocationResult that = (AllocationResult) o;
            return pageId == that.pageId &&
                   offset == that.offset &&
                   newAllocTail == that.newAllocTail;
        }

        @Override
        public int hashCode() {
            int result = (int) (pageId ^ (pageId >>> 32));
            result = 31 * result + (int) (offset ^ (offset >>> 32));
            result = 31 * result + (int) (newAllocTail ^ (newAllocTail >>> 32));
            return result;
        }
    }
}
