package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.api.FxCodecUpgradeHook;

/**
 * Context for codec version upgrade operations.
 */
public class CodecUpgradeContext {

    private final FxCodec<?> codec;
    private final String codecId;
    private final int fromVersion;
    private final int toVersion;
    private final FxCodecUpgradeHook upgradeHook;
    private final boolean upgradeNeeded;

    public CodecUpgradeContext(FxCodec<?> codec, int fromVersion, int toVersion,
                               FxCodecUpgradeHook upgradeHook, boolean upgradeNeeded) {
        this.codec = codec;
        this.codecId = codec != null ? codec.id() : null;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.upgradeHook = upgradeHook;
        this.upgradeNeeded = upgradeNeeded;
    }

    /**
     * Constructor with codec ID string (for compatibility).
     */
    public CodecUpgradeContext(String codecId, int fromVersion, int toVersion,
                               FxCodecUpgradeHook upgradeHook) {
        this.codec = null;
        this.codecId = codecId;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.upgradeHook = upgradeHook;
        this.upgradeNeeded = (fromVersion != toVersion);
    }

    /**
     * Create context for matching versions (no upgrade needed).
     */
    public static CodecUpgradeContext noUpgrade(FxCodec<?> codec) {
        return new CodecUpgradeContext(codec, codec.version(), codec.version(), null, false);
    }

    /**
     * Create context for version mismatch (upgrade needed).
     */
    public static CodecUpgradeContext needsUpgrade(FxCodec<?> codec, int fromVersion,
                                                   FxCodecUpgradeHook hook) {
        return new CodecUpgradeContext(codec, fromVersion, codec.version(), hook, true);
    }

    public FxCodec<?> getCodec() {
        return codec;
    }

    public int getFromVersion() {
        return fromVersion;
    }

    public int getToVersion() {
        return toVersion;
    }

    public FxCodecUpgradeHook getUpgradeHook() {
        return upgradeHook;
    }

    public boolean isUpgradeNeeded() {
        return upgradeNeeded;
    }

    /**
     * Get the codec ID (either from codec or stored string).
     */
    public String getCodecId() {
        return codecId;
    }

    /**
     * Upgrade bytes if needed.
     *
     * @param oldBytes bytes encoded with old version
     * @return bytes in new version format
     */
    public byte[] upgradeIfNeeded(byte[] oldBytes) {
        if (!upgradeNeeded || upgradeHook == null) {
            return oldBytes;
        }
        return upgradeHook.upgrade(codecId, fromVersion, toVersion, oldBytes);
    }
}
