package com.snoworca.fxstore.migration;

import com.snoworca.fxstore.api.FxStore;
import com.snoworca.fxstore.catalog.CollectionState;
import com.snoworca.fxstore.collection.FxDequeImpl;
import com.snoworca.fxstore.collection.LegacySeqEncoder;
import com.snoworca.fxstore.collection.OrderedSeqEncoder;
import com.snoworca.fxstore.collection.SeqEncoder;
import com.snoworca.fxstore.core.FxStoreImpl;

import java.util.Deque;

/**
 * Deque 마이그레이션 유틸리티
 *
 * <p>v0.6 이전의 LegacySeqEncoder(Little Endian)로 인코딩된 Deque를
 * v0.7+의 OrderedSeqEncoder(XOR + Big Endian)로 마이그레이션합니다.</p>
 *
 * <h3>마이그레이션 효과</h3>
 * <ul>
 *   <li>peekFirst/peekLast: O(n) → O(log n)</li>
 *   <li>size: O(n) → O(1)</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 마이그레이션 필요 여부 확인
 * if (DequeMigrator.needsMigration(store, "myDeque")) {
 *     DequeMigrator.migrate(store, "myDeque", String.class);
 * }
 * }</pre>
 *
 * @since 0.7
 */
public final class DequeMigrator {

    private DequeMigrator() {
        // Utility class
    }

    /**
     * Deque가 마이그레이션이 필요한지 확인
     *
     * <p>LEGACY 인코딩(v0.6 이전)을 사용하는 경우 true 반환</p>
     *
     * @param store FxStore 인스턴스
     * @param dequeName Deque 이름
     * @return 마이그레이션 필요 여부
     */
    public static boolean needsMigration(FxStore store, String dequeName) {
        if (!(store instanceof FxStoreImpl)) {
            return false;
        }

        FxStoreImpl impl = (FxStoreImpl) store;
        CollectionState state = impl.getCollectionState(dequeName);

        if (state == null) {
            return false;
        }

        return state.getSeqEncoderVersion() == CollectionState.SEQ_ENCODER_VERSION_LEGACY;
    }

    /**
     * Deque를 v0.7+ 형식으로 마이그레이션
     *
     * <p>원자적으로 수행됩니다. 실패 시 롤백됩니다.</p>
     *
     * @param store FxStore 인스턴스
     * @param dequeName Deque 이름
     * @param elementClass 요소 타입
     * @param <E> 요소 타입
     * @throws com.snoworca.fxstore.api.FxException 마이그레이션 실패 시
     */
    public static <E> void migrate(FxStore store, String dequeName, Class<E> elementClass) {
        if (!(store instanceof FxStoreImpl)) {
            throw new IllegalArgumentException("FxStoreImpl required");
        }

        if (!needsMigration(store, dequeName)) {
            return; // 이미 마이그레이션됨
        }

        FxStoreImpl storeImpl = (FxStoreImpl) store;

        // 쓰기 락 획득
        long stamp = storeImpl.acquireWriteLock();
        try {
            performMigration(storeImpl, dequeName, elementClass);
        } finally {
            storeImpl.releaseWriteLock(stamp);
        }
    }

    /**
     * 실제 마이그레이션 수행 (락 내에서 호출)
     *
     * <p>이 메서드는 이미 write lock을 획득한 상태에서 호출됩니다.
     * 따라서 FxDequeImpl의 unlocked 메서드들을 사용하여 데드락을 방지합니다.</p>
     */
    @SuppressWarnings("unchecked")
    private static <E> void performMigration(FxStoreImpl store, String dequeName, Class<E> elementClass) {
        // 1. 현재 Deque 열기 (LEGACY 인코딩)
        Deque<E> legacyDeque = store.openDeque(dequeName, elementClass);
        FxDequeImpl<E> legacyImpl = (FxDequeImpl<E>) legacyDeque;

        // 2. 모든 요소 읽기
        java.util.List<E> elements = new java.util.ArrayList<>();
        for (E element : legacyDeque) {
            elements.add(element);
        }

        // 3. 모든 요소 삭제 (unlocked 버전 사용 - 이미 lock 보유)
        while (!legacyDeque.isEmpty()) {
            legacyImpl.pollFirstUnlocked();
        }

        // 4. CollectionState 업데이트: LEGACY → ORDERED
        long collectionId = legacyImpl.getCollectionId();
        CollectionState oldState = store.getCollectionStateById(collectionId);
        CollectionState newState = oldState.withSeqEncoderVersion(CollectionState.SEQ_ENCODER_VERSION_ORDERED);
        store.updateCollectionState(collectionId, newState);

        // 5. FxDequeImpl 내부 SeqEncoder 변경
        legacyImpl.setSeqEncoder(OrderedSeqEncoder.getInstance());

        // 6. 모든 요소 재삽입 (ORDERED 인코딩, unlocked 버전 사용)
        for (E element : elements) {
            legacyImpl.addLastUnlocked(element);
        }

        // 7. 커밋
        store.commitIfAuto();
    }
}
