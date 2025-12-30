package com.snoworca.fxstore.collection;

/**
 * Deque 시퀀스 인코더 인터페이스
 *
 * <p>시퀀스 번호를 바이트 배열로 인코딩/디코딩합니다.
 * BTree의 바이트 비교 순서가 논리적 순서와 일치하도록 보장합니다.</p>
 *
 * <h3>불변식 (Invariant)</h3>
 * <p><b>INV-DQ1</b>: 모든 a, b에 대해
 * {@code a < b ⟺ compareBytes(encode(a), encode(b)) < 0}</p>
 *
 * <h3>SOLID 원칙</h3>
 * <ul>
 *   <li><b>SRP</b>: 시퀀스 인코딩/디코딩만 담당</li>
 *   <li><b>OCP</b>: 새 인코딩 방식은 새 구현체로 추가</li>
 *   <li><b>ISP</b>: 필요한 메서드만 정의</li>
 * </ul>
 *
 * @since 0.7
 * @see OrderedSeqEncoder
 * @see LegacySeqEncoder
 */
public interface SeqEncoder {

    /**
     * 시퀀스 번호를 바이트 배열로 인코딩
     *
     * @param seq 시퀀스 번호 (signed long)
     * @return 8바이트 인코딩 결과
     */
    byte[] encode(long seq);

    /**
     * 바이트 배열을 시퀀스 번호로 디코딩
     *
     * @param bytes 8바이트 인코딩된 데이터
     * @return 원래 시퀀스 번호
     * @throws IllegalArgumentException bytes가 null이거나 길이가 8이 아닌 경우
     */
    long decode(byte[] bytes);

    /**
     * 인코딩 버전 반환
     *
     * @return 버전 바이트 (v1=0x00, v2=0x01)
     */
    byte getVersion();
}
