# FxStore 불변식 검증 체크리스트

> **참조 문서**: [@docs/02.architecture.md](../spec/lagacy/02.architecture.md) - 1.2 핵심 불변식  
> **목적**: 모든 Phase에서 불변식 유지 검증  
> **우선순위**: 최상위 (Highest)

[← 목차로 돌아가기](00.index.md)

---

## 개요

FxStore의 정확성은 **9가지 핵심 불변식(Invariants)**에 기반합니다. 
이 불변식들이 깨지면 데이터 손상이 발생합니다.

**모든 Phase는 해당하는 불변식을 검증하는 테스트를 작성해야 합니다.**

---

## 9가지 핵심 불변식

### INV-1: CommitHeader seqNo 단조 증가

**정의:**
```
∀ commit c1, c2: c1.time < c2.time → c1.seqNo < c2.seqNo
```

**의미**: 커밋 순서대로 시퀀스 번호가 증가

**검증 Phase**: Phase 2 (CommitHeader), Phase 9 (커밋 프로토콜)

**검증 방법**:
```java
@Test
void testSeqNoMonotonicallyIncreases() {
    // 여러 커밋 수행
    long seqNo1 = getCurrentSeqNo();
    store.commit();
    long seqNo2 = getCurrentSeqNo();
    store.commit();
    long seqNo3 = getCurrentSeqNo();
    
    // 검증
    assertTrue(seqNo1 < seqNo2);
    assertTrue(seqNo2 < seqNo3);
}
```

**실패 시 증상**:
- 크래시 복구 시 잘못된 헤더 선택
- 데이터 손실

---

### INV-2: 커밋된 헤더의 모든 페이지 존재

**정의:**
```
∀ pageId ∈ reachable(header): pageId * pageSize < fileSize
```

**의미**: 커밋된 헤더가 가리키는 모든 페이지는 디스크에 존재

**검증 Phase**: Phase 2 (Allocator), Phase 9 (커밋)

**검증 방법**:
```java
@Test
void testAllReachablePagesExist() {
    CommitHeader header = loadCommittedHeader();
    Set<Long> reachablePageIds = collectReachablePages(header);
    
    long fileSize = storage.size();
    for (long pageId : reachablePageIds) {
        long offset = pageId * pageSize;
        assertTrue(offset < fileSize, 
            "Page " + pageId + " at offset " + offset + 
            " exceeds file size " + fileSize);
    }
}
```

**실패 시 증상**:
- I/O 오류 (읽기 실패)
- CORRUPTION 예외

---

### INV-3: Catalog name 유일성

**정의:**
```
∀ e1, e2 ∈ Catalog: e1.name = e2.name → e1 = e2
```

**의미**: 컬렉션 이름은 유일

**검증 Phase**: Phase 4 (Catalog)

**검증 방법**:
```java
@Test
void testCatalogNameUniqueness() {
    store.createMap("users", Long.class, String.class);
    
    // 중복 이름 생성 시도
    assertThrows(FxException.class, () -> 
        store.createSet("users", String.class)
    );
}

@Test
void testCatalogListUniqueNames() {
    store.createMap("m1", Long.class, String.class);
    store.createSet("s1", String.class);
    
    List<CollectionInfo> collections = store.list();
    Set<String> names = new HashSet<>();
    for (CollectionInfo info : collections) {
        assertTrue(names.add(info.name()), 
            "Duplicate name: " + info.name());
    }
}
```

**실패 시 증상**:
- open() 시 잘못된 컬렉션 반환
- drop() 시 여러 컬렉션 삭제

---

### INV-4: State collectionId 유일성

**정의:**
```
∀ s1, s2 ∈ State: s1.collectionId = s2.collectionId → s1 = s2
```

**의미**: 컬렉션 ID는 유일

**검증 Phase**: Phase 4 (State)

**검증 방법**:
```java
@Test
void testStateCollectionIdUniqueness() {
    long id1 = createCollection("c1");
    long id2 = createCollection("c2");
    
    assertNotEquals(id1, id2);
    
    // State 트리 스캔
    StateTree stateTree = getStateTree();
    Set<Long> ids = new HashSet<>();
    for (CollectionState state : stateTree.scanAll()) {
        assertTrue(ids.add(state.collectionId()), 
            "Duplicate collectionId: " + state.collectionId());
    }
}
```

**실패 시 증상**:
- 잘못된 컬렉션 데이터 반환
- 데이터 덮어쓰기

---

### INV-5: collectionId 재사용 금지

**정의:**
```
nextCollectionId는 항상 증가만 한다 (삭제 후에도)
```

**의미**: 삭제된 컬렉션의 ID를 재사용하지 않음

**검증 Phase**: Phase 4 (State)

**검증 방법**:
```java
@Test
void testCollectionIdNeverReused() {
    // 컬렉션 생성 후 ID 기록
    long id1 = createCollectionAndGetId("c1");
    store.drop("c1");
    
    // 새 컬렉션 생성
    long id2 = createCollectionAndGetId("c2");
    
    // 재사용 금지 검증
    assertTrue(id2 > id1, 
        "collectionId should never be reused: id1=" + id1 + ", id2=" + id2);
}

@Test
void testNextCollectionIdMonotonicallyIncreases() {
    long next1 = getNextCollectionId();
    createCollection("c1");
    long next2 = getNextCollectionId();
    
    assertTrue(next2 > next1);
}
```

**실패 시 증상**:
- drop 후 같은 이름으로 create 시 이전 데이터 노출

---

### INV-6: B+Tree 키 정렬 순서

**정의:**
```
∀ leaf: ∀ i < j: compare(leaf.keys[i], leaf.keys[j]) < 0
```

**의미**: 리프 노드 내 키들은 정렬됨

**검증 Phase**: Phase 3 (B+Tree)

**검증 방법**:
```java
@Test
void testBTreeKeySortOrder() {
    BTree tree = createTestTree();
    
    // 모든 리프 순회
    BTreeCursor cursor = tree.cursor();
    byte[] prevKey = null;
    
    while (cursor.hasNext()) {
        Entry<byte[], Long> entry = cursor.next();
        
        if (prevKey != null) {
            int cmp = keyComparator.compare(prevKey, entry.getKey());
            assertTrue(cmp < 0, 
                "Keys out of order: " + 
                Arrays.toString(prevKey) + " >= " + 
                Arrays.toString(entry.getKey()));
        }
        
        prevKey = entry.getKey();
    }
}

@Test
void testBTreeLeafKeysOrdered() {
    BTreeLeaf leaf = loadLeaf(leafPageId);
    
    for (int i = 1; i < leaf.keyCount(); i++) {
        byte[] key1 = leaf.getKey(i - 1);
        byte[] key2 = leaf.getKey(i);
        
        int cmp = keyComparator.compare(key1, key2);
        assertTrue(cmp < 0, 
            "Leaf keys out of order at index " + i);
    }
}
```

**실패 시 증상**:
- find() 실패 (키를 찾지 못함)
- 순회 시 중복 또는 누락

---

### INV-7: OST subtreeCount 정확성

**정의:**
```
∀ internal node n: sum(n.subtreeCounts) = totalElements(n)
```

**의미**: 각 자식의 subtreeCount 합 = 해당 서브트리의 전체 원소 수

**검증 Phase**: Phase 6 (OST)

**검증 방법**:
```java
@Test
void testOSTSubtreeCountAccuracy() {
    OST ost = createTestOST();
    
    // 재귀적으로 검증
    assertTrue(verifyOSTCounts(ost.getRootPageId()));
}

boolean verifyOSTCounts(long pageId) {
    OSTNode node = loadOSTNode(pageId);
    
    if (node instanceof OSTLeaf) {
        OSTLeaf leaf = (OSTLeaf) node;
        // 리프의 subtreeCount = 원소 수
        assertEquals(leaf.elementCount(), leaf.subtreeCount());
        return true;
    }
    
    // Internal 노드
    OSTInternal internal = (OSTInternal) node;
    int computedCount = 0;
    
    for (int i = 0; i < internal.childCount(); i++) {
        long childPageId = internal.getChild(i);
        int storedCount = internal.getSubtreeCount(i);
        
        // 재귀적으로 자식 검증
        assertTrue(verifyOSTCounts(childPageId));
        
        // 자식의 실제 count 계산
        int actualCount = computeActualCount(childPageId);
        
        assertEquals(actualCount, storedCount, 
            "subtreeCount mismatch at child " + i);
        
        computedCount += storedCount;
    }
    
    // 전체 합 검증
    assertEquals(computedCount, internal.subtreeCount());
    return true;
}
```

**실패 시 증상**:
- List.get(i) 잘못된 원소 반환
- List.size() 부정확

---

### INV-8: Deque headSeq와 tailSeq 관계

**정의:**
```
headSeq ≤ tailSeq + 1
```

**의미**: 
- 비어있으면 headSeq > tailSeq (예: head=0, tail=-1)
- 비어있지 않으면 headSeq ≤ tailSeq

**검증 Phase**: Phase 5 (Deque)

**검증 방법**:
```java
@Test
void testDequeHeadTailInvariant() {
    FxDeque<String> deque = createDeque();
    
    // 빈 Deque
    long head = deque.getHeadSeq();
    long tail = deque.getTailSeq();
    assertTrue(head <= tail + 1, 
        "Empty deque invariant: head=" + head + ", tail=" + tail);
    
    // 원소 추가
    deque.addLast("A");
    head = deque.getHeadSeq();
    tail = deque.getTailSeq();
    assertTrue(head <= tail, 
        "Non-empty deque: head=" + head + ", tail=" + tail);
    
    // size 검증
    long expectedSize = tail - head + 1;
    assertEquals(expectedSize, deque.size());
}

@Test
void testDequeSequenceAfterOperations() {
    FxDeque<String> deque = createDeque();
    
    deque.addFirst("A");  // head=-1, tail=-1
    assertEquals(-1, deque.getHeadSeq());
    assertEquals(-1, deque.getTailSeq());
    
    deque.addLast("B");   // head=-1, tail=0
    assertEquals(-1, deque.getHeadSeq());
    assertEquals(0, deque.getTailSeq());
    
    deque.removeFirst(); // head=0, tail=0
    assertEquals(0, deque.getHeadSeq());
    assertEquals(0, deque.getTailSeq());
}
```

**실패 시 증상**:
- size() 음수 또는 부정확
- removeFirst/removeLast 실패

---

### INV-9: allocTail 단조 증가

**정의:**
```
allocTail은 항상 증가만 한다 (컴팩션 제외)
```

**의미**: append-only 할당

**검증 Phase**: Phase 2 (Allocator)

**검증 방법**:
```java
@Test
void testAllocTailMonotonicallyIncreases() {
    Allocator allocator = new Allocator(storage, pageSize, 12288);
    
    long tail1 = allocator.getAllocTail();
    allocator.allocatePage();
    long tail2 = allocator.getAllocTail();
    allocator.allocateRecord(100);
    long tail3 = allocator.getAllocTail();
    
    assertTrue(tail1 < tail2);
    assertTrue(tail2 < tail3);
}

@Test
void testNoSpaceReuse() {
    Allocator allocator = new Allocator(storage, pageSize, 12288);
    
    // 페이지 할당
    long page1 = allocator.allocatePage();
    long tail1 = allocator.getAllocTail();
    
    // 레코드 할당 (페이지 삭제 후에도 공간 재사용 안 됨)
    long record1 = allocator.allocateRecord(100);
    long tail2 = allocator.getAllocTail();
    
    assertTrue(tail2 > tail1);
    assertTrue(record1 >= tail1); // 새 할당은 항상 tail 이후
}
```

**실패 시 증상**:
- 데이터 덮어쓰기
- 크래시 후 손상

---

## Phase별 불변식 검증 매트릭스

| Phase | INV-1 | INV-2 | INV-3 | INV-4 | INV-5 | INV-6 | INV-7 | INV-8 | INV-9 |
|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|
| Phase 0 | - | - | - | - | - | - | - | - | - |
| Phase 1 | - | - | - | - | - | - | - | - | - |
| Phase 2 | ✅ | ✅ | - | - | - | - | - | - | ✅ |
| Phase 3 | - | - | - | - | - | ✅ | - | - | - |
| Phase 4 | - | - | ✅ | ✅ | ✅ | - | - | - | - |
| Phase 5 | - | - | - | - | - | - | - | ✅ | - |
| Phase 6 | - | - | - | - | - | - | ✅ | - | - |
| Phase 7 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**Phase 7에서 모든 불변식을 최종 검증합니다.**

---

## 불변식 위반 시 대응

### 검출 방법

1. **단위 테스트**: 각 Phase의 불변식 검증 테스트
2. **통합 테스트**: 여러 연산 후 불변식 유지 확인
3. **verify()**: 운영 중 주기적 검증

### 대응 절차

```
불변식 위반 검출
   ↓
테스트 실패 분석
   ↓
코드 개선
   ↓
회귀 테스트 실행
   ↓
모든 불변식 재검증
   ↓
통과 시 → 다음 단계
실패 시 → 개선 단계로 복귀
```

### 타협 금지

**불변식 위반은 절대 허용되지 않습니다.**

- ❌ "대부분 유지되면 괜찮다"
- ❌ "특정 케이스만 위반"
- ❌ "성능을 위해 검증 생략"
- ✅ **모든 불변식 100% 유지 필수**

---

## 체크리스트

### Phase 완료 전 확인

각 Phase 완료 시 다음을 확인:

- [ ] 해당 Phase의 모든 불변식 테스트 작성
- [ ] 모든 불변식 테스트 통과
- [ ] 불변식 위반 시 명확한 오류 메시지
- [ ] 회귀 테스트에 불변식 검증 포함

### 최종 릴리스 전 확인

- [ ] 9가지 불변식 모두 검증
- [ ] 통합 테스트에서 불변식 유지 확인
- [ ] verify() 메서드가 모든 불변식 검사
- [ ] 문서화: 각 불변식의 의미와 중요성

---

## 참고 자료

- [@docs/02.architecture.md - 1.2 핵심 불변식](../spec/lagacy/02.architecture.md#12-핵심-불변식-invariants)
- [03.quality-criteria.md](03.quality-criteria.md)
- [04.regression-process.md](04.regression-process.md)

---

[← 목차로 돌아가기](00.index.md)

**작성일**: 2025-12-24  
**버전**: 1.0  
**우선순위**: 최상위 (Highest)
