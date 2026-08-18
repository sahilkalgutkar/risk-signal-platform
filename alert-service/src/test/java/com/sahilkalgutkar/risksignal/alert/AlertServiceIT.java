package com.sahilkalgutkar.risksignal.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahilkalgutkar.risksignal.alert.domain.AlertEntity;
import com.sahilkalgutkar.risksignal.alert.domain.AlertRepository;
import com.sahilkalgutkar.risksignal.alert.notification.NotificationDispatcher;
import com.sahilkalgutkar.risksignal.common.events.RiskLevel;
import com.sahilkalgutkar.risksignal.common.events.RiskScored;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doThrow;

@Testcontainers
@SpringBootTest(classes = AlertServiceApplication.class)
class AlertServiceIT {

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
        registry.add("alert.retry-attempts", () -> "2");
        registry.add("alert.retry-backoff-ms", () -> "100");
    }

    @Autowired
    private AlertRepository alertRepository;

    @SpyBean
    private NotificationDispatcher notificationDispatcher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaProducer<String, String> producer;

    @BeforeEach
    void setUpProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);
    }

    @AfterEach
    void tearDownProducer() {
        producer.close();
    }

    @Test
    void highRiskEventIsPersistedAndNotified() throws Exception {
        RiskScored event = new RiskScored("txn-alert-1", "acct-1", 90, RiskLevel.HIGH,
                List.of("amount-threshold", "country-mismatch"));

        producer.send(new ProducerRecord<>("risk-scores", event.transactionId(),
                objectMapper.writeValueAsString(event))).get();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Optional<AlertEntity> alert = alertRepository.findById("txn-alert-1");
            assertThat(alert).isPresent();
            assertThat(alert.get().isNotified()).isTrue();
        });
    }

    @Test
    void lowRiskEventIsIgnored() throws Exception {
        RiskScored event = new RiskScored("txn-alert-2", "acct-1", 10, RiskLevel.LOW, List.of());

        producer.send(new ProducerRecord<>("risk-scores", event.transactionId(),
                objectMapper.writeValueAsString(event))).get();

        // Give the listener a beat to (not) process it, then confirm no alert row was created.
        Thread.sleep(2000);
        assertThat(alertRepository.findById("txn-alert-2")).isEmpty();
    }

    @Test
    void notificationFailureRetriesThenLandsOnTheDeadLetterTopic() throws Exception {
        doThrow(new RuntimeException("paging provider unreachable"))
                .when(notificationDispatcher).dispatch(org.mockito.ArgumentMatchers.any());

        RiskScored event = new RiskScored("txn-alert-dlt", "acct-1", 95, RiskLevel.HIGH,
                List.of("amount-threshold"));

        try (KafkaConsumer<String, String> dltConsumer = dltConsumer()) {
            producer.send(new ProducerRecord<>("risk-scores", event.transactionId(),
                    objectMapper.writeValueAsString(event))).get();

            ConsumerRecords<String, String> records = poll(dltConsumer, Duration.ofSeconds(30));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            assertThat(records.iterator().next().value()).contains("txn-alert-dlt");
        }
    }

    private KafkaConsumer<String, String> dltConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlt-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("risk-scores-dlt"));
        return consumer;
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
