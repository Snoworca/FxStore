package com.snoworca.fxstore.api;

/**
 * Number type handling mode.
 */
public enum NumberMode {
    /**
     * Normalize integers to I64, floats to F64.
     * Mixed usage allowed if normalized type matches.
     */
    CANONICAL,

    /**
     * Distinguish exact number types.
     * Integer(1) and Long(1) are different keys.
     */
    STRICT
}
