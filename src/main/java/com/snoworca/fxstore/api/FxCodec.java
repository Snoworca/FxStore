package com.snoworca.fxstore.api;

/**
 * Serialization/deserialization and comparison rules for type T.
 *
 * @param <T> the type to encode/decode
 */
public interface FxCodec<T> {

    /**
     * Codec unique identifier (persisted, must not change).
     * Examples: "fx:i64", "custom:uuid"
     */
    String id();

    /**
     * Codec version (increment when serialization/ordering rules change)
     */
    int version();

    /**
     * Encode value to byte array.
     * Must be deterministic (same input produces same output).
     *
     * @param value the value to encode
     * @return encoded bytes
     * @throws NullPointerException if value is null
     */
    byte[] encode(T value);

    /**
     * Decode byte array to value.
     *
     * @param bytes the bytes to decode
     * @return decoded value
     * @throws NullPointerException if bytes is null
     */
    T decode(byte[] bytes);

    /**
     * Compare two byte arrays for ordering (total order).
     *
     * @param a first byte array
     * @param b second byte array
     * @return negative if a < b, zero if equal, positive if a > b
     */
    int compareBytes(byte[] a, byte[] b);

    /**
     * Check equality of two byte arrays.
     *
     * @param a first byte array
     * @param b second byte array
     * @return true if equal
     */
    boolean equalsBytes(byte[] a, byte[] b);

    /**
     * Compute hash of byte array.
     *
     * @param bytes the bytes to hash
     * @return hash value
     */
    int hashBytes(byte[] bytes);
}
