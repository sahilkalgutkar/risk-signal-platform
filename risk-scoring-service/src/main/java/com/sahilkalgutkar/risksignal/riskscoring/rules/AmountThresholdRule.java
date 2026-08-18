package com.sahilkalgutkar.risksignal.riskscoring.rules;

import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.riskscoring.RiskProperties;
import org.springframework.stereotype.Component;

@Component
public class AmountThresholdRule implements RiskRule {

    private final RiskProperties properties;

    public AmountThresholdRule(RiskProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleOutcome evaluate(TransactionSubmitted transaction) {
        if (transaction.amount().compareTo(properties.amountThreshold()) > 0) {
            return new RuleOutcome(properties.amountThresholdScore(), "amount-threshold");
        }
        return RuleOutcome.notTriggered();
    }
}
