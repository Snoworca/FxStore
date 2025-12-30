package com.snoworca.fxstore.catalog;

import com.snoworca.fxstore.api.CollectionKind;
import com.snoworca.fxstore.api.CodecRef;

import java.nio.ByteBuffer;

/**
 * 컬렉션 상태 정보
 * 
 * State BTree의 값으로 저장됨
 * Key: collectionId (Long), Value: CollectionState
 */
public class CollectionState {
    /**
     * SeqEncoder 버전 상수
     *
     * <ul>
     *   <li>VERSION_LEGACY (0): Little Endian 인코딩 (v0.6 호환, O(n))</li>
     *   <li>VERSION_ORDERED (1): XOR + Big Endian 인코딩 (v0.7+, O(log n))</li>
     * </ul>
     */
    public static final byte SEQ_ENCODER_VERSION_LEGACY = 0;
    public static final byte SEQ_ENCODER_VERSION_ORDERED = 1;

    private final long collectionId;
    private final CollectionKind kind;
    private final CodecRef keyCodec;
    private final CodecRef valueCodec;
    private final long rootPageId;
    private final long count;
    private final byte seqEncoderVersion;

    public CollectionState(
            long collectionId,
            CollectionKind kind,
            CodecRef keyCodec,
            CodecRef valueCodec,
            long rootPageId,
            long count) {
        this(collectionId, kind, keyCodec, valueCodec, rootPageId, count, SEQ_ENCODER_VERSION_LEGACY);
    }

    public CollectionState(
            long collectionId,
            CollectionKind kind,
            CodecRef keyCodec,
            CodecRef valueCodec,
            long rootPageId,
            long count,
            byte seqEncoderVersion) {
        this.collectionId = collectionId;
        this.kind = kind;
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
        this.rootPageId = rootPageId;
        this.count = count;
        this.seqEncoderVersion = seqEncoderVersion;
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
    
    public long getRootPageId() {
        return rootPageId;
    }
    
    public long getCount() {
        return count;
    }

    /**
     * Deque 시퀀스 인코더 버전 반환
     *
     * @return SEQ_ENCODER_VERSION_LEGACY (0) 또는 SEQ_ENCODER_VERSION_ORDERED (1)
     */
    public byte getSeqEncoderVersion() {
        return seqEncoderVersion;
    }
    
    /**
     * 인코딩 형식:
     * [0-7]: collectionId (8바이트 LE)
     * [8]: kind ordinal (1바이트)
     * [9-...]: keyCodec encoded (가변)
     * [...-...]: valueCodec encoded or null marker (가변)
     * [...-+7]: rootPageId (8바이트 LE)
     * [...-+7]: count (8바이트 LE)
     * [...]: seqEncoderVersion (1바이트, v0.7+)
     */
    public byte[] encode() {
        byte[] keyCodecBytes = keyCodec != null ? keyCodec.encode() : new byte[0];
        byte[] valueCodecBytes = valueCodec != null ? valueCodec.encode() : new byte[0];

        // +1 for seqEncoderVersion
        int totalSize = 8 + 1 + 4 + keyCodecBytes.length + 4 + valueCodecBytes.length + 8 + 8 + 1;

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        buffer.putLong(collectionId);
        buffer.put((byte) kind.ordinal());

        buffer.putInt(keyCodecBytes.length);
        buffer.put(keyCodecBytes);

        buffer.putInt(valueCodecBytes.length);
        buffer.put(valueCodecBytes);

        buffer.putLong(rootPageId);
        buffer.putLong(count);
        buffer.put(seqEncoderVersion);

        return buffer.array();
    }
    
    /**
     * 디코딩
     *
     * <p>하위 호환성: v0.6 이전 데이터에는 seqEncoderVersion이 없음 → LEGACY로 처리</p>
     */
    public static CollectionState decode(byte[] data) {
        if (data == null || data.length < 25) {
            throw new IllegalArgumentException("Invalid CollectionState data");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        long collectionId = buffer.getLong();
        CollectionKind kind = CollectionKind.values()[buffer.get()];

        int keyCodecLen = buffer.getInt();
        CodecRef keyCodec = null;
        if (keyCodecLen > 0) {
            byte[] keyCodecBytes = new byte[keyCodecLen];
            buffer.get(keyCodecBytes);
            keyCodec = CodecRef.decode(keyCodecBytes);
        }

        int valueCodecLen = buffer.getInt();
        CodecRef valueCodec = null;
        if (valueCodecLen > 0) {
            byte[] valueCodecBytes = new byte[valueCodecLen];
            buffer.get(valueCodecBytes);
            valueCodec = CodecRef.decode(valueCodecBytes);
        }

        long rootPageId = buffer.getLong();
        long count = buffer.getLong();

        // v0.7+: seqEncoderVersion 읽기 (하위 호환성: 없으면 LEGACY)
        byte seqEncoderVersion = SEQ_ENCODER_VERSION_LEGACY;
        if (buffer.hasRemaining()) {
            seqEncoderVersion = buffer.get();
        }

        return new CollectionState(collectionId, kind, keyCodec, valueCodec, rootPageId, count, seqEncoderVersion);
    }
    
    @Override
    public String toString() {
        return "CollectionState{" +
                "collectionId=" + collectionId +
                ", kind=" + kind +
                ", keyCodec=" + keyCodec +
                ", valueCodec=" + valueCodec +
                ", rootPageId=" + rootPageId +
                ", count=" + count +
                ", seqEncoderVersion=" + seqEncoderVersion +
                '}';
    }
    
    /**
     * rootPageId를 변경한 새로운 CollectionState 생성
     */
    public CollectionState withRootPageId(long newRootPageId) {
        return new CollectionState(
                collectionId, kind, keyCodec, valueCodec,
                newRootPageId, count, seqEncoderVersion);
    }

    /**
     * count를 변경한 새로운 CollectionState 생성
     */
    public CollectionState withCount(long newCount) {
        return new CollectionState(
                collectionId, kind, keyCodec, valueCodec,
                rootPageId, newCount, seqEncoderVersion);
    }

    /**
     * rootPageId와 count를 모두 변경한 새로운 CollectionState 생성
     */
    public CollectionState withRootAndCount(long newRootPageId, long newCount) {
        return new CollectionState(
                collectionId, kind, keyCodec, valueCodec,
                newRootPageId, newCount, seqEncoderVersion);
    }

    /**
     * seqEncoderVersion을 변경한 새로운 CollectionState 생성 (마이그레이션용)
     */
    public CollectionState withSeqEncoderVersion(byte newSeqEncoderVersion) {
        return new CollectionState(
                collectionId, kind, keyCodec, valueCodec,
                rootPageId, count, newSeqEncoderVersion);
    }
}
