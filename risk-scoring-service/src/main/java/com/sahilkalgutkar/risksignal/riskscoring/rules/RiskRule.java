package com.sahilkalgutkar.risksignal.riskscoring.rules;

import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;

/** One deterministic scoring signal. Spring collects every bean of this type into the engine. */
public interface RiskRule {

    RuleOutcome evaluate(TransactionSubmitted transaction);

    record RuleOutcome(int score, String reason) {

        public static RuleOutcome notTriggered() {
            return new RuleOutcome(0, null);
        }

        public boolean triggered() {
            return reason != null;
        }
    }
}
