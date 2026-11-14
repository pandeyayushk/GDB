# Activity 26: File Repositories, Atomic CSV Writes, and Backups

## Goal

Use CSV files for account and transaction persistence, atomically replace each file to prevent partial writes, and retain timestamped backups before changes. File mode is selected with `persistence.mode=file`; paths are set by `persistence.file.account.path` and `persistence.file.transaction.path`.

## Implemented behavior

- `CsvMapper` supports quoted commas and quotes and maps this project’s account metadata, including tenure and optional PIN. It also accepts the earlier account CSV column layout from the archive.
- `AtomicFileWriter` writes a sibling temporary file and swaps it into place, falling back when atomic moves are unavailable.
- `BackupManager` creates timestamped backups and prunes older copies to the configured retention limit.
- `FileAccountRepository` and `FileTransactionRepository` implement the repository contracts with synchronized caches and atomic persistence. File transfers update balances and add an audit entry, restoring account snapshots if the ledger write fails.
- `RepositoryFactory` selects CSV repositories when `persistence.mode=file`.

## Compile and run

### Windows PowerShell

```powershell
Copy-Item src\main\resources\* bin -Recurse -Force
javac -encoding UTF-8 -cp 'lib\*;.' -d bin (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
java -ea -cp 'lib\*;bin' com.gdb.tests.TestFileRepository
```

### Linux and macOS

```bash
mkdir -p bin && cp -r src/main/resources/* bin/
javac -encoding UTF-8 -cp 'lib/*:.' -d bin $(find src -name '*.java')
java -ea -cp 'lib/*:bin' com.gdb.tests.TestFileRepository
```

The test suite uses temporary files and checks CSV round trips, CRUD, atomic writes, backups, transfer audit logging, and rollback on insufficient funds.
