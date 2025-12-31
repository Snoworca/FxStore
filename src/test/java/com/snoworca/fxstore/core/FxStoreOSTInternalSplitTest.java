package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * OST splitInternalNode 경로 테스트
 *
 * <p>V19 커버리지 개선: OST 내부 노드 분할을 유발하는 극한 테스트</p>
 */
public class FxStoreOSTInternalSplitTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("test.fx");
        storeFile.delete();
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== OST 내부 노드 분할 테스트 ====================

    @Test
    public void list_massiveInsert_shouldTriggerInternalNodeSplit() throws Exception {
        // Given: 매우 많은 데이터를 삽입하여 OST 내부 노드 분할 유발
        store = FxStore.open(storeFile.toPath());
        List<Long> list = store.createList("massiveList", Long.class);

        // 50,000개 요소 삽입 - 내부 노드 분할 유발
        for (int i = 0; i < 50000; i++) {
            list.add((long) i);
        }
        store.commit();

        // Then: 데이터 무결성 확인
        assertEquals(50000, list.size());
        assertEquals(Long.valueOf(0L), list.get(0));
        assertEquals(Long.valueOf(49999L), list.get(49999));

        // 중간 인덱스 접근 테스트
        assertEquals(Long.valueOf(25000L), list.get(25000));
    }

    @Test
    public void list_randomInsert_shouldTriggerMultipleSplits() throws Exception {
        // Given: 랜덤 위치에 삽입하여 다양한 분할 경로 테스트
        store = FxStore.open(storeFile.toPath());
        List<Long> list = store.createList("randomList", Long.class);

        // 초기 데이터
        for (int i = 0; i < 1000; i++) {
            list.add((long) i);
        }

        // 랜덤 위치에 삽입
        Random random = new Random(42);
        for (int i = 0; i < 5000; i++) {
            int pos = random.nextInt(list.size() + 1);
            list.add(pos, (long) (10000 + i));
        }
        store.commit();

        // Then
        assertEquals(6000, list.size());
    }

    @Test
    public void list_randomRemove_shouldTriggerRebalancing() throws Exception {
        // Given: 대량 데이터 후 삭제로 리밸런싱 유발
        store = FxStore.open(storeFile.toPath());
        List<Long> list = store.createList("removeList", Long.class);

        // 대량 삽입
        for (int i = 0; i < 10000; i++) {
            list.add((long) i);
        }
        store.commit();

        // 랜덤 삭제
        Random random = new Random(42);
        for (int i = 0; i < 5000; i++) {
            if (!list.isEmpty()) {
                int pos = random.nextInt(list.size());
                list.remove(pos);
            }
        }
        store.commit();

        // Then
        assertEquals(5000, list.size());
    }

    @Test
    public void list_deepTree_shouldTraverseCorrectly() throws Exception {
        // Given: 매우 깊은 트리 생성
        store = FxStore.open(storeFile.toPath());
        List<String> list = store.createList("deepList", String.class);

        // 큰 요소로 깊은 트리 유발
        for (int i = 0; i < 20000; i++) {
            // 큰 문자열로 페이지 분할 빠르게 유발
            list.add("element-" + i + "-" + String.format("%0200d", i));
        }
        store.commit();

        // When: DEEP 통계
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertEquals(1, stats.collectionCount());
        assertTrue(stats.fileBytes() > 0);

        // 데이터 무결성
        assertEquals(20000, list.size());
    }

    @Test
    public void list_addRemovePattern_shouldMaintainIntegrity() throws Exception {
        // Given: 추가/삭제 패턴으로 트리 구조 변경
        store = FxStore.open(storeFile.toPath());
        List<Long> list = store.createList("patternList", Long.class);

        // 패턴: 많이 추가 -> 일부 삭제 -> 반복
        for (int round = 0; round < 10; round++) {
            // 추가
            for (int i = 0; i < 1000; i++) {
                list.add((long) (round * 1000 + i));
            }
            store.commit();

            // 일부 삭제
            for (int i = 0; i < 300 && !list.isEmpty(); i++) {
                list.remove(0);
            }
            store.commit();
        }

        // Then
        assertEquals(7000, list.size());
    }

    // ==================== Deque 극한 테스트 ====================

    @Test
    public void deque_massiveOperations_shouldWork() throws Exception {
        // Given: 대량 Deque 연산
        store = FxStore.open(storeFile.toPath());
        Deque<Long> deque = store.createDeque("massiveDeque", Long.class);

        // 앞뒤로 번갈아 삽입
        for (int i = 0; i < 10000; i++) {
            if (i % 2 == 0) {
                deque.addFirst((long) i);
            } else {
                deque.addLast((long) i);
            }
        }
        store.commit();

        // Then
        assertEquals(10000, deque.size());
    }

    @Test
    public void deque_pollPattern_shouldMaintainOrder() throws Exception {
        // Given: 대량 삽입 후 폴링
        store = FxStore.open(storeFile.toPath());
        Deque<Long> deque = store.createDeque("pollDeque", Long.class);

        for (int i = 0; i < 5000; i++) {
            deque.addLast((long) i);
        }
        store.commit();

        // 앞에서 절반 폴링
        for (int i = 0; i < 2500; i++) {
            Long value = deque.pollFirst();
            assertEquals(Long.valueOf(i), value);
        }
        store.commit();

        // Then
        assertEquals(2500, deque.size());
        assertEquals(Long.valueOf(2500L), deque.peekFirst());
    }

    // ==================== Map 극한 테스트 ====================

    @Test
    public void map_largeKeyRange_shouldWork() throws Exception {
        // Given: 넓은 키 범위의 맵
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("largeKeyMap", Long.class, String.class);

        // 넓은 범위의 키
        for (int i = 0; i < 10000; i++) {
            long key = (long) i * 1000000L; // 큰 간격의 키
            map.put(key, "value" + i);
        }
        store.commit();

        // Then
        assertEquals(10000, map.size());
        assertEquals("value0", map.get(0L));
        assertEquals("value9999", map.get(9999L * 1000000L));
    }

    @Test
    public void map_deleteHalf_shouldRebalance() throws Exception {
        // Given: 대량 삽입 후 절반 삭제
        store = FxStore.open(storeFile.toPath());
        NavigableMap<Long, String> map = store.createMap("deleteHalfMap", Long.class, String.class);

        for (int i = 0; i < 10000; i++) {
            map.put((long) i, "value" + i);
        }
        store.commit();

        // 홀수 키 삭제
        for (int i = 1; i < 10000; i += 2) {
            map.remove((long) i);
        }
        store.commit();

        // Then
        assertEquals(5000, map.size());
        assertTrue(map.containsKey(0L));
        assertTrue(map.containsKey(9998L));
        assertFalse(map.containsKey(1L));
    }

    // ==================== Set 극한 테스트 ====================

    @Test
    public void set_massiveElements_shouldWork() throws Exception {
        // Given: 대량 Set
        store = FxStore.open(storeFile.toPath());
        NavigableSet<Long> set = store.createSet("massiveSet", Long.class);

        for (int i = 0; i < 30000; i++) {
            set.add((long) i);
        }
        store.commit();

        // Then
        assertEquals(30000, set.size());
        assertTrue(set.contains(0L));
        assertTrue(set.contains(29999L));
    }

    // ==================== 복합 테스트 ====================

    @Test
    public void multipleCollections_deepStats_shouldCalculateAll() throws Exception {
        // Given: 여러 대용량 컬렉션
        store = FxStore.open(storeFile.toPath());

        List<Long> list = store.createList("bigList", Long.class);
        for (int i = 0; i < 5000; i++) {
            list.add((long) i);
        }

        NavigableMap<Long, String> map = store.createMap("bigMap", Long.class, String.class);
        for (int i = 0; i < 5000; i++) {
            map.put((long) i, "v" + i);
        }

        NavigableSet<Long> set = store.createSet("bigSet", Long.class);
        for (int i = 0; i < 5000; i++) {
            set.add((long) i);
        }

        Deque<Long> deque = store.createDeque("bigDeque", Long.class);
        for (int i = 0; i < 5000; i++) {
            deque.addLast((long) i);
        }

        store.commit();

        // When: DEEP 통계
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertEquals(4, stats.collectionCount());
        assertTrue(stats.fileBytes() > 0);
    }

    @Test
    public void compactTo_withMassiveData_shouldPreserveAll() throws Exception {
        // Given: 대량 데이터
        store = FxStore.open(storeFile.toPath());
        List<Long> list = store.createList("compactList", Long.class);
        for (int i = 0; i < 10000; i++) {
            list.add((long) i);
        }
        store.commit();

        // When: compactTo
        File targetFile = tempFolder.newFile("compact.fx");
        targetFile.delete();
        store.compactTo(targetFile.toPath());

        // Then: 데이터 보존 확인
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            List<Long> targetList = targetStore.openList("compactList", Long.class);
            assertEquals(10000, targetList.size());
            assertEquals(Long.valueOf(0L), targetList.get(0));
            assertEquals(Long.valueOf(9999L), targetList.get(9999));
        }
    }

    @Test
    public void verify_afterMassiveOperations_shouldPass() throws Exception {
        // Given: 대량 연산
        store = FxStore.open(storeFile.toPath());
        List<Long> list = store.createList("verifyList", Long.class);

        for (int i = 0; i < 20000; i++) {
            list.add((long) i);
        }
        store.commit();

        // 일부 삭제
        for (int i = 0; i < 5000; i++) {
            list.remove(0);
        }
        store.commit();

        // Then: verify 통과
        VerifyResult result = store.verify();
        assertTrue(result.ok());
    }
}
