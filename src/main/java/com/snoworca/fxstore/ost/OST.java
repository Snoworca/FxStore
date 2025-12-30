package com.snoworca.fxstore.ost;

import com.snoworca.fxstore.storage.Allocator;
import com.snoworca.fxstore.storage.Storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Order-Statistic Tree 구현.
 * 
 * <p>인덱스 기반 List 연산을 O(log n)에 제공합니다.</p>
 * 
 * <p>주요 연산:</p>
 * <ul>
 *   <li>get(index): O(log n)</li>
 *   <li>insert(index, element): O(log n)</li>
 *   <li>remove(index): O(log n)</li>
 * </ul>
 * 
 * <p>불변식 INV-7: 모든 내부 노드의 subtreeCount는 자식들의 subtreeCount 합과 일치</p>
 */
public class OST {

    // ==================== Stateless API 결과 타입 ====================

    /**
     * Stateless insert 연산 결과.
     * COW 방식으로 새 root page ID를 반환합니다.
     */
    public static class StatelessInsertResult {
        /** 삽입 후 새로운 root page ID */
        public final long newRootPageId;

        public StatelessInsertResult(long newRootPageId) {
            this.newRootPageId = newRootPageId;
        }
    }

    /**
     * Stateless remove 연산 결과.
     * COW 방식으로 새 root page ID와 삭제된 요소 RecordId를 반환합니다.
     */
    public static class StatelessRemoveResult {
        /** 삭제 후 새로운 root page ID */
        public final long newRootPageId;
        /** 삭제된 요소의 RecordId */
        public final long removedRecordId;

        public StatelessRemoveResult(long newRootPageId, long removedRecordId) {
            this.newRootPageId = newRootPageId;
            this.removedRecordId = removedRecordId;
        }
    }

    // ==================== 필드 ====================

    private final Storage storage;
    private final Allocator allocator;
    private final int pageSize;

    private long rootPageId;

    /**
     * 현재 allocTail (Stateless API 지원)
     *
     * <p>연산 시작 전 {@link #setAllocTail(long)}로 설정하고,
     * 연산 완료 후 {@link #getAllocTail()}로 읽어서 스냅샷에 반영합니다.</p>
     *
     * @since 0.9 (Stateless Allocator API 지원)
     */
    private long currentAllocTail;
    
    /**
     * OST 생성자.
     * 
     * @param storage 저장소
     * @param allocator 할당자
     * @param pageSize 페이지 크기
     */
    public OST(Storage storage, Allocator allocator, int pageSize) {
        this.storage = storage;
        this.allocator = allocator;
        this.pageSize = pageSize;
        this.rootPageId = 0L; // 빈 트리
    }
    
    /**
     * 루트 페이지 ID를 반환합니다.
     * 
     * @return 루트 페이지 ID (0이면 빈 트리)
     */
    public long getRootPageId() {
        return rootPageId;
    }
    
    /**
     * 루트 페이지 ID를 설정합니다.
     *
     * @param rootPageId 루트 페이지 ID
     */
    public void setRootPageId(long rootPageId) {
        this.rootPageId = rootPageId;
    }

    /**
     * 현재 allocTail 반환 (Stateless API 지원)
     *
     * <p>연산 완료 후 이 값을 스냅샷에 반영해야 합니다.</p>
     *
     * @return 현재 allocTail
     * @since 0.9
     */
    public long getAllocTail() {
        return currentAllocTail;
    }

    /**
     * allocTail 설정 (Stateless API 지원)
     *
     * <p>연산 시작 전 스냅샷에서 가져온 allocTail로 설정합니다.</p>
     *
     * @param allocTail 설정할 allocTail
     * @since 0.9
     */
    public void setAllocTail(long allocTail) {
        this.currentAllocTail = allocTail;
    }

    /**
     * OST의 전체 크기를 반환합니다.
     * 
     * @return 요소 개수
     */
    public int size() {
        if (rootPageId == 0L) {
            return 0;
        }
        
        OSTNode root = loadNode(rootPageId);
        return root.subtreeCount();
    }
    
    /**
     * OST가 비어있는지 확인합니다.
     * 
     * @return 비어있으면 true
     */
    public boolean isEmpty() {
        return rootPageId == 0L;
    }
    
    /**
     * 지정된 인덱스의 요소 RecordId를 반환합니다.
     * 
     * @param index 인덱스 (0-based)
     * @return 요소 RecordId
     * @throws IndexOutOfBoundsException 인덱스가 범위를 벗어난 경우
     */
    public long get(int index) {
        if (rootPageId == 0L) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for empty OST");
        }
        
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " is negative");
        }
        
        OSTNode node = loadNode(rootPageId);
        int remaining = index;
        
        // 트리를 하향 탐색
        while (!node.isLeaf()) {
            OSTInternal internal = (OSTInternal) node;
            int childCount = internal.getChildCount();
            int targetChild = -1;
            
            // 목표 인덱스를 포함하는 자식 찾기
            for (int i = 0; i < childCount; i++) {
                int count = internal.getSubtreeCount(i);
                if (remaining < count) {
                    targetChild = i;
                    break;
                }
                remaining -= count;
            }
            
            if (targetChild < 0) {
                throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for size " + node.subtreeCount());
            }
            
            node = loadNode(internal.getChildPageId(targetChild));
        }
        
        // 리프에 도달
        OSTLeaf leaf = (OSTLeaf) node;
        
        if (remaining >= leaf.subtreeCount()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for size " + size());
        }
        
        return leaf.getElementRecordId(remaining);
    }
    
    /**
     * 페이지 ID로부터 OSTNode를 로드합니다.
     * 
     * @param pageId 페이지 ID
     * @return OSTNode 인스턴스
     */
    private OSTNode loadNode(long pageId) {
        byte[] page = new byte[pageSize];
        storage.read(pageId, page, 0, pageSize);
        
        // pageType 확인 (0 = unknown, 1 = LEAF, 2 = INTERNAL)
        byte pageType = page[0];
        
        if (pageType == 1) {
            return OSTLeaf.fromPage(page);
        } else if (pageType == 2) {
            return OSTInternal.fromPage(page);
        } else {
            throw new IllegalStateException(
                "Invalid OST page type: " + pageType + " at pageId " + pageId);
        }
    }
    
    /**
     * OSTNode를 저장하고 오프셋을 반환합니다.
     *
     * @param node 저장할 노드
     * @return 저장된 오프셋 (storage.write의 첫 번째 인자)
     */
    private long saveNode(OSTNode node) {
        // 레거시 API 사용 (v0.9 전환 기간 동안 유지)
        // allocator.allocatePage()는 offset을 반환 (pageId가 아님!)
        long offset = allocator.allocatePage();
        byte[] page = node.toPage(pageSize);
        storage.write(offset, page, 0, pageSize);
        return offset;
    }
    
    /**
     * 빈 OST를 생성합니다.
     * 
     * @param storage 저장소
     * @param allocator 할당자
     * @param pageSize 페이지 크기
     * @return OST 인스턴스
     */
    public static OST createEmpty(Storage storage, Allocator allocator, int pageSize) {
        return new OST(storage, allocator, pageSize);
    }
    
    /**
     * 기존 OST를 오픈합니다.
     *
     * @param storage 저장소
     * @param allocator 할당자
     * @param pageSize 페이지 크기
     * @param rootPageId 루트 페이지 ID
     * @return OST 인스턴스
     */
    public static OST open(Storage storage, Allocator allocator, int pageSize, long rootPageId) {
        OST ost = new OST(storage, allocator, pageSize);
        ost.setRootPageId(rootPageId);
        return ost;
    }

    // ==================== Stateless API (Wait-free Read 지원) ====================

    /**
     * 지정된 root에서 트리의 크기를 반환합니다.
     * Wait-free read를 위한 stateless API입니다.
     *
     * @param rootPageId 조회할 트리의 root page ID (0이면 빈 트리)
     * @return 요소 개수
     */
    public int sizeWithRoot(long rootPageId) {
        if (rootPageId == 0L) {
            return 0;
        }
        OSTNode root = loadNode(rootPageId);
        return root.subtreeCount();
    }

    /**
     * 지정된 root에서 인덱스의 요소 RecordId를 반환합니다.
     * Wait-free read를 위한 stateless API입니다.
     *
     * @param rootPageId 조회할 트리의 root page ID (0이면 빈 트리)
     * @param index 인덱스 (0-based)
     * @return 요소 RecordId
     * @throws IndexOutOfBoundsException 인덱스가 범위를 벗어난 경우
     */
    public long getWithRoot(long rootPageId, int index) {
        if (rootPageId == 0L) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for empty OST");
        }

        if (index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " is negative");
        }

        OSTNode node = loadNode(rootPageId);
        int remaining = index;

        // 트리를 하향 탐색
        while (!node.isLeaf()) {
            OSTInternal internal = (OSTInternal) node;
            int childCount = internal.getChildCount();
            int targetChild = -1;

            // 목표 인덱스를 포함하는 자식 찾기
            for (int i = 0; i < childCount; i++) {
                int count = internal.getSubtreeCount(i);
                if (remaining < count) {
                    targetChild = i;
                    break;
                }
                remaining -= count;
            }

            if (targetChild < 0) {
                throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for size " + node.subtreeCount());
            }

            node = loadNode(internal.getChildPageId(targetChild));
        }

        // 리프에 도달
        OSTLeaf leaf = (OSTLeaf) node;

        if (remaining >= leaf.subtreeCount()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for size " + sizeWithRoot(rootPageId));
        }

        return leaf.getElementRecordId(remaining);
    }

    /**
     * 지정된 root에서 요소를 삽입하고 새 root를 반환합니다.
     * COW 방식으로 기존 트리는 변경되지 않습니다.
     *
     * @param rootPageId 현재 root page ID (0이면 빈 트리)
     * @param index 삽입 위치 (0-based)
     * @param elementRecordId 삽입할 요소의 RecordId
     * @return 새 root page ID를 포함한 결과
     * @throws IndexOutOfBoundsException 인덱스가 범위를 벗어난 경우
     */
    public StatelessInsertResult insertWithRoot(long rootPageId, int index, long elementRecordId) {
        int currentSize = sizeWithRoot(rootPageId);
        if (index < 0 || index > currentSize) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for size " + currentSize);
        }

        if (rootPageId == 0L) {
            // 빈 트리 - 새 리프 생성
            OSTLeaf newLeaf = new OSTLeaf();
            newLeaf.addElement(elementRecordId);
            long newRootPageId = saveNode(newLeaf);
            return new StatelessInsertResult(newRootPageId);
        }

        // 1. 검색 경로 수집
        List<OSTPathFrame> path = new ArrayList<>();
        int remaining = index;

        OSTNode node = loadNode(rootPageId);

        while (!node.isLeaf()) {
            OSTInternal internal = (OSTInternal) node;
            int childCount = internal.getChildCount();
            int targetChild = -1;
            int accum = 0;

            // 목표 인덱스를 포함하는 자식 찾기
            for (int i = 0; i < childCount; i++) {
                int count = internal.getSubtreeCount(i);
                if (remaining < accum + count) {
                    targetChild = i;
                    remaining -= accum;
                    break;
                }
                accum += count;
            }

            // 경계 케이스: 마지막 자식 뒤에 삽입 (append)
            if (targetChild < 0) {
                targetChild = childCount - 1;
                remaining -= (accum - internal.getSubtreeCount(childCount - 1));
            }

            path.add(new OSTPathFrame(internal, targetChild, remaining));
            node = loadNode(internal.getChildPageId(targetChild));
        }

        // 2. 리프에 삽입
        OSTLeaf leaf = (OSTLeaf) node;
        int localIndex = remaining;

        if (localIndex > leaf.subtreeCount()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + currentSize);
        }

        long newRootPageId;
        // 리프에 공간 있는지 확인 (최대 100개 원소)
        if (leaf.subtreeCount() < 100) {
            // 공간 있음: 단순 삽입
            OSTLeaf newLeaf = new OSTLeaf();
            for (int i = 0; i < leaf.subtreeCount(); i++) {
                if (i == localIndex) {
                    newLeaf.addElement(elementRecordId);
                }
                newLeaf.addElement(leaf.getElementRecordId(i));
            }
            if (localIndex == leaf.subtreeCount()) {
                newLeaf.addElement(elementRecordId);
            }

            long newLeafPageId = saveNode(newLeaf);
            newRootPageId = propagateOstCow(path, newLeafPageId, +1);
        } else {
            // 분할 필요
            newRootPageId = insertWithSplit(path, leaf, localIndex, elementRecordId);
        }

        return new StatelessInsertResult(newRootPageId);
    }

    /**
     * 지정된 root에서 요소를 삭제하고 새 root를 반환합니다.
     * COW 방식으로 기존 트리는 변경되지 않습니다.
     *
     * @param rootPageId 현재 root page ID
     * @param index 삭제할 인덱스 (0-based)
     * @return 새 root page ID와 삭제된 요소 RecordId를 포함한 결과
     * @throws IndexOutOfBoundsException 인덱스가 범위를 벗어난 경우
     */
    public StatelessRemoveResult removeWithRoot(long rootPageId, int index) {
        int currentSize = sizeWithRoot(rootPageId);
        if (rootPageId == 0L || index < 0 || index >= currentSize) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for size " + currentSize);
        }

        // 1. 검색 경로 수집
        List<OSTPathFrame> path = new ArrayList<>();
        int remaining = index;

        OSTNode node = loadNode(rootPageId);

        while (!node.isLeaf()) {
            OSTInternal internal = (OSTInternal) node;
            int childCount = internal.getChildCount();
            int targetChild = -1;

            for (int i = 0; i < childCount; i++) {
                int count = internal.getSubtreeCount(i);
                if (remaining < count) {
                    targetChild = i;
                    break;
                }
                remaining -= count;
            }

            if (targetChild < 0) {
                throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + currentSize);
            }

            path.add(new OSTPathFrame(internal, targetChild, remaining));
            node = loadNode(internal.getChildPageId(targetChild));
        }

        // 2. 리프에서 삭제
        OSTLeaf leaf = (OSTLeaf) node;
        int localIndex = remaining;

        if (localIndex >= leaf.subtreeCount()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + currentSize);
        }

        long removedRecordId = leaf.getElementRecordId(localIndex);

        // 새 리프 생성 (COW)
        OSTLeaf newLeaf = new OSTLeaf();
        for (int i = 0; i < leaf.subtreeCount(); i++) {
            if (i != localIndex) {
                newLeaf.addElement(leaf.getElementRecordId(i));
            }
        }

        long newRootPageId;
        if (newLeaf.subtreeCount() == 0) {
            // 리프가 비었음
            if (path.isEmpty()) {
                // 루트 리프가 비었음 - 빈 트리
                newRootPageId = 0L;
            } else {
                // 부모가 있는데 리프가 비었음 - 부모에서 자식 제거
                long newLeafPageId = saveNode(newLeaf);
                newRootPageId = propagateOstCow(path, newLeafPageId, -1);
            }
        } else {
            // 리프에 요소 남음
            long newLeafPageId = saveNode(newLeaf);
            newRootPageId = propagateOstCow(path, newLeafPageId, -1);
        }

        return new StatelessRemoveResult(newRootPageId, removedRecordId);
    }

    // ==================== Legacy API (Stateful) ====================

    /**
     * 지정된 인덱스에 요소를 삽입합니다.
     *
     * <p>COW 방식으로 구현되며, 필요 시 노드 분할이 발생합니다.</p>
     * <p>내부적으로 {@link #insertWithRoot(long, int, long)}를 호출합니다.</p>
     *
     * @param index 삽입 위치 (0-based)
     * @param elementRecordId 삽입할 요소의 RecordId
     * @throws IndexOutOfBoundsException 인덱스가 범위를 벗어난 경우
     */
    public void insert(int index, long elementRecordId) {
        StatelessInsertResult result = insertWithRoot(rootPageId, index, elementRecordId);
        this.rootPageId = result.newRootPageId;
    }

    /**
     * 지정된 인덱스의 요소를 삭제합니다.
     *
     * <p>COW 방식으로 구현됩니다.</p>
     * <p>내부적으로 {@link #removeWithRoot(long, int)}를 호출합니다.</p>
     *
     * @param index 삭제할 인덱스 (0-based)
     * @return 삭제된 요소의 RecordId
     * @throws IndexOutOfBoundsException 인덱스가 범위를 벗어난 경우
     */
    public long remove(int index) {
        StatelessRemoveResult result = removeWithRoot(rootPageId, index);
        this.rootPageId = result.newRootPageId;
        return result.removedRecordId;
    }
    
    /**
     * COW 전파 (subtreeCount 갱신 포함).
     * 
     * @param path 검색 경로
     * @param childPageId 변경된 자식 페이지 ID
     * @param countDelta 카운트 변화량 (+1 또는 -1)
     * @return 새 루트 페이지 ID
     */
    private long propagateOstCow(List<OSTPathFrame> path, long childPageId, int countDelta) {
        // 경로를 역순으로 올라가며 모든 조상 갱신
        for (int i = path.size() - 1; i >= 0; i--) {
            OSTPathFrame frame = path.get(i);
            OSTInternal internal = (OSTInternal) frame.node;
            
            // 새 내부 노드 생성 (COW)
            List<Long> newChildren = new ArrayList<>();
            List<Integer> newCounts = new ArrayList<>();
            
            for (int j = 0; j < internal.getChildCount(); j++) {
                if (j == frame.childIndex) {
                    newChildren.add(childPageId);
                    newCounts.add(internal.getSubtreeCount(j) + countDelta);
                } else {
                    newChildren.add(internal.getChildPageId(j));
                    newCounts.add(internal.getSubtreeCount(j));
                }
            }
            
            OSTInternal newInternal = new OSTInternal(internal.getLevel(), newChildren, newCounts);
            childPageId = saveNode(newInternal);
        }
        
        return childPageId;
    }
    
    /**
     * 리프 분할을 동반한 삽입.
     * 
     * @param path 검색 경로
     * @param leaf 분할할 리프
     * @param localIndex 삽입 위치
     * @param elementRecordId 삽입할 요소
     * @return 새 루트 페이지 ID
     */
    private long insertWithSplit(List<OSTPathFrame> path, OSTLeaf leaf, 
                                  int localIndex, long elementRecordId) {
        // 모든 원소 수집 (새 원소 포함)
        List<Long> allElements = new ArrayList<>();
        for (int i = 0; i < leaf.subtreeCount(); i++) {
            if (i == localIndex) {
                allElements.add(elementRecordId);
            }
            allElements.add(leaf.getElementRecordId(i));
        }
        if (localIndex == leaf.subtreeCount()) {
            allElements.add(elementRecordId);
        }
        
        // 분할 지점
        int splitPoint = allElements.size() / 2;
        
        // 왼쪽 리프
        OSTLeaf leftLeaf = new OSTLeaf();
        for (int i = 0; i < splitPoint; i++) {
            leftLeaf.addElement(allElements.get(i));
        }
        
        // 오른쪽 리프
        OSTLeaf rightLeaf = new OSTLeaf();
        for (int i = splitPoint; i < allElements.size(); i++) {
            rightLeaf.addElement(allElements.get(i));
        }
        
        long leftPageId = saveNode(leftLeaf);
        long rightPageId = saveNode(rightLeaf);
        
        // 부모에 삽입
        return insertIntoParent(path, leftPageId, leftLeaf.subtreeCount(), 
                               rightPageId, rightLeaf.subtreeCount());
    }
    
    /**
     * 부모 노드에 자식을 삽입합니다.
     * 
     * @param path 검색 경로
     * @param leftChildId 왼쪽 자식 페이지 ID
     * @param leftCount 왼쪽 자식 요소 수
     * @param rightChildId 오른쪽 자식 페이지 ID
     * @param rightCount 오른쪽 자식 요소 수
     * @return 새 루트 페이지 ID
     */
    private long insertIntoParent(List<OSTPathFrame> path, long leftChildId, int leftCount,
                                   long rightChildId, int rightCount) {
        if (path.isEmpty()) {
            // 새 루트 생성
            List<Long> children = new ArrayList<>();
            children.add(leftChildId);
            children.add(rightChildId);
            
            List<Integer> counts = new ArrayList<>();
            counts.add(leftCount);
            counts.add(rightCount);
            
            OSTInternal newRoot = new OSTInternal(1, children, counts);
            return saveNode(newRoot);
        }
        
        OSTPathFrame parentFrame = path.get(path.size() - 1);
        OSTInternal parent = (OSTInternal) parentFrame.node;
        int insertPos = parentFrame.childIndex;
        
        // 부모 수정: 기존 자식을 left, right로 대체
        // insertPos 위치의 자식이 분할되었으므로
        // insertPos에는 left, insertPos+1에는 right를 넣어야 함
        List<Long> newChildren = new ArrayList<>();
        List<Integer> newCounts = new ArrayList<>();
        
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (i < insertPos) {
                // insertPos 이전은 그대로
                newChildren.add(parent.getChildPageId(i));
                newCounts.add(parent.getSubtreeCount(i));
            } else if (i == insertPos) {
                // insertPos 위치: left와 right를 삽입
                newChildren.add(leftChildId);
                newCounts.add(leftCount);
                newChildren.add(rightChildId);
                newCounts.add(rightCount);
            } else {
                // insertPos 이후는 그대로
                newChildren.add(parent.getChildPageId(i));
                newCounts.add(parent.getSubtreeCount(i));
            }
        }
        
        // 부모에 공간 있는지 확인 (최대 128 자식)
        if (newChildren.size() <= 128) {
            OSTInternal newParent = new OSTInternal(parent.getLevel(), newChildren, newCounts);
            long newParentPageId = saveNode(newParent);
            
            // 상위 경로로 전파 (count +1 증가)
            path.remove(path.size() - 1);
            return propagateOstCow(path, newParentPageId, +1);
        } else {
            // 부모도 분할 필요
            return splitInternalNode(path, parent.getLevel(), newChildren, newCounts);
        }
    }
    
    /**
     * 내부 노드 분할.
     * 
     * @param path 검색 경로
     * @param level 트리 레벨
     * @param children 모든 자식 페이지 ID
     * @param counts 모든 자식의 subtreeCount
     * @return 새 루트 페이지 ID
     */
    private long splitInternalNode(List<OSTPathFrame> path, int level, 
                                     List<Long> children, List<Integer> counts) {
        int splitPoint = children.size() / 2;
        
        // 왼쪽 내부 노드
        List<Long> leftChildren = new ArrayList<>(children.subList(0, splitPoint));
        List<Integer> leftCounts = new ArrayList<>(counts.subList(0, splitPoint));
        OSTInternal leftInternal = new OSTInternal(level, leftChildren, leftCounts);
        
        // 오른쪽 내부 노드
        List<Long> rightChildren = new ArrayList<>(children.subList(splitPoint, children.size()));
        List<Integer> rightCounts = new ArrayList<>(counts.subList(splitPoint, counts.size()));
        OSTInternal rightInternal = new OSTInternal(level, rightChildren, rightCounts);
        
        long leftPageId = saveNode(leftInternal);
        long rightPageId = saveNode(rightInternal);
        
        // path에서 마지막 제거 후 상위 부모에 삽입
        path.remove(path.size() - 1);
        return insertIntoParent(path, leftPageId, leftInternal.subtreeCount(), 
                               rightPageId, rightInternal.subtreeCount());
    }
}
