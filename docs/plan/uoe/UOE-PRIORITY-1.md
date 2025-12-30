# UOE 개선 1순위: 뷰 생성 연산 (24개)

> **문서 버전:** 1.0
> **우선순위:** ⭐⭐⭐ (높음)
> **예상 기간:** 2-3일
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

뷰의 뷰 생성 및 KeySet 파생 연산 24개를 구현합니다. 경계 조건 조합만으로 구현 가능하며, 원본 데이터 수정이 필요 없어 가장 쉽게 구현할 수 있습니다.

### 핵심 원칙

- **경계 조건 위임**: 원본 Map/Set의 메서드를 호출하고 결과를 래핑
- **불변성 유지**: 뷰 생성은 원본 데이터를 수정하지 않음
- **LSP 준수**: Java 표준 NavigableMap/NavigableSet과 동일한 동작 보장

---

## 대상 UOE 목록

### A-1. KeySetView의 subSet/headSet/tailSet (6개)

| 클래스 | 메서드 | 현재 상태 | 구현 방법 |
|--------|--------|----------|----------|
| KeySetView | `subSet(from, fi, to, ti)` | UOE | `map.subMap(from, fi, to, ti).navigableKeySet()` |
| KeySetView | `headSet(to, incl)` | UOE | `map.headMap(to, incl).navigableKeySet()` |
| KeySetView | `tailSet(from, incl)` | UOE | `map.tailMap(from, incl).navigableKeySet()` |
| KeySetView | `subSet(from, to)` | UOE | `subSet(from, true, to, false)` |
| KeySetView | `headSet(to)` | UOE | `headSet(to, false)` |
| KeySetView | `tailSet(from)` | UOE | `tailSet(from, true)` |

### A-2. Views의 navigableKeySet/descendingKeySet (6개)

| 클래스 | 메서드 | 현재 상태 | 구현 방법 |
|--------|--------|----------|----------|
| SubMapView | `navigableKeySet()` | UOE | `new KeySetView<>(this, false)` |
| SubMapView | `descendingKeySet()` | UOE | `new KeySetView<>(this, true)` |
| HeadMapView | `navigableKeySet()` | UOE | `new KeySetView<>(this, false)` |
| HeadMapView | `descendingKeySet()` | UOE | `new KeySetView<>(this, true)` |
| TailMapView | `navigableKeySet()` | UOE | `new KeySetView<>(this, false)` |
| TailMapView | `descendingKeySet()` | UOE | `new KeySetView<>(this, true)` |

### A-3. Views의 descendingMap/descendingSet (6개)

| 클래스 | 메서드 | 현재 상태 | 구현 방법 |
|--------|--------|----------|----------|
| SubMapView | `descendingMap()` | UOE | 새로운 DescendingSubMapView 래퍼 |
| HeadMapView | `descendingMap()` | UOE | 새로운 DescendingHeadMapView 래퍼 |
| TailMapView | `descendingMap()` | UOE | 새로운 DescendingTailMapView 래퍼 |
| SubSetView | `descendingSet()` | UOE | 새로운 DescendingSubSetView 래퍼 |
| HeadSetView | `descendingSet()` | UOE | 새로운 DescendingHeadSetView 래퍼 |
| TailSetView | `descendingSet()` | UOE | 새로운 DescendingTailSetView 래퍼 |

### A-4. DescendingView의 뷰 생성 (6개)

| 클래스 | 메서드 | 현재 상태 | 구현 방법 |
|--------|--------|----------|----------|
| DescendingMapView | `subMap(from, fi, to, ti)` | UOE | `parent.subMap(to, ti, from, fi).descendingMap()` |
| DescendingMapView | `headMap(to, incl)` | UOE | `parent.tailMap(to, incl).descendingMap()` |
| DescendingMapView | `tailMap(from, incl)` | UOE | `parent.headMap(from, incl).descendingMap()` |
| DescendingSetView | `subSet(from, fi, to, ti)` | UOE | `parent.subSet(to, ti, from, fi).descendingSet()` |
| DescendingSetView | `headSet(to, incl)` | UOE | `parent.tailSet(to, incl).descendingSet()` |
| DescendingSetView | `tailSet(from, incl)` | UOE | `parent.headSet(from, incl).descendingSet()` |

---

## 구현 계획

### Day 1: KeySetView 및 Views의 KeySet 메서드

#### 1일차 오전: KeySetView 수정

**파일:** `FxNavigableMapImpl.java` (KeySetView 클래스)

```java
// 현재 코드 (UOE)
@Override
public NavigableSet<K> subSet(K fromElement, boolean fi, K toElement, boolean ti) {
    throw new UnsupportedOperationException("subSet() on KeySet not supported");
}

// 개선 코드
@Override
public NavigableSet<K> subSet(K fromElement, boolean fi, K toElement, boolean ti) {
    return map.subMap(fromElement, fi, toElement, ti).navigableKeySet();
}
```

#### 1일차 오후: Views의 navigableKeySet/descendingKeySet

**파일:** `FxNavigableMapImpl.java` (SubMapView, HeadMapView, TailMapView)

```java
// SubMapView 예시
@Override
public NavigableSet<K> navigableKeySet() {
    return new KeySetView<>(this, false);
}

@Override
public NavigableSet<K> descendingKeySet() {
    return new KeySetView<>(this, true);
}
```

**주의:** KeySetView 생성자가 NavigableMap을 받도록 수정 필요

```java
// KeySetView 생성자 수정
private static class KeySetView<K, V> extends AbstractSet<K> implements NavigableSet<K> {
    private final NavigableMap<K, V> map;  // FxNavigableMapImpl → NavigableMap
    private final boolean descending;

    KeySetView(NavigableMap<K, V> map, boolean descending) {
        this.map = map;
        this.descending = descending;
    }
    // ...
}
```

### Day 2: descendingMap/descendingSet 및 DescendingView

#### 2일차 오전: Views의 descendingMap/descendingSet

**접근 방법 1: 단순 래퍼**

기존 DescendingMapView를 재사용하되, 범위 뷰를 감싸는 형태:

```java
// SubMapView.descendingMap()
@Override
public NavigableMap<K, V> descendingMap() {
    return new DescendingMapView<>(this);  // this = SubMapView
}
```

**문제:** 현재 DescendingMapView는 FxNavigableMapImpl만 받음

**해결:** DescendingMapView가 NavigableMap을 받도록 일반화

```java
private static class DescendingMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {
    private final NavigableMap<K, V> parent;  // FxNavigableMapImpl → NavigableMap

    DescendingMapView(NavigableMap<K, V> parent) {
        this.parent = parent;
    }
    // 모든 메서드에서 parent 사용
}
```

#### 2일차 오후: DescendingView의 뷰 생성

**핵심:** Descending 뷰에서 subMap/headMap/tailMap 호출 시 경계를 뒤집어야 함

```java
// DescendingMapView.subMap(from, fi, to, ti)
@Override
public NavigableMap<K, V> subMap(K fromKey, boolean fi, K toKey, boolean ti) {
    // Descending이므로 from과 to를 뒤집음
    // descending에서 from=40, to=20이면 → ascending에서 from=20, to=40
    return parent.subMap(toKey, ti, fromKey, fi).descendingMap();
}

// DescendingMapView.headMap(to, incl)
@Override
public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
    // descending headMap(20) = ascending tailMap(20)의 역순
    return parent.tailMap(toKey, inclusive).descendingMap();
}

// DescendingMapView.tailMap(from, incl)
@Override
public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
    // descending tailMap(40) = ascending headMap(40)의 역순
    return parent.headMap(fromKey, inclusive).descendingMap();
}
```

### Day 3: 테스트 작성 및 회귀 테스트

#### 테스트 파일 목록

| 파일명 | 대상 |
|--------|------|
| `KeySetViewEnhancedTest.java` | KeySetView의 subSet/headSet/tailSet |
| `ViewKeySetTest.java` | Views의 navigableKeySet/descendingKeySet |
| `ViewDescendingTest.java` | Views의 descendingMap/descendingSet |
| `DescendingViewSubViewTest.java` | DescendingView의 뷰 생성 |

---

## 상세 구현 가이드

### KeySetView 전체 수정 코드

```java
private static class KeySetView<K, V> extends AbstractSet<K> implements NavigableSet<K> {
    private final NavigableMap<K, V> map;
    private final boolean descending;

    KeySetView(NavigableMap<K, V> map, boolean descending) {
        this.map = map;
        this.descending = descending;
    }

    // === 기존 메서드 유지 ===

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<K> iterator() {
        if (descending) {
            return map.descendingKeySet().iterator();
        }
        return map.keySet().iterator();
    }

    // === 수정된 메서드 ===

    @Override
    public NavigableSet<K> subSet(K fromElement, boolean fi, K toElement, boolean ti) {
        if (descending) {
            return map.subMap(toElement, ti, fromElement, fi).navigableKeySet();
        }
        return map.subMap(fromElement, fi, toElement, ti).navigableKeySet();
    }

    @Override
    public NavigableSet<K> headSet(K toElement, boolean inclusive) {
        if (descending) {
            return map.tailMap(toElement, inclusive).navigableKeySet();
        }
        return map.headMap(toElement, inclusive).navigableKeySet();
    }

    @Override
    public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
        if (descending) {
            return map.headMap(fromElement, inclusive).navigableKeySet();
        }
        return map.tailMap(fromElement, inclusive).navigableKeySet();
    }

    @Override
    public SortedSet<K> subSet(K fromElement, K toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<K> headSet(K toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<K> tailSet(K fromElement) {
        return tailSet(fromElement, true);
    }

    // === 나머지 메서드 유지 ===
}
```

### SubMapView navigableKeySet/descendingKeySet 추가

```java
// SubMapView 클래스 내
@Override
public NavigableSet<K> navigableKeySet() {
    return new KeySetView<>(this, false);
}

@Override
public NavigableSet<K> descendingKeySet() {
    return new KeySetView<>(this, true);
}

@Override
public Set<K> keySet() {
    return navigableKeySet();
}
```

### DescendingMapView 일반화

```java
private static class DescendingMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {

    private final NavigableMap<K, V> parent;

    DescendingMapView(NavigableMap<K, V> parent) {
        this.parent = parent;
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fi, K toKey, boolean ti) {
        return parent.subMap(toKey, ti, fromKey, fi).descendingMap();
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return parent.tailMap(toKey, inclusive).descendingMap();
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return parent.headMap(fromKey, inclusive).descendingMap();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    // 나머지 메서드는 parent에 위임하되 역순 처리
}
```

---

## 테스트 시나리오

### 시나리오 1: KeySetView.subSet()

```java
@Test
public void keySetView_subSet_shouldReturnSubSet() {
    // Given
    map.put(10L, "A");
    map.put(20L, "B");
    map.put(30L, "C");
    map.put(40L, "D");
    NavigableSet<Long> keySet = map.navigableKeySet();

    // When
    NavigableSet<Long> subSet = keySet.subSet(15L, true, 35L, true);

    // Then
    assertEquals(2, subSet.size());
    assertTrue(subSet.contains(20L));
    assertTrue(subSet.contains(30L));
    assertFalse(subSet.contains(10L));
    assertFalse(subSet.contains(40L));
}
```

### 시나리오 2: SubMapView.navigableKeySet()

```java
@Test
public void subMapView_navigableKeySet_shouldWork() {
    // Given
    map.put(10L, "A");
    map.put(20L, "B");
    map.put(30L, "C");
    NavigableMap<Long, String> subMap = map.subMap(15L, true, 25L, true);

    // When
    NavigableSet<Long> keySet = subMap.navigableKeySet();

    // Then
    assertEquals(1, keySet.size());
    assertTrue(keySet.contains(20L));
    assertEquals(Long.valueOf(20L), keySet.first());
}
```

### 시나리오 3: DescendingMapView.subMap()

```java
@Test
public void descendingMap_subMap_shouldReverseRange() {
    // Given
    map.put(10L, "A");
    map.put(20L, "B");
    map.put(30L, "C");
    map.put(40L, "D");
    NavigableMap<Long, String> descMap = map.descendingMap();

    // When: descending에서 subMap(40, 20) = ascending의 subMap(20, 40) 역순
    NavigableMap<Long, String> subMap = descMap.subMap(40L, true, 20L, true);

    // Then
    assertEquals(3, subMap.size());
    assertEquals(Long.valueOf(40L), subMap.firstKey());  // descending이므로 40이 first
    assertEquals(Long.valueOf(20L), subMap.lastKey());
}
```

### 시나리오 4: TreeMap 동등성 검증

```java
@Test
public void equivalence_keySetSubSet_shouldMatchTreeMap() {
    // Given
    TreeMap<Long, String> treeMap = new TreeMap<>();
    treeMap.put(10L, "A");
    treeMap.put(20L, "B");
    treeMap.put(30L, "C");

    map.put(10L, "A");
    map.put(20L, "B");
    map.put(30L, "C");

    // When
    NavigableSet<Long> treeSubSet = treeMap.navigableKeySet().subSet(15L, true, 35L, true);
    NavigableSet<Long> fxSubSet = map.navigableKeySet().subSet(15L, true, 35L, true);

    // Then
    assertEquals(treeSubSet.size(), fxSubSet.size());
    assertEquals(new ArrayList<>(treeSubSet), new ArrayList<>(fxSubSet));
}
```

---

## 품질 기준

### 커버리지 목표

| 항목 | 목표 | 측정 방법 |
|------|------|----------|
| 신규 코드 Line | ≥ 95% | JaCoCo |
| 신규 코드 Branch | ≥ 90% | JaCoCo |
| 수정된 메서드 | 100% | 단위 테스트 |

### SOLID 원칙 검증

| 원칙 | 검증 항목 |
|------|----------|
| **OCP** | KeySetView 생성자가 NavigableMap 인터페이스를 받도록 일반화 |
| **LSP** | TreeMap과 동일한 동작 (EquivalenceTest) |
| **ISP** | NavigableSet 인터페이스의 모든 메서드 구현 |

### 회귀 테스트

```bash
# 전체 회귀 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "com.fxstore.collection.KeySetView*"
./gradlew test --tests "com.fxstore.collection.*Descending*"
```

---

## 체크리스트

### Day 1 체크리스트

- [ ] KeySetView 생성자 일반화 (NavigableMap 받도록)
- [ ] KeySetView.subSet(4-param) 구현
- [ ] KeySetView.headSet(2-param) 구현
- [ ] KeySetView.tailSet(2-param) 구현
- [ ] KeySetView.subSet(2-param) 위임
- [ ] KeySetView.headSet(1-param) 위임
- [ ] KeySetView.tailSet(1-param) 위임
- [ ] SubMapView.navigableKeySet() 구현
- [ ] SubMapView.descendingKeySet() 구현
- [ ] HeadMapView.navigableKeySet() 구현
- [ ] HeadMapView.descendingKeySet() 구현
- [ ] TailMapView.navigableKeySet() 구현
- [ ] TailMapView.descendingKeySet() 구현

### Day 2 체크리스트

- [ ] DescendingMapView 일반화 (NavigableMap 받도록)
- [ ] SubMapView.descendingMap() 구현
- [ ] HeadMapView.descendingMap() 구현
- [ ] TailMapView.descendingMap() 구현
- [ ] SubSetView.descendingSet() 구현
- [ ] HeadSetView.descendingSet() 구현
- [ ] TailSetView.descendingSet() 구현
- [ ] DescendingMapView.subMap() 구현
- [ ] DescendingMapView.headMap() 구현
- [ ] DescendingMapView.tailMap() 구현
- [ ] DescendingSetView.subSet() 구현
- [ ] DescendingSetView.headSet() 구현
- [ ] DescendingSetView.tailSet() 구현

### Day 3 체크리스트

- [ ] KeySetViewEnhancedTest.java 작성
- [ ] ViewKeySetTest.java 작성
- [ ] ViewDescendingTest.java 작성
- [ ] DescendingViewSubViewTest.java 작성
- [ ] 전체 회귀 테스트 통과
- [ ] 커버리지 95% 이상 확인
- [ ] EquivalenceTest 통과

---

[← UOE 개선 인덱스로 돌아가기](UOE-IMPROVEMENT-INDEX.md)
