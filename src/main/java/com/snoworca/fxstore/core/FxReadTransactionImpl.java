package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.FxErrorCode;
import com.snoworca.fxstore.api.FxException;
import com.snoworca.fxstore.api.FxReadTransaction;
import com.snoworca.fxstore.btree.BTree;
import com.snoworca.fxstore.btree.BTreeCursor;
import java.util.Iterator;
import com.snoworca.fxstore.catalog.CollectionState;
import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.collection.FxCollection;
import com.snoworca.fxstore.collection.FxDequeImpl;
import com.snoworca.fxstore.collection.FxList;
import com.snoworca.fxstore.collection.FxNavigableMapImpl;
import com.snoworca.fxstore.collection.FxNavigableSetImpl;
import com.snoworca.fxstore.ost.OST;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 읽기 전용 트랜잭션 구현
 *
 * <p>트랜잭션 시작 시점의 스냅샷을 고정하여 일관된 읽기 뷰를 제공합니다.</p>
 *
 * <h3>불변식 (Invariants)</h3>
 * <ul>
 *   <li><b>INV-RT1</b>: 트랜잭션 내 스냅샷은 절대 변경 불가</li>
 *   <li><b>INV-RT2</b>: 동일 트랜잭션 내 모든 읽기는 동일 스냅샷 사용</li>
 *   <li><b>INV-RT3</b>: 다른 스레드의 쓰기가 활성 트랜잭션에 영향 없음</li>
 *   <li><b>INV-RT4</b>: close() 후 모든 연산은 예외 발생</li>
 *   <li><b>INV-RT5</b>: 트랜잭션은 생성된 store의 컬렉션만 접근 가능</li>
 * </ul>
 *
 * @since 0.6
 */
public class FxReadTransactionImpl implements FxReadTransaction {

    /** 이 트랜잭션을 소유한 Store */
    private final FxStoreImpl store;

    /** 트랜잭션 시작 시점의 스냅샷 (불변) */
    private final StoreSnapshot snapshot;

    /** 트랜잭션 활성 상태 */
    private volatile boolean active = true;

    /**
     * 읽기 트랜잭션 생성
     *
     * @param store 소유 Store
     * @param snapshot 트랜잭션 시작 시점의 스냅샷
     */
    public FxReadTransactionImpl(FxStoreImpl store, StoreSnapshot snapshot) {
        this.store = Objects.requireNonNull(store, "store");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    // ==================== 공통 헬퍼 ====================

    /**
     * 트랜잭션이 활성 상태인지 확인
     *
     * @throws IllegalStateException 트랜잭션이 닫힌 경우
     */
    private void checkActive() {
        if (!active) {
            throw new IllegalStateException("Read transaction is already closed");
        }
    }

    /**
     * 컬렉션이 이 Store에 속하는지 검증
     *
     * @param collection 대상 컬렉션
     * @return FxCollection 인터페이스
     * @throws IllegalArgumentException 다른 Store의 컬렉션인 경우
     */
    private FxCollection validateCollection(Object collection) {
        if (!(collection instanceof FxCollection)) {
            throw new IllegalArgumentException(
                "Collection is not an FxStore collection: " + collection.getClass().getName());
        }
        FxCollection fxColl = (FxCollection) collection;
        if (fxColl.getStore() != store) {
            throw new IllegalArgumentException(
                "Collection belongs to a different FxStore instance");
        }
        return fxColl;
    }

    /**
     * 스냅샷에서 컬렉션의 루트 페이지 ID 획득
     *
     * @param collectionId 컬렉션 ID
     * @return 루트 페이지 ID (0이면 빈 컬렉션)
     */
    private long getRootPageId(long collectionId) {
        Long rootPageId = snapshot.getRootPageId(collectionId);
        return rootPageId != null ? rootPageId : 0;
    }

    /**
     * BTree 생성 (스냅샷의 rootPageId 사용하지 않음 - 별도로 전달)
     */
    private BTree createBTree(long collectionId) {
        Comparator<byte[]> byteComparator = (a, b) -> {
            int minLen = Math.min(a.length, b.length);
            for (int i = 0; i < minLen; i++) {
                int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return a.length - b.length;
        };

        return new BTree(
            store.getStorage(),
            store.getPageSize(),
            byteComparator,
            0  // rootPageId는 각 연산에서 별도로 제공
        );
    }

    // ==================== Map 연산 ====================

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> V get(NavigableMap<K, V> map, K key) {
        checkActive();
        FxCollection fxColl = validateCollection(map);

        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        FxNavigableMapImpl<K, V> impl = (FxNavigableMapImpl<K, V>) map;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return null;  // 빈 Map
        }

        // 키 인코딩
        byte[] keyBytes = encodeKey(impl, key);

        // 스냅샷 기반 검색
        BTree btree = createBTree(collectionId);
        Long valueRecordId = btree.findWithRoot(rootPageId, keyBytes);

        if (valueRecordId == null) {
            return null;
        }

        byte[] valueBytes = store.readValueRecord(valueRecordId);
        return decodeValue(impl, valueBytes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> boolean containsKey(NavigableMap<K, V> map, K key) {
        checkActive();
        FxCollection fxColl = validateCollection(map);

        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        FxNavigableMapImpl<K, V> impl = (FxNavigableMapImpl<K, V>) map;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return false;
        }

        byte[] keyBytes = encodeKey(impl, key);
        BTree btree = createBTree(collectionId);
        return btree.findWithRoot(rootPageId, keyBytes) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Map.Entry<K, V> firstEntry(NavigableMap<K, V> map) {
        checkActive();
        FxCollection fxColl = validateCollection(map);

        FxNavigableMapImpl<K, V> impl = (FxNavigableMapImpl<K, V>) map;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return null;
        }

        BTree btree = createBTree(collectionId);
        BTreeCursor cursor = btree.cursorWithRoot(rootPageId);

        if (!cursor.hasNext()) {
            return null;
        }

        BTree.Entry entry = cursor.next();
        K key = decodeKey(impl, entry.getKey());
        byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
        V value = decodeValue(impl, valueBytes);

        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Map.Entry<K, V> lastEntry(NavigableMap<K, V> map) {
        checkActive();
        FxCollection fxColl = validateCollection(map);

        FxNavigableMapImpl<K, V> impl = (FxNavigableMapImpl<K, V>) map;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return null;
        }

        // 역순 커서로 마지막 엔트리 획득
        BTree btree = createBTree(collectionId);
        Iterator<BTree.Entry> cursor = btree.descendingCursorWithRoot(rootPageId);

        if (!cursor.hasNext()) {
            return null;
        }

        BTree.Entry entry = cursor.next();
        K key = decodeKey(impl, entry.getKey());
        byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
        V value = decodeValue(impl, valueBytes);

        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    @Override
    public <K, V> int size(NavigableMap<K, V> map) {
        checkActive();
        FxCollection fxColl = validateCollection(map);

        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return 0;
        }

        // BTree 순회하여 카운트
        BTree btree = createBTree(collectionId);
        BTreeCursor cursor = btree.cursorWithRoot(rootPageId);
        int count = 0;
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
        return count;
    }

    // ==================== Set 연산 ====================

    @Override
    @SuppressWarnings("unchecked")
    public <E> boolean contains(NavigableSet<E> set, E element) {
        checkActive();
        FxCollection fxColl = validateCollection(set);

        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }

        // Set은 내부적으로 Map<E, Boolean>로 구현됨
        FxNavigableSetImpl<E> impl = (FxNavigableSetImpl<E>) set;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return false;
        }

        // Set의 내부 map을 통해 keyCodec 접근
        byte[] keyBytes = encodeSetElement(impl, element);

        BTree btree = createBTree(collectionId);
        return btree.findWithRoot(rootPageId, keyBytes) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> E first(NavigableSet<E> set) {
        checkActive();
        FxCollection fxColl = validateCollection(set);

        FxNavigableSetImpl<E> impl = (FxNavigableSetImpl<E>) set;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return null;
        }

        BTree btree = createBTree(collectionId);
        BTreeCursor cursor = btree.cursorWithRoot(rootPageId);

        if (!cursor.hasNext()) {
            return null;
        }

        BTree.Entry entry = cursor.next();
        return decodeSetElement(impl, entry.getKey());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> E last(NavigableSet<E> set) {
        checkActive();
        FxCollection fxColl = validateCollection(set);

        FxNavigableSetImpl<E> impl = (FxNavigableSetImpl<E>) set;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return null;
        }

        BTree btree = createBTree(collectionId);
        Iterator<BTree.Entry> cursor = btree.descendingCursorWithRoot(rootPageId);

        if (!cursor.hasNext()) {
            return null;
        }

        BTree.Entry entry = cursor.next();
        return decodeSetElement(impl, entry.getKey());
    }

    @Override
    public <E> int size(NavigableSet<E> set) {
        checkActive();
        FxCollection fxColl = validateCollection(set);

        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return 0;
        }

        BTree btree = createBTree(collectionId);
        BTreeCursor cursor = btree.cursorWithRoot(rootPageId);
        int count = 0;
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
        return count;
    }

    // ==================== List 연산 ====================

    @Override
    @SuppressWarnings("unchecked")
    public <E> E get(List<E> list, int index) {
        checkActive();
        FxCollection fxColl = validateCollection(list);

        if (index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        FxList<E> impl = (FxList<E>) list;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
        }

        // OST를 통한 인덱스 접근
        OST ost = new OST(store.getStorage(), store.getAllocator(), store.getPageSize());
        int size = ost.sizeWithRoot(rootPageId);

        if (index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        long recordId = ost.getWithRoot(rootPageId, index);
        byte[] data = readListRecord(recordId);
        return decodeListElement(impl, data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> int size(List<E> list) {
        checkActive();
        FxCollection fxColl = validateCollection(list);

        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return 0;
        }

        OST ost = new OST(store.getStorage(), store.getAllocator(), store.getPageSize());
        return ost.sizeWithRoot(rootPageId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> int indexOf(List<E> list, E element) {
        checkActive();
        FxCollection fxColl = validateCollection(list);

        if (element == null) {
            return -1;
        }

        FxList<E> impl = (FxList<E>) list;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return -1;
        }

        OST ost = new OST(store.getStorage(), store.getAllocator(), store.getPageSize());
        int size = ost.sizeWithRoot(rootPageId);

        for (int i = 0; i < size; i++) {
            long recordId = ost.getWithRoot(rootPageId, i);
            byte[] data = readListRecord(recordId);
            E current = decodeListElement(impl, data);
            if (element.equals(current)) {
                return i;
            }
        }

        return -1;
    }

    // ==================== Deque 연산 ====================

    /**
     * Deque의 첫 번째 요소 조회 (스냅샷 기반)
     *
     * <p>v0.7 OrderedSeqEncoder 사용 시: O(log n) - BTree first
     * <p>v0.6 LegacySeqEncoder 사용 시: O(n) - 전체 순회
     *
     * @param deque 대상 Deque
     * @return 첫 번째 요소 또는 null (빈 경우)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <E> E peekFirst(Deque<E> deque) {
        checkActive();
        FxCollection fxColl = validateCollection(deque);

        FxDequeImpl<E> impl = (FxDequeImpl<E>) deque;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return null;
        }

        BTree btree = createBTree(collectionId);

        // v0.7: OrderedSeqEncoder 사용 시 O(log n)
        // INV-DQ1: 바이트 순서 = 논리적 순서이므로 BTree.first() = 논리적 첫 번째
        if (isOrderedEncoding(impl)) {
            BTree.Entry entry = btree.firstEntryWithRoot(rootPageId);
            if (entry == null) {
                return null;
            }
            byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
            return decodeDequeElement(impl, valueBytes);
        }

        // v0.6 호환: LegacySeqEncoder 사용 시 O(n) 전체 순회
        BTreeCursor cursor = btree.cursorWithRoot(rootPageId);
        Long minSeq = null;
        BTree.Entry minEntry = null;

        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            long seq = decodeSeqLegacy(entry.getKey());
            if (minSeq == null || seq < minSeq) {
                minSeq = seq;
                minEntry = entry;
            }
        }

        if (minEntry == null) {
            return null;
        }

        byte[] valueBytes = store.readValueRecord(minEntry.getValueRecordId());
        return decodeDequeElement(impl, valueBytes);
    }

    /**
     * Deque의 마지막 요소 조회 (스냅샷 기반)
     *
     * <p>v0.7 OrderedSeqEncoder 사용 시: O(log n) - BTree last
     * <p>v0.6 LegacySeqEncoder 사용 시: O(n) - 전체 순회
     *
     * @param deque 대상 Deque
     * @return 마지막 요소 또는 null (빈 경우)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <E> E peekLast(Deque<E> deque) {
        checkActive();
        FxCollection fxColl = validateCollection(deque);

        FxDequeImpl<E> impl = (FxDequeImpl<E>) deque;
        long collectionId = fxColl.getCollectionId();
        long rootPageId = getRootPageId(collectionId);

        if (rootPageId == 0) {
            return null;
        }

        BTree btree = createBTree(collectionId);

        // v0.7: OrderedSeqEncoder 사용 시 O(log n)
        // INV-DQ1: 바이트 순서 = 논리적 순서이므로 BTree.last() = 논리적 마지막
        if (isOrderedEncoding(impl)) {
            BTree.Entry entry = btree.lastEntryWithRoot(rootPageId);
            if (entry == null) {
                return null;
            }
            byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
            return decodeDequeElement(impl, valueBytes);
        }

        // v0.6 호환: LegacySeqEncoder 사용 시 O(n) 전체 순회
        BTreeCursor cursor = btree.cursorWithRoot(rootPageId);
        Long maxSeq = null;
        BTree.Entry maxEntry = null;

        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            long seq = decodeSeqLegacy(entry.getKey());
            if (maxSeq == null || seq > maxSeq) {
                maxSeq = seq;
                maxEntry = entry;
            }
        }

        if (maxEntry == null) {
            return null;
        }

        byte[] valueBytes = store.readValueRecord(maxEntry.getValueRecordId());
        return decodeDequeElement(impl, valueBytes);
    }

    @Override
    public <E> int size(Deque<E> deque) {
        checkActive();
        FxCollection fxColl = validateCollection(deque);

        long collectionId = fxColl.getCollectionId();

        // v0.7: 스냅샷에서 캐싱된 count 조회 (O(1))
        com.snoworca.fxstore.catalog.CollectionState state = snapshot.getState(collectionId);
        if (state != null) {
            return (int) state.getCount();
        }

        // Fallback: 스냅샷에 상태가 없는 경우 (마이그레이션 중)
        long rootPageId = getRootPageId(collectionId);
        if (rootPageId == 0) {
            return 0;
        }

        BTree btree = createBTree(collectionId);
        BTreeCursor cursor = btree.cursorWithRoot(rootPageId);
        int count = 0;
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
        return count;
    }

    /**
     * Deque가 OrderedSeqEncoder를 사용하는지 확인
     *
     * @param deque 대상 Deque
     * @return OrderedSeqEncoder 사용 여부
     */
    private <E> boolean isOrderedEncoding(FxDequeImpl<E> deque) {
        return deque.getSeqEncoder() instanceof com.snoworca.fxstore.collection.OrderedSeqEncoder;
    }

    // ==================== 트랜잭션 관리 ====================

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public long getSnapshotSeqNo() {
        return snapshot.getSeqNo();
    }

    @Override
    public void close() {
        // 멱등성: 이미 닫힌 경우 무시
        active = false;
    }

    // ==================== 코덱 헬퍼 (리플렉션 기반) ====================

    /**
     * Map 키 인코딩
     */
    @SuppressWarnings("unchecked")
    private <K, V> byte[] encodeKey(FxNavigableMapImpl<K, V> map, K key) {
        try {
            java.lang.reflect.Field field = FxNavigableMapImpl.class.getDeclaredField("keyCodec");
            field.setAccessible(true);
            FxCodec<K> codec = (FxCodec<K>) field.get(map);
            return codec.encode(key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode key", e);
        }
    }

    /**
     * Map 키 디코딩
     */
    @SuppressWarnings("unchecked")
    private <K, V> K decodeKey(FxNavigableMapImpl<K, V> map, byte[] keyBytes) {
        try {
            java.lang.reflect.Field field = FxNavigableMapImpl.class.getDeclaredField("keyCodec");
            field.setAccessible(true);
            FxCodec<K> codec = (FxCodec<K>) field.get(map);
            return codec.decode(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode key", e);
        }
    }

    /**
     * Map 값 디코딩
     */
    @SuppressWarnings("unchecked")
    private <K, V> V decodeValue(FxNavigableMapImpl<K, V> map, byte[] valueBytes) {
        try {
            java.lang.reflect.Field field = FxNavigableMapImpl.class.getDeclaredField("valueCodec");
            field.setAccessible(true);
            FxCodec<V> codec = (FxCodec<V>) field.get(map);
            return codec.decode(valueBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode value", e);
        }
    }

    /**
     * List 요소 디코딩
     */
    @SuppressWarnings("unchecked")
    private <E> E decodeListElement(FxList<E> list, byte[] data) {
        try {
            java.lang.reflect.Field field = FxList.class.getDeclaredField("codec");
            field.setAccessible(true);
            FxCodec<E> codec = (FxCodec<E>) field.get(list);
            return codec.decode(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode list element", e);
        }
    }

    /**
     * List 레코드 읽기
     */
    private byte[] readListRecord(long recordId) {
        return store.readValueRecord(recordId);
    }

    /**
     * Deque 요소 디코딩
     */
    @SuppressWarnings("unchecked")
    private <E> E decodeDequeElement(FxDequeImpl<E> deque, byte[] data) {
        try {
            java.lang.reflect.Field field = FxDequeImpl.class.getDeclaredField("elementCodec");
            field.setAccessible(true);
            FxCodec<E> codec = (FxCodec<E>) field.get(deque);
            return codec.decode(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode deque element", e);
        }
    }

    /**
     * Set 요소 인코딩 (내부 map의 keyCodec 사용)
     */
    @SuppressWarnings("unchecked")
    private <E> byte[] encodeSetElement(FxNavigableSetImpl<E> set, E element) {
        try {
            // Set의 내부 map 필드 접근
            java.lang.reflect.Field mapField = FxNavigableSetImpl.class.getDeclaredField("map");
            mapField.setAccessible(true);
            FxNavigableMapImpl<E, Boolean> internalMap = (FxNavigableMapImpl<E, Boolean>) mapField.get(set);

            // Map의 keyCodec 사용
            java.lang.reflect.Field codecField = FxNavigableMapImpl.class.getDeclaredField("keyCodec");
            codecField.setAccessible(true);
            FxCodec<E> codec = (FxCodec<E>) codecField.get(internalMap);
            return codec.encode(element);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode set element", e);
        }
    }

    /**
     * Set 요소 디코딩 (내부 map의 keyCodec 사용)
     */
    @SuppressWarnings("unchecked")
    private <E> E decodeSetElement(FxNavigableSetImpl<E> set, byte[] data) {
        try {
            // Set의 내부 map 필드 접근
            java.lang.reflect.Field mapField = FxNavigableSetImpl.class.getDeclaredField("map");
            mapField.setAccessible(true);
            FxNavigableMapImpl<E, Boolean> internalMap = (FxNavigableMapImpl<E, Boolean>) mapField.get(set);

            // Map의 keyCodec 사용
            java.lang.reflect.Field codecField = FxNavigableMapImpl.class.getDeclaredField("keyCodec");
            codecField.setAccessible(true);
            FxCodec<E> codec = (FxCodec<E>) codecField.get(internalMap);
            return codec.decode(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode set element", e);
        }
    }

    /**
     * 레거시 시퀀스 디코딩 (Little Endian long, v0.6 호환)
     */
    private long decodeSeqLegacy(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf.getLong();
    }
}
