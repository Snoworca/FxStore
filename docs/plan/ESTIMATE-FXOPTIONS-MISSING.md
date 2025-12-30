# FxOptions 미구현 옵션 구현 예상 보고서

**작성일**: 2025-12-26
**분석자**: Claude Code
**대상**: memoryLimitBytes, allowCodecUpgrade, codecUpgradeHook

---

## 1. 분석 요약

| 옵션 | 난이도 | 예상 코드량 | 테스트 포함 예상 시간 |
|------|--------|-------------|---------------------|
| memoryLimitBytes | ⭐ 쉬움 | ~20줄 | 15분 |
| allowCodecUpgrade | ⭐⭐ 보통 | ~15줄 | 10분 |
| codecUpgradeHook | ⭐⭐⭐ 보통-어려움 | ~50줄 | 30분 |

**전체 예상 시간**: 약 55분 (테스트 포함 1시간)

---

## 2. 상세 분석

### 2.1 memoryLimitBytes ⭐ 쉬움

**현재 상황**:
- `MemoryStorage.java`가 이미 `limitBytes` 파라미터를 완벽히 지원
- `FxStoreImpl.java:72`에서 하드코딩됨: `new MemoryStorage(100 * 1024 * 1024)`

**필요한 변경**:

1. **FxOptions.java** (~10줄)
   ```java
   // 필드 추가
   private final long memoryLimitBytes;

   // 생성자에 추가
   this.memoryLimitBytes = builder.memoryLimitBytes;

   // getter 추가
   public long memoryLimitBytes() { return memoryLimitBytes; }

   // Builder에 추가
   private long memoryLimitBytes = Long.MAX_VALUE;

   public Builder memoryLimitBytes(long bytes) {
       if (bytes < 0) throw FxException.illegalArgument("...");
       this.memoryLimitBytes = bytes;
       return this;
   }
   ```

2. **FxStoreImpl.java** (~2줄)
   ```java
   // 변경 전:
   this.storage = new MemoryStorage(100 * 1024 * 1024);

   // 변경 후:
   this.storage = new MemoryStorage(options.memoryLimitBytes());
   ```

**위험도**: 매우 낮음 - 기존 MemoryStorage 로직 변경 없음

**예상 시간**: 15분 (테스트 포함)

---

### 2.2 allowCodecUpgrade ⭐⭐ 보통

**현재 상황**:
- `FxStoreImpl.java:1314-1324`의 `validateCodec` 메서드에서 버전 불일치 시 예외 발생

**필요한 변경**:

1. **FxOptions.java** (~8줄)
   ```java
   // 필드 추가
   private final boolean allowCodecUpgrade;

   // getter
   public boolean allowCodecUpgrade() { return allowCodecUpgrade; }

   // Builder
   private boolean allowCodecUpgrade = false;

   public Builder allowCodecUpgrade(boolean allow) {
       this.allowCodecUpgrade = allow;
       return this;
   }
   ```

2. **FxStoreImpl.java** (~6줄 수정)
   ```java
   private <T> void validateCodec(CodecRef expected, FxCodec<T> actual) {
       // ID 체크 (변경 없음)
       if (!expected.getCodecId().equals(actual.id())) {
           throw FxException.typeMismatch(...);
       }

       // 버전 체크 (수정)
       if (expected.getCodecVersion() != actual.version()) {
           if (!options.allowCodecUpgrade()) {
               throw FxException.versionMismatch(...);
           }
           // allowCodecUpgrade=true면 통과 (codecUpgradeHook과 연계)
       }
   }
   ```

**위험도**: 낮음 - 기존 동작(false)이 기본값

**예상 시간**: 10분

---

### 2.3 codecUpgradeHook ⭐⭐⭐ 보통-어려움

**현재 상황**:
- 버전 불일치 시 업그레이드 메커니즘 없음
- 명세: `allowCodecUpgrade=true` 시 사용

**필요한 변경**:

1. **FxCodecUpgradeHook.java** (새 파일, ~15줄)
   ```java
   package com.fxstore.api;

   /**
    * 코덱 버전 업그레이드 훅
    *
    * @param <T> 데이터 타입
    */
   @FunctionalInterface
   public interface FxCodecUpgradeHook<T> {
       /**
        * 이전 버전 데이터를 새 버전으로 변환
        *
        * @param oldVersion 이전 버전 번호
        * @param newVersion 새 버전 번호
        * @param oldData 이전 버전으로 인코딩된 데이터
        * @return 새 버전으로 변환된 데이터
        */
       byte[] upgrade(int oldVersion, int newVersion, byte[] oldData);
   }
   ```

2. **FxOptions.java** (~12줄)
   ```java
   // 필드
   private final FxCodecUpgradeHook<?> codecUpgradeHook;

   // getter
   public FxCodecUpgradeHook<?> codecUpgradeHook() { return codecUpgradeHook; }

   // Builder
   private FxCodecUpgradeHook<?> codecUpgradeHook = null;

   public <T> Builder codecUpgradeHook(FxCodecUpgradeHook<T> hook) {
       this.codecUpgradeHook = hook;
       return this;
   }
   ```

3. **FxStoreImpl.java** (validateCodec 수정, ~15줄)
   ```java
   private <T> void validateCodec(CodecRef expected, FxCodec<T> actual) {
       if (!expected.getCodecId().equals(actual.id())) {
           throw FxException.typeMismatch(...);
       }

       if (expected.getCodecVersion() != actual.version()) {
           if (!options.allowCodecUpgrade()) {
               throw FxException.versionMismatch(...);
           }

           FxCodecUpgradeHook<?> hook = options.codecUpgradeHook();
           if (hook == null) {
               // allowCodecUpgrade=true but no hook: 경고만 하고 진행
               // 또는 예외? (명세 확인 필요)
           }
           // 실제 업그레이드는 데이터 읽기 시점에 적용
       }
   }
   ```

**설계 고려사항**:
- 업그레이드 훅이 실제로 적용되는 시점: 데이터 읽기 시
- 업그레이드된 데이터를 저장할지 여부
- 다중 버전 점프 지원 여부 (v1 → v3)

**위험도**: 중간 - 데이터 마이그레이션 로직 필요

**예상 시간**: 30분 (기본 구현), 추가 45분 (완전한 데이터 마이그레이션)

---

## 3. 구현 우선순위 권장

| 순위 | 옵션 | 이유 |
|------|------|------|
| 1 | memoryLimitBytes | 가장 쉬움, 즉시 활용 가능 |
| 2 | allowCodecUpgrade | codecUpgradeHook의 선행 조건 |
| 3 | codecUpgradeHook | 가장 복잡, 실제 사용 시나리오 검토 필요 |

---

## 4. 구현 영향도 분석

### 4.1 파일별 변경 범위

| 파일 | 변경 유형 | 변경량 |
|------|----------|--------|
| FxOptions.java | 수정 | +35줄 |
| FxStoreImpl.java | 수정 | +10줄 |
| FxCodecUpgradeHook.java | 신규 | ~15줄 |
| FxOptionsTest.java | 수정 | +20줄 |
| FxStoreImplTest.java | 수정 | +15줄 |

### 4.2 호환성

- **하위 호환성**: ✅ 유지 (모든 옵션에 기본값 있음)
- **API 변경**: ✅ 추가만 (기존 API 변경 없음)
- **파일 포맷**: ✅ 변경 없음

---

## 5. 결론

### 5.1 총 예상 시간

| 구현 범위 | 예상 시간 |
|----------|----------|
| 기본 구현 (3개 옵션) | 55분 |
| 테스트 추가 | 20분 |
| 문서 업데이트 | 10분 |
| **합계** | **~1시간 30분** |

### 5.2 권장사항

1. **memoryLimitBytes**: 즉시 구현 권장 (단순함)
2. **allowCodecUpgrade + codecUpgradeHook**:
   - 실제 사용 시나리오 정의 후 구현
   - v0.3 미지원 기능으로 명세에 이미 기재됨
   - v0.4 목표로 설정 권장

### 5.3 구현 순서

```
Phase A (15분): memoryLimitBytes
Phase B (40분): allowCodecUpgrade + codecUpgradeHook (기본)
Phase C (30분): codecUpgradeHook 실제 적용 로직
```

---

**보고서 완료일**: 2025-12-26
**분석자 서명**: Claude Code
