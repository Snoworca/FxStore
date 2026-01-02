# Phase 2 테스트 커버리지 개선 시나리오

> **작성일**: 2024-12-24  
> **목적**: Storage 패키지 커버리지를 95% 이상으로 개선  
> **현재 상태**: 64% → 목표: 95%

---

## 🎯 개선 목표

### 패키지별 커버리지 목표

| 패키지 | 현재 | 목표 | 우선순위 |
|--------|------|------|----------|
| FileStorage | 59% | 95% | 🔴 긴급 |
| MemoryStorage | 68% | 95% | 🔴 긴급 |
| 전체 storage | 64% | 95% | 🔴 긴급 |

---

## 📋 FileStorage 테스트 추가 항목

### FS-NEW-001: null 파라미터 검증
```
Given: FileStorage 클래스
When: 생성자에 null path 전달
Then: NullPointerException 발생
```

**테스트 코드:**
```java
@Test(expected = NullPointerException.class)
public void testConstructor_NullPath() {
    new FileStorage(null, false);
}
```

### FS-NEW-002: readOnly 모드 테스트
```
Given: 기존 파일 존재
When: readOnly=true로 FileStorage 생성
Then: 
  - 파일이 읽기 전용으로 열림
  - read() 성공
  - write() 시도 시 예외 발생
```

**테스트 코드:**
```java
@Test
public void testReadOnlyMode() throws Exception {
    // Given: 파일 생성 및 데이터 쓰기
    Path path = tempDir.resolve("readonly.fx");
    try (FileStorage ws = new FileStorage(path, false)) {
        ws.write(0, "TEST".getBytes(), 0, 4);
        ws.force(true);
    }
    
    // When: readOnly로 열기
    try (FileStorage rs = new FileStorage(path, true)) {
        // Then: 읽기 성공
        byte[] buffer = new byte[4];
        rs.read(0, buffer, 0, 4);
        assertEquals("TEST", new String(buffer));
        
        // 쓰기 시도
        try {
            rs.write(0, "FAIL".getBytes(), 0, 4);
            fail("Expected exception for write on readonly storage");
        } catch (FxException e) {
            // 예상된 예외
        }
    }
}
```

### FS-NEW-003: IOException 발생 시나리오
```
Given: FileStorage 인스턴스
When: 채널이 닫힌 후 read() 시도
Then: FxException(IO) 발생
```

**테스트 코드:**
```java
@Test
public void testReadAfterChannelClosed() throws Exception {
    Path path = tempDir.resolve("test.fx");
    FileStorage storage = new FileStorage(path, false);
    storage.close();
    
    byte[] buffer = new byte[10];
    try {
        storage.read(0, buffer, 0, 10);
        fail("Expected FxException");
    } catch (FxException e) {
        assertEquals(FxErrorCode.CLOSED, e.getCode());
    }
}
```

### FS-NEW-004: length=0 읽기/쓰기
```
Given: FileStorage 인스턴스
When: length=0으로 read() 또는 write() 호출
Then: 예외 없이 즉시 반환 (no-op)
```

**테스트 코드:**
```java
@Test
public void testZeroLengthRead() throws Exception {
    Path path = tempDir.resolve("test.fx");
    try (FileStorage storage = new FileStorage(path, false)) {
        byte[] buffer = new byte[10];
        storage.read(0, buffer, 0, 0);  // should not throw
    }
}

@Test
public void testZeroLengthWrite() throws Exception {
    Path path = tempDir.resolve("test.fx");
    try (FileStorage storage = new FileStorage(path, false)) {
        storage.write(0, new byte[10], 0, 0);  // should not throw
    }
}
```

### FS-NEW-005: force() IOException
```
Given: FileStorage 인스턴스, 채널이 닫힌 상태
When: force() 호출
Then: FxException(IO) 발생
```

**테스트 코드:**
```java
@Test
public void testForceOnClosedChannel() throws Exception {
    Path path = tempDir.resolve("test.fx");
    FileStorage storage = new FileStorage(path, false);
    storage.close();
    
    try {
        storage.force(true);
        fail("Expected FxException");
    } catch (FxException e) {
        assertEquals(FxErrorCode.CLOSED, e.getCode());
    }
}
```

### FS-NEW-006: write IOException 시나리오
```
Given: FileStorage 인스턴스
When: 디스크 full 또는 권한 없음 등으로 write 실패
Then: FxException(IO) 발생
```

**테스트 코드:**
```java
// Mock을 사용하여 IOException 강제 발생
@Test
public void testWriteIOException() throws Exception {
    // Note: 실제 IOException을 유발하기 어려우므로
    // 이 테스트는 통합 테스트 또는 수동 테스트로 대체 가능
    // 또는 FileChannel을 Mock하여 테스트
}
```

### FS-NEW-007: 경계값 테스트 - 큰 파일
```
Given: FileStorage 인스턴스
When: 1MB 크기의 데이터 쓰기
Then: 성공적으로 쓰고 읽을 수 있음
```

**테스트 코드:**
```java
@Test
public void testLargeFile() throws Exception {
    Path path = tempDir.resolve("large.fx");
    try (FileStorage storage = new FileStorage(path, false)) {
        byte[] data = new byte[1024 * 1024];  // 1MB
        Arrays.fill(data, (byte) 0xAB);
        
        storage.write(0, data, 0, data.length);
        storage.force(true);
        
        byte[] read = new byte[data.length];
        storage.read(0, read, 0, read.length);
        
        assertArrayEquals(data, read);
    }
}
```

---

## 📋 MemoryStorage 테스트 추가 항목

### MS-NEW-001: 메모리 한계 초과
```
Given: MemoryStorage(limitBytes=1024)
When: 2048 바이트 쓰기 시도
Then: FxException(OUT_OF_MEMORY) 발생
```

**테스트 코드:**
```java
@Test
public void testMemoryLimitExceeded() {
    MemoryStorage storage = new MemoryStorage(1024);
    byte[] data = new byte[2048];
    
    try {
        storage.write(0, data, 0, data.length);
        fail("Expected FxException(OUT_OF_MEMORY)");
    } catch (FxException e) {
        assertEquals(FxErrorCode.OUT_OF_MEMORY, e.getCode());
    }
}
```

### MS-NEW-002: 동적 확장 경계값
```
Given: MemoryStorage(초기 buffer=512, limit=4096)
When: 순차적으로 512, 512, 512, 512 바이트 쓰기
Then: 각 쓰기마다 버퍼 확장, 최종 size=2048
```

**테스트 코드:**
```java
@Test
public void testDynamicExpansion() {
    MemoryStorage storage = new MemoryStorage(4096);
    
    for (int i = 0; i < 4; i++) {
        byte[] data = new byte[512];
        Arrays.fill(data, (byte) i);
        storage.write(i * 512, data, 0, 512);
    }
    
    assertEquals(2048, storage.size());
    
    // 검증: 각 블록의 데이터 확인
    for (int i = 0; i < 4; i++) {
        byte[] read = new byte[512];
        storage.read(i * 512, read, 0, 512);
        assertEquals((byte) i, read[0]);
    }
}
```

### MS-NEW-003: read/write length=0
```
Given: MemoryStorage 인스턴스
When: length=0으로 read()/write() 호출
Then: 예외 없이 즉시 반환
```

**테스트 코드:**
```java
@Test
public void testZeroLengthOperations() {
    MemoryStorage storage = new MemoryStorage(1024);
    byte[] buffer = new byte[10];
    
    storage.write(0, buffer, 0, 0);  // no-op
    storage.read(0, buffer, 0, 0);   // no-op
    
    assertEquals(0, storage.size());
}
```

### MS-NEW-004: close 후 접근
```
Given: MemoryStorage 인스턴스, close() 호출
When: read() 또는 write() 시도
Then: FxException(CLOSED) 발생
```

**테스트 코드:**
```java
@Test
public void testOperationsAfterClose() {
    MemoryStorage storage = new MemoryStorage(1024);
    storage.close();
    
    byte[] buffer = new byte[10];
    try {
        storage.read(0, buffer, 0, 10);
        fail("Expected FxException(CLOSED)");
    } catch (FxException e) {
        assertEquals(FxErrorCode.CLOSED, e.getCode());
    }
    
    try {
        storage.write(0, buffer, 0, 10);
        fail("Expected FxException(CLOSED)");
    } catch (FxException e) {
        assertEquals(FxErrorCode.CLOSED, e.getCode());
    }
}
```

### MS-NEW-005: force() 동작
```
Given: MemoryStorage 인스턴스
When: force(true) 호출
Then: 예외 없이 즉시 반환 (no-op for memory)
```

**테스트 코드:**
```java
@Test
public void testForce() {
    MemoryStorage storage = new MemoryStorage(1024);
    storage.write(0, "TEST".getBytes(), 0, 4);
    storage.force(true);  // should not throw
    storage.force(false); // should not throw
}
```

---

## 📋 Storage 인터페이스 테스트 (공통)

### ST-NEW-001: 파라미터 검증
```
Given: Storage 인스턴스
When: 잘못된 파라미터로 read()/write() 호출
  - offset < 0
  - buffer == null
  - bufOffset < 0
  - length < 0
  - bufOffset + length > buffer.length
Then: IllegalArgumentException 발생
```

**테스트 코드:**
```java
@Test(expected = IllegalArgumentException.class)
public void testReadInvalidOffset() {
    storage.read(-1, new byte[10], 0, 10);
}

@Test(expected = NullPointerException.class)
public void testReadNullBuffer() {
    storage.read(0, null, 0, 10);
}

@Test(expected = IllegalArgumentException.class)
public void testReadInvalidBufferRange() {
    storage.read(0, new byte[5], 0, 10);  // 5 < 0+10
}
```

---

## 🎯 개선 후 예상 커버리지

| 클래스 | 현재 | 추가 테스트 | 예상 |
|--------|------|-------------|------|
| FileStorage | 59% | +11개 | 95%+ |
| MemoryStorage | 68% | +7개 | 95%+ |
| **전체 storage** | **64%** | **+18개** | **95%+** |

---

## 🔄 테스트 실행 계획

### 1단계: 테스트 코드 작성
```bash
# 위의 시나리오를 기반으로 테스트 클래스에 메서드 추가
- FileStorageTest.java: 11개 테스트 추가
- MemoryStorageTest.java: 7개 테스트 추가
```

### 2단계: 테스트 실행
```bash
./gradlew clean test
```

### 3단계: 커버리지 확인
```bash
./gradlew jacocoTestReport
cat build/reports/jacoco/test/html/index.html
```

### 4단계: 재평가
- 목표(95%) 달성 시 → Phase 2 품질 평가 계속 진행
- 미달 시 → 추가 테스트 작성 후 1단계로 복귀

---

## 📝 작성 후 체크리스트

- [ ] 모든 시나리오 검토 완료
- [ ] 테스트 코드 작성 시작
- [ ] 테스트 실행 및 검증
- [ ] 커버리지 95% 이상 달성 확인
- [ ] Phase 2 평가 문서 업데이트

---

*작성일: 2024-12-24*  
*목표: Storage 패키지 커버리지 95% 이상*
