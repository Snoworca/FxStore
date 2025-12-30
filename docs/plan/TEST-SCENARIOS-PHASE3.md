# Phase 3: B+Tree 구현 테스트 시나리오

> **Phase:** 3 - B+Tree 구현  
> **기간:** 3주  
> **작성일:** 2025-12-24  
> **기반 문서:** docs/01.api.md, docs/02.architecture.md

---

## 목차

- [Week 1: B+Tree 기본 연산](#week-1-btree-기본-연산)
- [Week 2: B+Tree 삽입 및 분할](#week-2-btree-삽입-및-분할)
- [Week 3: 삭제, COW, Cursor](#week-3-삭제-cow-cursor)
- [통합 시나리오](#통합-시나리오)

---

## Week 1: B+Tree 기본 연산

### 시나리오 그룹 1: BTreeLeaf 노드 연산

#### 시나리오 1.1: 빈 리프 노드 생성
**목적:** 빈 BTreeLeaf 초기화 검증

**전제 조건:**
- PageSize = 4096
- 최대 엔트리 수 = 100 (설계 기준)

**실행 단계:**
1. `BTreeLeaf leaf = new BTreeLeaf(pageSize, codec)`
2. `leaf.getEntryCount()`
3. `leaf.getFreeSpace()`

**예상 결과:**
- entryCount = 0
- freeSpace ≈ 4064 (page header 제외)
- leaf.isEmpty() = true

**사후 조건:**
- 노드가 유효한 상태

---

#### 시나리오 1.2: 단일 키 삽입
**목적:** 리프 노드에 첫 키-값 삽입 검증

**전제 조건:**
- 빈 BTreeLeaf

**실행 단계:**
1. `byte[] key = encodeString("apple")`
2. `long valueRecordId = 12345L`
3. `leaf.insert(0, key, valueRecordId)`
4. `int idx = leaf.find(key, comparator)`

**예상 결과:**
- idx = 0
- leaf.getKey(0) = "apple" (바이트 배열)
- leaf.getValueRecordId(0) = 12345L
- entryCount = 1

**사후 조건:**
- 정렬 순서 유지
- freeSpace 감소

---

#### 시나리오 1.3: 정렬 순서 삽입
**목적:** 여러 키 삽입 시 정렬 순서 유지 검증

**전제 조건:**
- 빈 BTreeLeaf

**실행 단계:**
1. insert("banana", 1)
2. insert("apple", 2)  ← "banana" 앞에 삽입되어야 함
3. insert("cherry", 3)
4. 순회하며 키 확인

**예상 결과:**
- keys = ["apple", "banana", "cherry"]
- valueRecordIds = [2, 1, 3]
- 정렬 불변식 유지: ∀i < j: keys[i] < keys[j]

---

#### 시나리오 1.4: 이진 탐색 정확성
**목적:** find() 메서드의 이진 탐색 정확성 검증

**전제 조건:**
- 리프에 50개 키 삽입 (랜덤 순서)

**실행 단계:**
1. 모든 삽입된 키에 대해 find() 호출
2. 존재하지 않는 키에 대해 find() 호출

**예상 결과:**
- 존재하는 키: idx >= 0
- 존재하지 않는 키: idx < 0 (insertion point = -(idx+1))

---

### 시나리오 그룹 2: BTreeInternal 노드 연산

#### 시나리오 2.1: Internal 노드 생성 및 자식 추가
**목적:** Internal 노드의 키-자식 관계 검증

**전제 조건:**
- 2개의 리프 노드 (left, right)

**실행 단계:**
1. `BTreeInternal internal = new BTreeInternal()`
2. `internal.insertChild(0, leftPageId)`
3. `internal.insertKey(0, separatorKey)`
4. `internal.insertChild(1, rightPageId)`

**예상 결과:**
- childCount = 2
- keyCount = 1
- findChild(key < separatorKey) = leftPageId
- findChild(key >= separatorKey) = rightPageId

---

#### 시나리오 2.2: Internal 노드 자식 검색
**목적:** findChild() 이진 탐색 정확성

**전제 조건:**
- Internal에 10개 분리자 키, 11개 자식

**실행 단계:**
1. 각 키 범위에 대해 findChild() 호출
2. 경계값 테스트 (separatorKey - 1, separatorKey, separatorKey + 1)

**예상 결과:**
- key < separator[0] → child[0]
- separator[i] <= key < separator[i+1] → child[i+1]
- key >= separator[last] → child[last+1]

---

### 시나리오 그룹 3: B+Tree 조회 (Find)

#### 시나리오 3.1: 빈 트리 조회
**목적:** 빈 트리에서 조회 시 null 반환 검증

**전제 조건:**
- rootPageId = 0 (빈 트리)

**실행 단계:**
1. `Long result = btree.find(anyKey)`

**예상 결과:**
- result = null

---

#### 시나리오 3.2: 단일 리프 트리 조회
**목적:** 높이 1 트리에서 조회 검증

**전제 조건:**
- root = BTreeLeaf (pageId=1)
- 삽입: ("apple", 100), ("banana", 200)

**실행 단계:**
1. `btree.find("apple")`
2. `btree.find("banana")`
3. `btree.find("cherry")`

**예상 결과:**
- find("apple") = 100
- find("banana") = 200
- find("cherry") = null

---

#### 시나리오 3.3: 높이 2 트리 조회
**목적:** Internal → Leaf 탐색 경로 검증

**전제 조건:**
```
        Internal (root)
       /       \
    Leaf1     Leaf2
    [a,b]     [c,d]
```

**실행 단계:**
1. find("a")
2. find("c")
3. find("z")

**예상 결과:**
- find("a") → Leaf1 탐색
- find("c") → Leaf2 탐색
- find("z") = null

---

#### 시나리오 3.4: 높이 3 트리 조회
**목적:** 깊은 트리 탐색 검증

**전제 조건:**
```
            Internal (L2)
           /           \
      Internal(L1)    Internal(L1)
      /      \         /      \
   Leaf1  Leaf2   Leaf3   Leaf4
```

**실행 단계:**
1. 각 리프 범위의 키 조회
2. 경계 키 조회

**예상 결과:**
- 올바른 리프까지 탐색
- O(log N) 복잡도

---

## Week 2: B+Tree 삽입 및 분할

### 시나리오 그룹 4: 삽입 (분할 없음)

#### 시나리오 4.1: 빈 트리에 첫 삽입
**목적:** 루트 리프 생성 검증

**전제 조건:**
- rootPageId = 0

**실행 단계:**
1. `btree.insert("key1", 100)`
2. `btree.getRootPageId()`

**예상 결과:**
- rootPageId != 0
- root는 BTreeLeaf
- find("key1") = 100

---

#### 시나리오 4.2: 여유 공간 있는 리프에 삽입
**목적:** 분할 없이 삽입 검증

**전제 조건:**
- 리프에 5개 엔트리 존재 (최대 100개)

**실행 단계:**
1. insert("newKey", 999)
2. find("newKey")

**예상 결과:**
- find("newKey") = 999
- 정렬 순서 유지
- 분할 발생하지 않음

---

#### 시나리오 4.3: 중복 키 삽입 (Replace)
**목적:** 동일 키 삽입 시 값 교체 검증

**전제 조건:**
- insert("key1", 100)

**실행 단계:**
1. insert("key1", 200)
2. find("key1")

**예상 결과:**
- find("key1") = 200
- entryCount 증가 없음

---

### 시나리오 그룹 5: 리프 분할 (Leaf Split)

#### 시나리오 5.1: 리프 오버플로우 시 분할
**목적:** 리프 분할 메커니즘 검증

**전제 조건:**
- 리프가 최대 용량 (100개 엔트리)

**실행 단계:**
1. insert("overflowKey", 9999)
2. 분할 확인

**예상 결과:**
- 2개의 리프 생성 (left, right)
- left: 50개 엔트리
- right: 51개 엔트리
- 부모 Internal 노드 생성
- separatorKey = right의 첫 키

**사후 조건:**
- 모든 키가 조회 가능
- 정렬 순서 유지
- 트리 높이 = 2

---

#### 시나리오 5.2: 순차 삽입 시 반복 분할
**목적:** 연속 분할 검증

**전제 조건:**
- 빈 트리

**실행 단계:**
1. 1부터 1000까지 순차 삽입
2. 트리 구조 확인

**예상 결과:**
- 여러 리프 생성 (약 10개)
- 트리 높이 ≈ 2~3
- 모든 키 find() 가능
- 정렬 순서 유지

---

#### 시나리오 5.3: 역순 삽입 시 분할
**목적:** 역순 삽입에서도 올바른 분할 검증

**전제 조건:**
- 빈 트리

**실행 단계:**
1. 1000부터 1까지 역순 삽입
2. 트리 구조 확인

**예상 결과:**
- 순차 삽입과 동일한 트리 특성
- 모든 키 조회 가능

---

#### 시나리오 5.4: 랜덤 삽입 시 균형
**목적:** 랜덤 삽입에서 트리 균형 검증

**전제 조건:**
- 빈 트리
- seed 고정 Random

**실행 단계:**
1. 1000개 랜덤 키 삽입
2. 트리 높이 및 리프 분포 확인

**예상 결과:**
- 트리 높이 ≤ log_fanout(N)
- 리프 간 엔트리 수 차이 ≤ 2배

---

### 시나리오 그룹 6: Internal 분할

#### 시나리오 6.1: Internal 오버플로우 시 분할
**목적:** Internal 노드 분할 검증

**전제 조건:**
- Internal 노드가 최대 용량 (128개 키, 129개 자식)

**실행 단계:**
1. 리프에 삽입하여 Internal까지 분할 전파
2. Internal 분할 확인

**예상 결과:**
- 2개의 Internal 생성
- middle key promoted to parent
- 트리 높이 증가

---

#### 시나리오 6.2: 루트까지 분할 전파
**목적:** 루트 분할로 트리 높이 증가 검증

**전제 조건:**
- 높이 2 트리
- 루트 Internal이 최대 용량

**실행 단계:**
1. 리프 삽입으로 루트 분할 유도
2. 트리 높이 확인

**예상 결과:**
- 새 루트 Internal 생성
- 트리 높이 = 3
- 모든 키 조회 가능

---

## Week 3: 삭제, COW, Cursor

### 시나리오 그룹 7: 삭제 (병합 없음)

#### 시나리오 7.1: 리프에서 키 삭제
**목적:** 단순 삭제 검증

**전제 조건:**
- 리프에 10개 엔트리

**실행 단계:**
1. delete("key5")
2. find("key5")

**예상 결과:**
- find("key5") = null
- entryCount = 9
- 정렬 순서 유지

---

#### 시나리오 7.2: 존재하지 않는 키 삭제
**목적:** 없는 키 삭제 시 no-op 검증

**전제 조건:**
- 리프에 몇 개 엔트리

**실행 단계:**
1. delete("nonexistentKey")

**예상 결과:**
- 아무 변화 없음
- 예외 발생하지 않음

---

#### 시나리오 7.3: 모든 키 삭제
**목적:** 빈 트리로 만들기

**전제 조건:**
- 트리에 100개 키

**실행 단계:**
1. 모든 키 삭제

**예상 결과:**
- rootPageId = 0 (빈 트리)
- find(anyKey) = null

---

### 시나리오 그룹 8: COW 전파

#### 시나리오 8.1: 리프 수정 시 COW
**목적:** 리프 변경 시 새 페이지 할당 검증

**전제 조건:**
- 단일 리프 트리
- original pageId = 100

**실행 단계:**
1. insert("newKey", 999)
2. 새 루트 pageId 확인

**예상 결과:**
- newRootPageId != 100
- 원본 페이지(100)는 그대로 유지 (이전 스냅샷)

---

#### 시나리오 8.2: Internal까지 COW 전파
**목적:** 경로상 모든 노드 COW 검증

**전제 조건:**
```
    Internal (pageId=200)
   /        \
Leaf1(100) Leaf2(101)
```

**실행 단계:**
1. Leaf1에 삽입
2. 새 경로 페이지 ID 확인

**예상 결과:**
- newLeaf1PageId != 100
- newInternalPageId != 200
- 원본 페이지들 유지

---

#### 시나리오 8.3: 이전 버전 유지 확인
**목적:** COW로 인한 스냅샷 격리 검증

**전제 조건:**
- 트리 A (root=300)

**실행 단계:**
1. oldRoot = btree.getRootPageId()  // 300
2. insert("key", 999)
3. newRoot = btree.getRootPageId()  // 400
4. oldRoot로 트리 읽기

**예상 결과:**
- oldRoot 트리: "key" 없음
- newRoot 트리: "key" 있음
- 두 버전 독립적 존재

---

### 시나리오 그룹 9: BTreeCursor 순회

#### 시나리오 9.1: 전체 순회
**목적:** Cursor로 모든 키-값 순회 검증

**전제 조건:**
- 트리에 100개 키 (랜덤 삽입)

**실행 단계:**
1. `BTreeCursor cursor = btree.cursor()`
2. `while (cursor.hasNext())` 순회
3. 순회된 키들 수집

**예상 결과:**
- 100개 키 모두 순회됨
- 정렬 순서대로 순회
- 중복/누락 없음

---

#### 시나리오 9.2: 범위 순회
**목적:** startKey, endKey 범위 순회 검증

**전제 조건:**
- 트리: [a, b, c, ..., z]

**실행 단계:**
1. `cursor = btree.cursor("d", "m")`
2. 순회

**예상 결과:**
- 순회 키: [d, e, ..., m]
- "c"와 "n" 제외됨

---

#### 시나리오 9.3: 빈 범위 순회
**목적:** startKey > endKey 시 빈 순회 검증

**전제 조건:**
- 트리에 데이터 존재

**실행 단계:**
1. `cursor = btree.cursor("z", "a")`
2. hasNext() 확인

**예상 결과:**
- hasNext() = false
- 아무것도 순회 안 됨

---

#### 시나리오 9.4: Cursor 중복 생성
**목적:** 여러 Cursor 동시 사용 검증

**전제 조건:**
- 트리에 100개 키

**실행 단계:**
1. cursor1 = btree.cursor()
2. cursor2 = btree.cursor()
3. cursor1.next() 10회
4. cursor2.next() 5회

**예상 결과:**
- 각 Cursor 독립적 상태 유지
- 서로 간섭 없음

---

## 통합 시나리오

### 시나리오 10.1: 대량 데이터 삽입/조회
**목적:** 실제 사용 사례 시뮬레이션

**전제 조건:**
- 빈 트리

**실행 단계:**
1. 10,000개 키-값 삽입 (랜덤)
2. 모든 키 find() 검증
3. cursor 순회로 정렬 순서 확인

**예상 결과:**
- 모든 키 조회 성공
- 순회 시 정렬됨
- 트리 높이 적절 (≤ 4)

---

### 시나리오 10.2: 혼합 연산 (Insert + Delete)
**목적:** 삽입/삭제 혼합 시 정확성 검증

**전제 조건:**
- 빈 트리

**실행 단계:**
1. 1000개 삽입
2. 랜덤 500개 삭제
3. 랜덤 300개 삽입
4. 나머지 키 조회

**예상 결과:**
- 현재 존재하는 키만 find() 성공
- 삭제된 키는 find() = null
- 정렬 순서 유지

---

### 시나리오 10.3: TreeMap 동등성 테스트
**목적:** Java TreeMap과 동일한 동작 검증

**전제 조건:**
- BTree와 TreeMap 각각 준비

**실행 단계:**
1. 동일한 연산 시퀀스 실행 (1000회)
   - 60% insert
   - 20% find
   - 20% delete
2. 매 100회마다 내용 비교

**예상 결과:**
- 모든 시점에서 BTree와 TreeMap 내용 일치
- cursor 순회 결과와 TreeMap.entrySet() 일치

---

## 경계 조건 시나리오

### 시나리오 11.1: 최대 크기 키
**목적:** 1 MiB 키 처리 검증 (API 명세 기준)

**실행 단계:**
1. byte[] key = new byte[1024 * 1024]
2. insert(key, 100)
3. find(key)

**예상 결과:**
- 삽입 성공
- 조회 성공
- 분할 정상 동작

---

### 시나리오 11.2: 최소 크기 키
**목적:** 빈 키 처리 (허용된다면)

**실행 단계:**
1. insert(new byte[0], 100)

**예상 결과:**
- API 명세에 따라 동작
- (빈 키 허용 시 정상 삽입, 불허 시 예외)

---

### 시나리오 11.3: 단일 엔트리 리프 분할
**목적:** 엣지 케이스 분할

**전제 조건:**
- 리프에 1개 엔트리 (매우 큰 키로 공간 소진)

**실행 단계:**
1. insert(largeKey2, 200)

**예상 결과:**
- 분할 정상 수행
- 2개 리프로 분리

---

## 예외 상황 시나리오

### 시나리오 12.1: null 키 삽입
**목적:** null 키 처리 검증

**실행 단계:**
1. insert(null, 100)

**예상 결과:**
- NullPointerException 발생 (API 명세 기준)

---

### 시나리오 12.2: 페이지 할당 실패
**목적:** 할당 실패 시 예외 처리

**전제 조건:**
- Allocator를 mock으로 실패 유도

**실행 단계:**
1. insert("key", 100) → 페이지 할당 실패

**예상 결과:**
- FxException(OUT_OF_MEMORY) 또는 FxException(IO)
- 트리 일관성 유지 (rollback)

---

## 성능 기준 시나리오

### 시나리오 13.1: 조회 복잡도
**목적:** find() O(log N) 복잡도 검증

**실행 단계:**
1. N = 10, 100, 1000, 10000 각각 테스트
2. find() 실행 시간 측정

**예상 결과:**
- 시간 = O(log N)
- N이 10배 증가 시, 시간은 상수배만 증가

---

### 시나리오 13.2: 삽입 복잡도
**목적:** insert() O(log N) 복잡도 검증

**실행 단계:**
1. 대량 삽입 시간 측정

**예상 결과:**
- 평균 삽입 시간 = O(log N)

---

## 불변식 검증 시나리오

### 시나리오 14.1: 정렬 순서 불변식
**목적:** INV-6 검증

**실행 단계:**
1. 랜덤 삽입/삭제 1000회
2. 모든 리프 순회하며 정렬 확인

**예상 결과:**
- 모든 리프: ∀i < j: keys[i] < keys[j]

---

### 시나리오 14.2: 부모-자식 일관성
**목적:** Internal의 separator key와 자식 범위 일치

**실행 단계:**
1. 트리 전체 검증
2. 각 Internal의 separator와 자식 범위 확인

**예상 결과:**
- separator[i-1] <= child[i]의 모든 키 < separator[i]

---

## 테스트 코드 작성 가이드

### JUnit 5 테스트 클래스 구조

```java
package com.fxstore.btree;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BTreeLeaf 테스트")
class BTreeLeafTest {
    
    private PageSize pageSize;
    private Codec<String> codec;
    private BTreeLeaf leaf;
    
    @BeforeEach
    void setUp() {
        pageSize = PageSize.PAGE_4K;
        codec = StringCodec.INSTANCE;
        leaf = new BTreeLeaf(pageSize, codec);
    }
    
    @Nested
    @DisplayName("시나리오 그룹 1: 기본 연산")
    class BasicOperations {
        
        @Test
        @DisplayName("시나리오 1.1: 빈 리프 노드 생성")
        void testEmptyLeafCreation() {
            // Given (전제 조건 - setUp에서 처리)
            
            // When
            int entryCount = leaf.getEntryCount();
            int freeSpace = leaf.getFreeSpace();
            boolean isEmpty = leaf.isEmpty();
            
            // Then
            assertEquals(0, entryCount);
            assertTrue(freeSpace >= 4000);  // ~4064 예상
            assertTrue(isEmpty);
        }
        
        @Test
        @DisplayName("시나리오 1.2: 단일 키 삽입")
        void testSingleKeyInsertion() {
            // Given
            byte[] key = codec.encode("apple");
            long valueRecordId = 12345L;
            
            // When
            leaf.insert(0, key, valueRecordId);
            int idx = leaf.find(key, codec.getComparator());
            
            // Then
            assertEquals(0, idx);
            assertArrayEquals(key, leaf.getKey(0));
            assertEquals(12345L, leaf.getValueRecordId(0));
            assertEquals(1, leaf.getEntryCount());
        }
    }
}
```

---

## 커버리지 목표

- **라인 커버리지:** 95% 이상
- **브랜치 커버리지:** 90% 이상
- **모든 public 메서드:** 100% 테스트
- **경계 조건:** 100% 테스트
- **예외 경로:** 100% 테스트

---

## 회귀 테스트 체크리스트

Phase 3 완료 후 다음 테스트 모두 통과해야 함:

- [ ] Phase 0 테스트 (유틸리티)
- [ ] Phase 1 테스트 (코덱)
- [ ] Phase 2 테스트 (Storage, Page)
- [ ] Phase 3 테스트 (B+Tree) ← 신규
- [ ] 통합 테스트
- [ ] 성능 벤치마크

---

**모든 시나리오를 테스트 코드로 구현한 후에만 구현을 시작합니다.**
**타협은 없습니다. A+ 또는 다시 하기.**
