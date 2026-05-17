package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.engine.Rule;
import com.fraudengine.infrastructure.cache.RedisCacheService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BlacklistRule implements Rule {

    private static final String RULE_NAME = "BlacklistRule";

    private final RulesConfig config;
    private final RedisCacheService cacheService;

    public BlacklistRule(RulesConfig config, RedisCacheService cacheService) {
        this.config = config;
        this.cacheService = cacheService;
    }

    @Override
    public RuleResult evaluate(Transaction transaction) {
        RulesConfig.BlacklistConfig cfg = config.getBlacklist();

        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        // Verifica userId — sempre presente, não nullable
        Optional<Boolean> userBlacklisted = cacheService.isBlacklisted(transaction.userId());
        if (userBlacklisted.isEmpty()) {
            return RuleResult.skippedDueToCacheUnavailability(RULE_NAME);
        }
        if (Boolean.TRUE.equals(userBlacklisted.get())) {
            return RuleResult.triggered(RULE_NAME, cfg.getScore(),
                    "userId %s is blacklisted".formatted(transaction.userId()));
        }

        // Verifica deviceId — nullable
        if (transaction.deviceId() != null) {
            Optional<Boolean> deviceBlacklisted = cacheService.isBlacklisted(transaction.deviceId());
            if (deviceBlacklisted.isEmpty()) {
                return RuleResult.skippedDueToCacheUnavailability(RULE_NAME);
            }
            if (Boolean.TRUE.equals(deviceBlacklisted.get())) {
                return RuleResult.triggered(RULE_NAME, cfg.getScore(),
                        "deviceId %s is blacklisted".formatted(transaction.deviceId()));
            }
        }

        // Verifica merchantId — nullable
        if (transaction.merchantId() != null) {
            Optional<Boolean> merchantBlacklisted = cacheService.isBlacklisted(transaction.merchantId());
            if (merchantBlacklisted.isEmpty()) {
                return RuleResult.skippedDueToCacheUnavailability(RULE_NAME);
            }
            if (Boolean.TRUE.equals(merchantBlacklisted.get())) {
                return RuleResult.triggered(RULE_NAME, cfg.getScore(),
                        "merchantId %s is blacklisted".formatted(transaction.merchantId()));
            }
        }

        return RuleResult.notTriggered(RULE_NAME);
    }
}