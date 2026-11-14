package com.gdb.tests;

import com.gdb.concurrency.AccountLock;
import com.gdb.concurrency.AccountLockRegistry;
import com.gdb.domain.AccountFactory;
import com.gdb.domain.IAccount;
import com.gdb.repository.AccountRepository;
import com.gdb.repository.InMemoryAccountRepository;
import com.gdb.repository.InMemoryTransactionRepository;
import com.gdb.repository.TransactionRepository;
import com.gdb.service.AtomicAccountService;
import com.gdb.service.LockedAccountService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Checks per-account isolation, ordered transfers, and lock-free atomic primitives. */
public final class TestLocksAndAtomics {
    private static final int PIN = 1234;

    public static void main(String[] args) {
        try {
            testConcurrentDepositsSameAccount();
            testConcurrentDepositsDifferentAccountsParallel();
            testDeadlockFreeBidirectionalTransfers();
            testAtomicServiceCountersAndCas();
            testAccountLockAutoClose();
            System.out.println("\n============================================================");
            System.out.println("  ALL ACTIVITY 28 LOCKS & ATOMICS TESTS PASSED!");
            System.out.println("============================================================");
        } catch (Throwable t) {
            System.err.println("TEST SUITE FAILED: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testConcurrentDepositsSameAccount() throws Exception {
        System.out.println("\n[TEST 1] Fine-Grained Locking - Concurrent Deposits to Same Account:");
        AccountRepository accounts = new InMemoryAccountRepository();
        TransactionRepository transactions = new InMemoryTransactionRepository();
        IAccount account = account(28001, "Same Account", 10000);
        accounts.save(account);
        LockedAccountService service = new LockedAccountService(accounts, transactions);
        runConcurrent(10, () -> {
            try { for (int i = 0; i < 100; i++) service.deposit(28001, 100); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        check(service.getBalance(28001, PIN) == 110000, "same-account deposits have no lost updates");
        check(transactions.findByAccount(28001).size() == 1000, "all deposits have transaction records");
        System.out.println("  Expected Balance: Rs. 110000.0, Actual: Rs. " + service.getBalance(28001, PIN));
        System.out.println("  -> PASSED");
    }

    private static void testConcurrentDepositsDifferentAccountsParallel() throws Exception {
        System.out.println("\n[TEST 2] Fine-Grained Locking - Distinct Accounts Run In Parallel:");
        InMemoryAccountRepository backing = new InMemoryAccountRepository();
        backing.save(account(28011, "Blocked Account", 10000));
        backing.save(account(28012, "Independent Account", 10000));
        BlockingAccountRepository accounts = new BlockingAccountRepository(backing, 28011);
        LockedAccountService service = new LockedAccountService(accounts, new InMemoryTransactionRepository());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> blocked = pool.submit(() -> { try { service.deposit(28011, 100); } catch (Exception e) { throw new RuntimeException(e); } });
            check(accounts.entered.await(2, TimeUnit.SECONDS), "first account reached blocking repository operation");
            Future<?> independent = pool.submit(() -> { try { service.deposit(28012, 100); } catch (Exception e) { throw new RuntimeException(e); } });
            independent.get(2, TimeUnit.SECONDS);
            accounts.release.countDown();
            blocked.get(2, TimeUnit.SECONDS);
            check(backing.findById(28011).getBalance() == 10100 && backing.findById(28012).getBalance() == 10100,
                    "both independent account operations complete");
        } finally {
            accounts.release.countDown();
            pool.shutdownNow();
        }
        System.out.println("  Independent account completed while the first account was blocked.");
        System.out.println("  -> PASSED");
    }

    private static void testDeadlockFreeBidirectionalTransfers() throws Exception {
        System.out.println("\n[TEST 3] Lock Ordering - Deadlock-Free Bidirectional Transfers:");
        AccountRepository accounts = new InMemoryAccountRepository();
        TransactionRepository transactions = new InMemoryTransactionRepository();
        IAccount a = account(28021, "Transfer A", 100000);
        IAccount b = account(28022, "Transfer B", 100000);
        accounts.save(a); accounts.save(b);
        LockedAccountService service = new LockedAccountService(accounts, transactions);
        runConcurrent(10, () -> {
            try {
                for (int i = 0; i < 100; i++) {
                    if ((i & 1) == 0) service.transfer(28021, 28022, 100, PIN);
                    else service.transfer(28022, 28021, 100, PIN);
                }
            } catch (Exception e) { throw new IllegalStateException(e); }
        }, 15, TimeUnit.SECONDS);
        double total = accounts.findById(28021).getBalance() + accounts.findById(28022).getBalance();
        check(total == 200000, "reciprocal transfers conserve the combined balance");
        check(transactions.findAll().size() == 1000, "all transfer audit rows are recorded");
        System.out.println("  Combined balance after transfers: Rs. " + total);
        System.out.println("  -> PASSED");
    }

    private static void testAtomicServiceCountersAndCas() throws Exception {
        System.out.println("\n[TEST 4] Lock-Free Atomic Primitives & CAS Status Updates:");
        AtomicAccountService service = new AtomicAccountService();
        Set<String> ids = ConcurrentHashMap.newKeySet();
        runConcurrent(10, () -> {
            for (int i = 0; i < 100; i++) {
                ids.add(service.nextTransactionId());
                service.recordDepositVolume(1.25);
            }
        });
        check(ids.size() == 1000, "transaction IDs are unique across threads");
        check(service.getGlobalTransactionCount() == 2000, "atomic transaction sequence count");
        check(service.getTotalDepositVolume() == 1250.0, "atomic cents accumulation");

        AtomicInteger successfulTransitions = new AtomicInteger();
        runConcurrent(20, () -> {
            if (service.updateSystemStatus("ONLINE", "MAINTENANCE")) successfulTransitions.incrementAndGet();
        });
        check(successfulTransitions.get() == 1, "exactly one CAS status transition succeeds");
        check("MAINTENANCE".equals(service.getSystemStatus()), "status changed atomically");
        System.out.println("  Generated 1000 unique IDs; final counter: " + service.getGlobalTransactionCount());
        System.out.println("  -> PASSED");
    }

    private static void testAccountLockAutoClose() {
        AccountLockRegistry registry = new AccountLockRegistry();
        check(registry.lockFor(1) == registry.lockFor(1), "same account gets the same lock");
        check(registry.lockFor(1) != registry.lockFor(2), "different accounts get independent locks");
        try (AccountLock ignored = registry.acquire(1)) { check(registry.lockFor(1).isHeldByCurrentThread(), "lock acquired"); }
        check(!registry.lockFor(1).isLocked(), "try-with-resources releases the lock");
    }

    private static IAccount account(int number, String name, double balance) throws Exception {
        IAccount account = AccountFactory.createAccount("SAVINGS", number, name, 30, balance, 1);
        account.setPin(PIN);
        return account;
    }

    private static void runConcurrent(int count, Runnable task) throws Exception {
        runConcurrent(count, task, 10, TimeUnit.SECONDS);
    }

    private static void runConcurrent(int count, Runnable task, long timeout, TimeUnit unit) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count), start = new CountDownLatch(1);
        @SuppressWarnings("unchecked") Future<?>[] futures = new Future<?>[count];
        try {
            for (int i = 0; i < count; i++) futures[i] = pool.submit(() -> {
                ready.countDown();
                try { start.await(); task.run(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            });
            check(ready.await(timeout, unit), "workers reached start barrier");
            start.countDown();
            for (Future<?> future : futures) future.get(timeout, unit);
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("Failed: " + message);
    }

    private static final class BlockingAccountRepository implements AccountRepository {
        private final AccountRepository delegate;
        private final int blockedAccount;
        private final AtomicBoolean blockOnce = new AtomicBoolean(true);
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private BlockingAccountRepository(AccountRepository delegate, int blockedAccount) {
            this.delegate = delegate; this.blockedAccount = blockedAccount;
        }
        @Override public void save(IAccount a) { delegate.save(a); }
        @Override public IAccount findById(int n) {
            if (n == blockedAccount && blockOnce.compareAndSet(true, false)) {
                entered.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for release"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            }
            return delegate.findById(n);
        }
        @Override public java.util.List<IAccount> findAll() { return delegate.findAll(); }
        @Override public void update(IAccount a) { delegate.update(a); }
        @Override public void delete(int n) { delegate.delete(n); }
        @Override public boolean exists(int n) { return delegate.exists(n); }
        @Override public int nextAccountNumber() { return delegate.nextAccountNumber(); }
        @Override public void transfer(int from, int to, double amount, int pin) { delegate.transfer(from, to, amount, pin); }
    }
}
