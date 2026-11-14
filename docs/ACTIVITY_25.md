# Activity 25: JDBC Connection Pooling with HikariCP

## Goal

Reuse JDBC connections through HikariCP while keeping `ConnectionProvider` as the common adapter used by repositories. `persistence.pool.enabled` switches the factory between HikariCP pooling and direct `DriverManager` connections.

## Configuration

`src/main/resources/config/persistence.properties` enables pooling and sets a maximum pool size of 10, a 30 second connection timeout, and a 10 minute idle timeout. Adjust these values for the deployment environment. Call `ConnectionProvider.shutdown()` when the application is stopping to close the pool.

## Compile and run

### Windows PowerShell

```powershell
Copy-Item src\main\resources\* bin -Recurse -Force
javac -encoding UTF-8 -cp 'lib\*;.' -d bin (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
java -ea -cp 'lib\*;bin' com.gdb.tests.TestConnectionPooling
```

### Linux and macOS

```bash
mkdir -p bin && cp -r src/main/resources/* bin/
javac -encoding UTF-8 -cp 'lib/*:.' -d bin $(find src -name '*.java')
java -ea -cp 'lib/*:bin' com.gdb.tests.TestConnectionPooling
```

The test verifies pool configuration, connection reuse under concurrent borrowing, repository operations through the pool, and reports a small pooled versus direct acquisition benchmark. Timing is reported rather than asserted because local database and machine load affect results.
