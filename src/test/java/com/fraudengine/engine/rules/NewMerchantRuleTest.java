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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewMerchantRuleTest {

    @Mock
    private RedisCacheService cacheService;

    private NewMerchantRule rule;

    @BeforeEach
    void setUp() {
        RulesConfig config = new RulesConfig();

        RulesConfig.NewMerchantConfig newMerchant = new RulesConfig.NewMerchantConfig();
        newMerchant.setEnabled(true);
        newMerchant.setMinAmountToTrigger(new BigDecimal("1000.00"));
        newMerchant.setScore(20);
        config.setNewMerchant(newMerchant);

        rule = new NewMerchantRule(config, cacheService);
    }

    @Test
    void shouldTriggerWhenMerchantIsNewAndAmountExceedsMinimum() {
        when(cacheService.isFirstTransactionAtMerchant(anyString(), anyString()))
                .thenReturn(Optional.of(true));

        Transaction tx = buildTransaction(new BigDecimal("2500.00"), "mch-new");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(20);
        assertThat(result.reason()).contains("mch-new");
    }

    @Test
    void shouldNotTriggerWhenMerchantIsKnown() {
        when(cacheService.isFirstTransactionAtMerchant(anyString(), anyString()))
                .thenReturn(Optional.of(false));

        Transaction tx = buildTransaction(new BigDecimal("5000.00"), "mch-known");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenAmountIsBelowMinimumEvenForNewMerchant() {
        // Valor baixo em merchant novo é comportamento normal — não consulta o cache
        Transaction tx = buildTransaction(new BigDecimal("50.00"), "mch-new");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
        verify(cacheService, never()).isFirstTransactionAtMerchant(anyString(), anyString());
    }

    @Test
    void shouldNotTriggerWhenAmountIsExactlyAtMinimumThreshold() {
        // Threshold: dispara apenas quando ESTRITAMENTE acima do mínimo
        Transaction tx = buildTransaction(new BigDecimal("1000.00"), "mch-new");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenMerchantIdIsNull() {
        Transaction tx = buildTransaction(new BigDecimal("5000.00"), null);
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldSkipRuleWhenCacheIsUnavailable() {
        when(cacheService.isFirstTransactionAtMerchant(anyString(), anyString()))
                .thenReturn(Optional.empty());

        Transaction tx = buildTransaction(new BigDecimal("5000.00"), "mch-new");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).contains("cache unavailable");
    }

    @Test
    void shouldNotTriggerWhenRuleIsDisabledInConfiguration() {
        RulesConfig config = new RulesConfig();
        RulesConfig.NewMerchantConfig newMerchant = new RulesConfig.NewMerchantConfig();
        newMerchant.setEnabled(false);
        newMerchant.setMinAmountToTrigger(new BigDecimal("1000.00"));
        newMerchant.setScore(20);
        config.setNewMerchant(newMerchant);

        NewMerchantRule disabledRule = new NewMerchantRule(config, cacheService);
        RuleResult result = disabledRule.evaluate(buildTransaction(new BigDecimal("9999.00"), "mch-new"));

        assertThat(result.triggered()).isFalse();
    }

    private Transaction buildTransaction(BigDecimal amount, String merchantId) {
        return new Transaction(
                "txn-001", "usr-123", "dev-abc",
                amount, merchantId, "Some Store", "BR", Instant.now()
        );
    }
}