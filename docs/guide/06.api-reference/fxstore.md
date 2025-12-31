---
name: FxStore API
description: FxStore 인터페이스의 전체 메서드 레퍼런스
---

# FxStore API 레퍼런스

`FxStore`는 FxStore 라이브러리의 메인 인터페이스입니다.

## Store 열기

### open(Path)
파일 기반 Store를 기본 옵션으로 엽니다.

```java
FxStore store = FxStore.open(Paths.get("data.fx"));
```

### open(Path, FxOptions)
파일 기반 Store를 지정된 옵션으로 엽니다.

```java
FxOptions options = FxOptions.defaults()
    .withCommitMode(CommitMode.BATCH)
    .build();
FxStore store = FxStore.open(Paths.get("data.fx"), options);
```

### openMemory()
메모리 기반 Store를 엽니다. Store가 닫히면 데이터가 사라집니다.

```java
FxStore store = FxStore.openMemory();
```

### openMemory(FxOptions)
메모리 기반 Store를 지정된 옵션으로 엽니다.

```java
FxStore store = FxStore.openMemory(options);
```

## 코덱

### registerCodec(Class, FxCodec)
Store-local 코덱을 등록합니다.

```java
store.registerCodec(User.class, new UserCodec());
```

**예외**: 이미 등록된 타입이면 `FxException(ILLEGAL_ARGUMENT)`

### codecs()
Store의 코덱 레지스트리를 반환합니다.

```java
FxCodecRegistry registry = store.codecs();
```

## DDL (컬렉션 관리)

### exists(String)
컬렉션 존재 여부를 확인합니다.

```java
boolean exists = store.exists("users");
```

### drop(String)
컬렉션을 삭제합니다. 공간 회수는 `compactTo()`로 수행합니다.

```java
boolean dropped = store.drop("old_collection");
```

**예외**: 컬렉션이 없으면 `FxException(NOT_FOUND)`

### rename(String, String)
컬렉션 이름을 변경합니다.

```java
boolean renamed = store.rename("old_name", "new_name");
```

**예외**:
- 기존 이름이 없으면 `FxException(NOT_FOUND)`
- 새 이름이 이미 존재하면 `FxException(ALREADY_EXISTS)`

### list()
모든 컬렉션 정보를 조회합니다.

```java
List<CollectionInfo> collections = store.list();
for (CollectionInfo info : collections) {
    System.out.println(info.name() + ": " + info.kind());
}
```

## Map 생성

### createMap(String, Class, Class)
새 Map을 생성합니다.

```java
NavigableMap<Long, String> map = store.createMap("users", Long.class, String.class);
```

**예외**: 이미 존재하면 `FxException(ALREADY_EXISTS)`

### openMap(String, Class, Class)
기존 Map을 엽니다.

```java
NavigableMap<Long, String> map = store.openMap("users", Long.class, String.class);
```

**예외**: 존재하지 않으면 `FxException(NOT_FOUND)`

### createOrOpenMap(String, Class, Class)
Map을 생성하거나, 이미 존재하면 엽니다.

```java
NavigableMap<Long, String> map = store.createOrOpenMap("users", Long.class, String.class);
```

## Set 생성

### createSet, openSet, createOrOpenSet
Map과 동일한 패턴으로 NavigableSet을 생성/열기합니다.

```java
NavigableSet<String> tags = store.createOrOpenSet("tags", String.class);
```

## List 생성

### createList, openList, createOrOpenList
Map과 동일한 패턴으로 List를 생성/열기합니다.

```java
List<String> logs = store.createOrOpenList("logs", String.class);
```

## Deque 생성

### createDeque, openDeque, createOrOpenDeque
Map과 동일한 패턴으로 Deque를 생성/열기합니다.

```java
Deque<String> tasks = store.createOrOpenDeque("tasks", String.class);
```

## 커밋 제어

### commitMode()
현재 커밋 모드를 반환합니다.

```java
CommitMode mode = store.commitMode();
```

### commit()
BATCH 모드에서 변경사항을 커밋합니다. AUTO 모드에서는 no-op입니다.

```java
store.commit();
```

**예외**: 커밋 실패 시 `FxException(IO)`

### rollback()
BATCH 모드에서 마지막 커밋 이후 변경을 롤백합니다.

```java
store.rollback();
```

## 읽기 트랜잭션

### beginRead()
읽기 전용 트랜잭션을 시작합니다.

```java
try (FxReadTransaction tx = store.beginRead()) {
    String value = tx.get(map, key);
}
```

**예외**: Store가 닫힌 경우 `IllegalStateException`

## 진단

### stats()
기본(FAST) 통계를 조회합니다.

```java
Stats stats = store.stats();
```

### stats(StatsMode)
지정된 모드로 통계를 조회합니다.

```java
Stats detailed = store.stats(StatsMode.DETAILED);
```

### verify()
Store의 무결성을 검증합니다.

```java
VerifyResult result = store.verify();
if (!result.isOk()) {
    for (VerifyError error : result.errors()) {
        System.err.println(error.kind() + ": " + error.message());
    }
}
```

## 유지보수

### compactTo(Path)
새 파일로 컴팩션합니다. 라이브 데이터만 재작성하여 공간을 회수합니다.

```java
store.compactTo(Paths.get("data_compacted.fx"));
```

**예외**: BATCH 모드에서 pending 변경이 있으면 `FxException(ILLEGAL_ARGUMENT)`

## Store 닫기

### close()
Store를 닫습니다.

```java
store.close();
```

**예외**: `OnClosePolicy.ERROR`일 때 pending 변경이 있으면 예외
