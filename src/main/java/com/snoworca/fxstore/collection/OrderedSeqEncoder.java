package com.snoworca.fxstore.collection;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 순서 보장 시퀀스 인코더 (v0.7)
 *
 * <p>signed long 시퀀스를 BTree 바이트 비교 순서가 논리적 순서와 일치하도록 인코딩합니다.</p>
 *
 * <h3>인코딩 알고리즘</h3>
 * <ol>
 *   <li>XOR with Long.MIN_VALUE: signed → unsigned 변환</li>
 *   <li>Big Endian 인코딩: MSB first로 바이트 순서 보장</li>
 * </ol>
 *
 * <h3>수학적 근거</h3>
 * <pre>
 * signed long 범위:  [-2^63, 2^63-1]
 * XOR 후 범위:       [0, 2^64-1] (unsigned 순서)
 *
 * 예시:
 * seq = -2 → XOR → 0x7FFFFFFFFFFFFFFE → bytes = [7F FF FF FF FF FF FF FE]
 * seq = -1 → XOR → 0x7FFFFFFFFFFFFFFF → bytes = [7F FF FF FF FF FF FF FF]
 * seq =  0 → XOR → 0x8000000000000000 → bytes = [80 00 00 00 00 00 00 00]
 * seq =  1 → XOR → 0x8000000000000001 → bytes = [80 00 00 00 00 00 00 01]
 *
 * 바이트 순서: [7F..FE] < [7F..FF] < [80..00] < [80..01]
 * 논리 순서:   -2      < -1      < 0       < 1        ✓
 * </pre>
 *
 * <h3>불변식</h3>
 * <p><b>INV-DQ1</b>: {@code a < b ⟺ compareBytes(encode(a), encode(b)) < 0}</p>
 *
 * @since 0.7
 */
public final class OrderedSeqEncoder implements SeqEncoder {

    /** 인코딩 버전: v2 (순서 보장) */
    public static final byte VERSION = 0x01;

    /** 싱글턴 인스턴스 */
    private static final OrderedSeqEncoder INSTANCE = new OrderedSeqEncoder();

    private OrderedSeqEncoder() {
    }

    /**
     * 싱글턴 인스턴스 반환
     *
     * @return OrderedSeqEncoder 인스턴스
     */
    public static OrderedSeqEncoder getInstance() {
        return INSTANCE;
    }

    /**
     * 시퀀스를 순서 보장 바이트로 인코딩
     *
     * <p>XOR with Long.MIN_VALUE로 signed → unsigned 변환 후
     * Big Endian으로 인코딩하여 바이트 순서 = 논리적 순서를 보장합니다.</p>
     *
     * @param seq 시퀀스 번호
     * @return 8바이트 인코딩 결과
     */
    @Override
    public byte[] encode(long seq) {
        // XOR with sign bit: signed → unsigned 순서 변환
        long unsigned = seq ^ Long.MIN_VALUE;

        // Big Endian: MSB first로 바이트 순서 보장
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(unsigned);
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
        buf.order(ByteOrder.BIG_ENDIAN);
        long unsigned = buf.getLong();

        // XOR with sign bit: unsigned → signed 역변환
        return unsigned ^ Long.MIN_VALUE;
    }

    @Override
    public byte getVersion() {
        return VERSION;
    }

    @Override
    public String toString() {
        return "OrderedSeqEncoder{version=" + VERSION + "}";
    }
}
