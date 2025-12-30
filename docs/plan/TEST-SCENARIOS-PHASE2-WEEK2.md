# Phase 2 Week 2 테스트 시나리오

> **Phase:** Storage 및 Page 관리 - Week 2  
> **목표:** Page 헤더, Slotted Page, CRC 검증 구현  
> **작성일:** 2025-12-24

[← 계획 문서로 돌아가기](00.index.md)

---

## 1. PageKind Enum 테스트

### TS-2.2.1: PageKind 값 검증
**Given:**
- PageKind enum 정의

**When:**
- 모든 값 조회

**Then:**
- BTREE_INTERNAL, BTREE_LEAF, OST_INTERNAL, OST_LEAF, CATALOG, STATE 존재
- ordinal() 값이 일관성 있음

---

## 2. PageHeader 테스트

### 2.1 바이트 레벨 레이아웃

#### TS-2.2.2: PageKind 필드 검증
**Given:**
- PageHeader(kind=BTREE_LEAF, pageSize=4096, usedBytes=100, pageId=42)

**When:**
- encode()

**Then:**
- **바이트 [0-1]:** BTREE_LEAF ordinal (2바이트 LE)

#### TS-2.2.3: PageSize 필드 검증
**Given:**
- PageHeader(pageSize=4096)

**When:**
- encode()

**Then:**
- **바이트 [2-5]:** 4096 (0x00 10 00 00 LE)

#### TS-2.2.4: UsedBytes 필드 검증
**Given:**
- PageHeader(usedBytes=256)

**When:**
- encode()

**Then:**
- **바이트 [6-9]:** 256 (0x00 01 00 00 LE)

#### TS-2.2.5: PageId 필드 검증
**Given:**
- PageHeader(pageId=9876543210L)

**When:**
- encode()

**Then:**
- **바이트 [10-17]:** 9876543210 (8바이트 LE)

#### TS-2.2.6: Reserved 영역 검증
**Given:**
- PageHeader 생성

**When:**
- encode()

**Then:**
- **바이트 [18-31]:** 모두 0x00

### 2.2 Encode/Decode 라운드트립

#### TS-2.2.7: 직렬화 역직렬화 일치
**Given:**
- PageHeader(kind=CATALOG, pageSize=8192, usedBytes=500, pageId=100)

**When:**
- encode() → decode()

**Then:**
- 모든 필드 일치

#### TS-2.2.8: 작은 바이트 배열 예외
**Given:**
- 20바이트 배열

**When:**
- PageHeader.decode()

**Then:**
- FxException(CORRUPTION)

---

## 3. SlottedPage 테스트

### 3.1 기본 동작

#### TS-2.2.9: 페이지 초기화
**Given:**
- SlottedPage(pageId=1, pageSize=4096)

**When:**
- 생성 직후

**Then:**
- freeSpace() == 4096 - 32 (헤더 크기)
- slotCount() == 0

#### TS-2.2.10: 단일 레코드 삽입
**Given:**
- SlottedPage
- 레코드 = "RECORD_1" (8바이트)

**When:**
- insert(record)

**Then:**
- 슬롯 인덱스 0 반환
- freeSpace() 감소 (8바이트 + 슬롯 메타데이터)
- get(0) == record

#### TS-2.2.11: 다중 레코드 삽입
**Given:**
- SlottedPage

**When:**
- insert("AAA") → slot 0
- insert("BBBB") → slot 1
- insert("CCCCC") → slot 2

**Then:**
- slotCount() == 3
- get(0) == "AAA"
- get(1) == "BBBB"
- get(2) == "CCCCC"

#### TS-2.2.12: 레코드 삭제
**Given:**
- SlottedPage with slots [0: "AAA", 1: "BBB", 2: "CCC"]

**When:**
- delete(1)

**Then:**
- slot 1 비활성화
- get(1) → null 또는 예외
- get(0) == "AAA" (유지)
- get(2) == "CCC" (유지)

#### TS-2.2.13: 삭제 후 재삽입
**Given:**
- SlottedPage, delete(1) 후

**When:**
- insert("DDD")

**Then:**
- 삭제된 슬롯 재사용 또는 새 슬롯 할당
- 레코드 정확히 저장됨

### 3.2 공간 관리

#### TS-2.2.14: 페이지 Full 감지
**Given:**
- SlottedPage(pageSize=1024)
- 900바이트 이미 사용

**When:**
- insert(200바이트 레코드)

**Then:**
- FxException(NO_SPACE) 또는 -1 반환

#### TS-2.2.15: Fragmentation 계산
**Given:**
- SlottedPage with slots [0: 100바이트, 1: 50바이트, 2: 75바이트]
- delete(1)

**When:**
- fragmentation() 계산

**Then:**
- 50바이트의 hole 존재 감지

#### TS-2.2.16: Compaction
**Given:**
- SlottedPage with fragmentation

**When:**
- compact()

**Then:**
- 모든 레코드 연속된 공간에 재배치
- freeSpace() 증가 (fragmentation 제거)
- 슬롯 인덱스 유지

### 3.3 슬롯 포인터

#### TS-2.2.17: 슬롯 메타데이터 검증
**Given:**
- SlottedPage with insert("TEST")

**When:**
- 슬롯 0의 (offset, length) 조회

**Then:**
- offset는 페이지 내 유효 범위
- length == 4

#### TS-2.2.18: 슬롯 순서 유지
**Given:**
- SlottedPage

**When:**
- insert("A"), insert("B"), insert("C")
- delete(1)
- insert("D")

**Then:**
- slot 0 → "A"
- slot 1 → deleted (또는 "D" 재사용)
- slot 2 → "C"

### 3.4 예외 케이스

#### TS-2.2.19: 잘못된 슬롯 인덱스
**Given:**
- SlottedPage with 3 slots

**When:**
- get(10)

**Then:**
- FxException(OUT_OF_BOUNDS) 또는 null

#### TS-2.2.20: 중복 삭제
**Given:**
- SlottedPage with slot 1

**When:**
- delete(1)
- delete(1) 다시 호출

**Then:**
- 예외 없음 또는 idempotent

---

## 4. Page CRC 테스트

### 4.1 CRC 계산

#### TS-2.2.21: CRC 계산 범위
**Given:**
- 4096바이트 페이지
- CRC는 마지막 4바이트

**When:**
- CRC 계산

**Then:**
- [0, 4091] 범위만 계산
- [4092-4095]에 CRC 저장

#### TS-2.2.22: CRC 검증 성공
**Given:**
- 올바른 CRC를 가진 페이지

**When:**
- verify()

**Then:**
- true 반환

#### TS-2.2.23: CRC 손상 감지
**Given:**
- 페이지 바이트[100] 변조

**When:**
- verify()

**Then:**
- false 반환

---

## 5. 통합 테스트

### 5.1 Page 저장 및 복원

#### TS-2.2.24: SlottedPage 파일 I/O
**Given:**
- SlottedPage with 여러 레코드
- FileStorage

**When:**
1. encode() → Storage.write()
2. Storage.read() → decode()

**Then:**
- 복원된 페이지의 모든 레코드 일치
- CRC 검증 통과

### 5.2 대용량 테스트

#### TS-2.2.25: 1000개 슬롯 삽입
**Given:**
- SlottedPage(pageSize=16384)

**When:**
- 10바이트 레코드 1000개 삽입

**Then:**
- 공간이 허용하는 한 성공
- 모든 레코드 정확히 저장
- get() 성능 < 1ms

---

## 6. 성능 테스트

### 6.1 Insert 성능

#### TS-2.2.26: 순차 삽입 성능
**Given:**
- SlottedPage

**When:**
- 100바이트 레코드 100개 삽입

**Then:**
- 평균 삽입 시간 < 0.1ms

### 6.2 Compact 성능

#### TS-2.2.27: Compaction 성능
**Given:**
- SlottedPage with 50% fragmentation

**When:**
- compact()

**Then:**
- 완료 시간 < 10ms (4KB 페이지)

---

## 7. 엣지 케이스

### 7.1 Boundary 값

#### TS-2.2.28: 최대 레코드 크기
**Given:**
- SlottedPage(pageSize=4096)
- 레코드 크기 = 4096 - 32 - 슬롯메타

**When:**
- insert(maxRecord)

**Then:**
- 성공
- freeSpace() == 0

#### TS-2.2.29: 빈 레코드
**Given:**
- SlottedPage

**When:**
- insert(new byte[0])

**Then:**
- 성공 또는 명확한 예외

---

## 체크리스트

- [ ] PageKind enum 정의
- [ ] PageHeader 바이트 레벨 검증
- [ ] SlottedPage insert/get/delete 구현
- [ ] Compaction 구현
- [ ] CRC 검증 구현
- [ ] 모든 테스트 작성
- [ ] 모든 테스트 통과
- [ ] 품질 평가 준비

---

**작성일:** 2025-12-24  
**검토자:** -  
**승인일:** -
