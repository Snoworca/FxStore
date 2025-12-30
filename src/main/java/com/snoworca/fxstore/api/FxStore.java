package com.snoworca.fxstore.api;

import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.api.FxCodecRegistry;
import com.snoworca.fxstore.core.FxStoreImpl;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;

/**
 * FxStore - 단일 파일/메모리 기반 영속 컬렉션 엔진
 *
 * <p>Java 8 기반 Key-Value 저장소로 NavigableMap, NavigableSet, List, Deque를
 * 제공합니다. COW (Copy-on-Write) 기반으로 크래시 일관성을 보장합니다.</p>
 *
 * <p>기본 사용 예시:</p>
 * <pre>{@code
 * FxStore store = FxStore.open(Paths.get("data.fx"));
 * NavigableMap<Long, String> users = store.createOrOpenMap("users", Long.class, String.class);
 * users.put(1L, "Alice");
 * store.close();
 * }</pre>
 *
 * <h3>Thread Safety (스레드 안전성)</h3>
 * <p>이 인터페이스의 구현체는 <b>스레드 안전하지 않습니다</b>.
 * 다중 스레드 환경에서 사용할 경우 외부에서 동기화를 제공해야 합니다.</p>
 *
 * <p>권장 사용 패턴:</p>
 * <ul>
 *   <li>단일 스레드에서 FxStore 인스턴스 사용</li>
 *   <li>또는 synchronized 블록/ReentrantLock으로 모든 접근 보호</li>
 *   <li>스레드별 별도 Store 인스턴스 사용 (FileLockMode.NONE 필요)</li>
 * </ul>
 *
 * <p>반환되는 컬렉션 (NavigableMap, NavigableSet, List, Deque)도 마찬가지로
 * 스레드 안전하지 않습니다.</p>
 */
public interface FxStore extends AutoCloseable {
    
    // ==================== 열기 ====================
    
    /**
     * 파일 기반 Store 열기 (기본 옵션)
     * 
     * @param file 저장소 파일 경로
     * @return FxStore 인스턴스
     * @throws FxException I/O 오류 또는 파일 손상 시
     */
    static FxStore open(Path file) {
        return open(file, FxOptions.defaults());
    }
    
    /**
     * 파일 기반 Store 열기
     * 
     * @param file 저장소 파일 경로
     * @param options 열기 옵션
     * @return FxStore 인스턴스
     * @throws FxException I/O 오류 또는 파일 손상 시
     */
    static FxStore open(Path file, FxOptions options) {
        return FxStoreImpl.open(file, options);
    }
    
    /**
     * 메모리 기반 Store 열기 (기본 옵션)
     * 
     * @return 메모리 기반 FxStore 인스턴스
     */
    static FxStore openMemory() {
        return openMemory(FxOptions.defaults());
    }
    
    /**
     * 메모리 기반 Store 열기
     * 
     * @param options 열기 옵션
     * @return 메모리 기반 FxStore 인스턴스
     */
    static FxStore openMemory(FxOptions options) {
        return FxStoreImpl.openMemory(options);
    }
    
    // ==================== 코덱 ====================
    
    /**
     * Store-local 코덱 등록
     * 
     * @param type 타입 클래스
     * @param codec 코덱 구현
     * @throws FxException 이미 등록된 타입이면 ILLEGAL_ARGUMENT
     */
    <T> void registerCodec(Class<T> type, FxCodec<T> codec);
    
    /**
     * Store의 코덱 레지스트리 반환
     * 
     * @return 코덱 레지스트리
     */
    FxCodecRegistry codecs();
    
    // ==================== DDL ====================
    
    /**
     * 컬렉션 존재 여부 확인
     * 
     * @param name 컬렉션 이름
     * @return 존재하면 true
     */
    boolean exists(String name);
    
    /**
     * 컬렉션 삭제 (공간 회수는 compactTo로)
     * 
     * @param name 컬렉션 이름
     * @return 삭제 성공 시 true, 컬렉션이 없으면 false
     * @throws FxException 컬렉션이 없으면 NOT_FOUND
     */
    boolean drop(String name);
    
    /**
     * 컬렉션 이름 변경
     * 
     * @param from 기존 이름
     * @param to 새 이름
     * @return 성공 시 true
     * @throws FxException from이 없으면 NOT_FOUND, to가 이미 존재하면 ALREADY_EXISTS
     */
    boolean rename(String from, String to);
    
    /**
     * 모든 컬렉션 정보 조회
     * 
     * @return 컬렉션 정보 리스트
     */
    List<CollectionInfo> list();
    
    // ==================== Map ====================
    
    /**
     * Map 생성 (이미 존재하면 ALREADY_EXISTS)
     * 
     * @param name 컬렉션 이름
     * @param keyClass 키 타입 클래스
     * @param valueClass 값 타입 클래스
     * @return NavigableMap 인스턴스
     * @throws FxException 이미 존재하면 ALREADY_EXISTS
     */
    <K, V> NavigableMap<K, V> createMap(String name, Class<K> keyClass, Class<V> valueClass);
    
    /**
     * Map 열기 (미존재하면 NOT_FOUND)
     * 
     * @param name 컬렉션 이름
     * @param keyClass 키 타입 클래스
     * @param valueClass 값 타입 클래스
     * @return NavigableMap 인스턴스
     * @throws FxException 미존재하면 NOT_FOUND
     */
    <K, V> NavigableMap<K, V> openMap(String name, Class<K> keyClass, Class<V> valueClass);
    
    /**
     * Map 생성 또는 열기
     * 
     * @param name 컬렉션 이름
     * @param keyClass 키 타입 클래스
     * @param valueClass 값 타입 클래스
     * @return NavigableMap 인스턴스
     */
    <K, V> NavigableMap<K, V> createOrOpenMap(String name, Class<K> keyClass, Class<V> valueClass);
    
    // ==================== Set ====================
    
    /**
     * Set 생성
     * 
     * @param name 컬렉션 이름
     * @param elementClass 원소 타입 클래스
     * @return NavigableSet 인스턴스
     * @throws FxException 이미 존재하면 ALREADY_EXISTS
     */
    <E> NavigableSet<E> createSet(String name, Class<E> elementClass);
    
    /**
     * Set 열기
     * 
     * @param name 컬렉션 이름
     * @param elementClass 원소 타입 클래스
     * @return NavigableSet 인스턴스
     * @throws FxException 미존재하면 NOT_FOUND
     */
    <E> NavigableSet<E> openSet(String name, Class<E> elementClass);
    
    /**
     * Set 생성 또는 열기
     * 
     * @param name 컬렉션 이름
     * @param elementClass 원소 타입 클래스
     * @return NavigableSet 인스턴스
     */
    <E> NavigableSet<E> createOrOpenSet(String name, Class<E> elementClass);
    
    // ==================== List ====================
    
    /**
     * List 생성
     * 
     * @param name 컬렉션 이름
     * @param elementClass 원소 타입 클래스
     * @return List 인스턴스
     * @throws FxException 이미 존재하면 ALREADY_EXISTS
     */
    <E> List<E> createList(String name, Class<E> elementClass);
    
    /**
     * List 열기
     * 
     * @param name 컬렉션 이름
     * @param elementClass 원소 타입 클래스
     * @return List 인스턴스
     * @throws FxException 미존재하면 NOT_FOUND
     */
    <E> List<E> openList(String name, Class<E> elementClass);
    
    /**
     * List 생성 또는 열기
     * 
     * @param name 컬렉션 이름
     * @param elementClass 원소 타입 클래스
     * @return List 인스턴스
     */
    <E> List<E> createOrOpenList(String name, Class<E> elementClass);
    
    // ==================== Deque ====================
    
    /**
     * Deque 생성
     * 
     * @param name 컬렉션 이름
     * @param elementClass 원소 타입 클래스
     * @return Deque 인스턴스
     * @throws FxException 이미 존재하면 ALREADY_EXISTS
     */
    <E> Deque<E> createDeque(String name, Class<E> elementClass);
    
    /**
     * Deque 열기
     * 
     * @param name 컬렉션 이름
     * @param elementClass 원소 타입 클래스
     * @return Deque 인스턴스
     * @throws FxException 미존재하면 NOT_FOUND
     */
    <E> Deque<E> openDeque(String name, Class<E> elementClass);
    
    /**
     * Deque 생성 또는 열기
     * 
     * @param name 컬렉션 이름
     * @param elementClass 원소 타입 클래스
     * @return Deque 인스턴스
     */
    <E> Deque<E> createOrOpenDeque(String name, Class<E> elementClass);
    
    // ==================== 커밋 제어 ====================
    
    /**
     * 현재 커밋 모드 반환
     * 
     * @return 커밋 모드
     */
    CommitMode commitMode();
    
    /**
     * BATCH 모드에서 변경 커밋
     * AUTO 모드에서는 no-op
     * 
     * @throws FxException 커밋 실패 시 IO
     */
    void commit();
    
    /**
     * BATCH 모드에서 변경 롤백
     * AUTO 모드에서는 no-op
     */
    void rollback();
    
    // ==================== 읽기 트랜잭션 ====================

    /**
     * 읽기 전용 트랜잭션 시작
     *
     * <p>트랜잭션 시작 시점의 스냅샷을 고정하여 일관된 읽기 뷰를 제공합니다.
     * 트랜잭션이 열려 있는 동안 다른 스레드의 쓰기가 발생해도
     * 이 트랜잭션 내에서는 시작 시점의 데이터만 보입니다.</p>
     *
     * <p>사용 예시:</p>
     * <pre>{@code
     * try (FxReadTransaction tx = store.beginRead()) {
     *     User user = tx.get(userMap, userId);
     *     Account account = tx.get(accountMap, user.getAccountId());
     *     // 두 읽기는 동일한 스냅샷에서 수행됨 (일관성 보장)
     * }
     * }</pre>
     *
     * <p><b>주의</b>: 반환된 트랜잭션은 반드시 닫아야 합니다.
     * try-with-resources 사용을 강력히 권장합니다.</p>
     *
     * @return 읽기 전용 트랜잭션
     * @throws IllegalStateException Store가 닫힌 경우
     * @since 0.6
     * @see FxReadTransaction
     */
    FxReadTransaction beginRead();

    // ==================== 진단 ====================

    /**
     * 통계 조회 (FAST 모드)
     * 
     * @return 통계 정보
     */
    Stats stats();
    
    /**
     * 통계 조회
     * 
     * @param mode 통계 모드
     * @return 통계 정보
     */
    Stats stats(StatsMode mode);
    
    /**
     * 무결성 검증
     * 
     * @return 검증 결과
     */
    VerifyResult verify();
    
    // ==================== 유지보수 ====================
    
    /**
     * 새 파일로 컴팩션 (live 데이터만 재작성)
     * 
     * @param newFile 새 파일 경로
     * @throws FxException BATCH 모드에서 pending 변경 있으면 ILLEGAL_ARGUMENT
     */
    void compactTo(Path newFile);
    
    // ==================== 닫기 ====================
    
    /**
     * Store 닫기
     * 
     * @throws FxException OnClosePolicy.ERROR일 때 pending 변경 있으면 ILLEGAL_ARGUMENT
     */
    @Override
    void close();
}
