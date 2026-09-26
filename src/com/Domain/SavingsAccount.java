package com.Domain;

import com.Exceptions.AccountException;
import com.Exceptions.MinimumBalanceViolationException;

public class SavingsAccount extends AbstractAccount {
    private final int tenureYears;
    private final double minBalance;
    private final double interestRate;

    public SavingsAccount(int accountNumber, String name, int age, double initialBalance) {
        this(accountNumber, name, age, initialBalance, 0);
    }

    public SavingsAccount(int accountNumber, String name, int age, double initialBalance, int tenureYears) {
        super(accountNumber, name, age, initialBalance, "Savings",
                AccountRulesEngine.getSavingsMinBalance(tenureYears));
        this.tenureYears = tenureYears;
        this.minBalance = AccountRulesEngine.getSavingsMinBalance(tenureYears);
        this.interestRate = AccountRulesEngine.getSavingsInterestRate(tenureYears);
    }

    @Override
    protected void processDebit(double amount) throws AccountException {
        if (getBalance() - amount < minBalance) {
            throw new MinimumBalanceViolationException("Cannot withdraw. Minimum balance of Rs " + minBalance
                    + " required. Available after withdrawal: Rs " + (getBalance() - amount));
        }
        setBalance(getBalance() - amount);
    }

    public void applyInterest() {
        try {
            deposit(getBalance() * interestRate / 100.0);
        } catch (AccountException e) {
            System.out.println("EXCEPTION: " + e.getMessage());
        }
    }

    public int getTenureYears() { return tenureYears; }
    public double getMinBalance() { return minBalance; }
    public double getInterestRate() { return interestRate; }
}
