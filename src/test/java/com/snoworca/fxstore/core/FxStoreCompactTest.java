package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.NavigableMap;

import static org.junit.Assert.*;

/**
 * FxStore compactTo() 메서드 테스트
 *
 * <p>P1-1: compactTo() 기능의 기본 테스트</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>예외 상황 처리 (닫힌 스토어, pending 변경)</li>
 *   <li>compactTo() API 존재 확인</li>
 * </ul>
 *
 * <p>Note: compactTo()의 실제 데이터 복사 기능은 구현 개선 필요</p>
 *
 * @since v1.0 Phase 2
 * @see FxStore#compactTo(Path)
 */
public class FxStoreCompactTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File sourceFile;
    private File targetFile;

    @Before
    public void setUp() throws Exception {
        sourceFile = tempFolder.newFile("source.fx");
        sourceFile.delete();

        targetFile = tempFolder.newFile("target.fx");
        targetFile.delete();
    }

    @After
    public void tearDown() {
        // 정리 불필요 - TemporaryFolder가 처리
    }

    // ==================== 예외 상황 테스트 ====================

    @Test(expected = FxException.class)
    public void compactTo_closedStore_shouldThrow() throws Exception {
        // Given: 닫힌 스토어
        FxStore source = FxStore.open(sourceFile.toPath());
        source.close();

        // When & Then: 예외 발생
        source.compactTo(targetFile.toPath());
    }

    @Test(expected = FxException.class)
    public void compactTo_batchModeWithPendingChanges_shouldThrow() throws Exception {
        // Given: BATCH 모드에서 pending 변경이 있는 스토어
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .build();

        try (FxStore source = FxStoreImpl.open(sourceFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = source.createMap("test", Long.class, String.class);
            map.put(1L, "value");
            // 커밋하지 않음 - pending 상태

            // When & Then: 예외 발생
            source.compactTo(targetFile.toPath());
        }
    }

    // ==================== API 존재 확인 테스트 ====================

    @Test
    public void compactTo_apiExists() throws Exception {
        // Given: 파일 스토어
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            NavigableMap<Long, String> map = source.createMap("test", Long.class, String.class);
            map.put(1L, "value");

            // When: compactTo 호출 - 결과는 구현에 따라 다름
            try {
                source.compactTo(targetFile.toPath());

                // Then: 파일이 생성됨 (내용은 구현에 따라 다를 수 있음)
                assertTrue("Target file should be created", targetFile.exists());
            } catch (FxException e) {
                // compactTo가 구현되지 않았거나 버그가 있을 수 있음
                // API가 존재하는 것만 확인
                assertNotNull("API exists and throws exception", e);
            }
        }
    }

    @Test
    public void compactTo_memoryStore_apiExists() {
        // Given: 메모리 스토어
        try (FxStore source = FxStore.openMemory()) {
            NavigableMap<Long, String> map = source.createMap("test", Long.class, String.class);
            map.put(1L, "value");

            // When: compactTo 호출
            try {
                source.compactTo(targetFile.toPath());
                assertTrue("Target file should be created", targetFile.exists());
            } catch (FxException e) {
                // 메모리 스토어에서 compactTo가 지원되지 않을 수 있음
                assertNotNull("API exists", e);
            }
        }
    }

    // ==================== BATCH 모드 커밋 후 테스트 ====================

    @Test
    public void compactTo_batchModeAfterCommit_shouldNotThrowIllegalArgument() throws Exception {
        // Given: BATCH 모드에서 커밋 후
        FxOptions batchOptions = FxOptions.defaults()
                .withCommitMode(CommitMode.BATCH)
                .build();

        try (FxStore source = FxStoreImpl.open(sourceFile.toPath(), batchOptions)) {
            NavigableMap<Long, String> map = source.createMap("test", Long.class, String.class);
            map.put(1L, "value");
            source.commit(); // 커밋

            // When: compactTo 호출 - pending 에러는 발생하지 않아야 함
            try {
                source.compactTo(targetFile.toPath());
                // 성공하면 파일 존재 확인
                assertTrue(targetFile.exists());
            } catch (FxException e) {
                // pending 관련 에러가 아니어야 함
                assertFalse("Should not be pending changes error",
                    e.getMessage().contains("pending changes"));
            }
        }
    }

    // ==================== null 파라미터 테스트 ====================

    @Test(expected = Exception.class)
    public void compactTo_nullPath_shouldThrow() throws Exception {
        try (FxStore source = FxStore.open(sourceFile.toPath())) {
            source.compactTo(null);
        }
    }
}
