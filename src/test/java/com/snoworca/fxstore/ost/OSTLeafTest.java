package com.snoworca.fxstore.ost;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * OSTLeaf 테스트
 * P2 클래스 커버리지 개선
 */
public class OSTLeafTest {

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_withPageId_shouldSetPageId() {
        // Given & When
        OSTLeaf leaf = new OSTLeaf(100L);

        // Then
        assertEquals(100L, leaf.getPageId());
        assertEquals(0, leaf.getValueCount());
        assertEquals(0, leaf.getNextLeafPageId());
    }

    @Test
    public void constructor_default_shouldSetPageIdZero() {
        // Given & When
        OSTLeaf leaf = new OSTLeaf();

        // Then
        assertEquals(0L, leaf.getPageId());
    }

    // ==================== OSTNode 인터페이스 테스트 ====================

    @Test
    public void isLeaf_shouldReturnTrue() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);

        // When & Then
        assertTrue(leaf.isLeaf());
    }

    @Test
    public void getSubtreeCount_shouldReturnValueCount() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);
        leaf.add(200L);
        leaf.add(300L);

        // When & Then
        assertEquals(3, leaf.getSubtreeCount());
    }

    @Test
    public void subtreeCount_shouldBeAliasForGetSubtreeCount() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);
        leaf.add(200L);

        // When & Then
        assertEquals(leaf.getSubtreeCount(), leaf.subtreeCount());
    }

    @Test
    public void setPageId_shouldUpdatePageId() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);

        // When
        leaf.setPageId(999L);

        // Then
        assertEquals(999L, leaf.getPageId());
    }

    // ==================== 값 관리 테스트 ====================

    @Test
    public void add_shouldIncreaseValueCount() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);

        // When
        leaf.add(100L);
        leaf.add(200L);

        // Then
        assertEquals(2, leaf.getValueCount());
    }

    @Test
    public void addElement_shouldBeAliasForAdd() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);

        // When
        leaf.addElement(100L);

        // Then
        assertEquals(1, leaf.getValueCount());
        assertEquals(100L, leaf.getValueRef(0));
    }

    @Test
    public void getValueRef_shouldReturnCorrectValue() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);
        leaf.add(200L);
        leaf.add(300L);

        // When & Then
        assertEquals(100L, leaf.getValueRef(0));
        assertEquals(200L, leaf.getValueRef(1));
        assertEquals(300L, leaf.getValueRef(2));
    }

    @Test
    public void getElementRecordId_shouldBeAliasForGetValueRef() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(500L);

        // When & Then
        assertEquals(leaf.getValueRef(0), leaf.getElementRecordId(0));
    }

    @Test
    public void setValueRef_shouldUpdateValue() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);

        // When
        leaf.setValueRef(0, 999L);

        // Then
        assertEquals(999L, leaf.getValueRef(0));
    }

    @Test
    public void insert_shouldAddAtIndex() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);
        leaf.add(300L);

        // When
        leaf.insert(1, 200L);

        // Then
        assertEquals(3, leaf.getValueCount());
        assertEquals(100L, leaf.getValueRef(0));
        assertEquals(200L, leaf.getValueRef(1));
        assertEquals(300L, leaf.getValueRef(2));
    }

    @Test
    public void remove_shouldRemoveAndReturnValue() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);
        leaf.add(200L);
        leaf.add(300L);

        // When
        long removed = leaf.remove(1);

        // Then
        assertEquals(200L, removed);
        assertEquals(2, leaf.getValueCount());
        assertEquals(100L, leaf.getValueRef(0));
        assertEquals(300L, leaf.getValueRef(1));
    }

    // ==================== 링크드 리프 테스트 ====================

    @Test
    public void getNextLeafPageId_default_shouldBeZero() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);

        // When & Then
        assertEquals(0L, leaf.getNextLeafPageId());
    }

    @Test
    public void setNextLeafPageId_shouldUpdateValue() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);

        // When
        leaf.setNextLeafPageId(999L);

        // Then
        assertEquals(999L, leaf.getNextLeafPageId());
    }

    // ==================== 분할/병합 테스트 ====================

    @Test
    public void needsSplit_underLimit_shouldReturnFalse() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);
        leaf.add(200L);

        // When & Then
        assertFalse(leaf.needsSplit(10));
    }

    @Test
    public void needsSplit_overLimit_shouldReturnTrue() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        for (int i = 0; i < 11; i++) {
            leaf.add((long) i);
        }

        // When & Then
        assertTrue(leaf.needsSplit(10));
    }

    @Test
    public void canMerge_underMin_shouldReturnTrue() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);

        // When & Then
        assertTrue(leaf.canMerge(5));
    }

    @Test
    public void canMerge_overMin_shouldReturnFalse() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        for (int i = 0; i < 10; i++) {
            leaf.add((long) i);
        }

        // When & Then
        assertFalse(leaf.canMerge(5));
    }

    @Test
    public void split_shouldDivideValues() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        for (int i = 0; i < 10; i++) {
            leaf.add((long) (i * 100));
        }
        leaf.setNextLeafPageId(999L);

        // When
        OSTLeaf right = leaf.split(2L);

        // Then: Left should have first half
        assertEquals(5, leaf.getValueCount());
        assertEquals(0L, leaf.getValueRef(0));
        assertEquals(400L, leaf.getValueRef(4));

        // Right should have second half
        assertEquals(5, right.getValueCount());
        assertEquals(500L, right.getValueRef(0));
        assertEquals(900L, right.getValueRef(4));

        // Links should be updated
        assertEquals(2L, leaf.getNextLeafPageId());
        assertEquals(999L, right.getNextLeafPageId());
    }

    @Test
    public void split_oddCount_shouldWork() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        for (int i = 0; i < 5; i++) {
            leaf.add((long) i);
        }

        // When
        OSTLeaf right = leaf.split(2L);

        // Then: 5 / 2 = 2 left, 3 right
        assertEquals(2, leaf.getValueCount());
        assertEquals(3, right.getValueCount());
    }

    // ==================== 직렬화/역직렬화 테스트 ====================

    @Test
    public void serialize_deserialize_shouldRoundTrip() {
        // Given
        OSTLeaf original = new OSTLeaf(100L);
        original.add(1000L);
        original.add(2000L);
        original.add(3000L);
        original.setNextLeafPageId(200L);

        // When
        byte[] serialized = original.serialize();
        OSTLeaf deserialized = OSTLeaf.deserialize(100L, serialized);

        // Then
        assertEquals(original.getPageId(), deserialized.getPageId());
        assertEquals(original.getValueCount(), deserialized.getValueCount());
        assertEquals(original.getNextLeafPageId(), deserialized.getNextLeafPageId());
        for (int i = 0; i < original.getValueCount(); i++) {
            assertEquals(original.getValueRef(i), deserialized.getValueRef(i));
        }
    }

    @Test
    public void serialize_emptyLeaf_shouldWork() {
        // Given
        OSTLeaf original = new OSTLeaf(1L);

        // When
        byte[] serialized = original.serialize();
        OSTLeaf deserialized = OSTLeaf.deserialize(1L, serialized);

        // Then
        assertEquals(0, deserialized.getValueCount());
    }

    @Test
    public void serialize_shouldProduceCorrectSize() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);
        leaf.add(200L);

        // When
        byte[] serialized = leaf.serialize();

        // Then: 2 (count) + 8 (nextLeaf) + 2*8 (values) = 26
        assertEquals(26, serialized.length);
    }

    // ==================== toPage / fromPage 테스트 ====================

    @Test
    public void toPage_shouldIncludePageType() {
        // Given
        OSTLeaf leaf = new OSTLeaf(1L);
        leaf.add(100L);

        // When
        byte[] page = leaf.toPage(4096);

        // Then
        assertEquals(4096, page.length);
        assertEquals(1, page[0]); // Page type: LEAF
    }

    @Test
    public void fromPage_shouldDeserializeWithoutPageType() {
        // Given
        OSTLeaf original = new OSTLeaf(1L);
        original.add(100L);
        original.add(200L);
        original.setNextLeafPageId(999L);
        byte[] page = original.toPage(4096);

        // When
        OSTLeaf deserialized = OSTLeaf.fromPage(page);

        // Then
        assertEquals(2, deserialized.getValueCount());
        assertEquals(100L, deserialized.getValueRef(0));
        assertEquals(200L, deserialized.getValueRef(1));
        assertEquals(999L, deserialized.getNextLeafPageId());
    }

    @Test
    public void toPage_fromPage_shouldRoundTrip() {
        // Given
        OSTLeaf original = new OSTLeaf(1L);
        for (int i = 0; i < 100; i++) {
            original.add((long) i);
        }
        original.setNextLeafPageId(12345L);

        // When
        byte[] page = original.toPage(4096);
        OSTLeaf deserialized = OSTLeaf.fromPage(page);

        // Then
        assertEquals(original.getValueCount(), deserialized.getValueCount());
        assertEquals(original.getNextLeafPageId(), deserialized.getNextLeafPageId());
    }

    // ==================== 경계값 테스트 ====================

    @Test
    public void largeValues_shouldWork() {
        // Given
        OSTLeaf leaf = new OSTLeaf(Long.MAX_VALUE);
        leaf.add(Long.MAX_VALUE);
        leaf.add(Long.MIN_VALUE);
        leaf.setNextLeafPageId(Long.MAX_VALUE);

        // When
        byte[] serialized = leaf.serialize();
        OSTLeaf deserialized = OSTLeaf.deserialize(Long.MAX_VALUE, serialized);

        // Then
        assertEquals(Long.MAX_VALUE, deserialized.getPageId());
        assertEquals(Long.MAX_VALUE, deserialized.getValueRef(0));
        assertEquals(Long.MIN_VALUE, deserialized.getValueRef(1));
        assertEquals(Long.MAX_VALUE, deserialized.getNextLeafPageId());
    }
}
