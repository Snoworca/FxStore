# UOE 제거 검증 테스트 시나리오

> **문서 버전:** 1.0
> **대상:** UOE 개선 1/2/3순위 전체
> **기반 문서:** [UOE-IMPROVEMENT-INDEX.md](UOE-IMPROVEMENT-INDEX.md)
> **작성일:** 2025-12-29

---

## 목차

1. [개요](#개요)
2. [테스트 카테고리](#테스트-카테고리)
3. [1순위: 뷰 생성 연산 테스트](#1순위-뷰-생성-연산-테스트)
4. [2순위: DescendingView 수정 연산 테스트](#2순위-descendingview-수정-연산-테스트)
5. [3순위: 범위 뷰 수정/Poll/중첩 테스트](#3순위-범위-뷰-수정poll중첩-테스트)
6. [EquivalenceTest 시나리오](#equivalencetest-시나리오)
7. [회귀 테스트 전략](#회귀-테스트-전략)
8. [테스트 파일 구성](#테스트-파일-구성)

---

## 개요

### 목표

66개 UOE 제거 후 모든 기능이 Java TreeMap/TreeSet과 동등하게 동작하는지 검증합니다.

### 테스트 원칙

1. **동등성 검증**: 모든 연산에서 TreeMap/TreeSet과 동일한 결과
2. **예외 조건 검증**: 동일한 조건에서 동일한 예외 발생
3. **상태 일관성**: 뷰 수정이 원본에 정확히 반영
4. **경계 조건**: inclusive/exclusive 경계 처리 정확성

---

## 테스트 카테고리

### 카테고리별 테스트 유형

| 카테고리 | 테스트 유형 | 파일 패턴 |
|---------|------------|----------|
| 기능 테스트 | 개별 메서드 동작 검증 | `*Test.java` |
| 동등성 테스트 | TreeMap/TreeSet 비교 | `*EquivalenceTest.java` |
| 경계 테스트 | inclusive/exclusive 경계 | `*BoundaryTest.java` |
| 통합 테스트 | 뷰 간 상호작용 | `*IntegrationTest.java` |

### 커버리지 목표

| 항목 | 목표 |
|------|------|
| Line Coverage | ≥ 95% |
| Branch Coverage | ≥ 90% |
| Mutation Coverage | ≥ 80% |

---

## 1순위: 뷰 생성 연산 테스트

### 1.1 KeySetView 서브뷰 테스트

**파일:** `KeySetViewSubViewTest.java`

```java
public class KeySetViewSubViewTest {
    private FxNavigableMap<Long, String> map;

    @Before
    public void setUp() {
        map = createMap();  // FxStore에서 생성
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");
        map.put(50L, "E");
    }

    // === subSet 테스트 ===

    @Test
    public void keySet_subSet_shouldReturnCorrectRange() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        SortedSet<Long> subSet = keySet.subSet(20L, 40L);

        assertEquals(2, subSet.size());
        assertTrue(subSet.contains(20L));
        assertTrue(subSet.contains(30L));
        assertFalse(subSet.contains(10L));
        assertFalse(subSet.contains(40L));  // exclusive
    }

    @Test
    public void keySet_subSet_inclusive_shouldIncludeBounds() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> subSet = keySet.subSet(20L, true, 40L, true);

        assertEquals(3, subSet.size());
        assertTrue(subSet.contains(20L));
        assertTrue(subSet.contains(30L));
        assertTrue(subSet.contains(40L));
    }

    @Test
    public void keySet_subSet_exclusive_shouldExcludeBounds() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> subSet = keySet.subSet(20L, false, 40L, false);

        assertEquals(1, subSet.size());
        assertTrue(subSet.contains(30L));
    }

    // === headSet 테스트 ===

    @Test
    public void keySet_headSet_shouldReturnElementsBeforeTo() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        SortedSet<Long> headSet = keySet.headSet(30L);

        assertEquals(2, headSet.size());
        assertTrue(headSet.contains(10L));
        assertTrue(headSet.contains(20L));
        assertFalse(headSet.contains(30L));  // exclusive
    }

    @Test
    public void keySet_headSet_inclusive_shouldIncludeTo() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> headSet = keySet.headSet(30L, true);

        assertEquals(3, headSet.size());
        assertTrue(headSet.contains(30L));
    }

    // === tailSet 테스트 ===

    @Test
    public void keySet_tailSet_shouldReturnElementsFromFrom() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        SortedSet<Long> tailSet = keySet.tailSet(30L);

        assertEquals(3, tailSet.size());
        assertTrue(tailSet.contains(30L));  // inclusive (SortedSet)
        assertTrue(tailSet.contains(40L));
        assertTrue(tailSet.contains(50L));
    }

    @Test
    public void keySet_tailSet_exclusive_shouldExcludeFrom() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        NavigableSet<Long> tailSet = keySet.tailSet(30L, false);

        assertEquals(2, tailSet.size());
        assertFalse(tailSet.contains(30L));
    }

    // === 빈 결과 테스트 ===

    @Test
    public void keySet_subSet_empty_shouldReturnEmptySet() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        SortedSet<Long> subSet = keySet.subSet(100L, 200L);

        assertTrue(subSet.isEmpty());
    }

    // === 뷰-원본 연동 테스트 ===

    @Test
    public void keySet_subSet_shouldReflectMapChanges() {
        NavigableSet<Long> keySet = map.navigableKeySet();
        SortedSet<Long> subSet = keySet.subSet(20L, 40L);

        assertEquals(2, subSet.size());

        // 원본 맵에 추가
        map.put(25L, "X");

        // 뷰에 반영됨
        assertEquals(3, subSet.size());
        assertTrue(subSet.contains(25L));
    }
}
```

### 1.2 Views navigableKeySet/descendingKeySet 테스트

**파일:** `ViewsKeySetTest.java`

```java
public class ViewsKeySetTest {
    private FxNavigableMap<Long, String> map;

    @Before
    public void setUp() {
        map = createMap();
        for (long i = 10; i <= 50; i += 10) {
            map.put(i, "V" + i);
        }
    }

    // === SubMapView.navigableKeySet() ===

    @Test
    public void subMap_navigableKeySet_shouldReturnCorrectSet() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);
        NavigableSet<Long> keySet = subMap.navigableKeySet();

        assertEquals(3, keySet.size());
        assertEquals(Long.valueOf(20L), keySet.first());
        assertEquals(Long.valueOf(40L), keySet.last());
    }

    // === SubMapView.descendingKeySet() ===

    @Test
    public void subMap_descendingKeySet_shouldReturnReversedSet() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);
        NavigableSet<Long> descKeySet = subMap.descendingKeySet();

        assertEquals(3, descKeySet.size());
        assertEquals(Long.valueOf(40L), descKeySet.first());
        assertEquals(Long.valueOf(20L), descKeySet.last());
    }

    // === HeadMapView.navigableKeySet() ===

    @Test
    public void headMap_navigableKeySet_shouldWork() {
        NavigableMap<Long, String> headMap = map.headMap(30L, true);
        NavigableSet<Long> keySet = headMap.navigableKeySet();

        assertEquals(3, keySet.size());
        assertTrue(keySet.contains(10L));
        assertTrue(keySet.contains(30L));
        assertFalse(keySet.contains(40L));
    }

    // === TailMapView.descendingKeySet() ===

    @Test
    public void tailMap_descendingKeySet_shouldReturnReversed() {
        NavigableMap<Long, String> tailMap = map.tailMap(30L, true);
        NavigableSet<Long> descKeySet = tailMap.descendingKeySet();

        Iterator<Long> it = descKeySet.iterator();
        assertEquals(Long.valueOf(50L), it.next());
        assertEquals(Long.valueOf(40L), it.next());
        assertEquals(Long.valueOf(30L), it.next());
    }
}
```

### 1.3 DescendingView 뷰 생성 테스트

**파일:** `DescendingViewCreationTest.java`

```java
public class DescendingViewCreationTest {
    private FxNavigableMap<Long, String> map;

    @Before
    public void setUp() {
        map = createMap();
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");
    }

    // === DescendingMap.subMap() ===

    @Test
    public void descendingMap_subMap_shouldReturnDescendingSubMap() {
        NavigableMap<Long, String> descMap = map.descendingMap();
        // descending에서 subMap(40, 20)은 내림차순 40~20 범위
        NavigableMap<Long, String> subMap = descMap.subMap(40L, true, 20L, true);

        assertEquals(3, subMap.size());
        // 내림차순이므로 first가 40, last가 20
        assertEquals(Long.valueOf(40L), subMap.firstKey());
        assertEquals(Long.valueOf(20L), subMap.lastKey());
    }

    // === DescendingMap.headMap() ===

    @Test
    public void descendingMap_headMap_shouldReturnHigherKeys() {
        NavigableMap<Long, String> descMap = map.descendingMap();
        NavigableMap<Long, String> headMap = descMap.headMap(30L, true);

        // 내림차순에서 head는 30보다 "큰" 키들
        assertEquals(2, headMap.size());
        assertTrue(headMap.containsKey(40L));
        assertTrue(headMap.containsKey(30L));
    }

    // === DescendingMap.tailMap() ===

    @Test
    public void descendingMap_tailMap_shouldReturnLowerKeys() {
        NavigableMap<Long, String> descMap = map.descendingMap();
        NavigableMap<Long, String> tailMap = descMap.tailMap(30L, true);

        // 내림차순에서 tail은 30보다 "작은" 키들
        assertEquals(3, tailMap.size());
        assertTrue(tailMap.containsKey(30L));
        assertTrue(tailMap.containsKey(20L));
        assertTrue(tailMap.containsKey(10L));
    }

    // === Double descending ===

    @Test
    public void descendingMap_descendingMap_shouldReturnOriginalOrder() {
        NavigableMap<Long, String> descMap = map.descendingMap();
        NavigableMap<Long, String> doubleDesc = descMap.descendingMap();

        assertEquals(Long.valueOf(10L), doubleDesc.firstKey());
        assertEquals(Long.valueOf(40L), doubleDesc.lastKey());
    }
}
```

---

## 2순위: DescendingView 수정 연산 테스트

### 2.1 DescendingMapView 수정 테스트

**파일:** `DescendingMapModificationTest.java`

```java
public class DescendingMapModificationTest {
    private FxNavigableMap<Long, String> map;

    @Before
    public void setUp() {
        map = createMap();
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
    }

    // === put() ===

    @Test
    public void descendingMap_put_shouldModifyOriginal() {
        NavigableMap<Long, String> descMap = map.descendingMap();

        V previous = descMap.put(40L, "D");

        assertNull(previous);  // 새 키
        assertEquals("D", map.get(40L));  // 원본에 반영
        assertEquals(4, map.size());
    }

    @Test
    public void descendingMap_put_update_shouldReturnPrevious() {
        NavigableMap<Long, String> descMap = map.descendingMap();

        String previous = descMap.put(20L, "B2");

        assertEquals("B", previous);
        assertEquals("B2", map.get(20L));
    }

    // === remove() ===

    @Test
    public void descendingMap_remove_shouldModifyOriginal() {
        NavigableMap<Long, String> descMap = map.descendingMap();

        String removed = descMap.remove(20L);

        assertEquals("B", removed);
        assertFalse(map.containsKey(20L));
        assertEquals(2, map.size());
    }

    @Test
    public void descendingMap_remove_nonexistent_shouldReturnNull() {
        NavigableMap<Long, String> descMap = map.descendingMap();

        String removed = descMap.remove(100L);

        assertNull(removed);
        assertEquals(3, map.size());
    }

    // === clear() ===

    @Test
    public void descendingMap_clear_shouldClearOriginal() {
        NavigableMap<Long, String> descMap = map.descendingMap();

        descMap.clear();

        assertTrue(map.isEmpty());
        assertTrue(descMap.isEmpty());
    }

    // === putAll() ===

    @Test
    public void descendingMap_putAll_shouldWork() {
        NavigableMap<Long, String> descMap = map.descendingMap();
        Map<Long, String> toAdd = new HashMap<>();
        toAdd.put(40L, "D");
        toAdd.put(50L, "E");

        descMap.putAll(toAdd);

        assertEquals(5, map.size());
        assertEquals("D", map.get(40L));
        assertEquals("E", map.get(50L));
    }

    // === 뷰 반영 테스트 ===

    @Test
    public void descendingMap_modifyOriginal_shouldReflectInView() {
        NavigableMap<Long, String> descMap = map.descendingMap();

        map.put(5L, "Z");

        assertTrue(descMap.containsKey(5L));
        assertEquals("Z", descMap.get(5L));
        assertEquals(Long.valueOf(5L), descMap.lastKey());  // 내림차순에서 마지막
    }
}
```

### 2.2 DescendingSetView 수정 테스트

**파일:** `DescendingSetModificationTest.java`

```java
public class DescendingSetModificationTest {
    private FxNavigableSet<Long> set;

    @Before
    public void setUp() {
        set = createSet();
        set.add(10L);
        set.add(20L);
        set.add(30L);
    }

    // === add() ===

    @Test
    public void descendingSet_add_shouldModifyOriginal() {
        NavigableSet<Long> descSet = set.descendingSet();

        boolean added = descSet.add(40L);

        assertTrue(added);
        assertTrue(set.contains(40L));
        assertEquals(4, set.size());
    }

    @Test
    public void descendingSet_add_duplicate_shouldReturnFalse() {
        NavigableSet<Long> descSet = set.descendingSet();

        boolean added = descSet.add(20L);

        assertFalse(added);
        assertEquals(3, set.size());
    }

    // === remove() ===

    @Test
    public void descendingSet_remove_shouldModifyOriginal() {
        NavigableSet<Long> descSet = set.descendingSet();

        boolean removed = descSet.remove(20L);

        assertTrue(removed);
        assertFalse(set.contains(20L));
        assertEquals(2, set.size());
    }

    // === clear() ===

    @Test
    public void descendingSet_clear_shouldClearOriginal() {
        NavigableSet<Long> descSet = set.descendingSet();

        descSet.clear();

        assertTrue(set.isEmpty());
    }

    // === addAll() ===

    @Test
    public void descendingSet_addAll_shouldWork() {
        NavigableSet<Long> descSet = set.descendingSet();

        descSet.addAll(Arrays.asList(40L, 50L));

        assertEquals(5, set.size());
        assertTrue(set.contains(40L));
        assertTrue(set.contains(50L));
    }

    // === removeAll() ===

    @Test
    public void descendingSet_removeAll_shouldWork() {
        NavigableSet<Long> descSet = set.descendingSet();

        descSet.removeAll(Arrays.asList(10L, 30L));

        assertEquals(1, set.size());
        assertTrue(set.contains(20L));
    }
}
```

---

## 3순위: 범위 뷰 수정/Poll/중첩 테스트

### 3.1 범위 뷰 수정 테스트

**파일:** `RangeViewModificationTest.java`

```java
public class RangeViewModificationTest {
    private FxNavigableMap<Long, String> map;

    @Before
    public void setUp() {
        map = createMap();
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");
        map.put(50L, "E");
    }

    // === SubMapView.put() ===

    @Test
    public void subMap_put_inRange_shouldWork() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);

        subMap.put(25L, "X");

        assertEquals("X", map.get(25L));
        assertEquals(6, map.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_put_belowRange_shouldThrow() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);
        subMap.put(15L, "X");  // 범위 밖
    }

    @Test(expected = IllegalArgumentException.class)
    public void subMap_put_aboveRange_shouldThrow() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);
        subMap.put(45L, "X");  // 범위 밖
    }

    @Test
    public void subMap_put_atExclusiveBound_shouldThrow() {
        NavigableMap<Long, String> subMap = map.subMap(20L, false, 40L, false);

        try {
            subMap.put(20L, "X");  // exclusive 경계
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    // === SubMapView.remove() ===

    @Test
    public void subMap_remove_inRange_shouldWork() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);

        String removed = subMap.remove(30L);

        assertEquals("C", removed);
        assertFalse(map.containsKey(30L));
    }

    @Test
    public void subMap_remove_outOfRange_shouldReturnNull() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);

        String removed = subMap.remove(10L);  // 범위 밖

        assertNull(removed);
        assertTrue(map.containsKey(10L));  // 원본 유지
    }

    // === HeadMapView.put() ===

    @Test
    public void headMap_put_inRange_shouldWork() {
        NavigableMap<Long, String> headMap = map.headMap(30L, true);

        headMap.put(25L, "X");

        assertEquals("X", map.get(25L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void headMap_put_aboveRange_shouldThrow() {
        NavigableMap<Long, String> headMap = map.headMap(30L, true);
        headMap.put(35L, "X");  // 범위 밖
    }

    // === TailMapView.put() ===

    @Test
    public void tailMap_put_inRange_shouldWork() {
        NavigableMap<Long, String> tailMap = map.tailMap(30L, true);

        tailMap.put(35L, "X");

        assertEquals("X", map.get(35L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tailMap_put_belowRange_shouldThrow() {
        NavigableMap<Long, String> tailMap = map.tailMap(30L, true);
        tailMap.put(25L, "X");  // 범위 밖
    }
}
```

### 3.2 Poll 연산 테스트

**파일:** `RangeViewPollTest.java`

```java
public class RangeViewPollTest {
    private FxNavigableMap<Long, String> map;

    @Before
    public void setUp() {
        map = createMap();
        map.put(10L, "A");
        map.put(20L, "B");
        map.put(30L, "C");
        map.put(40L, "D");
        map.put(50L, "E");
    }

    // === SubMapView.pollFirstEntry() ===

    @Test
    public void subMap_pollFirstEntry_shouldRemoveAndReturn() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);

        Entry<Long, String> entry = subMap.pollFirstEntry();

        assertEquals(Long.valueOf(20L), entry.getKey());
        assertEquals("B", entry.getValue());
        assertFalse(map.containsKey(20L));  // 원본에서 삭제
        assertEquals(4, map.size());
    }

    // === SubMapView.pollLastEntry() ===

    @Test
    public void subMap_pollLastEntry_shouldRemoveAndReturn() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);

        Entry<Long, String> entry = subMap.pollLastEntry();

        assertEquals(Long.valueOf(40L), entry.getKey());
        assertEquals("D", entry.getValue());
        assertFalse(map.containsKey(40L));
    }

    // === 빈 뷰에서 poll ===

    @Test
    public void subMap_pollFirstEntry_empty_shouldReturnNull() {
        NavigableMap<Long, String> subMap = map.subMap(100L, true, 200L, true);

        Entry<Long, String> entry = subMap.pollFirstEntry();

        assertNull(entry);
    }

    // === HeadMapView.pollFirstEntry() ===

    @Test
    public void headMap_pollFirstEntry_shouldRemoveFirst() {
        NavigableMap<Long, String> headMap = map.headMap(30L, true);

        Entry<Long, String> entry = headMap.pollFirstEntry();

        assertEquals(Long.valueOf(10L), entry.getKey());
        assertFalse(map.containsKey(10L));
    }

    // === TailMapView.pollLastEntry() ===

    @Test
    public void tailMap_pollLastEntry_shouldRemoveLast() {
        NavigableMap<Long, String> tailMap = map.tailMap(30L, true);

        Entry<Long, String> entry = tailMap.pollLastEntry();

        assertEquals(Long.valueOf(50L), entry.getKey());
        assertFalse(map.containsKey(50L));
    }

    // === DescendingMap.pollFirstEntry() ===

    @Test
    public void descendingMap_pollFirstEntry_shouldRemoveLastFromOriginal() {
        NavigableMap<Long, String> descMap = map.descendingMap();

        Entry<Long, String> entry = descMap.pollFirstEntry();

        // 내림차순에서 first는 원본의 last
        assertEquals(Long.valueOf(50L), entry.getKey());
        assertFalse(map.containsKey(50L));
    }

    // === 연속 poll 테스트 ===

    @Test
    public void subMap_pollAll_shouldEmptyView() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 40L, true);

        subMap.pollFirstEntry();  // 20
        subMap.pollFirstEntry();  // 30
        subMap.pollFirstEntry();  // 40

        assertTrue(subMap.isEmpty());
        assertNull(subMap.pollFirstEntry());
        assertEquals(2, map.size());  // 10, 50 남음
    }
}
```

### 3.3 중첩 뷰 테스트

**파일:** `NestedViewTest.java`

```java
public class NestedViewTest {
    private FxNavigableMap<Long, String> map;

    @Before
    public void setUp() {
        map = createMap();
        for (long i = 10; i <= 100; i += 10) {
            map.put(i, "V" + i);
        }
    }

    // === SubMap에서 SubMap ===

    @Test
    public void subMap_subMap_shouldIntersectRanges() {
        NavigableMap<Long, String> subMap1 = map.subMap(20L, true, 80L, true);
        NavigableMap<Long, String> subMap2 = subMap1.subMap(40L, true, 60L, true);

        assertEquals(3, subMap2.size());  // 40, 50, 60
        assertEquals(Long.valueOf(40L), subMap2.firstKey());
        assertEquals(Long.valueOf(60L), subMap2.lastKey());
    }

    // === HeadMap에서 TailMap ===

    @Test
    public void headMap_tailMap_shouldCreateSubMap() {
        NavigableMap<Long, String> headMap = map.headMap(70L, true);
        NavigableMap<Long, String> nested = headMap.tailMap(30L, true);

        // 30 <= key <= 70
        assertEquals(5, nested.size());
        assertTrue(nested.containsKey(30L));
        assertTrue(nested.containsKey(70L));
        assertFalse(nested.containsKey(80L));
    }

    // === TailMap에서 HeadMap ===

    @Test
    public void tailMap_headMap_shouldCreateSubMap() {
        NavigableMap<Long, String> tailMap = map.tailMap(30L, true);
        NavigableMap<Long, String> nested = tailMap.headMap(70L, true);

        // 30 <= key <= 70
        assertEquals(5, nested.size());
    }

    // === SubMap에서 HeadMap ===

    @Test
    public void subMap_headMap_shouldRestrictUpper() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 80L, true);
        NavigableMap<Long, String> headMap = subMap.headMap(50L, true);

        // 20 <= key <= 50
        assertEquals(4, headMap.size());
        assertTrue(headMap.containsKey(20L));
        assertTrue(headMap.containsKey(50L));
        assertFalse(headMap.containsKey(60L));
    }

    // === SubMap에서 TailMap ===

    @Test
    public void subMap_tailMap_shouldRestrictLower() {
        NavigableMap<Long, String> subMap = map.subMap(20L, true, 80L, true);
        NavigableMap<Long, String> tailMap = subMap.tailMap(50L, true);

        // 50 <= key <= 80
        assertEquals(4, tailMap.size());
        assertTrue(tailMap.containsKey(50L));
        assertTrue(tailMap.containsKey(80L));
        assertFalse(tailMap.containsKey(40L));
    }

    // === 범위 초과 예외 ===

    @Test(expected = IllegalArgumentException.class)
    public void subMap_subMap_exceedingRange_shouldThrow() {
        NavigableMap<Long, String> subMap = map.subMap(30L, true, 60L, true);
        subMap.subMap(20L, true, 70L, true);  // 원래 범위 초과
    }

    @Test(expected = IllegalArgumentException.class)
    public void headMap_headMap_exceedingRange_shouldThrow() {
        NavigableMap<Long, String> headMap = map.headMap(50L, true);
        headMap.headMap(60L, true);  // 원래 상한 초과
    }

    // === 3단계 중첩 ===

    @Test
    public void tripleNesting_shouldWork() {
        NavigableMap<Long, String> level1 = map.subMap(20L, true, 90L, true);
        NavigableMap<Long, String> level2 = level1.subMap(30L, true, 80L, true);
        NavigableMap<Long, String> level3 = level2.subMap(40L, true, 70L, true);

        assertEquals(4, level3.size());  // 40, 50, 60, 70
    }

    // === 중첩 뷰 수정 ===

    @Test
    public void nestedView_modification_shouldReflectToOriginal() {
        NavigableMap<Long, String> subMap = map.subMap(30L, true, 70L, true);
        NavigableMap<Long, String> nested = subMap.subMap(40L, true, 60L, true);

        nested.put(55L, "NEW");

        assertEquals("NEW", map.get(55L));
        assertEquals("NEW", subMap.get(55L));
        assertEquals("NEW", nested.get(55L));
    }
}
```

---

## EquivalenceTest 시나리오

### TreeMap 동등성 테스트

**파일:** `UOEEquivalenceTest.java`

```java
public class UOEEquivalenceTest {
    private TreeMap<Long, String> treeMap;
    private FxNavigableMap<Long, String> fxMap;

    @Before
    public void setUp() {
        treeMap = new TreeMap<>();
        fxMap = createMap();

        // 동일 데이터 삽입
        for (long i = 10; i <= 100; i += 10) {
            treeMap.put(i, "V" + i);
            fxMap.put(i, "V" + i);
        }
    }

    // === 1순위: 뷰 생성 동등성 ===

    @Test
    public void keySet_subSet_shouldMatchTreeMap() {
        SortedSet<Long> treeSubSet = treeMap.navigableKeySet().subSet(30L, 70L);
        SortedSet<Long> fxSubSet = fxMap.navigableKeySet().subSet(30L, 70L);

        assertEquals(treeSubSet.size(), fxSubSet.size());
        assertTrue(treeSubSet.containsAll(fxSubSet));
        assertTrue(fxSubSet.containsAll(treeSubSet));
    }

    @Test
    public void descendingMap_subMap_shouldMatchTreeMap() {
        NavigableMap<Long, String> treeDesc = treeMap.descendingMap();
        NavigableMap<Long, String> fxDesc = fxMap.descendingMap();

        NavigableMap<Long, String> treeSub = treeDesc.subMap(70L, true, 30L, true);
        NavigableMap<Long, String> fxSub = fxDesc.subMap(70L, true, 30L, true);

        assertEquals(treeSub.size(), fxSub.size());
        assertEquals(treeSub.firstKey(), fxSub.firstKey());
        assertEquals(treeSub.lastKey(), fxSub.lastKey());
    }

    // === 2순위: Descending 수정 동등성 ===

    @Test
    public void descendingMap_put_shouldMatchTreeMap() {
        treeMap.descendingMap().put(55L, "NEW");
        fxMap.descendingMap().put(55L, "NEW");

        assertEquals(treeMap.get(55L), fxMap.get(55L));
        assertEquals(treeMap.size(), fxMap.size());
    }

    @Test
    public void descendingMap_remove_shouldMatchTreeMap() {
        String treeRemoved = treeMap.descendingMap().remove(50L);
        String fxRemoved = fxMap.descendingMap().remove(50L);

        assertEquals(treeRemoved, fxRemoved);
        assertEquals(treeMap.containsKey(50L), fxMap.containsKey(50L));
    }

    // === 3순위: 범위 뷰 수정 동등성 ===

    @Test
    public void subMap_put_inRange_shouldMatchTreeMap() {
        treeMap.subMap(30L, true, 70L, true).put(55L, "NEW");
        fxMap.subMap(30L, true, 70L, true).put(55L, "NEW");

        assertEquals(treeMap.get(55L), fxMap.get(55L));
    }

    @Test
    public void subMap_put_outOfRange_bothShouldThrow() {
        Exception treeEx = null, fxEx = null;

        try {
            treeMap.subMap(30L, true, 70L, true).put(100L, "X");
        } catch (IllegalArgumentException e) {
            treeEx = e;
        }

        try {
            fxMap.subMap(30L, true, 70L, true).put(100L, "X");
        } catch (IllegalArgumentException e) {
            fxEx = e;
        }

        assertNotNull("TreeMap should throw", treeEx);
        assertNotNull("FxMap should throw", fxEx);
    }

    // === Poll 동등성 ===

    @Test
    public void subMap_pollFirstEntry_shouldMatchTreeMap() {
        Entry<Long, String> treeEntry = treeMap.subMap(30L, true, 70L, true).pollFirstEntry();
        Entry<Long, String> fxEntry = fxMap.subMap(30L, true, 70L, true).pollFirstEntry();

        assertEquals(treeEntry.getKey(), fxEntry.getKey());
        assertEquals(treeEntry.getValue(), fxEntry.getValue());
        assertEquals(treeMap.containsKey(30L), fxMap.containsKey(30L));
    }

    // === 중첩 뷰 동등성 ===

    @Test
    public void nestedSubMap_shouldMatchTreeMap() {
        NavigableMap<Long, String> treeSub1 = treeMap.subMap(20L, true, 80L, true);
        NavigableMap<Long, String> treeSub2 = treeSub1.subMap(40L, true, 60L, true);

        NavigableMap<Long, String> fxSub1 = fxMap.subMap(20L, true, 80L, true);
        NavigableMap<Long, String> fxSub2 = fxSub1.subMap(40L, true, 60L, true);

        assertEquals(treeSub2.size(), fxSub2.size());
        assertEquals(treeSub2.keySet(), fxSub2.keySet());
    }
}
```

---

## 회귀 테스트 전략

### 실행 순서

```bash
# 1. 기존 테스트 모두 통과 확인
./gradlew test

# 2. UOE 관련 새 테스트 실행
./gradlew test --tests "*ViewTest"
./gradlew test --tests "*EquivalenceTest"

# 3. 커버리지 리포트 생성
./gradlew jacocoTestReport

# 4. 전체 회귀 테스트
./gradlew clean test jacocoTestReport
```

### 기존 UOE 테스트 업데이트

다음 패턴의 기존 테스트는 삭제하거나 수정해야 합니다:

```java
// 삭제 대상
@Test(expected = UnsupportedOperationException.class)
public void descendingMap_put_shouldThrowUOE() {
    map.descendingMap().put(1L, "X");
}

// 대체 테스트
@Test
public void descendingMap_put_shouldWork() {
    map.descendingMap().put(1L, "X");
    assertEquals("X", map.get(1L));
}
```

### 영향받는 기존 테스트 파일

| 파일 | 예상 변경 |
|------|----------|
| FxNavigableMapImplTest.java | UOE 테스트 → 기능 테스트로 변경 |
| FxNavigableSetImplTest.java | UOE 테스트 → 기능 테스트로 변경 |
| DescendingMapViewTest.java | 수정 연산 테스트 추가 |
| SubMapViewTest.java | 수정/poll 테스트 추가 |

---

## 테스트 파일 구성

### 디렉토리 구조

```
src/test/java/com/fxstore/collection/
├── view/
│   ├── KeySetViewSubViewTest.java        # 1순위
│   ├── ViewsKeySetTest.java              # 1순위
│   ├── DescendingViewCreationTest.java   # 1순위
│   ├── DescendingMapModificationTest.java # 2순위
│   ├── DescendingSetModificationTest.java # 2순위
│   ├── RangeViewModificationTest.java    # 3순위
│   ├── RangeViewPollTest.java            # 3순위
│   ├── NestedViewTest.java               # 3순위
│   └── UOEEquivalenceTest.java           # 동등성
└── FxNavigableMapImplTest.java           # 기존 (수정)
```

### 테스트 명명 규칙

```
{대상클래스}_{메서드}_{조건}_{예상결과}

예:
- subMap_put_inRange_shouldWork
- subMap_put_outOfRange_shouldThrow
- descendingMap_pollFirstEntry_empty_shouldReturnNull
```

---

## 체크리스트

### 1순위 테스트 (24개 UOE)

- [ ] KeySetViewSubViewTest.java 작성
- [ ] ViewsKeySetTest.java 작성
- [ ] DescendingViewCreationTest.java 작성
- [ ] 1순위 EquivalenceTest 추가

### 2순위 테스트 (6개 UOE)

- [ ] DescendingMapModificationTest.java 작성
- [ ] DescendingSetModificationTest.java 작성
- [ ] 2순위 EquivalenceTest 추가

### 3순위 테스트 (36개 UOE)

- [ ] RangeViewModificationTest.java 작성
- [ ] RangeViewPollTest.java 작성
- [ ] NestedViewTest.java 작성
- [ ] 3순위 EquivalenceTest 추가

### 회귀 테스트

- [ ] 기존 UOE 테스트 삭제/수정 목록 확인
- [ ] 전체 테스트 통과 확인
- [ ] 커버리지 목표 달성 확인

---

[← UOE 개선 인덱스로 돌아가기](UOE-IMPROVEMENT-INDEX.md)
