package com.snoworca.fxstore.api;

import com.snoworca.fxstore.codec.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global codec utilities.
 */
public final class FxCodecs {

    private static final FxCodecRegistry GLOBAL = new GlobalCodecRegistry();

    static {
        // Register built-in codecs
        registerBuiltins();
    }

    @SuppressWarnings("unchecked")
    private static void registerBuiltins() {
        try {
            GLOBAL.register(Long.class, I64Codec.INSTANCE);
            GLOBAL.register(Double.class, F64Codec.INSTANCE);
            GLOBAL.register(String.class, StringCodec.INSTANCE);
            GLOBAL.register(byte[].class, BytesCodec.INSTANCE);
            GLOBAL.register(Integer.class, IntegerCodec.INSTANCE);
            GLOBAL.register(Float.class, FloatCodec.INSTANCE);
            GLOBAL.register(Short.class, ShortCodec.INSTANCE);
            GLOBAL.register(Byte.class, ByteCodec.INSTANCE);
        } catch (FxException e) {
            // Already registered (should not happen during static init)
        }
    }

    private FxCodecs() {
    }

    /**
     * Returns the global codec registry (thread-safe).
     */
    public static FxCodecRegistry global() {
        return GLOBAL;
    }

    private static final class GlobalCodecRegistry implements FxCodecRegistry {
        private final Map<Class<?>, FxCodec<?>> byType = new ConcurrentHashMap<>();
        private final Map<String, FxCodec<?>> byId = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> void register(Class<T> type, FxCodec<T> codec) {
            if (type == null || codec == null) {
                throw new FxException("type and codec must not be null", FxErrorCode.ILLEGAL_ARGUMENT);
            }
            FxCodec<?> existing = byType.putIfAbsent(type, codec);
            if (existing != null) {
                throw new FxException("Codec already registered for type: " + type.getName(),
                        FxErrorCode.ILLEGAL_ARGUMENT);
            }
            byId.put(codec.id() + ":" + codec.version(), codec);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> FxCodec<T> get(Class<T> type) {
            FxCodec<?> codec = byType.get(type);
            if (codec == null) {
                throw new FxException("No codec registered for type: " + type.getName(),
                        FxErrorCode.CODEC_NOT_FOUND);
            }
            return (FxCodec<T>) codec;
        }

        @Override
        public FxCodec<?> getById(String codecId, int version) {
            return byId.get(codecId + ":" + version);
        }
    }
}
