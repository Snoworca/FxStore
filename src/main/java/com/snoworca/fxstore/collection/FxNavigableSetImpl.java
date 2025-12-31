package com.snoworca.fxstore.collection;

import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.core.CodecUpgradeContext;
import com.snoworca.fxstore.core.FxStoreImpl;

import java.util.*;
import java.util.Objects;

/**
 * NavigableMap 기반 NavigableSet 구현
 *
 * <p>내부적으로 FxNavigableMap을 사용하며, 값은 Boolean.TRUE로 고정
 *
 * <p>SOLID 준수:
 * - SRP: NavigableSet 연산만 담당
 * - DIP: NavigableMap에 위임
 *
 * @param <E> 원소 타입
 */
public class FxNavigableSetImpl<E> implements NavigableSet<E>, FxCollection {

    private final NavigableMap<E, Boolean> map;

    /**
     * 생성자
     *
     * @param store FxStore 구현
     * @param collectionId 컬렉션 ID
     * @param elementCodec 원소 코덱
     * @param comparator 원소 비교자
     * @param elementUpgradeContext 원소 업그레이드 컨텍스트 (null 가능)
     */
    public FxNavigableSetImpl(FxStoreImpl store, long collectionId,
                              FxCodec<E> elementCodec, Comparator<E> comparator,
                              CodecUpgradeContext elementUpgradeContext) {
        // Boolean.TRUE를 값으로 사용하는 더미 코덱
        FxCodec<Boolean> dummyCodec = new FxCodec<Boolean>() {
            @Override
            public String id() {
                return "BOOL";
            }

            @Override
            public int version() {
                return 1;
            }

            @Override
            public byte[] encode(Boolean value) {
                return new byte[]{1};
            }

            @Override
            public Boolean decode(byte[] bytes) {
                return Boolean.TRUE;
            }

            @Override
            public int compareBytes(byte[] a, byte[] b) {
                return 0;
            }

            @Override
            public boolean equalsBytes(byte[] a, byte[] b) {
                return true;
            }

            @Override
            public int hashBytes(byte[] bytes) {
                return 1;
            }
        };

        // Set의 요소는 Map의 키로 저장되므로 elementUpgradeContext는 keyUpgradeContext로 전달
        this.map = new FxNavigableMapImpl<E, Boolean>(
            store, collectionId, elementCodec, dummyCodec, comparator,
            elementUpgradeContext, null);
    }

    // ==================== FxCollection 구현 ====================

    @Override
    public long getCollectionId() {
        return ((FxCollection) map).getCollectionId();
    }

    @Override
    public com.snoworca.fxstore.core.FxStoreImpl getStore() {
        return ((FxCollection) map).getStore();
    }
    
    @Override
    public boolean add(E e) {
        return map.put(e, Boolean.TRUE) == null;
    }
    
    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }
    
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }
    
    @Override
    public int size() {
        return map.size();
    }
    
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
    
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }
    
    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }
    
    @Override
    public <T> T[] toArray(T[] a) {
        return map.keySet().toArray(a);
    }
    
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }
    
    /**
     * 지정된 컬렉션에 포함된 요소만 유지합니다.
     *
     * <p>BUG-V11-001 수정: iterator().remove() 대신 map.remove() 직접 호출
     *
     * @param c 유지할 요소를 포함하는 컬렉션
     * @return 집합이 변경되면 true
     * @throws NullPointerException c가 null인 경우
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c, "Collection cannot be null");

        // 1단계: 삭제할 요소 수집
        List<E> toRemove = new ArrayList<>();
        for (E element : this) {
            if (!c.contains(element)) {
                toRemove.add(element);
            }
        }

        // 2단계: 수집된 요소 삭제 (map.remove() 직접 호출)
        boolean modified = false;
        for (E element : toRemove) {
            if (map.remove(element) != null) {
                modified = true;
            }
        }

        return modified;
    }
    
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            if (remove(o)) {
                modified = true;
            }
        }
        return modified;
    }
    
    @Override
    public void clear() {
        map.clear();
    }
    
    @Override
    public Comparator<? super E> comparator() {
        return map.comparator();
    }
    
    @Override
    public E first() {
        return map.firstKey();
    }
    
    @Override
    public E last() {
        return map.lastKey();
    }
    
    @Override
    public E lower(E e) {
        return map.lowerKey(e);
    }
    
    @Override
    public E floor(E e) {
        return map.floorKey(e);
    }
    
    @Override
    public E ceiling(E e) {
        return map.ceilingKey(e);
    }
    
    @Override
    public E higher(E e) {
        return map.higherKey(e);
    }
    
    @Override
    public E pollFirst() {
        Map.Entry<E, Boolean> entry = map.pollFirstEntry();
        return entry != null ? entry.getKey() : null;
    }
    
    @Override
    public E pollLast() {
        Map.Entry<E, Boolean> entry = map.pollLastEntry();
        return entry != null ? entry.getKey() : null;
    }
    
    /**
     * 이 집합의 역순 뷰를 반환합니다.
     *
     * <p>시간 복잡도: O(1) - 뷰 생성
     * <p>공간 복잡도: O(1) - 래퍼 객체만 생성
     *
     * <p>반환된 Set은 읽기 전용이며, 수정 연산은
     * {@link UnsupportedOperationException}을 던집니다.
     *
     * @return 내림차순으로 정렬된 뷰
     */
    @Override
    public NavigableSet<E> descendingSet() {
        return new DescendingSetView<>(this);
    }

    /**
     * 이 집합의 요소를 내림차순으로 순회하는 Iterator를 반환합니다.
     *
     * <p>시간 복잡도: O(N) - 전체 요소 수집 필요
     * <p>공간 복잡도: O(N) - 요소 목록 임시 저장
     *
     * <p>Iterator 특성:
     * <ul>
     *   <li>remove() 지원 안 함 (UnsupportedOperationException)</li>
     *   <li>스냅샷 기반: 생성 후 원본 변경 반영 안 됨</li>
     * </ul>
     *
     * @return 내림차순 Iterator
     */
    @Override
    public Iterator<E> descendingIterator() {
        List<E> snapshot = new ArrayList<>();
        for (E element : this) {
            snapshot.add(element);
        }
        Collections.reverse(snapshot);
        return Collections.unmodifiableList(snapshot).iterator();
    }
    
    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }
    
    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }
    
    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }
    
    /**
     * 이 집합에서 fromElement부터 toElement 범위의 부분 뷰를 반환합니다.
     *
     * <p>반환된 Set은 읽기 전용입니다.
     *
     * <p>시간 복잡도: O(1) - 뷰 생성
     * <p>공간 복잡도: O(1) - 래퍼 객체만 생성
     *
     * @param fromElement 범위 시작 요소 (null 불가)
     * @param fromInclusive fromElement 포함 여부
     * @param toElement 범위 종료 요소 (null 불가)
     * @param toInclusive toElement 포함 여부
     * @return 범위 내 요소만 포함하는 읽기 전용 뷰
     * @throws NullPointerException fromElement 또는 toElement가 null인 경우
     * @throws IllegalArgumentException fromElement > toElement인 경우
     */
    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                  E toElement, boolean toInclusive) {
        Objects.requireNonNull(fromElement, "fromElement cannot be null");
        Objects.requireNonNull(toElement, "toElement cannot be null");

        @SuppressWarnings("unchecked")
        Comparator<? super E> cmp = (Comparator<? super E>) map.comparator();
        if (cmp.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException(
                "fromElement > toElement: " + fromElement + " > " + toElement);
        }

        return new SubSetView<>(this, fromElement, fromInclusive, toElement, toInclusive);
    }

    /**
     * 이 집합에서 toElement보다 작은 요소들의 부분 뷰를 반환합니다.
     *
     * <p>반환된 Set은 읽기 전용입니다.
     *
     * @param toElement 범위 종료 요소 (null 불가)
     * @param inclusive toElement 포함 여부
     * @return 범위 내 요소만 포함하는 읽기 전용 뷰
     * @throws NullPointerException toElement가 null인 경우
     */
    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        Objects.requireNonNull(toElement, "toElement cannot be null");
        return new HeadSetView<>(this, toElement, inclusive);
    }

    /**
     * 이 집합에서 fromElement보다 크거나 같은 요소들의 부분 뷰를 반환합니다.
     *
     * <p>반환된 Set은 읽기 전용입니다.
     *
     * @param fromElement 범위 시작 요소 (null 불가)
     * @param inclusive fromElement 포함 여부
     * @return 범위 내 요소만 포함하는 읽기 전용 뷰
     * @throws NullPointerException fromElement가 null인 경우
     */
    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        Objects.requireNonNull(fromElement, "fromElement cannot be null");
        return new TailSetView<>(this, fromElement, inclusive);
    }

    // =====================================================================
    // 내부 뷰 클래스들
    // =====================================================================

    /**
     * 역순 Set 뷰
     *
     * <p>UOE 개선: 수정 연산(add, remove, clear), poll 연산, 뷰 생성 연산 지원
     */
    private static class DescendingSetView<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final FxNavigableSetImpl<E> parent;

        DescendingSetView(FxNavigableSetImpl<E> parent) {
            this.parent = parent;
        }

        @Override
        public int size() {
            return parent.size();
        }

        @Override
        public boolean contains(Object o) {
            return parent.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            return parent.descendingIterator();
        }

        @Override
        public Iterator<E> descendingIterator() {
            return parent.iterator();
        }

        /**
         * UOE 개선: parent에 직접 위임
         */
        @Override
        public boolean add(E e) {
            return parent.add(e);
        }

        /**
         * UOE 개선: parent에 직접 위임
         */
        @Override
        public boolean remove(Object o) {
            return parent.remove(o);
        }

        /**
         * UOE 개선: parent에 직접 위임
         */
        @Override
        public void clear() {
            parent.clear();
        }

        /**
         * BUG-V11-001 수정: retainAll() 오버라이드
         */
        @Override
        public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c, "Collection cannot be null");
            List<E> toRemove = new ArrayList<>();
            for (E element : this) {
                if (!c.contains(element)) {
                    toRemove.add(element);
                }
            }
            boolean modified = false;
            for (E element : toRemove) {
                if (remove(element)) {
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public Comparator<? super E> comparator() {
            return Collections.reverseOrder(parent.comparator());
        }

        @Override
        public E first() {
            return parent.last();
        }

        @Override
        public E last() {
            return parent.first();
        }

        @Override
        public E lower(E e) {
            return parent.higher(e);
        }

        @Override
        public E floor(E e) {
            return parent.ceiling(e);
        }

        @Override
        public E ceiling(E e) {
            return parent.floor(e);
        }

        @Override
        public E higher(E e) {
            return parent.lower(e);
        }

        /**
         * UOE 개선: descending에서 pollFirst = parent.pollLast
         */
        @Override
        public E pollFirst() {
            return parent.pollLast();
        }

        /**
         * UOE 개선: descending에서 pollLast = parent.pollFirst
         */
        @Override
        public E pollLast() {
            return parent.pollFirst();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return parent;
        }

        /**
         * UOE 개선: descending subSet(from, to) = parent.subSet(to, from).descendingSet()
         */
        @Override
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return parent.subSet(toElement, toInclusive, fromElement, fromInclusive).descendingSet();
        }

        /**
         * UOE 개선: descending headSet(to) = parent.tailSet(to).descendingSet()
         */
        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return parent.tailSet(toElement, inclusive).descendingSet();
        }

        /**
         * UOE 개선: descending tailSet(from) = parent.headSet(from).descendingSet()
         */
        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return parent.headSet(fromElement, inclusive).descendingSet();
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
    }

    /**
     * SubSet 뷰
     *
     * <p>UOE 개선: 수정 연산, poll 연산, 중첩 뷰 생성 지원
     */
    private static class SubSetView<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final FxNavigableSetImpl<E> parent;
        private final E fromElement, toElement;
        private final boolean fromInclusive, toInclusive;

        SubSetView(FxNavigableSetImpl<E> parent, E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            this.parent = parent;
            this.fromElement = fromElement;
            this.fromInclusive = fromInclusive;
            this.toElement = toElement;
            this.toInclusive = toInclusive;
        }

        // === 범위 검증 유틸리티 (UOE 개선) ===

        @SuppressWarnings("unchecked")
        private int compare(E e1, E e2) {
            Comparator<? super E> cmp = (Comparator<? super E>) parent.comparator();
            if (cmp != null) {
                return cmp.compare(e1, e2);
            }
            return ((Comparable<? super E>) e1).compareTo(e2);
        }

        private boolean tooLow(E element) {
            int c = compare(element, fromElement);
            return c < 0 || (c == 0 && !fromInclusive);
        }

        private boolean tooHigh(E element) {
            int c = compare(element, toElement);
            return c > 0 || (c == 0 && !toInclusive);
        }

        private boolean inRange(E element) {
            if (element == null) return false;
            return !tooLow(element) && !tooHigh(element);
        }

        private void checkInRange(E element) {
            if (!inRange(element)) {
                throw new IllegalArgumentException("element out of range: " + element);
            }
        }

        @Override
        public int size() {
            int count = 0;
            for (E e : parent) {
                if (inRange(e)) count++;
            }
            return count;
        }

        @Override
        public boolean contains(Object o) {
            @SuppressWarnings("unchecked")
            E e = (E) o;
            return inRange(e) && parent.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            List<E> filtered = new ArrayList<>();
            for (E e : parent) {
                if (inRange(e)) filtered.add(e);
            }
            return Collections.unmodifiableList(filtered).iterator();
        }

        /** UOE 개선: 범위 내 요소만 허용 */
        @Override
        public boolean add(E e) {
            checkInRange(e);
            return parent.add(e);
        }

        /** UOE 개선: 범위 밖 요소는 false 반환 */
        @Override
        public boolean remove(Object o) {
            @SuppressWarnings("unchecked")
            E e = (E) o;
            if (!inRange(e)) {
                return false;
            }
            return parent.remove(o);
        }

        /**
         * BUG-V11-001 수정: SubSetView.retainAll() 오버라이드
         */
        @Override
        public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c, "Collection cannot be null");
            List<E> toRemove = new ArrayList<>();
            for (E element : this) {
                if (!c.contains(element)) {
                    toRemove.add(element);
                }
            }
            boolean modified = false;
            for (E element : toRemove) {
                if (remove(element)) {
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public Comparator<? super E> comparator() {
            return parent.comparator();
        }

        @Override
        public E first() {
            for (E e : parent) {
                if (inRange(e)) return e;
            }
            throw new NoSuchElementException();
        }

        @Override
        public E last() {
            E last = null;
            for (E e : parent) {
                if (inRange(e)) last = e;
            }
            if (last == null) throw new NoSuchElementException();
            return last;
        }

        @Override
        public E lower(E e) {
            E result = null;
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) < 0) {
                    result = elem;
                }
            }
            return result;
        }

        @Override
        public E floor(E e) {
            E result = null;
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) <= 0) {
                    result = elem;
                }
            }
            return result;
        }

        @Override
        public E ceiling(E e) {
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) >= 0) {
                    return elem;
                }
            }
            return null;
        }

        @Override
        public E higher(E e) {
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) > 0) {
                    return elem;
                }
            }
            return null;
        }

        /** UOE 개선: 첫 번째 요소 조회 후 삭제 */
        @Override
        public E pollFirst() {
            E first = null;
            for (E e : parent) {
                if (inRange(e)) {
                    first = e;
                    break;
                }
            }
            if (first != null) {
                parent.remove(first);
            }
            return first;
        }

        /** UOE 개선: 마지막 요소 조회 후 삭제 */
        @Override
        public E pollLast() {
            E last = null;
            for (E e : parent) {
                if (inRange(e)) last = e;
            }
            if (last != null) {
                parent.remove(last);
            }
            return last;
        }

        @Override
        public Iterator<E> descendingIterator() {
            List<E> list = new ArrayList<>();
            for (E e : parent) {
                if (inRange(e)) list.add(e);
            }
            Collections.reverse(list);
            return list.iterator();
        }

        /** UOE 개선: descendingSet */
        @Override
        public NavigableSet<E> descendingSet() {
            // SubSetView를 래핑하는 DescendingSubSetView 반환
            return new DescendingSubSetView<>(this);
        }

        /** UOE 개선: 중첩 subSet */
        @Override
        public NavigableSet<E> subSet(E from, boolean fi, E to, boolean ti) {
            if (tooLow(from) || tooHigh(to)) {
                throw new IllegalArgumentException("subSet range out of bounds");
            }
            return parent.subSet(from, fi, to, ti);
        }

        /** UOE 개선: 중첩 headSet */
        @Override
        public NavigableSet<E> headSet(E to, boolean inclusive) {
            if (tooHigh(to)) {
                throw new IllegalArgumentException("toElement out of range: " + to);
            }
            return parent.subSet(fromElement, fromInclusive, to, inclusive);
        }

        /** UOE 개선: 중첩 tailSet */
        @Override
        public NavigableSet<E> tailSet(E from, boolean inclusive) {
            if (tooLow(from)) {
                throw new IllegalArgumentException("fromElement out of range: " + from);
            }
            return parent.subSet(from, inclusive, toElement, toInclusive);
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
    }

    /**
     * SubSetView를 위한 역순 뷰 헬퍼 클래스
     */
    private static class DescendingSubSetView<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final SubSetView<E> parent;

        DescendingSubSetView(SubSetView<E> parent) {
            this.parent = parent;
        }

        @Override public int size() { return parent.size(); }
        @Override public boolean contains(Object o) { return parent.contains(o); }
        @Override public Iterator<E> iterator() { return parent.descendingIterator(); }
        @Override public Iterator<E> descendingIterator() { return parent.iterator(); }
        @Override public boolean add(E e) { return parent.add(e); }
        @Override public boolean remove(Object o) { return parent.remove(o); }
        @Override public void clear() { for (E e : new ArrayList<>(parent)) { parent.remove(e); } }
        /** BUG-V11-001 수정 */
        @Override public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c, "Collection cannot be null");
            List<E> toRemove = new ArrayList<>();
            for (E element : this) { if (!c.contains(element)) { toRemove.add(element); } }
            boolean modified = false;
            for (E element : toRemove) { if (remove(element)) { modified = true; } }
            return modified;
        }
        @Override public Comparator<? super E> comparator() { return Collections.reverseOrder(parent.comparator()); }
        @Override public E first() { return parent.last(); }
        @Override public E last() { return parent.first(); }
        @Override public E lower(E e) { return parent.higher(e); }
        @Override public E floor(E e) { return parent.ceiling(e); }
        @Override public E ceiling(E e) { return parent.floor(e); }
        @Override public E higher(E e) { return parent.lower(e); }
        @Override public E pollFirst() { return parent.pollLast(); }
        @Override public E pollLast() { return parent.pollFirst(); }
        @Override public NavigableSet<E> descendingSet() { return parent; }
        @Override public NavigableSet<E> subSet(E from, boolean fi, E to, boolean ti) { return parent.subSet(to, ti, from, fi).descendingSet(); }
        @Override public NavigableSet<E> headSet(E to, boolean i) { return parent.tailSet(to, i).descendingSet(); }
        @Override public NavigableSet<E> tailSet(E from, boolean i) { return parent.headSet(from, i).descendingSet(); }
        @Override public SortedSet<E> subSet(E from, E to) { return subSet(from, true, to, false); }
        @Override public SortedSet<E> headSet(E to) { return headSet(to, false); }
        @Override public SortedSet<E> tailSet(E from) { return tailSet(from, true); }
    }

    /**
     * HeadSet 뷰
     *
     * <p>UOE 개선: 수정 연산, poll 연산, 중첩 뷰 생성 지원
     */
    private static class HeadSetView<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final FxNavigableSetImpl<E> parent;
        private final E toElement;
        private final boolean inclusive;

        HeadSetView(FxNavigableSetImpl<E> parent, E toElement, boolean inclusive) {
            this.parent = parent;
            this.toElement = toElement;
            this.inclusive = inclusive;
        }

        // === 범위 검증 유틸리티 (UOE 개선) ===

        @SuppressWarnings("unchecked")
        private int compare(E e1, E e2) {
            Comparator<? super E> cmp = (Comparator<? super E>) parent.comparator();
            if (cmp != null) {
                return cmp.compare(e1, e2);
            }
            return ((Comparable<? super E>) e1).compareTo(e2);
        }

        private boolean tooHigh(E element) {
            int c = compare(element, toElement);
            return c > 0 || (c == 0 && !inclusive);
        }

        private boolean inRange(E element) {
            if (element == null) return false;
            return !tooHigh(element);
        }

        private void checkInRange(E element) {
            if (!inRange(element)) {
                throw new IllegalArgumentException("element out of range: " + element);
            }
        }

        @Override
        public int size() {
            int count = 0;
            for (E e : parent) {
                if (inRange(e)) count++;
            }
            return count;
        }

        @Override
        public boolean contains(Object o) {
            @SuppressWarnings("unchecked")
            E e = (E) o;
            return inRange(e) && parent.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            List<E> filtered = new ArrayList<>();
            for (E e : parent) {
                if (inRange(e)) filtered.add(e);
            }
            return Collections.unmodifiableList(filtered).iterator();
        }

        /** UOE 개선: 범위 내 요소만 허용 */
        @Override
        public boolean add(E e) {
            checkInRange(e);
            return parent.add(e);
        }

        /** UOE 개선: 범위 밖 요소는 false 반환 */
        @Override
        public boolean remove(Object o) {
            @SuppressWarnings("unchecked")
            E e = (E) o;
            if (!inRange(e)) {
                return false;
            }
            return parent.remove(o);
        }

        /**
         * BUG-V11-001 수정: HeadSetView.retainAll() 오버라이드
         */
        @Override
        public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c, "Collection cannot be null");
            List<E> toRemove = new ArrayList<>();
            for (E element : this) {
                if (!c.contains(element)) {
                    toRemove.add(element);
                }
            }
            boolean modified = false;
            for (E element : toRemove) {
                if (remove(element)) {
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public Comparator<? super E> comparator() {
            return parent.comparator();
        }

        @Override
        public E first() {
            for (E e : parent) {
                if (inRange(e)) return e;
            }
            throw new NoSuchElementException();
        }

        @Override
        public E last() {
            E last = null;
            for (E e : parent) {
                if (inRange(e)) last = e;
            }
            if (last == null) throw new NoSuchElementException();
            return last;
        }

        @Override
        public E lower(E e) {
            E result = null;
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) < 0) {
                    result = elem;
                }
            }
            return result;
        }

        @Override
        public E floor(E e) {
            E result = null;
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) <= 0) {
                    result = elem;
                }
            }
            return result;
        }

        @Override
        public E ceiling(E e) {
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) >= 0) {
                    return elem;
                }
            }
            return null;
        }

        @Override
        public E higher(E e) {
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) > 0) {
                    return elem;
                }
            }
            return null;
        }

        /** UOE 개선: 첫 번째 요소 조회 후 삭제 */
        @Override
        public E pollFirst() {
            E first = null;
            for (E e : parent) {
                if (inRange(e)) {
                    first = e;
                    break;
                }
            }
            if (first != null) {
                parent.remove(first);
            }
            return first;
        }

        /** UOE 개선: 마지막 요소 조회 후 삭제 */
        @Override
        public E pollLast() {
            E last = null;
            for (E e : parent) {
                if (inRange(e)) last = e;
            }
            if (last != null) {
                parent.remove(last);
            }
            return last;
        }

        @Override
        public Iterator<E> descendingIterator() {
            List<E> list = new ArrayList<>();
            for (E e : parent) {
                if (inRange(e)) list.add(e);
            }
            Collections.reverse(list);
            return list.iterator();
        }

        /** UOE 개선: descendingSet */
        @Override
        public NavigableSet<E> descendingSet() {
            return new DescendingHeadSetView<>(this);
        }

        /** UOE 개선: 중첩 subSet */
        @Override
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E to, boolean toInclusive) {
            if (tooHigh(to)) {
                throw new IllegalArgumentException("toElement out of range: " + to);
            }
            return parent.subSet(fromElement, fromInclusive, to, toInclusive);
        }

        /** UOE 개선: 중첩 headSet */
        @Override
        public NavigableSet<E> headSet(E to, boolean toInclusive) {
            if (tooHigh(to)) {
                throw new IllegalArgumentException("toElement out of range: " + to);
            }
            return parent.headSet(to, toInclusive);
        }

        /** UOE 개선: 중첩 tailSet */
        @Override
        public NavigableSet<E> tailSet(E from, boolean fromInclusive) {
            return parent.subSet(from, fromInclusive, toElement, inclusive);
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
    }

    /**
     * HeadSetView를 위한 역순 뷰 헬퍼 클래스
     */
    private static class DescendingHeadSetView<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final HeadSetView<E> parent;

        DescendingHeadSetView(HeadSetView<E> parent) {
            this.parent = parent;
        }

        @Override public int size() { return parent.size(); }
        @Override public boolean contains(Object o) { return parent.contains(o); }
        @Override public Iterator<E> iterator() { return parent.descendingIterator(); }
        @Override public Iterator<E> descendingIterator() { return parent.iterator(); }
        @Override public boolean add(E e) { return parent.add(e); }
        @Override public boolean remove(Object o) { return parent.remove(o); }
        @Override public void clear() { for (E e : new ArrayList<>(parent)) { parent.remove(e); } }
        /** BUG-V11-001 수정 */
        @Override public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c, "Collection cannot be null");
            List<E> toRemove = new ArrayList<>();
            for (E element : this) { if (!c.contains(element)) { toRemove.add(element); } }
            boolean modified = false;
            for (E element : toRemove) { if (remove(element)) { modified = true; } }
            return modified;
        }
        @Override public Comparator<? super E> comparator() { return Collections.reverseOrder(parent.comparator()); }
        @Override public E first() { return parent.last(); }
        @Override public E last() { return parent.first(); }
        @Override public E lower(E e) { return parent.higher(e); }
        @Override public E floor(E e) { return parent.ceiling(e); }
        @Override public E ceiling(E e) { return parent.floor(e); }
        @Override public E higher(E e) { return parent.lower(e); }
        @Override public E pollFirst() { return parent.pollLast(); }
        @Override public E pollLast() { return parent.pollFirst(); }
        @Override public NavigableSet<E> descendingSet() { return parent; }
        @Override public NavigableSet<E> subSet(E from, boolean fi, E to, boolean ti) { return parent.subSet(to, ti, from, fi).descendingSet(); }
        @Override public NavigableSet<E> headSet(E to, boolean i) { return parent.tailSet(to, i).descendingSet(); }
        @Override public NavigableSet<E> tailSet(E from, boolean i) { return parent.headSet(from, i).descendingSet(); }
        @Override public SortedSet<E> subSet(E from, E to) { return subSet(from, true, to, false); }
        @Override public SortedSet<E> headSet(E to) { return headSet(to, false); }
        @Override public SortedSet<E> tailSet(E from) { return tailSet(from, true); }
    }

    /**
     * TailSet 뷰
     *
     * <p>UOE 개선: 수정 연산, poll 연산, 중첩 뷰 생성 지원
     */
    private static class TailSetView<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final FxNavigableSetImpl<E> parent;
        private final E fromElement;
        private final boolean inclusive;

        TailSetView(FxNavigableSetImpl<E> parent, E fromElement, boolean inclusive) {
            this.parent = parent;
            this.fromElement = fromElement;
            this.inclusive = inclusive;
        }

        // === 범위 검증 유틸리티 (UOE 개선) ===

        @SuppressWarnings("unchecked")
        private int compare(E e1, E e2) {
            Comparator<? super E> cmp = (Comparator<? super E>) parent.comparator();
            if (cmp != null) {
                return cmp.compare(e1, e2);
            }
            return ((Comparable<? super E>) e1).compareTo(e2);
        }

        private boolean tooLow(E element) {
            int c = compare(element, fromElement);
            return c < 0 || (c == 0 && !inclusive);
        }

        private boolean inRange(E element) {
            if (element == null) return false;
            return !tooLow(element);
        }

        private void checkInRange(E element) {
            if (!inRange(element)) {
                throw new IllegalArgumentException("element out of range: " + element);
            }
        }

        @Override
        public int size() {
            int count = 0;
            for (E e : parent) {
                if (inRange(e)) count++;
            }
            return count;
        }

        @Override
        public boolean contains(Object o) {
            @SuppressWarnings("unchecked")
            E e = (E) o;
            return inRange(e) && parent.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            List<E> filtered = new ArrayList<>();
            for (E e : parent) {
                if (inRange(e)) filtered.add(e);
            }
            return Collections.unmodifiableList(filtered).iterator();
        }

        /** UOE 개선: 범위 내 요소만 허용 */
        @Override
        public boolean add(E e) {
            checkInRange(e);
            return parent.add(e);
        }

        /** UOE 개선: 범위 밖 요소는 false 반환 */
        @Override
        public boolean remove(Object o) {
            @SuppressWarnings("unchecked")
            E e = (E) o;
            if (!inRange(e)) {
                return false;
            }
            return parent.remove(o);
        }

        /**
         * BUG-V11-001 수정: TailSetView.retainAll() 오버라이드
         */
        @Override
        public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c, "Collection cannot be null");
            List<E> toRemove = new ArrayList<>();
            for (E element : this) {
                if (!c.contains(element)) {
                    toRemove.add(element);
                }
            }
            boolean modified = false;
            for (E element : toRemove) {
                if (remove(element)) {
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public Comparator<? super E> comparator() {
            return parent.comparator();
        }

        @Override
        public E first() {
            for (E e : parent) {
                if (inRange(e)) return e;
            }
            throw new NoSuchElementException();
        }

        @Override
        public E last() {
            E last = null;
            for (E e : parent) {
                if (inRange(e)) last = e;
            }
            if (last == null) throw new NoSuchElementException();
            return last;
        }

        @Override
        public E lower(E e) {
            E result = null;
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) < 0) {
                    result = elem;
                }
            }
            return result;
        }

        @Override
        public E floor(E e) {
            E result = null;
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) <= 0) {
                    result = elem;
                }
            }
            return result;
        }

        @Override
        public E ceiling(E e) {
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) >= 0) {
                    return elem;
                }
            }
            return null;
        }

        @Override
        public E higher(E e) {
            for (E elem : parent) {
                if (inRange(elem) && compare(elem, e) > 0) {
                    return elem;
                }
            }
            return null;
        }

        /** UOE 개선: 첫 번째 요소 조회 후 삭제 */
        @Override
        public E pollFirst() {
            E first = null;
            for (E e : parent) {
                if (inRange(e)) {
                    first = e;
                    break;
                }
            }
            if (first != null) {
                parent.remove(first);
            }
            return first;
        }

        /** UOE 개선: 마지막 요소 조회 후 삭제 */
        @Override
        public E pollLast() {
            E last = null;
            for (E e : parent) {
                if (inRange(e)) last = e;
            }
            if (last != null) {
                parent.remove(last);
            }
            return last;
        }

        @Override
        public Iterator<E> descendingIterator() {
            List<E> list = new ArrayList<>();
            for (E e : parent) {
                if (inRange(e)) list.add(e);
            }
            Collections.reverse(list);
            return list.iterator();
        }

        /** UOE 개선: descendingSet */
        @Override
        public NavigableSet<E> descendingSet() {
            return new DescendingTailSetView<>(this);
        }

        /** UOE 개선: 중첩 subSet */
        @Override
        public NavigableSet<E> subSet(E from, boolean fromInclusive, E toElement, boolean toInclusive) {
            if (tooLow(from)) {
                throw new IllegalArgumentException("fromElement out of range: " + from);
            }
            return parent.subSet(from, fromInclusive, toElement, toInclusive);
        }

        /** UOE 개선: 중첩 headSet */
        @Override
        public NavigableSet<E> headSet(E to, boolean toInclusive) {
            return parent.subSet(fromElement, inclusive, to, toInclusive);
        }

        /** UOE 개선: 중첩 tailSet */
        @Override
        public NavigableSet<E> tailSet(E from, boolean fromInclusive) {
            if (tooLow(from)) {
                throw new IllegalArgumentException("fromElement out of range: " + from);
            }
            return parent.tailSet(from, fromInclusive);
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
    }

    /**
     * TailSetView를 위한 역순 뷰 헬퍼 클래스
     */
    private static class DescendingTailSetView<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final TailSetView<E> parent;

        DescendingTailSetView(TailSetView<E> parent) {
            this.parent = parent;
        }

        @Override public int size() { return parent.size(); }
        @Override public boolean contains(Object o) { return parent.contains(o); }
        @Override public Iterator<E> iterator() { return parent.descendingIterator(); }
        @Override public Iterator<E> descendingIterator() { return parent.iterator(); }
        @Override public boolean add(E e) { return parent.add(e); }
        @Override public boolean remove(Object o) { return parent.remove(o); }
        @Override public void clear() { for (E e : new ArrayList<>(parent)) { parent.remove(e); } }
        /** BUG-V11-001 수정 */
        @Override public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c, "Collection cannot be null");
            List<E> toRemove = new ArrayList<>();
            for (E element : this) { if (!c.contains(element)) { toRemove.add(element); } }
            boolean modified = false;
            for (E element : toRemove) { if (remove(element)) { modified = true; } }
            return modified;
        }
        @Override public Comparator<? super E> comparator() { return Collections.reverseOrder(parent.comparator()); }
        @Override public E first() { return parent.last(); }
        @Override public E last() { return parent.first(); }
        @Override public E lower(E e) { return parent.higher(e); }
        @Override public E floor(E e) { return parent.ceiling(e); }
        @Override public E ceiling(E e) { return parent.floor(e); }
        @Override public E higher(E e) { return parent.lower(e); }
        @Override public E pollFirst() { return parent.pollLast(); }
        @Override public E pollLast() { return parent.pollFirst(); }
        @Override public NavigableSet<E> descendingSet() { return parent; }
        @Override public NavigableSet<E> subSet(E from, boolean fi, E to, boolean ti) { return parent.subSet(to, ti, from, fi).descendingSet(); }
        @Override public NavigableSet<E> headSet(E to, boolean i) { return parent.tailSet(to, i).descendingSet(); }
        @Override public NavigableSet<E> tailSet(E from, boolean i) { return parent.headSet(from, i).descendingSet(); }
        @Override public SortedSet<E> subSet(E from, E to) { return subSet(from, true, to, false); }
        @Override public SortedSet<E> headSet(E to) { return headSet(to, false); }
        @Override public SortedSet<E> tailSet(E from) { return tailSet(from, true); }
    }
}
