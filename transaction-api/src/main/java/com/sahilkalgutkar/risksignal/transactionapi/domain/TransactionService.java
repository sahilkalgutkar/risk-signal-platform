package com.sahilkalgutkar.risksignal.transactionapi.domain;

import com.sahilkalgutkar.risksignal.common.events.KafkaTopics;
import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.transactionapi.api.TransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The transaction row is always durable before we touch Kafka. If the publish fails, the row is
 * left with {@code eventPublished=false} and the request still succeeds — a reconciliation job
 * (not implemented here) would be the production-grade way to sweep up and republish those rows.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository repository;
    private final KafkaTemplate<String, TransactionSubmitted> kafkaTemplate;
    private final Clock clock;

    public TransactionService(TransactionRepository repository,
                               KafkaTemplate<String, TransactionSubmitted> kafkaTemplate,
                               Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Transactional
    public Transaction submit(TransactionRequest request) {
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                request.accountId(),
                request.amount(),
                request.currency(),
                request.merchantCountry(),
                request.accountCountry(),
                Instant.now(clock));

        Transaction saved = repository.save(transaction);
        publishBestEffort(saved);
        return saved;
    }

    public Optional<Transaction> findById(String id) {
        return repository.findById(id);
    }

    private void publishBestEffort(Transaction transaction) {
        TransactionSubmitted event = new TransactionSubmitted(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getMerchantCountry(),
                transaction.getAccountCountry(),
                transaction.getSubmittedAt());

        try {
            kafkaTemplate.send(KafkaTopics.TRANSACTIONS, transaction.getId(), event).get();
            transaction.markEventPublished();
            repository.save(transaction);
        } catch (Exception e) {
            log.error("Failed to publish TransactionSubmitted for transaction {}; " +
                    "row is durable but event_published stays false", transaction.getId(), e);
        }
    }
}
