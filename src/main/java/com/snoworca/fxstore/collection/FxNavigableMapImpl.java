package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.CommitMode;
import com.snoworca.fxstore.api.FxErrorCode;
import com.snoworca.fxstore.api.FxException;
import com.snoworca.fxstore.btree.BTree;
import com.snoworca.fxstore.btree.BTreeCursor;
import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.core.CodecUpgradeContext;
import com.snoworca.fxstore.core.FxStoreImpl;
import com.snoworca.fxstore.core.StoreSnapshot;

import java.util.*;

/**
 * BTree 기반 NavigableMap 구현
 *
 * <p>SOLID 준수:
 * - SRP: NavigableMap 연산만 담당
 * - DIP: BTree와 FxCodec 인터페이스에 의존
 *
 * @param <K> 키 타입
 * @param <V> 값 타입
 */
public class FxNavigableMapImpl<K, V> implements NavigableMap<K, V>, FxCollection {

    private final FxStoreImpl store;
    private final long collectionId;
    private final FxCodec<K> keyCodec;
    private final FxCodec<V> valueCodec;
    private final Comparator<K> keyComparator;
    private final CodecUpgradeContext keyUpgradeContext;
    private final CodecUpgradeContext valueUpgradeContext;

    /**
     * 생성자
     *
     * @param store FxStore 구현
     * @param collectionId 컬렉션 ID
     * @param keyCodec 키 코덱
     * @param valueCodec 값 코덱
     * @param keyComparator 키 비교자
     * @param keyUpgradeContext 키 업그레이드 컨텍스트 (null 가능)
     * @param valueUpgradeContext 값 업그레이드 컨텍스트 (null 가능)
     */
    public FxNavigableMapImpl(FxStoreImpl store, long collectionId,
                              FxCodec<K> keyCodec, FxCodec<V> valueCodec,
                              Comparator<K> keyComparator,
                              CodecUpgradeContext keyUpgradeContext,
                              CodecUpgradeContext valueUpgradeContext) {
        this.store = store;
        this.collectionId = collectionId;
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
        this.keyComparator = keyComparator;
        this.keyUpgradeContext = keyUpgradeContext;
        this.valueUpgradeContext = valueUpgradeContext;
    }

    // ==================== FxCollection 구현 ====================

    @Override
    public long getCollectionId() {
        return collectionId;
    }

    @Override
    public FxStoreImpl getStore() {
        return store;
    }

    // ==================== 내부 헬퍼 ====================

    private BTree getBTree() {
        // 코덱 기반 비교 사용 - signed 숫자 타입에서 올바른 정렬 순서 보장
        return store.getBTreeForCollection(collectionId, keyCodec);
    }

    /**
     * 현재 스냅샷의 루트 페이지 ID 반환 (Wait-free)
     *
     * <p><b>INV-C3</b>: 읽기 전용, 락 없이 안전하게 호출 가능</p>
     *
     * @return 현재 루트 페이지 ID (없으면 0)
     */
    private long getCurrentRootPageId() {
        Long rootPageId = store.snapshot().getRootPageId(collectionId);
        return rootPageId != null ? rootPageId : 0;
    }

    private byte[] encodeKey(K key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        return keyCodec.encode(key);
    }
    
    private K decodeKey(byte[] keyBytes) {
        // 업그레이드 적용
        if (keyUpgradeContext != null) {
            keyBytes = keyUpgradeContext.upgradeIfNeeded(keyBytes);
        }
        return keyCodec.decode(keyBytes);
    }

    private byte[] encodeValue(V value) {
        if (value == null) {
            throw new NullPointerException("Value cannot be null");
        }
        return valueCodec.encode(value);
    }

    private V decodeValue(byte[] valueBytes) {
        // 업그레이드 적용
        if (valueUpgradeContext != null) {
            valueBytes = valueUpgradeContext.upgradeIfNeeded(valueBytes);
        }
        return valueCodec.decode(valueBytes);
    }
    
    /**
     * 지정된 키에 매핑된 값을 반환합니다.
     *
     * <p><b>INV-C3 (Wait-free Read)</b>: 이 메서드는 락 없이 스냅샷을 통해
     * 안전하게 읽기를 수행합니다. 읽기 중 쓰기가 발생해도 일관된 뷰를 보장합니다.</p>
     *
     * @param key 반환할 값의 키
     * @return 지정된 키에 매핑된 값, 또는 매핑이 없으면 null
     * @throws NullPointerException 키가 null인 경우
     */
    @Override
    public V get(Object key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        try {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            byte[] keyBytes = encodeKey(k);

            // Wait-free read: 스냅샷의 rootPageId로 검색
            BTree btree = getBTree();
            long rootPageId = getCurrentRootPageId();
            Long valueRecordId = btree.findWithRoot(rootPageId, keyBytes);

            if (valueRecordId == null) {
                return null;
            }

            byte[] valueBytes = store.readValueRecord(valueRecordId);
            return decodeValue(valueBytes);

        } catch (ClassCastException e) {
            return null;
        }
    }
    
    /**
     * 지정된 키와 값을 이 맵에 연결합니다.
     *
     * <p><b>INV-C1 (Single Writer)</b>: 이 메서드는 Write Lock을 획득하여
     * 단일 Writer만 동시에 쓰기할 수 있도록 보장합니다.</p>
     *
     * <p><b>COW (Copy-on-Write)</b>: BTree 삽입은 stateless API를 통해
     * 새 루트 페이지를 생성하며, 이전 스냅샷에는 영향을 주지 않습니다.</p>
     *
     * <p><b>CONC-001 수정</b>: oldValue 조회를 락 내에서 수행하여 TOCTOU 버그 해결</p>
     *
     * @param key 값이 연결될 키
     * @param value 키에 연결될 값
     * @return 이전에 연결되어 있던 값, 또는 없으면 null
     * @throws NullPointerException 키 또는 값이 null인 경우
     */
    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("Key and value cannot be null");
        }

        byte[] keyBytes = encodeKey(key);
        byte[] valueBytes = encodeValue(value);

        // Write Lock 획득 (INV-C1) - 모든 연산을 락 내에서 수행
        long stamp = store.acquireWriteLock();
        try {
            // 현재 스냅샷에서 루트 페이지 ID 획득
            long currentRoot = getCurrentRootPageId();
            BTree btree = getBTree();

            // CONC-001 수정: 락 내에서 oldValue 조회
            V oldValue = null;
            boolean isNewKey = true;
            Long existingRecordId = btree.findWithRoot(currentRoot, keyBytes);
            if (existingRecordId != null) {
                byte[] existingValueBytes = store.readValueRecord(existingRecordId);
                oldValue = decodeValue(existingValueBytes);
                isNewKey = false;
            }

            // 값 레코드 작성 (allocator 사용)
            long valueRecordId = store.writeValueRecord(valueBytes);

            // BTree 삽입 (COW - stateless API)
            BTree.StatelessInsertResult result = btree.insertWithRoot(currentRoot, keyBytes, valueRecordId);

            // PERF-001: 새 키 삽입 시 count 증가
            if (isNewKey) {
                long currentCount = store.getCollectionCount(collectionId);
                store.updateCollectionRootCountAndPublish(collectionId, result.newRootPageId, currentCount + 1);
            } else {
                // 기존 키 업데이트 - count 변경 없음
                store.updateCollectionRootAndPublish(collectionId, result.newRootPageId);
            }

            // AUTO 모드면 즉시 커밋
            store.commitIfAuto();

            return oldValue;
        } finally {
            store.releaseWriteLock(stamp);
        }
    }
    
    /**
     * 지정된 키에 대한 매핑을 제거합니다.
     *
     * <p><b>INV-C1 (Single Writer)</b>: 이 메서드는 Write Lock을 획득하여
     * 단일 Writer만 동시에 쓰기할 수 있도록 보장합니다.</p>
     *
     * <p><b>COW (Copy-on-Write)</b>: BTree 삭제는 stateless API를 통해
     * 새 루트 페이지를 생성하며, 이전 스냅샷에는 영향을 주지 않습니다.</p>
     *
     * <p><b>CONC-001 수정</b>: oldValue 조회를 락 내에서 수행하여 TOCTOU 버그 해결</p>
     *
     * @param key 매핑이 제거될 키
     * @return 이전에 연결되어 있던 값, 또는 없으면 null
     * @throws NullPointerException 키가 null인 경우
     */
    @Override
    public V remove(Object key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        try {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            byte[] keyBytes = encodeKey(k);

            // Write Lock 획득 (INV-C1) - 모든 연산을 락 내에서 수행
            long stamp = store.acquireWriteLock();
            try {
                // 현재 스냅샷에서 루트 페이지 ID 획득
                long currentRoot = getCurrentRootPageId();
                BTree btree = getBTree();

                // CONC-001 수정: 락 내에서 oldValue 조회
                Long existingRecordId = btree.findWithRoot(currentRoot, keyBytes);
                if (existingRecordId == null) {
                    return null;  // 키가 없으면 즉시 반환
                }

                byte[] existingValueBytes = store.readValueRecord(existingRecordId);
                V oldValue = decodeValue(existingValueBytes);

                // BTree 삭제 (COW - stateless API)
                BTree.StatelessDeleteResult result = btree.deleteWithRoot(currentRoot, keyBytes);

                if (result.deleted) {
                    // PERF-001: 삭제 성공 시 count 감소
                    long currentCount = store.getCollectionCount(collectionId);
                    store.updateCollectionRootCountAndPublish(collectionId, result.newRootPageId, currentCount - 1);

                    // AUTO 모드면 즉시 커밋
                    store.commitIfAuto();
                }

                return oldValue;
            } finally {
                store.releaseWriteLock(stamp);
            }

        } catch (ClassCastException e) {
            return null;
        }
    }
    
    /**
     * 맵의 엔트리 수를 반환합니다.
     *
     * <p><b>PERF-001 수정</b>: O(n) 순회에서 O(1) 조회로 개선.
     * CollectionState에 저장된 count를 직접 반환합니다.</p>
     *
     * @return 맵에 포함된 엔트리 수
     */
    @Override
    public int size() {
        long count = store.getCollectionCount(collectionId);
        // Integer 오버플로우 방지
        if (count > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) count;
    }
    
    @Override
    public boolean isEmpty() {
        return getBTree().isEmpty();
    }
    
    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }
    
    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }
        
        BTreeCursor cursor = getBTree().cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
            V v = decodeValue(valueBytes);
            if (value.equals(v)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 이 맵의 모든 매핑을 제거합니다.
     *
     * <p><b>INV-C1 (Single Writer)</b>: 이 메서드는 Write Lock을 획득하여
     * 단일 Writer만 동시에 쓰기할 수 있도록 보장합니다.</p>
     *
     * <p><b>PERF-003: O(1) clear</b>: 이전 구현은 모든 엔트리를 순회하며
     * 개별 삭제(O(N*log N))했지만, 이제 root를 0으로 설정하여 O(1)입니다.
     * COW 특성상 기존 페이지는 진행 중인 읽기에 영향을 주지 않습니다.
     * 실제 페이지 회수는 GC/compaction에서 처리됩니다.</p>
     *
     * <p>시간 복잡도: O(1)
     * <p>공간 복잡도: O(1)
     */
    @Override
    public void clear() {
        if (isEmpty()) {
            return;
        }

        // Write Lock 획득 (INV-C1)
        long stamp = store.acquireWriteLock();
        try {
            // PERF-003: O(1) clear - root를 0 (빈 트리)으로, count를 0으로 설정
            store.updateCollectionRootCountAndPublish(collectionId, 0, 0);

            // AUTO 모드면 즉시 커밋
            store.commitIfAuto();

        } finally {
            store.releaseWriteLock(stamp);
        }
    }
    
    @Override
    public Set<K> keySet() {
        Set<K> keys = new LinkedHashSet<K>();
        BTreeCursor cursor = getBTree().cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            keys.add(decodeKey(entry.getKey()));
        }
        return keys;
    }
    
    @Override
    public Collection<V> values() {
        List<V> vals = new ArrayList<V>();
        BTreeCursor cursor = getBTree().cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
            vals.add(decodeValue(valueBytes));
        }
        return vals;
    }
    
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = new LinkedHashSet<Entry<K, V>>();
        BTreeCursor cursor = getBTree().cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            K k = decodeKey(entry.getKey());
            byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
            V v = decodeValue(valueBytes);
            entries.add(new AbstractMap.SimpleImmutableEntry<K, V>(k, v));
        }
        return entries;
    }
    
    @Override
    public Comparator<? super K> comparator() {
        return keyComparator;
    }
    
    @Override
    public K firstKey() {
        Entry<K, V> entry = firstEntry();
        if (entry == null) {
            throw new NoSuchElementException();
        }
        return entry.getKey();
    }
    
    @Override
    public K lastKey() {
        Entry<K, V> entry = lastEntry();
        if (entry == null) {
            throw new NoSuchElementException();
        }
        return entry.getKey();
    }
    
    /**
     * PERF-002: O(log n)으로 최적화된 첫 번째 엔트리 조회
     *
     * <p>BTree의 firstEntryWithRoot() Stateless API를 사용하여
     * 가장 왼쪽 리프까지 O(log n)으로 직접 순회합니다.</p>
     */
    @Override
    public Entry<K, V> firstEntry() {
        long currentRoot = getCurrentRootPageId();
        BTree btree = getBTree();
        BTree.Entry entry = btree.firstEntryWithRoot(currentRoot);
        if (entry == null) {
            return null;
        }
        K k = decodeKey(entry.getKey());
        byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
        V v = decodeValue(valueBytes);
        return new AbstractMap.SimpleImmutableEntry<K, V>(k, v);
    }

    /**
     * PERF-002: O(log n)으로 최적화된 마지막 엔트리 조회
     *
     * <p>이전 구현은 cursor를 사용해 전체 순회 O(n)이었습니다.
     * BTree의 lastEntryWithRoot() Stateless API를 사용하여
     * 가장 오른쪽 리프까지 O(log n)으로 직접 순회합니다.</p>
     */
    @Override
    public Entry<K, V> lastEntry() {
        long currentRoot = getCurrentRootPageId();
        BTree btree = getBTree();
        BTree.Entry entry = btree.lastEntryWithRoot(currentRoot);
        if (entry == null) {
            return null;
        }
        K k = decodeKey(entry.getKey());
        byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
        V v = decodeValue(valueBytes);
        return new AbstractMap.SimpleImmutableEntry<K, V>(k, v);
    }
    
    /**
     * 첫 번째 엔트리를 원자적으로 조회하고 삭제합니다.
     *
     * <p><b>CONC-002 수정</b>: 조회와 삭제를 하나의 락 내에서 수행하여
     * Race Condition 해결</p>
     *
     * @return 삭제된 첫 번째 엔트리, 맵이 비어있으면 null
     */
    @Override
    public Entry<K, V> pollFirstEntry() {
        // CONC-002 수정: 조회와 삭제를 하나의 락 내에서 수행
        long stamp = store.acquireWriteLock();
        try {
            long currentRoot = getCurrentRootPageId();
            if (currentRoot == 0) {
                return null;  // 빈 맵
            }

            BTree btree = getBTree();

            // 락 내에서 첫 번째 엔트리 조회
            BTreeCursor cursor = btree.cursorWithRoot(currentRoot);
            if (!cursor.hasNext()) {
                return null;
            }

            BTree.Entry btreeEntry = cursor.next();
            K key = decodeKey(btreeEntry.getKey());
            byte[] valueBytes = store.readValueRecord(btreeEntry.getValueRecordId());
            V value = decodeValue(valueBytes);

            // 락 내에서 삭제
            BTree.StatelessDeleteResult result = btree.deleteWithRoot(
                currentRoot, btreeEntry.getKey());

            if (result.deleted) {
                // PERF-001: 삭제 시 count 감소
                long currentCount = store.getCollectionCount(collectionId);
                store.updateCollectionRootCountAndPublish(collectionId, result.newRootPageId, currentCount - 1);
                store.commitIfAuto();
            }

            return new AbstractMap.SimpleImmutableEntry<>(key, value);
        } finally {
            store.releaseWriteLock(stamp);
        }
    }

    /**
     * 마지막 엔트리를 원자적으로 조회하고 삭제합니다.
     *
     * <p><b>CONC-002 수정</b>: 조회와 삭제를 하나의 락 내에서 수행하여
     * Race Condition 해결</p>
     *
     * @return 삭제된 마지막 엔트리, 맵이 비어있으면 null
     */
    @Override
    public Entry<K, V> pollLastEntry() {
        // CONC-002 수정: 조회와 삭제를 하나의 락 내에서 수행
        long stamp = store.acquireWriteLock();
        try {
            long currentRoot = getCurrentRootPageId();
            if (currentRoot == 0) {
                return null;  // 빈 맵
            }

            BTree btree = getBTree();

            // 락 내에서 마지막 엔트리 조회 (O(n) 순회 - PERF-002에서 개선 예정)
            BTreeCursor cursor = btree.cursorWithRoot(currentRoot);
            BTree.Entry lastBtreeEntry = null;
            while (cursor.hasNext()) {
                lastBtreeEntry = cursor.next();
            }

            if (lastBtreeEntry == null) {
                return null;
            }

            K key = decodeKey(lastBtreeEntry.getKey());
            byte[] valueBytes = store.readValueRecord(lastBtreeEntry.getValueRecordId());
            V value = decodeValue(valueBytes);

            // 락 내에서 삭제
            BTree.StatelessDeleteResult result = btree.deleteWithRoot(
                currentRoot, lastBtreeEntry.getKey());

            if (result.deleted) {
                // PERF-001: 삭제 시 count 감소
                long currentCount = store.getCollectionCount(collectionId);
                store.updateCollectionRootCountAndPublish(collectionId, result.newRootPageId, currentCount - 1);
                store.commitIfAuto();
            }

            return new AbstractMap.SimpleImmutableEntry<>(key, value);
        } finally {
            store.releaseWriteLock(stamp);
        }
    }

    // === CONC-003: View 클래스용 내부 헬퍼 메서드 ===

    /**
     * Write Lock 획득 (View 클래스용).
     *
     * <p>View 클래스에서 atomic poll 연산을 구현하기 위해 사용합니다.
     * 호출자는 반드시 finally 블록에서 {@link #releaseWriteLockInternal(long)}을 호출해야 합니다.</p>
     *
     * @return lock stamp
     */
    long acquireWriteLockInternal() {
        return store.acquireWriteLock();
    }

    /**
     * Write Lock 해제 (View 클래스용).
     *
     * @param stamp {@link #acquireWriteLockInternal()}에서 반환된 stamp
     */
    void releaseWriteLockInternal(long stamp) {
        store.releaseWriteLock(stamp);
    }

    /**
     * 키로 엔트리 삭제 (락 없이, 호출자가 이미 Write Lock을 보유해야 함).
     *
     * <p><b>주의</b>: 이 메서드는 락을 획득하지 않습니다.
     * 호출자가 반드시 {@link #acquireWriteLockInternal()}로 Write Lock을 보유한 상태에서 호출해야 합니다.</p>
     *
     * @param key 삭제할 키
     * @return 삭제 성공 여부
     */
    boolean removeByKeyUnlocked(K key) {
        byte[] keyBytes = encodeKey(key);
        long currentRoot = getCurrentRootPageId();
        BTree btree = getBTree();
        BTree.StatelessDeleteResult result = btree.deleteWithRoot(currentRoot, keyBytes);
        if (result.deleted) {
            // PERF-001: 삭제 시 count 감소
            long currentCount = store.getCollectionCount(collectionId);
            store.updateCollectionRootCountAndPublish(collectionId, result.newRootPageId, currentCount - 1);
            store.commitIfAuto();
            return true;
        }
        return false;
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        
        Entry<K, V> result = null;
        BTreeCursor cursor = getBTree().cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            K k = decodeKey(entry.getKey());
            
            if (keyComparator.compare(k, key) < 0) {
                byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
                V v = decodeValue(valueBytes);
                result = new AbstractMap.SimpleImmutableEntry<K, V>(k, v);
            } else {
                break;
            }
        }
        return result;
    }
    
    @Override
    public K lowerKey(K key) {
        Entry<K, V> entry = lowerEntry(key);
        return entry != null ? entry.getKey() : null;
    }
    
    @Override
    public Entry<K, V> floorEntry(K key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        
        Entry<K, V> result = null;
        BTreeCursor cursor = getBTree().cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            K k = decodeKey(entry.getKey());
            
            int cmp = keyComparator.compare(k, key);
            if (cmp <= 0) {
                byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
                V v = decodeValue(valueBytes);
                result = new AbstractMap.SimpleImmutableEntry<K, V>(k, v);
                if (cmp == 0) {
                    break;
                }
            } else {
                break;
            }
        }
        return result;
    }
    
    @Override
    public K floorKey(K key) {
        Entry<K, V> entry = floorEntry(key);
        return entry != null ? entry.getKey() : null;
    }
    
    @Override
    public Entry<K, V> ceilingEntry(K key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        
        BTreeCursor cursor = getBTree().cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            K k = decodeKey(entry.getKey());
            
            if (keyComparator.compare(k, key) >= 0) {
                byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
                V v = decodeValue(valueBytes);
                return new AbstractMap.SimpleImmutableEntry<K, V>(k, v);
            }
        }
        return null;
    }
    
    @Override
    public K ceilingKey(K key) {
        Entry<K, V> entry = ceilingEntry(key);
        return entry != null ? entry.getKey() : null;
    }
    
    @Override
    public Entry<K, V> higherEntry(K key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        
        BTreeCursor cursor = getBTree().cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            K k = decodeKey(entry.getKey());
            
            if (keyComparator.compare(k, key) > 0) {
                byte[] valueBytes = store.readValueRecord(entry.getValueRecordId());
                V v = decodeValue(valueBytes);
                return new AbstractMap.SimpleImmutableEntry<K, V>(k, v);
            }
        }
        return null;
    }
    
    @Override
    public K higherKey(K key) {
        Entry<K, V> entry = higherEntry(key);
        return entry != null ? entry.getKey() : null;
    }
    
    /**
     * 이 맵의 역순 뷰를 반환합니다.
     *
     * <p>시간 복잡도: O(1) - 뷰 생성
     * <p>공간 복잡도: O(1) - 래퍼 객체만 생성
     *
     * <p>반환된 맵은 읽기 전용이며, 수정 연산은
     * {@link UnsupportedOperationException}을 던집니다.
     *
     * @return 내림차순으로 정렬된 뷰
     */
    @Override
    public NavigableMap<K, V> descendingMap() {
        return new DescendingMapView<>(this);
    }
    
    /**
     * 이 맵에 포함된 키의 NavigableSet 뷰를 반환합니다.
     *
     * <p>시간 복잡도: O(1) - 뷰 생성
     * <p>공간 복잡도: O(1) - 래퍼 객체만 생성
     *
     * <p>반환된 Set은 읽기 전용이며, 수정 연산은
     * {@link UnsupportedOperationException}을 던집니다.
     *
     * @return 오름차순으로 정렬된 키의 NavigableSet 뷰
     */
    @Override
    public NavigableSet<K> navigableKeySet() {
        return new KeySetView<>(this, false);
    }
    
    /**
     * 이 맵에 포함된 키의 역순 NavigableSet 뷰를 반환합니다.
     *
     * <p>시간 복잡도: O(1) - 뷰 생성
     * <p>공간 복잡도: O(1) - 래퍼 객체만 생성
     *
     * <p>반환된 Set은 읽기 전용이며, 수정 연산은
     * {@link UnsupportedOperationException}을 던집니다.
     *
     * @return 내림차순으로 정렬된 키의 NavigableSet 뷰
     */
    @Override
    public NavigableSet<K> descendingKeySet() {
        return new KeySetView<>(this, true);
    }
    
    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }
    
    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }
    
    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }
    
    /**
     * 이 맵에서 fromKey부터 toKey 범위의 부분 뷰를 반환합니다.
     *
     * <p>반환된 맵은 읽기 전용입니다. put(), remove() 등 수정 연산은
     * {@link UnsupportedOperationException}을 던집니다.
     *
     * <p>시간 복잡도: O(1) - 뷰 생성
     * <p>공간 복잡도: O(1) - 래퍼 객체만 생성
     *
     * @param fromKey 범위 시작 키 (null 불가)
     * @param fromInclusive fromKey 포함 여부
     * @param toKey 범위 종료 키 (null 불가)
     * @param toInclusive toKey 포함 여부
     * @return 범위 내 요소만 포함하는 읽기 전용 뷰
     * @throws NullPointerException fromKey 또는 toKey가 null인 경우
     * @throws IllegalArgumentException fromKey > toKey인 경우
     */
    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        Objects.requireNonNull(fromKey, "fromKey cannot be null");
        Objects.requireNonNull(toKey, "toKey cannot be null");

        if (keyComparator.compare(fromKey, toKey) > 0) {
            throw new IllegalArgumentException(
                "fromKey > toKey: " + fromKey + " > " + toKey);
        }

        return new SubMapView<>(this, fromKey, fromInclusive, toKey, toInclusive);
    }

    /**
     * 이 맵에서 toKey보다 작은 키들의 부분 뷰를 반환합니다.
     *
     * <p>반환된 맵은 읽기 전용입니다.
     *
     * <p>시간 복잡도: O(1) - 뷰 생성
     * <p>공간 복잡도: O(1) - 래퍼 객체만 생성
     *
     * @param toKey 범위 종료 키 (null 불가)
     * @param inclusive toKey 포함 여부
     * @return 범위 내 요소만 포함하는 읽기 전용 뷰
     * @throws NullPointerException toKey가 null인 경우
     */
    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        Objects.requireNonNull(toKey, "toKey cannot be null");
        return new HeadMapView<>(this, toKey, inclusive);
    }

    /**
     * 이 맵에서 fromKey보다 크거나 같은 키들의 부분 뷰를 반환합니다.
     *
     * <p>반환된 맵은 읽기 전용입니다.
     *
     * <p>시간 복잡도: O(1) - 뷰 생성
     * <p>공간 복잡도: O(1) - 래퍼 객체만 생성
     *
     * @param fromKey 범위 시작 키 (null 불가)
     * @param inclusive fromKey 포함 여부
     * @return 범위 내 요소만 포함하는 읽기 전용 뷰
     * @throws NullPointerException fromKey가 null인 경우
     */
    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        Objects.requireNonNull(fromKey, "fromKey cannot be null");
        return new TailMapView<>(this, fromKey, inclusive);
    }

    // =====================================================================
    // 내부 뷰 클래스들
    // =====================================================================

    /**
     * NavigableSet 기반 키 뷰
     *
     * <p>UOE 개선: NavigableMap 인터페이스를 받도록 일반화하여
     * SubMapView, HeadMapView, TailMapView에서도 사용 가능
     */
    private static class KeySetView<K, V> extends AbstractSet<K> implements NavigableSet<K> {
        private final NavigableMap<K, V> map;
        private final boolean descending;

        KeySetView(NavigableMap<K, V> map, boolean descending) {
            this.map = map;
            this.descending = descending;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        /**
         * UOE 개선: KeySetView에서 remove 지원
         * 맵에서 해당 키를 제거
         */
        @Override
        public boolean remove(Object o) {
            if (map.containsKey(o)) {
                map.remove(o);
                return true;
            }
            return false;
        }

        @Override
        public Iterator<K> iterator() {
            // entrySet()을 사용하여 무한 재귀 방지 (keySet() -> navigableKeySet() -> KeySetView -> iterator() -> keySet() 사이클)
            List<K> keys = new ArrayList<>();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                keys.add(entry.getKey());
            }
            if (descending) {
                Collections.reverse(keys);
            }
            return Collections.unmodifiableList(keys).iterator();
        }

        @Override
        public Iterator<K> descendingIterator() {
            // entrySet()을 사용하여 무한 재귀 방지
            List<K> keys = new ArrayList<>();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                keys.add(entry.getKey());
            }
            if (!descending) {
                Collections.reverse(keys);
            }
            return Collections.unmodifiableList(keys).iterator();
        }

        @Override
        public Comparator<? super K> comparator() {
            Comparator<? super K> cmp = map.comparator();
            return descending ? Collections.reverseOrder(cmp) : cmp;
        }

        @Override
        public K first() {
            return descending ? map.lastKey() : map.firstKey();
        }

        @Override
        public K last() {
            return descending ? map.firstKey() : map.lastKey();
        }

        @Override
        public K lower(K e) {
            return descending ? map.higherKey(e) : map.lowerKey(e);
        }

        @Override
        public K floor(K e) {
            return descending ? map.ceilingKey(e) : map.floorKey(e);
        }

        @Override
        public K ceiling(K e) {
            return descending ? map.floorKey(e) : map.ceilingKey(e);
        }

        @Override
        public K higher(K e) {
            return descending ? map.lowerKey(e) : map.higherKey(e);
        }

        @Override
        public K pollFirst() {
            Map.Entry<K, V> entry = descending ? map.pollLastEntry() : map.pollFirstEntry();
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public K pollLast() {
            Map.Entry<K, V> entry = descending ? map.pollFirstEntry() : map.pollLastEntry();
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public NavigableSet<K> descendingSet() {
            return new KeySetView<>(map, !descending);
        }

        /**
         * UOE 개선: KeySetView에서 subSet 지원
         * descending 모드일 때는 from/to를 뒤집어서 처리
         */
        @Override
        public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
            if (descending) {
                return map.subMap(toElement, toInclusive, fromElement, fromInclusive).navigableKeySet();
            }
            return map.subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
        }

        /**
         * UOE 개선: KeySetView에서 headSet 지원
         * descending 모드일 때는 tailMap으로 변환
         */
        @Override
        public NavigableSet<K> headSet(K toElement, boolean inclusive) {
            if (descending) {
                return map.tailMap(toElement, inclusive).navigableKeySet();
            }
            return map.headMap(toElement, inclusive).navigableKeySet();
        }

        /**
         * UOE 개선: KeySetView에서 tailSet 지원
         * descending 모드일 때는 headMap으로 변환
         */
        @Override
        public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
            if (descending) {
                return map.headMap(fromElement, inclusive).navigableKeySet();
            }
            return map.tailMap(fromElement, inclusive).navigableKeySet();
        }

        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public SortedSet<K> headSet(K toElement) {
            return headSet(toElement, false);
        }

        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return tailSet(fromElement, true);
        }
    }

    /**
     * 역순 맵 뷰
     *
     * <p>UOE 개선: NavigableMap 인터페이스를 받도록 일반화하고
     * 수정 연산(put, remove, clear)과 뷰 생성 연산(subMap, headMap, tailMap) 지원
     */
    private static class DescendingMapView<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
        private final NavigableMap<K, V> parent;

        DescendingMapView(NavigableMap<K, V> parent) {
            this.parent = parent;
        }

        @Override
        public V get(Object key) {
            return parent.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return parent.containsKey(key);
        }

        @Override
        public int size() {
            return parent.size();
        }

        @Override
        public boolean isEmpty() {
            return parent.isEmpty();
        }

        /**
         * UOE 개선: parent에 직접 위임
         * TreeMap.descendingMap().put()과 동일 동작
         */
        @Override
        public V put(K key, V value) {
            return parent.put(key, value);
        }

        /**
         * UOE 개선: parent에 직접 위임
         */
        @Override
        public V remove(Object key) {
            return parent.remove(key);
        }

        /**
         * UOE 개선: parent에 직접 위임
         */
        @Override
        public void clear() {
            parent.clear();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            List<Entry<K, V>> entries = new ArrayList<>(parent.entrySet());
            Collections.reverse(entries);
            return new LinkedHashSet<>(entries);
        }

        @Override
        public Comparator<? super K> comparator() {
            return Collections.reverseOrder(parent.comparator());
        }

        @Override
        public K firstKey() {
            return parent.lastKey();
        }

        @Override
        public K lastKey() {
            return parent.firstKey();
        }

        @Override
        public Entry<K, V> firstEntry() {
            return parent.lastEntry();
        }

        @Override
        public Entry<K, V> lastEntry() {
            return parent.firstEntry();
        }

        /**
         * UOE 개선: descending에서 pollFirst = parent.pollLast
         */
        @Override
        public Entry<K, V> pollFirstEntry() {
            return parent.pollLastEntry();
        }

        /**
         * UOE 개선: descending에서 pollLast = parent.pollFirst
         */
        @Override
        public Entry<K, V> pollLastEntry() {
            return parent.pollFirstEntry();
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            return parent.higherEntry(key);
        }

        @Override
        public K lowerKey(K key) {
            return parent.higherKey(key);
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            return parent.ceilingEntry(key);
        }

        @Override
        public K floorKey(K key) {
            return parent.ceilingKey(key);
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            return parent.floorEntry(key);
        }

        @Override
        public K ceilingKey(K key) {
            return parent.floorKey(key);
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            return parent.lowerEntry(key);
        }

        @Override
        public K higherKey(K key) {
            return parent.lowerKey(key);
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return parent;
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return new KeySetView<>(this, false);
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return new KeySetView<>(this, true);
        }

        /**
         * UOE 개선: descending에서 subMap(from, to) = parent.subMap(to, from).descendingMap()
         * descending 뷰에서 from > to (역순)이므로 parent에서는 to < from으로 변환
         */
        @Override
        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return parent.subMap(toKey, toInclusive, fromKey, fromInclusive).descendingMap();
        }

        /**
         * UOE 개선: descending headMap(to) = parent.tailMap(to).descendingMap()
         */
        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return parent.tailMap(toKey, inclusive).descendingMap();
        }

        /**
         * UOE 개선: descending tailMap(from) = parent.headMap(from).descendingMap()
         */
        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return parent.headMap(fromKey, inclusive).descendingMap();
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }
    }

    /**
     * SubMap 뷰
     *
     * <p>UOE 개선: 수정 연산(put, remove), poll 연산, 중첩 뷰 생성, 키셋 뷰 지원
     */
    private static class SubMapView<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
        private final FxNavigableMapImpl<K, V> parent;
        private final K fromKey, toKey;
        private final boolean fromInclusive, toInclusive;

        SubMapView(FxNavigableMapImpl<K, V> parent, K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            this.parent = parent;
            this.fromKey = fromKey;
            this.fromInclusive = fromInclusive;
            this.toKey = toKey;
            this.toInclusive = toInclusive;
        }

        // === 범위 검증 유틸리티 (UOE 개선) ===

        @SuppressWarnings("unchecked")
        private int compare(K k1, K k2) {
            Comparator<? super K> cmp = parent.comparator();
            if (cmp != null) {
                return cmp.compare(k1, k2);
            }
            return ((Comparable<? super K>) k1).compareTo(k2);
        }

        private boolean tooLow(K key) {
            int c = compare(key, fromKey);
            return c < 0 || (c == 0 && !fromInclusive);
        }

        private boolean tooHigh(K key) {
            int c = compare(key, toKey);
            return c > 0 || (c == 0 && !toInclusive);
        }

        private boolean inRange(K key) {
            if (key == null) return false;
            return !tooLow(key) && !tooHigh(key);
        }

        private void checkInRange(K key) {
            if (!inRange(key)) {
                throw new IllegalArgumentException("key out of range: " + key);
            }
        }

        @Override
        public V get(Object key) {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            if (!inRange(k)) return null;
            return parent.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            return inRange(k) && parent.containsKey(key);
        }

        /**
         * UOE 개선: 범위 내 키만 허용, 범위 밖이면 IllegalArgumentException
         */
        @Override
        public V put(K key, V value) {
            checkInRange(key);
            return parent.put(key, value);
        }

        /**
         * UOE 개선: 범위 밖 키는 null 반환 (TreeMap 동작과 동일)
         */
        @Override
        public V remove(Object key) {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            if (!inRange(k)) {
                return null;
            }
            return parent.remove(key);
        }

        @Override
        public int size() {
            int count = 0;
            for (K key : parent.keySet()) {
                if (inRange(key)) count++;
            }
            return count;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> result = new LinkedHashSet<>();
            for (Entry<K, V> entry : parent.entrySet()) {
                if (inRange(entry.getKey())) {
                    result.add(entry);
                }
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public Comparator<? super K> comparator() {
            return parent.comparator();
        }

        @Override
        public K firstKey() {
            for (K key : parent.keySet()) {
                if (inRange(key)) return key;
            }
            throw new NoSuchElementException();
        }

        @Override
        public K lastKey() {
            K last = null;
            for (K key : parent.keySet()) {
                if (inRange(key)) last = key;
            }
            if (last == null) throw new NoSuchElementException();
            return last;
        }

        @Override
        public Entry<K, V> firstEntry() {
            for (Entry<K, V> entry : parent.entrySet()) {
                if (inRange(entry.getKey())) return entry;
            }
            return null;
        }

        @Override
        public Entry<K, V> lastEntry() {
            Entry<K, V> last = null;
            for (Entry<K, V> entry : parent.entrySet()) {
                if (inRange(entry.getKey())) last = entry;
            }
            return last;
        }

        /**
         * CONC-003 수정: Atomic poll (조회 + 삭제를 단일 락 내에서 수행)
         *
         * <p>이전 구현은 firstEntry() 호출 후 별도로 remove()를 호출하여
         * Race Condition이 발생할 수 있었습니다. 이제 단일 Write Lock 내에서
         * 조회와 삭제를 원자적으로 수행합니다.</p>
         */
        @Override
        public Entry<K, V> pollFirstEntry() {
            long stamp = parent.acquireWriteLockInternal();
            try {
                // 범위 내 첫 번째 엔트리 조회 (락 내에서 안전)
                Entry<K, V> first = null;
                for (Entry<K, V> entry : parent.entrySet()) {
                    if (inRange(entry.getKey())) {
                        first = entry;
                        break;
                    }
                }
                if (first != null) {
                    parent.removeByKeyUnlocked(first.getKey());
                }
                return first;
            } finally {
                parent.releaseWriteLockInternal(stamp);
            }
        }

        /**
         * CONC-003 수정: Atomic poll (조회 + 삭제를 단일 락 내에서 수행)
         */
        @Override
        public Entry<K, V> pollLastEntry() {
            long stamp = parent.acquireWriteLockInternal();
            try {
                // 범위 내 마지막 엔트리 조회 (락 내에서 안전)
                Entry<K, V> last = null;
                for (Entry<K, V> entry : parent.entrySet()) {
                    if (inRange(entry.getKey())) {
                        last = entry;
                    }
                }
                if (last != null) {
                    parent.removeByKeyUnlocked(last.getKey());
                }
                return last;
            } finally {
                parent.releaseWriteLockInternal(stamp);
            }
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            Entry<K, V> result = null;
            for (Entry<K, V> entry : parent.entrySet()) {
                K k = entry.getKey();
                if (inRange(k) && compare(k, key) < 0) {
                    result = entry;
                }
            }
            return result;
        }

        @Override
        public K lowerKey(K key) {
            Entry<K, V> entry = lowerEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            Entry<K, V> result = null;
            for (Entry<K, V> entry : parent.entrySet()) {
                K k = entry.getKey();
                if (inRange(k) && compare(k, key) <= 0) {
                    result = entry;
                }
            }
            return result;
        }

        @Override
        public K floorKey(K key) {
            Entry<K, V> entry = floorEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            for (Entry<K, V> entry : parent.entrySet()) {
                K k = entry.getKey();
                if (inRange(k) && compare(k, key) >= 0) {
                    return entry;
                }
            }
            return null;
        }

        @Override
        public K ceilingKey(K key) {
            Entry<K, V> entry = ceilingEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            for (Entry<K, V> entry : parent.entrySet()) {
                K k = entry.getKey();
                if (inRange(k) && compare(k, key) > 0) {
                    return entry;
                }
            }
            return null;
        }

        @Override
        public K higherKey(K key) {
            Entry<K, V> entry = higherEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        /**
         * UOE 개선: SubMapView의 descendingMap
         */
        @Override
        public NavigableMap<K, V> descendingMap() {
            return new DescendingMapView<>(this);
        }

        /**
         * UOE 개선: SubMapView의 navigableKeySet
         */
        @Override
        public NavigableSet<K> navigableKeySet() {
            return new KeySetView<>(this, false);
        }

        /**
         * UOE 개선: SubMapView의 descendingKeySet
         */
        @Override
        public NavigableSet<K> descendingKeySet() {
            return new KeySetView<>(this, true);
        }

        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        /**
         * UOE 개선: 중첩 subMap (범위 교집합 계산)
         */
        @Override
        public NavigableMap<K, V> subMap(K from, boolean fi, K to, boolean ti) {
            if (tooLow(from) || tooHigh(to)) {
                throw new IllegalArgumentException("subMap range out of bounds");
            }
            return parent.subMap(from, fi, to, ti);
        }

        /**
         * UOE 개선: 중첩 headMap
         */
        @Override
        public NavigableMap<K, V> headMap(K to, boolean inclusive) {
            if (tooHigh(to)) {
                throw new IllegalArgumentException("toKey out of range: " + to);
            }
            K effectiveTo = to;
            boolean effectiveInclusive = inclusive;
            // 상한이 현재 SubMap 범위를 초과하면 현재 상한 사용
            if (compare(to, toKey) > 0 || (compare(to, toKey) == 0 && inclusive && !toInclusive)) {
                effectiveTo = toKey;
                effectiveInclusive = toInclusive;
            }
            return parent.subMap(fromKey, fromInclusive, effectiveTo, effectiveInclusive);
        }

        /**
         * UOE 개선: 중첩 tailMap
         */
        @Override
        public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
            if (tooLow(from)) {
                throw new IllegalArgumentException("fromKey out of range: " + from);
            }
            K effectiveFrom = from;
            boolean effectiveInclusive = inclusive;
            // 하한이 현재 SubMap 범위 밖이면 현재 하한 사용
            if (compare(from, fromKey) < 0 || (compare(from, fromKey) == 0 && !inclusive && fromInclusive)) {
                effectiveFrom = fromKey;
                effectiveInclusive = fromInclusive;
            }
            return parent.subMap(effectiveFrom, effectiveInclusive, toKey, toInclusive);
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }
    }

    /**
     * HeadMap 뷰
     *
     * <p>UOE 개선: 수정 연산, poll 연산, 중첩 뷰 생성, 키셋 뷰 지원
     */
    private static class HeadMapView<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
        private final FxNavigableMapImpl<K, V> parent;
        private final K toKey;
        private final boolean inclusive;

        HeadMapView(FxNavigableMapImpl<K, V> parent, K toKey, boolean inclusive) {
            this.parent = parent;
            this.toKey = toKey;
            this.inclusive = inclusive;
        }

        // === 범위 검증 유틸리티 (UOE 개선) ===

        @SuppressWarnings("unchecked")
        private int compare(K k1, K k2) {
            Comparator<? super K> cmp = parent.comparator();
            if (cmp != null) {
                return cmp.compare(k1, k2);
            }
            return ((Comparable<? super K>) k1).compareTo(k2);
        }

        private boolean inRange(K key) {
            if (key == null) return false;
            int cmp = compare(key, toKey);
            return inclusive ? cmp <= 0 : cmp < 0;
        }

        private void checkInRange(K key) {
            if (!inRange(key)) {
                throw new IllegalArgumentException("key out of range: " + key);
            }
        }

        @Override
        public V get(Object key) {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            if (!inRange(k)) return null;
            return parent.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            return inRange(k) && parent.containsKey(key);
        }

        /**
         * UOE 개선: 범위 내 키만 허용
         */
        @Override
        public V put(K key, V value) {
            checkInRange(key);
            return parent.put(key, value);
        }

        /**
         * UOE 개선: 범위 밖 키는 null 반환
         */
        @Override
        public V remove(Object key) {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            if (!inRange(k)) {
                return null;
            }
            return parent.remove(key);
        }

        @Override
        public int size() {
            int count = 0;
            for (K key : parent.keySet()) {
                if (inRange(key)) count++;
            }
            return count;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> result = new LinkedHashSet<>();
            for (Entry<K, V> entry : parent.entrySet()) {
                if (inRange(entry.getKey())) {
                    result.add(entry);
                }
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public Comparator<? super K> comparator() {
            return parent.comparator();
        }

        @Override
        public K firstKey() {
            Entry<K, V> first = firstEntry();
            if (first == null) throw new NoSuchElementException();
            return first.getKey();
        }

        @Override
        public K lastKey() {
            K last = null;
            for (K key : parent.keySet()) {
                if (inRange(key)) last = key;
            }
            if (last == null) throw new NoSuchElementException();
            return last;
        }

        @Override
        public Entry<K, V> firstEntry() {
            Entry<K, V> first = parent.firstEntry();
            return (first != null && inRange(first.getKey())) ? first : null;
        }

        @Override
        public Entry<K, V> lastEntry() {
            Entry<K, V> last = null;
            for (Entry<K, V> entry : parent.entrySet()) {
                if (inRange(entry.getKey())) last = entry;
            }
            return last;
        }

        /**
         * CONC-003 수정: Atomic poll (조회 + 삭제를 단일 락 내에서 수행)
         *
         * <p>HeadMapView의 firstEntry는 parent.firstEntry()를 사용하므로
         * 범위 검증만 추가로 수행합니다.</p>
         */
        @Override
        public Entry<K, V> pollFirstEntry() {
            long stamp = parent.acquireWriteLockInternal();
            try {
                // HeadMap의 firstEntry = parent의 firstEntry (범위 내인 경우)
                Entry<K, V> first = parent.firstEntry();
                if (first != null && inRange(first.getKey())) {
                    parent.removeByKeyUnlocked(first.getKey());
                    return first;
                }
                return null;
            } finally {
                parent.releaseWriteLockInternal(stamp);
            }
        }

        /**
         * CONC-003 수정: Atomic poll (조회 + 삭제를 단일 락 내에서 수행)
         */
        @Override
        public Entry<K, V> pollLastEntry() {
            long stamp = parent.acquireWriteLockInternal();
            try {
                // 범위 내 마지막 엔트리 조회 (락 내에서 안전)
                Entry<K, V> last = null;
                for (Entry<K, V> entry : parent.entrySet()) {
                    if (inRange(entry.getKey())) {
                        last = entry;
                    }
                }
                if (last != null) {
                    parent.removeByKeyUnlocked(last.getKey());
                }
                return last;
            } finally {
                parent.releaseWriteLockInternal(stamp);
            }
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            Entry<K, V> entry = parent.lowerEntry(key);
            return (entry != null && inRange(entry.getKey())) ? entry : null;
        }

        @Override
        public K lowerKey(K key) {
            Entry<K, V> entry = lowerEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            // key가 범위 외이면 범위 내 가장 큰 키 반환
            if (!inRange(key)) {
                int cmp = compare(key, toKey);
                if (cmp >= 0) {
                    return lastEntry();
                }
                return null;
            }
            Entry<K, V> entry = parent.floorEntry(key);
            return (entry != null && inRange(entry.getKey())) ? entry : null;
        }

        @Override
        public K floorKey(K key) {
            Entry<K, V> entry = floorEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            Entry<K, V> entry = parent.ceilingEntry(key);
            return (entry != null && inRange(entry.getKey())) ? entry : null;
        }

        @Override
        public K ceilingKey(K key) {
            Entry<K, V> entry = ceilingEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            Entry<K, V> entry = parent.higherEntry(key);
            return (entry != null && inRange(entry.getKey())) ? entry : null;
        }

        @Override
        public K higherKey(K key) {
            Entry<K, V> entry = higherEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        /** UOE 개선: descendingMap */
        @Override
        public NavigableMap<K, V> descendingMap() {
            return new DescendingMapView<>(this);
        }

        /** UOE 개선: navigableKeySet */
        @Override
        public NavigableSet<K> navigableKeySet() {
            return new KeySetView<>(this, false);
        }

        /** UOE 개선: descendingKeySet */
        @Override
        public NavigableSet<K> descendingKeySet() {
            return new KeySetView<>(this, true);
        }

        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        /** UOE 개선: 중첩 subMap */
        @Override
        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K to, boolean toInclusive) {
            if (!inRange(to) && compare(to, toKey) > 0) {
                throw new IllegalArgumentException("toKey out of range: " + to);
            }
            return parent.subMap(fromKey, fromInclusive, to, toInclusive);
        }

        /** UOE 개선: 중첩 headMap */
        @Override
        public NavigableMap<K, V> headMap(K to, boolean toInclusive) {
            // 새 상한이 현재 상한보다 크면 예외
            if (compare(to, toKey) > 0 || (compare(to, toKey) == 0 && toInclusive && !inclusive)) {
                throw new IllegalArgumentException("toKey out of range: " + to);
            }
            return parent.headMap(to, toInclusive);
        }

        /** UOE 개선: 중첩 tailMap */
        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean fromInclusive) {
            // tailMap은 하한만 지정, 상한은 현재 HeadMap의 toKey 유지
            if (compare(fromKey, toKey) > 0 || (compare(fromKey, toKey) == 0 && !inclusive)) {
                throw new IllegalArgumentException("fromKey out of range: " + fromKey);
            }
            return parent.subMap(fromKey, fromInclusive, toKey, inclusive);
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }
    }

    /**
     * TailMap 뷰
     *
     * <p>UOE 개선: 수정 연산, poll 연산, 중첩 뷰 생성, 키셋 뷰 지원
     */
    private static class TailMapView<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V> {
        private final FxNavigableMapImpl<K, V> parent;
        private final K fromKey;
        private final boolean inclusive;

        TailMapView(FxNavigableMapImpl<K, V> parent, K fromKey, boolean inclusive) {
            this.parent = parent;
            this.fromKey = fromKey;
            this.inclusive = inclusive;
        }

        // === 범위 검증 유틸리티 (UOE 개선) ===

        @SuppressWarnings("unchecked")
        private int compare(K k1, K k2) {
            Comparator<? super K> cmp = parent.comparator();
            if (cmp != null) {
                return cmp.compare(k1, k2);
            }
            return ((Comparable<? super K>) k1).compareTo(k2);
        }

        private boolean inRange(K key) {
            if (key == null) return false;
            int cmp = compare(key, fromKey);
            return inclusive ? cmp >= 0 : cmp > 0;
        }

        private void checkInRange(K key) {
            if (!inRange(key)) {
                throw new IllegalArgumentException("key out of range: " + key);
            }
        }

        @Override
        public V get(Object key) {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            if (!inRange(k)) return null;
            return parent.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            return inRange(k) && parent.containsKey(key);
        }

        /**
         * UOE 개선: 범위 내 키만 허용
         */
        @Override
        public V put(K key, V value) {
            checkInRange(key);
            return parent.put(key, value);
        }

        /**
         * UOE 개선: 범위 밖 키는 null 반환
         */
        @Override
        public V remove(Object key) {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            if (!inRange(k)) {
                return null;
            }
            return parent.remove(key);
        }

        @Override
        public int size() {
            int count = 0;
            for (K key : parent.keySet()) {
                if (inRange(key)) count++;
            }
            return count;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> result = new LinkedHashSet<>();
            for (Entry<K, V> entry : parent.entrySet()) {
                if (inRange(entry.getKey())) {
                    result.add(entry);
                }
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public Comparator<? super K> comparator() {
            return parent.comparator();
        }

        @Override
        public K firstKey() {
            for (K key : parent.keySet()) {
                if (inRange(key)) return key;
            }
            throw new NoSuchElementException();
        }

        @Override
        public K lastKey() {
            Entry<K, V> last = lastEntry();
            if (last == null) throw new NoSuchElementException();
            return last.getKey();
        }

        @Override
        public Entry<K, V> firstEntry() {
            for (Entry<K, V> entry : parent.entrySet()) {
                if (inRange(entry.getKey())) return entry;
            }
            return null;
        }

        @Override
        public Entry<K, V> lastEntry() {
            Entry<K, V> last = parent.lastEntry();
            return (last != null && inRange(last.getKey())) ? last : null;
        }

        /**
         * CONC-003 수정: Atomic poll (조회 + 삭제를 단일 락 내에서 수행)
         *
         * <p>TailMapView의 firstEntry는 순회가 필요하므로
         * 락 내에서 직접 순회하여 첫 번째 엔트리를 찾습니다.</p>
         */
        @Override
        public Entry<K, V> pollFirstEntry() {
            long stamp = parent.acquireWriteLockInternal();
            try {
                // 범위 내 첫 번째 엔트리 조회 (락 내에서 안전)
                Entry<K, V> first = null;
                for (Entry<K, V> entry : parent.entrySet()) {
                    if (inRange(entry.getKey())) {
                        first = entry;
                        break;
                    }
                }
                if (first != null) {
                    parent.removeByKeyUnlocked(first.getKey());
                }
                return first;
            } finally {
                parent.releaseWriteLockInternal(stamp);
            }
        }

        /**
         * CONC-003 수정: Atomic poll (조회 + 삭제를 단일 락 내에서 수행)
         *
         * <p>TailMapView의 lastEntry는 parent.lastEntry()를 사용하므로
         * 범위 검증만 추가로 수행합니다.</p>
         */
        @Override
        public Entry<K, V> pollLastEntry() {
            long stamp = parent.acquireWriteLockInternal();
            try {
                // TailMap의 lastEntry = parent의 lastEntry (범위 내인 경우)
                Entry<K, V> last = parent.lastEntry();
                if (last != null && inRange(last.getKey())) {
                    parent.removeByKeyUnlocked(last.getKey());
                    return last;
                }
                return null;
            } finally {
                parent.releaseWriteLockInternal(stamp);
            }
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            Entry<K, V> entry = parent.lowerEntry(key);
            return (entry != null && inRange(entry.getKey())) ? entry : null;
        }

        @Override
        public K lowerKey(K key) {
            Entry<K, V> entry = lowerEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            Entry<K, V> entry = parent.floorEntry(key);
            return (entry != null && inRange(entry.getKey())) ? entry : null;
        }

        @Override
        public K floorKey(K key) {
            Entry<K, V> entry = floorEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            // key가 범위 외(앞쪽)이면 범위 내 첫 번째 키 반환
            if (!inRange(key)) {
                int cmp = compare(key, fromKey);
                if (cmp < 0) {
                    return firstEntry();
                }
                return null;
            }
            Entry<K, V> entry = parent.ceilingEntry(key);
            return (entry != null && inRange(entry.getKey())) ? entry : null;
        }

        @Override
        public K ceilingKey(K key) {
            Entry<K, V> entry = ceilingEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            Entry<K, V> entry = parent.higherEntry(key);
            return (entry != null && inRange(entry.getKey())) ? entry : null;
        }

        @Override
        public K higherKey(K key) {
            Entry<K, V> entry = higherEntry(key);
            return entry != null ? entry.getKey() : null;
        }

        /** UOE 개선: descendingMap */
        @Override
        public NavigableMap<K, V> descendingMap() {
            return new DescendingMapView<>(this);
        }

        /** UOE 개선: navigableKeySet */
        @Override
        public NavigableSet<K> navigableKeySet() {
            return new KeySetView<>(this, false);
        }

        /** UOE 개선: descendingKeySet */
        @Override
        public NavigableSet<K> descendingKeySet() {
            return new KeySetView<>(this, true);
        }

        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        /** UOE 개선: 중첩 subMap */
        @Override
        public NavigableMap<K, V> subMap(K from, boolean fromInclusive, K toKey, boolean toInclusive) {
            if (!inRange(from) && compare(from, fromKey) < 0) {
                throw new IllegalArgumentException("fromKey out of range: " + from);
            }
            return parent.subMap(from, fromInclusive, toKey, toInclusive);
        }

        /** UOE 개선: 중첩 headMap */
        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean toInclusive) {
            // headMap은 상한만 지정, 하한은 현재 TailMap의 fromKey 유지
            return parent.subMap(fromKey, inclusive, toKey, toInclusive);
        }

        /** UOE 개선: 중첩 tailMap */
        @Override
        public NavigableMap<K, V> tailMap(K from, boolean fromInclusive) {
            // 새 하한이 현재 하한보다 작으면 예외
            if (compare(from, fromKey) < 0 || (compare(from, fromKey) == 0 && !fromInclusive && inclusive)) {
                throw new IllegalArgumentException("fromKey out of range: " + from);
            }
            return parent.tailMap(from, fromInclusive);
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }
    }
}
