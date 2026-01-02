# v1.1 버그 수정 계획서

> **문서 버전:** 1.0
> **작성일:** 2025-12-30
> **대상 버전:** v1.1
> **관련 문서:** [VERIFIED-ISSUE-REPORT.md](VERIFIED-ISSUE-REPORT.md), [CONCURRENCY-BUG-FIX-PLAN.md](CONCURRENCY-BUG-FIX-PLAN.md)

---

## 목차

1. [개요](#1-개요)
2. [BUG-V11-001: FxNavigableSetImpl.retainAll() UOE](#2-bug-v11-001-fxnavigablesetimplretainall-uoe)
3. [BUG-V11-002: BTree 리프 분할 이중 쓰기](#3-bug-v11-002-btree-리프-분할-이중-쓰기)
4. [테스트 시나리오](#4-테스트-시나리오)
5. [구현 WBS](#5-구현-wbs)
6. [위험 분석](#6-위험-분석)
7. [품질 체크리스트](#7-품질-체크리스트)

---

## 1. 개요

### 1.1 배경

COMPREHENSIVE-AUDIT-REPORT.md의 75개 이슈를 코드 단위로 전수 검증한 결과, 실제 수정이 필요한 버그 2건을 확인했습니다.

### 1.2 수정 대상

| ID | 심각도 | 파일 | 문제 | 영향 |
|----|--------|------|------|------|
| BUG-V11-001 | **HIGH** | FxNavigableSetImpl.java:155-165 | retainAll()에서 UnsupportedOperationException | 기능 장애 |
| BUG-V11-002 | **MINOR** | BTree.java:370-379 | 리프 분할 시 동일 노드 이중 쓰기 | 성능 저하 |

### 1.3 목표

- BUG-V11-001: `retainAll()` 정상 동작
- BUG-V11-002: 리프 분할 시 단일 쓰기로 최적화
- 기존 테스트 100% 통과 유지
- 신규 테스트 추가 (회귀 방지)

---

## 2. BUG-V11-001: FxNavigableSetImpl.retainAll() UOE

### 2.1 문제 분석

**파일:** `src/main/java/com/snoworca/fxstore/collection/FxNavigableSetImpl.java`
**라인:** 155-165

```java
// 현재 코드 (버그)
@Override
public boolean retainAll(Collection<?> c) {
    boolean modified = false;
    Iterator<E> it = iterator();  // unmodifiable iterator 반환
    while (it.hasNext()) {
        if (!c.contains(it.next())) {
            it.remove();  // UnsupportedOperationException!
            modified = true;
        }
    }
    return modified;
}
```

**근본 원인:**
1. `FxNavigableSetImpl.iterator()` → `map.keySet().iterator()` 호출
2. `FxNavigableMapImpl.keySet()` (라인 376-384)가 `LinkedHashSet` 복사본 반환
3. 복사본의 `iterator().remove()`는 **복사본에서만** 삭제 (원본 영향 없음)
4. 또한 `KeySetView.iterator()` (라인 911-921)는 `Collections.unmodifiableList().iterator()` 반환
5. `unmodifiableList`의 iterator는 `remove()` 지원 안 함 → **UnsupportedOperationException**

### 2.2 해결 방안

**전략:** 삭제할 요소를 먼저 수집 후 `map.remove()` 직접 호출

```java
// 수정 코드
@Override
public boolean retainAll(Collection<?> c) {
    Objects.requireNonNull(c, "Collection cannot be null");

    // 1단계: 삭제할 요소 수집
    List<E> toRemove = new ArrayList<>();
    for (E element : this) {
        if (!c.contains(element)) {
            toRemove.add(element);
        }
    }

    // 2단계: 수집된 요소 삭제 (map.remove() 직접 호출)
    boolean modified = false;
    for (E element : toRemove) {
        if (map.remove(element) != null) {
            modified = true;
        }
    }

    return modified;
}
```

### 2.3 영향 범위

| 클래스 | 메서드 | 수정 필요 |
|--------|--------|-----------|
| FxNavigableSetImpl | retainAll() | **예** |
| FxNavigableSetImpl | removeAll() | 아니오 (이미 정상) |
| SubSetView | retainAll() | **검토 필요** |
| DescendingSetView | retainAll() | **검토 필요** |

### 2.4 전체 영향 범위 (View 클래스 분석)

**분석 결과:** 모든 Set View 클래스가 AbstractSet을 상속하며, retainAll()을 오버라이드하지 않습니다. AbstractSet.retainAll()은 `iterator().remove()`를 호출하므로 **모든 View에서 동일한 버그**가 발생합니다.

| 클래스 | 라인 | iterator() 반환 타입 | retainAll() 오버라이드 | 버그 여부 |
|--------|------|---------------------|----------------------|----------|
| FxNavigableSetImpl | 119 | map.keySet().iterator() | **예 (버그 코드)** | **예** |
| DescendingSetView | 358 | parent.descendingIterator() | 아니오 | **예** |
| SubSetView | 510 | unmodifiableList().iterator() | 아니오 | **예** |
| DescendingSubSetView | 755 | parent.descendingIterator() | 아니오 | **예** |
| HeadSetView | 792 | unmodifiableList().iterator() | 아니오 | **예** |
| DescendingHeadSetView | 1026 | parent.descendingIterator() | 아니오 | **예** |
| TailSetView | 1063 | unmodifiableList().iterator() | 아니오 | **예** |
| DescendingTailSetView | 1297 | parent.descendingIterator() | 아니오 | **예** |

**수정 전략:**

1. **FxNavigableSetImpl.retainAll()**: 직접 수정 (섹션 2.2)
2. **모든 View 클래스**: `retainAll()` 오버라이드 추가
   - 공통 패턴: 삭제할 요소 수집 → parent.remove() 호출

```java
// 모든 View 클래스에 추가할 retainAll() 공통 패턴
@Override
public boolean retainAll(Collection<?> c) {
    Objects.requireNonNull(c, "Collection cannot be null");
    List<E> toRemove = new ArrayList<>();
    for (E element : this) {
        if (!c.contains(element)) {
            toRemove.add(element);
        }
    }
    boolean modified = false;
    for (E element : toRemove) {
        if (remove(element)) {  // 각 View의 remove() 호출
            modified = true;
        }
    }
    return modified;
}
```

**총 수정 필요 메서드:** 8개 클래스의 retainAll()

### 2.5 SOLID 원칙 준수

- **SRP**: retainAll()은 요소 유지/삭제만 담당 ✅
- **OCP**: 새 기능 추가 없이 버그만 수정 ✅
- **LSP**: NavigableSet 계약 준수 (TreeSet과 동등 동작) ✅
- **ISP**: 해당 없음 ✅
- **DIP**: map 인터페이스에만 의존 ✅

---

## 3. BUG-V11-002: BTree 리프 분할 이중 쓰기

### 3.1 문제 분석

**파일:** `src/main/java/com/snoworca/fxstore/btree/BTree.java`
**라인:** 370-379

```java
// 현재 코드 (비효율)
// 왼쪽 리프 저장 (1차)
long leftPageId = allocatePageId();
writeNode(splitResult.leftLeaf, leftPageId);  // 첫 번째 쓰기

// 오른쪽 리프 저장
long rightPageId = allocatePageId();
writeNode(splitResult.rightLeaf, rightPageId);

// 왼쪽 리프의 nextLeaf 연결
splitResult.leftLeaf.setNextLeafPageId(rightPageId);
writeNode(splitResult.leftLeaf, leftPageId);  // 두 번째 쓰기 (동일 위치)
```

**문제점:**
1. `leftLeaf`를 **같은 pageId에 2번 쓰기** (I/O 낭비)
2. `rightPageId` 할당 전에 `leftLeaf` 쓰기 → `nextLeafPageId` 설정 불가
3. 해결을 위해 순서 변경 필요

### 3.2 해결 방안

**전략:** `rightPageId`를 먼저 할당 후 `leftLeaf`에 설정, 한 번만 쓰기

```java
// 수정 코드
// 1. 페이지 ID 선할당
long leftPageId = allocatePageId();
long rightPageId = allocatePageId();

// 2. 왼쪽 리프의 nextLeaf 연결 (쓰기 전에 설정)
splitResult.leftLeaf.setNextLeafPageId(rightPageId);

// 3. 왼쪽 리프 저장 (단일 쓰기)
writeNode(splitResult.leftLeaf, leftPageId);

// 4. 오른쪽 리프 저장
writeNode(splitResult.rightLeaf, rightPageId);

return new InsertResult(true, leftPageId, rightPageId, splitResult.splitKey);
```

### 3.3 변경 사항 상세

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 쓰기 횟수 | 3회 (left, right, left) | 2회 (left, right) |
| 할당 순서 | left → right | left, right 동시 |
| nextLeaf 설정 | 쓰기 후 | 쓰기 전 |
| COW 불변식 | 위반 가능성 | 준수 |

### 3.4 불변식 검증

**INV-C3 (COW 불변식):** 스냅샷은 불변이며, 변경 시 새 페이지 할당

- **변경 전:** `leftLeaf` 쓰기 후 수정 → 같은 위치 재쓰기 (불변식 기술적 위반)
- **변경 후:** `leftLeaf` 완전 구성 후 단일 쓰기 (불변식 준수)

### 3.5 SOLID 원칙 준수

- **SRP**: insertRecursive()의 분할 로직만 수정 ✅
- **OCP**: 기존 인터페이스 변경 없음 ✅
- **LSP**: BTree 동작 동등성 유지 ✅
- **ISP**: 해당 없음 ✅
- **DIP**: Storage 인터페이스에만 의존 ✅

---

## 4. 테스트 시나리오

### 4.1 BUG-V11-001 테스트

#### TC-V11-001-01: retainAll() 기본 동작

```java
@Test
public void testRetainAllBasic() {
    NavigableSet<Integer> set = store.createSet("test", Integer.class);
    set.addAll(Arrays.asList(1, 2, 3, 4, 5));

    boolean modified = set.retainAll(Arrays.asList(2, 4));

    assertTrue(modified);
    assertEquals(2, set.size());
    assertTrue(set.contains(2));
    assertTrue(set.contains(4));
    assertFalse(set.contains(1));
    assertFalse(set.contains(3));
    assertFalse(set.contains(5));
}
```

#### TC-V11-001-02: retainAll() 빈 컬렉션

```java
@Test
public void testRetainAllEmptyCollection() {
    NavigableSet<Integer> set = store.createSet("test", Integer.class);
    set.addAll(Arrays.asList(1, 2, 3));

    boolean modified = set.retainAll(Collections.emptyList());

    assertTrue(modified);
    assertTrue(set.isEmpty());
}
```

#### TC-V11-001-03: retainAll() 모두 유지

```java
@Test
public void testRetainAllKeepAll() {
    NavigableSet<Integer> set = store.createSet("test", Integer.class);
    set.addAll(Arrays.asList(1, 2, 3));

    boolean modified = set.retainAll(Arrays.asList(1, 2, 3, 4, 5));

    assertFalse(modified);
    assertEquals(3, set.size());
}
```

#### TC-V11-001-04: retainAll() null 인자 예외

```java
@Test(expected = NullPointerException.class)
public void testRetainAllNullCollection() {
    NavigableSet<Integer> set = store.createSet("test", Integer.class);
    set.retainAll(null);
}
```

#### TC-V11-001-05: TreeSet 동등성 검증

```java
@Test
public void testRetainAllEquivalenceWithTreeSet() {
    // FxStore Set
    NavigableSet<String> fxSet = store.createSet("test", String.class);
    fxSet.addAll(Arrays.asList("a", "b", "c", "d", "e"));

    // TreeSet (참조 구현)
    TreeSet<String> treeSet = new TreeSet<>(Arrays.asList("a", "b", "c", "d", "e"));

    Collection<String> retain = Arrays.asList("b", "d", "f");

    boolean fxModified = fxSet.retainAll(retain);
    boolean treeModified = treeSet.retainAll(retain);

    assertEquals(treeModified, fxModified);
    assertEquals(treeSet, fxSet);
}
```

#### TC-V11-001-06: SubSetView.retainAll()

```java
@Test
public void testSubSetViewRetainAll() {
    NavigableSet<Integer> set = store.createSet("test", Integer.class);
    set.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

    // SubSet: [3, 8)
    NavigableSet<Integer> subSet = set.subSet(3, true, 8, false);
    assertEquals(5, subSet.size()); // 3, 4, 5, 6, 7

    boolean modified = subSet.retainAll(Arrays.asList(4, 6, 10));

    assertTrue(modified);
    assertEquals(2, subSet.size()); // 4, 6
    assertTrue(subSet.contains(4));
    assertTrue(subSet.contains(6));

    // 원본 Set에도 반영 확인
    assertEquals(7, set.size()); // 1, 2, 4, 6, 8, 9, 10
    assertFalse(set.contains(3));
    assertFalse(set.contains(5));
    assertFalse(set.contains(7));
}
```

#### TC-V11-001-07: HeadSetView.retainAll()

```java
@Test
public void testHeadSetViewRetainAll() {
    NavigableSet<Integer> set = store.createSet("test", Integer.class);
    set.addAll(Arrays.asList(1, 2, 3, 4, 5));

    NavigableSet<Integer> headSet = set.headSet(4, false); // 1, 2, 3

    boolean modified = headSet.retainAll(Arrays.asList(2, 5));

    assertTrue(modified);
    assertEquals(1, headSet.size());
    assertTrue(headSet.contains(2));

    // 원본 Set: 2, 4, 5
    assertEquals(3, set.size());
}
```

#### TC-V11-001-08: DescendingSet.retainAll()

```java
@Test
public void testDescendingSetRetainAll() {
    NavigableSet<Integer> set = store.createSet("test", Integer.class);
    set.addAll(Arrays.asList(1, 2, 3, 4, 5));

    NavigableSet<Integer> descSet = set.descendingSet();

    boolean modified = descSet.retainAll(Arrays.asList(2, 4));

    assertTrue(modified);
    assertEquals(2, descSet.size());

    // 원본에도 반영
    assertEquals(2, set.size());
    assertTrue(set.contains(2));
    assertTrue(set.contains(4));
}
```

### 4.2 BUG-V11-002 테스트

#### TC-V11-002-01: 리프 분할 후 nextLeaf 연결 검증

```java
@Test
public void testLeafSplitNextLeafLink() {
    NavigableMap<Integer, String> map = store.createMap("test", Integer.class, String.class);

    // 리프 용량 초과하도록 충분한 엔트리 삽입
    for (int i = 0; i < 200; i++) {
        map.put(i, "value-" + i);
    }

    // 모든 엔트리가 순서대로 순회되어야 함 (nextLeaf 링크 정상)
    int expected = 0;
    for (Map.Entry<Integer, String> entry : map.entrySet()) {
        assertEquals(Integer.valueOf(expected), entry.getKey());
        expected++;
    }
    assertEquals(200, expected);
}
```

#### TC-V11-002-02: 대량 삽입 후 무결성 검증

```java
@Test
public void testMassInsertIntegrity() {
    NavigableMap<Integer, String> map = store.createMap("test", Integer.class, String.class);

    // 1000개 삽입 (다수의 분할 유발)
    for (int i = 0; i < 1000; i++) {
        map.put(i, "v" + i);
    }

    // 크기 확인
    assertEquals(1000, map.size());

    // 모든 키 존재 확인
    for (int i = 0; i < 1000; i++) {
        assertTrue("Key " + i + " should exist", map.containsKey(i));
        assertEquals("v" + i, map.get(i));
    }
}
```

#### TC-V11-002-03: 스토어 재오픈 후 무결성

```java
@Test
public void testLeafSplitPersistence() throws Exception {
    Path path = tempDir.resolve("split-test.fx");

    // 1. 데이터 삽입
    try (FxStore store1 = FxStore.open(path)) {
        NavigableMap<Integer, String> map = store1.createMap("test", Integer.class, String.class);
        for (int i = 0; i < 500; i++) {
            map.put(i, "value-" + i);
        }
    }

    // 2. 재오픈 후 검증
    try (FxStore store2 = FxStore.open(path)) {
        NavigableMap<Integer, String> map = store2.openMap("test", Integer.class, String.class);
        assertEquals(500, map.size());

        // 순서 검증 (nextLeaf 링크 확인)
        int expected = 0;
        for (Integer key : map.keySet()) {
            assertEquals(Integer.valueOf(expected), key);
            expected++;
        }
    }
}
```

### 4.3 회귀 테스트

```bash
# 전체 회귀 테스트 실행
./gradlew test

# 예상 결과:
# - 기존 테스트: 2,395+ 통과
# - 신규 테스트: 8개 추가
# - 총 실행 시간: < 60초
```

---

## 5. 구현 WBS

### 5.1 일정

| 일차 | 작업 | 산출물 | 예상 시간 |
|------|------|--------|-----------|
| **Day 1 AM** | BUG-V11-001: FxNavigableSetImpl.retainAll() 수정 | FxNavigableSetImpl.java | 30분 |
| **Day 1 AM** | BUG-V11-001: 7개 View 클래스 retainAll() 추가 | FxNavigableSetImpl.java | 1.5시간 |
| **Day 1 PM** | TC-V11-001-* 테스트 작성 (메인 + View 클래스) | RetainAllTest.java | 1.5시간 |
| **Day 1 PM** | BUG-V11-002: BTree 리프 분할 수정 | BTree.java | 30분 |
| **Day 1 PM** | TC-V11-002-* 테스트 작성 | BTreeSplitTest.java | 1시간 |
| **Day 2 AM** | 회귀 테스트 + 커버리지 확인 | 테스트 리포트 | 1시간 |
| **총계** | - | - | **6시간** |

### 5.2 체크리스트

**BUG-V11-001: retainAll() UOE 수정 (8개 클래스)**
- [ ] FxNavigableSetImpl.retainAll() 수정
- [ ] DescendingSetView.retainAll() 오버라이드 추가
- [ ] SubSetView.retainAll() 오버라이드 추가
- [ ] DescendingSubSetView.retainAll() 오버라이드 추가
- [ ] HeadSetView.retainAll() 오버라이드 추가
- [ ] DescendingHeadSetView.retainAll() 오버라이드 추가
- [ ] TailSetView.retainAll() 오버라이드 추가
- [ ] DescendingTailSetView.retainAll() 오버라이드 추가
- [ ] retainAll() 테스트 8개 작성 및 통과 (메인 + 각 View)

**BUG-V11-002: BTree 리프 분할 최적화**
- [ ] BTree.insertRecursive() 리프 분할 단일 쓰기로 수정
- [ ] 리프 분할 테스트 3개 작성 및 통과

**품질 검증**
- [ ] 전체 회귀 테스트 통과 (2,395+ 테스트)
- [ ] 커버리지 유지 (91%+)
- [ ] TreeSet 동등성 테스트 통과

---

## 6. 위험 분석

### 6.1 BUG-V11-001 위험

| 위험 | 확률 | 영향 | 대응 |
|------|------|------|------|
| 다른 Collection 메서드에 유사 버그 | 중 | 중 | removeAll() 등 검토 |
| SubSetView에서 동일 문제 | 중 | 중 | SubSetView.retainAll() 검토 |
| 성능 저하 (대량 삭제 시) | 하 | 하 | 벤치마크로 확인 |

### 6.2 BUG-V11-002 위험

| 위험 | 확률 | 영향 | 대응 |
|------|------|------|------|
| 순서 변경으로 인한 부작용 | 하 | 고 | 철저한 테스트 |
| allocatePageId() 연속 호출 문제 | 극히 낮음 | 중 | Allocator 코드 검토 |
| 기존 테스트 실패 | 하 | 중 | 즉시 롤백 |

### 6.3 완화 조치

1. **점진적 커밋**: 각 버그 수정 후 개별 커밋
2. **회귀 테스트**: 매 수정 후 전체 테스트 실행
3. **TreeSet 동등성 테스트**: 표준 구현과 동작 비교

---

## 7. 품질 체크리스트

### 7.1 Plan-Code 정합성

- [ ] 수정 코드가 설계 문서와 일치
- [ ] 라인 번호, 파일 경로 정확
- [ ] 변경 전/후 코드 스니펫 검증

### 7.2 SOLID 원칙

- [ ] SRP: 단일 책임 유지
- [ ] OCP: 기존 인터페이스 변경 없음
- [ ] LSP: Java Collection 계약 준수
- [ ] ISP: 해당 없음
- [ ] DIP: 추상화 의존 유지

### 7.3 테스트 커버리지

- [ ] 수정 메서드 100% 커버리지
- [ ] Edge case 테스트 포함
- [ ] TreeSet 동등성 테스트 포함

### 7.4 코드 가독성

- [ ] 명확한 변수명 사용
- [ ] 주석으로 의도 설명
- [ ] 일관된 코딩 스타일

### 7.5 예외 처리

- [ ] null 인자 검증
- [ ] 적절한 예외 타입 사용
- [ ] 예외 메시지 명확

### 7.6 성능

- [ ] 불필요한 I/O 제거 (BUG-V11-002)
- [ ] O(n) 복잡도 유지 (BUG-V11-001)
- [ ] 메모리 효율성 유지

### 7.7 문서화

- [ ] JavaDoc 업데이트
- [ ] 변경 이력 기록
- [ ] 테스트 시나리오 문서화

---

## 부록: 관련 파일 목록

| 파일 | 설명 | 수정 여부 |
|------|------|-----------|
| `FxNavigableSetImpl.java` | NavigableSet 구현 | **수정** |
| `BTree.java` | B+Tree 구현 | **수정** |
| `FxNavigableMapImpl.java` | NavigableMap 구현 (참고) | 검토만 |
| `BTreeLeaf.java` | B+Tree 리프 노드 (참고) | 검토만 |
| `RetainAllTest.java` | retainAll 테스트 | **신규** |
| `BTreeSplitTest.java` | 리프 분할 테스트 | **신규** |

---

> **다음 단계:** 이 계획서 승인 후 Day 1 구현 시작
> **예상 완료:** 5시간 (1일 이내)

---

*문서 작성일: 2025-12-30*
