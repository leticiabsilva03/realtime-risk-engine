package com.fraudengine.domain.model;

public enum EvaluationDecision {

    APPROVE,
    REVIEW,
    BLOCK;

    public static EvaluationDecision from(int totalScore) {
        if (totalScore < 0 || totalScore > 100) {
            throw new IllegalArgumentException(
                    "Score deve estar entre 0 e 100, mas foi: " + totalScore
            );
        }

        if (totalScore <= 39) {
            return APPROVE;
        } else if (totalScore <= 69) {
            return REVIEW;
        } else {
            return BLOCK;
        }
    }
}