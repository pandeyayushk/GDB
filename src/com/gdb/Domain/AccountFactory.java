package com.gdb.Domain;

/** Creates account instances without exposing their concrete types to callers. */
public final class AccountFactory {
    private static final double DEFAULT_OVERDRAFT_LIMIT = 0.0;
    private static final int DEFAULT_FD_TENURE_MONTHS = 12;
    private static final String DEFAULT_EMPLOYER_NAME = "Not specified";

    private AccountFactory() {
        // Utility class.
    }

    /**
     * Creates an account using the details shared by all account types.
     * Type-specific values use sensible defaults; use the varargs overload to supply them.
     */
    public static IAccount createAccount(String accountType, int accountNumber, String customerName,
                                         int age, double initialBalance) {
        return createAccount(accountType, accountNumber, customerName, age, initialBalance, new Object[0]);
    }

    /**
     * Creates an account and optionally accepts type-specific values:
     * SAVINGS: customer tenure in years; CURRENT: overdraft limit;
     * FIXED_DEPOSIT/FD: tenure months, interest rate;
     * SALARY: employer name.
     */
    public static IAccount createAccount(String accountType, int accountNumber, String customerName,
                                         int age, double initialBalance, Object... typeSpecificDetails) {
        if (accountType == null) {
            throw new IllegalArgumentException("Account type cannot be null.");
        }

        switch (accountType.trim().toUpperCase()) {
            case "SAVINGS":
                return new SavingsAccount(accountNumber, customerName, age, initialBalance,
                        numberAt(typeSpecificDetails, 0, 0).intValue());
            case "CURRENT":
                return new CurrentAccount(accountNumber, customerName, age, initialBalance,
                        numberAt(typeSpecificDetails, 0, DEFAULT_OVERDRAFT_LIMIT).doubleValue());
            case "FIXED_DEPOSIT":
            case "FD": {
                int tenureMonths = numberAt(typeSpecificDetails, 0, DEFAULT_FD_TENURE_MONTHS).intValue();
                double defaultRate = AccountRulesEngine.getFDInterestRate(tenureMonths);
                return new FixedDepositAccount(accountNumber, customerName, age, initialBalance,
                        tenureMonths, numberAt(typeSpecificDetails, 1, defaultRate).doubleValue());
            }
            case "SALARY":
                return new SalaryAccount(accountNumber, customerName, age, initialBalance,
                        stringAt(typeSpecificDetails, 0, DEFAULT_EMPLOYER_NAME));
            default:
                throw new IllegalArgumentException("Unknown account type: " + accountType);
        }
    }

    private static Number numberAt(Object[] details, int index, Number defaultValue) {
        if (details.length <= index || details[index] == null) {
            return defaultValue;
        }
        if (!(details[index] instanceof Number)) {
            throw new IllegalArgumentException("Account detail " + (index + 1) + " must be numeric.");
        }
        return (Number) details[index];
    }

    private static String stringAt(Object[] details, int index, String defaultValue) {
        if (details.length <= index || details[index] == null) {
            return defaultValue;
        }
        if (!(details[index] instanceof String)) {
            throw new IllegalArgumentException("Account detail " + (index + 1) + " must be text.");
        }
        return (String) details[index];
    }
}
