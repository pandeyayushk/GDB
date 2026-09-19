package com.tests;

import com.Domain.*;
import com.Exceptions.*;

public class TestAccount {
    public static void main(String[] args) {
        System.out.println("=== Activity 9: Abstract Account & Template Pattern ===");

        AbstractAccount savings = new SavingsAccount(2001, "John Doe", 25, 10000.0);
        savings.setPin(1234);
        try {
            savings.withdraw(2000.0, 1234);
            System.out.println("[Savings] Withdraw 2000: SUCCESS | Balance: Rs " + savings.getBalance());
        } catch (AccountException e) {
            System.out.println("[Savings] Withdraw 2000: FAILED");
        }

        try {
            savings.withdraw(7500.0, 1234);
            System.out.println("[Savings] Withdraw below min balance: FAILED");
        } catch (MinimumBalanceViolationException e) {
            System.out.println("[Savings] Withdraw below min balance: Caught MinimumBalanceViolationException [PASS]");
        } catch (AccountException e) {
            System.out.println("[Savings] Withdraw below min balance: FAILED");
        }

        AbstractAccount current = new CurrentAccount(2002, "Jane Smith", 30, 5000.0,10000.0);
        current.setPin(1234);
        try {
            current.withdraw(8000.0, 1234);
            System.out.println("[Current] Overdraft debit: SUCCESS | Balance: Rs " + current.getBalance());
        } catch (AccountException e) {
            System.out.println("[Current] Overdraft debit: FAILED");
        }

        AbstractAccount fixedDeposit = new FixedDepositAccount(2003, "FD User", 40, 100000.0, 12, 0.06);
        fixedDeposit.setPin(1234);
        try {
            fixedDeposit.withdraw(5000.0, 1234);
            System.out.println("[FixedDeposit] Premature debit: FAILED");
        } catch (AccountException e) {
            System.out.println("[FixedDeposit] Premature debit: Caught AccountException [PASS]");
        }

        System.out.println("Template method pattern executed successfully!");
    }
}
