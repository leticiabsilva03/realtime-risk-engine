package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HighAmountRuleTest {

    private HighAmountRule rule;

    @BeforeEach
    void setUp() {
        RulesConfig config = new RulesConfig();

        RulesConfig.HighAmountConfig highAmount = new RulesConfig.HighAmountConfig();
        highAmount.setEnabled(true);
        highAmount.setThreshold(new BigDecimal("5000.00"));
        highAmount.setScore(30);
        config.setHighAmount(highAmount);

        rule = new HighAmountRule(config);
    }

    @Test
    void shouldTriggerWhenTransactionExceedsConfiguredThreshold() {
        Transaction tx = buildTransaction(new BigDecimal("9500.00"));

        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(30);
        assertThat(result.reason()).contains("9500");
    }

    @Test
    void shouldNotTriggerWhenTransactionIsBelowThreshold() {
        Transaction tx = buildTransaction(new BigDecimal("4999.99"));

        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenAmountIsExactlyAtThreshold() {
        Transaction tx = buildTransaction(new BigDecimal("5000.00"));

        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenRuleIsDisabledInConfiguration() {
        RulesConfig config = new RulesConfig();
        RulesConfig.HighAmountConfig highAmount = new RulesConfig.HighAmountConfig();
        highAmount.setEnabled(false);
        highAmount.setThreshold(new BigDecimal("5000.00"));
        highAmount.setScore(30);
        config.setHighAmount(highAmount);

        HighAmountRule disabledRule = new HighAmountRule(config);
        Transaction tx = buildTransaction(new BigDecimal("99999.00"));

        RuleResult result = disabledRule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldReturnZeroScoreAndNotTriggerForNullAmount() {
        Transaction tx = buildTransaction(null);

        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    private Transaction buildTransaction(BigDecimal amount) {
        return new Transaction(
                "txn-001",
                "usr-123",
                "dev-abc",
                amount,
                "mch-xyz",
                "Store XYZ",
                "BR",
                Instant.now()
        );
    }
}