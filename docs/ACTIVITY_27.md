# Activity 27: Concurrency and Thread Safety

## Goal

Reproduce lost updates in an intentionally unsynchronized read/modify/write example, then protect banking operations with a synchronized service layer. The concurrency test also checks transfers conserve funds and withdrawals respect the account’s minimum balance under contention.

## Implemented behavior

- `ConcurrentDepositDemo.runUnsafeDemo` coordinates concurrent reads and writes to demonstrate deterministic lost updates; `runSafeDemo` routes all deposits through the synchronized service.
- `ThreadSafeAccountService` synchronizes account creation, reads, deposits, withdrawals, and transfers, and writes transaction records after successful operations.
- `TestConcurrency` checks the unsafe demonstration, 1,000 concurrent deposits, reciprocal transfers, and competing withdrawals.

## Compile and run

### Windows PowerShell

```powershell
Copy-Item src\main\resources\* bin -Recurse -Force
javac -encoding UTF-8 -cp 'lib\*;.' -d bin (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
java -ea -cp 'lib\*;bin' com.gdb.tests.TestConcurrency
```

### Linux and macOS

```bash
mkdir -p bin && cp -r src/main/resources/* bin/
javac -encoding UTF-8 -cp 'lib/*:.' -d bin $(find src -name '*.java')
java -ea -cp 'lib/*:bin' com.gdb.tests.TestConcurrency
```
