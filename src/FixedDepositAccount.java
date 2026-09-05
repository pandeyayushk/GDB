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
        return 0.0; // FD accounts usually don’t require a running minimum balance
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
}
