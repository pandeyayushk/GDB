package com.tests;

import com.Domain.*;
import com.Exceptions.*;

public class TestAccount {
    public static void main(String[] args) {
        System.out.println("=== Activity 8: Polymorphism Test ===");
        Account savings = new SavingsAccount(2001, "John Doe", 25, 10000.0);
        savings.setPin(1234);

        try {
            savings.withdraw(9500.0, 1234);
            System.out.println("[Savings] Withdraw 9500 (breaches min balance 1000): FAILED");
        } catch (MinimumBalanceViolationException e) {
            System.out.println("[Savings] Withdraw 9500 (breaches min balance 1000): Caught MinimumBalanceViolationException [PASS]");
        } catch (Exception e) {
            System.out.println("[Savings] Unexpected exception: " + e.getMessage() + " [FAIL]");
        }

        Account current = new CurrentAccount(2002, "Jane Smith", 30, 5000.0);
        current.setPin(1234);
        try {
            current.withdraw(10000.0, 1234); // balance goes to -5000
            if (current.getBalance() == -5000.0) {
                System.out.println("[Current] Withdraw with Overdraft (Balance goes to -5000): SUCCESS [PASS]");
            } else {
                System.out.println("[Current] Balance mismatch after overdraft [FAIL]");
            }
        } catch (Exception e) {
            System.out.println("[Current] Unexpected exception: " + e.getMessage() + " [FAIL]");
        }

        try {
            current.withdraw(30000.0, 1234);
            System.out.println("[Current] Withdraw exceeding Overdraft (exceeds -25000): FAILED");
        } catch (InsufficientBalanceException e) {
            System.out.println("[Current] Withdraw exceeding Overdraft (exceeds -25000): Caught InsufficientBalanceException [PASS]");
        } catch (Exception e) {
            System.out.println("[Current] Unexpected exception: " + e.getMessage() + " [FAIL]");
        }

        Account fd = new FixedDepositAccount(2003, "FD User", 40, 100000.0, 12, 0.06);
        fd.setPin(1234);
        try {
            fd.withdraw(5000.0, 1234);
            System.out.println("[FixedDeposit] Withdraw attempt: FAILED");
        } catch (AccountException e) {
            System.out.println("[FixedDeposit] Withdraw attempt: Caught AccountException [PASS]");
        } catch (Exception e) {
            System.out.println("[FixedDeposit] Unexpected exception: " + e.getMessage() + " [FAIL]");
        }

        System.out.println("=== Complete Activity 8 polymorphism tests and verify output ===");
    }
}
