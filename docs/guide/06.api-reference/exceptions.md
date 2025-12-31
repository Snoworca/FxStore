---
name: 예외 처리
description: FxStore 예외 및 에러 코드 레퍼런스
---

# 예외 처리

## FxException

`FxException`은 FxStore의 모든 예외를 나타내는 런타임 예외입니다.

```java
public class FxException extends RuntimeException {
    public FxErrorCode getCode();
    public String getMessage();
}
```

## 에러 코드 (FxErrorCode)

### I/O 및 리소스 에러

| 코드 | 설명 | 발생 상황 |
|------|------|----------|
| `IO` | I/O 작업 실패 | 읽기/쓰기/force 오류 |
| `CORRUPTION` | 데이터 손상 감지 | CRC 불일치, 형식 위반 |
| `OUT_OF_MEMORY` | 메모리 한계 초과 | 메모리 모드 제한 초과 |
| `LOCK_FAILED` | 파일 잠금 실패 | 다른 프로세스가 파일 사용 중 |

### 상태 에러

| 코드 | 설명 | 발생 상황 |
|------|------|----------|
| `CLOSED` | 닫힌 Store 조작 | Store가 이미 닫힌 후 사용 |
| `ILLEGAL_STATE` | 잘못된 상태 | 현재 상태에서 유효하지 않은 작업 |

### 존재 에러

| 코드 | 설명 | 발생 상황 |
|------|------|----------|
| `NOT_FOUND` | 리소스를 찾을 수 없음 | 컬렉션이 존재하지 않음 |
| `ALREADY_EXISTS` | 리소스가 이미 존재 | 컬렉션이 이미 존재 |

### 타입/버전 에러

| 코드 | 설명 | 발생 상황 |
|------|------|----------|
| `TYPE_MISMATCH` | 타입 불일치 | 컬렉션 종류 또는 코덱 ID 불일치 |
| `VERSION_MISMATCH` | 버전 불일치 | 코덱 버전, NumberMode, 포맷 버전, 페이지 크기 충돌 |
| `CODEC_NOT_FOUND` | 코덱을 찾을 수 없음 | 등록되지 않은 타입 |
| `UPGRADE_FAILED` | 업그레이드 실패 | 코덱 업그레이드 훅 실패 |

### 일반 에러

| 코드 | 설명 | 발생 상황 |
|------|------|----------|
| `ILLEGAL_ARGUMENT` | 잘못된 인자 | null, 범위 초과, 크기 초과 등 |
| `UNSUPPORTED` | 지원되지 않는 기능 | 뷰에서 쓰기 시도 등 |
| `INTERNAL` | 내부 오류 | 프로그래밍 오류, 리플렉션 실패 등 |

## 예외 처리 패턴

### 기본 처리

```java
try {
    NavigableMap<Long, String> map = store.openMap("users", Long.class, String.class);
} catch (FxException e) {
    switch (e.getCode()) {
        case NOT_FOUND:
            System.err.println("Collection not found: " + e.getMessage());
            break;
        case IO:
            System.err.println("I/O error: " + e.getMessage());
            break;
        default:
            throw e;
    }
}
```

### 컬렉션 존재 확인

```java
// 방법 1: 예외 처리
try {
    map = store.openMap("users", Long.class, String.class);
} catch (FxException e) {
    if (e.getCode() == FxErrorCode.NOT_FOUND) {
        map = store.createMap("users", Long.class, String.class);
    } else {
        throw e;
    }
}

// 방법 2: createOrOpen 사용 (권장)
map = store.createOrOpenMap("users", Long.class, String.class);
```

### I/O 오류 처리

```java
try {
    store.commit();
} catch (FxException e) {
    if (e.getCode() == FxErrorCode.IO) {
        // 재시도 또는 대체 처리
        System.err.println("Commit failed: " + e.getMessage());
    }
    throw e;
}
```

## 팩토리 메서드

`FxException`은 팩토리 메서드를 제공합니다:

```java
// I/O 및 리소스 에러
throw FxException.io("Failed to write to disk");
throw FxException.io("Read failed", cause);  // 원인 예외 포함
throw FxException.corruption("Invalid page checksum");
throw FxException.outOfMemory("Memory limit exceeded");
throw FxException.lockFailed("File is locked by another process");

// 상태 에러
throw FxException.closed("Store is closed");
throw FxException.illegalState("Transaction already committed");

// 존재 에러
throw FxException.notFound("Collection 'users' not found");
throw FxException.alreadyExists("Collection 'users' already exists");

// 타입/버전 에러
throw FxException.typeMismatch("Expected Map but found Set");
throw FxException.versionMismatch("Codec version conflict");
throw FxException.codecNotFound("No codec for type: User");
throw FxException.upgradeFailed("Migration failed", cause);

// 일반 에러
throw FxException.illegalArgument("Key cannot be null");
throw FxException.unsupported("Feature not supported");
```

## 일반적인 예외 상황

### 컬렉션 생성 시

```java
// ALREADY_EXISTS
store.createMap("existing", Long.class, String.class);  // 이미 존재하면 예외

// CODEC_NOT_FOUND
store.createMap("custom", Long.class, UnregisteredType.class);  // 코덱 없음
```

### 컬렉션 열기 시

```java
// NOT_FOUND
store.openMap("nonexistent", Long.class, String.class);  // 존재하지 않음

// CODEC_MISMATCH
store.openMap("users", String.class, String.class);  // 키 타입 불일치
```

### Store 열기 시

```java
// IO
FxStore.open(Paths.get("/invalid/path"));  // 경로 오류

// CORRUPTED
FxStore.open(Paths.get("corrupted.fx"));  // 손상된 파일
```

### 커밋 시

```java
// IO
store.commit();  // 디스크 쓰기 실패

// ILLEGAL_ARGUMENT (BATCH 모드에서 compactTo)
store.put(...);
store.compactTo(newPath);  // pending 변경 있음
```

## 표준 예외

FxStore는 Java 표준 예외도 사용합니다:

| 예외 | 상황 |
|------|------|
| `NullPointerException` | null 키/값 |
| `IllegalStateException` | Store/트랜잭션이 닫힌 후 사용 |
| `IndexOutOfBoundsException` | List 인덱스 범위 초과 |
| `NoSuchElementException` | 빈 컬렉션에서 first/last |

```java
// NullPointerException
map.put(null, "value");  // 키가 null

// IllegalStateException
store.close();
store.createMap(...);  // 닫힌 Store 사용

// IndexOutOfBoundsException
list.get(100);  // 범위 초과
```
