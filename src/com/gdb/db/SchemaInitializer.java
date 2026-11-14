package com.gdb.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/** Executes the idempotent DDL script from the application classpath. */
public final class SchemaInitializer {
    private static final String SCHEMA = "/schema.sql";
    private SchemaInitializer() { }

    public static void initialize(ConnectionProvider provider) throws SQLException {
        String script;
        try (InputStream input = SchemaInitializer.class.getResourceAsStream(SCHEMA)) {
            if (input == null) throw new IllegalStateException("Missing classpath resource " + SCHEMA);
            StringBuilder contents = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) contents.append(line).append('\n');
            }
            script = contents.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + SCHEMA, e);
        }

        try (Connection connection = provider.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                String command = sql.trim();
                if (!command.isEmpty()) statement.execute(command);
            }
            // Upgrade databases created before account PIN and tenure were persisted.
            addColumnIfMissing(connection, "tenure_years", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(connection, "pin", "INTEGER");
        }
    }

    private static void addColumnIfMissing(Connection connection, String column, String declaration) throws SQLException {
        boolean found = false;
        try (Statement statement = connection.createStatement(); ResultSet columns = statement.executeQuery("PRAGMA table_info(accounts)")) {
            while (columns.next()) if (column.equalsIgnoreCase(columns.getString("name"))) found = true;
        }
        if (!found) try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE accounts ADD COLUMN " + column + " " + declaration);
        }
    }
}
