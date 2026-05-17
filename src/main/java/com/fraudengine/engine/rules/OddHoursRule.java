package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.engine.Rule;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class OddHoursRule implements Rule {

    private static final String RULE_NAME = "OddHoursRule";

    private final RulesConfig config;

    public OddHoursRule(RulesConfig config) {
        this.config = config;
    }

    @Override
    public RuleResult evaluate(Transaction transaction) {
        RulesConfig.OddHoursConfig cfg = config.getOddHours();

        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        if (transaction.transactionAt() == null) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        // Sempre avaliamos em UTC — sem ambiguidade de timezone
        int hourUtc = transaction.transactionAt()
                .atZone(ZoneOffset.UTC)
                .getHour();

        // Janela: [startHour, endHour) — startHour inclusivo, endHour exclusivo
        if (hourUtc >= cfg.getStartHour() && hourUtc < cfg.getEndHour()) {
            String reason = "transaction at %02d:00 UTC falls within odd-hours window [%02d:00, %02d:00)"
                    .formatted(hourUtc, cfg.getStartHour(), cfg.getEndHour());
            return RuleResult.triggered(RULE_NAME, cfg.getScore(), reason);
        }

        return RuleResult.notTriggered(RULE_NAME);
    }
}