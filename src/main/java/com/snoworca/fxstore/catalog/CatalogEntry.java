package com.snoworca.fxstore.catalog;

import com.snoworca.fxstore.api.CodecRef;
import com.snoworca.fxstore.api.CollectionKind;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Catalog entry representing a collection's metadata.
 * Stored in the Catalog B+Tree with name as key.
 */
public class CatalogEntry {

    private final String name;
    private final long collectionId;
    private final CollectionKind kind;
    private final CodecRef keyCodec;
    private final CodecRef valueCodec;

    public CatalogEntry(String name, long collectionId, CollectionKind kind,
                        CodecRef keyCodec, CodecRef valueCodec) {
        this.name = name;
        this.collectionId = collectionId;
        this.kind = kind;
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
    }

    /**
     * Simplified constructor for list/deque collections.
     */
    public CatalogEntry(String name, long collectionId) {
        this(name, collectionId, CollectionKind.LIST, null, null);
    }

    /**
     * Constructor with kind only.
     */
    public CatalogEntry(String name, long collectionId, CollectionKind kind) {
        this(name, collectionId, kind, null, null);
    }

    public String getName() {
        return name;
    }

    public long getCollectionId() {
        return collectionId;
    }

    public CollectionKind getKind() {
        return kind;
    }

    public CodecRef getKeyCodec() {
        return keyCodec;
    }

    public CodecRef getValueCodec() {
        return valueCodec;
    }

    public byte[] serialize() {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] keyCodecIdBytes = keyCodec != null ? keyCodec.codecId().getBytes(StandardCharsets.UTF_8) : new byte[0];
        byte[] valueCodecIdBytes = valueCodec != null ? valueCodec.codecId().getBytes(StandardCharsets.UTF_8) : new byte[0];

        int size = 4 + nameBytes.length +  // name
                8 +                          // collectionId
                1 +                          // kind ordinal
                1 + (keyCodec != null ? 4 + keyCodecIdBytes.length + 4 : 0) + // keyCodec
                1 + (valueCodec != null ? 4 + valueCodecIdBytes.length + 4 : 0); // valueCodec

        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

        buf.putInt(nameBytes.length);
        buf.put(nameBytes);
        buf.putLong(collectionId);
        buf.put((byte) kind.ordinal());

        // Key codec (null for non-MAP)
        if (keyCodec != null) {
            buf.put((byte) 1);
            buf.putInt(keyCodecIdBytes.length);
            buf.put(keyCodecIdBytes);
            buf.putInt(keyCodec.codecVersion());
        } else {
            buf.put((byte) 0);
        }

        // Value codec (can be null for lightweight CatalogEntry)
        if (valueCodec != null) {
            buf.put((byte) 1);
            buf.putInt(valueCodecIdBytes.length);
            buf.put(valueCodecIdBytes);
            buf.putInt(valueCodec.codecVersion());
        } else {
            buf.put((byte) 0);
        }

        return buf.array();
    }

    /**
     * Encode to byte array (alias for serialize).
     */
    public byte[] encode() {
        return serialize();
    }

    /**
     * Decode from byte array (alias for deserialize).
     */
    public static CatalogEntry decode(byte[] data) {
        return deserialize(data);
    }

    public static CatalogEntry deserialize(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int nameLen = buf.getInt();
        byte[] nameBytes = new byte[nameLen];
        buf.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);

        long collectionId = buf.getLong();
        CollectionKind kind = CollectionKind.values()[buf.get()];

        CodecRef keyCodec = null;
        if (buf.get() == 1) {
            int keyCodecIdLen = buf.getInt();
            byte[] keyCodecIdBytes = new byte[keyCodecIdLen];
            buf.get(keyCodecIdBytes);
            String keyCodecId = new String(keyCodecIdBytes, StandardCharsets.UTF_8);
            int keyCodecVersion = buf.getInt();
            keyCodec = new CodecRef(keyCodecId, keyCodecVersion, null);
        }

        CodecRef valueCodec = null;
        if (buf.get() == 1) {
            int valueCodecIdLen = buf.getInt();
            byte[] valueCodecIdBytes = new byte[valueCodecIdLen];
            buf.get(valueCodecIdBytes);
            String valueCodecId = new String(valueCodecIdBytes, StandardCharsets.UTF_8);
            int valueCodecVersion = buf.getInt();
            valueCodec = new CodecRef(valueCodecId, valueCodecVersion, null);
        }

        return new CatalogEntry(name, collectionId, kind, keyCodec, valueCodec);
    }

    @Override
    public String toString() {
        return "CatalogEntry{" +
                "name='" + name + '\'' +
                ", collectionId=" + collectionId +
                ", kind=" + kind +
                ", keyCodec=" + keyCodec +
                ", valueCodec=" + valueCodec +
                '}';
    }
}
