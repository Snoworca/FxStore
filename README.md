# FxStore

**Java 8 기반 단일 파일 영속 컬렉션 라이브러리**

[![Maven Central](https://img.shields.io/maven-central/v/com.snoworca/fxstore.svg)](https://central.sonatype.com/artifact/com.snoworca/fxstore)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Coverage](https://img.shields.io/badge/Coverage-92%25-brightgreen.svg)](https://github.com/Snoworca/FxStore)

FxStore는 Java 표준 컬렉션 인터페이스(`NavigableMap`(Map), `NavigableSet`(Set), `List`, `Deque`)를 구현하면서 데이터를 **단일 파일에 영속적으로 저장**하는 임베디드 데이터베이스입니다.

## 주요 특징

- **Zero-Dependency**: 외부 의존성 없음, Java 8 표준 라이브러리만 사용
- **단일 파일 저장**: 모든 데이터가 하나의 파일에 저장 (백업/이동/삭제 간편)
- **크래시 일관성**: COW(Copy-on-Write) 기반으로 크래시 후에도 데이터 일관성 보장
- **Java Collections API 호환**: 기존 Java 코드에 쉽게 통합

## 설치

### Gradle

```groovy
dependencies {
    implementation 'com.snoworca:fxstore:0.3.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.snoworca</groupId>
    <artifactId>fxstore</artifactId>
    <version>0.3.0</version>
</dependency>
```

## 빠른 시작

```java
import com.snoworca.fxstore.api.FxStore;
import java.nio.file.Paths;
import java.util.NavigableMap;

public class QuickStart {
    public static void main(String[] args) {
        // Store 열기
        FxStore store = FxStore.open(Paths.get("mydata.fx"));

        // Map 생성 및 데이터 저장
        NavigableMap<Long, String> users = store.createOrOpenMap(
            "users", Long.class, String.class);

        users.put(1L, "Alice");
        users.put(2L, "Bob");
        users.put(3L, "Charlie");

        // 데이터 조회
        System.out.println(users.get(1L));  // "Alice"

        // Store 닫기
        store.close();

        // 재시작 후에도 데이터 유지됨!
    }
}
```

### 다른 컬렉션 사용

```java
FxStore store = FxStore.open(Paths.get("data.fx"));

// Set: 고유 요소 저장
NavigableSet<String> tags = store.createOrOpenSet("tags", String.class);
tags.add("java");
tags.add("database");

// List: 순서가 있는 목록
List<String> logs = store.createOrOpenList("logs", String.class);
logs.add("Application started");

// Deque: 양방향 큐 (Stack/Queue로 활용)
Deque<String> tasks = store.createOrOpenDeque("tasks", String.class);
tasks.addLast("Task 1");
String next = tasks.pollFirst();

store.close();
```

### 메모리 모드 (테스트용)

```java
// 파일 없이 메모리에서만 동작
FxStore store = FxStore.openMemory();
// ... 테스트 코드 ...
store.close();
```

## 지원 컬렉션

| 컬렉션 | 인터페이스 | 용도 |
|--------|-----------|------|
| **Map** | `NavigableMap<K, V>` | 키-값 저장, 범위 조회, 정렬 순서 |
| **Set** | `NavigableSet<E>` | 고유 요소, 범위 조회, 정렬 순서 |
| **List** | `List<E>` | 인덱스 기반 접근, 순서 유지 |
| **Deque** | `Deque<E>` | 양방향 큐, Stack/Queue로 활용 |

## 지원 타입 (내장 코덱)

| 타입 | 설명 |
|------|------|
| `Long` | 64비트 정수 |
| `Integer` | 32비트 정수 |
| `Short` | 16비트 정수 |
| `Byte` | 8비트 정수 |
| `Double` | 64비트 부동소수점 |
| `Float` | 32비트 부동소수점 |
| `String` | UTF-8 문자열 |
| `byte[]` | 바이트 배열 |

커스텀 타입은 `FxCodec` 인터페이스를 구현하여 등록할 수 있습니다.

## 문서

상세한 사용법과 튜토리얼은 [사용자 가이드](docs/guide/00.index.md)를 참조하세요.

### 시작하기

| 문서 | 설명 |
|------|------|
| [소개](docs/guide/01.introduction.md) | FxStore가 무엇인지, 언제 사용하는지 |
| [빠른 시작](docs/guide/02.quick-start.md) | 5분 내 첫 코드 실행 |
| [설치](docs/guide/03.installation.md) | Gradle/Maven 설정 |
| [핵심 개념](docs/guide/04.core-concepts.md) | Store, 컬렉션, 코덱, 커밋 모드 |

### 튜토리얼

| 문서 | 난이도 | 설명 |
|------|--------|------|
| [Map 사용하기](docs/guide/05.tutorials/01.basic-map.md) | 초급 | NavigableMap CRUD, 범위 조회 |
| [Set 사용하기](docs/guide/05.tutorials/02.basic-set.md) | 초급 | NavigableSet 사용법 |
| [List 사용하기](docs/guide/05.tutorials/03.basic-list.md) | 초급 | List 인덱스 접근 |
| [Deque 사용하기](docs/guide/05.tutorials/04.basic-deque.md) | 초급 | 양방향 큐, Stack/Queue |
| [BATCH 모드](docs/guide/05.tutorials/05.batch-mode.md) | 중급 | 명시적 커밋, 롤백 |
| [읽기 트랜잭션](docs/guide/05.tutorials/06.read-transaction.md) | 중급 | 일관된 스냅샷 읽기 |
| [커스텀 코덱](docs/guide/05.tutorials/07.custom-codec.md) | 고급 | 사용자 정의 타입 저장 |
| [성능 튜닝](docs/guide/05.tutorials/08.performance.md) | 고급 | 최적화 방법 |

### API 레퍼런스

| 문서 | 설명 |
|------|------|
| [FxStore](docs/guide/06.api-reference/fxstore.md) | 메인 인터페이스 |
| [FxOptions](docs/guide/06.api-reference/fxoptions.md) | 설정 옵션 |
| [FxCodec](docs/guide/06.api-reference/fxcodec.md) | 코덱 인터페이스 |
| [FxReadTransaction](docs/guide/06.api-reference/fxreadtransaction.md) | 읽기 트랜잭션 |
| [예외 처리](docs/guide/06.api-reference/exceptions.md) | 에러 코드 및 예외 |

### 예제

| 문서 | 설명 |
|------|------|
| [사용자 캐시](docs/guide/07.examples/01.user-cache.md) | 사용자 정보 캐싱 |
| [시계열 데이터](docs/guide/07.examples/02.time-series.md) | 시계열 데이터 저장 |
| [작업 큐](docs/guide/07.examples/03.task-queue.md) | 영속적인 작업 큐 |
| [세션 저장소](docs/guide/07.examples/04.session-store.md) | 세션 관리 |

### 참고 자료

| 문서 | 설명 |
|------|------|
| [문제 해결](docs/guide/08.troubleshooting.md) | 일반적인 문제와 해결 방법 |
| [FAQ](docs/guide/09.faq.md) | 자주 묻는 질문 |
| [용어집](docs/guide/10.glossary.md) | 주요 용어 정의 |

## 사용 사례

### 적합한 경우

- 임베디드 데이터베이스: 별도 서버 없이 데이터 저장
- 로컬 캐시: 애플리케이션 재시작 후에도 유지되는 캐시
- 설정/상태 저장: 애플리케이션 설정, 사용자 선호도
- 로그/이벤트 저장: 시계열 데이터, 이벤트 로그
- 작업 큐: 영속적인 메시지 큐, 작업 대기열

### 부적합한 경우

- 대규모 분산 시스템
- 다중 프로세스 동시 쓰기
- 수 TB 이상 대용량 데이터
- 복잡한 SQL 쿼리

## 라이선스

[Apache License 2.0](LICENSE)

## 기여

이슈 및 풀 리퀘스트는 [GitHub](https://github.com/snoworca/FxStore)에서 환영합니다.
