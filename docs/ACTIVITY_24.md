# Activity 24: JDBC Transaction Repository and Data Migration

## Goal

Persist transaction history in SQLite, make transfers atomic across account balances and their audit record, and migrate account and transaction data between repository backends. `JdbcTransactionRepository` uses the existing schema and translates SQL failures to `DataAccessException`.

## Implemented behavior

- `save`, `findByAccount`, `findAll`, and `clear` manage the transaction ledger using prepared statements.
- `transfer` validates active source and destination accounts, checks available funds, updates both balances, and inserts a transfer audit row on one connection. Any failure rolls back the complete operation.
- `DataMigrator.migrate` supports both account/transaction repository migration and legacy `LogDestination` command-log migration.
- `RepositoryFactory` supplies JDBC transaction storage when `persistence.mode=jdbc`.

## Compile and run

### Windows PowerShell

```powershell
Copy-Item src\main\resources\* bin -Recurse -Force
javac -encoding UTF-8 -cp 'lib\*;.' -d bin (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
java -ea -cp 'lib\*;bin' com.gdb.tests.TestJdbcTransactionRepository
```

### Linux and macOS

```bash
mkdir -p bin && cp -r src/main/resources/* bin/
javac -encoding UTF-8 -cp 'lib/*:.' -d bin $(find src -name '*.java')
java -ea -cp 'lib/*:bin' com.gdb.tests.TestJdbcTransactionRepository
```

The test uses an isolated temporary SQLite database and checks persistence, account queries, transfer commit and rollback, and repository migration.
