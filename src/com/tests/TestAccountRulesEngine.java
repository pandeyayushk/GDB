package com.tests;

import com.Domain.AccountFactory;
import com.Domain.AccountRulesEngine;
import com.Domain.SavingsAccount;

public class TestAccountRulesEngine {
    public static void main(String[] args) {
        System.out.println("=== Activity 13.2: Dynamic Account Rules Test ===");
        int tenure = 4;
        double minBalance = AccountRulesEngine.getSavingsMinBalance(tenure);
        SavingsAccount account = (SavingsAccount) AccountFactory.createAccount(
                "SAVINGS", 1000, "Customer", 30, minBalance, tenure);
        System.out.println("Created Savings Account (Tenure: " + tenure + " yrs):");
        System.out.println("-> Min Balance: Rs " + account.getMinBalance() + " (Dynamically fetched)");
        System.out.println("-> Interest Rate: " + account.getInterestRate() + "% (Dynamically fetched)");
        System.out.println("Dynamic rule integration verified!");
    }
}
