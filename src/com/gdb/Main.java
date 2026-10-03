package com.gdb;

import com.gdb.domain.IAccount;
import com.gdb.logging.FileLogDestination;
import com.gdb.logging.LogDestination;
import com.gdb.logging.TransactionLogger;
import com.gdb.service.AccountService;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Global Digital Bank — Service Demo");
        System.out.println("=".repeat(60));

        // ============================================================
        // 📝 STEP 20: Wire Up Dependencies
        //
        // INSTRUCTIONS:
        //   1. LogDestination dest = new FileLogDestination();
        //   2. TransactionLogger logger = new TransactionLogger(dest);
        //   3. AccountService service = new AccountService(logger);
        // ============================================================

        LogDestination dest = new FileLogDestination();
        TransactionLogger logger = new TransactionLogger(dest);
        AccountService service = new AccountService(logger);

        // ============================================================
        // 📝 STEP 21: Run A Demo Workflow
        //
        // INSTRUCTIONS:
        //   1. Open accounts, set PINs, perform deposit, withdraw, transfer.
        //   2. Print balances and transaction history.
        // ============================================================

        IAccount acc1 = service.openAccount("SAVINGS", "Alice", 28, 25000);
        acc1.setPin(1111);
        IAccount acc2 = service.openAccount("CURRENT", "Bob", 35, 50000);
        acc2.setPin(2222);

        System.out.println("Opened: " + acc1.getAccountInfo());
        System.out.println("Opened: " + acc2.getAccountInfo());

        service.deposit(acc1.getAccountNumber(), 5000);
        service.withdraw(acc1.getAccountNumber(), 2000, 1111);
        service.transfer(acc1.getAccountNumber(), acc2.getAccountNumber(), 3000, 1111);

        System.out.println("\nBalances:");
        System.out.println("  " + acc1.getAccountHolderName() + ": Rs. " + acc1.getBalance());
        System.out.println("  " + acc2.getAccountHolderName() + ": Rs. " + acc2.getBalance());

        System.out.println("\nTransaction History (" + service.getTransactionHistory().size() + " records):");
        service.getTransactionHistory().forEach(cmd -> System.out.println("  " + cmd.getTransaction()));
    }
}
