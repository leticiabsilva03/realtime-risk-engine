package com.fraudengine.domain.service;

import com.fraudengine.domain.model.EvaluationDecision;
import com.fraudengine.domain.model.RuleResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringAggregatorTest {

    private final ScoringAggregator aggregator = new ScoringAggregator();

    @Test
    void shouldReturnApproveWhenTotalScoreIsBelow40() {
        List<RuleResult> results = List.of(
                RuleResult.triggered("RuleA", 20, "reason"),
                RuleResult.notTriggered("RuleB")
        );

        assertThat(aggregator.totalScore(results)).isEqualTo(20);
        assertThat(aggregator.decide(20)).isEqualTo(EvaluationDecision.APPROVE);
    }

    @Test
    void shouldReturnReviewWhenTotalScoreIsBetween40And69() {
        List<RuleResult> results = List.of(
                RuleResult.triggered("RuleA", 30, "reason A"),
                RuleResult.triggered("RuleB", 25, "reason B")
        );

        assertThat(aggregator.totalScore(results)).isEqualTo(55);
        assertThat(aggregator.decide(55)).isEqualTo(EvaluationDecision.REVIEW);
    }

    @Test
    void shouldReturnBlockWhenTotalScoreIsAtOrAbove70() {
        List<RuleResult> results = List.of(
                RuleResult.triggered("RuleA", 30, "reason A"),
                RuleResult.triggered("RuleB", 25, "reason B"),
                RuleResult.triggered("RuleC", 20, "reason C")
        );

        assertThat(aggregator.totalScore(results)).isEqualTo(75);
        assertThat(aggregator.decide(75)).isEqualTo(EvaluationDecision.BLOCK);
    }

    @Test
    void shouldIgnoreSkippedRulesInScoreCalculation() {
        List<RuleResult> results = List.of(
                RuleResult.triggered("RuleA", 30, "reason"),
                RuleResult.skippedDueToCacheUnavailability("RuleB")
        );

        assertThat(aggregator.totalScore(results)).isEqualTo(30);
    }

    @Test
    void shouldReturnZeroScoreWhenNoRulesTriggered() {
        List<RuleResult> results = List.of(
                RuleResult.notTriggered("RuleA"),
                RuleResult.notTriggered("RuleB")
        );

        assertThat(aggregator.totalScore(results)).isZero();
        assertThat(aggregator.decide(0)).isEqualTo(EvaluationDecision.APPROVE);
    }
}