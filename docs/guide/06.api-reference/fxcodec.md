---
name: FxCodec API
description: FxCodec 인터페이스 및 커스텀 코덱 구현 가이드
---

# FxCodec API 레퍼런스

`FxCodec<T>`는 타입 T의 직렬화/역직렬화 및 비교 규칙을 정의하는 인터페이스입니다.

## 인터페이스 정의

```java
public interface FxCodec<T> {
    String id();
    int version();
    byte[] encode(T value);
    T decode(byte[] bytes);
    int compareBytes(byte[] a, byte[] b);
    boolean equalsBytes(byte[] a, byte[] b);
    int hashBytes(byte[] bytes);
}
```

## 메서드 상세

### id()
코덱의 고유 식별자를 반환합니다.
**영속적**이며 한 번 정의하면 변경하면 안 됩니다.

```java
@Override
public String id() {
    return "app:user";  // 형식: "namespace:name"
}
```

**규칙**:
- 형식: `namespace:name`
- 내장 코덱: `fx:i64`, `fx:str` 등
- 커스텀 코덱: `app:user`, `mycompany:order` 등

### version()
직렬화 형식의 버전을 반환합니다.
형식이 변경되면 버전을 증가시켜야 합니다.

```java
@Override
public int version() {
    return 1;
}
```

### encode(T)
객체를 바이트 배열로 직렬화합니다.
**결정적**이어야 합니다 (같은 입력 → 같은 출력).

```java
@Override
public byte[] encode(User user) {
    ByteBuffer buffer = ByteBuffer.allocate(100);
    buffer.putLong(user.getId());
    // ...
    return buffer.array();
}
```

**예외**: value가 null이면 `NullPointerException`

### decode(byte[])
바이트 배열을 객체로 역직렬화합니다.

```java
@Override
public User decode(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    long id = buffer.getLong();
    // ...
    return new User(id, ...);
}
```

### compareBytes(byte[], byte[])
두 바이트 배열의 순서를 비교합니다 (정렬용).

```java
@Override
public int compareBytes(byte[] a, byte[] b) {
    // 음수: a < b
    // 0: a == b
    // 양수: a > b
    ByteBuffer bufA = ByteBuffer.wrap(a);
    ByteBuffer bufB = ByteBuffer.wrap(b);
    return Long.compare(bufA.getLong(), bufB.getLong());
}
```

**중요**: NavigableMap/Set의 키로 사용하는 타입은 이 메서드가 올바른 정렬 순서를 반환해야 합니다.

### equalsBytes(byte[], byte[])
두 바이트 배열의 동등성을 확인합니다.

```java
@Override
public boolean equalsBytes(byte[] a, byte[] b) {
    return Arrays.equals(a, b);
}
```

### hashBytes(byte[])
바이트 배열의 해시값을 계산합니다.

```java
@Override
public int hashBytes(byte[] bytes) {
    return Arrays.hashCode(bytes);
}
```

## 내장 코덱

| 타입 | 코덱 ID | 클래스 |
|------|--------|--------|
| `Long` | `fx:i64` | `I64Codec` |
| `Integer` | `fx:i32` | `IntegerCodec` |
| `Short` | `fx:i16` | `ShortCodec` |
| `Byte` | `fx:i8` | `ByteCodec` |
| `Double` | `fx:f64` | `F64Codec` |
| `Float` | `fx:f32` | `FloatCodec` |
| `String` | `fx:str` | `StringCodec` |
| `byte[]` | `fx:bytes` | `BytesCodec` |

## 구현 예제

### 단순 타입 코덱

```java
public class PointCodec implements FxCodec<Point> {
    public static final PointCodec INSTANCE = new PointCodec();

    @Override
    public String id() { return "app:point"; }

    @Override
    public int version() { return 1; }

    @Override
    public byte[] encode(Point p) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putInt(p.x);
        buf.putInt(p.y);
        return buf.array();
    }

    @Override
    public Point decode(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        return new Point(buf.getInt(), buf.getInt());
    }

    @Override
    public int compareBytes(byte[] a, byte[] b) {
        ByteBuffer bufA = ByteBuffer.wrap(a);
        ByteBuffer bufB = ByteBuffer.wrap(b);
        int cmp = Integer.compare(bufA.getInt(), bufB.getInt());
        if (cmp != 0) return cmp;
        return Integer.compare(bufA.getInt(), bufB.getInt());
    }

    @Override
    public boolean equalsBytes(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }

    @Override
    public int hashBytes(byte[] bytes) {
        return Arrays.hashCode(bytes);
    }
}
```

### 가변 길이 문자열 포함 타입

```java
public class PersonCodec implements FxCodec<Person> {

    @Override
    public byte[] encode(Person p) {
        byte[] nameBytes = p.getName().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(4 + nameBytes.length + 4);
        buf.putInt(nameBytes.length);
        buf.put(nameBytes);
        buf.putInt(p.getAge());
        return buf.array();
    }

    @Override
    public Person decode(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int nameLen = buf.getInt();
        byte[] nameBytes = new byte[nameLen];
        buf.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);
        int age = buf.getInt();
        return new Person(name, age);
    }

    // ... 나머지 메서드
}
```

## 코덱 등록

```java
FxStore store = FxStore.open(path);
store.registerCodec(Point.class, PointCodec.INSTANCE);
store.registerCodec(Person.class, new PersonCodec());
```

## 모범 사례

1. **결정적 인코딩**: 같은 객체는 항상 같은 바이트 생성
2. **버전 관리**: 형식 변경 시 version() 증가
3. **null 처리**: encode()에서 null 검사
4. **호환성**: 이전 버전 데이터 읽기 가능하도록 설계
