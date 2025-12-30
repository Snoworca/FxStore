package com.snoworca.fxstore.api;

import java.util.Objects;

/**
 * Collection metadata.
 */
public final class CollectionInfo {
    private final String name;
    private final CollectionKind kind;
    private final CodecRef keyCodec;
    private final CodecRef valueCodec;

    public CollectionInfo(String name, CollectionKind kind, CodecRef keyCodec, CodecRef valueCodec) {
        this.name = Objects.requireNonNull(name);
        this.kind = Objects.requireNonNull(kind);
        this.keyCodec = keyCodec;
        this.valueCodec = Objects.requireNonNull(valueCodec);
    }

    /**
     * Collection name
     */
    public String name() {
        return name;
    }

    /**
     * Collection kind
     */
    public CollectionKind kind() {
        return kind;
    }

    /**
     * Key codec (non-null for MAP only, null otherwise)
     */
    public CodecRef keyCodec() {
        return keyCodec;
    }

    /**
     * Value/element codec (always non-null)
     */
    public CodecRef valueCodec() {
        return valueCodec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollectionInfo that = (CollectionInfo) o;
        return Objects.equals(name, that.name) &&
                kind == that.kind &&
                Objects.equals(keyCodec, that.keyCodec) &&
                Objects.equals(valueCodec, that.valueCodec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, kind, keyCodec, valueCodec);
    }

    @Override
    public String toString() {
        return "CollectionInfo{" +
                "name='" + name + '\'' +
                ", kind=" + kind +
                ", keyCodec=" + keyCodec +
                ", valueCodec=" + valueCodec +
                '}';
    }
}
