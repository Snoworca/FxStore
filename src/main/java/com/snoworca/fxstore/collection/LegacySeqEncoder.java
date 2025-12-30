package com.snoworca.fxstore.collection;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 레거시 시퀀스 인코더 (v0.6 이전 호환)
 *
 * <p>기존 Little Endian 인코딩을 유지하여 마이그레이션 전 데이터와 호환됩니다.</p>
 *
 * <h3>주의</h3>
 * <p>이 인코더는 바이트 순서가 논리적 순서와 일치하지 않습니다.
 * 새 Deque에는 {@link OrderedSeqEncoder}를 사용하세요.</p>
 *
 * <h3>문제점</h3>
 * <pre>
 * Little Endian 인코딩:
 * seq = -2 → bytes = [FE FF FF FF FF FF FF FF]
 * seq = -1 → bytes = [FF FF FF FF FF FF FF FF]
 * seq =  0 → bytes = [00 00 00 00 00 00 00 00]
 * seq =  1 → bytes = [01 00 00 00 00 00 00 00]
 *
 * BTree 바이트 비교: [00..00] < [01..00] < [FE..FF] < [FF..FF]
 * 실제 순서:         seq=0  < seq=1  < seq=-2 < seq=-1  ✗
 * </pre>
 *
 * @since 0.7 (backward compatibility)
 * @see OrderedSeqEncoder
 */
public final class LegacySeqEncoder implements SeqEncoder {

    /** 인코딩 버전: v1 (레거시) */
    public static final byte VERSION = 0x00;

    /** 싱글턴 인스턴스 */
    private static final LegacySeqEncoder INSTANCE = new LegacySeqEncoder();

    private LegacySeqEncoder() {
    }

    /**
     * 싱글턴 인스턴스 반환
     *
     * @return LegacySeqEncoder 인스턴스
     */
    public static LegacySeqEncoder getInstance() {
        return INSTANCE;
    }

    /**
     * 시퀀스를 Little Endian 바이트로 인코딩 (레거시)
     *
     * @param seq 시퀀스 번호
     * @return 8바이트 인코딩 결과
     */
    @Override
    public byte[] encode(long seq) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(seq);
        return buf.array();
    }

    /**
     * 바이트를 시퀀스로 디코딩
     *
     * @param bytes 8바이트 인코딩된 데이터
     * @return 원래 시퀀스 번호
     * @throws IllegalArgumentException bytes가 null이거나 길이가 8이 아닌 경우
     */
    @Override
    public long decode(byte[] bytes) {
        if (bytes == null || bytes.length != 8) {
            throw new IllegalArgumentException(
                "bytes must be non-null and exactly 8 bytes, got: " +
                (bytes == null ? "null" : bytes.length));
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf.getLong();
    }

    @Override
    public byte getVersion() {
        return VERSION;
    }

    @Override
    public String toString() {
        return "LegacySeqEncoder{version=" + VERSION + "}";
    }
}
