package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.FxException;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Superblock 손상 및 검증 실패 테스트
 *
 * <p>V17 커버리지 개선: CRC 검증 실패, Magic 검증 실패 경로 테스트</p>
 */
public class SuperblockCorruptionTest {

    private static final int PAGE_SIZE = 4096;

    // ==================== CRC 검증 실패 테스트 ====================

    @Test
    public void verify_corruptedCrc_shouldReturnFalse() {
        // Given: 정상 Superblock 생성
        Superblock superblock = Superblock.create(PAGE_SIZE);
        byte[] data = superblock.encode();

        // When: CRC 영역 손상 (마지막 4바이트가 CRC)
        data[data.length - 1] ^= 0xFF;

        // Then: 검증 실패
        assertFalse(Superblock.verify(data));
    }

    @Test
    public void verify_allZeroCrc_shouldReturnFalse() {
        // Given: 정상 Superblock 생성
        Superblock superblock = Superblock.create(PAGE_SIZE);
        byte[] data = superblock.encode();

        // When: CRC를 모두 0으로 설정
        int crcOffset = data.length - 4;
        data[crcOffset] = 0;
        data[crcOffset + 1] = 0;
        data[crcOffset + 2] = 0;
        data[crcOffset + 3] = 0;

        // Then: 검증 실패
        assertFalse(Superblock.verify(data));
    }

    @Test
    public void verify_singleBitFlip_shouldReturnFalse() {
        // Given: 정상 Superblock 생성
        Superblock superblock = Superblock.create(PAGE_SIZE);
        byte[] data = superblock.encode();

        // When: 단일 비트 변조
        data[16] ^= 0x01;

        // Then: 검증 실패
        assertFalse(Superblock.verify(data));
    }

    // ==================== Magic 검증 실패 테스트 ====================

    @Test
    public void verify_corruptedMagic_shouldReturnFalse() {
        // Given: 정상 Superblock 생성
        Superblock superblock = Superblock.create(PAGE_SIZE);
        byte[] data = superblock.encode();

        // When: Magic 영역 손상 (처음 8바이트가 Magic)
        data[0] = (byte) 'X';

        // Then: 검증 실패
        assertFalse(Superblock.verify(data));
    }

    @Test
    public void verify_allZeroMagic_shouldReturnFalse() {
        // Given: 정상 Superblock 생성
        Superblock superblock = Superblock.create(PAGE_SIZE);
        byte[] data = superblock.encode();

        // When: Magic을 모두 0으로 설정
        for (int i = 0; i < 8; i++) {
            data[i] = 0;
        }

        // Then: 검증 실패
        assertFalse(Superblock.verify(data));
    }

    @Test
    public void verify_differentMagic_shouldReturnFalse() {
        // Given: 정상 Superblock 생성
        Superblock superblock = Superblock.create(PAGE_SIZE);
        byte[] data = superblock.encode();

        // When: Magic을 다른 값으로 설정
        byte[] wrongMagic = "WRONGMAG".getBytes();
        System.arraycopy(wrongMagic, 0, data, 0, 8);

        // Then: 검증 실패
        assertFalse(Superblock.verify(data));
    }

    // ==================== 정상 케이스 테스트 ====================

    @Test
    public void verify_validSuperblock_shouldReturnTrue() {
        // Given: 정상 Superblock 생성
        Superblock superblock = Superblock.create(PAGE_SIZE);
        byte[] data = superblock.encode();

        // Then: 검증 성공
        assertTrue(Superblock.verify(data));
    }

    @Test
    public void verify_afterDecode_shouldReturnTrue() {
        // Given: Superblock 생성 후 디코딩
        Superblock original = Superblock.create(PAGE_SIZE);
        byte[] data = original.encode();
        Superblock decoded = Superblock.decode(data);

        // When: 디코딩된 Superblock을 다시 인코딩
        byte[] reEncoded = decoded.encode();

        // Then: 검증 성공
        assertTrue(Superblock.verify(reEncoded));
    }

    // ==================== 빈 데이터 테스트 ====================

    @Test
    public void verify_emptyData_shouldReturnFalse() {
        // Given: 빈 데이터
        byte[] data = new byte[0];

        // Then: 검증 실패
        assertFalse(Superblock.verify(data));
    }

    @Test
    public void verify_tooShortData_shouldReturnFalse() {
        // Given: 너무 짧은 데이터
        byte[] data = new byte[10];

        // Then: 검증 실패
        assertFalse(Superblock.verify(data));
    }

    @Test
    public void verify_nullData_shouldReturnFalse() {
        // Then: null 데이터는 검증 실패
        assertFalse(Superblock.verify(null));
    }

    // ==================== decode 예외 테스트 ====================

    @Test(expected = Exception.class)
    public void decode_corruptedData_shouldThrow() {
        // Given: 손상된 데이터
        byte[] data = new byte[64];
        Arrays.fill(data, (byte) 0xFF);

        // When: 디코딩 시도
        Superblock.decode(data);
    }

    @Test(expected = Exception.class)
    public void decode_wrongMagic_shouldThrow() {
        // Given: 잘못된 Magic
        Superblock superblock = Superblock.create(PAGE_SIZE);
        byte[] data = superblock.encode();
        data[0] = 'X';

        // When: 디코딩 시도
        Superblock.decode(data);
    }

    // ==================== 페이지 크기 테스트 ====================

    @Test
    public void verify_differentPageSizes_shouldWork() {
        int[] pageSizes = {4096, 8192, 16384, 32768};

        for (int pageSize : pageSizes) {
            Superblock superblock = Superblock.create(pageSize);
            byte[] data = superblock.encode();
            assertTrue("Page size " + pageSize + " verification failed",
                      Superblock.verify(data));
        }
    }

    @Test
    public void verify_afterPageSizeChange_shouldFail() {
        // Given: 특정 페이지 크기로 생성
        Superblock superblock = Superblock.create(4096);
        byte[] data = superblock.encode();

        // When: 페이지 크기 필드 변조
        // pageSize는 offset 8에서 4바이트 (Little Endian)
        data[8] = (byte) 0x00;
        data[9] = (byte) 0x10; // 4096 -> 다른 값으로 변경

        // Then: 검증 실패 (CRC 불일치)
        assertFalse(Superblock.verify(data));
    }
}
