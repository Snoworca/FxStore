---
name: FxOptions API
description: FxOptions 설정 클래스의 전체 옵션 레퍼런스
---

# FxOptions API 레퍼런스

`FxOptions`는 Store 설정을 정의하는 불변 빌더 패턴 클래스입니다.

## 기본 사용법

```java
FxOptions options = FxOptions.defaults()
    .withCommitMode(CommitMode.BATCH)
    .withDurability(Durability.SYNC)
    .withCacheBytes(128 * 1024 * 1024)
    .build();

FxStore store = FxStore.open(path, options);
```

## 옵션 목록

### commitMode
커밋 모드를 설정합니다.

| 값 | 설명 | 기본값 |
|----|------|--------|
| `AUTO` | 각 쓰기가 즉시 커밋 | O |
| `BATCH` | `commit()` 호출 시 커밋 | |

```java
.withCommitMode(CommitMode.BATCH)
```

### durability
내구성 수준을 설정합니다.

| 값 | 설명 | 기본값 |
|----|------|--------|
| `ASYNC` | 비동기 디스크 동기화 | O |
| `SYNC` | 동기 디스크 동기화 | |

```java
.withDurability(Durability.SYNC)
```

### onClosePolicy
Store 닫기 시 미커밋 변경 처리 정책입니다.

| 값 | 설명 | 기본값 |
|----|------|--------|
| `ERROR` | 예외 발생 | O |
| `COMMIT` | 자동 커밋 | |
| `ROLLBACK` | 자동 롤백 | |

```java
.withOnClosePolicy(OnClosePolicy.ROLLBACK)
```

### fileLock
파일 잠금 모드입니다.

| 값 | 설명 | 기본값 |
|----|------|--------|
| `PROCESS` | 프로세스 단위 잠금 | O |
| `NONE` | 잠금 없음 | |

```java
.withFileLock(FileLockMode.NONE)
```

### pageSize
페이지 크기입니다.

| 값 | 바이트 | 기본값 |
|----|--------|--------|
| `PAGE_4K` | 4,096 | O |
| `PAGE_8K` | 8,192 | |
| `PAGE_16K` | 16,384 | |
| `PAGE_32K` | 32,768 | |
| `PAGE_64K` | 65,536 | |

```java
.withPageSize(PageSize.PAGE_16K)
```

### cacheBytes
페이지 캐시 크기 (바이트)입니다.

| 기본값 | 64MB |
|--------|------|

```java
.withCacheBytes(128 * 1024 * 1024)  // 128MB
```

### memoryLimitBytes
메모리 모드 전용: 최대 메모리 크기입니다.

| 기본값 | Long.MAX_VALUE (무제한) |
|--------|------------------------|

```java
.withMemoryLimitBytes(100 * 1024 * 1024)  // 100MB
```

### allowCodecUpgrade
코덱 버전 불일치 시 업그레이드 허용 여부입니다.

| 기본값 | false |
|--------|-------|

```java
.withAllowCodecUpgrade(true)
```

### codecUpgradeHook
코덱 업그레이드 시 데이터 변환 훅입니다.
`allowCodecUpgrade=true`일 때만 사용 가능합니다.

```java
.withAllowCodecUpgrade(true)
.withCodecUpgradeHook(new MyUpgradeHook())
```

### autoMigrateDeque
v0.6 이전 Deque를 v0.7 형식으로 자동 마이그레이션합니다.

| 기본값 | false |
|--------|-------|

```java
.withAutoMigrateDeque(true)
```

## 기본값 요약

```java
FxOptions defaults = FxOptions.defaults();
// commitMode:       AUTO
// durability:       ASYNC
// onClosePolicy:    ERROR
// fileLock:         PROCESS
// pageSize:         PAGE_4K
// cacheBytes:       64MB
// memoryLimitBytes: unlimited
// allowCodecUpgrade: false
// autoMigrateDeque: false
```

## 성능 최적화 예제

```java
// 대량 삽입용
FxOptions bulkInsert = FxOptions.defaults()
    .withCommitMode(CommitMode.BATCH)
    .withDurability(Durability.ASYNC)
    .withPageSize(PageSize.PAGE_16K)
    .withCacheBytes(256 * 1024 * 1024)
    .build();

// 최대 안전성
FxOptions safe = FxOptions.defaults()
    .withCommitMode(CommitMode.AUTO)
    .withDurability(Durability.SYNC)
    .build();
```
