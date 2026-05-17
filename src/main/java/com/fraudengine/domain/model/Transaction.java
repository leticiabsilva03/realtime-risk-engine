package com.fraudengine.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        String transactionId,
        String userId,
        String deviceId,        // nullable — device pode ser desconhecido
        BigDecimal amount,
        String merchantId,      // nullable
        String merchant,        // nullable — nome legível do merchant
        String country,         // nullable — código ISO 3166-1 alpha-2 (ex: "BR", "NG")
        Instant transactionAt
) {}