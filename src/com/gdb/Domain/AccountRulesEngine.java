package com.gdb.Domain;

public final class AccountRulesEngine {
    private static final AccountRulesPropertiesLoader SAVINGS =
            new AccountRulesPropertiesLoader("savings.properties");
    private static final AccountRulesPropertiesLoader CURRENT =
            new AccountRulesPropertiesLoader("current.properties");
    private static final AccountRulesPropertiesLoader FIXED_DEPOSIT =
            new AccountRulesPropertiesLoader("fixeddeposit.properties");
    private static final AccountRulesPropertiesLoader SALARY =
            new AccountRulesPropertiesLoader("salary.properties");

    private AccountRulesEngine() { }

    public static String getSavingsBucket(int tenureYears) {
        if (tenureYears >= 5) return "privilege";
        if (tenureYears >= 3) return "premium";
        if (tenureYears >= 1) return "standard";
        return "new";
    }

    public static double getSavingsMinBalance(int tenureYears) {
        return SAVINGS.getDouble("savings." + getSavingsBucket(tenureYears) + ".minBalance", 10000.0);
    }

    public static double getSavingsInterestRate(int tenureYears) {
        return SAVINGS.getDouble("savings." + getSavingsBucket(tenureYears) + ".interestRate", 2.70);
    }

    public static double getCurrentMinBalance() {
        return CURRENT.getDouble("current.minBalance", 5000.0);
    }

    public static double getCurrentOverdraftLimit(double monthlyTurnover) {
        double floor = CURRENT.getDouble("current.overdraft.minimum", 25000.0);
        double multiplier = CURRENT.getDouble("current.overdraft.turnoverMultiplier", 2.5);
        return Math.max(floor, monthlyTurnover * multiplier);
    }

    public static double getFDInterestRate(int months) {
        if (months >= 36) return FIXED_DEPOSIT.getDouble("fixeddeposit.rate.36MonthsOrMore", 7.50);
        if (months >= 12) return FIXED_DEPOSIT.getDouble("fixeddeposit.rate.12To35Months", 6.50);
        return FIXED_DEPOSIT.getDouble("fixeddeposit.rate.under12Months", 5.00);
    }

    public static double getSalaryMinBalance() {
        return SALARY.getDouble("salary.minBalance", 0.0);
    }

    public static int getSalaryInactivityMonths() {
        return (int) SALARY.getDouble("salary.inactivityMonthsBeforeClosure", 6.0);
    }
}
