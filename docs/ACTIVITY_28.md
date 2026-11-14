# Activity 28: Per-Account Locks, Atomics, and Concurrent Collections

## Goal

Replace a single coarse service lock with per-account locks, use a consistent account-number order for transfers, allow concurrent readers through read/write locks, and use CAS based counters and state transitions.

## Implemented behavior

- `AccountLockRegistry` reuses fair `ReentrantLock` and `ReentrantReadWriteLock` instances per account.
- `AccountLock` acquires its lock when created and releases it once when closed.
- `LockedAccountService` locks only the accounts involved in each operation. Transfers acquire both accounts in ascending number order to avoid deadlocks.
- `AtomicAccountService` assigns unique transaction sequence numbers, records deposit volume in cents, and changes system status with compare-and-set.
- `TestLocksAndAtomics` verifies same-account balance safety, independent account progress, reciprocal transfer completion, lock cleanup, and atomic values.

## Compile and run

### Windows PowerShell

```powershell
Copy-Item src\main\resources\* bin -Recurse -Force
javac -encoding UTF-8 -cp 'lib\*;.' -d bin (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
java -ea -cp 'lib\*;bin' com.gdb.tests.TestLocksAndAtomics
```

### Linux and macOS

```bash
mkdir -p bin && cp -r src/main/resources/* bin/
javac -encoding UTF-8 -cp 'lib/*:.' -d bin $(find src -name '*.java')
java -ea -cp 'lib/*:bin' com.gdb.tests.TestLocksAndAtomics
```
