package com.sahilkalgutkar.risksignal.riskscoring.domain;

import com.sahilkalgutkar.risksignal.common.events.RiskLevel;
import com.sahilkalgutkar.risksignal.common.events.RiskScored;
import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.riskscoring.RiskEngine;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.sahilkalgutkar.risksignal.common.events.KafkaTopics.RISK_SCORES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskScoringServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    private RiskEngine riskEngine;
    private RiskScoreRepository repository;
    private KafkaTemplate<String, RiskScored> kafkaTemplate;
    private RiskScoringService service;

    private final TransactionSubmitted transaction = new TransactionSubmitted(
            "txn-1", "acct-1", new BigDecimal("5000.00"), "USD", "GB", "US", FIXED_INSTANT);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        riskEngine = mock(RiskEngine.class);
        repository = mock(RiskScoreRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        service = new RiskScoringService(riskEngine, repository, kafkaTemplate, fixedClock, new SimpleMeterRegistry());

        when(repository.save(any(RiskScoreEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleScoresPersistsAndPublishes() {
        when(repository.existsById("txn-1")).thenReturn(false);
        when(riskEngine.score(transaction))
                .thenReturn(new RiskEngine.Result(75, RiskLevel.HIGH, List.of("amount-threshold", "country-mismatch")));
        SendResult<String, RiskScored> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(eq(RISK_SCORES), anyString(), any(RiskScored.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        service.handle(transaction);

        verify(repository, times(2)).save(any(RiskScoreEntity.class));
        verify(kafkaTemplate).send(eq(RISK_SCORES), eq("txn-1"), any(RiskScored.class));
    }

    @Test
    void handleSkipsAlreadyScoredTransaction() {
        when(repository.existsById("txn-1")).thenReturn(true);

        service.handle(transaction);

        verify(repository, never()).save(any(RiskScoreEntity.class));
        verify(riskEngine, never()).score(any());
    }
}
