# Phase 0 테스트 시나리오: 기반 설정

> **목적**: 프로젝트 기본 구조, 공통 유틸리티, Enum 타입의 정확성 검증

---

## 시나리오 1: ByteUtils 엔디안 검증

### 테스트 케이스 1.1: putI32LE / getI32LE - 엔디안 확인
- **입력**: `0x12345678`
- **기대**: Little-endian 바이트 순서 `[0x78, 0x56, 0x34, 0x12]`
- **검증**:
  ```java
  byte[] buf = new byte[4];
  ByteUtils.putI32LE(buf, 0, 0x12345678);
  assertEquals(0x78, buf[0] & 0xFF);
  assertEquals(0x56, buf[1] & 0xFF);
  assertEquals(0x34, buf[2] & 0xFF);
  assertEquals(0x12, buf[3] & 0xFF);
  
  int value = ByteUtils.getI32LE(buf, 0);
  assertEquals(0x12345678, value);
  ```

### 테스트 케이스 1.2: putI64LE / getI64LE - 왕복 검증
- **입력**: `0x0123456789ABCDEFL`
- **기대**: 동일한 값 복원
- **검증**: encode → decode → 원본과 동일

### 테스트 케이스 1.3: putF64 / getF64 - IEEE754 검증
- **입력**: `Math.PI`, `Double.MAX_VALUE`, `Double.MIN_VALUE`, `-0.0`, `Double.NaN`
- **기대**: IEEE 754 정확히 복원
- **검증**: Double.compare(원본, 복원) == 0

---

## 시나리오 2: CRC32C 정확성

### 테스트 케이스 2.1: 빈 배열
- **입력**: `byte[0]`
- **기대**: CRC32 초기값

### 테스트 케이스 2.2: 단일 바이트
- **입력**: `byte[] {0x00}`
- **기대**: 알려진 CRC32 값과 일치

### 테스트 케이스 2.3: 알려진 데이터
- **입력**: `"FXSTORE\0".getBytes()`
- **기대**: 일관된 CRC32 값 (여러 번 호출해도 동일)

---

## 시나리오 3: Enum 타입 일관성

### 테스트 케이스 3.1: FxErrorCode 모든 값 존재
- **검증**: 모든 enum 값이 정의되어 있는지 확인

### 테스트 케이스 3.2: PageSize.fromBytes() 정확성
- **입력**: `4096`, `8192`, `16384`
- **기대**: 각각 `PAGE_4K`, `PAGE_8K`, `PAGE_16K` 반환
- **입력**: `2048` (잘못된 값)
- **기대**: `IllegalArgumentException`

### 테스트 케이스 3.3: PageSize.bytes() 정확성
- **검증**: 각 PageSize의 bytes() 값이 올바른지 확인

---

## 시나리오 4: FxException 동작

### 테스트 케이스 4.1: 기본 생성자
- **입력**: `new FxException(FxErrorCode.IO, "test message")`
- **기대**: code == IO, message == "test message"

### 테스트 케이스 4.2: 편의 메서드
- **입력**: `FxException.io("IO error")`
- **기대**: code == IO, message == "IO error"

### 테스트 케이스 4.3: Cause 전달
- **입력**: `FxException.io("wrapper", new IOException("root"))`
- **기대**: getCause() != null, getCause() instanceof IOException

---

## 시나리오 5: FxOptions Builder 패턴

### 테스트 케이스 5.1: 기본값 검증
- **입력**: `FxOptions.defaults()`
- **기대**:
  - commitMode == AUTO
  - durability == ASYNC
  - onClosePolicy == ERROR
  - fileLock == PROCESS
  - pageSize == PAGE_4K
  - cacheBytes == 64 * 1024 * 1024
  - numberMode == CANONICAL

### 테스트 케이스 5.2: Builder 체이닝
- **입력**:
  ```java
  FxOptions.defaults()
      .withCommitMode(CommitMode.BATCH)
      .withCacheBytes(128 * 1024 * 1024)
      .build()
  ```
- **기대**: commitMode == BATCH, cacheBytes == 128MB, 나머지는 기본값

### 테스트 케이스 5.3: NumberMode.STRICT 거부
- **입력**: `FxOptions.defaults().withNumberMode(NumberMode.STRICT)`
- **기대**: `FxException` with code == UNSUPPORTED

### 테스트 케이스 5.4: null 검증
- **입력**: `FxOptions.defaults().withCommitMode(null)`
- **기대**: `FxException` with code == ILLEGAL_ARGUMENT

---

## 커버리지 목표

- **ByteUtils**: 라인 100%, 브랜치 100%
- **CRC32C**: 라인 100%, 브랜치 100%
- **FxException**: 라인 100%, 브랜치 100%
- **FxOptions**: 라인 100%, 브랜치 95%+ (일부 방어 코드 제외)
- **모든 Enum**: 라인 100%

---

## 성공 기준

- ✅ 모든 테스트 케이스 통과
- ✅ 커버리지 목표 달성
- ✅ 컴파일 경고 없음
- ✅ 빌드 성공
