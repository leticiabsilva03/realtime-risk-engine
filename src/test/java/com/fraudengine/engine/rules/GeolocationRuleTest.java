package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.infrastructure.cache.LastTransactionLocation;
import com.fraudengine.infrastructure.cache.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeolocationRuleTest {

    @Mock
    private RedisCacheService cacheService;

    private GeolocationRule rule;

    @BeforeEach
    void setUp() {
        RulesConfig config = new RulesConfig();

        RulesConfig.GeolocationConfig geo = new RulesConfig.GeolocationConfig();
        geo.setEnabled(true);
        geo.setImpossibilityWindowMinutes(120);
        geo.setHighRiskCountries(List.of("NG", "RO", "VN", "PK"));
        geo.setScoreImpossibility(40);
        geo.setScoreHighRiskCountry(20);
        config.setGeolocation(geo);

        rule = new GeolocationRule(config, cacheService);
    }

    @Test
    void shouldTriggerWithHighScoreWhenTravelIsPhysicallyImpossible() {
        // Última transação: EUA, 30 minutos atrás
        // Transação atual: Brasil — ~8.000 km em 30 min = impossível
        Instant thirtyMinutesAgo = Instant.now().minusSeconds(1800);
        when(cacheService.getLastTransactionLocation(anyString()))
                .thenReturn(Optional.of(new LastTransactionLocation("US", thirtyMinutesAgo)));

        Transaction tx = buildTransaction("BR");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(40);
        assertThat(result.reason()).contains("impossible");
    }

    @Test
    void shouldTriggerWithLowerScoreWhenCountryIsHighRisk() {
        // Sem impossibilidade física — país de alto risco é suficiente
        when(cacheService.getLastTransactionLocation(anyString()))
                .thenReturn(Optional.of(new LastTransactionLocation("NG", Instant.now().minusSeconds(7200))));

        Transaction tx = buildTransaction("NG");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(20);
        assertThat(result.reason()).contains("NG");
    }

    @Test
    void shouldNotTriggerForNormalDomesticTransaction() {
        when(cacheService.getLastTransactionLocation(anyString()))
                .thenReturn(Optional.of(new LastTransactionLocation("BR", Instant.now().minusSeconds(3600))));

        Transaction tx = buildTransaction("BR");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenNoLastLocationOnRecord() {
        // Primeira transação do usuário — sem histórico
        when(cacheService.getLastTransactionLocation(anyString()))
                .thenReturn(Optional.empty());

        Transaction tx = buildTransaction("BR");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldStillEvaluateHighRiskCountryEvenWhenCacheIsUnavailable() {
        // Cache indisponível não impede o check de país de alto risco
        when(cacheService.getLastTransactionLocation(anyString()))
                .thenReturn(Optional.empty());

        Transaction tx = buildTransaction("NG");
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(20);
    }

    @Test
    void shouldNotTriggerWhenCountryIsNull() {
        Transaction tx = buildTransaction(null);
        RuleResult result = rule.evaluate(tx);

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenRuleIsDisabledInConfiguration() {
        RulesConfig config = new RulesConfig();
        RulesConfig.GeolocationConfig geo = new RulesConfig.GeolocationConfig();
        geo.setEnabled(false);
        geo.setHighRiskCountries(List.of("NG"));
        geo.setScoreImpossibility(40);
        geo.setScoreHighRiskCountry(20);
        config.setGeolocation(geo);

        GeolocationRule disabledRule = new GeolocationRule(config, cacheService);
        RuleResult result = disabledRule.evaluate(buildTransaction("NG"));

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    private Transaction buildTransaction(String country) {
        return new Transaction(
                "txn-001", "usr-123", "dev-abc",
                new BigDecimal("500.00"), "mch-xyz", "Store XYZ",
                country, Instant.now()
        );
    }
}