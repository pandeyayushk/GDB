package com.gdb.tests;

import com.gdb.db.ConnectionProvider;
import com.gdb.db.DataMigrator;
import com.gdb.db.JdbcConnectionProvider;
import com.gdb.db.SchemaInitializer;
import com.gdb.command.DepositCommand;
import com.gdb.domain.AccountFactory;
import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransactionType;
import com.gdb.repository.AccountRepository;
import com.gdb.repository.InMemoryAccountRepository;
import com.gdb.repository.InMemoryTransactionRepository;
import com.gdb.repository.JdbcAccountRepository;
import com.gdb.repository.JdbcTransactionRepository;
import com.gdb.repository.TransactionRepository;
import com.gdb.logging.MemoryLogDestination;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

/** Integration checks for the Activity 24 transaction repository and migration. */
public final class TestJdbcTransactionRepository {
    public static void main(String[] args) throws Exception {
        Path db = Files.createTempFile("gdb-activity24-", ".db");
        ConnectionProvider provider = new JdbcConnectionProvider("jdbc:sqlite:" + db.toAbsolutePath(), "org.sqlite.JDBC");
        try {
            SchemaInitializer.initialize(provider);
            JdbcAccountRepository accounts = new JdbcAccountRepository(provider);
            JdbcTransactionRepository transactions = new JdbcTransactionRepository(provider);
            IAccount source = account(1001, "Source", 50000);
            IAccount target = account(1002, "Target", 30000);
            accounts.save(source); accounts.save(target);

            Transaction deposit = transaction("TXN-A24-1", 1001, TransactionType.DEPOSIT, 100, 50100, 0, 0);
            Transaction transfer = transaction("TXN-A24-2", 1001, TransactionType.TRANSFER, 50, 50050, 1001, 1002);
            transactions.save(deposit); transactions.save(transfer);
            check(transactions.findByAccount(1002).size() == 1, "account query includes to_account transfers");
            check(transactions.findAll().size() == 2, "find all transactions");
            check(transactions.findByAccount(1001).get(0).getType() == TransactionType.DEPOSIT, "transaction row mapping");

            Transaction receipt = transactions.transfer(1001, 1002, 10000);
            check(receipt.getType() == TransactionType.TRANSFER, "transfer audit type");
            check(accounts.findById(1001).getBalance() == 40000, "transfer debit");
            check(accounts.findById(1002).getBalance() == 40000, "transfer credit");
            check(transactions.findByAccount(1002).size() == 2, "transfer audit visible to destination account");

            double sourceBefore = accounts.findById(1001).getBalance();
            double targetBefore = accounts.findById(1002).getBalance();
            int logCountBefore = transactions.findAll().size();
            expectFailure(() -> transactions.transfer(1001, 1002, 999999), "insufficient funds rejected");
            check(accounts.findById(1001).getBalance() == sourceBefore, "insufficient funds preserves source balance");
            check(accounts.findById(1002).getBalance() == targetBefore, "insufficient funds preserves destination balance");
            check(transactions.findAll().size() == logCountBefore, "failed transfer has no audit row");

            try (Connection c = provider.getConnection(); Statement s = c.createStatement()) {
                s.execute("CREATE TRIGGER fail_credit BEFORE UPDATE OF balance ON accounts WHEN OLD.account_number=1002 BEGIN SELECT RAISE(ABORT, 'simulated credit failure'); END");
            }
            expectFailure(() -> transactions.transfer(1001, 1002, 100), "database credit failure rolls back");
            check(accounts.findById(1001).getBalance() == sourceBefore, "SQL failure rolls back debit");
            check(accounts.findById(1002).getBalance() == targetBefore, "SQL failure preserves destination");
            check(transactions.findAll().size() == logCountBefore, "SQL failure rolls back audit insert");
            try (Connection c = provider.getConnection(); Statement s = c.createStatement()) { s.execute("DROP TRIGGER fail_credit"); }

            InMemoryAccountRepository legacyAccounts = new InMemoryAccountRepository();
            InMemoryTransactionRepository legacyTransactions = new InMemoryTransactionRepository();
            for (int i = 0; i < 3; i++) legacyAccounts.save(account(1101 + i, "Legacy " + i, 10000 + i));
            for (int i = 0; i < 5; i++) legacyTransactions.save(transaction("TXN-MIG-" + i, 1101 + (i % 3),
                    TransactionType.DEPOSIT, i + 1, 1001 + i, 0, 0));
            DataMigrator.MigrationSummary summary = DataMigrator.migrate(legacyAccounts, accounts, legacyTransactions, transactions);
            check(summary.getAccountsMigrated() == 3 && summary.getTransactionsMigrated() == 5, "migration counts");
            check(accounts.findById(1103) != null, "migrated account exists");
            check(transactions.findByAccount(1101).size() == 2, "migrated transactions exist");

            MemoryLogDestination legacyLog = new MemoryLogDestination();
            DepositCommand legacyCommand = new DepositCommand(accounts.findById(1001), 25);
            legacyCommand.execute();
            legacyLog.write(legacyCommand);
            check(DataMigrator.migrate(legacyLog, transactions) == 1, "legacy command log migration count");
            check(transactions.findByAccount(1001).size() == 4, "legacy command log transaction migrated");

            transactions.clear();
            check(transactions.findAll().isEmpty(), "clear transaction rows");
            System.out.println("All JDBC transaction repository and migration checks passed.");
        } finally {
            provider.shutdown();
            Files.deleteIfExists(db);
            Files.deleteIfExists(Path.of(db + "-wal"));
            Files.deleteIfExists(Path.of(db + "-shm"));
        }
    }

    private static IAccount account(int number, String name, double balance) throws Exception {
        return AccountFactory.createAccount("SAVINGS", number, name, 30, balance, 2);
    }
    private static Transaction transaction(String id, int account, TransactionType type, double amount,
                                           double balance, int from, int to) {
        return new Transaction(id, LocalDateTime.now(), account, type, amount, balance, "SUCCESS", "Activity 24 test", from, to);
    }
    private static void expectFailure(Runnable action, String label) {
        try { action.run(); throw new AssertionError("Expected failure: " + label); }
        catch (RuntimeException expected) { }
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError("Failed: " + label);
    }
}
