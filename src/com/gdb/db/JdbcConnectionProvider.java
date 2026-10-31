package com.gdb.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/** DriverManager-backed provider. SQLite connections are opened per request. */
public class JdbcConnectionProvider implements ConnectionProvider {
    private final String url;
    private boolean shutdown;

    public JdbcConnectionProvider(String url, String driverClass) {
        this.url = Objects.requireNonNull(url, "url");
        if (driverClass != null && !driverClass.trim().isEmpty()) {
            try {
                Class.forName(driverClass.trim());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("JDBC driver not found: " + driverClass
                        + ". Add the driver jar to the classpath.", e);
            }
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (shutdown) throw new SQLException("Connection provider has been shut down");
        return DriverManager.getConnection(url);
    }

    /** No pooled resources are held by this provider. */
    public synchronized void shutdown() { shutdown = true; }
    public String getProviderName() { return "JDBC (" + url + ")"; }
}
