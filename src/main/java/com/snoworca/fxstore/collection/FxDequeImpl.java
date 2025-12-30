package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.btree.BTree;
import com.snoworca.fxstore.btree.BTreeCursor;
import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.core.CodecUpgradeContext;
import com.snoworca.fxstore.core.FxStoreImpl;

import java.util.*;
import java.util.Objects;

/**
 * BTree 기반 Deque 구현
 *
 * <p>시퀀스 번호를 키로 사용하여 FIFO/LIFO 지원
 * - headSeq: deque 앞쪽 시퀀스
 * - tailSeq: deque 뒤쪽 시퀀스
 * - addFirst: --headSeq
 * - addLast: tailSeq++
 *
 * <p>v0.7 시퀀스 인코딩:
 * - OrderedSeqEncoder: 바이트 순서 = 논리적 순서 (INV-DQ1)
 * - peekFirst/peekLast O(log n) 지원
 *
 * <p>SOLID 준수:
 * - SRP: Deque 연산만 담당
 * - DIP: BTree, FxCodec, SeqEncoder에 의존
 *
 * @param <E> 원소 타입
 */
public class FxDequeImpl<E> implements Deque<E>, FxCollection {

    private final FxStoreImpl store;
    private final long collectionId;
    private final FxCodec<E> elementCodec;
    private final CodecUpgradeContext elementUpgradeContext;

    /**
     * 시퀀스 인코더 (v0.7)
     *
     * <p>OrderedSeqEncoder: 바이트 순서 = 논리적 순서</p>
     * <p>LegacySeqEncoder: 마이그레이션 전 호환</p>
     *
     * <p>마이그레이션 시 {@link #setSeqEncoder(SeqEncoder)}로 변경 가능</p>
     */
    private volatile SeqEncoder seqEncoder;

    // volatile for wait-free reads (INV-C3)
    private volatile long headSeq; // 앞쪽 시퀀스 (감소)
    private volatile long tailSeq; // 뒤쪽 시퀀스 (증가)

    /**
     * 생성자 (기본: LegacySeqEncoder for backward compatibility)
     *
     * <p>v0.7에서 OrderedSeqEncoder로 마이그레이션하려면
     * {@link #FxDequeImpl(FxStoreImpl, long, FxCodec, long, long, CodecUpgradeContext, SeqEncoder)}
     * 생성자를 사용하여 명시적으로 인코더를 지정하세요.</p>
     *
     * @param store FxStore 구현
     * @param collectionId 컬렉션 ID
     * @param elementCodec 원소 코덱
     * @param headSeq 초기 headSeq
     * @param tailSeq 초기 tailSeq
     * @param elementUpgradeContext 원소 업그레이드 컨텍스트 (null 가능)
     */
    public FxDequeImpl(FxStoreImpl store, long collectionId,
                       FxCodec<E> elementCodec, long headSeq, long tailSeq,
                       CodecUpgradeContext elementUpgradeContext) {
        this(store, collectionId, elementCodec, headSeq, tailSeq,
             elementUpgradeContext, LegacySeqEncoder.getInstance());
    }

    /**
     * 생성자 (명시적 인코더 지정)
     *
     * @param store FxStore 구현
     * @param collectionId 컬렉션 ID
     * @param elementCodec 원소 코덱
     * @param headSeq 초기 headSeq
     * @param tailSeq 초기 tailSeq
     * @param elementUpgradeContext 원소 업그레이드 컨텍스트 (null 가능)
     * @param seqEncoder 시퀀스 인코더
     */
    public FxDequeImpl(FxStoreImpl store, long collectionId,
                       FxCodec<E> elementCodec, long headSeq, long tailSeq,
                       CodecUpgradeContext elementUpgradeContext,
                       SeqEncoder seqEncoder) {
        this.store = store;
        this.collectionId = collectionId;
        this.elementCodec = elementCodec;
        this.headSeq = headSeq;
        this.tailSeq = tailSeq;
        this.elementUpgradeContext = elementUpgradeContext;
        this.seqEncoder = seqEncoder != null ? seqEncoder : OrderedSeqEncoder.getInstance();
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
        return store.getBTreeForCollection(collectionId);
    }

    /**
     * 시퀀스 인코딩 (v0.7: SeqEncoder 사용)
     *
     * <p>INV-DQ1 보장: 인코딩된 바이트 순서 = 논리적 순서</p>
     *
     * @param seq 시퀀스 번호
     * @return 8바이트 인코딩 결과
     */
    private byte[] encodeSeq(long seq) {
        return seqEncoder.encode(seq);
    }

    /**
     * 시퀀스 인코더 반환 (테스트/마이그레이션용)
     *
     * @return 현재 사용 중인 SeqEncoder
     */
    public SeqEncoder getSeqEncoder() {
        return seqEncoder;
    }

    /**
     * 시퀀스 인코더 변경 (마이그레이션용)
     *
     * <p>DequeMigrator에서 LEGACY → ORDERED 마이그레이션 시 사용</p>
     * <p>주의: 반드시 쓰기 락 내에서 호출해야 합니다.</p>
     *
     * @param encoder 새 시퀀스 인코더 (null 불가)
     * @throws NullPointerException encoder가 null인 경우
     * @since 0.7
     */
    public void setSeqEncoder(SeqEncoder encoder) {
        if (encoder == null) {
            throw new NullPointerException("encoder cannot be null");
        }
        this.seqEncoder = encoder;
    }

    private byte[] encodeElement(E element) {
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }
        return elementCodec.encode(element);
    }

    private E decodeElement(byte[] bytes) {
        // 업그레이드 적용
        if (elementUpgradeContext != null) {
            bytes = elementUpgradeContext.upgradeIfNeeded(bytes);
        }
        return elementCodec.decode(bytes);
    }

    /**
     * 현재 스냅샷에서 이 컬렉션의 root page ID를 가져옵니다.
     * Wait-free read를 위해 snapshot을 통해 접근합니다.
     */
    private long getCurrentRootPageId() {
        Long rootPageId = store.snapshot().getRootPageId(collectionId);
        return rootPageId != null ? rootPageId : 0;
    }
    
    @Override
    public void addFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }

        byte[] valueBytes = encodeElement(e);

        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            long currentRoot = getCurrentRootPageId();
            BTree btree = getBTree();

            long newHeadSeq = headSeq - 1;
            byte[] keyBytes = encodeSeq(newHeadSeq);
            long valueRecordId = store.writeValueRecord(valueBytes);

            // COW: stateless insert
            BTree.StatelessInsertResult result = btree.insertWithRoot(currentRoot, keyBytes, valueRecordId);

            // 상태 업데이트 (Lock 내에서)
            headSeq = newHeadSeq;

            // v0.7: count도 함께 업데이트 (O(1) size 지원)
            long newCount = tailSeq - headSeq;
            store.updateCollectionRootCountAndPublish(collectionId, result.newRootPageId, newCount);
            store.commitIfAuto();
        } finally {
            store.releaseWriteLock(stamp);
        }
    }
    
    @Override
    public void addLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }

        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            addLastUnlocked(e);
        } finally {
            store.releaseWriteLock(stamp);
        }
    }

    /**
     * Lock 없이 마지막에 요소 추가 (마이그레이션용)
     *
     * <p>호출자가 이미 write lock을 보유하고 있어야 합니다.</p>
     * <p>주로 {@link com.snoworca.fxstore.migration.DequeMigrator}에서 사용됩니다.</p>
     *
     * @param e 추가할 요소
     * @throws NullPointerException e가 null인 경우
     * @since 0.7
     */
    public void addLastUnlocked(E e) {
        if (e == null) {
            throw new NullPointerException();
        }

        byte[] valueBytes = encodeElement(e);
        long currentRoot = getCurrentRootPageId();
        BTree btree = getBTree();

        byte[] keyBytes = encodeSeq(tailSeq);
        long valueRecordId = store.writeValueRecord(valueBytes);

        // COW: stateless insert
        BTree.StatelessInsertResult result = btree.insertWithRoot(currentRoot, keyBytes, valueRecordId);

        // 상태 업데이트 (Lock 내에서)
        tailSeq++;

        // v0.7: count도 함께 업데이트 (O(1) size 지원)
        long newCount = tailSeq - headSeq;
        store.updateCollectionRootCountAndPublish(collectionId, result.newRootPageId, newCount);
        store.commitIfAuto();
    }
    
    @Override
    public boolean offerFirst(E e) {
        try {
            addFirst(e);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    @Override
    public boolean offerLast(E e) {
        try {
            addLast(e);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    @Override
    public E removeFirst() {
        E element = pollFirst();
        if (element == null) {
            throw new NoSuchElementException();
        }
        return element;
    }
    
    @Override
    public E removeLast() {
        E element = pollLast();
        if (element == null) {
            throw new NoSuchElementException();
        }
        return element;
    }
    
    @Override
    public E pollFirst() {
        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            return pollFirstUnlocked();
        } finally {
            store.releaseWriteLock(stamp);
        }
    }

    /**
     * Lock 없이 첫 번째 요소 제거 (마이그레이션용)
     *
     * <p>호출자가 이미 write lock을 보유하고 있어야 합니다.</p>
     * <p>주로 {@link com.snoworca.fxstore.migration.DequeMigrator}에서 사용됩니다.</p>
     *
     * @return 첫 번째 요소 또는 null (비어있는 경우)
     * @since 0.7
     */
    public E pollFirstUnlocked() {
        // Lock 내에서 empty 체크 (headSeq, tailSeq가 Lock 내에서만 변경되므로)
        if (headSeq >= tailSeq) {
            return null;
        }

        long currentRoot = getCurrentRootPageId();
        BTree btree = getBTree();

        byte[] keyBytes = encodeSeq(headSeq);

        // Wait-free read within lock (stateless API)
        Long valueRecordId = btree.findWithRoot(currentRoot, keyBytes);

        if (valueRecordId == null) {
            return null;
        }

        byte[] valueBytes = store.readValueRecord(valueRecordId);
        E element = decodeElement(valueBytes);

        // COW: stateless delete
        BTree.StatelessDeleteResult result = btree.deleteWithRoot(currentRoot, keyBytes);

        // 상태 업데이트 (Lock 내에서)
        headSeq++;

        // v0.7: count도 함께 업데이트 (O(1) size 지원)
        long newCount = tailSeq - headSeq;
        store.updateCollectionRootCountAndPublish(collectionId, result.newRootPageId, newCount);
        store.commitIfAuto();

        return element;
    }
    
    @Override
    public E pollLast() {
        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            // Lock 내에서 empty 체크
            if (headSeq >= tailSeq) {
                return null;
            }

            long currentRoot = getCurrentRootPageId();
            BTree btree = getBTree();

            long newTailSeq = tailSeq - 1;
            byte[] keyBytes = encodeSeq(newTailSeq);

            // Wait-free read within lock (stateless API)
            Long valueRecordId = btree.findWithRoot(currentRoot, keyBytes);

            if (valueRecordId == null) {
                return null;
            }

            byte[] valueBytes = store.readValueRecord(valueRecordId);
            E element = decodeElement(valueBytes);

            // COW: stateless delete
            BTree.StatelessDeleteResult result = btree.deleteWithRoot(currentRoot, keyBytes);

            // 상태 업데이트 (Lock 내에서)
            tailSeq = newTailSeq;

            // v0.7: count도 함께 업데이트 (O(1) size 지원)
            long newCount = tailSeq - headSeq;
            store.updateCollectionRootCountAndPublish(collectionId, result.newRootPageId, newCount);
            store.commitIfAuto();

            return element;
        } finally {
            store.releaseWriteLock(stamp);
        }
    }
    
    @Override
    public E getFirst() {
        E element = peekFirst();
        if (element == null) {
            throw new NoSuchElementException();
        }
        return element;
    }
    
    @Override
    public E getLast() {
        E element = peekLast();
        if (element == null) {
            throw new NoSuchElementException();
        }
        return element;
    }
    
    @Override
    public E peekFirst() {
        // Wait-free read (INV-C3): volatile 읽기
        long currentHeadSeq = headSeq;
        long currentTailSeq = tailSeq;

        if (currentHeadSeq >= currentTailSeq) {
            return null;
        }

        byte[] keyBytes = encodeSeq(currentHeadSeq);
        BTree btree = getBTree();

        // Wait-free read: snapshot의 rootPageId 사용
        long rootPageId = getCurrentRootPageId();
        Long valueRecordId = btree.findWithRoot(rootPageId, keyBytes);

        if (valueRecordId == null) {
            return null;
        }

        byte[] valueBytes = store.readValueRecord(valueRecordId);
        return decodeElement(valueBytes);
    }
    
    @Override
    public E peekLast() {
        // Wait-free read (INV-C3): volatile 읽기
        long currentHeadSeq = headSeq;
        long currentTailSeq = tailSeq;

        if (currentHeadSeq >= currentTailSeq) {
            return null;
        }

        byte[] keyBytes = encodeSeq(currentTailSeq - 1);
        BTree btree = getBTree();

        // Wait-free read: snapshot의 rootPageId 사용
        long rootPageId = getCurrentRootPageId();
        Long valueRecordId = btree.findWithRoot(rootPageId, keyBytes);

        if (valueRecordId == null) {
            return null;
        }

        byte[] valueBytes = store.readValueRecord(valueRecordId);
        return decodeElement(valueBytes);
    }
    
    /**
     * 지정된 요소의 첫 번째 발생을 제거합니다.
     *
     * <p>시간 복잡도: O(N) - 선형 검색 + 삭제
     * <p>공간 복잡도: O(N) - 재구성을 위한 임시 저장
     *
     * @param o 제거할 요소 (null 가능)
     * @return 요소가 존재하여 제거되었으면 true
     */
    @Override
    public boolean removeFirstOccurrence(Object o) {
        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            if (headSeq >= tailSeq) {
                return false;
            }

            long currentRoot = getCurrentRootPageId();
            BTree btree = getBTree();

            // 모든 요소를 수집
            List<E> elements = new ArrayList<>();
            List<byte[]> keys = new ArrayList<>();
            for (long seq = headSeq; seq < tailSeq; seq++) {
                byte[] keyBytes = encodeSeq(seq);
                keys.add(keyBytes);
                Long valueRecordId = btree.findWithRoot(currentRoot, keyBytes);
                if (valueRecordId != null) {
                    byte[] valueBytes = store.readValueRecord(valueRecordId);
                    elements.add(decodeElement(valueBytes));
                }
            }

            // 첫 번째 일치 항목 찾기
            int indexToRemove = -1;
            for (int i = 0; i < elements.size(); i++) {
                E element = elements.get(i);
                if (Objects.equals(element, o)) {
                    indexToRemove = i;
                    break;
                }
            }

            if (indexToRemove == -1) {
                return false;
            }

            // 모든 요소 삭제 (COW)
            long root = currentRoot;
            for (byte[] key : keys) {
                BTree.StatelessDeleteResult result = btree.deleteWithRoot(root, key);
                root = result.newRootPageId;
            }

            // headSeq/tailSeq 초기화
            headSeq = 0;
            tailSeq = 0;

            // 해당 인덱스를 제외하고 다시 추가 (COW)
            for (int i = 0; i < elements.size(); i++) {
                if (i != indexToRemove) {
                    byte[] keyBytes = encodeSeq(tailSeq);
                    byte[] valueBytes = encodeElement(elements.get(i));
                    long valueRecordId = store.writeValueRecord(valueBytes);
                    BTree.StatelessInsertResult result = btree.insertWithRoot(root, keyBytes, valueRecordId);
                    root = result.newRootPageId;
                    tailSeq++;
                }
            }

            // Atomic snapshot switch (INV-C4)
            store.updateCollectionRootAndPublish(collectionId, root);
            store.commitIfAuto();

            return true;
        } finally {
            store.releaseWriteLock(stamp);
        }
    }

    /**
     * 지정된 요소의 마지막 발생을 제거합니다.
     *
     * <p>시간 복잡도: O(N) - 전체 순회 + 삭제
     * <p>공간 복잡도: O(N) - 재구성을 위한 임시 저장
     *
     * @param o 제거할 요소 (null 가능)
     * @return 요소가 존재하여 제거되었으면 true
     */
    @Override
    public boolean removeLastOccurrence(Object o) {
        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            if (headSeq >= tailSeq) {
                return false;
            }

            long currentRoot = getCurrentRootPageId();
            BTree btree = getBTree();

            // 모든 요소를 수집
            List<E> elements = new ArrayList<>();
            List<byte[]> keys = new ArrayList<>();
            for (long seq = headSeq; seq < tailSeq; seq++) {
                byte[] keyBytes = encodeSeq(seq);
                keys.add(keyBytes);
                Long valueRecordId = btree.findWithRoot(currentRoot, keyBytes);
                if (valueRecordId != null) {
                    byte[] valueBytes = store.readValueRecord(valueRecordId);
                    elements.add(decodeElement(valueBytes));
                }
            }

            // 마지막 일치 항목 찾기
            int indexToRemove = -1;
            for (int i = 0; i < elements.size(); i++) {
                E element = elements.get(i);
                if (Objects.equals(element, o)) {
                    indexToRemove = i;
                }
            }

            if (indexToRemove == -1) {
                return false;
            }

            // 모든 요소 삭제 (COW)
            long root = currentRoot;
            for (byte[] key : keys) {
                BTree.StatelessDeleteResult result = btree.deleteWithRoot(root, key);
                root = result.newRootPageId;
            }

            // headSeq/tailSeq 초기화
            headSeq = 0;
            tailSeq = 0;

            // 해당 인덱스를 제외하고 다시 추가 (COW)
            for (int i = 0; i < elements.size(); i++) {
                if (i != indexToRemove) {
                    byte[] keyBytes = encodeSeq(tailSeq);
                    byte[] valueBytes = encodeElement(elements.get(i));
                    long valueRecordId = store.writeValueRecord(valueBytes);
                    BTree.StatelessInsertResult result = btree.insertWithRoot(root, keyBytes, valueRecordId);
                    root = result.newRootPageId;
                    tailSeq++;
                }
            }

            // Atomic snapshot switch (INV-C4)
            store.updateCollectionRootAndPublish(collectionId, root);
            store.commitIfAuto();

            return true;
        } finally {
            store.releaseWriteLock(stamp);
        }
    }
    
    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }
    
    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }
    
    @Override
    public E remove() {
        return removeFirst();
    }
    
    @Override
    public E poll() {
        return pollFirst();
    }
    
    @Override
    public E element() {
        return getFirst();
    }
    
    @Override
    public E peek() {
        return peekFirst();
    }
    
    @Override
    public void push(E e) {
        addFirst(e);
    }
    
    @Override
    public E pop() {
        return removeFirst();
    }
    
    @Override
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }
    
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }

        // Wait-free read (INV-C3)
        long currentHeadSeq = headSeq;
        long currentTailSeq = tailSeq;
        long rootPageId = getCurrentRootPageId();
        BTree btree = getBTree();

        for (long seq = currentHeadSeq; seq < currentTailSeq; seq++) {
            byte[] keyBytes = encodeSeq(seq);
            Long valueRecordId = btree.findWithRoot(rootPageId, keyBytes);
            if (valueRecordId != null) {
                byte[] valueBytes = store.readValueRecord(valueRecordId);
                E element = decodeElement(valueBytes);
                if (o.equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public int size() {
        return (int) (tailSeq - headSeq);
    }
    
    @Override
    public boolean isEmpty() {
        return headSeq >= tailSeq;
    }
    
    @Override
    public Iterator<E> iterator() {
        // Wait-free read (INV-C3): 스냅샷 기반 iterator
        long currentHeadSeq = headSeq;
        long currentTailSeq = tailSeq;
        long rootPageId = getCurrentRootPageId();
        BTree btree = getBTree();

        List<E> elements = new ArrayList<>();
        for (long seq = currentHeadSeq; seq < currentTailSeq; seq++) {
            byte[] keyBytes = encodeSeq(seq);
            Long valueRecordId = btree.findWithRoot(rootPageId, keyBytes);
            if (valueRecordId != null) {
                byte[] valueBytes = store.readValueRecord(valueRecordId);
                elements.add(decodeElement(valueBytes));
            }
        }
        return elements.iterator();
    }
    
    /**
     * 이 Deque의 요소를 역순으로 순회하는 Iterator를 반환합니다.
     *
     * <p>시간 복잡도: O(N) - 전체 요소 수집 필요
     * <p>공간 복잡도: O(N) - 요소 목록 임시 저장
     *
     * <p>Iterator 특성:
     * <ul>
     *   <li>remove() 지원 안 함 (UnsupportedOperationException)</li>
     *   <li>스냅샷 기반: 생성 후 원본 변경 반영 안 됨</li>
     * </ul>
     *
     * @return 역순 Iterator
     */
    @Override
    public Iterator<E> descendingIterator() {
        // Wait-free read (INV-C3): 스냅샷 기반 iterator
        long currentHeadSeq = headSeq;
        long currentTailSeq = tailSeq;
        long rootPageId = getCurrentRootPageId();
        BTree btree = getBTree();

        List<E> elements = new ArrayList<>();
        for (long seq = currentHeadSeq; seq < currentTailSeq; seq++) {
            byte[] keyBytes = encodeSeq(seq);
            Long valueRecordId = btree.findWithRoot(rootPageId, keyBytes);
            if (valueRecordId != null) {
                byte[] valueBytes = store.readValueRecord(valueRecordId);
                elements.add(decodeElement(valueBytes));
            }
        }
        Collections.reverse(elements);
        return Collections.unmodifiableList(elements).iterator();
    }
    
    @Override
    public Object[] toArray() {
        List<Object> list = new ArrayList<Object>();
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list.toArray();
    }
    
    @Override
    public <T> T[] toArray(T[] a) {
        List<E> list = new ArrayList<E>();
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list.toArray(a);
    }
    
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }
    
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            if (remove(o)) {
                modified = true;
            }
        }
        return modified;
    }
    
    /**
     * 지정된 컬렉션에 포함된 요소만 유지합니다.
     *
     * <p>시간 복잡도: O(N) - 전체 순회 + 재구성
     * <p>공간 복잡도: O(N) - 유지할 요소 임시 저장
     *
     * @param c 유지할 요소를 포함하는 컬렉션
     * @return 이 deque가 변경되었으면 true
     * @throws NullPointerException c가 null인 경우
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);

        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            if (headSeq >= tailSeq) {
                return false;
            }

            long currentRoot = getCurrentRootPageId();
            BTree btree = getBTree();

            // 모든 요소를 수집
            List<E> elements = new ArrayList<>();
            List<byte[]> keys = new ArrayList<>();
            for (long seq = headSeq; seq < tailSeq; seq++) {
                byte[] keyBytes = encodeSeq(seq);
                keys.add(keyBytes);
                Long valueRecordId = btree.findWithRoot(currentRoot, keyBytes);
                if (valueRecordId != null) {
                    byte[] valueBytes = store.readValueRecord(valueRecordId);
                    elements.add(decodeElement(valueBytes));
                }
            }

            // 유지할 요소 필터링
            List<E> toRetain = new ArrayList<>();
            for (E element : elements) {
                if (c.contains(element)) {
                    toRetain.add(element);
                }
            }

            // 변경 여부 확인
            if (toRetain.size() == elements.size()) {
                return false;
            }

            // 모든 요소 삭제 (COW)
            long root = currentRoot;
            for (byte[] key : keys) {
                BTree.StatelessDeleteResult result = btree.deleteWithRoot(root, key);
                root = result.newRootPageId;
            }

            // headSeq/tailSeq 초기화
            headSeq = 0;
            tailSeq = 0;

            // 유지할 요소만 다시 추가 (COW)
            for (E element : toRetain) {
                byte[] keyBytes = encodeSeq(tailSeq);
                byte[] valueBytes = encodeElement(element);
                long valueRecordId = store.writeValueRecord(valueBytes);
                BTree.StatelessInsertResult result = btree.insertWithRoot(root, keyBytes, valueRecordId);
                root = result.newRootPageId;
                tailSeq++;
            }

            // Atomic snapshot switch (INV-C4)
            store.updateCollectionRootAndPublish(collectionId, root);
            store.commitIfAuto();

            return true;
        } finally {
            store.releaseWriteLock(stamp);
        }
    }
    
    @Override
    public void clear() {
        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            if (headSeq >= tailSeq) {
                return; // 이미 비어있음
            }

            long currentRoot = getCurrentRootPageId();
            BTree btree = getBTree();

            // 모든 요소 삭제 (COW)
            long root = currentRoot;
            for (long seq = headSeq; seq < tailSeq; seq++) {
                byte[] keyBytes = encodeSeq(seq);
                BTree.StatelessDeleteResult result = btree.deleteWithRoot(root, keyBytes);
                root = result.newRootPageId;
            }

            // headSeq/tailSeq 초기화
            headSeq = 0;
            tailSeq = 0;

            // Atomic snapshot switch (INV-C4)
            store.updateCollectionRootAndPublish(collectionId, root);
            store.commitIfAuto();
        } finally {
            store.releaseWriteLock(stamp);
        }
    }
}
