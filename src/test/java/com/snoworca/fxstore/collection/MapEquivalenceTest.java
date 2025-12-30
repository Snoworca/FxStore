package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.*;
import com.snoworca.fxstore.core.FxStoreImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * FxNavigableMap vs TreeMap Equivalence 테스트.
 *
 * FxNavigableMap과 Java 표준 TreeMap이 동일한 연산에 대해 동일한 결과를 내는지 검증합니다.
 */
public class MapEquivalenceTest {

    private FxStore store;
    private NavigableMap<Long, String> fxMap;
    private TreeMap<Long, String> refMap;
    private Random random;

    @Before
    public void setUp() {
        store = FxStoreImpl.openMemory(FxOptions.defaults());
        fxMap = store.createMap("testMap", Long.class, String.class);
        refMap = new TreeMap<>();
        random = new Random(42);
    }

    @After
    public void tearDown() {
        if (store != null) {
            try { store.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== 기본 연산 ====================

    @Test
    public void testEquivalence_PutGetRemove() {
        for (int i = 0; i < 100; i++) {
            Long key = (long) i;
            String value = "value" + i;

            assertEquals(refMap.put(key, value), fxMap.put(key, value));
        }

        assertMapsEqual();

        // Get
        for (int i = 0; i < 100; i++) {
            Long key = (long) i;
            assertEquals(refMap.get(key), fxMap.get(key));
        }

        // Remove
        for (int i = 0; i < 50; i++) {
            Long key = (long) (i * 2);
            assertEquals(refMap.remove(key), fxMap.remove(key));
        }

        assertMapsEqual();
    }

    @Test
    public void testEquivalence_ContainsKey() {
        for (int i = 0; i < 50; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(refMap.containsKey((long) i), fxMap.containsKey((long) i));
        }
    }

    @Test
    public void testEquivalence_ContainsValue() {
        for (int i = 0; i < 50; i++) {
            fxMap.put((long) i, "v" + (i % 10));
            refMap.put((long) i, "v" + (i % 10));
        }

        for (int i = 0; i < 20; i++) {
            String v = "v" + i;
            assertEquals(refMap.containsValue(v), fxMap.containsValue(v));
        }
    }

    // ==================== NavigableMap 연산 ====================

    @Test
    public void testEquivalence_FirstLastKey() {
        // Use deterministic values to avoid random differences
        for (int i = 0; i < 50; i++) {
            Long key = (long) i;
            String value = "v" + i;
            fxMap.put(key, value);
            refMap.put(key, value);
        }

        assertEquals(refMap.firstKey(), fxMap.firstKey());
        assertEquals(refMap.lastKey(), fxMap.lastKey());
    }

    @Test
    public void testEquivalence_FirstLastEntry() {
        for (int i = 0; i < 50; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        assertEquals(refMap.firstEntry().getKey(), fxMap.firstEntry().getKey());
        assertEquals(refMap.firstEntry().getValue(), fxMap.firstEntry().getValue());
        assertEquals(refMap.lastEntry().getKey(), fxMap.lastEntry().getKey());
        assertEquals(refMap.lastEntry().getValue(), fxMap.lastEntry().getValue());
    }

    @Test
    public void testEquivalence_FloorCeilingKey() {
        long[] keys = {10L, 20L, 30L, 40L, 50L};
        for (long key : keys) {
            fxMap.put(key, "v" + key);
            refMap.put(key, "v" + key);
        }

        // Test various floor/ceiling cases
        for (long k = 0; k <= 60; k += 5) {
            assertEquals("floorKey(" + k + ")", refMap.floorKey(k), fxMap.floorKey(k));
            assertEquals("ceilingKey(" + k + ")", refMap.ceilingKey(k), fxMap.ceilingKey(k));
            assertEquals("lowerKey(" + k + ")", refMap.lowerKey(k), fxMap.lowerKey(k));
            assertEquals("higherKey(" + k + ")", refMap.higherKey(k), fxMap.higherKey(k));
        }
    }

    @Test
    public void testEquivalence_FloorCeilingEntry() {
        long[] keys = {10L, 20L, 30L, 40L, 50L};
        for (long key : keys) {
            fxMap.put(key, "v" + key);
            refMap.put(key, "v" + key);
        }

        for (long k = 0; k <= 60; k += 5) {
            Map.Entry<Long, String> refFloor = refMap.floorEntry(k);
            Map.Entry<Long, String> fxFloor = fxMap.floorEntry(k);

            if (refFloor == null) {
                assertNull("floorEntry(" + k + ")", fxFloor);
            } else {
                assertNotNull("floorEntry(" + k + ")", fxFloor);
                assertEquals(refFloor.getKey(), fxFloor.getKey());
                assertEquals(refFloor.getValue(), fxFloor.getValue());
            }

            Map.Entry<Long, String> refCeiling = refMap.ceilingEntry(k);
            Map.Entry<Long, String> fxCeiling = fxMap.ceilingEntry(k);

            if (refCeiling == null) {
                assertNull("ceilingEntry(" + k + ")", fxCeiling);
            } else {
                assertNotNull("ceilingEntry(" + k + ")", fxCeiling);
                assertEquals(refCeiling.getKey(), fxCeiling.getKey());
                assertEquals(refCeiling.getValue(), fxCeiling.getValue());
            }
        }
    }

    // ==================== 서브맵 연산 ====================

    @Test
    public void testEquivalence_SubMap() {
        for (int i = 0; i < 100; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        NavigableMap<Long, String> fxSub = fxMap.subMap(20L, true, 80L, false);
        NavigableMap<Long, String> refSub = refMap.subMap(20L, true, 80L, false);

        assertEquals(refSub.size(), fxSub.size());
        assertEquals(refSub.firstKey(), fxSub.firstKey());
        assertEquals(refSub.lastKey(), fxSub.lastKey());
    }

    @Test
    public void testEquivalence_HeadMap() {
        for (int i = 0; i < 50; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        NavigableMap<Long, String> fxHead = fxMap.headMap(25L, false);
        NavigableMap<Long, String> refHead = refMap.headMap(25L, false);

        assertEquals(refHead.size(), fxHead.size());
        assertEquals(refHead.firstKey(), fxHead.firstKey());
        assertEquals(refHead.lastKey(), fxHead.lastKey());
    }

    @Test
    public void testEquivalence_TailMap() {
        for (int i = 0; i < 50; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        NavigableMap<Long, String> fxTail = fxMap.tailMap(25L, true);
        NavigableMap<Long, String> refTail = refMap.tailMap(25L, true);

        assertEquals(refTail.size(), fxTail.size());
        assertEquals(refTail.firstKey(), fxTail.firstKey());
        assertEquals(refTail.lastKey(), fxTail.lastKey());
    }

    // ==================== 뷰 연산 ====================

    @Test
    public void testEquivalence_KeySet() {
        for (int i = 0; i < 30; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        Set<Long> fxKeys = fxMap.keySet();
        Set<Long> refKeys = refMap.keySet();

        assertEquals(refKeys.size(), fxKeys.size());
        assertTrue(fxKeys.containsAll(refKeys));
        assertTrue(refKeys.containsAll(fxKeys));
    }

    @Test
    public void testEquivalence_Values() {
        for (int i = 0; i < 30; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        Collection<String> fxValues = fxMap.values();
        Collection<String> refValues = refMap.values();

        assertEquals(refValues.size(), fxValues.size());
    }

    @Test
    public void testEquivalence_EntrySet() {
        for (int i = 0; i < 30; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        Set<Map.Entry<Long, String>> fxEntries = fxMap.entrySet();
        Set<Map.Entry<Long, String>> refEntries = refMap.entrySet();

        assertEquals(refEntries.size(), fxEntries.size());
    }

    // ==================== 랜덤 연산 ====================

    @Test
    public void testEquivalence_RandomOperations() {
        for (int i = 0; i < 1000; i++) {
            int op = random.nextInt(100);
            Long key = (long) random.nextInt(200);
            String value = "v" + random.nextInt(1000);

            if (op < 50) {
                // 50% put
                assertEquals(refMap.put(key, value), fxMap.put(key, value));
            } else if (op < 70) {
                // 20% get
                assertEquals(refMap.get(key), fxMap.get(key));
            } else if (op < 90) {
                // 20% remove
                assertEquals(refMap.remove(key), fxMap.remove(key));
            } else {
                // 10% containsKey
                assertEquals(refMap.containsKey(key), fxMap.containsKey(key));
            }

            if (i % 100 == 0) {
                assertEquals(refMap.size(), fxMap.size());
            }
        }

        assertMapsEqual();
    }

    @Test
    public void testEquivalence_Iterator() {
        for (int i = 0; i < 50; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        Iterator<Map.Entry<Long, String>> fxIt = fxMap.entrySet().iterator();
        Iterator<Map.Entry<Long, String>> refIt = refMap.entrySet().iterator();

        while (refIt.hasNext()) {
            assertTrue(fxIt.hasNext());
            Map.Entry<Long, String> refEntry = refIt.next();
            Map.Entry<Long, String> fxEntry = fxIt.next();
            assertEquals(refEntry.getKey(), fxEntry.getKey());
            assertEquals(refEntry.getValue(), fxEntry.getValue());
        }

        assertFalse(fxIt.hasNext());
    }

    @Test
    public void testEquivalence_DescendingMap() {
        for (int i = 0; i < 30; i++) {
            fxMap.put((long) i, "v" + i);
            refMap.put((long) i, "v" + i);
        }

        NavigableMap<Long, String> fxDesc = fxMap.descendingMap();
        NavigableMap<Long, String> refDesc = refMap.descendingMap();

        assertEquals(refDesc.size(), fxDesc.size());
        assertEquals(refDesc.firstKey(), fxDesc.firstKey());
        assertEquals(refDesc.lastKey(), fxDesc.lastKey());
    }

    // ==================== 엣지 케이스 ====================

    @Test
    public void testEquivalence_EmptyMap() {
        assertTrue(fxMap.isEmpty());
        assertTrue(refMap.isEmpty());
        assertEquals(refMap.size(), fxMap.size());
        assertNull(fxMap.get(1L));
        assertNull(refMap.get(1L));
    }

    @Test
    public void testEquivalence_SingleElement() {
        fxMap.put(42L, "answer");
        refMap.put(42L, "answer");

        assertEquals(refMap.size(), fxMap.size());
        assertEquals(refMap.firstKey(), fxMap.firstKey());
        assertEquals(refMap.lastKey(), fxMap.lastKey());
        assertEquals(refMap.get(42L), fxMap.get(42L));
    }

    @Test
    public void testEquivalence_UpdateValue() {
        fxMap.put(1L, "old");
        refMap.put(1L, "old");

        assertEquals(refMap.put(1L, "new"), fxMap.put(1L, "new"));
        assertEquals(refMap.get(1L), fxMap.get(1L));
    }

    // ==================== 헬퍼 메서드 ====================

    private void assertMapsEqual() {
        assertEquals("Size mismatch", refMap.size(), fxMap.size());

        for (Map.Entry<Long, String> entry : refMap.entrySet()) {
            assertEquals(
                "Value mismatch for key " + entry.getKey(),
                entry.getValue(),
                fxMap.get(entry.getKey())
            );
        }
    }
}
