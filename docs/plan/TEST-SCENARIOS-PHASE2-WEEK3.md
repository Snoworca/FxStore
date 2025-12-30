# Phase 2 Week 3 테스트 시나리오: Page 캐시

> **Phase:** Storage 및 Page 관리 - Week 3  
> **대상:** PageCache (LRU 기반 페이지 캐싱)  
> **작성일:** 2025-12-24

[← 계획 문서로 돌아가기](00.index.md)

---

## 개요

Phase 2 Week 3에서는 LRU 기반 페이지 캐시를 구현합니다. 다음 시나리오를 **모두 구현**해야 합니다.

---

## TS-2.3: PageCache 테스트 시나리오

### TS-2.3.1: 기본 캐시 생성

**Given:**
- cacheBytes = 4096 * 10 (10 페이지 크기)
- pageSize = 4096

**When:**
- PageCache 생성

**Then:**
- maxPages == 10
- size() == 0

---

### TS-2.3.2: 단일 페이지 저장 및 조회

**Given:**
- PageCache 생성됨 (maxPages = 10)
- pageId = 1
- pageData = 4096 바이트 (모두 0xAA)

**When:**
- cache.put(1, pageData)

**Then:**
- cache.get(1) != null
- cache.get(1) == pageData (내용 동일)
- cache.get(1) != pageData (참조 다름, 복사본)
- cache.size() == 1

---

### TS-2.3.3: 캐시 미스

**Given:**
- PageCache 생성됨
- pageId = 1을 저장하지 않음

**When:**
- cache.get(1) 호출

**Then:**
- null 반환
- size() == 0

---

### TS-2.3.4: 다중 페이지 저장

**Given:**
- PageCache 생성됨 (maxPages = 10)
- pageId [1, 2, 3, 4, 5]를 순차적으로 저장

**When:**
- cache.put(1, data1)
- cache.put(2, data2)
- cache.put(3, data3)
- cache.put(4, data4)
- cache.put(5, data5)

**Then:**
- cache.size() == 5
- cache.get(1) == data1
- cache.get(5) == data5

---

### TS-2.3.5: LRU Eviction (최대 용량 초과)

**Given:**
- PageCache 생성됨 (maxPages = 3)
- pageId [1, 2, 3]을 순차적으로 저장 (캐시 full)

**When:**
- cache.put(4, data4) 호출 (용량 초과)

**Then:**
- cache.size() == 3
- cache.get(1) == null (가장 오래된 페이지 evict됨)
- cache.get(2) != null
- cache.get(3) != null
- cache.get(4) != null

---

### TS-2.3.6: LRU 접근 순서 (Access Order)

**Given:**
- PageCache 생성됨 (maxPages = 3)
- pageId [1, 2, 3]을 순차적으로 저장

**When:**
- cache.get(1) 호출 (페이지 1 접근 → 최근 사용됨)
- cache.put(4, data4) 호출 (용량 초과)

**Then:**
- cache.get(1) != null (최근 접근되어 유지됨)
- cache.get(2) == null (가장 오래된 페이지 evict됨)
- cache.get(3) != null
- cache.get(4) != null

---

### TS-2.3.7: 페이지 무효화 (Invalidate)

**Given:**
- PageCache 생성됨
- pageId = 1 저장됨

**When:**
- cache.invalidate(1)

**Then:**
- cache.get(1) == null
- cache.size() == 0

---

### TS-2.3.8: 존재하지 않는 페이지 무효화

**Given:**
- PageCache 생성됨
- pageId = 1 저장되지 않음

**When:**
- cache.invalidate(1)

**Then:**
- 예외 발생 안 함
- cache.size() == 0

---

### TS-2.3.9: Clear (모든 캐시 제거)

**Given:**
- PageCache 생성됨
- pageId [1, 2, 3, 4, 5] 저장됨
- cache.size() == 5

**When:**
- cache.clear()

**Then:**
- cache.size() == 0
- cache.get(1) == null
- cache.get(5) == null

---

### TS-2.3.10: 캐시 크기 0 (캐시 비활성화)

**Given:**
- cacheBytes = 0
- pageSize = 4096

**When:**
- PageCache 생성

**Then:**
- maxPages == 0
- cache.put(1, data) 호출 → 아무 일도 일어나지 않음
- cache.get(1) → null 반환
- cache.size() == 0

---

### TS-2.3.11: null 페이지 데이터 저장 시도

**Given:**
- PageCache 생성됨

**When:**
- cache.put(1, null)

**Then:**
- NullPointerException 발생

---

### TS-2.3.12: 잘못된 페이지 크기 저장 시도

**Given:**
- PageCache 생성됨 (pageSize = 4096)
- pageData = 2048 바이트 (잘못된 크기)

**When:**
- cache.put(1, pageData)

**Then:**
- IllegalArgumentException 발생
- 메시지: "pageData length (2048) must equal pageSize (4096)"

---

### TS-2.3.13: 불변성 보장 (데이터 복사)

**Given:**
- PageCache 생성됨
- pageData = 4096 바이트 (모두 0xAA)

**When:**
- cache.put(1, pageData)
- pageData[0] = 0xBB로 수정 (외부 변조)

**Then:**
- cache.get(1)[0] == 0xAA (캐시된 데이터는 변경 안 됨)

---

### TS-2.3.14: 조회 시 복사본 반환

**Given:**
- PageCache 생성됨
- pageId = 1 저장됨

**When:**
- byte[] retrieved = cache.get(1)
- retrieved[0] = 0xBB로 수정

**Then:**
- cache.get(1)[0] != 0xBB (캐시 내부 데이터는 보호됨)
- **참고:** 현재 구현은 복사본을 반환하지 않으므로 이는 향후 개선 항목

---

### TS-2.3.15: LRU 순서 정확성 (복잡한 접근 패턴)

**Given:**
- PageCache 생성됨 (maxPages = 4)
- pageId [1, 2, 3, 4] 저장됨

**When:**
1. cache.get(1)
2. cache.get(2)
3. cache.get(3)
4. cache.put(5, data5) 호출

**Then:**
- cache.get(4) == null (가장 오래 전에 접근됨, evict됨)
- cache.get(1) != null
- cache.get(2) != null
- cache.get(3) != null
- cache.get(5) != null

---

### TS-2.3.16: 동일 페이지 재저장 (덮어쓰기)

**Given:**
- PageCache 생성됨
- pageId = 1, data = [0xAA, 0xAA, ...] 저장됨

**When:**
- pageId = 1, newData = [0xBB, 0xBB, ...] 재저장

**Then:**
- cache.get(1)[0] == 0xBB (새 데이터로 덮어쓰여짐)
- cache.size() == 1 (크기는 증가하지 않음)

---

### TS-2.3.17: 음수 cacheBytes

**Given:**
- cacheBytes = -100

**When:**
- PageCache 생성 시도

**Then:**
- IllegalArgumentException 발생
- 메시지: "cacheBytes must be >= 0"

---

### TS-2.3.18: 음수 또는 0 pageSize

**Given:**
- cacheBytes = 4096
- pageSize = 0 또는 -100

**When:**
- PageCache 생성 시도

**Then:**
- IllegalArgumentException 발생
- 메시지: "pageSize must be > 0"

---

### TS-2.3.19: 매우 큰 캐시 크기

**Given:**
- cacheBytes = Long.MAX_VALUE
- pageSize = 4096

**When:**
- PageCache 생성

**Then:**
- maxPages 계산 시 오버플로우 없이 처리됨
- maxPages == (int)(Long.MAX_VALUE / 4096)

---

### TS-2.3.20: 페이지 ID 경계값 (Long.MIN_VALUE, Long.MAX_VALUE)

**Given:**
- PageCache 생성됨

**When:**
- cache.put(Long.MIN_VALUE, data)
- cache.put(Long.MAX_VALUE, data)

**Then:**
- cache.get(Long.MIN_VALUE) != null
- cache.get(Long.MAX_VALUE) != null
- 예외 발생 안 함

---

## 통합 테스트 시나리오

### TS-2.3.INT-1: 캐시 히트/미스 비율 측정

**Given:**
- PageCache 생성됨 (maxPages = 100)
- 1000개의 pageId에 대한 랜덤 접근 패턴

**When:**
- 각 pageId를 2회씩 접근 (첫 번째: 미스, 두 번째: 히트)

**Then:**
- 히트율 측정 (통계 수집)
- 히트율 >= 50% (두 번째 접근은 히트)

**참고:** 현재 PageCache에는 통계 기능이 없으므로 향후 Phase에서 추가 가능

---

### TS-2.3.INT-2: 다중 스레드 동시 접근 (향후)

**Given:**
- PageCache 생성됨
- 10개의 스레드가 동시에 읽기/쓰기

**When:**
- 각 스레드가 100회 put/get 수행

**Then:**
- Race condition 없음
- 모든 데이터 정확성 유지

**참고:** Phase 2에서는 스레드 안전성 미구현, Phase 7에서 추가

---

## 테스트 코드 요구사항

### 필수 구현 사항

1. **모든 시나리오는 별도의 @Test 메서드로 구현**
2. **Given-When-Then 패턴 사용**
3. **명확한 Assertion 메시지**
4. **예외 테스트는 assertThrows 사용 (JUnit 5)**
5. **바이트 배열 비교 시 Arrays.equals 사용**

### 테스트 클래스 구조

```java
package com.fxstore.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;

public class PageCacheTest {
    
    @Test
    void testCreateCache_WithValidParameters_Success() {
        // TS-2.3.1
    }
    
    @Test
    void testPutAndGet_SinglePage_ReturnsCorrectData() {
        // TS-2.3.2
    }
    
    // ... 모든 시나리오 구현
}
```

---

## 완료 기준

- ✅ 총 20개 시나리오 모두 테스트 코드로 구현
- ✅ 모든 테스트 통과 (0 failures, 0 errors)
- ✅ 7가지 품질 기준 모두 A+ 달성

---

**다음 단계:** [품질 평가](EVALUATION-PHASE2-WEEK3-QUALITY.md)
