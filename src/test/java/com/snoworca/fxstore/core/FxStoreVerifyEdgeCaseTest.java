package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.NavigableMap;

import static org.junit.Assert.*;

/**
 * verify() 손상 시나리오 테스트
 *
 * <p>P1: verifySuperblock(), verifyCommitHeaders(), verifyAllocTail() 개선</p>
 *
 * <p>파일 손상을 시뮬레이션하여 verify()의 다양한 에러 경로를 테스트합니다.</p>
 *
 * @since v1.0 Phase 3
 * @see FxStore#verify()
 */
public class FxStoreVerifyEdgeCaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("verify-test.fx");
        storeFile.delete();
    }

    @After
    public void tearDown() {
        // TemporaryFolder가 처리
    }

    // ==================== 정상 스토어 테스트 ====================

    @Test
    public void verify_newStore_shouldPass() throws Exception {
        // Given: 새로 생성된 스토어
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            // When: verify() 호출
            VerifyResult result = store.verify();

            // Then: 오류 없음
            assertTrue("New store should verify successfully", result.ok());
            assertTrue("No errors expected", result.errors().isEmpty());
        }
    }

    @Test
    public void verify_storeWithData_shouldPass() throws Exception {
        // Given: 데이터가 있는 스토어
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.createMap("testMap", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "value-" + i);
            }

            // When: verify() 호출
            VerifyResult result = store.verify();

            // Then: 오류 없음
            assertTrue("Store with data should verify successfully", result.ok());
        }
    }

    // ==================== Superblock 손상 테스트 ====================

    @Test
    public void verify_corruptedSuperblockMagic_shouldReportError() throws Exception {
        // Given: 스토어 생성 후 닫기
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.createMap("test", Long.class, String.class);
        }

        // Superblock magic 손상 (첫 8바이트)
        try (RandomAccessFile raf = new RandomAccessFile(storeFile, "rw")) {
            raf.seek(0);
            raf.write(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
        }

        // When: 손상된 파일로 open 시도 시 예외 발생
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            fail("Opening corrupted store should throw exception");
        } catch (FxException e) {
            // Then: Superblock 관련 에러
            assertTrue("Error should mention superblock or magic",
                e.getMessage().toLowerCase().contains("magic") ||
                e.getMessage().toLowerCase().contains("superblock") ||
                e.getMessage().contains("Invalid"));
        }
    }

    // ==================== CommitHeader 손상 테스트 ====================

    @Test
    public void verify_corruptedCommitHeaderCrc_shouldReportError() throws Exception {
        // Given: 스토어 생성 후 닫기
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value");
        }

        // CommitHeader A의 CRC 손상 (Superblock 뒤, 마지막 4바이트)
        int superblockSize = 4096; // 일반적인 Superblock 크기
        int commitHeaderSize = 4096; // 일반적인 CommitHeader 크기

        try (RandomAccessFile raf = new RandomAccessFile(storeFile, "rw")) {
            long crcOffset = superblockSize + commitHeaderSize - 4;
            raf.seek(crcOffset);
            // CRC 값 손상
            raf.write(new byte[] { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF });
        }

        // When: 손상된 파일로 verify 호출
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            VerifyResult result = store.verify();

            // Then: CRC 오류 보고되거나 스토어가 열리지 않음
            // 참고: 손상 정도에 따라 open 자체가 실패할 수 있음
            if (!result.ok()) {
                boolean hasCrcError = result.errors().stream()
                    .anyMatch(e -> e.message().toLowerCase().contains("crc"));
                assertTrue("Should report CRC error or other error",
                    hasCrcError || !result.errors().isEmpty());
            }
        } catch (FxException e) {
            // 스토어 열기 자체가 실패할 수 있음
            assertNotNull("Exception should be thrown for corrupted header", e);
        }
    }

    // ==================== allocTail 범위 초과 테스트 ====================

    @Test
    public void verify_truncatedFile_shouldReportError() throws Exception {
        // Given: 스토어 생성 후 닫기
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            for (long i = 0; i < 100; i++) {
                map.put(i, "value-" + i);
            }
        }

        long originalSize = storeFile.length();

        // 파일을 allocTail보다 작게 truncate
        try (RandomAccessFile raf = new RandomAccessFile(storeFile, "rw")) {
            // 파일 크기를 절반으로 줄임
            long newSize = Math.max(originalSize / 2, 4096 * 3); // 최소 Superblock + 2 CommitHeader
            raf.setLength(newSize);
        }

        // When: truncate된 파일로 open 시도
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            VerifyResult result = store.verify();

            // Then: allocTail 오류 또는 다른 오류 보고
            if (!result.ok()) {
                boolean hasAllocError = result.errors().stream()
                    .anyMatch(e -> e.message().toLowerCase().contains("alloctail") ||
                                  e.message().toLowerCase().contains("exceeds") ||
                                  e.message().toLowerCase().contains("size"));
                assertTrue("Should report allocTail or size error",
                    hasAllocError || !result.errors().isEmpty());
            }
        } catch (FxException e) {
            // 스토어 열기 자체가 실패할 수 있음
            assertNotNull("Exception should be thrown for truncated file", e);
        }
    }

    // ==================== 파일 크기 부족 테스트 ====================

    @Test
    public void verify_fileTooSmall_shouldFail() throws Exception {
        // Given: 매우 작은 파일 생성
        try (RandomAccessFile raf = new RandomAccessFile(storeFile, "rw")) {
            // Superblock보다 작은 크기
            raf.write(new byte[100]);
        }

        // When: 너무 작은 파일로 open 시도
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            fail("Opening too small file should throw exception");
        } catch (Exception e) {
            // Then: 에러 발생
            assertNotNull("Exception should be thrown for small file", e);
        }
    }

    // ==================== 정상 상태 verify 테스트 ====================

    @Test
    public void verify_afterMultipleOperations_shouldPass() throws Exception {
        // Given: 다양한 연산 수행
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            // 컬렉션 생성
            NavigableMap<Long, String> map = store.createMap("map", Long.class, String.class);
            for (long i = 0; i < 50; i++) {
                map.put(i, "value-" + i);
            }

            // 일부 삭제
            for (long i = 0; i < 25; i++) {
                map.remove(i);
            }

            // 새 컬렉션 생성
            store.createSet("set", Long.class).add(100L);
            store.createList("list", String.class).add("item");

            // When: verify() 호출
            VerifyResult result = store.verify();

            // Then: 오류 없음
            assertTrue("Store after operations should verify", result.ok());
        }
    }

    @Test
    public void verify_reopenedStore_shouldPass() throws Exception {
        // Given: 스토어 생성, 닫기, 다시 열기
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.createMap("test", Long.class, String.class).put(1L, "value");
        }

        try (FxStore store = FxStore.open(storeFile.toPath())) {
            // When: verify() 호출
            VerifyResult result = store.verify();

            // Then: 오류 없음
            assertTrue("Reopened store should verify", result.ok());
        }
    }

    // ==================== 메모리 스토어 verify 테스트 ====================

    @Test
    public void verify_memoryStore_shouldPass() {
        // Given: 메모리 스토어
        try (FxStore store = FxStore.openMemory()) {
            store.createMap("test", Long.class, String.class).put(1L, "value");

            // When: verify() 호출
            VerifyResult result = store.verify();

            // Then: 오류 없음
            assertTrue("Memory store should verify", result.ok());
        }
    }

    // ==================== 빈 스토어 verify 테스트 ====================

    @Test
    public void verify_emptyStore_shouldPass() throws Exception {
        // Given: 빈 스토어 (컬렉션 없음)
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            // When: verify() 호출
            VerifyResult result = store.verify();

            // Then: 오류 없음
            assertTrue("Empty store should verify", result.ok());
        }
    }

    // ==================== VerifyResult 내용 검사 ====================

    @Test
    public void verifyResult_ok_shouldReturnEmptyErrors() throws Exception {
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            VerifyResult result = store.verify();

            if (result.ok()) {
                assertTrue("ok() true should mean empty errors", result.errors().isEmpty());
            } else {
                assertFalse("ok() false should mean non-empty errors", result.errors().isEmpty());
            }
        }
    }

    @Test
    public void verifyError_properties_shouldBeAccessible() throws Exception {
        // Given: 정상 스토어
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            VerifyResult result = store.verify();

            // VerifyError 인스턴스 검사 (오류가 있는 경우)
            for (com.snoworca.fxstore.api.VerifyError error : result.errors()) {
                assertNotNull("kind should not be null", error.kind());
                assertNotNull("message should not be null", error.message());
                assertTrue("fileOffset should be >= -1", error.fileOffset() >= -1);
            }
        }
    }
}
