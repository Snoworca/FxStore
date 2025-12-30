package com.snoworca.fxstore.catalog;

import com.snoworca.fxstore.api.CodecRef;
import com.snoworca.fxstore.api.CollectionKind;
import com.snoworca.fxstore.api.FxType;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * CatalogEntry 테스트
 * P1 클래스 커버리지 개선
 */
public class CatalogEntryTest {

    // ==================== 생성자 테스트 (전체 파라미터) ====================

    @Test
    public void constructor_full_shouldSetAllFields() {
        // Given
        CodecRef keyCodec = new CodecRef("fx:i64", 1, FxType.I64);
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);

        // When
        CatalogEntry entry = new CatalogEntry("myMap", 100L, CollectionKind.MAP, keyCodec, valueCodec);

        // Then
        assertEquals("myMap", entry.getName());
        assertEquals(100L, entry.getCollectionId());
        assertEquals(CollectionKind.MAP, entry.getKind());
        assertEquals(keyCodec, entry.getKeyCodec());
        assertEquals(valueCodec, entry.getValueCodec());
    }

    @Test
    public void constructor_full_withNullCodecs_shouldWork() {
        // Given & When
        CatalogEntry entry = new CatalogEntry("mySet", 200L, CollectionKind.SET, null, null);

        // Then
        assertEquals("mySet", entry.getName());
        assertEquals(200L, entry.getCollectionId());
        assertEquals(CollectionKind.SET, entry.getKind());
        assertNull(entry.getKeyCodec());
        assertNull(entry.getValueCodec());
    }

    // ==================== 생성자 테스트 (간단 버전) ====================

    @Test
    public void constructor_simplified_shouldDefaultToList() {
        // Given & When
        CatalogEntry entry = new CatalogEntry("myList", 300L);

        // Then
        assertEquals("myList", entry.getName());
        assertEquals(300L, entry.getCollectionId());
        assertEquals(CollectionKind.LIST, entry.getKind());
        assertNull(entry.getKeyCodec());
        assertNull(entry.getValueCodec());
    }

    @Test
    public void constructor_withKindOnly_shouldSetKindAndNullCodecs() {
        // Given & When
        CatalogEntry entry = new CatalogEntry("myDeque", 400L, CollectionKind.DEQUE);

        // Then
        assertEquals("myDeque", entry.getName());
        assertEquals(400L, entry.getCollectionId());
        assertEquals(CollectionKind.DEQUE, entry.getKind());
        assertNull(entry.getKeyCodec());
        assertNull(entry.getValueCodec());
    }

    // ==================== 접근자 테스트 ====================

    @Test
    public void getName_shouldReturnName() {
        // Given
        CatalogEntry entry = new CatalogEntry("testName", 1L);

        // When & Then
        assertEquals("testName", entry.getName());
    }

    @Test
    public void getCollectionId_shouldReturnId() {
        // Given
        CatalogEntry entry = new CatalogEntry("test", 12345L);

        // When & Then
        assertEquals(12345L, entry.getCollectionId());
    }

    @Test
    public void getKind_shouldReturnKind() {
        // Given
        CatalogEntry entry = new CatalogEntry("test", 1L, CollectionKind.SET);

        // When & Then
        assertEquals(CollectionKind.SET, entry.getKind());
    }

    @Test
    public void getKind_allKinds_shouldWork() {
        // Given & When & Then
        for (CollectionKind kind : CollectionKind.values()) {
            CatalogEntry entry = new CatalogEntry("test", 1L, kind);
            assertEquals(kind, entry.getKind());
        }
    }

    // ==================== serialize / deserialize 테스트 ====================

    @Test
    public void serialize_deserialize_shouldRoundTrip() {
        // Given
        CodecRef keyCodec = new CodecRef("fx:i64", 1, FxType.I64);
        CodecRef valueCodec = new CodecRef("fx:string", 2, FxType.STRING);
        CatalogEntry original = new CatalogEntry("myCollection", 500L, CollectionKind.MAP, keyCodec, valueCodec);

        // When
        byte[] serialized = original.serialize();
        CatalogEntry deserialized = CatalogEntry.deserialize(serialized);

        // Then
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getCollectionId(), deserialized.getCollectionId());
        assertEquals(original.getKind(), deserialized.getKind());
        assertEquals(original.getKeyCodec().codecId(), deserialized.getKeyCodec().codecId());
        assertEquals(original.getKeyCodec().codecVersion(), deserialized.getKeyCodec().codecVersion());
        assertEquals(original.getValueCodec().codecId(), deserialized.getValueCodec().codecId());
        assertEquals(original.getValueCodec().codecVersion(), deserialized.getValueCodec().codecVersion());
    }

    @Test
    public void serialize_deserialize_withNullCodecs_shouldRoundTrip() {
        // Given
        CatalogEntry original = new CatalogEntry("setCollection", 600L, CollectionKind.SET, null, null);

        // When
        byte[] serialized = original.serialize();
        CatalogEntry deserialized = CatalogEntry.deserialize(serialized);

        // Then
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getCollectionId(), deserialized.getCollectionId());
        assertEquals(original.getKind(), deserialized.getKind());
        assertNull(deserialized.getKeyCodec());
        assertNull(deserialized.getValueCodec());
    }

    @Test
    public void serialize_deserialize_allKinds_shouldRoundTrip() {
        // Given & When & Then
        for (CollectionKind kind : CollectionKind.values()) {
            CatalogEntry original = new CatalogEntry("test_" + kind, (long) kind.ordinal(), kind);
            byte[] serialized = original.serialize();
            CatalogEntry deserialized = CatalogEntry.deserialize(serialized);

            assertEquals(original.getName(), deserialized.getName());
            assertEquals(original.getCollectionId(), deserialized.getCollectionId());
            assertEquals(original.getKind(), deserialized.getKind());
        }
    }

    @Test
    public void serialize_deserialize_longName_shouldRoundTrip() {
        // Given
        String longName = "very_long_collection_name_with_many_characters_for_testing_purposes";
        CatalogEntry original = new CatalogEntry(longName, 700L, CollectionKind.LIST);

        // When
        byte[] serialized = original.serialize();
        CatalogEntry deserialized = CatalogEntry.deserialize(serialized);

        // Then
        assertEquals(longName, deserialized.getName());
    }

    @Test
    public void serialize_deserialize_unicodeName_shouldRoundTrip() {
        // Given
        String unicodeName = "컬렉션_한글_テスト_emoji_🔥";
        CatalogEntry original = new CatalogEntry(unicodeName, 800L, CollectionKind.DEQUE);

        // When
        byte[] serialized = original.serialize();
        CatalogEntry deserialized = CatalogEntry.deserialize(serialized);

        // Then
        assertEquals(unicodeName, deserialized.getName());
    }

    @Test
    public void serialize_deserialize_maxCollectionId_shouldRoundTrip() {
        // Given
        CatalogEntry original = new CatalogEntry("maxId", Long.MAX_VALUE, CollectionKind.MAP);

        // When
        byte[] serialized = original.serialize();
        CatalogEntry deserialized = CatalogEntry.deserialize(serialized);

        // Then
        assertEquals(Long.MAX_VALUE, deserialized.getCollectionId());
    }

    // ==================== encode / decode alias 테스트 ====================

    @Test
    public void encode_shouldCallSerialize() {
        // Given
        CatalogEntry entry = new CatalogEntry("test", 100L, CollectionKind.SET);

        // When
        byte[] encoded = entry.encode();
        byte[] serialized = entry.serialize();

        // Then
        assertArrayEquals(serialized, encoded);
    }

    @Test
    public void decode_shouldCallDeserialize() {
        // Given
        CatalogEntry original = new CatalogEntry("test", 200L, CollectionKind.LIST);
        byte[] data = original.serialize();

        // When
        CatalogEntry fromDecode = CatalogEntry.decode(data);
        CatalogEntry fromDeserialize = CatalogEntry.deserialize(data);

        // Then
        assertEquals(fromDeserialize.getName(), fromDecode.getName());
        assertEquals(fromDeserialize.getCollectionId(), fromDecode.getCollectionId());
    }

    // ==================== toString 테스트 ====================

    @Test
    public void toString_shouldIncludeName() {
        // Given
        CatalogEntry entry = new CatalogEntry("myCollection", 100L, CollectionKind.MAP);

        // When
        String str = entry.toString();

        // Then
        assertTrue(str.contains("myCollection"));
    }

    @Test
    public void toString_shouldIncludeCollectionId() {
        // Given
        CatalogEntry entry = new CatalogEntry("test", 12345L, CollectionKind.SET);

        // When
        String str = entry.toString();

        // Then
        assertTrue(str.contains("12345"));
    }

    @Test
    public void toString_shouldIncludeKind() {
        // Given
        CatalogEntry entry = new CatalogEntry("test", 100L, CollectionKind.DEQUE);

        // When
        String str = entry.toString();

        // Then
        assertTrue(str.contains("DEQUE"));
    }

    @Test
    public void toString_shouldStartWithCatalogEntry() {
        // Given
        CatalogEntry entry = new CatalogEntry("test", 100L);

        // When
        String str = entry.toString();

        // Then
        assertTrue(str.startsWith("CatalogEntry{"));
    }

    // ==================== 경계값 테스트 ====================

    @Test
    public void emptyName_shouldWork() {
        // Given & When
        CatalogEntry entry = new CatalogEntry("", 1L, CollectionKind.LIST);

        // Then
        assertEquals("", entry.getName());

        // Serialize/Deserialize should work
        byte[] serialized = entry.serialize();
        CatalogEntry deserialized = CatalogEntry.deserialize(serialized);
        assertEquals("", deserialized.getName());
    }

    @Test
    public void zeroCollectionId_shouldWork() {
        // Given & When
        CatalogEntry entry = new CatalogEntry("test", 0L, CollectionKind.SET);

        // Then
        assertEquals(0L, entry.getCollectionId());

        // Serialize/Deserialize should work
        byte[] serialized = entry.serialize();
        CatalogEntry deserialized = CatalogEntry.deserialize(serialized);
        assertEquals(0L, deserialized.getCollectionId());
    }

    @Test
    public void serialize_shouldProduceNonEmptyBytes() {
        // Given
        CatalogEntry entry = new CatalogEntry("test", 100L, CollectionKind.MAP);

        // When
        byte[] serialized = entry.serialize();

        // Then
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);
    }

    // ==================== MAP 전용 테스트 ====================

    @Test
    public void map_withBothCodecs_shouldWork() {
        // Given
        CodecRef keyCodec = new CodecRef("fx:i64", 1, FxType.I64);
        CodecRef valueCodec = new CodecRef("custom:user", 3, null);

        // When
        CatalogEntry entry = new CatalogEntry("userMap", 1000L, CollectionKind.MAP, keyCodec, valueCodec);

        // Then
        assertEquals(CollectionKind.MAP, entry.getKind());
        assertNotNull(entry.getKeyCodec());
        assertNotNull(entry.getValueCodec());
        assertEquals("fx:i64", entry.getKeyCodec().codecId());
        assertEquals("custom:user", entry.getValueCodec().codecId());
    }

    @Test
    public void serialize_deserialize_mapWithCodecs_shouldPreserveCodecInfo() {
        // Given
        CodecRef keyCodec = new CodecRef("key:codec", 5, null);
        CodecRef valueCodec = new CodecRef("value:codec", 10, null);
        CatalogEntry original = new CatalogEntry("complexMap", 2000L, CollectionKind.MAP, keyCodec, valueCodec);

        // When
        byte[] serialized = original.serialize();
        CatalogEntry deserialized = CatalogEntry.deserialize(serialized);

        // Then
        assertEquals("key:codec", deserialized.getKeyCodec().codecId());
        assertEquals(5, deserialized.getKeyCodec().codecVersion());
        assertEquals("value:codec", deserialized.getValueCodec().codecId());
        assertEquals(10, deserialized.getValueCodec().codecVersion());
    }
}
