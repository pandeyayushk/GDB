package com.Domain;

import com.Exceptions.AccountException;
import com.Exceptions.MinimumBalanceViolationException;

public class SavingsAccount extends AbstractAccount {
    private static final double MIN_BALANCE = 1000.0;
    private static final double INTEREST_RATE = 0.04;

    public SavingsAccount(int accountNumber, String name, int age, double initialBalance) {
        super(accountNumber, name, age, initialBalance, "Savings", MIN_BALANCE);
    }

    @Override
    protected void processDebit(double amount) throws AccountException {
        if (getBalance() - amount < MIN_BALANCE) {
            throw new MinimumBalanceViolationException("Cannot withdraw. Minimum balance of Rs " + MIN_BALANCE
                    + " required. Available after withdrawal: Rs " + (getBalance() - amount));
        }
        setBalance(getBalance() - amount);
    }

    public void applyInterest() {
        try {
            deposit(getBalance() * INTEREST_RATE);
        } catch (AccountException e) {
            System.out.println("EXCEPTION: " + e.getMessage());
        }
    }
}
