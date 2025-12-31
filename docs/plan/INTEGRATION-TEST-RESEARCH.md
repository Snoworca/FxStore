# FxStore 통합 테스트 연구 보고서

> **작성일:** 2025-12-31
> **목적:** 미커버 영역의 버그 발생 가능성 분석 및 통합 테스트 케이스 설계
> **배경:** 옵션 A(93%/87% 커버리지 수용) 선택에 따른 특수 상황 버그 대비

---

## 1. 미커버 영역 위험도 분석

### 1.1 위험도 평가 기준

| 등급 | 설명 | 실제 발생 확률 | 영향도 |
|------|------|--------------|--------|
| **Critical** | 데이터 손실/무결성 위반 가능 | Medium-High | 치명적 |
| **High** | 시스템 장애 가능 | Medium | 높음 |
| **Medium** | 기능 오류 가능 | Low-Medium | 중간 |
| **Low** | 경미한 이슈 | Low | 낮음 |

### 1.2 미커버 메서드별 위험도

| 메서드 | 커버리지 | 위험도 | 발생 시나리오 | 영향 |
|--------|----------|--------|--------------|------|
| `countTreeBytes(long)` | 22% | **Medium** | 매우 깊은 트리(Internal 노드) | Stats 부정확 |
| `verifyCommitHeaders(List)` | 55% | **High** | 파일 손상, 비정상 종료 | 무결성 검증 실패 |
| `validateCodec(CodecRef, FxCodec, String)` | 33% | **Medium** | 레거시 파일 마이그레이션 | 데이터 호환성 |
| `verifyAllocTail(List)` | 31% | **High** | 파일 손상, 트랜잭션 실패 | 무결성 검증 실패 |
| `verifySuperblock(List)` | 33% | **Critical** | Superblock 손상 | 파일 열기 실패 |
| `splitInternalNode` | 0% | **Critical** | 수십만 요소 OST | 데이터 구조 손상 |
| `getCollectionState(long)` | 0% | **Low** | 내부 호출 경로 | 영향 낮음 |
| `syncBTreeAllocTail(BTree)` | 0% | **Low** | 내부 호출 경로 | 영향 낮음 |

### 1.3 위험 시나리오 상세

#### 1.3.1 Critical 위험: OST splitInternalNode (0%)

```java
// OST.java:709
private long splitInternalNode(List<OSTPathFrame> path, int level,
                                 List<Long> children, List<Integer> counts)
```

**발생 조건:**
- OST(Order Statistics Tree)에 수십만 개 이상의 요소 삽입
- 리프 노드 분할이 연쇄적으로 발생하여 Internal 노드까지 도달

**위험성:**
- 0% 커버리지로 프로덕션에서 처음 실행될 수 있음
- 분할 로직 오류 시 트리 구조 전체 손상 가능
- List 인덱스 오류, NPE 등 잠재 버그 존재 가능

**현재 코드 분석:**
```java
int splitPoint = children.size() / 2;
List<Long> leftChildren = new ArrayList<>(children.subList(0, splitPoint));
// ... rightChildren 생성
long leftPageId = saveNode(leftInternal);
long rightPageId = saveNode(rightInternal);
```
- `children.size()` 홀수일 때 비대칭 분할 발생
- 빈 children 리스트 시 예외 발생 가능

#### 1.3.2 Critical 위험: verifySuperblock (33%)

**미커버 경로:**
- CRC 불일치 검증 경로
- Magic number 불일치 검증 경로

**발생 조건:**
- 디스크 오류로 Superblock 비트 플립
- 부분 쓰기 후 크래시

**위험성:**
- Superblock 손상 시 전체 파일 접근 불가
- 복구 경로가 테스트되지 않음

#### 1.3.3 High 위험: verifyCommitHeaders (55%)

**미커버 경로:**
- seqNo gap > 1 검증 (`seqA - seqB > 1`)
- 양쪽 슬롯 모두 손상 시나리오
- 타임스탬프 역전 시나리오

**발생 조건:**
- 연속 크래시로 여러 커밋 손실
- 파일 손상으로 CRC 불일치

#### 1.3.4 High 위험: verifyAllocTail (31%)

**미커버 경로:**
- allocTail > fileSize 검증
- allocTail < minSize 검증

**발생 조건:**
- 파일 트렁케이션 (디스크 꽉 참)
- 메타데이터 손상

---

## 2. 통합 테스트 케이스 설계

### 2.1 카테고리별 테스트 시나리오

#### Category A: 대용량 데이터 무결성 (Priority: Critical)

| ID | 테스트 케이스 | 목적 | 검증 항목 |
|----|-------------|------|----------|
| A-1 | **100K 요소 List CRUD** | OST 깊은 트리 검증 | 삽입/조회/삭제 무결성 |
| A-2 | **100K 요소 Map CRUD** | BTree 깊은 트리 검증 | 키 정렬/범위 쿼리 정확성 |
| A-3 | **랜덤 순서 대량 삽입** | 트리 리밸런싱 검증 | 순서 보장, 데이터 무손실 |
| A-4 | **대량 삭제 후 삽입** | 공간 재활용 검증 | compaction 전후 무결성 |
| A-5 | **Mixed 컬렉션 대용량** | 다중 컬렉션 동시 운영 | 컬렉션 간 격리 |

#### Category B: 트랜잭션 경계 (Priority: High)

| ID | 테스트 케이스 | 목적 | 검증 항목 |
|----|-------------|------|----------|
| B-1 | **롤백 후 재시작** | 트랜잭션 원자성 | 커밋 전 데이터 미반영 |
| B-2 | **대량 변경 후 롤백** | 메모리 정리 | OOM 없이 롤백 완료 |
| B-3 | **중첩 트랜잭션 시뮬레이션** | 트랜잭션 격리 | 외부 트랜잭션 우선 |
| B-4 | **읽기 트랜잭션 + 쓰기** | MVCC 검증 | 스냅샷 격리 |
| B-5 | **장기 실행 읽기 트랜잭션** | 리소스 관리 | 오래된 스냅샷 정리 |

#### Category C: 파일 손상 복구 (Priority: Critical)

| ID | 테스트 케이스 | 목적 | 검증 항목 |
|----|-------------|------|----------|
| C-1 | **Superblock 손상 탐지** | 파일 무결성 | verify() 정확한 에러 보고 |
| C-2 | **CommitHeader 손상 탐지** | 헤더 무결성 | 손상 위치/유형 정확 식별 |
| C-3 | **단일 슬롯 손상 복구** | 이중화 효과 | 정상 슬롯으로 복구 |
| C-4 | **allocTail 불일치 탐지** | 할당 무결성 | 범위 위반 정확 식별 |
| C-5 | **compactTo 복구 시나리오** | 손상 파일 복구 | 새 파일로 데이터 복구 |

#### Category D: 동시성 스트레스 (Priority: High)

| ID | 테스트 케이스 | 목적 | 검증 항목 |
|----|-------------|------|----------|
| D-1 | **다중 읽기 트랜잭션 동시** | 읽기 동시성 | 레이스 컨디션 없음 |
| D-2 | **읽기 중 쓰기 (다른 스레드)** | MVCC 격리 | 읽기 일관성 유지 |
| D-3 | **빈번한 커밋 경합** | 쓰기 직렬화 | 데이터 손실 없음 |
| D-4 | **스냅샷 회전 스트레스** | GC 스트레스 | 리소스 누수 없음 |
| D-5 | **장기 실행 + 빈번한 GC** | 메모리 안정성 | OOM 없이 운영 |

#### Category E: 엣지 케이스 (Priority: Medium)

| ID | 테스트 케이스 | 목적 | 검증 항목 |
|----|-------------|------|----------|
| E-1 | **빈 컬렉션 연산** | 경계 조건 | 예외 없이 처리 |
| E-2 | **단일 요소 컬렉션** | 최소 케이스 | 정상 동작 |
| E-3 | **최대 키 크기** | 제한 검증 | 적절한 에러 메시지 |
| E-4 | **최대 값 크기** | 제한 검증 | 페이지 오버플로우 처리 |
| E-5 | **null 값 처리** | null 허용 여부 | 명확한 동작 정의 |

#### Category F: 파일 생명주기 (Priority: Medium)

| ID | 테스트 케이스 | 목적 | 검증 항목 |
|----|-------------|------|----------|
| F-1 | **open-close 반복** | 리소스 관리 | 핸들 누수 없음 |
| F-2 | **compactTo 후 원본 사용** | 복사 독립성 | 원본 영향 없음 |
| F-3 | **파일 이동 후 재오픈** | 경로 무관 | 정상 열림 |
| F-4 | **대용량 파일 compactTo** | 성능 검증 | 합리적 시간 내 완료 |
| F-5 | **다중 compactTo 연속** | 연속 작업 | 파일 무결성 |

---

## 3. 우선 구현 권장 테스트

### 3.1 Phase 1: Critical 위험 대응 (즉시)

```
┌─────────────────────────────────────────────────────────────┐
│  1. A-1: 100K 요소 List CRUD (OST splitInternalNode 검증)   │
│  2. C-1: Superblock 손상 탐지 (verify 경로 검증)            │
│  3. C-2: CommitHeader 손상 탐지 (이중 슬롯 검증)            │
│  4. B-1: 롤백 후 재시작 (트랜잭션 원자성)                   │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Phase 2: High 위험 대응 (1주 내)

```
┌─────────────────────────────────────────────────────────────┐
│  5. D-1: 다중 읽기 트랜잭션 동시 (동시성 안전성)            │
│  6. D-2: 읽기 중 쓰기 (MVCC 격리)                           │
│  7. C-3: 단일 슬롯 손상 복구 (이중화 효과 검증)             │
│  8. A-3: 랜덤 순서 대량 삽입 (리밸런싱 검증)                │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 Phase 3: Medium 위험 대응 (2주 내)

```
┌─────────────────────────────────────────────────────────────┐
│  9-12. E-1 ~ E-4: 엣지 케이스 전체                          │
│  13-16. F-1 ~ F-4: 파일 생명주기 전체                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 테스트 케이스 구현 가이드

### 4.1 A-1: 100K 요소 List CRUD

```java
/**
 * 목적: OST splitInternalNode 경로를 간접적으로 검증
 *
 * 100,000개 요소 삽입으로 OST 트리 깊이 증가 유발
 * Internal 노드 분할이 발생할 가능성 높음
 */
@Test
@Category(IntegrationTest.class)
public void list_100kElements_shouldMaintainIntegrity() {
    // Given: 100K 요소 삽입
    List<Long> list = store.createList("largeList", Long.class);
    for (int i = 0; i < 100_000; i++) {
        list.add((long) i);
    }
    store.commit();

    // When: 무결성 검증
    assertEquals(100_000, list.size());

    // Then: 랜덤 접근 검증
    Random random = new Random(42);
    for (int i = 0; i < 1000; i++) {
        int idx = random.nextInt(100_000);
        assertEquals(Long.valueOf(idx), list.get(idx));
    }

    // And: 순차 접근 검증 (Iterator)
    long expected = 0;
    for (Long value : list) {
        assertEquals(Long.valueOf(expected++), value);
    }

    // And: verify 통과
    assertTrue(store.verify().ok());
}
```

### 4.2 C-1: Superblock 손상 탐지

```java
/**
 * 목적: verifySuperblock 에러 경로 검증
 *
 * 주의: 실제 파일 손상 시뮬레이션
 * 테스트 후 파일 복구 필요
 */
@Test
@Category(IntegrationTest.class)
public void verify_corruptedSuperblock_shouldDetect() {
    // Given: 정상 파일 생성 후 닫기
    store = FxStore.open(testFile.toPath());
    store.createMap("test", String.class, String.class);
    store.commit();
    store.close();

    // When: Superblock 바이트 손상
    try (RandomAccessFile raf = new RandomAccessFile(testFile, "rw")) {
        // Magic 바이트 손상 (offset 0-3)
        raf.seek(0);
        raf.writeByte(0xFF);  // 원래 magic과 다른 값
    }

    // Then: verify()가 에러 보고
    store = FxStore.open(testFile.toPath());
    VerifyResult result = store.verify();

    assertFalse(result.ok());
    assertTrue(result.errors().stream()
        .anyMatch(e -> e.kind() == VerifyErrorKind.SUPERBLOCK));
}
```

### 4.3 C-2: CommitHeader 손상 탐지

```java
/**
 * 목적: verifyCommitHeaders seqNo gap 검증 경로
 */
@Test
@Category(IntegrationTest.class)
public void verify_corruptedCommitHeader_shouldDetect() {
    // Given: 여러 커밋 수행
    store = FxStore.open(testFile.toPath());
    for (int i = 0; i < 5; i++) {
        Map<String, String> map = store.openMap("test", String.class, String.class);
        map.put("key" + i, "value" + i);
        store.commit();
    }
    store.close();

    // When: CommitHeader CRC 손상
    try (RandomAccessFile raf = new RandomAccessFile(testFile, "rw")) {
        // Slot A의 CRC 위치 손상 (Superblock.SIZE + CRC offset)
        long slotAOffset = 4096;  // Superblock.SIZE
        raf.seek(slotAOffset + 100);  // CRC 근처
        raf.writeByte(0xFF);
    }

    // Then: verify()가 에러 보고
    store = FxStore.open(testFile.toPath());
    VerifyResult result = store.verify();

    assertFalse(result.ok());
    assertTrue(result.errors().stream()
        .anyMatch(e -> e.kind() == VerifyErrorKind.HEADER));
}
```

### 4.4 D-1: 다중 읽기 트랜잭션 동시

```java
/**
 * 목적: 동시 읽기 트랜잭션 안전성 검증
 */
@Test
@Category(IntegrationTest.class)
public void concurrentReads_shouldBeIsolated() throws Exception {
    // Given: 초기 데이터
    store = FxStore.open(testFile.toPath());
    NavigableMap<Long, String> map = store.createMap("concurrent", Long.class, String.class);
    for (int i = 0; i < 10_000; i++) {
        map.put((long) i, "value" + i);
    }
    store.commit();

    // When: 10개 스레드에서 동시 읽기
    int threadCount = 10;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicReference<Throwable> error = new AtomicReference<>();

    for (int t = 0; t < threadCount; t++) {
        new Thread(() -> {
            try {
                startLatch.await();
                try (FxReadTransaction tx = store.beginRead()) {
                    NavigableMap<Long, String> txMap = tx.openMap("concurrent", Long.class, String.class);
                    // 전체 순회
                    int count = 0;
                    for (Map.Entry<Long, String> entry : txMap.entrySet()) {
                        assertEquals("value" + entry.getKey(), entry.getValue());
                        count++;
                    }
                    assertEquals(10_000, count);
                    successCount.incrementAndGet();
                }
            } catch (Throwable e) {
                error.set(e);
            } finally {
                endLatch.countDown();
            }
        }).start();
    }

    // Then: 모든 스레드 성공
    startLatch.countDown();
    endLatch.await(30, TimeUnit.SECONDS);

    assertNull(error.get());
    assertEquals(threadCount, successCount.get());
}
```

---

## 5. 테스트 인프라 권장사항

### 5.1 테스트 분류 체계

```java
// 테스트 카테고리 정의
public interface IntegrationTest {}    // 통합 테스트
public interface StressTest {}         // 스트레스 테스트
public interface CorruptionTest {}     // 손상 시뮬레이션 테스트
public interface LongRunningTest {}    // 장시간 실행 테스트
```

### 5.2 Gradle 설정

```groovy
// build.gradle
test {
    useJUnit()

    // 기본 테스트 (빠른 테스트만)
    exclude '**/integration/**'
    exclude '**/stress/**'
}

task integrationTest(type: Test) {
    useJUnit()
    include '**/integration/**'
    maxHeapSize = '2g'
    timeout = Duration.ofMinutes(30)
}

task stressTest(type: Test) {
    useJUnit()
    include '**/stress/**'
    maxHeapSize = '4g'
    timeout = Duration.ofHours(2)
}
```

### 5.3 CI/CD 통합

```yaml
# .github/workflows/test.yml
jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps:
      - run: ./gradlew test

  integration-test:
    runs-on: ubuntu-latest
    needs: unit-test
    steps:
      - run: ./gradlew integrationTest

  stress-test:
    runs-on: ubuntu-latest
    if: github.event_name == 'schedule'  # 야간 빌드만
    steps:
      - run: ./gradlew stressTest
```

---

## 6. 모니터링 및 계측

### 6.1 테스트 메트릭 수집

```java
@Rule
public TestWatcher watcher = new TestWatcher() {
    @Override
    protected void succeeded(Description description) {
        Metrics.counter("test.success",
            "class", description.getClassName(),
            "method", description.getMethodName()
        ).increment();
    }

    @Override
    protected void failed(Throwable e, Description description) {
        Metrics.counter("test.failure",
            "class", description.getClassName(),
            "method", description.getMethodName(),
            "error", e.getClass().getSimpleName()
        ).increment();
    }
};
```

### 6.2 성능 기준선

| 테스트 | 허용 시간 | 메모리 제한 |
|--------|----------|------------|
| 100K 요소 CRUD | < 30초 | < 512MB |
| 동시 읽기 10스레드 | < 10초 | < 256MB |
| compactTo 100MB | < 60초 | < 1GB |

---

## 7. 결론 및 권장사항

### 7.1 즉시 조치 필요 (Phase 1)

1. **A-1 (100K List)**: OST splitInternalNode 간접 검증
2. **C-1 (Superblock 손상)**: 파일 무결성 기본 검증
3. **C-2 (CommitHeader 손상)**: 이중화 효과 검증
4. **B-1 (롤백 후 재시작)**: 트랜잭션 원자성 검증

### 7.2 권장 사항

1. **통합 테스트 디렉토리 분리**
   - `src/test/java/.../integration/` 생성
   - 단위 테스트와 분리 실행

2. **CI/CD 파이프라인 업데이트**
   - 통합 테스트 별도 단계 추가
   - 스트레스 테스트 야간 실행

3. **테스트 데이터 관리**
   - 손상 파일 샘플 저장소 구축
   - 테스트 데이터 버전 관리

### 7.3 예상 효과

| 항목 | 현재 | 통합 테스트 적용 후 |
|------|------|-------------------|
| Critical 버그 탐지 | 낮음 | 높음 |
| 프로덕션 장애 위험 | 중간 | 낮음 |
| 회귀 테스트 신뢰도 | 93% | 98%+ |

---

## 8. 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-12-31 | 최초 작성 |

---

## 부록 A: 테스트 파일 위치

```
src/test/java/com/snoworca/fxstore/
├── integration/                    # 통합 테스트
│   ├── LargeDataIntegrationTest.java
│   ├── CorruptionRecoveryTest.java
│   ├── ConcurrencyStressTest.java
│   └── LifecycleIntegrationTest.java
├── stress/                         # 스트레스 테스트
│   ├── OSTDeepTreeTest.java
│   └── LongRunningOperationTest.java
└── fixtures/                       # 테스트 데이터
    ├── corrupted-superblock.fx
    ├── corrupted-header-a.fx
    └── legacy-v06.fx
```
