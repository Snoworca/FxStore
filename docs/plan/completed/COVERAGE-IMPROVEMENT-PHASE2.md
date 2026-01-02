# FxStore 커버리지 개선 계획 Phase 2 (v0.9)

> **문서 버전:** 2.0 (A+ 개선)
> **작성일:** 2025-12-29
> **목표:** 저커버리지 클래스 개선 (80% 미만 → 90%+)
> **현재 상태:** 전체 92% instruction, 85% branch

---

## 목차

1. [개요](#개요)
2. [현황 분석](#현황-분석)
3. [미커버 라인 상세](#미커버-라인-상세)
4. [P1 작업 계획 (높음)](#p1-작업-계획-높음)
5. [P2 작업 계획 (중간)](#p2-작업-계획-중간)
6. [P3 작업 계획 (낮음)](#p3-작업-계획-낮음)
7. [테스트 시나리오](#테스트-시나리오)
8. [SOLID 원칙 적용 가이드](#solid-원칙-적용-가이드)
9. [에러 테스트 가이드](#에러-테스트-가이드)
10. [예상 결과](#예상-결과)
11. [실행 순서](#실행-순서)
12. [실패 대응 절차](#실패-대응-절차)
13. [품질 기준](#품질-기준)
14. [검증 방법](#검증-방법)

---

## 개요

### 목적
이전 P1/P2/P3 작업(COVERAGE-IMPROVEMENT-P1P2P3.md)에서 View 클래스와 CacheStats 커버리지를 개선했습니다.
이번 Phase 2에서는 **남은 저커버리지 클래스**를 대상으로 추가 개선을 수행합니다.

### 대상 클래스

| 우선순위 | 클래스 | 현재 커버리지 | 목표 | 패키지 |
|----------|--------|---------------|------|--------|
| **P1** | FxNavigableSetImpl$1 | 60% | 90%+ | collection |
| **P1** | FileStorage | 72% (83% branch) | 85%+ | storage |
| **P1** | HeadMapView | 74% (68% branch) | 85%+ | collection |
| **P1** | TailMapView | 76% (68% branch) | 85%+ | collection |
| **P2** | FxStoreImpl | 82% (68% branch) | 90%+ | core |
| **P2** | KeySetView | 83% (75% branch) | 90%+ | collection |
| **P2** | FxReadTransactionImpl | 85% (75% branch) | 90%+ | core |
| **P3** | SubMapView | 78% (82% branch) | 85%+ | collection |
| **P3** | DescendingSetView | 84% | 90%+ | collection |
| **P3** | StoreSnapshot | 99% (61% branch) | 85%+ branch | core |

### JaCoCo 리포트 링크

- **전체 리포트**: `build/reports/jacoco/test/html/index.html`
- **core 패키지**: `build/reports/jacoco/test/html/com.fxstore.core/index.html`
- **collection 패키지**: `build/reports/jacoco/test/html/com.fxstore.collection/index.html`
- **storage 패키지**: `build/reports/jacoco/test/html/com.fxstore.storage/index.html`

---

## 현황 분석

### 패키지별 현황

| 패키지 | Instruction | Branch | 주요 미흡 클래스 |
|--------|-------------|--------|------------------|
| com.fxstore.core | 88% | 76% | FxStoreImpl (82%), FxReadTransactionImpl (85%), StoreSnapshot (61% branch) |
| com.fxstore.collection | 90% | 84% | HeadMapView (74%), TailMapView (76%), SubMapView (78%) |
| com.fxstore.storage | 89% | 90% | FileStorage (72%) |

### 미커버 영역 분석

#### 1. FxNavigableSetImpl$1 (60%)
- **위치**: `FxNavigableSetImpl.java` 익명 FxCodec 클래스
- **미커버 원인**: 코덱의 encode/decode 메서드 일부 경로 미테스트
- **개선 방안**: NavigableSet의 다양한 타입 데이터로 코덱 테스트

#### 2. FileStorage (72%)
- **위치**: `com.fxstore.storage.FileStorage`
- **미커버 원인**:
  - IOException 핸들링 경로
  - 파일 확장/축소 에러 케이스
  - close() 후 재접근 에러
- **개선 방안**: Mock을 사용한 에러 시나리오 테스트

#### 3. HeadMapView/TailMapView (74-76%)
- **위치**: `FxNavigableMapImpl` 내부 클래스
- **미커버 원인**:
  - 경계 조건 테스트 부족 (inclusive/exclusive)
  - navigableKeySet(), descendingKeySet() 미호출
  - pollFirst/pollLast UnsupportedOperation 경로
- **개선 방안**: 경계 조건 및 모든 NavigableMap 메서드 테스트

#### 4. FxStoreImpl (82%, 68% branch)
- **위치**: `com.fxstore.core.FxStoreImpl`
- **미커버 원인**:
  - 다양한 옵션 조합 미테스트
  - 에러 복구 경로
  - 동시성 관련 분기
- **개선 방안**: FxOptions 다양한 조합, 에러 시나리오 테스트

#### 5. FxReadTransactionImpl (85%, 75% branch)
- **위치**: `com.fxstore.core.FxReadTransactionImpl`
- **미커버 원인**:
  - 닫힌 트랜잭션 접근 에러
  - 다중 컬렉션 읽기 시나리오
- **개선 방안**: 트랜잭션 라이프사이클 테스트

#### 6. StoreSnapshot (99%, 61% branch)
- **위치**: `com.fxstore.core.StoreSnapshot`
- **미커버 원인**: 조건 분기 일부 미테스트
- **개선 방안**: 다양한 스냅샷 상태 테스트

---

## 미커버 라인 상세

### FileStorage 미커버 메서드

| 메서드 | 라인 | 미커버 원인 | 테스트 방안 |
|--------|------|-------------|-------------|
| `read(int pageNo)` | 예외 분기 | IOException 발생 시 | Mock RandomAccessFile |
| `write(int pageNo, byte[] data)` | 예외 분기 | IOException 발생 시 | Mock RandomAccessFile |
| `close()` | 중복 close | 이미 닫힌 상태 체크 | close() 두 번 호출 |
| `ensureCapacity(int pages)` | 확장 실패 | 디스크 공간 부족 | Mock 사용 |

### HeadMapView/TailMapView 미커버 메서드

| 메서드 | 미커버 원인 | 테스트 방안 |
|--------|-------------|-------------|
| `navigableKeySet()` | 미호출 | 직접 호출 후 순회 |
| `descendingKeySet()` | 미호출 | 직접 호출 후 순회 |
| `pollFirstEntry()` | UOE 경로 | expected=UOE 테스트 |
| `pollLastEntry()` | UOE 경로 | expected=UOE 테스트 |
| `firstEntry()` | 빈 뷰 케이스 | 빈 뷰에서 호출 |
| `lastEntry()` | 빈 뷰 케이스 | 빈 뷰에서 호출 |

### FxStoreImpl 미커버 분기

| 분기 조건 | 라인 | 테스트 방안 |
|-----------|------|-------------|
| `options.pageSize != DEFAULT` | - | 다른 pageSize로 생성 |
| `options.cacheSize != DEFAULT` | - | 다른 cacheSize로 생성 |
| `catalog.contains(name)` | - | 중복 이름 생성 시도 |
| `file already open` | - | 같은 파일 두 번 open |

### StoreSnapshot 미커버 분기

| 분기 조건 | 테스트 방안 |
|-----------|-------------|
| `catalog == null` | null catalog로 스냅샷 생성 |
| `version == 0` | 초기 버전 스냅샷 |
| `equals(other)` | 다양한 객체와 비교 |
| `hashCode()` | 해시코드 일관성 검증 |

---

## P1 작업 계획 (높음)

### P1-1: FxNavigableSetImpl$1 커버리지 개선 (60% → 90%+)

**테스트 파일**: `src/test/java/com/fxstore/collection/NavigableSetCodecTest.java`

**테스트 시나리오**:
1. 다양한 타입의 NavigableSet 생성 및 직렬화
   - Long, Integer, String, Double 등
2. 빈 Set의 코덱 처리
3. 대용량 Set의 코덱 처리
4. null 요소 처리 (지원 여부에 따라)
5. 경계값 테스트 (Long.MIN_VALUE, Long.MAX_VALUE 등)

**예상 테스트 수**: 8-10개

### P1-2: FileStorage 커버리지 개선 (72% → 85%+)

**테스트 파일**: `src/test/java/com/fxstore/storage/FileStorageErrorTest.java`

**테스트 시나리오**:
1. 읽기 전용 파일 접근 시 에러
2. 디스크 공간 부족 시뮬레이션 (Mock)
3. close() 후 read/write 시도
4. 잘못된 페이지 번호 접근
5. 파일 확장 실패 케이스
6. 동시 접근 시 에러 (이미 열린 파일)

**예상 테스트 수**: 10-12개

### P1-3: HeadMapView/TailMapView 커버리지 개선 (74-76% → 85%+)

**테스트 파일**: `src/test/java/com/fxstore/collection/MapViewBoundaryTest.java`

**테스트 시나리오**:
1. inclusive/exclusive 경계 조합 (4가지)
2. navigableKeySet() 반환 및 순회
3. descendingKeySet() 반환 및 순회
4. firstEntry(), lastEntry() 경계 케이스
5. pollFirstEntry(), pollLastEntry() UOE 테스트
6. 빈 뷰에서의 first(), last() 호출
7. 단일 요소 뷰 테스트
8. containsKey(), containsValue() 경계 테스트

**예상 테스트 수**: 20-24개

---

## P2 작업 계획 (중간)

### P2-1: FxStoreImpl 커버리지 개선 (82% → 90%+, 68% → 85%+ branch)

**테스트 파일**: `src/test/java/com/fxstore/core/FxStoreOptionsTest.java`

**테스트 시나리오**:
1. 다양한 FxOptions 조합
   - pageSize 변경
   - cacheSize 변경
   - allowCodecUpgrade 활성화
2. 이미 열린 스토어 재오픈 시도
3. 손상된 파일 복구
4. 빈 스토어 동작
5. 다중 컬렉션 생성/삭제
6. 동일 이름 컬렉션 중복 생성

**예상 테스트 수**: 15-18개

### P2-2: KeySetView 커버리지 개선 (83% → 90%+)

**테스트 파일**: `src/test/java/com/fxstore/collection/KeySetViewTest.java`

**테스트 시나리오**:
1. iterator() 순회 및 remove
2. descendingIterator() 순회
3. subSet(), headSet(), tailSet() 호출
4. contains() 경계값
5. toArray() 다양한 형태
6. retainAll(), removeAll() (UOE 예상)

**예상 테스트 수**: 12-15개

### P2-3: FxReadTransactionImpl 커버리지 개선 (85% → 90%+)

**테스트 파일**: `src/test/java/com/fxstore/core/ReadTransactionLifecycleTest.java`

**테스트 시나리오**:
1. 닫힌 트랜잭션에서 읽기 시도
2. 다중 컬렉션 일관성 읽기
3. 트랜잭션 중 store 변경 후 읽기
4. 중첩 트랜잭션 (지원 여부)
5. 자동 close (try-with-resources)
6. getSnapshot() 반환값 검증

**예상 테스트 수**: 10-12개

---

## P3 작업 계획 (낮음)

### P3-1: SubMapView 커버리지 개선 (78% → 85%+)

**테스트 파일**: `src/test/java/com/fxstore/collection/SubMapViewTest.java`

**테스트 시나리오**:
1. 4가지 경계 조합 (fromInclusive, toInclusive)
2. 중첩 subMap 호출
3. 경계 밖 put 시도
4. navigableKeySet() 내 subSet
5. descendingMap() 호출

**예상 테스트 수**: 10-12개

### P3-2: DescendingSetView 커버리지 개선 (84% → 90%+)

**테스트 파일**: `src/test/java/com/fxstore/collection/DescendingSetViewTest.java`

**테스트 시나리오**:
1. descendingSet().descendingSet() (원래 순서 복원)
2. first(), last() 역순 검증
3. higher(), lower() 역순 동작
4. subSet(), headSet(), tailSet() 역순
5. iterator vs descendingIterator

**예상 테스트 수**: 8-10개

### P3-3: StoreSnapshot Branch 개선 (61% → 85%+ branch)

**테스트 파일**: `src/test/java/com/fxstore/core/StoreSnapshotBranchTest.java`

**테스트 시나리오**:
1. 빈 스냅샷 상태
2. 여러 버전의 스냅샷
3. 스냅샷 비교 연산
4. null 체크 분기
5. 동등성 검사 분기

**예상 테스트 수**: 8-10개

---

## 테스트 시나리오

### P1 테스트 시나리오 상세

#### NavigableSetCodecTest
```java
@Test public void codec_shouldEncodeDecodeLongSet()
@Test public void codec_shouldEncodeDecodeEmptySet()
@Test public void codec_shouldHandleBoundaryValues()
@Test public void codec_shouldHandleLargeSet()
```

#### FileStorageErrorTest
```java
@Test(expected = IllegalStateException.class)
public void readAfterClose_shouldThrow()

@Test(expected = IllegalStateException.class)
public void writeAfterClose_shouldThrow()

@Test public void invalidPageNumber_shouldThrow()
@Test public void concurrentOpen_shouldHandleGracefully()
```

#### MapViewBoundaryTest
```java
@Test public void headMap_inclusive_shouldIncludeBoundary()
@Test public void headMap_exclusive_shouldExcludeBoundary()
@Test public void tailMap_inclusive_shouldIncludeBoundary()
@Test public void tailMap_exclusive_shouldExcludeBoundary()
@Test public void headMap_navigableKeySet_shouldWork()
@Test public void tailMap_descendingKeySet_shouldWork()
@Test(expected = NoSuchElementException.class)
public void emptyView_first_shouldThrow()
```

---

## SOLID 원칙 적용 가이드

테스트 코드 작성 시 SOLID 원칙을 준수합니다.

### S - 단일 책임 원칙 (Single Responsibility)

**한 테스트 메서드는 하나의 시나리오만 검증합니다.**

```java
// 좋은 예: 한 테스트 = 한 시나리오
@Test
public void headMap_inclusive_shouldIncludeBoundary() {
    // Given
    NavigableMap<Long, String> map = createTestMap();

    // When
    NavigableMap<Long, String> head = map.headMap(30L, true);

    // Then
    assertTrue(head.containsKey(30L));
}

// 나쁜 예: 여러 시나리오를 하나의 테스트에
@Test
public void headMap_shouldWorkCorrectly() {
    // inclusive 테스트
    // exclusive 테스트
    // 빈 맵 테스트
    // ... 너무 많은 검증
}
```

### O - 개방-폐쇄 원칙 (Open/Closed)

**테스트 유틸리티는 확장에 열려있고, 수정에 닫혀있습니다.**

```java
// 테스트 데이터 생성 헬퍼 (확장 가능)
protected NavigableMap<Long, String> createTestMap() {
    NavigableMap<Long, String> map = store.createNavigableMap("test", Long.class, String.class);
    map.put(10L, "A");
    map.put(20L, "B");
    map.put(30L, "C");
    return map;
}
```

### L - 리스코프 치환 원칙 (Liskov Substitution)

**NavigableMap 인터페이스 계약을 검증합니다.**

```java
@Test
public void navigableMapContract_headMap_shouldBehaveAsNavigableMap() {
    NavigableMap<Long, String> head = map.headMap(30L, true);
    // NavigableMap 인터페이스의 모든 메서드가 정상 동작해야 함
    assertNotNull(head.firstEntry());
    assertNotNull(head.lastEntry());
    assertNotNull(head.navigableKeySet());
}
```

### I - 인터페이스 분리 원칙 (Interface Segregation)

**테스트 클래스를 기능별로 분리합니다.**

```
MapViewBoundaryTest.java      - 경계 조건 테스트
MapViewNavigationTest.java    - 탐색 메서드 테스트
MapViewModificationTest.java  - 수정 메서드 테스트
```

### D - 의존성 역전 원칙 (Dependency Inversion)

**테스트에서 구체 클래스가 아닌 인터페이스에 의존합니다.**

```java
// 좋은 예: 인터페이스에 의존
private NavigableMap<Long, String> map;  // 인터페이스

// 나쁜 예: 구체 클래스에 의존
private FxNavigableMapImpl<Long, String> map;  // 구체 클래스
```

---

## 에러 테스트 가이드

### IOException 시뮬레이션

FileStorage의 IOException 경로를 테스트하려면 다음 방법을 사용합니다:

#### 방법 1: 읽기 전용 파일

```java
@Test(expected = IOException.class)
public void write_toReadOnlyFile_shouldThrowIOException() throws Exception {
    // Given: 읽기 전용 파일 생성
    File readOnlyFile = tempFolder.newFile("readonly.db");
    readOnlyFile.setReadOnly();

    // When: 쓰기 시도
    try (FileStorage storage = new FileStorage(readOnlyFile.toPath())) {
        storage.write(0, new byte[4096]);
    }
}
```

#### 방법 2: 파일 시스템 권한

```java
@Test
public void open_nonExistentDirectory_shouldThrowIOException() {
    // Given: 존재하지 않는 디렉토리
    Path invalidPath = Paths.get("/nonexistent/path/file.db");

    // When & Then
    try {
        new FileStorage(invalidPath);
        fail("Expected IOException");
    } catch (IOException e) {
        assertTrue(e.getMessage().contains("No such file"));
    }
}
```

#### 방법 3: close 후 접근

```java
@Test(expected = IllegalStateException.class)
public void read_afterClose_shouldThrowIllegalStateException() throws Exception {
    // Given
    FileStorage storage = new FileStorage(tempFile.toPath());
    storage.close();

    // When: 닫힌 후 읽기 시도
    storage.read(0);  // IllegalStateException 예상
}
```

### 트랜잭션 에러 테스트

```java
@Test(expected = IllegalStateException.class)
public void getMap_afterClose_shouldThrow() {
    // Given
    FxReadTransaction tx = store.beginReadTransaction();
    tx.close();

    // When: 닫힌 후 접근
    tx.getMap("test");  // IllegalStateException 예상
}
```

---

## 예상 결과

### 커버리지 목표

| 우선순위 | 클래스 | 현재 | 목표 | 예상 테스트 수 |
|----------|--------|------|------|----------------|
| P1 | FxNavigableSetImpl$1 | 60% | 90%+ | 8-10 |
| P1 | FileStorage | 72% | 85%+ | 10-12 |
| P1 | HeadMapView | 74% | 85%+ | 10-12 |
| P1 | TailMapView | 76% | 85%+ | 10-12 |
| P2 | FxStoreImpl | 82% | 90%+ | 15-18 |
| P2 | KeySetView | 83% | 90%+ | 12-15 |
| P2 | FxReadTransactionImpl | 85% | 90%+ | 10-12 |
| P3 | SubMapView | 78% | 85%+ | 10-12 |
| P3 | DescendingSetView | 84% | 90%+ | 8-10 |
| P3 | StoreSnapshot | 61% branch | 85%+ branch | 8-10 |
| **합계** | - | - | - | **~100개** |

### 전체 커버리지 예상

- **현재**: 92% instruction, 85% branch
- **목표**: 95%+ instruction, 90%+ branch

---

## 실행 순서

### 의존성 순서

```
P1-1 (FxNavigableSetImpl$1) ─┐
                             ├─→ P2-1 (FxStoreImpl) ─┐
P1-2 (FileStorage) ──────────┤                       ├─→ 최종 검증
                             │                       │
P1-3 (HeadMapView/TailMapView)├─→ P2-2 (KeySetView)──┤
                             │                       │
                             └─→ P2-3 (FxReadTransaction)
                                       │
                                       ├─→ P3-1 (SubMapView)
                                       ├─→ P3-2 (DescendingSetView)
                                       └─→ P3-3 (StoreSnapshot)
```

### 1단계: P1 작업 (예상 시간: 2-3시간)
1. NavigableSetCodecTest.java 작성 및 실행
2. FileStorageErrorTest.java 작성 및 실행
3. MapViewBoundaryTest.java 작성 및 실행
4. 커버리지 확인 및 부족분 보완

### 2단계: P2 작업 (예상 시간: 2-3시간)
1. FxStoreOptionsTest.java 작성 및 실행
2. KeySetViewTest.java 작성 및 실행
3. ReadTransactionLifecycleTest.java 작성 및 실행
4. 커버리지 확인 및 부족분 보완

### 3단계: P3 작업 (예상 시간: 1-2시간)
1. SubMapViewTest.java 작성 및 실행
2. DescendingSetViewTest.java 작성 및 실행
3. StoreSnapshotBranchTest.java 작성 및 실행
4. 최종 커버리지 확인

### 4단계: 검증 (예상 시간: 30분)
1. 전체 테스트 실행
2. JaCoCo 리포트 생성
3. 목표 달성 확인
4. 텔레그램 결과 보고

---

## 실패 대응 절차

### 테스트 실패 시

```
테스트 실패
    │
    ▼
실패 원인 분석
    │
    ├─→ 테스트 코드 오류 ─→ 테스트 코드 수정 ─→ 재실행
    │
    ├─→ 프로덕션 버그 발견 ─→ 버그 기록 ─→ 별도 이슈로 처리
    │
    └─→ 설계 문제 ─→ 개발자 확인 필요 ─→ 작업 중단 및 보고
```

### 커버리지 미달 시

```
커버리지 목표 미달
    │
    ▼
미커버 영역 분석 (JaCoCo 리포트)
    │
    ├─→ 테스트 추가 가능 ─→ 테스트 추가 ─→ 재검증
    │
    ├─→ 도달 불가능 코드 ─→ Dead code 제거 검토
    │
    └─→ 에러 경로 ─→ Mock 사용 테스트 추가
```

### 롤백 전략

1. **Git 브랜치 사용**: 각 P1/P2/P3 작업을 별도 커밋으로 관리
2. **테스트 독립성 유지**: 한 테스트 실패가 다른 테스트에 영향 없음
3. **단계별 검증**: 각 단계 완료 후 전체 테스트 실행

### 디버깅 가이드

```bash
# 특정 테스트만 실행
./gradlew test --tests "FileStorageErrorTest"

# 실패한 테스트 상세 로그
./gradlew test --info

# 커버리지 리포트 생성
./gradlew jacocoTestReport
```

---

## 품질 기준

### 테스트 품질 기준

| 기준 | 요구사항 | 가중치 |
|------|----------|--------|
| 커버리지 달성 | 목표 커버리지 달성 | 30% |
| 테스트 독립성 | 각 테스트가 독립적으로 실행 가능 | 15% |
| 명확한 네이밍 | 테스트 메서드명이 시나리오를 설명 | 15% |
| Given-When-Then | 테스트 구조가 명확 | 15% |
| 에러 메시지 | 실패 시 원인 파악 용이 | 10% |
| 문서화 | Javadoc 또는 주석 제공 | 15% |

### 등급 기준

| 등급 | 점수 | 설명 |
|------|------|------|
| A+ | 95-100 | 완벽 |
| A | 90-94 | 우수 |
| B+ | 85-89 | 양호 |
| B | 80-84 | 보통 |
| C | 70-79 | 미흡 |
| F | 0-69 | 불합격 |

---

## 검증 방법

### 1. 테스트 실행
```bash
./gradlew test
```

### 2. 커버리지 리포트 생성
```bash
./gradlew jacocoTestReport
```

### 3. 커버리지 확인
- 리포트 위치: `build/reports/jacoco/test/html/index.html`
- 각 클래스별 목표 달성 여부 확인

### 4. 결과 보고
- 텔레그램으로 결과 전송
- docs/plan/EVALUATION-COVERAGE-PHASE2.md 업데이트

---

## 참고 문서

- [COVERAGE-IMPROVEMENT-P1P2P3.md](COVERAGE-IMPROVEMENT-P1P2P3.md) - 이전 커버리지 개선 계획
- [00.index.md](00.index.md) - 계획 문서 인덱스
- [03.quality-criteria.md](03.quality-criteria.md) - 품질 평가 기준
- [05.solid-compliance.md](05.solid-compliance.md) - SOLID 원칙 준수 가이드

---

*작성일: 2025-12-29*
*버전: 2.0 (A+ 개선)*

## 개선 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2025-12-29 | 초안 작성 |
| 2.0 | 2025-12-29 | A+ 달성을 위한 개선 적용 |

### v2.0 개선 사항
1. **미커버 라인 상세 섹션 추가** - 각 클래스별 미커버 메서드/라인 명시
2. **SOLID 원칙 적용 가이드 추가** - 테스트 코드 작성 시 SOLID 준수
3. **에러 테스트 가이드 추가** - IOException 시뮬레이션 방법
4. **실패 대응 절차 추가** - 테스트 실패/커버리지 미달 시 대응
5. **의존성 순서 다이어그램 추가** - 작업 순서 시각화
6. **JaCoCo 리포트 링크 추가** - 각 패키지별 리포트 경로
