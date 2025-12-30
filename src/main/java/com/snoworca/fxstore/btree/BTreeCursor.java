package com.snoworca.fxstore.btree;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * B+Tree 순회를 위한 Cursor
 *
 * <p>리프 노드를 따라 정렬 순서대로 키-값 쌍을 순회합니다.
 * <p>COW(Copy-on-Write) 일관성을 위해 tree descent 방식으로 다음 리프를 찾습니다.
 *
 * <p>테스트 시나리오: docs/plan/TEST-SCENARIOS-PHASE3.md 그룹 9
 *
 * <p>SOLID 준수:
 * - SRP: BTree 순회만 담당
 * - OCP: startKey/endKey로 범위 지정 가능
 * - DIP: BTree에 의존하지만 인터페이스로 추상화 가능
 */
public class BTreeCursor implements Iterator<BTree.Entry> {

    private final BTree btree;
    private final Comparator<byte[]> comparator;
    private final byte[] endKey;
    private final boolean endInclusive;

    /** 스냅샷 기반 읽기를 위한 루트 페이지 ID 오버라이드 */
    private final long rootPageIdOverride;

    private BTreeLeaf currentLeaf;
    private int currentIndex;
    private boolean exhausted;

    /**
     * 트리 순회를 위한 스택 (각 레벨의 internal node와 현재 child index 저장)
     */
    private final Deque<StackEntry> traversalStack;
    
    /**
     * 생성자
     *
     * @param btree B+Tree
     * @param comparator 키 비교자
     * @param startKey 시작 키 (null이면 처음부터)
     * @param endKey 종료 키 (null이면 끝까지)
     * @param startInclusive startKey 포함 여부
     * @param endInclusive endKey 포함 여부
     */
    public BTreeCursor(BTree btree, Comparator<byte[]> comparator,
                       byte[] startKey, byte[] endKey,
                       boolean startInclusive, boolean endInclusive) {
        this(btree, comparator, startKey, endKey, startInclusive, endInclusive, -1);
    }

    /**
     * 스냅샷 기반 읽기용 생성자
     *
     * @param btree B+Tree
     * @param comparator 키 비교자
     * @param startKey 시작 키 (null이면 처음부터)
     * @param endKey 종료 키 (null이면 끝까지)
     * @param startInclusive startKey 포함 여부
     * @param endInclusive endKey 포함 여부
     * @param rootPageId 사용할 루트 페이지 ID (-1이면 btree의 현재 rootPageId 사용)
     */
    public BTreeCursor(BTree btree, Comparator<byte[]> comparator,
                       byte[] startKey, byte[] endKey,
                       boolean startInclusive, boolean endInclusive,
                       long rootPageId) {
        this.btree = btree;
        this.comparator = comparator;
        this.endKey = endKey;
        this.endInclusive = endInclusive;
        this.rootPageIdOverride = rootPageId;
        this.exhausted = false;
        this.traversalStack = new ArrayDeque<>();

        // 실제 사용할 rootPageId 결정
        long effectiveRootPageId = getEffectiveRootPageId();

        // 시작 위치 찾기
        if (effectiveRootPageId == 0) {
            // 빈 트리
            this.exhausted = true;
            return;
        }

        if (startKey == null) {
            // 처음부터 시작 - 스택을 채우면서 첫 리프로 이동
            this.currentLeaf = findFirstLeafWithStack(effectiveRootPageId);
            this.currentIndex = 0;
        } else {
            // startKey 위치 찾기 - 스택을 채우면서 해당 리프로 이동
            this.currentLeaf = findLeafContainingWithStack(effectiveRootPageId, startKey);
            this.currentIndex = findStartPosition(currentLeaf, startKey, startInclusive);
        }

        // 초기 위치가 이미 범위를 벗어났는지 확인
        advanceIfNeeded();
    }

    /**
     * 실제 사용할 루트 페이지 ID 반환
     *
     * @return rootPageIdOverride가 -1이면 btree.getRootPageId(), 아니면 override 값
     */
    private long getEffectiveRootPageId() {
        return rootPageIdOverride >= 0 ? rootPageIdOverride : btree.getRootPageId();
    }
    
    @Override
    public boolean hasNext() {
        return !exhausted && currentLeaf != null && currentIndex < currentLeaf.size();
    }
    
    @Override
    public BTree.Entry next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements");
        }
        
        // 현재 엔트리 반환
        byte[] key = currentLeaf.getKey(currentIndex);
        long valueRecordId = currentLeaf.getValueRecordId(currentIndex);
        BTree.Entry entry = new BTree.Entry(key, valueRecordId);
        
        // 다음 위치로 이동
        currentIndex++;
        advanceIfNeeded();
        
        return entry;
    }
    
    /**
     * 다음 엔트리를 소비하지 않고 미리보기
     * 
     * @return 다음 엔트리, 없으면 null
     */
    public BTree.Entry peek() {
        if (!hasNext()) {
            return null;
        }
        
        byte[] key = currentLeaf.getKey(currentIndex);
        long valueRecordId = currentLeaf.getValueRecordId(currentIndex);
        return new BTree.Entry(key, valueRecordId);
    }
    
    /**
     * 다음 리프로 이동 (필요시)
     * <p>COW 일관성을 위해 tree descent 방식으로 다음 리프를 찾습니다.
     */
    private void advanceIfNeeded() {
        if (exhausted) {
            return;
        }

        // 현재 리프의 끝에 도달했으면 다음 리프로
        while (currentLeaf != null && currentIndex >= currentLeaf.size()) {
            BTreeLeaf nextLeaf = findNextLeafViaStack();
            if (nextLeaf == null) {
                // 마지막 리프
                exhausted = true;
                currentLeaf = null;
                return;
            }

            currentLeaf = nextLeaf;
            currentIndex = 0;
        }

        // endKey 체크
        if (currentLeaf != null && endKey != null && currentIndex < currentLeaf.size()) {
            byte[] currentKey = currentLeaf.getKey(currentIndex);
            int cmp = comparator.compare(currentKey, endKey);

            if (cmp > 0 || (cmp == 0 && !endInclusive)) {
                // 범위 초과
                exhausted = true;
                currentLeaf = null;
            }
        }
    }

    /**
     * 스택을 사용하여 다음 리프 찾기 (tree descent 방식)
     *
     * @return 다음 리프, 없으면 null
     */
    private BTreeLeaf findNextLeafViaStack() {
        // 스택에서 다음 형제가 있는 레벨을 찾기
        while (!traversalStack.isEmpty()) {
            StackEntry entry = traversalStack.peek();
            int nextChildIndex = entry.childIndex + 1;

            if (nextChildIndex < entry.internal.getChildCount()) {
                // 다음 형제가 있음
                entry.childIndex = nextChildIndex;
                long nextChildPageId = entry.internal.getChildPageId(nextChildIndex);

                // 해당 서브트리의 가장 왼쪽 리프로 이동
                return descendToLeftmostLeaf(nextChildPageId);
            } else {
                // 이 레벨에서 더 이상 형제가 없음, 상위 레벨로
                traversalStack.pop();
            }
        }

        // 스택이 비었음 - 순회 완료
        return null;
    }

    /**
     * 주어진 노드에서 가장 왼쪽 리프까지 하강
     *
     * @param nodePageId 시작 노드 페이지 ID
     * @return 가장 왼쪽 리프
     */
    private BTreeLeaf descendToLeftmostLeaf(long nodePageId) {
        BTreeNode node = btree.readNode(nodePageId);

        while (!node.isLeaf()) {
            BTreeInternal internal = (BTreeInternal) node;
            // 스택에 현재 internal 노드 추가 (child index 0에서 시작)
            traversalStack.push(new StackEntry(internal, 0));
            long firstChildPageId = internal.getChildPageId(0);
            node = btree.readNode(firstChildPageId);
        }

        return (BTreeLeaf) node;
    }

    /**
     * 첫 번째 리프 찾기 (스택 채우면서)
     */
    private BTreeLeaf findFirstLeafWithStack(long nodePageId) {
        return descendToLeftmostLeaf(nodePageId);
    }

    /**
     * 키를 포함하는 리프 찾기 (스택 채우면서)
     */
    private BTreeLeaf findLeafContainingWithStack(long nodePageId, byte[] key) {
        BTreeNode node = btree.readNode(nodePageId);

        while (!node.isLeaf()) {
            BTreeInternal internal = (BTreeInternal) node;
            int childIndex = internal.findChildIndex(key, comparator);
            // 스택에 현재 internal 노드와 child index 추가
            traversalStack.push(new StackEntry(internal, childIndex));
            long childPageId = internal.getChildPageId(childIndex);
            node = btree.readNode(childPageId);
        }

        return (BTreeLeaf) node;
    }

    /**
     * 시작 위치 찾기
     */
    private int findStartPosition(BTreeLeaf leaf, byte[] startKey, boolean inclusive) {
        int index = leaf.find(startKey, comparator);

        if (index >= 0) {
            // 정확히 찾음
            return inclusive ? index : index + 1;
        } else {
            // 못찾음: insertion point 반환
            return -(index + 1);
        }
    }

    /**
     * 스택 엔트리 (internal 노드와 현재 child index)
     */
    private static class StackEntry {
        final BTreeInternal internal;
        int childIndex;

        StackEntry(BTreeInternal internal, int childIndex) {
            this.internal = internal;
            this.childIndex = childIndex;
        }
    }
}
