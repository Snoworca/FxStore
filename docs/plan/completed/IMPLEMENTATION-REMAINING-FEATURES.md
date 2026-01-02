# 미구현 기능 구현 계획 (Remaining Features Implementation Plan)

> **문서 버전:** 2.0
> **작성일:** 2025-12-27
> **목표:** 모든 미구현 기능 완료 및 7가지 품질 기준 A+ 달성

[← 목차로 돌아가기](00.index.md)

---

## 목차

- [1. 개요](#1-개요)
- [2. 미구현 항목 분류](#2-미구현-항목-분류)
- [3. 구현 우선순위](#3-구현-우선순위)
- [4. 상세 구현 명세](#4-상세-구현-명세)
- [5. 테스트 계획](#5-테스트-계획)
- [6. 품질 검증 계획](#6-품질-검증-계획)
- [7. 일정 및 마일스톤](#7-일정-및-마일스톤)
- [부록 A: JavaDoc 템플릿](#부록-a-javadoc-템플릿)
- [부록 B: 불변식 검증 체크리스트](#부록-b-불변식-검증-체크리스트)

---

## 1. 개요

### 1.1 배경

Phase 0-7 구현 완료 후 전수조사 결과, 15개 컬렉션 메서드가 `UnsupportedOperationException`을 던지는 상태로 남아있습니다. 본 문서는 이들 미구현 기능의 체계적인 구현 계획을 정의합니다.

### 1.2 목표

| 목표 | 측정 지표 | 달성 기준 |
|------|-----------|-----------|
| 기능 완성 | 미구현 메서드 수 | 0개 |
| 테스트 완비 | 라인 커버리지 | ≥95% |
| 브랜치 커버리지 | 브랜치 커버리지 | ≥90% |
| 품질 보증 | 7가지 품질 기준 | 모두 A+ |

### 1.3 범위

**포함:**
- FxNavigableMapImpl 미구현 메서드 7개
- FxNavigableSetImpl 미구현 메서드 5개
- FxDequeImpl 미구현 메서드 3개
- 테스트 보강 2건

**제외 (설계 결정에 따른 의도적 미지원):**

| 기능 | 미지원 사유 | 대안 |
|------|-------------|------|
| 범위 뷰 쓰기 | COW 일관성 복잡도 | 읽기 전용 뷰 제공 |
| 온라인 컴팩션 | 단일 스레드 설계 | compactTo() 사용 |
| 다중 writer | 동시성 제어 복잡도 | 외부 동기화 |
| BTree merge | 구현 복잡도 | compactTo()로 대체 |
| NumberMode.STRICT | v0.3 범위 제외 | v0.4 검토 |
| 자동 코덱 마이그레이션 | 복잡도 | 수동 마이그레이션 |

---

## 2. 미구현 항목 분류

### 2.1 전체 목록 (15개 메서드)

| # | 클래스 | 메서드 시그니처 | 카테고리 | 복잡도 |
|---|--------|----------------|----------|--------|
| 1 | FxNavigableMapImpl | `void clear()` | A | O(N) |
| 2 | FxNavigableMapImpl | `NavigableMap<K,V> descendingMap()` | C | O(1) |
| 3 | FxNavigableMapImpl | `NavigableSet<K> navigableKeySet()` | C | O(1) |
| 4 | FxNavigableMapImpl | `NavigableSet<K> descendingKeySet()` | C | O(1) |
| 5 | FxNavigableMapImpl | `NavigableMap<K,V> subMap(K,boolean,K,boolean)` | B | O(1) |
| 6 | FxNavigableMapImpl | `NavigableMap<K,V> headMap(K,boolean)` | B | O(1) |
| 7 | FxNavigableMapImpl | `NavigableMap<K,V> tailMap(K,boolean)` | B | O(1) |
| 8 | FxNavigableSetImpl | `NavigableSet<E> descendingSet()` | C | O(1) |
| 9 | FxNavigableSetImpl | `Iterator<E> descendingIterator()` | C | O(N) |
| 10 | FxNavigableSetImpl | `NavigableSet<E> subSet(E,boolean,E,boolean)` | B | O(1) |
| 11 | FxNavigableSetImpl | `NavigableSet<E> headSet(E,boolean)` | B | O(1) |
| 12 | FxNavigableSetImpl | `NavigableSet<E> tailSet(E,boolean)` | B | O(1) |
| 13 | FxDequeImpl | `boolean removeFirstOccurrence(Object)` | D | O(N) |
| 14 | FxDequeImpl | `boolean removeLastOccurrence(Object)` | D | O(N) |
| 15 | FxDequeImpl | `Iterator<E> descendingIterator()` | C | O(1) |

### 2.2 카테고리 정의

| 카테고리 | 설명 | 우선순위 | 메서드 수 |
|----------|------|----------|-----------|
| A | 핵심 기능 (Map.clear) | 높음 | 1 |
| B | 범위 뷰 (읽기 전용) | 중간 | 6 |
| C | Descending 뷰/Iterator | 중간 | 6 |
| D | Deque 검색 | 낮음 | 2 |

---

## 3. 구현 우선순위

### 3.1 의존성 그래프

```
[clear()] ─── 독립
    │
    ▼
[navigableKeySet()] ──┬──► [descendingKeySet()]
                      │
[descendingIterator]──┴──► [descendingSet()]
                           [descendingMap()]
    │
    ▼
[subMap/headMap/tailMap] ──► [subSet/headSet/tailSet]
    │
    ▼
[removeFirst/LastOccurrence] ─── 독립
```

### 3.2 구현 순서

| 순서 | 메서드 | 의존성 | 예상 소요 |
|------|--------|--------|-----------|
| 1 | `clear()` | 없음 | 30분 |
| 2 | `navigableKeySet()` | 없음 | 30분 |
| 3 | `descendingIterator()` (Set, Deque) | 없음 | 1시간 |
| 4 | `descendingKeySet()` | navigableKeySet | 30분 |
| 5 | `descendingSet()` | descendingIterator | 1시간 |
| 6 | `descendingMap()` | descendingKeySet | 1시간 |
| 7 | `subMap()`, `headMap()`, `tailMap()` | 없음 | 3시간 |
| 8 | `subSet()`, `headSet()`, `tailSet()` | subMap 참조 | 2시간 |
| 9 | `removeFirst/LastOccurrence()` | 없음 | 1시간 |
| 10 | 테스트 보강 | 전체 완료 후 | 1시간 |

**총 예상 소요: 약 12시간**

---

## 4. 상세 구현 명세

### 4.1 clear() - FxNavigableMapImpl

**현재 위치:** `FxNavigableMapImpl.java:231`

**현재 코드:**
```java
@Override
public void clear() {
    throw new UnsupportedOperationException("clear() not supported");
}
```

**구현 명세:**

```java
/**
 * 이 맵의 모든 매핑을 제거합니다.
 *
 * <p>시간 복잡도: O(N) - 모든 엔트리 순회 및 삭제
 * <p>공간 복잡도: O(N) - 키 목록 임시 저장
 *
 * <p>불변식 보장:
 * <ul>
 *   <li>INV-3: BTree 구조 무결성 유지</li>
 *   <li>INV-5: 루트 페이지 ID 갱신</li>
 * </ul>
 *
 * @throws FxException IO 오류 발생 시
 */
@Override
public void clear() {
    if (isEmpty()) {
        return;  // 빈 맵은 아무것도 하지 않음
    }

    // 모든 키 수집 (삭제 중 ConcurrentModification 방지)
    List<byte[]> keysToRemove = new ArrayList<>();
    BTreeCursor cursor = btree.cursor();
    while (cursor.hasNext()) {
        keysToRemove.add(cursor.next().getKey().clone());
    }

    // 역순으로 삭제 (트리 밸런스 최적화)
    for (int i = keysToRemove.size() - 1; i >= 0; i--) {
        btree.remove(keysToRemove.get(i));
    }

    markDirty();
}
```

**SOLID 준수 검증:**
- **SRP**: clear()는 "컬렉션 비우기"라는 단일 책임만 수행
- **OCP**: 기존 btree.remove() 재사용, 새 삭제 로직 미추가
- **LSP**: java.util.Map.clear() 계약 완전 준수
- **ISP**: Map 인터페이스의 clear()만 구현
- **DIP**: BTree 추상화에 의존, 구체 구현과 분리

**예외 조건:**
| 조건 | 예외 | 메시지 |
|------|------|--------|
| I/O 오류 | FxException(IO) | "Failed to clear map: {원인}" |
| 스토어 닫힘 | FxException(CLOSED) | "Store is closed" |

**테스트 케이스:**
```java
@Test
public void testClear_emptyMap() {
    map.clear();  // 예외 없이 성공
    assertTrue(map.isEmpty());
    assertEquals(0, map.size());
}

@Test
public void testClear_nonEmptyMap() {
    map.put(1L, "a");
    map.put(2L, "b");
    map.put(3L, "c");

    map.clear();

    assertTrue(map.isEmpty());
    assertEquals(0, map.size());
    assertNull(map.get(1L));
    assertNull(map.get(2L));
    assertNull(map.get(3L));
}

@Test
public void testClear_afterClearCanAddAgain() {
    map.put(1L, "a");
    map.clear();
    map.put(2L, "b");

    assertEquals(1, map.size());
    assertEquals("b", map.get(2L));
}

@Test
public void testClear_multipleTimes() {
    map.put(1L, "a");
    map.clear();
    map.clear();  // 두 번 호출해도 안전
    assertTrue(map.isEmpty());
}
```

---

### 4.2 descendingIterator() - FxNavigableSetImpl

**현재 위치:** `FxNavigableSetImpl.java:224`

**구현 명세:**

```java
/**
 * 이 집합의 요소를 내림차순으로 순회하는 Iterator를 반환합니다.
 *
 * <p>시간 복잡도: O(N) - 전체 요소 수집 필요
 * <p>공간 복잡도: O(N) - 요소 목록 임시 저장
 *
 * <p>Iterator 특성:
 * <ul>
 *   <li>remove() 지원 안 함 (UnsupportedOperationException)</li>
 *   <li>스냅샷 기반: 생성 후 원본 변경 반영 안 됨</li>
 * </ul>
 *
 * @return 내림차순 Iterator
 */
@Override
public Iterator<E> descendingIterator() {
    // 스냅샷 생성 (COW 일관성)
    List<E> snapshot = new ArrayList<>();
    for (E element : this) {
        snapshot.add(element);
    }

    return new Iterator<E>() {
        private int index = snapshot.size() - 1;

        @Override
        public boolean hasNext() {
            return index >= 0;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }
            return snapshot.get(index--);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                "remove() not supported in descending iterator");
        }
    };
}
```

**테스트 케이스:**
```java
@Test
public void testDescendingIterator_emptySet() {
    Iterator<Long> it = set.descendingIterator();
    assertFalse(it.hasNext());
}

@Test
public void testDescendingIterator_singleElement() {
    set.add(42L);
    Iterator<Long> it = set.descendingIterator();

    assertTrue(it.hasNext());
    assertEquals(Long.valueOf(42L), it.next());
    assertFalse(it.hasNext());
}

@Test
public void testDescendingIterator_multipleElements() {
    set.add(1L);
    set.add(3L);
    set.add(2L);

    Iterator<Long> it = set.descendingIterator();
    assertEquals(Long.valueOf(3L), it.next());
    assertEquals(Long.valueOf(2L), it.next());
    assertEquals(Long.valueOf(1L), it.next());
    assertFalse(it.hasNext());
}

@Test(expected = NoSuchElementException.class)
public void testDescendingIterator_exhausted() {
    set.add(1L);
    Iterator<Long> it = set.descendingIterator();
    it.next();
    it.next();  // NoSuchElementException
}

@Test(expected = UnsupportedOperationException.class)
public void testDescendingIterator_removeNotSupported() {
    set.add(1L);
    Iterator<Long> it = set.descendingIterator();
    it.next();
    it.remove();  // UnsupportedOperationException
}
```

---

### 4.3 subMap() - FxNavigableMapImpl

**현재 위치:** `FxNavigableMapImpl.java:485`

**구현 명세:**

```java
/**
 * 이 맵에서 fromKey부터 toKey 범위의 부분 뷰를 반환합니다.
 *
 * <p>반환된 맵은 읽기 전용입니다. put(), remove() 등 수정 연산은
 * {@link UnsupportedOperationException}을 던집니다.
 *
 * <p>시간 복잡도: O(1) - 뷰 생성
 * <p>공간 복잡도: O(1) - 래퍼 객체만 생성
 *
 * <p>뷰 특성:
 * <ul>
 *   <li>라이브 뷰: 원본 맵 변경 시 뷰에 반영</li>
 *   <li>범위 외 키 접근 시 null 반환</li>
 *   <li>읽기 전용: 수정 연산 불가</li>
 * </ul>
 *
 * @param fromKey 범위 시작 키 (null 불가)
 * @param fromInclusive fromKey 포함 여부
 * @param toKey 범위 종료 키 (null 불가)
 * @param toInclusive toKey 포함 여부
 * @return 범위 내 요소만 포함하는 뷰
 * @throws NullPointerException fromKey 또는 toKey가 null인 경우
 * @throws IllegalArgumentException fromKey > toKey인 경우
 */
@Override
public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
                                  K toKey, boolean toInclusive) {
    Objects.requireNonNull(fromKey, "fromKey cannot be null");
    Objects.requireNonNull(toKey, "toKey cannot be null");

    if (comparator().compare(fromKey, toKey) > 0) {
        throw new IllegalArgumentException(
            "fromKey > toKey: " + fromKey + " > " + toKey);
    }

    return new SubMapView<>(this, fromKey, fromInclusive, toKey, toInclusive);
}

/**
 * 읽기 전용 SubMap 뷰 구현.
 */
private static class SubMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {

    private final FxNavigableMapImpl<K, V> parent;
    private final K fromKey, toKey;
    private final boolean fromInclusive, toInclusive;

    SubMapView(FxNavigableMapImpl<K, V> parent,
               K fromKey, boolean fromInclusive,
               K toKey, boolean toInclusive) {
        this.parent = parent;
        this.fromKey = fromKey;
        this.fromInclusive = fromInclusive;
        this.toKey = toKey;
        this.toInclusive = toInclusive;
    }

    /**
     * 키가 범위 내에 있는지 확인합니다.
     */
    private boolean inRange(K key) {
        if (key == null) return false;

        Comparator<? super K> cmp = parent.comparator();
        int fromCmp = cmp.compare(key, fromKey);
        int toCmp = cmp.compare(key, toKey);

        boolean afterFrom = fromInclusive ? fromCmp >= 0 : fromCmp > 0;
        boolean beforeTo = toInclusive ? toCmp <= 0 : toCmp < 0;

        return afterFrom && beforeTo;
    }

    @Override
    public V get(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        if (!inRange(k)) return null;
        return parent.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        return inRange(k) && parent.containsKey(key);
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("SubMap is read-only");
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("SubMap is read-only");
    }

    @Override
    public int size() {
        int count = 0;
        for (K key : parent.keySet()) {
            if (inRange(key)) count++;
        }
        return count;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> result = new LinkedHashSet<>();
        for (Entry<K, V> entry : parent.entrySet()) {
            if (inRange(entry.getKey())) {
                result.add(entry);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    // ... NavigableMap 추가 메서드 구현 (동일 패턴)

    @Override
    public Comparator<? super K> comparator() {
        return parent.comparator();
    }

    @Override
    public K firstKey() {
        for (K key : parent.keySet()) {
            if (inRange(key)) return key;
        }
        throw new NoSuchElementException();
    }

    @Override
    public K lastKey() {
        K last = null;
        for (K key : parent.keySet()) {
            if (inRange(key)) last = key;
        }
        if (last == null) throw new NoSuchElementException();
        return last;
    }

    // NavigableMap 메서드들은 동일한 패턴으로 구현
    // (범위 체크 → 부모 위임 → 결과 반환)
}
```

**테스트 케이스:**
```java
@Test
public void testSubMap_basic() {
    map.put(1L, "a");
    map.put(2L, "b");
    map.put(3L, "c");
    map.put(4L, "d");
    map.put(5L, "e");

    NavigableMap<Long, String> sub = map.subMap(2L, true, 4L, true);

    assertEquals(3, sub.size());
    assertEquals("b", sub.get(2L));
    assertEquals("c", sub.get(3L));
    assertEquals("d", sub.get(4L));
    assertNull(sub.get(1L));  // 범위 외
    assertNull(sub.get(5L));  // 범위 외
}

@Test
public void testSubMap_exclusive() {
    map.put(1L, "a");
    map.put(2L, "b");
    map.put(3L, "c");

    NavigableMap<Long, String> sub = map.subMap(1L, false, 3L, false);

    assertEquals(1, sub.size());
    assertEquals("b", sub.get(2L));
    assertNull(sub.get(1L));  // 제외
    assertNull(sub.get(3L));  // 제외
}

@Test
public void testSubMap_emptyRange() {
    map.put(1L, "a");
    map.put(5L, "b");

    NavigableMap<Long, String> sub = map.subMap(2L, true, 4L, true);

    assertTrue(sub.isEmpty());
    assertEquals(0, sub.size());
}

@Test(expected = UnsupportedOperationException.class)
public void testSubMap_putNotSupported() {
    map.put(1L, "a");
    NavigableMap<Long, String> sub = map.subMap(1L, true, 2L, true);
    sub.put(1L, "modified");  // UnsupportedOperationException
}

@Test(expected = NullPointerException.class)
public void testSubMap_nullFromKey() {
    map.subMap(null, true, 5L, true);
}

@Test(expected = IllegalArgumentException.class)
public void testSubMap_invalidRange() {
    map.subMap(5L, true, 1L, true);  // fromKey > toKey
}

@Test
public void testSubMap_liveView() {
    map.put(1L, "a");
    map.put(2L, "b");

    NavigableMap<Long, String> sub = map.subMap(1L, true, 3L, true);
    assertEquals(2, sub.size());

    // 원본 변경
    map.put(3L, "c");

    // 뷰에 반영 (3L은 toInclusive=true이므로 포함)
    // 단, 범위 외이므로 포함 안 됨
    assertEquals(2, sub.size());
}
```

---

### 4.4 removeFirstOccurrence() - FxDequeImpl

**현재 위치:** `FxDequeImpl.java:276`

**구현 명세:**

```java
/**
 * 지정된 요소의 첫 번째 발생을 제거합니다.
 *
 * <p>시간 복잡도: O(N) - 선형 검색 + 삭제
 * <p>공간 복잡도: O(1)
 *
 * @param o 제거할 요소 (null 가능)
 * @return 요소가 존재하여 제거되었으면 true
 */
@Override
public boolean removeFirstOccurrence(Object o) {
    int index = 0;
    for (E element : this) {
        if (Objects.equals(element, o)) {
            remove(index);
            return true;
        }
        index++;
    }
    return false;
}

/**
 * 지정된 요소의 마지막 발생을 제거합니다.
 *
 * <p>시간 복잡도: O(N) - 전체 순회 + 삭제
 * <p>공간 복잡도: O(1)
 *
 * @param o 제거할 요소 (null 가능)
 * @return 요소가 존재하여 제거되었으면 true
 */
@Override
public boolean removeLastOccurrence(Object o) {
    int lastIndex = -1;
    int currentIndex = 0;

    for (E element : this) {
        if (Objects.equals(element, o)) {
            lastIndex = currentIndex;
        }
        currentIndex++;
    }

    if (lastIndex >= 0) {
        remove(lastIndex);
        return true;
    }
    return false;
}
```

**테스트 케이스:**
```java
@Test
public void testRemoveFirstOccurrence_found() {
    deque.addLast("a");
    deque.addLast("b");
    deque.addLast("a");

    assertTrue(deque.removeFirstOccurrence("a"));
    assertEquals(2, deque.size());
    assertEquals("b", deque.peekFirst());
}

@Test
public void testRemoveFirstOccurrence_notFound() {
    deque.addLast("a");
    assertFalse(deque.removeFirstOccurrence("x"));
    assertEquals(1, deque.size());
}

@Test
public void testRemoveFirstOccurrence_null() {
    deque.addLast("a");
    deque.addLast(null);
    deque.addLast("b");

    assertTrue(deque.removeFirstOccurrence(null));
    assertEquals(2, deque.size());
}

@Test
public void testRemoveLastOccurrence_found() {
    deque.addLast("a");
    deque.addLast("b");
    deque.addLast("a");

    assertTrue(deque.removeLastOccurrence("a"));
    assertEquals(2, deque.size());
    assertEquals("b", deque.peekLast());
}

@Test
public void testRemoveLastOccurrence_singleElement() {
    deque.addLast("a");

    assertTrue(deque.removeLastOccurrence("a"));
    assertTrue(deque.isEmpty());
}
```

---

## 5. 테스트 계획

### 5.1 테스트 커버리지 목표

| 메트릭 | 목표 | 측정 방법 |
|--------|------|-----------|
| 라인 커버리지 | ≥95% | JaCoCo |
| 브랜치 커버리지 | ≥90% | JaCoCo |
| 메서드 커버리지 | 100% | JaCoCo |

### 5.2 테스트 케이스 분류

| 카테고리 | 테스트 유형 | 케이스 수 |
|----------|------------|-----------|
| 정상 경로 | Happy path | 30+ |
| 경계값 | 빈 컬렉션, 단일 요소, 최대 범위 | 20+ |
| 예외 경로 | null, 범위 초과, 지원 안 함 | 15+ |
| 동시성 | 라이브 뷰 반영 | 5+ |

### 5.3 경계값 테스트

| 메서드 | 경계 조건 | 예상 결과 |
|--------|-----------|-----------|
| clear() | 빈 맵 | 예외 없음, size=0 유지 |
| clear() | 1개 요소 | size=0 |
| clear() | 10,000개 요소 | size=0, 성능 ≤1초 |
| subMap() | fromKey = toKey, inclusive | 1개 요소 |
| subMap() | fromKey = toKey, exclusive | 빈 뷰 |
| subMap() | 전체 범위 | 원본과 동일 |
| descendingIterator() | 빈 집합 | hasNext()=false |
| removeFirstOccurrence() | 중복 3개 | 첫 번째만 삭제 |

### 5.4 testMixedCollectionFuzz 보강

```java
@Test
public void testMixedCollectionFuzz_withList() {
    // 4개 컬렉션 생성
    NavigableMap<Long, String> fxMap = store.createOrOpenMap("map", Long.class, String.class);
    NavigableSet<Long> fxSet = store.createOrOpenSet("set", Long.class);
    Deque<String> fxDeque = store.createOrOpenDeque("deque", String.class);
    List<String> fxList = store.createOrOpenList("list", String.class);

    // 참조 구현
    TreeMap<Long, String> refMap = new TreeMap<>();
    TreeSet<Long> refSet = new TreeSet<>();
    LinkedList<String> refDeque = new LinkedList<>();
    ArrayList<String> refList = new ArrayList<>();

    Random random = new Random(SEED);

    for (int i = 0; i < 3000; i++) {
        int collection = random.nextInt(4);  // 0-3 (List 포함)

        switch (collection) {
            case 0: doMapOperation(fxMap, refMap, random, i); break;
            case 1: doSetOperation(fxSet, refSet, random, i); break;
            case 2: doDequeOperation(fxDeque, refDeque, random, i); break;
            case 3: doListOperation(fxList, refList, random, i); break;
        }
    }

    // 최종 검증
    assertMapsEqual(refMap, fxMap);
    assertSetsEqual(refSet, fxSet);
    assertDequesEqual(refDeque, fxDeque);
    assertListsEqual(refList, fxList);
    assertTrue(store.verify().ok());
}
```

### 5.5 파일 영속성 테스트 보강

```java
@Test
public void testFileStore_reopenWithData_verifyAllCollections() throws Exception {
    File file = tempFolder.newFile("persistence.fxs");
    file.delete();

    // 1단계: 모든 컬렉션 타입 생성 및 데이터 추가
    FxStore store1 = FxStoreImpl.open(file.toPath(), FxOptions.defaults());

    NavigableMap<Long, String> map1 = store1.createMap("map", Long.class, String.class);
    map1.put(1L, "hello");
    map1.put(2L, "world");

    NavigableSet<Long> set1 = store1.createSet("set", Long.class);
    set1.add(100L);
    set1.add(200L);

    Deque<String> deque1 = store1.createDeque("deque", String.class);
    deque1.addFirst("first");
    deque1.addLast("last");

    List<String> list1 = store1.createList("list", String.class);
    list1.add("item1");
    list1.add("item2");

    store1.close();

    // 2단계: 재오픈 후 데이터 검증
    FxStore store2 = FxStoreImpl.open(file.toPath(), FxOptions.defaults());

    NavigableMap<Long, String> map2 = store2.openMap("map", Long.class, String.class);
    assertEquals(2, map2.size());
    assertEquals("hello", map2.get(1L));
    assertEquals("world", map2.get(2L));

    NavigableSet<Long> set2 = store2.openSet("set", Long.class);
    assertEquals(2, set2.size());
    assertTrue(set2.contains(100L));
    assertTrue(set2.contains(200L));

    Deque<String> deque2 = store2.openDeque("deque", String.class);
    assertEquals(2, deque2.size());
    assertEquals("first", deque2.peekFirst());
    assertEquals("last", deque2.peekLast());

    List<String> list2 = store2.openList("list", String.class);
    assertEquals(2, list2.size());
    assertEquals("item1", list2.get(0));
    assertEquals("item2", list2.get(1));

    store2.close();
}
```

---

## 6. 품질 검증 계획

### 6.1 7가지 품질 기준 체크리스트

| # | 기준 | 검증 방법 | 목표 점수 |
|---|------|-----------|-----------|
| 1 | Plan-Code 정합성 | 본 문서 vs 구현 코드 라인별 대조 | ≥95 |
| 2 | SOLID 원칙 준수 | 각 메서드별 SOLID 검증 표 확인 | ≥95 |
| 3 | 테스트 커버리지 | JaCoCo 리포트 | ≥95% 라인 |
| 4 | 코드 가독성 | 네이밍, 구조, JavaDoc 확인 | ≥95 |
| 5 | 예외 처리 및 안정성 | 예외 조건 표, null 처리 확인 | ≥95 |
| 6 | 성능 효율성 | 복잡도 표, 벤치마크 | ≥95 |
| 7 | 문서화 품질 | JavaDoc 완성도 확인 | ≥95 |

### 6.2 SOLID 준수 검증 매트릭스

| 메서드 | SRP | OCP | LSP | ISP | DIP | 총점 |
|--------|-----|-----|-----|-----|-----|------|
| clear() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| descendingIterator() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| subMap() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| headMap() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| tailMap() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| removeFirstOccurrence() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| removeLastOccurrence() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |

---

## 7. 일정 및 마일스톤

### 7.1 구현 일정

| Phase | 내용 | 산출물 | 예상 소요 |
|-------|------|--------|-----------|
| P1 | clear(), navigableKeySet() | 코드 + 테스트 | 1시간 |
| P2 | descendingIterator() (Set, Deque) | 코드 + 테스트 | 1시간 |
| P3 | descending* (Map, Set) | 코드 + 테스트 | 2시간 |
| P4 | subMap/headMap/tailMap | 코드 + 테스트 | 3시간 |
| P5 | subSet/headSet/tailSet | 코드 + 테스트 | 2시간 |
| P6 | removeFirst/LastOccurrence | 코드 + 테스트 | 1시간 |
| P7 | 테스트 보강 | FuzzTest, 영속성 테스트 | 1시간 |
| P8 | 품질 검증 | 평가서, 문서 갱신 | 1시간 |

**총 소요 시간: 12시간**

### 7.2 마일스톤

| 마일스톤 | 완료 기준 | 검증 방법 |
|----------|-----------|-----------|
| M1 | 핵심 기능 완료 | clear() 테스트 통과 |
| M2 | Iterator 완료 | 모든 descendingIterator 테스트 통과 |
| M3 | 범위 뷰 완료 | sub/head/tailMap 테스트 통과 |
| M4 | Deque 완료 | removeFirst/LastOccurrence 테스트 통과 |
| M5 | 테스트 완비 | 라인 커버리지 ≥95% |
| M6 | 품질 달성 | 7가지 기준 모두 A+ |

---

## 부록 A: JavaDoc 템플릿

### A.1 메서드 JavaDoc 템플릿

```java
/**
 * [메서드 설명 - 한 줄 요약]
 *
 * <p>[상세 설명 - 동작 방식, 특이사항]
 *
 * <p>시간 복잡도: O(...)
 * <p>공간 복잡도: O(...)
 *
 * <p>불변식 보장:
 * <ul>
 *   <li>INV-X: [불변식 설명]</li>
 * </ul>
 *
 * @param paramName [파라미터 설명] (null 허용 여부)
 * @return [반환값 설명]
 * @throws ExceptionType [예외 조건]
 * @see [관련 메서드/클래스]
 */
```

### A.2 클래스 JavaDoc 템플릿

```java
/**
 * [클래스 설명 - 한 줄 요약]
 *
 * <p>[상세 설명 - 책임, 사용법]
 *
 * <p>스레드 안전성: [설명]
 *
 * <p>SOLID 준수:
 * <ul>
 *   <li>SRP: [단일 책임 설명]</li>
 *   <li>OCP: [확장 방식 설명]</li>
 * </ul>
 *
 * @param <T> [타입 파라미터 설명]
 * @see [관련 클래스]
 */
```

---

## 부록 B: 불변식 검증 체크리스트

### B.1 구현 시 확인할 불변식

| 불변식 | 설명 | 검증 방법 |
|--------|------|-----------|
| INV-1 | seqNo 단조 증가 | commit 후 seqNo 증가 확인 |
| INV-3 | BTree 구조 무결성 | verify() 통과 |
| INV-5 | 루트 페이지 ID 유효 | 0이 아닌 값 |
| INV-7 | OST subtreeCount 일관성 | size() == subtreeCount |
| INV-9 | 코덱 불변성 | 동일 ID/version 유지 |

### B.2 테스트에서 검증할 항목

```java
// 각 테스트 종료 시 검증
@After
public void verifyInvariants() {
    if (store != null && !store.isClosed()) {
        assertTrue("Store should verify ok", store.verify().ok());
    }
}
```

---

[← 목차로 돌아가기](00.index.md)
