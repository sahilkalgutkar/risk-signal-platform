package com.sahilkalgutkar.risksignal.transactionapi.domain;

import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.transactionapi.api.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import static com.sahilkalgutkar.risksignal.common.events.KafkaTopics.TRANSACTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    private TransactionRepository repository;
    private KafkaTemplate<String, TransactionSubmitted> kafkaTemplate;
    private TransactionService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(TransactionRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        service = new TransactionService(repository, kafkaTemplate, fixedClock);

        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitPersistsThenPublishesAndMarksEventPublished() {
        TransactionRequest request = new TransactionRequest("acct-1", new BigDecimal("50.00"), "USD", "US", "US");
        SendResult<String, TransactionSubmitted> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(eq(TRANSACTIONS), anyString(), any(TransactionSubmitted.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        Transaction result = service.submit(request);

        assertThat(result.getAccountId()).isEqualTo("acct-1");
        assertThat(result.getSubmittedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(result.isEventPublished()).isTrue();

        // save() is called twice: once to persist the row, once to flip event_published after
        // the Kafka send succeeds. Transaction is mutable, so an ArgumentCaptor can't tell the
        // two calls apart by state (both captures alias the same, now-mutated instance) — the
        // call count is the meaningful assertion here.
        verify(repository, times(2)).save(any(Transaction.class));
        verify(kafkaTemplate).send(eq(TRANSACTIONS), eq(result.getId()), any(TransactionSubmitted.class));
    }

    @Test
    void submitStaysDurableWhenKafkaPublishFails() {
        TransactionRequest request = new TransactionRequest("acct-1", new BigDecimal("50.00"), "USD", "US", "US");
        CompletableFuture<SendResult<String, TransactionSubmitted>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unreachable"));
        when(kafkaTemplate.send(eq(TRANSACTIONS), anyString(), any(TransactionSubmitted.class)))
                .thenReturn(failed);

        Transaction result = service.submit(request);

        assertThat(result.isEventPublished()).isFalse();
        verify(repository, times(1)).save(any(Transaction.class));
    }
}
