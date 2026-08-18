package com.sahilkalgutkar.risksignal.common.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published to {@link KafkaTopics#TRANSACTIONS} by transaction-api when a transaction is recorded.
 *
 * @param transactionId   ID of the persisted transaction row; also the Kafka message key so all
 *                        events for one transaction land on the same partition
 * @param accountId       account the transaction is posted against
 * @param amount          transaction amount, always positive
 * @param currency        ISO 4217 currency code
 * @param merchantCountry ISO 3166-1 alpha-2 country the merchant is registered in
 * @param accountCountry  ISO 3166-1 alpha-2 country the account was opened in
 * @param submittedAt     when transaction-api accepted the request
 */
public record TransactionSubmitted(
        String transactionId,
        String accountId,
        BigDecimal amount,
        String currency,
        String merchantCountry,
        String accountCountry,
        Instant submittedAt
) {
}
