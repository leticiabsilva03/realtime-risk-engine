package com.fraudengine.infrastructure.persistence;

import com.fraudengine.api.dto.EvaluationResponse;
import com.fraudengine.api.dto.RuleResultResponse;
import com.fraudengine.domain.model.Transaction;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class AuditMapper {

    private final ObjectMapper objectMapper;

    public AuditMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditEntity toEntity(Transaction tx, EvaluationResponse response) {
        String rulesJson = toJson(response.rulesEvaluated());
        return new AuditEntity(
                tx.transactionId(),
                tx.userId(),
                tx.deviceId(),
                tx.amount(),
                tx.merchantId(),
                tx.country(),
                response.totalScore(),
                response.decision(),
                response.rulesVersion(),
                rulesJson,
                response.simulated(),
                response.evaluatedAt()
        );
    }

    private String toJson(List<RuleResultResponse> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize rules to JSON", e);
        }
    }
}