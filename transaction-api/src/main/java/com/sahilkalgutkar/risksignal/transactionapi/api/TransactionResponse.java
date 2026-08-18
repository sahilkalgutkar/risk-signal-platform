package com.sahilkalgutkar.risksignal.transactionapi.api;

import com.sahilkalgutkar.risksignal.transactionapi.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String id,
        String accountId,
        BigDecimal amount,
        String currency,
        String merchantCountry,
        String accountCountry,
        Instant submittedAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getMerchantCountry(),
                transaction.getAccountCountry(),
                transaction.getSubmittedAt());
    }
}
