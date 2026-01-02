# Phase 6 테스트 시나리오 - List (OST) 구현

> **Phase:** 6  
> **기간:** 2주  
> **목표:** Order-Statistic Tree 기반 List 구현 및 검증

[← 목차로 돌아가기](00.index.md)

---

## 시나리오 개요

Phase 6에서는 Order-Statistic Tree(OST) 자료구조를 구현하고, 이를 기반으로 `FxList<E>` 컬렉션을 완성합니다. OST는 각 노드에 서브트리의 요소 개수(subtreeCount)를 저장하여 O(log n) 시간에 인덱스 기반 접근을 가능하게 합니다.

---

## Week 1: OST 구조 및 조회

### 시나리오 1: OSTNode 구조 검증

**목표:** OST 노드 클래스들의 기본 구조와 subtreeCount 계산 검증

#### 테스트 케이스 1.1: OSTLeaf 생성 및 subtreeCount
```
Given: 빈 OSTLeaf 노드
When: elementRecordIds에 3개 요소 추가 [101L, 102L, 103L]
Then: subtreeCount() == 3
```

#### 테스트 케이스 1.2: OSTInternal subtreeCount 계산
```
Given: OSTInternal 노드
When: childPageIds = [page1, page2, page3]
      subtreeCounts = [10, 20, 15]
Then: subtreeCount() == 45 (10+20+15)
```

#### 테스트 케이스 1.3: 빈 노드 처리
```
Given: elementRecordIds가 빈 OSTLeaf
When: subtreeCount() 호출
Then: 0 반환
```

### 시나리오 2: OST get(index) 조회

**목표:** 인덱스 기반 요소 조회의 정확성 검증

#### 테스트 케이스 2.1: 단일 리프 노드 조회
```
Given: OSTLeaf with elements [100L, 101L, 102L, 103L, 104L]
When: get(0), get(2), get(4)
Then: 
  - get(0) == 100L
  - get(2) == 102L
  - get(4) == 104L
```

#### 테스트 케이스 2.2: 다층 트리 조회
```
Given: OST 트리 구조:
       Internal (subtreeCount=10)
       ├─ Leaf1 [0-4] (count=5)
       └─ Leaf2 [5-9] (count=5)
When: get(3), get(7)
Then:
  - get(3) → Leaf1에서 index 3 반환
  - get(7) → Leaf2에서 index 2(7-5) 반환
```

#### 테스트 케이스 2.3: 복잡한 트리 탐색
```
Given: 3단계 OST 트리
       Root (count=100)
       ├─ Internal1 (count=40)
       │  ├─ Leaf1 (count=20)
       │  └─ Leaf2 (count=20)
       └─ Internal2 (count=60)
          ├─ Leaf3 (count=30)
          └─ Leaf4 (count=30)
When: get(25), get(55), get(85)
Then:
  - get(25) → Internal1 → Leaf2 → index 5
  - get(55) → Internal2 → Leaf3 → index 15
  - get(85) → Internal2 → Leaf4 → index 25
```

### 시나리오 3: OST 경계 조건 및 예외 처리

**목표:** 인덱스 범위 검증 및 예외 처리

#### 테스트 케이스 3.1: 범위 초과 (상한)
```
Given: OST with size=10
When: get(10) 호출
Then: IndexOutOfBoundsException 발생
      메시지: "Index 10 out of bounds for size 10"
```

#### 테스트 케이스 3.2: 범위 초과 (하한)
```
Given: OST with size=10
When: get(-1) 호출
Then: IndexOutOfBoundsException 발생
```

#### 테스트 케이스 3.3: 빈 트리 조회
```
Given: 빈 OST (size=0)
When: get(0) 호출
Then: IndexOutOfBoundsException 발생
```

### 시나리오 4: OST 페이지 직렬화/역직렬화

**목표:** OSTNode의 toPage/fromPage 정확성 검증

#### 테스트 케이스 4.1: OSTLeaf 직렬화
```
Given: OSTLeaf with elementRecordIds=[100L, 200L, 300L]
When: toPage() 호출
Then: Page 구조 검증
  - [0-3]: 노드 타입 (OST_LEAF)
  - [4-7]: elementCount = 3
  - [8-15]: elementRecordId[0] = 100L
  - [16-23]: elementRecordId[1] = 200L
  - [24-31]: elementRecordId[2] = 300L
```

#### 테스트 케이스 4.2: OSTInternal 직렬화
```
Given: OSTInternal with:
       childPageIds = [1000L, 2000L]
       subtreeCounts = [50, 60]
When: toPage() 호출
Then: Page 구조 검증
  - [0-3]: 노드 타입 (OST_INTERNAL)
  - [4-7]: childCount = 2
  - [8-15]: childPageId[0] = 1000L
  - [16-19]: subtreeCount[0] = 50
  - [20-27]: childPageId[1] = 2000L
  - [28-31]: subtreeCount[1] = 60
```

#### 테스트 케이스 4.3: 직렬화-역직렬화 왕복
```
Given: OSTLeaf original
When: page = original.toPage()
      restored = OSTLeaf.fromPage(page)
Then: original.equals(restored)
      original.subtreeCount() == restored.subtreeCount()
```

### 시나리오 5: subtreeCount 일관성 (INV-7)

**목표:** 불변식 INV-7 검증 - 모든 노드의 subtreeCount는 자식들의 합과 일치

#### 테스트 케이스 5.1: Leaf 노드 일관성
```
Given: OSTLeaf with N elements
Then: subtreeCount() == elementRecordIds.size()
```

#### 테스트 케이스 5.2: Internal 노드 일관성
```
Given: OSTInternal with children
Then: subtreeCount() == sum(subtreeCounts)
```

#### 테스트 케이스 5.3: 전체 트리 일관성
```
Given: 복잡한 OST 트리
When: 전체 트리 순회
Then: 모든 Internal 노드에서
      node.subtreeCount() == sum(child.subtreeCount() for child in children)
```

---

## Week 2: OST 삽입/삭제 및 FxList

### 시나리오 6: OST insert(index, element)

**목표:** 인덱스 기반 삽입 및 subtreeCount 갱신 검증

#### 테스트 케이스 6.1: 리프에 삽입 (분할 없음)
```
Given: OSTLeaf with elements [100L, 200L, 300L]
When: insert(1, 150L)
Then: 
  - elements = [100L, 150L, 200L, 300L]
  - subtreeCount() == 4
```

#### 테스트 케이스 6.2: 리프 시작에 삽입
```
Given: OSTLeaf with elements [100L, 200L]
When: insert(0, 50L)
Then: elements = [50L, 100L, 200L]
```

#### 테스트 케이스 6.3: 리프 끝에 삽입
```
Given: OSTLeaf with elements [100L, 200L]
When: insert(2, 300L)
Then: elements = [100L, 200L, 300L]
```

#### 테스트 케이스 6.4: 리프 분할
```
Given: OSTLeaf가 최대 용량 (예: 128개)
When: insert(64, newElement)
Then:
  - 두 개의 리프로 분할
  - 원래 리프: elements[0-64] (count=65)
  - 새 리프: elements[65-128] (count=64)
  - 부모 Internal의 subtreeCount 갱신
```

#### 테스트 케이스 6.5: subtreeCount 전파
```
Given: 3단계 트리에서 Leaf에 삽입
When: insert 수행
Then: 경로상의 모든 Internal 노드의 subtreeCount가 +1 갱신됨
```

### 시나리오 7: OST remove(index)

**목표:** 인덱스 기반 삭제 및 subtreeCount 갱신 검증

#### 테스트 케이스 7.1: 리프에서 삭제
```
Given: OSTLeaf with elements [100L, 200L, 300L, 400L]
When: remove(1)
Then:
  - elements = [100L, 300L, 400L]
  - subtreeCount() == 3
  - 반환값 == 200L
```

#### 테스트 케이스 7.2: 첫 요소 삭제
```
Given: OSTLeaf with elements [100L, 200L, 300L]
When: remove(0)
Then: elements = [200L, 300L]
```

#### 테스트 케이스 7.3: 마지막 요소 삭제
```
Given: OSTLeaf with elements [100L, 200L, 300L]
When: remove(2)
Then: elements = [100L, 200L]
```

#### 테스트 케이스 7.4: 리프 병합 (underflow)
```
Given: 두 인접 리프가 병합 임계값 미만
When: remove 수행
Then: 두 리프 병합
      부모 Internal의 childPageIds 및 subtreeCounts 갱신
```

#### 테스트 케이스 7.5: subtreeCount 전파
```
Given: 다층 트리에서 remove
When: remove(index) 수행
Then: 경로상의 모든 Internal 노드의 subtreeCount가 -1 갱신됨
```

### 시나리오 8: FxList<E> 기본 연산

**목표:** Java List 인터페이스 구현 검증

#### 테스트 케이스 8.1: FxList 생성 및 추가
```
Given: FxList<String> list = store.createList("myList", STRING)
When: 
  list.add("apple")
  list.add("banana")
  list.add("cherry")
Then:
  - list.size() == 3
  - list.get(0) == "apple"
  - list.get(1) == "banana"
  - list.get(2) == "cherry"
```

#### 테스트 케이스 8.2: add(index, element)
```
Given: FxList ["A", "C", "D"]
When: list.add(1, "B")
Then: list = ["A", "B", "C", "D"]
      list.size() == 4
```

#### 테스트 케이스 8.3: set(index, element)
```
Given: FxList ["A", "B", "C"]
When: old = list.set(1, "X")
Then:
  - old == "B"
  - list.get(1) == "X"
  - list.size() == 3 (변경 없음)
```

#### 테스트 케이스 8.4: remove(index)
```
Given: FxList ["A", "B", "C", "D"]
When: removed = list.remove(1)
Then:
  - removed == "B"
  - list = ["A", "C", "D"]
  - list.size() == 3
```

#### 테스트 케이스 8.5: clear()
```
Given: FxList with 100 elements
When: list.clear()
Then:
  - list.size() == 0
  - list.isEmpty() == true
```

### 시나리오 9: FxList Iterator

**목표:** List의 iterator/listIterator 검증

#### 테스트 케이스 9.1: 순방향 iterator
```
Given: FxList ["A", "B", "C"]
When: Iterator<String> it = list.iterator()
Then:
  - it.next() == "A"
  - it.next() == "B"
  - it.next() == "C"
  - it.hasNext() == false
```

#### 테스트 케이스 9.2: listIterator 양방향
```
Given: FxList [1, 2, 3, 4, 5]
When: ListIterator<Integer> it = list.listIterator(2)
Then:
  - it.next() == 3
  - it.previous() == 3
  - it.hasPrevious() == true
  - it.previous() == 2
```

#### 테스트 케이스 9.3: iterator.remove()
```
Given: FxList ["A", "B", "C"]
When: 
  Iterator<String> it = list.iterator()
  it.next() // "A"
  it.remove()
Then: list = ["B", "C"]
```

### 시나리오 10: FxList 대용량 데이터

**목표:** 대용량 List 연산 성능 및 안정성 검증

#### 테스트 케이스 10.1: 10만 개 순차 삽입
```
Given: 빈 FxList<Integer>
When: for (int i = 0; i < 100_000; i++) list.add(i)
Then:
  - list.size() == 100_000
  - list.get(0) == 0
  - list.get(50_000) == 50_000
  - list.get(99_999) == 99_999
  - 시간 < 10초
```

#### 테스트 케이스 10.2: 중간 삽입 성능
```
Given: FxList with 10,000 elements
When: for (int i = 0; i < 1_000; i++) list.add(5000, i)
Then: 
  - list.size() == 11_000
  - 모든 삽입 완료 (크래시 없음)
```

#### 테스트 케이스 10.3: 랜덤 액세스
```
Given: FxList with 100,000 elements
When: Random 시드로 10,000번 랜덤 get(index)
Then: 모든 조회가 올바른 값 반환
```

### 시나리오 11: FxList 영속성

**목표:** List의 commit/rollback 및 재오픈 검증

#### 테스트 케이스 11.1: commit 후 재오픈
```
Given: FxList에 데이터 추가 후 commit()
When: store.close() 후 재오픈
      FxList<String> reopened = store.openList("myList", STRING)
Then: reopened의 모든 데이터가 원본과 동일
```

#### 테스트 케이스 11.2: rollback
```
Given: FxList ["A", "B", "C"] (committed)
When: 
  list.add("D")
  list.add("E")
  store.rollback()
Then: list = ["A", "B", "C"] (D, E 취소됨)
```

#### 테스트 케이스 11.3: 트랜잭션 중 crash 시뮬레이션
```
Given: FxList with committed data
When: 
  - 100개 요소 추가 (미commit)
  - 강제 종료 (commit 없이)
  - 재오픈
Then: 추가된 100개 요소는 사라짐 (롤백됨)
```

### 시나리오 12: Equivalence Test (FxList vs ArrayList)

**목표:** Java 표준 ArrayList와의 동작 일치성 검증

#### 테스트 케이스 12.1: 동일 연산 시퀀스
```
Given: FxList<Integer> fx = ..., ArrayList<Integer> ref = ...
When: 동일한 연산 100번 수행
  - add(element)
  - add(index, element)
  - remove(index)
  - set(index, element)
Then: 
  - fx.size() == ref.size()
  - for (int i : 0..size) fx.get(i) == ref.get(i)
```

#### 테스트 케이스 12.2: 랜덤 연산 1000회
```
Given: FxList, ArrayList (동일 시드)
When: Random으로 1000번 연산
  - 70% add
  - 20% remove
  - 10% set
Then: 모든 인덱스에서 fx == ref
```

#### 테스트 케이스 12.3: Iterator 동작 일치
```
Given: FxList, ArrayList (동일 데이터)
When: 양쪽 iterator로 순회
Then: 
  - 순회 순서 동일
  - iterator.remove() 결과 동일
```

### 시나리오 13: 엣지 케이스

**목표:** 특수 상황 및 경계 조건 검증

#### 테스트 케이스 13.1: 단일 요소 List
```
Given: FxList with 1 element ["X"]
When: remove(0)
Then: list.isEmpty() == true
```

#### 테스트 케이스 13.2: null 요소 (nullable 코덱)
```
Given: FxList<String> (nullable)
When: list.add(null)
Then: 
  - list.get(0) == null
  - list.size() == 1
```

#### 테스트 케이스 13.3: 큰 객체 저장
```
Given: FxList<byte[]>
When: list.add(new byte[1 << 20]) // 1MiB
Then: get(0).length == 1 << 20
```

#### 테스트 케이스 13.4: subList() 뷰
```
Given: FxList [0, 1, 2, 3, 4, 5]
When: List<Integer> sub = list.subList(2, 5)
Then:
  - sub.size() == 3
  - sub.get(0) == 2
  - sub에 수정 → 원본 list에 반영
```

---

## 커버리지 목표

### Phase 6 코드 커버리지
- **최소:** 95%
- **목표:** 98%

### 커버해야 할 핵심 경로
1. ✅ OSTLeaf: get, insert, remove, split
2. ✅ OSTInternal: get, insert, remove, subtreeCount 계산
3. ✅ OST 트리 탐색 (단일/다층)
4. ✅ 페이지 직렬화/역직렬화
5. ✅ FxList 모든 List 메서드
6. ✅ Iterator/ListIterator
7. ✅ 트랜잭션 통합 (commit/rollback)
8. ✅ 예외 처리 (IndexOutOfBoundsException 등)

---

## 성능 기준

### OST 연산 복잡도
- **get(index)**: O(log n)
- **insert(index, element)**: O(log n + 페이지 I/O)
- **remove(index)**: O(log n + 페이지 I/O)

### FxList 벤치마크 목표
- **100,000 순차 추가**: < 10초
- **10,000 랜덤 조회**: < 1초
- **1,000 중간 삽입**: < 5초

---

## 회귀 테스트 범위

Phase 6 완료 시 다음 모든 테스트가 통과해야 합니다:

1. ✅ Phase 0 테스트 (바이트 유틸, Enum)
2. ✅ Phase 1 테스트 (Codec)
3. ✅ Phase 2 테스트 (Storage, Page)
4. ✅ Phase 3 테스트 (BTree)
5. ✅ Phase 4 테스트 (Catalog, State)
6. ✅ Phase 5 테스트 (Map, Set, Deque)
7. ✅ **Phase 6 테스트 (OST, List)** ← 신규

---

## 불변식 검증

Phase 6에서 검증할 불변식:

- **INV-7**: OST 노드의 subtreeCount 일관성
  - Leaf: `subtreeCount == elementRecordIds.size()`
  - Internal: `subtreeCount == sum(child.subtreeCount for child in children)`

- **INV-1**: 모든 RecordId는 유효한 페이지를 가리킴
- **INV-2**: BTree 정렬 순서 (FxList는 순서 컬렉션이므로 N/A)
- **INV-3**: CatalogTree에 "myList" 엔트리 존재
- **INV-6**: Pending 변경사항은 StateTree에 기록됨

---

## 테스트 작성 순서

1. **Week 1 Day 5**: 시나리오 1-5 작성 (OST 구조, 조회, 직렬화)
2. **Week 2 Day 6**: 시나리오 6-13 작성 (삽입/삭제, FxList, Equivalence)
3. 각 시나리오마다 JUnit 테스트 클래스 생성
4. 실패 → 코드 개선 → 회귀 테스트 무한 반복
5. 모든 테스트 통과 + 커버리지 95% 달성

---

## 평가 기준 체크리스트

테스트 완료 후 [03.quality-criteria.md](03.quality-criteria.md)의 7가지 기준 평가:

- [ ] 기준 1: Plan-Code 정합성 (이 시나리오 문서와 코드 일치)
- [ ] 기준 2: SOLID 원칙 준수
- [ ] 기준 3: 테스트 커버리지 ≥ 95%
- [ ] 기준 4: 코드 가독성
- [ ] 기준 5: 예외 처리 및 안정성
- [ ] 기준 6: 성능 효율성
- [ ] 기준 7: 문서화 품질

**모든 기준 A+ 달성 시까지 개선 반복** (타협 없음)

---

*문서 작성일: 2025-12-25*  
*Phase 6 Week 1-2 통합 시나리오*
