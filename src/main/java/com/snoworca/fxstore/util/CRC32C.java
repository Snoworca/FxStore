package com.snoworca.fxstore.util;

import java.util.zip.CRC32;

/**
 * CRC32C checksum utility.
 * Uses Java's CRC32 as fallback (CRC32C requires Java 9+).
 * For Java 8 compatibility, this uses standard CRC32.
 */
public final class CRC32C {

    private CRC32C() {
    }

    /**
     * Compute CRC32 checksum of data.
     *
     * @param data   byte array
     * @param offset start offset
     * @param length number of bytes
     * @return CRC32 value as int
     */
    public static int compute(byte[] data, int offset, int length) {
        CRC32 crc = new CRC32();
        crc.update(data, offset, length);
        return (int) crc.getValue();
    }

    /**
     * Compute CRC32 checksum of entire array.
     */
    public static int compute(byte[] data) {
        return compute(data, 0, data.length);
    }

    /**
     * Verify CRC32 checksum.
     *
     * @param data     byte array
     * @param offset   start offset
     * @param length   number of bytes (excluding CRC field)
     * @param expected expected CRC value
     * @return true if checksum matches
     */
    public static boolean verify(byte[] data, int offset, int length, int expected) {
        return compute(data, offset, length) == expected;
    }
}
