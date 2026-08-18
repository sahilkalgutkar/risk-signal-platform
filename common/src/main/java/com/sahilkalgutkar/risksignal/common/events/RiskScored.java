package com.sahilkalgutkar.risksignal.common.events;

import java.util.List;

/**
 * Published to {@link KafkaTopics#RISK_SCORES} by risk-scoring-service once a transaction has
 * been scored. alert-service consumes this to decide whether to raise an alert.
 *
 * @param transactionId ID of the scored transaction; carried through as the Kafka message key
 * @param accountId     account the transaction was posted against, so alert-service doesn't
 *                       need to look it back up
 * @param score         0-100, higher is riskier
 * @param level         bucketed severity derived from {@code score}
 * @param reasons       human-readable rule names that fired, for the alert payload and for
 *                       operators triaging in Kibana
 */
public record RiskScored(
        String transactionId,
        String accountId,
        int score,
        RiskLevel level,
        List<String> reasons
) {
}
