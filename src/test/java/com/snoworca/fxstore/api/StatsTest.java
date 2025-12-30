package com.snoworca.fxstore.api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Stats 테스트
 * P1 클래스 커버리지 개선
 */
public class StatsTest {

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_shouldSetAllFields() {
        // Given & When
        Stats stats = new Stats(1024L, 800L, 224L, 0.22, 5);

        // Then
        assertEquals(1024L, stats.fileBytes());
        assertEquals(800L, stats.liveBytesEstimate());
        assertEquals(224L, stats.deadBytesEstimate());
        assertEquals(0.22, stats.deadRatio(), 0.001);
        assertEquals(5, stats.collectionCount());
    }

    @Test
    public void constructor_zeroValues_shouldWork() {
        // Given & When
        Stats stats = new Stats(0L, 0L, 0L, 0.0, 0);

        // Then
        assertEquals(0L, stats.fileBytes());
        assertEquals(0L, stats.liveBytesEstimate());
        assertEquals(0L, stats.deadBytesEstimate());
        assertEquals(0.0, stats.deadRatio(), 0.001);
        assertEquals(0, stats.collectionCount());
    }

    @Test
    public void constructor_largeValues_shouldWork() {
        // Given & When
        Stats stats = new Stats(Long.MAX_VALUE, Long.MAX_VALUE / 2, Long.MAX_VALUE / 2, 0.5, Integer.MAX_VALUE);

        // Then
        assertEquals(Long.MAX_VALUE, stats.fileBytes());
        assertEquals(Long.MAX_VALUE / 2, stats.liveBytesEstimate());
        assertEquals(Long.MAX_VALUE / 2, stats.deadBytesEstimate());
        assertEquals(0.5, stats.deadRatio(), 0.001);
        assertEquals(Integer.MAX_VALUE, stats.collectionCount());
    }

    // ==================== 접근자 테스트 ====================

    @Test
    public void fileBytes_shouldReturnCorrectValue() {
        // Given
        Stats stats = new Stats(4096L, 2048L, 2048L, 0.5, 10);

        // When & Then
        assertEquals(4096L, stats.fileBytes());
    }

    @Test
    public void liveBytesEstimate_shouldReturnCorrectValue() {
        // Given
        Stats stats = new Stats(10000L, 7500L, 2500L, 0.25, 3);

        // When & Then
        assertEquals(7500L, stats.liveBytesEstimate());
    }

    @Test
    public void deadBytesEstimate_shouldReturnCorrectValue() {
        // Given
        Stats stats = new Stats(10000L, 7500L, 2500L, 0.25, 3);

        // When & Then
        assertEquals(2500L, stats.deadBytesEstimate());
    }

    @Test
    public void deadRatio_shouldReturnCorrectValue() {
        // Given
        Stats stats = new Stats(1000L, 600L, 400L, 0.4, 2);

        // When & Then
        assertEquals(0.4, stats.deadRatio(), 0.001);
    }

    @Test
    public void deadRatio_zeroRatio_shouldWork() {
        // Given: No dead bytes
        Stats stats = new Stats(1000L, 1000L, 0L, 0.0, 1);

        // When & Then
        assertEquals(0.0, stats.deadRatio(), 0.001);
    }

    @Test
    public void deadRatio_oneRatio_shouldWork() {
        // Given: All dead bytes
        Stats stats = new Stats(1000L, 0L, 1000L, 1.0, 0);

        // When & Then
        assertEquals(1.0, stats.deadRatio(), 0.001);
    }

    @Test
    public void collectionCount_shouldReturnCorrectValue() {
        // Given
        Stats stats = new Stats(8192L, 4096L, 4096L, 0.5, 15);

        // When & Then
        assertEquals(15, stats.collectionCount());
    }

    // ==================== toString 테스트 ====================

    @Test
    public void toString_shouldIncludeFileBytes() {
        // Given
        Stats stats = new Stats(2048L, 1024L, 1024L, 0.5, 3);

        // When
        String str = stats.toString();

        // Then
        assertTrue(str.contains("fileBytes=2048"));
    }

    @Test
    public void toString_shouldIncludeLiveBytesEstimate() {
        // Given
        Stats stats = new Stats(2048L, 1024L, 1024L, 0.5, 3);

        // When
        String str = stats.toString();

        // Then
        assertTrue(str.contains("liveBytesEstimate=1024"));
    }

    @Test
    public void toString_shouldIncludeDeadBytesEstimate() {
        // Given
        Stats stats = new Stats(2048L, 1024L, 1024L, 0.5, 3);

        // When
        String str = stats.toString();

        // Then
        assertTrue(str.contains("deadBytesEstimate=1024"));
    }

    @Test
    public void toString_shouldIncludeDeadRatio() {
        // Given
        Stats stats = new Stats(2048L, 1024L, 1024L, 0.5, 3);

        // When
        String str = stats.toString();

        // Then
        assertTrue(str.contains("deadRatio=0.5"));
    }

    @Test
    public void toString_shouldIncludeCollectionCount() {
        // Given
        Stats stats = new Stats(2048L, 1024L, 1024L, 0.5, 3);

        // When
        String str = stats.toString();

        // Then
        assertTrue(str.contains("collectionCount=3"));
    }

    @Test
    public void toString_shouldContainStats() {
        // Given
        Stats stats = new Stats(1000L, 500L, 500L, 0.5, 2);

        // When
        String str = stats.toString();

        // Then
        assertTrue(str.startsWith("Stats{"));
        assertTrue(str.endsWith("}"));
    }

    // ==================== 경계값 테스트 ====================

    @Test
    public void negativeFileBytes_shouldBeAllowed() {
        // Given: API doesn't prevent negative values
        Stats stats = new Stats(-1L, 0L, 0L, 0.0, 0);

        // When & Then
        assertEquals(-1L, stats.fileBytes());
    }

    @Test
    public void negativeCollectionCount_shouldBeAllowed() {
        // Given: API doesn't prevent negative values
        Stats stats = new Stats(0L, 0L, 0L, 0.0, -1);

        // When & Then
        assertEquals(-1, stats.collectionCount());
    }

    // ==================== 불변성 테스트 ====================

    @Test
    public void stats_shouldBeImmutable() {
        // Given
        Stats stats = new Stats(1000L, 500L, 500L, 0.5, 2);

        // When: 여러 번 접근
        long bytes1 = stats.fileBytes();
        long bytes2 = stats.fileBytes();

        // Then: 값이 변하지 않음
        assertEquals(bytes1, bytes2);
        assertEquals(1000L, stats.fileBytes());
    }

    // ==================== 실제 시나리오 테스트 ====================

    @Test
    public void stats_emptyStore_shouldHaveNoDeadBytes() {
        // Given: Empty store
        Stats stats = new Stats(4096L, 0L, 0L, 0.0, 0);

        // When & Then
        assertEquals(4096L, stats.fileBytes()); // Initial file size
        assertEquals(0L, stats.liveBytesEstimate());
        assertEquals(0L, stats.deadBytesEstimate());
        assertEquals(0.0, stats.deadRatio(), 0.001);
    }

    @Test
    public void stats_afterCompaction_shouldHaveReducedDeadRatio() {
        // Given: Store before compaction
        Stats before = new Stats(10000L, 5000L, 5000L, 0.5, 10);

        // When: After compaction (simulated)
        Stats after = new Stats(5500L, 5000L, 500L, 0.09, 10);

        // Then
        assertTrue(after.deadRatio() < before.deadRatio());
        assertTrue(after.fileBytes() < before.fileBytes());
    }
}
