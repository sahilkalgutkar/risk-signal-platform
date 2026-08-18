package com.sahilkalgutkar.risksignal.alert.kafka;

import com.sahilkalgutkar.risksignal.alert.domain.AlertService;
import com.sahilkalgutkar.risksignal.common.events.RiskScored;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import static com.sahilkalgutkar.risksignal.common.events.KafkaTopics.RISK_SCORES;

/**
 * A failed notification dispatch (see {@link AlertService#handle}) retries a few times with
 * backoff on dedicated retry topics ({@code risk-scores-retry-0}, {@code -1}, ...) before landing
 * on {@code risk-scores-dlt}, instead of blocking the partition or silently dropping the alert.
 */
@Component
public class RiskScoredListener {

    private static final Logger log = LoggerFactory.getLogger(RiskScoredListener.class);

    private final AlertService alertService;

    public RiskScoredListener(AlertService alertService) {
        this.alertService = alertService;
    }

    @RetryableTopic(
            attempts = "${alert.retry-attempts}",
            backoff = @Backoff(delayExpression = "${alert.retry-backoff-ms}", multiplierExpression = "2"),
            autoCreateTopics = "true")
    @KafkaListener(topics = RISK_SCORES, groupId = "${spring.kafka.consumer.group-id}")
    public void onRiskScored(RiskScored event) {
        alertService.handle(event);
    }

    @DltHandler
    public void onDeadLetter(RiskScored event, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        log.error("Giving up on transaction {} after exhausting retries: {}",
                event.transactionId(), exceptionMessage);
    }
}
