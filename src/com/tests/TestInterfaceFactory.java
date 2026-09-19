package com.tests;

import com.Domain.AccountFactory;
import com.Domain.IAccount;

public class TestInterfaceFactory {
    public static void main(String[] args) {
        System.out.println("=== Activity 11: Interface & Factory Pattern Test ===");

        IAccount savings = AccountFactory.createAccount("SAVINGS", 4001, "Rajesh Sharma", 30, 10000.0);
        IAccount current = AccountFactory.createAccount("CURRENT", 4002, "Priya Patel", 32, 15000.0, 25000.0);
        IAccount fixedDeposit = AccountFactory.createAccount("FIXED_DEPOSIT", 4003, "Amit Kumar", 38,
                50000.0, 24, 0.065);
        IAccount salary = AccountFactory.createAccount("SALARY", 4004, "Sneha Verma", 27,
                5000.0, "GDB Bank");

        printCreated("SAVINGS", savings);
        printCreated("CURRENT", current);
        printCreated("FIXED_DEPOSIT", fixedDeposit);
        printCreated("SALARY", salary);
        System.out.println("All accounts successfully created through AccountFactory!");
    }

    private static void printCreated(String accountType, IAccount account) {
        System.out.println("Factory created: " + accountType + " account for " + account.getCustomerName());
    }
}
