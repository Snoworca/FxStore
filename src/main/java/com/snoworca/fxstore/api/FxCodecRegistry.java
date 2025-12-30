package com.snoworca.fxstore.api;

/**
 * Codec registry interface.
 */
public interface FxCodecRegistry {

    /**
     * Register a codec for a type.
     *
     * @param type  the class to register codec for
     * @param codec the codec implementation
     * @param <T>   the type
     * @throws FxException with ILLEGAL_ARGUMENT if already registered
     */
    <T> void register(Class<T> type, FxCodec<T> codec);

    /**
     * Get codec by type.
     *
     * @param type the class to look up
     * @param <T>  the type
     * @return the registered codec
     * @throws FxException with CODEC_NOT_FOUND if not registered
     */
    <T> FxCodec<T> get(Class<T> type);

    /**
     * Get codec by ID and version (for descriptor validation).
     *
     * @param codecId the codec ID
     * @param version the codec version
     * @return the matching codec, or null if not found
     */
    FxCodec<?> getById(String codecId, int version);
}
