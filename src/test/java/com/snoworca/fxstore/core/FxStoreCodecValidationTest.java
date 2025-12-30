package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.NavigableMap;

import static org.junit.Assert.*;

/**
 * 코덱 검증 테스트
 *
 * <p>P1: validateCodec(), codecRefToClass() 개선</p>
 *
 * <p>코덱 ID/버전 불일치, 업그레이드 시나리오를 테스트합니다.</p>
 *
 * @since v1.0 Phase 3
 * @see FxOptions#allowCodecUpgrade()
 * @see FxCodecUpgradeHook
 */
public class FxStoreCodecValidationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("codec-test.fx");
        storeFile.delete();
    }

    @After
    public void tearDown() {
        // TemporaryFolder가 처리
    }

    // ==================== 정상 코덱 테스트 ====================

    @Test
    public void validateCodec_matching_shouldSucceed() throws Exception {
        // Given: 스토어 생성
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value");
        }

        // When: 같은 코덱으로 다시 열기
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<Long, String> map = store.openMap("test", Long.class, String.class);

            // Then: 정상 동작
            assertEquals("value", map.get(1L));
        }
    }

    // ==================== 내장 코덱 타입 테스트 ====================

    @Test
    public void builtinCodecs_shouldAllWork() throws Exception {
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            // Byte
            NavigableMap<Byte, String> byteMap = store.createMap("byte", Byte.class, String.class);
            byteMap.put((byte)1, "byte-value");

            // Short
            NavigableMap<Short, String> shortMap = store.createMap("short", Short.class, String.class);
            shortMap.put((short)1, "short-value");

            // Integer
            NavigableMap<Integer, String> intMap = store.createMap("int", Integer.class, String.class);
            intMap.put(1, "int-value");

            // Long
            NavigableMap<Long, String> longMap = store.createMap("long", Long.class, String.class);
            longMap.put(1L, "long-value");

            // Float
            NavigableMap<Float, String> floatMap = store.createMap("float", Float.class, String.class);
            floatMap.put(1.0f, "float-value");

            // Double
            NavigableMap<Double, String> doubleMap = store.createMap("double", Double.class, String.class);
            doubleMap.put(1.0, "double-value");

            // String
            NavigableMap<String, String> stringMap = store.createMap("string", String.class, String.class);
            stringMap.put("key", "string-value");

            // 모든 맵에 데이터가 저장되었는지 확인
            assertEquals(1, byteMap.size());
            assertEquals(1, shortMap.size());
            assertEquals(1, intMap.size());
            assertEquals(1, longMap.size());
            assertEquals(1, floatMap.size());
            assertEquals(1, doubleMap.size());
            assertEquals(1, stringMap.size());
        }
    }

    // ==================== 코덱 타입 불일치 테스트 ====================
    // 참고: Long/Integer/Short/Byte는 모두 동일한 코덱 ID("fx:i64")를 사용
    // 따라서 다른 코덱 ID를 가진 타입(Long vs String, Long vs Double)을 테스트해야 함

    @Test(expected = FxException.class)
    public void validateCodec_keyTypeMismatch_shouldThrow() throws Exception {
        // Given: Long 키로 맵 생성
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.createMap("test", Long.class, String.class).put(1L, "value");
        }

        // When: String 키로 열기 시도 (fx:i64 vs fx:string - 다른 코덱)
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.openMap("test", String.class, String.class);
            // Then: FxException 발생
        }
    }

    @Test(expected = FxException.class)
    public void validateCodec_valueTypeMismatch_shouldThrow() throws Exception {
        // Given: String 값으로 맵 생성
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.createMap("test", Long.class, String.class).put(1L, "value");
        }

        // When: Double 값으로 열기 시도 (fx:string vs fx:f64 - 다른 코덱)
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.openMap("test", Long.class, Double.class);
            // Then: FxException 발생
        }
    }

    @Test
    public void validateCodec_typeMismatch_errorMessage() throws Exception {
        // Given: Long 키로 맵 생성
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.createMap("test", Long.class, String.class).put(1L, "value");
        }

        // When: String 키로 열기 시도 (fx:i64 vs fx:string - 다른 코덱)
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.openMap("test", String.class, String.class);
            fail("Expected FxException");
        } catch (FxException e) {
            // Then: 에러 메시지에 mismatch 관련 내용 포함
            assertTrue("Error should mention mismatch or ID",
                e.getMessage().toLowerCase().contains("mismatch") ||
                e.getMessage().contains("Codec ID"));
        }
    }

    // ==================== Set 코덱 테스트 ====================

    @Test(expected = FxException.class)
    public void validateCodec_setTypeMismatch_shouldThrow() throws Exception {
        // Given: Long 타입 Set 생성
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.createSet("test", Long.class).add(1L);
        }

        // When: String 타입으로 열기 시도
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.openSet("test", String.class);
            // Then: FxException 발생
        }
    }

    // ==================== List 코덱 테스트 ====================

    @Test(expected = FxException.class)
    public void validateCodec_listTypeMismatch_shouldThrow() throws Exception {
        // Given: String 타입 List 생성
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.createList("test", String.class).add("value");
        }

        // When: Integer 타입으로 열기 시도
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.openList("test", Integer.class);
            // Then: FxException 발생
        }
    }

    // ==================== Deque 코덱 테스트 ====================

    @Test(expected = FxException.class)
    public void validateCodec_dequeTypeMismatch_shouldThrow() throws Exception {
        // Given: String 타입 Deque 생성
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.createDeque("test", String.class).addLast("value");
        }

        // When: Integer 타입으로 열기 시도
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            store.openDeque("test", Integer.class);
            // Then: FxException 발생
        }
    }

    // ==================== allowCodecUpgrade 테스트 ====================

    @Test
    public void codecUpgrade_disabledByDefault() throws Exception {
        FxOptions options = FxOptions.defaults();
        assertFalse("allowCodecUpgrade should be false by default", options.allowCodecUpgrade());
    }

    @Test
    public void codecUpgrade_canBeEnabled() throws Exception {
        FxOptions options = FxOptions.defaults()
            .withAllowCodecUpgrade(true)
            .build();
        assertTrue("allowCodecUpgrade should be true", options.allowCodecUpgrade());
    }

    // ==================== 코덱 없는 타입 테스트 ====================

    @Test(expected = FxException.class)
    public void createMap_unknownType_shouldThrow() throws Exception {
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            // 코덱이 등록되지 않은 클래스 사용 시도
            store.createMap("test", UnknownType.class, String.class);
        }
    }

    // ==================== byte[] 코덱 테스트 ====================

    @Test
    public void byteArrayCodec_shouldWork() throws Exception {
        try (FxStore store = FxStore.open(storeFile.toPath())) {
            NavigableMap<byte[], String> map = store.createMap("bytes", byte[].class, String.class);
            map.put(new byte[] { 1, 2, 3 }, "test-value");

            // 바이트 배열 키로 조회
            // 참고: 바이트 배열은 사전순 비교됨
            assertEquals(1, map.size());
        }
    }

    // ==================== 메모리 스토어 코덱 테스트 ====================

    @Test
    public void memoryStore_codecValidation_works() {
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
            map.put(1L, "value");

            // 같은 타입으로 열기
            NavigableMap<Long, String> reopened = store.openMap("test", Long.class, String.class);
            assertEquals("value", reopened.get(1L));
        }
    }

    @Test
    public void memoryStore_cachedCollection_returnsSameInstance() {
        // 메모리 스토어에서는 컬렉션이 캐시되므로
        // 같은 이름으로 다시 열면 캐시된 인스턴스가 반환됨
        try (FxStore store = FxStore.openMemory()) {
            NavigableMap<Long, String> created = store.createMap("test", Long.class, String.class);
            created.put(1L, "value");

            // 같은 타입으로 다시 열기 - 같은 인스턴스 반환
            NavigableMap<Long, String> reopened = store.openMap("test", Long.class, String.class);
            assertSame("Should return cached instance", created, reopened);
            assertEquals("value", reopened.get(1L));
        }
    }

    // ==================== 헬퍼 클래스 ====================

    private static class UnknownType {
        // 코덱이 등록되지 않은 타입
    }
}
