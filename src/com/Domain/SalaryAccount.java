package com.Domain;

import com.Exceptions.AccountException;
import com.Exceptions.InsufficientBalanceException;

public class SalaryAccount extends AbstractAccount {
    private static final double MIN_BALANCE = 0.0;
    private final String employerName;
    private int inactiveMonths;

    public SalaryAccount(int accountNumber, String name, int age, double initialBalance, String employerName) {
        super(accountNumber, name, age, initialBalance, "Salary", MIN_BALANCE);
        this.employerName = employerName;
        this.inactiveMonths=0;
    }

    @Override
    protected void processDebit(double amount) throws AccountException {
        if (amount > getBalance()) {
            throw new InsufficientBalanceException("Insufficient balance. Available: Rs " + getBalance()
                    + ", Requested: Rs " + amount);
        }
        setBalance(getBalance() - amount);
    }

    public String getEmployerName() { return employerName; }
    public int getInactiveMonths() { return inactiveMonths; }

    public void incrementInactiveMonths() {
        inactiveMonths++;
        if (inactiveMonths >= 6) {
            closeAccount();
            System.out.println("Account closed due to inactivity for 6 months.");
        }
    }
}
