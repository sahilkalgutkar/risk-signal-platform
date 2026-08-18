package com.sahilkalgutkar.risksignal.riskscoring.kafka;

import com.sahilkalgutkar.risksignal.common.events.RiskScored;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, RiskScored> riskScoredProducerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null));
    }

    @Bean
    public KafkaTemplate<String, RiskScored> riskScoredKafkaTemplate(
            ProducerFactory<String, RiskScored> riskScoredProducerFactory) {
        return new KafkaTemplate<>(riskScoredProducerFactory);
    }
}
