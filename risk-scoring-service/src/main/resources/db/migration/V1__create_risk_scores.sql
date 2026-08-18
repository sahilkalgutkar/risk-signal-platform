CREATE TABLE risk_scores (
    transaction_id  VARCHAR(36)  NOT NULL PRIMARY KEY,
    account_id      VARCHAR(64)  NOT NULL,
    score           INT          NOT NULL,
    level           VARCHAR(10)  NOT NULL,
    reasons         VARCHAR(512) NOT NULL,
    scored_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    event_published BOOLEAN      NOT NULL DEFAULT FALSE
) ENGINE = InnoDB;

CREATE INDEX idx_risk_scores_account_id ON risk_scores (account_id);
CREATE INDEX idx_risk_scores_level ON risk_scores (level);
