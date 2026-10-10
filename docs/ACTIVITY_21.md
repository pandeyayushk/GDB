# Activity 21: Repository Pattern

## Goal

Decouple `AccountService` from storage details using repository interfaces. The service talks to `AccountRepository` and `TransactionRepository`; an in-memory implementation supplies the default storage selected by `config/persistence.properties`.

```text
AccountService ──> AccountRepository <── InMemoryAccountRepository
       │
       └─────────> TransactionRepository <── InMemoryTransactionRepository
                              ▲
                     RepositoryFactory
                              ▲
                    persistence.properties
```

## Repository contracts

`AccountRepository` provides save, lookup, listing, update, delete, existence checks, and account number allocation. `TransactionRepository` provides save, lookup by account, listing, and clear. The service only uses these interfaces, so a future JDBC or file implementation can replace the in-memory classes without changing service logic.

`InMemoryAccountRepository` stores accounts in a `Map<Integer, IAccount>` and starts account numbers at 1001. `InMemoryTransactionRepository` stores transactions in a list. Account filtering includes the account field and both ends of transfers.

## Configuration and factory

The default mode is `memory`:

```properties
persistence.mode=memory
```

`RepositoryFactory` reads `/config/persistence.properties` from the classpath. `jdbc` and `file` remain extension points; selecting an unsupported mode reports an error until those implementations are added. The service also accepts repositories through its constructor, which makes alternate implementations straightforward to wire.

## Compile and run

### Windows PowerShell

```powershell
New-Item bin -ItemType Directory -Force | Out-Null
Copy-Item src\main\resources\config bin -Recurse -Force
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse -Filter *.java src).FullName
java -cp bin com.gdb.tests.TestRepositoryInMemory
```

### Linux and macOS

```bash
mkdir -p bin && cp -r src/main/resources/config bin/
javac -encoding UTF-8 -d bin $(find src -name '*.java')
java -cp bin com.gdb.tests.TestRepositoryInMemory
```

## Exercises

1. Implement a file-backed repository and select it through the factory.
2. Add a JDBC implementation while keeping `AccountService` unchanged.
3. Decide how repositories should handle duplicate saves and updates for missing IDs.
4. Add pagination or sorting to the repository contracts and compare the impact on implementations.
