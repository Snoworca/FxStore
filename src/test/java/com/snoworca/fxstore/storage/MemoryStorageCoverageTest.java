package com.snoworca.fxstore.storage;

import com.snoworca.fxstore.api.FxException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * MemoryStorage 미커버 메서드 테스트
 *
 * <p>커버리지 개선 대상:</p>
 * <ul>
 *   <li>truncate(long)</li>
 *   <li>toByteArray()</li>
 *   <li>force(boolean)</li>
 *   <li>read 일부 브랜치</li>
 *   <li>checkClosed after close</li>
 * </ul>
 */
public class MemoryStorageCoverageTest {

    // ==================== truncate 테스트 ====================

    @Test
    public void truncate_shouldReduceSize() {
        // Given: 데이터가 있는 스토리지
        MemoryStorage storage = new MemoryStorage();
        byte[] data = new byte[1000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        storage.write(0, data, 0, data.length);
        assertEquals(1000, storage.size());

        // When: truncate
        storage.truncate(500);

        // Then: 크기 감소
        assertEquals(500, storage.size());
    }

    @Test
    public void truncate_largerSize_shouldNotChange() {
        // Given: 크기가 500인 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.write(0, new byte[500], 0, 500);
        assertEquals(500, storage.size());

        // When: 더 큰 값으로 truncate
        storage.truncate(1000);

        // Then: 크기 변경 없음
        assertEquals(500, storage.size());
    }

    // ==================== toByteArray 테스트 ====================

    @Test
    public void toByteArray_shouldReturnDataCopy() {
        // Given: 데이터가 있는 스토리지
        MemoryStorage storage = new MemoryStorage();
        byte[] original = {1, 2, 3, 4, 5};
        storage.write(0, original, 0, original.length);

        // When: toByteArray
        byte[] result = storage.toByteArray();

        // Then: 데이터 복사본 반환
        assertEquals(5, result.length);
        assertArrayEquals(original, result);

        // 독립적인 복사본
        result[0] = 99;
        byte[] readBack = new byte[5];
        storage.read(0, readBack, 0, 5);
        assertEquals(1, readBack[0]); // 원본 변경 안됨
    }

    // ==================== force 테스트 ====================

    @Test
    public void force_shouldNotThrow() {
        // Given: 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.write(0, new byte[100], 0, 100);

        // When/Then: force 호출 (예외 없음)
        storage.force(true);
        storage.force(false);
    }

    // ==================== checkClosed 테스트 ====================

    @Test(expected = FxException.class)
    public void read_afterClose_shouldThrow() {
        // Given: 닫힌 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.write(0, new byte[100], 0, 100);
        storage.close();

        // When: read 시도
        storage.read(0, new byte[10], 0, 10);

        // Then: FxException 발생
    }

    @Test(expected = FxException.class)
    public void write_afterClose_shouldThrow() {
        // Given: 닫힌 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.close();

        // When: write 시도
        storage.write(0, new byte[10], 0, 10);

        // Then: FxException 발생
    }

    @Test(expected = FxException.class)
    public void size_afterClose_shouldThrow() {
        // Given: 닫힌 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.close();

        // When: size 시도
        storage.size();

        // Then: FxException 발생
    }

    @Test(expected = FxException.class)
    public void extend_afterClose_shouldThrow() {
        // Given: 닫힌 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.close();

        // When: extend 시도
        storage.extend(1000);

        // Then: FxException 발생
    }

    @Test(expected = FxException.class)
    public void truncate_afterClose_shouldThrow() {
        // Given: 닫힌 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.write(0, new byte[100], 0, 100);
        storage.close();

        // When: truncate 시도
        storage.truncate(50);

        // Then: FxException 발생
    }

    @Test(expected = FxException.class)
    public void force_afterClose_shouldThrow() {
        // Given: 닫힌 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.close();

        // When: force 시도
        storage.force(true);

        // Then: FxException 발생
    }

    @Test(expected = FxException.class)
    public void toByteArray_afterClose_shouldThrow() {
        // Given: 닫힌 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.write(0, new byte[100], 0, 100);
        storage.close();

        // When: toByteArray 시도
        storage.toByteArray();

        // Then: FxException 발생
    }

    // ==================== read 브랜치 테스트 ====================

    @Test(expected = FxException.class)
    public void read_beyondSize_shouldThrow() {
        // Given: 크기가 100인 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.write(0, new byte[100], 0, 100);

        // When: 범위를 벗어난 read
        storage.read(50, new byte[100], 0, 100); // 50+100 > 100

        // Then: FxException 발생
    }

    @Test(expected = FxException.class)
    public void read_negativeOffset_shouldThrow() {
        // Given: 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.write(0, new byte[100], 0, 100);

        // When: 음수 오프셋
        storage.read(-1, new byte[10], 0, 10);

        // Then: FxException 발생
    }

    // ==================== ensureCapacity 브랜치 테스트 ====================

    @Test(expected = FxException.class)
    public void write_exceedsMemoryLimit_shouldThrow() {
        // Given: 메모리 제한이 있는 스토리지
        MemoryStorage storage = new MemoryStorage(1000);

        // When: 제한 초과 write
        storage.write(0, new byte[2000], 0, 2000);

        // Then: FxException 발생
    }

    @Test
    public void write_shouldExpandCapacity() {
        // Given: 스토리지
        MemoryStorage storage = new MemoryStorage();

        // When: 큰 데이터 write (자동 확장)
        byte[] data = new byte[10000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        storage.write(0, data, 0, data.length);

        // Then: 성공
        assertEquals(10000, storage.size());

        // 데이터 검증
        byte[] readBack = new byte[10000];
        storage.read(0, readBack, 0, 10000);
        assertArrayEquals(data, readBack);
    }

    // ==================== extend 브랜치 테스트 ====================

    @Test
    public void extend_smallerThanSize_shouldNotChange() {
        // Given: 크기가 500인 스토리지
        MemoryStorage storage = new MemoryStorage();
        storage.write(0, new byte[500], 0, 500);

        // When: 더 작은 값으로 extend
        storage.extend(100);

        // Then: 크기 변경 없음 (extend는 축소 안함)
        assertEquals(500, storage.size());
    }

    @Test
    public void extend_shouldIncreaseSize() {
        // Given: 빈 스토리지
        MemoryStorage storage = new MemoryStorage();
        assertEquals(0, storage.size());

        // When: extend
        storage.extend(1000);

        // Then: 크기 증가
        assertEquals(1000, storage.size());
    }

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_default_shouldHaveMaxLimit() {
        // Given: 기본 생성자
        MemoryStorage storage = new MemoryStorage();

        // When: 큰 데이터 write
        byte[] data = new byte[100000];
        storage.write(0, data, 0, data.length);

        // Then: 성공
        assertEquals(100000, storage.size());
    }

    @Test
    public void constructor_withLimit_shouldEnforceLimit() {
        // Given: 제한이 있는 생성자
        MemoryStorage storage = new MemoryStorage(500);

        // When: 제한 내 write
        storage.write(0, new byte[400], 0, 400);

        // Then: 성공
        assertEquals(400, storage.size());
    }

    // ==================== 정상 read 테스트 ====================

    @Test
    public void read_withinBounds_shouldSucceed() {
        // Given: 데이터가 있는 스토리지
        MemoryStorage storage = new MemoryStorage();
        byte[] data = {10, 20, 30, 40, 50};
        storage.write(0, data, 0, data.length);

        // When: 정상 read
        byte[] buffer = new byte[3];
        storage.read(1, buffer, 0, 3);

        // Then: 올바른 데이터
        assertArrayEquals(new byte[]{20, 30, 40}, buffer);
    }

    @Test
    public void read_exactBoundary_shouldSucceed() {
        // Given: 데이터가 있는 스토리지
        MemoryStorage storage = new MemoryStorage();
        byte[] data = new byte[100];
        storage.write(0, data, 0, data.length);

        // When: 정확한 경계 read
        byte[] buffer = new byte[100];
        storage.read(0, buffer, 0, 100);

        // Then: 성공
        assertEquals(100, buffer.length);
    }
}
