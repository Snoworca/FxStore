package com.snoworca.fxstore.ost;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * OSTInternal 노드 테스트
 *
 * <p>P0-3: OSTInternal 브랜치 커버리지 30% -> 60%+ 달성을 위한 테스트</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>노드 생성 및 초기화</li>
 *   <li>자식 노드 관리 (add, insert, remove, set)</li>
 *   <li>position 기반 자식 찾기 (findChildForPosition)</li>
 *   <li>노드 분할 (split)</li>
 *   <li>병합 조건 검사 (needsSplit, canMerge)</li>
 *   <li>직렬화/역직렬화 (serialize, deserialize, toPage, fromPage)</li>
 * </ul>
 *
 * @since v1.0 Phase 2
 * @see OSTInternal
 */
public class OSTInternalTest {

    private static final long PAGE_ID = 100L;
    private static final int LEVEL = 1;

    private OSTInternal node;

    @Before
    public void setUp() {
        node = new OSTInternal(PAGE_ID, LEVEL);
    }

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_basicPageIdAndLevel_shouldInitialize() {
        OSTInternal internal = new OSTInternal(200L, 2);

        assertEquals(200L, internal.getPageId());
        assertEquals(2, internal.getLevel());
        assertEquals(0, internal.getChildCount());
        assertFalse(internal.isLeaf());
    }

    @Test
    public void constructor_withChildrenAndCounts_shouldInitialize() {
        List<Long> children = Arrays.asList(10L, 20L, 30L);
        List<Integer> counts = Arrays.asList(5, 10, 15);

        OSTInternal internal = new OSTInternal(LEVEL, children, counts);

        assertEquals(0L, internal.getPageId()); // 기본값
        assertEquals(LEVEL, internal.getLevel());
        assertEquals(3, internal.getChildCount());
        assertEquals(10L, internal.getChild(0));
        assertEquals(20L, internal.getChild(1));
        assertEquals(30L, internal.getChild(2));
        assertEquals(5, internal.getChildSubtreeCount(0));
        assertEquals(10, internal.getChildSubtreeCount(1));
        assertEquals(15, internal.getChildSubtreeCount(2));
    }

    // ==================== isLeaf() 테스트 ====================

    @Test
    public void isLeaf_shouldAlwaysReturnFalse() {
        assertFalse(node.isLeaf());

        node.addChild(10L, 5);
        assertFalse(node.isLeaf());
    }

    // ==================== pageId 관련 테스트 ====================

    @Test
    public void getPageId_shouldReturnPageId() {
        assertEquals(PAGE_ID, node.getPageId());
    }

    @Test
    public void setPageId_shouldUpdatePageId() {
        node.setPageId(500L);
        assertEquals(500L, node.getPageId());
    }

    // ==================== level 관련 테스트 ====================

    @Test
    public void getLevel_shouldReturnLevel() {
        assertEquals(LEVEL, node.getLevel());
    }

    // ==================== addChild() 테스트 ====================

    @Test
    public void addChild_shouldIncreaseChildCount() {
        assertEquals(0, node.getChildCount());

        node.addChild(10L, 5);
        assertEquals(1, node.getChildCount());

        node.addChild(20L, 10);
        assertEquals(2, node.getChildCount());

        node.addChild(30L, 15);
        assertEquals(3, node.getChildCount());
    }

    @Test
    public void addChild_shouldStoreCorrectValues() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        assertEquals(10L, node.getChild(0));
        assertEquals(20L, node.getChild(1));
        assertEquals(5, node.getChildSubtreeCount(0));
        assertEquals(10, node.getChildSubtreeCount(1));
    }

    // ==================== insertChild() 테스트 ====================

    @Test
    public void insertChild_atBeginning_shouldShiftRight() {
        node.addChild(20L, 10);
        node.addChild(30L, 15);

        node.insertChild(0, 10L, 5);

        assertEquals(3, node.getChildCount());
        assertEquals(10L, node.getChild(0));
        assertEquals(20L, node.getChild(1));
        assertEquals(30L, node.getChild(2));
        assertEquals(5, node.getChildSubtreeCount(0));
    }

    @Test
    public void insertChild_atMiddle_shouldShiftRight() {
        node.addChild(10L, 5);
        node.addChild(30L, 15);

        node.insertChild(1, 20L, 10);

        assertEquals(3, node.getChildCount());
        assertEquals(10L, node.getChild(0));
        assertEquals(20L, node.getChild(1));
        assertEquals(30L, node.getChild(2));
    }

    @Test
    public void insertChild_atEnd_shouldAppend() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        node.insertChild(2, 30L, 15);

        assertEquals(3, node.getChildCount());
        assertEquals(30L, node.getChild(2));
        assertEquals(15, node.getChildSubtreeCount(2));
    }

    // ==================== removeChild() 테스트 ====================

    @Test
    public void removeChild_shouldDecreaseChildCount() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);
        node.addChild(30L, 15);

        assertEquals(3, node.getChildCount());

        node.removeChild(1);

        assertEquals(2, node.getChildCount());
    }

    @Test
    public void removeChild_atBeginning_shouldShiftLeft() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);
        node.addChild(30L, 15);

        node.removeChild(0);

        assertEquals(2, node.getChildCount());
        assertEquals(20L, node.getChild(0));
        assertEquals(30L, node.getChild(1));
    }

    @Test
    public void removeChild_atEnd_shouldRemoveLast() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);
        node.addChild(30L, 15);

        node.removeChild(2);

        assertEquals(2, node.getChildCount());
        assertEquals(10L, node.getChild(0));
        assertEquals(20L, node.getChild(1));
    }

    // ==================== setChild() 테스트 ====================

    @Test
    public void setChild_shouldUpdateChildPageId() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        node.setChild(0, 100L);
        node.setChild(1, 200L);

        assertEquals(100L, node.getChild(0));
        assertEquals(200L, node.getChild(1));
    }

    // ==================== setChildSubtreeCount() 테스트 ====================

    @Test
    public void setChildSubtreeCount_shouldUpdateCount() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        node.setChildSubtreeCount(0, 50);
        node.setChildSubtreeCount(1, 100);

        assertEquals(50, node.getChildSubtreeCount(0));
        assertEquals(100, node.getChildSubtreeCount(1));
    }

    // ==================== getChildPageId() 테스트 (alias) ====================

    @Test
    public void getChildPageId_shouldReturnSameAsGetChild() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        assertEquals(node.getChild(0), node.getChildPageId(0));
        assertEquals(node.getChild(1), node.getChildPageId(1));
    }

    // ==================== getSubtreeCount() 테스트 ====================

    @Test
    public void getSubtreeCount_emptyNode_shouldReturnZero() {
        assertEquals(0, node.getSubtreeCount());
    }

    @Test
    public void getSubtreeCount_withChildren_shouldReturnSum() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);
        node.addChild(30L, 15);

        assertEquals(30, node.getSubtreeCount()); // 5 + 10 + 15
    }

    @Test
    public void getSubtreeCountByIndex_shouldReturnSpecificCount() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        assertEquals(5, node.getSubtreeCount(0));
        assertEquals(10, node.getSubtreeCount(1));
    }

    @Test
    public void subtreeCount_shouldReturnSameAsGetSubtreeCount() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        assertEquals(node.getSubtreeCount(), node.subtreeCount());
    }

    // ==================== findChildForPosition() 테스트 ====================

    @Test
    public void findChildForPosition_firstChild_shouldReturnIndex0() {
        node.addChild(10L, 5);  // positions 0-4
        node.addChild(20L, 10); // positions 5-14
        node.addChild(30L, 15); // positions 15-29

        int[] result = node.findChildForPosition(0);
        assertEquals(0, result[0]); // childIndex
        assertEquals(0, result[1]); // localPosition

        result = node.findChildForPosition(4);
        assertEquals(0, result[0]);
        assertEquals(4, result[1]);
    }

    @Test
    public void findChildForPosition_middleChild_shouldReturnCorrectIndex() {
        node.addChild(10L, 5);  // positions 0-4
        node.addChild(20L, 10); // positions 5-14
        node.addChild(30L, 15); // positions 15-29

        int[] result = node.findChildForPosition(5);
        assertEquals(1, result[0]); // childIndex
        assertEquals(0, result[1]); // localPosition

        result = node.findChildForPosition(10);
        assertEquals(1, result[0]);
        assertEquals(5, result[1]);

        result = node.findChildForPosition(14);
        assertEquals(1, result[0]);
        assertEquals(9, result[1]);
    }

    @Test
    public void findChildForPosition_lastChild_shouldReturnCorrectIndex() {
        node.addChild(10L, 5);  // positions 0-4
        node.addChild(20L, 10); // positions 5-14
        node.addChild(30L, 15); // positions 15-29

        int[] result = node.findChildForPosition(15);
        assertEquals(2, result[0]);
        assertEquals(0, result[1]);

        result = node.findChildForPosition(29);
        assertEquals(2, result[0]);
        assertEquals(14, result[1]);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void findChildForPosition_outOfRange_shouldThrow() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        node.findChildForPosition(15); // 총 15개인데 position 15 요청
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void findChildForPosition_emptyNode_shouldThrow() {
        node.findChildForPosition(0);
    }

    // ==================== needsSplit() 테스트 ====================

    @Test
    public void needsSplit_belowMax_shouldReturnFalse() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);
        node.addChild(30L, 15);

        assertFalse(node.needsSplit(4)); // 3 children, max 4
        assertFalse(node.needsSplit(3)); // 3 children, max 3 (equal)
    }

    @Test
    public void needsSplit_aboveMax_shouldReturnTrue() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);
        node.addChild(30L, 15);

        assertTrue(node.needsSplit(2)); // 3 children > max 2
    }

    @Test
    public void needsSplit_emptyNode_shouldReturnFalse() {
        assertFalse(node.needsSplit(1));
    }

    // ==================== canMerge() 테스트 ====================

    @Test
    public void canMerge_belowMin_shouldReturnTrue() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        assertTrue(node.canMerge(3)); // 2 children < min 3
    }

    @Test
    public void canMerge_atMin_shouldReturnFalse() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        assertFalse(node.canMerge(2)); // 2 children, min 2 (equal)
    }

    @Test
    public void canMerge_aboveMin_shouldReturnFalse() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);
        node.addChild(30L, 15);

        assertFalse(node.canMerge(2)); // 3 children > min 2
    }

    @Test
    public void canMerge_emptyNode_shouldReturnTrue() {
        assertTrue(node.canMerge(1)); // 0 children < min 1
    }

    // ==================== split() 테스트 ====================

    @Test
    public void split_evenChildren_shouldDivideEqually() {
        node.addChild(10L, 2);
        node.addChild(20L, 4);
        node.addChild(30L, 6);
        node.addChild(40L, 8);

        long newPageId = 200L;
        OSTInternal right = node.split(newPageId);

        // 원본 노드: 앞쪽 절반
        assertEquals(2, node.getChildCount());
        assertEquals(10L, node.getChild(0));
        assertEquals(20L, node.getChild(1));

        // 새 노드: 뒷쪽 절반
        assertEquals(newPageId, right.getPageId());
        assertEquals(LEVEL, right.getLevel());
        assertEquals(2, right.getChildCount());
        assertEquals(30L, right.getChild(0));
        assertEquals(40L, right.getChild(1));
    }

    @Test
    public void split_oddChildren_shouldDivideProperly() {
        node.addChild(10L, 1);
        node.addChild(20L, 2);
        node.addChild(30L, 3);
        node.addChild(40L, 4);
        node.addChild(50L, 5);

        long newPageId = 300L;
        OSTInternal right = node.split(newPageId);

        // 원본: 앞쪽 2개 (5/2 = 2)
        assertEquals(2, node.getChildCount());
        assertEquals(10L, node.getChild(0));
        assertEquals(20L, node.getChild(1));

        // 새 노드: 뒷쪽 3개
        assertEquals(3, right.getChildCount());
        assertEquals(30L, right.getChild(0));
        assertEquals(40L, right.getChild(1));
        assertEquals(50L, right.getChild(2));
    }

    @Test
    public void split_shouldPreserveSubtreeCounts() {
        node.addChild(10L, 100);
        node.addChild(20L, 200);
        node.addChild(30L, 300);
        node.addChild(40L, 400);

        OSTInternal right = node.split(200L);

        // 원본
        assertEquals(100, node.getChildSubtreeCount(0));
        assertEquals(200, node.getChildSubtreeCount(1));
        assertEquals(300, node.getSubtreeCount()); // 100 + 200

        // 새 노드
        assertEquals(300, right.getChildSubtreeCount(0));
        assertEquals(400, right.getChildSubtreeCount(1));
        assertEquals(700, right.getSubtreeCount()); // 300 + 400
    }

    // ==================== serialize/deserialize 테스트 ====================

    @Test
    public void serialize_emptyNode_shouldProduceValidBytes() {
        byte[] data = node.serialize();

        // level(2) + count(2) = 4 bytes
        assertEquals(4, data.length);

        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(LEVEL, buf.getShort() & 0xFFFF);
        assertEquals(0, buf.getShort() & 0xFFFF);
    }

    @Test
    public void serialize_withChildren_shouldProduceValidBytes() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        byte[] data = node.serialize();

        // level(2) + count(2) + 2*(child(8) + subtree(4)) = 4 + 24 = 28 bytes
        assertEquals(28, data.length);

        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(LEVEL, buf.getShort() & 0xFFFF);
        assertEquals(2, buf.getShort() & 0xFFFF);
        assertEquals(10L, buf.getLong());
        assertEquals(5, buf.getInt());
        assertEquals(20L, buf.getLong());
        assertEquals(10, buf.getInt());
    }

    @Test
    public void deserialize_shouldRestoreNode() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);
        node.addChild(30L, 15);

        byte[] data = node.serialize();
        OSTInternal restored = OSTInternal.deserialize(PAGE_ID, data);

        assertEquals(PAGE_ID, restored.getPageId());
        assertEquals(LEVEL, restored.getLevel());
        assertEquals(3, restored.getChildCount());
        assertEquals(10L, restored.getChild(0));
        assertEquals(20L, restored.getChild(1));
        assertEquals(30L, restored.getChild(2));
        assertEquals(5, restored.getChildSubtreeCount(0));
        assertEquals(10, restored.getChildSubtreeCount(1));
        assertEquals(15, restored.getChildSubtreeCount(2));
    }

    @Test
    public void serializeDeserialize_roundTrip_shouldBeEqual() {
        node.addChild(100L, 1000);
        node.addChild(200L, 2000);

        byte[] data = node.serialize();
        OSTInternal restored = OSTInternal.deserialize(PAGE_ID, data);

        assertEquals(node.getChildCount(), restored.getChildCount());
        for (int i = 0; i < node.getChildCount(); i++) {
            assertEquals(node.getChild(i), restored.getChild(i));
            assertEquals(node.getChildSubtreeCount(i), restored.getChildSubtreeCount(i));
        }
    }

    // ==================== toPage/fromPage 테스트 ====================

    @Test
    public void toPage_shouldIncludePageTypeByte() {
        node.addChild(10L, 5);

        byte[] page = node.toPage(4096);

        assertEquals(4096, page.length);
        assertEquals(2, page[0]); // Page type: INTERNAL
    }

    @Test
    public void fromPage_shouldSkipPageTypeByte() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        byte[] page = node.toPage(4096);
        OSTInternal restored = OSTInternal.fromPage(page);

        assertEquals(0L, restored.getPageId()); // fromPage는 pageId를 0으로 설정
        assertEquals(LEVEL, restored.getLevel());
        assertEquals(2, restored.getChildCount());
        assertEquals(10L, restored.getChild(0));
        assertEquals(20L, restored.getChild(1));
    }

    @Test
    public void toPageFromPage_roundTrip_shouldPreserveData() {
        node.addChild(100L, 50);
        node.addChild(200L, 100);
        node.addChild(300L, 150);

        byte[] page = node.toPage(4096);
        OSTInternal restored = OSTInternal.fromPage(page);

        assertEquals(node.getChildCount(), restored.getChildCount());
        assertEquals(node.getSubtreeCount(), restored.getSubtreeCount());
    }

    // ==================== 경계 조건 테스트 ====================

    @Test
    public void largeSubtreeCounts_shouldHandleCorrectly() {
        node.addChild(1L, Integer.MAX_VALUE / 2);
        node.addChild(2L, Integer.MAX_VALUE / 2);

        // overflow 없이 총합 계산
        long total = node.getSubtreeCount();
        assertTrue(total > 0);
    }

    @Test
    public void manyChildren_shouldHandleCorrectly() {
        for (int i = 0; i < 100; i++) {
            node.addChild(i, i + 1);
        }

        assertEquals(100, node.getChildCount());

        // serialize/deserialize 확인
        byte[] data = node.serialize();
        OSTInternal restored = OSTInternal.deserialize(PAGE_ID, data);
        assertEquals(100, restored.getChildCount());
    }

    @Test
    public void zeroSubtreeCount_shouldWork() {
        node.addChild(10L, 0);
        node.addChild(20L, 0);

        assertEquals(0, node.getSubtreeCount());
        assertEquals(2, node.getChildCount());

        // subtreeCount가 모두 0이면 findChildForPosition은 예외 발생 (정상 동작)
        try {
            node.findChildForPosition(0);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected: 모든 자식의 subtreeCount가 0이면 어떤 position도 찾을 수 없음
            assertTrue(e.getMessage().contains("Position out of range"));
        }
    }

    @Test
    public void split_twoChildren_shouldDivide() {
        node.addChild(10L, 5);
        node.addChild(20L, 10);

        OSTInternal right = node.split(200L);

        // mid = 2/2 = 1
        assertEquals(1, node.getChildCount());
        assertEquals(1, right.getChildCount());
        assertEquals(10L, node.getChild(0));
        assertEquals(20L, right.getChild(0));
    }
}
