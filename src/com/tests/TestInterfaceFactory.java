package com.tests;

import com.Domain.AccountFactory;
import com.Domain.IAccount;
import com.Exceptions.AccountException;
import com.Exceptions.MinimumBalanceViolationException;

public class TestInterfaceFactory {
    public static void main(String[] args) {
        System.out.println("=== Activity 12: Factory-Driven System Suite ===");

        System.out.println("[Test 1] Savings Account Creation & Deposit: "
                + result(TestInterfaceFactory::testSavingsAccount));
        System.out.println("[Test 2] Current Account Overdraft Withdrawal: "
                + result(TestInterfaceFactory::testCurrentAccountOverdraft));
        System.out.println("[Test 3] Fixed Deposit Premature Withdrawal Block: "
                + result(TestInterfaceFactory::testFixedDepositWithdrawal));
        System.out.println("[Test 4] Invalid Type Rejection: "
                + result(TestInterfaceFactory::testInvalidAccountType));
        System.out.println("Factory-driven architecture successfully verified!");
    }

    private static void testSavingsAccount() throws AccountException {
        IAccount savings = AccountFactory.createAccount("SAVINGS", 5001, "Rajesh Sharma", 30, 1000.0);
        savings.setPin(1234);
        savings.deposit(500.0);

        try {
            savings.withdraw(600.0, "1234");
            throw new AssertionError("Savings withdrawal below the minimum balance was allowed.");
        } catch (MinimumBalanceViolationException expected) {
            if (savings.getBalance() != 1500.0) {
                throw new AssertionError("Savings balance changed after a rejected withdrawal.");
            }
        }
    }

    private static void testCurrentAccountOverdraft() throws AccountException {
        IAccount current = AccountFactory.createAccount("CURRENT", 5002, "Priya Patel", 32,
                5000.0, 10000.0);
        current.setPin(2345);
        current.withdraw(12000.0, "2345");

        if (current.getBalance() != -7000.0) {
            throw new AssertionError("Current-account overdraft was not applied correctly.");
        }
    }

    private static void testFixedDepositWithdrawal() throws AccountException {
        IAccount fixedDeposit = AccountFactory.createAccount("FIXED_DEPOSIT", 5003, "Amit Kumar", 38,
                50000.0, 24, 0.065);
        fixedDeposit.setPin(3456);

        try {
            fixedDeposit.withdraw(1000.0, "3456");
            throw new AssertionError("Premature fixed-deposit withdrawal was allowed.");
        } catch (AccountException expected) {
            // FixedDepositAccount deliberately blocks all withdrawals before maturity.
        }
    }

    private static void testInvalidAccountType() {
        try {
            AccountFactory.createAccount("INVESTMENT", 5004, "Sneha Verma", 27, 5000.0);
            throw new AssertionError("Unknown account type was accepted.");
        } catch (IllegalArgumentException expected) {
            // The factory must reject unsupported account types.
        }
    }

    private static String result(CheckedTest test) {
        try {
            test.run();
            return "[PASS]";
        } catch (Exception | AssertionError failure) {
            return "[FAIL]";
        }
    }

    @FunctionalInterface
    private interface CheckedTest {
        void run() throws Exception;
    }
}
