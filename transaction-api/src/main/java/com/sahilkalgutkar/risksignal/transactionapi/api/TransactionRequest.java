package com.sahilkalgutkar.risksignal.transactionapi.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank String accountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 code") String currency,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "must be a 2-letter ISO 3166-1 alpha-2 code") String merchantCountry,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "must be a 2-letter ISO 3166-1 alpha-2 code") String accountCountry
) {
}
