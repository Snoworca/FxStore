# Phase 7 테스트 시나리오 - 운영 기능 및 안정화

> **Phase:** 7
> **기간:** 2주
> **목표:** 통계, 검증, 컴팩션, 크래시 복구, 최종 안정화

[← 목차로 돌아가기](00.index.md)

---

## 시나리오 개요

Phase 7에서는 FxStore의 운영 기능(stats, verify, compactTo)을 완성하고, 크래시 복구 메커니즘, 정책 기반 동작(OnClosePolicy, FileLockMode), 스레드 안전성을 검증합니다. 또한 Fuzz 테스트와 성능 벤치마크를 통해 최종 안정성을 확보합니다.

---

## Week 1: 운영 기능

### 시나리오 1: Stats 통계 검증

**목표:** stats() 메서드의 FAST/DEEP 모드 정확성 검증

#### 테스트 케이스 1.1: Stats FAST 모드 기본 동작
```
Given: Map에 100개 엔트리 저장된 Store
When: stats(StatsMode.FAST) 호출
Then:
  - fileBytes > 0
  - collectionCount == 1
  - liveBytes > 0
  - deadBytes >= 0
  - deadRatio >= 0.0 && deadRatio <= 1.0
```

#### 테스트 케이스 1.2: Stats DEEP 모드 정확성
```
Given: Map에 50개 엔트리 저장 후 25개 삭제
When: stats(StatsMode.DEEP) 호출
Then:
  - liveBytes < allocatedBytes (삭제된 공간 반영)
  - deadBytes > 0 (삭제된 공간 존재)
  - deadRatio > 0.0 (dead space 비율 반영)
```

#### 테스트 케이스 1.3: Stats FAST vs DEEP 비교
```
Given: 빈 Store (신규 생성)
When:
  - fastStats = stats(StatsMode.FAST)
  - deepStats = stats(StatsMode.DEEP)
Then:
  - fastStats.fileBytes() == deepStats.fileBytes()
  - fastStats.collectionCount() == deepStats.collectionCount()
  - FAST와 DEEP 결과가 일관성 유지
```

#### 테스트 케이스 1.4: 다중 컬렉션 통계
```
Given: Store with Map "users" + Set "tags" + List "logs" + Deque "events"
When: stats(StatsMode.DEEP) 호출
Then: collectionCount == 4
```

### 시나리오 2: Verify 무결성 검증

**목표:** verify() 메서드의 파일 무결성 검증 정확성

#### 테스트 케이스 2.1: 정상 파일 검증
```
Given: 정상적으로 생성된 Store 파일
When: verify() 호출
Then:
  - result.ok() == true
  - result.errors().isEmpty() == true
```

#### 테스트 케이스 2.2: Superblock 검증
```
Given: 정상 Store
When: verify() 내부에서 Superblock 검사
Then:
  - Magic number == "FXST"
  - CRC32C 일치
  - Page size 일관성
```

#### 테스트 케이스 2.3: CommitHeader A/B 슬롯 검증
```
Given: 여러 번 commit된 Store
When: verify() 호출
Then:
  - A 슬롯 CRC 검증 통과 또는 오류 기록
  - B 슬롯 CRC 검증 통과 또는 오류 기록
  - 유효한 슬롯 최소 1개 존재
  - seqNo gap 경고 (INV-1 위반 시)
```

#### 테스트 케이스 2.4: AllocTail 범위 검증
```
Given: 정상 Store
When: verify() 호출
Then:
  - allocTail >= minOffset (Superblock + 2*CommitHeader)
  - allocTail <= fileSize
```

### 시나리오 3: CompactTo 컴팩션 검증

**목표:** compactTo() 메서드의 데이터 무손실 복사 검증

#### 테스트 케이스 3.1: Map 데이터 보존
```
Given: Store with Map "users" containing {1L:"Alice", 2L:"Bob", 3L:"Charlie"}
When: compactTo(newPath) 호출 후 newStore 열기
Then:
  - newStore.openMap("users") 가능
  - newMap.get(1L) == "Alice"
  - newMap.get(2L) == "Bob"
  - newMap.get(3L) == "Charlie"
  - newMap.size() == 3
```

#### 테스트 케이스 3.2: 다중 컬렉션 복사
```
Given: Store with:
  - Map "users": {1L:"A", 2L:"B"}
  - Set "tags": {"java", "kotlin"}
  - List "logs": ["log1", "log2", "log3"]
  - Deque "events": ["e1", "e2"]
When: compactTo(newPath) 호출
Then: 모든 컬렉션이 정확히 복사됨
```

#### 테스트 케이스 3.3: Dead space 제거
```
Given: Store with 1000개 삽입 후 900개 삭제 (high dead ratio)
When:
  - beforeStats = store.stats(DEEP)
  - store.compactTo(newPath)
  - afterStats = newStore.stats(DEEP)
Then:
  - afterStats.fileBytes() < beforeStats.fileBytes()
  - afterStats.deadRatio() < beforeStats.deadRatio()
```

#### 테스트 케이스 3.4: BATCH 모드 pending 변경 시 오류
```
Given: BATCH 모드 Store with pending changes (hasPendingChanges = true)
When: compactTo(path) 호출
Then: FxException(ILLEGAL_ARGUMENT) 발생
      메시지: "Cannot compact with pending changes"
```

---

## Week 2: 안정화 및 정책

### 시나리오 4: 크래시 복구 테스트

**목표:** CommitHeader A/B 슬롯 기반 크래시 복구 검증

#### 테스트 케이스 4.1: 정상 종료 후 재시작
```
Given: Store에 데이터 저장 후 정상 close()
When: 동일 파일로 재오픈
Then:
  - 모든 데이터 정상 접근
  - verify().ok() == true
```

#### 테스트 케이스 4.2: A/B 슬롯 seqNo 기반 선택
```
Given: A 슬롯 seqNo=5, B 슬롯 seqNo=6 (둘 다 유효)
When: Store 오픈
Then: B 슬롯 사용 (더 높은 seqNo)
```

#### 테스트 케이스 4.3: CRC 오류 슬롯 복구
```
Given: A 슬롯 CRC 오류, B 슬롯 정상
When: Store 오픈
Then: B 슬롯으로 복구하여 정상 동작
```

### 시나리오 5: OnClosePolicy 동작 검증

**목표:** BATCH 모드 close() 시 정책별 동작 확인

#### 테스트 케이스 5.1: OnClosePolicy.ERROR
```
Given: BATCH 모드 Store with OnClosePolicy.ERROR
       pending changes 존재 (put 후 commit 안함)
When: close() 호출
Then: FxException(ILLEGAL_ARGUMENT) 발생
      메시지: "Pending changes exist on close"
```

#### 테스트 케이스 5.2: OnClosePolicy.COMMIT
```
Given: BATCH 모드 Store with OnClosePolicy.COMMIT
       map.put(1L, "test") 호출 (pending)
When: close() 호출 후 재오픈
Then:
  - 예외 없이 close 완료
  - 재오픈 후 map.get(1L) == "test"
```

#### 테스트 케이스 5.3: OnClosePolicy.ROLLBACK
```
Given: BATCH 모드 Store with OnClosePolicy.ROLLBACK
       map.put(1L, "test") 호출 (pending)
When: close() 호출 후 재오픈
Then:
  - 예외 없이 close 완료
  - 재오픈 후 map.get(1L) == null (롤백됨)
```

#### 테스트 케이스 5.4: AUTO 모드에서는 무시
```
Given: AUTO 모드 Store (OnClosePolicy 무관)
When: close() 호출
Then: 항상 정상 종료 (pending changes 없음)
```

### 시나리오 6: FileLockMode 동작 검증

**목표:** 파일 잠금 모드별 동작 확인

#### 테스트 케이스 6.1: FileLockMode.PROCESS 배타적 잠금
```
Given: FileLockMode.PROCESS로 Store1 오픈
When: 동일 파일로 Store2 오픈 시도
Then: FxException(LOCK_FAILED) 발생
      메시지: "File is locked by another process"
```

#### 테스트 케이스 6.2: FileLockMode.NONE 다중 접근
```
Given: FileLockMode.NONE으로 Store1 오픈
When: 동일 파일로 FileLockMode.NONE으로 Store2 오픈
Then: 오류 없이 두 Store 모두 오픈 (주의: 동시 쓰기 시 손상 가능)
```

#### 테스트 케이스 6.3: 기본값 PROCESS 확인
```
Given: FxOptions.defaults() 사용
When: fileLock() 확인
Then: FileLockMode.PROCESS (기본값)
```

#### 테스트 케이스 6.4: 잠금 해제 후 재오픈
```
Given: FileLockMode.PROCESS로 Store 오픈
When:
  - store1.close()
  - store2 = FxStore.open(samePath)
Then: store2 정상 오픈
```

### 시나리오 7: Fuzz 테스트

**목표:** 랜덤 연산 시퀀스에서 크래시 없이 정상 동작 확인

#### 테스트 케이스 7.1: Map Fuzz 테스트
```
Given: 빈 FxMap<Long, String>
When: 10,000회 랜덤 연산:
  - 50% put(randomKey, randomValue)
  - 30% get(randomKey)
  - 15% remove(randomKey)
  - 5% clear()
Then:
  - 모든 연산 예외 없이 완료
  - 참조 구현(TreeMap)과 결과 비교 일치
  - verify().ok() == true
```

#### 테스트 케이스 7.2: List Fuzz 테스트
```
Given: 빈 FxList<String>
When: 10,000회 랜덤 연산:
  - 40% add(randomIndex, randomValue)
  - 30% get(randomIndex)
  - 20% remove(randomIndex)
  - 10% set(randomIndex, newValue)
Then:
  - 모든 연산 예외 없이 완료
  - 참조 구현(ArrayList)과 결과 비교 일치
```

#### 테스트 케이스 7.3: 혼합 컬렉션 Fuzz
```
Given: Store with Map, Set, List, Deque
When: 10,000회 랜덤 연산 (모든 컬렉션 대상)
Then:
  - 크래시 없이 완료
  - 모든 컬렉션 데이터 일관성 유지
```

### 시나리오 8: 성능 벤치마크

**목표:** 성능 목표 달성 확인

#### 테스트 케이스 8.1: Map.put 성능
```
Given: 빈 Map<Long, String>
When: 100,000회 순차 put 실행
Then: >= 50,000 ops/sec
```

#### 테스트 케이스 8.2: Map.get 성능
```
Given: 100,000 엔트리가 있는 Map
When: 100,000회 랜덤 get 실행
Then: >= 100,000 ops/sec
```

#### 테스트 케이스 8.3: List.get(i) 성능
```
Given: 10,000 요소가 있는 List
When: 10,000회 랜덤 인덱스 get 실행
Then: >= 20,000 ops/sec (O(log n) 보장)
```

#### 테스트 케이스 8.4: 메모리 사용량
```
Given: 100,000 엔트리 Map
When: 힙 메모리 측정
Then: <= 100MB
```

#### 테스트 케이스 8.5: Dead ratio
```
Given: 100,000 엔트리 삽입 후 50,000 삭제
When: stats(DEEP).deadRatio() 측정
Then: < 0.50 (50% 미만)
```

### 시나리오 9: 불변식 검증 (INV-1 ~ INV-9)

**목표:** Architecture 문서 불변식 준수 확인

#### 테스트 케이스 9.1: INV-1 CommitHeader seqNo 단조 증가
```
Given: 여러 번 commit 수행
When: 각 commit 후 seqNo 확인
Then: seqNo는 항상 이전 값보다 큼
```

#### 테스트 케이스 9.2: INV-6 B+Tree 키 정렬 순서
```
Given: Map에 랜덤 순서로 키 삽입
When: 전체 키 순회
Then: 키가 정렬된 순서로 반환
```

#### 테스트 케이스 9.3: INV-7 OST subtreeCount 정확성
```
Given: List에 N개 요소 삽입/삭제
When: 내부 OST 노드 검사
Then: 각 노드의 subtreeCount == 실제 하위 요소 수
```

#### 테스트 케이스 9.4: INV-8 Deque headSeq/tailSeq
```
Given: Deque에 addFirst/addLast 혼합 수행
When: 내부 상태 검사
Then: headSeq <= tailSeq + 1
```

---

## 통합 테스트

### 시나리오 10: 전체 시나리오 통합

#### 테스트 케이스 10.1: 전체 라이프사이클
```
Given: 새 Store 생성
When:
  1. Map/Set/List/Deque 생성
  2. 각 컬렉션에 데이터 저장
  3. stats() 확인
  4. verify() 확인
  5. compactTo() 실행
  6. 새 파일 verify() 확인
  7. close() 및 재오픈
Then: 모든 단계 성공, 데이터 무결성 유지
```

#### 테스트 케이스 10.2: 전체 회귀 테스트
```
Given: Phase 0~7 모든 테스트 케이스
When: 전체 테스트 스위트 실행
Then:
  - 모든 테스트 통과
  - 커버리지 >= 95%
```

---

## 테스트 우선순위

| 우선순위 | 시나리오 | 이유 |
|---------|---------|------|
| P0 | Stats, Verify, CompactTo 기본 | 핵심 운영 기능 |
| P0 | OnClosePolicy, FileLockMode | 데이터 안전성 |
| P1 | 크래시 복구 | 신뢰성 |
| P1 | Fuzz 테스트 | 안정성 |
| P2 | 성능 벤치마크 | 성능 목표 |
| P2 | 불변식 검증 | 정확성 보장 |

---

## 예상 테스트 파일

- `StatsTest.java` - 시나리오 1
- `VerifyTest.java` - 시나리오 2
- `CompactTest.java` - 시나리오 3
- `CrashRecoveryTest.java` - 시나리오 4
- `OnClosePolicyTest.java` - 시나리오 5
- `FileLockTest.java` - 시나리오 6
- `FuzzTest.java` - 시나리오 7
- `PerformanceBenchmarkTest.java` - 시나리오 8
- `InvariantsTest.java` - 시나리오 9

---

[← 목차로 돌아가기](00.index.md)
