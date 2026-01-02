# FxStore 전수 조사 보고서

> **조사일:** 2025-12-30
> **대상 버전:** v1.1 (P0/P1/P2 완료 후)
> **조사 범위:** 동시성, 컬렉션, BTree/Storage, API, 테스트 커버리지

---

## 목차

- [1. 요약](#1-요약)
- [2. CRITICAL 이슈 (즉시 수정)](#2-critical-이슈-즉시-수정)
- [3. HIGH 이슈 (우선 수정)](#3-high-이슈-우선-수정)
- [4. MEDIUM 이슈 (계획 수정)](#4-medium-이슈-계획-수정)
- [5. LOW 이슈 (개선 권장)](#5-low-이슈-개선-권장)
- [6. 테스트 커버리지 갭](#6-테스트-커버리지-갭)
- [7. 수정 우선순위 로드맵](#7-수정-우선순위-로드맵)

---

## 1. 요약

### 1.1 조사 결과 통계

| 심각도 | 동시성 | 컬렉션 | BTree/Storage | API | 합계 |
|--------|--------|--------|---------------|-----|------|
| **CRITICAL** | 1 | 1 | 3 | 1 | **6** |
| **HIGH** | 4 | 3 | 6 | 6 | **19** |
| **MEDIUM** | 5 | 15 | 12 | 12 | **44** |
| **LOW** | 1 | 2 | 0 | 3 | **6** |
| **합계** | 11 | 21 | 21 | 22 | **75** |

### 1.2 현재 품질 지표

- **테스트 커버리지:** 91% (Instructions), 85% (Branches)
- **테스트 수:** 2,395개
- **주요 위험 영역:** PageCache, OST, createOrOpen*, FxDequeImpl

---

## 2. CRITICAL 이슈 (즉시 수정)

### CRIT-001: PageCache 스레드 안전성 부재

| 항목 | 내용 |
|------|------|
| **파일** | `PageCache.java` 전체 |
| **문제** | LinkedHashMap을 동기화 없이 직접 사용. 여러 스레드가 readPage(), put(), writePage() 동시 호출 시 데이터 불일치/크래시 |
| **영향** | 데이터 손상, ConcurrentModificationException, 무한 루프 |
| **수정안** | ConcurrentHashMap 사용 또는 ReentrantReadWriteLock 추가 |

```java
// 현재 (위험)
private final LinkedHashMap<Long, byte[]> cache;

// 수정안
private final ConcurrentHashMap<Long, byte[]> cache;
// 또는
private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
```

---

### CRIT-002: OST pageId vs offset 혼동

| 항목 | 내용 |
|------|------|
| **파일** | `OST.java:215` |
| **문제** | `storage.read(pageId, ...)` 호출 시 pageId를 직접 전달하나, Storage는 바이트 offset을 기대. 잘못된 위치 읽기 |
| **영향** | 데이터 손상, 잘못된 노드 로드 |
| **수정안** | `long offset = pageId * pageSize;` 계산 후 전달 |

```java
// 현재 (오류)
storage.read(pageId, page, 0, pageSize);

// 수정안
long offset = pageId * pageSize;
storage.read(offset, page, 0, pageSize);
```

---

### CRIT-003: 빈 OST 리프 pageId 공유

| 항목 | 내용 |
|------|------|
| **파일** | `OST.java:366-367` |
| **문제** | 빈 OST 생성 시 `new OSTLeaf()`로 pageId=0인 리프 생성. 모든 빈 리프가 같은 pageId 공유 |
| **영향** | 데이터 덮어쓰기, 리프 손실 |
| **수정안** | allocator로 고유 pageId 할당 후 생성 |

---

### CRIT-004: FxDequeImpl headSeq/tailSeq 강제 리셋

| 항목 | 내용 |
|------|------|
| **파일** | `FxDequeImpl.java:552-553, 632-633, 896` |
| **문제** | removeFirstOccurrence(), removeLastOccurrence(), retainAll()에서 `headSeq = 0; tailSeq = 0;` 강제 리셋 |
| **영향** | Deque 상태 불일치, 데이터 손실, 음수 시퀀스 사용 불가 |
| **수정안** | 기존 시퀀스 범위 유지하며 재구성 |

```java
// 현재 (위험)
headSeq = 0;
tailSeq = 0;
for (E element : toRetain) { addLastUnlocked(element); }

// 수정안: 원래 headSeq 유지
long newHead = headSeq;
long newTail = headSeq;
for (E element : toRetain) {
    // insert at newTail, newTail++
}
headSeq = newHead;
tailSeq = newTail;
```

---

### CRIT-005: BTree 리프 분할 후 중복 쓰기

| 항목 | 내용 |
|------|------|
| **파일** | `BTree.java:378-379` |
| **문제** | 분할된 왼쪽 리프를 먼저 저장한 후, nextLeaf 포인터 설정 후 다시 저장. COW 불변식 위반 |
| **영향** | 스냅샷 격리 위반, 부분 상태 노출 |
| **수정안** | nextLeaf 설정 후 한 번만 저장 |

---

### CRIT-006: FxStoreImpl 메서드 체인 타입 오류

| 항목 | 내용 |
|------|------|
| **파일** | `FxStoreImpl.java:1604` |
| **문제** | `compactOptions.durability()` 호출이 FxOptions 반환하나 Builder 메서드 체인 예상. 컴파일 오류 또는 런타임 오류 |
| **영향** | 컴팩션 실패 |
| **수정안** | `withDurability()` 사용 |

---

## 3. HIGH 이슈 (우선 수정)

### 3.1 동시성 (4건)

| ID | 파일:라인 | 문제 | 수정안 |
|----|----------|------|--------|
| HIGH-C1 | FxStoreImpl:617-630 | createOrOpenMap TOCTOU - catalog.containsKey() 후 createMap() 사이 경쟁 | Write Lock 내에서 원자적 처리 |
| HIGH-C2 | FxStoreImpl:730-741, 848-859, 1033-1044 | createOrOpenSet/List/Deque 동일 TOCTOU | 동일 패턴 적용 |
| HIGH-C3 | PageCache:42-53 | removeEldestEntry 내 cacheBytes 동시 수정 | 동기화 추가 |
| HIGH-C4 | FxStoreImpl:576-606 | openMap computeIfAbsent 내 TOCTOU | 람다 시작 시 원자적 검증 |

### 3.2 컬렉션 (3건)

| ID | 파일:라인 | 문제 | 수정안 |
|----|----------|------|--------|
| HIGH-L1 | FxNavigableMapImpl:317-332 | containsValue() 비동시성 cursor 순회 | Wait-free read 패턴 적용 |
| HIGH-L2 | FxDequeImpl:507-575, 587-655 | removeFirst/LastOccurrence O(n²) | 부분 재구성 최적화 |
| HIGH-L3 | FxNavigableSetImpl:155-165 | retainAll()에서 읽기전용 iterator.remove() | 별도 삭제 로직 구현 |

### 3.3 BTree/Storage (6건)

| ID | 파일:라인 | 문제 | 수정안 |
|----|----------|------|--------|
| HIGH-B1 | BTree:410 | Internal 노드 자식 포인터 설정 충돌 | 원자적 교체 메서드 도입 |
| HIGH-B2 | BTree:449-468 | allocatePageId() 오버플로우 미검사 | Math.addExact() 사용 |
| HIGH-B3 | BTreeLeaf:154-172 | split() 메서드 이름 중복 (다른 의미) | 명확한 이름 분리 |
| HIGH-B4 | BTreeInternal:259-283 | fromPage() 버퍼 언더플로우 | 키 길이 검증 추가 |
| HIGH-B5 | MemoryStorage:64-66 | write() 정수 오버플로우 | long 범위 검증 |
| HIGH-B6 | OST:239-242 | Legacy/Stateless API 혼용 | Stateless API 통합 |

### 3.4 API (6건)

| ID | 파일:라인 | 문제 | 수정안 |
|----|----------|------|--------|
| HIGH-A1 | FxException:40-87 | 파라미터 순서 불일치 생성자 | 한 가지로 통일 |
| HIGH-A2 | FxOptions:258 | codecUpgradeHook 조건부 검증 누락 | 빌더에서 실시간 검증 |
| HIGH-A3 | FxStoreImpl:1814 | close() 예외 무시 | 예외 로깅/전파 |
| HIGH-A4 | FxStoreImpl:576-606 | computeIfAbsent 내 복합 로직 예외 | 사전 검증 분리 |
| HIGH-A5 | FxStoreImpl:1872-1913 | validateCodec() 복잡한 실패 경로 | 명시적 enum 결과 |
| HIGH-A6 | FxStoreImpl:1590-1627 | compactTo() 삭제 실패 무시 | 로깅 + 경고 |

---

## 4. MEDIUM 이슈 (계획 수정)

### 4.1 성능 최적화 필요 (15건)

| 파일 | 메서드 | 현재 | 목표 |
|------|--------|------|------|
| FxNavigableMapImpl | lowerEntry() | O(n) | O(log n) |
| FxNavigableMapImpl | floorEntry() | O(n) | O(log n) |
| FxNavigableMapImpl | ceilingEntry() | O(n) | O(log n) |
| FxNavigableMapImpl | higherEntry() | O(n) | O(log n) |
| FxNavigableMapImpl | SubMapView.size() | O(n) | O(1) |
| FxDequeImpl | iterator() | O(n) 복사 | Lazy iteration |
| FxList | listIterator() | Snapshot 복사 | Lazy snapshot |

### 4.2 일관성/안전성 (20건)

- keySet()/values()/entrySet() 스냅샷 미지원
- Cursor 상태 관리 동기화
- FileStorage.extend() 불완전 구현
- Allocator 오버플로우 검사 강화
- 직렬화 포맷 무결성 검증

### 4.3 API 개선 (9건)

- 반환값 null 문서화
- 예외 타입 일관성
- 리소스 관리 (try-with-resources)
- 코덱 안전성 강화

---

## 5. LOW 이슈 (개선 권장)

| ID | 파일 | 문제 | 수정안 |
|----|------|------|--------|
| LOW-1 | FxNavigableMapImpl:317 | containsValue() null 방어 | Objects.equals() 사용 |
| LOW-2 | FxDequeImpl:59 | OVERFLOW_THRESHOLD 비대칭 | 음수 headSeq 고려 |
| LOW-3 | FxReadTransactionImpl:67-71 | 검증 실패 시 에러 구분 | 다른 예외 타입 |
| LOW-4 | FxOptions:171-227 | null 검증 일관성 | 주석 추가 |
| LOW-5 | FxReadTransactionImpl:67-91 | checkActive() 후 store 참조 | volatile 또는 재검증 |

---

## 6. 테스트 커버리지 갭

### 6.1 누락된 테스트 시나리오 (우선순위: HIGH)

| 시나리오 | 현황 | 필요한 테스트 |
|---------|------|-------------|
| 크래시 복구 | 부분 | Superblock/CommitHeader 손상 복구 |
| 동시성 경합 | 기본 | createOrOpen* 중복 생성 경쟁 |
| 스냅샷 보호 | 부분 | Store 닫힘 후 트랜잭션 접근 |
| 오버플로우 | 없음 | Deque 시퀀스, Allocator 임계값 |
| 리소스 정리 | 기본 | 예외 시 락 해제 검증 |

### 6.2 Edge Case 테스트 (우선순위: MEDIUM)

- 빈 컬렉션 연산
- 최대값/최소값 경계
- null 처리 일관성
- 캐시 용량 초과

### 6.3 성능/부하 테스트 (우선순위: LOW)

- 100K+ 규모 데이터셋
- 장시간 메모리 누수 검사
- Lock 경합 분석

---

## 7. 수정 우선순위 로드맵

### Phase 1: CRITICAL (1-2일)

```
Day 1:
├── CRIT-001: PageCache 동기화 추가
├── CRIT-002: OST pageId→offset 수정
└── CRIT-003: OST 리프 pageId 할당

Day 2:
├── CRIT-004: FxDequeImpl 시퀀스 리셋 수정
├── CRIT-005: BTree 리프 분할 COW 수정
└── CRIT-006: compactTo() 메서드 체인 수정
```

### Phase 2: HIGH (3-4일)

```
Day 3:
├── HIGH-C1~C4: createOrOpen* TOCTOU 수정
└── HIGH-L1~L3: 컬렉션 동시성/성능

Day 4-5:
├── HIGH-B1~B6: BTree/Storage 안정성
└── HIGH-A1~A6: API 에러 처리
```

### Phase 3: MEDIUM (5-7일)

```
Day 6-7:
├── 성능 최적화 (lower/floor/ceiling/higher)
├── 일관성 개선
└── 테스트 커버리지 확대
```

### Phase 4: LOW + 테스트 (2-3일)

```
Day 8-10:
├── LOW 이슈 수정
├── 누락된 테스트 추가
└── 통합 테스트 강화
```

---

## 부록: 파일별 이슈 집계

| 파일 | CRITICAL | HIGH | MEDIUM | LOW |
|------|----------|------|--------|-----|
| PageCache.java | 1 | 1 | 0 | 0 |
| FxStoreImpl.java | 1 | 5 | 4 | 0 |
| FxNavigableMapImpl.java | 0 | 1 | 9 | 1 |
| FxDequeImpl.java | 1 | 2 | 3 | 1 |
| OST.java | 2 | 2 | 2 | 0 |
| BTree.java | 1 | 2 | 2 | 0 |
| BTreeLeaf.java | 0 | 1 | 2 | 0 |
| BTreeInternal.java | 0 | 1 | 1 | 0 |
| MemoryStorage.java | 0 | 1 | 0 | 0 |
| FileStorage.java | 0 | 0 | 2 | 0 |
| FxNavigableSetImpl.java | 0 | 1 | 1 | 0 |
| FxList.java | 0 | 0 | 3 | 0 |
| 기타 | 0 | 2 | 15 | 4 |

---

> **다음 단계:** 이 보고서를 기반으로 CRITICAL-BUG-FIX-PLAN.md를 작성하고 Phase 1부터 수정 진행
