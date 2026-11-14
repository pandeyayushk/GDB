package com.gdb.concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Creates and reuses independent locks for each account key. */
public final class AccountLockRegistry {
    private final ConcurrentHashMap<Integer, ReentrantLock> accountLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ReentrantReadWriteLock> rwLocks = new ConcurrentHashMap<>();

    public ReentrantLock lockFor(int accountNumber) {
        return accountLocks.computeIfAbsent(accountNumber, ignored -> new ReentrantLock(true));
    }

    public ReentrantReadWriteLock rwLockFor(int accountNumber) {
        return rwLocks.computeIfAbsent(accountNumber, ignored -> new ReentrantReadWriteLock(true));
    }

    public AccountLock acquire(int accountNumber) {
        return new AccountLock(lockFor(accountNumber));
    }
}
