# FxStore 통합 테스트 구현 계획

> **문서 버전:** 1.0
> **작성일:** 2025-12-31
> **기준 문서:** [INTEGRATION-TEST-RESEARCH.md](INTEGRATION-TEST-RESEARCH.md), [03.quality-criteria.md](03.quality-criteria.md)
> **목표:** 7가지 품질 기준 A+ 달성

[← 목차로 돌아가기](00.index.md)

---

## 1. 개요

### 1.1 배경

현재 FxStore는 93%/87% (Instruction/Branch) 커버리지를 달성했으나, 테스트로 커버되지 않는 특수 상황에서 버그가 발생할 위험이 있습니다. 이 계획은 [INTEGRATION-TEST-RESEARCH.md](INTEGRATION-TEST-RESEARCH.md)에서 식별된 위험 영역을 검증하기 위한 통합 테스트 구현 계획입니다.

### 1.2 목표

| 항목 | 목표 |
|------|------|
| **신규 통합 테스트** | 30개 케이스 |
| **위험 영역 커버** | Critical/High 100% |
| **테스트 통과율** | 100% |
| **품질 기준** | 7/7 A+ |

### 1.3 범위

```
┌─────────────────────────────────────────────────────────────┐
│  Category A: 대용량 데이터 무결성 테스트 (5개)              │
│  Category B: 트랜잭션 경계 테스트 (5개)                     │
│  Category C: 파일 손상 복구 테스트 (5개)                    │
│  Category D: 동시성 스트레스 테스트 (5개)                   │
│  Category E: 엣지 케이스 테스트 (5개)                       │
│  Category F: 파일 생명주기 테스트 (5개)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 아키텍처 설계

### 2.1 테스트 디렉토리 구조

```
src/test/java/com/snoworca/fxstore/
├── integration/                          # 통합 테스트 패키지
│   ├── category/                         # 카테고리별 분류
│   │   ├── LargeDataIntegrationTest.java     # Category A
│   │   ├── TransactionBoundaryTest.java      # Category B
│   │   ├── CorruptionRecoveryTest.java       # Category C
│   │   ├── ConcurrencyStressTest.java        # Category D
│   │   ├── EdgeCaseIntegrationTest.java      # Category E
│   │   └── LifecycleIntegrationTest.java     # Category F
│   ├── util/                             # 테스트 유틸리티
│   │   ├── TestDataGenerator.java            # 테스트 데이터 생성
│   │   ├── FileCorruptor.java                # 파일 손상 시뮬레이션
│   │   └── ConcurrencyTestHelper.java        # 동시성 테스트 헬퍼
│   └── IntegrationTestBase.java          # 공통 기반 클래스
└── fixtures/                             # 테스트 픽스처
    └── README.md                         # 픽스처 설명
```

### 2.2 기반 클래스 설계

```java
/**
 * 통합 테스트 기반 클래스
 *
 * <p>SOLID 준수:
 * - SRP: 테스트 설정/정리만 담당
 * - OCP: 서브클래스에서 확장 가능
 * - DIP: FxStore 인터페이스에 의존
 */
public abstract class IntegrationTestBase {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(300);  // 5분 제한

    protected File storeFile;
    protected FxStore store;

    @Before
    public void setUpBase() throws Exception {
        storeFile = tempFolder.newFile("test.fx");
        storeFile.delete();  // FxStore가 새로 생성
    }

    @After
    public void tearDownBase() {
        closeStore();
    }

    protected void openStore() throws Exception {
        store = FxStore.open(storeFile.toPath());
    }

    protected void closeStore() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
            store = null;
        }
    }

    protected void reopenStore() throws Exception {
        closeStore();
        openStore();
    }
}
```

### 2.3 테스트 유틸리티 설계

#### 2.3.1 TestDataGenerator

```java
/**
 * 테스트 데이터 생성기
 *
 * <p>SOLID 준수:
 * - SRP: 테스트 데이터 생성만 담당
 * - OCP: 새 데이터 타입 추가 시 확장 가능
 */
public final class TestDataGenerator {

    private TestDataGenerator() {}  // 유틸리티 클래스

    /**
     * 순차적 Long 데이터 생성
     */
    public static List<Long> sequentialLongs(int count) {
        List<Long> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add((long) i);
        }
        return result;
    }

    /**
     * 랜덤 Long 데이터 생성 (재현 가능한 시드)
     */
    public static List<Long> randomLongs(int count, long seed) {
        Random random = new Random(seed);
        List<Long> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(random.nextLong());
        }
        return result;
    }

    /**
     * 큰 문자열 데이터 생성
     */
    public static String largeString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        return sb.toString();
    }
}
```

#### 2.3.2 FileCorruptor

```java
/**
 * 파일 손상 시뮬레이션 유틸리티
 *
 * <p>주의: 테스트 목적으로만 사용
 */
public final class FileCorruptor {

    private FileCorruptor() {}

    /**
     * Superblock Magic 바이트 손상
     */
    public static void corruptSuperblockMagic(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(0);
            raf.writeByte(0xFF);  // Magic 바이트 손상
        }
    }

    /**
     * CommitHeader CRC 손상
     */
    public static void corruptCommitHeaderCRC(File file, int slot) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            long offset = 4096 + (slot * 512);  // Superblock.SIZE + slot * CommitHeader.SIZE
            raf.seek(offset + 500);  // CRC 근처
            raf.writeByte(0xFF);
        }
    }

    /**
     * 파일 트렁케이션 (부분 쓰기 시뮬레이션)
     */
    public static void truncateFile(File file, long newSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(newSize);
        }
    }
}
```

#### 2.3.3 ConcurrencyTestHelper

```java
/**
 * 동시성 테스트 헬퍼
 */
public final class ConcurrencyTestHelper {

    private ConcurrencyTestHelper() {}

    /**
     * 여러 스레드에서 동시 실행
     *
     * @param threadCount 스레드 수
     * @param task 각 스레드에서 실행할 작업
     * @return 모든 스레드 성공 여부
     */
    public static boolean runConcurrently(int threadCount, Runnable task)
            throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    task.run();
                } catch (Throwable e) {
                    success.set(false);
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();  // 모든 스레드 동시 시작
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);

        if (!completed) {
            throw new AssertionError("Timeout: threads did not complete in 60 seconds");
        }

        if (error.get() != null) {
            throw new AssertionError("Thread failed", error.get());
        }

        return success.get();
    }
}
```

---

## 3. 테스트 케이스 상세 설계

### 3.1 Category A: 대용량 데이터 무결성 테스트

#### A-1: 100K 요소 List CRUD

```java
/**
 * OST splitInternalNode 간접 검증
 *
 * <p>목적: 100,000개 요소 삽입으로 OST 깊은 트리 생성
 * <p>위험 영역: splitInternalNode (0% 커버리지)
 * <p>검증: 데이터 무결성, 랜덤 접근 정확성
 */
@Test
public void test_A1_list100kElements_shouldMaintainIntegrity() throws Exception {
    // Given
    openStore();
    List<Long> list = store.createList("largeList", Long.class);

    // When: 100K 요소 삽입
    for (int i = 0; i < 100_000; i++) {
        list.add((long) i);
    }
    store.commit();

    // Then: 크기 검증
    assertEquals(100_000, list.size());

    // And: 랜덤 접근 검증 (1000개 샘플)
    Random random = new Random(42);
    for (int i = 0; i < 1000; i++) {
        int idx = random.nextInt(100_000);
        assertEquals(Long.valueOf(idx), list.get(idx));
    }

    // And: 순차 접근 검증 (Iterator)
    int expected = 0;
    for (Long value : list) {
        assertEquals(Long.valueOf(expected++), value);
    }

    // And: verify 통과
    assertTrue(store.verify().ok());
}
```

#### A-2: 100K 요소 Map CRUD

```java
/**
 * BTree 깊은 트리 검증
 */
@Test
public void test_A2_map100kElements_shouldMaintainSortOrder() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("largeMap", Long.class, String.class);

    // When: 100K 요소 삽입 (역순)
    for (int i = 99_999; i >= 0; i--) {
        map.put((long) i, "value" + i);
    }
    store.commit();

    // Then: 크기 검증
    assertEquals(100_000, map.size());

    // And: 정렬 순서 검증
    Long prevKey = null;
    for (Long key : map.keySet()) {
        if (prevKey != null) {
            assertTrue("Keys must be sorted", prevKey < key);
        }
        prevKey = key;
    }

    // And: 범위 쿼리 검증
    assertEquals(Long.valueOf(50_000L), map.ceilingKey(50_000L));
    assertEquals(Long.valueOf(49_999L), map.floorKey(49_999L));
}
```

#### A-3: 랜덤 순서 대량 삽입

```java
/**
 * 트리 리밸런싱 검증
 */
@Test
public void test_A3_randomInsert_shouldRebalance() throws Exception {
    // Given
    openStore();
    List<Long> list = store.createList("randomList", Long.class);
    List<Long> expected = new ArrayList<>();

    // When: 초기 데이터 + 랜덤 위치 삽입
    Random random = new Random(42);
    for (int i = 0; i < 10_000; i++) {
        if (list.isEmpty()) {
            list.add((long) i);
            expected.add((long) i);
        } else {
            int pos = random.nextInt(list.size() + 1);
            list.add(pos, (long) i);
            expected.add(pos, (long) i);
        }
    }
    store.commit();

    // Then: ArrayList와 동일한 결과
    assertEquals(expected.size(), list.size());
    for (int i = 0; i < expected.size(); i++) {
        assertEquals(expected.get(i), list.get(i));
    }
}
```

#### A-4: 대량 삭제 후 삽입

```java
/**
 * 공간 재활용 검증
 */
@Test
public void test_A4_deleteAndInsert_shouldReuseSpace() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("deleteMap", Long.class, String.class);

    // When: 대량 삽입
    for (int i = 0; i < 50_000; i++) {
        map.put((long) i, "value" + i);
    }
    store.commit();
    long sizeAfterInsert = store.stats(StatsMode.FAST).fileBytes();

    // When: 절반 삭제
    for (int i = 0; i < 25_000; i++) {
        map.remove((long) i);
    }
    store.commit();

    // When: 다시 삽입
    for (int i = 50_000; i < 75_000; i++) {
        map.put((long) i, "newvalue" + i);
    }
    store.commit();

    // Then: 데이터 무결성
    assertEquals(50_000, map.size());
    assertNull(map.get(0L));  // 삭제된 데이터
    assertEquals("value25000", map.get(25_000L));  // 유지된 데이터
    assertEquals("newvalue50000", map.get(50_000L));  // 새 데이터
}
```

#### A-5: Mixed 컬렉션 대용량

```java
/**
 * 다중 컬렉션 동시 운영
 */
@Test
public void test_A5_mixedCollections_shouldIsolate() throws Exception {
    // Given
    openStore();
    List<Long> list = store.createList("bigList", Long.class);
    NavigableMap<Long, String> map = store.createMap("bigMap", Long.class, String.class);
    NavigableSet<Long> set = store.createSet("bigSet", Long.class);
    Deque<Long> deque = store.createDeque("bigDeque", Long.class);

    // When: 각 컬렉션에 대량 데이터
    for (int i = 0; i < 10_000; i++) {
        list.add((long) i);
        map.put((long) i, "v" + i);
        set.add((long) i);
        deque.addLast((long) i);
    }
    store.commit();

    // Then: 각 컬렉션 독립적으로 정확
    assertEquals(10_000, list.size());
    assertEquals(10_000, map.size());
    assertEquals(10_000, set.size());
    assertEquals(10_000, deque.size());

    // And: DEEP 통계 정확
    Stats stats = store.stats(StatsMode.DEEP);
    assertEquals(4, stats.collectionCount());
}
```

### 3.2 Category B: 트랜잭션 경계 테스트

#### B-1: 롤백 후 재시작

```java
/**
 * 트랜잭션 원자성 검증
 */
@Test
public void test_B1_rollback_shouldDiscardChanges() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("rollbackMap", Long.class, String.class);
    map.put(1L, "committed");
    store.commit();

    // When: 변경 후 롤백
    map.put(2L, "uncommitted");
    map.put(1L, "modified");
    store.rollback();

    // Then: 롤백 전 상태 유지
    assertEquals(1, map.size());
    assertEquals("committed", map.get(1L));
    assertNull(map.get(2L));

    // And: 재시작 후에도 유지
    reopenStore();
    map = store.openMap("rollbackMap", Long.class, String.class);
    assertEquals(1, map.size());
    assertEquals("committed", map.get(1L));
}
```

#### B-2: 대량 변경 후 롤백

```java
/**
 * 대량 롤백 시 메모리 정리 검증
 */
@Test
public void test_B2_largeRollback_shouldCleanMemory() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("largeRollback", Long.class, String.class);

    // 초기 데이터
    for (int i = 0; i < 1000; i++) {
        map.put((long) i, "initial" + i);
    }
    store.commit();

    // When: 대량 변경
    for (int i = 0; i < 50_000; i++) {
        map.put((long) (1000 + i), "uncommitted" + i);
    }

    // Then: 롤백 성공 (OOM 없음)
    store.rollback();

    // And: 초기 상태 유지
    assertEquals(1000, map.size());
    assertEquals("initial0", map.get(0L));
}
```

#### B-3: 읽기 트랜잭션 격리

```java
/**
 * 읽기 트랜잭션 스냅샷 격리 검증
 */
@Test
public void test_B3_readTransaction_shouldBeIsolated() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("isolationMap", Long.class, String.class);
    map.put(1L, "original");
    store.commit();

    // When: 읽기 트랜잭션 시작
    try (FxReadTransaction tx = store.beginRead()) {
        NavigableMap<Long, String> txMap = tx.openMap("isolationMap", Long.class, String.class);

        // 메인 스레드에서 수정
        map.put(1L, "modified");
        map.put(2L, "new");
        store.commit();

        // Then: 읽기 트랜잭션은 원래 값 유지
        assertEquals("original", txMap.get(1L));
        assertNull(txMap.get(2L));
        assertEquals(1, txMap.size());
    }

    // And: 트랜잭션 종료 후 최신 값 접근
    assertEquals("modified", map.get(1L));
    assertEquals("new", map.get(2L));
}
```

#### B-4: 장기 실행 읽기 트랜잭션

```java
/**
 * 장기 실행 트랜잭션 리소스 관리
 */
@Test
public void test_B4_longRunningRead_shouldMaintainSnapshot() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("longRunning", Long.class, String.class);
    for (int i = 0; i < 1000; i++) {
        map.put((long) i, "v" + i);
    }
    store.commit();

    // When: 읽기 트랜잭션 유지하면서 여러 커밋 발생
    try (FxReadTransaction tx = store.beginRead()) {
        NavigableMap<Long, String> txMap = tx.openMap("longRunning", Long.class, String.class);

        // 여러 번 커밋
        for (int batch = 0; batch < 10; batch++) {
            for (int i = 0; i < 100; i++) {
                map.put((long) (1000 + batch * 100 + i), "new" + i);
            }
            store.commit();
        }

        // Then: 읽기 트랜잭션은 원래 스냅샷 유지
        assertEquals(1000, txMap.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals("v" + i, txMap.get((long) i));
        }
    }
}
```

#### B-5: 커밋 없이 닫기

```java
/**
 * OnClosePolicy 동작 검증
 */
@Test
public void test_B5_closeWithoutCommit_shouldFollowPolicy() throws Exception {
    // Given: ROLLBACK 정책으로 열기
    store = FxStore.builder()
            .path(storeFile.toPath())
            .onClosePolicy(OnClosePolicy.ROLLBACK)
            .build();

    NavigableMap<Long, String> map = store.createMap("policyMap", Long.class, String.class);
    map.put(1L, "committed");
    store.commit();

    map.put(2L, "uncommitted");

    // When: 커밋 없이 닫기
    store.close();
    store = null;

    // Then: uncommitted 데이터 롤백됨
    openStore();
    map = store.openMap("policyMap", Long.class, String.class);
    assertEquals(1, map.size());
    assertEquals("committed", map.get(1L));
    assertNull(map.get(2L));
}
```

### 3.3 Category C: 파일 손상 복구 테스트

#### C-1: Superblock 손상 탐지

```java
/**
 * verifySuperblock 에러 경로 검증
 */
@Test
public void test_C1_corruptedSuperblock_shouldDetect() throws Exception {
    // Given: 정상 파일 생성
    openStore();
    store.createMap("test", String.class, String.class);
    store.commit();
    closeStore();

    // When: Superblock 손상
    FileCorruptor.corruptSuperblockMagic(storeFile);

    // Then: verify()가 에러 보고
    openStore();
    VerifyResult result = store.verify();

    assertFalse(result.ok());
    assertTrue(result.errors().stream()
            .anyMatch(e -> e.kind() == VerifyErrorKind.SUPERBLOCK));
}
```

#### C-2: CommitHeader 손상 탐지

```java
/**
 * verifyCommitHeaders 에러 경로 검증
 */
@Test
public void test_C2_corruptedCommitHeader_shouldDetect() throws Exception {
    // Given: 여러 커밋 수행
    openStore();
    NavigableMap<Long, String> map = store.createMap("headerTest", Long.class, String.class);
    for (int i = 0; i < 5; i++) {
        map.put((long) i, "value" + i);
        store.commit();
    }
    closeStore();

    // When: CommitHeader Slot A 손상
    FileCorruptor.corruptCommitHeaderCRC(storeFile, 0);

    // Then: verify()가 에러 보고
    openStore();
    VerifyResult result = store.verify();

    assertFalse(result.ok());
    assertTrue(result.errors().stream()
            .anyMatch(e -> e.kind() == VerifyErrorKind.HEADER));
}
```

#### C-3: 단일 슬롯 손상 복구

```java
/**
 * 이중 슬롯 복구 효과 검증
 */
@Test
public void test_C3_singleSlotCorruption_shouldRecoverFromOther() throws Exception {
    // Given: 정상 데이터
    openStore();
    NavigableMap<Long, String> map = store.createMap("recoverMap", Long.class, String.class);
    map.put(1L, "important");
    store.commit();
    closeStore();

    // When: 한쪽 슬롯만 손상 (다른 슬롯은 정상)
    FileCorruptor.corruptCommitHeaderCRC(storeFile, 0);

    // Then: 정상 슬롯으로 열림
    openStore();
    map = store.openMap("recoverMap", Long.class, String.class);
    assertEquals("important", map.get(1L));
}
```

#### C-4: allocTail 불일치 탐지

```java
/**
 * verifyAllocTail 경로 검증
 */
@Test
public void test_C4_allocTailMismatch_shouldDetect() throws Exception {
    // Given: 대량 데이터로 파일 크기 증가
    openStore();
    List<Long> list = store.createList("allocTest", Long.class);
    for (int i = 0; i < 10_000; i++) {
        list.add((long) i);
    }
    store.commit();
    long originalSize = storeFile.length();
    closeStore();

    // When: 파일 트렁케이션 (allocTail > fileSize 상황)
    FileCorruptor.truncateFile(storeFile, originalSize - 4096);

    // Then: verify()가 에러 보고
    openStore();
    VerifyResult result = store.verify();

    assertFalse(result.ok());
    // 트렁케이션으로 인한 에러 검출
}
```

#### C-5: compactTo 복구

```java
/**
 * 손상 파일에서 compactTo 복구
 */
@Test
public void test_C5_compactTo_shouldRecoverData() throws Exception {
    // Given: 정상 데이터
    openStore();
    NavigableMap<Long, String> map = store.createMap("compactMap", Long.class, String.class);
    for (int i = 0; i < 1000; i++) {
        map.put((long) i, "value" + i);
    }
    store.commit();

    // When: compactTo로 새 파일 생성
    File targetFile = tempFolder.newFile("compact.fx");
    targetFile.delete();
    store.compactTo(targetFile.toPath());
    closeStore();

    // Then: 새 파일에서 데이터 확인
    try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
        NavigableMap<Long, String> targetMap =
                targetStore.openMap("compactMap", Long.class, String.class);
        assertEquals(1000, targetMap.size());
        assertEquals("value0", targetMap.get(0L));
        assertEquals("value999", targetMap.get(999L));

        // verify 통과
        assertTrue(targetStore.verify().ok());
    }
}
```

### 3.4 Category D: 동시성 스트레스 테스트

#### D-1: 다중 읽기 트랜잭션 동시

```java
/**
 * 동시 읽기 트랜잭션 안전성
 */
@Test
public void test_D1_concurrentReads_shouldBeThreadSafe() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("concurrentRead", Long.class, String.class);
    for (int i = 0; i < 10_000; i++) {
        map.put((long) i, "value" + i);
    }
    store.commit();

    // When: 10개 스레드에서 동시 읽기
    final int threadCount = 10;
    AtomicInteger successCount = new AtomicInteger(0);

    boolean success = ConcurrencyTestHelper.runConcurrently(threadCount, () -> {
        try (FxReadTransaction tx = store.beginRead()) {
            NavigableMap<Long, String> txMap =
                    tx.openMap("concurrentRead", Long.class, String.class);

            // 전체 순회
            int count = 0;
            for (Map.Entry<Long, String> entry : txMap.entrySet()) {
                assertEquals("value" + entry.getKey(), entry.getValue());
                count++;
            }
            assertEquals(10_000, count);
            successCount.incrementAndGet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    // Then
    assertTrue(success);
    assertEquals(threadCount, successCount.get());
}
```

#### D-2: 읽기 중 쓰기

```java
/**
 * 읽기 중 쓰기 MVCC 격리
 */
@Test
public void test_D2_readWhileWrite_shouldIsolate() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("mvccMap", Long.class, String.class);
    for (int i = 0; i < 1000; i++) {
        map.put((long) i, "v" + i);
    }
    store.commit();

    final AtomicBoolean readerSuccess = new AtomicBoolean(false);
    final CountDownLatch readerStarted = new CountDownLatch(1);
    final CountDownLatch writerDone = new CountDownLatch(1);

    // When: 읽기 스레드
    Thread reader = new Thread(() -> {
        try (FxReadTransaction tx = store.beginRead()) {
            NavigableMap<Long, String> txMap =
                    tx.openMap("mvccMap", Long.class, String.class);
            readerStarted.countDown();

            // 쓰기 완료 대기
            writerDone.await();

            // 원래 값 확인
            assertEquals(1000, txMap.size());
            assertEquals("v0", txMap.get(0L));
            readerSuccess.set(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
    reader.start();

    // 쓰기
    readerStarted.await();
    for (int i = 0; i < 500; i++) {
        map.put((long) (1000 + i), "new" + i);
    }
    store.commit();
    writerDone.countDown();

    reader.join(10000);

    // Then
    assertTrue(readerSuccess.get());
    assertEquals(1500, map.size());  // 메인 스레드는 최신 값
}
```

#### D-3: 빈번한 커밋 경합

```java
/**
 * 빈번한 커밋 시 데이터 손실 없음
 */
@Test
public void test_D3_frequentCommits_shouldNotLoseData() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("frequentCommit", Long.class, String.class);

    // When: 빠른 연속 커밋
    for (int i = 0; i < 1000; i++) {
        map.put((long) i, "value" + i);
        store.commit();
    }

    // Then: 모든 데이터 존재
    assertEquals(1000, map.size());

    // And: 재시작 후에도 유지
    reopenStore();
    map = store.openMap("frequentCommit", Long.class, String.class);
    assertEquals(1000, map.size());
    for (int i = 0; i < 1000; i++) {
        assertEquals("value" + i, map.get((long) i));
    }
}
```

#### D-4: 스냅샷 회전 스트레스

```java
/**
 * 스냅샷 회전 시 리소스 누수 없음
 */
@Test
public void test_D4_snapshotRotation_shouldNotLeak() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("snapshotRotation", Long.class, String.class);

    // When: 많은 읽기 트랜잭션 생성/종료
    for (int i = 0; i < 1000; i++) {
        map.put((long) i, "v" + i);
        store.commit();

        // 읽기 트랜잭션 생성 후 즉시 종료
        try (FxReadTransaction tx = store.beginRead()) {
            NavigableMap<Long, String> txMap =
                    tx.openMap("snapshotRotation", Long.class, String.class);
            assertEquals(i + 1, txMap.size());
        }
    }

    // Then: verify 통과 (리소스 정상 해제)
    assertTrue(store.verify().ok());
}
```

#### D-5: 장기 실행 + 빈번한 GC

```java
/**
 * 장기 실행 시 메모리 안정성
 */
@Test
public void test_D5_longRunningWithGC_shouldBeStable() throws Exception {
    // Given
    openStore();
    NavigableMap<Long, String> map = store.createMap("longRunningGC", Long.class, String.class);

    // When: 반복적인 삽입/삭제 + GC
    for (int round = 0; round < 100; round++) {
        // 삽입
        for (int i = 0; i < 1000; i++) {
            map.put((long) (round * 1000 + i), "value" + i);
        }
        store.commit();

        // 일부 삭제
        for (int i = 0; i < 500; i++) {
            map.remove((long) (round * 1000 + i));
        }
        store.commit();

        // 강제 GC (메모리 부담 테스트)
        if (round % 10 == 0) {
            System.gc();
        }
    }

    // Then: 정상 동작 (OOM 없음)
    assertTrue(map.size() > 0);
    assertTrue(store.verify().ok());
}
```

### 3.5 Category E: 엣지 케이스 테스트

#### E-1 ~ E-5

```java
/**
 * 빈 컬렉션 연산
 */
@Test
public void test_E1_emptyCollection_shouldHandleGracefully() throws Exception {
    openStore();
    NavigableMap<Long, String> map = store.createMap("emptyMap", Long.class, String.class);
    store.commit();

    // Then: 빈 컬렉션 연산
    assertEquals(0, map.size());
    assertTrue(map.isEmpty());
    assertNull(map.get(1L));
    assertNull(map.firstKey());
    assertNull(map.lastKey());
    assertNull(map.pollFirstEntry());
    assertNull(map.pollLastEntry());
    assertTrue(map.keySet().isEmpty());
    assertTrue(map.values().isEmpty());
    assertTrue(map.entrySet().isEmpty());
}

/**
 * 단일 요소 컬렉션
 */
@Test
public void test_E2_singleElement_shouldWork() throws Exception {
    openStore();
    List<String> list = store.createList("singleList", String.class);
    list.add("only");
    store.commit();

    assertEquals(1, list.size());
    assertEquals("only", list.get(0));
    assertEquals("only", list.remove(0));
    assertTrue(list.isEmpty());
}

/**
 * 최대 키/값 크기
 */
@Test
public void test_E3_largeKeyValue_shouldHandleWithinLimit() throws Exception {
    openStore();
    NavigableMap<String, String> map = store.createMap("largeKV", String.class, String.class);

    // 큰 키/값 (1KB 미만은 허용)
    String largeKey = TestDataGenerator.largeString(500);
    String largeValue = TestDataGenerator.largeString(1000);

    map.put(largeKey, largeValue);
    store.commit();

    assertEquals(largeValue, map.get(largeKey));
}

/**
 * 특수 문자 처리
 */
@Test
public void test_E4_specialCharacters_shouldPreserve() throws Exception {
    openStore();
    NavigableMap<String, String> map = store.createMap("specialChar", String.class, String.class);

    // 유니코드, 이모지, 제어 문자
    String[] specialKeys = {
        "한글키",
        "日本語",
        "emoji\uD83D\uDE00",
        "tab\there",
        "newline\nhere"
    };

    for (String key : specialKeys) {
        map.put(key, "value:" + key);
    }
    store.commit();

    reopenStore();
    map = store.openMap("specialChar", String.class, String.class);
    for (String key : specialKeys) {
        assertEquals("value:" + key, map.get(key));
    }
}

/**
 * null 키 처리 (예외 발생해야 함)
 */
@Test(expected = FxException.class)
public void test_E5_nullKey_shouldThrow() throws Exception {
    openStore();
    NavigableMap<String, String> map = store.createMap("nullTest", String.class, String.class);
    map.put(null, "value");
}
```

### 3.6 Category F: 파일 생명주기 테스트

#### F-1 ~ F-5

```java
/**
 * open-close 반복
 */
@Test
public void test_F1_repeatedOpenClose_shouldNotLeak() throws Exception {
    for (int i = 0; i < 100; i++) {
        openStore();
        NavigableMap<Long, String> map = store.openOrCreateMap("repeatMap", Long.class, String.class);
        map.put((long) i, "value" + i);
        store.commit();
        closeStore();
    }

    // 최종 확인
    openStore();
    NavigableMap<Long, String> map = store.openMap("repeatMap", Long.class, String.class);
    assertEquals(100, map.size());
}

/**
 * compactTo 후 원본 독립성
 */
@Test
public void test_F2_compactTo_shouldBeIndependent() throws Exception {
    openStore();
    NavigableMap<Long, String> map = store.createMap("compactIndep", Long.class, String.class);
    map.put(1L, "original");
    store.commit();

    File targetFile = tempFolder.newFile("compact.fx");
    targetFile.delete();
    store.compactTo(targetFile.toPath());

    // 원본 수정
    map.put(1L, "modified");
    map.put(2L, "new");
    store.commit();

    // 복사본은 원래 값 유지
    try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
        NavigableMap<Long, String> targetMap =
                targetStore.openMap("compactIndep", Long.class, String.class);
        assertEquals("original", targetMap.get(1L));
        assertNull(targetMap.get(2L));
    }
}

/**
 * 여러 컬렉션 생성/삭제
 */
@Test
public void test_F3_createDropCollections_shouldManageCorrectly() throws Exception {
    openStore();

    // 여러 컬렉션 생성
    for (int i = 0; i < 50; i++) {
        store.createMap("map" + i, Long.class, String.class);
    }
    store.commit();

    // 일부 삭제
    for (int i = 0; i < 25; i++) {
        store.drop("map" + i);
    }
    store.commit();

    // 재시작 후 확인
    reopenStore();
    assertEquals(25, store.list().size());

    // 삭제된 컬렉션 접근 시 예외
    try {
        store.openMap("map0", Long.class, String.class);
        fail("Should throw NOT_FOUND");
    } catch (FxException e) {
        assertEquals(FxErrorCode.NOT_FOUND, e.errorCode());
    }
}

/**
 * 대용량 파일 compactTo 성능
 */
@Test
public void test_F4_largeFileCompact_shouldCompleteInTime() throws Exception {
    openStore();
    NavigableMap<Long, String> map = store.createMap("largeCompact", Long.class, String.class);

    // 대량 데이터
    for (int i = 0; i < 50_000; i++) {
        map.put((long) i, "value" + i);
    }
    store.commit();

    // 일부 삭제 (dead space 생성)
    for (int i = 0; i < 25_000; i++) {
        map.remove((long) i);
    }
    store.commit();

    long originalSize = storeFile.length();

    // compactTo 실행 시간 측정
    File targetFile = tempFolder.newFile("largeCompact.fx");
    targetFile.delete();

    long startTime = System.currentTimeMillis();
    store.compactTo(targetFile.toPath());
    long elapsed = System.currentTimeMillis() - startTime;

    // 60초 이내 완료
    assertTrue("CompactTo should complete within 60 seconds", elapsed < 60_000);

    // 파일 크기 감소
    assertTrue("Compacted file should be smaller",
               targetFile.length() < originalSize);
}

/**
 * 연속 compactTo
 */
@Test
public void test_F5_consecutiveCompactTo_shouldMaintainIntegrity() throws Exception {
    openStore();
    NavigableMap<Long, String> map = store.createMap("consCompact", Long.class, String.class);
    for (int i = 0; i < 1000; i++) {
        map.put((long) i, "v" + i);
    }
    store.commit();

    // 연속 compactTo
    File current = storeFile;
    for (int round = 0; round < 5; round++) {
        File target = tempFolder.newFile("compact" + round + ".fx");
        target.delete();

        try (FxStore sourceStore = FxStore.open(current.toPath())) {
            sourceStore.compactTo(target.toPath());
        }

        current = target;
    }

    // 최종 파일 무결성
    try (FxStore finalStore = FxStore.open(current.toPath())) {
        NavigableMap<Long, String> finalMap =
                finalStore.openMap("consCompact", Long.class, String.class);
        assertEquals(1000, finalMap.size());
        assertTrue(finalStore.verify().ok());
    }
}
```

---

## 4. 구현 일정 (WBS)

### 4.1 Phase 1: 인프라 구축 (1일)

| 작업 | 산출물 | 시간 |
|------|--------|------|
| 테스트 디렉토리 구조 생성 | integration/, util/ | 0.5h |
| IntegrationTestBase 구현 | IntegrationTestBase.java | 1h |
| TestDataGenerator 구현 | TestDataGenerator.java | 0.5h |
| FileCorruptor 구현 | FileCorruptor.java | 1h |
| ConcurrencyTestHelper 구현 | ConcurrencyTestHelper.java | 1h |

### 4.2 Phase 2: Category A-C 구현 (2일)

| 작업 | 산출물 | 시간 |
|------|--------|------|
| Category A (5개) | LargeDataIntegrationTest.java | 4h |
| Category B (5개) | TransactionBoundaryTest.java | 4h |
| Category C (5개) | CorruptionRecoveryTest.java | 4h |

### 4.3 Phase 3: Category D-F 구현 (2일)

| 작업 | 산출물 | 시간 |
|------|--------|------|
| Category D (5개) | ConcurrencyStressTest.java | 4h |
| Category E (5개) | EdgeCaseIntegrationTest.java | 3h |
| Category F (5개) | LifecycleIntegrationTest.java | 3h |

### 4.4 Phase 4: 검증 및 문서화 (1일)

| 작업 | 산출물 | 시간 |
|------|--------|------|
| 전체 테스트 실행 | 테스트 결과 | 2h |
| 커버리지 확인 | JaCoCo 리포트 | 1h |
| 문서 업데이트 | 00.index.md | 1h |
| 품질 평가 | 평가 문서 | 2h |

**총 기간: 6일**

---

## 5. 검증 기준

### 5.1 테스트 통과 기준

| 항목 | 기준 |
|------|------|
| 전체 테스트 | 100% 통과 |
| 테스트 시간 | 5분 이내 완료 |
| 메모리 사용 | OOM 없음 |
| 리소스 누수 | 파일 핸들 정상 해제 |

### 5.2 품질 기준 목표

| 기준 | 목표 점수 |
|------|----------|
| Plan-Code 정합성 | 95+ (A+) |
| SOLID 원칙 준수 | 95+ (A+) |
| 테스트 커버리지 | 95+ (A+) |
| 코드 가독성 | 95+ (A+) |
| 예외 처리 | 95+ (A+) |
| 성능 효율성 | 95+ (A+) |
| 문서화 품질 | 95+ (A+) |

---

## 6. 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-12-31 | 최초 작성 |

---

[← 목차로 돌아가기](00.index.md)
