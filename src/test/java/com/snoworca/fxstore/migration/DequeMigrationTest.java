package com.snoworca.fxstore.migration;

import com.snoworca.fxstore.api.FxOptions;
import com.snoworca.fxstore.api.FxReadTransaction;
import com.snoworca.fxstore.api.FxStore;
import com.snoworca.fxstore.catalog.CollectionState;
import com.snoworca.fxstore.collection.FxDequeImpl;
import com.snoworca.fxstore.collection.LegacySeqEncoder;
import com.snoworca.fxstore.collection.OrderedSeqEncoder;
import com.snoworca.fxstore.core.FxStoreImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Deque;

import static org.junit.Assert.*;

/**
 * Deque 마이그레이션 테스트
 *
 * <p>v0.6 LEGACY 인코딩에서 v0.7 ORDERED 인코딩으로의 마이그레이션 검증</p>
 *
 * @since 0.7
 */
public class DequeMigrationTest {

    private File tempFile;
    private FxStore store;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-migration-", ".db").toFile();
        tempFile.delete();
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) {
            store.close();
        }
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    // ==================== needsMigration 테스트 ====================

    @Test
    public void testNeedsMigration_newDeque_returnsFalse() {
        store = FxStore.open(tempFile.toPath());
        Deque<String> deque = store.createDeque("newDeque", String.class);
        deque.addLast("test");

        // 새로 생성된 Deque는 이미 ORDERED 인코딩 사용
        assertFalse(DequeMigrator.needsMigration(store, "newDeque"));
    }

    @Test
    public void testNeedsMigration_nonExistentDeque_returnsFalse() {
        store = FxStore.open(tempFile.toPath());

        assertFalse(DequeMigrator.needsMigration(store, "nonExistent"));
    }

    // ==================== migrate 테스트 ====================

    @Test
    public void testMigrate_alreadyMigrated_noOp() {
        store = FxStore.open(tempFile.toPath());
        Deque<String> deque = store.createDeque("testDeque", String.class);
        deque.addLast("A");
        deque.addLast("B");

        // 이미 ORDERED 인코딩이므로 마이그레이션 필요 없음
        DequeMigrator.migrate(store, "testDeque", String.class);

        // 데이터 보존 확인
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("A", tx.peekFirst(deque));
            assertEquals("B", tx.peekLast(deque));
            assertEquals(2, tx.size(deque));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMigrate_nonFxStoreImpl_throwsException() {
        // FxStoreImpl이 아닌 mock store로 호출 시 예외
        DequeMigrator.migrate(null, "test", String.class);
    }

    // ==================== 인코더 버전 확인 ====================

    @Test
    public void testNewDeque_usesOrderedEncoder() {
        store = FxStore.open(tempFile.toPath());
        Deque<String> deque = store.createDeque("orderedDeque", String.class);

        FxDequeImpl<String> impl = (FxDequeImpl<String>) deque;
        assertTrue(impl.getSeqEncoder() instanceof OrderedSeqEncoder);
    }

    @Test
    public void testNewDeque_collectionStateHasOrderedVersion() {
        store = FxStore.open(tempFile.toPath());
        store.createDeque("testDeque", String.class);

        FxStoreImpl storeImpl = (FxStoreImpl) store;
        CollectionState state = storeImpl.getCollectionState("testDeque");

        assertNotNull(state);
        assertEquals(CollectionState.SEQ_ENCODER_VERSION_ORDERED, state.getSeqEncoderVersion());
    }

    // ==================== 데이터 보존 테스트 ====================

    @Test
    public void testMigration_preservesData() {
        store = FxStore.open(tempFile.toPath());
        Deque<String> deque = store.createDeque("dataDeque", String.class);

        // 다양한 패턴으로 데이터 추가
        deque.addLast("A");  // seq=0
        deque.addLast("B");  // seq=1
        deque.addFirst("Z"); // seq=-1
        deque.addFirst("Y"); // seq=-2

        // 논리적 순서: Y, Z, A, B

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("Y", tx.peekFirst(deque));
            assertEquals("B", tx.peekLast(deque));
            assertEquals(4, tx.size(deque));
        }
    }

    // ==================== autoMigrateDeque 옵션 테스트 ====================

    @Test
    public void testAutoMigrateDeque_defaultIsFalse() {
        FxOptions opts = FxOptions.defaults();
        assertFalse(opts.autoMigrateDeque());
    }

    @Test
    public void testAutoMigrateDeque_canBeEnabled() {
        FxOptions opts = FxOptions.defaults()
            .withAutoMigrateDeque(true)
            .build();
        assertTrue(opts.autoMigrateDeque());
    }

    // ==================== 성능 테스트 (마이그레이션 후) ====================

    @Test
    public void testAfterMigration_peekIsEfficient() {
        store = FxStore.open(tempFile.toPath());
        Deque<String> deque = store.createDeque("perfDeque", String.class);

        // 1000개 요소 추가
        for (int i = 0; i < 1000; i++) {
            if (i % 2 == 0) {
                deque.addLast("last-" + i);
            } else {
                deque.addFirst("first-" + i);
            }
        }

        // 성능 측정
        long startTime = System.nanoTime();
        int iterations = 1000;
        try (FxReadTransaction tx = store.beginRead()) {
            for (int i = 0; i < iterations; i++) {
                tx.peekFirst(deque);
                tx.peekLast(deque);
            }
        }
        long elapsed = System.nanoTime() - startTime;
        double nsPerOp = (double) elapsed / (iterations * 2);

        System.out.printf("peekFirst/peekLast after migration (n=1000): %.2f ns/op%n", nsPerOp);

        // O(log n) 기대
        assertTrue("peek should be O(log n)", nsPerOp < 50000);
    }

    // ==================== 재오픈 후 버전 유지 테스트 ====================

    @Test
    public void testReopenStore_preservesEncoderVersion() throws Exception {
        // 1. Store 생성 및 Deque 추가
        store = FxStore.open(tempFile.toPath());
        Deque<String> deque = store.createDeque("persistDeque", String.class);
        deque.addLast("A");
        deque.addLast("B");
        store.close();

        // 2. Store 재오픈
        store = FxStore.open(tempFile.toPath());
        Deque<String> reopened = store.openDeque("persistDeque", String.class);

        // 3. ORDERED 인코딩 유지 확인
        FxDequeImpl<String> impl = (FxDequeImpl<String>) reopened;
        assertTrue("Should still use OrderedSeqEncoder after reopen",
                   impl.getSeqEncoder() instanceof OrderedSeqEncoder);

        // 4. 데이터 보존 확인
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("A", tx.peekFirst(reopened));
            assertEquals("B", tx.peekLast(reopened));
        }
    }

    // ==================== 경계 조건 테스트 ====================

    @Test
    public void testMigration_emptyDeque() {
        store = FxStore.open(tempFile.toPath());
        Deque<String> deque = store.createDeque("emptyDeque", String.class);

        // 빈 Deque도 마이그레이션 가능
        DequeMigrator.migrate(store, "emptyDeque", String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.peekFirst(deque));
            assertNull(tx.peekLast(deque));
            assertEquals(0, tx.size(deque));
        }
    }

    @Test
    public void testMigration_singleElement() {
        store = FxStore.open(tempFile.toPath());
        Deque<String> deque = store.createDeque("singleDeque", String.class);
        deque.addLast("only");

        DequeMigrator.migrate(store, "singleDeque", String.class);

        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("only", tx.peekFirst(deque));
            assertEquals("only", tx.peekLast(deque));
            assertEquals(1, tx.size(deque));
        }
    }
}
