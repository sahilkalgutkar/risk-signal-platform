package com.sahilkalgutkar.risksignal.alert.notification;

import com.sahilkalgutkar.risksignal.alert.domain.AlertEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stands in for a real paging integration (PagerDuty, Slack, SES) — logs loudly instead. */
@Component
public class LoggingNotificationDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationDispatcher.class);

    @Override
    public void dispatch(AlertEntity alert) {
        log.warn("ALERT transaction={} account={} score={} level={} reasons={}",
                alert.getTransactionId(), alert.getAccountId(), alert.getScore(), alert.getLevel(),
                alert.getReasons());
    }
}
