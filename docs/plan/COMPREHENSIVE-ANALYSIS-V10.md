# FxStore v1.0 종합 분석 보고서

> **문서 버전:** 1.0
> **작성일:** 2025-12-30
> **분석 대상:** FxStore 전체 코드베이스
> **목적:** 부족한 기능, 개선 포인트, 테스트 커버리지 전수 조사

[← 목차로 돌아가기](00.index.md)

---

## 1. 요약 (Executive Summary)

### 1.1 테스트 커버리지 현황

| 항목 | 값 | 비고 |
|------|-----|------|
| **명령어 커버리지** | **86%** | 3,288 / 24,347 missed |
| **브랜치 커버리지** | **79%** | 418 / 2,059 missed |
| **클래스 커버리지** | **100%** | 92개 클래스 모두 커버 |
| **메서드 커버리지** | **93%** | 90 / 1,215 missed |
| **라인 커버리지** | **88%** | 609 / 5,172 missed |

### 1.2 핵심 발견 사항

| 분류 | 항목 수 | 심각도 |
|------|---------|--------|
| **0% 커버리지 메서드** | 10개 | 🔴 High |
| **50% 미만 커버리지 메서드** | 8개 | 🟠 Medium |
| **미구현 기능 (의도적)** | 3개 | 🟢 Low (설계 결정) |
| **개선 권장 사항** | 5개 | 🟡 Enhancement |

---

## 2. 패키지별 테스트 커버리지 상세

### 2.1 커버리지 현황 (패키지별)

| 패키지 | 명령어 | 브랜치 | 평가 |
|--------|--------|--------|------|
| `util` | 98% | 90% | ✅ 우수 |
| `migration` | 98% | 93% | ✅ 우수 |
| `api` | 95% | 77% | ✅ 양호 |
| `collection` | 91% | 83% | ✅ 양호 |
| `codec` | 90% | 75% | ✅ 양호 |
| `catalog` | 90% | 83% | ✅ 양호 |
| `storage` | 83% | 87% | ✅ 양호 |
| `ost` | 82% | 83% | ✅ 양호 |
| `btree` | 81% | 80% | ✅ 양호 |
| **`core`** | **80%** | **68%** | ⚠️ **개선 필요** |

### 2.2 개선이 필요한 패키지: `com.snoworca.fxstore.core`

**FxStoreImpl.java** - 71% 명령어 커버리지, 57% 브랜치 커버리지

---

## 3. 0% 커버리지 메서드 목록 (P0: 즉시 개선 필요)

### 3.1 FxStoreImpl 클래스

| # | 메서드 | 라인 | 기능 | 우선순위 |
|---|--------|------|------|----------|
| 1 | `countTreeBytes(long)` | 1275 | B-Tree 바이트 계산 (Stats FULL 모드) | P1 |
| 2 | `calculateLiveBytes(long)` | 1235 | Live 바이트 계산 (Stats FULL 모드) | P1 |
| 3 | `lambda$openList$2()` | 802 | openList 시 검증 람다 | P2 |
| 4 | `copySet()` | 1721 | compactTo() 시 Set 복사 | P1 |
| 5 | `copyDeque()` | 1755 | compactTo() 시 Deque 복사 | P1 |
| 6 | `copyList()` | 1739 | compactTo() 시 List 복사 | P1 |
| 7 | `markCollectionChanged()` | 2030 | 컬렉션 변경 표시 | P2 |
| 8 | `syncSnapshotToLegacy()` | 2318 | 레거시 동기화 | P3 (제거 예정) |
| 9 | `syncBTreeAllocTail()` | 2233 | B-Tree allocTail 동기화 | P3 (제거 예정) |
| 10 | `getCollectionState(long)` | 1908 | ID로 컬렉션 상태 조회 | P2 |

### 3.2 원인 분석

| 메서드 그룹 | 원인 | 해결 방안 |
|-------------|------|-----------|
| Stats 관련 (1-2) | `stats(StatsMode.FULL)` 테스트 부족 | FULL 모드 테스트 추가 |
| Copy 관련 (4-6) | compactTo() 시 Set/List/Deque 복사 미테스트 | 다양한 컬렉션 compactTo 테스트 추가 |
| Lambda (3) | openList 특정 경로 미테스트 | 엣지 케이스 테스트 추가 |
| Legacy (8-9) | 레거시 코드 (제거 예정) | LEGACY-REMOVAL-PLAN.md 참조 |

---

## 4. 50% 미만 커버리지 메서드 목록 (P1: 개선 권장)

| # | 메서드 | 커버리지 | 브랜치 | 기능 |
|---|--------|----------|--------|------|
| 1 | `validateCodec()` | 13% | 25% | 코덱 검증 |
| 2 | `copyCollection()` | 16% | 0% | 컬렉션 복사 (compactTo) |
| 3 | `verifyAllocTail()` | 31% | 50% | allocTail 검증 |
| 4 | `verifySuperblock()` | 33% | 50% | Superblock 검증 |
| 5 | `validateCollectionName()` | 43% | 50% | 컬렉션 이름 검증 |
| 6 | `verifyCommitHeaders()` | 44% | 54% | CommitHeader 검증 |
| 7 | `codecRefToClass()` | 46% | 25% | CodecRef → Class 변환 |
| 8 | `copyMap()` | 47% | 0% | Map 복사 (compactTo) |

### 4.1 개선 테스트 시나리오

```
1. validateCodec 테스트
   - 유효한 코덱
   - 버전 불일치
   - ID 불일치
   - null 코덱

2. verify* 테스트 (손상 시나리오)
   - Superblock 손상
   - CommitHeader 손상
   - allocTail 범위 초과
   - CRC 불일치

3. compactTo 테스트
   - Map만 있는 스토어
   - Set만 있는 스토어
   - List만 있는 스토어
   - Deque만 있는 스토어
   - 모든 컬렉션 타입 혼합
```

---

## 5. B-Tree/OST 커버리지 상세

### 5.1 BTree 패키지 (81% 명령어, 80% 브랜치)

| 클래스 | 명령어 | 브랜치 | 비고 |
|--------|--------|--------|------|
| BTree | 86% | 90% | ✅ 양호 |
| BTreeInternal | **71%** | **72%** | ⚠️ 개선 필요 |
| BTreeLeaf | **75%** | **62%** | ⚠️ 개선 필요 |
| BTreeCursor | 88% | 75% | ✅ 양호 |
| StatelessInsertResult | **60%** | n/a | ⚠️ 개선 필요 |
| StatelessDeleteResult | **60%** | n/a | ⚠️ 개선 필요 |

### 5.2 개선 포인트

```
BTreeInternal (71%):
- 노드 분할 시 경계 조건
- 언더플로우 시 병합/재분배
- 다중 레벨 트리 연산

BTreeLeaf (75%):
- 분할 시 중간 키 선택
- 빈 리프 처리
- 범위 검색 경계 조건
```

---

## 6. 미구현 기능 분석 (의도적 설계 결정)

### 6.1 Iterator 쓰기 연산 (UnsupportedOperationException)

| 메서드 | 클래스 | 이유 |
|--------|--------|------|
| `Iterator.remove()` | FxList | 스냅샷 일관성 유지 |
| `ListIterator.remove()` | FxList | 스냅샷 일관성 유지 |
| `ListIterator.set()` | FxList | 스냅샷 일관성 유지 |

**설계 결정 근거:**
- 스냅샷 기반 Iterator는 생성 시점의 데이터를 반영
- Iterator를 통한 수정은 ConcurrentModificationException 대신 스냅샷 무결성 파괴
- 해결책: `FxList.remove(int)`, `FxList.set(int, E)` 직접 사용

### 6.2 NumberMode.STRICT (미지원)

- v0.3 단순화 결정으로 STRICT 모드 미구현
- CANONICAL 모드만 지원 (현재 사용에 충분)

---

## 7. 개선 권장 사항

### 7.1 테스트 커버리지 개선 (P0)

| 우선순위 | 대상 | 목표 | 예상 작업량 |
|----------|------|------|-------------|
| **P0-1** | Stats FULL 모드 테스트 | countTreeBytes, calculateLiveBytes 100% | 0.5일 |
| **P0-2** | compactTo 컬렉션별 테스트 | copySet/List/Deque 100% | 1일 |
| **P0-3** | verify 손상 시나리오 | verify* 메서드 80%+ | 1일 |
| **P0-4** | 코덱 검증 테스트 | validateCodec 80%+ | 0.5일 |

### 7.2 코드 품질 개선 (P1)

| 우선순위 | 대상 | 내용 |
|----------|------|------|
| **P1-1** | 레거시 코드 제거 | syncSnapshotToLegacy, syncBTreeAllocTail 제거 |
| **P1-2** | BTreeInternal/Leaf | 분할/병합 테스트 강화 |
| **P1-3** | 에러 메시지 개선 | validateCollectionName 상세 메시지 |

### 7.3 문서화 개선 (P2)

| 우선순위 | 대상 | 내용 |
|----------|------|------|
| **P2-1** | Iterator 제한 사항 | JavaDoc에 UOE 사유 명시 |
| **P2-2** | Stats 모드 차이 | FAST vs FULL 차이 문서화 |

---

## 8. 테스트 커버리지 개선 계획

### 8.1 Phase 1: 긴급 개선 (0% → 80%+)

```
목표: 0% 커버리지 메서드 해소
대상: 10개 메서드
기간: 3일
```

**테스트 파일 추가:**
1. `FxStoreStatsFullModeTest.java` - Stats FULL 모드 테스트
2. `FxStoreCompactCollectionsTest.java` - compactTo 컬렉션별 테스트
3. `FxStoreVerifyEdgeCaseTest.java` - verify 손상 시나리오

### 8.2 Phase 2: 브랜치 커버리지 개선 (68% → 85%+)

```
목표: core 패키지 브랜치 커버리지 85% 달성
대상: FxStoreImpl, FxReadTransactionImpl
기간: 2일
```

### 8.3 예상 최종 커버리지

| 항목 | 현재 | 목표 | 개선폭 |
|------|------|------|--------|
| 명령어 | 86% | 90%+ | +4% |
| 브랜치 | 79% | 85%+ | +6% |
| 0% 메서드 | 10개 | 0개 | -10 |

---

## 9. 결론

### 9.1 프로젝트 상태 요약

FxStore v1.0은 **기능적으로 완성된** Key-Value 저장소 엔진입니다:
- ✅ 모든 핵심 기능 구현 완료
- ✅ Phase 0-8 모두 7/7 A+ 달성
- ✅ 버그 수정 완료 (BUG-001, BUG-002, BUG-003)
- ✅ 전체 테스트 통과

### 9.2 개선 필요 영역

| 영역 | 현재 | 목표 | 우선순위 |
|------|------|------|----------|
| 테스트 커버리지 (core) | 80% | 90%+ | **P0** |
| 레거시 코드 제거 | 존재 | 제거 | P1 |
| 문서화 | 양호 | 우수 | P2 |

### 9.3 권장 다음 단계

1. **즉시 (P0):** Stats FULL 모드, compactTo 컬렉션별 테스트 추가
2. **단기 (P1):** 레거시 코드 제거 (LEGACY-REMOVAL-PLAN.md 실행)
3. **중기 (P2):** 문서화 개선, API 사용 가이드 작성

---

## 10. 부록: 전체 메서드 커버리지 상세

### 10.1 0% 커버리지 메서드 상세

```java
// 1. countTreeBytes - Stats FULL 모드에서 사용
private long countTreeBytes(long rootPageId) {
    // B-Tree 순회하며 바이트 계산
    // 테스트: stats(StatsMode.FULL) 호출 필요
}

// 2. calculateLiveBytes - Stats FULL 모드에서 사용
private long calculateLiveBytes(long rootPageId) {
    // 살아있는 데이터 바이트 계산
    // 테스트: stats(StatsMode.FULL) 호출 필요
}

// 3. copySet - compactTo에서 Set 복사
private void copySet(String name, CollectionInfo info, FxStore target) {
    // 테스트: Set이 포함된 스토어 compactTo 필요
}

// 4. copyDeque - compactTo에서 Deque 복사
private void copyDeque(String name, CollectionInfo info, FxStore target) {
    // 테스트: Deque가 포함된 스토어 compactTo 필요
}

// 5. copyList - compactTo에서 List 복사
private void copyList(String name, CollectionInfo info, FxStore target) {
    // 테스트: List가 포함된 스토어 compactTo 필요
}
```

---

*문서 작성일: 2025-12-30*
*분석 도구: JaCoCo 0.8.11*
*상태: **완료***
