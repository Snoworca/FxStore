package com.snoworca.fxstore.btree;

import com.snoworca.fxstore.api.FxErrorCode;
import com.snoworca.fxstore.api.FxException;
import com.snoworca.fxstore.storage.Allocator;
import com.snoworca.fxstore.storage.Storage;

import java.util.Comparator;

/**
 * B+Tree 구현
 * 
 * <p>COW(Copy-on-Write) 기반 B+Tree로, 모든 변경은 새 페이지를 할당하고
 * 기존 페이지는 수정하지 않습니다.
 * 
 * <p>SOLID 준수:
 * - SRP: B+Tree 연산만 담당 (저장소는 주입)
 * - OCP: Comparator 주입으로 다양한 키 타입 지원
 * - DIP: Storage 인터페이스에 의존 (구현 무관)
 * 
 * @see BTreeNode
 * @see BTreeLeaf
 * @see BTreeInternal
 */
public class BTree {
    
    /**
     * 루트 페이지 ID (0이면 빈 트리)
     */
    private long rootPageId;
    
    /**
     * 저장소
     */
    private final Storage storage;
    
    /**
     * 페이지 크기
     */
    private final int pageSize;
    
    /**
     * 키 비교자
     */
    private final Comparator<byte[]> keyComparator;

    /**
     * 외부 할당자 (선택적)
     */
    private final Allocator allocator;

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
     * 생성자
     *
     * @param storage 저장소
     * @param pageSize 페이지 크기
     * @param keyComparator 키 비교자
     */
    public BTree(Storage storage, int pageSize, Comparator<byte[]> keyComparator) {
        this(storage, pageSize, keyComparator, 0, null);
    }

    /**
     * 생성자 (기존 루트 지정)
     *
     * @param storage 저장소
     * @param pageSize 페이지 크기
     * @param keyComparator 키 비교자
     * @param rootPageId 루트 페이지 ID
     */
    public BTree(Storage storage, int pageSize, Comparator<byte[]> keyComparator, long rootPageId) {
        this(storage, pageSize, keyComparator, rootPageId, null);
    }

    /**
     * 생성자 (외부 할당자 지정)
     *
     * @param storage 저장소
     * @param pageSize 페이지 크기
     * @param keyComparator 키 비교자
     * @param rootPageId 루트 페이지 ID
     * @param allocator 외부 할당자 (null이면 내부 할당 사용)
     */
    public BTree(Storage storage, int pageSize, Comparator<byte[]> keyComparator, long rootPageId, Allocator allocator) {
        this.storage = storage;
        this.pageSize = pageSize;
        this.keyComparator = keyComparator;
        this.rootPageId = rootPageId;
        this.allocator = allocator;
    }
    
    /**
     * 루트 페이지 ID 반환
     * 
     * @return 루트 페이지 ID (0이면 빈 트리)
     */
    public long getRootPageId() {
        return rootPageId;
    }
    
    /**
     * 루트 페이지 ID 설정 (COW 후 갱신용)
     *
     * @param rootPageId 새 루트 페이지 ID
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
     * 키 검색
     * 
     * <p>루트에서 시작해서 리프까지 순회하며 키를 찾습니다.
     * 
     * @param key 찾을 키
     * @return 값 레코드 ID, 못찾으면 null
     * @throws FxException IO 오류 시
     */
    public Long find(byte[] key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        
        if (rootPageId == 0) {
            return null; // 빈 트리
        }
        
        try {
            BTreeNode node = readNode(rootPageId);
            
            // 리프까지 순회
            while (!node.isLeaf()) {
                BTreeInternal internal = (BTreeInternal) node;
                int childIndex = internal.findChildIndex(key, keyComparator);
                long childPageId = internal.getChildPageId(childIndex);
                node = readNode(childPageId);
            }
            
            // 리프에서 검색
            BTreeLeaf leaf = (BTreeLeaf) node;
            int index = leaf.find(key, keyComparator);
            
            if (index >= 0) {
                return leaf.getValueRecordId(index);
            } else {
                return null; // 못찾음
            }
            
        } catch (FxException e) {
            throw e;
        } catch (Exception e) {
            throw new FxException(FxErrorCode.IO, "Failed to read B+Tree node", e);
        }
    }
    
    /**
     * 페이지 ID로 노드 읽기
     * 
     * @param pageId 페이지 ID
     * @return 노드
     */
    public BTreeNode readNode(long pageId) {
        long offset = pageId * pageSize;
        byte[] page = new byte[pageSize];
        storage.read(offset, page, 0, pageSize);
        
        // DESIGN DECISION: PageHeader 대신 level 필드로 노드 타입 판단
        //
        // 현재 구현:
        // - level=0: BTreeLeaf (리프 노드)
        // - level>0: BTreeInternal (내부 노드)
        //
        // 이유:
        // 1. level 필드가 offset 32에 있어 효율적으로 타입 판단 가능
        // 2. PageHeader(fxstore.page) 도입 시 기존 파일과 호환성 문제 발생
        // 3. 현재 구현이 모든 테스트를 통과하며 안정적으로 동작
        //
        // 향후 개선 시:
        // - compactTo() 마이그레이션에서 PageKind 기반 판단으로 전환 가능
        //
        // 관련 문서: docs/plan/TODO-RESOLUTION-PLAN.md

        int levelOffset = 32; // PageHeader 영역 이후
        int level = (page[levelOffset] & 0xFF) | ((page[levelOffset + 1] & 0xFF) << 8);

        if (level == 0) {
            // 리프 노드 (level=0)
            return BTreeLeaf.fromPage(page, pageSize, pageId);
        } else {
            // 내부 노드 (level>0)
            return BTreeInternal.fromPage(page, pageSize, pageId);
        }
    }
    
    /**
     * Cursor 생성 (전체 순회)
     * 
     * @return Cursor
     */
    public BTreeCursor cursor() {
        return new BTreeCursor(this, keyComparator, null, null, true, true);
    }
    
    /**
     * Cursor 생성 (범위 순회)
     * 
     * @param startKey 시작 키 (null이면 처음부터)
     * @param endKey 종료 키 (null이면 끝까지)
     * @param startInclusive startKey 포함 여부
     * @param endInclusive endKey 포함 여부
     * @return Cursor
     */
    public BTreeCursor cursor(byte[] startKey, byte[] endKey, 
                              boolean startInclusive, boolean endInclusive) {
        return new BTreeCursor(this, keyComparator, startKey, endKey, 
                              startInclusive, endInclusive);
    }
    
    /**
     * 비어있는지 확인
     * 
     * @return 빈 트리이면 true
     */
    public boolean isEmpty() {
        return rootPageId == 0;
    }
    
    /**
     * 키-값 삽입
     * 
     * <p>키가 이미 존재하면 값을 교체합니다 (Replace 정책).
     * <p>COW(Copy-on-Write) 기반으로, 기존 페이지는 수정하지 않고 새 페이지를 할당합니다.
     * 
     * @param key 삽입할 키 (null 불가)
     * @param valueRecordId 값 레코드 ID
     * @throws NullPointerException key가 null인 경우
     * @throws FxException IO 오류 또는 페이지 오버플로우 시
     */
    public void insert(byte[] key, long valueRecordId) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        
        try {
            if (rootPageId == 0) {
                // 빈 트리: 첫 리프 생성
                BTreeLeaf newLeaf = new BTreeLeaf(pageSize);
                newLeaf.insert(0, key, valueRecordId);
                
                long newPageId = allocatePageId();
                writeNode(newLeaf, newPageId);
                this.rootPageId = newPageId;
                
            } else {
                // 기존 트리: COW 삽입
                InsertResult result = insertRecursive(rootPageId, key, valueRecordId, true);
                
                if (result.split) {
                    // 루트 분할 발생: 새 루트 생성
                    BTreeInternal newRoot = new BTreeInternal(pageSize, 1);
                    newRoot.insertChild(0, result.leftPageId);
                    newRoot.insertKey(0, result.splitKey);
                    newRoot.insertChild(1, result.rightPageId);
                    
                    long newRootPageId = allocatePageId();
                    writeNode(newRoot, newRootPageId);
                    this.rootPageId = newRootPageId;
                    
                } else {
                    // 루트 갱신 (COW)
                    this.rootPageId = result.leftPageId;
                }
            }
            
        } catch (FxException e) {
            throw e;
        } catch (Exception e) {
            throw new FxException(FxErrorCode.IO, "Failed to insert into B+Tree", e);
        }
    }
    
    /**
     * 재귀 삽입 (COW)
     * 
     * @param pageId 현재 노드 페이지 ID
     * @param key 삽입할 키
     * @param valueRecordId 값 레코드 ID
     * @param isRootPath 루트 경로 여부 (분할 처리용)
     * @return 삽입 결과 (분할 여부, 페이지 ID 등)
     */
    private InsertResult insertRecursive(long pageId, byte[] key, long valueRecordId, boolean isRootPath) {
        BTreeNode node = readNode(pageId);
        
        if (node.isLeaf()) {
            BTreeLeaf leaf = (BTreeLeaf) node;
            
            // 삽입 위치 찾기
            int index = leaf.find(key, keyComparator);
            
            // 중복 키 처리
            if (index >= 0) {
                // Replace: 새 리프 복사 후 값 교체
                BTreeLeaf newLeaf = leaf.copy();
                newLeaf.setValueRecordId(index, valueRecordId);
                
                long newPageId = allocatePageId();
                writeNode(newLeaf, newPageId);
                
                return new InsertResult(false, newPageId, 0L, null);
            }
            
            // 삽입 위치 계산 (find가 음수 반환)
            int insertPos = -(index + 1);
            
            // 공간 확인
            if (!leaf.isFull()) {
                // 공간 있음: 단순 삽입
                BTreeLeaf newLeaf = leaf.copy();
                newLeaf.insert(insertPos, key, valueRecordId);

                long newPageId = allocatePageId();
                writeNode(newLeaf, newPageId);

                return new InsertResult(false, newPageId, 0L, null);

            } else {
                // 공간 없음: 분할 필요
                // 먼저 키를 삽입한 후 분할
                BTreeLeaf tempLeaf = leaf.copy();
                tempLeaf.insert(insertPos, key, valueRecordId);

                // 분할 수행
                BTreeLeaf.SplitResult splitResult = tempLeaf.split();

                // 왼쪽 리프 저장
                long leftPageId = allocatePageId();
                writeNode(splitResult.leftLeaf, leftPageId);

                // 오른쪽 리프 저장
                long rightPageId = allocatePageId();
                writeNode(splitResult.rightLeaf, rightPageId);

                // 왼쪽 리프의 nextLeaf 연결
                splitResult.leftLeaf.setNextLeafPageId(rightPageId);
                writeNode(splitResult.leftLeaf, leftPageId);

                return new InsertResult(true, leftPageId, rightPageId, splitResult.splitKey);
            }
            
        } else {
            // Internal 노드
            BTreeInternal internal = (BTreeInternal) node;
            
            // 자식 찾기
            int childIndex = internal.findChildIndex(key, keyComparator);
            long childPageId = internal.getChildPageId(childIndex);
            
            // 재귀 삽입
            InsertResult childResult = insertRecursive(childPageId, key, valueRecordId, false);
            
            if (!childResult.split) {
                // 자식에서 분할 없음: 자식 포인터만 갱신 (COW)
                BTreeInternal newInternal = internal.copy();
                newInternal.setChildPageId(childIndex, childResult.leftPageId);

                long newPageId = allocatePageId();
                writeNode(newInternal, newPageId);

                return new InsertResult(false, newPageId, 0L, null);

            } else {
                // 자식에서 분할 발생: 분리자 키 삽입
                if (!internal.isFull()) {
                    // Internal 노드에 공간 있음: 단순 삽입
                    BTreeInternal newInternal = internal.copy();
                    newInternal.setChildPageId(childIndex, childResult.leftPageId);
                    newInternal.insertChild(childIndex, childResult.splitKey, childResult.rightPageId);

                    long newPageId = allocatePageId();
                    writeNode(newInternal, newPageId);

                    return new InsertResult(false, newPageId, 0L, null);

                } else {
                    // Internal 노드도 분할 필요
                    BTreeInternal tempInternal = internal.copy();
                    tempInternal.setChildPageId(childIndex, childResult.leftPageId);
                    tempInternal.insertChild(childIndex, childResult.splitKey, childResult.rightPageId);

                    // 분할 수행
                    BTreeInternal.SplitResult internalSplit = tempInternal.split();

                    // 왼쪽 노드 저장
                    long leftPageId = allocatePageId();
                    writeNode(internalSplit.leftNode, leftPageId);

                    // 오른쪽 노드 저장
                    long rightPageId = allocatePageId();
                    writeNode(internalSplit.rightNode, rightPageId);

                    return new InsertResult(true, leftPageId, rightPageId, internalSplit.splitKey);
                }
            }
        }
    }
    
    /**
     * 새 페이지 ID 할당
     *
     * <p>페이지 ID는 파일 오프셋 / pageSize로 계산됩니다.
     * <p>ID 0은 "null/빈 트리"를 의미하므로, 유효한 페이지 ID는 1부터 시작합니다.
     *
     * @return 새 페이지 ID (1부터 시작)
     */
    private long allocatePageId() {
        if (allocator != null) {
            // 레거시 API 사용 (v0.9 전환 기간 동안 유지)
            // allocator.allocatePage()는 offset을 반환하므로 pageId로 변환
            long offset = allocator.allocatePage();
            return offset / pageSize;
        }

        // 내부 할당 (기존 방식)
        long currentSize = storage.size();

        // extend 먼저 호출하여 새 페이지 공간 확보
        storage.extend(currentSize + pageSize);

        // 새 페이지 ID = 확장 후 크기 / pageSize
        // 이렇게 하면 첫 페이지 ID = 4096 / 4096 = 1이 됨
        long newPageId = (currentSize + pageSize) / pageSize;

        return newPageId;
    }
    
    /**
     * 노드를 페이지에 쓰기
     * 
     * @param node 쓸 노드
     * @param pageId 페이지 ID
     */
    private void writeNode(BTreeNode node, long pageId) {
        byte[] page = node.toPage();
        long offset = pageId * pageSize;
        storage.write(offset, page, 0, pageSize);
    }
    
    /**
     * 키 삭제 (COW)
     * 
     * <p>docs/02.architecture.md 4.5절의 "병합 없는 단순 Delete" 구현:
     * - 리프에서 엔트리 제거
     * - COW로 경로상 모든 노드 복사
     * - 병합(merge) 미구현 (컴팩션으로 공간 회수)
     * 
     * @param key 삭제할 키
     * @return 새 루트 페이지 ID (삭제 후 빈 트리면 0)
     * @throws FxException IO 오류 시
     */
    public long delete(byte[] key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        // 빈 트리에서 삭제 시도: no-op
        if (rootPageId == 0) {
            return 0;
        }
        
        // 루트부터 삭제 수행 (originalRoot = rootPageId)
        DeleteResult result = deleteInternal(rootPageId, key, rootPageId);

        if (result.found) {
            // 삭제 성공: 새 루트로 갱신
            rootPageId = result.newRootPageId;
        }
        // 키가 없으면 변경 없음
        
        return rootPageId;
    }
    
    /**
     * 내부 delete 재귀 구현
     *
     * @param nodePageId 현재 노드 페이지 ID
     * @param key 삭제할 키
     * @param originalRoot 원래 트리의 루트 페이지 ID (빈 트리 체크용)
     * @return 삭제 결과
     */
    private DeleteResult deleteInternal(long nodePageId, byte[] key, long originalRoot) {
        BTreeNode node = readNode(nodePageId);

        if (node.isLeaf()) {
            // 리프 노드: 직접 삭제
            BTreeLeaf leaf = (BTreeLeaf) node;
            int index = leaf.find(key, keyComparator);

            if (index < 0) {
                // 키가 없음: 변경 없음
                return new DeleteResult(false, nodePageId);
            }

            // COW: 리프 복사
            BTreeLeaf newLeaf = leaf.copy();
            newLeaf.deleteEntry(index);

            // 리프가 비어있으면?
            if (newLeaf.size() == 0) {
                // 빈 리프 → 루트였다면 빈 트리, 아니면 유지
                if (nodePageId == originalRoot) {
                    return new DeleteResult(true, 0); // 빈 트리
                }
            }

            // 새 리프 저장
            long newLeafPageId = allocatePageId();
            writeNode(newLeaf, newLeafPageId);

            return new DeleteResult(true, newLeafPageId);

        } else {
            // Internal 노드: 자식으로 재귀
            BTreeInternal internal = (BTreeInternal) node;
            int childIndex = internal.findChildIndex(key, keyComparator);
            long childPageId = internal.getChildPageId(childIndex);

            // 자식에서 삭제
            DeleteResult childResult = deleteInternal(childPageId, key, originalRoot);

            if (!childResult.found) {
                // 키가 없으면 변경 없음
                return new DeleteResult(false, nodePageId);
            }

            // 자식이 빈 트리가 되었으면?
            if (childResult.newRootPageId == 0) {
                // 자식 제거 필요
                // 단순 구현: 자식을 그대로 유지 (merge 없음)
                // 빈 리프도 구조상 유지
            }

            // COW: Internal 복사하고 자식 포인터 갱신
            BTreeInternal newInternal = internal.copy();
            newInternal.setChildPageId(childIndex, childResult.newRootPageId);

            // 새 Internal 저장
            long newInternalPageId = allocatePageId();
            writeNode(newInternal, newInternalPageId);

            return new DeleteResult(true, newInternalPageId);
        }
    }

    // ==================== Stateless API (Phase 8 동시성 지원) ====================

    /**
     * 지정된 root에서 검색 (Stateless, 읽기 전용)
     *
     * <p>Phase 8 동시성 지원: COW 페이지는 불변이므로 동시 읽기가 안전합니다.</p>
     *
     * @param rootPageId 검색 시작 root (스냅샷에서 획득)
     * @param key 검색 키
     * @return 값 레코드 ID, 없으면 null
     */
    public Long findWithRoot(long rootPageId, byte[] key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        if (rootPageId == 0) {
            return null; // 빈 트리
        }

        try {
            BTreeNode node = readNode(rootPageId);

            // 리프까지 순회
            while (!node.isLeaf()) {
                BTreeInternal internal = (BTreeInternal) node;
                int childIndex = internal.findChildIndex(key, keyComparator);
                long childPageId = internal.getChildPageId(childIndex);
                node = readNode(childPageId);
            }

            // 리프에서 검색
            BTreeLeaf leaf = (BTreeLeaf) node;
            int index = leaf.find(key, keyComparator);

            if (index >= 0) {
                return leaf.getValueRecordId(index);
            } else {
                return null; // 못찾음
            }

        } catch (FxException e) {
            throw e;
        } catch (Exception e) {
            throw new FxException(FxErrorCode.IO, "Failed to read B+Tree node", e);
        }
    }

    /**
     * 지정된 root에서 커서 생성 (Stateless, 읽기 전용)
     *
     * <p>Phase 8 동시성 지원: 스냅샷의 rootPageId로 커서를 생성하여
     * 일관된 읽기 뷰를 제공합니다.</p>
     *
     * @param rootPageId 사용할 루트 페이지 ID (스냅샷에서 획득)
     * @return BTreeCursor (정방향)
     */
    public BTreeCursor cursorWithRoot(long rootPageId) {
        return new BTreeCursor(this, keyComparator, null, null, true, true, rootPageId);
    }

    /**
     * 지정된 root에서 범위 커서 생성 (Stateless, 읽기 전용)
     *
     * @param rootPageId 사용할 루트 페이지 ID
     * @param startKey 시작 키 (null이면 처음부터)
     * @param endKey 종료 키 (null이면 끝까지)
     * @param startInclusive startKey 포함 여부
     * @param endInclusive endKey 포함 여부
     * @return BTreeCursor
     */
    public BTreeCursor cursorWithRoot(long rootPageId, byte[] startKey, byte[] endKey,
                                       boolean startInclusive, boolean endInclusive) {
        return new BTreeCursor(this, keyComparator, startKey, endKey,
                               startInclusive, endInclusive, rootPageId);
    }

    /**
     * 지정된 root에서 역방향 커서 생성 (Stateless, 읽기 전용)
     *
     * <p>마지막 엔트리부터 역순으로 순회합니다.
     * 현재 구현은 모든 엔트리를 수집 후 역순 반환합니다.</p>
     *
     * @param rootPageId 사용할 루트 페이지 ID
     * @return 역방향 Iterator
     */
    public java.util.Iterator<Entry> descendingCursorWithRoot(long rootPageId) {
        // 모든 엔트리 수집
        java.util.List<Entry> entries = new java.util.ArrayList<>();
        BTreeCursor cursor = cursorWithRoot(rootPageId);
        while (cursor.hasNext()) {
            entries.add(cursor.next());
        }
        // 역순 반환
        java.util.Collections.reverse(entries);
        return entries.iterator();
    }

    /**
     * 지정된 root에서 마지막 (최대) 엔트리 조회 (Stateless, O(log n))
     *
     * <p>가장 오른쪽 리프의 마지막 엔트리를 반환합니다.
     * 바이트 순서상 가장 큰 키를 가진 엔트리입니다.</p>
     *
     * @param rootPageId 사용할 루트 페이지 ID
     * @return 마지막 Entry 또는 null (빈 트리)
     * @since 0.7
     */
    public Entry lastEntryWithRoot(long rootPageId) {
        if (rootPageId == 0) {
            return null;
        }

        try {
            BTreeNode node = readNode(rootPageId);

            // 가장 오른쪽 리프까지 순회
            while (!node.isLeaf()) {
                BTreeInternal internal = (BTreeInternal) node;
                // 마지막 자식 (가장 오른쪽)
                int lastChildIndex = internal.getKeyCount();
                long lastChildPageId = internal.getChildPageId(lastChildIndex);
                node = readNode(lastChildPageId);
            }

            // 리프의 마지막 엔트리
            BTreeLeaf leaf = (BTreeLeaf) node;
            int entryCount = leaf.size();

            if (entryCount == 0) {
                return null;
            }

            byte[] lastKey = leaf.getKey(entryCount - 1);
            long lastValueRecordId = leaf.getValueRecordId(entryCount - 1);

            return new Entry(lastKey, lastValueRecordId);

        } catch (FxException e) {
            throw e;
        } catch (Exception e) {
            throw new FxException(FxErrorCode.IO, "Failed to read B+Tree node", e);
        }
    }

    /**
     * 지정된 root에서 첫 번째 (최소) 엔트리 조회 (Stateless, O(log n))
     *
     * <p>가장 왼쪽 리프의 첫 번째 엔트리를 반환합니다.
     * 바이트 순서상 가장 작은 키를 가진 엔트리입니다.</p>
     *
     * @param rootPageId 사용할 루트 페이지 ID
     * @return 첫 번째 Entry 또는 null (빈 트리)
     * @since 0.7
     */
    public Entry firstEntryWithRoot(long rootPageId) {
        if (rootPageId == 0) {
            return null;
        }

        try {
            BTreeNode node = readNode(rootPageId);

            // 가장 왼쪽 리프까지 순회
            while (!node.isLeaf()) {
                BTreeInternal internal = (BTreeInternal) node;
                // 첫 번째 자식 (가장 왼쪽)
                long firstChildPageId = internal.getChildPageId(0);
                node = readNode(firstChildPageId);
            }

            // 리프의 첫 번째 엔트리
            BTreeLeaf leaf = (BTreeLeaf) node;
            int entryCount = leaf.size();

            if (entryCount == 0) {
                return null;
            }

            byte[] firstKey = leaf.getKey(0);
            long firstValueRecordId = leaf.getValueRecordId(0);

            return new Entry(firstKey, firstValueRecordId);

        } catch (FxException e) {
            throw e;
        } catch (Exception e) {
            throw new FxException(FxErrorCode.IO, "Failed to read B+Tree node", e);
        }
    }

    /**
     * 지정된 root에서 삽입 (Stateless, Write Lock 하에서만 호출)
     *
     * <p>Phase 8 동시성 지원: COW 방식으로 새 페이지를 생성하고,
     * 기존 페이지는 수정하지 않습니다.</p>
     *
     * @param currentRoot 현재 root (스냅샷에서 획득, 0이면 빈 트리)
     * @param key 삽입 키
     * @param valueRecordId 값 레코드 ID
     * @return 삽입 결과 (새 root pageId 포함)
     */
    public StatelessInsertResult insertWithRoot(long currentRoot, byte[] key, long valueRecordId) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        try {
            if (currentRoot == 0) {
                // 빈 트리: 첫 리프 생성
                BTreeLeaf newLeaf = new BTreeLeaf(pageSize);
                newLeaf.insert(0, key, valueRecordId);

                long newPageId = allocatePageId();
                writeNode(newLeaf, newPageId);

                return new StatelessInsertResult(newPageId, false);

            } else {
                // 기존 트리: COW 삽입
                InsertResult result = insertRecursive(currentRoot, key, valueRecordId, true);

                if (result.split) {
                    // 루트 분할 발생: 새 루트 생성
                    BTreeInternal newRoot = new BTreeInternal(pageSize, 1);
                    newRoot.insertChild(0, result.leftPageId);
                    newRoot.insertKey(0, result.splitKey);
                    newRoot.insertChild(1, result.rightPageId);

                    long newRootPageId = allocatePageId();
                    writeNode(newRoot, newRootPageId);

                    return new StatelessInsertResult(newRootPageId, false);

                } else {
                    // 루트 갱신 (COW)
                    return new StatelessInsertResult(result.leftPageId, false);
                }
            }

        } catch (FxException e) {
            throw e;
        } catch (Exception e) {
            throw new FxException(FxErrorCode.IO, "Failed to insert into B+Tree", e);
        }
    }

    /**
     * 지정된 root에서 삭제 (Stateless, Write Lock 하에서만 호출)
     *
     * <p>Phase 8 동시성 지원: COW 방식으로 새 페이지를 생성하고,
     * 기존 페이지는 수정하지 않습니다.</p>
     *
     * @param currentRoot 현재 root (스냅샷에서 획득)
     * @param key 삭제 키
     * @return 삭제 결과 (새 root pageId 포함)
     */
    public StatelessDeleteResult deleteWithRoot(long currentRoot, byte[] key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        // 빈 트리에서 삭제 시도: no-op
        if (currentRoot == 0) {
            return new StatelessDeleteResult(0, false);
        }

        // 루트부터 삭제 수행 (originalRoot = currentRoot)
        DeleteResult result = deleteInternal(currentRoot, key, currentRoot);

        return new StatelessDeleteResult(result.newRootPageId, result.found);
    }

    /**
     * Stateless 삽입 결과 (Phase 8)
     */
    public static final class StatelessInsertResult {
        /** 새 루트 페이지 ID */
        public final long newRootPageId;
        /** 키가 이미 존재하여 값만 교체되었는지 여부 */
        public final boolean replaced;

        public StatelessInsertResult(long newRootPageId, boolean replaced) {
            this.newRootPageId = newRootPageId;
            this.replaced = replaced;
        }

        public long getNewRootPageId() {
            return newRootPageId;
        }

        public boolean isReplaced() {
            return replaced;
        }
    }

    /**
     * Stateless 삭제 결과 (Phase 8)
     */
    public static final class StatelessDeleteResult {
        /** 새 루트 페이지 ID (0이면 빈 트리) */
        public final long newRootPageId;
        /** 키를 찾아서 삭제했는지 여부 */
        public final boolean deleted;

        public StatelessDeleteResult(long newRootPageId, boolean deleted) {
            this.newRootPageId = newRootPageId;
            this.deleted = deleted;
        }

        public long getNewRootPageId() {
            return newRootPageId;
        }

        public boolean isDeleted() {
            return deleted;
        }
    }

    /**
     * 삽입 결과 (내부 사용)
     */
    private static class InsertResult {
        final boolean split;        // 분할 발생 여부
        final long leftPageId;      // 왼쪽 (또는 유일한) 페이지 ID
        final long rightPageId;     // 오른쪽 페이지 ID (split=true일 때)
        final byte[] splitKey;      // 분리자 키 (split=true일 때)
        
        InsertResult(boolean split, long leftPageId, long rightPageId, byte[] splitKey) {
            this.split = split;
            this.leftPageId = leftPageId;
            this.rightPageId = rightPageId;
            this.splitKey = splitKey;
        }
    }
    
    /**
     * 삭제 결과 (내부 사용)
     */
    private static class DeleteResult {
        final boolean found;           // 키를 찾아서 삭제했는지
        final long newRootPageId;      // 새 서브트리 루트 (0이면 빈 트리)
        
        DeleteResult(boolean found, long newRootPageId) {
            this.found = found;
            this.newRootPageId = newRootPageId;
        }
    }
    
    /**
     * 키-값 쌍 (Entry)
     */
    public static class Entry {
        public final byte[] key;
        public final Long valueRecordId;
        
        public Entry(byte[] key, Long valueRecordId) {
            this.key = key;
            this.valueRecordId = valueRecordId;
        }
        
        public byte[] getKey() {
            return key;
        }
        
        public Long getValueRecordId() {
            return valueRecordId;
        }
    }
    
    /**
     * 키에 대한 값 조회
     * 
     * @param key 찾을 키
     * @return 값 레코드 ID, 없으면 null
     */
    public Long get(byte[] key) {
        return find(key);
    }
    
    /**
     * 키-값 쌍 삽입 (insert와 동일)
     * 
     * @param key 키
     * @param valueRecordId 값 레코드 ID
     * @return 기존 값 (없으면 null) - 현재 구현에서는 항상 null
     */
    public Long put(byte[] key, Long valueRecordId) {
        Long oldValue = find(key);
        insert(key, valueRecordId);
        return oldValue;
    }
    
    /**
     * 키 삭제
     * 
     * @param key 삭제할 키
     * @return 삭제된 값 (없으면 null)
     */
    public Long remove(byte[] key) {
        Long oldValue = find(key);
        if (oldValue != null) {
            delete(key);
        }
        return oldValue;
    }
    
    /**
     * 트리 초기화 (모든 엔트리 삭제)
     */
    public void clear() {
        this.rootPageId = 0;
    }
    
    /**
     * 트리 크기 반환
     * 
     * @return 엔트리 수
     */
    public long size() {
        if (rootPageId == 0) {
            return 0;
        }
        return sizeRecursive(rootPageId);
    }
    
    private long sizeRecursive(long nodePageId) {
        BTreeNode node = readNode(nodePageId);
        
        if (node.isLeaf()) {
            BTreeLeaf leaf = (BTreeLeaf) node;
            return leaf.size();
        } else {
            BTreeInternal internal = (BTreeInternal) node;
            long count = 0;
            for (int i = 0; i < internal.getChildCount(); i++) {
                count += sizeRecursive(internal.getChildPageId(i));
            }
            return count;
        }
    }
    
    /**
     * 첫 번째 엔트리 반환
     * 
     * @return 첫 번째 엔트리, 빈 트리면 null
     */
    public Entry first() {
        if (rootPageId == 0) {
            return null;
        }
        
        // 가장 왼쪽 리프까지 하강
        BTreeNode node = readNode(rootPageId);
        while (!node.isLeaf()) {
            BTreeInternal internal = (BTreeInternal) node;
            long childPageId = internal.getChildPageId(0);
            node = readNode(childPageId);
        }
        
        BTreeLeaf leaf = (BTreeLeaf) node;
        if (leaf.size() == 0) {
            return null;
        }
        
        return new Entry(leaf.getKey(0), leaf.getValueRecordId(0));
    }
    
    /**
     * 마지막 엔트리 반환
     * 
     * @return 마지막 엔트리, 빈 트리면 null
     */
    public Entry last() {
        if (rootPageId == 0) {
            return null;
        }
        
        // 가장 오른쪽 리프까지 하강
        BTreeNode node = readNode(rootPageId);
        while (!node.isLeaf()) {
            BTreeInternal internal = (BTreeInternal) node;
            long childPageId = internal.getChildPageId(internal.getChildCount() - 1);
            node = readNode(childPageId);
        }
        
        BTreeLeaf leaf = (BTreeLeaf) node;
        if (leaf.size() == 0) {
            return null;
        }
        
        int lastIndex = leaf.size() - 1;
        return new Entry(leaf.getKey(lastIndex), leaf.getValueRecordId(lastIndex));
    }
    
    /**
     * 주어진 키보다 작은 가장 큰 엔트리 반환
     * 
     * @param key 기준 키
     * @return 엔트리, 없으면 null
     */
    public Entry lower(byte[] key) {
        return searchRelative(key, -1, false);
    }
    
    /**
     * 주어진 키보다 작거나 같은 가장 큰 엔트리 반환
     * 
     * @param key 기준 키
     * @return 엔트리, 없으면 null
     */
    public Entry floor(byte[] key) {
        return searchRelative(key, -1, true);
    }
    
    /**
     * 주어진 키보다 크거나 같은 가장 작은 엔트리 반환
     * 
     * @param key 기준 키
     * @return 엔트리, 없으면 null
     */
    public Entry ceiling(byte[] key) {
        return searchRelative(key, 1, true);
    }
    
    /**
     * 주어진 키보다 큰 가장 작은 엔트리 반환
     * 
     * @param key 기준 키
     * @return 엔트리, 없으면 null
     */
    public Entry higher(byte[] key) {
        return searchRelative(key, 1, false);
    }
    
    /**
     * 상대 검색 (lower/floor/ceiling/higher 구현용)
     * 
     * @param key 기준 키
     * @param direction -1이면 왼쪽(이전), 1이면 오른쪽(다음)
     * @param inclusive true면 같은 키 포함
     * @return 엔트리, 없으면 null
     */
    private Entry searchRelative(byte[] key, int direction, boolean inclusive) {
        if (rootPageId == 0) {
            return null;
        }
        
        // 커서를 사용하여 검색
        BTreeCursor cursor = cursor();
        
        // key와 가장 가까운 위치로 이동
        while (cursor.hasNext()) {
            Entry entry = cursor.next();
            int cmp = keyComparator.compare(entry.key, key);
            
            if (direction < 0) {
                // lower/floor: key보다 작은 것
                if (inclusive && cmp == 0) {
                    return entry;
                }
                if (cmp < 0) {
                    // 다음 것이 key보다 크거나 같으면 현재 반환
                    Entry next = cursor.hasNext() ? cursor.peek() : null;
                    if (next == null || keyComparator.compare(next.key, key) >= 0) {
                        return entry;
                    }
                }
            } else {
                // ceiling/higher: key보다 큰 것
                if (inclusive && cmp == 0) {
                    return entry;
                }
                if (cmp > 0) {
                    return entry;
                }
            }
        }
        
        return null;
    }
}
