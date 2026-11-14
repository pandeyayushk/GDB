package com.gdb.db;

import com.gdb.command.TransactionCommand;
import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.logging.LogDestination;
import com.gdb.repository.AccountRepository;
import com.gdb.repository.TransactionRepository;
import java.util.Objects;

/** Copies accounts and transaction records from one persistence backend to another. */
public final class DataMigrator {
    private DataMigrator() { }

    /** Migrates command based logs used by the legacy file and memory loggers. */
    public static int migrate(LogDestination source, TransactionRepository destination) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        int count = 0;
        for (TransactionCommand command : source.readAll()) {
            if (command == null) continue;
            Transaction transaction = command.getTransaction();
            if (transaction != null) { destination.save(transaction); count++; }
        }
        return count;
    }

    /** Migrates both accounts and transaction records and returns the counts. */
    public static MigrationSummary migrate(AccountRepository sourceAccounts, AccountRepository destinationAccounts,
                                           TransactionRepository sourceTransactions, TransactionRepository destinationTransactions) {
        Objects.requireNonNull(sourceAccounts, "sourceAccounts");
        Objects.requireNonNull(destinationAccounts, "destinationAccounts");
        Objects.requireNonNull(sourceTransactions, "sourceTransactions");
        Objects.requireNonNull(destinationTransactions, "destinationTransactions");
        int accounts = 0;
        for (IAccount account : sourceAccounts.findAll()) {
            destinationAccounts.save(account);
            accounts++;
        }
        int transactions = 0;
        for (Transaction transaction : sourceTransactions.findAll()) {
            destinationTransactions.save(transaction);
            transactions++;
        }
        return new MigrationSummary(accounts, transactions);
    }

    public static final class MigrationSummary {
        private final int accountsMigrated;
        private final int transactionsMigrated;
        public MigrationSummary(int accountsMigrated, int transactionsMigrated) {
            this.accountsMigrated = accountsMigrated;
            this.transactionsMigrated = transactionsMigrated;
        }
        public int getAccountsMigrated() { return accountsMigrated; }
        public int getTransactionsMigrated() { return transactionsMigrated; }
        @Override public String toString() { return "Migrated " + accountsMigrated + " accounts and " + transactionsMigrated + " transactions."; }
    }
}
