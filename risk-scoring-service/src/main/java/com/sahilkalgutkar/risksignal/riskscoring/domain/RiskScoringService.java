package com.sahilkalgutkar.risksignal.riskscoring.domain;

import com.sahilkalgutkar.risksignal.common.events.KafkaTopics;
import com.sahilkalgutkar.risksignal.common.events.RiskScored;
import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.riskscoring.RiskEngine;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class RiskScoringService {

    private static final Logger log = LoggerFactory.getLogger(RiskScoringService.class);

    private final RiskEngine riskEngine;
    private final RiskScoreRepository repository;
    private final KafkaTemplate<String, RiskScored> kafkaTemplate;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public RiskScoringService(RiskEngine riskEngine, RiskScoreRepository repository,
                               KafkaTemplate<String, RiskScored> kafkaTemplate, Clock clock,
                               MeterRegistry meterRegistry) {
        this.riskEngine = riskEngine;
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Idempotent per transactionId: a redelivered TransactionSubmitted (consumer restart, rebalance)
     * finds the row already scored and is a no-op rather than double-counting or re-alerting.
     */
    @Transactional
    public void handle(TransactionSubmitted transaction) {
        if (repository.existsById(transaction.transactionId())) {
            log.info("Transaction {} already scored, skipping redelivered event", transaction.transactionId());
            return;
        }

        RiskEngine.Result result = riskEngine.score(transaction);
        RiskScoreEntity entity = new RiskScoreEntity(
                transaction.transactionId(), transaction.accountId(), result.score(), result.level(),
                result.reasons(), Instant.now(clock));

        RiskScoreEntity saved = repository.save(entity);
        meterRegistry.counter("risk_scores_total", "level", result.level().name()).increment();
        publishBestEffort(saved);
    }

    private void publishBestEffort(RiskScoreEntity entity) {
        RiskScored event = new RiskScored(
                entity.getTransactionId(), entity.getAccountId(), entity.getScore(), entity.getLevel(),
                entity.getReasons());

        try {
            kafkaTemplate.send(KafkaTopics.RISK_SCORES, entity.getTransactionId(), event).get();
            entity.markEventPublished();
            repository.save(entity);
        } catch (Exception e) {
            log.error("Failed to publish RiskScored for transaction {}; row is durable but " +
                    "event_published stays false", entity.getTransactionId(), e);
        }
    }
}
