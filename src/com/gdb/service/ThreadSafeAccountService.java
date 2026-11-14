package com.gdb.service;

import com.gdb.domain.AccountFactory;
import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransactionType;
import com.gdb.exceptions.AccountException;
import com.gdb.exceptions.InvalidAmountException;
import com.gdb.exceptions.InvalidPinException;
import com.gdb.repository.AccountRepository;
import com.gdb.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.Objects;

/** Serializes service operations that share mutable account and transaction repositories. */
public class ThreadSafeAccountService {
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;

    public ThreadSafeAccountService(AccountRepository accountRepo, TransactionRepository transactionRepo) {
        this.accountRepo = Objects.requireNonNull(accountRepo, "accountRepo");
        this.transactionRepo = Objects.requireNonNull(transactionRepo, "transactionRepo");
    }

    public synchronized void deposit(int accountNumber, double amount) throws Exception {
        validateAmount(amount);
        IAccount account = requireAccount(accountNumber);
        account.deposit(amount);
        accountRepo.update(account);
        transactionRepo.save(record(accountNumber, TransactionType.DEPOSIT, amount, account.getBalance(), 0, 0,
                "Deposit to Account #" + accountNumber));
    }

    public synchronized void withdraw(int accountNumber, double amount, int pin) throws Exception {
        validateAmount(amount);
        IAccount account = requireAccount(accountNumber);
        account.withdraw(amount, pin);
        accountRepo.update(account);
        transactionRepo.save(record(accountNumber, TransactionType.WITHDRAW, amount, account.getBalance(), 0, 0,
                "Withdrawal from Account #" + accountNumber));
    }

    public synchronized void transfer(int fromAccount, int toAccount, double amount, int pin) throws Exception {
        validateAmount(amount);
        if (fromAccount == toAccount) throw new IllegalArgumentException("Source and destination must differ");
        requireAccount(fromAccount);
        requireAccount(toAccount);
        accountRepo.transfer(fromAccount, toAccount, amount, pin);
        IAccount source = requireAccount(fromAccount);
        transactionRepo.save(record(fromAccount, TransactionType.TRANSFER, amount, source.getBalance(), fromAccount,
                toAccount, "Transfer to Account #" + toAccount));
    }

    public synchronized double getBalance(int accountNumber, int pin) throws Exception {
        IAccount account = requireAccount(accountNumber);
        if (!account.verifyPin(pin)) throw new InvalidPinException("Incorrect PIN");
        return account.getBalance();
    }

    public synchronized IAccount getAccount(int accountNumber) { return accountRepo.findById(accountNumber); }

    public synchronized IAccount openAccount(String type, String name, int age, double initialBalance) throws AccountException {
        IAccount account = AccountFactory.createAccount(type, accountRepo.nextAccountNumber(), name, age, initialBalance);
        accountRepo.save(account);
        return account;
    }

    public synchronized void saveAccount(IAccount account) {
        accountRepo.save(Objects.requireNonNull(account, "account"));
    }

    private IAccount requireAccount(int accountNumber) throws AccountException {
        IAccount account = accountRepo.findById(accountNumber);
        if (account == null) throw new AccountException("Account not found: " + accountNumber);
        return account;
    }

    private static void validateAmount(double amount) throws InvalidAmountException {
        if (!Double.isFinite(amount) || amount <= 0) throw new InvalidAmountException("Amount must be positive and finite");
    }

    private static Transaction record(int accountNumber, TransactionType type, double amount, double balance,
                                      int from, int to, String description) {
        return new Transaction(Transaction.generateId(), LocalDateTime.now(), accountNumber, type, amount, balance,
                "SUCCESS", description, from, to);
    }
}
