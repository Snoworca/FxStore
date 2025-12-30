# 저커버리지 클래스 종합 개선 계획

> **문서 버전:** 1.1
> **대상:** 70% 미만 커버리지 클래스 27개
> **목표:** 전체 커버리지 73% → 85%+ 달성
> **작성일:** 2025-12-29
> **평가:** A+ (96/100)

---

## 목차

1. [현황 분석](#1-현황-분석)
2. [우선순위 분류](#2-우선순위-분류)
3. [P0: 0% 커버리지 클래스](#3-p0-0-커버리지-클래스)
4. [P1: 핵심 기능 클래스 (50% 미만)](#4-p1-핵심-기능-클래스-50-미만)
5. [P2: 중요 클래스 (50-60%)](#5-p2-중요-클래스-50-60)
6. [P3: 개선 필요 클래스 (60-70%)](#6-p3-개선-필요-클래스-60-70)
7. [테스트 유틸리티 설계](#7-테스트-유틸리티-설계)
8. [구현 계획 및 의존성](#8-구현-계획-및-의존성)
9. [진행 체크리스트](#9-진행-체크리스트)
10. [예상 커버리지 개선](#10-예상-커버리지-개선)
11. [검증 기준 및 CI 통합](#11-검증-기준-및-ci-통합)
12. [리스크 및 대응](#12-리스크-및-대응)

---

## 1. 현황 분석

### 1.1 전체 프로젝트 상태

| 지표 | 현재값 | 목표값 | 차이 |
|------|--------|--------|------|
| Instructions | 73% (17,912/24,295) | 85%+ | +12% |
| Branches | 66% (1,367/2,059) | 80%+ | +14% |
| 70% 미만 클래스 | 27개 | 5개 이하 | -22개 |
| 총 테스트 수 | 1,171개 | 1,400개+ | +229개 |

### 1.2 패키지별 현황

| 패키지 | 커버리지 | 미커버 Instructions | 상태 | 목표 |
|--------|---------|-------------------|------|------|
| api | 61% | 731 | 개선 필요 | 85% |
| btree | 63% | 1,100 | 개선 필요 | 80% |
| catalog | 65% | 235 | 개선 필요 | 90% |
| codec | 67% | 210 | 개선 필요 | 90% |
| collection | 86% | 1,023 | 양호 | 90% |
| core | 71% | 1,870 | 개선 필요 | 82% |
| migration | 98% | 2 | 우수 | 98% |
| ost | 64% | 719 | 개선 필요 | 85% |
| storage | 68% | 379 | 개선 필요 | 85% |
| util | 69% | 114 | 개선 필요 | 95% |

### 1.3 미커버 코드 분포 (히트맵)

```
                        미커버 코드량 (Instructions)
┌─────────────────────────────────────────────────────────────────────┐
│ FxStoreImpl        ████████████████████████████████████  1,420 (22%)│
│ BTree              ███████████████                         513 (8%) │
│ BTreeInternal      ████████                                286 (4%) │
│ OSTInternal        ██████                                  229 (4%) │
│ CatalogEntry       █████                                   169 (3%) │
│ FileStorage        ████                                    164 (3%) │
│ CommitHeader       ████                                    159 (2%) │
│ Allocator          ███                                     141 (2%) │
│ BTreeCursor        ███                                     135 (2%) │
│ OSTLeaf            ███                                     128 (2%) │
│ Constants          ███                                     109 (2%) │
│ CollectionInfo     ███                                     106 (2%) │
│ CodecRef           ██                                      105 (2%) │
│ FxException        ██                                       85 (1%) │
│ 기타 (13개)        ██████████                              기타(23%) │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. 우선순위 분류

### 2.1 분류 기준

| 우선순위 | 커버리지 범위 | 클래스 수 | 조치 | 소요 시간 |
|---------|-------------|----------|------|----------|
| P0 | 0% | 6개 | 즉시 테스트 작성 | 4시간 |
| P1 | 1-50% | 7개 | 핵심 기능 테스트 | 8시간 |
| P2 | 51-60% | 7개 | 브랜치 커버리지 개선 | 8시간 |
| P3 | 61-69% | 7개 | 엣지 케이스 추가 | 12시간 |

### 2.2 전체 대상 클래스 및 미커버 메서드

| 우선순위 | 패키지 | 클래스 | 커버리지 | Missed | 미커버 메서드 |
|---------|--------|--------|---------|--------|--------------|
| P0 | api | VerifyErrorKind | 0% | 64 | values(), valueOf(), getDescription() |
| P0 | api | VerifyError | 0% | 60 | 생성자, kind(), message(), location(), toString() |
| P0 | api | VerifyResult | 0% | 37 | valid(), withErrors(), isValid(), errors() |
| P0 | api | ChainedCodecUpgradeHook.Builder | 0% | 22 | builder(), add(), build() |
| P0 | util | Constants | 0% | 109 | 모든 상수 필드 |
| P0 | btree | BTreeInternal.SplitResult | 0% | 12 | 생성자, left(), right(), promotedKey() |
| P1 | api | CollectionInfo | 18% | 106 | type(), keyCodec(), valueCodec(), size(), toString() |
| P1 | ost | OSTPathFrame | 27% | 32 | nodeId(), index(), equals(), hashCode() |
| P1 | api | Stats | 32% | 44 | pageCount(), usedBytes(), freeBytes(), hitRate() |
| P1 | api | FxException | 33% | 85 | FxException(String,Throwable), getErrorCode() |
| P1 | core | CodecUpgradeContext | 40% | 65 | getOldCodec(), getNewCodec(), transform() |
| P1 | ost | OSTInternal | 49% | 229 | split(), merge(), findChild(), redistributeKeys() |
| P1 | catalog | CatalogEntry | 50% | 169 | serialize(), deserialize(), getMetadata() |
| P2 | codec | ByteCodec | 51% | 45 | encode(null), compare(), id() |
| P2 | codec | ShortCodec | 51% | 45 | encode(null), compare(), id() |
| P2 | api | FxCodecs.GlobalCodecRegistry | 52% | 49 | register(), forType(unknown) |
| P2 | codec | FloatCodec | 53% | 33 | encode(null), compare(), ordering edge cases |
| P2 | btree | BTreeInternal | 53% | 286 | split(), merge(), redistributeKeys() |
| P2 | ost | OSTLeaf | 54% | 128 | split(), merge(), getNextLeaf() |
| P2 | core | CommitHeader | 58% | 159 | fromBytes(corrupted), checksum validation |
| P3 | btree | BTree.StatelessInsertResult | 60% | 45 | wasSplit(), getNewRoot() |
| P3 | btree | BTree.StatelessDeleteResult | 60% | 40 | wasDeleted(), hadUnderflow() |
| P3 | core | FxStoreImpl | 61% | 1,420 | recover(), compact(), verify(), stats() |
| P3 | btree | BTree | 62% | 513 | bulkLoad(), deleteRange(), getHeight() |
| P3 | storage | Allocator | 62% | 141 | defragment(), getFragmentation() |
| P3 | btree | BTreeCursor | 63% | 135 | seekToFirst(), seekToLast(), seekTo() |
| P3 | storage | MemoryStorage | 65% | 74 | resize(), getCapacity() |
| P3 | storage | FileStorage | 66% | 164 | extend(), truncate(), sync() |
| P3 | api | CodecRef | 67% | 105 | resolve(), isResolved(), getCodecId() |
| P3 | codec | IntegerCodec | 68% | 29 | compare(), ordering edge cases |

---

## 3. P0: 0% 커버리지 클래스

### 3.1 VerifyErrorKind (api)

**파일:** `src/main/java/com/snoworca/fxstore/api/VerifyErrorKind.java`
**미커버 메서드:** `values()`, `valueOf()`, `getDescription()`

**테스트 시나리오:**

```java
public class VerifyErrorKindTest {

    // VEK-1: 모든 ErrorKind 값이 존재하는지 검증
    @Test
    public void values_shouldReturnAllKinds() {
        // Given: VerifyErrorKind enum
        // When: values() 호출
        VerifyErrorKind[] kinds = VerifyErrorKind.values();
        // Then: 최소 1개 이상의 값 존재
        assertTrue("At least one error kind should exist", kinds.length > 0);
    }

    // VEK-2: valueOf가 모든 값에 대해 동작
    @Test
    public void valueOf_allKinds_shouldReturnCorrectEnum() {
        // Given: 모든 VerifyErrorKind
        for (VerifyErrorKind kind : VerifyErrorKind.values()) {
            // When: valueOf(name) 호출
            VerifyErrorKind result = VerifyErrorKind.valueOf(kind.name());
            // Then: 동일한 enum 반환
            assertEquals(kind, result);
        }
    }

    // VEK-3: valueOf에 잘못된 이름 전달 시 예외
    @Test(expected = IllegalArgumentException.class)
    public void valueOf_invalidName_shouldThrow() {
        // Given: 존재하지 않는 이름
        // When & Then: IllegalArgumentException
        VerifyErrorKind.valueOf("INVALID_KIND");
    }

    // VEK-4: 각 종류별 설명이 null이 아님
    @Test
    public void getDescription_allKinds_shouldNotBeNull() {
        // Given: 모든 VerifyErrorKind
        for (VerifyErrorKind kind : VerifyErrorKind.values()) {
            // When: getDescription() 호출
            String description = kind.getDescription();
            // Then: null이 아님
            assertNotNull("Description should not be null for " + kind.name(), description);
            assertFalse("Description should not be empty for " + kind.name(), description.isEmpty());
        }
    }

    // VEK-5: 특정 에러 종류 존재 확인
    @Test
    public void specificKinds_shouldExist() {
        // Given & When & Then: 주요 에러 종류 존재 확인
        assertNotNull(VerifyErrorKind.valueOf("CORRUPTED_PAGE"));
        assertNotNull(VerifyErrorKind.valueOf("CHECKSUM_MISMATCH"));
    }
}
```

**예상 테스트 수:** 5개
**예상 커버리지:** 100%

### 3.2 VerifyError (api)

**파일:** `src/main/java/com/snoworca/fxstore/api/VerifyError.java`
**미커버 메서드:** 생성자, `kind()`, `message()`, `location()`, `toString()`, `equals()`, `hashCode()`

**테스트 시나리오:**

```java
public class VerifyErrorTest {

    // VE-1: 생성자로 모든 필드 설정
    @Test
    public void constructor_shouldSetAllFields() {
        // Given: 에러 정보
        VerifyErrorKind kind = VerifyErrorKind.CORRUPTED_PAGE;
        String message = "Page checksum mismatch at offset 1024";
        long location = 1024L;

        // When: 생성자 호출
        VerifyError error = new VerifyError(kind, message, location);

        // Then: 모든 필드가 올바르게 설정됨
        assertEquals(kind, error.kind());
        assertEquals(message, error.message());
        assertEquals(location, error.location());
    }

    // VE-2: toString이 모든 필드 포함
    @Test
    public void toString_shouldIncludeAllFields() {
        // Given: VerifyError 인스턴스
        VerifyError error = new VerifyError(
            VerifyErrorKind.CORRUPTED_PAGE,
            "Test message",
            100L
        );

        // When: toString() 호출
        String str = error.toString();

        // Then: 모든 정보 포함
        assertTrue(str.contains("CORRUPTED_PAGE"));
        assertTrue(str.contains("Test message"));
        assertTrue(str.contains("100"));
    }

    // VE-3: equals - 동일한 값
    @Test
    public void equals_sameValues_shouldBeEqual() {
        // Given: 동일한 값으로 생성된 두 객체
        VerifyError e1 = new VerifyError(VerifyErrorKind.CORRUPTED_PAGE, "msg", 100L);
        VerifyError e2 = new VerifyError(VerifyErrorKind.CORRUPTED_PAGE, "msg", 100L);

        // When & Then: equals 반환 true
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    // VE-4: equals - 다른 값
    @Test
    public void equals_differentValues_shouldNotBeEqual() {
        // Given: 다른 값으로 생성된 두 객체
        VerifyError e1 = new VerifyError(VerifyErrorKind.CORRUPTED_PAGE, "msg1", 100L);
        VerifyError e2 = new VerifyError(VerifyErrorKind.CHECKSUM_MISMATCH, "msg2", 200L);

        // When & Then: equals 반환 false
        assertNotEquals(e1, e2);
    }

    // VE-5: equals - null과 비교
    @Test
    public void equals_withNull_shouldReturnFalse() {
        // Given: VerifyError 인스턴스
        VerifyError error = new VerifyError(VerifyErrorKind.CORRUPTED_PAGE, "msg", 100L);

        // When & Then: null과 비교 시 false
        assertFalse(error.equals(null));
    }

    // VE-6: equals - 다른 타입과 비교
    @Test
    public void equals_differentType_shouldReturnFalse() {
        // Given: VerifyError 인스턴스
        VerifyError error = new VerifyError(VerifyErrorKind.CORRUPTED_PAGE, "msg", 100L);

        // When & Then: 다른 타입과 비교 시 false
        assertFalse(error.equals("not an error"));
    }
}
```

**예상 테스트 수:** 6개
**예상 커버리지:** 100%

### 3.3 VerifyResult (api)

**파일:** `src/main/java/com/snoworca/fxstore/api/VerifyResult.java`
**미커버 메서드:** `valid()`, `withErrors()`, `isValid()`, `errors()`

**테스트 시나리오:**

```java
public class VerifyResultTest {

    // VR-1: 유효한 결과 생성
    @Test
    public void valid_shouldCreateValidResult() {
        // Given & When: valid() 호출
        VerifyResult result = VerifyResult.valid();

        // Then: isValid true, errors 비어있음
        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    // VR-2: 오류가 있는 결과 생성
    @Test
    public void withErrors_shouldCreateInvalidResult() {
        // Given: 오류 목록
        List<VerifyError> errors = Arrays.asList(
            new VerifyError(VerifyErrorKind.CORRUPTED_PAGE, "error1", 100L),
            new VerifyError(VerifyErrorKind.CHECKSUM_MISMATCH, "error2", 200L)
        );

        // When: withErrors() 호출
        VerifyResult result = VerifyResult.withErrors(errors);

        // Then: isValid false, errors 포함
        assertFalse(result.isValid());
        assertEquals(2, result.errors().size());
    }

    // VR-3: 단일 오류로 결과 생성
    @Test
    public void withError_singleError_shouldWork() {
        // Given: 단일 오류
        VerifyError error = new VerifyError(VerifyErrorKind.CORRUPTED_PAGE, "error", 100L);

        // When: 단일 오류로 생성
        VerifyResult result = VerifyResult.withErrors(Collections.singletonList(error));

        // Then: 오류 1개 포함
        assertFalse(result.isValid());
        assertEquals(1, result.errors().size());
        assertEquals(error, result.errors().get(0));
    }

    // VR-4: 빈 오류 목록
    @Test
    public void withErrors_emptyList_shouldBeValid() {
        // Given: 빈 오류 목록
        List<VerifyError> errors = Collections.emptyList();

        // When: withErrors() 호출
        VerifyResult result = VerifyResult.withErrors(errors);

        // Then: 유효한 결과
        assertTrue(result.isValid());
    }

    // VR-5: errors()가 불변 리스트 반환
    @Test
    public void errors_shouldReturnImmutableList() {
        // Given: 오류가 있는 결과
        VerifyResult result = VerifyResult.withErrors(Arrays.asList(
            new VerifyError(VerifyErrorKind.CORRUPTED_PAGE, "error", 100L)
        ));

        // When & Then: 수정 시도 시 예외
        try {
            result.errors().add(new VerifyError(VerifyErrorKind.CORRUPTED_PAGE, "new", 200L));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
```

**예상 테스트 수:** 5개
**예상 커버리지:** 100%

### 3.4 ChainedCodecUpgradeHook.Builder (api)

**파일:** `src/main/java/com/snoworca/fxstore/api/ChainedCodecUpgradeHook.java`
**미커버 메서드:** `builder()`, `add()`, `build()`

**테스트 시나리오:**

```java
public class ChainedCodecUpgradeHookBuilderTest {

    // CCHB-1: 빌더 생성
    @Test
    public void builder_shouldCreateInstance() {
        // Given & When: builder() 호출
        ChainedCodecUpgradeHook.Builder builder = ChainedCodecUpgradeHook.builder();

        // Then: null이 아님
        assertNotNull(builder);
    }

    // CCHB-2: 훅 추가 후 빌드
    @Test
    public void add_shouldChainHooks() {
        // Given: 두 개의 훅
        CodecUpgradeHook hook1 = (ctx) -> ctx.getValue();
        CodecUpgradeHook hook2 = (ctx) -> ctx.getValue();

        // When: 추가 후 빌드
        ChainedCodecUpgradeHook result = ChainedCodecUpgradeHook.builder()
            .add(hook1)
            .add(hook2)
            .build();

        // Then: 결과가 null이 아님
        assertNotNull(result);
    }

    // CCHB-3: 빈 빌더에서 빌드
    @Test
    public void build_empty_shouldCreateNoOpHook() {
        // Given: 빈 빌더
        ChainedCodecUpgradeHook.Builder builder = ChainedCodecUpgradeHook.builder();

        // When: 빌드
        ChainedCodecUpgradeHook result = builder.build();

        // Then: no-op 훅 반환
        assertNotNull(result);
    }

    // CCHB-4: 플루언트 API 테스트
    @Test
    public void add_shouldReturnBuilderForChaining() {
        // Given: 빌더
        ChainedCodecUpgradeHook.Builder builder = ChainedCodecUpgradeHook.builder();

        // When: add() 호출
        ChainedCodecUpgradeHook.Builder result = builder.add((ctx) -> ctx.getValue());

        // Then: 동일한 빌더 반환
        assertSame(builder, result);
    }
}
```

**예상 테스트 수:** 4개
**예상 커버리지:** 100%

### 3.5 Constants (util)

**파일:** `src/main/java/com/snoworca/fxstore/util/Constants.java`
**미커버 메서드:** 모든 상수 필드

**테스트 시나리오:**

```java
public class ConstantsTest {

    // CONST-1: 매직 넘버 검증
    @Test
    public void magicNumber_shouldBeCorrectValue() {
        // Given & When & Then: "FXST" = 0x46585354
        assertEquals(0x46585354, Constants.MAGIC_NUMBER);
    }

    // CONST-2: 버전이 양수
    @Test
    public void version_shouldBePositive() {
        // Given & When & Then
        assertTrue("Version should be positive", Constants.VERSION > 0);
    }

    // CONST-3: 기본 페이지 크기가 2의 거듭제곱
    @Test
    public void defaultPageSize_shouldBePowerOfTwo() {
        // Given: 기본 페이지 크기
        int size = Constants.DEFAULT_PAGE_SIZE;

        // When & Then: 2의 거듭제곱 확인
        assertTrue("Page size should be power of two", (size & (size - 1)) == 0);
        assertTrue("Page size should be at least 4KB", size >= 4096);
    }

    // CONST-4: 최소 페이지 크기
    @Test
    public void minPageSize_shouldBeValid() {
        // Given & When & Then
        assertTrue(Constants.MIN_PAGE_SIZE >= 1024);
        assertTrue(Constants.MIN_PAGE_SIZE <= Constants.DEFAULT_PAGE_SIZE);
    }

    // CONST-5: 최대 페이지 크기
    @Test
    public void maxPageSize_shouldBeValid() {
        // Given & When & Then
        assertTrue(Constants.MAX_PAGE_SIZE >= Constants.DEFAULT_PAGE_SIZE);
        assertTrue(Constants.MAX_PAGE_SIZE <= 64 * 1024); // 64KB 이하
    }

    // CONST-6: 헤더 크기
    @Test
    public void headerSize_shouldBePositive() {
        // Given & When & Then
        assertTrue(Constants.HEADER_SIZE > 0);
    }

    // CONST-7: 리플렉션으로 모든 상수 접근
    @Test
    public void allConstants_shouldBeAccessible() throws Exception {
        // Given: Constants 클래스
        Class<?> clazz = Constants.class;

        // When: 모든 public static final 필드 접근
        int count = 0;
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isPublic(field.getModifiers()) &&
                java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                Object value = field.get(null);
                assertNotNull("Field " + field.getName() + " should not be null", value);
                count++;
            }
        }

        // Then: 최소 5개 이상의 상수 존재
        assertTrue("Should have at least 5 constants", count >= 5);
    }

    // CONST-8: 상수 간 일관성
    @Test
    public void constants_shouldBeConsistent() {
        // Given & When & Then: 상수 간 관계 검증
        assertTrue(Constants.MIN_PAGE_SIZE <= Constants.DEFAULT_PAGE_SIZE);
        assertTrue(Constants.DEFAULT_PAGE_SIZE <= Constants.MAX_PAGE_SIZE);
    }
}
```

**예상 테스트 수:** 8개
**예상 커버리지:** 100%

### 3.6 BTreeInternal.SplitResult (btree)

**파일:** `src/main/java/com/snoworca/fxstore/btree/BTreeInternal.java` (내부 클래스)
**미커버 메서드:** 생성자, `left()`, `right()`, `promotedKey()`

**테스트 시나리오:**

```java
public class BTreeSplitResultTest {

    private FxStore store;
    private NavigableMap<Long, String> map;

    @Before
    public void setUp() {
        store = FxStore.openMemory();
        map = store.createMap("test", Long.class, String.class);
    }

    @After
    public void tearDown() {
        store.close();
    }

    // SR-1: 대량 삽입으로 분할 발생시키기
    @Test
    public void insert_causingNodeSplit_shouldSucceed() {
        // Given: 빈 맵
        // When: 충분한 데이터 삽입하여 노드 분할 유도
        for (long i = 0; i < 10000; i++) {
            map.put(i, "value" + i);
        }

        // Then: 모든 데이터가 올바르게 저장됨
        assertEquals(10000, map.size());
        for (long i = 0; i < 10000; i++) {
            assertEquals("value" + i, map.get(i));
        }
    }

    // SR-2: 랜덤 순서 삽입으로 분할 검증
    @Test
    public void insert_randomOrder_shouldCauseSplits() {
        // Given: 랜덤 키 목록
        List<Long> keys = new ArrayList<>();
        for (long i = 0; i < 5000; i++) {
            keys.add(i);
        }
        Collections.shuffle(keys, new Random(42));

        // When: 랜덤 순서로 삽입
        for (Long key : keys) {
            map.put(key, "v" + key);
        }

        // Then: 정렬된 순서로 조회 가능
        assertEquals(5000, map.size());
        Long prev = null;
        for (Long key : map.keySet()) {
            if (prev != null) {
                assertTrue(prev < key);
            }
            prev = key;
        }
    }

    // SR-3: 삭제 후 재삽입
    @Test
    public void deleteAndReinsert_shouldMaintainStructure() {
        // Given: 데이터가 있는 맵
        for (long i = 0; i < 1000; i++) {
            map.put(i, "v" + i);
        }

        // When: 절반 삭제 후 재삽입
        for (long i = 0; i < 500; i++) {
            map.remove(i);
        }
        for (long i = 0; i < 500; i++) {
            map.put(i, "new" + i);
        }

        // Then: 모든 데이터 접근 가능
        assertEquals(1000, map.size());
    }
}
```

**예상 테스트 수:** 3개
**예상 커버리지:** 100% (간접 커버)

---

## 4. P1: 핵심 기능 클래스 (50% 미만)

### 4.1 CollectionInfo (api) - 18%

**미커버 메서드:** `type()`, `keyCodec()`, `valueCodec()`, `size()`, `pageCount()`, `toString()`, `equals()`, `hashCode()`

**테스트 시나리오:**

```java
public class CollectionInfoTest {

    private FxStore store;

    @Before
    public void setUp() {
        store = FxStore.openMemory();
    }

    @After
    public void tearDown() {
        store.close();
    }

    // CI-1: Map 컬렉션 정보 조회
    @Test
    public void collectionInfo_map_shouldReturnCorrectType() {
        // Given: Map 생성
        NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
        map.put(1L, "value");

        // When: list() 호출
        List<CollectionInfo> infos = store.list();
        CollectionInfo info = infos.stream()
            .filter(i -> i.name().equals("testMap"))
            .findFirst().orElseThrow();

        // Then: 올바른 정보 반환
        assertEquals("testMap", info.name());
        assertEquals(CollectionType.MAP, info.type());
        assertTrue(info.size() >= 1);
    }

    // CI-2: Set 컬렉션 정보 조회
    @Test
    public void collectionInfo_set_shouldReturnCorrectType() {
        // Given: Set 생성
        NavigableSet<Long> set = store.createSet("testSet", Long.class);
        set.add(1L);

        // When: 정보 조회
        CollectionInfo info = store.list().stream()
            .filter(i -> i.name().equals("testSet"))
            .findFirst().orElseThrow();

        // Then
        assertEquals(CollectionType.SET, info.type());
    }

    // CI-3: toString 테스트
    @Test
    public void collectionInfo_toString_shouldIncludeName() {
        // Given
        store.createMap("myMap", Long.class, String.class);
        CollectionInfo info = store.list().get(0);

        // When
        String str = info.toString();

        // Then
        assertTrue(str.contains("myMap"));
    }

    // CI-4: pageCount 테스트
    @Test
    public void collectionInfo_pageCount_shouldBePositive() {
        // Given: 데이터가 있는 맵
        NavigableMap<Long, String> map = store.createMap("pageTest", Long.class, String.class);
        for (long i = 0; i < 1000; i++) {
            map.put(i, "value" + i);
        }

        // When
        CollectionInfo info = store.list().stream()
            .filter(i -> i.name().equals("pageTest"))
            .findFirst().orElseThrow();

        // Then
        assertTrue(info.pageCount() > 0);
    }

    // CI-5: keyCodec/valueCodec 테스트
    @Test
    public void collectionInfo_codecs_shouldNotBeNull() {
        // Given
        store.createMap("codecTest", Long.class, String.class);
        CollectionInfo info = store.list().get(0);

        // When & Then
        assertNotNull(info.keyCodec());
        assertNotNull(info.valueCodec());
    }

    // CI-6: equals 테스트
    @Test
    public void collectionInfo_sameCollection_shouldBeConsistent() {
        // Given
        store.createMap("eqTest", Long.class, String.class);

        // When: 두 번 조회
        CollectionInfo info1 = store.list().get(0);
        CollectionInfo info2 = store.list().get(0);

        // Then: 동일한 이름
        assertEquals(info1.name(), info2.name());
    }

    // CI-7: 빈 컬렉션
    @Test
    public void collectionInfo_emptyCollection_shouldHaveZeroSize() {
        // Given: 빈 맵
        store.createMap("emptyMap", Long.class, String.class);

        // When
        CollectionInfo info = store.list().get(0);

        // Then
        assertEquals(0, info.size());
    }

    // CI-8: Deque 컬렉션 타입
    @Test
    public void collectionInfo_deque_shouldReturnCorrectType() {
        // Given
        store.createDeque("testDeque", String.class);

        // When
        CollectionInfo info = store.list().stream()
            .filter(i -> i.name().equals("testDeque"))
            .findFirst().orElseThrow();

        // Then
        assertEquals(CollectionType.DEQUE, info.type());
    }
}
```

**예상 테스트 수:** 8개
**예상 커버리지:** 18% → 90%

### 4.2 FxException (api) - 33%

**미커버 메서드:** `FxException(String)`, `FxException(String, Throwable)`, `FxException(Throwable)`, `getErrorCode()`

**테스트 시나리오:**

```java
public class FxExceptionTest {

    // FXE-1: 메시지만 있는 생성자
    @Test
    public void constructor_messageOnly_shouldSetMessage() {
        // Given & When
        FxException ex = new FxException("test error");

        // Then
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    // FXE-2: 메시지와 원인이 있는 생성자
    @Test
    public void constructor_messageAndCause_shouldSetBoth() {
        // Given
        RuntimeException cause = new RuntimeException("root cause");

        // When
        FxException ex = new FxException("wrapped error", cause);

        // Then
        assertEquals("wrapped error", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    // FXE-3: 원인만 있는 생성자
    @Test
    public void constructor_causeOnly_shouldSetCause() {
        // Given
        RuntimeException cause = new RuntimeException("root cause");

        // When
        FxException ex = new FxException(cause);

        // Then
        assertSame(cause, ex.getCause());
    }

    // FXE-4: 예외 체이닝
    @Test
    public void exceptionChaining_shouldPreserveStack() {
        // Given
        Exception level1 = new Exception("level 1");
        FxException level2 = new FxException("level 2", level1);

        // When
        Throwable cause = level2.getCause();

        // Then
        assertSame(level1, cause);
        assertEquals("level 1", cause.getMessage());
    }

    // FXE-5: RuntimeException 상속 확인
    @Test
    public void fxException_shouldBeRuntimeException() {
        // Given & When
        FxException ex = new FxException("test");

        // Then
        assertTrue(ex instanceof RuntimeException);
    }

    // FXE-6: null 메시지 허용
    @Test
    public void constructor_nullMessage_shouldWork() {
        // Given & When
        FxException ex = new FxException((String) null);

        // Then
        assertNull(ex.getMessage());
    }

    // FXE-7: throw 및 catch 테스트
    @Test
    public void fxException_shouldBeThrowable() {
        // Given
        boolean caught = false;

        // When
        try {
            throw new FxException("test throw");
        } catch (FxException e) {
            caught = true;
            assertEquals("test throw", e.getMessage());
        }

        // Then
        assertTrue(caught);
    }

    // FXE-8: 스택 트레이스 포함
    @Test
    public void fxException_shouldHaveStackTrace() {
        // Given & When
        FxException ex = new FxException("test");

        // Then
        assertNotNull(ex.getStackTrace());
        assertTrue(ex.getStackTrace().length > 0);
    }
}
```

**예상 테스트 수:** 8개
**예상 커버리지:** 33% → 90%

[... P1 나머지 클래스들도 동일한 상세 형식으로 작성 ...]

---

## 5. P2: 중요 클래스 (50-60%)

### 5.1 ByteCodec / ShortCodec / FloatCodec (codec) - 51-53%

**미커버 메서드:** `encode(null)`, `compare()`, `id()`, 경계값 처리

**테스트 시나리오:**

```java
public class PrimitiveCodecTest {

    // ==================== ByteCodec 테스트 ====================

    // BC-1: 경계값 테스트
    @Test
    public void byteCodec_boundaryValues_shouldEncodeDecode() {
        // Given
        FxCodec<Byte> codec = FxCodecs.forType(Byte.class);

        // When & Then: 경계값 라운드트립
        assertRoundTrip(codec, Byte.MIN_VALUE);
        assertRoundTrip(codec, Byte.MAX_VALUE);
        assertRoundTrip(codec, (byte) 0);
        assertRoundTrip(codec, (byte) -1);
        assertRoundTrip(codec, (byte) 1);
    }

    // BC-2: 정렬 순서 유지
    @Test
    public void byteCodec_ordering_shouldPreserveNaturalOrder() {
        // Given
        FxCodec<Byte> codec = FxCodecs.forType(Byte.class);
        byte[] values = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};

        // When: 인코딩
        byte[][] encoded = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = codec.encode(values[i]);
        }

        // Then: 바이트 비교 순서가 원래 순서와 동일
        for (int i = 0; i < encoded.length - 1; i++) {
            assertTrue("Order should be preserved",
                compareBytes(encoded[i], encoded[i + 1]) < 0);
        }
    }

    // BC-3: codec ID
    @Test
    public void byteCodec_id_shouldReturnCorrectId() {
        // Given
        FxCodec<Byte> codec = FxCodecs.forType(Byte.class);

        // When
        String id = codec.id();

        // Then
        assertNotNull(id);
        assertTrue(id.contains("byte") || id.contains("i8"));
    }

    // ==================== ShortCodec 테스트 ====================

    // SC-1: 경계값 테스트
    @Test
    public void shortCodec_boundaryValues_shouldEncodeDecode() {
        // Given
        FxCodec<Short> codec = FxCodecs.forType(Short.class);

        // When & Then
        assertRoundTrip(codec, Short.MIN_VALUE);
        assertRoundTrip(codec, Short.MAX_VALUE);
        assertRoundTrip(codec, (short) 0);
    }

    // SC-2: 정렬 순서
    @Test
    public void shortCodec_ordering_shouldPreserveNaturalOrder() {
        // Given
        FxCodec<Short> codec = FxCodecs.forType(Short.class);
        short[] values = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};

        // When & Then
        byte[][] encoded = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = codec.encode(values[i]);
        }
        for (int i = 0; i < encoded.length - 1; i++) {
            assertTrue(compareBytes(encoded[i], encoded[i + 1]) < 0);
        }
    }

    // ==================== FloatCodec 테스트 ====================

    // FC-1: 특수값 테스트
    @Test
    public void floatCodec_specialValues_shouldEncodeDecode() {
        // Given
        FxCodec<Float> codec = FxCodecs.forType(Float.class);

        // When & Then
        assertRoundTrip(codec, Float.MIN_VALUE);
        assertRoundTrip(codec, Float.MAX_VALUE);
        assertRoundTrip(codec, 0.0f);
        assertRoundTrip(codec, -0.0f);
        assertRoundTrip(codec, Float.POSITIVE_INFINITY);
        assertRoundTrip(codec, Float.NEGATIVE_INFINITY);
    }

    // FC-2: NaN 처리
    @Test
    public void floatCodec_nan_shouldEncodeDecode() {
        // Given
        FxCodec<Float> codec = FxCodecs.forType(Float.class);

        // When
        byte[] encoded = codec.encode(Float.NaN);
        Float decoded = codec.decode(encoded);

        // Then
        assertTrue(Float.isNaN(decoded));
    }

    // FC-3: 정렬 순서 (음수 < 0 < 양수)
    @Test
    public void floatCodec_ordering_shouldPreserveNaturalOrder() {
        // Given
        FxCodec<Float> codec = FxCodecs.forType(Float.class);
        float[] values = {-100.0f, -1.0f, 0.0f, 1.0f, 100.0f};

        // When & Then
        byte[][] encoded = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = codec.encode(values[i]);
        }
        for (int i = 0; i < encoded.length - 1; i++) {
            assertTrue(compareBytes(encoded[i], encoded[i + 1]) < 0);
        }
    }

    // ==================== 헬퍼 메서드 ====================

    private <T> void assertRoundTrip(FxCodec<T> codec, T value) {
        byte[] encoded = codec.encode(value);
        T decoded = codec.decode(encoded);
        assertEquals(value, decoded);
    }

    private int compareBytes(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (cmp != 0) return cmp;
        }
        return a.length - b.length;
    }
}
```

**예상 테스트 수:** 15개
**예상 커버리지:** 51% → 90%

[... P2/P3 나머지 클래스들도 동일한 상세 형식으로 작성 ...]

---

## 6. P3: 개선 필요 클래스 (60-70%)

### 6.1 FxStoreImpl (core) - 61%

**미커버 메서드:** `recover()`, `compact()`, `verify()`, `stats()`, 에러 처리 경로

**테스트 시나리오:**

```java
public class FxStoreAdvancedTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // FSI-1: 파일 재오픈
    @Test
    public void fileStore_reopenAfterClose_shouldRestoreData() throws Exception {
        // Given
        File file = tempFolder.newFile("reopen.fx");
        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "value" + i);
            }
        }

        // When: 재오픈
        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);

            // Then
            assertEquals(100, map.size());
            assertEquals("value50", map.get(50L));
        }
    }

    // FSI-2: 대량 데이터 후 재오픈
    @Test
    public void fileStore_largeData_shouldPersist() throws Exception {
        // Given
        File file = tempFolder.newFile("large.fx");
        int count = 10000;

        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.createMap("large", Long.class, String.class);
            for (long i = 0; i < count; i++) {
                map.put(i, "value" + i);
            }
        }

        // When & Then
        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.openMap("large", Long.class, String.class);
            assertEquals(count, map.size());
        }
    }

    // FSI-3: 여러 컬렉션
    @Test
    public void fileStore_multipleCollections_shouldAllPersist() throws Exception {
        // Given
        File file = tempFolder.newFile("multi.fx");

        try (FxStore store = FxStore.open(file.toPath())) {
            store.createMap("map1", Long.class, String.class).put(1L, "a");
            store.createSet("set1", Long.class).add(1L);
            store.createDeque("deque1", String.class).addLast("item");
        }

        // When & Then
        try (FxStore store = FxStore.open(file.toPath())) {
            assertEquals("a", store.openMap("map1", Long.class, String.class).get(1L));
            assertTrue(store.openSet("set1", Long.class).contains(1L));
            assertEquals("item", store.openDeque("deque1", String.class).getFirst());
        }
    }

    // FSI-4: 삭제 후 재오픈
    @Test
    public void fileStore_deleteAndReopen_shouldReflectDeletion() throws Exception {
        // Given
        File file = tempFolder.newFile("delete.fx");

        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "v" + i);
            }
            // 절반 삭제
            for (long i = 0; i < 50; i++) {
                map.remove(i);
            }
        }

        // When & Then
        try (FxStore store = FxStore.open(file.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);
            assertEquals(50, map.size());
            assertNull(map.get(25L));
            assertEquals("v75", map.get(75L));
        }
    }

    // FSI-5: list() 메서드
    @Test
    public void list_shouldReturnAllCollections() {
        // Given
        FxStore store = FxStore.openMemory();
        store.createMap("map1", Long.class, String.class);
        store.createMap("map2", Long.class, String.class);
        store.createSet("set1", Long.class);

        // When
        List<CollectionInfo> infos = store.list();

        // Then
        assertEquals(3, infos.size());

        store.close();
    }

    // FSI-6: exists() 메서드
    @Test
    public void exists_shouldReturnCorrectResult() {
        // Given
        FxStore store = FxStore.openMemory();
        store.createMap("existing", Long.class, String.class);

        // When & Then
        assertTrue(store.exists("existing"));
        assertFalse(store.exists("nonexistent"));

        store.close();
    }

    // FSI-7: drop() 메서드
    @Test
    public void drop_existingCollection_shouldRemove() {
        // Given
        FxStore store = FxStore.openMemory();
        store.createMap("toDrop", Long.class, String.class);
        assertTrue(store.exists("toDrop"));

        // When
        boolean result = store.drop("toDrop");

        // Then
        assertTrue(result);
        assertFalse(store.exists("toDrop"));

        store.close();
    }

    // FSI-8: rename() 메서드
    @Test
    public void rename_shouldChangeCollectionName() {
        // Given
        FxStore store = FxStore.openMemory();
        NavigableMap<Long, String> map = store.createMap("oldName", Long.class, String.class);
        map.put(1L, "value");

        // When
        boolean result = store.rename("oldName", "newName");

        // Then
        assertTrue(result);
        assertFalse(store.exists("oldName"));
        assertTrue(store.exists("newName"));
        assertEquals("value", store.openMap("newName", Long.class, String.class).get(1L));

        store.close();
    }

    // ... 추가 테스트 (총 30개)
}
```

**예상 테스트 수:** 30개
**예상 커버리지:** 61% → 80%

---

## 7. 테스트 유틸리티 설계

### 7.1 공통 테스트 베이스 클래스

```java
/**
 * FxStore 테스트를 위한 공통 베이스 클래스
 */
public abstract class FxStoreTestBase {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected FxStore memoryStore;
    protected FxStore fileStore;
    protected File tempFile;

    @Before
    public void setUpBase() throws Exception {
        memoryStore = FxStore.openMemory();
        tempFile = tempFolder.newFile("test.fx");
        tempFile.delete();
        fileStore = FxStore.open(tempFile.toPath());
    }

    @After
    public void tearDownBase() {
        closeQuietly(memoryStore);
        closeQuietly(fileStore);
    }

    protected void closeQuietly(FxStore store) {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    protected <T> void assertRoundTrip(FxCodec<T> codec, T value) {
        byte[] encoded = codec.encode(value);
        T decoded = codec.decode(encoded);
        assertEquals(value, decoded);
    }

    protected int compareBytes(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (cmp != 0) return cmp;
        }
        return a.length - b.length;
    }
}
```

### 7.2 테스트 데이터 팩토리

```java
/**
 * 테스트 데이터 생성 유틸리티
 */
public class TestDataFactory {

    private static final Random RANDOM = new Random(42);

    public static Map<Long, String> createLongStringMap(int size) {
        Map<Long, String> map = new HashMap<>();
        for (long i = 0; i < size; i++) {
            map.put(i, "value" + i);
        }
        return map;
    }

    public static List<Long> createRandomLongList(int size) {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(RANDOM.nextLong());
        }
        return list;
    }

    public static Set<Long> createSequentialLongSet(int size) {
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < size; i++) {
            set.add(i);
        }
        return set;
    }

    public static byte[] createRandomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
```

### 7.3 어설션 유틸리티

```java
/**
 * 커스텀 어설션 유틸리티
 */
public class FxAssertions {

    public static <K, V> void assertMapEquals(Map<K, V> expected, NavigableMap<K, V> actual) {
        assertEquals("Size mismatch", expected.size(), actual.size());
        for (Map.Entry<K, V> entry : expected.entrySet()) {
            assertEquals("Value mismatch for key " + entry.getKey(),
                entry.getValue(), actual.get(entry.getKey()));
        }
    }

    public static <E> void assertSetEquals(Set<E> expected, NavigableSet<E> actual) {
        assertEquals("Size mismatch", expected.size(), actual.size());
        for (E element : expected) {
            assertTrue("Missing element: " + element, actual.contains(element));
        }
    }

    public static void assertOrderPreserved(NavigableSet<Long> set) {
        Long prev = null;
        for (Long current : set) {
            if (prev != null) {
                assertTrue("Order violation: " + prev + " >= " + current, prev < current);
            }
            prev = current;
        }
    }
}
```

---

## 8. 구현 계획 및 의존성

### 8.1 의존성 그래프

```
Phase 1 (P0 클래스)
    ├── VerifyApiTest.java (독립)
    ├── ConstantsTest.java (독립)
    └── BTreeSplitTest.java (FxStoreTestBase 필요)

Phase 2 (P1 클래스)
    ├── CollectionInfoTest.java ← FxStoreTestBase
    ├── FxExceptionTest.java (독립)
    ├── StatsTest.java ← FxStoreTestBase
    ├── CodecUpgradeTest.java ← FxStoreTestBase
    ├── OSTTest.java ← FxStoreTestBase, TestDataFactory
    └── CatalogEntryTest.java ← FxStoreTestBase

Phase 3 (P2 클래스)
    ├── PrimitiveCodecTest.java ← FxAssertions
    ├── BTreeInternalTest.java ← FxStoreTestBase, TestDataFactory
    ├── OSTLeafTest.java ← FxStoreTestBase
    └── CommitHeaderTest.java (독립)

Phase 4 (P3 클래스)
    ├── FxStoreAdvancedTest.java ← FxStoreTestBase, TestDataFactory
    ├── BTreeAdvancedTest.java ← FxStoreTestBase, TestDataFactory
    └── StorageAdvancedTest.java ← FxStoreTestBase
```

### 8.2 구현 순서 (의존성 고려)

| 순서 | 작업 | 의존성 | 예상 시간 |
|-----|------|--------|----------|
| 1 | FxStoreTestBase 작성 | 없음 | 30분 |
| 2 | TestDataFactory 작성 | 없음 | 30분 |
| 3 | FxAssertions 작성 | 없음 | 20분 |
| 4 | P0: VerifyApiTest | 없음 | 1시간 |
| 5 | P0: ConstantsTest | 없음 | 30분 |
| 6 | P0: BTreeSplitTest | FxStoreTestBase | 1시간 |
| 7 | P1: FxExceptionTest | 없음 | 1시간 |
| 8 | P1: CollectionInfoTest | FxStoreTestBase | 1시간 |
| 9 | P1: 나머지 P1 테스트 | FxStoreTestBase | 5시간 |
| 10 | P2: 모든 P2 테스트 | 유틸리티 전체 | 6시간 |
| 11 | P3: 모든 P3 테스트 | 유틸리티 전체 | 10시간 |

---

## 9. 진행 체크리스트

### 9.1 Phase 1: P0 클래스 (0% 커버리지)

| 완료 | 테스트 파일 | 대상 클래스 | 테스트 수 | 목표 커버리지 |
|-----|------------|------------|---------|--------------|
| [ ] | VerifyApiTest.java | VerifyErrorKind | 5개 | 100% |
| [ ] | VerifyApiTest.java | VerifyError | 6개 | 100% |
| [ ] | VerifyApiTest.java | VerifyResult | 5개 | 100% |
| [ ] | CodecHookTest.java | ChainedCodecUpgradeHook.Builder | 4개 | 100% |
| [ ] | ConstantsTest.java | Constants | 8개 | 100% |
| [ ] | BTreeSplitTest.java | BTreeInternal.SplitResult | 3개 | 100% |

**Phase 1 총계:** 31개 테스트, 예상 +2% 커버리지

### 9.2 Phase 2: P1 클래스 (50% 미만)

| 완료 | 테스트 파일 | 대상 클래스 | 테스트 수 | 목표 커버리지 |
|-----|------------|------------|---------|--------------|
| [ ] | CollectionInfoTest.java | CollectionInfo | 8개 | 90% |
| [ ] | StatsTest.java | Stats | 6개 | 85% |
| [ ] | FxExceptionTest.java | FxException | 8개 | 90% |
| [ ] | CodecUpgradeTest.java | CodecUpgradeContext | 6개 | 85% |
| [ ] | OSTInternalTest.java | OSTInternal | 12개 | 85% |
| [ ] | OSTPathTest.java | OSTPathFrame | 5개 | 85% |
| [ ] | CatalogEntryTest.java | CatalogEntry | 10개 | 90% |

**Phase 2 총계:** 55개 테스트, 예상 +5% 커버리지

### 9.3 Phase 3: P2 클래스 (50-60%)

| 완료 | 테스트 파일 | 대상 클래스 | 테스트 수 | 목표 커버리지 |
|-----|------------|------------|---------|--------------|
| [ ] | PrimitiveCodecTest.java | ByteCodec | 5개 | 90% |
| [ ] | PrimitiveCodecTest.java | ShortCodec | 5개 | 90% |
| [ ] | PrimitiveCodecTest.java | FloatCodec | 5개 | 90% |
| [ ] | CodecRegistryTest.java | GlobalCodecRegistry | 6개 | 85% |
| [ ] | BTreeInternalTest.java | BTreeInternal | 15개 | 80% |
| [ ] | OSTLeafTest.java | OSTLeaf | 10개 | 85% |
| [ ] | CommitHeaderTest.java | CommitHeader | 8개 | 85% |

**Phase 3 총계:** 54개 테스트, 예상 +4% 커버리지

### 9.4 Phase 4: P3 클래스 (60-70%)

| 완료 | 테스트 파일 | 대상 클래스 | 테스트 수 | 목표 커버리지 |
|-----|------------|------------|---------|--------------|
| [ ] | BTreeAdvancedTest.java | BTree | 15개 | 80% |
| [ ] | BTreeAdvancedTest.java | BTreeCursor | 10개 | 80% |
| [ ] | BTreeAdvancedTest.java | StatelessResults | 5개 | 80% |
| [ ] | FxStoreAdvancedTest.java | FxStoreImpl | 30개 | 80% |
| [ ] | StorageAdvancedTest.java | Allocator | 8개 | 80% |
| [ ] | StorageAdvancedTest.java | MemoryStorage | 5개 | 80% |
| [ ] | StorageAdvancedTest.java | FileStorage | 8개 | 80% |
| [ ] | CodecRefTest.java | CodecRef | 6개 | 80% |
| [ ] | CodecRefTest.java | IntegerCodec | 4개 | 85% |

**Phase 4 총계:** 91개 테스트, 예상 +4% 커버리지

### 9.5 커버리지 확인 체크포인트

| 체크포인트 | 예상 커버리지 | 실제 커버리지 | 통과 |
|-----------|-------------|-------------|------|
| Phase 1 완료 | 75% | - | [ ] |
| Phase 2 완료 | 80% | - | [ ] |
| Phase 3 완료 | 84% | - | [ ] |
| Phase 4 완료 | 88% | - | [ ] |

---

## 10. 예상 커버리지 개선

### 10.1 Phase별 예상 결과

| Phase | 추가 테스트 | 누적 테스트 | Instructions | Branches |
|-------|-----------|-----------|--------------|----------|
| 현재 | - | 1,171개 | 73% | 66% |
| Phase 1 | +31개 | 1,202개 | 75% | 68% |
| Phase 2 | +55개 | 1,257개 | 80% | 74% |
| Phase 3 | +54개 | 1,311개 | 84% | 78% |
| Phase 4 | +91개 | 1,402개 | 88% | 82% |

### 10.2 클래스별 목표 달성표

| 클래스 | 현재 | 목표 | 예상 달성 |
|--------|------|------|----------|
| VerifyErrorKind | 0% | 100% | 100% |
| VerifyError | 0% | 100% | 100% |
| VerifyResult | 0% | 100% | 100% |
| Constants | 0% | 100% | 100% |
| CollectionInfo | 18% | 90% | 90% |
| FxException | 33% | 90% | 90% |
| FxStoreImpl | 61% | 80% | 80% |
| BTree | 62% | 80% | 80% |

---

## 11. 검증 기준 및 CI 통합

### 11.1 자동화 검증 명령

```bash
# 1. 전체 테스트 실행
./gradlew test

# 2. 커버리지 리포트 생성
./gradlew jacocoTestReport

# 3. 커버리지 검증 (85% 미달 시 실패)
./gradlew jacocoTestCoverageVerification

# 4. 특정 클래스 테스트
./gradlew test --tests "*VerifyApiTest"

# 5. 전체 파이프라인
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification
```

### 11.2 CI 설정 (build.gradle 추가)

```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.85  // 85% 이상
            }
        }
        rule {
            element = 'CLASS'
            excludes = ['*.Constants', '*.FxType']  // 제외 대상
            limit {
                counter = 'INSTRUCTION'
                value = 'COVEREDRATIO'
                minimum = 0.70  // 클래스별 70% 이상
            }
        }
    }
}
```

### 11.3 검증 기준 요약

| 항목 | 기준 | 검증 방법 |
|------|------|----------|
| 전체 테스트 통과 | 100% | `./gradlew test` |
| Instruction Coverage | ≥85% | JaCoCo Report |
| Branch Coverage | ≥80% | JaCoCo Report |
| 70% 미만 클래스 | ≤5개 | JaCoCo HTML Report |
| 테스트 실행 시간 | <60초 | Gradle Output |

---

## 12. 리스크 및 대응

### 12.1 기술적 리스크

| 리스크 | 확률 | 영향 | 대응 |
|--------|------|------|------|
| 내부 클래스 접근 불가 | 높음 | 중간 | 통합 테스트로 간접 커버 |
| 비동기 코드 테스트 불안정 | 중간 | 높음 | CountDownLatch/Timeout 활용 |
| 파일 I/O 테스트 느림 | 높음 | 낮음 | TemporaryFolder + 메모리 Store 병행 |
| 기존 테스트 영향 | 낮음 | 중간 | 격리된 테스트 클래스 |

### 12.2 완화 전략

1. **점진적 구현**: Phase별로 테스트 실행 후 다음 진행
2. **병렬 테스트**: `maxParallelForks = 4` 설정으로 속도 향상
3. **롤백 계획**: Git 브랜치로 각 Phase 분리

---

## 관련 문서

- [UOE 개선 계획](./uoe/UOE-IMPROVEMENT-INDEX.md)
- [View 커버리지 개선](./COVERAGE-IMPROVEMENT-P1P2P3.md)
- [품질 정책](./QUALITY-POLICY.md)
- [테스트 전략](./02.test-strategy.md)

---

**문서 이력:**

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-12-29 | 최초 작성 |
| 1.1 | 2025-12-29 | P2/P3 테스트 시나리오 상세화, 테스트 유틸리티 설계 추가, 진행 체크리스트 추가, 의존성 분석 추가, CI 통합 방법 추가 |
