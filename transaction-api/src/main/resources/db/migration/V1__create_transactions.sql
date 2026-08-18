CREATE TABLE transactions (
    id                 VARCHAR(36)     NOT NULL PRIMARY KEY,
    account_id         VARCHAR(64)     NOT NULL,
    amount             DECIMAL(19, 4)  NOT NULL,
    currency           VARCHAR(3)      NOT NULL,
    merchant_country   VARCHAR(2)      NOT NULL,
    account_country    VARCHAR(2)      NOT NULL,
    submitted_at       TIMESTAMP(6)    NOT NULL,
    event_published    BOOLEAN         NOT NULL DEFAULT FALSE
) ENGINE = InnoDB;

CREATE INDEX idx_transactions_account_id ON transactions (account_id);
