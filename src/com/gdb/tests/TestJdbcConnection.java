package com.gdb.tests;

import com.gdb.db.ConnectionProvider;
import com.gdb.db.JdbcConnectionProvider;
import com.gdb.db.SchemaInitializer;
import com.gdb.repository.RepositoryFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Verifies JDBC connection lifecycle and idempotent schema installation. */
public class TestJdbcConnection {
    public static void main(String[] args) throws Exception {
        ConnectionProvider provider = RepositoryFactory.getConnectionProvider();
        check(provider != null, "JDBC provider configured");
        check(provider.getProviderName().startsWith("JDBC"), "provider name");
        try (Connection connection = provider.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1")) {
            check(result.next() && result.getInt(1) == 1, "SELECT 1");
        }

        SchemaInitializer.initialize(provider);
        SchemaInitializer.initialize(provider);
        try (Connection connection = provider.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('accounts','transactions')")) {
            int count = 0;
            while (result.next()) count++;
            check(count == 2, "both tables exist");
        }
        provider.shutdown();
        try {
            provider.getConnection();
            throw new AssertionError("connection obtained after shutdown");
        } catch (java.sql.SQLException expected) {
            // expected lifecycle behavior
        }
        System.out.println("All JDBC connection checks passed.");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError("Failed: " + label);
    }
}
