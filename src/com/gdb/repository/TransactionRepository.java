package com.gdb.repository;

import com.gdb.domain.Transaction;
import java.util.List;

/** Persistence contract for transactions. */
public interface TransactionRepository {
    void save(Transaction transaction);
    List<Transaction> findByAccount(int accountNumber);
    List<Transaction> findAll();
    void clear();
    /** Executes a database-backed atomic transfer and records its audit row when supported. */
    default Transaction transfer(int fromAccountNumber, int toAccountNumber, double amount) {
        throw new UnsupportedOperationException("Atomic repository transfers require JDBC persistence");
    }
}
