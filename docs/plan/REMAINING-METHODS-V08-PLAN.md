# 미구현 메서드 완성 구현 계획 (Remaining Methods Implementation Plan)

> **문서 버전:** 1.0
> **대상 버전:** FxStore v0.8
> **Java 버전:** Java 8
> **작성일:** 2025-12-28
> **상태:** 계획 수립

[← 목차로 돌아가기](00.index.md)

---

## 목차

1. [개요](#1-개요)
2. [미구현 메서드 분석](#2-미구현-메서드-분석)
3. [기술 설계](#3-기술-설계)
4. [SOLID 원칙 적용](#4-solid-원칙-적용)
5. [구현 단계](#5-구현-단계)
6. [상세 구현 명세](#6-상세-구현-명세)
7. [테스트 전략](#7-테스트-전략)
8. [회귀 테스트 프로세스](#8-회귀-테스트-프로세스)
9. [위험 요소 및 대응](#9-위험-요소-및-대응)
10. [체크리스트](#10-체크리스트)

---

## 1. 개요

### 1.1 배경

FxStore v0.7까지 구현 완료 후, Java 표준 컬렉션 인터페이스의 일부 메서드가 `UnsupportedOperationException`을 던지는 상태로 남아있습니다. 본 문서는 이들 15개 미구현 메서드의 체계적인 구현 계획을 정의합니다.

### 1.2 목표

| 목표 | 측정 지표 | 달성 기준 |
|------|-----------|-----------|
| 기능 완성 | 미구현 메서드 수 | 0개 |
| 테스트 완비 | 라인 커버리지 | ≥95% |
| 브랜치 커버리지 | 브랜치 커버리지 | ≥90% |
| 품질 보증 | 7가지 품질 기준 | 모두 A+ |
| API 호환성 | Java 표준 인터페이스 준수 | 100% |

### 1.3 범위

**포함 (15개 메서드):**

| 클래스 | 메서드 수 | 카테고리 |
|--------|----------|----------|
| FxNavigableMapImpl | 7개 | Map 핵심 + 범위 뷰 + Descending |
| FxNavigableSetImpl | 5개 | 범위 뷰 + Descending |
| FxDequeImpl | 3개 | 검색/삭제 + Descending |

**제외 (설계 결정에 따른 의도적 미지원):**

| 기능 | 미지원 사유 | 대안 |
|------|-------------|------|
| 범위 뷰 쓰기 | COW 일관성 복잡도 | 읽기 전용 뷰 제공 |
| 뷰의 remove() | 원본 변경 시 복잡도 | UnsupportedOperationException |

### 1.4 핵심 불변식 (Invariants)

| ID | 불변식 | 설명 | 검증 방법 |
|----|--------|------|-----------|
| **INV-V1** | View Consistency | 뷰는 원본 변경 시 실시간 반영 | 라이브 뷰 테스트 |
| **INV-V2** | Range Validity | fromKey ≤ toKey 보장 | IllegalArgumentException 테스트 |
| **INV-V3** | Null Safety | null 키/값 처리 일관성 | NullPointerException 테스트 |
| **INV-V4** | Iterator Consistency | Iterator는 스냅샷 기반 | ConcurrentModification 테스트 |
| **INV-V5** | FIFO Ordering | Deque removeFirst/Last 순서 보장 | 순서 테스트 |

### 1.5 관련 문서

| 문서 | 연관성 |
|------|--------|
| [IMPLEMENTATION-REMAINING-FEATURES.md](IMPLEMENTATION-REMAINING-FEATURES.md) | 상세 구현 명세 원본 |
| [EVALUATION-REMAINING-FEATURES-V2.md](EVALUATION-REMAINING-FEATURES-V2.md) | 품질 평가 기준 |
| [01.implementation-phases.md](01.implementation-phases.md) | Phase 5: 컬렉션 구현 |
| [QUALITY-POLICY.md](QUALITY-POLICY.md) | 품질 정책 QP-001 |

---

## 2. 미구현 메서드 분석

### 2.1 전체 목록 (15개)

| # | 클래스 | 메서드 시그니처 | 카테고리 | 복잡도 | 우선순위 |
|---|--------|----------------|----------|--------|----------|
| 1 | FxNavigableMapImpl | `void clear()` | A: 핵심 | O(N) | P0 |
| 2 | FxNavigableMapImpl | `NavigableMap<K,V> descendingMap()` | C: Descending | O(1) | P2 |
| 3 | FxNavigableMapImpl | `NavigableSet<K> navigableKeySet()` | B: KeySet | O(1) | P1 |
| 4 | FxNavigableMapImpl | `NavigableSet<K> descendingKeySet()` | C: Descending | O(1) | P2 |
| 5 | FxNavigableMapImpl | `NavigableMap<K,V> subMap(K,boolean,K,boolean)` | D: 범위뷰 | O(1) | P1 |
| 6 | FxNavigableMapImpl | `NavigableMap<K,V> headMap(K,boolean)` | D: 범위뷰 | O(1) | P1 |
| 7 | FxNavigableMapImpl | `NavigableMap<K,V> tailMap(K,boolean)` | D: 범위뷰 | O(1) | P1 |
| 8 | FxNavigableSetImpl | `NavigableSet<E> descendingSet()` | C: Descending | O(1) | P2 |
| 9 | FxNavigableSetImpl | `Iterator<E> descendingIterator()` | C: Descending | O(N) | P2 |
| 10 | FxNavigableSetImpl | `NavigableSet<E> subSet(E,boolean,E,boolean)` | D: 범위뷰 | O(1) | P1 |
| 11 | FxNavigableSetImpl | `NavigableSet<E> headSet(E,boolean)` | D: 범위뷰 | O(1) | P1 |
| 12 | FxNavigableSetImpl | `NavigableSet<E> tailSet(E,boolean)` | D: 범위뷰 | O(1) | P1 |
| 13 | FxDequeImpl | `boolean removeFirstOccurrence(Object)` | E: 검색삭제 | O(N) | P3 |
| 14 | FxDequeImpl | `boolean removeLastOccurrence(Object)` | E: 검색삭제 | O(N) | P3 |
| 15 | FxDequeImpl | `Iterator<E> descendingIterator()` | C: Descending | O(N) | P2 |

### 2.2 카테고리 정의

| 카테고리 | 설명 | 메서드 수 | 구현 복잡도 |
|----------|------|-----------|-------------|
| A: 핵심 | Map.clear() - 필수 기능 | 1 | 중간 |
| B: KeySet | navigableKeySet() - Map 키 뷰 | 1 | 낮음 |
| C: Descending | 역순 뷰/Iterator | 5 | 중간 |
| D: 범위뷰 | sub/head/tailMap, sub/head/tailSet | 6 | 높음 |
| E: 검색삭제 | removeFirst/LastOccurrence | 2 | 낮음 |

### 2.3 의존성 분석

```
┌─────────────────────────────────────────────────────────────────────┐
│                        의존성 그래프                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  [clear()] ──────────────────────────────────── 독립                │
│                                                                      │
│  [navigableKeySet()] ────┬────► [descendingKeySet()]                │
│                          │                                           │
│                          └────► [NavigableKeySetView 클래스]        │
│                                                                      │
│  [descendingIterator()] ─┬────► [descendingSet()]                   │
│   (Set, Deque)           │                                           │
│                          └────► [descendingMap()]                    │
│                                                                      │
│  [subMap()] ─────────────┬────► [headMap()] (subMap 특수 케이스)    │
│                          │                                           │
│                          └────► [tailMap()] (subMap 특수 케이스)    │
│                                 │                                    │
│                                 └────► [subSet/headSet/tailSet]     │
│                                        (Map 뷰 패턴 재사용)          │
│                                                                      │
│  [removeFirst/LastOccurrence()] ──────────────── 독립               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.4 구현 순서 결정

| 순서 | Phase | 메서드 | 의존성 | 예상 소요 |
|------|-------|--------|--------|-----------|
| 1 | A | `clear()` | 없음 | 30분 |
| 2 | B | `navigableKeySet()` | 없음 | 1시간 |
| 3 | C | `descendingIterator()` (Set) | 없음 | 30분 |
| 4 | C | `descendingIterator()` (Deque) | 없음 | 30분 |
| 5 | C | `descendingKeySet()` | navigableKeySet | 30분 |
| 6 | C | `descendingSet()` | descendingIterator | 1시간 |
| 7 | C | `descendingMap()` | descendingKeySet | 1시간 |
| 8 | D | `subMap()` | 없음 | 2시간 |
| 9 | D | `headMap()` | subMap 패턴 | 30분 |
| 10 | D | `tailMap()` | subMap 패턴 | 30분 |
| 11 | D | `subSet()` | subMap 패턴 참조 | 1시간 |
| 12 | D | `headSet()` | subSet 패턴 | 30분 |
| 13 | D | `tailSet()` | subSet 패턴 | 30분 |
| 14 | E | `removeFirstOccurrence()` | 없음 | 30분 |
| 15 | E | `removeLastOccurrence()` | 없음 | 30분 |

**총 예상 소요: 약 11시간**

---

## 3. 기술 설계

### 3.1 공통 설계 원칙

#### 3.1.1 읽기 전용 뷰 패턴

```java
/**
 * 읽기 전용 뷰의 공통 특성:
 * 1. 원본 컬렉션 참조 유지
 * 2. 수정 연산 시 UnsupportedOperationException
 * 3. 읽기 연산은 원본에 위임
 * 4. 라이브 뷰: 원본 변경 시 실시간 반영
 */
public abstract class AbstractReadOnlyView<E> {
    protected final FxCollection parent;

    // 수정 불가
    public boolean add(E e) {
        throw new UnsupportedOperationException("View is read-only");
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("View is read-only");
    }

    public void clear() {
        throw new UnsupportedOperationException("View is read-only");
    }
}
```

#### 3.1.2 범위 검증 유틸리티

```java
/**
 * 범위 뷰를 위한 공통 유틸리티
 */
public final class RangeUtils {

    /**
     * 키가 범위 내에 있는지 확인
     *
     * @param key 검사할 키
     * @param fromKey 시작 키 (null이면 무제한)
     * @param fromInclusive 시작 키 포함 여부
     * @param toKey 종료 키 (null이면 무제한)
     * @param toInclusive 종료 키 포함 여부
     * @param comparator 비교자
     * @return 범위 내이면 true
     */
    public static <K> boolean inRange(K key,
                                       K fromKey, boolean fromInclusive,
                                       K toKey, boolean toInclusive,
                                       Comparator<? super K> comparator) {
        if (key == null) return false;

        // 하한 검사
        if (fromKey != null) {
            int cmp = comparator.compare(key, fromKey);
            if (fromInclusive ? cmp < 0 : cmp <= 0) {
                return false;
            }
        }

        // 상한 검사
        if (toKey != null) {
            int cmp = comparator.compare(key, toKey);
            if (toInclusive ? cmp > 0 : cmp >= 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * 범위 유효성 검증
     *
     * @throws IllegalArgumentException fromKey > toKey인 경우
     */
    public static <K> void validateRange(K fromKey, K toKey,
                                          Comparator<? super K> comparator) {
        if (fromKey != null && toKey != null) {
            if (comparator.compare(fromKey, toKey) > 0) {
                throw new IllegalArgumentException(
                    "fromKey > toKey: " + fromKey + " > " + toKey);
            }
        }
    }
}
```

### 3.2 FxNavigableMapImpl 설계

#### 3.2.1 clear() 구현

```java
/**
 * 이 맵의 모든 매핑을 제거합니다.
 *
 * <p>시간 복잡도: O(N) - 모든 엔트리 순회 및 삭제
 * <p>공간 복잡도: O(N) - 키 목록 임시 저장 (ConcurrentModification 방지)
 *
 * <p>불변식 보장:
 * <ul>
 *   <li>INV-3: BTree 구조 무결성 유지</li>
 *   <li>INV-5: 루트 페이지 ID 갱신</li>
 * </ul>
 *
 * @throws FxException I/O 오류 발생 시
 */
@Override
public void clear() {
    writeLock.lock();
    try {
        if (isEmpty()) {
            return;  // 빈 맵은 아무것도 하지 않음
        }

        // 모든 키 수집 (삭제 중 ConcurrentModification 방지)
        List<K> keysToRemove = new ArrayList<>();
        for (K key : keySet()) {
            keysToRemove.add(key);
        }

        // 역순으로 삭제 (BTree 밸런싱 최적화)
        for (int i = keysToRemove.size() - 1; i >= 0; i--) {
            remove(keysToRemove.get(i));
        }

        markDirty();
    } finally {
        writeLock.unlock();
    }
}
```

#### 3.2.2 SubMapView 클래스

```java
/**
 * NavigableMap의 범위 부분 뷰 구현.
 *
 * <p>특성:
 * <ul>
 *   <li>읽기 전용: put(), remove() 등 수정 연산 불가</li>
 *   <li>라이브 뷰: 원본 맵 변경 시 즉시 반영</li>
 *   <li>범위 체크: 모든 연산에서 범위 검증</li>
 * </ul>
 *
 * @param <K> 키 타입
 * @param <V> 값 타입
 */
private static class SubMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V>, Serializable {

    private static final long serialVersionUID = 1L;

    private final FxNavigableMapImpl<K, V> parent;
    private final K fromKey;
    private final boolean fromInclusive;
    private final K toKey;
    private final boolean toInclusive;

    // fromKey == null이면 하한 무제한 (headMap)
    // toKey == null이면 상한 무제한 (tailMap)

    SubMapView(FxNavigableMapImpl<K, V> parent,
               K fromKey, boolean fromInclusive,
               K toKey, boolean toInclusive) {
        this.parent = Objects.requireNonNull(parent);
        this.fromKey = fromKey;
        this.fromInclusive = fromInclusive;
        this.toKey = toKey;
        this.toInclusive = toInclusive;

        // 범위 유효성 검증
        if (fromKey != null && toKey != null) {
            RangeUtils.validateRange(fromKey, toKey, parent.comparator());
        }
    }

    /**
     * 키가 이 뷰의 범위 내에 있는지 확인
     */
    private boolean inRange(K key) {
        return RangeUtils.inRange(key, fromKey, fromInclusive,
                                   toKey, toInclusive, parent.comparator());
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
    public void clear() {
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
    public boolean isEmpty() {
        for (K key : parent.keySet()) {
            if (inRange(key)) return false;
        }
        return true;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> result = new LinkedHashSet<>();
        for (Entry<K, V> entry : parent.entrySet()) {
            if (inRange(entry.getKey())) {
                // 불변 엔트리로 래핑
                result.add(new AbstractMap.SimpleImmutableEntry<>(entry));
            }
        }
        return Collections.unmodifiableSet(result);
    }

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

    // NavigableMap 추가 메서드 구현
    @Override
    public Entry<K, V> lowerEntry(K key) {
        Entry<K, V> entry = parent.lowerEntry(key);
        if (entry != null && inRange(entry.getKey())) {
            return new AbstractMap.SimpleImmutableEntry<>(entry);
        }
        return null;
    }

    @Override
    public K lowerKey(K key) {
        Entry<K, V> entry = lowerEntry(key);
        return entry != null ? entry.getKey() : null;
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        Entry<K, V> entry = parent.floorEntry(key);
        if (entry != null && inRange(entry.getKey())) {
            return new AbstractMap.SimpleImmutableEntry<>(entry);
        }
        return null;
    }

    @Override
    public K floorKey(K key) {
        Entry<K, V> entry = floorEntry(key);
        return entry != null ? entry.getKey() : null;
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        Entry<K, V> entry = parent.ceilingEntry(key);
        if (entry != null && inRange(entry.getKey())) {
            return new AbstractMap.SimpleImmutableEntry<>(entry);
        }
        return null;
    }

    @Override
    public K ceilingKey(K key) {
        Entry<K, V> entry = ceilingEntry(key);
        return entry != null ? entry.getKey() : null;
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        Entry<K, V> entry = parent.higherEntry(key);
        if (entry != null && inRange(entry.getKey())) {
            return new AbstractMap.SimpleImmutableEntry<>(entry);
        }
        return null;
    }

    @Override
    public K higherKey(K key) {
        Entry<K, V> entry = higherEntry(key);
        return entry != null ? entry.getKey() : null;
    }

    @Override
    public Entry<K, V> firstEntry() {
        try {
            K first = firstKey();
            return new AbstractMap.SimpleImmutableEntry<>(first, parent.get(first));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public Entry<K, V> lastEntry() {
        try {
            K last = lastKey();
            return new AbstractMap.SimpleImmutableEntry<>(last, parent.get(last));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException("SubMap is read-only");
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException("SubMap is read-only");
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return new DescendingMapView<>(this);
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return new NavigableKeySetView<>(this);
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
                                      K toKey, boolean toInclusive) {
        // 현재 범위 내에서만 subMap 생성
        K newFromKey = this.fromKey;
        boolean newFromInclusive = this.fromInclusive;
        K newToKey = this.toKey;
        boolean newToInclusive = this.toInclusive;

        // 새 하한 조정
        if (fromKey != null) {
            if (newFromKey == null || comparator().compare(fromKey, newFromKey) > 0 ||
                (comparator().compare(fromKey, newFromKey) == 0 && !fromInclusive)) {
                newFromKey = fromKey;
                newFromInclusive = fromInclusive;
            }
        }

        // 새 상한 조정
        if (toKey != null) {
            if (newToKey == null || comparator().compare(toKey, newToKey) < 0 ||
                (comparator().compare(toKey, newToKey) == 0 && !toInclusive)) {
                newToKey = toKey;
                newToInclusive = toInclusive;
            }
        }

        return new SubMapView<>(parent, newFromKey, newFromInclusive,
                                 newToKey, newToInclusive);
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return subMap(fromKey, fromInclusive, toKey, inclusive);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return subMap(fromKey, inclusive, toKey, toInclusive);
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
}
```

#### 3.2.3 DescendingMapView 클래스

```java
/**
 * NavigableMap의 역순 뷰 구현.
 *
 * <p>특성:
 * <ul>
 *   <li>읽기 전용</li>
 *   <li>역순 비교자 사용</li>
 *   <li>first/last, higher/lower 반전</li>
 * </ul>
 */
private static class DescendingMapView<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {

    private final NavigableMap<K, V> parent;
    private final Comparator<? super K> reverseComparator;

    DescendingMapView(NavigableMap<K, V> parent) {
        this.parent = parent;
        this.reverseComparator = Collections.reverseOrder(parent.comparator());
    }

    @Override
    public Comparator<? super K> comparator() {
        return reverseComparator;
    }

    @Override
    public K firstKey() {
        return parent.lastKey();
    }

    @Override
    public K lastKey() {
        return parent.firstKey();
    }

    @Override
    public Entry<K, V> firstEntry() {
        return parent.lastEntry();
    }

    @Override
    public Entry<K, V> lastEntry() {
        return parent.firstEntry();
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        return parent.higherEntry(key);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        return parent.lowerEntry(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        return parent.ceilingEntry(key);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        return parent.floorEntry(key);
    }

    // 키 메서드는 Entry 메서드 위임
    @Override
    public K lowerKey(K key) {
        Entry<K, V> e = lowerEntry(key);
        return e != null ? e.getKey() : null;
    }

    @Override
    public K higherKey(K key) {
        Entry<K, V> e = higherEntry(key);
        return e != null ? e.getKey() : null;
    }

    @Override
    public K floorKey(K key) {
        Entry<K, V> e = floorEntry(key);
        return e != null ? e.getKey() : null;
    }

    @Override
    public K ceilingKey(K key) {
        Entry<K, V> e = ceilingEntry(key);
        return e != null ? e.getKey() : null;
    }

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

    @Override
    public Set<Entry<K, V>> entrySet() {
        // 역순으로 엔트리 수집
        List<Entry<K, V>> entries = new ArrayList<>(parent.entrySet());
        Collections.reverse(entries);
        return new LinkedHashSet<>(entries);
    }

    // 수정 연산 불가
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

    @Override
    public Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException("DescendingMap is read-only");
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException("DescendingMap is read-only");
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return parent;  // 이중 역순 = 원본
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return new NavigableKeySetView<>(this);
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return parent.navigableKeySet();
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
                                      K toKey, boolean toInclusive) {
        // 역순이므로 from/to 반전
        return parent.subMap(toKey, toInclusive, fromKey, fromInclusive)
                     .descendingMap();
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
}
```

#### 3.2.4 NavigableKeySetView 클래스

```java
/**
 * NavigableMap의 키 집합 뷰 구현.
 */
private static class NavigableKeySetView<K, V> extends AbstractSet<K>
        implements NavigableSet<K> {

    private final NavigableMap<K, V> map;

    NavigableKeySetView(NavigableMap<K, V> map) {
        this.map = map;
    }

    @Override
    public Iterator<K> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Comparator<? super K> comparator() {
        return map.comparator();
    }

    @Override
    public K first() {
        return map.firstKey();
    }

    @Override
    public K last() {
        return map.lastKey();
    }

    @Override
    public K lower(K e) {
        return map.lowerKey(e);
    }

    @Override
    public K floor(K e) {
        return map.floorKey(e);
    }

    @Override
    public K ceiling(K e) {
        return map.ceilingKey(e);
    }

    @Override
    public K higher(K e) {
        return map.higherKey(e);
    }

    @Override
    public K pollFirst() {
        throw new UnsupportedOperationException("KeySet is read-only");
    }

    @Override
    public K pollLast() {
        throw new UnsupportedOperationException("KeySet is read-only");
    }

    @Override
    public boolean add(K e) {
        throw new UnsupportedOperationException("KeySet is read-only");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("KeySet is read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("KeySet is read-only");
    }

    @Override
    public NavigableSet<K> descendingSet() {
        return map.descendingMap().navigableKeySet();
    }

    @Override
    public Iterator<K> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<K> subSet(K fromElement, boolean fromInclusive,
                                   K toElement, boolean toInclusive) {
        return map.subMap(fromElement, fromInclusive, toElement, toInclusive)
                  .navigableKeySet();
    }

    @Override
    public NavigableSet<K> headSet(K toElement, boolean inclusive) {
        return map.headMap(toElement, inclusive).navigableKeySet();
    }

    @Override
    public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
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
}
```

### 3.3 FxNavigableSetImpl 설계

#### 3.3.1 descendingIterator() 구현

```java
/**
 * 이 집합의 요소를 내림차순으로 순회하는 Iterator를 반환합니다.
 *
 * <p>시간 복잡도: O(N) - 전체 요소 수집 필요
 * <p>공간 복잡도: O(N) - 스냅샷 저장
 *
 * <p>Iterator 특성:
 * <ul>
 *   <li>스냅샷 기반: 생성 시점의 상태 유지</li>
 *   <li>remove() 미지원</li>
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

#### 3.3.2 SubSetView 클래스

```java
/**
 * NavigableSet의 범위 부분 뷰 구현.
 */
private static class SubSetView<E> extends AbstractSet<E>
        implements NavigableSet<E> {

    private final FxNavigableSetImpl<E> parent;
    private final E fromElement;
    private final boolean fromInclusive;
    private final E toElement;
    private final boolean toInclusive;

    SubSetView(FxNavigableSetImpl<E> parent,
               E fromElement, boolean fromInclusive,
               E toElement, boolean toInclusive) {
        this.parent = Objects.requireNonNull(parent);
        this.fromElement = fromElement;
        this.fromInclusive = fromInclusive;
        this.toElement = toElement;
        this.toInclusive = toInclusive;

        if (fromElement != null && toElement != null) {
            RangeUtils.validateRange(fromElement, toElement, parent.comparator());
        }
    }

    private boolean inRange(E element) {
        return RangeUtils.inRange(element, fromElement, fromInclusive,
                                   toElement, toInclusive, parent.comparator());
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<E> parentIter = parent.iterator();
            private E next = findNext();

            private E findNext() {
                while (parentIter.hasNext()) {
                    E e = parentIter.next();
                    if (inRange(e)) return e;
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public E next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                E result = next;
                next = findNext();
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("SubSet is read-only");
            }
        };
    }

    @Override
    public int size() {
        int count = 0;
        for (E e : parent) {
            if (inRange(e)) count++;
        }
        return count;
    }

    @Override
    public boolean contains(Object o) {
        @SuppressWarnings("unchecked")
        E e = (E) o;
        return inRange(e) && parent.contains(o);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("SubSet is read-only");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("SubSet is read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("SubSet is read-only");
    }

    @Override
    public Comparator<? super E> comparator() {
        return parent.comparator();
    }

    @Override
    public E first() {
        for (E e : parent) {
            if (inRange(e)) return e;
        }
        throw new NoSuchElementException();
    }

    @Override
    public E last() {
        E last = null;
        for (E e : parent) {
            if (inRange(e)) last = e;
        }
        if (last == null) throw new NoSuchElementException();
        return last;
    }

    @Override
    public E lower(E e) {
        E result = parent.lower(e);
        return (result != null && inRange(result)) ? result : null;
    }

    @Override
    public E floor(E e) {
        E result = parent.floor(e);
        return (result != null && inRange(result)) ? result : null;
    }

    @Override
    public E ceiling(E e) {
        E result = parent.ceiling(e);
        return (result != null && inRange(result)) ? result : null;
    }

    @Override
    public E higher(E e) {
        E result = parent.higher(e);
        return (result != null && inRange(result)) ? result : null;
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("SubSet is read-only");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("SubSet is read-only");
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new DescendingSetView<>(this);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                   E toElement, boolean toInclusive) {
        // 현재 범위 내에서만 subSet 생성
        E newFrom = this.fromElement;
        boolean newFromIncl = this.fromInclusive;
        E newTo = this.toElement;
        boolean newToIncl = this.toInclusive;

        if (fromElement != null) {
            if (newFrom == null || comparator().compare(fromElement, newFrom) > 0 ||
                (comparator().compare(fromElement, newFrom) == 0 && !fromInclusive)) {
                newFrom = fromElement;
                newFromIncl = fromInclusive;
            }
        }

        if (toElement != null) {
            if (newTo == null || comparator().compare(toElement, newTo) < 0 ||
                (comparator().compare(toElement, newTo) == 0 && !toInclusive)) {
                newTo = toElement;
                newToIncl = toInclusive;
            }
        }

        return new SubSetView<>(parent, newFrom, newFromIncl, newTo, newToIncl);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return subSet(fromElement, fromInclusive, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return subSet(fromElement, inclusive, toElement, toInclusive);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }
}
```

### 3.4 FxDequeImpl 설계

#### 3.4.1 removeFirstOccurrence() 구현

```java
/**
 * 지정된 요소의 첫 번째 발생을 제거합니다.
 *
 * <p>시간 복잡도: O(N) - 선형 검색 + 삭제
 * <p>공간 복잡도: O(1)
 *
 * <p>불변식:
 * <ul>
 *   <li>INV-V5: 나머지 요소 순서 유지</li>
 * </ul>
 *
 * @param o 제거할 요소 (null 가능)
 * @return 요소가 존재하여 제거되었으면 true
 */
@Override
public boolean removeFirstOccurrence(Object o) {
    writeLock.lock();
    try {
        Iterator<E> iter = iterator();
        while (iter.hasNext()) {
            E element = iter.next();
            if (Objects.equals(element, o)) {
                iter.remove();  // 내부적으로 BTree 삭제 수행
                return true;
            }
        }
        return false;
    } finally {
        writeLock.unlock();
    }
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
    writeLock.lock();
    try {
        // 인덱스 기반 접근이 필요하므로 리스트로 변환
        List<E> elements = new ArrayList<>();
        for (E e : this) {
            elements.add(e);
        }

        // 역순으로 검색
        for (int i = elements.size() - 1; i >= 0; i--) {
            if (Objects.equals(elements.get(i), o)) {
                // 해당 인덱스의 요소 삭제
                removeAt(i);
                return true;
            }
        }
        return false;
    } finally {
        writeLock.unlock();
    }
}

/**
 * 지정된 인덱스의 요소를 삭제합니다.
 *
 * @param index 삭제할 인덱스
 */
private void removeAt(int index) {
    Iterator<E> iter = iterator();
    int current = 0;
    while (iter.hasNext()) {
        iter.next();
        if (current == index) {
            iter.remove();
            return;
        }
        current++;
    }
}
```

#### 3.4.2 descendingIterator() 구현

```java
/**
 * 이 덱의 요소를 역순으로 순회하는 Iterator를 반환합니다.
 *
 * <p>시간 복잡도: O(N) - 전체 요소 수집
 * <p>공간 복잡도: O(N) - 스냅샷 저장
 *
 * @return 역순 Iterator
 */
@Override
public Iterator<E> descendingIterator() {
    // 스냅샷 생성
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

---

## 4. SOLID 원칙 적용

### 4.1 Single Responsibility Principle (SRP)

| 클래스 | 단일 책임 | 검증 |
|--------|----------|------|
| `SubMapView` | 범위 제한된 Map 뷰 제공 | ✓ |
| `DescendingMapView` | 역순 Map 뷰 제공 | ✓ |
| `NavigableKeySetView` | Map의 키 집합 뷰 제공 | ✓ |
| `SubSetView` | 범위 제한된 Set 뷰 제공 | ✓ |
| `RangeUtils` | 범위 검증 유틸리티 | ✓ |

### 4.2 Open/Closed Principle (OCP)

| 확장 포인트 | 설명 |
|------------|------|
| 뷰 클래스 | AbstractSet, AbstractMap 상속으로 확장 가능 |
| Comparator | 커스텀 비교자 지원 |
| Iterator | 표준 인터페이스로 다양한 순회 패턴 지원 |

### 4.3 Liskov Substitution Principle (LSP)

| 인터페이스 | 구현 클래스 | 계약 준수 |
|-----------|------------|-----------|
| NavigableMap | SubMapView, DescendingMapView | ✓ |
| NavigableSet | SubSetView, DescendingSetView, NavigableKeySetView | ✓ |
| Iterator | 모든 descendingIterator | ✓ |

### 4.4 Interface Segregation Principle (ISP)

| 설계 결정 | 근거 |
|----------|------|
| 읽기 전용 뷰 | 수정 연산 분리, 안전성 보장 |
| NavigableMap/NavigableSet 인터페이스 | Java 표준 인터페이스 사용 |

### 4.5 Dependency Inversion Principle (DIP)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        의존성 구조                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  FxNavigableMapImpl                                                  │
│        │                                                             │
│        ├────► NavigableMap<K,V> (인터페이스)                        │
│        │                                                             │
│        └────► SubMapView ────► AbstractMap<K,V>                     │
│               DescendingMapView                                      │
│               NavigableKeySetView ────► AbstractSet<K>              │
│                                                                      │
│  FxNavigableSetImpl                                                  │
│        │                                                             │
│        ├────► NavigableSet<E> (인터페이스)                          │
│        │                                                             │
│        └────► SubSetView ────► AbstractSet<E>                       │
│               DescendingSetView                                      │
│                                                                      │
│  FxDequeImpl                                                         │
│        │                                                             │
│        └────► Deque<E> (인터페이스)                                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.6 SOLID 검증 매트릭스

| 메서드 | SRP | OCP | LSP | ISP | DIP | 총점 |
|--------|-----|-----|-----|-----|-----|------|
| clear() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| descendingMap() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| navigableKeySet() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| descendingKeySet() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| subMap() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| headMap() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| tailMap() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| descendingSet() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| descendingIterator() (Set) | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| subSet() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| headSet() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| tailSet() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| removeFirstOccurrence() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| removeLastOccurrence() | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |
| descendingIterator() (Deque) | ✅ | ✅ | ✅ | ✅ | ✅ | 5/5 |

---

## 5. 구현 단계

### Phase A: 핵심 기능 (1시간)

| 일차 | 작업 | 산출물 |
|------|------|--------|
| 1일 | `clear()` 구현 | `FxNavigableMapImpl.java` 수정 |
| 1일 | `clear()` 테스트 작성 | `FxNavigableMapClearTest.java` |

### Phase B: KeySet 뷰 (1시간)

| 일차 | 작업 | 산출물 |
|------|------|--------|
| 1일 | `NavigableKeySetView` 내부 클래스 구현 | `FxNavigableMapImpl.java` 수정 |
| 1일 | `navigableKeySet()` 구현 및 테스트 | `NavigableKeySetTest.java` |

### Phase C: Descending 뷰 (3시간)

| 일차 | 작업 | 산출물 |
|------|------|--------|
| 2일 | `descendingIterator()` (Set) 구현 | `FxNavigableSetImpl.java` 수정 |
| 2일 | `descendingIterator()` (Deque) 구현 | `FxDequeImpl.java` 수정 |
| 2일 | `DescendingMapView` 구현 | `FxNavigableMapImpl.java` 수정 |
| 2일 | `descendingMap()`, `descendingKeySet()` 구현 | 동상 |
| 2일 | `DescendingSetView` 구현 | `FxNavigableSetImpl.java` 수정 |
| 2일 | `descendingSet()` 구현 | 동상 |
| 2일 | Descending 테스트 작성 | `DescendingViewTest.java` |

### Phase D: 범위 뷰 (4시간)

| 일차 | 작업 | 산출물 |
|------|------|--------|
| 3일 | `RangeUtils` 유틸리티 구현 | `RangeUtils.java` (신규) |
| 3일 | `SubMapView` 구현 | `FxNavigableMapImpl.java` 수정 |
| 3일 | `subMap()`, `headMap()`, `tailMap()` 구현 | 동상 |
| 4일 | `SubSetView` 구현 | `FxNavigableSetImpl.java` 수정 |
| 4일 | `subSet()`, `headSet()`, `tailSet()` 구현 | 동상 |
| 4일 | 범위 뷰 테스트 작성 | `RangeViewTest.java` |

### Phase E: Deque 검색 삭제 (1시간)

| 일차 | 작업 | 산출물 |
|------|------|--------|
| 5일 | `removeFirstOccurrence()` 구현 | `FxDequeImpl.java` 수정 |
| 5일 | `removeLastOccurrence()` 구현 | 동상 |
| 5일 | Deque 검색 삭제 테스트 작성 | `DequeRemoveOccurrenceTest.java` |

### Phase F: 통합 및 품질 검증 (2시간)

| 일차 | 작업 | 산출물 |
|------|------|--------|
| 6일 | 전체 회귀 테스트 실행 | 테스트 리포트 |
| 6일 | 테스트 커버리지 확인 | JaCoCo 리포트 |
| 6일 | JavaDoc 완성 | API 문서 |
| 6일 | 품질 평가 및 문서화 | 평가 문서 |

### 구현 의존성 순서

```
Phase A: 핵심 기능
    │
    └── clear()
              │
              ▼
Phase B: KeySet 뷰
    │
    └── navigableKeySet() ──────────────────────────┐
                                                     │
              ▼                                      │
Phase C: Descending 뷰                              │
    │                                                │
    ├── descendingIterator() (Set, Deque)           │
    │         │                                      │
    │         ▼                                      │
    ├── descendingKeySet() ◄─────────────────────────┘
    │         │
    │         ▼
    ├── descendingSet()
    │         │
    │         ▼
    └── descendingMap()
              │
              ▼
Phase D: 범위 뷰
    │
    ├── RangeUtils
    │
    ├── subMap() ────────────────────────────────────┐
    │         │                                      │
    │         ▼                                      │
    ├── headMap() (subMap 특수 케이스)               │
    │                                                │
    ├── tailMap() (subMap 특수 케이스)               │
    │                                                │
    ├── subSet() ◄───────────────────────────────────┘ (패턴 재사용)
    │         │
    │         ▼
    ├── headSet() (subSet 특수 케이스)
    │
    └── tailSet() (subSet 특수 케이스)
              │
              ▼
Phase E: Deque 검색 삭제
    │
    ├── removeFirstOccurrence() (독립)
    │
    └── removeLastOccurrence() (독립)
              │
              ▼
Phase F: 통합 및 품질 검증
```

### 예상 총 기간: 약 12시간 (1.5일)

---

## 6. 상세 구현 명세

### 6.1 예외 조건 표

| 메서드 | 조건 | 예외 | 메시지 |
|--------|------|------|--------|
| `clear()` | 스토어 닫힘 | `FxException(CLOSED)` | "Store is closed" |
| `clear()` | I/O 오류 | `FxException(IO)` | "Failed to clear: {원인}" |
| `subMap()` | fromKey == null | `NullPointerException` | "fromKey cannot be null" |
| `subMap()` | toKey == null | `NullPointerException` | "toKey cannot be null" |
| `subMap()` | fromKey > toKey | `IllegalArgumentException` | "fromKey > toKey" |
| `headMap()` | toKey == null | `NullPointerException` | "toKey cannot be null" |
| `tailMap()` | fromKey == null | `NullPointerException` | "fromKey cannot be null" |
| `descendingIterator().next()` | 요소 없음 | `NoSuchElementException` | "No more elements" |
| `descendingIterator().remove()` | 호출 시 | `UnsupportedOperationException` | "remove() not supported" |
| `SubMapView.put()` | 호출 시 | `UnsupportedOperationException` | "SubMap is read-only" |
| `SubMapView.remove()` | 호출 시 | `UnsupportedOperationException` | "SubMap is read-only" |

### 6.2 복잡도 표

| 메서드 | 시간 복잡도 | 공간 복잡도 | 비고 |
|--------|------------|------------|------|
| `clear()` | O(N) | O(N) | 키 리스트 임시 저장 |
| `navigableKeySet()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `descendingMap()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `descendingKeySet()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `subMap()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `headMap()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `tailMap()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `descendingSet()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `descendingIterator()` | O(N) | O(N) | 스냅샷 생성 |
| `subSet()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `headSet()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `tailSet()` | O(1) | O(1) | 뷰 래퍼만 생성 |
| `removeFirstOccurrence()` | O(N) | O(1) | 선형 검색 |
| `removeLastOccurrence()` | O(N) | O(N) | 전체 순회 + 리스트 저장 |
| `SubMapView.size()` | O(N) | O(1) | 범위 내 카운트 |
| `SubMapView.get()` | O(log N) | O(1) | 범위 체크 + 부모 위임 |

---

## 7. 테스트 전략

### 7.1 테스트 커버리지 목표

| 메트릭 | 목표 | 측정 방법 |
|--------|------|-----------|
| 라인 커버리지 | ≥95% | JaCoCo |
| 브랜치 커버리지 | ≥90% | JaCoCo |
| 메서드 커버리지 | 100% | JaCoCo |

### 7.2 테스트 케이스 분류

| 카테고리 | 테스트 유형 | 케이스 수 |
|----------|------------|-----------|
| 정상 경로 | Happy path | 45+ |
| 경계값 | 빈 컬렉션, 단일 요소, 최대 범위 | 30+ |
| 예외 경로 | null, 범위 초과, 지원 안 함 | 25+ |
| 라이브 뷰 | 원본 변경 반영 | 10+ |
| 동시성 | 스냅샷 격리 | 5+ |

### 7.3 경계값 테스트 표

| 메서드 | 경계 조건 | 예상 결과 |
|--------|-----------|-----------|
| `clear()` | 빈 맵 | 예외 없음, size=0 유지 |
| `clear()` | 1개 요소 | size=0 |
| `clear()` | 10,000개 요소 | size=0, 성능 ≤1초 |
| `subMap()` | fromKey = toKey, inclusive=true | 1개 요소 |
| `subMap()` | fromKey = toKey, inclusive=false | 빈 뷰 |
| `subMap()` | 전체 범위 | 원본과 동일 |
| `descendingIterator()` | 빈 컬렉션 | hasNext()=false |
| `descendingIterator()` | 단일 요소 | 1회 next() 성공 |
| `removeFirstOccurrence()` | 중복 3개 | 첫 번째만 삭제 |
| `removeLastOccurrence()` | 중복 3개 | 마지막만 삭제 |
| `removeFirstOccurrence(null)` | null 포함 덱 | null 삭제 성공 |

### 7.4 핵심 테스트 코드

#### 7.4.1 clear() 테스트

```java
public class FxNavigableMapClearTest {

    @Test
    public void testClear_emptyMap() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testClear_nonEmptyMap() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
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
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "a");
        map.clear();
        map.put(2L, "b");

        assertEquals(1, map.size());
        assertEquals("b", map.get(2L));
    }

    @Test
    public void testClear_multipleTimes() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "a");
        map.clear();
        map.clear();  // 두 번 호출해도 안전
        assertTrue(map.isEmpty());
    }

    @Test
    public void testClear_performance_10000elements() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (int i = 0; i < 10000; i++) {
            map.put((long) i, "value-" + i);
        }

        long startTime = System.nanoTime();
        map.clear();
        long elapsed = System.nanoTime() - startTime;

        assertTrue(map.isEmpty());
        assertTrue("clear() should complete within 1 second",
                   elapsed < 1_000_000_000L);
    }
}
```

#### 7.4.2 SubMap 테스트

```java
public class SubMapViewTest {

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
        sub.put(1L, "modified");
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
        map.put(3L, "c");

        NavigableMap<Long, String> sub = map.subMap(1L, true, 4L, true);
        assertEquals(3, sub.size());

        // 원본 변경 (범위 내)
        map.put(2L, "modified");

        // 뷰에 반영
        assertEquals("modified", sub.get(2L));
    }
}
```

#### 7.4.3 DescendingIterator 테스트

```java
public class DescendingIteratorTest {

    @Test
    public void testDescendingIterator_emptySet() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        Iterator<Long> it = set.descendingIterator();
        assertFalse(it.hasNext());
    }

    @Test
    public void testDescendingIterator_singleElement() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(42L);

        Iterator<Long> it = set.descendingIterator();

        assertTrue(it.hasNext());
        assertEquals(Long.valueOf(42L), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testDescendingIterator_multipleElements() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
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
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);

        Iterator<Long> it = set.descendingIterator();
        it.next();
        it.next();  // NoSuchElementException
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDescendingIterator_removeNotSupported() {
        NavigableSet<Long> set = store.createSet("test", Long.class);
        set.add(1L);

        Iterator<Long> it = set.descendingIterator();
        it.next();
        it.remove();  // UnsupportedOperationException
    }
}
```

#### 7.4.4 removeFirst/LastOccurrence 테스트

```java
public class DequeRemoveOccurrenceTest {

    @Test
    public void testRemoveFirstOccurrence_found() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("a");

        assertTrue(deque.removeFirstOccurrence("a"));
        assertEquals(2, deque.size());
        assertEquals("b", deque.peekFirst());
    }

    @Test
    public void testRemoveFirstOccurrence_notFound() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");

        assertFalse(deque.removeFirstOccurrence("x"));
        assertEquals(1, deque.size());
    }

    @Test
    public void testRemoveFirstOccurrence_null() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast(null);
        deque.addLast("b");

        assertTrue(deque.removeFirstOccurrence(null));
        assertEquals(2, deque.size());
    }

    @Test
    public void testRemoveLastOccurrence_found() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");
        deque.addLast("b");
        deque.addLast("a");

        assertTrue(deque.removeLastOccurrence("a"));
        assertEquals(2, deque.size());
        assertEquals("b", deque.peekLast());
    }

    @Test
    public void testRemoveLastOccurrence_singleElement() {
        Deque<String> deque = store.createDeque("test", String.class);
        deque.addLast("a");

        assertTrue(deque.removeLastOccurrence("a"));
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testRemoveOccurrence_emptyDeque() {
        Deque<String> deque = store.createDeque("test", String.class);

        assertFalse(deque.removeFirstOccurrence("a"));
        assertFalse(deque.removeLastOccurrence("a"));
    }
}
```

---

## 8. 회귀 테스트 프로세스

### 8.1 회귀 테스트 범위

| 범위 | 포함 테스트 | 실행 시점 |
|------|------------|-----------|
| 신규 메서드 단위 테스트 | 모든 15개 메서드 | 매 커밋 |
| 기존 Map/Set/Deque 테스트 | 기존 모든 테스트 | 매 커밋 |
| 통합 테스트 | 컬렉션 간 상호작용 | Phase 완료 시 |
| 성능 벤치마크 | clear() 성능 | Phase F |
| 전체 회귀 | 모든 테스트 | Phase F |

### 8.2 회귀 테스트 명령어

```bash
# 1. 신규 메서드 테스트만 실행
./gradlew test --tests "*SubMap*" --tests "*Descending*" --tests "*RemoveOccurrence*"

# 2. Map/Set/Deque 전체 테스트
./gradlew test --tests "*NavigableMap*" --tests "*NavigableSet*" --tests "*Deque*"

# 3. 전체 회귀
./gradlew clean test jacocoTestReport

# 4. 커버리지 확인
./gradlew jacocoTestCoverageVerification
```

### 8.3 품질 게이트 (A+ 달성 기준)

| 기준 | A+ 달성 조건 | 측정 도구 |
|------|-------------|-----------|
| Plan-Code 정합성 | 15개 메서드 모두 구현 | 수동 검토 |
| SOLID 원칙 준수 | 5개 원칙 모두 준수 | 코드 리뷰 |
| 테스트 커버리지 | ≥ 95% | JaCoCo |
| 코드 가독성 | 명확한 명명, 적절한 주석 | 코드 리뷰 |
| 예외 처리 | 모든 예외 문서화 및 처리 | 코드 리뷰 |
| 성능 효율성 | 복잡도 목표 달성 | 벤치마크 |
| 문서화 품질 | JavaDoc 100% | 수동 검토 |

---

## 9. 위험 요소 및 대응

### 9.1 기능적 위험

| 위험 | 영향 | 확률 | 대응 |
|------|------|------|------|
| 범위 뷰 일관성 오류 | 잘못된 데이터 반환 | 중간 | 철저한 경계값 테스트 |
| Iterator 동시 수정 | ConcurrentModificationException | 낮음 | 스냅샷 기반 구현 |
| null 처리 불일치 | NullPointerException | 중간 | null 케이스 명시적 처리 |

### 9.2 성능 위험

| 위험 | 영향 | 확률 | 대응 |
|------|------|------|------|
| clear() 대용량 성능 | 긴 처리 시간 | 중간 | 역순 삭제 최적화 |
| SubMapView.size() O(N) | 대용량 뷰 느림 | 낮음 | 문서화로 사용자 인지 |
| descendingIterator() 메모리 | 대용량 스냅샷 | 낮음 | 스트리밍 대안 검토 |

### 9.3 호환성 위험

| 위험 | 영향 | 확률 | 대응 |
|------|------|------|------|
| Java 표준 계약 위반 | API 호환성 문제 | 낮음 | Equivalence 테스트 |
| 기존 코드 영향 | 회귀 버그 | 낮음 | 철저한 회귀 테스트 |

---

## 10. 체크리스트

### 10.1 구현 체크리스트

#### Phase A: 핵심 기능
- [ ] `clear()` 구현
- [ ] `clear()` 테스트 작성 및 통과

#### Phase B: KeySet 뷰
- [ ] `NavigableKeySetView` 내부 클래스 구현
- [ ] `navigableKeySet()` 구현
- [ ] navigableKeySet 테스트 작성 및 통과

#### Phase C: Descending 뷰
- [ ] `descendingIterator()` (Set) 구현
- [ ] `descendingIterator()` (Deque) 구현
- [ ] `DescendingMapView` 구현
- [ ] `descendingMap()` 구현
- [ ] `descendingKeySet()` 구현
- [ ] `DescendingSetView` 구현
- [ ] `descendingSet()` 구현
- [ ] Descending 테스트 작성 및 통과

#### Phase D: 범위 뷰
- [ ] `RangeUtils` 유틸리티 구현
- [ ] `SubMapView` 구현
- [ ] `subMap()` 구현
- [ ] `headMap()` 구현
- [ ] `tailMap()` 구현
- [ ] `SubSetView` 구현
- [ ] `subSet()` 구현
- [ ] `headSet()` 구현
- [ ] `tailSet()` 구현
- [ ] 범위 뷰 테스트 작성 및 통과

#### Phase E: Deque 검색 삭제
- [ ] `removeFirstOccurrence()` 구현
- [ ] `removeLastOccurrence()` 구현
- [ ] Deque 검색 삭제 테스트 작성 및 통과

#### Phase F: 통합 및 품질 검증
- [ ] 전체 회귀 테스트 통과
- [ ] 테스트 커버리지 ≥95% 확인
- [ ] JavaDoc 완성
- [ ] 품질 평가 A+ 달성

### 10.2 품질 기준 체크리스트

| 기준 | 목표 | 확인 |
|------|------|------|
| Plan-Code 정합성 | 100% | [ ] |
| SOLID 원칙 준수 | A+ | [ ] |
| 테스트 커버리지 | ≥ 95% | [ ] |
| 코드 가독성 | A+ | [ ] |
| 예외 처리 | A+ | [ ] |
| 성능 효율성 | 복잡도 달성 | [ ] |
| 문서화 품질 | A+ | [ ] |

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
 * <p>뷰 특성:
 * <ul>
 *   <li>읽기 전용: 수정 연산 불가</li>
 *   <li>라이브 뷰: 원본 변경 시 반영</li>
 * </ul>
 *
 * @param paramName [파라미터 설명] (null 허용 여부)
 * @return [반환값 설명]
 * @throws ExceptionType [예외 조건]
 * @see [관련 메서드/클래스]
 */
```

### A.2 뷰 클래스 JavaDoc 템플릿

```java
/**
 * [뷰 클래스 설명 - 한 줄 요약]
 *
 * <p>[상세 설명 - 책임, 사용법]
 *
 * <p>스레드 안전성: 부모 컬렉션과 동일
 *
 * <p>지원 연산:
 * <ul>
 *   <li>get(), containsKey() 등 읽기 연산</li>
 *   <li>put(), remove() 등 쓰기 연산: UnsupportedOperationException</li>
 * </ul>
 *
 * @param <K> 키 타입
 * @param <V> 값 타입
 * @see NavigableMap
 */
```

---

## 부록 B: 불변식 검증 체크리스트

### B.1 구현 시 확인할 불변식

| 불변식 | 설명 | 검증 방법 |
|--------|------|-----------|
| INV-V1 | 뷰는 원본 변경 시 실시간 반영 | 라이브 뷰 테스트 |
| INV-V2 | fromKey ≤ toKey 보장 | IllegalArgumentException 테스트 |
| INV-V3 | null 처리 일관성 | NullPointerException 테스트 |
| INV-V4 | Iterator는 스냅샷 기반 | 동시 수정 테스트 |
| INV-V5 | Deque FIFO 순서 보장 | 순서 테스트 |

### B.2 테스트에서 검증할 항목

```java
@After
public void verifyInvariants() {
    if (store != null && !store.isClosed()) {
        assertTrue("Store should verify ok", store.verify().ok());
    }
}
```

---

[← 목차로 돌아가기](00.index.md)

---

## 업데이트 기록

| 날짜 | 내용 |
|------|------|
| 2025-12-28 | 초안 작성 |

*작성일: 2025-12-28*
*최종 업데이트: 2025-12-28*
