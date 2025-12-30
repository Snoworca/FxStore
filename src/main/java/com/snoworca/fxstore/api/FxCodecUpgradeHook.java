package com.snoworca.fxstore.api;

/**
 * Codec version upgrade hook.
 */
public interface FxCodecUpgradeHook {

    /**
     * Convert bytes from old version to new version.
     *
     * @param codecId     the codec ID
     * @param fromVersion the stored version
     * @param toVersion   the current codec version
     * @param oldBytes    bytes encoded with old version
     * @return bytes encoded with new version (must not be null)
     * @throws RuntimeException if conversion fails (wrapped as FxException(UPGRADE_FAILED))
     */
    byte[] upgrade(String codecId, int fromVersion, int toVersion, byte[] oldBytes);
}
