package com.sahilkalgutkar.risksignal.alert.domain;

import com.sahilkalgutkar.risksignal.common.events.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    @Column(name = "score", nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 10)
    private RiskLevel level;

    @Column(name = "reasons", nullable = false, length = 512)
    private String reasons;

    @Column(name = "alerted_at", nullable = false)
    private Instant alertedAt;

    @Column(name = "notified", nullable = false)
    private boolean notified;

    protected AlertEntity() {
        // JPA
    }

    public AlertEntity(String transactionId, String accountId, int score, RiskLevel level,
                        List<String> reasons, Instant alertedAt) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.score = score;
        this.level = level;
        this.reasons = String.join(",", reasons);
        this.alertedAt = alertedAt;
        this.notified = false;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public int getScore() {
        return score;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public List<String> getReasons() {
        return reasons.isEmpty() ? List.of() : List.of(reasons.split(","));
    }

    public Instant getAlertedAt() {
        return alertedAt;
    }

    public boolean isNotified() {
        return notified;
    }

    public void markNotified() {
        this.notified = true;
    }
}
