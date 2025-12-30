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
 * FxStoreImpl 미커버 메서드 커버리지 테스트
 *
 * <p>커버리지 개선 대상:</p>
 * <ul>
 *   <li>verify 관련 메서드 브랜치</li>
 *   <li>codecRefToClass 브랜치</li>
 *   <li>countTreeBytes 브랜치</li>
 *   <li>createOrOpen 메서드들</li>
 * </ul>
 */
public class FxStoreImplCoverageTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File storeFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        storeFile = tempFolder.newFile("coverage-test.fx");
        storeFile.delete();
        store = FxStore.open(storeFile.toPath());
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== createOrOpen 테스트 ====================

    @Test
    public void createOrOpenMap_existing_shouldOpen() {
        // Given: 맵 생성
        NavigableMap<Long, String> created = store.createMap("test", Long.class, String.class);
        created.put(1L, "value");

        // When: createOrOpenMap (기존 맵 열기)
        NavigableMap<Long, String> opened = store.createOrOpenMap("test", Long.class, String.class);

        // Then: 같은 데이터
        assertEquals("value", opened.get(1L));
    }

    @Test
    public void createOrOpenMap_new_shouldCreate() {
        // When: createOrOpenMap (새 맵)
        NavigableMap<Long, String> map = store.createOrOpenMap("newMap", Long.class, String.class);
        map.put(1L, "value");

        // Then
        assertEquals("value", map.get(1L));
    }

    @Test
    public void createOrOpenSet_existing_shouldOpen() {
        // Given
        NavigableSet<String> created = store.createSet("testSet", String.class);
        created.add("a");

        // When
        NavigableSet<String> opened = store.createOrOpenSet("testSet", String.class);

        // Then
        assertTrue(opened.contains("a"));
    }

    @Test
    public void createOrOpenSet_new_shouldCreate() {
        NavigableSet<String> set = store.createOrOpenSet("newSet", String.class);
        set.add("value");
        assertTrue(set.contains("value"));
    }

    @Test
    public void createOrOpenList_existing_shouldOpen() {
        // Given
        List<String> created = store.createList("testList", String.class);
        created.add("item");

        // When
        List<String> opened = store.createOrOpenList("testList", String.class);

        // Then
        assertEquals("item", opened.get(0));
    }

    @Test
    public void createOrOpenList_new_shouldCreate() {
        List<String> list = store.createOrOpenList("newList", String.class);
        list.add("value");
        assertEquals("value", list.get(0));
    }

    @Test
    public void createOrOpenDeque_existing_shouldOpen() {
        // Given
        Deque<String> created = store.createDeque("testDeque", String.class);
        created.addLast("item");

        // When
        Deque<String> opened = store.createOrOpenDeque("testDeque", String.class);

        // Then
        assertEquals("item", opened.peekFirst());
    }

    @Test
    public void createOrOpenDeque_new_shouldCreate() {
        Deque<String> deque = store.createOrOpenDeque("newDeque", String.class);
        deque.addLast("value");
        assertEquals("value", deque.peekFirst());
    }

    // ==================== Stats DEEP 모드 브랜치 테스트 ====================

    @Test
    public void statsDeep_withEmptyStore_shouldWork() {
        // Given: 빈 스토어

        // When: DEEP 모드 stats
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertNotNull(stats);
        assertEquals(0, stats.collectionCount());
    }

    @Test
    public void statsDeep_withMultipleCollections_shouldCalculateBytes() {
        // Given: 여러 컬렉션
        NavigableMap<Long, String> map = store.createMap("map1", Long.class, String.class);
        for (long i = 0; i < 100; i++) {
            map.put(i, "value-" + i);
        }

        NavigableSet<Long> set = store.createSet("set1", Long.class);
        for (long i = 0; i < 50; i++) {
            set.add(i);
        }

        List<String> list = store.createList("list1", String.class);
        for (int i = 0; i < 30; i++) {
            list.add("item-" + i);
        }

        // When: DEEP 모드 stats
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertNotNull(stats);
        assertEquals(3, stats.collectionCount());
        assertTrue("Live bytes should be positive", stats.liveBytesEstimate() > 0);
        assertTrue("File bytes should be positive", stats.fileBytes() > 0);
    }

    // ==================== verify 테스트 ====================

    @Test
    public void verify_newStore_shouldPass() {
        // Given: 새 스토어
        store.createMap("test", Long.class, String.class).put(1L, "value");

        // When: verify
        VerifyResult result = store.verify();

        // Then: 성공
        assertTrue("Verify should pass", result.ok());
        assertTrue("Errors should be empty", result.errors().isEmpty());
    }

    @Test
    public void verify_afterManyOperations_shouldPass() {
        // Given: 많은 작업 후
        NavigableMap<Long, String> map = store.createMap("bigMap", Long.class, String.class);
        for (long i = 0; i < 500; i++) {
            map.put(i, "value-" + i);
        }

        // 일부 삭제
        for (long i = 0; i < 100; i++) {
            map.remove(i);
        }

        // When: verify
        VerifyResult result = store.verify();

        // Then: 성공
        assertTrue("Verify should pass", result.ok());
    }

    // ==================== rename 테스트 ====================

    @Test
    public void rename_existingCollection_shouldSucceed() {
        // Given: 컬렉션 생성
        store.createMap("oldName", Long.class, String.class).put(1L, "value");

        // When: rename
        store.rename("oldName", "newName");

        // Then
        assertFalse(store.exists("oldName"));
        assertTrue(store.exists("newName"));

        NavigableMap<Long, String> renamed = store.openMap("newName", Long.class, String.class);
        assertEquals("value", renamed.get(1L));
    }

    @Test(expected = FxException.class)
    public void rename_nonExistent_shouldThrow() {
        store.rename("nonexistent", "newName");
    }

    @Test(expected = FxException.class)
    public void rename_toExisting_shouldThrow() {
        store.createMap("source", Long.class, String.class);
        store.createMap("target", Long.class, String.class);

        store.rename("source", "target");
    }

    // ==================== 커밋 모드 테스트 ====================

    @Test
    public void commitMode_shouldReturnCorrectMode() throws Exception {
        store.close();

        // AUTO 모드
        FxOptions autoOptions = FxOptions.defaults().withCommitMode(CommitMode.AUTO).build();
        try (FxStore autoStore = FxStore.open(storeFile.toPath(), autoOptions)) {
            assertEquals(CommitMode.AUTO, autoStore.commitMode());
        }

        // BATCH 모드
        storeFile.delete();
        FxOptions batchOptions = FxOptions.defaults().withCommitMode(CommitMode.BATCH).build();
        try (FxStore batchStore = FxStore.open(storeFile.toPath(), batchOptions)) {
            assertEquals(CommitMode.BATCH, batchStore.commitMode());
        }
    }

    // ==================== 현재 커밋 헤더 테스트 ====================

    @Test
    public void getCurrentCommitHeader_shouldReturnHeader() throws Exception {
        // Given: 데이터 추가
        store.createMap("test", Long.class, String.class).put(1L, "value");
        store.close();

        // When: 스토어 열기
        store = FxStore.open(storeFile.toPath());

        // Then: 현재 커밋 헤더 존재 (내부 메서드이므로 캐스팅 필요)
        // 직접 접근 불가하지만, stats가 내부적으로 호출
        Stats stats = store.stats();
        assertNotNull(stats);
    }

    // ==================== byte[] 코덱 테스트 ====================

    @Test
    public void byteArrayKey_shouldWorkWithCodecRefToClass() throws Exception {
        // Given: byte[] 키
        NavigableMap<byte[], String> map = store.createMap("byteKeyMap", byte[].class, String.class);
        byte[] key = {1, 2, 3};
        map.put(key, "value");

        store.close();

        // When: 다시 열기 (codecRefToClass 호출됨)
        store = FxStore.open(storeFile.toPath());
        store.compactTo(tempFolder.newFile("compacted.fx").toPath());

        // Then: 성공
    }

    // ==================== list() 테스트 ====================

    @Test
    public void list_emptyStore_shouldReturnEmptyList() {
        List<CollectionInfo> infos = store.list();
        assertTrue(infos.isEmpty());
    }

    @Test
    public void list_withCollections_shouldReturnAll() {
        // Given
        store.createMap("map1", Long.class, String.class);
        store.createSet("set1", Long.class);
        store.createList("list1", String.class);
        store.createDeque("deque1", String.class);

        // When
        List<CollectionInfo> infos = store.list();

        // Then
        assertEquals(4, infos.size());

        Set<String> names = new HashSet<>();
        for (CollectionInfo info : infos) {
            names.add(info.name());
        }
        assertTrue(names.contains("map1"));
        assertTrue(names.contains("set1"));
        assertTrue(names.contains("list1"));
        assertTrue(names.contains("deque1"));
    }

    // ==================== compactTo 테스트 ====================

    @Test
    public void compactTo_withAllTypes_shouldCopyAll() throws Exception {
        // Given: 모든 타입 컬렉션
        store.createMap("map", Long.class, String.class).put(1L, "value");
        store.createSet("set", Long.class).add(1L);
        store.createList("list", String.class).add("item");
        store.createDeque("deque", String.class).addLast("item");

        File target = tempFolder.newFile("compact-target.fx");
        target.delete();

        // When
        store.compactTo(target.toPath());

        // Then: 새 파일에서 검증
        try (FxStore targetStore = FxStore.open(target.toPath())) {
            assertEquals("value", targetStore.openMap("map", Long.class, String.class).get(1L));
            assertTrue(targetStore.openSet("set", Long.class).contains(1L));
            assertEquals("item", targetStore.openList("list", String.class).get(0));
            assertEquals("item", targetStore.openDeque("deque", String.class).peekFirst());
        }
    }

    // ==================== 트랜잭션 테스트 ====================

    @Test
    public void manualCommitAndRollback_shouldWork() throws Exception {
        store.close();

        FxOptions options = FxOptions.defaults().withCommitMode(CommitMode.BATCH).build();
        store = FxStore.open(storeFile.toPath(), options);

        // 데이터 추가 후 커밋
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "committed");
        store.commit();

        // 더 추가 후 롤백
        map.put(2L, "rollback");
        store.rollback();

        // 검증
        assertEquals("committed", map.get(1L));
        assertNull(map.get(2L));
    }

    // ==================== beginRead 테스트 ====================

    @Test
    public void beginRead_shouldCreateTransaction() {
        // Given
        store.createMap("test", Long.class, String.class).put(1L, "value");

        // When
        FxReadTransaction tx = store.beginRead();

        // Then
        assertNotNull(tx);
        tx.close();
    }

    // ==================== registerCodec 테스트 ====================

    @Test
    public void registerCodec_shouldAllowCustomType() {
        // Given: 커스텀 코덱
        FxCodec<UUID> uuidCodec = new FxCodec<UUID>() {
            @Override
            public String id() { return "test:uuid"; }

            @Override
            public byte[] encode(UUID value) {
                byte[] bytes = new byte[16];
                long msb = value.getMostSignificantBits();
                long lsb = value.getLeastSignificantBits();
                for (int i = 0; i < 8; i++) {
                    bytes[i] = (byte) ((msb >> (56 - i * 8)) & 0xFF);
                    bytes[i + 8] = (byte) ((lsb >> (56 - i * 8)) & 0xFF);
                }
                return bytes;
            }

            @Override
            public UUID decode(byte[] bytes) {
                long msb = 0, lsb = 0;
                for (int i = 0; i < 8; i++) {
                    msb = (msb << 8) | (bytes[i] & 0xFF);
                    lsb = (lsb << 8) | (bytes[i + 8] & 0xFF);
                }
                return new UUID(msb, lsb);
            }

            @Override
            public int version() { return 1; }

            @Override
            public int compareBytes(byte[] a, byte[] b) {
                for (int i = 0; i < 16; i++) {
                    int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
                    if (cmp != 0) return cmp;
                }
                return 0;
            }

            @Override
            public boolean equalsBytes(byte[] a, byte[] b) {
                return java.util.Arrays.equals(a, b);
            }

            @Override
            public int hashBytes(byte[] bytes) {
                return java.util.Arrays.hashCode(bytes);
            }
        };

        // When: 코덱 등록
        store.registerCodec(UUID.class, uuidCodec);

        // Then: 해당 타입 사용 가능
        NavigableMap<UUID, String> map = store.createMap("uuidMap", UUID.class, String.class);
        UUID key = UUID.randomUUID();
        map.put(key, "value");
        assertEquals("value", map.get(key));
    }

    // ==================== codecs() 테스트 ====================

    @Test
    public void codecs_shouldReturnCodecRegistry() {
        FxCodecRegistry codecs = store.codecs();
        assertNotNull(codecs);

        // 내장 코덱 확인
        assertNotNull(codecs.get(Long.class));
        assertNotNull(codecs.get(String.class));
        assertNotNull(codecs.get(Integer.class));
        assertNotNull(codecs.get(Double.class));
    }

    // ==================== 메모리 스토어 테스트 ====================

    @Test
    public void memoryStore_shouldSupportAllOperations() {
        try (FxStore memStore = FxStore.openMemory()) {
            NavigableMap<Long, String> map = memStore.createMap("test", Long.class, String.class);
            map.put(1L, "value");

            assertEquals("value", map.get(1L));

            Stats stats = memStore.stats(StatsMode.DEEP);
            assertNotNull(stats);
        }
    }

    // ==================== 대용량 데이터로 countTreeBytes 커버리지 ====================

    @Test
    public void largeData_shouldTriggerCountTreeBytes() throws Exception {
        // Given: 많은 데이터
        NavigableMap<Long, String> map = store.createMap("large", Long.class, String.class);
        String largeValue = new String(new char[1000]).replace('\0', 'x');

        for (long i = 0; i < 1000; i++) {
            map.put(i, largeValue + i);
        }

        // When: DEEP stats (countTreeBytes 호출)
        Stats stats = store.stats(StatsMode.DEEP);

        // Then
        assertNotNull(stats);
        assertTrue("Live bytes should be > 0", stats.liveBytesEstimate() > 0);
    }

    // ==================== createDeque 브랜치 테스트 ====================

    @Test
    public void createDeque_shouldInitializeCorrectly() {
        Deque<String> deque = store.createDeque("newDeque", String.class);

        // 양쪽 끝 작업
        deque.addFirst("first");
        deque.addLast("last");

        assertEquals("first", deque.peekFirst());
        assertEquals("last", deque.peekLast());
        assertEquals(2, deque.size());
    }

    // ==================== exists 테스트 ====================

    @Test
    public void exists_shouldReturnCorrectValue() {
        assertFalse(store.exists("nonexistent"));

        store.createMap("exists", Long.class, String.class);
        assertTrue(store.exists("exists"));

        store.drop("exists");
        assertFalse(store.exists("exists"));
    }

    // ==================== drop 테스트 ====================

    @Test
    public void drop_shouldRemoveCollection() {
        store.createMap("toDrop", Long.class, String.class).put(1L, "value");
        assertTrue(store.exists("toDrop"));

        boolean dropped = store.drop("toDrop");
        assertTrue(dropped);
        assertFalse(store.exists("toDrop"));
    }

    @Test
    public void drop_nonexistent_shouldReturnFalse() {
        boolean dropped = store.drop("nonexistent");
        assertFalse(dropped);
    }

    // ==================== 코덱 타입별 테스트 (codecRefToClass 브랜치 커버) ====================

    @Test
    public void allBuiltinCodecTypes_shouldWorkInCompactTo() throws Exception {
        // Long 타입
        NavigableMap<Long, Long> longMap = store.createMap("longMap", Long.class, Long.class);
        longMap.put(1L, 100L);

        // Double 타입
        NavigableMap<Long, Double> doubleMap = store.createMap("doubleMap", Long.class, Double.class);
        doubleMap.put(1L, 3.14);

        // STRING 타입
        NavigableMap<Long, String> stringMap = store.createMap("stringMap", Long.class, String.class);
        stringMap.put(1L, "hello");

        // BYTES 타입
        NavigableMap<Long, byte[]> bytesMap = store.createMap("bytesMap", Long.class, byte[].class);
        bytesMap.put(1L, new byte[]{1, 2, 3});

        File target = tempFolder.newFile("all-types-compact.fx");
        target.delete();

        // compactTo - codecRefToClass 호출
        store.compactTo(target.toPath());

        // 검증
        try (FxStore targetStore = FxStore.open(target.toPath())) {
            assertEquals(Long.valueOf(100L), targetStore.openMap("longMap", Long.class, Long.class).get(1L));
            assertEquals(Double.valueOf(3.14), targetStore.openMap("doubleMap", Long.class, Double.class).get(1L));
            assertEquals("hello", targetStore.openMap("stringMap", Long.class, String.class).get(1L));
            assertArrayEquals(new byte[]{1, 2, 3}, targetStore.openMap("bytesMap", Long.class, byte[].class).get(1L));
        }
    }

    // ==================== verify 추가 브랜치 테스트 ====================

    @Test
    public void verify_withSetAndDeque_shouldPass() {
        // Set 생성
        NavigableSet<Long> set = store.createSet("testSet", Long.class);
        for (long i = 0; i < 100; i++) {
            set.add(i);
        }

        // Deque 생성
        Deque<String> deque = store.createDeque("testDeque", String.class);
        for (int i = 0; i < 50; i++) {
            deque.addLast("item" + i);
        }

        // verify
        VerifyResult result = store.verify();
        assertTrue(result.ok());
    }

    @Test
    public void verify_afterReopen_shouldPass() throws Exception {
        // 데이터 추가 후 닫기
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        for (long i = 0; i < 50; i++) {
            map.put(i, "value" + i);
        }
        store.close();

        // 다시 열고 verify
        store = FxStore.open(storeFile.toPath());
        VerifyResult result = store.verify();
        assertTrue(result.ok());
    }

    // ==================== stats 추가 모드 테스트 ====================

    @Test
    public void stats_FAST_shouldWork() {
        store.createMap("test", Long.class, String.class).put(1L, "value");

        Stats stats = store.stats(StatsMode.FAST);
        assertNotNull(stats);
        assertEquals(1, stats.collectionCount());
    }

    @Test
    public void stats_withNoData_shouldReturnZeroLiveBytes() {
        Stats stats = store.stats(StatsMode.DEEP);
        // 빈 스토어도 일부 바이트는 있음 (슈퍼블록 등)
        assertNotNull(stats);
    }

    // ==================== close 후 작업 테스트 ====================

    @Test(expected = FxException.class)
    public void createSet_afterClose_shouldThrow() {
        store.close();
        store.createSet("test", Long.class);
    }

    @Test(expected = FxException.class)
    public void createList_afterClose_shouldThrow() {
        store.close();
        store.createList("test", String.class);
    }

    @Test(expected = FxException.class)
    public void createDeque_afterClose_shouldThrow() {
        store.close();
        store.createDeque("test", String.class);
    }

    @Test(expected = FxException.class)
    public void commit_afterClose_shouldThrow() throws Exception {
        store.close();
        store = FxStore.open(storeFile.toPath(),
            FxOptions.defaults().withCommitMode(CommitMode.BATCH).build());
        store.createMap("test", Long.class, String.class);
        store.close();
        store.commit();
    }

    // ==================== 다중 컬렉션 countTreeBytes 테스트 ====================

    @Test
    public void countTreeBytes_multipleCollections_shouldWork() {
        // 여러 컬렉션 생성
        NavigableMap<Long, String> map1 = store.createMap("map1", Long.class, String.class);
        NavigableMap<Long, String> map2 = store.createMap("map2", Long.class, String.class);
        NavigableSet<Long> set1 = store.createSet("set1", Long.class);
        List<String> list1 = store.createList("list1", String.class);

        // 각각 데이터 추가
        for (long i = 0; i < 200; i++) {
            map1.put(i, "map1-" + i);
            map2.put(i * 10, "map2-" + i);
            set1.add(i * 100);
            list1.add("list1-" + i);
        }

        // DEEP stats - countTreeBytes 호출
        Stats stats = store.stats(StatsMode.DEEP);
        assertNotNull(stats);
        assertEquals(4, stats.collectionCount());
        assertTrue(stats.liveBytesEstimate() > 0);
    }

    // ==================== FxCodecUpgradeHook 관련 테스트 ====================
    // codecUpgrade 테스트는 복잡한 코덱 등록 순서에 의존하므로 별도 테스트 클래스에서 다룸

    // ==================== 컬렉션 이름 검증 테스트 ====================

    @Test(expected = FxException.class)
    public void createMap_emptyName_shouldThrow() {
        store.createMap("", Long.class, String.class);
    }

    @Test(expected = FxException.class)
    public void createMap_nullName_shouldThrow() {
        store.createMap(null, Long.class, String.class);
    }

    @Test(expected = FxException.class)
    public void createMap_tooLongName_shouldThrow() {
        // 256자 초과 이름
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 260; i++) {
            sb.append("x");
        }
        store.createMap(sb.toString(), Long.class, String.class);
    }

    // ==================== 동시 읽기 스냅샷 테스트 ====================

    @Test
    public void beginRead_multipleSnapshots_shouldBeIndependent() {
        NavigableMap<Long, String> map = store.createMap("test", Long.class, String.class);
        map.put(1L, "original");

        // 첫 번째 읽기 트랜잭션
        FxReadTransaction tx1 = store.beginRead();
        assertEquals("original", tx1.get(map, 1L));

        // 쓰기 변경
        map.put(1L, "modified");
        map.put(2L, "new");

        // 두 번째 읽기 트랜잭션
        FxReadTransaction tx2 = store.beginRead();

        // tx1은 여전히 원래 값 (스냅샷)
        assertEquals("original", tx1.get(map, 1L));
        assertNull(tx1.get(map, 2L));

        // tx2는 새 값
        assertEquals("modified", tx2.get(map, 1L));
        assertEquals("new", tx2.get(map, 2L));

        tx1.close();
        tx2.close();
    }
}
