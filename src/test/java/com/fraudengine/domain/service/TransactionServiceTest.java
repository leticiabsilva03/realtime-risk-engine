package com.fraudengine.domain.service;

import com.fraudengine.api.dto.EvaluationResponse;
import com.fraudengine.api.dto.TransactionRequest;
import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.EvaluationDecision;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.repository.AuditWriteRepository;
import com.fraudengine.engine.RuleEngine;
import com.fraudengine.infrastructure.observability.EvaluationLogger;
import com.fraudengine.infrastructure.persistence.AuditEntity;
import com.fraudengine.infrastructure.persistence.AuditMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private RuleEngine ruleEngine;
    @Mock private ScoringAggregator scoringAggregator;
    @Mock private AuditWriteRepository auditWriteRepository;
    @Mock private AuditMapper auditMapper;
    @Mock private EvaluationLogger evaluationLogger;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        RulesConfig config = new RulesConfig();
        config.setVersion("1.0.0");
        service = new TransactionService(ruleEngine, scoringAggregator, config,
                auditWriteRepository, auditMapper, evaluationLogger);
    }

    @Test
    void shouldReturnApproveDecisionWhenScoreIsLow() {
        when(ruleEngine.evaluate(any())).thenReturn(List.of(
                RuleResult.triggered("RuleA", 20, "reason")
        ));
        when(scoringAggregator.totalScore(any())).thenReturn(20);
        when(scoringAggregator.decide(20)).thenReturn(EvaluationDecision.APPROVE);
        when(auditMapper.toEntity(any(), any())).thenReturn(mock(AuditEntity.class));

        EvaluationResponse response = service.evaluate(buildRequest(), false);

        assertThat(response.decision()).isEqualTo("APPROVE");
        assertThat(response.totalScore()).isEqualTo(20);
        assertThat(response.simulated()).isFalse();
        assertThat(response.rulesVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldPersistAuditWhenNotSimulated() {
        when(ruleEngine.evaluate(any())).thenReturn(List.of());
        when(scoringAggregator.totalScore(any())).thenReturn(0);
        when(scoringAggregator.decide(0)).thenReturn(EvaluationDecision.APPROVE);
        when(auditMapper.toEntity(any(), any())).thenReturn(mock(AuditEntity.class));

        service.evaluate(buildRequest(), false);

        verify(auditWriteRepository, times(1)).save(any());
    }

    @Test
    void shouldNotPersistAuditWhenSimulated() {
        when(ruleEngine.evaluate(any())).thenReturn(List.of());
        when(scoringAggregator.totalScore(any())).thenReturn(0);
        when(scoringAggregator.decide(0)).thenReturn(EvaluationDecision.APPROVE);

        service.evaluate(buildRequest(), true);

        verify(auditWriteRepository, never()).save(any());
        verify(auditMapper, never()).toEntity(any(), any());
    }

    @Test
    void shouldAlwaysLogRegardlessOfSimulatedFlag() {
        when(ruleEngine.evaluate(any())).thenReturn(List.of());
        when(scoringAggregator.totalScore(any())).thenReturn(0);
        when(scoringAggregator.decide(0)).thenReturn(EvaluationDecision.APPROVE);

        service.evaluate(buildRequest(), true);   // simulate
        service.evaluate(buildRequest(), false);  // evaluate

        verify(evaluationLogger, times(2)).logEvaluation(any(), any(), anyLong());
    }

    @Test
    void shouldReturnSimulatedTrueWhenCalledFromSimulateEndpoint() {
        when(ruleEngine.evaluate(any())).thenReturn(List.of());
        when(scoringAggregator.totalScore(any())).thenReturn(0);
        when(scoringAggregator.decide(0)).thenReturn(EvaluationDecision.APPROVE);

        EvaluationResponse response = service.evaluate(buildRequest(), true);

        assertThat(response.simulated()).isTrue();
    }

    @Test
    void shouldMapAllRuleResultsToResponse() {
        when(ruleEngine.evaluate(any())).thenReturn(List.of(
                RuleResult.triggered("HighAmountRule", 30, "amount too high"),
                RuleResult.notTriggered("VelocityRule")
        ));
        when(scoringAggregator.totalScore(any())).thenReturn(30);
        when(scoringAggregator.decide(30)).thenReturn(EvaluationDecision.APPROVE);
        when(auditMapper.toEntity(any(), any())).thenReturn(mock(AuditEntity.class));

        EvaluationResponse response = service.evaluate(buildRequest(), false);

        assertThat(response.rulesEvaluated()).hasSize(2);
        assertThat(response.rulesEvaluated().get(0).rule()).isEqualTo("HighAmountRule");
        assertThat(response.rulesEvaluated().get(0).triggered()).isTrue();
        assertThat(response.rulesEvaluated().get(1).triggered()).isFalse();
    }

    private TransactionRequest buildRequest() {
        return new TransactionRequest(
                "txn-001", "usr-123", "dev-abc",
                new BigDecimal("500.00"), "mch-xyz", "Store XYZ", "BR", Instant.now()
        );
    }
}