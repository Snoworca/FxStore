# v1.0 테스트 커버리지 개선 Phase 2 계획

> **문서 버전:** 1.0
> **작성일:** 2025-12-30
> **대상:** FxStore v1.0
> **목표:** 브랜치 커버리지 73% → 85%+
> **상태:** 📋 계획

[← 목차로 돌아가기](00.index.md)

---

## 1. 개요

### 1.1 현재 상태

v1.0 Phase 1 완료 후 달성된 커버리지:
- 명령어 커버리지: **82%**
- 브랜치 커버리지: **73%**

### 1.2 문제점 분석

전수조사 결과 발견된 핵심 문제:

| 클래스 | 명령어 | 브랜치 | 미테스트 메서드 | 심각도 |
|--------|--------|--------|-----------------|--------|
| FxStoreImpl | 66% | **48%** | 20개 | 🔴 위험 |
| Allocator | 62% | **47%** | 6개 | 🔴 위험 |
| OSTInternal | 50% | **30%** | 13개 | 🔴 위험 |
| BTree | 65% | 51% | 19개 | 🟡 주의 |
| BTreeCursor | 63% | 50% | 3개 | 🟡 주의 |

### 1.3 목표

| 지표 | 현재 | Phase 2 목표 | 최종 목표 |
|------|------|-------------|----------|
| 명령어 커버리지 | 82% | 88%+ | 90%+ |
| 브랜치 커버리지 | 73% | 80%+ | 85%+ |
| 50% 미만 클래스 | 4개 | 0개 | 0개 |
| 0% 메서드 수 | ~60개 | 20개 미만 | 0개 |

---

## 2. 우선순위 분류

### 2.1 P0: 긴급 (브랜치 커버리지 50% 미만)

**목표:** 위험 클래스 브랜치 커버리지 70%+ 달성

| ID | 클래스 | 현재 브랜치 | 목표 | 예상 기간 |
|----|--------|------------|------|----------|
| P0-1 | FxStoreImpl | 48% | 70%+ | 2일 |
| P0-2 | Allocator | 47% | 70%+ | 1일 |
| P0-3 | OSTInternal | 30% | 60%+ | 1일 |
| P0-4 | BTree 0% 메서드 | 0% | 50%+ | 1일 |

**P0 총 예상 기간:** 5일

### 2.2 P1: 중요 (핵심 기능 미테스트)

**목표:** 핵심 기능 테스트 커버리지 확보

| ID | 기능 | 대상 클래스 | 현재 | 목표 | 예상 기간 |
|----|------|-----------|------|------|----------|
| P1-1 | compactTo() | FxStoreImpl | 0% | 80%+ | 1일 |
| P1-2 | rollback() | FxStoreImpl | 0% | 80%+ | 0.5일 |
| P1-3 | verify*() 메서드 | FxStoreImpl | 30-44% | 80%+ | 1일 |
| P1-4 | BTree 탐색 | BTree | 0% | 80%+ | 1일 |
| P1-5 | BTreeCursor | BTreeCursor | 50% | 70%+ | 0.5일 |

**P1 총 예상 기간:** 4일

### 2.3 P2: 개선 (성능 및 품질)

**목표:** 코드 품질 및 문서화 개선

| ID | 항목 | 설명 | 예상 기간 |
|----|------|------|----------|
| P2-1 | MemoryStorage | truncate, toByteArray 등 | 0.5일 |
| P2-2 | FxList remove() | 23% → 70%+ | 0.5일 |
| P2-3 | 문서화 | @since 태그, 예외 가이드 | 0.5일 |

**P2 총 예상 기간:** 1.5일

---

## 3. P0 상세 계획

### 3.1 P0-1: FxStoreImpl (48% → 70%+)

#### 3.1.1 미테스트 메서드 목록

```
카테고리 1: 압축/복사 (compactTo 관련)
- compactTo(Path)
- copyMap(String, CollectionInfo, FxStore)
- copySet(String, CollectionInfo, FxStore)
- copyDeque(String, CollectionInfo, FxStore)
- copyList(String, CollectionInfo, FxStore)
- copyCollection(CollectionInfo, FxStore)
- calculateLiveBytes(long)
- countTreeBytes(long)

카테고리 2: 트랜잭션 관리
- rollback()
- isAutoCommit()
- commitMode()
- getCommitMode()

카테고리 3: 코덱 관리
- registerCodec(Class, FxCodec)
- validateCodec(CodecRef, FxCodec, String)
- codecRefToClass(CodecRef)

카테고리 4: 내부 상태 관리
- syncSnapshotToLegacy(StoreSnapshot)
- syncBTreeAllocTail(BTree)
- markCollectionChanged(long, long)
- getCollectionState(long)
- getPageCache()
- getAllocTail()
```

#### 3.1.2 테스트 시나리오

**파일:** `FxStoreImplAdvancedTest.java`

```java
// 테스트 클래스 구조
public class FxStoreImplAdvancedTest {

    // === 카테고리 1: 트랜잭션 관리 ===
    @Test void rollback_afterModification_shouldRevert();
    @Test void rollback_emptyStore_shouldNotThrow();
    @Test void isAutoCommit_default_shouldReturnTrue();
    @Test void commitMode_shouldReturnCorrectMode();

    // === 카테고리 2: 코덱 관리 ===
    @Test void registerCodec_customCodec_shouldWork();
    @Test void registerCodec_null_shouldThrow();
    @Test void validateCodec_versionMismatch_shouldWarn();

    // === 카테고리 3: 내부 상태 ===
    @Test void getCollectionState_existingCollection_shouldReturn();
    @Test void getCollectionState_nonExistent_shouldReturnNull();
    @Test void getPageCache_shouldReturnNonNull();
    @Test void getAllocTail_shouldReturnValidOffset();
}
```

#### 3.1.3 구현 상세

```
파일: src/test/java/com/snoworca/fxstore/core/FxStoreImplAdvancedTest.java

테스트 수: 15-20개
예상 커버리지 향상: 브랜치 48% → 65%+
의존성: 없음 (독립 실행 가능)
```

### 3.2 P0-2: Allocator (47% → 70%+)

#### 3.2.1 미테스트 메서드 목록

```
BATCH 모드 관련:
- allocateRecord(int)  // 레거시
- commitPending()
- rollbackPending()
- beginPending()
- getCommittedAllocTail()
- isPendingActive()
```

#### 3.2.2 테스트 시나리오

**파일:** `AllocatorBatchModeTest.java`

```java
public class AllocatorBatchModeTest {

    // === BATCH 모드 테스트 ===
    @Test void beginPending_shouldActivatePendingMode();
    @Test void commitPending_shouldPersistChanges();
    @Test void rollbackPending_shouldRevertChanges();
    @Test void isPendingActive_afterBegin_shouldReturnTrue();
    @Test void getCommittedAllocTail_shouldReturnLastCommitted();

    // === 레거시 API 테스트 ===
    @Test void allocateRecord_shouldAllocateCorrectSize();
}
```

#### 3.2.3 구현 상세

```
파일: src/test/java/com/snoworca/fxstore/storage/AllocatorBatchModeTest.java

테스트 수: 10-12개
예상 커버리지 향상: 브랜치 47% → 70%+
의존성: FileStorage 또는 MemoryStorage
```

### 3.3 P0-3: OSTInternal (30% → 60%+)

#### 3.3.1 미테스트 메서드 목록

```
노드 조작:
- split(long)
- findChildForPosition(int)
- addChild(long, int)
- insertChild(int, long, int)
- removeChild(int)

조건 검사:
- needsSplit(int)
- canMerge(int)
- getSubtreeCount()

게터/세터:
- setChild(int, long)
- setChildSubtreeCount(int, int)
- getChild(int)
- getChildSubtreeCount(int)
- setPageId(long)
```

#### 3.3.2 테스트 시나리오

**파일:** `OSTInternalTest.java`

```java
public class OSTInternalTest {

    // === 노드 분할 테스트 ===
    @Test void split_fullNode_shouldCreateTwoNodes();
    @Test void needsSplit_fullNode_shouldReturnTrue();
    @Test void needsSplit_emptyNode_shouldReturnFalse();

    // === 자식 관리 테스트 ===
    @Test void addChild_shouldIncreaseChildCount();
    @Test void insertChild_atMiddle_shouldShiftRight();
    @Test void removeChild_shouldDecreaseChildCount();
    @Test void findChildForPosition_shouldReturnCorrectIndex();

    // === 병합 테스트 ===
    @Test void canMerge_underfullNodes_shouldReturnTrue();
    @Test void canMerge_fullNodes_shouldReturnFalse();
}
```

#### 3.3.3 구현 상세

```
파일: src/test/java/com/snoworca/fxstore/ost/OSTInternalTest.java

테스트 수: 15-18개
예상 커버리지 향상: 브랜치 30% → 60%+
의존성: OST, Storage
```

### 3.4 P0-4: BTree 0% 메서드 (0% → 50%+)

#### 3.4.1 미테스트 메서드 목록

```
탐색 메서드:
- first()
- last()
- floor(byte[])
- ceiling(byte[])
- higher(byte[])
- lower(byte[])
- get(byte[])

수정 메서드:
- put(byte[], Long)
- remove(byte[])
- delete(byte[])
- clear()

상태 메서드:
- size()
- sizeRecursive(long)
- setRootPageId(long)
- setAllocTail(long)
- getAllocTail()

커서 메서드:
- cursor(...)
- cursorWithRoot(...)
- searchRelative(...)
```

#### 3.4.2 테스트 시나리오

**파일:** `BTreeNavigationTest.java`

```java
public class BTreeNavigationTest {

    // === 탐색 메서드 테스트 ===
    @Test void first_nonEmpty_shouldReturnSmallestKey();
    @Test void first_empty_shouldReturnNull();
    @Test void last_nonEmpty_shouldReturnLargestKey();
    @Test void last_empty_shouldReturnNull();

    @Test void floor_existingKey_shouldReturnKey();
    @Test void floor_betweenKeys_shouldReturnLower();
    @Test void ceiling_existingKey_shouldReturnKey();
    @Test void ceiling_betweenKeys_shouldReturnHigher();

    @Test void higher_shouldReturnNextKey();
    @Test void lower_shouldReturnPreviousKey();

    // === 수정 메서드 테스트 ===
    @Test void put_newKey_shouldInsert();
    @Test void put_existingKey_shouldUpdate();
    @Test void remove_existingKey_shouldDelete();
    @Test void remove_nonExisting_shouldReturnNull();
    @Test void clear_shouldRemoveAllEntries();

    // === 상태 메서드 테스트 ===
    @Test void size_empty_shouldReturnZero();
    @Test void size_afterInserts_shouldReturnCount();
}
```

#### 3.4.3 구현 상세

```
파일: src/test/java/com/snoworca/fxstore/btree/BTreeNavigationTest.java

테스트 수: 20-25개
예상 커버리지 향상: 0% → 50%+
의존성: Storage, Allocator
```

---

## 4. P1 상세 계획

### 4.1 P1-1: compactTo() 테스트

#### 4.1.1 기능 설명

`compactTo(Path)`는 현재 스토어를 새 파일로 압축 복사하는 기능:
- 라이브 데이터만 복사 (가비지 제외)
- 모든 컬렉션 타입 지원 (Map, Set, Deque, List)
- 원자적 실행

#### 4.1.2 테스트 시나리오

**파일:** `FxStoreCompactTest.java`

```java
public class FxStoreCompactTest {

    @Test void compactTo_emptyStore_shouldCreateValidFile();
    @Test void compactTo_withMap_shouldPreserveData();
    @Test void compactTo_withSet_shouldPreserveData();
    @Test void compactTo_withDeque_shouldPreserveData();
    @Test void compactTo_withList_shouldPreserveData();
    @Test void compactTo_withAllTypes_shouldPreserveAll();
    @Test void compactTo_afterDeletes_shouldReduceSize();
    @Test void compactTo_existingFile_shouldOverwrite();
    @Test void compactTo_invalidPath_shouldThrow();
}
```

### 4.2 P1-2: rollback() 테스트

#### 4.2.1 기능 설명

`rollback()`은 마지막 커밋 이후 변경사항을 취소:
- AUTO 모드에서는 효과 없음 (즉시 커밋)
- MANUAL 모드에서 동작
- 메모리 상태와 영속 상태 모두 롤백

#### 4.2.2 테스트 시나리오

**파일:** `FxStoreTransactionTest.java`

```java
public class FxStoreTransactionTest {

    @Test void rollback_manualMode_shouldRevertChanges();
    @Test void rollback_autoMode_shouldHaveNoEffect();
    @Test void rollback_afterCommit_shouldHaveNoEffect();
    @Test void rollback_multipleCollections_shouldRevertAll();
}
```

### 4.3 P1-3: verify*() 메서드 테스트

#### 4.3.1 대상 메서드

```
- verifySuperblock(List<String>)
- verifyCommitHeaders(List<String>)
- verifyAllocTail(List<String>)
```

#### 4.3.2 테스트 시나리오

**파일:** `FxStoreVerificationTest.java`

```java
public class FxStoreVerificationTest {

    @Test void verifySuperblock_validStore_shouldReturnEmpty();
    @Test void verifySuperblock_corruptedMagic_shouldReportError();
    @Test void verifyCommitHeaders_validStore_shouldReturnEmpty();
    @Test void verifyAllocTail_validStore_shouldReturnEmpty();
}
```

### 4.4 P1-4: BTree 탐색 메서드 테스트

floor, ceiling, higher, lower 메서드의 상세 테스트:

**파일:** `BTreeRangeQueryTest.java`

```java
public class BTreeRangeQueryTest {

    // 경계 조건 테스트
    @Test void floor_lessThanMin_shouldReturnNull();
    @Test void ceiling_greaterThanMax_shouldReturnNull();
    @Test void higher_atMax_shouldReturnNull();
    @Test void lower_atMin_shouldReturnNull();

    // TreeMap 동등성 테스트
    @Test void rangeQueries_shouldMatchTreeMapBehavior();
}
```

### 4.5 P1-5: BTreeCursor 테스트

**파일:** `BTreeCursorTest.java`

```java
public class BTreeCursorTest {

    @Test void cursor_rangeQuery_shouldReturnCorrectEntries();
    @Test void cursor_emptyRange_shouldReturnEmpty();
    @Test void peek_shouldNotAdvance();
    @Test void findLeafContaining_shouldFindCorrectLeaf();
}
```

---

## 5. P2 상세 계획

### 5.1 P2-1: MemoryStorage 테스트

**파일:** `MemoryStorageTest.java`

```java
public class MemoryStorageTest {

    @Test void truncate_shouldReduceSize();
    @Test void toByteArray_shouldReturnAllData();
    @Test void force_shouldBeNoOp();
    @Test void defaultConstructor_shouldCreateEmptyStorage();
}
```

### 5.2 P2-2: FxList remove() 테스트

**파일:** `FxListRemoveTest.java`

```java
public class FxListRemoveTest {

    @Test void remove_atIndex_shouldShiftElements();
    @Test void remove_first_shouldWork();
    @Test void remove_last_shouldWork();
    @Test void remove_outOfBounds_shouldThrow();
    @Test void remove_fromEmptyList_shouldThrow();
}
```

### 5.3 P2-3: 문서화 개선

| 대상 | 작업 | 우선순위 |
|------|------|----------|
| RecordStore | @since 0.3 추가 | 높음 |
| PageCache | @since 0.4 추가 | 높음 |
| BTreeCursor | @since 0.3 추가 | 높음 |
| FxErrorCode | 사용 가이드 작성 | 중간 |

---

## 6. 구현 일정

### 6.1 Phase 2 전체 일정

| 일차 | 작업 | 산출물 | 버퍼 |
|------|------|--------|------|
| 1일차 | P0-1: FxStoreImpl 테스트 (1/2) | FxStoreImplAdvancedTest.java | - |
| 2일차 | P0-1: FxStoreImpl 테스트 (2/2) | 테스트 완료, 커버리지 확인 | - |
| 3일차 | P0-2: Allocator 테스트 | AllocatorBatchModeTest.java | - |
| 4일차 | P0-3: OSTInternal 테스트 | OSTInternalTest.java | - |
| 5일차 | P0-4: BTree 0% 메서드 테스트 | BTreeNavigationTest.java | - |
| **6일차** | **P0 버퍼** | **지연 작업 완료, 회귀 테스트** | **✅** |
| 7일차 | P1-1, P1-2: compact, rollback | FxStoreCompactTest.java, FxStoreTransactionTest.java | - |
| 8일차 | P1-3, P1-4: verify, BTree 탐색 | FxStoreVerificationTest.java, BTreeRangeQueryTest.java | - |
| 9일차 | P1-5: BTreeCursor | BTreeCursorTest.java | - |
| **10일차** | **P1 버퍼** | **지연 작업 완료, 통합 테스트** | **✅** |
| 11일차 | P2 전체 + 문서화 | 나머지 테스트 + 문서 | - |
| **12일차** | **최종 버퍼 + 회귀 테스트** | **커버리지 검증, 최종 보고서** | **✅** |

**총 기간: 12일 (버퍼 3일 포함, 25%)**

### 6.2 병렬 작업 가능성 분석

#### 6.2.1 병렬 실행 가능 작업 그룹

```
그룹 A (독립적, 병렬 가능):
├── P0-2: AllocatorBatchModeTest
├── P0-3: OSTInternalTest
└── P0-4: BTreeNavigationTest

그룹 B (독립적, 병렬 가능):
├── P1-1: FxStoreCompactTest
├── P1-3: FxStoreVerificationTest
└── P1-5: BTreeCursorTest

그룹 C (순차 필요):
├── P0-1: FxStoreImplAdvancedTest (먼저)
└── P1-2: FxStoreTransactionTest (rollback 의존)
```

#### 6.2.2 병렬 실행 시 최적화 일정

| 일차 | 순차 작업 | 병렬 작업 1 | 병렬 작업 2 |
|------|----------|------------|------------|
| 1-2 | P0-1: FxStoreImpl | - | - |
| 3 | P0-2: Allocator | P0-3: OSTInternal | P0-4: BTree *(병렬)* |
| 4 | P1-1: compact | P1-3: verify | P1-5: Cursor *(병렬)* |
| 5 | P1-2: rollback | P2 전체 | - |
| 6 | 회귀 테스트 | 문서화 | 최종 검증 |

**병렬 실행 시: 6일 (50% 단축)**

#### 6.2.3 의존성 제약

| 작업 | 선행 의존성 | 비고 |
|------|-----------|------|
| P0-1 | 없음 | 독립 실행 가능 |
| P0-2 | 없음 | 독립 실행 가능 |
| P0-3 | 없음 | 독립 실행 가능 |
| P0-4 | 없음 | 독립 실행 가능 |
| P1-1 | P0-1 권장 | FxStoreImpl 이해 필요 |
| P1-2 | P0-1 필수 | 트랜잭션 테스트 의존 |
| P1-3 | 없음 | 독립 실행 가능 |
| P1-4 | P0-4 권장 | BTree 탐색 이해 필요 |
| P1-5 | P0-4 권장 | BTree 커서 의존 |
| P2 | 없음 | 독립 실행 가능 |

### 6.3 예상 결과

| 지표 | 현재 | Phase 2 후 |
|------|------|-----------|
| 명령어 커버리지 | 82% | 88%+ |
| 브랜치 커버리지 | 73% | 80%+ |
| 테스트 수 | 1,620+ | 1,720+ |
| 50% 미만 클래스 | 4개 | 0개 |

---

## 7. 테스트 파일 목록

### 7.1 신규 생성 파일

| 파일명 | 패키지 | 테스트 수 | 우선순위 |
|--------|--------|----------|----------|
| FxStoreImplAdvancedTest.java | core | 15-20 | P0 |
| AllocatorBatchModeTest.java | storage | 10-12 | P0 |
| OSTInternalTest.java | ost | 15-18 | P0 |
| BTreeNavigationTest.java | btree | 20-25 | P0 |
| FxStoreCompactTest.java | core | 8-10 | P1 |
| FxStoreTransactionTest.java | core | 4-6 | P1 |
| FxStoreVerificationTest.java | core | 4-6 | P1 |
| BTreeRangeQueryTest.java | btree | 8-10 | P1 |
| BTreeCursorTest.java | btree | 5-8 | P1 |
| MemoryStorageTest.java | storage | 4-5 | P2 |
| FxListRemoveTest.java | collection | 5-6 | P2 |

### 7.2 기존 파일 보강

| 파일명 | 추가 테스트 수 |
|--------|---------------|
| FileStorageErrorTest.java | +3 (truncate 관련) |
| DequeEquivalenceTest.java | +2 (경계 조건) |

---

## 8. 위험 관리

### 8.1 위험 매트릭스

| ID | 위험 | 가능성 | 영향 | 점수 | 대응 |
|----|------|--------|------|------|------|
| R1 | compactTo 테스트 복잡도 | 높음(4) | 중간(3) | 12 | 단계별 구현, 단순 케이스 우선 |
| R2 | OSTInternal 내부 구조 이해 | 중간(3) | 높음(4) | 12 | 기존 OST 테스트 참고 |
| R3 | BATCH 모드 동작 불명확 | 중간(3) | 중간(3) | 9 | 코드 분석 후 테스트 |
| R4 | 테스트 간 의존성 | 낮음(2) | 중간(3) | 6 | 독립 테스트 설계 |

### 8.2 롤백 계획

각 P0 작업 완료 후:
1. 전체 테스트 실행 (5분 이내)
2. 커버리지 확인
3. 실패 시 해당 테스트만 제거/수정
4. 기존 테스트 영향 없음 확인

---

## 9. 성공 기준

### 9.1 P0 완료 기준

- [ ] FxStoreImpl 브랜치 커버리지 65%+ 달성
- [ ] Allocator 브랜치 커버리지 70%+ 달성
- [ ] OSTInternal 브랜치 커버리지 60%+ 달성
- [ ] BTree 0% 메서드 50%+ 달성
- [ ] 전체 테스트 통과 (5분 이내)

### 9.2 P1 완료 기준

- [ ] compactTo() 테스트 80%+ 커버리지
- [ ] rollback() 테스트 완료
- [ ] verify*() 테스트 80%+ 커버리지
- [ ] BTree 탐색 메서드 80%+ 커버리지
- [ ] BTreeCursor 70%+ 커버리지

### 9.3 P2 완료 기준

- [ ] MemoryStorage 0% 메서드 테스트 완료
- [ ] FxList.remove() 70%+ 커버리지
- [ ] 주요 클래스 @since 태그 추가

### 9.4 최종 기준

- [ ] 전체 명령어 커버리지 88%+
- [ ] 전체 브랜치 커버리지 80%+
- [ ] 50% 미만 브랜치 커버리지 클래스 0개
- [ ] 전체 테스트 1,700+개
- [ ] 테스트 실행 시간 60초 이내

---

## 10. SOLID 원칙 준수

### 10.1 SRP (단일 책임 원칙)

각 테스트 클래스는 하나의 대상 클래스만 테스트:
- `FxStoreImplAdvancedTest` → FxStoreImpl
- `AllocatorBatchModeTest` → Allocator
- `OSTInternalTest` → OSTInternal

### 10.2 OCP (개방-폐쇄 원칙)

테스트 확장 시 기존 테스트 수정 없이 새 테스트 추가:
- 새 테스트 메서드 추가 → 기존 테스트 영향 없음
- 헬퍼 메서드 재사용 가능

#### 10.2.1 OCP 확장 시나리오

**시나리오 1: 새 Storage 구현체 테스트**
```java
// 기존 테스트를 수정하지 않고 새 구현체 테스트 추가
public class CustomStorageTest extends AbstractStorageTest {
    @Override
    protected Storage createStorage() {
        return new CustomStorage(); // 새 구현체
    }
    // 부모 클래스의 모든 테스트가 자동으로 실행됨
}
```

**시나리오 2: 새 테스트 카테고리 추가**
```java
// BTreeNavigationTest에 새 카테고리 추가
public class BTreeNavigationTest {
    // === 기존 테스트 (수정 없음) ===
    @Test void first_nonEmpty_shouldReturnSmallestKey();

    // === 신규 확장: 범위 쿼리 테스트 ===
    @Test void subMap_shouldReturnCorrectRange();
}
```

### 10.3 LSP (리스코프 치환 원칙)

Storage 인터페이스 테스트:
- FileStorage와 MemoryStorage 동일 테스트 적용 가능
- 인터페이스 기반 테스트 설계

### 10.4 ISP (인터페이스 분리 원칙)

테스트 인터페이스 분리:
- 읽기 테스트 / 쓰기 테스트 분리
- 탐색 테스트 / 수정 테스트 분리

### 10.5 DIP (의존성 역전 원칙)

테스트에서 인터페이스 의존:
- Storage 인터페이스 사용 (구현체 아님)
- FxStore 인터페이스 사용 (FxStoreImpl 아님)

#### 10.5.1 DIP 적용 예시

**예시 1: Storage 인터페이스 의존 테스트**
```java
public class BTreeNavigationTest {
    private Storage storage;  // 인터페이스 타입 선언
    private BTree btree;

    @Before
    public void setUp() {
        // DIP: 구체 클래스가 아닌 인터페이스에 의존
        storage = createStorage();  // 팩토리 메서드 패턴
        btree = new BTree(storage, allocator);
    }

    // 테스트 환경에 따라 구현체 교체 가능
    protected Storage createStorage() {
        return MemoryStorage.create();  // 테스트용
        // return FileStorage.create(path);  // 통합 테스트용
    }
}
```

**예시 2: FxStore 인터페이스 의존 테스트**
```java
public class FxStoreCompactTest {
    private FxStore store;  // 인터페이스 타입 선언

    @Before
    public void setUp() {
        // DIP: FxStore 인터페이스에 의존
        store = FxStoreImpl.openMemory(FxOptions.defaults());
    }

    @Test
    public void compactTo_withMap_shouldPreserveData() {
        // store는 인터페이스 타입이므로
        // 향후 다른 구현체로 교체해도 테스트 코드 수정 불필요
        Map<String, String> map = store.getMap("test", String.class, String.class);
        // ...
    }
}
```

---

## 11. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-12-30 | 초안 작성 |
| 1.1 | 2025-12-30 | 경계 조건, 위험 관리, 영향도 분석, 용어집 추가 |
| 1.2 | 2025-12-30 | OCP 확장 시나리오, DIP 적용 예시, 버퍼 시간, 병렬 작업 분석 추가 |

---

## 12. 경계 조건 및 에러 케이스 테스트

### 12.1 경계 조건 테스트 시나리오

| ID | 대상 클래스 | 메서드 | 시나리오 | 예상 결과 |
|----|-----------|--------|----------|----------|
| BC-01 | BTree | first() | 빈 트리 | null 반환 |
| BC-02 | BTree | last() | 빈 트리 | null 반환 |
| BC-03 | BTree | floor(key) | key가 최소값보다 작음 | null 반환 |
| BC-04 | BTree | ceiling(key) | key가 최대값보다 큼 | null 반환 |
| BC-05 | BTree | higher(key) | key가 최대값 | null 반환 |
| BC-06 | BTree | lower(key) | key가 최소값 | null 반환 |
| BC-07 | BTree | size() | 빈 트리 | 0 반환 |
| BC-08 | BTree | clear() | 빈 트리 | 예외 없음 |
| BC-09 | FxStoreImpl | rollback() | AUTO 모드 | 효과 없음 |
| BC-10 | FxStoreImpl | rollback() | 커밋 후 호출 | 효과 없음 |
| BC-11 | FxStoreImpl | compactTo() | 빈 스토어 | 유효한 빈 파일 생성 |
| BC-12 | Allocator | beginPending() | 이미 활성화됨 | 예외 또는 무시 |
| BC-13 | Allocator | commitPending() | 활성화 안됨 | 예외 또는 무시 |
| BC-14 | OSTInternal | split() | 최소 크기 노드 | 정상 분할 |
| BC-15 | OSTInternal | canMerge() | 가득 찬 노드 | false 반환 |

### 12.2 에러 케이스 테스트 시나리오

| ID | 대상 클래스 | 메서드 | 시나리오 | 예상 예외 |
|----|-----------|--------|----------|----------|
| EC-01 | FxStoreImpl | compactTo(null) | null 경로 | NullPointerException |
| EC-02 | FxStoreImpl | compactTo(invalid) | 잘못된 경로 | FxException(IO) |
| EC-03 | FxStoreImpl | registerCodec(null, codec) | null 클래스 | NullPointerException |
| EC-04 | FxStoreImpl | registerCodec(cls, null) | null 코덱 | NullPointerException |
| EC-05 | BTree | get(null) | null 키 | NullPointerException |
| EC-06 | BTree | put(null, value) | null 키 | NullPointerException |
| EC-07 | BTree | remove(null) | null 키 | NullPointerException |
| EC-08 | Allocator | allocatePage(-1) | 음수 tail | IllegalArgumentException |
| EC-09 | OSTInternal | getChild(-1) | 음수 인덱스 | IndexOutOfBoundsException |
| EC-10 | OSTInternal | setChild(-1, pageId) | 음수 인덱스 | IndexOutOfBoundsException |

### 12.3 null/empty 입력 테스트

| ID | 대상 | 입력 | 예상 동작 |
|----|------|------|----------|
| NE-01 | BTree.cursor() | null fromKey | 처음부터 시작 |
| NE-02 | BTree.cursor() | null toKey | 끝까지 순회 |
| NE-03 | BTree.cursorWithRoot() | rootPageId=0 | 빈 커서 반환 |
| NE-04 | FxStoreImpl.getCollectionState() | null 이름 | null 또는 예외 |
| NE-05 | FxStoreImpl.getCollectionState() | 빈 문자열 | null 또는 예외 |

---

## 13. 상세 위험 관리

### 13.1 위험 식별 및 평가 매트릭스

**가능성 척도:**
- 1: 매우 낮음 (10% 미만)
- 2: 낮음 (10-30%)
- 3: 중간 (30-50%)
- 4: 높음 (50-70%)
- 5: 매우 높음 (70% 이상)

**영향 척도:**
- 1: 미미 (일정 지연 1일 미만)
- 2: 경미 (일정 지연 1-2일)
- 3: 보통 (일정 지연 3-5일)
- 4: 심각 (일정 지연 1주 이상)
- 5: 치명적 (프로젝트 실패)

| ID | 위험 | 가능성 | 영향 | 점수 | 등급 |
|----|------|--------|------|------|------|
| RK-001 | compactTo 테스트 복잡도 | 4 | 3 | 12 | 🟡 높음 |
| RK-002 | OSTInternal 내부 구조 이해 부족 | 3 | 4 | 12 | 🟡 높음 |
| RK-003 | BATCH 모드 동작 불명확 | 3 | 3 | 9 | 🟡 중간 |
| RK-004 | 테스트 간 의존성 충돌 | 2 | 3 | 6 | 🟢 낮음 |
| RK-005 | 테스트 실행 시간 초과 | 2 | 4 | 8 | 🟡 중간 |
| RK-006 | 기존 테스트 회귀 | 2 | 5 | 10 | 🟡 높음 |

### 13.2 상세 대응 절차

#### RK-001: compactTo 테스트 복잡도

**단계별 대응:**
1. **감지 (4시간)**: 테스트 작성 4시간 초과 시 위험 감지
2. **초기 대응**: 단순 케이스(빈 스토어, 단일 컬렉션)로 범위 축소
3. **에스컬레이션**: 8시간 초과 시 P1으로 연기
4. **최종 조치**: 기본 테스트만 유지, 복잡 케이스 별도 이슈 생성

**담당자**: 개발자
**모니터링**: 일일 진행 체크

#### RK-002: OSTInternal 내부 구조 이해 부족

**단계별 대응:**
1. **사전 준비 (2시간)**: 기존 OST 테스트 코드 분석
2. **문서 참조**: docs/plan/01.implementation-phases.md Phase 6 참조
3. **점진적 접근**: 게터/세터 테스트 → 분할/병합 테스트 순서
4. **에스컬레이션**: 이해 불가 시 코드 주석 추가 후 기본 테스트만 진행

#### RK-003: BATCH 모드 동작 불명확

**단계별 대응:**
1. **코드 분석 (2시간)**: Allocator.java BATCH 관련 코드 분석
2. **기존 테스트 참조**: AllocatorStatelessTest.java 확인
3. **단순 시나리오**: begin → allocate → commit/rollback 기본 흐름만 테스트
4. **문서화**: 발견된 동작 주석으로 기록

#### RK-006: 기존 테스트 회귀

**단계별 대응:**
1. **사전 점검**: 신규 테스트 추가 전 전체 테스트 실행
2. **격리 실행**: 신규 테스트만 먼저 실행하여 독립성 확인
3. **문제 발생 시**: 신규 테스트 즉시 제거, 원인 분석
4. **복구**: 문제 해결 후 재추가

### 13.3 모니터링 계획

| 지표 | 측정 주기 | 정상 범위 | 경고 임계값 | 대응 |
|------|----------|----------|------------|------|
| 테스트 작성 속도 | 일일 | 4-6개/일 | 3개 미만 | 범위 조정 |
| 커버리지 증가율 | 일일 | 3-5%/일 | 2% 미만 | 전략 재검토 |
| 테스트 실행 시간 | 매 실행 | 50초 미만 | 60초 초과 | 테스트 최적화 |
| 실패 테스트 수 | 매 실행 | 0개 | 1개 이상 | 즉시 수정 |

### 13.4 에스컬레이션 경로

```
개발자 → 기술 리드 → 프로젝트 관리자

Level 1 (개발자):
- 일정 지연 1일 미만
- 단일 테스트 실패

Level 2 (기술 리드):
- 일정 지연 2일 이상
- 다수 테스트 실패
- 아키텍처 변경 필요

Level 3 (프로젝트 관리자):
- 일정 지연 1주 이상
- 목표 달성 불가능
- 범위 재조정 필요
```

---

## 14. 상세 영향도 분석

### 14.1 기존 테스트 충돌 분석

| 신규 테스트 | 기존 테스트 | 공유 리소스 | 충돌 가능성 | 대응 |
|------------|-----------|-----------|------------|------|
| BTreeNavigationTest | BTreeTest | BTree 인스턴스 | 낮음 | 독립 인스턴스 사용 |
| FxStoreImplAdvancedTest | FxStoreImplTest | 임시 파일 | 중간 | 고유 파일명 사용 |
| AllocatorBatchModeTest | AllocatorStatelessTest | Storage | 낮음 | 별도 Storage 생성 |
| OSTInternalTest | OSTTest | OST 인스턴스 | 낮음 | 독립 인스턴스 사용 |
| FxStoreCompactTest | - | 임시 파일 2개 | 없음 | - |

### 14.2 성능 영향 분석

| 항목 | 현재 | 예상 변화 | 허용 범위 | 대응 |
|------|------|----------|----------|------|
| 전체 테스트 시간 | 41초 | +15초 (예상) | 60초 이내 | 병렬 실행 고려 |
| 메모리 사용량 | 512MB | +100MB (예상) | 1GB 이내 | 테스트 후 cleanup |
| 임시 파일 생성 | 50개 | +20개 (예상) | 100개 이내 | @After 정리 확인 |
| 디스크 I/O | 보통 | +30% (예상) | 허용 | SSD 권장 |

### 14.3 의존성 그래프

```
FxStoreImplAdvancedTest
├── FxStoreImpl (테스트 대상)
├── Storage (의존)
│   ├── FileStorage
│   └── MemoryStorage
├── Allocator (의존)
└── BTree (의존)

AllocatorBatchModeTest
├── Allocator (테스트 대상)
└── Storage (의존)
    └── MemoryStorage (권장)

OSTInternalTest
├── OSTInternal (테스트 대상)
├── OST (의존)
└── Storage (의존)

BTreeNavigationTest
├── BTree (테스트 대상)
├── Storage (의존)
└── Allocator (의존)
```

### 14.4 코드 변경 영향 범위

| 영향 유형 | 대상 | 변경 내용 | 위험도 |
|----------|------|----------|--------|
| 신규 파일 | 11개 테스트 파일 | 신규 생성 | 없음 |
| 기존 파일 수정 | 0개 | 없음 | 없음 |
| 프로덕션 코드 | 0개 | 변경 없음 | 없음 |
| 빌드 설정 | 0개 | 변경 없음 | 없음 |

---

## 15. 용어집

| 용어 | 정의 |
|------|------|
| **브랜치 커버리지** | 조건문(if, switch 등)의 모든 분기가 최소 1회 실행된 비율 |
| **명령어 커버리지** | 바이트코드 명령어 중 실행된 명령어의 비율 |
| **BATCH 모드** | 여러 할당 연산을 하나의 트랜잭션으로 묶어 원자적으로 커밋/롤백하는 모드 |
| **OST** | Order-Statistic Tree, 순서 통계 트리. 인덱스 기반 접근(O(log n))을 지원하는 균형 트리 |
| **COW** | Copy-on-Write, 쓰기 시 복사. 수정 시 원본을 복사하여 새 버전 생성 |
| **StampedLock** | Java 8 도입된 락. Optimistic Read 지원으로 높은 동시성 제공 |
| **JaCoCo** | Java Code Coverage, Java 코드 커버리지 측정 도구 |
| **P0/P1/P2** | 우선순위 등급. P0=긴급, P1=중요, P2=개선 |

---

## 16. 약어 정의

| 약어 | 전체 명칭 |
|------|----------|
| BC | Boundary Condition (경계 조건) |
| EC | Error Case (에러 케이스) |
| NE | Null/Empty (null 또는 빈 값) |
| RK | Risk (위험) |
| SRP | Single Responsibility Principle (단일 책임 원칙) |
| OCP | Open/Closed Principle (개방-폐쇄 원칙) |
| LSP | Liskov Substitution Principle (리스코프 치환 원칙) |
| ISP | Interface Segregation Principle (인터페이스 분리 원칙) |
| DIP | Dependency Inversion Principle (의존성 역전 원칙) |

---

*문서 작성일: 2025-12-30*
*최종 수정: 2025-12-30 (v1.2)*
*상태: 📋 계획*
