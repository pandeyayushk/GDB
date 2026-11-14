package com.gdb.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Lock-free transaction numbering, deposit-volume accounting, and status transitions. */
public final class AtomicAccountService {
    private final AtomicLong globalTxnCounter = new AtomicLong(1000);
    private final AtomicLong totalDepositVolumeCents = new AtomicLong();
    private final AtomicReference<String> systemStatus = new AtomicReference<>("ONLINE");

    public String nextTransactionId() { return "ATOMIC-TXN-" + globalTxnCounter.incrementAndGet(); }

    public long recordDepositVolume(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive and finite");
        long cents;
        try { cents = BigDecimal.valueOf(amount).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Deposit amount is outside supported range", e); }
        return totalDepositVolumeCents.updateAndGet(current -> Math.addExact(current, cents));
    }

    public long getGlobalTransactionCount() { return globalTxnCounter.get(); }
    public double getTotalDepositVolume() { return totalDepositVolumeCents.get() / 100.0; }

    public boolean updateSystemStatus(String expected, String updated) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(updated, "updated");
        return systemStatus.compareAndSet(expected, updated);
    }

    public String getSystemStatus() { return systemStatus.get(); }
}
