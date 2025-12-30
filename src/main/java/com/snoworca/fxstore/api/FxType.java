package com.snoworca.fxstore.api;

/**
 * Built-in value types.
 */
public enum FxType {
    /** Integer (8-byte LE, signed) */
    I64,
    /** Float (IEEE754 8-byte) */
    F64,
    /** String (UTF-8) */
    STRING,
    /** Byte array (length-prefixed lexicographic ordering) */
    BYTES
}
