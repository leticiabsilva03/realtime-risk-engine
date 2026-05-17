package com.fraudengine.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(

        @NotBlank(message = "transactionId is required")
        String transactionId,

        @NotBlank(message = "userId is required")
        String userId,

        String deviceId,        // nullable

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        String merchantId,      // nullable
        String merchant,        // nullable
        String country,         // nullable

        @NotNull(message = "transactionAt is required")
        Instant transactionAt

) {}