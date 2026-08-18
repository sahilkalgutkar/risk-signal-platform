package com.sahilkalgutkar.risksignal.transactionapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import com.sahilkalgutkar.risksignal.transactionapi.api.TransactionRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionApiIT {

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

    @Autowired
    private TestRestTemplate restTemplate;

    private KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void subscribeToTransactionsTopic() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("transactions"));
    }

    @AfterEach
    void closeConsumer() {
        consumer.close();
    }

    @Test
    void submittingATransactionPersistsItAndPublishesTheEvent() throws Exception {
        TransactionRequest request = new TransactionRequest("acct-42", new BigDecimal("199.99"), "USD", "GB", "US");

        ResponseEntity<String> postResponse = restTemplate.postForEntity("/transactions", request, String.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ConsumerRecords<String, String> records = poll(consumer, Duration.ofSeconds(15));
        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        ConsumerRecord<String, String> record = records.iterator().next();
        TransactionSubmitted event = objectMapper.readValue(record.value(), TransactionSubmitted.class);

        assertThat(event.accountId()).isEqualTo("acct-42");
        assertThat(event.amount()).isEqualByComparingTo("199.99");
        assertThat(event.merchantCountry()).isEqualTo("GB");

        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                "/transactions/" + event.transactionId(), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
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
