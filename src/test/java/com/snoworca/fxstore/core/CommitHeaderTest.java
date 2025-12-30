package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.FxErrorCode;
import com.snoworca.fxstore.api.FxException;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * CommitHeader 테스트
 * P2 클래스 커버리지 개선
 */
public class CommitHeaderTest {

    // ==================== 상수 테스트 ====================

    @Test
    public void size_shouldBe4096() {
        assertEquals(4096, CommitHeader.SIZE);
    }

    @Test
    public void magic_shouldBeCorrect() {
        // "FXHDR\0\0\0"
        byte[] expected = {0x46, 0x58, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00};
        assertArrayEquals(expected, CommitHeader.MAGIC);
    }

    @Test
    public void currentVersion_shouldBe1() {
        assertEquals(1, CommitHeader.CURRENT_VERSION);
    }

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_shouldSetAllFields() {
        // Given & When
        CommitHeader header = new CommitHeader(
            100L,   // seqNo
            1L,     // committedFlags
            8192L,  // allocTail
            4096L,  // catalogRootPageId
            12288L, // stateRootPageId
            50L,    // nextCollectionId
            System.currentTimeMillis() // commitEpochMs
        );

        // Then
        assertEquals(100L, header.getSeqNo());
        assertEquals(1L, header.getCommittedFlags());
        assertEquals(8192L, header.getAllocTail());
        assertEquals(4096L, header.getCatalogRootPageId());
        assertEquals(12288L, header.getStateRootPageId());
        assertEquals(50L, header.getNextCollectionId());
    }

    @Test
    public void constructor_zeroValues_shouldWork() {
        // Given & When
        CommitHeader header = new CommitHeader(0L, 0L, 0L, 0L, 0L, 0L, 0L);

        // Then
        assertEquals(0L, header.getSeqNo());
        assertEquals(0L, header.getCommittedFlags());
        assertEquals(0L, header.getAllocTail());
        assertEquals(0L, header.getCatalogRootPageId());
        assertEquals(0L, header.getStateRootPageId());
        assertEquals(0L, header.getNextCollectionId());
        assertEquals(0L, header.getCommitEpochMs());
    }

    // ==================== encode / decode 테스트 ====================

    @Test
    public void encode_shouldProduceCorrectSize() {
        // Given
        CommitHeader header = createSampleHeader();

        // When
        byte[] encoded = header.encode();

        // Then
        assertEquals(CommitHeader.SIZE, encoded.length);
    }

    @Test
    public void encode_shouldStartWithMagic() {
        // Given
        CommitHeader header = createSampleHeader();

        // When
        byte[] encoded = header.encode();

        // Then
        byte[] magic = Arrays.copyOfRange(encoded, 0, 8);
        assertArrayEquals(CommitHeader.MAGIC, magic);
    }

    @Test
    public void encode_decode_shouldRoundTrip() {
        // Given
        long now = System.currentTimeMillis();
        CommitHeader original = new CommitHeader(
            1L,     // seqNo
            2L,     // committedFlags
            4096L,  // allocTail
            8192L,  // catalogRootPageId
            12288L, // stateRootPageId
            100L,   // nextCollectionId
            now     // commitEpochMs
        );

        // When
        byte[] encoded = original.encode();
        CommitHeader decoded = CommitHeader.decode(encoded);

        // Then
        assertEquals(original.getSeqNo(), decoded.getSeqNo());
        assertEquals(original.getCommittedFlags(), decoded.getCommittedFlags());
        assertEquals(original.getAllocTail(), decoded.getAllocTail());
        assertEquals(original.getCatalogRootPageId(), decoded.getCatalogRootPageId());
        assertEquals(original.getStateRootPageId(), decoded.getStateRootPageId());
        assertEquals(original.getNextCollectionId(), decoded.getNextCollectionId());
        assertEquals(original.getCommitEpochMs(), decoded.getCommitEpochMs());
    }

    @Test
    public void encode_decode_largeValues_shouldRoundTrip() {
        // Given
        CommitHeader original = new CommitHeader(
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE
        );

        // When
        byte[] encoded = original.encode();
        CommitHeader decoded = CommitHeader.decode(encoded);

        // Then
        assertEquals(original.getSeqNo(), decoded.getSeqNo());
        assertEquals(original.getAllocTail(), decoded.getAllocTail());
    }

    @Test(expected = FxException.class)
    public void decode_tooSmall_shouldThrow() {
        // Given
        byte[] data = new byte[100]; // Too small

        // When & Then
        CommitHeader.decode(data);
    }

    @Test
    public void decode_invalidMagic_shouldThrow() {
        // Given
        byte[] data = new byte[CommitHeader.SIZE];
        Arrays.fill(data, (byte) 0);

        // When & Then
        try {
            CommitHeader.decode(data);
            fail("Should throw FxException");
        } catch (FxException e) {
            assertEquals(FxErrorCode.CORRUPTION, e.code());
            assertTrue(e.getMessage().contains("magic"));
        }
    }

    @Test
    public void decode_invalidVersion_shouldThrow() {
        // Given: Valid magic but wrong version
        CommitHeader header = createSampleHeader();
        byte[] data = header.encode();
        // Change version bytes (offset 8-11)
        data[8] = 99; // Invalid version

        // When & Then
        try {
            CommitHeader.decode(data);
            fail("Should throw FxException");
        } catch (FxException e) {
            assertEquals(FxErrorCode.VERSION_MISMATCH, e.code());
        }
    }

    // ==================== verify 테스트 ====================

    @Test
    public void verify_validData_shouldReturnTrue() {
        // Given
        CommitHeader header = createSampleHeader();
        byte[] encoded = header.encode();

        // When
        boolean result = header.verify(encoded);

        // Then
        assertTrue(result);
    }

    @Test
    public void verify_corruptedData_shouldReturnFalse() {
        // Given
        CommitHeader header = createSampleHeader();
        byte[] encoded = header.encode();
        // Corrupt a byte in the data area
        encoded[50] ^= 0xFF;

        // When
        boolean result = header.verify(encoded);

        // Then
        assertFalse(result);
    }

    @Test
    public void verify_tooSmall_shouldReturnFalse() {
        // Given
        CommitHeader header = createSampleHeader();
        byte[] tooSmall = new byte[100];

        // When
        boolean result = header.verify(tooSmall);

        // Then
        assertFalse(result);
    }

    @Test
    public void verify_corruptedCrc_shouldReturnFalse() {
        // Given
        CommitHeader header = createSampleHeader();
        byte[] encoded = header.encode();
        // Corrupt CRC bytes (last 4 bytes)
        encoded[4092] ^= 0xFF;

        // When
        boolean result = header.verify(encoded);

        // Then
        assertFalse(result);
    }

    // ==================== selectHeader 테스트 ====================

    @Test
    public void selectHeader_bothValid_shouldReturnLargerSeqNo() {
        // Given
        CommitHeader headerA = new CommitHeader(10L, 0L, 4096L, 8192L, 12288L, 1L, 0L);
        CommitHeader headerB = new CommitHeader(20L, 0L, 4096L, 8192L, 12288L, 1L, 0L);

        byte[] slotA = headerA.encode();
        byte[] slotB = headerB.encode();

        // When
        CommitHeader selected = CommitHeader.selectHeader(slotA, slotB);

        // Then: B has larger seqNo
        assertEquals(20L, selected.getSeqNo());
    }

    @Test
    public void selectHeader_bothValid_equalSeqNo_shouldReturnA() {
        // Given
        CommitHeader headerA = new CommitHeader(10L, 0L, 4096L, 8192L, 12288L, 1L, 0L);
        CommitHeader headerB = new CommitHeader(10L, 0L, 4096L, 8192L, 12288L, 1L, 0L);

        byte[] slotA = headerA.encode();
        byte[] slotB = headerB.encode();

        // When
        CommitHeader selected = CommitHeader.selectHeader(slotA, slotB);

        // Then: A should be selected (>= comparison)
        assertEquals(10L, selected.getSeqNo());
    }

    @Test
    public void selectHeader_onlyAValid_shouldReturnA() {
        // Given
        CommitHeader headerA = new CommitHeader(10L, 0L, 4096L, 8192L, 12288L, 1L, 0L);
        byte[] slotA = headerA.encode();
        byte[] slotB = new byte[CommitHeader.SIZE]; // Invalid B

        // When
        CommitHeader selected = CommitHeader.selectHeader(slotA, slotB);

        // Then
        assertEquals(10L, selected.getSeqNo());
    }

    @Test
    public void selectHeader_onlyBValid_shouldReturnB() {
        // Given
        CommitHeader headerB = new CommitHeader(20L, 0L, 4096L, 8192L, 12288L, 1L, 0L);
        byte[] slotA = new byte[CommitHeader.SIZE]; // Invalid A
        byte[] slotB = headerB.encode();

        // When
        CommitHeader selected = CommitHeader.selectHeader(slotA, slotB);

        // Then
        assertEquals(20L, selected.getSeqNo());
    }

    @Test(expected = FxException.class)
    public void selectHeader_bothCorrupted_shouldThrow() {
        // Given
        byte[] slotA = new byte[CommitHeader.SIZE];
        byte[] slotB = new byte[CommitHeader.SIZE];

        // When & Then
        CommitHeader.selectHeader(slotA, slotB);
    }

    @Test
    public void selectHeader_aCrcCorrupted_shouldReturnB() {
        // Given
        CommitHeader headerA = new CommitHeader(30L, 0L, 4096L, 8192L, 12288L, 1L, 0L);
        CommitHeader headerB = new CommitHeader(20L, 0L, 4096L, 8192L, 12288L, 1L, 0L);

        byte[] slotA = headerA.encode();
        byte[] slotB = headerB.encode();

        // Corrupt A's CRC
        slotA[4092] ^= 0xFF;

        // When
        CommitHeader selected = CommitHeader.selectHeader(slotA, slotB);

        // Then: B should be selected because A is corrupted
        assertEquals(20L, selected.getSeqNo());
    }

    @Test
    public void selectHeader_bCrcCorrupted_shouldReturnA() {
        // Given
        CommitHeader headerA = new CommitHeader(10L, 0L, 4096L, 8192L, 12288L, 1L, 0L);
        CommitHeader headerB = new CommitHeader(30L, 0L, 4096L, 8192L, 12288L, 1L, 0L);

        byte[] slotA = headerA.encode();
        byte[] slotB = headerB.encode();

        // Corrupt B's CRC
        slotB[4092] ^= 0xFF;

        // When
        CommitHeader selected = CommitHeader.selectHeader(slotA, slotB);

        // Then: A should be selected because B is corrupted
        assertEquals(10L, selected.getSeqNo());
    }

    // ==================== toString 테스트 ====================

    @Test
    public void toString_shouldIncludeSeqNo() {
        // Given
        CommitHeader header = new CommitHeader(42L, 0L, 4096L, 8192L, 12288L, 10L, 1234567890L);

        // When
        String str = header.toString();

        // Then
        assertTrue(str.contains("42"));
    }

    @Test
    public void toString_shouldIncludeAllocTail() {
        // Given
        CommitHeader header = new CommitHeader(1L, 0L, 8888L, 8192L, 12288L, 10L, 0L);

        // When
        String str = header.toString();

        // Then
        assertTrue(str.contains("8888"));
    }

    @Test
    public void toString_shouldStartWithCommitHeader() {
        // Given
        CommitHeader header = createSampleHeader();

        // When
        String str = header.toString();

        // Then
        assertTrue(str.startsWith("CommitHeader{"));
    }

    // ==================== 접근자 테스트 ====================

    @Test
    public void getSeqNo_shouldReturnValue() {
        // Given
        CommitHeader header = new CommitHeader(999L, 0L, 0L, 0L, 0L, 0L, 0L);

        // When & Then
        assertEquals(999L, header.getSeqNo());
    }

    @Test
    public void getCommittedFlags_shouldReturnValue() {
        // Given
        CommitHeader header = new CommitHeader(0L, 7L, 0L, 0L, 0L, 0L, 0L);

        // When & Then
        assertEquals(7L, header.getCommittedFlags());
    }

    @Test
    public void getAllocTail_shouldReturnValue() {
        // Given
        CommitHeader header = new CommitHeader(0L, 0L, 16384L, 0L, 0L, 0L, 0L);

        // When & Then
        assertEquals(16384L, header.getAllocTail());
    }

    @Test
    public void getCatalogRootPageId_shouldReturnValue() {
        // Given
        CommitHeader header = new CommitHeader(0L, 0L, 0L, 4096L, 0L, 0L, 0L);

        // When & Then
        assertEquals(4096L, header.getCatalogRootPageId());
    }

    @Test
    public void getStateRootPageId_shouldReturnValue() {
        // Given
        CommitHeader header = new CommitHeader(0L, 0L, 0L, 0L, 8192L, 0L, 0L);

        // When & Then
        assertEquals(8192L, header.getStateRootPageId());
    }

    @Test
    public void getNextCollectionId_shouldReturnValue() {
        // Given
        CommitHeader header = new CommitHeader(0L, 0L, 0L, 0L, 0L, 100L, 0L);

        // When & Then
        assertEquals(100L, header.getNextCollectionId());
    }

    @Test
    public void getCommitEpochMs_shouldReturnValue() {
        // Given
        long now = System.currentTimeMillis();
        CommitHeader header = new CommitHeader(0L, 0L, 0L, 0L, 0L, 0L, now);

        // When & Then
        assertEquals(now, header.getCommitEpochMs());
    }

    // ==================== 헬퍼 메서드 ====================

    private CommitHeader createSampleHeader() {
        return new CommitHeader(1L, 1L, 4096L, 8192L, 12288L, 1L, System.currentTimeMillis());
    }
}
