# Phase 5 테스트 시나리오: Map/Set/Deque 컬렉션

> **Phase**: 5 - Map/Set/Deque 컬렉션 구현  
> **목표**: NavigableMap, NavigableSet, Deque Java 표준 인터페이스 구현  
> **작성일**: 2024-12-25

[← 목차로 돌아가기](00.index.md)

---

## 테스트 작성 원칙 (강제 지침)

**⚠️ 모든 코드 작성 후 반드시 아래 시나리오대로 테스트 코드를 작성해야 합니다.**

1. ✅ **Given-When-Then 패턴** 사용
2. ✅ **명확한 테스트 이름** (예: `put_singleKey_shouldStoreValue`)
3. ✅ **독립적 테스트** (테스트 간 상태 공유 금지)
4. ✅ **경계값 테스트** 포함
5. ✅ **예외 케이스 테스트** 필수
6. ✅ **null 검증** 필수
7. ✅ **모든 테스트 통과** 전까지 다음 단계 금지

---

## Week 1: NavigableMap / NavigableSet

### TS-5.1: FxNavigableMap 기본 연산

#### 시나리오 5.1.1: put과 get 기본 동작
```
Given: 빈 FxNavigableMap<Long, String>
When: map.put(1L, "apple")
Then: map.get(1L) should return "apple"
      map.size() should return 1
      map.isEmpty() should return false
```

**테스트 코드**:
```java
@Test
public void put_singleKey_shouldStoreAndRetrieve() {
    // Given
    FxNavigableMap<Long, String> map = createMap();
    
    // When
    String oldValue = map.put(1L, "apple");
    
    // Then
    assertNull(oldValue);
    assertEquals("apple", map.get(1L));
    assertEquals(1, map.size());
    assertFalse(map.isEmpty());
}
```

#### 시나리오 5.1.2: 기존 키 덮어쓰기 (replace)
```
Given: map = {1 → "apple"}
When: map.put(1L, "banana")
Then: 반환값 should be "apple"
      map.get(1L) should return "banana"
      map.size() should remain 1
```

**테스트 코드**:
```java
@Test
public void put_existingKey_shouldReplaceValue() {
    // Given
    FxNavigableMap<Long, String> map = createMap();
    map.put(1L, "apple");
    
    // When
    String oldValue = map.put(1L, "banana");
    
    // Then
    assertEquals("apple", oldValue);
    assertEquals("banana", map.get(1L));
    assertEquals(1, map.size());
}
```

#### 시나리오 5.1.3: null 키/값 거부
```
Given: 빈 map
When: map.put(null, "value")
Then: NullPointerException

When: map.put(1L, null)
Then: NullPointerException
```

**테스트 코드**:
```java
@Test(expected = NullPointerException.class)
public void put_nullKey_shouldThrowNPE() {
    FxNavigableMap<Long, String> map = createMap();
    map.put(null, "value");
}

@Test(expected = NullPointerException.class)
public void put_nullValue_shouldThrowNPE() {
    FxNavigableMap<Long, String> map = createMap();
    map.put(1L, null);
}
```

#### 시나리오 5.1.4: remove 동작
```
Given: map = {1 → "apple", 2 → "banana"}
When: map.remove(1L)
Then: 반환값 should be "apple"
      map.get(1L) should return null
      map.size() should be 1
      map.containsKey(1L) should be false
```

**테스트 코드**:
```java
@Test
public void remove_existingKey_shouldRemoveAndReturnValue() {
    // Given
    FxNavigableMap<Long, String> map = createMap();
    map.put(1L, "apple");
    map.put(2L, "banana");
    
    // When
    String removed = map.remove(1L);
    
    // Then
    assertEquals("apple", removed);
    assertNull(map.get(1L));
    assertEquals(1, map.size());
    assertFalse(map.containsKey(1L));
}

@Test
public void remove_nonExistingKey_shouldReturnNull() {
    FxNavigableMap<Long, String> map = createMap();
    String removed = map.remove(999L);
    assertNull(removed);
}
```

---

### TS-5.2: FxNavigableMap 정렬 및 Navigable 메서드

#### 시나리오 5.2.1: firstKey / lastKey
```
Given: map = {3 → "c", 1 → "a", 2 → "b"}
Then: map.firstKey() should return 1L
      map.lastKey() should return 3L
```

**테스트 코드**:
```java
@Test
public void firstLastKey_unsortedInsert_shouldReturnSortedKeys() {
    // Given
    FxNavigableMap<Long, String> map = createMap();
    map.put(3L, "c");
    map.put(1L, "a");
    map.put(2L, "b");
    
    // Then
    assertEquals(Long.valueOf(1L), map.firstKey());
    assertEquals(Long.valueOf(3L), map.lastKey());
}

@Test(expected = NoSuchElementException.class)
public void firstKey_emptyMap_shouldThrowNoSuchElement() {
    FxNavigableMap<Long, String> map = createMap();
    map.firstKey();
}
```

#### 시나리오 5.2.2: lowerKey / floorKey / ceilingKey / higherKey
```
Given: map = {10 → "a", 20 → "b", 30 → "c"}

When: lowerKey(15)
Then: return 10

When: floorKey(20)
Then: return 20

When: ceilingKey(25)
Then: return 30

When: higherKey(20)
Then: return 30

When: lowerKey(5)
Then: return null
```

**테스트 코드**:
```java
@Test
public void navigable_methods_shouldReturnCorrectKeys() {
    // Given
    FxNavigableMap<Long, String> map = createMap();
    map.put(10L, "a");
    map.put(20L, "b");
    map.put(30L, "c");
    
    // Then
    assertEquals(Long.valueOf(10L), map.lowerKey(15L));
    assertEquals(Long.valueOf(20L), map.floorKey(20L));
    assertEquals(Long.valueOf(30L), map.ceilingKey(25L));
    assertEquals(Long.valueOf(30L), map.higherKey(20L));
    assertNull(map.lowerKey(5L));
    assertNull(map.higherKey(100L));
}
```

---

### TS-5.3: FxNavigableSet 기본 연산

#### 시나리오 5.3.1: add와 contains
```
Given: 빈 FxNavigableSet<String>
When: set.add("apple")
Then: set.contains("apple") should return true
      set.size() should be 1
      set.add("apple") should return false (중복)
```

**테스트 코드**:
```java
@Test
public void add_newElement_shouldStoreAndContain() {
    // Given
    FxNavigableSet<String> set = createSet();
    
    // When
    boolean added = set.add("apple");
    
    // Then
    assertTrue(added);
    assertTrue(set.contains("apple"));
    assertEquals(1, set.size());
}

@Test
public void add_duplicateElement_shouldReturnFalse() {
    FxNavigableSet<String> set = createSet();
    set.add("apple");
    
    boolean addedAgain = set.add("apple");
    
    assertFalse(addedAgain);
    assertEquals(1, set.size());
}
```

#### 시나리오 5.3.2: remove
```
Given: set = {"apple", "banana"}
When: set.remove("apple")
Then: set.contains("apple") should be false
      set.size() should be 1
```

**테스트 코드**:
```java
@Test
public void remove_existingElement_shouldRemove() {
    // Given
    FxNavigableSet<String> set = createSet();
    set.add("apple");
    set.add("banana");
    
    // When
    boolean removed = set.remove("apple");
    
    // Then
    assertTrue(removed);
    assertFalse(set.contains("apple"));
    assertEquals(1, set.size());
}
```

#### 시나리오 5.3.3: first / last
```
Given: set = {"cherry", "apple", "banana"}
Then: set.first() should return "apple"
      set.last() should return "cherry"
```

**테스트 코드**:
```java
@Test
public void firstLast_unsortedAdd_shouldReturnSortedElements() {
    FxNavigableSet<String> set = createSet();
    set.add("cherry");
    set.add("apple");
    set.add("banana");
    
    assertEquals("apple", set.first());
    assertEquals("cherry", set.last());
}
```

---

## Week 2: Deque 및 커밋 모드

### TS-5.4: FxDeque 기본 연산

#### 시나리오 5.4.1: addFirst / addLast / getFirst / getLast
```
Given: 빈 FxDeque<String>
When: deque.addFirst("A")
      deque.addLast("B")
      deque.addFirst("C")
Then: deque.getFirst() should be "C"
      deque.getLast() should be "B"
      deque.size() should be 3
```

**테스트 코드**:
```java
@Test
public void addFirstLast_multipleElements_shouldMaintainOrder() {
    // Given
    FxDeque<String> deque = createDeque();
    
    // When
    deque.addFirst("A");
    deque.addLast("B");
    deque.addFirst("C");
    
    // Then
    assertEquals("C", deque.getFirst());
    assertEquals("B", deque.getLast());
    assertEquals(3, deque.size());
}
```

#### 시나리오 5.4.2: removeFirst / removeLast
```
Given: deque = ["C", "A", "B"] (앞→뒤 순서)
When: deque.removeFirst()
Then: 반환값 should be "C"
      deque.getFirst() should be "A"
      deque.size() should be 2

When: deque.removeLast()
Then: 반환값 should be "B"
      deque.getLast() should be "A"
      deque.size() should be 1
```

**테스트 코드**:
```java
@Test
public void removeFirstLast_shouldRemoveFromEnds() {
    // Given
    FxDeque<String> deque = createDeque();
    deque.addFirst("A");
    deque.addLast("B");
    deque.addFirst("C");
    
    // When & Then
    assertEquals("C", deque.removeFirst());
    assertEquals("A", deque.getFirst());
    assertEquals(2, deque.size());
    
    assertEquals("B", deque.removeLast());
    assertEquals("A", deque.getLast());
    assertEquals(1, deque.size());
}
```

#### 시나리오 5.4.3: 빈 Deque에서 remove
```
Given: 빈 deque
When: deque.removeFirst()
Then: NoSuchElementException

When: deque.pollFirst()
Then: return null
```

**테스트 코드**:
```java
@Test(expected = NoSuchElementException.class)
public void removeFirst_emptyDeque_shouldThrowNoSuchElement() {
    FxDeque<String> deque = createDeque();
    deque.removeFirst();
}

@Test
public void pollFirst_emptyDeque_shouldReturnNull() {
    FxDeque<String> deque = createDeque();
    assertNull(deque.pollFirst());
}
```

#### 시나리오 5.4.4: 시퀀스 번호 관리
```
Given: 빈 deque (headSeq=0, tailSeq=-1)
When: deque.addFirst("A")
Then: headSeq should be -1
      BTree should contain key=-1 → "A"

When: deque.addLast("B")
Then: tailSeq should be 0
      BTree should contain key=0 → "B"

When: deque.removeFirst()
Then: headSeq should be 0
      BTree key=-1 should be deleted
```

**테스트 코드**:
```java
@Test
public void sequenceManagement_shouldMaintainCorrectSeq() {
    // Given
    FxDeque<String> deque = createDeque();
    
    // When: addFirst
    deque.addFirst("A");
    
    // Then
    // headSeq=-1, tailSeq=-1 (내부 검증)
    assertEquals("A", deque.getFirst());
    assertEquals("A", deque.getLast());
    
    // When: addLast
    deque.addLast("B");
    
    // Then
    // headSeq=-1, tailSeq=0
    assertEquals("A", deque.getFirst());
    assertEquals("B", deque.getLast());
    assertEquals(2, deque.size());
}
```

---

### TS-5.5: AUTO 커밋 모드

#### 시나리오 5.5.1: AUTO 모드에서 즉시 반영
```
Given: FxStore in AUTO mode
      Map<Long, String> map1 = store.createMap("m1", ...)
When: map1.put(1L, "value")
Then: 즉시 CommitHeader에 반영됨
      다른 인스턴스에서 open 시 값이 보임
```

**테스트 코드**:
```java
@Test
public void autoMode_putOperation_shouldCommitImmediately() throws Exception {
    // Given
    Path file = tempDir.resolve("auto.fx");
    FxStore store1 = FxStore.open(file, FxOptions.defaults()
            .withCommitMode(CommitMode.AUTO));
    FxNavigableMap<Long, String> map = store1.createMap("m1", Long.class, String.class);
    
    // When
    map.put(1L, "value");
    store1.close();
    
    // Then: 다른 인스턴스에서 확인
    FxStore store2 = FxStore.open(file);
    FxNavigableMap<Long, String> map2 = store2.openMap("m1", Long.class, String.class);
    assertEquals("value", map2.get(1L));
    store2.close();
}
```

#### 시나리오 5.5.2: AUTO 모드에서 commit() 호출은 무시
```
Given: FxStore in AUTO mode
When: store.commit()
Then: no error (no-op)
```

**테스트 코드**:
```java
@Test
public void autoMode_commitCall_shouldBeNoOp() {
    FxStore store = FxStore.openMemory(FxOptions.defaults()
            .withCommitMode(CommitMode.AUTO));
    
    // No exception expected
    store.commit();
    store.rollback();
}
```

---

### TS-5.6: BATCH 커밋 모드

#### 시나리오 5.6.1: BATCH 모드에서 pending 누적
```
Given: FxStore in BATCH mode
      Map<Long, String> map = store.createMap("m1", ...)
When: map.put(1L, "a")
      map.put(2L, "b")
Then: 동일 Store 인스턴스에서는 값이 보임
      CommitHeader는 아직 갱신 안 됨
```

**테스트 코드**:
```java
@Test
public void batchMode_pendingChanges_shouldBeVisibleLocally() {
    // Given
    FxStore store = FxStore.openMemory(FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH));
    FxNavigableMap<Long, String> map = store.createMap("m1", Long.class, String.class);
    
    // When
    map.put(1L, "a");
    map.put(2L, "b");
    
    // Then: 로컬에서는 보임
    assertEquals("a", map.get(1L));
    assertEquals("b", map.get(2L));
}
```

#### 시나리오 5.6.2: BATCH 모드에서 commit()
```
Given: BATCH mode, pending changes exist
When: store.commit()
Then: CommitHeader 갱신됨
      다른 인스턴스에서 변경 사항 보임
```

**테스트 코드**:
```java
@Test
public void batchMode_commit_shouldPersistChanges() throws Exception {
    // Given
    Path file = tempDir.resolve("batch.fx");
    FxStore store1 = FxStore.open(file, FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH));
    FxNavigableMap<Long, String> map = store1.createMap("m1", Long.class, String.class);
    map.put(1L, "a");
    
    // When
    store1.commit();
    store1.close();
    
    // Then
    FxStore store2 = FxStore.open(file);
    FxNavigableMap<Long, String> map2 = store2.openMap("m1", Long.class, String.class);
    assertEquals("a", map2.get(1L));
    store2.close();
}
```

#### 시나리오 5.6.3: BATCH 모드에서 rollback()
```
Given: BATCH mode, pending changes exist
When: store.rollback()
Then: pending 변경사항 폐기됨
      이전 커밋 상태로 복원
```

**테스트 코드**:
```java
@Test
public void batchMode_rollback_shouldDiscardPendingChanges() {
    // Given
    FxStore store = FxStore.openMemory(FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH));
    FxNavigableMap<Long, String> map = store.createMap("m1", Long.class, String.class);
    map.put(1L, "committed");
    store.commit();
    
    // When
    map.put(2L, "pending");
    map.put(1L, "modified");
    store.rollback();
    
    // Then
    assertEquals("committed", map.get(1L));
    assertFalse(map.containsKey(2L));
}
```

#### 시나리오 5.6.4: OnClosePolicy 처리
```
Given: BATCH mode, OnClosePolicy.ERROR, pending changes exist
When: store.close()
Then: FxException(ILLEGAL_ARGUMENT) 발생
      리소스는 해제됨

Given: BATCH mode, OnClosePolicy.COMMIT, pending changes exist
When: store.close()
Then: 자동 commit 후 close

Given: BATCH mode, OnClosePolicy.ROLLBACK, pending changes exist
When: store.close()
Then: 자동 rollback 후 close
```

**테스트 코드**:
```java
@Test(expected = FxException.class)
public void batchMode_closeWithPending_errorPolicy_shouldThrow() {
    FxStore store = FxStore.openMemory(FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH)
            .withOnClosePolicy(OnClosePolicy.ERROR));
    FxNavigableMap<Long, String> map = store.createMap("m1", Long.class, String.class);
    map.put(1L, "pending");
    
    store.close(); // Should throw
}

@Test
public void batchMode_closeWithPending_commitPolicy_shouldCommit() throws Exception {
    Path file = tempDir.resolve("batch-commit-close.fx");
    FxStore store1 = FxStore.open(file, FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH)
            .withOnClosePolicy(OnClosePolicy.COMMIT));
    FxNavigableMap<Long, String> map = store1.createMap("m1", Long.class, String.class);
    map.put(1L, "auto-commit");
    
    store1.close(); // Auto-commit
    
    FxStore store2 = FxStore.open(file);
    FxNavigableMap<Long, String> map2 = store2.openMap("m1", Long.class, String.class);
    assertEquals("auto-commit", map2.get(1L));
    store2.close();
}

@Test
public void batchMode_closeWithPending_rollbackPolicy_shouldRollback() throws Exception {
    Path file = tempDir.resolve("batch-rollback-close.fx");
    FxStore store1 = FxStore.open(file, FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH)
            .withOnClosePolicy(OnClosePolicy.ROLLBACK));
    FxNavigableMap<Long, String> map = store1.createMap("m1", Long.class, String.class);
    map.put(1L, "committed");
    store1.commit();
    
    map.put(2L, "pending");
    store1.close(); // Auto-rollback
    
    FxStore store2 = FxStore.open(file);
    FxNavigableMap<Long, String> map2 = store2.openMap("m1", Long.class, String.class);
    assertEquals("committed", map2.get(1L));
    assertFalse(map2.containsKey(2L));
    store2.close();
}
```

---

## 통합 시나리오

### TS-5.7: Map + Deque + 커밋 모드 통합

#### 시나리오 5.7.1: 여러 컬렉션 동시 사용
```
Given: FxStore with BATCH mode
When: map.put(1L, "a")
      set.add("apple")
      deque.addFirst("x")
      store.commit()
Then: 모든 변경사항이 함께 커밋됨
```

**테스트 코드**:
```java
@Test
public void multipleCollections_batchCommit_shouldCommitAll() {
    // Given
    FxStore store = FxStore.openMemory(FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH));
    FxNavigableMap<Long, String> map = store.createMap("m1", Long.class, String.class);
    FxNavigableSet<String> set = store.createSet("s1", String.class);
    FxDeque<String> deque = store.createDeque("d1", String.class);
    
    // When
    map.put(1L, "a");
    set.add("apple");
    deque.addFirst("x");
    store.commit();
    
    // Then
    assertEquals("a", map.get(1L));
    assertTrue(set.contains("apple"));
    assertEquals("x", deque.getFirst());
}
```

---

## 회귀 테스트

### TS-5.8: 기존 Phase 회귀

#### 시나리오 5.8.1: Phase 0-4 전체 테스트 재실행
```
Given: Phase 5 구현 완료
When: Phase 0, 1, 2, 3, 4 모든 테스트 실행
Then: 모든 테스트 통과해야 함
```

**실행 명령**:
```bash
./gradlew test --tests "com.fxstore.*"
```

---

## 품질 기준 체크리스트

Phase 5 완료 시 모든 항목이 ✅여야 합니다:

- [ ] **TS-5.1**: FxNavigableMap 기본 연산 (10개 테스트)
- [ ] **TS-5.2**: NavigableMap 정렬 메서드 (5개 테스트)
- [ ] **TS-5.3**: FxNavigableSet 연산 (5개 테스트)
- [ ] **TS-5.4**: FxDeque 연산 (7개 테스트)
- [ ] **TS-5.5**: AUTO 모드 (2개 테스트)
- [ ] **TS-5.6**: BATCH 모드 (5개 테스트)
- [ ] **TS-5.7**: 통합 시나리오 (1개 테스트)
- [ ] **TS-5.8**: 회귀 테스트 (전체)
- [ ] **모든 테스트 100% 통과**
- [ ] **커버리지 95% 이상** (Phase 5 코드)
- [ ] **7가지 품질 기준 모두 A+**

---

## 다음 단계

Phase 5 완료 후:
1. ✅ 모든 테스트 통과 확인
2. ✅ 커버리지 측정
3. ✅ 품질 평가 수행
4. ✅ 평가 결과 문서화
5. → **Phase 6 진행** (List/OST 구현)

---

**작성자**: AI Assistant  
**검토**: Phase 5 구현팀  
**승인**: ✅ 준비 완료
