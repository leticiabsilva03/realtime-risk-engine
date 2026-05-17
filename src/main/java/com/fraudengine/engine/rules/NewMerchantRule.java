package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.engine.Rule;
import com.fraudengine.infrastructure.cache.RedisCacheService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NewMerchantRule implements Rule {

    private static final String RULE_NAME = "NewMerchantRule";

    private final RulesConfig config;
    private final RedisCacheService cacheService;

    public NewMerchantRule(RulesConfig config, RedisCacheService cacheService) {
        this.config = config;
        this.cacheService = cacheService;
    }

    @Override
    public RuleResult evaluate(Transaction transaction) {
        RulesConfig.NewMerchantConfig cfg = config.getNewMerchant();

        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        if (transaction.merchantId() == null || transaction.amount() == null) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        // Verifica amount primeiro — evita consulta ao Redis para transações de baixo valor
        if (transaction.amount().compareTo(cfg.getMinAmountToTrigger()) <= 0) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        Optional<Boolean> isFirst = cacheService.isFirstTransactionAtMerchant(
                transaction.userId(), transaction.merchantId()
        );

        if (isFirst.isEmpty()) {
            return RuleResult.skippedDueToCacheUnavailability(RULE_NAME);
        }

        if (Boolean.TRUE.equals(isFirst.get())) {
            String reason = "first transaction at merchant %s with amount above threshold %s"
                    .formatted(transaction.merchantId(), cfg.getMinAmountToTrigger());
            return RuleResult.triggered(RULE_NAME, cfg.getScore(), reason);
        }

        return RuleResult.notTriggered(RULE_NAME);
    }
}