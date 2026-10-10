package com.gdb.repository;

import com.gdb.domain.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** List-backed transaction repository. */
public class InMemoryTransactionRepository implements TransactionRepository {
    private final List<Transaction> transactions = new ArrayList<>();
    public void save(Transaction transaction) { transactions.add(transaction); }
    public List<Transaction> findByAccount(int accountNumber) {
        return transactions.stream().filter(t -> t.getAccountNumber() == accountNumber
                || t.getFromAccount() == accountNumber || t.getToAccount() == accountNumber)
                .collect(Collectors.toList());
    }
    public List<Transaction> findAll() { return new ArrayList<>(transactions); }
    public void clear() { transactions.clear(); }
}
