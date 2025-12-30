package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.api.FxCodecUpgradeHook;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * CodecUpgradeContext 테스트
 * P1 클래스 커버리지 개선
 */
public class CodecUpgradeContextTest {

    // 테스트용 Mock Codec
    private static class MockCodec implements FxCodec<String> {
        private final String id;
        private final int version;

        MockCodec(String id, int version) {
            this.id = id;
            this.version = version;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int version() {
            return version;
        }

        @Override
        public byte[] encode(String value) {
            return value.getBytes();
        }

        @Override
        public String decode(byte[] bytes) {
            return new String(bytes);
        }

        @Override
        public int compareBytes(byte[] a, byte[] b) {
            int minLen = Math.min(a.length, b.length);
            for (int i = 0; i < minLen; i++) {
                int cmp = Byte.compare(a[i], b[i]);
                if (cmp != 0) return cmp;
            }
            return Integer.compare(a.length, b.length);
        }

        @Override
        public boolean equalsBytes(byte[] a, byte[] b) {
            return java.util.Arrays.equals(a, b);
        }

        @Override
        public int hashBytes(byte[] bytes) {
            return java.util.Arrays.hashCode(bytes);
        }
    }

    // ==================== 생성자 테스트 (Codec 기반) ====================

    @Test
    public void constructor_withCodec_shouldSetAllFields() {
        // Given
        FxCodec<String> codec = new MockCodec("test:codec", 2);
        FxCodecUpgradeHook hook = (c, f, t, b) -> b;

        // When
        CodecUpgradeContext ctx = new CodecUpgradeContext(codec, 1, 2, hook, true);

        // Then
        assertSame(codec, ctx.getCodec());
        assertEquals("test:codec", ctx.getCodecId());
        assertEquals(1, ctx.getFromVersion());
        assertEquals(2, ctx.getToVersion());
        assertSame(hook, ctx.getUpgradeHook());
        assertTrue(ctx.isUpgradeNeeded());
    }

    @Test
    public void constructor_withCodec_noUpgradeNeeded_shouldWork() {
        // Given
        FxCodec<String> codec = new MockCodec("test:codec", 1);

        // When
        CodecUpgradeContext ctx = new CodecUpgradeContext(codec, 1, 1, null, false);

        // Then
        assertFalse(ctx.isUpgradeNeeded());
        assertNull(ctx.getUpgradeHook());
    }

    @Test
    public void constructor_withNullCodec_shouldWork() {
        // Given & When
        CodecUpgradeContext ctx = new CodecUpgradeContext((FxCodec<?>) null, 1, 2, null, true);

        // Then
        assertNull(ctx.getCodec());
        assertNull(ctx.getCodecId());
    }

    // ==================== 생성자 테스트 (String codecId 기반) ====================

    @Test
    public void constructor_withStringCodecId_shouldSetFields() {
        // Given
        FxCodecUpgradeHook hook = (c, f, t, b) -> b;

        // When
        CodecUpgradeContext ctx = new CodecUpgradeContext("my:codec", 1, 2, hook);

        // Then
        assertNull(ctx.getCodec());
        assertEquals("my:codec", ctx.getCodecId());
        assertEquals(1, ctx.getFromVersion());
        assertEquals(2, ctx.getToVersion());
        assertSame(hook, ctx.getUpgradeHook());
        assertTrue(ctx.isUpgradeNeeded()); // fromVersion != toVersion
    }

    @Test
    public void constructor_withStringCodecId_sameVersion_shouldNotNeedUpgrade() {
        // Given & When
        CodecUpgradeContext ctx = new CodecUpgradeContext("my:codec", 3, 3, null);

        // Then
        assertFalse(ctx.isUpgradeNeeded()); // fromVersion == toVersion
    }

    // ==================== noUpgrade 팩토리 메서드 테스트 ====================

    @Test
    public void noUpgrade_shouldCreateContextWithNoUpgrade() {
        // Given
        FxCodec<String> codec = new MockCodec("fx:string", 5);

        // When
        CodecUpgradeContext ctx = CodecUpgradeContext.noUpgrade(codec);

        // Then
        assertSame(codec, ctx.getCodec());
        assertEquals("fx:string", ctx.getCodecId());
        assertEquals(5, ctx.getFromVersion());
        assertEquals(5, ctx.getToVersion());
        assertNull(ctx.getUpgradeHook());
        assertFalse(ctx.isUpgradeNeeded());
    }

    @Test
    public void noUpgrade_versionsMatch_shouldEqual() {
        // Given
        FxCodec<String> codec = new MockCodec("test", 10);

        // When
        CodecUpgradeContext ctx = CodecUpgradeContext.noUpgrade(codec);

        // Then
        assertEquals(ctx.getFromVersion(), ctx.getToVersion());
    }

    // ==================== needsUpgrade 팩토리 메서드 테스트 ====================

    @Test
    public void needsUpgrade_shouldCreateContextWithUpgrade() {
        // Given
        FxCodec<String> codec = new MockCodec("fx:bytes", 3);
        FxCodecUpgradeHook hook = (c, f, t, b) -> b;

        // When
        CodecUpgradeContext ctx = CodecUpgradeContext.needsUpgrade(codec, 1, hook);

        // Then
        assertSame(codec, ctx.getCodec());
        assertEquals("fx:bytes", ctx.getCodecId());
        assertEquals(1, ctx.getFromVersion());
        assertEquals(3, ctx.getToVersion()); // codec.version()
        assertSame(hook, ctx.getUpgradeHook());
        assertTrue(ctx.isUpgradeNeeded());
    }

    @Test
    public void needsUpgrade_fromVersionLessThanToVersion() {
        // Given
        FxCodec<String> codec = new MockCodec("test", 5);

        // When
        CodecUpgradeContext ctx = CodecUpgradeContext.needsUpgrade(codec, 2, null);

        // Then
        assertTrue(ctx.getFromVersion() < ctx.getToVersion());
    }

    // ==================== upgradeIfNeeded 테스트 ====================

    @Test
    public void upgradeIfNeeded_noUpgrade_shouldReturnSameBytes() {
        // Given
        FxCodec<String> codec = new MockCodec("test", 1);
        CodecUpgradeContext ctx = CodecUpgradeContext.noUpgrade(codec);
        byte[] original = {1, 2, 3, 4};

        // When
        byte[] result = ctx.upgradeIfNeeded(original);

        // Then
        assertSame(original, result);
    }

    @Test
    public void upgradeIfNeeded_upgradeNeededWithNullHook_shouldReturnSameBytes() {
        // Given
        FxCodec<String> codec = new MockCodec("test", 2);
        CodecUpgradeContext ctx = new CodecUpgradeContext(codec, 1, 2, null, true);
        byte[] original = {1, 2, 3, 4};

        // When
        byte[] result = ctx.upgradeIfNeeded(original);

        // Then
        assertSame(original, result);
    }

    @Test
    public void upgradeIfNeeded_withHook_shouldCallHook() {
        // Given
        FxCodec<String> codec = new MockCodec("my:codec", 2);
        FxCodecUpgradeHook hook = (codecId, fromVersion, toVersion, oldBytes) -> {
            byte[] result = new byte[oldBytes.length + 1];
            System.arraycopy(oldBytes, 0, result, 0, oldBytes.length);
            result[oldBytes.length] = 99; // marker
            return result;
        };
        CodecUpgradeContext ctx = CodecUpgradeContext.needsUpgrade(codec, 1, hook);
        byte[] original = {1, 2, 3};

        // When
        byte[] result = ctx.upgradeIfNeeded(original);

        // Then
        assertEquals(4, result.length);
        assertEquals(99, result[3]);
    }

    @Test
    public void upgradeIfNeeded_hookReceivesCorrectParameters() {
        // Given
        final String expectedCodecId = "my:codec";
        final int expectedFromVersion = 1;
        final int expectedToVersion = 2;
        final byte[] expectedBytes = {10, 20, 30};

        FxCodec<String> codec = new MockCodec(expectedCodecId, expectedToVersion);
        FxCodecUpgradeHook hook = (codecId, fromVersion, toVersion, oldBytes) -> {
            assertEquals(expectedCodecId, codecId);
            assertEquals(expectedFromVersion, fromVersion);
            assertEquals(expectedToVersion, toVersion);
            assertArrayEquals(expectedBytes, oldBytes);
            return oldBytes;
        };
        CodecUpgradeContext ctx = CodecUpgradeContext.needsUpgrade(codec, expectedFromVersion, hook);

        // When & Then (assertions in hook)
        ctx.upgradeIfNeeded(expectedBytes);
    }

    // ==================== 접근자 테스트 ====================

    @Test
    public void getCodec_shouldReturnCodec() {
        // Given
        FxCodec<String> codec = new MockCodec("test", 1);
        CodecUpgradeContext ctx = CodecUpgradeContext.noUpgrade(codec);

        // When & Then
        assertSame(codec, ctx.getCodec());
    }

    @Test
    public void getCodecId_fromCodec_shouldReturnCodecId() {
        // Given
        FxCodec<String> codec = new MockCodec("fx:i64", 1);
        CodecUpgradeContext ctx = CodecUpgradeContext.noUpgrade(codec);

        // When & Then
        assertEquals("fx:i64", ctx.getCodecId());
    }

    @Test
    public void getCodecId_fromString_shouldReturnCodecId() {
        // Given
        CodecUpgradeContext ctx = new CodecUpgradeContext("custom:uuid", 1, 2, null);

        // When & Then
        assertEquals("custom:uuid", ctx.getCodecId());
    }

    @Test
    public void getFromVersion_shouldReturnValue() {
        // Given
        CodecUpgradeContext ctx = new CodecUpgradeContext("test", 5, 10, null);

        // When & Then
        assertEquals(5, ctx.getFromVersion());
    }

    @Test
    public void getToVersion_shouldReturnValue() {
        // Given
        CodecUpgradeContext ctx = new CodecUpgradeContext("test", 5, 10, null);

        // When & Then
        assertEquals(10, ctx.getToVersion());
    }

    @Test
    public void getUpgradeHook_shouldReturnHook() {
        // Given
        FxCodecUpgradeHook hook = (c, f, t, b) -> b;
        CodecUpgradeContext ctx = new CodecUpgradeContext("test", 1, 2, hook);

        // When & Then
        assertSame(hook, ctx.getUpgradeHook());
    }

    @Test
    public void isUpgradeNeeded_shouldReturnValue() {
        // Given
        FxCodec<String> codec = new MockCodec("test", 3);
        CodecUpgradeContext upgradeCtx = CodecUpgradeContext.needsUpgrade(codec, 1, null);
        CodecUpgradeContext noUpgradeCtx = CodecUpgradeContext.noUpgrade(codec);

        // When & Then
        assertTrue(upgradeCtx.isUpgradeNeeded());
        assertFalse(noUpgradeCtx.isUpgradeNeeded());
    }

    // ==================== 버전 관계 테스트 ====================

    @Test
    public void versionUpgrade_multipleSteps_shouldWork() {
        // Given: Version 1 -> 5
        FxCodec<String> codec = new MockCodec("test", 5);
        FxCodecUpgradeHook hook = (c, f, t, b) -> {
            // Simulate multi-version upgrade
            byte[] result = new byte[b.length];
            for (int i = 0; i < b.length; i++) {
                result[i] = (byte) (b[i] + (t - f)); // Add version diff
            }
            return result;
        };
        CodecUpgradeContext ctx = CodecUpgradeContext.needsUpgrade(codec, 1, hook);

        // When
        byte[] original = {10, 20, 30};
        byte[] upgraded = ctx.upgradeIfNeeded(original);

        // Then: Each byte increased by 4 (5-1)
        assertEquals(14, upgraded[0]);
        assertEquals(24, upgraded[1]);
        assertEquals(34, upgraded[2]);
    }
}
