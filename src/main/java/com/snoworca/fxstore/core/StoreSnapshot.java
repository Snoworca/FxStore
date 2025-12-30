package com.snoworca.fxstore.core;

import com.snoworca.fxstore.catalog.CatalogEntry;
import com.snoworca.fxstore.catalog.CollectionState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 불변 스냅샷 - Store의 특정 시점 상태
 *
 * <p>이 클래스는 FxStore의 모든 메타데이터를 불변 스냅샷으로 캡슐화합니다.
 * 동시성 모델에서 읽기 스레드는 이 스냅샷을 락 없이 안전하게 읽을 수 있습니다.</p>
 *
 * <h3>스레드 안전성 (Thread-safety)</h3>
 * <p><b>Immutable</b> - 생성 후 모든 필드는 변경되지 않습니다.
 * 모든 스레드에서 안전하게 읽기 가능합니다.</p>
 *
 * <h3>불변식 (Invariants)</h3>
 * <ul>
 *   <li><b>INV-C2</b>: 생성 후 모든 필드 변경 불가</li>
 *   <li>모든 Map은 {@link Collections#unmodifiableMap}으로 래핑됨</li>
 *   <li>생성자에서 방어적 복사(defensive copy) 수행</li>
 * </ul>
 *
 * <h3>사용 패턴</h3>
 * <pre>{@code
 * // 읽기 (Wait-free)
 * StoreSnapshot snap = store.snapshot();
 * CatalogEntry entry = snap.getCatalog().get("myCollection");
 *
 * // 쓰기 (새 스냅샷 생성)
 * StoreSnapshot newSnap = snap.withAllocTail(newTail);
 * store.publishSnapshot(newSnap);
 * }</pre>
 *
 * @since 0.4 (Phase 8 - 동시성 지원)
 * @see com.snoworca.fxstore.core.FxStoreImpl#snapshot()
 */
public final class StoreSnapshot {

    /**
     * 커밋 시퀀스 번호 (단조 증가)
     *
     * <p>INV-1: seqNo는 커밋마다 1씩 증가하며 절대 감소하지 않습니다.</p>
     */
    private final long seqNo;

    /**
     * 할당 위치 (Allocator tail)
     *
     * <p>INV-9: allocTail은 항상 증가만 합니다 (컴팩션 제외).</p>
     */
    private final long allocTail;

    /**
     * Catalog: 컬렉션 이름 → CatalogEntry 매핑 (불변)
     */
    private final Map<String, CatalogEntry> catalog;

    /**
     * CollectionStates: collectionId → CollectionState 매핑 (불변)
     */
    private final Map<Long, CollectionState> states;

    /**
     * 컬렉션별 루트 페이지 ID: collectionId → rootPageId 매핑 (불변)
     *
     * <p>BTree/OST의 루트 페이지 ID를 추적합니다.
     * COW 연산 후 새로운 루트가 생성되면 이 맵이 업데이트된 새 스냅샷이 생성됩니다.</p>
     */
    private final Map<Long, Long> rootPageIds;

    /**
     * 다음 컬렉션 ID (신규 컬렉션 생성 시 사용)
     */
    private final long nextCollectionId;

    /**
     * 스냅샷 생성 (방어적 복사 + 불변 래핑)
     *
     * <p>전달된 모든 Map은 복사되어 외부 변경에 영향받지 않습니다.</p>
     *
     * @param seqNo 커밋 시퀀스 번호
     * @param allocTail 할당 위치
     * @param catalog 컬렉션 카탈로그 (복사됨)
     * @param states 컬렉션 상태 맵 (복사됨)
     * @param rootPageIds 루트 페이지 ID 맵 (복사됨)
     * @param nextCollectionId 다음 컬렉션 ID
     * @throws NullPointerException catalog, states, rootPageIds가 null인 경우
     */
    public StoreSnapshot(
        long seqNo,
        long allocTail,
        Map<String, CatalogEntry> catalog,
        Map<Long, CollectionState> states,
        Map<Long, Long> rootPageIds,
        long nextCollectionId
    ) {
        this.seqNo = seqNo;
        this.allocTail = allocTail;
        // 방어적 복사 + 불변 래핑
        this.catalog = Collections.unmodifiableMap(
            new HashMap<>(Objects.requireNonNull(catalog, "catalog")));
        this.states = Collections.unmodifiableMap(
            new HashMap<>(Objects.requireNonNull(states, "states")));
        this.rootPageIds = Collections.unmodifiableMap(
            new HashMap<>(Objects.requireNonNull(rootPageIds, "rootPageIds")));
        this.nextCollectionId = nextCollectionId;
    }

    // ==================== With 메서드 (새 스냅샷 생성) ====================

    /**
     * 루트 페이지 ID를 변경한 새 스냅샷 생성
     *
     * <p>COW 연산 후 BTree/OST의 새로운 루트 페이지 ID를 반영합니다.
     * seqNo는 자동으로 1 증가합니다.</p>
     *
     * @param collectionId 컬렉션 ID
     * @param newRootPageId 새로운 루트 페이지 ID
     * @return 변경된 새 스냅샷 (원본 불변)
     */
    public StoreSnapshot withRootPageId(long collectionId, long newRootPageId) {
        Map<Long, Long> newRoots = new HashMap<>(this.rootPageIds);
        newRoots.put(collectionId, newRootPageId);
        return new StoreSnapshot(
            this.seqNo + 1,
            this.allocTail,
            this.catalog,
            this.states,
            newRoots,
            this.nextCollectionId
        );
    }

    /**
     * allocTail을 변경한 새 스냅샷 생성
     *
     * <p>페이지/레코드 할당 후 새로운 allocTail을 반영합니다.
     * seqNo는 자동으로 1 증가합니다.</p>
     *
     * @param newAllocTail 새로운 allocTail
     * @return 변경된 새 스냅샷 (원본 불변)
     */
    public StoreSnapshot withAllocTail(long newAllocTail) {
        return new StoreSnapshot(
            this.seqNo + 1,
            newAllocTail,
            this.catalog,
            this.states,
            this.rootPageIds,
            this.nextCollectionId
        );
    }

    /**
     * rootPageId와 allocTail을 동시에 변경한 새 스냅샷 생성
     *
     * <p>COW 연산은 보통 새 페이지 할당과 루트 변경이 함께 발생하므로
     * 이 메서드로 한 번에 처리할 수 있습니다.</p>
     *
     * @param collectionId 컬렉션 ID
     * @param newRootPageId 새로운 루트 페이지 ID
     * @param newAllocTail 새로운 allocTail
     * @return 변경된 새 스냅샷 (원본 불변)
     */
    public StoreSnapshot withRootAndAllocTail(long collectionId, long newRootPageId, long newAllocTail) {
        Map<Long, Long> newRoots = new HashMap<>(this.rootPageIds);
        newRoots.put(collectionId, newRootPageId);
        return new StoreSnapshot(
            this.seqNo + 1,
            newAllocTail,
            this.catalog,
            this.states,
            newRoots,
            this.nextCollectionId
        );
    }

    /**
     * rootPageId, count, allocTail을 동시에 변경한 새 스냅샷 생성
     *
     * <p>v0.7+: Deque 연산 시 rootPageId와 count를 함께 업데이트합니다.
     * 이를 통해 size() 연산이 O(1)로 동작합니다.</p>
     *
     * @param collectionId 컬렉션 ID
     * @param newRootPageId 새로운 루트 페이지 ID
     * @param newCount 새로운 count
     * @param newAllocTail 새로운 allocTail
     * @return 변경된 새 스냅샷 (원본 불변)
     * @since 0.7
     */
    public StoreSnapshot withRootCountAndAllocTail(long collectionId, long newRootPageId, long newCount, long newAllocTail) {
        Map<Long, Long> newRoots = new HashMap<>(this.rootPageIds);
        newRoots.put(collectionId, newRootPageId);

        // CollectionState의 count도 업데이트
        Map<Long, CollectionState> newStates = new HashMap<>(this.states);
        CollectionState oldState = this.states.get(collectionId);
        if (oldState != null) {
            CollectionState updatedState = oldState.withRootAndCount(newRootPageId, newCount);
            newStates.put(collectionId, updatedState);
        }

        return new StoreSnapshot(
            this.seqNo + 1,
            newAllocTail,
            this.catalog,
            newStates,
            newRoots,
            this.nextCollectionId
        );
    }

    /**
     * CollectionState를 변경한 새 스냅샷 생성
     *
     * @param collectionId 컬렉션 ID
     * @param newState 새로운 상태
     * @return 변경된 새 스냅샷 (원본 불변)
     */
    public StoreSnapshot withState(long collectionId, CollectionState newState) {
        Map<Long, CollectionState> newStates = new HashMap<>(this.states);
        newStates.put(collectionId, newState);
        return new StoreSnapshot(
            this.seqNo + 1,
            this.allocTail,
            this.catalog,
            newStates,
            this.rootPageIds,
            this.nextCollectionId
        );
    }

    /**
     * Catalog 엔트리를 추가한 새 스냅샷 생성
     *
     * @param name 컬렉션 이름
     * @param entry 카탈로그 엔트리
     * @return 변경된 새 스냅샷 (원본 불변)
     */
    public StoreSnapshot withCatalogEntry(String name, CatalogEntry entry) {
        Map<String, CatalogEntry> newCatalog = new HashMap<>(this.catalog);
        newCatalog.put(name, entry);
        return new StoreSnapshot(
            this.seqNo + 1,
            this.allocTail,
            newCatalog,
            this.states,
            this.rootPageIds,
            this.nextCollectionId
        );
    }

    /**
     * Catalog 엔트리를 삭제한 새 스냅샷 생성
     *
     * @param name 삭제할 컬렉션 이름
     * @return 변경된 새 스냅샷 (원본 불변)
     */
    public StoreSnapshot withoutCatalogEntry(String name) {
        Map<String, CatalogEntry> newCatalog = new HashMap<>(this.catalog);
        newCatalog.remove(name);
        return new StoreSnapshot(
            this.seqNo + 1,
            this.allocTail,
            newCatalog,
            this.states,
            this.rootPageIds,
            this.nextCollectionId
        );
    }

    /**
     * nextCollectionId를 변경한 새 스냅샷 생성
     *
     * @param newNextCollectionId 새로운 nextCollectionId
     * @return 변경된 새 스냅샷 (원본 불변)
     */
    public StoreSnapshot withNextCollectionId(long newNextCollectionId) {
        return new StoreSnapshot(
            this.seqNo + 1,
            this.allocTail,
            this.catalog,
            this.states,
            this.rootPageIds,
            newNextCollectionId
        );
    }

    /**
     * 복합 업데이트: 새 컬렉션 생성 시 사용
     *
     * <p>카탈로그 엔트리, 상태, 루트 페이지 ID, nextCollectionId를
     * 한 번에 업데이트합니다.</p>
     *
     * @param name 컬렉션 이름
     * @param entry 카탈로그 엔트리
     * @param state 컬렉션 상태
     * @param rootPageId 초기 루트 페이지 ID
     * @param newNextCollectionId 새로운 nextCollectionId
     * @return 변경된 새 스냅샷 (원본 불변)
     */
    public StoreSnapshot withNewCollection(
            String name,
            CatalogEntry entry,
            CollectionState state,
            long rootPageId,
            long newNextCollectionId) {
        Map<String, CatalogEntry> newCatalog = new HashMap<>(this.catalog);
        newCatalog.put(name, entry);

        Map<Long, CollectionState> newStates = new HashMap<>(this.states);
        newStates.put(state.getCollectionId(), state);

        Map<Long, Long> newRoots = new HashMap<>(this.rootPageIds);
        newRoots.put(state.getCollectionId(), rootPageId);

        return new StoreSnapshot(
            this.seqNo + 1,
            this.allocTail,
            newCatalog,
            newStates,
            newRoots,
            newNextCollectionId
        );
    }

    /**
     * 컬렉션 삭제 시 사용
     *
     * <p>카탈로그, 상태, 루트 페이지 ID에서 해당 컬렉션을 제거합니다.</p>
     *
     * @param name 컬렉션 이름
     * @param collectionId 컬렉션 ID
     * @return 변경된 새 스냅샷 (원본 불변)
     */
    public StoreSnapshot withoutCollection(String name, long collectionId) {
        Map<String, CatalogEntry> newCatalog = new HashMap<>(this.catalog);
        newCatalog.remove(name);

        Map<Long, CollectionState> newStates = new HashMap<>(this.states);
        newStates.remove(collectionId);

        Map<Long, Long> newRoots = new HashMap<>(this.rootPageIds);
        newRoots.remove(collectionId);

        return new StoreSnapshot(
            this.seqNo + 1,
            this.allocTail,
            newCatalog,
            newStates,
            newRoots,
            this.nextCollectionId
        );
    }

    // ==================== Getters (모두 불변 값 반환) ====================

    /**
     * 커밋 시퀀스 번호 반환
     *
     * @return 현재 seqNo
     */
    public long getSeqNo() {
        return seqNo;
    }

    /**
     * 할당 위치 반환
     *
     * @return 현재 allocTail
     */
    public long getAllocTail() {
        return allocTail;
    }

    /**
     * 카탈로그 반환 (불변 뷰)
     *
     * <p><b>주의:</b> 반환된 Map은 수정할 수 없습니다.
     * 수정 시도 시 {@link UnsupportedOperationException}이 발생합니다.</p>
     *
     * @return 카탈로그 (unmodifiable)
     */
    public Map<String, CatalogEntry> getCatalog() {
        return catalog;
    }

    /**
     * 컬렉션 상태 맵 반환 (불변 뷰)
     *
     * <p><b>주의:</b> 반환된 Map은 수정할 수 없습니다.</p>
     *
     * @return 상태 맵 (unmodifiable)
     */
    public Map<Long, CollectionState> getStates() {
        return states;
    }

    /**
     * 루트 페이지 ID 맵 반환 (불변 뷰)
     *
     * @return 루트 페이지 ID 맵 (unmodifiable)
     */
    public Map<Long, Long> getRootPageIds() {
        return rootPageIds;
    }

    /**
     * 특정 컬렉션의 루트 페이지 ID 반환
     *
     * @param collectionId 컬렉션 ID
     * @return 루트 페이지 ID 또는 null (없는 경우)
     */
    public Long getRootPageId(long collectionId) {
        return rootPageIds.get(collectionId);
    }

    /**
     * 특정 컬렉션의 CollectionState 반환
     *
     * @param collectionId 컬렉션 ID
     * @return CollectionState 또는 null (없는 경우)
     */
    public CollectionState getState(long collectionId) {
        return states.get(collectionId);
    }

    /**
     * 다음 컬렉션 ID 반환
     *
     * @return nextCollectionId
     */
    public long getNextCollectionId() {
        return nextCollectionId;
    }

    // ==================== Utility ====================

    @Override
    public String toString() {
        return "StoreSnapshot{" +
                "seqNo=" + seqNo +
                ", allocTail=" + allocTail +
                ", catalogSize=" + catalog.size() +
                ", statesSize=" + states.size() +
                ", rootPageIdsSize=" + rootPageIds.size() +
                ", nextCollectionId=" + nextCollectionId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoreSnapshot)) return false;
        StoreSnapshot that = (StoreSnapshot) o;
        return seqNo == that.seqNo &&
               allocTail == that.allocTail &&
               nextCollectionId == that.nextCollectionId &&
               Objects.equals(catalog, that.catalog) &&
               Objects.equals(states, that.states) &&
               Objects.equals(rootPageIds, that.rootPageIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seqNo, allocTail, catalog, states, rootPageIds, nextCollectionId);
    }
}
