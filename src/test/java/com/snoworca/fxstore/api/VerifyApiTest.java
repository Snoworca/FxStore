package com.snoworca.fxstore.api;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * VerifyErrorKind, VerifyError, VerifyResult 테스트
 * P0 클래스 커버리지 개선
 */
public class VerifyApiTest {

    // ==================== VerifyErrorKind 테스트 ====================

    @Test
    public void verifyErrorKind_values_shouldReturnAllKinds() {
        // Given & When
        VerifyErrorKind[] kinds = VerifyErrorKind.values();

        // Then
        assertTrue("At least one error kind should exist", kinds.length > 0);
        assertEquals(6, kinds.length); // SUPERBLOCK, HEADER, PAGE, RECORD, BTREE, OST
    }

    @Test
    public void verifyErrorKind_valueOf_shouldReturnCorrectEnum() {
        // Given & When & Then
        assertEquals(VerifyErrorKind.SUPERBLOCK, VerifyErrorKind.valueOf("SUPERBLOCK"));
        assertEquals(VerifyErrorKind.HEADER, VerifyErrorKind.valueOf("HEADER"));
        assertEquals(VerifyErrorKind.PAGE, VerifyErrorKind.valueOf("PAGE"));
        assertEquals(VerifyErrorKind.RECORD, VerifyErrorKind.valueOf("RECORD"));
        assertEquals(VerifyErrorKind.BTREE, VerifyErrorKind.valueOf("BTREE"));
        assertEquals(VerifyErrorKind.OST, VerifyErrorKind.valueOf("OST"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyErrorKind_valueOf_invalidName_shouldThrow() {
        // Given & When & Then
        VerifyErrorKind.valueOf("INVALID_KIND");
    }

    @Test
    public void verifyErrorKind_allKinds_shouldHaveDistinctOrdinals() {
        // Given
        VerifyErrorKind[] kinds = VerifyErrorKind.values();

        // When & Then
        for (int i = 0; i < kinds.length; i++) {
            assertEquals(i, kinds[i].ordinal());
        }
    }

    @Test
    public void verifyErrorKind_name_shouldMatchEnumName() {
        // Given & When & Then
        for (VerifyErrorKind kind : VerifyErrorKind.values()) {
            assertEquals(kind, VerifyErrorKind.valueOf(kind.name()));
        }
    }

    // ==================== VerifyError 테스트 ====================

    @Test
    public void verifyError_constructor_shouldSetAllFields() {
        // Given
        VerifyErrorKind kind = VerifyErrorKind.PAGE;
        long fileOffset = 1024L;
        long objectId = 42L;
        String message = "Page checksum mismatch";

        // When
        VerifyError error = new VerifyError(kind, fileOffset, objectId, message);

        // Then
        assertEquals(kind, error.kind());
        assertEquals(fileOffset, error.fileOffset());
        assertEquals(objectId, error.objectId());
        assertEquals(message, error.message());
    }

    @Test
    public void verifyError_toString_shouldIncludeAllFields() {
        // Given
        VerifyError error = new VerifyError(
                VerifyErrorKind.BTREE,
                2048L,
                100L,
                "Invalid node structure"
        );

        // When
        String str = error.toString();

        // Then
        assertTrue(str.contains("BTREE"));
        assertTrue(str.contains("2048"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("Invalid node structure"));
    }

    @Test
    public void verifyError_withNegativeOffset_shouldWork() {
        // Given: -1 indicates unknown offset
        VerifyError error = new VerifyError(
                VerifyErrorKind.SUPERBLOCK,
                -1L,
                0L,
                "Unknown location"
        );

        // When & Then
        assertEquals(-1L, error.fileOffset());
    }

    @Test
    public void verifyError_withZeroObjectId_shouldWork() {
        // Given: 0 indicates not applicable
        VerifyError error = new VerifyError(
                VerifyErrorKind.HEADER,
                4096L,
                0L,
                "Header corruption"
        );

        // When & Then
        assertEquals(0L, error.objectId());
    }

    @Test(expected = NullPointerException.class)
    public void verifyError_nullKind_shouldThrow() {
        // Given & When & Then
        new VerifyError(null, 0L, 0L, "message");
    }

    @Test(expected = NullPointerException.class)
    public void verifyError_nullMessage_shouldThrow() {
        // Given & When & Then
        new VerifyError(VerifyErrorKind.PAGE, 0L, 0L, null);
    }

    @Test
    public void verifyError_allKinds_shouldCreateSuccessfully() {
        // Given & When & Then
        for (VerifyErrorKind kind : VerifyErrorKind.values()) {
            VerifyError error = new VerifyError(kind, 0L, 0L, "Test error for " + kind);
            assertEquals(kind, error.kind());
        }
    }

    @Test
    public void verifyError_largeOffsets_shouldWork() {
        // Given: large file offsets
        VerifyError error = new VerifyError(
                VerifyErrorKind.RECORD,
                Long.MAX_VALUE,
                Long.MAX_VALUE,
                "Large offset test"
        );

        // When & Then
        assertEquals(Long.MAX_VALUE, error.fileOffset());
        assertEquals(Long.MAX_VALUE, error.objectId());
    }

    // ==================== VerifyResult 테스트 ====================

    @Test
    public void verifyResult_ok_shouldCreateValidResult() {
        // Given & When
        VerifyResult result = new VerifyResult(true, Collections.emptyList());

        // Then
        assertTrue(result.ok());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    public void verifyResult_withErrors_shouldCreateInvalidResult() {
        // Given
        List<VerifyError> errors = Arrays.asList(
                new VerifyError(VerifyErrorKind.PAGE, 1024L, 1L, "error1"),
                new VerifyError(VerifyErrorKind.BTREE, 2048L, 2L, "error2")
        );

        // When
        VerifyResult result = new VerifyResult(false, errors);

        // Then
        assertFalse(result.ok());
        assertEquals(2, result.errors().size());
    }

    @Test
    public void verifyResult_singleError_shouldWork() {
        // Given
        VerifyError error = new VerifyError(VerifyErrorKind.OST, 512L, 5L, "OST error");
        List<VerifyError> errors = Collections.singletonList(error);

        // When
        VerifyResult result = new VerifyResult(false, errors);

        // Then
        assertFalse(result.ok());
        assertEquals(1, result.errors().size());
        assertEquals(error.message(), result.errors().get(0).message());
    }

    @Test
    public void verifyResult_nullErrors_shouldReturnEmptyList() {
        // Given & When
        VerifyResult result = new VerifyResult(true, null);

        // Then
        assertTrue(result.ok());
        assertNotNull(result.errors());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    public void verifyResult_errors_shouldBeImmutable() {
        // Given
        List<VerifyError> errors = Arrays.asList(
                new VerifyError(VerifyErrorKind.PAGE, 0L, 0L, "error")
        );
        VerifyResult result = new VerifyResult(false, errors);

        // When & Then
        try {
            result.errors().add(new VerifyError(VerifyErrorKind.BTREE, 1L, 1L, "new"));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void verifyResult_toString_shouldIncludeStatus() {
        // Given
        VerifyResult okResult = new VerifyResult(true, Collections.emptyList());
        VerifyResult failResult = new VerifyResult(false, Arrays.asList(
                new VerifyError(VerifyErrorKind.HEADER, 0L, 0L, "fail")
        ));

        // When
        String okStr = okResult.toString();
        String failStr = failResult.toString();

        // Then
        assertTrue(okStr.contains("ok=true"));
        assertTrue(failStr.contains("ok=false"));
    }

    @Test
    public void verifyResult_manyErrors_shouldPreserveAll() {
        // Given
        List<VerifyError> errors = Arrays.asList(
                new VerifyError(VerifyErrorKind.SUPERBLOCK, 0L, 0L, "e1"),
                new VerifyError(VerifyErrorKind.HEADER, 4096L, 0L, "e2"),
                new VerifyError(VerifyErrorKind.PAGE, 8192L, 1L, "e3"),
                new VerifyError(VerifyErrorKind.RECORD, 12288L, 2L, "e4"),
                new VerifyError(VerifyErrorKind.BTREE, 16384L, 3L, "e5")
        );

        // When
        VerifyResult result = new VerifyResult(false, errors);

        // Then
        assertEquals(5, result.errors().size());
        for (int i = 0; i < errors.size(); i++) {
            assertEquals(errors.get(i).message(), result.errors().get(i).message());
        }
    }

    @Test
    public void verifyResult_okWithEmptyList_shouldBeValid() {
        // Given & When
        VerifyResult result = new VerifyResult(true, Collections.emptyList());

        // Then
        assertTrue(result.ok());
        assertEquals(0, result.errors().size());
    }
}
