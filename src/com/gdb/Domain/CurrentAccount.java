package com.gdb.Domain;

import com.gdb.Exceptions.AccountException;
import com.gdb.Exceptions.InsufficientBalanceException;

public class CurrentAccount extends AbstractAccount {
    private double overdraftLimit;

    public CurrentAccount(int accountNumber, String name, int age, double initialBalance, double overdraftLimit) {
        super(accountNumber, name, age, initialBalance, "Current", AccountRulesEngine.getCurrentMinBalance());
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
