package com.fraudengine.engine;

import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleEngine {

    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        this.rules = rules;
    }

    public List<RuleResult> evaluate(Transaction transaction) {
        return rules.stream()
                .map(rule -> rule.evaluate(transaction))
                .toList();
    }
}