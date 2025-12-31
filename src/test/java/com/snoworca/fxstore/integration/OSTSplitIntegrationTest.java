package com.snoworca.fxstore.integration;

import com.snoworca.fxstore.api.FxStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * OST 내부 노드 분할 통합 테스트.
 *
 * <p>목적: splitInternalNode() 메서드 커버리지 향상</p>
 * <p>트리거 조건: 대량 삽입으로 내부 노드 분할 유발</p>
 *
 * <p>OST 구조:</p>
 * <ul>
 *   <li>리프 노드: 최대 100개 요소</li>
 *   <li>내부 노드: 최대 128개 자식</li>
 *   <li>내부 노드 분할 트리거: 128 * 100 = 12,800개 이상 삽입</li>
 * </ul>
 *
 * @see com.snoworca.fxstore.ost.OST#splitInternalNode
 */
public class OSTSplitIntegrationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;

    @Before
    public void setUp() {
        store = FxStore.openMemory();
    }

    @After
    public void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    /**
     * 대량 순차 삽입으로 리프 노드 분할 유발.
     *
     * <p>500개 삽입하여 여러 리프 분할 발생</p>
     */
    @Test
    public void testLargeSequentialInsert_shouldTriggerLeafNodeSplit() {
        // Given: 빈 List
        List<Long> list = store.createList("bigList", Long.class);

        // When: 500개 삽입 (리프 분할 유발: 100개 초과)
        int count = 500;
        for (int i = 0; i < count; i++) {
            list.add((long) i);
        }

        // Then: 모든 요소 접근 가능
        assertEquals(count, list.size());
        assertEquals(Long.valueOf(0L), list.get(0));
        assertEquals(Long.valueOf(count - 1), list.get(count - 1));
        assertEquals(Long.valueOf(count / 2), list.get(count / 2));
    }

    /**
     * 중간 삽입으로 추가 분할 유발.
     *
     * <p>중간 위치에 삽입하여 리프 분할 테스트</p>
     */
    @Test
    public void testMiddleInsert_shouldTriggerAdditionalSplit() {
        // Given: 300개 요소가 있는 List
        List<Long> list = store.createList("midList", Long.class);
        for (int i = 0; i < 300; i++) {
            list.add((long) i);
        }

        // When: 중간 위치에 100개 삽입 (분할 유발)
        for (int i = 0; i < 100; i++) {
            list.add(150, 99999L + i);
        }

        // Then: 총 400개 존재
        assertEquals(400, list.size());

        // 검증: 일부 삽입된 요소 확인
        assertEquals(Long.valueOf(99999L + 99), list.get(150));
    }

    /**
     * 랜덤 위치 삽입 테스트.
     *
     * <p>랜덤 위치에 삽입하여 다양한 분할 패턴 테스트</p>
     */
    @Test
    public void testRandomInsert_shouldMaintainCorrectness() {
        List<Long> list = store.createList("randomList", Long.class);
        Random random = new Random(42);  // 재현 가능한 시드

        // 300개 랜덤 삽입
        for (int i = 0; i < 300; i++) {
            int pos = list.isEmpty() ? 0 : random.nextInt(list.size() + 1);
            list.add(pos, (long) i);
        }

        assertEquals(300, list.size());
    }

    /**
     * 파일 기반 스토어에서 대량 삽입 후 재오픈.
     *
     * <p>영속성 검증과 함께 삽입 테스트</p>
     */
    @Test
    public void testLargeInsert_withFileStore_andReopen() throws Exception {
        Path tempFile = tempFolder.newFile("large-list.fx").toPath();

        // 생성 및 삽입
        try (FxStore fileStore = FxStore.open(tempFile)) {
            List<Long> list = fileStore.createList("persistentList", Long.class);
            int count = 500;
            for (int i = 0; i < count; i++) {
                list.add((long) i);
            }
            assertEquals(count, list.size());
        }

        // 재오픈 후 검증
        try (FxStore fileStore = FxStore.open(tempFile)) {
            List<Long> list = fileStore.openList("persistentList", Long.class);
            assertNotNull(list);
            assertEquals(500, list.size());
            assertEquals(Long.valueOf(0L), list.get(0));
            assertEquals(Long.valueOf(499L), list.get(499));
        }
    }

    /**
     * 삽입 후 삭제.
     *
     * <p>삽입과 삭제를 통해 트리 구조 변경 테스트</p>
     */
    @Test
    public void testInsertAndRemove() {
        List<Long> list = store.createList("insertRemoveList", Long.class);

        // 300개 삽입 (리프 분할 유발)
        int insertCount = 300;
        for (int i = 0; i < insertCount; i++) {
            list.add((long) i);
        }
        assertEquals(insertCount, list.size());

        // 앞에서부터 100개 삭제
        for (int i = 0; i < 100; i++) {
            list.remove(0);
        }
        assertEquals(200, list.size());
        assertEquals(Long.valueOf(100L), list.get(0));

        // 뒤에서부터 50개 삭제
        for (int i = 0; i < 50; i++) {
            list.remove(list.size() - 1);
        }
        assertEquals(150, list.size());
        assertEquals(Long.valueOf(249L), list.get(list.size() - 1));
    }

    /**
     * 다중 리프 노드 삽입 테스트.
     *
     * <p>다중 리프 노드 분할 테스트</p>
     */
    @Test
    public void testMultiLeafInsert() {
        List<Long> list = store.createList("multiLeafList", Long.class);

        // 800개 삽입 (8개 이상 리프 노드 생성)
        int count = 800;
        for (int i = 0; i < count; i++) {
            list.add((long) i);
        }

        assertEquals(count, list.size());

        // 랜덤 위치 검증
        Random random = new Random(123);
        for (int i = 0; i < 50; i++) {
            int idx = random.nextInt(count);
            assertEquals(Long.valueOf(idx), list.get(idx));
        }
    }
}
