package com.gdb.concurrency;

import com.gdb.domain.IAccount;
import com.gdb.service.ThreadSafeAccountService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

/** Demonstrates a deliberately unsynchronized lost update and the synchronized service result. */
public final class ConcurrentDepositDemo {
    private ConcurrentDepositDemo() { }

    public static double runUnsafeDemo(IAccount account, int numThreads, double depositAmount, int iterations) {
        validate(numThreads, depositAmount, iterations);
        final double[] sharedBalance = {account.getBalance()};
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(numThreads);
        CyclicBarrier afterRead = new CyclicBarrier(numThreads), afterWrite = new CyclicBarrier(numThreads);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < numThreads; t++) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        double observed = sharedBalance[0];
                        afterRead.await();
                        sharedBalance[0] = observed + depositAmount;
                        afterWrite.await();
                    }
                } catch (Throwable e) { failure.compareAndSet(null, e); }
                finally { done.countDown(); }
            }, "unsafe-deposit-" + t);
            threads.add(worker); worker.start();
        }
        start.countDown();
        awaitAll(done, failure, threads);
        return sharedBalance[0];
    }

    public static double runSafeDemo(ThreadSafeAccountService service, int accountNumber,
                                     int numThreads, double depositAmount, int iterations) throws Exception {
        validate(numThreads, depositAmount, iterations);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(numThreads);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < numThreads; t++) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) service.deposit(accountNumber, depositAmount);
                } catch (Throwable e) { failure.compareAndSet(null, e); }
                finally { done.countDown(); }
            }, "safe-deposit-" + t);
            threads.add(worker); worker.start();
        }
        start.countDown();
        awaitAll(done, failure, threads);
        return service.getAccount(accountNumber).getBalance();
    }

    private static void awaitAll(CountDownLatch done, AtomicReference<Throwable> failure, List<Thread> threads) {
        try {
            done.await();
            for (Thread thread : threads) thread.join();
        } catch (InterruptedException e) {
            for (Thread thread : threads) thread.interrupt();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for deposit workers", e);
        }
        Throwable problem = failure.get();
        if (problem != null) throw new IllegalStateException("A deposit worker failed", problem);
    }

    private static void validate(int threads, double amount, int iterations) {
        if (threads <= 0 || iterations < 0 || !Double.isFinite(amount) || amount <= 0)
            throw new IllegalArgumentException("Thread count and deposit amount must be positive; iterations cannot be negative");
    }
}
