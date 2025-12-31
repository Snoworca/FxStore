package com.snoworca.fxstore.core;

import com.snoworca.fxstore.api.*;
import com.snoworca.fxstore.btree.BTree;
import com.snoworca.fxstore.api.FxCodecRegistry;
import com.snoworca.fxstore.api.CodecRef;
import com.snoworca.fxstore.storage.Storage;
import com.snoworca.fxstore.storage.MemoryStorage;
import com.snoworca.fxstore.storage.FileStorage;
import com.snoworca.fxstore.storage.Allocator;
import com.snoworca.fxstore.catalog.CatalogEntry;
import com.snoworca.fxstore.catalog.CollectionState;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

/**
 * FxStore의 핵심 구현체
 *
 * <p>메모리 또는 파일 기반 Store를 관리하며, AUTO/BATCH 커밋 모드를 지원합니다.</p>
 *
 * <p>책임:</p>
 * <ul>
 *   <li>Storage 레이어 관리 (메모리/파일)</li>
 *   <li>Catalog 관리 (컬렉션 메타데이터)</li>
 *   <li>커밋 모드 처리 (AUTO/BATCH)</li>
 *   <li>컬렉션 생성/열기/삭제</li>
 * </ul>
 *
 * <h3>스레드 안전성 (Thread Safety) - v0.4+</h3>
 * <p>이 클래스는 LMDB 스타일 + StampedLock 하이브리드 모델로 스레드 안전합니다:</p>
 * <ul>
 *   <li><b>Single Writer</b>: 쓰기는 StampedLock.writeLock()으로 직렬화</li>
 *   <li><b>Wait-free Reads</b>: 읽기는 락 없이 volatile 스냅샷 참조만 사용</li>
 *   <li><b>Immutable Snapshots</b>: 모든 메타데이터를 불변 스냅샷으로 패키징</li>
 * </ul>
 *
 * <h3>동시성 불변식 (Concurrency Invariants)</h3>
 * <ul>
 *   <li><b>INV-C1</b>: 동시에 하나의 쓰기 스레드만 활성화</li>
 *   <li><b>INV-C2</b>: StoreSnapshot 생성 후 절대 변경 불가</li>
 *   <li><b>INV-C3</b>: 읽기는 어떤 락도 획득하지 않음</li>
 *   <li><b>INV-C4</b>: 스냅샷 교체는 단일 volatile write로 원자적</li>
 *   <li><b>INV-C5</b>: 단일 락만 사용하여 교착 상태 불가능</li>
 * </ul>
 *
 * @since 0.3 (v0.4에서 스레드 안전성 추가)
 */
public class FxStoreImpl implements FxStore {

    // ==================== 핵심 인프라 (불변) ====================
    private final Storage storage;
    private final PageCache pageCache;
    private final Allocator allocator;
    private final FxOptions options;
    private final FxCodecRegistry codecRegistry;

    // ==================== 동시성 인프라 (v0.4+) ====================
    /**
     * 쓰기 동기화를 위한 StampedLock
     *
     * <p>INV-C1: 단일 Writer 보장</p>
     */
    private final StampedLock lock = new StampedLock();

    /**
     * 현재 스냅샷 (volatile로 원자적 교체 보장)
     *
     * <p>INV-C4: 스냅샷 교체는 단일 volatile write로 원자적</p>
     */
    private volatile StoreSnapshot currentSnapshot;

    // ==================== 캐시 필드 ====================
    /**
     * DESIGN DECISION: 레거시 필드 유지 (v0.4)
     *
     * <p>이 필드들({@code catalog}, {@code collectionStates}, {@code openCollections})은
     * StoreSnapshot과 병행 운영됩니다. ConcurrentHashMap + computeIfAbsent 패턴으로
     * 동시성 안전성을 보장합니다.</p>
     *
     * <h3>역할 구분</h3>
     * <ul>
     *   <li>레거시 필드: 쓰기 연산의 working copy</li>
     *   <li>StoreSnapshot: 읽기 연산의 일관된 스냅샷</li>
     * </ul>
     *
     * <h3>설계 근거</h3>
     * <ol>
     *   <li>ConcurrentHashMap 적용으로 동시성 안전 확보</li>
     *   <li>computeIfAbsent 패턴으로 원자적 캐시 연산</li>
     *   <li>StoreSnapshot은 읽기 전용 스냅샷 제공</li>
     *   <li>레거시 필드는 쓰기 연산의 working copy</li>
     * </ol>
     *
     * <h3>스레드 안전성</h3>
     * <p>INV-C1 (Single Writer) + ConcurrentHashMap으로 보장</p>
     *
     * @see StoreSnapshot
     */
    private final Map<String, CatalogEntry> catalog;

    /** 컬렉션 상태 캐시 (collectionId → CollectionState). @see #catalog */
    private final Map<Long, CollectionState> collectionStates;

    /** 열린 컬렉션 인스턴스 캐시 (name → collection). @see #catalog */
    private final Map<String, Object> openCollections;

    private volatile boolean closed = false;

    /**
     * IMP-001: 다음 컬렉션 ID (volatile 추가)
     *
     * <p>Write Lock 내에서만 수정되므로 현재도 안전하지만,
     * 의도 명확성과 코드 일관성을 위해 volatile 추가.</p>
     */
    private volatile long nextCollectionId = 1L;

    // BATCH 모드 전용
    private volatile boolean hasPendingChanges = false;

    /**
     * IMP-002: 작업 중 allocTail (Stateless API 지원, volatile 추가)
     *
     * <p>쓰기 연산 시작 시 snapshot().getAllocTail()에서 초기화되고,
     * 각 할당 후 업데이트됩니다. 연산 완료 후 스냅샷 생성에 사용됩니다.</p>
     *
     * <p><b>스레드 안전성:</b> 쓰기 락 내에서만 접근되므로 현재도 안전하지만,
     * 의도 명확성과 가시성 보장을 위해 volatile 추가.</p>
     *
     * @since 0.9
     */
    private volatile long workingAllocTail;
    
    /**
     * 메모리 기반 Store 생성자
     */
    private FxStoreImpl(FxOptions options) {
        this.options = Objects.requireNonNull(options, "options");
        // 메모리 저장소는 옵션의 memoryLimitBytes 사용
        this.storage = new MemoryStorage(options.memoryLimitBytes());
        this.pageCache = new PageCache(options.cacheBytes(), options.pageSize().bytes());
        
        long initialAllocTail = Superblock.SIZE + CommitHeader.SIZE * 2; // 12288
        this.allocator = new Allocator(options.pageSize().bytes(), initialAllocTail);
        
        this.codecRegistry = FxCodecs.global();
        this.catalog = new ConcurrentHashMap<>();
        this.collectionStates = new ConcurrentHashMap<>();
        this.openCollections = new ConcurrentHashMap<>();

        // 초기 Superblock/CommitHeader 작성
        initializeNewStore();

        // 초기 스냅샷 생성 (동시성 지원)
        this.currentSnapshot = createInitialSnapshot();
    }
    
    /**
     * 파일 기반 Store 생성자
     */
    private FxStoreImpl(Path file, FxOptions options) {
        this.options = Objects.requireNonNull(options, "options");
        this.storage = new FileStorage(file, false, options.fileLock()); // readOnly = false, with lock mode
        this.pageCache = new PageCache(options.cacheBytes(), options.pageSize().bytes());
        this.codecRegistry = FxCodecs.global();
        this.catalog = new ConcurrentHashMap<>();
        this.collectionStates = new ConcurrentHashMap<>();
        this.openCollections = new ConcurrentHashMap<>();

        if (storage.size() == 0) {
            // 새 파일
            long initialAllocTail = Superblock.SIZE + CommitHeader.SIZE * 2;
            this.allocator = new Allocator(options.pageSize().bytes(), initialAllocTail);
            initializeNewStore();
        } else {
            // 기존 파일 로드
            loadExistingStore();
            CommitHeader header = getCurrentCommitHeader();
            this.allocator = new Allocator(options.pageSize().bytes(), header.getAllocTail());
        }

        // 초기 스냅샷 생성 (동시성 지원)
        this.currentSnapshot = createInitialSnapshot();
    }
    
    /**
     * 메모리 Store 팩토리 메서드
     */
    public static FxStore openMemory(FxOptions options) {
        return new FxStoreImpl(options);
    }
    
    /**
     * 파일 Store 팩토리 메서드
     */
    public static FxStore open(Path file, FxOptions options) {
        return new FxStoreImpl(file, options);
    }
    
    // ==================== 초기화 ====================
    
    private void initializeNewStore() {
        // Superblock 작성
        Superblock sb = Superblock.create(options.pageSize().bytes());
        byte[] sbBytes = sb.encode();
        storage.write(0L, sbBytes, 0, sbBytes.length);

        // CommitHeader 초기화 (Slot A)
        long initialTail = Superblock.SIZE + CommitHeader.SIZE * 2;
        CommitHeader ch = new CommitHeader(
            0L,  // seqNo
            0L,  // committedFlags
            initialTail,  // allocTail
            0L,  // catalogRootPageId
            0L,  // stateRootPageId
            1L,  // nextCollectionId
            System.currentTimeMillis()  // commitEpochMs
        );
        byte[] chBytes = ch.encode();
        storage.write(Superblock.SIZE, chBytes, 0, chBytes.length);

        // BUG-003 수정: Slot B 영역도 초기화하여 파일 크기가 allocTail에 도달하도록 함
        // Slot B는 미초기화 상태(zeros)로 두되, 파일이 allocTail 크기까지 확장되도록 함
        byte[] slotBZeros = new byte[CommitHeader.SIZE];
        storage.write(Superblock.SIZE + CommitHeader.SIZE, slotBZeros, 0, slotBZeros.length);

        if (options.durability() == Durability.SYNC) {
            storage.force(true);
        }
    }
    
    private void loadExistingStore() {
        // Superblock 검증
        byte[] sbData = new byte[Superblock.SIZE];
        storage.read(0L, sbData, 0, Superblock.SIZE);
        Superblock sb = Superblock.decode(sbData);
        
        // CommitHeader 로드
        CommitHeader ch = getCurrentCommitHeader();
        this.nextCollectionId = ch.getNextCollectionId();
        
        // Catalog 로드
        if (ch.getCatalogRootPageId() != 0L) {
            loadCatalog(ch.getCatalogRootPageId());
        }
        
        // State 로드
        if (ch.getStateRootPageId() != 0L) {
            loadState(ch.getStateRootPageId());
        }
    }
    
    private CommitHeader getCurrentCommitHeader() {
        // Slot A와 B 중 seqNo가 큰 유효한 것 선택
        byte[] slotA = new byte[CommitHeader.SIZE];
        byte[] slotB = new byte[CommitHeader.SIZE];
        
        storage.read(Superblock.SIZE, slotA, 0, CommitHeader.SIZE);
        storage.read(Superblock.SIZE + CommitHeader.SIZE, slotB, 0, CommitHeader.SIZE);
        
        try {
            CommitHeader headerA = CommitHeader.decode(slotA);
            try {
                CommitHeader headerB = CommitHeader.decode(slotB);
                return headerA.getSeqNo() >= headerB.getSeqNo() ? headerA : headerB;
            } catch (Exception e) {
                return headerA;
            }
        } catch (Exception e) {
            return CommitHeader.decode(slotB);
        }
    }
    
    private void loadCatalog(long catalogRootPageId) {
        // Catalog BTree 생성 (String 키 비교자 사용)
        Comparator<byte[]> stringComparator = createLexicographicComparator();
        BTree catalogTree = new BTree(storage, options.pageSize().bytes(),
                                       stringComparator, catalogRootPageId);

        // BTreeCursor로 모든 엔트리 순회
        com.snoworca.fxstore.btree.BTreeCursor cursor = catalogTree.cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            // Value는 별도 레코드에 저장됨
            byte[] valueBytes = readValueRecord(entry.getValueRecordId());
            CatalogEntry catalogEntry = CatalogEntry.decode(valueBytes);
            catalog.put(catalogEntry.getName(), catalogEntry);
        }
    }

    private void loadState(long stateRootPageId) {
        // State BTree 생성 (Long 키 비교자 사용 - 8바이트 LE)
        Comparator<byte[]> longComparator = createLexicographicComparator();
        BTree stateTree = new BTree(storage, options.pageSize().bytes(),
                                    longComparator, stateRootPageId);

        // BTreeCursor로 모든 엔트리 순회
        com.snoworca.fxstore.btree.BTreeCursor cursor = stateTree.cursor();
        while (cursor.hasNext()) {
            BTree.Entry entry = cursor.next();
            // Value는 별도 레코드에 저장됨
            byte[] valueBytes = readValueRecord(entry.getValueRecordId());
            CollectionState state = CollectionState.decode(valueBytes);
            collectionStates.put(state.getCollectionId(), state);
        }
    }

    /**
     * Catalog를 BTree에 저장하고 루트 페이지 ID 반환
     */
    private long saveCatalog() {
        if (catalog.isEmpty()) {
            return 0L;
        }

        Comparator<byte[]> stringComparator = createLexicographicComparator();
        BTree catalogTree = new BTree(storage, options.pageSize().bytes(),
                                       stringComparator, 0L, allocator);

        for (CatalogEntry entry : catalog.values()) {
            byte[] keyBytes = entry.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] valueBytes = entry.encode();
            long valueRecordId = writeValueRecord(valueBytes);
            catalogTree.insert(keyBytes, valueRecordId);
        }

        return catalogTree.getRootPageId();
    }

    /**
     * CollectionState를 BTree에 저장하고 루트 페이지 ID 반환
     */
    private long saveState() {
        if (collectionStates.isEmpty()) {
            return 0L;
        }

        Comparator<byte[]> longComparator = createLexicographicComparator();
        BTree stateTree = new BTree(storage, options.pageSize().bytes(),
                                    longComparator, 0L, allocator);

        for (CollectionState state : collectionStates.values()) {
            // collectionId를 8바이트 LE로 인코딩
            byte[] keyBytes = new byte[8];
            long id = state.getCollectionId();
            keyBytes[0] = (byte) (id & 0xFF);
            keyBytes[1] = (byte) ((id >> 8) & 0xFF);
            keyBytes[2] = (byte) ((id >> 16) & 0xFF);
            keyBytes[3] = (byte) ((id >> 24) & 0xFF);
            keyBytes[4] = (byte) ((id >> 32) & 0xFF);
            keyBytes[5] = (byte) ((id >> 40) & 0xFF);
            keyBytes[6] = (byte) ((id >> 48) & 0xFF);
            keyBytes[7] = (byte) ((id >> 56) & 0xFF);

            byte[] valueBytes = state.encode();
            long valueRecordId = writeValueRecord(valueBytes);
            stateTree.insert(keyBytes, valueRecordId);
        }

        return stateTree.getRootPageId();
    }

    /**
     * Lexicographic 바이트 비교자 생성
     */
    private Comparator<byte[]> createLexicographicComparator() {
        return new Comparator<byte[]>() {
            @Override
            public int compare(byte[] a, byte[] b) {
                int minLen = Math.min(a.length, b.length);
                for (int i = 0; i < minLen; i++) {
                    int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                return a.length - b.length;
            }
        };
    }
    
    // ==================== 코덱 관리 ====================
    
    @Override
    public <T> void registerCodec(Class<T> type, com.snoworca.fxstore.api.FxCodec<T> codec) {
        checkNotClosed();
        codecRegistry.register(type, codec);
    }
    
    @Override
    public FxCodecRegistry codecs() {
        checkNotClosed();
        return codecRegistry;
    }
    
    // ==================== DDL ====================
    
    /**
     * 컬렉션 존재 여부 확인 (Wait-free)
     *
     * <p>INV-C3: 어떤 락도 획득하지 않음</p>
     *
     * @param name 컬렉션 이름
     * @return 존재하면 true
     */
    @Override
    public boolean exists(String name) {
        checkNotClosed();
        // Wait-free: volatile read only (INV-C3)
        return snapshot().getCatalog().containsKey(name);
    }
    
    /**
     * 컬렉션 삭제 (Write Lock 필수)
     *
     * <p>INV-C1: 단일 Writer 보장</p>
     *
     * @param name 컬렉션 이름
     * @return 삭제 성공 여부
     */
    @Override
    public boolean drop(String name) {
        checkNotClosed();

        long stamp = acquireWriteLock();
        try {
            CatalogEntry entry = catalog.remove(name);
            if (entry == null) {
                return false;
            }

            collectionStates.remove(entry.getCollectionId());
            openCollections.remove(name);
            markPendingChanges();
            return true;
        } finally {
            releaseWriteLock(stamp);
        }
    }
    
    /**
     * 컬렉션 이름 변경 (Write Lock 필수)
     *
     * <p>INV-C1: 단일 Writer 보장</p>
     *
     * @param from 기존 이름
     * @param to 새 이름
     * @return 변경 성공 여부
     */
    @Override
    public boolean rename(String from, String to) {
        checkNotClosed();

        long stamp = acquireWriteLock();
        try {
            if (!catalog.containsKey(from)) {
                throw new FxException(FxErrorCode.NOT_FOUND, "Collection not found: " + from);
            }
            if (catalog.containsKey(to)) {
                throw new FxException(FxErrorCode.ALREADY_EXISTS, "Collection already exists: " + to);
            }

            CatalogEntry entry = catalog.remove(from);
            catalog.put(to, new CatalogEntry(to, entry.getCollectionId()));

            Object collection = openCollections.remove(from);
            if (collection != null) {
                openCollections.put(to, collection);
            }

            markPendingChanges();
            return true;
        } finally {
            releaseWriteLock(stamp);
        }
    }
    
    /**
     * 컬렉션 목록 조회 (Wait-free)
     *
     * <p>INV-C3: 어떤 락도 획득하지 않음</p>
     *
     * @return 모든 컬렉션 정보 목록
     */
    @Override
    public List<CollectionInfo> list() {
        checkNotClosed();
        // Wait-free: volatile read only (INV-C3)
        StoreSnapshot snap = snapshot();
        List<CollectionInfo> result = new ArrayList<>();
        for (Map.Entry<String, CatalogEntry> e : snap.getCatalog().entrySet()) {
            CollectionState state = snap.getStates().get(e.getValue().getCollectionId());
            if (state != null) {
                result.add(new CollectionInfo(
                    e.getKey(),
                    state.getKind(),
                    state.getKeyCodec(),
                    state.getValueCodec()
                ));
            }
        }
        return result;
    }
    
    // ==================== Map ====================
    
    /**
     * Map 컬렉션 생성 (Write Lock 필수)
     *
     * <p>INV-C1: 단일 Writer 보장</p>
     *
     * @param name 컬렉션 이름
     * @param keyClass 키 타입
     * @param valueClass 값 타입
     * @return 생성된 NavigableMap
     */
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> NavigableMap<K, V> createMap(String name, Class<K> keyClass, Class<V> valueClass) {
        checkNotClosed();
        validateCollectionName(name);

        long stamp = acquireWriteLock();
        try {
            if (catalog.containsKey(name)) {
                throw FxException.alreadyExists("Collection already exists: " + name);
            }

            // 코덱 조회
            com.snoworca.fxstore.api.FxCodec<K> keyCodec = codecRegistry.get(keyClass);
            com.snoworca.fxstore.api.FxCodec<V> valueCodec = codecRegistry.get(valueClass);

            // Catalog 엔트리 생성
            long collectionId = nextCollectionId++;
            CatalogEntry catalogEntry = new CatalogEntry(name, collectionId);
            catalog.put(name, catalogEntry);

            // CollectionState 생성
            CodecRef keyCodecRef = new CodecRef(keyCodec.id(), keyCodec.version(), null);
            CodecRef valueCodecRef = new CodecRef(valueCodec.id(), valueCodec.version(), null);
            CollectionState state = new CollectionState(
                collectionId,
                CollectionKind.MAP,
                keyCodecRef,
                valueCodecRef,
                0L,  // 빈 rootPageId
                0L   // count
            );
            collectionStates.put(collectionId, state);

            markPendingChanges();

            // FxNavigableMapImpl 생성 및 캐시 (새 컬렉션이므로 업그레이드 불필요)
            Comparator<K> keyComparator = createComparator(keyCodec);
            com.snoworca.fxstore.collection.FxNavigableMapImpl<K, V> map =
                new com.snoworca.fxstore.collection.FxNavigableMapImpl<>(
                    this, collectionId, keyCodec, valueCodec, keyComparator,
                    null, null);
            openCollections.put(name, map);

            return map;
        } finally {
            releaseWriteLock(stamp);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> NavigableMap<K, V> openMap(String name, Class<K> keyClass, Class<V> valueClass) {
        checkNotClosed();

        // 원자적으로 캐시 확인 및 생성 (Thread-safe)
        @SuppressWarnings("unchecked")
        NavigableMap<K, V> result = (NavigableMap<K, V>) openCollections.computeIfAbsent(name, key -> {
            // catalog 확인
            if (!catalog.containsKey(key)) {
                throw FxException.notFound("Collection not found: " + key);
            }

            // State 조회
            CatalogEntry entry = catalog.get(key);
            CollectionState state = collectionStates.get(entry.getCollectionId());

            if (state.getKind() != CollectionKind.MAP) {
                throw FxException.typeMismatch("Collection is not a MAP: " + key);
            }

            // 코덱 조회
            com.snoworca.fxstore.api.FxCodec<K> keyCodec = codecRegistry.get(keyClass);
            com.snoworca.fxstore.api.FxCodec<V> valueCodec = codecRegistry.get(valueClass);

            // 코덱 검증 및 업그레이드 컨텍스트 생성
            CodecUpgradeContext keyUpgradeContext = validateCodec(state.getKeyCodec(), keyCodec, key);
            CodecUpgradeContext valueUpgradeContext = validateCodec(state.getValueCodec(), valueCodec, key);

            // FxNavigableMapImpl 생성
            Comparator<K> keyComparator = createComparator(keyCodec);
            return new com.snoworca.fxstore.collection.FxNavigableMapImpl<>(
                this, entry.getCollectionId(), keyCodec, valueCodec, keyComparator,
                keyUpgradeContext, valueUpgradeContext);
        });

        return result;
    }

    /**
     * Map 생성 또는 열기 (Thread-safe)
     *
     * <p>동시성 환경에서 race condition을 방지하기 위해
     * ALREADY_EXISTS 예외 발생 시 openMap으로 fallback합니다.</p>
     */
    @Override
    public <K, V> NavigableMap<K, V> createOrOpenMap(String name, Class<K> keyClass, Class<V> valueClass) {
        // 먼저 존재 여부 확인 (optimistic check)
        if (catalog.containsKey(name)) {
            return openMap(name, keyClass, valueClass);
        }
        // 생성 시도 - 다른 스레드가 먼저 생성했을 수 있음
        try {
            return createMap(name, keyClass, valueClass);
        } catch (FxException e) {
            if (e.getCode() == FxErrorCode.ALREADY_EXISTS) {
                // Race condition: 다른 스레드가 먼저 생성함 → open으로 fallback
                return openMap(name, keyClass, valueClass);
            }
            throw e;
        }
    }
    
    // ==================== Set ====================
    
    /**
     * Set 컬렉션 생성 (Write Lock 필수)
     *
     * <p>INV-C1: 단일 Writer 보장</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public <E> NavigableSet<E> createSet(String name, Class<E> elementClass) {
        checkNotClosed();
        validateCollectionName(name);

        long stamp = acquireWriteLock();
        try {
            if (catalog.containsKey(name)) {
                throw FxException.alreadyExists("Collection already exists: " + name);
            }

            // 코덱 조회
            com.snoworca.fxstore.api.FxCodec<E> elementCodec = codecRegistry.get(elementClass);

            // Catalog 엔트리 생성
            long collectionId = nextCollectionId++;
            CatalogEntry catalogEntry = new CatalogEntry(name, collectionId);
            catalog.put(name, catalogEntry);

            // CollectionState 생성 (Set은 keyCodec이 null)
            CodecRef elementCodecRef = new CodecRef(elementCodec.id(), elementCodec.version(), null);
            CollectionState state = new CollectionState(
                collectionId,
                CollectionKind.SET,
                null,  // Set은 keyCodec 없음
                elementCodecRef,
                0L,  // 빈 rootPageId
                0L   // count
            );
            collectionStates.put(collectionId, state);

            markPendingChanges();

            // FxNavigableSetImpl 생성 및 캐시 (새 컬렉션이므로 업그레이드 불필요)
            Comparator<E> elementComparator = createComparator(elementCodec);
            com.snoworca.fxstore.collection.FxNavigableSetImpl<E> set =
                new com.snoworca.fxstore.collection.FxNavigableSetImpl<>(
                    this, collectionId, elementCodec, elementComparator, null);
            openCollections.put(name, set);

            return set;
        } finally {
            releaseWriteLock(stamp);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> NavigableSet<E> openSet(String name, Class<E> elementClass) {
        checkNotClosed();

        // 원자적으로 캐시 확인 및 생성 (Thread-safe)
        @SuppressWarnings("unchecked")
        NavigableSet<E> result = (NavigableSet<E>) openCollections.computeIfAbsent(name, key -> {
            // catalog 확인
            if (!catalog.containsKey(key)) {
                throw FxException.notFound("Collection not found: " + key);
            }

            // State 조회
            CatalogEntry entry = catalog.get(key);
            CollectionState state = collectionStates.get(entry.getCollectionId());

            if (state.getKind() != CollectionKind.SET) {
                throw FxException.typeMismatch("Collection is not a SET: " + key);
            }

            // 코덱 조회
            com.snoworca.fxstore.api.FxCodec<E> elementCodec = codecRegistry.get(elementClass);

            // 코덱 검증 및 업그레이드 컨텍스트 생성
            CodecUpgradeContext elementUpgradeContext = validateCodec(state.getValueCodec(), elementCodec, key);

            // FxNavigableSetImpl 생성
            Comparator<E> elementComparator = createComparator(elementCodec);
            return new com.snoworca.fxstore.collection.FxNavigableSetImpl<>(
                this, entry.getCollectionId(), elementCodec, elementComparator, elementUpgradeContext);
        });

        return result;
    }

    /**
     * Set 생성 또는 열기 (Thread-safe)
     *
     * <p>동시성 환경에서 race condition을 방지하기 위해
     * ALREADY_EXISTS 예외 발생 시 openSet으로 fallback합니다.</p>
     */
    @Override
    public <E> NavigableSet<E> createOrOpenSet(String name, Class<E> elementClass) {
        if (catalog.containsKey(name)) {
            return openSet(name, elementClass);
        }
        try {
            return createSet(name, elementClass);
        } catch (FxException e) {
            if (e.getCode() == FxErrorCode.ALREADY_EXISTS) {
                return openSet(name, elementClass);
            }
            throw e;
        }
    }
    
    // ==================== List ====================

    /**
     * List 생성 (Write Lock 필수)
     *
     * <p>INV-C1: Single Writer 보장</p>
     *
     * @param name 컬렉션 이름
     * @param elementClass 요소 타입
     * @return 생성된 List
     */
    @Override
    @SuppressWarnings("unchecked")
    public <E> List<E> createList(String name, Class<E> elementClass) {
        checkNotClosed();
        validateCollectionName(name);

        long stamp = acquireWriteLock();
        try {
            if (catalog.containsKey(name)) {
                throw FxException.alreadyExists("Collection already exists: " + name);
            }

            // 코덱 조회
            com.snoworca.fxstore.api.FxCodec<E> elementCodec = codecRegistry.get(elementClass);

            // Catalog 엔트리 생성
            long collectionId = nextCollectionId++;
            CatalogEntry catalogEntry = new CatalogEntry(name, collectionId);
            catalog.put(name, catalogEntry);

            // CollectionState 생성 (List는 keyCodec 없음)
            CodecRef elementCodecRef = new CodecRef(elementCodec.id(), elementCodec.version(), null);
            CollectionState state = new CollectionState(
                collectionId,
                CollectionKind.LIST,
                null,  // List는 keyCodec 없음
                elementCodecRef,
                0L,  // 빈 rootPageId
                0L   // count
            );
            collectionStates.put(collectionId, state);

            markPendingChanges();

            // FxList 생성 및 캐시 (새 컬렉션이므로 업그레이드 불필요)
            com.snoworca.fxstore.ost.OST ost = new com.snoworca.fxstore.ost.OST(storage, allocator, options.pageSize().bytes());
            com.snoworca.fxstore.collection.FxList.RecordStore recordStore = createRecordStore(collectionId);
            com.snoworca.fxstore.collection.FxList<E> list =
                new com.snoworca.fxstore.collection.FxList<>(this, collectionId, ost, elementCodec, recordStore, null);
            openCollections.put(name, list);

            return list;
        } finally {
            releaseWriteLock(stamp);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> List<E> openList(String name, Class<E> elementClass) {
        checkNotClosed();

        // 원자적으로 캐시 확인 및 생성 (Thread-safe)
        @SuppressWarnings("unchecked")
        List<E> result = (List<E>) openCollections.computeIfAbsent(name, key -> {
            // catalog 확인
            if (!catalog.containsKey(key)) {
                throw FxException.notFound("Collection not found: " + key);
            }

            // State 조회
            CatalogEntry entry = catalog.get(key);
            CollectionState state = collectionStates.get(entry.getCollectionId());

            if (state.getKind() != CollectionKind.LIST) {
                throw FxException.typeMismatch("Collection is not a LIST: " + key);
            }

            // 코덱 조회
            com.snoworca.fxstore.api.FxCodec<E> elementCodec = codecRegistry.get(elementClass);

            // 코덱 검증 및 업그레이드 컨텍스트 생성
            CodecUpgradeContext elementUpgradeContext = validateCodec(state.getValueCodec(), elementCodec, key);

            // OST 복원
            com.snoworca.fxstore.ost.OST ost = com.snoworca.fxstore.ost.OST.open(
                storage, allocator, options.pageSize().bytes(), state.getRootPageId());

            // FxList 생성
            com.snoworca.fxstore.collection.FxList.RecordStore recordStore = createRecordStore(entry.getCollectionId());
            return new com.snoworca.fxstore.collection.FxList<>(this, entry.getCollectionId(), ost, elementCodec, recordStore, elementUpgradeContext);
        });

        return result;
    }

    /**
     * List 생성 또는 열기 (Thread-safe)
     *
     * <p>동시성 환경에서 race condition을 방지하기 위해
     * ALREADY_EXISTS 예외 발생 시 openList로 fallback합니다.</p>
     */
    @Override
    public <E> List<E> createOrOpenList(String name, Class<E> elementClass) {
        if (catalog.containsKey(name)) {
            return openList(name, elementClass);
        }
        try {
            return createList(name, elementClass);
        } catch (FxException e) {
            if (e.getCode() == FxErrorCode.ALREADY_EXISTS) {
                return openList(name, elementClass);
            }
            throw e;
        }
    }

    /**
     * List용 RecordStore 생성
     *
     * @param collectionId 컬렉션 ID
     * @return RecordStore 인스턴스
     */
    private com.snoworca.fxstore.collection.FxList.RecordStore createRecordStore(long collectionId) {
        return new com.snoworca.fxstore.collection.FxList.RecordStore() {
            @Override
            public long writeRecord(byte[] data) {
                long recordId = writeValueRecord(data);
                markPendingChanges();
                return recordId;
            }

            @Override
            public byte[] readRecord(long recordId) {
                return readValueRecord(recordId);
            }

            @Override
            public void deleteRecord(long recordId) {
                // 현재 구현에서는 dead space로 남김 (compactTo에서 정리)
                // 실제 삭제 로직은 필요 시 추가
            }
        };
    }
    
    // ==================== Deque ====================

    /**
     * Deque 생성 (Write Lock 필수)
     *
     * <p>INV-C1: Single Writer 보장</p>
     *
     * @param name 컬렉션 이름
     * @param elementClass 요소 타입
     * @return 생성된 Deque
     */
    @Override
    @SuppressWarnings("unchecked")
    public <E> Deque<E> createDeque(String name, Class<E> elementClass) {
        checkNotClosed();
        validateCollectionName(name);

        long stamp = acquireWriteLock();
        try {
            if (catalog.containsKey(name)) {
                throw FxException.alreadyExists("Collection already exists: " + name);
            }

            // 코덱 조회
            com.snoworca.fxstore.api.FxCodec<E> elementCodec = codecRegistry.get(elementClass);

            // Catalog 엔트리 생성
            long collectionId = nextCollectionId++;
            CatalogEntry catalogEntry = new CatalogEntry(name, collectionId);
            catalog.put(name, catalogEntry);

            // CollectionState 생성 (v0.7+: OrderedSeqEncoder 사용)
            CodecRef elementCodecRef = new CodecRef(elementCodec.id(), elementCodec.version(), null);
            CollectionState state = new CollectionState(
                collectionId,
                CollectionKind.DEQUE,
                null,  // Deque는 keyCodec 없음
                elementCodecRef,
                0L,  // 빈 rootPageId
                0L,  // count
                CollectionState.SEQ_ENCODER_VERSION_ORDERED  // v0.7+: O(log n) 지원
            );
            collectionStates.put(collectionId, state);

            markPendingChanges();

            // FxDequeImpl 생성 및 캐시 (v0.7+: OrderedSeqEncoder 사용)
            com.snoworca.fxstore.collection.FxDequeImpl<E> deque =
                new com.snoworca.fxstore.collection.FxDequeImpl<>(
                    this, collectionId, elementCodec, 0L, 0L, null,
                    com.snoworca.fxstore.collection.OrderedSeqEncoder.getInstance());
            openCollections.put(name, deque);

            return deque;
        } finally {
            releaseWriteLock(stamp);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Deque<E> openDeque(String name, Class<E> elementClass) {
        checkNotClosed();

        // 원자적으로 캐시 확인 및 생성 (Thread-safe)
        @SuppressWarnings("unchecked")
        Deque<E> result = (Deque<E>) openCollections.computeIfAbsent(name, key -> {
            // catalog 확인
            if (!catalog.containsKey(key)) {
                throw FxException.notFound("Collection not found: " + key);
            }

            // State 조회
            CatalogEntry entry = catalog.get(key);
            CollectionState state = collectionStates.get(entry.getCollectionId());

            if (state.getKind() != CollectionKind.DEQUE) {
                throw FxException.typeMismatch("Collection is not a DEQUE: " + key);
            }

            // 코덱 조회
            com.snoworca.fxstore.api.FxCodec<E> elementCodec = codecRegistry.get(elementClass);

            // 코덱 검증 및 업그레이드 컨텍스트 생성
            CodecUpgradeContext elementUpgradeContext = validateCodec(state.getValueCodec(), elementCodec, key);

            // v0.7: 저장된 인코더 버전에 따라 SeqEncoder 선택
            com.snoworca.fxstore.collection.SeqEncoder seqEncoder;
            if (state.getSeqEncoderVersion() == CollectionState.SEQ_ENCODER_VERSION_ORDERED) {
                seqEncoder = com.snoworca.fxstore.collection.OrderedSeqEncoder.getInstance();
            } else {
                seqEncoder = com.snoworca.fxstore.collection.LegacySeqEncoder.getInstance();
            }

            // headSeq, tailSeq를 BTree에서 복원
            long headSeq = 0L;
            long tailSeq = 0L;

            if (state.getRootPageId() != 0L) {
                // BTree에서 min/max 시퀀스 찾기
                BTree btree = getBTreeForCollection(entry.getCollectionId());
                com.snoworca.fxstore.btree.BTreeCursor cursor = btree.cursor();

                Long minSeq = null;
                Long maxSeq = null;

                while (cursor.hasNext()) {
                    BTree.Entry btreeEntry = cursor.next();
                    byte[] keyBytes = btreeEntry.getKey();

                    // SeqEncoder를 사용하여 시퀀스 디코딩
                    long seq = seqEncoder.decode(keyBytes);

                    if (minSeq == null || seq < minSeq) {
                        minSeq = seq;
                    }
                    if (maxSeq == null || seq > maxSeq) {
                        maxSeq = seq;
                    }
                }

                if (minSeq != null && maxSeq != null) {
                    headSeq = minSeq;
                    tailSeq = maxSeq + 1;
                }
            }

            // FxDequeImpl 생성 (저장된 인코더 버전 사용)
            return new com.snoworca.fxstore.collection.FxDequeImpl<>(
                this, entry.getCollectionId(), elementCodec, headSeq, tailSeq, elementUpgradeContext,
                seqEncoder);
        });

        return result;
    }
    
    /**
     * Deque 생성 또는 열기 (Thread-safe)
     *
     * <p>동시성 환경에서 race condition을 방지하기 위해
     * ALREADY_EXISTS 예외 발생 시 openDeque로 fallback합니다.</p>
     */
    @Override
    public <E> Deque<E> createOrOpenDeque(String name, Class<E> elementClass) {
        if (catalog.containsKey(name)) {
            return openDeque(name, elementClass);
        }
        try {
            return createDeque(name, elementClass);
        } catch (FxException e) {
            if (e.getCode() == FxErrorCode.ALREADY_EXISTS) {
                return openDeque(name, elementClass);
            }
            throw e;
        }
    }
    
    // ==================== 트랜잭션 ====================
    
    @Override
    public CommitMode commitMode() {
        checkNotClosed();
        return options.commitMode();
    }
    
    /**
     * 변경사항 커밋 (Write Lock 필수)
     *
     * <p>INV-C1: Single Writer 보장</p>
     */
    @Override
    public void commit() {
        checkNotClosed();
        long stamp = acquireWriteLock();
        try {
            doCommit();
        } finally {
            releaseWriteLock(stamp);
        }
    }

    /**
     * 실제 커밋 수행 (내부용)
     */
    private void doCommit() {
        // Catalog와 State를 BTree에 저장
        long newCatalogRootPageId = saveCatalog();
        long newStateRootPageId = saveState();

        // CommitHeader 갱신
        CommitHeader current = getCurrentCommitHeader();
        long newSeqNo = current.getSeqNo() + 1;

        CommitHeader updated = new CommitHeader(
            newSeqNo,
            current.getCommittedFlags(),
            allocator.getAllocTail(),  // 레거시 API 사용 (v0.9 전환 기간)
            newCatalogRootPageId,
            newStateRootPageId,
            nextCollectionId,
            System.currentTimeMillis()
        );

        // Slot A/B 교체 (seqNo가 짝수면 A, 홀수면 B)
        long slotOffset = (newSeqNo % 2 == 0)
            ? Superblock.SIZE
            : Superblock.SIZE + CommitHeader.SIZE;

        byte[] chBytes = updated.encode();
        storage.write(slotOffset, chBytes, 0, chBytes.length);

        if (options.durability() == Durability.SYNC) {
            storage.force(true);
        }

        hasPendingChanges = false;
    }
    
    /**
     * 변경사항 롤백 (Write Lock 필수)
     *
     * <p>INV-C1: Single Writer 보장</p>
     */
    @Override
    public void rollback() {
        checkNotClosed();

        if (options.commitMode() == CommitMode.AUTO) {
            return;  // AUTO 모드에서는 no-op
        }

        long stamp = acquireWriteLock();
        try {
            // BUG-002 수정: 캐시된 컬렉션 인스턴스 무효화
            // rollback 후 사용자는 openMap()으로 다시 컬렉션을 열어야 함
            openCollections.clear();

            // Pending 변경사항 폐기 (디스크에서 catalog/state 재로드)
            loadExistingStore();

            // workingAllocTail 복원 (CommitHeader에서)
            CommitHeader header = getCurrentCommitHeader();
            this.workingAllocTail = header.getAllocTail();

            // 스냅샷 재생성
            this.currentSnapshot = createInitialSnapshot();

            hasPendingChanges = false;
        } finally {
            releaseWriteLock(stamp);
        }
    }

    // ==================== 읽기 트랜잭션 ====================

    /**
     * 읽기 전용 트랜잭션 시작
     *
     * <p><b>INV-C3</b>: Wait-free - 어떤 락도 획득하지 않음</p>
     *
     * <p>트랜잭션 시작 시점의 스냅샷을 고정하여 일관된 읽기 뷰를 제공합니다.
     * 트랜잭션이 열려 있는 동안 다른 스레드의 쓰기가 발생해도
     * 이 트랜잭션 내에서는 시작 시점의 데이터만 보입니다.</p>
     *
     * @return 읽기 전용 트랜잭션
     * @throws IllegalStateException Store가 닫힌 경우
     */
    @Override
    public com.snoworca.fxstore.api.FxReadTransaction beginRead() {
        checkNotClosed();
        // Wait-free: 현재 스냅샷 참조만 획득 (volatile read)
        StoreSnapshot snap = snapshot();
        return new FxReadTransactionImpl(this, snap);
    }

    // ==================== 운영 ====================
    
    /**
     * 통계 조회 (Wait-free, FAST 모드)
     *
     * <p>INV-C3: 어떤 락도 획득하지 않음</p>
     *
     * @return 스토어 통계
     */
    @Override
    public Stats stats() {
        return stats(StatsMode.FAST);
    }

    /**
     * 통계 조회 (Wait-free)
     *
     * <p>INV-C3: 어떤 락도 획득하지 않음</p>
     *
     * @param mode 통계 모드 (FAST 또는 DEEP)
     * @return 스토어 통계
     */
    @Override
    public Stats stats(StatsMode mode) {
        checkNotClosed();

        // Wait-free: volatile read only (INV-C3)
        StoreSnapshot snap = snapshot();

        long fileBytes = storage.size();
        int collectionCount = snap.getCatalog().size();

        // 고정 오버헤드: Superblock + CommitHeader A + CommitHeader B
        long overhead = Superblock.SIZE + CommitHeader.SIZE * 2;

        // 할당된 영역 (allocTail까지) - snapshot에서 획득
        long allocatedBytes = snap.getAllocTail();

        // FAST 모드: 추정치 사용
        // - liveBytes: 할당된 바이트 전체 (dead 포함)
        // - deadBytes: 파일 크기에서 할당된 영역을 뺀 트레일링 공간
        //   (실제 dead bytes는 DEEP 스캔 없이 정확히 알 수 없음)
        long liveBytes;
        long deadBytes;

        if (mode == StatsMode.FAST) {
            // FAST: allocTail까지의 모든 데이터를 live로 간주
            liveBytes = allocatedBytes;
            // trailing space만 dead로 간주 (실제 내부 dead는 미계산)
            deadBytes = fileBytes - allocatedBytes;
        } else {
            // DEEP: 실제 라이브 데이터 스캔
            // Catalog와 State의 모든 페이지와 레코드를 순회하여 실제 사용량 계산
            liveBytes = calculateLiveBytes(overhead);
            deadBytes = allocatedBytes - liveBytes;
        }

        // 음수 방지
        if (deadBytes < 0) {
            deadBytes = 0;
        }

        // deadRatio 계산 (allocatedBytes 기준)
        double deadRatio = (allocatedBytes > 0) ?
            (double) deadBytes / allocatedBytes : 0.0;

        return new Stats(fileBytes, liveBytes, deadBytes, deadRatio, collectionCount);
    }

    /**
     * DEEP 모드: 실제 라이브 바이트 계산
     *
     * <p>Catalog, State, 모든 컬렉션의 루트 페이지를 순회하여
     * 실제로 사용 중인 바이트 수를 계산합니다.</p>
     *
     * @param overhead 고정 오버헤드 (Superblock + CommitHeaders)
     * @return 라이브 바이트 수
     */
    private long calculateLiveBytes(long overhead) {
        long liveBytes = overhead;

        try {
            CommitHeader header = getCurrentCommitHeader();

            // Catalog Tree 페이지 계산
            if (header.getCatalogRootPageId() != 0) {
                liveBytes += countTreeBytes(header.getCatalogRootPageId());
            }

            // State Tree 페이지 계산
            if (header.getStateRootPageId() != 0) {
                liveBytes += countTreeBytes(header.getStateRootPageId());
            }

            // 각 컬렉션의 데이터 페이지 계산
            for (CollectionState state : collectionStates.values()) {
                long rootPageId = state.getRootPageId();
                if (rootPageId != 0) {
                    liveBytes += countTreeBytes(rootPageId);
                }
            }

        } catch (FxException e) {
            // 오류 시 스냅샷 기반 추정치 사용 (v0.9: Stateless API)
            return currentSnapshot.getAllocTail();
        }

        return liveBytes;
    }

    /**
     * B-Tree/OST 페이지 바이트 수 계산 (재귀)
     *
     * <p>루트부터 시작하여 모든 노드 페이지의 바이트 수를 합산합니다.</p>
     *
     * @param rootPageId 루트 페이지 ID
     * @return 트리가 사용하는 총 바이트 수
     */
    private long countTreeBytes(long rootPageId) {
        if (rootPageId == 0) {
            return 0;
        }

        int pageSize = options.pageSize().bytes();
        long totalBytes = pageSize;  // 루트 페이지 자체

        try {
            byte[] pageData = new byte[pageSize];
            long offset = rootPageId * pageSize;
            storage.read(offset, pageData, 0, pageSize);

            // 페이지 타입 확인 (첫 바이트)
            int pageType = pageData[0] & 0xFF;

            // BTree Internal 또는 OST Internal 노드인 경우 자식 순회
            if (pageType == 1 || pageType == 3) {  // BTreeInternal or OSTInternal
                // 자식 페이지 수 (오프셋 4-7에 int로 저장됨)
                int childCount = ((pageData[4] & 0xFF)) |
                                ((pageData[5] & 0xFF) << 8) |
                                ((pageData[6] & 0xFF) << 16) |
                                ((pageData[7] & 0xFF) << 24);

                // 자식 페이지 ID들 순회 (오프셋 8부터 각 8바이트)
                for (int i = 0; i < childCount && i < 200; i++) {  // 최대 200개 제한
                    int childOffset = 8 + (i * 8);
                    if (childOffset + 8 <= pageSize) {
                        long childPageId =
                            ((long)(pageData[childOffset] & 0xFF)) |
                            ((long)(pageData[childOffset + 1] & 0xFF) << 8) |
                            ((long)(pageData[childOffset + 2] & 0xFF) << 16) |
                            ((long)(pageData[childOffset + 3] & 0xFF) << 24) |
                            ((long)(pageData[childOffset + 4] & 0xFF) << 32) |
                            ((long)(pageData[childOffset + 5] & 0xFF) << 40) |
                            ((long)(pageData[childOffset + 6] & 0xFF) << 48) |
                            ((long)(pageData[childOffset + 7] & 0xFF) << 56);

                        if (childPageId != 0) {
                            totalBytes += countTreeBytes(childPageId);
                        }
                    }
                }
            }
            // Leaf 노드는 추가 순회 불필요 (이미 pageSize 추가됨)

        } catch (Exception e) {
            // 페이지 읽기 실패 시 현재까지 계산된 값 반환
        }

        return totalBytes;
    }
    
    @Override
    public VerifyResult verify() {
        checkNotClosed();

        List<com.snoworca.fxstore.api.VerifyError> errors = new ArrayList<>();

        // 1. Superblock 검증
        verifySuperblock(errors);

        // 2. CommitHeader 검증 (A/B 슬롯)
        verifyCommitHeaders(errors);

        // 3. 할당 범위 검증
        verifyAllocTail(errors);

        // 4. Catalog/State 일관성 검증
        verifyCatalogState(errors);

        return new VerifyResult(errors.isEmpty(), errors);
    }

    /**
     * Superblock 무결성 검증
     */
    private void verifySuperblock(List<com.snoworca.fxstore.api.VerifyError> errors) {
        try {
            byte[] sbData = new byte[Superblock.SIZE];
            storage.read(0L, sbData, 0, Superblock.SIZE);

            // decode()는 magic과 CRC를 검증하고 실패 시 예외 발생
            Superblock sb = Superblock.decode(sbData);

            // 페이지 크기 일관성 검증
            if (sb.getPageSize() != options.pageSize().bytes()) {
                errors.add(new com.snoworca.fxstore.api.VerifyError(
                    VerifyErrorKind.SUPERBLOCK,
                    0L,
                    0L,
                    "Page size mismatch: file=" + sb.getPageSize() +
                    ", options=" + options.pageSize().bytes()
                ));
            }
        } catch (FxException e) {
            errors.add(new com.snoworca.fxstore.api.VerifyError(
                VerifyErrorKind.SUPERBLOCK,
                0L,
                0L,
                "Superblock verification failed: " + e.getMessage()
            ));
        }
    }

    /**
     * CommitHeader A/B 슬롯 검증
     */
    private void verifyCommitHeaders(List<com.snoworca.fxstore.api.VerifyError> errors) {
        long fileSize = storage.size();
        long minSize = Superblock.SIZE + CommitHeader.SIZE * 2;

        // 파일 크기가 충분하지 않으면 검증 스킵 (이미 allocTail 검증에서 처리됨)
        if (fileSize < minSize) {
            errors.add(new com.snoworca.fxstore.api.VerifyError(
                VerifyErrorKind.HEADER,
                0L,
                0L,
                "File too small for CommitHeaders: size=" + fileSize + ", required=" + minSize
            ));
            return;
        }

        byte[] slotA = new byte[CommitHeader.SIZE];
        byte[] slotB = new byte[CommitHeader.SIZE];

        storage.read(Superblock.SIZE, slotA, 0, CommitHeader.SIZE);
        storage.read(Superblock.SIZE + CommitHeader.SIZE, slotB, 0, CommitHeader.SIZE);

        boolean aValid = false;
        boolean bValid = false;
        boolean aUninitialized = isSlotUninitialized(slotA);
        boolean bUninitialized = isSlotUninitialized(slotB);
        CommitHeader headerA = null;
        CommitHeader headerB = null;

        // Slot A 검증 (미초기화된 슬롯은 에러로 처리하지 않음)
        if (!aUninitialized) {
            try {
                headerA = CommitHeader.decode(slotA);
                aValid = headerA.verify(slotA);
                if (!aValid) {
                    errors.add(new com.snoworca.fxstore.api.VerifyError(
                        VerifyErrorKind.HEADER,
                        Superblock.SIZE,
                        0L,
                        "CommitHeader Slot A: CRC mismatch"
                    ));
                }
            } catch (FxException e) {
                errors.add(new com.snoworca.fxstore.api.VerifyError(
                    VerifyErrorKind.HEADER,
                    Superblock.SIZE,
                    0L,
                    "CommitHeader Slot A: " + e.getMessage()
                ));
            }
        }

        // Slot B 검증 (미초기화된 슬롯은 에러로 처리하지 않음)
        if (!bUninitialized) {
            try {
                headerB = CommitHeader.decode(slotB);
                bValid = headerB.verify(slotB);
                if (!bValid) {
                    errors.add(new com.snoworca.fxstore.api.VerifyError(
                        VerifyErrorKind.HEADER,
                        Superblock.SIZE + CommitHeader.SIZE,
                        0L,
                        "CommitHeader Slot B: CRC mismatch"
                    ));
                }
            } catch (FxException e) {
                errors.add(new com.snoworca.fxstore.api.VerifyError(
                    VerifyErrorKind.HEADER,
                    Superblock.SIZE + CommitHeader.SIZE,
                    0L,
                    "CommitHeader Slot B: " + e.getMessage()
                ));
            }
        }

        // 최소 하나의 슬롯이 유효해야 함 (critical error)
        // 단, 미초기화된 슬롯은 유효하지 않은 것으로 취급
        if (!aValid && !bValid) {
            // 둘 다 미초기화된 경우도 에러
            errors.add(new com.snoworca.fxstore.api.VerifyError(
                VerifyErrorKind.HEADER,
                Superblock.SIZE,
                0L,
                "Both CommitHeader slots are corrupted or uninitialized - data recovery required"
            ));
        }

        // seqNo 단조 증가 검증 (INV-1)
        if (aValid && bValid && headerA != null && headerB != null) {
            // 두 헤더의 seqNo 차이는 1이어야 함 (정상적인 커밋 시)
            long diff = Math.abs(headerA.getSeqNo() - headerB.getSeqNo());
            if (diff > 1) {
                errors.add(new com.snoworca.fxstore.api.VerifyError(
                    VerifyErrorKind.HEADER,
                    Superblock.SIZE,
                    0L,
                    "CommitHeader seqNo gap > 1: A=" + headerA.getSeqNo() +
                    ", B=" + headerB.getSeqNo()
                ));
            }
        }
    }

    /**
     * 할당 범위 검증
     */
    private void verifyAllocTail(List<com.snoworca.fxstore.api.VerifyError> errors) {
        try {
            CommitHeader header = getCurrentCommitHeader();
            long allocTail = header.getAllocTail();
            long fileSize = storage.size();

            // allocTail이 파일 크기보다 크면 안됨
            if (allocTail > fileSize) {
                errors.add(new com.snoworca.fxstore.api.VerifyError(
                    VerifyErrorKind.RECORD,
                    allocTail,
                    0L,
                    "allocTail (" + allocTail + ") exceeds file size (" + fileSize + ")"
                ));
            }

            // allocTail은 최소한 헤더 영역 이후여야 함
            long minAllocTail = Superblock.SIZE + CommitHeader.SIZE * 2;
            if (allocTail < minAllocTail) {
                errors.add(new com.snoworca.fxstore.api.VerifyError(
                    VerifyErrorKind.RECORD,
                    allocTail,
                    0L,
                    "allocTail (" + allocTail + ") is less than minimum (" + minAllocTail + ")"
                ));
            }
        } catch (FxException e) {
            // 이미 CommitHeader 검증에서 오류가 추가됨
        }
    }

    /**
     * 슬롯이 미초기화 상태인지 확인
     * (magic이 일치하지 않거나 모든 바이트가 0인 경우)
     */
    private boolean isSlotUninitialized(byte[] slotData) {
        // magic이 일치하는지 확인
        if (slotData.length < CommitHeader.MAGIC.length) {
            return true;
        }

        // magic 불일치 = 미초기화
        for (int i = 0; i < CommitHeader.MAGIC.length; i++) {
            if (slotData[i] != CommitHeader.MAGIC[i]) {
                return true;
            }
        }

        return false;
    }

    /**
     * Catalog/State 일관성 검증
     */
    private void verifyCatalogState(List<com.snoworca.fxstore.api.VerifyError> errors) {
        // 메모리 내 Catalog과 State의 일관성 검증
        for (Map.Entry<String, CatalogEntry> entry : catalog.entrySet()) {
            long collectionId = entry.getValue().getCollectionId();
            CollectionState state = collectionStates.get(collectionId);

            if (state == null) {
                errors.add(new com.snoworca.fxstore.api.VerifyError(
                    VerifyErrorKind.RECORD,
                    0L,
                    collectionId,
                    "Catalog entry '" + entry.getKey() +
                    "' has no matching CollectionState"
                ));
            }
        }

        // CollectionState에만 있고 Catalog에 없는 경우 검증
        for (Map.Entry<Long, CollectionState> entry : collectionStates.entrySet()) {
            long collectionId = entry.getKey();
            boolean foundInCatalog = false;

            for (CatalogEntry catEntry : catalog.values()) {
                if (catEntry.getCollectionId() == collectionId) {
                    foundInCatalog = true;
                    break;
                }
            }

            if (!foundInCatalog) {
                errors.add(new com.snoworca.fxstore.api.VerifyError(
                    VerifyErrorKind.RECORD,
                    0L,
                    collectionId,
                    "CollectionState (id=" + collectionId +
                    ") has no matching Catalog entry"
                ));
            }
        }
    }
    
    @Override
    public void compactTo(Path destination) {
        checkNotClosed();

        // BATCH 모드에서 pending 변경이 있으면 에러
        if (options.commitMode() == CommitMode.BATCH && hasPendingChanges) {
            throw FxException.illegalArgument(
                "Cannot compact with pending changes. Commit or rollback first."
            );
        }

        // 새 Store 생성 (AUTO 커밋 모드로 - 각 컬렉션 복사 후 자동 커밋)
        FxOptions compactOptions = FxOptions.defaults()
            .withCommitMode(CommitMode.AUTO)
            .durability(Durability.SYNC)  // 안전한 동기화
            .pageSize(options.pageSize())
            .fileLock(FileLockMode.NONE)  // 임시 파일이므로 락 불필요
            .build();

        try (FxStore targetStore = FxStoreImpl.open(destination, compactOptions)) {
            // 모든 컬렉션 복사
            for (CollectionInfo info : list()) {
                copyCollection(info, targetStore);
            }
        } catch (Exception e) {
            // 실패 시 대상 파일 삭제 시도
            try {
                java.nio.file.Files.deleteIfExists(destination);
            } catch (java.io.IOException ioe) {
                // 삭제 실패 무시
            }

            if (e instanceof FxException) {
                throw (FxException) e;
            }
            throw new FxException(FxErrorCode.IO, "Compaction failed", e);
        }
    }

    /**
     * 개별 컬렉션을 대상 Store로 복사
     *
     * @param info 컬렉션 정보
     * @param targetStore 대상 Store
     */
    @SuppressWarnings("unchecked")
    private void copyCollection(CollectionInfo info, FxStore targetStore) {
        String name = info.name();
        CollectionKind kind = info.kind();

        switch (kind) {
            case MAP:
                copyMap(name, info, targetStore);
                break;
            case SET:
                copySet(name, info, targetStore);
                break;
            case LIST:
                copyList(name, info, targetStore);
                break;
            case DEQUE:
                copyDeque(name, info, targetStore);
                break;
            default:
                // 알 수 없는 종류는 스킵
                break;
        }
    }

    /**
     * CodecRef에서 Java 클래스 추출
     */
    private Class<?> codecRefToClass(CodecRef codecRef) {
        if (codecRef == null) {
            return Object.class;
        }

        FxType type = codecRef.getType();
        if (type != null) {
            switch (type) {
                case I64:
                    return Long.class;
                case F64:
                    return Double.class;
                case STRING:
                    return String.class;
                case BYTES:
                    return byte[].class;
                default:
                    return Object.class;
            }
        }

        // FxType이 null인 경우 codecId로 판단 (내장 코덱)
        String codecId = codecRef.getCodecId();
        if (codecId != null) {
            switch (codecId) {
                case "fx:i64":
                    // Byte, Short, Integer, Long 모두 I64로 저장됨
                    // compactTo에서는 Long으로 통일하여 처리
                    return Long.class;
                case "fx:f64":
                    // Float, Double 모두 F64로 저장됨
                    return Double.class;
                case "fx:string:utf8":
                    return String.class;
                case "fx:bytes:lenlex":
                    return byte[].class;
                default:
                    break;
            }
        }

        // 사용자 정의 코덱 - 타입을 알 수 없음
        return Object.class;
    }

    /**
     * Map 컬렉션 복사
     */
    @SuppressWarnings("unchecked")
    private void copyMap(String name, CollectionInfo info, FxStore targetStore) {
        Class<?> keyClass = codecRefToClass(info.keyCodec());
        Class<?> valueClass = codecRefToClass(info.valueCodec());

        NavigableMap<Object, Object> sourceMap =
            (NavigableMap<Object, Object>) openMap(name, keyClass, valueClass);

        NavigableMap<Object, Object> targetMap =
            (NavigableMap<Object, Object>) targetStore.createMap(name, keyClass, valueClass);

        for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
            targetMap.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Set 컬렉션 복사
     */
    @SuppressWarnings("unchecked")
    private void copySet(String name, CollectionInfo info, FxStore targetStore) {
        // Set은 valueCodec를 element 타입으로 사용
        Class<?> elementClass = codecRefToClass(info.valueCodec());

        NavigableSet<Object> sourceSet =
            (NavigableSet<Object>) openSet(name, elementClass);

        NavigableSet<Object> targetSet =
            (NavigableSet<Object>) targetStore.createSet(name, elementClass);

        for (Object element : sourceSet) {
            targetSet.add(element);
        }
    }

    /**
     * List 컬렉션 복사
     */
    @SuppressWarnings("unchecked")
    private void copyList(String name, CollectionInfo info, FxStore targetStore) {
        Class<?> elementClass = codecRefToClass(info.valueCodec());

        List<Object> sourceList =
            (List<Object>) openList(name, elementClass);

        List<Object> targetList =
            (List<Object>) targetStore.createList(name, elementClass);

        targetList.addAll(sourceList);
    }

    /**
     * Deque 컬렉션 복사
     */
    @SuppressWarnings("unchecked")
    private void copyDeque(String name, CollectionInfo info, FxStore targetStore) {
        Class<?> elementClass = codecRefToClass(info.valueCodec());

        Deque<Object> sourceDeque =
            (Deque<Object>) openDeque(name, elementClass);

        Deque<Object> targetDeque =
            (Deque<Object>) targetStore.createDeque(name, elementClass);

        for (Object element : sourceDeque) {
            targetDeque.addLast(element);
        }
    }
    
    /**
     * 스토어 닫기 (Write Lock 필수)
     *
     * <p>INV-C1: Single Writer 보장</p>
     *
     * <p>BATCH 모드에서 pending 변경이 있으면 OnClosePolicy에 따라 처리합니다.</p>
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        long stamp = acquireWriteLock();
        try {
            if (options.commitMode() == CommitMode.BATCH && hasPendingChanges) {
                switch (options.onClosePolicy()) {
                    case ERROR:
                        throw FxException.illegalArgument("Pending changes exist on close");
                    case COMMIT:
                        // 이미 Write Lock을 잡았으므로 doCommit() 직접 호출
                        doCommit();
                        break;
                    case ROLLBACK:
                        // Pending 변경사항 폐기 (재로드)
                        loadExistingStore();
                        hasPendingChanges = false;
                        break;
                }
            }

            openCollections.clear();
            try {
                storage.close();
            } catch (Exception e) {
                // Close exception은 무시 (리소스는 이미 해제됨)
            }
            closed = true;
        } finally {
            releaseWriteLock(stamp);
        }
    }
    
    // ==================== 내부 메서드 ====================
    
    /**
     * 컬렉션 변경 발생 시 호출 (AUTO 모드에서 즉시 커밋)
     *
     * <p>Phase 8: 레거시 필드 변경을 snapshot에 동기화합니다.</p>
     *
     * <p><b>주의:</b> 이 메서드는 Write Lock을 잡은 상태에서 호출됩니다.
     * StampedLock은 재진입을 지원하지 않으므로, commit() 대신 doCommit()을
     * 직접 호출합니다.</p>
     */
    public void markPendingChanges() {
        // Phase 8: Legacy fields → Snapshot 동기화
        syncLegacyToSnapshot();

        if (options.commitMode() == CommitMode.AUTO) {
            // 이미 Write Lock을 잡은 상태이므로 doCommit() 직접 호출 (deadlock 방지)
            doCommit();
        } else {
            hasPendingChanges = true;
        }
    }
    
    private void checkNotClosed() {
        if (closed) {
            throw FxException.illegalState("Store is closed");
        }
    }
    
    /**
     * 컬렉션 이름 검증
     */
    private void validateCollectionName(String name) {
        if (name == null || name.isEmpty()) {
            throw FxException.illegalArgument("Collection name cannot be null or empty");
        }
        if (name.length() > 255) {
            throw FxException.illegalArgument("Collection name too long (max 255 bytes): " + name);
        }
    }
    
    /**
     * 코덱 검증 및 업그레이드 컨텍스트 생성
     *
     * @param expected 파일에 저장된 코덱 정보
     * @param actual 현재 등록된 코덱
     * @param collectionName 컬렉션 이름 (에러 메시지용)
     * @return 업그레이드 필요 시 CodecUpgradeContext, 불필요 시 null
     * @throws FxException 코덱 ID 불일치 또는 업그레이드 미허용 시
     */
    private <T> CodecUpgradeContext validateCodec(CodecRef expected,
                                                   com.snoworca.fxstore.api.FxCodec<T> actual,
                                                   String collectionName) {
        // 1. 코덱 ID 검증 (변경 불가)
        if (!expected.getCodecId().equals(actual.id())) {
            throw FxException.typeMismatch(
                "Codec ID mismatch for collection '" + collectionName + "': " +
                "expected=" + expected.getCodecId() + ", actual=" + actual.id());
        }

        // 2. 버전 검증
        if (expected.getCodecVersion() != actual.version()) {
            if (!options.allowCodecUpgrade()) {
                throw FxException.versionMismatch(
                    "Codec version mismatch for collection '" + collectionName + "': " +
                    "expected=" + expected.getCodecVersion() + ", actual=" + actual.version() +
                    ". Set allowCodecUpgrade=true to enable migration.");
            }

            // 업그레이드 허용됨 - 컨텍스트 생성
            com.snoworca.fxstore.api.FxCodecUpgradeHook hook = options.codecUpgradeHook();
            if (hook == null) {
                // 훅 없이 허용만 한 경우 - 경고 출력 후 null 반환
                // WARNING: 디코딩 실패 가능성을 사용자에게 알림
                System.err.println("[FxStore WARNING] Codec version mismatch for collection '"
                    + collectionName + "': stored=" + expected.getCodecVersion()
                    + ", current=" + actual.version()
                    + ". No FxCodecUpgradeHook registered. Data may fail to decode. "
                    + "Consider registering a hook via FxOptions.codecUpgradeHook().");
                return null;
            }

            return new CodecUpgradeContext(
                actual.id(),
                expected.getCodecVersion(),
                actual.version(),
                hook
            );
        }

        return null; // 업그레이드 불필요
    }

    /**
     * CollectionState 조회 (내부용)
     */
    public CollectionState getCollectionState(long collectionId) {
        return collectionStates.get(collectionId);
    }

    /**
     * CollectionState 조회 by 이름 (마이그레이션용)
     *
     * @param name 컬렉션 이름
     * @return CollectionState 또는 null
     * @since 0.7
     */
    public CollectionState getCollectionState(String name) {
        CatalogEntry entry = catalog.get(name);
        if (entry == null) {
            return null;
        }
        return collectionStates.get(entry.getCollectionId());
    }

    /**
     * CollectionState 조회 by ID (마이그레이션용)
     *
     * @param collectionId 컬렉션 ID
     * @return CollectionState 또는 null
     * @since 0.7
     */
    public CollectionState getCollectionStateById(long collectionId) {
        return collectionStates.get(collectionId);
    }

    /**
     * 컬렉션의 현재 엔트리 수 반환 (PERF-001: O(1) size 지원)
     *
     * <p>CollectionState에 저장된 count를 직접 반환합니다.</p>
     *
     * @param collectionId 컬렉션 ID
     * @return 엔트리 수, 컬렉션이 없으면 0
     */
    public long getCollectionCount(long collectionId) {
        CollectionState state = collectionStates.get(collectionId);
        return state != null ? state.getCount() : 0;
    }

    /**
     * CollectionState 업데이트 (마이그레이션용)
     *
     * <p><b>전제조건:</b> 쓰기 락을 보유한 상태에서 호출해야 합니다.</p>
     *
     * @param collectionId 컬렉션 ID
     * @param newState 새 CollectionState
     * @since 0.7
     */
    public void updateCollectionState(long collectionId, CollectionState newState) {
        collectionStates.put(collectionId, newState);

        // 스냅샷 업데이트
        StoreSnapshot newSnapshot = currentSnapshot.withState(collectionId, newState);
        publishSnapshot(newSnapshot);

        markPendingChanges();
    }
    
    /**
     * Storage 접근 (내부용)
     */
    public Storage getStorage() {
        return storage;
    }
    
    /**
     * Allocator 접근 (내부용)
     */
    public Allocator getAllocator() {
        return allocator;
    }
    
    /**
     * PageCache 접근 (내부용)
     */
    public PageCache getPageCache() {
        return pageCache;
    }
    
    /**
     * PageSize 접근 (내부용)
     */
    public int getPageSize() {
        return options.pageSize().bytes();
    }
    
    /**
     * CommitMode 접근 (내부용)
     */
    public CommitMode getCommitMode() {
        return options.commitMode();
    }
    
    /**
     * 컬렉션의 BTree 조회 (내부용) - 기본 unsigned byte 비교
     *
     * <p><b>전제조건:</b> 쓰기 락을 보유한 상태에서 호출해야 합니다.</p>
     *
     * <p><b>주의:</b> 이 메서드는 Deque (OrderedSeqEncoder) 등 unsigned byte 비교가
     * 적합한 경우에만 사용하세요. NavigableMap/Set은 코덱 기반 비교를 사용하는
     * {@link #getBTreeForCollection(long, FxCodec)} 를 사용하세요.</p>
     */
    public BTree getBTreeForCollection(long collectionId) {
        CollectionState state = collectionStates.get(collectionId);
        if (state == null) {
            throw FxException.notFound("Collection not found: id=" + collectionId);
        }

        long rootPageId = state.getRootPageId();

        // 바이트 비교자 생성 (unsigned byte lexicographic order)
        // Deque의 OrderedSeqEncoder가 XOR + BigEndian으로 unsigned 순서를 생성하므로
        // 이 비교자와 호환됨
        Comparator<byte[]> byteComparator = createUnsignedByteComparator();

        // allocator를 전달하여 페이지 할당 일관성 유지
        // (레거시 API 사용 - allocator가 내부적으로 allocTail 관리)
        return new BTree(storage, options.pageSize().bytes(), byteComparator, rootPageId, allocator);
    }

    /**
     * 컬렉션의 BTree 조회 (코덱 기반 비교) - NavigableMap/Set 전용
     *
     * <p><b>전제조건:</b> 쓰기 락을 보유한 상태에서 호출해야 합니다.</p>
     *
     * <p>코덱의 {@link FxCodec#compareBytes(byte[], byte[])} 메서드를 사용하여
     * 타입에 맞는 정확한 비교를 수행합니다. I64Codec 등 signed 숫자 타입에서
     * 올바른 정렬 순서를 보장합니다.</p>
     *
     * @param collectionId 컬렉션 ID
     * @param keyCodec 키 코덱 (compareBytes 사용)
     * @return BTree 인스턴스
     * @throws FxException NOT_FOUND if collection not found
     */
    public BTree getBTreeForCollection(long collectionId, com.snoworca.fxstore.api.FxCodec<?> keyCodec) {
        CollectionState state = collectionStates.get(collectionId);
        if (state == null) {
            throw FxException.notFound("Collection not found: id=" + collectionId);
        }

        long rootPageId = state.getRootPageId();

        // 코덱의 compareBytes 사용 - 타입에 맞는 정확한 비교
        Comparator<byte[]> byteComparator = (a, b) -> keyCodec.compareBytes(a, b);

        return new BTree(storage, options.pageSize().bytes(), byteComparator, rootPageId, allocator);
    }

    /**
     * Unsigned byte 비교자 생성 (Deque 등 unsigned 순서 인코딩용)
     */
    private Comparator<byte[]> createUnsignedByteComparator() {
        return new Comparator<byte[]>() {
            @Override
            public int compare(byte[] a, byte[] b) {
                int minLen = Math.min(a.length, b.length);
                for (int i = 0; i < minLen; i++) {
                    int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                return a.length - b.length;
            }
        };
    }
    
    /**
     * 값 레코드 작성 (내부용)
     *
     * <p><b>전제조건:</b> 쓰기 락을 보유한 상태에서 호출해야 합니다.</p>
     *
     * @param valueBytes 저장할 값 바이트
     * @return 페이지 ID (offset / pageSize)
     */
    public long writeValueRecord(byte[] valueBytes) {
        // 레거시 API 사용 (v0.9 전환 기간 동안 유지)
        // allocator.allocatePage()는 offset을 반환
        long offset = allocator.allocatePage();
        long pageId = offset / options.pageSize().bytes();

        // 간단한 값 레코드: 길이(4) + 데이터
        byte[] record = new byte[4 + valueBytes.length];
        record[0] = (byte) (valueBytes.length & 0xFF);
        record[1] = (byte) ((valueBytes.length >> 8) & 0xFF);
        record[2] = (byte) ((valueBytes.length >> 16) & 0xFF);
        record[3] = (byte) ((valueBytes.length >> 24) & 0xFF);
        System.arraycopy(valueBytes, 0, record, 4, valueBytes.length);

        storage.write(offset, record, 0, record.length);

        return pageId;
    }
    
    /**
     * 값 레코드 읽기 (내부용)
     */
    public byte[] readValueRecord(long pageId) {
        long offset = pageId * options.pageSize().bytes();
        
        // 길이 읽기
        byte[] lenBytes = new byte[4];
        storage.read(offset, lenBytes, 0, 4);
        
        int len = (lenBytes[0] & 0xFF) | 
                  ((lenBytes[1] & 0xFF) << 8) | 
                  ((lenBytes[2] & 0xFF) << 16) | 
                  ((lenBytes[3] & 0xFF) << 24);
        
        // 데이터 읽기
        byte[] valueBytes = new byte[len];
        storage.read(offset + 4, valueBytes, 0, len);
        
        return valueBytes;
    }
    
    /**
     * 키 비교자 생성 (코덱 기반)
     *
     * <p>BUG-V12-001 수정: 코덱의 compareBytes()를 사용하여 signed 숫자 타입에서
     * 올바른 정렬 순서를 보장합니다.</p>
     *
     * @param codec 키 코덱
     * @return 키 비교자
     */
    private <K> Comparator<K> createComparator(com.snoworca.fxstore.api.FxCodec<K> codec) {
        return new Comparator<K>() {
            @Override
            public int compare(K a, K b) {
                byte[] aBytes = codec.encode(a);
                byte[] bBytes = codec.encode(b);
                // 코덱의 compareBytes 사용 - 타입에 맞는 정확한 비교
                return codec.compareBytes(aBytes, bBytes);
            }
        };
    }

    // ==================== 동시성 인프라 메서드 (v0.4+) ====================

    /**
     * 초기 스냅샷 생성
     *
     * <p>Store 초기화 시 현재 상태를 스냅샷으로 변환합니다.</p>
     *
     * @return 초기 StoreSnapshot
     */
    private StoreSnapshot createInitialSnapshot() {
        // rootPageIds 구성: collectionId → rootPageId
        Map<Long, Long> rootPageIds = new HashMap<>();
        for (CollectionState state : collectionStates.values()) {
            rootPageIds.put(state.getCollectionId(), state.getRootPageId());
        }

        return new StoreSnapshot(
            0L,  // seqNo (초기값)
            allocator.getAllocTail(),
            catalog,
            collectionStates,
            rootPageIds,
            nextCollectionId
        );
    }

    /**
     * 현재 스냅샷 획득 (Wait-free)
     *
     * <p><b>INV-C3</b>: 어떤 락도 획득하지 않음</p>
     *
     * <p>이 메서드는 volatile read만 수행하므로 어떤 상황에서도
     * 블로킹되지 않습니다. 반환된 스냅샷은 불변이므로
     * 안전하게 사용할 수 있습니다.</p>
     *
     * @return 현재 StoreSnapshot (불변)
     */
    public StoreSnapshot snapshot() {
        return currentSnapshot;  // volatile read only
    }

    /**
     * 쓰기 락 획득
     *
     * <p><b>INV-C1</b>: 단일 Writer 보장</p>
     *
     * <p>이 메서드를 호출한 스레드만 Store 상태를 변경할 수 있습니다.
     * 반드시 try-finally 블록에서 {@link #releaseWriteLock(long)}을 호출해야 합니다.</p>
     *
     * <pre>{@code
     * long stamp = store.acquireWriteLock();
     * try {
     *     // 쓰기 작업
     * } finally {
     *     store.releaseWriteLock(stamp);
     * }
     * }</pre>
     *
     * @return 락 해제에 필요한 stamp 값
     */
    public long acquireWriteLock() {
        long stamp = lock.writeLock();
        // 쓰기 연산 시작 시 workingAllocTail 초기화 (v0.9 Stateless API 지원)
        workingAllocTail = currentSnapshot.getAllocTail();
        return stamp;
    }

    /**
     * 쓰기 락 해제
     *
     * <p><b>중요:</b> 반드시 try-finally 블록에서 호출해야 합니다!
     * 예외가 발생해도 락이 해제되지 않으면 전체 Store가 교착 상태에 빠집니다.</p>
     *
     * @param stamp acquireWriteLock()에서 반환된 stamp 값
     */
    public void releaseWriteLock(long stamp) {
        lock.unlockWrite(stamp);
    }

    /**
     * 스냅샷 원자적 교체
     *
     * <p><b>INV-C4</b>: 단일 volatile write로 원자적 교체</p>
     *
     * <p>이 메서드는 반드시 쓰기 락을 보유한 상태에서 호출해야 합니다.
     * 새 스냅샷이 게시되면 이후의 모든 읽기 스레드는
     * 새 스냅샷을 보게 됩니다.</p>
     *
     * @param newSnapshot 새 스냅샷 (불변)
     */
    public void publishSnapshot(StoreSnapshot newSnapshot) {
        this.currentSnapshot = newSnapshot;  // volatile write
    }

    // ==================== 컬렉션 연산용 헬퍼 (Phase 8 동시성 지원) ====================

    /**
     * 현재 작업 중 allocTail 반환
     *
     * <p>COW 연산 후 새 스냅샷 생성 시 사용됩니다.</p>
     *
     * <p><b>전제조건:</b> 쓰기 락을 보유한 상태에서 호출해야 합니다.</p>
     *
     * @return 현재 workingAllocTail
     * @since 0.9
     */
    public long getAllocTail() {
        return workingAllocTail;
    }

    /**
     * BTree의 allocTail을 workingAllocTail에 동기화
     *
     * <p>BTree 연산 완료 후 호출하여 allocTail을 동기화합니다.</p>
     *
     * <p><b>전제조건:</b> 쓰기 락을 보유한 상태에서 호출해야 합니다.</p>
     *
     * @param btree BTree 인스턴스
     * @since 0.9
     */
    public void syncBTreeAllocTail(BTree btree) {
        workingAllocTail = btree.getAllocTail();
    }

    /**
     * 컬렉션 루트 업데이트 및 스냅샷 게시 (원자적)
     *
     * <p><b>전제조건:</b> 쓰기 락을 보유한 상태에서 호출해야 합니다.</p>
     *
     * <p>COW BTree 연산 후 새 루트 페이지 ID를 스냅샷에 반영합니다.
     * 레거시 상태(collectionStates)도 함께 업데이트하여 일관성을 유지합니다.</p>
     *
     * @param collectionId 컬렉션 ID
     * @param newRootPageId 새 루트 페이지 ID
     */
    public void updateCollectionRootAndPublish(long collectionId, long newRootPageId) {
        // 레거시 상태 업데이트 (backward compatibility)
        // v0.7: withRootPageId()를 사용하여 seqEncoderVersion 보존
        CollectionState state = collectionStates.get(collectionId);
        if (state != null) {
            CollectionState updatedState = state.withRootPageId(newRootPageId);
            collectionStates.put(collectionId, updatedState);
        }

        // 새 스냅샷 생성 및 게시 (레거시 API 사용)
        StoreSnapshot newSnapshot = currentSnapshot.withRootAndAllocTail(
            collectionId, newRootPageId, allocator.getAllocTail());
        publishSnapshot(newSnapshot);
    }

    /**
     * 컬렉션 루트 및 count 업데이트 후 스냅샷 게시 (원자적)
     *
     * <p><b>전제조건:</b> 쓰기 락을 보유한 상태에서 호출해야 합니다.</p>
     *
     * <p>v0.7+: Deque 연산 시 rootPageId와 count를 함께 업데이트합니다.
     * 이를 통해 FxReadTransaction.size(Deque)가 O(1)로 동작합니다.</p>
     *
     * @param collectionId 컬렉션 ID
     * @param newRootPageId 새 루트 페이지 ID
     * @param newCount 새 count
     * @since 0.7
     */
    public void updateCollectionRootCountAndPublish(long collectionId, long newRootPageId, long newCount) {
        // 레거시 상태 업데이트 (backward compatibility)
        CollectionState state = collectionStates.get(collectionId);
        if (state != null) {
            CollectionState updatedState = state.withRootAndCount(newRootPageId, newCount);
            collectionStates.put(collectionId, updatedState);
        }

        // 새 스냅샷 생성 및 게시 (count 포함, 레거시 API 사용)
        StoreSnapshot newSnapshot = currentSnapshot.withRootCountAndAllocTail(
            collectionId, newRootPageId, newCount, allocator.getAllocTail());
        publishSnapshot(newSnapshot);
    }

    /**
     * 컬렉션 연산 후 커밋 처리
     *
     * <p><b>전제조건:</b> 쓰기 락을 보유한 상태에서 호출해야 합니다.</p>
     *
     * <p>AUTO 모드에서 변경 후 즉시 커밋이 필요할 때 사용합니다.
     * commit()은 Write Lock을 다시 획득하려 하므로 deadlock을 방지하기 위해
     * 이미 락을 보유한 상태에서는 doCommit()을 직접 호출합니다.</p>
     */
    public void commitIfAuto() {
        if (options.commitMode() == CommitMode.AUTO) {
            doCommit();
        } else {
            hasPendingChanges = true;
        }
    }

    /**
     * 레거시 필드 상태를 스냅샷으로 동기화 후 게시
     *
     * <p>레거시 코드에서 mutable 필드를 변경한 후
     * 이 메서드를 호출하여 스냅샷을 업데이트합니다.</p>
     *
     * <p><b>주의:</b> 쓰기 락을 보유한 상태에서만 호출해야 합니다.</p>
     */
    protected void syncLegacyToSnapshot() {
        // rootPageIds 구성
        Map<Long, Long> rootPageIds = new HashMap<>();
        for (CollectionState state : collectionStates.values()) {
            rootPageIds.put(state.getCollectionId(), state.getRootPageId());
        }

        // Note: syncLegacyToSnapshot()은 쓰기 락 없이 호출될 수 있으므로
        // workingAllocTail 대신 currentSnapshot.getAllocTail() 사용
        // (이 메서드는 Phase 4에서 제거 예정)
        StoreSnapshot newSnapshot = new StoreSnapshot(
            currentSnapshot.getSeqNo() + 1,
            currentSnapshot.getAllocTail(),
            catalog,
            collectionStates,
            rootPageIds,
            nextCollectionId
        );

        publishSnapshot(newSnapshot);
    }

    /**
     * AUTO 커밋 모드 여부 확인
     *
     * @return AUTO 모드이면 true
     */
    public boolean isAutoCommit() {
        return options.commitMode() == CommitMode.AUTO;
    }
}
