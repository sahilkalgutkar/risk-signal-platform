package com.sahilkalgutkar.risksignal.riskscoring;

import com.sahilkalgutkar.risksignal.common.events.RiskLevel;
import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.riskscoring.rules.AmountThresholdRule;
import com.sahilkalgutkar.risksignal.riskscoring.rules.CountryMismatchRule;
import com.sahilkalgutkar.risksignal.riskscoring.rules.HighRiskCountryRule;
import com.sahilkalgutkar.risksignal.riskscoring.rules.RiskRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RiskEngineTest {

    private final RiskProperties properties = new RiskProperties(
            new BigDecimal("1000.00"), 40, 35, Set.of("KP", "IR", "SY"), 30, 40, 70);

    private final RiskEngine engine = new RiskEngine(
            List.of(new AmountThresholdRule(properties), new CountryMismatchRule(properties),
                    new HighRiskCountryRule(properties)),
            properties);

    private TransactionSubmitted transaction(String amount, String merchantCountry, String accountCountry) {
        return new TransactionSubmitted("txn-1", "acct-1", new BigDecimal(amount), "USD",
                merchantCountry, accountCountry, Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void cleanTransactionScoresLow() {
        RiskEngine.Result result = engine.score(transaction("50.00", "US", "US"));

        assertThat(result.score()).isZero();
        assertThat(result.level()).isEqualTo(RiskLevel.LOW);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void largeAmountAloneScoresMedium() {
        RiskEngine.Result result = engine.score(transaction("5000.00", "US", "US"));

        assertThat(result.score()).isEqualTo(40);
        assertThat(result.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.reasons()).containsExactly("amount-threshold");
    }

    @Test
    void largeAmountPlusCountryMismatchScoresHigh() {
        RiskEngine.Result result = engine.score(transaction("5000.00", "GB", "US"));

        assertThat(result.score()).isEqualTo(75);
        assertThat(result.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.reasons()).containsExactlyInAnyOrder("amount-threshold", "country-mismatch");
    }

    @Test
    void allSignalsTriggeredCapsScoreAt100() {
        RiskEngine.Result result = engine.score(transaction("5000.00", "KP", "US"));

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.reasons()).containsExactlyInAnyOrder(
                "amount-threshold", "country-mismatch", "high-risk-country");
    }

    @Test
    void amountThresholdRuleIsExclusiveOnBoundary() {
        RiskRule.RuleOutcome atThreshold = new AmountThresholdRule(properties)
                .evaluate(transaction("1000.00", "US", "US"));

        assertThat(atThreshold.triggered()).isFalse();
    }
}
