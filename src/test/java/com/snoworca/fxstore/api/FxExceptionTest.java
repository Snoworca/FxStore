package com.snoworca.fxstore.api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * FxException 테스트
 * P1 클래스 커버리지 개선
 */
public class FxExceptionTest {

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_messageAndCode_shouldSetBoth() {
        // Given & When
        FxException ex = new FxException("test error", FxErrorCode.IO);

        // Then
        assertEquals("test error", ex.getMessage());
        assertEquals(FxErrorCode.IO, ex.code());
        assertEquals(FxErrorCode.IO, ex.getCode());
        assertNull(ex.getCause());
    }

    @Test
    public void constructor_messageAndCauseAndCode_shouldSetAll() {
        // Given
        RuntimeException cause = new RuntimeException("root cause");

        // When
        FxException ex = new FxException("wrapped error", cause, FxErrorCode.CORRUPTION);

        // Then
        assertEquals("wrapped error", ex.getMessage());
        assertSame(cause, ex.getCause());
        assertEquals(FxErrorCode.CORRUPTION, ex.code());
    }

    @Test
    public void constructor_codeAndMessage_alternativeOrder_shouldWork() {
        // Given & When
        FxException ex = new FxException(FxErrorCode.NOT_FOUND, "not found error");

        // Then
        assertEquals("not found error", ex.getMessage());
        assertEquals(FxErrorCode.NOT_FOUND, ex.code());
    }

    @Test
    public void constructor_codeAndMessageAndCause_alternativeOrder_shouldWork() {
        // Given
        RuntimeException cause = new RuntimeException("original");

        // When
        FxException ex = new FxException(FxErrorCode.CLOSED, "closed error", cause);

        // Then
        assertEquals("closed error", ex.getMessage());
        assertEquals(FxErrorCode.CLOSED, ex.code());
        assertSame(cause, ex.getCause());
    }

    // ==================== 팩토리 메서드 테스트 ====================

    @Test
    public void illegalArgument_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.illegalArgument("bad argument");

        // Then
        assertEquals("bad argument", ex.getMessage());
        assertEquals(FxErrorCode.ILLEGAL_ARGUMENT, ex.code());
    }

    @Test
    public void unsupported_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.unsupported("not supported");

        // Then
        assertEquals("not supported", ex.getMessage());
        assertEquals(FxErrorCode.UNSUPPORTED, ex.code());
    }

    @Test
    public void io_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.io("io error");

        // Then
        assertEquals("io error", ex.getMessage());
        assertEquals(FxErrorCode.IO, ex.code());
    }

    @Test
    public void io_withCause_shouldCreateCorrectException() {
        // Given
        Exception cause = new java.io.IOException("disk full");

        // When
        FxException ex = FxException.io("io error", cause);

        // Then
        assertEquals("io error", ex.getMessage());
        assertEquals(FxErrorCode.IO, ex.code());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void corruption_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.corruption("data corrupted");

        // Then
        assertEquals("data corrupted", ex.getMessage());
        assertEquals(FxErrorCode.CORRUPTION, ex.code());
    }

    @Test
    public void closed_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.closed("store is closed");

        // Then
        assertEquals("store is closed", ex.getMessage());
        assertEquals(FxErrorCode.CLOSED, ex.code());
    }

    @Test
    public void notFound_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.notFound("collection not found");

        // Then
        assertEquals("collection not found", ex.getMessage());
        assertEquals(FxErrorCode.NOT_FOUND, ex.code());
    }

    @Test
    public void alreadyExists_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.alreadyExists("collection already exists");

        // Then
        assertEquals("collection already exists", ex.getMessage());
        assertEquals(FxErrorCode.ALREADY_EXISTS, ex.code());
    }

    @Test
    public void typeMismatch_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.typeMismatch("type mismatch");

        // Then
        assertEquals("type mismatch", ex.getMessage());
        assertEquals(FxErrorCode.TYPE_MISMATCH, ex.code());
    }

    @Test
    public void versionMismatch_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.versionMismatch("version mismatch");

        // Then
        assertEquals("version mismatch", ex.getMessage());
        assertEquals(FxErrorCode.VERSION_MISMATCH, ex.code());
    }

    @Test
    public void codecNotFound_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.codecNotFound("codec not found");

        // Then
        assertEquals("codec not found", ex.getMessage());
        assertEquals(FxErrorCode.CODEC_NOT_FOUND, ex.code());
    }

    @Test
    public void upgradeFailed_shouldCreateCorrectException() {
        // Given
        RuntimeException cause = new RuntimeException("upgrade error");

        // When
        FxException ex = FxException.upgradeFailed("upgrade failed", cause);

        // Then
        assertEquals("upgrade failed", ex.getMessage());
        assertEquals(FxErrorCode.UPGRADE_FAILED, ex.code());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void outOfMemory_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.outOfMemory("out of memory");

        // Then
        assertEquals("out of memory", ex.getMessage());
        assertEquals(FxErrorCode.OUT_OF_MEMORY, ex.code());
    }

    @Test
    public void lockFailed_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.lockFailed("lock failed");

        // Then
        assertEquals("lock failed", ex.getMessage());
        assertEquals(FxErrorCode.LOCK_FAILED, ex.code());
    }

    @Test
    public void illegalState_shouldCreateCorrectException() {
        // Given & When
        FxException ex = FxException.illegalState("illegal state");

        // Then
        assertEquals("illegal state", ex.getMessage());
        assertEquals(FxErrorCode.ILLEGAL_STATE, ex.code());
    }

    // ==================== RuntimeException 상속 테스트 ====================

    @Test
    public void fxException_shouldBeRuntimeException() {
        // Given & When
        FxException ex = new FxException("test", FxErrorCode.IO);

        // Then
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void fxException_shouldBeThrowable() {
        // Given
        boolean caught = false;

        // When
        try {
            throw FxException.illegalArgument("test throw");
        } catch (FxException e) {
            caught = true;
            assertEquals("test throw", e.getMessage());
        }

        // Then
        assertTrue(caught);
    }

    @Test
    public void fxException_shouldHaveStackTrace() {
        // Given & When
        FxException ex = new FxException("test", FxErrorCode.IO);

        // Then
        assertNotNull(ex.getStackTrace());
        assertTrue(ex.getStackTrace().length > 0);
    }

    // ==================== FxErrorCode 커버리지 ====================

    @Test
    public void fxErrorCode_values_shouldReturnAllCodes() {
        // Given & When
        FxErrorCode[] codes = FxErrorCode.values();

        // Then
        assertTrue(codes.length >= 14);
    }

    @Test
    public void fxErrorCode_valueOf_shouldWork() {
        // Given & When & Then
        assertEquals(FxErrorCode.IO, FxErrorCode.valueOf("IO"));
        assertEquals(FxErrorCode.CORRUPTION, FxErrorCode.valueOf("CORRUPTION"));
        assertEquals(FxErrorCode.OUT_OF_MEMORY, FxErrorCode.valueOf("OUT_OF_MEMORY"));
        assertEquals(FxErrorCode.LOCK_FAILED, FxErrorCode.valueOf("LOCK_FAILED"));
        assertEquals(FxErrorCode.CLOSED, FxErrorCode.valueOf("CLOSED"));
        assertEquals(FxErrorCode.ILLEGAL_STATE, FxErrorCode.valueOf("ILLEGAL_STATE"));
        assertEquals(FxErrorCode.NOT_FOUND, FxErrorCode.valueOf("NOT_FOUND"));
        assertEquals(FxErrorCode.ALREADY_EXISTS, FxErrorCode.valueOf("ALREADY_EXISTS"));
        assertEquals(FxErrorCode.TYPE_MISMATCH, FxErrorCode.valueOf("TYPE_MISMATCH"));
        assertEquals(FxErrorCode.VERSION_MISMATCH, FxErrorCode.valueOf("VERSION_MISMATCH"));
        assertEquals(FxErrorCode.CODEC_NOT_FOUND, FxErrorCode.valueOf("CODEC_NOT_FOUND"));
        assertEquals(FxErrorCode.UPGRADE_FAILED, FxErrorCode.valueOf("UPGRADE_FAILED"));
        assertEquals(FxErrorCode.ILLEGAL_ARGUMENT, FxErrorCode.valueOf("ILLEGAL_ARGUMENT"));
        assertEquals(FxErrorCode.UNSUPPORTED, FxErrorCode.valueOf("UNSUPPORTED"));
        assertEquals(FxErrorCode.INTERNAL, FxErrorCode.valueOf("INTERNAL"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fxErrorCode_valueOf_invalidName_shouldThrow() {
        // Given & When & Then
        FxErrorCode.valueOf("INVALID_CODE");
    }
}
