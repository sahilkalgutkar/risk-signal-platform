CREATE TABLE alerts (
    transaction_id VARCHAR(36)  NOT NULL PRIMARY KEY,
    account_id     VARCHAR(64)  NOT NULL,
    score          INT          NOT NULL,
    level          VARCHAR(10)  NOT NULL,
    reasons        VARCHAR(512) NOT NULL,
    alerted_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    notified       BOOLEAN      NOT NULL DEFAULT FALSE
) ENGINE = InnoDB;

CREATE INDEX idx_alerts_account_id ON alerts (account_id);
