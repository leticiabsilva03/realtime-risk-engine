package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.engine.Rule;
import com.fraudengine.infrastructure.cache.RedisCacheService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DeviceFingerprintRule implements Rule {

    private static final String RULE_NAME = "DeviceFingerprintRule";

    private final RulesConfig config;
    private final RedisCacheService cacheService;

    public DeviceFingerprintRule(RulesConfig config, RedisCacheService cacheService) {
        this.config = config;
        this.cacheService = cacheService;
    }

    @Override
    public RuleResult evaluate(Transaction transaction) {
        RulesConfig.DeviceFingerprintConfig cfg = config.getDeviceFingerprint();

        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        if (transaction.deviceId() == null) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        String windowKey = "device:%s:%dm".formatted(transaction.deviceId(), cfg.getWindowMinutes());
        Optional<Long> uniqueUserCount = cacheService.getUniqueUserCountForDevice(
                transaction.deviceId(), windowKey
        );

        if (uniqueUserCount.isEmpty()) {
            return RuleResult.skippedDueToCacheUnavailability(RULE_NAME);
        }

        // Dispara apenas quando o device aparece em MAIS de um userId
        if (uniqueUserCount.get() > 1L) {
            String reason = "device %s associated with %d different users in the last %d minutes"
                    .formatted(transaction.deviceId(), uniqueUserCount.get(), cfg.getWindowMinutes());
            return RuleResult.triggered(RULE_NAME, cfg.getScore(), reason);
        }

        return RuleResult.notTriggered(RULE_NAME);
    }
}