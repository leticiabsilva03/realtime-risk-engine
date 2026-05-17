package com.fraudengine.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelTest {

    // ──────────────────────────────────────────────────────────────────────────
    // Transaction
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldCreateTransactionWithAllFields() {
        var transaction = new Transaction(
                "txn-001",
                "usr-123",
                "dev-abc",
                new BigDecimal("9500.00"),
                "mch-xyz",
                "Store XYZ",
                "NG",
                Instant.parse("2025-02-06T22:07:00Z")
        );

        assertThat(transaction.transactionId()).isEqualTo("txn-001");
        assertThat(transaction.userId()).isEqualTo("usr-123");
        assertThat(transaction.deviceId()).isEqualTo("dev-abc");
        assertThat(transaction.amount()).isEqualByComparingTo("9500.00");
        assertThat(transaction.merchantId()).isEqualTo("mch-xyz");
        assertThat(transaction.merchant()).isEqualTo("Store XYZ");
        assertThat(transaction.country()).isEqualTo("NG");
        assertThat(transaction.transactionAt()).isEqualTo(Instant.parse("2025-02-06T22:07:00Z"));
    }

    @Test
    void shouldCreateTransactionWithNullableFieldsAbsent() {
        // deviceId, merchantId, merchant, country são todos nullable no modelo
        var transaction = new Transaction(
                "txn-002",
                "usr-456",
                null,
                new BigDecimal("100.00"),
                null,
                null,
                null,
                Instant.now()
        );

        assertThat(transaction.deviceId()).isNull();
        assertThat(transaction.merchantId()).isNull();
        assertThat(transaction.merchant()).isNull();
        assertThat(transaction.country()).isNull();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // RuleResult
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void shouldCreateTriggeredResultWithCorrectFields() {
        var result = RuleResult.triggered("HighAmountRule", 30, "amount 9500 exceeds threshold 5000");

        assertThat(result.ruleName()).isEqualTo("HighAmountRule");
        assertThat(result.score()).isEqualTo(30);
        assertThat(result.triggered()).isTrue();
        assertThat(result.reason()).isEqualTo("amount 9500 exceeds threshold 5000");
    }

    @Test
    void shouldCreateNotTriggeredResultWithZeroScoreAndNullReason() {
        var result = RuleResult.notTriggered("VelocityRule");

        assertThat(result.ruleName()).isEqualTo("VelocityRule");
        assertThat(result.score()).isZero();
        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).isNull();
    }

    @Test
    void shouldCreateCacheUnavailableResultAsNotTriggered() {
        var result = RuleResult.skippedDueToCacheUnavailability("BlacklistRule");

        // Quando o cache falha, a regra não deve inflar o score artificialmente
        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
        assertThat(result.reason()).contains("cache unavailable");
        assertThat(result.ruleName()).isEqualTo("BlacklistRule");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // EvaluationDecision
    // ──────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "score {0} deve resultar em {1}")
    @CsvSource({
            "0,   APPROVE",
            "1,   APPROVE",
            "39,  APPROVE",
            "40,  REVIEW",
            "55,  REVIEW",
            "69,  REVIEW",
            "70,  BLOCK",
            "85,  BLOCK",
            "100, BLOCK"
    })
    void shouldClassifyScoreIntoCorrectDecision(int score, EvaluationDecision expectedDecision) {
        assertThat(EvaluationDecision.from(score)).isEqualTo(expectedDecision);
    }

    @Test
    void shouldThrowWhenScoreIsNegative() {
        assertThatThrownBy(() -> EvaluationDecision.from(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }

    @Test
    void shouldThrowWhenScoreExceedsMaximum() {
        assertThatThrownBy(() -> EvaluationDecision.from(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("101");
    }

    @Test
    void shouldApproveAtExactLowerBoundary() {
        assertThat(EvaluationDecision.from(0)).isEqualTo(EvaluationDecision.APPROVE);
    }

    @Test
    void shouldApproveAtExactUpperBoundary() {
        assertThat(EvaluationDecision.from(39)).isEqualTo(EvaluationDecision.APPROVE);
    }

    @Test
    void shouldReviewAtExactLowerBoundary() {
        assertThat(EvaluationDecision.from(40)).isEqualTo(EvaluationDecision.REVIEW);
    }

    @Test
    void shouldBlockAtExactLowerBoundary() {
        assertThat(EvaluationDecision.from(70)).isEqualTo(EvaluationDecision.BLOCK);
    }

    @Test
    void shouldBlockAtExactUpperBoundary() {
        assertThat(EvaluationDecision.from(100)).isEqualTo(EvaluationDecision.BLOCK);
    }
}