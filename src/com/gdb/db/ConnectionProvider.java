package com.gdb.db;

import java.sql.Connection;
import java.sql.SQLException;

/** Supplies database connections without coupling clients to a driver. */
public interface ConnectionProvider extends AutoCloseable {
    Connection getConnection() throws SQLException;
    void shutdown() throws SQLException;
    String getProviderName();
    @Override default void close() throws SQLException { shutdown(); }
}
