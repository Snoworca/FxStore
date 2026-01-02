# FxStore

> English | **[한국어](README.md)**

**Java 8-based Single-File Persistent Collection Library**

[![Maven Central](https://img.shields.io/maven-central/v/com.snoworca/fxstore.svg)](https://central.sonatype.com/artifact/com.snoworca/fxstore)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Coverage](https://img.shields.io/badge/Coverage-92%25-brightgreen.svg)](https://github.com/Snoworca/FxStore)

FxStore is an embedded database that implements standard Java collection interfaces (`NavigableMap`(Map), `NavigableSet`(Set), `List`, `Deque`) while **persistently storing data in a single file**.

## Key Features

- **Zero-Dependency**: No external dependencies, uses only Java 8 standard library
- **Single-File Storage**: All data stored in one file (easy backup/move/delete)
- **Crash Consistency**: COW (Copy-on-Write) based, guarantees data consistency even after crashes
- **Java Collections API Compatible**: Easy integration into existing Java code

## Installation

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

## Quick Start

```java
import com.snoworca.fxstore.api.FxStore;
import java.nio.file.Paths;
import java.util.NavigableMap;

public class QuickStart {
    public static void main(String[] args) {
        // Open store
        FxStore store = FxStore.open(Paths.get("mydata.fx"));

        // Create map and store data
        NavigableMap<Long, String> users = store.createOrOpenMap(
            "users", Long.class, String.class);

        users.put(1L, "Alice");
        users.put(2L, "Bob");
        users.put(3L, "Charlie");

        // Query data
        System.out.println(users.get(1L));  // "Alice"

        // Close store
        store.close();

        // Data persists after restart!
    }
}
```

### Using Other Collections

```java
FxStore store = FxStore.open(Paths.get("data.fx"));

// Set: Store unique elements
NavigableSet<String> tags = store.createOrOpenSet("tags", String.class);
tags.add("java");
tags.add("database");

// List: Ordered list
List<String> logs = store.createOrOpenList("logs", String.class);
logs.add("Application started");

// Deque: Double-ended queue (used as Stack/Queue)
Deque<String> tasks = store.createOrOpenDeque("tasks", String.class);
tasks.addLast("Task 1");
String next = tasks.pollFirst();

store.close();
```

### Memory Mode (For Testing)

```java
// Operates only in memory without file
FxStore store = FxStore.openMemory();
// ... test code ...
store.close();
```

## Supported Collections

| Collection | Interface | Use Case |
|------------|-----------|----------|
| **Map** | `NavigableMap<K, V>` | Key-value storage, range queries, sorted order |
| **Set** | `NavigableSet<E>` | Unique elements, range queries, sorted order |
| **List** | `List<E>` | Index-based access, order preservation |
| **Deque** | `Deque<E>` | Double-ended queue, used as Stack/Queue |

## Supported Types (Built-in Codecs)

| Type | Description |
|------|-------------|
| `Long` | 64-bit integer |
| `Integer` | 32-bit integer |
| `Short` | 16-bit integer |
| `Byte` | 8-bit integer |
| `Double` | 64-bit floating point |
| `Float` | 32-bit floating point |
| `String` | UTF-8 string |
| `byte[]` | Byte array |

Custom types can be registered by implementing the `FxCodec` interface.

## Documentation

For detailed usage and tutorials, see the [User Guide](docs/guide.en/00.index.md).

### Getting Started

| Document | Description |
|----------|-------------|
| [Introduction](docs/guide.en/01.introduction.md) | What FxStore is and when to use it |
| [Quick Start](docs/guide.en/02.quick-start.md) | Run your first code in 5 minutes |
| [Installation](docs/guide.en/03.installation.md) | Gradle/Maven setup |
| [Core Concepts](docs/guide.en/04.core-concepts.md) | Store, collections, codecs, commit modes |

### Tutorials

| Document | Level | Description |
|----------|-------|-------------|
| [Using Map](docs/guide.en/05.tutorials/01.basic-map.md) | Beginner | NavigableMap CRUD, range queries |
| [Using Set](docs/guide.en/05.tutorials/02.basic-set.md) | Beginner | NavigableSet usage |
| [Using List](docs/guide.en/05.tutorials/03.basic-list.md) | Beginner | List index access |
| [Using Deque](docs/guide.en/05.tutorials/04.basic-deque.md) | Beginner | Double-ended queue, Stack/Queue |
| [BATCH Mode](docs/guide.en/05.tutorials/05.batch-mode.md) | Intermediate | Explicit commit, rollback |
| [Read Transactions](docs/guide.en/05.tutorials/06.read-transaction.md) | Intermediate | Consistent snapshot reads |
| [Custom Codecs](docs/guide.en/05.tutorials/07.custom-codec.md) | Advanced | Storing custom types |
| [Performance Tuning](docs/guide.en/05.tutorials/08.performance.md) | Advanced | Optimization techniques |

### API Reference

| Document | Description |
|----------|-------------|
| [FxStore](docs/guide.en/06.api-reference/fxstore.md) | Main interface |
| [FxOptions](docs/guide.en/06.api-reference/fxoptions.md) | Configuration options |
| [FxCodec](docs/guide.en/06.api-reference/fxcodec.md) | Codec interface |
| [FxReadTransaction](docs/guide.en/06.api-reference/fxreadtransaction.md) | Read transaction |
| [Exception Handling](docs/guide.en/06.api-reference/exceptions.md) | Error codes and exceptions |

### Examples

| Document | Description |
|----------|-------------|
| [User Cache](docs/guide.en/07.examples/01.user-cache.md) | User information caching |
| [Time-Series Data](docs/guide.en/07.examples/02.time-series.md) | Time-series data storage |
| [Task Queue](docs/guide.en/07.examples/03.task-queue.md) | Persistent task queue |
| [Session Store](docs/guide.en/07.examples/04.session-store.md) | Session management |

### Reference Materials

| Document | Description |
|----------|-------------|
| [Troubleshooting](docs/guide.en/08.troubleshooting.md) | Common problems and solutions |
| [FAQ](docs/guide.en/09.faq.md) | Frequently asked questions |
| [Glossary](docs/guide.en/10.glossary.md) | Key term definitions |

## Use Cases

### Suitable For

- Embedded database: Data storage without a separate server
- Local cache: Cache that persists after application restart
- Configuration/state storage: Application settings, user preferences
- Log/event storage: Time-series data, event logs
- Task queue: Persistent message queue, job queue

### Not Suitable For

- Large-scale distributed systems
- Multi-process concurrent writes
- Multi-terabyte scale data
- Complex SQL queries

## License

[Apache License 2.0](LICENSE)

## Contributing

Issues and pull requests are welcome on [GitHub](https://github.com/snoworca/FxStore).
