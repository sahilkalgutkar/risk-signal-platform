package com.sahilkalgutkar.risksignal.riskscoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class RiskScoringServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskScoringServiceApplication.class, args);
    }
}
