package com.gdb.repository;

import com.gdb.domain.IAccount;
import com.gdb.exceptions.DataAccessException;
import com.gdb.file.AtomicFileWriter;
import com.gdb.file.BackupManager;
import com.gdb.file.CsvMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** CSV backed account repository with an in-memory cache and atomic persistence. */
public class FileAccountRepository implements AccountRepository {
    private static final int MAX_BACKUPS = 5;
    private final String filePath;
    private final boolean autosave;
    private final Map<Integer, IAccount> cache = new TreeMap<>();
    private int nextAccountNumber;

    public FileAccountRepository(String filePath, boolean autosave, int startAccountNumber) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        this.autosave = autosave;
        this.nextAccountNumber = startAccountNumber;
        load();
    }
    public FileAccountRepository(String filePath, int startAccountNumber) { this(filePath, true, startAccountNumber); }
    public FileAccountRepository(String filePath) { this(filePath, true, 1001); }

    public synchronized void load() {
        cache.clear();
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) { nextAccountNumber = Math.max(nextAccountNumber, 1001); return; }
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty() || line.toLowerCase(java.util.Locale.ROOT).startsWith("account_number,")) continue;
                IAccount account = CsvMapper.fromCsv(line);
                cache.put(account.getAccountNumber(), account);
                nextAccountNumber = Math.max(nextAccountNumber, account.getAccountNumber() + 1);
            }
        } catch (IOException | RuntimeException e) {
            throw new DataAccessException("Unable to load accounts from " + filePath, e);
        }
    }

    public synchronized void flush() {
        List<String> lines = new ArrayList<>();
        lines.add(CsvMapper.ACCOUNT_HEADER);
        for (IAccount account : cache.values()) lines.add(CsvMapper.toCsv(account));
        try {
            BackupManager.backup(filePath, MAX_BACKUPS);
            AtomicFileWriter.write(filePath, lines);
        } catch (IOException e) {
            throw new DataAccessException("Unable to persist accounts to " + filePath, e);
        }
    }

    @Override public synchronized void save(IAccount account) {
        Objects.requireNonNull(account, "account");
        IAccount previous = cache.put(account.getAccountNumber(), account);
        int oldNext = nextAccountNumber;
        nextAccountNumber = Math.max(nextAccountNumber, account.getAccountNumber() + 1);
        if (autosave) try { flush(); } catch (RuntimeException e) {
            if (previous == null) cache.remove(account.getAccountNumber()); else cache.put(account.getAccountNumber(), previous);
            nextAccountNumber = oldNext;
            throw e;
        }
    }

    @Override public synchronized IAccount findById(int accountNumber) { return cache.get(accountNumber); }
    @Override public synchronized List<IAccount> findAll() { return new ArrayList<>(cache.values()); }

    @Override public synchronized void update(IAccount account) {
        Objects.requireNonNull(account, "account");
        if (cache.containsKey(account.getAccountNumber())) updateAll(java.util.Collections.singletonList(account));
    }

    /** Replaces a set of account snapshots with one atomic accounts-file rewrite. */
    public synchronized void updateAll(List<IAccount> accounts) {
        Map<Integer, IAccount> previous = new TreeMap<>();
        for (IAccount account : accounts) {
            Objects.requireNonNull(account, "account");
            int number = account.getAccountNumber();
            previous.put(number, cache.get(number));
            if (cache.containsKey(number)) cache.put(number, account);
        }
        if (!autosave) return;
        try { flush(); }
        catch (RuntimeException e) {
            for (Map.Entry<Integer, IAccount> entry : previous.entrySet()) {
                if (entry.getValue() == null) cache.remove(entry.getKey()); else cache.put(entry.getKey(), entry.getValue());
            }
            throw e;
        }
    }

    @Override public synchronized void delete(int accountNumber) {
        IAccount previous = cache.remove(accountNumber);
        if (previous == null || !autosave) return;
        try { flush(); } catch (RuntimeException e) { cache.put(accountNumber, previous); throw e; }
    }
    @Override public synchronized boolean exists(int accountNumber) { return cache.containsKey(accountNumber); }
    @Override public synchronized int nextAccountNumber() { return nextAccountNumber++; }

    @Override public synchronized void transfer(int fromNumber, int toNumber, double amount, int pin) {
        if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("Transfer amount must be positive and finite");
        if (fromNumber == toNumber) throw new IllegalArgumentException("Source and destination must differ");
        IAccount from = cache.get(fromNumber), to = cache.get(toNumber);
        if (from == null || to == null) throw new IllegalArgumentException("Both accounts must exist");
        IAccount oldFrom = CsvMapper.withBalance(from, from.getBalance());
        IAccount oldTo = CsvMapper.withBalance(to, to.getBalance());
        try {
            from.withdraw(amount, pin);
            to.deposit(amount);
            updateAll(java.util.Arrays.asList(from, to));
        } catch (Exception e) {
            try { updateAll(java.util.Arrays.asList(oldFrom, oldTo)); }
            catch (RuntimeException rollback) { e.addSuppressed(rollback); }
            throw new IllegalStateException("Unable to transfer funds", e);
        }
    }
}
