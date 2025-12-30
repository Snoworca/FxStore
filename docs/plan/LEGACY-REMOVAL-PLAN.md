# 레거시 코드 제거 계획서

> **문서 버전:** 1.0
> **대상:** FxStoreImpl, Allocator 레거시 코드
> **목표:** 이중 상태 관리 제거 및 StoreSnapshot 단일화
> **작성일:** 2025-12-29

[← 목차로 돌아가기](00.index.md)

---

## 1. 개요

### 1.1 배경

Phase 8 (v0.4) 동시성 지원 구현 시 `StoreSnapshot` 기반의 새로운 불변 스냅샷 모델이 도입되었습니다.
그러나 하위 호환성을 이유로 기존 레거시 필드들이 병행 유지되어 **이중 상태 관리**가 발생했습니다.

```
┌─────────────────────────────────────────────────────────────┐
│                    현재 상태 (이중 관리)                      │
├─────────────────────────────────────────────────────────────┤
│  쓰기 경로 (Mutable)              읽기 경로 (Immutable)       │
│  ├── catalog                     ├── StoreSnapshot          │
│  ├── collectionStates      ←sync→ │   ├── catalog           │
│  └── allocator.currentAllocTail   │   ├── states            │
│                                   │   └── allocTail          │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 문제점

| 문제 | 설명 | 영향 |
|------|------|------|
| **복잡성 증가** | 두 곳에서 동일 상태 관리 | 유지보수 비용 증가 |
| **동기화 오버헤드** | `syncLegacyToSnapshot()` 호출 필요 | 성능 저하 |
| **버그 가능성** | 동기화 누락 시 불일치 발생 | 데이터 무결성 위험 |
| **코드 가독성 저하** | 어느 소스가 정답인지 불명확 | 개발자 혼란 |

### 1.3 목표

```
┌─────────────────────────────────────────────────────────────┐
│                    목표 상태 (단일 관리)                      │
├─────────────────────────────────────────────────────────────┤
│                  StoreSnapshot (유일한 진실)                  │
│                  ├── catalog                                │
│                  ├── states                                 │
│                  ├── rootPageIds                            │
│                  ├── allocTail                              │
│                  └── nextCollectionId                       │
│                                                             │
│  쓰기: snapshot.withXxx() → 새 스냅샷 생성 → publishSnapshot()│
│  읽기: snapshot() → 불변 스냅샷 참조 (Wait-free)              │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 영향 범위 분석

### 2.1 제거 대상

#### 2.1.1 FxStoreImpl.java

| 필드/메서드 | 라인 | 유형 | 제거 이유 |
|------------|------|------|----------|
| `catalog` | 102 | 필드 | StoreSnapshot.catalog로 대체 |
| `collectionStates` | 105 | 필드 | StoreSnapshot.states로 대체 |
| `nextCollectionId` | 111 | 필드 | StoreSnapshot.nextCollectionId로 대체 |
| `syncLegacyToSnapshot()` | 2279 | 메서드 | 이중 관리 제거로 불필요 |
| `syncSnapshotToLegacy()` | 2258 | 메서드 | 이중 관리 제거로 불필요 |

**제거 코드 라인 추정**: ~150줄

#### 2.1.2 Allocator.java

| 필드/메서드 | 라인 | 유형 | 제거 이유 |
|------------|------|------|----------|
| `committedAllocTail` | 60 | 필드 | StoreSnapshot.allocTail로 대체 |
| `currentAllocTail` | 63 | 필드 | StoreSnapshot.allocTail로 대체 |
| `pendingActive` | 66 | 필드 | FxStoreImpl에서 관리 |
| `allocatePage()` | 212 | 메서드 | Stateless API 사용 |
| `allocateRecord(int)` | 244 | 메서드 | Stateless API 사용 |
| `beginPending()` | 280 | 메서드 | 불필요 |
| `commitPending()` | 293 | 메서드 | 불필요 |
| `rollbackPending()` | 308 | 메서드 | 불필요 |
| `getAllocTail()` | 323 | 메서드 | StoreSnapshot.getAllocTail() 사용 |
| `getCommittedAllocTail()` | 333 | 메서드 | 불필요 |
| `isPendingActive()` | 343 | 메서드 | 불필요 |
| 2-param 생성자 | 75 | 생성자 | 1-param 생성자만 필요 |

**제거 코드 라인 추정**: ~120줄

### 2.2 수정 대상

#### 2.2.1 레거시 API 호출 위치

| 파일 | 라인 | 현재 코드 | 변경 방향 |
|------|------|----------|----------|
| `BTree.java` | 418 | `allocator.allocatePage()` | Stateless API 호출 |
| `OST.java` | 203 | `allocator.allocatePage()` | Stateless API 호출 |
| `FxStoreImpl.java` | 1060 | `allocator.getAllocTail()` | `snapshot().getAllocTail()` |
| `FxStoreImpl.java` | 1232 | `allocator.getAllocTail()` | `snapshot().getAllocTail()` |
| `FxStoreImpl.java` | 2014 | `allocator.allocatePage()` | Stateless API 호출 |
| `FxStoreImpl.java` | 2175 | `allocator.getAllocTail()` | `snapshot().getAllocTail()` |
| `FxStoreImpl.java` | 2200 | `allocator.getAllocTail()` | `snapshot().getAllocTail()` |
| `FxStoreImpl.java` | 2227 | `allocator.getAllocTail()` | `snapshot().getAllocTail()` |
| `FxStoreImpl.java` | 2288 | `allocator.getAllocTail()` | `snapshot().getAllocTail()` |

#### 2.2.2 catalog/collectionStates 직접 접근

약 **50개** 위치에서 `catalog.` 또는 `collectionStates.` 접근 발생.
모두 `snapshot().getCatalog()` 또는 새 StoreSnapshot 생성으로 변경 필요.

### 2.3 유지 대상

| 항목 | 이유 |
|------|------|
| `openCollections` | 열린 컬렉션 인스턴스 캐시 (상태가 아닌 캐시) |
| `Allocator.allocatePage(long)` | Stateless API (권장) |
| `Allocator.allocateRecord(long, int)` | Stateless API (권장) |
| `Allocator.AllocationResult` | 불변 결과 클래스 |
| `StoreSnapshot` 전체 | 유일한 진실 소스 |

---

## 3. StoreSnapshot 확장

### 3.1 필요한 With 메서드 추가

현재 `StoreSnapshot`에 다음 메서드 추가 필요:

```java
/**
 * 새 컬렉션을 추가한 새 스냅샷 생성
 */
public StoreSnapshot withNewCollection(
    String name,
    CatalogEntry entry,
    CollectionState state
) {
    Map<String, CatalogEntry> newCatalog = new HashMap<>(this.catalog);
    newCatalog.put(name, entry);

    Map<Long, CollectionState> newStates = new HashMap<>(this.states);
    newStates.put(state.getCollectionId(), state);

    Map<Long, Long> newRoots = new HashMap<>(this.rootPageIds);
    newRoots.put(state.getCollectionId(), state.getRootPageId());

    return new StoreSnapshot(
        this.seqNo + 1,
        this.allocTail,
        newCatalog,
        newStates,
        newRoots,
        this.nextCollectionId + 1
    );
}

/**
 * 컬렉션을 삭제한 새 스냅샷 생성
 */
public StoreSnapshot withoutCollection(String name) {
    CatalogEntry entry = this.catalog.get(name);
    if (entry == null) return this;

    Map<String, CatalogEntry> newCatalog = new HashMap<>(this.catalog);
    newCatalog.remove(name);

    Map<Long, CollectionState> newStates = new HashMap<>(this.states);
    newStates.remove(entry.getCollectionId());

    Map<Long, Long> newRoots = new HashMap<>(this.rootPageIds);
    newRoots.remove(entry.getCollectionId());

    return new StoreSnapshot(
        this.seqNo + 1,
        this.allocTail,
        newCatalog,
        newStates,
        newRoots,
        this.nextCollectionId
    );
}

/**
 * CollectionState를 업데이트한 새 스냅샷 생성
 */
public StoreSnapshot withUpdatedState(long collectionId, CollectionState newState) {
    Map<Long, CollectionState> newStates = new HashMap<>(this.states);
    newStates.put(collectionId, newState);

    Map<Long, Long> newRoots = new HashMap<>(this.rootPageIds);
    newRoots.put(collectionId, newState.getRootPageId());

    return new StoreSnapshot(
        this.seqNo + 1,
        this.allocTail,
        this.catalog,
        newStates,
        newRoots,
        this.nextCollectionId
    );
}

/**
 * 컬렉션 이름을 변경한 새 스냅샷 생성
 */
public StoreSnapshot withRenamedCollection(String oldName, String newName) {
    CatalogEntry oldEntry = this.catalog.get(oldName);
    if (oldEntry == null) return this;

    Map<String, CatalogEntry> newCatalog = new HashMap<>(this.catalog);
    newCatalog.remove(oldName);
    newCatalog.put(newName, new CatalogEntry(newName, oldEntry.getCollectionId()));

    return new StoreSnapshot(
        this.seqNo + 1,
        this.allocTail,
        newCatalog,
        this.states,
        this.rootPageIds,
        this.nextCollectionId
    );
}
```

### 3.2 스냅샷 + allocTail 복합 업데이트

```java
/**
 * 컬렉션 추가와 allocTail 업데이트를 동시에 수행
 */
public StoreSnapshot withNewCollectionAndAllocTail(
    String name,
    CatalogEntry entry,
    CollectionState state,
    long newAllocTail
) {
    // ... 위와 동일 + allocTail 업데이트
}
```

---

## 4. 구현 계획

### 4.1 Phase 1: StoreSnapshot 확장 (1일)

**작업 내용:**
1. `withNewCollection()` 메서드 추가
2. `withoutCollection()` 메서드 추가
3. `withUpdatedState()` 메서드 추가
4. `withRenamedCollection()` 메서드 추가
5. 복합 업데이트 메서드 추가
6. 단위 테스트 작성

**테스트 시나리오:**
- 새 컬렉션 추가 후 스냅샷 불변성 검증
- 컬렉션 삭제 후 스냅샷 불변성 검증
- 상태 업데이트 후 스냅샷 불변성 검증
- 이름 변경 후 스냅샷 불변성 검증

### 4.2 Phase 2: Allocator 정리 (0.5일)

**작업 내용:**
1. 레거시 필드 제거 (`committedAllocTail`, `currentAllocTail`, `pendingActive`)
2. 레거시 메서드 제거
3. 2-param 생성자 제거
4. 1-param 생성자만 유지
5. Stateless API만 유지

**결과물:**
```java
public class Allocator {
    private final int pageSize;

    public Allocator(int pageSize) { ... }

    public AllocationResult allocatePage(long currentAllocTail) { ... }
    public AllocationResult allocateRecord(long currentAllocTail, int size) { ... }
    public int getPageSize() { ... }
}
```

### 4.3 Phase 3: FxStoreImpl 리팩토링 (2일)

**작업 내용:**

#### Day 1: 쓰기 경로 변환
1. `catalog` 필드 제거 → `snapshot().getCatalog()` 사용
2. `collectionStates` 필드 제거 → `snapshot().getStates()` 사용
3. `nextCollectionId` 필드 제거 → `snapshot().getNextCollectionId()` 사용
4. 모든 `catalog.put()` → `withNewCollection()` + `publishSnapshot()` 변환
5. 모든 `catalog.remove()` → `withoutCollection()` + `publishSnapshot()` 변환

#### Day 2: 동기화 메서드 제거 및 테스트
1. `syncLegacyToSnapshot()` 제거
2. `syncSnapshotToLegacy()` 제거
3. `allocator.getAllocTail()` → `snapshot().getAllocTail()` 변환
4. 회귀 테스트 실행

### 4.4 Phase 4: BTree/OST 변환 (0.5일)

**작업 내용:**
1. `BTree.java:418` - Stateless API 호출로 변경
2. `OST.java:203` - Stateless API 호출로 변경
3. 단위 테스트 확인

### 4.5 Phase 5: 전체 회귀 테스트 (1일)

**작업 내용:**
1. 전체 테스트 스위트 실행 (1,569+ 테스트)
2. 동시성 스트레스 테스트 실행
3. 성능 벤치마크 비교
4. 코드 커버리지 확인 (81%+ 유지)

---

## 5. 롤백 계획

### 5.1 실패 시 롤백 전략

각 Phase는 독립적으로 롤백 가능:

| Phase | 롤백 방법 | 롤백 명령어 |
|-------|----------|------------|
| Phase 1 | StoreSnapshot 추가 메서드만 제거 | `git revert HEAD~1` |
| Phase 2 | 레거시 필드/메서드 복원 | `git checkout main -- Allocator.java` |
| Phase 3 | catalog/collectionStates 필드 복원 | `git checkout main -- FxStoreImpl.java` |
| Phase 4 | 레거시 API 호출 복원 | `git checkout main -- BTree.java OST.java` |

### 5.2 Phase별 롤백 상세

#### Phase 1 롤백 (StoreSnapshot)

```bash
# StoreSnapshot 추가 메서드만 제거 (git revert 또는 수동)
git diff HEAD~1 -- src/main/java/com/snoworca/fxstore/core/StoreSnapshot.java
git checkout HEAD~1 -- src/main/java/com/snoworca/fxstore/core/StoreSnapshot.java

# 테스트로 검증
./gradlew test --tests "*StoreSnapshotTest*"
```

#### Phase 2 롤백 (Allocator)

```bash
# 레거시 필드/메서드 복원
git checkout main -- src/main/java/com/snoworca/fxstore/storage/Allocator.java

# 호환성 테스트
./gradlew test --tests "*AllocatorTest*"
```

#### Phase 3 롤백 (FxStoreImpl)

```bash
# 전체 FxStoreImpl 복원 (가장 복잡)
git checkout main -- src/main/java/com/snoworca/fxstore/core/FxStoreImpl.java

# 전체 회귀 테스트 필수
./gradlew test
```

#### Phase 4 롤백 (BTree/OST)

```bash
# Stateless API 호출을 레거시로 복원
git checkout main -- src/main/java/com/snoworca/fxstore/btree/BTree.java
git checkout main -- src/main/java/com/snoworca/fxstore/ost/OST.java

# 관련 테스트
./gradlew test --tests "*BTreeTest*" --tests "*OSTTest*"
```

### 5.3 롤백 판단 기준

| 상황 | 롤백 결정 | 조건 |
|------|----------|------|
| 테스트 실패 | 해당 Phase만 롤백 | 실패율 > 1% |
| 성능 저하 | 전체 롤백 검토 | 기존 대비 < 90% |
| 동시성 버그 | 즉시 전체 롤백 | 데이터 손상 위험 |
| 빌드 실패 | 해당 변경 롤백 | 컴파일 에러 발생 |

### 5.4 브랜치 전략

```
main
  └── feature/legacy-removal
        ├── phase1-snapshot-extension
        ├── phase2-allocator-cleanup
        ├── phase3-fxstore-refactor
        └── phase4-btree-ost-cleanup
```

---

## 6. 품질 평가 기준

### 6.1 7가지 평가 기준

| # | 기준 | 만점 | 설명 |
|---|------|------|------|
| 1 | **Plan-Code 정합성** | 100 | 계획과 구현의 일치도 |
| 2 | **SOLID 원칙 준수** | 100 | 객체지향 설계 원칙 |
| 3 | **테스트 커버리지** | 100 | 기존 커버리지 유지 (81%+) |
| 4 | **코드 가독성** | 100 | 단순화로 인한 가독성 향상 |
| 5 | **예외 처리 및 안정성** | 100 | 스냅샷 불변성 보장 |
| 6 | **성능 효율성** | 100 | 동기화 오버헤드 제거 |
| 7 | **기술 부채 해소** | 100 | 레거시 코드 완전 제거 |

### 6.2 성능 벤치마크 목표

#### 6.2.1 기준 성능 (변경 전)

Phase 8에서 측정된 기준 성능:

| 메트릭 | 값 | 비고 |
|--------|-----|------|
| 단일 Write | ≥ 50K ops/sec | Map.put |
| 단일 Read | ≥ 100K ops/sec | Map.get |
| 동시 Read 확장 | 선형 | 스레드 수 비례 |
| 혼합 워크로드 | ≥ 80K ops/sec | 80% 읽기, 20% 쓰기 |

#### 6.2.2 변경 후 목표 성능

| 메트릭 | 목표 | 근거 |
|--------|------|------|
| 단일 Write | ≥ 50K ops/sec (유지) | syncLegacyToSnapshot() 제거로 약간 향상 예상 |
| 단일 Read | ≥ 100K ops/sec (유지) | Wait-free 유지 |
| 스냅샷 생성 | ≤ 10µs | with* 메서드 복사 비용 |
| 메모리 사용 | ≤ 기존 + 5% | 불변 스냅샷 GC 부담 허용 |

#### 6.2.3 벤치마크 테스트 코드

```java
@Test
public void benchmark_snapshotCreation_shouldBeFast() {
    // Given
    StoreSnapshot initial = createLargeSnapshot(1000); // 1000개 컬렉션

    // When
    long start = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
        initial.withAllocTail(initial.getAllocTail() + 4096);
    }
    long elapsed = System.nanoTime() - start;

    // Then: 평균 10µs 이하
    long avgNanos = elapsed / 10000;
    assertTrue("스냅샷 생성 평균: " + avgNanos + "ns", avgNanos < 10_000);
}

@Test
public void benchmark_afterLegacyRemoval_shouldMaintainPerformance() {
    // Given
    FxStore store = FxStore.openMemory();
    NavigableMap<Long, String> map = store.createMap("bench", Long.class, String.class);

    // When: 50K 쓰기
    long start = System.nanoTime();
    for (long i = 0; i < 50_000; i++) {
        map.put(i, "value" + i);
    }
    long writeElapsed = System.nanoTime() - start;

    // Then: 1초 이내 (≥ 50K ops/sec)
    assertTrue("쓰기 성능: " + (writeElapsed / 1_000_000) + "ms",
               writeElapsed < 1_000_000_000L);

    // When: 100K 읽기
    start = System.nanoTime();
    for (long i = 0; i < 100_000; i++) {
        map.get(i % 50_000);
    }
    long readElapsed = System.nanoTime() - start;

    // Then: 1초 이내 (≥ 100K ops/sec)
    assertTrue("읽기 성능: " + (readElapsed / 1_000_000) + "ms",
               readElapsed < 1_000_000_000L);
}
```

#### 6.2.4 성능 비교 보고서 양식

```markdown
## 레거시 제거 성능 비교 보고서

### 환경
- Java: 8
- OS: ...
- CPU: ...
- 메모리: ...

### 결과

| 메트릭 | 변경 전 | 변경 후 | 변화 |
|--------|--------|--------|------|
| 단일 Write | XXK ops/sec | XXK ops/sec | +X% |
| 단일 Read | XXK ops/sec | XXK ops/sec | +X% |
| 스냅샷 생성 | N/A | Xµs | - |
| 메모리 사용 | XXX MB | XXX MB | +X% |

### 결론
✅ 모든 성능 목표 달성 / ❌ 목표 미달 (롤백 필요)
```

### 6.3 SOLID 원칙 상세 평가

#### S - Single Responsibility Principle

| 항목 | 변경 전 | 변경 후 | 평가 |
|------|--------|--------|------|
| FxStoreImpl | 상태 관리 + 스냅샷 관리 이중 책임 | 스냅샷 관리 단일 책임 | ✅ 개선 |
| Allocator | 상태 + Stateless API 이중 제공 | Stateless API 단일 제공 | ✅ 개선 |
| StoreSnapshot | 읽기 전용 스냅샷 | 읽기 + 빌더 메서드 (적절) | ✅ 유지 |

#### O - Open/Closed Principle

| 항목 | 평가 |
|------|------|
| StoreSnapshot with* 메서드 | 확장에 열려있고 수정에 닫힘 ✅ |
| Allocator Stateless API | 새 할당 전략 추가 가능 ✅ |

#### L - Liskov Substitution Principle

| 항목 | 평가 |
|------|------|
| FxStore 인터페이스 | 기존 동작 100% 유지 ✅ |
| NavigableMap/Set 계약 | 변경 없음 ✅ |

#### I - Interface Segregation Principle

| 항목 | 평가 |
|------|------|
| Allocator API | 레거시 제거로 인터페이스 단순화 ✅ |
| StoreSnapshot | with* 메서드별 단일 책임 ✅ |

#### D - Dependency Inversion Principle

| 항목 | 평가 |
|------|------|
| FxStoreImpl → StoreSnapshot | 불변 스냅샷 추상화 의존 ✅ |
| 컬렉션 → Store | Store 인터페이스 의존 유지 ✅ |

---

## 7. 위험 요소 및 완화 방안

### 7.1 식별된 위험

| 위험 | 확률 | 영향 | 완화 방안 |
|------|------|------|----------|
| 성능 저하 | 낮음 | 중간 | 벤치마크 비교 검증 |
| 회귀 버그 | 중간 | 높음 | 1,569+ 테스트 커버리지 |
| 동시성 이슈 | 낮음 | 높음 | 스트레스 테스트 강화 |
| 누락된 변환 | 중간 | 중간 | 정적 분석 + 컴파일 검증 |

### 7.2 테스트 시나리오 상세

#### 7.2.1 StoreSnapshot 확장 테스트

| # | 시나리오 | 검증 항목 | 예상 결과 |
|---|----------|----------|----------|
| T1 | `withNewCollection()` 호출 | 원본 스냅샷 불변성 | 원본 catalog 크기 변화 없음 |
| T2 | `withNewCollection()` 호출 | 새 스냅샷 상태 | 새 컬렉션 포함 |
| T3 | `withNewCollection()` 호출 | seqNo 증가 | +1 증가 |
| T4 | `withoutCollection()` 호출 | 원본 스냅샷 불변성 | 원본 catalog 유지 |
| T5 | `withoutCollection()` 존재하지 않는 이름 | 동일 스냅샷 반환 | this 반환 |
| T6 | `withUpdatedState()` 호출 | CollectionState 변경 | 새 상태 반영 |
| T7 | `withRenamedCollection()` 호출 | 이름 변경 | 새 이름으로 접근 가능 |
| T8 | `withNewCollectionAndAllocTail()` 호출 | allocTail 동시 변경 | 두 값 모두 변경 |

#### 7.2.2 Allocator Stateless 전환 테스트

| # | 시나리오 | 검증 항목 | 예상 결과 |
|---|----------|----------|----------|
| T9 | `allocatePage(tail)` 호출 | 결과 offset 정렬 | pageSize 배수 |
| T10 | `allocatePage(tail)` 호출 | newAllocTail | 이전 + pageSize |
| T11 | `allocateRecord(tail, size)` 호출 | 결과 offset 정렬 | 8바이트 정렬 |
| T12 | 레거시 메서드 제거 후 | 컴파일 에러 | `allocatePage()` 호출 시 컴파일 실패 |

#### 7.2.3 FxStoreImpl 통합 테스트

| # | 시나리오 | 검증 항목 | 예상 결과 |
|---|----------|----------|----------|
| T13 | `createMap()` 후 스냅샷 | 단일 소스 검증 | `snapshot().getCatalog()` 포함 |
| T14 | `drop()` 후 스냅샷 | 단일 소스 검증 | `snapshot().getCatalog()` 미포함 |
| T15 | `rename()` 후 스냅샷 | 단일 소스 검증 | 새 이름으로 접근 |
| T16 | 동시 읽기/쓰기 | 스냅샷 격리 | 읽기 중 스냅샷 불변 |
| T17 | 100 스레드 동시 접근 | 데이터 무결성 | 모든 연산 정확 |
| T18 | BATCH 모드 commit | 스냅샷 업데이트 | 커밋 후 새 스냅샷 |
| T19 | BATCH 모드 rollback | 스냅샷 유지 | 이전 스냅샷 유지 |

#### 7.2.4 회귀 테스트 (기존 테스트)

| 테스트 그룹 | 테스트 수 | 목표 |
|------------|----------|------|
| StoreSnapshotTest | 22 | 100% 통과 |
| ConcurrencyStressTest | 7 | 100% 통과 |
| RaceConditionTest | 7 | 100% 통과 |
| FxStoreAdvancedTest | 28 | 100% 통과 |
| 전체 테스트 스위트 | 1,569+ | 100% 통과 |

#### 7.2.5 레거시 제거 검증 테스트

```java
// 레거시 제거 검증 테스트
@Test
public void snapshot_shouldBeOnlySourceOfTruth() {
    // Given
    FxStore store = FxStore.openMemory();

    // When
    NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
    map.put(1L, "value");

    // Then: 모든 상태가 스냅샷에서만 관리됨
    StoreSnapshot snap = ((FxStoreImpl) store).snapshot();
    assertTrue(snap.getCatalog().containsKey("test"));

    // 레거시 필드 접근 불가 (컴파일 에러 발생해야 함)
    // ((FxStoreImpl) store).catalog; // 이 필드는 존재하지 않아야 함
}

@Test
public void allocator_shouldOnlyHaveStatelessAPI() {
    // Given
    Allocator allocator = new Allocator(4096);

    // When: Stateless API 호출
    AllocationResult result = allocator.allocatePage(12288L);

    // Then
    assertEquals(12288L, result.getOffset());
    assertEquals(12288L + 4096, result.getNewAllocTail());

    // 레거시 API 호출 불가 (컴파일 에러 발생해야 함)
    // allocator.allocatePage(); // 컴파일 에러
    // allocator.getAllocTail(); // 컴파일 에러
}
```

---

## 8. 완료 조건

### 8.1 정량적 기준

| 항목 | 목표 |
|------|------|
| 레거시 필드 제거 | 100% (0개 남음) |
| 레거시 메서드 제거 | 100% (0개 남음) |
| 테스트 통과율 | 100% (1,569+ 테스트) |
| 코드 커버리지 | 81%+ 유지 |
| 성능 벤치마크 | 기존 대비 ≥100% |

### 8.2 정성적 기준

| 항목 | 목표 |
|------|------|
| 단일 진실 소스 | StoreSnapshot만 상태 관리 |
| 동기화 메서드 | 모두 제거 |
| 코드 복잡도 | 감소 (중복 제거) |
| SOLID 준수 | 모든 원칙 A+ |

---

## 9. 일정

| Phase | 작업 | 기간 | 담당 |
|-------|------|------|------|
| Phase 1 | StoreSnapshot 확장 | 1일 | AI |
| Phase 2 | Allocator 정리 | 0.5일 | AI |
| Phase 3 | FxStoreImpl 리팩토링 | 2일 | AI |
| Phase 4 | BTree/OST 변환 | 0.5일 | AI |
| Phase 5 | 전체 회귀 테스트 | 1일 | AI |
| **총계** | | **5일** | |

---

## 10. 자체 평가

### 10.1 Iteration 1 평가

| # | 기준 | 점수 | 등급 | 비고 |
|---|------|------|------|------|
| 1 | Plan-Code 정합성 | 95 | A+ | 구체적 라인 번호 명시 |
| 2 | SOLID 원칙 준수 | 98 | A+ | 5원칙 모두 분석 |
| 3 | 테스트 커버리지 | 90 | A | 테스트 시나리오 추가 필요 |
| 4 | 코드 가독성 | 95 | A+ | 예시 코드 포함 |
| 5 | 예외 처리 및 안정성 | 92 | A | 롤백 계획 구체화 필요 |
| 6 | 성능 효율성 | 90 | A | 벤치마크 목표 추가 필요 |
| 7 | 기술 부채 해소 | 98 | A+ | 완전 제거 계획 |

**총점: 658/700 (94.0%)**
**미달 항목: 3, 5, 6**

### 10.2 Iteration 2 개선 사항

#### 10.2.1 테스트 시나리오 상세화 (섹션 7.2)
- ✅ T1-T8: StoreSnapshot 확장 테스트 시나리오 추가
- ✅ T9-T12: Allocator Stateless 전환 테스트 추가
- ✅ T13-T19: FxStoreImpl 통합 테스트 추가
- ✅ 회귀 테스트 그룹별 목표 명시
- ✅ 레거시 제거 검증 테스트 코드 추가

#### 10.2.2 롤백 계획 구체화 (섹션 5.2-5.3)
- ✅ Phase별 롤백 bash 명령어 추가
- ✅ 롤백 판단 기준 표 추가
- ✅ 각 Phase별 검증 테스트 명령어 추가

#### 10.2.3 벤치마크 목표 수치 (섹션 6.2)
- ✅ 기준 성능 표 추가 (변경 전)
- ✅ 목표 성능 표 추가 (변경 후)
- ✅ 벤치마크 테스트 코드 예시 추가
- ✅ 성능 비교 보고서 양식 추가

### 10.3 Iteration 2 평가

| # | 기준 | 점수 | 등급 | 비고 |
|---|------|------|------|------|
| 1 | Plan-Code 정합성 | 98 | A+ | 구체적 라인 번호 + 변경 방향 명시 |
| 2 | SOLID 원칙 준수 | 98 | A+ | 5원칙 모두 분석 |
| 3 | 테스트 커버리지 | 98 | A+ | 19개 테스트 시나리오 + 코드 예시 |
| 4 | 코드 가독성 | 98 | A+ | 예시 코드 + 보고서 양식 |
| 5 | 예외 처리 및 안정성 | 98 | A+ | Phase별 롤백 명령어 + 판단 기준 |
| 6 | 성능 효율성 | 98 | A+ | 4개 메트릭 목표 + 벤치마크 코드 |
| 7 | 기술 부채 해소 | 98 | A+ | 완전 제거 계획 |

**총점: 686/700 (98.0%)**
**결과: ✅ 모든 기준 A+ 달성**

---

## 11. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-12-29 | 초안 작성 (Iteration 1) |
| 1.1 | 2025-12-29 | Iteration 2 개선 (7/7 A+) |

---

*문서 작성일: 2025-12-29*
*최종 평가: Iteration 2*
*상태: ✅ 완료 (7/7 A+)*
