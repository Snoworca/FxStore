# FxStore 테스트 커버리지 향상 V11 개선 계획

> **작성일:** 2025-12-31
> **목표:** 테스트 커버리지 기준 A+ 달성 (Instruction 95%+, Branch 90%+)
> **기반 분석:** [EVALUATION-V11.md](EVALUATION-V11.md)

---

## 1. 현황 분석

### 1.1 현재 커버리지

| 지표 | 현재 | 목표 | 차이 |
|------|------|------|------|
| Instruction | 91% | 95% | -4% |
| Branch | 85% | 90% | -5% |

### 1.2 미커버 영역 분류

#### 구조적 테스트 불가 (12,900+ 요소 필요, OOM 발생)

| 메서드 | 미커버 | 제약 |
|--------|--------|------|
| `OST.splitInternalNode()` | 78 inst | 129개 이상 자식 노드 필요 |

**결론**: 이 영역은 프로덕션 코드 수정 없이 테스트 불가. 커버리지 목표에서 제외 검토.

#### 테스트 가능 영역 (우선순위순)

| 순위 | 메서드 | 미커버 | 현재 | 접근 방법 |
|------|--------|--------|------|----------|
| P0 | `OST.getWithRoot()` | 63 | 50% | Stateless API 테스트 |
| P0 | `OST.removeWithRoot()` | 48 | 74% | Stateless API 테스트 |
| P0 | `FxStoreImpl.countTreeBytes()` | 147 | 22% | 다층 트리 + DEEP 모드 |
| P1 | `FxStoreImpl.verifyCommitHeaders()` | 103 | 50% | 손상 파일 시나리오 |
| P1 | `FxStoreImpl.validateCodec()` | 67 | 33% | 코덱 불일치 테스트 |
| P2 | `FxStoreImpl.markCollectionChanged()` | 33 | 0% | 내부 상태 변경 테스트 |

---

## 2. 개선 전략

### 2.1 접근법 변경

**기존 접근**: 통합 테스트로 모든 경로 커버 시도
**새 접근**: 단위 테스트로 특정 메서드 직접 테스트

### 2.2 실행 계획

| 단계 | 작업 | 예상 효과 |
|------|------|----------|
| 1 | OST Stateless API 테스트 추가 | +1.5% Instruction |
| 2 | countTreeBytes 직접 테스트 | +0.5% Instruction |
| 3 | 레거시 코드 0% 제거 또는 테스트 | +0.5% Instruction |
| **합계** | | **+2.5%** → 93.5% |

### 2.3 현실적 목표 조정

OST.splitInternalNode (78 inst)는 OOM으로 테스트 불가하므로:
- 전체 Instructions: 25,128
- splitInternalNode 제외 시 실제 테스트 가능: 25,050
- 목표: 25,050 × 95% = 23,798 커버
- 현재 커버: 23,027
- 필요 추가 커버: 771 instructions

---

## 3. 테스트 구현

### 3.1 OST Stateless API 테스트

**파일**: `src/test/java/com/snoworca/fxstore/ost/OSTStatelessApiTest.java`

```java
/**
 * OST Stateless API 테스트.
 * getWithRoot, removeWithRoot 커버리지 향상.
 */
public class OSTStatelessApiTest {

    @Test
    public void getWithRoot_emptyTree_shouldReturnError() {
        // rootPageId = 0인 경우
    }

    @Test
    public void getWithRoot_negativeIndex_shouldThrow() {
        // index < 0인 경우
    }

    @Test
    public void removeWithRoot_variousIndices_shouldCoverBranches() {
        // 다양한 삭제 위치
    }
}
```

### 3.2 FxStoreImpl 내부 테스트

**파일**: `src/test/java/com/snoworca/fxstore/core/FxStoreImplInternalTest.java`

```java
/**
 * FxStoreImpl 내부 메서드 커버리지 테스트.
 */
public class FxStoreImplInternalTest {

    @Test
    public void stats_DEEP_shouldTraverseTreeBytes() {
        // 다층 트리 생성 후 DEEP 모드 stats 호출
    }

    @Test
    public void verify_corruptedHeaders_shouldReportErrors() {
        // 손상된 헤더 시나리오
    }
}
```

---

## 4. 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-12-31 | 초기 작성 |
