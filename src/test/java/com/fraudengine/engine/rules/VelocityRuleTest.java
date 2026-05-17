package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.infrastructure.cache.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VelocityRuleTest {

    @Mock
    private RedisCacheService cacheService;

    private VelocityRule rule;

    @BeforeEach
    void setUp() {
        RulesConfig config = new RulesConfig();

        RulesConfig.VelocityConfig velocity = new RulesConfig.VelocityConfig();
        velocity.setEnabled(true);
        velocity.setMaxTransactions(5);
        velocity.setWindowMinutes(10);
        velocity.setScore(25);
        config.setVelocity(velocity);

        rule = new VelocityRule(config, cacheService);
    }

    @Test
    void shouldTriggerWhenUserExceedsMaxTransactionsInWindow() {
        when(cacheService.getTransactionCount(anyString(), anyString()))
                .thenReturn(Optional.of(6L));

        RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(25);
        assertThat(result.reason()).contains("6");
    }

    @Test
    void shouldNotTriggerWhenUserIsBelowMaxTransactions() {
        when(cacheService.getTransactionCount(anyString(), anyString()))
                .thenReturn(Optional.of(3L));

        RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenCountIsExactlyAtMaxTransactions() {
        when(cacheService.getTransactionCount(anyString(), anyString()))
                .thenReturn(Optional.of(5L));

        RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenRuleIsDisabledInConfiguration() {
        RulesConfig config = new RulesConfig();
        RulesConfig.VelocityConfig velocity = new RulesConfig.VelocityConfig();
        velocity.setEnabled(false);
        velocity.setMaxTransactions(5);
        velocity.setWindowMinutes(10);
        velocity.setScore(25);
        config.setVelocity(velocity);

        VelocityRule disabledRule = new VelocityRule(config, cacheService);

        RuleResult result = disabledRule.evaluate(buildTransaction());

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldSkipRuleWhenCacheIsUnavailable() {
        when(cacheService.getTransactionCount(anyString(), anyString()))
                .thenReturn(Optional.empty());

        RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).contains("cache unavailable");
    }

    private Transaction buildTransaction() {
        return new Transaction(
                "txn-001",
                "usr-123",
                "dev-abc",
                new BigDecimal("500.00"),
                "mch-xyz",
                "Store XYZ",
                "BR",
                Instant.now()
        );
    }
}