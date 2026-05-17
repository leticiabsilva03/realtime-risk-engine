package com.fraudengine.domain.service;

import com.fraudengine.api.dto.EvaluationResponse;
import com.fraudengine.api.dto.RuleResultResponse;
import com.fraudengine.api.dto.TransactionRequest;
import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.EvaluationDecision;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.domain.repository.AuditWriteRepository;
import com.fraudengine.engine.RuleEngine;
import com.fraudengine.infrastructure.observability.EvaluationLogger;
import com.fraudengine.infrastructure.persistence.AuditEntity;
import com.fraudengine.infrastructure.persistence.AuditMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TransactionService {

    private final RuleEngine ruleEngine;
    private final ScoringAggregator scoringAggregator;
    private final RulesConfig rulesConfig;
    private final AuditWriteRepository auditWriteRepository;
    private final AuditMapper auditMapper;
    private final EvaluationLogger evaluationLogger;

    public TransactionService(RuleEngine ruleEngine,
                              ScoringAggregator scoringAggregator,
                              RulesConfig rulesConfig,
                              AuditWriteRepository auditWriteRepository,
                              AuditMapper auditMapper,
                              EvaluationLogger evaluationLogger) {
        this.ruleEngine = ruleEngine;
        this.scoringAggregator = scoringAggregator;
        this.rulesConfig = rulesConfig;
        this.auditWriteRepository = auditWriteRepository;
        this.auditMapper = auditMapper;
        this.evaluationLogger = evaluationLogger;
    }

    public EvaluationResponse evaluate(TransactionRequest request, boolean simulated) {
        long start = System.currentTimeMillis();
        Transaction transaction = toTransaction(request);

        List<RuleResult> results = ruleEngine.evaluate(transaction);
        int totalScore = scoringAggregator.totalScore(results);
        EvaluationDecision decision = scoringAggregator.decide(totalScore);

        List<RuleResultResponse> rulesEvaluated = results.stream()
                .map(r -> new RuleResultResponse(r.ruleName(), r.score(), r.triggered(), r.reason()))
                .toList();

        EvaluationResponse response = new EvaluationResponse(
                request.transactionId(),
                totalScore,
                decision.name(),
                rulesConfig.getVersion(),
                simulated,
                rulesEvaluated,
                Instant.now()
        );

        if (!simulated) {
            AuditEntity entity = auditMapper.toEntity(transaction, response);
            auditWriteRepository.save(entity);
        }

        evaluationLogger.logEvaluation(transaction, response, System.currentTimeMillis() - start);

        return response;
    }

    private Transaction toTransaction(TransactionRequest req) {
        return new Transaction(
                req.transactionId(),
                req.userId(),
                req.deviceId(),
                req.amount(),
                req.merchantId(),
                req.merchant(),
                req.country(),
                req.transactionAt()
        );
    }
}