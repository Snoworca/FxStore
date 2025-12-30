package com.snoworca.fxstore.api;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * ChainedCodecUpgradeHook 및 Builder 테스트
 * P0 클래스 커버리지 개선
 */
public class ChainedCodecUpgradeHookTest {

    // ==================== Builder 테스트 ====================

    @Test
    public void builder_shouldCreateInstance() {
        // Given & When
        ChainedCodecUpgradeHook.Builder builder = ChainedCodecUpgradeHook.builder();

        // Then
        assertNotNull(builder);
    }

    @Test
    public void builder_add_shouldChainHooks() {
        // Given
        FxCodecUpgradeHook hook1 = (codecId, fromVersion, toVersion, oldBytes) -> oldBytes;
        FxCodecUpgradeHook hook2 = (codecId, fromVersion, toVersion, oldBytes) -> oldBytes;

        // When
        ChainedCodecUpgradeHook result = ChainedCodecUpgradeHook.builder()
                .add(hook1)
                .add(hook2)
                .build();

        // Then
        assertNotNull(result);
    }

    @Test
    public void builder_build_empty_shouldCreateNoOpHook() {
        // Given
        ChainedCodecUpgradeHook.Builder builder = ChainedCodecUpgradeHook.builder();

        // When
        ChainedCodecUpgradeHook result = builder.build();

        // Then
        assertNotNull(result);
        // No-op hook should pass through data unchanged
        byte[] data = {1, 2, 3};
        byte[] upgraded = result.upgrade("test", 1, 2, data);
        assertArrayEquals(data, upgraded);
    }

    @Test
    public void builder_add_shouldReturnBuilderForChaining() {
        // Given
        ChainedCodecUpgradeHook.Builder builder = ChainedCodecUpgradeHook.builder();

        // When
        ChainedCodecUpgradeHook.Builder result = builder.add((codecId, fromVersion, toVersion, oldBytes) -> oldBytes);

        // Then
        assertSame(builder, result);
    }

    @Test(expected = NullPointerException.class)
    public void builder_add_nullHook_shouldThrow() {
        // Given & When & Then
        ChainedCodecUpgradeHook.builder().add(null);
    }

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_withList_shouldAcceptHooks() {
        // Given
        FxCodecUpgradeHook hook = (codecId, fromVersion, toVersion, oldBytes) -> oldBytes;

        // When
        ChainedCodecUpgradeHook chained = new ChainedCodecUpgradeHook(Collections.singletonList(hook));

        // Then
        assertNotNull(chained);
    }

    @Test
    public void constructor_default_shouldCreateEmptyChain() {
        // Given & When
        ChainedCodecUpgradeHook chained = new ChainedCodecUpgradeHook();

        // Then
        assertNotNull(chained);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullList_shouldThrow() {
        // Given & When & Then
        new ChainedCodecUpgradeHook(null);
    }

    // ==================== upgrade 메서드 테스트 ====================

    @Test
    public void upgrade_singleHook_shouldApply() {
        // Given
        FxCodecUpgradeHook doubleBytes = (codecId, fromVersion, toVersion, oldBytes) -> {
            byte[] result = new byte[oldBytes.length * 2];
            System.arraycopy(oldBytes, 0, result, 0, oldBytes.length);
            System.arraycopy(oldBytes, 0, result, oldBytes.length, oldBytes.length);
            return result;
        };
        ChainedCodecUpgradeHook chained = ChainedCodecUpgradeHook.builder()
                .add(doubleBytes)
                .build();

        // When
        byte[] original = {1, 2, 3};
        byte[] upgraded = chained.upgrade("test", 1, 2, original);

        // Then
        assertEquals(6, upgraded.length);
    }

    @Test
    public void upgrade_multipleHooks_shouldApplyInOrder() {
        // Given
        AtomicInteger counter = new AtomicInteger(0);

        FxCodecUpgradeHook hook1 = (codecId, fromVersion, toVersion, oldBytes) -> {
            assertEquals(0, counter.getAndIncrement());
            return oldBytes;
        };
        FxCodecUpgradeHook hook2 = (codecId, fromVersion, toVersion, oldBytes) -> {
            assertEquals(1, counter.getAndIncrement());
            return oldBytes;
        };
        FxCodecUpgradeHook hook3 = (codecId, fromVersion, toVersion, oldBytes) -> {
            assertEquals(2, counter.getAndIncrement());
            return oldBytes;
        };

        ChainedCodecUpgradeHook chained = ChainedCodecUpgradeHook.builder()
                .add(hook1)
                .add(hook2)
                .add(hook3)
                .build();

        // When
        chained.upgrade("test", 1, 3, new byte[]{});

        // Then
        assertEquals(3, counter.get());
    }

    @Test
    public void upgrade_shouldPassCorrectParameters() {
        // Given
        final String expectedCodecId = "myCodec";
        final int expectedFromVersion = 5;
        final int expectedToVersion = 10;
        final byte[] expectedBytes = {10, 20, 30};

        FxCodecUpgradeHook verifyingHook = (codecId, fromVersion, toVersion, oldBytes) -> {
            assertEquals(expectedCodecId, codecId);
            assertEquals(expectedFromVersion, fromVersion);
            assertEquals(expectedToVersion, toVersion);
            assertArrayEquals(expectedBytes, oldBytes);
            return oldBytes;
        };

        ChainedCodecUpgradeHook chained = ChainedCodecUpgradeHook.builder()
                .add(verifyingHook)
                .build();

        // When & Then (asserts are in the hook)
        chained.upgrade(expectedCodecId, expectedFromVersion, expectedToVersion, expectedBytes);
    }

    // ==================== register 메서드 테스트 ====================

    @Test
    public void register_shouldAddVersionSpecificHook() {
        // Given
        ChainedCodecUpgradeHook chained = new ChainedCodecUpgradeHook();
        FxCodecUpgradeHook specificHook = (codecId, fromVersion, toVersion, oldBytes) -> {
            byte[] result = new byte[oldBytes.length + 1];
            System.arraycopy(oldBytes, 0, result, 0, oldBytes.length);
            result[oldBytes.length] = 99; // marker byte
            return result;
        };

        // When
        chained.register("myCodec", 1, 2, specificHook);

        // Then
        byte[] original = {1, 2, 3};
        byte[] upgraded = chained.upgrade("myCodec", 1, 2, original);
        assertEquals(4, upgraded.length);
        assertEquals(99, upgraded[3]);
    }

    @Test
    public void register_differentVersions_shouldBeIndependent() {
        // Given
        ChainedCodecUpgradeHook chained = new ChainedCodecUpgradeHook();
        chained.register("codec", 1, 2, (c, f, t, b) -> new byte[]{1});
        chained.register("codec", 2, 3, (c, f, t, b) -> new byte[]{2});

        // When
        byte[] v1to2 = chained.upgrade("codec", 1, 2, new byte[]{0});
        byte[] v2to3 = chained.upgrade("codec", 2, 3, new byte[]{0});

        // Then
        assertArrayEquals(new byte[]{1}, v1to2);
        assertArrayEquals(new byte[]{2}, v2to3);
    }

    @Test
    public void register_differentCodecs_shouldBeIndependent() {
        // Given
        ChainedCodecUpgradeHook chained = new ChainedCodecUpgradeHook();
        chained.register("codecA", 1, 2, (c, f, t, b) -> new byte[]{10});
        chained.register("codecB", 1, 2, (c, f, t, b) -> new byte[]{20});

        // When
        byte[] resultA = chained.upgrade("codecA", 1, 2, new byte[]{0});
        byte[] resultB = chained.upgrade("codecB", 1, 2, new byte[]{0});

        // Then
        assertArrayEquals(new byte[]{10}, resultA);
        assertArrayEquals(new byte[]{20}, resultB);
    }

    @Test
    public void upgrade_registeredAndChainedHooks_shouldBothApply() {
        // Given
        ChainedCodecUpgradeHook chained = new ChainedCodecUpgradeHook(Arrays.asList(
                (c, f, t, b) -> {
                    // Double the array
                    byte[] result = new byte[b.length * 2];
                    System.arraycopy(b, 0, result, 0, b.length);
                    System.arraycopy(b, 0, result, b.length, b.length);
                    return result;
                }
        ));
        chained.register("myCodec", 1, 2, (c, f, t, b) -> {
            // Add 1 to first byte
            byte[] result = b.clone();
            if (result.length > 0) result[0]++;
            return result;
        });

        // When
        byte[] original = {5};
        byte[] upgraded = chained.upgrade("myCodec", 1, 2, original);

        // Then
        // Registered hook runs first: {5} -> {6}
        // Then chained hook doubles: {6} -> {6, 6}
        assertEquals(2, upgraded.length);
        assertEquals(6, upgraded[0]);
        assertEquals(6, upgraded[1]);
    }

    @Test
    public void upgrade_unregisteredVersion_shouldPassThrough() {
        // Given
        ChainedCodecUpgradeHook chained = new ChainedCodecUpgradeHook();
        chained.register("codec", 1, 2, (c, f, t, b) -> new byte[]{99});

        // When: upgrade with different version
        byte[] original = {1, 2, 3};
        byte[] upgraded = chained.upgrade("codec", 3, 4, original);

        // Then: unchanged (no registered hook for 3->4)
        assertArrayEquals(original, upgraded);
    }

    @Test
    public void upgrade_unregisteredCodec_shouldPassThrough() {
        // Given
        ChainedCodecUpgradeHook chained = new ChainedCodecUpgradeHook();
        chained.register("codecA", 1, 2, (c, f, t, b) -> new byte[]{99});

        // When: upgrade different codec
        byte[] original = {1, 2, 3};
        byte[] upgraded = chained.upgrade("codecB", 1, 2, original);

        // Then: unchanged (no registered hook for codecB)
        assertArrayEquals(original, upgraded);
    }
}
