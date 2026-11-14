package com.gdb.file;

import com.gdb.domain.Account;
import com.gdb.domain.AccountFactory;
import com.gdb.domain.IAccount;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransactionType;
import com.gdb.exceptions.AccountException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** CSV mapping for persisted account and transaction records. */
public final class CsvMapper {
    public static final String ACCOUNT_HEADER = "account_number,account_holder_name,age,balance,account_type,status,pin,opening_date,tenure_years";
    public static final String TRANSACTION_HEADER = "transaction_id,timestamp,account_number,type,amount,balance_after,status,from_account,to_account,description";

    private CsvMapper() { }

    public static String toCsv(IAccount account) {
        if (account == null) throw new IllegalArgumentException("account cannot be null");
        Account base = account instanceof Account ? (Account)account : null;
        List<String> fields = new ArrayList<>();
        fields.add(Integer.toString(account.getAccountNumber()));
        fields.add(account.getAccountHolderName());
        fields.add(Integer.toString(base == null ? 0 : base.getAge()));
        fields.add(Double.toString(account.getBalance()));
        fields.add(account.getAccountType());
        fields.add(account.isActive() ? "Active" : "Inactive");
        fields.add(base == null || base.getPin() == null ? "" : base.getPin().toString());
        fields.add(account.getOpeningDate());
        fields.add(Integer.toString(account.getTenureYears()));
        return join(fields);
    }

    public static IAccount fromCsv(String line) {
        List<String> fields = parse(line);
        try {
            if (fields.size() < 6) throw new IllegalArgumentException("Account CSV row requires at least 6 fields");
            int number = Integer.parseInt(fields.get(0).trim());
            String holder = fields.get(1);
            int age = Integer.parseInt(fields.get(2).trim());
            double balance = Double.parseDouble(fields.get(3).trim());
            String type = fields.get(4).trim().replace(" ", "");
            // The original archive schema ends with overdraft_used,interest_earned.
            int tenure = fields.size() == 9 && !fields.get(8).trim().isEmpty() ? Integer.parseInt(fields.get(8).trim()) : 0;
            IAccount account = AccountFactory.createAccount(type, number, holder, age, balance, tenure);
            if (account instanceof Account) {
                Account base = (Account)account;
                if (fields.size() >= 7 && !fields.get(6).trim().isEmpty()) base.setPin(Integer.parseInt(fields.get(6).trim()));
                if (fields.size() >= 8 && !fields.get(7).isEmpty()) base.setOpeningDate(fields.get(7));
            }
            if ("inactive".equalsIgnoreCase(fields.get(5).trim())) account.closeAccount();
            return account;
        } catch (AccountException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid account CSV row: " + e.getMessage(), e);
        }
    }

    public static String toCsv(Transaction transaction) {
        if (transaction == null) throw new IllegalArgumentException("transaction cannot be null");
        List<String> fields = new ArrayList<>();
        fields.add(transaction.getTransactionId());
        fields.add((transaction.getTimestamp() == null ? LocalDateTime.now() : transaction.getTimestamp()).toString());
        fields.add(Integer.toString(transaction.getAccountNumber()));
        fields.add(transaction.getType().name());
        fields.add(Double.toString(transaction.getAmount()));
        fields.add(Double.toString(transaction.getBalanceAfter()));
        fields.add(transaction.getStatus());
        fields.add(Integer.toString(transaction.getFromAccount()));
        fields.add(Integer.toString(transaction.getToAccount()));
        fields.add(transaction.getDescription());
        return join(fields);
    }

    public static Transaction fromCsvTransaction(String line) {
        List<String> f = parse(line);
        try {
            if (f.size() < 10) throw new IllegalArgumentException("Transaction CSV row requires 10 fields");
            return new Transaction(f.get(0), LocalDateTime.parse(f.get(1).trim()), Integer.parseInt(f.get(2).trim()),
                    TransactionType.valueOf(f.get(3).trim()), Double.parseDouble(f.get(4).trim()),
                    Double.parseDouble(f.get(5).trim()), f.get(6), f.get(9),
                    Integer.parseInt(f.get(7).trim()), Integer.parseInt(f.get(8).trim()));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid transaction CSV row: " + e.getMessage(), e);
        }
    }

    /** Builds a persisted account snapshot with a changed balance and all supported metadata retained. */
    public static IAccount withBalance(IAccount original, double balance) {
        if (!(original instanceof Account)) throw new IllegalArgumentException("Unsupported account implementation");
        Account source = (Account)original;
        try {
            IAccount replacement = AccountFactory.createAccount(original.getAccountType(), original.getAccountNumber(),
                    original.getAccountHolderName(), source.getAge(), original.getBalance(), original.getTenureYears());
            Account mapped = (Account)replacement;
            mapped.restoreBalance(balance);
            mapped.setOpeningDate(original.getOpeningDate());
            if (source.getPin() != null) mapped.setPin(source.getPin());
            if (!original.isActive()) mapped.closeAccount();
            return replacement;
        } catch (com.gdb.exceptions.AccountException e) {
            throw new IllegalArgumentException("Unable to copy account " + original.getAccountNumber(), e);
        }
    }

    private static String join(List<String> fields) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) result.append(',');
            String field = fields.get(i) == null ? "" : fields.get(i);
            if (field.indexOf(',') >= 0 || field.indexOf('"') >= 0 || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0) {
                result.append('"').append(field.replace("\"", "\"\"")).append('"');
            } else result.append(field);
        }
        return result.toString();
    }

    private static List<String> parse(String line) {
        if (line == null) throw new IllegalArgumentException("CSV row cannot be null");
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { value.append('"'); i++; }
                    else quoted = false;
                } else value.append(ch);
            } else if (ch == ',' ) {
                fields.add(value.toString()); value.setLength(0);
            } else if (ch == '"' && value.length() == 0) quoted = true;
            else value.append(ch);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed quoted CSV field");
        fields.add(value.toString());
        return fields;
    }
}
