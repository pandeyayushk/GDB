package com.gdb.tests;

import com.gdb.db.JdbcConnectionProvider;
import com.gdb.db.SchemaInitializer;
import com.gdb.domain.AccountFactory;
import com.gdb.domain.IAccount;
import com.gdb.repository.AccountRepository;
import com.gdb.repository.JdbcAccountRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

/** CRUD, mapping, transfer commit, and rollback checks against an isolated SQLite file. */
public final class TestJdbcAccountRepository {
    public static void main(String[] args) throws Exception {
        Path db = Files.createTempFile("gdb-activity23-", ".db");
        JdbcConnectionProvider provider = new JdbcConnectionProvider("jdbc:sqlite:" + db.toAbsolutePath(), "org.sqlite.JDBC");
        try {
            SchemaInitializer.initialize(provider);
            AccountRepository repository = new JdbcAccountRepository(provider);
            IAccount source = AccountFactory.createAccount("SAVINGS", 923001, "Source", 35, 50000, 3);
            source.setPin(1234);
            IAccount target = AccountFactory.createAccount("CURRENT", 923002, "Target", 40, 25000, 1);
            repository.save(source); repository.save(target);
            check(repository.exists(923001), "exists after insert");
            IAccount loaded = repository.findById(923001);
            check(loaded != null && loaded.getAccountType().equals("Savings"), "account type mapping");
            check(loaded.getBalance() == 50000 && loaded.getTenureYears() == 3 && loaded.verifyPin(1234), "persisted fields mapping");
            check(repository.findAll().size() == 2, "find all");
            loaded.setTenureYears(4); loaded.deposit(100); repository.update(loaded);
            check(repository.findById(923001).getBalance() == 50100, "update");
            check(repository.nextAccountNumber() == 923003, "next account number");

            repository.transfer(923001, 923002, 500, 1234);
            check(repository.findById(923001).getBalance() == 49600, "transfer debit");
            check(repository.findById(923002).getBalance() == 25500, "transfer credit");
            try (Connection c = provider.getConnection(); Statement s = c.createStatement()) {
                s.execute("CREATE TRIGGER fail_target_credit BEFORE UPDATE OF balance ON accounts " +
                        "WHEN OLD.account_number=923002 BEGIN SELECT RAISE(ABORT, 'simulated credit failure'); END");
            }
            double sourceBeforeFailure = repository.findById(923001).getBalance();
            expectFailure(() -> repository.transfer(923001, 923002, 100, 1234), "credit failure");
            check(repository.findById(923001).getBalance() == sourceBeforeFailure, "SQL failure rolls back debit");
            try (Connection c = provider.getConnection(); Statement s = c.createStatement()) { s.execute("DROP TRIGGER fail_target_credit"); }
            IAccount inactiveTarget = repository.findById(923002);
            inactiveTarget.closeAccount(); repository.update(inactiveTarget);
            double before = repository.findById(923001).getBalance();
            expectFailure(() -> repository.transfer(923001, 923002, 100, 1234), "inactive destination rejected");
            check(repository.findById(923001).getBalance() == before, "rollback preserves source balance");
            expectFailure(() -> repository.transfer(923001, 923001, 100, 1234), "same account rejected");

            repository.delete(923002); repository.delete(923001);
            check(!repository.exists(923001), "delete");
            System.out.println("All JDBC account repository checks passed.");
        } finally {
            provider.shutdown();
            Files.deleteIfExists(db);
            Files.deleteIfExists(Path.of(db.toString() + "-wal"));
            Files.deleteIfExists(Path.of(db.toString() + "-shm"));
        }
    }

    private static void expectFailure(Runnable action, String label) {
        try { action.run(); throw new AssertionError("Expected failure: " + label); }
        catch (RuntimeException expected) { }
    }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError("Failed: " + label); }
}
