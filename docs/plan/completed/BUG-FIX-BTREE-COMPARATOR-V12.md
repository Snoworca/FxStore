# BTree 바이트 비교자 버그 수정 계획 (v1.2)

> **문서 버전:** 1.0
> **작성일:** 2025-12-31
> **심각도:** Critical
> **영향 범위:** 모든 NavigableMap/Set 정렬 연산

[← 목차로 돌아가기](00.index.md)

---

## 1. 문제 요약

### 1.1 발견 경위
통합 테스트(INTEGRATION-TEST-IMPLEMENTATION-PLAN.md) 실행 중 대규모 데이터에서 정렬 문제 발견.

### 1.2 증상

| 증상 | 예상값 | 실제값 | 테스트 케이스 |
|------|--------|--------|--------------|
| `lastKey()` (100K 데이터) | 99999 | 65535 (2^16-1) | test_A2 |
| `firstKey()` (음수 포함) | Long.MIN_VALUE | 0 | test_E_boundaryLongs |
| `subMap(10000, 10010).size()` | 11 | 3911 | test_A2 |

### 1.3 근본 원인

**BTree의 바이트 비교자가 Unsigned Byte 비교를 사용하여 Signed Long을 잘못 정렬**

```java
// 현재 코드 (FxStoreImpl.java:2029-2041)
Comparator<byte[]> byteComparator = (a, b) -> {
    for (int i = 0; i < minLen; i++) {
        int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);  // ❌ Unsigned byte 비교
        if (cmp != 0) return cmp;
    }
    return a.length - b.length;
};
```

**문제점:**
- Little-Endian 인코딩에서 음수의 MSB(최상위 바이트)는 `0x80~0xFF` 범위
- Unsigned byte 비교 시 음수가 큰 양수처럼 취급됨
- BTree 트리 구조가 잘못된 순서로 구성됨

**대조:**
- `I64Codec.compareBytes()`는 올바르게 `Long.compare()` 사용
- 그러나 BTree는 이 메서드를 사용하지 않음

---

## 2. 영향 분석

### 2.1 영향받는 코드

| 파일 | 라인 | 설명 |
|------|------|------|
| `FxStoreImpl.java` | 2029-2041 | `getBTreeForCollection()` 내 byteComparator |
| `FxStoreImpl.java` | 2100-2117 | `createComparator()` 내 바이트 비교 |
| `FxReadTransactionImpl.java` | 108-117 | `createBTree()` 내 byteComparator |

### 2.2 영향받는 연산

- **NavigableMap**: `firstKey()`, `lastKey()`, `ceilingKey()`, `floorKey()`, `higherKey()`, `lowerKey()`
- **범위 쿼리**: `subMap()`, `headMap()`, `tailMap()`
- **뷰 클래스**: `SubMapView`, `HeadMapView`, `TailMapView`, `DescendingMapView`
- **NavigableSet**: 동일한 연산들
- **BTree 내부**: 모든 삽입/검색/삭제 경로

### 2.3 영향받지 않는 연산

- `get()`, `put()`, `remove()` - 키 존재 확인만 하므로 영향 적음 (단, 순서 의존 로직 제외)
- String 키 - 바이트 비교가 맞음
- byte[] 키 - 바이트 비교가 맞음

---

## 3. 수정 방안

### 3.1 권장 방안: 코덱의 compareBytes() 사용

**원칙**: BTree는 자체 비교 로직 대신 코덱의 `compareBytes()` 메서드를 사용

**장점:**
- 각 타입에 맞는 정확한 비교
- `FxCodec` 인터페이스의 설계 의도 충족
- 모든 코덱 타입에 자동 적용

**단점:**
- BTree 생성 시 코덱 정보 필요
- 약간의 API 변경

### 3.2 구현 계획

#### Phase 1: 코덱 기반 비교자 팩토리 추가

```java
// FxStoreImpl.java에 추가
private Comparator<byte[]> createByteComparator(FxCodec<?> keyCodec) {
    return (a, b) -> keyCodec.compareBytes(a, b);
}
```

#### Phase 2: BTree 생성 수정

```java
// FxStoreImpl.getBTreeForCollection() 수정
public BTree getBTreeForCollection(long collectionId, FxCodec<?> keyCodec) {
    // ...
    Comparator<byte[]> byteComparator = createByteComparator(keyCodec);
    return new BTree(storage, pageSize, byteComparator, rootPageId, allocator);
}
```

#### Phase 3: FxReadTransactionImpl 수정

```java
// FxReadTransactionImpl.createBTree() 수정
private BTree createBTree(long collectionId, FxCodec<?> keyCodec) {
    Comparator<byte[]> byteComparator = (a, b) -> keyCodec.compareBytes(a, b);
    return new BTree(storage, pageSize, byteComparator, 0);
}
```

#### Phase 4: 호출 지점 수정

모든 `getBTreeForCollection()` 호출 지점에서 keyCodec 전달

---

## 4. 테스트 계획

### 4.1 버그 재현 테스트 (우선 작성)

새 테스트 클래스: `BTreeByteComparatorBugTest.java`

```java
/**
 * BUG-V12-001: BTree 바이트 비교자 버그 재현 테스트
 *
 * <p>증상: Unsigned byte 비교로 인한 정렬 오류
 */
public class BTreeByteComparatorBugTest extends IntegrationTestBase {

    /**
     * BUG-V12-001-1: lastKey()가 65535를 반환하는 버그
     */
    @Test
    public void test_bug_lastKey_returns65535_shouldReturn99999() {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug1", Long.class, String.class);

        for (int i = 0; i < 100_000; i++) {
            map.put((long) i, "v" + i);
        }
        store.commit();

        // 버그 수정 전: 65535 반환
        // 버그 수정 후: 99999 반환
        assertEquals(Long.valueOf(99999L), map.lastKey());
    }

    /**
     * BUG-V12-001-2: 음수 firstKey() 버그
     */
    @Test
    public void test_bug_firstKey_shouldReturnMinValue() {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug2", Long.class, String.class);

        map.put(Long.MIN_VALUE, "min");
        map.put(-1L, "neg");
        map.put(0L, "zero");
        map.put(1L, "pos");
        map.put(Long.MAX_VALUE, "max");
        store.commit();

        // 버그 수정 전: 0 반환 (양수가 먼저)
        // 버그 수정 후: Long.MIN_VALUE 반환
        assertEquals(Long.valueOf(Long.MIN_VALUE), map.firstKey());
        assertEquals(Long.valueOf(Long.MAX_VALUE), map.lastKey());
    }

    /**
     * BUG-V12-001-3: subMap 범위 오류
     */
    @Test
    public void test_bug_subMap_shouldReturn11Elements() {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug3", Long.class, String.class);

        for (int i = 0; i < 100_000; i++) {
            map.put((long) i, "v" + i);
        }
        store.commit();

        NavigableMap<Long, String> sub = map.subMap(10000L, true, 10010L, true);

        // 버그 수정 전: 3911 반환
        // 버그 수정 후: 11 반환
        assertEquals(11, sub.size());
    }

    /**
     * BUG-V12-001-4: 정렬 순서 검증
     */
    @Test
    public void test_bug_sortOrder_shouldBeSigned() {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug4", Long.class, String.class);

        Long[] values = {Long.MIN_VALUE, -1000L, -1L, 0L, 1L, 1000L, Long.MAX_VALUE};
        for (Long v : values) {
            map.put(v, "v" + v);
        }
        store.commit();

        // Iterator 순서 검증 (signed 순서여야 함)
        Iterator<Long> it = map.keySet().iterator();
        Long prev = it.next();
        while (it.hasNext()) {
            Long curr = it.next();
            assertTrue("Keys should be in signed order: " + prev + " < " + curr,
                      prev < curr);
            prev = curr;
        }
    }

    /**
     * BUG-V12-001-5: ceiling/floor 정확성
     */
    @Test
    public void test_bug_ceilingFloor_shouldWork() {
        openStore();
        NavigableMap<Long, String> map = store.createMap("bug5", Long.class, String.class);

        map.put(-100L, "a");
        map.put(0L, "b");
        map.put(100L, "c");
        store.commit();

        assertEquals(Long.valueOf(-100L), map.ceilingKey(-150L));
        assertEquals(Long.valueOf(0L), map.ceilingKey(-50L));
        assertEquals(Long.valueOf(100L), map.floorKey(150L));
    }
}
```

### 4.2 회귀 테스트

| 테스트 스위트 | 목적 | 통과 기준 |
|--------------|------|----------|
| 단위 테스트 전체 | 기존 기능 보존 | 100% 통과 |
| 통합 테스트 전체 | 대규모 데이터 정렬 | 100% 통과 |
| BTreeByteComparatorBugTest | 버그 수정 확인 | 100% 통과 |

---

## 5. 구현 상세

### 5.1 수정 파일 목록

| 파일 | 수정 내용 |
|------|----------|
| `FxStoreImpl.java` | byteComparator를 코덱 기반으로 변경 |
| `FxReadTransactionImpl.java` | byteComparator를 코덱 기반으로 변경 |
| `FxNavigableMapImpl.java` | getBTreeForCollection() 호출 시 keyCodec 전달 |
| `FxNavigableSetImpl.java` | getBTreeForCollection() 호출 시 keyCodec 전달 |

### 5.2 코드 변경 상세

#### 5.2.1 FxStoreImpl.java

**변경 전:**
```java
// Line 2029-2041
Comparator<byte[]> byteComparator = new Comparator<byte[]>() {
    @Override
    public int compare(byte[] a, byte[] b) {
        int minLen = Math.min(a.length, b.length);
        for (int i = 0; i < minLen; i++) {
            int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (cmp != 0) {
                return cmp;
            }
        }
        return a.length - b.length;
    }
};
```

**변경 후:**
```java
// 메서드 시그니처 변경
public BTree getBTreeForCollection(long collectionId, FxCodec<?> keyCodec) {
    // ...
    Comparator<byte[]> byteComparator = (a, b) -> keyCodec.compareBytes(a, b);
    return new BTree(storage, options.pageSize().bytes(), byteComparator, rootPageId, allocator);
}

// 하위 호환성을 위한 오버로드 (deprecated)
@Deprecated
public BTree getBTreeForCollection(long collectionId) {
    // 기본 lexicographic 비교 사용 (String 등 비수치 타입용)
    return getBTreeForCollection(collectionId, null);
}
```

#### 5.2.2 FxReadTransactionImpl.java

**변경 전:**
```java
// Line 108-117
private BTree createBTree(long collectionId) {
    Comparator<byte[]> byteComparator = (a, b) -> {
        int minLen = Math.min(a.length, b.length);
        for (int i = 0; i < minLen; i++) {
            int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (cmp != 0) return cmp;
        }
        return a.length - b.length;
    };
    return new BTree(...);
}
```

**변경 후:**
```java
private BTree createBTree(long collectionId, FxCodec<?> keyCodec) {
    Comparator<byte[]> byteComparator = (a, b) -> keyCodec.compareBytes(a, b);
    return new BTree(store.getStorage(), store.getPageSize(), byteComparator, 0);
}
```

---

## 6. 위험 및 완화

### 6.1 기존 데이터 호환성

**위험:** 기존 데이터베이스의 BTree가 잘못된 순서로 저장되어 있음

**완화:**
- 기존 데이터는 `compactTo()`로 재구성 시 올바른 순서로 저장됨
- 마이그레이션 가이드 문서 작성

### 6.2 성능 영향

**위험:** 코덱 compareBytes() 호출 오버헤드

**완화:**
- I64Codec.compareBytes()는 간단한 Long.compare() 사용
- 성능 벤치마크로 영향 측정

### 6.3 API 변경

**위험:** getBTreeForCollection() 시그니처 변경

**완화:**
- @Deprecated 오버로드 유지
- 내부 API이므로 외부 영향 없음

---

## 7. 일정

| 단계 | 예상 시간 | 산출물 |
|------|----------|--------|
| 버그 재현 테스트 작성 | 0.5일 | BTreeByteComparatorBugTest.java |
| 테스트 실행 (버그 확인) | 0.1일 | 5개 테스트 실패 확인 |
| 코드 수정 | 0.5일 | FxStoreImpl, FxReadTransactionImpl 수정 |
| 회귀 테스트 | 0.2일 | 전체 테스트 통과 |
| 문서화 | 0.2일 | 완료 보고서, 마이그레이션 가이드 |
| **총계** | **1.5일** | |

---

## 8. 검증 체크리스트

### 8.1 버그 수정 확인

- [ ] `test_bug_lastKey_returns65535_shouldReturn99999` 통과
- [ ] `test_bug_firstKey_shouldReturnMinValue` 통과
- [ ] `test_bug_subMap_shouldReturn11Elements` 통과
- [ ] `test_bug_sortOrder_shouldBeSigned` 통과
- [ ] `test_bug_ceilingFloor_shouldWork` 통과

### 8.2 회귀 테스트

- [ ] 기존 단위 테스트 전체 통과
- [ ] 기존 통합 테스트 전체 통과
- [ ] 커버리지 유지 (93%+ Instruction, 87%+ Branch)

### 8.3 문서화

- [ ] 변경 사항 문서화
- [ ] 마이그레이션 가이드 작성 (필요시)
- [ ] 품질 평가 완료 (7/7 A+)

---

## 9. 참고 자료

### 9.1 관련 코드

- `I64Codec.java:53-57` - 올바른 Long 비교 구현
- `FxCodec.java:41-47` - compareBytes() 인터페이스 정의

### 9.2 관련 문서

- [INTEGRATION-TEST-IMPLEMENTATION-PLAN.md](INTEGRATION-TEST-IMPLEMENTATION-PLAN.md) - 버그 발견 테스트
- [EVALUATION-INTEGRATION-TESTS.md](EVALUATION-INTEGRATION-TESTS.md) - 통합 테스트 평가

---

*작성일: 2025-12-31*
