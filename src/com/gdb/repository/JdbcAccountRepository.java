package com.gdb.repository;

import com.gdb.db.ConnectionProvider;
import com.gdb.domain.AccountFactory;
import com.gdb.domain.IAccount;
import com.gdb.exceptions.AccountException;
import com.gdb.exceptions.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** SQLite-backed account repository. Every value is bound through a PreparedStatement. */
public class JdbcAccountRepository implements AccountRepository {
    private static final String COLUMNS = "account_number, account_type, holder_name, age, balance, active, opening_date, tenure_years, pin";
    private final ConnectionProvider provider;

    public JdbcAccountRepository(ConnectionProvider provider) { this.provider = Objects.requireNonNull(provider, "provider"); }

    @Override public void save(IAccount account) {
        Objects.requireNonNull(account, "account");
        String sql = "INSERT INTO accounts (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, account); s.executeUpdate();
        } catch (SQLException e) { throw failure("save account " + account.getAccountNumber(), e); }
    }

    @Override public IAccount findById(int accountNumber) {
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + " FROM accounts WHERE account_number = ?")) {
            s.setInt(1, accountNumber);
            try (ResultSet rs = s.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException | AccountException e) { throw failure("find account " + accountNumber, e); }
    }

    @Override public List<IAccount> findAll() {
        List<IAccount> accounts = new ArrayList<>();
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + " FROM accounts ORDER BY account_number"); ResultSet rs = s.executeQuery()) {
            while (rs.next()) accounts.add(map(rs));
            return accounts;
        } catch (SQLException | AccountException e) { throw failure("list accounts", e); }
    }

    @Override public void update(IAccount account) {
        Objects.requireNonNull(account, "account");
        String sql = "UPDATE accounts SET account_type=?, holder_name=?, age=?, balance=?, active=?, opening_date=?, tenure_years=?, pin=? WHERE account_number=?";
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, normalizedType(account.getAccountType())); s.setString(2, account.getAccountHolderName());
            s.setInt(3, age(account)); s.setDouble(4, account.getBalance()); s.setInt(5, account.isActive() ? 1 : 0);
            s.setString(6, account.getOpeningDate()); s.setInt(7, account.getTenureYears()); s.setObject(8, pin(account));
            s.setInt(9, account.getAccountNumber()); s.executeUpdate();
        } catch (SQLException e) { throw failure("update account " + account.getAccountNumber(), e); }
    }

    @Override public void delete(int accountNumber) {
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM accounts WHERE account_number = ?")) {
            s.setInt(1, accountNumber); s.executeUpdate();
        } catch (SQLException e) { throw failure("delete account " + accountNumber, e); }
    }

    @Override public boolean exists(int accountNumber) {
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement("SELECT 1 FROM accounts WHERE account_number = ?")) {
            s.setInt(1, accountNumber); try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw failure("check account " + accountNumber, e); }
    }

    @Override public int nextAccountNumber() {
        try (Connection c = provider.getConnection(); PreparedStatement s = c.prepareStatement("SELECT COALESCE(MAX(account_number), 1000) + 1 FROM accounts"); ResultSet rs = s.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1001;
        } catch (SQLException e) { throw failure("get next account number", e); }
    }

    /** Debits and credits inside one database transaction; any validation or SQL failure rolls both back. */
    @Override public void transfer(int fromNumber, int toNumber, double amount, int pin) {
        if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("Transfer amount must be positive and finite");
        if (fromNumber == toNumber) throw new IllegalArgumentException("Source and destination must differ");
        try (Connection c = provider.getConnection()) {
            c.setAutoCommit(false);
            try {
                IAccount from = selectForTransfer(c, fromNumber), to = selectForTransfer(c, toNumber);
                if (!from.isActive() || !to.isActive()) throw new IllegalStateException("Both accounts must be active");
                if (!from.verifyPin(pin)) throw new IllegalArgumentException("Incorrect PIN");
                from.withdraw(amount, pin);
                to.deposit(amount);
                try (PreparedStatement debit = c.prepareStatement("UPDATE accounts SET balance=? WHERE account_number=? AND active=1");
                     PreparedStatement credit = c.prepareStatement("UPDATE accounts SET balance=? WHERE account_number=? AND active=1")) {
                    debit.setDouble(1, from.getBalance()); debit.setInt(2, fromNumber);
                    if (debit.executeUpdate() != 1) throw new SQLException("Source account changed during transfer");
                    credit.setDouble(1, to.getBalance()); credit.setInt(2, toNumber);
                    if (credit.executeUpdate() != 1) throw new SQLException("Destination account changed during transfer");
                }
                c.commit();
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof RuntimeException) throw (RuntimeException)e;
                throw failure("transfer funds", e);
            }
        } catch (SQLException e) { throw failure("transfer funds", e); }
    }

    private IAccount selectForTransfer(Connection c, int number) throws SQLException, AccountException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + " FROM accounts WHERE account_number=?")) {
            s.setInt(1, number); try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Account not found: " + number);
                return map(rs);
            }
        }
    }

    private static IAccount map(ResultSet rs) throws SQLException, AccountException {
        IAccount account = AccountFactory.createAccount(rs.getString("account_type"), rs.getInt("account_number"),
                rs.getString("holder_name"), rs.getInt("age"), rs.getDouble("balance"), rs.getInt("tenure_years"));
        int pin = rs.getInt("pin"); if (!rs.wasNull()) account.setPin(pin);
        if (account instanceof com.gdb.domain.Account)
            ((com.gdb.domain.Account)account).setOpeningDate(rs.getString("opening_date"));
        if (rs.getInt("active") == 0) account.closeAccount();
        return account;
    }

    private static void bind(PreparedStatement s, IAccount a) throws SQLException {
        s.setInt(1, a.getAccountNumber()); s.setString(2, normalizedType(a.getAccountType()));
        s.setString(3, a.getAccountHolderName()); s.setInt(4, age(a)); s.setDouble(5, a.getBalance());
        s.setInt(6, a.isActive() ? 1 : 0); s.setString(7, a.getOpeningDate()); s.setInt(8, a.getTenureYears());
        s.setObject(9, pin(a));
    }
    private static Integer pin(IAccount a) { return a instanceof com.gdb.domain.Account ? ((com.gdb.domain.Account)a).getPin() : null; }
    private static int age(IAccount a) { return a instanceof com.gdb.domain.Account ? ((com.gdb.domain.Account)a).getAge() : 0; }
    private static String normalizedType(String type) { return type.replace(" ", "").toUpperCase(java.util.Locale.ROOT); }
    private static DataAccessException failure(String action, Exception cause) { return new DataAccessException("Unable to " + action, cause); }
}
