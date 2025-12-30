package com.snoworca.fxstore.api;

/**
 * Configuration options for FxStore.
 * 
 * <p>Uses immutable builder pattern for type-safe configuration.
 * 
 * <p>Example:
 * <pre>{@code
 * FxOptions opts = FxOptions.defaults()
 *     .withCommitMode(CommitMode.BATCH)
 *     .withDurability(Durability.SYNC)
 *     .withCacheBytes(128 * 1024 * 1024);
 * }</pre>
 */
public final class FxOptions {
    private final CommitMode commitMode;
    private final Durability durability;
    private final OnClosePolicy onClosePolicy;
    private final FileLockMode fileLock;
    private final PageSize pageSize;
    private final long cacheBytes;
    private final NumberMode numberMode;
    private final long memoryLimitBytes;
    private final boolean allowCodecUpgrade;
    private final FxCodecUpgradeHook codecUpgradeHook;
    private final boolean autoMigrateDeque;

    private FxOptions(Builder builder) {
        this.commitMode = builder.commitMode;
        this.durability = builder.durability;
        this.onClosePolicy = builder.onClosePolicy;
        this.fileLock = builder.fileLock;
        this.pageSize = builder.pageSize;
        this.cacheBytes = builder.cacheBytes;
        this.numberMode = builder.numberMode;
        this.memoryLimitBytes = builder.memoryLimitBytes;
        this.allowCodecUpgrade = builder.allowCodecUpgrade;
        this.codecUpgradeHook = builder.codecUpgradeHook;
        this.autoMigrateDeque = builder.autoMigrateDeque;
    }
    
    /**
     * Returns default options:
     * - CommitMode: AUTO
     * - Durability: ASYNC
     * - OnClosePolicy: ERROR
     * - FileLockMode: PROCESS
     * - PageSize: 4K
     * - CacheBytes: 64 MB
     * - NumberMode: CANONICAL
     * - memoryLimitBytes: Long.MAX_VALUE (unlimited)
     * - allowCodecUpgrade: false
     * - codecUpgradeHook: null
     */
    public static FxOptions defaults() {
        return new Builder().build();
    }
    
    // Getters
    public CommitMode commitMode() { return commitMode; }
    public Durability durability() { return durability; }
    public OnClosePolicy onClosePolicy() { return onClosePolicy; }
    public FileLockMode fileLock() { return fileLock; }
    public PageSize pageSize() { return pageSize; }
    public long cacheBytes() { return cacheBytes; }
    public NumberMode numberMode() { return numberMode; }
    /** 메모리 모드 전용: 최대 메모리 크기 (바이트) */
    public long memoryLimitBytes() { return memoryLimitBytes; }
    /** 코덱 버전 불일치 시 업그레이드 허용 여부 (기본값: false) */
    public boolean allowCodecUpgrade() { return allowCodecUpgrade; }
    /** 코덱 버전 업그레이드 시 데이터 변환 훅 (기본값: null) */
    public FxCodecUpgradeHook codecUpgradeHook() { return codecUpgradeHook; }
    /**
     * Deque 자동 마이그레이션 활성화 여부
     *
     * <p>true: v0.6 이전 Deque를 열 때 자동으로 v0.7 형식(OrderedSeqEncoder)으로 변환
     * <p>false: 레거시 형식 그대로 사용 (peekFirst/peekLast가 O(n))
     *
     * @return 자동 마이그레이션 활성화 여부 (기본값: false)
     * @since 0.7
     */
    public boolean autoMigrateDeque() { return autoMigrateDeque; }

    // Builder methods (return new Builder initialized with current values)
    public Builder withCommitMode(CommitMode commitMode) {
        return toBuilder().commitMode(commitMode);
    }
    
    public Builder withDurability(Durability durability) {
        return toBuilder().durability(durability);
    }
    
    public Builder withOnClosePolicy(OnClosePolicy onClosePolicy) {
        return toBuilder().onClosePolicy(onClosePolicy);
    }
    
    public Builder withFileLock(FileLockMode fileLock) {
        return toBuilder().fileLock(fileLock);
    }
    
    public Builder withPageSize(PageSize pageSize) {
        return toBuilder().pageSize(pageSize);
    }
    
    public Builder withCacheBytes(long cacheBytes) {
        return toBuilder().cacheBytes(cacheBytes);
    }
    
    public Builder withNumberMode(NumberMode numberMode) {
        return toBuilder().numberMode(numberMode);
    }

    public Builder withMemoryLimitBytes(long memoryLimitBytes) {
        return toBuilder().memoryLimitBytes(memoryLimitBytes);
    }

    public Builder withAllowCodecUpgrade(boolean allowCodecUpgrade) {
        return toBuilder().allowCodecUpgrade(allowCodecUpgrade);
    }

    public Builder withCodecUpgradeHook(FxCodecUpgradeHook codecUpgradeHook) {
        return toBuilder().codecUpgradeHook(codecUpgradeHook);
    }

    /**
     * Deque 자동 마이그레이션 활성화 설정
     *
     * @param autoMigrateDeque true면 레거시 Deque를 열 때 자동 마이그레이션
     * @return Builder
     * @since 0.7
     */
    public Builder withAutoMigrateDeque(boolean autoMigrateDeque) {
        return toBuilder().autoMigrateDeque(autoMigrateDeque);
    }

    private Builder toBuilder() {
        return new Builder()
            .commitMode(commitMode)
            .durability(durability)
            .onClosePolicy(onClosePolicy)
            .fileLock(fileLock)
            .pageSize(pageSize)
            .cacheBytes(cacheBytes)
            .numberMode(numberMode)
            .memoryLimitBytes(memoryLimitBytes)
            .allowCodecUpgrade(allowCodecUpgrade)
            .codecUpgradeHook(codecUpgradeHook)
            .autoMigrateDeque(autoMigrateDeque);
    }
    
    /**
     * Builder for FxOptions.
     */
    public static final class Builder {
        private CommitMode commitMode = CommitMode.AUTO;
        private Durability durability = Durability.ASYNC;
        private OnClosePolicy onClosePolicy = OnClosePolicy.ERROR;
        private FileLockMode fileLock = FileLockMode.PROCESS;
        private PageSize pageSize = PageSize.PAGE_4K;
        private long cacheBytes = 64 * 1024 * 1024; // 64 MB
        private NumberMode numberMode = NumberMode.CANONICAL;
        private long memoryLimitBytes = Long.MAX_VALUE;
        private boolean allowCodecUpgrade = false;
        private FxCodecUpgradeHook codecUpgradeHook = null;
        private boolean autoMigrateDeque = false;

        private Builder() {}
        
        public Builder commitMode(CommitMode commitMode) {
            if (commitMode == null) {
                throw FxException.illegalArgument("commitMode cannot be null");
            }
            this.commitMode = commitMode;
            return this;
        }
        
        public Builder durability(Durability durability) {
            if (durability == null) {
                throw FxException.illegalArgument("durability cannot be null");
            }
            this.durability = durability;
            return this;
        }
        
        public Builder onClosePolicy(OnClosePolicy onClosePolicy) {
            if (onClosePolicy == null) {
                throw FxException.illegalArgument("onClosePolicy cannot be null");
            }
            this.onClosePolicy = onClosePolicy;
            return this;
        }
        
        public Builder fileLock(FileLockMode fileLock) {
            if (fileLock == null) {
                throw FxException.illegalArgument("fileLock cannot be null");
            }
            this.fileLock = fileLock;
            return this;
        }
        
        public Builder pageSize(PageSize pageSize) {
            if (pageSize == null) {
                throw FxException.illegalArgument("pageSize cannot be null");
            }
            this.pageSize = pageSize;
            return this;
        }
        
        public Builder cacheBytes(long cacheBytes) {
            if (cacheBytes <= 0) {
                throw FxException.illegalArgument("cacheBytes must be positive");
            }
            this.cacheBytes = cacheBytes;
            return this;
        }
        
        public Builder numberMode(NumberMode numberMode) {
            if (numberMode == null) {
                throw FxException.illegalArgument("numberMode cannot be null");
            }
            if (numberMode == NumberMode.STRICT) {
                throw FxException.unsupported("NumberMode.STRICT is not supported in v0.3");
            }
            this.numberMode = numberMode;
            return this;
        }

        /**
         * 메모리 모드 전용: 최대 메모리 크기 설정
         * @param memoryLimitBytes 최대 메모리 크기 (바이트), 0 이상
         * @throws FxException memoryLimitBytes < 0
         */
        public Builder memoryLimitBytes(long memoryLimitBytes) {
            if (memoryLimitBytes < 0) {
                throw FxException.illegalArgument("memoryLimitBytes cannot be negative: " + memoryLimitBytes);
            }
            this.memoryLimitBytes = memoryLimitBytes;
            return this;
        }

        /**
         * 코덱 버전 불일치 시 업그레이드 허용 여부 설정
         * @param allowCodecUpgrade true면 버전 불일치 시 업그레이드 시도
         */
        public Builder allowCodecUpgrade(boolean allowCodecUpgrade) {
            this.allowCodecUpgrade = allowCodecUpgrade;
            return this;
        }

        /**
         * 코덱 버전 업그레이드 훅 설정
         *
         * <p>주의: codecUpgradeHook을 설정하려면 allowCodecUpgrade=true여야 합니다.</p>
         *
         * @param codecUpgradeHook 업그레이드 훅 (null 허용)
         */
        public Builder codecUpgradeHook(FxCodecUpgradeHook codecUpgradeHook) {
            this.codecUpgradeHook = codecUpgradeHook;
            return this;
        }

        /**
         * Deque 자동 마이그레이션 활성화 설정
         *
         * <p>true: v0.6 이전 Deque를 열 때 자동으로 v0.7 형식(OrderedSeqEncoder)으로 변환
         * <p>false: 레거시 형식 그대로 사용 (peekFirst/peekLast가 O(n))
         *
         * @param autoMigrateDeque 자동 마이그레이션 활성화 여부
         * @since 0.7
         */
        public Builder autoMigrateDeque(boolean autoMigrateDeque) {
            this.autoMigrateDeque = autoMigrateDeque;
            return this;
        }

        public FxOptions build() {
            // codecUpgradeHook 설정 시 allowCodecUpgrade 필수 검증
            if (codecUpgradeHook != null && !allowCodecUpgrade) {
                throw FxException.illegalArgument(
                    "codecUpgradeHook requires allowCodecUpgrade=true");
            }
            return new FxOptions(this);
        }
    }
}
