package com.snoworca.fxstore.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Chains multiple upgrade hooks for multi-version jumps.
 */
public final class ChainedCodecUpgradeHook implements FxCodecUpgradeHook {

    private final List<FxCodecUpgradeHook> hooks;
    private final java.util.Map<String, FxCodecUpgradeHook> registeredHooks = new java.util.HashMap<>();

    public ChainedCodecUpgradeHook(List<FxCodecUpgradeHook> hooks) {
        this.hooks = new ArrayList<>(Objects.requireNonNull(hooks));
    }

    /**
     * Default constructor for mutable registration.
     */
    public ChainedCodecUpgradeHook() {
        this.hooks = new ArrayList<>();
    }

    /**
     * Register an upgrade hook for a specific version transition.
     *
     * @param codecId     the codec ID
     * @param fromVersion source version
     * @param toVersion   target version
     * @param hook        the upgrade function
     */
    public void register(String codecId, int fromVersion, int toVersion, FxCodecUpgradeHook hook) {
        String key = codecId + ":" + fromVersion + ":" + toVersion;
        registeredHooks.put(key, hook);
    }

    @Override
    public byte[] upgrade(String codecId, int fromVersion, int toVersion, byte[] oldBytes) {
        byte[] current = oldBytes;

        // Check registered hooks first
        String key = codecId + ":" + fromVersion + ":" + toVersion;
        FxCodecUpgradeHook registered = registeredHooks.get(key);
        if (registered != null) {
            current = registered.upgrade(codecId, fromVersion, toVersion, current);
        }

        // Then run chained hooks
        for (FxCodecUpgradeHook hook : hooks) {
            current = hook.upgrade(codecId, fromVersion, toVersion, current);
        }
        return current;
    }

    /**
     * Create a builder for chaining hooks.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<FxCodecUpgradeHook> hooks = new ArrayList<>();

        public Builder add(FxCodecUpgradeHook hook) {
            hooks.add(Objects.requireNonNull(hook));
            return this;
        }

        public ChainedCodecUpgradeHook build() {
            return new ChainedCodecUpgradeHook(hooks);
        }
    }
}
