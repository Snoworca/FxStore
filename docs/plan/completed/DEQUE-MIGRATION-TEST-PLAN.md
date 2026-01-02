# DequeMigrator LEGACY 마이그레이션 테스트 계획

> **버전:** 1.0
> **작성일:** 2025-12-28
> **목표:** DequeMigrator 커버리지 27% → 80%+ 달성

---

## 목차

1. [현황 분석](#1-현황-분석)
2. [문제 정의](#2-문제-정의)
3. [테스트 전략](#3-테스트-전략)
4. [테스트 케이스 설계](#4-테스트-케이스-설계)
5. [구현 상세](#5-구현-상세)
6. [검증 기준](#6-검증-기준)

---

## 1. 현황 분석

### 1.1 커버리지 현황

| 항목 | 현재 | 목표 |
|------|------|------|
| Instructions | 27% (32/117) | 80%+ |
| Branches | 43% (7/16) | 80%+ |
| Lines | 29% (10/34) | 80%+ |
| Methods | 66% (2/3) | 100% |

### 1.2 미테스트 코드 분석

#### `performMigration()` (lines 100-132) - **전체 미테스트**

```java
private static <E> void performMigration(FxStoreImpl store, String dequeName, Class<E> elementClass) {
    // 1. 현재 Deque 열기 (LEGACY 인코딩)
    Deque<E> legacyDeque = store.openDeque(dequeName, elementClass);  // line 102
    FxDequeImpl<E> legacyImpl = (FxDequeImpl<E>) legacyDeque;         // line 103

    // 2. 모든 요소 읽기
    java.util.List<E> elements = new java.util.ArrayList<>();        // line 106
    for (E element : legacyDeque) {                                   // line 107
        elements.add(element);                                        // line 108
    }

    // 3. 모든 요소 삭제
    while (!legacyDeque.isEmpty()) {                                  // line 112
        legacyDeque.pollFirst();                                      // line 113
    }

    // 4. CollectionState 업데이트: LEGACY → ORDERED
    long collectionId = legacyImpl.getCollectionId();                 // line 117
    CollectionState oldState = store.getCollectionStateById(collectionId);  // line 118
    CollectionState newState = oldState.withSeqEncoderVersion(
        CollectionState.SEQ_ENCODER_VERSION_ORDERED);                 // line 119
    store.updateCollectionState(collectionId, newState);              // line 120

    // 5. FxDequeImpl 내부 SeqEncoder 변경
    legacyImpl.setSeqEncoder(OrderedSeqEncoder.getInstance());        // line 123

    // 6. 모든 요소 재삽입 (ORDERED 인코딩)
    for (E element : elements) {                                      // line 126
        legacyDeque.addLast(element);                                 // line 127
    }

    // 7. 커밋
    store.commitIfAuto();                                             // line 131
}
```

### 1.3 테스트 불가 원인

**핵심 문제**: 신규 Deque는 모두 `SEQ_ENCODER_VERSION_ORDERED` (v0.7+)로 생성됨

```java
// FxStoreImpl.java:888
CollectionState.SEQ_ENCODER_VERSION_ORDERED  // v0.7+: O(log n) 지원
```

따라서 `needsMigration()`이 항상 `false`를 반환하여 `performMigration()`이 호출되지 않음.

---

## 2. 문제 정의

### 2.1 LEGACY Deque 생성 방법

LEGACY 인코딩 Deque를 생성하려면 다음 내부 API를 사용해야 함:

1. **CollectionState 조작**: `FxStoreImpl.updateCollectionState()`
2. **SeqEncoder 조작**: `FxDequeImpl.setSeqEncoder()`

### 2.2 테스트 시나리오

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│  1. Deque 생성   │ ──▶ │  2. LEGACY 설정  │ ──▶ │  3. 마이그레이션  │
│  (ORDERED)      │      │  (수동 조작)     │      │  (테스트 대상)   │
└─────────────────┘      └─────────────────┘      └─────────────────┘
```

---

## 3. 테스트 전략

### 3.1 테스트 유형

| 유형 | 설명 | 테스트 수 |
|------|------|----------|
| LEGACY 설정 | LEGACY Deque 생성 검증 | 2 |
| 마이그레이션 실행 | performMigration() 경로 | 4 |
| 데이터 무결성 | 마이그레이션 후 데이터 보존 | 3 |
| 경계 조건 | 빈 Deque, 단일 요소 등 | 3 |
| 상태 검증 | CollectionState, SeqEncoder | 2 |

**총 14개 테스트 케이스**

### 3.2 Helper 메서드 설계

```java
/**
 * LEGACY 인코딩 Deque 생성 헬퍼
 */
private <E> Deque<E> createLegacyDeque(FxStoreImpl store, String name, Class<E> elementClass) {
    // 1. 일반 Deque 생성 (ORDERED)
    Deque<E> deque = store.createDeque(name, elementClass);
    FxDequeImpl<E> impl = (FxDequeImpl<E>) deque;

    // 2. LEGACY로 다운그레이드
    long collectionId = impl.getCollectionId();
    CollectionState oldState = store.getCollectionStateById(collectionId);
    CollectionState legacyState = oldState.withSeqEncoderVersion(
        CollectionState.SEQ_ENCODER_VERSION_LEGACY);
    store.updateCollectionState(collectionId, legacyState);

    // 3. SeqEncoder 변경
    impl.setSeqEncoder(LegacySeqEncoder.getInstance());

    return deque;
}
```

---

## 4. 테스트 케이스 설계

### 4.1 LEGACY 설정 테스트

| ID | 테스트명 | 시나리오 | 검증 |
|----|---------|---------|------|
| L1 | `testCreateLegacyDeque_needsMigrationReturnsTrue` | LEGACY Deque 생성 | `needsMigration() == true` |
| L2 | `testCreateLegacyDeque_hasLegacyEncoder` | LEGACY Deque 확인 | SeqEncoder == LegacySeqEncoder |

### 4.2 마이그레이션 실행 테스트

| ID | 테스트명 | 시나리오 | 검증 |
|----|---------|---------|------|
| M1 | `testMigrate_legacyDeque_executesPerformMigration` | LEGACY → ORDERED | `needsMigration() == false` 이후 |
| M2 | `testMigrate_legacyDeque_updatesCollectionState` | CollectionState 변경 | `SEQ_ENCODER_VERSION_ORDERED` |
| M3 | `testMigrate_legacyDeque_changesSeqEncoder` | SeqEncoder 변경 | `OrderedSeqEncoder` |
| M4 | `testMigrate_legacyDeque_withMixedAddOperations` | addFirst/addLast 혼합 | 순서 보존 |

### 4.3 데이터 무결성 테스트

| ID | 테스트명 | 시나리오 | 검증 |
|----|---------|---------|------|
| D1 | `testMigrate_preservesElementOrder` | 다수 요소 마이그레이션 | 순서 동일 |
| D2 | `testMigrate_preservesElementValues` | 값 보존 | 모든 값 일치 |
| D3 | `testMigrate_preservesDequeSize` | 크기 보존 | size 동일 |

### 4.4 경계 조건 테스트

| ID | 테스트명 | 시나리오 | 검증 |
|----|---------|---------|------|
| E1 | `testMigrate_emptyLegacyDeque` | 빈 LEGACY Deque | 정상 완료 |
| E2 | `testMigrate_singleElementLegacyDeque` | 단일 요소 | 값 보존 |
| E3 | `testMigrate_largeDeque` | 1000+ 요소 | 모든 값 보존 |

### 4.5 상태 검증 테스트

| ID | 테스트명 | 시나리오 | 검증 |
|----|---------|---------|------|
| S1 | `testAfterMigration_reopenStorePreservesOrdered` | Store 재오픈 | ORDERED 유지 |
| S2 | `testAfterMigration_peekOperationsAreEfficient` | O(log n) 성능 | 성능 기준 충족 |

---

## 5. 구현 상세

### 5.1 테스트 클래스 구조

```java
/**
 * DequeMigrator LEGACY 마이그레이션 테스트
 *
 * performMigration() 경로 테스트를 위해 LEGACY Deque를 수동 생성
 */
public class DequeMigratorLegacyTest {

    private File tempFile;
    private FxStore store;
    private FxStoreImpl storeImpl;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-legacy-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());
        storeImpl = (FxStoreImpl) store;
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) store.close();
        if (tempFile != null && tempFile.exists()) tempFile.delete();
    }

    // Helper method
    private <E> Deque<E> createLegacyDeque(String name, Class<E> elementClass) {
        // ... (섹션 3.2 참조)
    }

    // 14개 테스트 메서드
}
```

### 5.2 핵심 테스트 코드 예시

#### M1: 마이그레이션 실행 테스트

```java
@Test
public void testMigrate_legacyDeque_executesPerformMigration() {
    // Given: LEGACY Deque with data
    Deque<String> deque = createLegacyDeque("legacyDeque", String.class);
    deque.addLast("A");
    deque.addLast("B");
    deque.addLast("C");

    // Verify: needsMigration returns true before
    assertTrue(DequeMigrator.needsMigration(store, "legacyDeque"));

    // When: Migrate
    DequeMigrator.migrate(store, "legacyDeque", String.class);

    // Then: needsMigration returns false after
    assertFalse(DequeMigrator.needsMigration(store, "legacyDeque"));

    // And: Data preserved
    try (FxReadTransaction tx = store.beginRead()) {
        assertEquals("A", tx.peekFirst(deque));
        assertEquals("C", tx.peekLast(deque));
        assertEquals(3, tx.size(deque));
    }
}
```

#### M4: 혼합 연산 테스트

```java
@Test
public void testMigrate_legacyDeque_withMixedAddOperations() {
    // Given: LEGACY Deque with mixed operations
    Deque<String> deque = createLegacyDeque("mixedDeque", String.class);
    deque.addLast("A");   // [A]
    deque.addLast("B");   // [A, B]
    deque.addFirst("Z");  // [Z, A, B]
    deque.addFirst("Y");  // [Y, Z, A, B]

    // When: Migrate
    DequeMigrator.migrate(store, "mixedDeque", String.class);

    // Then: Order preserved
    try (FxReadTransaction tx = store.beginRead()) {
        assertEquals("Y", tx.peekFirst(deque));
        assertEquals("B", tx.peekLast(deque));

        // Verify full order
        List<String> expected = Arrays.asList("Y", "Z", "A", "B");
        List<String> actual = new ArrayList<>();
        for (String s : deque) {
            actual.add(s);
        }
        assertEquals(expected, actual);
    }
}
```

#### E3: 대용량 테스트

```java
@Test
public void testMigrate_largeDeque() {
    // Given: LEGACY Deque with 1000 elements
    Deque<Integer> deque = createLegacyDeque("largeDeque", Integer.class);
    for (int i = 0; i < 1000; i++) {
        deque.addLast(i);
    }

    // When: Migrate
    DequeMigrator.migrate(store, "largeDeque", Integer.class);

    // Then: All elements preserved
    try (FxReadTransaction tx = store.beginRead()) {
        assertEquals(Integer.valueOf(0), tx.peekFirst(deque));
        assertEquals(Integer.valueOf(999), tx.peekLast(deque));
        assertEquals(1000, tx.size(deque));
    }

    // Verify order
    int expected = 0;
    for (Integer actual : deque) {
        assertEquals(Integer.valueOf(expected++), actual);
    }
}
```

---

## 6. 검증 기준

### 6.1 커버리지 목표

| 항목 | 현재 | 목표 | 증가량 |
|------|------|------|--------|
| Instructions | 27% | 80%+ | +53%+ |
| Branches | 43% | 80%+ | +37%+ |
| Lines | 29% | 80%+ | +51%+ |
| Methods | 66% | 100% | +34% |

### 6.2 테스트 통과 기준

- [ ] 14개 테스트 모두 통과
- [ ] `performMigration()` 전체 라인 커버
- [ ] Instructions 커버리지 80%+ 달성
- [ ] 기존 테스트 회귀 없음

### 6.3 성공 조건

```
✅ DequeMigrator 커버리지 80%+ 달성
✅ performMigration() 완전 테스트
✅ 14개 테스트 모두 통과
✅ 기존 테스트 영향 없음
```

---

## 부록 A: 참조 코드

### A.1 CollectionState 버전 상수

```java
// CollectionState.java
public static final byte SEQ_ENCODER_VERSION_LEGACY = 0;
public static final byte SEQ_ENCODER_VERSION_ORDERED = 1;
```

### A.2 FxStoreImpl 내부 API

```java
// FxStoreImpl.java
public CollectionState getCollectionStateById(long collectionId);
public void updateCollectionState(long collectionId, CollectionState newState);
```

### A.3 FxDequeImpl 내부 API

```java
// FxDequeImpl.java
public void setSeqEncoder(SeqEncoder encoder);
public SeqEncoder getSeqEncoder();
public long getCollectionId();
```

---

## 부록 B: 관련 문서

- `DequeMigrator.java` - 마이그레이션 유틸리티
- `CollectionState.java` - 컬렉션 상태 관리
- `FxDequeImpl.java` - Deque 구현체
- `LegacySeqEncoder.java` - v0.6 이전 인코더
- `OrderedSeqEncoder.java` - v0.7+ 인코더

---

**문서 버전:** 1.0
**작성자:** Claude Code
**검토 대기:** 품질 평가 수행 예정
