package com.gdb.repository;

import com.gdb.db.ConnectionProvider;
import com.gdb.db.JdbcConnectionProvider;
import com.gdb.db.SchemaInitializer;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;

/** Creates repositories and initializes configured database infrastructure. */
public final class RepositoryFactory {
    private static final String CONFIG = "/config/persistence.properties";
    private static final Properties PROPERTIES = loadProperties();
    private static final String MODE = PROPERTIES.getProperty("persistence.mode", "memory").trim().toLowerCase(Locale.ROOT);
    private static final AccountRepository ACCOUNTS = new InMemoryAccountRepository();
    private static final TransactionRepository TRANSACTIONS = new InMemoryTransactionRepository();

    private RepositoryFactory() { }

    private static final class JdbcProviderHolder {
        private static final ConnectionProvider INSTANCE = createConnectionProvider();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = RepositoryFactory.class.getResourceAsStream(CONFIG)) {
            if (input != null) properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + CONFIG, e);
        }
        return properties;
    }

    private static ConnectionProvider createConnectionProvider() {
        if ("memory".equals(MODE)) return null;
        if (!"jdbc".equals(MODE)) {
            throw new IllegalArgumentException("Unsupported persistence.mode: " + MODE + " (file is a placeholder)");
        }
        String url = PROPERTIES.getProperty("persistence.db.url", "jdbc:sqlite:gdb.db");
        String driver = PROPERTIES.getProperty("persistence.db.driver", "org.sqlite.JDBC");
        JdbcConnectionProvider provider = new JdbcConnectionProvider(url, driver);
        try {
            SchemaInitializer.initialize(provider);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize JDBC schema", e);
        }
        return provider;
    }

    public static AccountRepository getAccountRepository() { return ACCOUNTS; }
    public static TransactionRepository getTransactionRepository() { return TRANSACTIONS; }
    public static ConnectionProvider getConnectionProvider() { return JdbcProviderHolder.INSTANCE; }
    public static String getPersistenceMode() { return MODE; }

    /** Repository implementations remain in-memory until the JDBC repository activity. */
    public static AccountRepository createAccountRepository(String mode) {
        if (mode == null || "memory".equalsIgnoreCase(mode.trim()) || "jdbc".equalsIgnoreCase(mode.trim()))
            return new InMemoryAccountRepository();
        throw new IllegalArgumentException("Unsupported persistence.mode: " + mode);
    }
    public static TransactionRepository createTransactionRepository(String mode) {
        if (mode == null || "memory".equalsIgnoreCase(mode.trim()) || "jdbc".equalsIgnoreCase(mode.trim()))
            return new InMemoryTransactionRepository();
        throw new IllegalArgumentException("Unsupported persistence.mode: " + mode);
    }
}
