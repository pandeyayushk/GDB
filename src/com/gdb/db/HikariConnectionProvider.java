package com.gdb.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.gdb.exceptions.DataAccessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/** ConnectionProvider adapter backed by a reusable HikariCP connection pool. */
public final class HikariConnectionProvider implements ConnectionProvider {
    private final HikariDataSource dataSource;

    public HikariConnectionProvider(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        String jdbcUrl = properties.getProperty("persistence.db.url", "jdbc:sqlite:gdb.db").trim();
        String driver = properties.getProperty("persistence.db.driver", "org.sqlite.JDBC").trim();
        int poolSize = positiveInt(properties, "persistence.pool.size", 10);
        long timeout = positiveLong(properties, "persistence.pool.timeout", 30000L);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (!driver.isEmpty()) config.setDriverClassName(driver);
        config.setMaximumPoolSize(poolSize);
        config.setConnectionTimeout(Math.max(250L, timeout));
        config.setIdleTimeout(positiveLong(properties, "persistence.pool.idleTimeout", 600000L));
        config.setPoolName(properties.getProperty("persistence.pool.name", "GDB-Hikari").trim());
        try {
            dataSource = new HikariDataSource(config);
        } catch (RuntimeException e) {
            throw new DataAccessException("Unable to initialize HikariCP connection pool", e);
        }
    }

    @Override public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override public void shutdown() {
        if (!dataSource.isClosed()) dataSource.close();
    }

    @Override public String getProviderName() {
        return "HikariCP (" + dataSource.getPoolName() + ", max=" + dataSource.getMaximumPoolSize() + ")";
    }

    public HikariDataSource getDataSource() { return dataSource; }

    private static int positiveInt(Properties properties, String key, int fallback) {
        long value = positiveLong(properties, key, fallback);
        if (value > Integer.MAX_VALUE) throw new IllegalArgumentException(key + " exceeds integer range");
        return (int)value;
    }

    private static long positiveLong(Properties properties, String key, long fallback) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try {
            long value = Long.parseLong(raw.trim());
            if (value <= 0) throw new IllegalArgumentException(key + " must be positive");
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value for " + key + ": " + raw, e);
        }
    }
}
