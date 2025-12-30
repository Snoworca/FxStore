package com.snoworca.fxstore.ost;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * OSTPathFrame 테스트
 * P1 클래스 커버리지 개선
 */
public class OSTPathFrameTest {

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_shouldSetAllFields() {
        // Given
        OSTInternal node = new OSTInternal(100L, 3);

        // When
        OSTPathFrame frame = new OSTPathFrame(node, 2, 50);

        // Then
        assertSame(node, frame.node);
        assertEquals(2, frame.childIndex);
        assertEquals(node, frame.getNode());
        assertEquals(2, frame.getChildIndex());
        assertEquals(50, frame.getLocalPosition());
    }

    @Test
    public void constructor_zeroValues_shouldWork() {
        // Given
        OSTInternal node = new OSTInternal(0L, 0);

        // When
        OSTPathFrame frame = new OSTPathFrame(node, 0, 0);

        // Then
        assertSame(node, frame.node);
        assertEquals(0, frame.childIndex);
        assertEquals(0, frame.getLocalPosition());
    }

    @Test
    public void constructor_largeValues_shouldWork() {
        // Given
        OSTInternal node = new OSTInternal(Long.MAX_VALUE, Integer.MAX_VALUE);

        // When
        OSTPathFrame frame = new OSTPathFrame(node, Integer.MAX_VALUE, Integer.MAX_VALUE);

        // Then
        assertEquals(Integer.MAX_VALUE, frame.getChildIndex());
        assertEquals(Integer.MAX_VALUE, frame.getLocalPosition());
    }

    // ==================== Public 필드 접근 테스트 ====================

    @Test
    public void publicField_node_shouldBeAccessible() {
        // Given
        OSTInternal node = new OSTInternal(200L, 5);
        OSTPathFrame frame = new OSTPathFrame(node, 1, 10);

        // When & Then: Public 필드 직접 접근
        assertSame(node, frame.node);
    }

    @Test
    public void publicField_childIndex_shouldBeAccessible() {
        // Given
        OSTInternal node = new OSTInternal(300L, 2);
        OSTPathFrame frame = new OSTPathFrame(node, 7, 20);

        // When & Then: Public 필드 직접 접근
        assertEquals(7, frame.childIndex);
    }

    // ==================== getNode 테스트 ====================

    @Test
    public void getNode_shouldReturnSameAsPublicField() {
        // Given
        OSTInternal node = new OSTInternal(400L, 4);
        OSTPathFrame frame = new OSTPathFrame(node, 3, 30);

        // When & Then
        assertSame(frame.node, frame.getNode());
    }

    @Test
    public void getNode_shouldReturnOSTInternal() {
        // Given
        OSTInternal node = new OSTInternal(500L, 1);
        OSTPathFrame frame = new OSTPathFrame(node, 0, 0);

        // When
        OSTInternal result = frame.getNode();

        // Then
        assertNotNull(result);
        assertEquals(500L, result.getPageId());
    }

    // ==================== getChildIndex 테스트 ====================

    @Test
    public void getChildIndex_shouldReturnSameAsPublicField() {
        // Given
        OSTInternal node = new OSTInternal(600L, 3);
        OSTPathFrame frame = new OSTPathFrame(node, 5, 40);

        // When & Then
        assertEquals(frame.childIndex, frame.getChildIndex());
    }

    @Test
    public void getChildIndex_variousValues_shouldWork() {
        // Given
        OSTInternal node = new OSTInternal(700L, 2);

        // When & Then
        assertEquals(0, new OSTPathFrame(node, 0, 0).getChildIndex());
        assertEquals(1, new OSTPathFrame(node, 1, 0).getChildIndex());
        assertEquals(10, new OSTPathFrame(node, 10, 0).getChildIndex());
        assertEquals(100, new OSTPathFrame(node, 100, 0).getChildIndex());
    }

    // ==================== getLocalPosition 테스트 ====================

    @Test
    public void getLocalPosition_shouldReturnCorrectValue() {
        // Given
        OSTInternal node = new OSTInternal(800L, 1);
        OSTPathFrame frame = new OSTPathFrame(node, 0, 999);

        // When & Then
        assertEquals(999, frame.getLocalPosition());
    }

    @Test
    public void getLocalPosition_variousValues_shouldWork() {
        // Given
        OSTInternal node = new OSTInternal(900L, 0);

        // When & Then
        assertEquals(0, new OSTPathFrame(node, 0, 0).getLocalPosition());
        assertEquals(1, new OSTPathFrame(node, 0, 1).getLocalPosition());
        assertEquals(1000, new OSTPathFrame(node, 0, 1000).getLocalPosition());
        assertEquals(Integer.MAX_VALUE, new OSTPathFrame(node, 0, Integer.MAX_VALUE).getLocalPosition());
    }

    // ==================== toString 테스트 ====================

    @Test
    public void toString_shouldIncludePageId() {
        // Given
        OSTInternal node = new OSTInternal(12345L, 5);
        OSTPathFrame frame = new OSTPathFrame(node, 2, 100);

        // When
        String str = frame.toString();

        // Then
        assertTrue(str.contains("12345"));
    }

    @Test
    public void toString_shouldIncludeChildIndex() {
        // Given
        OSTInternal node = new OSTInternal(1L, 1);
        OSTPathFrame frame = new OSTPathFrame(node, 7, 50);

        // When
        String str = frame.toString();

        // Then
        assertTrue(str.contains("childIndex=7"));
    }

    @Test
    public void toString_shouldIncludeLocalPosition() {
        // Given
        OSTInternal node = new OSTInternal(1L, 1);
        OSTPathFrame frame = new OSTPathFrame(node, 0, 999);

        // When
        String str = frame.toString();

        // Then
        assertTrue(str.contains("localPosition=999"));
    }

    @Test
    public void toString_shouldStartWithOSTPathFrame() {
        // Given
        OSTInternal node = new OSTInternal(1L, 1);
        OSTPathFrame frame = new OSTPathFrame(node, 0, 0);

        // When
        String str = frame.toString();

        // Then
        assertTrue(str.startsWith("OSTPathFrame{"));
        assertTrue(str.endsWith("}"));
    }

    @Test
    public void toString_allFieldsPresent() {
        // Given
        OSTInternal node = new OSTInternal(42L, 3);
        OSTPathFrame frame = new OSTPathFrame(node, 5, 100);

        // When
        String str = frame.toString();

        // Then
        assertTrue(str.contains("pageId=42"));
        assertTrue(str.contains("childIndex=5"));
        assertTrue(str.contains("localPosition=100"));
    }

    // ==================== 불변성 테스트 ====================

    @Test
    public void frame_shouldBeEffectivelyImmutable() {
        // Given
        OSTInternal node = new OSTInternal(1000L, 5);
        OSTPathFrame frame = new OSTPathFrame(node, 3, 50);

        // When: 여러 번 접근
        OSTInternal node1 = frame.getNode();
        OSTInternal node2 = frame.getNode();
        int idx1 = frame.getChildIndex();
        int idx2 = frame.getChildIndex();
        int pos1 = frame.getLocalPosition();
        int pos2 = frame.getLocalPosition();

        // Then: 값이 변하지 않음
        assertSame(node1, node2);
        assertEquals(idx1, idx2);
        assertEquals(pos1, pos2);
    }

    // ==================== 경계값 테스트 ====================

    @Test
    public void negativeChildIndex_shouldBeAllowed() {
        // Given: API doesn't prevent negative values
        OSTInternal node = new OSTInternal(1L, 1);

        // When
        OSTPathFrame frame = new OSTPathFrame(node, -1, 0);

        // Then
        assertEquals(-1, frame.getChildIndex());
    }

    @Test
    public void negativeLocalPosition_shouldBeAllowed() {
        // Given: API doesn't prevent negative values
        OSTInternal node = new OSTInternal(1L, 1);

        // When
        OSTPathFrame frame = new OSTPathFrame(node, 0, -10);

        // Then
        assertEquals(-10, frame.getLocalPosition());
    }

    // ==================== 실제 시나리오 테스트 ====================

    @Test
    public void pathFrame_representingTreeTraversal_shouldWork() {
        // Given: Simulating a path from root to leaf
        OSTInternal root = new OSTInternal(1L, 3);
        OSTInternal level2 = new OSTInternal(10L, 2);
        OSTInternal level1 = new OSTInternal(100L, 1);

        // When: Creating path frames
        OSTPathFrame rootFrame = new OSTPathFrame(root, 2, 50);
        OSTPathFrame level2Frame = new OSTPathFrame(level2, 1, 20);
        OSTPathFrame level1Frame = new OSTPathFrame(level1, 0, 5);

        // Then: Each frame represents a step in the path
        assertEquals(3, rootFrame.getNode().getLevel());
        assertEquals(2, level2Frame.getNode().getLevel());
        assertEquals(1, level1Frame.getNode().getLevel());
    }

    @Test
    public void multipleFrames_sameNode_differentIndex_shouldWork() {
        // Given
        OSTInternal node = new OSTInternal(500L, 2);

        // When
        OSTPathFrame frame1 = new OSTPathFrame(node, 0, 10);
        OSTPathFrame frame2 = new OSTPathFrame(node, 1, 20);
        OSTPathFrame frame3 = new OSTPathFrame(node, 2, 30);

        // Then: Same node but different child indices
        assertSame(node, frame1.getNode());
        assertSame(node, frame2.getNode());
        assertSame(node, frame3.getNode());
        assertNotEquals(frame1.getChildIndex(), frame2.getChildIndex());
        assertNotEquals(frame2.getChildIndex(), frame3.getChildIndex());
    }
}
