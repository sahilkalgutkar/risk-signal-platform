package com.sahilkalgutkar.risksignal.riskscoring.kafka;

import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.riskscoring.domain.RiskScoringService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.sahilkalgutkar.risksignal.common.events.KafkaTopics.TRANSACTIONS;

@Component
public class TransactionListener {

    private final RiskScoringService riskScoringService;

    public TransactionListener(RiskScoringService riskScoringService) {
        this.riskScoringService = riskScoringService;
    }

    @KafkaListener(topics = TRANSACTIONS, groupId = "${spring.kafka.consumer.group-id}")
    public void onTransactionSubmitted(TransactionSubmitted transaction) {
        riskScoringService.handle(transaction);
    }
}
