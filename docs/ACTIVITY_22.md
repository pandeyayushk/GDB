# Activity 22: JDBC Foundation

## Goal

Add a JDBC connection boundary and deploy the relational schema without tying application code to SQLite APIs. `JdbcConnectionProvider` uses `DriverManager`; `SchemaInitializer` loads classpath DDL and safely re-runs `CREATE ... IF NOT EXISTS` statements.

```text
Application ──> ConnectionProvider ──> DriverManager ──> SQLite
                         │
                         └── SchemaInitializer ──> schema.sql
```

## Runtime dependencies

Place `sqlite-jdbc-3.44.1.0.jar` and the SLF4J API/runtime jars in `lib/`. The Java sources use only `java.sql`, so they compile without the driver, but JDBC execution requires the jars on the runtime classpath.

## Configuration

`src/main/resources/config/persistence.properties` selects JDBC and supplies the URL and driver class:

```properties
persistence.mode=jdbc
persistence.db.url=jdbc:sqlite:gdb.db
persistence.db.driver=org.sqlite.JDBC
```

`RepositoryFactory.getConnectionProvider()` initializes the provider and deploys the schema. Account and transaction repositories remain in-memory in this activity; later work can implement their JDBC versions behind the same interfaces.

## Schema

`schema.sql` creates `accounts` and `transactions` with primary keys, a transaction-to-account foreign key, and an account lookup index. Every DDL statement uses `IF NOT EXISTS`, so `SchemaInitializer.initialize` is idempotent.

## Compile and run

### Windows PowerShell

```powershell
New-Item bin -ItemType Directory -Force | Out-Null
Copy-Item src\main\resources\* bin -Recurse -Force
javac -encoding UTF-8 -cp "lib\*;." -d bin (Get-ChildItem -Recurse -Filter *.java src).FullName
java -cp "lib\*;bin" com.gdb.tests.TestJdbcConnection
```

### Linux and macOS

```bash
mkdir -p bin && cp -r src/main/resources/* bin/
javac -encoding UTF-8 -cp 'lib/*:.' -d bin $(find src -name '*.java')
java -cp 'lib/*:bin' com.gdb.tests.TestJdbcConnection
```

## Exercises

1. Try a different SQLite URL and describe where the database file is created.
2. Add a schema version table and a migration strategy.
3. Add indexes based on likely transaction history queries.
4. Compare this provider with a pooled provider while keeping the interface unchanged.
