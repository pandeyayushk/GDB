package com.gdb.repository;

import com.gdb.db.ConnectionProvider;
import com.gdb.db.HikariConnectionProvider;
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
    private static final AccountRepository ACCOUNTS = createConfiguredAccountRepository();
    private static final TransactionRepository TRANSACTIONS = createConfiguredTransactionRepository();

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
        if ("memory".equals(MODE) || "file".equals(MODE)) return null;
        if (!"jdbc".equals(MODE)) {
            throw new IllegalArgumentException("Unsupported persistence.mode: " + MODE + " (file is a placeholder)");
        }
        String url = PROPERTIES.getProperty("persistence.db.url", "jdbc:sqlite:gdb.db");
        String driver = PROPERTIES.getProperty("persistence.db.driver", "org.sqlite.JDBC");
        ConnectionProvider provider;
        if (Boolean.parseBoolean(PROPERTIES.getProperty("persistence.pool.enabled", "false"))) {
            provider = new HikariConnectionProvider(PROPERTIES);
        } else {
            provider = new JdbcConnectionProvider(url, driver);
        }
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

    private static AccountRepository createConfiguredAccountRepository() {
        if ("jdbc".equals(MODE)) return new JdbcAccountRepository(getConnectionProvider());
        if ("file".equals(MODE)) return new FileAccountRepository(accountFilePath());
        return new InMemoryAccountRepository();
    }

    private static TransactionRepository createConfiguredTransactionRepository() {
        if ("jdbc".equals(MODE)) return new JdbcTransactionRepository(getConnectionProvider());
        if ("file".equals(MODE)) {
            return new FileTransactionRepository(transactionFilePath(), ACCOUNTS);
        }
        return new InMemoryTransactionRepository();
    }

    public static AccountRepository createAccountRepository(String mode) {
        if (mode == null || "memory".equalsIgnoreCase(mode.trim())) return new InMemoryAccountRepository();
        if ("jdbc".equalsIgnoreCase(mode.trim())) return new JdbcAccountRepository(getConnectionProvider());
        if ("file".equalsIgnoreCase(mode.trim())) return new FileAccountRepository(accountFilePath());
        throw new IllegalArgumentException("Unsupported persistence.mode: " + mode);
    }
    public static TransactionRepository createTransactionRepository(String mode) {
        if (mode == null || "memory".equalsIgnoreCase(mode.trim())) return new InMemoryTransactionRepository();
        if ("jdbc".equalsIgnoreCase(mode.trim())) return new JdbcTransactionRepository(getConnectionProvider());
        if ("file".equalsIgnoreCase(mode.trim())) {
            AccountRepository accounts = "file".equals(MODE) ? ACCOUNTS : new FileAccountRepository(accountFilePath());
            return new FileTransactionRepository(transactionFilePath(), accounts);
        }
        throw new IllegalArgumentException("Unsupported persistence.mode: " + mode);
    }

    private static String accountFilePath() {
        return PROPERTIES.getProperty("persistence.file.account.path",
                PROPERTIES.getProperty("persistence.file.accounts", "data/accounts.csv"));
    }
    private static String transactionFilePath() {
        return PROPERTIES.getProperty("persistence.file.transaction.path",
                PROPERTIES.getProperty("persistence.file.transactions", "data/transactions.csv"));
    }
}
