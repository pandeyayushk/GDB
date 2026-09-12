package com.Domain;

import com.Exceptions.*;
public class FixedDepositAccount extends Account {
    private int tenureMonths;
    private double interestRate;

    public FixedDepositAccount(int accountNumber, String name, int age, double initialBalance,
                               int tenureMonths, double interestRate) {
        super(accountNumber, name, age, initialBalance);
        this.tenureMonths = tenureMonths;
        this.interestRate = interestRate;
    }

    @Override
    public double getMinimumBalance() {
        return 0.0;
    }

    @Override
    public String getAccountType() {
        return "Fixed Deposit";
    }

    public double calculateMaturityAmount() {
        double principal = getBalance();
        double monthlyRate = interestRate / 12;
        return principal * Math.pow(1 + monthlyRate, tenureMonths);
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public double getInterestRate() {
        return interestRate;
    }

    @Override
    public void withdraw(double amount, int pin) throws AccountException {
        throw new AccountException(
                "Premature withdrawals are not permitted on Fixed Deposit accounts before maturity."
        );
    }


}
