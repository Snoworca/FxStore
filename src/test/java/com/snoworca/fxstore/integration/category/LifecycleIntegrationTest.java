package com.snoworca.fxstore.integration.category;

import com.snoworca.fxstore.api.FxErrorCode;
import com.snoworca.fxstore.api.FxException;
import com.snoworca.fxstore.api.FxStore;
import com.snoworca.fxstore.integration.IntegrationTestBase;
import org.junit.Test;

import java.io.File;
import java.util.NavigableMap;
import java.util.NavigableSet;

import static org.junit.Assert.*;

/**
 * Category F: 파일 생명주기 테스트
 *
 * <p>목적: Store의 생성, 열기, 닫기, 압축 등 생명주기를 검증합니다.
 *
 * <p>위험 영역:
 * <ul>
 *   <li>파일 핸들 관리</li>
 *   <li>compactTo 무결성</li>
 *   <li>컬렉션 DDL 연산</li>
 * </ul>
 *
 * <p>테스트 케이스:
 * <ul>
 *   <li>F-1: open-close 반복</li>
 *   <li>F-2: compactTo 후 원본 독립성</li>
 *   <li>F-3: 여러 컬렉션 생성/삭제</li>
 *   <li>F-4: 대용량 파일 compactTo 성능</li>
 *   <li>F-5: 연속 compactTo</li>
 * </ul>
 */
public class LifecycleIntegrationTest extends IntegrationTestBase {

    /**
     * F-1: open-close 반복
     *
     * <p>목적: 반복적인 열기/닫기 시 리소스 누수 없음
     * <p>위험 영역: 파일 핸들 관리
     * <p>검증: 100회 반복 후에도 정상 동작
     */
    @Test
    public void test_F1_repeatedOpenClose_shouldNotLeak() throws Exception {
        for (int i = 0; i < 100; i++) {
            openStore();
            NavigableMap<Long, String> map = store.createOrOpenMap("repeatMap", Long.class, String.class);
            map.put((long) i, "value" + i);
            store.commit();
            closeStore();
        }

        // 최종 확인
        openStore();
        NavigableMap<Long, String> map = store.openMap("repeatMap", Long.class, String.class);
        assertEquals(100, map.size());

        // 랜덤 샘플 검증
        for (int i = 0; i < 100; i += 10) {
            assertEquals("value" + i, map.get((long) i));
        }

        // verify 통과
        assertTrue(store.verify().ok());
    }

    /**
     * F-2: compactTo 후 원본 독립성
     *
     * <p>목적: compactTo 후 원본과 복사본이 독립적
     * <p>위험 영역: 파일 복사 무결성
     * <p>검증: 원본 수정이 복사본에 영향 없음
     */
    @Test
    public void test_F2_compactTo_shouldBeIndependent() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("compactIndep", Long.class, String.class);
        map.put(1L, "original");
        store.commit();

        File targetFile = newTempFile("compact.fx");
        store.compactTo(targetFile.toPath());

        // 원본 수정
        map.put(1L, "modified");
        map.put(2L, "new");
        store.commit();

        // 복사본은 원래 값 유지
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableMap<Long, String> targetMap =
                    targetStore.openMap("compactIndep", Long.class, String.class);
            assertEquals("original", targetMap.get(1L));
            assertNull(targetMap.get(2L));
            assertEquals(1, targetMap.size());
        }

        // 원본은 수정된 값
        assertEquals("modified", map.get(1L));
        assertEquals("new", map.get(2L));
        assertEquals(2, map.size());
    }

    /**
     * F-3: 여러 컬렉션 생성/삭제
     *
     * <p>목적: 컬렉션 DDL 연산의 정확성
     * <p>위험 영역: Catalog 관리
     * <p>검증: 생성/삭제 후 상태 일관성
     */
    @Test
    public void test_F3_createDropCollections_shouldManageCorrectly() throws Exception {
        openStore();

        // 여러 컬렉션 생성
        for (int i = 0; i < 50; i++) {
            store.createMap("map" + i, Long.class, String.class);
        }
        store.commit();

        assertEquals(50, store.list().size());

        // 일부 삭제
        for (int i = 0; i < 25; i++) {
            assertTrue(store.drop("map" + i));
        }
        store.commit();

        // 재시작 후 확인
        reopenStore();
        assertEquals(25, store.list().size());

        // 삭제된 컬렉션 접근 시 예외
        try {
            store.openMap("map0", Long.class, String.class);
            fail("Should throw NOT_FOUND");
        } catch (FxException e) {
            assertEquals(FxErrorCode.NOT_FOUND, e.code());
        }

        // 남은 컬렉션 접근 가능
        NavigableMap<Long, String> map25 = store.openMap("map25", Long.class, String.class);
        assertNotNull(map25);
    }

    /**
     * F-4: 대용량 파일 compactTo 성능
     *
     * <p>목적: 큰 파일의 compactTo 시간 및 공간 효율
     * <p>위험 영역: compactTo 성능
     * <p>검증: 60초 이내 완료, 파일 크기 감소
     */
    @Test
    public void test_F4_largeFileCompact_shouldCompleteInTime() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("largeCompact", Long.class, String.class);

        // 대량 데이터
        for (int i = 0; i < 50_000; i++) {
            map.put((long) i, "value" + i);
        }
        store.commit();

        // 일부 삭제 (dead space 생성)
        for (int i = 0; i < 25_000; i++) {
            map.remove((long) i);
        }
        store.commit();

        long originalSize = storeFile.length();

        // compactTo 실행 시간 측정
        File targetFile = newTempFile("largeCompact.fx");

        long startTime = System.currentTimeMillis();
        store.compactTo(targetFile.toPath());
        long elapsed = System.currentTimeMillis() - startTime;

        // 60초 이내 완료
        assertTrue("CompactTo should complete within 60 seconds, took " + elapsed + "ms",
                elapsed < 60_000);

        // 파일 크기 감소 (dead space 제거)
        assertTrue("Compacted file should be smaller: original=" + originalSize +
                        ", compacted=" + targetFile.length(),
                targetFile.length() < originalSize);

        // 데이터 무결성
        try (FxStore targetStore = FxStore.open(targetFile.toPath())) {
            NavigableMap<Long, String> targetMap =
                    targetStore.openMap("largeCompact", Long.class, String.class);
            assertEquals(25_000, targetMap.size());
            assertTrue(targetStore.verify().ok());
        }
    }

    /**
     * F-5: 연속 compactTo
     *
     * <p>목적: 연속적인 compactTo 작업의 무결성
     * <p>위험 영역: 반복 압축
     * <p>검증: 데이터 무결성 유지
     */
    @Test
    public void test_F5_consecutiveCompactTo_shouldMaintainIntegrity() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("consCompact", Long.class, String.class);
        for (int i = 0; i < 1000; i++) {
            map.put((long) i, "v" + i);
        }
        store.commit();
        closeStore();

        // 연속 compactTo
        File current = storeFile;
        for (int round = 0; round < 5; round++) {
            File target = newTempFile("compact" + round + ".fx");

            try (FxStore sourceStore = FxStore.open(current.toPath())) {
                sourceStore.compactTo(target.toPath());
            }

            current = target;
        }

        // 최종 파일 무결성
        try (FxStore finalStore = FxStore.open(current.toPath())) {
            NavigableMap<Long, String> finalMap =
                    finalStore.openMap("consCompact", Long.class, String.class);
            assertEquals(1000, finalMap.size());
            assertEquals("v0", finalMap.get(0L));
            assertEquals("v999", finalMap.get(999L));
            assertTrue(finalStore.verify().ok());
        }
    }

    /**
     * 추가 테스트: 컬렉션 이름 변경
     */
    @Test
    public void test_F_renameCollection_shouldWork() throws Exception {
        openStore();
        NavigableMap<Long, String> map = store.createMap("oldName", Long.class, String.class);
        map.put(1L, "data");
        store.commit();

        // 이름 변경
        assertTrue(store.rename("oldName", "newName"));
        store.commit();

        // 이전 이름으로 접근 불가
        assertFalse(store.exists("oldName"));

        // 새 이름으로 접근 가능
        assertTrue(store.exists("newName"));
        NavigableMap<Long, String> renamedMap = store.openMap("newName", Long.class, String.class);
        assertEquals("data", renamedMap.get(1L));

        // 재시작 후 확인
        reopenStore();
        assertFalse(store.exists("oldName"));
        assertTrue(store.exists("newName"));
    }

    /**
     * 추가 테스트: 존재하지 않는 컬렉션 삭제
     */
    @Test
    public void test_F_dropNonExistent_shouldReturnFalseOrThrow() throws Exception {
        openStore();

        try {
            boolean result = store.drop("nonExistent");
            // drop이 false를 반환하는 경우도 허용
            assertFalse("Drop should return false for non-existent", result);
        } catch (FxException e) {
            // 예외를 던지는 경우도 허용
            assertEquals(FxErrorCode.NOT_FOUND, e.code());
        }
    }

    /**
     * 추가 테스트: 동일 이름 컬렉션 재생성
     */
    @Test
    public void test_F_recreateAfterDrop_shouldWork() throws Exception {
        openStore();

        // 생성
        NavigableMap<Long, String> map = store.createMap("recreate", Long.class, String.class);
        map.put(1L, "first");
        store.commit();

        // 삭제
        assertTrue(store.drop("recreate"));
        store.commit();

        // 재생성
        map = store.createMap("recreate", Long.class, String.class);
        assertTrue(map.isEmpty());
        map.put(2L, "second");
        store.commit();

        // 확인
        assertEquals(1, map.size());
        assertNull(map.get(1L));
        assertEquals("second", map.get(2L));

        // 재시작 후 확인
        reopenStore();
        map = store.openMap("recreate", Long.class, String.class);
        assertEquals(1, map.size());
        assertEquals("second", map.get(2L));
    }

    /**
     * 추가 테스트: 여러 타입 컬렉션 동시 생성
     */
    @Test
    public void test_F_multipleCollectionTypes_shouldCoexist() throws Exception {
        openStore();

        // 각 타입별 컬렉션 생성
        NavigableMap<Long, String> map = store.createMap("myMap", Long.class, String.class);
        NavigableSet<Long> set = store.createSet("mySet", Long.class);
        java.util.List<Long> list = store.createList("myList", Long.class);
        java.util.Deque<Long> deque = store.createDeque("myDeque", Long.class);

        // 데이터 삽입
        map.put(1L, "map");
        set.add(2L);
        list.add(3L);
        deque.addLast(4L);
        store.commit();

        // 모든 컬렉션 존재 확인
        assertEquals(4, store.list().size());

        // 재시작 후 확인
        reopenStore();
        assertEquals(4, store.list().size());

        map = store.openMap("myMap", Long.class, String.class);
        set = store.openSet("mySet", Long.class);
        list = store.openList("myList", Long.class);
        deque = store.openDeque("myDeque", Long.class);

        assertEquals("map", map.get(1L));
        assertTrue(set.contains(2L));
        assertEquals(Long.valueOf(3L), list.get(0));
        assertEquals(Long.valueOf(4L), deque.peekFirst());
    }
}
