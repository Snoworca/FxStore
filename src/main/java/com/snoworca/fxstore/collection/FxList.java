package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.core.CodecUpgradeContext;
import com.snoworca.fxstore.core.FxStoreImpl;
import com.snoworca.fxstore.ost.OST;

import java.util.*;

/**
 * FxStore List 구현.
 *
 * <p>Order-Statistic Tree(OST) 기반으로 인덱스 접근을 O(log n)에 제공합니다.</p>
 *
 * <p>주요 연산 복잡도:</p>
 * <ul>
 *   <li>get(index): O(log n)</li>
 *   <li>add(element): O(log n)</li>
 *   <li>add(index, element): O(log n)</li>
 *   <li>remove(index): O(log n)</li>
 *   <li>size(): O(1)</li>
 * </ul>
 *
 * <h3>Iterator 제한 사항</h3>
 * <p>이 클래스의 Iterator와 ListIterator는 <b>읽기 전용</b>입니다.
 * 다음 메서드는 {@link UnsupportedOperationException}을 발생시킵니다:</p>
 * <ul>
 *   <li>{@link java.util.Iterator#remove()} - 대신 {@link #remove(int)} 사용</li>
 *   <li>{@link java.util.ListIterator#remove()} - 대신 {@link #remove(int)} 사용</li>
 *   <li>{@link java.util.ListIterator#set(Object)} - 대신 {@link #set(int, Object)} 사용</li>
 *   <li>{@link java.util.ListIterator#add(Object)} - 대신 {@link #add(int, Object)} 사용</li>
 * </ul>
 *
 * <p><b>설계 근거:</b> 스냅샷 기반 Iterator는 반복 중 리스트가 변경되어도 일관된 뷰를 제공합니다.
 * ConcurrentModificationException 없이 안전하게 반복할 수 있습니다.
 * Iterator를 통한 수정은 스냅샷 무결성을 깨트리므로 의도적으로 지원하지 않습니다.</p>
 *
 * <h3>동시성 지원 (Phase 8)</h3>
 * <ul>
 *   <li>Wait-free Read (INV-C3): snapshot의 rootPageId 사용</li>
 *   <li>Single Writer (INV-C1): Write Lock으로 직렬화</li>
 *   <li>COW (INV-C4): Stateless OST API로 불변성 보장</li>
 * </ul>
 *
 * @param <E> 원소 타입
 * @see java.util.List
 * @see java.util.ListIterator
 */
public class FxList<E> extends AbstractList<E> implements List<E>, FxCollection {

    // 동시성 지원을 위한 필드 (Phase 8)
    private final FxStoreImpl store;
    private final long collectionId;

    private final OST ost;
    private final FxCodec<E> codec;
    private final RecordStore recordStore;
    private final CodecUpgradeContext elementUpgradeContext;

    /**
     * FxList 생성자 (동시성 지원).
     *
     * @param store FxStore 구현체 (Lock 및 snapshot 관리)
     * @param collectionId 컬렉션 ID
     * @param ost Order-Statistic Tree
     * @param codec 요소 코덱
     * @param recordStore 레코드 저장소
     * @param elementUpgradeContext 요소 업그레이드 컨텍스트 (null 가능)
     */
    public FxList(FxStoreImpl store, long collectionId, OST ost,
                  FxCodec<E> codec, RecordStore recordStore,
                  CodecUpgradeContext elementUpgradeContext) {
        this.store = store;
        this.collectionId = collectionId;
        this.ost = ost;
        this.codec = codec;
        this.recordStore = recordStore;
        this.elementUpgradeContext = elementUpgradeContext;
    }

    /**
     * FxList 생성자 (하위 호환성).
     *
     * @param ost Order-Statistic Tree
     * @param codec 요소 코덱
     * @param recordStore 레코드 저장소
     * @deprecated 동시성 지원을 위해 {@link #FxList(FxStoreImpl, long, OST, FxCodec, RecordStore, CodecUpgradeContext)} 사용
     */
    @Deprecated
    public FxList(OST ost, FxCodec<E> codec, RecordStore recordStore) {
        this(null, 0, ost, codec, recordStore, null);
    }

    /**
     * FxList 생성자 (업그레이드 컨텍스트 포함, 하위 호환성).
     *
     * @param ost Order-Statistic Tree
     * @param codec 요소 코덱
     * @param recordStore 레코드 저장소
     * @param elementUpgradeContext 요소 업그레이드 컨텍스트 (null 가능)
     * @deprecated 동시성 지원을 위해 {@link #FxList(FxStoreImpl, long, OST, FxCodec, RecordStore, CodecUpgradeContext)} 사용
     */
    @Deprecated
    public FxList(OST ost, FxCodec<E> codec, RecordStore recordStore,
                  CodecUpgradeContext elementUpgradeContext) {
        this(null, 0, ost, codec, recordStore, elementUpgradeContext);
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

    // ==================== 헬퍼 메서드 ====================

    /**
     * 현재 스냅샷에서 이 컬렉션의 root page ID를 가져옵니다.
     * Wait-free read를 위해 snapshot을 통해 접근합니다.
     */
    private long getCurrentRootPageId() {
        if (store == null) {
            // 하위 호환: store 없으면 OST의 rootPageId 직접 사용
            return ost.getRootPageId();
        }
        Long rootPageId = store.snapshot().getRootPageId(collectionId);
        return rootPageId != null ? rootPageId : 0;
    }

    /**
     * 동시성 지원 여부 확인
     */
    private boolean isConcurrencyEnabled() {
        return store != null;
    }

    /**
     * 바이트 데이터를 요소로 디코딩 (업그레이드 적용)
     */
    private E decodeElement(byte[] data) {
        if (elementUpgradeContext != null) {
            data = elementUpgradeContext.upgradeIfNeeded(data);
        }
        return codec.decode(data);
    }

    // ==================== Read 연산 (Wait-free) ====================

    @Override
    public int size() {
        // Wait-free read (INV-C3)
        long rootPageId = getCurrentRootPageId();
        return ost.sizeWithRoot(rootPageId);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public E get(int index) {
        // Wait-free read (INV-C3)
        long rootPageId = getCurrentRootPageId();
        int currentSize = ost.sizeWithRoot(rootPageId);

        if (index < 0 || index >= currentSize) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for size " + currentSize);
        }

        long recordId = ost.getWithRoot(rootPageId, index);
        byte[] data = recordStore.readRecord(recordId);
        return decodeElement(data);
    }

    // ==================== Write 연산 (Single Writer) ====================

    @Override
    public E set(int index, E element) {
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }

        byte[] encoded = codec.encode(element);

        if (!isConcurrencyEnabled()) {
            // 하위 호환: 기존 로직
            int currentSize = ost.size();
            if (index < 0 || index >= currentSize) {
                throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for size " + currentSize);
            }

            E oldValue = get(index);

            // 새 레코드 작성
            long newRecordId = recordStore.writeRecord(encoded);

            // OST 업데이트 (제거 후 삽입)
            long oldRecordId = ost.get(index);
            ost.remove(index);
            ost.insert(index, newRecordId);

            // 구 레코드 삭제
            recordStore.deleteRecord(oldRecordId);

            return oldValue;
        }

        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            long currentRoot = getCurrentRootPageId();
            int currentSize = ost.sizeWithRoot(currentRoot);

            if (index < 0 || index >= currentSize) {
                throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for size " + currentSize);
            }

            // 기존 값 읽기
            long oldRecordId = ost.getWithRoot(currentRoot, index);
            byte[] oldData = recordStore.readRecord(oldRecordId);
            E oldElement = decodeElement(oldData);

            // 새 레코드 작성
            long newRecordId = recordStore.writeRecord(encoded);

            // COW: remove + insert
            OST.StatelessRemoveResult removeResult = ost.removeWithRoot(currentRoot, index);
            OST.StatelessInsertResult insertResult = ost.insertWithRoot(
                removeResult.newRootPageId, index, newRecordId);

            // Atomic snapshot switch (INV-C4)
            store.updateCollectionRootAndPublish(collectionId, insertResult.newRootPageId);
            store.commitIfAuto();

            return oldElement;
        } finally {
            store.releaseWriteLock(stamp);
        }
    }

    @Override
    public boolean add(E element) {
        add(size(), element);
        return true;
    }

    @Override
    public void add(int index, E element) {
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }

        byte[] encoded = codec.encode(element);

        if (!isConcurrencyEnabled()) {
            // 하위 호환: 기존 로직
            int currentSize = ost.size();
            if (index < 0 || index > currentSize) {
                throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for insert (size " + currentSize + ")");
            }

            long recordId = recordStore.writeRecord(encoded);
            ost.insert(index, recordId);
            return;
        }

        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            long currentRoot = getCurrentRootPageId();
            int currentSize = ost.sizeWithRoot(currentRoot);

            if (index < 0 || index > currentSize) {
                throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for insert (size " + currentSize + ")");
            }

            long recordId = recordStore.writeRecord(encoded);

            // COW: stateless insert
            OST.StatelessInsertResult result = ost.insertWithRoot(currentRoot, index, recordId);

            // Atomic snapshot switch (INV-C4)
            store.updateCollectionRootAndPublish(collectionId, result.newRootPageId);
            store.commitIfAuto();
        } finally {
            store.releaseWriteLock(stamp);
        }
    }

    @Override
    public E remove(int index) {
        if (!isConcurrencyEnabled()) {
            // 하위 호환: 기존 로직
            int currentSize = ost.size();
            if (index < 0 || index >= currentSize) {
                throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for size " + currentSize);
            }

            E oldValue = get(index);
            long recordId = ost.remove(index);
            recordStore.deleteRecord(recordId);

            return oldValue;
        }

        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            long currentRoot = getCurrentRootPageId();
            int currentSize = ost.sizeWithRoot(currentRoot);

            if (index < 0 || index >= currentSize) {
                throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for size " + currentSize);
            }

            // 먼저 값을 읽음
            long recordId = ost.getWithRoot(currentRoot, index);
            byte[] data = recordStore.readRecord(recordId);
            E element = decodeElement(data);

            // COW: stateless remove
            OST.StatelessRemoveResult result = ost.removeWithRoot(currentRoot, index);

            // Atomic snapshot switch (INV-C4)
            store.updateCollectionRootAndPublish(collectionId, result.newRootPageId);
            store.commitIfAuto();

            return element;
        } finally {
            store.releaseWriteLock(stamp);
        }
    }

    @Override
    public void clear() {
        if (!isConcurrencyEnabled()) {
            // 하위 호환: 기존 로직
            while (size() > 0) {
                remove(0);
            }
            return;
        }

        // Single Writer (INV-C1): Write Lock 획득
        long stamp = store.acquireWriteLock();
        try {
            long currentRoot = getCurrentRootPageId();
            int currentSize = ost.sizeWithRoot(currentRoot);

            if (currentSize == 0) {
                return; // 이미 비어있음
            }

            // 모든 요소 순차 삭제 (COW) - 역순으로 삭제
            long root = currentRoot;
            for (int i = currentSize - 1; i >= 0; i--) {
                OST.StatelessRemoveResult result = ost.removeWithRoot(root, i);
                root = result.newRootPageId;
            }

            // Atomic snapshot switch (INV-C4)
            store.updateCollectionRootAndPublish(collectionId, root);
            store.commitIfAuto();
        } finally {
            store.releaseWriteLock(stamp);
        }
    }

    // ==================== Iterator (스냅샷 기반) ====================

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        // Wait-free read: 스냅샷 기반 읽기 전용 iterator
        long rootPageId = getCurrentRootPageId();
        int currentSize = ost.sizeWithRoot(rootPageId);

        if (index < 0 || index > currentSize) {
            throw new IndexOutOfBoundsException("Index " + index);
        }

        // 스냅샷 시점의 모든 요소 수집
        List<E> snapshot = new ArrayList<>(currentSize);
        for (int i = 0; i < currentSize; i++) {
            long recordId = ost.getWithRoot(rootPageId, i);
            byte[] data = recordStore.readRecord(recordId);
            snapshot.add(decodeElement(data));
        }

        return new SnapshotListIterator(snapshot, index);
    }

    /**
     * 스냅샷 기반 읽기 전용 ListIterator.
     *
     * <p>쓰기 연산(remove, set, add)은 UnsupportedOperationException을 발생시킵니다.</p>
     * <p>스냅샷 시점의 데이터를 반환하므로, iterator 생성 후 원본이 변경되어도 반영되지 않습니다.</p>
     */
    private class SnapshotListIterator implements ListIterator<E> {
        private final List<E> snapshot;
        private int cursor;

        SnapshotListIterator(List<E> snapshot, int index) {
            this.snapshot = snapshot;
            this.cursor = index;
        }

        @Override
        public boolean hasNext() {
            return cursor < snapshot.size();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return snapshot.get(cursor++);
        }

        @Override
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @Override
        public E previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            return snapshot.get(--cursor);
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                "Snapshot iterator does not support remove. Use FxList.remove(index) instead.");
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException(
                "Snapshot iterator does not support set. Use FxList.set(index, element) instead.");
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException(
                "Snapshot iterator does not support add. Use FxList.add(index, element) instead.");
        }
    }

    // ==================== RecordStore 인터페이스 ====================

    /**
     * 레코드 저장소 인터페이스.
     *
     * <p>FxList가 요소를 저장하고 읽기 위한 추상화입니다.</p>
     */
    public interface RecordStore {
        /**
         * 레코드를 작성하고 ID를 반환합니다.
         *
         * @param data 레코드 데이터
         * @return 레코드 ID
         */
        long writeRecord(byte[] data);

        /**
         * 레코드를 읽습니다.
         *
         * @param recordId 레코드 ID
         * @return 레코드 데이터
         */
        byte[] readRecord(long recordId);

        /**
         * 레코드를 삭제합니다.
         *
         * @param recordId 레코드 ID
         */
        void deleteRecord(long recordId);
    }
}
