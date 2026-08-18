package com.sahilkalgutkar.risksignal.riskscoring.rules;

import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.riskscoring.RiskProperties;
import org.springframework.stereotype.Component;

/** Flags merchant countries on the configured watchlist. Illustrative default list, not a compliance feed. */
@Component
public class HighRiskCountryRule implements RiskRule {

    private final RiskProperties properties;

    public HighRiskCountryRule(RiskProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleOutcome evaluate(TransactionSubmitted transaction) {
        if (properties.highRiskCountries().contains(transaction.merchantCountry())) {
            return new RuleOutcome(properties.highRiskCountryScore(), "high-risk-country");
        }
        return RuleOutcome.notTriggered();
    }
}
