# Phase 2 Week 1 테스트 시나리오

> **Phase:** Storage 및 Page 관리 - Week 1  
> **목표:** Storage 레이어, Superblock, CommitHeader 구현 및 바이트 레벨 검증  
> **작성일:** 2025-12-24

[← 계획 문서로 돌아가기](00.index.md)

---

## 1. Storage 인터페이스 테스트

### 1.1 FileStorage 테스트

#### TS-2.1.1: 파일 생성 및 초기 상태
**Given:**
- 존재하지 않는 파일 경로
- FileStorage 인스턴스 생성

**When:**
- FileStorage 생성

**Then:**
- 파일이 생성됨
- size() == 0
- 예외 없음

#### TS-2.1.2: 바이트 쓰기 및 읽기
**Given:**
- FileStorage 인스턴스

**When:**
- offset=0에 "FXSTORE\0" (8바이트) 쓰기
- offset=0에서 8바이트 읽기

**Then:**
- 읽은 데이터가 쓴 데이터와 일치
- size() == 8

#### TS-2.1.3: 비연속 오프셋 쓰기
**Given:**
- FileStorage 인스턴스

**When:**
- offset=0에 데이터 쓰기
- offset=4096에 데이터 쓰기

**Then:**
- size() == 4096 + 쓴 데이터 길이
- offset=0 데이터 유지됨
- offset=1~4095는 0x00

#### TS-2.1.4: force() 동기화
**Given:**
- FileStorage에 데이터 쓴 상태

**When:**
- force() 호출

**Then:**
- 예외 없음
- 파일 시스템에 데이터 플러시 확인

#### TS-2.1.5: close() 후 접근
**Given:**
- FileStorage 닫힌 상태

**When:**
- read() 호출

**Then:**
- IOException 발생

### 1.2 MemoryStorage 테스트

#### TS-2.1.6: 메모리 기반 쓰기/읽기
**Given:**
- MemoryStorage 인스턴스

**When:**
- 데이터 쓰고 읽기

**Then:**
- 파일 I/O 없음
- 데이터 일치

#### TS-2.1.7: 메모리 확장
**Given:**
- MemoryStorage 초기 크기 1KB

**When:**
- 10KB 데이터 쓰기

**Then:**
- 자동으로 버퍼 확장
- size() == 10KB

#### TS-2.1.8: force()는 no-op
**Given:**
- MemoryStorage

**When:**
- force() 호출

**Then:**
- 예외 없음 (아무 동작 안 함)

---

## 2. Superblock 테스트

### 2.1 바이트 레벨 레이아웃 검증

#### TS-2.2.1: Magic 필드 검증
**Given:**
- 새 Superblock 생성
- formatVersion=1, pageSize=4096, createdAt=현재 시각

**When:**
- encode() 호출

**Then:**
- **바이트 [0-7]** == `0x46 58 53 54 4F 52 45 00` ("FXSTORE\0")

#### TS-2.2.2: FormatVersion 필드 검증 (LE)
**Given:**
- formatVersion=1

**When:**
- encode()

**Then:**
- **바이트 [8-11]** == `0x01 00 00 00` (Little-Endian)

#### TS-2.2.3: PageSize 필드 검증
**Given:**
- pageSize=4096

**When:**
- encode()

**Then:**
- **바이트 [12-15]** == `0x00 10 00 00` (LE, 4096 = 0x1000)

#### TS-2.2.4: FeatureFlags 필드 검증
**Given:**
- featureFlags=0

**When:**
- encode()

**Then:**
- **바이트 [16-23]** == `0x00 00 00 00 00 00 00 00`

#### TS-2.2.5: CreatedAt 필드 검증
**Given:**
- createdAt=1609459200000L (2021-01-01 00:00:00 UTC)

**When:**
- encode()

**Then:**
- **바이트 [24-31]** == `0x00 E0 91 54 76 01 00 00` (LE)

#### TS-2.2.6: Reserved 영역 검증
**Given:**
- Superblock 생성

**When:**
- encode()

**Then:**
- **바이트 [32-4091]** 모두 `0x00`

#### TS-2.2.7: CRC32C 계산 및 검증
**Given:**
- 완전한 Superblock

**When:**
- encode() 호출
- CRC 계산: data[0-4091]

**Then:**
- **바이트 [4092-4095]** == 계산된 CRC32C (LE)
- verify() == true

#### TS-2.2.8: CRC 손상 감지
**Given:**
- 올바른 Superblock 바이트 배열
- 바이트[100]을 임의로 변경

**When:**
- decode() 후 verify()

**Then:**
- verify() == false

### 2.2 Encode/Decode 라운드트립

#### TS-2.2.9: 직렬화 역직렬화 일치
**Given:**
- Superblock(formatVersion=1, pageSize=4096, featureFlags=0, createdAt=now)

**When:**
- encode() → decode()

**Then:**
- 복원된 객체의 모든 필드가 원본과 일치

#### TS-2.2.10: 잘못된 Magic 감지
**Given:**
- 바이트 배열의 Magic을 "WRONGMAG"로 변경

**When:**
- decode()

**Then:**
- FxException(CORRUPTION) 발생

#### TS-2.2.11: 지원되지 않는 FormatVersion
**Given:**
- formatVersion=999

**When:**
- decode()

**Then:**
- FxException(VERSION_MISMATCH) 발생

---

## 3. CommitHeader 테스트

### 3.1 바이트 레벨 레이아웃 검증

#### TS-2.3.1: Magic 필드 검증
**Given:**
- 새 CommitHeader 생성

**When:**
- encode()

**Then:**
- **바이트 [0-7]** == `0x46 58 48 44 52 00 00 00` ("FXHDR\0\0\0")

#### TS-2.3.2: HeaderVersion 필드 검증
**Given:**
- headerVersion=1

**When:**
- encode()

**Then:**
- **바이트 [8-11]** == `0x01 00 00 00` (LE)

#### TS-2.3.3: SeqNo 필드 검증
**Given:**
- seqNo=123L

**When:**
- encode()

**Then:**
- **바이트 [16-23]** == `0x7B 00 00 00 00 00 00 00` (LE, 123 = 0x7B)

#### TS-2.3.4: CommittedFlags 필드 검증
**Given:**
- committedFlags=1L (SYNC)

**When:**
- encode()

**Then:**
- **바이트 [24-31]** == `0x01 00 00 00 00 00 00 00` (LE)

#### TS-2.3.5: AllocTail 필드 검증
**Given:**
- allocTail=1048576L (1MB)

**When:**
- encode()

**Then:**
- **바이트 [32-39]** == `0x00 00 10 00 00 00 00 00` (LE, 1MB = 0x100000)

#### TS-2.3.6: CatalogRootPageId 필드 검증
**Given:**
- catalogRootPageId=42L

**When:**
- encode()

**Then:**
- **바이트 [40-47]** == `0x2A 00 00 00 00 00 00 00` (LE, 42 = 0x2A)

#### TS-2.3.7: StateRootPageId 필드 검증
**Given:**
- stateRootPageId=84L

**When:**
- encode()

**Then:**
- **바이트 [48-55]** == `0x54 00 00 00 00 00 00 00` (LE, 84 = 0x54)

#### TS-2.3.8: NextCollectionId 필드 검증
**Given:**
- nextCollectionId=100L

**When:**
- encode()

**Then:**
- **바이트 [56-63]** == `0x64 00 00 00 00 00 00 00` (LE, 100 = 0x64)

#### TS-2.3.9: CommitEpochMs 필드 검증
**Given:**
- commitEpochMs=1609459200000L

**When:**
- encode()

**Then:**
- **바이트 [64-71]** == `0x00 E0 91 54 76 01 00 00` (LE)

#### TS-2.3.10: Reserved 영역 검증
**Given:**
- CommitHeader 생성

**When:**
- encode()

**Then:**
- **바이트 [72-4091]** 모두 `0x00`

#### TS-2.3.11: CRC32C 검증
**Given:**
- 완전한 CommitHeader

**When:**
- encode()
- CRC 계산: data[0-4091]

**Then:**
- **바이트 [4092-4095]** == 계산된 CRC32C (LE)
- verify() == true

### 3.2 Encode/Decode 라운드트립

#### TS-2.3.12: 직렬화 역직렬화 일치
**Given:**
- CommitHeader(seqNo=1, catalogRoot=10, stateRoot=20, allocTail=4096, ...)

**When:**
- encode() → decode()

**Then:**
- 모든 필드 일치

#### TS-2.3.13: CRC 손상 감지
**Given:**
- 올바른 CommitHeader 바이트 배열
- seqNo 필드 변조

**When:**
- decode() 후 verify()

**Then:**
- verify() == false

### 3.3 CommitHeader 선택 로직

#### TS-2.3.14: 두 슬롯 모두 유효, seqNo 비교
**Given:**
- Slot A: seqNo=10, valid CRC
- Slot B: seqNo=15, valid CRC

**When:**
- selectHeader(slotA, slotB)

**Then:**
- Slot B 선택 (더 큰 seqNo)

#### TS-2.3.15: Slot A 손상, Slot B 유효
**Given:**
- Slot A: 잘못된 CRC
- Slot B: seqNo=5, valid CRC

**When:**
- selectHeader(slotA, slotB)

**Then:**
- Slot B 선택

#### TS-2.3.16: 두 슬롯 모두 손상
**Given:**
- Slot A: 잘못된 CRC
- Slot B: 잘못된 CRC

**When:**
- selectHeader(slotA, slotB)

**Then:**
- FxException(CORRUPTION) 발생

---

## 4. 통합 테스트

### 4.1 Superblock 파일 쓰기/읽기

#### TS-2.4.1: Superblock 저장 및 복원
**Given:**
- FileStorage
- Superblock(formatVersion=1, pageSize=4096)

**When:**
1. Superblock.encode()를 offset=0에 쓰기
2. force()
3. FileStorage 닫기
4. 다시 열기
5. offset=0에서 4096바이트 읽기
6. Superblock.decode()

**Then:**
- 복원된 Superblock의 모든 필드 일치
- CRC 검증 통과

### 4.2 CommitHeader A/B 슬롯 관리

#### TS-2.4.2: 교대로 슬롯 쓰기
**Given:**
- FileStorage
- CommitHeader(seqNo=1) → Slot A
- CommitHeader(seqNo=2) → Slot B

**When:**
1. Slot A에 seqNo=1 쓰기
2. Slot B에 seqNo=2 쓰기
3. selectHeader()

**Then:**
- seqNo=2인 Slot B 선택

#### TS-2.4.3: 크래시 시뮬레이션 (Slot B 미완성)
**Given:**
- Slot A: seqNo=1, valid
- Slot B: 쓰기 중단 (CRC 없음)

**When:**
- selectHeader()

**Then:**
- Slot A 선택 (Slot B 무효)

---

## 5. 성능 테스트

### 5.1 Storage 성능

#### TS-2.5.1: 대량 순차 쓰기
**Given:**
- FileStorage
- 1MB 데이터 (256 * 4KB)

**When:**
- 4KB씩 256회 순차 쓰기

**Then:**
- 총 시간 < 1초
- 데이터 정확성 검증

#### TS-2.5.2: 랜덤 읽기 성능
**Given:**
- FileStorage에 1MB 데이터 저장

**When:**
- 랜덤 오프셋에서 4KB씩 1000회 읽기

**Then:**
- 평균 읽기 시간 < 1ms

---

## 6. 예외 케이스

### 6.1 Storage 예외

#### TS-2.6.1: 쓰기 권한 없는 파일
**Given:**
- 읽기 전용 파일

**When:**
- FileStorage.write()

**Then:**
- FxException(IO) 발생

#### TS-2.6.2: 디스크 공간 부족 (Mock)
**Given:**
- Mock Storage로 디스크 부족 시뮬레이션

**When:**
- 대량 데이터 쓰기

**Then:**
- FxException(IO) 발생

### 6.2 Superblock 예외

#### TS-2.6.3: 너무 작은 바이트 배열
**Given:**
- 100바이트 배열

**When:**
- Superblock.decode()

**Then:**
- FxException(CORRUPTION) 발생

#### TS-2.6.4: 잘못된 PageSize
**Given:**
- pageSize=1024 (4096, 8192, 16384만 유효)

**When:**
- Superblock 생성

**Then:**
- FxException(ILLEGAL_ARGUMENT) 발생

---

## 7. 엣지 케이스

### 7.1 Boundary 값

#### TS-2.7.1: SeqNo 오버플로우 근처
**Given:**
- seqNo=Long.MAX_VALUE - 1

**When:**
- encode() → decode()

**Then:**
- 값 일치

#### TS-2.7.2: AllocTail = 0
**Given:**
- allocTail=0 (빈 파일)

**When:**
- encode() → decode()

**Then:**
- allocTail == 0

---

## 체크리스트

- [ ] 모든 테스트 시나리오 검토
- [ ] **바이트 레벨 검증 시나리오 포함 확인** ✨
- [ ] 테스트 코드 작성 완료
- [ ] 모든 테스트 통과
- [ ] 품질 평가 준비

---

**작성일:** 2025-12-24  
**검토자:** -  
**승인일:** -
