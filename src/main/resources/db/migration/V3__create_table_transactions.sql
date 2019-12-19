CREATE TABLE transactions(
    id VARCHAR(128) NOT NULL PRIMARY KEY,
    amount DECIMAL(15, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    timestamp TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,
    from_account VARCHAR(128) NOT NULL,
    to_account VARCHAR(128) NOT NULL,

    FOREIGN KEY (from_account) references accounts(id),
    FOREIGN KEY (to_account) references accounts(id)
)
