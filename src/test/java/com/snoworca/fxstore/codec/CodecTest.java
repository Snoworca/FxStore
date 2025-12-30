package com.snoworca.fxstore.codec;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Codec 시스템 통합 테스트.
 *
 * 모든 코덱의 encode/decode, compare, equals, hash 연산을 검증합니다.
 */
public class CodecTest {

    private static final Random random = new Random(42);

    // ==================== I64Codec 테스트 ====================

    @Test
    public void testI64Codec_BasicEncodeDecode() {
        I64Codec codec = I64Codec.INSTANCE;

        // 기본 값
        assertEquals(0L, (long) codec.decode(codec.encode(0L)));
        assertEquals(1L, (long) codec.decode(codec.encode(1L)));
        assertEquals(-1L, (long) codec.decode(codec.encode(-1L)));
        assertEquals(Long.MAX_VALUE, (long) codec.decode(codec.encode(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE, (long) codec.decode(codec.encode(Long.MIN_VALUE)));
    }

    @Test
    public void testI64Codec_CompareBytes() {
        I64Codec codec = I64Codec.INSTANCE;

        // 양수 비교
        byte[] b1 = codec.encode(100L);
        byte[] b2 = codec.encode(200L);
        assertTrue(codec.compareBytes(b1, b2) < 0);
        assertTrue(codec.compareBytes(b2, b1) > 0);
        assertEquals(0, codec.compareBytes(b1, codec.encode(100L)));

        // 음수 비교
        byte[] neg1 = codec.encode(-100L);
        byte[] neg2 = codec.encode(-200L);
        assertTrue(codec.compareBytes(neg2, neg1) < 0);
        assertTrue(codec.compareBytes(neg1, neg2) > 0);

        // 양수 vs 음수
        assertTrue(codec.compareBytes(neg1, b1) < 0);
        assertTrue(codec.compareBytes(b1, neg1) > 0);
    }

    @Test
    public void testI64Codec_EqualsBytes() {
        I64Codec codec = I64Codec.INSTANCE;

        byte[] b1 = codec.encode(12345L);
        byte[] b2 = codec.encode(12345L);
        byte[] b3 = codec.encode(12346L);

        assertTrue(codec.equalsBytes(b1, b2));
        assertFalse(codec.equalsBytes(b1, b3));
    }

    @Test
    public void testI64Codec_HashBytes() {
        I64Codec codec = I64Codec.INSTANCE;

        byte[] b1 = codec.encode(12345L);
        byte[] b2 = codec.encode(12345L);

        assertEquals(codec.hashBytes(b1), codec.hashBytes(b2));
    }

    @Test
    public void testI64Codec_Id() {
        I64Codec codec = I64Codec.INSTANCE;
        assertEquals("fx:i64", codec.id());
        assertEquals(1, codec.version());
    }

    @Test
    public void testI64Codec_RandomValues() {
        I64Codec codec = I64Codec.INSTANCE;

        for (int i = 0; i < 1000; i++) {
            long value = random.nextLong();
            byte[] encoded = codec.encode(value);
            long decoded = codec.decode(encoded);
            assertEquals(value, decoded);
        }
    }

    // ==================== F64Codec 테스트 ====================

    @Test
    public void testF64Codec_BasicEncodeDecode() {
        F64Codec codec = F64Codec.INSTANCE;

        assertEquals(0.0, codec.decode(codec.encode(0.0)), 0.0001);
        assertEquals(1.0, codec.decode(codec.encode(1.0)), 0.0001);
        assertEquals(-1.0, codec.decode(codec.encode(-1.0)), 0.0001);
        assertEquals(Double.MAX_VALUE, codec.decode(codec.encode(Double.MAX_VALUE)), 0.0001);
        assertEquals(Double.MIN_VALUE, codec.decode(codec.encode(Double.MIN_VALUE)), 0.0001);
    }

    @Test
    public void testF64Codec_SpecialValues() {
        F64Codec codec = F64Codec.INSTANCE;

        // NaN
        assertTrue(Double.isNaN(codec.decode(codec.encode(Double.NaN))));

        // Infinity
        assertEquals(Double.POSITIVE_INFINITY, codec.decode(codec.encode(Double.POSITIVE_INFINITY)), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, codec.decode(codec.encode(Double.NEGATIVE_INFINITY)), 0.0);
    }

    @Test
    public void testF64Codec_CompareBytes() {
        F64Codec codec = F64Codec.INSTANCE;

        byte[] b1 = codec.encode(1.5);
        byte[] b2 = codec.encode(2.5);
        assertTrue(codec.compareBytes(b1, b2) < 0);
        assertTrue(codec.compareBytes(b2, b1) > 0);
        assertEquals(0, codec.compareBytes(b1, codec.encode(1.5)));
    }

    @Test
    public void testF64Codec_Id() {
        F64Codec codec = F64Codec.INSTANCE;
        assertEquals("fx:f64", codec.id());
        assertEquals(1, codec.version());
    }

    @Test
    public void testF64Codec_RandomValues() {
        F64Codec codec = F64Codec.INSTANCE;

        for (int i = 0; i < 1000; i++) {
            double value = random.nextDouble() * 1000000 - 500000;
            byte[] encoded = codec.encode(value);
            double decoded = codec.decode(encoded);
            assertEquals(value, decoded, 0.0001);
        }
    }

    // ==================== StringCodec 테스트 ====================

    @Test
    public void testStringCodec_BasicEncodeDecode() {
        StringCodec codec = StringCodec.INSTANCE;

        assertEquals("", codec.decode(codec.encode("")));
        assertEquals("hello", codec.decode(codec.encode("hello")));
        assertEquals("한글", codec.decode(codec.encode("한글")));
        assertEquals("emoji 😀", codec.decode(codec.encode("emoji 😀")));
    }

    @Test
    public void testStringCodec_CompareBytes() {
        StringCodec codec = StringCodec.INSTANCE;

        byte[] apple = codec.encode("apple");
        byte[] banana = codec.encode("banana");
        assertTrue(codec.compareBytes(apple, banana) < 0);
        assertTrue(codec.compareBytes(banana, apple) > 0);
        assertEquals(0, codec.compareBytes(apple, codec.encode("apple")));
    }

    @Test
    public void testStringCodec_EqualsBytes() {
        StringCodec codec = StringCodec.INSTANCE;

        byte[] b1 = codec.encode("test");
        byte[] b2 = codec.encode("test");
        byte[] b3 = codec.encode("other");

        assertTrue(codec.equalsBytes(b1, b2));
        assertFalse(codec.equalsBytes(b1, b3));
    }

    @Test
    public void testStringCodec_HashBytes() {
        StringCodec codec = StringCodec.INSTANCE;

        byte[] b1 = codec.encode("test");
        byte[] b2 = codec.encode("test");

        assertEquals(codec.hashBytes(b1), codec.hashBytes(b2));
    }

    @Test
    public void testStringCodec_Id() {
        StringCodec codec = StringCodec.INSTANCE;
        assertEquals("fx:string:utf8", codec.id());
        assertEquals(1, codec.version());
    }

    @Test
    public void testStringCodec_LongString() {
        StringCodec codec = StringCodec.INSTANCE;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("a");
        }
        String longString = sb.toString();

        assertEquals(longString, codec.decode(codec.encode(longString)));
    }

    // ==================== BytesCodec 테스트 ====================

    @Test
    public void testBytesCodec_BasicEncodeDecode() {
        BytesCodec codec = BytesCodec.INSTANCE;

        byte[] empty = new byte[0];
        assertArrayEquals(empty, codec.decode(codec.encode(empty)));

        byte[] data = {1, 2, 3, 4, 5};
        assertArrayEquals(data, codec.decode(codec.encode(data)));
    }

    @Test
    public void testBytesCodec_CompareBytes() {
        BytesCodec codec = BytesCodec.INSTANCE;

        byte[] b1 = codec.encode(new byte[]{1, 2, 3});
        byte[] b2 = codec.encode(new byte[]{1, 2, 4});
        assertTrue(codec.compareBytes(b1, b2) < 0);
        assertTrue(codec.compareBytes(b2, b1) > 0);
    }

    @Test
    public void testBytesCodec_EqualsBytes() {
        BytesCodec codec = BytesCodec.INSTANCE;

        byte[] b1 = codec.encode(new byte[]{1, 2, 3});
        byte[] b2 = codec.encode(new byte[]{1, 2, 3});
        byte[] b3 = codec.encode(new byte[]{1, 2, 4});

        assertTrue(codec.equalsBytes(b1, b2));
        assertFalse(codec.equalsBytes(b1, b3));
    }

    @Test
    public void testBytesCodec_Id() {
        BytesCodec codec = BytesCodec.INSTANCE;
        assertEquals("fx:bytes:lenlex", codec.id());
        assertEquals(1, codec.version());
    }

    @Test
    public void testBytesCodec_RandomData() {
        BytesCodec codec = BytesCodec.INSTANCE;

        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[random.nextInt(1000)];
            random.nextBytes(data);
            assertArrayEquals(data, codec.decode(codec.encode(data)));
        }
    }

    // ==================== IntegerCodec 테스트 ====================

    @Test
    public void testIntegerCodec_BasicEncodeDecode() {
        IntegerCodec codec = IntegerCodec.INSTANCE;

        assertEquals(Integer.valueOf(0), codec.decode(codec.encode(0)));
        assertEquals(Integer.valueOf(1), codec.decode(codec.encode(1)));
        assertEquals(Integer.valueOf(-1), codec.decode(codec.encode(-1)));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), codec.decode(codec.encode(Integer.MAX_VALUE)));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), codec.decode(codec.encode(Integer.MIN_VALUE)));
    }

    @Test
    public void testIntegerCodec_CompareBytes() {
        IntegerCodec codec = IntegerCodec.INSTANCE;

        byte[] b1 = codec.encode(100);
        byte[] b2 = codec.encode(200);
        assertTrue(codec.compareBytes(b1, b2) < 0);
        assertTrue(codec.compareBytes(b2, b1) > 0);
    }

    @Test
    public void testIntegerCodec_Id() {
        IntegerCodec codec = IntegerCodec.INSTANCE;
        assertEquals("fx:i64", codec.id());
        assertEquals(1, codec.version());
    }

    // ==================== FloatCodec 테스트 ====================

    @Test
    public void testFloatCodec_BasicEncodeDecode() {
        FloatCodec codec = FloatCodec.INSTANCE;

        assertEquals(0.0f, codec.decode(codec.encode(0.0f)), 0.0001f);
        assertEquals(1.0f, codec.decode(codec.encode(1.0f)), 0.0001f);
        assertEquals(-1.0f, codec.decode(codec.encode(-1.0f)), 0.0001f);
    }

    @Test
    public void testFloatCodec_Id() {
        FloatCodec codec = FloatCodec.INSTANCE;
        assertEquals("fx:f64", codec.id());
        assertEquals(1, codec.version());
    }

    // ==================== ShortCodec 테스트 ====================

    @Test
    public void testShortCodec_BasicEncodeDecode() {
        ShortCodec codec = ShortCodec.INSTANCE;

        assertEquals(Short.valueOf((short) 0), codec.decode(codec.encode((short) 0)));
        assertEquals(Short.valueOf(Short.MAX_VALUE), codec.decode(codec.encode(Short.MAX_VALUE)));
        assertEquals(Short.valueOf(Short.MIN_VALUE), codec.decode(codec.encode(Short.MIN_VALUE)));
    }

    @Test
    public void testShortCodec_Id() {
        ShortCodec codec = ShortCodec.INSTANCE;
        assertEquals("fx:i64", codec.id());
        assertEquals(1, codec.version());
    }

    // ==================== ByteCodec 테스트 ====================

    @Test
    public void testByteCodec_BasicEncodeDecode() {
        ByteCodec codec = ByteCodec.INSTANCE;

        assertEquals(Byte.valueOf((byte) 0), codec.decode(codec.encode((byte) 0)));
        assertEquals(Byte.valueOf(Byte.MAX_VALUE), codec.decode(codec.encode(Byte.MAX_VALUE)));
        assertEquals(Byte.valueOf(Byte.MIN_VALUE), codec.decode(codec.encode(Byte.MIN_VALUE)));
    }

    @Test
    public void testByteCodec_Id() {
        ByteCodec codec = ByteCodec.INSTANCE;
        assertEquals("fx:i64", codec.id());
        assertEquals(1, codec.version());
    }

    // ==================== 정렬 순서 테스트 ====================

    @Test
    public void testI64Codec_SortOrder() {
        I64Codec codec = I64Codec.INSTANCE;

        long[] values = {Long.MIN_VALUE, -1000L, -1L, 0L, 1L, 1000L, Long.MAX_VALUE};
        byte[][] encoded = new byte[values.length][];

        for (int i = 0; i < values.length; i++) {
            encoded[i] = codec.encode(values[i]);
        }

        // 정렬 순서 검증
        for (int i = 0; i < values.length - 1; i++) {
            assertTrue("Sort order violated at index " + i,
                codec.compareBytes(encoded[i], encoded[i + 1]) < 0);
        }
    }

    @Test
    public void testStringCodec_SortOrder() {
        StringCodec codec = StringCodec.INSTANCE;

        String[] values = {"a", "aa", "ab", "b", "ba", "bb"};
        byte[][] encoded = new byte[values.length][];

        for (int i = 0; i < values.length; i++) {
            encoded[i] = codec.encode(values[i]);
        }

        // 정렬 순서 검증
        for (int i = 0; i < values.length - 1; i++) {
            assertTrue("Sort order violated at index " + i,
                codec.compareBytes(encoded[i], encoded[i + 1]) < 0);
        }
    }
}
