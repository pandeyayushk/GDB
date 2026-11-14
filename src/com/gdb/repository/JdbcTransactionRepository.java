package com.gdb.repository;

import com.gdb.db.ConnectionProvider;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransactionType;
import com.gdb.exceptions.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** SQLite transaction ledger with atomic account transfers. */
public final class JdbcTransactionRepository implements TransactionRepository {
    private final ConnectionProvider provider;

    public JdbcTransactionRepository(ConnectionProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override public void save(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction");
        String sql = "INSERT INTO transactions (transaction_id, timestamp, account_number, transaction_type, amount, balance_after, status, description, from_account, to_account) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, transaction);
            s.executeUpdate();
        } catch (SQLException e) {
            throw failure("save transaction", e);
        }
    }

    @Override public List<Transaction> findByAccount(int accountNumber) {
        String sql = "SELECT * FROM transactions WHERE account_number = ? OR from_account = ? OR to_account = ? ORDER BY timestamp ASC, transaction_id ASC";
        List<Transaction> transactions = new ArrayList<>();
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, accountNumber); s.setInt(2, accountNumber); s.setInt(3, accountNumber);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) transactions.add(mapRow(rs));
            }
            return transactions;
        } catch (SQLException | IllegalArgumentException e) {
            throw failure("find transactions for account " + accountNumber, e);
        }
    }

    @Override public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement("SELECT * FROM transactions ORDER BY timestamp ASC, transaction_id ASC"); ResultSet rs = s.executeQuery()) {
            while (rs.next()) transactions.add(mapRow(rs));
            return transactions;
        } catch (SQLException | IllegalArgumentException e) {
            throw failure("list transactions", e);
        }
    }

    @Override public void clear() {
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM transactions")) {
            s.executeUpdate();
        } catch (SQLException e) {
            throw failure("clear transactions", e);
        }
    }

    /** Atomically updates both balances and writes one transfer audit record. */
    @Override public Transaction transfer(int fromAccountNumber, int toAccountNumber, double amount) {
        if (fromAccountNumber == toAccountNumber) throw new IllegalArgumentException("Source and destination accounts must differ");
        if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("Transfer amount must be positive and finite");
        try (Connection c = provider.getConnection()) {
            c.setAutoCommit(false);
            try {
                double sourceBalance = balance(c, fromAccountNumber);
                balance(c, toAccountNumber); // Verify destination exists before modifying either row.
                if (sourceBalance < amount) throw new IllegalArgumentException("Insufficient balance");
                try (PreparedStatement debit = c.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND active = 1 AND balance >= ?");
                     PreparedStatement credit = c.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE account_number = ? AND active = 1")) {
                    debit.setDouble(1, amount); debit.setInt(2, fromAccountNumber); debit.setDouble(3, amount);
                    if (debit.executeUpdate() != 1) throw new IllegalStateException("Source account is inactive or has insufficient balance");
                    credit.setDouble(1, amount); credit.setInt(2, toAccountNumber);
                    if (credit.executeUpdate() != 1) throw new IllegalStateException("Destination account is inactive or missing");
                }
                Transaction record = new Transaction(Transaction.generateId(), LocalDateTime.now(), fromAccountNumber,
                        TransactionType.TRANSFER, amount, sourceBalance - amount, "SUCCESS",
                        "Transfer to Account #" + toAccountNumber, fromAccountNumber, toAccountNumber);
                insert(c, record);
                c.commit();
                return record;
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof RuntimeException) throw (RuntimeException)e;
                throw failure("transfer funds", e);
            }
        } catch (SQLException e) {
            throw failure("transfer funds", e);
        }
    }

    private static double balance(Connection c, int accountNumber) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT balance FROM accounts WHERE account_number = ? AND active = 1")) {
            s.setInt(1, accountNumber);
            try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Active account not found: " + accountNumber);
                return rs.getDouble(1);
            }
        }
    }

    private static void insert(Connection c, Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (transaction_id, timestamp, account_number, transaction_type, amount, balance_after, status, description, from_account, to_account) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement s = c.prepareStatement(sql)) { bind(s, transaction); s.executeUpdate(); }
    }

    private static void bind(PreparedStatement s, Transaction t) throws SQLException {
        s.setString(1, t.getTransactionId());
        s.setString(2, (t.getTimestamp() == null ? LocalDateTime.now() : t.getTimestamp()).toString());
        s.setInt(3, t.getAccountNumber());
        s.setString(4, t.getType().name());
        s.setDouble(5, t.getAmount()); s.setDouble(6, t.getBalanceAfter());
        s.setString(7, t.getStatus()); s.setString(8, t.getDescription());
        s.setInt(9, t.getFromAccount()); s.setInt(10, t.getToAccount());
    }

    private static Transaction mapRow(ResultSet rs) throws SQLException {
        String timestamp = rs.getString("timestamp");
        return new Transaction(rs.getString("transaction_id"), timestamp == null ? LocalDateTime.now() : LocalDateTime.parse(timestamp),
                rs.getInt("account_number"), TransactionType.valueOf(rs.getString("transaction_type")),
                rs.getDouble("amount"), rs.getDouble("balance_after"), rs.getString("status"),
                rs.getString("description"), rs.getInt("from_account"), rs.getInt("to_account"));
    }

    private static DataAccessException failure(String action, Exception cause) {
        return new DataAccessException("Unable to " + action, cause);
    }
}
