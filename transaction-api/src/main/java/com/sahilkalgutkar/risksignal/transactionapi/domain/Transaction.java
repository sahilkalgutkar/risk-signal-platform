package com.sahilkalgutkar.risksignal.transactionapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "merchant_country", nullable = false, length = 2)
    private String merchantCountry;

    @Column(name = "account_country", nullable = false, length = 2)
    private String accountCountry;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "event_published", nullable = false)
    private boolean eventPublished;

    protected Transaction() {
        // JPA
    }

    public Transaction(String id, String accountId, BigDecimal amount, String currency,
                        String merchantCountry, String accountCountry, Instant submittedAt) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.merchantCountry = merchantCountry;
        this.accountCountry = accountCountry;
        this.submittedAt = submittedAt;
        this.eventPublished = false;
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMerchantCountry() {
        return merchantCountry;
    }

    public String getAccountCountry() {
        return accountCountry;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public boolean isEventPublished() {
        return eventPublished;
    }

    public void markEventPublished() {
        this.eventPublished = true;
    }
}
