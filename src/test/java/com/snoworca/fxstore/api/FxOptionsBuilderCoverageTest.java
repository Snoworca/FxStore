package com.snoworca.fxstore.api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * FxOptions.Builder 미커버 브랜치 테스트
 *
 * <p>커버리지 개선 대상:</p>
 * <ul>
 *   <li>Builder null 체크 브랜치</li>
 *   <li>Builder 유효성 검증 브랜치</li>
 *   <li>withXxx() 메서드 체이닝</li>
 * </ul>
 */
public class FxOptionsBuilderCoverageTest {

    // ==================== null 체크 테스트 ====================

    @Test(expected = FxException.class)
    public void commitMode_null_shouldThrow() {
        FxOptions.defaults().withCommitMode(null);
    }

    @Test(expected = FxException.class)
    public void durability_null_shouldThrow() {
        FxOptions.defaults().withDurability(null);
    }

    @Test(expected = FxException.class)
    public void onClosePolicy_null_shouldThrow() {
        FxOptions.defaults().withOnClosePolicy(null);
    }

    @Test(expected = FxException.class)
    public void fileLock_null_shouldThrow() {
        FxOptions.defaults().withFileLock(null);
    }

    @Test(expected = FxException.class)
    public void pageSize_null_shouldThrow() {
        FxOptions.defaults().withPageSize(null);
    }

    @Test(expected = FxException.class)
    public void numberMode_null_shouldThrow() {
        FxOptions.defaults().withNumberMode(null);
    }

    // ==================== 유효성 검증 테스트 ====================

    @Test(expected = FxException.class)
    public void cacheBytes_zero_shouldThrow() {
        FxOptions.defaults().withCacheBytes(0);
    }

    @Test(expected = FxException.class)
    public void cacheBytes_negative_shouldThrow() {
        FxOptions.defaults().withCacheBytes(-1);
    }

    @Test(expected = FxException.class)
    public void memoryLimitBytes_negative_shouldThrow() {
        FxOptions.defaults().withMemoryLimitBytes(-1);
    }

    @Test(expected = FxException.class)
    public void numberMode_strict_shouldThrow() {
        FxOptions.defaults().withNumberMode(NumberMode.STRICT);
    }

    @Test(expected = FxException.class)
    public void codecUpgradeHook_withoutAllowUpgrade_shouldThrow() {
        FxCodecUpgradeHook hook = (codecId, fromVersion, toVersion, oldBytes) -> oldBytes;
        FxOptions.defaults()
            .withCodecUpgradeHook(hook)
            .build();
    }

    // ==================== 정상 빌드 테스트 ====================

    @Test
    public void defaults_shouldReturnValidOptions() {
        FxOptions opts = FxOptions.defaults();

        assertEquals(CommitMode.AUTO, opts.commitMode());
        assertEquals(Durability.ASYNC, opts.durability());
        assertEquals(OnClosePolicy.ERROR, opts.onClosePolicy());
        assertEquals(FileLockMode.PROCESS, opts.fileLock());
        assertEquals(PageSize.PAGE_4K, opts.pageSize());
        assertEquals(64 * 1024 * 1024, opts.cacheBytes());
        assertEquals(NumberMode.CANONICAL, opts.numberMode());
        assertEquals(Long.MAX_VALUE, opts.memoryLimitBytes());
        assertFalse(opts.allowCodecUpgrade());
        assertNull(opts.codecUpgradeHook());
        assertFalse(opts.autoMigrateDeque());
    }

    @Test
    public void withCommitMode_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH)
            .build();

        assertEquals(CommitMode.BATCH, opts.commitMode());
    }

    @Test
    public void withDurability_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withDurability(Durability.SYNC)
            .build();

        assertEquals(Durability.SYNC, opts.durability());
    }

    @Test
    public void withOnClosePolicy_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withOnClosePolicy(OnClosePolicy.COMMIT)
            .build();

        assertEquals(OnClosePolicy.COMMIT, opts.onClosePolicy());
    }

    @Test
    public void withFileLock_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withFileLock(FileLockMode.NONE)
            .build();

        assertEquals(FileLockMode.NONE, opts.fileLock());
    }

    @Test
    public void withPageSize_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withPageSize(PageSize.PAGE_8K)
            .build();

        assertEquals(PageSize.PAGE_8K, opts.pageSize());
    }

    @Test
    public void withCacheBytes_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withCacheBytes(128 * 1024 * 1024)
            .build();

        assertEquals(128 * 1024 * 1024, opts.cacheBytes());
    }

    @Test
    public void withNumberMode_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withNumberMode(NumberMode.CANONICAL)
            .build();

        assertEquals(NumberMode.CANONICAL, opts.numberMode());
    }

    @Test
    public void withMemoryLimitBytes_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withMemoryLimitBytes(1024 * 1024)
            .build();

        assertEquals(1024 * 1024, opts.memoryLimitBytes());
    }

    @Test
    public void withMemoryLimitBytes_zero_shouldSucceed() {
        FxOptions opts = FxOptions.defaults()
            .withMemoryLimitBytes(0)
            .build();

        assertEquals(0, opts.memoryLimitBytes());
    }

    @Test
    public void withAllowCodecUpgrade_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withAllowCodecUpgrade(true)
            .build();

        assertTrue(opts.allowCodecUpgrade());
    }

    @Test
    public void withCodecUpgradeHook_withAllowUpgrade_shouldSucceed() {
        FxCodecUpgradeHook hook = (codecId, fromVersion, toVersion, oldBytes) -> oldBytes;
        FxOptions opts = FxOptions.defaults()
            .withAllowCodecUpgrade(true)
            .codecUpgradeHook(hook)
            .build();

        assertNotNull(opts.codecUpgradeHook());
    }

    @Test
    public void withAutoMigrateDeque_shouldSetCorrectly() {
        FxOptions opts = FxOptions.defaults()
            .withAutoMigrateDeque(true)
            .build();

        assertTrue(opts.autoMigrateDeque());
    }

    // ==================== 체이닝 테스트 ====================

    @Test
    public void chainedBuilding_shouldWorkCorrectly() {
        // FxOptions.withXxx returns Builder, then use Builder methods (without 'with' prefix)
        FxOptions opts = FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH)
            .durability(Durability.SYNC)
            .onClosePolicy(OnClosePolicy.COMMIT)
            .fileLock(FileLockMode.NONE)
            .pageSize(PageSize.PAGE_16K)
            .cacheBytes(256 * 1024 * 1024)
            .memoryLimitBytes(512 * 1024 * 1024)
            .allowCodecUpgrade(true)
            .autoMigrateDeque(true)
            .build();

        assertEquals(CommitMode.BATCH, opts.commitMode());
        assertEquals(Durability.SYNC, opts.durability());
        assertEquals(OnClosePolicy.COMMIT, opts.onClosePolicy());
        assertEquals(FileLockMode.NONE, opts.fileLock());
        assertEquals(PageSize.PAGE_16K, opts.pageSize());
        assertEquals(256 * 1024 * 1024, opts.cacheBytes());
        assertEquals(512 * 1024 * 1024, opts.memoryLimitBytes());
        assertTrue(opts.allowCodecUpgrade());
        assertTrue(opts.autoMigrateDeque());
    }

    // ==================== 기존 옵션에서 빌더 생성 테스트 ====================

    @Test
    public void toBuilder_shouldPreserveSettings() {
        FxOptions original = FxOptions.defaults()
            .withCommitMode(CommitMode.BATCH)
            .durability(Durability.SYNC)
            .build();

        // 기존 옵션에서 새 빌더 생성하여 일부 수정
        FxOptions modified = original
            .withCacheBytes(128 * 1024 * 1024)
            .build();

        // 수정된 값 확인
        assertEquals(128 * 1024 * 1024, modified.cacheBytes());
        // 기존 값 유지 확인
        assertEquals(CommitMode.BATCH, modified.commitMode());
        assertEquals(Durability.SYNC, modified.durability());
    }

    // ==================== OnClosePolicy 테스트 ====================

    @Test
    public void onClosePolicy_rollback_shouldWork() {
        FxOptions opts = FxOptions.defaults()
            .withOnClosePolicy(OnClosePolicy.ROLLBACK)
            .build();

        assertEquals(OnClosePolicy.ROLLBACK, opts.onClosePolicy());
    }

    // ==================== PageSize 테스트 ====================

    @Test
    public void pageSize_allSizes_shouldWork() {
        assertEquals(4096, PageSize.PAGE_4K.bytes());
        assertEquals(8192, PageSize.PAGE_8K.bytes());
        assertEquals(16384, PageSize.PAGE_16K.bytes());
    }
}
