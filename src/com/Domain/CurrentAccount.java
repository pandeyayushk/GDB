package com.Domain;

import com.Exceptions.AccountException;
import com.Exceptions.InsufficientBalanceException;

public class CurrentAccount extends AbstractAccount {
    private static final double MIN_BALANCE = 5000.0;
    private double overdraftLimit;

    public CurrentAccount(int accountNumber, String name, int age, double initialBalance, double overdraftLimit) {
        super(accountNumber, name, age, initialBalance, "Current", MIN_BALANCE);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    protected void processDebit(double amount) throws AccountException {
        double availableFunds = getBalance() + overdraftLimit;
        if (amount > availableFunds) {
            throw new InsufficientBalanceException("Insufficient funds. Available including overdraft: Rs "
                    + availableFunds + ", Requested: Rs " + amount);
        }
        setBalance(getBalance() - amount);
    }

    public double getOverdraftLimit() { return overdraftLimit; }
    public void setOverdraftLimit(double overdraftLimit) { this.overdraftLimit = overdraftLimit; }
}
