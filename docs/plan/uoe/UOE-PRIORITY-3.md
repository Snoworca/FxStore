# UOE 개선 3순위: 범위 뷰 수정/Poll/중첩 (36개)

> **문서 버전:** 1.0
> **우선순위:** ⭐ (일반)
> **예상 기간:** 3-5일
> **복잡도:** 중간
> **작성일:** 2025-12-29

---

## 목차

1. [개요](#개요)
2. [대상 UOE 목록](#대상-uoe-목록)
3. [구현 계획](#구현-계획)
4. [상세 구현 가이드](#상세-구현-가이드)
5. [테스트 시나리오](#테스트-시나리오)
6. [품질 기준](#품질-기준)
7. [체크리스트](#체크리스트)

---

## 개요

### 목표

범위 뷰(SubMapView, HeadMapView, TailMapView, SubSetView, HeadSetView, TailSetView)의 수정 연산 및 Poll 연산, 중첩 뷰 생성을 구현합니다.

### 핵심 과제

1. **범위 검증**: 수정할 키/요소가 뷰의 범위 내에 있는지 검증
2. **Poll 연산**: 조회 + 삭제 조합으로 구현
3. **중첩 뷰**: 경계 조건 교집합 계산

### 복잡도가 높은 이유

- **범위 검증 로직 필요**: 경계 조건(inclusive/exclusive) 처리
- **IllegalArgumentException 조건**: 범위 밖 키로 수정 시 예외 발생
- **경계 조합 계산**: 중첩 뷰 생성 시 경계 교집합 계산

---

## 대상 UOE 목록

### B-1. 범위 뷰 수정 연산 (12개)

| 클래스 | 메서드 | 현재 상태 | 복잡도 |
|--------|--------|----------|--------|
| SubMapView | `put(K, V)` | UOE | 중간 |
| SubMapView | `remove(Object)` | UOE | 낮음 |
| HeadMapView | `put(K, V)` | UOE | 중간 |
| HeadMapView | `remove(Object)` | UOE | 낮음 |
| TailMapView | `put(K, V)` | UOE | 중간 |
| TailMapView | `remove(Object)` | UOE | 낮음 |
| SubSetView | `add(E)` | UOE | 중간 |
| SubSetView | `remove(Object)` | UOE | 낮음 |
| HeadSetView | `add(E)` | UOE | 중간 |
| HeadSetView | `remove(Object)` | UOE | 낮음 |
| TailSetView | `add(E)` | UOE | 중간 |
| TailSetView | `remove(Object)` | UOE | 낮음 |

### B-2. Poll 연산 (12개)

| 클래스 | 메서드 | 현재 상태 |
|--------|--------|----------|
| SubMapView | `pollFirstEntry()` | UOE |
| SubMapView | `pollLastEntry()` | UOE |
| HeadMapView | `pollFirstEntry()` | UOE |
| HeadMapView | `pollLastEntry()` | UOE |
| TailMapView | `pollFirstEntry()` | UOE |
| TailMapView | `pollLastEntry()` | UOE |
| SubSetView | `pollFirst()` | UOE |
| SubSetView | `pollLast()` | UOE |
| HeadSetView | `pollFirst()` | UOE |
| HeadSetView | `pollLast()` | UOE |
| TailSetView | `pollFirst()` | UOE |
| TailSetView | `pollLast()` | UOE |

### B-3. 중첩 뷰 생성 (12개)

| 클래스 | 메서드 | 현재 상태 |
|--------|--------|----------|
| SubMapView | `subMap(from, to)` | UOE |
| SubMapView | `headMap(to)` | UOE |
| SubMapView | `tailMap(from)` | UOE |
| HeadMapView | `subMap(from, to)` | UOE |
| HeadMapView | `headMap(to)` | UOE |
| HeadMapView | `tailMap(from)` | UOE |
| TailMapView | `subMap(from, to)` | UOE |
| TailMapView | `headMap(to)` | UOE |
| TailMapView | `tailMap(from)` | UOE |
| SubSetView | `subSet(from, to)` | UOE |
| SubSetView | `headSet(to)` | UOE |
| SubSetView | `tailSet(from)` | UOE |

---

## 구현 계획

### Day 1: 범위 검증 유틸리티

| 작업 | 설명 |
|------|------|
| inRange(K key) 메서드 | 키가 뷰 범위 내인지 검사 |
| checkInRange(K key) 메서드 | 범위 밖이면 IllegalArgumentException |
| compare(K k1, K k2) 메서드 | Comparator 사용 비교 |

### Day 2: Map View 수정 연산

| 클래스 | 작업 |
|--------|------|
| SubMapView | put, remove 구현 |
| HeadMapView | put, remove 구현 |
| TailMapView | put, remove 구현 |

### Day 3: Set View 수정 연산

| 클래스 | 작업 |
|--------|------|
| SubSetView | add, remove 구현 |
| HeadSetView | add, remove 구현 |
| TailSetView | add, remove 구현 |

### Day 4: Poll 연산

| 클래스 | 작업 |
|--------|------|
| 모든 Map Views | pollFirstEntry, pollLastEntry |
| 모든 Set Views | pollFirst, pollLast |

### Day 5: 중첩 뷰 및 테스트

| 클래스 | 작업 |
|--------|------|
| 모든 Views | subMap/headMap/tailMap, subSet/headSet/tailSet |
| 테스트 | 단위 테스트, EquivalenceTest, 회귀 테스트 |

---

## 상세 구현 가이드

### 1. 범위 검증 유틸리티

**SubMapView에 추가할 헬퍼 메서드:**

```java
private static class SubMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {

    private final FxNavigableMapImpl<K, V> parent;
    private final K fromKey;
    private final boolean fromInclusive;
    private final K toKey;
    private final boolean toInclusive;

    // === 범위 검증 유틸리티 ===

    /**
     * 키가 범위 내에 있는지 검사
     */
    private boolean inRange(K key) {
        return !tooLow(key) && !tooHigh(key);
    }

    /**
     * 키가 하한보다 낮은지 검사
     */
    private boolean tooLow(K key) {
        int c = compare(key, fromKey);
        return c < 0 || (c == 0 && !fromInclusive);
    }

    /**
     * 키가 상한보다 높은지 검사
     */
    private boolean tooHigh(K key) {
        int c = compare(key, toKey);
        return c > 0 || (c == 0 && !toInclusive);
    }

    /**
     * 범위 밖이면 예외 발생
     */
    private void checkInRange(K key) {
        if (!inRange(key)) {
            throw new IllegalArgumentException("key out of range");
        }
    }

    /**
     * Comparator 사용 비교
     */
    @SuppressWarnings("unchecked")
    private int compare(K k1, K k2) {
        Comparator<? super K> cmp = parent.comparator();
        if (cmp != null) {
            return cmp.compare(k1, k2);
        }
        return ((Comparable<? super K>) k1).compareTo(k2);
    }
}
```

### 2. SubMapView 수정 연산

```java
@Override
public V put(K key, V value) {
    checkInRange(key);
    return parent.put(key, value);
}

@Override
public V remove(Object key) {
    // remove는 범위 밖 키도 허용 (TreeMap 동작과 동일)
    // 범위 밖이면 어차피 없으므로 null 반환
    @SuppressWarnings("unchecked")
    K k = (K) key;
    if (!inRange(k)) {
        return null;  // 범위 밖이면 존재하지 않으므로 null
    }
    return parent.remove(key);
}
```

### 3. HeadMapView 수정 연산

```java
private static class HeadMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {

    private final FxNavigableMapImpl<K, V> parent;
    private final K toKey;
    private final boolean toInclusive;

    private boolean inRange(K key) {
        int c = compare(key, toKey);
        return c < 0 || (c == 0 && toInclusive);
    }

    private void checkInRange(K key) {
        if (!inRange(key)) {
            throw new IllegalArgumentException("key out of range: " + key);
        }
    }

    @Override
    public V put(K key, V value) {
        checkInRange(key);
        return parent.put(key, value);
    }

    @Override
    public V remove(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        if (!inRange(k)) {
            return null;
        }
        return parent.remove(key);
    }
}
```

### 4. TailMapView 수정 연산

```java
private static class TailMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {

    private final FxNavigableMapImpl<K, V> parent;
    private final K fromKey;
    private final boolean fromInclusive;

    private boolean inRange(K key) {
        int c = compare(key, fromKey);
        return c > 0 || (c == 0 && fromInclusive);
    }

    private void checkInRange(K key) {
        if (!inRange(key)) {
            throw new IllegalArgumentException("key out of range: " + key);
        }
    }

    @Override
    public V put(K key, V value) {
        checkInRange(key);
        return parent.put(key, value);
    }

    @Override
    public V remove(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        if (!inRange(k)) {
            return null;
        }
        return parent.remove(key);
    }
}
```

### 5. Poll 연산 구현

**SubMapView:**

```java
@Override
public Entry<K, V> pollFirstEntry() {
    Entry<K, V> first = firstEntry();
    if (first != null) {
        parent.remove(first.getKey());
    }
    return first;
}

@Override
public Entry<K, V> pollLastEntry() {
    Entry<K, V> last = lastEntry();
    if (last != null) {
        parent.remove(last.getKey());
    }
    return last;
}
```

**SubSetView:**

```java
@Override
public E pollFirst() {
    E first = first();
    if (first != null) {
        parent.remove(first);
    }
    return first;
}

@Override
public E pollLast() {
    E last = last();
    if (last != null) {
        parent.remove(last);
    }
    return last;
}
```

**빈 뷰 처리:**

```java
@Override
public E pollFirst() {
    if (isEmpty()) {
        return null;  // NoSuchElementException 대신 null 반환
    }
    E first = first();
    parent.remove(first);
    return first;
}
```

### 6. 중첩 뷰 생성

**SubMapView:**

```java
@Override
public NavigableMap<K, V> subMap(K from, boolean fi, K to, boolean ti) {
    // 새 범위가 현재 범위 내에 있는지 검증
    if (!inRange(from) && !inRange(to)) {
        throw new IllegalArgumentException("range out of bounds");
    }
    // 범위 교집합 계산
    K newFrom = tooLow(from) ? fromKey : from;
    boolean newFi = (compare(from, fromKey) == 0) ? (fi && fromInclusive) : fi;

    K newTo = tooHigh(to) ? toKey : to;
    boolean newTi = (compare(to, toKey) == 0) ? (ti && toInclusive) : ti;

    return parent.subMap(newFrom, newFi, newTo, newTi);
}

@Override
public NavigableMap<K, V> headMap(K to, boolean inclusive) {
    if (tooHigh(to)) {
        throw new IllegalArgumentException("toKey out of range");
    }
    return parent.subMap(fromKey, fromInclusive, to, inclusive);
}

@Override
public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
    if (tooLow(from)) {
        throw new IllegalArgumentException("fromKey out of range");
    }
    return parent.subMap(from, inclusive, toKey, toInclusive);
}
```

**HeadMapView:**

```java
@Override
public NavigableMap<K, V> subMap(K from, boolean fi, K to, boolean ti) {
    if (!inRange(to) && compare(to, toKey) > 0) {
        throw new IllegalArgumentException("toKey out of range");
    }
    return parent.subMap(from, fi, to, ti);
}

@Override
public NavigableMap<K, V> headMap(K to, boolean inclusive) {
    // 새 상한이 현재 상한보다 크면 현재 상한 사용
    if (compare(to, toKey) > 0 || (compare(to, toKey) == 0 && inclusive && !toInclusive)) {
        throw new IllegalArgumentException("toKey out of range");
    }
    return parent.headMap(to, inclusive);
}

@Override
public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
    // tailMap은 하한만 지정, 상한은 현재 HeadMap의 toKey 유지
    if (compare(from, toKey) > 0 || (compare(from, toKey) == 0 && !toInclusive)) {
        throw new IllegalArgumentException("fromKey out of range");
    }
    return parent.subMap(from, inclusive, toKey, toInclusive);
}
```

---

## 테스트 시나리오

### 시나리오 1: SubMap.put() 범위 내

```java
@Test
public void subMap_put_inRange_shouldWork() {
    // Given
    map.put(10L, "A");
    map.put(30L, "C");
    NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);

    // When
    subMap.put(20L, "B");

    // Then
    assertEquals("B", map.get(20L));  // 원본에 반영
    assertEquals("B", subMap.get(20L));
}
```

### 시나리오 2: SubMap.put() 범위 밖

```java
@Test(expected = IllegalArgumentException.class)
public void subMap_put_outOfRange_shouldThrow() {
    // Given
    map.put(10L, "A");
    NavigableMap<Long, String> subMap = map.subMap(15L, true, 25L, true);

    // When: 범위 밖 키로 put
    subMap.put(30L, "X");
}
```

### 시나리오 3: SubMap.remove() 범위 밖

```java
@Test
public void subMap_remove_outOfRange_shouldReturnNull() {
    // Given
    map.put(10L, "A");
    map.put(20L, "B");
    NavigableMap<Long, String> subMap = map.subMap(15L, true, 25L, true);

    // When: 범위 밖 키로 remove
    String removed = subMap.remove(10L);

    // Then
    assertNull(removed);  // 범위 밖이므로 null
    assertEquals("A", map.get(10L));  // 원본은 유지
}
```

### 시나리오 4: pollFirstEntry()

```java
@Test
public void subMap_pollFirstEntry_shouldRemoveFirst() {
    // Given
    map.put(10L, "A");
    map.put(20L, "B");
    map.put(30L, "C");
    NavigableMap<Long, String> subMap = map.subMap(15L, true, 35L, true);

    // When
    Entry<Long, String> polled = subMap.pollFirstEntry();

    // Then
    assertEquals(Long.valueOf(20L), polled.getKey());
    assertEquals("B", polled.getValue());
    assertNull(map.get(20L));  // 원본에서 삭제됨
    assertEquals(2, map.size());
}
```

### 시나리오 5: 중첩 뷰 생성

```java
@Test
public void headMap_tailMap_shouldCreateSubMap() {
    // Given
    map.put(10L, "A");
    map.put(20L, "B");
    map.put(30L, "C");
    map.put(40L, "D");
    NavigableMap<Long, String> headMap = map.headMap(35L, true);

    // When: headMap에서 tailMap 호출
    NavigableMap<Long, String> nested = headMap.tailMap(15L, true);

    // Then: 15 <= key <= 35
    assertEquals(2, nested.size());
    assertTrue(nested.containsKey(20L));
    assertTrue(nested.containsKey(30L));
    assertFalse(nested.containsKey(10L));
    assertFalse(nested.containsKey(40L));
}
```

### 시나리오 6: TreeMap 동등성

```java
@Test
public void equivalence_subMap_put_shouldMatchTreeMap() {
    // Given
    TreeMap<Long, String> treeMap = new TreeMap<>();
    treeMap.put(10L, "A");
    treeMap.put(30L, "C");

    map.put(10L, "A");
    map.put(30L, "C");

    // When
    treeMap.subMap(15L, true, 35L, true).put(20L, "B");
    map.subMap(15L, true, 35L, true).put(20L, "B");

    // Then
    assertEquals(treeMap.get(20L), map.get(20L));
}

@Test(expected = IllegalArgumentException.class)
public void equivalence_subMap_put_outOfRange_shouldThrow() {
    // TreeMap도 동일하게 예외 발생
    TreeMap<Long, String> treeMap = new TreeMap<>();
    treeMap.subMap(10L, true, 20L, true).put(30L, "X");
}
```

---

## 품질 기준

### 커버리지 목표

| 항목 | 목표 |
|------|------|
| 수정된 메서드 Line Coverage | ≥ 95% |
| 수정된 메서드 Branch Coverage | ≥ 90% |
| 범위 검증 로직 | 100% |

### SOLID 원칙 검증

| 원칙 | 검증 항목 |
|------|----------|
| **SRP** | 범위 검증 로직 분리 (inRange, tooLow, tooHigh) |
| **OCP** | 새로운 뷰 타입 추가 시 기존 코드 수정 최소화 |
| **LSP** | TreeMap.subMap()과 동일한 예외 조건 |

### 예외 조건 검증

| 조건 | 예상 예외 |
|------|----------|
| put/add with key out of range | IllegalArgumentException |
| subMap with invalid range | IllegalArgumentException |
| poll on empty view | null 반환 (예외 없음) |

---

## 체크리스트

### Day 1 체크리스트

- [ ] SubMapView.inRange() 구현
- [ ] SubMapView.tooLow() 구현
- [ ] SubMapView.tooHigh() 구현
- [ ] SubMapView.checkInRange() 구현
- [ ] SubMapView.compare() 구현
- [ ] HeadMapView 범위 검증 메서드 구현
- [ ] TailMapView 범위 검증 메서드 구현

### Day 2 체크리스트

- [ ] SubMapView.put() 구현
- [ ] SubMapView.remove() 구현
- [ ] HeadMapView.put() 구현
- [ ] HeadMapView.remove() 구현
- [ ] TailMapView.put() 구현
- [ ] TailMapView.remove() 구현

### Day 3 체크리스트

- [ ] SubSetView.add() 구현
- [ ] SubSetView.remove() 구현
- [ ] HeadSetView.add() 구현
- [ ] HeadSetView.remove() 구현
- [ ] TailSetView.add() 구현
- [ ] TailSetView.remove() 구현

### Day 4 체크리스트

- [ ] SubMapView.pollFirstEntry() 구현
- [ ] SubMapView.pollLastEntry() 구현
- [ ] HeadMapView.pollFirstEntry() 구현
- [ ] HeadMapView.pollLastEntry() 구현
- [ ] TailMapView.pollFirstEntry() 구현
- [ ] TailMapView.pollLastEntry() 구현
- [ ] SubSetView.pollFirst() 구현
- [ ] SubSetView.pollLast() 구현
- [ ] HeadSetView.pollFirst()/pollLast() 구현
- [ ] TailSetView.pollFirst()/pollLast() 구현

### Day 5 체크리스트

- [ ] SubMapView.subMap/headMap/tailMap 구현
- [ ] HeadMapView.subMap/headMap/tailMap 구현
- [ ] TailMapView.subMap/headMap/tailMap 구현
- [ ] SubSetView.subSet/headSet/tailSet 구현
- [ ] HeadSetView 중첩 뷰 구현
- [ ] TailSetView 중첩 뷰 구현
- [ ] RangeViewModificationTest.java 작성
- [ ] RangeViewPollTest.java 작성
- [ ] NestedViewTest.java 작성
- [ ] RangeViewEquivalenceTest.java 작성
- [ ] 전체 회귀 테스트 통과

---

## 구현 시 주의사항

### 1. TreeMap 동작 정확히 확인

```java
// TreeMap의 범위 밖 put 동작 확인
TreeMap<Long, String> treeMap = new TreeMap<>();
treeMap.put(10L, "A");

NavigableMap<Long, String> subMap = treeMap.subMap(5L, true, 8L, true);
subMap.put(6L, "B");  // OK - 범위 내
subMap.put(15L, "C"); // IllegalArgumentException!
```

### 2. remove는 예외 없이 null 반환

```java
// TreeMap의 범위 밖 remove 동작
NavigableMap<Long, String> subMap = treeMap.subMap(5L, true, 8L, true);
String result = subMap.remove(15L);  // null 반환, 예외 없음
```

### 3. 빈 뷰에서 poll

```java
NavigableMap<Long, String> emptySubMap = treeMap.subMap(100L, true, 200L, true);
Entry<Long, String> entry = emptySubMap.pollFirstEntry();  // null 반환
```

### 4. 기존 UOE 테스트 업데이트

```java
// 삭제할 테스트
@Test(expected = UnsupportedOperationException.class)
public void subMap_put_shouldThrowUOE() { ... }

// 새로 작성할 테스트
@Test
public void subMap_put_inRange_shouldWork() { ... }

@Test(expected = IllegalArgumentException.class)
public void subMap_put_outOfRange_shouldThrow() { ... }
```

---

[← UOE 개선 인덱스로 돌아가기](UOE-IMPROVEMENT-INDEX.md)
