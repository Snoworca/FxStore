---
name: FxReadTransaction API
description: 읽기 전용 트랜잭션 인터페이스 레퍼런스
---

# FxReadTransaction API 레퍼런스

`FxReadTransaction`은 읽기 전용 트랜잭션으로, 시작 시점의 스냅샷을 고정하여 일관된 읽기를 제공합니다.

## 기본 사용법

```java
try (FxReadTransaction tx = store.beginRead()) {
    String value = tx.get(map, key);
    // ...
} // 자동 close
```

## Map 연산

### get(NavigableMap, K)
Map에서 키로 값을 조회합니다.

```java
String value = tx.get(userMap, 1L);
```

**반환**: 키에 해당하는 값, 없으면 null

### containsKey(NavigableMap, K)
Map에 키가 존재하는지 확인합니다.

```java
boolean exists = tx.containsKey(userMap, 1L);
```

### firstEntry(NavigableMap)
Map의 첫 번째 엔트리를 조회합니다.

```java
Map.Entry<Long, String> first = tx.firstEntry(userMap);
```

**반환**: 첫 번째 엔트리, 비어있으면 null

### lastEntry(NavigableMap)
Map의 마지막 엔트리를 조회합니다.

```java
Map.Entry<Long, String> last = tx.lastEntry(userMap);
```

### size(NavigableMap)
Map의 엔트리 수를 반환합니다.

```java
int size = tx.size(userMap);
```

## Set 연산

### contains(NavigableSet, E)
Set에 요소가 존재하는지 확인합니다.

```java
boolean has = tx.contains(tagSet, "java");
```

### first(NavigableSet)
Set의 첫 번째 요소를 조회합니다.

```java
String first = tx.first(tagSet);
```

### last(NavigableSet)
Set의 마지막 요소를 조회합니다.

```java
String last = tx.last(tagSet);
```

### size(NavigableSet)
Set의 요소 수를 반환합니다.

```java
int size = tx.size(tagSet);
```

## List 연산

### get(List, int)
List에서 인덱스로 요소를 조회합니다.

```java
String item = tx.get(logList, 0);
```

**예외**: 인덱스가 범위를 벗어나면 `IndexOutOfBoundsException`

### size(List)
List의 요소 수를 반환합니다.

```java
int size = tx.size(logList);
```

### indexOf(List, E)
List에서 요소의 인덱스를 찾습니다.

```java
int idx = tx.indexOf(logList, "target");
```

**반환**: 요소의 인덱스, 없으면 -1

## Deque 연산

### peekFirst(Deque)
Deque의 첫 번째 요소를 조회합니다 (제거하지 않음).

```java
String first = tx.peekFirst(taskDeque);
```

### peekLast(Deque)
Deque의 마지막 요소를 조회합니다 (제거하지 않음).

```java
String last = tx.peekLast(taskDeque);
```

### size(Deque)
Deque의 요소 수를 반환합니다.

```java
int size = tx.size(taskDeque);
```

## 트랜잭션 관리

### isActive()
트랜잭션이 활성 상태인지 확인합니다.

```java
boolean active = tx.isActive();
```

### getSnapshotSeqNo()
트랜잭션의 스냅샷 시퀀스 번호를 반환합니다 (디버깅용).

```java
long seqNo = tx.getSnapshotSeqNo();
```

### close()
트랜잭션을 종료합니다. try-with-resources 사용을 권장합니다.

```java
tx.close();
```

## 예외

모든 연산에서 발생할 수 있는 예외:

| 예외 | 원인 |
|------|------|
| `IllegalStateException` | 트랜잭션이 이미 닫힌 경우 |
| `IllegalArgumentException` | 다른 Store의 컬렉션을 사용한 경우 |

## 불변식

- **INV-RT1**: 트랜잭션 내 스냅샷은 절대 변경 불가
- **INV-RT2**: 동일 트랜잭션 내 모든 읽기는 동일 스냅샷 사용
- **INV-RT3**: 다른 스레드의 쓰기가 활성 트랜잭션에 영향 없음
- **INV-RT4**: close() 후 모든 연산은 예외 발생
- **INV-RT5**: 트랜잭션은 생성된 store의 컬렉션만 접근 가능

## 예제

```java
FxStore store = FxStore.open(path);
NavigableMap<Long, String> users = store.createOrOpenMap("users", Long.class, String.class);
NavigableMap<Long, Integer> scores = store.createOrOpenMap("scores", Long.class, Integer.class);

// 일관된 읽기
try (FxReadTransaction tx = store.beginRead()) {
    // 두 Map에서 일관된 시점의 데이터 조회
    String userName = tx.get(users, 1L);
    Integer userScore = tx.get(scores, 1L);

    // 중간에 다른 스레드가 데이터 수정해도
    // 이 트랜잭션에서는 시작 시점 데이터만 보임

    System.out.println(userName + ": " + userScore);
}

store.close();
```
