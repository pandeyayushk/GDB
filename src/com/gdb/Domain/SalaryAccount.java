package com.gdb.Domain;

import com.gdb.Exceptions.AccountException;
import com.gdb.Exceptions.InsufficientBalanceException;

public class SalaryAccount extends AbstractAccount {
    private final String employerName;
    private int inactiveMonths;

    public SalaryAccount(int accountNumber, String name, int age, double initialBalance, String employerName) {
        super(accountNumber, name, age, initialBalance, "Salary", AccountRulesEngine.getSalaryMinBalance());
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
        if (inactiveMonths >= AccountRulesEngine.getSalaryInactivityMonths()) {
            closeAccount();
            System.out.println("Account closed due to inactivity for 6 months.");
        }
    }
}
