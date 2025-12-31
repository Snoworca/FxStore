---
name: 코드-문서 일치 검증 보고서
description: Phase 3 검증 결과 및 수정 사항
---

# 코드-문서 일치 검증 보고서

**검증 일시**: 2026-01-01
**검증 대상**: docs/guide/ 전체 문서

## 검증 결과: ✅ 통과

모든 가이드 문서가 실제 코드와 일치합니다.

## 검증 항목

### 1. API 시그니처 일치 ✅

| 인터페이스 | 검증 결과 | 비고 |
|-----------|----------|------|
| FxStore | ✅ 일치 | 모든 메서드 확인 |
| FxOptions | ✅ 일치 | 모든 옵션 및 기본값 확인 |
| FxCodec | ✅ 일치 | 6개 메서드 모두 문서화 |
| FxReadTransaction | ✅ 일치 | Map/Set/List/Deque 연산 확인 |
| FxException | ✅ 일치 | 에러 코드 수정 완료 |

### 2. 에러 코드 일치 ✅

**수정된 항목**:
- `CORRUPTED` → `CORRUPTION` (실제 코드와 일치하도록 수정)
- 누락된 에러 코드 추가:
  - `OUT_OF_MEMORY`
  - `LOCK_FAILED`
  - `CLOSED`
  - `ILLEGAL_STATE`
  - `TYPE_MISMATCH`
  - `VERSION_MISMATCH`
  - `UPGRADE_FAILED`

### 3. 팩토리 메서드 일치 ✅

**수정된 항목**:
- `corrupted()` → `corruption()`
- 추가된 메서드 문서화:
  - `closed()`
  - `typeMismatch()`
  - `versionMismatch()`
  - `upgradeFailed()`
  - `outOfMemory()`
  - `lockFailed()`
  - `illegalState()`

### 4. 패키지명 일치 ✅

| 문서 | 패키지명 | 상태 |
|------|---------|------|
| 예제 파일 4개 | `com.snoworca.fxstore.api.*` | ✅ 수정됨 |
| 튜토리얼 8개 | `com.snoworca.fxstore.api.*` | ✅ 정확 |
| 빠른 시작 | `com.snoworca.fxstore.api.FxStore` | ✅ 정확 |

### 5. 기본값 일치 ✅

| 옵션 | 문서 값 | 실제 코드 | 상태 |
|------|--------|----------|------|
| CommitMode | AUTO | AUTO | ✅ |
| Durability | ASYNC | ASYNC | ✅ |
| OnClosePolicy | ERROR | ERROR | ✅ |
| FileLockMode | PROCESS | PROCESS | ✅ |
| PageSize | PAGE_4K | PAGE_4K | ✅ |
| cacheBytes | 64MB | 64MB | ✅ |
| memoryLimitBytes | unlimited | Long.MAX_VALUE | ✅ |
| allowCodecUpgrade | false | false | ✅ |
| autoMigrateDeque | false | false | ✅ |

### 6. 버전 정보 ✅

- 문서 버전: 0.3.0
- Java 요구사항: 8+
- 의존성: 없음 (zero-dependency)

## 수정 이력

1. **exceptions.md**
   - 에러 코드 테이블을 실제 FxErrorCode enum과 일치하도록 재작성
   - 팩토리 메서드 목록 업데이트

2. **예제 파일 (07.examples/)**
   - 4개 파일의 import 문을 `com.snoworca.fxstore.api.*`로 수정

## 검증 완료

모든 검증 항목을 통과했습니다. 가이드 문서는 FxStore 코드베이스와 100% 일치합니다.
