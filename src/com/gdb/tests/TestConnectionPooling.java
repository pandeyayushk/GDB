package com.gdb.tests;

import com.gdb.db.ConnectionProvider;
import com.gdb.db.HikariConnectionProvider;
import com.gdb.db.JdbcConnectionProvider;
import com.gdb.db.SchemaInitializer;
import com.gdb.domain.AccountFactory;
import com.gdb.domain.IAccount;
import com.gdb.repository.JdbcAccountRepository;
import com.gdb.repository.RepositoryFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Functional and lightweight pooled-connection checks for Activity 25. */
public final class TestConnectionPooling {
    public static void main(String[] args) throws Exception {
        Path db = Files.createTempFile("gdb-activity25-", ".db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        Properties properties = new Properties();
        properties.setProperty("persistence.db.url", url);
        properties.setProperty("persistence.db.driver", "org.sqlite.JDBC");
        properties.setProperty("persistence.pool.size", "5");
        properties.setProperty("persistence.pool.timeout", "10000");
        properties.setProperty("persistence.pool.idleTimeout", "600000");

        HikariConnectionProvider pooled = new HikariConnectionProvider(properties);
        JdbcConnectionProvider direct = new JdbcConnectionProvider(url, "org.sqlite.JDBC");
        try {
            check(pooled.getDataSource().getMaximumPoolSize() == 5, "configured pool size");
            SchemaInitializer.initialize(pooled);
            try (Connection c = pooled.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT 1")) {
                check(rs.next() && rs.getInt(1) == 1, "borrowed connection works");
            }

            ExecutorService workers = Executors.newFixedThreadPool(8);
            try {
                @SuppressWarnings("unchecked") Future<Integer>[] results = new Future[24];
                for (int i = 0; i < results.length; i++) results[i] = workers.submit(() -> {
                    try (Connection c = pooled.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT 42")) {
                        if (!rs.next()) throw new AssertionError("No query result");
                        return rs.getInt(1);
                    }
                });
                for (Future<Integer> result : results) check(result.get() == 42, "concurrent pooled query");
            } finally { workers.shutdownNow(); }

            JdbcAccountRepository repository = new JdbcAccountRepository(pooled);
            IAccount account = AccountFactory.createAccount("CURRENT", 25001, "Pool Test", 35, 50000, 2);
            repository.save(account);
            check(repository.findById(25001).getBalance() == 50000, "repository operation over HikariCP");

            long pooledTime = benchmark(pooled, 50);
            long directTime = benchmark(direct, 50);
            System.out.println("Pooled 50 connection cycles: " + pooledTime + " ms");
            System.out.println("Direct 50 connection cycles: " + directTime + " ms");
            if ("jdbc".equals(RepositoryFactory.getPersistenceMode())) {
                ConnectionProvider configuredProvider = RepositoryFactory.getConnectionProvider();
                check(configuredProvider instanceof HikariConnectionProvider,
                        "factory selects Hikari when pooling is enabled");
                configuredProvider.shutdown();
            } else if ("file".equals(RepositoryFactory.getPersistenceMode())) {
                check(RepositoryFactory.getAccountRepository() instanceof com.gdb.repository.FileAccountRepository,
                        "factory keeps the configured file persistence mode");
            }
            System.out.println("All Activity 25 connection pooling checks passed.");
        } finally {
            pooled.shutdown();
            direct.shutdown();
            Files.deleteIfExists(db);
            Files.deleteIfExists(Path.of(db + "-wal"));
            Files.deleteIfExists(Path.of(db + "-shm"));
        }
    }

    private static long benchmark(ConnectionProvider provider, int iterations) throws Exception {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (Connection c = provider.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT 1")) {
                if (!rs.next()) throw new AssertionError("No benchmark result");
            }
        }
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("Failed: " + message);
    }
}
