# Phase 2 테스트 시나리오 - Storage 및 Page 관리

> **작성일**: 2025-12-24  
> **Phase**: Phase 2 - Storage 및 Page 관리  
> **목표**: 모든 구현 전 테스트 시나리오를 먼저 작성 (Test-First)

---

## 시나리오 작성 원칙

**이 문서는 코드 작성 전에 반드시 작성되어야 합니다.**

1. ✅ 모든 공개 API에 대한 시나리오 작성
2. ✅ 성공 케이스 + 실패 케이스 + 경계 케이스 모두 포함
3. ✅ Given-When-Then 형식 사용
4. ✅ 구체적인 입력값과 기대 출력 명시

---

## Week 1: Storage 레이어

### 1.1 FileStorage - 기본 읽기/쓰기

#### 시나리오 FS-001: 새 파일 생성 및 쓰기
```
Given: 존재하지 않는 파일 경로 "test.fx"
When: FileStorage를 생성하고 offset=0에 "FXSTORE\0" (8바이트) 쓰기
Then:
  - 파일이 생성됨
  - size()는 8을 반환
  - 같은 offset에서 읽으면 "FXSTORE\0"와 일치
```

#### 시나리오 FS-002: 기존 파일 열기
```
Given: "test.fx" 파일이 존재하고 처음 8바이트가 "FXSTORE\0"
When: FileStorage를 열기 모드로 생성
Then:
  - size()는 파일 크기를 반환
  - offset=0에서 8바이트 읽으면 "FXSTORE\0"
```

#### 시나리오 FS-003: 페이지 정렬된 쓰기
```
Given: FileStorage 인스턴스
When: offset=4096에 4096바이트 쓰기
Then:
  - size()는 8192를 반환
  - offset=4096에서 읽으면 쓴 데이터와 일치
```

#### 시나리오 FS-004: 부분 읽기/쓰기
```
Given: FileStorage 인스턴스
When: offset=100, length=50으로 쓰기
Then:
  - offset=100, length=50으로 읽으면 동일
  - offset=90, length=20으로 읽으면 처음 10바이트는 0, 나머지 10바이트는 쓴 데이터의 앞부분
```

#### 시나리오 FS-005: force (fsync)
```
Given: FileStorage 인스턴스, 일부 데이터 쓰기 완료
When: force(true) 호출
Then:
  - 프로세스 종료 후 재시작해도 데이터 유지
  - FxException 발생 안 함
```

#### 시나리오 FS-006: 잘못된 offset 읽기
```
Given: FileStorage 인스턴스, size=1000
When: offset=1000에서 100바이트 읽기 시도
Then: FxException(IO) 발생 (EOF)
```

#### 시나리오 FS-007: 파일 확장 (extend)
```
Given: FileStorage 인스턴스, size=1000
When: extend(5000) 호출
Then:
  - size()는 5000을 반환
  - offset=4000에 쓰기 가능
```

---

### 1.2 MemoryStorage - 메모리 기반

#### 시나리오 MS-001: 메모리 Storage 생성
```
Given: MemoryStorage 인스턴스 (limitBytes=1MB)
When: 생성 직후
Then:
  - size()는 0
  - offset=0에 쓰기 가능
```

#### 시나리오 MS-002: 동적 확장
```
Given: MemoryStorage 인스턴스
When: offset=0에 100KB 쓰기
Then:
  - size()는 100KB
  - 내부 버퍼 자동 확장
```

#### 시나리오 MS-003: 메모리 한계 초과
```
Given: MemoryStorage 인스턴스 (limitBytes=1KB)
When: 2KB 쓰기 시도
Then: FxException(OUT_OF_MEMORY) 발생
```

#### 시나리오 MS-004: force는 no-op
```
Given: MemoryStorage 인스턴스
When: force(true) 호출
Then: 아무 일도 일어나지 않음 (예외 없음)
```

---

### 1.3 FileLock - 파일 잠금

#### 시나리오 FL-001: PROCESS 모드 - 단일 writer
```
Given: 파일 "test.fx", FileLockMode.PROCESS
When: FileStorage 생성
Then: 파일에 배타적 잠금 획득
```

#### 시나리오 FL-002: PROCESS 모드 - 중복 writer 차단
```
Given: 파일 "test.fx"에 이미 FileStorage 인스턴스 1이 잠금 보유
When: 동일 파일로 FileStorage 인스턴스 2 생성 시도
Then: FxException(LOCK_FAILED) 발생
```

#### 시나리오 FL-003: NONE 모드 - 잠금 없음
```
Given: 파일 "test.fx", FileLockMode.NONE
When: FileStorage 생성
Then: 잠금 시도하지 않음
```

#### 시나리오 FL-004: close 시 잠금 해제
```
Given: FileStorage 인스턴스 (FileLockMode.PROCESS)
When: close() 호출
Then:
  - 잠금 해제
  - 다른 프로세스가 파일 열 수 있음
```

---

## Week 2: Page 시스템

### 2.1 PageCache - LRU 캐싱

#### 시나리오 PC-001: 캐시 미스 후 저장
```
Given: PageCache 인스턴스 (maxPages=10)
When:
  - pageId=100으로 get() → null 반환
  - put(100, data) 호출
  - get(100) 호출
Then: 저장한 data 반환
```

#### 시나리오 PC-002: LRU 제거
```
Given: PageCache 인스턴스 (maxPages=3)
When:
  - put(1, data1)
  - put(2, data2)
  - put(3, data3)
  - put(4, data4)  # 캐시 가득 참
Then:
  - get(1) → null (LRU로 제거됨)
  - get(2), get(3), get(4) → 데이터 반환
```

#### 시나리오 PC-003: 접근 시 LRU 갱신
```
Given: PageCache 인스턴스 (maxPages=3), put(1), put(2), put(3)
When:
  - get(1)  # 1을 가장 최근으로
  - put(4)  # 캐시 초과
Then:
  - get(1) → 데이터 반환 (제거 안됨)
  - get(2) → null (LRU로 제거됨)
```

#### 시나리오 PC-004: 동일 pageId 중복 저장
```
Given: PageCache 인스턴스
When:
  - put(100, data1)
  - put(100, data2)  # 덮어쓰기
Then: get(100) → data2 반환
```

#### 시나리오 PC-005: invalidate
```
Given: PageCache 인스턴스, put(100, data)
When: invalidate(100) 호출
Then: get(100) → null
```

#### 시나리오 PC-006: clear
```
Given: PageCache 인스턴스, put(1, data1), put(2, data2)
When: clear() 호출
Then:
  - get(1) → null
  - get(2) → null
```

---

### 2.2 Allocator - Append-only 할당

#### 시나리오 AL-001: 페이지 할당 (정렬)
```
Given: Allocator 인스턴스 (pageSize=4096, allocTail=12288)
When: allocPage() 호출
Then:
  - pageId=3 반환 (offset 12288 / pageSize 4096)
  - effectiveTail()는 16384 (12288 + 4096)
```

#### 시나리오 AL-002: 레코드 할당 (8바이트 정렬)
```
Given: Allocator 인스턴스 (allocTail=12290)
When: allocRecord(100) 호출
Then:
  - offset=12296 반환 (12290 → 12296으로 정렬)
  - effectiveTail()는 12396 (12296 + 100)
```

#### 시나리오 AL-003: BATCH 모드 - pending 관리
```
Given: Allocator 인스턴스 (allocTail=1000)
When:
  - beginBatch()
  - allocRecord(100) → offset1
  - allocRecord(50) → offset2
  - effectiveTail()는 1000 + alignUp(100) + alignUp(50)
  - commitBatch()
Then:
  - allocTail이 effectiveTail로 갱신
  - pendingAllocTail=null
```

#### 시나리오 AL-004: BATCH 롤백
```
Given: Allocator 인스턴스 (allocTail=1000)
When:
  - beginBatch()
  - allocRecord(100)
  - rollbackBatch()
Then:
  - effectiveTail()는 1000 (원복)
  - pendingAllocTail=null
```

#### 시나리오 AL-005: 파일 자동 확장
```
Given: Allocator 인스턴스, Storage size=10000
When: allocRecord(5000) 호출 (allocTail=8000)
Then:
  - Storage.extend(13000 이상) 호출됨
  - 예외 발생 안 함
```

---

### 2.3 Page 구조 - 공통 헤더

#### 시나리오 PG-001: Page 헤더 인코딩
```
Given: Page 객체 (type=BTREE_LEAF, pageId=100, lsn=5)
When: 헤더를 32바이트로 인코딩
Then:
  - offset=0: magic "FXPG" (4바이트)
  - offset=4: pageType=2 (u16)
  - offset=6: flags=0 (u16)
  - offset=8: pageId=100 (u64)
  - offset=16: lsn=5 (u64)
  - offset=24: payloadCrc=계산된 값 (u32)
  - offset=28: reserved=0 (u32)
```

#### 시나리오 PG-002: Page 헤더 디코딩
```
Given: 32바이트 헤더 데이터
When: 디코딩
Then: Page 객체의 필드가 정확히 일치
```

#### 시나리오 PG-003: CRC 검증 실패
```
Given: 손상된 헤더 (CRC 불일치)
When: verifyPageCrc() 호출
Then: false 반환
```

---

### 2.4 SlottedPage - 가변 길이 엔트리

#### 시나리오 SP-001: 빈 슬롯 페이지 생성
```
Given: SlottedPage 생성 (payloadSize=4000)
When: 생성 직후
Then:
  - entryCount=0
  - freeSpaceOffset=payloadSize
  - availableSpace()는 payloadSize - 고정헤더크기
```

#### 시나리오 SP-002: 엔트리 삽입
```
Given: 빈 SlottedPage
When: 10바이트 엔트리 삽입
Then:
  - entryCount=1
  - freeSpaceOffset=payloadSize - 10
  - slots[0]은 엔트리 오프셋 가리킴
```

#### 시나리오 SP-003: 정렬된 삽입
```
Given: SlottedPage에 key=5, key=10 엔트리 존재
When: key=7 엔트리를 position=1에 삽입
Then:
  - entryCount=3
  - slots[0] → key=5
  - slots[1] → key=7
  - slots[2] → key=10
```

#### 시나리오 SP-004: 공간 부족
```
Given: SlottedPage (availableSpace=20)
When: 30바이트 엔트리 삽입 시도
Then: false 반환 또는 예외 (공간 부족)
```

#### 시나리오 SP-005: 엔트리 삭제
```
Given: SlottedPage에 3개 엔트리
When: position=1 엔트리 삭제
Then:
  - entryCount=2
  - slots[1]은 이전 slots[2] 가리킴
  - freeSpaceOffset 변경 없음 (dead space 남음)
```

---

## Week 3: Superblock 및 CommitHeader

### 3.1 Superblock

#### 시나리오 SB-001: Superblock 생성 및 인코딩
```
Given: 새 Superblock (pageSize=4096, formatVersion=1)
When: 4096바이트로 인코딩
Then:
  - offset=0: magic "FXSTORE\0"
  - offset=8: formatVersion=1
  - offset=12: pageSize=4096
  - offset=16: featureFlags=1 (CRC enabled)
  - offset=24: createdAtEpochMs > 0
  - offset=4092: CRC32C 값
```

#### 시나리오 SB-002: Superblock 디코딩
```
Given: 유효한 Superblock 바이트 배열
When: 디코딩
Then: 모든 필드 정확히 복원
```

#### 시나리오 SB-003: CRC 검증 성공
```
Given: 유효한 Superblock
When: verifySuperblock() 호출
Then: true 반환
```

#### 시나리오 SB-004: CRC 검증 실패
```
Given: 손상된 Superblock (CRC 불일치)
When: verifySuperblock() 호출
Then: false 반환
```

#### 시나리오 SB-005: Magic 검증 실패
```
Given: 잘못된 magic을 가진 Superblock
When: verifySuperblock() 호출
Then: false 반환
```

#### 시나리오 SB-006: pageSize 검증 실패
```
Given: pageSize=3000 (비표준)
When: verifySuperblock() 호출
Then: false 반환
```

---

### 3.2 CommitHeader

#### 시나리오 CH-001: CommitHeader 생성 및 인코딩
```
Given: CommitHeader (seqNo=10, allocTail=50000, catalogRoot=100, stateRoot=200)
When: 4096바이트로 인코딩
Then:
  - offset=0: magic "FXHDR\0\0\0"
  - offset=8: headerVersion=1
  - offset=16: seqNo=10
  - offset=32: allocTail=50000
  - offset=40: catalogRootPageId=100
  - offset=48: stateRootPageId=200
  - offset=56: nextCollectionId > 0
  - offset=64: commitEpochMs > 0
  - offset=4092: CRC32C 값
```

#### 시나리오 CH-002: CommitHeader 디코딩
```
Given: 유효한 CommitHeader 바이트 배열
When: 디코딩
Then: 모든 필드 정확히 복원
```

#### 시나리오 CH-003: 헤더 슬롯 선택 - 둘 다 유효
```
Given:
  - Slot A: seqNo=10, CRC 유효
  - Slot B: seqNo=15, CRC 유효
When: selectHeader() 호출
Then: Slot B 선택 (seqNo가 더 큼)
```

#### 시나리오 CH-004: 헤더 슬롯 선택 - 하나만 유효
```
Given:
  - Slot A: CRC 유효
  - Slot B: CRC 무효
When: selectHeader() 호출
Then: Slot A 선택
```

#### 시나리오 CH-005: 헤더 슬롯 선택 - 둘 다 무효
```
Given:
  - Slot A: CRC 무효
  - Slot B: CRC 무효
When: selectHeader() 호출
Then: FxException(CORRUPTION) 발생
```

#### 시나리오 CH-006: 헤더 교체 (A → B)
```
Given: 현재 Slot A 사용 중 (seqNo=10)
When: Slot B에 seqNo=11로 쓰기
Then:
  - selectHeader()는 Slot B 반환
  - Slot A는 이전 상태 유지 (COW)
```

---

## Week 4: ValueRecord

### 4.1 ValueRecord 인코딩

#### 시나리오 VR-001: ValueRecord 생성
```
Given: payload = "hello world" (11바이트)
When: ValueRecord 인코딩
Then:
  - recMagic="FXRC" (4바이트)
  - recType=1 (VALUE)
  - recFlags=0
  - payloadLen=11 (varint)
  - payloadCrc=CRC32C(payload)
  - payload="hello world"
  - 총 크기 = 4+2+2+varint(11)+4+11 ≈ 24바이트
```

#### 시나리오 VR-002: ValueRecord 디코딩
```
Given: 인코딩된 ValueRecord 바이트 배열
When: 디코딩
Then: payload "hello world" 복원
```

#### 시나리오 VR-003: CRC 검증 성공
```
Given: 유효한 ValueRecord
When: verifyRecordCrc() 호출
Then: true 반환
```

#### 시나리오 VR-004: CRC 검증 실패
```
Given: payload가 손상된 ValueRecord
When: verifyRecordCrc() 호출
Then: false 반환
```

#### 시나리오 VR-005: 큰 payload (1KB)
```
Given: payload = 1024바이트
When: ValueRecord 인코딩
Then:
  - payloadLen varint가 2바이트 사용
  - 총 크기 = 헤더 + 1024
```

---

## 통합 시나리오

### INT-001: 파일 생성부터 페이지 저장까지
```
Given: 새 파일 "test.fx"
When:
  1. FileStorage 생성
  2. Superblock 쓰기 (offset=0)
  3. CommitHeader A 쓰기 (offset=4096)
  4. CommitHeader B 쓰기 (offset=8192)
  5. Allocator로 페이지 할당 (pageId=3)
  6. Page 데이터 쓰기 (offset=12288)
  7. force(true)
Then:
  - 파일 크기 = 16384
  - 모든 데이터 정상 읽기 가능
  - CRC 검증 통과
```

### INT-002: 크래시 시뮬레이션
```
Given: Storage에 일부 데이터 쓰기 완료 (force 안 함)
When:
  1. Storage close (force 없이)
  2. Storage 재오픈
Then:
  - force하지 않은 데이터는 손실 가능
  - CommitHeader로 마지막 일관 상태 복구
```

### INT-003: BATCH 모드 롤백
```
Given: Allocator, Storage
When:
  1. beginBatch()
  2. 페이지 여러 개 할당
  3. Storage에 데이터 쓰기
  4. rollbackBatch()
Then:
  - allocTail은 beginBatch() 이전 값으로 복원
  - 쓴 데이터는 파일에 남지만 dead로 취급
```

---

## 경계 케이스 (Edge Cases)

### EDGE-001: 빈 파일 열기
```
Given: 0바이트 파일
When: FileStorage로 열기
Then: 정상 동작 (size=0 반환)
```

### EDGE-002: 페이지 크기 경계
```
Given: pageSize=4096
When: 4095바이트 페이지 할당 시도
Then: 실패 (페이지는 pageSize 단위)
```

### EDGE-003: allocTail이 파일 크기 초과
```
Given: CommitHeader.allocTail=100000, 실제 파일 크기=50000
When: 복구 시도
Then:
  - 경고 로그
  - allocTail을 파일 크기로 조정
```

### EDGE-004: 동시 접근 (FileLock NONE)
```
Given: FileLockMode.NONE
When: 2개 프로세스가 동시에 같은 파일 열기
Then: 둘 다 성공 (단, 데이터 충돌 가능성)
```

---

## 성능 시나리오

### PERF-001: PageCache 히트율
```
Given: PageCache (maxPages=100)
When: 100개 페이지를 10번 반복 접근
Then:
  - 첫 접근: 100번 미스
  - 이후 접근: 0번 미스 (100% 히트율)
```

### PERF-002: Allocator 속도
```
Given: Allocator
When: 10000개 레코드 연속 할당
Then: 평균 할당 시간 < 1us
```

### PERF-003: Slotted Page 삽입 성능
```
Given: SlottedPage (최대 100 엔트리)
When: 100개 엔트리 삽입
Then: 전체 시간 < 1ms
```

---

## 실패 케이스 (Failure Cases)

### FAIL-001: Storage I/O 오류
```
Given: FileStorage, 디스크 쓰기 권한 없음
When: write() 호출
Then: FxException(IO) 발생
```

### FAIL-002: 메모리 한계 초과
```
Given: MemoryStorage (limit=1MB)
When: 2MB 할당 시도
Then: FxException(OUT_OF_MEMORY) 발생
```

### FAIL-003: 잘못된 페이지 타입
```
Given: 페이지 헤더 (pageType=99)
When: 디코딩 시도
Then: FxException(CORRUPTION) 또는 IllegalStateException 발생
```

---

## 테스트 우선순위

### 높음 (P0)
- FS-001~007: FileStorage 기본 기능
- MS-001~004: MemoryStorage 기본 기능
- SB-001~006: Superblock 인코딩/검증
- CH-001~006: CommitHeader 인코딩/슬롯 선택
- AL-001~005: Allocator 할당 로직

### 중간 (P1)
- PC-001~006: PageCache
- SP-001~005: SlottedPage
- VR-001~005: ValueRecord
- FL-001~004: FileLock

### 낮음 (P2)
- 통합 시나리오
- 성능 시나리오
- 경계 케이스

---

## 체크리스트

### 시나리오 작성 완료
- [x] Week 1: Storage 레이어 (18개 시나리오)
- [x] Week 2: Page 시스템 (17개 시나리오)
- [x] Week 3: Superblock 및 CommitHeader (12개 시나리오)
- [x] Week 4: ValueRecord (5개 시나리오)
- [x] 통합 시나리오 (3개)
- [x] 경계 케이스 (4개)
- [x] 성능 시나리오 (3개)
- [x] 실패 케이스 (3개)

**총 시나리오 수**: 65개

### 다음 단계
- [ ] 각 시나리오를 JUnit 테스트로 변환
- [ ] 테스트 실행 (Red)
- [ ] 구현 코드 작성 (Green)
- [ ] 리팩토링 (Refactor)
- [ ] 회귀 테스트 (모든 테스트 통과)

---

**문서 상태**: ✅ 완료  
**다음 작업**: 테스트 코드 작성
