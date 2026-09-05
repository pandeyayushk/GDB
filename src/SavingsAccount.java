public class SavingsAccount extends Account {
    private static final double MIN_BALANCE = 1000.0;
    private static final double INTEREST_RATE = 0.04; // 4%

    public SavingsAccount(int accountNumber, String name, int age, double initialBalance) {
        super(accountNumber, name, age, initialBalance);
    }

    @Override
    public double getMinimumBalance() {
        return MIN_BALANCE;
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }

    public void applyInterest() {
        double interest = getBalance() * INTEREST_RATE;
        try {
            deposit(interest);
            System.out.println("Interest applied: ₹" + interest);
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getMessage());
        }
    }
}
