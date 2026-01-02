# UOE 개선 2순위: DescendingView 수정 연산 (6개)

> **문서 버전:** 1.0
> **우선순위:** ⭐⭐ (중간)
> **예상 기간:** 1일
> **복잡도:** 낮음
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

DescendingMapView와 DescendingSetView의 수정 연산(put, remove, clear, add)을 구현합니다. 이 연산들은 **parent에 직접 위임**하면 되므로 복잡도가 낮습니다.

### 핵심 원칙

- **직접 위임**: DescendingView는 원본의 역순 "뷰"이므로, 수정은 원본에 직접 반영
- **순서 독립성**: put/remove/add는 키/요소 기준이므로 순서와 무관
- **LSP 준수**: Java TreeMap.descendingMap()과 동일하게 쓰기 가능

### Java 표준 동작 확인

```java
// TreeMap.descendingMap()은 쓰기를 지원함
TreeMap<Long, String> treeMap = new TreeMap<>();
treeMap.put(10L, "A");

NavigableMap<Long, String> descMap = treeMap.descendingMap();
descMap.put(20L, "B");  // 정상 동작 - 원본에 반영됨

System.out.println(treeMap.get(20L));  // "B" 출력
```

---

## 대상 UOE 목록

### DescendingMapView (3개)

| 메서드 | 현재 상태 | 구현 방법 |
|--------|----------|----------|
| `put(K key, V value)` | UOE | `parent.put(key, value)` |
| `remove(Object key)` | UOE | `parent.remove(key)` |
| `clear()` | UOE | `parent.clear()` |

### DescendingSetView (3개)

| 메서드 | 현재 상태 | 구현 방법 |
|--------|----------|----------|
| `add(E e)` | UOE | `parent.add(e)` |
| `remove(Object o)` | UOE | `parent.remove(o)` |
| `clear()` | UOE | `parent.clear()` |

**참고:** `pollFirst()`, `pollLast()`는 3순위에서 처리 (조회+삭제 조합)

---

## 구현 계획

### Day 1: 전체 구현 및 테스트

| 시간 | 작업 내용 |
|------|----------|
| 오전 (2h) | DescendingMapView.put/remove/clear 구현 |
| 오전 (1h) | DescendingSetView.add/remove/clear 구현 |
| 오후 (2h) | 단위 테스트 작성 |
| 오후 (2h) | EquivalenceTest 작성 및 회귀 테스트 |

---

## 상세 구현 가이드

### DescendingMapView 수정

**파일:** `FxNavigableMapImpl.java`

**현재 코드:**
```java
private static class DescendingMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {
    private final FxNavigableMapImpl<K, V> parent;

    // ...

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("DescendingMap is read-only");
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("DescendingMap is read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("DescendingMap is read-only");
    }
}
```

**개선 코드:**
```java
private static class DescendingMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {
    private final NavigableMap<K, V> parent;

    DescendingMapView(NavigableMap<K, V> parent) {
        this.parent = parent;
    }

    // === 수정 연산 - parent에 직접 위임 ===

    @Override
    public V put(K key, V value) {
        return parent.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return parent.remove(key);
    }

    @Override
    public void clear() {
        parent.clear();
    }

    // === 기존 읽기 메서드 유지 ===

    @Override
    public V get(Object key) {
        return parent.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return parent.containsKey(key);
    }

    @Override
    public int size() {
        return parent.size();
    }

    @Override
    public boolean isEmpty() {
        return parent.isEmpty();
    }

    // ... 나머지 메서드
}
```

### DescendingSetView 수정

**파일:** `FxNavigableSetImpl.java`

**현재 코드:**
```java
private class DescendingSetView extends AbstractSet<E> implements NavigableSet<E> {
    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("DescendingSet is read-only");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("DescendingSet is read-only");
    }

    // clear()는 AbstractSet에서 상속받아 iterator().remove() 호출 → UOE
}
```

**개선 코드:**
```java
private class DescendingSetView extends AbstractSet<E> implements NavigableSet<E> {
    @Override
    public boolean add(E e) {
        return FxNavigableSetImpl.this.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return FxNavigableSetImpl.this.remove(o);
    }

    @Override
    public void clear() {
        FxNavigableSetImpl.this.clear();
    }

    // ... 기존 메서드 유지
}
```

### putAll, addAll, removeAll, retainAll 처리

`AbstractMap`과 `AbstractSet`은 `put`/`add`/`remove`를 사용하여 bulk 연산을 구현합니다. 따라서 위의 수정만으로 다음 메서드도 자동으로 동작합니다:

- `putAll(Map)` → 내부적으로 `put()` 호출
- `addAll(Collection)` → 내부적으로 `add()` 호출
- `removeAll(Collection)` → 내부적으로 `remove()` 호출
- `retainAll(Collection)` → 내부적으로 `remove()` 호출

---

## 테스트 시나리오

### 시나리오 1: DescendingMap.put()

```java
@Test
public void descendingMap_put_shouldModifyOriginal() {
    // Given
    map.put(10L, "A");
    NavigableMap<Long, String> descMap = map.descendingMap();

    // When
    descMap.put(20L, "B");

    // Then
    assertEquals("B", map.get(20L));  // 원본에 반영
    assertEquals("B", descMap.get(20L));  // 뷰에서도 조회 가능
    assertEquals(2, map.size());
}
```

### 시나리오 2: DescendingMap.remove()

```java
@Test
public void descendingMap_remove_shouldModifyOriginal() {
    // Given
    map.put(10L, "A");
    map.put(20L, "B");
    NavigableMap<Long, String> descMap = map.descendingMap();

    // When
    String removed = descMap.remove(10L);

    // Then
    assertEquals("A", removed);
    assertNull(map.get(10L));  // 원본에서 삭제됨
    assertEquals(1, map.size());
}
```

### 시나리오 3: DescendingMap.clear()

```java
@Test
public void descendingMap_clear_shouldClearOriginal() {
    // Given
    map.put(10L, "A");
    map.put(20L, "B");
    NavigableMap<Long, String> descMap = map.descendingMap();

    // When
    descMap.clear();

    // Then
    assertTrue(map.isEmpty());
    assertTrue(descMap.isEmpty());
}
```

### 시나리오 4: DescendingSet.add()

```java
@Test
public void descendingSet_add_shouldModifyOriginal() {
    // Given
    set.add(10L);
    NavigableSet<Long> descSet = set.descendingSet();

    // When
    boolean added = descSet.add(20L);

    // Then
    assertTrue(added);
    assertTrue(set.contains(20L));  // 원본에 반영
    assertEquals(2, set.size());
}
```

### 시나리오 5: DescendingSet.remove()

```java
@Test
public void descendingSet_remove_shouldModifyOriginal() {
    // Given
    set.add(10L);
    set.add(20L);
    NavigableSet<Long> descSet = set.descendingSet();

    // When
    boolean removed = descSet.remove(10L);

    // Then
    assertTrue(removed);
    assertFalse(set.contains(10L));  // 원본에서 삭제됨
    assertEquals(1, set.size());
}
```

### 시나리오 6: TreeMap 동등성

```java
@Test
public void equivalence_descendingMap_put_shouldMatchTreeMap() {
    // Given
    TreeMap<Long, String> treeMap = new TreeMap<>();
    treeMap.put(10L, "A");

    map.put(10L, "A");

    // When
    treeMap.descendingMap().put(20L, "B");
    map.descendingMap().put(20L, "B");

    // Then
    assertEquals(treeMap.size(), map.size());
    assertEquals(treeMap.get(20L), map.get(20L));
}
```

### 시나리오 7: putAll via DescendingMap

```java
@Test
public void descendingMap_putAll_shouldWork() {
    // Given
    NavigableMap<Long, String> descMap = map.descendingMap();
    Map<Long, String> toAdd = new HashMap<>();
    toAdd.put(10L, "A");
    toAdd.put(20L, "B");

    // When
    descMap.putAll(toAdd);

    // Then
    assertEquals(2, map.size());
    assertEquals("A", map.get(10L));
    assertEquals("B", map.get(20L));
}
```

---

## 품질 기준

### 커버리지 목표

| 항목 | 목표 |
|------|------|
| 수정된 메서드 Line Coverage | 100% |
| 수정된 메서드 Branch Coverage | 100% |

### SOLID 원칙 검증

| 원칙 | 검증 항목 |
|------|----------|
| **LSP** | TreeMap.descendingMap().put()과 동일 동작 |
| **SRP** | DescendingView는 역순 뷰 제공만 담당, 수정은 parent 위임 |

### 동작 불변식

| 불변식 | 검증 방법 |
|--------|----------|
| `descMap.put(k, v)` 후 `map.get(k) == v` | 단위 테스트 |
| `descMap.remove(k)` 후 `!map.containsKey(k)` | 단위 테스트 |
| `descMap.clear()` 후 `map.isEmpty()` | 단위 테스트 |

---

## 체크리스트

### 구현 체크리스트

- [ ] DescendingMapView.put() 구현
- [ ] DescendingMapView.remove() 구현
- [ ] DescendingMapView.clear() 구현
- [ ] DescendingSetView.add() 구현
- [ ] DescendingSetView.remove() 구현
- [ ] DescendingSetView.clear() 구현

### 테스트 체크리스트

- [ ] DescendingMapModificationTest.java 작성
- [ ] DescendingSetModificationTest.java 작성
- [ ] DescendingMapEquivalenceTest.java 작성 (TreeMap 비교)
- [ ] DescendingSetEquivalenceTest.java 작성 (TreeSet 비교)
- [ ] putAll/addAll/removeAll 동작 확인
- [ ] 전체 회귀 테스트 통과

### 검증 체크리스트

- [ ] 원본 수정이 뷰에 반영됨 확인
- [ ] 뷰 수정이 원본에 반영됨 확인
- [ ] 빈 맵/셋에서 동작 확인
- [ ] null 키/값 처리 확인 (FxStore가 null 허용하는 경우)

---

## 구현 시 주의사항

### 1. parent 타입 일반화

1순위에서 DescendingMapView가 `NavigableMap<K, V>`를 받도록 수정했다면, 여기서도 동일하게 사용합니다.

```java
// 1순위에서 수정된 생성자
DescendingMapView(NavigableMap<K, V> parent) {
    this.parent = parent;
}
```

### 2. entrySet() 동작

`AbstractMap.entrySet().remove()`는 `DescendingMapView.remove()`를 호출하므로 자동으로 동작합니다.

### 3. 기존 테스트 영향

기존에 `@Test(expected = UnsupportedOperationException.class)`로 UOE를 기대하는 테스트가 있다면 수정이 필요합니다:

```java
// 기존 테스트 (삭제 또는 수정)
@Test(expected = UnsupportedOperationException.class)
public void descendingMap_put_shouldThrowUOE() { ... }

// 새 테스트로 대체
@Test
public void descendingMap_put_shouldWork() { ... }
```

---

[← UOE 개선 인덱스로 돌아가기](UOE-IMPROVEMENT-INDEX.md)
