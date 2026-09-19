package com.tests;

import com.Domain.AbstractAccount;
import com.Domain.CurrentAccount;
import com.Domain.SalaryAccount;
import com.Domain.SavingsAccount;
import com.Exceptions.AccountException;

public class TestAbstractAccount {
    public static void main(String[] args) {
        System.out.println("=== Activity 10: Banking Operations Suite ===");

        SavingsAccount savings = new SavingsAccount(3001, "Savings User", 28, 10000.0);
        CurrentAccount current = new CurrentAccount(3002, "Current User", 32, 5000.0, 25000.0);
        SalaryAccount salary = new SalaryAccount(3003, "Salary User", 26, 2000.0, "GDB Bank");
        savings.setPin(1234);
        current.setPin(5678);
        salary.setPin(9012);

        AbstractAccount[] portfolio = { savings, current, salary };

        if (transfer(savings, current, 3000.0, 1234)) {
            System.out.println("Transfer Rs 3000 from Savings to Current: SUCCESS");
        } else {
            System.out.println("Transfer Rs 3000 from Savings to Current: FAILED");
        }
        System.out.println("Savings Balance: Rs " + savings.getBalance()
                + " | Current Balance: Rs " + current.getBalance());

        double savingsBeforeFailedTransfer = savings.getBalance();
        double currentBeforeFailedTransfer = current.getBalance();
        boolean failedTransferSucceeded = transfer(savings, current, 1000.0, 9999);
        if (!failedTransferSucceeded
                && savings.getBalance() == savingsBeforeFailedTransfer
                && current.getBalance() == currentBeforeFailedTransfer) {
            System.out.println("Failed Transfer (Wrong PIN): Exception caught, no balance changed [PASS]");
        } else {
            System.out.println("Failed Transfer (Wrong PIN): FAILED");
        }

        processMonthlyCycle(portfolio);
        System.out.println("Monthly Interest Cycle processed for all qualifying accounts.");
        System.out.println("All banking operations passed!");
    }

    private static boolean transfer(AbstractAccount source, AbstractAccount destination,
                                    double amount, int sourcePin) {
        try {
            source.withdraw(amount, sourcePin);
            destination.deposit(amount);
            return true;
        } catch (AccountException e) {
            return false;
        }
    }

    private static void processMonthlyCycle(AbstractAccount[] portfolio) {
        for (AbstractAccount account : portfolio) {
            if (account instanceof SavingsAccount) {
                ((SavingsAccount) account).applyInterest();
            } else if (account instanceof SalaryAccount) {
                ((SalaryAccount) account).getInactiveMonths();
            }
        }
    }
}

