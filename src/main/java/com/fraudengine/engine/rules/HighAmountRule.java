package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.engine.Rule;
import org.springframework.stereotype.Component;

@Component
public class HighAmountRule implements Rule {

    private static final String RULE_NAME = "HighAmountRule";

    private final RulesConfig config;

    public HighAmountRule(RulesConfig config) {
        this.config = config;
    }

    @Override
    public RuleResult evaluate(Transaction transaction) {
        RulesConfig.HighAmountConfig cfg = config.getHighAmount();

        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        if (transaction.amount() == null) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        // Dispara apenas quando ESTRITAMENTE maior que o threshold
        if (transaction.amount().compareTo(cfg.getThreshold()) > 0) {
            String reason = "amount %s exceeds threshold %s"
                    .formatted(transaction.amount(), cfg.getThreshold());
            return RuleResult.triggered(RULE_NAME, cfg.getScore(), reason);
        }

        return RuleResult.notTriggered(RULE_NAME);
    }
}