# Phase 4 테스트 시나리오 - Catalog/State 관리

**Phase**: Phase 4  
**목표**: Catalog와 State 관리 기능 구현 검증  
**작성일**: 2024-12-24  
**Java 버전**: Java 8

[← 목차로 돌아가기](00.index.md)

---

## 테스트 개요

### 테스트 범위
- CatalogEntry 직렬화/역직렬화
- CollectionState 직렬화/역직렬화
- DDL 연산 (create, open, drop, rename, list)
- 예외 처리 (중복, 미존재, 타입 불일치)
- 불변식 검증 (INV-3, INV-4, INV-5)

### 테스트 전략
1. **Unit Test**: CatalogEntry, CollectionState 단독 테스트
2. **Integration Test**: CatalogTree + StateTree 통합
3. **DDL Test**: create → open → drop 시나리오
4. **Edge Case Test**: 경계 조건 및 오류 상황

---

## 시나리오 1: CatalogEntry 직렬화

### 목표
CatalogEntry 인코딩/디코딩 정확성 검증

### 테스트 케이스

#### TC-1.1: 기본 인코딩/디코딩
```
Given: CatalogEntry(name="users", collectionId=100)
When: encode() → decode()
Then: 원본과 동일한 객체 복원
```

**검증 항목**:
- ✅ name 정확히 복원
- ✅ collectionId 정확히 복원

#### TC-1.2: 긴 이름 처리
```
Given: name 길이 255바이트 (최대)
When: encode() → decode()
Then: 정확히 복원
```

#### TC-1.3: 빈 이름 (경계값)
```
Given: name = ""
When: encode()
Then: FxException.INVALID_ARGUMENT 발생
```

#### TC-1.4: 바이트 레벨 검증
```
Given: CatalogEntry(name="test", collectionId=42)
When: encode()
Then: 
  - [0-3]: name 길이 (4바이트 LE) = 0x04000000
  - [4-7]: "test" UTF-8
  - [8-15]: collectionId (8바이트 LE) = 0x2A00000000000000
```

---

## 시나리오 2: CollectionState 직렬화

### 목표
CollectionState 인코딩/디코딩 정확성 검증

### 테스트 케이스

#### TC-2.1: Map 상태 인코딩
```
Given: CollectionState {
  collectionId: 100
  kind: MAP
  keyCodec: I64
  valueCodec: STRING
  rootPageId: 1000
  count: 500
}
When: encode() → decode()
Then: 모든 필드 정확히 복원
```

#### TC-2.2: Set 상태 인코딩
```
Given: CollectionState {
  kind: SET
  valueCodec: null (Set은 값 코덱 없음)
}
When: encode() → decode()
Then: valueCodec = null 유지
```

#### TC-2.3: 바이트 레벨 검증
```
Given: CollectionState(collectionId=1, kind=MAP, ...)
When: encode()
Then:
  - [0-7]: collectionId (8바이트 LE)
  - [8]: kind ordinal (1바이트) = 0x00 (MAP=0)
  - [9-10]: keyCodec.typeOrdinal (2바이트 LE)
  - [11-12]: valueCodec.typeOrdinal or 0xFFFF (null)
  - [13-20]: rootPageId (8바이트 LE)
  - [21-28]: count (8바이트 LE)
```

#### TC-2.4: CodecRef 인코딩
```
Given: CodecRef(type=I64, options="")
When: encode()
Then: [0-1] = I64 ordinal, [2] = 0 (옵션 길이)
```

---

## 시나리오 3: DDL - Create 연산

### 목표
컬렉션 생성 로직 검증

### 테스트 케이스

#### TC-3.1: Map 생성 성공
```
Given: Store가 비어있음
When: createMap("users", I64, STRING)
Then:
  - Catalog에 "users" 엔트리 추가
  - State에 collectionId=1 추가
  - rootPageId 초기화 (0 또는 새 페이지)
  - count = 0
  - nextCollectionId = 2로 증가
```

**검증 항목**:
- ✅ Catalog.find("users") → CatalogEntry 반환
- ✅ State.find(1) → CollectionState 반환
- ✅ INV-3: Catalog의 name 유일성 유지
- ✅ INV-4: State의 collectionId 유일성 유지
- ✅ INV-5: nextCollectionId 증가

#### TC-3.2: Set 생성 성공
```
When: createSet("tags", STRING)
Then:
  - kind = SET
  - valueCodec = null
  - keyCodec = STRING
```

#### TC-3.3: List 생성 성공
```
When: createList("events", BYTES)
Then:
  - kind = LIST
  - keyCodec = null (List는 인덱스 사용)
  - valueCodec = BYTES
```

#### TC-3.4: Deque 생성 성공
```
When: createDeque("queue", I64)
Then:
  - kind = DEQUE
  - keyCodec = I64 (시퀀스 번호)
  - valueCodec = I64 (요소)
```

#### TC-3.5: 중복 이름 생성 실패
```
Given: "users" 컬렉션이 이미 존재
When: createMap("users", ...)
Then: FxException.ALREADY_EXISTS 발생
```

**검증**:
- ✅ 기존 컬렉션 변경 없음
- ✅ nextCollectionId 증가하지 않음

#### TC-3.6: 유효하지 않은 이름
```
When: createMap(null, ...)
Then: FxException.INVALID_ARGUMENT

When: createMap("", ...)
Then: FxException.INVALID_ARGUMENT
```

---

## 시나리오 4: DDL - Open 연산

### 목표
컬렉션 조회 로직 검증

### 테스트 케이스

#### TC-4.1: 존재하는 컬렉션 조회
```
Given: "users" Map(I64, STRING)이 존재
When: openMap("users", I64, STRING)
Then: 
  - FxNavigableMap<Long, String> 반환
  - rootPageId 복원
  - count 복원
```

**검증**:
- ✅ 반환된 Map으로 put/get 가능
- ✅ 코덱이 일치하는지 확인

#### TC-4.2: 존재하지 않는 컬렉션
```
Given: "users" 컬렉션이 없음
When: openMap("users", ...)
Then: FxException.NOT_FOUND 발생
```

#### TC-4.3: 타입 불일치
```
Given: "users"는 SET으로 생성됨
When: openMap("users", ...)
Then: FxException.TYPE_MISMATCH 발생
```

**검증**:
- ✅ CollectionState.kind 확인 필수

#### TC-4.4: 코덱 불일치
```
Given: "users" Map(I64, STRING)
When: openMap("users", STRING, I64) // 키/값 반대
Then: FxException.TYPE_MISMATCH 발생
```

**검증**:
- ✅ keyCodec, valueCodec 검증 필수

---

## 시나리오 5: DDL - Drop 연산

### 목표
컬렉션 삭제 로직 검증

### 테스트 케이스

#### TC-5.1: 정상 삭제
```
Given: "users" 컬렉션 존재
When: drop("users")
Then:
  - Catalog에서 "users" 제거
  - State에서 해당 collectionId 제거
  - nextCollectionId는 변경 없음 (재사용 금지)
```

**검증**:
- ✅ open("users") → NOT_FOUND
- ✅ INV-5: collectionId 재사용 안 됨

#### TC-5.2: collectionId 재사용 금지
```
Given: "users"(id=1) drop
When: createMap("admins", ...)
Then: 
  - 새 컬렉션 id = 2 (1이 아님)
  - nextCollectionId = 3
```

#### TC-5.3: 존재하지 않는 컬렉션 삭제
```
When: drop("nonexistent")
Then: FxException.NOT_FOUND 발생
```

#### TC-5.4: 삭제 후 데이터는 dead
```
Given: "users"에 1000개 키 삽입
When: drop("users")
Then:
  - 파일 크기 변하지 않음 (컴팩션 전까지)
  - dead ratio 증가
```

---

## 시나리오 6: DDL - Rename 연산

### 목표
컬렉션 이름 변경 로직 검증

### 테스트 케이스

#### TC-6.1: 정상 이름 변경
```
Given: "users" Map 존재
When: rename("users", "accounts")
Then:
  - Catalog에서 "users" 제거, "accounts" 추가
  - State는 변경 없음 (같은 collectionId)
  - 데이터는 그대로 유지
```

**검증**:
- ✅ open("users") → NOT_FOUND
- ✅ open("accounts") → 성공, 데이터 동일
- ✅ collectionId 변경 없음

#### TC-6.2: 새 이름 중복
```
Given: "users", "admins" 둘 다 존재
When: rename("users", "admins")
Then: FxException.ALREADY_EXISTS 발생
```

#### TC-6.3: 존재하지 않는 원본 이름
```
When: rename("nonexistent", "new")
Then: FxException.NOT_FOUND 발생
```

#### TC-6.4: 자기 자신으로 rename
```
Given: "users" 존재
When: rename("users", "users")
Then: 성공 (아무 변경 없음) 또는 ALREADY_EXISTS
```

**구현 선택**: ALREADY_EXISTS로 거부 권장

---

## 시나리오 7: DDL - List 연산

### 목표
전체 컬렉션 목록 조회 검증

### 테스트 케이스

#### TC-7.1: 빈 Store
```
Given: 컬렉션 없음
When: list()
Then: 빈 리스트 반환
```

#### TC-7.2: 여러 컬렉션
```
Given: 
  - "users" (Map)
  - "tags" (Set)
  - "events" (List)
When: list()
Then: ["events", "tags", "users"] (정렬된 순서)
```

**검증**:
- ✅ Catalog 전체 스캔
- ✅ 사전 순 정렬

#### TC-7.3: drop 후 list
```
Given: "users", "admins" 존재
When: drop("users"), list()
Then: ["admins"]만 반환
```

---

## 시나리오 8: CatalogTree 및 StateTree 통합

### 목표
Catalog와 State의 BTree 기반 저장 검증

### 테스트 케이스

#### TC-8.1: Catalog → BTree 매핑
```
Given: CatalogTree rootPageId = 10
When: 
  - insert("users", CatalogEntry(id=1))
  - insert("admins", CatalogEntry(id=2))
Then:
  - BTree.find("users") → CatalogEntry(id=1)
  - BTree.find("admins") → CatalogEntry(id=2)
```

**검증**:
- ✅ name을 키로 사용 (StringCodec)
- ✅ CatalogEntry를 값으로 직렬화

#### TC-8.2: State → BTree 매핑
```
Given: StateTree rootPageId = 20
When:
  - insert(1, CollectionState(...))
  - insert(2, CollectionState(...))
Then:
  - BTree.find(1) → CollectionState
  - BTree.find(2) → CollectionState
```

**검증**:
- ✅ collectionId를 키로 사용 (I64Codec)
- ✅ CollectionState를 값으로 직렬화

#### TC-8.3: Catalog/State 동기화
```
When: createMap("users", ...)
Then:
  - Catalog.insert("users", entry)
  - State.insert(entry.collectionId, state)
  - 두 트리 모두 커밋
```

**검증**:
- ✅ 원자성: 둘 다 성공 또는 둘 다 실패
- ✅ CommitHeader에 catalogRootPageId, stateRootPageId 반영

---

## 시나리오 9: 불변식 검증

### 목표
INV-3, INV-4, INV-5 유지 확인

### 테스트 케이스

#### TC-9.1: INV-3 - Catalog name 유일성
```
Given: Catalog에 ["users", "admins"]
When: 수동으로 BTree에 중복 "users" 삽입 시도
Then: 거부되어야 함
```

**구현 제약**:
- create 전에 Catalog.find(name) != null 확인

#### TC-9.2: INV-4 - State collectionId 유일성
```
Given: State에 [1, 2]
When: 수동으로 중복 collectionId 1 삽입
Then: 거부되어야 함
```

#### TC-9.3: INV-5 - collectionId 재사용 금지
```
Given: nextCollectionId = 5
When: 
  - createMap("a") → id=5, next=6
  - createMap("b") → id=6, next=7
  - drop("a")
  - createMap("c") → id=7, next=8
Then: id=5는 절대 재사용되지 않음
```

**검증 방법**:
- ✅ nextCollectionId는 항상 증가만
- ✅ CommitHeader에 nextCollectionId 기록

---

## 시나리오 10: BATCH 모드와 DDL

### 목표
BATCH 커밋 모드에서 DDL 동작 검증

### 테스트 케이스

#### TC-10.1: AUTO 모드 DDL
```
Given: CommitMode = AUTO
When: createMap("users", ...)
Then: 즉시 CommitHeader에 반영
```

#### TC-10.2: BATCH 모드 DDL - commit
```
Given: CommitMode = BATCH
When:
  - createMap("users", ...)
  - createSet("tags", ...)
  - commit()
Then: 
  - commit 전까지 pending
  - commit 후 Catalog/State 반영
```

#### TC-10.3: BATCH 모드 DDL - rollback
```
Given: CommitMode = BATCH
When:
  - createMap("users", ...)
  - rollback()
Then:
  - "users" 생성 취소
  - nextCollectionId 원복
```

**구현 복잡도**:
- Phase 4에서는 **AUTO 모드만 구현** (단순화)
- BATCH 모드 DDL은 **Phase 5에서 구현**

---

## 시나리오 11: 예외 처리

### 목표
모든 오류 상황에서 적절한 예외 발생

### 테스트 케이스

#### TC-11.1: FxErrorCode 매핑
```
| 상황 | ErrorCode |
|------|-----------|
| 중복 이름 create | ALREADY_EXISTS |
| 미존재 이름 open | NOT_FOUND |
| 미존재 이름 drop | NOT_FOUND |
| rename 대상 중복 | ALREADY_EXISTS |
| 타입 불일치 open | TYPE_MISMATCH |
| null/빈 이름 | INVALID_ARGUMENT |
```

#### TC-11.2: 예외 메시지 품질
```
When: createMap("users", ...) 중복
Then: 
  - message = "Collection 'users' already exists"
  - code = ALREADY_EXISTS
```

---

## 시나리오 12: 성능 및 스트레스 테스트

### 목표
대량 컬렉션 처리 성능 검증

### 테스트 케이스

#### TC-12.1: 1000개 컬렉션 생성
```
When: for i in 0..999: createMap(f"col{i}", I64, STRING)
Then:
  - 모든 생성 성공
  - list() 1000개 반환
  - Catalog/State BTree 높이 적절 (3-4 레벨)
```

#### TC-12.2: 이름 검색 성능
```
Given: 10,000개 컬렉션
When: open("col5000")
Then: O(log N) 시간 (BTree 효율)
```

---

## 테스트 커버리지 목표

### 코드 커버리지
- **CatalogEntry**: 100% (단순 DTO)
- **CollectionState**: 100% (단순 DTO)
- **CatalogTree**: 95% (BTree 재사용)
- **StateTree**: 95% (BTree 재사용)
- **DDL 로직**: 100% (핵심 기능)

### 브랜치 커버리지
- 모든 if/else 분기 테스트
- 예외 경로 모두 검증

---

## 회귀 테스트 항목

Phase 4 완료 후 다음 회귀 테스트 실행:

1. ✅ **Phase 0**: Superblock, CommitHeader, Allocator
2. ✅ **Phase 1**: Codec 시스템
3. ✅ **Phase 2**: Storage, Page 관리
4. ✅ **Phase 3**: BTree 삽입/삭제/조회
5. ✅ **Phase 4 신규**: Catalog/State 모든 테스트

**통과 기준**: 모든 기존 + 신규 테스트 100% 통과

---

## 테스트 자동화

### Gradle 테스트 태스크
```bash
./gradlew test --tests "*CatalogEntryTest"
./gradlew test --tests "*CollectionStateTest"
./gradlew test --tests "*DDLTest"
./gradlew test --tests "*Phase4*"
```

### JaCoCo 커버리지
```bash
./gradlew jacocoTestReport
# build/reports/jacoco/test/html/index.html
```

---

## 품질 기준 체크리스트

Phase 4 완료 후 [03.quality-criteria.md](03.quality-criteria.md) 7가지 기준 평가:

1. ✅ **Plan-Code 정합성**: 01.api.md의 DDL 명세 구현
2. ✅ **SOLID 원칙**: CatalogEntry/State는 DTO (SRP), BTree 재사용 (OCP)
3. ✅ **테스트 커버리지**: 95%+ 달성
4. ✅ **코드 가독성**: 명확한 메서드 이름, 적절한 주석
5. ✅ **예외 처리**: 모든 오류 케이스 FxException 발생
6. ✅ **성능**: O(log N) Catalog 검색
7. ✅ **문서화**: Javadoc, 테스트 시나리오 완비

---

## 부록: 테스트 데이터 예시

### 샘플 CatalogEntry
```java
CatalogEntry entry1 = new CatalogEntry("users", 100L);
CatalogEntry entry2 = new CatalogEntry("admins", 101L);
```

### 샘플 CollectionState
```java
CollectionState state1 = new CollectionState(
    100L,                      // collectionId
    CollectionKind.MAP,        // kind
    CodecRef.of(FxType.I64),   // keyCodec
    CodecRef.of(FxType.STRING),// valueCodec
    1000L,                     // rootPageId
    500L                       // count
);
```

### 샘플 DDL 흐름
```java
// Create
store.createMap("users", I64, STRING);
// Open
NavigableMap<Long, String> users = store.openMap("users", I64, STRING);
users.put(1L, "Alice");
// Rename
store.rename("users", "accounts");
// Drop
store.drop("accounts");
```

---

**문서 작성일**: 2024-12-24  
**작성자**: Phase 4 Implementation Team  
**검토 상태**: ✅ 승인됨

[← 목차로 돌아가기](00.index.md)
