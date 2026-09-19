package com.Domain;

import com.Exceptions.*;

public abstract class AbstractAccount implements IAccount {
    private static final int MIN_AGE = 18;
    private static final int MIN_PIN = 1000;
    private static final int MAX_PIN = 9999;

    private final int accountNumber;
    private final String name;
    private final int age;
    private double balance;
    private final String accountType;
    private String status;
    private Integer pin;

    protected AbstractAccount(int accountNumber, String name, int age, double initialBalance,
                              String accountType, double minimumInitialBalance) {
        if (age < MIN_AGE) {
            throw new IllegalArgumentException("Customer must be at least " + MIN_AGE + " years old. Provided: " + age);
        }
        if (initialBalance < minimumInitialBalance) {
            throw new IllegalArgumentException(accountType + " account requires minimum balance of Rs "
                    + minimumInitialBalance + ". Provided: Rs " + initialBalance);
        }
        this.accountNumber = accountNumber;
        this.name = name;
        this.age = age;
        this.balance = initialBalance;
        this.accountType = accountType;
        this.status = "Active";
    }

    @Override
    public void deposit(double amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be positive. Provided: Rs " + amount);
        }
        balance += amount;
    }

    /** Fixed withdrawal sequence; subclasses provide only their debit rule. */
    public final void withdraw(double amount, int pin) throws AccountException {
        validatePin(pin);
        validateActive();
        validateAmount(amount);
        processDebit(amount);
    }

    @Override
    public final void withdraw(double amount, String pin) throws AccountException {
        try {
            withdraw(amount, Integer.parseInt(pin));
        } catch (NumberFormatException e) {
            throw new InvalidPinException("PIN must be a 4-digit number.");
        }
    }

    protected abstract void processDebit(double amount) throws AccountException;

    protected void validatePin(int pin) throws InvalidPinException {
        if (this.pin == null) throw new InvalidPinException("PIN not set for this account");
        if (!verifyPin(pin)) throw new InvalidPinException("Incorrect PIN");
    }

    protected void validateActive() throws InactiveAccountException {
        if (!"Active".equalsIgnoreCase(status)) {
            throw new InactiveAccountException("Account is inactive. Please reopen the account or contact support.");
        }
    }

    protected void validateAmount(double amount) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Withdrawal amount must be positive. Provided: Rs " + amount);
    }

    @Override
    public void setPin(int pin) {
        validateNewPin(pin);
        this.pin = pin;
    }

    public void changePin(int currentPin, int newPin) throws InvalidPinException {
        validatePin(currentPin);
        validateNewPin(newPin);
        this.pin = newPin;
    }

    private void validateNewPin(int pin) {
        if (pin < MIN_PIN || pin > MAX_PIN) throw new IllegalArgumentException("PIN must be a 4-digit number.");
    }

    public boolean verifyPin(int pin) { return this.pin != null && this.pin == pin; }
    public boolean hasPin() { return pin != null; }

    public void closeAccount() {
        if ("Inactive".equalsIgnoreCase(status)) throw new IllegalStateException("Account is already closed.");
        status = "Inactive";
    }

    public void reopenAccount() {
        if ("Active".equalsIgnoreCase(status)) throw new IllegalStateException("Account is already active.");
        status = "Active";
    }

    @Override
    public void displayAccountInfo() {
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Name: " + name);
        System.out.println("Age: " + age);
        System.out.println("Account Type: " + accountType);
        System.out.println("Balance: Rs " + balance);
        System.out.println("Status: " + status);
    }

    protected void setBalance(double balance) { this.balance = balance; }
    @Override
    public int getAccountNumber() { return accountNumber; }
    public String getName() { return name; }
    @Override
    public String getCustomerName() { return name; }
    public int getAge() { return age; }
    @Override
    public double getBalance() { return balance; }
    @Override
    public String getAccountType() { return accountType; }
    @Override
    public String getStatus() { return status; }
    public Integer getPin() { return pin; }
}
