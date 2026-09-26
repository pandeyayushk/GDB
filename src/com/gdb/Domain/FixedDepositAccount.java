package com.gdb.Domain;

import com.gdb.Exceptions.AccountException;

public class FixedDepositAccount extends AbstractAccount {
    private final int tenureMonths;
    private final double interestRate;

    public FixedDepositAccount(int accountNumber, String name, int age, double initialBalance,
                               int tenureMonths, double interestRate) {
        super(accountNumber, name, age, initialBalance, "Fixed Deposit", 0.0);
        this.tenureMonths = tenureMonths;
        this.interestRate = interestRate;
    }

    @Override
    protected void processDebit(double amount) throws AccountException {
        throw new AccountException("Premature withdrawals are not permitted on Fixed Deposit accounts before maturity.");
    }

    public double calculateMaturityAmount() {
        return getBalance() * Math.pow(1 + interestRate / 12, tenureMonths);
    }

    public int getTenureMonths() { return tenureMonths; }
    public double getInterestRate() { return interestRate; }
}
