CREATE TABLE accounts(
    id VARCHAR(128) NOT NULL PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    balance DECIMAL(13, 4) NOT NULL default 0,

    FOREIGN KEY (user_id) references users(id)
)
