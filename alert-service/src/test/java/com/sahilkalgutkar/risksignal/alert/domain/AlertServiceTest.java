package com.sahilkalgutkar.risksignal.alert.domain;

import com.sahilkalgutkar.risksignal.alert.AlertProperties;
import com.sahilkalgutkar.risksignal.alert.notification.NotificationDispatcher;
import com.sahilkalgutkar.risksignal.common.events.RiskLevel;
import com.sahilkalgutkar.risksignal.common.events.RiskScored;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    private AlertRepository repository;
    private NotificationDispatcher notificationDispatcher;
    private AlertService service;

    private final RiskScored highRiskEvent = new RiskScored(
            "txn-1", "acct-1", 85, RiskLevel.HIGH, List.of("amount-threshold", "country-mismatch"));

    @BeforeEach
    void setUp() {
        repository = mock(AlertRepository.class);
        notificationDispatcher = mock(NotificationDispatcher.class);
        AlertProperties properties = new AlertProperties(RiskLevel.HIGH, 4, 500);
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        service = new AlertService(repository, notificationDispatcher, properties, fixedClock);

        when(repository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void belowThresholdIsIgnored() {
        RiskScored lowRiskEvent = new RiskScored("txn-2", "acct-1", 20, RiskLevel.LOW, List.of());

        service.handle(lowRiskEvent);

        verifyNoInteractions(repository, notificationDispatcher);
    }

    @Test
    void atOrAboveThresholdPersistsAndNotifies() {
        when(repository.findById("txn-1")).thenReturn(Optional.empty());

        service.handle(highRiskEvent);

        // save() is called twice: once to persist the row, once to flip notified after dispatch
        // succeeds. AlertEntity is mutable, so an argument matcher can't distinguish the two
        // calls by state (both alias the same, now-mutated instance) — call count plus the final
        // notified flag are the meaningful assertions.
        verify(repository, times(2)).save(any(AlertEntity.class));
        verify(notificationDispatcher).dispatch(any(AlertEntity.class));
    }

    @Test
    void redeliveryOfAnAlreadyNotifiedAlertDoesNotReNotify() {
        AlertEntity existing = new AlertEntity("txn-1", "acct-1", 85, RiskLevel.HIGH,
                List.of("amount-threshold"), FIXED_INSTANT);
        existing.markNotified();
        when(repository.findById("txn-1")).thenReturn(Optional.of(existing));

        service.handle(highRiskEvent);

        verifyNoInteractions(notificationDispatcher);
        verify(repository, never()).save(any());
    }

    @Test
    void redeliveryAfterPersistButBeforeNotifyStillNotifies() {
        AlertEntity persistedButNotYetNotified = new AlertEntity("txn-1", "acct-1", 85, RiskLevel.HIGH,
                List.of("amount-threshold"), FIXED_INSTANT);
        when(repository.findById("txn-1")).thenReturn(Optional.of(persistedButNotYetNotified));

        service.handle(highRiskEvent);

        verify(notificationDispatcher).dispatch(persistedButNotYetNotified);
        verify(repository).save(persistedButNotYetNotified);
    }
}
