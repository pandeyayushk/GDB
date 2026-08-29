import Exceptions.*;

public class Account {
    private int accountNumber;
    private String name;
    private int age;
    private double balance;
    private String accountType;
    private String status;
    private Integer pin;

    private static final int MIN_AGE = 18;
    private static final double MIN_BALANCE_SAVINGS = 500.0;
    private static final double MIN_BALANCE_CURRENT = 1000.0;
    private static final int MIN_PIN = 1000;
    private static final int MAX_PIN = 9999;

    public Account(int accountNumber, String name, int age,
                   double initialBalance, String accountType) {
        if (age < MIN_AGE) {
            throw new IllegalArgumentException("Age must be at least " + MIN_AGE);
        }
        if (!accountType.equalsIgnoreCase("Savings") &&
                !accountType.equalsIgnoreCase("Current")) {
            throw new IllegalArgumentException("Invalid account type.");
        }

        this.accountNumber = accountNumber;
        this.name = name;
        this.age = age;
        this.accountType = accountType;
        this.status = "ACTIVE";
        this.pin = null;

        if (initialBalance < getMinimumBalance()) {
            throw new IllegalArgumentException("Initial balance below minimum.");
        }
        this.balance = initialBalance;
    }

    public void deposit(double amount)
            throws InvalidAmountException, InactiveAccountException {
        validateActive();
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be positive.");
        }
        balance += amount;
    }

    public void withdraw(double amount, int pin)
            throws InvalidAmountException, InsufficientBalanceException,
            MinimumBalanceViolationException, InactiveAccountException,
            InvalidPinException {
        validateActive();
        if (!hasPin() || !verifyPin(pin)) {
            throw new InvalidPinException("Invalid or unset PIN.");
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be positive.");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException("Insufficient funds.");
        }
        if (balance - amount < getMinimumBalance()) {
            throw new MinimumBalanceViolationException("Withdrawal violates minimum balance.");
        }
        balance -= amount;
    }

    public void closeAccount() {
        if (status.equalsIgnoreCase("INACTIVE")) {
            throw new IllegalStateException("Account already closed.");
        }
        status = "INACTIVE";
    }

    public void reopenAccount() {
        if (status.equalsIgnoreCase("ACTIVE")) {
            throw new IllegalStateException("Account already active.");
        }
        status = "ACTIVE";
    }

    public void setPin(int pin) {
        if (pin < MIN_PIN || pin > MAX_PIN) {
            throw new IllegalArgumentException("PIN must be a 4-digit number.");
        }
        this.pin = pin;
    }

    public boolean verifyPin(int pin) {
        return this.pin != null && this.pin == pin;
    }

    public boolean hasPin() {
        return this.pin != null;
    }

    private double getMinimumBalance() {
        return accountType.equalsIgnoreCase("Savings")
                ? MIN_BALANCE_SAVINGS : MIN_BALANCE_CURRENT;
    }

    private void validateActive() throws InactiveAccountException {
        if (!status.equalsIgnoreCase("ACTIVE")) {
            throw new InactiveAccountException("Account is inactive.");
        }
    }

    public int getAccountNumber() { return accountNumber; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public double getBalance() { return balance; }
    public String getAccountType() { return accountType; }
    public String getStatus() { return status; }
}
