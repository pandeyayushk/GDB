package com.Domain;

import com.Exceptions.AccountException;
import com.Exceptions.InvalidAmountException;

/**
 * Common operations available on every type of bank account.
 */
public interface IAccount {
    int getAccountNumber();

    String getCustomerName();

    double getBalance();

    String getAccountType();

    String getStatus();

    void deposit(double amount) throws InvalidAmountException;

    void setPin(int pin);

    void withdraw(double amount, String pin) throws AccountException;

    void displayAccountInfo();
}
