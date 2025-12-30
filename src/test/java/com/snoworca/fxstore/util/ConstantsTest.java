package com.snoworca.fxstore.util;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Constants 클래스 테스트
 * P0 클래스 커버리지 개선
 */
public class ConstantsTest {

    // ==================== Magic Numbers 테스트 ====================

    @Test
    public void superblockMagic_shouldBeCorrectValue() {
        // Given & When & Then
        byte[] magic = Constants.SUPERBLOCK_MAGIC;
        assertNotNull(magic);
        assertEquals(8, magic.length);
        assertEquals('F', magic[0]);
        assertEquals('X', magic[1]);
        assertEquals('S', magic[2]);
        assertEquals('T', magic[3]);
        assertEquals('O', magic[4]);
        assertEquals('R', magic[5]);
        assertEquals('E', magic[6]);
        assertEquals('\0', magic[7]);
    }

    @Test
    public void headerMagic_shouldBeCorrectValue() {
        // Given & When & Then
        byte[] magic = Constants.HEADER_MAGIC;
        assertNotNull(magic);
        assertEquals(8, magic.length);
        assertEquals('F', magic[0]);
        assertEquals('X', magic[1]);
        assertEquals('H', magic[2]);
        assertEquals('D', magic[3]);
        assertEquals('R', magic[4]);
    }

    @Test
    public void pageMagic_shouldBeCorrectValue() {
        // Given & When & Then
        byte[] magic = Constants.PAGE_MAGIC;
        assertNotNull(magic);
        assertEquals(4, magic.length);
        assertEquals('F', magic[0]);
        assertEquals('X', magic[1]);
        assertEquals('P', magic[2]);
        assertEquals('G', magic[3]);
    }

    @Test
    public void recordMagic_shouldBeCorrectValue() {
        // Given & When & Then
        byte[] magic = Constants.RECORD_MAGIC;
        assertNotNull(magic);
        assertEquals(4, magic.length);
        assertEquals('F', magic[0]);
        assertEquals('X', magic[1]);
        assertEquals('R', magic[2]);
        assertEquals('C', magic[3]);
    }

    // ==================== File Layout 테스트 ====================

    @Test
    public void superblockOffset_shouldBeZero() {
        // Given & When & Then
        assertEquals(0L, Constants.SUPERBLOCK_OFFSET);
    }

    @Test
    public void superblockSize_shouldBe4096() {
        // Given & When & Then
        assertEquals(4096, Constants.SUPERBLOCK_SIZE);
    }

    @Test
    public void headerOffsets_shouldBeConsecutive() {
        // Given & When & Then
        assertEquals(4096L, Constants.HEADER_A_OFFSET);
        assertEquals(8192L, Constants.HEADER_B_OFFSET);
        assertEquals(4096, Constants.HEADER_B_OFFSET - Constants.HEADER_A_OFFSET);
    }

    @Test
    public void headerSize_shouldBe4096() {
        // Given & When & Then
        assertEquals(4096, Constants.HEADER_SIZE);
    }

    @Test
    public void allocStart_shouldBeAfterHeaders() {
        // Given & When & Then
        assertEquals(12288L, Constants.ALLOC_START);
        assertTrue(Constants.ALLOC_START > Constants.HEADER_B_OFFSET);
    }

    // ==================== Format Versions 테스트 ====================

    @Test
    public void formatVersion_shouldBePositive() {
        // Given & When & Then
        assertTrue(Constants.FORMAT_VERSION > 0);
    }

    @Test
    public void headerVersion_shouldBePositive() {
        // Given & When & Then
        assertTrue(Constants.HEADER_VERSION > 0);
    }

    // ==================== Page Types 테스트 ====================

    @Test
    public void pageTypes_shouldBeDistinct() {
        // Given & When & Then
        int[] types = {
                Constants.PAGE_TYPE_BTREE_INTERNAL,
                Constants.PAGE_TYPE_BTREE_LEAF,
                Constants.PAGE_TYPE_OST_INTERNAL,
                Constants.PAGE_TYPE_OST_LEAF
        };

        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals("Page types should be distinct: " + i + " vs " + j,
                        types[i], types[j]);
            }
        }
    }

    @Test
    public void pageTypes_shouldBePositive() {
        // Given & When & Then
        assertTrue(Constants.PAGE_TYPE_BTREE_INTERNAL > 0);
        assertTrue(Constants.PAGE_TYPE_BTREE_LEAF > 0);
        assertTrue(Constants.PAGE_TYPE_OST_INTERNAL > 0);
        assertTrue(Constants.PAGE_TYPE_OST_LEAF > 0);
    }

    // ==================== Record Types 테스트 ====================

    @Test
    public void recordTypes_shouldBeDistinct() {
        // Given & When & Then
        int[] types = {
                Constants.RECORD_TYPE_VALUE,
                Constants.RECORD_TYPE_OVERFLOW,
                Constants.RECORD_TYPE_CODEC_META
        };

        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals("Record types should be distinct: " + i + " vs " + j,
                        types[i], types[j]);
            }
        }
    }

    @Test
    public void recordTypes_shouldBePositive() {
        // Given & When & Then
        assertTrue(Constants.RECORD_TYPE_VALUE > 0);
        assertTrue(Constants.RECORD_TYPE_OVERFLOW > 0);
        assertTrue(Constants.RECORD_TYPE_CODEC_META > 0);
    }

    // ==================== Page Header 테스트 ====================

    @Test
    public void pageHeaderSize_shouldBePositive() {
        // Given & When & Then
        assertTrue(Constants.PAGE_HEADER_SIZE > 0);
        assertEquals(32, Constants.PAGE_HEADER_SIZE);
    }

    // ==================== Limits 테스트 ====================

    @Test
    public void maxNameLength_shouldBeReasonable() {
        // Given & When & Then
        assertTrue(Constants.MAX_NAME_LENGTH > 0);
        assertEquals(255, Constants.MAX_NAME_LENGTH);
    }

    @Test
    public void maxKvSize_shouldBe1MiB() {
        // Given & When & Then
        assertEquals(1024 * 1024, Constants.MAX_KV_SIZE);
    }

    // ==================== Default Options 테스트 ====================

    @Test
    public void defaultPageSize_shouldBe4096() {
        // Given & When & Then
        assertEquals(4096, Constants.DEFAULT_PAGE_SIZE);
    }

    @Test
    public void defaultPageSize_shouldBePowerOfTwo() {
        // Given
        int size = Constants.DEFAULT_PAGE_SIZE;

        // When & Then
        assertTrue("Page size should be power of two", (size & (size - 1)) == 0);
    }

    @Test
    public void defaultCacheBytes_shouldBe64MiB() {
        // Given & When & Then
        assertEquals(64 * 1024 * 1024L, Constants.DEFAULT_CACHE_BYTES);
    }

    @Test
    public void defaultMemoryLimit_shouldBeMaxValue() {
        // Given & When & Then
        assertEquals(Long.MAX_VALUE, Constants.DEFAULT_MEMORY_LIMIT);
    }

    // ==================== Consistency 테스트 ====================

    @Test
    public void layoutOffsets_shouldBeConsistent() {
        // Given & When & Then
        // Superblock at 0, size 4096 -> Header A at 4096
        assertEquals(Constants.SUPERBLOCK_OFFSET + Constants.SUPERBLOCK_SIZE,
                Constants.HEADER_A_OFFSET);

        // Header A at 4096, size 4096 -> Header B at 8192
        assertEquals(Constants.HEADER_A_OFFSET + Constants.HEADER_SIZE,
                Constants.HEADER_B_OFFSET);

        // Header B at 8192, size 4096 -> Alloc start at 12288
        assertEquals(Constants.HEADER_B_OFFSET + Constants.HEADER_SIZE,
                Constants.ALLOC_START);
    }

    @Test
    public void allConstants_shouldBeAccessible() throws Exception {
        // Given
        Class<?> clazz = Constants.class;

        // When
        int count = 0;
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isFinal(field.getModifiers())) {
                Object value = field.get(null);
                assertNotNull("Field " + field.getName() + " should not be null", value);
                count++;
            }
        }

        // Then
        assertTrue("Should have at least 15 constants", count >= 15);
    }

    @Test
    public void constantsClass_shouldNotBeInstantiable() {
        // Given
        Class<?> clazz = Constants.class;

        // When & Then
        try {
            java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
            assertTrue("Constructor should be private",
                    Modifier.isPrivate(constructor.getModifiers()));
        } catch (NoSuchMethodException e) {
            fail("Should have a private constructor");
        }
    }
}
