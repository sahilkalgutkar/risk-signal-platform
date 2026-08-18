package com.sahilkalgutkar.risksignal.alert.notification;

import com.sahilkalgutkar.risksignal.alert.domain.AlertEntity;

/**
 * Sends the actual page/notification for an alert. A real deployment would call PagerDuty,
 * Slack, or SES here; a failure must propagate so the Kafka listener's retry/DLT handling
 * (see {@code RiskScoredListener}) gets a chance to redeliver rather than silently dropping a page.
 */
public interface NotificationDispatcher {

    void dispatch(AlertEntity alert);
}
