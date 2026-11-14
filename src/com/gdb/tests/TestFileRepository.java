package com.gdb.tests;

import com.gdb.domain.AccountFactory;
import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransactionType;
import com.gdb.file.AtomicFileWriter;
import com.gdb.file.BackupManager;
import com.gdb.file.CsvMapper;
import com.gdb.repository.FileAccountRepository;
import com.gdb.repository.FileTransactionRepository;
import com.gdb.repository.RepositoryFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Isolated integration checks for CSV storage, atomic writes, backups, and file transfers. */
public final class TestFileRepository {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("gdb-activity26-");
        Path accountsFile = directory.resolve("accounts.csv");
        Path transactionsFile = directory.resolve("transactions.csv");
        try {
            check("file".equals(RepositoryFactory.getPersistenceMode()), "file persistence mode configured");
            check(RepositoryFactory.getAccountRepository() instanceof FileAccountRepository, "factory selects file account repository");
            check(RepositoryFactory.getTransactionRepository() instanceof FileTransactionRepository, "factory selects file transaction repository");

            Path atomicFile = directory.resolve("nested").resolve("atomic.txt");
            AtomicFileWriter.write(atomicFile.toString(), Arrays.asList("old", "content"));
            AtomicFileWriter.write(atomicFile.toString(), Arrays.asList("complete", "replacement"));
            check(Files.readAllLines(atomicFile).equals(Arrays.asList("complete", "replacement")), "atomic replace contents");
            check(!Files.exists(atomicFile.resolveSibling("atomic.txt.tmp")), "temporary file cleaned up");

            Path legacyCsv = directory.resolve("legacy-accounts.csv");
            AtomicFileWriter.write(legacyCsv.toString(), Arrays.asList(
                    "account_number,account_holder_name,age,balance,account_type,status,pin,opening_date,overdraft_used,interest_earned",
                    "26999,Legacy User,30,50000,Savings,Active,9876,2025-01-01,0.0,0.0"));
            check(new FileAccountRepository(legacyCsv.toString()).findById(26999).verifyPin(9876), "legacy archive CSV remains readable");

            FileAccountRepository accounts = new FileAccountRepository(accountsFile.toString());
            IAccount source = AccountFactory.createAccount("SAVINGS", 26001, "Rajesh, \"R\" Sharma", 30, 50000, 1);
            source.setPin(1234);
            IAccount target = AccountFactory.createAccount("CURRENT", 26002, "Receiver", 35, 40000, 2);
            target.setPin(2345);
            accounts.save(source); accounts.save(target);
            check(accounts.findById(26001).getAccountHolderName().equals("Rajesh, \"R\" Sharma"), "CSV quotes and commas round trip");
            check(accounts.findById(26001).verifyPin(1234), "PIN round trips from CSV");
            check(accounts.findAll().size() == 2, "account list");
            check(accounts.nextAccountNumber() == 26003, "next number follows stored maximum");

            IAccount updated = accounts.findById(26001);
            updated.deposit(10000);
            accounts.update(updated);
            check(new FileAccountRepository(accountsFile.toString()).findById(26001).getBalance() == 60000, "account update persists on disk");

            IAccount disposable = AccountFactory.createAccount("SAVINGS", 26003, "Delete Me", 40, 10000, 1);
            accounts.save(disposable);
            check(accounts.exists(26003), "account exists");
            accounts.delete(26003);
            check(!accounts.exists(26003), "account delete");

            FileTransactionRepository transactions = new FileTransactionRepository(transactionsFile.toString(), accounts);
            Transaction first = transaction("FILE-TXN-1", 26001, TransactionType.DEPOSIT, 10, 60010, 0, 0, "cash, counter");
            Transaction second = transaction("FILE-TXN-2", 26002, TransactionType.WITHDRAW, 20, 39980, 0, 0, "branch \"A\"");
            transactions.save(first); transactions.save(second);
            check(transactions.findAll().size() == 2, "transaction list");
            check(transactions.findByAccount(26002).size() == 1, "transaction account filter");
            check(CsvMapper.fromCsvTransaction(CsvMapper.toCsv(first)).getDescription().equals("cash, counter"), "transaction CSV escaping");

            Transaction transfer = transactions.transfer(26001, 26002, 5000);
            check(transfer.getType() == TransactionType.TRANSFER, "transfer audit record type");
            check(accounts.findById(26001).getBalance() == 55000, "transfer sender balance");
            check(accounts.findById(26002).getBalance() == 45000, "transfer receiver balance");
            check(transactions.findByAccount(26002).size() == 2, "transfer is visible in destination history");

            double before = accounts.findById(26001).getBalance();
            int ledgerBefore = transactions.findAll().size();
            expectFailure(() -> transactions.transfer(26001, 26002, 999999), "insufficient funds rejected");
            check(accounts.findById(26001).getBalance() == before, "failed transfer keeps sender unchanged");
            check(transactions.findAll().size() == ledgerBefore, "failed transfer does not write audit row");

            FileAccountRepository restartedAccounts = new FileAccountRepository(accountsFile.toString());
            FileTransactionRepository restartedTransactions = new FileTransactionRepository(transactionsFile.toString(), restartedAccounts);
            check(restartedAccounts.findById(26001).getBalance() == 55000, "accounts survive repository restart");
            check(restartedTransactions.findByAccount(26001).size() == 2, "transactions survive repository restart");

            List<Path> accountBackups = backupsFor(accountsFile);
            check(!accountBackups.isEmpty() && accountBackups.size() <= 5, "account backups created and retained");
            for (int i = 0; i < 5; i++) BackupManager.backup(accountsFile.toString(), 2);
            check(backupsFor(accountsFile).size() == 2, "backup retention prunes oldest copies");

            transactions.clear();
            check(transactions.findAll().isEmpty(), "clear transaction ledger");
            check(Files.readAllLines(transactionsFile, StandardCharsets.UTF_8).size() == 1, "clear persists header only");
            System.out.println("All Activity 26 file repository checks passed.");
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).collect(Collectors.toList())) Files.deleteIfExists(path);
            }
        }
    }

    private static Transaction transaction(String id, int account, TransactionType type, double amount,
                                           double balance, int from, int to, String description) {
        return new Transaction(id, LocalDateTime.now(), account, type, amount, balance, "SUCCESS", description, from, to);
    }
    private static List<Path> backupsFor(Path file) throws Exception {
        try (java.util.stream.Stream<Path> paths = Files.list(file.getParent())) {
            return paths.filter(path -> path.getFileName().toString().startsWith(file.getFileName() + ".bak.")).collect(Collectors.toList());
        }
    }
    private static void expectFailure(Runnable action, String label) {
        try { action.run(); throw new AssertionError("Expected failure: " + label); }
        catch (RuntimeException expected) { }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("Failed: " + message);
    }
}
