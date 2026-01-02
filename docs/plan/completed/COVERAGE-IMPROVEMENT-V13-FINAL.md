# FxStore 테스트 커버리지 향상 V13 최종 계획

> **작성일:** 2025-12-31
> **목표:** 테스트 커버리지 A+ 달성 (Instruction 95%+, Branch 90%+)
> **현재:** Instruction 92%, Branch 85%

---

## 1. 현황 분석

### 1.1 미커버 영역 상세

| 메서드 | 커버리지 | 미커버 Instructions | 원인 | 해결 방안 |
|--------|----------|---------------------|------|-----------|
| `OST.splitInternalNode()` | 0% | 78 | 12,801+ 요소 필요 | **테스트 추가** (55MB) |
| `FxStoreImpl.countTreeBytes()` | 22% | 147 | 내부 노드 순회 미커버 | **테스트 추가** |
| `markCollectionChanged()` | 0% | 33 | Dead Code | **제거** |
| `syncSnapshotToLegacy()` | 0% | 21 | Dead Code | **제거** |

### 1.2 메모리 요구사항

| 테스트 | 필요 요소 | 예상 메모리 | 시스템 메모리 | 가능 여부 |
|--------|-----------|-------------|---------------|-----------|
| splitInternalNode | 12,801개 | ~55 MB | 128 GB | ✅ 가능 |
| countTreeBytes 내부노드 | ~200개 | ~2 MB | 128 GB | ✅ 가능 |

---

## 2. 실행 계획

### Phase 1: OST.splitInternalNode() 테스트 (예상 효과: +0.3%)

**파일:** `src/test/java/com/snoworca/fxstore/ost/OSTInternalNodeSplitTest.java`

```java
/**
 * OST 내부 노드 분할 테스트.
 *
 * 트리거 조건: Internal 노드 자식 > 128개
 * 필요 요소: 12,801개 이상 (128 leaves × 100 elements + 1)
 * 메모리: ~55MB (128GB 시스템에서 충분)
 */
@Test
public void insert_triggersInternalNodeSplit() {
    // 13,000개 삽입으로 splitInternalNode 트리거
    OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);
    for (int i = 0; i < 13000; i++) {
        ost.insert(i, 1000L + i);
    }
    assertEquals(13000, ost.size());
}
```

### Phase 2: countTreeBytes() 내부 노드 순회 테스트 (예상 효과: +0.5%)

**파일:** `src/test/java/com/snoworca/fxstore/core/FxStoreImplInternalTest.java` (기존 파일 확장)

```java
/**
 * countTreeBytes 내부 노드 순회 테스트.
 *
 * BTree/OST Internal 노드가 있을 때 자식 순회 로직 테스트.
 */
@Test
public void stats_DEEP_withInternalNodes_shouldTraverseChildren() {
    // 13,000개 요소로 내부 노드 생성
    // stats(DEEP) 호출 시 countTreeBytes가 내부 노드 순회
}
```

### Phase 3: Dead Code 제거 (예상 효과: +0.2%)

**대상:**
- `FxStoreImpl.markCollectionChanged()` - 33 instructions
- `FxStoreImpl.syncSnapshotToLegacy()` - 21 instructions

**방안:**
1. 호출자가 없으므로 안전하게 제거
2. `@Deprecated` 어노테이션 후 제거 또는 즉시 제거

---

## 3. 예상 커버리지 개선

| 단계 | 작업 | 미커버 감소 | 예상 Instruction | 예상 Branch |
|------|------|-------------|------------------|-------------|
| 현재 | - | - | 92% | 85% |
| Phase 1 | splitInternalNode 테스트 | -78 inst | 92.3% | 85.5% |
| Phase 2 | countTreeBytes 테스트 | -100 inst | 92.7% | 86% |
| Phase 3 | Dead Code 제거 | -54 inst | **93%** | 86.5% |

**최종 예상:** Instruction 93%, Branch 86.5%

---

## 4. 목표 95%/90% 달성을 위한 추가 분석

### 4.1 남은 갭 분석

목표 달성까지 필요:
- Instruction: 93% → 95% = +2% (~500 instructions)
- Branch: 86.5% → 90% = +3.5% (~75 branches)

### 4.2 추가 개선 대상

| 패키지 | 현재 | 미커버 | 개선 가능성 |
|--------|------|--------|-------------|
| collection | 93% | 544 inst | iterator 에러 경로 |
| core | 87% | 824 inst | verify 에러 경로 |
| btree | 91% | 246 inst | 분할/병합 에러 경로 |

---

## 5. 테스트 구현 상세

### 5.1 OSTInternalNodeSplitTest.java

```java
package com.snoworca.fxstore.ost;

import com.snoworca.fxstore.storage.Allocator;
import com.snoworca.fxstore.storage.MemoryStorage;
import com.snoworca.fxstore.storage.Storage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * OST 내부 노드 분할 테스트.
 *
 * <p>목적: splitInternalNode() 0% → 100% 커버리지</p>
 * <p>조건: 128개 이상의 leaf 노드 생성 (12,801+ 요소)</p>
 * <p>메모리: ~55MB (대용량 테스트)</p>
 */
public class OSTInternalNodeSplitTest {

    private static final int PAGE_SIZE = 4096;
    private static final int ELEMENTS_FOR_INTERNAL_SPLIT = 13000;

    private Storage storage;
    private Allocator allocator;

    @Before
    public void setUp() {
        storage = new MemoryStorage();
        // 충분한 공간 할당 (100MB)
        storage.extend(PAGE_SIZE * 25000L);
        allocator = new Allocator(PAGE_SIZE, PAGE_SIZE * 3);
    }

    @Test
    public void insert_massiveData_shouldTriggerInternalNodeSplit() {
        // Given: 빈 OST
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);

        // When: 13,000개 요소 삽입 (내부 노드 분할 트리거)
        for (int i = 0; i < ELEMENTS_FOR_INTERNAL_SPLIT; i++) {
            ost.insert(i, 1000L + i);
        }

        // Then: 모든 요소 정상 저장
        assertEquals(ELEMENTS_FOR_INTERNAL_SPLIT, ost.size());

        // 데이터 무결성 검증 (샘플링)
        assertEquals(1000L, ost.get(0));
        assertEquals(1000L + 6500, ost.get(6500));
        assertEquals(1000L + 12999, ost.get(12999));
    }

    @Test
    public void statelessInsert_massiveData_shouldTriggerSplit() {
        // Given: 빈 트리
        long rootPageId = 0L;
        allocator = new Allocator(PAGE_SIZE, PAGE_SIZE * 3);
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);

        // When: Stateless API로 13,000개 삽입
        for (int i = 0; i < ELEMENTS_FOR_INTERNAL_SPLIT; i++) {
            OST.StatelessInsertResult result = ost.insertWithRoot(rootPageId, i, 2000L + i);
            rootPageId = result.newRootPageId;
            ost.setAllocTail(ost.getAllocTail());
        }

        // Then
        assertEquals(ELEMENTS_FOR_INTERNAL_SPLIT, ost.sizeWithRoot(rootPageId));
    }

    @Test
    public void remove_afterMassiveInsert_shouldMaintainIntegrity() {
        // Given: 13,000개 요소가 있는 트리
        OST ost = OST.createEmpty(storage, allocator, PAGE_SIZE);
        for (int i = 0; i < ELEMENTS_FOR_INTERNAL_SPLIT; i++) {
            ost.insert(i, 1000L + i);
        }

        // When: 절반 삭제
        for (int i = ELEMENTS_FOR_INTERNAL_SPLIT - 1; i >= 6500; i--) {
            ost.remove(i);
        }

        // Then
        assertEquals(6500, ost.size());
        for (int i = 0; i < 6500; i++) {
            assertEquals(1000L + i, ost.get(i));
        }
    }
}
```

### 5.2 countTreeBytes 내부 노드 테스트 추가

```java
// FxStoreImplInternalTest.java에 추가

@Test
public void stats_DEEP_massiveList_shouldTraverseInternalNodes() {
    // Given: 많은 요소를 가진 List (내부 노드 생성)
    try (FxStore store = FxStore.openMemory()) {
        List<Long> list = store.createList("massiveList", Long.class);

        // 13,000개 요소 삽입
        for (long i = 0; i < 13000; i++) {
            list.add(i);
        }

        // When: DEEP 모드 stats (countTreeBytes 내부 노드 순회)
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertNotNull(stats);
        assertEquals(1, stats.collectionCount());
        assertTrue("Should have significant bytes from internal nodes",
                   stats.liveBytesEstimate() > 100000);
    }
}
```

---

## 6. Dead Code 제거

### 6.1 markCollectionChanged 제거

```java
// FxStoreImpl.java에서 제거
// Line 2053-2063: markCollectionChanged 메서드 전체 삭제
```

### 6.2 syncSnapshotToLegacy 제거

```java
// FxStoreImpl.java에서 제거
// Line 2340-2351: syncSnapshotToLegacy 메서드 전체 삭제
```

---

## 7. 실행 순서

| 순서 | 작업 | 파일 | 예상 시간 |
|------|------|------|-----------|
| 1 | OSTInternalNodeSplitTest 생성 | 신규 | 5분 |
| 2 | FxStoreImplInternalTest 확장 | 수정 | 3분 |
| 3 | Dead Code 제거 | FxStoreImpl.java | 2분 |
| 4 | 전체 테스트 실행 | - | 2분 |
| 5 | 커버리지 측정 | - | 1분 |

---

## 8. 성공 기준

| 지표 | 현재 | 목표 | Phase 3 후 예상 |
|------|------|------|-----------------|
| Instruction | 92% | 95% | 93%+ |
| Branch | 85% | 90% | 86.5%+ |
| 테스트 통과 | 2,534 | 2,540+ | 2,540+ |

---

## 9. 리스크 및 완화

| 리스크 | 가능성 | 영향 | 완화 방안 |
|--------|--------|------|-----------|
| OOM 발생 | 낮음 | 높음 | -Xmx512m 설정, 128GB 충분 |
| 테스트 시간 증가 | 중간 | 낮음 | 대용량 테스트 별도 분리 |
| Dead code 제거 부작용 | 낮음 | 중간 | 호출자 없음 확인 완료 |

---

## 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-12-31 | 초기 작성 |
