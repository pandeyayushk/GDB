public class SalaryAccount extends Account {
    private static final double MIN_BALANCE = 0.0;
    private String employerName;
    private int inactiveMonths;

    public SalaryAccount(int accountNumber, String name, int age, double initialBalance,
                         String employerName) {
        super(accountNumber, name, age, initialBalance);
        this.employerName = employerName;
        this.inactiveMonths = 0;
    }

    @Override
    public double getMinimumBalance() {
        return MIN_BALANCE;
    }

    @Override
    public String getAccountType() {
        return "Salary";
    }

    public String getEmployerName() {
        return employerName;
    }

    public int getInactiveMonths() {
        return inactiveMonths;
    }

    public void incrementInactiveMonths() {
        inactiveMonths++;
        if (inactiveMonths >= 6) {
            closeAccount();
            System.out.println("Account closed due to inactivity for 6 months.");
        }
    }
}
