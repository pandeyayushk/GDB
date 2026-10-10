CREATE TABLE IF NOT EXISTS accounts (
    account_number INTEGER PRIMARY KEY,
    account_type TEXT NOT NULL,
    holder_name TEXT NOT NULL,
    age INTEGER NOT NULL,
    balance REAL NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    opening_date TEXT
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id TEXT PRIMARY KEY,
    timestamp TEXT NOT NULL,
    account_number INTEGER NOT NULL,
    transaction_type TEXT NOT NULL,
    amount REAL NOT NULL,
    balance_after REAL NOT NULL,
    status TEXT NOT NULL,
    description TEXT,
    from_account INTEGER NOT NULL DEFAULT 0,
    to_account INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
);

CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_number);
