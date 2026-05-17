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
class DeviceFingerprintRuleTest {

    @Mock
    private RedisCacheService cacheService;

    private DeviceFingerprintRule rule;

    @BeforeEach
    void setUp() {
        RulesConfig config = new RulesConfig();

        RulesConfig.DeviceFingerprintConfig deviceFingerprint = new RulesConfig.DeviceFingerprintConfig();
        deviceFingerprint.setEnabled(true);
        deviceFingerprint.setScore(35);
        deviceFingerprint.setWindowMinutes(60);
        config.setDeviceFingerprint(deviceFingerprint);

        rule = new DeviceFingerprintRule(config, cacheService);
    }

    @Test
    void shouldTriggerWhenDeviceIsAssociatedWithMultipleUsers() {
        when(cacheService.getUniqueUserCountForDevice(anyString(), anyString()))
                .thenReturn(Optional.of(3L));

        RuleResult result = rule.evaluate(buildTransaction("dev-abc"));

        assertThat(result.triggered()).isTrue();
        assertThat(result.score()).isEqualTo(35);
        assertThat(result.reason()).contains("dev-abc").contains("3");
    }

    @Test
    void shouldNotTriggerWhenDeviceIsAssociatedWithSingleUser() {
        when(cacheService.getUniqueUserCountForDevice(anyString(), anyString()))
                .thenReturn(Optional.of(1L));

        RuleResult result = rule.evaluate(buildTransaction("dev-abc"));

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenUniqueUserCountIsExactlyOne() {
        when(cacheService.getUniqueUserCountForDevice(anyString(), anyString()))
                .thenReturn(Optional.of(1L));

        RuleResult result = rule.evaluate(buildTransaction("dev-abc"));

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenRuleIsDisabledInConfiguration() {
        RulesConfig config = new RulesConfig();
        RulesConfig.DeviceFingerprintConfig deviceFingerprint = new RulesConfig.DeviceFingerprintConfig();
        deviceFingerprint.setEnabled(false);
        deviceFingerprint.setScore(35);
        deviceFingerprint.setWindowMinutes(60);
        config.setDeviceFingerprint(deviceFingerprint);

        DeviceFingerprintRule disabledRule = new DeviceFingerprintRule(config, cacheService);

        RuleResult result = disabledRule.evaluate(buildTransaction("dev-abc"));

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldNotTriggerWhenDeviceIdIsNull() {
        RuleResult result = rule.evaluate(buildTransaction(null));

        assertThat(result.triggered()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void shouldSkipRuleWhenCacheIsUnavailable() {
        when(cacheService.getUniqueUserCountForDevice(anyString(), anyString()))
                .thenReturn(Optional.empty());

        RuleResult result = rule.evaluate(buildTransaction("dev-abc"));

        assertThat(result.triggered()).isFalse();
        assertThat(result.reason()).contains("cache unavailable");
    }

    private Transaction buildTransaction(String deviceId) {
        return new Transaction(
                "txn-001",
                "usr-123",
                deviceId,
                new BigDecimal("500.00"),
                "mch-xyz",
                "Store XYZ",
                "BR",
                Instant.now()
        );
    }
}