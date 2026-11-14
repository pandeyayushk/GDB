package com.gdb.repository;

import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransactionType;
import com.gdb.exceptions.DataAccessException;
import com.gdb.file.AtomicFileWriter;
import com.gdb.file.BackupManager;
import com.gdb.file.CsvMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** CSV backed transaction ledger with coordinated account-file transfers. */
public class FileTransactionRepository implements TransactionRepository {
    private static final int MAX_BACKUPS = 5;
    private final String filePath;
    private final boolean autosave;
    private final AccountRepository accounts;
    private final List<Transaction> cache = new ArrayList<>();

    public FileTransactionRepository(String filePath, boolean autosave, int startId) {
        this(filePath, autosave, startId, new FileAccountRepository(accountPathFor(filePath)));
    }
    public FileTransactionRepository(String filePath, int startId) { this(filePath, true, startId); }
    public FileTransactionRepository(String filePath) { this(filePath, true, 1); }
    public FileTransactionRepository(String filePath, AccountRepository accounts) { this(filePath, true, 1, accounts); }
    public FileTransactionRepository(String filePath, boolean autosave, int startId, AccountRepository accounts) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        this.autosave = autosave;
        this.accounts = Objects.requireNonNull(accounts, "accounts");
        load();
    }

    public synchronized void load() {
        cache.clear();
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return;
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty() || line.equals(CsvMapper.TRANSACTION_HEADER)) continue;
                cache.add(CsvMapper.fromCsvTransaction(line));
            }
        } catch (IOException | RuntimeException e) {
            throw new DataAccessException("Unable to load transactions from " + filePath, e);
        }
    }

    public synchronized void flush() {
        List<String> lines = new ArrayList<>();
        lines.add(CsvMapper.TRANSACTION_HEADER);
        for (Transaction transaction : cache) lines.add(CsvMapper.toCsv(transaction));
        try {
            BackupManager.backup(filePath, MAX_BACKUPS);
            AtomicFileWriter.write(filePath, lines);
        } catch (IOException e) {
            throw new DataAccessException("Unable to persist transactions to " + filePath, e);
        }
    }

    @Override public synchronized void save(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction");
        cache.add(transaction);
        if (autosave) try { flush(); } catch (RuntimeException e) { cache.remove(cache.size() - 1); throw e; }
    }

    @Override public synchronized List<Transaction> findByAccount(int accountNumber) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction transaction : cache) {
            if (transaction.getAccountNumber() == accountNumber || transaction.getFromAccount() == accountNumber
                    || transaction.getToAccount() == accountNumber) result.add(transaction);
        }
        return result;
    }
    @Override public synchronized List<Transaction> findAll() { return new ArrayList<>(cache); }

    @Override public synchronized void clear() {
        List<Transaction> previous = new ArrayList<>(cache);
        cache.clear();
        if (autosave) try { flush(); } catch (RuntimeException e) { cache.addAll(previous); throw e; }
    }

    @Override public synchronized Transaction transfer(int fromNumber, int toNumber, double amount) {
        if (fromNumber == toNumber) throw new IllegalArgumentException("Source and destination must differ");
        if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("Transfer amount must be positive and finite");
        if (accounts instanceof FileAccountRepository) ((FileAccountRepository)accounts).load();
        IAccount from = accounts.findById(fromNumber), to = accounts.findById(toNumber);
        if (from == null || to == null) throw new IllegalArgumentException("Both accounts must exist");
        if (!from.isActive() || !to.isActive()) throw new IllegalStateException("Both accounts must be active");
        if (!from.canWithdraw(amount)) throw new IllegalStateException("Insufficient available balance");
        double newSourceBalance = from.getBalance() - amount;
        double newDestinationBalance = to.getBalance() + amount;
        if (!Double.isFinite(newDestinationBalance)) throw new IllegalArgumentException("Destination balance overflow");
        IAccount oldFrom = CsvMapper.withBalance(from, from.getBalance());
        IAccount oldTo = CsvMapper.withBalance(to, to.getBalance());
        IAccount debited = CsvMapper.withBalance(from, newSourceBalance);
        IAccount credited = CsvMapper.withBalance(to, newDestinationBalance);
        Transaction record = new Transaction(Transaction.generateId(), LocalDateTime.now(), fromNumber,
                TransactionType.TRANSFER, amount, newSourceBalance, "SUCCESS",
                "Transfer to Account #" + toNumber, fromNumber, toNumber);
        try {
            replaceAccounts(Arrays.asList(debited, credited));
            save(record);
            return record;
        } catch (RuntimeException e) {
            try { replaceAccounts(Arrays.asList(oldFrom, oldTo)); }
            catch (RuntimeException rollback) { e.addSuppressed(rollback); }
            throw new DataAccessException("Unable to complete file-backed transfer", e);
        }
    }

    private void replaceAccounts(List<IAccount> replacements) {
        if (accounts instanceof FileAccountRepository) ((FileAccountRepository)accounts).updateAll(replacements);
        else for (IAccount account : replacements) accounts.update(account);
    }

    private static String accountPathFor(String transactionPath) {
        Path path = Paths.get(transactionPath).toAbsolutePath();
        return path.resolveSibling("accounts.csv").toString();
    }
}
