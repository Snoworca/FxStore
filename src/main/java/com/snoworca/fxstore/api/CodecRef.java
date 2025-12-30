package com.snoworca.fxstore.api;

import java.util.Objects;

/**
 * Codec identification information.
 */
public final class CodecRef {
    private final String codecId;
    private final int codecVersion;
    private final FxType builtinType;

    public CodecRef(String codecId, int codecVersion, FxType builtinType) {
        this.codecId = Objects.requireNonNull(codecId);
        this.codecVersion = codecVersion;
        this.builtinType = builtinType;
    }

    /**
     * Codec unique identifier (e.g., "fx:i64", "custom:uuid")
     */
    public String codecId() {
        return codecId;
    }

    /**
     * Codec version
     */
    public int codecVersion() {
        return codecVersion;
    }

    /**
     * Built-in FxType if applicable, null for custom codecs
     */
    public FxType builtinType() {
        return builtinType;
    }

    /**
     * Alias for builtinType (for compatibility).
     */
    public FxType getType() {
        return builtinType;
    }

    /**
     * Alias for codecId.
     */
    public String getCodecId() {
        return codecId;
    }

    /**
     * Alias for codecVersion.
     */
    public int getCodecVersion() {
        return codecVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodecRef codecRef = (CodecRef) o;
        return codecVersion == codecRef.codecVersion &&
                Objects.equals(codecId, codecRef.codecId) &&
                builtinType == codecRef.builtinType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codecId, codecVersion, builtinType);
    }

    @Override
    public String toString() {
        return "CodecRef{" +
                "codecId='" + codecId + '\'' +
                ", codecVersion=" + codecVersion +
                ", builtinType=" + builtinType +
                '}';
    }

    /**
     * Encode to byte array for persistence.
     */
    public byte[] encode() {
        byte[] idBytes = codecId.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[4 + idBytes.length + 4 + 1];

        // id length
        result[0] = (byte) idBytes.length;
        result[1] = (byte) (idBytes.length >> 8);
        result[2] = (byte) (idBytes.length >> 16);
        result[3] = (byte) (idBytes.length >> 24);

        // id bytes
        System.arraycopy(idBytes, 0, result, 4, idBytes.length);

        // version
        int offset = 4 + idBytes.length;
        result[offset] = (byte) codecVersion;
        result[offset + 1] = (byte) (codecVersion >> 8);
        result[offset + 2] = (byte) (codecVersion >> 16);
        result[offset + 3] = (byte) (codecVersion >> 24);

        // builtinType ordinal (-1 if null)
        result[offset + 4] = (byte) (builtinType != null ? builtinType.ordinal() : -1);

        return result;
    }

    /**
     * Decode from byte array.
     */
    public static CodecRef decode(byte[] data) {
        int idLen = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8) |
                ((data[2] & 0xFF) << 16) | ((data[3] & 0xFF) << 24);

        String id = new String(data, 4, idLen, java.nio.charset.StandardCharsets.UTF_8);

        int offset = 4 + idLen;
        int version = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24);

        int typeOrdinal = data[offset + 4];
        FxType type = typeOrdinal >= 0 && typeOrdinal < FxType.values().length ?
                FxType.values()[typeOrdinal] : null;

        return new CodecRef(id, version, type);
    }
}
