package com.gdb.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
        }
    }
}
