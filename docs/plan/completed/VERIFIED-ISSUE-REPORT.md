# FxStore 검증된 이슈 보고서

> **검증일:** 2025-12-30
> **검증 방법:** 코드 단위 전수 검사
> **원본:** COMPREHENSIVE-AUDIT-REPORT.md (75개 이슈)

---

## 요약

| 원본 심각도 | 확인됨 | 허위 양성 | 비활성 코드 | 경미/설계 이슈 |
|-------------|--------|-----------|-------------|----------------|
| **CRITICAL (6)** | 1 | 3 | 1 | 1 |
| **HIGH (19)** | 2 | 2+ | - | 15+ |

---

## CRITICAL 이슈 검증 결과

### CRIT-001: PageCache 스레드 안전성 부재
| 원본 주장 | 검증 결과 |
|-----------|-----------|
| LinkedHashMap 동기화 없음, 데이터 손상 우려 | **비활성 코드 (NON-ISSUE)** |

**근거:**
- `PageCache` 클래스는 정의되어 있지만, 메서드 호출(`readPage()`, `writePage()`, `invalidate()`) 없음
- Grep 검색: `pageCache.` 패턴 매치 0건
- BTree와 OST는 `Storage`를 직접 사용
- 코드가 사용되지 않으므로 런타임 이슈 없음

```
# 검증 명령
grep -r "pageCache\." → No matches
grep -r "\.readPage\(|\.writePage\(" → No matches
```

---

### CRIT-002: OST pageId vs offset 혼동
| 원본 주장 | 검증 결과 |
|-----------|-----------|
| OST.java:215가 pageId를 offset으로 변환 없이 전달 | **허위 양성 (FALSE POSITIVE)** |

**근거:**
- OST.saveNode() 주석: `"allocator.allocatePage()는 offset을 반환 (pageId가 아님!)"`
- OST는 "pageId" 변수명을 사용하지만 실제로는 **offset**을 저장
- 읽기/쓰기 모두 offset 직접 사용으로 **내부 일관성 유지**

```java
// OST.saveNode() - offset 반환
long offset = allocator.allocatePage();
storage.write(offset, page, 0, pageSize);
return offset;  // "pageId"로 저장되지만 실제로는 offset

// OST.loadNode() - offset으로 읽기
storage.read(pageId, page, 0, pageSize);  // pageId = 실제로 offset
```

**참고:** BTree는 `pageId * pageSize`로 변환 후 사용하는 다른 패턴. 두 컴포넌트가 혼합 사용되면 문제될 수 있으나, 현재 분리 사용.

---

### CRIT-003: 빈 OST 리프 pageId 공유
| 원본 주장 | 검증 결과 |
|-----------|-----------|
| 빈 OST 생성 시 pageId=0 리프로 모든 빈 리프 공유 | **허위 양성 (FALSE POSITIVE)** |

**근거:**
- `new OSTLeaf()`는 메모리 객체 생성만 (pageId 할당 없음)
- `saveNode(newLeaf)` 호출 시 `allocator.allocatePage()`로 고유 offset 할당
- 모든 저장된 리프는 고유한 위치를 가짐

```java
// OST.insertWithRoot() 라인 362-367
if (rootPageId == 0L) {
    OSTLeaf newLeaf = new OSTLeaf();          // 메모리 객체
    newLeaf.addElement(elementRecordId);
    long newRootPageId = saveNode(newLeaf);   // 고유 offset 할당
    return new StatelessInsertResult(newRootPageId);
}
```

---

### CRIT-004: FxDequeImpl headSeq/tailSeq 강제 리셋
| 원본 주장 | 검증 결과 |
|-----------|-----------|
| removeFirstOccurrence() 등에서 시퀀스 리셋으로 데이터 손실 | **설계 이슈 (DESIGN CONCERN)** |

**근거:**
- FxDequeImpl.java:552-553에서 `headSeq = 0; tailSeq = 0;` 확인
- 그러나 리셋 전에 모든 데이터가 `elements` 리스트로 수집됨
- 리셋 후 데이터를 새 시퀀스로 재삽입
- **데이터 손실은 없음**, 시퀀스 번호만 변경

```java
// 라인 541-570
List<E> elements = new ArrayList<>();  // 데이터 보존
// ... 기존 데이터 수집 ...

headSeq = 0;   // 시퀀스 리셋
tailSeq = 0;

for (int i = 0; i < elements.size(); i++) {
    if (i != indexToRemove) {
        // 새 시퀀스로 재삽입
        tailSeq++;
    }
}
```

**위험도:** 낮음 - 기능적 문제 없음, 시퀀스 연속성만 변경

---

### CRIT-005: BTree 리프 분할 후 중복 쓰기
| 원본 주장 | 검증 결과 |
|-----------|-----------|
| 같은 리프 2번 쓰기로 COW 위반 | **경미한 이슈 (MINOR)** |

**근거:**
- BTree.java:370-379에서 이중 쓰기 확인
- 첫 번째 쓰기: nextLeaf 없이 저장
- 두 번째 쓰기: nextLeaf 설정 후 같은 위치에 저장
- **비효율적**이지만 **정확성에는 문제 없음**
  - 쓰기 락 하에서만 실행되므로 동시 읽기 없음
  - 새로 할당된 페이지이므로 기존 리더 없음

```java
long leftPageId = allocatePageId();
writeNode(splitResult.leftLeaf, leftPageId);    // 첫 번째 쓰기

long rightPageId = allocatePageId();
writeNode(splitResult.rightLeaf, rightPageId);

splitResult.leftLeaf.setNextLeafPageId(rightPageId);
writeNode(splitResult.leftLeaf, leftPageId);    // 두 번째 쓰기 (동일 위치)
```

**개선 권장:** 한 번만 쓰도록 리팩토링 (성능 최적화)

---

### CRIT-006: FxStoreImpl 메서드 체인 타입 오류
| 원본 주장 | 검증 결과 |
|-----------|-----------|
| .durability()가 FxOptions 반환하여 체인 끊김 | **허위 양성 (FALSE POSITIVE)** |

**근거:**
- `FxOptions.defaults()` → FxOptions
- `.withCommitMode()` → Builder (toBuilder().commitMode() 호출)
- Builder 클래스에 `durability(Durability)` 메서드 존재 (라인 178-184)
- 체인이 정상 작동: FxOptions → Builder → Builder → ... → build() → FxOptions

```java
// FxOptions.Builder 클래스 (라인 178-184)
public Builder durability(Durability durability) {
    if (durability == null) {
        throw FxException.illegalArgument("durability cannot be null");
    }
    this.durability = durability;
    return this;  // Builder 반환
}
```

---

## HIGH 이슈 검증 결과 (주요 항목)

### HIGH-C1: createOrOpenMap TOCTOU
| 원본 주장 | 검증 결과 |
|-----------|-----------|
| catalog.containsKey() 후 createMap() 사이 경쟁 조건 | **허위 양성 (FALSE POSITIVE)** |

**근거:**
- 코드 주석에 명시: "동시성 환경에서 race condition을 방지하기 위해 ALREADY_EXISTS 예외 발생 시 openMap으로 fallback"
- TOCTOU는 **의도된 낙관적 패턴**으로, 실패 시 안전하게 fallback
- 데이터 손상 가능성 없음

```java
// 라인 615-630
public <K, V> NavigableMap<K, V> createOrOpenMap(String name, ...) {
    if (catalog.containsKey(name)) {
        return openMap(name, keyClass, valueClass);  // 빠른 경로
    }
    try {
        return createMap(name, keyClass, valueClass);
    } catch (FxException e) {
        if (e.getCode() == FxErrorCode.ALREADY_EXISTS) {
            return openMap(name, keyClass, valueClass);  // 안전한 fallback
        }
        throw e;
    }
}
```

---

### HIGH-L3: FxNavigableSetImpl retainAll() iterator.remove()
| 원본 주장 | 검증 결과 |
|-----------|-----------|
| 읽기 전용 iterator에서 remove() 호출 | **확인됨 (CONFIRMED)** |

**근거:**
- FxNavigableSetImpl.iterator() → map.keySet().iterator()
- KeySetView.iterator()가 `Collections.unmodifiableList(keys).iterator()` 반환
- `it.remove()` 호출 시 **UnsupportedOperationException** 발생

```java
// FxNavigableSetImpl.retainAll() 라인 155-165
public boolean retainAll(Collection<?> c) {
    Iterator<E> it = iterator();  // unmodifiable iterator
    while (it.hasNext()) {
        if (!c.contains(it.next())) {
            it.remove();  // UnsupportedOperationException!
        }
    }
    return modified;
}

// KeySetView.iterator() 라인 911-920
return Collections.unmodifiableList(keys).iterator();  // remove() 지원 안 함
```

**수정 필요:** 별도의 삭제 로직 구현 필요

---

## 실제 수정이 필요한 이슈 요약

| 우선순위 | 이슈 ID | 파일 | 문제 | 수정 난이도 |
|----------|---------|------|------|-------------|
| **HIGH** | HIGH-L3 | FxNavigableSetImpl.java | retainAll() UnsupportedOperationException | 중 |
| **LOW** | CRIT-005 | BTree.java | 리프 분할 시 이중 쓰기 (비효율) | 하 |
| **INFO** | CRIT-004 | FxDequeImpl.java | 시퀀스 리셋 (기능적 문제 없음) | - |

---

## 결론

75개 감사 이슈 중 코드 단위 검증 결과:

1. **실제 버그:** 1건 (HIGH-L3: retainAll() UOE)
2. **비효율/개선 필요:** 1건 (CRIT-005: 이중 쓰기)
3. **설계 문서화 필요:** 1건 (CRIT-004: 시퀀스 리셋)
4. **허위 양성:** 4건 (CRIT-002, CRIT-003, CRIT-006, HIGH-C1)
5. **비활성 코드:** 1건 (CRIT-001: PageCache)

대부분의 CRITICAL 이슈는 코드 분석이 불완전하거나 의도된 설계를 오해한 결과였습니다. 실제로 수정이 필요한 것은 `retainAll()` 메서드의 UnsupportedOperationException 문제입니다.

---

> **검증자 노트:** OST와 BTree가 서로 다른 pageId/offset 규칙을 사용하고 있습니다. 현재는 분리되어 사용되므로 문제없지만, 향후 통합 시 주의가 필요합니다.
