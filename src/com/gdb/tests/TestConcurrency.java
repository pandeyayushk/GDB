package com.gdb.tests;

import com.gdb.concurrency.ConcurrentDepositDemo;
import com.gdb.domain.AccountFactory;
import com.gdb.domain.IAccount;
import com.gdb.repository.AccountRepository;
import com.gdb.repository.InMemoryAccountRepository;
import com.gdb.repository.InMemoryTransactionRepository;
import com.gdb.repository.TransactionRepository;
import com.gdb.service.ThreadSafeAccountService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Stress checks for race conditions, service synchronization, and funds conservation. */
public final class TestConcurrency {
    private static final int PIN = 1234;

    public static void main(String[] args) {
        try {
            testUnsynchronizedRaceCondition();
            testSynchronizedConcurrentDeposits();
            testConcurrentTransfersConservationOfFunds();
            testConcurrentWithdrawalsSafety();
            System.out.println("\n============================================================");
            System.out.println("  ALL ACTIVITY 27 CONCURRENCY TESTS PASSED!");
            System.out.println("============================================================");
        } catch (Throwable t) {
            System.err.println("TEST SUITE FAILED: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testUnsynchronizedRaceCondition() throws Exception {
        System.out.println("\n[TEST 1] Unsynchronized Race Condition Demonstration:");
        IAccount account = account(27001, "Unsafe", 10000);
        double actual = ConcurrentDepositDemo.runUnsafeDemo(account, 10, 100, 100);
        double expected = 110000;
        check(actual < expected, "unsynchronized read-modify-write loses concurrent updates");
        check(actual == 20000, "barrier makes the lost-update demonstration deterministic");
        check(account.getBalance() == 10000, "unsafe demonstration does not mutate repository account");
        System.out.println("  Expected without races: Rs. " + expected + "; unsafe result: Rs. " + actual);
        System.out.println("  -> PASSED (lost updates reproduced)");
    }

    private static void testSynchronizedConcurrentDeposits() throws Exception {
        System.out.println("\n[TEST 2] Synchronized Concurrent Deposits:");
        AccountRepository accounts = new InMemoryAccountRepository();
        TransactionRepository transactions = new InMemoryTransactionRepository();
        IAccount account = account(27002, "Deposit Target", 10000);
        accounts.save(account);
        ThreadSafeAccountService service = new ThreadSafeAccountService(accounts, transactions);
        double actual = ConcurrentDepositDemo.runSafeDemo(service, 27002, 10, 100, 100);
        check(actual == 110000, "all 1000 serialized deposits are applied");
        check(transactions.findByAccount(27002).size() == 1000, "one ledger entry per successful deposit");
        check(service.getBalance(27002, PIN) == 110000, "synchronized balance read");
        System.out.println("  Initial balance: Rs. 10000.0; final balance: Rs. " + actual);
        System.out.println("  -> PASSED (zero lost updates)");
    }

    private static void testConcurrentTransfersConservationOfFunds() throws Exception {
        System.out.println("\n[TEST 3] Concurrent Transfers - Conservation of Total Funds:");
        AccountRepository accounts = new InMemoryAccountRepository();
        TransactionRepository transactions = new InMemoryTransactionRepository();
        IAccount first = account(27003, "Transfer A", 100000);
        IAccount second = account(27004, "Transfer B", 100000);
        accounts.save(first); accounts.save(second);
        ThreadSafeAccountService service = new ThreadSafeAccountService(accounts, transactions);
        double initialTotal = first.getBalance() + second.getBalance();
        int threads = 10, operations = 100;
        runWorkers(threads, index -> {
            for (int i = 0; i < operations; i++) {
                if ((i & 1) == 0) service.transfer(27003, 27004, 100, PIN);
                else service.transfer(27004, 27003, 100, PIN);
            }
        });
        double finalTotal = accounts.findById(27003).getBalance() + accounts.findById(27004).getBalance();
        check(finalTotal == initialTotal, "concurrent transfers preserve total funds");
        check(transactions.findAll().size() == threads * operations, "all transfer audit entries recorded");
        System.out.println("  Total before: Rs. " + initialTotal + "; total after: Rs. " + finalTotal);
        System.out.println("  -> PASSED (funds conserved)");
    }

    private static void testConcurrentWithdrawalsSafety() throws Exception {
        System.out.println("\n[TEST 4] Concurrent Withdrawals Under Contention:");
        AccountRepository accounts = new InMemoryAccountRepository();
        TransactionRepository transactions = new InMemoryTransactionRepository();
        IAccount account = account(27005, "Withdrawal Target", 50000);
        accounts.save(account);
        ThreadSafeAccountService service = new ThreadSafeAccountService(accounts, transactions);
        AtomicInteger successful = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        runWorkers(50, index -> {
            try { service.withdraw(27005, 1000, PIN); successful.incrementAndGet(); }
            catch (com.gdb.exceptions.InsufficientBalanceException expected) { }
            catch (Throwable failure) { unexpected.compareAndSet(null, failure); }
        });
        if (unexpected.get() != null) throw new AssertionError("Unexpected withdrawal failure", unexpected.get());
        double expectedBalance = 50000 - successful.get() * 1000.0;
        check(successful.get() == 42, "only withdrawals that preserve the minimum balance succeed");
        check(accounts.findById(27005).getBalance() == expectedBalance, "withdrawal balance matches successful operations");
        check(accounts.findById(27005).getBalance() >= accounts.findById(27005).getMinimumBalance(), "minimum balance remains intact");
        System.out.println("  Successful withdrawals: " + successful.get() + "; final balance: Rs. " + expectedBalance);
        System.out.println("  -> PASSED (no overdraft or lost updates)");
    }

    private static IAccount account(int number, String name, double balance) throws Exception {
        IAccount account = AccountFactory.createAccount("SAVINGS", number, name, 30, balance, 1);
        account.setPin(PIN);
        return account;
    }

    private interface Worker { void run(int index) throws Exception; }

    private static void runWorkers(int count, Worker action) throws Exception {
        CountDownLatch ready = new CountDownLatch(count), start = new CountDownLatch(1), done = new CountDownLatch(count);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                ready.countDown();
                try { start.await(); action.run(index); }
                catch (Throwable e) { failure.compareAndSet(null, e); }
                finally { done.countDown(); }
            }, "concurrency-test-" + i);
            workers.add(thread); thread.start();
        }
        ready.await(); start.countDown(); done.await();
        for (Thread worker : workers) worker.join();
        if (failure.get() != null) throw new AssertionError("Worker thread failed", failure.get());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("Failed: " + message);
    }
}
