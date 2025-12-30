package com.snoworca.fxstore.storage;

import com.snoworca.fxstore.api.FileLockMode;
import com.snoworca.fxstore.api.FxException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * FileStorage 에러 경로 및 경계 조건 테스트 (P1)
 *
 * <p>대상 클래스:</p>
 * <ul>
 *   <li>FileStorage (72% → 85%+)</li>
 * </ul>
 *
 * @since 0.9
 * @see FileStorage
 */
public class FileStorageErrorTest {

    private File tempFile;
    private Path tempPath;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-filestorage-error-", ".db").toFile();
        tempPath = tempFile.toPath();
    }

    @After
    public void tearDown() throws Exception {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    // ==================== close 후 접근 테스트 ====================

    @Test
    public void read_afterClose_shouldThrowFxException() throws Exception {
        // Given
        FileStorage storage = new FileStorage(tempPath, false);
        storage.close();

        // When & Then
        try {
            byte[] buffer = new byte[100];
            storage.read(0, buffer, 0, 100);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("closed"));
        }
    }

    @Test
    public void write_afterClose_shouldThrowFxException() throws Exception {
        // Given
        FileStorage storage = new FileStorage(tempPath, false);
        storage.close();

        // When & Then
        try {
            byte[] buffer = new byte[100];
            storage.write(0, buffer, 0, 100);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("closed"));
        }
    }

    @Test
    public void force_afterClose_shouldThrowFxException() throws Exception {
        // Given
        FileStorage storage = new FileStorage(tempPath, false);
        storage.close();

        // When & Then
        try {
            storage.force(true);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("closed"));
        }
    }

    @Test
    public void size_afterClose_shouldThrowFxException() throws Exception {
        // Given
        FileStorage storage = new FileStorage(tempPath, false);
        storage.close();

        // When & Then
        try {
            storage.size();
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("closed"));
        }
    }

    @Test
    public void extend_afterClose_shouldThrowFxException() throws Exception {
        // Given
        FileStorage storage = new FileStorage(tempPath, false);
        storage.close();

        // When & Then
        try {
            storage.extend(1000);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("closed"));
        }
    }

    // ==================== 중복 close 테스트 ====================

    @Test
    public void close_twice_shouldNotThrow() throws Exception {
        // Given
        FileStorage storage = new FileStorage(tempPath, false);

        // When: close twice
        storage.close();
        storage.close(); // should not throw

        // Then: no exception
    }

    // ==================== 파라미터 검증 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void read_nullBuffer_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            storage.read(0, null, 0, 100);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void read_negativeOffset_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.read(-1, buffer, 0, 100);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void read_negativeBufOffset_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.read(0, buffer, -1, 100);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void read_negativeLength_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.read(0, buffer, 0, -1);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void read_bufferOverflow_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.read(0, buffer, 50, 100); // 50 + 100 > 100
        }
    }

    @Test(expected = NullPointerException.class)
    public void write_nullBuffer_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            storage.write(0, null, 0, 100);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void write_negativeOffset_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.write(-1, buffer, 0, 100);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void extend_negativeSize_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            storage.extend(-100);
        }
    }

    // ==================== 정상 동작 테스트 (edge case) ====================

    @Test
    public void read_zeroLength_shouldSucceed() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.read(0, buffer, 0, 0); // no-op
        }
    }

    @Test
    public void write_zeroLength_shouldSucceed() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.write(0, buffer, 0, 0); // no-op
        }
    }

    @Test
    public void extend_sameSize_shouldSucceed() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            // Write some data first
            byte[] data = new byte[100];
            storage.write(0, data, 0, 100);

            long currentSize = storage.size();
            storage.extend(currentSize); // extend to same size - no change
            assertEquals(currentSize, storage.size());
        }
    }

    @Test
    public void extend_smallerSize_shouldNotShrink() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            // Write some data first
            byte[] data = new byte[1000];
            storage.write(0, data, 0, 1000);

            long originalSize = storage.size();
            storage.extend(500); // extend to smaller size - should not shrink
            assertEquals(originalSize, storage.size());
        }
    }

    // ==================== 읽기 전용 테스트 ====================

    @Test
    public void readOnly_read_shouldSucceed() throws Exception {
        // Given: 먼저 데이터를 쓰고
        try (FileStorage writeStorage = new FileStorage(tempPath, false)) {
            byte[] data = "Hello World".getBytes();
            writeStorage.write(0, data, 0, data.length);
        }

        // When: 읽기 전용으로 열기
        try (FileStorage readStorage = new FileStorage(tempPath, true)) {
            byte[] buffer = new byte[11];
            readStorage.read(0, buffer, 0, 11);

            // Then
            assertEquals("Hello World", new String(buffer));
        }
    }

    // ==================== 파일 잠금 테스트 ====================

    @Test
    public void fileLockMode_NONE_shouldWork() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false, FileLockMode.NONE)) {
            byte[] data = new byte[100];
            storage.write(0, data, 0, 100);
            assertEquals(100, storage.size());
        }
    }

    @Test
    public void fileLockMode_PROCESS_shouldWork() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false, FileLockMode.PROCESS)) {
            byte[] data = new byte[100];
            storage.write(0, data, 0, 100);
            assertEquals(100, storage.size());
        }
    }

    @Test
    public void fileLockMode_PROCESS_readOnly_shouldWork() throws Exception {
        // 먼저 파일 생성
        try (FileStorage writeStorage = new FileStorage(tempPath, false)) {
            byte[] data = new byte[100];
            writeStorage.write(0, data, 0, 100);
        }

        // 읽기 전용 + PROCESS 잠금
        try (FileStorage readStorage = new FileStorage(tempPath, true, FileLockMode.PROCESS)) {
            assertEquals(100, readStorage.size());
        }
    }

    @Test
    public void fileLockMode_duplicateLock_shouldThrow() throws Exception {
        // Given: 첫 번째 스토리지가 PROCESS 잠금을 획득
        FileStorage first = new FileStorage(tempPath, false, FileLockMode.PROCESS);

        try {
            // When: 두 번째 스토리지가 같은 파일에 PROCESS 잠금 시도
            try {
                FileStorage second = new FileStorage(tempPath, false, FileLockMode.PROCESS);
                second.close();
                fail("Expected FxException for duplicate lock");
            } catch (FxException e) {
                // Then: LOCK_FAILED 에러
                assertTrue(e.getMessage().toLowerCase().contains("lock"));
            }
        } finally {
            first.close();
        }
    }

    // ==================== null 생성자 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void constructor_nullPath_shouldThrow() {
        new FileStorage(null, false);
    }

    @Test
    public void constructor_nullLockMode_shouldDefaultToNone() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false, null)) {
            // Should work with default NONE lock mode
            assertEquals(0, storage.size());
        }
    }

    // ==================== getPath 테스트 ====================

    @Test
    public void getPath_shouldReturnOriginalPath() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            assertEquals(tempPath, storage.getPath());
        }
    }

    // ==================== force 테스트 ====================

    @Test
    public void force_withMetadata_shouldSucceed() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] data = new byte[100];
            storage.write(0, data, 0, 100);
            storage.force(true); // with metadata
        }
    }

    @Test
    public void force_withoutMetadata_shouldSucceed() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] data = new byte[100];
            storage.write(0, data, 0, 100);
            storage.force(false); // without metadata
        }
    }

    // ==================== 존재하지 않는 파일 테스트 ====================

    @Test
    public void open_nonExistentFile_readWrite_shouldCreate() throws Exception {
        // Given: 새 파일 경로 (존재하지 않음)
        File newFile = new File(tempFile.getParent(), "new-file-" + System.nanoTime() + ".db");
        Path newPath = newFile.toPath();

        try {
            // When: 읽기/쓰기 모드로 열기
            try (FileStorage storage = new FileStorage(newPath, false)) {
                // Then: 파일이 생성됨
                assertTrue(newFile.exists());
                assertEquals(0, storage.size());
            }
        } finally {
            newFile.delete();
        }
    }

    @Test
    public void open_nonExistentFile_readOnly_shouldThrow() {
        // Given: 존재하지 않는 파일
        Path nonExistent = tempFile.toPath().resolveSibling("non-existent-" + System.nanoTime() + ".db");

        // When & Then
        try {
            new FileStorage(nonExistent, true);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("open") ||
                       e.getMessage().toLowerCase().contains("failed"));
        }
    }

    // ==================== truncate 테스트 ====================

    @Test
    public void truncate_afterClose_shouldThrowFxException() throws Exception {
        // Given
        FileStorage storage = new FileStorage(tempPath, false);
        storage.close();

        // When & Then
        try {
            storage.truncate(100);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue(e.getMessage().toLowerCase().contains("closed"));
        }
    }

    @Test
    public void truncate_normal_shouldSucceed() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            // Write some data first
            byte[] data = new byte[1000];
            storage.write(0, data, 0, 1000);
            assertEquals(1000, storage.size());

            // Truncate to smaller size
            storage.truncate(500);
            assertEquals(500, storage.size());
        }
    }

    @Test
    public void truncate_readOnly_shouldThrowFxException() throws Exception {
        // 먼저 파일 생성
        try (FileStorage writeStorage = new FileStorage(tempPath, false)) {
            byte[] data = new byte[1000];
            writeStorage.write(0, data, 0, 1000);
        }

        // 읽기 전용으로 열기
        try (FileStorage readStorage = new FileStorage(tempPath, true)) {
            try {
                readStorage.truncate(500);
                fail("Expected FxException");
            } catch (FxException e) {
                assertTrue(e.getMessage().toLowerCase().contains("read-only"));
            }
        }
    }

    // ==================== isReadOnly 테스트 ====================

    @Test
    public void isReadOnly_false_shouldReturnFalse() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            assertFalse(storage.isReadOnly());
        }
    }

    @Test
    public void isReadOnly_true_shouldReturnTrue() throws Exception {
        // 먼저 파일 생성
        try (FileStorage writeStorage = new FileStorage(tempPath, false)) {
            byte[] data = new byte[100];
            writeStorage.write(0, data, 0, 100);
        }

        // 읽기 전용으로 열기
        try (FileStorage readStorage = new FileStorage(tempPath, true)) {
            assertTrue(readStorage.isReadOnly());
        }
    }

    // ==================== write 파라미터 검증 추가 ====================

    @Test(expected = IllegalArgumentException.class)
    public void write_negativeBufOffset_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.write(0, buffer, -1, 100);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void write_negativeLength_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.write(0, buffer, 0, -1);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void write_bufferOverflow_shouldThrow() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath, false)) {
            byte[] buffer = new byte[100];
            storage.write(0, buffer, 50, 100); // 50 + 100 > 100
        }
    }

    @Test
    public void write_readOnly_shouldThrowFxException() throws Exception {
        // 먼저 파일 생성
        try (FileStorage writeStorage = new FileStorage(tempPath, false)) {
            byte[] data = new byte[100];
            writeStorage.write(0, data, 0, 100);
        }

        // 읽기 전용으로 열기
        try (FileStorage readStorage = new FileStorage(tempPath, true)) {
            try {
                byte[] buffer = new byte[100];
                readStorage.write(0, buffer, 0, 100);
                fail("Expected FxException");
            } catch (FxException e) {
                assertTrue(e.getMessage().toLowerCase().contains("read-only"));
            }
        }
    }

    @Test
    public void extend_readOnly_shouldThrowFxException() throws Exception {
        // 먼저 파일 생성
        try (FileStorage writeStorage = new FileStorage(tempPath, false)) {
            byte[] data = new byte[100];
            writeStorage.write(0, data, 0, 100);
        }

        // 읽기 전용으로 열기
        try (FileStorage readStorage = new FileStorage(tempPath, true)) {
            try {
                readStorage.extend(1000);
                fail("Expected FxException");
            } catch (FxException e) {
                assertTrue(e.getMessage().toLowerCase().contains("read-only"));
            }
        }
    }

    // ==================== 단일 인자 생성자 테스트 ====================

    @Test
    public void constructor_singleArg_shouldUseDefaults() throws Exception {
        try (FileStorage storage = new FileStorage(tempPath)) {
            assertFalse(storage.isReadOnly());
            assertEquals(tempPath, storage.getPath());
        }
    }
}
