package com.snoworca.fxstore.api;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;

/**
 * 읽기 전용 트랜잭션
 *
 * <p>트랜잭션 시작 시점의 스냅샷을 고정하여 일관된 읽기 뷰를 제공합니다.
 * 트랜잭션이 열려 있는 동안 다른 스레드의 쓰기가 발생해도
 * 이 트랜잭션 내에서는 시작 시점의 데이터만 보입니다.</p>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * try (FxReadTransaction tx = store.beginRead()) {
 *     User user = tx.get(userMap, userId);
 *     Account account = tx.get(accountMap, user.getAccountId());
 *     // 두 읽기는 동일한 스냅샷에서 수행됨 (일관성 보장)
 * }
 * }</pre>
 *
 * <h3>스레드 안전성</h3>
 * <p>단일 스레드에서만 사용해야 합니다.
 * 여러 스레드에서 동시에 사용하려면 각 스레드마다 별도의 트랜잭션을 시작하세요.</p>
 *
 * <h3>불변식 (Invariants)</h3>
 * <ul>
 *   <li><b>INV-RT1</b>: 트랜잭션 내 스냅샷은 절대 변경 불가</li>
 *   <li><b>INV-RT2</b>: 동일 트랜잭션 내 모든 읽기는 동일 스냅샷 사용</li>
 *   <li><b>INV-RT3</b>: 다른 스레드의 쓰기가 활성 트랜잭션에 영향 없음</li>
 *   <li><b>INV-RT4</b>: close() 후 모든 연산은 예외 발생</li>
 *   <li><b>INV-RT5</b>: 트랜잭션은 생성된 store의 컬렉션만 접근 가능</li>
 * </ul>
 *
 * @since 0.6
 * @see FxStore#beginRead()
 */
public interface FxReadTransaction extends AutoCloseable {

    // ==================== Map 연산 ====================

    /**
     * Map에서 키로 값 조회
     *
     * @param map 대상 Map (이 store에서 생성된 것이어야 함)
     * @param key 조회할 키
     * @param <K> 키 타입
     * @param <V> 값 타입
     * @return 키에 해당하는 값, 없으면 null
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     * @throws IllegalArgumentException 다른 store의 컬렉션인 경우
     */
    <K, V> V get(NavigableMap<K, V> map, K key);

    /**
     * Map에 키 존재 여부 확인
     *
     * @param map 대상 Map
     * @param key 확인할 키
     * @param <K> 키 타입
     * @param <V> 값 타입
     * @return 키가 존재하면 true
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <K, V> boolean containsKey(NavigableMap<K, V> map, K key);

    /**
     * Map의 첫 번째 엔트리 조회
     *
     * @param map 대상 Map
     * @param <K> 키 타입
     * @param <V> 값 타입
     * @return 첫 번째 엔트리, 비어있으면 null
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <K, V> Map.Entry<K, V> firstEntry(NavigableMap<K, V> map);

    /**
     * Map의 마지막 엔트리 조회
     *
     * @param map 대상 Map
     * @param <K> 키 타입
     * @param <V> 값 타입
     * @return 마지막 엔트리, 비어있으면 null
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <K, V> Map.Entry<K, V> lastEntry(NavigableMap<K, V> map);

    /**
     * Map의 크기 조회
     *
     * @param map 대상 Map
     * @param <K> 키 타입
     * @param <V> 값 타입
     * @return Map의 엔트리 수
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <K, V> int size(NavigableMap<K, V> map);

    // ==================== Set 연산 ====================

    /**
     * Set에 요소 존재 여부 확인
     *
     * @param set 대상 Set
     * @param element 확인할 요소
     * @param <E> 요소 타입
     * @return 요소가 존재하면 true
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <E> boolean contains(NavigableSet<E> set, E element);

    /**
     * Set의 첫 번째 요소 조회
     *
     * @param set 대상 Set
     * @param <E> 요소 타입
     * @return 첫 번째 요소, 비어있으면 null
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <E> E first(NavigableSet<E> set);

    /**
     * Set의 마지막 요소 조회
     *
     * @param set 대상 Set
     * @param <E> 요소 타입
     * @return 마지막 요소, 비어있으면 null
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <E> E last(NavigableSet<E> set);

    /**
     * Set의 크기 조회
     *
     * @param set 대상 Set
     * @param <E> 요소 타입
     * @return Set의 요소 수
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <E> int size(NavigableSet<E> set);

    // ==================== List 연산 ====================

    /**
     * List에서 인덱스로 요소 조회
     *
     * @param list 대상 List
     * @param index 조회할 인덱스
     * @param <E> 요소 타입
     * @return 해당 인덱스의 요소
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     * @throws IndexOutOfBoundsException 인덱스가 범위를 벗어난 경우
     */
    <E> E get(List<E> list, int index);

    /**
     * List의 크기 조회
     *
     * @param list 대상 List
     * @param <E> 요소 타입
     * @return List의 요소 수
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <E> int size(List<E> list);

    /**
     * List에서 요소의 인덱스 조회
     *
     * @param list 대상 List
     * @param element 찾을 요소
     * @param <E> 요소 타입
     * @return 요소의 인덱스, 없으면 -1
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <E> int indexOf(List<E> list, E element);

    // ==================== Deque 연산 ====================

    /**
     * Deque의 첫 번째 요소 조회 (제거하지 않음)
     *
     * @param deque 대상 Deque
     * @param <E> 요소 타입
     * @return 첫 번째 요소, 비어있으면 null
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <E> E peekFirst(Deque<E> deque);

    /**
     * Deque의 마지막 요소 조회 (제거하지 않음)
     *
     * @param deque 대상 Deque
     * @param <E> 요소 타입
     * @return 마지막 요소, 비어있으면 null
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <E> E peekLast(Deque<E> deque);

    /**
     * Deque의 크기 조회
     *
     * @param deque 대상 Deque
     * @param <E> 요소 타입
     * @return Deque의 요소 수
     * @throws IllegalStateException 트랜잭션이 이미 닫힌 경우
     */
    <E> int size(Deque<E> deque);

    // ==================== 트랜잭션 관리 ====================

    /**
     * 트랜잭션이 활성 상태인지 확인
     *
     * @return 활성 상태면 true, 닫힌 상태면 false
     */
    boolean isActive();

    /**
     * 트랜잭션에서 사용 중인 스냅샷의 시퀀스 번호
     *
     * <p>디버깅 및 모니터링 목적으로 사용됩니다.</p>
     *
     * @return 스냅샷 시퀀스 번호
     */
    long getSnapshotSeqNo();

    /**
     * 트랜잭션 종료 (스냅샷 참조 해제)
     *
     * <p>try-with-resources 사용을 권장합니다:</p>
     * <pre>{@code
     * try (FxReadTransaction tx = store.beginRead()) {
     *     // 읽기 연산...
     * } // 자동으로 close() 호출
     * }</pre>
     *
     * <p>이미 닫힌 트랜잭션에 대해 close()를 호출해도 안전합니다 (no-op).</p>
     */
    @Override
    void close();
}
