package com.gdb.concurrency;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/** Acquires a lock at construction and releases it once when closed. */
public final class AccountLock implements AutoCloseable {
    private final ReentrantLock lock;
    private boolean held = true;

    public AccountLock(ReentrantLock lock) {
        this.lock = Objects.requireNonNull(lock, "lock");
        this.lock.lock();
    }

    @Override public void close() {
        if (held) {
            held = false;
            lock.unlock();
        }
    }
}
