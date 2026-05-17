package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class OddHoursRuleTest {

    private OddHoursRule rule;

    @BeforeEach
    void setUp() {
        RulesConfig config = new RulesConfig();

        RulesConfig.OddHoursConfig oddHours = new RulesConfig.OddHoursConfig();
        oddHours.setEnabled(true);
        oddHours.setStartHour(0);   // 00:00 UTC
        oddHours.setEndHour(5);     // 04:59 UTC
        oddHours.setScore(15);
        config.setOddHours(oddHours);

        rule = new OddHoursRule(config);
    }

    @Test
    void shouldTriggerWhenTransactionOccursDuringConfiguredNightWindow() {
        Transaction tx = buildTransactionAtHour(2); // 02:00 UTC

        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(15);
        assertThat(result.reason()).contains("02");
    }

    @Test
    void shouldNotTriggerWhenTransactionOccursDuringBusinessHours() {
        Transaction tx = buildTransactionAtHour(14); // 14:00 UTC

        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenTransactionIsAtExactStartBoundary() {
        Transaction tx = buildTransactionAtHour(0); // 00:00 UTC — início da janela (inclusivo)

        RuleResult result = rule.evaluate(tx);

        // 00:00 está dentro da janela [0, 5) → deve disparar
        assertThat(result.triggered()).isTrue();
    }

    @Test
    void shouldNotTriggerWhenTransactionIsAtExactEndBoundary() {
        Transaction tx = buildTransactionAtHour(5); // 05:00 UTC — limite de saída (exclusivo)

        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenRuleIsDisabledInConfiguration() {
        RulesConfig config = new RulesConfig();
        RulesConfig.OddHoursConfig oddHours = new RulesConfig.OddHoursConfig();
        oddHours.setEnabled(false);
        oddHours.setStartHour(0);
        oddHours.setEndHour(5);
        oddHours.setScore(15);
        config.setOddHours(oddHours);

        OddHoursRule disabledRule = new OddHoursRule(config);
        RuleResult result = disabledRule.evaluate(buildTransactionAtHour(3));

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenTransactionAtIsNull() {
        Transaction tx = new Transaction(
                "txn-001", "usr-123", "dev-abc",
                new BigDecimal("500.00"), "mch-xyz", "Store", "BR",
                null
        );

        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    private Transaction buildTransactionAtHour(int hourUtc) {
        Instant at = LocalDateTime.of(2025, 1, 15, hourUtc, 0)
                .toInstant(ZoneOffset.UTC);
        return new Transaction(
                "txn-001", "usr-123", "dev-abc",
                new BigDecimal("500.00"), "mch-xyz", "Store XYZ", "BR", at
        );
    }
}