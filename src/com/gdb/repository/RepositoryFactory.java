package com.gdb.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Creates repository implementations using config/persistence.properties. */
public final class RepositoryFactory {
    private static final String CONFIG = "/config/persistence.properties";
    private static final String MODE = loadMode();
    private static final AccountRepository ACCOUNTS = createAccountRepository();
    private static final TransactionRepository TRANSACTIONS = createTransactionRepository();

    private RepositoryFactory() { }
    private static String loadMode() {
        Properties properties = new Properties();
        try (InputStream input = RepositoryFactory.class.getResourceAsStream(CONFIG)) {
            if (input != null) properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + CONFIG, e);
        }
        return properties.getProperty("persistence.mode", "memory").trim().toLowerCase();
    }
    private static AccountRepository createAccountRepository() {
        if ("memory".equals(MODE)) return new InMemoryAccountRepository();
        throw new IllegalArgumentException("Unsupported persistence.mode: " + MODE + " (jdbc and file are placeholders)");
    }
    private static TransactionRepository createTransactionRepository() {
        if ("memory".equals(MODE)) return new InMemoryTransactionRepository();
        throw new IllegalArgumentException("Unsupported persistence.mode: " + MODE + " (jdbc and file are placeholders)");
    }
    public static AccountRepository getAccountRepository() { return ACCOUNTS; }
    public static TransactionRepository getTransactionRepository() { return TRANSACTIONS; }
    public static AccountRepository createAccountRepository(String mode) {
        if (mode == null || "memory".equalsIgnoreCase(mode.trim())) return new InMemoryAccountRepository();
        throw new IllegalArgumentException("Unsupported persistence.mode: " + mode);
    }
    public static TransactionRepository createTransactionRepository(String mode) {
        if (mode == null || "memory".equalsIgnoreCase(mode.trim())) return new InMemoryTransactionRepository();
        throw new IllegalArgumentException("Unsupported persistence.mode: " + mode);
    }
}
