# Phase 2 Week 4: Allocator 테스트 시나리오

> **Phase:** Storage 및 Page 관리 - Week 4  
> **구현 대상:** Allocator (Append-only 할당자)  
> **작성일:** 2025-12-24

[← 계획 문서로 돌아가기](00.index.md)

---

## 개요

Week 4에서는 **Append-only Allocator**를 구현합니다. Allocator는 페이지와 레코드 할당을 관리하며, 각각 적절한 정렬을 보장합니다.

**핵심 요구사항:**
- 페이지: pageSize 정렬 (4096, 8192, 또는 16384 바이트)
- 레코드: 8바이트 정렬
- Append-only: allocTail은 항상 증가만 함 (컴팩션 제외)
- BATCH 모드 지원: pending 할당 관리

**참고 문서:**
- [02.architecture.md](../../spec/lagacy/02.architecture.md) - Section 3.3: Allocator 구현
- [01.api.md](../../spec/lagacy/01.api.md) - CommitMode

---

## 시나리오 그룹 A: 기본 할당 연산

### TS-2.4.1: 초기 상태 생성

**목적:** Allocator가 올바른 초기 상태로 생성되는지 검증

**전제 조건:**
- pageSize = 4096
- 초기 allocTail = 12288 (Superblock + 2 CommitHeaders)

**실행 단계:**
1. Allocator(pageSize=4096, initialTail=12288) 생성
2. getAllocTail() 호출

**예상 결과:**
- allocTail == 12288
- 예외 발생 없음

**커버 기준:**
- 기준 1 (Plan-Code 정합성): Allocator 초기화 명세 준수
- 기준 3 (테스트 커버리지): 생성자 커버

---

### TS-2.4.2: 첫 페이지 할당

**목적:** 첫 페이지가 올바르게 정렬되어 할당되는지 검증

**전제 조건:**
- allocTail = 12288 (이미 4096 정렬됨)
- pageSize = 4096

**실행 단계:**
1. allocatePage() 호출
2. 반환된 offset 확인
3. getAllocTail() 호출

**예상 결과:**
- 반환 offset = 12288
- 새 allocTail = 16384 (12288 + 4096)
- offset % 4096 == 0 (정렬 확인)

**커버 기준:**
- 기준 1: allocatePage() 명세 준수
- 기준 3: allocatePage 메서드 커버
- 기준 6 (성능): O(1) 할당 시간

---

### TS-2.4.3: 연속 페이지 할당

**목적:** 여러 페이지가 순차적으로 정렬되어 할당되는지 검증

**전제 조건:**
- allocTail = 12288
- pageSize = 4096

**실행 단계:**
1. allocatePage() 호출 3회
2. 각 호출의 반환 offset 저장

**예상 결과:**
- offset1 = 12288
- offset2 = 16384
- offset3 = 20480
- 모든 offset이 4096의 배수
- 새 allocTail = 24576

**커버 기준:**
- 기준 1: 연속 할당 동작 검증
- 기준 3: 반복 호출 경로 커버

---

### TS-2.4.4: 첫 레코드 할당

**목적:** 레코드가 8바이트 정렬되어 할당되는지 검증

**전제 조건:**
- allocTail = 12288 (8로 나누어 떨어짐)
- recordSize = 100 바이트

**실행 단계:**
1. allocateRecord(100) 호출
2. 반환된 offset 확인
3. getAllocTail() 호출

**예상 결과:**
- 반환 offset = 12288 (이미 8 정렬됨)
- 새 allocTail = 12388 (12288 + 100)
- offset % 8 == 0 (정렬 확인)

**커버 기준:**
- 기준 1: allocateRecord() 명세 준수
- 기준 3: allocateRecord 메서드 커버

---

### TS-2.4.5: 레코드 할당 시 정렬 조정

**목적:** allocTail이 8로 나누어 떨어지지 않을 때 정렬 조정 검증

**전제 조건:**
- allocTail = 12289 (8로 나누어 떨어지지 않음)
- recordSize = 50 바이트

**실행 단계:**
1. allocateRecord(50) 호출
2. 반환된 offset 확인

**예상 결과:**
- 반환 offset = 12296 (12289를 8로 올림)
  - 12289 / 8 = 1536.125 → 올림 = 1537
  - 1537 * 8 = 12296
- 새 allocTail = 12346 (12296 + 50)
- offset % 8 == 0

**커버 기준:**
- 기준 1: 정렬 로직 정확성
- 기준 3: 정렬 조정 경로 커버
- 기준 5 (예외 처리): 경계 조건 처리

---

### TS-2.4.6: 혼합 할당 (페이지 + 레코드)

**목적:** 페이지와 레코드 할당을 섞어 사용할 때 정렬 유지 검증

**전제 조건:**
- allocTail = 12288
- pageSize = 4096

**실행 단계:**
1. allocateRecord(100) → offset1
2. allocatePage() → offset2
3. allocateRecord(200) → offset3
4. allocatePage() → offset4

**예상 결과:**
- offset1 = 12288, allocTail = 12388
- offset2 = 16384 (12388을 4096로 올림), allocTail = 20480
- offset3 = 20480, allocTail = 20680
- offset4 = 24576 (20680을 4096로 올림), allocTail = 28672
- 모든 페이지 offset은 4096의 배수
- 모든 레코드 offset은 8의 배수

**커버 기준:**
- 기준 1: 혼합 사용 시나리오
- 기준 3: 복합 경로 커버
- 기준 5: 정렬 일관성

---

## 시나리오 그룹 B: 다양한 페이지 크기

### TS-2.4.7: pageSize = 8192 정렬

**목적:** 8K 페이지 크기에서 정렬 검증

**전제 조건:**
- pageSize = 8192
- allocTail = 24576 (8192의 배수)

**실행 단계:**
1. allocatePage() 호출 2회

**예상 결과:**
- offset1 = 24576
- offset2 = 32768
- offset1 % 8192 == 0
- offset2 % 8192 == 0
- allocTail = 40960

**커버 기준:**
- 기준 1: 다양한 pageSize 지원
- 기준 3: 8K 경로 커버

---

### TS-2.4.8: pageSize = 16384 정렬

**목적:** 16K 페이지 크기에서 정렬 검증

**전제 조건:**
- pageSize = 16384
- allocTail = 49152 (16384의 배수)

**실행 단계:**
1. allocatePage() 호출
2. allocateRecord(500) 호출

**예상 결과:**
- page offset = 49152
- record offset = 65536 (49152 + 16384)
- page offset % 16384 == 0
- record offset % 8 == 0
- allocTail = 66036

**커버 기준:**
- 기준 1: 16K 페이지 지원
- 기준 3: 16K 경로 커버

---

## 시나리오 그룹 C: BATCH 모드 지원

### TS-2.4.9: BATCH 모드 진입 (beginPending)

**목적:** BATCH 모드 진입 시 pending 상태 생성 검증

**전제 조건:**
- allocTail = 12288
- BATCH 모드 비활성 상태

**실행 단계:**
1. beginPending() 호출
2. allocatePage() 호출
3. getAllocTail() 호출
4. getCommittedAllocTail() 호출

**예상 결과:**
- beginPending() 후 pending 상태 활성화
- getAllocTail() = 16384 (pending에 반영)
- getCommittedAllocTail() = 12288 (커밋 전 상태 유지)

**커버 기준:**
- 기준 1: BATCH 모드 명세 준수
- 기준 3: beginPending 메서드 커버
- 기준 4 (코드 가독성): 상태 분리 명확성

---

### TS-2.4.10: BATCH 모드에서 pending 할당

**목적:** Pending 상태에서 할당이 메모리에만 반영되는지 검증

**전제 조건:**
- allocTail = 12288
- BATCH 모드 활성

**실행 단계:**
1. beginPending()
2. allocatePage() → offset1
3. allocateRecord(100) → offset2
4. getAllocTail() 확인

**예상 결과:**
- offset1 = 12288
- offset2 = 16384
- getAllocTail() = 16484 (pending에만 반영)
- getCommittedAllocTail() = 12288 (변경 없음)

**커버 기준:**
- 기준 1: Pending 격리 검증
- 기준 3: Pending 할당 경로 커버
- 기준 5: 상태 일관성

---

### TS-2.4.11: BATCH 커밋 (commitPending)

**목적:** Pending 상태를 커밋하여 실제 allocTail 반영 검증

**전제 조건:**
- allocTail = 12288
- pending allocTail = 20480

**실행 단계:**
1. commitPending() 호출
2. getCommittedAllocTail() 호출
3. getAllocTail() 호출

**예상 결과:**
- commitPending() 성공
- getCommittedAllocTail() = 20480 (pending 반영)
- getAllocTail() = 20480
- pending 상태 비활성화

**커버 기준:**
- 기준 1: 커밋 프로토콜 준수
- 기준 3: commitPending 메서드 커버
- 기준 7 (크래시 일관성): 원자적 커밋

---

### TS-2.4.12: BATCH 롤백 (rollbackPending)

**목적:** Pending 상태를 롤백하여 이전 상태로 복원 검증

**전제 조건:**
- allocTail = 12288
- pending allocTail = 20480

**실행 단계:**
1. rollbackPending() 호출
2. getAllocTail() 호출
3. 새로 allocatePage() 호출

**예상 결과:**
- rollbackPending() 성공
- getAllocTail() = 12288 (pending 폐기)
- 새 페이지 offset = 12288 (이전 상태에서 재시작)
- pending 상태 비활성화

**커버 기준:**
- 기준 1: 롤백 프로토콜 준수
- 기준 3: rollbackPending 메서드 커버
- 기준 5: 상태 복원 정확성

---

### TS-2.4.13: 중첩 BATCH 모드 시도 (예외)

**목적:** 이미 BATCH 모드인 상태에서 다시 beginPending 호출 시 예외 발생 검증

**전제 조건:**
- pending 상태 활성화됨

**실행 단계:**
1. beginPending() 호출 (이미 활성 상태)

**예상 결과:**
- IllegalStateException 발생
- 메시지: "Already in pending mode"

**커버 기준:**
- 기준 5 (예외 처리): 잘못된 상태 전이 방지
- 기준 3: 예외 경로 커버

---

### TS-2.4.14: 비활성 상태에서 커밋 시도 (예외)

**목적:** Pending 상태가 아닐 때 commitPending 호출 시 예외 발생 검증

**전제 조건:**
- pending 상태 비활성화

**실행 단계:**
1. commitPending() 호출

**예상 결과:**
- IllegalStateException 발생
- 메시지: "Not in pending mode"

**커버 기준:**
- 기준 5: 상태 전제 조건 검증
- 기준 3: 예외 경로 커버

---

## 시나리오 그룹 D: 경계 조건 및 예외 처리

### TS-2.4.15: 0 크기 레코드 할당 (예외)

**목적:** 크기가 0인 레코드 할당 시도 시 예외 발생 검증

**전제 조건:**
- allocTail = 12288

**실행 단계:**
1. allocateRecord(0) 호출

**예상 결과:**
- IllegalArgumentException 발생
- 메시지: "Record size must be positive"

**커버 기준:**
- 기준 5: 입력 검증
- 기준 3: 경계 조건 커버

---

### TS-2.4.16: 음수 크기 레코드 할당 (예외)

**목적:** 음수 크기 레코드 할당 시도 시 예외 발생 검증

**전제 조건:**
- allocTail = 12288

**실행 단계:**
1. allocateRecord(-100) 호출

**예상 결과:**
- IllegalArgumentException 발생
- 메시지: "Record size must be positive"

**커버 기준:**
- 기준 5: 입력 검증
- 기준 3: 경계 조건 커버

---

### TS-2.4.17: 매우 큰 레코드 할당 (허용)

**목적:** 큰 레코드(1MB)도 정상 할당되는지 검증

**전제 조건:**
- allocTail = 12288
- recordSize = 1,048,576 (1 MiB)

**실행 단계:**
1. allocateRecord(1_048_576) 호출
2. getAllocTail() 확인

**예상 결과:**
- 반환 offset = 12288
- 새 allocTail = 1,060,864 (12288 + 1,048,576)
- offset % 8 == 0
- 예외 발생 없음

**커버 기준:**
- 기준 1: 큰 레코드 지원 (API 명세 1 MiB 한계)
- 기준 3: 큰 값 경로 커버
- 기준 6: 큰 할당 성능

---

### TS-2.4.18: allocTail 오버플로우 (경계)

**목적:** allocTail이 Long.MAX_VALUE에 가까울 때 동작 검증

**전제 조건:**
- allocTail = Long.MAX_VALUE - 10000
- pageSize = 4096

**실행 단계:**
1. allocatePage() 호출

**예상 결과:**
- 반환 offset = Long.MAX_VALUE - 4095 (4096으로 정렬)
- 새 allocTail 계산 시 오버플로우 체크
- 오버플로우 발생 시 IllegalStateException
- 메시지: "Allocation overflow"

**커버 기준:**
- 기준 5: 오버플로우 방지
- 기준 3: 극한 경계 조건 커버

---

### TS-2.4.19: 잘못된 pageSize로 생성 (예외)

**목적:** 유효하지 않은 pageSize로 Allocator 생성 시 예외 발생 검증

**전제 조건:**
- pageSize = 2048 (유효 값: 4096, 8192, 16384만 허용)

**실행 단계:**
1. new Allocator(2048, 12288) 호출

**예상 결과:**
- IllegalArgumentException 발생
- 메시지: "Invalid pageSize: must be 4096, 8192, or 16384"

**커버 기준:**
- 기준 1: PageSize enum 준수
- 기준 5: 생성자 검증
- 기준 3: 생성자 예외 경로 커버

---

### TS-2.4.20: 음수 initialTail로 생성 (예외)

**목적:** 음수 initialTail로 생성 시 예외 발생 검증

**전제 조건:**
- pageSize = 4096
- initialTail = -100

**실행 단계:**
1. new Allocator(4096, -100) 호출

**예상 결과:**
- IllegalArgumentException 발생
- 메시지: "initialTail must be non-negative"

**커버 기준:**
- 기준 5: 초기값 검증
- 기준 3: 생성자 예외 경로 커버

---

## 시나리오 그룹 E: 불변식 검증

### TS-2.4.21: INV-9 검증 - allocTail 단조 증가

**목적:** allocTail이 항상 증가만 하는지 검증 (컴팩션 제외)

**전제 조건:**
- allocTail = 12288

**실행 단계:**
1. initialTail = getAllocTail()
2. allocatePage() 호출 10회
3. 각 호출 후 getAllocTail() 확인

**예상 결과:**
- 모든 getAllocTail() 결과가 이전보다 큼
- tail[i] < tail[i+1] for all i

**커버 기준:**
- 기준 1: 불변식 INV-9 준수
- 기준 5: 불변식 보장

---

### TS-2.4.22: BATCH 커밋 전후 allocTail 불변

**목적:** BATCH 모드에서 커밋 전까지 committedAllocTail이 변하지 않는지 검증

**전제 조건:**
- allocTail = 12288

**실행 단계:**
1. beginPending()
2. initialCommitted = getCommittedAllocTail()
3. allocatePage() 5회
4. afterPendingCommitted = getCommittedAllocTail()
5. commitPending()
6. finalCommitted = getCommittedAllocTail()

**예상 결과:**
- initialCommitted == afterPendingCommitted (12288)
- finalCommitted > initialCommitted (pending 반영됨)

**커버 기준:**
- 기준 1: BATCH 모드 격리
- 기준 5: 상태 불변식

---

## 시나리오 그룹 F: 통합 시나리오

### TS-2.4.23: 복잡한 BATCH 워크플로우

**목적:** 실제 사용 패턴과 유사한 복합 시나리오 검증

**전제 조건:**
- allocTail = 12288
- pageSize = 4096

**실행 단계:**
1. allocatePage() → p1
2. beginPending()
3. allocateRecord(100) → r1
4. allocatePage() → p2
5. allocateRecord(200) → r2
6. rollbackPending()
7. beginPending()
8. allocatePage() → p3
9. allocateRecord(150) → r3
10. commitPending()
11. allocatePage() → p4

**예상 결과:**
- p1 = 12288
- r1, p2, r2는 롤백으로 무효화 (재할당 안 됨)
- p3 = 16384 (p1 이후)
- r3 = 20480
- p4 = 24576
- 최종 allocTail = 28672

**커버 기준:**
- 기준 1: 복합 시나리오
- 기준 3: 복잡한 경로 커버
- 기준 5: 롤백 후 재시작 정확성

---

### TS-2.4.24: 대량 할당 성능

**목적:** 대량 할당 시 성능이 O(1)인지 검증

**전제 조건:**
- allocTail = 12288
- 할당 횟수 = 100,000

**실행 단계:**
1. 시작 시간 기록
2. allocatePage() 100,000회 호출
3. 종료 시간 기록
4. 평균 시간 계산

**예상 결과:**
- 총 소요 시간 < 500ms (Java 8 기준, 평균 하드웨어)
- 평균 할당 시간 < 5μs
- 메모리 누수 없음

**커버 기준:**
- 기준 6 (성능): O(1) 할당 시간 검증
- 기준 3: 대량 반복 경로 커버

---

### TS-2.4.25: 메모리 누수 검증

**목적:** 할당 및 롤백 반복 시 메모리 누수 없음을 검증

**전제 조건:**
- allocTail = 12288

**실행 단계:**
1. GC 실행하여 초기 메모리 기준 확보
2. for (i = 0; i < 1000; i++) {
     beginPending();
     allocatePage() 100회;
     rollbackPending();
   }
3. GC 실행
4. 메모리 사용량 확인

**예상 결과:**
- 초기 메모리 대비 증가량 < 1MB
- 메모리 누수 없음

**커버 기준:**
- 기준 5: 메모리 안정성
- 기준 6: 메모리 효율성

---

## 요약

**총 시나리오 수:** 25개

**그룹별 분포:**
- A: 기본 할당 연산 (6개)
- B: 다양한 페이지 크기 (2개)
- C: BATCH 모드 지원 (6개)
- D: 경계 조건 및 예외 처리 (6개)
- E: 불변식 검증 (2개)
- F: 통합 시나리오 (3개)

**커버하는 품질 기준:**
- ✅ 기준 1 (Plan-Code 정합성): 24개 시나리오
- ✅ 기준 2 (SOLID 원칙): 상태 분리, 단일 책임 검증
- ✅ 기준 3 (테스트 커버리지): 모든 public 메서드 커버
- ✅ 기준 4 (코드 가독성): 명확한 상태 분리 검증
- ✅ 기준 5 (예외 처리 및 안정성): 11개 예외 시나리오
- ✅ 기준 6 (성능 효율성): 성능 테스트 2개
- ✅ 기준 7 (크래시 일관성): 커밋/롤백 원자성 검증

---

## 다음 단계

1. **테스트 코드 작성**: `AllocatorTest.java` 작성
2. **구현 코드 작성**: `Allocator.java` 작성
3. **테스트 실행**: 모든 테스트 통과 확인
4. **회귀 테스트**: Phase 2 Week 1-3 테스트 재실행
5. **품질 평가**: 7가지 기준 평가
6. **개선 반복**: A+ 미달 시 개선 후 재평가

---

**작성자:** FxStore 개발팀  
**검토 필요:** ✅ 테스트 시나리오 완성

