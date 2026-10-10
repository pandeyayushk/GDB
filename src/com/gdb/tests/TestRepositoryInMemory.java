package com.gdb.tests;

import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.logging.MemoryLogDestination;
import com.gdb.logging.TransactionLogger;
import com.gdb.repository.*;
import com.gdb.service.AccountService;

/** Smoke test for repository CRUD, ID generation, transactions, and service wiring. */
public class TestRepositoryInMemory {
    public static void main(String[] args) throws Exception {
        AccountRepository accounts = new InMemoryAccountRepository();
        TransactionRepository transactions = new InMemoryTransactionRepository();
        AccountService service = new AccountService(
                new TransactionLogger(new MemoryLogDestination()), accounts, transactions);

        IAccount account = service.openAccount("SAVINGS", "Repository User", 25, 15000);
        account.setPin(1234);
        check(account.getAccountNumber() == 1001, "first account number");
        check(accounts.exists(1001), "saved account exists");
        check(accounts.findById(1001) == account, "find account");
        check(accounts.findAll().size() == 1, "list accounts");
        service.deposit(1001, 250);
        check(transactions.findAll().size() == 1, "transaction saved by service");
        check(transactions.findByAccount(1001).size() == 1, "find transaction by account");
        check(service.openAccount("CURRENT", "Second User", 30, 25000).getAccountNumber() == 1002,
                "incrementing account number");
        accounts.delete(1002);
        check(!accounts.exists(1002), "delete account");
        accounts.update(account);
        check(accounts.exists(1001), "update account");
        transactions.clear();
        check(transactions.findAll().isEmpty(), "clear transactions");
        check(RepositoryFactory.createAccountRepository("memory") instanceof InMemoryAccountRepository,
                "factory memory account repository");
        check(RepositoryFactory.createTransactionRepository("memory") instanceof InMemoryTransactionRepository,
                "factory memory transaction repository");
        System.out.println("All in-memory repository checks passed.");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError("Failed: " + label);
    }
}
