package com.fraudengine.api.dto;

public record RuleResultResponse(
        String rule,
        int score,
        boolean triggered,
        String reason
) {}