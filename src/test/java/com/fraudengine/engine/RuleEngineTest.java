package com.fraudengine.engine;

import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private Rule ruleA;

    @Mock
    private Rule ruleB;

    @Test
    void shouldExecuteAllRulesAndReturnAllResults() {
        Transaction tx = buildTransaction();
        when(ruleA.evaluate(tx)).thenReturn(RuleResult.triggered("RuleA", 30, "reason A"));
        when(ruleB.evaluate(tx)).thenReturn(RuleResult.notTriggered("RuleB"));

        RuleEngine engine = new RuleEngine(List.of(ruleA, ruleB));
        List<RuleResult> results = engine.evaluate(tx);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).triggered()).isTrue();
        assertThat(results.get(1).triggered()).isFalse();
    }

    @Test
    void shouldReturnEmptyListWhenNoRulesConfigured() {
        RuleEngine engine = new RuleEngine(List.of());
        List<RuleResult> results = engine.evaluate(buildTransaction());

        assertThat(results).isEmpty();
    }

    private Transaction buildTransaction() {
        return new Transaction(
                "txn-001", "usr-123", "dev-abc",
                new BigDecimal("1000.00"), "mch-xyz", "Store", "BR",
                Instant.now()
        );
    }
}