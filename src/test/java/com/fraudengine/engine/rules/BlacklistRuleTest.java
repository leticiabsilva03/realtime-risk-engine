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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistRuleTest {

    @Mock
    private RedisCacheService cacheService;

    private BlacklistRule rule;

    @BeforeEach
    void setUp() {
        RulesConfig config = new RulesConfig();

        RulesConfig.BlacklistConfig blacklist = new RulesConfig.BlacklistConfig();
        blacklist.setEnabled(true);
        blacklist.setScore(100);
        config.setBlacklist(blacklist);

        rule = new BlacklistRule(config, cacheService);
    }

    @Test
    void shouldTriggerWhenUserIdIsBlacklisted() {
        when(cacheService.isBlacklisted("usr-123")).thenReturn(Optional.of(true));

        RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.reason()).contains("usr-123");
    }

    @Test
    void shouldTriggerWhenDeviceIdIsBlacklisted() {
        when(cacheService.isBlacklisted("usr-123")).thenReturn(Optional.of(false));
        when(cacheService.isBlacklisted("dev-abc")).thenReturn(Optional.of(true));

        RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.reason()).contains("dev-abc");
    }

    @Test
    void shouldTriggerWhenMerchantIdIsBlacklisted() {
        when(cacheService.isBlacklisted("usr-123")).thenReturn(Optional.of(false));
        when(cacheService.isBlacklisted("dev-abc")).thenReturn(Optional.of(false));
        when(cacheService.isBlacklisted("mch-xyz")).thenReturn(Optional.of(true));

        RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.reason()).contains("mch-xyz");
    }

    @Test
    void shouldNotTriggerWhenNoneAreBlacklisted() {
        when(cacheService.isBlacklisted("usr-123")).thenReturn(Optional.of(false));
        when(cacheService.isBlacklisted("dev-abc")).thenReturn(Optional.of(false));
        when(cacheService.isBlacklisted("mch-xyz")).thenReturn(Optional.of(false));

        RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldSkipRuleWhenCacheIsUnavailable() {
        when(cacheService.isBlacklisted("usr-123")).thenReturn(Optional.empty());

        RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).contains("cache unavailable");
    }

    @Test
    void shouldNotTriggerWhenRuleIsDisabledInConfiguration() {
        RulesConfig config = new RulesConfig();
        RulesConfig.BlacklistConfig blacklist = new RulesConfig.BlacklistConfig();
        blacklist.setEnabled(false);
        blacklist.setScore(100);
        config.setBlacklist(blacklist);

        BlacklistRule disabledRule = new BlacklistRule(config, cacheService);
        RuleResult result = disabledRule.evaluate(buildTransaction());

        assertThat(result.triggered()).isFalse();
    }

    private Transaction buildTransaction() {
        return new Transaction(
                "txn-001", "usr-123", "dev-abc",
                new BigDecimal("500.00"), "mch-xyz", "Store XYZ", "BR", Instant.now()
        );
    }
}