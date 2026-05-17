package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.engine.Rule;
import com.fraudengine.infrastructure.cache.RedisCacheService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VelocityRule implements Rule {

    private static final String RULE_NAME = "VelocityRule";

    private final RulesConfig config;
    private final RedisCacheService cacheService;

    public VelocityRule(RulesConfig config, RedisCacheService cacheService) {
        this.config = config;
        this.cacheService = cacheService;
    }

    @Override
    public RuleResult evaluate(Transaction transaction) {
        RulesConfig.VelocityConfig cfg = config.getVelocity();

        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        String windowKey = "velocity:%s:%dm".formatted(transaction.userId(), cfg.getWindowMinutes());
        Optional<Long> count = cacheService.getTransactionCount(transaction.userId(), windowKey);

        if (count.isEmpty()) {
            return RuleResult.skippedDueToCacheUnavailability(RULE_NAME);
        }

        if (count.get() > cfg.getMaxTransactions()) {
            String reason = "%d transactions in last %d minutes"
                    .formatted(count.get(), cfg.getWindowMinutes());
            return RuleResult.triggered(RULE_NAME, cfg.getScore(), reason);
        }

        return RuleResult.notTriggered(RULE_NAME);
    }
}