---
name: FxOptions API
description: Complete options reference for FxOptions configuration class
---

# FxOptions API Reference

`FxOptions` is an immutable builder pattern class for defining Store configuration.

## Basic Usage

```java
FxOptions options = FxOptions.defaults()
    .withCommitMode(CommitMode.BATCH)
    .withDurability(Durability.SYNC)
    .withCacheBytes(128 * 1024 * 1024)
    .build();

FxStore store = FxStore.open(path, options);
```

## Options List

### commitMode
Sets the commit mode.

| Value | Description | Default |
|-------|-------------|---------|
| `AUTO` | Each write commits immediately | O |
| `BATCH` | Commits on `commit()` call | |

```java
.withCommitMode(CommitMode.BATCH)
```

### durability
Sets the durability level.

| Value | Description | Default |
|-------|-------------|---------|
| `ASYNC` | Asynchronous disk sync | O |
| `SYNC` | Synchronous disk sync | |

```java
.withDurability(Durability.SYNC)
```

### onClosePolicy
Policy for handling uncommitted changes on Store close.

| Value | Description | Default |
|-------|-------------|---------|
| `ERROR` | Throw exception | O |
| `COMMIT` | Auto commit | |
| `ROLLBACK` | Auto rollback | |

```java
.withOnClosePolicy(OnClosePolicy.ROLLBACK)
```

### fileLock
File locking mode.

| Value | Description | Default |
|-------|-------------|---------|
| `PROCESS` | Process-level locking | O |
| `NONE` | No locking | |

```java
.withFileLock(FileLockMode.NONE)
```

### pageSize
Page size.

| Value | Bytes | Default |
|-------|-------|---------|
| `PAGE_4K` | 4,096 | O |
| `PAGE_8K` | 8,192 | |
| `PAGE_16K` | 16,384 | |
| `PAGE_32K` | 32,768 | |
| `PAGE_64K` | 65,536 | |

```java
.withPageSize(PageSize.PAGE_16K)
```

### cacheBytes
Page cache size (bytes).

| Default | 64MB |
|---------|------|

```java
.withCacheBytes(128 * 1024 * 1024)  // 128MB
```

### memoryLimitBytes
Memory mode only: Maximum memory size.

| Default | Long.MAX_VALUE (unlimited) |
|---------|----------------------------|

```java
.withMemoryLimitBytes(100 * 1024 * 1024)  // 100MB
```

### allowCodecUpgrade
Whether to allow codec upgrade on version mismatch.

| Default | false |
|---------|-------|

```java
.withAllowCodecUpgrade(true)
```

### codecUpgradeHook
Data conversion hook for codec upgrades.
Only usable when `allowCodecUpgrade=true`.

```java
.withAllowCodecUpgrade(true)
.withCodecUpgradeHook(new MyUpgradeHook())
```

### autoMigrateDeque
Auto-migrate pre-v0.6 Deque to v0.7 format.

| Default | false |
|---------|-------|

```java
.withAutoMigrateDeque(true)
```

## Default Values Summary

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

## Performance Optimization Examples

```java
// For bulk insertion
FxOptions bulkInsert = FxOptions.defaults()
    .withCommitMode(CommitMode.BATCH)
    .withDurability(Durability.ASYNC)
    .withPageSize(PageSize.PAGE_16K)
    .withCacheBytes(256 * 1024 * 1024)
    .build();

// Maximum safety
FxOptions safe = FxOptions.defaults()
    .withCommitMode(CommitMode.AUTO)
    .withDurability(Durability.SYNC)
    .build();
```
