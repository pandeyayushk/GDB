package com.gdb.repository;

import com.gdb.domain.IAccount;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Map-backed account repository with account numbers beginning at 1001. */
public class InMemoryAccountRepository implements AccountRepository {
    private final Map<Integer, IAccount> accounts = new LinkedHashMap<>();
    private int nextAccountNumber = 1001;

    public void save(IAccount account) {
        accounts.put(account.getAccountNumber(), account);
        nextAccountNumber = Math.max(nextAccountNumber, account.getAccountNumber() + 1);
    }
    public IAccount findById(int accountNumber) { return accounts.get(accountNumber); }
    public List<IAccount> findAll() { return new ArrayList<>(accounts.values()); }
    public void update(IAccount account) {
        if (exists(account.getAccountNumber())) accounts.put(account.getAccountNumber(), account);
    }
    public void delete(int accountNumber) { accounts.remove(accountNumber); }
    public boolean exists(int accountNumber) { return accounts.containsKey(accountNumber); }
    public int nextAccountNumber() { return nextAccountNumber; }
    public synchronized void transfer(int fromAccountNumber, int toAccountNumber, double amount, int pin) {
        if (amount <= 0 || !Double.isFinite(amount)) throw new IllegalArgumentException("Transfer amount must be positive and finite");
        IAccount from = accounts.get(fromAccountNumber), to = accounts.get(toAccountNumber);
        if (from == null || to == null) throw new IllegalArgumentException("Both accounts must exist");
        if (!from.isActive() || !to.isActive() || from.getBalance() < amount)
            throw new IllegalStateException("Accounts must be active and source balance sufficient");
        try { from.withdraw(amount, pin); to.deposit(amount); }
        catch (com.gdb.exceptions.AccountException e) { throw new IllegalStateException(e.getMessage(), e); }
    }
}
