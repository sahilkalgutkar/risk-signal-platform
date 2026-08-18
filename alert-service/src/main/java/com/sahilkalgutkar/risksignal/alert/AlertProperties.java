package com.sahilkalgutkar.risksignal.alert;

import com.sahilkalgutkar.risksignal.common.events.RiskLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alert")
public record AlertProperties(RiskLevel minLevel, int retryAttempts, long retryBackoffMs) {
}
