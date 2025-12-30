package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.codec.I64Codec;
import com.snoworca.fxstore.ost.OST;
import com.snoworca.fxstore.storage.Allocator;
import com.snoworca.fxstore.storage.MemoryStorage;
import com.snoworca.fxstore.storage.Storage;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * FxList vs ArrayList Equivalence 테스트.
 * 
 * FxList와 Java 표준 ArrayList가 동일한 연산에 대해 동일한 결과를 내는지 검증합니다.
 */
public class ListEquivalenceTest {
    
    private Storage storage;
    private Allocator allocator;
    private OST ost;
    private FxList<Integer> fxList;
    private ArrayList<Integer> refList;
    private SimpleRecordStore recordStore;
    private static final int PAGE_SIZE = 4096;
    private Random random;
    
    @Before
    public void setUp() {
        storage = new MemoryStorage(10 * 1024 * 1024); // 10MB
        allocator = new Allocator(PAGE_SIZE, 12288);
        ost = OST.createEmpty(storage, allocator, PAGE_SIZE);
        recordStore = new SimpleRecordStore();
        
        // Integer용 코덱 - I64Codec 기반
        FxCodec<Integer> intCodec = new FxCodec<Integer>() {
            private final I64Codec delegate = I64Codec.INSTANCE;
            
            @Override
            public String id() {
                return delegate.id();
            }
            
            @Override
            public int version() {
                return delegate.version();
            }
            
            @Override
            public byte[] encode(Integer value) {
                return delegate.encode(value.longValue());
            }
            
            @Override
            public Integer decode(byte[] bytes) {
                return delegate.decode(bytes).intValue();
            }
            
            @Override
            public int compareBytes(byte[] a, byte[] b) {
                return delegate.compareBytes(a, b);
            }
            
            @Override
            public boolean equalsBytes(byte[] a, byte[] b) {
                return delegate.equalsBytes(a, b);
            }
            
            @Override
            public int hashBytes(byte[] bytes) {
                return delegate.hashBytes(bytes);
            }
        };
        
        fxList = new FxList<>(ost, intCodec, recordStore);
        refList = new ArrayList<>();
        random = new Random(42); // 고정 시드
    }
    
    /**
     * 시나리오 12.1: 동일 연산 시퀀스.
     */
    @Test
    public void testEquivalence_IdenticalOperations() {
        // 100번 동일 연산 수행
        for (int i = 0; i < 100; i++) {
            int op = i % 4;
            
            switch (op) {
                case 0: // add(element)
                    fxList.add(i);
                    refList.add(i);
                    break;
                    
                case 1: // add(index, element)
                    if (!fxList.isEmpty()) {
                        int idx = i % (fxList.size() + 1);
                        fxList.add(idx, i + 1000);
                        refList.add(idx, i + 1000);
                    }
                    break;
                    
                case 2: // remove(index)
                    if (!fxList.isEmpty()) {
                        int idx = i % fxList.size();
                        Integer fxRemoved = fxList.remove(idx);
                        Integer refRemoved = refList.remove(idx);
                        assertEquals(refRemoved, fxRemoved);
                    }
                    break;
                    
                case 3: // set(index, element)
                    if (!fxList.isEmpty()) {
                        int idx = i % fxList.size();
                        Integer fxOld = fxList.set(idx, i + 2000);
                        Integer refOld = refList.set(idx, i + 2000);
                        assertEquals(refOld, fxOld);
                    }
                    break;
            }
        }
        
        // 최종 검증
        assertListsEqual();
    }
    
    /**
     * 시나리오 12.2: 랜덤 연산 1000회.
     */
    @Test
    public void testEquivalence_RandomOperations() {
        for (int i = 0; i < 1000; i++) {
            int op = random.nextInt(100);
            
            if (op < 70) {
                // 70% add
                int value = random.nextInt(10000);
                fxList.add(value);
                refList.add(value);
                
            } else if (op < 90 && !fxList.isEmpty()) {
                // 20% remove
                int idx = random.nextInt(fxList.size());
                Integer fxRemoved = fxList.remove(idx);
                Integer refRemoved = refList.remove(idx);
                assertEquals("Remove mismatch at iteration " + i, refRemoved, fxRemoved);
                
            } else if (!fxList.isEmpty()) {
                // 10% set
                int idx = random.nextInt(fxList.size());
                int value = random.nextInt(10000);
                Integer fxOld = fxList.set(idx, value);
                Integer refOld = refList.set(idx, value);
                assertEquals("Set mismatch at iteration " + i, refOld, fxOld);
            }
            
            // 매 100회마다 검증
            if (i % 100 == 0) {
                assertListsEqual();
            }
        }
        
        // 최종 검증
        assertListsEqual();
    }
    
    /**
     * 시나리오 12.3: Iterator 동작 일치.
     */
    @Test
    public void testEquivalence_Iterator() {
        // 데이터 준비
        for (int i = 0; i < 50; i++) {
            fxList.add(i);
            refList.add(i);
        }
        
        // Iterator 순회
        Iterator<Integer> fxIt = fxList.iterator();
        Iterator<Integer> refIt = refList.iterator();
        
        while (refIt.hasNext()) {
            assertTrue("FxList iterator should have next", fxIt.hasNext());
            assertEquals(refIt.next(), fxIt.next());
        }
        
        assertFalse("FxList iterator should not have next", fxIt.hasNext());
    }
    
    /**
     * 시나리오 12.4: iterator.remove() - 동시성 안전을 위해 미지원.
     * FxList의 Iterator는 스냅샷 기반으로 동작하여 remove()를 지원하지 않습니다.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testEquivalence_IteratorRemove() {
        // 데이터 준비
        for (int i = 0; i < 20; i++) {
            fxList.add(i);
        }

        // FxList iterator.remove()는 UnsupportedOperationException 발생
        Iterator<Integer> fxIt = fxList.iterator();
        fxIt.next();
        fxIt.remove(); // 예외 발생
    }
    
    /**
     * 시나리오 12.5: subList 일치.
     */
    @Test
    public void testEquivalence_SubList() {
        // 데이터 준비 [0, 1, 2, ..., 9]
        for (int i = 0; i < 10; i++) {
            fxList.add(i);
            refList.add(i);
        }
        
        // subList(2, 7)
        List<Integer> fxSub = fxList.subList(2, 7);
        List<Integer> refSub = refList.subList(2, 7);
        
        assertEquals(refSub.size(), fxSub.size());
        for (int i = 0; i < refSub.size(); i++) {
            assertEquals(refSub.get(i), fxSub.get(i));
        }
    }
    
    /**
     * 시나리오 12.6: contains/indexOf 일치.
     */
    @Test
    public void testEquivalence_Search() {
        for (int i = 0; i < 20; i++) {
            fxList.add(i % 5); // [0,1,2,3,4,0,1,2,3,4,...]
            refList.add(i % 5);
        }
        
        for (int target = 0; target < 10; target++) {
            assertEquals(
                "contains(" + target + ")",
                refList.contains(target),
                fxList.contains(target)
            );
            
            assertEquals(
                "indexOf(" + target + ")",
                refList.indexOf(target),
                fxList.indexOf(target)
            );
            
            assertEquals(
                "lastIndexOf(" + target + ")",
                refList.lastIndexOf(target),
                fxList.lastIndexOf(target)
            );
        }
    }
    
    /**
     * 시나리오 12.7: toArray 일치.
     */
    @Test
    public void testEquivalence_ToArray() {
        for (int i = 0; i < 30; i++) {
            fxList.add(i);
            refList.add(i);
        }
        
        Object[] fxArray = fxList.toArray();
        Object[] refArray = refList.toArray();
        
        assertArrayEquals(refArray, fxArray);
    }
    
    /**
     * FxList와 ArrayList가 동일한지 검증.
     */
    private void assertListsEqual() {
        assertEquals("Size mismatch", refList.size(), fxList.size());
        
        for (int i = 0; i < refList.size(); i++) {
            assertEquals(
                "Element mismatch at index " + i,
                refList.get(i),
                fxList.get(i)
            );
        }
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
