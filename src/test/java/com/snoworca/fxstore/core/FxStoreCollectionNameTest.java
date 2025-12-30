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
 * 컬렉션 이름 검증 테스트
 *
 * <p>P1: validateCollectionName() 개선</p>
 *
 * <p>컬렉션 이름 유효성 검사의 다양한 경로를 테스트합니다.</p>
 *
 * @since v1.0 Phase 3
 */
public class FxStoreCollectionNameTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FxStore store;
    private File storeFile;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("name-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== null 이름 테스트 ====================

    @Test(expected = FxException.class)
    public void createMap_nullName_shouldThrow() {
        store.createMap(null, Long.class, String.class);
    }

    @Test(expected = FxException.class)
    public void createSet_nullName_shouldThrow() {
        store.createSet(null, Long.class);
    }

    @Test(expected = FxException.class)
    public void createList_nullName_shouldThrow() {
        store.createList(null, String.class);
    }

    @Test(expected = FxException.class)
    public void createDeque_nullName_shouldThrow() {
        store.createDeque(null, String.class);
    }

    // ==================== 빈 이름 테스트 ====================

    @Test(expected = FxException.class)
    public void createMap_emptyName_shouldThrow() {
        store.createMap("", Long.class, String.class);
    }

    @Test(expected = FxException.class)
    public void createSet_emptyName_shouldThrow() {
        store.createSet("", Long.class);
    }

    @Test(expected = FxException.class)
    public void createList_emptyName_shouldThrow() {
        store.createList("", String.class);
    }

    @Test(expected = FxException.class)
    public void createDeque_emptyName_shouldThrow() {
        store.createDeque("", String.class);
    }

    // ==================== 너무 긴 이름 테스트 (> 255) ====================

    @Test(expected = FxException.class)
    public void createMap_tooLongName_shouldThrow() {
        String tooLong = new String(new char[256]).replace('\0', 'a');
        store.createMap(tooLong, Long.class, String.class);
    }

    @Test(expected = FxException.class)
    public void createSet_tooLongName_shouldThrow() {
        String tooLong = new String(new char[256]).replace('\0', 'a');
        store.createSet(tooLong, Long.class);
    }

    @Test(expected = FxException.class)
    public void createList_tooLongName_shouldThrow() {
        String tooLong = new String(new char[256]).replace('\0', 'a');
        store.createList(tooLong, String.class);
    }

    @Test(expected = FxException.class)
    public void createDeque_tooLongName_shouldThrow() {
        String tooLong = new String(new char[256]).replace('\0', 'a');
        store.createDeque(tooLong, String.class);
    }

    // ==================== 최대 길이 이름 테스트 (== 255) ====================

    @Test
    public void createMap_maxLengthName_shouldSucceed() {
        String maxLength = new String(new char[255]).replace('\0', 'a');
        NavigableMap<Long, String> map = store.createMap(maxLength, Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists(maxLength));
    }

    @Test
    public void createSet_maxLengthName_shouldSucceed() {
        String maxLength = new String(new char[255]).replace('\0', 'b');
        assertNotNull(store.createSet(maxLength, Long.class));
        assertTrue(store.exists(maxLength));
    }

    @Test
    public void createList_maxLengthName_shouldSucceed() {
        String maxLength = new String(new char[255]).replace('\0', 'c');
        assertNotNull(store.createList(maxLength, String.class));
        assertTrue(store.exists(maxLength));
    }

    @Test
    public void createDeque_maxLengthName_shouldSucceed() {
        String maxLength = new String(new char[255]).replace('\0', 'd');
        assertNotNull(store.createDeque(maxLength, String.class));
        assertTrue(store.exists(maxLength));
    }

    // ==================== 유니코드 이름 테스트 ====================

    @Test
    public void createMap_unicodeName_shouldSucceed() {
        String unicodeName = "한글이름";
        NavigableMap<Long, String> map = store.createMap(unicodeName, Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists(unicodeName));
    }

    @Test
    public void createMap_japaneseName_shouldSucceed() {
        String japaneseName = "日本語名前";
        NavigableMap<Long, String> map = store.createMap(japaneseName, Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists(japaneseName));
    }

    @Test
    public void createMap_emojiName_shouldSucceed() {
        String emojiName = "collection_🚀_test";
        NavigableMap<Long, String> map = store.createMap(emojiName, Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists(emojiName));
    }

    @Test
    public void createMap_mixedLanguageName_shouldSucceed() {
        String mixedName = "English한글日本語";
        NavigableMap<Long, String> map = store.createMap(mixedName, Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists(mixedName));
    }

    // ==================== 특수 문자 이름 테스트 ====================

    @Test
    public void createMap_withSpaces_shouldSucceed() {
        String spaceName = "collection with spaces";
        NavigableMap<Long, String> map = store.createMap(spaceName, Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists(spaceName));
    }

    @Test
    public void createMap_withSpecialChars_shouldSucceed() {
        String specialName = "collection-with_special.chars";
        NavigableMap<Long, String> map = store.createMap(specialName, Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists(specialName));
    }

    @Test
    public void createMap_withNumbers_shouldSucceed() {
        String numberedName = "collection123";
        NavigableMap<Long, String> map = store.createMap(numberedName, Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists(numberedName));
    }

    @Test
    public void createMap_startingWithNumber_shouldSucceed() {
        String numberedName = "123collection";
        NavigableMap<Long, String> map = store.createMap(numberedName, Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists(numberedName));
    }

    // ==================== 에러 메시지 검증 ====================

    @Test
    public void createMap_nullName_errorMessage() {
        try {
            store.createMap(null, Long.class, String.class);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue("Error should mention null or empty",
                e.getMessage().contains("null") || e.getMessage().contains("empty"));
        }
    }

    @Test
    public void createMap_emptyName_errorMessage() {
        try {
            store.createMap("", Long.class, String.class);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue("Error should mention null or empty",
                e.getMessage().contains("null") || e.getMessage().contains("empty"));
        }
    }

    @Test
    public void createMap_tooLongName_errorMessage() {
        String tooLong = new String(new char[256]).replace('\0', 'x');
        try {
            store.createMap(tooLong, Long.class, String.class);
            fail("Expected FxException");
        } catch (FxException e) {
            assertTrue("Error should mention too long or 255",
                e.getMessage().contains("too long") || e.getMessage().contains("255"));
        }
    }

    // ==================== 이름 재사용 테스트 ====================

    @Test
    public void createMap_afterDrop_shouldSucceed() {
        // 생성
        String name = "reusable";
        store.createMap(name, Long.class, String.class);
        assertTrue(store.exists(name));

        // 삭제
        store.drop(name);
        assertFalse(store.exists(name));

        // 재생성
        NavigableMap<Long, String> newMap = store.createMap(name, Long.class, String.class);
        assertNotNull(newMap);
        assertTrue(store.exists(name));
    }

    // ==================== 경계 테스트 ====================

    @Test
    public void createMap_singleCharName_shouldSucceed() {
        NavigableMap<Long, String> map = store.createMap("a", Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists("a"));
    }

    @Test
    public void createMap_twoCharName_shouldSucceed() {
        NavigableMap<Long, String> map = store.createMap("ab", Long.class, String.class);
        assertNotNull(map);
        assertTrue(store.exists("ab"));
    }

    // ==================== reopen 후 이름 유지 ====================

    @Test
    public void unicodeName_persistsAfterReopen() throws Exception {
        String unicodeName = "테스트컬렉션";
        NavigableMap<Long, String> map = store.createMap(unicodeName, Long.class, String.class);
        map.put(1L, "value");

        store.close();
        store = FxStore.open(storeFile.toPath());

        assertTrue("Unicode name should persist after reopen", store.exists(unicodeName));
        NavigableMap<Long, String> reopened = store.openMap(unicodeName, Long.class, String.class);
        assertEquals("value", reopened.get(1L));
    }
}
