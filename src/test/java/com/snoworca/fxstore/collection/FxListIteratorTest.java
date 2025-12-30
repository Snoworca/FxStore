package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.codec.StringCodec;
import com.snoworca.fxstore.ost.OST;
import com.snoworca.fxstore.storage.Allocator;
import com.snoworca.fxstore.storage.MemoryStorage;
import com.snoworca.fxstore.storage.Storage;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * FxList Iterator 테스트.
 */
public class FxListIteratorTest {
    
    private Storage storage;
    private Allocator allocator;
    private OST ost;
    private FxList<String> list;
    private SimpleRecordStore recordStore;
    private static final int PAGE_SIZE = 4096;
    
    @Before
    public void setUp() {
        storage = new MemoryStorage(1024 * 1024);
        allocator = new Allocator(PAGE_SIZE, 12288);
        ost = OST.createEmpty(storage, allocator, PAGE_SIZE);
        recordStore = new SimpleRecordStore();
        
        FxCodec<String> stringCodec = StringCodec.INSTANCE;
        list = new FxList<>(ost, stringCodec, recordStore);
    }
    
    /**
     * 시나리오 9.1: 순방향 iterator.
     */
    @Test
    public void testIterator_Forward() {
        // Given: ["A", "B", "C"]
        list.add("A");
        list.add("B");
        list.add("C");
        
        // When
        Iterator<String> it = list.iterator();
        
        // Then
        assertTrue(it.hasNext());
        assertEquals("A", it.next());
        assertTrue(it.hasNext());
        assertEquals("B", it.next());
        assertTrue(it.hasNext());
        assertEquals("C", it.next());
        assertFalse(it.hasNext());
    }
    
    /**
     * 시나리오 9.2: listIterator 양방향.
     */
    @Test
    public void testListIterator_Bidirectional() {
        // Given: [1, 2, 3, 4, 5]
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");
        
        // When: listIterator(2)
        ListIterator<String> it = list.listIterator(2);
        
        // Then
        assertTrue(it.hasNext());
        assertEquals("3", it.next());
        
        assertTrue(it.hasPrevious());
        assertEquals("3", it.previous());
        
        assertTrue(it.hasPrevious());
        assertEquals("2", it.previous());
        
        assertEquals(1, it.nextIndex());
        assertEquals(0, it.previousIndex());
    }
    
    /**
     * 시나리오 9.3: iterator.remove() - 동시성 안전을 위해 미지원.
     * 스냅샷 기반 Iterator는 수정 연산을 지원하지 않습니다.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testIterator_Remove() {
        // Given: ["A", "B", "C"]
        list.add("A");
        list.add("B");
        list.add("C");

        // When: remove는 UnsupportedOperationException 발생
        Iterator<String> it = list.iterator();
        it.next(); // "A"
        it.remove(); // 예외 발생
    }
    
    /**
     * 시나리오 9.4: listIterator.set() - 동시성 안전을 위해 미지원.
     * 스냅샷 기반 Iterator는 수정 연산을 지원하지 않습니다.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testListIterator_Set() {
        // Given: ["A", "B", "C"]
        list.add("A");
        list.add("B");
        list.add("C");

        // When: set은 UnsupportedOperationException 발생
        ListIterator<String> it = list.listIterator();
        it.next(); // "A"
        it.set("X"); // 예외 발생
    }
    
    /**
     * 시나리오 9.5: listIterator.add() - 동시성 안전을 위해 미지원.
     * 스냅샷 기반 Iterator는 수정 연산을 지원하지 않습니다.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testListIterator_Add() {
        // Given: ["A", "C"]
        list.add("A");
        list.add("C");

        // When: add는 UnsupportedOperationException 발생
        ListIterator<String> it = list.listIterator(1);
        it.add("B"); // 예외 발생
    }
    
    /**
     * 시나리오 9.6: iterator 빈 리스트.
     */
    @Test
    public void testIterator_EmptyList() {
        Iterator<String> it = list.iterator();
        assertFalse(it.hasNext());
    }
    
    /**
     * 시나리오 9.7: iterator NoSuchElementException.
     */
    @Test(expected = NoSuchElementException.class)
    public void testIterator_NoSuchElement() {
        list.add("A");
        
        Iterator<String> it = list.iterator();
        it.next(); // "A"
        it.next(); // 예외 발생
    }
    
    /**
     * 시나리오 9.8: iterator remove - UnsupportedOperationException.
     * 스냅샷 기반 Iterator는 remove를 지원하지 않습니다.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testIterator_Remove_IllegalState() {
        list.add("A");
        list.add("B");

        Iterator<String> it = list.iterator();
        it.remove(); // 항상 UnsupportedOperationException 발생
    }
    
    /**
     * 시나리오 9.9: enhanced for loop.
     */
    @Test
    public void testIterator_EnhancedForLoop() {
        list.add("A");
        list.add("B");
        list.add("C");

        List<String> collected = new ArrayList<>();
        for (String s : list) {
            collected.add(s);
        }

        assertEquals(3, collected.size());
        assertEquals("A", collected.get(0));
        assertEquals("B", collected.get(1));
        assertEquals("C", collected.get(2));
    }

    /**
     * 시나리오 9.10: listIterator previous() NoSuchElementException.
     *
     * 이 테스트는 FxListIterator.previous() 메서드의 line 174-175를 커버합니다:
     * - !hasPrevious() 조건에서 NoSuchElementException 발생
     */
    @Test(expected = NoSuchElementException.class)
    public void testListIterator_Previous_NoSuchElement() {
        // Given: ["A", "B"]
        list.add("A");
        list.add("B");

        // When: listIterator at index 0 (no previous elements)
        ListIterator<String> it = list.listIterator(0);
        assertFalse(it.hasPrevious()); // 확인: previous 없음

        // Then: previous() 호출 시 예외 발생
        it.previous();
    }

    /**
     * 시나리오 9.11: listIterator set() IllegalStateException.
     *
     * 이 테스트는 FxListIterator.set() 메서드의 line 206-207을 커버합니다:
     * - lastRet < 0 조건에서 IllegalStateException 발생
     */
    /**
     * set() - 동시성 안전을 위해 미지원.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testListIterator_Set_IllegalState() {
        // Given: ["A", "B"]
        list.add("A");
        list.add("B");

        // When: set은 항상 UnsupportedOperationException 발생
        ListIterator<String> it = list.listIterator();
        it.set("X"); // 예외 발생
    }

    /**
     * 간단한 RecordStore 구현.
     */
    private static class SimpleRecordStore implements FxList.RecordStore {
        private final Map<Long, byte[]> records = new HashMap<>();
        private long nextId = 1L;
        
        @Override
        public long writeRecord(byte[] data) {
            long id = nextId++;
            records.put(id, Arrays.copyOf(data, data.length));
            return id;
        }
        
        @Override
        public byte[] readRecord(long recordId) {
            byte[] data = records.get(recordId);
            if (data == null) {
                throw new IllegalArgumentException("Record not found: " + recordId);
            }
            return data;
        }
        
        @Override
        public void deleteRecord(long recordId) {
            records.remove(recordId);
        }
    }
}
