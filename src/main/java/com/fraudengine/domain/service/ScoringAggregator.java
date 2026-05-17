package com.fraudengine.domain.service;

import com.fraudengine.domain.model.EvaluationDecision;
import com.fraudengine.domain.model.RuleResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoringAggregator {

    public int totalScore(List<RuleResult> results) {
        return results.stream()
                .mapToInt(RuleResult::score)
                .sum();
    }

    public EvaluationDecision decide(int totalScore) {
        return EvaluationDecision.from(totalScore);
    }
}