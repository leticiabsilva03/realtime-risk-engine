package com.fraudengine.infrastructure.cache;

import java.time.Instant;

public record LastTransactionLocation(
        String country,
        Instant transactionAt
) {}