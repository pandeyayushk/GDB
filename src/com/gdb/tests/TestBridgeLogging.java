package com.gdb.tests;

import com.gdb.command.*;
import com.gdb.db.SimulatedDatabase;
import com.gdb.domain.*;
import com.gdb.logging.*;
import java.util.List;

public class TestBridgeLogging {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  ACTIVITY 18 — BRIDGE PATTERN (FILE + DB)");
        System.out.println("=".repeat(60));

        // Setup test accounts
        IAccount acc1 = AccountFactory.createAccount("SAVINGS", 1001, "John", 25, 15000);
        acc1.setPin(1234);
        IAccount acc2 = AccountFactory.createAccount("SAVINGS", 1002, "Jane", 30, 10000);

        // ============================================================
        // 📝 STEP 24: Create Destinations
        //
        // INSTRUCTIONS:
        //   1. fileDest = new FileLogDestination(); fileDest.clear();
        //   2. db = new SimulatedDatabase();
        //   3. dbDest = new DatabaseLogDestination(db);
        //   4. memDest = new MemoryLogDestination();
        // ============================================================
        // TODO: instantiate all log destinations
        LogDestination fileDest = new FileLogDestination();
        fileDest.clear();
        SimulatedDatabase db = new SimulatedDatabase();
        LogDestination dbDest = new DatabaseLogDestination(db);
        LogDestination memDest = new MemoryLogDestination();

        // ============================================================
        // 📝 STEP 25: Create TransactionLogger with File Destination
        //
        // INSTRUCTIONS:
        //   1. logger = new TransactionLogger(fileDest);
        //   2. Execute and log 3 commands (Deposit, Withdraw, Transfer).
        //   3. Print count from logger.readAll().
        // ============================================================
        // TODO: log transactions to FILE destination
        System.out.println("\n[STEP 25] Logging to FILE destination...");
        TransactionLogger logger = new TransactionLogger(fileDest);
        DepositCommand dep1 = new DepositCommand(acc1, 5000);
        dep1.execute();
        logger.log(dep1);

        WithdrawCommand wth1 = new WithdrawCommand(acc1, 2000, 1234);
        wth1.execute();
        logger.log(wth1);

        TransferCommand trf1 = new TransferCommand(acc1, acc2, 3000, 1234);
        trf1.execute();
        logger.log(trf1);

        System.out.println("  " + logger.getDestinationName() + " log count: " + logger.readAll().size());

        // ============================================================
        // 📝 STEP 26: Switch to Database Destination
        //
        // INSTRUCTIONS:
        //   1. logger.setDestination(dbDest);
        //   2. Execute and log 3 commands.
        //   3. Print count from logger.readAll().
        // ============================================================
        // TODO: log transactions to DATABASE destination
        System.out.println("\n[STEP 26] Switched to DATABASE destination...");
        logger.setDestination(dbDest);
        DepositCommand dep2 = new DepositCommand(acc1, 5000);
        dep2.execute();
        logger.log(dep2);

        WithdrawCommand wth2 = new WithdrawCommand(acc1, 2000, 1234);
        wth2.execute();
        logger.log(wth2);

        TransferCommand trf2 = new TransferCommand(acc1, acc2, 3000, 1234);
        trf2.execute();
        logger.log(trf2);

        System.out.println("  " + logger.getDestinationName() + " log count: " + logger.readAll().size());

        // ============================================================
        // 📝 STEP 27: Switch to Memory Destination
        //
        // INSTRUCTIONS:
        //   1. logger.setDestination(memDest);
        //   2. Execute and log 3 commands.
        //   3. Print count from logger.readAll().
        // ============================================================
        // TODO: log transactions to MEMORY destination
        System.out.println("\n[STEP 27] Switched to MEMORY destination...");
        logger.setDestination(memDest);
        DepositCommand dep3 = new DepositCommand(acc1, 5000);
        dep3.execute();
        logger.log(dep3);

        WithdrawCommand wth3 = new WithdrawCommand(acc1, 2000, 1234);
        wth3.execute();
        logger.log(wth3);

        TransferCommand trf3 = new TransferCommand(acc1, acc2, 3000, 1234);
        trf3.execute();
        logger.log(trf3);

        System.out.println("  " + logger.getDestinationName() + " log count: " + logger.readAll().size());

        // ============================================================
        // 📝 STEP 28: Verify Data Isolation Across Backends
        //
        // INSTRUCTIONS:
        //   1. logger.setDestination(fileDest); print count (should be 3).
        //   2. logger.setDestination(dbDest); print count (should be 3).
        //   3. logger.setDestination(memDest); print count (should be 3).
        // ============================================================
        // TODO: verify each backend maintained its independent data store
        System.out.println("\n[STEP 28] Verifying Data Isolation:");
        logger.setDestination(fileDest);
        int fileCount = logger.readAll().size();
        System.out.println("  FILE count: " + fileCount + " [EXPECTED: 3]");

        logger.setDestination(dbDest);
        int dbCount = logger.readAll().size();
        System.out.println("  DATABASE count: " + dbCount + " [EXPECTED: 3]");

        logger.setDestination(memDest);
        int memCount = logger.readAll().size();
        System.out.println("  MEMORY count: " + memCount + " [EXPECTED: 3]");

        // ============================================================
        // 📝 STEP 29: Print Destination Names
        //
        // INSTRUCTIONS:
        //   Print getDestinationName() for each backend.
        // ============================================================
        // TODO: display active backend names
        if (fileCount == 3 && dbCount == 3 && memCount == 3) {
            System.out.println("\n[STEP 29] All Bridge Pattern log backends verified successfully!");
        }
    }
}
