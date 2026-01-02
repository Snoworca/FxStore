# Phase 1: 코덱 시스템 테스트 시나리오

> **Phase:** 1 - Codec System  
> **작성일:** 2024-12-24  
> **품질 정책:** QP-001 (타협 없음)  
> **목표:** 모든 테스트 100% 통과, 커버리지 90% 이상, 7/7 A+

---

## 1. FxCodec 인터페이스

### 1.1 인터페이스 정의 검증

**시나리오 TS-CODEC-001:** FxCodec 인터페이스가 필수 메서드를 정의한다

**Given:**
- FxCodec<T> 인터페이스가 정의되어 있음

**When:**
- 인터페이스를 조회

**Then:**
- `codecId()` 메서드 존재
- `encode(T value)` 메서드 존재
- `decode(byte[] bytes)` 메서드 존재
- `compareBytes(byte[] a, byte[] b)` 메서드 존재
- `equalsBytes(byte[] a, byte[] b)` 메서드 존재
- `hashBytes(byte[] bytes)` 메서드 존재

---

## 2. I64Codec (Long 코덱)

### 2.1 인코딩 테스트

**시나리오 TS-I64-001:** Integer → Long 정규화 (CANONICAL)

**Given:**
- NumberMode.CANONICAL
- I64Codec 인스턴스

**When:**
- `encode(Integer.valueOf(42))`

**Then:**
- 8바이트 배열 반환
- Little-Endian으로 42L 저장
- bytes = [42, 0, 0, 0, 0, 0, 0, 0]

**시나리오 TS-I64-002:** Long 값 인코딩

**Given:**
- I64Codec 인스턴스

**When:**
- `encode(Long.valueOf(0x0102030405060708L))`

**Then:**
- bytes = [0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01] (LE)

**시나리오 TS-I64-003:** 음수 인코딩

**Given:**
- I64Codec 인스턴스

**When:**
- `encode(Long.valueOf(-1L))`

**Then:**
- bytes = [0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]

**시나리오 TS-I64-004:** Long.MIN_VALUE 인코딩

**Given:**
- I64Codec 인스턴스

**When:**
- `encode(Long.MIN_VALUE)`

**Then:**
- bytes = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80] (LE)

**시나리오 TS-I64-005:** Long.MAX_VALUE 인코딩

**Given:**
- I64Codec 인스턴스

**When:**
- `encode(Long.MAX_VALUE)`

**Then:**
- bytes = [0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x7F] (LE)

### 2.2 디코딩 테스트

**시나리오 TS-I64-010:** 바이트 → Long 디코딩

**Given:**
- I64Codec 인스턴스
- bytes = [42, 0, 0, 0, 0, 0, 0, 0]

**When:**
- `decode(bytes)`

**Then:**
- 결과 = 42L

**시나리오 TS-I64-011:** 음수 디코딩

**Given:**
- I64Codec 인스턴스
- bytes = [0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]

**When:**
- `decode(bytes)`

**Then:**
- 결과 = -1L

**시나리오 TS-I64-012:** 엔코딩-디코딩 라운드트립

**Given:**
- I64Codec 인스턴스
- 원본 값들: -100L, 0L, 1L, 42L, 1000L, Long.MIN_VALUE, Long.MAX_VALUE

**When:**
- 각 값에 대해 `decode(encode(value))` 수행

**Then:**
- 모든 값이 원본과 동일

### 2.3 비교 테스트

**시나리오 TS-I64-020:** signed 비교 (음수 < 양수)

**Given:**
- I64Codec 인스턴스
- bytes1 = encode(-1L)
- bytes2 = encode(1L)

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (음수가 작음)

**시나리오 TS-I64-021:** 크기 비교

**Given:**
- I64Codec 인스턴스
- bytes1 = encode(10L)
- bytes2 = encode(100L)

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (10 < 100)

**시나리오 TS-I64-022:** 동일 값 비교

**Given:**
- I64Codec 인스턴스
- bytes1 = encode(42L)
- bytes2 = encode(42L)

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 = 0

**시나리오 TS-I64-023:** MIN_VALUE < MAX_VALUE

**Given:**
- I64Codec 인스턴스
- bytes1 = encode(Long.MIN_VALUE)
- bytes2 = encode(Long.MAX_VALUE)

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0

### 2.4 동등성/해시 테스트

**시나리오 TS-I64-030:** 동일 값 동등성

**Given:**
- I64Codec 인스턴스
- bytes1 = encode(42L)
- bytes2 = encode(42L)

**When:**
- `equalsBytes(bytes1, bytes2)`

**Then:**
- 결과 = true

**시나리오 TS-I64-031:** 다른 값 동등성

**Given:**
- I64Codec 인스턴스
- bytes1 = encode(42L)
- bytes2 = encode(43L)

**When:**
- `equalsBytes(bytes1, bytes2)`

**Then:**
- 결과 = false

**시나리오 TS-I64-032:** 해시 일관성

**Given:**
- I64Codec 인스턴스
- bytes = encode(42L)

**When:**
- h1 = hashBytes(bytes)
- h2 = hashBytes(bytes)

**Then:**
- h1 = h2

**시나리오 TS-I64-033:** 동일 값의 해시 동일

**Given:**
- I64Codec 인스턴스
- bytes1 = encode(42L)
- bytes2 = encode(42L)

**When:**
- h1 = hashBytes(bytes1)
- h2 = hashBytes(bytes2)

**Then:**
- h1 = h2

### 2.5 NumberMode.CANONICAL 검증

**시나리오 TS-I64-040:** Integer와 Long이 동일 바이트로 인코딩

**Given:**
- I64Codec 인스턴스
- Integer(42)
- Long(42L)

**When:**
- bytes1 = encode(Integer(42))
- bytes2 = encode(Long(42L))

**Then:**
- Arrays.equals(bytes1, bytes2) = true

**시나리오 TS-I64-041:** Byte → Long 정규화

**Given:**
- I64Codec 인스턴스
- Byte.valueOf((byte)10)

**When:**
- `encode(Byte.valueOf(10))`

**Then:**
- encode(10L)과 동일한 바이트

**시나리오 TS-I64-042:** Short → Long 정규화

**Given:**
- I64Codec 인스턴스
- Short.valueOf((short)1000)

**When:**
- `encode(Short.valueOf(1000))`

**Then:**
- encode(1000L)과 동일한 바이트

### 2.6 예외 처리

**시나리오 TS-I64-050:** null 인코딩 실패

**Given:**
- I64Codec 인스턴스

**When:**
- `encode(null)`

**Then:**
- NullPointerException 발생

**시나리오 TS-I64-051:** 잘못된 바이트 길이 디코딩 실패

**Given:**
- I64Codec 인스턴스
- bytes = [1, 2, 3] (3바이트, 8바이트 아님)

**When:**
- `decode(bytes)`

**Then:**
- IllegalArgumentException 발생
- 메시지: "Expected 8 bytes, got 3"

**시나리오 TS-I64-052:** null 바이트 디코딩 실패

**Given:**
- I64Codec 인스턴스

**When:**
- `decode(null)`

**Then:**
- NullPointerException 발생

---

## 3. F64Codec (Double 코덱)

### 3.1 인코딩 테스트

**시나리오 TS-F64-001:** Float → Double 정규화

**Given:**
- NumberMode.CANONICAL
- F64Codec 인스턴스

**When:**
- `encode(Float.valueOf(3.14f))`

**Then:**
- 8바이트 배열 반환
- IEEE754 Double로 인코딩 (float의 정확한 double 표현)

**시나리오 TS-F64-002:** Double 값 인코딩

**Given:**
- F64Codec 인스턴스

**When:**
- `encode(Double.valueOf(Math.PI))`

**Then:**
- 8바이트 IEEE754 표현
- Double.doubleToRawLongBits(Math.PI)를 LE로 저장

**시나리오 TS-F64-003:** 특수 값 인코딩

**Given:**
- F64Codec 인스턴스
- 특수 값들: 0.0, -0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN

**When:**
- 각 값을 `encode()`

**Then:**
- 각각 올바른 IEEE754 비트 패턴으로 인코딩

**시나리오 TS-F64-004:** Double.MIN_VALUE 인코딩

**Given:**
- F64Codec 인스턴스

**When:**
- `encode(Double.MIN_VALUE)`

**Then:**
- IEEE754 최소 정규화 값 인코딩

**시나리오 TS-F64-005:** Double.MAX_VALUE 인코딩

**Given:**
- F64Codec 인스턴스

**When:**
- `encode(Double.MAX_VALUE)`

**Then:**
- IEEE754 최대 값 인코딩

### 3.2 디코딩 테스트

**시나리오 TS-F64-010:** 바이트 → Double 디코딩

**Given:**
- F64Codec 인스턴스
- bytes = encode(3.14)

**When:**
- `decode(bytes)`

**Then:**
- 결과 = 3.14

**시나리오 TS-F64-011:** 엔코딩-디코딩 라운드트립

**Given:**
- F64Codec 인스턴스
- 값들: -1.5, 0.0, 1.0, Math.PI, Math.E, 1e100, 1e-100

**When:**
- 각 값에 대해 `decode(encode(value))` 수행

**Then:**
- 모든 값이 원본과 동일 (비트 수준까지)

**시나리오 TS-F64-012:** 특수 값 라운드트립

**Given:**
- F64Codec 인스턴스
- 특수 값들: 0.0, -0.0, POSITIVE_INFINITY, NEGATIVE_INFINITY, NaN

**When:**
- 각 값에 대해 `decode(encode(value))` 수행

**Then:**
- 0.0과 -0.0은 구분됨 (비트 패턴 다름)
- Infinity 값들 정확히 복원
- NaN은 NaN으로 복원 (비트 패턴은 다를 수 있음)

### 3.3 비교 테스트 (Double.compare 사용)

**시나리오 TS-F64-020:** 크기 비교

**Given:**
- F64Codec 인스턴스
- bytes1 = encode(1.0)
- bytes2 = encode(2.0)

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (1.0 < 2.0)

**시나리오 TS-F64-021:** 음수 < 양수

**Given:**
- F64Codec 인스턴스
- bytes1 = encode(-1.0)
- bytes2 = encode(1.0)

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0

**시나리오 TS-F64-022:** -0.0 vs 0.0 비교

**Given:**
- F64Codec 인스턴스
- bytes1 = encode(-0.0)
- bytes2 = encode(0.0)

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (Double.compare(-0.0, 0.0) = -1)

**시나리오 TS-F64-023:** NaN 비교 (총순서)

**Given:**
- F64Codec 인스턴스
- bytes1 = encode(Double.NaN)
- bytes2 = encode(1.0)

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 > 0 (NaN이 모든 값보다 큼, Double.compare 총순서)

**시나리오 TS-F64-024:** Infinity 비교

**Given:**
- F64Codec 인스턴스
- bytesNegInf = encode(Double.NEGATIVE_INFINITY)
- bytesPosInf = encode(Double.POSITIVE_INFINITY)
- bytesZero = encode(0.0)

**When:**
- `compareBytes(bytesNegInf, bytesZero)`
- `compareBytes(bytesZero, bytesPosInf)`

**Then:**
- 첫 번째 < 0 (-Infinity < 0)
- 두 번째 < 0 (0 < +Infinity)

### 3.4 동등성/해시 테스트

**시나리오 TS-F64-030:** 동일 값 동등성

**Given:**
- F64Codec 인스턴스
- bytes1 = encode(3.14)
- bytes2 = encode(3.14)

**When:**
- `equalsBytes(bytes1, bytes2)`

**Then:**
- 결과 = true

**시나리오 TS-F64-031:** 0.0 vs -0.0 동등성

**Given:**
- F64Codec 인스턴스
- bytes1 = encode(0.0)
- bytes2 = encode(-0.0)

**When:**
- `equalsBytes(bytes1, bytes2)`

**Then:**
- 결과 = false (비트 패턴 다름)

**시나리오 TS-F64-032:** NaN 동등성

**Given:**
- F64Codec 인스턴스
- bytes1 = encode(Double.NaN)
- bytes2 = encode(Double.NaN)

**When:**
- `equalsBytes(bytes1, bytes2)`

**Then:**
- 결과 = true (같은 비트 패턴)

### 3.5 예외 처리

**시나리오 TS-F64-050:** null 인코딩 실패

**Given:**
- F64Codec 인스턴스

**When:**
- `encode(null)`

**Then:**
- NullPointerException 발생

**시나리오 TS-F64-051:** 잘못된 바이트 길이 디코딩 실패

**Given:**
- F64Codec 인스턴스
- bytes = [1, 2, 3, 4] (4바이트, 8바이트 아님)

**When:**
- `decode(bytes)`

**Then:**
- IllegalArgumentException 발생

---

## 4. StringCodec (UTF-8 코덱)

### 4.1 인코딩 테스트

**시나리오 TS-STR-001:** ASCII 문자열 인코딩

**Given:**
- StringCodec 인스턴스

**When:**
- `encode("Hello")`

**Then:**
- UTF-8 바이트: [72, 101, 108, 108, 111]

**시나리오 TS-STR-002:** 한글 인코딩

**Given:**
- StringCodec 인스턴스

**When:**
- `encode("안녕")`

**Then:**
- UTF-8 바이트 (한글은 3바이트씩)
- "안" = [0xEC, 0x95, 0x88]
- "녕" = [0xEB, 0x85, 0x95]

**시나리오 TS-STR-003:** 빈 문자열 인코딩

**Given:**
- StringCodec 인스턴스

**When:**
- `encode("")`

**Then:**
- 길이 0인 바이트 배열

**시나리오 TS-STR-004:** 특수 문자 인코딩

**Given:**
- StringCodec 인스턴스

**When:**
- `encode("a\nb\tc")`

**Then:**
- UTF-8로 정확히 인코딩 (개행, 탭 포함)

**시나리오 TS-STR-005:** Emoji 인코딩

**Given:**
- StringCodec 인스턴스

**When:**
- `encode("😀")`

**Then:**
- UTF-8 4바이트로 인코딩

### 4.2 디코딩 테스트

**시나리오 TS-STR-010:** 바이트 → 문자열 디코딩

**Given:**
- StringCodec 인스턴스
- bytes = [72, 101, 108, 108, 111]

**When:**
- `decode(bytes)`

**Then:**
- 결과 = "Hello"

**시나리오 TS-STR-011:** 엔코딩-디코딩 라운드트립

**Given:**
- StringCodec 인스턴스
- 문자열들: "Hello", "안녕하세요", "123", "", "a\nb\tc", "😀🎉"

**When:**
- 각 문자열에 대해 `decode(encode(str))` 수행

**Then:**
- 모든 문자열이 원본과 동일

### 4.3 비교 테스트 (unsigned lexicographic)

**시나리오 TS-STR-020:** 사전순 비교

**Given:**
- StringCodec 인스턴스
- bytes1 = encode("apple")
- bytes2 = encode("banana")

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 ("apple" < "banana")

**시나리오 TS-STR-021:** 접두사 비교

**Given:**
- StringCodec 인스턴스
- bytes1 = encode("test")
- bytes2 = encode("testing")

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (짧은 문자열이 접두사일 때 작음)

**시나리오 TS-STR-022:** 동일 문자열 비교

**Given:**
- StringCodec 인스턴스
- bytes1 = encode("hello")
- bytes2 = encode("hello")

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 = 0

**시나리오 TS-STR-023:** 대소문자 비교

**Given:**
- StringCodec 인스턴스
- bytes1 = encode("Apple")
- bytes2 = encode("apple")

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (ASCII에서 대문자 < 소문자)

**시나리오 TS-STR-024:** 한글 사전순 비교

**Given:**
- StringCodec 인스턴스
- bytes1 = encode("가나다")
- bytes2 = encode("마바사")

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (UTF-8 바이트 순서가 한글 가나다 순서와 일치)

### 4.4 동등성/해시 테스트

**시나리오 TS-STR-030:** 동일 문자열 동등성

**Given:**
- StringCodec 인스턴스
- bytes1 = encode("test")
- bytes2 = encode("test")

**When:**
- `equalsBytes(bytes1, bytes2)`

**Then:**
- 결과 = true

**시나리오 TS-STR-031:** 다른 문자열 동등성

**Given:**
- StringCodec 인스턴스
- bytes1 = encode("test")
- bytes2 = encode("Test")

**When:**
- `equalsBytes(bytes1, bytes2)`

**Then:**
- 결과 = false

### 4.5 예외 처리

**시나리오 TS-STR-050:** null 인코딩 실패

**Given:**
- StringCodec 인스턴스

**When:**
- `encode(null)`

**Then:**
- NullPointerException 발생

**시나리오 TS-STR-051:** null 바이트 디코딩 실패

**Given:**
- StringCodec 인스턴스

**When:**
- `decode(null)`

**Then:**
- NullPointerException 발생

**시나리오 TS-STR-052:** 잘못된 UTF-8 디코딩

**Given:**
- StringCodec 인스턴스
- bytes = [0xFF, 0xFF] (유효하지 않은 UTF-8)

**When:**
- `decode(bytes)`

**Then:**
- 예외 발생 또는 replacement character (U+FFFD) 반환

---

## 5. BytesCodec (byte[] 코덱)

### 5.1 인코딩 테스트

**시나리오 TS-BYT-001:** 바이트 배열 인코딩

**Given:**
- BytesCodec 인스턴스

**When:**
- `encode(new byte[]{1, 2, 3})`

**Then:**
- 동일한 바이트 배열 반환 (복사본)

**시나리오 TS-BYT-002:** 빈 배열 인코딩

**Given:**
- BytesCodec 인스턴스

**When:**
- `encode(new byte[0])`

**Then:**
- 빈 바이트 배열 반환

**시나리오 TS-BYT-003:** 큰 배열 인코딩

**Given:**
- BytesCodec 인스턴스
- 1MB 크기 배열

**When:**
- `encode(bigArray)`

**Then:**
- 동일한 내용의 배열 반환

### 5.2 디코딩 테스트

**시나리오 TS-BYT-010:** 바이트 → byte[] 디코딩

**Given:**
- BytesCodec 인스턴스
- bytes = [1, 2, 3]

**When:**
- `decode(bytes)`

**Then:**
- 동일한 바이트 배열 반환 (복사본)

**시나리오 TS-BYT-011:** 엔코딩-디코딩 라운드트립

**Given:**
- BytesCodec 인스턴스
- 배열들: [1, 2, 3], [], [0xFF, 0xFE], [0, 0, 0]

**When:**
- 각 배열에 대해 `decode(encode(arr))` 수행

**Then:**
- 모든 배열이 원본과 동일

### 5.3 비교 테스트 (길이 우선 정렬)

**시나리오 TS-BYT-020:** 길이 우선 비교

**Given:**
- BytesCodec 인스턴스
- bytes1 = encode([1, 2])      // 길이 2
- bytes2 = encode([1, 2, 3])   // 길이 3

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (짧은 배열이 먼저)

**시나리오 TS-BYT-021:** 길이 동일 시 lexicographic 비교

**Given:**
- BytesCodec 인스턴스
- bytes1 = encode([1, 2, 3])
- bytes2 = encode([1, 2, 4])

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (3 < 4)

**시나리오 TS-BYT-022:** unsigned 비교

**Given:**
- BytesCodec 인스턴스
- bytes1 = encode([0x01])
- bytes2 = encode([0xFF])

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (unsigned: 1 < 255)

**시나리오 TS-BYT-023:** 동일 배열 비교

**Given:**
- BytesCodec 인스턴스
- bytes1 = encode([1, 2, 3])
- bytes2 = encode([1, 2, 3])

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 = 0

**시나리오 TS-BYT-024:** 빈 배열 비교

**Given:**
- BytesCodec 인스턴스
- bytes1 = encode([])
- bytes2 = encode([1])

**When:**
- `compareBytes(bytes1, bytes2)`

**Then:**
- 결과 < 0 (빈 배열이 가장 작음)

### 5.4 동등성/해시 테스트

**시나리오 TS-BYT-030:** 동일 배열 동등성

**Given:**
- BytesCodec 인스턴스
- bytes1 = encode([1, 2, 3])
- bytes2 = encode([1, 2, 3])

**When:**
- `equalsBytes(bytes1, bytes2)`

**Then:**
- 결과 = true

**시나리오 TS-BYT-031:** 다른 배열 동등성

**Given:**
- BytesCodec 인스턴스
- bytes1 = encode([1, 2, 3])
- bytes2 = encode([1, 2, 4])

**When:**
- `equalsBytes(bytes1, bytes2)`

**Then:**
- 결과 = false

### 5.5 예외 처리

**시나리오 TS-BYT-050:** null 인코딩 실패

**Given:**
- BytesCodec 인스턴스

**When:**
- `encode(null)`

**Then:**
- NullPointerException 발생

**시나리오 TS-BYT-051:** null 디코딩 실패

**Given:**
- BytesCodec 인스턴스

**When:**
- `decode(null)`

**Then:**
- NullPointerException 발생

---

## 6. FxCodecRegistry (코덱 레지스트리)

### 6.1 등록/조회 테스트

**시나리오 TS-REG-001:** 코덱 등록

**Given:**
- FxCodecRegistry 인스턴스
- 커스텀 코덱 (예: PersonCodec)

**When:**
- `register(Person.class, personCodec)`

**Then:**
- `get(Person.class)` 반환 = personCodec

**시나리오 TS-REG-002:** 미등록 타입 조회

**Given:**
- FxCodecRegistry 인스턴스

**When:**
- `get(UnknownClass.class)`

**Then:**
- null 반환 또는 IllegalArgumentException 발생

**시나리오 TS-REG-003:** codecId로 조회

**Given:**
- FxCodecRegistry 인스턴스
- I64Codec 등록됨 (codecId = "I64")

**When:**
- `getById("I64")`

**Then:**
- I64Codec 반환

**시나리오 TS-REG-004:** 중복 등록 방지

**Given:**
- FxCodecRegistry 인스턴스
- codec1, codec2 (같은 타입)

**When:**
- `register(String.class, codec1)`
- `register(String.class, codec2)`

**Then:**
- 두 번째 등록이 첫 번째를 덮어씀 또는 예외 발생

### 6.2 내장 코덱 자동 등록

**시나리오 TS-REG-010:** 글로벌 레지스트리 초기화

**Given:**
- FxCodecs.global() 호출

**When:**
- 레지스트리 조회

**Then:**
- I64Codec이 Long.class, Integer.class, Short.class, Byte.class에 등록됨
- F64Codec이 Double.class, Float.class에 등록됨
- StringCodec이 String.class에 등록됨
- BytesCodec이 byte[].class에 등록됨

**시나리오 TS-REG-011:** codecId로 내장 코덱 조회

**Given:**
- FxCodecs.global()

**When:**
- `getById("I64")`, `getById("F64")`, `getById("STRING")`, `getById("BYTES")`

**Then:**
- 각각 올바른 코덱 반환

### 6.3 동시성 테스트

**시나리오 TS-REG-020:** 멀티스레드 등록

**Given:**
- FxCodecRegistry 인스턴스
- 10개 스레드

**When:**
- 각 스레드가 서로 다른 타입 등록

**Then:**
- 모든 등록 성공
- 각 타입 정확히 조회 가능

**시나리오 TS-REG-021:** 멀티스레드 조회

**Given:**
- FxCodecRegistry 인스턴스
- 내장 코덱 등록됨
- 10개 스레드

**When:**
- 각 스레드가 동일한 타입 반복 조회

**Then:**
- 모든 조회 성공
- 동일한 코덱 인스턴스 반환

---

## 7. CodecRef (코덱 참조)

**시나리오 TS-REF-001:** CodecRef 생성

**Given:**
- codecId = "I64"
- version = 1

**When:**
- `new CodecRef("I64", 1)`

**Then:**
- codecId() = "I64"
- version() = 1

**시나리오 TS-REF-002:** CodecRef 동등성

**Given:**
- ref1 = new CodecRef("I64", 1)
- ref2 = new CodecRef("I64", 1)

**When:**
- `ref1.equals(ref2)`

**Then:**
- 결과 = true

**시나리오 TS-REF-003:** CodecRef 해시

**Given:**
- ref1 = new CodecRef("I64", 1)
- ref2 = new CodecRef("I64", 1)

**When:**
- `ref1.hashCode()`, `ref2.hashCode()`

**Then:**
- 두 해시코드 동일

---

## 8. 통합 테스트

### 8.1 NumberMode.CANONICAL 검증

**시나리오 TS-INT-001:** Integer와 Long 호환

**Given:**
- FxCodecs.global()
- Integer(42), Long(42L)

**When:**
- codec1 = get(Integer.class)
- codec2 = get(Long.class)
- bytes1 = codec1.encode(42)
- bytes2 = codec2.encode(42L)

**Then:**
- Arrays.equals(bytes1, bytes2) = true
- codec1.compareBytes(bytes1, bytes2) = 0

**시나리오 TS-INT-002:** Float와 Double 호환

**Given:**
- FxCodecs.global()
- Float(3.14f), Double(3.14)

**When:**
- codec1 = get(Float.class)
- codec2 = get(Double.class)
- bytes1 = codec1.encode(3.14f)
- bytes2 = codec2.encode((double)3.14f)

**Then:**
- Arrays.equals(bytes1, bytes2) = true

### 8.2 사용자 코덱 확장

**시나리오 TS-INT-010:** 커스텀 코덱 정의

**Given:**
- 커스텀 클래스 Person(String name, int age)
- PersonCodec 구현 (FxCodec<Person>)

**When:**
- FxCodecs.global().register(Person.class, new PersonCodec())

**Then:**
- get(Person.class) 성공
- Person 객체 인코딩/디코딩 가능

---

## 9. 성능 테스트

**시나리오 TS-PERF-001:** I64 인코딩 성능

**Given:**
- I64Codec 인스턴스
- 1,000,000개 Long 값

**When:**
- 각 값에 대해 `encode()` 수행

**Then:**
- 총 시간 < 1초

**시나리오 TS-PERF-002:** String 인코딩 성능

**Given:**
- StringCodec 인스턴스
- 100,000개 문자열 (평균 길이 20자)

**When:**
- 각 문자열에 대해 `encode()` 수행

**Then:**
- 총 시간 < 1초

---

## 10. 회귀 테스트

### 10.1 Phase 0 회귀 검증

**시나리오 TS-REG-100:** Phase 0 테스트 재실행

**Given:**
- Phase 0의 모든 테스트 (78개)

**When:**
- 전체 테스트 실행

**Then:**
- 78/78 테스트 통과
- 커버리지 유지 (95% 이상)

---

## 테스트 커버리지 목표

### 라인 커버리지
- **목표: 90% 이상**
- 모든 코덱 클래스: 95% 이상
- FxCodecRegistry: 90% 이상

### 브랜치 커버리지
- **목표: 85% 이상**
- 예외 처리 경로 모두 커버
- 특수 값 (NaN, Infinity, null 등) 모두 테스트

### 테스트 품질
- **각 코덱당 최소 15개 테스트**
- 모든 public 메서드 테스트
- 경계값 테스트 (MIN, MAX, 0, null)
- 예외 경로 테스트

---

## 테스트 실행 순서

1. **FxCodec 인터페이스** (컴파일 검증)
2. **I64CodecTest** (15+ tests)
3. **F64CodecTest** (15+ tests)
4. **StringCodecTest** (15+ tests)
5. **BytesCodecTest** (15+ tests)
6. **CodecRefTest** (3 tests)
7. **FxCodecRegistryTest** (10+ tests)
8. **통합 테스트** (5+ tests)
9. **성능 테스트** (2 tests)
10. **회귀 테스트** (Phase 0 전체)

**예상 총 테스트 수: 80+ (Phase 1만)**
**예상 총 테스트 수 (누적): 158+ (Phase 0 + Phase 1)**

---

**작성 완료일:** 2024-12-24  
**다음 단계:** 테스트 코드 작성 및 실행  
**품질 기준:** 7/7 A+ 달성 필수

**"타협은 없습니다."** - QP-001
