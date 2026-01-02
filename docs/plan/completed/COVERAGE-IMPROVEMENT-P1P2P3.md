# View 커버리지 종합 개선 계획 (P1/P2/P3)

> **문서 버전:** 1.1
> **대상:** NavigableMap Views, NavigableSet Views, CacheStats
> **목표:** 전체 커버리지 91% → 93%+ 달성
> **작성일:** 2025-12-29
> **평가:** A+ (98.55/100)

---

## 목차

1. [현황 분석](#1-현황-분석)
2. [P1: NavigableMap Views 테스트 계획](#2-p1-navigablemap-views-테스트-계획)
3. [P2: NavigableSet Views 테스트 계획](#3-p2-navigableset-views-테스트-계획)
4. [P3: CacheStats 테스트 계획](#4-p3-cachestats-테스트-계획)
5. [테스트 파일 구조](#5-테스트-파일-구조)
6. [예상 커버리지 개선](#6-예상-커버리지-개선)
7. [검증 기준](#7-검증-기준)
8. [구현 순서](#8-구현-순서)
9. [리스크 및 대응](#9-리스크-및-대응)
10. [관련 문서](#10-관련-문서)

---

## 1. 현황 분석

### 1.1 전체 프로젝트 상태

| 지표 | 현재값 | 목표값 |
|------|--------|--------|
| Instructions | 91% | 93%+ |
| Branches | 85% | 88%+ |
| 미흡 클래스 수 | 7개 | 0개 |

### 1.2 우선순위별 대상

#### P1: NavigableMap Views (긴급)

| 클래스 | Instructions | Branches | 미테스트 Lines |
|--------|--------------|----------|----------------|
| FxNavigableMapImpl.TailMapView | 63% (233/367) | 62% (36/58) | 26 |
| FxNavigableMapImpl.HeadMapView | 64% (243/375) | 63% (38/60) | 26 |
| FxNavigableMapImpl.KeySetView | 65% (124/190) | 65% (13/20) | 11 |
| FxNavigableMapImpl.DescendingMapView | 69% (121/173) | n/a | 10 |

**목표**: 각 클래스 85%+ 달성

#### P2: NavigableSet Views (중요)

| 클래스 | Instructions | Branches | 미테스트 Lines |
|--------|--------------|----------|----------------|
| FxNavigableSetImpl.SubSetView | 66% (196/295) | 81% (36/44) | 13 |
| FxNavigableSetImpl.DescendingSetView | 72% (80/111) | n/a | 6 |

**목표**: 각 클래스 85%+ 달성

#### P3: CacheStats (일반)

| 클래스 | Instructions | Branches | 미테스트 Lines |
|--------|--------------|----------|----------------|
| ConcurrentPageCache.CacheStats | 56% (49/87) | 50% | 4 |

**목표**: 80%+ 달성

---

## 2. P1: NavigableMap Views 테스트 계획

### 2.1 TailMapView 테스트 (14개)

#### 2.1.1 Iterator 테스트 (4개)

```java
// T-IT-1: tailMap iterator 순방향 반복
@Test
public void tailMap_iterator_shouldIterateFromKey() {
    // Given: map = {10→A, 20→B, 30→C, 40→D, 50→E}
    // When: tailMap(25).keySet().iterator()
    // Then: [30, 40, 50] 순서
}

// T-IT-2: tailMap inclusive 경계 포함
@Test
public void tailMap_iterator_inclusive_shouldIncludeBoundary() {
    // Given: map = {10→A, 20→B, 30→C, 40→D, 50→E}
    // When: tailMap(30, true).keySet().iterator()
    // Then: [30, 40, 50] 순서
}

// T-IT-3: tailMap exclusive 경계 제외
@Test
public void tailMap_iterator_exclusive_shouldExcludeBoundary() {
    // Given: map = {10→A, 20→B, 30→C, 40→D, 50→E}
    // When: tailMap(30, false).keySet().iterator()
    // Then: [40, 50] 순서
}

// T-IT-4: tailMap descendingIterator
@Test
public void tailMap_descendingKeyIterator_shouldIterateReverse() {
    // Given: tailMap(25)
    // When: descendingKeySet().iterator()
    // Then: [50, 40, 30] 역순
}
```

#### 2.1.2 Navigation 테스트 (6개)

```java
// T-NAV-1: lowerEntry
@Test
public void tailMap_lowerEntry_shouldReturnLowerInRange() {
    // Given: tailMap(25) = {30→C, 40→D, 50→E}
    // When: lowerEntry(40)
    // Then: 30→C
}

// T-NAV-2: floorEntry
@Test
public void tailMap_floorEntry_shouldReturnFloorInRange() {
    // Given: tailMap(25) = {30→C, 40→D, 50→E}
    // When: floorEntry(35)
    // Then: 30→C
}

// T-NAV-3: ceilingEntry
@Test
public void tailMap_ceilingEntry_shouldReturnCeilingInRange() {
    // Given: tailMap(25) = {30→C, 40→D, 50→E}
    // When: ceilingEntry(35)
    // Then: 40→D
}

// T-NAV-4: higherEntry
@Test
public void tailMap_higherEntry_shouldReturnHigherInRange() {
    // Given: tailMap(25) = {30→C, 40→D, 50→E}
    // When: higherEntry(30)
    // Then: 40→D
}

// T-NAV-5: firstEntry
@Test
public void tailMap_firstEntry_shouldReturnFirstInRange() {
    // Given: tailMap(25) = {30→C, 40→D, 50→E}
    // When: firstEntry()
    // Then: 30→C
}

// T-NAV-6: lastEntry
@Test
public void tailMap_lastEntry_shouldReturnLastInRange() {
    // Given: tailMap(25) = {30→C, 40→D, 50→E}
    // When: lastEntry()
    // Then: 50→E
}
```

#### 2.1.3 경계 및 특수 케이스 테스트 (4개)

```java
// T-EDGE-1: 빈 tailMap
@Test
public void tailMap_empty_shouldReturnEmptyView() {
    // Given: map = {10→A, 20→B, 30→C}
    // When: tailMap(100)
    // Then: isEmpty() == true
}

// T-EDGE-2: 범위 밖 navigation
@Test
public void tailMap_lowerEntry_outOfRange_shouldReturnNull() {
    // Given: tailMap(25) = {30→C, 40→D, 50→E}
    // When: lowerEntry(30)
    // Then: null (30보다 작은 요소가 범위 내 없음)
}

// T-EDGE-3: containsKey 범위 검사
@Test
public void tailMap_containsKey_shouldCheckRange() {
    // Given: tailMap(25) = {30→C, 40→D, 50→E}
    // When: containsKey(20)
    // Then: false (범위 밖)
}

// T-EDGE-4: get 범위 검사
@Test
public void tailMap_get_outOfRange_shouldReturnNull() {
    // Given: tailMap(25), original map has 10→A
    // When: get(10)
    // Then: null (범위 밖)
}
```

### 2.2 HeadMapView 테스트 (14개)

#### 2.2.1 Iterator 테스트 (4개)

```java
// H-IT-1: headMap iterator 순방향 반복
@Test
public void headMap_iterator_shouldIterateToKey() {
    // Given: map = {10→A, 20→B, 30→C, 40→D, 50→E}
    // When: headMap(35).keySet().iterator()
    // Then: [10, 20, 30] 순서
}

// H-IT-2: headMap inclusive 경계 포함
@Test
public void headMap_iterator_inclusive_shouldIncludeBoundary() {
    // Given: headMap(30, true)
    // Then: [10, 20, 30] 순서
}

// H-IT-3: headMap exclusive 경계 제외
@Test
public void headMap_iterator_exclusive_shouldExcludeBoundary() {
    // Given: headMap(30, false)
    // Then: [10, 20] 순서
}

// H-IT-4: headMap descendingIterator
@Test
public void headMap_descendingKeyIterator_shouldIterateReverse() {
    // Given: headMap(35)
    // When: descendingKeySet().iterator()
    // Then: [30, 20, 10] 역순
}
```

#### 2.2.2 Navigation 테스트 (6개)

```java
// H-NAV-1 ~ H-NAV-6: TailMapView와 대칭 구조
// lowerEntry, floorEntry, ceilingEntry, higherEntry, firstEntry, lastEntry
```

#### 2.2.3 경계 및 특수 케이스 테스트 (4개)

```java
// H-EDGE-1 ~ H-EDGE-4: TailMapView와 대칭 구조
```

### 2.3 KeySetView 테스트 (10개)

```java
// KS-1: iterator
@Test
public void keySet_iterator_shouldIterateAllKeys() {
    // Given: map = {10→A, 20→B, 30→C}
    // When: navigableKeySet().iterator()
    // Then: [10, 20, 30] 순서
}

// KS-2: descendingIterator
@Test
public void keySet_descendingIterator_shouldIterateReverse() {
    // Given: map = {10→A, 20→B, 30→C}
    // When: navigableKeySet().descendingIterator()
    // Then: [30, 20, 10] 역순
}

// KS-3 ~ KS-6: lower, floor, ceiling, higher
// KS-7 ~ KS-8: first, last
// KS-9 ~ KS-10: headSet, tailSet (UOE 검증)
```

### 2.4 DescendingMapView 테스트 (10개)

```java
// DM-1: 역순 iteration
@Test
public void descendingMap_iterator_shouldIterateReverse() {
    // Given: map = {10→A, 20→B, 30→C}
    // When: descendingMap().keySet().iterator()
    // Then: [30, 20, 10] 역순
}

// DM-2: firstEntry는 원본의 lastEntry
@Test
public void descendingMap_firstEntry_shouldReturnOriginalLast() {
    // Given: map = {10→A, 20→B, 30→C}
    // When: descendingMap().firstEntry()
    // Then: 30→C
}

// DM-3 ~ DM-10: navigation, containsKey, get, size 등
```

---

## 3. P2: NavigableSet Views 테스트 계획

### 3.1 SubSetView 테스트 (12개)

#### 3.1.1 기본 기능 테스트 (6개)

```java
// SS-1: subSet iterator 범위 내 반복
@Test
public void subSet_iterator_shouldIterateInRange() {
    // Given: set = {10, 20, 30, 40, 50}, subSet(15, 45)
    // When: iterator()
    // Then: [20, 30, 40] 순서
}

// SS-2: subSet inclusive/exclusive 조합
@Test
public void subSet_inclusiveExclusive_shouldRespectBounds() {
    // Given: set = {10, 20, 30, 40, 50}
    // When: subSet(20, true, 40, false)
    // Then: [20, 30] (20 포함, 40 제외)
}

// SS-3: lower
@Test
public void subSet_lower_shouldReturnLowerInRange() {
    // Given: subSet(15, 45) = {20, 30, 40}
    // When: lower(30)
    // Then: 20
}

// SS-4: floor
@Test
public void subSet_floor_shouldReturnFloorInRange() {
    // Given: subSet(15, 45) = {20, 30, 40}
    // When: floor(25)
    // Then: 20
}

// SS-5: ceiling
@Test
public void subSet_ceiling_shouldReturnCeilingInRange() {
    // Given: subSet(15, 45) = {20, 30, 40}
    // When: ceiling(25)
    // Then: 30
}

// SS-6: higher
@Test
public void subSet_higher_shouldReturnHigherInRange() {
    // Given: subSet(15, 45) = {20, 30, 40}
    // When: higher(25)
    // Then: 30
}
```

#### 3.1.2 경계 및 특수 케이스 테스트 (6개)

```java
// SS-7: 빈 subSet
@Test
public void subSet_empty_shouldReturnEmptyView() {
    // Given: set = {10, 20, 30}
    // When: subSet(15, 18)
    // Then: isEmpty() == true
}

// SS-8: 범위 밖 lower
@Test
public void subSet_lower_outOfRange_shouldReturnNull() {
    // Given: subSet(15, 45) = {20, 30, 40}
    // When: lower(20)
    // Then: null (20보다 작은 요소 없음)
}

// SS-9: first/last
@Test
public void subSet_first_last_shouldReturnBoundaryElements() {
    // Given: subSet(15, 45) = {20, 30, 40}
    // When: first(), last()
    // Then: 20, 40
}

// SS-10: contains 범위 검사
@Test
public void subSet_contains_shouldCheckRange() {
    // Given: subSet(15, 45) = {20, 30, 40}
    // When: contains(10)
    // Then: false (범위 밖)
}

// SS-11: descendingIterator
@Test
public void subSet_descendingIterator_shouldIterateReverse() {
    // Given: subSet(15, 45) = {20, 30, 40}
    // When: descendingIterator()
    // Then: [40, 30, 20] 역순
}

// SS-12: size
@Test
public void subSet_size_shouldReturnRangeCount() {
    // Given: subSet(15, 45) = {20, 30, 40}
    // When: size()
    // Then: 3
}
```

### 3.2 DescendingSetView 테스트 (6개)

```java
// DS-1: 역순 iteration
@Test
public void descendingSet_iterator_shouldIterateReverse() {
    // Given: set = {10, 20, 30}
    // When: descendingSet().iterator()
    // Then: [30, 20, 10] 역순
}

// DS-2: first는 원본의 last
@Test
public void descendingSet_first_shouldReturnOriginalLast() {
    // Given: set = {10, 20, 30}
    // When: descendingSet().first()
    // Then: 30
}

// DS-3: last는 원본의 first
@Test
public void descendingSet_last_shouldReturnOriginalFirst() {
    // Given: set = {10, 20, 30}
    // When: descendingSet().last()
    // Then: 10
}

// DS-4: lower는 원본의 higher (역전)
@Test
public void descendingSet_lower_shouldReturnOriginalHigher() {
    // Given: set = {10, 20, 30}
    // When: descendingSet().lower(20)
    // Then: 30 (역순에서 20보다 "작은" = 원본에서 더 큰)
}

// DS-5: descendingIterator는 원본 순서
@Test
public void descendingSet_descendingIterator_shouldReturnOriginalOrder() {
    // Given: set = {10, 20, 30}
    // When: descendingSet().descendingIterator()
    // Then: [10, 20, 30] 원본 순서
}

// DS-6: contains
@Test
public void descendingSet_contains_shouldWork() {
    // Given: set = {10, 20, 30}
    // When: descendingSet().contains(20)
    // Then: true
}
```

---

## 4. P3: CacheStats 테스트 계획

### 4.1 CacheStats 테스트 (8개)

```java
// CS-1: 초기 상태 검증
@Test
public void cacheStats_initial_shouldBeZero() {
    // Given: new CacheStats()
    // Then: hits=0, misses=0, evictions=0
}

// CS-2: hit 기록
@Test
public void cacheStats_recordHit_shouldIncrementHits() {
    // Given: stats
    // When: recordHit()
    // Then: hits == 1
}

// CS-3: miss 기록
@Test
public void cacheStats_recordMiss_shouldIncrementMisses() {
    // Given: stats
    // When: recordMiss()
    // Then: misses == 1
}

// CS-4: eviction 기록
@Test
public void cacheStats_recordEviction_shouldIncrementEvictions() {
    // Given: stats
    // When: recordEviction()
    // Then: evictions == 1
}

// CS-5: hitRate 계산
@Test
public void cacheStats_hitRate_shouldCalculateCorrectly() {
    // Given: hits=7, misses=3
    // When: hitRate()
    // Then: 0.7 (70%)
}

// CS-6: hitRate 0 requests
@Test
public void cacheStats_hitRate_noRequests_shouldReturnZero() {
    // Given: hits=0, misses=0
    // When: hitRate()
    // Then: 0.0
}

// CS-7: toString 형식
@Test
public void cacheStats_toString_shouldFormatCorrectly() {
    // Given: hits=100, misses=50, evictions=10
    // When: toString()
    // Then: 적절한 형식의 문자열
}

// CS-8: 다중 스레드 안전성
@Test
public void cacheStats_concurrent_shouldBeThreadSafe() {
    // Given: stats
    // When: 10 threads × 1000 recordHit()
    // Then: hits == 10000
}
```

---

## 5. 테스트 파일 구조

### 5.1 파일 위치

```
src/test/java/com/fxstore/collection/
├── MapViewCoverageTest.java      # P1: NavigableMap Views (48개 테스트)
├── SetViewAdditionalTest.java    # P2: NavigableSet Views (18개 테스트)
└── (기존) SetViewCoverageTest.java

src/test/java/com/fxstore/core/
└── CacheStatsTest.java           # P3: CacheStats (8개 테스트)
```

### 5.2 테스트 수 요약

| 우선순위 | 대상 | 테스트 수 | 예상 소요 |
|----------|------|-----------|-----------|
| P1 | TailMapView | 14 | 15분 |
| P1 | HeadMapView | 14 | 15분 |
| P1 | KeySetView | 10 | 10분 |
| P1 | DescendingMapView | 10 | 10분 |
| P2 | SubSetView | 12 | 10분 |
| P2 | DescendingSetView | 6 | 5분 |
| P3 | CacheStats | 8 | 10분 |
| **총계** | | **74** | **75분** |

---

## 6. 예상 커버리지 개선

### 6.1 클래스별 목표

| 클래스 | 현재 | 목표 | 개선폭 |
|--------|------|------|--------|
| TailMapView | 63% | 85%+ | +22% |
| HeadMapView | 64% | 85%+ | +21% |
| KeySetView | 65% | 85%+ | +20% |
| DescendingMapView | 69% | 85%+ | +16% |
| SubSetView | 66% | 85%+ | +19% |
| DescendingSetView | 72% | 85%+ | +13% |
| CacheStats | 56% | 80%+ | +24% |

### 6.2 패키지별 영향

| 패키지 | 현재 | 예상 | 개선폭 |
|--------|------|------|--------|
| com.fxstore.collection | 86% | 90%+ | +4% |
| com.fxstore.core | 88% | 89%+ | +1% |
| **전체** | **91%** | **93%+** | **+2%** |

---

## 7. 검증 기준

### 7.1 성공 기준

- [ ] P1: 모든 48개 테스트 통과
- [ ] P1: TailMapView, HeadMapView, KeySetView, DescendingMapView 각 85%+
- [ ] P2: 모든 18개 테스트 통과
- [ ] P2: SubSetView, DescendingSetView 각 85%+
- [ ] P3: 모든 8개 테스트 통과
- [ ] P3: CacheStats 80%+
- [ ] 기존 테스트 회귀 없음
- [ ] 전체 커버리지 93%+ 달성

### 7.2 검증 명령

```bash
./gradlew test jacocoTestReport
# 결과: build/reports/jacoco/test/html/index.html
```

---

## 8. 구현 순서

### 8.1 Phase 1: P1 (NavigableMap Views)

```
1. MapViewCoverageTest.java 생성
2. TailMapView 테스트 구현 (14개)
3. HeadMapView 테스트 구현 (14개)
4. KeySetView 테스트 구현 (10개)
5. DescendingMapView 테스트 구현 (10개)
6. 테스트 실행 및 커버리지 확인
```

### 8.2 Phase 2: P2 (NavigableSet Views)

```
1. SetViewAdditionalTest.java 생성
2. SubSetView 테스트 구현 (12개)
3. DescendingSetView 테스트 구현 (6개)
4. 테스트 실행 및 커버리지 확인
```

### 8.3 Phase 3: P3 (CacheStats)

```
1. CacheStatsTest.java 생성
2. CacheStats 테스트 구현 (8개)
3. 테스트 실행 및 커버리지 확인
```

---

## 9. 리스크 및 대응

| 리스크 | 가능성 | 영향 | 대응 |
|--------|--------|------|------|
| View 범위 로직 복잡성 | 중 | 중 | inclusive/exclusive 모든 조합 테스트 |
| 역순 navigation 혼란 | 중 | 낮음 | DescendingView 동작 명확히 문서화 |
| CacheStats 동시성 이슈 | 낮음 | 중 | AtomicLong 사용 여부 확인 |
| 기존 테스트 회귀 | 낮음 | 높음 | 전체 테스트 실행 검증 |

---

## 10. 관련 문서

- [SETVIEW-COVERAGE-PLAN.md](SETVIEW-COVERAGE-PLAN.md) - HeadSetView/TailSetView 계획 (완료)
- [DEQUE-MIGRATION-TEST-PLAN.md](DEQUE-MIGRATION-TEST-PLAN.md) - DequeMigrator 계획 (완료)
- [REMAINING-METHODS-V08-PLAN.md](REMAINING-METHODS-V08-PLAN.md) - v0.8 메서드 구현 계획
- [02.test-strategy.md](02.test-strategy.md) - 테스트 전략
- [03.quality-criteria.md](03.quality-criteria.md) - 품질 기준

---

**문서 작성일:** 2025-12-29
**작성자:** Claude Code
**다음 단계:** 문서 품질 평가 → A+ 달성 → 구현 시작
