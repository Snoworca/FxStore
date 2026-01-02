# Phase 8: 동시성 지원 테스트 시나리오

> **문서 버전:** 1.0
> **대상 Phase:** Phase 8 (동시성 지원)
> **테스트 유형:** Unit, Stress, Race Condition, Benchmark

[← 목차로 돌아가기](00.index.md)

---

## 목차

- [1. StoreSnapshot 테스트](#1-storesnapshot-테스트)
- [2. ConcurrentPageCache 테스트](#2-concurrentpagecache-테스트)
- [3. Wait-free 읽기 테스트](#3-wait-free-읽기-테스트)
- [4. Write Lock 쓰기 테스트](#4-write-lock-쓰기-테스트)
- [5. 동시성 스트레스 테스트](#5-동시성-스트레스-테스트)
- [6. Race Condition 테스트](#6-race-condition-테스트)
- [7. BATCH 모드 동시성 테스트](#7-batch-모드-동시성-테스트)
- [8. Iterator 스냅샷 격리 테스트](#8-iterator-스냅샷-격리-테스트)
- [9. 성능 벤치마크](#9-성능-벤치마크)
- [10. 불변식 검증](#10-불변식-검증)

---

## 1. StoreSnapshot 테스트

### 1.1 불변성 검증

| ID | 시나리오 | 입력 | 기대 결과 |
|----|---------|------|----------|
| SS-001 | 원본 Map 수정 후 스냅샷 불변 | 원본 catalog에 항목 추가 | 스냅샷 catalog는 변경되지 않음 |
| SS-002 | 스냅샷 Map 직접 수정 시도 | `snapshot.getCatalog().put()` | `UnsupportedOperationException` |
| SS-003 | null 파라미터 | catalog = null | `NullPointerException` |
| SS-004 | 빈 Map 스냅샷 | 모두 빈 Map | 정상 생성, 모든 getter가 빈 Map 반환 |

**테스트 코드 예시:**
```java
@Test
public void testImmutability_SS001() {
    Map<String, CatalogEntry> original = new HashMap<>();
    original.put("col1", entry1);

    StoreSnapshot snap = new StoreSnapshot(1L, 1024L, original, ...);

    // 원본 수정
    original.put("col2", entry2);

    // 스냅샷 불변 확인
    assertEquals(1, snap.getCatalog().size());
    assertFalse(snap.getCatalog().containsKey("col2"));
}

@Test(expected = UnsupportedOperationException.class)
public void testImmutability_SS002() {
    StoreSnapshot snap = new StoreSnapshot(1L, 1024L, ...);
    snap.getCatalog().put("new", entry);  // 예외 발생
}
```

### 1.2 with* 메서드 테스트

| ID | 시나리오 | 입력 | 기대 결과 |
|----|---------|------|----------|
| SS-010 | withAllocTail | 새 allocTail 값 | 새 스냅샷 생성, 원본 불변, seqNo 증가 |
| SS-011 | withRootPageId | collectionId, newRootPageId | 해당 컬렉션 root만 변경, 다른 필드 유지 |
| SS-012 | 연쇄 with 호출 | `snap.withAllocTail().withRootPageId()` | 모든 변경 적용된 새 스냅샷 |

**테스트 코드 예시:**
```java
@Test
public void testWithAllocTail_SS010() {
    StoreSnapshot snap1 = new StoreSnapshot(1L, 1024L, ...);
    StoreSnapshot snap2 = snap1.withAllocTail(2048L);

    // 원본 불변
    assertEquals(1024L, snap1.getAllocTail());
    assertEquals(1L, snap1.getSeqNo());

    // 새 스냅샷
    assertEquals(2048L, snap2.getAllocTail());
    assertEquals(2L, snap2.getSeqNo());
}
```

---

## 2. ConcurrentPageCache 테스트

### 2.1 기본 동작 테스트

| ID | 시나리오 | 입력 | 기대 결과 |
|----|---------|------|----------|
| PC-001 | put 후 get | put(1L, data), get(1L) | 동일 데이터 반환 |
| PC-002 | 존재하지 않는 키 get | get(999L) | null 반환 |
| PC-003 | invalidate 후 get | invalidate(1L), get(1L) | null 반환 |
| PC-004 | clear 후 size | clear(), size() | 0 반환 |
| PC-005 | LRU 퇴거 | maxEntries 초과 put | 가장 오래된 항목 퇴거 |

### 2.2 동시성 테스트

| ID | 시나리오 | 스레드 구성 | 기대 결과 |
|----|---------|-----------|----------|
| PC-010 | 동시 읽기 | 100 readers | 모든 읽기 성공, 일관된 값 |
| PC-011 | 읽기 중 쓰기 | 10 readers + 1 writer | 읽기 실패 없음, 쓰기 완료 |
| PC-012 | 동시 쓰기 | 10 writers | 모든 쓰기 성공, 마지막 값 유지 |
| PC-013 | Optimistic Read 실패 | 읽기 중 쓰기 발생 | readLock으로 폴백, 정확한 값 반환 |

**테스트 코드 예시:**
```java
@Test
public void testConcurrentReads_PC010() throws Exception {
    ConcurrentPageCache cache = new ConcurrentPageCache(1024 * 1024, 4096);
    byte[] testData = new byte[4096];
    Arrays.fill(testData, (byte) 0x42);
    cache.put(1L, testData);

    int threadCount = 100;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                byte[] data = cache.get(1L);
                if (data != null && data[0] == 0x42) {
                    successCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    assertEquals(threadCount, successCount.get());
}
```

---

## 3. Wait-free 읽기 테스트

### 3.1 FxStore 읽기 메서드

| ID | 시나리오 | 메서드 | 기대 결과 |
|----|---------|--------|----------|
| WF-001 | exists() wait-free | `store.exists("col")` | 락 없이 즉시 반환 |
| WF-002 | list() wait-free | `store.list()` | 락 없이 컬렉션 목록 반환 |
| WF-003 | stats() wait-free | `store.stats()` | 락 없이 통계 반환 |
| WF-004 | commitMode() wait-free | `store.commitMode()` | 락 없이 모드 반환 |

### 3.2 컬렉션 읽기 메서드

| ID | 시나리오 | 메서드 | 기대 결과 |
|----|---------|--------|----------|
| WF-010 | map.get() wait-free | `map.get(key)` | 락 없이 값 반환 |
| WF-011 | map.containsKey() wait-free | `map.containsKey(key)` | 락 없이 결과 반환 |
| WF-012 | map.size() wait-free | `map.size()` | 락 없이 크기 반환 |
| WF-013 | set.contains() wait-free | `set.contains(elem)` | 락 없이 결과 반환 |
| WF-014 | list.get() wait-free | `list.get(index)` | 락 없이 값 반환 |
| WF-015 | deque.peekFirst() wait-free | `deque.peekFirst()` | 락 없이 값 반환 |

**검증 방법:**
```java
@Test
public void testWaitFreeRead_WF001() throws Exception {
    FxStore store = FxStore.openMemory(FxOptions.defaults());
    store.createMap("test", Long.class, String.class);

    // 쓰기 락을 계속 잡고 있는 스레드
    Thread blocker = new Thread(() -> {
        // 내부적으로 writeLock 획득하는 방법이 없으므로
        // 대신 쓰기 작업을 계속 수행
        NavigableMap<Long, String> map = store.createOrOpenMap("test", Long.class, String.class);
        for (int i = 0; i < 100000; i++) {
            map.put((long) i, "value");
        }
    });
    blocker.start();

    // 읽기가 블로킹되지 않음을 확인
    long start = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
        store.exists("test");  // wait-free이므로 빨라야 함
    }
    long elapsed = System.nanoTime() - start;

    // 1000번 호출이 100ms 이내 완료 (wait-free 증거)
    assertTrue(elapsed < 100_000_000);

    blocker.join();
    store.close();
}
```

---

## 4. Write Lock 쓰기 테스트

### 4.1 직렬화 검증

| ID | 시나리오 | 스레드 구성 | 기대 결과 |
|----|---------|-----------|----------|
| WL-001 | 동시 put 직렬화 | 10 threads, 각 1000 puts | 모든 put 성공, 최종 크기 = 10000 |
| WL-002 | 동시 createMap | 10 threads, 같은 이름 | 1개 성공, 9개 ALREADY_EXISTS |
| WL-003 | 동시 drop | 2 threads, 같은 컬렉션 | 1개 성공, 1개 NOT_FOUND |
| WL-004 | put과 remove 경쟁 | 동시에 같은 키 | 둘 중 하나 최종 상태 |

**테스트 코드 예시:**
```java
@Test
public void testWriteSerialization_WL001() throws Exception {
    FxStore store = FxStore.openMemory(FxOptions.defaults());
    NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

    int threadCount = 10;
    int opsPerThread = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(() -> {
            try {
                for (int i = 0; i < opsPerThread; i++) {
                    long key = threadId * opsPerThread + i;
                    map.put(key, "value-" + key);
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    assertEquals(threadCount * opsPerThread, map.size());

    store.close();
}
```

---

## 5. 동시성 스트레스 테스트

### 5.1 혼합 워크로드

| ID | 시나리오 | 구성 | 기간 | 기대 결과 |
|----|---------|------|------|----------|
| ST-001 | 읽기 헤비 | 16 readers, 2 writers | 1분 | 에러 0, 데이터 일관성 유지 |
| ST-002 | 쓰기 헤비 | 4 readers, 8 writers | 1분 | 에러 0, 모든 쓰기 반영 |
| ST-003 | 균형 | 8 readers, 8 writers | 1분 | 에러 0 |
| ST-004 | 다중 컬렉션 | 4 maps, 각각 4 threads | 1분 | 모든 컬렉션 정상 |

### 5.2 장시간 테스트

| ID | 시나리오 | 구성 | 기간 | 기대 결과 |
|----|---------|------|------|----------|
| ST-010 | 10분 지속 | 10 readers, 2 writers | 10분 | 메모리 누수 없음, 성능 저하 없음 |
| ST-011 | 100만 연산 | 각 스레드 100K ops | 완료까지 | 모든 연산 성공 |

**테스트 코드 예시:**
```java
@Test
public void testStress_ST001() throws Exception {
    FxStore store = FxStore.openMemory(FxOptions.defaults());
    NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

    // 초기 데이터
    for (int i = 0; i < 10000; i++) {
        map.put((long) i, "initial-" + i);
    }

    int readerCount = 16;
    int writerCount = 2;
    AtomicBoolean running = new AtomicBoolean(true);
    AtomicInteger errors = new AtomicInteger(0);
    AtomicLong readOps = new AtomicLong(0);
    AtomicLong writeOps = new AtomicLong(0);

    ExecutorService executor = Executors.newFixedThreadPool(readerCount + writerCount);

    // Readers
    for (int i = 0; i < readerCount; i++) {
        executor.submit(() -> {
            Random random = new Random();
            while (running.get()) {
                try {
                    map.get((long) random.nextInt(10000));
                    readOps.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }
        });
    }

    // Writers
    for (int i = 0; i < writerCount; i++) {
        final int writerId = i;
        executor.submit(() -> {
            long counter = 0;
            while (running.get()) {
                try {
                    long key = 10000 + writerId * 1000000 + counter++;
                    map.put(key, "value-" + key);
                    writeOps.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }
        });
    }

    // 1분간 실행
    Thread.sleep(60_000);
    running.set(false);
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    assertEquals(0, errors.get());
    System.out.printf("Reads: %d, Writes: %d%n", readOps.get(), writeOps.get());

    store.close();
}
```

---

## 6. Race Condition 테스트

### 6.1 가시성 테스트

| ID | 시나리오 | 검증 내용 | 기대 결과 |
|----|---------|----------|----------|
| RC-001 | 쓰기 후 읽기 가시성 | 쓰기 완료 후 다른 스레드에서 읽기 | 새 값이 보임 |
| RC-002 | 스냅샷 원자성 | 여러 필드 동시 관찰 | 일관된 스냅샷 상태 |
| RC-003 | 스냅샷 교체 원자성 | 교체 중간 상태 관찰 불가 | 이전 또는 이후 스냅샷만 관찰 |

### 6.2 데이터 무결성 테스트

| ID | 시나리오 | 검증 내용 | 기대 결과 |
|----|---------|----------|----------|
| RC-010 | 페이지 손상 없음 | 동시 읽기/쓰기 후 페이지 CRC 검증 | 모든 CRC 일치 |
| RC-011 | 컬렉션 크기 일관성 | size()와 실제 엔트리 수 | 항상 일치 |
| RC-012 | Iterator 일관성 | 순회 중 수정 | Iterator는 생성 시점 스냅샷 유지 |

**테스트 코드 예시:**
```java
@Test
public void testVisibility_RC001() throws Exception {
    FxStore store = FxStore.openMemory(FxOptions.defaults());
    NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

    AtomicBoolean writeComplete = new AtomicBoolean(false);
    AtomicBoolean readSuccess = new AtomicBoolean(false);

    Thread writer = new Thread(() -> {
        map.put(1L, "value");
        writeComplete.set(true);
    });

    Thread reader = new Thread(() -> {
        while (!writeComplete.get()) {
            Thread.yield();
        }
        // 쓰기 완료 후 즉시 읽기
        String value = map.get(1L);
        readSuccess.set("value".equals(value));
    });

    writer.start();
    reader.start();
    writer.join();
    reader.join();

    assertTrue("Write should be visible to reader", readSuccess.get());

    store.close();
}
```

---

## 7. BATCH 모드 동시성 테스트

### 7.1 BATCH 연산

| ID | 시나리오 | 검증 내용 | 기대 결과 |
|----|---------|----------|----------|
| BT-001 | 동시 commit 호출 | 2 threads, 같은 store | 1개 성공, 1개 무시 또는 대기 |
| BT-002 | 동시 rollback 호출 | 2 threads, 같은 store | 정상 처리 |
| BT-003 | commit 중 읽기 | commit 진행 중 다른 스레드 읽기 | 이전 커밋 스냅샷 반환 |
| BT-004 | rollback 후 읽기 | rollback 직후 읽기 | 마지막 커밋 상태 반환 |

### 7.2 BATCH 격리

| ID | 시나리오 | 검증 내용 | 기대 결과 |
|----|---------|----------|----------|
| BT-010 | 미커밋 변경 격리 | 쓰기 후 commit 전 다른 스레드 읽기 | 변경 보이지 않음 |
| BT-011 | commit 후 가시성 | commit 후 다른 스레드 읽기 | 변경 보임 |

**테스트 코드 예시:**
```java
@Test
public void testBatchIsolation_BT010() throws Exception {
    FxStore store = FxStore.openMemory(FxOptions.defaults().withCommitMode(CommitMode.BATCH));
    NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

    // 쓰기 (아직 commit 안 함)
    map.put(1L, "uncommitted");

    AtomicReference<String> readValue = new AtomicReference<>();

    Thread reader = new Thread(() -> {
        readValue.set(map.get(1L));
    });

    reader.start();
    reader.join();

    // BATCH 모드에서 미커밋 변경은 다른 관점에서 어떻게 보일지?
    // 설계에 따라 다름:
    // 1) 같은 스레드에서만 보임
    // 2) 모든 스레드에서 보임 (commit만 원자적)

    // 현재 설계: Single Writer이므로 읽기 스레드도 pending 스냅샷 볼 수 있음
    // 그러나 commit/rollback만 원자적

    store.close();
}
```

---

## 8. Iterator 스냅샷 격리 테스트

### 8.1 스냅샷 격리

| ID | 시나리오 | 검증 내용 | 기대 결과 |
|----|---------|----------|----------|
| IT-001 | 순회 중 삽입 | Iterator 순회 중 새 항목 삽입 | 새 항목 보이지 않음 |
| IT-002 | 순회 중 삭제 | Iterator 순회 중 항목 삭제 | 삭제된 항목 여전히 보임 |
| IT-003 | 순회 중 수정 | Iterator 순회 중 값 변경 | 이전 값 보임 |

### 8.2 동시 순회

| ID | 시나리오 | 검증 내용 | 기대 결과 |
|----|---------|----------|----------|
| IT-010 | 다중 Iterator 동시 순회 | 10개 Iterator 동시 생성 및 순회 | 모든 순회 성공 |
| IT-011 | 순회 중 다른 Iterator 생성 | 순회 중 새 Iterator 생성 | 새 Iterator는 최신 스냅샷 |

**테스트 코드 예시:**
```java
@Test
public void testIteratorIsolation_IT001() {
    FxStore store = FxStore.openMemory(FxOptions.defaults());
    NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

    // 초기 데이터
    for (long i = 0; i < 100; i++) {
        map.put(i, "value-" + i);
    }

    // Iterator 생성 (스냅샷 고정)
    Iterator<Map.Entry<Long, String>> iter = map.entrySet().iterator();

    // 순회 중 새 항목 삽입
    map.put(200L, "new-value");

    // Iterator는 생성 시점 스냅샷만 봄
    int count = 0;
    while (iter.hasNext()) {
        iter.next();
        count++;
    }

    assertEquals(100, count);  // 새 항목 안 보임
    assertEquals(101, map.size());  // 실제로는 추가됨

    store.close();
}
```

---

## 9. 성능 벤치마크

### 9.1 단일 스레드 성능

| ID | 메트릭 | 목표 | 측정 방법 |
|----|--------|------|----------|
| BM-001 | Map.put ops/sec | >= 50,000 | 100K puts, 시간 측정 |
| BM-002 | Map.get ops/sec | >= 100,000 | 100K gets, 시간 측정 |
| BM-003 | List.add ops/sec | >= 30,000 | 100K adds, 시간 측정 |
| BM-004 | Deque.addFirst ops/sec | >= 40,000 | 100K adds, 시간 측정 |

### 9.2 동시 읽기 확장성

| ID | 스레드 수 | 기대 처리량 | 비고 |
|----|----------|------------|------|
| BM-010 | 1 | 기준 (100%) | 단일 스레드 읽기 |
| BM-011 | 2 | >= 190% | 거의 2배 |
| BM-012 | 4 | >= 370% | 거의 4배 |
| BM-013 | 8 | >= 700% | 거의 8배 |
| BM-014 | 16 | >= 1200% | 선형에 근접 |

### 9.3 혼합 워크로드

| ID | 읽기:쓰기 비율 | 목표 처리량 | 스레드 구성 |
|----|--------------|------------|-----------|
| BM-020 | 90:10 | >= 90K ops/sec | 9R + 1W |
| BM-021 | 80:20 | >= 80K ops/sec | 8R + 2W |
| BM-022 | 50:50 | >= 50K ops/sec | 5R + 5W |

### 9.4 오버헤드 측정

| ID | 메트릭 | 허용 오버헤드 | 측정 방법 |
|----|--------|-------------|----------|
| BM-030 | 읽기 락 오버헤드 | < 5% | 락 없는 버전 대비 |
| BM-031 | 쓰기 락 오버헤드 | < 15% | 락 없는 버전 대비 |
| BM-032 | 스냅샷 생성 비용 | < 1μs | 스냅샷 생성 시간 |

**벤치마크 코드 예시:**
```java
@Test
public void benchmarkConcurrentReadScaling() throws Exception {
    FxStore store = FxStore.openMemory(FxOptions.defaults());
    NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

    // 데이터 준비
    for (int i = 0; i < 10000; i++) {
        map.put((long) i, "value-" + i);
    }

    int[] threadCounts = {1, 2, 4, 8, 16};
    long[] throughputs = new long[threadCounts.length];

    for (int t = 0; t < threadCounts.length; t++) {
        int threadCount = threadCounts[t];
        int opsPerThread = 100000 / threadCount;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicLong totalOps = new AtomicLong(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random();
                    for (int j = 0; j < opsPerThread; j++) {
                        map.get((long) random.nextInt(10000));
                        totalOps.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startLatch.countDown();
        doneLatch.await();
        long elapsed = System.nanoTime() - start;

        throughputs[t] = (long) (totalOps.get() / (elapsed / 1_000_000_000.0));
        executor.shutdown();
    }

    // 확장성 검증
    long baseline = throughputs[0];
    for (int t = 1; t < threadCounts.length; t++) {
        double scalingFactor = (double) throughputs[t] / baseline;
        double expectedMin = threadCounts[t] * 0.7;  // 70% 효율

        System.out.printf("%d threads: %d ops/sec (%.1fx scaling)%n",
            threadCounts[t], throughputs[t], scalingFactor);

        assertTrue(
            String.format("Scaling factor %.1f below expected %.1f", scalingFactor, expectedMin),
            scalingFactor >= expectedMin
        );
    }

    store.close();
}
```

---

## 10. 불변식 검증

### 10.1 동시성 불변식 (INV-C1 ~ INV-C5)

| ID | 불변식 | 검증 방법 | 기대 결과 |
|----|--------|----------|----------|
| INV-C1 | Single Writer | 동시 쓰기 시도 | 직렬화됨, 동시에 2개 이상 쓰기 불가 |
| INV-C2 | Snapshot Immutability | 스냅샷 수정 시도 | UnsupportedOperationException |
| INV-C3 | Wait-free Read | 읽기 중 쓰기 블로킹 | 읽기는 블로킹되지 않음 |
| INV-C4 | Atomic Snapshot Switch | 중간 상태 관찰 시도 | 항상 완전한 스냅샷만 관찰 |
| INV-C5 | No Deadlock | 교차 접근 패턴 | 30초 내 완료 |

**검증 코드 예시:**
```java
@Test
public void verifyINV_C1_SingleWriter() throws Exception {
    FxStore store = FxStore.openMemory(FxOptions.defaults());
    NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);

    AtomicInteger concurrentWrites = new AtomicInteger(0);
    AtomicInteger maxConcurrent = new AtomicInteger(0);

    // 쓰기 시작/종료 시 카운터 증감하는 래퍼 (실제 구현에서는 AOP나 계측 필요)
    // 여기서는 개념적 테스트

    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
        executor.submit(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    // 실제 쓰기
                    map.put(Thread.currentThread().getId() * 1000 + i, "value");
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();

    // maxConcurrent가 1을 초과하면 Single Writer 위반
    // (실제 검증은 내부 계측 필요)

    store.close();
}

@Test
public void verifyINV_C5_NoDeadlock() throws Exception {
    FxStore store = FxStore.openMemory(FxOptions.defaults());
    NavigableMap<Long, String> map1 = store.createMap("map1", Long.class, String.class);
    NavigableMap<Long, String> map2 = store.createMap("map2", Long.class, String.class);

    AtomicBoolean completed = new AtomicBoolean(false);

    Thread t1 = new Thread(() -> {
        for (int i = 0; i < 1000; i++) {
            map1.put((long) i, "v1");
            map2.put((long) i, "v2");
        }
    });

    Thread t2 = new Thread(() -> {
        for (int i = 0; i < 1000; i++) {
            map2.put((long) i, "v2");
            map1.put((long) i, "v1");
        }
    });

    t1.start();
    t2.start();

    // 30초 내 완료되어야 함
    t1.join(30000);
    t2.join(30000);

    assertFalse("Deadlock detected: t1 still alive", t1.isAlive());
    assertFalse("Deadlock detected: t2 still alive", t2.isAlive());

    store.close();
}
```

---

## 테스트 커버리지 목표

### 라인 커버리지 목표

| 패키지 | 목표 | 근거 |
|--------|------|------|
| `com.fxstore.core` (동시성 관련) | **95%** | 신규 코드, 완벽 검증 필요 |
| `com.fxstore.core.StoreSnapshot` | **100%** | 불변 클래스, 모든 경로 검증 |
| `com.fxstore.core.ConcurrentPageCache` | **95%** | Optimistic Read 폴백 포함 |
| `com.fxstore.collection.*` (수정) | **95%** | 기존 코드 + 동시성 래퍼 |

### 브랜치 커버리지 목표

| 카테고리 | 목표 | 핵심 브랜치 |
|----------|------|------------|
| Lock 분기 | **100%** | tryOptimisticRead 성공/실패 |
| 스냅샷 분기 | **100%** | null 체크, 빈 Map 처리 |
| 예외 분기 | **90%** | IllegalStateException 등 |

### 뮤테이션 테스트 목표

| 메트릭 | 목표 |
|--------|------|
| 전체 뮤테이션 점수 | **85%** |
| 경계값 뮤테이션 | **90%** |
| null 반환 뮤테이션 | **100%** |

---

## 테스트 실행 체크리스트

### Week 3 Day 1 (단위 테스트)
- [ ] StoreSnapshotTest 모두 통과
- [ ] ConcurrentPageCacheTest 모두 통과

### Week 3 Day 2 (스트레스 테스트)
- [ ] ConcurrencyStressTest 모두 통과
- [ ] 1분 스트레스 테스트 에러 0

### Week 3 Day 3 (Race Condition)
- [ ] RaceConditionTest 모두 통과
- [ ] 가시성 테스트 통과
- [ ] 원자성 테스트 통과

### Week 3 Day 4 (벤치마크)
- [ ] 단일 스레드 성능 목표 달성
- [ ] 동시 읽기 선형 확장성 확인
- [ ] 혼합 워크로드 목표 달성

### Week 3 Day 5-6 (통합)
- [ ] 모든 Phase 0-7 회귀 테스트 통과
- [ ] 테스트 커버리지 **95% 이상**

---

*문서 작성일: 2025-12-27*
