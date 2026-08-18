package com.sahilkalgutkar.risksignal.riskscoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sahilkalgutkar.risksignal.common.events.RiskScored;
import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = RiskScoringServiceApplication.class)
class RiskScoringServiceIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("risksignal")
            .withUsername("risksignal")
            .withPassword("risksignal");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.1"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private KafkaProducer<String, String> producer;
    private KafkaConsumer<String, String> consumer;

    @BeforeEach
    void setUpClients() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(producerProps);

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + System.nanoTime());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList("risk-scores"));
    }

    @AfterEach
    void tearDownClients() {
        producer.close();
        consumer.close();
    }

    @Test
    void consumingATransactionProducesARiskScoredEvent() throws Exception {
        TransactionSubmitted transaction = new TransactionSubmitted(
                "txn-it-1", "acct-it-1", new BigDecimal("5000.00"), "USD", "KP", "US",
                Instant.parse("2026-01-01T00:00:00Z"));

        producer.send(new ProducerRecord<>("transactions", transaction.transactionId(),
                objectMapper.writeValueAsString(transaction))).get();

        ConsumerRecords<String, String> records = poll(consumer, Duration.ofSeconds(20));
        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        RiskScored event = objectMapper.readValue(records.iterator().next().value(), RiskScored.class);
        assertThat(event.transactionId()).isEqualTo("txn-it-1");
        assertThat(event.score()).isEqualTo(100);
        assertThat(event.reasons()).containsExactlyInAnyOrder(
                "amount-threshold", "country-mismatch", "high-risk-country");
    }

    private static ConsumerRecords<String, String> poll(KafkaConsumer<String, String> consumer, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records;
            }
        }
        return ConsumerRecords.empty();
    }
}
