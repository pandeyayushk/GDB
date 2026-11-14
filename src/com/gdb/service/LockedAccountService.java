package com.gdb.service;

import com.gdb.concurrency.AccountLock;
import com.gdb.concurrency.AccountLockRegistry;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Fine-grained account locking with consistent ordering for multi-account operations. */
public class LockedAccountService {
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;
    private final AccountLockRegistry lockRegistry;

    public LockedAccountService(AccountRepository accountRepo, TransactionRepository transactionRepo) {
        this.accountRepo = Objects.requireNonNull(accountRepo, "accountRepo");
        this.transactionRepo = Objects.requireNonNull(transactionRepo, "transactionRepo");
        this.lockRegistry = new AccountLockRegistry();
    }

    public void deposit(int accountNumber, double amount) throws Exception {
        validateAmount(amount);
        try (AccountLock ignored = lockRegistry.acquire(accountNumber)) {
            Lock write = lockRegistry.rwLockFor(accountNumber).writeLock();
            write.lock();
            try {
                IAccount account = requireAccount(accountNumber);
                account.deposit(amount);
                accountRepo.update(account);
                transactionRepo.save(record(accountNumber, TransactionType.DEPOSIT, amount,
                        account.getBalance(), 0, 0, "Deposit to Account #" + accountNumber));
            } finally { write.unlock(); }
        }
    }

    public void withdraw(int accountNumber, double amount, int pin) throws Exception {
        validateAmount(amount);
        try (AccountLock ignored = lockRegistry.acquire(accountNumber)) {
            Lock write = lockRegistry.rwLockFor(accountNumber).writeLock();
            write.lock();
            try {
                IAccount account = requireAccount(accountNumber);
                account.withdraw(amount, pin);
                accountRepo.update(account);
                transactionRepo.save(record(accountNumber, TransactionType.WITHDRAW, amount,
                        account.getBalance(), 0, 0, "Withdrawal from Account #" + accountNumber));
            } finally { write.unlock(); }
        }
    }

    public void transfer(int fromAccount, int toAccount, double amount, int pin) throws Exception {
        validateAmount(amount);
        if (fromAccount == toAccount) throw new IllegalArgumentException("Source and destination must differ");
        int firstAccount = Math.min(fromAccount, toAccount);
        int secondAccount = Math.max(fromAccount, toAccount);
        try (AccountLock first = lockRegistry.acquire(firstAccount);
             AccountLock second = lockRegistry.acquire(secondAccount)) {
            Lock firstWrite = lockRegistry.rwLockFor(firstAccount).writeLock();
            Lock secondWrite = lockRegistry.rwLockFor(secondAccount).writeLock();
            firstWrite.lock();
            secondWrite.lock();
            try {
                requireAccount(fromAccount);
                requireAccount(toAccount);
                accountRepo.transfer(fromAccount, toAccount, amount, pin);
                IAccount source = requireAccount(fromAccount);
                transactionRepo.save(record(fromAccount, TransactionType.TRANSFER, amount,
                        source.getBalance(), fromAccount, toAccount, "Transfer to Account #" + toAccount));
            } finally {
                secondWrite.unlock();
                firstWrite.unlock();
            }
        }
    }

    public double getBalance(int accountNumber, int pin) throws Exception {
        ReentrantReadWriteLock rw = lockRegistry.rwLockFor(accountNumber);
        Lock read = rw.readLock();
        read.lock();
        try {
            IAccount account = requireAccount(accountNumber);
            if (!account.verifyPin(pin)) throw new InvalidPinException("Incorrect PIN");
            return account.getBalance();
        } finally { read.unlock(); }
    }

    public IAccount getAccount(int accountNumber) {
        ReentrantReadWriteLock rw = lockRegistry.rwLockFor(accountNumber);
        Lock read = rw.readLock();
        read.lock();
        try { return accountRepo.findById(accountNumber); }
        finally { read.unlock(); }
    }

    public void saveAccount(IAccount account) {
        Objects.requireNonNull(account, "account");
        try (AccountLock ignored = lockRegistry.acquire(account.getAccountNumber())) {
            Lock write = lockRegistry.rwLockFor(account.getAccountNumber()).writeLock();
            write.lock();
            try { accountRepo.save(account); }
            finally { write.unlock(); }
        }
    }

    private IAccount requireAccount(int number) throws AccountException {
        IAccount account = accountRepo.findById(number);
        if (account == null) throw new AccountException("Account not found: " + number);
        return account;
    }

    private static void validateAmount(double amount) throws InvalidAmountException {
        if (!Double.isFinite(amount) || amount <= 0) throw new InvalidAmountException("Amount must be positive and finite");
    }

    private static Transaction record(int account, TransactionType type, double amount, double balance,
                                      int from, int to, String description) {
        return new Transaction(Transaction.generateId(), LocalDateTime.now(), account, type, amount, balance,
                "SUCCESS", description, from, to);
    }
}
