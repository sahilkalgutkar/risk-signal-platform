package com.sahilkalgutkar.risksignal.riskscoring;

import com.sahilkalgutkar.risksignal.common.events.RiskLevel;
import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.riskscoring.rules.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RiskEngine {

    private final List<RiskRule> rules;
    private final RiskProperties properties;

    public RiskEngine(List<RiskRule> rules, RiskProperties properties) {
        this.rules = rules;
        this.properties = properties;
    }

    public Result score(TransactionSubmitted transaction) {
        int totalScore = 0;
        List<String> reasons = new java.util.ArrayList<>();

        for (RiskRule rule : rules) {
            RiskRule.RuleOutcome outcome = rule.evaluate(transaction);
            if (outcome.triggered()) {
                totalScore += outcome.score();
                reasons.add(outcome.reason());
            }
        }

        int cappedScore = Math.min(totalScore, 100);
        return new Result(cappedScore, levelFor(cappedScore), List.copyOf(reasons));
    }

    private RiskLevel levelFor(int score) {
        if (score >= properties.highThreshold()) {
            return RiskLevel.HIGH;
        }
        if (score >= properties.mediumThreshold()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public record Result(int score, RiskLevel level, List<String> reasons) {
    }
}
