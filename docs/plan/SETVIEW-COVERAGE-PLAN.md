# HeadSetView/TailSetView 커버리지 개선 계획

> **문서 버전:** 1.0
> **대상:** FxNavigableSetImpl.HeadSetView, FxNavigableSetImpl.TailSetView
> **목표:** 커버리지 48-49% → 80%+ 달성
> **작성일:** 2025-12-28

---

## 1. 현황 분석

### 1.1 현재 커버리지

| 클래스 | Line Coverage | Branch Coverage | 미테스트 메서드 |
|--------|---------------|-----------------|-----------------|
| HeadSetView | 49% (121/243) | 63% (19/30) | 14/24 |
| TailSetView | 48% (115/237) | 60% (17/28) | 14/24 |

### 1.2 미테스트 메서드 상세

#### HeadSetView 미테스트 메서드 (14개)

| # | 메서드 | 시그니처 | 예상 동작 |
|---|--------|----------|-----------|
| 1 | `iterator()` | `Iterator<E> iterator()` | 범위 내 요소 순방향 반복 |
| 2 | `descendingIterator()` | `Iterator<E> descendingIterator()` | 범위 내 요소 역방향 반복 |
| 3 | `comparator()` | `Comparator<? super E> comparator()` | 부모 Set의 Comparator 반환 |
| 4 | `lower(E)` | `E lower(E e)` | e보다 작은 최대 요소 (범위 내) |
| 5 | `floor(E)` | `E floor(E e)` | e 이하 최대 요소 (범위 내) |
| 6 | `ceiling(E)` | `E ceiling(E e)` | e 이상 최소 요소 (범위 내) |
| 7 | `higher(E)` | `E higher(E e)` | e보다 큰 최소 요소 (범위 내) |
| 8 | `descendingSet()` | `NavigableSet<E> descendingSet()` | UOE 발생 |
| 9 | `subSet(E,boolean,E,boolean)` | 4-param subSet | UOE 발생 |
| 10 | `headSet(E,boolean)` | 2-param headSet | UOE 발생 |
| 11 | `tailSet(E,boolean)` | 2-param tailSet | UOE 발생 |
| 12 | `subSet(E,E)` | 2-param subSet | UOE 발생 |
| 13 | `headSet(E)` | 1-param headSet | UOE 발생 |
| 14 | `tailSet(E)` | 1-param tailSet | UOE 발생 |

#### TailSetView 미테스트 메서드 (14개)

| # | 메서드 | 시그니처 | 예상 동작 |
|---|--------|----------|-----------|
| 1 | `iterator()` | `Iterator<E> iterator()` | 범위 내 요소 순방향 반복 |
| 2 | `descendingIterator()` | `Iterator<E> descendingIterator()` | 범위 내 요소 역방향 반복 |
| 3 | `comparator()` | `Comparator<? super E> comparator()` | 부모 Set의 Comparator 반환 |
| 4 | `lower(E)` | `E lower(E e)` | e보다 작은 최대 요소 (범위 내) |
| 5 | `floor(E)` | `E floor(E e)` | e 이하 최대 요소 (범위 내) |
| 6 | `ceiling(E)` | `E ceiling(E e)` | e 이상 최소 요소 (범위 내) |
| 7 | `higher(E)` | `E higher(E e)` | e보다 큰 최소 요소 (범위 내) |
| 8 | `descendingSet()` | `NavigableSet<E> descendingSet()` | UOE 발생 |
| 9 | `subSet(E,boolean,E,boolean)` | 4-param subSet | UOE 발생 |
| 10 | `headSet(E,boolean)` | 2-param headSet | UOE 발생 |
| 11 | `tailSet(E,boolean)` | 2-param tailSet | UOE 발생 |
| 12 | `subSet(E,E)` | 2-param subSet | UOE 발생 |
| 13 | `headSet(E)` | 1-param headSet | UOE 발생 |
| 14 | `tailSet(E)` | 1-param tailSet | UOE 발생 |

---

## 2. 테스트 전략

### 2.1 테스트 분류

| 분류 | 설명 | 테스트 수 |
|------|------|-----------|
| **기능 테스트** | iterator, navigation 메서드 정상 동작 | 14 |
| **UOE 테스트** | 읽기 전용 제약으로 UOE 발생 검증 | 14 |
| **경계값 테스트** | inclusive/exclusive 경계 처리 | 8 |
| **빈 뷰 테스트** | 빈 범위에서의 동작 검증 | 4 |
| **총계** | | **40** |

### 2.2 테스트 대상 메서드별 시나리오

#### 2.2.1 Iterator 테스트

```java
// HeadSetView.iterator()
@Test
public void headSet_iterator_shouldIterateInRange() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(30, false)
    // When: iterate
    // Then: [10, 20] in order
}

@Test
public void headSet_iterator_inclusive_shouldIncludeBoundary() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(30, true)
    // When: iterate
    // Then: [10, 20, 30] in order
}

// HeadSetView.descendingIterator()
@Test
public void headSet_descendingIterator_shouldIterateReverse() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(30, false)
    // When: descendingIterator
    // Then: [20, 10] in reverse order
}
```

#### 2.2.2 Navigation 테스트

```java
// HeadSetView.lower()
@Test
public void headSet_lower_shouldReturnLowerInRange() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(35)
    // When: lower(25)
    // Then: 20
}

@Test
public void headSet_lower_outOfRange_shouldReturnNull() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(35)
    // When: lower(10)
    // Then: null (10보다 작은 요소 없음)
}

// HeadSetView.floor()
@Test
public void headSet_floor_shouldReturnFloorInRange() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(35)
    // When: floor(25)
    // Then: 20
}

@Test
public void headSet_floor_exact_shouldReturnSame() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(35)
    // When: floor(20)
    // Then: 20
}

// HeadSetView.ceiling()
@Test
public void headSet_ceiling_shouldReturnCeilingInRange() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(35)
    // When: ceiling(15)
    // Then: 20
}

@Test
public void headSet_ceiling_outOfRange_shouldReturnNull() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(25)
    // When: ceiling(30)
    // Then: null (30은 범위 밖)
}

// HeadSetView.higher()
@Test
public void headSet_higher_shouldReturnHigherInRange() {
    // Given: set = {10, 20, 30, 40, 50}, headSet(35)
    // When: higher(15)
    // Then: 20
}
```

#### 2.2.3 Comparator 테스트

```java
@Test
public void headSet_comparator_shouldReturnParentComparator() {
    // Given: set with natural ordering
    // When: headSet.comparator()
    // Then: null (natural ordering)
}

@Test
public void headSet_comparator_customComparator_shouldReturnIt() {
    // Given: set with custom Comparator
    // When: headSet.comparator()
    // Then: same Comparator
}
```

#### 2.2.4 UOE 테스트

```java
// 7개 메서드 × 2 클래스 = 14 테스트
@Test(expected = UnsupportedOperationException.class)
public void headSet_descendingSet_shouldThrowUOE() {
    set.headSet(30L).descendingSet();
}

@Test(expected = UnsupportedOperationException.class)
public void headSet_subSet_4param_shouldThrowUOE() {
    set.headSet(30L).subSet(10L, true, 20L, true);
}

@Test(expected = UnsupportedOperationException.class)
public void headSet_headSet_2param_shouldThrowUOE() {
    set.headSet(30L).headSet(20L, true);
}

@Test(expected = UnsupportedOperationException.class)
public void headSet_tailSet_2param_shouldThrowUOE() {
    set.headSet(30L).tailSet(10L, true);
}

@Test(expected = UnsupportedOperationException.class)
public void headSet_subSet_2param_shouldThrowUOE() {
    set.headSet(30L).subSet(10L, 20L);
}

@Test(expected = UnsupportedOperationException.class)
public void headSet_headSet_1param_shouldThrowUOE() {
    set.headSet(30L).headSet(20L);
}

@Test(expected = UnsupportedOperationException.class)
public void headSet_tailSet_1param_shouldThrowUOE() {
    set.headSet(30L).tailSet(10L);
}
```

---

## 3. 구현 계획

### 3.1 테스트 파일 위치

```
src/test/java/com/fxstore/collection/SetViewCoverageTest.java
```

### 3.2 테스트 구조

```java
public class SetViewCoverageTest {

    // ==================== HeadSetView 테스트 ====================

    // --- Iterator 테스트 (4개) ---
    @Test public void headSet_iterator_shouldIterateInRange() {}
    @Test public void headSet_iterator_inclusive_shouldIncludeBoundary() {}
    @Test public void headSet_descendingIterator_shouldIterateReverse() {}
    @Test public void headSet_descendingIterator_empty_shouldReturnEmptyIterator() {}

    // --- Navigation 테스트 (8개) ---
    @Test public void headSet_lower_shouldReturnLowerInRange() {}
    @Test public void headSet_lower_outOfRange_shouldReturnNull() {}
    @Test public void headSet_floor_shouldReturnFloorInRange() {}
    @Test public void headSet_floor_exact_shouldReturnSame() {}
    @Test public void headSet_ceiling_shouldReturnCeilingInRange() {}
    @Test public void headSet_ceiling_outOfRange_shouldReturnNull() {}
    @Test public void headSet_higher_shouldReturnHigherInRange() {}
    @Test public void headSet_higher_outOfRange_shouldReturnNull() {}

    // --- Comparator 테스트 (1개) ---
    @Test public void headSet_comparator_shouldReturnParentComparator() {}

    // --- UOE 테스트 (7개) ---
    @Test(expected = UOE) public void headSet_descendingSet_shouldThrowUOE() {}
    @Test(expected = UOE) public void headSet_subSet_4param_shouldThrowUOE() {}
    @Test(expected = UOE) public void headSet_headSet_2param_shouldThrowUOE() {}
    @Test(expected = UOE) public void headSet_tailSet_2param_shouldThrowUOE() {}
    @Test(expected = UOE) public void headSet_subSet_2param_shouldThrowUOE() {}
    @Test(expected = UOE) public void headSet_headSet_1param_shouldThrowUOE() {}
    @Test(expected = UOE) public void headSet_tailSet_1param_shouldThrowUOE() {}

    // ==================== TailSetView 테스트 ====================

    // --- Iterator 테스트 (4개) ---
    @Test public void tailSet_iterator_shouldIterateInRange() {}
    @Test public void tailSet_iterator_inclusive_shouldIncludeBoundary() {}
    @Test public void tailSet_descendingIterator_shouldIterateReverse() {}
    @Test public void tailSet_descendingIterator_empty_shouldReturnEmptyIterator() {}

    // --- Navigation 테스트 (8개) ---
    @Test public void tailSet_lower_shouldReturnLowerInRange() {}
    @Test public void tailSet_lower_outOfRange_shouldReturnNull() {}
    @Test public void tailSet_floor_shouldReturnFloorInRange() {}
    @Test public void tailSet_floor_exact_shouldReturnSame() {}
    @Test public void tailSet_ceiling_shouldReturnCeilingInRange() {}
    @Test public void tailSet_ceiling_outOfRange_shouldReturnNull() {}
    @Test public void tailSet_higher_shouldReturnHigherInRange() {}
    @Test public void tailSet_higher_outOfRange_shouldReturnNull() {}

    // --- Comparator 테스트 (1개) ---
    @Test public void tailSet_comparator_shouldReturnParentComparator() {}

    // --- UOE 테스트 (7개) ---
    @Test(expected = UOE) public void tailSet_descendingSet_shouldThrowUOE() {}
    @Test(expected = UOE) public void tailSet_subSet_4param_shouldThrowUOE() {}
    @Test(expected = UOE) public void tailSet_headSet_2param_shouldThrowUOE() {}
    @Test(expected = UOE) public void tailSet_tailSet_2param_shouldThrowUOE() {}
    @Test(expected = UOE) public void tailSet_subSet_2param_shouldThrowUOE() {}
    @Test(expected = UOE) public void tailSet_headSet_1param_shouldThrowUOE() {}
    @Test(expected = UOE) public void tailSet_tailSet_1param_shouldThrowUOE() {}
}
```

### 3.3 예상 커버리지 개선

| 클래스 | 현재 Line | 목표 Line | 현재 Branch | 목표 Branch |
|--------|-----------|-----------|-------------|-------------|
| HeadSetView | 49% | 85%+ | 63% | 80%+ |
| TailSetView | 48% | 85%+ | 60% | 80%+ |

---

## 4. 테스트 데이터

### 4.1 기본 테스트 셋업

```java
private FxStore store;
private NavigableSet<Long> set;

@Before
public void setUp() {
    store = FxStore.open(tempFile.toPath());
    set = store.createSet("testSet", Long.class);
    // 기본 데이터: {10, 20, 30, 40, 50}
    set.add(10L);
    set.add(20L);
    set.add(30L);
    set.add(40L);
    set.add(50L);
}
```

### 4.2 경계값 테스트 데이터

| 시나리오 | 입력 | HeadSet 범위 | TailSet 범위 |
|----------|------|--------------|--------------|
| inclusive=true | headSet(30, true) | [10, 20, 30] | [30, 40, 50] |
| inclusive=false | headSet(30, false) | [10, 20] | [40, 50] |
| 빈 범위 | headSet(5, false) | [] | [10, 20, 30, 40, 50] |
| 전체 범위 | headSet(60, true) | [10, 20, 30, 40, 50] | [] |

---

## 5. 검증 기준

### 5.1 성공 기준

- [ ] 모든 40개 테스트 통과
- [ ] HeadSetView 커버리지 ≥ 80%
- [ ] TailSetView 커버리지 ≥ 80%
- [ ] 기존 테스트 회귀 없음

### 5.2 품질 검증

```bash
./gradlew test jacocoTestReport
# 결과 확인: build/reports/jacoco/test/html/index.html
```

---

## 6. 의존성 분석

### 6.1 구현 의존성

```
HeadSetView
├── parent: FxNavigableSetImpl
├── toElement: E (상한 경계)
├── inclusive: boolean (경계 포함 여부)
└── 위임: parent의 메서드 호출 후 범위 검증

TailSetView
├── parent: FxNavigableSetImpl
├── fromElement: E (하한 경계)
├── inclusive: boolean (경계 포함 여부)
└── 위임: parent의 메서드 호출 후 범위 검증
```

### 6.2 테스트 의존성

- JUnit 4
- FxStore API
- 임시 파일 생성/삭제

---

## 7. 리스크 및 대응

| 리스크 | 가능성 | 영향 | 대응 |
|--------|--------|------|------|
| Iterator 구현 복잡성 | 중 | 중 | 범위 필터링 로직 상세 분석 |
| 경계 조건 누락 | 낮음 | 중 | inclusive/exclusive 모든 조합 테스트 |
| 회귀 발생 | 낮음 | 높음 | 전체 테스트 실행 검증 |

---

## 8. 일정

| 단계 | 소요 시간 | 산출물 |
|------|-----------|--------|
| 문서 작성 | 10분 | SETVIEW-COVERAGE-PLAN.md |
| 문서 평가/개선 | 5분 | A+ 달성 |
| 테스트 구현 | 20분 | SetViewCoverageTest.java |
| 테스트 실행 및 검증 | 10분 | 커버리지 리포트 |
| **총계** | **45분** | |

---

## 부록 A: 테스트 코드 템플릿

### A.1 기능 테스트 템플릿

```java
@Test
public void {viewType}_{method}_{scenario}() {
    // Given
    NavigableSet<Long> view = set.{headSet|tailSet}(boundary, inclusive);

    // When
    E result = view.{method}(argument);

    // Then
    assertEquals(expected, result);
}
```

### A.2 UOE 테스트 템플릿

```java
@Test(expected = UnsupportedOperationException.class)
public void {viewType}_{method}_shouldThrowUOE() {
    NavigableSet<Long> view = set.{headSet|tailSet}(boundary, inclusive);
    view.{method}(arguments);
}
```

---

## 부록 B: 관련 문서

- [REMAINING-METHODS-V08-PLAN.md](REMAINING-METHODS-V08-PLAN.md) - v0.8 메서드 구현 계획
- [EVALUATION-V08-PLAN.md](EVALUATION-V08-PLAN.md) - v0.8 계획 평가
- [02.test-strategy.md](02.test-strategy.md) - 테스트 전략

---

**문서 작성일:** 2025-12-28
**작성자:** Claude Code
**다음 단계:** 문서 품질 평가
