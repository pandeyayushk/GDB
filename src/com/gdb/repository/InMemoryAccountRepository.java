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
}
