package com.sahilkalgutkar.risksignal.riskscoring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Set;

@ConfigurationProperties(prefix = "risk")
public record RiskProperties(
        BigDecimal amountThreshold,
        int amountThresholdScore,
        int countryMismatchScore,
        Set<String> highRiskCountries,
        int highRiskCountryScore,
        int mediumThreshold,
        int highThreshold
) {
}
