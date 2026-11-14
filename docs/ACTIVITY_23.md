# Activity 23: JDBC Account Repository

## Goal

Implement relational account persistence with JDBC `PreparedStatement`s and make a transfer an all-or-nothing database operation. `JdbcAccountRepository` maps SQLite rows to the existing account subclasses, supports CRUD, and wraps SQL failures in unchecked `DataAccessException`.

```text
AccountRepository
      ├── InMemoryAccountRepository
      └── JdbcAccountRepository ──> ConnectionProvider ──> SQLite
                                         │
                                         └── transaction: read → validate → debit → credit → commit
```

## Account mapping

The `accounts` table stores the account number, type, holder, age, balance, active state, opening date, tenure, and optional PIN. The schema initializer adds `tenure_years` and `pin` to existing databases when those columns are absent. Account types are reconstructed through `AccountFactory`, preserving each type's rules and behavior.

Every query that includes caller supplied values uses a prepared statement. A missing row returns `null`; `exists` returns `false`; `nextAccountNumber` returns one greater than the maximum number, starting at 1001.

## Atomic transfer

`transfer(fromAccountNumber, toAccountNumber, amount, pin)` rejects nonpositive or non-finite amounts, identical/missing accounts, inactive accounts, incorrect PINs, and debits disallowed by the account's withdrawal rules. Both balance updates run with auto-commit disabled. The repository commits only when the debit and credit each update one active row. Any validation or SQL error rolls the transaction back.

This transfer API runs at the repository boundary and does not write transaction history or apply the service's daily transfer limit. Those concerns remain in the service layer.

## Factory configuration

With `persistence.mode=jdbc`, `RepositoryFactory.getAccountRepository()` returns `JdbcAccountRepository`. With `persistence.mode=memory`, it returns `InMemoryAccountRepository`. The configured JDBC provider initializes the schema before the repository is returned.

## Compile and run

### Windows PowerShell

```powershell
New-Item bin -ItemType Directory -Force | Out-Null
Copy-Item src\main\resources\* bin -Recurse -Force
javac -encoding UTF-8 -cp "lib\*;." -d bin (Get-ChildItem -Recurse -Filter *.java src).FullName
java -cp "lib\*;bin" com.gdb.tests.TestJdbcAccountRepository
```

### Linux and macOS

```bash
mkdir -p bin && cp -r src/main/resources/* bin/
javac -encoding UTF-8 -cp 'lib/*:.' -d bin $(find src -name '*.java')
java -cp 'lib/*:bin' com.gdb.tests.TestJdbcAccountRepository
```

The test driver creates a temporary SQLite database and checks CRUD, row mapping, successful transfer, and rollback after a simulated credit failure.

## Exercises

1. Add pagination to `findAll` without changing the repository's row mapping.
2. Add transaction history writes to the same transfer transaction.
3. Consider how concurrent transfers should lock or conditionally update account rows.
