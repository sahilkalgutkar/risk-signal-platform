package com.sahilkalgutkar.risksignal.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void transactionSubmittedRoundTripsThroughJson() throws Exception {
        TransactionSubmitted original = new TransactionSubmitted(
                "txn-1", "acct-1", new BigDecimal("125.50"), "USD", "US", "US", Instant.parse("2026-01-01T00:00:00Z"));

        String json = mapper.writeValueAsString(original);
        TransactionSubmitted roundTripped = mapper.readValue(json, TransactionSubmitted.class);

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void riskScoredRoundTripsThroughJson() throws Exception {
        RiskScored original = new RiskScored(
                "txn-1", "acct-1", 82, RiskLevel.HIGH, List.of("amount-threshold", "country-mismatch"));

        String json = mapper.writeValueAsString(original);
        RiskScored roundTripped = mapper.readValue(json, RiskScored.class);

        assertThat(roundTripped).isEqualTo(original);
    }
}
