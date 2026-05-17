package com.fraudengine.api.dto;

import java.time.Instant;
import java.util.List;

public record EvaluationResponse(
        String transactionId,
        int totalScore,
        String decision,
        String rulesVersion,
        boolean simulated,
        List<RuleResultResponse> rulesEvaluated,
        Instant evaluatedAt
) {}