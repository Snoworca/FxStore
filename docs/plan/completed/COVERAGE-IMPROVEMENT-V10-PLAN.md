# v1.0 테스트 커버리지 향상 및 기능 개선 계획

> **문서 버전:** 1.1
> **대상 버전:** v1.0
> **작성일:** 2025-12-30
> **상태:** 계획 수립 완료 (Iteration 2 개선)

[← 목차로 돌아가기](00.index.md)

---

## 1. 개요

### 1.1 현재 상황

v0.9 안정화 평가에서 테스트 커버리지 데이터가 **잘못 기록**되었음이 확인되었습니다.

| 메트릭 | 기존 문서 | 실제 값 | 차이 |
|--------|---------|--------|------|
| 명령어 커버리지 | 81% | **29%** | -52% |
| 브랜치 커버리지 | 72% | **16%** | -56% |
| 0% 커버리지 클래스 | 0개 | **26개** | - |

### 1.2 목표

| 목표 | 현재 | 목표 | 증가량 |
|------|------|------|--------|
| 명령어 커버리지 | 29% | **60%+** | +31% |
| 브랜치 커버리지 | 16% | **40%+** | +24% |
| 0% 커버리지 클래스 | 26개 | **0개** | -26개 |

### 1.3 범위

1. **기능 개선**: FxList Iterator 쓰기 연산 구현
2. **핵심 테스트**: 0% 커버리지 클래스 테스트 작성
3. **패키지별 개선**: 저 커버리지 패키지 집중 테스트

---

## 2. 기능 개선: FxList Iterator 쓰기 연산

### 2.1 현재 상태

**파일**: `src/main/java/com/snoworca/fxstore/collection/FxList.java`
**위치**: 행 468-483

```java
// 현재 구현 (읽기 전용)
@Override
public void remove() {
    throw new UnsupportedOperationException("Snapshot iterator is read-only");
}

@Override
public void set(E e) {
    throw new UnsupportedOperationException("Snapshot iterator is read-only");
}

@Override
public void add(E e) {
    throw new UnsupportedOperationException("Snapshot iterator is read-only");
}
```

### 2.2 설계 결정

#### 옵션 A: Snapshot Iterator 유지 (읽기 전용) ✅ 권장
- **장점**: 동시성 안전, Wait-free Read 보장
- **단점**: 쓰기 연산 미지원
- **결정**: Snapshot 기반 설계의 핵심 원칙 유지

#### 옵션 B: Mutable Iterator 추가 (쓰기 지원)
- **장점**: Java List 완전 호환
- **단점**: 동시성 복잡도 증가, ConcurrentModificationException 처리 필요
- **구현 난이도**: 높음

### 2.3 구현 계획 (옵션 A 채택)

**이유**: FxStore의 핵심 설계 원칙인 **Snapshot Isolation**과 **Wait-free Read**를 유지하기 위해 읽기 전용 Iterator를 유지합니다.

**개선 사항**:
1. Javadoc에 읽기 전용임을 명확히 문서화
2. `UnsupportedOperationException` 메시지 개선
3. 대안 API 제공 (`FxList.set(index, element)` 직접 호출 안내)

#### 2.3.1 코드 변경

```java
// 개선된 구현
/**
 * This iterator is read-only because FxList uses snapshot isolation.
 * Use {@link FxList#set(int, Object)} for modifications.
 *
 * @throws UnsupportedOperationException always
 */
@Override
public void remove() {
    throw new UnsupportedOperationException(
        "Snapshot iterator is read-only. Use FxList.remove(index) instead.");
}

/**
 * This iterator is read-only because FxList uses snapshot isolation.
 * Use {@link FxList#set(int, Object)} for modifications.
 *
 * @throws UnsupportedOperationException always
 */
@Override
public void set(E e) {
    throw new UnsupportedOperationException(
        "Snapshot iterator is read-only. Use FxList.set(index, element) instead.");
}

/**
 * This iterator is read-only because FxList uses snapshot isolation.
 * Use {@link FxList#add(int, Object)} for modifications.
 *
 * @throws UnsupportedOperationException always
 */
@Override
public void add(E e) {
    throw new UnsupportedOperationException(
        "Snapshot iterator is read-only. Use FxList.add(index, element) instead.");
}
```

#### 2.3.2 테스트 추가

```java
// FxListIteratorTest.java
@Test(expected = UnsupportedOperationException.class)
public void testIteratorRemoveThrowsUOE() {
    FxList<String> list = createTestList();
    ListIterator<String> it = list.listIterator();
    it.next();
    it.remove();  // Should throw UOE with helpful message
}

@Test
public void testIteratorRemoveExceptionMessage() {
    FxList<String> list = createTestList();
    ListIterator<String> it = list.listIterator();
    it.next();
    try {
        it.remove();
        fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
        assertTrue(e.getMessage().contains("FxList.remove"));
    }
}
```

---

## 3. 테스트 커버리지 향상 계획

### 3.1 우선순위별 분류

#### P0 (긴급) - 0% 커버리지 클래스

| # | 패키지 | 클래스 | 미실행 명령어 | 예상 시간 |
|---|--------|--------|-------------|----------|
| 1 | migration | DequeMigrator | 117 | 2시간 |
| 2 | storage | FileStorage | 494 | 4시간 |
| 3 | collection | FxDequeImpl | 1,478 | 8시간 |
| 4 | collection | FxNavigableMapImpl Views (8개) | 3,000+ | 12시간 |
| 5 | collection | FxNavigableSetImpl Views (6개) | 2,000+ | 8시간 |
| 6 | api | VerifyError, VerifyResult, etc. | 200+ | 2시간 |

**소계**: 36시간 (약 4.5일)

#### P1 (높음) - 10-30% 커버리지 클래스

| # | 패키지 | 클래스 | 현재 커버리지 | 목표 | 예상 시간 |
|---|--------|--------|-------------|------|----------|
| 1 | codec | ByteCodec, ShortCodec, etc. | 9-13% | 70%+ | 4시간 |
| 2 | core | FxReadTransactionImpl | 14% | 60%+ | 4시간 |
| 3 | core | PageCache | 13% | 50%+ | 2시간 |
| 4 | util | ByteUtils | 17% | 80%+ | 2시간 |

**소계**: 12시간 (약 1.5일)

#### P2 (중간) - 30-50% 커버리지 클래스

| # | 패키지 | 클래스 | 현재 커버리지 | 목표 | 예상 시간 |
|---|--------|--------|-------------|------|----------|
| 1 | core | FxStoreImpl | 42% | 60%+ | 6시간 |
| 2 | btree | BTree | 36% | 60%+ | 4시간 |
| 3 | storage | Allocator | 41% | 60%+ | 3시간 |
| 4 | catalog | CatalogEntry, CollectionState | 34-45% | 60%+ | 3시간 |

**소계**: 16시간 (약 2일)

---

### 3.2 Phase별 구현 계획

#### Phase 1: P0 핵심 클래스 (Week 1)

##### Day 1-2: DequeMigrator + FileStorage
```
작업:
1. DequeMigrator 테스트 (마이그레이션 시나리오)
   - LEGACY → ORDERED 변환 테스트
   - 데이터 무결성 검증
   - 롤백 시나리오

2. FileStorage 테스트
   - 파일 생성/열기/닫기
   - 읽기/쓰기 연산
   - 리소스 정리 (close)
   - 예외 처리 (IOException)
```

##### Day 3-4: FxDequeImpl
```
작업:
1. 기본 연산 테스트
   - addFirst/addLast
   - removeFirst/removeLast
   - peekFirst/peekLast
   - size, isEmpty

2. Iterator 테스트
   - 순방향/역방향 순회
   - 빈 Deque 처리

3. 경계 조건 테스트
   - null 요소 처리
   - 대량 데이터 (10,000+ 요소)
```

##### Day 5: View 클래스 테스트 시작
```
작업:
1. FxNavigableMapImpl Views
   - SubMapView: subMap(fromKey, toKey)
   - HeadMapView: headMap(toKey)
   - TailMapView: tailMap(fromKey)
   - KeySetView: keySet()
   - DescendingMapView: descendingMap()

테스트 패턴:
- 범위 검증
- 수정 연산 전파
- Iterator 동작
```

#### Phase 2: P0 View 클래스 완료 (Week 2 Day 1-3)

##### NavigableSet Views
```
작업:
1. FxNavigableSetImpl Views
   - SubSetView: subSet(fromElement, toElement)
   - HeadSetView: headSet(toElement)
   - TailSetView: tailSet(fromElement)
   - DescendingSetView: descendingSet()
   - DescendingHeadSetView, DescendingTailSetView, DescendingSubSetView

테스트 패턴:
- TreeSet과 동등성 테스트
- 범위 경계 테스트
- Descending 순서 검증
```

##### API 검증 클래스
```
작업:
1. VerifyError, VerifyErrorKind, VerifyResult
   - 오류 생성 테스트
   - 오류 종류별 테스트
   - 결과 집계 테스트
```

#### Phase 3: P1 저 커버리지 (Week 2 Day 4-5)

##### Codec 테스트
```java
// 모든 코덱 테스트 패턴
@Test
public void testByteCodecRoundTrip() {
    ByteCodec codec = new ByteCodec();
    for (byte b = Byte.MIN_VALUE; b < Byte.MAX_VALUE; b++) {
        byte[] encoded = codec.encode(b);
        Byte decoded = codec.decode(encoded);
        assertEquals(b, decoded.byteValue());
    }
}

// Short, Float, Double, String 등 동일 패턴 적용
```

##### Transaction 테스트
```java
// FxReadTransactionImpl 테스트
@Test
public void testReadTransactionIsolation() {
    FxStore store = createStore();
    FxNavigableMap<String, String> map = store.createMap("test", String.class, String.class);
    map.put("key1", "value1");
    store.commit();

    // 트랜잭션 시작
    FxReadTransaction tx = store.beginReadTransaction();
    FxNavigableMap<String, String> txMap = tx.openMap("test", String.class, String.class);

    // 외부 수정
    map.put("key1", "modified");
    store.commit();

    // 트랜잭션 내에서는 이전 값 유지
    assertEquals("value1", txMap.get("key1"));

    tx.close();
}
```

#### Phase 4: P2 중간 커버리지 (Week 3)

##### FxStoreImpl 심층 테스트
```
테스트 영역:
1. 컬렉션 생성 (Map, Set, List, Deque)
2. 컬렉션 열기/재열기
3. 트랜잭션 (commit, rollback)
4. 스냅샷 격리
5. 리소스 정리
6. 예외 상황 (중복 이름, 타입 불일치)
```

##### BTree 심층 테스트
```
테스트 영역:
1. 삽입 시나리오
   - 리프 분할
   - 내부 노드 분할
   - 루트 분할

2. 삭제 시나리오
   - 리프 병합
   - 키 재분배
   - 언더플로우 처리

3. 범위 검색
   - 순방향/역방향
   - 경계 조건
```

---

### 3.3 테스트 시나리오 상세

#### 3.3.1 DequeMigrator 테스트

```java
public class DequeMigratorTest {

    @Test
    public void testMigrateLegacyToOrdered() {
        // Given: LEGACY 인코딩으로 저장된 Deque
        FxStore store = createStoreWithLegacyDeque();
        FxDeque<String> deque = store.openDeque("test", String.class);

        // When: 마이그레이션 실행
        DequeMigrator.migrate(store, "test");

        // Then: ORDERED 인코딩으로 변환되고 데이터 유지
        FxDeque<String> migratedDeque = store.openDeque("test", String.class);
        assertEquals("first", migratedDeque.peekFirst());
        assertEquals("last", migratedDeque.peekLast());
    }

    @Test
    public void testMigrationPreservesOrder() {
        // 10,000개 요소의 순서 보존 검증
    }

    @Test
    public void testMigrationRollbackOnFailure() {
        // 실패 시 원본 데이터 유지 검증
    }
}
```

#### 3.3.2 FileStorage 테스트

```java
public class FileStorageTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testCreateNewFile() throws Exception {
        File file = tempFolder.newFile("test.fx");
        try (FileStorage storage = new FileStorage(file, FileLockMode.EXCLUSIVE)) {
            storage.write(0, new byte[4096], 0, 4096);
            byte[] read = new byte[4096];
            storage.read(0, read, 0, 4096);
            // 검증
        }
    }

    @Test
    public void testFileLocking() {
        // 배타적 잠금 테스트
    }

    @Test
    public void testResourceCleanup() {
        // close 후 파일 핸들 해제 검증
    }

    @Test(expected = FxException.class)
    public void testReadFromClosedStorage() {
        // 닫힌 스토리지 읽기 시 예외
    }
}
```

#### 3.3.3 NavigableMap View 테스트

```java
public class FxNavigableMapViewTest {

    private FxNavigableMap<Integer, String> map;

    @Before
    public void setUp() {
        // 1, 2, 3, ..., 100 삽입
        map = createMapWith100Elements();
    }

    // SubMapView 테스트
    @Test
    public void testSubMapRange() {
        NavigableMap<Integer, String> sub = map.subMap(25, true, 75, true);
        assertEquals(51, sub.size());
        assertEquals(Integer.valueOf(25), sub.firstKey());
        assertEquals(Integer.valueOf(75), sub.lastKey());
    }

    @Test
    public void testSubMapPutWithinRange() {
        NavigableMap<Integer, String> sub = map.subMap(25, true, 75, true);
        sub.put(50, "modified");
        assertEquals("modified", map.get(50));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubMapPutOutOfRange() {
        NavigableMap<Integer, String> sub = map.subMap(25, true, 75, true);
        sub.put(100, "out");  // 범위 초과
    }

    // HeadMapView 테스트
    @Test
    public void testHeadMapRange() {
        NavigableMap<Integer, String> head = map.headMap(50, false);
        assertEquals(49, head.size());
        assertEquals(Integer.valueOf(1), head.firstKey());
        assertEquals(Integer.valueOf(49), head.lastKey());
    }

    // TailMapView 테스트
    @Test
    public void testTailMapRange() {
        NavigableMap<Integer, String> tail = map.tailMap(50, true);
        assertEquals(51, tail.size());
        assertEquals(Integer.valueOf(50), tail.firstKey());
        assertEquals(Integer.valueOf(100), tail.lastKey());
    }

    // DescendingMapView 테스트
    @Test
    public void testDescendingMapOrder() {
        NavigableMap<Integer, String> desc = map.descendingMap();
        assertEquals(Integer.valueOf(100), desc.firstKey());
        assertEquals(Integer.valueOf(1), desc.lastKey());
    }

    // KeySetView 테스트
    @Test
    public void testKeySetIteration() {
        NavigableSet<Integer> keys = map.navigableKeySet();
        Iterator<Integer> it = keys.iterator();
        int prev = 0;
        while (it.hasNext()) {
            int curr = it.next();
            assertTrue(curr > prev);
            prev = curr;
        }
    }

    // TreeMap 동등성 테스트
    @Test
    public void testEquivalenceWithTreeMap() {
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        for (int i = 1; i <= 100; i++) {
            treeMap.put(i, "value" + i);
        }

        // subMap 동등성
        assertEquals(
            treeMap.subMap(25, true, 75, true).size(),
            map.subMap(25, true, 75, true).size()
        );

        // headMap 동등성
        assertEquals(
            treeMap.headMap(50, false).size(),
            map.headMap(50, false).size()
        );

        // tailMap 동등성
        assertEquals(
            treeMap.tailMap(50, true).size(),
            map.tailMap(50, true).size()
        );
    }
}
```

---

## 4. 코드 품질 개선

### 4.1 FxStoreImpl 리팩토링

#### 4.1.1 현재 문제

```java
// 중복된 패턴 (4종 × 3 = 12개 메서드)
public FxNavigableMap<K, V> createMap(...) { ... }
public FxNavigableMap<K, V> openMap(...) { ... }
public FxNavigableMap<K, V> createOrOpenMap(...) { ... }

public FxNavigableSet<E> createSet(...) { ... }
public FxNavigableSet<E> openSet(...) { ... }
public FxNavigableSet<E> createOrOpenSet(...) { ... }

// List, Deque도 동일 패턴
```

#### 4.1.2 개선 방안

```java
// Template Method 패턴 도입
private <T> T createOrOpenCollection(
    String name,
    CollectionKind kind,
    CreateMode mode,
    Class<?>[] typeParams,
    CollectionFactory<T> factory
) {
    acquireWriteLock();
    try {
        CatalogEntry existing = catalog.get(name);

        switch (mode) {
            case CREATE_ONLY:
                if (existing != null) {
                    throw new FxException("Collection exists: " + name,
                        FxErrorCode.ALREADY_EXISTS);
                }
                return createNewCollection(name, kind, typeParams, factory);

            case OPEN_ONLY:
                if (existing == null) {
                    throw new FxException("Collection not found: " + name,
                        FxErrorCode.NOT_FOUND);
                }
                return openExistingCollection(name, existing, factory);

            case CREATE_OR_OPEN:
                if (existing != null) {
                    return openExistingCollection(name, existing, factory);
                }
                return createNewCollection(name, kind, typeParams, factory);
        }
    } finally {
        releaseWriteLock();
    }
}

// 사용 예
public <K, V> FxNavigableMap<K, V> createMap(String name, Class<K> keyClass, Class<V> valueClass) {
    return createOrOpenCollection(
        name,
        CollectionKind.MAP,
        CreateMode.CREATE_ONLY,
        new Class<?>[] { keyClass, valueClass },
        (btree, codec) -> new FxNavigableMapImpl<>(btree, codec, this)
    );
}
```

### 4.2 예외 처리 강화

#### 4.2.1 FileStorage

```java
// 현재 (문제)
try { channel.close(); } catch (IOException ignored) {}

// 개선
@Override
public void close() throws FxException {
    try {
        if (channel != null && channel.isOpen()) {
            channel.force(true);  // 버퍼 플러시
            channel.close();
        }
    } catch (IOException e) {
        throw new FxException("Failed to close file storage", e, FxErrorCode.IO);
    }
}
```

---

## 5. 영향도 분석

### 5.1 변경 영향 범위

| 변경 항목 | 영향받는 파일 | 위험도 | 회귀 테스트 범위 |
|----------|-------------|--------|----------------|
| FxList Iterator 문서화 | FxList.java | 낮음 | FxListTest |
| DequeMigrator 테스트 | - (테스트 추가) | 없음 | DequeMigratorTest |
| FileStorage 테스트 | - (테스트 추가) | 없음 | FileStorageTest |
| View 클래스 테스트 | - (테스트 추가) | 없음 | View 관련 테스트 |
| FxStoreImpl 리팩토링 | FxStoreImpl.java | 높음 | 전체 회귀 테스트 |

### 5.2 의존성 그래프

```
FxStoreImpl (핵심)
├── Allocator
│   └── Storage (FileStorage/MemoryStorage)
├── BTree
│   ├── BTreeInternal
│   └── BTreeLeaf
├── OST
│   ├── OSTInternal
│   └── OSTLeaf
├── Catalog
│   └── CatalogEntry
└── Collections
    ├── FxNavigableMapImpl
    │   └── Views (8개)
    ├── FxNavigableSetImpl
    │   └── Views (6개)
    ├── FxList
    └── FxDequeImpl
```

---

## 6. SOLID 원칙 준수

### 6.1 Single Responsibility Principle (SRP)

| 클래스 | 책임 | 준수 여부 |
|--------|------|----------|
| FxList | 리스트 연산 | ✅ |
| SnapshotListIterator | 읽기 전용 순회 | ✅ |
| DequeMigrator | Deque 마이그레이션 | ✅ |
| FileStorage | 파일 I/O | ✅ |
| FxStoreImpl | 스토어 관리 (개선 필요) | 🟡 |

### 6.2 Open/Closed Principle (OCP)

- **FxCodec**: 새 코덱 추가 시 기존 코드 수정 불필요 ✅
- **Storage**: FileStorage/MemoryStorage 교체 가능 ✅
- **Views**: 새 View 타입 추가 용이 ✅

### 6.3 Liskov Substitution Principle (LSP)

- **FxNavigableMap ← TreeMap 대체**: 동일 인터페이스, 동일 동작 ✅
- **FxNavigableSet ← TreeSet 대체**: 동일 인터페이스, 동일 동작 ✅
- **FxList ← ArrayList 대체**: Iterator 쓰기 제한 (문서화로 해결) 🟡

### 6.4 Interface Segregation Principle (ISP)

- **FxStore**: 필요한 메서드만 노출 ✅
- **Storage 인터페이스**: read/write/close만 정의 ✅
- **Iterator**: 표준 인터페이스 준수 ✅

### 6.5 Dependency Inversion Principle (DIP)

- **Storage 추상화**: FxStoreImpl → Storage (인터페이스) ✅
- **Codec 추상화**: Collections → FxCodec (인터페이스) ✅
- **PageCache 주입**: 생성자 주입 ✅

---

## 7. 테스트 전략

### 7.1 테스트 레벨

| 레벨 | 대상 | 목표 커버리지 |
|------|------|-------------|
| Unit | 개별 클래스 | 80%+ |
| Integration | 컴포넌트 조합 | 70%+ |
| Equivalence | TreeMap/Set 비교 | 100% 동일 동작 |
| Stress | 대량 데이터 | 성능 저하 없음 |

### 7.2 테스트 명명 규칙

```
test{MethodName}_{Scenario}_{ExpectedResult}

예:
testPut_WithNullKey_ThrowsNPE
testSubMap_WithinRange_ReturnsSubset
testIterator_AfterModification_ThrowsCME
```

### 7.3 테스트 구조

```java
@Test
public void testMethodName_Scenario_Expected() {
    // Given: 사전 조건
    FxNavigableMap<K, V> map = createTestMap();

    // When: 테스트 동작
    V result = map.put(key, value);

    // Then: 검증
    assertEquals(expected, result);
}
```

---

## 8. 일정

### 8.1 주간 계획

| 주차 | 작업 | 목표 커버리지 |
|------|------|-------------|
| Week 1 | P0 클래스 테스트 (DequeMigrator, FileStorage, FxDequeImpl) | 40% |
| Week 2 | P0 View 클래스 + P1 코덱/트랜잭션 | 50% |
| Week 3 | P2 핵심 클래스 (FxStoreImpl, BTree) | 60% |

### 8.2 마일스톤

| 마일스톤 | 날짜 | 조건 |
|----------|------|------|
| M1 | Week 1 완료 | 0% 클래스 → 50%+ |
| M2 | Week 2 완료 | 전체 커버리지 50%+ |
| M3 | Week 3 완료 | 전체 커버리지 60%+ |

---

## 9. 품질 게이트

### 9.1 Phase 완료 조건

1. **모든 테스트 통과** (5분 이내)
2. **커버리지 목표 달성** (패키지별)
3. **Javadoc 완성** (public API)
4. **회귀 테스트 통과**

### 9.2 평가 기준

| # | 기준 | 만점 | 목표 |
|---|------|------|------|
| 1 | Plan-Code 정합성 | 100 | A+ (95+) |
| 2 | SOLID 원칙 준수 | 100 | A+ (95+) |
| 3 | 테스트 커버리지 | 100 | A+ (95+) |
| 4 | 코드 가독성 | 100 | A+ (95+) |
| 5 | 예외 처리 및 안정성 | 100 | A+ (95+) |
| 6 | 성능 효율성 | 100 | A+ (95+) |
| 7 | 문서화 품질 | 100 | A+ (95+) |

---

## 10. 위험 관리 (강화됨 v1.1)

### 10.1 정량적 위험 매트릭스

| ID | 위험 | 가능성 | 영향 | 위험 점수 | 담당자 | 상태 |
|----|------|--------|------|----------|--------|------|
| R1 | View 클래스 테스트 복잡 | 4/5 | 3/5 | **12** (높음) | 테스트 담당 | 모니터링 |
| R2 | FxStoreImpl 리팩토링 회귀 | 3/5 | 5/5 | **15** (매우 높음) | 리드 개발자 | 주의 |
| R3 | 테스트 시간 초과 | 2/5 | 3/5 | **6** (중간) | CI/CD 담당 | 모니터링 |
| R4 | 코덱 호환성 깨짐 | 2/5 | 4/5 | **8** (중간) | 코덱 담당 | 대비 |
| R5 | 마이그레이션 데이터 손실 | 1/5 | 5/5 | **5** (낮음) | DBA | 대비 |

**점수 계산**: 가능성 × 영향 (1-25점)
- **15-25점**: 매우 높음 (즉시 대응)
- **10-14점**: 높음 (우선 대응)
- **5-9점**: 중간 (계획 대응)
- **1-4점**: 낮음 (모니터링)

### 10.2 위험별 상세 대응

#### R1: View 클래스 테스트 복잡
```
원인: 14개 View 클래스 (8 Map + 6 Set), 각각 수십 개 메서드
대응:
1. TreeMap/TreeSet 동등성 테스트 기반 자동 생성
2. 공통 테스트 추상 클래스 도입 (AbstractViewTest)
3. 파라미터화 테스트 활용 (JUnit Parameterized)

롤백 계획: View 테스트 실패 시 기존 동작 유지, 개별 수정
에스컬레이션: 3일 지연 시 리드 개발자에게 보고
```

#### R2: FxStoreImpl 리팩토링 회귀
```
원인: 2,351줄 핵심 클래스, 12개 컬렉션 메서드 중복
대응:
1. 리팩토링 전 전체 테스트 스냅샷 저장
2. 점진적 리팩토링 (1개 메서드씩)
3. 각 단계마다 전체 회귀 테스트
4. 코드 리뷰 필수 (2인 이상)

롤백 계획:
- Git revert 즉시 가능하도록 커밋 분리
- 리팩토링 브랜치 별도 관리
- 실패 시 24시간 내 롤백

에스컬레이션: 1일 내 회귀 발견 시 PM에게 보고
```

#### R3: 테스트 시간 초과
```
원인: 대량 데이터 테스트, 무한 루프 버그
대응:
1. 5분 제한 강제 (CI/CD 타임아웃)
2. 테스트별 개별 타임아웃 설정
3. 일일 테스트 시간 추적

롤백 계획: 초과 테스트 @Ignore 처리 후 분석
```

#### R4: 코덱 호환성 깨짐
```
원인: 코덱 테스트 추가 시 기존 데이터 읽기 실패 가능
대응:
1. 기존 데이터 파일로 호환성 테스트
2. 버전별 테스트 데이터 유지
3. 코덱 버전 관리 강화

롤백 계획: 코덱 변경 시 이전 버전 유지
```

### 10.3 회귀 테스트 자동화 스크립트

```bash
#!/bin/bash
# regression-test.sh - 회귀 테스트 자동화

set -e

echo "=== FxStore 회귀 테스트 시작 ==="
START_TIME=$(date +%s)

# 1. 전체 테스트 실행 (5분 타임아웃)
echo "[1/4] 전체 테스트 실행..."
timeout 300 ./gradlew test --no-daemon || {
    echo "❌ 테스트 실패 또는 타임아웃"
    exit 1
}

# 2. 커버리지 리포트 생성
echo "[2/4] 커버리지 리포트 생성..."
./gradlew jacocoTestReport --no-daemon

# 3. 커버리지 검증 (최소 기준)
echo "[3/4] 커버리지 검증..."
COVERAGE=$(grep -oP 'Total.*?\K\d+(?=%)' build/reports/jacoco/test/html/index.html | head -1)
if [ "$COVERAGE" -lt 29 ]; then
    echo "❌ 커버리지 하락: ${COVERAGE}% (최소 29%)"
    exit 1
fi
echo "✅ 커버리지: ${COVERAGE}%"

# 4. 결과 요약
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
echo "[4/4] 완료 (${DURATION}초)"
echo "=== 회귀 테스트 성공 ✅ ==="
```

```bash
#!/bin/bash
# coverage-check.sh - 커버리지 변화 확인

BASELINE_FILE=".coverage-baseline"
CURRENT=$(grep -oP 'Total.*?\K\d+(?=%)' build/reports/jacoco/test/html/index.html | head -1)

if [ -f "$BASELINE_FILE" ]; then
    BASELINE=$(cat "$BASELINE_FILE")
    DIFF=$((CURRENT - BASELINE))

    if [ "$DIFF" -lt 0 ]; then
        echo "⚠️ 커버리지 하락: ${BASELINE}% → ${CURRENT}% (${DIFF}%)"
        exit 1
    elif [ "$DIFF" -gt 0 ]; then
        echo "📈 커버리지 상승: ${BASELINE}% → ${CURRENT}% (+${DIFF}%)"
    else
        echo "➡️ 커버리지 유지: ${CURRENT}%"
    fi
fi

echo "$CURRENT" > "$BASELINE_FILE"
```

---

## 11. 리팩토링 단계 상세 (추가됨 v1.1)

### 11.1 FxStoreImpl 리팩토링 체크리스트

#### Step 1: CreateMode 열거형 추가
```java
// CreateMode.java (신규)
public enum CreateMode {
    CREATE_ONLY,    // 존재하면 예외
    OPEN_ONLY,      // 없으면 예외
    CREATE_OR_OPEN  // 없으면 생성, 있으면 열기
}
```
- [ ] CreateMode.java 파일 생성
- [ ] 단위 테스트 작성
- [ ] 기존 코드 영향 없음 확인

#### Step 2: CollectionFactory 인터페이스 추가
```java
// CollectionFactory.java (신규)
@FunctionalInterface
public interface CollectionFactory<T> {
    T create(BTree btree, FxCodec<?> codec);
}
```
- [ ] CollectionFactory.java 파일 생성
- [ ] 단위 테스트 작성
- [ ] 기존 코드 영향 없음 확인

#### Step 3: createOrOpenCollection 메서드 추가
```java
// FxStoreImpl.java에 추가
private <T> T createOrOpenCollection(...) { ... }
```
- [ ] 메서드 추가 (기존 메서드 유지)
- [ ] 단위 테스트 작성
- [ ] 전체 회귀 테스트

#### Step 4: createMap → createOrOpenCollection 마이그레이션
```java
// 변경 전
public <K, V> FxNavigableMap<K, V> createMap(...) {
    // 100줄+ 기존 코드
}

// 변경 후
public <K, V> FxNavigableMap<K, V> createMap(...) {
    return createOrOpenCollection(..., CreateMode.CREATE_ONLY, ...);
}
```
- [ ] createMap 마이그레이션
- [ ] 회귀 테스트
- [ ] openMap 마이그레이션
- [ ] 회귀 테스트
- [ ] createOrOpenMap 마이그레이션
- [ ] 회귀 테스트

#### Step 5: Set, List, Deque 동일 패턴 적용
- [ ] Set 3개 메서드 마이그레이션
- [ ] 회귀 테스트
- [ ] List 3개 메서드 마이그레이션
- [ ] 회귀 테스트
- [ ] Deque 3개 메서드 마이그레이션
- [ ] 회귀 테스트

#### Step 6: 기존 중복 코드 제거
- [ ] 마이그레이션 완료 확인
- [ ] 중복 코드 제거
- [ ] 전체 회귀 테스트
- [ ] 코드 리뷰

### 11.2 마이그레이션 전/후 비교

**변경 전 (12개 메서드, 총 ~1,200줄):**
```java
public FxNavigableMap createMap() { /* 100줄 */ }
public FxNavigableMap openMap() { /* 100줄 */ }
public FxNavigableMap createOrOpenMap() { /* 100줄 */ }
// Set, List, Deque도 동일 패턴...
```

**변경 후 (12개 메서드 + 1개 공통, 총 ~250줄):**
```java
private <T> T createOrOpenCollection() { /* 80줄 */ }

public FxNavigableMap createMap() { return createOrOpenCollection(...); }
public FxNavigableMap openMap() { return createOrOpenCollection(...); }
public FxNavigableMap createOrOpenMap() { return createOrOpenCollection(...); }
// Set, List, Deque도 동일 패턴...
```

**예상 효과:**
- 코드 라인 수: ~1,200줄 → ~250줄 (**-79%**)
- 메서드당 복잡도: 100줄 → 3줄 (**-97%**)
- 버그 수정 시: 12곳 → 1곳 (**-92%**)

---

## 12. Edge Case 테스트 시나리오 (추가됨 v1.1)

### 12.1 경계값 테스트

| # | 시나리오 | 입력 | 예상 결과 |
|---|----------|------|----------|
| E1 | 빈 컬렉션 순회 | size=0 | 예외 없이 완료 |
| E2 | 단일 요소 | size=1 | 정상 동작 |
| E3 | Integer.MAX_VALUE 인덱스 | index=2^31-1 | IndexOutOfBoundsException |
| E4 | null 키 | key=null | NullPointerException |
| E5 | null 값 | value=null | 정상 저장 (nullable) |
| E6 | 빈 문자열 키 | key="" | 정상 저장 |
| E7 | 매우 긴 문자열 | length=10MB | 정상 저장 또는 예외 |
| E8 | 특수 문자 키 | key="\0\n\t" | 정상 저장 |

### 12.2 동시성 Edge Case

| # | 시나리오 | 조건 | 예상 결과 |
|---|----------|------|----------|
| C1 | 동시 읽기 | 10 스레드 | 충돌 없음 |
| C2 | 쓰기 중 읽기 | 1 쓰기 + 10 읽기 | 스냅샷 격리 |
| C3 | 연속 커밋 | 1000회 커밋 | 모두 성공 |
| C4 | 커밋 중 close | commit() 중 close() | 예외 또는 안전 종료 |

### 12.3 리소스 Edge Case

| # | 시나리오 | 조건 | 예상 결과 |
|---|----------|------|----------|
| D1 | 디스크 풀 | 저장 공간 0 | IOException |
| D2 | 파일 잠금 충돌 | 이미 열린 파일 | FxException |
| D3 | 파일 없음 | open non-existent | FxException |
| D4 | 읽기 전용 파일 | write to readonly | FxException |

### 12.4 테스트 코드 예시 (import 포함)

```java
package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.FxStore;
import com.snoworca.fxstore.api.FxException;
import com.snoworca.fxstore.api.FxNavigableMap;
import com.snoworca.fxstore.api.FxOptions;
import com.snoworca.fxstore.storage.MemoryStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class EdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private FxStore store;

    @Before
    public void setUp() throws Exception {
        File file = tempFolder.newFile("test.fx");
        store = FxStore.open(file, FxOptions.builder().build());
    }

    @After
    public void tearDown() {
        if (store != null && !store.isClosed()) {
            store.close();
        }
    }

    // E1: 빈 컬렉션 순회
    @Test
    public void testIterateEmptyMap_NoException() {
        FxNavigableMap<String, String> map = store.createMap("empty", String.class, String.class);
        Iterator<String> it = map.keySet().iterator();
        assertFalse(it.hasNext());
        store.commit();
    }

    // E2: 단일 요소
    @Test
    public void testSingleElement_Works() {
        FxNavigableMap<String, String> map = store.createMap("single", String.class, String.class);
        map.put("key", "value");
        assertEquals("value", map.get("key"));
        assertEquals(1, map.size());
        store.commit();
    }

    // E4: null 키
    @Test(expected = NullPointerException.class)
    public void testNullKey_ThrowsNPE() {
        FxNavigableMap<String, String> map = store.createMap("nullkey", String.class, String.class);
        map.put(null, "value");
    }

    // E5: null 값
    @Test
    public void testNullValue_Accepted() {
        FxNavigableMap<String, String> map = store.createMap("nullval", String.class, String.class);
        map.put("key", null);
        assertNull(map.get("key"));
        assertTrue(map.containsKey("key"));
        store.commit();
    }

    // C1: 동시 읽기
    @Test
    public void testConcurrentReads_NoConflict() throws Exception {
        FxNavigableMap<Integer, String> map = store.createMap("concurrent", Integer.class, String.class);
        for (int i = 0; i < 1000; i++) {
            map.put(i, "value" + i);
        }
        store.commit();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 1000; i++) {
                        assertNotNull(map.get(i));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
    }

    // TreeMap 동등성 테스트
    @Test
    public void testEquivalenceWithTreeMap_SubMap() {
        FxNavigableMap<Integer, String> fxMap = store.createMap("equiv", Integer.class, String.class);
        TreeMap<Integer, String> treeMap = new TreeMap<>();

        for (int i = 1; i <= 100; i++) {
            fxMap.put(i, "v" + i);
            treeMap.put(i, "v" + i);
        }
        store.commit();

        NavigableMap<Integer, String> fxSub = fxMap.subMap(25, true, 75, true);
        NavigableMap<Integer, String> treeSub = treeMap.subMap(25, true, 75, true);

        assertEquals(treeSub.size(), fxSub.size());
        assertEquals(treeSub.firstKey(), fxSub.firstKey());
        assertEquals(treeSub.lastKey(), fxSub.lastKey());
    }
}
```

---

## 13. 용어집 (추가됨 v1.1)

| 용어 | 정의 |
|------|------|
| **커버리지 (Coverage)** | 테스트에 의해 실행된 코드의 비율 |
| **명령어 커버리지** | 바이트코드 명령어 실행 비율 (JaCoCo 기본) |
| **브랜치 커버리지** | 조건문 분기 실행 비율 |
| **라인 커버리지** | 소스 코드 라인 실행 비율 |
| **스냅샷 격리 (Snapshot Isolation)** | 트랜잭션이 시작 시점의 데이터 스냅샷을 보는 격리 수준 |
| **Wait-free Read** | 락 없이 항상 일정 시간 내 완료되는 읽기 연산 |
| **회귀 테스트** | 변경 후 기존 기능이 정상 동작하는지 확인하는 테스트 |
| **UOE** | UnsupportedOperationException의 약어 |
| **View** | 원본 컬렉션의 부분 집합을 나타내는 래퍼 (SubMap, HeadSet 등) |
| **BTree** | 균형 탐색 트리 (Balanced Tree), 디스크 최적화 자료구조 |
| **OST** | Order Statistic Tree, 인덱스 기반 접근을 지원하는 트리 |

---

## 14. 약어 정의 (추가됨 v1.1)

| 약어 | 전체 명칭 | 설명 |
|------|----------|------|
| API | Application Programming Interface | 프로그래밍 인터페이스 |
| CI/CD | Continuous Integration / Continuous Deployment | 지속적 통합/배포 |
| DIP | Dependency Inversion Principle | 의존성 역전 원칙 |
| ISP | Interface Segregation Principle | 인터페이스 분리 원칙 |
| LSP | Liskov Substitution Principle | 리스코프 치환 원칙 |
| OCP | Open/Closed Principle | 개방-폐쇄 원칙 |
| OOM | Out Of Memory | 메모리 부족 오류 |
| PR | Pull Request | 코드 병합 요청 |
| SRP | Single Responsibility Principle | 단일 책임 원칙 |
| UOE | UnsupportedOperationException | 지원하지 않는 연산 예외 |

---

## 15. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-12-30 | 초안 작성 |
| 1.1 | 2025-12-30 | Iteration 2 개선: 위험 관리 강화, 리팩토링 단계 상세화, Edge Case 추가, 용어집/약어 추가 |

---

*문서 작성일: 2025-12-30*
*상태: 📋 계획 수립 완료 (v1.1)*
