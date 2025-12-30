package com.snoworca.fxstore.migration;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static org.junit.Assert.*;

/**
 * DequeMigrator LEGACY 마이그레이션 테스트
 *
 * <p>performMigration() 경로 테스트를 위해 LEGACY Deque를 수동 생성합니다.</p>
 *
 * <p>테스트 시나리오:</p>
 * <ul>
 *   <li>L1-L2: LEGACY 설정 검증</li>
 *   <li>M1-M4: 마이그레이션 실행</li>
 *   <li>D1-D3: 데이터 무결성</li>
 *   <li>E1-E3: 경계 조건</li>
 *   <li>S1-S2: 상태 검증</li>
 * </ul>
 *
 * @since 0.7
 * @see DequeMigrator
 * @see DequeMigrationTest
 */
public class DequeMigratorLegacyTest {

    private File tempFile;
    private FxStore store;
    private FxStoreImpl storeImpl;

    @Before
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("fxstore-legacy-", ".db").toFile();
        tempFile.delete();
        store = FxStore.open(tempFile.toPath());
        storeImpl = (FxStoreImpl) store;
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

    // ==================== Helper Methods ====================

    /**
     * LEGACY 인코딩 Deque 생성 헬퍼
     *
     * <p>1. 일반 Deque 생성 (ORDERED)
     * <p>2. CollectionState를 LEGACY로 변경
     * <p>3. SeqEncoder를 LegacySeqEncoder로 변경
     *
     * @param name Deque 이름
     * @param elementClass 요소 타입
     * @param <E> 요소 타입
     * @return LEGACY 인코딩 Deque
     */
    private <E> Deque<E> createLegacyDeque(String name, Class<E> elementClass) {
        // 1. 일반 Deque 생성 (ORDERED)
        Deque<E> deque = store.createDeque(name, elementClass);
        FxDequeImpl<E> impl = (FxDequeImpl<E>) deque;

        // 2. LEGACY로 다운그레이드
        long collectionId = impl.getCollectionId();
        CollectionState oldState = storeImpl.getCollectionStateById(collectionId);
        CollectionState legacyState = oldState.withSeqEncoderVersion(
            CollectionState.SEQ_ENCODER_VERSION_LEGACY);
        storeImpl.updateCollectionState(collectionId, legacyState);

        // 3. SeqEncoder 변경
        impl.setSeqEncoder(LegacySeqEncoder.getInstance());

        return deque;
    }

    // ==================== L: LEGACY 설정 테스트 ====================

    /**
     * L1: LEGACY Deque 생성 시 needsMigration이 true 반환
     */
    @Test
    public void testCreateLegacyDeque_needsMigrationReturnsTrue() {
        // Given: LEGACY Deque 생성
        Deque<String> deque = createLegacyDeque("legacyTest", String.class);
        deque.addLast("test");

        // Then: needsMigration returns true
        assertTrue("LEGACY Deque should need migration",
                   DequeMigrator.needsMigration(store, "legacyTest"));
    }

    /**
     * L2: LEGACY Deque가 LegacySeqEncoder 사용 확인
     */
    @Test
    public void testCreateLegacyDeque_hasLegacyEncoder() {
        // Given: LEGACY Deque 생성
        Deque<String> deque = createLegacyDeque("legacyEncoder", String.class);

        // Then: LegacySeqEncoder 사용
        FxDequeImpl<String> impl = (FxDequeImpl<String>) deque;
        assertTrue("Should use LegacySeqEncoder",
                   impl.getSeqEncoder() instanceof LegacySeqEncoder);
    }

    // ==================== M: 마이그레이션 실행 테스트 ====================

    /**
     * M1: LEGACY Deque 마이그레이션 시 performMigration 실행
     */
    @Test
    public void testMigrate_legacyDeque_executesPerformMigration() {
        // Given: LEGACY Deque with data
        Deque<String> deque = createLegacyDeque("migrateTest", String.class);
        deque.addLast("A");
        deque.addLast("B");
        deque.addLast("C");

        // Verify: needsMigration returns true before
        assertTrue(DequeMigrator.needsMigration(store, "migrateTest"));

        // When: Migrate
        DequeMigrator.migrate(store, "migrateTest", String.class);

        // Then: needsMigration returns false after
        assertFalse("After migration, should not need migration",
                    DequeMigrator.needsMigration(store, "migrateTest"));

        // And: Data preserved
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("A", tx.peekFirst(deque));
            assertEquals("C", tx.peekLast(deque));
            assertEquals(3, tx.size(deque));
        }
    }

    /**
     * M2: 마이그레이션 후 CollectionState가 ORDERED로 변경
     */
    @Test
    public void testMigrate_legacyDeque_updatesCollectionState() {
        // Given: LEGACY Deque
        Deque<String> deque = createLegacyDeque("stateTest", String.class);
        deque.addLast("test");
        FxDequeImpl<String> impl = (FxDequeImpl<String>) deque;
        long collectionId = impl.getCollectionId();

        // Verify: Before migration, state is LEGACY
        CollectionState beforeState = storeImpl.getCollectionStateById(collectionId);
        assertEquals("Before migration should be LEGACY",
                     CollectionState.SEQ_ENCODER_VERSION_LEGACY,
                     beforeState.getSeqEncoderVersion());

        // When: Migrate
        DequeMigrator.migrate(store, "stateTest", String.class);

        // Then: After migration, state is ORDERED
        CollectionState afterState = storeImpl.getCollectionStateById(collectionId);
        assertEquals("After migration should be ORDERED",
                     CollectionState.SEQ_ENCODER_VERSION_ORDERED,
                     afterState.getSeqEncoderVersion());
    }

    /**
     * M3: 마이그레이션 후 SeqEncoder가 OrderedSeqEncoder로 변경
     */
    @Test
    public void testMigrate_legacyDeque_changesSeqEncoder() {
        // Given: LEGACY Deque
        Deque<String> deque = createLegacyDeque("encoderTest", String.class);
        deque.addLast("test");
        FxDequeImpl<String> impl = (FxDequeImpl<String>) deque;

        // Verify: Before migration, uses LegacySeqEncoder
        assertTrue(impl.getSeqEncoder() instanceof LegacySeqEncoder);

        // When: Migrate
        DequeMigrator.migrate(store, "encoderTest", String.class);

        // Then: After migration, uses OrderedSeqEncoder
        // Note: We need to reopen the deque to get the updated encoder
        Deque<String> reopened = store.openDeque("encoderTest", String.class);
        FxDequeImpl<String> reopenedImpl = (FxDequeImpl<String>) reopened;
        assertTrue("After migration should use OrderedSeqEncoder",
                   reopenedImpl.getSeqEncoder() instanceof OrderedSeqEncoder);
    }

    /**
     * M4: addFirst/addLast 혼합 연산 후 마이그레이션
     */
    @Test
    public void testMigrate_legacyDeque_withMixedAddOperations() {
        // Given: LEGACY Deque with mixed operations
        Deque<String> deque = createLegacyDeque("mixedTest", String.class);
        deque.addLast("A");   // [A]
        deque.addLast("B");   // [A, B]
        deque.addFirst("Z");  // [Z, A, B]
        deque.addFirst("Y");  // [Y, Z, A, B]

        // When: Migrate
        DequeMigrator.migrate(store, "mixedTest", String.class);

        // Then: Order preserved
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("Y", tx.peekFirst(deque));
            assertEquals("B", tx.peekLast(deque));
        }

        // Verify full order
        List<String> expected = Arrays.asList("Y", "Z", "A", "B");
        List<String> actual = new ArrayList<>();
        for (String s : deque) {
            actual.add(s);
        }
        assertEquals("Order should be preserved after migration", expected, actual);
    }

    // ==================== D: 데이터 무결성 테스트 ====================

    /**
     * D1: 마이그레이션 후 요소 순서 보존
     */
    @Test
    public void testMigrate_preservesElementOrder() {
        // Given: LEGACY Deque with ordered elements
        Deque<Long> deque = createLegacyDeque("orderTest", Long.class);
        for (long i = 0; i < 100; i++) {
            deque.addLast(i);
        }

        // When: Migrate
        DequeMigrator.migrate(store, "orderTest", Long.class);

        // Then: Order preserved
        long expected = 0;
        for (Long actual : deque) {
            assertEquals("Element order should be preserved",
                         Long.valueOf(expected++), actual);
        }
    }

    /**
     * D2: 마이그레이션 후 요소 값 보존
     */
    @Test
    public void testMigrate_preservesElementValues() {
        // Given: LEGACY Deque with specific values
        Deque<String> deque = createLegacyDeque("valueTest", String.class);
        String[] values = {"Hello", "World", "FxStore", "마이그레이션"};
        for (String v : values) {
            deque.addLast(v);
        }

        // When: Migrate
        DequeMigrator.migrate(store, "valueTest", String.class);

        // Then: Values preserved
        int i = 0;
        for (String actual : deque) {
            assertEquals("Element value should be preserved", values[i++], actual);
        }
    }

    /**
     * D3: 마이그레이션 후 Deque 크기 보존
     */
    @Test
    public void testMigrate_preservesDequeSize() {
        // Given: LEGACY Deque with 50 elements
        Deque<Long> deque = createLegacyDeque("sizeTest", Long.class);
        for (long i = 0; i < 50; i++) {
            deque.addLast(i);
        }

        // When: Migrate
        DequeMigrator.migrate(store, "sizeTest", Long.class);

        // Then: Size preserved
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("Size should be preserved", 50, tx.size(deque));
        }
    }

    // ==================== E: 경계 조건 테스트 ====================

    /**
     * E1: 빈 LEGACY Deque 마이그레이션
     */
    @Test
    public void testMigrate_emptyLegacyDeque() {
        // Given: Empty LEGACY Deque
        Deque<String> deque = createLegacyDeque("emptyTest", String.class);

        // Verify: needsMigration should still return true
        assertTrue(DequeMigrator.needsMigration(store, "emptyTest"));

        // When: Migrate
        DequeMigrator.migrate(store, "emptyTest", String.class);

        // Then: Migration completes successfully
        assertFalse(DequeMigrator.needsMigration(store, "emptyTest"));

        try (FxReadTransaction tx = store.beginRead()) {
            assertNull(tx.peekFirst(deque));
            assertNull(tx.peekLast(deque));
            assertEquals(0, tx.size(deque));
        }
    }

    /**
     * E2: 단일 요소 LEGACY Deque 마이그레이션
     */
    @Test
    public void testMigrate_singleElementLegacyDeque() {
        // Given: LEGACY Deque with single element
        Deque<String> deque = createLegacyDeque("singleTest", String.class);
        deque.addLast("only");

        // When: Migrate
        DequeMigrator.migrate(store, "singleTest", String.class);

        // Then: Element preserved
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("only", tx.peekFirst(deque));
            assertEquals("only", tx.peekLast(deque));
            assertEquals(1, tx.size(deque));
        }
    }

    /**
     * E3: 대용량 LEGACY Deque 마이그레이션
     */
    @Test
    public void testMigrate_largeDeque() {
        // Given: LEGACY Deque with 200 elements
        Deque<Long> deque = createLegacyDeque("largeTest", Long.class);
        final int count = 200;
        for (long i = 0; i < count; i++) {
            deque.addLast(i);
        }

        // Verify before migration
        assertEquals("Before migration, size should be " + count, count, deque.size());

        // When: Migrate
        DequeMigrator.migrate(store, "largeTest", Long.class);

        // Then: needsMigration returns false
        assertFalse("After migration, should not need migration",
                    DequeMigrator.needsMigration(store, "largeTest"));

        // Verify using the original deque reference
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("Size should be preserved", count, tx.size(deque));
        }

        // Verify order via iteration
        long expected = 0;
        for (Long actual : deque) {
            assertEquals(Long.valueOf(expected++), actual);
        }
        assertEquals("All elements should be present", (long) count, expected);
    }

    // ==================== S: 상태 검증 테스트 ====================

    /**
     * S1: Store 재오픈 후 ORDERED 상태 유지
     */
    @Test
    public void testAfterMigration_reopenStorePreservesOrdered() throws Exception {
        // Given: Migrate LEGACY Deque
        Deque<String> deque = createLegacyDeque("reopenTest", String.class);
        deque.addLast("A");
        deque.addLast("B");
        DequeMigrator.migrate(store, "reopenTest", String.class);
        store.close();

        // When: Reopen store
        store = FxStore.open(tempFile.toPath());
        storeImpl = (FxStoreImpl) store;

        // Then: ORDERED state preserved
        assertFalse("After reopen, should not need migration",
                    DequeMigrator.needsMigration(store, "reopenTest"));

        Deque<String> reopened = store.openDeque("reopenTest", String.class);
        FxDequeImpl<String> impl = (FxDequeImpl<String>) reopened;
        assertTrue("Should use OrderedSeqEncoder after reopen",
                   impl.getSeqEncoder() instanceof OrderedSeqEncoder);

        // Data preserved
        try (FxReadTransaction tx = store.beginRead()) {
            assertEquals("A", tx.peekFirst(reopened));
            assertEquals("B", tx.peekLast(reopened));
        }
    }

    /**
     * S2: 마이그레이션 후 peekFirst/peekLast 성능 개선 (O(log n))
     */
    @Test
    public void testAfterMigration_peekOperationsAreEfficient() {
        // Given: Migrate LEGACY Deque with 1000 elements
        Deque<Integer> deque = createLegacyDeque("perfTest", Integer.class);
        for (int i = 0; i < 1000; i++) {
            deque.addLast(i);
        }
        DequeMigrator.migrate(store, "perfTest", Integer.class);

        // When: Measure peek performance
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

        System.out.printf("LEGACY→ORDERED migration peekFirst/peekLast (n=1000): %.2f ns/op%n", nsPerOp);

        // Then: O(log n) performance - should be under 50000 ns/op
        assertTrue("peek should be O(log n) after migration", nsPerOp < 50000);
    }
}
