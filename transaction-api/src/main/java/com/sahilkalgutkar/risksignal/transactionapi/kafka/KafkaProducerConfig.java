package com.sahilkalgutkar.risksignal.transactionapi.kafka;

import com.sahilkalgutkar.risksignal.common.events.TransactionSubmitted;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, TransactionSubmitted> transactionProducerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null));
    }

    @Bean
    public KafkaTemplate<String, TransactionSubmitted> transactionKafkaTemplate(
            ProducerFactory<String, TransactionSubmitted> transactionProducerFactory) {
        return new KafkaTemplate<>(transactionProducerFactory);
    }
}
