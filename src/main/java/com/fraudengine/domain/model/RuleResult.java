package com.fraudengine.domain.model;

public record RuleResult(
        String ruleName,
        int score,
        boolean triggered,
        String reason          // null quando triggered=false
) {


    public static RuleResult triggered(String ruleName, int score, String reason) {
        return new RuleResult(ruleName, score, true, reason);
    }

    public static RuleResult notTriggered(String ruleName) {
        return new RuleResult(ruleName, 0, false, null);
    }

    public static RuleResult skippedDueToCacheUnavailability(String ruleName) {
        return new RuleResult(ruleName, 0, false, "cache unavailable — rule skipped");
    }
}