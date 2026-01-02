# BTree Insert 테스트 실패 문제 해결 보고서

**작성일:** 2024-12-24  
**문제 유형:** 페이지 ID 할당 오류  
**심각도:** Critical  
**상태:** ✅ 해결 완료

---

## 1. 문제 증상

### 1.1 실패한 테스트

```
com.fxstore.btree.BTreeInsertTest > testInsertDuplicateKey FAILED
    java.lang.NullPointerException at BTreeInsertTest.java:140

com.fxstore.btree.BTreeInsertTest > testInsertWithSpace FAILED
    java.lang.AssertionError at BTreeInsertTest.java:113

com.fxstore.btree.BTreeInsertTest > testInsertIntoEmptyTree FAILED
    java.lang.AssertionError at BTreeInsertTest.java:64

com.fxstore.btree.BTreeInsertTest > testInsertMultipleKeysOrder FAILED
    java.lang.NullPointerException at BTreeInsertTest.java:184
```

### 1.2 구체적 오류 내용

**testInsertIntoEmptyTree:**
```
java.lang.AssertionError: 삽입 후 루트 페이지 ID가 할당되어야 함. Actual: 0
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.failEquals(Assert.java:187)
	at org.junit.Assert.assertNotEquals(Assert.java:201)
	at com.fxstore.btree.BTreeInsertTest.testInsertIntoEmptyTree(BTreeInsertTest.java:64)
```

- 빈 트리에 키-값을 삽입한 후에도 `rootPageId`가 0으로 남아있음
- 이는 페이지가 제대로 할당되지 않았음을 의미

---

## 2. 원인 분석

### 2.1 문제 발생 코드

**BTree.java - `allocatePageId()` 메서드:**

```java
private long allocatePageId() {
    long currentSize = storage.size();    // 초기: 0
    long newPageId = currentSize / pageSize;  // 0 / 4096 = 0 ❌
    
    storage.extend(currentSize + pageSize);   // extend(4096)
    
    return newPageId;  // 0을 반환!
}
```

### 2.2 근본 원인

1. **페이지 ID 0의 의미:**
   - FxStore 설계에서 `pageId = 0`은 "null" 또는 "빈 트리"를 의미
   - 유효한 페이지는 ID 1부터 시작해야 함

2. **계산 오류:**
   - `currentSize = 0`일 때 `newPageId = 0 / 4096 = 0`
   - `storage.extend()` **후에** 페이지 ID를 계산해야 하는데, **전에** 계산함

3. **디자인 문서 참조:**
   - docs/02.architecture.md:
     ```
     오프셋 0-4095: Superblock
     오프셋 4096-8191: CommitHeader A
     오프셋 8192-12287: CommitHeader B
     오프셋 12288~: 할당 영역 시작
     
     pageId = offset / pageSize
     ```
   - 실제 파일 기반에서는 첫 페이지 ID = 12288 / 4096 = 3
   - 하지만 테스트에서는 MemoryStorage를 사용하므로 단순화하여 ID 1부터 시작

### 2.3 원인 파악 과정

1. **테스트 결과 확인:**
   - `btree.getRootPageId()` 반환값이 0
   - 삽입 후에도 변경되지 않음

2. **코드 추적:**
   - `BTree.insert()` → 빈 트리일 때 새 리프 생성
   - `allocatePageId()` 호출
   - `storage.size() = 0` (초기 상태)
   - `newPageId = 0 / 4096 = 0` 계산

3. **디자인 문서 검토:**
   - 페이지 ID 0은 예약됨 (null 의미)
   - 파일 구조상 초기 영역은 헤더용

---

## 3. 해결 방법

### 3.1 수정된 코드

**BTree.java - `allocatePageId()` 메서드:**

```java
/**
 * 새 페이지 ID 할당
 * 
 * <p>페이지 ID는 파일 오프셋 / pageSize로 계산됩니다.
 * <p>ID 0은 "null/빈 트리"를 의미하므로, 유효한 페이지 ID는 1부터 시작합니다.
 * 
 * @return 새 페이지 ID (1부터 시작)
 */
private long allocatePageId() {
    long currentSize = storage.size();
    
    // extend 먼저 호출하여 새 페이지 공간 확보
    storage.extend(currentSize + pageSize);
    
    // 새 페이지 ID = 확장 후 크기 / pageSize
    // 이렇게 하면 첫 페이지 ID = 4096 / 4096 = 1이 됨
    long newPageId = (currentSize + pageSize) / pageSize;
    
    return newPageId;
}
```

### 3.2 수정 사항

**Before:**
```java
long newPageId = currentSize / pageSize;  // 0 반환
storage.extend(currentSize + pageSize);
return newPageId;  // 0
```

**After:**
```java
storage.extend(currentSize + pageSize);  // 먼저 확장
long newPageId = (currentSize + pageSize) / pageSize;  // 1 반환
return newPageId;  // 1
```

**핵심 변경점:**
1. `storage.extend()`를 **먼저** 호출
2. 확장 **후** 크기로 페이지 ID 계산
3. `(currentSize + pageSize) / pageSize` 사용

### 3.3 계산 예시

**첫 번째 페이지 할당:**
- `currentSize = 0`
- `extend(0 + 4096)` → `currentSize = 4096`
- `newPageId = (0 + 4096) / 4096 = 1` ✅

**두 번째 페이지 할당:**
- `currentSize = 4096`
- `extend(4096 + 4096)` → `currentSize = 8192`
- `newPageId = (4096 + 4096) / 4096 = 2` ✅

**세 번째 페이지 할당:**
- `currentSize = 8192`
- `extend(8192 + 4096)` → `currentSize = 12288`
- `newPageId = (8192 + 4096) / 4096 = 3` ✅

---

## 4. 테스트 결과

### 4.1 수정 전

```
> Task :test FAILED

com.fxstore.btree.BTreeInsertTest > testInsertDuplicateKey FAILED
com.fxstore.btree.BTreeInsertTest > testInsertWithSpace FAILED
com.fxstore.btree.BTreeInsertTest > testInsertIntoEmptyTree FAILED
com.fxstore.btree.BTreeInsertTest > testInsertMultipleKeysOrder FAILED

5 tests completed, 4 failed
```

### 4.2 수정 후

```
> Task :test
> Task :jacocoTestReport

BUILD SUCCESSFUL in 5s
```

**모든 BTreeInsertTest 통과:**
- ✅ testInsertIntoEmptyTree
- ✅ testInsertWithSpace
- ✅ testInsertDuplicateKey
- ✅ testInsertNullKey
- ✅ testInsertMultipleKeysOrder

### 4.3 전체 테스트 실행

```bash
$ ./gradlew test

BUILD SUCCESSFUL

All tests passed
```

---

## 5. 교훈 및 개선사항

### 5.1 발견한 문제점

1. **디자인 문서와 구현 불일치:**
   - 문서: "pageId 0은 null"
   - 구현: pageId 0을 할당하려 시도

2. **경계 조건 테스트 부족:**
   - 빈 트리에 첫 삽입 시 페이지 ID 검증 필요
   - 단위 테스트가 이를 잘 잡아냄 ✅

3. **순서 의존성:**
   - `extend()` 후 `계산` vs `계산` 후 `extend()`
   - 순서가 중요한 연산은 명확히 문서화 필요

### 5.2 재발 방지 대책

1. **코드 리뷰 체크리스트에 추가:**
   - [ ] 페이지 ID 0 예약 확인
   - [ ] allocatePageId() 호출 후 반환값이 0이 아닌지 검증
   - [ ] 빈 상태에서의 첫 할당 테스트

2. **Assertion 추가:**
   ```java
   private long allocatePageId() {
       // ...
       assert newPageId > 0 : "pageId must be positive, got: " + newPageId;
       return newPageId;
   }
   ```

3. **문서 업데이트:**
   - BTree.java 주석에 페이지 ID 규칙 명시
   - allocatePageId() 메서드에 자세한 설명 추가 ✅

### 5.3 SOLID 원칙 준수 확인

**Single Responsibility Principle (SRP):**
- ✅ `allocatePageId()`는 페이지 ID 할당만 담당
- ✅ Storage 확장은 Storage 인터페이스를 통해

**Dependency Inversion Principle (DIP):**
- ✅ BTree는 Storage 인터페이스에 의존
- ✅ MemoryStorage/FileStorage 교체 가능

**Test-Driven Development:**
- ✅ 테스트가 버그를 먼저 발견
- ✅ 수정 후 테스트로 검증

---

## 6. 결론

### 6.1 문제 요약

페이지 ID 할당 시 계산 순서 오류로 인해 항상 0을 반환하여, 빈 트리 삽입이 실패했습니다.

### 6.2 해결 요약

`storage.extend()` 호출 후 확장된 크기로 페이지 ID를 계산하도록 수정하여 유효한 ID (1부터)를 반환하게 했습니다.

### 6.3 검증 완료

- ✅ 모든 BTreeInsertTest 통과
- ✅ 전체 테스트 스위트 통과
- ✅ 회귀 테스트 없음

### 6.4 다음 단계

이제 BTree Insert가 정상 작동하므로, 다음 구현을 진행할 수 있습니다:
1. BTree Split (리프 및 Internal 노드)
2. BTree Delete
3. BTree Cursor 구현
4. Order-Statistic Tree (List 구현)
5. Deque 구현

---

**해결 완료일:** 2024-12-24  
**담당자:** AI Assistant  
**리뷰 상태:** 문서화 완료
