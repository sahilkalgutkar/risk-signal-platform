package com.sahilkalgutkar.risksignal.riskscoring.rules;

import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.riskscoring.RiskProperties;
import org.springframework.stereotype.Component;

@Component
public class CountryMismatchRule implements RiskRule {

    private final RiskProperties properties;

    public CountryMismatchRule(RiskProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleOutcome evaluate(TransactionSubmitted transaction) {
        if (!transaction.merchantCountry().equals(transaction.accountCountry())) {
            return new RuleOutcome(properties.countryMismatchScore(), "country-mismatch");
        }
        return RuleOutcome.notTriggered();
    }
}
