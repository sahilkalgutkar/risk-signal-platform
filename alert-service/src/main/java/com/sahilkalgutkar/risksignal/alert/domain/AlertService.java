package com.sahilkalgutkar.risksignal.alert.domain;

import com.sahilkalgutkar.risksignal.alert.AlertProperties;
import com.sahilkalgutkar.risksignal.alert.notification.NotificationDispatcher;
import com.sahilkalgutkar.risksignal.common.events.RiskScored;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Idempotent per transactionId, the same way transaction-api and risk-scoring-service are: a
 * retried delivery (Kafka rebalance, or a redelivery from {@code @RetryableTopic} after this
 * method itself throws) finds the alert already persisted and only re-attempts the notification.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository repository;
    private final NotificationDispatcher notificationDispatcher;
    private final AlertProperties properties;
    private final Clock clock;

    public AlertService(AlertRepository repository, NotificationDispatcher notificationDispatcher,
                         AlertProperties properties, Clock clock) {
        this.repository = repository;
        this.notificationDispatcher = notificationDispatcher;
        this.properties = properties;
        this.clock = clock;
    }

    public void handle(RiskScored event) {
        if (event.level().compareTo(properties.minLevel()) < 0) {
            log.debug("Transaction {} scored {} ({}), below alert threshold {}",
                    event.transactionId(), event.score(), event.level(), properties.minLevel());
            return;
        }

        AlertEntity alert = repository.findById(event.transactionId())
                .orElseGet(() -> persist(event));

        if (!alert.isNotified()) {
            notificationDispatcher.dispatch(alert);
            alert.markNotified();
            repository.save(alert);
        }
    }

    @Transactional
    protected AlertEntity persist(RiskScored event) {
        AlertEntity alert = new AlertEntity(
                event.transactionId(), event.accountId(), event.score(), event.level(),
                event.reasons(), Instant.now(clock));
        return repository.save(alert);
    }
}
