# FxStore 동시성 지원 연구 보고서

**작성일**: 2025-12-27
**상태**: 연구 완료, 설계안 제시

---

## 1. 현재 상태 분석

### 1.1 공유 가변 상태 (동기화 필요)

| 컴포넌트 | 필드 | 현재 동기화 | 위험도 |
|---------|------|------------|-------|
| **Allocator** | `allocTail`, `pendingAllocTail` | 없음 | **CRITICAL** |
| **PageCache** | `LinkedHashMap<Long, byte[]>` | 없음 | **HIGH** |
| **FxStoreImpl** | `catalog`, `collectionStates`, `openCollections` | 없음 | **CRITICAL** |
| **BTree** | `rootPageId` (COW 시 변경) | 없음 | **HIGH** |
| **FileStorage** | `FileChannel` | 없음 | **HIGH** |
| **MemoryStorage** | `ByteBuffer` | synchronized | MEDIUM |

### 1.2 읽기 vs 쓰기 경로

**읽기 전용 경로:**
```
get(key) → BTree.find() → readNode() → storage.read()
```
- Catalog, CollectionStates, Allocator 변경 없음
- 여러 스레드가 동시에 안전하게 읽기 가능

**쓰기 경로:**
```
put(key, value)
  → allocator.allocPage()     // allocTail 변경
  → BTree.insert()            // COW로 새 페이지 생성
  → markCollectionChanged()   // collectionStates 변경
  → commit()                  // CommitHeader 기록
```

### 1.3 핵심 문제

**서로 다른 컬렉션도 공유 상태 사용:**
```
mapA.put() ─┬─► Allocator.allocTail (공유)
            ├─► Storage.write() (공유)
            └─► StateTree (공유)

mapB.put() ─┬─► Allocator.allocTail (공유) ← 충돌!
            ├─► Storage.write() (공유)
            └─► StateTree (공유)
```

---

## 2. 동시성 전략 비교

### 2.1 Option A: ReentrantReadWriteLock

```java
private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

public V get(K key) {
    rwLock.readLock().lock();
    try {
        return doGet(key);
    } finally {
        rwLock.readLock().unlock();
    }
}

public V put(K key, V value) {
    rwLock.writeLock().lock();
    try {
        return doPut(key, value);
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

**장점:**
- 구현 단순
- Reentrant (재진입 가능)
- 잘 알려진 패턴

**단점:**
- Writer starvation 가능성
- 읽기 락 획득 시 캐시 무효화로 성능 저하
- 읽기 락 최대 65,536개 제한 (Virtual Thread 환경에서 문제)

**성능:** 읽기 락 오버헤드가 ~2000 CPU cycles

### 2.2 Option B: StampedLock (Optimistic Read)

```java
private final StampedLock stampedLock = new StampedLock();

public V get(K key) {
    long stamp = stampedLock.tryOptimisticRead();
    V value = doGet(key);
    if (!stampedLock.validate(stamp)) {
        // Optimistic read 실패, 읽기 락으로 전환
        stamp = stampedLock.readLock();
        try {
            value = doGet(key);
        } finally {
            stampedLock.unlockRead(stamp);
        }
    }
    return value;
}

public V put(K key, V value) {
    long stamp = stampedLock.writeLock();
    try {
        return doPut(key, value);
    } finally {
        stampedLock.unlockWrite(stamp);
    }
}
```

**장점:**
- ReadWriteLock 대비 읽기 ~50% 빠름
- Optimistic read는 Writer를 차단하지 않음
- Writer starvation 없음

**단점:**
- **Not reentrant** (자기 자신에게 deadlock 가능)
- 구현 복잡도 증가
- Optimistic read 실패 시 재시도 비용

**성능:** Read-heavy 워크로드에서 최고

### 2.3 Option C: LMDB 스타일 (Single Writer + MVCC Snapshots)

```java
// 단일 Writer 스레드 + 읽기 전용 스냅샷
private final Lock writeLock = new ReentrantLock();
private volatile Snapshot currentSnapshot;

public ReadTransaction beginRead() {
    // 락 없이 현재 스냅샷 참조만 획득
    return new ReadTransaction(currentSnapshot);
}

public WriteTransaction beginWrite() {
    writeLock.lock();
    return new WriteTransaction(currentSnapshot, writeLock);
}
```

**장점:**
- 읽기는 완전히 Wait-free
- Writer가 Reader를 차단하지 않음
- Reader가 Writer를 차단하지 않음
- 선형 확장성 (읽기 스레드 수에 비례)
- FxStore의 COW와 자연스럽게 결합

**단점:**
- 구현 복잡도 높음
- 오래된 스냅샷이 공간을 차지 (GC 필요)
- Single writer 제약

**성능:** 읽기 최적화, 쓰기 직렬화

### 2.4 Option D: Lock Striping (컬렉션별 락)

```java
private final ConcurrentHashMap<String, ReentrantReadWriteLock> collectionLocks;
private final ReentrantLock globalWriteLock; // Allocator, Commit 용

public V get(String collection, K key) {
    ReentrantReadWriteLock lock = collectionLocks.get(collection);
    lock.readLock().lock();
    try {
        return doGet(collection, key);
    } finally {
        lock.readLock().unlock();
    }
}

public V put(String collection, K key, V value) {
    ReentrantReadWriteLock lock = collectionLocks.get(collection);
    lock.writeLock().lock();
    globalWriteLock.lock(); // Allocator 보호
    try {
        return doPut(collection, key, value);
    } finally {
        globalWriteLock.unlock();
        lock.writeLock().unlock();
    }
}
```

**장점:**
- 서로 다른 컬렉션 간 읽기 동시성 최대화
- 특정 컬렉션 쓰기가 다른 컬렉션 읽기 차단 안 함

**단점:**
- Allocator는 여전히 글로벌 락 필요
- 락 순서 관리 복잡 (deadlock 위험)
- 메모리 오버헤드 (컬렉션당 락 객체)

---

## 3. 업계 사례 연구

### 3.1 LMDB (Lightning Memory-Mapped Database)

> "LMDB databases may have only one writer at a time, however unlike many similar key-value databases, write transactions do not block readers, nor do readers block writers."

**모델:**
- Single Writer + Multiple Readers (SWMR)
- Copy-on-Write로 MVCC 구현
- 읽기는 스냅샷 기반, 완전히 Wait-free
- Writer는 글로벌 mutex로 직렬화

**FxStore와의 유사점:**
- 이미 COW 사용 중
- A/B 슬롯 스위칭으로 커밋 원자성 보장
- 단일 스레드 설계 의도

### 3.2 SQLite (WAL 모드)

> "SQLite uses a single-writer and multiple-reader (SWMR) concurrency model"

**모델:**
- Write-Ahead Logging (WAL)
- 읽기/쓰기 동시 가능
- Checkpoint으로 WAL → 메인 DB 병합

**FxStore와의 차이:**
- FxStore는 WAL 없이 COW만 사용
- WAL은 쓰기 성능 향상, COW는 읽기 성능 향상

### 3.3 RocksDB

**모델:**
- 내부적으로 Lock-free 구조 많이 사용
- Column Family별 독립적 Memtable
- 백그라운드 Compaction

---

## 4. 권장안: Hybrid LMDB-Style

### 4.1 선택 근거

| 요소 | ReadWriteLock | StampedLock | LMDB-Style |
|-----|---------------|-------------|------------|
| 읽기 성능 | O | O+ | **O++** |
| 구현 복잡도 | **O++** | O | O- |
| Deadlock 위험 | Low | **HIGH** | Low |
| COW 호환성 | O | O | **O++** |
| 확장성 | O | O+ | **O++** |
| Reentrant | **Yes** | No | N/A |

**결론: LMDB 스타일 + StampedLock 조합 권장**

### 4.2 제안 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        FxStore                               │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │  WriteGuard     │    │  ReadSnapshot                   │ │
│  │  (StampedLock   │    │  (volatile reference)           │ │
│  │   writeLock)    │    │                                 │ │
│  └────────┬────────┘    └─────────────┬───────────────────┘ │
│           │                           │                     │
│           ▼                           ▼                     │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Immutable Snapshot                         ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  ││
│  │  │ allocTail   │  │ rootPageIds │  │ collectionState │  ││
│  │  │ (immutable) │  │ (immutable) │  │ (immutable)     │  ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  ││
│  └─────────────────────────────────────────────────────────┘│
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    Storage Layer                        ││
│  │  (Append-only, COW pages never modified after write)    ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 4.3 핵심 설계 원칙

1. **Immutable Snapshot**: 모든 메타데이터를 불변 스냅샷으로 패키징
2. **Single Writer**: StampedLock.writeLock()으로 쓰기 직렬화
3. **Wait-free Reads**: volatile 스냅샷 참조만 읽음
4. **COW 활용**: 페이지는 쓰기 후 불변, 새 버전만 생성

---

## 5. 상세 구현 설계

### 5.1 새 클래스: `StoreSnapshot`

```java
/**
 * 불변 스냅샷 - Store의 특정 시점 상태
 *
 * Thread-safety: Immutable, 모든 스레드에서 안전하게 읽기 가능
 */
public final class StoreSnapshot {
    private final long seqNo;
    private final long allocTail;
    private final Map<String, CatalogEntry> catalog;      // Immutable copy
    private final Map<Long, CollectionState> states;      // Immutable copy
    private final Map<Long, Long> rootPageIds;            // collectionId → rootPageId

    // 생성자에서 방어적 복사 + Collections.unmodifiableMap()
    public StoreSnapshot(
        long seqNo,
        long allocTail,
        Map<String, CatalogEntry> catalog,
        Map<Long, CollectionState> states,
        Map<Long, Long> rootPageIds
    ) {
        this.seqNo = seqNo;
        this.allocTail = allocTail;
        this.catalog = Collections.unmodifiableMap(new HashMap<>(catalog));
        this.states = Collections.unmodifiableMap(new HashMap<>(states));
        this.rootPageIds = Collections.unmodifiableMap(new HashMap<>(rootPageIds));
    }

    // Getters only, no setters
}
```

### 5.2 수정: `FxStoreImpl`

```java
public class FxStoreImpl implements FxStore {

    // 기존 필드 제거:
    // - private final Map<String, CatalogEntry> catalog;
    // - private final Map<Long, CollectionState> collectionStates;

    // 새 필드:
    private final StampedLock lock = new StampedLock();
    private volatile StoreSnapshot currentSnapshot;

    // === 읽기 경로 (Wait-free) ===

    @Override
    public boolean exists(String name) {
        // 락 없이 volatile 읽기만
        StoreSnapshot snapshot = currentSnapshot;
        return snapshot.getCatalog().containsKey(name);
    }

    public <K,V> V mapGet(long collectionId, K key) {
        StoreSnapshot snapshot = currentSnapshot;
        long rootPageId = snapshot.getRootPageId(collectionId);
        // BTree는 immutable 페이지만 읽으므로 안전
        return btreeFind(rootPageId, key);
    }

    // === 쓰기 경로 (Write Lock) ===

    @Override
    public <K,V> V mapPut(long collectionId, K key, V value) {
        long stamp = lock.writeLock();
        try {
            StoreSnapshot snapshot = currentSnapshot;

            // 1. 새 페이지 할당 (COW)
            long newPageId = allocator.allocPage();

            // 2. BTree 수정 (새 root 생성)
            long newRootPageId = btreeInsert(
                snapshot.getRootPageId(collectionId),
                key, value
            );

            // 3. 새 스냅샷 생성
            Map<Long, Long> newRootPages = new HashMap<>(snapshot.getRootPageIds());
            newRootPages.put(collectionId, newRootPageId);

            StoreSnapshot newSnapshot = new StoreSnapshot(
                snapshot.getSeqNo() + 1,
                allocator.getAllocTail(),
                snapshot.getCatalog(),
                snapshot.getStates(),
                newRootPages
            );

            // 4. 원자적 스냅샷 교체 (volatile write)
            this.currentSnapshot = newSnapshot;

            // 5. AUTO 모드면 커밋
            if (options.commitMode() == CommitMode.AUTO) {
                persistCommitHeader(newSnapshot);
            }

            return oldValue;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
```

### 5.3 읽기 전용 트랜잭션 (Optional, 향후 확장)

```java
/**
 * 읽기 전용 트랜잭션 - 스냅샷 고정
 */
public class ReadTransaction implements AutoCloseable {
    private final StoreSnapshot snapshot;
    private volatile boolean closed = false;

    ReadTransaction(StoreSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public <K,V> V get(FxNavigableMap<K,V> map, K key) {
        checkNotClosed();
        // 고정된 스냅샷으로 읽기
        return map.getWithSnapshot(snapshot, key);
    }

    @Override
    public void close() {
        closed = true;
        // 스냅샷 참조 해제 (GC 허용)
    }
}
```

### 5.4 PageCache 동기화

```java
public class PageCache {
    private final StampedLock lock = new StampedLock();
    private final LinkedHashMap<Long, byte[]> cache;

    public byte[] get(long pageId) {
        // Optimistic read 시도
        long stamp = lock.tryOptimisticRead();
        byte[] data = cache.get(pageId);
        if (lock.validate(stamp)) {
            return data;
        }

        // 실패 시 읽기 락
        stamp = lock.readLock();
        try {
            return cache.get(pageId);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public void put(long pageId, byte[] data) {
        long stamp = lock.writeLock();
        try {
            cache.put(pageId, data);
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
```

---

## 6. 구현 계획

### Phase 1: 핵심 인프라 (1주)
- [ ] `StoreSnapshot` 클래스 구현
- [ ] `FxStoreImpl`에 StampedLock 추가
- [ ] 기존 mutable 상태를 snapshot 기반으로 전환

### Phase 2: 읽기 경로 최적화 (1주)
- [ ] 모든 읽기 메서드를 wait-free로 전환
- [ ] PageCache에 StampedLock 적용
- [ ] 동시 읽기 테스트

### Phase 3: 쓰기 경로 안전화 (1주)
- [ ] 쓰기 메서드에 writeLock 적용
- [ ] 스냅샷 교체 원자성 검증
- [ ] BATCH 모드 동시성 테스트

### Phase 4: 테스트 및 벤치마크 (1주)
- [ ] 멀티스레드 스트레스 테스트
- [ ] 성능 벤치마크 (단일 vs 다중 스레드)
- [ ] Race condition 탐지 (jcstress)

---

## 7. 대안 검토: 단순 ReadWriteLock

만약 LMDB 스타일이 과도하다면, 단순 ReadWriteLock도 충분할 수 있습니다:

```java
public class FxStoreImpl implements FxStore {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    // 읽기 작업
    public V get(K key) {
        rwLock.readLock().lock();
        try {
            return doGet(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    // 쓰기 작업
    public V put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            return doPut(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

**장점:** 구현 간단, 안정적
**단점:** 읽기 성능 최적화 포기

---

## 8. 결론 및 권장사항

### 최종 권장: **Option B + C 하이브리드**

1. **StampedLock** 사용 (ReadWriteLock 대비 50% 빠른 읽기)
2. **Immutable Snapshot** 패턴 적용 (LMDB 스타일)
3. **Wait-free 읽기** 구현 (volatile snapshot 참조)
4. **Single Writer 직렬화** (writeLock)

이 조합은:
- FxStore의 기존 COW 아키텍처와 자연스럽게 결합
- 읽기 heavy 워크로드에서 최적 성능
- 구현 복잡도를 관리 가능한 수준으로 유지

### 개발자 결정 필요

1. **복잡도 vs 성능 트레이드오프**: LMDB 스타일 vs 단순 ReadWriteLock
2. **ReadTransaction API 추가 여부**: 스냅샷 격리 명시적 지원
3. **Virtual Thread 고려**: Java 21+ 환경이면 StampedLock 필수

---

## 참고 자료

- [LMDB Wikipedia](https://en.wikipedia.org/wiki/Lightning_Memory-Mapped_Database)
- [How LMDB Works](https://xgwang.me/posts/how-lmdb-works/)
- [StampedLock Performance](https://medium.com/@apusingh1967/low-latency-programming-stampedlock-is-the-champion-a8b07f8c95be)
- [Java StampedLock Dangers](https://www.javaspecialists.eu/archive/Issue321-StampedLock-ReadWriteLock-Dangers.html)
- [SQLite vs LMDB](https://db-engines.com/en/system/LMDB%3BSQLite)
